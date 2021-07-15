package org.batfish.question.filterlinereachability;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.common.bdd.PermitAndDenyBdds.takeDifferentActions;
import static org.batfish.question.filterlinereachability.FilterLineReachabilityRows.createMetadata;
import static org.batfish.question.filterlinereachability.FilterLineReachabilityUtils.getReferencedAcls;
import static org.batfish.question.filterlinereachability.FilterLineReachabilityUtils.getReferencedInterfaces;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.common.Answerer;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.common.bdd.PermitAndDenyBdds;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.CollectionUtil;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.acl.ActionGetter;
import org.batfish.datamodel.acl.CanonicalAcl;
import org.batfish.datamodel.acl.CircularReferenceException;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.UndefinedReferenceException;
import org.batfish.datamodel.answers.AclSpecs;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.specifier.FilterSpecifier;
import org.batfish.specifier.SpecifierContext;

/** Answers {@link FilterLineReachabilityQuestion}. */
@ParametersAreNonnullByDefault
public class FilterLineReachabilityAnswerer extends Answerer {

  public FilterLineReachabilityAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public TableAnswerElement answer(NetworkSnapshot snapshot) {
    FilterLineReachabilityQuestion question = (FilterLineReachabilityQuestion) _question;
    FilterLineReachabilityRows answerRows = new FilterLineReachabilityRows();

    SpecifierContext ctxt = _batfish.specifierContext(snapshot);

    Map<String, Set<IpAccessList>> specifiedAcls = getSpecifiedFilters(question, ctxt);

    SortedMap<String, Configuration> configurations = _batfish.loadConfigurations(snapshot);
    List<AclSpecs> aclSpecs = getAclSpecs(configurations, specifiedAcls, answerRows);
    answerAclReachability(aclSpecs, answerRows);
    TableAnswerElement answer = new TableAnswerElement(createMetadata(question));
    answer.postProcessAnswer(question, answerRows.getRows());
    return answer;
  }

  private static final class AclNode {

    private final class Dependency {
      public final AclNode dependency;
      public final Set<Integer> lineNums = new TreeSet<>();

      public Dependency(AclNode dependency, int lineNum) {
        this.dependency = dependency;
        lineNums.add(lineNum);
      }
    }

    private final IpAccessList _acl;
    private final Set<Integer> _linesWithUndefinedRefs = new TreeSet<>();
    private final Set<Integer> _linesInCycles = new TreeSet<>();
    private final List<Dependency> _dependencies = new ArrayList<>();
    private final Set<String> _interfaces = new TreeSet<>();
    private IpAccessList _sanitizedAcl;
    private List<AclLine> _sanitizedLines;

    public AclNode(IpAccessList acl) {
      _acl = acl;
    }

    /**
     * Replaces match expr of the line at the given {@code lineNum} with a {@link FalseExpr}. Used
     * to ignore lines with undefined or cyclic references.
     */
    public void markLineUnmatchable(int lineNum) {
      _sanitizedLines = firstNonNull(_sanitizedLines, new ArrayList<>(_acl.getLines()));
      AclLine originalLine = _sanitizedLines.remove(lineNum);

      // If the original line has a concrete action, preserve it; otherwise default to DENY
      LineAction unmatchableLineAction =
          firstNonNull(ActionGetter.getAction(originalLine), LineAction.DENY);
      _sanitizedLines.add(
          lineNum,
          ExprAclLine.builder()
              .setName(originalLine.getName())
              .setMatchCondition(FalseExpr.INSTANCE)
              .setAction(unmatchableLineAction)
              .build());
    }

    public void sanitizeLine(int lineNum, AclLine sanitizedLine) {
      _sanitizedLines = firstNonNull(_sanitizedLines, new ArrayList<>(_acl.getLines()));
      _sanitizedLines.remove(lineNum);
      _sanitizedLines.add(lineNum, sanitizedLine);
    }

    public void sanitizeCycle(ImmutableList<String> cycleAcls) {
      // Remove previous ACL from referencing ACLs
      int aclIndex = cycleAcls.indexOf(_acl.getName());
      int cycleSize = cycleAcls.size();

      // Remove next ACL from dependencies, and record line numbers that reference dependency
      String nextAclName = cycleAcls.get((aclIndex + 1) % cycleSize);
      int dependencyIndex = 0;
      while (!_dependencies.get(dependencyIndex).dependency.getName().equals(nextAclName)) {
        dependencyIndex++;
      }

      for (int lineNum : _dependencies.remove(dependencyIndex).lineNums) {
        _linesInCycles.add(lineNum);
        markLineUnmatchable(lineNum);
      }
    }

    public List<AclNode> getDependencies() {
      return _dependencies.stream().map(d -> d.dependency).collect(Collectors.toList());
    }

    public Map<String, IpAccessList> getFlatDependencies() {
      Map<String, IpAccessList> ret = new TreeMap<>();
      for (Dependency d : _dependencies) {
        ret.put(d.dependency.getName(), d.dependency._sanitizedAcl);
        ret.putAll(d.dependency.getFlatDependencies());
      }
      return ret;
    }

    public Set<String> getInterfaceDependencies() {
      Set<String> interfaceDependencies = new TreeSet<>(_interfaces);
      for (Dependency d : _dependencies) {
        interfaceDependencies.addAll(d.dependency.getInterfaceDependencies());
      }
      return interfaceDependencies;
    }

    public void addDependency(AclNode dependency, int lineNum) {
      for (Dependency d : _dependencies) {
        if (d.dependency.getName().equals(dependency.getName())) {
          d.lineNums.add(lineNum);
          return;
        }
      }
      _dependencies.add(new Dependency(dependency, lineNum));
    }

    public void addInterfaces(Set<String> newInterfaces) {
      _interfaces.addAll(newInterfaces);
    }

    public void addUndefinedRef(int lineNum) {
      _linesWithUndefinedRefs.add(lineNum);
      markLineUnmatchable(lineNum);
    }

    public void buildSanitizedAcl() {
      // If _sanitizedLines was never initialized, just use original ACL for sanitized ACL
      _sanitizedAcl =
          _sanitizedLines == null
              ? _acl
              : IpAccessList.builder().setName(getName()).setLines(_sanitizedLines).build();
    }

    public IpAccessList getAcl() {
      return _acl;
    }

    public Set<Integer> getLinesInCycles() {
      return _linesInCycles;
    }

    public Set<Integer> getLinesWithUndefinedRefs() {
      return _linesWithUndefinedRefs;
    }

    public IpAccessList getSanitizedAcl() {
      return _sanitizedAcl;
    }

    public String getName() {
      return _acl.getName();
    }
  }

  private static void createAclNode(
      IpAccessList acl,
      Map<String, AclNode> aclNodeMap,
      Map<String, IpAccessList> acls,
      HeaderSpaceSanitizer headerSpaceSanitizer,
      Set<String> nodeInterfaces) {

    // Create ACL node for current ACL
    AclNode node = new AclNode(acl);
    aclNodeMap.put(acl.getName(), node);

    // Go through lines and add dependencies
    int index = 0;
    for (AclLine line : acl.getLines()) {
      boolean lineMarkedUnmatchable = false;

      // Find all references to other ACLs and record them
      Set<String> referencedAcls = getReferencedAcls(line);
      if (!referencedAcls.isEmpty()) {
        if (!acls.keySet().containsAll(referencedAcls)) {
          // Not all referenced ACLs exist. Mark line as unmatchable.
          node.addUndefinedRef(index);
          lineMarkedUnmatchable = true;
        } else {
          for (String referencedAclName : referencedAcls) {
            AclNode referencedAclNode = aclNodeMap.get(referencedAclName);
            if (referencedAclNode == null) {
              // Referenced ACL not yet recorded; recurse on it
              createAclNode(
                  acls.get(referencedAclName),
                  aclNodeMap,
                  acls,
                  headerSpaceSanitizer,
                  nodeInterfaces);
              referencedAclNode = aclNodeMap.get(referencedAclName);
            }
            // Referenced ACL has now been recorded; add dependency
            node.addDependency(referencedAclNode, index);
          }
        }
      }

      // Dereference all IpSpace references, or mark line unmatchable if it has invalid references
      if (!lineMarkedUnmatchable) {
        try {
          AclLine sanitizedForIpSpaces = headerSpaceSanitizer.visit(line);
          if (!line.equals(sanitizedForIpSpaces)) {
            node.sanitizeLine(index, sanitizedForIpSpaces);
          }
        } catch (CircularReferenceException | UndefinedReferenceException e) {
          // Line contains invalid IpSpaceReference: undefined or part of a circular reference chain
          node.addUndefinedRef(index);
          lineMarkedUnmatchable = true;
        }
      }

      // Find all references to interfaces and ensure they exist
      if (!lineMarkedUnmatchable) {
        Set<String> referencedInterfaces = getReferencedInterfaces(line);
        if (!nodeInterfaces.containsAll(referencedInterfaces)) {
          // Line references an undefined source interface. Report undefined ref.
          node.addUndefinedRef(index);
        } else {
          node.addInterfaces(referencedInterfaces);
        }
      }

      index++;
    }
  }

  private static List<ImmutableList<String>> sanitizeNode(
      AclNode node, List<AclNode> visited, Set<String> sanitized, Map<String, AclNode> aclNodeMap) {

    // Mark starting node as visited
    visited.add(node);

    // Create set to hold cycles found
    List<ImmutableList<String>> cyclesFound = new ArrayList<>();

    // Go through dependencies (each ACL this one depends on will only appear as one dependency)
    for (AclNode dependency : node.getDependencies()) {
      if (sanitized.contains(dependency.getName())) {
        // We've already checked out the dependency. It must not be in a cycle with current ACL.
        continue;
      }
      int dependencyIndex = visited.indexOf(dependency);
      if (dependencyIndex != -1) {
        // Found a new cycle.
        ImmutableList<String> cycleAcls =
            ImmutableList.copyOf(
                visited.subList(dependencyIndex, visited.size()).stream()
                    .map(AclNode::getName)
                    .collect(Collectors.toList()));
        cyclesFound.add(cycleAcls);
      } else {
        // No cycle found; recurse on dependency to see if there is a cycle farther down.
        cyclesFound.addAll(
            sanitizeNode(aclNodeMap.get(dependency.getName()), visited, sanitized, aclNodeMap));
      }
    }
    // Remove current node from visited list
    visited.remove(node);

    // Record found cycles in this node.
    for (ImmutableList<String> cycleAcls : cyclesFound) {
      int indexOfThisAcl = cycleAcls.indexOf(node.getName());
      if (indexOfThisAcl != -1) {
        node.sanitizeCycle(cycleAcls);
      }
    }

    // Now that all cycles are recorded, never explore this node again, and sanitize its ACL.
    node.buildSanitizedAcl();
    sanitized.add(node.getName());
    return cyclesFound;
  }

  /**
   * Collects the list of specified filters that we need to process, based on the nodes desired, the
   * filters desired, and whether generated filters are ignored
   */
  static Map<String, Set<IpAccessList>> getSpecifiedFilters(
      FilterLineReachabilityQuestion question, SpecifierContext ctxt) {
    Set<String> specifiedNodes = question.nodeSpecifier().resolve(ctxt);
    FilterSpecifier filterSpecifier = question.getFilterSpecifier();

    return CollectionUtil.toImmutableMap(
        specifiedNodes,
        Function.identity(),
        node ->
            filterSpecifier.resolve(node, ctxt).stream()
                .filter(f -> !(question.getIgnoreComposites() && f.isComposite()))
                .collect(ImmutableSet.toImmutableSet()));
  }

  /**
   * Generates the list of {@link AclSpecs} objects to analyze in preparation for some ACL
   * reachability question. Besides returning the {@link AclSpecs}, this method adds cycle results
   * to the answer and provides sanitized {@link IpAccessList}s without cycles, undefined
   * references, or dependence on named IP spaces.
   *
   * @param configurations Mapping of all hostnames to their {@link Configuration}s
   * @param specifiedAcls Specified ACLs to canonicalize, indexed by hostname.
   * @param answer Answer for ACL reachability question for which these {@link AclSpecs} are being
   *     generated
   * @return List of {@link AclSpecs} objects to analyze for an ACL reachability question with the
   *     given specified nodes and ACL regex.
   */
  @VisibleForTesting
  public static List<AclSpecs> getAclSpecs(
      SortedMap<String, Configuration> configurations,
      Map<String, Set<IpAccessList>> specifiedAcls,
      FilterLineReachabilityRows answer) {
    List<AclSpecs.Builder> aclSpecs = new ArrayList<>();

    /*
     - For each ACL, build a CanonicalAcl structure with that ACL and referenced ACLs & interfaces
     - Deal with any references to undefined ACLs, IpSpaces, or interfaces
     - Deal with any cycles in ACL references
    */
    for (String hostname : configurations.keySet()) {
      if (specifiedAcls.containsKey(hostname)) {
        Configuration c = configurations.get(hostname);
        Set<IpAccessList> acls = specifiedAcls.get(hostname);
        HeaderSpaceSanitizer headerSpaceSanitizer = new HeaderSpaceSanitizer(c.getIpSpaces());
        Map<String, Interface> nodeInterfaces = c.getAllInterfaces();

        // Build graph of AclNodes containing pointers to dependencies and referencing nodes
        Map<String, AclNode> aclNodeMap = new TreeMap<>();
        for (IpAccessList acl : acls) {
          String aclName = acl.getName();
          if (!aclNodeMap.containsKey(aclName)) {
            createAclNode(
                acl,
                aclNodeMap,
                c.getIpAccessLists(),
                headerSpaceSanitizer,
                nodeInterfaces.keySet());
          }
        }

        // Sanitize nodes in graph (finds all cycles, creates sanitized versions of IpAccessLists)
        Set<String> sanitizedAcls = new TreeSet<>();
        for (AclNode node : aclNodeMap.values()) {
          if (!sanitizedAcls.contains(node.getName())) {
            List<ImmutableList<String>> cycles =
                sanitizeNode(node, new ArrayList<>(), sanitizedAcls, aclNodeMap);
            for (ImmutableList<String> cycleAcls : cycles) {
              answer.addCycle(hostname, cycleAcls);
            }
          }
        }

        // For each ACL specified by aclRegex, create a CanonicalAcl with its dependencies
        for (IpAccessList acl : acls) {
          String aclName = acl.getName();
          AclNode node = aclNodeMap.get(aclName);

          // Finalize interfaces. If ACL references all interfaces on the device, keep interfaces
          // list as-is; otherwise, add one extra interface to represent the "unreferenced
          // interface not originating from router" possibility. Needs to have a name different
          // from any referenced interface.
          Set<String> referencedInterfaces = node.getInterfaceDependencies();
          if (referencedInterfaces.size() < nodeInterfaces.size()) {
            // At least one interface was not referenced by the ACL. Represent that option.
            String unreferencedIfaceName = "unreferencedInterface";
            int n = 0;
            while (referencedInterfaces.contains(unreferencedIfaceName)) {
              unreferencedIfaceName = "unreferencedInterface" + n;
              n++;
            }
            referencedInterfaces = new TreeSet<>(referencedInterfaces);
            referencedInterfaces.add(unreferencedIfaceName);
          }

          CanonicalAcl currentAcl =
              new CanonicalAcl(
                  node.getSanitizedAcl(),
                  node.getAcl(),
                  node.getFlatDependencies(),
                  referencedInterfaces,
                  node.getLinesWithUndefinedRefs(),
                  node.getLinesInCycles());

          // If an identical ACL exists, add current hostname/aclName pair; otherwise, add new ACL
          boolean added = false;
          for (AclSpecs.Builder aclSpec : aclSpecs) {
            if (aclSpec.getAcl().equals(currentAcl)) {
              aclSpec.addSource(hostname, aclName);
              added = true;
              break;
            }
          }
          if (!added) {
            aclSpecs.add(AclSpecs.builder().setAcl(currentAcl).addSource(hostname, aclName));
          }
        }
      }
    }
    return aclSpecs.stream().map(AclSpecs.Builder::build).collect(Collectors.toList());
  }

  private static class LineAndWeight {
    private final int _line;
    private final double _weight;

    public LineAndWeight(int line, double weight) {
      _line = line;
      _weight = weight;
    }

    private int getLine() {
      return _line;
    }

    private double getWeight() {
      return _weight;
    }

    private static final Comparator<LineAndWeight> COMPARATOR =
        Comparator.comparing(LineAndWeight::getWeight)
            .reversed()
            .thenComparing(LineAndWeight::getLine);
  }

  /**
   * Info about how some ACL line is blocked: which lines block it and whether any of them treat any
   * packet differently than the blocked line would
   */
  static class BlockingProperties {
    @Nonnull private final SortedSet<Integer> _blockingLineNums;
    private final boolean _diffAction;

    BlockingProperties(@Nonnull SortedSet<Integer> blockingLineNums, boolean diffAction) {
      _blockingLineNums = blockingLineNums;
      _diffAction = diffAction;
    }

    @Nonnull
    SortedSet<Integer> getBlockingLineNums() {
      return _blockingLineNums;
    }

    /**
     * If true, indicates that some packet that matches the blocked line would be treated
     * differently by some blocking line. (Does not require that the packet be able to reach that
     * blocking line.)
     */
    boolean getDiffAction() {
      return _diffAction;
    }
  }

  @VisibleForTesting
  static BlockingProperties findBlockingPropsForLine(
      int blockedLineNum, List<PermitAndDenyBdds> bdds) {
    PermitAndDenyBdds blockedLine = bdds.get(blockedLineNum);

    ImmutableSortedSet.Builder<LineAndWeight> linesByWeight =
        ImmutableSortedSet.orderedBy(LineAndWeight.COMPARATOR);

    // First, we find all lines before blockedLine that actually terminate any packets
    // blockedLine intends to. These, collectively, are the (partially-)blocking lines.
    //
    // In this same loop, we also compute the overlap of each such line with the blocked line
    // and weight each blocking line by that overlap.
    //
    // Finally, we record whether any of these lines has a different action than the blocked line.
    PermitAndDenyBdds restOfLine = blockedLine;
    boolean diffAction = false; // true if some partially-blocking line has a different action.
    for (int prevLineNum = 0; prevLineNum < blockedLineNum && !restOfLine.isZero(); prevLineNum++) {
      PermitAndDenyBdds prevLine = bdds.get(prevLineNum);

      if (!prevLine.getMatchBdd().andSat(restOfLine.getMatchBdd())) {
        continue;
      }

      BDD blockedLineOverlap = prevLine.getMatchBdd().and(blockedLine.getMatchBdd());
      linesByWeight.add(new LineAndWeight(prevLineNum, blockedLineOverlap.satCount()));
      diffAction = diffAction || takeDifferentActions(prevLine, restOfLine);
      restOfLine = restOfLine.diff(prevLine.getMatchBdd());
    }

    // In this second loop, we compute the answer:
    // * include partially-blocking lines in weight order until the blocked line is fully blocked by
    //   this subset.
    // * also include the largest blocking line with a different action than the blocked line, if
    //   not already in the above subset.
    ImmutableSortedSet.Builder<Integer> answerLines = ImmutableSortedSet.naturalOrder();
    restOfLine = blockedLine;
    boolean needDiffAction = diffAction;
    for (LineAndWeight line : linesByWeight.build()) {
      int curLineNum = line.getLine();
      PermitAndDenyBdds curLine = bdds.get(curLineNum);
      boolean curDiff = takeDifferentActions(curLine, blockedLine);

      // The original line is still not blocked, or this is the first line with a different action.
      if (!restOfLine.isZero() || needDiffAction && curDiff) {
        restOfLine = restOfLine.diff(curLine.getMatchBdd());
        answerLines.add(curLineNum);
        needDiffAction = needDiffAction && !curDiff;
      }

      // The original line is blocked and we have a line with a different action (if such exists).
      if (restOfLine.isZero() && !needDiffAction) {
        break;
      }
    }

    return new BlockingProperties(answerLines.build(), diffAction);
  }

  public static void answerAclReachabilityLine(
      AclSpecs aclSpec, BDDPacket bddPacket, FilterLineReachabilityRows answerRows) {
    BDDFactory bddFactory = bddPacket.getFactory();
    BDDSourceManager sourceMgr =
        BDDSourceManager.forInterfaces(bddPacket, aclSpec.acl.getInterfaces());
    IpAccessListToBdd ipAccessListToBdd =
        new IpAccessListToBddImpl(
            bddPacket, sourceMgr, aclSpec.acl.getDependencies(), ImmutableMap.of());

    IpAccessList ipAcl = aclSpec.acl.getSanitizedAcl();
    List<AclLine> lines = ipAcl.getLines();

    /* Convert every line to permit and deny BDDs. */
    List<PermitAndDenyBdds> ipLineToBDDMap =
        lines.stream().map(ipAccessListToBdd::toPermitAndDenyBdds).collect(Collectors.toList());

    /* Pass over BDDs to classify each as unmatchable, unreachable, or (implicitly) reachable. */
    BDD unmatchedPackets = bddFactory.one(); // The packets that are not yet matched by the ACL.
    ListIterator<PermitAndDenyBdds> lineIt = ipLineToBDDMap.listIterator();
    while (lineIt.hasNext()) {
      int lineNum = lineIt.nextIndex();
      PermitAndDenyBdds lineBDDs = lineIt.next();
      if (lineBDDs.isZero()) {
        // This line is unmatchable
        answerRows.addUnmatchableLine(aclSpec, lineNum);
      } else if (unmatchedPackets.isZero() || !lineBDDs.getMatchBdd().andSat(unmatchedPackets)) {
        // No unmatched packets in the ACL match this line, so this line is unreachable.
        BlockingProperties blockingProps = findBlockingPropsForLine(lineNum, ipLineToBDDMap);
        answerRows.addBlockedLine(aclSpec, lineNum, blockingProps);
      }
      unmatchedPackets = unmatchedPackets.diff(lineBDDs.getMatchBdd());
    }
  }

  private static void answerAclReachability(
      List<AclSpecs> aclSpecs, FilterLineReachabilityRows answerRows) {
    BDDPacket bddPacket = new BDDPacket();

    for (AclSpecs aclSpec : aclSpecs) {
      answerAclReachabilityLine(aclSpec, bddPacket, answerRows);
    }
  }
}

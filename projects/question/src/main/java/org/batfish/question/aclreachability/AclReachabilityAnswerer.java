package org.batfish.question.aclreachability;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.datamodel.answers.AclReachabilityRows.createMetadata;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.common.Answerer;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBDD;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.CanonicalAcl;
import org.batfish.datamodel.acl.CircularReferenceException;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TypeMatchExprsCollector;
import org.batfish.datamodel.acl.UndefinedReferenceException;
import org.batfish.datamodel.answers.AclReachabilityRows;
import org.batfish.datamodel.answers.AclSpecs;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.specifier.FilterSpecifier;
import org.batfish.specifier.SpecifierContext;

@ParametersAreNonnullByDefault
public class AclReachabilityAnswerer extends Answerer {

  public AclReachabilityAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public TableAnswerElement answer() {
    AclReachabilityQuestion question = (AclReachabilityQuestion) _question;
    AclReachabilityRows answerRows = new AclReachabilityRows();

    SpecifierContext ctxt = _batfish.specifierContext();
    Set<String> specifiedNodes = question.nodeSpecifier().resolve(ctxt);
    FilterSpecifier filterSpecifier = question.filterSpecifier();

    Map<String, Set<IpAccessList>> specifiedAcls =
        CommonUtil.toImmutableMap(
            specifiedNodes, Function.identity(), node -> filterSpecifier.resolve(node, ctxt));

    SortedMap<String, Configuration> configurations = _batfish.loadConfigurations();
    List<AclSpecs> aclSpecs = getAclSpecs(configurations, specifiedAcls, answerRows);
    answerAclReachability(aclSpecs, answerRows);
    TableAnswerElement answer = new TableAnswerElement(createMetadata(question));
    answer.postProcessAnswer(question, answerRows.getRows());
    return answer;
  }

  private static final TypeMatchExprsCollector<PermittedByAcl> permittedByAclCollector =
      new TypeMatchExprsCollector<>(PermittedByAcl.class);

  private static final TypeMatchExprsCollector<MatchSrcInterface> matchSrcInterfaceCollector =
      new TypeMatchExprsCollector<>(MatchSrcInterface.class);

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
    private List<IpAccessListLine> _sanitizedLines;

    public AclNode(IpAccessList acl) {
      _acl = acl;
    }

    public void sanitizeLine(int lineNum, AclLineMatchExpr newMatchExpr) {
      _sanitizedLines = firstNonNull(_sanitizedLines, new ArrayList<>(_acl.getLines()));
      IpAccessListLine originalLine = _sanitizedLines.remove(lineNum);
      _sanitizedLines.add(
          lineNum,
          IpAccessListLine.builder()
              .setMatchCondition(newMatchExpr)
              .setAction(originalLine.getAction())
              .setName(originalLine.getName())
              .build());
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
        sanitizeLine(lineNum, FalseExpr.INSTANCE);
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
      sanitizeLine(lineNum, FalseExpr.INSTANCE);
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
      SortedMap<String, IpAccessList> acls,
      HeaderSpaceSanitizer headerSpaceSanitizer,
      Set<String> nodeInterfaces) {

    // Create ACL node for current ACL
    AclNode node = new AclNode(acl);
    aclNodeMap.put(acl.getName(), node);

    // Go through lines and add dependencies
    int index = 0;
    for (IpAccessListLine line : acl.getLines()) {
      AclLineMatchExpr matchExpr = line.getMatchCondition();
      boolean lineMarkedUnmatchable = false;

      // Find all references to other ACLs and record them
      List<PermittedByAcl> permittedByAclExprs = matchExpr.accept(permittedByAclCollector);
      if (!permittedByAclExprs.isEmpty()) {
        Set<String> referencedAcls =
            permittedByAclExprs
                .stream()
                .map(PermittedByAcl::getAclName)
                .collect(Collectors.toSet());
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
          AclLineMatchExpr sanitizedForIpSpaces = matchExpr.accept(headerSpaceSanitizer);
          if (!matchExpr.equals(sanitizedForIpSpaces)) {
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
        List<MatchSrcInterface> matchSrcInterfaceExprs =
            matchExpr.accept(matchSrcInterfaceCollector);
        Set<String> referencedInterfaces =
            matchSrcInterfaceExprs
                .stream()
                .flatMap(expr -> expr.getSrcInterfaces().stream())
                .collect(Collectors.toSet());
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
                visited
                    .subList(dependencyIndex, visited.size())
                    .stream()
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
      AclReachabilityRows answer) {
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

  private static void answerAclReachability(
      List<AclSpecs> aclSpecs, AclReachabilityRows answerRows) {
    BDDPacket bddPacket = new BDDPacket();
    BDDFactory bddFactory = bddPacket.getFactory();

    for (AclSpecs aclSpec : aclSpecs) {
      BDDSourceManager sourceMgr =
          BDDSourceManager.forInterfaces(bddPacket, aclSpec.acl.getInterfaces());
      IpAccessListToBDD ipAccessListToBDD =
          IpAccessListToBDD.create(
              bddPacket, sourceMgr, aclSpec.acl.getDependencies(), ImmutableMap.of());

      IpAccessList ipAcl = aclSpec.acl.getSanitizedAcl();
      List<IpAccessListLine> lines = ipAcl.getLines();

      List<BDD> ipLineToBDDMap = new ArrayList<>();
      Set<Integer> unreachableButMatchableLineNums = new HashSet<>();

      // compute if each acl line is unmatchable and/or unreachable
      BDD rest = bddFactory.one();
      for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
        IpAccessListLine line = lines.get(lineNum);
        AclLineMatchExpr matchExpr = line.getMatchCondition();
        BDD lineBDD = matchExpr.accept(ipAccessListToBDD);
        ipLineToBDDMap.add(lineBDD);
        if (lineBDD.isZero()) {
          // this line is unmatchable
          answerRows.addUnreachableLine(aclSpec, lineNum, true, new TreeSet<>());
        } else if (rest.isZero() || lineBDD.and(rest).isZero()) {
          unreachableButMatchableLineNums.add(lineNum);
        }
        rest = rest.and(lineBDD.not());
      }

      // compute blocking lines
      for (int lineNum : unreachableButMatchableLineNums) {
        SortedSet<Integer> blockingLineNums = new TreeSet<Integer>();
        BDD restOfLine = ipLineToBDDMap.get(lineNum);

        for (int prevLineNum = 0; prevLineNum < lineNum; prevLineNum++) {
          if (restOfLine.isZero()) {
            break;
          }
          BDD prevBDD = ipLineToBDDMap.get(prevLineNum);

          if (!(prevBDD.and(restOfLine).isZero())) {
            blockingLineNums.add(prevLineNum);
            restOfLine = restOfLine.and(prevBDD.not());
          }
        }
        answerRows.addUnreachableLine(aclSpec, lineNum, false, blockingLineNums);
      }
    }
  }
}

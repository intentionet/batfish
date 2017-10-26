package org.batfish.role;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.NodeRoleSpecifier;
import org.batfish.datamodel.RoleEdge;
import org.batfish.datamodel.Topology;

public class InferRoles implements Callable<NodeRoleSpecifier> {

  private IBatfish _batfish;
  private Map<String,Configuration> _configurations;
  private List<String> _nodes;

  // the node name that is used to infer a regex
  private String _chosenNode;
  // a tokenized version of _chosenNode
  private List<Pair<String,InferRolesCharClass>> _tokens;
  // the regex produced by generalizing from _tokens
  private List<String> _regex;
  // the list of nodes that match _regex
  private List<String> _matchingNodes;

  // the percentage of nodes that must match a regex for it to be used as
  // the base for determining roles
  private static final double REGEX_THRESHOLD = 0.5;
  // the percentage of nodes that must have an alphabetic string at some position,
  // in order for that position to be considered as a possible role
  private static final double GROUP_THRESHOLD = 0.5;
  // the minimum role score for a candidate role regex to be chosen
  private static final double ROLE_THRESHOLD = 0.9;

  private static final String ALPHANUMERIC_REGEX = "\\p{Alnum}+";

  public InferRoles(List<String> nodes, Map<String, Configuration> configurations,
      IBatfish batfish) {
    _nodes = nodes;
    _configurations = configurations;
    _batfish = batfish;
  }

  // Node names are parsed into a sequence of alphanumeric strings and
  // delimiters (strings of non-alphanumeric characters).
  public enum InferRolesCharClass {
    ALPHANUMERIC,
    DELIMITER;

    public static InferRolesCharClass charToCharClass(char c) {
      if (Character.isAlphabetic(c) || Character.isDigit(c)) {
        return InferRolesCharClass.ALPHANUMERIC;
      } else {
        return InferRolesCharClass.DELIMITER;
      }
    }

    public String tokenToRegex(String s) {
      switch (this) {
      case ALPHANUMERIC:
        return ALPHANUMERIC_REGEX;
      case DELIMITER:
        return Pattern.quote(s);
      default:
        throw new BatfishException("this case should be unreachable");
      }
    }
  }

  @Override
  public NodeRoleSpecifier call() {

    NodeRoleSpecifier emptySpecifier = new NodeRoleSpecifier(true);

    int allNodesCount = _nodes.size();

    if (allNodesCount == 0) {
      return emptySpecifier;
    }

    boolean commonRegexFound = inferCommonRegex(_nodes);

    if (!commonRegexFound) {
      return emptySpecifier;
    }


    // find the possible candidates that have a single role group
    List<List<String>> candidateRegexes = possibleRoleGroups();

    if (candidateRegexes.size() == 0) {
      return emptySpecifier;
    }

    Pair<Integer,Double> bestRegexAndScore = findBestRegex(candidateRegexes);

    // select the regex of maximum score, if that score is above threshold
    Optional<NodeRoleSpecifier> optResult = toRoleSpecifierIfAboveThreshold(
        bestRegexAndScore, candidateRegexes);
    if (optResult.isPresent()) {
      return optResult.get();
    }

    // otherwise we attempt to make the best role found so far more specific
    // NOTE: we could try to refine all possible roles we've considered, rather than
    // greedily only refining the best one, if the greedy approach fails often.

    // try adding a second group around any alphanumeric sequence in the regex;
    // now the role is a concatenation of the strings of both groups
    // NOTE: We could also consider just using the leading alphabetic portion of an alphanumeric
    // sequence as the second group, which would result in less specific groups and could
    // be appropriate for some naming schemes.

    candidateRegexes = possibleSecondRoleGroups(
        candidateRegexes.get(bestRegexAndScore.getFirst()));

    if (candidateRegexes.size() == 0) {
      return emptySpecifier;
    }

    optResult = toRoleSpecifierIfAboveThreshold(
        findBestRegex(candidateRegexes), candidateRegexes);
    if (optResult.isPresent()) {
      return optResult.get();
    } else {
      // give up and don't infer any roles if the best one is still below threshold
      return emptySpecifier;
    }


  }

  // try to identify a regex that most node names match
  private boolean inferCommonRegex(List<String> nodes) {
    for (int attempts = 0 ; attempts < 10; attempts++) {
      // pick a random node name, in order to find one with a common pattern
      _chosenNode = nodes.get(new Random().nextInt(nodes.size()));
      _tokens = tokenizeName(_chosenNode);
      _regex = _tokens
          .stream()
          .map((p) -> p.getSecond().tokenToRegex(p.getFirst()))
          .collect(Collectors.toList());
      Pattern p = Pattern.compile(String.join("", _regex));
      _matchingNodes = nodes
          .stream()
          .filter((node) -> p.matcher(node).matches())
          .collect(Collectors.toList());
      // keep this regex if it matches more than half of the node names; otherwise try again
      if ((double) _matchingNodes.size() / nodes.size() >= REGEX_THRESHOLD) {
        return true;
      }
    }
    return false;
  }

  // a very simple lexer that tokenizes a name into a sequence of
  // alphanumeric and non-alphanumeric strings
  private List<Pair<String,InferRolesCharClass>> tokenizeName(String name) {
    List<Pair<String,InferRolesCharClass>> pattern = new ArrayList<>();
    char c = name.charAt(0);
    InferRolesCharClass cc = InferRolesCharClass.charToCharClass(c);
    StringBuffer curr = new StringBuffer();
    curr.append(c);
    for (int i = 1; i < name.length(); i++) {
      c = name.charAt(i);
      InferRolesCharClass currClass = InferRolesCharClass.charToCharClass(c);
      if (currClass == cc) {
        curr.append(c);
      } else {
        pattern.add(new Pair<>(new String(curr), cc));
        curr = new StringBuffer();
        cc = currClass;
        curr.append(c);
      }
    }
    pattern.add(new Pair<>(new String(curr), cc));
    return pattern;
  }

  private static String regexTokensToRegex(List<String> tokens) {
    return String.join("", tokens);
  }

  // If for enough node names matching the identified regex,
  // a particular alphanumeric token starts with one or more alphabetic characters,
  // the string of initial alphabetic characters is considered a candidate for the role name.
  // This method returns all such candidates, each represented as a regex
  // with a single group indicating the role name; the regex is returned as a sequence
  // of tokens.
  private List<List<String>> possibleRoleGroups() {
    int numAll = _matchingNodes.size();
    List<List<String>> candidateRegexes = new ArrayList<>();
    for (int i = 0; i < _tokens.size(); i++) {
      if (_tokens.get(i).getSecond() == InferRolesCharClass.DELIMITER) {
        continue;
      }
      List<String> regexCopy = new ArrayList<>(_regex);
      regexCopy.set(i, "(\\p{Alpha}+)\\p{Alnum}*");
      Pattern newp = Pattern.compile(regexTokensToRegex(regexCopy));
      int numMatches = 0;
      for (String node : _matchingNodes) {
        Matcher newm = newp.matcher(node);
        if (newm.matches()) {
          numMatches++;
        }
      }
      if ((double) numMatches / numAll >= GROUP_THRESHOLD) {
        candidateRegexes.add(regexCopy);
      }
    }
    return candidateRegexes;
  }

  private List<List<String>> possibleSecondRoleGroups(List<String> tokens) {
    List<List<String>> candidateRegexes = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      if (!tokens.get(i).equals(ALPHANUMERIC_REGEX)) {
        continue;
      }
      List<String> regexCopy = new ArrayList<>(tokens);
      regexCopy.set(i, "(" + ALPHANUMERIC_REGEX + ")");
      candidateRegexes.add(regexCopy);
    }
    return candidateRegexes;
  }

  private double computeRoleScore(String regex) {

    SortedMap<String, SortedSet<String>> nodeRolesMap =
        regexToRoleSpecifier(regex).createNodeRolesMap(new TreeSet<>(_nodes));

    Topology topology = _batfish.computeTopology(_configurations);

    // produce a role-level topology and the list of nodes in each edge's source role
    // that have an edge to some node in the edge's target role
    SortedMap<RoleEdge, SortedSet<String>> roleEdges = new TreeMap<>();
    for (Edge e : topology.getEdges()) {
      String n1 = e.getNode1();
      String n2 = e.getNode2();
      SortedSet<String> roles1 = nodeRolesMap.get(n1);
      SortedSet<String> roles2 = nodeRolesMap.get(n2);
      if (roles1 != null && roles2 != null && roles1.size() == 1 && roles2.size() == 1) {
        String role1 = roles1.first();
        String role2 = roles2.first();
        // ignore self-edges
        if (role1.equals(role2)) {
          continue;
        }
        RoleEdge redge = new RoleEdge(role1, role2);
        SortedSet<String> roleEdgeNodes = roleEdges.getOrDefault(
            redge, new TreeSet<>()
        );
        roleEdgeNodes.add(n1);
        roleEdges.put(redge, roleEdgeNodes);
      }
    }

    int numEdges = roleEdges.size();
    if (numEdges == 0) {
      return 0.0;
    }

    // compute the "support" of each edge in the role-level topology:
    // the percentage of nodes playing the source role that have an edge
    // to a node in the target role.
    // the score of this regex is then the average support across all role edges
    SortedMap<String, SortedSet<String>> roleNodesMap =
        regexToRoleSpecifier(regex).createRoleNodesMap(_configurations.keySet());

    double supportSum = 0.0;
    for (Map.Entry<RoleEdge, SortedSet<String>> roleEdgeCount : roleEdges.entrySet()) {
      RoleEdge redge = roleEdgeCount.getKey();
      int count = roleEdgeCount.getValue().size();
      supportSum += (double) count / roleNodesMap.get(redge.getRole1()).size();
    }
    return supportSum / numEdges;
  }


  // the list of candidates must have at least one element
  private Pair<Integer,Double> findBestRegex(final List<List<String>> candidates) {
    // choose the candidate role regex with the maximal "role score"
    return IntStream.range(0, candidates.size())
        .mapToObj(i -> new Pair<>(i, computeRoleScore(regexTokensToRegex(candidates.get(i)))))
        .max(Comparator.comparingDouble(Pair::getSecond))
        .orElseThrow(() -> new BatfishException("this exception should not be reachable"));
  }

  // the list of candidates must have at least one element
  private Optional<NodeRoleSpecifier> toRoleSpecifierIfAboveThreshold(
      Pair<Integer,Double> bestRegexAndScore, List<List<String>> candidates) {
    List<String> bestRegexTokens = candidates.get(bestRegexAndScore.getFirst());
    String bestRegex = regexTokensToRegex(bestRegexTokens);
    // if the score is high enough, we're done
    if (bestRegexAndScore.getSecond() >= ROLE_THRESHOLD) {
      return Optional.of(regexToRoleSpecifier(bestRegex));
    } else {
      return Optional.empty();
    }
  }

  NodeRoleSpecifier regexToRoleSpecifier(String regex) {
    List<String> regexes = new ArrayList<>();
    regexes.add(regex);
    NodeRoleSpecifier result = new NodeRoleSpecifier(true);
    result.setRoleRegexes(regexes);
    return result;
  }

}

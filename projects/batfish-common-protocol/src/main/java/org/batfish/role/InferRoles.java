package org.batfish.role;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.RoleEdge;
import org.batfish.datamodel.Topology;
import org.batfish.role.NodeRoleDimension.Type;

public final class InferRoles {

  private final Collection<String> _nodes;
  private final Topology _topology;

  // a tokenized version of _chosenNode
  private List<Pair<String, Token>> _tokens;
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

  private static final String ALPHABETIC_REGEX = "\\p{Alpha}";
  private static final String ALPHANUMERIC_REGEX = "\\p{Alnum}";
  private static final String DIGIT_REGEX = "\\p{Digit}";

  public InferRoles(Collection<String> nodes, Topology topology) {
    _nodes = ImmutableSortedSet.copyOf(nodes);
    _topology = topology;
  }

  // A node's name is first parsed into a sequence of simple "pretokens",
  // and then these pretokens are combined to form tokens.
  public enum PreToken {
    ALPHA_PLUS, // sequence of alphabetic characters
    DELIMITER, // sequence of non-alphanumeric characters
    DIGIT_PLUS; // sequence of digits

    public static PreToken charToPreToken(char c) {
      if (Character.isAlphabetic(c)) {
        return ALPHA_PLUS;
      } else if (Character.isDigit(c)) {
        return DIGIT_PLUS;
      } else {
        return DELIMITER;
      }
    }
  }

  public enum Token {
    ALPHA_PLUS_DIGIT_PLUS,
    ALNUM_PLUS,
    DELIMITER,
    DIGIT_PLUS;

    public String tokenToRegex(String s) {
      switch (this) {
        case ALPHA_PLUS_DIGIT_PLUS:
          return plus(ALPHABETIC_REGEX) + plus(DIGIT_REGEX);
        case ALNUM_PLUS:
          return ALPHABETIC_REGEX + star(ALPHANUMERIC_REGEX);
        case DELIMITER:
          return Pattern.quote(s);
        case DIGIT_PLUS:
          return plus(DIGIT_REGEX);
        default:
          throw new BatfishException("this case should be unreachable");
      }
    }
  }

  // some useful operations on regexes
  private static String plus(String s) {
    return s + "+";
  }

  private static String star(String s) {
    return s + "*";
  }

  private static String group(String s) {
    return "(" + s + ")";
  }

  public SortedSet<NodeRoleDimension> inferRoles() {

    if (_nodes.isEmpty()) {
      return ImmutableSortedSet.of();
    }

    boolean commonRegexFound = inferCommonRegex(_nodes);

    if (!commonRegexFound) {
      return ImmutableSortedSet.of();
    }

    // find the possible candidates that have a single role group
    List<List<String>> candidateRegexes = possibleRoleGroups();

    if (candidateRegexes.isEmpty()) {
      return ImmutableSortedSet.of();
    }

    // if there is at least one role group, let's fine the best role dimension according
    // to our metric, and also keep all the others.

    SortedSet<NodeRoleDimension> allDims;

    Pair<Integer, Double> bestRegexAndScore = findBestRegex(candidateRegexes);

    // select the regex of maximum score, if that score is above threshold
    Optional<NodeRoleDimension> optResult =
        toPrimaryNodeRoleDimensionIfAboveThreshold(bestRegexAndScore, candidateRegexes);
    boolean bestIsAboveThreshold = optResult.isPresent();
    if (bestIsAboveThreshold) {
      // remove the dimension that has been selected as the primary inferred role dimension,
      // so we do not duplicate it when creating the other role dimensions
      candidateRegexes.remove(bestRegexAndScore.getFirst().intValue());
    }

    // record the set of role "dimensions" for each node, which is a part of its name
    // that may indicate a useful grouping of nodes
    // (e.g., the node's function, location, device type, etc.)
    allDims = createRoleDimensions(candidateRegexes);

    if (bestIsAboveThreshold) {
      allDims.add(optResult.get());
    } else {

      // if no single role group is above threshold, we attempt to make the best role found so far
      // more specific.
      // NOTE: we could try to refine all possible roles we've considered, rather than
      // greedily only refining the best one, if the greedy approach fails often.

      // try adding a second group around any alphanumeric sequence in the regex;
      // now the role is a concatenation of the strings of both groups
      // NOTE: We could also consider just using the leading alphabetic portion of an alphanumeric
      // sequence as the second group, which would result in less specific groups and could
      // be appropriate for some naming schemes.

      candidateRegexes =
          possibleSecondRoleGroups(candidateRegexes.get(bestRegexAndScore.getFirst()));

      if (candidateRegexes.size() != 0) {
        // determine the best one according to our metric, even if it's below threshold
        allDims.add(
            toNodeRoleDimension(
                findBestRegex(candidateRegexes),
                candidateRegexes,
                NodeRoleDimension.AUTO_DIMENSION_PRIMARY));
      }
    }

    return allDims;
  }

  // try to identify a regex that most node names match
  private boolean inferCommonRegex(Collection<String> nodes) {
    for (int attempts = 0; attempts < 10; attempts++) {
      // pick a random node name, in order to find one with a common pattern
      // the node name that is used to infer a regex
      String chosenNode = Iterables.get(nodes, new Random().nextInt(nodes.size()));
      _tokens = tokenizeName(chosenNode);
      _regex =
          _tokens
              .stream()
              .map((p) -> p.getSecond().tokenToRegex(p.getFirst()))
              .collect(Collectors.toList());
      Pattern p = Pattern.compile(String.join("", _regex));
      _matchingNodes =
          nodes.stream().filter((node) -> p.matcher(node).matches()).collect(Collectors.toList());
      // keep this regex if it matches a sufficient fraction of node names; otherwise try again
      if ((double) _matchingNodes.size() / nodes.size() >= REGEX_THRESHOLD) {
        return true;
      }
    }
    return false;
  }

  // If delimiters (non-alphanumeric characters) are being used in the node names, we use them
  // to separate the different tokens.
  private static List<Pair<String, Token>> preTokensToDelimitedTokens(
      List<Pair<String, PreToken>> pretokens) {
    List<Pair<String, Token>> tokens = new ArrayList<>();
    int size = pretokens.size();
    for (int i = 0; i < size; i++) {
      StringBuilder chars = new StringBuilder(pretokens.get(i).getFirst());
      PreToken pt = pretokens.get(i).getSecond();
      switch (pt) {
        case ALPHA_PLUS:
          // combine everything up to the next delimiter into a single alphanumeric token
          int next = i + 1;
          while (next < size && pretokens.get(next).getSecond() != PreToken.DELIMITER) {
            chars.append(pretokens.get(next).getFirst());
            next++;
          }
          i = next - 1;
          tokens.add(new Pair<>(chars.toString(), Token.ALNUM_PLUS));
          break;
        case DELIMITER:
          tokens.add(new Pair<>(chars.toString(), Token.DELIMITER));
          break;
        case DIGIT_PLUS:
          tokens.add(new Pair<>(chars.toString(), Token.DIGIT_PLUS));
          break;
        default:
          throw new BatfishException("Unknown pretoken " + pt);
      }
    }
    return tokens;
  }

  // If delimiters (non-alphanumeric characters) are not being used in the node names, we treat
  // each consecutive string matching alpha+digit+ as a distinct token.
  private static List<Pair<String, Token>> preTokensToUndelimitedTokens(
      List<Pair<String, PreToken>> pretokens) {
    List<Pair<String, Token>> tokens = new ArrayList<>();
    int size = pretokens.size();
    for (int i = 0; i < size; i++) {
      String chars = pretokens.get(i).getFirst();
      PreToken pt = pretokens.get(i).getSecond();
      switch (pt) {
        case ALPHA_PLUS:
          int next = i + 1;
          if (next >= size) {
            // generalize a final alphabetic sequence to allow alphanumeric ones
            tokens.add(new Pair<>(chars, Token.ALNUM_PLUS));
          } else {
            // the next token must be DIGIT_PLUS since we know there are no delimiters
            int third = i + 2;
            String bothChars = chars + pretokens.get(next).getFirst();
            if (third >= size) {
              // generalize a final ALPHA+DIGIT+ to ALNUM+
              tokens.add(new Pair<>(bothChars, Token.ALNUM_PLUS));
            } else {
              // keep ALPHA+DIGIT+ as is, to separate it from the subsequent
              // alphabetic sequence
              tokens.add(new Pair<>(bothChars, Token.ALPHA_PLUS_DIGIT_PLUS));
            }
            i++;
          }
          break;
        case DIGIT_PLUS:
          tokens.add(new Pair<>(chars, Token.DIGIT_PLUS));
          break;
        default:
          throw new BatfishException("Unexpected pretoken " + pt);
      }
    }
    return tokens;
  }

  private static List<Pair<String, Token>> tokenizeName(String name) {
    List<Pair<String, PreToken>> pretokens = pretokenizeName(name);
    if (pretokens.stream().anyMatch((p) -> p.getSecond() == PreToken.DELIMITER)) {
      return preTokensToDelimitedTokens(pretokens);
    } else {
      return preTokensToUndelimitedTokens(pretokens);
    }
  }

  // tokenizes a name into a sequence of pretokens defined by the PreToken enum above
  private static List<Pair<String, PreToken>> pretokenizeName(String name) {
    List<Pair<String, PreToken>> pattern = new ArrayList<>();
    char c = name.charAt(0);
    PreToken currPT = PreToken.charToPreToken(c);
    StringBuffer curr = new StringBuffer();
    curr.append(c);
    for (int i = 1; i < name.length(); i++) {
      c = name.charAt(i);
      PreToken newPT = PreToken.charToPreToken(c);
      if (newPT != currPT) {
        pattern.add(new Pair<>(new String(curr), currPT));
        curr = new StringBuffer();
        currPT = newPT;
      }
      curr.append(c);
    }
    pattern.add(new Pair<>(new String(curr), currPT));
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
      switch (_tokens.get(i).getSecond()) {
        case ALNUM_PLUS:
          List<String> regexCopy = new ArrayList<>(_regex);
          regexCopy.set(i, group(plus(ALPHABETIC_REGEX)) + star(ALPHANUMERIC_REGEX));
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
          break;
        case ALPHA_PLUS_DIGIT_PLUS:
          List<String> regexCopy2 = new ArrayList<>(_regex);
          regexCopy2.set(i, group(plus(ALPHABETIC_REGEX)) + plus(DIGIT_REGEX));
          candidateRegexes.add(regexCopy2);
          break;
        default:
          break;
      }
    }
    return candidateRegexes;
  }

  private SortedSet<NodeRoleDimension> createRoleDimensions(List<List<String>> regexes) {

    SortedSet<NodeRoleDimension> result = new TreeSet<>();
    for (int i = 0; i < regexes.size(); i++) {
      String dimName = NodeRoleDimension.AUTO_DIMENSION_PREFIX + (i + 1);
      String regex = regexTokensToRegex(regexes.get(i));
      result.add(regexToNodeRoleDimension(regex, dimName));
    }
    return result;
  }

  private static List<List<String>> possibleSecondRoleGroups(List<String> tokens) {
    List<List<String>> candidateRegexes = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);
      // skip the token if it's a delimiter or the primary group
      if (token.startsWith("\\Q") || token.startsWith("(")) {
        continue;
      }
      List<String> regexCopy = new ArrayList<>(tokens);
      regexCopy.set(i, group(token));
      candidateRegexes.add(regexCopy);
    }
    return candidateRegexes;
  }

  private double computeRoleScore(String regex) {

    SortedMap<String, SortedSet<String>> nodeRolesMap = regexToNodeRolesMap(regex, _nodes);

    // produce a role-level topology and the list of nodes in each edge's source role
    // that have an edge to some node in the edge's target role
    SortedMap<RoleEdge, SortedSet<String>> roleEdges = new TreeMap<>();
    for (Edge e : _topology.getEdges()) {
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
        SortedSet<String> roleEdgeNodes = roleEdges.getOrDefault(redge, new TreeSet<>());
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
    SortedMap<String, SortedSet<String>> roleNodesMap = regexToRoleNodesMap(regex, _nodes);

    double supportSum = 0.0;
    for (Map.Entry<RoleEdge, SortedSet<String>> roleEdgeCount : roleEdges.entrySet()) {
      RoleEdge redge = roleEdgeCount.getKey();
      int count = roleEdgeCount.getValue().size();
      supportSum += (double) count / roleNodesMap.get(redge.getRole1()).size();
    }
    return supportSum / numEdges;
  }

  private static SortedMap<String, SortedSet<String>> regexToRoleNodesMap(
      String regex, Collection<String> nodes) {
    SortedMap<String, SortedSet<String>> roleNodesMap = new TreeMap<>();
    Pattern pattern;
    try {
      pattern = Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      throw new BatfishException("Supplied regex is not a valid Java regex: \"" + regex + "\"", e);
    }
    for (String node : nodes) {
      Matcher matcher = pattern.matcher(node);
      int numGroups = matcher.groupCount();
      if (matcher.matches()) {
        try {
          List<String> roleParts =
              IntStream.range(1, numGroups + 1)
                  .mapToObj(matcher::group)
                  .collect(Collectors.toList());
          String role = String.join("-", roleParts);

          SortedSet<String> currNodes = roleNodesMap.computeIfAbsent(role, k -> new TreeSet<>());
          currNodes.add(node);
        } catch (IndexOutOfBoundsException e) {
          throw new BatfishException(
              "Supplied regex does not contain "
                  + numGroups
                  + "groups: \""
                  + pattern.pattern()
                  + "\"",
              e);
        }
      }
    }
    return roleNodesMap;
  }

  // return a map from each node name to the set of roles that it plays
  private static SortedMap<String, SortedSet<String>> regexToNodeRolesMap(
      String regex, Collection<String> allNodes) {

    SortedMap<String, SortedSet<String>> roleNodesMap = regexToRoleNodesMap(regex, allNodes);

    // invert the map from roles to nodes, to create a map from nodes to roles
    SortedMap<String, SortedSet<String>> nodeRolesMap = new TreeMap<>();

    roleNodesMap.forEach(
        (role, nodes) -> {
          for (String node : nodes) {
            SortedSet<String> nodeRoles = nodeRolesMap.computeIfAbsent(node, k -> new TreeSet<>());
            nodeRoles.add(role);
          }
        });

    return nodeRolesMap;
  }

  // the list of candidates must have at least one element
  private Pair<Integer, Double> findBestRegex(final List<List<String>> candidates) {
    // choose the candidate role regex with the maximal "role score"
    return IntStream.range(0, candidates.size())
        .mapToObj(i -> new Pair<>(i, computeRoleScore(regexTokensToRegex(candidates.get(i)))))
        .max(Comparator.comparingDouble(Pair::getSecond))
        .orElseThrow(() -> new BatfishException("this exception should not be reachable"));
  }

  // the list of candidates must have at least one element
  private Optional<NodeRoleDimension> toPrimaryNodeRoleDimensionIfAboveThreshold(
      Pair<Integer, Double> bestRegexAndScore, List<List<String>> candidates) {
    if (bestRegexAndScore.getSecond() >= ROLE_THRESHOLD) {
      NodeRoleDimension bestNRD =
          toNodeRoleDimension(
              bestRegexAndScore, candidates, NodeRoleDimension.AUTO_DIMENSION_PRIMARY);
      return Optional.of(bestNRD);
    } else {
      return Optional.empty();
    }
  }

  // the list of candidates must have at least one element
  private NodeRoleDimension toNodeRoleDimension(
      Pair<Integer, Double> bestRegexAndScore, List<List<String>> candidates, String dimName) {
    List<String> bestRegexTokens = candidates.get(bestRegexAndScore.getFirst());
    String bestRegex = regexTokensToRegex(bestRegexTokens);
    return regexToNodeRoleDimension(bestRegex, dimName);
  }

  // converts a regex containing one or more groups indicating roles into a NodeRoleDimension
  private NodeRoleDimension regexToNodeRoleDimension(String regex, String dimName) {
    SortedSet<NodeRole> inferredRoles = new TreeSet<>();

    Set<String> roles = regexToRoleNodesMap(regex, _nodes).keySet();
    for (String role : roles) {
      inferredRoles.add(new NodeRole(role, specializeRegexForRole(role, regex)));
    }

    return new NodeRoleDimension(
        dimName, inferredRoles, Type.AUTO, Collections.singletonList(regex));
  }

  // regex is a regular expression that uses one or more groups to indicate the role
  // role is a particular choice of values for those groups, delimited by -
  // the result is a version of regex specialized to these particular values
  private static String specializeRegexForRole(String role, String regex) {
    String[] roleParts = role.split("-");
    String result = regex;
    for (String rolePart : roleParts) {
      result = result.replaceFirst("\\([^\\)]*\\)", rolePart);
    }
    return result;
  }
}

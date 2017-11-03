package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
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

public class NodeRoleSpecifier {

  private static final String PROP_ROLE_MAP = "roleMap";

  private static final String PROP_ROLE_REGEXES = "roleRegexes";

  private static final String PROP_INFERRED = "inferred";

  // a map from roles to the set of nodes that have that role
  private SortedMap<String, SortedSet<String>> _roleMap;

  // an ordered list of regexes used to identify roles from node names.
  // each regex in regexes has at least one group in it that locates the role name
  // within a node name.
  // there are multiple regexes to handle node names that have different formats.
  private List<String> _roleRegexes;

  // indicates whether this NodeRoleSpecifier was automatically inferred.
  private boolean _inferred;

  public NodeRoleSpecifier() {
    _roleMap = new TreeMap<>();
    _roleRegexes = new ArrayList<>();
  }

  public NodeRoleSpecifier(boolean inferred) {
    this();
    _inferred = inferred;
  }

  private void addToRoleNodesMap(
      SortedMap<String, SortedSet<String>> roleNodesMap, Set<String> nodes) {
    List<Pattern> patList = new ArrayList<>();
    for (String regex : _roleRegexes) {
      try {
        patList.add(Pattern.compile(regex));
      } catch (PatternSyntaxException e) {
        throw new BatfishException(
            "Supplied regex is not a valid Java regex: \"" + regex + "\"", e);
      }
    }
    for (String node : nodes) {
      for (Pattern pattern : patList) {
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
                "Supplied regex does not contain a group: \"" + pattern.pattern() + "\"", e);
          }
          break;
        }
      }
    }
  }

  // return a map from each role name to the set of nodes that play that role
  public SortedMap<String, SortedSet<String>> createRoleNodesMap(Set<String> allNodes) {
    SortedMap<String, SortedSet<String>> roleNodesMap = new TreeMap<>();

    roleNodesMap.putAll(_roleMap);
    addToRoleNodesMap(roleNodesMap, allNodes);

    return roleNodesMap;
  }

  // return a map from each node name to the set of roles that it plays
  public SortedMap<String, SortedSet<String>> createNodeRolesMap(Set<String> allNodes) {

    SortedMap<String, SortedSet<String>> roleNodesMap = createRoleNodesMap(allNodes);

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

  @JsonProperty(PROP_ROLE_MAP)
  public SortedMap<String, SortedSet<String>> getRoleMap() {
    return _roleMap;
  }

  @JsonProperty(PROP_ROLE_REGEXES)
  public List<String> getRoleRegexes() {
    return _roleRegexes;
  }

  @JsonProperty(PROP_INFERRED)
  public boolean getInferred() {
    return _inferred;
  }

  @JsonProperty(PROP_ROLE_MAP)
  public void setRoleMap(SortedMap<String, SortedSet<String>> roleMap) {
    _roleMap = roleMap;
  }

  @JsonProperty(PROP_ROLE_REGEXES)
  public void setRoleRegexes(List<String> roleRegexes) {
    _roleRegexes = roleRegexes;
  }

  // We do not make the setter for _inferred a JSON property;
  // it will only be set internally.
  public void setInferred(boolean inferred) {
    _inferred = inferred;
  }
}

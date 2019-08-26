package org.batfish.role;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/** Objects of this class represent a roles specification for the network. */
@ParametersAreNonnullByDefault
public class NodeRolesSpecification {

  private static final String PROP_ROLE_DIMENSION_NAMES = "roleDimensionNames";
  private static final String PROP_ROLE_MAPPINGS = "roleMappings";

  // the role dimensions used by this network, ordered for hierarchical
  // visualization, i.e., if D1 comes before D2 then nodes will be
  // first partitioned via dimension D1 and then D2.
  @Nonnull private List<String> _roleDimensionNames;
  // the role mappings used to determine the role dimensions of each
  // node and the corresponding role names for each dimension
  @Nonnull private List<RoleMapping> _roleMappings;

  @JsonCreator
  public NodeRolesSpecification(
      @JsonProperty(PROP_ROLE_DIMENSION_NAMES) List<String> roleDimensionNames,
      @JsonProperty(PROP_ROLE_MAPPINGS) List<RoleMapping> roleMappings) {
    _roleDimensionNames = firstNonNull(roleDimensionNames, ImmutableList.of());
    _roleMappings = firstNonNull(roleMappings, ImmutableList.of());
  }

  @JsonProperty(PROP_ROLE_DIMENSION_NAMES)
  @Nonnull
  public List<String> getRoleDimensionNames() {
    return _roleDimensionNames;
  }

  @JsonProperty(PROP_ROLE_MAPPINGS)
  @Nonnull
  public List<RoleMapping> getRoleMappings() {
    return _roleMappings;
  }

  @Nonnull
  public NodeRolesData toNodeRolesData() {
    // convert the list of role mappings to a list of role dimension mappings per
    // dimension name
    Map<String, List<RoleDimensionMapping>> rdMaps = new TreeMap<>();
    for (RoleMapping rmap : _roleMappings) {
      String regex = rmap.getRegex();
      Map<String, List<Integer>> rdGroups = rmap.getRoleDimensionsGroups();
      Map<String, Map<String, String>> canonicalRoleNames = rmap.getCanonicalRoleNames();
      for (Map.Entry<String, List<Integer>> entry : rdGroups.entrySet()) {
        String dim = entry.getKey();
        List<Integer> groups = entry.getValue();
        RoleDimensionMapping rdmap =
            new RoleDimensionMapping(
                regex, groups, canonicalRoleNames.getOrDefault(dim, ImmutableMap.of()));
        List<RoleDimensionMapping> dimMaps = rdMaps.computeIfAbsent(dim, k -> new LinkedList<>());
        dimMaps.add(rdmap);
      }
    }
    // now build the NodeRoleDimensions, one per dimension name
    SortedSet<NodeRoleDimension> nodeRoleDimensions = new TreeSet<>();
    for (Map.Entry<String, List<RoleDimensionMapping>> entry : rdMaps.entrySet()) {
      String dim = entry.getKey();
      List<RoleDimensionMapping> rdmaps = entry.getValue();
      nodeRoleDimensions.add(
          NodeRoleDimension.builder(dim).setRoleDimensionMappings(rdmaps).build());
    }
    return NodeRolesData.builder().setRoleDimensions(nodeRoleDimensions).build();
  }
}

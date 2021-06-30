package org.batfish.common.topology;

import static org.batfish.common.topology.TopologyUtil.isBorderToIspEdge;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.collections.NodeInterfacePair;

/** Something that can tell whether two different L3 interfaces are in the same broadcast domain. */
@ParametersAreNonnullByDefault
public final class LegacyL3Adjacencies implements L3Adjacencies {

  public LegacyL3Adjacencies(
      @Nonnull Layer1Topology rawLayer1Topology,
      @Nonnull Layer1Topology layer1LogicalTopology,
      @Nonnull Layer2Topology layer2Topology,
      @Nonnull Map<String, Configuration> configurations) {
    _nodesWithL1Topology =
        Stream.concat(
                rawLayer1Topology.getGraph().edges().stream(),
                layer1LogicalTopology.getGraph().edges().stream())
            .filter(
                // Ignore border-to-ISP edges when computing the set of nodes for which users
                // provided L1 topology. Batfish adds these edges during ISP modeling, and not
                // excluding them impact L3 edge inference for border.
                l1Edge -> !isBorderToIspEdge(l1Edge, configurations))
            .map(l1Edge -> l1Edge.getNode1().getHostname())
            .collect(ImmutableSet.toImmutableSet());
    _layer2Topology = layer2Topology;

    _physicalPointToPoint = computePhysicalPointToPoint(layer1LogicalTopology);
    _l3ToPhysical = computeL3ToPhysical(configurations);
    _physicalToL3 = computePhysicalToL3(_l3ToPhysical);
  }

  @Override
  public boolean inSameBroadcastDomain(NodeInterfacePair i1, NodeInterfacePair i2) {
    // true if either node is not in tail of edge in layer-1, or if vertices are in
    // same broadcast domain
    return !_nodesWithL1Topology.contains(i1.getHostname())
        || !_nodesWithL1Topology.contains(i2.getHostname())
        || _layer2Topology.inSameBroadcastDomain(i1, i2);
  }

  @Override
  public @Nonnull Optional<NodeInterfacePair> pairedPointToPointL3Interface(
      NodeInterfacePair iface) {
    NodeInterfacePair physical = _l3ToPhysical.get(iface);
    if (physical == null) {
      return Optional.empty();
    }
    NodeInterfacePair neighbor = _physicalPointToPoint.get(physical);
    if (neighbor == null) {
      return Optional.empty();
    }
    @Nullable NodeInterfacePair ret = null;
    for (NodeInterfacePair l3 : _physicalToL3.get(neighbor)) {
      if (l3 == null || !_layer2Topology.inSameBroadcastDomain(iface, l3)) {
        continue;
      } else if (ret != null) {
        // Two p2p neighbors, not unique. Should not happen.
        return Optional.empty();
      }
      ret = l3;
    }
    return Optional.ofNullable(ret);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof LegacyL3Adjacencies)) {
      return false;
    }
    LegacyL3Adjacencies that = (LegacyL3Adjacencies) o;
    return _layer2Topology.equals(that._layer2Topology)
        && _nodesWithL1Topology.equals(that._nodesWithL1Topology)
        && _physicalPointToPoint.equals(that._physicalPointToPoint)
        && _l3ToPhysical.equals(that._l3ToPhysical)
        && _physicalToL3.equals(that._physicalToL3);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _nodesWithL1Topology, _layer2Topology, _physicalPointToPoint, _l3ToPhysical, _physicalToL3);
  }

  private static Map<NodeInterfacePair, NodeInterfacePair> computePhysicalPointToPoint(
      Layer1Topology layer1LogicalTopology) {
    Map<NodeInterfacePair, NodeInterfacePair> pointToPoint = new HashMap<>();
    for (Layer1Node n : layer1LogicalTopology.getGraph().nodes()) {
      Set<Layer1Node> neighbors = layer1LogicalTopology.getGraph().adjacentNodes(n);
      if (neighbors.size() != 1) {
        // Not unique, so no point-to-point link.
        continue;
      }
      Layer1Node neighbor = Iterables.getOnlyElement(neighbors);
      Set<Layer1Node> neighborNeighbors = layer1LogicalTopology.getGraph().adjacentNodes(neighbor);
      if (!neighborNeighbors.isEmpty() && !neighborNeighbors.equals(ImmutableSet.of(n))) {
        // Neighbor has either too many neighbors or the wrong neighbor; empty is okay though.
        // (Keep in mind: topology is directed, and may be asymmetric.)
        continue;
      }
      NodeInterfacePair nip = NodeInterfacePair.of(n.getHostname(), n.getInterfaceName());
      NodeInterfacePair neighborNip =
          NodeInterfacePair.of(neighbor.getHostname(), neighbor.getInterfaceName());
      @Nullable NodeInterfacePair previous = pointToPoint.put(nip, neighborNip);
      assert previous == null || previous.equals(neighborNip); // sanity
      previous = pointToPoint.put(neighborNip, nip);
      assert previous == null || previous.equals(nip); // sanity
    }
    return ImmutableMap.copyOf(pointToPoint);
  }

  private static Map<NodeInterfacePair, NodeInterfacePair> computeL3ToPhysical(
      Map<String, Configuration> configs) {
    ImmutableMap.Builder<NodeInterfacePair, NodeInterfacePair> ret = ImmutableMap.builder();
    for (Configuration c : configs.values()) {
      for (Interface i : c.getAllInterfaces().values()) {
        if (!i.getActive() || i.getSwitchport() || i.getAllAddresses().isEmpty()) {
          continue;
        }
        NodeInterfacePair l3 = NodeInterfacePair.of(i);
        if (i.getInterfaceType() == InterfaceType.PHYSICAL
            || i.getInterfaceType().equals(InterfaceType.AGGREGATED)
            || i.getInterfaceType().equals(InterfaceType.REDUNDANT)) {
          ret.put(l3, l3);
        } else {
          i.getDependencies().stream()
              .filter(d -> d.getType() == DependencyType.BIND)
              .findFirst()
              .map(Dependency::getInterfaceName)
              .map(n -> c.getAllInterfaces().get(n))
              .map(NodeInterfacePair::of)
              .ifPresent(parent -> ret.put(l3, parent));
        }
      }
    }
    return ret.build();
  }

  private static @Nonnull Multimap<NodeInterfacePair, NodeInterfacePair> computePhysicalToL3(
      Map<NodeInterfacePair, NodeInterfacePair> l3ToPhysical) {
    ImmutableMultimap.Builder<NodeInterfacePair, NodeInterfacePair> physicalToL3 =
        ImmutableMultimap.builder();
    l3ToPhysical.forEach((l3, phys) -> physicalToL3.put(phys, l3));
    return physicalToL3.build();
  }

  private final Set<String> _nodesWithL1Topology;
  private final Layer2Topology _layer2Topology;
  private final Map<NodeInterfacePair, NodeInterfacePair> _physicalPointToPoint;
  private final Map<NodeInterfacePair, NodeInterfacePair> _l3ToPhysical;
  private final Multimap<NodeInterfacePair, NodeInterfacePair> _physicalToL3;
}

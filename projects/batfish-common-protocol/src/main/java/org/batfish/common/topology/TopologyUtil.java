package org.batfish.common.topology;

import static org.batfish.common.util.CommonUtil.forEachWithIndex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.Graphs;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;

public final class TopologyUtil {

  private static void addLayer2TrunkEdges(
      Interface i1,
      Interface i2,
      ImmutableSet.Builder<Layer2Edge> edges,
      Layer1Node node1,
      Layer1Node node2) {
    if (i1.getSwitchportMode() == SwitchportMode.TRUNK
        && i2.getSwitchportMode() == SwitchportMode.TRUNK) {
      i1.getAllowedVlans()
          .stream()
          .flatMapToInt(SubRange::asStream)
          .filter(
              i1AllowedVlan ->
                  i2.getAllowedVlans().stream().anyMatch(sr -> sr.includes(i1AllowedVlan)))
          .forEach(
              allowedVlan ->
                  edges.add(new Layer2Edge(node1, allowedVlan, node2, allowedVlan, allowedVlan)));
      edges.add(new Layer2Edge(node1, i1.getNativeVlan(), node2, i2.getNativeVlan(), null));
    } else if (i1.getSwitchportMode() == SwitchportMode.TRUNK) {
      Integer node2VlanId =
          i2.getSwitchportMode() == SwitchportMode.ACCESS ? i2.getAccessVlan() : null;
      edges.add(new Layer2Edge(node1, i1.getNativeVlan(), node2, node2VlanId, null));
    } else if (i2.getSwitchportMode() == SwitchportMode.TRUNK) {
      Integer node1VlanId =
          i1.getSwitchportMode() == SwitchportMode.ACCESS ? i1.getAccessVlan() : null;
      edges.add(new Layer2Edge(node1, node1VlanId, node2, i2.getNativeVlan(), null));
    }
  }

  private static void computeAugmentedLayer2SelfEdges(
      @Nonnull String hostname, @Nonnull Vrf vrf, @Nonnull ImmutableSet.Builder<Layer2Edge> edges) {
    Map<Integer, ImmutableList.Builder<String>> switchportsByVlan = new HashMap<>();
    vrf.getInterfaces()
        .values()
        .stream()
        .filter(Interface::getActive)
        .forEach(
            i -> {
              if (i.getSwitchportMode() == SwitchportMode.TRUNK) {
                switchportsByVlan
                    .computeIfAbsent(i.getNativeVlan(), n -> ImmutableList.builder())
                    .add(i.getName());
                i.getAllowedVlans()
                    .stream()
                    .flatMapToInt(SubRange::asStream)
                    .forEach(
                        allowedVlan -> {
                          switchportsByVlan
                              .computeIfAbsent(allowedVlan, n -> ImmutableList.builder())
                              .add(i.getName());
                        });
              }
              if (i.getSwitchportMode() == SwitchportMode.ACCESS) {
                switchportsByVlan
                    .computeIfAbsent(i.getAccessVlan(), n -> ImmutableList.builder())
                    .add(i.getName());
              }
              if (i.getInterfaceType() == InterfaceType.VLAN) {
                switchportsByVlan
                    .computeIfAbsent(i.getVlan(), n -> ImmutableList.builder())
                    .add(i.getName());
              }
            });
    switchportsByVlan.forEach(
        (vlanId, interfaceNamesBuilder) -> {
          List<String> interfaceNames = interfaceNamesBuilder.build();
          forEachWithIndex(
              interfaceNames,
              (i, i1Name) -> {
                for (int j = i + 1; j < interfaceNames.size(); j++) {
                  String i2Name = interfaceNames.get(j);
                  edges.add(
                      new Layer2Edge(hostname, i1Name, vlanId, hostname, i2Name, vlanId, null));
                  edges.add(
                      new Layer2Edge(hostname, i2Name, vlanId, hostname, i1Name, vlanId, null));
                }
              });
        });
  }

  private static Layer2Topology computeAugmentedLayer2Topology(
      Layer2Topology layer2Topology, Map<String, Configuration> configurations) {
    ImmutableSet.Builder<Layer2Edge> augmentedEdges = ImmutableSet.builder();
    augmentedEdges.addAll(layer2Topology.getGraph().edges());
    configurations
        .values()
        .forEach(
            c -> {
              c.getVrfs()
                  .values()
                  .forEach(
                      vrf -> computeAugmentedLayer2SelfEdges(c.getHostname(), vrf, augmentedEdges));
            });

    // Now compute the transitive closure to connect interfaces across devices
    Layer2Topology initial = new Layer2Topology(augmentedEdges.build());
    Graph<Layer2Node> initialGraph = initial.getGraph().asGraph();
    Graph<Layer2Node> closure = Graphs.transitiveClosure(initialGraph);

    /*
     * We must remove edges connecting existing endpoint pairs since they may clash due to missing
     * encapsulation vlan id. Also, we remove self-edges on the same interfaces by convention.
     */
    Set<EndpointPair<Layer2Node>> newEndpoints =
        Sets.difference(closure.edges(), initialGraph.edges());
    newEndpoints
        .stream()
        .filter(ne -> !ne.source().equals(ne.target()))
        .forEach(
            newEndpoint ->
                augmentedEdges.add(
                    new Layer2Edge(newEndpoint.source(), newEndpoint.target(), null)));
    return new Layer2Topology(augmentedEdges.build());
  }

  public static @Nonnull Layer1Topology computeLayer1Topology(
      @Nonnull Layer1Topology rawLayer1Topology,
      @Nonnull Map<String, Configuration> configurations) {
    /* Filter out inactive interfaces */
    return new Layer1Topology(
        rawLayer1Topology
            .getGraph()
            .edges()
            .stream()
            .filter(
                edge -> {
                  Interface i1 = getInterface(edge.getNode1(), configurations);
                  Interface i2 = getInterface(edge.getNode2(), configurations);
                  return i1 != null && i2 != null && i1.getActive() && i2.getActive();
                })
            .collect(ImmutableSet.toImmutableSet()));
  }

  private static void computeLayer2EdgesForLayer1Edge(
      @Nonnull Layer1Edge layer1Edge,
      @Nonnull Map<String, Configuration> configurations,
      @Nonnull ImmutableSet.Builder<Layer2Edge> edges) {
    Layer1Node node1 = layer1Edge.getNode1();
    Layer1Node node2 = layer1Edge.getNode2();
    Interface i1 = getInterface(node1, configurations);
    Interface i2 = getInterface(node2, configurations);
    if (i1 == null || i2 == null) {
      return;
    }
    if (i1.getSwitchportMode() == SwitchportMode.TRUNK
        || i2.getSwitchportMode() == SwitchportMode.TRUNK) {
      addLayer2TrunkEdges(i1, i2, edges, node1, node2);
    } else {
      Integer node1VlanId =
          i1.getSwitchportMode() == SwitchportMode.ACCESS ? i1.getAccessVlan() : null;
      Integer node2VlanId =
          i2.getSwitchportMode() == SwitchportMode.ACCESS ? i2.getAccessVlan() : null;
      edges.add(new Layer2Edge(node1, node1VlanId, node2, node2VlanId, null));
    }
  }

  private static void computeLayer2SelfEdges(
      @Nonnull String hostname, @Nonnull Vrf vrf, @Nonnull ImmutableSet.Builder<Layer2Edge> edges) {
    Map<Integer, ImmutableList.Builder<String>> switchportsByVlan = new HashMap<>();
    vrf.getInterfaces()
        .values()
        .stream()
        .filter(Interface::getActive)
        .forEach(
            i -> {
              if (i.getSwitchportMode() == SwitchportMode.TRUNK) {
                switchportsByVlan
                    .computeIfAbsent(i.getNativeVlan(), n -> ImmutableList.builder())
                    .add(i.getName());
                i.getAllowedVlans()
                    .stream()
                    .flatMapToInt(SubRange::asStream)
                    .forEach(
                        allowedVlan -> {
                          switchportsByVlan
                              .computeIfAbsent(allowedVlan, n -> ImmutableList.builder())
                              .add(i.getName());
                        });
              }
              if (i.getSwitchportMode() == SwitchportMode.ACCESS) {
                switchportsByVlan
                    .computeIfAbsent(i.getAccessVlan(), n -> ImmutableList.builder())
                    .add(i.getName());
              }
            });
    switchportsByVlan.forEach(
        (vlanId, interfaceNamesBuilder) -> {
          List<String> interfaceNames = interfaceNamesBuilder.build();
          CommonUtil.forEachWithIndex(
              interfaceNames,
              (i, i1Name) -> {
                for (int j = i + 1; j < interfaceNames.size(); j++) {
                  String i2Name = interfaceNames.get(j);
                  edges.add(
                      new Layer2Edge(hostname, i1Name, vlanId, hostname, i2Name, vlanId, null));
                  edges.add(
                      new Layer2Edge(hostname, i2Name, vlanId, hostname, i1Name, vlanId, null));
                }
              });
        });
  }

  /**
   * Compute the layer-2 topology via the {@code level1Topology} and switching information contained
   * in the {@code configurations}.
   */
  public static @Nonnull Layer2Topology computeLayer2Topology(
      @Nonnull Layer1Topology layer1Topology, @Nonnull Map<String, Configuration> configurations) {
    ImmutableSet.Builder<Layer2Edge> edges = ImmutableSet.builder();
    // First add layer2 edges for physical links.
    layer1Topology
        .getGraph()
        .edges()
        .stream()
        .forEach(layer1Edge -> computeLayer2EdgesForLayer1Edge(layer1Edge, configurations, edges));

    // Then add edges within each node to connect switchports on the same VLAN(s).
    configurations
        .values()
        .forEach(
            c -> {
              c.getVrfs()
                  .values()
                  .forEach(vrf -> computeLayer2SelfEdges(c.getHostname(), vrf, edges));
            });

    // Now compute the transitive closure to connect interfaces across devices
    Layer2Topology initial = new Layer2Topology(edges.build());
    Graph<Layer2Node> initialGraph = initial.getGraph().asGraph();
    Graph<Layer2Node> closure = Graphs.transitiveClosure(initialGraph);

    /*
     * We must remove edges connecting existing endpoint pairs since they may clash due to missing
     * encapsulation vlan id. Also, we remove self-edges on the same interfaces since our network
     * type does not allow them.
     */
    Set<EndpointPair<Layer2Node>> newEndpoints =
        Sets.difference(closure.edges(), initialGraph.edges());
    newEndpoints
        .stream()
        .filter(ne -> !ne.source().equals(ne.target()))
        .forEach(
            newEndpoint ->
                edges.add(new Layer2Edge(newEndpoint.source(), newEndpoint.target(), null)));
    return new Layer2Topology(edges.build());
  }

  /**
   * Compute the layer 3 topology from the layer-2 topology and layer-3 information contained in the
   * configurations.
   */
  public static @Nonnull Layer3Topology computeLayer3Topology(
      @Nonnull Layer2Topology layer2Topology, @Nonnull Map<String, Configuration> configurations) {
    /*
     * The computation proceeds by augmenting the layer-2 topology with self-edges between Vlan/IRB
     * interfaces and switchports on those VLANs.
     * Then transitive closure and filtering are done as in layer-2 topology computation.
     * Finally, the resulting edges are filtered down to those with proper layer-3 addressing,
     * and transformed to only contain layer-2 relevant information.
     */
    ImmutableSet.Builder<Layer3Edge> layer3Edges = ImmutableSet.builder();
    Layer2Topology vlanInterfaceAugmentedLayer2Topology =
        computeAugmentedLayer2Topology(layer2Topology, configurations);
    vlanInterfaceAugmentedLayer2Topology
        .getGraph()
        .edges()
        .forEach(
            layer2Edge -> {
              Interface i1 = getInterface(layer2Edge.getNode1(), configurations);
              Interface i2 = getInterface(layer2Edge.getNode2(), configurations);
              if (i1 == null
                  || i2 == null
                  || (i1.getOwner().equals(i2.getOwner())
                      && i1.getVrfName().equals(i2.getVrfName()))) {
                return;
              }
              if (!i1.getAllAddresses().isEmpty() || !i2.getAllAddresses().isEmpty()) {
                assert Boolean.TRUE;
              }
              if (!matchingSubnet(i1.getAllAddresses(), i2.getAllAddresses())) {
                return;
              }
              layer3Edges.add(toLayer3Edge(layer2Edge));
            });
    return new Layer3Topology(layer3Edges.build());
  }

  private static @Nullable Configuration getConfiguration(
      Layer1Node layer1Node, Map<String, Configuration> configurations) {
    return configurations.get(layer1Node.getHostname());
  }

  private static @Nullable Configuration getConfiguration(
      Layer2Node layer2Node, Map<String, Configuration> configurations) {
    return configurations.get(layer2Node.getHostname());
  }

  public static @Nullable Interface getInterface(
      @Nonnull Layer1Node layer1Node, @Nonnull Map<String, Configuration> configurations) {
    Configuration c = getConfiguration(layer1Node, configurations);
    if (c == null) {
      return null;
    }
    return c.getInterfaces().get(layer1Node.getInterfaceName());
  }

  public static @Nullable Interface getInterface(
      @Nonnull Layer2Node layer2Node, @Nonnull Map<String, Configuration> configurations) {
    Configuration c = getConfiguration(layer2Node, configurations);
    if (c == null) {
      return null;
    }
    return c.getInterfaces().get(layer2Node.getInterfaceName());
  }

  private static boolean matchingSubnet(
      @Nonnull InterfaceAddress address1, @Nonnull InterfaceAddress address2) {
    return address1.getPrefix().equals(address2.getPrefix())
        && !address1.getIp().equals(address2.getIp());
  }

  private static boolean matchingSubnet(
      @Nonnull Set<InterfaceAddress> addresses1, @Nonnull Set<InterfaceAddress> addresses2) {
    return addresses1
        .stream()
        .anyMatch(
            address1 ->
                addresses2.stream().anyMatch(address2 -> matchingSubnet(address1, address2)));
  }

  public static @Nonnull Edge toEdge(@Nonnull Layer3Edge layer3Edge) {
    return new Edge(
        toNodeInterfacePair(layer3Edge.getNode1()), toNodeInterfacePair(layer3Edge.getNode2()));
  }

  public static @Nonnull Layer2Node toLayer2Node(Layer1Node layer1Node, int vlanId) {
    return new Layer2Node(layer1Node.getHostname(), layer1Node.getInterfaceName(), vlanId);
  }

  private static @Nonnull Layer3Edge toLayer3Edge(@Nonnull Layer2Edge layer2Edge) {
    return new Layer3Edge(toLayer3Node(layer2Edge.getNode1()), toLayer3Node(layer2Edge.getNode2()));
  }

  private static @Nonnull Layer3Node toLayer3Node(@Nonnull Layer2Node node) {
    return new Layer3Node(node.getHostname(), node.getInterfaceName());
  }

  private static @Nonnull NodeInterfacePair toNodeInterfacePair(@Nonnull Layer3Node node) {
    return new NodeInterfacePair(node.getHostname(), node.getInterfaceName());
  }

  public static @Nonnull Topology toTopology(Layer3Topology layer3Topology) {
    return new Topology(
        layer3Topology
            .getGraph()
            .edges()
            .stream()
            .map(TopologyUtil::toEdge)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  private TopologyUtil() {}
}

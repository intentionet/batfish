package org.batfish.common.topology;

import static org.batfish.common.util.CollectionUtil.toImmutableMap;
import static org.batfish.common.util.IpsecUtil.initIpsecTopology;
import static org.batfish.common.util.IpsecUtil.retainCompatibleTunnelEdges;
import static org.batfish.datamodel.Interface.TUNNEL_INTERFACE_TYPES;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.graph.EndpointPair;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.plugin.TracerouteEngine;
import org.batfish.common.topology.TunnelTopology.Builder;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpsecSession;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.TraceAndReverseFlow;
import org.batfish.datamodel.ipsec.IpsecTopology;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.VxlanNode;
import org.batfish.datamodel.vxlan.VxlanTopology;

public final class TopologyUtil {

  /** Returns true iff the given trunk interface allows its own native vlan. */
  private static boolean trunkWithNativeVlanAllowed(Interface i) {
    return i.getSwitchportMode() == SwitchportMode.TRUNK
        && i.getNativeVlan() != null
        && i.getAllowedVlans().contains(i.getNativeVlan());
  }

  // Precondition: at least one of i1 and i2 is a trunk
  private static void addLayer2TrunkEdges(
      Interface i1,
      Interface i2,
      Consumer<Layer2Edge> edges,
      Layer1Node l1Node1,
      Layer1Node l1Node2,
      Map<String, InterfacesByVlanRange> vlanRangesPerNode) {
    Integer i1Tag = i1.getEncapsulationVlan();
    Integer i2Tag = i2.getEncapsulationVlan();
    InterfacesByVlanRange node1Ranges = vlanRangesPerNode.get(l1Node1.getHostname());
    InterfacesByVlanRange node2Ranges = vlanRangesPerNode.get(l1Node2.getHostname());
    if (i1.getSwitchportMode() == SwitchportMode.TRUNK
        && i2.getSwitchportMode() == SwitchportMode.TRUNK) {
      /*
        Both sides are trunks, so add edges from n1,vlan_range to n2,vlan_range for all shared VLANs.
        VLAN ranges are canonical and have been determined globally, so no key in node1Ranges can
        overlap with any key in node2Ranges unless they're the same range.
      */
      Set<Range<Integer>> sharedRanges =
          Sets.intersection(node1Ranges.getMap().keySet(), node2Ranges.getMap().keySet()).stream()
              .filter(
                  r ->
                      node1Ranges.getMap().get(r).contains(l1Node1.getInterfaceName())
                          && node2Ranges.getMap().get(r).contains(l1Node2.getInterfaceName()))
              .collect(ImmutableSet.toImmutableSet());
      for (Range<Integer> sharedRange : sharedRanges) {
        // This frame will be tagged by i1 and we can directly check whether i2 allows.
        edges.accept(new Layer2Edge(l1Node1, sharedRange, l1Node2, sharedRange));
      }
      if (i1.getNativeVlan() != null && trunkWithNativeVlanAllowed(i2)) {
        // This frame will not be tagged by i1, and i2 accepts untagged frames.
        assert i2.getNativeVlan() != null; // invariant of trunkWithNativeVlanAllowed
        edges.accept(
            new Layer2Edge(
                l1Node1,
                Range.singleton(i1.getNativeVlan()),
                l1Node2,
                Range.singleton(i2.getNativeVlan())));
      }
    } else if (i1Tag != null) {
      // i1 is a tagged layer-3 interface, and the other side is a trunk. The only possible edge is
      // i2 receiving frames for a non-native allowed vlan.
      if (!i1Tag.equals(i2.getNativeVlan()) && i2.getAllowedVlans().contains(i1Tag)) {
        edges.accept(new Layer2Edge(l1Node1, null, l1Node2, node2Ranges.getRange(i1Tag)));
      }
    } else if (i2Tag != null) {
      // i1 is a trunk, and the other side is a tagged layer-3 interface. The only possible edge is
      // i2 receiving frames for from a non-native allowed vlan of i1.
      if (!i2Tag.equals(i1.getNativeVlan()) && i1.getAllowedVlans().contains(i2Tag)) {
        edges.accept(new Layer2Edge(l1Node1, node1Ranges.getRange(i2Tag), l1Node2, null));
      }
    } else if (trunkWithNativeVlanAllowed(i1)) {
      // i1 is a trunk, but the other side is not and does not use tags. The only edge that will
      // come up is i2 receiving untagged packets.
      Integer node2VlanId =
          i2.getSwitchportMode() == SwitchportMode.ACCESS ? i2.getAccessVlan() : null;
      assert i1.getNativeVlan() != null; // invariant of trunkWithNativeVlanAllowed
      edges.accept(
          new Layer2Edge(
              l1Node1,
              node1Ranges.getRange(i1.getNativeVlan()),
              l1Node2,
              node2VlanId == null ? null : node2Ranges.getRange(node2VlanId)));
    } else if (trunkWithNativeVlanAllowed(i2)) {
      // i1 is not a trunk and does not use tags, but the other side is a trunk. The only edge that
      // will come up is the other side receiving untagged packets and treating them as native VLAN.
      Integer node1VlanId =
          i1.getSwitchportMode() == SwitchportMode.ACCESS ? i1.getAccessVlan() : null;
      assert i2.getNativeVlan() != null; // invariant of trunkWithNativeVlanAllowed
      edges.accept(
          new Layer2Edge(
              l1Node1,
              node1VlanId == null ? null : node1Ranges.getRange(node1VlanId),
              l1Node2,
              node2Ranges.getRange(i2.getNativeVlan())));
    }
  }

  /**
   * Cleans {@link Layer1Topology}, by removing non-existent interfaces and making connectivity
   * symmetric.
   */
  public static @Nonnull Layer1Topology cleanLayer1PhysicalTopology(
      @Nonnull Layer1Topology rawLayer1Topology,
      @Nonnull Map<String, Configuration> configurations) {
    ImmutableSet.Builder<Layer1Edge> edges = ImmutableSet.builder();
    rawLayer1Topology.getGraph().edges().stream()
        .filter(
            edge -> {
              Interface i1 = getInterface(edge.getNode1(), configurations);
              Interface i2 = getInterface(edge.getNode2(), configurations);
              return i1 != null && i2 != null && i1.getActive() && i2.getActive();
            })
        .forEach(
            edge -> {
              edges.add(edge);
              edges.add(edge.reverse());
            });
    /* Filter out inactive interfaces */
    return new Layer1Topology(edges.build());
  }

  /**
   * Takes the union of two {@link Optional}s containing {@link Layer1Topology}. If either topology
   * is not present, the other is returned. If both are present, the returned topology is the union
   * of the edges in the two.
   */
  public static @Nonnull Optional<Layer1Topology> unionLayer1PhysicalTopologies(
      Optional<Layer1Topology> topology1, Optional<Layer1Topology> topology2) {
    if (!topology1.isPresent()) {
      return topology2;
    }
    if (!topology2.isPresent()) {
      return topology1;
    }
    return Optional.of(
        new Layer1Topology(
            Sets.union(topology1.get().getGraph().edges(), topology2.get().getGraph().edges())));
  }

  private static void computeLayer2EdgesForLayer1Edge(
      @Nonnull Layer1Edge layer1Edge,
      @Nonnull Map<String, Configuration> configurations,
      @Nonnull Consumer<Layer2Edge> edges,
      Map<Layer1Node, Set<Layer1Node>> parentChildrenMap,
      Map<String, InterfacesByVlanRange> vlanRangesPerNode) {
    Layer1Node node1 = layer1Edge.getNode1();
    Layer1Node node2 = layer1Edge.getNode2();

    // Map each layer1-node to a set of layer-1 nodes corresponding to its children. Attempt to
    // construct a layer-2 edge between each child in the cross product of the child sets.
    parentChildrenMap
        .getOrDefault(node1, ImmutableSet.of(node1))
        .forEach(
            node1Child ->
                parentChildrenMap
                    .getOrDefault(node2, ImmutableSet.of(node2))
                    .forEach(
                        node2Child ->
                            tryComputeLayer2EdgesForLayer1ChildEdge(
                                new Layer1Edge(node1Child, node2Child),
                                configurations,
                                edges,
                                vlanRangesPerNode)));
  }

  private static void tryComputeLayer2EdgesForLayer1ChildEdge(
      Layer1Edge layer1MappedEdge,
      Map<String, Configuration> configurations,
      Consumer<Layer2Edge> edges,
      Map<String, InterfacesByVlanRange> vlanRangesPerNode) {
    Layer1Node node1 = layer1MappedEdge.getNode1();
    Layer1Node node2 = layer1MappedEdge.getNode2();
    Interface i1 = getInterface(node1, configurations);
    Interface i2 = getInterface(node2, configurations);
    // Exit early if either interface is missing
    if (i1 == null || i2 == null) {
      return;
    }
    if (i1.getSwitchportMode() == SwitchportMode.TRUNK
        || i2.getSwitchportMode() == SwitchportMode.TRUNK) {
      addLayer2TrunkEdges(i1, i2, edges, node1, node2, vlanRangesPerNode);
    } else if (i1.getEncapsulationVlan() != null || i2.getEncapsulationVlan() != null) {
      // Both interfaces are tagged Layer3 interfaces
      tryAddLayer2TaggedNonTrunkEdge(i1, i2, edges, node1, node2);
    } else {
      Integer node1VlanId =
          i1.getSwitchportMode() == SwitchportMode.ACCESS ? i1.getAccessVlan() : null;
      Integer node2VlanId =
          i2.getSwitchportMode() == SwitchportMode.ACCESS ? i2.getAccessVlan() : null;
      edges.accept(
          new Layer2Edge(
              node1,
              node1VlanId == null ? null : Range.singleton(node1VlanId),
              node2,
              node2VlanId == null ? null : Range.singleton(node2VlanId)));
    }
  }

  private static void tryAddLayer2TaggedNonTrunkEdge(
      Interface i1, Interface i2, Consumer<Layer2Edge> edges, Layer1Node node1, Layer1Node node2) {
    Integer i1Tag = i1.getEncapsulationVlan();
    Integer i2Tag = i2.getEncapsulationVlan();
    // precondition: i1Tag != null || i2Tag != null
    if (i1Tag == null || i2Tag == null || !i1Tag.equals(i2Tag)) {
      return;
    }
    edges.accept(new Layer2Edge(node1, null, node2, null));
  }

  @VisibleForTesting
  static String computeVniName(int vni) {
    return String.format("~vni~%d", vni);
  }

  @VisibleForTesting
  static void computeLayer2SelfEdges(
      @Nonnull Configuration config,
      InterfacesByVlanRange switchportsByVlan,
      @Nonnull Consumer<Layer2Edge> edges) {

    String hostname = config.getHostname();

    // Note that since the L2 model is transitive, we only need add a single spanning tree rather
    // than all O(N^2) edges to get complete connectivity for all interfaces on the same switchport.
    // Additionally, edges need not be added symmetrically.
    switchportsByVlan
        .getMap()
        .forEach(
            (vlanRange, interfaceNames) -> {
              Range<Integer> canonicalRange = vlanRange.canonical(DiscreteDomain.integers());
              if (canonicalRange.isEmpty()) {
                // RangeMap produces all intervals, and is not always aware that the underlying
                // domain is discrete. So it can produce empty intervals like (3, 4). Skip these.
                return;
              }
              assert !interfaceNames.isEmpty();
              Iterator<String> iterator = interfaceNames.iterator();
              String firstInterface = iterator.next();
              Layer2Node firstNode = new Layer2Node(hostname, firstInterface, canonicalRange);
              while (iterator.hasNext()) {
                String otherInterface = iterator.next();
                Layer2Node otherNode = new Layer2Node(hostname, otherInterface, canonicalRange);
                edges.accept(new Layer2Edge(firstNode, otherNode));
              }
            });
    Map<Integer, Layer2Vni> vniSettingsByVlan =
        config.getVrfs().values().stream()
            .flatMap(vrf -> vrf.getLayer2Vnis().values().stream())
            .collect(ImmutableMap.toImmutableMap(Layer2Vni::getVlan, Function.identity()));
    config.getAllInterfaces().values().stream()
        .filter(Interface::getActive)
        .filter(i -> i.getInterfaceType() == InterfaceType.VLAN && i.getVlan() != null)
        .forEach(
            irbInterface -> {
              String irbName = irbInterface.getName();
              int vlanId = irbInterface.getVlan();
              computeSelfSwitchportNonSwitchportEdges(switchportsByVlan, hostname, irbName, vlanId)
                  .forEach(edges::accept);
              // Link IRB to VNI in same VLAN
              Optional.ofNullable(vniSettingsByVlan.get(vlanId))
                  .map(vniSettings -> computeVniName(vniSettings.getVni()))
                  .ifPresent(
                      vniName ->
                          edges.accept(
                              new Layer2Edge(hostname, irbName, null, hostname, vniName, null)));
            });
    // Link each VNI to switchports in same VLAN
    vniSettingsByVlan.forEach(
        (vlanId, vniSettings) -> {
          String vniName = computeVniName(vniSettings.getVni());
          computeSelfSwitchportNonSwitchportEdges(switchportsByVlan, hostname, vniName, vlanId)
              .forEach(edges::accept);
        });
  }

  /**
   * Generates {@link NodeInterfacePairsByVlanRange} for the given {@code hostnames}. Assumes all
   * hostnames are present in {@code configs}.
   */
  @Nonnull
  private static NodeInterfacePairsByVlanRange computeNodeInterfacePairsByVlan(
      @Nonnull Map<String, Configuration> configs, Set<String> hostnames) {
    NodeInterfacePairsByVlanRange nisByVlan = NodeInterfacePairsByVlanRange.create();
    for (String hostname : hostnames) {
      for (Interface i : configs.get(hostname).getActiveInterfaces().values()) {
        NodeInterfacePair ni = NodeInterfacePair.of(i);
        if (i.getSwitchportMode() == SwitchportMode.TRUNK) {
          IntegerSpace allowedVlansNoNative =
              i.getAllowedVlans()
                  .difference(
                      i.getNativeVlan() != null
                          ? IntegerSpace.of(i.getNativeVlan())
                          : IntegerSpace.EMPTY);
          allowedVlansNoNative.getRanges().forEach(vlanRange -> nisByVlan.add(vlanRange, ni));
          // special handling for native VLAN to avoid conflating edges for tagged and non-tagged
          // packets
          if (i.getNativeVlan() != null) {
            nisByVlan.add(i.getNativeVlan(), ni);
          }
        } else if (i.getSwitchportMode() == SwitchportMode.ACCESS && i.getAccessVlan() != null) {
          nisByVlan.add(i.getAccessVlan(), ni);
        } else if (i.getSwitchportMode() == SwitchportMode.NONE && i.getVlan() != null) {
          nisByVlan.add(i.getVlan(), ni);
        }
      }
    }
    return nisByVlan;
  }

  /**
   * Computes intra-node edges between non-switchport layer-2 entity (IRB or VNI) and switchport
   * interfaces associated with the entity's VLAN
   */
  private static @Nonnull Stream<Layer2Edge> computeSelfSwitchportNonSwitchportEdges(
      InterfacesByVlanRange switchportsByVlan,
      String hostname,
      String nonSwitchportName,
      int vlanId) {
    Stream.Builder<Layer2Edge> edges = Stream.builder();
    switchportsByVlan
        .get(vlanId)
        .forEach(
            switchportName ->
                edges.accept(
                    new Layer2Edge(
                        hostname, nonSwitchportName, null, hostname, switchportName, vlanId)));
    return edges.build();
  }

  /**
   * Compute the layer-2 topology via:
   *
   * <ul>
   *   <li>wiring information from {@code layer1LogicalTopology}
   *   <li>established VXLAN bridges from {@code vxlanTopology}
   *   <li>switching information from {@code configurations}
   * </ul>
   */
  public static @Nonnull Layer2Topology computeLayer2Topology(
      @Nonnull Layer1Topology layer1LogicalTopology,
      VxlanTopology vxlanTopology,
      @Nonnull Map<String, Configuration> configurations) {
    Layer2Topology.Builder l2TopologyBuilder = Layer2Topology.builder();

    // Compute mapping from parent interface -> child interfaces
    Map<Layer1Node, Set<Layer1Node>> parentChildrenMap = computeParentChildrenMap(configurations);

    // Pre-compute nodes that have L1 or VXLAN edges, and only compute L2 edges for those.
    Set<String> nodesWithL1Edge =
        layer1LogicalTopology.getGraph().edges().stream()
            .flatMap(edge -> Stream.of(edge.getNode1(), edge.getNode2()))
            .map(Layer1Node::getHostname)
            .collect(ImmutableSet.toImmutableSet());
    Set<String> nodesWithVxlan =
        vxlanTopology.getGraph().edges().stream()
            .flatMap(edge -> Stream.of(edge.nodeU(), edge.nodeV()))
            .map(VxlanNode::getHostname)
            .collect(ImmutableSet.toImmutableSet());

    // Break up each (useful) node into VLAN ranges based on interface configuration
    NodeInterfacePairsByVlanRange nisByVlanRange =
        computeNodeInterfacePairsByVlan(
            configurations, Sets.union(nodesWithL1Edge, nodesWithVxlan));
    Map<String, InterfacesByVlanRange> nodesWithUsefulL2SelfEdges = nisByVlanRange.splitByNode();

    // Then add edges within each node to connect switchports and VNIs on the same VLAN(s).
    configurations.values().stream()
        // Optimization: skip nodes with neither L1 nor VXLAN edges, since their L2 self-edges will
        // not contribute to broadcast domains.
        .filter(c -> nodesWithUsefulL2SelfEdges.containsKey(c.getHostname()))
        .forEach(
            c ->
                computeLayer2SelfEdges(
                    c,
                    nodesWithUsefulL2SelfEdges.get(c.getHostname()),
                    l2TopologyBuilder::addEdge));

    // Add layer2 edges for physical links.
    layer1LogicalTopology
        .getGraph()
        .edges()
        .forEach(
            layer1Edge ->
                computeLayer2EdgesForLayer1Edge(
                    layer1Edge,
                    configurations,
                    l2TopologyBuilder::addEdge,
                    parentChildrenMap,
                    nodesWithUsefulL2SelfEdges));

    // Finally add edges between connected VNIs on different nodes
    computeVniInterNodeEdges(vxlanTopology).forEach(l2TopologyBuilder::addEdge);

    return l2TopologyBuilder.build();
  }

  /**
   * Create {@link Layer2Edge}s corresponding the to inter-node {@link VxlanNode} pairs in the
   * {@link VxlanTopology}.
   */
  @VisibleForTesting
  static @Nonnull Stream<Layer2Edge> computeVniInterNodeEdges(VxlanTopology vxlanTopology) {
    return vxlanTopology.getGraph().edges().stream().flatMap(TopologyUtil::toVniVniEdges);
  }

  /**
   * Create pair of directional {@link Layer2Edge}s for undirected pair of inter-node {@link
   * VxlanNode}s.
   */
  private static @Nonnull Stream<Layer2Edge> toVniVniEdges(EndpointPair<VxlanNode> edge) {
    VxlanNode n1 = edge.nodeU();
    VxlanNode n2 = edge.nodeV();
    String h1 = n1.getHostname();
    String h2 = n2.getHostname();
    int vni1 = n1.getVni();
    int vni2 = n2.getVni();
    String vni1Name = computeVniName(vni1);
    String vni2Name = computeVniName(vni2);
    return Stream.of(
        new Layer2Edge(h1, vni1Name, null, h2, vni2Name, null),
        new Layer2Edge(h2, vni2Name, null, h1, vni1Name, null));
  }

  private static Map<Layer1Node, Set<Layer1Node>> computeParentChildrenMap(
      Map<String, Configuration> configurations) {
    // parent -> set of children
    Map<Layer1Node, ImmutableSet.Builder<Layer1Node>> builderMap = new HashMap<>();

    // Map each parent interface that is the target of a bind dependency to the set of its child
    // interfaces (e.g. Juniper units).
    configurations.forEach(
        (hostname, c) ->
            c.getAllInterfaces()
                .forEach(
                    (iName, i) ->
                        // i is a potential child interface
                        i.getDependencies().stream()
                            .filter(dependency -> dependency.getType() == DependencyType.BIND)
                            .forEach(
                                bindDependency ->
                                    builderMap
                                        .computeIfAbsent(
                                            new Layer1Node(
                                                hostname, bindDependency.getInterfaceName()),
                                            n -> ImmutableSet.builder())
                                        .add(new Layer1Node(hostname, iName)))));
    // finalize and freeze
    return toImmutableMap(builderMap, Entry::getKey, e -> e.getValue().build());
  }

  /**
   * Compute the raw layer 3 topology from the layer-1 and layer-2 topologies, and layer-3
   * information contained in the configurations.
   */
  @VisibleForTesting
  public static @Nonnull Topology computeRawLayer3Topology(
      @Nonnull L3Adjacencies adjacencies, @Nonnull Map<String, Configuration> configurations) {
    Stream<Edge> filteredEdgeStream =
        synthesizeL3Topology(configurations).getEdges().stream()
            .filter(
                edge ->
                    adjacencies.inSameBroadcastDomain(edge.getHead(), edge.getTail())
                        // Keep if virtual wire
                        || isVirtualWireSameDevice(edge));
    NetworkConfigurations nc = NetworkConfigurations.of(configurations);
    // For link-layer edges, we see if there is a unique point-to-point interface in the same
    // broadcast domain that is unnumbered. If so, we create an edge, otherwise there is no
    // neighbor.
    Stream<Edge> linkLocalEdges =
        configurations.values().stream()
            .flatMap(Configuration::activeInterfaces)
            .filter(i -> !i.getAllLinkLocalAddresses().isEmpty())
            .map(i -> NodeInterfacePair.of(i.getOwner().getHostname(), i.getName()))
            .flatMap(
                nip -> {
                  @Nullable
                  NodeInterfacePair paired =
                      adjacencies
                          .pairedPointToPointL3Interface(nip)
                          .filter(
                              other ->
                                  nc.getInterface(other.getHostname(), other.getInterface())
                                      .map(
                                          i ->
                                              i.getActive()
                                                  && !i.getAllLinkLocalAddresses().isEmpty())
                                      .orElse(false))
                          .orElse(null);
                  if (paired == null) {
                    return Stream.of();
                  }
                  return Stream.of(new Edge(nip, paired), new Edge(paired, nip));
                });

    return new Topology(
        Streams.concat(filteredEdgeStream, linkLocalEdges)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  /**
   * Compute the layer-3 topology from the raw layer-3 topology, configuration information, and
   * overlay edges.
   *
   * @param rawLayer3Topology raw layer 3 {@link Topology}
   * @param overlayEdges overlay edges to be added to the rawLayer3Topology
   */
  public static @Nonnull Topology computeLayer3Topology(
      Topology rawLayer3Topology, Set<Edge> overlayEdges) {
    return new Topology(
        ImmutableSortedSet.<Edge>naturalOrder()
            .addAll(rawLayer3Topology.getEdges())
            .addAll(overlayEdges)
            .build());
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
    return c.getAllInterfaces().get(layer1Node.getInterfaceName());
  }

  public static @Nullable Interface getInterface(
      @Nonnull Layer2Node layer2Node, @Nonnull Map<String, Configuration> configurations) {
    Configuration c = getConfiguration(layer2Node, configurations);
    if (c == null) {
      return null;
    }
    return c.getAllInterfaces().get(layer2Node.getInterfaceName());
  }

  private TopologyUtil() {}

  /**
   * Computes the {@link IpsecTopology} from {@link Configuration}s. The returned topology will only
   * contain the compatible edges which have successfully negotiated {@link IpsecSession}s
   *
   * @param configurations {@link Map} of {@link Configuration}s
   * @return {@link IpsecTopology}
   */
  public static IpsecTopology computeIpsecTopology(Map<String, Configuration> configurations) {
    return retainCompatibleTunnelEdges(initIpsecTopology(configurations), configurations);
  }

  /**
   * Compute the interfaces of each node.
   *
   * @param configurations The {@link Configuration}s for the network
   * @return A map from hostname to the interfaces of that node.
   */
  public static Map<String, Set<Interface>> computeNodeInterfaces(
      Map<String, Configuration> configurations) {
    return toImmutableMap(
        configurations,
        Entry::getKey,
        e -> ImmutableSet.copyOf(e.getValue().getAllInterfaces().values()));
  }

  /** Returns {@code true} if any {@link Ip IP address} is owned by both devices. */
  private static boolean haveIpInCommon(Interface i1, Interface i2) {
    for (ConcreteInterfaceAddress ia : i1.getAllConcreteAddresses()) {
      for (ConcreteInterfaceAddress ia2 : i2.getAllConcreteAddresses()) {
        if (ia.getIp().equals(ia2.getIp())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns a {@link Topology} inferred from the L3 configuration of interfaces on the devices.
   *
   * <p>Ignores {@code Loopback} interfaces and inactive interfaces.
   */
  public static Topology synthesizeL3Topology(Map<String, Configuration> configurations) {
    Map<Prefix, List<Interface>> prefixInterfaces = computeInterfacesBucketByPrefix(configurations);

    ImmutableSortedSet.Builder<Edge> edges = ImmutableSortedSet.naturalOrder();
    for (Entry<Prefix, List<Interface>> bucketEntry : prefixInterfaces.entrySet()) {
      Prefix p = bucketEntry.getKey();
      Set<Interface> candidateInterfaces = candidateInterfacesForPrefix(prefixInterfaces, p);

      for (Interface iface1 : bucketEntry.getValue()) {
        for (Interface iface2 : candidateInterfaces) {
          // No device self-adjacencies in the same VRF.
          if (!isValidLayer3Adjacency(iface1, iface2)) {
            continue;
          }
          // Additionally, don't connect if any of the two endpoint interfaces have Tunnel or VPN
          // interfaceTypes
          if (TUNNEL_INTERFACE_TYPES.contains(iface1.getInterfaceType())
              || TUNNEL_INTERFACE_TYPES.contains(iface2.getInterfaceType())) {
            continue;
          }
          edges.add(new Edge(iface1, iface2));
        }
      }
    }
    return new Topology(edges.build());
  }

  /**
   * Collect all interfaces that have subnets overlapping P iff they have an IP address in P. Use an
   * IdentityHashSet to prevent duplicates.
   */
  @Nonnull
  private static Set<Interface> candidateInterfacesForPrefix(
      Map<Prefix, List<Interface>> prefixBuckets, Prefix p) {
    Set<Interface> candidateInterfaces = Sets.newIdentityHashSet();
    IntStream.range(0, Prefix.MAX_PREFIX_LENGTH)
        .mapToObj(
            i -> prefixBuckets.getOrDefault(Prefix.create(p.getStartIp(), i), ImmutableList.of()))
        .flatMap(Collection::stream)
        .filter(
            iface ->
                iface.getAllConcreteAddresses().stream().anyMatch(ia -> p.containsIp(ia.getIp())))
        .forEach(candidateInterfaces::add);
    return candidateInterfaces;
  }

  /** Bucket Interfaces that are not loopbacks and not /32s by their prefix */
  @Nonnull
  private static Map<Prefix, List<Interface>> computeInterfacesBucketByPrefix(
      Map<String, Configuration> configurations) {
    Map<Prefix, List<Interface>> prefixInterfaces = new HashMap<>();
    configurations.forEach(
        (nodeName, node) -> {
          for (Interface iface : node.getAllInterfaces().values()) {
            if (iface.isLoopback() || !iface.getActive()) {
              continue;
            }
            // Look at all allocated addresses to determine subnet buckets
            for (ConcreteInterfaceAddress address : iface.getAllConcreteAddresses()) {
              Prefix prefix = address.getPrefix();
              if (prefix.getPrefixLength() < Prefix.MAX_PREFIX_LENGTH) {
                List<Interface> interfaceBucket =
                    prefixInterfaces.computeIfAbsent(prefix, k -> new LinkedList<>());
                interfaceBucket.add(iface);
              }
            }
          }
        });
    return prefixInterfaces;
  }

  /**
   * Returns true if the edge is from a snapshot node to ISP. Such edges are added as part of ISP
   * modeling, and need to ignored for the purposes of determining if the snapshot node had
   * user-provided L1 topology.
   *
   * <p>If either end of the edge is not present in configurations, returns false.
   */
  @VisibleForTesting
  static boolean isBorderToIspEdge(Layer1Edge l1Edge, Map<String, Configuration> configurations) {
    return configurations.containsKey(l1Edge.getNode1().getHostname())
        && configurations.containsKey(l1Edge.getNode2().getHostname())
        // edge ends at ISP
        && configurations.get(l1Edge.getNode2().getHostname()).getDeviceModel()
            == DeviceModel.BATFISH_ISP
        // but does not start at Internet
        && configurations.get(l1Edge.getNode1().getHostname()).getDeviceModel()
            != DeviceModel.BATFISH_INTERNET;
  }

  /**
   * Check if the link between two given interfaces is a valid layer 3 edge (e.g., not a self loop,
   * doesn't have overlapping IPs)
   */
  private static boolean isValidLayer3Adjacency(Interface iface1, Interface iface2) {
    // No device self-adjacencies in the same VRF.
    if (iface1.getOwner() == iface2.getOwner() && iface1.getVrfName().equals(iface2.getVrfName())) {
      return false;
    }
    // Don't connect interfaces that have any IP address in common
    return !haveIpInCommon(iface1, iface2);
  }

  /**
   * Returns true if the given edge could correspond to a virtual link on the same device. Such
   * edges do not appear in L1 topology, but should not be pruned off when L1 is present.
   *
   * <p><b>Note</b> not every self-edge is a valid virtual wire (e.g., VRFs could have IP reuse,
   * wires could be physical, etc.) This method employs some (questionable) heuristics to determine
   * if a pair of interfaces constitutes a virtual wire.
   *
   * @param edge a valid (i.e., subnets match up) L3 edge.
   */
  @VisibleForTesting
  static boolean isVirtualWireSameDevice(Edge edge) {
    return edge.getTail().getHostname().equals(edge.getHead().getHostname())
        // Cisco's cross-VRF NAT interfaces.
        && edge.getInt1().startsWith("vasi")
        && edge.getInt2().startsWith("vasi");
  }

  public static @Nonnull Layer1Topology computeLayer1LogicalTopology(
      Layer1Topology layer1PhysicalTopology, Map<String, Configuration> configurations) {
    return new Layer1Topology(
        layer1PhysicalTopology.getGraph().edges().stream()
            .map(pEdge -> pEdge.toLogicalEdge(NetworkConfigurations.of(configurations)))
            .filter(Objects::nonNull)
            .collect(ImmutableSet.toImmutableSet()));
  }

  /**
   * Compute candidate tunnel topology (see {@link TunnelTopology} for definition). Includes all
   * possibly valid edges (according to IP addresse and interface types), but not verified using
   * reachability checks.
   */
  @Nonnull
  public static TunnelTopology computeInitialTunnelTopology(
      Map<String, Configuration> configurations) {
    Map<Prefix, List<Interface>> prefixInterfaces = computeInterfacesBucketByPrefix(configurations);
    TunnelTopology.Builder builder = TunnelTopology.builder();
    for (Entry<Prefix, List<Interface>> bucketEntry : prefixInterfaces.entrySet()) {
      Prefix p = bucketEntry.getKey();
      Set<Interface> candidateInterfaces = candidateInterfacesForPrefix(prefixInterfaces, p);

      for (Interface iface1 : bucketEntry.getValue()) {
        for (Interface iface2 : candidateInterfaces) {
          if (!isValidLayer3Adjacency(iface1, iface2)) {
            continue;
          }

          // connect only if of both of the endpoints have Tunnel interface type, and their tunnel
          // configs match
          if (iface1.getInterfaceType() == InterfaceType.TUNNEL
              && iface2.getInterfaceType() == InterfaceType.TUNNEL
              && iface1.getTunnelConfig() != null
              && iface2.getTunnelConfig() != null
              && iface1
                  .getTunnelConfig()
                  .getSourceAddress()
                  .equals(iface2.getTunnelConfig().getDestinationAddress())
              && iface1
                  .getTunnelConfig()
                  .getDestinationAddress()
                  .equals(iface2.getTunnelConfig().getSourceAddress())) {
            builder.add(NodeInterfacePair.of(iface1), NodeInterfacePair.of(iface2));
          }
        }
      }
    }
    return builder.build();
  }

  /** Return a {@link TunnelTopology} where tunnel endpoints can reach each other. */
  @Nonnull
  public static TunnelTopology pruneUnreachableTunnelEdges(
      TunnelTopology initialTunnelTopology,
      NetworkConfigurations configurations,
      TracerouteEngine tracerouteEngine) {
    Builder builder = TunnelTopology.builder();
    initialTunnelTopology
        .getGraph()
        .edges()
        .forEach(
            edge -> {
              if (tracerouteForTunnelEdge(configurations, tracerouteEngine, edge)) {
                builder.add(edge.nodeU(), edge.nodeV());
              }
            });
    return builder.build();
  }

  /** Traceroute between two tunnel interfaces. Returns true if traceroute succeeds. */
  private static boolean tracerouteForTunnelEdge(
      NetworkConfigurations configurations,
      TracerouteEngine tracerouteEngine,
      EndpointPair<NodeInterfacePair> edge) {
    NodeInterfacePair src = edge.nodeU();
    NodeInterfacePair dst = edge.nodeV();
    Interface tailTunnel =
        configurations
            .getInterface(src.getHostname(), src.getInterface())
            .orElseThrow(
                () -> new IllegalStateException(String.format("Invalid tunnel interface %s", src)));
    Interface headTunnel =
        configurations
            .getInterface(dst.getHostname(), dst.getInterface())
            .orElseThrow(
                () -> new IllegalStateException(String.format("Invalid tunnel interface %s", dst)));
    if (tailTunnel.getTunnelConfig() == null || headTunnel.getTunnelConfig() == null) {
      return false;
    }
    Flow flow =
        Flow.builder()
            .setIngressNode(src.getHostname())
            .setIngressVrf(tailTunnel.getVrfName())
            .setSrcIp(tailTunnel.getTunnelConfig().getSourceAddress())
            .setDstIp(tailTunnel.getTunnelConfig().getDestinationAddress())
            .setIpProtocol(IpProtocol.GRE)
            .build();
    SortedMap<Flow, List<TraceAndReverseFlow>> tracerouteResult =
        tracerouteEngine.computeTracesAndReverseFlows(ImmutableSet.of(flow), false);
    List<TraceAndReverseFlow> traceAndReverseFlows = tracerouteResult.get(flow);
    return traceAndReverseFlows != null
        && traceAndReverseFlows.stream()
            .filter(TopologyUtil::isSuccessfulFlow)
            .map(
                // Go backward direction
                tr ->
                    tracerouteEngine
                        .computeTracesAndReverseFlows(ImmutableSet.of(tr.getReverseFlow()), false)
                        .get(tr.getReverseFlow()))
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .anyMatch(TopologyUtil::isSuccessfulFlow);
  }

  private static boolean isSuccessfulFlow(TraceAndReverseFlow tr) {
    return tr.getTrace().getDisposition() == FlowDisposition.ACCEPTED
        && tr.getReverseFlow() != null;
  }
}

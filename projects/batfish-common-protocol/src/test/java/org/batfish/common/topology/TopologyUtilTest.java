package org.batfish.common.topology;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.common.topology.IpOwners.computeIpInterfaceOwners;
import static org.batfish.common.topology.TopologyUtil.computeInitialTunnelTopology;
import static org.batfish.common.topology.TopologyUtil.computeLayer1LogicalTopology;
import static org.batfish.common.topology.TopologyUtil.computeLayer1PhysicalTopology;
import static org.batfish.common.topology.TopologyUtil.computeLayer2SelfEdges;
import static org.batfish.common.topology.TopologyUtil.computeLayer2Topology;
import static org.batfish.common.topology.TopologyUtil.computeRawLayer3Topology;
import static org.batfish.common.topology.TopologyUtil.computeVniInterNodeEdges;
import static org.batfish.common.topology.TopologyUtil.computeVniName;
import static org.batfish.datamodel.matchers.EdgeMatchers.hasHead;
import static org.batfish.datamodel.matchers.EdgeMatchers.hasNode1;
import static org.batfish.datamodel.matchers.EdgeMatchers.hasNode2;
import static org.batfish.datamodel.matchers.EdgeMatchers.hasTail;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.util.IspModelingUtils;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Configuration.Builder;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LinkLocalAddress;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.isp_configuration.BorderInterfaceInfo;
import org.batfish.datamodel.isp_configuration.IspConfiguration;
import org.batfish.datamodel.isp_configuration.IspFilter;
import org.batfish.datamodel.vxlan.VxlanNode;
import org.batfish.datamodel.vxlan.VxlanTopology;
import org.junit.Before;
import org.junit.Test;

/** Tests of {@link TopologyUtil}. */
public final class TopologyUtilTest {

  private Builder _cb;
  private Interface.Builder _ib;
  private NetworkFactory _nf;
  private Vrf.Builder _vb;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _vb = _nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    _ib = _nf.interfaceBuilder();
  }

  /** Make an interface with the specified parameters */
  private Interface iface(String interfaceName, String ip, boolean active, boolean blacklisted) {
    return _nf.interfaceBuilder()
        .setName(interfaceName)
        .setActive(active)
        .setAddress(ConcreteInterfaceAddress.parse(ip))
        .setBlacklisted(blacklisted)
        .build();
  }

  @Test
  public void testComputeLayer1LogicalTopologyAggregate() {
    String c1Name = "c1";
    String c2Name = "c2";
    String c1a1Name = "c1a1";
    String c1i1aName = "c1i1a";
    String c1i1bName = "c1i1b";
    String c2a1Name = "c2a1";
    String c2i1aName = "c2i1a";
    String c2i1bName = "c2i1b";

    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1);
    _ib.setName(c1i1aName).build().setChannelGroup(c1a1Name);
    _ib.setName(c1i1bName).build().setChannelGroup(c1a1Name);
    _ib.setName(c1a1Name).build();

    Configuration c2 = _cb.setHostname(c2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2);
    _ib.setName(c2a1Name).build();
    _ib.setName(c2i1aName).build().setChannelGroup(c2a1Name);
    _ib.setName(c2i1bName).build().setChannelGroup(c2a1Name);

    Map<String, Configuration> configurations = ImmutableMap.of(c1Name, c1, c2Name, c2);
    Layer1Topology layer1PhysicalTopology =
        new Layer1Topology(
            ImmutableSet.of(
                new Layer1Edge(
                    new Layer1Node(c1Name, c1i1aName), new Layer1Node(c2Name, c2i1aName)),
                new Layer1Edge(
                    new Layer1Node(c2Name, c2i1aName), new Layer1Node(c1Name, c1i1aName)),
                new Layer1Edge(
                    new Layer1Node(c1Name, c1i1bName), new Layer1Node(c2Name, c2i1bName)),
                new Layer1Edge(
                    new Layer1Node(c2Name, c2i1bName), new Layer1Node(c1Name, c1i1bName))));

    // Test that physical interface edges where the interfaces are members of aggregates get
    // aggregated in the layer-1 logical topology.
    assertThat(
        TopologyUtil.computeLayer1LogicalTopology(layer1PhysicalTopology, configurations)
            .getGraph()
            .edges(),
        containsInAnyOrder(
            new Layer1Edge(new Layer1Node(c1Name, c1a1Name), new Layer1Node(c2Name, c2a1Name)),
            new Layer1Edge(new Layer1Node(c2Name, c2a1Name), new Layer1Node(c1Name, c1a1Name))));
  }

  @Test
  public void testComputeLayer1PhysicalTopology() {
    String c1Name = "c1";
    String c2Name = "c2";
    String c1i1Name = "c1i1";
    String c1i2Name = "c1i2";
    String c2i1Name = "c2i1";
    String c2i2Name = "c2i2";

    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1);
    _ib.setName(c1i1Name).build();
    _ib.setName(c1i2Name).build();

    Configuration c2 = _cb.setHostname(c2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2);
    _ib.setName(c2i1Name).build();
    _ib.setName(c2i2Name).setActive(false).build();

    Map<String, Configuration> configurations = ImmutableMap.of(c1Name, c1, c2Name, c2);
    Layer1Topology rawLayer1Topology =
        new Layer1Topology(
            ImmutableSet.of(
                new Layer1Edge(new Layer1Node(c1Name, c1i1Name), new Layer1Node(c2Name, c2i1Name)),
                new Layer1Edge(new Layer1Node(c2Name, c2i1Name), new Layer1Node(c1Name, c1i1Name)),
                new Layer1Edge(new Layer1Node(c1Name, c1i2Name), new Layer1Node(c2Name, c2i2Name)),
                new Layer1Edge(
                    new Layer1Node(c2Name, c2i2Name), new Layer1Node(c1Name, c1i2Name))));

    // inactive c2i2 should break c1i2<=>c2i2 link
    assertThat(
        TopologyUtil.computeLayer1PhysicalTopology(rawLayer1Topology, configurations)
            .getGraph()
            .edges(),
        containsInAnyOrder(
            new Layer1Edge(new Layer1Node(c1Name, c1i1Name), new Layer1Node(c2Name, c2i1Name)),
            new Layer1Edge(new Layer1Node(c2Name, c2i1Name), new Layer1Node(c1Name, c1i1Name))));
  }

  private static Layer1Topology layer1Topology(String... names) {
    checkArgument(names.length % 4 == 0);
    Set<Layer1Edge> edges = new HashSet<>();
    for (int i = 0; i < names.length; i += 4) {
      String h1 = names[i];
      String i1 = names[i + 1];
      String h2 = names[i + 2];
      String i2 = names[i + 3];
      Layer1Node n1 = new Layer1Node(h1, i1);
      Layer1Node n2 = new Layer1Node(h2, i2);
      edges.add(new Layer1Edge(n1, n2));
      edges.add(new Layer1Edge(n2, n1));
    }
    return new Layer1Topology(edges);
  }

  @Test
  public void testComputeLayer2TopologyCrossVni() {
    /*
     * SWP1      I1 I2      SWP2
     * <=== S1 ===> <=== S2 ===>
     *
     * - S1 and S2 are switches
     * - SWP1 and SWP2 are access ports for VLAN 2
     * - I1 and I2 are layer-3-only interfaces
     * - Working VXLAN tunnel on VNI 10002 between I1 and I2
     * - VNI 10002 associated with VLAN 2 on S1 and S2
     *
     * Then SWP1 and SWP2 should be in same broadcast domain
     */
    String s1Name = "S1";
    String s2Name = "S2";
    String swp1Name = "SWP1";
    String swp2Name = "SWP2";
    int vlanId = 2;
    int vni = 10002;
    String vrfName = "v1";
    Configuration s1 = _cb.setHostname(s1Name).build();
    Configuration s2 = _cb.setHostname(s2Name).build();
    Vrf v1 = _vb.setOwner(s1).setName(vrfName).build();
    Vrf v2 = _vb.setOwner(s2).setName(vrfName).build();
    _ib.setActive(true);

    // Switchports
    _ib.setAccessVlan(vlanId).setSwitchport(true).setSwitchportMode(SwitchportMode.ACCESS);
    _ib.setOwner(s1).setVrf(v1).setName(swp1Name).build();
    _ib.setOwner(s2).setVrf(v2).setName(swp2Name).build();
    // (Assume existence of layer-3 interfaces)

    // VNIs
    VniSettings.Builder vnb =
        VniSettings.builder()
            .setBumTransportIps(ImmutableSortedSet.of(Ip.FIRST_MULTICAST_IP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setVlan(vlanId)
            .setVni(vni);
    v1.getVniSettings().put(vni, vnb.build());
    v2.getVniSettings().put(vni, vnb.build());

    MutableGraph<VxlanNode> graph = GraphBuilder.undirected().allowsSelfLoops(false).build();
    graph.putEdge(new VxlanNode(s1Name, vni), new VxlanNode(s2Name, vni));
    VxlanTopology vxlanTopology = new VxlanTopology(graph);
    Layer1Topology layer1Topology = new Layer1Topology(ImmutableList.of());

    assertTrue(
        computeLayer2Topology(
                layer1Topology, vxlanTopology, ImmutableMap.of(s1Name, s1, s2Name, s2))
            .inSameBroadcastDomain(
                new Layer2Node(s1Name, swp1Name, vlanId),
                new Layer2Node(s2Name, swp2Name, vlanId)));
  }

  @Test
  public void testComputeVniInterNodeEdges() {
    String s1Name = "S1";
    String s2Name = "S2";
    int vlanId = 2;
    int vni = 10002;
    String vrfName = "v1";
    Configuration s1 = _cb.setHostname(s1Name).build();
    Configuration s2 = _cb.setHostname(s2Name).build();
    Vrf v1 = _vb.setOwner(s1).setName(vrfName).build();
    Vrf v2 = _vb.setOwner(s2).setName(vrfName).build();
    VniSettings.Builder vnb =
        VniSettings.builder()
            .setBumTransportIps(ImmutableSortedSet.of(Ip.FIRST_MULTICAST_IP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setVlan(vlanId)
            .setVni(vni);
    v1.getVniSettings().put(vni, vnb.build());
    v2.getVniSettings().put(vni, vnb.build());
    String vniName = computeVniName(vni);

    MutableGraph<VxlanNode> graph = GraphBuilder.undirected().allowsSelfLoops(false).build();
    graph.putEdge(new VxlanNode(s1Name, vni), new VxlanNode(s2Name, vni));
    VxlanTopology vxlanTopology = new VxlanTopology(graph);
    Layer2Node n1 = new Layer2Node(s1Name, vniName, null);
    Layer2Node n2 = new Layer2Node(s2Name, vniName, null);

    assertThat(
        computeVniInterNodeEdges(vxlanTopology).collect(ImmutableList.toImmutableList()),
        containsInAnyOrder(new Layer2Edge(n1, n2, null), new Layer2Edge(n2, n1, null)));
  }

  @Test
  public void testComputeLayer2Topology_layer1() {
    String c1Name = "c1";
    String c2Name = "c2";
    String c3Name = "c3";
    String c4Name = "c4";

    String c1i1Name = "c1i1";
    String c2i1Name = "c2i1";
    String c3i1Name = "c3i1";
    String c3i2Name = "c3i2";
    String c4i1Name = "c4i1";
    String c4i2Name = "c4i2";
    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1).setActive(true);
    _ib.setName(c1i1Name).build();

    Configuration c2 = _cb.setHostname(c2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2);
    _ib.setName(c2i1Name).build();

    {
      /* c1i1 and c2i1 are non-switchport interfaces, connected in layer1. Thus, they are connected
       * in layer2
       */
      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2);
      Layer1Topology layer1Topology = layer1Topology(c1Name, c1i1Name, c2Name, c2i1Name);
      Layer2Topology layer2Topology =
          computeLayer2Topology(layer1Topology, VxlanTopology.EMPTY, configs);
      assertTrue(
          "c1:i1 and c2:i1 are in the same broadcast domain",
          layer2Topology.inSameBroadcastDomain(c1Name, c1i1Name, c2Name, c2i1Name));
    }

    {
      /* c1i1 and c2i1 are not connected in layer1, but are connected to ACCESS ports on the same
       * VLAN
       */
      Configuration c3 = _cb.setHostname(c3Name).build();
      Vrf v3 = _vb.setOwner(c3).build();
      _ib.setOwner(c3).setVrf(v3);
      Interface c3i1 = _ib.setName(c3i1Name).build();
      c3i1.setSwitchport(true);
      c3i1.setSwitchportMode(SwitchportMode.ACCESS);
      c3i1.setAccessVlan(1);
      Interface c3i2 = _ib.setName(c3i2Name).build();
      c3i2.setSwitchport(true);
      c3i2.setSwitchportMode(SwitchportMode.ACCESS);
      c3i2.setAccessVlan(1);
      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2, c3Name, c3);
      Layer1Topology layer1Topology =
          layer1Topology(
              c1Name, c1i1Name, c3Name, c3i1Name, //
              c2Name, c2i1Name, c3Name, c3i2Name);
      Layer2Topology layer2Topology =
          computeLayer2Topology(layer1Topology, VxlanTopology.EMPTY, configs);
      assertTrue(
          "c1:i1 and c2:i1 are in the same broadcast domain",
          layer2Topology.inSameBroadcastDomain(c1Name, c1i1Name, c2Name, c2i1Name));
    }

    {
      /* c1i1 and c2i1 are not connected in layer1, and are connected to ACCESS ports on different
       * VLANs. So they are not in the same broadcast domain
       */
      Configuration c3 = _cb.setHostname(c3Name).build();
      Vrf v3 = _vb.setOwner(c3).build();
      _ib.setOwner(c3).setVrf(v3);
      Interface c3i1 = _ib.setName(c3i1Name).build();
      c3i1.setSwitchport(true);
      c3i1.setSwitchportMode(SwitchportMode.ACCESS);
      c3i1.setAccessVlan(1);
      Interface c3i2 = _ib.setName(c3i2Name).build();
      c3i2.setSwitchport(true);
      c3i2.setSwitchportMode(SwitchportMode.ACCESS);
      c3i2.setAccessVlan(2);
      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2, c3Name, c3);
      Layer1Topology layer1Topology =
          layer1Topology(
              c1Name, c1i1Name, c3Name, c3i1Name, //
              c2Name, c2i1Name, c3Name, c3i2Name);
      Layer2Topology layer2Topology =
          computeLayer2Topology(layer1Topology, VxlanTopology.EMPTY, configs);
      assertFalse(
          "c1:i1 and c2:i1 are not in the same broadcast domain",
          layer2Topology.inSameBroadcastDomain(c1Name, c1i1Name, c2Name, c2i1Name));
    }

    {
      /* c1i1 and c2i1 are not connected in layer1, but are connected to TRUNK and ACCESS ports, and
       * the ACCESS port's VLAN is the TRUNK's native VLAN
       */
      Configuration c3 = _cb.setHostname(c3Name).build();
      Vrf v3 = _vb.setOwner(c3).build();
      _ib.setOwner(c3).setVrf(v3);
      Interface c3i1 = _ib.setName(c3i1Name).build();
      c3i1.setSwitchport(true);
      c3i1.setSwitchportMode(SwitchportMode.TRUNK);
      c3i1.setAllowedVlans(IntegerSpace.of(new SubRange(1, 3)));
      c3i1.setNativeVlan(1);
      Interface c3i2 = _ib.setName(c3i2Name).build();
      c3i2.setSwitchport(true);
      c3i2.setSwitchportMode(SwitchportMode.ACCESS);
      c3i2.setAccessVlan(1);
      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2, c3Name, c3);
      Layer1Topology layer1Topology =
          layer1Topology(
              c1Name, c1i1Name, c3Name, c3i1Name, //
              c2Name, c2i1Name, c3Name, c3i2Name);
      Layer2Topology layer2Topology =
          computeLayer2Topology(layer1Topology, VxlanTopology.EMPTY, configs);
      assertTrue(
          "c1:i1 and c2:i1 are in the same broadcast domain",
          layer2Topology.inSameBroadcastDomain(c1Name, c1i1Name, c2Name, c2i1Name));
    }

    {
      /* c1i1 and c2i1 are not connected in layer1, but are connected to TRUNK and ACCESS ports, and
       * the ACCESS port's VLAN is allowed by the TRUNK, but not it's native VLAN.
       */
      Configuration c3 = _cb.setHostname(c3Name).build();
      Vrf v3 = _vb.setOwner(c3).build();
      _ib.setOwner(c3).setVrf(v3);
      Interface c3i1 = _ib.setName(c3i1Name).build();
      c3i1.setSwitchport(true);
      c3i1.setSwitchportMode(SwitchportMode.TRUNK);
      c3i1.setAllowedVlans(IntegerSpace.of(new SubRange(1, 3)));
      c3i1.setNativeVlan(1);
      Interface c3i2 = _ib.setName(c3i2Name).build();
      c3i2.setSwitchport(true);
      c3i2.setSwitchportMode(SwitchportMode.ACCESS);
      c3i2.setAccessVlan(2);
      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2, c3Name, c3);
      Layer1Topology layer1Topology =
          layer1Topology(
              c1Name, c1i1Name, c3Name, c3i1Name, //
              c2Name, c2i1Name, c3Name, c3i2Name);
      Layer2Topology layer2Topology =
          computeLayer2Topology(layer1Topology, VxlanTopology.EMPTY, configs);
      assertFalse(
          "c1:i1 and c2:i1 are not in the same broadcast domain",
          layer2Topology.inSameBroadcastDomain(c1Name, c1i1Name, c2Name, c2i1Name));
    }

    {
      /* c1i1 and c2i1 are not connected in layer1, and are connected to TRUNK and ACCESS ports with
       * incompatible VLANs.
       */
      Configuration c3 = _cb.setHostname(c3Name).build();
      Vrf v3 = _vb.setOwner(c3).build();
      _ib.setOwner(c3).setVrf(v3);
      Interface c3i1 = _ib.setName(c3i1Name).build();
      c3i1.setSwitchport(true);
      c3i1.setSwitchportMode(SwitchportMode.TRUNK);
      c3i1.setAllowedVlans(IntegerSpace.of(new SubRange(1, 3)));
      c3i1.setNativeVlan(1);
      Interface c3i2 = _ib.setName(c3i2Name).build();
      c3i2.setSwitchport(true);
      c3i2.setSwitchportMode(SwitchportMode.ACCESS);
      c3i2.setAccessVlan(4);
      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2, c3Name, c3);
      Layer1Topology layer1Topology =
          layer1Topology(
              c1Name, c1i1Name, c3Name, c3i1Name, //
              c2Name, c2i1Name, c3Name, c3i2Name);
      Layer2Topology layer2Topology =
          computeLayer2Topology(layer1Topology, VxlanTopology.EMPTY, configs);
      assertFalse(
          "c1:i1 and c2:i1 are not in the same broadcast domain",
          layer2Topology.inSameBroadcastDomain(c1Name, c1i1Name, c2Name, c2i1Name));
    }

    {
      /* c1i1 and c2i1 are not connected in layer1, and are connected to ACCESS ports with two
       * TRUNKs between them.
       */
      Configuration c3 = _cb.setHostname(c3Name).build();
      Vrf v3 = _vb.setOwner(c3).build();
      _ib.setOwner(c3).setVrf(v3);
      Interface c3i1 = _ib.setName(c3i1Name).build();
      c3i1.setSwitchport(true);
      c3i1.setSwitchportMode(SwitchportMode.ACCESS);
      c3i1.setAccessVlan(2);
      Interface c3i2 = _ib.setName(c3i2Name).build();
      c3i2.setSwitchport(true);
      c3i2.setSwitchportMode(SwitchportMode.TRUNK);
      c3i2.setAllowedVlans(IntegerSpace.of(new SubRange(1, 3)));
      c3i2.setNativeVlan(1);

      Configuration c4 = _cb.setHostname(c4Name).build();
      Vrf v4 = _vb.setOwner(c4).build();
      _ib.setOwner(c4).setVrf(v4);
      Interface c4i1 = _ib.setName(c4i1Name).build();
      c4i1.setSwitchport(true);
      c4i1.setSwitchportMode(SwitchportMode.TRUNK);
      c4i1.setAllowedVlans(IntegerSpace.of(new SubRange(1, 3)));
      c4i1.setNativeVlan(1);
      Interface c4i2 = _ib.setName(c4i2Name).build();
      c4i2.setSwitchport(true);
      c4i2.setSwitchportMode(SwitchportMode.ACCESS);
      c4i2.setAccessVlan(2);

      Map<String, Configuration> configs =
          ImmutableMap.of(c1Name, c1, c2Name, c2, c3Name, c3, c4Name, c4);
      Layer1Topology layer1Topology =
          layer1Topology(
              c1Name, c1i1Name, c3Name, c3i1Name, //
              c3Name, c3i2Name, c4Name, c4i1Name, //
              c4Name, c4i2Name, c2Name, c2i1Name);
      Layer2Topology layer2Topology =
          computeLayer2Topology(layer1Topology, VxlanTopology.EMPTY, configs);
      assertTrue(
          "c1:i1 and c2:i1 are in the same broadcast domain",
          layer2Topology.inSameBroadcastDomain(c1Name, c1i1Name, c2Name, c2i1Name));
    }

    {
      /* c1i1 and c2i1 are not connected in layer1, and are connected to ACCESS ports with TRUNKs
       * between them. Not in the same broadcast domain, because the VLAN tagging doesn't line up
       */
      Configuration c3 = _cb.setHostname(c3Name).build();
      Vrf v3 = _vb.setOwner(c3).build();
      _ib.setOwner(c3).setVrf(v3);
      Interface c3i1 = _ib.setName(c3i1Name).build();
      c3i1.setSwitchport(true);
      c3i1.setSwitchportMode(SwitchportMode.ACCESS);
      c3i1.setAccessVlan(2);
      Interface c3i2 = _ib.setName(c3i2Name).build();
      c3i2.setSwitchport(true);
      c3i2.setSwitchportMode(SwitchportMode.TRUNK);
      c3i2.setAllowedVlans(IntegerSpace.of(new SubRange(1, 3)));
      c3i2.setNativeVlan(1);

      Configuration c4 = _cb.setHostname(c4Name).build();
      Vrf v4 = _vb.setOwner(c4).build();
      _ib.setOwner(c4).setVrf(v4);
      Interface c4i1 = _ib.setName(c4i1Name).build();
      c4i1.setSwitchport(true);
      c4i1.setSwitchportMode(SwitchportMode.ACCESS);
      c4i1.setAccessVlan(2);

      Interface c4i2 = _ib.setName(c4i2Name).build();
      c4i2.setSwitchport(true);
      c4i2.setSwitchportMode(SwitchportMode.ACCESS);
      c4i2.setAccessVlan(2);

      Map<String, Configuration> configs =
          ImmutableMap.of(c1Name, c1, c2Name, c2, c3Name, c3, c4Name, c4);
      Layer1Topology layer1Topology =
          layer1Topology(
              c1Name, c1i1Name, c3Name, c3i1Name, //
              c3Name, c3i2Name, c4Name, c4i2Name, //
              c4Name, c4i1Name, c2Name, c2i1Name);
      Layer2Topology layer2Topology =
          computeLayer2Topology(layer1Topology, VxlanTopology.EMPTY, configs);
      assertFalse(
          "c1:i1 and c2:i1 are not in the same broadcast domain",
          layer2Topology.inSameBroadcastDomain(c1Name, c1i1Name, c2Name, c2i1Name));
    }
  }

  @Test
  public void testComputeLayer2SelfEdges() {
    String c1Name = "c1";

    String i1Name = "i1";
    String i2Name = "i2";
    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1).setActive(true);
    Interface i1 = _ib.setName(i1Name).build();
    i1.setSwitchport(true);
    i1.setSwitchportMode(SwitchportMode.ACCESS);
    i1.setAccessVlan(2);

    Vrf v2 = _vb.setOwner(c1).build();
    Interface i2 = _ib.setVrf(v2).setName(i2Name).build();
    i2.setSwitchport(true);
    i2.setSwitchportMode(SwitchportMode.ACCESS);
    i2.setAccessVlan(2);

    ImmutableSet.Builder<Layer2Edge> builder = ImmutableSet.builder();
    computeLayer2SelfEdges(c1, builder::add);

    assertThat(
        builder.build(),
        equalTo(
            ImmutableSet.of(
                new Layer2Edge(c1Name, i1Name, 2, c1Name, i2Name, 2, null),
                new Layer2Edge(c1Name, i2Name, 2, c1Name, i1Name, 2, null))));
  }

  @Test
  public void testComputeLayer2SelfEdgesVniIrb() {
    String c1Name = "c1";

    int vlanId = 2;
    int vni = 10002;

    String irbName = "irb1";
    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1)
        .setVrf(v1)
        .setActive(true)
        .setName(irbName)
        .setType(InterfaceType.VLAN)
        .setVlan(vlanId)
        .build();

    String vniName = TopologyUtil.computeVniName(vni);
    v1.getVniSettings()
        .put(
            vni,
            VniSettings.builder()
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                .setBumTransportIps(ImmutableSortedSet.of(Ip.FIRST_CLASS_B_PRIVATE_IP))
                .setVni(vni)
                .setVlan(vlanId)
                .build());

    ImmutableSet.Builder<Layer2Edge> builder = ImmutableSet.builder();
    computeLayer2SelfEdges(c1, builder::add);

    assertThat(
        builder.build(),
        equalTo(
            ImmutableSet.of(
                new Layer2Edge(c1Name, irbName, null, c1Name, vniName, null, null),
                new Layer2Edge(c1Name, vniName, null, c1Name, irbName, null, null))));
  }

  @Test
  public void testComputeLayer2SelfEdgesVniSwitchport() {
    String c1Name = "c1";

    int vlanId = 2;
    int vni = 10002;

    String switchportName = "swp1";
    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1)
        .setVrf(v1)
        .setActive(true)
        .setName(switchportName)
        .setType(InterfaceType.PHYSICAL)
        .setAccessVlan(vlanId)
        .setSwitchport(true)
        .setSwitchportMode(SwitchportMode.ACCESS)
        .build();

    String vniName = TopologyUtil.computeVniName(vni);
    v1.getVniSettings()
        .put(
            vni,
            VniSettings.builder()
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                .setBumTransportIps(ImmutableSortedSet.of(Ip.FIRST_CLASS_B_PRIVATE_IP))
                .setVni(vni)
                .setVlan(vlanId)
                .build());

    ImmutableSet.Builder<Layer2Edge> builder = ImmutableSet.builder();
    computeLayer2SelfEdges(c1, builder::add);

    assertThat(
        builder.build(),
        equalTo(
            ImmutableSet.of(
                new Layer2Edge(c1Name, switchportName, vlanId, c1Name, vniName, null, null),
                new Layer2Edge(c1Name, vniName, null, c1Name, switchportName, vlanId, null))));
  }

  @Test
  public void testComputeLayer2TopologyTaggedLayer3ToTaggedLayer3() {
    String n1Name = "n1";
    String n2Name = "n2";
    String i1Name = "i1";
    String i1aName = "i1a";
    String i1bName = "i1b";
    int i1aVlan = 10;
    int i1bVlan = 20;

    // Nodes
    Configuration n1 = _cb.setHostname(n1Name).build();
    Configuration n2 = _cb.setHostname(n2Name).build();

    // Vrfs
    Vrf v1 = _vb.setOwner(n1).build();
    Vrf v2 = _vb.setOwner(n2).build();

    // Interfaces
    _ib.setActive(true);
    // n1 interfaces
    _ib.setOwner(n1).setVrf(v1);
    _ib.setName(i1Name).setDependencies(ImmutableList.of()).setEncapsulationVlan(null).build();
    _ib.setDependencies(ImmutableList.of(new Dependency(i1Name, DependencyType.BIND)));
    _ib.setName(i1aName).setEncapsulationVlan(i1aVlan).build();
    _ib.setName(i1bName).setEncapsulationVlan(i1bVlan).build();
    // n2 interfaces
    _ib.setOwner(n2).setVrf(v2);
    _ib.setName(i1Name).setDependencies(ImmutableList.of()).setEncapsulationVlan(null).build();
    _ib.setDependencies(ImmutableList.of(new Dependency(i1Name, DependencyType.BIND)));
    _ib.setName(i1aName).setEncapsulationVlan(i1aVlan).build();
    _ib.setName(i1bName).setEncapsulationVlan(i1bVlan).build();

    // Layer1
    Layer1Topology layer1LogicalTopology =
        layer1Topology(
            n1Name, i1Name, n2Name, i1Name //
            );

    // Layer2
    Layer2Topology layer2Topology =
        computeLayer2Topology(
            layer1LogicalTopology, VxlanTopology.EMPTY, ImmutableMap.of(n1Name, n1, n2Name, n2));

    assertTrue(
        "n1:i1a and n2:i1a are in the same broadcast domain",
        layer2Topology.inSameBroadcastDomain(n1Name, i1aName, n2Name, i1aName));
    assertTrue(
        "n1:i1b and n2:i1b are in the same broadcast domain",
        layer2Topology.inSameBroadcastDomain(n1Name, i1bName, n2Name, i1bName));
    assertFalse(
        "n1:i1a and n2:i1b are NOT in the same broadcast domain",
        layer2Topology.inSameBroadcastDomain(n1Name, i1aName, n2Name, i1bName));
  }

  @Test
  public void testComputeLayer2TopologyTaggedLayer3ToTrunk() {
    // n1:iTagged <=> n2:iTrunkParent

    // n1:iTagged children:
    // - n1:ia - tag is 10
    // - n1:ib - tag is 20
    // - n1:ic - tag is 30

    // n2:iTrunkParent child is n2:iTrunk
    // n2:iTrunk:
    // - native 20
    // - allowed: 10,20
    // n2 has four IRB interfaces:
    // - n2:ia - vlan 10
    // - n2:ib - vlan 20
    // - n2:ic - vlan 30
    // - n2:id - null vlan

    // we expect:
    // D(n1:ia)=D(n2:ia) // tags match (10=10)
    // D(n1:ib)!=D(n2:ib) // trunk does not send tag on native vlan
    // D(n1:ic)!=D(n2:ic) // trunk does not allow traffic with this tag
    // nothing crashes with null vlan on id

    String n1Name = "n1";
    String n2Name = "n2";
    String iTaggedName = "iTagged";
    String iTrunkParentName = "iTrunkParent";
    String iTrunkName = "iTrunk";
    String iaName = "ia";
    String ibName = "ib";
    String icName = "ic";
    String idName = "id";
    int iaVlan = 10;
    int ibVlan = 20;
    int icVlan = 30;

    // Nodes
    Configuration n1 = _cb.setHostname(n1Name).build();
    Configuration n2 = _cb.setHostname(n2Name).build();

    // Vrfs
    Vrf v1 = _vb.setOwner(n1).build();
    Vrf v2 = _vb.setOwner(n2).build();

    // Interfaces
    _ib.setActive(true);
    // n1 interfaces
    _ib.setOwner(n1).setVrf(v1);
    // parent interface that multiplexes based on tags
    _ib.setName(iTaggedName).setDependencies(ImmutableList.of()).setEncapsulationVlan(null).build();
    _ib.setDependencies(ImmutableList.of(new Dependency(iTaggedName, DependencyType.BIND)));
    _ib.setName(iaName).setEncapsulationVlan(iaVlan).build();
    _ib.setName(ibName).setEncapsulationVlan(ibVlan).build();
    _ib.setName(icName).setEncapsulationVlan(icVlan).build();
    _ib.setName(idName).setEncapsulationVlan(null).build();
    // n2 interfaces
    _ib.setOwner(n2).setVrf(v2);
    _ib.setDependencies(ImmutableList.of()).setEncapsulationVlan(null);
    Interface vlanA = _ib.setName(iaName).build();
    vlanA.setInterfaceType(InterfaceType.VLAN);
    vlanA.setVlan(iaVlan);
    Interface vlanB = _ib.setName(ibName).build();
    vlanB.setInterfaceType(InterfaceType.VLAN);
    vlanB.setVlan(ibVlan);
    Interface vlanC = _ib.setName(icName).build();
    vlanC.setInterfaceType(InterfaceType.VLAN);
    vlanC.setVlan(icVlan);
    Interface vlanD = _ib.setName(idName).build();
    vlanD.setInterfaceType(InterfaceType.VLAN);
    vlanD.setVlan(null);
    _ib.setName(iTrunkParentName).build();
    Interface trunk =
        _ib.setName(iTrunkName)
            .setDependencies(
                ImmutableList.of(new Dependency(iTrunkParentName, DependencyType.BIND)))
            .build();
    trunk.setNativeVlan(ibVlan);
    trunk.setAllowedVlans(IntegerSpace.builder().including(iaVlan).including(ibVlan).build());
    trunk.setSwitchport(true);
    trunk.setSwitchportMode(SwitchportMode.TRUNK);

    // Layer1
    Layer1Topology layer1LogicalTopology =
        layer1Topology(
            n1Name, iTaggedName, n2Name, iTrunkParentName //
            );

    // Layer2
    Layer2Topology layer2Topology =
        computeLayer2Topology(
            layer1LogicalTopology, VxlanTopology.EMPTY, ImmutableMap.of(n1Name, n1, n2Name, n2));

    assertTrue(
        "n1:ia and n2:ia are in the same broadcast domain",
        layer2Topology.inSameBroadcastDomain(n1Name, iaName, n2Name, iaName));
    assertFalse(
        "n1:ib and n2:ib are NOT in the same broadcast domain",
        layer2Topology.inSameBroadcastDomain(n1Name, ibName, n2Name, ibName));
    assertFalse(
        "n1:ic and n2:ic are NOT in the same broadcast domain",
        layer2Topology.inSameBroadcastDomain(n1Name, icName, n2Name, icName));
    assertFalse(
        "n1:ia and n2:ib are NOT in the same broadcast domain",
        layer2Topology.inSameBroadcastDomain(n1Name, iaName, n2Name, ibName));
    assertFalse(
        "n1:ia and n2:ic are NOT in the same broadcast domain",
        layer2Topology.inSameBroadcastDomain(n1Name, iaName, n2Name, icName));
  }

  @Test
  public void testComputeLayer3Topology() {
    String c1Name = "c1";
    String c2Name = "c2";

    String c1i1Name = "c1i1";
    String c2i1Name = "c2i1";

    Layer1Node l1c1i1 = new Layer1Node(c1Name, c1i1Name);
    Layer1Node l1c2i1 = new Layer1Node(c2Name, c2i1Name);

    Layer2Node c1i1 = new Layer2Node(c1Name, c1i1Name, null);
    Layer2Node c2i1 = new Layer2Node(c2Name, c2i1Name, null);

    Edge c1i1c2i1 = Edge.of(c1Name, c1i1Name, c2Name, c2i1Name);
    Edge c2i1c1i1 = Edge.of(c2Name, c2i1Name, c1Name, c1i1Name);

    Configuration c1 = _cb.setHostname(c1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1).setActive(true);

    Configuration c2 = _cb.setHostname(c2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2);

    ConcreteInterfaceAddress p1Addr1 = ConcreteInterfaceAddress.parse("1.0.0.1/24");
    ConcreteInterfaceAddress p1Addr2 = ConcreteInterfaceAddress.parse("1.0.0.2/24");
    ConcreteInterfaceAddress p2Addr1 = ConcreteInterfaceAddress.parse("2.0.0.1/24");

    Layer1Topology rawL1AllPresent =
        new Layer1Topology(
            ImmutableList.of(new Layer1Edge(l1c1i1, l1c2i1), new Layer1Edge(l1c2i1, l1c1i1)));
    Layer1Topology rawL1NonePresent = new Layer1Topology(ImmutableList.of());

    Layer2Topology sameDomain =
        Layer2Topology.fromDomains(ImmutableList.of(ImmutableSet.of(c1i1, c2i1)));
    Layer2Topology differentDomains =
        Layer2Topology.fromDomains(ImmutableList.of(ImmutableSet.of(c1i1), ImmutableSet.of(c2i1)));

    {
      // c1i1 and c2i1 are in the same subnet and the same broadcast domain, so connected at layer3
      _ib.setOwner(c1).setVrf(v1).setName(c1i1Name).setAddress(p1Addr1).build();
      _ib.setOwner(c2).setVrf(v2).setName(c2i1Name).setAddress(p1Addr2).build();

      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2);
      Topology layer3Topology =
          computeRawLayer3Topology(rawL1AllPresent, Layer1Topology.EMPTY, sameDomain, configs);
      assertThat(layer3Topology.getEdges(), containsInAnyOrder(c1i1c2i1, c2i1c1i1));
    }

    {
      // c1i1 and c2i1 are in different subnets, and different broadcast domains. not connected
      _ib.setOwner(c1).setVrf(v1).setName(c1i1Name).setAddress(p1Addr1).build();
      _ib.setOwner(c2).setVrf(v2).setName(c2i1Name).setAddress(p2Addr1).build();

      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2);
      Topology layer3Topology =
          computeRawLayer3Topology(
              rawL1AllPresent, Layer1Topology.EMPTY, differentDomains, configs);
      assertThat(layer3Topology.getEdges(), empty());
    }

    {
      // c1i1 and c2i1 are in the same broadcast domain but different subnets. not connected
      _ib.setOwner(c1).setVrf(v1).setName(c1i1Name).setAddress(p1Addr1).build();
      _ib.setOwner(c2).setVrf(v2).setName(c2i1Name).setAddress(p2Addr1).build();

      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2);
      Topology layer3Topology =
          computeRawLayer3Topology(rawL1AllPresent, Layer1Topology.EMPTY, sameDomain, configs);
      assertThat(layer3Topology.getEdges(), empty());
    }

    {
      // c1i1 and c2i1 are in the same subnet but different broadcast domains. not connected
      _ib.setOwner(c1).setVrf(v1).setName(c1i1Name).setAddress(p1Addr1).build();
      _ib.setOwner(c2).setVrf(v2).setName(c2i1Name).setAddress(p1Addr2).build();

      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2);
      Topology layer3Topology =
          computeRawLayer3Topology(
              rawL1AllPresent, Layer1Topology.EMPTY, differentDomains, configs);
      assertThat(layer3Topology.getEdges(), empty());
    }

    {
      // c1i1 and c2i1 are in the same subnet, and insufficient information exists in L1 to prune.
      // layer-2 information should be ignored, so connected at layer3.
      _ib.setOwner(c1).setVrf(v1).setName(c1i1Name).setAddress(p1Addr1).build();
      _ib.setOwner(c2).setVrf(v2).setName(c2i1Name).setAddress(p1Addr2).build();

      Map<String, Configuration> configs = ImmutableMap.of(c1Name, c1, c2Name, c2);
      Topology layer3Topology =
          computeRawLayer3Topology(
              rawL1NonePresent, Layer1Topology.EMPTY, differentDomains, configs);
      assertThat(layer3Topology.getEdges(), containsInAnyOrder(c1i1c2i1, c2i1c1i1));
    }
  }

  @Test
  public void testIncompleteLayer1TopologyHandlingIsp() {
    /*
     * Connectivity between border routers and generated ISP nodes
     * Expected: h1 <=> b1 <=> INTERNET <=> b2 <=> h2
     * Provided: h1 <=> b1   b2 <=> h2
     * Use case: b1 and b2 are declared as border routers; topological connectivity should be
     *           synthesized via generated ISP nodes
     */
    String b1Name = "b1";
    String b2Name = "b2";
    String h1Name = "h1";
    String h2Name = "h2";

    String i1Name = "i1";
    String i2Name = "i2";

    Layer1Node l1B1 = new Layer1Node(b1Name, i1Name);
    Layer1Node l1B2 = new Layer1Node(b2Name, i1Name);
    Layer1Node l1H1 = new Layer1Node(h1Name, i1Name);
    Layer1Node l1H2 = new Layer1Node(h2Name, i1Name);

    ConcreteInterfaceAddress b1i2Address = ConcreteInterfaceAddress.parse("10.0.0.0/31");
    Ip internetToB1Address = Ip.parse("10.0.0.1");
    ConcreteInterfaceAddress b2i2Address = ConcreteInterfaceAddress.parse("10.0.0.2/31");
    Ip internetToB2Address = Ip.parse("10.0.0.3");

    long asB1 = 1;
    long asInternetToB1 = 2;
    long asB2 = 3;
    long asInternetToB2 = 4;

    Configuration cH1 = _cb.setHostname(h1Name).build();
    Vrf vH1 = _vb.setOwner(cH1).build();
    _ib.setOwner(cH1).setVrf(vH1).setName(i1Name).build(); // h1 => b1

    Configuration cH2 = _cb.setHostname(h2Name).build();
    Vrf vH2 = _vb.setOwner(cH2).build();
    _ib.setOwner(cH2).setVrf(vH2).setName(i1Name).build(); // h2 => b2

    BgpProcess.Builder pb =
        _nf.bgpProcessBuilder().setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS);

    Configuration cB1 = _cb.setHostname(b1Name).build();
    Vrf vB1 = _vb.setOwner(cB1).build();
    _ib.setOwner(cB1).setVrf(vB1).setName(i1Name).build(); // b1 => h1
    _ib.setName(i2Name).setAddress(b1i2Address).build(); // b1 => INTERNET
    BgpProcess b1Proc = pb.setRouterId(b1i2Address.getIp()).setVrf(vB1).build();
    _nf.bgpNeighborBuilder()
        .setBgpProcess(b1Proc)
        .setLocalAs(asB1)
        .setRemoteAs(asInternetToB1)
        .setLocalIp(b1i2Address.getIp())
        .setPeerAddress(internetToB1Address)
        .build();

    Configuration cB2 = _cb.setHostname(b2Name).build();
    Vrf vB2 = _vb.setOwner(cB2).build();
    _ib.setOwner(cB2).setVrf(vB2).setName(i1Name).setAddress(null).build(); // b2 => h2
    _ib.setName(i2Name).setAddress(b2i2Address).build(); // B2 => INTERNET
    BgpProcess b2Proc = pb.setRouterId(b2i2Address.getIp()).setVrf(vB2).build();
    _nf.bgpNeighborBuilder()
        .setBgpProcess(b2Proc)
        .setLocalAs(asB2)
        .setRemoteAs(asInternetToB2)
        .setLocalIp(b2i2Address.getIp())
        .setPeerAddress(internetToB2Address)
        .build();

    Layer1Topology rawLayer1Topology =
        new Layer1Topology(
            ImmutableList.of(
                new Layer1Edge(l1B1, l1H1),
                new Layer1Edge(l1H1, l1B1),
                new Layer1Edge(l1B2, l1H2),
                new Layer1Edge(l1H2, l1B2)));
    Map<String, Configuration> explicitConfigurations =
        ImmutableMap.of(h1Name, cH1, h2Name, cH2, b1Name, cB1, b2Name, cB2);
    IspConfiguration ispConfiguration =
        new IspConfiguration(
            ImmutableList.of(
                new BorderInterfaceInfo(new NodeInterfacePair(b1Name, i2Name)),
                new BorderInterfaceInfo(new NodeInterfacePair(b2Name, i2Name))),
            new IspFilter(ImmutableList.of(), ImmutableList.of()));
    Map<String, Configuration> ispConfigurations =
        IspModelingUtils.getInternetAndIspNodes(
            explicitConfigurations,
            ispConfiguration,
            new BatfishLogger(BatfishLogger.LEVELSTR_ERROR, false),
            new Warnings());
    Map<String, Configuration> configurations =
        ImmutableMap.<String, Configuration>builder()
            .putAll(explicitConfigurations)
            .putAll(ispConfigurations)
            .build();

    Layer1Topology layer1PhysicalTopology =
        computeLayer1PhysicalTopology(rawLayer1Topology, configurations);

    // Layer-1 physical topology should include edges in each direction between each border router
    // and corresponding host
    assertThat(
        layer1PhysicalTopology.getGraph().edges(),
        containsInAnyOrder(
            new Layer1Edge(l1B1, l1H1),
            new Layer1Edge(l1H1, l1B1),
            new Layer1Edge(l1B2, l1H2),
            new Layer1Edge(l1H2, l1B2)));

    Topology layer3Topology =
        computeRawLayer3Topology(
            rawLayer1Topology,
            Layer1Topology.EMPTY,
            computeLayer2Topology(
                computeLayer1LogicalTopology(layer1PhysicalTopology, configurations),
                VxlanTopology.EMPTY,
                configurations),
            configurations);

    NodeInterfacePair l3B1 = new NodeInterfacePair(b1Name, i2Name);
    NodeInterfacePair l3B2 = new NodeInterfacePair(b2Name, i2Name);

    Set<String> explicitNodes = explicitConfigurations.keySet();
    Set<String> ispNodes = ispConfigurations.keySet();

    // Layer-3 topology should include edges in each direction between each border router and
    // generated ISP node
    assertThat(
        layer3Topology.getEdges().stream()
            .filter(
                edge ->
                    explicitNodes.contains(edge.getNode1())
                        || explicitNodes.contains(edge.getNode2()))
            .collect(ImmutableSet.toImmutableSet()),
        containsInAnyOrder(
            both(hasHead(l3B1)).and(hasNode1(in(ispNodes))), // INTERNET => b1
            both(hasTail(l3B1)).and(hasNode2(in(ispNodes))), // b1 => INTERNET
            both(hasHead(l3B2)).and(hasNode1(in(ispNodes))), // INTERNET => b2
            both(hasTail(l3B2)).and(hasNode2(in(ispNodes))))); // b2 => INTERNET

    // Layer-3 topology should also contain other synthetic edges for the modeled ISPs
    assertThat(
        layer3Topology.getEdges().stream()
            .filter(
                edge -> ispNodes.contains(edge.getNode1()) && ispNodes.contains(edge.getNode2()))
            .collect(ImmutableSet.toImmutableSet()),
        not(empty()));
  }

  @Test
  public void testIncompleteLayer1TopologyHandlingOneSided() {
    /*
     * One-sided Layer-1 edges
     * Expected L1: N1 <=> N2
     * Provided L1: N1 => N2
     * Use case: L1 input is missing info from N2 due to snapshot preparation problem
     */
    String n1Name = "N1";
    String n2Name = "N2";
    String iName = "i1";

    Layer1Node n1 = new Layer1Node(n1Name, iName);
    Layer1Node n2 = new Layer1Node(n2Name, iName);

    _ib.setActive(true).setName(iName);

    Configuration c1 = _cb.setHostname(n1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1).build();

    Configuration c2 = _cb.setHostname(n2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2).build();

    Layer1Topology rawLayer1Topology = new Layer1Topology(ImmutableList.of(new Layer1Edge(n1, n2)));
    Map<String, Configuration> configurations = ImmutableMap.of(n1Name, c1, n2Name, c2);
    Layer1Topology layer1PhysicalTopology =
        TopologyUtil.computeLayer1PhysicalTopology(rawLayer1Topology, configurations);

    // Layer-1 physical topology should include edges in each direction
    assertThat(
        layer1PhysicalTopology.getGraph().edges(),
        containsInAnyOrder(new Layer1Edge(n1, n2), new Layer1Edge(n2, n1)));
  }

  @Test
  public void testIncompleteLayer1TopologyHandlingInconsistentAvailability() {
    /*
     * Inconsistent availability of Layer-1 information
     * Expected L1: N1 <=> N2 <=> N3 <=> N4
     *          L3: N2 <=> N3 <=> N4
     * Provided L1: N1 <=> N2
     * Use case: L1 information is unavailable for N3 and N4
     */
    String n1Name = "N1";
    String n2Name = "N2";
    String n3Name = "N3";
    String n4Name = "N4";
    String i1Name = "i1";
    String i2Name = "i2";

    Layer1Node l1n1 = new Layer1Node(n1Name, i1Name);
    Layer1Node l1n2 = new Layer1Node(n2Name, i1Name);

    NodeInterfacePair l3n2i2 = new NodeInterfacePair(n2Name, i2Name);
    NodeInterfacePair l3n3i1 = new NodeInterfacePair(n3Name, i1Name);
    NodeInterfacePair l3n3i2 = new NodeInterfacePair(n3Name, i2Name);
    NodeInterfacePair l3n4i1 = new NodeInterfacePair(n4Name, i1Name);

    _ib.setActive(true);

    ConcreteInterfaceAddress n2n3Address = ConcreteInterfaceAddress.parse("10.0.0.0/31");
    ConcreteInterfaceAddress n3n2Address = ConcreteInterfaceAddress.parse("10.0.0.1/31");
    ConcreteInterfaceAddress n3n4Address = ConcreteInterfaceAddress.parse("10.0.0.2/31");
    ConcreteInterfaceAddress n4n3Address = ConcreteInterfaceAddress.parse("10.0.0.3/31");

    Configuration c1 = _cb.setHostname(n1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1).setName(i1Name).build();

    Configuration c2 = _cb.setHostname(n2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2).build(); // N2=>N1
    _ib.setName(i2Name).setAddress(n2n3Address).build(); // N2=>N3

    Configuration c3 = _cb.setHostname(n3Name).build();
    Vrf v3 = _vb.setOwner(c3).build();
    _ib.setOwner(c3).setVrf(v3).setName(i1Name).setAddress(n3n2Address).build(); // N3=>N2
    _ib.setName(i2Name).setAddress(n3n4Address).build(); // N3=>N4

    Configuration c4 = _cb.setHostname(n4Name).build();
    Vrf v4 = _vb.setOwner(c4).build();
    _ib.setOwner(c4).setVrf(v4).setName(i1Name).setAddress(n4n3Address).build(); // N4=>N3

    Layer1Topology rawLayer1Topology =
        new Layer1Topology(
            ImmutableList.of(new Layer1Edge(l1n1, l1n2), new Layer1Edge(l1n2, l1n1)));
    Map<String, Configuration> configurations =
        ImmutableMap.of(n1Name, c1, n2Name, c2, n3Name, c3, n4Name, c4);
    Layer1Topology layer1PhysicalTopology =
        computeLayer1PhysicalTopology(rawLayer1Topology, configurations);

    // Layer-1 physical topology should include edges in each direction
    assertThat(
        layer1PhysicalTopology.getGraph().edges(),
        containsInAnyOrder(new Layer1Edge(l1n1, l1n2), new Layer1Edge(l1n2, l1n1)));

    Topology layer3Topology =
        computeRawLayer3Topology(
            rawLayer1Topology,
            Layer1Topology.EMPTY,
            computeLayer2Topology(
                computeLayer1LogicalTopology(layer1PhysicalTopology, configurations),
                VxlanTopology.EMPTY,
                configurations),
            configurations);

    // Layer-3 topology should include edges in each direction for n2-n3, n3-n4
    assertThat(
        layer3Topology.getEdges(),
        containsInAnyOrder(
            new Edge(l3n2i2, l3n3i1), // n2=>n3
            new Edge(l3n3i1, l3n2i2), // n3=>n2
            new Edge(l3n3i2, l3n4i1), // n3=>n4
            new Edge(l3n4i1, l3n3i2))); // n4=>n3
  }

  @Test
  public void testIncompleteLayer1TopologyHandlingUnusable() {
    /*
     * Incorrect/Unusable Layer-1 information
     * Expected L1: N1 <=> N2
     * Provided L1: N1 => N2, N2 => NCorrupt
     * Use case: L1 input has corrupt (e.g. truncated) info from N2 due to snapshot preparation problem
     */
    String n1Name = "N1";
    String n2Name = "N2";
    String nCorruptName = "N";
    String iName = "i1";

    Layer1Node n1 = new Layer1Node(n1Name, iName);
    Layer1Node n2 = new Layer1Node(n2Name, iName);
    Layer1Node nCorrupt = new Layer1Node(nCorruptName, iName);

    _ib.setActive(true).setName(iName);

    Configuration c1 = _cb.setHostname(n1Name).build();
    Vrf v1 = _vb.setOwner(c1).build();
    _ib.setOwner(c1).setVrf(v1).build();

    Configuration c2 = _cb.setHostname(n2Name).build();
    Vrf v2 = _vb.setOwner(c2).build();
    _ib.setOwner(c2).setVrf(v2).build();

    Layer1Topology rawLayer1Topology =
        new Layer1Topology(ImmutableList.of(new Layer1Edge(n1, n2), new Layer1Edge(n2, nCorrupt)));
    Map<String, Configuration> configurations = ImmutableMap.of(n1Name, c1, n2Name, c2);
    Layer1Topology layer1PhysicalTopology =
        TopologyUtil.computeLayer1PhysicalTopology(rawLayer1Topology, configurations);

    // Layer-1 physical topology should include edges in each direction between n1 and n2, and throw
    // out corrupt edge.
    assertThat(
        layer1PhysicalTopology.getGraph().edges(),
        containsInAnyOrder(new Layer1Edge(n1, n2), new Layer1Edge(n2, n1)));
  }

  /**
   * Tests that inactive and blacklisted interfaces are properly included or excluded from the
   * output of {@link IpOwners#computeIpInterfaceOwners(Map, boolean)}
   */
  @Test
  public void testIpInterfaceOwnersActiveInclusion() {
    Map<String, Set<Interface>> nodeInterfaces =
        ImmutableMap.of(
            "node",
            ImmutableSet.of(
                iface("active", "1.1.1.1/32", true, false),
                iface("shut", "1.1.1.1/32", false, false),
                iface("active-black", "1.1.1.1/32", true, true),
                iface("shut-black", "1.1.1.1/32", false, true)));

    assertThat(
        computeIpInterfaceOwners(nodeInterfaces, true),
        equalTo(
            ImmutableMap.of(
                Ip.parse("1.1.1.1"), ImmutableMap.of("node", ImmutableSet.of("active")))));

    assertThat(
        computeIpInterfaceOwners(nodeInterfaces, false),
        equalTo(
            ImmutableMap.of(
                Ip.parse("1.1.1.1"),
                ImmutableMap.of(
                    "node", ImmutableSet.of("active", "shut", "active-black", "shut-black")))));
  }

  @Test
  public void testSynthesizeTopology_asymmetric() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Configuration c2 = cb.build();
    Interface i1 =
        nf.interfaceBuilder()
            .setOwner(c1)
            .setAddresses(ConcreteInterfaceAddress.parse("1.2.3.4/24"))
            .build();
    Interface i2 =
        nf.interfaceBuilder()
            .setOwner(c2)
            .setAddresses(ConcreteInterfaceAddress.parse("1.2.3.5/28"))
            .build();
    Topology t =
        TopologyUtil.synthesizeL3Topology(
            ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2));
    assertThat(t.getEdges(), equalTo(ImmutableSet.of(new Edge(i1, i2), new Edge(i2, i1))));
  }

  @Test
  public void testSynthesizeTopology_selfEdges() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf v1 = nf.vrfBuilder().setOwner(c).setName("v1").build();
    Vrf v2 = nf.vrfBuilder().setOwner(c).setName("v2").build();
    Interface.Builder builder = nf.interfaceBuilder().setOwner(c);
    Interface i1 =
        builder.setAddresses(ConcreteInterfaceAddress.parse("1.2.3.4/24")).setVrf(v1).build();
    Interface i2 =
        builder.setAddresses(ConcreteInterfaceAddress.parse("1.2.3.5/24")).setVrf(v1).build();
    Interface i3 =
        builder.setAddresses(ConcreteInterfaceAddress.parse("1.2.3.6/24")).setVrf(v2).build();
    Topology t = TopologyUtil.synthesizeL3Topology(ImmutableMap.of(c.getHostname(), c));
    assertThat(
        t.getEdges(),
        equalTo(
            ImmutableSet.of(
                new Edge(i1, i3), new Edge(i3, i1), new Edge(i2, i3), new Edge(i3, i2))));
  }

  @Test
  public void testSynthesizeTopology_asymmetricPartialOverlap() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Configuration c2 = cb.build();
    nf.interfaceBuilder()
        .setOwner(c1)
        .setAddresses(ConcreteInterfaceAddress.parse("1.2.3.4/24"))
        .build();
    nf.interfaceBuilder()
        .setOwner(c2)
        .setAddresses(ConcreteInterfaceAddress.parse("1.2.3.17/28"))
        .build();
    Topology t =
        TopologyUtil.synthesizeL3Topology(
            ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2));
    assertThat(t.getEdges(), empty());
  }

  @Test
  public void testSynthesizeTopology_asymmetricSharedIp() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Configuration c2 = cb.build();
    nf.interfaceBuilder()
        .setOwner(c1)
        .setAddresses(ConcreteInterfaceAddress.parse("1.2.3.4/24"))
        .build();
    nf.interfaceBuilder()
        .setOwner(c2)
        .setAddresses(ConcreteInterfaceAddress.parse("1.2.3.4/28"))
        .build();
    Topology t =
        TopologyUtil.synthesizeL3Topology(
            ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2));
    assertThat(t.getEdges(), empty());
  }

  @Test
  public void testComputeLayer3Topology_linkLocalAddresses() {
    _cb.setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = _cb.build();
    Configuration c2 = _cb.build();
    Ip ip = Ip.parse("169.254.0.1");
    Interface i1 = _ib.setOwner(c1).setAddress(LinkLocalAddress.of(ip)).build();
    Interface i2 = _ib.setOwner(c2).setAddress(LinkLocalAddress.of(ip)).build();

    Layer1Topology layer1Topology =
        new Layer1Topology(
            Collections.singleton(
                new Layer1Edge(
                    new Layer1Node(c1.getHostname(), i1.getName()),
                    new Layer1Node(c2.getHostname(), i2.getName()))));
    Topology t =
        computeRawLayer3Topology(
            layer1Topology,
            layer1Topology,
            Layer2Topology.EMPTY,
            ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2));
    Edge edge =
        new Edge(
            new NodeInterfacePair(c1.getHostname(), i1.getName()),
            new NodeInterfacePair(c2.getHostname(), i2.getName()));
    assertThat(t.getEdges(), containsInAnyOrder(edge, edge.reverse()));
  }

  @Test
  public void testComputeInitialTunnelTopology() {
    _cb.setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = _cb.build();
    Configuration c2 = _cb.build();
    Ip ip1 = Ip.parse("1.1.1.1");
    Ip ip2 = Ip.parse("1.1.1.2");
    Ip ip3 = Ip.parse("1.1.1.3");
    int subnetMask = 24;
    Interface i1 =
        _ib.setOwner(c1)
            .setAddress(ConcreteInterfaceAddress.create(ip1, subnetMask))
            .setType(InterfaceType.TUNNEL)
            .build();
    Interface i2 =
        _ib.setOwner(c2)
            .setAddress(ConcreteInterfaceAddress.create(ip2, subnetMask))
            .setType(InterfaceType.TUNNEL)
            .build();
    _ib.setOwner(c2)
        .setAddress(ConcreteInterfaceAddress.create(ip3, subnetMask))
        .setType(InterfaceType.PHYSICAL)
        .build();

    assertThat(
        computeInitialTunnelTopology(ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2))
            .asEdgeSet(),
        containsInAnyOrder(new Edge(i1, i2), new Edge(i2, i1)));
  }
}

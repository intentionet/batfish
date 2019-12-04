package org.batfish.datamodel.vxlan;

import static com.google.common.collect.ImmutableSortedSet.of;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.addVniEdge;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.addVniEdges;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.buildVxlanNode;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.compatibleVniSettings;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.computeVniSettingsTable;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.computeVxlanTopology;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.prunedVxlanTopology;
import static org.batfish.datamodel.vxlan.VxlanTopologyUtils.vxlanFlowDelivered;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Table;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.batfish.common.plugin.TracerouteEngine;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.flow.FirewallSessionTraceInfo;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.flow.TraceAndReverseFlow;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.vxlan.VxlanTopologyUtils.VrfId;
import org.junit.Before;
import org.junit.Test;

/** Test of {@link VxlanTopologyUtils}. */
public final class VxlanTopologyUtilsTest {

  private static class TestTracerouteEngine implements TracerouteEngine {

    private final Map<String, List<Trace>> _results;

    private TestTracerouteEngine(Map<String, TestTracerouteEngineResult> results) {
      _results =
          results.entrySet().stream()
              .collect(
                  ImmutableMap.toImmutableMap(
                      Entry::getKey,
                      resultEntry ->
                          ImmutableList.of(
                              new Trace(
                                  resultEntry.getValue()._accept
                                      ? FlowDisposition.ACCEPTED
                                      : FlowDisposition.DENIED_OUT,
                                  resultEntry.getValue()._addHop
                                      ? ImmutableList.of(
                                          new Hop(
                                              new Node(resultEntry.getValue()._receivingNode),
                                              ImmutableList.of()))
                                      : ImmutableList.of()))));
    }

    @Override
    public SortedMap<Flow, List<Trace>> computeTraces(Set<Flow> flows, boolean ignoreFilters) {
      return flows.stream()
          .collect(
              ImmutableSortedMap.toImmutableSortedMap(
                  Comparator.naturalOrder(),
                  Function.<Flow>identity(),
                  flow -> _results.get(flow.getIngressNode())));
    }

    @Override
    public SortedMap<Flow, List<TraceAndReverseFlow>> computeTracesAndReverseFlows(
        Set<Flow> flows, Set<FirewallSessionTraceInfo> sessions, boolean ignoreFilters) {
      throw new UnsupportedOperationException();
    }
  }

  private static class TestTracerouteEngineResult {
    private final boolean _accept;
    private final boolean _addHop;
    private final String _receivingNode;

    private TestTracerouteEngineResult(String receivingNode, boolean accept, boolean addHop) {
      _receivingNode = receivingNode;
      _accept = accept;
      _addHop = addHop;
    }
  }

  private static final String IFACE_NAME = "i0";
  private static final Ip MULTICAST_GROUP = Ip.parse("224.0.0.1");
  private static final String NODE1 = "n1";
  private static final String NODE2 = "n2";
  private static final Ip SRC_IP1 = Ip.parse("10.0.0.0");
  private static final Ip SRC_IP2 = Ip.parse("10.0.0.1");
  private static final int UDP_PORT = 5555;
  private static final int VLAN1 = 1;
  private static final int VLAN2 = 2;
  private static final int VNI = 5000;

  private Configuration _c1;
  private Configuration _c2;
  private Vrf _v1;
  private Vrf _v2;

  private Map<String, Configuration> compatibleVxlanConfigs() {
    Interface.Builder ib =
        Interface.builder().setType(InterfaceType.PHYSICAL).setName(IFACE_NAME).setActive(true);
    ib.setAddresses(ConcreteInterfaceAddress.create(SRC_IP1, 31)).setOwner(_c1).setVrf(_v1).build();
    ib.setAddresses(ConcreteInterfaceAddress.create(SRC_IP2, 31)).setOwner(_c2).setVrf(_v2).build();
    Layer2Vni.Builder vsb =
        Layer2Vni.builder()
            .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI);
    _v1.addLayer2Vni(vsb.setBumTransportIps(of(SRC_IP2)).setSourceAddress(SRC_IP1).build());
    _v2.addLayer2Vni(vsb.setBumTransportIps(of(SRC_IP1)).setSourceAddress(SRC_IP2).build());
    return ImmutableMap.of(NODE1, _c1, NODE2, _c2);
  }

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _c1 = cb.setHostname(NODE1).build();
    _c2 = cb.setHostname(NODE2).build();
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    _v1 = vb.setOwner(_c1).build();
    _v2 = vb.setOwner(_c2).build();
  }

  @Test
  public void testAddVniEdge() {
    MutableGraph<VxlanNode> graph = GraphBuilder.undirected().allowsSelfLoops(false).build();
    Layer2Vni.Builder vniSettingsBuilder =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setUdpPort(UDP_PORT)
            .setVni(VNI);
    Layer2Vni vniSettingsTail =
        vniSettingsBuilder.setSourceAddress(SRC_IP1).setVlan(VLAN1).setVni(VNI).build();
    _v1.setLayer2Vnis(ImmutableSet.of(vniSettingsTail));
    Layer2Vni vniSettingsHead =
        vniSettingsBuilder.setSourceAddress(SRC_IP2).setVlan(VLAN2).setVni(VNI).build();
    _v2.setLayer2Vnis(ImmutableSet.of(vniSettingsHead));
    addVniEdge(
        graph,
        new VrfId(_c1.getHostname(), _v1.getName()),
        vniSettingsTail,
        new VrfId(_c2.getHostname(), _v2.getName()),
        vniSettingsHead);
    Set<EndpointPair<VxlanNode>> edges = graph.edges();

    assertThat(
        edges,
        equalTo(
            ImmutableSet.of(
                EndpointPair.unordered(
                    VxlanNode.builder().setHostname(NODE2).setVni(VNI).build(),
                    VxlanNode.builder().setHostname(NODE1).setVni(VNI).build()))));
  }

  @Test
  public void testAddVniEdgeIncompatible() {
    MutableGraph<VxlanNode> graph = GraphBuilder.undirected().allowsSelfLoops(false).build();
    Layer2Vni.Builder vniSettingsBuilder =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setUdpPort(UDP_PORT)
            .setVni(VNI);
    Layer2Vni vniSettingsTail = vniSettingsBuilder.setSourceAddress(SRC_IP1).setVlan(VLAN1).build();
    _v1.setLayer2Vnis(ImmutableSet.of(vniSettingsTail));
    Layer2Vni vniSettingsHead = vniSettingsBuilder.setSourceAddress(SRC_IP1).setVlan(VLAN2).build();
    _v2.setLayer2Vnis(ImmutableSet.of(vniSettingsHead));
    addVniEdge(
        graph,
        new VrfId(_c1.getHostname(), _v1.getName()),
        vniSettingsTail,
        new VrfId(_c2.getHostname(), _v2.getName()),
        vniSettingsHead);
    Set<EndpointPair<VxlanNode>> edges = graph.edges();

    assertThat(edges, empty());
  }

  @Test
  public void testAddVniEdges() {
    Map<String, Configuration> configurations = ImmutableMap.of(NODE1, _c1, NODE2, _c2);
    MutableGraph<VxlanNode> graph = GraphBuilder.undirected().allowsSelfLoops(false).build();
    Layer2Vni.Builder vniSettingsBuilder =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setUdpPort(UDP_PORT)
            .setVni(VNI);
    Layer2Vni vniSettingsTail = vniSettingsBuilder.setSourceAddress(SRC_IP1).setVlan(VLAN1).build();
    _v1.setLayer2Vnis(ImmutableSet.of(vniSettingsTail));
    Layer2Vni vniSettingsHead = vniSettingsBuilder.setSourceAddress(SRC_IP2).setVlan(VLAN2).build();
    _v2.setLayer2Vnis(ImmutableSet.of(vniSettingsHead));
    addVniEdges(
        graph,
        computeVniSettingsTable(configurations),
        VNI,
        ImmutableMap.of(
            new VrfId(_c1.getHostname(), _v1.getName()), vniSettingsTail,
            new VrfId(_c2.getHostname(), _v2.getName()), vniSettingsHead));

    VxlanNode nodeTail = VxlanNode.builder().setHostname(NODE1).setVni(VNI).build();
    VxlanNode nodeHead = VxlanNode.builder().setHostname(NODE2).setVni(VNI).build();

    assertThat(graph.edges(), equalTo(ImmutableSet.of(EndpointPair.unordered(nodeTail, nodeHead))));
  }

  @Test
  public void testBuildVxlanNode() {
    Layer2Vni vniSettings =
        Layer2Vni.builder()
            .setSourceAddress(SRC_IP1)
            .setVlan(VLAN1)
            .setVni(VNI)
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .build();
    assertThat(
        buildVxlanNode(new VrfId(_c1.getHostname(), _v1.getName()), vniSettings),
        equalTo(VxlanNode.builder().setHostname(NODE1).setVni(VNI).build()));
  }

  @Test
  public void testCompatibleVniSettingsMismatchBumTransportHeadFloodGroup() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(SRC_IP2))
            .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(SRC_IP2))
            .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
            .setSourceAddress(SRC_IP2)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(false));
  }

  @Test
  public void testCompatibleVniSettingsMismatchBumTransportMethod() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
            .setSourceAddress(SRC_IP2)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(false));
  }

  @Test
  public void testCompatibleVniSettingsMismatchBumTransportMulticastGroup() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(Ip.parse("224.0.0.5")))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP2)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(false));
  }

  @Test
  public void testCompatibleVniSettingsMismatchBumTransportMulticastGroupUnicast() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(SRC_IP2))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(SRC_IP1))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP2)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(false));
  }

  @Test
  public void testCompatibleVniSettingsMismatchBumTransportTailFloodGroup() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(SRC_IP1))
            .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(SRC_IP1))
            .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
            .setSourceAddress(SRC_IP2)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(false));
  }

  @Test
  public void testCompatibleVniSettingsMismatchDifferentUdpPort() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP2)
            .setUdpPort(UDP_PORT + 1)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(false));
  }

  @Test
  public void testCompatibleVniSettingsMismatchNullHeadSourceAddress() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(null)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(false));
  }

  @Test
  public void testCompatibleVniSettingsMismatchNullTailSourceAddress() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(null)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP2)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(false));
  }

  @Test
  public void testCompatibleVniSettingsMismatchSameSourceAddress() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(false));
  }

  @Test
  public void testCompatibleVniSettingsMulticast() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setSourceAddress(SRC_IP2)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(true));
  }

  @Test
  public void testCompatibleVniSettingsUnicast() {
    Layer2Vni vniSettingsTail =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(SRC_IP2))
            .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
            .setSourceAddress(SRC_IP1)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN1)
            .setVni(VNI)
            .build();
    Layer2Vni vniSettingsHead =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(SRC_IP1))
            .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
            .setSourceAddress(SRC_IP2)
            .setUdpPort(UDP_PORT)
            .setVlan(VLAN2)
            .setVni(VNI)
            .build();

    assertThat(compatibleVniSettings(vniSettingsTail, vniSettingsHead), equalTo(true));
  }

  @Test
  public void testInitialVxlanTopology() {
    Map<String, Configuration> configurations = ImmutableMap.of(NODE1, _c1, NODE2, _c2);
    Layer2Vni.Builder vniSettingsBuilder =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setUdpPort(UDP_PORT)
            .setVni(VNI);
    Layer2Vni vniSettingsTail = vniSettingsBuilder.setSourceAddress(SRC_IP1).setVlan(VLAN1).build();
    _v1.setLayer2Vnis(ImmutableSet.of(vniSettingsTail));
    _v2.setLayer2Vnis(
        ImmutableSet.of(vniSettingsBuilder.setSourceAddress(SRC_IP2).setVlan(VLAN2).build()));

    VxlanNode nodeTail = VxlanNode.builder().setHostname(NODE1).setVni(VNI).build();
    VxlanNode nodeHead = VxlanNode.builder().setHostname(NODE2).setVni(VNI).build();

    assertThat(
        VxlanTopologyUtils.computeVxlanTopology(configurations).getGraph().edges(),
        equalTo(ImmutableSet.of(EndpointPair.unordered(nodeTail, nodeHead))));
  }

  @Test
  public void testInitialVxlanTopologyFromTable() {
    Layer2Vni.Builder vniSettingsBuilder =
        Layer2Vni.builder()
            .setBumTransportIps(ImmutableSortedSet.of(MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setUdpPort(UDP_PORT)
            .setVni(VNI);
    Layer2Vni vniSettingsTail = vniSettingsBuilder.setSourceAddress(SRC_IP1).setVlan(VLAN1).build();
    _v1.setLayer2Vnis(ImmutableSet.of(vniSettingsTail));
    Layer2Vni vniSettingsHead = vniSettingsBuilder.setSourceAddress(SRC_IP2).setVlan(VLAN2).build();
    _v2.setLayer2Vnis(ImmutableSet.of(vniSettingsHead));
    Table<String, String, Set<Layer2Vni>> table = HashBasedTable.create();
    table.put(_c1.getHostname(), _v1.getName(), ImmutableSet.of(vniSettingsTail));
    table.put(_c2.getHostname(), _v2.getName(), ImmutableSet.of(vniSettingsHead));

    VxlanNode nodeTail = VxlanNode.builder().setHostname(NODE1).setVni(VNI).build();
    VxlanNode nodeHead = VxlanNode.builder().setHostname(NODE2).setVni(VNI).build();

    assertThat(
        computeVxlanTopology(table).getGraph().edges(),
        equalTo(ImmutableSet.of(EndpointPair.unordered(nodeTail, nodeHead))));
  }

  @Test
  public void testPrunedVxlanTopologyDiscard() {
    MutableGraph<VxlanNode> graph = GraphBuilder.undirected().allowsSelfLoops(false).build();
    graph.putEdge(new VxlanNode(NODE1, VNI), new VxlanNode(NODE2, VNI));
    VxlanTopology initial = new VxlanTopology(graph);

    // no reachability
    assertThat(
        prunedVxlanTopology(
                initial,
                compatibleVxlanConfigs(),
                new TestTracerouteEngine(
                    ImmutableMap.of(
                        NODE1,
                        new TestTracerouteEngineResult(NODE2, false, true),
                        NODE2,
                        new TestTracerouteEngineResult(NODE1, false, true))))
            .getGraph()
            .edges(),
        empty());
  }

  @Test
  public void testPrunedVxlanTopologyKeep() {
    MutableGraph<VxlanNode> graph = GraphBuilder.undirected().allowsSelfLoops(false).build();
    graph.putEdge(new VxlanNode(NODE1, VNI), new VxlanNode(NODE2, VNI));
    VxlanTopology initial = new VxlanTopology(graph);

    assertEquals(
        prunedVxlanTopology(
            initial,
            compatibleVxlanConfigs(),
            new TestTracerouteEngine(
                ImmutableMap.of(
                    NODE1,
                    new TestTracerouteEngineResult(NODE2, true, true),
                    NODE2,
                    new TestTracerouteEngineResult(NODE1, true, true)))),
        initial);
  }

  @Test
  public void testReachableEdgeBothDirections() {
    assertTrue(
        VxlanTopologyUtils.reachableEdge(
            EndpointPair.unordered(new VxlanNode(NODE1, VNI), new VxlanNode(NODE2, VNI)),
            NetworkConfigurations.of(compatibleVxlanConfigs()),
            new TestTracerouteEngine(
                ImmutableMap.of(
                    NODE1,
                    new TestTracerouteEngineResult(NODE2, true, true),
                    NODE2,
                    new TestTracerouteEngineResult(NODE1, true, true)))));
  }

  @Test
  public void testReachableEdgeFirstDirectionOnly() {
    assertFalse(
        VxlanTopologyUtils.reachableEdge(
            EndpointPair.unordered(new VxlanNode(NODE1, VNI), new VxlanNode(NODE2, VNI)),
            NetworkConfigurations.of(compatibleVxlanConfigs()),
            new TestTracerouteEngine(
                ImmutableMap.of(
                    NODE1,
                    new TestTracerouteEngineResult(NODE2, true, true),
                    NODE2,
                    new TestTracerouteEngineResult(NODE1, false, true)))));
  }

  @Test
  public void testReachableEdgeMulticast() {
    // TODO: passing case when multicast is suppported
    Map<String, Configuration> configs = compatibleVxlanConfigs();
    // make VNI settings use multicast
    Stream.of(configs.get(NODE1), configs.get(NODE2))
        .forEach(
            config -> {
              Vrf vrf = config.getDefaultVrf();
              vrf.setLayer2Vnis(
                  vrf.getLayer2Vnis().values().stream()
                      .map(
                          vni ->
                              vni.toBuilder()
                                  .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
                                  .build())
                      .collect(ImmutableSet.toImmutableSet()));
            });

    assertFalse(
        VxlanTopologyUtils.reachableEdge(
            EndpointPair.unordered(new VxlanNode(NODE1, VNI), new VxlanNode(NODE2, VNI)),
            NetworkConfigurations.of(configs),
            new TestTracerouteEngine(
                ImmutableMap.of(
                    NODE1,
                    new TestTracerouteEngineResult(NODE2, true, true),
                    NODE2,
                    new TestTracerouteEngineResult(NODE1, true, true)))));
  }

  @Test
  public void testReachableEdgeSecondDirectionOnly() {
    assertFalse(
        VxlanTopologyUtils.reachableEdge(
            EndpointPair.unordered(new VxlanNode(NODE1, VNI), new VxlanNode(NODE2, VNI)),
            NetworkConfigurations.of(compatibleVxlanConfigs()),
            new TestTracerouteEngine(
                ImmutableMap.of(
                    NODE1,
                    new TestTracerouteEngineResult(NODE2, false, true),
                    NODE2,
                    new TestTracerouteEngineResult(NODE1, true, true)))));
  }

  @Test
  public void testVxlanFlowDeliveredNoHops() {
    assertFalse(
        vxlanFlowDelivered(
            NODE1,
            _v1.getName(),
            SRC_IP1,
            NODE2,
            SRC_IP2,
            UDP_PORT,
            new TestTracerouteEngine(
                ImmutableMap.of(NODE1, new TestTracerouteEngineResult(NODE2, true, false)))));
  }

  @Test
  public void testVxlanFlowDeliveredNotAccepted() {
    assertFalse(
        vxlanFlowDelivered(
            NODE1,
            _v1.getName(),
            SRC_IP1,
            NODE2,
            SRC_IP2,
            UDP_PORT,
            new TestTracerouteEngine(
                ImmutableMap.of(NODE1, new TestTracerouteEngineResult(NODE2, false, true)))));
  }

  @Test
  public void testVxlanFlowDeliveredSuccess() {
    assertTrue(
        vxlanFlowDelivered(
            NODE1,
            _v1.getName(),
            SRC_IP1,
            NODE2,
            SRC_IP2,
            UDP_PORT,
            new TestTracerouteEngine(
                ImmutableMap.of(NODE1, new TestTracerouteEngineResult(NODE2, true, true)))));
  }

  @Test
  public void testVxlanFlowDeliveredWrongNode() {
    assertFalse(
        vxlanFlowDelivered(
            NODE1,
            _v1.getName(),
            SRC_IP1,
            NODE2,
            SRC_IP2,
            UDP_PORT,
            new TestTracerouteEngine(
                ImmutableMap.of(
                    NODE1, new TestTracerouteEngineResult("wrong place", true, true)))));
  }
}

package org.batfish.question.edges;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.edges.EdgesAnswerer.COL_AS_NUMBER;
import static org.batfish.question.edges.EdgesAnswerer.COL_INTERFACE;
import static org.batfish.question.edges.EdgesAnswerer.COL_IP;
import static org.batfish.question.edges.EdgesAnswerer.COL_IPS;
import static org.batfish.question.edges.EdgesAnswerer.COL_NODE;
import static org.batfish.question.edges.EdgesAnswerer.COL_REMOTE_AS_NUMBER;
import static org.batfish.question.edges.EdgesAnswerer.COL_REMOTE_INTERFACE;
import static org.batfish.question.edges.EdgesAnswerer.COL_REMOTE_IP;
import static org.batfish.question.edges.EdgesAnswerer.COL_REMOTE_IPS;
import static org.batfish.question.edges.EdgesAnswerer.COL_REMOTE_NODE;
import static org.batfish.question.edges.EdgesAnswerer.COL_REMOTE_SOURCE_INTERFACE;
import static org.batfish.question.edges.EdgesAnswerer.COL_REMOTE_TUNNEL_INTERFACE;
import static org.batfish.question.edges.EdgesAnswerer.COL_REMOTE_VLAN;
import static org.batfish.question.edges.EdgesAnswerer.COL_SOURCE_INTERFACE;
import static org.batfish.question.edges.EdgesAnswerer.COL_TUNNEL_INTERFACE;
import static org.batfish.question.edges.EdgesAnswerer.COL_VLAN;
import static org.batfish.question.edges.EdgesAnswerer.eigrpEdgeToRow;
import static org.batfish.question.edges.EdgesAnswerer.getBgpEdges;
import static org.batfish.question.edges.EdgesAnswerer.getEigrpEdges;
import static org.batfish.question.edges.EdgesAnswerer.getIpsecEdges;
import static org.batfish.question.edges.EdgesAnswerer.getIsisEdges;
import static org.batfish.question.edges.EdgesAnswerer.getLayer1Edges;
import static org.batfish.question.edges.EdgesAnswerer.getLayer3Edges;
import static org.batfish.question.edges.EdgesAnswerer.getOspfEdgeRow;
import static org.batfish.question.edges.EdgesAnswerer.getOspfEdges;
import static org.batfish.question.edges.EdgesAnswerer.getRipEdgeRow;
import static org.batfish.question.edges.EdgesAnswerer.getRipEdges;
import static org.batfish.question.edges.EdgesAnswerer.getTableMetadata;
import static org.batfish.question.edges.EdgesAnswerer.getVxlanEdges;
import static org.batfish.question.edges.EdgesAnswerer.isisEdgeToRow;
import static org.batfish.question.edges.EdgesAnswerer.layer1EdgeToRow;
import static org.batfish.question.edges.EdgesAnswerer.layer2EdgeToRow;
import static org.batfish.question.edges.EdgesAnswerer.layer3EdgeToRow;
import static org.batfish.question.edges.EdgesAnswerer.vxlanEdgeToRow;
import static org.batfish.question.edges.EdgesAnswerer.vxlanEdgeToRows;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.NetworkBuilder;
import com.google.common.graph.ValueGraphBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Pair;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Node;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Edge;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.EdgeType;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpsecPeerConfigId;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.IpsecSession;
import org.batfish.datamodel.IpsecStaticPeerConfig;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipNeighbor;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.eigrp.EigrpEdge;
import org.batfish.datamodel.eigrp.EigrpInterface;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisNode;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.OspfTopologyUtils;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.vxlan.VxlanNode;
import org.batfish.datamodel.vxlan.VxlanTopology;
import org.batfish.datamodel.vxlan.VxlanTopologyUtils;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link EdgesAnswerer} */
public class EdgesAnswererTest {

  private static final int VXLAN_VNI = 5000;
  private static final String VXLAN_NODE1 = "n1";
  private static final String VXLAN_NODE2 = "n2";
  private static final Ip VXLAN_MULTICAST_GROUP = Ip.parse("224.0.0.1");
  private static final Ip VXLAN_SRC_IP1 = Ip.parse("1.1.1.1");
  private static final Ip VXLAN_SRC_IP2 = Ip.parse("2.2.2.2");
  private static final int VXLAN_UDP_PORT = 5555;
  private static final int VXLAN_VLAN1 = 1;
  private static final int VXLAN_VLAN2 = 2;

  private static @Nonnull NetworkConfigurations buildVxlanNetworkConfigurations() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.setHostname(VXLAN_NODE1).build();
    Configuration c2 = cb.setHostname(VXLAN_NODE2).build();
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Vrf v1 = vb.setOwner(c1).build();
    Vrf v2 = vb.setOwner(c2).build();
    Map<String, Configuration> configurations = ImmutableMap.of(VXLAN_NODE1, c1, VXLAN_NODE2, c2);
    VniSettings.Builder vniSettingsBuilder =
        VniSettings.builder()
            .setBumTransportIps(ImmutableSortedSet.of(VXLAN_MULTICAST_GROUP))
            .setBumTransportMethod(BumTransportMethod.MULTICAST_GROUP)
            .setUdpPort(VXLAN_UDP_PORT)
            .setVni(VXLAN_VNI);
    VniSettings vniSettingsTail =
        vniSettingsBuilder.setSourceAddress(VXLAN_SRC_IP1).setVlan(VXLAN_VLAN1).build();
    v1.setVniSettings(ImmutableSortedMap.of(VXLAN_VNI, vniSettingsTail));
    v2.setVniSettings(
        ImmutableSortedMap.of(
            VXLAN_VNI,
            vniSettingsBuilder.setSourceAddress(VXLAN_SRC_IP2).setVlan(VXLAN_VLAN2).build()));
    return NetworkConfigurations.of(configurations);
  }

  private Configuration _host1;
  private Configuration _host2;
  private Map<String, Configuration> _configurations;
  private Set<String> _includeNodes;
  private Set<String> _includeRemoteNodes;

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    _host1 = cb.setHostname("host1").build();
    _host1.setInterfaces(
        ImmutableSortedMap.of(
            "int1",
            Interface.builder()
                .setName("int1")
                .setAddress(new InterfaceAddress(Ip.parse("1.1.1.1"), 24))
                .build()));

    _host2 = cb.setHostname("host2").build();
    _host2.setInterfaces(
        ImmutableSortedMap.of(
            "int2",
            Interface.builder()
                .setName("int2")
                .setAddress(new InterfaceAddress(Ip.parse("2.2.2.2"), 24))
                .build()));

    _configurations = ImmutableSortedMap.of("host1", _host1, "host2", _host2);
    _includeNodes = ImmutableSortedSet.of("host1", "host2");
    _includeRemoteNodes = ImmutableSortedSet.of("host1", "host2");
  }

  @Test
  public void testGetIpsecEdges() {
    MutableValueGraph<IpsecPeerConfigId, IpsecSession> ipsecTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();

    IpsecPeerConfigId peerId1 = new IpsecPeerConfigId("peer1", "host1");
    IpsecPeerConfigId peerId2 = new IpsecPeerConfigId("peer2", "host1");
    IpsecPeerConfigId peerId3 = new IpsecPeerConfigId("peer3", "host2");
    IpsecPeerConfigId peerId4 = new IpsecPeerConfigId("peer4", "host2");

    ipsecTopology.putEdgeValue(
        peerId1,
        peerId3,
        IpsecSession.builder().setNegotiatedIpsecP2Proposal(new IpsecPhase2Proposal()).build());
    // non-established edge
    ipsecTopology.putEdgeValue(peerId2, peerId4, IpsecSession.builder().build());

    IpsecStaticPeerConfig peer1 =
        IpsecStaticPeerConfig.builder()
            .setSourceInterface("int11")
            .setTunnelInterface("tunnel11")
            .build();
    IpsecStaticPeerConfig peer2 =
        IpsecStaticPeerConfig.builder()
            .setSourceInterface("int12")
            .setTunnelInterface("tunnel12")
            .build();
    IpsecStaticPeerConfig peer3 =
        IpsecStaticPeerConfig.builder()
            .setSourceInterface("int21")
            .setTunnelInterface("tunnel21")
            .build();
    IpsecStaticPeerConfig peer4 =
        IpsecStaticPeerConfig.builder()
            .setSourceInterface("int22")
            .setTunnelInterface("tunnel22")
            .build();

    _host1.setIpsecPeerConfigs(ImmutableSortedMap.of("peer1", peer1, "peer2", peer2));
    _host2.setIpsecPeerConfigs(ImmutableSortedMap.of("peer3", peer3, "peer4", peer4));

    Multiset<Row> rows = getIpsecEdges(ipsecTopology, _configurations);

    // only one edge should be present
    assertThat(
        rows,
        containsInAnyOrder(
            ImmutableList.of(
                allOf(
                    hasColumn(
                        COL_SOURCE_INTERFACE,
                        equalTo(new NodeInterfacePair("host1", "int11")),
                        Schema.INTERFACE),
                    hasColumn(
                        COL_TUNNEL_INTERFACE,
                        equalTo(new NodeInterfacePair("host1", "tunnel11")),
                        Schema.INTERFACE),
                    hasColumn(
                        COL_REMOTE_SOURCE_INTERFACE,
                        equalTo(new NodeInterfacePair("host2", "int21")),
                        Schema.INTERFACE),
                    hasColumn(
                        COL_REMOTE_TUNNEL_INTERFACE,
                        equalTo(new NodeInterfacePair("host2", "tunnel21")),
                        Schema.INTERFACE)))));
  }

  @Test
  public void testGetEigrpEdges() {
    MutableNetwork<EigrpInterface, EigrpEdge> eigrpTopology =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();

    EigrpInterface eigrpInterface1 = new EigrpInterface("host1", "int1", "vrf1");
    EigrpInterface eigrpInterface2 = new EigrpInterface("host2", "int2", "vrf2");

    eigrpTopology.addEdge(
        eigrpInterface1, eigrpInterface2, new EigrpEdge(eigrpInterface1, eigrpInterface2));

    Multiset<Row> rows =
        getEigrpEdges(_includeNodes, _includeRemoteNodes, new EigrpTopology(eigrpTopology));

    assertThat(
        rows,
        contains(
            allOf(
                hasColumn(
                    COL_INTERFACE,
                    equalTo(new NodeInterfacePair("host1", "int1")),
                    Schema.INTERFACE),
                hasColumn(
                    COL_REMOTE_INTERFACE,
                    equalTo(new NodeInterfacePair("host2", "int2")),
                    Schema.INTERFACE))));
  }

  @Test
  public void testGetBgpEdges() {
    BgpProcess bgp1 = new BgpProcess(Ip.parse("1.1.1.1"));
    BgpProcess bgp2 = new BgpProcess(Ip.parse("2.2.2.2"));

    // Edge between active peers
    Ip ip1 = Ip.parse("1.1.1.1");
    Ip ip2 = Ip.parse("2.2.2.2");

    BgpActivePeerConfig activePeer1 =
        BgpActivePeerConfig.builder().setLocalIp(ip1).setLocalAs(1L).build();
    bgp1.getActiveNeighbors().put(Prefix.create(ip2, Prefix.MAX_PREFIX_LENGTH), activePeer1);
    BgpPeerConfigId activeId1 =
        new BgpPeerConfigId("host1", "vrf1", Prefix.create(ip2, Prefix.MAX_PREFIX_LENGTH), false);

    BgpActivePeerConfig activePeer2 =
        BgpActivePeerConfig.builder().setLocalIp(ip2).setLocalAs(2L).build();
    bgp2.getActiveNeighbors().put(Prefix.create(ip1, Prefix.MAX_PREFIX_LENGTH), activePeer2);
    BgpPeerConfigId activeId2 =
        new BgpPeerConfigId("host2", "vrf2", Prefix.create(ip1, Prefix.MAX_PREFIX_LENGTH), false);

    // Edge between unnumbered peers
    String iface1 = "iface1";
    String iface2 = "iface2";

    BgpUnnumberedPeerConfig unnumPeer1 =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface(iface1)
            .setLocalAs(1L)
            .setLocalIp(Ip.parse("169.254.0.1"))
            .build();
    bgp1.getInterfaceNeighbors().put(iface1, unnumPeer1);
    BgpPeerConfigId unnumId1 = new BgpPeerConfigId("host1", "vrf1", iface1);

    BgpUnnumberedPeerConfig unnumPeer2 =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface(iface2)
            .setLocalAs(2L)
            .setLocalIp(Ip.parse("169.254.0.1"))
            .build();
    bgp2.getInterfaceNeighbors().put(iface2, unnumPeer2);
    BgpPeerConfigId unnumId2 = new BgpPeerConfigId("host2", "vrf2", iface2);

    Vrf vrf1 = new Vrf("vrf1");
    vrf1.setBgpProcess(bgp1);
    Vrf vrf2 = new Vrf("vrf2");
    vrf2.setBgpProcess(bgp2);

    _host1.setVrfs(ImmutableSortedMap.of("vrf1", vrf1));
    _host2.setVrfs(ImmutableSortedMap.of("vrf2", vrf2));

    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    bgpTopology.putEdgeValue(
        activeId1, activeId2, BgpSessionProperties.from(activePeer1, activePeer2, false));
    bgpTopology.putEdgeValue(
        unnumId1, unnumId2, BgpSessionProperties.from(unnumPeer1, unnumPeer2, false));

    Multiset<Row> rows =
        getBgpEdges(
            _configurations, _includeNodes, _includeRemoteNodes, new BgpTopology(bgpTopology));

    Row expectedActiveRow =
        Row.builder()
            .put(COL_NODE, new Node("host1"))
            .put(COL_IP, ip1)
            .put(COL_INTERFACE, null)
            .put(COL_AS_NUMBER, 1L)
            .put(COL_REMOTE_NODE, new Node("host2"))
            .put(COL_REMOTE_IP, ip2)
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_REMOTE_AS_NUMBER, 2L)
            .build();
    Row expectedUnnumRow =
        Row.builder()
            .put(COL_NODE, new Node("host1"))
            .put(COL_IP, null)
            .put(COL_INTERFACE, iface1)
            .put(COL_AS_NUMBER, 1L)
            .put(COL_REMOTE_NODE, new Node("host2"))
            .put(COL_REMOTE_IP, null)
            .put(COL_REMOTE_INTERFACE, iface2)
            .put(COL_REMOTE_AS_NUMBER, 2L)
            .build();
    assertThat(rows, containsInAnyOrder(expectedActiveRow, expectedUnnumRow));
  }

  @Test
  public void testGetOspfEdges() {
    NetworkFactory nf = new NetworkFactory();
    OspfProcess ospf1 = OspfProcess.builder(nf).setReferenceBandwidth(1e8).build();
    OspfProcess ospf2 = OspfProcess.builder(nf).setReferenceBandwidth(1e8).build();

    OspfArea.builder(nf).setNumber(1L).setOspfProcess(ospf1).addInterface("int1").build();
    OspfArea.builder(nf).setNumber(1L).setOspfProcess(ospf2).addInterface("int2").build();

    Vrf vrf1 = new Vrf("vrf1");
    vrf1.setOspfProcesses(Stream.of(ospf1));
    Vrf vrf2 = new Vrf("vrf2");
    vrf2.setOspfProcesses(Stream.of(ospf2));

    _host1.setVrfs(ImmutableSortedMap.of("vrf1", vrf1));
    _host2.setVrfs(ImmutableSortedMap.of("vrf2", vrf2));
    Interface i1 = _host1.getAllInterfaces().get("int1");
    i1.setVrf(vrf1);
    i1.setOspfProcess(ospf1.getProcessId());
    Interface i2 = _host2.getAllInterfaces().get("int2");
    i2.setVrf(vrf2);
    i2.setOspfProcess(ospf2.getProcessId());

    OspfTopologyUtils.initNeighborConfigs(NetworkConfigurations.of(_configurations));
    // Need edges to be bi-directional
    Topology l3topo =
        new Topology(
            ImmutableSortedSet.of(
                Edge.of("host1", "int1", "host2", "int2"),
                Edge.of("host2", "int2", "host1", "int1")));
    Multiset<Row> rows =
        getOspfEdges(
            _configurations,
            _includeNodes,
            _includeRemoteNodes,
            OspfTopologyUtils.computeOspfTopology(
                NetworkConfigurations.of(_configurations), l3topo));
    assertThat(
        rows,
        contains(
            ImmutableList.of(
                allOf(
                    hasColumn(
                        COL_INTERFACE,
                        equalTo(new NodeInterfacePair("host1", "int1")),
                        Schema.INTERFACE),
                    hasColumn(
                        COL_REMOTE_INTERFACE,
                        equalTo(new NodeInterfacePair("host2", "int2")),
                        Schema.INTERFACE)),
                allOf(
                    hasColumn(
                        COL_INTERFACE,
                        equalTo(new NodeInterfacePair("host2", "int2")),
                        Schema.INTERFACE),
                    hasColumn(
                        COL_REMOTE_INTERFACE,
                        equalTo(new NodeInterfacePair("host1", "int1")),
                        Schema.INTERFACE)))));
  }

  @Test
  public void testGetRipEdges() {
    RipProcess rip1 = new RipProcess();
    RipProcess rip2 = new RipProcess();
    RipNeighbor ripNeighbor1 =
        new RipNeighbor(new Pair<>(Ip.parse("1.1.1.1"), Ip.parse("2.2.2.2")));
    RipNeighbor ripNeighbor2 =
        new RipNeighbor(new Pair<>(Ip.parse("2.2.2.2"), Ip.parse("1.1.1.1")));
    rip1.setRipNeighbors(
        ImmutableSortedMap.of(new Pair<>(Ip.parse("1.1.1.1"), Ip.parse("2.2.2.2")), ripNeighbor1));
    rip2.setRipNeighbors(
        ImmutableSortedMap.of(new Pair<>(Ip.parse("2.2.2.2"), Ip.parse("1.1.1.1")), ripNeighbor2));
    ripNeighbor1.setOwner(_host1);
    ripNeighbor2.setOwner(_host2);
    ripNeighbor1.setIface(_host1.getAllInterfaces().get("int1"));
    ripNeighbor2.setIface(_host2.getAllInterfaces().get("int2"));
    ripNeighbor1.setRemoteRipNeighbor(ripNeighbor2);
    ripNeighbor2.setRemoteRipNeighbor(ripNeighbor1);

    Vrf vrf1 = new Vrf("vrf1");
    Vrf vrf2 = new Vrf("vrf2");

    vrf1.setRipProcess(rip1);
    vrf2.setRipProcess(rip2);

    _host1.setVrfs(ImmutableSortedMap.of("vrf1", vrf1));
    _host2.setVrfs(ImmutableSortedMap.of("vrf2", vrf2));

    Multiset<Row> rows = getRipEdges(_configurations, _includeNodes, _includeRemoteNodes);

    assertThat(
        rows,
        containsInAnyOrder(
            ImmutableList.of(
                allOf(
                    hasColumn(
                        COL_INTERFACE,
                        equalTo(new NodeInterfacePair("host1", "int1")),
                        Schema.INTERFACE),
                    hasColumn(
                        COL_REMOTE_INTERFACE,
                        equalTo(new NodeInterfacePair("host2", "int2")),
                        Schema.INTERFACE)),
                allOf(
                    hasColumn(
                        COL_INTERFACE,
                        equalTo(new NodeInterfacePair("host2", "int2")),
                        Schema.INTERFACE),
                    hasColumn(
                        COL_REMOTE_INTERFACE,
                        equalTo(new NodeInterfacePair("host1", "int1")),
                        Schema.INTERFACE)))));
  }

  @Test
  public void testGetVxlanEdges() {
    NetworkConfigurations nc = buildVxlanNetworkConfigurations();
    Map<String, Configuration> configurations = nc.getMap();
    VxlanTopology vxlanTopology = VxlanTopologyUtils.initialVxlanTopology(nc.getMap());
    Set<String> includeNodes = configurations.keySet();
    Set<String> includeRemoteNodes = configurations.keySet();

    assertThat(
        getVxlanEdges(nc, includeNodes, includeRemoteNodes, vxlanTopology),
        equalTo(
            vxlanEdgeToRows(
                    nc,
                    includeNodes,
                    includeRemoteNodes,
                    EndpointPair.unordered(
                        VxlanNode.builder().setHostname(VXLAN_NODE1).setVni(VXLAN_VNI).build(),
                        VxlanNode.builder().setHostname(VXLAN_NODE2).setVni(VXLAN_VNI).build()))
                .collect(ImmutableMultiset.toImmutableMultiset())));
  }

  @Test
  public void testVxlanEdgeToRows() {
    NetworkConfigurations nc = buildVxlanNetworkConfigurations();
    Map<String, Configuration> configurations = nc.getMap();
    Set<String> includeNodes = configurations.keySet();
    Set<String> includeRemoteNodes = configurations.keySet();
    VxlanNode node1 = VxlanNode.builder().setHostname(VXLAN_NODE1).setVni(VXLAN_VNI).build();
    VxlanNode node2 = VxlanNode.builder().setHostname(VXLAN_NODE2).setVni(VXLAN_VNI).build();

    // no filter
    assertThat(
        vxlanEdgeToRows(nc, includeNodes, includeRemoteNodes, EndpointPair.unordered(node1, node2))
            .collect(ImmutableList.toImmutableList()),
        containsInAnyOrder(vxlanEdgeToRow(nc, node1, node2), vxlanEdgeToRow(nc, node2, node1)));
    // only node1 in node position
    assertThat(
        vxlanEdgeToRows(
                nc,
                ImmutableSet.of(VXLAN_NODE1),
                includeRemoteNodes,
                EndpointPair.unordered(node1, node2))
            .collect(ImmutableList.toImmutableList()),
        containsInAnyOrder(vxlanEdgeToRow(nc, node1, node2)));
    // only node1 in remoteNode position
    assertThat(
        vxlanEdgeToRows(
                nc,
                includeNodes,
                ImmutableSet.of(VXLAN_NODE1),
                EndpointPair.unordered(node1, node2))
            .collect(ImmutableList.toImmutableList()),
        containsInAnyOrder(vxlanEdgeToRow(nc, node2, node1)));
  }

  @Test
  public void testVxlanEdgeToRow() {
    NetworkConfigurations nc = buildVxlanNetworkConfigurations();

    assertThat(
        vxlanEdgeToRow(
            nc,
            VxlanNode.builder().setHostname(VXLAN_NODE1).setVni(VXLAN_VNI).build(),
            VxlanNode.builder().setHostname(VXLAN_NODE2).setVni(VXLAN_VNI).build()),
        equalTo(
            Row.builder()
                .put(EdgesAnswerer.COL_VNI, VXLAN_VNI)
                .put(EdgesAnswerer.COL_NODE, new Node(VXLAN_NODE1))
                .put(EdgesAnswerer.COL_REMOTE_NODE, new Node(VXLAN_NODE2))
                .put(EdgesAnswerer.COL_VTEP_ADDRESS, VXLAN_SRC_IP1)
                .put(EdgesAnswerer.COL_REMOTE_VTEP_ADDRESS, VXLAN_SRC_IP2)
                .put(EdgesAnswerer.COL_VLAN, VXLAN_VLAN1)
                .put(EdgesAnswerer.COL_REMOTE_VLAN, VXLAN_VLAN2)
                .put(EdgesAnswerer.COL_UDP_PORT, VXLAN_UDP_PORT)
                .put(EdgesAnswerer.COL_MULTICAST_GROUP, VXLAN_MULTICAST_GROUP)
                .build()));
  }

  @Test
  public void testGetIsisEdges() {
    IsisNode node1 = new IsisNode("host1", "int1");
    IsisNode node2 = new IsisNode("host2", "int2");

    IsisEdge edge = new IsisEdge(IsisLevel.LEVEL_1, node1, node2);

    MutableNetwork<IsisNode, IsisEdge> isisTopology =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();
    isisTopology.addEdge(node1, node2, edge);

    Multiset<Row> rows =
        getIsisEdges(_includeNodes, _includeRemoteNodes, new IsisTopology(isisTopology));
    assertThat(
        rows,
        contains(
            allOf(
                hasColumn(
                    COL_INTERFACE,
                    equalTo(new NodeInterfacePair("host1", "int1")),
                    Schema.INTERFACE),
                hasColumn(
                    COL_REMOTE_INTERFACE,
                    equalTo(new NodeInterfacePair("host2", "int2")),
                    Schema.INTERFACE))));
  }

  @Test
  public void testGetLayer1Edges() {
    Layer1Node layer1Node1 = new Layer1Node("host1", "int1");
    Layer1Node layer1Node2 = new Layer1Node("host2", "int2");

    Multiset<Row> rows =
        getLayer1Edges(
            _includeNodes,
            _includeRemoteNodes,
            new Layer1Topology(ImmutableSortedSet.of(new Layer1Edge(layer1Node1, layer1Node2))));
    assertThat(
        rows,
        contains(
            allOf(
                hasColumn(
                    COL_INTERFACE,
                    equalTo(new NodeInterfacePair("host1", "int1")),
                    Schema.INTERFACE),
                hasColumn(
                    COL_REMOTE_INTERFACE,
                    equalTo(new NodeInterfacePair("host2", "int2")),
                    Schema.INTERFACE))));
  }

  @Test
  public void testGetLayer3Edges() {
    Topology layer3Topology =
        new Topology(ImmutableSortedSet.of(Edge.of("host1", "int1", "host2", "int2")));

    Multiset<Row> rows =
        getLayer3Edges(_configurations, _includeNodes, _includeRemoteNodes, layer3Topology);

    assertThat(
        rows,
        contains(
            allOf(
                hasColumn(
                    COL_INTERFACE,
                    equalTo(new NodeInterfacePair("host1", "int1")),
                    Schema.INTERFACE),
                hasColumn(
                    COL_IPS, equalTo(ImmutableSet.of(Ip.parse("1.1.1.1"))), Schema.set(Schema.IP)),
                hasColumn(
                    COL_REMOTE_INTERFACE,
                    equalTo(new NodeInterfacePair("host2", "int2")),
                    Schema.INTERFACE),
                hasColumn(
                    COL_REMOTE_IPS,
                    equalTo(ImmutableSet.of(Ip.parse("2.2.2.2"))),
                    Schema.set(Schema.IP)))));
  }

  @Test
  public void testEigrpToRow() {
    EigrpEdge testEdge =
        new EigrpEdge(
            new EigrpInterface("host1", "int1", "vrf1"),
            new EigrpInterface("host2", "int2", "vrf2"));
    Row row = eigrpEdgeToRow(testEdge);

    assertThat(
        row,
        allOf(
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testOspfToRow() {
    Row row = getOspfEdgeRow("host1", "int1", "host2", "int2");

    assertThat(
        row,
        allOf(
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testIsisToRow() {
    IsisEdge testEdge =
        new IsisEdge(
            IsisLevel.LEVEL_1, new IsisNode("host1", "int1"), new IsisNode("host2", "int2"));
    Row row = isisEdgeToRow(testEdge);

    assertThat(
        row,
        allOf(
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testRipToRow() {
    Row row = getRipEdgeRow("host1", "int1", "host2", "int2");

    assertThat(
        row,
        allOf(
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testLayer1ToRow() {
    Row row = layer1EdgeToRow(new Layer1Edge("host1", "int1", "host2", "int2"));
    assertThat(
        row,
        allOf(
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testLayer2ToRow() {
    Row row = layer2EdgeToRow(new Layer2Edge("host1", "int1", 1, "host2", "int2", 2, 12));
    assertThat(
        row,
        allOf(
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(COL_VLAN, equalTo("1"), Schema.STRING),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE),
            hasColumn(COL_REMOTE_VLAN, equalTo("2"), Schema.STRING)));
  }

  @Test
  public void testLayer3ToRow() {
    Map<String, Configuration> configurationMap =
        ImmutableSortedMap.of("host1", _host1, "host2", _host2);
    Row row = layer3EdgeToRow(configurationMap, Edge.of("host1", "int1", "host2", "int2"));
    assertThat(
        row,
        allOf(
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(
                COL_IPS, equalTo(ImmutableSet.of(Ip.parse("1.1.1.1"))), Schema.set(Schema.IP)),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE),
            hasColumn(
                COL_REMOTE_IPS,
                equalTo(ImmutableSet.of(Ip.parse("2.2.2.2"))),
                Schema.set(Schema.IP))));
  }

  @Test
  public void testTableMetadataLayer3() {
    List<ColumnMetadata> columnMetadata = getTableMetadata(EdgeType.LAYER3).getColumnMetadata();
    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        contains(COL_INTERFACE, COL_IPS, COL_REMOTE_INTERFACE, COL_REMOTE_IPS));

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        contains(Schema.INTERFACE, Schema.set(Schema.IP), Schema.INTERFACE, Schema.set(Schema.IP)));
  }

  @Test
  public void testTableMetadataLayer2() {
    List<ColumnMetadata> columnMetadata = getTableMetadata(EdgeType.LAYER2).getColumnMetadata();
    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        contains(COL_INTERFACE, COL_VLAN, COL_REMOTE_INTERFACE, COL_REMOTE_VLAN));

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        contains(Schema.INTERFACE, Schema.STRING, Schema.INTERFACE, Schema.STRING));
  }

  @Test
  public void testTableMetadataBgp() {
    List<ColumnMetadata> columnMetadata = getTableMetadata(EdgeType.BGP).getColumnMetadata();
    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        contains(
            COL_NODE,
            COL_IP,
            COL_INTERFACE,
            COL_AS_NUMBER,
            COL_REMOTE_NODE,
            COL_REMOTE_IP,
            COL_REMOTE_INTERFACE,
            COL_REMOTE_AS_NUMBER));

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        contains(
            Schema.NODE,
            Schema.IP,
            Schema.STRING,
            Schema.STRING,
            Schema.NODE,
            Schema.IP,
            Schema.STRING,
            Schema.STRING));
  }

  @Test
  public void testTableMetadataOthers() {
    List<ColumnMetadata> columnMetadata = getTableMetadata(EdgeType.OSPF).getColumnMetadata();
    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        contains(COL_INTERFACE, COL_REMOTE_INTERFACE));

    assertThat(
        columnMetadata.stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        contains(Schema.INTERFACE, Schema.INTERFACE));
  }
}

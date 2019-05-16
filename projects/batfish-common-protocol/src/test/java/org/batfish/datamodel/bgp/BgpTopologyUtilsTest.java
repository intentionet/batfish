package org.batfish.datamodel.bgp;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.bgp.BgpTopologyUtils.initBgpTopology;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ValueGraph;
import java.util.Map;
import java.util.Set;
import org.batfish.common.topology.Layer2Edge;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Tests of {@link BgpTopologyUtils} */
public class BgpTopologyUtilsTest {

  private static String NODE1 = "n1";
  private static String NODE2 = "n2";
  private static String NODE3 = "n3";
  private static BgpProcess _node1BgpProcess = new BgpProcess(Ip.parse("0.0.0.1"));
  private static BgpProcess _node2BgpProcess = new BgpProcess(Ip.parse("0.0.0.2"));
  private static BgpProcess _node3BgpProcess = new BgpProcess(Ip.parse("0.0.0.3"));
  private static Map<String, Configuration> _configs;

  /** Sets up three nodes with a BGP process on each. Tests can populate BGP processes. */
  @BeforeClass
  public static void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    Vrf vrf1 = new Vrf(DEFAULT_VRF_NAME);
    vrf1.setBgpProcess(_node1BgpProcess);
    Configuration c1 = cb.setHostname(NODE1).build();
    c1.setVrfs(ImmutableMap.of(DEFAULT_VRF_NAME, vrf1));

    Vrf vrf2 = new Vrf(DEFAULT_VRF_NAME);
    vrf2.setBgpProcess(_node2BgpProcess);
    Configuration c2 = cb.setHostname(NODE2).build();
    c2.setVrfs(ImmutableMap.of(DEFAULT_VRF_NAME, vrf2));

    Vrf vrf3 = new Vrf(DEFAULT_VRF_NAME);
    vrf3.setBgpProcess(_node3BgpProcess);
    Configuration c3 = cb.setHostname(NODE3).build();
    c3.setVrfs(ImmutableMap.of(DEFAULT_VRF_NAME, vrf3));

    _configs = ImmutableMap.of(NODE1, c1, NODE2, c2, NODE3, c3);
  }

  @Before
  public void clearBgpProcesses() {
    _node1BgpProcess.setNeighbors(ImmutableSortedMap.of());
    _node2BgpProcess.setNeighbors(ImmutableSortedMap.of());
    _node3BgpProcess.setNeighbors(ImmutableSortedMap.of());
    _node1BgpProcess.setPassiveNeighbors(ImmutableSortedMap.of());
    _node2BgpProcess.setPassiveNeighbors(ImmutableSortedMap.of());
    _node3BgpProcess.setPassiveNeighbors(ImmutableSortedMap.of());
    _node1BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of());
    _node2BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of());
    _node3BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of());
  }

  @Test
  public void testInitTopologyRemotePrefixNotMatchingLocalIp() {
    // Peer 1 on node1 with IP 1.1.1.1 is active, set up to peer with 2.2.2.2
    // Peer 2 on node2 with IP 2.2.2.2 is passive, with remote prefix 1.1.1.0/24
    // Should see one session come up in BGP topology: peer 1 to peer 2

    Ip ip1 = Ip.parse("1.1.1.1");
    Ip ip2 = Ip.parse("2.2.2.2");

    Prefix peer1PeerPrefix = Prefix.create(ip2, 32);
    BgpActivePeerConfig peer1 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip1)
            .setLocalAs(1L)
            .setPeerAddress(ip2)
            .setRemoteAs(2L)
            .build();
    _node1BgpProcess.setNeighbors(ImmutableSortedMap.of(peer1PeerPrefix, peer1));

    Prefix peer2PeerPrefix = Prefix.create(ip1, 24);
    BgpPassivePeerConfig peer2 =
        BgpPassivePeerConfig.builder()
            .setLocalIp(Ip.AUTO)
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setPeerPrefix(peer2PeerPrefix)
            .build();
    _node2BgpProcess.setPassiveNeighbors(ImmutableSortedMap.of(peer2PeerPrefix, peer2));

    Map<Ip, Set<String>> ipOwners =
        ImmutableMap.of(ip1, ImmutableSet.of(NODE1), ip2, ImmutableSet.of(NODE2));

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(_configs, ipOwners, true, false, null, null).getGraph();
    assertThat(bgpTopology.edges(), hasSize(2));
    EndpointPair<BgpPeerConfigId> edge = bgpTopology.edges().iterator().next();
    assertThat(edge.source().getHostname(), equalTo(NODE1));
    assertThat(edge.target().getHostname(), equalTo(NODE2));
  }

  @Test
  public void testInitTopologyPeerAddressNotMatchingRemoteHost() {
    // Peer 1 on node1 with IP 1.1.1.1 is active, set up to peer with 1.1.1.2
    // Peer 2 on node2 with IP 1.1.1.2 is passive, able to peer with peer 1
    // Peer 3 has the same configuration as peer 2, but on node3 with IP 1.1.1.3
    // Should see one session come up in BGP topology: peer 1 to peer 2

    Ip ip1 = Ip.parse("1.1.1.1");
    Ip ip2 = Ip.parse("1.1.1.2");
    Ip ip3 = Ip.parse("1.1.1.3");

    Prefix peer1PeerPrefix = Prefix.create(ip2, 32);
    BgpActivePeerConfig peer1 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip1)
            .setLocalAs(1L)
            .setPeerAddress(ip2)
            .setRemoteAs(2L)
            .build();
    _node1BgpProcess.setNeighbors(ImmutableSortedMap.of(peer1PeerPrefix, peer1));

    Prefix prefixForPeer1 = Prefix.create(ip1, 24);
    BgpPassivePeerConfig.Builder passivePeerBuilder =
        BgpPassivePeerConfig.builder()
            .setLocalIp(Ip.AUTO)
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setPeerPrefix(prefixForPeer1);

    BgpPassivePeerConfig peer2 = passivePeerBuilder.build();
    BgpPassivePeerConfig peer3 = passivePeerBuilder.build();
    _node2BgpProcess.setPassiveNeighbors(ImmutableSortedMap.of(prefixForPeer1, peer2));
    _node3BgpProcess.setPassiveNeighbors(ImmutableSortedMap.of(prefixForPeer1, peer3));

    Map<Ip, Set<String>> ipOwners =
        ImmutableMap.of(
            ip1, ImmutableSet.of(NODE1), ip2, ImmutableSet.of(NODE2), ip3, ImmutableSet.of(NODE3));

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(_configs, ipOwners, true, false, null, null).getGraph();
    assertThat(bgpTopology.edges(), hasSize(2));
    EndpointPair<BgpPeerConfigId> edge = bgpTopology.edges().iterator().next();
    assertThat(edge.source().getHostname(), equalTo(NODE1));
    assertThat(edge.target().getHostname(), equalTo(NODE2));
  }

  @Test
  public void testInitTopologyBgpUnnumberedEbgp() {
    /*
         AS 1          AS 2
           N1 ---------- N2
      Peers on N1 and N2 are compatible. Session should come up iff in the same broadcast domain.
    */

    String iface1 = "iface1";
    String iface2 = "iface2";

    BgpUnnumberedPeerConfig.Builder builder =
        BgpUnnumberedPeerConfig.builder().setLocalIp(Ip.parse("169.254.0.1"));
    BgpUnnumberedPeerConfig peer1 =
        builder.setLocalAs(1L).setRemoteAs(2L).setPeerInterface(iface1).build();
    BgpUnnumberedPeerConfig peer2 =
        builder.setLocalAs(2L).setRemoteAs(1L).setPeerInterface(iface2).build();
    _node1BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface1, peer1));
    _node2BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface2, peer2));

    // Shouldn't see session come up if nodes are not connected in layer 2
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(_configs, ImmutableMap.of(), true, false, null, Layer2Topology.EMPTY)
            .getGraph();
    assertThat(bgpTopology.nodes(), hasSize(2));
    assertThat(bgpTopology.edges(), empty());

    // Should see session if they're connected
    Layer2Edge edge = new Layer2Edge(NODE1, iface1, null, NODE2, iface2, null, null);
    Layer2Topology connectedLayer2Topology = Layer2Topology.fromEdges(ImmutableSet.of(edge));
    bgpTopology =
        initBgpTopology(_configs, ImmutableMap.of(), true, false, null, connectedLayer2Topology)
            .getGraph();
    BgpPeerConfigId peer1Id = new BgpPeerConfigId(NODE1, DEFAULT_VRF_NAME, iface1);
    BgpPeerConfigId peer2To1Id = new BgpPeerConfigId(NODE2, DEFAULT_VRF_NAME, iface2);
    assertThat(bgpTopology.nodes(), hasSize(2));
    assertThat(
        bgpTopology.edges(),
        containsInAnyOrder(
            EndpointPair.ordered(peer1Id, peer2To1Id), EndpointPair.ordered(peer2To1Id, peer1Id)));

    // Node 1 and 2 both have layer 2 edges but are not connected to any common node
    Layer2Edge edge1To3 = new Layer2Edge(NODE2, iface2, null, "node3", "iface3", null, null);
    Layer2Edge edge2To4 = new Layer2Edge(NODE2, iface2, null, "node4", "iface4", null, null);
    Layer2Topology disconnected = Layer2Topology.fromEdges(ImmutableSet.of(edge1To3, edge2To4));
    bgpTopology =
        initBgpTopology(_configs, ImmutableMap.of(), true, false, null, disconnected).getGraph();
    assertThat(bgpTopology.nodes(), hasSize(2));
    assertThat(bgpTopology.edges(), empty());
  }

  @Test
  public void testInitTopologyBgpUnnumberedIbgp() {
    /*
         AS 1          AS 1
           N1 ---------- N2
      Peers on N1 and N2 are compatible and connected on layer 2. Session should come up.
    */

    String iface1 = "iface1";
    String iface2 = "iface2";

    BgpUnnumberedPeerConfig.Builder builder =
        BgpUnnumberedPeerConfig.builder().setLocalIp(Ip.parse("169.254.0.1"));
    BgpUnnumberedPeerConfig peer1 =
        builder.setLocalAs(1L).setRemoteAs(1L).setPeerInterface(iface1).build();
    BgpUnnumberedPeerConfig peer2 =
        builder.setLocalAs(1L).setRemoteAs(1L).setPeerInterface(iface2).build();
    _node1BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface1, peer1));
    _node2BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface2, peer2));

    Layer2Edge edge = new Layer2Edge(NODE1, iface1, null, NODE2, iface2, null, null);
    Layer2Topology connectedLayer2Topology = Layer2Topology.fromEdges(ImmutableSet.of(edge));
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(_configs, ImmutableMap.of(), true, false, null, connectedLayer2Topology)
            .getGraph();
    BgpPeerConfigId peer1Id = new BgpPeerConfigId(NODE1, DEFAULT_VRF_NAME, iface1);
    BgpPeerConfigId peer2To1Id = new BgpPeerConfigId(NODE2, DEFAULT_VRF_NAME, iface2);
    assertThat(bgpTopology.nodes(), hasSize(2));
    assertThat(
        bgpTopology.edges(),
        containsInAnyOrder(
            EndpointPair.ordered(peer1Id, peer2To1Id), EndpointPair.ordered(peer2To1Id, peer1Id)));
  }

  @Test
  public void testInitTopologyIncompatibleBgpUnnumbered() {
    /*
         AS 1          AS 2
           N1 ---------- N2
      Peers on N1 and N2 are NOT compatible: N2 peer has remote AS 3 instead of 1
    */

    String iface1 = "iface1";
    String iface2 = "iface2";

    BgpUnnumberedPeerConfig.Builder builder =
        BgpUnnumberedPeerConfig.builder().setLocalIp(Ip.parse("169.254.0.1"));
    BgpUnnumberedPeerConfig peer1 =
        builder.setLocalAs(1L).setRemoteAs(2L).setPeerInterface(iface1).build();
    BgpUnnumberedPeerConfig peer2 =
        builder.setLocalAs(2L).setRemoteAs(3L).setPeerInterface(iface2).build();
    _node1BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface1, peer1));
    _node2BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface2, peer2));

    Layer2Edge edge = new Layer2Edge(NODE1, iface1, null, NODE2, iface2, null, null);
    Layer2Topology connectedLayer2Topology = Layer2Topology.fromEdges(ImmutableSet.of(edge));

    // Shouldn't see session come up because of incompatible remote AS
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(_configs, ImmutableMap.of(), true, false, null, connectedLayer2Topology)
            .getGraph();
    assertThat(bgpTopology.nodes(), hasSize(2));
    assertThat(bgpTopology.edges(), empty());
  }
}

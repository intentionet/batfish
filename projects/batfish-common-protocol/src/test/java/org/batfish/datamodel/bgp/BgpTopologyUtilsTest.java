package org.batfish.datamodel.bgp;

import static org.batfish.datamodel.BgpPeerConfig.ALL_AS_NUMBERS;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.bgp.BgpTopologyUtils.bgpCandidateHasCompatibleIpOrPrefix;
import static org.batfish.datamodel.bgp.BgpTopologyUtils.computeAsPair;
import static org.batfish.datamodel.bgp.BgpTopologyUtils.initBgpTopology;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ValueGraph;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.topology.L3Adjacencies;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.BgpTopologyUtils.AsPair;
import org.batfish.datamodel.bgp.BgpTopologyUtils.ConfedSessionType;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Tests of {@link BgpTopologyUtils} */
public class BgpTopologyUtilsTest {

  private static String NODE1 = "n1";
  private static String NODE2 = "n2";
  private static String NODE3 = "n3";
  private static BgpProcess _node1BgpProcess =
      new BgpProcess(Ip.parse("0.0.0.1"), ConfigurationFormat.CISCO_IOS);
  private static BgpProcess _node2BgpProcess =
      new BgpProcess(Ip.parse("0.0.0.2"), ConfigurationFormat.CISCO_IOS);
  private static BgpProcess _node3BgpProcess =
      new BgpProcess(Ip.parse("0.0.0.3"), ConfigurationFormat.CISCO_IOS);
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

    Prefix peer1PeerPrefix = ip2.toPrefix();
    BgpActivePeerConfig peer1 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip1)
            .setLocalAs(1L)
            .setPeerAddress(ip2)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(
                Ipv4UnicastAddressFamily.builder()
                    .setAddressFamilyCapabilities(AddressFamilyCapabilities.builder().build())
                    .build())
            .build();
    _node1BgpProcess.setNeighbors(ImmutableSortedMap.of(peer1PeerPrefix, peer1));

    Prefix peer2PeerPrefix = Prefix.create(ip1, 24);
    BgpPassivePeerConfig peer2 =
        BgpPassivePeerConfig.builder()
            .setLocalIp(Ip.AUTO)
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setPeerPrefix(peer2PeerPrefix)
            .setIpv4UnicastAddressFamily(
                Ipv4UnicastAddressFamily.builder()
                    .setAddressFamilyCapabilities(AddressFamilyCapabilities.builder().build())
                    .build())
            .build();
    _node2BgpProcess.setPassiveNeighbors(ImmutableSortedMap.of(peer2PeerPrefix, peer2));

    Map<Ip, Map<String, Set<String>>> ipOwners =
        ImmutableMap.of(
            ip1,
            ImmutableMap.of(NODE1, ImmutableSet.of(DEFAULT_VRF_NAME)),
            ip2,
            ImmutableMap.of(NODE2, ImmutableSet.of(DEFAULT_VRF_NAME)));

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(_configs, ipOwners, true, false, null, null).getGraph();
    assertThat(bgpTopology.edges(), hasSize(2));
    EndpointPair<BgpPeerConfigId> edge = bgpTopology.edges().iterator().next();
    assertThat(edge.source().getHostname(), equalTo(NODE1));
    assertThat(edge.target().getHostname(), equalTo(NODE2));
  }

  @Test
  public void testInitTopologyNoSelfLoop() {
    // Peer 1 on node1 with IP 1.1.1.1 is active, set up to peer with 1.1.1.1
    // Should see no sessions, and should also not crash.

    Ip ip = Ip.parse("1.1.1.1");

    Prefix peer1PeerPrefix = ip.toPrefix();
    BgpActivePeerConfig peer1 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip)
            .setLocalAs(1L)
            .setPeerAddress(ip)
            .setRemoteAs(1L)
            .setIpv4UnicastAddressFamily(
                Ipv4UnicastAddressFamily.builder()
                    .setAddressFamilyCapabilities(AddressFamilyCapabilities.builder().build())
                    .build())
            .build();
    _node1BgpProcess.setNeighbors(ImmutableSortedMap.of(peer1PeerPrefix, peer1));

    Map<Ip, Map<String, Set<String>>> ipOwners =
        ImmutableMap.of(ip, ImmutableMap.of(NODE1, ImmutableSet.of(DEFAULT_VRF_NAME)));

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(_configs, ipOwners, true, false, null, null).getGraph();
    assertThat(bgpTopology.edges(), empty());
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

    Prefix peer1PeerPrefix = ip2.toPrefix();
    BgpActivePeerConfig peer1 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip1)
            .setLocalAs(1L)
            .setPeerAddress(ip2)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    _node1BgpProcess.setNeighbors(ImmutableSortedMap.of(peer1PeerPrefix, peer1));

    Prefix prefixForPeer1 = Prefix.create(ip1, 24);
    BgpPassivePeerConfig.Builder passivePeerBuilder =
        BgpPassivePeerConfig.builder()
            .setLocalIp(Ip.AUTO)
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .setPeerPrefix(prefixForPeer1);

    BgpPassivePeerConfig peer2 = passivePeerBuilder.build();
    BgpPassivePeerConfig peer3 = passivePeerBuilder.build();
    _node2BgpProcess.setPassiveNeighbors(ImmutableSortedMap.of(prefixForPeer1, peer2));
    _node3BgpProcess.setPassiveNeighbors(ImmutableSortedMap.of(prefixForPeer1, peer3));

    Map<Ip, Map<String, Set<String>>> ipOwners =
        ImmutableMap.of(
            ip1,
            ImmutableMap.of(NODE1, ImmutableSet.of(DEFAULT_VRF_NAME)),
            ip2,
            ImmutableMap.of(NODE2, ImmutableSet.of(DEFAULT_VRF_NAME)),
            ip3,
            ImmutableMap.of(NODE3, ImmutableSet.of(DEFAULT_VRF_NAME)));

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
        BgpUnnumberedPeerConfig.builder()
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .setLocalIp(Ip.parse("169.254.0.1"));
    BgpUnnumberedPeerConfig peer1 =
        builder
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .setPeerInterface(iface1)
            .build();
    BgpUnnumberedPeerConfig peer2 =
        builder
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .setPeerInterface(iface2)
            .build();
    _node1BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface1, peer1));
    _node2BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface2, peer2));

    // Shouldn't see session come up if nodes are not connected in layer 2
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(
                _configs, ImmutableMap.of(), true, false, null, new FixedL3Adjacencies(false))
            .getGraph();
    assertThat(bgpTopology.nodes(), hasSize(2));
    assertThat(bgpTopology.edges(), empty());

    // Should see session if they're connected
    bgpTopology =
        initBgpTopology(
                _configs, ImmutableMap.of(), true, false, null, new FixedL3Adjacencies(true))
            .getGraph();
    BgpPeerConfigId peer1Id = new BgpPeerConfigId(NODE1, DEFAULT_VRF_NAME, iface1);
    BgpPeerConfigId peer2To1Id = new BgpPeerConfigId(NODE2, DEFAULT_VRF_NAME, iface2);
    assertThat(bgpTopology.nodes(), hasSize(2));
    assertThat(
        bgpTopology.edges(),
        containsInAnyOrder(
            EndpointPair.ordered(peer1Id, peer2To1Id), EndpointPair.ordered(peer2To1Id, peer1Id)));
  }

  /**
   * For testing unnumbered links, make an L3 adjacencies that always returns the same value for
   * checking interfaces are point to point and in the same domain.
   */
  @ParametersAreNonnullByDefault
  private static class FixedL3Adjacencies implements L3Adjacencies {
    public FixedL3Adjacencies(boolean answer) {
      _answer = answer;
    }

    @Override
    public boolean inSamePointToPointDomain(NodeInterfacePair i1, NodeInterfacePair i2) {
      return _answer;
    }

    @Override
    public boolean inSameBroadcastDomain(NodeInterfacePair i1, NodeInterfacePair i2) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Optional<NodeInterfacePair> pairedPointToPointL3Interface(NodeInterfacePair iface) {
      throw new UnsupportedOperationException();
    }

    private final boolean _answer;
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
        builder
            .setLocalAs(1L)
            .setRemoteAs(1L)
            .setPeerInterface(iface1)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpUnnumberedPeerConfig peer2 =
        builder
            .setLocalAs(1L)
            .setRemoteAs(1L)
            .setPeerInterface(iface2)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    _node1BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface1, peer1));
    _node2BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface2, peer2));

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(
                _configs, ImmutableMap.of(), true, false, null, new FixedL3Adjacencies(true))
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
        builder
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setPeerInterface(iface1)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpUnnumberedPeerConfig peer2 =
        builder
            .setLocalAs(2L)
            .setRemoteAs(3L)
            .setPeerInterface(iface2)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    _node1BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface1, peer1));
    _node2BgpProcess.setInterfaceNeighbors(ImmutableSortedMap.of(iface2, peer2));

    // Shouldn't see session come up because of incompatible remote AS
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(
                _configs, ImmutableMap.of(), true, false, null, new FixedL3Adjacencies(true))
            .getGraph();
    assertThat(bgpTopology.nodes(), hasSize(2));
    assertThat(bgpTopology.edges(), empty());
  }

  private static void assertPair(
      @Nullable Long initiatorLocalAs,
      @Nullable Long initiatorConfed,
      @Nonnull LongSpace initiatorRemoteAsns,
      @Nullable Long listenerLocalAs,
      @Nullable Long listenerConfed,
      @Nonnull LongSpace listenerRemoteAsns,
      @Nullable AsPair result) {
    assertThat(
        computeAsPair(
            initiatorLocalAs,
            initiatorConfed,
            initiatorRemoteAsns,
            listenerLocalAs,
            listenerConfed,
            listenerRemoteAsns),
        result != null ? equalTo(result) : nullValue());
    assertThat(
        computeAsPair(
            listenerLocalAs,
            listenerConfed,
            listenerRemoteAsns,
            initiatorLocalAs,
            initiatorConfed,
            initiatorRemoteAsns),
        result != null ? equalTo(result.reverse()) : nullValue());
  }

  @Test
  public void testComputeAsPair() {
    // Misconfigured
    assertPair(null, null, ALL_AS_NUMBERS, 3L, null, ALL_AS_NUMBERS, null);
    assertPair(1L, null, ALL_AS_NUMBERS, null, null, ALL_AS_NUMBERS, null);
    // Direct match
    assertPair(
        1L,
        null,
        ALL_AS_NUMBERS,
        2L,
        null,
        ALL_AS_NUMBERS,
        new AsPair(1, 2, ConfedSessionType.NO_CONFED));
    assertPair(
        1L,
        null,
        LongSpace.of(2),
        2L,
        null,
        LongSpace.of(1),
        new AsPair(1, 2, ConfedSessionType.NO_CONFED));
    // Direct but no match
    assertPair(1L, null, LongSpace.of(2), 2L, null, LongSpace.of(3), null);
    // Direct match inside same confederation
    assertPair(
        1L,
        55L,
        LongSpace.of(2),
        2L,
        55L,
        LongSpace.of(1),
        new AsPair(1, 2, ConfedSessionType.WITHIN_CONFED));
    // No match across confederations, but confederation match
    assertPair(
        1L,
        55L,
        LongSpace.of(56),
        2L,
        56L,
        LongSpace.of(55),
        new AsPair(55, 56, ConfedSessionType.ACROSS_CONFED_BORDER));
    // Confed match
    assertPair(
        1L,
        3L,
        LongSpace.of(4),
        4L,
        null,
        LongSpace.of(3L),
        new AsPair(3, 4, ConfedSessionType.ACROSS_CONFED_BORDER));
    // Confed no match
    assertPair(1L, 3L, LongSpace.of(4), 4L, null, LongSpace.of(5), null);
    assertPair(1L, 3L, LongSpace.of(4), 4L, 9L, LongSpace.of(5), null);

    // One peer implicitly matches other's confed, they shares same AS
    assertPair(
        1L,
        3L,
        LongSpace.of(1L),
        1L,
        null,
        LongSpace.of(1L),
        new AsPair(1, 1, ConfedSessionType.WITHIN_CONFED));
    // One peer implicitly matches other's confed, but remote ASN doesn't overlap local AS
    assertPair(1L, 3L, LongSpace.of(2L), 1L, null, LongSpace.of(1L), null);
    assertPair(1L, 3L, LongSpace.of(1L), 1L, null, LongSpace.of(2L), null);

    // Compatible when ignoring confederations, but incompatible because non-matching confederations
    // are present
    assertPair(1L, 3L, LongSpace.of(4), 4L, 9L, LongSpace.of(1), null);
  }

  @Test
  public void testBgpCandidateHasCompatibleIpOrPrefix() {
    BgpActivePeerConfig initiator =
        BgpActivePeerConfig.builder()
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setPeerAddress(Ip.parse("4.4.4.4"))
            .build();

    {
      // passive candidate case
      assertTrue(
          bgpCandidateHasCompatibleIpOrPrefix(
              initiator,
              BgpPassivePeerConfig.builder().setPeerPrefix(Prefix.parse("2.2.2.0/24")).build()));
      assertFalse(
          bgpCandidateHasCompatibleIpOrPrefix(
              initiator,
              BgpPassivePeerConfig.builder().setPeerPrefix(Prefix.parse("3.3.3.0/24")).build()));
    }
    {
      // active candidate

      // candidate without local IP and matching peer address
      assertTrue(
          bgpCandidateHasCompatibleIpOrPrefix(
              initiator,
              BgpActivePeerConfig.builder()
                  .setPeerAddress(Ip.parse("2.2.2.2"))
                  .setEbgpMultihop(true)
                  .build()));

      // candidate without local IP and incompatible peer address
      assertFalse(
          bgpCandidateHasCompatibleIpOrPrefix(
              initiator,
              BgpActivePeerConfig.builder()
                  .setPeerAddress(Ip.parse("3.3.3.3"))
                  .setEbgpMultihop(true)
                  .build()));

      // candidate with compatible local IP
      assertTrue(
          bgpCandidateHasCompatibleIpOrPrefix(
              initiator,
              BgpActivePeerConfig.builder()
                  .setPeerAddress(Ip.parse("2.2.2.2"))
                  .setLocalIp(Ip.parse("4.4.4.4"))
                  .build()));

      // candidate with incompatible local IP
      assertFalse(
          bgpCandidateHasCompatibleIpOrPrefix(
              initiator,
              BgpActivePeerConfig.builder()
                  .setPeerAddress(Ip.parse("2.2.2.2"))
                  .setLocalIp(Ip.parse("3.3.3.3"))
                  .build()));
    }
  }
}

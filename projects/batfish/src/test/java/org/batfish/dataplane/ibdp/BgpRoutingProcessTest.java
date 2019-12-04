package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.BumTransportMethod.UNICAST_FLOOD_GROUP;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.vxlan.Layer2Vni.builder;
import static org.batfish.dataplane.ibdp.BgpRoutingProcess.initEvpnType3Route;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.EvpnType3Route;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.AddressFamily.Type;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.BgpTopology.EdgeId;
import org.batfish.datamodel.bgp.EvpnAddressFamily;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig.Builder;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.VniConfig;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.dataplane.rib.Rib;
import org.junit.Before;
import org.junit.Test;

/** Tests of {@link BgpRoutingProcess} */
public class BgpRoutingProcessTest {

  private Configuration _c;
  private Vrf _vrf;
  private Vrf _vrf2;
  private BgpProcess _bgpProcess;
  private BgpProcess _bgpProcess2;
  private BgpRoutingProcess _routingProcess;
  private NetworkFactory _nf;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _c =
        _nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("c1")
            .build();
    _vrf = _nf.vrfBuilder().setOwner(_c).setName(DEFAULT_VRF_NAME).build();
    _vrf2 = _nf.vrfBuilder().setOwner(_c).setName("vrf2").build();
    _bgpProcess =
        _nf.bgpProcessBuilder()
            .setRouterId(Ip.ZERO)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    _bgpProcess2 =
        _nf.bgpProcessBuilder()
            .setRouterId(Ip.ZERO)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    _vrf.setBgpProcess(_bgpProcess);
    _vrf2.setBgpProcess(_bgpProcess2);
    _routingProcess =
        new BgpRoutingProcess(
            _bgpProcess, _c, DEFAULT_VRF_NAME, new Rib(), BgpTopology.EMPTY, new PrefixTracer());
  }

  @Test
  public void testInitRibsEmpty() {
    // iBGP
    assertThat(_routingProcess._ibgpv4Rib.getRoutes(), empty());
    assertThat(_routingProcess._ibgpv4StagingRib.getRoutes(), empty());
    // eBGP
    assertThat(_routingProcess._ebgpv4Rib.getRoutes(), empty());
    assertThat(_routingProcess._ebgpv4StagingRib.getRoutes(), empty());
    // Combined bgp
    assertThat(_routingProcess._bgpv4Rib.getRoutes(), empty());
  }

  @Test
  public void testInitEvpnType3Route() {
    Ip ip = Ip.parse("1.1.1.1");
    ExtendedCommunity routeTarget = ExtendedCommunity.target(1, 1);
    RouteDistinguisher routeDistinguisher = RouteDistinguisher.from(ip, 1);
    int admin = 20;
    EvpnType3Route route =
        initEvpnType3Route(
            admin,
            Layer2Vni.builder()
                .setVlan(1)
                .setVni(10001)
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                .setSourceAddress(ip)
                .build(),
            routeTarget,
            routeDistinguisher,
            ip);
    assertThat(
        route,
        equalTo(
            EvpnType3Route.builder()
                .setAdmin(admin)
                .setRouteDistinguisher(routeDistinguisher)
                .setCommunities(ImmutableSet.of(routeTarget))
                .setNonRouting(true)
                .setProtocol(RoutingProtocol.BGP)
                .setOriginType(OriginType.EGP)
                .setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE)
                .setVniIp(ip)
                .setOriginatorIp(ip)
                .build()));
  }

  @Test
  public void testInitEvpnRoutes() {
    // Setup
    Ip localIp = Ip.parse("2.2.2.2");
    int vni = 10001;
    int vni2 = 10002;
    Builder vniConfigBuilder =
        Layer3VniConfig.builder()
            .setVni(vni)
            .setVrf(DEFAULT_VRF_NAME)
            .setRouteDistinguisher(RouteDistinguisher.from(_bgpProcess.getRouterId(), 2))
            .setRouteTarget(ExtendedCommunity.target(65500, vni))
            .setImportRouteTarget(VniConfig.importRtPatternForAnyAs(vni))
            .setAdvertiseV4Unicast(false);
    Layer3VniConfig vniConfig1 = vniConfigBuilder.build();
    Layer3VniConfig vniConfig2 =
        vniConfigBuilder
            .setVni(vni2)
            .setVrf(_vrf2.getName())
            .setRouteTarget(ExtendedCommunity.target(65500, vni2))
            .build();
    BgpActivePeerConfig evpnPeer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(localIp)
            .setLocalAs(2L)
            .setEvpnAddressFamily(
                EvpnAddressFamily.builder()
                    .setL2Vnis(ImmutableSet.of())
                    .setL3Vnis(ImmutableSet.of(vniConfig1, vniConfig2))
                    .setPropagateUnmatched(true)
                    .build())
            .build();
    _bgpProcess.getActiveNeighbors().put(localIp.toPrefix(), evpnPeer);
    _vrf.addLayer2Vni(
        builder()
            .setVni(vni)
            .setVlan(1)
            .setBumTransportMethod(UNICAST_FLOOD_GROUP)
            .setSourceAddress(localIp)
            .build());
    _vrf2.addLayer2Vni(
        builder()
            .setVni(vni2)
            .setVlan(2)
            .setBumTransportMethod(UNICAST_FLOOD_GROUP)
            .setSourceAddress(localIp)
            .build());

    Node node = new Node(_c);
    BgpRoutingProcess defaultProc =
        node.getVirtualRouters().get(DEFAULT_VRF_NAME).getBgpRoutingProcess();

    // Test
    defaultProc.initLocalEvpnRoutes(node);

    // The VRF/process that the neighbor is in
    assertThat(
        defaultProc.getUpdatesForMainRib().getRoutesStream().collect(ImmutableSet.toImmutableSet()),
        contains(
            EvpnType3Route.builder()
                .setVniIp(localIp)
                .setRouteDistinguisher(RouteDistinguisher.from(_bgpProcess.getRouterId(), 2))
                .setCommunities(ImmutableSet.of(ExtendedCommunity.target(65500, vni)))
                .setLocalPreference(Bgpv4Route.DEFAULT_LOCAL_PREFERENCE)
                .setNonRouting(true)
                .setOriginType(OriginType.EGP)
                .setProtocol(RoutingProtocol.BGP)
                .setAdmin(20)
                .setOriginatorIp(_bgpProcess.getRouterId())
                .build()));
    // Sibling VRF, for vni2
    assertThat(
        node.getVirtualRouters()
            .get("vrf2")
            .getBgpRoutingProcess()
            .getUpdatesForMainRib()
            .getRoutesStream()
            .collect(ImmutableSet.toImmutableSet()),
        contains(
            EvpnType3Route.builder()
                .setVniIp(localIp)
                .setRouteDistinguisher(RouteDistinguisher.from(_bgpProcess.getRouterId(), 2))
                .setCommunities(ImmutableSet.of(ExtendedCommunity.target(65500, vni2)))
                .setLocalPreference(Bgpv4Route.DEFAULT_LOCAL_PREFERENCE)
                .setNonRouting(true)
                .setOriginType(OriginType.EGP)
                .setProtocol(RoutingProtocol.BGP)
                .setAdmin(20)
                .setOriginatorIp(_bgpProcess.getRouterId())
                .build()));
  }

  @Test
  public void testComputeRtToVrfMapping() {
    Ip localIp = Ip.parse("2.2.2.2");
    int vni = 10001;
    int vni2 = 10002;
    Builder vniConfigBuilder =
        Layer3VniConfig.builder()
            .setVni(vni)
            .setVrf(_vrf.getName())
            .setRouteDistinguisher(RouteDistinguisher.from(_bgpProcess.getRouterId(), 2))
            .setRouteTarget(ExtendedCommunity.target(65500, vni))
            .setAdvertiseV4Unicast(false);
    Layer3VniConfig vniConfig1 = vniConfigBuilder.build();
    Layer3VniConfig vniConfig2 =
        vniConfigBuilder
            .setVni(vni2)
            .setVrf(_vrf2.getName())
            .setRouteTarget(ExtendedCommunity.target(65500, vni2))
            .build();
    Ip peerAddress = Ip.parse("1.1.1.1");
    BgpActivePeerConfig evpnPeer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(peerAddress)
            .setRemoteAs(1L)
            .setLocalIp(localIp)
            .setLocalAs(2L)
            .setEvpnAddressFamily(
                EvpnAddressFamily.builder()
                    .setL2Vnis(ImmutableSet.of())
                    .setL3Vnis(ImmutableSet.of(vniConfig1, vniConfig2))
                    .setPropagateUnmatched(true)
                    .build())
            .build();
    _bgpProcess.getActiveNeighbors().put(peerAddress.toPrefix(), evpnPeer);

    Map<String, String> actual = BgpRoutingProcess.computeRouteTargetToVrfMap(Stream.of(evpnPeer));
    assertThat(
        actual,
        equalTo(
            ImmutableMap.of(
                VniConfig.importRtPatternForAnyAs(vni),
                _vrf.getName(),
                VniConfig.importRtPatternForAnyAs(vni2),
                _vrf2.getName())));
  }

  @Test
  public void testQueueInitializationAddressFamiliesMustOverlap() {
    Ip ip1 = Ip.parse("1.1.1.1");
    Ip ip2 = Ip.parse("2.2.2.2");
    long localAs = 1;
    long remoteAs = 1;
    Prefix remotePeerPrefix = ip1.toPrefix();
    BgpActivePeerConfig peer1 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip1)
            .setLocalAs(localAs)
            .setRemoteAs(remoteAs)
            .setPeerAddress(ip2)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    BgpProcess bgpProc =
        BgpProcess.builder()
            .setRouterId(Ip.ZERO)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    bgpProc.setNeighbors(ImmutableSortedMap.of(remotePeerPrefix, peer1));
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> graph =
        ValueGraphBuilder.directed().build();

    BgpPeerConfigId peer1Id =
        new BgpPeerConfigId(_c.getHostname(), DEFAULT_VRF_NAME, remotePeerPrefix, false);
    BgpPeerConfigId peer2Id =
        new BgpPeerConfigId("someHost", DEFAULT_VRF_NAME, ip1.toPrefix(), false);
    BgpSessionProperties.Builder sessionBuilderForward =
        BgpSessionProperties.builder()
            .setHeadAs(remoteAs)
            .setTailAs(localAs)
            .setHeadIp(ip2)
            .setTailIp(ip1)
            .setAddressFamilies(ImmutableSet.of(Type.EVPN));
    BgpSessionProperties.Builder sessionBuilderReverse =
        BgpSessionProperties.builder()
            .setHeadAs(localAs)
            .setTailAs(remoteAs)
            .setHeadIp(ip1)
            .setTailIp(ip2)
            .setAddressFamilies(ImmutableSet.of(Type.EVPN));
    graph.putEdgeValue(peer1Id, peer2Id, sessionBuilderForward.build());
    graph.putEdgeValue(peer2Id, peer1Id, sessionBuilderReverse.build());

    BgpTopology topology = new BgpTopology(graph);
    BgpRoutingProcess routingProcess =
        new BgpRoutingProcess(
            bgpProc, _c, DEFAULT_VRF_NAME, new Rib(), topology, new PrefixTracer());

    // No compatible peers for IPv4
    assertThat(
        routingProcess
            .getEdgeIdStream(graph, BgpPeerConfig::getIpv4UnicastAddressFamily, Type.IPV4_UNICAST)
            .collect(Collectors.toSet()),
        empty());

    // Replace session to include IPv4 AF. Let routingProcess know we made the new topology
    graph.putEdgeValue(
        peer1Id,
        peer2Id,
        sessionBuilderForward.setAddressFamilies(ImmutableSet.of(Type.IPV4_UNICAST)).build());
    graph.putEdgeValue(
        peer2Id,
        peer1Id,
        sessionBuilderReverse.setAddressFamilies(ImmutableSet.of(Type.IPV4_UNICAST)).build());
    topology = new BgpTopology(graph);
    routingProcess.updateTopology(topology);

    assertThat(
        routingProcess
            .getEdgeIdStream(graph, BgpPeerConfig::getIpv4UnicastAddressFamily, Type.IPV4_UNICAST)
            .collect(Collectors.toSet()),
        contains(new EdgeId(peer2Id, peer1Id)));
  }

  /** Ensure that we send out EVPN type 3 routes to newly established BGP sessions */
  @Test
  public void testResendInitializationOnTopologyUpdate() {
    // Setup
    long localAs = 2;
    long peerAs = 1;
    Ip localIp = Ip.parse("2.2.2.2");
    Ip peerIp = Ip.parse("1.1.1.1");
    int vni = 10001;
    int vni2 = 10002;
    Builder vniConfigBuilder =
        Layer3VniConfig.builder()
            .setVni(vni)
            .setVrf(DEFAULT_VRF_NAME)
            .setRouteDistinguisher(RouteDistinguisher.from(_bgpProcess.getRouterId(), 2))
            .setRouteTarget(ExtendedCommunity.target(65500, vni))
            .setImportRouteTarget(VniConfig.importRtPatternForAnyAs(vni))
            .setAdvertiseV4Unicast(false);
    Layer3VniConfig vniConfig1 = vniConfigBuilder.build();
    String policyName = "POL";
    BgpActivePeerConfig evpnPeer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(peerIp)
            .setRemoteAs(peerAs)
            .setLocalIp(localIp)
            .setLocalAs(localAs)
            .setEvpnAddressFamily(
                EvpnAddressFamily.builder()
                    .setL2Vnis(ImmutableSet.of())
                    .setL3Vnis(ImmutableSet.of(vniConfig1))
                    .setPropagateUnmatched(true)
                    .setExportPolicy(policyName)
                    .build())
            .build();
    RoutingPolicy.builder()
        .setOwner(_c)
        .setName(policyName)
        .setStatements(Collections.singletonList(Statements.ExitAccept.toStaticStatement()))
        .build();
    _bgpProcess.getActiveNeighbors().put(peerIp.toPrefix(), evpnPeer);
    _vrf.addLayer2Vni(
        builder()
            .setVni(vni)
            .setVlan(1)
            .setBumTransportMethod(UNICAST_FLOOD_GROUP)
            .setSourceAddress(localIp)
            .build());
    _vrf2.addLayer2Vni(
        builder()
            .setVni(vni)
            .setVlan(2)
            .setBumTransportMethod(UNICAST_FLOOD_GROUP)
            .setSourceAddress(localIp)
            .build());

    Node node = new Node(_c);
    BgpRoutingProcess routingProcNode1 =
        node.getVirtualRouters().get(DEFAULT_VRF_NAME).getBgpRoutingProcess();

    /////////// Node 2

    Configuration c2 =
        _nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("c2")
            .build();
    Vrf vrf2 = _nf.vrfBuilder().setOwner(c2).setName(DEFAULT_VRF_NAME).build();
    BgpProcess bgp2 =
        _nf.bgpProcessBuilder()
            .setRouterId(Ip.MAX)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    vrf2.setBgpProcess(bgp2);
    BgpActivePeerConfig node2Peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(localIp)
            .setRemoteAs(localAs)
            .setLocalIp(peerIp)
            .setLocalAs(peerAs)
            .setEvpnAddressFamily(
                EvpnAddressFamily.builder()
                    .setL2Vnis(ImmutableSet.of())
                    .setL3Vnis(ImmutableSet.of())
                    .setPropagateUnmatched(true)
                    .build())
            .build();
    bgp2.getActiveNeighbors().put(localIp.toPrefix(), node2Peer);
    Node node2 = new Node(c2);
    BgpRoutingProcess routingProcNode2 =
        node2.getVirtualRouters().get(DEFAULT_VRF_NAME).getBgpRoutingProcess();
    routingProcNode2.initialize(node2);

    /*
    Test:
    1. initalize
    2. execute one iteration/clear initialization state
    3. Update topology
    4. Ensure BGP rib state (at least type 3 routes) are sent to new neighbors.
    */
    routingProcNode1.initialize(node);
    routingProcNode1.executeIteration(ImmutableMap.of());
    // Update topology
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> graph =
        ValueGraphBuilder.directed().build();
    BgpPeerConfigId peer1Id =
        new BgpPeerConfigId(_c.getHostname(), DEFAULT_VRF_NAME, peerIp.toPrefix(), false);
    BgpPeerConfigId peer2Id =
        new BgpPeerConfigId("c2", DEFAULT_VRF_NAME, localIp.toPrefix(), false);
    BgpSessionProperties.Builder sessionBuilderForward =
        BgpSessionProperties.builder()
            .setHeadAs(localAs)
            .setTailAs(peerAs)
            .setHeadIp(localIp)
            .setTailIp(peerIp)
            .setAddressFamilies(ImmutableSet.of(Type.EVPN));
    BgpSessionProperties.Builder sessionBuilderReverse =
        BgpSessionProperties.builder()
            .setHeadAs(peerAs)
            .setTailAs(localAs)
            .setHeadIp(peerIp)
            .setTailIp(localIp)
            .setAddressFamilies(ImmutableSet.of(Type.EVPN));
    graph.putEdgeValue(
        peer1Id,
        peer2Id,
        sessionBuilderForward.setAddressFamilies(ImmutableSet.of(Type.EVPN)).build());
    graph.putEdgeValue(
        peer2Id,
        peer1Id,
        sessionBuilderReverse.setAddressFamilies(ImmutableSet.of(Type.EVPN)).build());
    routingProcNode1.updateTopology(new BgpTopology(graph));
    routingProcNode2.updateTopology(new BgpTopology(graph));
    routingProcNode1.executeIteration(
        ImmutableSortedMap.of(
            node.getConfiguration().getHostname(),
            node,
            node2.getConfiguration().getHostname(),
            node2));

    assertThat(
        routingProcNode2._evpnType3IncomingRoutes.get(new BgpTopology.EdgeId(peer1Id, peer2Id)),
        not(empty()));
  }
}

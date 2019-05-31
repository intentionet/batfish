package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.dataplane.ibdp.BgpRoutingProcess.initEvpnType3Route;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.stream.Stream;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
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
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.EvpnAddressFamily;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig.Builder;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
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

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    _c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("c1")
            .build();
    _vrf = nf.vrfBuilder().setOwner(_c).setName(DEFAULT_VRF_NAME).build();
    _vrf2 = nf.vrfBuilder().setOwner(_c).setName("vrf2").build();
    _bgpProcess =
        nf.bgpProcessBuilder()
            .setRouterId(Ip.ZERO)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    _bgpProcess2 =
        nf.bgpProcessBuilder()
            .setRouterId(Ip.ZERO)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    _vrf.setBgpProcess(_bgpProcess);
    _vrf2.setBgpProcess(_bgpProcess2);
    _routingProcess =
        new BgpRoutingProcess(_bgpProcess, _c, DEFAULT_VRF_NAME, new Rib(), BgpTopology.EMPTY);
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
            VniSettings.builder()
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
            .setImportRouteTarget(Layer3VniConfig.importRtPatternForAnyAs(vni))
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
                new EvpnAddressFamily(ImmutableSet.of(), ImmutableSet.of(vniConfig1, vniConfig2)))
            .build();
    _bgpProcess
        .getActiveNeighbors()
        .put(Prefix.create(localIp, Prefix.MAX_PREFIX_LENGTH), evpnPeer);
    _vrf.getVniSettings()
        .put(
            vni,
            VniSettings.builder()
                .setVni(vni)
                .setVlan(1)
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                .setSourceAddress(localIp)
                .build());
    _vrf2
        .getVniSettings()
        .put(
            vni2,
            VniSettings.builder()
                .setVni(vni)
                .setVlan(2)
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
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
            .setVrf(DEFAULT_VRF_NAME)
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
    BgpActivePeerConfig evpnPeer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(localIp)
            .setLocalAs(2L)
            .setEvpnAddressFamily(
                new EvpnAddressFamily(ImmutableSet.of(), ImmutableSet.of(vniConfig1, vniConfig2)))
            .build();
    _bgpProcess
        .getActiveNeighbors()
        .put(Prefix.create(localIp, Prefix.MAX_PREFIX_LENGTH), evpnPeer);

    Map<String, String> actual = BgpRoutingProcess.computeRouteTargetToVrfMap(Stream.of(evpnPeer));
    assertThat(
        actual,
        equalTo(
            ImmutableMap.of(
                Layer3VniConfig.importRtPatternForAnyAs(vni),
                DEFAULT_VRF_NAME,
                Layer3VniConfig.importRtPatternForAnyAs(vni2),
                _vrf2.getName())));
  }
}

package org.batfish.representation.cumulus;

import static org.batfish.datamodel.InterfaceType.PHYSICAL;
import static org.batfish.representation.cumulus.CumulusConversions.computeBgpGenerationPolicyName;
import static org.batfish.representation.cumulus.CumulusConversions.computeMatchSuppressedSummaryOnlyPolicyName;
import static org.batfish.representation.cumulus.CumulusNcluConfiguration.computeBgpNeighborImportRoutingPolicy;
import static org.batfish.representation.cumulus.CumulusNcluConfiguration.computeLocalIpForBgpNeighbor;
import static org.batfish.representation.cumulus.CumulusNcluConfiguration.getSetNextHop;
import static org.batfish.representation.cumulus.CumulusNcluConfiguration.toCommunityList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.SortedMap;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Bgpv4Route.Builder;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.batfish.datamodel.routing_policy.expr.MatchCommunitySet;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.junit.Test;

/** Test for {@link CumulusNcluConfiguration}. */
public class CumulusNcluConfigurationTest {
  @Test
  public void testToInterface_active() {
    Interface vsIface = new Interface("swp1", CumulusInterfaceType.PHYSICAL, null, 1);
    org.batfish.datamodel.Interface viIface = new CumulusNcluConfiguration().toInterface(vsIface);
    assertTrue(viIface.getActive());
  }

  @Test
  public void testToInterface_inactive() {
    Interface vsIface = new Interface("swp1", CumulusInterfaceType.PHYSICAL, null, 1);
    vsIface.setDisabled(true);
    org.batfish.datamodel.Interface viIface = new CumulusNcluConfiguration().toInterface(vsIface);
    assertFalse(viIface.getActive());
  }

  @Test
  public void testToInterface_sub_active() {
    Interface vsIface = new Interface("swp1s1", CumulusInterfaceType.PHYSICAL, null, 1);
    org.batfish.datamodel.Interface viIface =
        new CumulusNcluConfiguration().toInterface(vsIface, "swp1");
    assertTrue(viIface.getActive());
  }

  @Test
  public void testToInterface_sub_inactive() {
    Interface vsIface = new Interface("swp1s1", CumulusInterfaceType.PHYSICAL, null, 1);
    vsIface.setDisabled(true);
    org.batfish.datamodel.Interface viIface =
        new CumulusNcluConfiguration().toInterface(vsIface, "swp1");
    assertFalse(viIface.getActive());
  }

  @Test
  public void testToRouteFilterLine() {
    IpPrefixListLine prefixListLine =
        new IpPrefixListLine(
            LineAction.PERMIT, 10, Prefix.parse("10.0.0.1/24"), new SubRange(27, 30));

    RouteFilterLine rfl = CumulusNcluConfiguration.toRouteFilterLine(prefixListLine);
    assertThat(
        rfl,
        equalTo(
            new RouteFilterLine(
                LineAction.PERMIT, Prefix.parse("10.0.0.1/24"), new SubRange(27, 30))));
  }

  @Test
  public void testToRouteFilter() {
    IpPrefixList prefixList = new IpPrefixList("name");
    prefixList
        .getLines()
        .put(
            10L,
            new IpPrefixListLine(
                LineAction.DENY, 10, Prefix.parse("10.0.0.1/24"), new SubRange(27, 30)));
    prefixList
        .getLines()
        .put(
            20L,
            new IpPrefixListLine(
                LineAction.PERMIT, 20, Prefix.parse("10.0.2.1/24"), new SubRange(28, 31)));

    RouteFilterList rfl = CumulusNcluConfiguration.toRouteFilterList(prefixList);

    assertThat(
        rfl,
        equalTo(
            new RouteFilterList(
                "name",
                ImmutableList.of(
                    new RouteFilterLine(
                        LineAction.DENY, Prefix.parse("10.0.0.1/24"), new SubRange(27, 30)),
                    new RouteFilterLine(
                        LineAction.PERMIT, Prefix.parse("10.0.2.1/24"), new SubRange(28, 31))))));
  }

  @Test
  public void testToIpCommunityList() {
    String name = "name";
    IpCommunityListExpanded ipCommunityList =
        new IpCommunityListExpanded(
            name,
            LineAction.PERMIT,
            ImmutableList.of(StandardCommunity.of(10000, 1), StandardCommunity.of(20000, 2)));
    CommunityList result = toCommunityList(ipCommunityList);
    assertThat(
        result,
        equalTo(
            new CommunityList(
                name,
                ImmutableList.of(
                    new CommunityListLine(
                        LineAction.PERMIT, new LiteralCommunity(StandardCommunity.of(10000, 1))),
                    new CommunityListLine(
                        LineAction.PERMIT, new LiteralCommunity(StandardCommunity.of(20000, 2)))),
                false)));
  }

  @Test
  public void testGetAcceptStatements() {
    BgpNeighbor bgpNeighbor = new BgpIpNeighbor("10.0.0.1");
    BgpVrf bgpVrf = new BgpVrf("bgpVrf");

    {
      // if no as set, do not set next-hop-self
      SetNextHop setNextHop = getSetNextHop(bgpNeighbor, bgpVrf);
      assertNull(setNextHop);
    }

    {
      // if is not ibgp, do not set next-hop-self
      bgpNeighbor.setRemoteAs(10000L);
      bgpVrf.setAutonomousSystem(20000L);
      SetNextHop setNextHop = getSetNextHop(bgpNeighbor, bgpVrf);
      assertNull(setNextHop);
    }

    {
      // if is ibgp but no address family set, do not set next-hop-self
      bgpNeighbor.setRemoteAs(10000L);
      bgpVrf.setAutonomousSystem(10000L);
      SetNextHop setNextHop = getSetNextHop(bgpNeighbor, bgpVrf);
      assertNull(setNextHop);
    }

    {
      // if is ibgp and has address family set but no neighbor configuration, do not set
      // next-hop-self
      bgpNeighbor.setRemoteAs(10000L);
      bgpVrf.setAutonomousSystem(10000L);
      bgpVrf.setIpv4Unicast(new BgpIpv4UnicastAddressFamily());
      SetNextHop setNextHop = getSetNextHop(bgpNeighbor, bgpVrf);
      assertNull(setNextHop);
    }

    {
      // if is ibgp but neighbor configuration does not have next-hop-self set, do not set
      // next-hop-self
      bgpNeighbor.setRemoteAs(10000L);
      bgpVrf.setAutonomousSystem(10000L);
      bgpNeighbor.setIpv4UnicastAddressFamily(new BgpNeighborIpv4UnicastAddressFamily());

      assertNull(getSetNextHop(bgpNeighbor, bgpVrf));
    }

    {
      // if is ibgp and neighbor configuration set next-hop-self set, then set
      // next-hop-self
      bgpNeighbor.setRemoteAs(10000L);
      bgpVrf.setAutonomousSystem(10000L);
      BgpNeighborIpv4UnicastAddressFamily ipv4af = new BgpNeighborIpv4UnicastAddressFamily();
      bgpNeighbor.setIpv4UnicastAddressFamily(ipv4af);
      ipv4af.setNextHopSelf(true);

      assertThat(
          getSetNextHop(bgpNeighbor, bgpVrf), equalTo(new SetNextHop(SelfNextHop.getInstance())));
    }
  }

  @Test
  public void testComputeLocalIpForBgpNeighbor() {
    Ip remoteIp = Ip.parse("1.1.1.1");
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CUMULUS_NCLU).build();
    String vrfName = "vrf";
    Vrf vrf = nf.vrfBuilder().setName(vrfName).setOwner(c).build();
    org.batfish.datamodel.Interface.Builder ib = nf.interfaceBuilder().setVrf(vrf);

    // Should not accept interface whose subnet doesn't include the remote IP
    InterfaceAddress addr1 = ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 24);
    ib.setType(PHYSICAL).setOwner(c).setName("i1").setAddress(addr1).build();
    assertNull(computeLocalIpForBgpNeighbor(remoteIp, c, vrfName));

    // Should not accept interface that owns the remote IP
    InterfaceAddress addr2 = ConcreteInterfaceAddress.create(remoteIp, 24);
    ib.setType(PHYSICAL).setOwner(c).setName("i2").setAddress(addr2).build();
    assertNull(computeLocalIpForBgpNeighbor(remoteIp, c, vrfName));

    // Should accept interface that doesn't own the remote IP but whose subnet does include it
    Ip ifaceIp = Ip.parse("1.1.1.2");
    InterfaceAddress addr3 = ConcreteInterfaceAddress.create(ifaceIp, 24);
    ib.setType(PHYSICAL).setOwner(c).setName("i3").setAddress(addr3).build();
    assertThat(computeLocalIpForBgpNeighbor(remoteIp, c, vrfName), equalTo(ifaceIp));
  }

  @Test
  public void testComputeLocalIpForBgpNeighbor_vrf() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CUMULUS_NCLU).build();

    Vrf vrf1 = nf.vrfBuilder().setOwner(c).build();
    Vrf vrf2 = nf.vrfBuilder().setOwner(c).build();

    org.batfish.datamodel.Interface.Builder ib =
        nf.interfaceBuilder().setOwner(c).setType(PHYSICAL);
    ib.setVrf(vrf1).setAddress(ConcreteInterfaceAddress.parse("1.1.1.1/24")).build();
    ib.setVrf(vrf2).setAddress(ConcreteInterfaceAddress.parse("2.2.2.2/24")).build();

    Ip remoteIp = Ip.parse("1.1.1.3");

    // vrf1 owns the compatible localIp = 1.1.1.1
    assertThat(
        computeLocalIpForBgpNeighbor(remoteIp, c, vrf1.getName()), equalTo(Ip.parse("1.1.1.1")));

    // vrf2 does not own a compatible localIp
    assertNull(computeLocalIpForBgpNeighbor(remoteIp, c, vrf2.getName()));
  }

  @Test
  public void testToBgpProcess_aggregateRoutes() {
    testToBgpProcess_aggregateRoutes(true);
    testToBgpProcess_aggregateRoutes(false);
  }

  private static void testToBgpProcess_aggregateRoutes(boolean summaryOnly) {
    // setup VI model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CUMULUS_NCLU).build();
    Vrf viVrf = nf.vrfBuilder().setOwner(viConfig).setName(Configuration.DEFAULT_VRF_NAME).build();

    // setup VS model
    CumulusNcluConfiguration vsConfig = new CumulusNcluConfiguration();
    BgpProcess bgpProcess = new BgpProcess();
    vsConfig.setBgpProcess(bgpProcess);
    vsConfig.setConfiguration(viConfig);

    // setup BgpVrf
    BgpVrf vrf = bgpProcess.getDefaultVrf();
    vrf.setRouterId(Ip.parse("1.1.1.1"));
    BgpIpv4UnicastAddressFamily ipv4Unicast = new BgpIpv4UnicastAddressFamily();
    Prefix prefix = Prefix.parse("1.2.3.0/24");

    BgpVrfAddressFamilyAggregateNetworkConfiguration agg =
        new BgpVrfAddressFamilyAggregateNetworkConfiguration();
    agg.setSummaryOnly(summaryOnly);

    ipv4Unicast.getAggregateNetworks().put(prefix, agg);
    vrf.setIpv4Unicast(ipv4Unicast);

    // the method under test
    vsConfig.toBgpProcess(Configuration.DEFAULT_VRF_NAME, vrf);

    // generation policy exists
    assertThat(
        viConfig.getRoutingPolicies(),
        hasKey(
            computeBgpGenerationPolicyName(
                true, Configuration.DEFAULT_VRF_NAME, prefix.toString())));

    // generated route exists
    assertTrue(viVrf.getGeneratedRoutes().stream().anyMatch(gr -> gr.getNetwork().equals(prefix)));

    if (summaryOnly) {
      // suppress summary only filter list exists
      assertThat(
          viConfig.getRouteFilterLists(),
          hasKey(computeMatchSuppressedSummaryOnlyPolicyName(viVrf.getName())));
    } else {
      // suppress summary only filter list does not exist
      assertThat(
          viConfig.getRouteFilterLists(),
          not(hasKey(computeMatchSuppressedSummaryOnlyPolicyName(viVrf.getName()))));
    }
  }

  @Test
  public void testGenerateBgpActivePeerConfig_SetEbgpMultiHop() {
    // set VI configuration
    Configuration configuration = new Configuration("Host", ConfigurationFormat.CUMULUS_NCLU);

    // set bgp neighbor
    BgpIpNeighbor neighbor = new BgpIpNeighbor("BgpNeighbor");
    neighbor.setRemoteAs(10000L);
    neighbor.setRemoteAsType(RemoteAsType.INTERNAL);
    neighbor.setPeerIp(Ip.parse("10.0.0.2"));
    neighbor.setEbgpMultihop(3L);

    // set bgp process
    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("10.0.0.1"), ConfigurationFormat.CUMULUS_NCLU);

    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    ncluConfiguration.generateBgpActivePeerConfig(
        neighbor,
        10000L,
        new BgpVrf("Vrf"),
        newProc,
        new RoutingPolicy("Routing", configuration),
        null,
        configuration);

    BgpActivePeerConfig peerConfig = newProc.getActiveNeighbors().get(Prefix.parse("10.0.0.2/32"));
    assertTrue(peerConfig.getEbgpMultihop());
  }

  @Test
  public void testGenerateBgpUnnumberedPeerConfig_SetEbgpMultiHop() {

    // set VI configuration
    Configuration configuration = new Configuration("Host", ConfigurationFormat.CUMULUS_NCLU);

    // set bgp neighbor
    BgpInterfaceNeighbor neighbor = new BgpInterfaceNeighbor("BgpNeighbor");
    neighbor.setRemoteAs(10000L);
    neighbor.setRemoteAsType(RemoteAsType.INTERNAL);
    neighbor.setEbgpMultihop(3L);

    // set bgp process
    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("10.0.0.1"), ConfigurationFormat.CUMULUS_NCLU);

    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    ncluConfiguration.generateBgpUnnumberedPeerConfig(
        neighbor,
        10000L,
        new BgpVrf("Vrf"),
        newProc,
        new RoutingPolicy("Routing", configuration),
        null);

    BgpUnnumberedPeerConfig peerConfig = newProc.getInterfaceNeighbors().get("BgpNeighbor");

    assertTrue(peerConfig.getEbgpMultihop());
  }

  @Test
  public void testConvertIpv4UnicastAddressFamily_deactivated() {
    // setup vi model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CUMULUS_NCLU).build();
    RoutingPolicy policy = nf.routingPolicyBuilder().build();

    // setup vs model
    CumulusNcluConfiguration vsConfig = new CumulusNcluConfiguration();
    vsConfig.setConfiguration(viConfig);
    BgpNeighborIpv4UnicastAddressFamily af = new BgpNeighborIpv4UnicastAddressFamily();
    af.setActivated(false); // explicitly deactivated
    af.setRouteReflectorClient(true);

    assertNull(vsConfig.convertIpv4UnicastAddressFamily(af, policy, null));
  }

  @Test
  public void testConvertIpv4UnicastAddressFamily_routeReflectorClient() {

    // setup vi model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CUMULUS_NCLU).build();
    RoutingPolicy policy = nf.routingPolicyBuilder().build();

    // setup vs model
    CumulusNcluConfiguration vsConfig = new CumulusNcluConfiguration();
    vsConfig.setConfiguration(viConfig);

    // route-reflector-client is false if ipv4af is null
    assertFalse(
        vsConfig.convertIpv4UnicastAddressFamily(null, policy, null).getRouteReflectorClient());

    // VI route-reflector-client is true if VS activate and route-reflector-client are both true
    {
      BgpNeighborIpv4UnicastAddressFamily af = new BgpNeighborIpv4UnicastAddressFamily();
      af.setActivated(true);
      af.setRouteReflectorClient(true);
      assertTrue(
          vsConfig.convertIpv4UnicastAddressFamily(af, policy, null).getRouteReflectorClient());
    }

    // Despite cumulus docs, GNS3 testing confirms VI route-reflector-client should be true even if
    // activate is null (i.e. not explicitly activated).
    {
      BgpNeighborIpv4UnicastAddressFamily af = new BgpNeighborIpv4UnicastAddressFamily();
      af.setRouteReflectorClient(true);
      assertTrue(
          vsConfig.convertIpv4UnicastAddressFamily(af, policy, null).getRouteReflectorClient());
    }
  }

  @Test
  public void testComputeBgpNeighborImportRoutingPolicy() {
    BgpNeighbor neighbor = new BgpIpNeighbor("neighbor");

    BgpNeighborIpv4UnicastAddressFamily neighborIpv4UnicastFamily =
        new BgpNeighborIpv4UnicastAddressFamily();

    neighborIpv4UnicastFamily.setRouteMapIn("peerMapIn");

    neighbor.setIpv4UnicastAddressFamily(neighborIpv4UnicastFamily);

    BgpVrf bgpVrf = new BgpVrf("bgpVrf");

    Configuration config = new Configuration("host", ConfigurationFormat.CUMULUS_NCLU);

    RoutingPolicy importPolicy = computeBgpNeighborImportRoutingPolicy(neighbor, bgpVrf, config);

    // checking the import policy is correct (i.e. it successfully calls the route map to
    // permite/block route).
    RoutingPolicy routemapPolicy =
        RoutingPolicy.builder()
            .setOwner(config)
            .setName("peerMapIn")
            .addStatement(
                new If(
                    "match community 10000:1 routes",
                    new MatchCommunitySet(new LiteralCommunity(StandardCommunity.parse("10000:1"))),
                    ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                    ImmutableList.of(Statements.ExitReject.toStaticStatement())))
            .build();

    Builder builder =
        Bgpv4Route.builder()
            .setOriginatorIp(Ip.parse("10.0.0.1"))
            .setOriginType(OriginType.EGP)
            .setProtocol(RoutingProtocol.BGP)
            .setNetwork(Prefix.parse("10.0.0.0/24"));
    {
      // should permit route
      Bgpv4Route ipv4Route =
          builder.setCommunities(ImmutableSet.of(StandardCommunity.parse("10000:1"))).build();

      Environment envIpv4 =
          Environment.builder(config)
              .setRoutingPolicies(ImmutableMap.of("peerMapIn", routemapPolicy))
              .setOriginalRoute(ipv4Route)
              .setDirection(Direction.IN)
              .build();
      Result result = importPolicy.call(envIpv4);
      assertTrue(result.getBooleanValue());
    }

    {
      // should block route
      Bgpv4Route ipv4Route =
          builder.setCommunities(ImmutableSet.of(StandardCommunity.parse("20000:1"))).build();

      Environment envIpv4 =
          Environment.builder(config)
              .setRoutingPolicies(ImmutableMap.of("peerMapIn", routemapPolicy))
              .setOriginalRoute(ipv4Route)
              .setDirection(Direction.IN)
              .build();
      Result result = importPolicy.call(envIpv4);
      assertFalse(result.getBooleanValue());
    }
  }

  @Test
  public void testToOspfProcess_NoRouterId() {
    OspfVrf ospfVrf = new OspfVrf(Configuration.DEFAULT_VRF_NAME);
    Vrf vrf = new Vrf(Configuration.DEFAULT_VRF_NAME);

    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    OspfProcess ospfProcess = ncluConfiguration.toOspfProcess(ospfVrf, vrf);
    assertThat(ospfProcess.getRouterId(), equalTo(Ip.parse("0.0.0.0")));
    assertThat(ospfProcess.getProcessId(), equalTo("1"));
    assertThat(
        ospfProcess.getReferenceBandwidth(),
        equalTo(org.batfish.representation.cumulus.OspfProcess.DEFAULT_REFERENCE_BANDWIDTH));
  }

  @Test
  public void testToOspfProcess_InferRouterId() {
    OspfVrf ospfVrf = new OspfVrf(Configuration.DEFAULT_VRF_NAME);

    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Loopback lo = ncluConfiguration.getLoopback();
    lo.setConfigured(true);
    lo.getAddresses().add(ConcreteInterfaceAddress.parse("1.1.1.1/24"));
    OspfProcess ospfProcess =
        ncluConfiguration.toOspfProcess(ospfVrf, new Vrf(Configuration.DEFAULT_VRF_NAME));
    assertThat(ospfProcess.getRouterId(), equalTo(Ip.parse("1.1.1.1")));
    assertThat(ospfProcess.getProcessId(), equalTo("1"));
    assertThat(
        ospfProcess.getReferenceBandwidth(),
        equalTo(org.batfish.representation.cumulus.OspfProcess.DEFAULT_REFERENCE_BANDWIDTH));
  }

  @Test
  public void testToOspfProcess_ConfigedRouterId() {
    OspfVrf ospfVrf = new OspfVrf(Configuration.DEFAULT_VRF_NAME);
    ospfVrf.setRouterId(Ip.parse("1.2.3.4"));

    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();

    OspfProcess ospfProcess =
        ncluConfiguration.toOspfProcess(ospfVrf, new Vrf(Configuration.DEFAULT_VRF_NAME));
    assertThat(ospfProcess.getRouterId(), equalTo(Ip.parse("1.2.3.4")));
    assertThat(ospfProcess.getProcessId(), equalTo("1"));
    assertThat(
        ospfProcess.getReferenceBandwidth(),
        equalTo(org.batfish.representation.cumulus.OspfProcess.DEFAULT_REFERENCE_BANDWIDTH));
  }

  @Test
  public void testInferRouterID() {
    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Loopback lo = ncluConfiguration.getLoopback();
    lo.setConfigured(true);
    lo.getAddresses().add(ConcreteInterfaceAddress.parse("1.1.1.1/24"));
    assertThat(ncluConfiguration.inferRouteId(), equalTo(Ip.parse("1.1.1.1")));
  }

  @Test
  public void testAddOspfInterfaces_HasArea() {
    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Interface vsIface = new Interface("iface", CumulusInterfaceType.PHYSICAL, null, null);
    vsIface.getOrCreateOspf().setOspfArea(1L);
    ncluConfiguration.getInterfaces().put("iface", vsIface);

    Vrf vrf = new Vrf(Configuration.DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    ncluConfiguration.addOspfInterfaces(vrf);
    assertThat(viIface.getOspfAreaName(), equalTo(1L));
  }

  @Test
  public void testAddOspfInterfaces_NoArea() {
    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Interface vsIface = new Interface("iface", CumulusInterfaceType.PHYSICAL, null, null);
    ncluConfiguration.getInterfaces().put("iface", vsIface);
    vsIface.getOrCreateOspf();

    Vrf vrf = new Vrf(Configuration.DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    assertNull(viIface.getOspfAreaName());
  }

  @Test
  public void testAddOspfInterfaces_NoNetworkType() {
    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Interface vsIface = new Interface("iface", CumulusInterfaceType.PHYSICAL, null, null);
    ncluConfiguration.getInterfaces().put("iface", vsIface);
    vsIface.getOrCreateOspf().setOspfArea(0L);

    Vrf vrf = new Vrf(Configuration.DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    ncluConfiguration.addOspfInterfaces(vrf);
    assertNull(viIface.getOspfNetworkType());
  }

  @Test
  public void testAddOspfInterfaces_NoPassiveInterface() {
    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Interface vsIface = new Interface("iface", CumulusInterfaceType.PHYSICAL, null, null);
    ncluConfiguration.getInterfaces().put("iface", vsIface);
    vsIface.getOrCreateOspf();

    Vrf vrf = new Vrf(Configuration.DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    ncluConfiguration.addOspfInterfaces(vrf);
    assertFalse(viIface.getOspfPassive());
  }

  @Test
  public void testAddOspfInterfaces_PassiveInterface() {
    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Interface vsIface = new Interface("iface", CumulusInterfaceType.PHYSICAL, null, null);
    ncluConfiguration.getInterfaces().put("iface", vsIface);
    OspfInterface ospf = vsIface.getOrCreateOspf();
    ospf.setOspfArea(0L);
    ospf.setPassive(true);

    Vrf vrf = new Vrf(Configuration.DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    ncluConfiguration.addOspfInterfaces(vrf);
    assertTrue(viIface.getOspfPassive());
  }

  @Test
  public void testAddOspfInterfaces_NetworkTypeP2P() {
    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Interface vsIface = new Interface("iface", CumulusInterfaceType.PHYSICAL, null, null);
    ncluConfiguration.getInterfaces().put("iface", vsIface);
    vsIface.getOrCreateOspf().setOspfArea(0L);
    vsIface.getOrCreateOspf().setNetwork(OspfNetworkType.POINT_TO_POINT);

    Vrf vrf = new Vrf(Configuration.DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    ncluConfiguration.addOspfInterfaces(vrf);
    assertThat(
        viIface.getOspfNetworkType(),
        equalTo(org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT));
  }

  @Test
  public void testComputeOspfProcess_HasArea() {
    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Interface vsIface = new Interface("iface", CumulusInterfaceType.PHYSICAL, null, null);
    vsIface.getOrCreateOspf().setOspfArea(1L);
    ncluConfiguration.getInterfaces().put("iface", vsIface);

    SortedMap<Long, OspfArea> areas = ncluConfiguration.computeOspfAreas(ImmutableList.of("iface"));
    assertThat(
        areas,
        equalTo(
            ImmutableSortedMap.of(
                1L, OspfArea.builder().addInterface("iface").setNumber(1L).build())));
  }

  @Test
  public void testComputeOspfProcess_NoArea() {
    CumulusNcluConfiguration ncluConfiguration = new CumulusNcluConfiguration();
    Interface vsIface = new Interface("iface", CumulusInterfaceType.PHYSICAL, null, null);
    ncluConfiguration.getInterfaces().put("iface", vsIface);
    vsIface.getOrCreateOspf();

    SortedMap<Long, OspfArea> areas = ncluConfiguration.computeOspfAreas(ImmutableList.of("iface"));
    assertThat(areas, equalTo(ImmutableSortedMap.of()));
  }

  @Test
  public void testPopulateLoopback() {
    Interface iface = new Interface("lo", CumulusInterfaceType.LOOPBACK, null, null);
    iface.getIpAddresses().add(ConcreteInterfaceAddress.parse("1.1.1.1/30"));
    Loopback loopback = new Loopback();

    CumulusNcluConfiguration.populateLoInInterfacesToLoopback(iface, loopback);
    assertThat(
        loopback.getAddresses(),
        equalTo(ImmutableList.of(ConcreteInterfaceAddress.parse("1.1.1.1/30"))));
  }

  @Test
  public void testToRouteMapSetLocalPref() {
    CumulusNcluConfiguration vendorConfiguration = new CumulusNcluConfiguration();
    RouteMap rm = new RouteMap("RM");
    RouteMapEntry rme = new RouteMapEntry(10, LineAction.PERMIT);
    rme.setSetLocalPreference(new RouteMapSetLocalPreference(200L));
    rm.getEntries().put(10, rme);
    RoutingPolicy policy = vendorConfiguration.toRouteMap(rm);

    StaticRoute.Builder routeBuilder =
        org.batfish.datamodel.StaticRoute.builder()
            .setNetwork(Prefix.ZERO)
            .setAdmin(1)
            .setNextHopInterface("iface");
    Builder outputBuilder = Bgpv4Route.builder();
    Environment.Builder env =
        Environment.builder(new Configuration("h", ConfigurationFormat.CUMULUS_CONCATENATED));
    policy.call(env.setOutputRoute(outputBuilder).build());
    assertThat(outputBuilder.getLocalPreference(), equalTo(200L));
  }
}

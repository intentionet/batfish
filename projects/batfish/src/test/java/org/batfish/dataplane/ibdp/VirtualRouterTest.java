package org.batfish.dataplane.ibdp;

import static org.batfish.common.topology.TopologyUtil.computeIpNodeOwners;
import static org.batfish.common.topology.TopologyUtil.synthesizeL3Topology;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.bgp.BgpTopologyUtils.initBgpTopology;
import static org.batfish.datamodel.eigrp.EigrpTopology.initEigrpTopology;
import static org.batfish.datamodel.isis.IsisTopology.initIsisTopology;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.EigrpExternalRoute;
import org.batfish.datamodel.EigrpInternalRoute;
import org.batfish.datamodel.EvpnType3Route;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.KernelRoute;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.EvpnAddressFamily;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.eigrp.EigrpProcessMode;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.isis.IsisNode;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.MatchSourceVrf;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;
import org.junit.Test;

/** Tests of {@link VirtualRouter} */
public class VirtualRouterTest {
  /** Make a CISCO IOS router with 3 interfaces named Eth1-Eth3, /16 prefixes on each interface */
  private static final Map<String, InterfaceAddress> exampleInterfaceAddresses =
      ImmutableMap.<String, InterfaceAddress>builder()
          .put("Ethernet1", new InterfaceAddress("10.1.0.0/16"))
          .put("Ethernet2", new InterfaceAddress("10.2.0.0/16"))
          .put("Ethernet3", new InterfaceAddress("10.3.0.0/16"))
          .build();

  private static void addInterfaces(
      Configuration c, Map<String, InterfaceAddress> interfaceAddresses) {
    NetworkFactory nf = new NetworkFactory();
    Interface.Builder ib = nf.interfaceBuilder().setOwner(c).setVrf(c.getDefaultVrf());
    interfaceAddresses.forEach(
        (ifaceName, address) ->
            ib.setName(ifaceName).setAddress(address).setBandwidth(100d).build());
  }

  private static Map<String, Node> makeIosRouters(String... hostnames) {
    return Arrays.stream(hostnames)
        .collect(ImmutableMap.toImmutableMap(hostname -> hostname, TestUtils::makeIosRouter));
  }

  private static VirtualRouter makeF5VirtualRouter(String hostname) {
    Node n = TestUtils.makeF5Router(hostname);
    return n.getVirtualRouters().get(DEFAULT_VRF_NAME);
  }

  private static VirtualRouter makeIosVirtualRouter(String hostname) {
    Node n = TestUtils.makeIosRouter(hostname);
    return n.getVirtualRouters().get(DEFAULT_VRF_NAME);
  }

  private static Set<AbstractRoute> makeOneRouteOfEveryType() {
    return ImmutableSet.of(
        Bgpv4Route.builder()
            .setNetwork(Prefix.parse("1.0.0.0/24"))
            .setOriginatorIp(Ip.parse("8.8.8.8"))
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.IBGP)
            .build(),
        ConnectedRoute.builder()
            .setNetwork(Prefix.parse("1.0.1.0/24"))
            .setNextHopInterface("iface")
            .build(),
        EigrpExternalRoute.builder()
            .setNetwork(Prefix.parse("1.0.2.0/24"))
            .setDestinationAsn(1L)
            .setEigrpMetric(
                EigrpMetric.builder()
                    .setBandwidth(1E8)
                    .setDelay(1D)
                    .setMode(EigrpProcessMode.CLASSIC)
                    .build())
            .setProcessAsn(2L)
            .build(),
        EigrpInternalRoute.builder()
            .setNetwork(Prefix.parse("1.0.3.0/24"))
            .setEigrpMetric(
                EigrpMetric.builder()
                    .setBandwidth(1E8)
                    .setDelay(1D)
                    .setMode(EigrpProcessMode.CLASSIC)
                    .build())
            .setProcessAsn(2L)
            .build(),
        GeneratedRoute.builder().setNetwork(Prefix.parse("1.0.4.0/24")).setAdmin(1).build(),
        IsisRoute.builder()
            .setNetwork(Prefix.parse("1.0.5.0/24"))
            .setLevel(IsisLevel.LEVEL_1)
            .setArea("0")
            .setProtocol(RoutingProtocol.ISIS_L1)
            .setSystemId("id")
            .build(),
        LocalRoute.builder()
            .setNetwork(Prefix.parse("1.0.6.0/24"))
            .setSourcePrefixLength(24)
            .build(),
        OspfInterAreaRoute.builder().setNetwork(Prefix.parse("1.0.7.0/24")).setArea(2L).build(),
        OspfIntraAreaRoute.builder().setNetwork(Prefix.parse("1.0.8.0/24")).setArea(2L).build(),
        OspfExternalType1Route.builder()
            .setNetwork(Prefix.parse("1.0.9.0/24"))
            .setOspfMetricType(OspfMetricType.E1)
            .setLsaMetric(2L)
            .setCostToAdvertiser(3L)
            .setAdvertiser("advertiser")
            .setArea(4L)
            .build(),
        OspfExternalType2Route.builder()
            .setNetwork(Prefix.parse("1.1.0.0/24"))
            .setOspfMetricType(OspfMetricType.E1)
            .setLsaMetric(2L)
            .setCostToAdvertiser(3L)
            .setAdvertiser("advertiser")
            .setArea(4L)
            .build(),
        RipInternalRoute.builder().setNetwork(Prefix.parse("1.1.1.0/24")).build(),
        StaticRoute.builder().setNetwork(Prefix.parse("1.1.2.0/24")).setAdmin(1).build());
  }

  /**
   * Test that {@link VirtualRouter#activateStaticRoutes()} removes a route if a route to its
   * next-hop IP disappears.
   */
  @Test
  public void testActivateStaticRoutesRemoval() {
    VirtualRouter vr = makeIosVirtualRouter("n1");
    addInterfaces(vr.getConfiguration(), exampleInterfaceAddresses);

    StaticRoute baseRoute =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("1.1.1.0/24"))
            .setNextHopInterface("Ethernet1")
            .setAdministrativeCost(1)
            .build();
    StaticRoute dependentRoute =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("2.2.2.2/32"))
            .setNextHopIp(Ip.parse("1.1.1.1"))
            .setAdministrativeCost(1)
            .build();

    vr.getConfiguration()
        .getVrfs()
        .get(DEFAULT_VRF_NAME)
        .setStaticRoutes(ImmutableSortedSet.of(baseRoute, dependentRoute));

    // Initial activation
    vr.initStaticRibs();
    vr.activateStaticRoutes();

    // Test: remove baseRoute, rerun activation
    vr.getMainRib().removeRoute(new AnnotatedRoute<>(baseRoute, DEFAULT_VRF_NAME));
    vr.activateStaticRoutes();

    // Assert dependent route is not there
    assertThat(vr.getMainRib().getRoutes(), not(hasItem(dependentRoute)));
  }

  /** Check that initialization of Connected RIB is as expected */
  @Test
  public void testInitConnectedRib() {
    // Setup
    VirtualRouter vr = makeIosVirtualRouter(null);
    addInterfaces(vr.getConfiguration(), exampleInterfaceAddresses);
    vr.initRibs();

    // Test
    vr.initConnectedRib();

    // Assert that all interface prefixes have been processed
    assertThat(
        vr.getConnectedRib().getTypedRoutes(),
        equalTo(
            exampleInterfaceAddresses.entrySet().stream()
                .map(
                    e ->
                        new AnnotatedRoute<>(
                            new ConnectedRoute(e.getValue().getPrefix(), e.getKey()),
                            DEFAULT_VRF_NAME))
                .collect(ImmutableSet.toImmutableSet())));
  }

  @Test
  public void testInitEvpnRoutes() {
    // Setup
    NetworkFactory nf = new NetworkFactory();
    VirtualRouter vr = makeIosVirtualRouter("c");
    Ip routerId = Ip.parse("1.1.1.1");
    vr.initRibs();
    BgpActivePeerConfig evpnPeer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setEvpnAddressFamily(
                new EvpnAddressFamily(
                    ImmutableSet.of(),
                    ImmutableSet.of(
                        new Layer3VniConfig(
                            1,
                            DEFAULT_VRF_NAME,
                            RouteDistinguisher.from(routerId, 2),
                            ExtendedCommunity.target(65500, 10001),
                            false))))
            .build();
    BgpProcess bgpProcess = nf.bgpProcessBuilder().setVrf(vr._vrf).setRouterId(routerId).build();
    bgpProcess.getActiveNeighbors().put(Prefix.parse("2.2.2.2/32"), evpnPeer);
    vr._vrf
        .getVniSettings()
        .put(
            1,
            VniSettings.builder()
                .setVni(1)
                .setVlan(1)
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                .setSourceAddress(Ip.parse("2.2.2.2"))
                .build());
    vr.initForEgpComputation(TopologyContext.builder().build());

    // Test
    vr.initEvpnRoutes();

    Ip vniSourceAddress = Ip.parse("2.2.2.2");
    assertThat(
        vr._bgpRoutingProcess._ebgpEvpnRib.getTypedRoutes(),
        equalTo(
            ImmutableSet.of(
                EvpnType3Route.builder()
                    .setVniIp(vniSourceAddress)
                    .setRouteDistinguisher(RouteDistinguisher.from(Ip.parse("1.1.1.1"), 2))
                    .setCommunities(ImmutableSet.of(ExtendedCommunity.target(65500, 10001)))
                    .setLocalPreference(Bgpv4Route.DEFAULT_LOCAL_PREFERENCE)
                    .setOriginType(OriginType.EGP)
                    .setProtocol(RoutingProtocol.BGP)
                    .setSrcProtocol(RoutingProtocol.BGP)
                    .setAdmin(20)
                    .setOriginatorIp(vniSourceAddress)
                    .build())));
  }

  /** Check that initialization of Kernel RIB is as expected */
  @Test
  public void testInitKernelRib() {
    // Setup
    VirtualRouter vr = makeF5VirtualRouter(null);
    vr.getConfiguration()
        .getDefaultVrf()
        .setKernelRoutes(ImmutableSortedSet.of(new KernelRoute(Prefix.ZERO)));
    vr.initRibs();

    // Test
    vr.initKernelRib();

    // Assert that all kernel routes have been processed
    assertThat(
        vr._kernelRib.getTypedRoutes(),
        equalTo(
            ImmutableSet.of(new AnnotatedRoute<>(new KernelRoute(Prefix.ZERO), DEFAULT_VRF_NAME))));
  }

  /** Check that initialization of Local RIB is as expected */
  @Test
  public void testInitLocalRib() {
    // Setup
    VirtualRouter vr = makeIosVirtualRouter(null);
    addInterfaces(vr.getConfiguration(), exampleInterfaceAddresses);
    vr.initRibs();

    // Test
    vr.initLocalRib();

    // Assert that all interface prefixes have been processed
    assertThat(
        vr._localRib.getTypedRoutes(),
        equalTo(
            exampleInterfaceAddresses.entrySet().stream()
                .filter(e -> e.getValue().getPrefix().getPrefixLength() < Prefix.MAX_PREFIX_LENGTH)
                .map(
                    e ->
                        new AnnotatedRoute<>(
                            new LocalRoute(e.getValue(), e.getKey()), DEFAULT_VRF_NAME))
                .collect(ImmutableSet.toImmutableSet())));
  }

  /** Check that VRF static routes are put into appropriate RIBs upon initialization. */
  @Test
  public void testInitStaticRibs() {
    VirtualRouter vr = makeIosVirtualRouter(null);
    addInterfaces(vr.getConfiguration(), exampleInterfaceAddresses);

    List<StaticRoute> routes =
        ImmutableList.of(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("1.1.1.1/32"))
                .setNextHopIp(null)
                .setNextHopInterface("Ethernet1")
                .setAdministrativeCost(1)
                .setMetric(0L)
                .setTag(1)
                .build(),
            StaticRoute.builder()
                .setNetwork(Prefix.parse("2.2.2.2/32"))
                .setNextHopIp(Ip.parse("9.9.9.8"))
                .setNextHopInterface(null)
                .setAdministrativeCost(1)
                .setMetric(0L)
                .setTag(1)
                .build(),
            StaticRoute.builder()
                .setNetwork(Prefix.parse("3.3.3.3/32"))
                .setNextHopIp(Ip.parse("9.9.9.9"))
                .setNextHopInterface("Ethernet1")
                .setAdministrativeCost(1)
                .setMetric(0L)
                .setTag(1)
                .build(),
            StaticRoute.builder()
                .setNetwork(Prefix.parse("4.4.4.4/32"))
                .setNextHopIp(null)
                .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
                .setAdministrativeCost(1)
                .setMetric(0L)
                .setTag(1)
                .build(),

            // These do not get activated due to missing/incorrect interface names
            StaticRoute.builder()
                .setNetwork(Prefix.parse("5.5.5.5/32"))
                .setNextHopIp(null)
                .setNextHopInterface("Eth1")
                .setAdministrativeCost(1)
                .setMetric(0L)
                .setTag(1)
                .build(),
            StaticRoute.builder()
                .setNetwork(Prefix.parse("6.6.6.6/32"))
                .setNextHopIp(null)
                .setNextHopInterface(null)
                .setAdministrativeCost(1)
                .setMetric(0L)
                .setTag(1)
                .build());
    vr.getConfiguration()
        .getVrfs()
        .get(DEFAULT_VRF_NAME)
        .setStaticRoutes(ImmutableSortedSet.copyOf(routes));

    // Test
    vr.initStaticRibs();

    assertThat(
        vr._staticInterfaceRib.getTypedRoutes(),
        containsInAnyOrder(routes.get(0), routes.get(2), routes.get(3)));
    assertThat(vr._staticNextHopRib.getTypedRoutes(), containsInAnyOrder(routes.get(1)));
  }

  @Test
  public void testInitRibsEmpty() {
    VirtualRouter vr = makeIosVirtualRouter(null);

    // We expect the router to have the following RIBs and all of them are empty
    vr.initRibs();

    // Simple RIBs
    assertThat(vr.getConnectedRib().getRoutes(), empty());
    assertThat(vr._staticNextHopRib.getRoutes(), empty());
    assertThat(vr._staticInterfaceRib.getRoutes(), empty());
    assertThat(vr._independentRib.getRoutes(), empty());

    // RIP RIBs
    assertThat(vr._ripInternalRib.getRoutes(), empty());
    assertThat(vr._ripInternalStagingRib.getRoutes(), empty());
    assertThat(vr._ripRib.getRoutes(), empty());

    // Main RIB
    assertThat(vr._mainRib.getRoutes(), empty());
  }

  /** Check that initialization of RIP internal routes happens correctly */
  @Test
  public void testRipInitialization() {
    // Incomplete Setup
    VirtualRouter vr = makeIosVirtualRouter(null);
    addInterfaces(vr.getConfiguration(), exampleInterfaceAddresses);
    vr.initRibs();
    vr.initBaseRipRoutes();

    // Check that nothing happens
    assertThat(vr._ripInternalRib.getRoutes(), empty());

    // Complete setup by adding a process
    RipProcess ripProcess = new RipProcess();
    ripProcess.setInterfaces(vr._vrf.getInterfaceNames());
    vr._vrf.setRipProcess(ripProcess);

    vr.initBaseRipRoutes();

    assertThat(
        vr._ripInternalRib.getTypedRoutes(),
        equalTo(
            exampleInterfaceAddresses.values().stream()
                .map(
                    address ->
                        new RipInternalRoute(
                            address.getPrefix(),
                            Route.UNSET_ROUTE_NEXT_HOP_IP,
                            RoutingProtocol.RIP.getDefaultAdministrativeCost(
                                vr.getConfiguration().getConfigurationFormat()),
                            RipProcess.DEFAULT_RIP_COST))
                .collect(ImmutableSet.toImmutableSet())));
  }

  /** Test that the static RIB correctly pulls static routes from the VRF */
  @Test
  public void testStaticRibInit() {
    VirtualRouter vr = makeIosVirtualRouter(null);
    vr.initRibs();
    SortedSet<StaticRoute> routeSet =
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("1.1.1.1/32"))
                .setNextHopIp(Ip.ZERO)
                .setNextHopInterface(null)
                .setAdministrativeCost(1)
                .setMetric(0L)
                .setTag(0)
                .build());
    vr._vrf.setStaticRoutes(routeSet);

    // Test
    vr.initStaticRibs();

    assertThat(vr._staticNextHopRib.getTypedRoutes(), equalTo(routeSet));
  }

  /** Test basic message queuing operations */
  @Test
  public void testQueueDelta() {
    Queue<RouteAdvertisement<AbstractRoute>> q = new ConcurrentLinkedQueue<>();

    // Test queueing empty deltas
    VirtualRouter.queueDelta(q, RibDelta.empty());
    assertThat(q, empty());

    RibDelta<AbstractRoute> delta = RibDelta.empty();
    VirtualRouter.queueDelta(q, delta);
    assertThat(q, empty());

    // Test queueing non-empty delta
    StaticRoute sr1 =
        StaticRoute.builder()
            .setNetwork(Prefix.create(Ip.parse("1.1.1.1"), Prefix.MAX_PREFIX_LENGTH))
            .setNextHopIp(Ip.ZERO)
            .setNextHopInterface(null)
            .setAdministrativeCost(1)
            .setMetric(0L)
            .setTag(1)
            .build();
    StaticRoute sr2 =
        StaticRoute.builder()
            .setNetwork(Prefix.create(Ip.parse("1.1.1.1"), Prefix.MAX_PREFIX_LENGTH))
            .setNextHopIp(Ip.ZERO)
            .setNextHopInterface(null)
            .setAdministrativeCost(100)
            .setMetric(0L)
            .setTag(1)
            .build();
    RibDelta.Builder<AbstractRoute> builder = RibDelta.<AbstractRoute>builder().add(sr1);

    // Add one route
    VirtualRouter.queueDelta(q, builder.build());
    assertThat(q, hasSize(1));

    // Repeats are allowed; So existing route + 1 add + 1 remove = 3 total
    builder.remove(sr2, Reason.WITHDRAW);
    VirtualRouter.queueDelta(q, builder.build());
    assertThat(q, hasSize(3));
  }

  /** Test that removed routes are queued before added routes */
  @Test
  public void testQueueDeltaOrder() {
    Queue<RouteAdvertisement<AbstractRoute>> q = new ConcurrentLinkedQueue<>();
    StaticRoute sr1 =
        StaticRoute.builder()
            .setNetwork(Prefix.create(Ip.parse("1.1.1.1"), Prefix.MAX_PREFIX_LENGTH))
            .setNextHopIp(Ip.ZERO)
            .setNextHopInterface(null)
            .setAdministrativeCost(1)
            .setMetric(0L)
            .setTag(1)
            .build();
    StaticRoute sr2 =
        StaticRoute.builder()
            .setNetwork(Prefix.create(Ip.parse("1.1.1.1"), Prefix.MAX_PREFIX_LENGTH))
            .setNextHopIp(Ip.ZERO)
            .setNextHopInterface(null)
            .setAdministrativeCost(100)
            .setMetric(0L)
            .setTag(1)
            .build();
    RibDelta.Builder<AbstractRoute> builder = RibDelta.builder();

    // Test queueing empty deltas
    builder.add(sr1).remove(sr2, Reason.WITHDRAW);
    VirtualRouter.queueDelta(q, builder.build());

    // Check queuing order.
    // Note: contains complains about generics, do manual remove/check
    assertThat(q.remove(), equalTo(new RouteAdvertisement<>(sr1)));
    assertThat(
        q.remove(),
        equalTo(RouteAdvertisement.builder().setRoute(sr2).setReason(Reason.WITHDRAW).build()));
    assertThat(q, empty());
  }

  @Test
  public void testInitQueuesAndDeltaBuilders() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.setHostname("r1").build();
    Configuration c2 = cb.setHostname("r2").build();

    Vrf vrf1 = nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(c1).build();
    Vrf vrf2 = nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(c2).build();
    // Set bgp processes and neighbors
    BgpProcess proc1 = nf.bgpProcessBuilder().setVrf(vrf1).setRouterId(Ip.parse("1.1.1.1")).build();
    BgpProcess proc2 = nf.bgpProcessBuilder().setVrf(vrf2).setRouterId(Ip.parse("1.1.1.2")).build();
    nf.bgpNeighborBuilder()
        .setPeerAddress(Ip.parse("1.1.1.2"))
        .setLocalIp(Ip.parse("1.1.1.1"))
        .setBgpProcess(proc1)
        .setRemoteAs(2L)
        .setLocalAs(1L)
        .build();
    nf.bgpNeighborBuilder()
        .setPeerAddress(Ip.parse("1.1.1.1"))
        .setLocalIp(Ip.parse("1.1.1.2"))
        .setBgpProcess(proc2)
        .setRemoteAs(1L)
        .setLocalAs(2L)
        .build();

    Interface.Builder ib = nf.interfaceBuilder().setActive(false);
    ib.setAddress(new InterfaceAddress("1.1.1.1/30")).setOwner(c1).build();
    ib.setAddress(new InterfaceAddress("1.1.1.2/30")).setOwner(c2).build();

    Topology topology = synthesizeL3Topology(ImmutableMap.of("r1", c1, "r2", c2));

    Map<String, Configuration> configs = ImmutableMap.of("r1", c1, "r2", c2);
    BgpTopology bgpTopology = BgpTopology.EMPTY;
    IsisTopology isisTopology = initIsisTopology(configs, topology);

    Map<String, Node> nodes =
        ImmutableMap.of(c1.getHostname(), new Node(c1), c2.getHostname(), new Node(c2));

    Map<String, VirtualRouter> vrs =
        nodes.values().stream()
            .map(n -> n.getVirtualRouters().get(DEFAULT_VRF_NAME))
            .collect(
                ImmutableMap.toImmutableMap(
                    vr -> vr.getConfiguration().getHostname(), Function.identity()));
    IsisTopology initialIsisTopology = initIsisTopology(configs, topology);

    EigrpTopology eigrpTopology = initEigrpTopology(configs, topology);
    vrs.values()
        .forEach(
            vr ->
                vr.initForEgpComputation(
                    TopologyContext.builder()
                        .setBgpTopology(bgpTopology)
                        .setEigrpTopology(eigrpTopology)
                        .setIsisTopology(initialIsisTopology)
                        .build()));

    // Assert that queues are empty as there are no OSPF, BGP, EIGRP, nor IS-IS processes if the
    // topologies are empty
    vrs.values()
        .forEach(
            vr -> {
              assertThat(vr._bgpRoutingProcess._bgpv4IncomingRoutes, anEmptyMap());
              vr._virtualEigrpProcesses
                  .values()
                  .forEach(process -> assertThat(process._incomingRoutes, anEmptyMap()));
              assertThat(vr._isisIncomingRoutes, anEmptyMap());
            });

    // Re-run with non-empty topology
    BgpTopology bgpTopology2 =
        initBgpTopology(configs, computeIpNodeOwners(configs, false), false, null);
    for (Node n : nodes.values()) {
      n.getVirtualRouters()
          .get(DEFAULT_VRF_NAME)
          .initForEgpComputation(
              TopologyContext.builder()
                  .setBgpTopology(bgpTopology2)
                  .setEigrpTopology(eigrpTopology)
                  .setIsisTopology(isisTopology)
                  .build());
    }
    // Assert that queues are initialized
    vrs.values()
        .forEach(
            vr -> {
              assertThat(vr._bgpRoutingProcess._bgpv4IncomingRoutes, is(notNullValue()));
              assertThat(vr._bgpRoutingProcess._bgpv4IncomingRoutes.values(), hasSize(1));
            });
  }

  @Test
  public void testInitIsisQueuesAndDeltaBuilders() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder().setName(DEFAULT_VRF_NAME);
    IsisLevelSettings levelSettings = IsisLevelSettings.builder().build();
    IsisProcess.Builder isb =
        IsisProcess.builder().setLevel1(levelSettings).setLevel2(levelSettings);
    IsisInterfaceLevelSettings isisInterfaceLevelSettings =
        IsisInterfaceLevelSettings.builder().setMode(IsisInterfaceMode.ACTIVE).build();
    IsisInterfaceSettings isisInterfaceSettings =
        IsisInterfaceSettings.builder()
            .setPointToPoint(true)
            .setLevel1(isisInterfaceLevelSettings)
            .setLevel2(isisInterfaceLevelSettings)
            .build();
    Interface.Builder ib = nf.interfaceBuilder();

    Configuration c1 = cb.build();
    Vrf v1 = vb.setOwner(c1).build();
    // Area: 0001, SystemID: 0100.0000.0000
    isb.setVrf(v1).setNetAddress(new IsoAddress("49.0001.0100.0000.0000.00")).build();
    Interface i1 =
        ib.setOwner(c1).setVrf(v1).setAddress(new InterfaceAddress("10.0.0.0/31")).build();

    Configuration c2 = cb.build();
    Vrf v2 = vb.setOwner(c2).build();
    // Area: 0001, SystemID: 0100.0000.0001
    isb.setVrf(v2).setNetAddress(new IsoAddress("49.0001.0100.0000.0001.00")).build();
    Interface i2 =
        ib.setOwner(c2).setVrf(v2).setAddress(new InterfaceAddress("10.0.0.1/31")).build();

    Map<String, Configuration> configs =
        ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    Topology topology = synthesizeL3Topology(configs);
    Map<String, Node> nodes =
        ImmutableMap.of(c1.getHostname(), new Node(c1), c2.getHostname(), new Node(c2));
    Map<String, VirtualRouter> vrs =
        nodes.values().stream()
            .map(n -> n.getVirtualRouters().get(DEFAULT_VRF_NAME))
            .collect(
                ImmutableMap.toImmutableMap(
                    vr -> vr.getConfiguration().getHostname(), Function.identity()));
    IsisTopology initialIsisTopology = initIsisTopology(configs, topology);
    EigrpTopology eigrpTopology = initEigrpTopology(configs, topology);
    vrs.values()
        .forEach(
            vr ->
                vr.initQueuesAndDeltaBuilders(
                    TopologyContext.builder()
                        .setEigrpTopology(eigrpTopology)
                        .setIsisTopology(initialIsisTopology)
                        .build()));

    // Assert that queues are empty as there are no OSPF, BGP, nor IS-IS processes
    vrs.values()
        .forEach(
            vr -> {
              assertThat(vr._isisIncomingRoutes, anEmptyMap());
            });

    // Set IS-IS on interfaces
    i1.setIsis(isisInterfaceSettings);
    i2.setIsis(isisInterfaceSettings);
    IsisTopology updatedIsisTopology = initIsisTopology(configs, topology);

    // Re-run
    vrs.values()
        .forEach(
            vr ->
                vr.initQueuesAndDeltaBuilders(
                    TopologyContext.builder()
                        .setEigrpTopology(eigrpTopology)
                        .setIsisTopology(updatedIsisTopology)
                        .build()));

    // Assert that queues are initialized
    assertThat(
        vrs.get(c1.getHostname())._isisIncomingRoutes.keySet(),
        equalTo(
            ImmutableSet.of(
                new IsisEdge(
                    IsisLevel.LEVEL_1_2,
                    new IsisNode(c2.getHostname(), i2.getName()),
                    new IsisNode(c1.getHostname(), i1.getName())))));
    assertThat(
        vrs.get(c2.getHostname())._isisIncomingRoutes.keySet(),
        equalTo(
            ImmutableSet.of(
                new IsisEdge(
                    IsisLevel.LEVEL_1_2,
                    new IsisNode(c1.getHostname(), i1.getName()),
                    new IsisNode(c2.getHostname(), i2.getName())))));
  }

  @Test
  public void testCrossVrfRouteLeaking() {
    String vrfWithRoutesName = "VRF1";
    String emptyVrfName = "VRF2";
    String importPolicyName = "IMPORT-POLICY";

    /*
    Create configuration containing 2 VRFs:
      - vrfWithRoutes has routes of every type, to ensure all route types get leaked correctly
      - emptyVrf has no routes and is set up to import all routes from vrfWithRoutes
    Tests:
      - ensure initCrossVrfImports() correctly leaks all routes from vrfWithRoutes' main RIB
      - ensure queueCrossVrfImports() correctly leaks all routes from vrfWithRoutes' main RIB delta
    */
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();

    // Create cross-VRF import policy to accept routes from vrfWithRoutes (and reject all others)
    RoutingPolicy.builder()
        .setOwner(c)
        .setName(importPolicyName)
        .setStatements(
            ImmutableList.of(
                new If(
                    new MatchSourceVrf(vrfWithRoutesName),
                    ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                    ImmutableList.of(Statements.ExitReject.toStaticStatement()))))
        .build();

    // Create VRFs in configuration and set up cross-VRF import VRF and policy for empty one
    nf.vrfBuilder().setOwner(c).setName(vrfWithRoutesName).build();
    Vrf emptyVrf = nf.vrfBuilder().setOwner(c).setName(emptyVrfName).build();
    emptyVrf.setCrossVrfImportVrfs(ImmutableList.of(vrfWithRoutesName));
    emptyVrf.setCrossVrfImportPolicy(importPolicyName);

    // Create a Node based on the configuration and get its VirtualRouters
    Node n = new Node(c);
    VirtualRouter vrWithRoutes = n.getVirtualRouters().get(vrfWithRoutesName);
    VirtualRouter emptyVr = n.getVirtualRouters().get(emptyVrfName);

    // Create routes of every type and inject them into vrWithRoutes' main RIB and main RIB delta
    Set<AnnotatedRoute<AbstractRoute>> annotatedRoutes =
        makeOneRouteOfEveryType().stream()
            .map(r -> new AnnotatedRoute<>(r, vrfWithRoutesName))
            .collect(ImmutableSet.toImmutableSet());
    for (AnnotatedRoute<AbstractRoute> r : annotatedRoutes) {
      vrWithRoutes._mainRibRouteDeltaBuilder.from(vrWithRoutes._mainRib.mergeRouteGetDelta(r));
    }

    // Run initial leaking (i.e. what would happen at beginning of
    // computeNonMonotonicPortionOfDataPlane()); all routes should leak from vrWithRoutes' main RIB
    emptyVr.initCrossVrfQueues();
    emptyVr.initCrossVrfImports();
    emptyVr.processCrossVrfRoutes();
    assertThat(emptyVr.getMainRib().getTypedRoutes(), equalTo(annotatedRoutes));

    // Clear emptyVr's RIB and queues and run intermediate leaking (i.e. what would happen in one
    // computeDependentRoutesIteration()); all routes should leak from vrWithRoutes' main RIB delta
    emptyVr._mainRib.clear();
    emptyVr.initCrossVrfQueues();
    emptyVr.queueCrossVrfImports();
    emptyVr.processCrossVrfRoutes();
    assertThat(emptyVr.getMainRib().getTypedRoutes(), equalTo(annotatedRoutes));
  }
}

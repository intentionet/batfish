package org.batfish.dataplane.ibdp;

import static org.batfish.common.topology.TopologyUtil.computeIpNodeOwners;
import static org.batfish.common.util.CommonUtil.synthesizeTopology;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.bgp.BgpTopologyUtils.initBgpTopology;
import static org.batfish.datamodel.eigrp.EigrpTopology.initEigrpTopology;
import static org.batfish.datamodel.isis.IsisTopology.initIsisTopology;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterableOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.Network;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfInternalRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.RipRoute;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.eigrp.EigrpEdge;
import org.batfish.datamodel.eigrp.EigrpInterface;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.isis.IsisNode;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RibDelta.Builder;
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

  private static VirtualRouter makeIosVirtualRouter(String hostname) {
    Node n = TestUtils.makeIosRouter(hostname);
    return n.getVirtualRouters().get(DEFAULT_VRF_NAME);
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
    vr.getMainRib().removeRoute(baseRoute);
    vr.activateStaticRoutes();

    // Assert dependent route is not there
    assertThat(vr.getMainRib().getRoutes(), not(containsInAnyOrder(dependentRoute)));
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
        vr.getConnectedRib().getRoutes(),
        containsInAnyOrder(
            exampleInterfaceAddresses
                .entrySet()
                .stream()
                .map(e -> new ConnectedRoute(e.getValue().getPrefix(), e.getKey()))
                .collect(Collectors.toList())
                .toArray(new ConnectedRoute[] {})));
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
        vr._localRib.getRoutes(),
        containsInAnyOrder(
            exampleInterfaceAddresses
                .entrySet()
                .stream()
                .filter(e -> e.getValue().getPrefix().getPrefixLength() < Prefix.MAX_PREFIX_LENGTH)
                .map(e -> new LocalRoute(e.getValue(), e.getKey()))
                .collect(Collectors.toList())
                .toArray(new LocalRoute[] {})));
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
        vr._staticInterfaceRib.getRoutes(),
        containsInAnyOrder(routes.get(0), routes.get(2), routes.get(3)));
    assertThat(vr._staticNextHopRib.getRoutes(), containsInAnyOrder(routes.get(1)));
  }

  @Test
  public void testInitRibsEmpty() {
    VirtualRouter vr = makeIosVirtualRouter(null);

    // We expect the router to have the following RIBs and all of them are empty
    vr.initRibs();

    // Simple RIBs
    assertThat(vr.getConnectedRib().getRoutes(), is(emptyIterableOf(ConnectedRoute.class)));
    assertThat(vr._staticNextHopRib.getRoutes(), is(emptyIterableOf(StaticRoute.class)));
    assertThat(vr._staticInterfaceRib.getRoutes(), is(emptyIterableOf(StaticRoute.class)));
    assertThat(vr._independentRib.getRoutes(), is(emptyIterableOf(AbstractRoute.class)));

    // RIP RIBs
    assertThat(vr._ripInternalRib.getRoutes(), is(emptyIterableOf(RipInternalRoute.class)));
    assertThat(vr._ripInternalStagingRib.getRoutes(), is(emptyIterableOf(RipInternalRoute.class)));
    assertThat(vr._ripRib.getRoutes(), is(emptyIterableOf(RipRoute.class)));

    // OSPF RIBs
    assertThat(vr._ospfRib.getRoutes(), is(emptyIterableOf(OspfRoute.class)));
    assertThat(
        vr._ospfExternalType1Rib.getRoutes(), is(emptyIterableOf(OspfExternalType1Route.class)));
    assertThat(
        vr._ospfExternalType1StagingRib.getRoutes(),
        is(emptyIterableOf(OspfExternalType1Route.class)));
    assertThat(
        vr._ospfExternalType2Rib.getRoutes(), is(emptyIterableOf(OspfExternalType2Route.class)));
    assertThat(
        vr._ospfExternalType2StagingRib.getRoutes(),
        is(emptyIterableOf(OspfExternalType2Route.class)));
    assertThat(vr._ospfInterAreaRib.getRoutes(), is(emptyIterableOf(OspfInterAreaRoute.class)));
    assertThat(
        vr._ospfInterAreaStagingRib.getRoutes(), is(emptyIterableOf(OspfInterAreaRoute.class)));
    assertThat(vr._ospfIntraAreaRib.getRoutes(), is(emptyIterableOf(OspfIntraAreaRoute.class)));
    assertThat(
        vr._ospfIntraAreaStagingRib.getRoutes(), is(emptyIterableOf(OspfIntraAreaRoute.class)));
    assertThat(vr._ospfRib.getRoutes(), is(emptyIterableOf(OspfRoute.class)));

    // BGP ribs
    // Ibgp
    assertThat(vr._ibgpRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ibgpStagingRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    // Ebgp
    assertThat(vr._ebgpRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ebgpStagingRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    // Combined bgp
    assertThat(vr._bgpRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));

    // Main RIB
    assertThat(vr._mainRib.getRoutes(), is(emptyIterableOf(AbstractRoute.class)));
  }

  /** Ensure no route propagation when the interfaces are disabled or passive */
  @Test
  public void testOSPFPassiveInterfaceRejection() {
    // Setup
    String testRouterName = "R1";
    String exportingRouterName = "R2";
    String exportingRouterInterfaceName = "Ethernet1";
    Map<String, Node> nodes = makeIosRouters(testRouterName, exportingRouterName);
    Map<String, VirtualRouter> routers =
        nodes
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey, e -> e.getValue().getVirtualRouters().get(DEFAULT_VRF_NAME)));
    VirtualRouter testRouter = routers.get(testRouterName);
    VirtualRouter exportingRouter = routers.get(exportingRouterName);
    testRouter.initRibs();
    exportingRouter.initRibs();
    addInterfaces(testRouter.getConfiguration(), exampleInterfaceAddresses);
    addInterfaces(
        exportingRouter.getConfiguration(),
        ImmutableMap.of(exportingRouterInterfaceName, new InterfaceAddress("10.4.0.0/16")));
    int adminCost =
        RoutingProtocol.OSPF.getDefaultAdministrativeCost(
            testRouter.getConfiguration().getConfigurationFormat());

    Prefix prefix = Prefix.parse("7.7.7.0/24");
    OspfIntraAreaRoute route =
        (OspfIntraAreaRoute)
            OspfInternalRoute.builder()
                .setProtocol(RoutingProtocol.OSPF)
                .setNetwork(prefix)
                .setNextHopIp(Ip.parse("7.7.1.1"))
                .setAdmin(adminCost)
                .setMetric(20)
                .setArea(1L)
                .build();
    exportingRouter._ospfIntraAreaRib.mergeRoute(route);

    // Set interaces on router 1 to be OSPF passive
    testRouter
        .getConfiguration()
        .getAllInterfaces()
        .forEach((name, iface) -> iface.setActive(false));

    // Test 1
    testRouter.propagateOspfInternalRoutesFromNeighbor(
        testRouter._vrf.getOspfProcess(),
        nodes.get("R2"),
        testRouter.getConfiguration().getAllInterfaces().firstEntry().getValue(),
        exportingRouter.getConfiguration().getAllInterfaces().get(exportingRouterInterfaceName),
        adminCost);

    assertThat(
        testRouter._ospfInterAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfInterAreaRoute.class)));
    assertThat(
        testRouter._ospfIntraAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfIntraAreaRoute.class)));

    // Flip interfaces on router 2 to be passive now
    testRouter
        .getConfiguration()
        .getAllInterfaces()
        .forEach((name, iface) -> iface.setActive(true));
    exportingRouter
        .getConfiguration()
        .getAllInterfaces()
        .forEach((name, iface) -> iface.setActive(false));

    // Test 2
    testRouter.propagateOspfInternalRoutesFromNeighbor(
        testRouter._vrf.getOspfProcess(),
        nodes.get("R2"),
        testRouter.getConfiguration().getAllInterfaces().firstEntry().getValue(),
        exportingRouter.getConfiguration().getAllInterfaces().get(exportingRouterInterfaceName),
        adminCost);

    assertThat(
        testRouter._ospfInterAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfInterAreaRoute.class)));
    assertThat(
        testRouter._ospfIntraAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfIntraAreaRoute.class)));
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
    assertThat(vr._ripInternalRib.getRoutes(), is(emptyIterableOf(RipInternalRoute.class)));

    // Complete setup by adding a process
    RipProcess ripProcess = new RipProcess();
    ripProcess.setInterfaces(vr._vrf.getInterfaceNames());
    vr._vrf.setRipProcess(ripProcess);

    vr.initBaseRipRoutes();

    assertThat(
        vr._ripInternalRib.getRoutes(),
        containsInAnyOrder(
            exampleInterfaceAddresses
                .values()
                .stream()
                .map(
                    address ->
                        new RipInternalRoute(
                            address.getPrefix(),
                            null,
                            RoutingProtocol.RIP.getDefaultAdministrativeCost(
                                vr.getConfiguration().getConfigurationFormat()),
                            RipProcess.DEFAULT_RIP_COST))
                .collect(Collectors.toList())
                .toArray(new RipInternalRoute[] {})));
    vr._ripInternalRib.getRoutes();
  }

  /** Test that staging of a single OSPF Inter-Area route works as expected */
  @Test
  public void testStageOSPFInterAreaRoute() {
    VirtualRouter vr = makeIosVirtualRouter(null);
    vr.initRibs();

    int admin = 50;
    int metric = 100;
    long area = 1L;
    Prefix prefix = Prefix.parse("7.7.7.0/24");
    OspfInterAreaRoute iaroute =
        (OspfInterAreaRoute)
            OspfInternalRoute.builder()
                .setProtocol(RoutingProtocol.OSPF_IA)
                .setNetwork(prefix)
                .setNextHopIp(Ip.parse("7.7.1.1"))
                .setAdmin(admin)
                .setMetric(metric)
                .setArea(area)
                .build();

    // Test
    Ip newNextHop = Ip.parse("10.2.1.1");
    vr.stageOspfInterAreaRoute(iaroute, null, newNextHop, 10, admin, area);

    // Check what's in the RIB is correct.
    // Note the new nextHopIP and the increased metric on the new route.
    OspfInterAreaRoute expected =
        (OspfInterAreaRoute)
            OspfInternalRoute.builder()
                .setProtocol(RoutingProtocol.OSPF_IA)
                .setNetwork(prefix)
                .setNextHopIp(newNextHop)
                .setAdmin(admin)
                .setMetric(metric + 10)
                .setArea(area)
                .build();
    assertThat(vr._ospfInterAreaStagingRib.getRoutes(), contains(expected));
    assertThat(vr._ospfInterAreaStagingRib.getRoutes(), not(contains(iaroute)));
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

    assertThat(vr._staticNextHopRib.getRoutes(), equalTo(routeSet));
  }

  /** Test basic message queuing operations */
  @Test
  public void testQueueDelta() {
    Queue<RouteAdvertisement<AbstractRoute>> q = new ConcurrentLinkedQueue<>();

    // Test queueing empty deltas
    VirtualRouter.queueDelta(q, null);
    assertThat(q, empty());

    RibDelta<AbstractRoute> delta = new RibDelta.Builder<>(null).build();
    VirtualRouter.queueDelta(q, delta);
    assertThat(q, empty());

    // Test queueing non-empty delta
    StaticRoute sr1 =
        StaticRoute.builder()
            .setNetwork(Prefix.create(Ip.parse("1.1.1.1"), 32))
            .setNextHopIp(Ip.ZERO)
            .setNextHopInterface(null)
            .setAdministrativeCost(1)
            .setMetric(0L)
            .setTag(1)
            .build();
    StaticRoute sr2 =
        StaticRoute.builder()
            .setNetwork(Prefix.create(Ip.parse("1.1.1.1"), 32))
            .setNextHopIp(Ip.ZERO)
            .setNextHopInterface(null)
            .setAdministrativeCost(100)
            .setMetric(0L)
            .setTag(1)
            .build();
    RibDelta.Builder<AbstractRoute> builder = new Builder<>(null).add(sr1);

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
            .setNetwork(Prefix.create(Ip.parse("1.1.1.1"), 32))
            .setNextHopIp(Ip.ZERO)
            .setNextHopInterface(null)
            .setAdministrativeCost(1)
            .setMetric(0L)
            .setTag(1)
            .build();
    StaticRoute sr2 =
        StaticRoute.builder()
            .setNetwork(Prefix.create(Ip.parse("1.1.1.1"), 32))
            .setNextHopIp(Ip.ZERO)
            .setNextHopInterface(null)
            .setAdministrativeCost(100)
            .setMetric(0L)
            .setTag(1)
            .build();
    RibDelta.Builder<AbstractRoute> builder = new Builder<>(null);

    // Test queueing empty deltas
    builder.add(sr1).remove(sr2, Reason.WITHDRAW);
    VirtualRouter.queueDelta(q, builder.build());

    // Check queuing order.
    // Note: contains compains about generics, do manual remove/check
    assertThat(q.remove(), equalTo(new RouteAdvertisement<>(sr1)));
    assertThat(q.remove(), equalTo(new RouteAdvertisement<>(sr2, true, Reason.WITHDRAW)));
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

    Interface.Builder ib = nf.interfaceBuilder().setActive(false);
    ib.setAddress(new InterfaceAddress("1.1.1.1/30")).setOwner(c1).build();
    ib.setAddress(new InterfaceAddress("1.1.1.2/30")).setOwner(c2).build();

    Topology topology = synthesizeTopology(ImmutableMap.of("r1", c1, "r2", c2));

    Map<String, Configuration> configs = ImmutableMap.of("r1", c1, "r2", c2);
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(configs, computeIpNodeOwners(configs, false), false);
    Network<IsisNode, IsisEdge> isisTopology = initIsisTopology(configs, topology);

    Map<String, Node> nodes =
        ImmutableMap.of(c1.getHostname(), new Node(c1), c2.getHostname(), new Node(c2));

    Map<String, VirtualRouter> vrs =
        nodes
            .values()
            .stream()
            .map(n -> n.getVirtualRouters().get(DEFAULT_VRF_NAME))
            .collect(
                ImmutableMap.toImmutableMap(
                    vr -> vr.getConfiguration().getHostname(), Function.identity()));
    Network<IsisNode, IsisEdge> initialIsisTopology = initIsisTopology(configs, topology);

    Network<EigrpInterface, EigrpEdge> eigrpTopology = initEigrpTopology(configs, topology);
    vrs.values()
        .forEach(
            vr ->
                vr.initQueuesAndDeltaBuilders(
                    nodes, topology, bgpTopology, eigrpTopology, initialIsisTopology));

    // Assert that queues are empty as there are no OSPF, BGP, EIGRP, nor IS-IS processes
    vrs.values()
        .forEach(
            vr -> {
              assertThat(vr._bgpIncomingRoutes, anEmptyMap());
              vr._virtualEigrpProcesses
                  .values()
                  .forEach(process -> assertThat(process._incomingRoutes, anEmptyMap()));
              assertThat(vr._isisIncomingRoutes, anEmptyMap());
              assertThat(vr._ospfExternalIncomingRoutes, anEmptyMap());
            });

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

    // Re-run
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology2 =
        initBgpTopology(configs, computeIpNodeOwners(configs, false), false);
    for (Node n : nodes.values()) {
      n.getVirtualRouters()
          .get(DEFAULT_VRF_NAME)
          .initQueuesAndDeltaBuilders(nodes, topology, bgpTopology2, eigrpTopology, isisTopology);
    }
    // Assert that queues are initialized
    vrs.values()
        .forEach(
            vr -> {
              assertThat(vr._bgpIncomingRoutes, is(notNullValue()));
              assertThat(vr._bgpIncomingRoutes.values(), hasSize(1));
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
    Topology topology = synthesizeTopology(configs);
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(false).build());
    Map<String, Node> nodes =
        ImmutableMap.of(c1.getHostname(), new Node(c1), c2.getHostname(), new Node(c2));
    Map<String, VirtualRouter> vrs =
        nodes
            .values()
            .stream()
            .map(n -> n.getVirtualRouters().get(DEFAULT_VRF_NAME))
            .collect(
                ImmutableMap.toImmutableMap(
                    vr -> vr.getConfiguration().getHostname(), Function.identity()));
    Network<IsisNode, IsisEdge> initialIsisTopology = initIsisTopology(configs, topology);
    Network<EigrpInterface, EigrpEdge> eigrpTopology = initEigrpTopology(configs, topology);
    vrs.values()
        .forEach(
            vr ->
                vr.initQueuesAndDeltaBuilders(
                    nodes, topology, bgpTopology, eigrpTopology, initialIsisTopology));

    // Assert that queues are empty as there are no OSPF, BGP, nor IS-IS processes
    vrs.values()
        .forEach(
            vr -> {
              assertThat(vr._isisIncomingRoutes, anEmptyMap());
            });

    // Set IS-IS on interfaces
    i1.setIsis(isisInterfaceSettings);
    i2.setIsis(isisInterfaceSettings);
    Network<IsisNode, IsisEdge> updatedIsisTopology = initIsisTopology(configs, topology);

    // Re-run
    vrs.values()
        .forEach(
            vr ->
                vr.initQueuesAndDeltaBuilders(
                    nodes, topology, bgpTopology, eigrpTopology, updatedIsisTopology));

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

  /*
   * Setup:
   * - Node R1 with ISIS interface with active L1 and passive L2
   * - Node R2 with ISIS interface with passive L1 and active L2
   * Manually send route advertisements to both routers at both levels. Should see:
   * - R1 propagate advertisements at L1 but not L2
   * - R2 propagate advertisements at L2 but not L1
   */
  @Test
  public void testIsisRoutePropagationWithPassiveInterfaceLevels() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Configuration c2 = cb.build();

    Vrf.Builder vb = nf.vrfBuilder().setName(DEFAULT_VRF_NAME);
    Vrf v1 = vb.setOwner(c1).build();
    Vrf v2 = vb.setOwner(c2).build();

    IsisLevelSettings levelSettings = IsisLevelSettings.builder().build();
    IsisProcess.Builder isb =
        IsisProcess.builder().setLevel1(levelSettings).setLevel2(levelSettings);
    Interface.Builder ib = nf.interfaceBuilder();

    // Area: 0001, SystemID: 0100.0000.0000
    isb.setVrf(v1).setNetAddress(new IsoAddress("49.0001.0100.0000.0000.00")).build();
    Interface i1 =
        ib.setOwner(c1).setVrf(v1).setAddress(new InterfaceAddress("10.0.0.0/31")).build();

    // Area: 0001, SystemID: 0100.0000.0001
    isb.setVrf(v2).setNetAddress(new IsoAddress("49.0001.0100.0000.0001.00")).build();
    Interface i2 =
        ib.setOwner(c2).setVrf(v2).setAddress(new InterfaceAddress("10.0.0.1/31")).build();

    Map<String, Configuration> configs =
        ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    Topology topology = synthesizeTopology(configs);
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(false).build());
    Map<String, Node> nodes =
        ImmutableMap.of(c1.getHostname(), new Node(c1), c2.getHostname(), new Node(c2));
    Map<String, VirtualRouter> vrs =
        nodes
            .values()
            .stream()
            .map(n -> n.getVirtualRouters().get(DEFAULT_VRF_NAME))
            .collect(
                ImmutableMap.toImmutableMap(
                    vr -> vr.getConfiguration().getHostname(), Function.identity()));
    Network<IsisNode, IsisEdge> initialIsisTopology = initIsisTopology(configs, topology);
    Network<EigrpInterface, EigrpEdge> eigrpTopology = initEigrpTopology(configs, topology);
    vrs.values()
        .forEach(
            vr ->
                vr.initQueuesAndDeltaBuilders(
                    nodes, topology, bgpTopology, eigrpTopology, initialIsisTopology));

    // Set IS-IS on interfaces. Set cost to 0 for convenience - this way route advertisements in rib
    // delta should be equal to incoming route advertisements because the metrics won't increase.
    IsisInterfaceLevelSettings activeLevelSettings =
        IsisInterfaceLevelSettings.builder().setMode(IsisInterfaceMode.ACTIVE).setCost(0L).build();
    IsisInterfaceLevelSettings passiveLevelSettings =
        IsisInterfaceLevelSettings.builder().setMode(IsisInterfaceMode.PASSIVE).setCost(0L).build();
    i1.setIsis(
        IsisInterfaceSettings.builder()
            .setPointToPoint(true)
            .setLevel1(activeLevelSettings)
            .setLevel2(passiveLevelSettings)
            .build());
    i2.setIsis(
        IsisInterfaceSettings.builder()
            .setPointToPoint(true)
            .setLevel1(passiveLevelSettings)
            .setLevel2(activeLevelSettings)
            .build());
    Network<IsisNode, IsisEdge> updatedIsisTopology = initIsisTopology(configs, topology);

    // Re-run
    vrs.values()
        .forEach(
            vr ->
                vr.initQueuesAndDeltaBuilders(
                    nodes, topology, bgpTopology, eigrpTopology, updatedIsisTopology));

    // Build route advertisements.
    IsisRoute.Builder routeBuilder =
        new IsisRoute.Builder()
            .setAdmin(115)
            .setArea("1")
            .setSystemId("id")
            .setNetwork(Prefix.parse("1.1.1.1/24"))
            .setNextHopIp(Ip.parse("10.0.0.0"));

    RouteAdvertisement<IsisRoute> level1Advert1to2 =
        new RouteAdvertisement<>(
            routeBuilder.setLevel(IsisLevel.LEVEL_1).setProtocol(RoutingProtocol.ISIS_L1).build());
    RouteAdvertisement<IsisRoute> level2Advert1to2 =
        new RouteAdvertisement<>(
            routeBuilder.setLevel(IsisLevel.LEVEL_2).setProtocol(RoutingProtocol.ISIS_L2).build());

    routeBuilder.setNetwork(Prefix.parse("2.2.2.2/24")).setNextHopIp(Ip.parse("10.0.0.1"));

    RouteAdvertisement<IsisRoute> level1Advert2to1 =
        new RouteAdvertisement<>(
            routeBuilder.setLevel(IsisLevel.LEVEL_1).setProtocol(RoutingProtocol.ISIS_L1).build());
    RouteAdvertisement<IsisRoute> level2Advert2to1 =
        new RouteAdvertisement<>(
            routeBuilder.setLevel(IsisLevel.LEVEL_2).setProtocol(RoutingProtocol.ISIS_L2).build());

    vrs.get(c1.getHostname())._isisIncomingRoutes.values().forEach(q -> q.add(level1Advert2to1));
    vrs.get(c1.getHostname())._isisIncomingRoutes.values().forEach(q -> q.add(level2Advert2to1));
    vrs.get(c2.getHostname())._isisIncomingRoutes.values().forEach(q -> q.add(level1Advert1to2));
    vrs.get(c2.getHostname())._isisIncomingRoutes.values().forEach(q -> q.add(level2Advert1to2));

    NetworkConfigurations nc = NetworkConfigurations.of(configs);

    // Tests for R1
    Entry<RibDelta<IsisRoute>, RibDelta<IsisRoute>> ribDelta =
        vrs.get(c1.getHostname()).propagateIsisRoutes(nc);
    RibDelta<IsisRoute> L1Delta = ribDelta.getKey();
    RibDelta<IsisRoute> L2Delta = ribDelta.getValue();
    assertThat(L1Delta.getActions(), contains(level1Advert2to1));
    assertThat(L2Delta, nullValue());

    // Tests for R2
    ribDelta = vrs.get(c2.getHostname()).propagateIsisRoutes(nc);
    L1Delta = ribDelta.getKey();
    L2Delta = ribDelta.getValue();
    assertThat(L1Delta, nullValue());
    assertThat(L2Delta.getActions(), contains(level2Advert1to2));
  }
}

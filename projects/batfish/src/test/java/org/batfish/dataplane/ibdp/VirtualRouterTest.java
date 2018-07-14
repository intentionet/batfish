package org.batfish.dataplane.ibdp;

import static org.batfish.common.util.CommonUtil.computeIpNodeOwners;
import static org.batfish.common.util.CommonUtil.initBgpTopology;
import static org.batfish.common.util.CommonUtil.synthesizeTopology;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.isis.IsisTopology.initIsisTopology;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasDestinationIp;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasNetwork;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasOriginatorIp;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasSourceIp;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasType;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
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
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.isis.IsisNode;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.dataplane.rib.BgpBestPathRib;
import org.batfish.dataplane.rib.BgpMultipathRib;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RibDelta.Builder;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;
import org.batfish.main.BatfishTestUtils;
import org.junit.Before;
import org.junit.Test;

public class VirtualRouterTest {
  /** Make a CISCO IOS router with 3 interfaces named Eth1-Eth3, /16 prefixes on each interface */
  private static final Map<String, InterfaceAddress> exampleInterfaceAddresses =
      ImmutableMap.<String, InterfaceAddress>builder()
          .put("Ethernet1", new InterfaceAddress("10.1.0.0/16"))
          .put("Ethernet2", new InterfaceAddress("10.2.0.0/16"))
          .put("Ethernet3", new InterfaceAddress("10.3.0.0/16"))
          .build();

  private static final String NEIGHBOR_HOST_NAME = "neighbornode";
  private static final int TEST_ADMIN = 100;
  private static final Long TEST_AREA = 1L;
  private static final long TEST_AS1 = 1;
  private static final long TEST_AS2 = 2;
  private static final long TEST_AS3 = 3;
  private static final Ip TEST_DEST_IP = new Ip("2.2.2.2");
  private static final ConfigurationFormat FORMAT = ConfigurationFormat.CISCO_IOS;
  private static final int TEST_METRIC = 30;
  private static final Ip TEST_SRC_IP = new Ip("1.1.1.1");
  private static final Prefix TEST_NETWORK = Prefix.parse("4.4.4.4/32");
  private static final Ip TEST_NEXT_HOP_IP1 = new Ip("1.2.3.4");
  private static final Ip TEST_NEXT_HOP_IP2 = new Ip("2.3.4.5");
  private static final String TEST_VIRTUAL_ROUTER_NAME = "testvirtualrouter";

  private BgpActivePeerConfig.Builder _bgpNeighborBuilder;
  private BgpRoute.Builder _bgpRouteBuilder;
  private Statement _exitAcceptStatement = Statements.ExitAccept.toStaticStatement();
  private Statement _exitRejectStatement = Statements.ExitReject.toStaticStatement();
  private Map<Ip, Set<String>> _ipOwners;
  private RoutingPolicy.Builder _routingPolicyBuilder;
  private VirtualRouter _testVirtualRouter;

  private static void addInterfaces(
      Configuration c, Map<String, InterfaceAddress> interfaceAddresses) {
    NetworkFactory nf = new NetworkFactory();
    Interface.Builder ib =
        nf.interfaceBuilder().setActive(true).setOwner(c).setVrf(c.getDefaultVrf());
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
   * Creates an empty {@link VirtualRouter} along with creating owner {@link Configuration} and
   * {@link Vrf}
   *
   * @param nodeName Node name of the owner {@link Configuration}
   * @return new instance of {@link VirtualRouter}
   */
  private static VirtualRouter createEmptyVirtualRouter(NetworkFactory nf, String nodeName) {
    Configuration config = BatfishTestUtils.createTestConfiguration(nodeName, FORMAT, "interface1");
    Vrf.Builder vb = nf.vrfBuilder().setName(DEFAULT_VRF_NAME);
    Vrf vrf = vb.setOwner(config).build();
    config.getVrfs().put(TEST_VIRTUAL_ROUTER_NAME, vrf);
    VirtualRouter virtualRouter = new VirtualRouter(TEST_VIRTUAL_ROUTER_NAME, config);
    virtualRouter.initRibs();
    virtualRouter._sentBgpAdvertisements = new LinkedHashSet<>();
    return virtualRouter;
  }

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    _testVirtualRouter = createEmptyVirtualRouter(nf, TEST_VIRTUAL_ROUTER_NAME);
    BgpProcess bgpProcess =
        nf.bgpProcessBuilder().setVrf(_testVirtualRouter._vrf).setRouterId(TEST_SRC_IP).build();
    nf.configurationBuilder()
        .setConfigurationFormat(FORMAT)
        .setHostname(NEIGHBOR_HOST_NAME)
        .build();
    _bgpNeighborBuilder =
        nf.bgpNeighborBuilder()
            .setPeerAddress(TEST_DEST_IP)
            .setLocalIp(TEST_SRC_IP)
            .setLocalAs(TEST_AS1)
            .setBgpProcess(bgpProcess);
    _bgpRouteBuilder =
        new BgpRoute.Builder()
            .setNetwork(TEST_NETWORK)
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(TEST_SRC_IP);
    _ipOwners = ImmutableMap.of(TEST_SRC_IP, ImmutableSet.of(TEST_VIRTUAL_ROUTER_NAME));
    _routingPolicyBuilder =
        nf.routingPolicyBuilder().setOwner(_testVirtualRouter.getConfiguration());
  }

  @Test
  public void computeBgpAdvertisementsSentToOutsideNoBgp() {

    // checking that no bgp advertisements are sent if the vrf has no BGP process
    assertThat(_testVirtualRouter.computeBgpAdvertisementsToOutside(_ipOwners), equalTo(0));
  }

  @Test
  public void computeBgpAdvertisementsSentToOutsideIgp() {
    RoutingPolicy exportPolicy =
        _routingPolicyBuilder
            .setStatements(
                ImmutableList.of(
                    new SetOrigin(new LiteralOrigin(OriginType.INCOMPLETE, null)),
                    _exitAcceptStatement))
            .build();
    _bgpNeighborBuilder.setExportPolicy(exportPolicy.getName()).setRemoteAs(TEST_AS2).build();

    _testVirtualRouter._mainRib.mergeRoute(
        new OspfInternalRoute.Builder()
            .setNetwork(TEST_NETWORK)
            .setMetric(TEST_METRIC)
            .setArea(TEST_AREA)
            .setAdmin(TEST_ADMIN)
            .setProtocol(RoutingProtocol.OSPF)
            .build());

    // checking number of bgp advertisements
    assertThat(_testVirtualRouter.computeBgpAdvertisementsToOutside(_ipOwners), equalTo(1));

    BgpAdvertisement bgpAdvertisement = _testVirtualRouter._sentBgpAdvertisements.iterator().next();

    // checking the attributes of the bgp advertisement
    assertThat(bgpAdvertisement, hasDestinationIp(TEST_DEST_IP));
    assertThat(bgpAdvertisement, hasNetwork(TEST_NETWORK));
    assertThat(bgpAdvertisement, hasOriginatorIp(TEST_SRC_IP));
    assertThat(bgpAdvertisement, hasType(BgpAdvertisementType.EBGP_SENT));
    assertThat(bgpAdvertisement, hasSourceIp(TEST_SRC_IP));
  }

  @Test
  public void computeBgpAdvertisementsSTOIbgpAdvertiseExternal() {
    RoutingPolicy exportPolicy =
        _routingPolicyBuilder.setStatements(ImmutableList.of(_exitAcceptStatement)).build();
    _bgpNeighborBuilder
        .setRemoteAs(TEST_AS1)
        .setExportPolicy(exportPolicy.getName())
        .setAdvertiseExternal(true)
        .build();

    _testVirtualRouter._ebgpBestPathRib.mergeRoute(
        _bgpRouteBuilder
            .setNextHopIp(TEST_NEXT_HOP_IP1)
            .setReceivedFromIp(TEST_NEXT_HOP_IP1)
            .build());

    /* checking that the route in EBGP Best Path Rib got advertised */
    assertThat(_testVirtualRouter.computeBgpAdvertisementsToOutside(_ipOwners), equalTo(1));

    BgpAdvertisement bgpAdvertisement = _testVirtualRouter._sentBgpAdvertisements.iterator().next();

    // checking the attributes of the bgp advertisement
    assertThat(bgpAdvertisement, hasDestinationIp(TEST_DEST_IP));
    assertThat(bgpAdvertisement, hasNetwork(TEST_NETWORK));
    assertThat(bgpAdvertisement, hasOriginatorIp(TEST_SRC_IP));
    assertThat(bgpAdvertisement, hasType(BgpAdvertisementType.IBGP_SENT));
    assertThat(bgpAdvertisement, hasSourceIp(TEST_SRC_IP));
  }

  @Test
  public void computeBgpAdvertisementsSTOIbgpAdditionalPaths() {
    RoutingPolicy exportPolicy =
        _routingPolicyBuilder.setStatements(ImmutableList.of(_exitAcceptStatement)).build();

    _bgpNeighborBuilder
        .setRemoteAs(TEST_AS1)
        .setExportPolicy(exportPolicy.getName())
        .setAdditionalPathsSend(true)
        .setAdditionalPathsSelectAll(true)
        .build();

    _testVirtualRouter._bgpMultipathRib.mergeRoute(
        _bgpRouteBuilder
            .setReceivedFromIp(TEST_NEXT_HOP_IP1)
            .setNextHopIp(TEST_NEXT_HOP_IP1)
            .build());
    // adding second similar route in the Multipath rib with a different Next Hop IP
    _testVirtualRouter._bgpMultipathRib.mergeRoute(
        _bgpRouteBuilder
            .setReceivedFromIp(TEST_NEXT_HOP_IP2)
            .setNextHopIp(TEST_NEXT_HOP_IP2)
            .build());

    // checking that both the routes in BGP Multipath Rib got advertised
    assertThat(_testVirtualRouter.computeBgpAdvertisementsToOutside(_ipOwners), equalTo(2));

    // checking that both bgp advertisements have the same network and the supplied next hop IPs
    Set<Ip> nextHopIps = new HashSet<>();
    _testVirtualRouter._sentBgpAdvertisements.forEach(
        bgpAdvertisement -> {
          assertThat(bgpAdvertisement, hasNetwork(TEST_NETWORK));
          nextHopIps.add(bgpAdvertisement.getNextHopIp());
        });

    assertThat(
        "Next Hop IPs not valid in BGP advertisements",
        nextHopIps,
        containsInAnyOrder(TEST_NEXT_HOP_IP1, TEST_NEXT_HOP_IP2));
  }

  @Test
  public void computeBgpAdvertisementsSTOEbgpAdvertiseInactive() {
    RoutingPolicy exportPolicy =
        _routingPolicyBuilder
            .setStatements(
                ImmutableList.of(
                    new SetOrigin(new LiteralOrigin(OriginType.INCOMPLETE, null)),
                    _exitAcceptStatement))
            .build();

    _bgpNeighborBuilder
        .setRemoteAs(TEST_AS2)
        .setExportPolicy(exportPolicy.getName())
        .setAdvertiseInactive(true)
        .build();

    _testVirtualRouter._bgpBestPathRib.mergeRoute(
        _bgpRouteBuilder
            .setNextHopIp(TEST_NEXT_HOP_IP1)
            .setReceivedFromIp(TEST_NEXT_HOP_IP1)
            .setAsPath(AsPath.ofSingletonAsSets(TEST_AS3).getAsSets())
            .build());

    // adding a connected route in main rib
    _testVirtualRouter._mainRib.mergeRoute(
        new ConnectedRoute(TEST_NETWORK, Route.UNSET_NEXT_HOP_INTERFACE));

    // checking that the inactive BGP route got advertised along with the active OSPF route
    assertThat(_testVirtualRouter.computeBgpAdvertisementsToOutside(_ipOwners), equalTo(2));

    // checking that both bgp advertisements have the same network and correct AS Paths
    Set<AsPath> asPaths = new HashSet<>();
    _testVirtualRouter._sentBgpAdvertisements.forEach(
        bgpAdvertisement -> {
          assertThat(bgpAdvertisement, hasNetwork(TEST_NETWORK));
          asPaths.add(bgpAdvertisement.getAsPath());
        });

    // next Hop IP for the active OSPF route  will be the neighbor's local IP
    assertThat(
        "AS Paths not valid in BGP advertisements",
        asPaths,
        equalTo(
            ImmutableSet.of(
                AsPath.ofSingletonAsSets(TEST_AS1, TEST_AS3), AsPath.ofSingletonAsSets(TEST_AS1))));
  }

  @Test
  public void computeBgpAdvertisementsSTOIbgpNeighborReject() {
    RoutingPolicy exportPolicy =
        _routingPolicyBuilder.setStatements(ImmutableList.of(_exitRejectStatement)).build();

    _bgpNeighborBuilder
        .setRemoteAs(TEST_AS1)
        .setExportPolicy(exportPolicy.getName())
        .setAdvertiseExternal(true)
        .setAdditionalPathsSend(true)
        .setAdditionalPathsSelectAll(true)
        .build();

    _testVirtualRouter._ebgpBestPathRib.mergeRoute(
        _bgpRouteBuilder
            .setNextHopIp(TEST_NEXT_HOP_IP1)
            .setReceivedFromIp(TEST_NEXT_HOP_IP1)
            .build());
    _testVirtualRouter._bgpMultipathRib.mergeRoute(_bgpRouteBuilder.build());

    // number of BGP advertisements should be zero given the reject all export policy
    assertThat(_testVirtualRouter.computeBgpAdvertisementsToOutside(_ipOwners), equalTo(0));
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
            .build();
    StaticRoute dependentRoute =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("2.2.2.2/32"))
            .setNextHopIp(new Ip("1.1.1.1"))
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
            new StaticRoute(Prefix.parse("1.1.1.1/32"), null, "Ethernet1", 1, 0L, 1),
            new StaticRoute(Prefix.parse("2.2.2.2/32"), new Ip("9.9.9.8"), null, 1, 0L, 1),
            new StaticRoute(Prefix.parse("3.3.3.3/32"), new Ip("9.9.9.9"), "Ethernet1", 1, 0L, 1),
            new StaticRoute(
                Prefix.parse("4.4.4.4/32"), null, Interface.NULL_INTERFACE_NAME, 1, 0L, 1),

            // These do not get activated due to missing/incorrect interface names
            new StaticRoute(Prefix.parse("5.5.5.5/32"), null, "Eth1", 1, 0L, 1),
            new StaticRoute(Prefix.parse("6.6.6.6/32"), null, null, 1, 0L, 1));
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
    assertThat(vr._ibgpBestPathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ibgpMultipathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ibgpStagingRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    // Ebgp
    assertThat(vr._ebgpBestPathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ebgpMultipathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ebgpStagingRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    // Combined bgp
    assertThat(vr._bgpBestPathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._bgpMultipathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));

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
    OspfIntraAreaRoute route = new OspfIntraAreaRoute(prefix, new Ip("7.7.1.1"), adminCost, 20, 1);
    exportingRouter._ospfIntraAreaRib.mergeRoute(route);

    // Set interaces on router 1 to be OSPF passive
    testRouter.getConfiguration().getInterfaces().forEach((name, iface) -> iface.setActive(false));

    // Test 1
    testRouter.propagateOspfInternalRoutesFromNeighbor(
        testRouter._vrf.getOspfProcess(),
        nodes.get("R2"),
        testRouter.getConfiguration().getInterfaces().firstEntry().getValue(),
        exportingRouter.getConfiguration().getInterfaces().get(exportingRouterInterfaceName),
        adminCost);

    assertThat(
        testRouter._ospfInterAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfInterAreaRoute.class)));
    assertThat(
        testRouter._ospfIntraAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfIntraAreaRoute.class)));

    // Flip interfaces on router 2 to be passive now
    testRouter.getConfiguration().getInterfaces().forEach((name, iface) -> iface.setActive(true));
    exportingRouter
        .getConfiguration()
        .getInterfaces()
        .forEach((name, iface) -> iface.setActive(false));

    // Test 2
    testRouter.propagateOspfInternalRoutesFromNeighbor(
        testRouter._vrf.getOspfProcess(),
        nodes.get("R2"),
        testRouter.getConfiguration().getInterfaces().firstEntry().getValue(),
        exportingRouter.getConfiguration().getInterfaces().get(exportingRouterInterfaceName),
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
        new OspfInterAreaRoute(prefix, new Ip("7.7.1.1"), admin, metric, area);

    // Test
    Ip newNextHop = new Ip("10.2.1.1");
    vr.stageOspfInterAreaRoute(iaroute, null, newNextHop, 10, admin, area);

    // Check what's in the RIB is correct.
    // Note the new nextHopIP and the increased metric on the new route.
    assertThat(
        vr._ospfInterAreaStagingRib.getRoutes(),
        contains(new OspfInterAreaRoute(prefix, newNextHop, admin, metric + 10, area)));
    assertThat(vr._ospfInterAreaStagingRib.getRoutes(), not(contains(iaroute)));
  }

  /** Test that the static RIB correctly pulls static routes from the VRF */
  @Test
  public void testStaticRibInit() {
    VirtualRouter vr = makeIosVirtualRouter(null);
    vr.initRibs();
    SortedSet<StaticRoute> routeSet =
        ImmutableSortedSet.of(new StaticRoute(Prefix.parse("1.1.1.1/32"), Ip.ZERO, null, 1, 0L, 0));
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
    StaticRoute sr1 = new StaticRoute(new Prefix(new Ip("1.1.1.1"), 32), Ip.ZERO, null, 1, 0L, 1);
    StaticRoute sr2 = new StaticRoute(new Prefix(new Ip("1.1.1.1"), 32), Ip.ZERO, null, 100, 0L, 1);
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
    StaticRoute sr1 = new StaticRoute(new Prefix(new Ip("1.1.1.1"), 32), Ip.ZERO, null, 1, 0L, 1);
    StaticRoute sr2 = new StaticRoute(new Prefix(new Ip("1.1.1.1"), 32), Ip.ZERO, null, 100, 0L, 1);
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

    Interface.Builder ib = nf.interfaceBuilder();
    ib.setAddress(new InterfaceAddress("1.1.1.1/30")).setOwner(c1).build();
    ib.setAddress(new InterfaceAddress("1.1.1.2/30")).setOwner(c2).build();

    Topology topology = synthesizeTopology(ImmutableMap.of("r1", c1, "r2", c2));

    Map<String, Configuration> configs = ImmutableMap.of("r1", c1, "r2", c2);
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(configs, computeIpNodeOwners(configs, false), false);
    Network<IsisNode, IsisEdge> isisTopology = initIsisTopology(configs, topology);

    Map<String, Node> nodes =
        ImmutableMap.of(c1.getName(), new Node(c1), c2.getName(), new Node(c2));

    Map<String, VirtualRouter> vrs =
        nodes
            .values()
            .stream()
            .map(n -> n.getVirtualRouters().get(DEFAULT_VRF_NAME))
            .collect(
                ImmutableMap.toImmutableMap(
                    vr -> vr.getConfiguration().getHostname(), Function.identity()));
    Network<IsisNode, IsisEdge> initialIsisTopology = initIsisTopology(configs, topology);
    vrs.values()
        .forEach(
            vr -> vr.initQueuesAndDeltaBuilders(nodes, topology, bgpTopology, initialIsisTopology));

    // Assert that queues are empty as there are no OSPF, BGP, nor IS-IS processes
    vrs.values()
        .forEach(
            vr -> {
              assertThat(vr._bgpIncomingRoutes, anEmptyMap());
              assertThat(vr._isisIncomingRoutes, anEmptyMap());
              assertThat(vr._ospfExternalIncomingRoutes, anEmptyMap());
            });

    // Set bgp processes and neighbors
    BgpProcess proc1 = nf.bgpProcessBuilder().setVrf(vrf1).setRouterId(new Ip("1.1.1.1")).build();
    BgpProcess proc2 = nf.bgpProcessBuilder().setVrf(vrf2).setRouterId(new Ip("1.1.1.2")).build();
    nf.bgpNeighborBuilder()
        .setPeerAddress(new Ip("1.1.1.2"))
        .setLocalIp(new Ip("1.1.1.1"))
        .setBgpProcess(proc1)
        .setRemoteAs(2L)
        .setLocalAs(1L)
        .build();
    nf.bgpNeighborBuilder()
        .setPeerAddress(new Ip("1.1.1.1"))
        .setLocalIp(new Ip("1.1.1.2"))
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
          .initQueuesAndDeltaBuilders(nodes, topology, bgpTopology2, isisTopology);
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
    Interface.Builder ib = nf.interfaceBuilder().setActive(true);

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

    Map<String, Configuration> configs = ImmutableMap.of(c1.getName(), c1, c2.getName(), c2);
    Topology topology = synthesizeTopology(configs);
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ImmutableValueGraph.copyOf(ValueGraphBuilder.directed().allowsSelfLoops(false).build());
    Map<String, Node> nodes =
        ImmutableMap.of(c1.getName(), new Node(c1), c2.getName(), new Node(c2));
    Map<String, VirtualRouter> vrs =
        nodes
            .values()
            .stream()
            .map(n -> n.getVirtualRouters().get(DEFAULT_VRF_NAME))
            .collect(
                ImmutableMap.toImmutableMap(
                    vr -> vr.getConfiguration().getHostname(), Function.identity()));
    Network<IsisNode, IsisEdge> initialIsisTopology = initIsisTopology(configs, topology);
    vrs.values()
        .forEach(
            vr -> vr.initQueuesAndDeltaBuilders(nodes, topology, bgpTopology, initialIsisTopology));

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
            vr -> vr.initQueuesAndDeltaBuilders(nodes, topology, bgpTopology, updatedIsisTopology));

    // Assert that queues are initialized
    assertThat(
        vrs.get(c1.getName())._isisIncomingRoutes.keySet(),
        equalTo(
            ImmutableSet.of(
                new IsisEdge(
                    IsisLevel.LEVEL_1_2,
                    new IsisNode(c2.getHostname(), i2.getName()),
                    new IsisNode(c1.getHostname(), i1.getName())))));
    assertThat(
        vrs.get(c2.getName())._isisIncomingRoutes.keySet(),
        equalTo(
            ImmutableSet.of(
                new IsisEdge(
                    IsisLevel.LEVEL_1_2,
                    new IsisNode(c1.getHostname(), i1.getName()),
                    new IsisNode(c2.getHostname(), i2.getName())))));
  }

  /** Test that the routes are exact route matches are removed from the RIB by default */
  @Test
  public void testImportRibExactRemoval() {
    BgpMultipathRib rib = new BgpMultipathRib(MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    BgpRoute r1 =
        new BgpRoute.Builder()
            .setNetwork(new Prefix(new Ip("1.1.1.1"), 32))
            .setProtocol(RoutingProtocol.IBGP)
            .setOriginType(OriginType.IGP)
            .setOriginatorIp(new Ip("7.7.7.7"))
            .setReceivedFromIp(new Ip("7.7.7.7"))
            .build();
    BgpRoute r2 =
        new BgpRoute.Builder()
            .setNetwork(new Prefix(new Ip("1.1.1.1"), 32))
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.IGP)
            .setOriginatorIp(new Ip("7.7.7.7"))
            .setReceivedFromIp(new Ip("7.7.7.7"))
            .build();

    // Setup
    rib.mergeRoute(r1);
    RibDelta<BgpRoute> delta = new Builder<>(rib).add(r2).remove(r1, Reason.WITHDRAW).build();
    // Test
    RibDelta.importRibDelta(rib, delta);
    // r1 remains due to different protocol
    assertThat(rib.getRoutes(), contains(r2));
  }

  @Test
  public void testMultipathAddWithReplacement() {
    BgpBestPathRib bestPathRib = new BgpBestPathRib(BgpTieBreaker.CLUSTER_LIST_LENGTH, null, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(MultipathEquivalentAsPathMatchMode.EXACT_PATH);

    RibDelta<BgpRoute> staging;
    BgpRoute.Builder routeBuilder = new BgpRoute.Builder();
    routeBuilder
        .setNetwork(new Prefix(new Ip("1.1.1.1"), 32))
        .setProtocol(RoutingProtocol.IBGP)
        .setOriginType(OriginType.IGP)
        .setOriginatorIp(new Ip("7.7.7.7"))
        .setReceivedFromIp(new Ip("7.7.7.1"))
        .setClusterList(ImmutableSortedSet.of(1L, 2L, 3L))
        .build();
    BgpRoute oldRoute1 = routeBuilder.build();
    routeBuilder.setReceivedFromIp(new Ip("7.7.7.2"));
    BgpRoute oldRoute2 = routeBuilder.build();

    // Setup original RIB state
    multipathRib.mergeRoute(oldRoute1);
    multipathRib.mergeRoute(oldRoute2);
    bestPathRib.mergeRoute(oldRoute1);
    bestPathRib.mergeRoute(oldRoute2);
    multipathRib.setBestAsPaths(bestPathRib.getBestAsPaths());
    // Just a sanity check
    assertThat(bestPathRib.getRoutes(), hasSize(1));
    assertThat(multipathRib.getRoutes(), hasSize(2));

    // Create better routes
    BgpRoute newRoute1 =
        routeBuilder
            .setReceivedFromIp(new Ip("7.7.7.1"))
            .setClusterList(ImmutableSortedSet.of(1L))
            .build();
    BgpRoute newRoute2 =
        routeBuilder
            .setReceivedFromIp(new Ip("7.7.7.2"))
            .setClusterList(ImmutableSortedSet.of(1L))
            .build();
    Builder<BgpRoute> builder = new Builder<>(null);

    staging = builder.add(newRoute1).add(newRoute2).build();

    // TEST: propagate in sync here
    Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> e =
        VirtualRouter.syncBgpDeltaPropagation(bestPathRib, multipathRib, staging);
    RibDelta<BgpRoute> mpDelta = e.getValue();

    // One route only, with lower cluster list length and lower receivedFromIp
    assertThat(bestPathRib.getRoutes(), contains(newRoute1));
    // Both new routes
    assertThat(multipathRib.getRoutes(), containsInAnyOrder(newRoute1, newRoute2));

    assert mpDelta != null;
    // 2 removals, 2 additions
    assertThat(mpDelta.getActions(), hasSize(4));
  }

  @Test
  public void testMultipathAddNoReplacementNoBestPathChange() {
    BgpBestPathRib bestPathRib = new BgpBestPathRib(BgpTieBreaker.CLUSTER_LIST_LENGTH, null, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(MultipathEquivalentAsPathMatchMode.EXACT_PATH);

    RibDelta<BgpRoute> staging;
    BgpRoute.Builder routeBuilder = new BgpRoute.Builder();
    routeBuilder
        .setNetwork(new Prefix(new Ip("1.1.1.1"), 32))
        .setProtocol(RoutingProtocol.IBGP)
        .setOriginType(OriginType.IGP)
        .setOriginatorIp(new Ip("7.7.7.7"))
        .setReceivedFromIp(new Ip("7.7.7.1"))
        .setClusterList(ImmutableSortedSet.of(1L, 2L, 3L))
        .build();
    BgpRoute oldRoute1 = routeBuilder.build();

    // Setup original RIB state
    multipathRib.mergeRoute(oldRoute1);
    bestPathRib.mergeRoute(oldRoute1);
    multipathRib.setBestAsPaths(bestPathRib.getBestAsPaths());
    // Just a sanity check
    assertThat(bestPathRib.getRoutes(), hasSize(1));
    assertThat(multipathRib.getRoutes(), hasSize(1));

    // Create additional routes, no better than oldRoute1
    BgpRoute addedRoute = routeBuilder.setReceivedFromIp(new Ip("7.7.7.2")).build();
    Builder<BgpRoute> builder = new Builder<>(null);

    staging = builder.add(addedRoute).build();

    // TEST: propagate in sync here
    Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> e =
        VirtualRouter.syncBgpDeltaPropagation(bestPathRib, multipathRib, staging);
    RibDelta<BgpRoute> mpDelta = e.getValue();

    // One route only, with lower cluster list length and lower receivedFromIp
    assertThat(bestPathRib.getRoutes(), contains(oldRoute1));
    // Both new routes
    assertThat(multipathRib.getRoutes(), containsInAnyOrder(oldRoute1, addedRoute));

    assert mpDelta != null;
    // 1 addition
    assertThat(mpDelta.getActions(), hasSize(1));
  }

  @Test
  public void testMultipathRemovalNoReplacementNoBestPathChange() {
    BgpBestPathRib bestPathRib = new BgpBestPathRib(BgpTieBreaker.CLUSTER_LIST_LENGTH, null, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(MultipathEquivalentAsPathMatchMode.EXACT_PATH);

    RibDelta<BgpRoute> staging;
    BgpRoute.Builder routeBuilder = new BgpRoute.Builder();
    routeBuilder
        .setNetwork(new Prefix(new Ip("1.1.1.1"), 32))
        .setProtocol(RoutingProtocol.IBGP)
        .setOriginType(OriginType.IGP)
        .setOriginatorIp(new Ip("7.7.7.7"))
        .setReceivedFromIp(new Ip("7.7.7.1"))
        .setClusterList(ImmutableSortedSet.of(1L, 2L, 3L))
        .build();
    BgpRoute oldRoute1 = routeBuilder.build();
    routeBuilder.setReceivedFromIp(new Ip("7.7.7.2"));
    BgpRoute oldRoute2 = routeBuilder.build();

    // Setup original RIB state
    multipathRib.mergeRoute(oldRoute1);
    multipathRib.mergeRoute(oldRoute2);
    bestPathRib.mergeRoute(oldRoute1);
    multipathRib.setBestAsPaths(bestPathRib.getBestAsPaths());
    // Just a sanity check
    assertThat(bestPathRib.getRoutes(), hasSize(1));
    assertThat(multipathRib.getRoutes(), hasSize(2));

    // Remove the worse route
    Builder<BgpRoute> builder = new Builder<>(null);
    staging = builder.remove(oldRoute2, Reason.WITHDRAW).build();

    // TEST: propagate in sync here
    Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> e =
        VirtualRouter.syncBgpDeltaPropagation(bestPathRib, multipathRib, staging);
    RibDelta<BgpRoute> mpDelta = e.getValue();

    // One route only, with lower cluster list length and lower receivedFromIp
    assertThat(bestPathRib.getRoutes(), contains(oldRoute1));
    // Both new routes
    assertThat(multipathRib.getRoutes(), contains(oldRoute1));

    assert mpDelta != null;
    // 1 removal
    assertThat(mpDelta.getActions(), hasSize(1));
  }

  @Test
  public void testMultipathReplacementBestPathChange() {
    BgpBestPathRib bestPathRib = new BgpBestPathRib(BgpTieBreaker.CLUSTER_LIST_LENGTH, null, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(MultipathEquivalentAsPathMatchMode.EXACT_PATH);

    RibDelta<BgpRoute> staging;
    BgpRoute.Builder routeBuilder = new BgpRoute.Builder();
    routeBuilder
        .setNetwork(new Prefix(new Ip("1.1.1.1"), 32))
        .setProtocol(RoutingProtocol.IBGP)
        .setOriginType(OriginType.IGP)
        .setOriginatorIp(new Ip("7.7.7.7"))
        .setReceivedFromIp(new Ip("7.7.7.1"))
        .setClusterList(ImmutableSortedSet.of(1L, 2L, 3L))
        .build();
    BgpRoute oldRoute1 = routeBuilder.build();
    routeBuilder.setReceivedFromIp(new Ip("7.7.7.2"));
    BgpRoute oldRoute2 = routeBuilder.build();

    // Setup original RIB state
    multipathRib.mergeRoute(oldRoute1);
    multipathRib.mergeRoute(oldRoute2);
    bestPathRib.mergeRoute(oldRoute1);
    multipathRib.setBestAsPaths(bestPathRib.getBestAsPaths());
    // Just a sanity check
    assertThat(bestPathRib.getRoutes(), hasSize(1));
    assertThat(multipathRib.getRoutes(), hasSize(2));

    // Create better routes
    BgpRoute newGoodRoute1 =
        routeBuilder
            .setNetwork(Prefix.parse("2.2.2.2/32"))
            .setReceivedFromIp(new Ip("7.7.7.1"))
            .setClusterList(ImmutableSortedSet.of(1L))
            .build();
    BgpRoute newGoodRoute2 =
        routeBuilder
            .setNetwork(Prefix.parse("2.2.2.2/32"))
            .setReceivedFromIp(new Ip("7.7.7.2"))
            .setClusterList(ImmutableSortedSet.of(1L))
            .build();
    Builder<BgpRoute> builder = new Builder<>(null);

    staging =
        builder.remove(oldRoute1, Reason.REPLACE).add(newGoodRoute1).add(newGoodRoute2).build();
    /*
     * TEST: propagate deltas in sync here.
     * Old routes should all be gone (no best path remains) and two
     * new routes should exist for the new prefix
     */
    Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> e =
        VirtualRouter.syncBgpDeltaPropagation(bestPathRib, multipathRib, staging);

    // One route only, with lower cluster list length and lower receivedFromIp
    assertThat(bestPathRib.getRoutes(), contains(newGoodRoute1));
    // Both good routes
    assertThat(multipathRib.getRoutes(), containsInAnyOrder(newGoodRoute1, newGoodRoute2));

    RibDelta<BgpRoute> mpDelta = e.getValue();
    assert e.getValue() != null;
    // 4 operations in multipath: 2 removals, 2 additions
    assertThat(mpDelta.getActions(), hasSize(4));
  }

  @Test
  public void testMutipathBestPathWithdrawalMultipathAvail() {
    BgpBestPathRib bestPathRib = new BgpBestPathRib(BgpTieBreaker.CLUSTER_LIST_LENGTH, null, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(MultipathEquivalentAsPathMatchMode.EXACT_PATH);

    RibDelta<BgpRoute> staging;
    BgpRoute.Builder routeBuilder = new BgpRoute.Builder();
    routeBuilder
        .setNetwork(new Prefix(new Ip("1.1.1.1"), 32))
        .setProtocol(RoutingProtocol.IBGP)
        .setOriginType(OriginType.IGP)
        .setOriginatorIp(new Ip("7.7.7.7"))
        .setReceivedFromIp(new Ip("7.7.7.1"))
        .setClusterList(ImmutableSortedSet.of(1L, 2L, 3L))
        .build();
    BgpRoute oldRoute1 = routeBuilder.build();
    routeBuilder.setReceivedFromIp(new Ip("7.7.7.2"));
    BgpRoute oldRoute2 = routeBuilder.build();
    routeBuilder.setReceivedFromIp(new Ip("7.7.7.3"));
    BgpRoute oldRoute3 = routeBuilder.build();

    // Setup original RIB state
    multipathRib.mergeRoute(oldRoute1);
    multipathRib.mergeRoute(oldRoute2);
    multipathRib.mergeRoute(oldRoute3);
    bestPathRib.mergeRoute(oldRoute1);
    multipathRib.setBestAsPaths(bestPathRib.getBestAsPaths());
    // Just a sanity check
    assertThat(bestPathRib.getRoutes(), hasSize(1));
    assertThat(multipathRib.getRoutes(), hasSize(3));

    Builder<BgpRoute> builder = new Builder<>(null);
    staging = builder.remove(oldRoute1, Reason.WITHDRAW).build();
    /*
     * TEST: propagate deltas in sync here.
     * Old best path is gone. Now must choose best path from the multipath RIB.
     * new routes should exist for the new prefix
     */
    VirtualRouter.syncBgpDeltaPropagation(bestPathRib, multipathRib, staging);

    // One route only, taken from the multipathRib
    assertThat(bestPathRib.getRoutes(), contains(oldRoute2));
    // Both good routes
    assertThat(multipathRib.getRoutes(), containsInAnyOrder(oldRoute2, oldRoute3));
  }
}

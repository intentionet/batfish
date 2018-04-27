package org.batfish.dataplane.ibdp;

import static org.batfish.common.util.CommonUtil.computeIpOwners;
import static org.batfish.common.util.CommonUtil.initBgpTopology;
import static org.batfish.common.util.CommonUtil.synthesizeTopology;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasDestinationIp;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasNetwork;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasOriginatorIp;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasSourceIp;
import static org.batfish.datamodel.matchers.BgpAdvertisementMatchers.hasType;
import static org.hamcrest.CoreMatchers.not;
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
import com.google.common.graph.Network;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
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
  private static final int TEST_AS1 = 1;
  private static final int TEST_AS2 = 2;
  private static final int TEST_AS3 = 3;
  private static final Ip TEST_DEST_IP = new Ip("2.2.2.2");
  private static final ConfigurationFormat FORMAT = ConfigurationFormat.CISCO_IOS;
  private static final int TEST_METRIC = 30;
  private static final Ip TEST_SRC_IP = new Ip("1.1.1.1");
  private static final Prefix TEST_NETWORK = Prefix.parse("4.4.4.4/32");
  private static final Ip TEST_NEXT_HOP_IP1 = new Ip("1.2.3.4");
  private static final Ip TEST_NEXT_HOP_IP2 = new Ip("2.3.4.5");
  private static final String TEST_VIRTUAL_ROUTER_NAME = "testvirtualrouter";

  private BgpNeighbor.Builder _bgpNeighborBuilder;
  private BgpRoute.Builder _bgpRouteBuilder;
  private Statement _exitAcceptStatement = Statements.ExitAccept.toStaticStatement();
  private Statement _exitRejectStatement = Statements.ExitReject.toStaticStatement();
  private Map<Ip, Set<String>> _ipOwners;
  private RoutingPolicy.Builder _routingPolicyBuilder;
  private VirtualRouter _testVirtualRouter;

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    _testVirtualRouter = createEmptyVirtualRouter(nf, TEST_VIRTUAL_ROUTER_NAME);
    BgpProcess bgpProcess =
        nf.bgpProcessBuilder().setVrf(_testVirtualRouter._vrf).setRouterId(TEST_SRC_IP).build();
    Configuration neighborConfiguration =
        nf.configurationBuilder()
            .setConfigurationFormat(FORMAT)
            .setHostname(NEIGHBOR_HOST_NAME)
            .build();
    _bgpNeighborBuilder =
        nf.bgpNeighborBuilder()
            .setOwner(neighborConfiguration)
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
    return n.getVirtualRouters().get(Configuration.DEFAULT_VRF_NAME);
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
        .setAdditionalPathSend(true)
        .setAdditionalPathSelectAll(true)
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
    _testVirtualRouter
        ._sentBgpAdvertisements
        .stream()
        .forEach(
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
    _testVirtualRouter
        ._sentBgpAdvertisements
        .stream()
        .forEach(
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
        .setAdditionalPathSend(true)
        .setAdditionalPathSelectAll(true)
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
   * Creates an empty {@link VirtualRouter} along with creating owner {@link Configuration} and
   * {@link Vrf}
   *
   * @param nodeName Node name of the owner {@link Configuration}
   * @return new instance of {@link VirtualRouter}
   */
  private static VirtualRouter createEmptyVirtualRouter(NetworkFactory nf, String nodeName) {
    Configuration config = BatfishTestUtils.createTestConfiguration(nodeName, FORMAT, "interface1");
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Vrf vrf = vb.setOwner(config).build();
    config.getVrfs().put(TEST_VIRTUAL_ROUTER_NAME, vrf);
    VirtualRouter virtualRouter = new VirtualRouter(TEST_VIRTUAL_ROUTER_NAME, config);
    virtualRouter.initRibs();
    virtualRouter._sentBgpAdvertisements = new LinkedHashSet<>();
    return virtualRouter;
  }

  @Test
  public void testGetBetterOspfRouteMetric() {
    Prefix ospfInterAreaRoutePrefix = Prefix.parse("1.1.1.1/24");
    long definedMetric = 5;
    long definedArea = 1;
    OspfInterAreaRoute route =
        new OspfInterAreaRoute(
            ospfInterAreaRoutePrefix,
            Ip.MAX,
            RoutingProtocol.OSPF_IA.getDefaultAdministrativeCost(FORMAT),
            definedMetric,
            0);

    // The route is in the prefix and existing metric is null, so return the route's metric
    assertThat(
        VirtualRouter.computeUpdatedOspfSummaryMetric(route, Prefix.ZERO, null, definedArea, true),
        equalTo(definedMetric));
    // Return the lower metric if the existing not null and using old RFC
    assertThat(
        VirtualRouter.computeUpdatedOspfSummaryMetric(route, Prefix.ZERO, 10L, definedArea, true),
        equalTo(definedMetric));
    // Return the higher metric if the existing metric is not null and using new RFC
    assertThat(
        VirtualRouter.computeUpdatedOspfSummaryMetric(route, Prefix.ZERO, 10L, definedArea, false),
        equalTo(10L));
    // The route is in the prefix but the existing metric is lower, so return the existing metric
    assertThat(
        VirtualRouter.computeUpdatedOspfSummaryMetric(route, Prefix.ZERO, 4L, definedArea, true),
        equalTo(4L));
    // The route is in the prefix but the existing metric is lower, so return the existing metric
    assertThat(
        VirtualRouter.computeUpdatedOspfSummaryMetric(route, Prefix.ZERO, 4L, definedArea, false),
        equalTo(definedMetric));
    // The route is not in the area's prefix, return the current metric
    assertThat(
        VirtualRouter.computeUpdatedOspfSummaryMetric(
            route, Prefix.parse("2.0.0.0/8"), 4L, definedArea, true),
        equalTo(4L));

    OspfInterAreaRoute sameAreaRoute =
        new OspfInterAreaRoute(
            ospfInterAreaRoutePrefix,
            Ip.MAX,
            RoutingProtocol.OSPF_IA.getDefaultAdministrativeCost(FORMAT),
            definedMetric,
            1); // the area is the same as definedArea
    // Thus the metric should remain null
    assertThat(
        VirtualRouter.computeUpdatedOspfSummaryMetric(
            sameAreaRoute, Prefix.ZERO, null, definedArea, true),
        equalTo(null));
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

  @Test
  public void testInitRibsEmpty() {
    VirtualRouter vr = makeIosVirtualRouter(null);

    // We expect the router to have the following RIBs and all of them are empty
    vr.initRibs();

    // Simple RIBs
    assertThat(vr.getConnectedRib().getRoutes(), is(emptyIterableOf(ConnectedRoute.class)));
    assertThat(vr._staticRib.getRoutes(), is(emptyIterableOf(StaticRoute.class)));
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
                    Entry::getKey,
                    e -> e.getValue().getVirtualRouters().get(Configuration.DEFAULT_VRF_NAME)));
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
        ImmutableSortedSet.of(new StaticRoute(Prefix.parse("1.1.1.1/32"), Ip.ZERO, null, 1, 0));
    vr._vrf.setStaticRoutes(routeSet);

    // Test
    vr.initStaticRib();

    assertThat(vr._staticRib.getRoutes(), equalTo(routeSet));
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
    StaticRoute sr1 = new StaticRoute(new Prefix(new Ip("1.1.1.1"), 32), Ip.ZERO, null, 1, 1);
    StaticRoute sr2 = new StaticRoute(new Prefix(new Ip("1.1.1.1"), 32), Ip.ZERO, null, 100, 1);
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
    StaticRoute sr1 = new StaticRoute(new Prefix(new Ip("1.1.1.1"), 32), Ip.ZERO, null, 1, 1);
    StaticRoute sr2 = new StaticRoute(new Prefix(new Ip("1.1.1.1"), 32), Ip.ZERO, null, 100, 1);
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
    Node n1 = TestUtils.makeIosRouter("r1");
    Node n2 = TestUtils.makeIosRouter("r2");
    addInterfaces(
        n1.getConfiguration(), ImmutableMap.of("eth1", new InterfaceAddress("1.1.1.0/24")));
    addInterfaces(
        n2.getConfiguration(), ImmutableMap.of("eth1", new InterfaceAddress("1.1.1.0/24")));
    Topology topology =
        synthesizeTopology(
            ImmutableMap.of("r1", n1.getConfiguration(), "r2", n2.getConfiguration()));

    Map<String, Configuration> configs =
        ImmutableMap.of("r1", n1.getConfiguration(), "r2", n2.getConfiguration());
    Network<BgpNeighbor, BgpSession> bgpTopology =
        initBgpTopology(configs, computeIpOwners(configs, false), false);

    Map<String, Node> nodes = ImmutableMap.of("r1", n1, "r2", n2);
    Map<String, VirtualRouter> vrs =
        nodes
            .values()
            .stream()
            .map(n -> n.getVirtualRouters().get(Configuration.DEFAULT_VRF_NAME))
            .collect(
                ImmutableMap.toImmutableMap(
                    vr -> vr.getConfiguration().getHostname(), Function.identity()));

    for (Node n : nodes.values()) {
      n.getVirtualRouters()
          .get(Configuration.DEFAULT_VRF_NAME)
          .initQueuesAndDeltaBuilders(nodes, topology, bgpTopology);
    }
    // Assert that queues are empty as there is no OSPF or BGP processes
    vrs.values()
        .forEach(
            vr -> {
              assertThat(vr._bgpIncomingRoutes, is(notNullValue()));
              assertThat(vr._ospfExternalIncomingRoutes, is(notNullValue()));
            });

    // Set bgp
    n1.getConfiguration()
        .getVrfs()
        .get(Configuration.DEFAULT_VRF_NAME)
        .setBgpProcess(new BgpProcess());
    n2.getConfiguration()
        .getVrfs()
        .get(Configuration.DEFAULT_VRF_NAME)
        .setBgpProcess(new BgpProcess());

    // Re-run
    for (Node n : nodes.values()) {
      n.getVirtualRouters()
          .get(Configuration.DEFAULT_VRF_NAME)
          .initQueuesAndDeltaBuilders(nodes, topology, bgpTopology);
    }
    // Assert that queues are initialized
    vrs.values()
        .forEach(
            vr -> {
              assertThat(vr._bgpIncomingRoutes, is(notNullValue()));
              // TODO: change test init for this to work
              //              assertThat(vr._bgpIncomingRoutes.values(), hasSize(1));
            });
  }

  /** Test that the routes are exact route matches are removed from the RIB by default */
  @Test
  public void testImportRibExactRemoval() {
    BgpMultipathRib rib =
        new BgpMultipathRib(_testVirtualRouter, MultipathEquivalentAsPathMatchMode.EXACT_PATH);
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
    BgpBestPathRib bestPathRib = new BgpBestPathRib(null, BgpTieBreaker.CLUSTER_LIST_LENGTH, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(null, MultipathEquivalentAsPathMatchMode.EXACT_PATH);

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
    BgpBestPathRib bestPathRib = new BgpBestPathRib(null, BgpTieBreaker.CLUSTER_LIST_LENGTH, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(null, MultipathEquivalentAsPathMatchMode.EXACT_PATH);

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
    BgpBestPathRib bestPathRib = new BgpBestPathRib(null, BgpTieBreaker.CLUSTER_LIST_LENGTH, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(null, MultipathEquivalentAsPathMatchMode.EXACT_PATH);

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
    BgpBestPathRib bestPathRib = new BgpBestPathRib(null, BgpTieBreaker.CLUSTER_LIST_LENGTH, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(null, MultipathEquivalentAsPathMatchMode.EXACT_PATH);

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
    BgpBestPathRib bestPathRib = new BgpBestPathRib(null, BgpTieBreaker.CLUSTER_LIST_LENGTH, null);
    BgpMultipathRib multipathRib =
        new BgpMultipathRib(null, MultipathEquivalentAsPathMatchMode.EXACT_PATH);

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
    Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> e =
        VirtualRouter.syncBgpDeltaPropagation(bestPathRib, multipathRib, staging);

    // One route only, taken from the multipathRib
    assertThat(bestPathRib.getRoutes(), contains(oldRoute2));
    // Both good routes
    assertThat(multipathRib.getRoutes(), containsInAnyOrder(oldRoute2, oldRoute3));

    RibDelta<BgpRoute> bpDelta = e.getKey();
    RibDelta<BgpRoute> mpDelta = e.getValue();
    assert bpDelta != null;
    assert mpDelta != null;
    // 2 operations for best path rib, one withdraw, one add
    assertThat(bpDelta.getActions(), hasSize(2));
    // 1 operations for multipath rib, one withdraw
    assertThat(mpDelta.getActions(), hasSize(1));
  }
}

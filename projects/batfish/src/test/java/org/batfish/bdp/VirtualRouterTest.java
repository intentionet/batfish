package org.batfish.bdp;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.batfish.main.BatfishTestUtils.createTestConfiguration;
import static org.batfish.main.BatfishTestUtils.createTestRouter;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterableOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.RipRoute;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.hamcrest.Matchers;
import org.junit.Test;

public class VirtualRouterTest {
  /** Make a CISCO IOS router with 3 interfaces named Eth1-Eth3, /16 prefixes on each interface */
  private static final Map<String, Prefix> exampleInterfacePrefixes = new HashMap<>();

  static {
    exampleInterfacePrefixes.put("Eth1", new Prefix("10.1.0.0/16"));
    exampleInterfacePrefixes.put("Eth2", new Prefix("10.2.0.0/16"));
    exampleInterfacePrefixes.put("Eth3", new Prefix("10.3.0.0/16"));
  }

  private static VirtualRouter makeIosRouter(String hostname) {
    Configuration c = createTestConfiguration(DEFAULT_VRF_NAME, CISCO_IOS, "Eth1", "Eth2", "Eth3");
    return createTestRouter(hostname, c);
  }

  private static List<VirtualRouter> makeIosRouters(String[] hostnames) {
    List<VirtualRouter> routers = new LinkedList<>();
    Map<String, Node> nodes = new HashMap<>();
    for (String hostname : hostnames) {
      Configuration c =
          createTestConfiguration(DEFAULT_VRF_NAME, CISCO_IOS, "Eth1", "Eth2", "Eth3");
      c.getVrfs().computeIfAbsent(Configuration.DEFAULT_VRF_NAME, Vrf::new);
      Node node = new Node(c, nodes);
      nodes.put(hostname, node);
      VirtualRouter r = new VirtualRouter(Configuration.DEFAULT_VRF_NAME, c, nodes);
      node._virtualRouters.put(Configuration.DEFAULT_VRF_NAME, r);
      routers.add(r);
    }
    return routers;
  }

  private static void addInterfaces(VirtualRouter vr, Map<String, Prefix> ifaceToPrefix) {
    Configuration c = vr._c;
    c.getVrfs().get(DEFAULT_VRF_NAME).setInterfaces(new TreeMap<>());

    ifaceToPrefix.forEach(
        (name, prefix) -> {
          c.getInterfaces().get(name).getAllPrefixes().add(prefix);
          c.getInterfaces().get(name).setPrefix(prefix);
          c.getVrfs()
              .get(DEFAULT_VRF_NAME)
              .getInterfaces()
              .computeIfAbsent(name, k -> new Interface(name, c))
              .getAllPrefixes()
              .add(prefix);
          c.getVrfs().get(DEFAULT_VRF_NAME).getInterfaces().get(name).setPrefix(prefix);
        });
  }

  @Test
  public void testGetBetterOspfRouteMetric() {
    long definedMetric = 5;
    long definedArea = 1;
    OspfInterAreaRoute route =
        new OspfInterAreaRoute(
            new Prefix("1.1.1.1/24"),
            Ip.MAX,
            RoutingProtocol.OSPF_IA.getDefaultAdministrativeCost(CISCO_IOS),
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
            route, new Prefix("2.0.0.0/8"), 4L, definedArea, true),
        equalTo(4L));

    OspfInterAreaRoute sameAreaRoute =
        new OspfInterAreaRoute(
            new Prefix("1.1.1.1/24"),
            Ip.MAX,
            RoutingProtocol.OSPF_IA.getDefaultAdministrativeCost(CISCO_IOS),
            definedMetric,
            1); // the area is the same as definedArea
    // Thus the metric should remain null
    assertThat(
        VirtualRouter.computeUpdatedOspfSummaryMetric(
            sameAreaRoute, Prefix.ZERO, null, definedArea, true),
        equalTo(null));
  }

  @Test
  public void testInitRibsEmpty() {
    VirtualRouter r = makeIosRouter("R1");

    // We expect the router to have the following RIBs and all of them are empty
    r.initRibs();

    // Simple RIBs
    assertThat(r._connectedRib.getRoutes(), is(emptyIterableOf(ConnectedRoute.class)));
    assertThat(r._staticRib.getRoutes(), is(emptyIterableOf(StaticRoute.class)));
    assertThat(r._staticInterfaceRib.getRoutes(), is(emptyIterableOf(StaticRoute.class)));
    assertThat(r._independentRib.getRoutes(), is(emptyIterableOf(AbstractRoute.class)));

    // RIP RIBs
    assertThat(r._ripInternalRib.getRoutes(), is(emptyIterableOf(RipInternalRoute.class)));
    assertThat(r._ripInternalStagingRib.getRoutes(), is(emptyIterableOf(RipInternalRoute.class)));
    assertThat(r._ripRib.getRoutes(), is(emptyIterableOf(RipRoute.class)));

    // OSPF RIBs
    assertThat(r._ospfRib.getRoutes(), is(emptyIterableOf(OspfRoute.class)));
    assertThat(
        r._ospfExternalType1Rib.getRoutes(), is(emptyIterableOf(OspfExternalType1Route.class)));
    assertThat(
        r._ospfExternalType1StagingRib.getRoutes(),
        is(emptyIterableOf(OspfExternalType1Route.class)));
    assertThat(
        r._ospfExternalType2Rib.getRoutes(), is(emptyIterableOf(OspfExternalType2Route.class)));
    assertThat(
        r._ospfExternalType2StagingRib.getRoutes(),
        is(emptyIterableOf(OspfExternalType2Route.class)));
    assertThat(r._ospfInterAreaRib.getRoutes(), is(emptyIterableOf(OspfInterAreaRoute.class)));
    assertThat(
        r._ospfInterAreaStagingRib.getRoutes(), is(emptyIterableOf(OspfInterAreaRoute.class)));
    assertThat(r._ospfIntraAreaRib.getRoutes(), is(emptyIterableOf(OspfIntraAreaRoute.class)));
    assertThat(
        r._ospfIntraAreaStagingRib.getRoutes(), is(emptyIterableOf(OspfIntraAreaRoute.class)));
    assertThat(r._ospfRib.getRoutes(), is(emptyIterableOf(OspfRoute.class)));

    // BGP ribs
    // Ibgp
    assertThat(r._baseIbgpRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(r._ibgpBestPathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(r._ibgpMultipathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(r._ibgpStagingRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    // Ebgp
    assertThat(r._baseEbgpRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(r._ebgpBestPathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(r._ebgpMultipathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(r._ebgpStagingRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    // Combined bgp
    assertThat(r._bgpBestPathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(r._bgpMultipathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));

    // Main RIB
    assertThat(r._mainRib.getRoutes(), is(emptyIterableOf(AbstractRoute.class)));

    // Prev Ribs are expected to be null
    assertThat(r._prevOspfExternalType1Rib, is(nullValue()));
    assertThat(r._prevOspfExternalType2Rib, is(nullValue()));

    assertThat(r._prevBgpBestPathRib, is(nullValue()));
    assertThat(r._prevEbgpBestPathRib, is(nullValue()));
    assertThat(r._prevIbgpBestPathRib, is(nullValue()));

    assertThat(r._prevBgpMultipathRib, is(nullValue()));
    assertThat(r._prevEbgpMultipathRib, is(nullValue()));
    assertThat(r._prevIbgpMultipathRib, is(nullValue()));

    assertThat(r._prevMainRib, is(nullValue()));
  }

  /** Check that initialization of Connected RIB is as expected */
  @Test
  public void testInitConnectedRib() {
    // Setup
    VirtualRouter r = makeIosRouter("R1");
    addInterfaces(r, exampleInterfacePrefixes);
    r.initRibs();

    // Test
    r.initConnectedRib();

    // Assert that all interface prefixes have been processed
    assertThat(
        r._connectedRib.getRoutes(),
        Matchers.containsInAnyOrder(
            new ConnectedRoute(new Prefix("10.1.0.0/16"), "Eth1"),
            new ConnectedRoute(new Prefix("10.2.0.0/16"), "Eth2"),
            new ConnectedRoute(new Prefix("10.3.0.0/16"), "Eth3")));
  }

  /** Check that initialization of RIP internal routes happens correctly */
  @Test
  public void testRipInitialization() {
    // Incomplete Setup
    VirtualRouter vr = makeIosRouter("R1");
    addInterfaces(vr, exampleInterfacePrefixes);
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
            new RipInternalRoute(
                new Prefix("10.1.0.0/16"),
                null,
                RoutingProtocol.RIP.getDefaultAdministrativeCost(vr._c.getConfigurationFormat()),
                RipProcess.DEFAULT_RIP_COST),
            new RipInternalRoute(
                new Prefix("10.2.0.0/16"),
                null,
                RoutingProtocol.RIP.getDefaultAdministrativeCost(vr._c.getConfigurationFormat()),
                RipProcess.DEFAULT_RIP_COST),
            new RipInternalRoute(
                new Prefix("10.3.0.0/16"),
                null,
                RoutingProtocol.RIP.getDefaultAdministrativeCost(vr._c.getConfigurationFormat()),
                RipProcess.DEFAULT_RIP_COST)));
    vr._ripInternalRib.getRoutes();
  }

  @Test
  public void testSticRibInit() {
    VirtualRouter vr = makeIosRouter("R1");
    vr.initRibs();
    TreeSet<StaticRoute> routeSet = new TreeSet<>();
    routeSet.add(new StaticRoute(new Prefix("1.1.1.1/32"), Ip.ZERO, null, 1, 0));
    vr._vrf.setStaticRoutes(routeSet);

    // Test
    vr.initStaticRib();

    assertThat(vr._staticRib.getRoutes(), equalTo(routeSet));
  }

  /** Test that staging of a single OSPF Inter-Area route works as expected */
  @Test
  public void testStageOSPFInterAreaRoute() {
    VirtualRouter vr = makeIosRouter("R1");
    vr.initRibs();

    int admin = 50;
    int metric = 100;
    long area = 1L;
    Prefix prefix = new Prefix("7.7.7.0/24");
    OspfInterAreaRoute iaroute =
        new OspfInterAreaRoute(prefix, new Ip("7.7.1.1"), admin, metric, area);

    // Test
    Ip newNextHop = new Ip("10.2.1.1");
    vr.stageOspfInterAreaRoute(iaroute, newNextHop, 10, admin, area);

    // Check what's in the RIB is correct.
    // Note the new nextHopIP and the increased metric on the new route.
    assertThat(
        vr._ospfInterAreaStagingRib.getRoutes(),
        contains(new OspfInterAreaRoute(prefix, newNextHop, admin, metric + 10, area)));
    assertThat(vr._ospfInterAreaStagingRib.getRoutes(), not(contains(iaroute)));
  }

  /** Test OSPF internal route propagation from neighbor to neighbor in the same OSPF area */
  @Test
  public void testpropagateOspfInternalSameAreaNeighbor() {
    // Setup
    List<VirtualRouter> routers = makeIosRouters(new String[] {"R1", "R2"});
    VirtualRouter testRouter = routers.get(0);
    VirtualRouter exportingRouter = routers.get(1);
    addInterfaces(testRouter, exampleInterfacePrefixes);
    HashMap<String, Prefix> r2prefixes = new HashMap<>();
    r2prefixes.put("Eth1", new Prefix("10.4.0.0/16"));
    addInterfaces(exportingRouter, r2prefixes);

    routers.forEach(
        r -> {
          r.initRibs();
          r._vrf.setOspfProcess(new OspfProcess());
          r._c.getInterfaces().forEach((name, iface) -> iface.setOspfArea(new OspfArea(0L)));
          r._c.getInterfaces().forEach((name, iface) -> iface.setOspfCost(10));
        });

    // Put some OSPF routes in the exporting router
    int admin = 50;
    int metric = 100;
    long area = 1L;
    int interfaceCost = 22;
    OspfIntraAreaRoute testRouteIntraArea =
        new OspfIntraAreaRoute(
            new Prefix("192.168.1.1/32"), new Ip("7.7.1.1"), admin, metric, area);
    OspfInterAreaRoute testRouteInterAreaSame =
        new OspfInterAreaRoute(
            new Prefix("192.168.1.2/32"), new Ip("7.7.1.2"), admin, metric, area);
    OspfInterAreaRoute testRouteInterAreaDiff =
        new OspfInterAreaRoute(
            new Prefix("192.168.1.3/32"), new Ip("7.7.1.3"), admin, metric, area + 1);
    // Intra area route should be propagated
    exportingRouter._ospfIntraAreaRib.mergeRoute(testRouteIntraArea);

    // Inter area from our own are is ignored
    exportingRouter._ospfInterAreaRib.mergeRoute(testRouteInterAreaSame);
    // Inter area from a different area is propagated
    exportingRouter._ospfInterAreaRib.mergeRoute(testRouteInterAreaDiff);

    // Test
    // Assert preconditions
    assertThat(
        testRouter._ospfIntraAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfIntraAreaRoute.class)));
    assertThat(exportingRouter._ospfIntraAreaRib.containsRoute(testRouteIntraArea), is(true));

    testRouter.propagateOspfInternalRoutesSameAreaNeighbor(
        exportingRouter.getNodes().get("R2"),
        exportingRouter._c.getInterfaces().get("Eth1"),
        interfaceCost,
        admin,
        area);

    // Assert post-conditions, that routes have been successfully propagated, and with updated
    // metrics!
    assertThat(
        testRouter._ospfIntraAreaStagingRib.containsRoute(
            new OspfIntraAreaRoute(
                new Prefix("192.168.1.1/32"),
                new Ip("10.4.0.0"),
                admin,
                metric + interfaceCost,
                area)),
        is(true));
    assertThat(testRouter._ospfIntraAreaStagingRib.containsRoute(testRouteIntraArea), is(false));
    // not propagated, inter-area within same area makes no sense
    assertThat(
        testRouter._ospfInterAreaStagingRib.containsRoute(testRouteInterAreaSame), is(false));
    assertThat(
        testRouter._ospfInterAreaStagingRib.containsRoute(testRouteInterAreaDiff), is(false));
    assertThat(
        testRouter._ospfInterAreaStagingRib.containsRoute(
            new OspfInterAreaRoute(
                new Prefix("192.168.1.3/32"),
                new Ip("10.4.0.0"),
                admin,
                metric + interfaceCost,
                area)),
        is(true));
  }

  /** Test OSPF internal route propagation from neighbor to neighbor in the different OSPF areas */
  @Test
  public void testpropagateOspfInternalDifferentAreaNeighbor() {
    // Setup
    List<VirtualRouter> routers = makeIosRouters(new String[] {"R1", "R2"});
    VirtualRouter testRouter = routers.get(0);
    VirtualRouter exportingRouter = routers.get(1);
    addInterfaces(testRouter, exampleInterfacePrefixes);
    HashMap<String, Prefix> r2prefixes = new HashMap<>();
    r2prefixes.put("Eth1", new Prefix("10.4.0.0/16"));
    addInterfaces(exportingRouter, r2prefixes);
    // Ensure routers are in different OSPF areas
    testRouter._c.getInterfaces().forEach((name, iface) -> iface.setOspfArea(new OspfArea(1L)));
    exportingRouter
        ._c
        .getInterfaces()
        .forEach((name, iface) -> iface.setOspfArea(new OspfArea(0L)));

    routers.forEach(
        r -> {
          r.initRibs();
          r._vrf.setOspfProcess(new OspfProcess());
          r._c.getInterfaces().forEach((name, iface) -> iface.setOspfCost(10));
        });

    // Put some OSPF routes in the exporting router
    int admin = 50;
    int metric = 100;
    long area = 1L;
    int interfaceCost = 22;
    OspfInterAreaRoute reject1 =
        new OspfInterAreaRoute(new Prefix("192.168.1.1/32"), new Ip("7.7.1.1"), admin, metric, 1);
    OspfInterAreaRoute accept1 =
        new OspfInterAreaRoute(new Prefix("192.168.1.3/32"), new Ip("7.7.1.3"), admin, metric, 2);
    OspfInterAreaRoute accept2 =
        new OspfInterAreaRoute(new Prefix("192.168.1.2/32"), new Ip("7.7.1.2"), admin, metric, 0);
    OspfIntraAreaRoute acceptIntra =
        new OspfIntraAreaRoute(new Prefix("192.168.1.4/32"), new Ip("7.7.1.4"), admin, metric, 2);
    exportingRouter._ospfInterAreaRib.mergeRoute(reject1);
    exportingRouter._ospfInterAreaRib.mergeRoute(accept2);
    exportingRouter._ospfInterAreaRib.mergeRoute(accept1);
    exportingRouter._ospfIntraAreaRib.mergeRoute(acceptIntra);

    // Test
    // Assert preconditions
    assertThat(
        testRouter._ospfInterAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfInterAreaRoute.class)));

    testRouter.propagateOspfInternalRoutesDifferentAreaNeighbor(
        exportingRouter.getNodes().get("R2"),
        exportingRouter._c.getInterfaces().get("Eth1"),
        interfaceCost,
        admin,
        area);

    // Assert post-conditions, that routes have been successfully propagated, and with updated
    // metrics. In this case one of the inter-routes and one of the intra routes (converted to
    // inter-route) should make it to the test router
    for (String prefixString :
        new String[] {"192.168.1.2/32", "192.168.1.3/32", "192.168.1.4/32"}) {
      assertThat(
          testRouter._ospfInterAreaStagingRib.containsRoute(
              new OspfInterAreaRoute(
                  new Prefix(prefixString),
                  new Ip("10.4.0.0"),
                  admin,
                  metric + interfaceCost,
                  area)),
          is(true));
    }
    // original route without updated metrics should not be in the RIB
    assertThat(testRouter._ospfInterAreaStagingRib.containsRoute(accept1), is(false));
    // Similarly no prefix matches for routes that had to be rejected
    assertThat(
        testRouter._ospfInterAreaStagingRib.longestPrefixMatch(reject1.getNetwork().getAddress()),
        is(emptyIterableOf(OspfInterAreaRoute.class)));

    // Flip the routers to ensure that receiving in area 0 filters correctly
    testRouter._ospfInterAreaRib.mergeRoute(
        new OspfInterAreaRoute(new Prefix("3.3.3.3/32"), new Ip("33.33.33.33"), admin, 10, 3));
    exportingRouter.propagateOspfInternalRoutesDifferentAreaNeighbor(
        testRouter.getNodes().get("R1"),
        testRouter._c.getInterfaces().get("Eth1"),
        interfaceCost,
        admin,
        0);
    assertThat(
        exportingRouter._ospfInterAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfInterAreaRoute.class)));
  }

  /** Ensure no route propagation when the interfaces are disabled or passive */
  @Test
  public void testOSPFPassiveInterfaceRejection() {
    // Setup
    List<VirtualRouter> routers = makeIosRouters(new String[] {"R1", "R2"});
    VirtualRouter testRouter = routers.get(0);
    testRouter.initRibs();
    VirtualRouter exportingRouter = routers.get(1);
    exportingRouter.initRibs();
    addInterfaces(testRouter, exampleInterfacePrefixes);
    HashMap<String, Prefix> r2prefixes = new HashMap<>();
    r2prefixes.put("Eth1", new Prefix("10.4.0.0/16"));
    addInterfaces(exportingRouter, r2prefixes);
    int adminCost =
        RoutingProtocol.OSPF.getDefaultAdministrativeCost(testRouter._c.getConfigurationFormat());

    Prefix prefix = new Prefix("7.7.7.0/24");
    OspfIntraAreaRoute route = new OspfIntraAreaRoute(prefix, new Ip("7.7.1.1"), adminCost, 20, 1);
    exportingRouter._ospfIntraAreaRib.mergeRoute(route);

    // Set interaces on router 1 to be OSPF passive
    testRouter._c.getInterfaces().forEach((name, iface) -> iface.setActive(false));

    // Test 1
    testRouter.propagateOspfInternalRoutesFromNeighbor(
        exportingRouter.getNodes().get("R2"),
        testRouter._c.getInterfaces().get("Eth1"),
        exportingRouter._c.getInterfaces().get("Eth1"),
        adminCost);

    assertThat(
        testRouter._ospfInterAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfInterAreaRoute.class)));
    assertThat(
        testRouter._ospfIntraAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfIntraAreaRoute.class)));

    // Flip interfaces on router 2 to be passive now
    testRouter._c.getInterfaces().forEach((name, iface) -> iface.setActive(true));
    exportingRouter._c.getInterfaces().forEach((name, iface) -> iface.setActive(false));

    // Test 2
    testRouter.propagateOspfInternalRoutesFromNeighbor(
        exportingRouter.getNodes().get("R2"),
        testRouter._c.getInterfaces().get("Eth1"),
        exportingRouter._c.getInterfaces().get("Eth1"),
        adminCost);

    assertThat(
        testRouter._ospfInterAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfInterAreaRoute.class)));
    assertThat(
        testRouter._ospfIntraAreaStagingRib.getRoutes(),
        is(emptyIterableOf(OspfIntraAreaRoute.class)));
  }
}

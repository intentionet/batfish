package org.batfish.bdp;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterableOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.RipRoute;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.main.BatfishTestUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

public class VirtualRouterTest {
  /** Make a CISCO IOS router with 3 interfaces named Eth1-Eth3, /16 prefixes on each interface */
  private static final Map<String, Prefix> exampleInterfacePrefixes =
      ImmutableMap.<String, Prefix>builder()
          .put("Ethernet1", new Prefix("10.1.0.0/16"))
          .put("Ethernet2", new Prefix("10.2.0.0/16"))
          .put("Ethernet3", new Prefix("10.3.0.0/16"))
          .build();

  private static final ConfigurationFormat FORMAT = ConfigurationFormat.CISCO_IOS;
  private static final Ip SRC_IP = new Ip("1.1.1.1");
  private static final Ip DEST_IP = new Ip("2.2.2.2");
  private static final Prefix TEST_NETWORK = new Prefix("4.4.4.4/32");
  private static final Ip TEST_NEXT_HOP_IP = new Ip("1.2.3.4");
  private static final String TEST_NODE_NAME = "testVirtualRouterNode";
  private static final String TEST_VIRTUAL_ROUTER_NAME = "testvirtualrouter";

  private static void addInterfaces(Configuration c, Map<String, Prefix> interfacePrefixes) {
    NetworkFactory nf = new NetworkFactory();
    Interface.Builder ib =
        nf.interfaceBuilder().setActive(true).setOwner(c).setVrf(c.getDefaultVrf());
    interfacePrefixes.forEach(
        (ifaceName, prefix) -> {
          ib.setName(ifaceName).setPrefix(prefix).build();
        });
  }

  private static Node makeIosRouter(String hostname) {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setHostname(hostname)
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME).setOwner(c).build();
    return new Node(c);
  }

  private static Map<String, Node> makeIosRouters(String... hostnames) {
    return Arrays.asList(hostnames)
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(hostname -> hostname, hostname -> makeIosRouter(hostname)));
  }

  private static VirtualRouter makeIosVirtualRouter(String hostname) {
    Node n = makeIosRouter(hostname);
    return n._virtualRouters.get(Configuration.DEFAULT_VRF_NAME);
  }

  @Test
  public void computeBgpAdvertisementsSentToOutsideNoBgp() {
    VirtualRouter virtualRouter = createEmptyVirtualRouter("testVirtualRouter");
    Map<Ip, Set<String>> ipOwners =
        Collections.singletonMap(SRC_IP, Sets.newHashSet("testVirtualRouter"));

    // checking that no bgp advertisements are sent if the vrf has no BGP process
    assertThat(virtualRouter.computeBgpAdvertisementsToOutside(ipOwners), equalTo(0));
  }

  @Test
  public void computeBgpAdvertisementsSentToOutsideIgp() {
    VirtualRouter virtualRouter = createEmptyVirtualRouter(TEST_NODE_NAME);

    Configuration neighborConfig =
        BatfishTestUtils.createTestConfiguration("neighborNode", FORMAT, "neighborInterface");

    NetworkFactory nf = new NetworkFactory();
    RoutingPolicy.Builder rpb = nf.routingPolicyBuilder();
    RoutingPolicy exportPolicy =
        rpb.setOwner(virtualRouter._c)
            .setStatements(
                ImmutableList.of(
                    new SetOrigin(new LiteralOrigin(OriginType.INCOMPLETE, null)),
                    Statements.ExitAccept.toStaticStatement()))
            .build();

    BgpProcess.Builder bpBuilder = nf.bgpProcessBuilder();
    bpBuilder.setVrf(virtualRouter._vrf);
    BgpProcess bgpProcess = bpBuilder.build();
    bgpProcess.setRouterId(SRC_IP);

    createBgpNeighbor(bgpProcess, neighborConfig, SRC_IP, DEST_IP, 1, 2, exportPolicy.getName());

    OspfIntraAreaRoute ospfIntraAreaRoute = new OspfIntraAreaRoute(TEST_NETWORK, null, 100, 30, 1);
    virtualRouter.initRibs();
    virtualRouter._mainRib.mergeRoute(ospfIntraAreaRoute);

    Map<Ip, Set<String>> ipOwners = ImmutableMap.of(SRC_IP, ImmutableSet.of(TEST_NODE_NAME));
    int bgpAdvertisements = virtualRouter.computeBgpAdvertisementsToOutside(ipOwners);

    // checking number of bgp advertisements
    assertThat(bgpAdvertisements, equalTo(1));

    // checking the attributes of the bgp advertisement
    Iterator<BgpAdvertisement> iterator = virtualRouter._sentBgpAdvertisements.iterator();
    while (iterator.hasNext()) {
      BgpAdvertisement bgpAdvertisement = iterator.next();
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasDestinationIp(DEST_IP));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasNetwork(TEST_NETWORK));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasOriginatorIp(SRC_IP));
      assertThat(
          bgpAdvertisement, BgpAdvertisementMatchUtils.hasType(BgpAdvertisementType.EBGP_SENT));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasSourceIp(SRC_IP));
    }
  }

  @Test
  public void computeBgpAdvertisementsSTOIbgpNeighborEbgpBestPath() {
    VirtualRouter virtualRouter = createEmptyVirtualRouter(TEST_NODE_NAME);
    Configuration neighborConfig =
        BatfishTestUtils.createTestConfiguration("neighborNode", FORMAT, "neighborInterface");

    NetworkFactory nf = new NetworkFactory();
    RoutingPolicy.Builder rpb = nf.routingPolicyBuilder();
    RoutingPolicy exportPolicy =
        rpb.setOwner(virtualRouter._c)
            .setStatements(ImmutableList.of(Statements.ExitAccept.toStaticStatement()))
            .build();

    BgpProcess.Builder bpBuilder = nf.bgpProcessBuilder();
    bpBuilder.setVrf(virtualRouter._vrf);
    BgpProcess bgpProcess = bpBuilder.build();
    bgpProcess.setRouterId(SRC_IP);

    BgpNeighbor ibgpNeighbor =
        createBgpNeighbor(
            bgpProcess, neighborConfig, SRC_IP, DEST_IP, 1, 1, exportPolicy.getName());
    ibgpNeighbor.setAdvertiseExternal(true);
    ibgpNeighbor.setAdditionalPathsSend(true);
    ibgpNeighbor.setAdditionalPathsSelectAll(true);

    BgpRoute.Builder bgpBuilder =
        new BgpRoute.Builder()
            .setNetwork(TEST_NETWORK)
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(SRC_IP)
            .setNextHopIp(TEST_NEXT_HOP_IP);

    virtualRouter._ebgpBestPathRib.mergeRoute(bgpBuilder.build());

    Map<Ip, Set<String>> ipOwners = ImmutableMap.of(SRC_IP, ImmutableSet.of(TEST_NODE_NAME));
    int bgpAdvertisements = virtualRouter.computeBgpAdvertisementsToOutside(ipOwners);

    // checking number of bgp advertisements
    assertThat(bgpAdvertisements, equalTo(1));

    // checking the attributes of the bgp advertisement
    Iterator<BgpAdvertisement> iterator = virtualRouter._sentBgpAdvertisements.iterator();
    while (iterator.hasNext()) {
      BgpAdvertisement bgpAdvertisement = iterator.next();
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasDestinationIp(DEST_IP));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasNetwork(TEST_NETWORK));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasOriginatorIp(SRC_IP));
      assertThat(
          bgpAdvertisement, BgpAdvertisementMatchUtils.hasType(BgpAdvertisementType.IBGP_SENT));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasSourceIp(SRC_IP));
    }
  }

  @Test
  public void computeBgpAdvertisementsSTOIbgpNeighborBgpMultiPath() {
    VirtualRouter virtualRouter = createEmptyVirtualRouter(TEST_NODE_NAME);
    Configuration neighborConfig =
        BatfishTestUtils.createTestConfiguration("neighborNode", FORMAT, "neighborInterface");

    NetworkFactory nf = new NetworkFactory();
    RoutingPolicy.Builder rpb = nf.routingPolicyBuilder();
    RoutingPolicy exportPolicy =
        rpb.setOwner(virtualRouter._c)
            .setStatements(
                ImmutableList.of(
                    new SetOrigin(new LiteralOrigin(OriginType.INCOMPLETE, null)),
                    Statements.ExitAccept.toStaticStatement()))
            .build();

    BgpProcess.Builder bpBuilder = nf.bgpProcessBuilder();
    bpBuilder.setVrf(virtualRouter._vrf);
    BgpProcess bgpProcess = bpBuilder.build();
    bgpProcess.setRouterId(SRC_IP);

    BgpNeighbor ibgpNeighbor =
        createBgpNeighbor(
            bgpProcess, neighborConfig, SRC_IP, DEST_IP, 1, 1, exportPolicy.getName());
    ibgpNeighbor.setAdvertiseExternal(true);
    ibgpNeighbor.setAdditionalPathsSend(true);
    ibgpNeighbor.setAdditionalPathsSelectAll(true);

    BgpRoute.Builder bgpBuilder =
        new BgpRoute.Builder()
            .setNetwork(TEST_NETWORK)
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(SRC_IP)
            .setNextHopIp(TEST_NEXT_HOP_IP);

    virtualRouter._bgpMultipathRib.mergeRoute(bgpBuilder.build());

    Map<Ip, Set<String>> ipOwners = ImmutableMap.of(SRC_IP, ImmutableSet.of(TEST_NODE_NAME));
    int bgpAdvertisements = virtualRouter.computeBgpAdvertisementsToOutside(ipOwners);

    // checking number of bgp advertisements
    assertThat(bgpAdvertisements, equalTo(1));

    // checking the attributes of the bgp advertisement
    Iterator<BgpAdvertisement> iterator = virtualRouter._sentBgpAdvertisements.iterator();
    while (iterator.hasNext()) {
      BgpAdvertisement bgpAdvertisement = iterator.next();
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasDestinationIp(DEST_IP));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasNetwork(TEST_NETWORK));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasOriginatorIp(SRC_IP));
      assertThat(
          bgpAdvertisement, BgpAdvertisementMatchUtils.hasType(BgpAdvertisementType.IBGP_SENT));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasSourceIp(SRC_IP));
    }
  }

  @Test
  public void computeBgpAdvertisementsSTOEbgpNeighbor() {

    VirtualRouter virtualRouter = createEmptyVirtualRouter(TEST_NODE_NAME);
    Configuration neighborConfig =
        BatfishTestUtils.createTestConfiguration("neighborNode", FORMAT, "neighborInterface");

    NetworkFactory nf = new NetworkFactory();
    RoutingPolicy.Builder rpb = nf.routingPolicyBuilder();
    RoutingPolicy exportPolicy =
        rpb.setOwner(virtualRouter._c)
            .setStatements(ImmutableList.of(Statements.ExitAccept.toStaticStatement()))
            .build();

    BgpProcess.Builder bpBuilder = nf.bgpProcessBuilder();
    bpBuilder.setVrf(virtualRouter._vrf);
    BgpProcess bgpProcess = bpBuilder.build();
    bgpProcess.setRouterId(SRC_IP);

    BgpNeighbor ebgpNeighbor =
        createBgpNeighbor(
            bgpProcess, neighborConfig, SRC_IP, DEST_IP, 1, 2, exportPolicy.getName());
    ebgpNeighbor.setAdvertiseInactive(true);

    BgpRoute.Builder bgpBuilder =
        new BgpRoute.Builder()
            .setNetwork(TEST_NETWORK)
            .setReceivedFromRouteReflectorClient(true)
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(SRC_IP);

    virtualRouter._bgpBestPathRib.mergeRoute(bgpBuilder.build());

    Map<Ip, Set<String>> ipOwners = ImmutableMap.of(SRC_IP, ImmutableSet.of(TEST_NODE_NAME));
    int bgpAdvertisements = virtualRouter.computeBgpAdvertisementsToOutside(ipOwners);

    // checking number of bgp advertisements
    assertThat(bgpAdvertisements, equalTo(1));

    // checking the attributes of the bgp advertisement
    Iterator<BgpAdvertisement> iterator = virtualRouter._sentBgpAdvertisements.iterator();
    while (iterator.hasNext()) {
      BgpAdvertisement bgpAdvertisement = iterator.next();
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasDestinationIp(DEST_IP));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasNetwork(TEST_NETWORK));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasOriginatorIp(SRC_IP));
      assertThat(
          bgpAdvertisement, BgpAdvertisementMatchUtils.hasType(BgpAdvertisementType.EBGP_SENT));
      assertThat(bgpAdvertisement, BgpAdvertisementMatchUtils.hasSourceIp(SRC_IP));
    }
  }

  @Test
  public void computeBgpAdvertisementsSTOIbgpNeighborReject() {
    VirtualRouter virtualRouter = createEmptyVirtualRouter(TEST_NODE_NAME);
    Configuration neighborConfig =
        BatfishTestUtils.createTestConfiguration("neighborNode", FORMAT, "neighborInterface");

    NetworkFactory nf = new NetworkFactory();
    RoutingPolicy.Builder rpb = nf.routingPolicyBuilder();

    RoutingPolicy exportPolicy =
        rpb.setOwner(virtualRouter._c)
            .setStatements(ImmutableList.of(Statements.ExitReject.toStaticStatement()))
            .build();

    BgpProcess.Builder bpBuilder = nf.bgpProcessBuilder();
    bpBuilder.setVrf(virtualRouter._vrf);
    BgpProcess bgpProcess = bpBuilder.build();
    bgpProcess.setRouterId(SRC_IP);

    BgpNeighbor ibgpNeighbor =
        createBgpNeighbor(
            bgpProcess, neighborConfig, SRC_IP, DEST_IP, 1, 1, exportPolicy.getName());

    ibgpNeighbor.setAdvertiseExternal(true);
    ibgpNeighbor.setAdditionalPathsSend(true);
    ibgpNeighbor.setAdditionalPathsSelectAll(true);

    BgpRoute.Builder bgpBuilder1 =
        new BgpRoute.Builder()
            .setNetwork(TEST_NETWORK)
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(SRC_IP)
            .setNextHopIp(TEST_NEXT_HOP_IP);

    BgpRoute.Builder bgpBuilder2 =
        new BgpRoute.Builder()
            .setNetwork(TEST_NETWORK)
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(SRC_IP)
            .setNextHopIp(TEST_NEXT_HOP_IP);

    virtualRouter._ebgpBestPathRib.mergeRoute(bgpBuilder1.build());
    virtualRouter._bgpMultipathRib.mergeRoute(bgpBuilder2.build());

    Map<Ip, Set<String>> ipOwners = ImmutableMap.of(SRC_IP, ImmutableSet.of(TEST_NODE_NAME));
    int numAdvertisements = virtualRouter.computeBgpAdvertisementsToOutside(ipOwners);

    // number of BGP advertisements should be zero given the reject all export policy
    assertThat(numAdvertisements, equalTo(0));
  }

  /**
   * Creates a {@link BgpNeighbor} with given attributes and adds them to a {@link BgpProcess}
   *
   * @param bgpProcess {@link BgpProcess} to which the neighbor will be added
   * @param owner Owner {@link Configuration} of the Neighbor
   * @param localIp Local {@link Ip} of the neighbor
   * @param remoteIp Peer Address of the neighbor
   * @param localAs {@link BgpNeighbor#_localAs} for the neighbor
   * @param remoteAs {@link BgpNeighbor#_remoteAs} for the neighbor
   * @param exportPolicy Name of the routing policy for this {@link BgpNeighbor}
   * @return new instance of {@link BgpNeighbor}
   */
  private static BgpNeighbor createBgpNeighbor(
      BgpProcess bgpProcess,
      Configuration owner,
      Ip localIp,
      Ip remoteIp,
      int localAs,
      int remoteAs,
      String exportPolicy) {
    NetworkFactory nf = new NetworkFactory();
    BgpNeighbor.Builder bb =
        nf.bgpNeighborBuilder()
            .setOwner(owner)
            .setPeerAddress(remoteIp)
            .setLocalAs(localAs)
            .setRemoteAs(remoteAs)
            .setLocalIp(localIp)
            .setBgpProcess(bgpProcess);
    BgpNeighbor neighbor = bb.build();
    neighbor.setExportPolicy(exportPolicy);
    return neighbor;
  }

  /**
   * Creates an empty {@link VirtualRouter} along with creating owner {@link Configuration} and
   * {@link Vrf}
   *
   * @param nodeName Node name of the owner {@link Configuration}
   * @return new instance of {@link VirtualRouter}
   */
  private static VirtualRouter createEmptyVirtualRouter(String nodeName) {
    NetworkFactory nf = new NetworkFactory();
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
    Prefix ospfInterAreaRoutePrefix = new Prefix("1.1.1.1/24");
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
            route, new Prefix("2.0.0.0/8"), 4L, definedArea, true),
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
    addInterfaces(vr._c, exampleInterfacePrefixes);
    vr.initRibs();

    // Test
    vr.initConnectedRib();

    // Assert that all interface prefixes have been processed
    assertThat(
        vr._connectedRib.getRoutes(),
        Matchers.containsInAnyOrder(
            exampleInterfacePrefixes
                .entrySet()
                .stream()
                .map(e -> new ConnectedRoute(e.getValue(), e.getKey()))
                .collect(Collectors.toList())
                .toArray(new ConnectedRoute[] {})));
  }

  @Test
  public void testInitRibsEmpty() {
    VirtualRouter vr = makeIosVirtualRouter(null);

    // We expect the router to have the following RIBs and all of them are empty
    vr.initRibs();

    // Simple RIBs
    assertThat(vr._connectedRib.getRoutes(), is(emptyIterableOf(ConnectedRoute.class)));
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
    assertThat(vr._baseIbgpRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ibgpBestPathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ibgpMultipathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ibgpStagingRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    // Ebgp
    assertThat(vr._baseEbgpRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ebgpBestPathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ebgpMultipathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._ebgpStagingRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    // Combined bgp
    assertThat(vr._bgpBestPathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));
    assertThat(vr._bgpMultipathRib.getRoutes(), is(emptyIterableOf(BgpRoute.class)));

    // Main RIB
    assertThat(vr._mainRib.getRoutes(), is(emptyIterableOf(AbstractRoute.class)));

    // Prev Ribs are expected to be null
    assertThat(vr._prevOspfExternalType1Rib, is(nullValue()));
    assertThat(vr._prevOspfExternalType2Rib, is(nullValue()));

    assertThat(vr._prevBgpBestPathRib, is(nullValue()));
    assertThat(vr._prevEbgpBestPathRib, is(nullValue()));
    assertThat(vr._prevIbgpBestPathRib, is(nullValue()));

    assertThat(vr._prevBgpMultipathRib, is(nullValue()));
    assertThat(vr._prevEbgpMultipathRib, is(nullValue()));
    assertThat(vr._prevIbgpMultipathRib, is(nullValue()));

    assertThat(vr._prevMainRib, is(nullValue()));
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
                    e -> e.getValue()._virtualRouters.get(Configuration.DEFAULT_VRF_NAME)));
    VirtualRouter testRouter = routers.get(testRouterName);
    VirtualRouter exportingRouter = routers.get(exportingRouterName);
    testRouter.initRibs();
    exportingRouter.initRibs();
    addInterfaces(testRouter._c, exampleInterfacePrefixes);
    addInterfaces(
        exportingRouter._c,
        ImmutableMap.of(exportingRouterInterfaceName, new Prefix("10.4.0.0/16")));
    int adminCost =
        RoutingProtocol.OSPF.getDefaultAdministrativeCost(testRouter._c.getConfigurationFormat());

    Prefix prefix = new Prefix("7.7.7.0/24");
    OspfIntraAreaRoute route = new OspfIntraAreaRoute(prefix, new Ip("7.7.1.1"), adminCost, 20, 1);
    exportingRouter._ospfIntraAreaRib.mergeRoute(route);

    // Set interaces on router 1 to be OSPF passive
    testRouter._c.getInterfaces().forEach((name, iface) -> iface.setActive(false));

    // Test 1
    testRouter.propagateOspfInternalRoutesFromNeighbor(
        testRouter._vrf.getOspfProcess(),
        nodes.get("R2"),
        testRouter._c.getInterfaces().firstEntry().getValue(),
        exportingRouter._c.getInterfaces().get(exportingRouterInterfaceName),
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
        testRouter._vrf.getOspfProcess(),
        nodes.get("R2"),
        testRouter._c.getInterfaces().firstEntry().getValue(),
        exportingRouter._c.getInterfaces().get(exportingRouterInterfaceName),
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
    addInterfaces(vr._c, exampleInterfacePrefixes);
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
            exampleInterfacePrefixes
                .values()
                .stream()
                .map(
                    p ->
                        new RipInternalRoute(
                            p,
                            null,
                            RoutingProtocol.RIP.getDefaultAdministrativeCost(
                                vr._c.getConfigurationFormat()),
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
    Prefix prefix = new Prefix("7.7.7.0/24");
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

  @Test
  public void testStaticRibInit() {
    VirtualRouter vr = makeIosVirtualRouter(null);
    vr.initRibs();
    SortedSet<StaticRoute> routeSet =
        ImmutableSortedSet.of(new StaticRoute(new Prefix("1.1.1.1/32"), Ip.ZERO, null, 1, 0));
    vr._vrf.setStaticRoutes(routeSet);

    // Test
    vr.initStaticRib();

    assertThat(vr._staticRib.getRoutes(), equalTo(routeSet));
  }
}

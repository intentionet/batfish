package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.IpAccessListLine.REJECT_ALL;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ValueGraph;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.plugin.DataPlanePlugin.ComputeDataPlaneResult;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.GeneratedRoute.Builder;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.bgp.BgpTopologyUtils;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.datamodel.isis.IsisInterfaceLevelSettings;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.isis.IsisLevelSettings;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.SetDefaultPolicy;
import org.batfish.dataplane.TracerouteEngineImpl;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link IncrementalDataPlanePlugin}. */
public class IncrementalDataPlanePluginTest {

  private static final String TESTRIGS_PREFIX = "org/batfish/grammar/cisco/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static final String CORE_NAME = "core";
  private NetworkFactory _nf;
  private Configuration.Builder _cb;
  private Interface.Builder _ib;
  private BgpActivePeerConfig.Builder _nb;
  private BgpProcess.Builder _pb;
  private Vrf.Builder _vb;
  private RoutingPolicy.Builder _epb;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder();
    _ib = _nf.interfaceBuilder();
    _nb = _nf.bgpNeighborBuilder();
    _pb = _nf.bgpProcessBuilder();
    _vb = _nf.vrfBuilder();
    _epb = _nf.routingPolicyBuilder();
  }

  @Test(timeout = 5000)
  public void testComputeFixedPoint() throws IOException {
    SortedMap<String, Configuration> configurations = new TreeMap<>();
    // creating configurations with no vrfs
    configurations.put(
        "h1", BatfishTestUtils.createTestConfiguration("h1", ConfigurationFormat.HOST, "eth0"));
    configurations.put(
        "h2", BatfishTestUtils.createTestConfiguration("h2", ConfigurationFormat.HOST, "e0"));
    Batfish batfish = BatfishTestUtils.getBatfish(configurations, _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    // Test that compute Data Plane finishes in a finite time
    dataPlanePlugin.computeDataPlane();
  }

  private SortedMap<String, Configuration> generateNetworkWithDuplicates() {
    Ip coreId = Ip.parse("1.1.1.1");
    Ip neighborId1 = Ip.parse("1.1.1.9");
    Ip neighborId2 = Ip.parse("1.1.1.2");
    final int interfcePrefixBits = 30;
    _vb.setName(DEFAULT_VRF_NAME);

    /*
     * Setup as follows:
     * 1.1.1.9               1.1.1.1            1.1.1.2
     * n1+---------x---------+core+---------------+n2
     * Core only has route to n2, because of interface masks
     */

    _epb.setStatements(ImmutableList.of(new SetDefaultPolicy("DEF")));
    Configuration core =
        _cb.setHostname(CORE_NAME).setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();

    Vrf corevrf = _vb.setOwner(core).build();
    _ib.setOwner(core).setVrf(corevrf);
    _ib.setAddress(new InterfaceAddress(coreId, interfcePrefixBits)).build();
    BgpProcess coreProc = _pb.setRouterId(coreId).setVrf(corevrf).build();
    _nb.setBgpProcess(coreProc)
        .setRemoteAs(1L)
        .setLocalAs(1L)
        .setLocalIp(coreId)
        .setPeerAddress(neighborId1)
        .setExportPolicy(_epb.setOwner(core).build().getName())
        .build();
    _nb.setRemoteAs(1L).setLocalAs(1L).setLocalIp(coreId).setPeerAddress(neighborId2).build();

    Configuration n1 = _cb.setHostname("n1").build();
    Vrf n1Vrf = _vb.setOwner(n1).build();
    _ib.setOwner(n1).setVrf(n1Vrf);
    _ib.setAddress(new InterfaceAddress(neighborId1, interfcePrefixBits)).build();
    BgpProcess n1Proc = _pb.setRouterId(neighborId1).setVrf(n1Vrf).build();
    _nb.setBgpProcess(n1Proc)
        .setRemoteAs(1L)
        .setLocalAs(1L)
        .setLocalIp(neighborId1)
        .setPeerAddress(coreId)
        .setExportPolicy(_epb.setOwner(n1).build().getName())
        .build();

    Configuration n2 = _cb.setHostname("n2").build();
    Vrf n2Vrf = _vb.setOwner(n2).build();
    _ib.setOwner(n2).setVrf(n2Vrf);
    _ib.setAddress(new InterfaceAddress(neighborId2, interfcePrefixBits)).build();
    BgpProcess n2Proc = _pb.setRouterId(neighborId2).setVrf(n2Vrf).build();
    _nb.setBgpProcess(n2Proc)
        .setRemoteAs(1L)
        .setLocalAs(1L)
        .setLocalIp(neighborId2)
        .setPeerAddress(coreId)
        .setExportPolicy(_epb.setOwner(n2).build().getName())
        .build();

    return ImmutableSortedMap.of(CORE_NAME, core, "n1", n1, "n2", n2);
  }

  @Test
  public void testBgpOscillation() throws IOException {
    String testrigName = "bgp-oscillation";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();

    // Really just test that no exception is thrown
    dataPlanePlugin.computeDataPlane();
  }

  @Test
  public void testEbgpAcceptSameNeighborID() throws IOException {
    String testrigName = "ebgp-accept-routerid-match";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    IncrementalDataPlanePlugin dataPlanePlugin = new IncrementalDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);
    ComputeDataPlaneResult dp = dataPlanePlugin.computeDataPlane();
    SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs =
        dp._dataPlane.getRibs();

    Set<AbstractRoute> r1Routes = ribs.get("r1").get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> r3Routes = ribs.get("r3").get(DEFAULT_VRF_NAME).getRoutes();
    Set<Prefix> r1Prefixes =
        r1Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Set<Prefix> r3Prefixes =
        r3Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Prefix r1Loopback0Prefix = Prefix.parse("1.0.0.1/32");
    Prefix r3Loopback0Prefix = Prefix.parse("3.0.0.3/32");

    // Ensure that r3loopback was accepted by r1
    assertThat(r3Loopback0Prefix, in(r1Prefixes));
    // Check the other direction (r1loopback is accepted by r3)
    assertThat(r1Loopback0Prefix, in(r3Prefixes));
  }

  @Test
  public void testIbgpRejectOwnAs() throws IOException {
    String testrigName = "ibgp-reject-own-as";
    List<String> configurationNames = ImmutableList.of("r1", "r2a", "r2b");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    ComputeDataPlaneResult dp = dataPlanePlugin.computeDataPlane();
    SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs =
        dp._dataPlane.getRibs();
    Set<AbstractRoute> r2aRoutes = ribs.get("r2a").get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> r2bRoutes = ribs.get("r2b").get(DEFAULT_VRF_NAME).getRoutes();
    Set<Prefix> r2aPrefixes =
        r2aRoutes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Set<Prefix> r2bPrefixes =
        r2bRoutes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Prefix r1Loopback0Prefix = Prefix.parse("1.0.0.1/32");
    Prefix r1Loopback1Prefix = Prefix.parse("1.0.0.2/32");
    assertTrue(r2aPrefixes.contains(r1Loopback0Prefix));
    assertTrue(r2aPrefixes.contains(r1Loopback1Prefix));
    /*
     * 1.0.0.2/32 should be accepted r2b as a normal iBGP route forwarded from r1.
     */
    assertTrue(r2bPrefixes.contains(r1Loopback1Prefix));
    /*
     * 1.0.0.1/32 should be rejected by r2b since it already contains AS#2 in its AS-path due to
     * r2a prepending 2 in the matching route-map clause.
     */
    assertFalse(r2bPrefixes.contains(r1Loopback0Prefix));
  }

  @Test
  public void testIbgpRejectSameNeighborID() throws IOException {
    String testrigName = "ibgp-reject-routerid-match";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3", "r4");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    ComputeDataPlaneResult dp = dataPlanePlugin.computeDataPlane();
    SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs =
        dp._dataPlane.getRibs();

    Set<AbstractRoute> r2Routes = ribs.get("r2").get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> r3Routes = ribs.get("r3").get(DEFAULT_VRF_NAME).getRoutes();
    Set<Prefix> r2Prefixes =
        r2Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Set<Prefix> r3Prefixes =
        r3Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    // 9.9.9.9/32 is the prefix we test with
    Prefix r1AdvertisedPrefix = Prefix.parse("9.9.9.9/32");

    // Ensure that the prefix is accepted by r2, because router ids are different
    assertThat(r1AdvertisedPrefix, in(r2Prefixes));
    // Ensure that the prefix is rejected by r3, because router ids are the same
    assertThat(r1AdvertisedPrefix, not(in(r3Prefixes)));
  }

  @Test
  public void testIosRtStaticMatchesBdp() throws IOException {
    String testrigResourcePrefix = TESTRIGS_PREFIX + "ios-rt-static-ad";
    List<String> configurationNames = ImmutableList.of("r1");
    List<String> routingTableNames = ImmutableList.of("r1");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(testrigResourcePrefix, configurationNames)
                .setRoutingTablesText(testrigResourcePrefix, routingTableNames)
                .build(),
            _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    ComputeDataPlaneResult dp = dataPlanePlugin.computeDataPlane();
    SortedMap<String, RoutesByVrf> environmentRoutes = batfish.loadEnvironmentRoutingTables();
    SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs =
        dp._dataPlane.getRibs();
    Prefix staticRoutePrefix = Prefix.parse("10.0.0.0/8");
    Set<AbstractRoute> r1BdpRoutes = ribs.get("r1").get(DEFAULT_VRF_NAME).getRoutes();
    AbstractRoute r1BdpRoute =
        r1BdpRoutes.stream()
            .filter(r -> r.getNetwork().equals(staticRoutePrefix))
            .findFirst()
            .get();
    SortedSet<Route> r1EnvironmentRoutes = environmentRoutes.get("r1").get(DEFAULT_VRF_NAME);
    Route r1EnvironmentRoute =
        r1EnvironmentRoutes.stream()
            .filter(r -> r.getNetwork().equals(staticRoutePrefix))
            .findFirst()
            .get();
    assertThat(
        r1BdpRoute.getAdministrativeCost(), equalTo(r1EnvironmentRoute.getAdministrativeCost()));
    assertThat(r1BdpRoute.getMetric(), equalTo(r1EnvironmentRoute.getMetric()));
    assertThat(r1BdpRoute.getProtocol(), equalTo(r1EnvironmentRoute.getProtocol()));
  }

  @Test
  public void testStaticInterfaceRoutesWithoutEdge() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf vrf = nf.vrfBuilder().setOwner(c).setName(DEFAULT_VRF_NAME).build();
    Interface i =
        nf.interfaceBuilder()
            .setOwner(c)
            .setVrf(vrf)
            .setAddress(new InterfaceAddress("10.0.0.0/24"))
            .build();
    StaticRoute srBoth =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("10.0.1.0/24"))
            .setNextHopInterface(i.getName())
            .setNextHopIp(Ip.parse("10.0.0.1"))
            .setAdministrativeCost(1)
            .build();
    vrf.getStaticRoutes().add(srBoth);
    StaticRoute srJustInterface =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("10.0.2.0/24"))
            .setNextHopInterface(i.getName())
            .setAdministrativeCost(1)
            .build();
    vrf.getStaticRoutes().add(srJustInterface);
    IncrementalBdpEngine engine =
        new IncrementalBdpEngine(
            // TODO: parametrize settings with different schedules
            new IncrementalDataPlaneSettings(),
            new BatfishLogger(BatfishLogger.LEVELSTR_DEBUG, false));
    Topology topology = new Topology(Collections.emptySortedSet());
    ComputeDataPlaneResult dp =
        engine.computeDataPlane(
            ImmutableMap.of(c.getHostname(), c),
            TopologyContext.builder().setLayer3Topology(topology).build(),
            Collections.emptySet());

    // generating fibs should not crash
    dp._dataPlane.getFibs();
  }

  @Test
  public void testBgpNeighborReachability() throws IOException {
    // Only connect one neighbor (n2) to core router
    SortedMap<String, Configuration> configs = generateNetworkWithDuplicates();

    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    ComputeDataPlaneResult result = dataPlanePlugin.computeDataPlane();
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        result._topologies.getBgpTopology().getGraph();

    // N2 has proper neighbor relationship
    Set<Entry<Prefix, BgpActivePeerConfig>> n2Neighbors =
        configs
            .get("n2")
            .getVrfs()
            .get(DEFAULT_VRF_NAME)
            .getBgpProcess()
            .getActiveNeighbors()
            .entrySet();
    Entry<Prefix, BgpActivePeerConfig> e = n2Neighbors.iterator().next();
    assertThat(n2Neighbors, hasSize(1));
    assertThat(
        bgpTopology.degree(new BgpPeerConfigId("n2", DEFAULT_VRF_NAME, e.getKey(), false)),
        equalTo(2));

    // N1 does not have a full session established, because it's not reachable
    Collection<BgpActivePeerConfig> n1Neighbors =
        configs
            .get("n1")
            .getVrfs()
            .get(DEFAULT_VRF_NAME)
            .getBgpProcess()
            .getActiveNeighbors()
            .values();
    e = n2Neighbors.iterator().next();
    assertThat(n1Neighbors, hasSize(1));
    assertThat(
        bgpTopology.degree(new BgpPeerConfigId("n1", DEFAULT_VRF_NAME, e.getKey(), false)),
        equalTo(0));
  }

  @Test
  public void testGeneratedRoutesInMainRib() throws IOException {
    Configuration n1 =
        _nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("n1")
            .build();
    Vrf vrf = _vb.setOwner(n1).build();

    // Create generated route
    Prefix genRoutePrefix = Prefix.parse("1.1.1.1/32");
    Builder grb = GeneratedRoute.builder();
    GeneratedRoute route = grb.setNetwork(genRoutePrefix).setDiscard(true).build();
    vrf.setGeneratedRoutes(ImmutableSortedSet.of(route));

    Batfish batfish =
        BatfishTestUtils.getBatfish(ImmutableSortedMap.of(n1.getHostname(), n1), _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    DataPlane dp = dataPlanePlugin.computeDataPlane()._dataPlane;

    assertThat(
        dp.getRibs().get(n1.getHostname()).get(vrf.getName()).getRoutes(),
        hasItem(hasPrefix(genRoutePrefix)));
  }

  @Test
  public void testEbgpSinglehopSuccess() throws IOException {
    SortedMap<String, Configuration> configs = generateNetworkWithThreeHops(false, false);

    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    ComputeDataPlaneResult result = dataPlanePlugin.computeDataPlane();
    DataPlane dp = result._dataPlane;

    BgpPeerConfigId initiator =
        new BgpPeerConfigId("node1", "~Vrf_0~", Prefix.parse("1.0.0.0/32"), false);
    BgpPeerConfigId listener =
        new BgpPeerConfigId("node2", "~Vrf_1~", Prefix.parse("1.0.0.1/32"), false);

    BgpActivePeerConfig source =
        BgpActivePeerConfig.builder()
            .setLocalIp(Ip.parse("1.0.0.0"))
            .setPeerAddress(Ip.parse("1.0.0.1"))
            .setEbgpMultihop(false)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .build();

    // the neighbor should be reachable because it is only one hop away from the initiator
    assertTrue(
        BgpTopologyUtils.isReachableBgpNeighbor(
            initiator,
            listener,
            source,
            new TracerouteEngineImpl(dp, result._topologies.getLayer3Topology())));
  }

  @Test
  public void testEbgpSinglehopFailure() throws IOException {
    SortedMap<String, Configuration> configs = generateNetworkWithThreeHops(false, false);

    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    ComputeDataPlaneResult result = dataPlanePlugin.computeDataPlane();
    DataPlane dp = result._dataPlane;

    BgpPeerConfigId initiator =
        new BgpPeerConfigId("node1", "~Vrf_0~", Prefix.parse("1.0.0.0/32"), false);
    BgpPeerConfigId listener =
        new BgpPeerConfigId("node3", "~Vrf_2~", Prefix.parse("1.0.0.3/32"), false);

    BgpActivePeerConfig source =
        BgpActivePeerConfig.builder()
            .setLocalIp(Ip.parse("1.0.0.0"))
            .setPeerAddress(Ip.parse("1.0.0.3"))
            .setEbgpMultihop(false)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .build();

    // the neighbor should be not be reachable because it is two hops away from the initiator
    assertFalse(
        BgpTopologyUtils.isReachableBgpNeighbor(
            initiator,
            listener,
            source,
            new TracerouteEngineImpl(dp, result._topologies.getLayer3Topology())));
  }

  @Test
  public void testEbgpMultihopSuccess() throws IOException {
    SortedMap<String, Configuration> configs = generateNetworkWithThreeHops(false, false);

    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    ComputeDataPlaneResult result = dataPlanePlugin.computeDataPlane();
    DataPlane dp = result._dataPlane;

    BgpPeerConfigId initiator =
        new BgpPeerConfigId("node1", "~Vrf_0~", Prefix.parse("1.0.0.0/32"), false);
    BgpPeerConfigId listener =
        new BgpPeerConfigId("node3", "~Vrf_2~", Prefix.parse("1.0.0.3/32"), false);

    BgpActivePeerConfig source =
        BgpActivePeerConfig.builder()
            .setLocalIp(Ip.parse("1.0.0.0"))
            .setPeerAddress(Ip.parse("1.0.0.3"))
            .setEbgpMultihop(true)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .build();

    // the neighbor should be reachable because multi-hops are allowed
    assertTrue(
        BgpTopologyUtils.isReachableBgpNeighbor(
            initiator,
            listener,
            source,
            new TracerouteEngineImpl(dp, result._topologies.getLayer3Topology())));
  }

  @Test
  public void testEbgpMultihopFailureWithAcl() throws IOException {
    // use a network with a deny all ACL on node 3
    SortedMap<String, Configuration> configs = generateNetworkWithThreeHops(true, false);

    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    ComputeDataPlaneResult result = dataPlanePlugin.computeDataPlane();
    DataPlane dp = result._dataPlane;

    BgpPeerConfigId initiator =
        new BgpPeerConfigId("node1", "~Vrf_0~", Prefix.parse("1.0.0.0/32"), false);
    BgpPeerConfigId listener =
        new BgpPeerConfigId("node3", "~Vrf_2~", Prefix.parse("1.0.0.3/32"), false);

    BgpActivePeerConfig source =
        BgpActivePeerConfig.builder()
            .setLocalIp(Ip.parse("1.0.0.0"))
            .setPeerAddress(Ip.parse("1.0.0.3"))
            .setEbgpMultihop(true)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .build();

    // the neighbor should not be reachable even though multihops are allowed as traceroute would be
    // denied in on node 3
    assertFalse(
        BgpTopologyUtils.isReachableBgpNeighbor(
            initiator,
            listener,
            source,
            new TracerouteEngineImpl(dp, result._topologies.getLayer3Topology())));
  }

  @Test
  public void testEbgpWithAclPermitEstablished() throws IOException {
    // use a network with an allow established connection ACL on node1
    SortedMap<String, Configuration> configs = generateNetworkWithThreeHops(false, true);

    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    ComputeDataPlaneResult result = dataPlanePlugin.computeDataPlane();
    DataPlane dp = result._dataPlane;

    BgpPeerConfigId initiator =
        new BgpPeerConfigId("node1", "~Vrf_0~", Prefix.parse("1.0.0.0/32"), false);
    BgpPeerConfigId listener =
        new BgpPeerConfigId("node3", "~Vrf_2~", Prefix.parse("1.0.0.3/32"), false);

    BgpActivePeerConfig source =
        BgpActivePeerConfig.builder()
            .setLocalIp(Ip.parse("1.0.0.0"))
            .setPeerAddress(Ip.parse("1.0.0.3"))
            .setEbgpMultihop(true)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .build();

    // neighbor should be reachable because ACL allows established connection back into node1 and
    // allows everything out
    assertTrue(
        BgpTopologyUtils.isReachableBgpNeighbor(
            initiator,
            listener,
            source,
            new TracerouteEngineImpl(dp, result._topologies.getLayer3Topology())));
  }

  /**
   * Generates configurations for a three node network with connectivity as shown in the diagram
   * below. Also adds static routes from node 1 to node 3 and back from node 3 to node 1
   *
   * @param denyIntoHop3 If true, add an incoming ACL on node3 that blocks all traffic
   * @param allowOnlyEstablishedIntoHop1 If true, add an ACL on node1 which permits only established
   *     TCP connections back into node1
   * @return {@link SortedMap} of generated configuration names and corresponding {@link
   *     Configuration}s
   */

  /* +-----------+                       +-------------+                   +--------------+
     |           |1.0.0.0/31             |             |                   |              |
     |           +-----------------------+             |                   |    node3     |
     |   node1   |            1.0.0.1/31 |   node2     |1.0.0.2/31         |              |
     |           |                       |             +-------------------+              |
     |           |                       |             |         1.0.0.3/31|              |
     +-----------+                       +-------------+                   +--------------+

  */
  private static SortedMap<String, Configuration> generateNetworkWithThreeHops(
      boolean denyIntoHop3, boolean allowOnlyEstablishedIntoHop1) {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    // first node
    Configuration c1 = cb.setHostname("node1").build();
    Vrf v1 = nf.vrfBuilder().setOwner(c1).build();
    InterfaceAddress c1Addr1 = new InterfaceAddress("1.0.0.0/31");
    Interface i11 = nf.interfaceBuilder().setOwner(c1).setVrf(v1).setAddress(c1Addr1).build();

    // second node
    Configuration c2 = cb.setHostname("node2").build();
    Vrf v2 = nf.vrfBuilder().setOwner(c2).build();
    InterfaceAddress c2Addr1 = new InterfaceAddress("1.0.0.1/31");
    nf.interfaceBuilder().setOwner(c2).setVrf(v2).setAddress(c2Addr1).build();
    InterfaceAddress c2Addr2 = new InterfaceAddress("1.0.0.2/31");
    nf.interfaceBuilder().setOwner(c2).setVrf(v2).setAddress(c2Addr2).build();

    // third node
    Configuration c3 = cb.setHostname("node3").build();
    Vrf v3 = nf.vrfBuilder().setOwner(c3).build();
    InterfaceAddress c3Addr1 = new InterfaceAddress("1.0.0.3/31");
    Interface i31 = nf.interfaceBuilder().setOwner(c3).setVrf(v3).setAddress(c3Addr1).build();

    // static routes on node1
    v1.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("1.0.0.3/32"))
                .setAdministrativeCost(1)
                .setNextHopInterface(i11.getName())
                .setNextHopIp(c2Addr1.getIp())
                .build()));

    // static routes on node 3 to get back to node1
    v3.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("1.0.0.0/32"))
                .setAdministrativeCost(1)
                .setNextHopInterface(i31.getName())
                .setNextHopIp(c2Addr2.getIp())
                .build()));

    if (denyIntoHop3) {
      // stop the flow from entering Node3
      i31.setIncomingFilter(
          nf.aclBuilder()
              .setOwner(c3)
              .setLines(
                  ImmutableList.of(
                      IpAccessListLine.rejecting(
                          AclLineMatchExprs.matchSrc(UniverseIpSpace.INSTANCE))))
              .build());
    }
    if (allowOnlyEstablishedIntoHop1) {
      i11.setOutgoingFilter(
          nf.aclBuilder()
              .setOwner(c1)
              .setLines(ImmutableList.of(IpAccessListLine.ACCEPT_ALL))
              .build());
      i11.setIncomingFilter(
          nf.aclBuilder()
              .setOwner(c1)
              .setLines(
                  ImmutableList.of(
                      IpAccessListLine.acceptingHeaderSpace(
                          HeaderSpace.builder()
                              .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
                              .setTcpFlags(ImmutableSet.of(TcpFlagsMatchConditions.ACK_TCP_FLAG))
                              .build()),
                      REJECT_ALL))
              .build());
    }

    return ImmutableSortedMap.of(c1.getHostname(), c1, c2.getHostname(), c2, c3.getHostname(), c3);
  }

  /**
   * Check that ibdp topology fixed-point computation is performed correctly. In particular, ensure
   * that iBGP adjacency are established between loopbacks over IS-IS as the IGP.
   */
  @Test
  public void testBgpOverIsis() throws IOException {
    /*
    *
    * Network setup: an IBGP loopback peering with ISIS as the IGP.
    *
                +-----+ 1.1.1.2/31           1.1.1.3/31+-----+
     1.1.1.1/32 |  n1 +--------------------------------+  n2 |2.2.2.2/32
                +-----+                                +-----+
    *
    */

    Ip lo1Ip = Ip.parse("1.1.1.1");
    Ip lo2Ip = Ip.parse("2.2.2.2");
    IsoAddress isoAddress1 = new IsoAddress("49.0001.0100.0100.1001.00");
    IsoAddress isoAddress2 = new IsoAddress("49.0001.0100.0200.2002.00");
    IsisInterfaceSettings isisInterfaceSettings =
        IsisInterfaceSettings.builder()
            .setPointToPoint(true)
            .setLevel2(
                IsisInterfaceLevelSettings.builder()
                    .setCost(10L)
                    .setMode(IsisInterfaceMode.ACTIVE)
                    .build())
            .build();

    // Node 1
    Configuration c1 =
        _cb.setConfigurationFormat(ConfigurationFormat.CISCO_IOS).setHostname("n1").build();
    Vrf vrf1 = _vb.setName(DEFAULT_VRF_NAME).setOwner(c1).build();
    // Interfaces: loopback and connecting
    _ib.setOwner(c1)
        .setVrf(vrf1)
        .setName("Loopback0")
        .setType(InterfaceType.LOOPBACK)
        .setAddress(new InterfaceAddress(lo1Ip, Prefix.MAX_PREFIX_LENGTH))
        .setIsis(isisInterfaceSettings)
        .build();

    _ib.setOwner(c1)
        .setName("Ethernet0")
        .setType(InterfaceType.PHYSICAL)
        .setVrf(vrf1)
        .setAddress(new InterfaceAddress("1.1.1.2/31"))
        .setIsis(isisInterfaceSettings)
        .build();
    // ISIS process
    IsisProcess.builder()
        .setNetAddress(isoAddress1)
        .setLevel1(null)
        .setLevel2(IsisLevelSettings.builder().build())
        .setVrf(vrf1)
        .build();
    // Bgp process and neighbor:
    BgpProcess bgpp1 = _pb.setVrf(vrf1).setRouterId(Ip.parse("1.1.1.2")).build();
    _nb.setPeerAddress(lo2Ip)
        .setLocalAs(1L)
        .setLocalIp(lo1Ip)
        .setRemoteAs(1L)
        .setBgpProcess(bgpp1)
        .setExportPolicy(_epb.setOwner(c1).build().getName())
        .build();

    // Node 2
    Configuration c2 =
        _cb.setConfigurationFormat(ConfigurationFormat.CISCO_IOS).setHostname("n2").build();
    Vrf vrf2 = _vb.setName(DEFAULT_VRF_NAME).setOwner(c2).build();
    // Interfaces: loopback and connecting
    _ib.setOwner(c2)
        .setVrf(vrf2)
        .setName("Loopback0")
        .setType(InterfaceType.LOOPBACK)
        .setAddress(new InterfaceAddress(lo2Ip, Prefix.MAX_PREFIX_LENGTH))
        .setIsis(isisInterfaceSettings)
        .build();
    _ib.setOwner(c2)
        .setName("Ethernet0")
        .setType(InterfaceType.PHYSICAL)
        .setVrf(vrf2)
        .setAddress(new InterfaceAddress("1.1.1.3/31"))
        .setIsis(isisInterfaceSettings)
        .build();
    // ISIS process
    IsisProcess.builder()
        .setNetAddress(isoAddress2)
        .setLevel1(null)
        .setLevel2(IsisLevelSettings.builder().build())
        .setVrf(vrf2)
        .build();
    BgpProcess bgpp2 = _pb.setVrf(vrf2).setRouterId(Ip.parse("1.1.1.3")).build();
    // Bgp neighbor:
    _nb.setPeerAddress(lo1Ip)
        .setLocalAs(1L)
        .setLocalIp(lo2Ip)
        .setRemoteAs(1L)
        .setBgpProcess(bgpp2)
        .setExportPolicy(_epb.setOwner(c2).build().getName())
        .build();

    ImmutableSortedMap<String, Configuration> configs = ImmutableSortedMap.of("n1", c1, "n2", c2);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.computeDataPlane();

    assertThat(
        batfish
            .getTopologyProvider()
            .getBgpTopology(batfish.getNetworkSnapshot())
            .getGraph()
            .edges()
            .size(),
        equalTo(2));
    BgpPeerConfigId bgpConfig1 =
        new BgpPeerConfigId(
            "n1", DEFAULT_VRF_NAME, Prefix.create(lo2Ip, Prefix.MAX_PREFIX_LENGTH), false);
    BgpPeerConfigId bgpConfig2 =
        new BgpPeerConfigId(
            "n2", DEFAULT_VRF_NAME, Prefix.create(lo1Ip, Prefix.MAX_PREFIX_LENGTH), false);
    assertThat(
        batfish
            .getTopologyProvider()
            .getBgpTopology(batfish.getNetworkSnapshot())
            .getGraph()
            .edges(),
        equalTo(
            ImmutableSet.of(
                EndpointPair.ordered(bgpConfig1, bgpConfig2),
                EndpointPair.ordered(bgpConfig2, bgpConfig1))));
  }

  @Test
  public void testGetForwardingAnalysisDeserialized() throws IOException {
    String hostname = "n1";
    Configuration c = new Configuration(hostname, ConfigurationFormat.CISCO_IOS);
    Batfish batfish = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);
    batfish.computeDataPlane();
    DataPlane deserializedDataPlane = SerializationUtils.clone(batfish.loadDataPlane());

    assertNotNull(deserializedDataPlane.getForwardingAnalysis());
  }
}

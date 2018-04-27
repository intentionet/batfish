package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.Network;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.batfish.common.BatfishLogger;
import org.batfish.common.plugin.DataPlanePlugin.ComputeDataPlaneResult;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.BdpAnswerElement;
import org.batfish.datamodel.collections.RoutesByVrf;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.SetDefaultPolicy;
import org.batfish.dataplane.rib.BgpBestPathRib;
import org.batfish.dataplane.rib.BgpMultipathRib;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.hamcrest.Matcher;
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
  private BgpNeighbor.Builder _nb;
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
    IncrementalDataPlanePlugin dataPlanePlugin = new IncrementalDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);

    // Test that compute Data Plane finishes in a finite time
    dataPlanePlugin.computeDataPlane(false);
  }

  private SortedMap<String, Configuration> generateNetworkWithDuplicates() {
    Ip coreId = new Ip("1.1.1.1");
    Ip neighborId1 = new Ip("1.1.1.9");
    Ip neighborId2 = new Ip("1.1.1.2");
    final int interfcePrefixBits = 30;
    _vb.setName(DEFAULT_VRF_NAME);

    /*
     * Setup as follows:
     * 1.1.1.9               1.1.1.1            1.1.1.2
     * n1+---------x---------+core+---------------+n2
     * n1 & n2 have same Ips. Core only has route to n2, because of interface masks
     */

    _epb.setStatements(ImmutableList.of(new SetDefaultPolicy("DEF")));
    Configuration core =
        _cb.setHostname(CORE_NAME).setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();

    Vrf corevrf = _vb.setOwner(core).build();
    _ib.setOwner(core).setVrf(corevrf).setActive(true);
    _ib.setAddress(new InterfaceAddress(coreId, interfcePrefixBits)).build();
    BgpProcess coreProc = _pb.setRouterId(coreId).setVrf(corevrf).build();
    _nb.setOwner(core)
        .setVrf(corevrf)
        .setBgpProcess(coreProc)
        .setRemoteAs(1)
        .setLocalAs(1)
        .setLocalIp(coreId)
        .setPeerAddress(neighborId1)
        .setExportPolicy(_epb.setOwner(core).build().getName())
        .build();
    _nb.setRemoteAs(1).setLocalAs(1).setLocalIp(coreId).setPeerAddress(neighborId2).build();

    Configuration n1 = _cb.setHostname("n1").build();
    Vrf n1Vrf = _vb.setOwner(n1).build();
    _ib.setOwner(n1).setVrf(n1Vrf);
    _ib.setAddress(new InterfaceAddress(neighborId1, interfcePrefixBits)).build();
    BgpProcess n1Proc = _pb.setRouterId(neighborId1).setVrf(n1Vrf).build();
    _nb.setOwner(n1)
        .setVrf(n1Vrf)
        .setBgpProcess(n1Proc)
        .setRemoteAs(1)
        .setLocalAs(1)
        .setLocalIp(neighborId1)
        .setPeerAddress(coreId)
        .setExportPolicy(_epb.setOwner(n1).build().getName())
        .build();

    Configuration n2 = _cb.setHostname("n2").build();
    Vrf n2Vrf = _vb.setOwner(n2).build();
    _ib.setOwner(n2).setVrf(n2Vrf);
    _ib.setAddress(new InterfaceAddress(neighborId2, interfcePrefixBits)).build();
    BgpProcess n2Proc = _pb.setRouterId(neighborId2).setVrf(n2Vrf).build();
    _nb.setOwner(n2)
        .setVrf(n2Vrf)
        .setBgpProcess(n2Proc)
        .setRemoteAs(1)
        .setLocalAs(1)
        .setLocalIp(neighborId2)
        .setPeerAddress(coreId)
        .setExportPolicy(_epb.setOwner(n2).build().getName())
        .build();

    return ImmutableSortedMap.of(CORE_NAME, core, "n1", n1, "n2", n2);
  }

  private void testBgpAsPathMultipathHelper(
      MultipathEquivalentAsPathMatchMode multipathEquivalentAsPathMatchMode,
      boolean primeBestPathInMultipathBgpRib,
      boolean expectRoute2,
      boolean expectRoute3a,
      boolean expectRoute3b,
      boolean expectRoute3c,
      boolean expectRoute3d) {
    /*
     * Properties of the routes
     */
    // Should appear only for path-length match
    List<SortedSet<Integer>> asPath2 = AsPath.ofSingletonAsSets(2, 4, 6).getAsSets();
    // Should appear only for first-as match and path-length match
    List<SortedSet<Integer>> asPath3a = AsPath.ofSingletonAsSets(3, 5, 6).getAsSets();
    // Should never appear
    List<SortedSet<Integer>> asPath3b = AsPath.ofSingletonAsSets(3, 4, 4, 6).getAsSets();
    // Should always appear
    AsPath bestAsPath = AsPath.ofSingletonAsSets(3, 4, 6);
    List<SortedSet<Integer>> asPath3c = bestAsPath.getAsSets();
    List<SortedSet<Integer>> asPath3d = bestAsPath.getAsSets();
    Ip nextHop2 = new Ip("2.0.0.0");
    Ip nextHop3a = new Ip("3.0.0.1");
    Ip nextHop3b = new Ip("3.0.0.2");
    Ip nextHop3c = new Ip("3.0.0.3");
    Ip nextHop3d = new Ip("3.0.0.4");

    /*
     * Common attributes for all routes
     */
    Prefix p = Prefix.ZERO;
    BgpRoute.Builder b =
        new BgpRoute.Builder()
            .setNetwork(p)
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.INCOMPLETE);

    /*
     * Boilerplate virtual-router setup
     */
    String hostname = "r1";
    Configuration c =
        BatfishTestUtils.createTestConfiguration(hostname, ConfigurationFormat.CISCO_IOS);
    BgpProcess proc = new BgpProcess();
    c.getVrfs().computeIfAbsent(DEFAULT_VRF_NAME, Vrf::new).setBgpProcess(proc);
    VirtualRouter vr = new VirtualRouter(DEFAULT_VRF_NAME, c);

    /*
     * Instantiate routes
     */
    BgpRoute route2 =
        b.setAsPath(asPath2)
            .setNextHopIp(nextHop2)
            .setOriginatorIp(nextHop2)
            .setReceivedFromIp(nextHop2)
            .build();
    BgpRoute route3a =
        b.setAsPath(asPath3a)
            .setNextHopIp(nextHop3a)
            .setOriginatorIp(nextHop3a)
            .setReceivedFromIp(nextHop3a)
            .build();
    BgpRoute route3b =
        b.setAsPath(asPath3b)
            .setNextHopIp(nextHop3b)
            .setOriginatorIp(nextHop3b)
            .setReceivedFromIp(nextHop3b)
            .build();
    BgpRoute route3c =
        b.setAsPath(asPath3c)
            .setNextHopIp(nextHop3c)
            .setOriginatorIp(nextHop3c)
            .setReceivedFromIp(nextHop3c)
            .build();
    BgpRoute route3d =
        b.setAsPath(asPath3d)
            .setNextHopIp(nextHop3d)
            .setOriginatorIp(nextHop3d)
            .setReceivedFromIp(nextHop3d)
            .build();

    /*
     * Set the as-path match mode prior to instantiating bgp multipath RIB
     */
    proc.setMultipathEquivalentAsPathMatchMode(multipathEquivalentAsPathMatchMode);
    BgpMultipathRib bmr = new BgpMultipathRib(vr, proc.getMultipathEquivalentAsPathMatchMode());

    /*
     * Prime bgp multipath RIB with best path for the prefix
     */
    if (primeBestPathInMultipathBgpRib) {
      bmr.setBestAsPaths(Collections.singletonMap(p, bestAsPath));
    }

    /*
     * Add routes to multipath RIB.
     */
    bmr.mergeRoute(route2);
    bmr.mergeRoute(route3a);
    bmr.mergeRoute(route3b);
    bmr.mergeRoute(route3c);
    bmr.mergeRoute(route3d);

    /*
     * Initialize the matchers with respect to the output route set
     */
    Set<BgpRoute> postMergeRoutes = bmr.getRoutes();
    Matcher<BgpRoute> present = isIn(postMergeRoutes);
    Matcher<BgpRoute> absent = not(present);

    /*
     * ASSERTIONS:
     * Only the expected routes for the given match mode should be present at end
     */
    assertThat(route2, expectRoute2 ? present : absent);
    assertThat(route3a, expectRoute3a ? present : absent);
    assertThat(route3b, expectRoute3b ? present : absent);
    assertThat(route3c, expectRoute3c ? present : absent);
    assertThat(route3c, expectRoute3d ? present : absent);
  }

  @Test
  public void testBgpAsPathMultipathExactPath() {
    /*
     * Only routes with first-as matching that of best as path should appear in RIB post-merge.
     */
    testBgpAsPathMultipathHelper(
        MultipathEquivalentAsPathMatchMode.EXACT_PATH, true, false, false, false, true, true);
  }

  @Test
  public void testBgpAsPathMultipathFirstAs() {
    /*
     * Only routes with first-as matching that of best as path should appear in RIB post-merge.
     */
    testBgpAsPathMultipathHelper(
        MultipathEquivalentAsPathMatchMode.FIRST_AS, true, false, true, false, true, true);
  }

  @Test
  public void testBgpAsPathMultipathPathLength() {
    /*
     * All routes with same as-path-length as that of best as-path should appear in RIB post-merge.
     */
    testBgpAsPathMultipathHelper(
        MultipathEquivalentAsPathMatchMode.PATH_LENGTH, true, true, true, false, true, true);
  }

  @Test
  public void testBgpAsPathMultipathUnprimed() {
    /*
     * Without priming best as path map, all paths except the longer one should be considered
     * equivalent. Results should be independent of chosen mode.
     */
    for (MultipathEquivalentAsPathMatchMode mode : MultipathEquivalentAsPathMatchMode.values()) {
      testBgpAsPathMultipathHelper(mode, false, true, true, false, true, true);
    }
  }

  @Test
  public void testBgpCompareOriginId() {
    String hostname = "r1";
    Configuration c =
        BatfishTestUtils.createTestConfiguration(hostname, ConfigurationFormat.CISCO_IOS);
    BgpProcess proc = new BgpProcess();
    c.getVrfs().computeIfAbsent(DEFAULT_VRF_NAME, Vrf::new).setBgpProcess(proc);
    VirtualRouter vr = new VirtualRouter(DEFAULT_VRF_NAME, c);
    BgpBestPathRib bbr = BgpBestPathRib.initial(vr, null);
    BgpMultipathRib bmr = new BgpMultipathRib(vr, proc.getMultipathEquivalentAsPathMatchMode());
    Prefix p = Prefix.ZERO;
    BgpRoute.Builder b = new BgpRoute.Builder().setNetwork(p).setProtocol(RoutingProtocol.IBGP);

    /*
     *  Initialize with different originator ips, which should not affect comparison of routes with
     *  different origin type.
     */
    Map<OriginType, List<BgpRoute>> routesByOriginType = new LinkedHashMap<>();
    for (OriginType originType : OriginType.values()) {
      List<BgpRoute> routes =
          routesByOriginType.computeIfAbsent(originType, o -> new ArrayList<>());
      routes.add(
          b.setOriginatorIp(Ip.ZERO).setReceivedFromIp(Ip.ZERO).setOriginType(originType).build());
      routes.add(
          b.setOriginatorIp(Ip.MAX).setReceivedFromIp(Ip.MAX).setOriginType(originType).build());
    }

    /*
     * Whenever origin type is different, it should be overriding factor in preference.
     */
    for (OriginType o1 : OriginType.values()) {
      List<BgpRoute> lhsList = routesByOriginType.get(o1);
      for (OriginType o2 : OriginType.values()) {
        List<BgpRoute> rhsList = routesByOriginType.get(o2);
        for (BgpRoute lhs : lhsList) {
          for (BgpRoute rhs : rhsList) {
            if (o1.getPreference() > o2.getPreference()) {
              assertThat(bbr.comparePreference(lhs, rhs), greaterThan(0));
              assertThat(bmr.comparePreference(lhs, rhs), greaterThan(0));
            } else if (o1.getPreference() < o2.getPreference()) {
              assertThat(bbr.comparePreference(lhs, rhs), lessThan(0));
              assertThat(bmr.comparePreference(lhs, rhs), lessThan(0));
            }
          }
        }
      }
    }
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
    IncrementalDataPlanePlugin dataPlanePlugin = new IncrementalDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);

    // Really just test that no exception is thrown
    dataPlanePlugin.computeDataPlane(false);
  }

  @Test
  public void testBgpTieBreaker() {
    String hostname = "r1";
    Configuration c =
        BatfishTestUtils.createTestConfiguration(hostname, ConfigurationFormat.CISCO_IOS);
    BgpProcess proc = new BgpProcess();
    c.getVrfs().computeIfAbsent(DEFAULT_VRF_NAME, Vrf::new).setBgpProcess(proc);
    VirtualRouter vr = new VirtualRouter(DEFAULT_VRF_NAME, c);

    // good for both ebgp and ibgp
    BgpMultipathRib bmr = new BgpMultipathRib(vr, proc.getMultipathEquivalentAsPathMatchMode());
    // ebgp
    BgpBestPathRib ebgpBpr = BgpBestPathRib.initial(vr, null);
    BgpRoute.Builder ebgpBuilder =
        new BgpRoute.Builder()
            .setNetwork(Prefix.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(Ip.ZERO)
            .setProtocol(RoutingProtocol.BGP)
            .setReceivedFromIp(Ip.ZERO);
    BgpRoute ebgpOlderHigherOriginator =
        ebgpBuilder.setOriginatorIp(Ip.MAX).setReceivedFromIp(new Ip("1.1.1.1")).build();
    BgpRoute ebgpNewerHigherOriginator =
        ebgpBuilder.setOriginatorIp(Ip.MAX).setReceivedFromIp(new Ip("1.1.1.2")).build();
    BgpRoute ebgpLowerOriginator = ebgpBuilder.setOriginatorIp(Ip.ZERO).build();
    // ibgp
    BgpBestPathRib ibgpBpr = BgpBestPathRib.initial(vr, null);
    BgpRoute.Builder ibgpBuilder =
        new BgpRoute.Builder()
            .setNetwork(Prefix.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(Ip.ZERO)
            .setProtocol(RoutingProtocol.IBGP)
            .setReceivedFromIp(Ip.ZERO);
    BgpRoute ibgpOlderHigherOriginator =
        ibgpBuilder.setOriginatorIp(Ip.MAX).setReceivedFromIp(new Ip("1.1.1.1")).build();
    BgpRoute ibgpNewerHigherOriginator =
        ibgpBuilder.setOriginatorIp(Ip.MAX).setReceivedFromIp(new Ip("1.1.1.2")).build();
    BgpRoute ibgpLowerOriginator = ibgpBuilder.setOriginatorIp(Ip.ZERO).build();

    ebgpBpr.mergeRoute(ebgpOlderHigherOriginator);
    ibgpBpr.mergeRoute(ibgpOlderHigherOriginator);

    /*
     * Given default tie-breaking, and all more important attributes being equivalent:
     * - When comparing two eBGP adverts, best-path rib prefers older advert.
     * - If neither is older, or one is iBGP, best-path rib prefers advert with higher router-id.
     * - Multipath RIB ignores both age and router-id, seeing both adverts as equal.
     */

    // Test age comparisons first
    assertThat(
        ebgpBpr.comparePreference(ebgpNewerHigherOriginator, ebgpOlderHigherOriginator),
        lessThan(0));
    assertThat(bmr.comparePreference(ebgpNewerHigherOriginator, ebgpLowerOriginator), equalTo(0));
    assertThat(
        ibgpBpr.comparePreference(ibgpNewerHigherOriginator, ibgpLowerOriginator), lessThan(0));
    assertThat(bmr.comparePreference(ibgpNewerHigherOriginator, ibgpLowerOriginator), equalTo(0));

    /*
     * No two routes have the same arrival time, so force different non-default tie breaker that
     * will *not* break the tie, and we get to originator comparisons.
     */
    proc.setTieBreaker(BgpTieBreaker.CLUSTER_LIST_LENGTH);
    // Test IP
    assertThat(
        ebgpBpr.comparePreference(ebgpNewerHigherOriginator, ebgpLowerOriginator), lessThan(0));
    assertThat(
        ibgpBpr.comparePreference(ibgpNewerHigherOriginator, ibgpLowerOriginator), lessThan(0));

    /*
     * Finally check the neighbor IP is used as last resolution step
     */
    assertThat(
        ebgpBpr.comparePreference(ebgpNewerHigherOriginator, ebgpOlderHigherOriginator),
        lessThan(0));
    assertThat(
        ibgpBpr.comparePreference(ibgpNewerHigherOriginator, ibgpOlderHigherOriginator),
        lessThan(0));
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
    ComputeDataPlaneResult dp = dataPlanePlugin.computeDataPlane(false);
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes(dp._dataPlane);

    SortedSet<AbstractRoute> r1Routes = routes.get("r1").get(DEFAULT_VRF_NAME);
    SortedSet<AbstractRoute> r3Routes = routes.get("r3").get(DEFAULT_VRF_NAME);
    Set<Prefix> r1Prefixes =
        r1Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Set<Prefix> r3Prefixes =
        r3Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Prefix r1Loopback0Prefix = Prefix.parse("1.0.0.1/32");
    Prefix r3Loopback0Prefix = Prefix.parse("3.0.0.3/32");

    // Ensure that r3loopback was accepted by r1
    assertThat(r3Loopback0Prefix, isIn(r1Prefixes));
    // Check the other direction (r1loopback is accepted by r3)
    assertThat(r1Loopback0Prefix, isIn(r3Prefixes));
  }

  @Test
  public void testContainsRoute() {
    String hostname = "r1";
    Configuration c =
        BatfishTestUtils.createTestConfiguration(hostname, ConfigurationFormat.CISCO_IOS);
    BgpProcess proc = new BgpProcess();
    c.getVrfs().computeIfAbsent(DEFAULT_VRF_NAME, Vrf::new).setBgpProcess(proc);
    VirtualRouter vr = new VirtualRouter(DEFAULT_VRF_NAME, c);
    BgpBestPathRib bbr = BgpBestPathRib.initial(vr, null);
    BgpMultipathRib bmr = new BgpMultipathRib(vr, proc.getMultipathEquivalentAsPathMatchMode());
    Ip ip1 = new Ip("1.0.0.0");
    Ip ip2 = new Ip("2.2.0.0");
    BgpRoute.Builder b1 =
        new BgpRoute.Builder()
            .setNextHopIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(Ip.ZERO)
            .setProtocol(RoutingProtocol.BGP)
            .setReceivedFromIp(Ip.ZERO);
    BgpRoute.Builder b2 =
        new BgpRoute.Builder()
            .setNextHopIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(Ip.MAX)
            .setProtocol(RoutingProtocol.BGP)
            .setReceivedFromIp(Ip.ZERO);

    /*
     * Toss a bunch of different routes in each RIB. In the best-path rib, only lower originatorIp
     * routes should remain. In the multipath RIB, all routes should remain.
     */
    for (int i = 8; i <= Prefix.MAX_PREFIX_LENGTH; i++) {
      Prefix p = new Prefix(ip1, i);
      b1.setNetwork(p);
      b2.setNetwork(p);
      bbr.mergeRoute(b1.build());
      bbr.mergeRoute(b2.build());
      bmr.mergeRoute(b1.build());
      bmr.mergeRoute(b2.build());
    }
    for (int i = 16; i <= Prefix.MAX_PREFIX_LENGTH; i++) {
      Prefix p = new Prefix(ip2, i);
      b1.setNetwork(p);
      b2.setNetwork(p);
      bbr.mergeRoute(b1.build());
      bbr.mergeRoute(b2.build());
      bmr.mergeRoute(b1.build());
      bmr.mergeRoute(b2.build());
    }
    for (int i = 8; i <= Prefix.MAX_PREFIX_LENGTH; i++) {
      Prefix p = new Prefix(ip1, i);
      assertTrue(bbr.containsRoute(b1.setNetwork(p).build()));
      b1.setNetwork(p);
      b2.setNetwork(p);
      assertTrue(bbr.containsRoute(b1.build()));
      assertFalse(bbr.containsRoute(b2.build()));
      assertTrue(bmr.containsRoute(b1.build()));
      assertTrue(bmr.containsRoute(b2.build()));
    }
    for (int i = 16; i <= Prefix.MAX_PREFIX_LENGTH; i++) {
      Prefix p = new Prefix(ip2, i);
      b1.setNetwork(p);
      b2.setNetwork(p);
      assertTrue(bbr.containsRoute(b1.build()));
      assertFalse(bbr.containsRoute(b2.build()));
      assertTrue(bmr.containsRoute(b1.build()));
      assertTrue(bmr.containsRoute(b2.build()));
    }
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
    IncrementalDataPlanePlugin dataPlanePlugin = new IncrementalDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);
    ComputeDataPlaneResult dp = dataPlanePlugin.computeDataPlane(false);
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes(dp._dataPlane);
    SortedSet<AbstractRoute> r2aRoutes = routes.get("r2a").get(DEFAULT_VRF_NAME);
    SortedSet<AbstractRoute> r2bRoutes = routes.get("r2b").get(DEFAULT_VRF_NAME);
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
    IncrementalDataPlanePlugin dataPlanePlugin = new IncrementalDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);
    ComputeDataPlaneResult dp = dataPlanePlugin.computeDataPlane(false);
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes(dp._dataPlane);

    SortedSet<AbstractRoute> r2Routes = routes.get("r2").get(DEFAULT_VRF_NAME);
    SortedSet<AbstractRoute> r3Routes = routes.get("r3").get(DEFAULT_VRF_NAME);
    Set<Prefix> r2Prefixes =
        r2Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Set<Prefix> r3Prefixes =
        r3Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    // 9.9.9.9/32 is the prefix we test with
    Prefix r1AdvertisedPrefix = Prefix.parse("9.9.9.9/32");

    // Ensure that the prefix is accepted by r2, because router ids are different
    assertThat(r1AdvertisedPrefix, isIn(r2Prefixes));
    // Ensure that the prefix is rejected by r3, because router ids are the same
    assertThat(r1AdvertisedPrefix, not(isIn(r3Prefixes)));
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
    IncrementalDataPlanePlugin dataPlanePlugin = new IncrementalDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);
    ComputeDataPlaneResult dp = dataPlanePlugin.computeDataPlane(false);
    SortedMap<String, RoutesByVrf> environmentRoutes = batfish.loadEnvironmentRoutingTables();
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes(dp._dataPlane);
    Prefix staticRoutePrefix = Prefix.parse("10.0.0.0/8");
    SortedSet<AbstractRoute> r1BdpRoutes = routes.get("r1").get(DEFAULT_VRF_NAME);
    AbstractRoute r1BdpRoute =
        r1BdpRoutes
            .stream()
            .filter(r -> r.getNetwork().equals(staticRoutePrefix))
            .findFirst()
            .get();
    SortedSet<Route> r1EnvironmentRoutes = environmentRoutes.get("r1").get(DEFAULT_VRF_NAME);
    Route r1EnvironmentRoute =
        r1EnvironmentRoutes
            .stream()
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
            .setActive(true)
            .build();
    StaticRoute srBoth =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("10.0.1.0/24"))
            .setNextHopInterface(i.getName())
            .setNextHopIp(new Ip("10.0.0.1"))
            .build();
    vrf.getStaticRoutes().add(srBoth);
    StaticRoute srJustInterface =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("10.0.2.0/24"))
            .setNextHopInterface(i.getName())
            .build();
    vrf.getStaticRoutes().add(srJustInterface);
    IncrementalBdpEngine engine =
        new IncrementalBdpEngine(
            // TODO: parametrize settings with different schedules
            new IncrementalDataPlaneSettings(),
            new BatfishLogger(BatfishLogger.LEVELSTR_DEBUG, false),
            (a, b) -> new AtomicInteger());
    Topology topology = new Topology(Collections.emptySortedSet());
    IncrementalDataPlane dp =
        engine.computeDataPlane(
            false,
            ImmutableMap.of(c.getName(), c),
            topology,
            Collections.emptySet(),
            new BdpAnswerElement());

    // generating fibs should not crash
    dp.getFibs();
  }

  @Test
  public void testBgpNeighborReachability() throws IOException {
    // Only connect one neighbor (n2) to core router
    SortedMap<String, Configuration> configs = generateNetworkWithDuplicates();

    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    IncrementalDataPlanePlugin dataPlanePlugin = new IncrementalDataPlanePlugin();
    dataPlanePlugin.initialize(batfish);
    DataPlane dp = dataPlanePlugin.computeDataPlane(false)._dataPlane;

    Network<BgpNeighbor, BgpSession> bgpTopology = dp.getBgpTopology();

    // N2 has proper neighbor relationship
    Collection<BgpNeighbor> n2Neighbors =
        configs.get("n2").getVrfs().get(DEFAULT_VRF_NAME).getBgpProcess().getNeighbors().values();
    assertThat(n2Neighbors, hasSize(1));
    assertThat(bgpTopology.degree(n2Neighbors.iterator().next()), is(2));

    // N1 does not have a full session established, because it's not reachable
    Collection<BgpNeighbor> n1Neighbors =
        configs.get("n1").getVrfs().get(DEFAULT_VRF_NAME).getBgpProcess().getNeighbors().values();
    assertThat(n1Neighbors, hasSize(1));
    assertThat(bgpTopology.degree(n1Neighbors.iterator().next()), is(0));
  }
}

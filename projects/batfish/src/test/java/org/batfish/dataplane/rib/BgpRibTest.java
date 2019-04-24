package org.batfish.dataplane.rib;

import static org.batfish.dataplane.ibdp.TestUtils.annotateRoute;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link BgpRib} */
@RunWith(JUnit4.class)
public class BgpRibTest {

  private BgpRoute.Builder _rb;
  private BgpRib _multiPathRib;
  private BgpRib _bestPathRib;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    _rb = BgpRoute.builder();
    _rb.setAsPath(AsPath.ofSingletonAsSets(1L, 2L))
        .setNetwork(Prefix.parse("10.0.0.0/8"))
        .setProtocol(RoutingProtocol.BGP)
        .setLocalPreference(100)
        .setClusterList(ImmutableSortedSet.of(3L, 4L))
        .setCommunities(ImmutableSortedSet.of(StandardCommunity.of(5L), StandardCommunity.of(6L)))
        .setOriginatorIp(Ip.parse("1.1.1.1"))
        .setOriginType(OriginType.IGP)
        .setReceivedFromIp(Ip.parse("1.1.1.1"))
        .setSrcProtocol(RoutingProtocol.CONNECTED)
        .setWeight(0);

    _multiPathRib =
        new BgpRib(
            null,
            null,
            BgpTieBreaker.ROUTER_ID,
            null,
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    _bestPathRib = new BgpRib(null, null, BgpTieBreaker.ROUTER_ID, 1, null);
  }

  @Test
  public void testParameterValidationMaxPaths() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Invalid max-paths value");
    new BgpRib(null, null, BgpTieBreaker.ARRIVAL_ORDER, 0, null);
  }

  @Test
  public void testParameterValidationMatchMode() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Multipath AS-Path-Match-mode must be specified");
    new BgpRib(null, null, BgpTieBreaker.ARRIVAL_ORDER, 2, null);
  }

  @Test
  public void testParameterValidationMatchModeNullMaxPaths() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Multipath AS-Path-Match-mode must be specified");
    new BgpRib(null, null, BgpTieBreaker.ARRIVAL_ORDER, null, null);
  }

  @Test
  public void testIsMultipath() {
    BgpRib rib = new BgpRib(null, null, BgpTieBreaker.ARRIVAL_ORDER, 1, null);
    assertTrue("MaxPaths=1, not multipath", !rib.isMultipath());
    rib =
        new BgpRib(
            null,
            null,
            BgpTieBreaker.ARRIVAL_ORDER,
            2,
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    assertTrue("Maxpaths=2 -> multipath", rib.isMultipath());
    rib =
        new BgpRib(
            null,
            null,
            BgpTieBreaker.ARRIVAL_ORDER,
            null,
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    assertTrue("Maxpaths=null -> multipath", rib.isMultipath());
  }

  @Test
  public void testWeightPreference() {
    BgpRoute worse = _rb.setWeight(0).build();
    // Higher value is preferred
    BgpRoute best = _rb.setWeight(1).build();
    _multiPathRib.mergeRoute(worse);
    _multiPathRib.mergeRoute(best);

    assertThat(_multiPathRib.getRoutes(), contains(best));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(best));
  }

  @Test
  public void testLocalPreference() {
    BgpRoute worse = _rb.setLocalPreference(100).build();
    // Higher value is preferred
    BgpRoute best = _rb.setLocalPreference(200).build();
    _multiPathRib.mergeRoute(worse);
    _multiPathRib.mergeRoute(best);

    assertThat(_multiPathRib.getRoutes(), contains(best));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(best));
  }

  @Test
  public void testAggregatePreference() {
    BgpRoute worse = _rb.setProtocol(RoutingProtocol.BGP).build();
    BgpRoute best = _rb.setProtocol(RoutingProtocol.AGGREGATE).build();
    _multiPathRib.mergeRoute(worse);
    _multiPathRib.mergeRoute(best);

    assertThat(_multiPathRib.getRoutes(), contains(best));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(best));
  }

  @Test
  public void testAsPathLengthPreference() {
    BgpRoute worse = _rb.setAsPath(AsPath.ofSingletonAsSets(1L, 2L)).build();
    // shorter path is preferred
    BgpRoute best = _rb.setAsPath(AsPath.ofSingletonAsSets(3L)).build();
    _multiPathRib.mergeRoute(worse);
    _multiPathRib.mergeRoute(best);

    assertThat(_multiPathRib.getRoutes(), contains(best));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(best));
  }

  @Test
  public void testOriginTypePreference() {
    BgpRoute worst = _rb.setOriginType(OriginType.INCOMPLETE).build();
    BgpRoute medium = _rb.setOriginType(OriginType.EGP).build();
    BgpRoute best = _rb.setOriginType(OriginType.IGP).build();

    _multiPathRib.mergeRoute(worst);
    _multiPathRib.mergeRoute(medium);

    assertThat(_multiPathRib.getRoutes(), contains(medium));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(medium));

    _multiPathRib.mergeRoute(best);
    assertThat(_multiPathRib.getRoutes(), contains(best));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(best));
  }

  @Test
  public void testEIbgpProtocolPreference() {
    BgpRoute worse = _rb.setProtocol(RoutingProtocol.IBGP).build();
    BgpRoute best = _rb.setProtocol(RoutingProtocol.BGP).build();

    _multiPathRib.mergeRoute(worse);
    _multiPathRib.mergeRoute(best);

    assertThat(_multiPathRib.getRoutes(), contains(best));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(best));
  }

  @Test
  public void testMedPreference() {
    BgpRoute worse = _rb.setMetric(1).build();
    BgpRoute best = _rb.setMetric(0).build();

    _multiPathRib.mergeRoute(worse);
    _multiPathRib.mergeRoute(best);

    assertThat(_multiPathRib.getRoutes(), contains(best));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(best));
  }

  @Test
  public void testIgpCostPreference() {
    Rib mainRib = new Rib();
    StaticRoute.Builder sb =
        StaticRoute.builder().setAdministrativeCost(1).setNextHopInterface("eth0");
    mainRib.mergeRoute(
        annotateRoute(sb.setNetwork(Prefix.parse("5.5.5.5/32")).setMetric(1).build()));
    mainRib.mergeRoute(
        annotateRoute(sb.setNetwork(Prefix.parse("5.5.5.6/32")).setMetric(2).build()));

    BgpRib rib =
        new BgpRib(
            null,
            mainRib,
            BgpTieBreaker.ROUTER_ID,
            null,
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);

    BgpRoute worse = _rb.setNextHopIp(Ip.parse("5.5.5.6")).build();
    // Lower IGP cost to next hop is better
    BgpRoute best = _rb.setNextHopIp(Ip.parse("5.5.5.5")).build();

    rib.mergeRoute(worse);
    rib.mergeRoute(best);
    assertThat(rib.getRoutes(), contains(best));
    assertThat(rib.getBestPathRoutes(), contains(best));
  }

  @Test
  public void testMultipathAsPathModeExactPath() {
    BgpRib rib =
        new BgpRib(
            null,
            null,
            BgpTieBreaker.ROUTER_ID,
            null,
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);

    BgpRoute base = _rb.setAsPath(AsPath.ofSingletonAsSets(1L, 2L)).build();
    BgpRoute candidate1 =
        _rb.setAsPath(AsPath.ofSingletonAsSets(1L, 2L)).setNextHopIp(Ip.parse("5.5.5.5")).build();
    BgpRoute candidate2 =
        _rb.setAsPath(AsPath.ofSingletonAsSets(1L, 3L)).setNextHopIp(Ip.parse("5.5.5.6")).build();

    rib.mergeRoute(base);
    assertTrue("Exact AS path match, allow merge", rib.mergeRoute(candidate1));
    assertFalse("Not an exact AS path match, don't merge", rib.mergeRoute(candidate2));
    assertThat(rib.getRoutes(), hasSize(2));
    assertThat(rib.getBestPathRoutes(), hasSize(1));
  }

  @Test
  public void testMultipathAsPathModeFirstAs() {
    BgpRib rib =
        new BgpRib(
            null, null, BgpTieBreaker.ROUTER_ID, null, MultipathEquivalentAsPathMatchMode.FIRST_AS);

    BgpRoute base = _rb.setAsPath(AsPath.ofSingletonAsSets(1L, 2L)).build();
    BgpRoute candidate1 =
        _rb.setAsPath(AsPath.ofSingletonAsSets(1L, 3L)).setNextHopIp(Ip.parse("5.5.5.5")).build();
    BgpRoute candidate2 =
        _rb.setAsPath(AsPath.ofSingletonAsSets(2L, 3L)).setNextHopIp(Ip.parse("5.5.5.6")).build();

    rib.mergeRoute(base);
    assertTrue("Exact AS path match, allow merge", rib.mergeRoute(candidate1));
    assertTrue("Not an exact AS path match, don't merge", !rib.mergeRoute(candidate2));
    assertThat(rib.getRoutes(), hasSize(2));
    assertThat(rib.getBestPathRoutes(), hasSize(1));
  }

  @Test
  public void testMultipathDiffOriginator() {
    BgpRoute bestPath = _rb.build();
    _multiPathRib.mergeRoute(_rb.setOriginatorIp(Ip.parse("2.2.2.2")).build());
    _multiPathRib.mergeRoute(_rb.setOriginatorIp(Ip.parse("2.2.2.3")).build());
    _multiPathRib.mergeRoute(bestPath);
    assertThat(_multiPathRib.getRoutes(), hasSize(3));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(bestPath));
  }

  @Test
  public void testMultipathArrivalOrder() {
    _multiPathRib =
        new BgpRib(
            null,
            null,
            BgpTieBreaker.ARRIVAL_ORDER,
            null,
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    BgpRoute best = _rb.build();
    BgpRoute earliest = _rb.setOriginatorIp(Ip.parse("2.2.2.2")).build();
    _multiPathRib.mergeRoute(earliest);
    _multiPathRib.mergeRoute(_rb.setOriginatorIp(Ip.parse("2.2.2.3")).build());
    _multiPathRib.mergeRoute(best);

    assertThat(_multiPathRib.getRoutes(), hasSize(3));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(earliest));
  }

  @Test
  public void testMultipathDiffClusterList() {
    _rb.setProtocol(RoutingProtocol.IBGP).setClusterList(ImmutableSortedSet.of());
    BgpRoute bestPath = _rb.build();
    _multiPathRib.mergeRoute(bestPath);
    _multiPathRib.mergeRoute(_rb.setClusterList(ImmutableSortedSet.of(11L)).build());
    _multiPathRib.mergeRoute(_rb.setClusterList(ImmutableSortedSet.of(22L, 33L)).build());

    assertThat(_multiPathRib.getRoutes(), hasSize(3));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(bestPath));
  }

  @Test
  public void testMultipathDiffNeighbor() {
    BgpRoute bestPath = _rb.build();
    _multiPathRib.mergeRoute(_rb.setReceivedFromIp(Ip.parse("2.2.2.2")).build());
    _multiPathRib.mergeRoute(_rb.setReceivedFromIp(Ip.parse("2.2.2.3")).build());
    _multiPathRib.mergeRoute(bestPath);

    assertThat(_multiPathRib.getRoutes(), hasSize(3));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(bestPath));
  }

  @Test
  public void testMultipathSamePath() {
    BgpRoute bestPath = _rb.build();
    _multiPathRib.mergeRoute(bestPath);
    _multiPathRib.mergeRoute(bestPath);
    _multiPathRib.mergeRoute(bestPath);

    assertThat(_multiPathRib.getRoutes(), hasSize(1));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(bestPath));
  }

  @Test
  public void testMultipathEviction() {
    _multiPathRib.mergeRoute(_rb.setOriginatorIp(Ip.parse("4.4.4.4")).build());
    _multiPathRib.mergeRoute(_rb.setReceivedFromIp(Ip.parse("2.2.2.2")).build());
    _multiPathRib.mergeRoute(_rb.setReceivedFromIp(Ip.parse("2.2.2.3")).build());

    assertThat(_multiPathRib.getRoutes(), hasSize(3));
    assertThat(_multiPathRib.getBestPathRoutes(), hasSize(1));
    BgpRoute bestPath = _rb.setLocalPreference(1000).build();
    _multiPathRib.mergeRoute(bestPath);

    assertThat(_multiPathRib.getRoutes(), contains(bestPath));
    assertThat(_multiPathRib.getBestPathRoutes(), contains(bestPath));
  }

  @Test
  public void testBestPathsEqualRoutesForBestPathRib() {
    BgpRib bestPathRib = new BgpRib(null, null, BgpTieBreaker.ROUTER_ID, 1, null);
    BgpRoute bestPath = _rb.build();
    bestPathRib.mergeRoute(_rb.setReceivedFromIp(Ip.parse("2.2.2.2")).build());
    bestPathRib.mergeRoute(_rb.setReceivedFromIp(Ip.parse("2.2.2.3")).build());
    bestPathRib.mergeRoute(bestPath);

    assertThat(bestPathRib.getRoutes(), contains(bestPath));
    assertThat(bestPathRib.getBestPathRoutes(), contains(bestPath));
  }

  @Test
  public void testMergeMultiplePrefixes() {
    _multiPathRib.mergeRoute(_rb.build());
    _multiPathRib.mergeRoute(_rb.setNetwork(Prefix.parse("10.1.0.0/16")).build());
    _multiPathRib.mergeRoute(_rb.setNetwork(Prefix.parse("10.1.1.0/24")).build());

    assertThat(_multiPathRib.getRoutes(), hasSize(3));
    assertThat(_multiPathRib.getRoutes(), equalTo(_multiPathRib.getBestPathRoutes()));

    _multiPathRib.mergeRoute(
        _rb.setNetwork(Prefix.parse("10.1.1.0/24"))
            .setOriginatorIp(Ip.parse("22.22.22.22"))
            .build());
    assertThat(_multiPathRib.getRoutes(), hasSize(4));
    assertThat(_multiPathRib.getBestPathRoutes(), hasSize(3));
  }

  @Test
  public void testBestPathSelectionWithoutTieBreaking() {
    _bestPathRib.mergeRoute(_rb.build());
    BgpRoute bestPath = _rb.setLocalPreference(200).build();
    _bestPathRib.mergeRoute(bestPath);

    assertThat(_bestPathRib.getRoutes(), contains(bestPath));
  }

  @Test
  public void testBestPathSelectionTieBreakRouterId() {
    _bestPathRib.mergeRoute(_rb.build());
    // Lower originator IP is better
    BgpRoute bestPath = _rb.setOriginatorIp(Ip.parse("1.1.0.1")).build();
    _bestPathRib.mergeRoute(bestPath);

    assertThat(_bestPathRib.getRoutes(), contains(bestPath));
  }

  @Test
  public void testBestPathSelectionTieBreakReceivedFrom() {
    _bestPathRib.mergeRoute(_rb.build());
    // Lower IP is better
    BgpRoute bestPath = _rb.setReceivedFromIp(Ip.parse("1.1.0.1")).build();
    _bestPathRib.mergeRoute(bestPath);

    assertThat(_bestPathRib.getRoutes(), contains(bestPath));
  }

  @Test
  public void testBestPathSelectionTieBreakingArrivalOrder() {
    _bestPathRib = new BgpRib(null, null, BgpTieBreaker.ARRIVAL_ORDER, 1, null);
    BgpRoute bestPath = _rb.build();
    _bestPathRib.mergeRoute(bestPath);
    // Oldest route should win despite newer having lower in Originator IP
    _bestPathRib.mergeRoute(_rb.setOriginatorIp(Ip.parse("1.1.0.1")).build());

    assertThat(_bestPathRib.getRoutes(), contains(bestPath));
  }

  //////////////////////////////////////////////////////////////////////////////////

  // NOTE: Tests below are old and may be somewhat hard to comprehend.
  // For now, keeping them as regression tests.

  //////////////////////////////////////////////////////////////////////////////////

  private static void testBgpAsPathMultipathHelper(
      MultipathEquivalentAsPathMatchMode multipathEquivalentAsPathMatchMode,
      boolean expectRoute2,
      boolean expectRoute3a,
      boolean expectRoute3b,
      boolean expectRoute3c,
      boolean expectRoute3d) {
    /*
     * Properties of the routes
     */
    // Should appear only for path-length match
    AsPath asPath2 = AsPath.ofSingletonAsSets(2L, 4L, 6L);
    // Should appear only for first-as match and path-length match
    AsPath asPath3a = AsPath.ofSingletonAsSets(3L, 5L, 6L);
    // Should never appear
    AsPath asPath3b = AsPath.ofSingletonAsSets(3L, 4L, 4L, 6L);
    // Should always appear
    AsPath bestAsPath = AsPath.ofSingletonAsSets(3L, 4L, 6L);
    AsPath asPath3c = bestAsPath;
    AsPath asPath3d = bestAsPath;
    Ip nextHop2 = Ip.parse("2.0.0.0");
    Ip nextHop3a = Ip.parse("3.0.0.1");
    Ip nextHop3b = Ip.parse("3.0.0.2");
    Ip nextHop3c = Ip.parse("3.0.0.3");
    Ip nextHop3d = Ip.parse("3.0.0.4");

    /*
     * Common attributes for all routes
     */
    Prefix p = Prefix.ZERO;
    BgpRoute.Builder b =
        new BgpRoute.Builder()
            .setNetwork(p)
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.INCOMPLETE);

    BgpRoute bestRoute =
        b.setAsPath(bestAsPath)
            .setNextHopIp(Ip.parse("1.1.1.1"))
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setReceivedFromIp(Ip.parse("1.1.1.1."))
            .build();

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

    BgpRib bmr =
        new BgpRib(
            null, null, BgpTieBreaker.ARRIVAL_ORDER, null, multipathEquivalentAsPathMatchMode);

    /*
     * Add routes to multipath RIB.
     */
    bmr.mergeRoute(bestRoute);
    bmr.mergeRoute(route2);
    bmr.mergeRoute(route3a);
    bmr.mergeRoute(route3b);
    bmr.mergeRoute(route3c);
    bmr.mergeRoute(route3d);

    /*
     * Initialize the matchers with respect to the output route set
     */
    Set<BgpRoute> postMergeRoutes = bmr.getTypedRoutes();
    Matcher<BgpRoute> present = in(postMergeRoutes);
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
     * Only routes with exact AS path match to that of best AS path should appear in RIB post-merge.
     */
    testBgpAsPathMultipathHelper(
        MultipathEquivalentAsPathMatchMode.EXACT_PATH, false, false, false, true, true);
  }

  @Test
  public void testBgpAsPathMultipathFirstAs() {
    /*
     * Only routes with first-as matching that of best as path should appear in RIB post-merge.
     */
    testBgpAsPathMultipathHelper(
        MultipathEquivalentAsPathMatchMode.FIRST_AS, false, true, false, true, true);
  }

  @Test
  public void testBgpAsPathMultipathPathLength() {
    /*
     * All routes with same as-path-length as that of best as-path should appear in RIB post-merge.
     */
    testBgpAsPathMultipathHelper(
        MultipathEquivalentAsPathMatchMode.PATH_LENGTH, true, true, false, true, true);
  }

  @Test
  public void testBgpCompareOriginType() {
    BgpRib bbr =
        new BgpRib(
            null, null, BgpTieBreaker.ROUTER_ID, 1, MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    BgpRib bmr =
        new BgpRib(
            null,
            null,
            BgpTieBreaker.ROUTER_ID,
            null,
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);

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
  public void testContainsRoute() {

    BgpRib bbr =
        new BgpRib(
            null, null, BgpTieBreaker.ROUTER_ID, 1, MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    BgpRib bmr =
        new BgpRib(
            null,
            null,
            BgpTieBreaker.ROUTER_ID,
            null,
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    Ip ip1 = Ip.parse("1.0.0.0");
    Ip ip2 = Ip.parse("2.2.0.0");
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
      Prefix p = Prefix.create(ip1, i);
      b1.setNetwork(p);
      b2.setNetwork(p);
      bbr.mergeRoute(b1.build());
      bbr.mergeRoute(b2.build());
      bmr.mergeRoute(b1.build());
      bmr.mergeRoute(b2.build());
    }
    for (int i = 16; i <= Prefix.MAX_PREFIX_LENGTH; i++) {
      Prefix p = Prefix.create(ip2, i);
      b1.setNetwork(p);
      b2.setNetwork(p);
      bbr.mergeRoute(b1.build());
      bbr.mergeRoute(b2.build());
      bmr.mergeRoute(b1.build());
      bmr.mergeRoute(b2.build());
    }
    for (int i = 8; i <= Prefix.MAX_PREFIX_LENGTH; i++) {
      Prefix p = Prefix.create(ip1, i);
      assertTrue(bbr.containsRoute(b1.setNetwork(p).build()));
      b1.setNetwork(p);
      b2.setNetwork(p);
      assertTrue(bbr.containsRoute(b1.build()));
      assertFalse(bbr.containsRoute(b2.build()));
      assertTrue(bmr.containsRoute(b1.build()));
      assertTrue(bmr.containsRoute(b2.build()));
    }
    for (int i = 16; i <= Prefix.MAX_PREFIX_LENGTH; i++) {
      Prefix p = Prefix.create(ip2, i);
      b1.setNetwork(p);
      b2.setNetwork(p);
      assertTrue(bbr.containsRoute(b1.build()));
      assertFalse(bbr.containsRoute(b2.build()));
      assertTrue(bmr.containsRoute(b1.build()));
      assertTrue(bmr.containsRoute(b2.build()));
    }
  }

  @Test
  public void testBgpTieBreaker() {
    // good for both ebgp and ibgp
    BgpRib bmr =
        new BgpRib(
            null,
            null,
            BgpTieBreaker.ROUTER_ID,
            null,
            MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    // ebgp
    BgpRib ebgpBpr =
        new BgpRib(
            null, null, BgpTieBreaker.ROUTER_ID, 1, MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    BgpRoute.Builder ebgpBuilder =
        new BgpRoute.Builder()
            .setNetwork(Prefix.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(Ip.ZERO)
            .setProtocol(RoutingProtocol.BGP)
            .setReceivedFromIp(Ip.ZERO);
    BgpRoute ebgpOlderHigherOriginator =
        ebgpBuilder.setOriginatorIp(Ip.MAX).setReceivedFromIp(Ip.parse("1.1.1.1")).build();
    BgpRoute ebgpNewerHigherOriginator =
        ebgpBuilder.setOriginatorIp(Ip.MAX).setReceivedFromIp(Ip.parse("1.1.1.2")).build();
    BgpRoute ebgpLowerOriginator = ebgpBuilder.setOriginatorIp(Ip.ZERO).build();
    // ibgp
    BgpRib ibgpBpr =
        new BgpRib(
            null, null, BgpTieBreaker.ROUTER_ID, 1, MultipathEquivalentAsPathMatchMode.EXACT_PATH);
    BgpRoute.Builder ibgpBuilder =
        new BgpRoute.Builder()
            .setNetwork(Prefix.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setOriginatorIp(Ip.ZERO)
            .setProtocol(RoutingProtocol.IBGP)
            .setReceivedFromIp(Ip.ZERO);
    BgpRoute ibgpOlderHigherOriginator =
        ibgpBuilder.setOriginatorIp(Ip.MAX).setReceivedFromIp(Ip.parse("1.1.1.1")).build();
    BgpRoute ibgpNewerHigherOriginator =
        ibgpBuilder.setOriginatorIp(Ip.MAX).setReceivedFromIp(Ip.parse("1.1.1.2")).build();
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
  }
}

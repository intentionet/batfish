package org.batfish.bdp;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterableOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests of {@link AbstractRib} */
public class AbstractRibTest {
  /*
   * Test the AbstractRib tree logic. To avoid worrying about route preference comparisons, testing
   * with StaticRoutes and StaticRib. All static routes will be stored in the RIB, without eviction
   * based on preference.
   */
  private AbstractRib<StaticRoute> _rib;
  private static final StaticRoute _mostGeneralRoute =
      new StaticRoute(Prefix.ZERO, Ip.ZERO, null, 0, 0);
  @Rule public ExpectedException _expectedException = ExpectedException.none();

  @Before
  public void setupEmptyRib() {
    _rib = new StaticRib(null);
  }

  @Test
  public void testRibConstructor() {
    // Assertions: Ensure that a new rib is empty upon construction
    assertThat(_rib.getRoutes(), is(emptyIterableOf(StaticRoute.class)));
    assertThat(_rib.containsRoute(_mostGeneralRoute), is(false));
  }

  @Test
  public void testSingleRouteAdd() {
    // Test: Adding one route
    _rib.mergeRoute(_mostGeneralRoute);

    // Assertions
    // Check routes size
    assertThat(_rib.getRoutes(), hasSize(1));
    // Check that containsRoute works as expected for this simple case
    assertThat(_rib.containsRoute(_mostGeneralRoute), is(true));
    assertThat(
        _rib.containsRoute(new StaticRoute(Prefix.fromString("1.1.1.1/32"), Ip.ZERO, null, 0, 0)),
        is(false));
  }

  /** Inserts overlapping routes into the RIB and returns a (manually) ordered list of them */
  private List<StaticRoute> setupOverlappingRoutes() {
    // Setup helper
    String[] testPrefixes =
        new String[] {"10.0.0.0/8", "10.0.0.0/9", "10.128.0.0/9", "10.1.1.1/32"};
    List<StaticRoute> routes = new ArrayList<>();

    // Test: merge the routes into the RIB
    for (String prefixStr : testPrefixes) {
      StaticRoute r = new StaticRoute(Prefix.fromString(prefixStr), Ip.ZERO, null, 0, 0);
      _rib.mergeRoute(r);
      routes.add(r);
    }
    return routes;
  }

  /** Ensure that only one copy of a routes is stored, regardless of how many times we add it) */
  @Test
  public void testRepeatedAdd() {
    // Setup
    StaticRoute route = new StaticRoute(Prefix.fromString("10.0.0.0/11"), Ip.ZERO, null, 0, 0);

    // Test
    for (int i = 0; i < 5; i++) {
      _rib.mergeRoute(route);
    }

    // Assertions
    // Ensure only one route is stored
    assertThat(_rib.getRoutes().size(), is(1));
    // Check that containsRoute works as expected for this simple case
    assertThat(_rib.containsRoute(route), is(true));
  }

  /**
   * Check that containsRoute and route collection works as expected in the presence of
   * non-overlapping routes in the RIB
   */
  @Test
  public void testNonOverlappingRouteAdd() {
    // Setup
    StaticRoute r1 = new StaticRoute(Prefix.fromString("1.1.1.1/32"), Ip.ZERO, null, 0, 0);
    StaticRoute r2 = new StaticRoute(Prefix.fromString("128.1.1.1/32"), Ip.ZERO, null, 0, 0);

    // Test:
    _rib.mergeRoute(r1);
    _rib.mergeRoute(r2);

    // Assertions
    // Check that both routes exist
    Set<StaticRoute> collectedRoutes = _rib.getRoutes();
    assertThat(collectedRoutes, hasSize(2));
    assertThat(_rib.containsRoute(r1), is(true));
    assertThat(_rib.containsRoute(r2), is(true));
    // Also check route collection via getRoutes()
    assertThat(collectedRoutes, hasItem(r1));
    assertThat(collectedRoutes, hasItem(r2));
  }

  /**
   * Check that containsRoute and route collection works as expected in the presence of overlapping
   * routes in the RIB
   */
  @Test
  public void testMultiOverlappingRouteAdd() {
    // Setup/Test: Add multiple routes with overlapping prefixes
    List<StaticRoute> routes = setupOverlappingRoutes();

    // Assertions
    Set<StaticRoute> collectedRoutes = _rib.getRoutes();
    assertThat(collectedRoutes, hasSize(routes.size()));
    assertThat(_rib.containsRoute(_mostGeneralRoute), is(false));
    for (StaticRoute r : routes) {
      assertThat(_rib.containsRoute(r), is(true));
      assertThat(collectedRoutes, hasItem(r));
    }
  }

  /** Ensure that empty RIB doesn't have any prefix matches */
  @Test
  public void testLongestPrefixMatchWhenEmpty() {
    assertThat(_rib.longestPrefixMatch(new Ip("1.1.1.1")), is(emptyIterableOf(StaticRoute.class)));
    assertThat(_rib.longestPrefixMatch(new Ip("0.0.0.0")), is(emptyIterableOf(StaticRoute.class)));
  }

  /** Ensure that longestPrefixMatch() returns correct routes when the RIB is non-empty */
  @Test
  public void testLongestPrefixMatch() {
    List<StaticRoute> routes = setupOverlappingRoutes();

    // Assertions
    Set<StaticRoute> match = _rib.longestPrefixMatch(new Ip("10.1.1.1"));
    assertThat(match, hasSize(1));
    assertThat(match, contains(routes.get(3)));

    match = _rib.longestPrefixMatch(new Ip("10.1.1.2"));
    assertThat(match, hasSize(1));
    assertThat(match, contains(routes.get(1)));

    match = _rib.longestPrefixMatch(new Ip("11.1.1.1"));
    assertThat(match, is(emptyIterableOf(StaticRoute.class)));
  }

  @Test
  public void testSelfHasSameRoutes() {
    assertThat(_rib, equalTo(_rib));

    // Add some stuff to the rib
    setupOverlappingRoutes();
    assertThat(_rib, equalTo(_rib));
  }

  @Test
  public void testHasSameRoutes() {
    List<StaticRoute> routes = setupOverlappingRoutes();

    // And create a new different RIB
    AbstractRib<StaticRoute> rib2 = new StaticRib(null);
    assertThat(rib2, not(equalTo(_rib)));

    // Add routes
    rib2.mergeRoute(routes.get(0));
    rib2.mergeRoute(routes.get(2));
    rib2.mergeRoute(routes.get(3));
    assertThat(rib2, not(equalTo(_rib)));

    rib2.mergeRoute(routes.get(1));
    assertThat(rib2, equalTo(_rib));
  }

  /**
   * Check that getRoutes works as expected even when routes replace other routes based on
   * preference
   */
  @Test
  public void testGetRoutesWithReplacement() {
    // Use OSPF RIBs for this, as routes with better metric can replace other routes
    OspfIntraAreaRib rib = new OspfIntraAreaRib(null);
    Prefix prefix = Prefix.fromString("1.1.1.1/32");
    rib.mergeRoute(new OspfIntraAreaRoute(prefix, null, 100, 30, 1));

    assertThat(rib.getRoutes(), hasSize(1));
    // This new route replaces old route
    OspfIntraAreaRoute newRoute = new OspfIntraAreaRoute(prefix, null, 100, 10, 1);
    rib.mergeRoute(newRoute);
    assertThat(rib.getRoutes(), contains(newRoute));

    // Add completely new route and check that the size increases
    rib.mergeRoute(new OspfIntraAreaRoute(Prefix.fromString("2.2.2.2/32"), null, 100, 30, 1));
    assertThat(rib.getRoutes(), hasSize(2));
  }

  /** Test that routes obtained from getRoutes() cannot be modified */
  @Test
  public void testGetRoutesCannotBeModified() {
    _rib.mergeRoute(_mostGeneralRoute);
    Set<StaticRoute> routes = _rib.getRoutes();
    StaticRoute r1 = new StaticRoute(Prefix.fromString("1.1.1.1/32"), Ip.ZERO, null, 0, 0);

    // Exception because ImmutableSet
    _expectedException.expect(UnsupportedOperationException.class);
    routes.add(r1);
  }

  /** Test that routes obtained from getRoutes() do NOT reflect subsequent changes to the RIB */
  @Test
  public void testGetRoutesIsNotAView() {
    _rib.mergeRoute(_mostGeneralRoute);
    Set<StaticRoute> routes = _rib.getRoutes();
    StaticRoute r1 = new StaticRoute(Prefix.fromString("1.1.1.1/32"), Ip.ZERO, null, 0, 0);

    _rib.mergeRoute(r1);

    assertThat(routes, not(containsInAnyOrder(r1)));
  }

  /**
   * Test that multiple calls to getRoutes() return the same object, if the RIB has not been
   * modified
   */
  @Test
  public void testGetRoutesSameObject() {
    _rib.mergeRoute(_mostGeneralRoute);

    Set<StaticRoute> routes = _rib.getRoutes();
    assertThat(_rib.getRoutes(), is(routes));
  }
}

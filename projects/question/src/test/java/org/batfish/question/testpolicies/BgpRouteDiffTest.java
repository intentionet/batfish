package org.batfish.question.testpolicies;

import static org.batfish.datamodel.AbstractRoute.PROP_METRIC;
import static org.batfish.datamodel.BgpRoute.PROP_AS_PATH;
import static org.batfish.datamodel.BgpRoute.PROP_COMMUNITIES;
import static org.batfish.datamodel.BgpRoute.PROP_LOCAL_PREFERENCE;
import static org.batfish.question.testpolicies.BgpRouteDiff.routeDiffs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.junit.Test;

/** Tests of {@link BgpRouteDiff}. */
public class BgpRouteDiffTest {
  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new BgpRouteDiff(PROP_AS_PATH, "a", "b"), new BgpRouteDiff(PROP_AS_PATH, "a", "b"))
        .addEqualityGroup(new BgpRouteDiff(PROP_COMMUNITIES, "a", "b"))
        .addEqualityGroup(new BgpRouteDiff(PROP_AS_PATH, "c", "b"))
        .addEqualityGroup(new BgpRouteDiff(PROP_AS_PATH, "a", "c"))
        .testEquals();
  }

  private static BgpRoute.Builder builder() {
    return BgpRoute.builder()
        .setNetwork(Prefix.ZERO)
        .setOriginatorIp(Ip.ZERO)
        .setOriginType(OriginType.IGP)
        .setProtocol(RoutingProtocol.BGP);
  }

  @Test
  public void testRouteDiffs() {
    BgpRoute route1;
    BgpRoute route2;

    // change AS path
    route1 = builder().setAsPath(AsPath.ofSingletonAsSets(1L, 2L)).build();
    route2 = builder().setAsPath(AsPath.ofSingletonAsSets(2L, 3L)).build();
    assertThat(
        routeDiffs(route1, route2), contains(new BgpRouteDiff(PROP_AS_PATH, "[1, 2]", "[2, 3]")));

    // change communities
    route1 = builder().setCommunities(ImmutableSet.of(1L, 2L)).build();
    route2 = builder().setCommunities(ImmutableSet.of(2L, 3L)).build();
    assertThat(
        routeDiffs(route1, route2),
        contains(new BgpRouteDiff(PROP_COMMUNITIES, "[1, 2]", "[2, 3]")));

    // change local preference
    route1 = builder().setLocalPreference(1).build();
    route2 = builder().setLocalPreference(2).build();
    assertThat(
        routeDiffs(route1, route2), contains(new BgpRouteDiff(PROP_LOCAL_PREFERENCE, "1", "2")));

    // change metric
    route1 = builder().setMetric(1).build();
    route2 = builder().setMetric(2).build();
    assertThat(routeDiffs(route1, route2), contains(new BgpRouteDiff(PROP_METRIC, "1", "2")));

    // change all three
    route1 =
        builder()
            .setAsPath(AsPath.ofSingletonAsSets(1L, 2L))
            .setCommunities(ImmutableSet.of(1L, 2L))
            .setLocalPreference(1)
            .setMetric(1)
            .build();
    route2 =
        builder()
            .setAsPath(AsPath.ofSingletonAsSets(2L, 3L))
            .setCommunities(ImmutableSet.of(2L, 3L))
            .setLocalPreference(2)
            .setMetric(2)
            .build();
    assertThat(
        routeDiffs(route1, route2),
        containsInAnyOrder(
            new BgpRouteDiff(PROP_AS_PATH, "[1, 2]", "[2, 3]"),
            new BgpRouteDiff(PROP_COMMUNITIES, "[1, 2]", "[2, 3]"),
            new BgpRouteDiff(PROP_LOCAL_PREFERENCE, "1", "2"),
            new BgpRouteDiff(PROP_METRIC, "1", "2")));
  }
}

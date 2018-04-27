package org.batfish.dataplane.ibdp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.dataplane.rib.BgpBestPathRib;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link BgpBestPathRib} */
public class BgpBestPathRibTest {

  private BgpBestPathRib _bpRib;

  @Before
  public void setup() {
    _bpRib = new BgpBestPathRib(null, BgpTieBreaker.ARRIVAL_ORDER, null);
  }

  @Test
  public void ensureSingleRouteForPrefix() {

    BgpRoute r1 =
        new BgpRoute.Builder()
            .setNetwork(new Prefix(new Ip("1.1.1.1"), 32))
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.IGP)
            .setOriginatorIp(new Ip("7.7.7.7"))
            .setReceivedFromIp(new Ip("7.7.7.7"))
            .setAsPath(ImmutableList.of(ImmutableSortedSet.of(1, 2)))
            .build();
    BgpRoute r2 =
        new BgpRoute.Builder()
            .setNetwork(new Prefix(new Ip("1.1.1.1"), 32))
            .setProtocol(RoutingProtocol.BGP)
            .setOriginType(OriginType.IGP)
            .setOriginatorIp(new Ip("7.7.7.7"))
            .setReceivedFromIp(new Ip("7.7.7.7"))
            // Higher localpref will kick out r1
            .setLocalPreference(200)
            .setAsPath(ImmutableList.of(ImmutableSortedSet.of(1)))
            .build();

    // Test merging same route twice squashes them
    _bpRib.mergeRoute(r1);
    _bpRib.mergeRoute(r1);
    assertThat(_bpRib.getRoutes(), hasSize(1));
    assertThat(_bpRib.getBestAsPaths().size(), equalTo(1));
    assertThat(
        _bpRib.getBestAsPaths().get(r1.getNetwork()),
        equalTo(new AsPath(ImmutableList.of(ImmutableSortedSet.of(1, 2)))));

    // Test better AS path wins
    _bpRib.mergeRoute(r2);
    assertThat(_bpRib.getRoutes(), hasSize(1));
    assertThat(_bpRib.getBestAsPaths().size(), equalTo(1));
    assertThat(
        _bpRib.getBestAsPaths().get(r1.getNetwork()),
        equalTo(new AsPath(ImmutableList.of(ImmutableSortedSet.of(1)))));
  }

  // TODO: test the rest of the tie breaking more thoroughly
}

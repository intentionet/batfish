package org.batfish.dataplane.protocols;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import org.batfish.common.WellKnownCommunity;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.dataplane.exceptions.BgpRoutePropagationException;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of {@link BgpProtocolHelper#transformBgpRoutePreExport(BgpPeerConfig, BgpPeerConfig,
 * BgpSessionProperties, Vrf, Vrf, AbstractRoute) BgpProtocolHelper.transformBgpRouteOnExport}.
 */
public final class BgpProtocolHelperTransformBgpRouteOnExportTest {

  private final NetworkFactory _nf = new NetworkFactory();
  private BgpActivePeerConfig _fromNeighbor;
  private BgpPeerConfig _toNeighbor;
  private BgpSessionProperties _sessionProperties;
  private Vrf _fromVrf;
  private Vrf _toVrf;

  private GeneratedRoute.Builder _baseAggRouteBuilder;
  private BgpRoute.Builder _baseBgpRouteBuilder;

  /** AS for both source and dest peers if IBGP, or only source peer if EBGP */
  private static final Long AS1 = 1L;
  /** AS for dest peer if EBGP */
  private static final Long AS2 = 2L;

  private static final Ip DEST_IP = Ip.parse("3.3.3.3");
  private static final Prefix DEST_NETWORK = Prefix.parse("4.4.4.0/24");
  private static final Ip ORIGINATOR_IP = Ip.parse("1.1.1.1");
  private static final Ip SOURCE_IP = Ip.parse("2.2.2.2");

  /**
   * Sets aggregate and BGP route builders to the most basic form that will result in a route that
   * gets exported (given the default BGP session setup created in {@link #setUpPeers(boolean)}).
   */
  @Before
  public void resetDefaultRouteBuilders() {
    _baseAggRouteBuilder = GeneratedRoute.builder().setNetwork(DEST_NETWORK);
    _baseBgpRouteBuilder =
        new BgpRoute.Builder()
            .setOriginatorIp(ORIGINATOR_IP)
            .setOriginType(OriginType.IGP)
            .setNetwork(DEST_NETWORK)
            .setNextHopIp(DEST_IP)
            .setProtocol(RoutingProtocol.IBGP)
            .setReceivedFromIp(Ip.ZERO);
  }

  /**
   * Sets up the class variables to represent a BGP peer relationship. Then they may be used as
   * parameters to {@link BgpProtocolHelper#transformBgpRoutePreExport(BgpPeerConfig, BgpPeerConfig,
   * BgpSessionProperties, Vrf, Vrf, AbstractRoute) transformBgpRouteOnExport}.
   *
   * @param ibgp Whether to make the peer relationship IBGP
   */
  private void setUpPeers(boolean ibgp) {
    Configuration c1 =
        _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Configuration c2 =
        _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    _fromNeighbor =
        _nf.bgpNeighborBuilder()
            .setLocalAs(AS1)
            .setRemoteAs(ibgp ? AS1 : AS2)
            .setLocalIp(SOURCE_IP)
            .build();
    _toNeighbor =
        _nf.bgpNeighborBuilder()
            .setLocalAs(ibgp ? AS1 : AS2)
            .setRemoteAs(AS1)
            .setLocalIp(DEST_IP)
            .build();
    _sessionProperties = BgpSessionProperties.from(_fromNeighbor, _toNeighbor, false);
    _fromVrf = _nf.vrfBuilder().setOwner(c1).build();
    _nf.bgpProcessBuilder().setVrf(_fromVrf).setRouterId(SOURCE_IP).build();
    _toVrf = _nf.vrfBuilder().setOwner(c2).build();
    _nf.bgpProcessBuilder().setVrf(_toVrf).setRouterId(DEST_IP).build();
  }

  /**
   * Calls {@link BgpProtocolHelper#transformBgpRoutePreExport(BgpPeerConfig, BgpPeerConfig,
   * BgpSessionProperties, Vrf, Vrf, AbstractRoute) transformBgpRoutePreExport} with the given
   * {@code route} and the class variables representing the BGP session.
   */
  private BgpRoute.Builder runTransformBgpRoutePreExport(AbstractRoute route)
      throws BgpRoutePropagationException {
    return BgpProtocolHelper.transformBgpRoutePreExport(
        _fromNeighbor, _toNeighbor, _sessionProperties, _fromVrf, _toVrf, route);
  }

  /**
   * Calls {@link BgpProtocolHelper#transformBgpRoutePostExport(BgpRoute.Builder, BgpPeerConfig,
   * BgpSessionProperties) transformBgpRoutePostExport} with the given {@code routeBuilder} and the
   * class variables representing the BGP session.
   */
  private void runTransformBgpRoutePostExport(BgpRoute.Builder routeBuilder)
      throws BgpRoutePropagationException {
    BgpProtocolHelper.transformBgpRoutePostExport(routeBuilder, _fromNeighbor, _sessionProperties);
  }

  /**
   * Proves that unmodified setup does export the base routes, so that when later tests show a route
   * does not get exported, we know it's due to modifications made in that test.
   */
  @Test
  public void testBaseRoutesGetExported() throws BgpRoutePropagationException {
    for (boolean isIbgp : ImmutableList.of(false, true)) {
      setUpPeers(isIbgp);
      assertThat(runTransformBgpRoutePreExport(_baseAggRouteBuilder.build()), not(nullValue()));
      assertThat(runTransformBgpRoutePreExport(_baseBgpRouteBuilder.build()), not(nullValue()));
    }
  }

  /** Test that transformBgpRouteOnExport copies the tag from the input route */
  @Test
  public void testTagInTransformedRoute() throws BgpRoutePropagationException {
    setUpPeers(true);
    AbstractRoute route =
        StaticRoute.builder()
            .setNetwork(DEST_NETWORK)
            .setTag(12345)
            .setAdministrativeCost(1)
            .build();
    BgpRoute.Builder transformedRoute = runTransformBgpRoutePreExport(route);
    assertThat(transformedRoute.getTag(), equalTo(12345));
  }

  /**
   * Test that communities are correctly transferred to the transformed route if source peer has
   * sendCommunity set, in IBGP or EBGP.
   */
  @Test
  public void testCommunitiesInTransformedRoute() throws BgpRoutePropagationException {
    for (boolean isIbgp : ImmutableList.of(false, true)) {
      setUpPeers(isIbgp);
      Set<Community> communities = ImmutableSortedSet.of(StandardCommunity.of(10L));
      GeneratedRoute aggRoute = _baseAggRouteBuilder.setCommunities(communities).build();
      BgpRoute bgpRoute = _baseBgpRouteBuilder.setCommunities(communities).build();

      // By default, _fromNeighbor doesn't have sendCommunity set; should see no communities
      BgpRoute.Builder transformedAggregateRoute = runTransformBgpRoutePreExport(aggRoute);
      BgpRoute.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpRoute);
      assertThat(transformedAggregateRoute.getCommunities(), empty());
      assertThat(transformedBgpRoute.getCommunities(), empty());

      // Now set sendCommunity and make sure communities appear in transformed routes.
      _fromNeighbor =
          _nf.bgpNeighborBuilder()
              .setSendCommunity(true)
              .setLocalAs(AS1)
              .setRemoteAsns(_fromNeighbor.getRemoteAsns())
              .build();
      transformedAggregateRoute = runTransformBgpRoutePreExport(aggRoute);
      transformedBgpRoute = runTransformBgpRoutePreExport(bgpRoute);
      assertThat(transformedAggregateRoute.getCommunities(), equalTo(communities));
      assertThat(transformedBgpRoute.getCommunities(), equalTo(communities));
    }
  }

  /**
   * Test that transformBgpRouteOnExport returns {@code null} (meaning do not export the route) if
   * it has the {@value org.batfish.common.WellKnownCommunity#NO_ADVERTISE} community (IBGP or EBGP)
   */
  @Test
  public void testRoutesWithNoAdvertiseSetNotExported() throws BgpRoutePropagationException {
    for (boolean isIbgp : ImmutableList.of(false, true)) {
      setUpPeers(isIbgp);
      Set<Community> noAdvertiseCommunitySet =
          ImmutableSortedSet.of(StandardCommunity.of(WellKnownCommunity.NO_ADVERTISE));

      BgpRoute.Builder transformedAggregateRoute =
          runTransformBgpRoutePreExport(
              _baseAggRouteBuilder.setCommunities(noAdvertiseCommunitySet).build());
      BgpRoute.Builder transformedBgpRoute =
          runTransformBgpRoutePreExport(
              _baseBgpRouteBuilder.setCommunities(noAdvertiseCommunitySet).build());

      assertThat(transformedAggregateRoute, nullValue());
      assertThat(transformedBgpRoute, nullValue());
    }
  }

  /**
   * Test that AS path is correctly transferred to the transformed route (not testing for IBGP
   * because that is tested in {@link #testEbgpDoesNotExportWithAsLoop()})
   */
  @Test
  public void testAsPathInTransformedRoute() throws BgpRoutePropagationException {
    setUpPeers(false);
    Long asInPath = 3L;
    AsPath originalAsPath = AsPath.of(AsSet.of(asInPath));
    BgpRoute.Builder transformedAggregateRoute =
        runTransformBgpRoutePreExport(_baseAggRouteBuilder.setAsPath(originalAsPath).build());
    BgpRoute.Builder transformedBgpRoute =
        runTransformBgpRoutePreExport(_baseBgpRouteBuilder.setAsPath(originalAsPath).build());

    // Expect correct as-path before export policy is applied
    AsPath expectedAsPathPreExport = AsPath.of(ImmutableList.of(AsSet.of(asInPath)));
    assertThat(transformedAggregateRoute.getAsPath(), equalTo(expectedAsPathPreExport));
    assertThat(transformedBgpRoute.getAsPath(), equalTo(expectedAsPathPreExport));

    // Expect final transformed route to have source peer's AS prepended to its AS path (since in
    // EBGP)
    AsPath expectedAsPathPostExport =
        AsPath.of(ImmutableList.of(AsSet.of(AS1), AsSet.of(asInPath)));
    runTransformBgpRoutePostExport(transformedAggregateRoute);
    runTransformBgpRoutePostExport(transformedBgpRoute);
    assertThat(transformedAggregateRoute.getAsPath(), equalTo(expectedAsPathPostExport));
    assertThat(transformedBgpRoute.getAsPath(), equalTo(expectedAsPathPostExport));

    // Also check for a route type that does not have an asPath property
    StaticRoute staticRoute =
        StaticRoute.builder().setNetwork(DEST_NETWORK).setAdministrativeCost(1).build();
    BgpRoute.Builder transformedRoute = runTransformBgpRoutePreExport(staticRoute);
    assertThat(transformedRoute.getAsPath(), equalTo(AsPath.empty()));

    runTransformBgpRoutePostExport(transformedRoute);
    assertThat(transformedRoute.getAsPath(), equalTo(AsPath.of(AsSet.of(AS1))));
  }

  /**
   * Test that a route whose AS path contains the destination peer's AS is exported in IBGP but not
   * in EBGP (when source peer hasn't set allowRemoteAsOut).
   */
  @Test
  public void testEbgpDoesNotExportWithAsLoop() throws BgpRoutePropagationException {
    AsPath asPathContainingDestPeer = AsPath.of(AsSet.of(AS2));
    BgpRoute bgpRoute = _baseBgpRouteBuilder.setAsPath(asPathContainingDestPeer).build();
    GeneratedRoute aggRoute = _baseAggRouteBuilder.setAsPath(asPathContainingDestPeer).build();

    // IBGP: Routes should get exported with the expected AS path
    setUpPeers(true);
    BgpRoute.Builder transformedAggregateRoute = runTransformBgpRoutePreExport(aggRoute);
    BgpRoute.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpRoute);
    assertThat(transformedAggregateRoute.getAsPath(), equalTo(asPathContainingDestPeer));
    assertThat(transformedBgpRoute.getAsPath(), equalTo(asPathContainingDestPeer));

    // EBGP: Routes shouldn't get exported
    setUpPeers(false);
    transformedAggregateRoute = runTransformBgpRoutePreExport(aggRoute);
    transformedBgpRoute = runTransformBgpRoutePreExport(bgpRoute);
    assertThat(transformedAggregateRoute, nullValue());
    assertThat(transformedBgpRoute, nullValue());
  }

  /** Test that MED is not preserved/advertised to EBGP peers. */
  @Test
  public void testEbgpDoesNotExportWithMEDSet() throws BgpRoutePropagationException {
    BgpRoute bgpRoute = _baseBgpRouteBuilder.setMetric(1000).build();

    setUpPeers(false);
    BgpRoute.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpRoute);
    assertThat(transformedBgpRoute.getMetric(), equalTo(0L));
  }

  /** Test that MED is preserved/advertised to IBGP peers. */
  @Test
  public void testIbgpDoesNotExportWithMEDSet() throws BgpRoutePropagationException {
    BgpRoute bgpRoute = _baseBgpRouteBuilder.setMetric(1000).build();

    setUpPeers(true);
    BgpRoute.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpRoute);
    assertThat(transformedBgpRoute.getMetric(), equalTo(1000L));
  }

  /** Test that weight is cleared before exporting for both IBGP and EBGP. */
  @Test
  public void testWeightIsClearedBeforeExport() throws BgpRoutePropagationException {
    BgpRoute bgpRoute = _baseBgpRouteBuilder.setWeight(19).build();

    // IBGP
    setUpPeers(true);
    BgpRoute.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpRoute);
    assertThat(transformedBgpRoute.getWeight(), equalTo(0));

    // EBGP
    setUpPeers(false);
    transformedBgpRoute = runTransformBgpRoutePreExport(bgpRoute);
    assertThat(transformedBgpRoute.getWeight(), equalTo(0));
  }
}

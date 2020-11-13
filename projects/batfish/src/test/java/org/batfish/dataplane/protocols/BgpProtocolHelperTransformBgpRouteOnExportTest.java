package org.batfish.dataplane.protocols;

import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopIp;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.convertGeneratedRouteToBgp;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.convertNonBgpRouteToBgpRoute;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.transformBgpRoutePostExport;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Bgpv4Route.Builder;
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
import org.batfish.datamodel.bgp.AddressFamily.Type;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.BgpTopologyUtils.ConfedSessionType;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.junit.Before;
import org.junit.Test;

/** Tests of {@link BgpProtocolHelper#transformBgpRoutePreExport} */
public final class BgpProtocolHelperTransformBgpRouteOnExportTest {

  private final NetworkFactory _nf = new NetworkFactory();
  private BgpActivePeerConfig _headNeighbor;
  private BgpPeerConfig _tailNeighbor;
  private BgpSessionProperties _sessionProperties;
  private BgpProcess _fromBgpProcess;
  private BgpProcess _toBgpProcess;

  private GeneratedRoute.Builder _baseAggRouteBuilder;
  private Bgpv4Route.Builder _baseBgpRouteBuilder;

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
        new Bgpv4Route.Builder()
            .setOriginatorIp(ORIGINATOR_IP)
            .setOriginType(OriginType.IGP)
            .setNetwork(DEST_NETWORK)
            .setNextHopIp(DEST_IP)
            .setProtocol(RoutingProtocol.IBGP)
            .setReceivedFromIp(Ip.ZERO);
  }

  /**
   * Sets up the class variables to represent a BGP peer relationship. Then they may be used as
   * parameters to {@link BgpProtocolHelper#transformBgpRoutePreExport}.
   *
   * @param ibgp Whether to make the peer relationship IBGP
   */
  private void setUpPeers(boolean ibgp) {
    Configuration c1 =
        _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Configuration c2 =
        _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    _headNeighbor =
        _nf.bgpNeighborBuilder()
            .setLocalAs(AS1)
            .setRemoteAs(ibgp ? AS1 : AS2)
            .setLocalIp(SOURCE_IP)
            .build();
    _tailNeighbor =
        _nf.bgpNeighborBuilder()
            .setLocalAs(ibgp ? AS1 : AS2)
            .setRemoteAs(AS1)
            .setLocalIp(DEST_IP)
            .build();
    // Flip so the session props since we're exporting from head -> tail.
    _sessionProperties = BgpSessionProperties.from(_headNeighbor, _tailNeighbor, true);
    BgpProcess.Builder pb =
        _nf.bgpProcessBuilder().setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS);
    Vrf fromVrf = _nf.vrfBuilder().setOwner(c1).build();
    _fromBgpProcess = pb.setVrf(fromVrf).setRouterId(SOURCE_IP).build();
    Vrf toVrf = _nf.vrfBuilder().setOwner(c2).build();
    _toBgpProcess = pb.setVrf(toVrf).setRouterId(DEST_IP).build();
  }

  /**
   * Calls {@link BgpProtocolHelper#transformBgpRoutePreExport} with the given {@code route} and the
   * class variables representing the BGP session.
   */
  private Bgpv4Route.Builder runTransformBgpRoutePreExport(AbstractRoute route) {
    if (route instanceof GeneratedRoute) {
      return BgpProtocolHelper.transformBgpRoutePreExport(
          _headNeighbor,
          _tailNeighbor,
          _sessionProperties,
          _fromBgpProcess,
          _toBgpProcess,
          convertGeneratedRouteToBgp(
                  (GeneratedRoute) route,
                  _fromBgpProcess.getRouterId(),
                  _headNeighbor.getLocalIp(),
                  false)
              .build(),
          Type.IPV4_UNICAST);
    } else if (route instanceof Bgpv4Route) {
      return BgpProtocolHelper.transformBgpRoutePreExport(
          _headNeighbor,
          _tailNeighbor,
          _sessionProperties,
          _fromBgpProcess,
          _toBgpProcess,
          (Bgpv4Route) route,
          Type.IPV4_UNICAST);
    } else {
      RoutingProtocol protocol =
          _sessionProperties.isEbgp() ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
      return BgpProtocolHelper.transformBgpRoutePreExport(
          _headNeighbor,
          _tailNeighbor,
          _sessionProperties,
          _fromBgpProcess,
          _toBgpProcess,
          convertNonBgpRouteToBgpRoute(
                  route,
                  _fromBgpProcess.getRouterId(),
                  _sessionProperties.getTailIp(),
                  protocol.getDefaultAdministrativeCost(ConfigurationFormat.CISCO_IOS),
                  protocol)
              .build(),
          Type.IPV4_UNICAST);
    }
  }

  /**
   * Calls {@link BgpProtocolHelper#transformBgpRoutePostExport} with the given {@code routeBuilder}
   * and the class variables representing the BGP session.
   */
  private void runTransformBgpRoutePostExport(Bgpv4Route.Builder routeBuilder) {
    transformBgpRoutePostExport(
        routeBuilder,
        _sessionProperties.isEbgp(),
        ConfedSessionType.NO_CONFED,
        _headNeighbor.getLocalAs(),
        Ip.ZERO,
        Ip.ZERO);
  }

  /**
   * Proves that unmodified setup does export the base routes, so that when later tests show a route
   * does not get exported, we know it's due to modifications made in that test.
   */
  @Test
  public void testBaseRoutesGetExported() {
    for (boolean isIbgp : ImmutableList.of(false, true)) {
      setUpPeers(isIbgp);
      assertThat(runTransformBgpRoutePreExport(_baseAggRouteBuilder.build()), not(nullValue()));
      assertThat(runTransformBgpRoutePreExport(_baseBgpRouteBuilder.build()), not(nullValue()));
    }
  }

  @Test
  public void testConvertGeneratedToBgpHasNextHop() {
    Ip nextHopIp = Ip.parse("1.1.1.1");
    assertThat(
        convertGeneratedRouteToBgp(_baseAggRouteBuilder.build(), Ip.ZERO, nextHopIp, false).build(),
        hasNextHopIp(nextHopIp));
  }

  /**
   * Test that communities are correctly transferred to the transformed route if source peer has
   * sendCommunity set, in IBGP or EBGP.
   */
  @Test
  public void testCommunitiesInTransformedRoute() {
    for (boolean isIbgp : ImmutableList.of(false, true)) {
      setUpPeers(isIbgp);
      Set<Community> communities = ImmutableSortedSet.of(StandardCommunity.of(10L));
      GeneratedRoute aggRoute = _baseAggRouteBuilder.setCommunities(communities).build();
      Bgpv4Route bgpv4Route = _baseBgpRouteBuilder.setCommunities(communities).build();

      // By default, _fromNeighbor doesn't have sendCommunity set; should see no communities
      Bgpv4Route.Builder transformedAggregateRoute = runTransformBgpRoutePreExport(aggRoute);
      Bgpv4Route.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpv4Route);
      assertThat(transformedAggregateRoute.getCommunities(), empty());
      assertThat(transformedBgpRoute.getCommunities(), empty());

      // Now set sendCommunity and make sure communities appear in transformed routes.
      _headNeighbor =
          _nf.bgpNeighborBuilder()
              .setIpv4UnicastAddressFamily(
                  Ipv4UnicastAddressFamily.builder()
                      .setAddressFamilyCapabilities(
                          AddressFamilyCapabilities.builder().setSendCommunity(true).build())
                      .build())
              .setLocalAs(AS1)
              .setRemoteAsns(_headNeighbor.getRemoteAsns())
              .build();
      transformedAggregateRoute = runTransformBgpRoutePreExport(aggRoute);
      transformedBgpRoute = runTransformBgpRoutePreExport(bgpv4Route);
      assertThat(transformedAggregateRoute.getCommunities(), equalTo(communities));
      assertThat(transformedBgpRoute.getCommunities(), equalTo(communities));
    }
  }

  /**
   * Test that transformBgpRouteOnExport returns {@code null} (meaning do not export the route) if
   * it has the {@value org.batfish.common.WellKnownCommunity#NO_ADVERTISE} community (IBGP or EBGP)
   */
  @Test
  public void testRoutesWithNoAdvertiseSetNotExported() {
    for (boolean isIbgp : ImmutableList.of(false, true)) {
      setUpPeers(isIbgp);
      Set<Community> noAdvertiseCommunitySet =
          ImmutableSortedSet.of(StandardCommunity.NO_ADVERTISE);

      Bgpv4Route.Builder transformedAggregateRoute =
          runTransformBgpRoutePreExport(
              _baseAggRouteBuilder.setCommunities(noAdvertiseCommunitySet).build());
      Bgpv4Route.Builder transformedBgpRoute =
          runTransformBgpRoutePreExport(
              _baseBgpRouteBuilder.setCommunities(noAdvertiseCommunitySet).build());

      assertThat(transformedAggregateRoute, nullValue());
      assertThat(transformedBgpRoute, nullValue());
    }
  }

  /**
   * Test that transformBgpRouteOnExport returns {@code null} (meaning do not export the route) if
   * it has the {@value org.batfish.common.WellKnownCommunity#NO_EXPORT} community and the session
   * is EBGP.
   */
  @Test
  public void testRoutesWithNoExportSetNotExported() {
    Set<Community> noExportCommunitySet = ImmutableSortedSet.of(StandardCommunity.NO_EXPORT);

    {
      // iBGP
      setUpPeers(true);
      Bgpv4Route.Builder transformedAggregateRoute =
          runTransformBgpRoutePreExport(
              _baseAggRouteBuilder.setCommunities(noExportCommunitySet).build());
      Bgpv4Route.Builder transformedBgpRoute =
          runTransformBgpRoutePreExport(
              _baseBgpRouteBuilder.setCommunities(noExportCommunitySet).build());
      assertThat(transformedAggregateRoute, notNullValue());
      assertThat(transformedBgpRoute, notNullValue());
    }

    {
      // eBGP
      setUpPeers(false);
      Bgpv4Route.Builder transformedAggregateRoute =
          runTransformBgpRoutePreExport(
              _baseAggRouteBuilder.setCommunities(noExportCommunitySet).build());
      Bgpv4Route.Builder transformedBgpRoute =
          runTransformBgpRoutePreExport(
              _baseBgpRouteBuilder.setCommunities(noExportCommunitySet).build());
      assertThat(transformedAggregateRoute, nullValue());
      assertThat(transformedBgpRoute, nullValue());
    }
    {
      // eBGP within confederation
      _sessionProperties =
          BgpSessionProperties.builder()
              .setTailIp(_tailNeighbor.getLocalIp())
              .setTailAs(_tailNeighbor.getLocalAs())
              .setHeadAs(_headNeighbor.getLocalAs())
              .setHeadIp(_headNeighbor.getLocalIp())
              .setConfedSessionType(ConfedSessionType.WITHIN_CONFED)
              .build();
      Bgpv4Route.Builder transformedAggregateRoute =
          runTransformBgpRoutePreExport(
              _baseAggRouteBuilder.setCommunities(noExportCommunitySet).build());
      Bgpv4Route.Builder transformedBgpRoute =
          runTransformBgpRoutePreExport(
              _baseBgpRouteBuilder.setCommunities(noExportCommunitySet).build());
      assertThat(transformedAggregateRoute, notNullValue());
      assertThat(transformedBgpRoute, notNullValue());
    }
  }

  /**
   * Test that AS path is correctly transferred to the transformed route (not testing for IBGP
   * because that is tested in {@link #testEbgpDoesNotExportWithAsLoop()})
   */
  @Test
  public void testAsPathInTransformedRoute() {
    setUpPeers(false);
    long asInPath = 3L;
    AsPath originalAsPath = AsPath.of(AsSet.of(asInPath));
    Bgpv4Route.Builder transformedAggregateRoute =
        runTransformBgpRoutePreExport(_baseAggRouteBuilder.setAsPath(originalAsPath).build());
    Bgpv4Route.Builder transformedBgpRoute =
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
    Bgpv4Route.Builder transformedRoute = runTransformBgpRoutePreExport(staticRoute);
    assertThat(transformedRoute.getAsPath(), equalTo(AsPath.empty()));

    runTransformBgpRoutePostExport(transformedRoute);
    assertThat(transformedRoute.getAsPath(), equalTo(AsPath.of(AsSet.of(AS1))));
  }

  /**
   * Test that a route whose AS path contains the destination peer's AS is exported in IBGP but not
   * in EBGP (when source peer hasn't set allowRemoteAsOut).
   */
  @Test
  public void testEbgpDoesNotExportWithAsLoop() {
    AsPath asPathContainingDestPeer = AsPath.of(AsSet.of(AS2));
    Bgpv4Route bgpv4Route = _baseBgpRouteBuilder.setAsPath(asPathContainingDestPeer).build();
    GeneratedRoute aggRoute = _baseAggRouteBuilder.setAsPath(asPathContainingDestPeer).build();

    // IBGP: Routes should get exported with the expected AS path
    setUpPeers(true);
    Bgpv4Route.Builder transformedAggregateRoute = runTransformBgpRoutePreExport(aggRoute);
    Bgpv4Route.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpv4Route);
    assertThat(transformedAggregateRoute.getAsPath(), equalTo(asPathContainingDestPeer));
    assertThat(transformedBgpRoute.getAsPath(), equalTo(asPathContainingDestPeer));

    // EBGP: Routes shouldn't get exported
    setUpPeers(false);
    transformedAggregateRoute = runTransformBgpRoutePreExport(aggRoute);
    transformedBgpRoute = runTransformBgpRoutePreExport(bgpv4Route);
    assertThat(transformedAggregateRoute, nullValue());
    assertThat(transformedBgpRoute, nullValue());
  }

  /** Test that MED is not preserved/advertised to EBGP peers. */
  @Test
  public void testEbgpDoesNotExportWithMEDSet() {
    Bgpv4Route bgpv4Route = _baseBgpRouteBuilder.setMetric(1000).build();

    setUpPeers(false);
    Bgpv4Route.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpv4Route);
    assertThat(transformedBgpRoute.getMetric(), equalTo(0L));
  }

  /** Test that MED is preserved/advertised to IBGP peers. */
  @Test
  public void testIbgpDoesNotExportWithMEDSet() {
    Bgpv4Route bgpv4Route = _baseBgpRouteBuilder.setMetric(1000).build();

    setUpPeers(true);
    Bgpv4Route.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpv4Route);
    assertThat(transformedBgpRoute.getMetric(), equalTo(1000L));
  }

  /** Test that weight is cleared before exporting for both IBGP and EBGP. */
  @Test
  public void testWeightIsClearedBeforeExport() {
    Bgpv4Route bgpv4Route = _baseBgpRouteBuilder.setWeight(19).build();

    // IBGP
    setUpPeers(true);
    Bgpv4Route.Builder transformedBgpRoute = runTransformBgpRoutePreExport(bgpv4Route);
    assertThat(transformedBgpRoute.getWeight(), equalTo(0));

    // EBGP
    setUpPeers(false);
    transformedBgpRoute = runTransformBgpRoutePreExport(bgpv4Route);
    assertThat(transformedBgpRoute.getWeight(), equalTo(0));
  }

  @Test
  public void testNonBgpToBgpKeepTag() {
    long tag = 100L;
    setUpPeers(false);
    assertThat(
        convertNonBgpRouteToBgpRoute(
                StaticRoute.builder()
                    .setNetwork(Prefix.ZERO)
                    .setNextHopInterface("foo")
                    .setAdministrativeCost(1)
                    .setTag(tag)
                    .build(),
                _fromBgpProcess.getRouterId(),
                _sessionProperties.getTailIp(),
                170,
                RoutingProtocol.BGP)
            .getTag(),
        equalTo(tag));
  }

  @Test
  public void testNonBgpToBgpKeepMetric() {
    setUpPeers(false);
    long metric = 333;
    assertThat(
        convertNonBgpRouteToBgpRoute(
                StaticRoute.builder()
                    .setNetwork(Prefix.ZERO)
                    .setNextHopInterface("foo")
                    .setAdministrativeCost(1)
                    .setMetric(metric)
                    .build(),
                _fromBgpProcess.getRouterId(),
                _sessionProperties.getTailIp(),
                170,
                RoutingProtocol.BGP)
            .getMetric(),
        equalTo(metric));
  }

  @Test
  public void testAggregateProtocolIsCleared() {
    Builder routeBuilder = Bgpv4Route.builder().setProtocol(RoutingProtocol.AGGREGATE);
    transformBgpRoutePostExport(
        routeBuilder, true, ConfedSessionType.NO_CONFED, 1, Ip.MAX, Ip.ZERO);
    assertThat(
        "Protocol overriden to BGP", routeBuilder.getProtocol(), equalTo(RoutingProtocol.BGP));
  }
}

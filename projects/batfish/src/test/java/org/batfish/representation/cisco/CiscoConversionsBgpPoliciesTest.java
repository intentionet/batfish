package org.batfish.representation.cisco;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpCommonExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpPeerExportPolicyName;
import static org.batfish.representation.cisco.CiscoConversions.generateBgpExportPolicy;
import static org.batfish.representation.cisco.CiscoConversions.generateBgpImportPolicy;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.batfish.common.Warnings;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.expr.MatchCommunitySet;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CiscoConversions} class */
@RunWith(JUnit4.class)
public class CiscoConversionsBgpPoliciesTest {

  private static final Ip PEER_ADDRESS = Ip.parse("1.2.3.4");
  private static final String PREFIX_LIST_NAME = "PREFIX_LIST";
  private static final String ROUTE_MAP_NAME = "ROUTE_MAP";
  private static final Prefix PERMITTED_PREFIX = Prefix.parse("1.1.1.0/24");
  private static final Prefix DENIED_PREFIX = Prefix.parse("2.2.2.0/24");
  private static final Set<Community> PERMITTED_COMMUNITY_SET =
      ImmutableSet.of(StandardCommunity.of(10L));
  private static final Set<Community> DENIED_COMMUNITY_SET =
      ImmutableSet.of(StandardCommunity.of(20L));

  private Configuration _c;
  private Warnings _w;
  private LeafBgpPeerGroup _peerGroup;

  /**
   * Initializes {@link #_c} to be a configuration that contains:
   *
   * <ul>
   *   <li>a BGP common export policy that accepts all routes
   *   <li>{@link RoutingPolicy} called {@link #ROUTE_MAP_NAME} that accepts routes with community
   *       {@link #PERMITTED_COMMUNITY_SET} and denies routes with community {@link
   *       #DENIED_COMMUNITY_SET}
   *   <li>{@link RouteFilterList} called {@link #PREFIX_LIST_NAME} that accepts routes to {@link
   *       #PERMITTED_PREFIX} and denies routes to {@link #DENIED_PREFIX}
   * </ul>
   */
  @Before
  public void before() {
    _w = new Warnings(true, true, true);
    _peerGroup = new IpBgpPeerGroup(PEER_ADDRESS);

    NetworkFactory nf = new NetworkFactory();
    _c = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    RoutingPolicy.builder()
        .setName(computeBgpCommonExportPolicyName(DEFAULT_VRF_NAME))
        .setOwner(_c)
        .addStatement(Statements.ReturnTrue.toStaticStatement())
        .build();
    RouteFilterList prefixList =
        new RouteFilterList(
            PREFIX_LIST_NAME,
            ImmutableList.of(
                new RouteFilterLine(LineAction.PERMIT, PrefixRange.fromPrefix(PERMITTED_PREFIX)),
                new RouteFilterLine(LineAction.DENY, PrefixRange.fromPrefix(DENIED_PREFIX))));
    _c.getRouteFilterLists().put(PREFIX_LIST_NAME, prefixList);
    RoutingPolicy.builder()
        .setOwner(_c)
        .setName(ROUTE_MAP_NAME)
        .addStatement(
            new If(
                new MatchCommunitySet(new LiteralCommunitySet(PERMITTED_COMMUNITY_SET)),
                ImmutableList.of(Statements.ReturnTrue.toStaticStatement())))
        .addStatement(
            new If(
                new MatchCommunitySet(new LiteralCommunitySet(DENIED_COMMUNITY_SET)),
                ImmutableList.of(Statements.ReturnFalse.toStaticStatement())))
        .addStatement(Statements.ReturnFalse.toStaticStatement())
        .build();
    nf.vrfBuilder().setOwner(_c).setName(Configuration.DEFAULT_VRF_NAME).build();
  }

  /**
   * Tests that the given {@link RoutingPolicy} permits route with prefix {@link #PERMITTED_PREFIX}
   * and denies routes with prefix {@link #DENIED_PREFIX} or arbitrary other prefix.
   */
  private void testPolicyMatchesPrefixList(RoutingPolicy p, Direction direction) {
    BgpRoute.Builder r =
        BgpRoute.builder()
            .setOriginatorIp(PEER_ADDRESS)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.IBGP);
    BgpRoute permitted = r.setNetwork(PERMITTED_PREFIX).build();
    BgpRoute denied = r.setNetwork(DENIED_PREFIX).build();
    BgpRoute unmatched = r.setNetwork(Prefix.parse("3.3.3.0/24")).build();
    assertThat(
        p.process(permitted, permitted.toBuilder(), PEER_ADDRESS, DEFAULT_VRF_NAME, direction),
        equalTo(true));
    assertThat(
        p.process(denied, denied.toBuilder(), PEER_ADDRESS, DEFAULT_VRF_NAME, direction),
        equalTo(false));
    assertThat(
        p.process(unmatched, unmatched.toBuilder(), PEER_ADDRESS, DEFAULT_VRF_NAME, direction),
        equalTo(false));
  }

  /**
   * Tests that the given {@link RoutingPolicy} permits route with community {@link
   * #PERMITTED_COMMUNITY_SET} and denies routes with community {@link #DENIED_COMMUNITY_SET} or
   * arbitrary other community.
   */
  private void testPolicyMatchesRouteMap(RoutingPolicy p, Direction direction) {
    BgpRoute.Builder r =
        BgpRoute.builder()
            .setNetwork(Prefix.parse("5.6.7.0/24"))
            .setOriginatorIp(PEER_ADDRESS)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.IBGP);
    BgpRoute permitted = r.setCommunities(PERMITTED_COMMUNITY_SET).build();
    BgpRoute denied = r.setCommunities(DENIED_COMMUNITY_SET).build();
    BgpRoute unmatched = r.setCommunities(ImmutableSet.of(StandardCommunity.of(30L))).build();
    assertThat(
        p.process(permitted, permitted.toBuilder(), PEER_ADDRESS, DEFAULT_VRF_NAME, direction),
        equalTo(true));
    assertThat(
        p.process(denied, denied.toBuilder(), PEER_ADDRESS, DEFAULT_VRF_NAME, direction),
        equalTo(false));
    assertThat(
        p.process(unmatched, unmatched.toBuilder(), PEER_ADDRESS, DEFAULT_VRF_NAME, direction),
        equalTo(false));
  }

  @Test
  public void testGenerateBgpImportPolicyWithNoConstraints() {
    // Create a BGP import policy from a peer group with no constraints on incoming routes.
    // Resulting import policy should be null.
    String bgpImportPolicyName = generateBgpImportPolicy(_peerGroup, DEFAULT_VRF_NAME, _c, _w);
    assertThat(bgpImportPolicyName, nullValue());
    assertThat(_w.getRedFlagWarnings(), empty());
  }

  @Test
  public void testGenerateBgpExportPolicyWithNoConstraints() {
    // Create a BGP export policy from a peer group with no constraints on outgoing routes.
    // Resulting export policy should accept any BGP routes.
    _peerGroup.setDefaultOriginate(false);
    generateBgpExportPolicy(_peerGroup, DEFAULT_VRF_NAME, true, _c, _w);
    RoutingPolicy bgpExportPolicy =
        _c.getRoutingPolicies()
            .get(computeBgpPeerExportPolicyName(DEFAULT_VRF_NAME, _peerGroup.getName()));
    assertThat(bgpExportPolicy, notNullValue());
    assertThat(_w.getRedFlagWarnings(), empty());

    BgpRoute.Builder r =
        BgpRoute.builder()
            .setOriginatorIp(PEER_ADDRESS)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.IBGP)
            .setNetwork(Prefix.parse("5.6.7.0/24"));
    assertThat(
        bgpExportPolicy.process(r.build(), r, PEER_ADDRESS, DEFAULT_VRF_NAME, Direction.OUT),
        equalTo(true));
  }

  @Test
  public void testGenerateBgpImportPolicyWithPrefixList() {
    // Create a BGP import policy from a peer group that uses the prefix-list to filter incoming
    // routes. Resulting import policy should match prefix-list.
    _peerGroup.setInboundPrefixList(PREFIX_LIST_NAME);
    String bgpImportPolicyName = generateBgpImportPolicy(_peerGroup, DEFAULT_VRF_NAME, _c, _w);
    assertThat(bgpImportPolicyName, notNullValue());
    assertThat(_w.getRedFlagWarnings(), empty());

    RoutingPolicy bgpImportPolicy = _c.getRoutingPolicies().get(bgpImportPolicyName);
    assertThat(bgpImportPolicy, notNullValue());

    // Test that the generated policy matches the prefix list
    testPolicyMatchesPrefixList(bgpImportPolicy, Direction.IN);
  }

  @Test
  public void testGenerateBgpExportPolicyWithPrefixList() {
    // Create a BGP export policy from a peer group that uses the prefix-list to filter outgoing
    // routes. Resulting export policy should match prefix-list.
    _peerGroup.setDefaultOriginate(false);
    _peerGroup.setOutboundPrefixList(PREFIX_LIST_NAME);
    generateBgpExportPolicy(_peerGroup, DEFAULT_VRF_NAME, true, _c, _w);

    RoutingPolicy bgpExportPolicy =
        _c.getRoutingPolicies()
            .get(computeBgpPeerExportPolicyName(DEFAULT_VRF_NAME, _peerGroup.getName()));
    assertThat(bgpExportPolicy, notNullValue());
    assertThat(_w.getRedFlagWarnings(), empty());

    // Test that the generated policy matches the prefix list
    testPolicyMatchesPrefixList(bgpExportPolicy, Direction.OUT);
  }

  @Test
  public void testGenerateBgpImportPolicyWithRouteMap() {
    // Create a BGP import policy from a peer group that uses the route-map to filter incoming
    // routes. Resulting import policy should be the route-map.
    _peerGroup.setInboundRouteMap(ROUTE_MAP_NAME);
    String bgpImportPolicyName = generateBgpImportPolicy(_peerGroup, DEFAULT_VRF_NAME, _c, _w);

    RoutingPolicy bgpImportPolicy = _c.getRoutingPolicies().get(bgpImportPolicyName);
    assertThat(bgpImportPolicy, notNullValue());
    assertThat(_w.getRedFlagWarnings(), empty());

    // Test that the generated policy matches the route map
    testPolicyMatchesRouteMap(bgpImportPolicy, Direction.IN);
  }

  @Test
  public void testGenerateBgpExportPolicyWithRouteMap() {
    // Create a BGP export policy from a peer group that uses the route-map to filter outgoing
    // routes. Resulting export policy should match the route-map.
    _peerGroup.setOutboundRouteMap(ROUTE_MAP_NAME);
    _peerGroup.setDefaultOriginate(false);
    generateBgpExportPolicy(_peerGroup, DEFAULT_VRF_NAME, true, _c, _w);

    RoutingPolicy bgpExportPolicy =
        _c.getRoutingPolicies()
            .get(computeBgpPeerExportPolicyName(DEFAULT_VRF_NAME, _peerGroup.getName()));
    assertThat(bgpExportPolicy, notNullValue());
    assertThat(_w.getRedFlagWarnings(), empty());

    // Test that the generated policy matches the route map
    testPolicyMatchesRouteMap(bgpExportPolicy, Direction.OUT);
  }

  @Test
  public void testGenerateBgpImportPolicyWithRouteMapAndPrefixList() {
    // Create a BGP import policy from a peer group that uses both to filter incoming routes.
    // Resulting import policy should be the route-map, and a warning should be generated indicating
    // the prefix-list was ignored.
    _peerGroup.setInboundPrefixList(PREFIX_LIST_NAME);
    _peerGroup.setInboundRouteMap(ROUTE_MAP_NAME);
    String bgpImportPolicyName = generateBgpImportPolicy(_peerGroup, DEFAULT_VRF_NAME, _c, _w);

    RoutingPolicy bgpImportPolicy = _c.getRoutingPolicies().get(bgpImportPolicyName);
    assertThat(bgpImportPolicy, notNullValue());
    assertThat(_w.getRedFlagWarnings(), hasSize(1));
    assertThat(
        _w.getRedFlagWarnings().get(0).getText(),
        equalTo(
            "Batfish does not support configuring more than one filter (route-map/prefix-list/distribute-list) for incoming BGP routes."
                + " When this occurs, only the route-map will be used, or the prefix-list if no route-map is configured."));

    // Test that the generated policy matches the route map
    testPolicyMatchesRouteMap(bgpImportPolicy, Direction.IN);
  }

  @Test
  public void testGenerateBgpExportPolicyWithRouteMapAndPrefixList() {
    // Create a BGP export policy from a peer group that uses both to filter outgoing routes.
    // Resulting export policy should match the route-map, and a warning should be generated
    // indicating the prefix-list was ignored.
    _peerGroup.setOutboundPrefixList(PREFIX_LIST_NAME);
    _peerGroup.setOutboundRouteMap(ROUTE_MAP_NAME);
    _peerGroup.setDefaultOriginate(false);
    generateBgpExportPolicy(_peerGroup, DEFAULT_VRF_NAME, true, _c, _w);

    RoutingPolicy bgpExportPolicy =
        _c.getRoutingPolicies()
            .get(computeBgpPeerExportPolicyName(DEFAULT_VRF_NAME, _peerGroup.getName()));
    assertThat(bgpExportPolicy, notNullValue());
    assertThat(_w.getRedFlagWarnings(), hasSize(1));
    assertThat(
        _w.getRedFlagWarnings().get(0).getText(),
        equalTo(
            "Batfish does not support configuring more than one filter (route-map/prefix-list/distribute-list) for outgoing BGP routes."
                + " When this occurs, only the route-map will be used, or the prefix-list if no route-map is configured."));

    // Test that the generated policy matches the route map
    testPolicyMatchesRouteMap(bgpExportPolicy, Direction.OUT);
  }
}

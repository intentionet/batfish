package org.batfish.grammar.cumulus_frr;

import static org.batfish.datamodel.routing_policy.Environment.Direction.OUT;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.cumulus.CumulusRoutingProtocol.CONNECTED;
import static org.batfish.representation.cumulus.CumulusRoutingProtocol.STATIC;
import static org.batfish.representation.cumulus.CumulusStructureType.IP_COMMUNITY_LIST;
import static org.batfish.representation.cumulus.CumulusStructureUsage.ROUTE_MAP_MATCH_COMMUNITY_LIST;
import static org.batfish.representation.cumulus.RemoteAsType.EXPLICIT;
import static org.batfish.representation.cumulus.RemoteAsType.EXTERNAL;
import static org.batfish.representation.cumulus.RemoteAsType.INTERNAL;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute.Builder;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.main.Batfish;
import org.batfish.representation.cumulus.BgpInterfaceNeighbor;
import org.batfish.representation.cumulus.BgpIpNeighbor;
import org.batfish.representation.cumulus.BgpNeighbor;
import org.batfish.representation.cumulus.BgpPeerGroupNeighbor;
import org.batfish.representation.cumulus.BgpRedistributionPolicy;
import org.batfish.representation.cumulus.BgpVrfAddressFamilyAggregateNetworkConfiguration;
import org.batfish.representation.cumulus.CumulusInterfaceType;
import org.batfish.representation.cumulus.CumulusNcluConfiguration;
import org.batfish.representation.cumulus.CumulusStructureType;
import org.batfish.representation.cumulus.CumulusStructureUsage;
import org.batfish.representation.cumulus.Interface;
import org.batfish.representation.cumulus.IpCommunityListExpanded;
import org.batfish.representation.cumulus.IpPrefixList;
import org.batfish.representation.cumulus.IpPrefixListLine;
import org.batfish.representation.cumulus.OspfNetworkType;
import org.batfish.representation.cumulus.RouteMap;
import org.batfish.representation.cumulus.RouteMapEntry;
import org.batfish.representation.cumulus.StaticRoute;
import org.batfish.representation.cumulus.Vrf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link CumulusFrrParser}. */
public class CumulusFrrGrammarTest {
  private static final String FILENAME = "";
  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cumulus_frr/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static CumulusNcluConfiguration CONFIG;

  @Before
  public void setup() {
    CONFIG = new CumulusNcluConfiguration();
    CONFIG.setFilename(FILENAME);
    CONFIG.setAnswerElement(new ConvertConfigurationAnswerElement());
    CONFIG.setWarnings(new Warnings());
  }

  private static DefinedStructureInfo getDefinedStructureInfo(
      CumulusStructureType type, String name) {
    return CONFIG
        .getAnswerElement()
        .getDefinedStructures()
        .get(CONFIG.getFilename())
        .getOrDefault(type.getDescription(), ImmutableSortedMap.of())
        .get(name);
  }

  private Set<Integer> getStructureReferences(
      CumulusStructureType type, String name, CumulusStructureUsage usage) {
    // The config keeps reference data in a private variable, and only copies into the answer
    // element when you set it.
    CONFIG.setAnswerElement(new ConvertConfigurationAnswerElement());
    return CONFIG
        .getAnswerElement()
        .getReferencedStructures()
        .get(FILENAME)
        .get(type.getDescription())
        .get(name)
        .get(usage.getDescription());
  }

  private static void parseVendorConfig(String filename) {
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    parseVendorConfig(filename, settings);
  }

  private static void parseVendorConfig(String filename, Settings settings) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + filename);
    parseFromTextWithSettings(src, settings);
  }

  private static void parse(String src) {
    Settings settings = new Settings();
    settings.setDisableUnrecognized(true);
    settings.setThrowOnLexerError(true);
    settings.setThrowOnParserError(true);

    parseFromTextWithSettings(src, settings);
  }

  private static void parseLines(String... lines) {
    parse(String.join("\n", lines) + "\n");
  }

  private static void parseFromTextWithSettings(String src, Settings settings) {
    CumulusFrrCombinedParser parser = new CumulusFrrCombinedParser(src, settings, 1, 0);
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    ParseTreeWalker walker = new BatfishParseTreeWalker(parser);
    CumulusFrrConfigurationBuilder cb =
        new CumulusFrrConfigurationBuilder(CONFIG, parser, CONFIG.getWarnings());
    walker.walk(cb, tree);
  }

  @Test
  public void testBgp_defaultVrf() {
    parse("router bgp 12345\n");
    assertThat(CONFIG.getBgpProcess().getDefaultVrf().getAutonomousSystem(), equalTo(12345L));
  }

  @Test
  public void testBgp_vrf() {
    parse("router bgp 12345 vrf foo\n");
    assertThat(CONFIG.getBgpProcess().getVrfs().get("foo").getAutonomousSystem(), equalTo(12345L));
    assertThat(
        getStructureReferences(CumulusStructureType.VRF, "foo", CumulusStructureUsage.BGP_VRF),
        contains(1));
  }

  @Test
  public void testBgpAddressFamily_ipv4Unicast() {
    parse("router bgp 1\n address-family ipv4 unicast\n exit-address-family\n");
    assertNotNull(CONFIG.getBgpProcess().getDefaultVrf().getIpv4Unicast());
  }

  @Test
  public void testBgpAddressFamilyIpv4UnicastNetwork() {
    parseLines(
        "router bgp 1", "address-family ipv4 unicast", "network 1.2.3.4/24", "exit-address-family");
    assertThat(
        CONFIG.getBgpProcess().getDefaultVrf().getIpv4Unicast().getNetworks().keySet(),
        contains(Prefix.parse("1.2.3.4/24")));
  }

  @Test
  public void testBgpAddressFamilyIpv4UnicastRedistributeConnected() {
    parseLines(
        "router bgp 1",
        "address-family ipv4 unicast",
        "redistribute connected",
        "exit-address-family");
    BgpRedistributionPolicy policy =
        CONFIG
            .getBgpProcess()
            .getDefaultVrf()
            .getIpv4Unicast()
            .getRedistributionPolicies()
            .get(CONNECTED);
    assertNotNull(policy);
    assertNull(policy.getRouteMap());
  }

  @Test
  public void testBgpAddressFamilyIpv4UnicastRedistributeRouteMap() {
    parseLines(
        "router bgp 1",
        "address-family ipv4 unicast",
        "redistribute connected route-map foo",
        "exit-address-family");
    BgpRedistributionPolicy policy =
        CONFIG
            .getBgpProcess()
            .getDefaultVrf()
            .getIpv4Unicast()
            .getRedistributionPolicies()
            .get(CONNECTED);
    assertNotNull(policy);
    assertThat(policy.getRouteMap(), equalTo("foo"));
  }

  @Test
  public void testBgpAddressFamilyIpv4UnicastRedistributeStatic() {
    parseLines(
        "router bgp 1",
        "address-family ipv4 unicast",
        "redistribute static",
        "exit-address-family");
    BgpRedistributionPolicy policy =
        CONFIG
            .getBgpProcess()
            .getDefaultVrf()
            .getIpv4Unicast()
            .getRedistributionPolicies()
            .get(STATIC);
    assertNotNull(policy);
    assertNull(policy.getRouteMap());
  }

  @Test
  public void testBgpAddressFamilyIpv4UnicastAggregateAddress() {
    parseLines(
        "router bgp 1",
        "address-family ipv4 unicast",
        "aggregate-address 1.2.3.0/24",
        "exit-address-family");
    Map<Prefix, BgpVrfAddressFamilyAggregateNetworkConfiguration> aggregateNetworks =
        CONFIG.getBgpProcess().getDefaultVrf().getIpv4Unicast().getAggregateNetworks();
    Prefix prefix = Prefix.parse("1.2.3.0/24");
    assertThat(aggregateNetworks, hasKey(prefix));
    assertFalse(aggregateNetworks.get(prefix).isSummaryOnly());
  }

  @Test
  public void testBgpAddressFamily_l2vpn_evpn() {
    parse("router bgp 1\n address-family l2vpn evpn\n exit-address-family\n");
    assertNotNull(CONFIG.getBgpProcess().getDefaultVrf().getL2VpnEvpn());
  }

  @Test
  public void testBgpAdressFamilyL2vpnEvpnAdvertiseAllVni() {
    parseLines(
        "router bgp 1", "address-family l2vpn evpn", "advertise-all-vni", "exit-address-family");
    assertTrue(CONFIG.getBgpProcess().getDefaultVrf().getL2VpnEvpn().getAdvertiseAllVni());
  }

  @Test
  public void testBgpAdressFamilyL2vpnEvpnAdvertiseIpv4Unicast() {
    parseLines(
        "router bgp 1",
        "address-family l2vpn evpn",
        "advertise ipv4 unicast",
        "exit-address-family");
    assertNotNull(CONFIG.getBgpProcess().getDefaultVrf().getL2VpnEvpn().getAdvertiseIpv4Unicast());
  }

  @Test
  public void testBgpAdressFamilyL2vpnEvpnNeighbor() {
    parseLines(
        "router bgp 1",
        "neighbor n interface description a",
        "neighbor 1.2.3.4 description a",
        "address-family l2vpn evpn",
        "neighbor n activate",
        "neighbor 1.2.3.4 activate",
        "exit-address-family");
    Map<String, BgpNeighbor> neighbors = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors();
    assertTrue(neighbors.get("n").getL2vpnEvpnAddressFamily().getActivated());
    assertTrue(neighbors.get("1.2.3.4").getL2vpnEvpnAddressFamily().getActivated());
  }

  @Test
  public void testBgpAddressFamilyNeighborNextHopSelf() {
    parseLines(
        "router bgp 1",
        "neighbor 10.0.0.1 description x",
        "address-family ipv4 unicast",
        "neighbor 10.0.0.1 next-hop-self",
        "exit-address-family");
    assertTrue(
        CONFIG
            .getBgpProcess()
            .getDefaultVrf()
            .getNeighbors()
            .get("10.0.0.1")
            .getIpv4UnicastAddressFamily()
            .getNextHopSelf());
  }

  @Test
  public void testBgpAlwaysCompareMed() {
    parse("router bgp 1\n bgp always-compare-med\n");
  }

  @Test
  public void testBgpAddressFamilyNeighborActivate() {
    parseLines(
        "router bgp 1",
        "neighbor N interface description N",
        "address-family ipv4 unicast",
        "neighbor N activate",
        "exit-address-family");
    assertTrue(
        CONFIG
            .getBgpProcess()
            .getDefaultVrf()
            .getNeighbors()
            .get("N")
            .getIpv4UnicastAddressFamily()
            .getActivated());
  }

  @Test
  public void testBgpAddressFamilyNeighborRouteReflectorClient() {
    parseLines(
        "router bgp 1",
        "neighbor N interface description N",
        "address-family ipv4 unicast",
        "neighbor N route-reflector-client",
        "exit-address-family");
    assertTrue(
        CONFIG
            .getBgpProcess()
            .getDefaultVrf()
            .getNeighbors()
            .get("N")
            .getIpv4UnicastAddressFamily()
            .getRouteReflectorClient());
  }

  @Test
  public void testBgpAddressFamilyNeighborSendCommunity() {
    // No extraction because these are already enabled by default
    parseLines(
        "router bgp 1",
        "address-family ipv4 unicast",
        "neighbor N send-community",
        "exit-address-family");
    parseLines(
        "router bgp 1",
        "address-family ipv4 unicast",
        "neighbor N send-community extended",
        "exit-address-family");
  }

  @Test
  public void testBgpAddressFamilyNeighborSoftReconfiguration() {
    parseLines(
        "router bgp 1",
        "address-family ipv4 unicast",
        "neighbor N soft-reconfiguration inbound",
        "exit-address-family");
    parseLines(
        "router bgp 1",
        "address-family ipv4 unicast",
        "neighbor 10.0.0.1 soft-reconfiguration inbound",
        "exit-address-family");
  }

  @Test
  public void testBgpNeighbor_peerGroup() {
    parse("router bgp 1\n neighbor foo peer-group\n");
    Map<String, BgpNeighbor> neighbors = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors();
    assertThat(neighbors.keySet(), contains("foo"));
    assertThat(neighbors.get("foo"), isA(BgpPeerGroupNeighbor.class));
  }

  @Test
  public void testBgpNeighbor_peerGroup_remote_as() {
    parse("router bgp 1\n neighbor foo peer-group\n neighbor foo remote-as 2\n");
    Map<String, BgpNeighbor> neighbors = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors();
    assertThat(neighbors.keySet(), contains("foo"));
    BgpNeighbor foo = neighbors.get("foo");
    assertThat(foo, isA(BgpPeerGroupNeighbor.class));
    assertThat(foo.getRemoteAs(), equalTo(2L));
  }

  @Test
  public void testBgpNeighbor_interface() {
    parse("router bgp 1\n neighbor foo interface remote-as 2\n");
    Map<String, BgpNeighbor> neighbors = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors();
    assertThat(neighbors.keySet(), contains("foo"));
    assertThat(neighbors.get("foo"), isA(BgpInterfaceNeighbor.class));
  }

  @Test
  public void testBgpNeighborProperty_descrption() {
    parse("router bgp 1\n neighbor n interface description a b c! d\n");
    BgpNeighbor neighbor = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors().get("n");
    assertThat(neighbor.getDescription(), equalTo("a b c! d"));
  }

  @Test
  public void testBgpNeighborProperty_remoteAs_explicit() {
    parse("router bgp 1\n neighbor n interface remote-as 2\n");
    Map<String, BgpNeighbor> neighbors = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors();
    assertThat(neighbors.keySet(), contains("n"));
    BgpNeighbor foo = neighbors.get("n");
    assertThat(foo.getRemoteAsType(), equalTo(EXPLICIT));
    assertThat(foo.getRemoteAs(), equalTo(2L));
  }

  @Test
  public void testBgpNeighborProperty_remoteAs_external() {
    parse("router bgp 1\n neighbor n interface remote-as external\n");
    Map<String, BgpNeighbor> neighbors = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors();
    assertThat(neighbors.keySet(), contains("n"));
    BgpNeighbor foo = neighbors.get("n");
    assertThat(foo.getRemoteAsType(), equalTo(EXTERNAL));
    assertNull(foo.getRemoteAs());
  }

  @Test
  public void testBgpNeighborProperty_remoteAs_internal() {
    parse("router bgp 1\n neighbor n interface remote-as internal\n");
    Map<String, BgpNeighbor> neighbors = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors();
    assertThat(neighbors.keySet(), contains("n"));
    BgpNeighbor foo = neighbors.get("n");
    assertThat(foo.getRemoteAsType(), equalTo(INTERNAL));
    assertNull(foo.getRemoteAs());
  }

  @Test
  public void testBgpNeighborProperty_peerGroup() {
    parse("router bgp 1\n neighbor n interface peer-group pg\n");
    Map<String, BgpNeighbor> neighbors = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors();
    assertThat(neighbors.keySet(), contains("n"));
    BgpNeighbor foo = neighbors.get("n");
    assertThat(foo.getPeerGroup(), equalTo("pg"));
  }

  @Test
  public void testBgpNeighbor_ip() {
    parse("router bgp 1\n neighbor 1.2.3.4 remote-as 2\n");
    Map<String, BgpNeighbor> neighbors = CONFIG.getBgpProcess().getDefaultVrf().getNeighbors();
    assertThat(neighbors.keySet(), contains("1.2.3.4"));
    BgpNeighbor neighbor = neighbors.get("1.2.3.4");
    assertThat(neighbor, isA(BgpIpNeighbor.class));
    assertThat(neighbor.getRemoteAs(), equalTo(2L));
  }

  @Test
  public void testBgpBestpathAsPathMultipathRelax() {
    parse("router bgp 1\n bgp bestpath as-path multipath-relax\n");
    assertTrue(CONFIG.getBgpProcess().getDefaultVrf().getAsPathMultipathRelax());
  }

  @Test
  public void testBgpRouterId() {
    parse("router bgp 1\n bgp router-id 1.2.3.4\n");
    assertThat(CONFIG.getBgpProcess().getDefaultVrf().getRouterId(), equalTo(Ip.parse("1.2.3.4")));
  }

  @Test
  public void testHostname() {
    parse("hostname asdf235jgij981\n");
  }

  @Test
  public void testCumulusFrrVrf() {
    CONFIG.getVrfs().put("NAME", new Vrf("NAME"));
    parse("vrf NAME\n exit-vrf\n");
    assertThat(
        getDefinedStructureInfo(CumulusStructureType.VRF, "NAME").getDefinitionLines(),
        contains(1, 2));
  }

  @Test
  public void testCumulusFrrVrfVni() {
    Vrf vrf = new Vrf("NAME");
    CONFIG.getVrfs().put("NAME", vrf);
    parse("vrf NAME\n vni 170000\n exit-vrf\n");
    assertThat(vrf.getVni(), equalTo(170000));
  }

  @Test
  public void testCumulusFrrVrfIpRoutes() {
    Vrf vrf = new Vrf("NAME");
    CONFIG.getVrfs().put("NAME", vrf);
    parse("vrf NAME\n ip route 1.0.0.0/8 10.0.2.1\n ip route 0.0.0.0/0 10.0.0.1\n exit-vrf\n");
    assertThat(
        vrf.getStaticRoutes(),
        equalTo(
            ImmutableSet.of(
                new StaticRoute(Prefix.parse("1.0.0.0/8"), Ip.parse("10.0.2.1"), null),
                new StaticRoute(Prefix.parse("0.0.0.0/0"), Ip.parse("10.0.0.1"), null))));
  }

  @Test
  public void testCumulusFrrRouteMap() {
    String name = "ROUTE-MAP-NAME";
    parse(String.format("route-map %s permit 10\nroute-map %s deny 20\n", name, name));
    assertThat(CONFIG.getRouteMaps().keySet(), equalTo(ImmutableSet.of(name)));

    RouteMap rm = CONFIG.getRouteMaps().get(name);
    assertThat(rm.getEntries().keySet(), equalTo(ImmutableSet.of(10, 20)));

    RouteMapEntry entry1 = rm.getEntries().get(10);
    assertThat(entry1.getAction(), equalTo(LineAction.PERMIT));

    RouteMapEntry entry2 = rm.getEntries().get(20);
    assertThat(entry2.getAction(), equalTo(LineAction.DENY));

    assertThat(
        getDefinedStructureInfo(CumulusStructureType.ROUTE_MAP, name).getDefinitionLines(),
        equalTo(ImmutableSet.of(1, 2)));
  }

  @Test
  public void testCumulusFrrVrfRouteMapDescription() {
    String name = "ROUTE-MAP-NAME";
    String description = "PERmit Xxx Yy_+!@#$%^&*()";

    parse(String.format("route-map %s permit 10\ndescription %s\n", name, description));

    RouteMap rm = CONFIG.getRouteMaps().get(name);
    RouteMapEntry entry1 = rm.getEntries().get(10);
    assertThat(entry1.getDescription(), equalTo(description));
  }

  @Test
  public void testCumulusFrrVrfRouteMapMatchCommunity() {
    String name = "ROUTE-MAP-NAME";

    parse(String.format("route-map %s permit 10\nmatch community CN1 CN2\n", name));

    RouteMapEntry entry = CONFIG.getRouteMaps().get(name).getEntries().get(10);
    assertThat(entry.getMatchCommunity().getNames(), equalTo(ImmutableList.of("CN1", "CN2")));

    assertThat(
        getStructureReferences(IP_COMMUNITY_LIST, "CN1", ROUTE_MAP_MATCH_COMMUNITY_LIST),
        contains(2));
    assertThat(
        getStructureReferences(IP_COMMUNITY_LIST, "CN2", ROUTE_MAP_MATCH_COMMUNITY_LIST),
        contains(2));
  }

  @Test
  public void testCumulusFrrVrfRouteMapMatchCommunity_multiline() {
    parseLines("route-map RM permit 10", "match community CN1", "match community CN2");

    RouteMapEntry entry = CONFIG.getRouteMaps().get("RM").getEntries().get(10);
    assertThat(entry.getMatchCommunity().getNames(), equalTo(ImmutableList.of("CN1", "CN2")));

    assertThat(
        getStructureReferences(IP_COMMUNITY_LIST, "CN1", ROUTE_MAP_MATCH_COMMUNITY_LIST),
        contains(2));
    assertThat(
        getStructureReferences(IP_COMMUNITY_LIST, "CN2", ROUTE_MAP_MATCH_COMMUNITY_LIST),
        contains(3));
  }

  @Test
  public void testCumulusFrrVrfRouteMapMatchPrefixList() {
    String name = "ROUTE-MAP-NAME";
    String match1 = "match ip address prefix-list PREFIX_LIST1";
    String match2 = "match ip address prefix-list PREFIX_LIST2";

    parse(String.format("route-map %s permit 10\n%s\n%s\n", name, match1, match2));

    RouteMapEntry entry = CONFIG.getRouteMaps().get(name).getEntries().get(10);
    assertThat(
        entry.getMatchIpAddressPrefixList().getNames(),
        equalTo(ImmutableList.of("PREFIX_LIST1", "PREFIX_LIST2")));

    Set<Integer> reference =
        getStructureReferences(
            CumulusStructureType.IP_PREFIX_LIST,
            "PREFIX_LIST1",
            CumulusStructureUsage.ROUTE_MAP_MATCH_IP_ADDRESS_PREFIX_LIST);
    assertThat(reference, equalTo(ImmutableSet.of(2)));

    reference =
        getStructureReferences(
            CumulusStructureType.IP_PREFIX_LIST,
            "PREFIX_LIST2",
            CumulusStructureUsage.ROUTE_MAP_MATCH_IP_ADDRESS_PREFIX_LIST);
    assertThat(reference, equalTo(ImmutableSet.of(3)));
  }

  @Test
  public void testCumulusFrrVrfRouteMapSetMetric() {
    String name = "ROUTE-MAP-NAME";

    parse(String.format("route-map %s permit 10\nset metric 30\n", name));

    RouteMapEntry entry = CONFIG.getRouteMaps().get(name).getEntries().get(10);
    assertThat(entry.getSetMetric().getMetric(), equalTo(30L));
  }

  @Test
  public void testCumulusFrrIpCommunityListExpanded() {
    String name = "NAME";

    parse(String.format("ip community-list expanded %s permit 10000:10 20000:20\n", name));

    IpCommunityListExpanded communityList =
        (IpCommunityListExpanded) CONFIG.getIpCommunityLists().get(name);

    assertThat(
        communityList.getCommunities(),
        equalTo(
            ImmutableList.of(
                StandardCommunity.parse("10000:10"), StandardCommunity.parse("20000:20"))));

    assertThat(
        getDefinedStructureInfo(IP_COMMUNITY_LIST, name).getDefinitionLines(),
        equalTo(ImmutableSet.of(1)));
  }

  @Test
  public void testCumulusFrrVrfRouteMapMatchInterface() {
    String name = "ROUTE-MAP-NAME";

    parse(String.format("route-map %s permit 10\nmatch interface lo\n", name));

    RouteMapEntry entry = CONFIG.getRouteMaps().get(name).getEntries().get(10);
    assertThat(entry.getMatchInterface().getInterfaces(), equalTo(ImmutableSet.of("lo")));

    assertThat(
        getStructureReferences(
            CumulusStructureType.ABSTRACT_INTERFACE,
            "lo",
            CumulusStructureUsage.ROUTE_MAP_MATCH_INTERFACE),
        equalTo(ImmutableSet.of(2)));
  }

  @Test
  public void testCumulusFrrRouteMapMatchTagExtraction() {
    String name = "ROUTE-MAP-NAME";

    parse(String.format("route-map %s permit 10\nmatch tag 65555\n", name));

    RouteMapEntry c = CONFIG.getRouteMaps().get(name).getEntries().get(10);

    assertThat(c.getMatchTag().getTag(), equalTo(65555L));
  }

  @Test
  public void testCumulusFrrRouteMapMatchTagConversion() {
    String name = "ROUTE-MAP-NAME";

    parse(String.format("route-map %s permit 10\nmatch tag 65555\n", name));
    Configuration c = CONFIG.toVendorIndependentConfigurations().get(0);
    RoutingPolicy policy = c.getRoutingPolicies().get(name);

    Builder routeBuilder =
        org.batfish.datamodel.StaticRoute.builder()
            .setNetwork(Prefix.ZERO)
            .setAdmin(1)
            .setNextHopInterface("iface");
    // NOTE: re-using the builder, order of asserts may matter
    // With missing tag -- no match
    assertFalse(policy.process(routeBuilder.build(), Bgpv4Route.builder(), null, "default", OUT));
    // With tag -- match
    assertTrue(
        policy.process(
            routeBuilder.setTag(65555L).build(), Bgpv4Route.builder(), null, "default", OUT));
    // With different tag -- no match
    assertFalse(
        policy.process(
            routeBuilder.setTag(65554L).build(), Bgpv4Route.builder(), null, "default", OUT));
  }

  @Test
  public void testCumulusFrrRouteMapSetLocalPref() {
    String name = "ROUTE-MAP-NAME";

    parse(String.format("route-map %s permit 10\nset local-preference 200\n", name));

    RouteMapEntry c = CONFIG.getRouteMaps().get(name).getEntries().get(10);

    assertThat(c.getSetLocalPreference().getLocalPreference(), equalTo(200L));
  }

  @Test
  public void testCumulusFrrVrfRouteMapSetIpNextHop() {
    String name = "ROUTE-MAP-NAME";
    String clause1 = "set ip next-hop 10.0.0.1";

    parse(String.format("route-map %s permit 10\n%s\n", name, clause1));

    RouteMapEntry entry = CONFIG.getRouteMaps().get(name).getEntries().get(10);
    assertThat(entry.getSetIpNextHop().getNextHop(), equalTo(Ip.parse("10.0.0.1")));
  }

  @Test
  public void testCumulusFrrVrfRouteMapSetCommunity() {
    String name = "ROUTE-MAP-NAME";

    parse(String.format("route-map %s permit 10\nset community 10000:1 20000:2\n", name));

    RouteMapEntry entry = CONFIG.getRouteMaps().get(name).getEntries().get(10);
    assertThat(
        entry.getSetCommunity().getCommunities(),
        equalTo(ImmutableList.of(StandardCommunity.of(10000, 1), StandardCommunity.of(20000, 2))));
  }

  @Test
  public void testCumulusFrrIpPrefixList() {
    String name = "NAME";
    String prefix1 = "10.0.0.1/24";
    String prefix2 = "10.0.1.2/24";
    parse(
        String.format(
            "ip prefix-list %s seq 10 permit %s\n"
                + "ip prefix-list %s seq 20 deny %s ge 27 le 30 \n",
            name, prefix1, name, prefix2));

    assertThat(CONFIG.getIpPrefixLists().keySet(), equalTo(ImmutableSet.of(name)));
    IpPrefixList prefixList = CONFIG.getIpPrefixLists().get(name);
    assertThat(prefixList.getName(), equalTo(name));
    assertThat(prefixList.getLines().keySet(), equalTo(ImmutableSet.of(10L, 20L)));

    IpPrefixListLine line1 = prefixList.getLines().get(10L);
    assertThat(line1.getLine(), equalTo(10L));
    assertThat(line1.getAction(), equalTo(LineAction.PERMIT));
    assertThat(line1.getLengthRange(), equalTo(new SubRange(24, 32)));
    assertThat(line1.getPrefix(), equalTo(Prefix.parse("10.0.0.1/24")));

    IpPrefixListLine line2 = prefixList.getLines().get(20L);
    assertThat(line2.getLine(), equalTo(20L));
    assertThat(line2.getAction(), equalTo(LineAction.DENY));
    assertThat(line2.getLengthRange(), equalTo(new SubRange(27, 30)));
    assertThat(line2.getPrefix(), equalTo(Prefix.parse("10.0.1.2/24")));
  }

  @Test
  public void testCumulusFrrAgentx() {
    parse("agentx\n");
  }

  @Test
  public void testCumulusService() {
    parse("service integrated-vtysh-config\n");
  }

  @Test
  public void testCumulusFrrSyslog() {
    parse("log syslog informational\n");
  }

  @Test
  public void testLineVty() {
    parse("line vty\n");
  }

  @Test
  public void testCumulusFrrUsername() {
    parse("username cumulus nopassword\n");
  }

  @Test
  public void testCumulusFrrVersion() {
    parse("frr version\n");
    parse("frr version sV4@%)!@#$%^&**()_+|\n");
  }

  @Test
  public void testCumulusFrrBgpNeighborBfd() {
    parse("router bgp 10000 vrf VRF\nneighbor N bfd 1 10 20\n");
    parse("router bgp 10000 vrf VRF\nneighbor N bfd\n");
  }

  @Test
  public void testCumulusFrrNeightborPassword() {
    parse("router bgp 10000\nneighbor N password sV4@%)!@#$%^&**()_+|\n");
  }

  @Test
  public void testBgpNeighborEbgpMultihopPeerGroup() {
    parseLines("router bgp 10000", "neighbor N peer-group", "neighbor N ebgp-multihop 3");
    assertThat(
        CONFIG.getBgpProcess().getDefaultVrf().getNeighbors().get("N").getEbgpMultihop(),
        equalTo(3L));
  }

  @Test
  public void testBgpNeighborEbgpMultihopPeer() {
    parseLines("router bgp 10000", "neighbor 10.0.0.1 ebgp-multihop 3");
    assertThat(
        CONFIG.getBgpProcess().getDefaultVrf().getNeighbors().get("10.0.0.1").getEbgpMultihop(),
        equalTo(3L));
  }

  @Test
  public void testIp4vUnicastRoutemap_in() {
    parseLines(
        "router bgp 10000",
        "neighbor N peer-group",
        "address-family ipv4 unicast",
        "neighbor N route-map R in",
        "exit-address-family");
    assertThat(
        CONFIG
            .getBgpProcess()
            .getDefaultVrf()
            .getNeighbors()
            .get("N")
            .getIpv4UnicastAddressFamily()
            .getRouteMapIn(),
        equalTo("R"));
    assertThat(
        getStructureReferences(
            CumulusStructureType.ROUTE_MAP,
            "R",
            CumulusStructureUsage.BGP_IPV4_UNICAST_NEIGHBOR_ROUTE_MAP_IN),
        contains(4));
  }

  @Test
  public void testIp4vUnicastRoutemap_out() {
    parseLines(
        "router bgp 10000",
        "neighbor N2 peer-group",
        "address-family ipv4 unicast",
        "neighbor N2 route-map R out",
        "exit-address-family");
    assertThat(
        CONFIG
            .getBgpProcess()
            .getDefaultVrf()
            .getNeighbors()
            .get("N2")
            .getIpv4UnicastAddressFamily()
            .getRouteMapOut(),
        equalTo("R"));
    assertThat(
        getStructureReferences(
            CumulusStructureType.ROUTE_MAP,
            "R",
            CumulusStructureUsage.BGP_IPV4_UNICAST_NEIGHBOR_ROUTE_MAP_OUT),
        contains(4));
  }

  @Test
  public void testInterface_NoInterface() {
    parseLines("interface swp1 vrf VRF", "description rt1010svc01 swp1s1");
    assertThat(CONFIG.getWarnings().getParseWarnings(), empty());
    assertThat(CONFIG.getInterfaces().keySet(), contains("swp1"));
    assertThat(CONFIG.getInterfaces().get("swp1").getVrf(), equalTo("VRF"));
  }

  @Test
  public void testInterface_InterfaceVrfNotMatch() {
    // has vrf but not match
    Interface i1 = new Interface("swp1", CumulusInterfaceType.PHYSICAL, null, null);
    i1.setVrf("VRF2");
    CONFIG.getInterfaces().put("swp1", i1);
    parseLines("interface swp1 vrf VRF", "description rt1010svc01 swp1s1");
    assertThat(CONFIG.getWarnings().getParseWarnings(), hasSize(1));
    assertThat(
        CONFIG.getWarnings().getParseWarnings().get(0).getComment(),
        equalTo("vrf VRF of interface swp1 does not match vrf VRF2 defined already"));
    assertThat(CONFIG.getInterfaces().get("swp1").getAlias(), equalTo("rt1010svc01 swp1s1"));
  }

  @Test
  public void testInterface_InterfaceDefaultVrfNotMatch() {
    // default vrf not match
    Interface i2 = new Interface("swp2", CumulusInterfaceType.PHYSICAL, null, null);
    i2.setVrf("VRF2");
    CONFIG.getInterfaces().put("swp2", i2);
    parseLines("interface swp2", "description rt1010svc01 swp1s1");
    assertThat(CONFIG.getWarnings().getParseWarnings(), hasSize(1));
    assertThat(
        CONFIG.getWarnings().getParseWarnings().get(0).getComment(),
        equalTo("default vrf of interface swp2 does not match vrf VRF2 defined already"));
    assertThat(CONFIG.getInterfaces().get("swp2").getAlias(), equalTo("rt1010svc01 swp1s1"));
  }

  @Test
  public void testInterface_Correct() {
    Interface i1 = new Interface("swp1", CumulusInterfaceType.PHYSICAL, null, null);
    i1.setVrf("VRF");
    i1.setAlias("rt1010svc01 swp1s1");
    CONFIG.getInterfaces().put("swp1", i1);
    parseLines("interface swp1 vrf VRF", "description rt1010svc01 swp1s1");
    assertThat(CONFIG.getWarnings().getParseWarnings(), empty());
  }

  @Test
  public void testRouterOspf() {
    parse("router ospf\n log-adjacency-changes detail\n");
    assertThat(CONFIG.getWarnings().getParseWarnings(), empty());
    assertNotNull(CONFIG.getOspfProcess());
  }

  @Test
  public void testInterface_ospf_area() {
    Interface i1 = new Interface("swp1", CumulusInterfaceType.PHYSICAL, null, null);
    i1.setVrf("VRF");
    CONFIG.getInterfaces().put("swp1", i1);
    parse("interface swp1 vrf VRF\n ip ospf area 0.0.0.0\n");
    assertThat(CONFIG.getWarnings().getParseWarnings(), empty());
    assertThat(CONFIG.getInterfaces().get("swp1").getOspf().getOspfArea(), equalTo(0L));
  }

  @Test
  public void testInterface_ospf_authentication() {
    Interface i1 = new Interface("swp1", CumulusInterfaceType.PHYSICAL, null, null);
    CONFIG.getInterfaces().put("swp1", i1);
    parse("interface swp1\n ip ospf authentication message-digest\n");
  }

  @Test
  public void testInterface_ospf_authentication_key() {
    Interface i1 = new Interface("swp1", CumulusInterfaceType.PHYSICAL, null, null);
    CONFIG.getInterfaces().put("swp1", i1);
    parse("interface swp1\n ip ospf message-digest-key 1 md5 <SCRUBBED>\n");
  }

  @Test
  public void testInterface_ospf_p2p() {
    Interface i1 = new Interface("swp1", CumulusInterfaceType.PHYSICAL, null, null);
    CONFIG.getInterfaces().put("swp1", i1);
    parse("interface swp1\n ip ospf network point-to-point\n");
    assertThat(i1.getOspf().getNetwork(), equalTo(OspfNetworkType.POINT_TO_POINT));
  }

  @Test
  public void testRouterOspfPassiveInterface_NoInterface() {
    parse("router ospf\n passive-interface lo\n");
    assertThat(CONFIG.getWarnings().getParseWarnings(), hasSize(1));
    assertThat(
        CONFIG.getWarnings().getParseWarnings().get(0).getComment(),
        equalTo("interface lo is not defined"));
  }

  @Test
  public void testRouterOspfPassiveInterface() {
    Interface iface = new Interface("lo", CumulusInterfaceType.PHYSICAL, null, null);
    CONFIG.getInterfaces().put("lo", iface);
    parse("router ospf\n passive-interface lo\n");
    assertTrue(iface.getOspf().getPassive());
  }

  @Test
  public void testCreatePhysicalInterfaceInFRR() {
    String name = "eth1";
    parse(String.format("interface %s\n", name));
    assertThat(CONFIG.getWarnings().getParseWarnings(), empty());
    assertThat(CONFIG.getInterfaces().keySet(), contains(name));
    Interface i1 = CONFIG.getInterfaces().get(name);
    assertThat(i1.getType(), equalTo(CumulusInterfaceType.PHYSICAL));
  }

  @Test
  public void testCreateLoopbackInterfaceInFRR() {
    String name = "lo";
    parse(String.format("interface %s\n", name));
    assertThat(CONFIG.getWarnings().getParseWarnings(), empty());
    assertThat(CONFIG.getInterfaces().keySet(), contains(name));
    Interface i1 = CONFIG.getInterfaces().get(name);
    assertThat(i1.getType(), equalTo(CumulusInterfaceType.LOOPBACK));
  }

  @Test
  public void testNoCreateOtherInterfaceInFRR() {
    String name = "vlan1";
    parse(String.format("interface %s\n", name));
    assertThat(CONFIG.getWarnings().getParseWarnings(), hasSize(1));
    assertThat(
        CONFIG.getWarnings().getParseWarnings().get(0).getComment(),
        equalTo("cannot recognize interface vlan1. Only support loopback and physical interfaces"));
    assertThat(CONFIG.getInterfaces().keySet(), empty());
  }

  @Test
  public void testSetInterfaceIpAddress() {
    parseLines("interface eth1", "ip address 1.1.1.1/30");
    assertThat(
        CONFIG.getInterfaces().get("eth1").getIpAddresses(),
        equalTo(ImmutableList.of(ConcreteInterfaceAddress.parse("1.1.1.1/30"))));
  }

  @Test
  public void testFRRDefaultTraditional() {
    parse("frr defaults traditional\n");
  }

  @Test
  public void testNoIpv6Forwarding() {
    parse("no ipv6 forwarding\n");
  }

  @Test
  public void testPasswordEncryption() {
    parse("service password-encryption\n");
  }

  @Test
  public void testLog() {
    parse("log file /var/log/frr/frr.log\n");
    parse("log commands\n");
  }

  @Test
  public void testEnd() {
    parseLines("frr version", "end\n");
  }
}

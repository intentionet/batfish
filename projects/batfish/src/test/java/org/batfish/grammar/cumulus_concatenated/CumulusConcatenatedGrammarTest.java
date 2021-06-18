package org.batfish.grammar.cumulus_concatenated;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAddress;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.batfish.datamodel.routing_policy.Common.SUMMARY_ONLY_SUPPRESSION_POLICY_NAME;
import static org.batfish.datamodel.routing_policy.Environment.Direction.OUT;
import static org.batfish.main.BatfishTestUtils.TEST_SNAPSHOT;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.cumulus.CumulusConversions.DEFAULT_LOOPBACK_MTU;
import static org.batfish.representation.cumulus.CumulusConversions.DEFAULT_PORT_MTU;
import static org.batfish.representation.cumulus.CumulusConversions.computeBgpGenerationPolicyName;
import static org.batfish.representation.cumulus.CumulusConversions.computeMatchSuppressedSummaryOnlyPolicyName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ValueGraph;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.plugin.IBatfish;
import org.batfish.config.Settings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BddTestbed;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LinkLocalAddress;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.bgp.BgpAggregate;
import org.batfish.datamodel.bgp.BgpConfederation;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.grammar.GrammarSettings;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.cumulus.BgpVrfAddressFamilyAggregateNetworkConfiguration;
import org.batfish.representation.cumulus.CumulusConcatenatedConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class CumulusConcatenatedGrammarTest {
  private static final String INTERFACES_DELIMITER = "# This file describes the network interfaces";
  private static final String PORTS_DELIMITER = "# ports.conf --";
  private static final String FRR_DELIMITER = "frr version 4.0+cl3u8";

  private static final String TESTCONFIGS_PREFIX =
      "org/batfish/grammar/cumulus_concatenated/testconfigs/";

  private static final String TESTRIGS_PREFIX =
      "org/batfish/grammar/cumulus_concatenated/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private final BddTestbed _bddTestbed = new BddTestbed(ImmutableMap.of(), ImmutableMap.of());
  private final IpSpaceToBDD _dstIpBdd = _bddTestbed.getDstIpBdd();

  private static CumulusConcatenatedConfiguration parseFromTextWithSettings(
      String src, Settings settings) {
    CumulusConcatenatedCombinedParser parser = new CumulusConcatenatedCombinedParser(src, settings);
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    CumulusConcatenatedControlPlaneExtractor extractor =
        new CumulusConcatenatedControlPlaneExtractor(
            src, new Warnings(), "", settings, null, false, new SilentSyntaxCollection());
    extractor.processParseTree(TEST_SNAPSHOT, tree);
    return SerializationUtils.clone(
        (CumulusConcatenatedConfiguration) extractor.getVendorConfiguration());
  }

  private static CumulusConcatenatedConfiguration parse(String src) {
    Settings settings = new Settings();
    settings.setDisableUnrecognized(true);
    settings.setThrowOnLexerError(true);
    settings.setThrowOnParserError(true);

    return parseFromTextWithSettings(src, settings);
  }

  private static CumulusConcatenatedConfiguration parseLines(String... lines) {
    return parse(String.join("\n", lines) + "\n");
  }

  private static CumulusConcatenatedConfiguration parseVendorConfig(String filename) {
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    return parseVendorConfig(filename, settings);
  }

  private static CumulusConcatenatedConfiguration parseVendorConfig(
      String filename, GrammarSettings settings) {
    String src = readResource(TESTCONFIGS_PREFIX + filename, UTF_8);
    CumulusConcatenatedCombinedParser parser = new CumulusConcatenatedCombinedParser(src, settings);
    CumulusConcatenatedControlPlaneExtractor extractor =
        new CumulusConcatenatedControlPlaneExtractor(
            src,
            new Warnings(),
            filename,
            parser.getSettings(),
            null,
            false,
            new SilentSyntaxCollection());
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(TEST_SNAPSHOT, tree);
    CumulusConcatenatedConfiguration config =
        (CumulusConcatenatedConfiguration) extractor.getVendorConfiguration();
    config.setFilename(TESTCONFIGS_PREFIX + filename);
    return config;
  }

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private SortedMap<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    IBatfish iBatfish = getBatfishForConfigurationNames(configurationNames);
    return iBatfish.loadConfigurations(iBatfish.getSnapshot());
  }

  private @Nonnull Configuration parseConfig(String hostname) throws IOException {
    Map<String, Configuration> configs = parseTextConfigs(hostname);
    String canonicalHostname = hostname.toLowerCase();
    assertThat(configs, hasEntry(equalTo(canonicalHostname), hasHostname(canonicalHostname)));
    return configs.get(canonicalHostname);
  }

  private @Nonnull Bgpv4Route processRouteIn(RoutingPolicy routingPolicy, Bgpv4Route route) {
    Bgpv4Route.Builder builder = route.toBuilder();
    assertTrue(routingPolicy.process(route, builder, Direction.IN));
    return builder.build();
  }

  @Test
  public void testConcatenation() {
    CumulusConcatenatedConfiguration cfg = parseVendorConfig("concatenation");
    assertThat(cfg.getHostname(), equalTo("hostname"));
  }

  @Test
  public void testConcatenationWithLeadingGarbage() {
    CumulusConcatenatedConfiguration cfg = parseVendorConfig("concatenation_with_leading_garbage");
    assertThat(cfg.getHostname(), equalTo("hostname"));
  }

  @Test
  public void testConcatenationWithMissingHostname() {
    CumulusConcatenatedConfiguration cfg = parseVendorConfig("concatenation_with_missing_hostname");
    assertThat(cfg.getHostname(), emptyString());
  }

  @Test
  public void testPortsUnrecognized() {
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    settings.setDisableUnrecognized(false);
    settings.setThrowOnLexerError(false);
    settings.setThrowOnParserError(false);
    CumulusConcatenatedConfiguration cfg = parseVendorConfig("ports_unrecognized", settings);
    assertThat(cfg.getHostname(), equalTo("hostname"));
  }

  @Test
  public void testBgpAggregateAddress_e2e() {
    CumulusConcatenatedConfiguration vsConfig = parseVendorConfig("bgp_aggregate_address");
    Configuration viConfig = vsConfig.toVendorIndependentConfigurations().get(0);
    Vrf vrf = viConfig.getDefaultVrf();

    Prefix prefix1 = Prefix.parse("1.1.1.0/24");
    Prefix prefix2 = Prefix.parse("2.2.0.0/16");

    // Test that the expected routes maps were generated. We test their semantics elsewhere.
    assertThat(
        viConfig.getRoutingPolicies(),
        hasKey(
            computeBgpGenerationPolicyName(
                true, Configuration.DEFAULT_VRF_NAME, prefix1.toString())));
    assertThat(
        viConfig.getRoutingPolicies(),
        hasKey(
            computeBgpGenerationPolicyName(
                true, Configuration.DEFAULT_VRF_NAME, prefix2.toString())));

    // Test that expected generated routes exist
    assertThat(
        vrf.getGeneratedRoutes().stream()
            .map(GeneratedRoute::getNetwork)
            .collect(ImmutableList.toImmutableList()),
        containsInAnyOrder(prefix1, prefix2));

    // suppression route map exists. Semantics tested elsewhere
    assertThat(
        viConfig.getRouteFilterLists(),
        hasKey(computeMatchSuppressedSummaryOnlyPolicyName(vrf.getName())));
  }

  @Test
  public void testVrf() {
    CumulusConcatenatedConfiguration c =
        parseLines(
            "hostname",
            INTERFACES_DELIMITER,
            // declare vrf1
            "iface vrf1",
            "  vrf-table auto",
            PORTS_DELIMITER,
            FRR_DELIMITER,
            // add definition
            "vrf vrf1",
            "  vni 1000",
            "exit-vrf");
    assertThat(c.getFrrConfiguration().getVrfs().get("vrf1").getVni(), equalTo(1000));
  }

  @Test
  public void testBgpConfederationConversion() throws IOException {
    Configuration c = parseConfig("bgp_confederation");
    BgpProcess bgpProcess = c.getDefaultVrf().getBgpProcess();
    assertThat(
        bgpProcess.getConfederation(), equalTo(new BgpConfederation(12, ImmutableSet.of(65000L))));
    BgpActivePeerConfig neighbor = bgpProcess.getActiveNeighbors().get(Prefix.parse("1.1.1.1/32"));
    assertThat(neighbor.getConfederationAsn(), equalTo(12L));
    assertThat(neighbor.getLocalAs(), equalTo(65000L));
  }

  @Test
  public void testInterfaces() throws IOException {
    Configuration c = parseConfig("interface_test");

    assertThat(c.getAllInterfaces().keySet(), contains("lo", "swp1", "swp2"));

    Interface lo = c.getAllInterfaces().get("lo");
    assertThat(lo, allOf(hasAddress("1.1.1.1/32"), hasMtu(DEFAULT_LOOPBACK_MTU)));

    Interface swp1 = c.getAllInterfaces().get("swp1");
    assertThat(swp1, allOf(hasAddress("2.2.2.2/24"), hasMtu(DEFAULT_PORT_MTU)));

    Interface swp2 = c.getAllInterfaces().get("swp2");
    assertEquals(swp2.getAddress(), ConcreteInterfaceAddress.parse("3.3.3.3/24"));
    assertEquals(swp2.getSpeed(), Double.valueOf(10000 * 10e6));
  }

  @Test
  public void testStaticRoute() {
    CumulusConcatenatedConfiguration vsConfig = parseVendorConfig("static_route");
    Configuration viConfig = vsConfig.toVendorIndependentConfigurations().get(0);
    assertThat(
        viConfig.getDefaultVrf().getStaticRoutes(),
        equalTo(
            ImmutableSet.of(
                StaticRoute.testBuilder()
                    .setNetwork(Prefix.parse("1.1.1.1/24"))
                    .setNextHopIp(Ip.parse("10.0.0.1"))
                    .setAdministrativeCost(100)
                    .build(),
                StaticRoute.testBuilder()
                    .setNetwork(Prefix.parse("3.3.3.3/24"))
                    .setNextHopInterface("null_interface")
                    .setAdministrativeCost(1)
                    .build(),
                StaticRoute.testBuilder()
                    .setNetwork(Prefix.parse("4.4.4.4/24"))
                    .setNextHopInterface("Eth0")
                    .setAdministrativeCost(1)
                    .build(),
                StaticRoute.testBuilder()
                    .setNetwork(Prefix.parse("6.6.6.6/24"))
                    .setNextHopInterface("null_interface")
                    .setAdministrativeCost(1)
                    .build(),
                StaticRoute.testBuilder()
                    .setNetwork(Prefix.parse("7.7.7.7/24"))
                    .setNextHopInterface("null_interface")
                    .setAdministrativeCost(250)
                    .build())));
    assertThat(
        viConfig.getVrfs().get("VRF").getStaticRoutes(),
        equalTo(
            ImmutableSet.of(
                StaticRoute.testBuilder()
                    .setNetwork(Prefix.parse("2.2.2.2/24"))
                    .setNextHopIp(Ip.parse("10.0.0.2"))
                    .setAdministrativeCost(1)
                    .build(),
                StaticRoute.testBuilder()
                    .setNetwork(Prefix.parse("5.5.5.5/24"))
                    .setNextHopInterface("eth0-1")
                    .setAdministrativeCost(1)
                    .build())));
  }

  @Test
  public void testBgpSessionUpdateSource() throws IOException {
    String testrigName = "bgp_update_source";
    List<String> configurationNames = ImmutableList.of("n1", "n2");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        batfish.getTopologyProvider().getBgpTopology(snapshot).getGraph();

    String vrf = "default";
    // Edge one direction
    assertThat(
        bgpTopology
            .adjacentNodes(new BgpPeerConfigId("n1", vrf, Prefix.parse("10.0.0.2/32"), false))
            .iterator()
            .next(),
        equalTo(new BgpPeerConfigId("n2", vrf, Prefix.parse("10.0.0.1/32"), false)));

    // Edge the other direction
    assertThat(
        bgpTopology
            .adjacentNodes(new BgpPeerConfigId("n2", vrf, Prefix.parse("10.0.0.1/32"), false))
            .iterator()
            .next(),
        equalTo(new BgpPeerConfigId("n1", vrf, Prefix.parse("10.0.0.2/32"), false)));
  }

  @Test
  public void testSetCommunityAdditive() throws IOException {
    Ip origNextHopIp = Ip.parse("192.0.2.254");
    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setAsPath(AsPath.ofSingletonAsSets(2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(origNextHopIp)
            .setNetwork(Prefix.parse("10.20.30.0/31"))
            .setTag(0L)
            .build();
    Configuration c = parseConfig("set_community_additive_test");
    RoutingPolicy rp1 = c.getRoutingPolicies().get("RM_SET_ADDITIVE_TEST_1");
    RoutingPolicy rp2 = c.getRoutingPolicies().get("RM_SET_ADDITIVE_TEST_2");
    RoutingPolicy rp3 = c.getRoutingPolicies().get("RM_SET_ADDITIVE_TEST_3");
    Bgpv4Route inRoute =
        base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(4, 4))).build();
    Bgpv4Route outputRoute1 = processRouteIn(rp1, inRoute);
    Bgpv4Route outputRoute2 = processRouteIn(rp2, inRoute);
    Bgpv4Route outputRoute3 = processRouteIn(rp3, inRoute);
    assertThat(
        outputRoute1.getCommunities().getCommunities(),
        containsInAnyOrder(
            StandardCommunity.of(2, 2), StandardCommunity.of(3, 3), StandardCommunity.of(4, 4)));
    assertThat(
        outputRoute2.getCommunities().getCommunities(), contains(StandardCommunity.of(1, 1)));
    assertThat(
        outputRoute3.getCommunities().getCommunities(),
        containsInAnyOrder(
            StandardCommunity.of(2, 2), StandardCommunity.of(3, 3), StandardCommunity.of(4, 4)));
  }

  @Test
  public void testSetCommunityContinue() throws IOException {
    Ip origNextHopIp = Ip.parse("192.0.2.254");
    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setAsPath(AsPath.ofSingletonAsSets(2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(origNextHopIp)
            .setNetwork(Prefix.parse("10.20.30.0/31"))
            .setTag(0L)
            .build();
    Configuration c = parseConfig("set_community_additive_continue");
    RoutingPolicy rp1 = c.getRoutingPolicies().get("RM_SET_ADDITIVE_TEST_1");
    Bgpv4Route inRoute =
        base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(4, 4))).build();
    Bgpv4Route outputRoute1 = processRouteIn(rp1, inRoute);
    assertThat(
        outputRoute1.getCommunities().getCommunities(),
        containsInAnyOrder(
            StandardCommunity.of(2, 2),
            StandardCommunity.of(3, 3),
            StandardCommunity.of(4, 4),
            StandardCommunity.of(7, 7)));
  }

  @Test
  public void testSetMetric() throws IOException {
    Ip origNextHopIp = Ip.parse("192.0.2.254");
    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setAsPath(AsPath.ofSingletonAsSets(2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(origNextHopIp)
            .setNetwork(Prefix.parse("10.20.30.0/31"))
            .setTag(0L)
            .setMetric(2L)
            .build();
    Configuration c = parseConfig("set_metric_test");
    RoutingPolicy rp1 = c.getRoutingPolicies().get("RM_METRIC_TEST");
    RoutingPolicy rp2 = c.getRoutingPolicies().get("RM_METRIC_PLUS_TEST");
    RoutingPolicy rp3 = c.getRoutingPolicies().get("RM_METRIC_MINUS_TEST");
    RoutingPolicy rp4 = c.getRoutingPolicies().get("RM_METRIC_OVERFLOW_TEST");
    RoutingPolicy rp5 = c.getRoutingPolicies().get("RM_METRIC_UNDERFLOW_TEST");
    Bgpv4Route inRoute =
        base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(4, 4))).build();
    Bgpv4Route outputRoute1 = processRouteIn(rp1, inRoute);
    Bgpv4Route outputRoute2 = processRouteIn(rp2, inRoute);
    Bgpv4Route outputRoute3 = processRouteIn(rp3, inRoute);
    Bgpv4Route outputRoute4 = processRouteIn(rp4, inRoute);
    Bgpv4Route outputRoute5 = processRouteIn(rp5, inRoute);
    assertThat(outputRoute1.getMetric(), equalTo(10L));
    assertThat(outputRoute2.getMetric(), equalTo(3L));
    assertThat(outputRoute3.getMetric(), equalTo(1L));
    assertThat(outputRoute4.getMetric(), equalTo(0xFFFFFFFFL));
    assertThat(outputRoute5.getMetric(), equalTo(0L));
  }

  @Test
  public void testSetCommListDelete() throws IOException {
    Ip origNextHopIp = Ip.parse("192.0.2.254");
    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setAsPath(AsPath.ofSingletonAsSets(2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(origNextHopIp)
            .setNetwork(Prefix.parse("10.20.30.0/31"))
            .setTag(0L)
            .build();
    Configuration c = parseConfig("set_comm_list_delete_test");
    Bgpv4Route inRoute =
        base.toBuilder()
            .setCommunities(
                ImmutableSet.of(
                    StandardCommunity.of(1, 1),
                    StandardCommunity.of(1, 2),
                    StandardCommunity.of(2, 1),
                    StandardCommunity.of(2, 2),
                    StandardCommunity.of(3, 1),
                    StandardCommunity.of(3, 2)))
            .build();

    // RMs using expanded comm-lists.
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("RM_EXPANDED_TEST_DELETE_ALL_COMMUNITIES");
      assertThat(processRouteIn(rp, inRoute).getCommunities().getCommunities(), empty());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("RM_EXPANDED_TEST_DELETE_COMM_BEGIN_WITH_1");
      assertThat(
          processRouteIn(rp, inRoute).getCommunities().getCommunities(),
          contains(
              StandardCommunity.of(2, 1),
              StandardCommunity.of(2, 2),
              StandardCommunity.of(3, 1),
              StandardCommunity.of(3, 2)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("RM_EXPANDED_TEST_DELETE_COMM_BEGIN_WITH_2");
      assertThat(
          processRouteIn(rp, inRoute).getCommunities().getCommunities(),
          contains(
              StandardCommunity.of(1, 1),
              StandardCommunity.of(1, 2),
              StandardCommunity.of(3, 1),
              StandardCommunity.of(3, 2)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("RM_EXPANDED_TEST_DELETE_COMM_BEGIN_WITH_3");
      assertThat(
          processRouteIn(rp, inRoute).getCommunities().getCommunities(),
          contains(
              StandardCommunity.of(1, 1),
              StandardCommunity.of(1, 2),
              StandardCommunity.of(2, 1),
              StandardCommunity.of(2, 2)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("RM_EXPANDED_TEST_DELETE_COMM_DENY_PERMIT");
      assertThat(
          processRouteIn(rp, inRoute).getCommunities().getCommunities(),
          contains(
              StandardCommunity.of(1, 1),
              StandardCommunity.of(1, 2),
              StandardCommunity.of(3, 1),
              StandardCommunity.of(3, 2)));
    }

    // RMs using standard comm-lists.
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("RM_STANDARD_TEST_DELETE_COMM_1_1");
      assertThat(
          processRouteIn(rp, inRoute).getCommunities().getCommunities(),
          contains(
              StandardCommunity.of(1, 2),
              StandardCommunity.of(2, 1),
              StandardCommunity.of(2, 2),
              StandardCommunity.of(3, 1),
              StandardCommunity.of(3, 2)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("RM_STANDARD_TEST_DELETE_COMM_2_1");
      assertThat(
          processRouteIn(rp, inRoute).getCommunities().getCommunities(),
          contains(
              StandardCommunity.of(1, 1),
              StandardCommunity.of(1, 2),
              StandardCommunity.of(2, 2),
              StandardCommunity.of(3, 1),
              StandardCommunity.of(3, 2)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("RM_STANDARD_TEST_DELETE_COMM_3_1");
      assertThat(
          processRouteIn(rp, inRoute).getCommunities().getCommunities(),
          contains(
              StandardCommunity.of(1, 1),
              StandardCommunity.of(1, 2),
              StandardCommunity.of(2, 1),
              StandardCommunity.of(2, 2),
              StandardCommunity.of(3, 2)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("RM_STANDARD_TEST_DELETE_COMM_DENY_PERMIT");
      assertThat(
          processRouteIn(rp, inRoute).getCommunities().getCommunities(),
          contains(
              StandardCommunity.of(1, 1),
              StandardCommunity.of(2, 1),
              StandardCommunity.of(2, 2),
              StandardCommunity.of(3, 1),
              StandardCommunity.of(3, 2)));
    }
  }

  @Test
  public void testCommSetMatchExpr() throws IOException {
    Ip origNextHopIp = Ip.parse("192.0.2.254");
    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setAsPath(AsPath.ofSingletonAsSets(2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(origNextHopIp)
            .setNetwork(Prefix.parse("10.20.30.0/31"))
            .setTag(0L)
            .build();
    Configuration c = parseConfig("comm_set_match_expr_test");

    // Route-map with match on comm-list with single community.
    // Input route has the same community.
    {
      Bgpv4Route inRoute =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(1, 1))).build();

      // Use standard comm-lists.
      RoutingPolicy rp = c.getRoutingPolicies().get("Standard_RM1");
      assertThat(processRouteIn(rp, inRoute).getMetric(), equalTo(1L));

      // Use expanded comm-lists.
      rp = c.getRoutingPolicies().get("Expanded_RM1");
      assertThat(processRouteIn(rp, inRoute).getMetric(), equalTo(1L));
    }

    // Route-map with match on comm-list with multiple communities.
    // Input route has the same communities.
    {
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2)))
              .build();

      // Use standard comm-lists.
      RoutingPolicy rp = c.getRoutingPolicies().get("Standard_RM2");
      assertThat(processRouteIn(rp, inRoute).getMetric(), equalTo(2L));

      // Use expanded comm-lists.
      rp = c.getRoutingPolicies().get("Expanded_RM2");
      assertThat(processRouteIn(rp, inRoute).getMetric(), equalTo(2L));
    }

    // Route-map with match on comm-list with single community.
    // Input route has additional communities.
    {
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(
                      StandardCommunity.of(1, 1),
                      StandardCommunity.of(2, 2),
                      StandardCommunity.of(3, 3)))
              .build();

      // Use standard comm-lists.
      RoutingPolicy rp = c.getRoutingPolicies().get("Standard_RM2");
      assertThat(processRouteIn(rp, inRoute).getMetric(), equalTo(2L));

      // Use expanded comm-lists.
      rp = c.getRoutingPolicies().get("Expanded_RM2");
      assertThat(processRouteIn(rp, inRoute).getMetric(), equalTo(2L));
    }

    // Route-map with match on comm-list with communities.
    // Input route partially matches comm-list.
    {
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.of(3, 3)))
              .build();

      // Use standard comm-lists.
      RoutingPolicy rp = c.getRoutingPolicies().get("Standard_RM2");
      Bgpv4Route.Builder builder = inRoute.toBuilder();
      rp.process(inRoute, builder, Direction.IN);
      assertThat(builder.build().getMetric(), equalTo(0L));

      // Use expanded comm-lists.
      rp = c.getRoutingPolicies().get("Expanded_RM2");
      builder = inRoute.toBuilder();
      rp.process(inRoute, builder, Direction.IN);
      assertThat(builder.build().getMetric(), equalTo(0L));
    }

    // Route-map with match on comm-list with deny and permit statements on communities.
    // Input route has a community that matches the deny followed by permit.
    {
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2)))
              .build();

      // Use standard comm-lists.
      RoutingPolicy rp = c.getRoutingPolicies().get("Standard_RM3");
      Bgpv4Route.Builder builder = inRoute.toBuilder();
      rp.process(inRoute, builder, Direction.IN);
      assertThat(builder.build().getMetric(), equalTo(0L));

      // Use expanded comm-lists.
      rp = c.getRoutingPolicies().get("Expanded_RM3");
      builder = inRoute.toBuilder();
      rp.process(inRoute, builder, Direction.IN);
      assertThat(builder.build().getMetric(), equalTo(0L));
    }

    // Test a route-map with expanded comm-lists against an input route whose communities don't
    // satisfy its regex.
    {
      Bgpv4Route inRoute =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(3, 3))).build();

      RoutingPolicy rp = c.getRoutingPolicies().get("Expanded_RM1");
      Bgpv4Route.Builder builder = inRoute.toBuilder();
      rp.process(inRoute, builder, Direction.IN);
      assertThat(builder.build().getMetric(), equalTo(0L));
    }
  }

  @Test
  public void testInterfaceDefinition() throws IOException {
    Configuration c = parseConfig("interface_definition_test");
    assertThat(
        c.getActiveInterfaces()
            .get("eth1")
            .getVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Ip.parse("10.20.40.0").toPrefix())
            .getIpv4UnicastAddressFamily()
            .getExportPolicySources(),
        hasSize(1));
    assertThat(
        c.getActiveInterfaces()
            .get("bond2")
            .getVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Ip.parse("10.20.50.0").toPrefix())
            .getIpv4UnicastAddressFamily()
            .getExportPolicySources(),
        hasSize(1));
    assertThat(
        c.getActiveInterfaces()
            .get("eth3")
            .getVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Ip.parse("10.20.60.0").toPrefix())
            .getIpv4UnicastAddressFamily()
            .getExportPolicySources(),
        hasSize(1));
    assertThat(
        c.getActiveInterfaces()
            .get("bond4")
            .getVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Ip.parse("10.20.70.0").toPrefix())
            .getIpv4UnicastAddressFamily()
            .getExportPolicySources(),
        hasSize(1));
  }

  @Test
  public void testOptionalAddressFamily() throws IOException {
    Configuration c = parseConfig("optional_address_family_identifier");
    assertThat(
        c.getActiveInterfaces()
            .get("eth1")
            .getVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Ip.parse("10.20.50.0").toPrefix())
            .getIpv4UnicastAddressFamily()
            .getExportPolicySources(),
        hasSize(1));
    assertThat(
        c.getActiveInterfaces()
            .get("eth2")
            .getVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Ip.parse("10.20.60.0").toPrefix())
            .getIpv4UnicastAddressFamily()
            .getExportPolicySources(),
        hasSize(1));
  }

  @Test
  public void testOspfAreaNetwork() throws IOException {
    Configuration c = parseConfig("ospf-network-area");
    assertThat(c.getAllInterfaces().get("eth1").getOspfAreaName(), equalTo(10L));
  }

  @Test
  public void testLocalAs() throws IOException {
    Configuration c = parseConfig("local_as_test");
    org.batfish.datamodel.Vrf defaultVrf = c.getDefaultVrf();
    Long neighbor_ip_local_as =
        defaultVrf
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Prefix.parse("2.2.2.2/32"))
            .getLocalAs();
    assertThat(neighbor_ip_local_as, equalTo(10L));
    Long neighbor_iface_local_as =
        defaultVrf.getBgpProcess().getInterfaceNeighbors().get("bond2").getLocalAs();
    assertThat(neighbor_iface_local_as, equalTo(10L));
  }

  @Test
  public void testLocalAsWarn() throws IOException {
    String hostname = "local_as_test_warn";
    IBatfish batfish = getBatfishForConfigurationNames(hostname);
    String filename = "configs/" + hostname;
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    // should get two copies of this warning
    assertThat(
        pvcae.getWarnings().get(filename).getParseWarnings().stream()
            .filter(
                w ->
                    w.getComment()
                        .equals("local-as is supported only in 'no-prepend replace-as' mode"))
            .count(),
        equalTo(2L));
  }

  @Test
  public void testRouteMapMatchTagNonBgp() throws IOException {
    Configuration c = parseConfig("frr-match-tag-non-bgp");
    RoutingPolicy policy = c.getRoutingPolicies().get("SET_METRIC");
    // Don't crash
    policy.process(
        new ConnectedRoute(Prefix.parse("1.1.1.0/24"), "iface"),
        OspfExternalType2Route.builder(),
        OUT);
  }

  @Test
  public void testIpReuse() throws IOException {
    Configuration c = parseConfig("ip_reuse");

    assertThat(
        c.getAllInterfaces(), hasKeys("swp1", "swp2", "swp3", "swp4", "swp5", "lo", "v1", "v2"));
    {
      Interface iface = c.getAllInterfaces().get("swp1");
      assertThat(iface.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.1.1.2/32")));
      assertThat(
          iface.getAllAddresses(),
          contains(
              ConcreteInterfaceAddress.parse("10.0.0.2/32"),
              ConcreteInterfaceAddress.parse("10.1.1.2/32")));
      assertThat(
          iface.getAdditionalArpIps().accept(_dstIpBdd),
          equalTo(
              AclIpSpace.union(Ip.parse("10.0.0.1").toIpSpace(), Ip.parse("10.1.1.1").toIpSpace())
                  .accept(_dstIpBdd)));
    }
    {
      Interface iface = c.getAllInterfaces().get("swp2");
      assertThat(iface.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.0.1/32")));
      assertThat(iface.getAllAddresses(), contains(ConcreteInterfaceAddress.parse("10.0.0.1/32")));
      assertThat(
          iface.getAdditionalArpIps().accept(_dstIpBdd),
          equalTo(
              AclIpSpace.union(Ip.parse("10.1.1.1").toIpSpace(), Ip.parse("10.1.1.2").toIpSpace())
                  .accept(_dstIpBdd)));
    }
    {
      Interface iface = c.getAllInterfaces().get("swp3");
      assertThat(iface.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.1.1.1/32")));
      assertThat(iface.getAllAddresses(), contains(ConcreteInterfaceAddress.parse("10.1.1.1/32")));
      assertThat(
          iface.getAdditionalArpIps().accept(_dstIpBdd),
          equalTo(
              AclIpSpace.union(Ip.parse("10.0.0.1").toIpSpace(), Ip.parse("10.0.0.2").toIpSpace())
                  .accept(_dstIpBdd)));
    }
    {
      Interface iface = c.getAllInterfaces().get("swp4");
      assertThat(iface.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.0.1/32")));
      assertThat(iface.getAllAddresses(), contains(ConcreteInterfaceAddress.parse("10.0.0.1/32")));
      assertThat(
          iface.getAdditionalArpIps().accept(_dstIpBdd),
          equalTo(EmptyIpSpace.INSTANCE.accept(_dstIpBdd)));
    }
    {
      Interface iface = c.getAllInterfaces().get("swp5");
      assertThat(iface.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.1.1.1/32")));
      assertThat(iface.getAllAddresses(), contains(ConcreteInterfaceAddress.parse("10.1.1.1/32")));
      assertThat(
          iface.getAdditionalArpIps().accept(_dstIpBdd),
          equalTo(EmptyIpSpace.INSTANCE.accept(_dstIpBdd)));
    }
  }

  @Test
  public void testOspfAddresses() throws IOException {
    Configuration c = parseConfig("ip_reuse");
    assertThat(
        c.getAllInterfaces(), hasKeys("swp1", "swp2", "swp3", "swp4", "swp5", "lo", "v1", "v2"));
    {
      Interface iface = c.getAllInterfaces().get("swp1");
      assertNotNull(iface.getOspfSettings());
      assertThat(
          iface.getOspfSettings().getOspfAddresses().getAddresses(),
          contains(
              ConcreteInterfaceAddress.parse("10.0.0.1/32"),
              ConcreteInterfaceAddress.parse("10.0.0.2/32"),
              ConcreteInterfaceAddress.parse("10.1.1.1/32"),
              ConcreteInterfaceAddress.parse("10.1.1.2/32")));
    }
    {
      Interface iface = c.getAllInterfaces().get("swp2");
      assertNotNull(iface.getOspfSettings());
      assertThat(
          iface.getOspfSettings().getOspfAddresses().getAddresses(),
          contains(
              ConcreteInterfaceAddress.parse("10.0.0.1/32"),
              ConcreteInterfaceAddress.parse("10.1.1.1/32"),
              ConcreteInterfaceAddress.parse("10.1.1.2/32")));
    }
    {
      Interface iface = c.getAllInterfaces().get("swp3");
      assertNotNull(iface.getOspfSettings());
      assertThat(
          iface.getOspfSettings().getOspfAddresses().getAddresses(),
          contains(
              ConcreteInterfaceAddress.parse("10.0.0.1/32"),
              ConcreteInterfaceAddress.parse("10.0.0.2/32"),
              ConcreteInterfaceAddress.parse("10.1.1.1/32")));
    }
    {
      Interface iface = c.getAllInterfaces().get("swp4");
      assertNull(iface.getOspfSettings());
    }
    {
      Interface iface = c.getAllInterfaces().get("swp5");
      // TODO: support OSPF in another VRF
      assertNull(iface.getOspfSettings());
    }
  }

  @Test
  public void testOspfUnnumberedLLAs() throws IOException {
    Configuration c = parseConfig("ospf_link_local");
    assertThat(c.getAllInterfaces(), hasKeys("swp1", "swp2", "swp3", "swp4", "swp5", "swp6", "lo"));
    {
      Interface iface = c.getAllInterfaces().get("swp1");
      assertThat(iface.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.0.1/32")));
      assertThat(
          iface.getAllAddresses(),
          containsInAnyOrder(
              equalTo(ConcreteInterfaceAddress.parse("10.0.0.1/32")),
              instanceOf(LinkLocalAddress.class)));
    }
    {
      Interface iface = c.getAllInterfaces().get("swp2");
      assertThat(iface.getAddress(), instanceOf(LinkLocalAddress.class));
      assertThat(iface.getAllAddresses(), contains(instanceOf(LinkLocalAddress.class)));
    }
    {
      Interface iface = c.getAllInterfaces().get("swp3");
      assertNull(iface.getAddress());
      assertThat(iface.getAllAddresses(), empty());
    }
    {
      Interface iface = c.getAllInterfaces().get("swp4");
      assertNull(iface.getAddress());
      assertThat(iface.getAllAddresses(), empty());
    }
    {
      Interface iface = c.getAllInterfaces().get("swp5");
      assertNull(iface.getAddress());
      assertThat(iface.getAllAddresses(), empty());
    }
    {
      Interface iface = c.getAllInterfaces().get("swp6");
      assertNull(iface.getAddress());
      assertThat(iface.getAllAddresses(), empty());
    }
    {
      Interface iface = c.getAllInterfaces().get("lo");
      assertNull(iface.getAddress());
      assertThat(iface.getAllAddresses(), empty());
    }
  }

  @Test
  public void testLoopbackInterfaceType() throws IOException {
    Configuration c = parseConfig("loopback");
    assertThat(c.getAllInterfaces(), hasKeys("lo"));
    assertThat(c.getAllInterfaces().get("lo").getInterfaceType(), equalTo(InterfaceType.LOOPBACK));
  }

  @Test
  public void testAggregateAddressExtraction() {
    String hostname = "frr-aggregate-address";
    CumulusConcatenatedConfiguration vc = parseVendorConfig(hostname);
    Map<Prefix, BgpVrfAddressFamilyAggregateNetworkConfiguration> aggs =
        vc.getBgpProcess().getDefaultVrf().getIpv4Unicast().getAggregateNetworks();
    assertThat(aggs, aMapWithSize(9));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("1.1.0.0/16")),
            equalTo(
                new BgpVrfAddressFamilyAggregateNetworkConfiguration(
                    false, false, null, null, false, null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("1.2.0.0/16")),
            equalTo(
                new BgpVrfAddressFamilyAggregateNetworkConfiguration(
                    false, false, null, "rm1", false, null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("2.1.0.0/16")),
            equalTo(
                new BgpVrfAddressFamilyAggregateNetworkConfiguration(
                    true, false, null, null, false, null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("2.2.0.0/16")),
            equalTo(
                new BgpVrfAddressFamilyAggregateNetworkConfiguration(
                    true, false, null, "rm2", false, null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.1.0.0/16")),
            equalTo(
                new BgpVrfAddressFamilyAggregateNetworkConfiguration(
                    false, false, null, null, true, null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.2.0.0/16")),
            equalTo(
                new BgpVrfAddressFamilyAggregateNetworkConfiguration(
                    false, false, null, null, false, "sm1"))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.3.0.0/16")),
            equalTo(
                new BgpVrfAddressFamilyAggregateNetworkConfiguration(
                    false, false, null, null, true, "sm2"))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("4.0.0.0/16")),
            equalTo(
                new BgpVrfAddressFamilyAggregateNetworkConfiguration(
                    false, false, null, "undefined", false, "undefined"))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("5.0.0.0/16")),
            equalTo(
                new BgpVrfAddressFamilyAggregateNetworkConfiguration(
                    false, true, OriginType.INCOMPLETE, null, false, null))));
  }

  @Test
  public void testAggregateAddressConversion() throws IOException {
    String hostname = "frr-aggregate-address";
    Configuration c = parseConfig(hostname);

    Map<Prefix, BgpAggregate> aggs = c.getDefaultVrf().getBgpProcess().getAggregates();
    assertThat(aggs, aMapWithSize(9));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("1.1.0.0/16")),
            equalTo(BgpAggregate.of(Prefix.parse("1.1.0.0/16"), null, null, null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("1.2.0.0/16")),
            equalTo(BgpAggregate.of(Prefix.parse("1.2.0.0/16"), null, null, "rm1"))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("2.1.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("2.1.0.0/16"),
                    null,
                    // TODO: generation policy should incorporate as-set
                    null,
                    null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("2.2.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("2.2.0.0/16"),
                    null,
                    // TODO: generation policy should incorporate as-set
                    null,
                    "rm2"))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.1.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("3.1.0.0/16"),
                    SUMMARY_ONLY_SUPPRESSION_POLICY_NAME,
                    null,
                    null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.2.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("3.2.0.0/16"),
                    // TODO: suppression policy should incorporate suppress-map
                    null,
                    null,
                    null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.3.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("3.3.0.0/16"),
                    // TODO: suppression policy should incorporate suppress-map and ignore
                    //       summary-only.
                    SUMMARY_ONLY_SUPPRESSION_POLICY_NAME,
                    null,
                    null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("4.0.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("4.0.0.0/16"),
                    // TODO: verify undefined route-map treated as omitted
                    null,
                    null,
                    null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("5.0.0.0/16")),
            // TODO: implement matching-med-only and origin
            equalTo(BgpAggregate.of(Prefix.parse("5.0.0.0/16"), null, null, null))));
  }
}

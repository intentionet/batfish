package org.batfish.grammar.f5_bigip_imish;

import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.Prefix.MAX_PREFIX_LENGTH;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasMetric;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopIp;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasDescription;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalIp;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEquivalentAsPathMatchMode;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbors;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasRouterId;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasCommunities;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasOriginType;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.isBgpRouteThat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.KernelRouteMatchers.isKernelRouteThat;
import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.representation.f5_bigip.F5BigipConfiguration.computeAccessListRouteFilterName;
import static org.batfish.representation.f5_bigip.F5BigipConfiguration.computeBgpPeerExportPolicyName;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.BGP_NEIGHBOR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.BGP_PROCESS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.ROUTE_MAP;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishLogger;
import org.batfish.common.ParseTreeSentences;
import org.batfish.common.Warnings;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDPrefix;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.KernelRoute;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredCombinedParser;
import org.batfish.grammar.f5_bigip_structured.F5BigipStructuredControlPlaneExtractor;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.f5_bigip.F5BigipConfiguration;
import org.batfish.representation.f5_bigip.PrefixList;
import org.batfish.representation.f5_bigip.PrefixListEntry;
import org.batfish.representation.f5_bigip.RouteMap;
import org.batfish.representation.f5_bigip.RouteMapEntry;
import org.batfish.representation.f5_bigip.RouteMapMatchPrefixList;
import org.batfish.representation.f5_bigip.RouteMapSetMetric;
import org.batfish.representation.f5_bigip.RouteMapSetOrigin;
import org.batfish.vendor.VendorConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public final class F5BigipImishGrammarTest {
  private static final String SNAPSHOTS_PREFIX = "org/batfish/grammar/f5_bigip_imish/snapshots/";
  private static final String TESTCONFIGS_PREFIX =
      "org/batfish/grammar/f5_bigip_imish/testconfigs/";

  private BDDPrefix _bddPrefix;

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private void assertAcceptsKernelRoute(RoutingPolicy rp, Ip peerAddress) {
    BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
    assertTrue(
        rp.process(
            new KernelRoute(Prefix.ZERO),
            outputBuilder,
            peerAddress,
            Prefix.create(peerAddress, Prefix.MAX_PREFIX_LENGTH),
            Configuration.DEFAULT_VRF_NAME,
            Direction.OUT));
  }

  private void assertRejectsKernelRoute(RoutingPolicy rp, Ip peerAddress) {
    BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
    assertFalse(
        rp.process(
            new KernelRoute(Prefix.ZERO),
            outputBuilder,
            peerAddress,
            Prefix.create(peerAddress, Prefix.MAX_PREFIX_LENGTH),
            Configuration.DEFAULT_VRF_NAME,
            Direction.OUT));
  }

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private void initBddPrefix() {
    BDDPacket packet = new BDDPacket();
    _bddPrefix = new BDDPrefix(packet.getDstIp(), packet.getDscp());
  }

  private BgpRoute.Builder makeBgpOutputRouteBuilder() {
    return BgpRoute.builder()
        .setNetwork(Prefix.ZERO)
        .setOriginType(OriginType.INCOMPLETE)
        .setOriginatorIp(Ip.ZERO)
        .setProtocol(RoutingProtocol.BGP);
  }

  private @Nonnull BgpRoute makeBgpRoute(Prefix prefix) {
    return BgpRoute.builder()
        .setNetwork(prefix)
        .setOriginType(OriginType.INCOMPLETE)
        .setOriginatorIp(Ip.ZERO)
        .setProtocol(RoutingProtocol.BGP)
        .build();
  }

  private Configuration parseConfig(String hostname) throws IOException {
    return parseTextConfigs(hostname).get(hostname.toLowerCase());
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.parseTextConfigs(_folder, names);
  }

  private @Nonnull F5BigipConfiguration parseVendorConfig(String hostname) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    settings.setDisableUnrecognized(true);
    settings.setThrowOnLexerError(true);
    settings.setThrowOnParserError(true);
    F5BigipStructuredCombinedParser parser = new F5BigipStructuredCombinedParser(src, settings);
    ParseTreeSentences pts = new ParseTreeSentences();
    F5BigipStructuredControlPlaneExtractor extractor =
        new F5BigipStructuredControlPlaneExtractor(
            src,
            parser,
            new Warnings(),
            String.format("configs/%s", hostname),
            () -> pts,
            settings.getPrintParseTreeLineNums());
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(tree);
    assertThat(
        String.format("Ensure '%s' was successfully parsed", hostname),
        extractor.getVendorConfiguration(),
        notNullValue());
    VendorConfiguration vc = extractor.getVendorConfiguration();
    assertThat(vc, instanceOf(F5BigipConfiguration.class));
    return (F5BigipConfiguration) vc;
  }

  private @Nonnull BgpRoute processBgpRoute(RoutingPolicy rp1, Ip peerAddress) {
    BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
    assertTrue(
        rp1.process(
            makeBgpRoute(Prefix.ZERO),
            outputBuilder,
            peerAddress,
            Prefix.create(peerAddress, Prefix.MAX_PREFIX_LENGTH),
            Configuration.DEFAULT_VRF_NAME,
            Direction.OUT));
    return outputBuilder.build();
  }

  @Test
  public void testAccessList() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_access_list");
    String acl1Name = "acl1";
    String acl2Name = "acl2";

    // setup
    IpAccessListToBdd toBDD = toBDD();

    // acl1
    assertThat(c.getIpAccessLists(), hasKey(acl1Name));

    IpAccessList acl1 = c.getIpAccessLists().get(acl1Name);

    // acl1 should permit everything
    assertTrue(toBDD.toBdd(acl1).isOne());

    // acl2
    assertThat(c.getIpAccessLists(), hasKey(acl2Name));

    IpAccessList acl2 = c.getIpAccessLists().get(acl2Name);

    assertThat(
        "acl2 permits packets with source IP in 192.0.2.0/24 except 192.0.2.128",
        toBDD.toBdd(acl2),
        equalTo(
            toBDD.toBdd(
                new MatchHeaderSpace(
                    HeaderSpace.builder()
                        .setSrcIps(
                            AclIpSpace.difference(
                                Prefix.parse("192.0.2.0/24").toIpSpace(),
                                Ip.parse("192.0.2.128").toIpSpace()))
                        .build()))));
  }

  @Test
  public void testAccessListToRouteFilter() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_route_map");
    String acl1RfName = computeAccessListRouteFilterName("acl1");
    String acl2RfName = computeAccessListRouteFilterName("acl2");

    // setup
    initBddPrefix();

    // acl1
    assertThat(c.getRouteFilterLists(), hasKey(acl1RfName));

    RouteFilterList acl1Rf = c.getRouteFilterLists().get(acl1RfName);

    // acl1 should permit everything
    assertThat(
        _bddPrefix.permittedByRouteFilterList(acl1Rf),
        equalTo(
            _bddPrefix.inPrefixRange(
                new PrefixRange(Prefix.ZERO, new SubRange(0, Prefix.MAX_PREFIX_LENGTH)))));

    // acl2
    assertThat(c.getRouteFilterLists(), hasKey(acl2RfName));

    RouteFilterList acl2Rf = c.getRouteFilterLists().get(acl2RfName);

    assertThat(
        "acl2 permits packets with source IP in 192.0.2.0/24 except 192.0.2.128",
        _bddPrefix.permittedByRouteFilterList(acl2Rf),
        equalTo(
            _bddPrefix
                .inPrefixRange(
                    new PrefixRange(
                        Prefix.strict("192.0.2.0/24"), new SubRange(24, Prefix.MAX_PREFIX_LENGTH)))
                .and(
                    _bddPrefix
                        .inPrefixRange(
                            new PrefixRange(
                                Prefix.strict("192.0.2.128/32"),
                                new SubRange(Prefix.MAX_PREFIX_LENGTH, Prefix.MAX_PREFIX_LENGTH)))
                        .not())));
  }

  @Test
  public void testBgpAlwaysCompareMedExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_imish_bgp_always_compare_med");

    assertTrue(
        "Ensure always-compare-med is extracted",
        vc.getBgpProcesses().get("123").getAlwaysCompareMed());
  }

  @Test
  public void testBgpDeterministicMedExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_imish_bgp_deterministic_med");

    assertTrue(
        "Ensure deterministic-med is extracted",
        vc.getBgpProcesses().get("123").getDeterministicMed());
  }

  @Test
  public void testBgpKernelRouteRedistribution() throws IOException {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(SNAPSHOTS_PREFIX + "bgp_e2e", "r1", "r2")
                .build(),
            _folder);
    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> routes1 =
        dp.getRibs().get("r1").get(Configuration.DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> routes2 =
        dp.getRibs().get("r2").get(Configuration.DEFAULT_VRF_NAME).getRoutes();

    // kernel routes should be installed
    assertThat(routes1, hasItem(isKernelRouteThat(hasPrefix(Prefix.strict("10.0.0.1/32")))));
    assertThat(routes2, hasItem(isKernelRouteThat(hasPrefix(Prefix.strict("10.0.0.2/32")))));

    // kernel routes should be redistributed
    assertThat(routes1, hasItem(isBgpRouteThat(hasPrefix(Prefix.strict("10.0.0.2/32")))));
    assertThat(routes2, hasItem(isBgpRouteThat(hasPrefix(Prefix.strict("10.0.0.1/32")))));
  }

  @Test
  public void testBgpKernelRouteRedistributionNoRouteMap() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_bgp_redistribute_kernel_no_route_map");
    Ip peerAddress = Ip.parse("192.0.2.2");
    String rpName = computeBgpPeerExportPolicyName("1", peerAddress);

    assertThat(c.getRoutingPolicies(), hasKey(rpName));

    RoutingPolicy rp = c.getRoutingPolicies().get(rpName);

    assertAcceptsKernelRoute(rp, peerAddress);
  }

  @Test
  public void testBgpKernelRouteRedistributionRouteMapAccept() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_bgp_redistribute_kernel_route_map_accept");
    Ip peerAddress = Ip.parse("192.0.2.2");
    String rpName = computeBgpPeerExportPolicyName("1", peerAddress);

    assertThat(c.getRoutingPolicies(), hasKey(rpName));

    RoutingPolicy rp = c.getRoutingPolicies().get(rpName);

    assertAcceptsKernelRoute(rp, peerAddress);
  }

  @Test
  public void testBgpKernelRouteRedistributionRouteMapReject() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_bgp_redistribute_kernel_route_map_reject");
    Ip peerAddress = Ip.parse("192.0.2.2");
    String rpName = computeBgpPeerExportPolicyName("1", peerAddress);

    assertThat(c.getRoutingPolicies(), hasKey(rpName));

    RoutingPolicy rp = c.getRoutingPolicies().get(rpName);

    assertRejectsKernelRoute(rp, peerAddress);
  }

  @Test
  public void testBgpNextHopSelfConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_bgp_next_hop_self");

    Ip localIp = Ip.parse("192.0.2.254");
    String peer1 = "192.0.2.1";
    String peer2 = "192.0.2.2";
    String peer3 = "192.0.2.3";
    Ip peer1Ip = Ip.parse(peer1);
    Ip peer2Ip = Ip.parse(peer2);
    Ip peer3Ip = Ip.parse(peer3);
    Prefix peer1Prefix = Prefix.create(peer1Ip, MAX_PREFIX_LENGTH);
    Prefix peer2Prefix = Prefix.create(peer2Ip, MAX_PREFIX_LENGTH);
    Prefix peer3Prefix = Prefix.create(peer3Ip, MAX_PREFIX_LENGTH);

    assertThat(
        c,
        hasDefaultVrf(hasBgpProcess(hasNeighbors(hasKeys(peer1Prefix, peer2Prefix, peer3Prefix)))));
    RoutingPolicy rp1 =
        c.getRoutingPolicies().get(computeBgpPeerExportPolicyName("65501", peer1Ip));
    RoutingPolicy rp2 =
        c.getRoutingPolicies().get(computeBgpPeerExportPolicyName("65501", peer2Ip));
    RoutingPolicy rp3 =
        c.getRoutingPolicies().get(computeBgpPeerExportPolicyName("65501", peer3Ip));

    // 192.0.2.1 with next-hop-self should use next-hop-ip of interface
    assertThat(processBgpRoute(rp1, peer1Ip), hasNextHopIp(equalTo(localIp)));

    // 192.0.2.2 with next-hop-self inherited from pg1 should use next-hop-ip of interface
    assertThat(processBgpRoute(rp2, peer2Ip), hasNextHopIp(equalTo(localIp)));

    // 192.0.2.3 without next-hop-self should leave next-hop-ip unset for dp engine to handle
    assertThat(processBgpRoute(rp3, peer3Ip), hasNextHopIp(equalTo(UNSET_ROUTE_NEXT_HOP_IP)));
  }

  @Test
  public void testBgpNextHopSelfExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_imish_bgp_next_hop_self");

    assertThat(
        vc.getBgpProcesses().get("65501").getNeighbors(),
        hasKeys("192.0.2.1", "192.0.2.2", "192.0.2.3"));
    assertTrue(
        "Ensure next-hop-self is extracted for ip neighbor",
        vc.getBgpProcesses().get("65501").getNeighbors().get("192.0.2.1").getNextHopSelf());
    assertTrue(
        "Ensure next-hop-self is extracted for peer-group",
        vc.getBgpProcesses().get("65501").getPeerGroups().get("pg1").getNextHopSelf());
    assertThat(
        "Ensure next-hop-self is non-inherited in VS for ip neighbor",
        vc.getBgpProcesses().get("65501").getNeighbors().get("192.0.2.2").getNextHopSelf(),
        nullValue());
    assertThat(
        "Ensure next-hop-self is unset",
        vc.getBgpProcesses().get("65501").getNeighbors().get("192.0.2.3").getNextHopSelf(),
        nullValue());
  }

  @Test
  public void testBgpProcessConversion() throws IOException {
    String hostname = "f5_bigip_imish_bgp";
    Configuration c = parseConfig(hostname);

    // process config
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasMultipathEquivalentAsPathMatchMode(EXACT_PATH))));

    // peer config
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.strict("192.0.2.1/32"),
                    hasDescription("Cool IPv4 BGP neighbor description")))));
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(hasActiveNeighbor(Prefix.strict("192.0.2.1/32"), hasLocalAs(123L)))));
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.strict("192.0.2.1/32"), hasLocalIp(Ip.parse("192.0.2.2"))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(hasActiveNeighbor(Prefix.strict("192.0.2.1/32"), hasRemoteAs(456L)))));

    //// generated routing policies
    String bgpProcessName = "123";
    String commonExportPolicyName =
        F5BigipConfiguration.computeBgpCommonExportPolicyName(bgpProcessName);
    String peerExportPolicyName =
        F5BigipConfiguration.computeBgpPeerExportPolicyName(bgpProcessName, Ip.parse("192.0.2.1"));

    BgpRoute.Builder bgpRouteBuilder =
        BgpRoute.builder()
            .setAdmin(10)
            .setMetric(10)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(Ip.parse("1.2.3.4"));
    BgpRoute bgpRouteAllowedByPeerPolicy =
        bgpRouteBuilder.setNetwork(Prefix.strict("10.0.0.0/24")).build();
    BgpRoute bgpRouteAllowedOnlyByCommonPolicy =
        bgpRouteBuilder.setNetwork(Prefix.strict("10.0.1.0/24")).build();
    ConnectedRoute connectedRoute = new ConnectedRoute(Prefix.strict("10.0.0.0/24"), "blah");
    KernelRoute kernelRoute = new KernelRoute(Prefix.strict("10.0.0.0/24"));

    // common export policy
    assertThat(c.getRoutingPolicies(), hasKey(commonExportPolicyName));
    RoutingPolicy commonExportPolicy = c.getRoutingPolicies().get(commonExportPolicyName);

    // peer export policy
    assertThat(c.getRoutingPolicies(), hasKey(peerExportPolicyName));
    RoutingPolicy peerExportPolicy = c.getRoutingPolicies().get(peerExportPolicyName);

    {
      // BGP input route acceptable to common export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          commonExportPolicy
              .call(
                  Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                      .setOriginalRoute(bgpRouteAllowedByPeerPolicy)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
      BgpRoute outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities(empty()));
    }

    {
      // BGP input route acceptable to peer export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          peerExportPolicy
              .call(
                  Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                      .setOriginalRoute(bgpRouteAllowedByPeerPolicy)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
      BgpRoute outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities(contains(StandardCommunity.parse("2:2"))));
    }

    {
      // With below test, BGP input route acceptable ONLY to common export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          commonExportPolicy
              .call(
                  Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                      .setOriginalRoute(bgpRouteAllowedOnlyByCommonPolicy)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
    }

    {
      // BGP input route unacceptable to peer export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertFalse(
          peerExportPolicy
              .call(
                  Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                      .setOriginalRoute(bgpRouteAllowedOnlyByCommonPolicy)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
    }

    {
      // Connected input route unacceptable to common export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertFalse(
          commonExportPolicy
              .call(
                  Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                      .setOriginalRoute(connectedRoute)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
    }

    {
      // Connected input route unacceptable to peer export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertFalse(
          peerExportPolicy
              .call(
                  Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                      .setOriginalRoute(connectedRoute)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
    }

    {
      // Kernel input route acceptable to common export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          commonExportPolicy
              .call(
                  Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                      .setOriginalRoute(kernelRoute)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
      BgpRoute outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities(empty()));
    }

    {
      // Kernel input route acceptable to peer export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          peerExportPolicy
              .call(
                  Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                      .setOriginalRoute(kernelRoute)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
      BgpRoute outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities(contains(StandardCommunity.parse("2:2"))));
    }
  }

  @Test
  public void testBgpProcessReferences() throws IOException {
    String hostname = "f5_bigip_imish_bgp_process_references";
    String file = "configs/" + hostname;
    String used = "123";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, BGP_PROCESS, used, 1));
    assertThat(ans, hasNumReferrers(file, BGP_NEIGHBOR, "192.0.2.1", 1));
  }

  @Test
  public void testBgpRouterIdAuto() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_bgp_router_id_auto");

    // BGP Router-ID automatically chosen from highest IP address
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasRouterId(Ip.parse("192.0.2.1")))));
  }

  @Test
  public void testBgpRouterIdManual() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_bgp_router_id_manual");

    // BGP Router-ID manually set
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasRouterId(Ip.parse("192.0.2.1")))));
  }

  @Test
  public void testPrefixListExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_imish_prefix_list");

    // check all lists are extracted
    assertThat(vc.getPrefixLists(), hasKeys("pl_simple", "pl_le", "pl_ge", "pl_ge_le", "pl_deny"));
    // check lists know their own names
    vc.getPrefixLists()
        .forEach((name, prefixList) -> assertThat(prefixList.getName(), equalTo(name)));

    PrefixList plSimple = vc.getPrefixLists().get("pl_simple");
    PrefixList plLe = vc.getPrefixLists().get("pl_le");
    PrefixList plGe = vc.getPrefixLists().get("pl_ge");
    PrefixList plGeLe = vc.getPrefixLists().get("pl_ge_le");
    PrefixList plDeny = vc.getPrefixLists().get("pl_deny");

    // check presence of entries
    assertThat(plSimple.getEntries(), hasKeys(10L));
    assertThat(plLe.getEntries(), hasKeys(10L));
    assertThat(plGe.getEntries(), hasKeys(10L));
    assertThat(plGeLe.getEntries(), hasKeys(10L));
    assertThat(plDeny.getEntries(), hasKeys(10L, 20L));

    PrefixListEntry plSimple10 = plSimple.getEntries().get(10L);
    PrefixListEntry plLe10 = plLe.getEntries().get(10L);
    PrefixListEntry plGe10 = plGe.getEntries().get(10L);
    PrefixListEntry plGeLe10 = plGeLe.getEntries().get(10L);
    PrefixListEntry plDeny10 = plDeny.getEntries().get(10L);
    PrefixListEntry plDeny20 = plDeny.getEntries().get(20L);

    // check entry actions
    assertThat(plSimple10.getAction(), equalTo(LineAction.PERMIT));
    assertThat(plLe10.getAction(), equalTo(LineAction.PERMIT));
    assertThat(plGe10.getAction(), equalTo(LineAction.PERMIT));
    assertThat(plGeLe10.getAction(), equalTo(LineAction.PERMIT));
    assertThat(plDeny10.getAction(), equalTo(LineAction.DENY));
    assertThat(plDeny20.getAction(), equalTo(LineAction.PERMIT));

    // check entry prefixes
    assertThat(plSimple10.getPrefix(), equalTo(Prefix.parse("10.0.0.0/24")));
    assertThat(plLe10.getPrefix(), equalTo(Prefix.parse("10.0.0.0/16")));
    assertThat(plGe10.getPrefix(), equalTo(Prefix.parse("10.0.0.0/16")));
    assertThat(plGeLe10.getPrefix(), equalTo(Prefix.parse("10.0.0.0/16")));
    assertThat(plDeny10.getPrefix(), equalTo(Prefix.parse("10.0.0.0/32")));
    assertThat(plDeny20.getPrefix(), equalTo(Prefix.parse("10.0.0.0/16")));

    // check entry length-ranges
    assertThat(plSimple10.getLengthRange(), equalTo(new SubRange(24, 32)));
    assertThat(plLe10.getLengthRange(), equalTo(new SubRange(16, 24)));
    assertThat(plGe10.getLengthRange(), equalTo(new SubRange(24, 32)));
    assertThat(plGeLe10.getLengthRange(), equalTo(new SubRange(24, 28)));
    assertThat(plDeny10.getLengthRange(), equalTo(new SubRange(32, 32)));
    assertThat(plDeny20.getLengthRange(), equalTo(new SubRange(16, 32)));
  }

  @Test
  public void testRecovery() throws IOException {
    String hostname = "f5_bigip_imish_recovery";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.getSettings().setDisableUnrecognized(false);
    batfish.getSettings().setThrowOnLexerError(false);
    batfish.getSettings().setThrowOnParserError(false);
    Configuration c = batfish.loadConfigurations().get(hostname);
    assertThat(c, hasIpAccessLists(hasKey("acl2")));
    InitInfoAnswerElement initAns = batfish.initInfo(false, true);
    assertThat(initAns.getParseStatus().get(filename), equalTo(ParseStatus.PARTIALLY_UNRECOGNIZED));
  }

  @Test
  public void testRouteMap() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_route_map");
    String acceptAllName = "ACCEPT_ALL";
    String rm1Name = "rm1";

    // ACCEPT_ALL
    assertThat(c.getRoutingPolicies(), hasKey(acceptAllName));
    assertTrue(
        "ACCEPT_ALL accepts arbitrary prefix 10.0.0.0/24",
        c.getRoutingPolicies()
            .get(acceptAllName)
            .call(
                Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                    .setDirection(Direction.OUT)
                    .setOriginalRoute(
                        new ConnectedRoute(Prefix.strict("10.0.0.0/24"), "/Common/outint"))
                    .build())
            .getBooleanValue());

    // rm1
    assertThat(c.getRoutingPolicies(), hasKey(rm1Name));

    RoutingPolicy rm1 = c.getRoutingPolicies().get(rm1Name);

    assertTrue(
        "rm1 denies prefix 10.0.0.0/24 (via 10)",
        !rm1.call(
                Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                    .setDirection(Direction.OUT)
                    .setOriginalRoute(
                        new ConnectedRoute(Prefix.strict("10.0.0.0/24"), "/Common/outint"))
                    .build())
            .getBooleanValue());

    ConnectedRoute acceptedRoute =
        new ConnectedRoute(Prefix.strict("10.0.1.0/24"), "/Common/outint");
    BgpRoute.Builder outputRoute =
        BgpRoute.builder()
            .setNetwork(acceptedRoute.getNetwork())
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    Environment acceptedPrefixEnvironment =
        Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
            .setDirection(Direction.OUT)
            .setOutputRoute(outputRoute)
            .setOriginalRoute(acceptedRoute)
            .build();
    Result acceptedBy20 = rm1.call(acceptedPrefixEnvironment);

    assertTrue("rm1 accepts prefix 10.0.1.0/24 (via 20)", acceptedBy20.getBooleanValue());
    assertThat(
        "rm1 sets communities 1:2 and 33:44 on the output route",
        outputRoute.build().getCommunities(),
        equalTo(ImmutableSet.of(StandardCommunity.of(1, 2), StandardCommunity.of(33, 44))));

    assertTrue(
        "rm1 rejects prefix 10.0.2.0/24 (no matching entry)",
        !rm1.call(
                Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
                    .setDirection(Direction.OUT)
                    .setOriginalRoute(
                        new ConnectedRoute(Prefix.strict("10.0.2.0/24"), "/Common/outint"))
                    .build())
            .getBooleanValue());
  }

  @Test
  public void testRouteMapMatchIpAddressPrefixListConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_route_map_match_ip_address_prefix_list");
    String rp1Name = "rm1";
    String rp2Name = "rm2";

    assertThat(c.getRoutingPolicies(), hasKeys(rp1Name, rp2Name));

    // (default route) accepted
    assertAcceptsKernelRoute(c.getRoutingPolicies().get(rp1Name), Ip.ZERO);
    // (default route) rejected
    assertRejectsKernelRoute(c.getRoutingPolicies().get(rp2Name), Ip.ZERO);
  }

  @Test
  public void testRouteMapMatchIpAddressPrefixListExtraction() {
    F5BigipConfiguration vc =
        parseVendorConfig("f5_bigip_imish_route_map_match_ip_address_prefix_list");
    String rm1Name = "rm1";
    String rm2Name = "rm2";

    assertThat(vc.getRouteMaps(), hasKeys(rm1Name, rm2Name));

    RouteMap rm1 = vc.getRouteMaps().get(rm1Name);
    RouteMap rm2 = vc.getRouteMaps().get(rm2Name);

    assertThat(rm1.getEntries(), hasKeys(10L));
    assertThat(rm2.getEntries(), hasKeys(10L));

    RouteMapEntry entry1 = rm1.getEntries().get(10L);
    RouteMapEntry entry2 = rm2.getEntries().get(10L);

    RouteMapMatchPrefixList match1 = entry1.getMatchPrefixList();
    RouteMapMatchPrefixList match2 = entry2.getMatchPrefixList();

    assertThat(match1, notNullValue());
    assertThat(match1.getPrefixList(), equalTo("pl1"));
    assertThat(entry1.getMatches().collect(ImmutableList.toImmutableList()), contains(match1));
    assertThat(match2, notNullValue());
    assertThat(match2.getPrefixList(), equalTo("pl2"));
    assertThat(entry2.getMatches().collect(ImmutableList.toImmutableList()), contains(match2));
  }

  @Test
  public void testRouteMapReferences() throws IOException {
    String hostname = "f5_bigip_imish_route_map_references";
    String file = "configs/" + hostname;
    String undefined = "route-map-undefined";
    String unused = "route-map-unused";
    String used = "route-map-used";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, ROUTE_MAP, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, ROUTE_MAP, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, ROUTE_MAP, used, 2));
  }

  @Test
  public void testRouteMapSetMetricConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_route_map_set_metric");
    String rpName = "rm1";

    assertThat(c.getRoutingPolicies(), hasKeys(rpName));

    assertThat(processBgpRoute(c.getRoutingPolicies().get(rpName), Ip.ZERO), hasMetric(50L));
  }

  @Test
  public void testRouteMapSetMetricExtraction() throws IOException {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_imish_route_map_set_metric");
    String rmName = "rm1";

    assertThat(vc.getRouteMaps(), hasKeys(rmName));

    RouteMap rm = vc.getRouteMaps().get(rmName);

    assertThat(rm.getEntries(), hasKeys(10L));

    RouteMapEntry entry = rm.getEntries().get(10L);

    RouteMapSetMetric set = entry.getSetMetric();

    assertThat(set, notNullValue());
    assertThat(set.getMetric(), equalTo(50L));
    assertThat(entry.getSets().collect(ImmutableList.toImmutableList()), contains(set));
  }

  @Test
  public void testRouteMapSetOriginConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_imish_route_map_set_origin");
    String rpName = "rm1";

    assertThat(c.getRoutingPolicies(), hasKeys(rpName));

    assertThat(
        processBgpRoute(c.getRoutingPolicies().get(rpName), Ip.ZERO),
        hasOriginType(OriginType.IGP));
  }

  @Test
  public void testRouteMapSetOriginExtraction() throws IOException {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_imish_route_map_set_origin");
    String rmName = "rm1";

    assertThat(vc.getRouteMaps(), hasKeys(rmName));

    RouteMap rm = vc.getRouteMaps().get(rmName);

    assertThat(rm.getEntries(), hasKeys(10L));

    RouteMapEntry entry = rm.getEntries().get(10L);

    RouteMapSetOrigin set = entry.getSetOrigin();

    assertThat(set, notNullValue());
    assertThat(set.getOrigin(), equalTo(OriginType.IGP));
    assertThat(entry.getSets().collect(ImmutableList.toImmutableList()), contains(set));
  }

  private @Nonnull IpAccessListToBdd toBDD() {
    BDDPacket pkt = new BDDPacket();
    BDDSourceManager mgr = BDDSourceManager.forInterfaces(pkt, ImmutableSet.of("dummy"));
    return new IpAccessListToBddImpl(pkt, mgr, ImmutableMap.of(), ImmutableMap.of());
  }
}

package org.batfish.grammar.f5_bigip_structured;

import static org.batfish.common.util.CommonUtil.communityStringToLong;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasDescription;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalIp;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEquivalentAsPathMatchMode;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasRouterId;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasCommunities;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.isBgpRouteThat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRoute6FilterLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAddress;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllowedVlans;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasNativeVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSpeed;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isSwitchport;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.KernelRouteMatchers.isKernelRouteThat;
import static org.batfish.datamodel.matchers.RouteFilterListMatchers.permits;
import static org.batfish.datamodel.matchers.RouteFilterListMatchers.rejects;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasKernelRoutes;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.BGP_NEIGHBOR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.BGP_PROCESS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.INTERFACE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_HTTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_HTTPS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.NODE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE_SOURCE_ADDR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE_SSL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.POOL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PREFIX_LIST;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_CLIENT_SSL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_HTTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_OCSP_STAPLING_PARAMS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_ONE_CONNECT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SERVER_SSL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_TCP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.ROUTE_MAP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.RULE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SELF;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SNAT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SNATPOOL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SNAT_TRANSLATION;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VIRTUAL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VIRTUAL_ADDRESS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VLAN;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDiff;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.KernelRoute;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.Step;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.flow.TransformationStep.TransformationStepDetail;
import org.batfish.datamodel.flow.TransformationStep.TransformationType;
import org.batfish.datamodel.matchers.Route6FilterListMatchers;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.f5_bigip.Builtin;
import org.batfish.representation.f5_bigip.BuiltinMonitor;
import org.batfish.representation.f5_bigip.BuiltinPersistence;
import org.batfish.representation.f5_bigip.BuiltinProfile;
import org.batfish.representation.f5_bigip.F5BigipConfiguration;
import org.batfish.representation.f5_bigip.F5BigipStructureType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public final class F5BigipStructuredGrammarTest {
  private static final String SNAPSHOTS_PREFIX =
      "org/batfish/grammar/f5_bigip_structured/snapshots/";
  private static final String TESTCONFIGS_PREFIX =
      "org/batfish/grammar/f5_bigip_structured/testconfigs/";

  /**
   * Assert that {@code ans} does not contain undefined references to builtins. This happens when
   * builtins are referenced but not correctly identified.
   *
   * @param ans The answer element containing the map of undefined references
   * @param types The types using the namespace searched by {@code nameToBuiltIn}
   * @param nameToBuiltin A function that returns a {@link Builtin} for a given name, or {@code
   *     null} if the name does not correspond to a builtin.
   */
  private static void assertNoUndefinedReferencesToBuiltins(
      ConvertConfigurationAnswerElement ans,
      Stream<F5BigipStructureType> types,
      Function<String, ? extends Builtin> nameToBuiltin) {
    types.forEach(
        type ->
            ans.getUndefinedReferences()
                .values()
                .forEach(
                    undefinedReferencesForFile ->
                        undefinedReferencesForFile
                            .get(type.getDescription())
                            .keySet()
                            .forEach(
                                structureName -> {
                                  String msg =
                                      String.format(
                                          "Reference to '%s' of type '%s' should not be undefined because '%s' is a builtin.",
                                          structureName, type.getDescription(), structureName);
                                  assertThat(msg, nameToBuiltin.apply(structureName), nullValue());
                                  assertThat(
                                      msg,
                                      nameToBuiltin.apply(Builtin.COMMON_PREFIX + structureName),
                                      nullValue());
                                })));
  }

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private BgpRoute.Builder makeBgpOutputRouteBuilder() {
    return BgpRoute.builder()
        .setNetwork(Prefix.ZERO)
        .setOriginType(OriginType.INCOMPLETE)
        .setOriginatorIp(Ip.ZERO)
        .setProtocol(RoutingProtocol.BGP);
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
  public void testBgpProcessConversion() throws IOException {
    String hostname = "f5_bigip_structured_net_routing_bgp";
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
    String bgpProcessName = "/Common/my_bgp";
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
                  Environment.builder(c)
                      .setOriginalRoute(bgpRouteAllowedByPeerPolicy)
                      .setOutputRoute(outputBuilder)
                      .setVrf(Configuration.DEFAULT_VRF_NAME)
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
                  Environment.builder(c)
                      .setOriginalRoute(bgpRouteAllowedByPeerPolicy)
                      .setOutputRoute(outputBuilder)
                      .setVrf(Configuration.DEFAULT_VRF_NAME)
                      .build())
              .getBooleanValue());
      BgpRoute outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities(contains(communityStringToLong("2:2"))));
    }

    {
      // BGP input route unacceptable to peer export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertFalse(
          peerExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(bgpRouteAllowedOnlyByCommonPolicy)
                      .setOutputRoute(outputBuilder)
                      .setVrf(Configuration.DEFAULT_VRF_NAME)
                      .build())
              .getBooleanValue());
    }

    {
      // Connected input route unacceptable to common export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertFalse(
          commonExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(connectedRoute)
                      .setOutputRoute(outputBuilder)
                      .setVrf(Configuration.DEFAULT_VRF_NAME)
                      .build())
              .getBooleanValue());
    }

    {
      // Connected input route unacceptable to peer export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertFalse(
          peerExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(connectedRoute)
                      .setOutputRoute(outputBuilder)
                      .setVrf(Configuration.DEFAULT_VRF_NAME)
                      .build())
              .getBooleanValue());
    }

    {
      // Kernel input route acceptable to common export policy
      BgpRoute.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          commonExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(kernelRoute)
                      .setOutputRoute(outputBuilder)
                      .setVrf(Configuration.DEFAULT_VRF_NAME)
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
                  Environment.builder(c)
                      .setOriginalRoute(kernelRoute)
                      .setOutputRoute(outputBuilder)
                      .setVrf(Configuration.DEFAULT_VRF_NAME)
                      .build())
              .getBooleanValue());
      BgpRoute outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities(contains(communityStringToLong("2:2"))));
    }
  }

  @Test
  public void testBgpProcessReferences() throws IOException {
    String hostname = "f5_bigip_structured_bgp_process_references";
    String file = "configs/" + hostname;
    String used = "/Common/my_bgp_process";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, BGP_PROCESS, used, 1));
    assertThat(ans, hasNumReferrers(file, BGP_NEIGHBOR, "192.0.2.1", 1));

    // bgp neighbor update-source
    assertThat(ans, hasNumReferrers(file, VLAN, "/Common/vlan_used", 1));
  }

  @Test
  public void testBgpRouteIdAuto() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_bgp_router_id_auto");

    // BGP Router-ID automatically chosen from highest IP address
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasRouterId(Ip.parse("192.0.2.1")))));
  }

  @Test
  public void testBgpRouterIdManual() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_bgp_router_id_manual");

    // BGP Router-ID manually set
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasRouterId(Ip.parse("192.0.2.1")))));
  }

  @Test
  public void testDnat() throws IOException {
    String snapshotName = "dnat";
    String natHostname = "f5_bigip_structured_dnat";
    String hostname = "host1";
    String hostFilename = hostname + ".json";
    String tag = "tag";

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(SNAPSHOTS_PREFIX + snapshotName, natHostname)
                .setHostsText(SNAPSHOTS_PREFIX + snapshotName, hostFilename)
                .build(),
            _folder);
    batfish.computeDataPlane();

    {
      // DNAT modulo ARP
      Flow flow =
          Flow.builder()
              .setTag(tag)
              .setDstIp(Ip.parse("192.0.2.1"))
              .setDstPort(80)
              .setIngressInterface("/Common/SOME_VLAN")
              .setIngressNode(natHostname)
              .setIpProtocol(IpProtocol.TCP)
              .setSrcIp(Ip.parse("8.8.8.8"))
              .setSrcPort(50000)
              .build();
      SortedMap<Flow, List<Trace>> flowTraces =
          batfish.getTracerouteEngine().computeTraces(ImmutableSet.of(flow), false);
      List<Trace> traces = flowTraces.get(flow);
      Optional<TransformationStepDetail> stepDetailOptional =
          traces.stream()
              .map(Trace::getHops)
              .flatMap(Collection::stream)
              .map(Hop::getSteps)
              .flatMap(Collection::stream)
              .map(Step::getDetail)
              .filter(Predicates.instanceOf(TransformationStepDetail.class))
              .map(TransformationStepDetail.class::cast)
              .filter(d -> d.getTransformationType() == TransformationType.DEST_NAT)
              .findFirst();

      assertTrue("There is a DNAT transformation step.", stepDetailOptional.isPresent());

      TransformationStepDetail detail = stepDetailOptional.get();

      assertThat(
          detail.getFlowDiffs(),
          hasItem(
              equalTo(
                  FlowDiff.flowDiff(
                      IpField.DESTINATION, Ip.parse("192.0.2.1"), Ip.parse("192.0.2.10")))));
    }

    {
      // DNAT with ARP
      Flow flow =
          Flow.builder()
              .setTag(tag)
              .setDstIp(Ip.parse("192.0.2.1"))
              .setDstPort(80)
              .setIngressNode(hostname)
              .setIpProtocol(IpProtocol.TCP)
              .setSrcIp(Ip.parse("192.0.2.2"))
              .setSrcPort(50000)
              .build();
      SortedMap<Flow, List<Trace>> flowTraces =
          batfish.getTracerouteEngine().computeTraces(ImmutableSet.of(flow), false);
      List<Trace> traces = flowTraces.get(flow);
      Optional<TransformationStepDetail> stepDetailOptional =
          traces.stream()
              .map(Trace::getHops)
              .flatMap(Collection::stream)
              .map(Hop::getSteps)
              .flatMap(Collection::stream)
              .map(Step::getDetail)
              .filter(Predicates.instanceOf(TransformationStepDetail.class))
              .map(TransformationStepDetail.class::cast)
              .filter(d -> d.getTransformationType() == TransformationType.DEST_NAT)
              .findFirst();

      assertTrue("There is a DNAT transformation step.", stepDetailOptional.isPresent());

      TransformationStepDetail detail = stepDetailOptional.get();

      assertThat(
          detail.getFlowDiffs(),
          hasItem(
              equalTo(
                  FlowDiff.flowDiff(
                      IpField.DESTINATION, Ip.parse("192.0.2.1"), Ip.parse("192.0.2.10")))));
    }
  }

  @Test
  public void testHostname() throws IOException {
    String filename = "f5_bigip_structured_hostname";
    String hostname = "myhostname";
    Map<String, Configuration> configurations = parseTextConfigs(filename);

    assertThat(configurations, hasKey(hostname));
  }

  @Test
  public void testInterfaceReferences() throws IOException {
    String hostname = "f5_bigip_structured_interface_references";
    String file = "configs/" + hostname;
    String undefined = "3.0";
    String unused = "2.0";
    String used = "1.0";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, INTERFACE, undefined));

    // detected unused structure (except self-reference)
    assertThat(ans, hasNumReferrers(file, INTERFACE, unused, 1));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, INTERFACE, used, 2));
  }

  @Test
  public void testInterfaceSpeed() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_interface");

    assertThat(c.getAllInterfaces().keySet(), containsInAnyOrder("1.0", "2.0"));
    assertThat(c, hasInterface("1.0", hasSpeed(40E9D)));
    assertThat(c, hasInterface("2.0", hasSpeed(100E9D)));
  }

  @Test
  public void testKernelRoutes() throws IOException {
    String hostname = "f5_bigip_structured_ltm";
    Configuration c = parseConfig(hostname);

    assertThat(
        c,
        hasDefaultVrf(hasKernelRoutes(contains(new KernelRoute(Prefix.strict("192.0.2.8/32"))))));
  }

  @Test
  public void testMonitorReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // monitor http
    {
      String undefined = "/Common/monitor_http_undefined";
      String unused = "/Common/monitor_http_unused";
      String used = "/Common/monitor_http_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, MONITOR, undefined));
      assertThat(ans, hasUndefinedReference(file, MONITOR_HTTP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, MONITOR_HTTP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, MONITOR_HTTP, used, 2));
    }

    // monitor https
    {
      String undefined = "/Common/monitor_https_undefined";
      String unused = "/Common/monitor_https_unused";
      String used = "/Common/monitor_https_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, MONITOR, undefined));
      assertThat(ans, hasUndefinedReference(file, MONITOR_HTTPS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, MONITOR_HTTPS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, MONITOR_HTTPS, used, 2));
    }

    assertNoUndefinedReferencesToBuiltins(
        ans, Stream.of(MONITOR, MONITOR_HTTP, MONITOR_HTTPS), BuiltinMonitor::getBuiltinMonitor);
  }

  @Test
  public void testNodeReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    String undefined = "/Common/node_undefined";
    String unused = "/Common/node_unused";
    String used = "/Common/node_used";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, NODE, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, NODE, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, NODE, used, 1));
  }

  @Test
  public void testPersistenceReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // persistence source-addr
    {
      String undefined = "/Common/persistence_source_addr_undefined";
      String unused = "/Common/persistence_source_addr_unused";
      String used = "/Common/persistence_source_addr_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PERSISTENCE, undefined));
      assertThat(ans, hasUndefinedReference(file, PERSISTENCE_SOURCE_ADDR, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PERSISTENCE_SOURCE_ADDR, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PERSISTENCE_SOURCE_ADDR, used, 2));
    }

    // persistence ssl
    {
      String undefined = "/Common/persistence_ssl_undefined";
      String unused = "/Common/persistence_ssl_unused";
      String used = "/Common/persistence_ssl_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PERSISTENCE, undefined));
      assertThat(ans, hasUndefinedReference(file, PERSISTENCE_SSL, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PERSISTENCE_SSL, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PERSISTENCE_SSL, used, 2));
    }

    assertNoUndefinedReferencesToBuiltins(
        ans,
        Stream.of(PERSISTENCE, PERSISTENCE_SOURCE_ADDR, PERSISTENCE_SSL),
        BuiltinPersistence::getBuiltinPersistence);
  }

  @Test
  public void testPoolReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    String undefined = "/Common/pool_undefined";
    String unused = "/Common/pool_unused";
    String used = "/Common/pool_used";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, POOL, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, POOL, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, POOL, used, 1));
  }

  @Test
  public void testPrefixList() throws IOException {
    String hostname = "f5_bigip_structured_net_routing_prefix_list";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    String v4Name = "/Common/MY_IPV4_PREFIX_LIST";
    String v6Name = "/Common/MY_IPV6_PREFIX_LIST";
    String invalidName = "/Common/INVALID_MIXED_PREFIX_LIST";

    // Check the presence and behavior of the IPv4 prefix-list
    assertThat(c, hasRouteFilterLists(hasKey(v4Name)));

    RouteFilterList v4 = c.getRouteFilterLists().get(v4Name);

    assertThat(v4, rejects(Prefix.parse("192.0.2.0/31")));
    assertThat(v4, permits(Prefix.parse("192.0.2.4/30")));
    assertThat(v4, permits(Prefix.parse("192.0.2.4/31")));
    assertThat(v4, rejects(Prefix.parse("192.0.2.4/32")));

    // Check the presence and behavior of the IPv6 prefix-list
    assertThat(c, hasRoute6FilterLists(hasKey(v6Name)));

    Route6FilterList v6 = c.getRoute6FilterLists().get(v6Name);

    assertThat(v6, Route6FilterListMatchers.permits(new Prefix6("dead:beef:1::/64")));
    assertThat(v6, Route6FilterListMatchers.rejects(new Prefix6("dead:beef:1::/128")));

    // The invalid list should not make it into the data model
    assertThat(c, hasRouteFilterLists(not(hasKey(invalidName))));
    assertThat(c, hasRoute6FilterLists(not(hasKey(invalidName))));

    // Check errors
    Warnings warnings = batfish.initInfo(false, true).getWarnings().get(hostname);

    assertTrue(
        "Missing IPv4 prefix reported",
        warnings.getRedFlagWarnings().stream()
            .map(Warning::getText)
            .anyMatch(Predicates.containsPattern("Missing IPv4 prefix.*PL4_WITH_MISSING_PREFIX")));
    assertTrue(
        "Missing IPv6 prefix reported",
        warnings.getRedFlagWarnings().stream()
            .map(Warning::getText)
            .anyMatch(Predicates.containsPattern("Missing IPv6 prefix.*PL6_WITH_MISSING_PREFIX")));
    assertTrue(
        "Invalid IPv4 length-range reported",
        warnings.getRedFlagWarnings().stream()
            .map(Warning::getText)
            .anyMatch(
                Predicates.containsPattern(
                    "Invalid IPv4 prefix-len-range.*PL4_WITH_INVALID_LENGTH")));
    assertTrue(
        "Invalid IPv6 length-range reported",
        warnings.getRedFlagWarnings().stream()
            .map(Warning::getText)
            .anyMatch(
                Predicates.containsPattern(
                    "Invalid IPv6 prefix-len-range.*PL6_WITH_INVALID_LENGTH")));
    assertTrue(
        "Missing action reported for IPv4 prefix-list",
        warnings.getRedFlagWarnings().stream()
            .map(Warning::getText)
            .anyMatch(Predicates.containsPattern("Missing action.*PL4_WITH_MISSING_ACTION")));
    assertTrue(
        "Missing action reported for IPv6 prefix-list",
        warnings.getRedFlagWarnings().stream()
            .map(Warning::getText)
            .anyMatch(Predicates.containsPattern("Missing action.*PL6_WITH_MISSING_ACTION")));
  }

  @Test
  public void testPrefixListReferences() throws IOException {
    String hostname = "f5_bigip_structured_prefix_list_references";
    String file = "configs/" + hostname;
    String undefined = "/Common/prefix-list-undefined";
    String unused = "/Common/prefix-list-unused";
    String used = "/Common/prefix-list-used";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, PREFIX_LIST, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, PREFIX_LIST, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, PREFIX_LIST, used, 1));
  }

  @Test
  public void testProfileReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // profile client-ssl
    {
      String undefined = "/Common/profile_client_ssl_undefined";
      String unused = "/Common/profile_client_ssl_unused";
      String used = "/Common/profile_client_ssl_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_CLIENT_SSL, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_CLIENT_SSL, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_CLIENT_SSL, used, 2));
    }

    // profile http
    {
      String undefined = "/Common/profile_http_undefined";
      String unused = "/Common/profile_http_unused";
      String used = "/Common/profile_http_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_HTTP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_HTTP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_HTTP, used, 2));
    }

    // profile ocsp-stapling-params
    {
      String undefined = "/Common/profile_ocsp_stapling_params_undefined";
      String unused = "/Common/profile_ocsp_stapling_params_unused";
      String used = "/Common/profile_ocsp_stapling_params_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_OCSP_STAPLING_PARAMS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_OCSP_STAPLING_PARAMS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_OCSP_STAPLING_PARAMS, used, 2));
    }

    // profile one-connect
    {
      String undefined = "/Common/profile_one_connect_undefined";
      String unused = "/Common/profile_one_connect_unused";
      String used = "/Common/profile_one_connect_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_ONE_CONNECT, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_ONE_CONNECT, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_ONE_CONNECT, used, 2));
    }

    // profile server-ssl
    {
      String undefined = "/Common/profile_server_ssl_undefined";
      String unused = "/Common/profile_server_ssl_unused";
      String used = "/Common/profile_server_ssl_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_SERVER_SSL, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_SERVER_SSL, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_SERVER_SSL, used, 3));
    }

    // profile tcp
    {
      String undefined = "/Common/profile_tcp_undefined";
      String unused = "/Common/profile_tcp_unused";
      String used = "/Common/profile_tcp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_TCP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_TCP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_TCP, used, 2));
    }

    assertNoUndefinedReferencesToBuiltins(
        ans,
        Stream.of(
            PROFILE,
            PROFILE_CLIENT_SSL,
            PROFILE_HTTP,
            PROFILE_OCSP_STAPLING_PARAMS,
            PROFILE_ONE_CONNECT,
            PROFILE_SERVER_SSL,
            PROFILE_TCP),
        BuiltinProfile::getBuiltinProfile);
  }

  @Test
  public void testRouteMap() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_net_routing_route_map");
    String acceptAllName = "/Common/ACCEPT_ALL";
    String rm1Name = "/Common/rm1";

    // ACCEPT_ALL
    assertThat(c.getRoutingPolicies(), hasKey(acceptAllName));
    assertTrue(
        "ACCEPT_ALL accepts arbitrary prefix 10.0.0.0/24",
        c.getRoutingPolicies()
            .get(acceptAllName)
            .call(
                Environment.builder(c)
                    .setDirection(Direction.OUT)
                    .setVrf(Configuration.DEFAULT_VRF_NAME)
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
                Environment.builder(c)
                    .setDirection(Direction.OUT)
                    .setVrf(Configuration.DEFAULT_VRF_NAME)
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
        Environment.builder(c)
            .setDirection(Direction.OUT)
            .setOutputRoute(outputRoute)
            .setVrf(Configuration.DEFAULT_VRF_NAME)
            .setOriginalRoute(acceptedRoute)
            .build();
    Result acceptedBy20 = rm1.call(acceptedPrefixEnvironment);

    assertTrue("rm1 accepts prefix 10.0.1.0/24 (via 20)", acceptedBy20.getBooleanValue());
    assertThat(
        "rm1 sets communities 1:2 and 33:44 on the output route",
        outputRoute.build().getCommunities(),
        equalTo(
            Stream.of("1:2", "33:44")
                .map(CommonUtil::communityStringToLong)
                .collect(ImmutableSet.toImmutableSet())));

    assertTrue(
        "rm1 rejects prefix 10.0.2.0/24 (no matching entry)",
        !rm1.call(
                Environment.builder(c)
                    .setDirection(Direction.OUT)
                    .setVrf(Configuration.DEFAULT_VRF_NAME)
                    .setOriginalRoute(
                        new ConnectedRoute(Prefix.strict("10.0.2.0/24"), "/Common/outint"))
                    .build())
            .getBooleanValue());
  }

  @Test
  public void testRouteMapReferences() throws IOException {
    String hostname = "f5_bigip_structured_route_map_references";
    String file = "configs/" + hostname;
    String undefined = "/Common/route-map-undefined";
    String unused = "/Common/route-map-unused";
    String used = "/Common/route-map-used";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, ROUTE_MAP, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, ROUTE_MAP, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, ROUTE_MAP, used, 3));
  }

  @Test
  public void testRuleReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    String undefined = "/Common/rule_undefined";
    String unused = "/Common/rule_unused";
    String used = "/Common/rule_used";

    // detect undefined references
    assertThat(ans, hasUndefinedReference(file, RULE, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, RULE, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, RULE, used, 1));
  }

  @Test
  public void testSelfReferences() throws IOException {
    String hostname = "f5_bigip_structured_self_references";
    String file = "configs/" + hostname;
    String used = "/Common/self_used";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, SELF, used, 1));
  }

  @Test
  public void testSnatCases() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_ltm_snat_cases");

    IpSpace vlan1AdditionalArpIps = c.getAllInterfaces().get("/Common/vlan1").getAdditionalArpIps();

    // no_vlans
    assertThat(vlan1AdditionalArpIps, containsIp(Ip.parse("192.0.2.1")));
    // vlans_missing_names
    assertThat(vlan1AdditionalArpIps, containsIp(Ip.parse("192.0.2.2")));
    // vlans
    assertThat(vlan1AdditionalArpIps, containsIp(Ip.parse("192.0.2.3")));

    IpSpace vlan2AdditionalArpIps = c.getAllInterfaces().get("/Common/vlan2").getAdditionalArpIps();

    // no_vlans
    assertThat(vlan2AdditionalArpIps, containsIp(Ip.parse("192.0.2.1")));
    // vlans_missing_names
    assertThat(vlan2AdditionalArpIps, containsIp(Ip.parse("192.0.2.2")));
    // vlans
    assertThat(vlan2AdditionalArpIps, not(containsIp(Ip.parse("192.0.2.3"))));
  }

  @Test
  public void testSnatpoolReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    String undefined = "/Common/snatpool_undefined";
    String unused = "/Common/snatpool_unused";
    String used = "/Common/snatpool_used";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, SNATPOOL, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, SNATPOOL, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, SNATPOOL, used, 2));
  }

  @Test
  public void testSnatReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    String used = "/Common/snat_used";

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, SNAT, used, 1));
  }

  @Test
  public void testSnatTranslationReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    String undefined = "/Common/192.0.2.6";
    String unused = "/Common/192.0.2.5";
    String used = "/Common/192.0.2.4";

    // detect undefined references
    assertThat(ans, hasUndefinedReference(file, SNAT_TRANSLATION, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, SNAT_TRANSLATION, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, SNAT_TRANSLATION, used, 1));
  }

  @Test
  public void testVirtualAddressReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    String undefined = "/Common/192.0.2.9";
    String unused = "/Common/192.0.2.8";
    String used = "/Common/192.0.2.7";

    // detect undefined references
    assertThat(ans, hasUndefinedReference(file, VIRTUAL_ADDRESS, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, VIRTUAL_ADDRESS, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, VIRTUAL_ADDRESS, used, 1));
  }

  @Test
  public void testVirtualCases() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_ltm_virtual_cases");

    IpSpace vlan1AdditionalArpIps = c.getAllInterfaces().get("/Common/vlan1").getAdditionalArpIps();

    // implicit_mask
    assertThat(vlan1AdditionalArpIps, containsIp(Ip.parse("192.0.2.1")));
    // vlans_missing_names
    assertThat(vlan1AdditionalArpIps, containsIp(Ip.parse("192.0.2.3")));
    // vlans
    assertThat(vlan1AdditionalArpIps, containsIp(Ip.parse("192.0.2.4")));

    IpSpace vlan2AdditionalArpIps = c.getAllInterfaces().get("/Common/vlan2").getAdditionalArpIps();

    // implicit_mask
    assertThat(vlan2AdditionalArpIps, containsIp(Ip.parse("192.0.2.1")));
    // vlans_missing_names
    assertThat(vlan2AdditionalArpIps, containsIp(Ip.parse("192.0.2.3")));
    // vlans
    assertThat(vlan2AdditionalArpIps, not(containsIp(Ip.parse("192.0.2.4"))));
  }

  @Test
  public void testVirtualReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    String used = "/Common/virtual_used";

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, VIRTUAL, used, 1));
  }

  @Test
  public void testVlan() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_vlan");
    String portName = "1.0";
    String vlanName = "/Common/MYVLAN";

    assertThat(c.getAllInterfaces().keySet(), containsInAnyOrder(portName, vlanName));

    // port interface
    assertThat(c, hasInterface(portName, isActive()));
    assertThat(c, hasInterface(portName, isSwitchport()));
    assertThat(c, hasInterface(portName, hasSwitchPortMode(SwitchportMode.TRUNK)));
    assertThat(c, hasInterface(portName, hasAllowedVlans(IntegerSpace.of(123))));
    assertThat(c, hasInterface(portName, hasNativeVlan(nullValue())));

    // vlan interface
    assertThat(c, hasInterface(vlanName, isActive()));
    assertThat(c, hasInterface(vlanName, hasVlan(123)));
    assertThat(c, hasInterface(vlanName, hasAddress("10.0.0.1/24")));
  }

  @Test
  public void testVlanReferences() throws IOException {
    String hostname = "f5_bigip_structured_vlan_references";
    String file = "configs/" + hostname;
    String undefined = "/Common/vlan_undefined";
    String unused = "/Common/vlan_unused";
    String used = "/Common/vlan_used";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, VLAN, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, VLAN, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, VLAN, used, 3));
  }
}

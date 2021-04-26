package org.batfish.grammar.f5_bigip_structured;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.datamodel.Interface.DependencyType.AGGREGATE;
import static org.batfish.datamodel.InterfaceType.AGGREGATED;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasAdministrativeCost;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasMetric;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopIp;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasDescription;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalIp;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEquivalentAsPathMatchMode;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasRouterId;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasCommunities;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.isBgpv4RouteThat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRoute6FilterLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.FlowDiffMatchers.isIpRewrite;
import static org.batfish.datamodel.matchers.FlowDiffMatchers.isPortRewrite;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAddress;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllowedVlans;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasBandwidth;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDependencies;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasInterfaceType;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasNativeVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSpeed;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isSwitchport;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.accepts;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.rejects;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.KernelRouteMatchers.isKernelRouteThat;
import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.batfish.datamodel.matchers.RouteFilterListMatchers.permits;
import static org.batfish.datamodel.matchers.RouteFilterListMatchers.rejects;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasKernelRoutes;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.batfish.datamodel.transformation.TransformationEvaluator.eval;
import static org.batfish.main.BatfishTestUtils.TEST_SNAPSHOT;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.BGP_NEIGHBOR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.BGP_PROCESS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.DATA_GROUP_INTERNAL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.INTERFACE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_DNS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_GATEWAY_ICMP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_HTTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_HTTPS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_LDAP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.MONITOR_TCP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.NODE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE_COOKIE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE_SOURCE_ADDR;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PERSISTENCE_SSL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.POOL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PREFIX_LIST;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_ANALYTICS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_CERTIFICATE_AUTHORITY;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_CLASSIFICATION;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_CLIENT_LDAP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_CLIENT_SSL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_DHCPV4;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_DHCPV6;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_DIAMETER;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_DNS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_FASTHTTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_FASTL4;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_FIX;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_FTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_GTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_HTML;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_HTTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_HTTP2;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_HTTP_COMPRESSION;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_HTTP_PROXY_CONNECT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_ICAP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_ILX;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_IPOTHER;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_IPSECALG;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_MAP_T;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_MQTT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_NETFLOW;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_OCSP_STAPLING_PARAMS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_ONE_CONNECT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_PCP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_PPTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_QOE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_RADIUS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_REQUEST_ADAPT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_REQUEST_LOG;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_RESPONSE_ADAPT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_REWRITE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_RTSP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SCTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SERVER_LDAP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SERVER_SSL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SIP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SMTPS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SOCKS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SPLITSESSIONCLIENT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_SPLITSESSIONSERVER;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_STATISTICS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_STREAM;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_TCP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_TCP_ANALYTICS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_TFTP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_TRAFFIC_ACCELERATION;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_UDP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_WEBSOCKET;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_WEB_ACCELERATION;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_WEB_SECURITY;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.PROFILE_XML;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.ROUTE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.ROUTE_MAP;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.RULE;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SELF;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SNAT;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SNATPOOL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.SNAT_TRANSLATION;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.TRUNK;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VIRTUAL;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VIRTUAL_ADDRESS;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VLAN;
import static org.batfish.representation.f5_bigip.F5BigipStructureType.VLAN_MEMBER_INTERFACE;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.common.plugin.IBatfish;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.FilterResult;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDiff;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.KernelRoute;
import org.batfish.datamodel.MacAddress;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.flow.FirewallSessionTraceInfo;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.Step;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.flow.TraceAndReverseFlow;
import org.batfish.datamodel.flow.TransformationStep.TransformationStepDetail;
import org.batfish.datamodel.flow.TransformationStep.TransformationType;
import org.batfish.datamodel.matchers.IpAccessListMatchers;
import org.batfish.datamodel.matchers.Route6FilterListMatchers;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.PortField;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationEvaluator.TransformationResult;
import org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroupType;
import org.batfish.datamodel.vendor_family.f5_bigip.F5BigipFamily;
import org.batfish.grammar.SilentSyntaxCollection;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.f5_bigip.Builtin;
import org.batfish.representation.f5_bigip.BuiltinMonitor;
import org.batfish.representation.f5_bigip.BuiltinPersistence;
import org.batfish.representation.f5_bigip.BuiltinProfile;
import org.batfish.representation.f5_bigip.ConcreteUnicastAddressIp;
import org.batfish.representation.f5_bigip.Device;
import org.batfish.representation.f5_bigip.DeviceGroup;
import org.batfish.representation.f5_bigip.DeviceGroupDevice;
import org.batfish.representation.f5_bigip.F5BigipConfiguration;
import org.batfish.representation.f5_bigip.F5BigipStructureType;
import org.batfish.representation.f5_bigip.HaGroup;
import org.batfish.representation.f5_bigip.HaGroupPool;
import org.batfish.representation.f5_bigip.HaGroupTrunk;
import org.batfish.representation.f5_bigip.ManagementIp;
import org.batfish.representation.f5_bigip.Route;
import org.batfish.representation.f5_bigip.TrafficGroup;
import org.batfish.representation.f5_bigip.UnicastAddress;
import org.batfish.representation.f5_bigip.Virtual;
import org.batfish.representation.f5_bigip.VirtualAddress;
import org.junit.Before;
import org.junit.Ignore;
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
                                          "Reference to '%s' of type '%s' should not be undefined"
                                              + " because '%s' is a builtin.",
                                          structureName, type.getDescription(), structureName);
                                  assertThat(msg, nameToBuiltin.apply(structureName), nullValue());
                                  assertThat(
                                      msg,
                                      nameToBuiltin.apply(Builtin.COMMON_PREFIX + structureName),
                                      nullValue());
                                })));
  }

  private static @Nonnull Flow createHttpFlow(String ingressNode, Ip dstIp) {
    return Flow.builder()
        .setDstIp(dstIp)
        .setDstPort(NamedPort.HTTP.number())
        .setIngressNode(ingressNode)
        .setIpProtocol(IpProtocol.TCP)
        .setSrcIp(Ip.ZERO)
        .setSrcPort(50000)
        .build();
  }

  private static @Nonnull Flow createIcmpFlow(String ingressNode, Ip dstIp) {
    return Flow.builder()
        .setSrcIp(Ip.ZERO)
        .setDstIp(dstIp)
        .setIngressNode(ingressNode)
        .setIpProtocol(IpProtocol.ICMP)
        .setIcmpType(IcmpType.ECHO_REQUEST)
        .setIcmpCode(0)
        .build();
  }

  private static boolean matchesNonTrivially(IpAccessList acl, Flow flow) {
    FilterResult result = acl.filter(flow, null, ImmutableMap.of(), ImmutableMap.of());
    Integer matchLine = result.getMatchLine();
    if (matchLine == null) {
      return false;
    }
    return !toBDD().toPermitAndDenyBdds(acl.getLines().get(matchLine)).getMatchBdd().isOne();
  }

  private @Nonnull F5BigipConfiguration parseVendorConfig(String filename) {
    String src = readResource(TESTCONFIGS_PREFIX + filename, UTF_8);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    settings.setDisableUnrecognized(_disableUnrecognized);
    F5BigipStructuredCombinedParser parser = new F5BigipStructuredCombinedParser(src, settings);
    F5BigipStructuredControlPlaneExtractor extractor =
        new F5BigipStructuredControlPlaneExtractor(
            src, parser, new Warnings(), filename, null, false, new SilentSyntaxCollection());
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(TEST_SNAPSHOT, tree);
    F5BigipConfiguration vendorConfiguration =
        (F5BigipConfiguration) extractor.getVendorConfiguration();
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + filename);
    return SerializationUtils.clone(vendorConfiguration);
  }

  private static @Nonnull IpAccessListToBdd toBDD() {
    BDDPacket pkt = new BDDPacket();
    BDDSourceManager mgr = BDDSourceManager.forInterfaces(pkt, ImmutableSet.of("dummy"));
    return new IpAccessListToBddImpl(pkt, mgr, ImmutableMap.of(), ImmutableMap.of());
  }

  // TODO: switch to true when tests are fixed
  private static boolean DEFAULT_DISABLE_UNRECOGNIZED = true;

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private boolean _disableUnrecognized;

  @Before
  public void setup() {
    _disableUnrecognized = DEFAULT_DISABLE_UNRECOGNIZED;
  }

  private @Nonnull Batfish getBatfishForConfigurationNames(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    Batfish batfish = BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
    batfish.getSettings().setDisableUnrecognized(_disableUnrecognized);
    return batfish;
  }

  private Bgpv4Route.Builder makeBgpOutputRouteBuilder() {
    return Bgpv4Route.testBuilder()
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
    IBatfish iBatfish = getBatfishForConfigurationNames(configurationNames);
    return iBatfish.loadConfigurations(iBatfish.getSnapshot());
  }

  @Test
  public void testBgpKernelRouteRedistribution() throws IOException {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(SNAPSHOTS_PREFIX + "bgp_e2e", "r1", "r2")
                .build(),
            _folder);
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    Set<AbstractRoute> routes1 =
        dp.getRibs().get("r1").get(Configuration.DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> routes2 =
        dp.getRibs().get("r2").get(Configuration.DEFAULT_VRF_NAME).getRoutes();

    // kernel routes should be installed
    assertThat(routes1, hasItem(isKernelRouteThat(hasPrefix(Prefix.strict("10.0.0.1/32")))));
    assertThat(routes2, hasItem(isKernelRouteThat(hasPrefix(Prefix.strict("10.0.0.2/32")))));

    // kernel routes should be redistributed
    assertThat(routes1, hasItem(isBgpv4RouteThat(hasPrefix(Prefix.strict("10.0.0.2/32")))));
    assertThat(routes2, hasItem(isBgpv4RouteThat(hasPrefix(Prefix.strict("10.0.0.1/32")))));
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

    Bgpv4Route.Builder bgpRouteBuilder =
        Bgpv4Route.testBuilder()
            .setAdmin(10)
            .setMetric(10)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(Ip.parse("1.2.3.4"));
    Bgpv4Route bgpv4RouteAllowedByPeerPolicy =
        bgpRouteBuilder.setNetwork(Prefix.strict("10.0.0.0/24")).build();
    Bgpv4Route bgpv4RouteAllowedOnlyByCommonPolicy =
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
      Bgpv4Route.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          commonExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(bgpv4RouteAllowedByPeerPolicy)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
      Bgpv4Route outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities());
    }

    {
      // BGP input route acceptable to peer export policy
      Bgpv4Route.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          peerExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(bgpv4RouteAllowedByPeerPolicy)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
      Bgpv4Route outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities(StandardCommunity.parse("2:2")));
    }

    {
      // With below test, BGP input route acceptable ONLY to common export policy
      Bgpv4Route.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          commonExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(bgpv4RouteAllowedOnlyByCommonPolicy)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
    }

    {
      // BGP input route unacceptable to peer export policy
      Bgpv4Route.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertFalse(
          peerExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(bgpv4RouteAllowedOnlyByCommonPolicy)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
    }

    {
      // Connected input route unacceptable to common export policy
      Bgpv4Route.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertFalse(
          commonExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(connectedRoute)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
    }

    {
      // Connected input route unacceptable to peer export policy
      Bgpv4Route.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertFalse(
          peerExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(connectedRoute)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
    }

    {
      // Kernel input route acceptable to common export policy
      Bgpv4Route.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          commonExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(kernelRoute)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
      Bgpv4Route outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities());
    }

    {
      // Kernel input route acceptable to peer export policy
      Bgpv4Route.Builder outputBuilder = makeBgpOutputRouteBuilder();
      assertTrue(
          peerExportPolicy
              .call(
                  Environment.builder(c)
                      .setOriginalRoute(kernelRoute)
                      .setOutputRoute(outputBuilder)
                      .build())
              .getBooleanValue());
      Bgpv4Route outputRoute = outputBuilder.build();
      assertThat(outputRoute, hasCommunities(StandardCommunity.parse("2:2")));
    }
  }

  @Test
  public void testBgpProcessReferences() throws IOException {
    String hostname = "f5_bigip_structured_bgp_process_references";
    String file = "configs/" + hostname;
    String used = "/Common/my_bgp_process";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, BGP_PROCESS, used, 1));
    assertThat(ans, hasNumReferrers(file, BGP_NEIGHBOR, "192.0.2.1", 1));

    // bgp neighbor update-source
    assertThat(ans, hasNumReferrers(file, VLAN, "/Common/vlan_used", 1));
  }

  @Test
  public void testBgpRouterIdAuto() throws IOException {
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
  public void testBgpUpdateSource() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_bgp_neighbor_update_source");

    // eBGP single-hop should use interface IP and ignore update-source
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.strict("10.0.0.2/32"), hasLocalIp(Ip.parse("10.0.0.1"))))));

    // eBGP multihop should use update-source
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.strict("10.0.1.2/32"), hasLocalIp(Ip.parse("10.0.0.1"))))));

    // iBGP should use update-source
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.strict("10.0.2.2/32"), hasLocalIp(Ip.parse("10.0.0.1"))))));

    // iBGP should use interface IP when update-source is not declared
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.strict("10.0.0.3/32"), hasLocalIp(Ip.parse("10.0.0.1"))))));

    // iBGP should use interface IP when update-source is undefined
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.strict("10.0.0.4/32"), hasLocalIp(Ip.parse("10.0.0.1"))))));

    // iBGP should use interface IP when update-source has no IP
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.strict("10.0.0.5/32"), hasLocalIp(Ip.parse("10.0.0.1"))))));

    // iBGP should use null when no usable IP
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(Prefix.strict("10.0.3.2/32"), hasLocalIp(nullValue())))));
  }

  @Test
  public void testCmConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_cm");

    F5BigipFamily f = c.getVendorFamily().getF5Bigip();

    // cm device
    assertThat(
        f.getDevices(),
        hasKeys("/Common/f5_bigip_structured_cm", "/Common/f5_bigip_structured_cm2"));
    {
      org.batfish.datamodel.vendor_family.f5_bigip.Device d =
          f.getDevices().get("/Common/f5_bigip_structured_cm");
      assertThat(d.getBaseMac(), equalTo(MacAddress.parse("00:00:00:00:00:01")));
      assertThat(d.getConfigSyncIp(), equalTo(Ip.parse("192.0.2.1")));
      assertThat(d.getHostname(), equalTo("f5_bigip_structured_cm"));
      assertThat(d.getManagementIp(), equalTo(Ip.parse("192.0.2.2")));
      assertThat(d.getSelfDevice(), equalTo(Boolean.TRUE));

      // unicast-address
      Iterator<org.batfish.datamodel.vendor_family.f5_bigip.UnicastAddress> unicastAddresses =
          d.getUnicastAddresses().iterator();
      {
        org.batfish.datamodel.vendor_family.f5_bigip.UnicastAddress ua = unicastAddresses.next();
        assertThat(
            ua.getEffectiveIp(),
            instanceOf(
                org.batfish.datamodel.vendor_family.f5_bigip.ConcreteUnicastAddressIp.class));
        assertThat(ua.getEffectivePort(), equalTo(1026));
        assertThat(
            ((org.batfish.datamodel.vendor_family.f5_bigip.ConcreteUnicastAddressIp)
                    ua.getEffectiveIp())
                .getIp(),
            equalTo(Ip.parse("192.0.2.1")));
        assertThat(
            ua.getIp(),
            instanceOf(
                org.batfish.datamodel.vendor_family.f5_bigip.ConcreteUnicastAddressIp.class));
        assertThat(
            ((org.batfish.datamodel.vendor_family.f5_bigip.ConcreteUnicastAddressIp) ua.getIp())
                .getIp(),
            equalTo(Ip.parse("192.0.2.1")));
        assertThat(ua.getPort(), nullValue());
      }
      {
        org.batfish.datamodel.vendor_family.f5_bigip.UnicastAddress ua = unicastAddresses.next();
        assertThat(
            ua.getEffectiveIp(),
            instanceOf(org.batfish.datamodel.vendor_family.f5_bigip.ManagementIp.class));
        assertThat(ua.getEffectivePort(), equalTo(1026));
        assertThat(
            ua.getIp(),
            instanceOf(org.batfish.datamodel.vendor_family.f5_bigip.ManagementIp.class));
        assertThat(ua.getPort(), equalTo(1026));
      }
      assertFalse(unicastAddresses.hasNext());
    }
    {
      org.batfish.datamodel.vendor_family.f5_bigip.Device d =
          f.getDevices().get("/Common/f5_bigip_structured_cm2");
      assertThat(d.getBaseMac(), equalTo(MacAddress.parse("00:00:00:00:00:02")));
      assertThat(d.getConfigSyncIp(), equalTo(Ip.parse("192.0.2.3")));
      assertThat(d.getHostname(), equalTo("f5_bigip_structured_cm2"));
      assertThat(d.getManagementIp(), equalTo(Ip.parse("192.0.2.4")));
      assertThat(d.getSelfDevice(), nullValue());

      // unicast-address
      Iterator<org.batfish.datamodel.vendor_family.f5_bigip.UnicastAddress> unicastAddresses =
          d.getUnicastAddresses().iterator();
      {
        org.batfish.datamodel.vendor_family.f5_bigip.UnicastAddress ua = unicastAddresses.next();
        assertThat(
            ua.getEffectiveIp(),
            instanceOf(
                org.batfish.datamodel.vendor_family.f5_bigip.ConcreteUnicastAddressIp.class));
        assertThat(ua.getEffectivePort(), equalTo(1026));
        assertThat(
            ((org.batfish.datamodel.vendor_family.f5_bigip.ConcreteUnicastAddressIp)
                    ua.getEffectiveIp())
                .getIp(),
            equalTo(Ip.parse("192.0.2.3")));
        assertThat(
            ua.getIp(),
            instanceOf(
                org.batfish.datamodel.vendor_family.f5_bigip.ConcreteUnicastAddressIp.class));
        assertThat(
            ((org.batfish.datamodel.vendor_family.f5_bigip.ConcreteUnicastAddressIp) ua.getIp())
                .getIp(),
            equalTo(Ip.parse("192.0.2.3")));
        assertThat(ua.getPort(), nullValue());
      }
      {
        org.batfish.datamodel.vendor_family.f5_bigip.UnicastAddress ua = unicastAddresses.next();
        assertThat(
            ua.getEffectiveIp(),
            instanceOf(org.batfish.datamodel.vendor_family.f5_bigip.ManagementIp.class));
        assertThat(ua.getEffectivePort(), equalTo(1026));
        assertThat(
            ua.getIp(),
            instanceOf(org.batfish.datamodel.vendor_family.f5_bigip.ManagementIp.class));
        assertThat(ua.getPort(), equalTo(1026));
      }
      assertFalse(unicastAddresses.hasNext());
    }

    // device-group
    assertThat(
        f.getDeviceGroups(),
        hasKeys("/Common/device_group_snc", "/Common/device_trust_group", "/Common/gtm"));
    {
      org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroup dg =
          f.getDeviceGroups().get("/Common/device_group_snc");
      assertThat(dg.getAutoSync(), nullValue());

      // devices
      assertThat(
          dg.getDevices(),
          hasKeys("/Common/f5_bigip_structured_cm", "/Common/f5_bigip_structured_cm2"));
      {
        org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroupDevice dgd =
            dg.getDevices().get("/Common/f5_bigip_structured_cm");
        assertFalse(dgd.getSetSyncLeader());
      }
      {
        org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroupDevice dgd =
            dg.getDevices().get("/Common/f5_bigip_structured_cm2");
        assertFalse(dgd.getSetSyncLeader());
      }

      assertThat(dg.getHidden(), nullValue());
      assertThat(dg.getNetworkFailover(), nullValue());
      assertThat(
          dg.getType(),
          equalTo(org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroupType.SYNC_FAILOVER));
    }
    {
      org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroup dg =
          f.getDeviceGroups().get("/Common/device_trust_group");
      assertThat(dg.getAutoSync(), equalTo(Boolean.TRUE));

      // devices
      assertThat(
          dg.getDevices(),
          hasKeys("/Common/f5_bigip_structured_cm", "/Common/f5_bigip_structured_cm2"));
      {
        org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroupDevice dgd =
            dg.getDevices().get("/Common/f5_bigip_structured_cm");
        assertFalse(dgd.getSetSyncLeader());
      }
      {
        org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroupDevice dgd =
            dg.getDevices().get("/Common/f5_bigip_structured_cm2");
        assertTrue(dgd.getSetSyncLeader());
      }

      assertThat(dg.getHidden(), equalTo(Boolean.TRUE));
      assertThat(dg.getNetworkFailover(), equalTo(Boolean.FALSE));
      assertThat(
          dg.getType(),
          equalTo(org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroupType.SYNC_ONLY));
    }
    {
      org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroup dg =
          f.getDeviceGroups().get("/Common/gtm");
      assertThat(dg.getAutoSync(), nullValue());

      // devices
      assertThat(dg.getDevices(), hasKeys("/Common/f5_bigip_structured_cm"));
      {
        org.batfish.datamodel.vendor_family.f5_bigip.DeviceGroupDevice dgd =
            dg.getDevices().get("/Common/f5_bigip_structured_cm");
        assertFalse(dgd.getSetSyncLeader());
      }

      assertThat(dg.getHidden(), equalTo(Boolean.TRUE));
      assertThat(dg.getNetworkFailover(), equalTo(Boolean.FALSE));
      assertThat(dg.getType(), nullValue());
    }

    // traffic-group
    assertThat(
        f.getTrafficGroups(),
        hasKeys("/Common/traffic-group-1", "/Common/traffic-group-local-only"));
    {
      org.batfish.datamodel.vendor_family.f5_bigip.TrafficGroup tg =
          f.getTrafficGroups().get("/Common/traffic-group-1");
      assertThat(tg.getHaGroup(), equalTo("/Common/t1"));
      assertThat(tg.getMac(), equalTo(MacAddress.parse("00:00:00:00:00:03")));
      assertThat(tg.getUnitId(), equalTo(1));
    }
    {
      org.batfish.datamodel.vendor_family.f5_bigip.TrafficGroup tg =
          f.getTrafficGroups().get("/Common/traffic-group-local-only");
      assertThat(tg.getHaGroup(), nullValue());
      assertThat(tg.getMac(), nullValue());
      assertThat(tg.getUnitId(), nullValue());
    }
  }

  @Test
  public void testCmExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_structured_cm");

    // cm device
    assertThat(
        vc.getDevices(),
        hasKeys("/Common/f5_bigip_structured_cm", "/Common/f5_bigip_structured_cm2"));
    {
      Device d = vc.getDevices().get("/Common/f5_bigip_structured_cm");
      assertThat(d.getBaseMac(), equalTo(MacAddress.parse("00:00:00:00:00:01")));
      assertThat(d.getConfigSyncIp(), equalTo(Ip.parse("192.0.2.1")));
      assertThat(d.getHostname(), equalTo("f5_bigip_structured_cm"));
      assertThat(d.getManagementIp(), equalTo(Ip.parse("192.0.2.2")));
      assertThat(d.getSelfDevice(), equalTo(Boolean.TRUE));

      // unicast-address
      Iterator<UnicastAddress> unicastAddresses = d.getUnicastAddresses().iterator();
      {
        UnicastAddress ua = unicastAddresses.next();
        assertThat(ua.getEffectiveIp(), instanceOf(ConcreteUnicastAddressIp.class));
        assertThat(ua.getEffectivePort(), equalTo(1026));
        assertThat(
            ((ConcreteUnicastAddressIp) ua.getEffectiveIp()).getIp(),
            equalTo(Ip.parse("192.0.2.1")));
        assertThat(ua.getIp(), instanceOf(ConcreteUnicastAddressIp.class));
        assertThat(((ConcreteUnicastAddressIp) ua.getIp()).getIp(), equalTo(Ip.parse("192.0.2.1")));
        assertThat(ua.getPort(), nullValue());
      }
      {
        UnicastAddress ua = unicastAddresses.next();
        assertThat(ua.getEffectiveIp(), instanceOf(ManagementIp.class));
        assertThat(ua.getEffectivePort(), equalTo(1026));
        assertThat(ua.getIp(), instanceOf(ManagementIp.class));
        assertThat(ua.getPort(), equalTo(1026));
      }
      assertFalse(unicastAddresses.hasNext());
    }
    {
      Device d = vc.getDevices().get("/Common/f5_bigip_structured_cm2");
      assertThat(d.getBaseMac(), equalTo(MacAddress.parse("00:00:00:00:00:02")));
      assertThat(d.getConfigSyncIp(), equalTo(Ip.parse("192.0.2.3")));
      assertThat(d.getHostname(), equalTo("f5_bigip_structured_cm2"));
      assertThat(d.getManagementIp(), equalTo(Ip.parse("192.0.2.4")));
      assertThat(d.getSelfDevice(), nullValue());

      // unicast-address
      Iterator<UnicastAddress> unicastAddresses = d.getUnicastAddresses().iterator();
      {
        UnicastAddress ua = unicastAddresses.next();
        assertThat(ua.getEffectiveIp(), instanceOf(ConcreteUnicastAddressIp.class));
        assertThat(ua.getEffectivePort(), equalTo(1026));
        assertThat(
            ((ConcreteUnicastAddressIp) ua.getEffectiveIp()).getIp(),
            equalTo(Ip.parse("192.0.2.3")));
        assertThat(ua.getIp(), instanceOf(ConcreteUnicastAddressIp.class));
        assertThat(((ConcreteUnicastAddressIp) ua.getIp()).getIp(), equalTo(Ip.parse("192.0.2.3")));
        assertThat(ua.getPort(), nullValue());
      }
      {
        UnicastAddress ua = unicastAddresses.next();
        assertThat(ua.getEffectiveIp(), instanceOf(ManagementIp.class));
        assertThat(ua.getEffectivePort(), equalTo(1026));
        assertThat(ua.getIp(), instanceOf(ManagementIp.class));
        assertThat(ua.getPort(), equalTo(1026));
      }
      assertFalse(unicastAddresses.hasNext());
    }

    // device-group
    assertThat(
        vc.getDeviceGroups(),
        hasKeys("/Common/device_group_snc", "/Common/device_trust_group", "/Common/gtm"));
    {
      DeviceGroup dg = vc.getDeviceGroups().get("/Common/device_group_snc");
      assertThat(dg.getAutoSync(), nullValue());

      // devices
      assertThat(
          dg.getDevices(),
          hasKeys("/Common/f5_bigip_structured_cm", "/Common/f5_bigip_structured_cm2"));
      {
        DeviceGroupDevice dgd = dg.getDevices().get("/Common/f5_bigip_structured_cm");
        assertFalse(dgd.getSetSyncLeader());
      }
      {
        DeviceGroupDevice dgd = dg.getDevices().get("/Common/f5_bigip_structured_cm2");
        assertFalse(dgd.getSetSyncLeader());
      }

      assertThat(dg.getHidden(), nullValue());
      assertThat(dg.getNetworkFailover(), nullValue());
      assertThat(dg.getType(), equalTo(DeviceGroupType.SYNC_FAILOVER));
    }
    {
      DeviceGroup dg = vc.getDeviceGroups().get("/Common/device_trust_group");
      assertThat(dg.getAutoSync(), equalTo(Boolean.TRUE));

      // devices
      assertThat(
          dg.getDevices(),
          hasKeys("/Common/f5_bigip_structured_cm", "/Common/f5_bigip_structured_cm2"));
      {
        DeviceGroupDevice dgd = dg.getDevices().get("/Common/f5_bigip_structured_cm");
        assertFalse(dgd.getSetSyncLeader());
      }
      {
        DeviceGroupDevice dgd = dg.getDevices().get("/Common/f5_bigip_structured_cm2");
        assertTrue(dgd.getSetSyncLeader());
      }

      assertThat(dg.getHidden(), equalTo(Boolean.TRUE));
      assertThat(dg.getNetworkFailover(), equalTo(Boolean.FALSE));
      assertThat(dg.getType(), equalTo(DeviceGroupType.SYNC_ONLY));
    }
    {
      DeviceGroup dg = vc.getDeviceGroups().get("/Common/gtm");
      assertThat(dg.getAutoSync(), nullValue());

      // devices
      assertThat(dg.getDevices(), hasKeys("/Common/f5_bigip_structured_cm"));
      {
        DeviceGroupDevice dgd = dg.getDevices().get("/Common/f5_bigip_structured_cm");
        assertFalse(dgd.getSetSyncLeader());
      }

      assertThat(dg.getHidden(), equalTo(Boolean.TRUE));
      assertThat(dg.getNetworkFailover(), equalTo(Boolean.FALSE));
      assertThat(dg.getType(), nullValue());
    }

    // traffic-group
    assertThat(
        vc.getTrafficGroups(),
        hasKeys("/Common/traffic-group-1", "/Common/traffic-group-local-only"));
    {
      TrafficGroup tg = vc.getTrafficGroups().get("/Common/traffic-group-1");
      assertThat(tg.getHaGroup(), equalTo("/Common/t1"));
      assertThat(tg.getMac(), equalTo(MacAddress.parse("00:00:00:00:00:03")));
      assertThat(tg.getUnitId(), equalTo(1));
    }
    {
      TrafficGroup tg = vc.getTrafficGroups().get("/Common/traffic-group-local-only");
      assertThat(tg.getHaGroup(), nullValue());
      assertThat(tg.getMac(), nullValue());
      assertThat(tg.getUnitId(), nullValue());
    }
  }

  @Test
  public void testDataGroupDefinitions() throws IOException {
    String hostname = "f5_bigip_structured_ltm_data_group";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ans.getDefinedStructures().get(file).get(DATA_GROUP_INTERNAL.getDescription()),
        aMapWithSize(2));
    assertThat(ans, hasNumReferrers(file, DATA_GROUP_INTERNAL, "/Common/complex", 1));
    assertThat(ans, hasNumReferrers(file, DATA_GROUP_INTERNAL, "/Common/simple", 1));
  }

  @Test
  public void testDnat() throws IOException {
    String snapshotName = "dnat";
    String natHostname = "f5_bigip_structured_dnat";
    String hostname = "host1";
    String hostFilename = hostname + ".json";

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(SNAPSHOTS_PREFIX + snapshotName, natHostname)
                .setHostsFiles(SNAPSHOTS_PREFIX + snapshotName, hostFilename)
                .build(),
            _folder);
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);

    {
      // DNAT modulo ARP
      Flow flow =
          Flow.builder()
              .setDstIp(Ip.parse("192.0.2.1"))
              .setDstPort(80)
              .setIngressInterface("/Common/SOME_VLAN")
              .setIngressNode(natHostname)
              .setIpProtocol(IpProtocol.TCP)
              .setSrcIp(Ip.parse("8.8.8.8"))
              .setSrcPort(50000)
              .build();
      SortedMap<Flow, List<Trace>> flowTraces =
          batfish.getTracerouteEngine(snapshot).computeTraces(ImmutableSet.of(flow), false);
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
              .setDstIp(Ip.parse("192.0.2.1"))
              .setDstPort(80)
              .setIngressNode(hostname)
              .setIpProtocol(IpProtocol.TCP)
              .setSrcIp(Ip.parse("192.0.2.2"))
              .setSrcPort(50000)
              .build();
      SortedMap<Flow, List<Trace>> flowTraces =
          batfish.getTracerouteEngine(snapshot).computeTraces(ImmutableSet.of(flow), false);
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
      // bidirectional traceroute with DNAT
      Flow flow =
          Flow.builder()
              .setDstIp(Ip.parse("192.0.2.1"))
              .setDstPort(80)
              .setIngressInterface("/Common/SOME_VLAN")
              .setIngressNode(natHostname)
              .setIpProtocol(IpProtocol.TCP)
              .setSrcIp(Ip.parse("8.8.8.8"))
              .setSrcPort(50000)
              .build();
      SortedMap<Flow, List<TraceAndReverseFlow>> flowTraces =
          batfish
              .getTracerouteEngine(snapshot)
              .computeTracesAndReverseFlows(ImmutableSet.of(flow), false);
      List<TraceAndReverseFlow> traces = flowTraces.get(flow);

      assertThat(traces, hasSize(1));

      Flow reverseFlow = traces.get(0).getReverseFlow();
      assertThat(
          reverseFlow,
          equalTo(
              Flow.builder()
                  .setSrcIp(Ip.parse("192.0.2.10"))
                  .setSrcPort(80)
                  .setIngressInterface("/Common/SOME_VLAN")
                  .setIngressNode(natHostname)
                  .setIpProtocol(IpProtocol.TCP)
                  .setDstIp(Ip.parse("8.8.8.8"))
                  .setDstPort(50000)
                  .build()));

      Set<FirewallSessionTraceInfo> sessions = traces.get(0).getNewFirewallSessions();
      SortedMap<Flow, List<TraceAndReverseFlow>> reverseFlowTraces =
          batfish
              .getTracerouteEngine(snapshot)
              .computeTracesAndReverseFlows(ImmutableSet.of(reverseFlow), sessions, false);

      Optional<TransformationStepDetail> stepDetailOptional =
          reverseFlowTraces.get(reverseFlow).stream()
              .map(TraceAndReverseFlow::getTrace)
              .map(Trace::getHops)
              .flatMap(Collection::stream)
              .map(Hop::getSteps)
              .flatMap(Collection::stream)
              .map(Step::getDetail)
              .filter(Predicates.instanceOf(TransformationStepDetail.class))
              .map(TransformationStepDetail.class::cast)
              .filter(d -> d.getTransformationType() == TransformationType.SOURCE_NAT)
              .findFirst();

      assertTrue("There is a DNAT transformation step.", stepDetailOptional.isPresent());

      TransformationStepDetail detail = stepDetailOptional.get();

      assertThat(
          detail.getFlowDiffs(),
          hasItem(
              equalTo(
                  FlowDiff.flowDiff(
                      IpField.SOURCE, Ip.parse("192.0.2.10"), Ip.parse("192.0.2.1")))));
    }
  }

  @Test
  public void testHaGroupExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_structured_sys_ha_group");

    assertThat(vc.getHaGroups(), hasKeys("g1"));
    {
      HaGroup g = vc.getHaGroups().get("g1");
      assertThat(g.getActiveBonus(), equalTo(12));

      // pools
      assertThat(g.getPools(), hasKeys("/Common/p1", "/Common/p2"));
      {
        HaGroupPool p = g.getPools().get("/Common/p1");
        assertThat(p.getWeight(), equalTo(34));
      }
      {
        HaGroupPool p = g.getPools().get("/Common/p2");
        assertThat(p.getWeight(), nullValue());
      }

      // trunks
      assertThat(g.getTrunks(), hasKeys("t1", "t2"));
      {
        HaGroupTrunk t = g.getTrunks().get("t1");
        assertThat(t.getWeight(), equalTo(56));
      }
      {
        HaGroupTrunk t = g.getTrunks().get("t2");
        assertThat(t.getWeight(), nullValue());
      }
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
  public void testHostnameCapital() throws IOException {
    String filename = "f5_bigip_structured_hostname_capital";
    String hostname = "myhostname";
    Map<String, Configuration> configurations = parseTextConfigs(filename);

    assertThat(configurations, hasKey(hostname));
  }

  @Test
  public void testImish() {
    assertTrue(
        "Configuration contains an imish component",
        parseVendorConfig("f5_bigip_structured_with_imish").getImish());
  }

  @Test
  public void testInterfaceDescriptionExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_structured_net_interface_description");

    assertThat(vc.getInterfaces(), hasKeys("1.0", "2.0"));
    assertNull(vc.getInterfaces().get("1.0").getDescription());
    assertThat(vc.getInterfaces().get("2.0").getDescription(), equalTo("foo"));
  }

  @Test
  public void testInterfaceDisabledConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_net_interface_disabled");

    assertThat(
        c.getAllInterfaces(),
        hasKeys("1.0", "2.0", "3.0", "11.0", "12.0", "13.0", "trunk1", "/Common/vlan1"));

    assertFalse(c.getAllInterfaces().get("1.0").getActive());
    assertTrue(c.getAllInterfaces().get("2.0").getActive());
    assertTrue(c.getAllInterfaces().get("3.0").getActive());
    assertFalse(c.getAllInterfaces().get("11.0").getActive());
    assertTrue(c.getAllInterfaces().get("12.0").getActive());
    assertTrue(c.getAllInterfaces().get("13.0").getActive());
  }

  @Test
  public void testInterfaceDisabledExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_structured_net_interface_disabled");

    assertThat(vc.getInterfaces(), hasKeys("1.0", "2.0", "3.0", "11.0", "12.0", "13.0"));

    assertTrue(vc.getInterfaces().get("1.0").getDisabled());
    assertFalse(vc.getInterfaces().get("2.0").getDisabled());
    assertThat(vc.getInterfaces().get("3.0").getDisabled(), nullValue());
    assertTrue(vc.getInterfaces().get("11.0").getDisabled());
    assertFalse(vc.getInterfaces().get("12.0").getDisabled());
    assertThat(vc.getInterfaces().get("13.0").getDisabled(), nullValue());
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, VLAN_MEMBER_INTERFACE, undefined));

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
  public void testRuleDefinitions() throws IOException {
    String hostname = "f5_bigip_structured_ltm_rule";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(
        ans.getDefinedStructures().get(file).get(F5BigipStructureType.RULE.getDescription()),
        aMapWithSize(11));
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // monitor dns
    {
      String undefined = "/Common/monitor_dns_undefined";
      String unused = "/Common/monitor_dns_unused";
      String used = "/Common/monitor_dns_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, MONITOR, undefined));
      assertThat(ans, hasUndefinedReference(file, MONITOR_DNS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, MONITOR_DNS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, MONITOR_DNS, used, 2));
    }

    // monitor gateway-icmp
    {
      String undefined = "/Common/monitor_gateway_icmp_undefined";
      String unused = "/Common/monitor_gateway_icmp_unused";
      String used = "/Common/monitor_gateway_icmp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, MONITOR, undefined));
      assertThat(ans, hasUndefinedReference(file, MONITOR_GATEWAY_ICMP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, MONITOR_GATEWAY_ICMP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, MONITOR_GATEWAY_ICMP, used, 2));
    }

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

    // monitor ldap
    {
      String undefined = "/Common/monitor_ldap_undefined";
      String unused = "/Common/monitor_ldap_unused";
      String used = "/Common/monitor_ldap_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, MONITOR, undefined));
      assertThat(ans, hasUndefinedReference(file, MONITOR_LDAP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, MONITOR_LDAP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, MONITOR_LDAP, used, 2));
    }

    // monitor tcp
    {
      String undefined = "/Common/monitor_tcp_undefined";
      String unused = "/Common/monitor_tcp_unused";
      String used = "/Common/monitor_tcp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, MONITOR, undefined));
      assertThat(ans, hasUndefinedReference(file, MONITOR_TCP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, MONITOR_TCP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, MONITOR_TCP, used, 2));
    }

    // conjunction of monitors
    {
      String undefined1 = "/Common/undef1";
      String undefined2 = "/Common/undef2";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, MONITOR, undefined1));
      assertThat(ans, hasUndefinedReference(file, MONITOR, undefined2));
    }

    assertNoUndefinedReferencesToBuiltins(
        ans,
        Stream.of(
            MONITOR,
            MONITOR_DNS,
            MONITOR_GATEWAY_ICMP,
            MONITOR_HTTP,
            MONITOR_HTTPS,
            MONITOR_LDAP,
            MONITOR_TCP),
        BuiltinMonitor::getBuiltinMonitor);
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, NODE, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, NODE, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, NODE, used, 1));
  }

  @Test
  public void testNtp() throws IOException {
    String hostname = "f5_bigip_structured_sys_ntp";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getNtpServers(),
        containsInAnyOrder("0.ntp.example.com", "1.ntp.example.com", "192.0.2.1"));
  }

  @Test
  public void testPersistenceReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // persistence cookie
    {
      String undefined = "/Common/persistence_cookie_undefined";
      String unused = "/Common/persistence_cookie_unused";
      String used = "/Common/persistence_cookie_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PERSISTENCE, undefined));
      assertThat(ans, hasUndefinedReference(file, PERSISTENCE_COOKIE, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PERSISTENCE_COOKIE, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PERSISTENCE_COOKIE, used, 2));
    }

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
        Stream.of(PERSISTENCE, PERSISTENCE_COOKIE, PERSISTENCE_SOURCE_ADDR, PERSISTENCE_SSL),
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
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

    assertThat(v6, Route6FilterListMatchers.permits(Prefix6.parse("dead:beef:1::/64")));
    assertThat(v6, Route6FilterListMatchers.rejects(Prefix6.parse("dead:beef:1::/128")));

    // The invalid list should not make it into the data model
    assertThat(c, hasRouteFilterLists(not(hasKey(invalidName))));
    assertThat(c, hasRoute6FilterLists(not(hasKey(invalidName))));

    // Check errors
    Warnings warnings =
        batfish.initInfo(batfish.getSnapshot(), false, true).getWarnings().get(hostname);

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // profile analytics
    {
      String undefined = "/Common/profile_analytics_undefined";
      String unused = "/Common/profile_analytics_unused";
      String used = "/Common/profile_analytics_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_ANALYTICS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_ANALYTICS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_ANALYTICS, used, 2));
    }

    // profile certificate-authority
    {
      String undefined = "/Common/profile_certificate_authority_undefined";
      String unused = "/Common/profile_certificate_authority_unused";
      String used = "/Common/profile_certificate_authority_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_CERTIFICATE_AUTHORITY, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_CERTIFICATE_AUTHORITY, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_CERTIFICATE_AUTHORITY, used, 2));
    }

    // profile classification
    {
      String undefined = "/Common/profile_classification_undefined";
      String unused = "/Common/profile_classification_unused";
      String used = "/Common/profile_classification_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_CLASSIFICATION, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_CLASSIFICATION, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_CLASSIFICATION, used, 2));
    }

    // profile client-ldap
    {
      String undefined = "/Common/profile_client_ldap_undefined";
      String unused = "/Common/profile_client_ldap_unused";
      String used = "/Common/profile_client_ldap_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_CLIENT_LDAP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_CLIENT_LDAP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_CLIENT_LDAP, used, 2));
    }

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

    // profile dhcpv4
    {
      String undefined = "/Common/profile_dhcpv4_undefined";
      String unused = "/Common/profile_dhcpv4_unused";
      String used = "/Common/profile_dhcpv4_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_DHCPV4, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_DHCPV4, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_DHCPV4, used, 2));
    }

    // profile dhcpv6
    {
      String undefined = "/Common/profile_dhcpv6_undefined";
      String unused = "/Common/profile_dhcpv6_unused";
      String used = "/Common/profile_dhcpv6_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_DHCPV6, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_DHCPV6, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_DHCPV6, used, 2));
    }

    // profile diameter
    {
      String undefined = "/Common/profile_diameter_undefined";
      String unused = "/Common/profile_diameter_unused";
      String used = "/Common/profile_diameter_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_DIAMETER, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_DIAMETER, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_DIAMETER, used, 2));
    }

    // profile dns
    {
      String undefined = "/Common/profile_dns_undefined";
      String unused = "/Common/profile_dns_unused";
      String used = "/Common/profile_dns_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_DNS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_DNS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_DNS, used, 2));
    }

    // profile fasthttp
    {
      String undefined = "/Common/profile_fasthttp_undefined";
      String unused = "/Common/profile_fasthttp_unused";
      String used = "/Common/profile_fasthttp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_FASTHTTP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_FASTHTTP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_FASTHTTP, used, 2));
    }

    // profile fastl4
    {
      String undefined = "/Common/profile_fastl4_undefined";
      String unused = "/Common/profile_fastl4_unused";
      String used = "/Common/profile_fastl4_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_FASTL4, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_FASTL4, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_FASTL4, used, 2));
    }

    // profile fix
    {
      String undefined = "/Common/profile_fix_undefined";
      String unused = "/Common/profile_fix_unused";
      String used = "/Common/profile_fix_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_FIX, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_FIX, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_FIX, used, 2));
    }

    // profile ftp
    {
      String undefined = "/Common/profile_ftp_undefined";
      String unused = "/Common/profile_ftp_unused";
      String used = "/Common/profile_ftp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_FTP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_FTP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_FTP, used, 2));
    }

    // profile gtp
    {
      String undefined = "/Common/profile_gtp_undefined";
      String unused = "/Common/profile_gtp_unused";
      String used = "/Common/profile_gtp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_GTP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_GTP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_GTP, used, 2));
    }

    // profile html
    {
      String undefined = "/Common/profile_html_undefined";
      String unused = "/Common/profile_html_unused";
      String used = "/Common/profile_html_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_HTML, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_HTML, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_HTML, used, 2));
    }

    // profile http2
    {
      String undefined = "/Common/profile_http2_undefined";
      String unused = "/Common/profile_http2_unused";
      String used = "/Common/profile_http2_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_HTTP2, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_HTTP2, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_HTTP2, used, 2));
    }

    // profile http-compression
    {
      String undefined = "/Common/profile_http_compression_undefined";
      String unused = "/Common/profile_http_compression_unused";
      String used = "/Common/profile_http_compression_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_HTTP_COMPRESSION, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_HTTP_COMPRESSION, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_HTTP_COMPRESSION, used, 2));
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

    // profile http-proxy-connect
    {
      String undefined = "/Common/profile_http_proxy_connect_undefined";
      String unused = "/Common/profile_http_proxy_connect_unused";
      String used = "/Common/profile_http_proxy_connect_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_HTTP_PROXY_CONNECT, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_HTTP_PROXY_CONNECT, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_HTTP_PROXY_CONNECT, used, 2));
    }

    // profile icap
    {
      String undefined = "/Common/profile_icap_undefined";
      String unused = "/Common/profile_icap_unused";
      String used = "/Common/profile_icap_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_ICAP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_ICAP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_ICAP, used, 2));
    }

    // profile ilx
    {
      String undefined = "/Common/profile_ilx_undefined";
      String unused = "/Common/profile_ilx_unused";
      String used = "/Common/profile_ilx_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_ILX, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_ILX, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_ILX, used, 2));
    }

    // profile ipother
    {
      String undefined = "/Common/profile_ipother_undefined";
      String unused = "/Common/profile_ipother_unused";
      String used = "/Common/profile_ipother_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_IPOTHER, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_IPOTHER, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_IPOTHER, used, 2));
    }

    // profile ipsecalg
    {
      String undefined = "/Common/profile_ipsecalg_undefined";
      String unused = "/Common/profile_ipsecalg_unused";
      String used = "/Common/profile_ipsecalg_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_IPSECALG, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_IPSECALG, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_IPSECALG, used, 2));
    }

    // profile map-t
    {
      String undefined = "/Common/profile_map_t_undefined";
      String unused = "/Common/profile_map_t_unused";
      String used = "/Common/profile_map_t_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_MAP_T, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_MAP_T, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_MAP_T, used, 2));
    }

    // profile mqtt
    {
      String undefined = "/Common/profile_mqtt_undefined";
      String unused = "/Common/profile_mqtt_unused";
      String used = "/Common/profile_mqtt_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_MQTT, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_MQTT, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_MQTT, used, 2));
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

    // profile netflow
    {
      String undefined = "/Common/profile_netflow_undefined";
      String unused = "/Common/profile_netflow_unused";
      String used = "/Common/profile_netflow_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_NETFLOW, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_NETFLOW, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_NETFLOW, used, 2));
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

    // profile pcp
    {
      String undefined = "/Common/profile_pcp_undefined";
      String unused = "/Common/profile_pcp_unused";
      String used = "/Common/profile_pcp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_PCP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_PCP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_PCP, used, 2));
    }

    // profile pptp
    {
      String undefined = "/Common/profile_pptp_undefined";
      String unused = "/Common/profile_pptp_unused";
      String used = "/Common/profile_pptp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_PPTP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_PPTP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_PPTP, used, 2));
    }

    // profile qoe
    {
      String undefined = "/Common/profile_qoe_undefined";
      String unused = "/Common/profile_qoe_unused";
      String used = "/Common/profile_qoe_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_QOE, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_QOE, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_QOE, used, 2));
    }

    // profile radius
    {
      String undefined = "/Common/profile_radius_undefined";
      String unused = "/Common/profile_radius_unused";
      String used = "/Common/profile_radius_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_RADIUS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_RADIUS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_RADIUS, used, 2));
    }

    // profile request-adapt
    {
      String undefined = "/Common/profile_request_adapt_undefined";
      String unused = "/Common/profile_request_adapt_unused";
      String used = "/Common/profile_request_adapt_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_REQUEST_ADAPT, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_REQUEST_ADAPT, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_REQUEST_ADAPT, used, 2));
    }

    // profile request-log
    {
      String undefined = "/Common/profile_request_log_undefined";
      String unused = "/Common/profile_request_log_unused";
      String used = "/Common/profile_request_log_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_REQUEST_LOG, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_REQUEST_LOG, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_REQUEST_LOG, used, 2));
    }

    // profile response-adapt
    {
      String undefined = "/Common/profile_response_adapt_undefined";
      String unused = "/Common/profile_response_adapt_unused";
      String used = "/Common/profile_response_adapt_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_RESPONSE_ADAPT, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_RESPONSE_ADAPT, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_RESPONSE_ADAPT, used, 2));
    }

    // profile rewrite
    {
      String undefined = "/Common/profile_rewrite_undefined";
      String unused = "/Common/profile_rewrite_unused";
      String used = "/Common/profile_rewrite_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_REWRITE, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_REWRITE, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_REWRITE, used, 2));
    }

    // profile rtsp
    {
      String undefined = "/Common/profile_rtsp_undefined";
      String unused = "/Common/profile_rtsp_unused";
      String used = "/Common/profile_rtsp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_RTSP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_RTSP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_RTSP, used, 2));
    }

    // profile sctp
    {
      String undefined = "/Common/profile_sctp_undefined";
      String unused = "/Common/profile_sctp_unused";
      String used = "/Common/profile_sctp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_SCTP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_SCTP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_SCTP, used, 2));
    }

    // profile server-ldap
    {
      String undefined = "/Common/profile_server_ldap_undefined";
      String unused = "/Common/profile_server_ldap_unused";
      String used = "/Common/profile_server_ldap_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_SERVER_LDAP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_SERVER_LDAP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_SERVER_LDAP, used, 2));
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

    // profile sip
    {
      String undefined = "/Common/profile_sip_undefined";
      String unused = "/Common/profile_sip_unused";
      String used = "/Common/profile_sip_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_SIP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_SIP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_SIP, used, 2));
    }

    // profile smtps
    {
      String undefined = "/Common/profile_smtps_undefined";
      String unused = "/Common/profile_smtps_unused";
      String used = "/Common/profile_smtps_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_SMTPS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_SMTPS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_SMTPS, used, 2));
    }

    // profile socks
    {
      String undefined = "/Common/profile_socks_undefined";
      String unused = "/Common/profile_socks_unused";
      String used = "/Common/profile_socks_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_SOCKS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_SOCKS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_SOCKS, used, 2));
    }

    // profile splitsessionclient
    {
      String undefined = "/Common/profile_splitsessionclient_undefined";
      String unused = "/Common/profile_splitsessionclient_unused";
      String used = "/Common/profile_splitsessionclient_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_SPLITSESSIONCLIENT, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_SPLITSESSIONCLIENT, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_SPLITSESSIONCLIENT, used, 2));
    }

    // profile splitsessionserver
    {
      String undefined = "/Common/profile_splitsessionserver_undefined";
      String unused = "/Common/profile_splitsessionserver_unused";
      String used = "/Common/profile_splitsessionserver_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_SPLITSESSIONSERVER, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_SPLITSESSIONSERVER, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_SPLITSESSIONSERVER, used, 2));
    }

    // profile statistics
    {
      String undefined = "/Common/profile_statistics_undefined";
      String unused = "/Common/profile_statistics_unused";
      String used = "/Common/profile_statistics_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_STATISTICS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_STATISTICS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_STATISTICS, used, 2));
    }

    // profile stream
    {
      String undefined = "/Common/profile_stream_undefined";
      String unused = "/Common/profile_stream_unused";
      String used = "/Common/profile_stream_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_STREAM, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_STREAM, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_STREAM, used, 2));
    }

    // profile tcp-analytics
    {
      String undefined = "/Common/profile_tcp_analytics_undefined";
      String unused = "/Common/profile_tcp_analytics_unused";
      String used = "/Common/profile_tcp_analytics_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_TCP_ANALYTICS, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_TCP_ANALYTICS, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_TCP_ANALYTICS, used, 2));
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

    // profile tftp
    {
      String undefined = "/Common/profile_tftp_undefined";
      String unused = "/Common/profile_tftp_unused";
      String used = "/Common/profile_tftp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_TFTP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_TFTP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_TFTP, used, 2));
    }

    // profile traffic-acceleration
    {
      String undefined = "/Common/profile_traffic_acceleration_undefined";
      String unused = "/Common/profile_traffic_acceleration_unused";
      String used = "/Common/profile_traffic_acceleration_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_TRAFFIC_ACCELERATION, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_TRAFFIC_ACCELERATION, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_TRAFFIC_ACCELERATION, used, 2));
    }

    // profile udp
    {
      String undefined = "/Common/profile_udp_undefined";
      String unused = "/Common/profile_udp_unused";
      String used = "/Common/profile_udp_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_UDP, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_UDP, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_UDP, used, 2));
    }

    // profile web-acceleration
    {
      String undefined = "/Common/profile_web_acceleration_undefined";
      String unused = "/Common/profile_web_acceleration_unused";
      String used = "/Common/profile_web_acceleration_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_WEB_ACCELERATION, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_WEB_ACCELERATION, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_WEB_ACCELERATION, used, 2));
    }

    // profile web-security
    {
      String undefined = "/Common/profile_web_security_undefined";
      String unused = "/Common/profile_web_security_unused";
      String used = "/Common/profile_web_security_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_WEB_SECURITY, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_WEB_SECURITY, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_WEB_SECURITY, used, 2));
    }

    // profile websocket
    {
      String undefined = "/Common/profile_websocket_undefined";
      String unused = "/Common/profile_websocket_unused";
      String used = "/Common/profile_websocket_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_WEBSOCKET, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_WEBSOCKET, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_WEBSOCKET, used, 2));
    }

    // profile xml
    {
      String undefined = "/Common/profile_xml_undefined";
      String unused = "/Common/profile_xml_unused";
      String used = "/Common/profile_xml_used";
      // detect undefined references
      assertThat(ans, hasUndefinedReference(file, PROFILE, undefined));
      assertThat(ans, hasUndefinedReference(file, PROFILE_XML, undefined));

      // detected unused structure
      assertThat(ans, hasNumReferrers(file, PROFILE_XML, unused, 0));

      // detect all structure references
      assertThat(ans, hasNumReferrers(file, PROFILE_XML, used, 2));
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
  public void testRouteConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_net_route");

    assertThat(c, hasDefaultVrf(hasStaticRoutes(hasSize(1))));

    StaticRoute sr = c.getDefaultVrf().getStaticRoutes().iterator().next();

    assertThat(sr, hasAdministrativeCost(1));
    assertThat(sr, hasMetric(0L));
    assertThat(sr, hasNextHopIp(Ip.parse("192.0.2.1")));
    assertThat(sr, hasPrefix(Prefix.strict("10.0.0.0/8")));
  }

  @Test
  public void testRouteExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_structured_net_route");

    String routeName = "/Common/route1";

    assertThat(vc.getRoutes(), hasKeys(routeName));

    Route route = vc.getRoutes().get(routeName);

    assertThat(route.getName(), equalTo(routeName));
    assertThat(route.getGw(), equalTo(Ip.parse("192.0.2.1")));
    assertThat(route.getNetwork(), equalTo(Prefix.parse("10.0.0.0/8")));
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
                    .setOriginalRoute(
                        new ConnectedRoute(Prefix.strict("10.0.0.0/24"), "/Common/outint"))
                    .build())
            .getBooleanValue());

    ConnectedRoute acceptedRoute =
        new ConnectedRoute(Prefix.strict("10.0.1.0/24"), "/Common/outint");
    Bgpv4Route.Builder outputRoute =
        Bgpv4Route.testBuilder()
            .setNetwork(acceptedRoute.getNetwork())
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    Environment acceptedPrefixEnvironment =
        Environment.builder(c)
            .setDirection(Direction.OUT)
            .setOutputRoute(outputRoute)
            .setOriginalRoute(acceptedRoute)
            .build();
    Result acceptedBy20 = rm1.call(acceptedPrefixEnvironment);

    assertTrue("rm1 accepts prefix 10.0.1.0/24 (via 20)", acceptedBy20.getBooleanValue());
    assertThat(
        "rm1 sets communities 1:2 and 33:44 on the output route",
        outputRoute.build().getCommunities().getCommunities(),
        equalTo(ImmutableSet.of(StandardCommunity.of(1, 2), StandardCommunity.of(33, 44))));

    assertTrue(
        "rm1 rejects prefix 10.0.2.0/24 (no matching entry)",
        !rm1.call(
                Environment.builder(c)
                    .setDirection(Direction.OUT)
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, ROUTE_MAP, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, ROUTE_MAP, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, ROUTE_MAP, used, 3));
  }

  @Test
  public void testRouteReferences() throws IOException {
    String hostname = "f5_bigip_structured_net_route";
    String file = "configs/" + hostname;
    String used = "/Common/route1";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, ROUTE, used, 1));
  }

  @Test
  public void testRuleReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, SELF, used, 1));
  }

  @Test
  public void testSnatBidirectionalTraceroute() throws IOException {
    String hostname = "f5_bigip_structured_snat";

    parseConfig(hostname);

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);

    // SNAT via snat /Common/snat1
    Flow flow =
        Flow.builder()
            .setDstIp(Ip.parse("192.0.2.1"))
            .setDstPort(80)
            .setIngressInterface("/Common/vlan1")
            .setIngressNode(hostname)
            .setIpProtocol(IpProtocol.TCP)
            .setSrcIp(Ip.parse("8.8.8.8"))
            .setSrcPort(50000)
            .build();
    SortedMap<Flow, List<TraceAndReverseFlow>> flowTraces =
        batfish
            .getTracerouteEngine(snapshot)
            .computeTracesAndReverseFlows(ImmutableSet.of(flow), false);
    List<TraceAndReverseFlow> traces = flowTraces.get(flow);

    assertThat(traces, hasSize(1));

    Flow reverseFlow = traces.get(0).getReverseFlow();
    assertThat(
        reverseFlow,
        equalTo(
            Flow.builder()
                .setSrcIp(Ip.parse("192.0.2.1"))
                .setSrcPort(80)
                .setIngressInterface("/Common/vlan1")
                .setIngressNode(hostname)
                .setIpProtocol(IpProtocol.TCP)
                .setDstIp(Ip.parse("10.200.1.2"))
                .setDstPort(1024)
                .build()));

    Set<FirewallSessionTraceInfo> sessions = traces.get(0).getNewFirewallSessions();
    SortedMap<Flow, List<TraceAndReverseFlow>> reverseFlowTraces =
        batfish
            .getTracerouteEngine(snapshot)
            .computeTracesAndReverseFlows(ImmutableSet.of(reverseFlow), sessions, false);

    Optional<TransformationStepDetail> stepDetailOptional =
        reverseFlowTraces.get(reverseFlow).stream()
            .map(TraceAndReverseFlow::getTrace)
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
        contains(
            FlowDiff.flowDiff(IpField.DESTINATION, Ip.parse("10.200.1.2"), Ip.parse("8.8.8.8")),
            FlowDiff.flowDiff(PortField.DESTINATION, 1024, 50000)));
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
  public void testSnatMatchingSnatButNoVirtual() throws IOException {
    String hostname = "f5_bigip_structured_snat";

    Configuration c = parseConfig(hostname);

    // Assume a flow is going out of /Common/vlan1
    Transformation outgoingTransformation =
        c.getAllInterfaces().get("/Common/vlan1").getOutgoingTransformation();

    // SNAT via snat /Common/snat1
    Flow flow =
        Flow.builder()
            .setDstIp(Ip.parse("192.0.2.1"))
            .setDstPort(80)
            .setIngressInterface("/Common/vlan1")
            .setIngressNode(hostname)
            .setIpProtocol(IpProtocol.TCP)
            .setSrcIp(Ip.parse("10.100.1.1"))
            .setSrcPort(50000)
            .build();
    TransformationResult result =
        eval(outgoingTransformation, flow, "dummy", ImmutableMap.of(), ImmutableMap.of());
    Optional<TransformationStepDetail> stepDetailOptional =
        result.getTraceSteps().stream()
            .map(Step::getDetail)
            .filter(Predicates.instanceOf(TransformationStepDetail.class))
            .map(TransformationStepDetail.class::cast)
            .filter(d -> d.getTransformationType() == TransformationType.SOURCE_NAT)
            .findFirst();

    assertTrue("There is an SNAT transformation step.", stepDetailOptional.isPresent());

    TransformationStepDetail detail = stepDetailOptional.get();

    assertThat(
        detail.getFlowDiffs(),
        hasItem(isIpRewrite(IpField.SOURCE, Ip.parse("10.100.1.1"), Ip.parse("10.200.1.2"))));
    assertThat(
        detail.getFlowDiffs(),
        hasItem(
            isPortRewrite(
                PortField.SOURCE,
                equalTo(50000),
                both(greaterThanOrEqualTo(1024)).and(lessThanOrEqualTo(65535)))));
  }

  // TODO: re-enable after it becomes possible to remember state between incoming and outgoing
  // transformations https://github.com/batfish/batfish/issues/3243
  @Ignore
  @Test
  public void testSnatMatchingVirtual() throws IOException {
    String hostname = "f5_bigip_structured_snat";
    Configuration c = parseConfig("hostname");

    // Assume a flow is going out of /Common/vlan1
    Transformation outgoingTransformation =
        c.getAllInterfaces().get("/Common/vlan1").getOutgoingTransformation();

    // SNAT via virtual /Common/virtual1
    Flow flow =
        Flow.builder()
            .setDstIp(Ip.parse("192.0.2.1"))
            .setDstPort(80)
            .setIngressInterface("/Common/vlan1")
            .setIngressNode(hostname)
            .setIpProtocol(IpProtocol.TCP)
            .setSrcIp(Ip.parse("8.8.8.8"))
            .setSrcPort(50000)
            .build();
    // TODO: transformation context must include fact that /Common/virtual1 was matched during
    // incoming transformation phase
    TransformationResult result =
        eval(outgoingTransformation, flow, "dummy", ImmutableMap.of(), ImmutableMap.of());
    Optional<TransformationStepDetail> stepDetailOptional =
        result.getTraceSteps().stream()
            .map(Step::getDetail)
            .filter(Predicates.instanceOf(TransformationStepDetail.class))
            .map(TransformationStepDetail.class::cast)
            .filter(d -> d.getTransformationType() == TransformationType.SOURCE_NAT)
            .findFirst();

    assertTrue("There is an SNAT transformation step.", stepDetailOptional.isPresent());

    TransformationStepDetail detail = stepDetailOptional.get();

    assertThat(
        detail.getFlowDiffs(),
        hasItem(
            equalTo(
                FlowDiff.flowDiff(IpField.SOURCE, Ip.parse("8.8.8.8"), Ip.parse("10.200.1.1")))));
    assertThat(
        detail.getFlowDiffs(),
        hasItem(
            isPortRewrite(
                PortField.SOURCE,
                equalTo(50000),
                both(greaterThanOrEqualTo(1024)).and(lessThanOrEqualTo(65535)))));
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
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
  public void testTrunkInterfaceImplicitConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_net_trunk_interface_implicit");

    assertThat(c, hasInterface("1.0", isActive()));
  }

  @Test
  public void testTrunkInterfaceImplicitExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_structured_net_trunk_interface_implicit");
    String ifaceName = "1.0";

    assertThat(vc.getInterfaces(), hasKey(ifaceName));
    assertThat(vc.getInterfaces().get(ifaceName).getDisabled(), nullValue());
  }

  @Test
  public void testTrunk() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_trunk");
    String trunk1Name = "trunk1";
    String trunk2Name = "trunk2";

    assertThat(
        c.getAllInterfaces().keySet(), containsInAnyOrder(trunk1Name, trunk2Name, "1.0", "2.0"));

    //// trunk1
    // Should be disabled since it has no members
    assertThat(c, hasInterface(trunk1Name, isActive(false)));
    assertThat(c, hasInterface(trunk1Name, hasInterfaceType(AGGREGATED)));

    //// trunk2
    assertThat(c, hasInterface(trunk2Name, isActive(true)));
    // Each of the two constituent interfaces has bandwidth of 40E9
    assertThat(c, hasInterface(trunk2Name, hasBandwidth(equalTo(80E9))));
    assertThat(
        c,
        hasInterface(
            trunk2Name,
            hasDependencies(
                containsInAnyOrder(
                    new Dependency("1.0", AGGREGATE), new Dependency("2.0", AGGREGATE)))));
    assertThat(c, hasInterface(trunk2Name, hasInterfaceType(AGGREGATED)));
  }

  @Test
  public void testTrunkReferences() throws IOException {
    String hostname = "f5_bigip_structured_trunk_references";
    String file = "configs/" + hostname;
    String undefined = "trunk_undefined";
    String unused = "trunk_unused";
    String used = "trunk_used";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, VLAN_MEMBER_INTERFACE, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, TRUNK, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, TRUNK, used, 1));
  }

  @Test
  public void testUnrecognized() throws IOException {
    String hostname = "f5_bigip_structured_unrecognized";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.getSettings().setDisableUnrecognized(false);
    batfish.getSettings().setThrowOnLexerError(false);
    batfish.getSettings().setThrowOnParserError(false);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    assertThat(c, hasInterfaces(hasKey("1.0")));
    InitInfoAnswerElement initAns = batfish.initInfo(batfish.getSnapshot(), false, true);
    assertThat(initAns.getParseStatus().get(filename), equalTo(ParseStatus.PARTIALLY_UNRECOGNIZED));
  }

  @Test
  public void testVipDescriptionConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_vip_description");
    F5BigipFamily f = c.getVendorFamily().getF5Bigip();

    // check structure presence
    assertThat(f.getPools(), hasKeys("/Common/pool1", "/Common/pool2", "/Common/pool3"));
    assertThat(
        f.getVirtuals(), hasKeys("/Common/virtual1", "/Common/virtual2", "/Common/virtual3"));
    assertThat(
        f.getVirtualAddresses(),
        hasKeys("/Common/192.0.2.1", "/Common/192.0.2.2", "/Common/192.0.2.3"));
    assertThat(f.getPools().get("/Common/pool1").getMembers(), hasKeys("/Common/node1:80"));
    assertThat(f.getPools().get("/Common/pool2").getMembers(), hasKeys("/Common/node2:80"));
    assertThat(f.getPools().get("/Common/pool3").getMembers(), hasKeys("/Common/node3:80"));

    // check descriptions
    assertThat(f.getPools().get("/Common/pool1").getDescription(), equalTo("pool1 is cool"));
    assertThat(f.getPools().get("/Common/pool2").getDescription(), equalTo("pool2 is lame"));
    assertThat(f.getPools().get("/Common/pool3").getDescription(), nullValue());

    assertThat(
        f.getVirtuals().get("/Common/virtual1").getDescription(), equalTo("virtual1 is cool"));
    assertThat(f.getVirtuals().get("/Common/virtual2").getDescription(), nullValue());
    assertThat(f.getVirtuals().get("/Common/virtual3").getDescription(), nullValue());

    assertThat(
        f.getPools().get("/Common/pool1").getMembers().get("/Common/node1:80").getDescription(),
        equalTo("node1_is_cool"));
    assertThat(
        f.getPools().get("/Common/pool2").getMembers().get("/Common/node2:80").getDescription(),
        equalTo("node2_is_lame"));
    assertThat(
        f.getPools().get("/Common/pool3").getMembers().get("/Common/node3:80").getDescription(),
        equalTo("node3_is_ok"));
  }

  @Test
  public void testVipDescriptionExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_structured_vip_description");

    // check structure presence
    assertThat(vc.getNodes(), hasKeys("/Common/node1", "/Common/node2", "/Common/node3"));
    assertThat(vc.getPools(), hasKeys("/Common/pool1", "/Common/pool2", "/Common/pool3"));
    assertThat(
        vc.getVirtuals(), hasKeys("/Common/virtual1", "/Common/virtual2", "/Common/virtual3"));
    assertThat(
        vc.getVirtualAddresses(),
        hasKeys("/Common/192.0.2.1", "/Common/192.0.2.2", "/Common/192.0.2.3"));
    assertThat(vc.getPools().get("/Common/pool1").getMembers(), hasKeys("/Common/node1:80"));
    assertThat(vc.getPools().get("/Common/pool2").getMembers(), hasKeys("/Common/node2:80"));
    assertThat(vc.getPools().get("/Common/pool3").getMembers(), hasKeys("/Common/node3:80"));

    // check descriptions
    assertThat(vc.getPools().get("/Common/pool1").getDescription(), equalTo("pool1 is cool"));
    assertThat(vc.getPools().get("/Common/pool2").getDescription(), equalTo("pool2 is lame"));
    assertThat(vc.getPools().get("/Common/pool3").getDescription(), nullValue());

    assertThat(
        vc.getVirtuals().get("/Common/virtual1").getDescription(), equalTo("virtual1 is cool"));
    assertThat(vc.getVirtuals().get("/Common/virtual2").getDescription(), nullValue());
    assertThat(vc.getVirtuals().get("/Common/virtual3").getDescription(), nullValue());

    assertThat(
        vc.getPools().get("/Common/pool1").getMembers().get("/Common/node1:80").getDescription(),
        equalTo("node1_is_cool"));
    assertThat(
        vc.getPools().get("/Common/pool2").getMembers().get("/Common/node2:80").getDescription(),
        equalTo("node2_is_lame"));
    assertThat(
        vc.getPools().get("/Common/pool3").getMembers().get("/Common/node3:80").getDescription(),
        equalTo("node3_is_ok"));
  }

  @Test
  public void testVirtualAddressArpDisabledConversion() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_ltm_virtual_address_arp_disabled");

    IpSpace arpIps = c.getAllInterfaces().get("/Common/vlan1").getAdditionalArpIps();

    // disabled
    assertThat(arpIps, not(containsIp(Ip.parse("192.0.2.1"))));
    // enabled
    assertThat(arpIps, containsIp(Ip.parse("192.0.2.2")));
    // implicitly enabled
    assertThat(arpIps, containsIp(Ip.parse("192.0.2.3")));
  }

  @Test
  public void testVirtualAddressArpDisabledExtraction() {
    F5BigipConfiguration vc =
        parseVendorConfig("f5_bigip_structured_ltm_virtual_address_arp_disabled");
    String vaDisabledName = "/Common/192.0.2.1";
    String vaEnabledName = "/Common/192.0.2.2";
    String vaImplicitlyEnabledName = "/Common/192.0.2.3";

    assertThat(
        vc.getVirtualAddresses(), hasKeys(vaDisabledName, vaEnabledName, vaImplicitlyEnabledName));

    VirtualAddress vaDisabled = vc.getVirtualAddresses().get(vaDisabledName);
    VirtualAddress vaEnabled = vc.getVirtualAddresses().get(vaEnabledName);
    VirtualAddress vaImplicitlyEnabled = vc.getVirtualAddresses().get(vaImplicitlyEnabledName);

    assertTrue(vaDisabled.getArpDisabled());
    assertFalse(vaEnabled.getArpDisabled());
    assertThat(vaImplicitlyEnabled.getArpDisabled(), nullValue());
  }

  @Test
  public void testVirtualAddressIcmpEchoDisabledConversion() throws IOException {
    String hostname = "f5_bigip_structured_ltm_virtual_address_icmp_echo_disabled";
    Configuration c = parseConfig(hostname);
    String ifaceName = "/Common/vlan1";

    IpAccessList inFilter = c.getAllInterfaces().get(ifaceName).getIncomingFilter();

    // disabled
    assertThat(
        inFilter,
        IpAccessListMatchers.rejects(
            createIcmpFlow(hostname, Ip.parse("192.0.2.1")), ifaceName, c));
    // enabled
    assertThat(
        inFilter,
        IpAccessListMatchers.accepts(
            createIcmpFlow(hostname, Ip.parse("192.0.2.2")), ifaceName, c));
    // implicitly enabled
    assertThat(
        inFilter,
        IpAccessListMatchers.accepts(
            createIcmpFlow(hostname, Ip.parse("192.0.2.3")), ifaceName, c));
  }

  @Test
  public void testVirtualAddressIcmpEchoDisabledExtraction() {
    F5BigipConfiguration vc =
        parseVendorConfig("f5_bigip_structured_ltm_virtual_address_icmp_echo_disabled");
    String vaDisabledName = "/Common/192.0.2.1";
    String vaEnabledName = "/Common/192.0.2.2";
    String vaImplicitlyEnabledName = "/Common/192.0.2.3";

    assertThat(
        vc.getVirtualAddresses(), hasKeys(vaDisabledName, vaEnabledName, vaImplicitlyEnabledName));

    VirtualAddress vaDisabled = vc.getVirtualAddresses().get(vaDisabledName);
    VirtualAddress vaEnabled = vc.getVirtualAddresses().get(vaEnabledName);
    VirtualAddress vaImplicitlyEnabled = vc.getVirtualAddresses().get(vaImplicitlyEnabledName);

    assertThat(vaDisabled.getIcmpEchoDisabled(), equalTo(true));
    assertThat(vaEnabled.getIcmpEchoDisabled(), equalTo(false));
    assertThat(vaImplicitlyEnabled.getIcmpEchoDisabled(), nullValue());
  }

  @Test
  public void testVirtualAddressReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
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
  public void testVirtualDisabledConversion() throws IOException {
    String hostname = "f5_bigip_structured_ltm_virtual_disabled";
    Configuration c = parseConfig(hostname);

    Ip ipDisabled = Ip.parse("192.0.2.1");
    Ip ipEnabled = Ip.parse("192.0.2.2");
    Ip ipImplicitlyEnabled = Ip.parse("192.0.2.3");
    String vlanName = "/Common/vlan1";

    org.batfish.datamodel.Interface vlan1 = c.getAllInterfaces().get(vlanName);
    IpAccessList incomingFilter = vlan1.getIncomingFilter();
    IpSpace arpIps = vlan1.getAdditionalArpIps();

    assertFalse(
        "The incoming filter should not have specific handling for flows to a disabled virtual",
        matchesNonTrivially(incomingFilter, createHttpFlow(hostname, ipDisabled)));
    assertTrue(
        "The incoming filter should have specific handling for flows to an enabled virtual",
        matchesNonTrivially(incomingFilter, createHttpFlow(hostname, ipEnabled)));
    assertTrue(
        "The incoming filter should have specific handling for flows to an implicitly-enabled"
            + " virtual",
        matchesNonTrivially(incomingFilter, createHttpFlow(hostname, ipImplicitlyEnabled)));

    assertThat(arpIps, not(containsIp(ipDisabled)));
    assertThat(arpIps, containsIp(ipEnabled));
    assertThat(arpIps, containsIp(ipImplicitlyEnabled));
  }

  @Test
  public void testVirtualDisabledExtraction() {
    F5BigipConfiguration vc = parseVendorConfig("f5_bigip_structured_ltm_virtual_disabled");
    String vDisabledName = "/Common/virtual_disabled";
    String vEnabledName = "/Common/virtual_enabled";
    String vImplicitlyEnabledName = "/Common/virtual_implicitly_enabled";

    assertThat(vc.getVirtuals(), hasKeys(vDisabledName, vEnabledName, vImplicitlyEnabledName));

    assertTrue(vc.getVirtuals().get(vDisabledName).getDisabled());
    assertFalse(vc.getVirtuals().get(vEnabledName).getDisabled());
    assertThat(vc.getVirtuals().get(vImplicitlyEnabledName).getDisabled(), nullValue());
  }

  @Test
  public void testVirtualMatchesIpProtocol() throws IOException {
    String hostname = "f5_bigip_structured_ltm_virtual_ip_protocol";
    Configuration c = parseConfig(hostname);
    Transformation incomingTransformation =
        c.getAllInterfaces().get("/Common/vlan1").getIncomingTransformation();
    Flow.Builder builder =
        Flow.builder()
            .setDstIp(Ip.parse("192.0.2.1"))
            .setDstPort(80)
            .setIngressInterface("/Common/SOME_VLAN")
            .setIngressNode(hostname)
            .setSrcIp(Ip.parse("8.8.8.8"))
            .setSrcPort(50000);

    Flow matchingFlow = builder.setIpProtocol(IpProtocol.TCP).build();
    Flow nonMatchingFlow = builder.setIpProtocol(IpProtocol.UDP).build();

    assertTrue(
        "Flow with correct IpProtocol TCP is matched by incoming transformation",
        eval(incomingTransformation, matchingFlow, "dummy", ImmutableMap.of(), ImmutableMap.of())
            .getTraceSteps()
            .stream()
            .map(Step::getDetail)
            .filter(Predicates.instanceOf(TransformationStepDetail.class))
            .map(TransformationStepDetail.class::cast)
            .anyMatch(d -> d.getTransformationType() == TransformationType.DEST_NAT));
    assertFalse(
        "Flow with incorrect IpProtocol UDP is not matched by incoming transformation",
        eval(incomingTransformation, nonMatchingFlow, "dummy", ImmutableMap.of(), ImmutableMap.of())
            .getTraceSteps()
            .stream()
            .map(Step::getDetail)
            .filter(Predicates.instanceOf(TransformationStepDetail.class))
            .map(TransformationStepDetail.class::cast)
            .anyMatch(d -> d.getTransformationType() == TransformationType.DEST_NAT));
  }

  @Test
  public void testVirtualMisconfigurations() {
    F5BigipConfiguration c = parseVendorConfig("f5_bigip_structured_virtual_misconfigurations");
    Stream.of("ip_forward", "reject")
        .forEach(
            mode ->
                IntStream.of(1, 2, 3)
                    .forEach(
                        i -> {
                          Virtual virtual =
                              c.getVirtuals()
                                  .get(String.format("/Common/virtual_%s_translate%d", mode, i));
                          // translation should not occur regardless of whether directives appear
                          assertFalse(virtual.getTranslateAddress());
                          assertFalse(virtual.getTranslatePort());
                        }));
    IntStream.of(1, 2)
        .forEach(
            i -> {
              Virtual virtual =
                  c.getVirtuals()
                      .get(String.format("/Common/virtual_ip_forward_two_incompatible%d", i));
              // ip-forward first
              assertTrue(virtual.getIpForward());
              assertThat(virtual.getPool(), nullValue());
              assertFalse(virtual.getReject());
            });
    IntStream.of(1, 2)
        .forEach(
            i -> {
              Virtual virtual =
                  c.getVirtuals().get(String.format("/Common/virtual_pool_two_incompatible%d", i));
              // pool first
              assertFalse(virtual.getIpForward());
              assertThat(virtual.getPool(), notNullValue());
              assertFalse(virtual.getReject());
            });
    IntStream.of(1, 2)
        .forEach(
            i -> {
              Virtual virtual =
                  c.getVirtuals()
                      .get(String.format("/Common/virtual_reject_two_incompatible%d", i));
              // reject first
              assertFalse(virtual.getIpForward());
              assertThat(virtual.getPool(), nullValue());
              assertTrue(virtual.getReject());
            });
  }

  @Test
  public void testVirtualPoolTranslationOptions() throws IOException {
    // Test of pool-mode 'virtual' options:
    // - translate-address enabled/disabled: rewrite destination IP iff enabled
    // - translate-port enabled/disabled: rewrite destination port iff enabled
    String hostname = "f5_bigip_structured_virtual_pool_translation_options";

    Configuration c = parseConfig(hostname);

    // Assume a flow is received by /Common/vlan1
    Transformation incomingTransformation =
        c.getAllInterfaces().get("/Common/vlan1").getIncomingTransformation();
    Flow.Builder flowBuilder =
        Flow.builder()
            .setDstPort(80)
            .setIngressInterface("/Common/vlan1")
            .setIngressNode(hostname)
            .setIpProtocol(IpProtocol.TCP)
            .setSrcIp(Ip.parse("8.8.8.8"))
            .setSrcPort(50000);
    {
      // should match virtual /Common/virtual_translate_address
      Ip dstIp = Ip.parse("192.0.2.1");
      Flow flow = flowBuilder.setDstIp(dstIp).build();
      TransformationResult result =
          eval(incomingTransformation, flow, "dummy", ImmutableMap.of(), ImmutableMap.of());
      Optional<TransformationStepDetail> stepDetailOptional =
          result.getTraceSteps().stream()
              .map(Step::getDetail)
              .filter(Predicates.instanceOf(TransformationStepDetail.class))
              .map(TransformationStepDetail.class::cast)
              .filter(d -> d.getTransformationType() == TransformationType.DEST_NAT)
              .findFirst();

      assertTrue("There is a DNAT transformation step.", stepDetailOptional.isPresent());

      TransformationStepDetail detail = stepDetailOptional.get();

      // only destination IP should have been rewritten
      assertThat(
          detail.getFlowDiffs(),
          contains(isIpRewrite(IpField.DESTINATION, dstIp, Ip.parse("192.0.2.10"))));
    }

    {
      // should match virtual /Common/virtual_translate_both
      Ip dstIp = Ip.parse("192.0.2.2");
      Flow flow = flowBuilder.setDstIp(dstIp).build();
      TransformationResult result =
          eval(incomingTransformation, flow, "dummy", ImmutableMap.of(), ImmutableMap.of());
      Optional<TransformationStepDetail> stepDetailOptional =
          result.getTraceSteps().stream()
              .map(Step::getDetail)
              .filter(Predicates.instanceOf(TransformationStepDetail.class))
              .map(TransformationStepDetail.class::cast)
              .filter(d -> d.getTransformationType() == TransformationType.DEST_NAT)
              .findFirst();

      assertTrue("There is a DNAT transformation step.", stepDetailOptional.isPresent());

      TransformationStepDetail detail = stepDetailOptional.get();

      // both destination IP and destination port should have been rewritten
      assertThat(
          detail.getFlowDiffs(),
          containsInAnyOrder(
              isIpRewrite(IpField.DESTINATION, dstIp, Ip.parse("192.0.2.10")),
              isPortRewrite(PortField.DESTINATION, equalTo(80), equalTo(8080))));
    }

    {
      // should match virtual /Common/virtual_translate_neither
      Ip dstIp = Ip.parse("192.0.2.3");
      Flow flow = flowBuilder.setDstIp(dstIp).build();
      TransformationResult result =
          eval(incomingTransformation, flow, "dummy", ImmutableMap.of(), ImmutableMap.of());
      Optional<TransformationStepDetail> stepDetailOptional =
          result.getTraceSteps().stream()
              .map(Step::getDetail)
              .filter(Predicates.instanceOf(TransformationStepDetail.class))
              .map(TransformationStepDetail.class::cast)
              .filter(d -> d.getTransformationType() == TransformationType.DEST_NAT)
              .findFirst();

      assertTrue("There is a DNAT transformation step.", stepDetailOptional.isPresent());

      TransformationStepDetail detail = stepDetailOptional.get();

      // neither destination IP nor destination port should have been rewritten
      assertThat(detail.getFlowDiffs(), empty());
    }

    {
      // should match virtual /Common/virtual_translate_port
      Ip dstIp = Ip.parse("192.0.2.4");
      Flow flow = flowBuilder.setDstIp(dstIp).build();
      TransformationResult result =
          eval(incomingTransformation, flow, "dummy", ImmutableMap.of(), ImmutableMap.of());
      Optional<TransformationStepDetail> stepDetailOptional =
          result.getTraceSteps().stream()
              .map(Step::getDetail)
              .filter(Predicates.instanceOf(TransformationStepDetail.class))
              .map(TransformationStepDetail.class::cast)
              .filter(d -> d.getTransformationType() == TransformationType.DEST_NAT)
              .findFirst();

      assertTrue("There is a DNAT transformation step.", stepDetailOptional.isPresent());

      TransformationStepDetail detail = stepDetailOptional.get();

      // only destination port should have been rewritten
      assertThat(
          detail.getFlowDiffs(),
          contains(isPortRewrite(PortField.DESTINATION, equalTo(80), equalTo(8080))));
    }
  }

  @Test
  public void testVirtualReferences() throws IOException {
    String hostname = "f5_bigip_structured_ltm_references";
    String file = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    String used = "/Common/virtual_used";

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, VIRTUAL, used, 1));
  }

  @Test
  public void testVirtualRejectFilter() throws IOException {
    // Test that:
    // - Traffic matching a 'virtual' in 'ip-forward' is not filtered at ingress
    // - Traffic matching a 'virtual' in 'reject' mode is filtered at ingress
    String hostname = "f5_bigip_structured_virtual_reject";
    String ifaceName = "/Common/vlan1";

    Configuration c = parseConfig(hostname);
    IpAccessList incomingFilter = c.getAllInterfaces().get(ifaceName).getIncomingFilter();

    assertThat("Incoming filter assigned to interface", incomingFilter, notNullValue());

    String expectedName = F5BigipConfiguration.computeInterfaceIncomingFilterName(ifaceName);

    assertThat(
        "Incoming filter has expected name",
        incomingFilter,
        IpAccessListMatchers.hasName(expectedName));

    Flow.Builder builder =
        Flow.builder()
            .setDstPort(80)
            .setIngressInterface("/Common/vlan1")
            .setIngressNode(hostname)
            .setIpProtocol(IpProtocol.TCP)
            .setSrcIp(Ip.parse("8.8.8.8"))
            .setSrcPort(50000);

    {
      // Forwarded traffic
      Flow flow = builder.setDstIp(Ip.parse("192.0.2.1")).build();

      assertThat(
          "Filter does not reject traffic to forwarding virtual 'virtual_forward'",
          incomingFilter,
          accepts(flow, "dummy", c));
    }

    {
      // Rejected traffic
      Flow flow = builder.setDstIp(Ip.parse("192.0.2.2")).build();

      assertThat(
          "Filter rejects traffic to rejecting virtual 'virtual_reject'",
          incomingFilter,
          rejects(flow, "dummy", c));
    }
  }

  @Test
  public void testVlan() throws IOException {
    Configuration c = parseConfig("f5_bigip_structured_vlan");
    String portName = "1.0";
    String trunkName = "trunk1";
    String vlanName = "/Common/MYVLAN";

    assertThat(
        c.getAllInterfaces().keySet(),
        containsInAnyOrder(portName, trunkName, "2.0", "3.0", vlanName));

    // port interface
    assertThat(c, hasInterface(portName, isActive()));
    assertThat(c, hasInterface(portName, isSwitchport()));
    assertThat(c, hasInterface(portName, hasSwitchPortMode(SwitchportMode.TRUNK)));
    assertThat(c, hasInterface(portName, hasAllowedVlans(IntegerSpace.of(123))));
    assertThat(c, hasInterface(portName, hasNativeVlan(nullValue())));

    // trunk interface
    assertThat(c, hasInterface(trunkName, isActive()));
    assertThat(c, hasInterface(trunkName, isSwitchport()));
    assertThat(c, hasInterface(trunkName, hasSwitchPortMode(SwitchportMode.TRUNK)));
    assertThat(c, hasInterface(trunkName, hasAllowedVlans(IntegerSpace.of(123))));
    assertThat(c, hasInterface(trunkName, hasNativeVlan(nullValue())));

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // detect undefined reference
    assertThat(ans, hasUndefinedReference(file, VLAN, undefined));

    // detected unused structure
    assertThat(ans, hasNumReferrers(file, VLAN, unused, 0));

    // detect all structure references
    assertThat(ans, hasNumReferrers(file, VLAN, used, 3));
  }
}

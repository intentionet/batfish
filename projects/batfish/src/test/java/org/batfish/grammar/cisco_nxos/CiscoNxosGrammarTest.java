package org.batfish.grammar.cisco_nxos;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.datamodel.IpWildcard.ipWithWildcardMask;
import static org.batfish.datamodel.Route.UNSET_NEXT_HOP_INTERFACE;
import static org.batfish.datamodel.VniSettings.DEFAULT_UDP_PORT;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDscp;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDstPort;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchFragmentOffset;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchIcmp;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchIcmpType;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchIpProtocol;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchPacketLength;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrc;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcPort;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchTcpFlags;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasAdministrativeCost;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopInterface;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopIp;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasAllowLocalAsIn;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasSendCommunity;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasSendExtendedCommunity;
import static org.batfish.datamodel.matchers.AddressFamilyMatchers.hasAddressFamilyCapabilites;
import static org.batfish.datamodel.matchers.AddressFamilyMatchers.hasExportPolicy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.HsrpGroupMatchers.hasHelloTime;
import static org.batfish.datamodel.matchers.HsrpGroupMatchers.hasHoldTime;
import static org.batfish.datamodel.matchers.HsrpGroupMatchers.hasPreempt;
import static org.batfish.datamodel.matchers.HsrpGroupMatchers.hasPriority;
import static org.batfish.datamodel.matchers.HsrpGroupMatchers.hasTrackActions;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAddress;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasBandwidth;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasChannelGroup;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasChannelGroupMembers;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDependencies;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDescription;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasHsrpGroup;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasHsrpVersion;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasInterfaceType;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSpeed;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isAutoState;
import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasSuppressType7;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasNssa;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasStub;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasArea;
import static org.batfish.datamodel.matchers.StaticRouteMatchers.hasTag;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasBumTransportIps;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasBumTransportMethod;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasSourceAddress;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasUdpPort;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasVni;
import static org.batfish.datamodel.matchers.VrfMatchers.hasVniSettings;
import static org.batfish.grammar.cisco_nxos.CiscoNxosCombinedParser.DEBUG_FLAG_USE_NEW_CISCO_NXOS_PARSER;
import static org.batfish.grammar.cisco_nxos.CiscoNxosControlPlaneExtractor.PACKET_LENGTH_RANGE;
import static org.batfish.grammar.cisco_nxos.CiscoNxosControlPlaneExtractor.TCP_PORT_RANGE;
import static org.batfish.grammar.cisco_nxos.CiscoNxosControlPlaneExtractor.UDP_PORT_RANGE;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.NULL_VRF_NAME;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.toJavaRegex;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.OBJECT_GROUP_IP_ADDRESS;
import static org.batfish.representation.cisco_nxos.OspfInterface.DEFAULT_DEAD_INTERVAL_S;
import static org.batfish.representation.cisco_nxos.OspfInterface.DEFAULT_HELLO_INTERVAL_S;
import static org.batfish.representation.cisco_nxos.OspfMaxMetricRouterLsa.DEFAULT_OSPF_MAX_METRIC;
import static org.batfish.representation.cisco_nxos.OspfNetworkType.BROADCAST;
import static org.batfish.representation.cisco_nxos.OspfNetworkType.POINT_TO_POINT;
import static org.batfish.representation.cisco_nxos.OspfProcess.DEFAULT_AUTO_COST_REFERENCE_BANDWIDTH_MBPS;
import static org.batfish.representation.cisco_nxos.OspfProcess.DEFAULT_TIMERS_LSA_ARRIVAL_MS;
import static org.batfish.representation.cisco_nxos.OspfProcess.DEFAULT_TIMERS_LSA_GROUP_PACING_S;
import static org.batfish.representation.cisco_nxos.OspfProcess.DEFAULT_TIMERS_THROTTLE_LSA_HOLD_INTERVAL_MS;
import static org.batfish.representation.cisco_nxos.OspfProcess.DEFAULT_TIMERS_THROTTLE_LSA_MAX_INTERVAL_MS;
import static org.batfish.representation.cisco_nxos.OspfProcess.DEFAULT_TIMERS_THROTTLE_LSA_START_INTERVAL_MS;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.bdd.HeaderSpaceToBDD;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.BddTestbed;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.DscpType;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IcmpCode;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.bgp.EvpnAddressFamily;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Layer2VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.matchers.HsrpGroupMatchers;
import org.batfish.datamodel.matchers.NssaSettingsMatchers;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.datamodel.matchers.RouteFilterListMatchers;
import org.batfish.datamodel.matchers.StubSettingsMatchers;
import org.batfish.datamodel.matchers.VniSettingsMatchers;
import org.batfish.datamodel.matchers.VrfMatchers;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityConjunction;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.tracking.DecrementPriority;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.ParserBatfishException;
import org.batfish.representation.cisco_nxos.ActionIpAccessListLine;
import org.batfish.representation.cisco_nxos.AddrGroupIpAddressSpec;
import org.batfish.representation.cisco_nxos.AddressFamily;
import org.batfish.representation.cisco_nxos.BgpGlobalConfiguration;
import org.batfish.representation.cisco_nxos.BgpRedistributionPolicy;
import org.batfish.representation.cisco_nxos.BgpVrfConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfIpv4AddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfL2VpnEvpnAddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfL2VpnEvpnAddressFamilyConfiguration.RetainRouteType;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType;
import org.batfish.representation.cisco_nxos.CiscoNxosStructureType;
import org.batfish.representation.cisco_nxos.DefaultVrfOspfProcess;
import org.batfish.representation.cisco_nxos.Evpn;
import org.batfish.representation.cisco_nxos.EvpnVni;
import org.batfish.representation.cisco_nxos.ExtendedCommunityOrAuto;
import org.batfish.representation.cisco_nxos.FragmentsBehavior;
import org.batfish.representation.cisco_nxos.HsrpGroup;
import org.batfish.representation.cisco_nxos.IcmpOptions;
import org.batfish.representation.cisco_nxos.Interface;
import org.batfish.representation.cisco_nxos.InterfaceAddressWithAttributes;
import org.batfish.representation.cisco_nxos.InterfaceHsrp;
import org.batfish.representation.cisco_nxos.IpAccessList;
import org.batfish.representation.cisco_nxos.IpAccessListLine;
import org.batfish.representation.cisco_nxos.IpAddressSpec;
import org.batfish.representation.cisco_nxos.IpAsPathAccessList;
import org.batfish.representation.cisco_nxos.IpAsPathAccessListLine;
import org.batfish.representation.cisco_nxos.IpCommunityListStandard;
import org.batfish.representation.cisco_nxos.IpCommunityListStandardLine;
import org.batfish.representation.cisco_nxos.IpPrefixList;
import org.batfish.representation.cisco_nxos.IpPrefixListLine;
import org.batfish.representation.cisco_nxos.Lacp;
import org.batfish.representation.cisco_nxos.Layer3Options;
import org.batfish.representation.cisco_nxos.LiteralIpAddressSpec;
import org.batfish.representation.cisco_nxos.LiteralPortSpec;
import org.batfish.representation.cisco_nxos.Nve;
import org.batfish.representation.cisco_nxos.Nve.HostReachabilityProtocol;
import org.batfish.representation.cisco_nxos.Nve.IngressReplicationProtocol;
import org.batfish.representation.cisco_nxos.NveVni;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddress;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddressLine;
import org.batfish.representation.cisco_nxos.OspfArea;
import org.batfish.representation.cisco_nxos.OspfAreaAuthentication;
import org.batfish.representation.cisco_nxos.OspfAreaNssa;
import org.batfish.representation.cisco_nxos.OspfAreaRange;
import org.batfish.representation.cisco_nxos.OspfAreaStub;
import org.batfish.representation.cisco_nxos.OspfDefaultOriginate;
import org.batfish.representation.cisco_nxos.OspfInterface;
import org.batfish.representation.cisco_nxos.OspfMaxMetricRouterLsa;
import org.batfish.representation.cisco_nxos.OspfProcess;
import org.batfish.representation.cisco_nxos.OspfSummaryAddress;
import org.batfish.representation.cisco_nxos.PortGroupPortSpec;
import org.batfish.representation.cisco_nxos.PortSpec;
import org.batfish.representation.cisco_nxos.RouteDistinguisherOrAuto;
import org.batfish.representation.cisco_nxos.RouteMap;
import org.batfish.representation.cisco_nxos.RouteMapEntry;
import org.batfish.representation.cisco_nxos.RouteMapMatchAsPath;
import org.batfish.representation.cisco_nxos.RouteMapMatchCommunity;
import org.batfish.representation.cisco_nxos.RouteMapMatchInterface;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpAddress;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpAddressPrefixList;
import org.batfish.representation.cisco_nxos.RouteMapMatchMetric;
import org.batfish.representation.cisco_nxos.RouteMapMatchTag;
import org.batfish.representation.cisco_nxos.RouteMapMetricType;
import org.batfish.representation.cisco_nxos.RouteMapSetAsPathPrependLastAs;
import org.batfish.representation.cisco_nxos.RouteMapSetAsPathPrependLiteralAs;
import org.batfish.representation.cisco_nxos.RouteMapSetCommunity;
import org.batfish.representation.cisco_nxos.RouteMapSetIpNextHopLiteral;
import org.batfish.representation.cisco_nxos.RouteMapSetIpNextHopUnchanged;
import org.batfish.representation.cisco_nxos.RouteMapSetLocalPreference;
import org.batfish.representation.cisco_nxos.RouteMapSetMetric;
import org.batfish.representation.cisco_nxos.RouteMapSetMetricType;
import org.batfish.representation.cisco_nxos.RouteMapSetOrigin;
import org.batfish.representation.cisco_nxos.RouteMapSetTag;
import org.batfish.representation.cisco_nxos.StaticRoute;
import org.batfish.representation.cisco_nxos.TcpOptions;
import org.batfish.representation.cisco_nxos.UdpOptions;
import org.batfish.representation.cisco_nxos.Vlan;
import org.batfish.representation.cisco_nxos.Vrf;
import org.batfish.representation.cisco_nxos.VrfAddressFamily;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@ParametersAreNonnullByDefault
public final class CiscoNxosGrammarTest {

  private static final BddTestbed BDD_TESTBED =
      new BddTestbed(ImmutableMap.of(), ImmutableMap.of());
  private static final IpAccessListToBdd ACL_TO_BDD;
  private static final IpSpaceToBDD DST_IP_BDD;
  private static final HeaderSpaceToBDD HS_TO_BDD;
  private static final IpSpaceToBDD SRC_IP_BDD;

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco_nxos/testconfigs/";

  static {
    DST_IP_BDD = BDD_TESTBED.getDstIpBdd();
    SRC_IP_BDD = BDD_TESTBED.getSrcIpBdd();
    HS_TO_BDD = BDD_TESTBED.getHsToBdd();
    ACL_TO_BDD = BDD_TESTBED.getAclToBdd();
  }

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static @Nonnull OspfExternalRoute.Builder ospfExternalRouteBuilder() {
    return OspfExternalRoute.builder()
        .setAdvertiser("dummy")
        .setArea(0L)
        .setCostToAdvertiser(0L)
        .setLsaMetric(0L);
  }

  private static @Nonnull BDD toBDD(AclLineMatchExpr aclLineMatchExpr) {
    return ACL_TO_BDD.toBdd(aclLineMatchExpr);
  }

  private static @Nonnull BDD toBDD(HeaderSpace headerSpace) {
    return HS_TO_BDD.toBDD(headerSpace);
  }

  private static @Nonnull BDD toIcmpIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toIfBDD(
        AclLineMatchExprs.and(
            matchFragmentOffset(0), matchIpProtocol(IpProtocol.ICMP), aclLineMatchExpr));
  }

  private static @Nonnull BDD toIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toBDD(AclLineMatchExprs.and(matchFragmentOffset(0), aclLineMatchExpr));
  }

  private static @Nonnull BDD toNonIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toBDD(
        AclLineMatchExprs.and(
            matchFragmentOffset(IntegerSpace.of(Range.closed(1, 8191))), aclLineMatchExpr));
  }

  private static @Nonnull BDD toTcpIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toIfBDD(
        AclLineMatchExprs.and(
            matchFragmentOffset(0), matchIpProtocol(IpProtocol.TCP), aclLineMatchExpr));
  }

  private static @Nonnull BDD toUdpIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toIfBDD(
        AclLineMatchExprs.and(
            matchFragmentOffset(0), matchIpProtocol(IpProtocol.UDP), aclLineMatchExpr));
  }

  private @Nonnull Batfish getBatfishForConfigurationNames(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    Batfish batfish = BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
    batfish.getSettings().setDebugFlags(ImmutableList.of(DEBUG_FLAG_USE_NEW_CISCO_NXOS_PARSER));
    return batfish;
  }

  private @Nonnull Configuration parseConfig(String hostname) throws IOException {
    Map<String, Configuration> configs = parseTextConfigs(hostname);
    String canonicalHostname = hostname.toLowerCase();
    assertThat(configs, hasEntry(equalTo(canonicalHostname), hasHostname(canonicalHostname)));
    return configs.get(canonicalHostname);
  }

  private @Nonnull Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    return getBatfishForConfigurationNames(configurationNames).loadConfigurations();
  }

  private @Nonnull CiscoNxosConfiguration parseVendorConfig(String hostname) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    settings.setDebugFlags(ImmutableList.of(DEBUG_FLAG_USE_NEW_CISCO_NXOS_PARSER));
    CiscoNxosCombinedParser ciscoNxosParser = new CiscoNxosCombinedParser(src, settings);
    CiscoNxosControlPlaneExtractor extractor =
        new CiscoNxosControlPlaneExtractor(src, ciscoNxosParser, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(
            ciscoNxosParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(tree);
    CiscoNxosConfiguration vendorConfiguration =
        (CiscoNxosConfiguration) extractor.getVendorConfiguration();
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + hostname);
    // crash if not serializable
    SerializationUtils.clone(vendorConfiguration);
    return vendorConfiguration;
  }

  private void assertRoutingPolicyDeniesRoute(RoutingPolicy routingPolicy, AbstractRoute route) {
    assertFalse(
        routingPolicy.process(
            route,
            Bgpv4Route.builder().setNetwork(route.getNetwork()),
            Ip.parse("192.0.2.1"),
            DEFAULT_VRF_NAME,
            Direction.OUT));
  }

  private void assertRoutingPolicyPermitsRoute(RoutingPolicy routingPolicy, AbstractRoute route) {
    assertTrue(
        routingPolicy.process(
            route,
            Bgpv4Route.builder().setNetwork(route.getNetwork()),
            Ip.parse("192.0.2.1"),
            DEFAULT_VRF_NAME,
            Direction.OUT));
  }

  private @Nonnull Bgpv4Route processRouteIn(RoutingPolicy routingPolicy, Bgpv4Route route) {
    Bgpv4Route.Builder builder = route.toBuilder();
    routingPolicy.process(route, builder, Ip.parse("192.0.2.1"), DEFAULT_VRF_NAME, Direction.IN);
    return builder.build();
  }

  private @Nonnull OspfExternalRoute processRouteRedistributeOspf(
      RoutingPolicy routingPolicy, Bgpv4Route route) {
    OspfExternalRoute.Builder builder =
        OspfExternalRoute.builder()
            .setNetwork(route.getNetwork())
            .setLsaMetric(123L)
            .setArea(456L)
            .setCostToAdvertiser(789L)
            .setAdvertiser("n1");
    routingPolicy.process(route, builder, Ip.parse("192.0.2.1"), DEFAULT_VRF_NAME, Direction.OUT);
    return builder.build();
  }

  @Test
  public void testAaaParsing() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_aaa"), notNullValue());
  }

  @Test
  public void testBannerExtraction() {
    String bannerHostname = "nxos_banner";
    String bannerEmptyHostname = "nxos_banner_empty";
    CiscoNxosConfiguration banner = parseVendorConfig(bannerHostname);
    CiscoNxosConfiguration bannerEmpty = parseVendorConfig(bannerEmptyHostname);

    assertThat(banner.getBannerExec(), equalTo("multi\nline"));
    assertThat(banner.getBannerMotd(), equalTo("oneline"));
    assertThat(bannerEmpty.getBannerExec(), emptyString());
  }

  /**
   * Temporary parsing test of port of unified Cisco NX-OS BGP grammar. The test file should be
   * moved to parsing ref tests once bit is flipped on new parser.
   */
  @Test
  public void testBgpParsingTemporary() {
    String hostname = "nxos_bgp_parsing_temporary";

    // Just test that parser does not choke.
    assertThat(parseVendorConfig(hostname), notNullValue());
  }

  @Test
  public void testClassMapParsing() {
    // TODO: make into an extraction test
    assertThat(parseVendorConfig("nxos_class_map"), notNullValue());
  }

  @Test
  public void testBgpExtraction() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_bgp");
    BgpGlobalConfiguration bgpGlobal = vc.getBgpGlobalConfiguration();
    assertThat(bgpGlobal, notNullValue());
    assertThat(bgpGlobal.getLocalAs(), equalTo(1L));

    assertThat(bgpGlobal.getVrfs(), hasKeys(DEFAULT_VRF_NAME));
    {
      BgpVrfConfiguration vrf = bgpGlobal.getOrCreateVrf(DEFAULT_VRF_NAME);

      BgpVrfIpv4AddressFamilyConfiguration ipv4u = vrf.getIpv4UnicastAddressFamily();
      assertThat(ipv4u, notNullValue());
      assertThat(
          ipv4u.getRedistributionPolicy(RoutingProtocol.CONNECTED),
          equalTo(new BgpRedistributionPolicy("DIR_MAP", null)));
      assertThat(
          ipv4u.getRedistributionPolicy(RoutingProtocol.OSPF),
          equalTo(new BgpRedistributionPolicy("OSPF_MAP", "ospf_proc")));

      BgpVrfL2VpnEvpnAddressFamilyConfiguration l2vpn = vrf.getL2VpnEvpnAddressFamily();
      assertThat(l2vpn, notNullValue());
      assertThat(l2vpn.getRetainMode(), equalTo(RetainRouteType.ROUTE_MAP));
      assertThat(l2vpn.getRetainRouteMap(), equalTo("RETAIN_MAP"));
    }
  }

  /** Like {@link #testBgpExtraction()}, but for second variants of global parameters. */
  @Test
  public void testBgpExtraction2() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_bgp_2");

    BgpGlobalConfiguration bgpGlobal = vc.getBgpGlobalConfiguration();
    assertThat(bgpGlobal, notNullValue());
    assertThat(bgpGlobal.getVrfs(), hasKeys(DEFAULT_VRF_NAME));
    {
      BgpVrfConfiguration vrf = bgpGlobal.getOrCreateVrf(DEFAULT_VRF_NAME);

      BgpVrfL2VpnEvpnAddressFamilyConfiguration l2vpn = vrf.getL2VpnEvpnAddressFamily();
      assertThat(l2vpn, notNullValue());
      assertThat(l2vpn.getRetainMode(), equalTo(RetainRouteType.ALL));
    }
  }

  @Test
  public void testSwitchnameConversion() throws IOException {
    String hostname = "nxos_switchname";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasHostname(hostname));
  }

  @Test
  public void testSwitchnameExtraction() {
    String hostname = "nxos_switchname";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getHostname(), equalTo(hostname));
  }

  @Test
  public void testTemplatePeerBgpAddressFamilyConversion() throws IOException {
    Configuration c = parseConfig("nxos_bgp_peer_template_af_inheritance");

    BgpActivePeerConfig peer =
        Iterables.getOnlyElement(c.getDefaultVrf().getBgpProcess().getActiveNeighbors().values());
    assertThat(
        peer.getGeneratedRoutes(),
        equalTo(
            ImmutableSet.of(
                GeneratedRoute.builder().setNetwork(Prefix.ZERO).setAdmin(32767).build())));

    Ipv4UnicastAddressFamily ipv4Af = peer.getIpv4UnicastAddressFamily();
    assertThat(ipv4Af, notNullValue());
    assertThat(
        ipv4Af,
        hasAddressFamilyCapabilites(allOf(hasSendCommunity(true), hasAllowLocalAsIn(true))));

    String commonBgpExportPolicy = "~BGP_COMMON_EXPORT_POLICY:default~";
    String defaultRouteOriginatePolicy = "~BGP_DEFAULT_ROUTE_PEER_EXPORT_POLICY:IPv4~";
    String peerExportPolicyName = "~BGP_PEER_EXPORT_POLICY:default:1.1.1.1~";

    assertThat(ipv4Af, hasExportPolicy(peerExportPolicyName));
    assertThat(
        c.getRoutingPolicies().get(peerExportPolicyName).getStatements(),
        equalTo(
            ImmutableList.of(
                new If(
                    new CallExpr(defaultRouteOriginatePolicy),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement())),
                new If(
                    new Conjunction(
                        ImmutableList.of(
                            new CallExpr(commonBgpExportPolicy), new CallExpr("match_metric"))),
                    ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                    ImmutableList.of(Statements.ExitReject.toStaticStatement())))));
  }

  @Test
  public void testTemplatePeerEvpnAddressFamilyConversion() throws IOException {
    Configuration c = parseConfig("nxos_bgp_peer_template_af_inheritance");

    BgpActivePeerConfig peer =
        Iterables.getOnlyElement(c.getDefaultVrf().getBgpProcess().getActiveNeighbors().values());

    EvpnAddressFamily evpnAf = peer.getEvpnAddressFamily();
    assertThat(evpnAf, notNullValue());
    assertThat(
        evpnAf,
        hasAddressFamilyCapabilites(
            allOf(
                hasSendCommunity(true), hasSendExtendedCommunity(true), hasAllowLocalAsIn(true))));
    assertTrue(evpnAf.getPropagateUnmatched());

    String peerEvpnExportPolicyName = "~BGP_PEER_EXPORT_POLICY_EVPN:default:1.1.1.1~";
    assertThat(evpnAf, hasExportPolicy(peerEvpnExportPolicyName));
    assertThat(
        c.getRoutingPolicies().get(peerEvpnExportPolicyName).getStatements(),
        equalTo(
            ImmutableList.of(
                new If(
                    new Conjunction(
                        ImmutableList.of(
                            new MatchProtocol(RoutingProtocol.BGP, RoutingProtocol.IBGP),
                            new CallExpr("match_metric"))),
                    ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                    ImmutableList.of(Statements.ExitReject.toStaticStatement())))));
  }

  @Test
  public void testEvpnL2L3Vni() throws IOException {
    Configuration c = parseConfig("nxos_l2_l3_vnis");

    Ip routerId = Ip.parse("10.1.1.1");
    // All defined VXLAN Vnis
    ImmutableSortedSet<Layer2VniConfig> expectedL2Vnis =
        ImmutableSortedSet.of(
            Layer2VniConfig.builder()
                .setVni(1111)
                .setVrf(DEFAULT_VRF_NAME)
                .setRouteDistinguisher(RouteDistinguisher.from(routerId, 1111))
                .setRouteTarget(ExtendedCommunity.target(1, 1111))
                .setImportRouteTarget(ExtendedCommunity.target(1, 1111).matchString())
                .build());
    ImmutableSortedSet<Layer3VniConfig> expectedL3Vnis =
        ImmutableSortedSet.of(
            Layer3VniConfig.builder()
                .setVni(3333)
                .setVrf(DEFAULT_VRF_NAME)
                .setRouteDistinguisher(RouteDistinguisher.from(routerId, 3333))
                .setRouteTarget(ExtendedCommunity.target(1, 3333))
                .setImportRouteTarget(ExtendedCommunity.target(1, 3333).matchString())
                .setAdvertiseV4Unicast(false)
                .build());
    BgpPeerConfig peer =
        c.getDefaultVrf().getBgpProcess().getActiveNeighbors().get(Prefix.parse("1.1.1.1/32"));
    assertThat(peer.getEvpnAddressFamily(), notNullValue());
    assertThat(peer.getEvpnAddressFamily().getL2VNIs(), equalTo(expectedL2Vnis));
    assertThat(peer.getEvpnAddressFamily().getL3VNIs(), equalTo(expectedL3Vnis));
  }

  @Test
  public void testTrackExtraction() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_track"), notNullValue());
  }

  @Test
  public void testEvpnExtraction() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_evpn");
    Evpn evpn = vc.getEvpn();
    assertThat(evpn, not(nullValue()));

    assertThat(evpn.getVnis(), hasKeys(1, 2, 3));
    {
      EvpnVni vni = evpn.getVni(1);
      assertThat(vni.getRd(), equalTo(RouteDistinguisherOrAuto.auto()));
      assertThat(vni.getExportRt(), equalTo(ExtendedCommunityOrAuto.auto()));
      assertThat(vni.getImportRt(), equalTo(ExtendedCommunityOrAuto.auto()));
    }
    {
      EvpnVni vni = evpn.getVni(2);
      assertThat(vni.getRd(), nullValue());
      assertThat(
          vni.getExportRt(),
          equalTo(ExtendedCommunityOrAuto.of(ExtendedCommunity.target(65002L, 1L))));
      assertThat(
          vni.getImportRt(),
          equalTo(ExtendedCommunityOrAuto.of(ExtendedCommunity.target(65002L, 2L))));
    }
    {
      EvpnVni vni = evpn.getVni(3);
      assertThat(
          vni.getRd(),
          equalTo(RouteDistinguisherOrAuto.of(RouteDistinguisher.from(Ip.parse("3.3.3.3"), 0))));
      assertThat(
          vni.getExportRt(),
          equalTo(ExtendedCommunityOrAuto.of(ExtendedCommunity.target(65003L, 2L))));
      assertThat(
          vni.getImportRt(),
          equalTo(ExtendedCommunityOrAuto.of(ExtendedCommunity.target(65003L, 1L))));
    }
  }

  @Test
  public void testFeatureParsing() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_feature"), notNullValue());
  }

  @Test
  public void testHostnameConversion() throws IOException {
    String hostname = "nxos_hostname";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasHostname(hostname));
  }

  @Test
  public void testHostnameExtraction() {
    String hostname = "nxos_hostname";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getHostname(), equalTo(hostname));
  }

  @Test
  public void testIgnore() {
    // TODO: make into a ref test
    assertThat(parseVendorConfig("nxos_ignore"), notNullValue());
  }

  @Test
  public void testInterfaceBindDependency() throws IOException {
    String hostname = "nxos_interface_bind_dependency";
    String ifaceName = "Ethernet1/1";
    String subName = "Ethernet1/1.1";
    Configuration c = parseConfig(hostname);

    // parent should have no dependencies
    assertThat(c, hasInterface(ifaceName, hasDependencies(empty())));
    // subinterface should be bound to parent
    assertThat(
        c,
        hasInterface(
            subName, hasDependencies(contains(new Dependency(ifaceName, DependencyType.BIND)))));
  }

  @Test
  public void testInterfaceHsrpConversion() throws IOException {
    String hostname = "nxos_interface_hsrp";
    String ifaceName = "Ethernet1/1";
    Configuration c = parseConfig(hostname);

    assertThat(c.getAllInterfaces(), hasKeys(ifaceName));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
      assertThat(iface, hasHsrpVersion("2"));
      assertThat(
          iface,
          hasHsrpGroup(
              2,
              allOf(
                  hasHelloTime(250),
                  hasHoldTime(750),
                  HsrpGroupMatchers.hasIp(Ip.parse("192.0.2.1")),
                  hasPreempt(),
                  hasPriority(105),
                  hasTrackActions(
                      equalTo(
                          ImmutableSortedMap.of(
                              "1", new DecrementPriority(10), "2", new DecrementPriority(20)))))));
    }
  }

  @Test
  public void testInterfaceHsrpExtraction() {
    String hostname = "nxos_interface_hsrp";
    String ifaceName = "Ethernet1/1";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getInterfaces(), hasKeys(ifaceName));
    {
      Interface iface = vc.getInterfaces().get(ifaceName);
      InterfaceHsrp hsrp = iface.getHsrp();
      assertThat(hsrp, notNullValue());
      assertThat(hsrp.getDelayReloadSeconds(), equalTo(60));
      assertThat(hsrp.getVersion(), equalTo(2));
      assertThat(hsrp.getGroups(), hasKeys(2));
      HsrpGroup group = hsrp.getGroups().get(2);
      assertThat(group.getIp(), equalTo(Ip.parse("192.0.2.1")));
      assertThat(group.getPreemptDelayMinimumSeconds(), equalTo(30));
      assertThat(group.getPreemptDelayReloadSeconds(), equalTo(40));
      assertThat(group.getPreemptDelaySyncSeconds(), equalTo(50));
      assertThat(group.getPriority(), equalTo(105));
      assertThat(group.getHelloIntervalMs(), equalTo(250));
      assertThat(group.getHoldTimeMs(), equalTo(750));
      assertThat(group.getTracks(), hasKeys(1, 2));
      assertThat(group.getTracks().get(1).getDecrement(), equalTo(10));
      assertThat(group.getTracks().get(2).getDecrement(), equalTo(20));
    }
  }

  @Test
  public void testInterfaceIpAddressConversion() throws IOException {
    String hostname = "nxos_interface_ip_address";
    String ifaceName = "Ethernet1/1";
    Configuration c = parseConfig(hostname);

    assertThat(
        c,
        hasInterface(
            ifaceName,
            allOf(
                hasAddress(ConcreteInterfaceAddress.parse("10.0.0.1/24")),
                hasAllAddresses(
                    containsInAnyOrder(
                        ConcreteInterfaceAddress.parse("10.0.0.1/24"),
                        ConcreteInterfaceAddress.parse("10.0.0.2/24"),
                        ConcreteInterfaceAddress.parse("10.0.0.3/24"))))));
  }

  @Test
  public void testInterfaceIpAddressExtraction() {
    String hostname = "nxos_interface_ip_address";
    String ifaceName = "Ethernet1/1";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getInterfaces(), hasKey(ifaceName));

    Interface iface = vc.getInterfaces().get(ifaceName);
    InterfaceAddressWithAttributes primary =
        new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.0.0.1/24"));
    InterfaceAddressWithAttributes secondary2 =
        new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.0.0.2/24"));
    InterfaceAddressWithAttributes secondary3 =
        new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.0.0.3/24"));
    secondary3.setTag(3L);

    assertThat(iface.getAddress(), equalTo(primary));
    assertThat(iface.getSecondaryAddresses(), containsInAnyOrder(secondary2, secondary3));
  }

  @Test
  public void testInterfaceMulticastParsing() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_interface_multicast"), notNullValue());
  }

  @Test
  public void testInterfacePbrExtraction() {
    CiscoNxosConfiguration c = parseVendorConfig("nxos_interface_ip_policy");
    assertThat(c.getInterfaces().get("Ethernet1/1").getPbrPolicy(), equalTo("PBR_POLICY"));
  }

  @Test
  public void testInterfaceRangeConversion() throws IOException {
    String hostname = "nxos_interface_range";
    Configuration c = parseConfig(hostname);

    assertThat(
        c, hasInterfaces(hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/1.1", "Ethernet1/1.2")));
  }

  @Test
  public void testInterfaceRangeExtraction() {
    String hostname = "nxos_interface_range";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/1.1", "Ethernet1/1.2"));
  }

  @Test
  public void testInterfaceSpanningTreeParsing() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_interface_spanning_tree"), notNullValue());
  }

  @Test
  public void testInterfaceSpeedConversion() throws IOException {
    String hostname = "nxos_interface_speed";
    Configuration c = parseConfig(hostname);

    assertThat(c.getAllInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2", "port-channel1"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(iface, hasSpeed(100E9D));
      assertThat(iface, hasBandwidth(100E9D));
    }
    // TODO: assert inferred speed for Ethernet1/2 maybe
  }

  @Test
  public void testInterfaceSpeedExtraction() {
    String hostname = "nxos_interface_speed";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2", "port-channel1"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(iface.getSpeedMbps(), equalTo(100000));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertThat(iface.getSpeedMbps(), nullValue());
    }
  }

  @Test
  public void testInterfaceStormControlParsing() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_interface_storm_control"), notNullValue());
  }

  @Test
  public void testInterfaceSwitchportConversion() throws IOException {
    String hostname = "nxos_interface_switchport";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/2.1",
            "Ethernet1/2.2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8",
            "Ethernet1/9",
            "Ethernet1/10",
            "Ethernet1/11",
            "Ethernet1/12",
            "Ethernet1/13",
            "Ethernet1/14",
            "Ethernet1/15",
            "loopback0",
            "mgmt0",
            "port-channel1",
            "port-channel2",
            "port-channel2.1"));

    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("loopback0");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("mgmt0");
      // Management interfaces are blacklisted in post-processing
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel1");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2.1");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2.2");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), equalTo(2));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel2");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel2.1");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(2));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(2));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/6");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/7");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/8");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3966))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/9");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/10");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 2))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/11");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    // TODO: something with dot1q-tunnel, fex-fabric
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/14");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/15");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
  }

  @Test
  public void testInterfaceSwitchportExtraction() {
    String hostname = "nxos_interface_switchport";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/2.1",
            "Ethernet1/2.2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8",
            "Ethernet1/9",
            "Ethernet1/10",
            "Ethernet1/11",
            "Ethernet1/12",
            "Ethernet1/13",
            "Ethernet1/14",
            "Ethernet1/15",
            "loopback0",
            "mgmt0",
            "port-channel1",
            "port-channel2",
            "port-channel2.1"));

    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("loopback0");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("mgmt0");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2.1");
      assertTrue(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2.2");
      assertTrue(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), equalTo(2));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel2.1");
      assertTrue(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(2));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(2));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/7");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/8");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3966))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/9");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/10");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 2))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/11");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    // TODO: something with dot1q-tunnel, fex-fabric
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/14");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/15");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertTrue(iface.getSwitchportMonitor());
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
  }

  @Test
  public void testInterfaceSwitchportExtractionInvalid() {
    String hostname = "nxos_interface_switchport_invalid";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/2.1", "Ethernet1/3", "Ethernet1/4"));

    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2.1");
      assertTrue(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
  }

  @Test
  public void testInterfacePortChannelParsing() {
    // TODO: change to extraction test
    assertThat(parseVendorConfig("nxos_interface_port_channel"), notNullValue());
  }

  /**
   * A generic test that exercised basic interface property extraction and conversion.
   *
   * <p>Note that this should only be for <strong>simple</strong> properties; anything with many
   * cases deserves its own unit test. (See, e.g., {@link #testInterfaceSwitchportExtraction()}.
   */
  @Test
  public void testInterfacePropertiesConversion() throws IOException {
    Configuration c = parseConfig("nxos_interface_properties");
    assertThat(c, hasInterface("Ethernet1/1", any(org.batfish.datamodel.Interface.class)));

    org.batfish.datamodel.Interface eth11 = c.getAllInterfaces().get("Ethernet1/1");
    assertThat(
        eth11,
        allOf(
            hasDescription(
                "here is a description with punctuation! and IP address 1.2.3.4/24 etc."),
            hasMtu(9216)));
    // TODO: convert and test delay
  }

  @Test
  public void testInterfacePropertiesExtraction() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_interface_properties");
    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(iface.getDelayTensOfMicroseconds(), equalTo(10));
      assertThat(
          iface.getDescription(),
          equalTo("here is a description with punctuation! and IP address 1.2.3.4/24 etc."));
      assertThat(iface.getMtu(), equalTo(9216));
    }
  }

  @Test
  public void testIpAccessListConversion() throws IOException {
    String hostname = "nxos_ip_access_list";
    Configuration c = parseConfig(hostname);
    BddTestbed tb = new BddTestbed(c.getIpAccessLists(), c.getIpSpaces());

    assertThat(
        c.getIpAccessLists(),
        hasKeys(
            "acl_global_options",
            "acl_indices",
            "acl_simple_protocols",
            "acl_common_ip_options_destination_ip",
            "acl_common_ip_options_source_ip",
            "acl_common_ip_options_dscp",
            "acl_common_ip_options_packet_length",
            "acl_common_ip_options_precedence",
            "acl_common_ip_options_ttl",
            "acl_common_ip_options_log",
            "acl_icmp",
            "acl_igmp",
            "acl_tcp_flags",
            "acl_tcp_flags_mask",
            "acl_tcp_destination_ports",
            "acl_tcp_destination_ports_named",
            "acl_tcp_source_ports",
            "acl_tcp_http_method",
            "acl_tcp_option_length",
            "acl_tcp_established",
            "acl_udp_destination_ports",
            "acl_udp_destination_ports_named",
            "acl_udp_source_ports",
            "acl_udp_vxlan",
            "acl_l4_fragments_semantics"));
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_global_options");
      assertThat(acl.getLines(), empty());
    }
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_indices");
      assertThat(
          acl.getLines().stream()
              .map(line -> toBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toBDD(matchIpProtocol(1)),
              toBDD(matchIpProtocol(4)),
              toBDD(matchIpProtocol(2)),
              toBDD(matchIpProtocol(3))));
    }
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_simple_protocols");
      assertThat(
          acl.getLines().stream()
              .map(line -> toBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toBDD(matchIpProtocol(IpProtocol.AHP)),
              toBDD(matchIpProtocol(IpProtocol.EIGRP)),
              toBDD(matchIpProtocol(IpProtocol.ESP)),
              toBDD(matchIpProtocol(IpProtocol.GRE)),
              toBDD(AclLineMatchExprs.TRUE),
              toBDD(matchIpProtocol(IpProtocol.IPIP)),
              toBDD(matchIpProtocol(IpProtocol.OSPF)),
              toBDD(matchIpProtocol(IpProtocol.IPCOMP)),
              toBDD(matchIpProtocol(IpProtocol.PIM)),
              toBDD(matchIpProtocol(1))));
    }
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_common_ip_options_destination_ip");
      assertThat(
          acl.getLines().stream()
              .map(line -> tb.toBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              tb.toBDD(matchDst(ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.0.0.255")))),
              tb.toBDD(matchDst(Prefix.parse("10.0.1.0/24"))),
              tb.toBDD(matchDst(Ip.parse("10.0.5.5"))),
              tb.toBDD(matchDst(Ip.parse("10.0.2.2"))),
              tb.toBDD(AclLineMatchExprs.TRUE)));
    }
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_common_ip_options_source_ip");
      assertThat(
          acl.getLines().stream()
              .map(line -> tb.toBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              tb.toBDD(matchSrc(ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.0.0.255")))),
              tb.toBDD(matchSrc(Prefix.parse("10.0.1.0/24"))),
              tb.toBDD(matchSrc(Ip.parse("10.0.5.6"))),
              tb.toBDD(matchSrc(Ip.parse("10.0.2.2"))),
              tb.toBDD(AclLineMatchExprs.TRUE)));
    }
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_common_ip_options_dscp");
      assertThat(
          acl.getLines().stream()
              .map(line -> toBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toBDD(matchDscp(1)),
              toBDD(matchDscp(DscpType.AF11)),
              toBDD(matchDscp(DscpType.AF12)),
              toBDD(matchDscp(DscpType.AF13)),
              toBDD(matchDscp(DscpType.AF21)),
              toBDD(matchDscp(DscpType.AF22)),
              toBDD(matchDscp(DscpType.AF23)),
              toBDD(matchDscp(DscpType.AF31)),
              toBDD(matchDscp(DscpType.AF32)),
              toBDD(matchDscp(DscpType.AF33)),
              toBDD(matchDscp(DscpType.AF41)),
              toBDD(matchDscp(DscpType.AF42)),
              toBDD(matchDscp(DscpType.AF43)),
              toBDD(matchDscp(DscpType.CS1)),
              toBDD(matchDscp(DscpType.CS2)),
              toBDD(matchDscp(DscpType.CS3)),
              toBDD(matchDscp(DscpType.CS4)),
              toBDD(matchDscp(DscpType.CS5)),
              toBDD(matchDscp(DscpType.CS6)),
              toBDD(matchDscp(DscpType.CS7)),
              toBDD(matchDscp(DscpType.DEFAULT)),
              toBDD(matchDscp(DscpType.EF))));
    }
    // ignore 'log' option
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_common_ip_options_packet_length");
      assertThat(
          acl.getLines().stream()
              .map(line -> toBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toBDD(matchPacketLength(100)),
              toBDD(matchPacketLength(IntegerSpace.of(Range.closed(0, 199)))),
              toBDD(
                  matchPacketLength(
                      IntegerSpace.of(Range.closed(300, Integer.MAX_VALUE))
                          .intersection(PACKET_LENGTH_RANGE))),
              toBDD(matchPacketLength(PACKET_LENGTH_RANGE.difference(IntegerSpace.of(400)))),
              toBDD(matchPacketLength(IntegerSpace.of(Range.closed(500, 600))))));
    }
    // TODO: support precedence matching
    // TODO: support TTL matching
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_icmp");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toIcmpIfBDD(matchIcmpType(0)),
              toIcmpIfBDD(matchIcmp(1, 2)),
              toIcmpIfBDD(
                  matchIcmp(
                      IcmpType.DESTINATION_UNREACHABLE,
                      IcmpCode.COMMUNICATION_ADMINISTRATIVELY_PROHIBITED)),
              toIcmpIfBDD(matchIcmpType(IcmpType.ALTERNATE_ADDRESS)),
              toIcmpIfBDD(matchIcmpType(IcmpType.CONVERSION_ERROR)),
              toIcmpIfBDD(
                  matchIcmp(
                      IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_HOST_PROHIBITED)),
              toIcmpIfBDD(
                  matchIcmp(
                      IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_NETWORK_PROHIBITED)),
              toIcmpIfBDD(matchIcmpType(IcmpType.ECHO_REQUEST)),
              toIcmpIfBDD(matchIcmpType(IcmpType.ECHO_REPLY)),
              toIcmpIfBDD(matchIcmpType(IcmpType.PARAMETER_PROBLEM)),
              toIcmpIfBDD(
                  matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.SOURCE_HOST_ISOLATED)),
              toIcmpIfBDD(
                  matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.HOST_PRECEDENCE_VIOLATION)),
              toIcmpIfBDD(matchIcmp(IcmpType.REDIRECT_MESSAGE, IcmpCode.HOST_ERROR)),
              toIcmpIfBDD(matchIcmp(IcmpType.REDIRECT_MESSAGE, IcmpCode.TOS_AND_HOST_ERROR)),
              toIcmpIfBDD(
                  matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.HOST_UNREACHABLE_FOR_TOS)),
              toIcmpIfBDD(
                  matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_HOST_UNKNOWN)),
              toIcmpIfBDD(matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.HOST_UNREACHABLE)),
              toIcmpIfBDD(matchIcmpType(IcmpType.INFO_REPLY)),
              toIcmpIfBDD(matchIcmpType(IcmpType.INFO_REQUEST)),
              toIcmpIfBDD(matchIcmpType(IcmpType.MASK_REPLY)),
              toIcmpIfBDD(matchIcmpType(IcmpType.MASK_REQUEST)),
              toIcmpIfBDD(matchIcmpType(IcmpType.MOBILE_REDIRECT)),
              toIcmpIfBDD(matchIcmp(IcmpType.REDIRECT_MESSAGE, IcmpCode.NETWORK_ERROR)),
              toIcmpIfBDD(matchIcmp(IcmpType.REDIRECT_MESSAGE, IcmpCode.TOS_AND_NETWORK_ERROR)),
              toIcmpIfBDD(
                  matchIcmp(
                      IcmpType.DESTINATION_UNREACHABLE, IcmpCode.NETWORK_UNREACHABLE_FOR_TOS)),
              toIcmpIfBDD(
                  matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.NETWORK_UNREACHABLE)),
              toIcmpIfBDD(
                  matchIcmp(
                      IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_NETWORK_UNKNOWN)),
              toIcmpIfBDD(matchIcmp(IcmpType.PARAMETER_PROBLEM, IcmpCode.BAD_LENGTH)),
              toIcmpIfBDD(matchIcmp(IcmpType.PARAMETER_PROBLEM, IcmpCode.REQUIRED_OPTION_MISSING)),
              toIcmpIfBDD(
                  matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.FRAGMENTATION_NEEDED)),
              toIcmpIfBDD(matchIcmp(IcmpType.PARAMETER_PROBLEM, IcmpCode.INVALID_IP_HEADER)),
              toIcmpIfBDD(matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.PORT_UNREACHABLE)),
              toIcmpIfBDD(
                  matchIcmp(
                      IcmpType.DESTINATION_UNREACHABLE, IcmpCode.PRECEDENCE_CUTOFF_IN_EFFECT)),
              toIcmpIfBDD(
                  matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.PROTOCOL_UNREACHABLE)),
              toIcmpIfBDD(
                  matchIcmp(
                      IcmpType.TIME_EXCEEDED, IcmpCode.TIME_EXCEEDED_DURING_FRAGMENT_REASSEMBLY)),
              toIcmpIfBDD(matchIcmpType(IcmpType.REDIRECT_MESSAGE)),
              toIcmpIfBDD(matchIcmpType(IcmpType.ROUTER_ADVERTISEMENT)),
              toIcmpIfBDD(matchIcmpType(IcmpType.ROUTER_SOLICITATION)),
              toIcmpIfBDD(matchIcmpType(IcmpType.SOURCE_QUENCH)),
              toIcmpIfBDD(
                  matchIcmp(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.SOURCE_ROUTE_FAILED)),
              toIcmpIfBDD(matchIcmpType(IcmpType.TIME_EXCEEDED)),
              toIcmpIfBDD(matchIcmpType(IcmpType.TIMESTAMP_REPLY)),
              toIcmpIfBDD(matchIcmpType(IcmpType.TIMESTAMP_REQUEST)),
              toIcmpIfBDD(matchIcmpType(IcmpType.TRACEROUTE)),
              toIcmpIfBDD(matchIcmp(IcmpType.TIME_EXCEEDED, IcmpCode.TTL_EQ_ZERO_DURING_TRANSIT)),
              toIcmpIfBDD(matchIcmpType(IcmpType.DESTINATION_UNREACHABLE))));
    }
    // TODO: support IGMP option matching
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_tcp_destination_ports");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toTcpIfBDD(matchDstPort(1)),
              toTcpIfBDD(
                  matchDstPort(
                      TCP_PORT_RANGE.intersection(
                          IntegerSpace.of(Range.closed(6, Integer.MAX_VALUE))))),
              toTcpIfBDD(matchDstPort(IntegerSpace.of(Range.closed(0, 9)))),
              toTcpIfBDD(matchDstPort(TCP_PORT_RANGE.difference(IntegerSpace.of(15)))),
              toTcpIfBDD(AclLineMatchExprs.FALSE), // TODO: support portgroup
              toTcpIfBDD(matchDstPort(IntegerSpace.of(Range.closed(20, 25))))));
    }
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_tcp_destination_ports_named");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toTcpIfBDD(matchDstPort(NamedPort.BGP.number())),
              toTcpIfBDD(matchDstPort(NamedPort.CHARGEN.number())),
              toTcpIfBDD(matchDstPort(NamedPort.CMDtcp_OR_SYSLOGudp.number())),
              toTcpIfBDD(matchDstPort(NamedPort.DAYTIME.number())),
              toTcpIfBDD(matchDstPort(NamedPort.DISCARD.number())),
              toTcpIfBDD(matchDstPort(NamedPort.DOMAIN.number())),
              toTcpIfBDD(matchDstPort(NamedPort.DRIP.number())),
              toTcpIfBDD(matchDstPort(NamedPort.ECHO.number())),
              toTcpIfBDD(matchDstPort(NamedPort.BIFFudp_OR_EXECtcp.number())),
              toTcpIfBDD(matchDstPort(NamedPort.FINGER.number())),
              toTcpIfBDD(matchDstPort(NamedPort.FTP.number())),
              toTcpIfBDD(matchDstPort(NamedPort.FTP_DATA.number())),
              toTcpIfBDD(matchDstPort(NamedPort.GOPHER.number())),
              toTcpIfBDD(matchDstPort(NamedPort.HOSTNAME.number())),
              toTcpIfBDD(matchDstPort(NamedPort.IDENT.number())),
              toTcpIfBDD(matchDstPort(NamedPort.IRC.number())),
              toTcpIfBDD(matchDstPort(NamedPort.KLOGIN.number())),
              toTcpIfBDD(matchDstPort(NamedPort.KSHELL.number())),
              toTcpIfBDD(matchDstPort(NamedPort.LOGINtcp_OR_WHOudp.number())),
              toTcpIfBDD(matchDstPort(NamedPort.LPD.number())),
              toTcpIfBDD(matchDstPort(NamedPort.NNTP.number())),
              toTcpIfBDD(matchDstPort(NamedPort.PIM_AUTO_RP.number())),
              toTcpIfBDD(matchDstPort(NamedPort.POP2.number())),
              toTcpIfBDD(matchDstPort(NamedPort.POP3.number())),
              toTcpIfBDD(matchDstPort(NamedPort.SMTP.number())),
              toTcpIfBDD(matchDstPort(NamedPort.SUNRPC.number())),
              toTcpIfBDD(matchDstPort(NamedPort.TACACS.number())),
              toTcpIfBDD(matchDstPort(NamedPort.TALK.number())),
              toTcpIfBDD(matchDstPort(NamedPort.TELNET.number())),
              toTcpIfBDD(matchDstPort(NamedPort.TIME.number())),
              toTcpIfBDD(matchDstPort(NamedPort.UUCP.number())),
              toTcpIfBDD(matchDstPort(NamedPort.WHOIS.number())),
              toTcpIfBDD(matchDstPort(NamedPort.HTTP.number()))));
    }
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_tcp_source_ports");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toTcpIfBDD(matchSrcPort(1)),
              toTcpIfBDD(
                  matchSrcPort(
                      TCP_PORT_RANGE.intersection(
                          IntegerSpace.of(Range.closed(6, Integer.MAX_VALUE))))),
              toTcpIfBDD(matchSrcPort(IntegerSpace.of(Range.closed(0, 9)))),
              toTcpIfBDD(matchSrcPort(TCP_PORT_RANGE.difference(IntegerSpace.of(15)))),
              toTcpIfBDD(AclLineMatchExprs.FALSE), // TODO: support portgroup
              toTcpIfBDD(matchSrcPort(IntegerSpace.of(Range.closed(20, 25))))));
    }
    // TODO: support HTTP method matching
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_tcp_flags");
      TcpFlagsMatchConditions.Builder conditions =
          TcpFlagsMatchConditions.builder()
              .setUseAck(true)
              .setUseCwr(false)
              .setUseEce(false)
              .setUseFin(true)
              .setUsePsh(true)
              .setUseRst(true)
              .setUseSyn(true)
              .setUseUrg(true);
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toTcpIfBDD(
                  matchTcpFlags(
                      conditions.setTcpFlags(TcpFlags.builder().setAck(true).build()).build())),
              toTcpIfBDD(
                  matchTcpFlags(
                      conditions.setTcpFlags(TcpFlags.builder().setFin(true).build()).build())),
              toTcpIfBDD(
                  matchTcpFlags(
                      conditions.setTcpFlags(TcpFlags.builder().setPsh(true).build()).build())),
              toTcpIfBDD(
                  matchTcpFlags(
                      conditions.setTcpFlags(TcpFlags.builder().setRst(true).build()).build())),
              toTcpIfBDD(
                  matchTcpFlags(
                      conditions.setTcpFlags(TcpFlags.builder().setSyn(true).build()).build())),
              toTcpIfBDD(
                  matchTcpFlags(
                      conditions.setTcpFlags(TcpFlags.builder().setUrg(true).build()).build()))));
    }
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_tcp_flags_mask");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toTcpIfBDD(
                  matchTcpFlags(
                      TcpFlagsMatchConditions.builder()
                          .setUseAck(true)
                          .setTcpFlags(TcpFlags.builder().setAck(true).build())
                          .build()))));
    }
    // TODO: support tcp option length
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_tcp_established");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toTcpIfBDD(
                  matchTcpFlags(
                      TcpFlagsMatchConditions.builder()
                          .setUseAck(true)
                          .setTcpFlags(TcpFlags.builder().setAck(true).build())
                          .build(),
                      TcpFlagsMatchConditions.builder()
                          .setUseRst(true)
                          .setTcpFlags(TcpFlags.builder().setRst(true).build())
                          .build()))));
    }
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_udp_destination_ports");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toUdpIfBDD(matchDstPort(1)),
              toUdpIfBDD(
                  matchDstPort(
                      UDP_PORT_RANGE.intersection(
                          IntegerSpace.of(Range.closed(6, Integer.MAX_VALUE))))),
              toUdpIfBDD(matchDstPort(IntegerSpace.of(Range.closed(0, 9)))),
              toUdpIfBDD(matchDstPort(UDP_PORT_RANGE.difference(IntegerSpace.of(15)))),
              toUdpIfBDD(AclLineMatchExprs.FALSE), // TODO: support portgroup
              toUdpIfBDD(matchDstPort(IntegerSpace.of(Range.closed(20, 25))))));
    }
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_udp_destination_ports_named");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toUdpIfBDD(matchDstPort(NamedPort.BIFFudp_OR_EXECtcp.number())),
              toUdpIfBDD(matchDstPort(NamedPort.BOOTPC.number())),
              toUdpIfBDD(matchDstPort(NamedPort.BOOTPS_OR_DHCP.number())),
              toUdpIfBDD(matchDstPort(NamedPort.DISCARD.number())),
              toUdpIfBDD(matchDstPort(NamedPort.DNSIX.number())),
              toUdpIfBDD(matchDstPort(NamedPort.DOMAIN.number())),
              toUdpIfBDD(matchDstPort(NamedPort.ECHO.number())),
              toUdpIfBDD(matchDstPort(NamedPort.ISAKMP.number())),
              toUdpIfBDD(matchDstPort(NamedPort.MOBILE_IP_AGENT.number())),
              toUdpIfBDD(matchDstPort(NamedPort.NAMESERVER.number())),
              toUdpIfBDD(matchDstPort(NamedPort.NETBIOS_DGM.number())),
              toUdpIfBDD(matchDstPort(NamedPort.NETBIOS_NS.number())),
              toUdpIfBDD(matchDstPort(NamedPort.NETBIOS_SSN.number())),
              toUdpIfBDD(matchDstPort(NamedPort.NON500_ISAKMP.number())),
              toUdpIfBDD(matchDstPort(NamedPort.NTP.number())),
              toUdpIfBDD(matchDstPort(NamedPort.PIM_AUTO_RP.number())),
              toUdpIfBDD(matchDstPort(NamedPort.EFStcp_OR_RIPudp.number())),
              toUdpIfBDD(matchDstPort(NamedPort.SNMP.number())),
              toUdpIfBDD(matchDstPort(NamedPort.SNMPTRAP.number())),
              toUdpIfBDD(matchDstPort(NamedPort.SUNRPC.number())),
              toUdpIfBDD(matchDstPort(NamedPort.CMDtcp_OR_SYSLOGudp.number())),
              toUdpIfBDD(matchDstPort(NamedPort.TACACS.number())),
              toUdpIfBDD(matchDstPort(NamedPort.TALK.number())),
              toUdpIfBDD(matchDstPort(NamedPort.TFTP.number())),
              toUdpIfBDD(matchDstPort(NamedPort.TIME.number())),
              toUdpIfBDD(matchDstPort(NamedPort.LOGINtcp_OR_WHOudp.number())),
              toUdpIfBDD(matchDstPort(NamedPort.XDMCP.number()))));
    }
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_udp_source_ports");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toUdpIfBDD(matchSrcPort(1)),
              toUdpIfBDD(
                  matchSrcPort(
                      UDP_PORT_RANGE.intersection(
                          IntegerSpace.of(Range.closed(6, Integer.MAX_VALUE))))),
              toUdpIfBDD(matchSrcPort(IntegerSpace.of(Range.closed(0, 9)))),
              toUdpIfBDD(matchSrcPort(UDP_PORT_RANGE.difference(IntegerSpace.of(15)))),
              toUdpIfBDD(AclLineMatchExprs.FALSE), // TODO: support portgroup
              toUdpIfBDD(matchSrcPort(IntegerSpace.of(Range.closed(20, 25))))));
    }
    // TODO: support UDP VXLAN matching
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_l4_fragments_semantics");
      // check behavior for initial fragments
      assertThat(
          acl.getLines().stream()
              .map(line -> toIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toIcmpIfBDD(AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIcmpType(0))),
              toIcmpIfBDD(AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIcmpType(1))),
              toIfBDD(AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIpProtocol(2))),
              toIfBDD(AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIpProtocol(3))),
              toBDD(AclLineMatchExprs.FALSE),
              toBDD(AclLineMatchExprs.FALSE)));

      // check behavior for non-initial fragments
      assertThat(
          acl.getLines().stream()
              .map(line -> toNonIfBDD(line.getMatchCondition()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              toNonIfBDD(
                  AclLineMatchExprs.and(
                      matchSrc(Ip.parse("192.0.2.1")), matchIpProtocol(IpProtocol.ICMP))),
              toBDD(AclLineMatchExprs.FALSE),
              toNonIfBDD(
                  AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIpProtocol(2))),
              toNonIfBDD(
                  AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIpProtocol(3))),
              toNonIfBDD(
                  AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIpProtocol(4))),
              toNonIfBDD(
                  AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIpProtocol(5)))));
    }
  }

  @Test
  public void testIpAccessListExtraction() {
    String hostname = "nxos_ip_access_list";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getIpAccessLists(),
        hasKeys(
            "acl_global_options",
            "acl_indices",
            "acl_simple_protocols",
            "acl_common_ip_options_destination_ip",
            "acl_common_ip_options_source_ip",
            "acl_common_ip_options_dscp",
            "acl_common_ip_options_packet_length",
            "acl_common_ip_options_precedence",
            "acl_common_ip_options_ttl",
            "acl_common_ip_options_log",
            "acl_icmp",
            "acl_igmp",
            "acl_tcp_flags",
            "acl_tcp_flags_mask",
            "acl_tcp_destination_ports",
            "acl_tcp_destination_ports_named",
            "acl_tcp_source_ports",
            "acl_tcp_http_method",
            "acl_tcp_option_length",
            "acl_tcp_established",
            "acl_udp_destination_ports",
            "acl_udp_destination_ports_named",
            "acl_udp_source_ports",
            "acl_udp_vxlan",
            "acl_l4_fragments_semantics"));
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_global_options");
      assertThat(acl.getFragmentsBehavior(), equalTo(FragmentsBehavior.PERMIT_ALL));
      assertThat(acl.getLines(), anEmptyMap());
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_indices");
      assertThat(acl.getFragmentsBehavior(), equalTo(FragmentsBehavior.DEFAULT));
      // check keySet directly to test iteration order
      assertThat(acl.getLines().keySet(), contains(1L, 10L, 13L, 15L, 25L, 35L));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_simple_protocols");
      assertThat(acl.getFragmentsBehavior(), equalTo(FragmentsBehavior.DEFAULT));
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getProtocol)
              .collect(Collectors.toList()), // cannot use immutable list because of null value
          contains(
              equalTo(IpProtocol.AHP),
              equalTo(IpProtocol.EIGRP),
              equalTo(IpProtocol.ESP),
              equalTo(IpProtocol.GRE),
              nullValue(),
              equalTo(IpProtocol.IPIP),
              equalTo(IpProtocol.OSPF),
              equalTo(IpProtocol.IPCOMP),
              equalTo(IpProtocol.PIM),
              equalTo(IpProtocol.fromNumber(1))));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_destination_ip");
      Iterator<IpAddressSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getDstAddressSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      IpAddressSpec spec;

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(DST_IP_BDD),
          equalTo(
              ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.0.0.255"))
                  .toIpSpace()
                  .accept(DST_IP_BDD)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(DST_IP_BDD),
          equalTo(Prefix.parse("10.0.1.0/24").toIpSpace().accept(DST_IP_BDD)));

      spec = specs.next();
      assertThat(((AddrGroupIpAddressSpec) spec).getName(), equalTo("mydstaddrgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(DST_IP_BDD),
          equalTo(Ip.parse("10.0.2.2").toIpSpace().accept(DST_IP_BDD)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(DST_IP_BDD),
          equalTo(UniverseIpSpace.INSTANCE.accept(DST_IP_BDD)));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_source_ip");
      Iterator<IpAddressSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getSrcAddressSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      IpAddressSpec spec;

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(SRC_IP_BDD),
          equalTo(
              ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.0.0.255"))
                  .toIpSpace()
                  .accept(SRC_IP_BDD)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(SRC_IP_BDD),
          equalTo(Prefix.parse("10.0.1.0/24").toIpSpace().accept(SRC_IP_BDD)));

      spec = specs.next();
      assertThat(((AddrGroupIpAddressSpec) spec).getName(), equalTo("mysrcaddrgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(SRC_IP_BDD),
          equalTo(Ip.parse("10.0.2.2").toIpSpace().accept(SRC_IP_BDD)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(SRC_IP_BDD),
          equalTo(UniverseIpSpace.INSTANCE.accept(SRC_IP_BDD)));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_dscp");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL3Options)
              .map(Layer3Options::getDscp)
              .collect(ImmutableList.toImmutableList()),
          contains(
              1,
              DscpType.AF11.number(),
              DscpType.AF12.number(),
              DscpType.AF13.number(),
              DscpType.AF21.number(),
              DscpType.AF22.number(),
              DscpType.AF23.number(),
              DscpType.AF31.number(),
              DscpType.AF32.number(),
              DscpType.AF33.number(),
              DscpType.AF41.number(),
              DscpType.AF42.number(),
              DscpType.AF43.number(),
              DscpType.CS1.number(),
              DscpType.CS2.number(),
              DscpType.CS3.number(),
              DscpType.CS4.number(),
              DscpType.CS5.number(),
              DscpType.CS6.number(),
              DscpType.CS7.number(),
              DscpType.DEFAULT.number(),
              DscpType.EF.number()));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_log");
      Iterator<IpAccessListLine> lines = acl.getLines().values().iterator();
      IpAccessListLine line;
      line = lines.next();
      assertTrue(((ActionIpAccessListLine) line).getLog());
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_packet_length");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL3Options)
              .map(Layer3Options::getPacketLength)
              .collect(ImmutableList.toImmutableList()),
          contains(
              IntegerSpace.of(100),
              IntegerSpace.of(Range.closed(20, 199)),
              IntegerSpace.of(Range.closed(301, 9210)),
              IntegerSpace.of(Range.closed(20, 9210)).difference(IntegerSpace.of(400)),
              IntegerSpace.of(Range.closed(500, 600))));
    }
    // TODO: extract and test precedence once name->value mappings are known
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_ttl");
      assertThat(
          ((ActionIpAccessListLine) acl.getLines().values().iterator().next())
              .getL3Options()
              .getTtl(),
          equalTo(5));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_icmp");
      assertThat(
          acl.getLines().values().stream()
              .filter(ActionIpAccessListLine.class::isInstance) // filter ICMPv6
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(IcmpOptions.class::cast)
              .map(icmpOptions -> immutableEntry(icmpOptions.getType(), icmpOptions.getCode()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              immutableEntry(0, null),
              immutableEntry(1, 2),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE,
                  IcmpCode.COMMUNICATION_ADMINISTRATIVELY_PROHIBITED),
              immutableEntry(IcmpType.ALTERNATE_ADDRESS, null),
              immutableEntry(IcmpType.CONVERSION_ERROR, null),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_HOST_PROHIBITED),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_NETWORK_PROHIBITED),
              immutableEntry(IcmpType.ECHO_REQUEST, null),
              immutableEntry(IcmpType.ECHO_REPLY, null),
              immutableEntry(IcmpType.PARAMETER_PROBLEM, null),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.SOURCE_HOST_ISOLATED),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.HOST_PRECEDENCE_VIOLATION),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, IcmpCode.HOST_ERROR),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, IcmpCode.TOS_AND_HOST_ERROR),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.HOST_UNREACHABLE_FOR_TOS),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_HOST_UNKNOWN),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.HOST_UNREACHABLE),
              immutableEntry(IcmpType.INFO_REPLY, null),
              immutableEntry(IcmpType.INFO_REQUEST, null),
              immutableEntry(IcmpType.MASK_REPLY, null),
              immutableEntry(IcmpType.MASK_REQUEST, null),
              immutableEntry(IcmpType.MOBILE_REDIRECT, null),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, IcmpCode.NETWORK_ERROR),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, IcmpCode.TOS_AND_NETWORK_ERROR),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.NETWORK_UNREACHABLE_FOR_TOS),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.NETWORK_UNREACHABLE),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_NETWORK_UNKNOWN),
              immutableEntry(IcmpType.PARAMETER_PROBLEM, IcmpCode.BAD_LENGTH),
              immutableEntry(IcmpType.PARAMETER_PROBLEM, IcmpCode.REQUIRED_OPTION_MISSING),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.FRAGMENTATION_NEEDED),
              immutableEntry(IcmpType.PARAMETER_PROBLEM, IcmpCode.INVALID_IP_HEADER),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.PORT_UNREACHABLE),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.PRECEDENCE_CUTOFF_IN_EFFECT),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.PROTOCOL_UNREACHABLE),
              immutableEntry(
                  IcmpType.TIME_EXCEEDED, IcmpCode.TIME_EXCEEDED_DURING_FRAGMENT_REASSEMBLY),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, null),
              immutableEntry(IcmpType.ROUTER_ADVERTISEMENT, null),
              immutableEntry(IcmpType.ROUTER_SOLICITATION, null),
              immutableEntry(IcmpType.SOURCE_QUENCH, null),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.SOURCE_ROUTE_FAILED),
              immutableEntry(IcmpType.TIME_EXCEEDED, null),
              immutableEntry(IcmpType.TIMESTAMP_REPLY, null),
              immutableEntry(IcmpType.TIMESTAMP_REQUEST, null),
              immutableEntry(IcmpType.TRACEROUTE, null),
              immutableEntry(IcmpType.TIME_EXCEEDED, IcmpCode.TTL_EQ_ZERO_DURING_TRANSIT),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, null)));
    }
    // TODO: extract and test IGMP types (and codes?) once name->value mappings are known
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_destination_ports");
      Iterator<PortSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(TcpOptions.class::cast)
              .map(TcpOptions::getDstPortSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      PortSpec spec;

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(1)));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(6, 65535))));

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(0, 9))));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(),
          equalTo(IntegerSpace.of(Range.closed(0, 65535)).difference(IntegerSpace.of(15))));

      spec = specs.next();
      assertThat(((PortGroupPortSpec) spec).getName(), equalTo("mydstportgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(20, 25))));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_destination_ports_named");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(TcpOptions.class::cast)
              .map(TcpOptions::getDstPortSpec)
              .map(LiteralPortSpec.class::cast)
              .map(LiteralPortSpec::getPorts)
              .map(IntegerSpace::singletonValue)
              .collect(ImmutableList.toImmutableList()),
          contains(
              NamedPort.BGP.number(),
              NamedPort.CHARGEN.number(),
              NamedPort.CMDtcp_OR_SYSLOGudp.number(),
              NamedPort.DAYTIME.number(),
              NamedPort.DISCARD.number(),
              NamedPort.DOMAIN.number(),
              NamedPort.DRIP.number(),
              NamedPort.ECHO.number(),
              NamedPort.BIFFudp_OR_EXECtcp.number(),
              NamedPort.FINGER.number(),
              NamedPort.FTP.number(),
              NamedPort.FTP_DATA.number(),
              NamedPort.GOPHER.number(),
              NamedPort.HOSTNAME.number(),
              NamedPort.IDENT.number(),
              NamedPort.IRC.number(),
              NamedPort.KLOGIN.number(),
              NamedPort.KSHELL.number(),
              NamedPort.LOGINtcp_OR_WHOudp.number(),
              NamedPort.LPD.number(),
              NamedPort.NNTP.number(),
              NamedPort.PIM_AUTO_RP.number(),
              NamedPort.POP2.number(),
              NamedPort.POP3.number(),
              NamedPort.SMTP.number(),
              NamedPort.SUNRPC.number(),
              NamedPort.TACACS.number(),
              NamedPort.TALK.number(),
              NamedPort.TELNET.number(),
              NamedPort.TIME.number(),
              NamedPort.UUCP.number(),
              NamedPort.WHOIS.number(),
              NamedPort.HTTP.number()));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_source_ports");
      Iterator<PortSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(TcpOptions.class::cast)
              .map(TcpOptions::getSrcPortSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      PortSpec spec;

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(1)));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(6, 65535))));

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(0, 9))));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(),
          equalTo(IntegerSpace.of(Range.closed(0, 65535)).difference(IntegerSpace.of(15))));

      spec = specs.next();
      assertThat(((PortGroupPortSpec) spec).getName(), equalTo("mysrcportgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(20, 25))));
    }
    // TODO: extract and test http-method match
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_flags");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(TcpOptions.class::cast)
              .map(TcpOptions::getTcpFlags)
              .collect(ImmutableList.toImmutableList()),
          contains(
              TcpFlags.builder().setAck(true).build(),
              TcpFlags.builder().setFin(true).build(),
              TcpFlags.builder().setPsh(true).build(),
              TcpFlags.builder().setRst(true).build(),
              TcpFlags.builder().setSyn(true).build(),
              TcpFlags.builder().setUrg(true).build()));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_flags_mask");
      assertThat(
          ((TcpOptions)
                  ((ActionIpAccessListLine) acl.getLines().values().iterator().next())
                      .getL4Options())
              .getTcpFlagsMask(),
          equalTo(47));
    }
    // TODO: extract and test tcp-option-length match
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_established");
      assertTrue(
          ((TcpOptions)
                  ((ActionIpAccessListLine) acl.getLines().values().iterator().next())
                      .getL4Options())
              .getEstablished());
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_udp_destination_ports");
      Iterator<PortSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(UdpOptions.class::cast)
              .map(UdpOptions::getDstPortSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      PortSpec spec;

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(1)));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(6, 65535))));

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(0, 9))));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(),
          equalTo(IntegerSpace.of(Range.closed(0, 65535)).difference(IntegerSpace.of(15))));

      spec = specs.next();
      assertThat(((PortGroupPortSpec) spec).getName(), equalTo("mydstportgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(20, 25))));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_udp_destination_ports_named");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(UdpOptions.class::cast)
              .map(UdpOptions::getDstPortSpec)
              .map(LiteralPortSpec.class::cast)
              .map(LiteralPortSpec::getPorts)
              .map(IntegerSpace::singletonValue)
              .collect(ImmutableList.toImmutableList()),
          contains(
              NamedPort.BIFFudp_OR_EXECtcp.number(),
              NamedPort.BOOTPC.number(),
              NamedPort.BOOTPS_OR_DHCP.number(),
              NamedPort.DISCARD.number(),
              NamedPort.DNSIX.number(),
              NamedPort.DOMAIN.number(),
              NamedPort.ECHO.number(),
              NamedPort.ISAKMP.number(),
              NamedPort.MOBILE_IP_AGENT.number(),
              NamedPort.NAMESERVER.number(),
              NamedPort.NETBIOS_DGM.number(),
              NamedPort.NETBIOS_NS.number(),
              NamedPort.NETBIOS_SSN.number(),
              NamedPort.NON500_ISAKMP.number(),
              NamedPort.NTP.number(),
              NamedPort.PIM_AUTO_RP.number(),
              NamedPort.EFStcp_OR_RIPudp.number(),
              NamedPort.SNMP.number(),
              NamedPort.SNMPTRAP.number(),
              NamedPort.SUNRPC.number(),
              NamedPort.CMDtcp_OR_SYSLOGudp.number(),
              NamedPort.TACACS.number(),
              NamedPort.TALK.number(),
              NamedPort.TFTP.number(),
              NamedPort.TIME.number(),
              NamedPort.LOGINtcp_OR_WHOudp.number(),
              NamedPort.XDMCP.number()));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_udp_source_ports");
      Iterator<PortSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(UdpOptions.class::cast)
              .map(UdpOptions::getSrcPortSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      PortSpec spec;

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(1)));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(6, 65535))));

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(0, 9))));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(),
          equalTo(IntegerSpace.of(Range.closed(0, 65535)).difference(IntegerSpace.of(15))));

      spec = specs.next();
      assertThat(((PortGroupPortSpec) spec).getName(), equalTo("mysrcportgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(20, 25))));
    }
    // TODO: extract and test UDP nve vni match
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_l4_fragments_semantics");
      Iterator<IpAccessListLine> lines = acl.getLines().values().iterator();
      ActionIpAccessListLine line;

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(SRC_IP_BDD),
          equalTo(SRC_IP_BDD.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.ICMP));

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(SRC_IP_BDD),
          equalTo(SRC_IP_BDD.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.ICMP));

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(SRC_IP_BDD),
          equalTo(SRC_IP_BDD.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.fromNumber(2)));

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(SRC_IP_BDD),
          equalTo(SRC_IP_BDD.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.fromNumber(3)));

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(SRC_IP_BDD),
          equalTo(SRC_IP_BDD.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.fromNumber(4)));
      assertTrue(line.getFragments());

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(SRC_IP_BDD),
          equalTo(SRC_IP_BDD.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.fromNumber(5)));
      assertTrue(line.getFragments());
    }
  }

  @Test
  public void testIpAccessListReferences() throws IOException {
    String hostname = "nxos_ip_access_list_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(
        ans, hasNumReferrers(filename, CiscoNxosStructureType.IP_ACCESS_LIST, "acl_unused", 0));
  }

  @Test
  public void testIpAsPathAccessListConversion() throws IOException {
    String hostname = "nxos_ip_as_path_access_list";
    Configuration c = parseConfig(hostname);

    assertThat(c.getAsPathAccessLists(), hasKeys("aspacl_seq", "aspacl_test"));
    {
      AsPathAccessList list = c.getAsPathAccessLists().get("aspacl_seq");
      Iterator<AsPathAccessListLine> lines = list.getLines().iterator();
      AsPathAccessListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo(toJavaRegex("^1$")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo(toJavaRegex("^5$")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo(toJavaRegex("^10$")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo(toJavaRegex("^11$")));
    }
    {
      AsPathAccessList list = c.getAsPathAccessLists().get("aspacl_test");
      Iterator<AsPathAccessListLine> lines = list.getLines().iterator();
      AsPathAccessListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getRegex(), equalTo(toJavaRegex("(_1_2_|_2_1_)")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo(toJavaRegex("_1_")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo(toJavaRegex("_2_")));
    }
  }

  @Test
  public void testIpAsPathAccessListExtraction() {
    String hostname = "nxos_ip_as_path_access_list";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpAsPathAccessLists(), hasKeys("aspacl_seq", "aspacl_test"));
    {
      IpAsPathAccessList acl = vc.getIpAsPathAccessLists().get("aspacl_seq");
      // check keySet directly to test iteration order
      assertThat(acl.getLines().keySet(), contains(1L, 5L, 10L, 11L));
    }
    {
      IpAsPathAccessList acl = vc.getIpAsPathAccessLists().get("aspacl_test");
      // check keySet directly to test iteration order
      Iterator<IpAsPathAccessListLine> lines = acl.getLines().values().iterator();
      IpAsPathAccessListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getRegex(), equalTo("(_1_2_|_2_1_)"));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo("_1_"));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo("_2_"));
    }
  }

  @Test
  public void testIpAsPathAccessListInvalid() {
    _thrown.expect(ParserBatfishException.class);
    parseVendorConfig("nxos_ip_as_path_access_list_invalid");
  }

  @Test
  public void testIpCommunityListStandardConversion() throws IOException {
    String hostname = "nxos_ip_community_list_standard";
    Configuration c = parseConfig(hostname);

    assertThat(c.getCommunityLists(), hasKeys("cl_seq", "cl_values", "cl_test"));
    {
      CommunityList cl = c.getCommunityLists().get("cl_seq");
      Iterator<CommunityListLine> lines = cl.getLines().iterator();
      CommunityListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          contains(StandardCommunity.of(1, 1)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          contains(StandardCommunity.of(5, 5)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          contains(StandardCommunity.of(10, 10)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          contains(StandardCommunity.of(11, 11)));
    }
    {
      CommunityList cl = c.getCommunityLists().get("cl_values");
      Iterator<CommunityListLine> lines = cl.getLines().iterator();
      CommunityListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          contains(StandardCommunity.of(1, 1)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          contains(StandardCommunity.of(WellKnownCommunity.INTERNET)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          contains(StandardCommunity.of(WellKnownCommunity.NO_EXPORT_SUBCONFED)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          contains(StandardCommunity.of(WellKnownCommunity.NO_ADVERTISE)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          contains(StandardCommunity.of(WellKnownCommunity.NO_EXPORT)));
    }
    {
      CommunityList cl = c.getCommunityLists().get("cl_test");
      Iterator<CommunityListLine> lines = cl.getLines().iterator();
      CommunityListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          containsInAnyOrder(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          containsInAnyOrder(StandardCommunity.of(1, 1)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralCommunityConjunction) line.getMatchCondition()).getRequiredCommunities(),
          containsInAnyOrder(StandardCommunity.of(2, 2)));
    }
  }

  @Test
  public void testIpCommunityListStandardExtraction() {
    String hostname = "nxos_ip_community_list_standard";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpCommunityLists(), hasKeys("cl_seq", "cl_values", "cl_test"));
    {
      IpCommunityListStandard cl = (IpCommunityListStandard) vc.getIpCommunityLists().get("cl_seq");
      Iterator<IpCommunityListStandardLine> lines = cl.getLines().values().iterator();
      IpCommunityListStandardLine line;

      line = lines.next();
      assertThat(line.getLine(), equalTo(1L));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(1, 1)));

      line = lines.next();
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(5, 5)));

      line = lines.next();
      assertThat(line.getLine(), equalTo(10L));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(10, 10)));

      line = lines.next();
      assertThat(line.getLine(), equalTo(11L));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(11, 11)));

      assertFalse(lines.hasNext());
    }
    {
      IpCommunityListStandard cl =
          (IpCommunityListStandard) vc.getIpCommunityLists().get("cl_values");
      assertThat(
          cl.getLines().values().stream()
              .map(IpCommunityListStandardLine::getCommunities)
              .map(Iterables::getOnlyElement)
              .collect(ImmutableList.toImmutableList()),
          contains(
              StandardCommunity.of(1, 1),
              StandardCommunity.of(WellKnownCommunity.INTERNET),
              StandardCommunity.of(WellKnownCommunity.NO_EXPORT_SUBCONFED),
              StandardCommunity.of(WellKnownCommunity.NO_ADVERTISE),
              StandardCommunity.of(WellKnownCommunity.NO_EXPORT)));
    }
    {
      IpCommunityListStandard cl =
          (IpCommunityListStandard) vc.getIpCommunityLists().get("cl_test");
      Iterator<IpCommunityListStandardLine> lines = cl.getLines().values().iterator();
      IpCommunityListStandardLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(
          line.getCommunities(), contains(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(1, 1)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(2, 2)));

      assertFalse(lines.hasNext());
    }
  }

  @Test
  public void testIpPrefixListConversion() throws IOException {
    String hostname = "nxos_ip_prefix_list";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasRouteFilterLists(hasKeys("pl_empty", "pl_test", "pl_range")));
    {
      RouteFilterList r = c.getRouteFilterLists().get("pl_empty");
      assertThat(r, RouteFilterListMatchers.hasLines(empty()));
    }
    {
      RouteFilterList r = c.getRouteFilterLists().get("pl_test");
      Iterator<RouteFilterLine> lines = r.getLines().iterator();
      RouteFilterLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.0.3.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.0.1.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.0.2.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.0.4.0/24")));
    }
    {
      RouteFilterList r = c.getRouteFilterLists().get("pl_range");
      Iterator<RouteFilterLine> lines = r.getLines().iterator();
      RouteFilterLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(16, Prefix.MAX_PREFIX_LENGTH)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(24, 24)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(8, Prefix.MAX_PREFIX_LENGTH)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(20, 24)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(16, 24)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));
    }
  }

  @Test
  public void testIpPrefixListExtraction() {
    String hostname = "nxos_ip_prefix_list";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpPrefixLists(), hasKeys("pl_empty", "pl_test", "pl_range"));

    {
      IpPrefixList pl = vc.getIpPrefixLists().get("pl_empty");
      assertThat(pl.getDescription(), equalTo("An empty prefix-list"));
      assertThat(pl.getLines(), anEmptyMap());
    }
    {
      IpPrefixList pl = vc.getIpPrefixLists().get("pl_test");
      assertThat(pl.getDescription(), nullValue());
      assertThat(pl.getLines().keySet(), contains(3L, 5L, 10L, 15L));
      Iterator<IpPrefixListLine> lines = pl.getLines().values().iterator();
      IpPrefixListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLine(), equalTo(3L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.0.3.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.0.1.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getLine(), equalTo(10L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.0.2.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLine(), equalTo(15L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.0.4.0/24")));
    }
    {
      IpPrefixList pl = vc.getIpPrefixLists().get("pl_range");
      assertThat(pl.getDescription(), nullValue());
      assertThat(pl.getLines().keySet(), contains(5L, 10L, 15L, 20L, 25L));
      Iterator<IpPrefixListLine> lines = pl.getLines().values().iterator();
      IpPrefixListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(16, Prefix.MAX_PREFIX_LENGTH)));
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(24, 24)));
      assertThat(line.getLine(), equalTo(10L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(8, Prefix.MAX_PREFIX_LENGTH)));
      assertThat(line.getLine(), equalTo(15L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(20, 24)));
      assertThat(line.getLine(), equalTo(20L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(16, 24)));
      assertThat(line.getLine(), equalTo(25L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));
    }
  }

  @Test
  public void testIpv6AccessListParsing() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_ipv6_access_list"), notNullValue());
  }

  @Test
  public void testNveExtraction() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_nve");
    Map<Integer, Nve> nves = vc.getNves();
    assertThat(nves, hasKeys(1, 2, 3, 4));
    {
      Nve nve = nves.get(1);
      assertFalse(nve.isShutdown());
      assertThat(nve.getSourceInterface(), equalTo("loopback0"));
      assertThat(nve.getGlobalIngressReplicationProtocol(), nullValue());
      assertTrue(nve.isGlobalSuppressArp());
      assertThat(nve.getHostReachabilityProtocol(), equalTo(HostReachabilityProtocol.BGP));
      assertThat(nve.getMulticastGroupL2(), equalTo(Ip.parse("233.0.0.0")));
      assertThat(nve.getMulticastGroupL3(), equalTo(Ip.parse("234.0.0.0")));
      int vni = 10001;
      assertThat(nve.getMemberVnis(), hasKeys(vni));
      NveVni vniConfig = nve.getMemberVni(vni);
      assertThat(vniConfig.getVni(), equalTo(vni));
      assertThat(vniConfig.getSuppressArp(), nullValue());
      assertThat(
          vniConfig.getIngressReplicationProtocol(), equalTo(IngressReplicationProtocol.BGP));
      assertThat(vniConfig.getMcastGroup(), equalTo(Ip.parse("235.0.0.0")));
    }
    {
      Nve nve = nves.get(2);
      assertFalse(nve.isShutdown());
      assertThat(nve.getSourceInterface(), equalTo("loopback0"));
      assertThat(
          nve.getGlobalIngressReplicationProtocol(), equalTo(IngressReplicationProtocol.BGP));
      assertTrue(nve.isGlobalSuppressArp());
      assertThat(nve.getHostReachabilityProtocol(), equalTo(HostReachabilityProtocol.BGP));
      int vni = 20001;
      assertThat(nve.getMemberVnis(), hasKeys(vni));
      NveVni vniConfig = nve.getMemberVni(vni);
      assertThat(vniConfig.getVni(), equalTo(vni));
      assertThat(vniConfig.getSuppressArp(), equalTo(Boolean.FALSE));
      assertThat(vniConfig.getIngressReplicationProtocol(), nullValue());
      assertThat(vniConfig.getMcastGroup(), nullValue());
      assertThat(nve.getMulticastGroupL2(), nullValue());
      assertThat(nve.getMulticastGroupL3(), nullValue());
    }
    {
      Nve nve = nves.get(3);
      assertTrue(nve.isShutdown());
      assertThat(nve.getSourceInterface(), nullValue());
      assertThat(nve.getGlobalIngressReplicationProtocol(), nullValue());
      assertFalse(nve.isGlobalSuppressArp());
      assertThat(nve.getHostReachabilityProtocol(), nullValue());
      assertThat(nve.getMulticastGroupL2(), nullValue());
      assertThat(nve.getMulticastGroupL3(), nullValue());
      int vni = 30001;
      assertThat(nve.getMemberVnis(), hasKeys(vni));
      NveVni vniConfig = nve.getMemberVni(vni);
      assertThat(vniConfig.getSuppressArp(), nullValue());
      assertThat(
          vniConfig.getIngressReplicationProtocol(), equalTo(IngressReplicationProtocol.STATIC));
      assertThat(vniConfig.getMcastGroup(), nullValue());
    }
    {
      Nve nve = nves.get(4);
      assertTrue(nve.isShutdown());
      assertThat(nve.getSourceInterface(), equalTo("loopback4"));
      assertThat(nve.getGlobalIngressReplicationProtocol(), nullValue());
      assertFalse(nve.isGlobalSuppressArp());
      assertThat(nve.getMulticastGroupL2(), nullValue());
      assertThat(nve.getMulticastGroupL3(), nullValue());
      int vni = 40001;
      assertThat(nve.getMemberVnis(), hasKeys(vni));
      NveVni vniConfig = nve.getMemberVni(vni);
      assertThat(vniConfig.getSuppressArp(), nullValue());
      assertThat(
          vniConfig.getIngressReplicationProtocol(), equalTo(IngressReplicationProtocol.STATIC));
      assertThat(vniConfig.getMcastGroup(), nullValue());
      assertThat(vniConfig.getPeerIps(), equalTo(ImmutableSet.of(Ip.parse("4.0.0.1"))));
    }
  }

  @Test
  public void testNveVnisConversion() throws IOException {
    Configuration c = parseConfig("nxos_nve_vnis");

    assertThat(c, hasDefaultVrf(hasVniSettings(hasKey(10001))));
    assertThat(
        c.getDefaultVrf().getVniSettings().get(10001),
        allOf(
            hasBumTransportIps(equalTo(ImmutableSortedSet.of(Ip.parse("235.0.0.0")))),
            hasBumTransportMethod(equalTo(BumTransportMethod.MULTICAST_GROUP)),
            hasSourceAddress(nullValue()),
            hasUdpPort(equalTo(DEFAULT_UDP_PORT)),
            VniSettingsMatchers.hasVlan(equalTo(2)),
            hasVni(10001)));

    assertThat(c, hasDefaultVrf(hasVniSettings(hasKey(20001))));
    assertThat(
        c.getDefaultVrf().getVniSettings().get(20001),
        allOf(
            // L3 mcast IP
            hasBumTransportIps(equalTo(ImmutableSortedSet.of(Ip.parse("234.0.0.0")))),
            hasBumTransportMethod(equalTo(BumTransportMethod.MULTICAST_GROUP)),
            hasSourceAddress(nullValue()),
            hasUdpPort(equalTo(DEFAULT_UDP_PORT)),
            VniSettingsMatchers.hasVlan(equalTo(3)),
            hasVni(20001)));

    assertThat(c, hasDefaultVrf(hasVniSettings(hasKey(30001))));
    assertThat(
        c.getDefaultVrf().getVniSettings().get(30001),
        allOf(
            // L2 mcast IP
            hasBumTransportIps(equalTo(ImmutableSortedSet.of(Ip.parse("233.0.0.0")))),
            hasBumTransportMethod(equalTo(BumTransportMethod.MULTICAST_GROUP)),
            hasSourceAddress(nullValue()),
            hasUdpPort(equalTo(DEFAULT_UDP_PORT)),
            VniSettingsMatchers.hasVlan(equalTo(4)),
            hasVni(30001)));

    assertThat(c, hasDefaultVrf(hasVniSettings(hasKey(40001))));
    assertThat(
        c.getDefaultVrf().getVniSettings().get(40001),
        allOf(
            hasBumTransportIps(equalTo(ImmutableSortedSet.of(Ip.parse("4.0.0.1")))),
            hasBumTransportMethod(equalTo(BumTransportMethod.UNICAST_FLOOD_GROUP)),
            hasSourceAddress(equalTo(Ip.parse("1.1.1.1"))),
            hasUdpPort(equalTo(DEFAULT_UDP_PORT)),
            VniSettingsMatchers.hasVlan(equalTo(5)),
            hasVni(40001)));

    // VLAN for VNI 500001 is shutdown
    assertThat(c, not(hasDefaultVrf(hasVniSettings(hasKey(50001)))));
  }

  @Test
  public void testObjectGroupIpAddressExtraction() {
    String hostname = "nxos_object_group_ip_address";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getObjectGroups(), hasKeys("og_indices", "og_syntax"));
    {
      ObjectGroupIpAddress group = (ObjectGroupIpAddress) vc.getObjectGroups().get("og_indices");
      assertThat(group.getLines(), hasKeys(10L, 13L, 15L, 25L));
      assertThat(
          group.getLines().values().stream()
              .map(ObjectGroupIpAddressLine::getLine)
              .collect(ImmutableList.toImmutableList()),
          contains(10L, 13L, 15L, 25L));
    }
    {
      ObjectGroupIpAddress group = (ObjectGroupIpAddress) vc.getObjectGroups().get("og_syntax");
      Iterator<ObjectGroupIpAddressLine> lines = group.getLines().values().iterator();
      ObjectGroupIpAddressLine line;

      line = lines.next();
      assertThat(line.getIpWildcard(), equalTo(IpWildcard.create(Ip.parse("10.0.0.1"))));

      line = lines.next();
      assertThat(
          line.getIpWildcard(),
          equalTo(ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.255.0.255"))));

      line = lines.next();
      assertThat(line.getIpWildcard(), equalTo(IpWildcard.create(Prefix.parse("10.0.0.0/24"))));
    }
  }

  @Test
  public void testObjectGroupIpAddressReferences() throws IOException {
    String hostname = "nxos_object_group_ip_address_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ans, hasNumReferrers(filename, OBJECT_GROUP_IP_ADDRESS, "og_used", 2));
    assertThat(ans, hasNumReferrers(filename, OBJECT_GROUP_IP_ADDRESS, "og_unused", 0));
    assertThat(ans, hasUndefinedReference(filename, OBJECT_GROUP_IP_ADDRESS, "og_undefined"));
  }

  @Test
  public void testObjectGroupIpAddressConversion() throws IOException {
    String hostname = "nxos_object_group_ip_address";
    Configuration c = parseConfig(hostname);

    assertThat(c.getIpSpaces(), hasKeys("og_indices", "og_syntax"));
    {
      IpSpace ipSpace = c.getIpSpaces().get("og_syntax");
      assertThat(
          ipSpace.accept(SRC_IP_BDD),
          equalTo(
              AclIpSpace.union(
                      Ip.parse("10.0.0.1").toIpSpace(),
                      ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.255.0.255")).toIpSpace(),
                      Prefix.parse("10.0.0.0/24").toIpSpace())
                  .accept(SRC_IP_BDD)));
    }
  }

  @Test
  public void testOspfConversion() throws IOException {
    String hostname = "nxos_ospf";
    Configuration c = parseConfig(hostname);
    org.batfish.datamodel.Vrf defaultVrf = c.getDefaultVrf();

    assertThat(
        defaultVrf.getOspfProcesses(),
        hasKeys(
            "a_auth",
            "a_auth_m",
            "a_default_cost",
            "a_filter_list",
            "a_nssa",
            "a_nssa_no_r",
            "a_nssa_no_s",
            "a_nssa_rm",
            "a_r",
            "a_r_cost",
            "a_r_not_advertise",
            "a_stub",
            "a_stub_no_summary",
            "a_virtual_link",
            "auto_cost",
            "auto_cost_m",
            "auto_cost_g",
            "bfd",
            "dio",
            "dio_always",
            "dio_route_map",
            "dio_always_route_map",
            "lac",
            "lac_detail",
            "mm",
            "mm_external_lsa",
            "mm_external_lsa_m",
            "mm_include_stub",
            "mm_on_startup",
            "mm_on_startup_t",
            "mm_on_startup_w",
            "mm_on_startup_tw",
            "mm_summary_lsa",
            "mm_summary_lsa_m",
            "network",
            "pi_d",
            "r_direct",
            "r_mp",
            "r_mp_t",
            "r_mp_warn",
            "r_mp_withdraw",
            "r_mp_withdraw_n",
            "r_static",
            "router_id",
            "sa",
            "timers",
            "with_vrf"));
    // TODO: convert and test "a_auth" - OSPF area authentication
    // TODO: convert and test "a_auth_m" - OSPF area authentication using message-digest
    // TODO: convert and test "a_default_cost" - OSPF area default-cost
    // TODO: convert and test "a_filter_list" - OSPF area input/output filter-list (route-map)
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("a_nssa");
      assertThat(proc, hasArea(1L, hasNssa(notNullValue())));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("a_nssa_no_r");
      assertThat(proc, hasArea(1L, hasNssa(hasSuppressType7(true))));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("a_nssa_no_s");
      assertThat(proc, hasArea(1L, hasNssa(NssaSettingsMatchers.hasSuppressType3())));
    }
    // TODO: convert and test "a_nssa_rm" - OSPF NSSA with route-map
    // TODO: convert and test "a_r" - OSPF area range
    // TODO: convert and test "a_r_cost" - OSPF area range-specific cost
    // TODO: convert and test "a_r_not_advertise" - OSPF area range advertisement suppression
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("a_stub");
      assertThat(proc, hasArea(1L, hasStub(notNullValue())));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("a_stub_no_summary");
      assertThat(proc, hasArea(1L, hasStub(StubSettingsMatchers.hasSuppressType3())));
    }
    // TODO: convert and test "a_virtual_link" - OSPF area virtual-link
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("auto_cost");
      assertThat(proc.getReferenceBandwidth(), equalTo(1E9D));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("auto_cost_m");
      assertThat(proc.getReferenceBandwidth(), equalTo(2E9D));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("auto_cost_g");
      assertThat(proc.getReferenceBandwidth(), equalTo(3E12D));
    }
    // TODO: convert and test "bfd" - OSPF bfd
    {
      // common routes for default-originate tests
      org.batfish.datamodel.StaticRoute staticInputRoute =
          org.batfish.datamodel.StaticRoute.builder()
              .setAdmin(1)
              .setNetwork(Prefix.ZERO)
              .setNextHopInterface(org.batfish.datamodel.Interface.NULL_INTERFACE_NAME)
              .build();
      org.batfish.datamodel.GeneratedRoute generatedInputRoute =
          org.batfish.datamodel.GeneratedRoute.builder()
              .setNetwork(Prefix.ZERO)
              .setNextHopInterface(org.batfish.datamodel.Interface.NULL_INTERFACE_NAME)
              .build();
      {
        org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("dio");
        // should not generate route
        assertThat(proc.getGeneratedRoutes(), empty());
        OspfExternalRoute.Builder outputRoute = ospfExternalRouteBuilder().setNetwork(Prefix.ZERO);
        // accept main-RIB route
        assertTrue(
            c.getRoutingPolicies()
                .get(proc.getExportPolicy())
                .process(
                    staticInputRoute,
                    outputRoute,
                    Ip.ZERO,
                    Configuration.DEFAULT_VRF_NAME,
                    Direction.OUT));
        assertThat(outputRoute.build().getOspfMetricType(), equalTo(OspfMetricType.E1));
      }
      {
        org.batfish.datamodel.ospf.OspfProcess proc =
            defaultVrf.getOspfProcesses().get("dio_always");
        // should have generated route
        assertThat(proc.getGeneratedRoutes(), contains(allOf(hasPrefix(Prefix.ZERO))));
        OspfExternalRoute.Builder outputRoute = ospfExternalRouteBuilder().setNetwork(Prefix.ZERO);
        // reject main-RIB route
        assertFalse(
            c.getRoutingPolicies()
                .get(proc.getExportPolicy())
                .process(
                    staticInputRoute,
                    OspfExternalRoute.builder(),
                    Ip.ZERO,
                    Configuration.DEFAULT_VRF_NAME,
                    Direction.OUT));
        // accept generated route
        assertTrue(
            c.getRoutingPolicies()
                .get(proc.getExportPolicy())
                .process(
                    generatedInputRoute,
                    outputRoute,
                    Ip.ZERO,
                    Configuration.DEFAULT_VRF_NAME,
                    Direction.OUT));
        assertThat(outputRoute.build().getOspfMetricType(), equalTo(OspfMetricType.E1));
      }
      {
        org.batfish.datamodel.ospf.OspfProcess proc =
            defaultVrf.getOspfProcesses().get("dio_route_map");
        // should not generate route
        assertThat(proc.getGeneratedRoutes(), empty());
        OspfExternalRoute.Builder outputRoute = ospfExternalRouteBuilder().setNetwork(Prefix.ZERO);
        // accept main-RIB route
        assertTrue(
            c.getRoutingPolicies()
                .get(proc.getExportPolicy())
                .process(
                    staticInputRoute,
                    outputRoute,
                    Ip.ZERO,
                    Configuration.DEFAULT_VRF_NAME,
                    Direction.OUT));
        // assign E2 metric-type from route-map
        assertThat(outputRoute.build().getOspfMetricType(), equalTo(OspfMetricType.E2));
      }
      {
        org.batfish.datamodel.ospf.OspfProcess proc =
            defaultVrf.getOspfProcesses().get("dio_always_route_map");
        // should have generated route
        assertThat(proc.getGeneratedRoutes(), contains(allOf(hasPrefix(Prefix.ZERO))));
        OspfExternalRoute.Builder outputRoute = ospfExternalRouteBuilder().setNetwork(Prefix.ZERO);
        // reject main-RIB route
        assertFalse(
            c.getRoutingPolicies()
                .get(proc.getExportPolicy())
                .process(
                    staticInputRoute,
                    OspfExternalRoute.builder(),
                    Ip.ZERO,
                    Configuration.DEFAULT_VRF_NAME,
                    Direction.OUT));
        // accept generated route
        assertTrue(
            c.getRoutingPolicies()
                .get(proc.getExportPolicy())
                .process(
                    generatedInputRoute,
                    outputRoute,
                    Ip.ZERO,
                    Configuration.DEFAULT_VRF_NAME,
                    Direction.OUT));
        // assign E2 metric-type from route-map
        assertThat(outputRoute.build().getOspfMetricType(), equalTo(OspfMetricType.E2));
      }
    }
    // TODO: convert and test "lac" - OSPF log-adjacency-changes
    // TODO: convert and test "lac_detail" - OSPF log-adjacency-changes detail
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("mm");
      assertThat(proc.getMaxMetricTransitLinks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("mm_external_lsa");
      assertThat(proc.getMaxMetricTransitLinks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
      assertThat(proc.getMaxMetricExternalNetworks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("mm_external_lsa_m");
      assertThat(proc.getMaxMetricTransitLinks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
      assertThat(proc.getMaxMetricExternalNetworks(), equalTo(123L));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("mm_include_stub");
      assertThat(proc.getMaxMetricTransitLinks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
      assertThat(proc.getMaxMetricStubNetworks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("mm_include_stub");
      assertThat(proc.getMaxMetricTransitLinks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
      assertThat(proc.getMaxMetricStubNetworks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
    }
    // ignore on-startup settings
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("mm_summary_lsa");
      assertThat(proc.getMaxMetricTransitLinks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
      assertThat(proc.getMaxMetricSummaryNetworks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("mm_summary_lsa_m");
      assertThat(proc.getMaxMetricTransitLinks(), equalTo((long) DEFAULT_OSPF_MAX_METRIC));
      assertThat(proc.getMaxMetricSummaryNetworks(), equalTo(456L));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("network");
      assertThat(
          proc,
          hasArea(
              0, OspfAreaMatchers.hasInterfaces(containsInAnyOrder("Ethernet1/2", "Ethernet1/3"))));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("pi_d");
      assertThat(
          proc, hasArea(0, OspfAreaMatchers.hasInterfaces(containsInAnyOrder("Ethernet1/1"))));
      // passivity tested in interfaces section
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("r_direct");
      ConnectedRoute inputRoute = new ConnectedRoute(Prefix.parse("1.2.3.4/32"), "dummy2");
      OspfExternalRoute.Builder outputRoute =
          ospfExternalRouteBuilder().setNetwork(Prefix.parse("1.2.3.4/32"));
      assertTrue(
          c.getRoutingPolicies()
              .get(proc.getExportPolicy())
              .process(
                  inputRoute, outputRoute, Ip.ZERO, Configuration.DEFAULT_VRF_NAME, Direction.OUT));
      assertThat(outputRoute.build().getOspfMetricType(), equalTo(OspfMetricType.E1));
    }
    // TODO: convert and test OSPF redistribute maximum-prefix
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("r_static");
      // can redistribute static default route on NX-OS
      org.batfish.datamodel.StaticRoute inputRoute =
          org.batfish.datamodel.StaticRoute.builder()
              .setAdmin(1)
              .setNetwork(Prefix.ZERO)
              .setNextHopInterface("dummy")
              .build();
      OspfExternalRoute.Builder outputRoute = ospfExternalRouteBuilder().setNetwork(Prefix.ZERO);
      assertTrue(
          c.getRoutingPolicies()
              .get(proc.getExportPolicy())
              .process(
                  inputRoute, outputRoute, Ip.ZERO, Configuration.DEFAULT_VRF_NAME, Direction.OUT));
      assertThat(outputRoute.build().getOspfMetricType(), equalTo(OspfMetricType.E1));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("router_id");
      assertThat(proc.getRouterId(), equalTo(Ip.parse("192.0.2.1")));
      assertThat(proc, hasArea(0L, OspfAreaMatchers.hasInterfaces(contains("Ethernet1/4"))));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("sa");
      Prefix p0 = Prefix.parse("192.168.0.0/24");
      Prefix p1 = Prefix.create(Ip.parse("192.168.1.0"), Ip.parse("255.255.255.0"));
      Prefix p2 = Prefix.create(Ip.parse("192.168.2.0"), Ip.parse("255.255.255.0"));
      Map<Prefix, OspfAreaSummary> summaries = proc.getAreas().get(0L).getSummaries();

      assertThat(summaries, hasKeys(p0, p1, p2));
      assertTrue(summaries.get(p0).getAdvertised());
      assertFalse(summaries.get(p1).getAdvertised());
      assertTrue(summaries.get(p2).getAdvertised());
      // TODO: convert and test tags
    }
    // TODO: convert and test OSPF timers
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("with_vrf");
      assertThat(proc.getAreas(), hasKeys(0L));
      assertThat(c.getVrfs().get("v1").getOspfProcesses(), hasKeys("with_vrf"));
      org.batfish.datamodel.ospf.OspfProcess vrfProc =
          c.getVrfs().get("v1").getOspfProcesses().get("with_vrf");
      assertThat(vrfProc.getAreas(), hasKeys(1L));
    }

    assertThat(
        c.getAllInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3", "Ethernet1/4"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(iface.getOspfCost(), equalTo(12));
      assertTrue(iface.getOspfEnabled());
      assertThat(iface.getOspfAreaName(), equalTo(0L));
      // TODO: convert and test bfd
      assertTrue(iface.getOspfPassive());
      assertFalse(iface.getOspfPointToPoint());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertTrue(iface.getOspfEnabled());
      assertThat(iface.getOspfAreaName(), equalTo(0L));
      assertFalse(iface.getOspfPassive());
      assertFalse(iface.getOspfPointToPoint());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      assertTrue(iface.getOspfEnabled());
      assertThat(iface.getOspfAreaName(), equalTo(0L));
      assertFalse(iface.getOspfPassive());
      assertTrue(iface.getOspfPointToPoint());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertTrue(iface.getOspfEnabled());
      assertThat(iface.getOspfAreaName(), equalTo(0L));
      assertTrue(iface.getOspfPassive());
      assertFalse(iface.getOspfPointToPoint());
    }
  }

  @Test
  public void testOspfExtraction() {
    String hostname = "nxos_ospf";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getOspfProcesses(),
        hasKeys(
            "a_auth",
            "a_auth_m",
            "a_default_cost",
            "a_filter_list",
            "a_nssa",
            "a_nssa_no_r",
            "a_nssa_no_s",
            "a_nssa_rm",
            "a_r",
            "a_r_cost",
            "a_r_not_advertise",
            "a_stub",
            "a_stub_no_summary",
            "a_virtual_link",
            "auto_cost",
            "auto_cost_m",
            "auto_cost_g",
            "bfd",
            "dio",
            "dio_always",
            "dio_route_map",
            "dio_always_route_map",
            "lac",
            "lac_detail",
            "mm",
            "mm_external_lsa",
            "mm_external_lsa_m",
            "mm_include_stub",
            "mm_on_startup",
            "mm_on_startup_t",
            "mm_on_startup_w",
            "mm_on_startup_tw",
            "mm_summary_lsa",
            "mm_summary_lsa_m",
            "network",
            "pi_d",
            "r_direct",
            "r_mp",
            "r_mp_t",
            "r_mp_warn",
            "r_mp_withdraw",
            "r_mp_withdraw_n",
            "r_static",
            "router_id",
            "sa",
            "timers",
            "with_vrf"));
    {
      DefaultVrfOspfProcess proc = vc.getOspfProcesses().get("a_auth");
      assertThat(proc.getName(), equalTo("a_auth"));
      assertThat(proc.getAreas(), hasKeys(0L));
      OspfArea area = proc.getAreas().get(0L);
      assertThat(area.getId(), equalTo(0L));
      assertThat(area.getAuthentication(), equalTo(OspfAreaAuthentication.SIMPLE));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_auth_m");
      assertThat(proc.getAreas(), hasKeys(0L));
      OspfArea area = proc.getAreas().get(0L);
      assertThat(area.getAuthentication(), equalTo(OspfAreaAuthentication.MESSAGE_DIGEST));

      // check default for next test
      assertThat(area.getDefaultCost(), equalTo(OspfArea.DEFAULT_DEFAULT_COST));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_default_cost");
      assertThat(proc.getAreas(), hasKeys(0L));
      OspfArea area = proc.getAreas().get(0L);
      assertThat(area.getDefaultCost(), equalTo(10));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_filter_list");
      assertThat(proc.getAreas(), hasKeys(0L));
      OspfArea area = proc.getAreas().get(0L);
      assertThat(area.getFilterListIn(), equalTo("rm1"));
      assertThat(area.getFilterListOut(), equalTo("rm2"));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_nssa");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaNssa nssa = (OspfAreaNssa) proc.getAreas().get(1L).getTypeSettings();
      assertThat(nssa, notNullValue());
      assertFalse(nssa.getNoRedistribution());
      assertFalse(nssa.getNoSummary());
      assertThat(nssa.getRouteMap(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_nssa_no_r");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaNssa nssa = (OspfAreaNssa) proc.getAreas().get(1L).getTypeSettings();
      assertThat(nssa, notNullValue());
      assertTrue(nssa.getNoRedistribution());
      assertFalse(nssa.getNoSummary());
      assertThat(nssa.getRouteMap(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_nssa_no_s");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaNssa nssa = (OspfAreaNssa) proc.getAreas().get(1L).getTypeSettings();
      assertThat(nssa, notNullValue());
      assertFalse(nssa.getNoRedistribution());
      assertTrue(nssa.getNoSummary());
      assertThat(nssa.getRouteMap(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_nssa_rm");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaNssa nssa = (OspfAreaNssa) proc.getAreas().get(1L).getTypeSettings();
      assertThat(nssa, notNullValue());
      assertFalse(nssa.getNoRedistribution());
      assertFalse(nssa.getNoSummary());
      assertThat(nssa.getRouteMap(), equalTo("rm1"));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_r");
      assertThat(proc.getAreas(), hasKeys(0L));
      OspfArea area = proc.getAreas().get(0L);
      Prefix prefix = Prefix.parse("1.1.1.1/32");
      assertThat(area.getRanges(), hasKeys(prefix));
      OspfAreaRange range = area.getRanges().get(prefix);
      assertThat(range.getCost(), nullValue());
      assertFalse(range.getNotAdvertise());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_r_cost");
      assertThat(proc.getAreas(), hasKeys(0L));
      OspfArea area = proc.getAreas().get(0L);
      Prefix prefix = Prefix.parse("1.1.1.1/32");
      assertThat(area.getRanges(), hasKeys(prefix));
      OspfAreaRange range = area.getRanges().get(prefix);
      assertThat(range.getCost(), equalTo(5));
      assertFalse(range.getNotAdvertise());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_r_not_advertise");
      assertThat(proc.getAreas(), hasKeys(0L));
      OspfArea area = proc.getAreas().get(0L);
      Prefix prefix = Prefix.parse("1.1.1.1/32");
      assertThat(area.getRanges(), hasKeys(prefix));
      OspfAreaRange range = area.getRanges().get(prefix);
      assertThat(range.getCost(), nullValue());
      assertTrue(range.getNotAdvertise());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_stub");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaStub stub = (OspfAreaStub) proc.getAreas().get(1L).getTypeSettings();
      assertThat(stub, notNullValue());
      assertFalse(stub.getNoSummary());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_stub_no_summary");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaStub stub = (OspfAreaStub) proc.getAreas().get(1L).getTypeSettings();
      assertThat(stub, notNullValue());
      assertTrue(stub.getNoSummary());

      // check default for next test
      assertThat(
          proc.getAutoCostReferenceBandwidthMbps(),
          equalTo(DEFAULT_AUTO_COST_REFERENCE_BANDWIDTH_MBPS));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("auto_cost");
      assertThat(proc.getAutoCostReferenceBandwidthMbps(), equalTo(1_000));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("auto_cost_m");
      assertThat(proc.getAutoCostReferenceBandwidthMbps(), equalTo(2_000));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("auto_cost_g");
      assertThat(proc.getAutoCostReferenceBandwidthMbps(), equalTo(3_000_000));
      assertFalse(proc.getBfd());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("bfd");
      assertTrue(proc.getBfd());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("dio");
      OspfDefaultOriginate defaultOriginate = proc.getDefaultOriginate();
      assertFalse(defaultOriginate.getAlways());
      assertThat(defaultOriginate.getRouteMap(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("dio_always");
      OspfDefaultOriginate defaultOriginate = proc.getDefaultOriginate();
      assertTrue(defaultOriginate.getAlways());
      assertThat(defaultOriginate.getRouteMap(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("dio_route_map");
      OspfDefaultOriginate defaultOriginate = proc.getDefaultOriginate();
      assertFalse(defaultOriginate.getAlways());
      assertThat(defaultOriginate.getRouteMap(), equalTo("rm_e2"));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("dio_always_route_map");
      OspfDefaultOriginate defaultOriginate = proc.getDefaultOriginate();
      assertTrue(defaultOriginate.getAlways());
      assertThat(defaultOriginate.getRouteMap(), equalTo("rm_e2"));
      assertThat(proc.getMaxMetricRouterLsa(), nullValue());
    }
    // TODO: extract and test log-adjacency-changes
    {
      OspfProcess proc = vc.getOspfProcesses().get("mm");
      OspfMaxMetricRouterLsa mm = proc.getMaxMetricRouterLsa();
      assertThat(mm.getExternalLsa(), nullValue());
      assertFalse(mm.getIncludeStub());
      assertThat(mm.getSummaryLsa(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("mm_external_lsa");
      OspfMaxMetricRouterLsa mm = proc.getMaxMetricRouterLsa();
      assertThat(mm.getExternalLsa(), equalTo(OspfMaxMetricRouterLsa.DEFAULT_OSPF_MAX_METRIC));
      assertFalse(mm.getIncludeStub());
      assertThat(mm.getSummaryLsa(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("mm_external_lsa_m");
      OspfMaxMetricRouterLsa mm = proc.getMaxMetricRouterLsa();
      assertThat(mm.getExternalLsa(), equalTo(123));
      assertFalse(mm.getIncludeStub());
      assertThat(mm.getSummaryLsa(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("mm_include_stub");
      OspfMaxMetricRouterLsa mm = proc.getMaxMetricRouterLsa();
      assertThat(mm.getExternalLsa(), nullValue());
      assertTrue(mm.getIncludeStub());
      assertThat(mm.getSummaryLsa(), nullValue());
    }
    // TODO: record on-startup
    {
      OspfProcess proc = vc.getOspfProcesses().get("mm_summary_lsa");
      OspfMaxMetricRouterLsa mm = proc.getMaxMetricRouterLsa();
      assertThat(mm.getExternalLsa(), nullValue());
      assertFalse(mm.getIncludeStub());
      assertThat(mm.getSummaryLsa(), equalTo(OspfMaxMetricRouterLsa.DEFAULT_OSPF_MAX_METRIC));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("mm_summary_lsa_m");
      OspfMaxMetricRouterLsa mm = proc.getMaxMetricRouterLsa();
      assertThat(mm.getExternalLsa(), nullValue());
      assertFalse(mm.getIncludeStub());
      assertThat(mm.getSummaryLsa(), equalTo(456));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("network");
      assertThat(
          proc.getNetworks(),
          equalTo(
              ImmutableMap.of(
                  IpWildcard.ipWithWildcardMask(Ip.parse("192.168.0.0"), Ip.parse("0.0.255.255")),
                  0L,
                  IpWildcard.create(Prefix.strict("172.16.0.0/24")),
                  0L)));
      assertFalse(proc.getPassiveInterfaceDefault());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("pi_d");
      assertTrue(proc.getPassiveInterfaceDefault());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("r_direct");
      assertThat(proc.getRedistributeDirectRouteMap(), equalTo("rm1"));
    }
    // TODO: extract and test redistribute maximum-prefix
    {
      OspfProcess proc = vc.getOspfProcesses().get("r_static");
      assertThat(proc.getRedistributeStaticRouteMap(), equalTo("rm1"));
      assertThat(proc.getRouterId(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("router_id");
      assertThat(proc.getRouterId(), equalTo(Ip.parse("192.0.2.1")));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("sa");
      assertThat(
          proc.getSummaryAddresses(),
          hasKeys(
              Prefix.strict("192.168.0.0/24"),
              Prefix.strict("192.168.1.0/24"),
              Prefix.strict("192.168.2.0/24")));
      OspfSummaryAddress sa;

      sa = proc.getSummaryAddresses().get(Prefix.strict("192.168.0.0/24"));
      assertFalse(sa.getNotAdvertise());
      assertThat(sa.getTag(), equalTo(0L));

      sa = proc.getSummaryAddresses().get(Prefix.strict("192.168.1.0/24"));
      assertTrue(sa.getNotAdvertise());
      assertThat(sa.getTag(), equalTo(0L));

      sa = proc.getSummaryAddresses().get(Prefix.strict("192.168.2.0/24"));
      assertFalse(sa.getNotAdvertise());
      assertThat(sa.getTag(), equalTo(5L));

      // check defaults for next test
      assertThat(proc.getTimersLsaArrival(), equalTo(DEFAULT_TIMERS_LSA_ARRIVAL_MS));
      assertThat(proc.getTimersLsaGroupPacing(), equalTo(DEFAULT_TIMERS_LSA_GROUP_PACING_S));
      assertThat(
          proc.getTimersLsaStartInterval(), equalTo(DEFAULT_TIMERS_THROTTLE_LSA_START_INTERVAL_MS));
      assertThat(
          proc.getTimersLsaHoldInterval(), equalTo(DEFAULT_TIMERS_THROTTLE_LSA_HOLD_INTERVAL_MS));
      assertThat(
          proc.getTimersLsaMaxInterval(), equalTo(DEFAULT_TIMERS_THROTTLE_LSA_MAX_INTERVAL_MS));
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("timers");
      assertThat(proc.getTimersLsaArrival(), equalTo(10));
      assertThat(proc.getTimersLsaGroupPacing(), equalTo(15));
      assertThat(proc.getTimersLsaStartInterval(), equalTo(111));
      assertThat(proc.getTimersLsaHoldInterval(), equalTo(222));
      assertThat(proc.getTimersLsaMaxInterval(), equalTo(333));
    }
    {
      DefaultVrfOspfProcess proc = vc.getOspfProcesses().get("with_vrf");
      assertThat(proc.getAreas(), hasKeys(0L));
      assertThat(proc.getVrfs(), hasKeys("v1"));
      assertThat(proc.getVrfs().get("v1").getAreas(), hasKeys(1L));
    }

    assertThat(
        vc.getInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3", "Ethernet1/4"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      OspfInterface ospf = iface.getOspf();
      assertThat(ospf, notNullValue());
      // TODO: extract and test message-digest-key
      assertTrue(ospf.getBfd());
      assertThat(ospf.getCost(), equalTo(12));
      assertThat(ospf.getDeadIntervalS(), equalTo(10));
      assertThat(ospf.getHelloIntervalS(), equalTo(20));
      assertThat(ospf.getPassive(), nullValue());
      assertThat(ospf.getProcess(), equalTo("pi_d"));
      assertThat(ospf.getArea(), equalTo(0L));
      assertThat(ospf.getNetwork(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      OspfInterface ospf = iface.getOspf();
      assertThat(ospf, notNullValue());
      assertFalse(ospf.getBfd());
      assertThat(ospf.getCost(), nullValue());
      assertThat(ospf.getDeadIntervalS(), equalTo(DEFAULT_DEAD_INTERVAL_S));
      assertThat(ospf.getHelloIntervalS(), equalTo(DEFAULT_HELLO_INTERVAL_S));
      assertThat(ospf.getPassive(), nullValue());
      assertThat(ospf.getProcess(), nullValue());
      assertThat(ospf.getArea(), nullValue());
      assertThat(ospf.getNetwork(), equalTo(BROADCAST));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      OspfInterface ospf = iface.getOspf();
      assertThat(ospf, notNullValue());
      assertThat(ospf.getPassive(), nullValue());
      assertThat(ospf.getNetwork(), equalTo(POINT_TO_POINT));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      OspfInterface ospf = iface.getOspf();
      assertThat(ospf, notNullValue());
      assertThat(ospf.getPassive(), equalTo(true));
      assertThat(ospf.getNetwork(), nullValue());
    }
  }

  @Test
  public void testPolicyMapParsing() {
    // TODO: make into an extraction test
    assertThat(parseVendorConfig("nxos_policy_map"), notNullValue());
  }

  @Test
  public void testPortChannelConversion() throws IOException {
    String hostname = "nxos_port_channel";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces(),
        hasKeys(
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8",
            "Ethernet1/9",
            "Ethernet1/10",
            "Ethernet1/11",
            "Ethernet1/12",
            "Ethernet1/13",
            "Ethernet1/14",
            "port-channel1",
            "port-channel2",
            "port-channel3",
            "port-channel4",
            "port-channel5",
            "port-channel6",
            "port-channel7"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertThat(iface, isActive(false));
      assertThat(iface, hasChannelGroup("port-channel2"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel3"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel3"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive(false));
      assertThat(iface, hasChannelGroup("port-channel3"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/6");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel4"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/7");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel4"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/8");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel5"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/9");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel5"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/10");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel6"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/11");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel6"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    // TODO: conversion for channel-group mode for Ethernet1/12-1/14
    // TOOD: convert and test lacp settings
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel1");
      assertThat(iface, isActive(false));
      assertThat(iface, hasChannelGroupMembers(empty()));
      assertThat(iface, hasDependencies(empty()));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel2");
      assertThat(iface, isActive(false));
      assertThat(iface, hasChannelGroupMembers(contains("Ethernet1/2")));
      assertThat(
          iface,
          hasDependencies(contains(new Dependency("Ethernet1/2", DependencyType.AGGREGATE))));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel3");
      assertThat(iface, isActive(true));
      assertThat(
          iface, hasChannelGroupMembers(contains("Ethernet1/3", "Ethernet1/4", "Ethernet1/5")));
      assertThat(
          iface,
          hasDependencies(
              containsInAnyOrder(
                  new Dependency("Ethernet1/3", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/4", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/5", DependencyType.AGGREGATE))));
      assertThat(iface, hasBandwidth(200E6));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel4");
      assertThat(iface, isActive(true));
      assertThat(iface, hasChannelGroupMembers(contains("Ethernet1/6", "Ethernet1/7")));
      assertThat(
          iface,
          hasDependencies(
              containsInAnyOrder(
                  new Dependency("Ethernet1/6", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/7", DependencyType.AGGREGATE))));
      assertThat(iface, hasBandwidth(200E6));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel5");
      assertThat(iface, isActive(true));
      assertThat(iface, hasChannelGroupMembers(contains("Ethernet1/8", "Ethernet1/9")));
      assertThat(
          iface,
          hasDependencies(
              containsInAnyOrder(
                  new Dependency("Ethernet1/8", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/9", DependencyType.AGGREGATE))));
      assertThat(iface, hasBandwidth(200E6));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel6");
      assertThat(iface, isActive(true));
      assertThat(iface, hasChannelGroupMembers(contains("Ethernet1/10", "Ethernet1/11")));
      assertThat(
          iface,
          hasDependencies(
              containsInAnyOrder(
                  new Dependency("Ethernet1/10", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/11", DependencyType.AGGREGATE))));
      assertThat(iface, hasBandwidth(200E6));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    // TODO: something with port-channel7 having to do with its constituents' channel-group modes
  }

  @Test
  public void testPortChannelExtraction() {
    String hostname = "nxos_port_channel";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys(
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8",
            "Ethernet1/9",
            "Ethernet1/10",
            "Ethernet1/11",
            "Ethernet1/12",
            "Ethernet1/13",
            "Ethernet1/14",
            "port-channel1",
            "port-channel2",
            "port-channel3",
            "port-channel4",
            "port-channel5",
            "port-channel6",
            "port-channel7"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertTrue(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel2"));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel3"));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel3"));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertTrue(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel3"));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel4"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/7");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel4"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/8");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel5"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/9");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel5"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/10");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel6"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/11");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel6"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    // TODO: extract channel-group mode for Ethernet1/12-1/14
    {
      Interface iface = vc.getInterfaces().get("port-channel1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
      Lacp lacp = iface.getLacp();
      assertThat(lacp, notNullValue());
      assertThat(lacp.getMinLinks(), equalTo(2));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel5");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel6");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    // TODO: something with port-channel7 having to do with its constituents' channel-group modes
  }

  @Test
  public void testPortChannelExtractionInvalid() {
    String hostname = "nxos_port_channel_invalid";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "port-channel1"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertTrue(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
  }

  @Test
  public void testPortChannelReferences() throws IOException {
    String hostname = "nxos_port_channel_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(
        ans, hasNumReferrers(filename, CiscoNxosStructureType.PORT_CHANNEL, "port-channel1", 1));
    assertThat(
        ans, hasNumReferrers(filename, CiscoNxosStructureType.PORT_CHANNEL, "port-channel2", 0));
  }

  @Test
  public void testRoleParsing() {
    // TODO: make into ref test
    assertThat(parseVendorConfig("nxos_role"), notNullValue());
  }

  @Test
  public void testRouteMapConversion() throws IOException {
    String hostname = "nxos_route_map";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getRoutingPolicies(),
        hasKeys(
            "empty_deny",
            "empty_permit",
            "empty_pbr_statistics",
            "match_as_path",
            "match_community",
            "match_interface",
            "match_ip_address",
            "match_ip_address_prefix_list",
            "match_metric",
            "match_tag",
            "set_as_path_prepend_last_as",
            "set_as_path_prepend_literal_as",
            "set_community",
            "set_community_additive",
            "set_ip_next_hop_literal",
            "set_ip_next_hop_literal2",
            "set_ip_next_hop_unchanged",
            "set_local_preference",
            "set_metric",
            "set_metric_type_external",
            "set_metric_type_internal",
            "set_metric_type_type_1",
            "set_metric_type_type_2",
            "set_origin_egp",
            "set_origin_igp",
            "set_origin_incomplete",
            "set_tag",
            "match_undefined_access_list",
            "match_undefined_community_list",
            "match_undefined_prefix_list",
            "continue_skip_deny",
            "continue_from_deny_to_permit",
            "continue_from_permit_to_fall_off",
            "continue_from_permit_and_set_to_fall_off",
            "continue_with_set_and_fall_off",
            "continue_from_set_to_match_on_set_field"));
    Ip origNextHopIp = Ip.parse("192.0.2.254");
    Bgpv4Route base =
        Bgpv4Route.builder()
            .setAsPath(AsPath.ofSingletonAsSets(2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(origNextHopIp)
            .setNetwork(Prefix.ZERO)
            .setTag(0L)
            .build();
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("empty_deny");
      assertRoutingPolicyDeniesRoute(rp, base);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("empty_permit");
      assertRoutingPolicyPermitsRoute(rp, base);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("empty_pbr_statistics");
      assertRoutingPolicyDeniesRoute(rp, base);
    }

    // matches
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_as_path");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route route = base.toBuilder().setAsPath(AsPath.ofSingletonAsSets(1L)).build();
      assertRoutingPolicyPermitsRoute(rp, route);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_community");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route routeOnlyOneCommunity =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(1, 1))).build();
      assertRoutingPolicyDeniesRoute(rp, routeOnlyOneCommunity);
      Bgpv4Route routeBothCommunities =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2)))
              .build();
      assertRoutingPolicyPermitsRoute(rp, routeBothCommunities);
      Bgpv4Route routeBothCommunitiesAndMore =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(
                      StandardCommunity.of(1, 1),
                      StandardCommunity.of(2, 2),
                      StandardCommunity.of(3, 3)))
              .build();
      assertRoutingPolicyPermitsRoute(rp, routeBothCommunitiesAndMore);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_interface");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route routeConnected = base.toBuilder().setNetwork(Prefix.parse("192.0.2.1/24")).build();
      assertRoutingPolicyPermitsRoute(rp, routeConnected);
      Bgpv4Route routeDirect = base.toBuilder().setNetwork(Prefix.parse("192.0.2.1/32")).build();
      assertRoutingPolicyPermitsRoute(rp, routeDirect);
    }
    // Skip match ip address - not relevant to routing
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_ip_address_prefix_list");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route route = base.toBuilder().setNetwork(Prefix.parse("192.168.1.0/24")).build();
      assertRoutingPolicyPermitsRoute(rp, route);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_metric");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route route = base.toBuilder().setMetric(1L).build();
      assertRoutingPolicyPermitsRoute(rp, route);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_tag");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route route = base.toBuilder().setTag(1L).build();
      assertRoutingPolicyPermitsRoute(rp, route);
    }

    // sets
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_as_path_prepend_last_as");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getAsPath(), equalTo(AsPath.ofSingletonAsSets(2L, 2L, 2L, 2L)));
      // TODO: test out direction. Requires BGP process, environment with neighor.
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_as_path_prepend_literal_as");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getAsPath(), equalTo(AsPath.ofSingletonAsSets(65000L, 65100L, 2L)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_community");
      Bgpv4Route inRoute =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(3, 3))).build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities(), contains(StandardCommunity.of(1, 1), StandardCommunity.of(1, 2)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_community_additive");
      Bgpv4Route inRoute =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(3, 3))).build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities(),
          contains(
              StandardCommunity.of(1, 1), StandardCommunity.of(1, 2), StandardCommunity.of(3, 3)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_ip_next_hop_literal");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("192.0.2.50")));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_ip_next_hop_literal2");
      Bgpv4Route route = processRouteIn(rp, base);
      // When there are two next-hop-IPs being set, the statement is not applicable to routing. So
      // the next-hop-IP should not change.
      assertThat(route.getNextHopIp(), equalTo(origNextHopIp));
    }
    // TODO: test set ip next-hop unchanged. Requires BGP process, environment with eBGP neighor.
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_local_preference");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getLocalPreference(), equalTo(1L));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_metric");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getMetric(), equalTo(1L));
    }
    // TODO: test set metric-type external
    // TODO: test set metric-type internal
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_metric_type_type_1");
      OspfExternalRoute route = processRouteRedistributeOspf(rp, base);
      assertThat(route.getOspfMetricType(), equalTo(OspfMetricType.E1));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_metric_type_type_2");
      OspfExternalRoute route = processRouteRedistributeOspf(rp, base);
      assertThat(route.getOspfMetricType(), equalTo(OspfMetricType.E2));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_origin_egp");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getOriginType(), equalTo(OriginType.EGP));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_origin_igp");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getOriginType(), equalTo(OriginType.IGP));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_origin_incomplete");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getOriginType(), equalTo(OriginType.INCOMPLETE));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_tag");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getTag(), equalTo(1L));
    }

    // matches with undefined references
    // TODO: match ip address (undefined) - relevant to routing?
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_undefined_community_list");
      assertRoutingPolicyDeniesRoute(rp, base);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_undefined_prefix_list");
      assertRoutingPolicyDeniesRoute(rp, base);
    }
    // TODO: continue route-maps
  }

  @Test
  public void testRouteMapExtraction() {
    String hostname = "nxos_route_map";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getRouteMaps(),
        hasKeys(
            "empty_deny",
            "empty_permit",
            "empty_pbr_statistics",
            "match_as_path",
            "match_community",
            "match_interface",
            "match_ip_address",
            "match_ip_address_prefix_list",
            "match_metric",
            "match_tag",
            "set_as_path_prepend_last_as",
            "set_as_path_prepend_literal_as",
            "set_community",
            "set_community_additive",
            "set_ip_next_hop_literal",
            "set_ip_next_hop_literal2",
            "set_ip_next_hop_unchanged",
            "set_local_preference",
            "set_metric",
            "set_metric_type_external",
            "set_metric_type_internal",
            "set_metric_type_type_1",
            "set_metric_type_type_2",
            "set_origin_egp",
            "set_origin_igp",
            "set_origin_incomplete",
            "set_tag",
            "match_undefined_access_list",
            "match_undefined_community_list",
            "match_undefined_prefix_list",
            "continue_skip_deny",
            "continue_from_deny_to_permit",
            "continue_from_permit_to_fall_off",
            "continue_from_permit_and_set_to_fall_off",
            "continue_with_set_and_fall_off",
            "continue_from_set_to_match_on_set_field"));
    {
      RouteMap rm = vc.getRouteMaps().get("empty_deny");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.DENY));
      assertThat(entry.getSequence(), equalTo(10));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("empty_permit");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("empty_pbr_statistics");
      assertThat(rm.getEntries(), anEmptyMap());
      assertTrue(rm.getPbrStatistics());
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_as_path");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchAsPath match = entry.getMatchAsPath();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("as_path_access_list1"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_community");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchCommunity match = entry.getMatchCommunity();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("community_list1"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_interface");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchInterface match = entry.getMatchInterface();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("loopback0"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_ip_address");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchIpAddress match = entry.getMatchIpAddress();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getName(), equalTo("access_list1"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_ip_address_prefix_list");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchIpAddressPrefixList match = entry.getMatchIpAddressPrefixList();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("prefix_list1"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_metric");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchMetric match = entry.getMatchMetric();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getMetric(), equalTo(1L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_tag");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchTag match = entry.getMatchTag();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTag(), equalTo(1L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_as_path_prepend_last_as");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetAsPathPrependLastAs set =
          (RouteMapSetAsPathPrependLastAs) entry.getSetAsPathPrepend();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getNumPrepends(), equalTo(3));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_as_path_prepend_literal_as");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetAsPathPrependLiteralAs set =
          (RouteMapSetAsPathPrependLiteralAs) entry.getSetAsPathPrepend();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getAsNumbers(), contains(65000L, 65100L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_community");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetCommunity set = entry.getSetCommunity();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(
          set.getCommunities(), contains(StandardCommunity.of(1, 1), StandardCommunity.of(1, 2)));
      assertFalse(set.getAdditive());
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_community_additive");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetCommunity set = entry.getSetCommunity();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(
          set.getCommunities(), contains(StandardCommunity.of(1, 1), StandardCommunity.of(1, 2)));
      assertTrue(set.getAdditive());
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_ip_next_hop_literal");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetIpNextHopLiteral set = (RouteMapSetIpNextHopLiteral) entry.getSetIpNextHop();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getNextHops(), contains(Ip.parse("192.0.2.50")));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_ip_next_hop_literal2");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetIpNextHopLiteral set = (RouteMapSetIpNextHopLiteral) entry.getSetIpNextHop();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getNextHops(), contains(Ip.parse("192.0.2.50"), Ip.parse("192.0.2.51")));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_ip_next_hop_unchanged");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetIpNextHopUnchanged set = (RouteMapSetIpNextHopUnchanged) entry.getSetIpNextHop();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_local_preference");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetLocalPreference set = entry.getSetLocalPreference();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getLocalPreference(), equalTo(1L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetric set = entry.getSetMetric();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetric(), equalTo(1L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric_type_external");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetricType set = entry.getSetMetricType();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetricType(), equalTo(RouteMapMetricType.EXTERNAL));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric_type_internal");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetricType set = entry.getSetMetricType();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetricType(), equalTo(RouteMapMetricType.INTERNAL));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric_type_type_1");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetricType set = entry.getSetMetricType();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetricType(), equalTo(RouteMapMetricType.TYPE_1));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric_type_type_2");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetricType set = entry.getSetMetricType();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetricType(), equalTo(RouteMapMetricType.TYPE_2));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_origin_egp");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetOrigin set = entry.getSetOrigin();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getOrigin(), equalTo(OriginType.EGP));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_origin_igp");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetOrigin set = entry.getSetOrigin();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getOrigin(), equalTo(OriginType.IGP));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_origin_incomplete");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetOrigin set = entry.getSetOrigin();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getOrigin(), equalTo(OriginType.INCOMPLETE));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_tag");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetTag set = entry.getSetTag();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getTag(), equalTo(1L));
    }
    // TODO: route-map 'continue' extraction
  }

  @Test
  public void testSnmpServerParsing() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_snmp_server"), notNullValue());
  }

  @Test
  public void testSpanningTreeParsing() {
    // TODO: make into an extraction test
    assertThat(parseVendorConfig("nxos_spanning_tree"), notNullValue());
  }

  @Test
  public void testStaticRouteConversion() throws IOException {
    String hostname = "nxos_static_route";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVrfs(), hasKeys(DEFAULT_VRF_NAME, NULL_VRF_NAME, "vrf1"));
    assertThat(
        c.getDefaultVrf().getStaticRoutes(),
        containsInAnyOrder(
            hasPrefix(Prefix.strict("10.0.0.0/24")),
            hasPrefix(Prefix.strict("10.0.1.0/24")),
            hasPrefix(Prefix.strict("10.0.2.0/24")),
            hasPrefix(Prefix.strict("10.0.3.0/24")),
            hasPrefix(Prefix.strict("10.0.4.0/24")),
            hasPrefix(Prefix.strict("10.0.5.0/24")),
            hasPrefix(Prefix.strict("10.0.6.0/24")),
            hasPrefix(Prefix.strict("10.0.7.0/24")),
            hasPrefix(Prefix.strict("10.0.8.0/24"))));
    assertThat(
        c.getVrfs().get("vrf1").getStaticRoutes(),
        contains(hasPrefix(Prefix.strict("10.0.11.0/24"))));
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.0.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasNextHopInterface(NULL_INTERFACE_NAME));
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.1.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasNextHopInterface(UNSET_NEXT_HOP_INTERFACE));
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.2.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.3.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.4.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.5.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(1000L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.6.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(5));
      assertThat(route, hasTag(1000L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.7.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(5));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.8.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(5));
      assertThat(route, hasTag(1000L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    // TODO: support next-hop-vrf used by 10.0.9.0/24 and 10.0.10.0/24
    {
      org.batfish.datamodel.StaticRoute route =
          c.getVrfs().get("vrf1").getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.11.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.2.254")));
    }
  }

  @Test
  public void testStaticRouteExtraction() {
    String hostname = "nxos_static_route";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getVrfs(), hasKeys("vrf1"));
    assertThat(
        vc.getDefaultVrf().getStaticRoutes().asMap(),
        hasKeys(
            Prefix.strict("10.0.0.0/24"),
            Prefix.strict("10.0.1.0/24"),
            Prefix.strict("10.0.2.0/24"),
            Prefix.strict("10.0.3.0/24"),
            Prefix.strict("10.0.4.0/24"),
            Prefix.strict("10.0.5.0/24"),
            Prefix.strict("10.0.6.0/24"),
            Prefix.strict("10.0.7.0/24"),
            Prefix.strict("10.0.8.0/24"),
            Prefix.strict("10.0.9.0/24"),
            Prefix.strict("10.0.10.0/24")));
    assertThat(
        vc.getVrfs().get("vrf1").getStaticRoutes().asMap(),
        hasKeys(Prefix.strict("10.0.11.0/24"), Prefix.strict("10.0.12.0/24")));
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.0.0/24")).iterator().next();
      assertTrue(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.1.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.2.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.3.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.4.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.5.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.6.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.7.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.8.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.9.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.2.254")));
      assertThat(route.getNextHopInterface(), nullValue());
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
      assertThat(route.getNextHopVrf(), equalTo("vrf2"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.10.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.2.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/2"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
      assertThat(route.getNextHopVrf(), equalTo("vrf2"));
    }
    {
      StaticRoute route =
          vc.getVrfs()
              .get("vrf1")
              .getStaticRoutes()
              .get(Prefix.strict("10.0.11.0/24"))
              .iterator()
              .next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.2.254")));
      assertThat(route.getNextHopVrf(), nullValue());
    }
    {
      StaticRoute route =
          vc.getVrfs()
              .get("vrf1")
              .getStaticRoutes()
              .get(Prefix.strict("10.0.12.0/24"))
              .iterator()
              .next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.2.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/100"));
      assertThat(route.getNextHopVrf(), nullValue());
    }
  }

  @Test
  public void testStaticRouteReferences() throws IOException {
    String hostname = "nxos_static_route_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.INTERFACE, "Ethernet1/1", 2));
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VRF, "vrf1", 2));
  }

  @Test
  public void testVersionExtraction() {
    String hostname = "nxos_version";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);
    assertThat(vc.getVersion(), equalTo("9.2(3) Bios:version"));
  }

  @Test
  public void testVlanConversion() throws IOException {
    String hostname = "nxos_vlan";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces(),
        hasKeys("Ethernet1/1", "Vlan1", "Vlan2", "Vlan3", "Vlan4", "Vlan6", "Vlan7"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan1");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(1));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan2");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(2));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan3");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState(false));
      assertThat(iface, hasVlan(3));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan4");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(4));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan6");
      assertThat(iface, isActive());
      assertThat(iface, isAutoState(false));
      assertThat(iface, hasVlan(6));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan7");
      assertThat(iface, isActive());
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(7));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
  }

  @Test
  public void testVlanExtraction() {
    String hostname = "nxos_vlan";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Vlan1", "Vlan2", "Vlan3", "Vlan4", "Vlan6", "Vlan7"));
    {
      Interface iface = vc.getInterfaces().get("Vlan1");
      assertTrue(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(1));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan2");
      assertTrue(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(2));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan3");
      assertFalse(iface.getShutdown());
      assertFalse(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(3));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan4");
      assertFalse(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(4));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan6");
      assertFalse(iface.getShutdown());
      assertFalse(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(6));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan7");
      assertFalse(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(7));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }

    assertThat(vc.getVlans(), hasKeys(2, 4, 6, 7, 8));
    {
      Vlan vlan = vc.getVlans().get(2);
      assertThat(vlan.getVni(), equalTo(12345));
    }
    {
      Vlan vlan = vc.getVlans().get(4);
      assertThat(vlan.getVni(), nullValue());
    }
  }

  @Test
  public void testVlanExtractionInvalid() {
    String hostname = "nxos_vlan_invalid";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getInterfaces(), anEmptyMap());
    assertThat(vc.getVlans(), anEmptyMap());
  }

  @Test
  public void testVlanReferences() throws IOException {
    String hostname = "nxos_vlan_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VLAN, "1", 1));
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VLAN, "2", 0));
    assertThat(ans, hasUndefinedReference(filename, CiscoNxosStructureType.VLAN, "3"));
  }

  @Test
  public void testVrfConversion() throws IOException {
    String hostname = "nxos_vrf";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVrfs(), hasKeys(DEFAULT_VRF_NAME, NULL_VRF_NAME, "Vrf1", "vrf3"));
    {
      org.batfish.datamodel.Vrf vrf = c.getVrfs().get(DEFAULT_VRF_NAME);
      assertThat(vrf, VrfMatchers.hasInterfaces(empty()));
    }
    {
      org.batfish.datamodel.Vrf vrf = c.getVrfs().get(NULL_VRF_NAME);
      assertThat(vrf, VrfMatchers.hasInterfaces(contains("Ethernet1/2")));
    }
    {
      org.batfish.datamodel.Vrf vrf = c.getVrfs().get("Vrf1");
      assertThat(
          vrf, VrfMatchers.hasInterfaces(contains("Ethernet1/1", "Ethernet1/3", "Ethernet1/4")));
    }
    {
      org.batfish.datamodel.Vrf vrf = c.getVrfs().get("vrf3");
      assertThat(vrf, VrfMatchers.hasInterfaces(contains("Ethernet1/5")));
    }

    assertThat(
        c.getAllInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3", "Ethernet1/4", "Ethernet1/5"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(iface, isActive());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      assertThat(iface, isActive());
      assertThat(iface, hasAddress(nullValue()));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
      assertThat(iface, hasAddress("10.0.4.1/24"));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive(false));
      assertThat(iface, hasAddress("10.0.5.1/24"));
    }
  }

  @Test
  public void testVrfExtraction() {
    String hostname = "nxos_vrf";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getVrfs(), hasKeys("Vrf1", "vrf3"));
    {
      Vrf vrf = vc.getDefaultVrf();
      assertFalse(vrf.getShutdown());
    }
    {
      Vrf vrf = vc.getVrfs().get("Vrf1");
      assertFalse(vrf.getShutdown());
      assertThat(
          vrf.getRd(), equalTo(RouteDistinguisherOrAuto.of(RouteDistinguisher.from(65001, 10L))));
      VrfAddressFamily af4 = vrf.getAddressFamily(AddressFamily.IPV4_UNICAST);
      assertThat(af4.getImportRtEvpn(), equalTo(ExtendedCommunityOrAuto.auto()));
      assertThat(af4.getExportRtEvpn(), equalTo(ExtendedCommunityOrAuto.auto()));
      assertThat(
          af4.getImportRt(),
          equalTo(ExtendedCommunityOrAuto.of(ExtendedCommunity.target(11L, 65536L))));
      assertThat(af4.getExportRt(), nullValue());

      VrfAddressFamily af6 = vrf.getAddressFamily(AddressFamily.IPV6_UNICAST);
      assertThat(
          af6.getImportRtEvpn(),
          equalTo(ExtendedCommunityOrAuto.of(ExtendedCommunity.target(65001L, 11L))));
      assertThat(
          af6.getExportRtEvpn(),
          equalTo(ExtendedCommunityOrAuto.of(ExtendedCommunity.target(65001L, 11L))));
      assertThat(af6.getImportRt(), equalTo(ExtendedCommunityOrAuto.auto()));
      assertThat(af6.getExportRt(), equalTo(ExtendedCommunityOrAuto.auto()));
    }
    {
      Vrf vrf = vc.getVrfs().get("vrf3");
      assertTrue(vrf.getShutdown());
      assertThat(vrf.getRd(), equalTo(RouteDistinguisherOrAuto.auto()));
    }

    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3", "Ethernet1/4", "Ethernet1/5"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("Vrf1"));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("vrf2"));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("Vrf1"));
      assertThat(iface.getAddress(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("Vrf1"));
      assertThat(
          iface.getAddress().getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.4.1/24")));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("vrf3"));
      assertThat(
          iface.getAddress().getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.5.1/24")));
    }
  }

  @Test
  public void testVrfExtractionInvalid() {
    String hostname = "nxos_vrf_invalid";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getVrfs(), hasKeys("vrf1"));
    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(iface.getVrfMember(), nullValue());
    }
  }

  @Test
  public void testVrfReferences() throws IOException {
    String hostname = "nxos_vrf_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VRF, "vrf_used", 1));
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VRF, "vrf_unused", 0));
    assertThat(ans, hasUndefinedReference(filename, CiscoNxosStructureType.VRF, "vrf_undefined"));
  }
}

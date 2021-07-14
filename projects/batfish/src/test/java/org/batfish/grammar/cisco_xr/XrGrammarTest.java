package org.batfish.grammar.cisco_xr;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.bdd.BDDMatchers.isZero;
import static org.batfish.common.matchers.ParseWarningMatchers.hasComment;
import static org.batfish.common.matchers.ParseWarningMatchers.hasText;
import static org.batfish.common.matchers.WarningsMatchers.hasParseWarnings;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.datamodel.AsPath.ofSingletonAsSets;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Names.generatedOspfInboundDistributeListName;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopIp;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasConfigurationFormat;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasBandwidth;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructure;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasParseWarning;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRedFlagWarning;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasReferencedStructure;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRoute6FilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.DataModelMatchers.permits;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasEncapsulationVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST;
import static org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT;
import static org.batfish.datamodel.routing_policy.Common.SUMMARY_ONLY_SUPPRESSION_POLICY_NAME;
import static org.batfish.datamodel.routing_policy.RoutingPolicy.isGenerated;
import static org.batfish.datamodel.routing_policy.expr.IntComparator.EQ;
import static org.batfish.datamodel.routing_policy.expr.IntComparator.GE;
import static org.batfish.datamodel.routing_policy.expr.IntComparator.LE;
import static org.batfish.main.BatfishTestUtils.TEST_SNAPSHOT;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.cisco_xr.CiscoXrConfiguration.RESOLUTION_POLICY_NAME;
import static org.batfish.representation.cisco_xr.CiscoXrConfiguration.computeAbfIpv4PolicyName;
import static org.batfish.representation.cisco_xr.CiscoXrConfiguration.computeCommunitySetMatchAnyName;
import static org.batfish.representation.cisco_xr.CiscoXrConfiguration.computeCommunitySetMatchEveryName;
import static org.batfish.representation.cisco_xr.CiscoXrConfiguration.computeExtcommunitySetRtName;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.CLASS_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.DYNAMIC_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.ETHERNET_SERVICES_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.FLOW_EXPORTER_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.FLOW_MONITOR_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.IPV6_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.POLICY_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PREFIX_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.RD_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.SAMPLER_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BRIDGE_DOMAIN_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BRIDGE_DOMAIN_ROUTED_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.FLOW_MONITOR_MAP_EXPORTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV4_MONITOR_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV4_MONITOR_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV4_SAMPLER_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV4_SAMPLER_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV6_MONITOR_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV6_MONITOR_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV6_SAMPLER_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV6_SAMPLER_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV4_ACCESS_GROUP_COMMON;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV4_ACCESS_GROUP_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV4_ACCESS_GROUP_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV6_ACCESS_GROUP_COMMON;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV6_ACCESS_GROUP_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV6_ACCESS_GROUP_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MPLS_LDP_AF_IPV4_DISCOVERY_TARGETED_HELLO_ACCEPT_FROM;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MPLS_LDP_AF_IPV4_REDISTRIBUTE_BGP_ADVERTISE_TO;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MPLS_LDP_AF_IPV6_DISCOVERY_TARGETED_HELLO_ACCEPT_FROM;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MPLS_LDP_AF_IPV6_REDISTRIBUTE_BGP_ADVERTISE_TO;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MULTICAST_ROUTING_CORE_TREE_PROTOCOL_RSVP_TE_GROUP_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_ACCESS_GROUP_PEER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_ACCESS_GROUP_QUERY_ONLY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_ACCESS_GROUP_SERVE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_ACCESS_GROUP_SERVE_ONLY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_IGMP_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_IGMP_EXPLICIT_TRACKING;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_IGMP_MAXIMUM_GROUPS_PER_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_IGMP_SSM_MAP_STATIC;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MLD_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MLD_EXPLICIT_TRACKING;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MLD_MAXIMUM_GROUPS_PER_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MLD_SSM_MAP_STATIC;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MSDP_CACHE_SA_STATE_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MSDP_CACHE_SA_STATE_RP_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MSDP_SA_FILTER_IN_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MSDP_SA_FILTER_IN_RP_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MSDP_SA_FILTER_OUT_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_MSDP_SA_FILTER_OUT_RP_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_ACCEPT_REGISTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_ALLOW_RP_GROUP_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_ALLOW_RP_RP_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_AUTO_RP_CANDIDATE_RP_GROUP_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_BSR_CANDIDATE_RP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_MDT_NEIGHBOR_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_MOFRR_FLOW;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_MOFRR_RIB;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_NEIGHBOR_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_RPF_TOPOLOGY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_RP_ADDRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_RP_STATIC_DENY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_SG_EXPIRY_TIMER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_SPT_THRESHOLD;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_SSM_THRESHOLD_RANGE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_RD_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_COMMUNITY_ACL4;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_COMMUNITY_ACL6;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.VRF_EXPORT_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.VRF_EXPORT_TO_DEFAULT_VRF_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.VRF_IMPORT_FROM_DEFAULT_VRF_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.VRF_IMPORT_ROUTE_POLICY;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.PermitAndDenyBdds;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AbstractRouteBuilder;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BddTestbed;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Bgpv4Route.Builder;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DscpType;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Ip6AccessList;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.bgp.BgpAggregate;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.packet_policy.Drop;
import org.batfish.datamodel.packet_policy.FibLookup;
import org.batfish.datamodel.packet_policy.FibLookupOverrideLookupIp;
import org.batfish.datamodel.packet_policy.FlowEvaluator;
import org.batfish.datamodel.packet_policy.IngressInterfaceVrf;
import org.batfish.datamodel.packet_policy.LiteralVrfName;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.route.nh.NextHopDiscard;
import org.batfish.datamodel.route.nh.NextHopInterface;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.CommunityContext;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExprEvaluator;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.cisco_xr.AddressFamilyType;
import org.batfish.representation.cisco_xr.AsPathSetElem;
import org.batfish.representation.cisco_xr.AsPathSetExpr;
import org.batfish.representation.cisco_xr.AsPathSetReference;
import org.batfish.representation.cisco_xr.AsPathSetVariable;
import org.batfish.representation.cisco_xr.BgpAggregateIpv4Network;
import org.batfish.representation.cisco_xr.BridgeDomain;
import org.batfish.representation.cisco_xr.CiscoXrConfiguration;
import org.batfish.representation.cisco_xr.DfaRegexAsPathSetElem;
import org.batfish.representation.cisco_xr.DistributeList;
import org.batfish.representation.cisco_xr.DistributeList.DistributeListFilterType;
import org.batfish.representation.cisco_xr.ExtcommunitySetRt;
import org.batfish.representation.cisco_xr.ExtcommunitySetRtElemAsColon;
import org.batfish.representation.cisco_xr.ExtcommunitySetRtElemAsDotColon;
import org.batfish.representation.cisco_xr.InlineAsPathSet;
import org.batfish.representation.cisco_xr.IosRegexAsPathSetElem;
import org.batfish.representation.cisco_xr.Ipv4AccessList;
import org.batfish.representation.cisco_xr.Ipv4AccessListLine;
import org.batfish.representation.cisco_xr.Ipv6AccessList;
import org.batfish.representation.cisco_xr.Ipv6AccessListLine;
import org.batfish.representation.cisco_xr.LengthAsPathSetElem;
import org.batfish.representation.cisco_xr.LiteralUint16;
import org.batfish.representation.cisco_xr.LiteralUint16Range;
import org.batfish.representation.cisco_xr.LiteralUint32;
import org.batfish.representation.cisco_xr.NeighborIsAsPathSetElem;
import org.batfish.representation.cisco_xr.OriginatesFromAsPathSetElem;
import org.batfish.representation.cisco_xr.OspfNetworkType;
import org.batfish.representation.cisco_xr.OspfProcess;
import org.batfish.representation.cisco_xr.PassesThroughAsPathSetElem;
import org.batfish.representation.cisco_xr.PeerAs;
import org.batfish.representation.cisco_xr.PrivateAs;
import org.batfish.representation.cisco_xr.RdSet;
import org.batfish.representation.cisco_xr.RdSetAsDot;
import org.batfish.representation.cisco_xr.RdSetAsPlain16;
import org.batfish.representation.cisco_xr.RdSetAsPlain32;
import org.batfish.representation.cisco_xr.RdSetDfaRegex;
import org.batfish.representation.cisco_xr.RdSetIosRegex;
import org.batfish.representation.cisco_xr.RdSetIpAddress;
import org.batfish.representation.cisco_xr.RdSetIpPrefix;
import org.batfish.representation.cisco_xr.RoutePolicy;
import org.batfish.representation.cisco_xr.RoutePolicyBoolean;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathIn;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathIsLocal;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathLength;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathNeighborIs;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathOriginatesFrom;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathPassesThrough;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathUniqueLength;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanValidationStateIs;
import org.batfish.representation.cisco_xr.RoutePolicyDispositionStatement;
import org.batfish.representation.cisco_xr.RoutePolicyDispositionType;
import org.batfish.representation.cisco_xr.RoutePolicyElseIfBlock;
import org.batfish.representation.cisco_xr.RoutePolicyIfStatement;
import org.batfish.representation.cisco_xr.RoutePolicyStatement;
import org.batfish.representation.cisco_xr.SimpleExtendedAccessListServiceSpecifier;
import org.batfish.representation.cisco_xr.TagRewritePolicy;
import org.batfish.representation.cisco_xr.TagRewritePop;
import org.batfish.representation.cisco_xr.UnimplementedBoolean;
import org.batfish.representation.cisco_xr.UniqueLengthAsPathSetElem;
import org.batfish.representation.cisco_xr.Vrf;
import org.batfish.representation.cisco_xr.WildcardUint16RangeExpr;
import org.batfish.representation.cisco_xr.WildcardUint32RangeExpr;
import org.batfish.representation.cisco_xr.XrCommunitySet;
import org.batfish.representation.cisco_xr.XrCommunitySetDfaRegex;
import org.batfish.representation.cisco_xr.XrCommunitySetHighLowRangeExprs;
import org.batfish.representation.cisco_xr.XrCommunitySetIosRegex;
import org.batfish.representation.cisco_xr.XrInlineCommunitySet;
import org.batfish.representation.cisco_xr.XrRoutePolicyBooleanCommunityMatchesAny;
import org.batfish.representation.cisco_xr.XrRoutePolicyBooleanCommunityMatchesEvery;
import org.batfish.representation.cisco_xr.XrRoutePolicyDeleteCommunityStatement;
import org.batfish.representation.cisco_xr.XrRoutePolicySetCommunity;
import org.batfish.representation.cisco_xr.XrWildcardCommunitySetElem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link CiscoXrParser} and {@link CiscoXrControlPlaneExtractor}. */
public final class XrGrammarTest {
  private final BddTestbed _bddTestbed = new BddTestbed(ImmutableMap.of(), ImmutableMap.of());
  private final IpAccessListToBdd _aclToBdd = _bddTestbed.getAclToBdd();

  static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco_xr/testconfigs/";
  static final String SNAPSHOTS_PREFIX = "org/batfish/grammar/cisco_xr/snapshots/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private Batfish getBatfishForConfigurationNames(String... configurationNames) {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    try {
      return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.parseTextConfigs(_folder, names);
  }

  private @Nonnull Configuration parseConfig(String hostname) {
    try {
      Map<String, Configuration> configs = parseTextConfigs(hostname);
      assertThat(configs, hasKey(hostname.toLowerCase()));
      Configuration c = configs.get(hostname.toLowerCase());
      assertThat(c, hasConfigurationFormat(ConfigurationFormat.CISCO_IOS_XR));
      // Ensure that we used the CiscoXr parser.
      assertThat(c.getVendorFamily().getCiscoXr(), notNullValue());
      return c;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private @Nonnull CiscoXrConfiguration parseVendorConfig(String hostname) {
    String src = readResource(TESTCONFIGS_PREFIX + hostname, UTF_8);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    CiscoXrCombinedParser ciscoXrParser = new CiscoXrCombinedParser(src, settings);
    CiscoXrControlPlaneExtractor extractor =
        new CiscoXrControlPlaneExtractor(
            src,
            ciscoXrParser,
            ConfigurationFormat.CISCO_IOS_XR,
            new Warnings(),
            new SilentSyntaxCollection());
    ParserRuleContext tree =
        Batfish.parse(
            ciscoXrParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(TEST_SNAPSHOT, tree);
    CiscoXrConfiguration vendorConfiguration =
        (CiscoXrConfiguration) extractor.getVendorConfiguration();
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + hostname);
    // crash if not serializable
    return SerializationUtils.clone(vendorConfiguration);
  }

  private static void assertRoutingPolicyDeniesRoute(
      RoutingPolicy routingPolicy, AbstractRoute route) {
    assertFalse(
        routingPolicy.process(
            route, Bgpv4Route.testBuilder().setNetwork(route.getNetwork()), Direction.OUT));
  }

  private static void assertRoutingPolicyPermitsRoute(
      RoutingPolicy routingPolicy, AbstractRoute route) {
    assertRoutingPolicyPermitsRoute(
        routingPolicy, route, Bgpv4Route.testBuilder().setNetwork(route.getNetwork()));
  }

  private static void assertRoutingPolicyPermitsRoute(
      RoutingPolicy routingPolicy, AbstractRoute route, AbstractRouteBuilder<?, ?> builder) {
    assertTrue(routingPolicy.process(route, builder, Direction.OUT));
  }

  private static @Nonnull Bgpv4Route processRouteIn(RoutingPolicy routingPolicy, Bgpv4Route route) {
    Builder builder = route.toBuilder();
    assertTrue(routingPolicy.process(route, builder, Direction.IN));
    return builder.build();
  }

  @Test
  public void testAclExtraction() {
    CiscoXrConfiguration c = parseVendorConfig("acl");
    assertThat(c.getIpv4Acls(), hasKeys("acl"));
    Ipv4AccessList acl = c.getIpv4Acls().get("acl");
    // TODO: get the remark line in there too.
    assertThat(acl.getLines(), hasSize(8));

    assertThat(c.getIpv6Acls(), hasKeys("aclv6"));
    Ipv6AccessList aclv6 = c.getIpv6Acls().get("aclv6");
    // TODO: get the remark line in there too.
    assertThat(aclv6.getLines(), hasSize(4));
  }

  @Test
  public void testAclConversion() {
    Configuration c = parseConfig("acl");
    assertThat(c.getIpAccessLists(), hasKeys("acl"));
    IpAccessList acl = c.getIpAccessLists().get("acl");
    // TODO: get the remark line in there too.
    assertThat(acl.getLines(), hasSize(8));
    {
      // Test fragments.
      AclLine fragmentLine = acl.getLines().get(7);
      PermitAndDenyBdds bdds = _aclToBdd.toPermitAndDenyBdds(fragmentLine);
      HeaderSpace expected =
          HeaderSpace.builder()
              .setNotFragmentOffsets(ImmutableList.of(SubRange.singleton(0)))
              .build();
      assertThat(bdds.getPermitBdd(), equalTo(_aclToBdd.getHeaderSpaceToBDD().toBDD(expected)));
      assertThat(bdds.getDenyBdd(), isZero());
    }

    assertThat(c.getIp6AccessLists(), hasKeys("aclv6"));
    Ip6AccessList aclv6 = c.getIp6AccessLists().get("aclv6");
    // TODO: get the remark line in there too.
    assertThat(aclv6.getLines(), hasSize(4));
  }

  @Test
  public void testBanner() {
    Configuration c = parseConfig("banner");
    assertThat(
        c.getVendorFamily().getCiscoXr().getBanners(),
        equalTo(
            ImmutableMap.of(
                "exec",
                "First line.\nSecond line, with no ignored text.",
                "login",
                "First line.\nSecond line.")));
  }

  @Test
  public void testBundleEtherSubInterfaces() {
    Configuration c = parseConfig("bundle-ether-subif");
    assertThat(
        c.getAllInterfaces(),
        hasKeys(
            "Bundle-Ether500",
            "Bundle-Ether500.2",
            "TenGigE0/1",
            "Bundle-Ether600",
            "Bundle-Ether600.3",
            "TenGigE0/2"));
    assertThat(c.getAllInterfaces().get("Bundle-Ether500"), allOf(isActive(), hasBandwidth(10e9)));
    assertThat(
        c.getAllInterfaces().get("Bundle-Ether500.2"),
        allOf(isActive(), hasBandwidth(10e9), hasEncapsulationVlan(2)));
    assertThat(
        c.getAllInterfaces().get("Bundle-Ether600"), allOf(isActive(false), hasBandwidth(10e9)));
    assertThat(
        c.getAllInterfaces().get("Bundle-Ether600.3"),
        allOf(isActive(false), hasBandwidth(10e9), hasEncapsulationVlan(3)));
  }

  /**
   * Regression test for a parser crash related to peer stack indexing issues.
   *
   * <p>The test config is a minimized version of user configuration submitted through Batfish
   * diagnostics.
   */
  @Test
  public void testBgpNeighborCrash() {
    // Don't crash.
    parseConfig("bgp-neighbor-crash");
  }

  /**
   * Regression test for a parser crash related to multiple routers with different ASNs
   * (fat-fingered).
   */
  @Test
  public void testMultipleRouterCrash() {
    // Don't crash.
    parseConfig("bgp-multiple-routers");
  }

  @Test
  public void testRoutePolicyCommunityInlineExtraction() {
    String hostname = "rp-community-inline";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);
    assertThat(
        vc.getRoutePolicies(),
        hasKeys(
            "set-well-known",
            "set-literal",
            "set-peeras",
            "matches-any",
            "matches-every",
            "delete-well-known",
            "delete-literal",
            "delete-halves",
            "delete-all",
            "delete-regex"));
    {
      RoutePolicy rp = vc.getRoutePolicies().get("set-well-known");
      assertThat(
          rp.getStatements(),
          contains(
              new XrRoutePolicySetCommunity(
                  new XrInlineCommunitySet(
                      new XrCommunitySet(
                          ImmutableList.of(
                              XrCommunitySetHighLowRangeExprs.of(WellKnownCommunity.ACCEPT_OWN)))),
                  false),
              new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)));
    }
    {
      RoutePolicy rp = vc.getRoutePolicies().get("set-literal");
      assertThat(
          rp.getStatements(),
          contains(
              new XrRoutePolicySetCommunity(
                  new XrInlineCommunitySet(
                      new XrCommunitySet(
                          ImmutableList.of(
                              new XrCommunitySetHighLowRangeExprs(
                                  new LiteralUint16(1), new LiteralUint16(1))))),
                  false),
              new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)));
    }
    {
      RoutePolicy rp = vc.getRoutePolicies().get("set-peeras");
      assertThat(
          rp.getStatements(),
          contains(
              new XrRoutePolicySetCommunity(
                  new XrInlineCommunitySet(
                      new XrCommunitySet(
                          ImmutableList.of(
                              new XrCommunitySetHighLowRangeExprs(
                                  PeerAs.instance(), new LiteralUint16(1))))),
                  false),
              new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)));
    }
    {
      RoutePolicy rp = vc.getRoutePolicies().get("matches-any");
      assertThat(
          rp.getStatements(),
          contains(
              new RoutePolicyIfStatement(
                  new XrRoutePolicyBooleanCommunityMatchesAny(
                      new XrInlineCommunitySet(
                          new XrCommunitySet(
                              ImmutableList.of(
                                  new XrCommunitySetHighLowRangeExprs(
                                      new LiteralUint16(1), new LiteralUint16(1)),
                                  new XrCommunitySetHighLowRangeExprs(
                                      new LiteralUint16(2), new LiteralUint16(2)))))),
                  ImmutableList.of(
                      new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)),
                  ImmutableList.of(),
                  null)));
    }
    {
      RoutePolicy rp = vc.getRoutePolicies().get("matches-every");
      assertThat(
          rp.getStatements(),
          contains(
              new RoutePolicyIfStatement(
                  new XrRoutePolicyBooleanCommunityMatchesEvery(
                      new XrInlineCommunitySet(
                          new XrCommunitySet(
                              ImmutableList.of(
                                  new XrCommunitySetHighLowRangeExprs(
                                      new LiteralUint16(1), new LiteralUint16(1)),
                                  new XrCommunitySetHighLowRangeExprs(
                                      new LiteralUint16(2), new LiteralUint16(2)))))),
                  ImmutableList.of(
                      new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)),
                  ImmutableList.of(),
                  null)));
    }
    {
      RoutePolicy rp = vc.getRoutePolicies().get("delete-well-known");
      assertThat(
          rp.getStatements(),
          contains(
              new XrRoutePolicyDeleteCommunityStatement(
                  false,
                  new XrInlineCommunitySet(
                      new XrCommunitySet(
                          ImmutableList.of(
                              XrCommunitySetHighLowRangeExprs.of(WellKnownCommunity.ACCEPT_OWN))))),
              new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)));
    }
    {
      RoutePolicy rp = vc.getRoutePolicies().get("delete-literal");
      assertThat(
          rp.getStatements(),
          contains(
              new XrRoutePolicyDeleteCommunityStatement(
                  false,
                  new XrInlineCommunitySet(
                      new XrCommunitySet(
                          ImmutableList.of(
                              new XrCommunitySetHighLowRangeExprs(
                                  new LiteralUint16(1), new LiteralUint16(1)))))),
              new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)));
    }
    {
      RoutePolicy rp = vc.getRoutePolicies().get("delete-halves");
      assertThat(
          rp.getStatements(),
          contains(
              new XrRoutePolicyDeleteCommunityStatement(
                  false,
                  new XrInlineCommunitySet(
                      new XrCommunitySet(
                          ImmutableList.of(
                              new XrCommunitySetHighLowRangeExprs(
                                  new LiteralUint16(1), new LiteralUint16Range(new SubRange(2, 3))),
                              new XrCommunitySetHighLowRangeExprs(
                                  new LiteralUint16Range(new SubRange(4, 5)), new LiteralUint16(6)),
                              new XrCommunitySetHighLowRangeExprs(
                                  WildcardUint16RangeExpr.instance(), new LiteralUint16(7)),
                              new XrCommunitySetHighLowRangeExprs(
                                  new LiteralUint16(8), WildcardUint16RangeExpr.instance()),
                              new XrCommunitySetHighLowRangeExprs(
                                  PeerAs.instance(), new LiteralUint16(9)),
                              new XrCommunitySetHighLowRangeExprs(
                                  PrivateAs.instance(), new LiteralUint16(10)))))),
              new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)));
    }
    {
      RoutePolicy rp = vc.getRoutePolicies().get("delete-all");
      assertThat(
          rp.getStatements(),
          contains(
              new XrRoutePolicyDeleteCommunityStatement(
                  false,
                  new XrInlineCommunitySet(
                      new XrCommunitySet(ImmutableList.of(XrWildcardCommunitySetElem.instance())))),
              new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)));
    }
    {
      RoutePolicy rp = vc.getRoutePolicies().get("delete-regex");
      assertThat(
          rp.getStatements(),
          contains(
              new XrRoutePolicyDeleteCommunityStatement(
                  false,
                  new XrInlineCommunitySet(
                      new XrCommunitySet(
                          ImmutableList.of(
                              new XrCommunitySetIosRegex("_1234:.*"),
                              new XrCommunitySetDfaRegex("_5678:.*"))))),
              new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)));
    }
  }

  @Test
  public void testCommunitySetExtraction() {
    String hostname = "community-set";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getCommunitySets(), hasKeys("mixed", "universe", "universe2", "wellknown"));
    {
      XrCommunitySet set = vc.getCommunitySets().get("mixed");
      assertThat(
          set.getElements(),
          contains(
              new XrCommunitySetDfaRegex("_5678:.*"),
              new XrCommunitySetIosRegex("_1234:.*"),
              new XrCommunitySetHighLowRangeExprs(new LiteralUint16(1), new LiteralUint16(2)),
              new XrCommunitySetHighLowRangeExprs(
                  WildcardUint16RangeExpr.instance(), new LiteralUint16(3)),
              new XrCommunitySetHighLowRangeExprs(
                  new LiteralUint16(4), WildcardUint16RangeExpr.instance()),
              new XrCommunitySetHighLowRangeExprs(
                  new LiteralUint16(6), new LiteralUint16Range(new SubRange(100, 103)))));
    }
    {
      XrCommunitySet set = vc.getCommunitySets().get("universe");
      assertThat(
          set.getElements(),
          contains(
              new XrCommunitySetHighLowRangeExprs(
                  WildcardUint16RangeExpr.instance(), WildcardUint16RangeExpr.instance())));
    }
    {
      XrCommunitySet set = vc.getCommunitySets().get("universe2");
      assertThat(set.getElements(), contains(XrWildcardCommunitySetElem.instance()));
    }
    {
      XrCommunitySet set = vc.getCommunitySets().get("wellknown");
      assertThat(
          set.getElements(),
          contains(
              XrCommunitySetHighLowRangeExprs.of(WellKnownCommunity.ACCEPT_OWN),
              XrCommunitySetHighLowRangeExprs.of(WellKnownCommunity.GRACEFUL_SHUTDOWN),
              XrCommunitySetHighLowRangeExprs.of(WellKnownCommunity.INTERNET),
              XrCommunitySetHighLowRangeExprs.of(WellKnownCommunity.NO_EXPORT_SUBCONFED),
              XrCommunitySetHighLowRangeExprs.of(WellKnownCommunity.NO_ADVERTISE),
              XrCommunitySetHighLowRangeExprs.of(WellKnownCommunity.NO_EXPORT)));
    }
  }

  @Test
  public void testRoutePolicyImplicitActionsConversion() {
    Configuration c = parseConfig("route-policy-implicit-actions");

    Prefix prefixNoMatch = Prefix.parse("10.11.0.0/16");
    Prefix prefixLocalPref = Prefix.parse("10.10.0.0/16");
    Prefix prefixLocalPrefThenDrop = Prefix.parse("10.10.10.0/24");
    Prefix prefixAsPath = Prefix.parse("192.168.2.0/24");
    Prefix prefixOspfMetricType = Prefix.parse("192.168.1.0/24");
    Builder bgpLocalPref = Bgpv4Route.testBuilder().setNetwork(prefixLocalPref);
    Builder bgpLocalPrefThenDrop = Bgpv4Route.testBuilder().setNetwork(prefixLocalPrefThenDrop);
    Builder bgpNoMatch = Bgpv4Route.testBuilder().setNetwork(prefixNoMatch);
    Builder bgpAsPath = Bgpv4Route.testBuilder().setNetwork(prefixAsPath);
    OspfExternalRoute.Builder ospfMetricType =
        OspfExternalType1Route.testBuilder().setNetwork(prefixOspfMetricType);

    assertThat(c.getRoutingPolicies(), hasKeys("implicit-actions", RESOLUTION_POLICY_NAME));
    RoutingPolicy rp = c.getRoutingPolicies().get("implicit-actions");

    // If routes are updated, default-deny doesn't apply
    // Confirm default is accept when local-pref is updated
    assertRoutingPolicyPermitsRoute(rp, bgpLocalPref.build(), bgpLocalPref);
    assertThat(bgpLocalPref.build().getLocalPreference(), equalTo(100L));
    // Confirm default is pass when as-path is updated
    assertRoutingPolicyPermitsRoute(rp, bgpAsPath.build(), bgpAsPath);
    assertThat(bgpAsPath.build().getAsPath(), equalTo(ofSingletonAsSets(65432L)));
    // Confirm default is pass when OSPF metric type is updated
    assertRoutingPolicyPermitsRoute(rp, ospfMetricType.build(), ospfMetricType);
    assertThat(ospfMetricType.build().getOspfMetricType(), equalTo(OspfMetricType.E2));

    // Even if default is pass, explicit drop should still take effect
    assertRoutingPolicyDeniesRoute(rp, bgpLocalPrefThenDrop.build());

    // No match / no route update should use default-deny
    assertRoutingPolicyDeniesRoute(rp, bgpNoMatch.build());
  }

  @Test
  public void testCommunitySetConversion() {
    Configuration c = parseConfig("community-set");
    CommunityContext ctx = CommunityContext.builder().build();

    // TODO: test wellknown

    // Test CommunityMatchExprs
    assertThat(c.getCommunityMatchExprs(), hasKeys("universe", "universe2", "mixed", "wellknown"));
    {
      CommunityMatchExpr expr = c.getCommunityMatchExprs().get("universe");
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(1L)));
    }
    {
      CommunityMatchExpr expr = c.getCommunityMatchExprs().get("universe2");
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(1L)));
    }
    {
      CommunityMatchExpr expr = c.getCommunityMatchExprs().get("mixed");
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(1234, 1)));
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(1, 2)));
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(2, 3)));
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(4, 5)));
      assertFalse(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(6, 99)));
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(6, 100)));
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(6, 101)));
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(6, 102)));
      assertTrue(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(6, 103)));
      assertFalse(expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(6, 104)));
    }

    // Test CommunitySetExprs
    assertThat(c.getCommunitySetExprs(), hasKeys("universe", "universe2", "mixed", "wellknown"));
    {
      CommunitySetExpr expr = c.getCommunitySetExprs().get("universe");
      assertThat(
          expr.accept(CommunitySetExprEvaluator.instance(), ctx), equalTo(CommunitySet.empty()));
    }
    {
      CommunitySetExpr expr = c.getCommunitySetExprs().get("universe2");
      assertThat(
          expr.accept(CommunitySetExprEvaluator.instance(), ctx), equalTo(CommunitySet.empty()));
    }
    {
      CommunitySetExpr expr = c.getCommunitySetExprs().get("mixed");
      assertThat(
          expr.accept(CommunitySetExprEvaluator.instance(), ctx),
          equalTo(CommunitySet.of(StandardCommunity.of(1, 2))));
    }

    // Test CommunitySetMatchExprs
    assertThat(
        c.getCommunitySetMatchExprs(),
        hasKeys(
            computeCommunitySetMatchAnyName("universe"),
            computeCommunitySetMatchEveryName("universe"),
            computeCommunitySetMatchAnyName("universe2"),
            computeCommunitySetMatchEveryName("universe2"),
            computeCommunitySetMatchAnyName("mixed"),
            computeCommunitySetMatchEveryName("mixed"),
            computeCommunitySetMatchAnyName("wellknown"),
            computeCommunitySetMatchEveryName("wellknown")));
    {
      CommunitySetMatchExpr expr =
          c.getCommunitySetMatchExprs().get(computeCommunitySetMatchAnyName("universe"));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(5, 5), StandardCommunity.of(7, 7))));
    }
    {
      CommunitySetMatchExpr expr =
          c.getCommunitySetMatchExprs().get(computeCommunitySetMatchEveryName("universe"));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(5, 5), StandardCommunity.of(7, 7))));
    }
    {
      CommunitySetMatchExpr expr =
          c.getCommunitySetMatchExprs().get(computeCommunitySetMatchAnyName("universe2"));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(5, 5), StandardCommunity.of(7, 7))));
    }
    {
      CommunitySetMatchExpr expr =
          c.getCommunitySetMatchExprs().get(computeCommunitySetMatchEveryName("universe2"));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(5, 5), StandardCommunity.of(7, 7))));
    }
    {
      CommunitySetMatchExpr expr =
          c.getCommunitySetMatchExprs().get(computeCommunitySetMatchAnyName("mixed"));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(1234, 1))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(1, 2))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(2, 3))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(4, 5))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(6, 100))));
    }
    {
      CommunitySetMatchExpr expr =
          c.getCommunitySetMatchExprs().get(computeCommunitySetMatchEveryName("mixed"));
      assertFalse(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(1234, 1))));
      assertFalse(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(1, 2))));
      assertFalse(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(2, 3))));
      assertFalse(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(4, 5))));
      assertFalse(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(StandardCommunity.of(6, 100))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(
                  StandardCommunity.of(5678, 1),
                  StandardCommunity.of(1234, 1),
                  StandardCommunity.of(1, 2),
                  StandardCommunity.of(2, 3),
                  StandardCommunity.of(4, 5),
                  StandardCommunity.of(6, 100))));
    }

    // Test route-policy match and set
    assertThat(
        c.getRoutingPolicies(),
        hasKeys(
            "any",
            "every",
            "setmixed",
            "setmixedadditive",
            "deleteall",
            "deletein",
            "deleteininline",
            "deletenotin",
            RESOLUTION_POLICY_NAME));
    Ip origNextHopIp = Ip.parse("192.0.2.254");
    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setAsPath(ofSingletonAsSets(2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(origNextHopIp)
            .setNetwork(Prefix.ZERO)
            .setTag(0L)
            .build();
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("any");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route routeOneMatchingCommunity =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(3, 3))).build();
      assertRoutingPolicyPermitsRoute(rp, routeOneMatchingCommunity);
      Bgpv4Route routeNoMatchingCommunity =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(9, 9))).build();
      assertRoutingPolicyDeniesRoute(rp, routeNoMatchingCommunity);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("every");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route routeOneMatchingCommunity =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(3, 3))).build();
      assertRoutingPolicyDeniesRoute(rp, routeOneMatchingCommunity);
      Bgpv4Route routeAllMatchingCommunities =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(
                      StandardCommunity.of(5678, 1),
                      StandardCommunity.of(1234, 1),
                      StandardCommunity.of(1, 2),
                      StandardCommunity.of(2, 3),
                      StandardCommunity.of(4, 5),
                      StandardCommunity.of(6, 100)))
              .build();
      assertRoutingPolicyPermitsRoute(rp, routeAllMatchingCommunities);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("setmixed");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(9, 9), ExtendedCommunity.target(1L, 1L)))
              .build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities().getCommunities(),
          containsInAnyOrder(StandardCommunity.of(1, 2), ExtendedCommunity.target(1L, 1L)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("setmixedadditive");
      Bgpv4Route inRoute =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(9, 9))).build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities().getCommunities(),
          containsInAnyOrder(StandardCommunity.of(1, 2), StandardCommunity.of(9, 9)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("deleteall");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.INTERNET))
              .build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities().getCommunities(), containsInAnyOrder(StandardCommunity.INTERNET));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("deletein");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.INTERNET))
              .build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(route.getCommunities().getCommunities(), empty());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("deleteininline");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.INTERNET))
              .build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(route.getCommunities().getCommunities(), empty());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("deletenotin");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.INTERNET))
              .build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities().getCommunities(),
          containsInAnyOrder(StandardCommunity.of(1, 1), StandardCommunity.INTERNET));
    }
  }

  @Test
  public void testExtcommunitySetRtConversion() {
    Configuration c = parseConfig("ios-xr-extcommunity-set-rt");
    CommunityContext ctx = CommunityContext.builder().build();

    // Test CommunityMatchExprs
    assertThat(c.getCommunityMatchExprs(), hasKeys(computeExtcommunitySetRtName("rt1")));
    {
      CommunityMatchExpr expr = c.getCommunityMatchExprs().get(computeExtcommunitySetRtName("rt1"));
      assertTrue(
          expr.accept(ctx.getCommunityMatchExprEvaluator(), ExtendedCommunity.target(1234L, 56L)));
      assertTrue(
          expr.accept(ctx.getCommunityMatchExprEvaluator(), ExtendedCommunity.target(1234L, 57L)));
      assertFalse(
          expr.accept(ctx.getCommunityMatchExprEvaluator(), ExtendedCommunity.target(1234L, 0L)));
      assertFalse(
          expr.accept(ctx.getCommunityMatchExprEvaluator(), StandardCommunity.of(1234, 56)));
    }

    // Test CommunitySetExprs
    assertThat(c.getCommunitySetExprs(), hasKeys(computeExtcommunitySetRtName("rt1")));
    {
      CommunitySetExpr expr = c.getCommunitySetExprs().get(computeExtcommunitySetRtName("rt1"));
      assertThat(
          expr.accept(CommunitySetExprEvaluator.instance(), ctx),
          equalTo(
              CommunitySet.of(
                  ExtendedCommunity.target(1234L, 56L),
                  ExtendedCommunity.target(1234L, 57L),
                  ExtendedCommunity.target((12L << 16) | 34L, 56L))));
    }

    // Test CommunitySetMatchExprs
    assertThat(
        c.getCommunitySetMatchExprs(),
        hasKeys(
            computeCommunitySetMatchAnyName(computeExtcommunitySetRtName("rt1")),
            computeCommunitySetMatchEveryName(computeExtcommunitySetRtName("rt1"))));
    {
      CommunitySetMatchExpr expr =
          c.getCommunitySetMatchExprs()
              .get(computeCommunitySetMatchAnyName(computeExtcommunitySetRtName("rt1")));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(ExtendedCommunity.target(1234L, 56L))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(ExtendedCommunity.target(1234L, 57L))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(
                  ExtendedCommunity.target(1234L, 56L), ExtendedCommunity.target(1234L, 57L))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(
                  ExtendedCommunity.target(1234L, 56L),
                  ExtendedCommunity.target(1234L, 57L),
                  ExtendedCommunity.target(1234L, 58L))));
      assertFalse(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(ExtendedCommunity.target(1L, 1L))));
      assertFalse(expr.accept(ctx.getCommunitySetMatchExprEvaluator(), CommunitySet.of()));
    }
    {
      CommunitySetMatchExpr expr =
          c.getCommunitySetMatchExprs()
              .get(computeCommunitySetMatchEveryName(computeExtcommunitySetRtName("rt1")));
      assertFalse(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(ExtendedCommunity.target(1234L, 56L))));
      assertFalse(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(ExtendedCommunity.target(1234L, 57L))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(
                  ExtendedCommunity.target(1234L, 56L),
                  ExtendedCommunity.target(1234L, 57L),
                  ExtendedCommunity.target((12L << 16) | 34L, 56L))));
      assertTrue(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(
                  ExtendedCommunity.target(1234L, 56L),
                  ExtendedCommunity.target(1234L, 57L),
                  ExtendedCommunity.target((12L << 16) | 34L, 56L),
                  ExtendedCommunity.target(1234L, 58L))));
      assertFalse(
          expr.accept(
              ctx.getCommunitySetMatchExprEvaluator(),
              CommunitySet.of(ExtendedCommunity.target(1L, 1L))));
      assertFalse(expr.accept(ctx.getCommunitySetMatchExprEvaluator(), CommunitySet.of()));
    }

    // Test route-policy match and set
    assertThat(
        c.getRoutingPolicies(),
        hasKeys("set-rt1", "set-inline", "set-inline-additive", RESOLUTION_POLICY_NAME));
    Ip origNextHopIp = Ip.parse("192.0.2.254");
    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setAsPath(ofSingletonAsSets(2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(origNextHopIp)
            .setNetwork(Prefix.ZERO)
            .setTag(0L)
            .build();
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set-rt1");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(9, 9), ExtendedCommunity.target(1L, 2L)))
              .build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities().getCommunities(),
          containsInAnyOrder(
              StandardCommunity.of(9, 9),
              ExtendedCommunity.target(1234L, 56L),
              ExtendedCommunity.target(1234L, 57L),
              ExtendedCommunity.target((12L << 16) | 34L, 56L)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set-inline");
      Bgpv4Route inRoute = base.toBuilder().build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities().getCommunities(),
          containsInAnyOrder(
              ExtendedCommunity.target(1L, 1L), ExtendedCommunity.target((1L << 16) | 2, 3L)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set-inline-additive");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(ImmutableSet.of(ExtendedCommunity.target(2L, 2L)))
              .build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities().getCommunities(),
          containsInAnyOrder(ExtendedCommunity.target(1L, 1L), ExtendedCommunity.target(2L, 2L)));
    }
  }

  @Test
  public void testExtcommunitySetRtExtraction() {
    CiscoXrConfiguration c = parseVendorConfig("ios-xr-extcommunity-set-rt");

    assertThat(c.getExtcommunitySetRts(), hasKeys("rt1"));
    {
      ExtcommunitySetRt set = c.getExtcommunitySetRts().get("rt1");
      assertThat(
          set.getElements(),
          contains(
              new ExtcommunitySetRtElemAsColon(new LiteralUint32(1234L), new LiteralUint16(56)),
              new ExtcommunitySetRtElemAsColon(new LiteralUint32(1234L), new LiteralUint16(57)),
              new ExtcommunitySetRtElemAsDotColon(
                  new LiteralUint16(12), new LiteralUint16(34), new LiteralUint16(56))));
    }
  }

  @Test
  public void testOspfInterfaceCost() {
    Configuration c = parseConfig("ospf-interface-cost");
    String ifaceEth1Name = "GigabitEthernet0/0/0/1";
    String ifaceEth2Name = "GigabitEthernet0/0/0/2";
    String ifaceLoop1Name = "Loopback1";
    String ifaceLoop2Name = "Loopback2";
    String ifaceLoop3Name = "Loopback3";
    Map<String, Interface> ifaces = c.getAllInterfaces();
    assertThat(
        ifaces.keySet(),
        contains(ifaceEth1Name, ifaceEth2Name, ifaceLoop1Name, ifaceLoop2Name, ifaceLoop3Name));
    Interface ifaceEth1 = ifaces.get(ifaceEth1Name);
    Interface ifaceEth2 = ifaces.get(ifaceEth2Name);
    Interface ifaceLoop1 = ifaces.get(ifaceLoop1Name);
    Interface ifaceLoop2 = ifaces.get(ifaceLoop2Name);
    Interface ifaceLoop3 = ifaces.get(ifaceLoop3Name);

    // Confirm explicitly configured costs are applied
    assertThat(ifaceEth1.getOspfCost(), equalTo(1));
    assertThat(ifaceLoop3.getOspfCost(), equalTo(12));

    // Confirm other costs are calculated correctly
    assertThat(ifaceEth2.getOspfCost(), equalTo(400));
    assertThat(ifaceLoop1.getOspfCost(), equalTo(1));
    assertThat(ifaceLoop2.getOspfCost(), equalTo(1));
  }

  @Test
  public void testOspfInterface() {
    Configuration c = parseConfig("ospf-interface");
    String ifaceName = "Bundle-Ether201";
    Map<String, Interface> ifaces = c.getAllInterfaces();
    assertThat(ifaces.keySet(), contains(ifaceName));

    // Confirm the interface has the correct OSPF process and area
    assertThat(ifaces.get(ifaceName).getOspfProcess(), equalTo("2"));
    assertThat(ifaces.get(ifaceName).getOspfAreaName(), equalTo(0L));
  }

  @Test
  public void testOspfCost() {
    Configuration manual = parseConfig("ospf-cost");
    assertThat(
        manual.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(), equalTo(10e6d));

    Configuration defaults = parseConfig("ospf-cost-defaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(),
        equalTo(OspfProcess.DEFAULT_OSPF_REFERENCE_BANDWIDTH));
  }

  @Test
  public void testOspfDistributeListExtraction() {
    CiscoXrConfiguration c = parseVendorConfig("ospf-distribute-list");
    assertThat(c.getDefaultVrf().getOspfProcesses(), hasKeys("1", "2"));
    OspfProcess p1 = c.getDefaultVrf().getOspfProcesses().get("1");
    assertThat(
        p1.getInboundGlobalDistributeList(),
        equalTo(new DistributeList("RP", DistributeListFilterType.ROUTE_POLICY)));
    assertThat(
        p1.getOutboundGlobalDistributeList(),
        equalTo(new DistributeList("ACL2", DistributeListFilterType.ACCESS_LIST)));
    OspfProcess p2 = c.getDefaultVrf().getOspfProcesses().get("2");
    assertThat(
        p2.getInboundGlobalDistributeList(),
        equalTo(new DistributeList("ACL3", DistributeListFilterType.ACCESS_LIST)));
    assertThat(p2.getOutboundGlobalDistributeList(), nullValue());
  }

  @Test
  public void testOspfDistributeListConversion() {
    Configuration c = parseConfig("ospf-distribute-list");
    assertThat(c.getDefaultVrf().getOspfProcesses(), hasKeys("1", "2"));

    String iface1Name = "GigabitEthernet0/0/0/1";
    String iface2Name = "GigabitEthernet0/0/0/2";
    OspfInterfaceSettings settings1 = c.getActiveInterfaces().get(iface1Name).getOspfSettings();
    OspfInterfaceSettings settings2 = c.getActiveInterfaces().get(iface2Name).getOspfSettings();
    assert settings1 != null && settings2 != null;

    // First OSPF process uses a routing policy called RP for inbound distribute-list
    assertThat(settings1.getInboundDistributeListPolicy(), equalTo("RP"));
    assertThat(c.getRoutingPolicies(), hasKey("RP"));

    // Second OSPF process uses an ACL for inbound distribute-list.
    // Semantics of the generated routing policy are tested elsewhere.
    String generatedRpName = generatedOspfInboundDistributeListName(DEFAULT_VRF_NAME, "2");
    assertThat(settings2.getInboundDistributeListPolicy(), equalTo(generatedRpName));
    assertThat(c.getRoutingPolicies(), hasKey(generatedRpName));
  }

  @Test
  public void testOspfRedistributionRoutePolicy() {
    String hostname = "ospf-redist-policy";
    Configuration c = parseConfig(hostname);

    Prefix permittedPrefix = Prefix.parse("1.2.3.4/32");
    Prefix permittedPrefix2 = Prefix.parse("1.2.3.5/32");
    Prefix rejectedPrefix = Prefix.parse("2.0.0.0/8");
    Prefix unmatchedPrefix = Prefix.parse("3.0.0.0/8");

    StaticRoute permittedRoute = StaticRoute.testBuilder().setNetwork(permittedPrefix).build();
    StaticRoute permittedRoute2 = StaticRoute.testBuilder().setNetwork(permittedPrefix2).build();
    StaticRoute rejectedRoute = StaticRoute.testBuilder().setNetwork(rejectedPrefix).build();
    StaticRoute unmatchedRoute = StaticRoute.testBuilder().setNetwork(unmatchedPrefix).build();

    org.batfish.datamodel.ospf.OspfProcess ospfProc = c.getDefaultVrf().getOspfProcesses().get("1");
    RoutingPolicy ospfExportPolicy = c.getRoutingPolicies().get(ospfProc.getExportPolicy());

    // Export policy should permit static routes according to the specified redistribution policy
    assertTrue(
        ospfExportPolicy.process(
            permittedRoute,
            OspfExternalRoute.builder().setNextHop(NextHopDiscard.instance()),
            Direction.OUT));
    assertTrue(
        ospfExportPolicy.process(
            permittedRoute2,
            OspfExternalRoute.builder().setNextHop(NextHopDiscard.instance()),
            Direction.OUT));
    assertFalse(
        ospfExportPolicy.process(
            rejectedRoute,
            OspfExternalRoute.builder().setNextHop(NextHopDiscard.instance()),
            Direction.OUT));
    assertFalse(
        ospfExportPolicy.process(
            unmatchedRoute,
            OspfExternalRoute.builder().setNextHop(NextHopDiscard.instance()),
            Direction.OUT));

    // Export policy does not permit routes of OSPF or other protocols, even if the static
    // redistribution policy would permit them
    assertFalse(
        ospfExportPolicy.process(
            ConnectedRoute.builder()
                .setNetwork(permittedPrefix)
                .setNextHop(NextHopInterface.of("iface"))
                .build(),
            OspfExternalRoute.builder().setNextHop(NextHopDiscard.instance()),
            Direction.OUT));
    assertFalse(
        ospfExportPolicy.process(
            OspfExternalType1Route.testBuilder().setNetwork(permittedPrefix).build(),
            OspfExternalRoute.builder().setNextHop(NextHopDiscard.instance()),
            Direction.OUT));
  }

  @Test
  public void testPrefixSet() {
    String hostname = "prefix-set";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);

    Prefix permittedPrefix = Prefix.parse("1.2.3.4/30");
    Prefix6 permittedPrefix6 = Prefix6.parse("2001::ffff:0/124");
    Prefix rejectedPrefix = Prefix.parse("1.2.4.4/30");
    Prefix6 rejectedPrefix6 = Prefix6.parse("2001::fffe:0/124");

    /*
     * Confirm the generated route filter lists permit correct prefixes and do not permit others
     */
    assertThat(c, hasRouteFilterList("pre_ipv4", permits(permittedPrefix)));
    assertThat(c, hasRouteFilterList("pre_ipv4", not(permits(rejectedPrefix))));
    assertThat(c, hasRoute6FilterList("pre_ipv6", permits(permittedPrefix6)));
    assertThat(c, hasRoute6FilterList("pre_ipv6", not(permits(rejectedPrefix6))));
    assertThat(c, hasRouteFilterList("pre_combo", permits(permittedPrefix)));
    assertThat(c, hasRouteFilterList("pre_combo", not(permits(rejectedPrefix))));
    assertThat(c, hasRoute6FilterList("pre_combo", permits(permittedPrefix6)));
    assertThat(c, hasRoute6FilterList("pre_combo", not(permits(rejectedPrefix6))));

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    String filename = "configs/" + hostname;
    /*
     * pre_combo should be the only prefix set without a referrer
     */
    assertThat(ccae, hasNumReferrers(filename, PREFIX_SET, "pre_ipv4", 1));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_SET, "pre_ipv6", 1));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_SET, "pre_combo", 0));

    /*
     * pre_undef should be the only undefined reference
     */
    assertThat(ccae, not(hasUndefinedReference(filename, PREFIX_SET, "pre_ipv4")));
    assertThat(ccae, not(hasUndefinedReference(filename, PREFIX_SET, "pre_ipv6")));
    assertThat(ccae, hasUndefinedReference(filename, PREFIX_SET, "pre_undef"));
  }

  @Test
  public void testRoutePolicyDone() {
    String hostname = "route-policy-done";
    Configuration c = parseConfig(hostname);

    Prefix permittedPrefix = Prefix.parse("1.2.3.4/32");
    Prefix permittedPrefix2 = Prefix.parse("1.2.3.5/32");
    Prefix rejectedPrefix = Prefix.parse("2.0.0.0/8");

    StaticRoute permittedRoute =
        StaticRoute.testBuilder().setAdministrativeCost(1).setNetwork(permittedPrefix).build();
    StaticRoute permittedRoute2 =
        StaticRoute.testBuilder().setAdministrativeCost(1).setNetwork(permittedPrefix2).build();
    StaticRoute rejectedRoute =
        StaticRoute.testBuilder().setAdministrativeCost(1).setNetwork(rejectedPrefix).build();

    // The route-policy accepts and rejects the same prefixes.
    RoutingPolicy rp = c.getRoutingPolicies().get("rp_ip");
    assertThat(rp, notNullValue());
    assertTrue(rp.process(permittedRoute, Bgpv4Route.testBuilder(), Direction.OUT));
    assertTrue(rp.process(permittedRoute2, Bgpv4Route.testBuilder(), Direction.OUT));
    assertFalse(rp.process(rejectedRoute, Bgpv4Route.testBuilder(), Direction.OUT));

    // The BGP peer export policy also accepts and rejects the same prefixes.
    BgpActivePeerConfig bgpCfg =
        c.getDefaultVrf().getBgpProcess().getActiveNeighbors().get(Prefix.parse("10.1.1.1/32"));
    assertThat(bgpCfg, notNullValue());
    RoutingPolicy bgpRpOut =
        c.getRoutingPolicies().get(bgpCfg.getIpv4UnicastAddressFamily().getExportPolicy());
    assertThat(bgpRpOut, notNullValue());

    assertTrue(bgpRpOut.process(permittedRoute, Bgpv4Route.testBuilder(), Direction.OUT));
    assertTrue(bgpRpOut.process(permittedRoute2, Bgpv4Route.testBuilder(), Direction.OUT));
    assertFalse(bgpRpOut.process(rejectedRoute, Bgpv4Route.testBuilder(), Direction.OUT));
  }

  @Test
  public void testRoutePolicyValidationStateExtraction() {
    CiscoXrConfiguration c = parseVendorConfig("rp-validation-state");
    assertThat(c.getRoutePolicies(), hasKeys("validation-state-testing"));
    RoutePolicy p = c.getRoutePolicies().get("validation-state-testing");
    assertThat(p.getStatements(), contains(instanceOf(RoutePolicyIfStatement.class)));
    RoutePolicyIfStatement ifs = (RoutePolicyIfStatement) p.getStatements().get(0);
    assertThat(ifs.getGuard(), equalTo(new RoutePolicyBooleanValidationStateIs(false)));
    assertThat(ifs.getElseBlock(), nullValue());
    assertThat(ifs.getStatements(), hasSize(1));
    assertThat(
        ifs.getStatements(),
        contains(new RoutePolicyDispositionStatement(RoutePolicyDispositionType.DROP)));
    assertThat(ifs.getElseIfBlocks(), hasSize(1));
    RoutePolicyElseIfBlock elses = Iterables.getOnlyElement(ifs.getElseIfBlocks());
    assertThat(elses.getGuard(), equalTo(new RoutePolicyBooleanValidationStateIs(true)));
    assertThat(
        elses.getStatements(),
        contains(new RoutePolicyDispositionStatement(RoutePolicyDispositionType.PASS)));
  }

  @Test
  public void testPolicyMap() {
    String hostname = "policy-map";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    String filename = "configs/" + hostname;

    assertThat(ccae, hasNumReferrers(filename, POLICY_MAP, "POLICY-MAP", 1));
    assertThat(ccae, hasNumReferrers(filename, CLASS_MAP, "PPP", 3));

    assertThat(ccae, hasNumReferrers(filename, POLICY_MAP, "PM", 0));
    assertThat(ccae, hasUndefinedReference(filename, DYNAMIC_TEMPLATE, "TEMPLATE1"));

    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());

    assertThat(pvcae, hasParseWarning(filename, containsString("Policy map of type accounting")));
    assertThat(pvcae, hasParseWarning(filename, containsString("Policy map of type qos")));
    assertThat(pvcae, hasParseWarning(filename, containsString("Policy map of type pbr")));
    assertThat(pvcae, hasParseWarning(filename, containsString("Policy map of type redirect")));
    assertThat(pvcae, hasParseWarning(filename, containsString("Policy map of type traffic")));
    assertThat(
        pvcae, hasParseWarning(filename, containsString("Policy map of type performance-traffic")));
  }

  @Test
  public void testVrfRouteTargetExtraction() {
    String hostname = "xr-vrf-route-target";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);
    assertThat(
        vc.getVrfs(),
        hasKeys(
            DEFAULT_VRF_NAME, "none", "single-oneline", "single-block", "multiple", "multiple-af"));
    {
      Vrf v = vc.getVrfs().get("none");
      assertThat(v.getIpv4UnicastAddressFamily().getRouteTargetExport(), empty());
      assertThat(v.getIpv4UnicastAddressFamily().getRouteTargetImport(), empty());
    }
    {
      Vrf v = vc.getVrfs().get("single-oneline");
      assertThat(
          v.getIpv4UnicastAddressFamily().getRouteTargetExport(),
          contains(ExtendedCommunity.target(1L, 1L)));
      assertThat(
          v.getIpv4UnicastAddressFamily().getRouteTargetImport(),
          contains(ExtendedCommunity.target(2L, 2L)));
    }
    {
      Vrf v = vc.getVrfs().get("single-block");
      assertThat(
          v.getIpv4UnicastAddressFamily().getRouteTargetExport(),
          contains(ExtendedCommunity.target(3L, 3L)));
      assertThat(
          v.getIpv4UnicastAddressFamily().getRouteTargetImport(),
          contains(ExtendedCommunity.target(4L, 4L)));
    }
    {
      Vrf v = vc.getVrfs().get("multiple");
      assertThat(
          v.getIpv4UnicastAddressFamily().getRouteTargetExport(),
          contains(ExtendedCommunity.target(5L, 5L), ExtendedCommunity.target(6L, 6L)));
      assertThat(
          v.getIpv4UnicastAddressFamily().getRouteTargetImport(),
          contains(
              ExtendedCommunity.target(7L, 7L),
              ExtendedCommunity.target(8L, 9L),
              ExtendedCommunity.target(((10L << 16) + 11L), 12L)));
    }
    {
      Vrf v = vc.getVrfs().get("multiple-af");
      assertThat(
          v.getAddressFamilies().get(AddressFamilyType.IPV4_UNICAST).getRouteTargetExport(),
          contains(ExtendedCommunity.target(1L, 13L)));
      assertThat(
          v.getAddressFamilies().get(AddressFamilyType.IPV4_MULTICAST).getRouteTargetExport(),
          contains(ExtendedCommunity.target(1L, 14L)));
      assertThat(
          v.getAddressFamilies().get(AddressFamilyType.IPV4_FLOWSPEC).getRouteTargetExport(),
          contains(ExtendedCommunity.target(1L, 15L)));
      assertThat(
          v.getAddressFamilies().get(AddressFamilyType.IPV6_UNICAST).getRouteTargetExport(),
          contains(ExtendedCommunity.target(1L, 16L)));
      assertThat(
          v.getAddressFamilies().get(AddressFamilyType.IPV6_MULTICAST).getRouteTargetExport(),
          contains(ExtendedCommunity.target(1L, 17L)));
      assertThat(
          v.getAddressFamilies().get(AddressFamilyType.IPV6_FLOWSPEC).getRouteTargetExport(),
          contains(ExtendedCommunity.target(1L, 18L)));
    }
  }

  @Test
  public void testVrfRoutePolicyExtraction() {
    String hostname = "xr-vrf-route-policy";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);
    assertThat(vc.getVrfs(), hasKeys(DEFAULT_VRF_NAME, "v0", "v1", "v2"));
    {
      Vrf v = vc.getVrfs().get("v0");
      assertThat(v.getIpv4UnicastAddressFamily().getExportPolicy(), nullValue());
      assertThat(v.getIpv4UnicastAddressFamily().getExportToDefaultVrfPolicy(), nullValue());
      assertThat(v.getIpv4UnicastAddressFamily().getImportPolicy(), nullValue());
      assertThat(v.getIpv4UnicastAddressFamily().getImportFromDefaultVrfPolicy(), nullValue());
    }
    {
      Vrf v = vc.getVrfs().get("v1");
      assertThat(v.getIpv4UnicastAddressFamily().getExportPolicy(), equalTo("p1"));
      assertThat(v.getIpv4UnicastAddressFamily().getExportToDefaultVrfPolicy(), equalTo("p2"));
      assertThat(v.getIpv4UnicastAddressFamily().getImportPolicy(), equalTo("p3"));
      assertThat(v.getIpv4UnicastAddressFamily().getImportFromDefaultVrfPolicy(), equalTo("p4"));
    }
    {
      Vrf v = vc.getVrfs().get("v2");
      assertThat(v.getIpv4UnicastAddressFamily(), nullValue());
    }
  }

  @Test
  public void testVrfRoutePolicyReferences() {
    String hostname = "xr-vrf-route-policy";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(ccae, hasReferencedStructure(filename, ROUTE_POLICY, "p1", VRF_EXPORT_ROUTE_POLICY));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, ROUTE_POLICY, "p2", VRF_EXPORT_TO_DEFAULT_VRF_ROUTE_POLICY));
    assertThat(ccae, hasReferencedStructure(filename, ROUTE_POLICY, "p3", VRF_IMPORT_ROUTE_POLICY));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, ROUTE_POLICY, "p4", VRF_IMPORT_FROM_DEFAULT_VRF_ROUTE_POLICY));
  }

  @Test
  public void testAccessListReferences() {
    String hostname = "xr-access-list-refs";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // ipv4
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl1", INTERFACE_IPV4_ACCESS_GROUP_COMMON));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl2", INTERFACE_IPV4_ACCESS_GROUP_INGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl3", INTERFACE_IPV4_ACCESS_GROUP_EGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl4", NTP_ACCESS_GROUP_PEER));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl5", NTP_ACCESS_GROUP_QUERY_ONLY));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl6", NTP_ACCESS_GROUP_SERVE));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl7", NTP_ACCESS_GROUP_SERVE_ONLY));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename,
            IPV4_ACCESS_LIST,
            "ipv4acl8",
            MPLS_LDP_AF_IPV4_DISCOVERY_TARGETED_HELLO_ACCEPT_FROM));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename,
            IPV4_ACCESS_LIST,
            "ipv4acl9",
            MPLS_LDP_AF_IPV4_REDISTRIBUTE_BGP_ADVERTISE_TO));

    // ipv6
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl1", INTERFACE_IPV6_ACCESS_GROUP_COMMON));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl2", INTERFACE_IPV6_ACCESS_GROUP_INGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl3", INTERFACE_IPV6_ACCESS_GROUP_EGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl4", NTP_ACCESS_GROUP_PEER));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl5", NTP_ACCESS_GROUP_QUERY_ONLY));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl6", NTP_ACCESS_GROUP_SERVE));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl7", NTP_ACCESS_GROUP_SERVE_ONLY));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename,
            IPV6_ACCESS_LIST,
            "ipv6acl8",
            MPLS_LDP_AF_IPV6_DISCOVERY_TARGETED_HELLO_ACCEPT_FROM));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename,
            IPV6_ACCESS_LIST,
            "ipv6acl9",
            MPLS_LDP_AF_IPV6_REDISTRIBUTE_BGP_ADVERTISE_TO));
  }

  @Test
  public void testBfdParsing() {
    String hostname = "xr-bfd";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testIpv6AccessListParsing() {
    String hostname = "xr-ipv6-access-list";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testInterfaceAddressExtraction() {
    String hostname = "xr-interface-address";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    String i1Name = "GigabitEthernet0/0/0/0";

    assertThat(vc.getInterfaces(), hasKeys(i1Name));

    {
      org.batfish.representation.cisco_xr.Interface iface = vc.getInterfaces().get(i1Name);
      ConcreteInterfaceAddress primary = ConcreteInterfaceAddress.parse("10.0.0.1/31");
      ConcreteInterfaceAddress secondary1 = ConcreteInterfaceAddress.parse("10.0.0.3/31");
      ConcreteInterfaceAddress secondary2 = ConcreteInterfaceAddress.parse("10.0.0.5/31");

      assertThat(iface.getAllAddresses(), containsInAnyOrder(primary, secondary1, secondary2));
      assertThat(iface.getAddress(), equalTo(primary));
      assertThat(iface.getSecondaryAddresses(), containsInAnyOrder(secondary1, secondary2));
    }
  }

  @Test
  public void testRdSetExtraction() {
    String hostname = "rd-set";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getRdSets(), hasKeys("mixed", "universe"));
    {
      RdSet set = vc.getRdSets().get("mixed");
      assertThat(
          set.getElements(),
          contains(
              new RdSetDfaRegex("_5678:.*"),
              new RdSetIosRegex("_1234:.*"),
              new RdSetAsDot(new LiteralUint16(1), new LiteralUint16(2), new LiteralUint16(3)),
              new RdSetAsPlain16(new LiteralUint16(4), new LiteralUint32(5)),
              new RdSetAsPlain32(new LiteralUint32(600000L), new LiteralUint16(6)),
              new RdSetAsPlain32(WildcardUint32RangeExpr.instance(), new LiteralUint16(7)),
              new RdSetAsPlain32(new LiteralUint32(800000L), WildcardUint16RangeExpr.instance()),
              new RdSetAsDot(
                  new LiteralUint16(9), WildcardUint16RangeExpr.instance(), new LiteralUint16(10)),
              /* TODO: should this be something like WildcardPint16RangeExpr, since you cannot enter 0
              as first component when using asdot? */
              new RdSetAsDot(
                  WildcardUint16RangeExpr.instance(), new LiteralUint16(0), new LiteralUint16(11)),
              new RdSetIpPrefix(Prefix.strict("1.1.1.0/24"), new LiteralUint16(3)),
              new RdSetIpAddress(Ip.parse("4.4.4.4"), new LiteralUint16(5))));
    }
    {
      RdSet set = vc.getRdSets().get("universe");
      assertThat(
          set.getElements(),
          contains(
              new RdSetAsPlain32(
                  WildcardUint32RangeExpr.instance(), WildcardUint16RangeExpr.instance())));
    }
  }

  @Test
  public void testBgpParsing() {
    String hostname = "xr-bgp";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testRdSetReferences() {
    String hostname = "xr-rd-set-refs";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // ipv4
    assertThat(ccae, hasReferencedStructure(filename, RD_SET, "rdset1", ROUTE_POLICY_RD_IN));
  }

  @Test
  public void testMiscIgnoredParsing() {
    String hostname = "xr-misc-ignored";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    // Do not crash
    assertNotNull(vc);

    // Also make sure context isn't lost when hitting ignored lines, specifically:
    // Interface is still associated with bridge-domain, not interpreted as top-level
    assertThat(
        vc.getBridgeGroups().get("BG1").getBridgeDomains().get("BD1").getInterfaces(),
        contains("GigabitEthernet0/0/0/1.123"));
  }

  @Test
  public void testHostNameDomain() {
    Configuration c = parseConfig("xr-hostname.domain");
    assertThat(c.getHumanName(), equalTo("xr-hostname.domain"));
  }

  @Test
  public void testHumanName() {
    Configuration c = parseConfig("xr-humanname");
    assertThat(c.getHumanName(), equalTo("XR-humanname"));
  }

  @Test
  public void testIgmpReferences() {
    String hostname = "xr-igmp-references";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl1", ROUTER_IGMP_ACCESS_GROUP));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl2", ROUTER_IGMP_EXPLICIT_TRACKING));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl3", ROUTER_IGMP_MAXIMUM_GROUPS_PER_INTERFACE));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl4", ROUTER_IGMP_SSM_MAP_STATIC));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl5", ROUTER_IGMP_ACCESS_GROUP));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl6", ROUTER_IGMP_EXPLICIT_TRACKING));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl7", ROUTER_IGMP_MAXIMUM_GROUPS_PER_INTERFACE));
  }

  @Test
  public void testMldReferences() {
    String hostname = "xr-mld-references";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl1", ROUTER_MLD_ACCESS_GROUP));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl2", ROUTER_MLD_EXPLICIT_TRACKING));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl3", ROUTER_MLD_MAXIMUM_GROUPS_PER_INTERFACE));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl4", ROUTER_MLD_SSM_MAP_STATIC));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl5", ROUTER_MLD_ACCESS_GROUP));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl6", ROUTER_MLD_EXPLICIT_TRACKING));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl7", ROUTER_MLD_MAXIMUM_GROUPS_PER_INTERFACE));
  }

  @Test
  public void testPimReferences() {
    String hostname = "xr-pim-references";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // route-policy
    assertThat(
        ccae, hasReferencedStructure(filename, ROUTE_POLICY, "rp1", ROUTER_PIM_RPF_TOPOLOGY));

    // ipv4 acls
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl1", ROUTER_PIM_ACCEPT_REGISTER));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl2", ROUTER_PIM_ALLOW_RP_GROUP_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl3", ROUTER_PIM_ALLOW_RP_RP_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl4", ROUTER_PIM_BSR_CANDIDATE_RP));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl5", ROUTER_PIM_MDT_NEIGHBOR_FILTER));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl6", ROUTER_PIM_MOFRR_FLOW));
    assertThat(
        ccae, hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl7", ROUTER_PIM_MOFRR_RIB));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl8", ROUTER_PIM_NEIGHBOR_FILTER));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl9", ROUTER_PIM_RP_ADDRESS));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl10", ROUTER_PIM_RP_STATIC_DENY));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl11", ROUTER_PIM_SG_EXPIRY_TIMER));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl12", ROUTER_PIM_SPT_THRESHOLD));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl13", ROUTER_PIM_SSM_THRESHOLD_RANGE));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl14", ROUTER_PIM_AUTO_RP_CANDIDATE_RP_GROUP_LIST));

    // ipv6 acls
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl1", ROUTER_PIM_ACCEPT_REGISTER));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl2", ROUTER_PIM_ALLOW_RP_GROUP_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl3", ROUTER_PIM_ALLOW_RP_RP_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl4", ROUTER_PIM_BSR_CANDIDATE_RP));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl5", ROUTER_PIM_MDT_NEIGHBOR_FILTER));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl6", ROUTER_PIM_MOFRR_FLOW));
    assertThat(
        ccae, hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl7", ROUTER_PIM_MOFRR_RIB));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl8", ROUTER_PIM_NEIGHBOR_FILTER));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl9", ROUTER_PIM_RP_ADDRESS));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl10", ROUTER_PIM_RP_STATIC_DENY));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl11", ROUTER_PIM_SG_EXPIRY_TIMER));
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl12", ROUTER_PIM_SPT_THRESHOLD));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV6_ACCESS_LIST, "ipv6acl13", ROUTER_PIM_SSM_THRESHOLD_RANGE));
  }

  @Test
  public void testMsdpReferences() {
    String hostname = "xr-msdp-references";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl1", ROUTER_MSDP_CACHE_SA_STATE_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl2", ROUTER_MSDP_CACHE_SA_STATE_RP_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl3", ROUTER_MSDP_SA_FILTER_IN_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl4", ROUTER_MSDP_SA_FILTER_IN_RP_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl5", ROUTER_MSDP_SA_FILTER_OUT_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl6", ROUTER_MSDP_SA_FILTER_OUT_RP_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl7", ROUTER_MSDP_SA_FILTER_IN_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl8", ROUTER_MSDP_SA_FILTER_IN_RP_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl9", ROUTER_MSDP_SA_FILTER_OUT_LIST));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, IPV4_ACCESS_LIST, "ipv4acl10", ROUTER_MSDP_SA_FILTER_OUT_RP_LIST));
  }

  @Test
  public void testMulticastRoutingReferences() {
    String hostname = "xr-multicast-routing-references";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // ipv4
    assertThat(
        ccae,
        hasReferencedStructure(
            filename,
            IPV4_ACCESS_LIST,
            "ipv4acl1",
            MULTICAST_ROUTING_CORE_TREE_PROTOCOL_RSVP_TE_GROUP_LIST));

    // ipv6
    assertThat(
        ccae,
        hasReferencedStructure(
            filename,
            IPV6_ACCESS_LIST,
            "ipv6acl1",
            MULTICAST_ROUTING_CORE_TREE_PROTOCOL_RSVP_TE_GROUP_LIST));
  }

  @Test
  public void testFlowReferences() {
    String hostname = "xr-flow";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae,
        hasReferencedStructure(filename, FLOW_EXPORTER_MAP, "fem1", FLOW_MONITOR_MAP_EXPORTER));
    assertThat(
        ccae,
        hasReferencedStructure(filename, FLOW_EXPORTER_MAP, "fem2", FLOW_MONITOR_MAP_EXPORTER));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, FLOW_MONITOR_MAP, "fmm1", INTERFACE_FLOW_IPV4_MONITOR_EGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, FLOW_MONITOR_MAP, "fmm2", INTERFACE_FLOW_IPV4_MONITOR_INGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, FLOW_MONITOR_MAP, "fmm3", INTERFACE_FLOW_IPV6_MONITOR_EGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, FLOW_MONITOR_MAP, "fmm4", INTERFACE_FLOW_IPV6_MONITOR_INGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(filename, SAMPLER_MAP, "sm1", INTERFACE_FLOW_IPV4_SAMPLER_EGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(filename, SAMPLER_MAP, "sm2", INTERFACE_FLOW_IPV4_SAMPLER_INGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(filename, SAMPLER_MAP, "sm3", INTERFACE_FLOW_IPV6_SAMPLER_EGRESS));
    assertThat(
        ccae,
        hasReferencedStructure(filename, SAMPLER_MAP, "sm4", INTERFACE_FLOW_IPV6_SAMPLER_INGRESS));
  }

  @Test
  public void testMplsParsing() {
    String hostname = "xr-mpls";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testSnmpServerParsing() {
    String hostname = "xr-snmp-server";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // ipv4
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV4_ACCESS_LIST, "ipv4acl1", SNMP_SERVER_COMMUNITY_ACL4));

    // ipv6
    assertThat(
        ccae,
        hasReferencedStructure(filename, IPV6_ACCESS_LIST, "ipv6acl1", SNMP_SERVER_COMMUNITY_ACL6));
  }

  @Test
  public void testOspfParsing() {
    String hostname = "xr-ospf";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testTftpParsing() {
    String hostname = "xr-tftp";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testEthernetServicesDefinitions() {
    String hostname = "xr-ethernet-services";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(ccae, hasDefinedStructure(filename, ETHERNET_SERVICES_ACCESS_LIST, "esacl1"));
  }

  @Test
  public void testLogging() {
    String hostname = "xr-logging";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testDscpExtraction() {
    String hostname = "xr-dscp";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);
    String aclName = "ipv4dscpacl";

    assertThat(vc.getIpv4Acls(), hasKeys(aclName));

    Ipv4AccessList acl = vc.getIpv4Acls().get(aclName);
    Iterator<Ipv4AccessListLine> i = acl.getLines().iterator();
    acl.getLines()
        .forEach(
            line ->
                assertThat(
                    line.getServiceSpecifier(),
                    instanceOf(SimpleExtendedAccessListServiceSpecifier.class)));
    Ipv4AccessListLine line;
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF11.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF12.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF13.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF21.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF22.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF23.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF31.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF32.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF33.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF41.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF42.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.AF43.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.CS1.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.CS2.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.CS3.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.CS4.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.CS5.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.CS6.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.CS7.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.DEFAULT.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(DscpType.EF.number()));
    }
    {
      line = i.next();
      assertThat(
          ((SimpleExtendedAccessListServiceSpecifier) line.getServiceSpecifier()).getDscps(),
          contains(1));
    }
    assertFalse(i.hasNext());
  }

  @Test
  public void testBgpRedistributionRoutePolicy() {
    /*
     * Config has static routes 1.2.3.4/32, 1.2.3.5/32, and 2.0.0.0/8. Static routes are
     * redistributed into BGP with a route-map that permits the first two (using keywords "done" and
     * "pass", respectively) and denies everything else.
     *
     * Should see BGP copies of the first two static routes in the BGP RIB as local routes.
     */
    String hostname = "bgp-redist-policy";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    Set<Bgpv4Route> bgpRibRoutes = dp.getBgpRoutes().get(hostname, Configuration.DEFAULT_VRF_NAME);
    Ip routerId = Ip.parse("1.1.1.1");
    Prefix permittedPrefix1 = Prefix.parse("1.2.3.4/32");
    Prefix permittedPrefix2 = Prefix.parse("1.2.3.5/32");
    Bgpv4Route expectedRoute1 =
        Bgpv4Route.builder()
            .setNetwork(permittedPrefix1)
            .setNonRouting(true)
            .setAdmin(20)
            .setLocalPreference(100)
            .setNextHop(NextHopDiscard.instance())
            .setOriginatorIp(routerId)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setReceivedFromIp(Ip.ZERO) // indicates local origination
            .setSrcProtocol(RoutingProtocol.STATIC)
            .build();
    Bgpv4Route expectedRoute2 = expectedRoute1.toBuilder().setNetwork(permittedPrefix2).build();
    assertThat(bgpRibRoutes, containsInAnyOrder(expectedRoute1, expectedRoute2));

    // Sanity check: Make sure main RIB has all 3 static routes and no BGP routes
    Set<AbstractRoute> mainRibRoutes =
        dp.getRibs().get(hostname).get(Configuration.DEFAULT_VRF_NAME).getRoutes();
    assertThat(mainRibRoutes, hasSize(3));
    assertThat(mainRibRoutes, everyItem(instanceOf(StaticRoute.class)));
  }

  @Test
  public void testAggregateAddressExtraction() {
    String hostname = "xr-aggregate-address";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);
    Map<Prefix, BgpAggregateIpv4Network> aggs =
        vc.getDefaultVrf().getBgpProcess().getAggregateNetworks();
    assertThat(aggs, aMapWithSize(7));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("1.1.0.0/16")),
            equalTo(new BgpAggregateIpv4Network(false, Prefix.parse("1.1.0.0/16"), null, false))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("1.2.0.0/16")),
            equalTo(
                new BgpAggregateIpv4Network(false, Prefix.parse("1.2.0.0/16"), "gen1", false))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("2.1.0.0/16")),
            equalTo(new BgpAggregateIpv4Network(false, Prefix.parse("2.1.0.0/16"), null, true))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("2.2.0.0/16")),
            equalTo(new BgpAggregateIpv4Network(false, Prefix.parse("2.2.0.0/16"), "gen2", true))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.1.0.0/16")),
            equalTo(new BgpAggregateIpv4Network(true, Prefix.parse("3.1.0.0/16"), null, false))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.2.0.0/16")),
            equalTo(new BgpAggregateIpv4Network(true, Prefix.parse("3.2.0.0/16"), "gen3", false))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("4.0.0.0/16")),
            equalTo(
                new BgpAggregateIpv4Network(
                    false, Prefix.parse("4.0.0.0/16"), "undefined", false))));
  }

  @Test
  public void testAggregateAddressConversion() {
    String hostname = "xr-aggregate-address";
    Configuration c = parseConfig(hostname);

    Map<Prefix, BgpAggregate> aggs = c.getDefaultVrf().getBgpProcess().getAggregates();
    assertThat(aggs, aMapWithSize(7));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("1.1.0.0/16")),
            equalTo(BgpAggregate.of(Prefix.parse("1.1.0.0/16"), null, null, null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("1.2.0.0/16")),
            equalTo(BgpAggregate.of(Prefix.parse("1.2.0.0/16"), null, "gen1", null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("2.1.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("2.1.0.0/16"),
                    SUMMARY_ONLY_SUPPRESSION_POLICY_NAME,
                    null,
                    null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("2.2.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("2.2.0.0/16"),
                    SUMMARY_ONLY_SUPPRESSION_POLICY_NAME,
                    "gen2",
                    null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.1.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("3.1.0.0/16"),
                    null,
                    // TODO: should be generated policy when inheritance is implemented
                    null,
                    null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("3.2.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("3.2.0.0/16"),
                    null,
                    // TODO: should be generated policy when inheritance is implemented
                    "gen3",
                    null))));
    assertThat(
        aggs,
        hasEntry(
            equalTo(Prefix.parse("4.0.0.0/16")),
            equalTo(
                BgpAggregate.of(
                    Prefix.parse("4.0.0.0/16"),
                    null,
                    // undefined -> null for best effort on invalid config
                    null,
                    null))));
  }

  @Test
  public void testBgpAggregateWithLocalSuppressedRoutes() {
    /*
     * Config has static routes:
     * - 1.1.1.0/24
     * - 2.2.2.0/24
     * - 3.0.0.0/8
     * - 4.4.4.0/24
     * - 5.5.0.0/16
     *
     * BGP is configured to unconditionally redistribute static routes,
     * and has aggregates:
     * 1.1.0.0/16 (not summary-only)
     * 2.2.0.0/16 (summary-only)
     * 3.0.0.0/16 (summary-only)
     * 4.4.0.0/16 (summary-only)
     * 4.4.4.0/31 (summary-only)
     * 5.5.0.0/16 (summary-only)
     *
     * In the BGP RIB, we should see:
     * - all local routes
     * - the 3 aggregate routes with more specific local routes:
     *   - 1.1.0/0/16
     *   - 2.2.0.0/16
     *   - 4.4.0.0/16
     *
     * In the main RIB, we should see the static routes and the 3 aggregates activated in the BGP RIB.
     */
    String hostname = "bgp-aggregate";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    // TODO: change to local bgp cost once supported
    int aggAdmin =
        batfish
            .loadConfigurations(batfish.getSnapshot())
            .get(hostname)
            .getDefaultVrf()
            .getBgpProcess()
            .getAdminCost(RoutingProtocol.IBGP);
    Set<Bgpv4Route> bgpRibRoutes = dp.getBgpRoutes().get(hostname, Configuration.DEFAULT_VRF_NAME);
    Ip routerId = Ip.parse("1.1.1.1");
    Prefix staticPrefix1 = Prefix.parse("1.1.1.0/24");
    Prefix staticPrefix2 = Prefix.parse("2.2.2.0/24");
    Prefix staticPrefix3 = Prefix.parse("3.0.0.0/8");
    Prefix staticPrefix4 = Prefix.parse("4.4.4.0/24");
    Prefix staticPrefix5 = Prefix.parse("5.5.0.0/16");
    Prefix aggPrefix1 = Prefix.parse("1.1.0.0/16");
    Prefix aggPrefix2 = Prefix.parse("2.2.0.0/16");
    Prefix aggPrefix4General = Prefix.parse("4.4.0.0/16");
    Bgpv4Route localRoute1 =
        Bgpv4Route.builder()
            .setNetwork(staticPrefix1)
            .setNonRouting(true)
            .setAdmin(20)
            .setLocalPreference(100)
            .setNextHop(NextHopDiscard.instance())
            .setOriginatorIp(routerId)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setReceivedFromIp(Ip.ZERO) // indicates local origination
            .setSrcProtocol(RoutingProtocol.STATIC)
            .build();
    Bgpv4Route localRoute2 = localRoute1.toBuilder().setNetwork(staticPrefix2).build();
    Bgpv4Route localRoute3 = localRoute1.toBuilder().setNetwork(staticPrefix3).build();
    Bgpv4Route localRoute4 = localRoute1.toBuilder().setNetwork(staticPrefix4).build();
    Bgpv4Route localRoute5 = localRoute1.toBuilder().setNetwork(staticPrefix5).build();
    Bgpv4Route aggRoute1 =
        Bgpv4Route.builder()
            .setNetwork(aggPrefix1)
            .setAdmin(aggAdmin)
            .setLocalPreference(100)
            .setNextHop(NextHopDiscard.instance())
            .setOriginatorIp(routerId)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.AGGREGATE)
            .setReceivedFromIp(Ip.ZERO) // indicates local origination
            .setSrcProtocol(RoutingProtocol.AGGREGATE)
            .build();
    Bgpv4Route aggRoute2 = aggRoute1.toBuilder().setNetwork(aggPrefix2).build();
    Bgpv4Route aggRoute4General = aggRoute1.toBuilder().setNetwork(aggPrefix4General).build();
    assertThat(
        bgpRibRoutes,
        containsInAnyOrder(
            localRoute1,
            localRoute2,
            localRoute3,
            localRoute4,
            localRoute5,
            aggRoute1,
            aggRoute2,
            aggRoute4General));

    Set<AbstractRoute> mainRibRoutes =
        dp.getRibs().get(hostname).get(Configuration.DEFAULT_VRF_NAME).getRoutes();
    assertThat(mainRibRoutes, hasItem(hasPrefix(aggPrefix1)));
    assertThat(mainRibRoutes, hasItem(hasPrefix(aggPrefix2)));
    assertThat(mainRibRoutes, hasItem(hasPrefix(aggPrefix4General)));
  }

  @Test
  public void testBgpAggregateWithLearnedSuppressedRoutes() throws IOException {
    /*
     * Snapshot contains c1, c2, and c3. c1 redistributes static routes 1.1.1.0/16 and 2.2.2.0/16
     * into BGP and advertises them to c2. c2 has aggregates 1.1.0.0/16 (not summary-only) and
     * 2.2.0.0/16 (summary-only). c2 advertises both aggregates and 1.1.1.0/16 to c3 (not
     * 2.2.2.0/16, which is suppressed by the summary-only aggregate).
     *
     * c1 should also receive c2's aggregate routes. Worth checking in addition to c3's routes
     * because the c1-c2 peering is IBGP, whereas the c2-c3 peering is EBGP.
     */
    String snapshotName = "bgp-agg-learned-contributors";
    String c1 = "c1";
    String c2 = "c2";
    String c3 = "c3";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(
                    SNAPSHOTS_PREFIX + snapshotName, ImmutableList.of(c1, c2, c3))
                .build(),
            _folder);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    // TODO: change to local bgp cost once supported
    int aggAdmin =
        batfish
            .loadConfigurations(batfish.getSnapshot())
            .get(c1)
            .getDefaultVrf()
            .getBgpProcess()
            .getAdminCost(RoutingProtocol.IBGP);

    Prefix learnedPrefix1 = Prefix.parse("1.1.1.0/24");
    Prefix learnedPrefix2 = Prefix.parse("2.2.2.0/24");
    Prefix aggPrefix1 = Prefix.parse("1.1.0.0/16");
    Prefix aggPrefix2 = Prefix.parse("2.2.0.0/16");
    {
      // Check c2 routes.
      Set<Bgpv4Route> bgpRibRoutes = dp.getBgpRoutes().get(c2, Configuration.DEFAULT_VRF_NAME);
      Bgpv4Route aggRoute1 =
          Bgpv4Route.builder()
              .setNetwork(aggPrefix1)
              .setAdmin(aggAdmin)
              .setLocalPreference(100)
              .setNextHop(NextHopDiscard.instance())
              .setOriginatorIp(Ip.parse("2.2.2.2"))
              .setOriginType(OriginType.IGP)
              .setProtocol(RoutingProtocol.AGGREGATE)
              .setReceivedFromIp(Ip.ZERO) // indicates local origination
              .setSrcProtocol(RoutingProtocol.AGGREGATE)
              .build();
      Bgpv4Route aggRoute2 = aggRoute1.toBuilder().setNetwork(aggPrefix2).build();
      assertThat(
          bgpRibRoutes,
          containsInAnyOrder(
              hasPrefix(learnedPrefix1),
              // TODO Once we mark routes as suppressed, assert this one is suppressed
              hasPrefix(learnedPrefix2),
              equalTo(aggRoute1),
              equalTo(aggRoute2)));
      Set<AbstractRoute> mainRibRoutes =
          dp.getRibs().get(c2).get(Configuration.DEFAULT_VRF_NAME).getRoutes();
      assertThat(mainRibRoutes, hasItem(hasPrefix(learnedPrefix1)));
      // Suppressed routes still go in the main RIB and are used for forwarding
      assertThat(mainRibRoutes, hasItem(hasPrefix(learnedPrefix2)));
      assertThat(mainRibRoutes, hasItem(hasPrefix(aggPrefix1)));
      assertThat(mainRibRoutes, hasItem(hasPrefix(aggPrefix2)));
    }
    {
      // Check c1 routes. (Has both learned routes because it originates them itself.)
      Set<Bgpv4Route> bgpRibRoutes = dp.getBgpRoutes().get(c1, Configuration.DEFAULT_VRF_NAME);
      assertThat(
          bgpRibRoutes,
          containsInAnyOrder(
              hasPrefix(learnedPrefix1),
              hasPrefix(learnedPrefix2),
              hasPrefix(aggPrefix1),
              hasPrefix(aggPrefix2)));
    }
    {
      // Check c3 routes.
      Set<Bgpv4Route> bgpRibRoutes = dp.getBgpRoutes().get(c3, Configuration.DEFAULT_VRF_NAME);
      assertThat(
          bgpRibRoutes,
          containsInAnyOrder(
              hasPrefix(learnedPrefix1), hasPrefix(aggPrefix1), hasPrefix(aggPrefix2)));
    }
  }

  @Test
  public void testBgpDefaultImportExport() {
    // BGP peers with no import/export filters defined deny all routes in XR
    String hostname = "bgp-default-import-export";
    Configuration c = parseConfig(hostname);
    Map<Prefix, BgpActivePeerConfig> peers = c.getDefaultVrf().getBgpProcess().getActiveNeighbors();
    Bgpv4Route route = Bgpv4Route.testBuilder().setNetwork(Prefix.parse("1.1.1.0/24")).build();

    // EBGP peers with unconfigured or undefined import/export policies should deny all routes
    for (Prefix ebgpPeerPrefix :
        ImmutableList.of(Prefix.parse("10.1.0.2/32"), Prefix.parse("10.1.0.3/32"))) {
      BgpActivePeerConfig ebgpPeer = peers.get(ebgpPeerPrefix);
      RoutingPolicy importPolicy =
          c.getRoutingPolicies().get(ebgpPeer.getIpv4UnicastAddressFamily().getImportPolicy());
      RoutingPolicy exportPolicy =
          c.getRoutingPolicies().get(ebgpPeer.getIpv4UnicastAddressFamily().getExportPolicy());
      assertFalse(importPolicy.process(route, Bgpv4Route.testBuilder(), Direction.IN));
      assertFalse(exportPolicy.process(route, Bgpv4Route.testBuilder(), Direction.OUT));
    }

    // IBGP peers with unconfigured or undefined import/export policies should permit all BGP routes
    for (Prefix ibgpPeerPrefix :
        ImmutableList.of(Prefix.parse("10.1.0.4/32"), Prefix.parse("10.1.0.5/32"))) {
      BgpActivePeerConfig ibgpPeer = peers.get(ibgpPeerPrefix);
      RoutingPolicy exportPolicy =
          c.getRoutingPolicies().get(ibgpPeer.getIpv4UnicastAddressFamily().getExportPolicy());
      // Currently, there is no import policy in this case because import is completely unrestricted
      assertNull(ibgpPeer.getIpv4UnicastAddressFamily().getImportPolicy());
      assertTrue(exportPolicy.process(route, Bgpv4Route.testBuilder(), Direction.OUT));
    }
  }

  @Test
  public void testConflictPolicyHighestIpParsing() {
    String hostname = "xr-conflict-policy-highest-ip";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testConflictPolicyLongestPrefixParsing() {
    String hostname = "xr-conflict-policy-longest-prefix";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testConflictPolicyStaticParsing() {
    String hostname = "xr-conflict-policy-static";
    // Do not crash
    assertNotNull(parseVendorConfig(hostname));
  }

  @Test
  public void testAsPathSetExtraction() {
    String hostname = "xr-as-path-set";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getAsPathSets(), hasKeys("mixed"));
    assertThat(
        vc.getAsPathSets().get("mixed").getElements(),
        equalTo(
            ImmutableList.of(
                new DfaRegexAsPathSetElem("^1_"),
                new IosRegexAsPathSetElem("^2_"),
                new LengthAsPathSetElem(EQ, 1, false),
                new LengthAsPathSetElem(EQ, 2, true),
                new LengthAsPathSetElem(GE, 3, false),
                new LengthAsPathSetElem(GE, 4, true),
                new LengthAsPathSetElem(EQ, 5, false),
                new LengthAsPathSetElem(EQ, 6, true),
                new LengthAsPathSetElem(LE, 7, false),
                new LengthAsPathSetElem(LE, 8, true),
                new NeighborIsAsPathSetElem(false, Range.singleton(65537L), Range.closed(2L, 3L)),
                new NeighborIsAsPathSetElem(true, Range.singleton(4L), Range.closed(5L, 6L)),
                new OriginatesFromAsPathSetElem(false, Range.singleton(1L), Range.closed(2L, 3L)),
                new OriginatesFromAsPathSetElem(true, Range.singleton(4L), Range.closed(5L, 6L)),
                new PassesThroughAsPathSetElem(false, Range.singleton(1L), Range.closed(2L, 3L)),
                new PassesThroughAsPathSetElem(true, Range.singleton(4L), Range.closed(5L, 6L)),
                new UniqueLengthAsPathSetElem(EQ, 1, false),
                new UniqueLengthAsPathSetElem(EQ, 2, true),
                new UniqueLengthAsPathSetElem(GE, 3, false),
                new UniqueLengthAsPathSetElem(GE, 4, true),
                new UniqueLengthAsPathSetElem(EQ, 5, false),
                new UniqueLengthAsPathSetElem(EQ, 6, true),
                new UniqueLengthAsPathSetElem(LE, 7, false),
                new UniqueLengthAsPathSetElem(LE, 8, true))));
  }

  @Test
  public void testAsPathSetConversion() {
    String hostname = "xr-as-path-set";
    // Do not crash
    assertNotNull(parseConfig(hostname));
  }

  @Test
  public void testAsPathBooleanExtraction() {
    String hostname = "xr-as-path-boolean";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getRoutePolicies(),
        hasKeys(
            "rp1",
            "rp-neighbor-is",
            "rp-originates-from",
            "rp-passes-through",
            "rp-length",
            "rp-unique-length",
            "rp-is-local"));

    RoutePolicy rp = vc.getRoutePolicies().get("rp1");
    Iterator<RoutePolicyStatement> i = rp.getStatements().iterator();
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(guard, instanceOf(RoutePolicyBooleanAsPathIn.class));
      AsPathSetExpr expr = ((RoutePolicyBooleanAsPathIn) guard).getAsPathSetExpr();
      assertThat(expr, instanceOf(InlineAsPathSet.class));
      assertThat(
          ((InlineAsPathSet) expr).getAsPathSet().getElements(),
          equalTo(
              ImmutableList.of(
                  new DfaRegexAsPathSetElem("^1_"),
                  new IosRegexAsPathSetElem("^2_"),
                  new LengthAsPathSetElem(EQ, 1, false),
                  new LengthAsPathSetElem(GE, 2, true),
                  new NeighborIsAsPathSetElem(true, Range.singleton(65537L), Range.closed(2L, 3L)),
                  new OriginatesFromAsPathSetElem(false, Range.singleton(4L), Range.closed(5L, 6L)),
                  new PassesThroughAsPathSetElem(false, Range.singleton(7L), Range.closed(8L, 9L)),
                  new UniqueLengthAsPathSetElem(EQ, 1, false),
                  new UniqueLengthAsPathSetElem(EQ, 2, true))));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(guard, instanceOf(RoutePolicyBooleanAsPathIn.class));
      AsPathSetExpr expr = ((RoutePolicyBooleanAsPathIn) guard).getAsPathSetExpr();
      assertThat(expr, instanceOf(InlineAsPathSet.class));
      Iterator<AsPathSetElem> ei = ((InlineAsPathSet) expr).getAsPathSet().getElements().iterator();
      // TODO: record expressions using parameters
      assertFalse(ei.hasNext());
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(guard, instanceOf(RoutePolicyBooleanAsPathIn.class));
      AsPathSetExpr expr = ((RoutePolicyBooleanAsPathIn) guard).getAsPathSetExpr();
      assertThat(expr, equalTo(new AsPathSetReference("set1")));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(guard, instanceOf(RoutePolicyBooleanAsPathIn.class));
      AsPathSetExpr expr = ((RoutePolicyBooleanAsPathIn) guard).getAsPathSetExpr();
      assertThat(expr, equalTo(new AsPathSetVariable("$setname")));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(guard, equalTo(RoutePolicyBooleanAsPathIsLocal.instance()));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(guard, equalTo(new RoutePolicyBooleanAsPathLength(EQ, 1, false)));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      // TODO: record length with var
      assertThat(guard, equalTo(UnimplementedBoolean.instance()));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(
          guard,
          equalTo(
              new RoutePolicyBooleanAsPathNeighborIs(
                  false, Range.singleton(1L), Range.closed(2L, 3L))));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      // TODO: record boolean neighbor-is with var
      assertThat(guard, equalTo(UnimplementedBoolean.instance()));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(
          guard,
          equalTo(
              new RoutePolicyBooleanAsPathOriginatesFrom(
                  true, Range.singleton(1L), Range.closed(2L, 3L))));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      // TODO: record boolean originates-from with var
      assertThat(guard, equalTo(UnimplementedBoolean.instance()));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(
          guard,
          equalTo(
              new RoutePolicyBooleanAsPathPassesThrough(
                  true, Range.singleton(1L), Range.closed(2L, 3L))));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      // TODO: record boolean passes-through with var
      assertThat(guard, equalTo(UnimplementedBoolean.instance()));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      assertThat(guard, equalTo(new RoutePolicyBooleanAsPathUniqueLength(EQ, 1, true)));
    }
    {
      RoutePolicyBoolean guard = asPathBooleanExtractionTestHelper(i.next());
      // TODO: record unique-length with var
      assertThat(guard, equalTo(UnimplementedBoolean.instance()));
    }
    assertFalse(i.hasNext());
  }

  private static RoutePolicyBoolean asPathBooleanExtractionTestHelper(
      RoutePolicyStatement statement) {
    assertThat(statement, instanceOf(RoutePolicyIfStatement.class));
    RoutePolicyIfStatement ifStatement = (RoutePolicyIfStatement) statement;
    RoutePolicyBoolean guard = ifStatement.getGuard();
    assertNotNull(guard);
    return guard;
  }

  @Test
  public void testAsPathBooleanConversion() {
    String hostname = "xr-as-path-boolean";
    Configuration c = parseConfig(hostname);

    Builder rb = Bgpv4Route.testBuilder().setNetwork(Prefix.ZERO);
    assertThat(
        c.getRoutingPolicies().keySet().stream()
            .filter(rpName -> !isGenerated(rpName))
            .collect(ImmutableList.toImmutableList()),
        containsInAnyOrder(
            "rp1",
            "rp-neighbor-is",
            "rp-originates-from",
            "rp-passes-through",
            "rp-length",
            "rp-unique-length",
            "rp-is-local"));
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("rp-neighbor-is");
      // as-path neighbor-is
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 4L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 3L, 3L, 4L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(5L, 1L, 2L, 4L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 4L, 5L)).build());
      // as-path neighbor-is exact
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(6L, 7L, 8L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(6L, 7L, 7L, 8L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(5L, 6L, 7L, 8L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(6L, 7L, 8L, 5L)).build());
      // as-path in (neighbor-is)
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(11L, 22L, 44L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(11L, 33L, 33L, 44L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 11L, 22L, 44L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(11L, 22L, 44L, 5L)).build());
      // as-path in (neighbor-is exact)
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(66L, 77L, 88L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(66L, 77L, 77L, 88L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 66L, 77L, 88L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(66L, 77L, 88L, 5L)).build());
      // as-path in <as-path-set-name>
      //   neighbor-is
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 222L, 444L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 333L, 333L, 444L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 111L, 222L, 444L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 222L, 444L, 5L)).build());
      //   neighbor-is exact
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(666L, 777L, 888L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(666L, 777L, 777L, 888L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 666L, 777L, 888L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(666L, 777L, 888L, 5L)).build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("rp-originates-from");
      // as-path originates-from
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 4L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 3L, 3L, 4L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(5L, 1L, 2L, 4L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 4L, 5L)).build());
      // as-path originates-from exact
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(6L, 7L, 8L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(6L, 7L, 7L, 8L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(5L, 6L, 7L, 8L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(6L, 7L, 8L, 5L)).build());
      // as-path in (originates-from)
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(11L, 22L, 44L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(11L, 33L, 33L, 44L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 11L, 22L, 44L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(11L, 22L, 44L, 5L)).build());
      // as-path in (originates-from exact)
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(66L, 77L, 88L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(66L, 77L, 77L, 88L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 66L, 77L, 88L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(66L, 77L, 88L, 5L)).build());
      // as-path in <as-path-set-name>
      //   originates-from
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 222L, 444L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 333L, 333L, 444L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 111L, 222L, 444L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 222L, 444L, 5L)).build());
      //   originates-from exact
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(666L, 777L, 888L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(666L, 777L, 777L, 888L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 666L, 777L, 888L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(666L, 777L, 888L, 5L)).build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("rp-passes-through");
      // as-path passes-through
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 4L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 3L, 3L, 4L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(5L, 1L, 2L, 4L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 4L, 5L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 5L, 4L)).build());
      // as-path passes-through exact
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(6L, 7L, 8L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(6L, 7L, 7L, 8L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(5L, 6L, 7L, 8L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(6L, 7L, 8L, 5L)).build());
      // as-path in (passes-through)
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(11L, 22L, 44L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(11L, 33L, 33L, 44L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 11L, 22L, 44L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(11L, 22L, 44L, 5L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(11L, 22L, 5L, 44L)).build());
      // as-path in (passes-through exact)
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(66L, 77L, 88L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(66L, 77L, 77L, 88L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 66L, 77L, 88L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(66L, 77L, 88L, 5L)).build());
      // as-path in <as-path-set-name>
      //   passes-through
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 222L, 444L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 333L, 333L, 444L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 111L, 222L, 444L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 222L, 444L, 5L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(111L, 222L, 5L, 444L)).build());
      //   passes-through exact
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(666L, 777L, 888L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(666L, 777L, 777L, 888L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(5L, 666L, 777L, 888L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(666L, 777L, 888L, 5L)).build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("rp-length");
      // length 2
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 1L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 2L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L)).build());
      // in (length 5)
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 4L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 5L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L)).build());
      // as-path in <as-path-set-name): length 8
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L, 7L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 8L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)).build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("rp-unique-length");
      // unique-length 2
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 1L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L)).build());
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 2L)).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L)).build());
      // in (unique-length 5)
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 4L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 5L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L)).build());
      // as-path in <as-path-set-name): unique-length 8
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L, 7L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)).build());
      assertRoutingPolicyPermitsRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 8L)).build());
      assertRoutingPolicyDeniesRoute(
          rp, rb.setAsPath(ofSingletonAsSets(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)).build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("rp-is-local");
      assertRoutingPolicyPermitsRoute(rp, rb.setAsPath(AsPath.empty()).build());
      assertRoutingPolicyDeniesRoute(rp, rb.setAsPath(ofSingletonAsSets(1L)).build());
    }
  }

  @Test
  public void testResolutionPolicyFiltering() throws IOException {
    String hostname = "resolution_policy";
    Configuration c = parseConfig(hostname);
    assertThat(c.getRoutingPolicies(), hasKey(RESOLUTION_POLICY_NAME));
    assertThat(c.getDefaultVrf().getResolutionPolicy(), equalTo(RESOLUTION_POLICY_NAME));
    RoutingPolicy r = c.getRoutingPolicies().get(RESOLUTION_POLICY_NAME);

    // Policy should accept non-default routes
    assertTrue(
        r.processReadOnly(
            StaticRoute.testBuilder()
                .setNetwork(Prefix.create(Ip.parse("10.10.10.10"), 24))
                .build()));

    // Policy should not accept default routes
    assertFalse(r.processReadOnly(StaticRoute.testBuilder().setNetwork(Prefix.ZERO).build()));
  }

  @Test
  public void testResolutionPolicyRibRoutes() throws IOException {
    String hostname = "resolution_policy";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.loadConfigurations(batfish.getSnapshot());
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());
    Set<AbstractRoute> routes = dp.getRibs().get(hostname).get("default").getRoutes();

    // Rib should have the static route whose NHI is determined from a non-default route
    assertThat(
        routes,
        hasItem(
            allOf(hasPrefix(Prefix.parse("10.101.1.1/32")), hasNextHopIp(Ip.parse("10.0.1.100")))));

    // Rib should NOT have the static route whose NHI is determined from the default route
    // and the default route should exist
    assertThat(routes, hasItem(hasPrefix(Prefix.ZERO)));
    assertThat(routes, not(hasItem(hasPrefix(Prefix.parse("10.103.3.1/32")))));
  }

  @Test
  public void testOspfNetworkType() {
    String hostname = "ospf-network-type";
    CiscoXrConfiguration c = parseVendorConfig(hostname);

    assertThat(
        c.getInterfaces().keySet(),
        containsInAnyOrder(
            "GigabitEthernet0/0/0/1",
            "GigabitEthernet0/0/0/2",
            "GigabitEthernet0/0/0/3",
            "GigabitEthernet0/0/0/4",
            "GigabitEthernet0/0/0/5"));

    // Configured in OSPF router
    assertThat(
        c.getInterfaces().get("GigabitEthernet0/0/0/1").getOspfNetworkType(),
        equalTo(org.batfish.representation.cisco_xr.OspfNetworkType.POINT_TO_POINT));
    assertThat(
        c.getInterfaces().get("GigabitEthernet0/0/0/2").getOspfNetworkType(),
        equalTo(org.batfish.representation.cisco_xr.OspfNetworkType.BROADCAST));
    assertThat(
        c.getInterfaces().get("GigabitEthernet0/0/0/3").getOspfNetworkType(),
        equalTo(org.batfish.representation.cisco_xr.OspfNetworkType.NON_BROADCAST));
    assertThat(
        c.getInterfaces().get("GigabitEthernet0/0/0/4").getOspfNetworkType(),
        equalTo(org.batfish.representation.cisco_xr.OspfNetworkType.POINT_TO_MULTIPOINT));
    assertThat(
        c.getInterfaces().get("GigabitEthernet0/0/0/5").getOspfNetworkType(),
        equalTo(OspfNetworkType.POINT_TO_MULTIPOINT_NON_BROADCAST));
  }

  @Test
  public void testOspfNetworkTypeOverrideExtraction() {
    String hostname = "ospf-network-type-override";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces().keySet(),
        containsInAnyOrder("GigabitEthernet0/0/0/1", "GigabitEthernet0/0/0/2"));

    // Network types configured at OSPF interface level
    assertThat(
        vc.getInterfaces().get("GigabitEthernet0/0/0/1").getOspfNetworkType(),
        equalTo(org.batfish.representation.cisco_xr.OspfNetworkType.BROADCAST));
    assertNull(vc.getInterfaces().get("GigabitEthernet0/0/0/2").getOspfNetworkType());

    // Network type configured at OSPF router level
    assertThat(
        vc.getDefaultVrf().getOspfProcesses().get("65100").getDefaultNetworkType(),
        equalTo(OspfNetworkType.POINT_TO_POINT));
  }

  @Test
  public void testOspfNetworkTypeOverrideConversion() {
    String hostname = "ospf-network-type-override";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces().keySet(),
        containsInAnyOrder("GigabitEthernet0/0/0/1", "GigabitEthernet0/0/0/2"));

    // Network type configured at OSPF interface level overrides OSPF router level type
    assertThat(
        c.getAllInterfaces().get("GigabitEthernet0/0/0/1").getOspfNetworkType(),
        equalTo(BROADCAST));

    // Network type inherited from OSPF router level
    assertThat(
        c.getAllInterfaces().get("GigabitEthernet0/0/0/2").getOspfNetworkType(),
        equalTo(POINT_TO_POINT));
  }

  /** Test extraction of ACL based forwarding constructs in IP access-lists */
  @Test
  public void testAbfExtraction() {
    String hostname = "abf_extraction";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpv4Acls().keySet(), contains("aclv4"));
    assertThat(vc.getIpv6Acls().keySet(), contains("aclv6"));

    // Ipv4
    {
      Ipv4AccessList acl = vc.getIpv4Acls().get("aclv4");
      assertThat(acl.getLines(), iterableWithSize(2));
      Ipv4AccessListLine nhIpLine = acl.getLines().get(0);
      Ipv4AccessListLine nhVrfLine = acl.getLines().get(1);

      assertThat(nhIpLine.getNexthop1().getIp(), equalTo(Ip.parse("10.0.13.1")));
      assertNull(nhIpLine.getNexthop1().getVrf());
      assertThat(nhIpLine.getNexthop2().getIp(), equalTo(Ip.parse("10.0.13.2")));
      assertThat(nhIpLine.getNexthop2().getVrf(), equalTo("vrf2"));
      assertThat(nhIpLine.getNexthop3().getIp(), equalTo(Ip.parse("10.0.13.3")));
      assertNull(nhIpLine.getNexthop3().getVrf());

      assertThat(nhVrfLine.getNexthop1().getIp(), equalTo(Ip.parse("10.0.14.1")));
      assertThat(nhVrfLine.getNexthop1().getVrf(), equalTo("vrf1"));
      assertNull(nhVrfLine.getNexthop2());
      assertNull(nhVrfLine.getNexthop3());
    }

    // Ipv6
    {
      Ipv6AccessList acl = vc.getIpv6Acls().get("aclv6");
      assertThat(acl.getLines(), iterableWithSize(2));
      Ipv6AccessListLine nhIpLine = acl.getLines().get(0);
      Ipv6AccessListLine nhVrfLine = acl.getLines().get(1);

      assertThat(nhIpLine.getNexthop1().getIp(), equalTo(Ip6.parse("3001::")));
      assertNull(nhIpLine.getNexthop1().getVrf());
      assertThat(nhIpLine.getNexthop2().getIp(), equalTo(Ip6.parse("3002::")));
      assertThat(nhIpLine.getNexthop2().getVrf(), equalTo("vrf2"));
      assertThat(nhIpLine.getNexthop3().getIp(), equalTo(Ip6.parse("3003::")));
      assertNull(nhIpLine.getNexthop3().getVrf());

      assertThat(nhVrfLine.getNexthop1().getIp(), equalTo(Ip6.parse("4001::")));
      assertThat(nhVrfLine.getNexthop1().getVrf(), equalTo("vrf1"));
      assertNull(nhVrfLine.getNexthop2());
      assertNull(nhVrfLine.getNexthop3());
    }
  }

  @Test
  public void testAbfExtractionWarning() {
    String hostname = "abf_extraction";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    Warnings warnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        warnings,
        hasParseWarnings(
            containsInAnyOrder(
                allOf(
                    hasComment(
                        "ACL based forwarding can only be configured on an ACL line with a permit"
                            + " action"),
                    hasText("100 deny tcp any host 10.0.10.1 nexthop1 ipv4 10.10.10.10")),
                allOf(
                    hasComment(
                        "ACL based forwarding can only be configured on an ACL line with a permit"
                            + " action"),
                    hasText("100 deny tcp any host 1111:: nexthop1 ipv6 1112::")))));
  }

  /** Test conversion of ACL based forwarding constructs in IP access-lists */
  @Test
  public void testAbfConversion() {
    String hostname = "abf_conversion";
    Configuration c = parseConfig(hostname);
    String abfPolicyName = computeAbfIpv4PolicyName("aclv4");
    String gigE0 = "GigabitEthernet0/0/0/0";
    FibLookup regularFibLookup = new FibLookup(IngressInterfaceVrf.instance());

    assertThat(c.getPacketPolicies().keySet(), contains(abfPolicyName));

    PacketPolicy policy = c.getPacketPolicies().get(abfPolicyName);
    Flow permittedNoAbf =
        Flow.builder()
            .setIpProtocol(IpProtocol.TCP)
            .setIngressNode(hostname)
            .setIngressInterface(gigE0)
            // Arbitrary source
            .setSrcIp(Ip.parse("8.8.8.8"))
            .setSrcPort(22222)
            .setDstIp(Ip.parse("10.0.5.1"))
            .setDstPort(22)
            .build();
    Flow permittedAbfNoVrf = permittedNoAbf.toBuilder().setDstIp(Ip.parse("10.0.3.1")).build();
    Flow permittedAbfWithVrf = permittedNoAbf.toBuilder().setDstIp(Ip.parse("10.0.4.1")).build();
    Flow denied = permittedNoAbf.toBuilder().setSrcIp(Ip.parse("10.0.0.1")).build();

    // Permitted by non-ABF line
    assertThat(
        FlowEvaluator.evaluate(
                permittedNoAbf,
                gigE0,
                DEFAULT_VRF_NAME,
                policy,
                c.getIpAccessLists(),
                ImmutableMap.of(),
                ImmutableMap.of())
            .getAction(),
        equalTo(regularFibLookup));

    // Permitted by ABF line (nexthop specified but not vrf)
    assertThat(
        FlowEvaluator.evaluate(
                permittedAbfNoVrf,
                gigE0,
                DEFAULT_VRF_NAME,
                policy,
                c.getIpAccessLists(),
                ImmutableMap.of(),
                ImmutableMap.of())
            .getAction(),
        equalTo(
            FibLookupOverrideLookupIp.builder()
                .setIps(
                    ImmutableList.of(
                        Ip.parse("10.0.13.1"), Ip.parse("10.0.13.2"), Ip.parse("10.0.13.3")))
                .setVrfExpr(IngressInterfaceVrf.instance())
                .setDefaultAction(regularFibLookup)
                .setRequireConnected(false)
                .build()));

    // Permitted by ABF line (nexthop AND vrf specified)
    assertThat(
        FlowEvaluator.evaluate(
                permittedAbfWithVrf,
                gigE0,
                DEFAULT_VRF_NAME,
                policy,
                c.getIpAccessLists(),
                ImmutableMap.of(),
                ImmutableMap.of())
            .getAction(),
        equalTo(
            FibLookupOverrideLookupIp.builder()
                .setIps(ImmutableList.of(Ip.parse("10.0.14.1")))
                .setVrfExpr(new LiteralVrfName("vrf1"))
                .setDefaultAction(regularFibLookup)
                .setRequireConnected(false)
                .build()));

    // Denied by explicit deny line
    assertThat(
        FlowEvaluator.evaluate(
                denied,
                gigE0,
                DEFAULT_VRF_NAME,
                policy,
                c.getIpAccessLists(),
                ImmutableMap.of(),
                ImmutableMap.of())
            .getAction(),
        equalTo(Drop.instance()));
    // Similar to denied flow, but not matching source IP or dest port
    assertThat(
        FlowEvaluator.evaluate(
                denied.toBuilder().setSrcIp(Ip.parse("10.0.0.2")).build(),
                gigE0,
                DEFAULT_VRF_NAME,
                policy,
                c.getIpAccessLists(),
                ImmutableMap.of(),
                ImmutableMap.of())
            .getAction(),
        equalTo(regularFibLookup));
    assertThat(
        FlowEvaluator.evaluate(
                denied.toBuilder().setDstPort(23).build(),
                gigE0,
                DEFAULT_VRF_NAME,
                policy,
                c.getIpAccessLists(),
                ImmutableMap.of(),
                ImmutableMap.of())
            .getAction(),
        equalTo(regularFibLookup));

    assertThat(c.getAllInterfaces().get(gigE0).getPacketPolicyName(), equalTo(abfPolicyName));
  }

  @Test
  public void testAbfConversionWarning() {
    String hostname = "abf_conversion_warning";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                "Access-list lines with different nexthop VRFs are not yet supported. Line '60"
                    + " permit tcp any host 10.0.1.1 nexthop1 vrf vrf1 ipv4 10.0.11.1 nexthop2 vrf"
                    + " vrfOther ipv4 10.0.11.2' in ACL aclv4 will be ignored.")));
    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                "Access-list lines with different nexthop VRFs are not yet supported. Line '70"
                    + " permit tcp any host 10.0.1.1 nexthop1 vrf vrf1 ipv4 10.0.11.1 nexthop2 vrf"
                    + " vrf1 ipv4 10.0.11.2 nexthop3 vrf vrfOther ipv4 10.0.11.3' in ACL aclv4 will"
                    + " be ignored.")));
    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                "Access-list lines with different nexthop VRFs are not yet supported. Line '80"
                    + " permit tcp any host 10.0.1.1 nexthop1 vrf vrf1 ipv4 10.0.11.1 nexthop2 ipv4"
                    + " 10.0.11.2' in ACL aclv4 will be ignored.")));
    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                "Access-list lines with different nexthop VRFs are not yet supported. Line '90"
                    + " permit tcp any host 10.0.1.1 nexthop1 ipv4 10.0.11.1 nexthop2 vrf vrfOther"
                    + " ipv4 10.0.11.2' in ACL aclv4 will be ignored.")));
    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                "ACL based forwarding rule aclv4 cannot be applied to an egress interface.")));

    // No other warnings, i.e. other lines are converted successfully
    assertThat(ccae.getWarnings().get(hostname).getRedFlagWarnings(), iterableWithSize(5));
  }

  @Test
  public void testL2vpnExtraction() {
    String hostname = "l2vpn";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getInterfaces(), hasKeys("BVI1", "GigabitEthernet0/0/0/1.1"));

    assertThat(vc.getBridgeGroups(), hasKeys("BG1"));
    Map<String, BridgeDomain> bridgeDomains = vc.getBridgeGroups().get("BG1").getBridgeDomains();
    assertThat(bridgeDomains, hasKeys("BD1", "BD2", "BD3"));

    BridgeDomain bd1 = bridgeDomains.get("BD1");
    assertThat(bd1.getName(), equalTo("BD1"));
    assertThat(
        bd1.getInterfaces(),
        containsInAnyOrder("GigabitEthernet0/0/0/1.1", "GigabitEthernet0/0/0/2.1"));
    assertThat(bd1.getRoutedInterface(), equalTo("BVI1"));

    BridgeDomain bd2 = bridgeDomains.get("BD2");
    assertThat(bd2.getName(), equalTo("BD2"));
    assertThat(bd2.getInterfaces(), empty());
    assertThat(bd2.getRoutedInterface(), equalTo("BVI2"));

    BridgeDomain bd3 = bridgeDomains.get("BD3");
    assertThat(bd3.getName(), equalTo("BD3"));
    assertThat(bd3.getInterfaces(), empty());
    assertNull(bd3.getRoutedInterface());
  }

  @Test
  public void testL2vpnReferences() {
    String hostname = "l2vpn";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae,
        hasReferencedStructure(
            filename, INTERFACE, "GigabitEthernet0/0/0/1.1", BRIDGE_DOMAIN_INTERFACE));
    assertThat(
        ccae,
        hasReferencedStructure(
            filename, INTERFACE, "GigabitEthernet0/0/0/2.1", BRIDGE_DOMAIN_INTERFACE));
    assertThat(
        ccae, hasReferencedStructure(filename, INTERFACE, "BVI1", BRIDGE_DOMAIN_ROUTED_INTERFACE));
    assertThat(
        ccae, hasReferencedStructure(filename, INTERFACE, "BVI2", BRIDGE_DOMAIN_ROUTED_INTERFACE));

    assertThat(
        ccae,
        hasUndefinedReference(
            filename, INTERFACE, "GigabitEthernet0/0/0/2.1", BRIDGE_DOMAIN_INTERFACE));
    assertThat(
        ccae, hasUndefinedReference(filename, INTERFACE, "BVI2", BRIDGE_DOMAIN_ROUTED_INTERFACE));
  }

  @Test
  public void testInterfaceL2transportExtraction() {
    String hostname = "interface_l2transport";
    CiscoXrConfiguration vc = parseVendorConfig(hostname);

    assertNotNull(vc);

    assertThat(
        vc.getInterfaces(),
        hasKeys("GigabitEthernet0/0/0/1", "GigabitEthernet0/0/0/1.1", "GigabitEthernet0/0/0/1.2"));

    org.batfish.representation.cisco_xr.Interface ge1 =
        vc.getInterfaces().get("GigabitEthernet0/0/0/1");
    org.batfish.representation.cisco_xr.Interface ge11 =
        vc.getInterfaces().get("GigabitEthernet0/0/0/1.1");
    TagRewritePolicy ge11Policy = ge11.getRewriteIngressTag();
    org.batfish.representation.cisco_xr.Interface ge12 =
        vc.getInterfaces().get("GigabitEthernet0/0/0/1.2");
    TagRewritePolicy ge12Policy = ge12.getRewriteIngressTag();

    assertFalse(ge1.getL2transport());
    assertThat(ge1.getEncapsulationVlan(), nullValue());

    assertTrue(ge11.getL2transport());
    assertThat(ge11Policy, instanceOf(TagRewritePop.class));
    assertThat(((TagRewritePop) ge11Policy).getPopCount(), equalTo(1));
    assertFalse(((TagRewritePop) ge11Policy).getSymmetric());
    assertThat(ge11.getEncapsulationVlan(), equalTo(1));

    assertTrue(ge12.getL2transport());
    assertThat(ge12Policy, instanceOf(TagRewritePop.class));
    assertThat(((TagRewritePop) ge12Policy).getPopCount(), equalTo(2));
    assertTrue(((TagRewritePop) ge12Policy).getSymmetric());
    assertThat(ge12.getEncapsulationVlan(), equalTo(2));
  }

  @Test
  public void testInterfaceL2transportWarning() {
    String hostname = "interface_l2transport_warning";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    Warnings warnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        warnings,
        hasParseWarnings(
            containsInAnyOrder(
                hasComment("Expected rewrite ingress tag pop range in range 1-2, but got '0'"),
                hasComment("Expected rewrite ingress tag pop range in range 1-2, but got '3'"),
                allOf(
                    hasComment(
                        "Rewrite policy can only be configured on l2transport interfaces. Ignoring"
                            + " this line."),
                    hasText("rewrite ingress tag pop 1 symmetric")))));
  }
}

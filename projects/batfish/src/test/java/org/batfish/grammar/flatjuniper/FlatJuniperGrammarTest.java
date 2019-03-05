package org.batfish.grammar.flatjuniper;

import static org.batfish.common.util.CommonUtil.communityStringToLong;
import static org.batfish.datamodel.AuthenticationMethod.GROUP_RADIUS;
import static org.batfish.datamodel.AuthenticationMethod.GROUP_TACACS;
import static org.batfish.datamodel.AuthenticationMethod.PASSWORD;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Names.zoneToZoneFilter;
import static org.batfish.datamodel.acl.AclLineMatchExprs.match;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.matchers.AaaAuthenticationLoginListMatchers.hasMethods;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AnnotatedRouteMatchers.hasSourceVrf;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasAllowLocalAsIn;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasClusterId;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasEnforceFirstAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEbgp;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathIbgp;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbors;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasPassiveNeighbor;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasCommunities;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkePhase1Policy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkePhase1Proposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessList;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPeerConfig;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPhase2Policy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPhase2Proposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrfs;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasBandwidth;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructureWithDefinitionLines;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasIsisProcess;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasReferenceBandwidth;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReferenceWithReferenceLines;
import static org.batfish.datamodel.matchers.GeneratedRouteMatchers.isDiscard;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasIkePhase1Key;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasIkePhase1Proposals;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAccessVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAdditionalArpIps;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllowedVlans;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDescription;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasIsis;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasNativeVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfAreaName;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfCost;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfPointToPoint;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasZoneName;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPassive;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.accepts;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.rejects;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasDestinationAddress;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.isIpsecStaticPeerConfigThat;
import static org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchers.hasHelloAuthenticationType;
import static org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchers.hasHoldTime;
import static org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchers.hasMode;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasBfdLivenessDetectionMinimumInterval;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasBfdLivenessDetectionMultiplier;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasIsoAddress;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasLevel1;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasLevel2;
import static org.batfish.datamodel.matchers.IsisLevelSettingsMatchers.hasWideMetricsOnly;
import static org.batfish.datamodel.matchers.IsisProcessMatchers.hasNetAddress;
import static org.batfish.datamodel.matchers.IsisProcessMatchers.hasOverload;
import static org.batfish.datamodel.matchers.LineMatchers.hasAuthenticationLoginList;
import static org.batfish.datamodel.matchers.LiteralIntMatcher.hasVal;
import static org.batfish.datamodel.matchers.LiteralIntMatcher.isLiteralIntThat;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasDefaultOriginateType;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasSuppressType3;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasInjectDefaultRoute;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasMetricOfDefaultRoute;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasNssa;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasStub;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasStubType;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasSummary;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.hasMetric;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.isAdvertised;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasArea;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasRouterId;
import static org.batfish.datamodel.matchers.RouteFilterListMatchers.permits;
import static org.batfish.datamodel.matchers.SetAdministrativeCostMatchers.hasAdmin;
import static org.batfish.datamodel.matchers.SetAdministrativeCostMatchers.isSetAdministrativeCostThat;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasGeneratedRoutes;
import static org.batfish.datamodel.matchers.VrfMatchers.hasOspfProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.batfish.datamodel.transformation.Noop.NOOP_DEST_NAT;
import static org.batfish.datamodel.transformation.Transformation.when;
import static org.batfish.datamodel.transformation.TransformationStep.assignDestinationIp;
import static org.batfish.datamodel.transformation.TransformationStep.assignSourceIp;
import static org.batfish.datamodel.transformation.TransformationStep.assignSourcePort;
import static org.batfish.datamodel.vendor_family.juniper.JuniperFamily.AUXILIARY_LINE_NAME;
import static org.batfish.datamodel.vendor_family.juniper.JuniperFamily.CONSOLE_LINE_NAME;
import static org.batfish.representation.juniper.JuniperConfiguration.ACL_NAME_EXISTING_CONNECTION;
import static org.batfish.representation.juniper.JuniperConfiguration.ACL_NAME_GLOBAL_POLICY;
import static org.batfish.representation.juniper.JuniperConfiguration.ACL_NAME_SECURITY_POLICY;
import static org.batfish.representation.juniper.JuniperConfiguration.computeOspfExportPolicyName;
import static org.batfish.representation.juniper.JuniperConfiguration.computePeerExportPolicyName;
import static org.batfish.representation.juniper.JuniperStructureType.APPLICATION;
import static org.batfish.representation.juniper.JuniperStructureType.APPLICATION_OR_APPLICATION_SET;
import static org.batfish.representation.juniper.JuniperStructureType.APPLICATION_SET;
import static org.batfish.representation.juniper.JuniperStructureType.AUTHENTICATION_KEY_CHAIN;
import static org.batfish.representation.juniper.JuniperStructureType.FIREWALL_FILTER;
import static org.batfish.representation.juniper.JuniperStructureType.INTERFACE;
import static org.batfish.representation.juniper.JuniperStructureType.PREFIX_LIST;
import static org.batfish.representation.juniper.JuniperStructureType.VLAN;
import static org.batfish.representation.juniper.JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION;
import static org.batfish.representation.juniper.JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION_SET;
import static org.batfish.representation.juniper.JuniperStructureUsage.INTERFACE_VLAN;
import static org.batfish.representation.juniper.JuniperStructureUsage.OSPF_AREA_INTERFACE;
import static org.batfish.representation.juniper.JuniperStructureUsage.SECURITY_POLICY_MATCH_APPLICATION;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.GeneratedRoute6;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpsecAuthenticationAlgorithm;
import org.batfish.datamodel.IpsecEncapsulationMode;
import org.batfish.datamodel.IpsecProtocol;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.Line;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.isis.IsisHelloAuthenticationType;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.matchers.IkePhase1KeyMatchers;
import org.batfish.datamodel.matchers.IkePhase1ProposalMatchers;
import org.batfish.datamodel.matchers.IpAccessListMatchers;
import org.batfish.datamodel.matchers.IpsecPeerConfigMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2PolicyMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2ProposalMatchers;
import org.batfish.datamodel.matchers.IsisInterfaceLevelSettingsMatchers;
import org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers;
import org.batfish.datamodel.matchers.IsisProcessMatchers;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.datamodel.matchers.RouteFilterListMatchers;
import org.batfish.datamodel.matchers.StubSettingsMatchers;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetAdministrativeCost;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.AssignPortFromPool;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.VendorConfigurationFormatDetector;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Flat_juniper_configurationContext;
import org.batfish.grammar.flattener.Flattener;
import org.batfish.grammar.flattener.FlattenerLineMap;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.juniper.IcmpLarge;
import org.batfish.representation.juniper.InterfaceRange;
import org.batfish.representation.juniper.InterfaceRangeMember;
import org.batfish.representation.juniper.InterfaceRangeMemberRange;
import org.batfish.representation.juniper.IpUnknownProtocol;
import org.batfish.representation.juniper.JuniperConfiguration;
import org.batfish.representation.juniper.Nat;
import org.batfish.representation.juniper.Nat.Type;
import org.batfish.representation.juniper.NatPacketLocation;
import org.batfish.representation.juniper.NatPool;
import org.batfish.representation.juniper.NatRule;
import org.batfish.representation.juniper.NatRuleMatchDstAddr;
import org.batfish.representation.juniper.NatRuleMatchDstAddrName;
import org.batfish.representation.juniper.NatRuleMatchDstPort;
import org.batfish.representation.juniper.NatRuleMatchSrcAddr;
import org.batfish.representation.juniper.NatRuleMatchSrcAddrName;
import org.batfish.representation.juniper.NatRuleMatchSrcPort;
import org.batfish.representation.juniper.NatRuleSet;
import org.batfish.representation.juniper.NatRuleThenInterface;
import org.batfish.representation.juniper.NatRuleThenOff;
import org.batfish.representation.juniper.NatRuleThenPool;
import org.batfish.representation.juniper.NoPortTranslation;
import org.batfish.representation.juniper.PatPool;
import org.batfish.representation.juniper.Screen;
import org.batfish.representation.juniper.ScreenAction;
import org.batfish.representation.juniper.ScreenOption;
import org.batfish.representation.juniper.TcpFinNoAck;
import org.batfish.representation.juniper.TcpNoFlag;
import org.batfish.representation.juniper.TcpSynFin;
import org.batfish.representation.juniper.Zone;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link FlatJuniperParser} and {@link FlatJuniperControlPlaneExtractor}. */
public final class FlatJuniperGrammarTest {

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/juniper/testconfigs/";

  private static final String TESTRIGS_PREFIX = "org/batfish/grammar/juniper/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static Flow createFlow(String sourceAddress, String destinationAddress) {
    return createFlow(sourceAddress, destinationAddress, FlowState.NEW);
  }

  private static Flow createFlow(String sourceAddress, String destinationAddress, FlowState state) {
    Flow.Builder fb = new Flow.Builder();
    fb.setIngressNode("node");
    fb.setSrcIp(Ip.parse(sourceAddress));
    fb.setDstIp(Ip.parse(destinationAddress));
    fb.setState(state);
    fb.setTag("test");
    return fb.build();
  }

  private static Flow createFlow(IpProtocol protocol, int port) {
    Flow.Builder fb = new Flow.Builder();
    fb.setIngressNode("node");
    fb.setIpProtocol(protocol);
    fb.setDstPort(port);
    fb.setSrcPort(port);
    fb.setTag("test");
    return fb.build();
  }

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private Configuration parseConfig(String hostname) throws IOException {
    return parseTextConfigs(hostname).get(hostname.toLowerCase());
  }

  private JuniperConfiguration parseJuniperConfig(String hostname) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    FlatJuniperCombinedParser flatJuniperParser =
        new FlatJuniperCombinedParser(src, settings, null);
    FlatJuniperControlPlaneExtractor extractor =
        new FlatJuniperControlPlaneExtractor(src, flatJuniperParser, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(
            flatJuniperParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(tree);
    return (JuniperConfiguration) extractor.getVendorConfiguration();
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    return getBatfishForConfigurationNames(configurationNames).loadConfigurations();
  }

  @Test
  public void testApplications() throws IOException {
    String hostname = "applications";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm application usage is tracked properly */
    assertThat(ccae, hasNumReferrers(filename, APPLICATION, "a2", 0));
    assertThat(ccae, hasNumReferrers(filename, APPLICATION, "a1", 1));
    assertThat(ccae, hasNumReferrers(filename, APPLICATION, "a3", 1));

    /* Confirm undefined reference is identified */
    assertThat(ccae, hasUndefinedReference(filename, APPLICATION_OR_APPLICATION_SET, "a_undef"));
  }

  @Test
  public void testApplicationSet() throws IOException {
    String hostname = "application-set";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    Configuration c = parseConfig(hostname);

    /* Check that appset2 contains definition of appset1 concatenated with definition of a3 */
    assertThat(
        c,
        hasIpAccessList(
            ACL_NAME_GLOBAL_POLICY,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
                                .setSrcPorts(ImmutableList.of(new SubRange(1, 1)))
                                .build()),
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setDstPorts(ImmutableList.of(new SubRange(2, 2)))
                                .setIpProtocols(ImmutableList.of(IpProtocol.UDP))
                                .build()),
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setDstPorts(ImmutableList.of(new SubRange(3, 3)))
                                .setIpProtocols(ImmutableList.of(IpProtocol.UDP))
                                .build()))))));

    /* Check that appset1 and appset2 are referenced, but appset3 is not */
    assertThat(ccae, hasNumReferrers(filename, APPLICATION_SET, "appset1", 1));
    assertThat(ccae, hasNumReferrers(filename, APPLICATION_SET, "appset2", 1));
    assertThat(ccae, hasNumReferrers(filename, APPLICATION_SET, "appset3", 0));

    /*
     * Check that there is an undefined reference to appset4, but not to appset1-3
     * (via reference in security policy).
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            APPLICATION_OR_APPLICATION_SET,
            "appset4",
            SECURITY_POLICY_MATCH_APPLICATION));
    assertThat(
        ccae, not(hasUndefinedReference(hostname, APPLICATION_OR_APPLICATION_SET, "appset1")));
    assertThat(
        ccae, not(hasUndefinedReference(hostname, APPLICATION_OR_APPLICATION_SET, "appset2")));
    assertThat(
        ccae, not(hasUndefinedReference(hostname, APPLICATION_OR_APPLICATION_SET, "appset3")));

    /*
     * Check that there is an undefined reference to application-set appset4, but not to appset1-3
     * (via reference in application-set definition).
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, APPLICATION_SET, "appset4", APPLICATION_SET_MEMBER_APPLICATION_SET));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_SET, "appset1")));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_SET, "appset2")));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_SET, "appset3")));

    /*
     * Check that there is an undefined reference to application a4 but not a1-3
     * (via reference in application-set definition).
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, APPLICATION_OR_APPLICATION_SET, "a4", APPLICATION_SET_MEMBER_APPLICATION));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_OR_APPLICATION_SET, "a1")));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_OR_APPLICATION_SET, "a2")));
    assertThat(ccae, not(hasUndefinedReference(filename, APPLICATION_OR_APPLICATION_SET, "a3")));
  }

  @Test
  public void testApplicationSetNested() throws IOException {
    String hostname = "application-set-nested";
    Configuration c = parseConfig(hostname);

    String aclNameNonNested = zoneToZoneFilter("z1", "z2");
    String aclNameNested = zoneToZoneFilter("z1", "z3");
    String aclNameMultiNested = zoneToZoneFilter("z1", "z4");
    String z1Interface = "ge-0/0/0.0";
    IpAccessList aclNonNested = c.getIpAccessLists().get(aclNameNonNested);
    IpAccessList aclNested = c.getIpAccessLists().get(aclNameNested);
    IpAccessList aclMultiNested = c.getIpAccessLists().get(aclNameMultiNested);
    /* Allowed application permits TCP from port 1 only */
    Flow permittedFlow = createFlow(IpProtocol.TCP, 1);
    Flow rejectedFlow = createFlow(IpProtocol.TCP, 2);

    /*
     * Confirm non-nested application-set acl accepts the allowed protocol-port combo and reject
     * others
     */
    assertThat(
        aclNonNested, accepts(permittedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclNonNested, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm nested application-set acl accepts the allowed protocol-port combo and reject others
     */
    assertThat(
        aclNested, accepts(permittedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclNested, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm multi-nested application-set acl accepts the allowed protocol-port combo and reject
     * others
     */
    assertThat(
        aclMultiNested, accepts(permittedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclMultiNested, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testApplicationWithTerms() throws IOException {
    String hostname = "application-with-terms";
    Configuration c = parseConfig(hostname);

    /*
     * An IpAccessList should be generated for the cross-zone policy from z1 to z2. Its definition
     * should inline the matched application, with the action applied to each generated line
     * from the application. One line should be generated per application term.
     */
    assertThat(
        c,
        hasIpAccessList(
            ACL_NAME_GLOBAL_POLICY,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setDstPorts(ImmutableList.of(new SubRange(1, 1)))
                                .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
                                .setSrcPorts(ImmutableList.of(new SubRange(2, 2)))
                                .build()),
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setDstPorts(ImmutableList.of(new SubRange(3, 3)))
                                .setIpProtocols(ImmutableList.of(IpProtocol.UDP))
                                .setSrcPorts(ImmutableList.of(new SubRange(4, 4)))
                                .build()))))));
  }

  @Test
  public void testApplyPathWarning() throws IOException {
    String hostname = "apply-path-warning";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement();

    Warnings warnings = pvcae.getWarnings().values().iterator().next();

    assertThat(warnings.getParseWarnings().get(0).getText(), equalTo("1::/64"));
    assertThat(warnings.getParseWarnings().get(0).getLine(), equalTo(6));
    assertThat(warnings.getParseWarnings().get(1).getText(), equalTo("2::1/128"));
    assertThat(warnings.getParseWarnings().get(1).getLine(), equalTo(6));
  }

  @Test
  public void testAuthenticationKeyChain() throws IOException {
    String hostname = "authentication-key-chain";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm filter usage is tracked properly */
    assertThat(ccae, hasNumReferrers(filename, AUTHENTICATION_KEY_CHAIN, "KC", 1));
    assertThat(ccae, hasNumReferrers(filename, AUTHENTICATION_KEY_CHAIN, "KC_UNUSED", 0));

    /* Confirm undefined reference is identified */
    assertThat(ccae, hasUndefinedReference(filename, AUTHENTICATION_KEY_CHAIN, "KC_UNDEF"));
  }

  @Test
  public void testAuthenticationOrder() throws IOException {
    String hostname = "authentication-order";

    Configuration configuration = parseConfig(hostname);
    SortedMap<String, Line> lines = configuration.getVendorFamily().getJuniper().getLines();

    assertThat(lines.get(AUXILIARY_LINE_NAME), nullValue());

    assertThat(
        lines.get(CONSOLE_LINE_NAME),
        hasAuthenticationLoginList(hasMethods(equalTo(Collections.singletonList(GROUP_TACACS)))));

    assertThat(
        lines.get("telnet"),
        hasAuthenticationLoginList(hasMethods(equalTo(Arrays.asList(GROUP_TACACS, PASSWORD)))));

    assertThat(
        lines.get("ssh"),
        hasAuthenticationLoginList(hasMethods(equalTo(Arrays.asList(GROUP_RADIUS, GROUP_TACACS)))));

    assertThat(
        lines.get("ftp"),
        hasAuthenticationLoginList(hasMethods(equalTo(Collections.singletonList(GROUP_RADIUS)))));
  }

  @Test
  public void testAutonomousSystem() throws IOException {
    String testrigName = "autonomous-system";
    String c1Name = "as1";
    String c2Name = "as2";
    String c3Name = "as3";
    Prefix neighborPrefix = Prefix.parse("1.0.0.1/32");

    List<String> configurationNames = ImmutableList.of(c1Name, c2Name, c3Name);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration c1 = configurations.get(c1Name);
    Configuration c2 = configurations.get(c2Name);
    Configuration c3 = configurations.get(c3Name);

    assertThat(c1, hasDefaultVrf(hasBgpProcess(hasActiveNeighbor(neighborPrefix, hasLocalAs(1L)))));
    assertThat(c2, hasDefaultVrf(hasBgpProcess(hasActiveNeighbor(neighborPrefix, hasLocalAs(1L)))));
    assertThat(c3, hasDefaultVrf(hasBgpProcess(hasActiveNeighbor(neighborPrefix, hasLocalAs(1L)))));
  }

  @Test
  public void testStaticRouteCommunities() throws IOException {
    /*
    Setup: r1 has a BGP import policy that rejects routes with community 100:1001.
    r2 exports:
    - static route 10.20.20.0/24 with community 100:1001
    - static route 10.20.20.0/23 with community 100:1002
    - static route 10.20.22.0/24, no communities
    r1 should reject first route, but install the others in its RIB.
     */
    String testrigName = "static-route-communities";
    String c1Name = "r1";
    String c2Name = "r2";
    Long acceptedCommunity = communityStringToLong("100:1002");

    List<String> configurationNames = ImmutableList.of(c1Name, c2Name);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> r1Routes = dp.getRibs().get(c1Name).get(DEFAULT_VRF_NAME).getRoutes();

    assertThat(r1Routes, not(hasItem(hasPrefix(Prefix.parse("10.20.20.0/24")))));
    assertThat(
        r1Routes,
        hasItem(
            allOf(
                hasPrefix(Prefix.parse("10.20.20.0/23")),
                hasCommunities(contains(acceptedCommunity)))));
    assertThat(
        r1Routes,
        hasItem(allOf(hasPrefix(Prefix.parse("10.20.22.0/24")), hasCommunities(empty()))));
  }

  @Test
  public void testAutonomousSystemLoops() throws IOException {
    Configuration c = parseConfig("autonomous-system-loops");
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.parse("2.2.2.2/32"), allOf(hasAllowLocalAsIn(true), hasLocalAs(1L))))));
    assertThat(
        c,
        hasVrf(
            "FOO",
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.parse("3.3.3.3/32"), allOf(hasAllowLocalAsIn(true), hasLocalAs(1L))))));
  }

  @Test
  public void testAutonomousSystemLoopsNonDefaultRoutingInstance() throws IOException {
    Configuration c = parseConfig("autonomous-system-loops-routing-instance");
    assertThat(
        c,
        hasVrf(
            "FOO",
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.parse("2.2.2.2/32"), allOf(hasAllowLocalAsIn(true), hasLocalAs(1L))))));
  }

  /** Tests support for dynamic bgp parsing using "bgp allow" command */
  @Test
  public void testBgpAllow() throws IOException {
    Configuration c = parseConfig("bgp-allow");
    assertThat(
        c,
        hasDefaultVrf(hasBgpProcess(hasPassiveNeighbor(Prefix.parse("10.1.1.0/24"), anything()))));
  }

  @Test
  public void testParentChildTopology() throws IOException {
    String resourcePrefix = "org/batfish/grammar/juniper/testrigs/topology";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setLayer1TopologyText(resourcePrefix)
                .setConfigurationText(resourcePrefix, "r1", "r2")
                .build(),
            _folder);

    Layer1Topology layer1LogicalTopology =
        batfish.getTopologyProvider().getLayer1LogicalTopology(batfish.getNetworkSnapshot()).get();
    Layer2Topology layer2Topology =
        batfish.getTopologyProvider().getLayer2Topology(batfish.getNetworkSnapshot()).get();
    Topology layer3Topology =
        batfish.getTopologyProvider().getLayer3Topology(batfish.getNetworkSnapshot());

    // check layer-1 logical adjacencies
    assertThat(
        layer1LogicalTopology.getGraph().edges(),
        hasItem(new Layer1Edge("r1", "ae0", "r2", "ae0")));

    // check layer-2 adjacencies
    assertThat(
        layer2Topology.inSameBroadcastDomain("r1", "ge-0/0/0.0", "r2", "ge-0/0/0.0"),
        equalTo(true));
    assertThat(
        layer2Topology.inSameBroadcastDomain("r1", "ge-0/0/1.0", "r2", "ge-0/0/1.0"),
        equalTo(true));
    assertThat(layer2Topology.inSameBroadcastDomain("r1", "ae0.0", "r2", "ae0.0"), equalTo(true));

    // check layer-3 adjacencies
    assertThat(
        layer3Topology.getEdges(), not(hasItem(Edge.of("r1", "ge-0/0/0.0", "r2", "ge-0/0/0.0"))));
    assertThat(layer3Topology.getEdges(), hasItem(Edge.of("r1", "ge-0/0/1.0", "r2", "ge-0/0/1.0")));
    assertThat(layer3Topology.getEdges(), hasItem(Edge.of("r1", "ae0.0", "r2", "ae0.0")));
  }

  @Test
  public void testBgpClusterId() throws IOException {
    String testrigName = "rr";
    String configName = "rr";
    Ip neighbor1Ip = Ip.parse("2.2.2.2");
    Ip neighbor2Ip = Ip.parse("4.4.4.4");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration rr = configurations.get(configName);
    BgpProcess proc = rr.getDefaultVrf().getBgpProcess();
    BgpPeerConfig neighbor1 =
        proc.getActiveNeighbors().get(Prefix.create(neighbor1Ip, Prefix.MAX_PREFIX_LENGTH));
    BgpPeerConfig neighbor2 =
        proc.getActiveNeighbors().get(Prefix.create(neighbor2Ip, Prefix.MAX_PREFIX_LENGTH));

    assertThat(neighbor1, hasClusterId(Ip.parse("3.3.3.3").asLong()));
    assertThat(neighbor2, hasClusterId(Ip.parse("1.1.1.1").asLong()));
  }

  @Test
  public void testBgpMultipath() throws IOException {
    assertThat(
        parseConfig("bgp-multipath").getDefaultVrf(),
        hasBgpProcess(allOf(hasMultipathEbgp(true), hasMultipathIbgp(true))));

    assertThat(
        parseConfig("bgp-multipath-internal").getDefaultVrf(),
        hasBgpProcess(allOf(hasMultipathEbgp(false), hasMultipathIbgp(true))));

    assertThat(
        parseConfig("bgp-multipath-external").getDefaultVrf(),
        hasBgpProcess(allOf(hasMultipathEbgp(true), hasMultipathIbgp(false))));
  }

  @Test
  public void testBgpMultipathMultipleAs() throws IOException {
    String testrigName = "multipath-multiple-as";
    List<String> configurationNames =
        ImmutableList.of("multiple_as_disabled", "multiple_as_enabled", "multiple_as_mixed");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    MultipathEquivalentAsPathMatchMode multipleAsDisabled =
        configurations
            .get("multiple_as_disabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode multipleAsEnabled =
        configurations
            .get("multiple_as_enabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode multipleAsMixed =
        configurations
            .get("multiple_as_mixed")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();

    assertThat(multipleAsDisabled, equalTo(MultipathEquivalentAsPathMatchMode.FIRST_AS));
    assertThat(multipleAsEnabled, equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
    assertThat(multipleAsMixed, equalTo(MultipathEquivalentAsPathMatchMode.FIRST_AS));
  }

  /** Make sure bgp type internal properly sets remote as when non explicitly specified */
  @Test
  public void testBgpTypeInternalPeerAs() throws IOException {
    String hostname = "bgp-type-internal";
    Configuration c = parseConfig(hostname);
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(hasActiveNeighbor(Prefix.parse("1.1.1.1/32"), hasRemoteAs(1L)))));
  }

  @Test
  public void testSetCommunity() throws IOException {
    Configuration c = parseConfig("community");

    ConnectedRoute cr = new ConnectedRoute(Prefix.strict("1.0.0.0/24"), "blah");

    // p1
    RoutingPolicy p1 = c.getRoutingPolicies().get("p1");
    BgpRoute.Builder b1 =
        BgpRoute.builder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(5L))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p1.process(cr, b1, Ip.ZERO, DEFAULT_VRF_NAME, Direction.OUT);
    BgpRoute br1 = b1.build();

    assertThat(
        br1.getCommunities(),
        equalTo(
            ImmutableSet.of(
                WellKnownCommunity.NO_ADVERTISE,
                WellKnownCommunity.NO_EXPORT,
                WellKnownCommunity.NO_EXPORT_SUBCONFED)));

    // p2
    RoutingPolicy p2 = c.getRoutingPolicies().get("p2");
    BgpRoute.Builder b2 =
        BgpRoute.builder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(5L))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p2.process(cr, b2, Ip.ZERO, DEFAULT_VRF_NAME, Direction.OUT);
    BgpRoute br2 = b2.build();

    assertThat(br2.getCommunities(), equalTo(ImmutableSet.of(2L, 3L)));

    // p3
    RoutingPolicy p3 = c.getRoutingPolicies().get("p3");
    BgpRoute.Builder b3 =
        BgpRoute.builder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(5L))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p3.process(cr, b3, Ip.ZERO, DEFAULT_VRF_NAME, Direction.OUT);
    BgpRoute br3 = b3.build();

    assertThat(br3.getCommunities(), equalTo(ImmutableSet.of(5L)));
  }

  @Test
  public void testAddCommunity() throws IOException {
    Configuration c = parseConfig("community");

    ConnectedRoute cr = new ConnectedRoute(Prefix.strict("1.0.0.0/24"), "blah");

    // p4
    RoutingPolicy p4 = c.getRoutingPolicies().get("p4");
    BgpRoute.Builder b4 =
        BgpRoute.builder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(5L))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p4.process(cr, b4, Ip.ZERO, DEFAULT_VRF_NAME, Direction.OUT);
    BgpRoute br4 = b4.build();

    assertThat(
        br4.getCommunities(),
        equalTo(
            ImmutableSet.of(
                WellKnownCommunity.NO_ADVERTISE,
                WellKnownCommunity.NO_EXPORT,
                WellKnownCommunity.NO_EXPORT_SUBCONFED,
                5L)));

    // p5
    RoutingPolicy p5 = c.getRoutingPolicies().get("p5");
    BgpRoute.Builder b5 =
        BgpRoute.builder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(5L))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p5.process(cr, b5, Ip.ZERO, DEFAULT_VRF_NAME, Direction.OUT);
    BgpRoute br5 = b5.build();

    assertThat(br5.getCommunities(), equalTo(ImmutableSet.of(2L, 3L, 5L)));

    // p6
    RoutingPolicy p6 = c.getRoutingPolicies().get("p6");
    BgpRoute.Builder b6 =
        BgpRoute.builder()
            .setNetwork(cr.getNetwork())
            .setCommunities(ImmutableSet.of(5L))
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    p6.process(cr, b6, Ip.ZERO, DEFAULT_VRF_NAME, Direction.OUT);
    BgpRoute br6 = b6.build();

    assertThat(br6.getCommunities(), equalTo(ImmutableSet.of(5L)));
  }

  @Test
  public void testDefaultApplications() throws IOException {
    String hostname = "default-applications";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    SortedMap<String, SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>>>
        undefinedReferences = ccae.getUndefinedReferences();
    Configuration c = parseConfig(hostname);

    String aclApplicationsName = zoneToZoneFilter("z1", "z2");
    String aclApplicationSetName = zoneToZoneFilter("z1", "z3");
    String aclApplicationSetAnyName = zoneToZoneFilter("z1", "z4");
    String aclApplicationAnyName = zoneToZoneFilter("z1", "z5");
    String z1Interface = "ge-0/0/0.0";
    IpAccessList aclApplication = c.getIpAccessLists().get(aclApplicationsName);
    IpAccessList aclApplicationSet = c.getIpAccessLists().get(aclApplicationSetName);
    IpAccessList aclApplicationSetAny = c.getIpAccessLists().get(aclApplicationSetAnyName);
    IpAccessList aclApplicationAny = c.getIpAccessLists().get(aclApplicationAnyName);
    /* Allowed applications permits TCP to port 80 and 443 */
    Flow permittedHttpFlow = createFlow(IpProtocol.TCP, 80);
    Flow permittedHttpsFlow = createFlow(IpProtocol.TCP, 443);
    Flow rejectedFlow = createFlow(IpProtocol.TCP, 100);

    /*
     * Confirm there are no undefined references
     */
    assertThat(undefinedReferences.keySet(), emptyIterable());

    /*
     * Confirm acl with explicit application constraints accepts http and https flows and rejects
     * others
     */
    assertThat(
        aclApplication,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplication,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplication, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm acl with indirect constraints (application-set) accepts http and https flows and
     * rejects others
     */
    assertThat(
        aclApplicationSet,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSet,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSet,
        rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm policy referencing permissive application-set accepts all three flows
     */
    assertThat(
        aclApplicationSetAny,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSetAny,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSetAny,
        accepts(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm policy directly permitting any application accepts all three flows
     */
    assertThat(
        aclApplicationAny,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationAny,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationAny,
        accepts(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testEnforceFirstAs() throws IOException {
    String hostname = "bgp-enforce-first-as";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasNeighbors(hasValue(hasEnforceFirstAs())))));
  }

  @Test
  public void testEthernetSwitchingFilterReference() throws IOException {
    String hostname = "ethernet-switching-filter";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* esfilter should be referred, while esfilter2 should be unreferred */
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "esfilter", 1));
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "esfilter2", 0));
  }

  @Test
  public void testFirewallFilters() throws IOException {
    String hostname = "firewall-filters";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm filter usage is tracked properly */
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "FILTER1", 1));
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "FILTER2", 2));
    assertThat(ccae, hasNumReferrers(filename, FIREWALL_FILTER, "FILTER_UNUSED", 0));

    /* Confirm undefined reference is identified */
    assertThat(ccae, hasUndefinedReference(filename, FIREWALL_FILTER, "FILTER_UNDEF"));
  }

  @Test
  public void testFirewallCombinedPolicies() throws IOException {
    Configuration c = parseConfig("firewall-combined-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String addrPermitted = "1.2.3.1";
    String addrDeniedByZonePolicy = "1.2.3.3";
    String addrDeniedByGlobalPolicy = "1.2.3.7";
    String addrDeniedByFilter = "2.2.2.2";
    String addrDefaultPolicy = "1.2.3.15";
    String untrustIpAddr = "1.2.4.5";

    Flow flowPermitted = createFlow(addrPermitted, untrustIpAddr);
    Flow flowDeniedByZonePolicy = createFlow(addrDeniedByZonePolicy, untrustIpAddr);
    Flow flowDeniedByGlobalPolicy = createFlow(addrDeniedByGlobalPolicy, untrustIpAddr);
    Flow flowDeniedByFilter = createFlow(addrDeniedByFilter, untrustIpAddr);
    Flow flowDefaultPolicy = createFlow(addrDefaultPolicy, untrustIpAddr);

    IpAccessList aclUntrustOut = c.getAllInterfaces().get(interfaceNameUntrust).getOutgoingFilter();
    IpAccessList aclUntrustSecurity =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /* Confirm flow from address explicitly allowed by zone policy is accepted */
    assertThat(
        aclUntrustSecurity,
        accepts(flowPermitted, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm flow from trust interface not matching any policy deny is accepted (accepted by default permit-all) */
    assertThat(
        aclUntrustSecurity,
        accepts(flowDefaultPolicy, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm flow matching zone policy deny is rejected */
    assertThat(
        aclUntrustSecurity,
        rejects(flowDeniedByZonePolicy, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm flow blocked by the outgoing filter is rejected */
    assertThat(
        aclUntrustOut,
        rejects(flowDeniedByFilter, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm flow matching global policy deny is rejected */
    assertThat(
        aclUntrustSecurity,
        rejects(
            flowDeniedByGlobalPolicy, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));

    /* Confirm traffic originating from the device is not blocked by policies */
    assertThat(
        aclUntrustSecurity,
        accepts(flowDeniedByZonePolicy, null, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclUntrustSecurity,
        accepts(flowDeniedByGlobalPolicy, null, c.getIpAccessLists(), c.getIpSpaces()));
    /* Confirm traffic originating from the device is still blocked by an outgoing filter */
    assertThat(
        aclUntrustOut, rejects(flowDeniedByFilter, null, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallGlobalAddressBook() throws IOException {
    Configuration c = parseConfig("firewall-global-address-book");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String specificSpaceName = "global~ADDR1";
    String wildcardSpaceName = "global~ADDR2";
    String indirectSpaceName = "global~ADDRSET";

    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-set
    String specificAddr = "2.2.2.2";
    // Address allowed by the wildcard-address in the address-set
    String wildcardAddr = "1.3.3.4";
    // Address not allowed by either entry in the address-set
    String notWildcardAddr = "1.2.3.5";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromWildcardAddr = createFlow(wildcardAddr, untrustIpAddr);
    Flow flowFromNotWildcardAddr = createFlow(notWildcardAddr, untrustIpAddr);
    IpAccessList untrustAcl =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have three global IpSpaces in the config
    assertThat(
        c.getIpSpaces().keySet(),
        containsInAnyOrder(specificSpaceName, wildcardSpaceName, indirectSpaceName));
    // And associated metadata
    assertThat(
        c.getIpSpaceMetadata().keySet(),
        containsInAnyOrder(specificSpaceName, wildcardSpaceName, indirectSpaceName));

    IpSpace specificSpace = c.getIpSpaces().get(specificSpaceName);
    IpSpace wildcardSpace = c.getIpSpaces().get(wildcardSpaceName);
    IpSpace indirectSpace = c.getIpSpaces().get(indirectSpaceName);

    // Specific space should contain the specific addr and not others
    assertThat(specificSpace, containsIp(Ip.parse(specificAddr)));
    assertThat(specificSpace, not(containsIp(Ip.parse(wildcardAddr))));

    // Wildcard space should contain the wildcard addr and not others
    assertThat(wildcardSpace, containsIp(Ip.parse(wildcardAddr)));
    assertThat(wildcardSpace, not(containsIp(Ip.parse(notWildcardAddr))));

    // Indirect space should contain both specific and wildcard addr, but not others
    assertThat(indirectSpace, containsIp(Ip.parse(specificAddr), c.getIpSpaces()));
    assertThat(indirectSpace, containsIp(Ip.parse(wildcardAddr), c.getIpSpaces()));
    assertThat(indirectSpace, not(containsIp(Ip.parse(notWildcardAddr), c.getIpSpaces())));

    // Specifically allowed source addr should be accepted
    assertThat(
        untrustAcl,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source addr covered by the wildcard entry should be accepted
    assertThat(
        untrustAcl,
        accepts(flowFromWildcardAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source addr covered by neither addr-set entry should be rejected
    assertThat(
        untrustAcl,
        rejects(
            flowFromNotWildcardAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallGlobalPolicy() throws IOException {
    Configuration c = parseConfig("firewall-global-policy");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /*
     * Should have four ACLs:
     *  Explicitly defined in the config file:
     *    One from the global security policy
     *  Generated by logic in toVendorIndependent
     *    One permitting existing connections (default firewall behavior)
     *    Two defining security policies for each interface (combines explicit security policy with
     *        implicit security policies like allow existing connection)
     */
    assertThat(
        c.getIpAccessLists().keySet(),
        containsInAnyOrder(
            ACL_NAME_GLOBAL_POLICY,
            ACL_NAME_EXISTING_CONNECTION,
            ACL_NAME_SECURITY_POLICY + interfaceNameTrust,
            ACL_NAME_SECURITY_POLICY + interfaceNameUntrust));

    /* Flows in either direction should be permitted by the global policy */
    assertThat(
        aclUntrustOut,
        accepts(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        accepts(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallGlobalPolicyGlobalAddressBook() throws IOException {
    /*
     * Test address book behavior when used in a global policy
     * i.e. a policy that does not have fromZone or toZone
     */
    Configuration c = parseConfig("firewall-global-policy-global-address-book");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";
    String trustedSpaceName = "global~ADDR1";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /* Make sure the global-address-book address is the only config ipSpace */
    assertThat(c.getIpSpaces().keySet(), containsInAnyOrder(trustedSpaceName));

    IpSpace ipSpace = Iterables.getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(Ip.parse(trustedIpAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(Ip.parse(untrustedIpAddr))));

    /* Flow from ADDR1 to untrust should be permitted */
    assertThat(
        aclUntrustOut,
        accepts(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Flow from not ADDR1 to trust should be rejected */
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallNoPolicies() throws IOException {
    Configuration c = parseConfig("firewall-no-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /*
     * Should have three ACLs generated by logic in toVendorIndependent:
     *    One permitting existing connections (default firewall behavior)
     *    Two defining security policies for each interface (combines explicit security policy with
     *        implicit security policies like allow existing connection)
     */
    assertThat(
        c.getIpAccessLists().keySet(),
        containsInAnyOrder(
            ACL_NAME_EXISTING_CONNECTION,
            ACL_NAME_SECURITY_POLICY + interfaceNameTrust,
            ACL_NAME_SECURITY_POLICY + interfaceNameUntrust));

    /* Simple flow in either direction should be blocked */
    assertThat(
        aclUntrustOut,
        rejects(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallPolicies() throws IOException {
    Configuration c = parseConfig("firewall-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String securityPolicyName = zoneToZoneFilter("trust", "untrust");
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);
    Flow trustToUntrustReturnFlow =
        createFlow(trustedIpAddr, untrustedIpAddr, FlowState.ESTABLISHED);
    Flow untrustToTrustReturnFlow =
        createFlow(untrustedIpAddr, trustedIpAddr, FlowState.ESTABLISHED);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    /*
     * Should have four ACLs:
     *  Explicitly defined in the config file:
     *    One from the security policy from trust to untrust
     *  Generated by logic in toVendorIndependent
     *    One permitting existing connections (default firewall behavior)
     *    Two defining security policies for each interface (combines explicit security policy with
     *        implicit security policies like allow existing connection)
     */
    assertThat(
        c.getIpAccessLists().keySet(),
        containsInAnyOrder(
            securityPolicyName,
            aclTrustOut.getName(),
            aclUntrustOut.getName(),
            ACL_NAME_EXISTING_CONNECTION));

    /* Simple flow from trust to untrust should be permitted */
    assertThat(
        aclUntrustOut,
        accepts(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));

    /* Simple flow from untrust to trust should be blocked */
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));

    /* Return flow in either direction should be permitted */
    assertThat(
        aclUntrustOut,
        accepts(
            trustToUntrustReturnFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        accepts(
            untrustToTrustReturnFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZoneAddressBookInline() throws IOException {
    Configuration c = parseConfig("firewall-zone-address-book-inline");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-book
    String specificAddr = "2.2.2.2";
    // Address not allowed by the address-book
    String notAllowedAddr = "3.3.3.3";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromNotAllowedAddr = createFlow(notAllowedAddr, untrustIpAddr);

    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have a an IpSpace in the config corresponding to the trust zone's ADDR1 address
    final String ipSpaceName = "trust~ADDR1";
    assertThat(c.getIpSpaces(), hasKey(equalTo(ipSpaceName)));

    // It should be the only IpSpace
    assertThat(c.getIpSpaces().keySet(), iterableWithSize(1));
    IpSpace ipSpace = Iterables.getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(Ip.parse(specificAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(Ip.parse(notAllowedAddr))));

    // There should be metadata for this ipspace
    assertThat(c.getIpSpaceMetadata(), hasKey(ipSpaceName));

    // Specifically allowed source address should be accepted
    assertThat(
        aclUntrustOut,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source address not covered by the address-book should be rejected
    assertThat(
        aclUntrustOut,
        rejects(flowFromNotAllowedAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  /**
   * Similar test to {@link #testFirewallZoneAddressBookInline()} except with attached instead of
   * inlined address book
   */
  @Test
  public void testFirewallZoneAddressBookAttach() throws IOException {
    Configuration c = parseConfig("firewall-zone-address-book-attach");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-book
    String specificAddr = "2.2.2.2";
    // Address not allowed by the address-book
    String notAllowedAddr = "3.3.3.3";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromNotAllowedAddr = createFlow(notAllowedAddr, untrustIpAddr);

    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have a an IpSpace in the config corresponding to the trust zone's ADDR1 address
    final String ipSpaceName = "trust-book~ADDR1";
    assertThat(c.getIpSpaces(), hasKey(equalTo(ipSpaceName)));

    // It should be the only IpSpace
    assertThat(c.getIpSpaces().keySet(), iterableWithSize(1));
    IpSpace ipSpace = Iterables.getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(Ip.parse(specificAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(Ip.parse(notAllowedAddr))));

    // There should be metadata for this ipspace
    assertThat(c.getIpSpaceMetadata(), hasKey(ipSpaceName));

    // Specifically allowed source address should be accepted
    assertThat(
        aclUntrustOut,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source address not covered by the address-book should be rejected
    assertThat(
        aclUntrustOut,
        rejects(flowFromNotAllowedAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  /**
   * Similar test to {@link #testFirewallZoneAddressBookInline()} except with attached instead of
   * inlined address book
   */
  @Test
  public void testFirewallZoneAddressBookGlobal() throws IOException {
    Configuration c = parseConfig("firewall-zone-address-book-global");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-book
    String specificAddr = "2.2.2.2";
    // Address not allowed by the address-book
    String notAllowedAddr = "3.3.3.3";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromNotAllowedAddr = createFlow(notAllowedAddr, untrustIpAddr);

    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have a an IpSpace in the config corresponding to the trust zone's ADDR1 address
    final String ipSpaceName = "global~ADDR1";
    assertThat(c.getIpSpaces(), hasKey(equalTo(ipSpaceName)));

    // It should be the only IpSpace
    assertThat(c.getIpSpaces().keySet(), iterableWithSize(1));
    IpSpace ipSpace = Iterables.getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(Ip.parse(specificAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(Ip.parse(notAllowedAddr))));

    // There should be metadata for this ipspace
    assertThat(c.getIpSpaceMetadata(), hasKey(ipSpaceName));

    // Specifically allowed source address should be accepted
    assertThat(
        aclUntrustOut,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source address not covered by the address-book should be rejected
    assertThat(
        aclUntrustOut,
        rejects(flowFromNotAllowedAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  /**
   * When both global and zone-specific (attached or inline) address books are present, global loses
   */
  @Test
  public void testFirewallZoneAddressBookGlobalLoses() throws IOException {
    Configuration c = parseConfig("firewall-zone-address-book-global-loses");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-book
    String specificAddr = "2.2.2.2";
    // Address not allowed by the address-book
    String notAllowedAddr = "3.3.3.3";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromNotAllowedAddr = createFlow(notAllowedAddr, untrustIpAddr);

    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Specifically allowed source address should be accepted
    assertThat(
        aclUntrustOut,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source address not covered by the zone address-book should be rejected
    assertThat(
        aclUntrustOut,
        rejects(flowFromNotAllowedAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZoneAddressUndefined() throws IOException {
    Configuration c = parseConfig("firewall-zone-address-undefined");

    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String addrAccepted = "2.2.2.2";
    String addrRejected = "2.2.2.3";

    Flow flowAccepted = createFlow(addrAccepted, addrAccepted);
    Flow flowRejected = createFlow(addrAccepted, addrRejected);

    IpAccessList aclUntrust =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();
    IpAccessList aclTrust =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();

    // Make sure flow matching address-book entry is accepted despite the rule having one undefined
    // destination address
    assertThat(
        aclUntrust,
        accepts(flowAccepted, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Make sure flow not matching address-book entry is rejected
    assertThat(
        aclUntrust,
        rejects(flowRejected, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));

    // Make sure both flows are rejected by rule with no defined destination address
    assertThat(
        aclTrust,
        rejects(flowAccepted, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrust,
        rejects(flowRejected, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZoneAddressBookAttachAndGlobal() throws IOException {
    Configuration c = parseConfig("firewall-zone-address-book-attach-and-global");

    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Destination address allowed by the address-book
    String destAddr = "2.2.2.2";
    // Source address allowed by the address-book
    String sourceAddr = "3.3.3.3";

    Flow flowAllowed = createFlow(sourceAddr, destAddr);
    Flow flowRejected1 = createFlow(destAddr, destAddr);
    Flow flowRejected2 = createFlow(sourceAddr, sourceAddr);

    IpAccessList acl =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Confirm both global and attached address-book entries are processed properly
    // Make sure the flow with source address matching global book and destination address matching
    // attached book is accepted
    assertThat(
        acl, accepts(flowAllowed, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Make sure flow with different addresses is denied
    assertThat(
        acl, rejects(flowRejected1, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        acl, rejects(flowRejected2, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZones() throws IOException {
    Configuration c = parseConfig("firewall-no-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String zoneTrust = "trust";
    String zoneUntrust = "untrust";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut =
        c.getAllInterfaces().get(interfaceNameTrust).getPreTransformationOutgoingFilter();
    IpAccessList aclUntrustOut =
        c.getAllInterfaces().get(interfaceNameUntrust).getPreTransformationOutgoingFilter();

    // Should have two zones
    assertThat(c.getZones().keySet(), containsInAnyOrder(zoneTrust, zoneUntrust));

    // Should have two logical interfaces
    assertThat(c.getAllInterfaces().keySet(), hasItems(interfaceNameTrust, interfaceNameUntrust));

    // Confirm the interfaces are associated with their zones
    assertThat(c.getAllInterfaces().get(interfaceNameTrust), hasZoneName(equalTo(zoneTrust)));
    assertThat(c.getAllInterfaces().get(interfaceNameUntrust), hasZoneName(equalTo(zoneUntrust)));

    /* Simple flows should be blocked */
    assertThat(
        aclUntrustOut,
        rejects(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testAggregateDefaults() throws IOException {
    Configuration config = parseConfig("aggregate-defaults");

    Set<GeneratedRoute> aggregateRoutes = config.getDefaultVrf().getGeneratedRoutes();
    GeneratedRoute ar1 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar2 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar3 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();

    // passive default
    // policies should be generated only for the active ones
    assertThat(ar1.getGenerationPolicy(), nullValue());
    assertThat(
        ar2.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(ar3.getGenerationPolicy(), nullValue());

    Set<GeneratedRoute> aggregateRoutesRi1 = config.getVrfs().get("ri1").getGeneratedRoutes();
    GeneratedRoute ar1Ri1 =
        aggregateRoutesRi1.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar2Ri1 =
        aggregateRoutesRi1.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar3Ri1 =
        aggregateRoutesRi1.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();

    // active default
    // policies should be generated only for the active ones
    assertThat(
        ar1Ri1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("1.0.0.0/8"))));
    assertThat(
        ar2Ri1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(ar3Ri1.getGenerationPolicy(), nullValue());
  }

  @Test
  public void testAggregateRoutesGenerationPolicies() throws IOException {
    Configuration config = parseConfig("aggregate-routes");

    Set<GeneratedRoute> aggregateRoutes = config.getDefaultVrf().getGeneratedRoutes();
    GeneratedRoute ar1 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar2 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar3 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar4 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("4.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar5 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("5.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute ar6 =
        aggregateRoutes.stream()
            .filter(ar -> ar.getNetwork().equals(Prefix.parse("6.0.0.0/8")))
            .findAny()
            .get();

    // policies should be generated only for the active ones
    assertThat(
        ar1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("1.0.0.0/8"))));
    assertThat(
        ar2.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeAggregatedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(ar3.getGenerationPolicy(), nullValue());

    // the second one should only accept 2.0.0.0/32 as a contributor
    RoutingPolicy rp2 = config.getRoutingPolicies().get(ar2.getGenerationPolicy());
    ConnectedRoute cr31 = new ConnectedRoute(Prefix.parse("2.0.0.0/31"), "blah");
    ConnectedRoute cr32 = new ConnectedRoute(Prefix.parse("2.0.0.0/32"), "blah");
    assertThat(
        rp2.process(cr31, BgpRoute.builder(), null, DEFAULT_VRF_NAME, Direction.OUT),
        equalTo(false));
    assertThat(
        rp2.process(cr32, BgpRoute.builder(), null, DEFAULT_VRF_NAME, Direction.OUT),
        equalTo(true));

    // all should be discard routes
    assertThat(ar1.getDiscard(), equalTo(true));
    assertThat(ar2.getDiscard(), equalTo(true));
    assertThat(ar3.getDiscard(), equalTo(true));

    // policy semantics

    // falls through without changing default, so accept
    RoutingPolicy rp4 = config.getRoutingPolicies().get(ar4.getGenerationPolicy());
    ConnectedRoute cr4 = new ConnectedRoute(Prefix.parse("4.0.0.0/32"), "blah");
    assertThat(
        rp4.process(cr4, BgpRoute.builder(), null, DEFAULT_VRF_NAME, Direction.OUT), equalTo(true));

    // rejects first, so reject
    RoutingPolicy rp5 = config.getRoutingPolicies().get(ar5.getGenerationPolicy());
    ConnectedRoute cr5 = new ConnectedRoute(Prefix.parse("5.0.0.0/32"), "blah");
    assertThat(
        rp5.process(cr5, BgpRoute.builder(), null, DEFAULT_VRF_NAME, Direction.OUT),
        equalTo(false));

    // accepts first, so accept
    RoutingPolicy rp6 = config.getRoutingPolicies().get(ar6.getGenerationPolicy());
    ConnectedRoute cr6 = new ConnectedRoute(Prefix.parse("6.0.0.0/32"), "blah");
    assertThat(
        rp6.process(cr6, BgpRoute.builder(), null, DEFAULT_VRF_NAME, Direction.OUT), equalTo(true));
  }

  @Test
  public void testGeneratedDefaults() throws IOException {
    Configuration config = parseConfig("generated-defaults");

    Set<GeneratedRoute> generatedRoutes = config.getDefaultVrf().getGeneratedRoutes();
    GeneratedRoute gr1 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr2 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr3 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();

    // passive default
    // policies should be generated only for the active ones
    assertThat(gr1.getGenerationPolicy(), nullValue());
    assertThat(
        gr2.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(gr3.getGenerationPolicy(), nullValue());

    Set<GeneratedRoute> generatedRi1 = config.getVrfs().get("ri1").getGeneratedRoutes();
    GeneratedRoute gr1Ri1 =
        generatedRi1.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr2Ri1 =
        generatedRi1.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr3Ri1 =
        generatedRi1.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();

    // active default
    // policies should be generated only for the active ones
    assertThat(
        gr1Ri1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("1.0.0.0/8"))));
    assertThat(
        gr2Ri1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(gr3Ri1.getGenerationPolicy(), nullValue());
  }

  @Test
  public void testGeneratedRoutesGenerationPolicies() throws IOException {
    Configuration config = parseConfig("generated-routes");

    Set<GeneratedRoute> generatedRoutes = config.getDefaultVrf().getGeneratedRoutes();
    GeneratedRoute gr1 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("1.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr2 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("2.0.0.0/8")))
            .findAny()
            .get();
    GeneratedRoute gr3 =
        generatedRoutes.stream()
            .filter(gr -> gr.getNetwork().equals(Prefix.parse("3.0.0.0/8")))
            .findAny()
            .get();

    // policies should be generated only for the active ones
    assertThat(
        gr1.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("1.0.0.0/8"))));
    assertThat(
        gr2.getGenerationPolicy(),
        equalTo(
            JuniperConfiguration.computeGeneratedRouteGenerationPolicyName(
                Prefix.parse("2.0.0.0/8"))));
    assertThat(gr3.getGenerationPolicy(), nullValue());

    // the second one should only accept 2.0.0.0/32 as a contributor
    RoutingPolicy rp2 = config.getRoutingPolicies().get(gr2.getGenerationPolicy());
    ConnectedRoute cr31 = new ConnectedRoute(Prefix.parse("2.0.0.0/31"), "blah");
    ConnectedRoute cr32 = new ConnectedRoute(Prefix.parse("2.0.0.0/32"), "blah");
    assertThat(
        rp2.process(cr31, BgpRoute.builder(), null, DEFAULT_VRF_NAME, Direction.OUT),
        equalTo(false));
    assertThat(
        rp2.process(cr32, BgpRoute.builder(), null, DEFAULT_VRF_NAME, Direction.OUT),
        equalTo(true));

    // none should be discard routes
    assertThat(gr1.getDiscard(), equalTo(false));
    assertThat(gr2.getDiscard(), equalTo(false));
    assertThat(gr3.getDiscard(), equalTo(false));
  }

  @Test
  public void testGeneratedRouteCommunities() throws IOException {
    Configuration config =
        BatfishTestUtils.parseTextConfigs(
                _folder, "org/batfish/grammar/juniper/testconfigs/generated-route-communities")
            .get("generated-route-communities");
    assertThat(
        config.getDefaultVrf().getGeneratedRoutes().stream()
            .map(GeneratedRoute::getCommunities)
            .collect(ImmutableSet.toImmutableSet()),
        equalTo(ImmutableSet.of(ImmutableSortedSet.of(65537L))));
  }

  @Test
  public void testNestedConfig() throws IOException {
    String hostname = "nested-config";

    /* Confirm a simple extraction (hostname) works for nested config format */
    assertThat(parseTextConfigs(hostname).keySet(), contains(hostname));
  }

  @Test
  public void testNestedConfigLineComments() throws IOException {
    String hostname = "nested-config-line-comments";

    // Confirm extraction works for nested configs even in the presence of line comments
    assertThat(parseTextConfigs(hostname).keySet(), contains(hostname));
  }

  @Test
  public void testNestedConfigStructureDef() throws IOException {
    String hostname = "nested-config-structure-def";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm defined structures in nested config show up with original definition line numbers */
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, FIREWALL_FILTER, "FILTER1", contains(6, 7, 8, 9, 11, 12)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, FIREWALL_FILTER, "FILTER2", contains(16, 17, 18, 19)));
  }

  @Test
  public void testNestedConfigLineMap() {
    String hostname = "nested-config";
    Flattener flattener =
        Batfish.flatten(
            CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname),
            new BatfishLogger(BatfishLogger.LEVELSTR_OUTPUT, false),
            new Settings(),
            new Warnings(),
            ConfigurationFormat.JUNIPER,
            VendorConfigurationFormatDetector.BATFISH_FLATTENED_JUNIPER_HEADER);
    FlattenerLineMap lineMap = flattener.getOriginalLineMap();
    /*
     * Flattened config should be two lines: header line and set-host-name line
     * This test is only checking content of the set-host-name line
     */
    String flatText = flattener.getFlattenedConfigurationText().split("\n", -1)[1];

    /* Confirm original line numbers are preserved */
    assertThat(lineMap.getOriginalLine(2, flatText.indexOf("system")), equalTo(2));
    assertThat(lineMap.getOriginalLine(2, flatText.indexOf("host-name")), equalTo(3));
    assertThat(lineMap.getOriginalLine(2, flatText.indexOf("nested-config")), equalTo(3));
  }

  @Test
  public void testOspfAreaDefaultMetric() throws IOException {
    Configuration config =
        BatfishTestUtils.parseTextConfigs(
                _folder, "org/batfish/grammar/juniper/testconfigs/ospf-area-default-metric")
            .get("ospf-area-default-metric");
    OspfArea area1 = config.getDefaultVrf().getOspfProcess().getAreas().get(1L);
    assertThat(area1, hasInjectDefaultRoute());
    assertThat(area1, hasMetricOfDefaultRoute(equalTo(10)));

    OspfArea area2 = config.getDefaultVrf().getOspfProcess().getAreas().get(2L);
    assertThat(area2, not(hasInjectDefaultRoute()));
  }

  @Test
  public void testOspfMetric() throws IOException {
    Configuration config =
        BatfishTestUtils.parseTextConfigs(
                _folder, "org/batfish/grammar/juniper/testconfigs/ospfmetric")
            .get("ospfmetric");
    OspfAreaSummary summary =
        config
            .getDefaultVrf()
            .getOspfProcess()
            .getAreas()
            .get(1L)
            .getSummaries()
            .get(Prefix.parse("10.0.0.0/16"));
    assertThat(summary, not(isAdvertised()));
    assertThat(summary, hasMetric(123L));

    // Defaults
    summary =
        config
            .getDefaultVrf()
            .getOspfProcess()
            .getAreas()
            .get(2L)
            .getSummaries()
            .get(Prefix.parse("10.0.0.0/16"));
    assertThat(summary, isAdvertised());
    assertThat(summary, hasMetric(nullValue()));

    // Interface override
    assertThat(config, hasInterface("fe-1/0/1.0", hasOspfCost(equalTo(17))));
  }

  @Test
  public void testOspfPsk() throws IOException {
    /* allow both encrypted and unencrypted key */
    parseConfig("ospf-psk");
  }

  @Test
  public void testOspfReferenceBandwidth() throws IOException {
    String hostname = "ospf-reference-bandwidth";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasReferenceBandwidth(equalTo(1E9D)))));
    assertThat(c, hasVrf("vrf1", hasOspfProcess(hasReferenceBandwidth(equalTo(2E9D)))));
    assertThat(c, hasVrf("vrf2", hasOspfProcess(hasReferenceBandwidth(equalTo(3E9D)))));
    assertThat(c, hasVrf("vrf3", hasOspfProcess(hasReferenceBandwidth(equalTo(4E9D)))));
    assertThat(c, hasVrf("vrf4", hasOspfProcess(hasReferenceBandwidth(equalTo(5E9D)))));
  }

  @Test
  public void testPsPreferenceBehavior() throws IOException {
    Configuration c = parseConfig("policy-statement-preference");

    RoutingPolicy policyPreference = c.getRoutingPolicies().get("preference");

    StaticRoute staticRoute =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("10.0.1.0/24"))
            .setNextHopInterface("nextint")
            .setNextHopIp(Ip.parse("10.0.0.1"))
            .setAdministrativeCost(1)
            .build();

    Environment.Builder eb = Environment.builder(c).setDirection(Direction.IN);
    eb.setVrf("vrf1");
    policyPreference.call(
        eb.setOriginalRoute(staticRoute).setOutputRoute(OspfExternalType2Route.builder()).build());

    // Checking admin cost set on the output route
    assertThat(eb.build().getOutputRoute().getAdmin(), equalTo(123));
  }

  @Test
  public void testPsPreferenceStructure() throws IOException {
    Configuration c = parseConfig("policy-statement-preference");

    Environment.Builder eb = Environment.builder(c).setDirection(Direction.IN);
    eb.setVrf("vrf1");

    RoutingPolicy policyPreference = c.getRoutingPolicies().get("preference");

    assertThat(policyPreference.getStatements(), hasSize(2));

    // Extracting the If statement
    assertThat(policyPreference.getStatements().get(0), instanceOf(If.class));

    If i = (If) policyPreference.getStatements().get(0);

    assertThat(i.getTrueStatements(), hasSize(1));
    assertThat(
        Iterables.getOnlyElement(i.getTrueStatements()), instanceOf(SetAdministrativeCost.class));

    assertThat(
        Iterables.getOnlyElement(i.getTrueStatements()),
        isSetAdministrativeCostThat(hasAdmin(isLiteralIntThat(hasVal(123)))));
  }

  @Test
  public void testTacplusPsk() throws IOException {
    /* allow both encrypted and unencrypted key */
    parseConfig("tacplus-psk");
  }

  @Test
  public void testIkePolicy() throws IOException {
    Configuration c = parseConfig("ike-policy");

    assertThat(
        c,
        hasIkePhase1Policy(
            "policy1",
            allOf(
                hasIkePhase1Key(
                    IkePhase1KeyMatchers.hasKeyHash(
                        CommonUtil.sha256Digest("psk1" + CommonUtil.salt()))),
                hasIkePhase1Proposals(equalTo(ImmutableList.of("proposal1"))))));
  }

  @Test
  public void testIkeProposal() throws IOException {
    Configuration c = parseConfig("ike-proposal");

    // test for IKE phase1 proposals
    assertThat(
        c,
        hasIkePhase1Proposal(
            "proposal1",
            allOf(
                IkePhase1ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC),
                IkePhase1ProposalMatchers.hasAuthenticationMethod(
                    IkeAuthenticationMethod.PRE_SHARED_KEYS),
                IkePhase1ProposalMatchers.hasHashingAlgorithm(IkeHashingAlgorithm.MD5),
                IkePhase1ProposalMatchers.hasDiffieHellmanGroup(DiffieHellmanGroup.GROUP14),
                IkePhase1ProposalMatchers.hasLifeTimeSeconds(50000))));
  }

  @Test
  public void testInterfaceArp() throws IOException {
    Configuration c = parseConfig("interface-arp");

    /* The additional ARP IP set for irb.0 should appear in the data model */
    assertThat(c, hasInterface("irb.0", hasAdditionalArpIps(containsIp(Ip.parse("1.0.0.2")))));
  }

  @Test
  public void testInterfaceBandwidth() throws IOException {
    Configuration c = parseConfig("interface-bandwidth");

    // Configuration has ge-0/0/0 with four units configured bandwidths 5000000000, 5000000k, 5000m,
    // 5g. Physical interface should have default bandwidth (1E9), unit interfaces should have 5E9.
    double unitBandwidth = 5E9;
    double physicalBandwidth =
        org.batfish.representation.juniper.Interface.getDefaultBandwidthByName("ge-0/0/0");

    assertThat(c, hasInterface("ge-0/0/0", hasBandwidth(physicalBandwidth)));
    assertThat(c, hasInterface("ge-0/0/0.0", hasBandwidth(unitBandwidth)));
    assertThat(c, hasInterface("ge-0/0/0.1", hasBandwidth(unitBandwidth)));
    assertThat(c, hasInterface("ge-0/0/0.2", hasBandwidth(unitBandwidth)));
    assertThat(c, hasInterface("ge-0/0/0.3", hasBandwidth(unitBandwidth)));

    // Configuration has ge-1/0/0 with one unit with configured bandwidth 10c (1c = 384 bps).
    // Physical interface should have default bandwidth (1E9), unit 3840.
    assertThat(c, hasInterface("ge-1/0/0", hasBandwidth(physicalBandwidth)));
    assertThat(c, hasInterface("ge-1/0/0.0", hasBandwidth(3840)));
  }

  @Test
  public void testInterfaceMtu() throws IOException {
    Configuration c = parseConfig("interfaceMtu");

    /* Properly configured interfaces should be present in respective areas. */
    assertThat(c.getAllInterfaces().keySet(), hasItem("xe-0/0/0:0.0"));
    assertThat(c, hasInterface("xe-0/0/0:0.0", hasMtu(9000)));
  }

  @Test
  public void testInterfaceNativeVlan() throws IOException {
    String hostname = "interface-native-vlan";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasInterface("ge-0/1/0.0", hasNativeVlan(3)));
    assertThat(c, hasInterface("ge-0/2/0.0", hasNativeVlan(1)));
    assertThat(c, hasInterface("ge-0/3/0.0", hasNativeVlan(nullValue())));
  }

  @Test
  public void testInterfaceOspfPointToPoint() throws IOException {
    String hostname = "ospf-interface-point-to-point";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasInterface("ge-0/0/0.0", hasOspfPointToPoint(equalTo(true))));
  }

  @Test
  public void testInterfaceRange() throws IOException {
    String hostname = "interface-range";
    JuniperConfiguration juniperConfig = parseJuniperConfig(hostname);

    // range definitions are inserted properly into the vendor model
    InterfaceRange ae1 =
        juniperConfig.getMasterLogicalSystem().getInterfaceRanges().get("ae1-members");
    assertThat(ae1.getMtu(), equalTo(8000));
    assertThat(ae1.getDescription(), equalTo("dodo"));
    assertThat(
        ae1.getMembers(), equalTo(ImmutableList.of(new InterfaceRangeMember("xe-0/0/[0,1]"))));
    assertThat(
        ae1.getMemberRanges(),
        equalTo(ImmutableList.of(new InterfaceRangeMemberRange("xe-0/0/0", "xe-0/0/1"))));

    InterfaceRange ae2 =
        juniperConfig.getMasterLogicalSystem().getInterfaceRanges().get("ae2-members");
    assertThat(ae2.getDescription(), equalTo("dodo"));
    assertThat(ae2.getMembers(), equalTo(ImmutableList.of(new InterfaceRangeMember("xe-8/1/2"))));
    assertThat(ae2.get8023adInterface(), equalTo("ae1"));
    assertThat(ae2.getRedundantParentInterface(), equalTo("reth0"));

    // all interfaces should show up; no need to test their specific settings here
    Configuration c = parseConfig("interface-range");
    assertThat(
        c.getAllInterfaces().keySet(),
        equalTo(ImmutableSet.of("xe-0/0/0", "xe-0/0/1", "xe-8/1/2")));
  }

  @Test
  public void testInterfaceVlan() throws IOException {
    Configuration c = parseConfig("interface-vlan");

    // Expecting an Interface in ACCESS mode with VLAN 101
    assertThat(c, hasInterface("ge-0/0/0.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertThat(c, hasInterface("ge-0/0/0.0", hasAccessVlan(101)));

    // Expecting an Interface in TRUNK mode with VLANs 1-5
    assertThat(c, hasInterface("ge-0/3/0.0", hasSwitchPortMode(SwitchportMode.TRUNK)));
    assertThat(
        c, hasInterface("ge-0/3/0.0", hasAllowedVlans(IntegerSpace.of(new SubRange("1-5")))));
  }

  @Test
  public void testInterfaceVlanReferences() throws IOException {
    String hostname = "interface-vlan";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * We expect an undefined reference for VLAN_TEST_UNDEFINED
     */
    assertThat(ccae, hasUndefinedReference(filename, VLAN, "VLAN_TEST_UNDEFINED", INTERFACE_VLAN));

    /*
     * Named VLANs
     */
    assertThat(
        ccae.getDefinedStructures()
            .get(filename)
            .getOrDefault(VLAN.getDescription(), Collections.emptySortedMap()),
        allOf(hasKey("VLAN_TEST"), hasKey("VLAN_TEST_UNUSED")));
  }

  @Test
  public void testIrbInterfaces() throws IOException {
    String hostname = "irb-interfaces";
    Configuration c = parseConfig(hostname);

    // No parent 'irb' interface should be created
    assertThat(c.getAllInterfaces(), not(hasKey("irb")));

    // irb.0 should be created
    assertThat(c.getAllInterfaces(), hasKey("irb.0"));

    Interface irb0 = c.getAllInterfaces().get("irb.0");

    // irb.0 should not have bind dependency to "irb", since it is not a real parent interface
    assertThat(
        irb0.getDependencies(),
        not(hasItem(equalTo(new Interface.Dependency("irb", DependencyType.BIND)))));

    // verify interface type
    assertThat(irb0.getInterfaceType(), equalTo(InterfaceType.VLAN));

    // verify vlan assignment
    assertThat(irb0.getVlan(), equalTo(5));
  }

  @Test
  public void testIpProtocol() throws IOException {
    String hostname = "firewall-filter-ip-protocol";
    Configuration c = parseConfig(hostname);

    Flow tcpFlow = createFlow(IpProtocol.TCP, 0);
    Flow icmpFlow = createFlow(IpProtocol.ICMP, 0);

    // Tcp flow should be accepted by the filter and others should be rejected
    assertThat(c, hasIpAccessList("FILTER", accepts(tcpFlow, null, c)));
    assertThat(c, hasIpAccessList("FILTER", rejects(icmpFlow, null, c)));
  }

  @Test
  public void testSourceAddress() throws IOException {
    Configuration c = parseConfig("firewall-source-address");
    String filterNameV4 = "FILTER";
    String filterNameV6 = "FILTERv6";

    assertThat(c.getIpAccessLists().keySet(), containsInAnyOrder(filterNameV4, filterNameV6));

    IpAccessList fwSourceAddressAcl = c.getIpAccessLists().get(filterNameV4);
    assertThat(fwSourceAddressAcl.getLines(), hasSize(1));

    // should have the same acl as defined in the config
    assertThat(
        c,
        hasIpAccessList(
            filterNameV4,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        IpAccessListLine.builder()
                            .setAction(LineAction.PERMIT)
                            .setMatchCondition(
                                new MatchHeaderSpace(
                                    HeaderSpace.builder()
                                        .setSrcIps(
                                            AclIpSpace.union(
                                                new IpWildcard(
                                                        Ip.parse("1.0.3.0"),
                                                        Ip.parse("0.255.0.255"))
                                                    .toIpSpace(),
                                                new IpWildcard("2.3.4.5/24").toIpSpace()))
                                        .build()))
                            .setName("TERM")
                            .build())))));
  }

  @Test
  public void testIpsecPolicy() throws IOException {
    Configuration c = parseConfig("ipsec-policy");

    // tests for conversion to IPSec phase 2 policies
    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy1",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(
                    equalTo(ImmutableList.of("TRANSFORM-SET1", "TRANSFORM-SET2"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP14)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy2",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP15)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy3",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP16)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy4",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP19)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy5",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP20)))));
    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy6",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of())),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP5)))));
  }

  @Test
  public void testIpsecProposalSet() throws IOException {
    Configuration c = parseConfig("ipsec-proposal-set");

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "NOPFS_ESP_3DES_MD5",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.THREEDES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "NOPFS_ESP_3DES_SHA",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.THREEDES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "NOPFS_ESP_DES_MD5",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "NOPFS_ESP_DES_SHA",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "G2_ESP_3DES_SHA",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.THREEDES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "G2_ESP_AES128_SHA",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy1",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(
                    equalTo(ImmutableList.of("G2_ESP_3DES_SHA", "G2_ESP_AES128_SHA"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP2)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "policy2",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(
                    equalTo(
                        ImmutableList.of(
                            "NOPFS_ESP_3DES_SHA",
                            "NOPFS_ESP_3DES_MD5",
                            "NOPFS_ESP_DES_SHA",
                            "NOPFS_ESP_DES_MD5"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(nullValue()))));
  }

  @Test
  public void testIpsecProposalToIpsecPhase2Proposal() throws IOException {
    Configuration c = parseConfig("ipsec-proposal");
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop1",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(
                    ImmutableSortedSet.of(IpsecProtocol.ESP, IpsecProtocol.AH)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop2",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_192_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.AH)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop3",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.THREEDES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop4",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_GCM),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop5",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_192_GCM),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "prop6",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_256_GCM),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
  }

  @Test
  public void testToIpsecPeerConfig() throws IOException {
    Configuration c = parseConfig("ipsec-vpn");

    assertThat(
        c,
        hasIpsecPeerConfig(
            "ike-vpn-chicago",
            isIpsecStaticPeerConfigThat(
                allOf(
                    hasDestinationAddress(Ip.parse("198.51.100.102")),
                    IpsecPeerConfigMatchers.hasIkePhase1Policy("ike-phase1-policy"),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("ipsec-phase2-policy"),
                    IpsecPeerConfigMatchers.hasSourceInterface("ge-0/0/3.0"),
                    IpsecPeerConfigMatchers.hasLocalAddress(Ip.parse("198.51.100.2")),
                    IpsecPeerConfigMatchers.hasTunnelInterface(equalTo("st0.0"))))));
  }

  @Test
  public void testDestinationAddress() throws IOException {
    Configuration c = parseConfig("firewall-destination-address");
    String filterNameV4 = "FILTER";
    String filterNameV6 = "FILTERv6";

    assertThat(c.getIpAccessLists().keySet(), containsInAnyOrder(filterNameV4, filterNameV6));

    IpAccessList fwDestinationAddressAcl = c.getIpAccessLists().get(filterNameV4);
    assertThat(fwDestinationAddressAcl.getLines(), hasSize(1));

    // should have the same acl as defined in the config
    assertThat(
        c,
        hasIpAccessList(
            filterNameV4,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        IpAccessListLine.builder()
                            .setAction(LineAction.PERMIT)
                            .setMatchCondition(
                                new MatchHeaderSpace(
                                    HeaderSpace.builder()
                                        .setDstIps(
                                            AclIpSpace.union(
                                                new IpWildcard(
                                                        Ip.parse("1.0.3.0"),
                                                        Ip.parse("0.255.0.255"))
                                                    .toIpSpace(),
                                                new IpWildcard("2.3.4.5/24").toIpSpace()))
                                        .build()))
                            .setName("TERM")
                            .build())))));
  }

  @Test
  public void testSourceAddressBehavior() throws IOException {
    Configuration c = parseConfig("firewall-source-address");

    assertThat(c.getIpAccessLists().keySet(), hasSize(2));

    Flow whiteListedSrc = createFlow("1.8.3.9", "2.5.6.7");
    Flow blackListedSrc = createFlow("5.8.4.9", "2.5.6.7");

    IpAccessList incomingFilter = c.getAllInterfaces().get("xe-0/0/0.0").getIncomingFilter();

    // Whitelisted source address should be allowed
    assertThat(incomingFilter, accepts(whiteListedSrc, "xe-0/0/0.0", c));

    // Blacklisted source address should be denied
    assertThat(incomingFilter, rejects(blackListedSrc, "xe-0/0/0.0", c));
  }

  @Test
  public void testDestinationAddressBehavior() throws IOException {
    Configuration c = parseConfig("firewall-destination-address");

    assertThat(c.getIpAccessLists().keySet(), hasSize(2));

    Flow whiteListedDst = createFlow("2.5.6.7", "1.8.3.9");
    Flow blackListedDst = createFlow("2.5.6.7", "5.8.4.9");

    IpAccessList incomingFilter = c.getAllInterfaces().get("xe-0/0/0.0").getIncomingFilter();

    // Whitelisted source address should be allowed
    assertThat(incomingFilter, accepts(whiteListedDst, "xe-0/0/0.0", c));

    // Blacklisted source address should be denied
    assertThat(incomingFilter, rejects(blackListedDst, "xe-0/0/0.0", c));
  }

  @Test
  public void testJuniperApplyGroupsNode() throws IOException {
    String filename = "juniper-apply-groups-node";

    Batfish batfish = getBatfishForConfigurationNames(filename);
    Configuration c = batfish.loadConfigurations().entrySet().iterator().next().getValue();

    /* hostname should not be overwritten from node0 nor node1 group */
    assertThat(c, hasHostname(filename));
    /* other lines from node0 and node1 groups should be applied */
    assertThat(
        c, hasInterface("lo0.1", hasAllAddresses(contains(new InterfaceAddress("1.1.1.1/32")))));
    assertThat(
        c, hasInterface("lo0.2", hasAllAddresses(contains(new InterfaceAddress("2.2.2.2/32")))));
  }

  @Test
  public void testJuniperApplyGroupsNodeNoHostname() throws IOException {
    String filename = "juniper-apply-groups-node-no-hostname";

    Batfish batfish = getBatfishForConfigurationNames(filename);
    Configuration c = batfish.loadConfigurations().entrySet().iterator().next().getValue();

    /* hostname should be generated, and not gotten from node0 nor node1 group */
    assertThat(c, hasHostname(not(equalTo("juniper-apply-groups-node0"))));
    assertThat(c, hasHostname(not(equalTo("juniper-apply-groups-node1"))));
    /* other lines from node0 and node1 groups should be applied */
    assertThat(
        c, hasInterface("lo0.1", hasAllAddresses(contains(new InterfaceAddress("1.1.1.1/32")))));
    assertThat(
        c, hasInterface("lo0.2", hasAllAddresses(contains(new InterfaceAddress("2.2.2.2/32")))));
  }

  @Test
  public void testJuniperIsis() throws IOException {
    String hostname = "juniper-isis";
    String loopback = "lo0.0";
    String physical = "ge-0/0/0.0";

    Configuration c = parseConfig(hostname);

    double expectedReferenceBandwidth = 100E9;
    assertThat(c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasLevel1(nullValue()))));
    assertThat(
        c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasLevel2(hasWideMetricsOnly()))));
    assertThat(c, hasDefaultVrf(hasIsisProcess(hasOverload(false))));
    assertThat(
        c,
        hasDefaultVrf(
            hasIsisProcess(IsisProcessMatchers.hasReferenceBandwidth(expectedReferenceBandwidth))));

    assertThat(
        c,
        hasInterface(
            loopback, hasIsis(hasIsoAddress(new IsoAddress("12.1234.1234.1234.1234.00")))));
    assertThat(c, hasInterface(loopback, hasIsis(hasLevel1(nullValue()))));
    assertThat(c, hasInterface(loopback, hasIsis(hasLevel2(hasMode(IsisInterfaceMode.PASSIVE)))));

    // Loopback did not set an IS-IS metric, so its cost should be based on the reference bandwidth.
    // First confirm the expected cost isn't coincidentally equal to the Juniper default cost of 10.
    // No need to worry about getBandwidth() returning null for Juniper interfaces.
    long expectedCost =
        Math.max(
            (long) (expectedReferenceBandwidth / c.getAllInterfaces().get(loopback).getBandwidth()),
            1L);
    assertThat(expectedCost, not(equalTo(10L)));
    assertThat(
        c,
        hasInterface(
            loopback,
            hasIsis(hasLevel2(IsisInterfaceLevelSettingsMatchers.hasCost(expectedCost)))));

    assertThat(
        c,
        hasInterface(
            physical, hasIsis(hasIsoAddress(new IsoAddress("12.1234.1234.1234.1234.01")))));
    assertThat(c, hasInterface(physical, hasIsis(hasBfdLivenessDetectionMinimumInterval(250))));
    assertThat(c, hasInterface(physical, hasIsis(hasBfdLivenessDetectionMultiplier(3))));
    assertThat(c, hasInterface(physical, hasIsis(IsisInterfaceSettingsMatchers.hasPointToPoint())));
    assertThat(c, hasInterface(physical, hasIsis(hasLevel1(nullValue()))));
    assertThat(
        c,
        hasInterface(physical, hasIsis(hasLevel2(IsisInterfaceLevelSettingsMatchers.hasCost(5L)))));
    assertThat(c, hasInterface(physical, hasIsis(hasLevel2(hasMode(IsisInterfaceMode.ACTIVE)))));
    assertThat(
        c,
        hasInterface(
            physical,
            hasIsis(hasLevel2(hasHelloAuthenticationType(IsisHelloAuthenticationType.MD5)))));
    assertThat(
        c,
        hasInterface(
            physical, hasIsis(hasLevel2(IsisInterfaceLevelSettingsMatchers.hasHelloInterval(1)))));
    assertThat(c, hasInterface(physical, hasIsis(hasLevel2(hasHoldTime(3)))));

    // Assert non-ISIS interface has no ISIS, but has IP address
    assertThat(c, hasInterface("ge-1/0/0.0", hasIsis(nullValue())));
    assertThat(
        c,
        hasInterface(
            "ge-1/0/0.0",
            hasAllAddresses(contains(new InterfaceAddress(Ip.parse("10.1.1.1"), 24)))));
  }

  @Test
  public void testJuniperIsisNoIsoAddress() throws IOException {
    Configuration c = parseConfig("juniper-isis-no-iso");

    assertThat(c, hasDefaultVrf(hasIsisProcess(nullValue())));
  }

  @Test
  public void testJuniperIsisNonLoopbackIsoAddress() throws IOException {
    Configuration c = parseConfig("juniper-isis-iso-non-loopback");

    assertThat(
        c,
        hasDefaultVrf(
            hasIsisProcess(hasNetAddress(equalTo(new IsoAddress("12.1234.1234.1234.1234.01"))))));
  }

  @Test
  public void testJuniperIsisNoReferenceBandwidth() throws IOException {
    Configuration c = parseConfig("juniper-isis-no-reference-bandwidth");

    // With no set metric or reference bandwidth, Juniper IS-IS cost should default to 10
    assertThat(
        c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasReferenceBandwidth((Double) null))));
    assertThat(
        c,
        hasInterface("lo0.0", hasIsis(hasLevel2(IsisInterfaceLevelSettingsMatchers.hasCost(10L)))));
  }

  @Test
  public void testJuniperIsisOverload() throws IOException {
    Configuration c = parseConfig("juniper-isis-overload");
    assertThat(c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasOverload(true))));
  }

  @Test
  public void testJuniperIsisOverloadWithTimeout() throws IOException {
    Configuration c = parseConfig("juniper-isis-overload-with-timeout");
    assertThat(c, hasDefaultVrf(hasIsisProcess(IsisProcessMatchers.hasOverload(false))));
  }

  @Test
  public void testJuniperIsisPassiveLevel() throws IOException {
    Configuration c = parseConfig("juniper-isis-passive-level");
    assertThat(
        c,
        hasInterface(
            "ge-1/2/0.0",
            hasIsis(
                allOf(
                    hasLevel1(hasMode(IsisInterfaceMode.PASSIVE)),
                    hasLevel2(hasMode(IsisInterfaceMode.ACTIVE))))));
  }

  @Test
  public void testJuniperOspfStubSettings() throws IOException {
    Configuration c = parseConfig("juniper-ospf-stub-settings");

    // Check correct stub types are assigned
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(0L, hasStubType(StubType.NONE)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(1L, hasStubType(StubType.NSSA)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(2L, hasStubType(StubType.NSSA)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(3L, hasStubType(StubType.STUB)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(4L, hasStubType(StubType.STUB)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(5L, hasStubType(StubType.NONE)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(6L, hasStubType(StubType.STUB)))));

    // Check for stub subtype settings
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(
                    1L, hasNssa(hasDefaultOriginateType(OspfDefaultOriginateType.INTER_AREA))))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(1L, hasNssa(hasSuppressType3(false))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(2L, hasNssa(hasDefaultOriginateType(OspfDefaultOriginateType.NONE))))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(2L, hasNssa(hasSuppressType3())))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(3L, hasStub(StubSettingsMatchers.hasSuppressType3(false))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(4L, hasStub(StubSettingsMatchers.hasSuppressType3())))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(6L, hasStub(StubSettingsMatchers.hasSuppressType3())))));
  }

  @Test
  public void testJuniperPolicyStatementPrefixListDisjunction() throws IOException {
    // Configuration has policy statement with term that checks two prefix lists.
    Configuration c = parseConfig("juniper-from-prefix-list");

    // Accept if network matches either prefix list
    for (Prefix p : ImmutableList.of(Prefix.parse("1.1.1.0/24"), Prefix.parse("2.2.2.0/24"))) {
      Result result =
          c.getRoutingPolicies()
              .get("POLICY-NAME")
              .call(
                  Environment.builder(c)
                      .setVrf(DEFAULT_VRF_NAME)
                      .setOriginalRoute(new ConnectedRoute(p, "nextHop"))
                      .build());
      assertThat(result.getBooleanValue(), equalTo(true));
    }

    // Reject if network does not match protocol
    Result result =
        c.getRoutingPolicies()
            .get("POLICY-NAME")
            .call(
                Environment.builder(c)
                    .setVrf(DEFAULT_VRF_NAME)
                    .setOriginalRoute(
                        StaticRoute.builder()
                            .setAdministrativeCost(0)
                            .setNetwork(Prefix.parse("1.1.1.0/24"))
                            .build())
                    .build());
    assertThat(result.getBooleanValue(), equalTo(false));

    // Reject if network is missing from both prefix lists
    result =
        c.getRoutingPolicies()
            .get("POLICY-NAME")
            .call(
                Environment.builder(c)
                    .setVrf(DEFAULT_VRF_NAME)
                    .setOriginalRoute(new ConnectedRoute(Prefix.parse("3.3.3.0/24"), "nextHop"))
                    .build());
    assertThat(result.getBooleanValue(), equalTo(false));
  }

  @Test
  public void testJuniperPolicyStatementTermFromEvaluation() throws IOException {
    // Configuration has policy statements
    Configuration c = parseConfig("juniper-policy-statement-term");
    Prefix testPrefix = Prefix.parse("1.1.1.1/28");
    Result result;

    /*
    COMMUNITY_POLICY should accept routes with either set community, but no others
    BGP1 matches 1, BGP2 matches 2
      set policy-options policy-statement COMMUNITY_POLICY term T1 from community BGP1
      set policy-options policy-statement COMMUNITY_POLICY term T1 from community BGP2
    */
    RoutingPolicy communityPolicy = c.getRoutingPolicies().get("COMMUNITY_POLICY");
    BgpRoute.Builder brb =
        BgpRoute.builder()
            .setAdmin(100)
            .setNetwork(testPrefix)
            .setOriginatorIp(Ip.parse("2.2.2.2"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);
    result = communityPolicy.call(envWithRoute(c, brb.setCommunities(ImmutableSet.of(1L)).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result = communityPolicy.call(envWithRoute(c, brb.setCommunities(ImmutableSet.of(2L)).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result = communityPolicy.call(envWithRoute(c, brb.setCommunities(ImmutableSet.of(3L)).build()));
    assertThat(result.getBooleanValue(), equalTo(false));

    /*
    FAMILY_POLICY should accept only inet6 (each set overwrites previous)
      set policy-options policy-statement FAMILY_POLICY term T1 from family inet
      set policy-options policy-statement FAMILY_POLICY term T1 from family inet6
    */
    RoutingPolicy familyPolicy = c.getRoutingPolicies().get("FAMILY_POLICY");
    result = familyPolicy.call(envWithRoute(c, new ConnectedRoute(testPrefix, "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(false));
    result =
        familyPolicy.call(
            Environment.builder(c)
                .setVrf(DEFAULT_VRF_NAME)
                .setOriginalRoute6(new GeneratedRoute6(Prefix6.ZERO))
                .build());
    assertThat(result.getBooleanValue(), equalTo(true));

    /*
    INTERFACE_POLICY should accept routes from either set interface, but not from other networks
      set interfaces ge-0/0/1 unit 0 family inet address 10.0.0.1/30
      set interfaces ge-0/0/2 unit 0 family inet address 10.0.0.5/30
      set policy-options policy-statement INTERFACE_POLICY term T1 from interface ge-0/0/1.0
      set policy-options policy-statement INTERFACE_POLICY term T1 from interface ge-0/0/2.0
    */
    RoutingPolicy interfacePolicy = c.getRoutingPolicies().get("INTERFACE_POLICY");
    result =
        interfacePolicy.call(
            envWithRoute(c, new ConnectedRoute(Prefix.parse("10.0.0.1/30"), "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        interfacePolicy.call(
            envWithRoute(c, new ConnectedRoute(Prefix.parse("10.0.0.5/30"), "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        interfacePolicy.call(
            envWithRoute(c, new ConnectedRoute(Prefix.parse("10.0.0.9/30"), "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(false));

    /*
    NETWORK_POLICY should accept routes with networks matching any prefix list or route filter line.
    Prefix lists are all defined as PLX = [ X.X.X.0/24 ].
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list PL1
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list PL2
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list-filter PL3 longer
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list-filter PL4 longer
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list-filter PL5 orlonger
      set policy-options policy-statement NETWORK_POLICY term T1 from prefix-list-filter PL6 orlonger
      set policy-options policy-statement NETWORK_POLICY term T1 from route-filter 7.7.7.0/24 exact
      set policy-options policy-statement NETWORK_POLICY term T1 from route-filter 8.8.8.0/24 exact
    */
    RoutingPolicy networkPolicy = c.getRoutingPolicies().get("NETWORK_POLICY");
    StaticRoute.Builder srb = StaticRoute.builder().setAdministrativeCost(100);
    // prefix-list statements should match exact length
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("1.1.1.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("2.2.2.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("1.1.1.0/25")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    // prefix-list-filter longer statements should match any longer length, but not exact length
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("3.3.3.0/25")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("4.4.4.0/26")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("3.3.3.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    // prefix-list-filter orlonger statements should match /24 or longer
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("5.5.5.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("5.5.5.0/25")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("6.6.6.0/26")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    // route-filter statements should match exact length
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("7.7.7.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("8.8.8.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("7.7.7.0/25")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    // shorter prefix should not match for any
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("1.1.1.0/23")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("3.3.3.0/23")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("5.5.5.0/23")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("7.7.7.0/23")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));
    // random prefix should not match
    result =
        networkPolicy.call(envWithRoute(c, srb.setNetwork(Prefix.parse("9.9.9.0/24")).build()));
    assertThat(result.getBooleanValue(), equalTo(false));

    /*
    PROTOCOL_POLICY should accept routes with either set protocol, but no others
      set policy-options policy-statement PROTOCOL_POLICY term TERM1 from protocol direct
      set policy-options policy-statement PROTOCOL_POLICY term TERM1 from protocol static
    */
    RoutingPolicy protocolPolicy = c.getRoutingPolicies().get("PROTOCOL_POLICY");
    result = protocolPolicy.call(envWithRoute(c, new ConnectedRoute(testPrefix, "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(true));
    result = protocolPolicy.call(envWithRoute(c, srb.build()));
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        protocolPolicy.call(
            envWithRoute(
                c, new LocalRoute(new InterfaceAddress(Ip.parse("1.1.1.1"), 28), "nextHop")));
    assertThat(result.getBooleanValue(), equalTo(false));

    /*
    TAG_POLICY should accept routes with either set tag, but not from other tags
      set policy-options policy-statement TAG_POLICY term T1 from tag 1
      set policy-options policy-statement TAG_POLICY term T1 from tag 2
    */
    RoutingPolicy tagPolicy = c.getRoutingPolicies().get("TAG_POLICY");
    srb = StaticRoute.builder().setAdministrativeCost(100).setNetwork(testPrefix);
    result =
        tagPolicy.call(
            Environment.builder(c).setVrf(DEFAULT_VRF_NAME).setOutputRoute(srb.setTag(1)).build());
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        tagPolicy.call(
            Environment.builder(c).setVrf(DEFAULT_VRF_NAME).setOutputRoute(srb.setTag(2)).build());
    assertThat(result.getBooleanValue(), equalTo(true));
    result =
        tagPolicy.call(
            Environment.builder(c).setVrf(DEFAULT_VRF_NAME).setOutputRoute(srb.setTag(3)).build());
    assertThat(result.getBooleanValue(), equalTo(false));
  }

  private static Environment envWithRoute(Configuration c, AbstractRoute route) {
    return Environment.builder(c).setVrf(DEFAULT_VRF_NAME).setOriginalRoute(route).build();
  }

  @Test
  public void testJuniperWildcards() throws IOException {
    String hostname = "juniper-wildcards";
    String loopback = "lo0.0";
    String prefix1 = "1.1.1.1/32";
    String prefix2 = "3.3.3.3/32";
    String prefixList1 = "p1";
    String prefixList2 = "p2";
    Prefix neighborPrefix = Prefix.parse("2.2.2.2/32");

    Configuration c = parseConfig(hostname);

    /* apply-groups using group containing interface wildcard should function as expected. */
    assertThat(c, hasInterface(loopback, hasAllAddresses(contains(new InterfaceAddress(prefix1)))));

    /* The wildcard copied out of groups should disappear and not be treated as an actual interface */
    assertThat(c, hasInterfaces(not(hasKey("*.*"))));

    /* The wildcard-looking interface description should not be pruned since its parse-tree node was not created via preprocessor. */
    assertThat(c, hasInterface(loopback, hasDescription("<SCRUBBED>")));

    /* apply-path should work with wildcard. Its line should not be pruned since its parse-tree node was not created via preprocessor. */
    assertThat(c, hasRouteFilterList(prefixList1, permits(Prefix.parse(prefix1))));

    /* prefix-list p2 should get content from g2, but no prefix-list named "<*>" should be created */
    assertThat(c, hasRouteFilterList(prefixList2, permits(Prefix.parse(prefix2))));
    assertThat(c, hasRouteFilterLists(not(hasKey("<*>"))));

    /* The wildcard-looking BGP group name should not be pruned since its parse-tree node was not created via preprocessor. */
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasNeighbors(hasKey(neighborPrefix)))));
  }

  @Test
  public void testJuniperWildcardsReference() throws IOException {
    String hostname = "juniper-wildcards";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm definitions are tracked properly for structures defined by apply-groups/apply-path
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, INTERFACE, "lo0", containsInAnyOrder(4, 8)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, PREFIX_LIST, "p1", containsInAnyOrder(4, 9)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(filename, PREFIX_LIST, "p2", containsInAnyOrder(5)));

    // Confirm undefined references are also tracked properly for apply-groups related references
    assertThat(
        ccae,
        hasUndefinedReferenceWithReferenceLines(
            filename, INTERFACE, "et-0/0/0.0", OSPF_AREA_INTERFACE, containsInAnyOrder(6, 14)));
  }

  @Test
  public void testLogicalSystems() throws IOException {
    String snapshotName = "logical-systems";
    String configName = "master1";

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + snapshotName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    // There should be 3 configs: the master, and one for each logical system
    // ls1's name should be derived from master hostname and logical-system name
    // ls2's name was manually configured
    assertThat(
        configurations.keySet(),
        containsInAnyOrder(
            configName,
            JuniperConfiguration.computeLogicalSystemDefaultHostname(configName, "ls1"),
            "ls2.example.com"));
  }

  @Test
  public void testLogicalSystemsInterfaces() throws IOException {
    String snapshotName = "logical-systems";
    String configName = "master1";
    String lsConfigName =
        JuniperConfiguration.computeLogicalSystemDefaultHostname(configName, "ls1");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + snapshotName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration masterConfig = configurations.get(configName);
    Configuration lsConfig = configurations.get(lsConfigName);

    // ensure interfaces have been divided appropriately
    assertThat(
        masterConfig.getAllInterfaces().keySet(),
        containsInAnyOrder("xe-0/0/0", "xe-0/0/0.0", "xe-0/0/1", "xe-0/0/1.1"));
    assertThat(
        lsConfig.getAllInterfaces().keySet(),
        containsInAnyOrder("xe-0/0/1", "xe-0/0/1.0", "xe-0/0/2", "xe-0/0/2.0"));

    // shared physical interface should have same settings on both configs
    assertThat(masterConfig.getAllInterfaces().get("xe-0/0/1.1"), hasMtu(2345));
    assertThat(lsConfig.getAllInterfaces().get("xe-0/0/1.0"), hasMtu(2345));
  }

  @Test
  public void testLogicalSystemsFirewallFilters() throws IOException {
    String snapshotName = "logical-systems";
    String configName = "master1";
    String lsConfigName =
        JuniperConfiguration.computeLogicalSystemDefaultHostname(configName, "ls1");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + snapshotName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration masterConfig = configurations.get(configName);
    Configuration lsConfig = configurations.get(lsConfigName);

    // ff1 is defined only on master, but should be accessible to both
    assertThat(masterConfig.getIpAccessLists(), hasKey("ff1"));
    assertThat(lsConfig.getIpAccessLists(), hasKey("ff1"));
    assertThat(
        masterConfig.getIpAccessLists().get("ff1"),
        equalTo(lsConfig.getIpAccessLists().get("ff1")));

    // ff2 is defined only on both, but should have different definitions on each
    assertThat(masterConfig.getIpAccessLists(), hasKey("ff2"));
    assertThat(lsConfig.getIpAccessLists(), hasKey("ff2"));
    assertThat(
        masterConfig.getIpAccessLists().get("ff2"),
        not(equalTo(lsConfig.getIpAccessLists().get("ff2"))));

    // ff3 is defined only on logical system, so should be out of scope for master
    assertThat(masterConfig.getIpAccessLists(), not(hasKey("ff3")));
    assertThat(lsConfig.getIpAccessLists(), hasKey("ff3"));
  }

  @Test
  public void testLogicalSystemsPolicyStatements() throws IOException {
    String snapshotName = "logical-systems";
    String configName = "master1";
    String lsConfigName =
        JuniperConfiguration.computeLogicalSystemDefaultHostname(configName, "ls1");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + snapshotName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration masterConfig = configurations.get(configName);
    Configuration lsConfig = configurations.get(lsConfigName);

    // ps1 is defined only on master, but should be accessible to both
    assertThat(masterConfig.getRoutingPolicies(), hasKey("ps1"));
    assertThat(lsConfig.getRoutingPolicies(), hasKey("ps1"));
    assertThat(
        masterConfig.getRoutingPolicies().get("ps1"),
        equalTo(lsConfig.getRoutingPolicies().get("ps1")));

    // ps2 is defined only on both, but should have dipserent definitions on each
    assertThat(masterConfig.getRoutingPolicies(), hasKey("ps2"));
    assertThat(lsConfig.getRoutingPolicies(), hasKey("ps2"));
    assertThat(
        masterConfig.getRoutingPolicies().get("ps2"),
        not(equalTo(lsConfig.getRoutingPolicies().get("ps2"))));

    // ps3 is defined only on logical system, so should be out of scope for master
    assertThat(masterConfig.getRoutingPolicies(), not(hasKey("ps3")));
    assertThat(lsConfig.getRoutingPolicies(), hasKey("ps3"));
  }

  @Test
  public void testLocalRouteExportBgp() throws IOException {
    Configuration c = parseConfig("local-route-export-bgp");
    Environment.Builder eb = Environment.builder(c).setDirection(Direction.OUT);

    String peer1Vrf = "peer1Vrf";
    RoutingPolicy peer1RejectAllLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("1.0.0.1/32")));

    String peer2Vrf = "peer2Vrf";
    RoutingPolicy peer2RejectPtpLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("2.0.0.1/32")));

    String peer3Vrf = "peer3Vrf";
    RoutingPolicy peer3RejectLanLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("3.0.0.1/32")));

    String peer4Vrf = "peer3Vrf";
    RoutingPolicy peer4AllowAllLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("4.0.0.1/32")));

    LocalRoute localRoutePtp = new LocalRoute(new InterfaceAddress("10.0.0.0/31"), "ge-0/0/0.0");
    LocalRoute localRouteLan = new LocalRoute(new InterfaceAddress("10.0.1.0/30"), "ge-0/0/1.0");

    // Peer policies should reject local routes not exported by their VRFs
    eb.setVrf(peer1Vrf);
    assertThat(
        peer1RejectAllLocal
            .call(eb.setOriginalRoute(localRoutePtp).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        peer1RejectAllLocal
            .call(eb.setOriginalRoute(localRouteLan).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(false));

    eb.setVrf(peer2Vrf);
    assertThat(
        peer2RejectPtpLocal
            .call(eb.setOriginalRoute(localRoutePtp).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        peer2RejectPtpLocal
            .call(eb.setOriginalRoute(localRouteLan).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(true));

    eb.setVrf(peer3Vrf);
    assertThat(
        peer3RejectLanLocal
            .call(eb.setOriginalRoute(localRoutePtp).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        peer3RejectLanLocal
            .call(eb.setOriginalRoute(localRouteLan).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(false));

    eb.setVrf(peer4Vrf);
    assertThat(
        peer4AllowAllLocal
            .call(eb.setOriginalRoute(localRoutePtp).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        peer4AllowAllLocal
            .call(eb.setOriginalRoute(localRouteLan).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(true));
  }

  @Test
  public void testLocalRouteExportOspf() throws IOException {
    Configuration c = parseConfig("local-route-export-ospf");
    Environment.Builder eb = Environment.builder(c).setDirection(Direction.OUT);

    String vrf1 = "vrf1";
    RoutingPolicy vrf1RejectAllLocal =
        c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf1));

    String vrf2 = "vrf2";
    RoutingPolicy vrf2RejectPtpLocal =
        c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf2));

    String vrf3 = "vrf3";
    RoutingPolicy vrf3RejectLanLocal =
        c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf3));

    String vrf4 = "vrf4";
    RoutingPolicy vrf4AllowAllLocal = c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf4));

    LocalRoute localRoutePtp = new LocalRoute(new InterfaceAddress("10.0.0.0/31"), "ge-0/0/0.0");
    LocalRoute localRouteLan = new LocalRoute(new InterfaceAddress("10.0.1.0/30"), "ge-0/0/1.0");

    // Peer policies should reject local routes not exported by their VRFs
    eb.setVrf(vrf1);
    assertThat(
        vrf1RejectAllLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        vrf1RejectAllLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));

    eb.setVrf(vrf2);
    assertThat(
        vrf2RejectPtpLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        vrf2RejectPtpLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));

    eb.setVrf(vrf3);
    assertThat(
        vrf3RejectLanLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        vrf3RejectLanLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));

    eb.setVrf(vrf4);
    assertThat(
        vrf4AllowAllLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        vrf4AllowAllLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(OspfExternalType2Route.builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));
  }

  @Test
  public void testNatDest() throws IOException {
    Configuration config = parseConfig("nat-dest");

    NavigableMap<String, Interface> interfaces = config.getAllInterfaces();
    assertThat(interfaces.keySet(), containsInAnyOrder("ge-0/0/0", "ge-0/0/0.0"));

    assertThat(interfaces.get("ge-0/0/0").getIncomingTransformation(), nullValue());

    Interface iface = interfaces.get("ge-0/0/0.0");

    Ip pool1Start = Prefix.parse("10.10.10.10/24").getFirstHostIp();
    Ip pool1End = Prefix.parse("10.10.10.10/24").getLastHostIp();

    Ip pool2Start = Ip.parse("10.10.10.10");
    Ip pool2End = Ip.parse("10.10.10.20");

    Transformation ruleSetRIRule1Transformation =
        when(match(HeaderSpace.builder().setSrcPorts(ImmutableList.of(new SubRange(5, 5))).build()))
            .apply(NOOP_DEST_NAT)
            .build();

    Transformation ruleSetZoneRule3Transformation =
        when(match(
                HeaderSpace.builder()
                    .setSrcIps(Prefix.parse("3.3.3.3/24").toIpSpace())
                    .setDstIps(Prefix.parse("1.1.1.1/32").toIpSpace())
                    .build()))
            .apply(assignDestinationIp(pool1Start, pool1End))
            .setOrElse(ruleSetRIRule1Transformation)
            .build();

    Transformation ruleSetZoneRule2Transformation =
        when(match(
                HeaderSpace.builder()
                    .setDstIps(new IpSpaceReference("global~DA-NAME"))
                    .setSrcIps(Prefix.parse("2.2.2.2/24").toIpSpace())
                    .build()))
            .apply(assignDestinationIp(pool2Start, pool2End))
            .setOrElse(ruleSetZoneRule3Transformation)
            .build();

    Transformation ruleSetZoneRule1Transformation =
        when(match(
                HeaderSpace.builder()
                    .setDstIps(new IpSpaceReference("global~NAME"))
                    .setDstPorts(ImmutableList.of(new SubRange(100, 200)))
                    .setSrcPorts(ImmutableList.of(new SubRange(80, 80)))
                    .setSrcIps(new IpSpaceReference("global~SA-NAME"))
                    .build()))
            .apply(NOOP_DEST_NAT)
            .setOrElse(ruleSetZoneRule2Transformation)
            .build();

    Transformation ruleSetIfaceRule3Transformation =
        when(match(HeaderSpace.builder().setSrcPorts(ImmutableList.of(new SubRange(6, 6))).build()))
            .apply(NOOP_DEST_NAT)
            .setOrElse(ruleSetZoneRule1Transformation)
            .build();

    assertThat(iface.getIncomingTransformation(), equalTo(ruleSetIfaceRule3Transformation));
  }

  @Test
  public void testNatDestJuniperConfig() {
    JuniperConfiguration config = parseJuniperConfig("nat-dest");

    Nat nat = config.getMasterLogicalSystem().getNatDestination();
    assertThat(nat.getType(), equalTo(Type.DESTINATION));

    // test pools
    Map<String, NatPool> pools = nat.getPools();
    assertThat(pools.keySet(), equalTo(ImmutableSet.of("POOL1", "POOL2")));

    NatPool pool1 = pools.get("POOL1");
    assertThat(pool1.getFromAddress(), equalTo(Ip.parse("10.10.10.1")));
    assertThat(pool1.getToAddress(), equalTo(Ip.parse("10.10.10.254")));

    NatPool pool2 = pools.get("POOL2");
    assertThat(pool2.getFromAddress(), equalTo(Ip.parse("10.10.10.10")));
    assertThat(pool2.getToAddress(), equalTo(Ip.parse("10.10.10.20")));

    // test rule sets
    Map<String, NatRuleSet> ruleSets = nat.getRuleSets();
    assertThat(
        ruleSets.keySet(), containsInAnyOrder("RULE-SET-RI", "RULE-SET-ZONE", "RULE-SET-IFACE"));

    // test fromLocations
    NatPacketLocation fromLocation = ruleSets.get("RULE-SET-IFACE").getFromLocation();
    assertThat(fromLocation.getInterface(), equalTo("ge-0/0/0.0"));
    assertThat(fromLocation.getRoutingInstance(), nullValue());
    assertThat(fromLocation.getZone(), nullValue());

    fromLocation = ruleSets.get("RULE-SET-RI").getFromLocation();
    assertThat(fromLocation.getInterface(), nullValue());
    assertThat(fromLocation.getRoutingInstance(), equalTo("RI"));
    assertThat(fromLocation.getZone(), nullValue());

    fromLocation = ruleSets.get("RULE-SET-ZONE").getFromLocation();
    assertThat(fromLocation.getInterface(), nullValue());
    assertThat(fromLocation.getRoutingInstance(), nullValue());
    assertThat(fromLocation.getZone(), equalTo("ZONE"));

    // test RULE-SET-ZONE rules
    List<NatRule> rules = ruleSets.get("RULE-SET-ZONE").getRules();
    assertThat(rules, hasSize(3));

    // test rule1
    NatRule rule1 = rules.get(0);
    assertThat(rule1.getName(), equalTo("RULE1"));
    assertThat(
        rule1.getMatches(),
        contains(
            new NatRuleMatchSrcPort(80, 80),
            new NatRuleMatchDstPort(100, 200),
            new NatRuleMatchDstAddrName("NAME"),
            new NatRuleMatchSrcAddrName("SA-NAME")));
    assertThat(rule1.getThen(), equalTo(NatRuleThenOff.INSTANCE));

    // test rule2
    NatRule rule2 = rules.get(1);
    assertThat(rule2.getName(), equalTo("RULE2"));
    assertThat(
        rule2.getMatches(),
        contains(
            new NatRuleMatchSrcAddr(Prefix.parse("2.2.2.2/24")),
            new NatRuleMatchDstAddrName("DA-NAME")));
    assertThat(rule2.getThen(), equalTo(new NatRuleThenPool("POOL2")));

    // test rule3
    NatRule rule3 = rules.get(2);
    assertThat(rule3.getName(), equalTo("RULE3"));
    assertThat(
        rule3.getMatches(),
        contains(
            new NatRuleMatchSrcAddr(Prefix.parse("3.3.3.3/24")),
            new NatRuleMatchDstAddr(Prefix.parse("1.1.1.1/32"))));
    assertThat(rule3.getThen(), equalTo(new NatRuleThenPool("POOL1")));
  }

  @Test
  public void testNatSource() throws IOException {
    Configuration config = parseConfig("nat-source2");
    Map<String, Interface> interfaceMap = config.getAllInterfaces();

    assertThat(
        interfaceMap.keySet(),
        containsInAnyOrder("ge-0/0/0.0", "ge-0/0/1.0", "ge-0/0/0", "ge-0/0/1"));

    Interface iface0 = interfaceMap.get("ge-0/0/0.0");
    Interface iface1 = interfaceMap.get("ge-0/0/1.0");

    assertThat(iface0.getOutgoingTransformation(), nullValue());

    // rule set 1 has a routing instance from location, so it goes last
    AssignIpAddressFromPool transformationStep =
        assignSourceIp(Ip.parse("10.10.10.1"), Ip.parse("10.10.10.254"));

    AssignPortFromPool portTransformationStep =
        assignSourcePort(Nat.DEFAULT_FROM_PORT, Nat.DEFAULT_TO_PORT);

    Transformation ruleSet1Transformation =
        when(matchSrcInterface("ge-0/0/0.0"))
            .setAndThen(
                when(matchDst(Prefix.parse("1.1.1.1/24")))
                    .apply(transformationStep, portTransformationStep)
                    .build())
            .build();

    // rule set 3 has a zone from location, so it goes second
    Transformation ruleSet3Transformation =
        when(matchSrcInterface("ge-0/0/0.0", "ge-0/0/1.0"))
            .setAndThen(
                when(matchDst(Prefix.parse("3.3.3.3/24")))
                    .apply(transformationStep, portTransformationStep)
                    .setOrElse(ruleSet1Transformation)
                    .build())
            .setOrElse(ruleSet1Transformation)
            .build();

    // rule set 2 has an interface from location, so it goes first
    Transformation ruleSet2Transformation =
        when(matchSrcInterface("ge-0/0/0.0"))
            .setAndThen(
                when(matchDst(Prefix.parse("2.2.2.2/24")))
                    .apply(transformationStep, portTransformationStep)
                    .setOrElse(
                        // routing instance rule set
                        ruleSet3Transformation)
                    .build())
            .setOrElse(ruleSet3Transformation)
            .build();

    assertThat(iface1.getOutgoingTransformation(), equalTo(ruleSet2Transformation));
  }

  @Test
  public void testNatSourceJuniperConfig() {
    JuniperConfiguration config = parseJuniperConfig("nat-source");

    Nat nat = config.getMasterLogicalSystem().getNatSource();
    assertThat(nat.getType(), equalTo(Type.SOURCE));

    // test pools
    Map<String, NatPool> pools = nat.getPools();
    assertThat(pools.keySet(), containsInAnyOrder("POOL1", "POOL2"));

    NatPool pool1 = pools.get("POOL1");
    Ip ip1 = Ip.parse("10.10.10.1");
    Ip ip2 = Ip.parse("10.10.10.254");
    assertThat(pool1.getFromAddress(), equalTo(ip1));
    assertThat(pool1.getToAddress(), equalTo(ip2));

    NatPool pool2 = pools.get("POOL2");
    assertThat(pool2.getFromAddress(), equalTo(Ip.parse("10.10.10.10")));
    assertThat(pool2.getToAddress(), equalTo(Ip.parse("10.10.10.20")));

    // test rule sets
    Map<String, NatRuleSet> ruleSets = nat.getRuleSets();
    assertThat(ruleSets.keySet(), contains("RULE-SET"));

    NatRuleSet ruleSet = ruleSets.get("RULE-SET");

    /*
     * test from location lines -- it doesn't make sense to have more than one of these, but the
     * extraction supports it.
     */
    assertThat(ruleSet.getFromLocation().getInterface(), nullValue());
    assertThat(ruleSet.getFromLocation().getRoutingInstance(), nullValue());
    assertThat(ruleSet.getFromLocation().getZone(), equalTo("FROM-ZONE"));

    // test to location lines
    assertThat(ruleSet.getToLocation().getInterface(), nullValue());
    assertThat(ruleSet.getToLocation().getRoutingInstance(), nullValue());
    assertThat(ruleSet.getToLocation().getZone(), equalTo("TO-ZONE"));

    // test rules
    List<NatRule> rules = ruleSet.getRules();
    assertThat(rules, hasSize(3));

    // test rule1
    NatRule rule1 = rules.get(0);
    assertThat(rule1.getName(), equalTo("RULE1"));
    assertThat(
        rule1.getMatches(),
        equalTo(
            ImmutableList.of(
                new NatRuleMatchDstAddr(Prefix.parse("1.1.1.1/24")),
                new NatRuleMatchDstAddrName("NAME"))));
    assertThat(rule1.getThen(), equalTo(NatRuleThenOff.INSTANCE));

    // test rule2
    NatRule rule2 = rules.get(1);
    assertThat(rule2.getName(), equalTo("RULE2"));
    assertThat(
        rule2.getMatches(),
        equalTo(
            ImmutableList.of(
                new NatRuleMatchSrcAddr(Prefix.parse("2.2.2.2/24")),
                new NatRuleMatchSrcAddrName("SA-NAME"))));
    assertThat(rule2.getThen(), equalTo(new NatRuleThenPool("POOL")));

    // test rule3
    NatRule rule3 = rules.get(2);
    assertThat(rule3.getName(), equalTo("RULE3"));
    assertThat(
        rule3.getMatches(),
        equalTo(ImmutableList.of(new NatRuleMatchSrcAddr(Prefix.parse("3.3.3.0/24")))));
    assertThat(rule3.getThen(), equalTo(NatRuleThenInterface.INSTANCE));
  }

  @Test
  public void testNatSourceJuniperConfig2() {
    JuniperConfiguration config = parseJuniperConfig("nat-source2");

    Nat nat = config.getMasterLogicalSystem().getNatSource();
    assertThat(nat.getType(), equalTo(Type.SOURCE));

    Map<String, NatPool> pools = nat.getPools();
    assertThat(pools.keySet(), contains("POOL1"));

    NatPool pool1 = pools.get("POOL1");
    Ip ip1 = Ip.parse("10.10.10.1");
    Ip ip2 = Ip.parse("10.10.10.254");
    assertThat(pool1.getFromAddress(), equalTo(ip1));
    assertThat(pool1.getToAddress(), equalTo(ip2));

    // test rule sets
    Map<String, NatRuleSet> ruleSets = nat.getRuleSets();
    assertThat(ruleSets.keySet(), contains("RULE-SET1", "RULE-SET2", "RULE-SET3"));

    NatRuleSet ruleSet = ruleSets.get("RULE-SET1");
    assertThat(ruleSet.getFromLocation().getRoutingInstance(), equalTo("RI"));
    assertThat(ruleSet.getToLocation().getInterface(), equalTo("ge-0/0/1.0"));

    NatRuleSet ruleSet2 = ruleSets.get("RULE-SET2");
    assertThat(ruleSet2.getFromLocation().getInterface(), equalTo("ge-0/0/0.0"));
    assertThat(ruleSet2.getToLocation().getInterface(), equalTo("ge-0/0/1.0"));

    // test rules
    List<NatRule> rules = ruleSet.getRules();
    assertThat(rules, hasSize(1));

    NatRule rule1 = rules.get(0);
    assertThat(rule1.getName(), equalTo("RULE1"));
    assertThat(
        rule1.getMatches(),
        equalTo(ImmutableList.of(new NatRuleMatchDstAddr(Prefix.parse("1.1.1.1/24")))));
    assertThat(rule1.getThen(), equalTo(new NatRuleThenPool("POOL1")));

    rules = ruleSet2.getRules();
    assertThat(rules, hasSize(1));

    NatRule rule2 = rules.get(0);
    assertThat(rule2.getName(), equalTo("RULE1"));
    assertThat(
        rule2.getMatches(),
        equalTo(ImmutableList.of(new NatRuleMatchDstAddr(Prefix.parse("2.2.2.2/24")))));
    assertThat(rule2.getThen(), equalTo(new NatRuleThenPool("POOL1")));
  }

  @Test
  public void testOspfInterfaceAreaAssignment() throws IOException {
    Configuration c = parseConfig("ospfInterfaceAreaAssignment");

    /* Properly configured interfaces should be present in respective areas. */
    assertThat(c, hasInterface("xe-0/0/0.0", hasOspfAreaName(0L)));
    assertThat(c, hasInterface("xe-0/0/0.0", isOspfPassive(equalTo(false))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(0L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.0"))))));

    assertThat(c, hasInterface("xe-0/0/0.1", hasOspfAreaName(1L)));
    assertThat(c, hasInterface("xe-0/0/0.1", isOspfPassive()));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(1L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.1"))))));

    /* The following interfaces should be absent since they have no IP addresses assigned. */
    assertThat(c, hasInterface("xe-0/0/0.2", hasOspfAreaName(nullValue())));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(0L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.2")))))));

    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(0L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.3")))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(1L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.3")))))));
  }

  @Test
  public void testOspfRouterId() throws IOException {
    Configuration c = parseConfig("ospf-router-id");

    assertThat(c, hasVrf("default", hasOspfProcess(hasRouterId(equalTo(Ip.parse("1.0.0.0"))))));
  }

  @Test
  public void testOspfSummaries() throws IOException {
    Configuration c = parseConfig(("ospf-abr-with-summaries"));

    assertThat(
        c,
        hasDefaultVrf(
            hasGeneratedRoutes(
                hasItem(allOf(hasPrefix(Prefix.parse("10.0.1.0/24")), isDiscard())))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(
                    1L,
                    allOf(
                        hasStubType(equalTo(StubType.STUB)),
                        hasSummary(Prefix.parse("10.0.1.0/24"), isAdvertised()))))));
    String summaryFilterName =
        c.getDefaultVrf().getOspfProcess().getAreas().get(1L).getSummaryFilter();
    assertThat(summaryFilterName, not(nullValue()));
    assertThat(c.getRouteFilterLists().get(summaryFilterName), not(nullValue()));
    Prefix blockPrefix = Prefix.parse("10.0.1.0/24");
    assertThat(
        c.getRouteFilterLists().get(summaryFilterName).getLines(),
        equalTo(
            ImmutableList.of(
                new RouteFilterLine(
                    LineAction.DENY,
                    blockPrefix,
                    new SubRange(blockPrefix.getPrefixLength() + 1, Prefix.MAX_PREFIX_LENGTH)),
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.ZERO, new SubRange(0, Prefix.MAX_PREFIX_LENGTH)))));
  }

  @Test
  public void testParsingRecovery() {
    String recoveryText =
        CommonUtil.readResource("org/batfish/grammar/juniper/testconfigs/recovery");
    Settings settings = new Settings();
    FlatJuniperCombinedParser cp = new FlatJuniperCombinedParser(recoveryText, settings);
    Flat_juniper_configurationContext ctx = cp.parse();
    FlatJuniperRecoveryExtractor extractor = new FlatJuniperRecoveryExtractor();
    ParseTreeWalker walker = new BatfishParseTreeWalker(cp);
    walker.walk(extractor, ctx);

    assertThat(extractor.getNumSets(), equalTo(9));
    assertThat(extractor.getNumErrorNodes(), equalTo(7));
  }

  @Test
  public void testPredefinedJunosApplications() throws IOException {
    Batfish batfish = getBatfishForConfigurationNames("pre-defined-junos-applications");
    InitInfoAnswerElement answer = batfish.initInfo(false, true);
    assertThat(
        answer.prettyPrint(),
        not(Matchers.containsString("unimplemented pre-defined junos application")));
  }

  @Test
  public void testPredefinedJunosApplicationSets() throws IOException {
    Batfish batfish = getBatfishForConfigurationNames("pre-defined-junos-application-sets");
    InitInfoAnswerElement answer = batfish.initInfo(false, true);
    assertThat(
        answer.prettyPrint(),
        not(Matchers.containsString("unimplemented pre-defined junos application-set")));
  }

  @Test
  public void testPrefixList() throws IOException {
    String hostname = "prefix-lists";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    Flow flowAccepted1 = createFlow("1.2.3.4", "0.0.0.0");
    Flow flowAccepted2 = createFlow("1.2.3.5", "0.0.0.0");
    Flow flowDenied = createFlow("9.8.7.6", "0.0.0.0");

    IpAccessList filterPrefixList = c.getIpAccessLists().get("FILTER_PL");

    // Confirm referrers are tracked properly
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "PL", 5));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "PL_UNUSED", 0));

    // Confirm undefined reference is detected
    assertThat(ccae, hasUndefinedReference(filename, PREFIX_LIST, "PL_UNDEF"));

    // Only flow from accepted source-prefixes should be accepted
    assertThat(filterPrefixList, rejects(flowDenied, null, c));
    assertThat(filterPrefixList, accepts(flowAccepted1, null, c));
    assertThat(filterPrefixList, accepts(flowAccepted2, null, c));
  }

  @Test
  public void testPrefixListEmpty() throws IOException {
    Configuration c = parseConfig("prefix-list-empty");
    Flow testFlow1 = createFlow("9.8.7.6", "0.0.0.0");
    Flow testFlow2 = createFlow("1.2.3.4", "1.2.3.4");
    Flow testFlow3 = createFlow("0.0.0.0", "9.8.7.6");

    IpAccessList incomingFilterSource = c.getIpAccessLists().get("TEST_FILTER_SOURCE");
    IpAccessList incomingFilterSourceExcept = c.getIpAccessLists().get("TEST_FILTER_SOURCE_EXCEPT");

    IpAccessList incomingFilterDestination = c.getIpAccessLists().get("TEST_FILTER_DESTINATION");
    IpAccessList incomingFilterDestinationExcept =
        c.getIpAccessLists().get("TEST_FILTER_DESTINATION_EXCEPT");

    IpAccessList incomingFilter = c.getIpAccessLists().get("TEST_FILTER");

    // No source IP should match the empty prefix list
    assertThat(incomingFilterSource, rejects(testFlow1, null, c));
    assertThat(incomingFilterSource, rejects(testFlow2, null, c));
    assertThat(incomingFilterSource, rejects(testFlow3, null, c));

    // Every source IP should match the empty prefix list
    assertThat(incomingFilterSourceExcept, accepts(testFlow1, null, c));
    assertThat(incomingFilterSourceExcept, accepts(testFlow2, null, c));
    assertThat(incomingFilterSourceExcept, accepts(testFlow3, null, c));

    // No destination IP should match the empty prefix list
    assertThat(incomingFilterDestination, rejects(testFlow1, null, c));
    assertThat(incomingFilterDestination, rejects(testFlow2, null, c));
    assertThat(incomingFilterDestination, rejects(testFlow3, null, c));

    // Every destination IP should match the empty prefix list
    assertThat(incomingFilterDestinationExcept, accepts(testFlow1, null, c));
    assertThat(incomingFilterDestinationExcept, accepts(testFlow2, null, c));
    assertThat(incomingFilterDestinationExcept, accepts(testFlow3, null, c));

    // No dest or source IP should match the empty prefix list
    assertThat(incomingFilter, rejects(testFlow1, null, c));
    assertThat(incomingFilter, rejects(testFlow2, null, c));
    assertThat(incomingFilter, rejects(testFlow3, null, c));
  }

  @Test
  public void testRouteFilters() throws IOException {
    Configuration c = parseConfig("route-filter");
    RouteFilterList rfl = c.getRouteFilterLists().get("route-filter-test:t1");
    assertThat(
        rfl,
        RouteFilterListMatchers.hasLines(
            containsInAnyOrder(
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.parse("1.2.0.0/16"), new SubRange(16, 16)),
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.parse("1.2.0.0/16"), new SubRange(17, 32)),
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.parse("1.7.0.0/16"), new SubRange(16, 16)),
                new RouteFilterLine(
                    LineAction.PERMIT, Prefix.parse("1.7.0.0/17"), new SubRange(17, 17)),
                new RouteFilterLine(
                    LineAction.PERMIT,
                    new IpWildcard("1.0.0.0:0.255.0.255"),
                    new SubRange(16, 16)))));
  }

  @Test
  public void testRoutingInstanceType() throws IOException {
    Configuration c = parseConfig("routing-instance-type");

    /* All types for now should result in a VRF */
    /* TODO: perhaps some types e.g. forwarding should not result in a VRF */
    assertThat(c, hasVrfs(hasKey("ri-forwarding")));
    assertThat(c, hasVrfs(hasKey("ri-l2vpn")));
    assertThat(c, hasVrfs(hasKey("ri-virtual-router")));
    assertThat(c, hasVrfs(hasKey("ri-virtual-switch")));
    assertThat(c, hasVrfs(hasKey("ri-vrf")));
  }

  @Test
  public void testRoutingPolicy() throws IOException {
    Configuration c = parseConfig("routing-policy");
    Environment.Builder eb = Environment.builder(c).setDirection(Direction.IN);

    RoutingPolicy policyExact = c.getRoutingPolicies().get("route-filter-exact");
    RoutingPolicy policyLonger = c.getRoutingPolicies().get("route-filter-longer");
    RoutingPolicy policyPrange = c.getRoutingPolicies().get("route-filter-prange");
    RoutingPolicy policyThrough = c.getRoutingPolicies().get("route-filter-through");
    RoutingPolicy policyAddressmask = c.getRoutingPolicies().get("route-filter-addressmask");

    ConnectedRoute connectedRouteExact =
        new ConnectedRoute(Prefix.parse("1.2.3.4/16"), "nhinttest");
    ConnectedRoute connectedRouteLonger =
        new ConnectedRoute(Prefix.parse("1.2.3.4/19"), "nhinttest");
    ConnectedRoute connectedRouteInRange =
        new ConnectedRoute(Prefix.parse("1.2.3.4/17"), "nhinttest");
    ConnectedRoute connectedRouteInvalidPrefix =
        new ConnectedRoute(Prefix.parse("2.3.3.4/17"), "nhinttest");
    ConnectedRoute connectedRouteInvalidLength =
        new ConnectedRoute(Prefix.parse("2.3.3.4/29"), "nhinttest");

    ConnectedRoute connectedRouteMaskInvalidPrefix =
        new ConnectedRoute(Prefix.parse("9.2.9.4/16"), "nhinttest");
    ConnectedRoute connectedRouteMaskValid =
        new ConnectedRoute(Prefix.parse("1.9.3.9/16"), "nhinttest");
    ConnectedRoute connectedRouteMaskInvalidLength =
        new ConnectedRoute(Prefix.parse("1.9.3.9/17"), "nhinttest");

    eb.setVrf("vrf1");

    assertThat(
        policyExact.call(eb.setOriginalRoute(connectedRouteExact).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyExact.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(false));
    assertThat(
        policyExact
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyLonger.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyLonger
            .call(eb.setOriginalRoute(connectedRouteInvalidLength).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        policyLonger
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyPrange.call(eb.setOriginalRoute(connectedRouteInRange).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyPrange.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(false));
    assertThat(
        policyPrange
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyThrough.call(eb.setOriginalRoute(connectedRouteInRange).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyThrough.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(false));
    assertThat(
        policyThrough
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyAddressmask
            .call(eb.setOriginalRoute(connectedRouteMaskValid).build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        policyAddressmask
            .call(eb.setOriginalRoute(connectedRouteMaskInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        policyAddressmask
            .call(eb.setOriginalRoute(connectedRouteMaskInvalidLength).build())
            .getBooleanValue(),
        equalTo(false));
  }

  @Test
  public void testStaticRoutePreference() throws IOException {
    Configuration c = parseConfig("static-route-preference");
    assertThat(
        c,
        hasVrf(
            "default",
            hasStaticRoutes(
                equalTo(
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("1.2.3.4/24"))
                            .setNextHopIp(Ip.parse("10.0.0.1"))
                            .setAdministrativeCost(250)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("2.3.4.5/24"))
                            .setNextHopIp(Ip.parse("10.0.0.2"))
                            .setAdministrativeCost(5)
                            .build())))));
  }

  @Test
  public void testStaticRoutes() throws IOException {
    Configuration c = parseConfig("static-routes");
    assertThat(
        c,
        allOf(
            hasDefaultVrf(
                hasStaticRoutes(
                    containsInAnyOrder(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("1.0.0.0/8"))
                            .setNextHopIp(Ip.parse("10.0.0.1"))
                            .setAdministrativeCost(5)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("3.0.0.0/8"))
                            .setNonForwarding(true)
                            .setAdministrativeCost(5)
                            .build(),
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("4.0.0.0/8"))
                            .setNextHopInterface("ge-0/0/0.0")
                            .setAdministrativeCost(5)
                            .build()))),
            hasVrf(
                "ri2",
                hasStaticRoutes(
                    contains(
                        StaticRoute.builder()
                            .setNetwork(Prefix.parse("2.0.0.0/8"))
                            .setNextHopIp(Ip.parse("10.0.0.2"))
                            .setAdministrativeCost(5)
                            .build())))));
  }

  @Test
  public void testStormControl() throws IOException {
    /* allow storm-control configuration in an interface */
    parseConfig("storm-control");
  }

  @Test
  public void testSecurityAddressBookGlobalAddress() throws IOException {
    Configuration config = parseConfig("security-address-book-global-address");
    Map<String, IpSpace> ipSpaces = config.getIpSpaces();
    assertThat(ipSpaces.keySet(), contains("global~NAME"));
  }

  @Test
  public void testSecurityPolicy() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("security-policy");
    Map<String, Zone> zones = juniperConfiguration.getMasterLogicalSystem().getZones();

    assertThat(zones.keySet(), containsInAnyOrder("trust", "untrust"));

    Zone trust = zones.get("trust");
    assertThat(trust.getFromZonePolicies().keySet(), hasSize(0));
    assertThat(trust.getToZonePolicies().keySet(), hasSize(1));

    Zone untrust = zones.get("untrust");
    assertThat(untrust.getFromZonePolicies().keySet(), hasSize(1));
    assertThat(untrust.getToZonePolicies().keySet(), hasSize(0));
  }

  @Test
  public void testPreSourceNatOutgoingFilter() throws IOException {
    Configuration config = parseConfig("security-policy");
    String ifaceIn = "ge-0/0/0.0";
    String ifaceOut = "ge-0/0/1.0";

    IpAccessList securityPolicy1 =
        config.getAllInterfaces().get(ifaceOut).getPreTransformationOutgoingFilter();

    // Any arbitrary flow from trust to untrust should be permitted
    Flow flow1 = createFlow(IpProtocol.UDP, 90);
    Flow flow2 = createFlow(IpProtocol.TCP, 9000);

    assertThat(
        securityPolicy1, accepts(flow1, ifaceIn, config.getIpAccessLists(), config.getIpSpaces()));

    assertThat(
        securityPolicy1, accepts(flow2, ifaceIn, config.getIpAccessLists(), config.getIpSpaces()));

    // Packet to ifaceIn should be denied by default
    IpAccessList securityPolicy2 =
        config.getAllInterfaces().get(ifaceIn).getPreTransformationOutgoingFilter();

    assertThat(
        securityPolicy2, rejects(flow1, ifaceOut, config.getIpAccessLists(), config.getIpSpaces()));

    assertThat(
        securityPolicy2, rejects(flow2, ifaceOut, config.getIpAccessLists(), config.getIpSpaces()));
  }

  @Test
  public void testPsFromInterface() throws IOException {
    Configuration config = parseConfig("juniper-routing-policy");

    // Matches iface prefix, connected route
    for (Prefix p : ImmutableList.of(Prefix.parse("1.1.1.1/24"), Prefix.parse("2.2.2.2/24"))) {
      Result result =
          config
              .getRoutingPolicies()
              .get("POLICY-NAME")
              .call(
                  Environment.builder(config)
                      .setVrf(DEFAULT_VRF_NAME)
                      .setOriginalRoute(new ConnectedRoute(p, "iface"))
                      .build());
      assertThat(result.getBooleanValue(), equalTo(true));
    }

    // Does not match wrong prefix
    Result result =
        config
            .getRoutingPolicies()
            .get("POLICY-NAME")
            .call(
                Environment.builder(config)
                    .setVrf(DEFAULT_VRF_NAME)
                    .setOriginalRoute(new ConnectedRoute(Prefix.parse("3.3.3.3/24"), "iface"))
                    .build());
    assertThat(result.getBooleanValue(), equalTo(false));

    // Does not match static route, even with correct prefix
    result =
        config
            .getRoutingPolicies()
            .get("POLICY-NAME")
            .call(
                Environment.builder(config)
                    .setVrf(DEFAULT_VRF_NAME)
                    .setOriginalRoute(
                        StaticRoute.builder()
                            .setNextHopInterface("iface")
                            .setNetwork(Prefix.parse("1.1.1.1/24"))
                            .setAdministrativeCost(1)
                            .build())
                    .build());
    assertThat(result.getBooleanValue(), equalTo(false));
  }

  @Test
  public void testSourceNatPool() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("juniper-sourcenat-pool");
    Map<String, NatPool> pools =
        juniperConfiguration.getMasterLogicalSystem().getNatSource().getPools();

    Ip ip0 = Ip.parse("1.0.0.0");
    Ip ip1 = Ip.parse("1.0.0.1");
    Ip ip2 = Ip.parse("1.0.0.2");
    Ip ip3 = Ip.parse("1.0.0.3");

    assertThat(pools.keySet(), hasSize(5));

    assertThat(pools.get("POOL1").getFromAddress(), equalTo(ip1));
    assertThat(pools.get("POOL1").getToAddress(), equalTo(ip3));

    assertThat(pools.get("POOL2").getFromAddress(), equalTo(ip1));
    assertThat(pools.get("POOL2").getToAddress(), equalTo(ip3));

    assertThat(pools.get("POOL3").getFromAddress(), equalTo(ip0));
    assertThat(pools.get("POOL3").getToAddress(), equalTo(ip0));

    assertThat(pools.get("POOL4").getFromAddress(), equalTo(ip0));
    assertThat(pools.get("POOL4").getToAddress(), equalTo(ip1));

    assertThat(pools.get("POOL5").getFromAddress(), equalTo(ip1));
    assertThat(pools.get("POOL5").getToAddress(), equalTo(ip2));
  }

  @Test
  public void testScreenOptions() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("screen-options");

    assertThat(
        juniperConfiguration.getMasterLogicalSystem().getScreens().get("ALARM_OPTION").getAction(),
        equalTo(ScreenAction.ALARM_WITHOUT_DROP));

    Screen ids = juniperConfiguration.getMasterLogicalSystem().getScreens().get("IDS_OPTION_NAME");

    assertThat(ids, not(nullValue()));

    assertThat(ids.getAction(), equalTo(ScreenAction.DROP));
    assertThat(
        ids.getScreenOptions(),
        equalTo(
            ImmutableList.of(
                IcmpLarge.INSTANCE,
                IpUnknownProtocol.INSTANCE,
                TcpFinNoAck.INSTANCE,
                TcpSynFin.INSTANCE,
                TcpNoFlag.INSTANCE)));
  }

  @Test
  public void testScreenOptionsToVIModel() throws IOException {
    Configuration config = parseConfig("screen-options");

    IpAccessList inAcl = config.getIpAccessLists().get("FILTER1");
    IpAccessList screenAcl = config.getIpAccessLists().get("~SCREEN~IDS_OPTION_NAME");
    IpAccessList ifaceScreenAcl = config.getIpAccessLists().get("~SCREEN_INTERFACE~ge-0/0/0.0");
    IpAccessList zoneScreenAcl = config.getIpAccessLists().get("~SCREEN_ZONE~untrust");
    IpAccessList combinedInAcl =
        config.getIpAccessLists().get("~COMBINED_INCOMING_FILTER~ge-0/0/0.0");

    assertThat(inAcl, notNullValue());
    assertThat(screenAcl, notNullValue());
    assertThat(zoneScreenAcl, notNullValue());
    assertThat(ifaceScreenAcl, notNullValue());
    assertThat(combinedInAcl, notNullValue());

    List<ScreenOption> supportedOptions =
        ImmutableList.of(
            IcmpLarge.INSTANCE,
            IpUnknownProtocol.INSTANCE,
            TcpFinNoAck.INSTANCE,
            TcpSynFin.INSTANCE,
            TcpNoFlag.INSTANCE);

    assertThat(
        inAcl,
        equalTo(
            IpAccessList.builder()
                .setName("FILTER1")
                .setLines(
                    ImmutableList.of(
                        new IpAccessListLine(
                            LineAction.PERMIT,
                            AclLineMatchExprs.match(
                                HeaderSpace.builder()
                                    .setSrcIps(new IpWildcard(Ip.parse("1.2.3.6")).toIpSpace())
                                    .build()),
                            "TERM")))
                .build()));

    assertThat(
        screenAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN~IDS_OPTION_NAME")
                .setLines(
                    ImmutableList.of(
                        IpAccessListLine.rejecting(
                            new OrMatchExpr(
                                supportedOptions.stream()
                                    .map(ScreenOption::getAclLineMatchExpr)
                                    .collect(Collectors.toList()))),
                        IpAccessListLine.ACCEPT_ALL))
                .build()));

    assertThat(
        zoneScreenAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN_ZONE~untrust")
                .setLines(
                    ImmutableList.of(
                        IpAccessListLine.accepting(
                            new AndMatchExpr(
                                ImmutableList.of(
                                    new PermittedByAcl("~SCREEN~IDS_OPTION_NAME", false))))))
                .build()));

    assertThat(
        ifaceScreenAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~SCREEN_INTERFACE~ge-0/0/0.0")
                .setLines(
                    ImmutableList.of(
                        IpAccessListLine.accepting(
                            new PermittedByAcl("~SCREEN_ZONE~untrust", false))))
                .build()));

    assertThat(
        combinedInAcl,
        equalTo(
            IpAccessList.builder()
                .setName("~COMBINED_INCOMING_FILTER~ge-0/0/0.0")
                .setLines(
                    ImmutableList.of(
                        IpAccessListLine.accepting(
                            new AndMatchExpr(
                                ImmutableList.of(
                                    new PermittedByAcl("~SCREEN_INTERFACE~ge-0/0/0.0", false),
                                    new PermittedByAcl("FILTER1", false))))))
                .build()));

    assertThat(
        config.getAllInterfaces().get("ge-0/0/0.0").getIncomingFilter(), equalTo(combinedInAcl));
  }

  @Test
  public void testInstanceImport() throws IOException {
    String hostname = "instance-import";
    Configuration c = parseConfig(hostname);

    /*
     * instance-import for default VRF imports two policies, PS1 followed by PS2.
     * PS1 accepts VRF3, then VRF1. PS2 accepts VRF1 and an undefined MYSTERY_VRF, and rejects VRF2.
     * Resulting list of VRFs whose routes to import should have the referenced VRFs in the order
     * they were referenced, ignoring second reference to VRF1, not including undefined MYSTERY_VRF.
     */
    Vrf defaultVrf = c.getVrfs().get(DEFAULT_VRF_NAME);
    assertThat(defaultVrf.getCrossVrfImportVrfs(), contains("VRF3", "VRF1", "VRF2"));

    /*
    Test instance import policy behavior.
     - Routes from VRF1 and VRF3 should be accepted, but not routes from VRF2.
     - Routes from arbitrary other VRFs should be rejected by default policy.
    The only thing in the arguments to process that should get checked is source VRF.
    */
    RoutingPolicy instanceImportPolicy =
        c.getRoutingPolicies().get(defaultVrf.getCrossVrfImportPolicy());
    StaticRoute sr = StaticRoute.builder().setNetwork(Prefix.ZERO).setAdmin(5).build();
    assertThat(
        instanceImportPolicy.process(
            new AnnotatedRoute<>(sr, "VRF1"), null, null, DEFAULT_VRF_NAME, null),
        equalTo(true));
    assertThat(
        instanceImportPolicy.process(
            new AnnotatedRoute<>(sr, "VRF2"), null, null, DEFAULT_VRF_NAME, null),
        equalTo(false));
    assertThat(
        instanceImportPolicy.process(
            new AnnotatedRoute<>(sr, "VRF3"), null, null, DEFAULT_VRF_NAME, null),
        equalTo(true));
    assertThat(
        instanceImportPolicy.process(
            new AnnotatedRoute<>(sr, "ANOTHER_VRF"), null, null, DEFAULT_VRF_NAME, null),
        equalTo(false));

    /*
    Check resulting state of routes in snapshot.
     - VRF1 has static 1.1.1.1/30, which should be in default VRF since VRF1 routes are accepted
     - VRF2 has static 2.2.2.2/30, which should not be in default VRF since VRF2 routes are rejected
     - VRF3 doesn't have any routes, so should have no impact on default VRF
    */
    Batfish batfish = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);
    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    ImmutableMap<String, Set<AnnotatedRoute<AbstractRoute>>> routes =
        dp.getRibs().get(hostname).entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getTypedRoutes()));

    assertThat(
        routes.get(DEFAULT_VRF_NAME),
        contains(allOf(hasPrefix(Prefix.parse("1.1.1.1/30")), hasSourceVrf("VRF1"))));

    // Ensure that VRF2 does in fact have 2.2.2.2/30, as expected
    assertThat(routes.get("VRF2"), hasItem(hasPrefix(Prefix.parse("2.2.2.2/30"))));
  }

  @Test
  public void testInterfaceRibGroup() throws IOException {
    String hostname = "juniper-interface-ribgroup";
    Configuration c = parseConfig(hostname);
    Batfish batfish = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);
    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();

    ImmutableMap<String, Set<AnnotatedRoute<AbstractRoute>>> routes =
        dp.getRibs().get(hostname).entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getTypedRoutes()));
    String vrf2Name = "VRF2";

    Set<AnnotatedRoute<AbstractRoute>> defaultExpectedRoutes =
        ImmutableSet.of(
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.2/31"), "ge-0/0/0.0"),
                DEFAULT_VRF_NAME));
    Set<AnnotatedRoute<AbstractRoute>> vrf2ExpectedRoutes =
        ImmutableSet.of(
            // From default VRF
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            // Present normally
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.8/31"), "ge-0/0/3.0"), vrf2Name),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.8/31"), "ge-0/0/3.0"), vrf2Name));
    assertThat(routes.get(DEFAULT_VRF_NAME), equalTo(defaultExpectedRoutes));
    assertThat(routes.get(vrf2Name), equalTo(vrf2ExpectedRoutes));
  }

  @Test
  public void testInterfaceRibGroupWithPolicies() throws IOException {
    String hostname = "juniper-interface-ribgroup-with-policy";
    Configuration c = parseConfig(hostname);
    Batfish batfish = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);
    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();

    ImmutableMap<String, Set<AnnotatedRoute<AbstractRoute>>> routes =
        dp.getRibs().get(hostname).entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getTypedRoutes()));
    String vrf2Name = "VRF2";

    Set<AnnotatedRoute<AbstractRoute>> vrf2ExpectedRoutes =
        ImmutableSet.of(
            // allowed Default policy
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.4/31"), "ge-0/0/1.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.6/31"), "ge-0/0/2.0"), DEFAULT_VRF_NAME),
            // allowed by RIB_IN
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.4/31"), "ge-0/0/1.0"), DEFAULT_VRF_NAME),
            // Present normally
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.8/31"), "ge-0/0/3.0"), vrf2Name),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.8/31"), "ge-0/0/3.0"), vrf2Name));
    Set<AnnotatedRoute<AbstractRoute>> defaultExpectedRoutes =
        ImmutableSet.of(
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.4/31"), "ge-0/0/1.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.4/31"), "ge-0/0/1.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.6/31"), "ge-0/0/2.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.6/31"), "ge-0/0/2.0"),
                DEFAULT_VRF_NAME));
    assertThat(routes.get(DEFAULT_VRF_NAME), equalTo(defaultExpectedRoutes));
    assertThat(routes.get(vrf2Name), equalTo(vrf2ExpectedRoutes));
  }

  @Test
  public void testInterfaceRibGroupWithTransformation() throws IOException {
    String hostname = "juniper-interface-ribgroup-with-transformation";
    Configuration c = parseConfig(hostname);
    Batfish batfish = BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);
    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();

    ImmutableMap<String, Set<AnnotatedRoute<AbstractRoute>>> routes =
        dp.getRibs().get(hostname).entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getTypedRoutes()));
    String vrf2Name = "VRF2";

    Set<AnnotatedRoute<AbstractRoute>> defaultExpectedRoutes =
        ImmutableSet.of(
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0", 0), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.2/31"), "ge-0/0/0.0"),
                DEFAULT_VRF_NAME));
    Set<AnnotatedRoute<AbstractRoute>> vrf2ExpectedRoutes =
        ImmutableSet.of(
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "lo0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new LocalRoute(new InterfaceAddress("2.2.2.2/31"), "ge-0/0/0.0"), DEFAULT_VRF_NAME),
            new AnnotatedRoute<>(
                new ConnectedRoute(Prefix.parse("2.2.2.2/31"), "ge-0/0/0.0", 123),
                DEFAULT_VRF_NAME));
    assertThat(routes.get(DEFAULT_VRF_NAME), equalTo(defaultExpectedRoutes));
    assertThat(routes.get(vrf2Name), equalTo(vrf2ExpectedRoutes));
  }

  @Test
  public void testBgpRibGroup() throws IOException {
    String hostname = "juniper-bgp-rib-group";
    Configuration c = parseConfig(hostname);
    BatfishTestUtils.getBatfish(ImmutableSortedMap.of(hostname, c), _folder);

    assertThat(
        c.getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Prefix.parse("1.1.1.3/32"))
            .getAppliedRibGroup()
            .getName(),
        equalTo("RIB_GROUP_1"));
    assertThat(
        c.getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Prefix.parse("1.1.1.5/32"))
            .getAppliedRibGroup()
            .getName(),
        equalTo("RIB_GROUP_2"));
  }

  /** Throws the creation of {@link FirewallSessionInterfaceInfo} objects for juniper devices. */
  @Test
  public void testFirewallSession() throws IOException {
    Configuration c = parseConfig("firewall-session-info");

    String i0Name = "ge-0/0/0.0";
    String i1Name = "ge-0/0/0.1";
    String i2Name = "ge-0/0/0.2";
    String i3Name = "ge-0/0/0.3";

    assertThat(
        c.getAllInterfaces().get(i0Name).getFirewallSessionInterfaceInfo(),
        equalTo(
            new FirewallSessionInterfaceInfo(
                ImmutableList.of(i0Name, i1Name), "FILTER1", "FILTER2")));

    assertThat(
        c.getAllInterfaces().get(i1Name).getFirewallSessionInterfaceInfo(),
        equalTo(new FirewallSessionInterfaceInfo(ImmutableList.of(i0Name, i1Name), null, null)));

    assertThat(
        c.getAllInterfaces().get(i2Name).getFirewallSessionInterfaceInfo(),
        equalTo(new FirewallSessionInterfaceInfo(ImmutableList.of(i2Name), null, null)));

    // ge-0/0/0.3 is not part of any zoone, so no firewall session interface info.
    assertThat(c.getAllInterfaces().get(i3Name).getFirewallSessionInterfaceInfo(), nullValue());
  }

  @Test
  public void testPortAddressTranslation() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("juniper-nat-pat");
    Nat sourceNat = juniperConfiguration.getMasterLogicalSystem().getNatSource();
    NatPool pool0 = sourceNat.getPools().get("POOL0");
    NatPool pool1 = sourceNat.getPools().get("POOL1");
    NatPool pool2 = sourceNat.getPools().get("POOL2");
    NatPool pool3 = sourceNat.getPools().get("POOL3");

    // pool0 should not have pat specified
    assertNotNull(pool0);
    assertNull(pool0.getPortAddressTranslation());

    // pool1 should has a pat pool with range [2000,3000] with no port translation configured
    assertNotNull(pool1);
    assertNotNull(pool1.getPortAddressTranslation());
    assertThat(pool1.getPortAddressTranslation(), equalTo(new PatPool(2000, 3000)));

    // pool2 should specify no translation
    assertNotNull(pool2);
    assertNotNull(pool2.getPortAddressTranslation());
    assertThat(pool2.getPortAddressTranslation(), equalTo(NoPortTranslation.INSTANCE));

    // pool3 should have a pat pool ranging [10000,20000]
    assertNotNull(pool3);
    assertNotNull(pool3.getPortAddressTranslation());
    assertThat(pool3.getPortAddressTranslation(), equalTo(new PatPool(10000, 20000)));

    Nat destNat = juniperConfiguration.getMasterLogicalSystem().getNatDestination();
    NatPool pool4 = destNat.getPools().get("POOL4");
    NatPool pool5 = destNat.getPools().get("POOL5");

    // pool4 should have a pat pool ranging [6000,6000]
    assertNotNull(pool4);
    assertThat(pool4.getPortAddressTranslation(), equalTo(new PatPool(6000, 6000)));

    // pool5 should not have pat specified
    assertNotNull(pool5);
    assertNull(pool5.getPortAddressTranslation());
  }

  @Test
  public void testVrfSpecificPoolPortAddressTranslation() {
    JuniperConfiguration juniperConfiguration = parseJuniperConfig("juniper-nat-vrf");
    Nat sourceNat = juniperConfiguration.getMasterLogicalSystem().getNatSource();
    NatPool pool0 = sourceNat.getPools().get("POOL0");
    NatPool pool1 = sourceNat.getPools().get("POOL1");
    assertThat(pool0.getOwner(), equalTo("R1"));
    assertNull(pool1.getOwner());
  }
}

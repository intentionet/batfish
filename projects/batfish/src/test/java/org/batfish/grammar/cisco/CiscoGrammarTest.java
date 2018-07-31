package org.batfish.grammar.cisco;

import static org.batfish.datamodel.AuthenticationMethod.ENABLE;
import static org.batfish.datamodel.AuthenticationMethod.GROUP_RADIUS;
import static org.batfish.datamodel.AuthenticationMethod.GROUP_TACACS;
import static org.batfish.datamodel.AuthenticationMethod.GROUP_USER_DEFINED;
import static org.batfish.datamodel.AuthenticationMethod.KRB5;
import static org.batfish.datamodel.AuthenticationMethod.KRB5_TELNET;
import static org.batfish.datamodel.AuthenticationMethod.LINE;
import static org.batfish.datamodel.AuthenticationMethod.LOCAL;
import static org.batfish.datamodel.AuthenticationMethod.LOCAL_CASE;
import static org.batfish.datamodel.AuthenticationMethod.NONE;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.matchers.AaaAuthenticationLoginListMatchers.hasMethod;
import static org.batfish.datamodel.matchers.AaaAuthenticationLoginMatchers.hasListForKey;
import static org.batfish.datamodel.matchers.AaaAuthenticationMatchers.hasLogin;
import static org.batfish.datamodel.matchers.AaaMatchers.hasAuthentication;
import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.hasConjuncts;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.isAndMatchExprThat;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasAllowRemoteAsOut;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEbgp;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEquivalentAsPathMatchMode;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbors;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasConfigurationFormat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkeGateway;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkePhase1Policy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkePhase1Proposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkeProposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessList;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessLists;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpSpace;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPeerConfig;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPhase2Policy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPhase2Proposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPolicy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecProposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecVpn;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVendorFamily;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrfs;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasAclName;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasBandwidth;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructure;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasIpProtocols;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasMemberInterfaces;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasName;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasOutgoingFilter;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasOutgoingFilterName;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRedFlagWarning;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRoute6FilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasSrcOrDstPorts;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasZone;
import static org.batfish.datamodel.matchers.DataModelMatchers.isIpSpaceReferenceThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.isPermittedByAclThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.permits;
import static org.batfish.datamodel.matchers.EigrpInterfaceSettingsMatchers.hasDelay;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasDstIps;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasSrcIps;
import static org.batfish.datamodel.matchers.IkeGatewayMatchers.hasAddress;
import static org.batfish.datamodel.matchers.IkeGatewayMatchers.hasExternalInterface;
import static org.batfish.datamodel.matchers.IkeGatewayMatchers.hasIkePolicy;
import static org.batfish.datamodel.matchers.IkeGatewayMatchers.hasLocalIp;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasIkePhase1Key;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasIkePhase1Proposals;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasLocalInterface;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasRemoteIdentity;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasSelfIdentity;
import static org.batfish.datamodel.matchers.IkePolicyMatchers.hasPresharedKeyHash;
import static org.batfish.datamodel.matchers.IkeProposalMatchers.hasAuthenticationAlgorithm;
import static org.batfish.datamodel.matchers.IkeProposalMatchers.hasAuthenticationMethod;
import static org.batfish.datamodel.matchers.IkeProposalMatchers.hasDiffieHellmanGroup;
import static org.batfish.datamodel.matchers.IkeProposalMatchers.hasEncryptionAlgorithm;
import static org.batfish.datamodel.matchers.IkeProposalMatchers.hasLifeTimeSeconds;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDeclaredNames;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasEigrp;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfArea;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVrf;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPassive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPointToPoint;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isProxyArp;
import static org.batfish.datamodel.matchers.IpAccessListLineMatchers.hasMatchCondition;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.accepts;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.hasLines;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.rejects;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasDestinationAddress;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasLocalAddress;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasPhysicalInterface;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasPolicyAccessList;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasTunnelInterface;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.isIpsecDynamicPeerConfigThat;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.isIpsecStaticPeerConfigThat;
import static org.batfish.datamodel.matchers.IpsecPolicyMatchers.hasIpsecProposals;
import static org.batfish.datamodel.matchers.IpsecPolicyMatchers.hasPfsKeyGroup;
import static org.batfish.datamodel.matchers.IpsecProposalMatchers.hasProtocols;
import static org.batfish.datamodel.matchers.IpsecSessionMatchers.hasNegotiatedIkeP1Key;
import static org.batfish.datamodel.matchers.IpsecSessionMatchers.hasNegotiatedIkeP1Proposal;
import static org.batfish.datamodel.matchers.IpsecSessionMatchers.hasNegotiatedIpsecP2Proposal;
import static org.batfish.datamodel.matchers.IpsecVpnMatchers.hasBindInterface;
import static org.batfish.datamodel.matchers.IpsecVpnMatchers.hasIkeGatewaay;
import static org.batfish.datamodel.matchers.IpsecVpnMatchers.hasPolicy;
import static org.batfish.datamodel.matchers.LineMatchers.hasAuthenticationLoginList;
import static org.batfish.datamodel.matchers.LineMatchers.requiresAuthentication;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.hasHeaderSpace;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.isMatchHeaderSpaceThat;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasDefaultOriginateType;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasSuppressType3;
import static org.batfish.datamodel.matchers.OrMatchExprMatchers.hasDisjuncts;
import static org.batfish.datamodel.matchers.OrMatchExprMatchers.isOrMatchExprThat;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasNssa;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasStub;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasStubType;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasSummary;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.hasMetric;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.isAdvertised;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasArea;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasAreas;
import static org.batfish.datamodel.matchers.SnmpServerMatchers.hasCommunities;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasEigrpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasOspfProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasSnmpServer;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.batfish.datamodel.vendor_family.VendorFamilyMatchers.hasCisco;
import static org.batfish.datamodel.vendor_family.cisco.CiscoFamilyMatchers.hasAaa;
import static org.batfish.datamodel.vendor_family.cisco.CiscoFamilyMatchers.hasLogging;
import static org.batfish.datamodel.vendor_family.cisco.LoggingMatchers.isOn;
import static org.batfish.grammar.cisco.CiscoControlPlaneExtractor.SERIAL_LINE;
import static org.batfish.representation.cisco.CiscoConfiguration.computeCombinedOutgoingAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeInspectClassMapAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeInspectPolicyMapAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeProtocolObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeServiceObjectAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeServiceObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeZonePairAclName;
import static org.batfish.representation.cisco.CiscoStructureType.ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.INSPECT_CLASS_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.INSPECT_POLICY_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.IPV4_ACCESS_LIST_EXTENDED;
import static org.batfish.representation.cisco.CiscoStructureType.IPV4_ACCESS_LIST_STANDARD;
import static org.batfish.representation.cisco.CiscoStructureType.IPV6_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.IPV6_ACCESS_LIST_EXTENDED;
import static org.batfish.representation.cisco.CiscoStructureType.IPV6_ACCESS_LIST_STANDARD;
import static org.batfish.representation.cisco.CiscoStructureType.IP_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.KEYRING;
import static org.batfish.representation.cisco.CiscoStructureType.MAC_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.NETWORK_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureType.NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX_SET;
import static org.batfish.representation.cisco.CiscoStructureType.PROTOCOL_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.SECURITY_ZONE;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INSPECT_POLICY_MAP_INSPECT_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_INCOMING_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_OUTGOING_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_DESTINATION_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISAKMP_PROFILE_KEYRING;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_ACCESS_LIST;
import static org.batfish.representation.cisco.OspfProcess.getReferenceOspfBandwidth;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ValueGraph;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.IpsecUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpsecAuthenticationAlgorithm;
import org.batfish.datamodel.IpsecEncapsulationMode;
import org.batfish.datamodel.IpsecPeerConfigId;
import org.batfish.datamodel.IpsecProtocol;
import org.batfish.datamodel.IpsecSession;
import org.batfish.datamodel.Line;
import org.batfish.datamodel.LineType;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.matchers.ConfigurationMatchers;
import org.batfish.datamodel.matchers.EigrpInterfaceSettingsMatchers;
import org.batfish.datamodel.matchers.EigrpProcessMatchers;
import org.batfish.datamodel.matchers.IkeGatewayMatchers;
import org.batfish.datamodel.matchers.IkePhase1KeyMatchers;
import org.batfish.datamodel.matchers.IkePhase1ProposalMatchers;
import org.batfish.datamodel.matchers.InterfaceMatchers;
import org.batfish.datamodel.matchers.IpsecPeerConfigMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2PolicyMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2ProposalMatchers;
import org.batfish.datamodel.matchers.IpsecPolicyMatchers;
import org.batfish.datamodel.matchers.IpsecProposalMatchers;
import org.batfish.datamodel.matchers.IpsecVpnMatchers;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.datamodel.matchers.StubSettingsMatchers;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link CiscoParser} and {@link CiscoControlPlaneExtractor}. */
public class CiscoGrammarTest {

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco/testconfigs/";
  private static final String TESTRIGS_PREFIX = "org/batfish/grammar/cisco/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private Flow createFlow(IpProtocol protocol, int srcPort, int dstPort) {
    return Flow.builder()
        .setIngressNode("")
        .setTag("")
        .setIpProtocol(protocol)
        .setSrcPort(srcPort)
        .setDstPort(dstPort)
        .build();
  }

  private Flow createIcmpFlow(Integer icmpType) {
    return Flow.builder()
        .setIngressNode("")
        .setTag("")
        .setIpProtocol(IpProtocol.ICMP)
        .setIcmpType(icmpType)
        .build();
  }

  @Test
  public void testAaaNewmodel() throws IOException {
    Configuration newModelConfiguration = parseConfig("aaaNewmodel");
    boolean aaaNewmodel = newModelConfiguration.getVendorFamily().getCisco().getAaa().getNewModel();
    assertTrue(aaaNewmodel);

    Configuration noNewModelConfiguration = parseConfig("aaaNoNewmodel");
    aaaNewmodel = noNewModelConfiguration.getVendorFamily().getCisco().getAaa().getNewModel();
    assertFalse(aaaNewmodel);
  }

  @Test
  public void testLineAuthenticationMethods() throws IOException {
    // test IOS
    Configuration iosConfiguration = parseConfig("aaaAuthenticationIos");
    SortedMap<String, Line> iosLines = iosConfiguration.getVendorFamily().getCisco().getLines();

    assertThat(iosLines.get("con0"), hasAuthenticationLoginList(hasMethod(GROUP_TACACS)));
    assertThat(iosLines.get("con0"), hasAuthenticationLoginList(hasMethod(KRB5)));
    assertThat(iosLines.get("con0"), hasAuthenticationLoginList(hasMethod(LOCAL_CASE)));
    assertThat(iosLines.get("con0"), hasAuthenticationLoginList(hasMethod(LOCAL)));

    assertThat(iosLines.get("vty0"), hasAuthenticationLoginList(hasMethod(KRB5_TELNET)));
    assertThat(iosLines.get("vty0"), hasAuthenticationLoginList(hasMethod(GROUP_RADIUS)));
    assertThat(iosLines.get("vty0"), hasAuthenticationLoginList(hasMethod(ENABLE)));

    assertThat(iosLines.get("aux0"), hasAuthenticationLoginList(hasMethod(GROUP_USER_DEFINED)));
    assertThat(iosLines.get("aux0"), hasAuthenticationLoginList(hasMethod(LINE)));
    assertThat(iosLines.get("aux0"), hasAuthenticationLoginList(hasMethod(NONE)));

    // test ASA
    Configuration asaConfiguration = parseConfig("aaaAuthenticationAsa");
    SortedMap<String, Line> asaLines = asaConfiguration.getVendorFamily().getCisco().getLines();

    assertThat(asaLines.get("http"), not(hasAuthenticationLoginList(notNullValue())));

    assertThat(asaLines.get("ssh"), hasAuthenticationLoginList(hasMethod(GROUP_USER_DEFINED)));
    assertThat(asaLines.get("ssh"), hasAuthenticationLoginList(hasMethod(LOCAL_CASE)));

    assertThat(asaLines.get(SERIAL_LINE), not(hasAuthenticationLoginList()));

    assertThat(asaLines.get("telnet"), hasAuthenticationLoginList(hasMethod(GROUP_USER_DEFINED)));
    assertThat(asaLines.get("telnet"), hasAuthenticationLoginList(not(hasMethod(LOCAL_CASE))));
  }

  @Test
  public void testAaaAuthenticationLogin() throws IOException {
    // test ASA config
    Configuration aaaAuthAsaConfiguration = parseConfig("aaaAuthenticationAsa");
    SortedMap<String, Line> asaLines =
        aaaAuthAsaConfiguration.getVendorFamily().getCisco().getLines();
    for (Line line : asaLines.values()) {
      if (line.getLineType() == LineType.HTTP || line.getLineType() == LineType.SERIAL) {
        assertThat(line, not(requiresAuthentication()));
      } else {
        assertThat(line, requiresAuthentication());
      }
    }

    assertThat(
        aaaAuthAsaConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(hasAuthentication(hasLogin(hasListForKey(hasMethod(LOCAL_CASE), "ssh")))))));
    assertThat(
        aaaAuthAsaConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(hasListForKey(hasMethod(GROUP_USER_DEFINED), "ssh")))))));
    assertThat(
        aaaAuthAsaConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(not(hasListForKey(hasMethod(LOCAL_CASE), "http"))))))));
    assertThat(
        aaaAuthAsaConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(not(hasListForKey(hasMethod(GROUP_USER_DEFINED), "http"))))))));
    assertThat(
        aaaAuthAsaConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(not(hasListForKey(hasMethod(LOCAL_CASE), SERIAL_LINE))))))));
    assertThat(
        aaaAuthAsaConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(
                            not(hasListForKey(hasMethod(GROUP_USER_DEFINED), SERIAL_LINE))))))));
    assertThat(
        aaaAuthAsaConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(hasListForKey(not(hasMethod(LOCAL_CASE)), "telnet")))))));
    assertThat(
        aaaAuthAsaConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(hasListForKey(hasMethod(GROUP_USER_DEFINED), "telnet")))))));

    // test IOS config
    Configuration aaaAuthIosConfiguration = parseConfig("aaaAuthenticationIos");

    SortedMap<String, Line> iosLines =
        aaaAuthIosConfiguration.getVendorFamily().getCisco().getLines();

    for (Line line : iosLines.values()) {
      if (line.getLineType() == LineType.AUX) {
        assertThat(line, not(requiresAuthentication()));
      } else {
        assertThat(line, requiresAuthentication());
      }
    }

    assertThat(
        aaaAuthIosConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(hasListForKey(hasMethod(GROUP_TACACS), "default")))))));
    assertThat(
        aaaAuthIosConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(hasAuthentication(hasLogin(hasListForKey(hasMethod(LOCAL), "default")))))));
    assertThat(
        aaaAuthIosConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(hasListForKey(not(hasMethod(GROUP_RADIUS)), "default")))))));
    assertThat(
        aaaAuthIosConfiguration,
        hasVendorFamily(
            hasCisco(
                hasAaa(
                    hasAuthentication(
                        hasLogin(not(hasListForKey(hasMethod(GROUP_TACACS), "ssh"))))))));

    // test IOS config with no default login list defined
    Configuration aaaAuthIosConfigNoDefault = parseConfig("aaaAuthenticationIosNoDefault");

    SortedMap<String, Line> iosNoDefaultLines =
        aaaAuthIosConfigNoDefault.getVendorFamily().getCisco().getLines();

    for (Line line : iosNoDefaultLines.values()) {
      if (line.getLineType() == LineType.AUX || line.getLineType() == LineType.CON) {
        assertThat(line, not(requiresAuthentication()));
      } else {
        assertThat(line, requiresAuthentication());
      }
    }
  }

  @Test
  public void testAGAclReferrers() throws IOException {
    String filename = "configs/iosAccessGroupAcl";
    String testrigName = "access-group-acl";
    List<String> configurationNames = ImmutableList.of("iosAccessGroupAcl");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // check expected references for {mac,ip}_acl{_unused,}
    assertThat(ccae, hasNumReferrers(filename, MAC_ACCESS_LIST, "mac_acl_unused", 0));
    assertThat(ccae, hasNumReferrers(filename, MAC_ACCESS_LIST, "mac_acl", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "ip_acl_unused", 0));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "ip_acl", 1));
  }

  @Test
  public void testAGAclUndefined() throws IOException {
    String filename = "configs/iosAccessGroupAcl";
    String testrigName = "access-group-acl";
    List<String> configurationNames = ImmutableList.of("iosAccessGroupAcl");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    SortedMap<String, SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>>>
        undefinedReferences =
            batfish.loadConvertConfigurationAnswerElementOrReparse().getUndefinedReferences();

    // only mac_acl_udef and ip_acl_udef should be undefined references
    assertThat(undefinedReferences, hasKey(filename));
    SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>> byHost =
        undefinedReferences.get(filename);
    assertThat(byHost, hasKey(ACCESS_LIST.getDescription()));
    SortedMap<String, SortedMap<String, SortedSet<Integer>>> byType =
        byHost.get(ACCESS_LIST.getDescription());

    assertThat(byType.keySet(), hasSize(2));
    assertThat(byType, hasKey("ip_acl_udef"));
    assertThat(byType, hasKey("mac_acl_udef"));
  }

  @Test
  public void testAristaOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("aristaOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(3e6d));

    Configuration defaults = parseConfig("aristaOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.ARISTA)));
  }

  @Test
  public void testArubaConfigurationFormat() throws IOException {
    Configuration arubaConfig = parseConfig("arubaConfiguration");

    assertThat(arubaConfig, hasConfigurationFormat(equalTo(ConfigurationFormat.ARUBAOS)));
  }

  @Test
  public void testAsaOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("asaOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(3e6d));

    Configuration defaults = parseConfig("asaOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_ASA)));
  }

  @Test
  public void testAsaServiceObject() throws IOException {
    String hostname = "asa-service-object";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String osIcmpAclName = computeServiceObjectAclName("OS_ICMP");
    String osTcpAclName = computeServiceObjectAclName("OS_TCPUDP");
    String ogsAclName = computeServiceObjectGroupAclName("OGS1");

    Flow flowIcmpPass = createIcmpFlow(IcmpType.ECHO_REQUEST);
    Flow flowIcmpFail = createIcmpFlow(IcmpType.ECHO_REPLY);
    Flow flowInlinePass = createFlow(IpProtocol.UDP, 1, 1234);
    Flow flowTcpPass = createFlow(IpProtocol.TCP, 65535, 1);
    Flow flowUdpPass = createFlow(IpProtocol.UDP, 65535, 1);
    Flow flowTcpFail = createFlow(IpProtocol.TCP, 65534, 1);

    /* Confirm service objects have the correct number of referrers */
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT, "OS_TCPUDP", 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT, "OS_ICMP", 0));
    /* Confirm undefined reference shows up as such */
    assertThat(ccae, hasUndefinedReference(filename, SERVICE_OBJECT, "OS_UNDEFINED"));

    /* Confirm IpAcls created from service objects permit and reject the correct flows */
    assertThat(c, hasIpAccessList(osTcpAclName, accepts(flowTcpPass, null, c)));
    assertThat(c, hasIpAccessList(osTcpAclName, accepts(flowUdpPass, null, c)));
    assertThat(c, hasIpAccessList(osTcpAclName, not(accepts(flowTcpFail, null, c))));
    assertThat(c, hasIpAccessList(osIcmpAclName, accepts(flowIcmpPass, null, c)));
    assertThat(c, hasIpAccessList(osIcmpAclName, not(accepts(flowIcmpFail, null, c))));

    /* Confirm object-group permits and rejects the flows determined by its constituent service objects */
    assertThat(c, hasIpAccessList(ogsAclName, accepts(flowTcpPass, null, c)));
    assertThat(c, hasIpAccessList(ogsAclName, not(accepts(flowTcpFail, null, c))));
    assertThat(c, hasIpAccessList(ogsAclName, accepts(flowInlinePass, null, c)));
  }

  @Test
  public void testIosLoggingOnDefault() throws IOException {
    Configuration loggingOnOmitted = parseConfig("iosLoggingOnOmitted");
    assertThat(loggingOnOmitted, hasVendorFamily(hasCisco(hasLogging(isOn()))));
  }

  @Test
  public void testIosAclObjectGroup() throws IOException {
    String hostname = "ios-acl-object-group";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * The produced ACL should permit if source matchers object-group ogn1, destination matches
     * ogn2, and service matches ogs1.
     */
    assertThat(
        c,
        hasIpAccessList(
            "acl1",
            hasLines(
                hasItem(
                    hasMatchCondition(
                        isAndMatchExprThat(
                            hasConjuncts(
                                containsInAnyOrder(
                                    ImmutableList.of(
                                        isMatchHeaderSpaceThat(
                                            hasHeaderSpace(
                                                allOf(
                                                    hasDstIps(
                                                        isIpSpaceReferenceThat(hasName("ogn2"))),
                                                    hasSrcIps(
                                                        isIpSpaceReferenceThat(hasName("ogn1")))))),
                                        isPermittedByAclThat(
                                            hasAclName(
                                                computeServiceObjectGroupAclName("ogs1"))))))))))));

    /*
     * We expect only object-groups ogsunused1, ognunused1 to have zero referrers
     */
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "ogs1", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "ogn1", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "ogn2", 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "ogsunused1", 0));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "ognunused1", 0));

    /*
     * We expect undefined references only to object-groups ogsfake, ognfake1, ognfake2
     */
    assertThat(ccae, not(hasUndefinedReference(filename, SERVICE_OBJECT_GROUP, "ogs1")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT_GROUP, "ogn1")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT_GROUP, "ogn2")));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            PROTOCOL_OR_SERVICE_OBJECT_GROUP,
            "ogsfake",
            EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, NETWORK_OBJECT_GROUP, "ognfake2", EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, NETWORK_OBJECT_GROUP, "ognfake1", EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP));
  }

  @Test
  public void testIosAclReferences() throws IOException {
    String hostname = "ios-acl";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm reference counts are correct for ACLs
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "AL", 2));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "AL_IF", 3));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "10", 0));
    assertThat(ccae, hasNumReferrers(filename, IPV6_ACCESS_LIST_EXTENDED, "AL6", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV6_ACCESS_LIST_STANDARD, "AL6_UNUSED", 0));

    // Confirm undefined references are detected
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, IPV4_ACCESS_LIST, "AL_UNDEF", ROUTE_MAP_MATCH_IPV4_ACCESS_LIST));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, IPV6_ACCESS_LIST, "AL6_UNDEF", ROUTE_MAP_MATCH_IPV6_ACCESS_LIST));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, IPV4_ACCESS_LIST, "AL_IF_UNDEF", IP_NAT_DESTINATION_ACCESS_LIST));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, IPV4_ACCESS_LIST, "AL_IF_UNDEF", INTERFACE_INCOMING_FILTER));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, IPV4_ACCESS_LIST, "AL_IF_UNDEF", INTERFACE_OUTGOING_FILTER));
  }

  @Test
  public void testIosEigrpNetwork() throws IOException {
    Configuration c = parseConfig("ios-eigrp-network");

    /* Confirm classic mode networks are configured correctly */
    assertThat(c, hasDefaultVrf(hasEigrpProcess(EigrpProcessMatchers.hasAsn(1L))));
    assertThat(c, hasInterface("Ethernet0", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));
    assertThat(c, hasInterface("Ethernet1", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));
    assertThat(c, hasInterface("Ethernet2", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));

    /* Confirm named mode networks are configured correctly */
    assertThat(
        c,
        ConfigurationMatchers.hasVrf("vrf-name", hasEigrpProcess(EigrpProcessMatchers.hasAsn(2L))));
    assertThat(c, hasInterface("Ethernet3", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(2L))));
    assertThat(c, hasInterface("Ethernet4", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(2L))));
    assertThat(c, hasInterface("Ethernet5", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(2L))));

    /* Passive interfaces are configured correctly */
    assertThat(
        c, hasInterface("Ethernet0", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(false))));
    assertThat(
        c, hasInterface("Ethernet1", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(false))));
    assertThat(
        c, hasInterface("Ethernet2", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(true))));
    assertThat(
        c, hasInterface("Ethernet3", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(false))));
    assertThat(
        c, hasInterface("Ethernet4", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(false))));
    assertThat(
        c, hasInterface("Ethernet5", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(true))));
  }

  @Test
  public void testIosInspection() throws IOException {
    Configuration c = parseConfig("ios-inspection");

    String zone1Name = "z1";
    String zone2Name = "z2";
    String inspectAclName = "inspectacl";
    String inspectClassMapName = "ci";
    String inspectPolicyMapName = "pmi";
    String eth0Name = "Ethernet0";
    String eth1Name = "Ethernet1";
    String eth2Name = "Ethernet2";
    String eth3Name = "Ethernet3";
    String eth3OriginalAclName = "acl3out";
    String eth3CombinedAclName = computeCombinedOutgoingAclName(eth3Name);
    String zonePairAclName = computeZonePairAclName(zone1Name, zone2Name);
    String zone2OutgoingAclName = CiscoConfiguration.computeZoneOutgoingAclName(zone2Name);

    /* Check for expected generated ACLs */
    assertThat(c, hasIpAccessLists(hasKey(inspectAclName)));
    assertThat(c, hasIpAccessLists(hasKey(computeInspectClassMapAclName(inspectClassMapName))));
    assertThat(c, hasIpAccessLists(hasKey(computeInspectPolicyMapAclName(inspectPolicyMapName))));
    assertThat(c, hasIpAccessLists(hasKey(zonePairAclName)));
    assertThat(c, hasIpAccessLists(hasKey(eth3OriginalAclName)));
    assertThat(c, hasIpAccessLists(hasKey(eth3CombinedAclName)));
    assertThat(c, hasIpAccessLists(hasKey(zone2OutgoingAclName)));

    /* Check that interfaces have correct ACLs assigned */
    assertThat(c, hasInterface(eth2Name, hasOutgoingFilterName(zone2OutgoingAclName)));
    assertThat(c, hasInterface(eth3Name, hasOutgoingFilterName(eth3CombinedAclName)));

    IpAccessList eth2Acl = c.getInterfaces().get(eth2Name).getOutgoingFilter();
    IpAccessList eth3Acl = c.getInterfaces().get(eth3Name).getOutgoingFilter();

    /* Check that expected traffic is permitted/denied */
    Flow permittedByBoth =
        Flow.builder()
            .setSrcIp(new Ip("1.1.1.1"))
            .setDstIp(new Ip("2.2.2.2"))
            .setIpProtocol(IpProtocol.TCP)
            .setDstPort(80)
            .setIngressNode("internet")
            .setTag("none")
            .build();
    assertThat(eth2Acl, accepts(permittedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth2Acl, accepts(permittedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, accepts(permittedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, accepts(permittedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));

    Flow permittedThroughEth2Only =
        Flow.builder()
            .setSrcIp(new Ip("1.1.1.2"))
            .setDstIp(new Ip("2.2.2.2"))
            .setIpProtocol(IpProtocol.TCP)
            .setDstPort(80)
            .setIngressNode("internet")
            .setTag("none")
            .build();
    assertThat(
        eth2Acl,
        accepts(permittedThroughEth2Only, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        eth2Acl,
        accepts(permittedThroughEth2Only, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        eth3Acl,
        rejects(permittedThroughEth2Only, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        eth3Acl,
        rejects(permittedThroughEth2Only, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));

    Flow deniedByBoth =
        Flow.builder()
            .setSrcIp(new Ip("1.1.1.1"))
            .setDstIp(new Ip("2.2.2.2"))
            .setIpProtocol(IpProtocol.TCP)
            .setDstPort(81)
            .setIngressNode("internet")
            .setTag("none")
            .build();
    assertThat(eth2Acl, rejects(deniedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth2Acl, rejects(deniedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, rejects(deniedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, rejects(deniedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testIosInterfaceDelay() throws IOException {
    Configuration c = parseConfig("ios-interface-delay");

    assertThat(c, hasInterface("GigabitEthernet0/0", hasEigrp(hasDelay(1E7))));
    assertThat(c, hasInterface("GigabitEthernet0/1", hasEigrp(hasDelay(1E10))));
    assertThat(c, hasInterface("FastEthernet0/1", hasEigrp(hasDelay(1E8))));
  }

  @Test
  public void testIosInterfaceSpeed() throws IOException {
    Configuration c = parseConfig("ios-interface-speed");

    assertThat(c, hasInterface("GigabitEthernet0/0", hasBandwidth(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/1", hasBandwidth(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/2", hasBandwidth(100E6D)));
  }

  @Test
  public void testIosHttpInspection() throws IOException {
    String hostname = "ios-http-inspection";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ccae, hasNumReferrers(filename, INSPECT_CLASS_MAP, "ci", 1));
    assertThat(ccae, hasNumReferrers(filename, INSPECT_CLASS_MAP, "ciunused", 0));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, INSPECT_CLASS_MAP, "ciundefined", INSPECT_POLICY_MAP_INSPECT_CLASS));
  }

  @Test
  public void testIosKeyring() throws IOException {
    String hostname = "ios-keyrings";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ccae, hasNumReferrers(filename, KEYRING, "kused", 1));
    assertThat(ccae, hasNumReferrers(filename, KEYRING, "kunused", 0));
    assertThat(
        ccae, hasUndefinedReference(filename, KEYRING, "kundefined", ISAKMP_PROFILE_KEYRING));
  }

  @Test
  public void testIosNeighborDefaultOriginate() throws IOException {
    String testrigName = "ios-default-originate";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    TESTRIGS_PREFIX + testrigName, ImmutableList.of("originator", "listener"))
                .build(),
            _folder);

    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> routesOnListener =
        dp.getRibs().get("listener").get(DEFAULT_VRF_NAME).getRoutes();

    // Ensure that default route is advertised to and installed on listener
    assertThat(routesOnListener, hasItem(hasPrefix(Prefix.ZERO)));
  }

  @Test
  public void testIosObjectGroupNetwork() throws IOException {
    String hostname = "ios-object-group-network";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    Ip ognWildcardIp = new Ip("1.128.0.0");
    Ip ognHostIp = new Ip("2.0.0.1");
    Ip ognUnmatchedIp = new Ip("2.0.0.0");

    String ognNameHost = "ogn_host";
    String ognNameIndirect = "ogn_indirect";
    String ognNameNetworkObject = "ogn_network_object";
    String ognNameNetworkObjectIndirect = "ogn_object_group_indirect";
    String ognNameUndef = "ogn_undef";
    String ognNameUnused = "ogn_unused";
    String ognNameWildcard = "ogn_wildcard";

    /* Each object group should permit an IP iff it is in its space. */
    assertThat(c, hasIpSpace(ognNameHost, containsIp(ognHostIp)));
    assertThat(c, hasIpSpace(ognNameHost, not(containsIp(ognUnmatchedIp))));
    assertThat(c, hasIpSpace(ognNameIndirect, containsIp(ognWildcardIp, c.getIpSpaces())));
    assertThat(c, hasIpSpace(ognNameIndirect, not(containsIp(ognUnmatchedIp, c.getIpSpaces()))));
    assertThat(c, hasIpSpace(ognNameNetworkObject, containsIp(ognHostIp)));
    assertThat(c, hasIpSpace(ognNameNetworkObject, containsIp(ognWildcardIp)));
    assertThat(c, hasIpSpace(ognNameNetworkObject, not(containsIp(ognUnmatchedIp))));
    assertThat(c, hasIpSpace(ognNameNetworkObjectIndirect, containsIp(ognHostIp, c.getIpSpaces())));
    assertThat(
        c, hasIpSpace(ognNameNetworkObjectIndirect, containsIp(ognWildcardIp, c.getIpSpaces())));
    assertThat(
        c,
        hasIpSpace(ognNameNetworkObjectIndirect, not(containsIp(ognUnmatchedIp, c.getIpSpaces()))));
    assertThat(c, hasIpSpace(ognNameUnused, not(containsIp(ognUnmatchedIp))));
    assertThat(c, hasIpSpace(ognNameWildcard, containsIp(ognWildcardIp)));
    assertThat(c, hasIpSpace(ognNameWildcard, not(containsIp(ognUnmatchedIp))));

    /* Confirm the used object groups have the correct number of referrers */
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, ognNameWildcard, 2));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, ognNameHost, 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, ognNameIndirect, 1));
    /* Confirm the unused object group has no referrers */
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, ognNameUnused, 0));
    /* Confirm the undefined reference shows up as such */
    assertThat(ccae, hasUndefinedReference(filename, NETWORK_OBJECT_GROUP, ognNameUndef));
  }

  @Test
  public void testIosObjectGroupProtocol() throws IOException {
    String hostname = "ios-object-group-protocol";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    String ogpIcmpName = "ogp1";
    String ogpTcpUdpName = "ogp2";
    String ogpEmptyName = "ogp3";
    String ogpDuplicateName = "ogp4";
    String ogpUnusedName = "ogp5";
    String ogpUndefName = "ogpundef";
    String aclIcmpName = "aclicmp";
    String aclTcpUdpName = "acltcpudp";
    String aclEmptyName = "aclempty";
    String aclDuplicateName = "aclduplicate";
    String aclUndefName = "aclundef";
    String ogpAclIcmpName = computeProtocolObjectGroupAclName(ogpIcmpName);
    String ogpAclTcpUdpName = computeProtocolObjectGroupAclName(ogpTcpUdpName);
    String ogpAclEmptyName = computeProtocolObjectGroupAclName(ogpEmptyName);
    String ogpAclDuplicateName = computeProtocolObjectGroupAclName(ogpDuplicateName);
    Flow icmpFlow =
        Flow.builder().setTag("").setIngressNode("").setIpProtocol(IpProtocol.ICMP).build();
    Flow tcpFlow =
        Flow.builder().setTag("").setIngressNode("").setIpProtocol(IpProtocol.TCP).build();

    /* Confirm the used object groups have referrers */
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, ogpIcmpName, 1));
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, ogpTcpUdpName, 1));
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, ogpEmptyName, 1));
    /* Confirm the unused object group has no referrers */
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, ogpUnusedName, 0));
    /* Confirm the undefined reference shows up as such */
    assertThat(
        ccae, hasUndefinedReference(filename, PROTOCOL_OR_SERVICE_OBJECT_GROUP, ogpUndefName));

    /*
     * Icmp protocol object group and the acl referencing it should only accept Icmp and reject Tcp
     */
    assertThat(c, hasIpAccessList(ogpAclIcmpName, accepts(icmpFlow, null, c)));
    assertThat(c, hasIpAccessList(ogpAclIcmpName, rejects(tcpFlow, null, c)));
    assertThat(c, hasIpAccessList(aclIcmpName, accepts(icmpFlow, null, c)));
    assertThat(c, hasIpAccessList(aclIcmpName, rejects(tcpFlow, null, c)));

    /*
     * TcpUdp protocol object group and the acl referencing it should reject Icmp and accept Tcp
     */
    assertThat(c, hasIpAccessList(ogpAclTcpUdpName, rejects(icmpFlow, null, c)));
    assertThat(c, hasIpAccessList(ogpAclTcpUdpName, accepts(tcpFlow, null, c)));
    assertThat(c, hasIpAccessList(aclTcpUdpName, rejects(icmpFlow, null, c)));
    assertThat(c, hasIpAccessList(aclTcpUdpName, accepts(tcpFlow, null, c)));

    /*
     * Empty protocol object group and the acl referencing it should reject everything
     */
    assertThat(c, hasIpAccessList(ogpAclEmptyName, rejects(icmpFlow, null, c)));
    assertThat(c, hasIpAccessList(ogpAclEmptyName, rejects(tcpFlow, null, c)));
    assertThat(c, hasIpAccessList(aclEmptyName, rejects(icmpFlow, null, c)));
    assertThat(c, hasIpAccessList(aclEmptyName, rejects(tcpFlow, null, c)));

    /*
     * Empty protocol object group that is erroneously redefined should still reject everything
     */
    assertThat(c, hasIpAccessList(ogpAclDuplicateName, rejects(icmpFlow, null, c)));
    assertThat(c, hasIpAccessList(ogpAclDuplicateName, rejects(tcpFlow, null, c)));
    assertThat(c, hasIpAccessList(aclDuplicateName, rejects(icmpFlow, null, c)));
    assertThat(c, hasIpAccessList(aclDuplicateName, rejects(tcpFlow, null, c)));

    /*
     * Undefined protocol object group should reject everything
     */
    assertThat(c, hasIpAccessList(aclUndefName, rejects(icmpFlow, null, c)));
    assertThat(c, hasIpAccessList(aclUndefName, rejects(tcpFlow, null, c)));
  }

  @Test
  public void testIosObjectGroupService() throws IOException {
    String hostname = "ios-object-group-service";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm the used object groups have referrers */
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "og-icmp", 2));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "og-tcp", 1));
    /* Confirm the unused object group has no referrers */
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "og-udp", 0));
    /* Confirm the undefined reference shows up as such */
    assertThat(ccae, hasUndefinedReference(filename, PROTOCOL_OR_SERVICE_OBJECT_GROUP, "og-undef"));

    /* og-icmp */
    assertThat(
        c,
        hasIpAccessList(
            computeServiceObjectGroupAclName("og-icmp"),
            hasLines(
                containsInAnyOrder(
                    ImmutableList.of(
                        hasMatchCondition(
                            isOrMatchExprThat(
                                hasDisjuncts(
                                    contains(
                                        isMatchHeaderSpaceThat(
                                            hasHeaderSpace(
                                                hasIpProtocols(
                                                    contains(IpProtocol.ICMP)))))))))))));
    /* og-tcp */
    assertThat(
        c,
        hasIpAccessList(
            computeServiceObjectGroupAclName("og-tcp"),
            hasLines(
                containsInAnyOrder(
                    ImmutableList.of(
                        hasMatchCondition(
                            isOrMatchExprThat(
                                hasDisjuncts(
                                    containsInAnyOrder(
                                        ImmutableList.of(
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(new SubRange(65500)))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.DOMAIN.number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.CMDtcp_OR_SYSLOGudp
                                                                        .number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.HTTP
                                                                        .number()))))))))))))))));
    /* og-udp */
    assertThat(
        c,
        hasIpAccessList(
            computeServiceObjectGroupAclName("og-udp"),
            hasLines(
                containsInAnyOrder(
                    ImmutableList.of(
                        hasMatchCondition(
                            isOrMatchExprThat(
                                hasDisjuncts(
                                    containsInAnyOrder(
                                        ImmutableList.of(
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(new SubRange(65501)))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.NTP.number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.SNMPTRAP
                                                                        .number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.CMDtcp_OR_SYSLOGudp
                                                                        .number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.TFTP
                                                                        .number()))))))))))))))));
  }

  @Test
  public void testIosOspfNetwork() throws IOException {
    Configuration c = parseConfig("ios-interface-ospf-network");

    /*
     * Confirm interfaces with ospf network broadcast, non-broadcast, etc do not show up as
     * point-to-point
     */
    assertThat(c, hasInterface("Ethernet0/0", not(isOspfPointToPoint())));
    assertThat(c, hasInterface("Ethernet0/2", not(isOspfPointToPoint())));
    assertThat(c, hasInterface("Ethernet0/3", not(isOspfPointToPoint())));
    assertThat(c, hasInterface("Ethernet0/4", not(isOspfPointToPoint())));

    /* Confirm the point-to-point interface shows up as such */
    assertThat(c, hasInterface("Ethernet0/1", isOspfPointToPoint()));
  }

  @Test
  public void testIosOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("iosOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(10e6d));

    Configuration defaults = parseConfig("iosOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_IOS)));
  }

  @Test
  public void testIosOspfStubSettings() throws IOException {
    Configuration c = parseConfig("ios-ospf-stub-settings");

    // Check correct stub types are assigned
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(0L, hasStubType(StubType.NONE)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(1L, hasStubType(StubType.NSSA)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(2L, hasStubType(StubType.NSSA)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(3L, hasStubType(StubType.STUB)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(4L, hasStubType(StubType.STUB)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasArea(5L, hasStubType(StubType.NONE)))));

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
  }

  @Test
  public void testIosPrefixList() throws IOException {
    String hostname = "ios-prefix-list";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm prefix list uses are counted correctly */
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "pre_list", 2));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "pre_list_unused", 0));

    /* Confirm undefined prefix lists are detected in different contexts */
    /* Bgp neighbor context */
    assertThat(ccae, hasUndefinedReference(filename, PREFIX_LIST, "pre_list_undef1"));
    /* Route-map match context */
    assertThat(ccae, hasUndefinedReference(filename, PREFIX_LIST, "pre_list_undef2"));
  }

  @Test
  public void testIosRouteMap() throws IOException {
    String hostname = "ios-route-map";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm route map uses are counted correctly */
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rm_if", 1));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rm_ospf", 4));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rm_bgp", 9));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rm_unused", 0));

    /* Confirm undefined route-map is detected */
    assertThat(ccae, hasUndefinedReference(filename, ROUTE_MAP, "rm_undef"));
  }

  @Test
  public void testIosSnmpCommunityString() throws IOException {
    String hostname = "ios-snmp-community-string";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    Configuration c = batfish.loadConfigurations().get(hostname);

    /* Confirm community strings are correctly parsed */
    assertThat(c, hasDefaultVrf(hasSnmpServer(hasCommunities(hasKey("test$#!@")))));
    assertThat(c, hasDefaultVrf(hasSnmpServer(hasCommunities(hasKey("quoted$#!@")))));

    /* Confirm ACL is referenced */
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "80", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "90", 1));
  }

  @Test
  public void testIosClassMapInspect() throws IOException {
    String hostname = "ios-class-map-inspect";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * We expected the only unused acl to be aclunused
     */
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "acldefined", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "aclunused", 0));

    /*
     * We expect an undefined reference only to aclundefined
     */
    assertThat(ccae, not(hasUndefinedReference(filename, IP_ACCESS_LIST, "acldefined")));
    assertThat(ccae, hasUndefinedReference(filename, IP_ACCESS_LIST, "aclundefined"));
  }

  @Test
  public void testIosPolicyMapInspect() throws IOException {
    String hostname = "ios-policy-map-inspect";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * We expected the only unused class-map to be cmunused
     */
    assertThat(ccae, hasNumReferrers(filename, INSPECT_CLASS_MAP, "cmdefined", 1));
    assertThat(ccae, hasNumReferrers(filename, INSPECT_CLASS_MAP, "cmunused", 0));

    /*
     * We expect the only unused policy-map to be pmiunused
     */
    assertThat(ccae, hasNumReferrers(filename, INSPECT_POLICY_MAP, "pmidefined", 1));
    assertThat(ccae, hasNumReferrers(filename, INSPECT_POLICY_MAP, "pmiunused", 0));

    /*
     * We expect undefined references only to cmundefined and pmmiundefined
     */
    assertThat(ccae, not(hasUndefinedReference(filename, INSPECT_CLASS_MAP, "cmdefined")));
    assertThat(ccae, hasUndefinedReference(filename, INSPECT_CLASS_MAP, "cmundefined"));
    assertThat(ccae, not(hasUndefinedReference(filename, INSPECT_POLICY_MAP, "pmidefined")));
    assertThat(ccae, hasUndefinedReference(filename, INSPECT_POLICY_MAP, "pmiundefined"));
  }

  @Test
  public void testIosPrefixSet() throws IOException {
    String hostname = "ios-prefix-set";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    Prefix permittedPrefix = Prefix.parse("1.2.3.4/30");
    Prefix6 permittedPrefix6 = new Prefix6("2001::ffff:0/124");
    Prefix rejectedPrefix = Prefix.parse("1.2.4.4/30");
    Prefix6 rejectedPrefix6 = new Prefix6("2001::fffe:0/124");

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
  }

  @Test
  public void testIosProxyArp() throws IOException {
    Configuration proxyArpOmitted = parseConfig("iosProxyArp");
    assertThat(proxyArpOmitted, hasInterfaces(hasEntry(equalTo("Ethernet0/0"), isProxyArp())));
    assertThat(proxyArpOmitted, hasInterfaces(hasEntry(equalTo("Ethernet0/1"), isProxyArp())));
    assertThat(
        proxyArpOmitted,
        hasInterfaces(hasEntry(equalTo("Ethernet0/2"), isProxyArp(equalTo(false)))));
  }

  @Test
  public void testIosZoneSecurity() throws IOException {
    String hostname = "ios-zone-security";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * We expected the only unused zone to be zunreferenced
     */
    assertThat(ccae, hasNumReferrers(filename, SECURITY_ZONE, "z1", 4));
    assertThat(ccae, hasNumReferrers(filename, SECURITY_ZONE, "z2", 2));
    assertThat(ccae, hasNumReferrers(filename, SECURITY_ZONE, "zempty", 1));
    assertThat(ccae, hasNumReferrers(filename, SECURITY_ZONE, "zunreferenced", 0));

    /*
     * We expect an undefined reference only to zundefined
     */
    assertThat(ccae, not(hasUndefinedReference(filename, SECURITY_ZONE, "z1")));
    assertThat(ccae, not(hasUndefinedReference(filename, SECURITY_ZONE, "z2")));
    assertThat(ccae, not(hasUndefinedReference(filename, SECURITY_ZONE, "zempty")));
    assertThat(ccae, hasUndefinedReference(filename, SECURITY_ZONE, "zundefined"));

    /*
     * We only expect zempty to be empty (have no interfaces)
     */
    assertThat(c, hasZone("z1", hasMemberInterfaces(not(empty()))));
    assertThat(c, hasZone("z2", hasMemberInterfaces(not(empty()))));
    assertThat(c, hasZone("zempty", hasMemberInterfaces(empty())));
  }

  @Test
  public void testNetworkObject() throws IOException {
    String hostname = "network-object";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    Ip on1Ip = new Ip("1.2.3.4");
    Ip on2IpStart = new Ip("2.2.2.0");
    Ip on2IpEnd = new Ip("2.2.2.255");
    Ip inlineIp = new Ip("3.3.3.3");

    /* Confirm network object IpSpaces cover the correct Ip addresses */
    assertThat(c, hasIpSpace("ON1", containsIp(on1Ip)));
    assertThat(c, hasIpSpace("ON1", not(containsIp(on2IpStart))));
    assertThat(c, hasIpSpace("ON2", containsIp(on2IpStart)));
    assertThat(c, hasIpSpace("ON2", containsIp(on2IpEnd)));
    assertThat(c, hasIpSpace("ON2", not(containsIp(on1Ip))));

    /* Confirm object-group also covers the IpSpaces its network objects cover */
    assertThat(c, hasIpSpace("OGN", containsIp(on1Ip, c.getIpSpaces())));
    assertThat(c, hasIpSpace("OGN", containsIp(inlineIp, c.getIpSpaces())));
    assertThat(c, hasIpSpace("OGN", not(containsIp(on2IpStart, c.getIpSpaces()))));

    /* Confirm network objects have the correct number of referrers */
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "ON1", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "ON2", 0));
    /* Confirm undefined reference shows up as such */
    assertThat(ccae, hasUndefinedReference(filename, NETWORK_OBJECT, "ON_UNDEFINED"));
  }

  @Test
  public void testOspfSummaryRouteMetric() throws IOException {
    Configuration manual = parseConfig("iosOspfCost");

    assertThat(
        manual,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(
                    1L, hasSummary(Prefix.parse("10.0.0.0/16"), isAdvertised(equalTo(false)))))));
    assertThat(
        manual,
        hasDefaultVrf(
            hasOspfProcess(hasArea(1L, hasSummary(Prefix.parse("10.0.0.0/16"), hasMetric(100L))))));

    Configuration defaults = parseConfig("iosOspfCostDefaults");

    assertThat(
        defaults,
        hasDefaultVrf(
            hasOspfProcess(hasArea(1L, hasSummary(Prefix.parse("10.0.0.0/16"), isAdvertised())))));
    assertThat(
        defaults,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(1L, hasSummary(Prefix.parse("10.0.0.0/16"), hasMetric(nullValue()))))));
  }

  @Test
  public void testIosXePolicyMapClassDefault() throws IOException {
    Configuration c = parseConfig("ios-xe-policy-map-class-default");

    String dropPolicyMapAclName = computeInspectPolicyMapAclName("pdrop");
    String passPolicyMapAclName = computeInspectPolicyMapAclName("ppass");
    String unspecifiedPolicyMapAclName = computeInspectPolicyMapAclName("punspecified");

    Flow flow = Flow.builder().setTag("").setIngressNode("").build();

    assertThat(c, hasIpAccessList(dropPolicyMapAclName, rejects(flow, null, c)));
    assertThat(c, hasIpAccessList(passPolicyMapAclName, accepts(flow, null, c)));
    assertThat(c, hasIpAccessList(unspecifiedPolicyMapAclName, rejects(flow, null, c)));
  }

  @Test
  public void testIosXePolicyMapInspectClassInspectActions() throws IOException {
    Configuration c = parseConfig("ios-xe-policy-map-inspect-class-inspect-actions");

    String policyMapName = "pm";
    String policyMapAclName = computeInspectPolicyMapAclName(policyMapName);

    String classMapPassName = "cpass";
    String classMapPassAclName = computeInspectClassMapAclName(classMapPassName);
    String classMapInspectName = "cinspect";
    String classMapInspectAclName = computeInspectClassMapAclName(classMapInspectName);
    String classMapDropName = "cdrop";
    String classMapDropAclName = computeInspectClassMapAclName(classMapDropName);

    Flow flowPass =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setTag("")
            .setIpProtocol(IpProtocol.TCP)
            .build();
    Flow flowInspect =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setTag("")
            .setIpProtocol(IpProtocol.UDP)
            .build();
    Flow flowDrop =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setTag("")
            .setIpProtocol(IpProtocol.ICMP)
            .build();

    assertThat(c, hasIpAccessList(policyMapAclName, accepts(flowPass, null, c)));
    assertThat(c, hasIpAccessList(policyMapAclName, accepts(flowInspect, null, c)));
    assertThat(c, hasIpAccessList(policyMapAclName, rejects(flowDrop, null, c)));

    assertThat(c, hasIpAccessList(classMapPassAclName, accepts(flowPass, null, c)));
    assertThat(c, hasIpAccessList(classMapPassAclName, rejects(flowInspect, null, c)));
    assertThat(c, hasIpAccessList(classMapPassAclName, rejects(flowDrop, null, c)));

    assertThat(c, hasIpAccessList(classMapInspectAclName, rejects(flowPass, null, c)));
    assertThat(c, hasIpAccessList(classMapInspectAclName, accepts(flowInspect, null, c)));
    assertThat(c, hasIpAccessList(classMapInspectAclName, rejects(flowDrop, null, c)));

    assertThat(c, hasIpAccessList(classMapDropAclName, rejects(flowPass, null, c)));
    assertThat(c, hasIpAccessList(classMapDropAclName, rejects(flowInspect, null, c)));
    assertThat(c, hasIpAccessList(classMapDropAclName, accepts(flowDrop, null, c)));
  }

  @Test
  public void testIosXeZoneDefaultBehavior() throws IOException {
    Configuration c = parseConfig("ios-xe-zone-default-behavior");

    /* Ethernet1 and Ethernet2 are in zone z12 */
    String e1Name = "Ethernet1";
    String e2Name = "Ethernet2";

    /* Ethernet3 is in zone z3 */
    String e3Name = "Ethernet3";

    Flow flow = Flow.builder().setIngressNode(c.getHostname()).setTag("").build();

    /* Traffic originating from device should not be subject to zone filtering */
    assertThat(c, hasInterface(e1Name, hasOutgoingFilter(accepts(flow, null, c))));
    assertThat(c, hasInterface(e2Name, hasOutgoingFilter(accepts(flow, null, c))));
    assertThat(c, hasInterface(e3Name, hasOutgoingFilter(accepts(flow, null, c))));

    /* Traffic with src and dst interface in same zone should be permitted by default */
    assertThat(c, hasInterface(e1Name, hasOutgoingFilter(accepts(flow, e1Name, c))));
    assertThat(c, hasInterface(e1Name, hasOutgoingFilter(accepts(flow, e2Name, c))));
    assertThat(c, hasInterface(e2Name, hasOutgoingFilter(accepts(flow, e1Name, c))));
    assertThat(c, hasInterface(e2Name, hasOutgoingFilter(accepts(flow, e2Name, c))));
    assertThat(c, hasInterface(e3Name, hasOutgoingFilter(accepts(flow, e3Name, c))));

    /* Traffic crossing zones should be blocked by default */
    assertThat(c, hasInterface(e1Name, hasOutgoingFilter(rejects(flow, e3Name, c))));
    assertThat(c, hasInterface(e2Name, hasOutgoingFilter(rejects(flow, e3Name, c))));
    assertThat(c, hasInterface(e3Name, hasOutgoingFilter(rejects(flow, e1Name, c))));
    assertThat(c, hasInterface(e3Name, hasOutgoingFilter(rejects(flow, e2Name, c))));
  }

  @Test
  public void testIosXrOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("iosxrOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(10e6d));

    Configuration defaults = parseConfig("iosxrOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_IOS_XR)));
  }

  @Test
  public void testNxosOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("nxosOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(10e9d));

    Configuration defaults = parseConfig("nxosOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_NX)));
  }

  @Test
  public void testNxosVrfContext() throws IOException {
    Configuration vrfC = parseConfig("nxos-vrf-context");
    assertThat(
        vrfC,
        ConfigurationMatchers.hasVrf(
            "management",
            hasStaticRoutes(
                equalTo(
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNetwork(Prefix.ZERO)
                            .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
                            .setAdministrativeCost(1)
                            .build())))));
  }

  @Test
  public void testBgpLocalAs() throws IOException {
    String testrigName = "bgp-local-as";
    List<String> configurationNames = ImmutableList.of("r1", "r2");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        CommonUtil.initBgpTopology(configurations, ipOwners, false);

    // Edge one direction
    assertThat(
        bgpTopology
            .adjacentNodes(
                new BgpPeerConfigId("r1", DEFAULT_VRF_NAME, Prefix.parse("1.2.0.2/32"), false))
            .iterator()
            .next(),
        equalTo(new BgpPeerConfigId("r2", DEFAULT_VRF_NAME, Prefix.parse("1.2.0.1/32"), false)));

    // Edge the other direction
    assertThat(
        bgpTopology
            .adjacentNodes(
                new BgpPeerConfigId("r2", DEFAULT_VRF_NAME, Prefix.parse("1.2.0.1/32"), false))
            .iterator()
            .next(),
        equalTo(new BgpPeerConfigId("r1", DEFAULT_VRF_NAME, Prefix.parse("1.2.0.2/32"), false)));
  }

  @Test
  public void testBgpMultipathRelax() throws IOException {
    String testrigName = "bgp-multipath-relax";
    List<String> configurationNames =
        ImmutableList.of("arista_disabled", "arista_enabled", "nxos_disabled", "nxos_enabled");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    CommonUtil.initBgpTopology(configurations, ipOwners, false);
    org.batfish.datamodel.BgpProcess aristaDisabled =
        configurations.get("arista_disabled").getDefaultVrf().getBgpProcess();
    org.batfish.datamodel.BgpProcess aristaEnabled =
        configurations.get("arista_enabled").getDefaultVrf().getBgpProcess();
    org.batfish.datamodel.BgpProcess nxosDisabled =
        configurations.get("nxos_disabled").getDefaultVrf().getBgpProcess();
    org.batfish.datamodel.BgpProcess nxosEnabled =
        configurations.get("nxos_enabled").getDefaultVrf().getBgpProcess();

    assertThat(
        aristaDisabled,
        hasMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.EXACT_PATH));
    assertThat(
        aristaEnabled,
        hasMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
    assertThat(
        nxosDisabled,
        hasMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.EXACT_PATH));
    assertThat(
        nxosEnabled,
        hasMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));

    assertThat(aristaDisabled, hasMultipathEbgp(false));
    assertThat(aristaEnabled, hasMultipathEbgp(true));
    assertThat(nxosDisabled, hasMultipathEbgp(false));
    assertThat(nxosEnabled, hasMultipathEbgp(true));
  }

  @Test
  public void testBgpOriginationSpace() throws IOException {
    Configuration c = parseConfig("ios-bgp-origination-space");

    assertThat(
        c.getVrfs().get(DEFAULT_VRF_NAME).getBgpProcess().getOriginationSpace(),
        equalTo(
            new PrefixSpace(
                PrefixRange.fromPrefix(Prefix.parse("1.1.1.1/32")),
                PrefixRange.fromPrefix(Prefix.parse("1.1.2.0/24")))));
  }

  @Test
  public void testBgpRemovePrivateAs() throws IOException {
    String testrigName = "bgp-remove-private-as";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    CommonUtil.initBgpTopology(configurations, ipOwners, false);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    batfish.computeDataPlane(false); // compute and cache the dataPlane

    // Check that 1.1.1.1/32 appears on r3
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes(batfish.loadDataPlane());
    SortedSet<AbstractRoute> r3Routes = routes.get("r3").get(DEFAULT_VRF_NAME);
    Set<Prefix> r3Prefixes =
        r3Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Prefix r1Loopback = Prefix.parse("1.1.1.1/32");
    assertTrue(r3Prefixes.contains(r1Loopback));

    // check that private AS is present in path in received 1.1.1.1/32 advert on r2
    batfish.initBgpAdvertisements(configurations);
    Configuration r2 = configurations.get("r2");
    boolean r2HasPrivate =
        r2.getReceivedEbgpAdvertisements()
            .stream()
            .filter(a -> a.getNetwork().equals(r1Loopback))
            .toArray(BgpAdvertisement[]::new)[0]
            .getAsPath()
            .getAsSets()
            .stream()
            .flatMap(Collection::stream)
            .anyMatch(AsPath::isPrivateAs);
    assertTrue(r2HasPrivate);

    // check that private AS is absent from path in received 1.1.1.1/32 advert on r3
    Configuration r3 = configurations.get("r3");
    boolean r3HasPrivate =
        r3.getReceivedEbgpAdvertisements()
            .stream()
            .filter(a -> a.getNetwork().equals(r1Loopback))
            .toArray(BgpAdvertisement[]::new)[0]
            .getAsPath()
            .getAsSets()
            .stream()
            .flatMap(Collection::stream)
            .anyMatch(AsPath::isPrivateAs);
    assertFalse(r3HasPrivate);
  }

  @Test
  public void testCommunityListConversion() throws IOException {
    String testrigName = "community-list-conversion";
    String iosName = "ios";
    String nxosName = "nxos";
    String eosName = "eos";
    List<String> configurationNames = ImmutableList.of(iosName, nxosName, eosName);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosCommunityListConfig = configurations.get(iosName);
    SortedMap<String, CommunityList> iosCommunityLists = iosCommunityListConfig.getCommunityLists();

    Configuration eosCommunityListConfig = configurations.get(eosName);
    SortedMap<String, CommunityList> eosCommunityLists = eosCommunityListConfig.getCommunityLists();

    Configuration nxosCommunityListConfig = configurations.get(nxosName);
    SortedMap<String, CommunityList> nxosCommunityLists =
        nxosCommunityListConfig.getCommunityLists();

    String iosRegexImpliedStd = getCLRegex(iosCommunityLists, "40");
    String iosRegexImpliedExp = getCLRegex(iosCommunityLists, "400");
    String iosRegexStd = getCLRegex(iosCommunityLists, "std_community");
    String iosRegexExp = getCLRegex(iosCommunityLists, "exp_community");
    String iosRegexStdAsnn = getCLRegex(iosCommunityLists, "std_as_nn");
    String iosRegexExpAsnn = getCLRegex(iosCommunityLists, "exp_as_nn");
    String iosRegexStdGshut = getCLRegex(iosCommunityLists, "std_gshut");
    String iosRegexExpGshut = getCLRegex(iosCommunityLists, "exp_gshut");
    String iosRegexStdInternet = getCLRegex(iosCommunityLists, "std_internet");
    String iosRegexExpInternet = getCLRegex(iosCommunityLists, "exp_internet");
    String iosRegexStdLocalAs = getCLRegex(iosCommunityLists, "std_local_AS");
    String iosRegexExpLocalAs = getCLRegex(iosCommunityLists, "exp_local_AS");
    String iosRegexStdNoAdv = getCLRegex(iosCommunityLists, "std_no_advertise");
    String iosRegexExpNoAdv = getCLRegex(iosCommunityLists, "exp_no_advertise");
    String iosRegexStdNoExport = getCLRegex(iosCommunityLists, "std_no_export");
    String iosRegexExpNoExport = getCLRegex(iosCommunityLists, "exp_no_export");

    String eosRegexStd = getCLRegex(eosCommunityLists, "eos_std");
    String eosRegexExp = getCLRegex(eosCommunityLists, "eos_exp");
    String eosRegexStdGshut = getCLRegex(eosCommunityLists, "eos_std_gshut");
    String eosRegexStdInternet = getCLRegex(eosCommunityLists, "eos_std_internet");
    String eosRegexStdLocalAs = getCLRegex(eosCommunityLists, "eos_std_local_AS");
    String eosRegexStdNoAdv = getCLRegex(eosCommunityLists, "eos_std_no_adv");
    String eosRegexStdNoExport = getCLRegex(eosCommunityLists, "eos_std_no_export");
    String eosRegexStdMulti = getCLRegex(eosCommunityLists, "eos_std_multi");
    String eosRegexExpMulti = getCLRegex(eosCommunityLists, "eos_exp_multi");

    String nxosRegexStd = getCLRegex(nxosCommunityLists, "nxos_std");
    String nxosRegexExp = getCLRegex(nxosCommunityLists, "nxos_exp");
    String nxosRegexStdInternet = getCLRegex(nxosCommunityLists, "nxos_std_internet");
    String nxosRegexStdLocalAs = getCLRegex(nxosCommunityLists, "nxos_std_local_AS");
    String nxosRegexStdNoAdv = getCLRegex(nxosCommunityLists, "nxos_std_no_adv");
    String nxosRegexStdNoExport = getCLRegex(nxosCommunityLists, "nxos_std_no_export");
    String nxosRegexStdMulti = getCLRegex(nxosCommunityLists, "nxos_std_multi");
    String nxosRegexExpMulti = getCLRegex(nxosCommunityLists, "nxos_exp_multi");

    // Check well known community regexes are generated properly
    String regexInternet = "^" + CommonUtil.longToCommunity(WellKnownCommunity.INTERNET) + "$";
    String regexNoAdv = "^" + CommonUtil.longToCommunity(WellKnownCommunity.NO_ADVERTISE) + "$";
    String regexNoExport = "^" + CommonUtil.longToCommunity(WellKnownCommunity.NO_EXPORT) + "$";
    String regexGshut =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.GRACEFUL_SHUTDOWN) + "$";
    String regexLocalAs =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.NO_EXPORT_SUBCONFED) + "$";
    assertThat(iosRegexStdInternet, equalTo(regexInternet));
    assertThat(iosRegexStdNoAdv, equalTo(regexNoAdv));
    assertThat(iosRegexStdNoExport, equalTo(regexNoExport));
    assertThat(iosRegexStdGshut, equalTo(regexGshut));
    assertThat(iosRegexStdLocalAs, equalTo(regexLocalAs));
    assertThat(eosRegexStdInternet, equalTo(regexInternet));
    assertThat(eosRegexStdNoAdv, equalTo(regexNoAdv));
    assertThat(eosRegexStdNoExport, equalTo(regexNoExport));
    assertThat(eosRegexStdGshut, equalTo(regexGshut));
    assertThat(eosRegexStdLocalAs, equalTo(regexLocalAs));
    // NX-OS does not support gshut
    assertThat(nxosRegexStdInternet, equalTo(regexInternet));
    assertThat(nxosRegexStdNoAdv, equalTo(regexNoAdv));
    assertThat(nxosRegexStdNoExport, equalTo(regexNoExport));
    assertThat(nxosRegexStdLocalAs, equalTo(regexLocalAs));

    // Confirm for the same literal communities, standard and expanded regexs are different
    assertThat(iosRegexImpliedStd, not(equalTo(iosRegexImpliedExp)));
    assertThat(iosRegexStd, not(equalTo(iosRegexExp)));
    assertThat(iosRegexStdAsnn, not(equalTo(iosRegexExpAsnn)));
    assertThat(iosRegexStdInternet, not(equalTo(iosRegexExpInternet)));
    assertThat(iosRegexStdNoAdv, not(equalTo(iosRegexExpNoAdv)));
    assertThat(iosRegexStdNoExport, not(equalTo(iosRegexExpNoExport)));
    assertThat(iosRegexStdGshut, not(equalTo(iosRegexExpGshut)));
    assertThat(iosRegexStdLocalAs, not(equalTo(iosRegexExpLocalAs)));
    assertThat(eosRegexStd, not(equalTo(eosRegexExp)));
    assertThat(eosRegexStdMulti, not(equalTo(eosRegexExpMulti)));
    assertThat(nxosRegexStd, not(equalTo(nxosRegexExp)));
    assertThat(nxosRegexStdMulti, not(equalTo(nxosRegexExpMulti)));
  }

  @Test
  public void testToIpsecPolicies() throws IOException {
    Configuration c = parseConfig("ios-crypto-map");

    assertThat(
        c,
        hasIpsecPolicy(
            "mymap:10",
            allOf(
                IpsecPolicyMatchers.hasIkeGateway(IkeGatewayMatchers.hasName("ISAKMP-PROFILE")),
                hasPfsKeyGroup(DiffieHellmanGroup.GROUP14),
                hasIpsecProposals(
                    contains(ImmutableList.of(IpsecProposalMatchers.hasName("ts1")))))));

    assertThat(
        c,
        hasIpsecPolicy(
            "mymap:30:5",
            hasIpsecProposals(contains(ImmutableList.of(IpsecProposalMatchers.hasName("ts1"))))));

    assertThat(
        c,
        hasIpsecPolicy(
            "mymap:30:15",
            hasIpsecProposals(contains(ImmutableList.of(IpsecProposalMatchers.hasName("ts2"))))));

    // tests for IPSec phase 2 policies conversion
    assertThat(
        c,
        hasIpsecPhase2Policy(
            "IPSEC-PROFILE1",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(
                    equalTo(ImmutableList.of("ts1", "ts2"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP14)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "~IPSEC_PHASE2_POLICY:mymap:10~",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of("ts1"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP14)))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "~IPSEC_PHASE2_POLICY:mymap:30:15~",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of("ts2"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(nullValue()))));

    assertThat(
        c,
        hasIpsecPhase2Policy(
            "~IPSEC_PHASE2_POLICY:mymap:30:5~",
            allOf(
                IpsecPhase2PolicyMatchers.hasIpsecProposals(equalTo(ImmutableList.of("ts1"))),
                IpsecPhase2PolicyMatchers.hasPfsKeyGroup(equalTo(DiffieHellmanGroup.GROUP2)))));
  }

  @Test
  public void testCryptoMapsAndTunnelsToIpsecPeerConfigs() throws IOException {
    Configuration c = parseConfig("ios-crypto-map");

    List<IpAccessListLine> expectedAclLines =
        ImmutableList.of(
            IpAccessListLine.accepting()
                .setName("permit ip 1.1.1.1 0.0.0.0 2.2.2.2 0.0.0.0")
                .setMatchCondition(
                    new MatchHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(new IpWildcard("1.1.1.1").toIpSpace())
                            .setDstIps(new IpWildcard("2.2.2.2").toIpSpace())
                            .build()))
                .build(),
            IpAccessListLine.accepting()
                .setMatchCondition(
                    new MatchHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(new IpWildcard("2.2.2.2").toIpSpace())
                            .setDstIps(new IpWildcard("1.1.1.1").toIpSpace())
                            .build()))
                .build());

    assertThat(
        c,
        hasIpsecPeerConfig(
            "~IPSEC_PEER_CONFIG:mymap:20_TenGigabitEthernet0/0~",
            isIpsecStaticPeerConfigThat(
                allOf(
                    hasDestinationAddress(new Ip("3.4.5.6")),
                    IpsecPeerConfigMatchers.hasIkePhase1Policy("ISAKMP-PROFILE-MATCHED"),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("~IPSEC_PHASE2_POLICY:mymap:20~"),
                    hasPhysicalInterface("TenGigabitEthernet0/0"),
                    hasPolicyAccessList(hasLines(equalTo(expectedAclLines))),
                    hasLocalAddress(new Ip("2.3.4.6"))))));
    assertThat(
        c,
        hasIpsecPeerConfig(
            "~IPSEC_PEER_CONFIG:mymap:10_TenGigabitEthernet0/0~",
            isIpsecStaticPeerConfigThat(
                allOf(
                    hasDestinationAddress(new Ip("1.2.3.4")),
                    IpsecPeerConfigMatchers.hasIkePhase1Policy("ISAKMP-PROFILE"),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("~IPSEC_PHASE2_POLICY:mymap:10~"),
                    hasPhysicalInterface("TenGigabitEthernet0/0"),
                    hasPolicyAccessList(hasLines(equalTo(expectedAclLines))),
                    hasLocalAddress(new Ip("2.3.4.6"))))));

    assertThat(
        c,
        hasIpsecPeerConfig(
            "~IPSEC_PEER_CONFIG:mymap:30:15_TenGigabitEthernet0/0~",
            isIpsecDynamicPeerConfigThat(
                allOf(
                    IpsecPeerConfigMatchers.hasIkePhase1Policies(
                        equalTo(ImmutableList.of("ISAKMP-PROFILE", "ISAKMP-PROFILE-MATCHED"))),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("~IPSEC_PHASE2_POLICY:mymap:30:15~"),
                    hasPhysicalInterface("TenGigabitEthernet0/0"),
                    hasPolicyAccessList(hasLines(equalTo(expectedAclLines))),
                    hasLocalAddress(new Ip("2.3.4.6")),
                    hasTunnelInterface(nullValue())))));

    assertThat(
        c,
        hasIpsecPeerConfig(
            "~IPSEC_PEER_CONFIG:mymap:30:5_TenGigabitEthernet0/0~",
            isIpsecDynamicPeerConfigThat(
                allOf(
                    IpsecPeerConfigMatchers.hasIkePhase1Policies(
                        equalTo(ImmutableList.of("ISAKMP-PROFILE", "ISAKMP-PROFILE-MATCHED"))),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("~IPSEC_PHASE2_POLICY:mymap:30:5~"),
                    hasPhysicalInterface("TenGigabitEthernet0/0"),
                    hasPolicyAccessList(hasLines(equalTo(expectedAclLines))),
                    hasLocalAddress(new Ip("2.3.4.6")),
                    hasTunnelInterface(nullValue())))));

    assertThat(
        c,
        hasIpsecPeerConfig(
            "Tunnel1",
            isIpsecStaticPeerConfigThat(
                allOf(
                    hasDestinationAddress(new Ip("1.2.3.4")),
                    IpsecPeerConfigMatchers.hasIkePhase1Policy("ISAKMP-PROFILE"),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("IPSEC-PROFILE1"),
                    hasPhysicalInterface("TenGigabitEthernet0/0"),
                    hasLocalAddress(new Ip("2.3.4.6")),
                    hasTunnelInterface(equalTo("Tunnel1"))))));
  }

  @Test
  public void testCryptoMapsToIpsecVpns() throws IOException {
    Configuration c = parseConfig("ios-crypto-map");

    List<IpAccessListLine> expectedAclLines =
        ImmutableList.of(
            IpAccessListLine.accepting()
                .setName("permit ip 1.1.1.1 0.0.0.0 2.2.2.2 0.0.0.0")
                .setMatchCondition(
                    new MatchHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(new IpWildcard("1.1.1.1").toIpSpace())
                            .setDstIps(new IpWildcard("2.2.2.2").toIpSpace())
                            .build()))
                .build(),
            IpAccessListLine.accepting()
                .setMatchCondition(
                    new MatchHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(new IpWildcard("2.2.2.2").toIpSpace())
                            .setDstIps(new IpWildcard("1.1.1.1").toIpSpace())
                            .build()))
                .build());
    assertThat(
        c,
        hasIpsecVpn(
            "mymap:10:TenGigabitEthernet0/0",
            allOf(
                hasBindInterface(InterfaceMatchers.hasName("TenGigabitEthernet0/0")),
                IpsecVpnMatchers.hasIpsecPolicy(IpsecPolicyMatchers.hasName("mymap:10")),
                hasIkeGatewaay(IkeGatewayMatchers.hasName("ISAKMP-PROFILE")),
                hasPolicy(hasLines(equalTo(expectedAclLines))))));
    assertThat(
        c,
        hasIpsecVpn(
            "mymap:30:5:TenGigabitEthernet0/0",
            allOf(
                hasBindInterface(InterfaceMatchers.hasName("TenGigabitEthernet0/0")),
                IpsecVpnMatchers.hasIpsecPolicy(IpsecPolicyMatchers.hasName("mymap:30:5")),
                hasPolicy(hasLines(equalTo(expectedAclLines))))));
    assertThat(
        c,
        hasIpsecVpn(
            "mymap:30:15:TenGigabitEthernet0/0",
            allOf(
                hasBindInterface(InterfaceMatchers.hasName("TenGigabitEthernet0/0")),
                IpsecVpnMatchers.hasIpsecPolicy(IpsecPolicyMatchers.hasName("mymap:30:15")),
                hasPolicy(hasLines(equalTo(expectedAclLines))))));

    assertThat(c, hasInterface("TenGigabitEthernet0/0", isActive()));
  }

  @Test
  public void testInvalidCryptoMapDef() throws IOException {
    String hostname = "ios-crypto-map";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                "Interface TenGigabitEthernet0/1 with declared crypto-map mymap has no ip-address")));
  }

  @Test
  public void testIsakmpPolicyAruba() throws IOException {
    Configuration c = parseConfig("arubaCrypto");
    assertThat(
        c,
        hasIkeProposal(
            "20",
            allOf(
                hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC),
                hasAuthenticationMethod(IkeAuthenticationMethod.RSA_SIGNATURES),
                hasAuthenticationAlgorithm(IkeHashingAlgorithm.SHA_256),
                hasDiffieHellmanGroup(DiffieHellmanGroup.GROUP19),
                hasLifeTimeSeconds(86400))));
    // asserting the default values being set
    assertThat(
        c,
        hasIkeProposal(
            "30",
            allOf(
                hasEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC),
                hasAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS),
                hasAuthenticationAlgorithm(IkeHashingAlgorithm.SHA1),
                hasDiffieHellmanGroup(DiffieHellmanGroup.GROUP1),
                hasLifeTimeSeconds(86400))));
  }

  @Test
  public void testIsakmpPolicyIos() throws IOException {
    Configuration c = parseConfig("ios-crypto");

    assertThat(
        c,
        hasIkeProposal(
            "10",
            allOf(
                hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC),
                hasAuthenticationMethod(IkeAuthenticationMethod.RSA_SIGNATURES),
                hasAuthenticationAlgorithm(IkeHashingAlgorithm.MD5),
                hasDiffieHellmanGroup(DiffieHellmanGroup.GROUP1),
                hasLifeTimeSeconds(14400))));

    // asserting the default values being set
    assertThat(
        c,
        hasIkeProposal(
            "20",
            allOf(
                hasEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC),
                hasAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS),
                hasAuthenticationAlgorithm(IkeHashingAlgorithm.SHA1),
                hasDiffieHellmanGroup(DiffieHellmanGroup.GROUP2),
                hasLifeTimeSeconds(86400))));

    // test for IKE phase1 proposals
    assertThat(
        c,
        hasIkePhase1Proposal(
            "10",
            allOf(
                IkePhase1ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC),
                IkePhase1ProposalMatchers.hasAuthenticationMethod(
                    IkeAuthenticationMethod.RSA_SIGNATURES),
                IkePhase1ProposalMatchers.hasHashingAlgorithm(IkeHashingAlgorithm.MD5),
                IkePhase1ProposalMatchers.hasDiffieHellmanGroup(DiffieHellmanGroup.GROUP1),
                IkePhase1ProposalMatchers.hasLifeTimeSeconds(14400))));

    assertThat(
        c,
        hasIkePhase1Proposal(
            "20",
            allOf(
                IkePhase1ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC),
                IkePhase1ProposalMatchers.hasAuthenticationMethod(
                    IkeAuthenticationMethod.PRE_SHARED_KEYS),
                IkePhase1ProposalMatchers.hasHashingAlgorithm(IkeHashingAlgorithm.SHA1),
                IkePhase1ProposalMatchers.hasDiffieHellmanGroup(DiffieHellmanGroup.GROUP2),
                IkePhase1ProposalMatchers.hasLifeTimeSeconds(86400))));
  }

  @Test
  public void testIsakmpProfile() throws IOException {
    Configuration c = parseConfig("ios-crypto");
    assertThat(
        c,
        hasIkeGateway(
            "ISAKMP-PROFILE-ADDRESS",
            allOf(
                hasAddress(new Ip("1.2.3.4")),
                hasExternalInterface(InterfaceMatchers.hasName("TenGigabitEthernet0/0")),
                hasLocalIp(new Ip("2.3.4.6")),
                hasIkePolicy(
                    hasPresharedKeyHash(CommonUtil.sha256Digest("psk1" + CommonUtil.salt()))))));

    // The interface in the local-address should also be mapped with the same local ip
    assertThat(
        c,
        hasIkeGateway(
            "ISAKMP-PROFILE-INTERFACE",
            allOf(
                hasAddress(new Ip("1.2.3.4")),
                hasExternalInterface(InterfaceMatchers.hasName("TenGigabitEthernet0/0")),
                hasLocalIp(new Ip("2.3.4.6")),
                hasIkePolicy(
                    hasPresharedKeyHash(CommonUtil.sha256Digest("psk1" + CommonUtil.salt()))))));

    // test for IKE phase 1 policy
    assertThat(
        c,
        hasIkePhase1Policy(
            "ISAKMP-PROFILE-ADDRESS",
            allOf(
                hasIkePhase1Key(
                    IkePhase1KeyMatchers.hasKeyHash(
                        CommonUtil.sha256Digest("psk1" + CommonUtil.salt()))),
                hasRemoteIdentity(containsIp(new Ip("1.2.3.4"))),
                hasSelfIdentity(equalTo(new Ip("2.3.4.6"))),
                hasLocalInterface(equalTo("TenGigabitEthernet0/0")),
                hasIkePhase1Proposals(equalTo(ImmutableList.of("10", "20"))))));

    assertThat(
        c,
        hasIkePhase1Policy(
            "ISAKMP-PROFILE-INTERFACE",
            allOf(
                hasIkePhase1Key(
                    IkePhase1KeyMatchers.hasKeyHash(
                        CommonUtil.sha256Digest("psk1" + CommonUtil.salt()))),
                hasRemoteIdentity(containsIp(new Ip("1.2.3.4"))),
                hasSelfIdentity(equalTo(new Ip("2.3.4.6"))),
                hasLocalInterface(equalTo("TenGigabitEthernet0/0")),
                hasIkePhase1Proposals(equalTo(ImmutableList.of("10", "20"))))));
  }

  private static String getCLRegex(
      SortedMap<String, CommunityList> communityLists, String communityName) {
    return communityLists.get(communityName).getLines().get(0).getRegex();
  }

  @Test
  public void testEosBgpPeers() throws IOException {
    String hostname = "eos-bgp-peers";
    Prefix neighborWithRemoteAs = Prefix.parse("1.1.1.1/32");
    Prefix neighborWithoutRemoteAs = Prefix.parse("2.2.2.2/32");

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * The peer with a remote-as should appear in the datamodel. The peer without a remote-as
     * should not appear, and there should be a warning about the missing remote-as.
     */
    assertThat(
        c, hasDefaultVrf(hasBgpProcess(hasActiveNeighbor(neighborWithRemoteAs, hasRemoteAs(1L)))));
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasNeighbors(not(hasKey(neighborWithoutRemoteAs))))));
    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                String.format(
                    "No remote-as set for peer: %s", neighborWithoutRemoteAs.getStartIp()))));

    /*
     * Also ensure that default value of allowRemoteAsOut is true.
     */
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(hasActiveNeighbor(neighborWithRemoteAs, hasAllowRemoteAsOut(true)))));
  }

  @Test
  public void testEosPortChannel() throws IOException {
    String hostname = "eos-port-channel";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);

    assertThat(c, hasInterface("Ethernet0", hasBandwidth(40E9D)));
    assertThat(c, hasInterface("Ethernet1", hasBandwidth(40E9D)));
    assertThat(c, hasInterface("Ethernet2", hasBandwidth(40E9D)));
    assertThat(c, hasInterface("Port-Channel1", hasBandwidth(80E9D)));
    assertThat(c, hasInterface("Port-Channel2", isActive(false)));
  }

  @Test
  public void testInterfaceNames() throws IOException {
    String testrigName = "interface-names";
    String iosHostname = "ios";
    String i1Name = "Ethernet0/0";

    List<String> configurationNames = ImmutableList.of(iosHostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Interface i1 = configurations.get(iosHostname).getInterfaces().get(i1Name);
    assertThat(i1, hasDeclaredNames("Ethernet0/0", "e0/0", "Eth0/0", "ether0/0-1"));
  }

  @Test
  public void testIpsecProfile() throws IOException {
    Configuration c = parseConfig("ios-crypto-ipsec-profile");

    assertThat(
        c,
        hasIpsecPolicy(
            "IPSEC-PROFILE1",
            allOf(
                IpsecPolicyMatchers.hasIkeGateway(
                    allOf(hasAddress(new Ip("1.2.3.4")), hasLocalIp(new Ip("2.3.4.6")))),
                hasIpsecProposals(
                    contains(
                        ImmutableList.of(
                            allOf(
                                IpsecProposalMatchers.hasEncryptionAlgorithm(
                                    EncryptionAlgorithm.AES_256_CBC),
                                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                                    IpsecAuthenticationAlgorithm.HMAC_MD5_96)),
                            allOf(
                                IpsecProposalMatchers.hasEncryptionAlgorithm(
                                    EncryptionAlgorithm.AES_256_CBC),
                                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96))))),
                hasPfsKeyGroup(DiffieHellmanGroup.GROUP14))));

    // testing the Diffie Hellman groups
    assertThat(c, hasIpsecPolicy("IPSEC-PROFILE2", hasPfsKeyGroup(DiffieHellmanGroup.GROUP15)));
    assertThat(c, hasIpsecPolicy("IPSEC-PROFILE3", hasPfsKeyGroup(DiffieHellmanGroup.GROUP16)));
    assertThat(c, hasIpsecPolicy("IPSEC-PROFILE4", hasPfsKeyGroup(DiffieHellmanGroup.GROUP19)));
    assertThat(c, hasIpsecPolicy("IPSEC-PROFILE5", hasPfsKeyGroup(DiffieHellmanGroup.GROUP21)));
    assertThat(c, hasIpsecPolicy("IPSEC-PROFILE6", hasPfsKeyGroup(DiffieHellmanGroup.GROUP5)));
  }

  @Test
  public void testArubaIpsecTransformset() throws IOException {
    Configuration c = parseConfig("arubaCryptoTransformSet");
    assertThat(
        c,
        hasIpsecProposal(
            "ts1",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_CBC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts2",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_192_CBC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts3",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_256_CBC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts4",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.DES_CBC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts5",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts6",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_GCM),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts7",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_256_GCM),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
  }

  @Test
  public void testIpsecTransformset() throws IOException {
    Configuration c = parseConfig("ios-crypto-transform-set");
    assertThat(
        c,
        hasIpsecProposal(
            "ts1",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_256_CBC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts2",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP, IpsecProtocol.AH)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts3",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_192_CBC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP, IpsecProtocol.AH)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts4",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_GCM),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts5",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_256_GCM),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts6",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_128_GMAC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts7",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_256_GMAC),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts8",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.SEAL_160),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
    assertThat(
        c,
        hasIpsecProposal(
            "ts9",
            allOf(
                IpsecProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.NULL),
                hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)))));
  }

  @Test
  public void testTransformsetToIpsecphase2Proposal() throws IOException {
    Configuration c = parseConfig("ios-crypto-transform-set");
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "ts1",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_256_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "ts2",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_SHA1_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.THREEDES_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(
                    ImmutableSortedSet.of(IpsecProtocol.ESP, IpsecProtocol.AH)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "ts3",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_192_CBC),
                IpsecPhase2ProposalMatchers.hasProtocols(
                    ImmutableSortedSet.of(IpsecProtocol.ESP, IpsecProtocol.AH)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "ts4",
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
            "ts5",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(EncryptionAlgorithm.AES_256_GCM),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));

    assertThat(
        c,
        hasIpsecPhase2Proposal(
            "ts6",
            allOf(
                IpsecPhase2ProposalMatchers.hasAuthenticationAlgorithm(
                    IpsecAuthenticationAlgorithm.HMAC_MD5_96),
                IpsecPhase2ProposalMatchers.hasEncryptionAlgorithm(
                    EncryptionAlgorithm.AES_128_GMAC),
                IpsecPhase2ProposalMatchers.hasProtocols(ImmutableSortedSet.of(IpsecProtocol.ESP)),
                IpsecPhase2ProposalMatchers.hasIpsecEncapsulationMode(
                    IpsecEncapsulationMode.TUNNEL))));
  }

  @Test
  public void testIpsecVpnIos() throws IOException {
    String testrigName = "ipsec-vpn-ios";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    assertThat(
        configurations.values().stream().mapToLong(c -> c.getIpsecVpns().values().size()).sum(),
        equalTo(6L));
    configurations
        .values()
        .stream()
        .flatMap(c -> c.getIpsecVpns().values().stream())
        .forEach(iv -> assertThat(iv.getRemoteIpsecVpn(), not(nullValue())));
    /* Two tunnels should not be established because of a password mismatch between r1 and r3 */
    assertThat(
        configurations
            .values()
            .stream()
            .flatMap(c -> c.getInterfaces().values().stream())
            .filter(i -> i.getInterfaceType().equals(InterfaceType.TUNNEL) && i.getActive())
            .count(),
        equalTo(4L));
  }

  @Test
  public void testIpsecTopology() throws IOException {
    String testrigName = "ios-crypto-ipsec";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    ValueGraph<IpsecPeerConfigId, IpsecSession> graph = IpsecUtil.initIpsecTopology(configurations);

    Set<EndpointPair<IpsecPeerConfigId>> edges = graph.edges();

    // there should be six edges in total, two for the static crypto map session between r1 and r2
    // two for the dynamic crypto map session from r1->r3 and r2->r3 (unidirectional)
    // two for the tunnel interface IPSec session between r2 and r3
    assertThat(edges, hasSize(6));

    // checking that the negotiated IKE and IPSec proposals are set in all the sessions
    for (EndpointPair<IpsecPeerConfigId> edge : edges) {
      IpsecSession ipsecSession = graph.edgeValueOrDefault(edge.nodeU(), edge.nodeV(), null);

      assertThat(ipsecSession, notNullValue());

      assertThat(ipsecSession, hasNegotiatedIkeP1Proposal(notNullValue()));
      assertThat(ipsecSession, hasNegotiatedIkeP1Key(notNullValue()));
      assertThat(ipsecSession, hasNegotiatedIpsecP2Proposal(notNullValue()));
    }
  }

  @Test
  public void testNxosOspfAreaParameters() throws IOException {
    String testrigName = "nxos-ospf";
    String hostname = "nxos-ospf-area";
    String ifaceName = "Ethernet1";
    long areaNum = 1L;
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    /* Ensure bidirectional references between OSPF area and interface */
    assertThat(configurations, hasKey(hostname));
    Configuration c = configurations.get(hostname);
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasAreas(hasKey(areaNum)))));
    OspfArea area = c.getDefaultVrf().getOspfProcess().getAreas().get(areaNum);
    assertThat(area, OspfAreaMatchers.hasInterfaces(hasItem(ifaceName)));
    assertThat(c, hasInterface(ifaceName, hasOspfArea(sameInstance(area))));
    assertThat(c, hasInterface(ifaceName, isOspfPassive(equalTo(false))));
    assertThat(c, hasInterface(ifaceName, isOspfPointToPoint()));
  }

  @Test
  public void testNxosOspfNonDefaultVrf() throws IOException {
    String testrigName = "nxos-ospf";
    String hostname = "nxos-ospf-iface-in-vrf";
    String ifaceName = "Ethernet1";
    String vrfName = "OTHER-VRF";
    long areaNum = 1L;
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    /* Ensure bidirectional references between OSPF area and interface */
    assertThat(configurations, hasKey(hostname));
    Configuration c = configurations.get(hostname);
    assertThat(c, hasVrfs(hasKey(vrfName)));
    Vrf vrf = c.getVrfs().get(vrfName);
    assertThat(vrf, hasOspfProcess(hasAreas(hasKey(areaNum))));
    OspfArea area = vrf.getOspfProcess().getAreas().get(areaNum);
    assertThat(area, OspfAreaMatchers.hasInterfaces(hasItem(ifaceName)));
    assertThat(c, hasInterface(ifaceName, hasVrf(sameInstance(vrf))));
    assertThat(c, hasInterface(ifaceName, hasOspfArea(sameInstance(area))));
    assertThat(c, hasInterface(ifaceName, isOspfPassive(equalTo(false))));
    assertThat(c, hasInterface(ifaceName, isOspfPointToPoint()));
  }

  @Test
  public void testOspfMaxMetric() throws IOException {
    String testrigName = "ospf-max-metric";
    String iosMaxMetricName = "ios-max-metric";
    String iosMaxMetricCustomName = "ios-max-metric-custom";
    String iosMaxMetricOnStartupName = "ios-max-metric-on-startup";
    List<String> configurationNames =
        ImmutableList.of(iosMaxMetricName, iosMaxMetricCustomName, iosMaxMetricOnStartupName);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosMaxMetric = configurations.get(iosMaxMetricName);
    Configuration iosMaxMetricCustom = configurations.get(iosMaxMetricCustomName);
    Configuration iosMaxMetricOnStartup = configurations.get(iosMaxMetricOnStartupName);
    OspfProcess proc = iosMaxMetric.getDefaultVrf().getOspfProcess();
    OspfProcess procCustom = iosMaxMetricCustom.getDefaultVrf().getOspfProcess();
    OspfProcess procOnStartup = iosMaxMetricOnStartup.getDefaultVrf().getOspfProcess();
    long expectedMaxMetricRouterLsa =
        org.batfish.representation.cisco.OspfProcess.MAX_METRIC_ROUTER_LSA;
    long expectedMaxMetricStub = org.batfish.representation.cisco.OspfProcess.MAX_METRIC_ROUTER_LSA;
    long expectedMaxMetricExternal =
        org.batfish.representation.cisco.OspfProcess.DEFAULT_MAX_METRIC_EXTERNAL_LSA;
    long expectedMaxMetricSummary =
        org.batfish.representation.cisco.OspfProcess.DEFAULT_MAX_METRIC_SUMMARY_LSA;
    long expectedCustomMaxMetricExternal = 12345L;
    long expectedCustomMaxMetricSummary = 23456L;

    assertThat(proc.getMaxMetricTransitLinks(), equalTo(expectedMaxMetricRouterLsa));
    assertThat(proc.getMaxMetricStubNetworks(), equalTo(expectedMaxMetricStub));
    assertThat(proc.getMaxMetricExternalNetworks(), equalTo(expectedMaxMetricExternal));
    assertThat(proc.getMaxMetricSummaryNetworks(), equalTo(expectedMaxMetricSummary));
    assertThat(procCustom.getMaxMetricTransitLinks(), equalTo(expectedMaxMetricRouterLsa));
    assertThat(procCustom.getMaxMetricStubNetworks(), equalTo(expectedMaxMetricStub));
    assertThat(procCustom.getMaxMetricExternalNetworks(), equalTo(expectedCustomMaxMetricExternal));
    assertThat(procCustom.getMaxMetricSummaryNetworks(), equalTo(expectedCustomMaxMetricSummary));
    assertThat(procOnStartup.getMaxMetricTransitLinks(), is(nullValue()));
    assertThat(procOnStartup.getMaxMetricStubNetworks(), is(nullValue()));
    assertThat(procOnStartup.getMaxMetricExternalNetworks(), is(nullValue()));
    assertThat(procOnStartup.getMaxMetricSummaryNetworks(), is(nullValue()));
  }

  @Test
  public void testOspfPointToPoint() throws IOException {
    String testrigName = "ospf-point-to-point";
    String iosOspfPointToPoint = "ios-ospf-point-to-point";
    List<String> configurationNames = ImmutableList.of(iosOspfPointToPoint);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosMaxMetric = configurations.get(iosOspfPointToPoint);
    Interface e0Sub0 = iosMaxMetric.getInterfaces().get("Ethernet0/0");
    Interface e0Sub1 = iosMaxMetric.getInterfaces().get("Ethernet0/1");

    assertTrue(e0Sub0.getOspfPointToPoint());
    assertFalse(e0Sub1.getOspfPointToPoint());
  }

  @Test
  public void testParsingRecovery() throws IOException {
    String testrigName = "parsing-recovery";
    String hostname = "ios-recovery";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosRecovery = configurations.get(hostname);
    Map<String, Interface> iosRecoveryInterfaces = iosRecovery.getInterfaces();
    Set<String> iosRecoveryInterfaceNames = iosRecoveryInterfaces.keySet();
    Set<InterfaceAddress> l3Prefixes = iosRecoveryInterfaces.get("Loopback3").getAllAddresses();
    Set<InterfaceAddress> l4Prefixes = iosRecoveryInterfaces.get("Loopback4").getAllAddresses();

    assertThat("Loopback0", in(iosRecoveryInterfaceNames));
    assertThat("Loopback1", in(iosRecoveryInterfaceNames));
    assertThat("Loopback2", not(in(iosRecoveryInterfaceNames)));
    assertThat("Loopback3", in(iosRecoveryInterfaceNames));
    assertThat(new InterfaceAddress("10.0.0.1/32"), not(in(l3Prefixes)));
    assertThat(new InterfaceAddress("10.0.0.2/32"), in(l3Prefixes));
    assertThat("Loopback4", in(iosRecoveryInterfaceNames));
    assertThat(new InterfaceAddress("10.0.0.3/32"), not(in(l4Prefixes)));
    assertThat(new InterfaceAddress("10.0.0.4/32"), in(l4Prefixes));
  }

  @Test
  public void testParsingRecovery1141() throws IOException {
    // Test for https://github.com/batfish/batfish/issues/1141
    String testrigName = "parsing-recovery";
    String hostname = "ios-recovery-1141";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosRecovery = configurations.get(hostname);
    assertThat(iosRecovery, allOf(Matchers.notNullValue(), hasInterface("Loopback0", anything())));
  }

  @Test
  public void testParsingRecoveryNoInfiniteLoopDuringAdaptivePredictionAtEof() throws IOException {
    String testrigName = "parsing-recovery";
    String hostname = "ios-blankish-file";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    /* Hostname is unknown, but a file should be generated nonetheless */
    assertThat(configurations.entrySet(), hasSize(1));
  }

  @Test
  public void testParsingUnrecognizedInterfaceName() throws IOException {
    String testrigName = "parsing-recovery";
    String hostname = "ios-bad-interface-name";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    /* Parser should not crash, and configuration with hostname from file should be generated */
    assertThat(configurations, hasKey(hostname));
  }

  private Configuration parseConfig(String hostname) throws IOException {
    return parseTextConfigs(hostname).get(hostname);
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.parseTextConfigs(_folder, names);
  }

  @Test
  public void testPrefixListNameParsing() throws IOException {
    String hostname = "prefix-list-name-parsing";
    String filename = "configs/" + hostname;
    String prefixListName = "SET_COMMUNITY_65535:200";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm prefix-list with correct name was defined */
    assertThat(ccae, hasDefinedStructure(filename, PREFIX_LIST, prefixListName));

    /* Confirm prefix-list is referenced */
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, prefixListName, 1));
  }

  @Test
  public void testRfc1583Compatible() throws IOException {
    String[] configurationNames =
        new String[] {"rfc1583Compatible", "rfc1583NoCompatible", "rfc1583Unconfigured"};
    Map<String, Configuration> configurations = parseTextConfigs(configurationNames);

    Boolean[] expectedResults = new Boolean[] {Boolean.TRUE, Boolean.FALSE, null};
    for (int i = 0; i < configurationNames.length; i++) {
      Configuration configuration = configurations.get(configurationNames[i]);
      assertThat(configuration.getVrfs().size(), equalTo(1));
      for (Vrf vrf : configuration.getVrfs().values()) {
        assertThat(vrf.getOspfProcess().getRfc1583Compatible(), is(expectedResults[i]));
      }
    }
  }

  @Test
  public void testAristaSubinterfaceMtu() throws IOException {
    Configuration c = parseConfig("aristaInterface");

    assertThat(c, hasInterface("Ethernet3/2/1.4", hasMtu(9000)));
  }

  @Test
  public void testNxosBgpVrf() throws IOException {
    Configuration c = parseConfig("nxosBgpVrf");
    assertThat(c.getVrfs().get("bar").getBgpProcess().getActiveNeighbors().values(), hasSize(2));
    assertThat(
        c,
        ConfigurationMatchers.hasVrf(
            "bar",
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.parse("2.2.2.2/32"),
                    allOf(hasRemoteAs(2L), hasLocalAs(1L), hasAllowRemoteAsOut(true))))));
    assertThat(
        c,
        ConfigurationMatchers.hasVrf(
            "bar",
            hasBgpProcess(
                hasActiveNeighbor(Prefix.parse("3.3.3.3/32"), hasAllowRemoteAsOut(false)))));
  }

  @Test
  public void testArista100gfullInterface() throws IOException {
    Configuration c = parseConfig("arista100gfull");
    assertThat(
        c,
        hasInterface(
            "Ethernet1/1",
            hasAllAddresses(containsInAnyOrder(new InterfaceAddress("10.20.0.3/31")))));
  }
}

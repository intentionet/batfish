package org.batfish.grammar.cisco;

import static java.util.Objects.requireNonNull;
import static org.batfish.common.matchers.ParseWarningMatchers.hasComment;
import static org.batfish.common.matchers.ParseWarningMatchers.hasText;
import static org.batfish.common.util.CommonUtil.sha256Digest;
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
import static org.batfish.datamodel.Interface.UNSET_LOCAL_INTERFACE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrc;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.datamodel.matchers.AaaAuthenticationLoginListMatchers.hasMethod;
import static org.batfish.datamodel.matchers.AaaAuthenticationLoginMatchers.hasListForKey;
import static org.batfish.datamodel.matchers.AaaAuthenticationMatchers.hasLogin;
import static org.batfish.datamodel.matchers.AaaMatchers.hasAuthentication;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasProtocol;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasAllowRemoteAsOut;
import static org.batfish.datamodel.matchers.AddressFamilyMatchers.hasAddressFamilyCapabilites;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.hasConjuncts;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.isAndMatchExprThat;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasIpv4UnicastAddressFamily;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEbgp;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEquivalentAsPathMatchMode;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbors;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasWeight;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.isBgpv4RouteThat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasConfigurationFormat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkePhase1Policy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIkePhase1Proposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessList;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessLists;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpSpace;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPeerConfig;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPhase2Policy;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpsecPhase2Proposal;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasMlagConfig;
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
import static org.batfish.datamodel.matchers.DataModelMatchers.hasPostTransformationIncomingFilter;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasPreTransformationOutgoingFilter;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRedFlagWarning;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRoute6FilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasZone;
import static org.batfish.datamodel.matchers.DataModelMatchers.isIpSpaceReferenceThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.isPermittedByAclThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.permits;
import static org.batfish.datamodel.matchers.EigrpMetricMatchers.hasDelay;
import static org.batfish.datamodel.matchers.EigrpRouteMatchers.hasEigrpMetric;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasDstIps;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasDstPorts;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasSrcIps;
import static org.batfish.datamodel.matchers.HsrpGroupMatchers.hasTrackActions;
import static org.batfish.datamodel.matchers.IkePhase1KeyMatchers.hasKeyType;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasIkePhase1Key;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasIkePhase1Proposals;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasLocalInterface;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasRemoteIdentity;
import static org.batfish.datamodel.matchers.IkePhase1PolicyMatchers.hasSelfIdentity;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAccessVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAddress;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllowedVlans;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDeclaredNames;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasEigrp;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasEncapsulationVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasHsrpGroup;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasHsrpVersion;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasInterfaceType;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasIsis;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMlagId;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasNativeVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfArea;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfAreaName;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSpeed;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortEncapsulation;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVrf;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPassive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPointToPoint;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isProxyArp;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isSwitchport;
import static org.batfish.datamodel.matchers.IpAccessListLineMatchers.hasMatchCondition;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.accepts;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.hasLines;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.rejects;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasDestinationAddress;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasLocalAddress;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasPolicyAccessList;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasSourceInterface;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.hasTunnelInterface;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.isIpsecDynamicPeerConfigThat;
import static org.batfish.datamodel.matchers.IpsecPeerConfigMatchers.isIpsecStaticPeerConfigThat;
import static org.batfish.datamodel.matchers.IpsecSessionMatchers.hasNegotiatedIkeP1Key;
import static org.batfish.datamodel.matchers.IpsecSessionMatchers.hasNegotiatedIkeP1Proposal;
import static org.batfish.datamodel.matchers.IpsecSessionMatchers.hasNegotiatedIpsecP2Proposal;
import static org.batfish.datamodel.matchers.IsisInterfaceSettingsMatchers.hasLevel2;
import static org.batfish.datamodel.matchers.LineMatchers.hasAuthenticationLoginList;
import static org.batfish.datamodel.matchers.LineMatchers.requiresAuthentication;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.hasHeaderSpace;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.isMatchHeaderSpaceThat;
import static org.batfish.datamodel.matchers.MlagMatchers.hasId;
import static org.batfish.datamodel.matchers.MlagMatchers.hasPeerAddress;
import static org.batfish.datamodel.matchers.MlagMatchers.hasPeerInterface;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasDefaultOriginateType;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasSuppressType3;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasSuppressType7;
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
import static org.batfish.datamodel.matchers.RegexCommunitySetMatchers.hasRegex;
import static org.batfish.datamodel.matchers.RegexCommunitySetMatchers.isRegexCommunitySet;
import static org.batfish.datamodel.matchers.SnmpServerMatchers.hasCommunities;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasBumTransportIps;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasBumTransportMethod;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasSourceAddress;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasUdpPort;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasVlan;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasEigrpProcesses;
import static org.batfish.datamodel.matchers.VrfMatchers.hasOspfProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasSnmpServer;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.batfish.datamodel.matchers.VrfMatchers.hasVniSettings;
import static org.batfish.datamodel.transformation.Transformation.when;
import static org.batfish.datamodel.transformation.TransformationStep.assignDestinationIp;
import static org.batfish.datamodel.transformation.TransformationStep.assignSourceIp;
import static org.batfish.datamodel.transformation.TransformationStep.shiftDestinationIp;
import static org.batfish.datamodel.transformation.TransformationStep.shiftSourceIp;
import static org.batfish.datamodel.vendor_family.VendorFamilyMatchers.hasCisco;
import static org.batfish.datamodel.vendor_family.cisco.CiscoFamilyMatchers.hasAaa;
import static org.batfish.datamodel.vendor_family.cisco.CiscoFamilyMatchers.hasLogging;
import static org.batfish.datamodel.vendor_family.cisco.LoggingMatchers.isOn;
import static org.batfish.grammar.cisco.CiscoControlPlaneExtractor.SERIAL_LINE;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpPeerExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpPeerImportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeCombinedOutgoingAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeIcmpObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeInspectClassMapAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeInspectPolicyMapAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeProtocolObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeSecurityLevelZoneName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeServiceObjectAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeServiceObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeZonePairAclName;
import static org.batfish.representation.cisco.CiscoIosDynamicNat.computeDynamicDestinationNatAclName;
import static org.batfish.representation.cisco.CiscoStructureType.ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.BFD_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureType.ICMP_TYPE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.INSPECT_CLASS_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.INSPECT_POLICY_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureType.IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.IPV4_ACCESS_LIST_EXTENDED;
import static org.batfish.representation.cisco.CiscoStructureType.IPV4_ACCESS_LIST_STANDARD;
import static org.batfish.representation.cisco.CiscoStructureType.IPV6_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.IPV6_ACCESS_LIST_EXTENDED;
import static org.batfish.representation.cisco.CiscoStructureType.IPV6_ACCESS_LIST_STANDARD;
import static org.batfish.representation.cisco.CiscoStructureType.IP_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.KEYRING;
import static org.batfish.representation.cisco.CiscoStructureType.MAC_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.NAMED_RSA_PUB_KEY;
import static org.batfish.representation.cisco.CiscoStructureType.NETWORK_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureType.NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX6_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX_SET;
import static org.batfish.representation.cisco.CiscoStructureType.PROTOCOL_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.SECURITY_ZONE;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.TRACK;
import static org.batfish.representation.cisco.CiscoStructureType.VXLAN;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.INSPECT_POLICY_MAP_INSPECT_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_BFD_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_INCOMING_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_OUTGOING_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_DESTINATION_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISAKMP_PROFILE_KEYRING;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_ACCESS_LIST;
import static org.batfish.representation.cisco.OspfProcess.getReferenceOspfBandwidth;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Range;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ValueGraph;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.topology.IpOwners;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.IpsecUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.EigrpExternalRoute;
import org.batfish.datamodel.EigrpInternalRoute;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.IkeKeyType;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpaceReference;
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
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TunnelConfiguration;
import org.batfish.datamodel.TunnelConfiguration.Builder;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.bgp.BgpTopologyUtils;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.eigrp.EigrpNeighborConfig;
import org.batfish.datamodel.eigrp.EigrpProcessMode;
import org.batfish.datamodel.matchers.CommunityListLineMatchers;
import org.batfish.datamodel.matchers.CommunityListMatchers;
import org.batfish.datamodel.matchers.ConfigurationMatchers;
import org.batfish.datamodel.matchers.EigrpInterfaceSettingsMatchers;
import org.batfish.datamodel.matchers.HsrpGroupMatchers;
import org.batfish.datamodel.matchers.IkePhase1KeyMatchers;
import org.batfish.datamodel.matchers.IkePhase1ProposalMatchers;
import org.batfish.datamodel.matchers.IpsecPeerConfigMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2PolicyMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2ProposalMatchers;
import org.batfish.datamodel.matchers.MlagMatchers;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.datamodel.matchers.Route6FilterListMatchers;
import org.batfish.datamodel.matchers.RouteFilterListMatchers;
import org.batfish.datamodel.matchers.StubSettingsMatchers;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.CommunityHalvesExpr;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityConjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityHalf;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.RangeCommunityHalf;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.tracking.DecrementPriority;
import org.batfish.datamodel.tracking.TrackInterface;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.dataplane.ibdp.IncrementalDataPlane;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.cisco.CiscoAsaNat;
import org.batfish.representation.cisco.CiscoAsaNat.Section;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.batfish.representation.cisco.DistributeList;
import org.batfish.representation.cisco.DistributeList.DistributeListFilterType;
import org.batfish.representation.cisco.EigrpProcess;
import org.batfish.representation.cisco.NetworkObject;
import org.batfish.representation.cisco.NetworkObjectAddressSpecifier;
import org.batfish.representation.cisco.NetworkObjectGroupAddressSpecifier;
import org.batfish.representation.cisco.Tunnel.TunnelMode;
import org.batfish.representation.cisco.WildcardAddressSpecifier;
import org.batfish.representation.cisco.eos.AristaEosVxlan;
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
    return createFlow(protocol, srcPort, dstPort, FlowState.NEW);
  }

  private Flow createFlow(IpProtocol protocol, int srcPort, int dstPort, FlowState state) {
    return Flow.builder()
        .setIngressNode("")
        .setTag("")
        .setIpProtocol(protocol)
        .setState(state)
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
        .setIcmpCode(0)
        .build();
  }

  private CiscoConfiguration parseCiscoConfig(String hostname, ConfigurationFormat format) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    CiscoCombinedParser ciscoParser = new CiscoCombinedParser(src, settings, format);
    CiscoControlPlaneExtractor extractor =
        new CiscoControlPlaneExtractor(src, ciscoParser, format, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(
            ciscoParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(tree);
    CiscoConfiguration vendorConfiguration =
        (CiscoConfiguration) extractor.getVendorConfiguration();
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + hostname);
    return vendorConfiguration;
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
  public void testEncoding() throws IOException {
    // Don't crash with lexer error
    parseConfig("encoding_test");
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
  public void testAllowedVlans() throws IOException {
    Configuration c = parseConfig("eos-allowed-vlans");

    assertThat(
        c.getAllInterfaces().get("Port-Channel1").getAllowedVlans(),
        equalTo(IntegerSpace.of(Range.closed(1, 2))));
    assertThat(
        c.getAllInterfaces().get("Port-Channel2").getAllowedVlans(),
        equalTo(IntegerSpace.of(Range.closed(2, 3))));
    assertThat(
        c.getAllInterfaces().get("Port-Channel3").getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    assertThat(
        c.getAllInterfaces().get("Port-Channel4").getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    assertThat(
        c.getAllInterfaces().get("Port-Channel5").getAllowedVlans(),
        equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    assertThat(
        c.getAllInterfaces().get("Port-Channel6").getAllowedVlans(),
        equalTo(IntegerSpace.of(Range.closed(1, 4))));
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
    assertThat(
        manual.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(), equalTo(3e6d));

    Configuration defaults = parseConfig("aristaOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.ARISTA)));
  }

  @Test
  public void testAristaVsIosOspfRedistributeBgpSyntax() throws IOException {
    // Both an Arista and an IOS config contain these "redistribute bgp" lines in their OSPF setups:
    String l1 = "redistribute bgp";
    String l2 = "redistribute bgp route-map MAP";
    String l3 = "redistribute bgp 65100";
    String l4 = "redistribute bgp 65100 route-map MAP";
    String l5 = "redistribute bgp 65100 metric 10";
    /*
    Arista does not allow specification of an AS, while IOS requires it, and IOS allows setting
    metric and a few other settings where Arista does not (both allow route-maps). So:
    - First two lines should generate warnings on IOS but not Arista
    - Remaining lines should generate warnings on Arista but not IOS
    */
    String testrigName = "arista-vs-ios-warnings";
    String iosName = "ios-ospf-redistribute-bgp";
    String aristaName = "arista-ospf-redistribute-bgp";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    TESTRIGS_PREFIX + testrigName, ImmutableList.of(aristaName, iosName))
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement();
    assertThat(
        pvcae.getWarnings().get("configs/" + iosName).getParseWarnings(),
        contains(
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l1))),
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l2)))));
    assertThat(
        pvcae.getWarnings().get("configs/" + aristaName).getParseWarnings(),
        contains(
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l3))),
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l4))),
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l5)))));
  }

  @Test
  public void testAristaBgpDefaultOriginatePolicy() throws IOException {
    /*
     Arista originator has: neighbor 10.1.1.2 default-originate route-map ROUTE_MAP
     Because this is an Arista device, the route-map should be applied to the default route before
     it is exported rather than used as a default route generation policy.
    */
    String testrigName = "arista-bgp-default-originate";
    List<String> configurationNames = ImmutableList.of("arista-originator", "ios-listener");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    batfish.computeDataPlane();
    IncrementalDataPlane dp = (IncrementalDataPlane) batfish.loadDataPlane();
    Set<AbstractRoute> listenerRoutes =
        dp.getRibs().get("ios-listener").get(DEFAULT_VRF_NAME).getRoutes();

    // ROUTE_MAP adds two communities to default route. Make sure listener's default route has them.
    Bgpv4Route expectedDefaultRoute =
        Bgpv4Route.builder()
            .setCommunities(
                ImmutableSet.of(StandardCommunity.of(7274718L), StandardCommunity.of(21823932L)))
            .setNetwork(Prefix.ZERO)
            .setNextHopIp(Ip.parse("10.1.1.1"))
            .setReceivedFromIp(Ip.parse("10.1.1.1"))
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setSrcProtocol(RoutingProtocol.BGP)
            .setAsPath(AsPath.ofSingletonAsSets(1L))
            .setAdmin(20)
            .setLocalPreference(100)
            .build();
    assertThat(listenerRoutes, hasItem(equalTo(expectedDefaultRoute)));
  }

  @Test
  public void testAristaBgpDefaultOriginateUndefinedRouteMap() throws IOException {
    /*
     Arista originator has: neighbor 10.1.1.2 default-originate route-map ROUTE_MAP
     However, ROUTE_MAP is undefined. Since this is Arista, default route should still be exported
     (on IOS, where ROUTE_MAP is used as a generation policy rather than an export policy in this
     context, the default route would not be exported).
    */
    String testrigName = "arista-bgp-default-originate";
    List<String> configurationNames =
        ImmutableList.of("arista-originator-undefined-rm", "ios-listener");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    batfish.computeDataPlane();
    IncrementalDataPlane dp = (IncrementalDataPlane) batfish.loadDataPlane();
    Set<AbstractRoute> listenerRoutes =
        dp.getRibs().get("ios-listener").get(DEFAULT_VRF_NAME).getRoutes();

    Bgpv4Route expectedDefaultRoute =
        Bgpv4Route.builder()
            .setNetwork(Prefix.ZERO)
            .setNextHopIp(Ip.parse("10.1.1.1"))
            .setReceivedFromIp(Ip.parse("10.1.1.1"))
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setSrcProtocol(RoutingProtocol.BGP)
            .setAsPath(AsPath.ofSingletonAsSets(1L))
            .setCommunities(ImmutableSet.of())
            .setAdmin(20)
            .setLocalPreference(100)
            .build();
    assertThat(listenerRoutes, hasItem(equalTo(expectedDefaultRoute)));
  }

  @Test
  public void testArubaConfigurationFormat() throws IOException {
    Configuration arubaConfig = parseConfig("arubaConfiguration");

    assertThat(arubaConfig, hasConfigurationFormat(equalTo(ConfigurationFormat.ARUBAOS)));
  }

  @Test
  public void testAsaAclObject() throws IOException {
    String hostname = "asa-acl-object";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * The produced ACL should permit if source matcher object on1, destination matches on2, and
     * service matches os1.
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
                                                        isIpSpaceReferenceThat(hasName("on2"))),
                                                    hasSrcIps(
                                                        isIpSpaceReferenceThat(hasName("on1")))))),
                                        isPermittedByAclThat(
                                            hasAclName(
                                                computeServiceObjectAclName("os1"))))))))))));

    /*
     * We expect only objects osunused1, onunused1 to have zero referrers
     */
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT, "os1", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "on1", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "on2", 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT, "osunused1", 0));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "onunused1", 0));

    /*
     * We expect undefined references only to objects osfake, onfake1, onfake2
     */
    assertThat(ccae, not(hasUndefinedReference(filename, SERVICE_OBJECT, "os1")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "on1")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "on2")));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, SERVICE_OBJECT, "osfake", EXTENDED_ACCESS_LIST_SERVICE_OBJECT));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, NETWORK_OBJECT, "onfake2", EXTENDED_ACCESS_LIST_NETWORK_OBJECT));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, NETWORK_OBJECT, "onfake1", EXTENDED_ACCESS_LIST_NETWORK_OBJECT));
  }

  @Test
  public void testAsaEigrpNetwork() {
    CiscoConfiguration config = parseCiscoConfig("asa-eigrp", ConfigurationFormat.CISCO_ASA);

    // ASN is 1
    EigrpProcess eigrpProcess = config.getDefaultVrf().getEigrpProcesses().get(1L);
    assertThat(eigrpProcess.getWildcardNetworks(), contains(IpWildcard.parse("10.0.0.0/24")));
  }

  @Test
  public void testAsaEigrpPassive() throws IOException {
    Configuration config = parseConfig("asa-eigrp");

    assertThat(
        config, hasInterface("inside", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(true))));
  }

  @Test
  public void testAsaOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("asaOspfCost");
    assertThat(
        manual.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(), equalTo(3e6d));

    Configuration defaults = parseConfig("asaOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_ASA)));
  }

  @Test
  public void testAsaFilters() throws IOException {
    String hostname = "asa-filters";
    Configuration c = parseConfig(hostname);

    String ifaceAlias = "name1";

    Flow flowPass = createFlow(IpProtocol.TCP, 1, 123);
    Flow flowFail = createFlow(IpProtocol.TCP, 1, 1);

    // Confirm access list permits only traffic matching ACL
    assertThat(
        c,
        hasInterface(ifaceAlias, hasPreTransformationOutgoingFilter(accepts(flowPass, null, c))));
    assertThat(
        c,
        hasInterface(
            ifaceAlias, hasPreTransformationOutgoingFilter(not(accepts(flowFail, null, c)))));
  }

  @Test
  public void testAsaFiltersGlobal() throws IOException {
    String hostname = "asa-filters-global";
    Configuration c = parseConfig(hostname);

    String iface1Alias = "name1";
    String iface2Alias = "name2";

    Flow flowPass = createFlow(IpProtocol.TCP, 1, 123);
    Flow flowFail = createFlow(IpProtocol.TCP, 1, 1);

    // Confirm global ACL affects all interfaces
    assertThat(
        c,
        hasInterface(iface1Alias, hasPostTransformationIncomingFilter(accepts(flowPass, null, c))));
    assertThat(
        c,
        hasInterface(iface2Alias, hasPostTransformationIncomingFilter(accepts(flowPass, null, c))));
    assertThat(
        c,
        hasInterface(
            iface1Alias, hasPostTransformationIncomingFilter(not(accepts(flowFail, null, c)))));
    assertThat(
        c,
        hasInterface(
            iface2Alias, hasPostTransformationIncomingFilter(not(accepts(flowFail, null, c)))));
  }

  @Test
  public void testAsaFiltersGlobalReference() throws IOException {
    String hostname = "asa-filters-global";
    String filename = "configs/" + hostname;
    parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm reference tracking is correct for globally applied ASA access list in access group
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "FILTER_GLOBAL", 1));
  }

  @Test
  public void testAsaFiltersReference() throws IOException {
    String hostname = "asa-filters";
    String filename = "configs/" + hostname;
    parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm reference tracking is correct for ASA access lists in access group
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "FILTER_IN", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "FILTER_IN4", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "FILTER_OUT", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "FILTER_OUT5", 1));
    assertThat(ccae, hasUndefinedReference(filename, IP_ACCESS_LIST, "FILTER_UNDEF"));
  }

  @Test
  public void testAsaSecurityLevelAndFilters() throws IOException {
    String hostname = "asa-filters";
    Configuration c = parseConfig(hostname);

    String highIface1 = "name1"; // GigabitEthernet0/1
    String lowIface2 = "name2"; // GigabitEthernet0/2
    String highIface3 = "name3"; // GigabitEthernet0/3
    String lowIface4 = "name4"; // GigabitEthernet0/4
    String lowIface5 = "name5"; // GigabitEthernet0/5

    Flow flowPass = createFlow(IpProtocol.TCP, 1, 123);
    Flow flowFail = createFlow(IpProtocol.TCP, 1, 1);
    Flow anyFlow = createFlow(IpProtocol.IP, 0, 0, FlowState.NEW);

    // Confirm access list permits only traffic matching both ACL and security level restrictions
    // highIface1 has inbound filter permitting all IP traffic
    // highIface1 has outbound filter permitting only TCP port 123
    // highIface1 rejects all traffic from lowIface2 due to security level restriction
    assertThat(
        c,
        hasInterface(
            highIface1, hasPreTransformationOutgoingFilter(rejects(anyFlow, lowIface2, c))));

    // Confirm access list permits only traffic matching both ACL and security level restrictions
    // highIface1 has a higher security level than lowIface5
    // lowIface5 has no inbound filter
    // lowIface5 rejects all outbound traffic except TCP port 123
    assertThat(
        c,
        hasInterface(
            lowIface5, hasPreTransformationOutgoingFilter(rejects(flowFail, highIface1, c))));
    assertThat(
        c,
        hasInterface(
            lowIface5, hasPreTransformationOutgoingFilter(accepts(flowPass, highIface1, c))));

    // lowIface4 has inbound filter permitting only TCP port 123
    // highIface3 has no explicit outbound filter
    assertThat(
        c,
        hasInterface(
            lowIface4, hasPostTransformationIncomingFilter(accepts(flowPass, lowIface4, c))));
    assertThat(
        c,
        hasInterface(
            lowIface4, hasPostTransformationIncomingFilter(rejects(flowFail, lowIface4, c))));
    // any flow outbound on highIface3 from lowIface4 is allowed, assuming it was allowed incoming
    // security level restriction is removed because lowIface4 has an inbound ACL
    assertThat(
        c,
        hasInterface(
            highIface3, hasPreTransformationOutgoingFilter(accepts(anyFlow, lowIface4, c))));
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
    Flow flowInlinePass1 = createFlow(IpProtocol.UDP, 1, 1234);
    Flow flowInlinePass2 = createFlow(IpProtocol.UDP, 1, 1235);
    Flow flowInlinePass3 = createFlow(IpProtocol.UDP, 3, 1236);
    Flow flowInlinePass4 = createFlow(IpProtocol.UDP, 3020, 1); // cifs
    Flow flowTcpPass = createFlow(IpProtocol.TCP, 65535, 1);
    Flow flowUdpPass = createFlow(IpProtocol.UDP, 65535, 1);
    Flow flowTcpFail = createFlow(IpProtocol.TCP, 65534, 1);
    Flow flowUdpFail = createFlow(IpProtocol.UDP, 1, 1236);

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
    assertThat(c, hasIpAccessList(ogsAclName, accepts(flowInlinePass1, null, c)));
    assertThat(c, hasIpAccessList(ogsAclName, accepts(flowInlinePass2, null, c)));
    assertThat(c, hasIpAccessList(ogsAclName, accepts(flowInlinePass3, null, c)));
    assertThat(c, hasIpAccessList(ogsAclName, accepts(flowInlinePass4, null, c)));
    assertThat(c, hasIpAccessList(ogsAclName, not(accepts(flowUdpFail, null, c))));
  }

  @Test
  public void testAsaServiceObjectInline() throws IOException {
    String hostname = "asa-service-object-inline";
    Configuration c = parseConfig(hostname);

    String icmpAclName = "ACL_ICMP";
    String ospfAclName = "ACL_OSPF";

    Flow flowIcmpPass = createIcmpFlow(IcmpType.ECHO_REQUEST);
    Flow flowIcmpFail = createIcmpFlow(IcmpType.ECHO_REPLY);
    Flow flowOspfPass = createFlow(IpProtocol.OSPF, 0, 0);
    Flow flowOspfFail = createFlow(IpProtocol.UDP, 0, 0);

    /* Confirm IpAcls created from inline service objects permit and reject the correct flows */
    assertThat(c, hasIpAccessList(icmpAclName, accepts(flowIcmpPass, null, c)));
    assertThat(c, hasIpAccessList(icmpAclName, rejects(flowIcmpFail, null, c)));
    assertThat(c, hasIpAccessList(ospfAclName, accepts(flowOspfPass, null, c)));
    assertThat(c, hasIpAccessList(ospfAclName, rejects(flowOspfFail, null, c)));
  }

  @Test
  public void testAsaNestedIcmpTypeObjectGroup() throws IOException {
    String hostname = "asa-nested-icmp-type-object-group";
    String filename = "configs/" + hostname;
    String services = computeIcmpObjectGroupAclName("services");
    String mixedGroup = computeIcmpObjectGroupAclName("mixed_group");

    Flow echoReplyFlow = createIcmpFlow(IcmpType.ECHO_REPLY);
    Flow unreachableFlow = createIcmpFlow(IcmpType.DESTINATION_UNREACHABLE);
    Flow redirectFlow = createIcmpFlow(IcmpType.REDIRECT_MESSAGE);
    Flow maskReplyFlow = createIcmpFlow(IcmpType.MASK_REPLY);
    Flow otherFlow = createIcmpFlow(IcmpType.TRACEROUTE);

    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm objects have the correct number of referrers */
    assertThat(ccae, hasNumReferrers(filename, ICMP_TYPE_OBJECT_GROUP, "echo_group", 1));
    assertThat(ccae, hasNumReferrers(filename, ICMP_TYPE_OBJECT_GROUP, "unreachable_group", 1));
    assertThat(ccae, hasNumReferrers(filename, ICMP_TYPE_OBJECT_GROUP, "redirect_group", 1));
    assertThat(ccae, hasNumReferrers(filename, ICMP_TYPE_OBJECT_GROUP, "services", 0));
    assertThat(ccae, hasNumReferrers(filename, ICMP_TYPE_OBJECT_GROUP, "mask_reply_group", 1));
    assertThat(ccae, hasNumReferrers(filename, ICMP_TYPE_OBJECT_GROUP, "mixed_group", 0));
    /* Confirm undefined reference shows up as such */
    assertThat(ccae, hasUndefinedReference(filename, ICMP_TYPE_OBJECT_GROUP, "UNDEFINED_GROUP"));
    assertThat(
        ccae, hasUndefinedReference(filename, ICMP_TYPE_OBJECT_GROUP, "UNDEFINED_GROUP_MIXED"));

    assertThat(c, hasIpAccessList(services, accepts(echoReplyFlow, null, c)));
    assertThat(c, hasIpAccessList(services, accepts(unreachableFlow, null, c)));
    assertThat(c, hasIpAccessList(services, accepts(redirectFlow, null, c)));
    assertThat(c, hasIpAccessList(mixedGroup, accepts(maskReplyFlow, null, c)));
    assertThat(c, hasIpAccessList(services, not(accepts(otherFlow, null, c))));
  }

  @Test
  public void testAsaNestedNetworkObjectGroup() throws IOException {
    String hostname = "asa-nested-network-object-group";
    String filename = "configs/" + hostname;
    Ip engHostIp = Ip.parse("10.1.1.5");
    Ip hrHostIp = Ip.parse("10.1.2.8");
    Ip financeHostIp = Ip.parse("10.1.4.89");
    Ip itIp = Ip.parse("10.2.3.4");
    Ip otherIp = Ip.parse("1.2.3.4");

    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm service have the correct number of referrers */
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "eng", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "hr", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "finance", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "admin", 0));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "it", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "mixed_group", 0));
    /* Confirm undefined reference shows up as such */
    assertThat(ccae, hasUndefinedReference(filename, NETWORK_OBJECT_GROUP, "UNDEFINED_GROUP"));
    assertThat(
        ccae, hasUndefinedReference(filename, NETWORK_OBJECT_GROUP, "UNDEFINED_GROUP_MIXED"));

    assertThat(c, hasIpSpace("admin", containsIp(engHostIp, c.getIpSpaces())));
    assertThat(c, hasIpSpace("admin", containsIp(hrHostIp, c.getIpSpaces())));
    assertThat(c, hasIpSpace("admin", containsIp(financeHostIp, c.getIpSpaces())));
    assertThat(c, hasIpSpace("mixed_group", containsIp(itIp, c.getIpSpaces())));
    assertThat(c, hasIpSpace("admin", not(containsIp(otherIp, c.getIpSpaces()))));
  }

  @Test
  public void testAsaNestedProtocolObjectGroup() throws IOException {
    String hostname = "asa-nested-protocol-object-group";
    String filename = "configs/" + hostname;
    String protocols = computeProtocolObjectGroupAclName("protocols");
    String mixedGroup = computeProtocolObjectGroupAclName("mixed_group");
    int someSrcPort = 65535;
    int someDstPort = 1;

    Flow igmpFlow = createFlow(IpProtocol.IGMP, someSrcPort, someDstPort);
    Flow tcpFlow = createFlow(IpProtocol.TCP, someSrcPort, someDstPort);
    Flow ospfFlow = createFlow(IpProtocol.OSPF, someSrcPort, someDstPort);
    Flow greFlow = createFlow(IpProtocol.GRE, someSrcPort, someDstPort);
    Flow otherFlow = createFlow(IpProtocol.AHP, someSrcPort, someDstPort);

    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm service have the correct number of referrers */
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, "proto1", 1));
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, "proto2", 1));
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, "proto3", 1));
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, "protocols", 0));
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, "proto4", 1));
    assertThat(ccae, hasNumReferrers(filename, PROTOCOL_OBJECT_GROUP, "mixed_group", 0));
    /* Confirm undefined reference shows up as such */
    assertThat(ccae, hasUndefinedReference(filename, PROTOCOL_OBJECT_GROUP, "UNDEFINED_GROUP"));
    assertThat(
        ccae, hasUndefinedReference(filename, PROTOCOL_OBJECT_GROUP, "UNDEFINED_GROUP_MIXED"));

    assertThat(c, hasIpAccessList(protocols, accepts(igmpFlow, null, c)));
    assertThat(c, hasIpAccessList(protocols, accepts(tcpFlow, null, c)));
    assertThat(c, hasIpAccessList(protocols, accepts(ospfFlow, null, c)));
    assertThat(c, hasIpAccessList(mixedGroup, accepts(greFlow, null, c)));
    assertThat(c, hasIpAccessList(protocols, not(accepts(otherFlow, null, c))));
  }

  @Test
  public void testAsaNestedServiceObjectGroup() throws IOException {
    String hostname = "asa-nested-service-object-group";
    String filename = "configs/" + hostname;
    String services = computeServiceObjectGroupAclName("services");
    String mixedGroup = computeServiceObjectGroupAclName("mixed_group");
    int someSrcPort = 65535;
    int someDstPort = 1;

    Flow dns = createFlow(IpProtocol.UDP, someSrcPort, 53);
    Flow customPort = createFlow(IpProtocol.UDP, someSrcPort, 1234);
    Flow customPortInRange = createFlow(IpProtocol.TCP, someSrcPort, 2350);
    Flow customNestedPort = createFlow(IpProtocol.UDP, someSrcPort, 4444);
    Flow otherFlow = createFlow(IpProtocol.AHP, someSrcPort, someDstPort);

    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm objects have the correct number of referrers */
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "service1", 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "service2", 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "service3", 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "services", 0));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "service4", 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_OBJECT_GROUP, "mixed_group", 0));
    /* Confirm undefined reference shows up as such */
    assertThat(ccae, hasUndefinedReference(filename, SERVICE_OBJECT_GROUP, "UNDEFINED_GROUP"));
    assertThat(
        ccae, hasUndefinedReference(filename, SERVICE_OBJECT_GROUP, "UNDEFINED_GROUP_MIXED"));

    assertThat(c, hasIpAccessList(services, accepts(dns, null, c)));
    assertThat(c, hasIpAccessList(services, accepts(customPort, null, c)));
    assertThat(c, hasIpAccessList(services, accepts(customPortInRange, null, c)));
    assertThat(c, hasIpAccessList(mixedGroup, accepts(customNestedPort, null, c)));
    assertThat(c, hasIpAccessList(services, not(accepts(otherFlow, null, c))));
  }

  @Test
  public void testIosLoggingOnDefault() throws IOException {
    Configuration loggingOnOmitted = parseConfig("iosLoggingOnOmitted");
    assertThat(loggingOnOmitted, hasVendorFamily(hasCisco(hasLogging(isOn()))));
  }

  @Test
  public void testIosAccessVlan() throws IOException {
    String hostname = "ios-access-vlan";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasInterface("Ethernet1", hasAccessVlan(1)));
    assertThat(c, hasInterface("Ethernet2", hasAccessVlan(nullValue())));
    assertThat(c, hasInterface("Ethernet3", hasAccessVlan(nullValue())));
  }

  @Test
  public void testIosAclInRouteMap() throws IOException {
    String hostname = "ios-acl-in-routemap";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasRouteFilterList("10", permits(Prefix.parse("10.0.0.0/8"))));
    assertThat(c, hasRouteFilterList("10", permits(Prefix.parse("10.1.0.0/16"))));
    assertThat(
        c, hasRouteFilterList("10", RouteFilterListMatchers.rejects(Prefix.parse("10.0.0.0/7"))));
    assertThat(
        c,
        hasIpAccessList(
            "10",
            accepts(
                Flow.builder()
                    .setSrcIp(Ip.parse("10.1.1.1"))
                    .setDstIp(Ip.parse("11.1.1.1"))
                    .setIngressNode(hostname)
                    .setTag("test")
                    .build(),
                "Ethernet1",
                c)));
    assertThat(
        c,
        hasIpAccessList(
            "10",
            rejects(
                Flow.builder()
                    .setSrcIp(Ip.parse("11.1.1.1"))
                    .setDstIp(Ip.parse("10.1.1.1"))
                    .setIngressNode(hostname)
                    .setTag("test")
                    .build(),
                "Ethernet1",
                c)));
    // Check Ipv6 as well
    assertThat(c, hasRoute6FilterList("v6list", permits(new Prefix6("::FFFF:10.0.0.0/105"))));
    assertThat(
        c,
        hasRoute6FilterList(
            "v6list", Route6FilterListMatchers.rejects(new Prefix6("::FFFF:10.0.0.0/103"))));
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
  public void testIosBfdTemplate() throws IOException {
    String hostname = "ios-bfd-template";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ccae, hasNumReferrers(filename, BFD_TEMPLATE, "bfd-template-unused", 0));
    assertThat(ccae, hasNumReferrers(filename, BFD_TEMPLATE, "bfd-template-used", 1));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, BFD_TEMPLATE, "bfd-template-undefined", INTERFACE_BFD_TEMPLATE));
  }

  @Test
  public void testIosBgpPrefixListReferences() throws IOException {
    String hostname = "ios_bgp_prefix_list_references";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "pl4in", 1));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "pl4out", 1));
    assertThat(ccae, hasNumReferrers(filename, PREFIX6_LIST, "pl6in", 1));
    assertThat(ccae, hasNumReferrers(filename, PREFIX6_LIST, "pl6out", 1));
  }

  @Test
  public void testIosIbgpMissingUpdateSource() throws IOException {
    /*
    r1 is missing update-source, but session should still be established between r1 and r2. Both
    redistribute static routes, so should see BGP routes on both for the other's static route.
    */
    String testrigName = "ibgp-no-update-source";
    List<String> configurationNames = ImmutableList.of("r1", "r2");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    // Confirm that BGP peer on r1 is missing its local IP, as expected
    Prefix r1NeighborPeerAddress = Prefix.parse("2.2.2.2/32");
    Configuration r1 = batfish.loadConfigurations().get("r1");
    SortedMap<Prefix, BgpActivePeerConfig> r1Peers =
        r1.getVrfs().get(DEFAULT_VRF_NAME).getBgpProcess().getActiveNeighbors();
    assertTrue(r1Peers.containsKey(r1NeighborPeerAddress));
    assertThat(r1Peers.get(r1NeighborPeerAddress).getLocalIp(), nullValue());

    /*
    r1 has a static route to 7.7.7.7/32; r2 has a static route to 8.8.8.8/32. Confirm that both
    received a BGP route to the other's static route.
    */
    batfish.computeDataPlane();
    IncrementalDataPlane dp = (IncrementalDataPlane) batfish.loadDataPlane();
    Set<AbstractRoute> r1Routes = dp.getRibs().get("r1").get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> r2Routes = dp.getRibs().get("r2").get(DEFAULT_VRF_NAME).getRoutes();
    assertThat(r1Routes, hasItem(isBgpv4RouteThat(hasPrefix(Prefix.parse("8.8.8.8/32")))));
    assertThat(r2Routes, hasItem(isBgpv4RouteThat(hasPrefix(Prefix.parse("7.7.7.7/32")))));
  }

  /**
   * Test EIGRP address family configured within another process EIGRP configuration can declare a
   * process that is nested in the configuration of another process. The processes are not connected
   * in any way. The nesting is only one layer.
   */
  @Test
  public void testIosEigrpAddressFamily() throws IOException {
    Configuration c = parseConfig("ios-eigrp-address-family");

    /* Confirm both processes are present */
    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(1L))));
    assertThat(c, ConfigurationMatchers.hasVrf("vrf-name", hasEigrpProcesses(hasKey(2L))));

    /* Confirm interfaces were matched */
    assertThat(c, hasInterface("Ethernet0", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));
    assertThat(c, hasInterface("Ethernet1", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(2L))));
  }

  /**
   * Test EIGRP autonomous-system stanza Cisco does not recommend configuring the autonomous system
   * number this way, but it is possible.
   */
  @Test
  public void testIosEigrpAsn() throws IOException {
    Configuration c = parseConfig("ios-eigrp-asn");

    /* Confirm both processes are present */
    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(1L))));
    assertThat(c, ConfigurationMatchers.hasVrf("vrf-name", hasEigrpProcesses(hasKey(2L))));

    /* Confirm interfaces were matched */
    assertThat(c, hasInterface("Ethernet0", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));
    assertThat(c, hasInterface("Ethernet1", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(2L))));
  }

  /** Test classic EIGRP process with passive interfaces */
  @Test
  public void testIosEigrpClassic() throws IOException {
    Configuration c = parseConfig("ios-eigrp-classic");

    /* Confirm process is present */
    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(1L))));

    /* Confirm interfaces were matched */
    assertThat(c, hasInterface("Ethernet0", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));
    assertThat(c, hasInterface("Ethernet1", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));
    assertThat(c, hasInterface("Ethernet2", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));

    /* Passive interfaces are configured correctly */
    assertThat(
        c, hasInterface("Ethernet0", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(false))));
    assertThat(
        c, hasInterface("Ethernet1", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(false))));
    assertThat(
        c, hasInterface("Ethernet2", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(true))));
  }

  /** Test mixing named and classic EIGRP processes in separate VRFs */
  @Test
  public void testIosEigrpMixed() throws IOException {
    Configuration c = parseConfig("ios-eigrp-mixed");

    /* Confirm classic mode networks are configured correctly */
    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(1L))));
    assertThat(c, hasInterface("Ethernet0", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));

    /* Confirm named mode networks are configured correctly */
    assertThat(c, ConfigurationMatchers.hasVrf("vrf-name", hasEigrpProcesses(hasKey(2L))));
    assertThat(c, hasInterface("Ethernet1", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(2L))));
  }

  /** Test multiple classic EIGRP processes in the same VRF */
  @Test
  public void testIosEigrpMultiple() throws IOException {
    Configuration c = parseConfig("ios-eigrp-multiple");

    /* Confirm both processes are present */
    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(1L))));
    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(2L))));

    /* Confirm interfaces were matched */
    assertThat(c, hasInterface("Ethernet0", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(1L))));
    assertThat(c, hasInterface("Ethernet1", hasEigrp(EigrpInterfaceSettingsMatchers.hasAsn(2L))));
  }

  /** Test named EIGRP process with passive interfaces */
  @Test
  public void testIosEigrpNamed() throws IOException {
    Configuration c = parseConfig("ios-eigrp-named");

    /* Confirm process is present */
    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(2L))));

    /* Passive interfaces are configured correctly */
    assertThat(
        c, hasInterface("Ethernet3", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(false))));
    assertThat(
        c, hasInterface("Ethernet4", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(false))));
    assertThat(
        c, hasInterface("Ethernet5", hasEigrp(EigrpInterfaceSettingsMatchers.hasPassive(true))));
  }

  /**
   * Test EIGRP into EIGRP route redistribution. Checks if routing policy accepts EIGRP routes and
   * sets metric correctly.
   */
  @Test
  public void testIosEigrpRedistributeEigrp() throws IOException {
    Configuration c = parseConfig("ios-eigrp-redistribute-eigrp");

    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(1L))));
    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(2L))));
    String exportPolicyName =
        c.getVrfs().get(DEFAULT_VRF_NAME).getEigrpProcesses().get(2L).getExportPolicy();
    assertThat(exportPolicyName, notNullValue());
    RoutingPolicy routingPolicy = c.getRoutingPolicies().get(exportPolicyName);
    assertThat(routingPolicy, notNullValue());

    EigrpExternalRoute.Builder outputRouteBuilder = EigrpExternalRoute.builder();
    outputRouteBuilder
        .setDestinationAsn(1L)
        .setNetwork(Prefix.parse("1.0.0.0/32"))
        .setProcessAsn(2L);

    EigrpMetric originalMetric =
        EigrpMetric.builder()
            .setBandwidth(2e9)
            .setDelay(4e5)
            .setMode(EigrpProcessMode.CLASSIC)
            .build();
    assertNotNull(originalMetric);

    // VirtualEigrpProcess sets metric to route metric by default
    outputRouteBuilder.setEigrpMetric(originalMetric);

    EigrpInternalRoute originalRoute =
        EigrpInternalRoute.builder()
            .setAdmin(90)
            .setEigrpMetric(originalMetric)
            .setNetwork(outputRouteBuilder.getNetwork())
            .setProcessAsn(1L)
            .build();
    assertNotNull(originalRoute);

    // Check if routingPolicy accepts EIGRP route and sets correct metric from original route
    assertTrue(
        routingPolicy.process(
            originalRoute, outputRouteBuilder, null, DEFAULT_VRF_NAME, Direction.OUT));
    assertThat(outputRouteBuilder.build(), hasEigrpMetric(originalRoute.getEigrpMetric()));
  }

  /**
   * Test EIGRP route redistribution. Redistributes a connected route and checks if routing policy
   * accepts OSPF routes given "redistribute ospf 1"
   */
  @Test
  public void testIosEigrpRedistribution() throws IOException {
    Configuration c = parseConfig("ios-eigrp-redistribution");
    long asn = 1L;

    assertThat(c, hasDefaultVrf(hasEigrpProcesses(hasKey(asn))));
    String exportPolicyName =
        c.getVrfs().get(DEFAULT_VRF_NAME).getEigrpProcesses().get(asn).getExportPolicy();
    assertThat(exportPolicyName, notNullValue());
    RoutingPolicy routingPolicy = c.getRoutingPolicies().get(exportPolicyName);
    assertThat(routingPolicy, notNullValue());

    EigrpExternalRoute.Builder outputRouteBuilder = EigrpExternalRoute.builder();
    outputRouteBuilder
        .setDestinationAsn(asn)
        .setNetwork(Prefix.parse("1.0.0.0/32"))
        .setProcessAsn(asn);
    EigrpMetric.Builder metricBuilder = EigrpMetric.builder().setMode(EigrpProcessMode.CLASSIC);

    // Check if routingPolicy accepts connected route and sets correct metric
    assertTrue(
        routingPolicy.process(
            new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "Loopback0"),
            outputRouteBuilder,
            null,
            DEFAULT_VRF_NAME,
            Direction.OUT));
    assertThat(
        outputRouteBuilder.build(),
        hasEigrpMetric(requireNonNull(metricBuilder.setBandwidth(1E5).setDelay(1E8).build())));

    // Check if routingPolicy rejects RIP route
    assertFalse(
        routingPolicy.process(
            new RipInternalRoute(
                Prefix.parse("2.2.2.2/32"), Ip.parse("3.3.3.3"), 1, 1, Route.UNSET_ROUTE_TAG),
            outputRouteBuilder,
            null,
            DEFAULT_VRF_NAME,
            Direction.OUT));

    // Check if routingPolicy accepts OSPF route and sets correct default metric
    assertTrue(
        routingPolicy.process(
            OspfIntraAreaRoute.builder()
                .setNetwork(Prefix.parse("4.4.4.4/32"))
                .setAdmin(1)
                .setMetric(1)
                .setArea(1L)
                .build(),
            outputRouteBuilder,
            null,
            DEFAULT_VRF_NAME,
            Direction.OUT));
    assertThat(
        outputRouteBuilder.build(),
        hasEigrpMetric(requireNonNull(metricBuilder.setBandwidth(2E5).setDelay(2E8).build())));
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

    IpAccessList eth2Acl = c.getAllInterfaces().get(eth2Name).getOutgoingFilter();
    IpAccessList eth3Acl = c.getAllInterfaces().get(eth3Name).getOutgoingFilter();

    /* Check that expected traffic is permitted/denied */
    Flow permittedByBoth =
        Flow.builder()
            .setSrcIp(Ip.parse("1.1.1.1"))
            .setDstIp(Ip.parse("2.2.2.2"))
            .setIpProtocol(IpProtocol.TCP)
            .setSrcPort(NamedPort.EPHEMERAL_LOWEST.number())
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
            .setSrcIp(Ip.parse("1.1.1.2"))
            .setDstIp(Ip.parse("2.2.2.2"))
            .setIpProtocol(IpProtocol.TCP)
            .setSrcPort(NamedPort.EPHEMERAL_LOWEST.number())
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
            .setSrcIp(Ip.parse("1.1.1.1"))
            .setDstIp(Ip.parse("2.2.2.2"))
            .setIpProtocol(IpProtocol.TCP)
            .setSrcPort(NamedPort.EPHEMERAL_LOWEST.number())
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

    assertThat(
        c,
        hasInterface(
            "GigabitEthernet0/0",
            hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(1L)))));
    assertThat(
        c,
        hasInterface(
            "GigabitEthernet0/1",
            hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(1000L)))));
    assertThat(
        c,
        hasInterface(
            "FastEthernet0/1",
            hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(10L)))));
    assertThat(
        c,
        hasInterface(
            "Loopback0", hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(500L)))));
    assertThat(
        c,
        hasInterface(
            "Tunnel0", hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(5000L)))));
  }

  @Test
  public void testIosInterfaceSpeed() throws IOException {
    Configuration c = parseConfig("ios-interface-speed");

    assertThat(c, hasInterface("GigabitEthernet0/0", hasBandwidth(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/0", hasSpeed(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/1", hasBandwidth(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/1", hasSpeed(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/2", hasBandwidth(100E6D)));
    assertThat(c, hasInterface("GigabitEthernet0/2", hasSpeed(100E6D)));
    assertThat(c, hasInterface("Loopback0", hasBandwidth(8E9D)));
    assertThat(c, hasInterface("Tunnel0", hasBandwidth(1E5D)));
  }

  @Test
  public void testIosInterfaceStandby() throws IOException {
    Configuration c = parseConfig("ios-interface-standby");
    Interface i = c.getAllInterfaces().get("Ethernet0");

    assertThat(
        c,
        ConfigurationMatchers.hasTrackingGroups(
            equalTo(ImmutableMap.of("1", new TrackInterface("Tunnel1")))));
    assertThat(
        i,
        hasHsrpGroup(
            1001,
            HsrpGroupMatchers.hasAuthentication(
                sha256Digest("012345678901234567890123456789012345678"))));
    assertThat(i, hasHsrpGroup(1001, HsrpGroupMatchers.hasHelloTime(500)));
    assertThat(i, hasHsrpGroup(1001, HsrpGroupMatchers.hasHoldTime(2000)));
    assertThat(i, hasHsrpGroup(1001, HsrpGroupMatchers.hasIp(Ip.parse("10.0.0.1"))));
    assertThat(i, hasHsrpGroup(1001, HsrpGroupMatchers.hasPriority(105)));
    assertThat(i, hasHsrpGroup(1001, HsrpGroupMatchers.hasPreempt()));
    assertThat(
        i,
        hasHsrpGroup(
            1001, hasTrackActions(equalTo(ImmutableMap.of("1", new DecrementPriority(20))))));
    assertThat(i, hasHsrpVersion("2"));
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
  public void testIosIpRouteVrf() throws IOException {
    String hostname = "ios-ip-route-vrf";
    String vrf = "management";
    Configuration c = parseConfig(hostname);

    assertThat(
        c,
        hasVrfs(
            hasEntry(
                equalTo(vrf),
                hasStaticRoutes(
                    equalTo(
                        ImmutableSet.of(
                            StaticRoute.builder()
                                .setAdministrativeCost(1)
                                .setNetwork(Prefix.ZERO)
                                .setNextHopIp(Ip.parse("1.2.3.4"))
                                .build()))))));
  }

  @Test
  public void testIosIsisConfigOnInterface() throws IOException {
    String hostname = "ios-isis";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasInterface("Loopback0", hasIsis(hasLevel2(notNullValue()))));
    assertThat(c, hasInterface("Loopback100", hasIsis(nullValue())));
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
  public void testIosNativeVlan() throws IOException {
    String hostname = "ios-native-vlan";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasInterface("Ethernet1", hasNativeVlan(nullValue())));
    assertThat(c, hasInterface("Ethernet2", hasNativeVlan(3)));
    assertThat(c, hasInterface("Ethernet3", hasNativeVlan(1)));
  }

  @Test
  public void testIosNeighborDefaultOriginateRouteMapsAsGenerationPolicies() throws IOException {
    /*
                              Listener 2
                                  |
                               (Peer 2)
       Listener 1 -- (Peer 1) Originator (Peer 3) -- Listener 3

     All listeners have EBGP sessions established with the originator, and the originator has
     default-originate configured for all three peers. Some peers also have a route-map configured
     with default-originate, which will act as a generation policy for the default route.

      - Peer 1: default-originate has no route-map attached, but there is a route-map configured as
        an export policy that denies the default route. However, the default-originate route doesn't
        go through configured export policies, so should reach listener 1.

      - Peer 2: default-originate has a route-map attached that permits routes to 1.2.3.4; the
      originator has a static route to 1.2.3.4, so default route should still be generated.

      - Peer 3: default-originate has a route-map attached that permits routes to 5.6.7.8; the
      originator has no route to 5.6.7.8, so no default route should appear on listener 3.
    */
    String testrigName = "ios-default-originate";
    String originatorName = "originator";
    String l1Name = "listener1";
    String l2Name = "listener2";
    String l3Name = "listener3";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    TESTRIGS_PREFIX + testrigName,
                    ImmutableList.of(originatorName, l1Name, l2Name, l3Name))
                .build(),
            _folder);

    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> l1Routes = dp.getRibs().get(l1Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> l2Routes = dp.getRibs().get(l2Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> l3Routes = dp.getRibs().get(l3Name).get(DEFAULT_VRF_NAME).getRoutes();

    // Listener 1
    Ip originatorId = Ip.parse("1.1.1.1");
    Ip originatorIp = Ip.parse("10.1.1.1");
    Long originatorAs = 1L;
    Bgpv4Route expected =
        Bgpv4Route.builder()
            .setNetwork(Prefix.ZERO)
            .setNextHopIp(originatorIp)
            .setAdmin(20)
            .setAsPath(AsPath.of(AsSet.of(originatorAs)))
            .setLocalPreference(100)
            .setOriginatorIp(originatorId)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.BGP)
            .setSrcProtocol(RoutingProtocol.BGP)
            .setReceivedFromIp(originatorIp)
            .build();
    assertThat(l1Routes, hasItem(expected));

    // Listener 2
    originatorIp = Ip.parse("10.2.2.1");
    expected =
        expected.toBuilder().setNextHopIp(originatorIp).setReceivedFromIp(originatorIp).build();
    assertThat(l2Routes, hasItem(expected));

    // Listener 3
    assertThat(l3Routes, not(hasItem(hasPrefix(Prefix.ZERO))));
  }

  @Test
  public void testIosRedistributeStaticDefaultWithDefaultOriginatePolicy() throws IOException {
    /*
       Listener 1 -- (Peer 1) Originator (Peer 2) -- Listener 2

     Both listeners have EBGP sessions established with the originator. The originator has
     default-originate configured on both peers, but with a generation policy that only matches
     routes to 1.2.3.4, so default routes won't be generated. This way both peers' BGP export
     policies include the default route export policy, but we don't have to worry about the
     default-originate route overwriting other default routes in neighbors' RIBs.

     The originator has a static default route and redistributes it to BGP on both peers with a
     route-map that sets community 50, so we can be certain of the route's origin in neighbors.

     Peer 1 has no outbound route-map, so the static route should be redistributed to listener 1.

     Peer 2 has an outbound route-map that denies 0.0.0.0/0, so no default route on listener 2.
    */
    String testrigName = "ios-default-originate";
    String originatorName = "originator-static-route";
    String l1Name = "listener1";
    String l2Name = "listener2";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    TESTRIGS_PREFIX + testrigName, ImmutableList.of(originatorName, l1Name, l2Name))
                .build(),
            _folder);

    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> l1Routes = dp.getRibs().get(l1Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> l2Routes = dp.getRibs().get(l2Name).get(DEFAULT_VRF_NAME).getRoutes();

    Ip originatorId = Ip.parse("1.1.1.1");
    Ip originatorIp = Ip.parse("10.1.1.1");
    long originatorAs = 1L;
    Bgpv4Route redistributedStaticRoute =
        Bgpv4Route.builder()
            .setCommunities(ImmutableSet.of(StandardCommunity.of(50)))
            .setNetwork(Prefix.ZERO)
            .setNextHopIp(originatorIp)
            .setAdmin(20)
            .setAsPath(AsPath.of(AsSet.of(originatorAs)))
            .setLocalPreference(100)
            .setOriginatorIp(originatorId)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setSrcProtocol(RoutingProtocol.BGP)
            .setReceivedFromIp(originatorIp)
            .build();

    // Listener 1 should have received the static route
    assertThat(l1Routes, hasItem(redistributedStaticRoute));

    // Listener 2 should not have received the static route since export policy prevents it
    assertThat(l2Routes, not(hasItem(hasPrefix(Prefix.ZERO))));
  }

  @Test
  public void testIosObjectGroupNetwork() throws IOException {
    String hostname = "ios-object-group-network";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    Ip ognWildcardIp = Ip.parse("1.128.0.0");
    Ip ognHostIp = Ip.parse("2.0.0.1");
    Ip ognUnmatchedIp = Ip.parse("2.0.0.0");

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
        Flow.builder()
            .setTag("")
            .setIngressNode("")
            .setIpProtocol(IpProtocol.ICMP)
            .setIcmpCode(0)
            .setIcmpType(0)
            .build();
    Flow tcpFlow =
        Flow.builder()
            .setTag("")
            .setIngressNode("")
            .setIpProtocol(IpProtocol.TCP)
            .setSrcPort(0)
            .setDstPort(0)
            .build();

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
                                                        hasDstPorts(
                                                            hasItem(new SubRange(65500)))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.DOMAIN.number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.CMDtcp_OR_SYSLOGudp
                                                                        .number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasDstPorts(
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
                                                        hasDstPorts(
                                                            hasItem(new SubRange(65501)))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.NTP.number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.SNMPTRAP
                                                                        .number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.CMDtcp_OR_SYSLOGudp
                                                                        .number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.TFTP
                                                                        .number()))))))))))))))));
  }

  @Test
  public void testIosOspfDefaultOriginateAlways() throws IOException {
    /*   ________      ________      ________
        |   R1   |    |        |    |   R2   |
        | Area 0 |----|  ABR   |----| Area 1 |
        |        |    |        |    |  NSSA  |
         --------      --------      --------
      ABR has `default-information originate always` configured at the process level, so R1 should
      install a default route to the ABR even though the ABR has no default route of its own. R2
      should not install a default route because it's in an NSSA.
    */

    String testrigName = "ospf-default-originate";
    String area0Name = "ios-area-0";
    String area1NssaName = "ios-area-1-nssa";
    String originatorName = "originator-always";
    List<String> configurationNames = ImmutableList.of(area0Name, area1NssaName, originatorName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration abr = configurations.get(originatorName);

    // Sanity check: ensure the ABR has a generated default route in its OSPF process
    Set<GeneratedRoute> abrOspfGeneratedRoutes =
        abr.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    assertThat(abrOspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));

    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> area0Routes = dp.getRibs().get(area0Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> area1NssaRoutes =
        dp.getRibs().get(area1NssaName).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> abrRoutes =
        dp.getRibs().get(originatorName).get(DEFAULT_VRF_NAME).getRoutes();

    // R1 should have default route, but the ABR and R2 should not
    assertThat(
        area0Routes, hasItem(allOf(hasPrefix(Prefix.ZERO), hasProtocol(RoutingProtocol.OSPF_E2))));
    assertThat(area1NssaRoutes, not(hasItem(hasPrefix(Prefix.ZERO))));
    assertThat(abrRoutes, not(hasItem(hasPrefix(Prefix.ZERO))));
  }

  @Test
  public void testIosOspfDefaultOriginateNoRoute() throws IOException {
    /*   ________      ________      ________
        |   R1   |    |        |    |   R2   |
        | Area 0 |----|  ABR   |----| Area 1 |
        |        |    |        |    |  NSSA  |
         --------      --------      --------
      ABR has `default-information originate` configured at the process level, but no default route
      in its RIB, so it should not export a default route.
    */

    String testrigName = "ospf-default-originate";
    String area0Name = "ios-area-0";
    String area1NssaName = "ios-area-1-nssa";
    String originatorName = "originator-no-route";
    List<String> configurationNames = ImmutableList.of(area0Name, area1NssaName, originatorName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration abr = configurations.get(originatorName);

    // Sanity check: ensure the ABR has a generated default route in its OSPF process
    Set<GeneratedRoute> abrOspfGeneratedRoutes =
        abr.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    assertThat(abrOspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));

    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> area0Routes = dp.getRibs().get(area0Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> area1NssaRoutes =
        dp.getRibs().get(area1NssaName).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> abrRoutes =
        dp.getRibs().get(originatorName).get(DEFAULT_VRF_NAME).getRoutes();

    // None should have default route
    assertThat(area0Routes, not(hasItem(hasPrefix(Prefix.ZERO))));
    assertThat(area1NssaRoutes, not(hasItem(hasPrefix(Prefix.ZERO))));
    assertThat(abrRoutes, not(hasItem(hasPrefix(Prefix.ZERO))));
  }

  @Test
  public void testIosOspfDefaultOriginateStaticRoute() throws IOException {
    /*   ________      ________      ________
        |   R1   |    |        |    |   R2   |
        | Area 0 |----|  ABR   |----| Area 1 |
        |        |    |        |    |  NSSA  |
         --------      --------      --------
      ABR has `default-information originate` configured at the process level and a statically
      configured default route, so it should advertise the default route to R1.
    */

    String testrigName = "ospf-default-originate";
    String area0Name = "ios-area-0";
    String area1NssaName = "ios-area-1-nssa";
    String originatorName = "originator-static-route";
    List<String> configurationNames = ImmutableList.of(area0Name, area1NssaName, originatorName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration abr = configurations.get(originatorName);

    // Sanity check: ensure the ABR has a generated default route in its OSPF process
    Set<GeneratedRoute> abrOspfGeneratedRoutes =
        abr.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    assertThat(abrOspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));

    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> area0Routes = dp.getRibs().get(area0Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> area1NssaRoutes =
        dp.getRibs().get(area1NssaName).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> abrRoutes =
        dp.getRibs().get(originatorName).get(DEFAULT_VRF_NAME).getRoutes();

    // R1 should have default route, but the ABR and R2 should not
    assertThat(
        area0Routes, hasItem(allOf(hasPrefix(Prefix.ZERO), hasProtocol(RoutingProtocol.OSPF_E2))));
    assertThat(area1NssaRoutes, not(hasItem(hasPrefix(Prefix.ZERO))));
    assertThat(
        abrRoutes, hasItem(allOf(hasPrefix(Prefix.ZERO), hasProtocol(RoutingProtocol.STATIC))));
  }

  @Test
  public void testIosOspfDefaultOriginateLoop() throws IOException {
    /*
    Setup: 2-node network in OSPF area 0.
      - R1 has `default-information originate always` configured at the process level
      - R2 has `default-information originate` configured at the process level
    R2 should have a default route to R1; R1 shouldn't have a default route in its main RIB.
    */

    String testrigName = "ospf-default-originate-loop";
    String r1Name = "ios-originator-1-always";
    String r2Name = "ios-originator-2";
    List<String> configurationNames = ImmutableList.of(r1Name, r2Name);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    // Sanity check: both devices have a generated default route in their OSPF processes
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration r1 = configurations.get(r1Name);
    Configuration r2 = configurations.get(r2Name);
    Set<GeneratedRoute> r1OspfGeneratedRoutes =
        r1.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    Set<GeneratedRoute> r2OspfGeneratedRoutes =
        r2.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    assertThat(r1OspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));
    assertThat(r2OspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));

    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> r1Routes = dp.getRibs().get(r1Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> r2Routes = dp.getRibs().get(r2Name).get(DEFAULT_VRF_NAME).getRoutes();

    // R2 should have default route, R1 should not
    assertThat(
        r2Routes, hasItem(allOf(hasPrefix(Prefix.ZERO), hasProtocol(RoutingProtocol.OSPF_E2))));
    assertThat(r1Routes, not(hasItem(hasPrefix(Prefix.ZERO))));
  }

  @Test
  public void testIosOspfDistributeListReference() throws IOException {
    String hostname = "ios-ospf-distribute-list";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "aclin", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "aclout", 1));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "plin", 1));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "plout", 1));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rmin", 1));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rmout", 1));
  }

  @Test
  public void testIosEigrpDistributeList() {
    CiscoConfiguration c =
        parseCiscoConfig("ios-eigrp-distribute-list", ConfigurationFormat.CISCO_IOS);

    assertThat(
        c.getDefaultVrf().getEigrpProcesses().get(1L).getOutboundInterfaceDistributeLists(),
        equalTo(
            ImmutableMap.of(
                "GigabitEthernet0/0",
                new DistributeList("2", DistributeListFilterType.ACCESS_LIST))));
  }

  @Test
  public void testIosEigrpMarkForRouting() throws IOException {
    Configuration c = parseConfig("ios-eigrp-distribute-list");

    assertThat(c.getRouteFilterLists(), hasKey("2"));
  }

  @Test
  public void testIosEigrpNeighborConfigs() throws IOException {
    Configuration c = parseConfig("ios-eigrp-classic");

    assertThat(
        c.getDefaultVrf().getEigrpProcesses().get(1L).getNeighbors(),
        equalTo(
            ImmutableMap.of(
                "Ethernet0",
                    EigrpNeighborConfig.builder()
                        .setInterfaceName("Ethernet0")
                        .setPassive(false)
                        .setHostname("ios-eigrp-classic")
                        .setVrfName("default")
                        .setIp(Ip.parse("10.0.0.1"))
                        .build(),
                "Ethernet1",
                    EigrpNeighborConfig.builder()
                        .setInterfaceName("Ethernet1")
                        .setPassive(false)
                        .setHostname("ios-eigrp-classic")
                        .setVrfName("default")
                        .setIp(Ip.parse("10.0.1.1"))
                        .build(),
                "Ethernet2",
                    EigrpNeighborConfig.builder()
                        .setInterfaceName("Ethernet2")
                        .setPassive(true)
                        .setHostname("ios-eigrp-classic")
                        .setVrfName("default")
                        .setIp(Ip.parse("10.0.2.1"))
                        .build())));
  }

  @Test
  public void testIosOspfDistributeList() {
    CiscoConfiguration c = parseCiscoConfig("iosOspfDistributeList", ConfigurationFormat.CISCO_IOS);
    DistributeList globalInPrefix =
        new DistributeList("block_5", DistributeListFilterType.PREFIX_LIST);
    DistributeList globalOutPrefix =
        new DistributeList("block_6", DistributeListFilterType.PREFIX_LIST);
    DistributeList dlGig0InPrefix =
        new DistributeList("block_1", DistributeListFilterType.PREFIX_LIST);
    DistributeList dlGig1InPrefix =
        new DistributeList("block_2", DistributeListFilterType.PREFIX_LIST);
    DistributeList dlGig0OutPrefix =
        new DistributeList("block_3", DistributeListFilterType.PREFIX_LIST);
    DistributeList dlGig1OutPrefix =
        new DistributeList("block_4", DistributeListFilterType.PREFIX_LIST);

    DistributeList globalInRm = new DistributeList("rm1", DistributeListFilterType.ROUTE_MAP);
    DistributeList globalOutRm = new DistributeList("rm2", DistributeListFilterType.ROUTE_MAP);

    DistributeList globalInAcl = new DistributeList("acl3", DistributeListFilterType.ACCESS_LIST);
    DistributeList globalOutAcl = new DistributeList("acl4", DistributeListFilterType.ACCESS_LIST);
    DistributeList dlGig0InAcl = new DistributeList("acl1", DistributeListFilterType.ACCESS_LIST);
    DistributeList dlGig1OutAcl = new DistributeList("acl2", DistributeListFilterType.ACCESS_LIST);

    org.batfish.representation.cisco.OspfProcess ospfProcessPrefix =
        c.getDefaultVrf().getOspfProcesses().get("1");

    assertThat(ospfProcessPrefix.getInboundGlobalDistributeList(), equalTo(globalInPrefix));
    assertThat(ospfProcessPrefix.getOutboundGlobalDistributeList(), equalTo(globalOutPrefix));
    assertThat(
        ospfProcessPrefix.getInboundInterfaceDistributeLists(),
        equalTo(
            ImmutableMap.of(
                "GigabitEthernet0/0", dlGig0InPrefix, "GigabitEthernet1/0", dlGig1InPrefix)));
    assertThat(
        ospfProcessPrefix.getOutboundInterfaceDistributeLists(),
        equalTo(
            ImmutableMap.of(
                "GigabitEthernet0/0", dlGig0OutPrefix, "GigabitEthernet1/0", dlGig1OutPrefix)));

    org.batfish.representation.cisco.OspfProcess ospfProcessRouteMap =
        c.getDefaultVrf().getOspfProcesses().get("2");

    assertThat(ospfProcessRouteMap.getInboundGlobalDistributeList(), equalTo(globalInRm));
    assertThat(ospfProcessRouteMap.getOutboundGlobalDistributeList(), equalTo(globalOutRm));

    org.batfish.representation.cisco.OspfProcess ospfProcessAcl =
        c.getDefaultVrf().getOspfProcesses().get("3");

    assertThat(ospfProcessAcl.getInboundGlobalDistributeList(), equalTo(globalInAcl));
    assertThat(ospfProcessAcl.getOutboundGlobalDistributeList(), equalTo(globalOutAcl));
    assertThat(
        ospfProcessAcl.getInboundInterfaceDistributeLists(),
        equalTo(ImmutableMap.of("GigabitEthernet0/0", dlGig0InAcl)));
    assertThat(
        ospfProcessAcl.getOutboundInterfaceDistributeLists(),
        equalTo(ImmutableMap.of("GigabitEthernet0/0", dlGig1OutAcl)));
  }

  @Test
  public void testIosOspfDistributeListPrefixList() throws IOException {
    Configuration c = parseConfig("iosOspfDistributeListPrefixList");
    String distListPolicyName0 = "~OSPF_DIST_LIST_default_1_GigabitEthernet0/0~";
    String distListPolicyName1 = "~OSPF_DIST_LIST_default_1_GigabitEthernet1/0~";

    assertThat(c.getRoutingPolicies(), hasKey(distListPolicyName0));

    assertThat(
        c.getAllInterfaces().get("GigabitEthernet0/0").getOspfInboundDistributeListPolicy(),
        equalTo(distListPolicyName0));
    assertThat(
        c.getAllInterfaces().get("GigabitEthernet1/0").getOspfInboundDistributeListPolicy(),
        equalTo(distListPolicyName1));

    RoutingPolicy routingPolicy0 = c.getRoutingPolicies().get(distListPolicyName0);
    List<org.batfish.datamodel.routing_policy.statement.Statement> statements =
        routingPolicy0.getStatements();

    assertThat(
        statements,
        equalTo(
            ImmutableList.of(
                new If(
                    new Conjunction(
                        ImmutableList.of(
                            new MatchPrefixSet(
                                DestinationNetwork.instance(), new NamedPrefixSet("filter_2")),
                            new MatchPrefixSet(
                                DestinationNetwork.instance(), new NamedPrefixSet("filter_1")))),
                    ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                    ImmutableList.of(Statements.ExitReject.toStaticStatement())))));

    assertFalse(
        routingPolicy0.process(
            OspfIntraAreaRoute.builder().setNetwork(Prefix.parse("1.1.1.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
            null,
            "default",
            Direction.IN));
    assertFalse(
        routingPolicy0.process(
            OspfIntraAreaRoute.builder().setNetwork(Prefix.parse("2.2.2.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
            null,
            "default",
            Direction.IN));
    assertTrue(
        routingPolicy0.process(
            OspfIntraAreaRoute.builder().setNetwork(Prefix.parse("3.3.3.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
            null,
            "default",
            Direction.IN));

    assertThat(
        c.getRoutingPolicies().get(distListPolicyName1).getStatements(),
        equalTo(
            ImmutableList.of(
                new If(
                    new Conjunction(
                        ImmutableList.of(
                            new MatchPrefixSet(
                                DestinationNetwork.instance(), new NamedPrefixSet("filter_2")),
                            new MatchPrefixSet(
                                DestinationNetwork.instance(), new NamedPrefixSet("filter_1")))),
                    ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                    ImmutableList.of(Statements.ExitReject.toStaticStatement())))));
  }

  @Test
  public void testIosOspfDistributeListPrefixListInterface() throws IOException {
    Configuration c = parseConfig("iosOspfDistributeListPrefixListInterface");
    String distListPolicyName = "~OSPF_DIST_LIST_default_1_GigabitEthernet0/0~";

    assertThat(c.getRoutingPolicies(), hasKey(distListPolicyName));

    assertThat(
        c.getAllInterfaces().get("GigabitEthernet0/0").getOspfInboundDistributeListPolicy(),
        equalTo(distListPolicyName));

    RoutingPolicy routingPolicy = c.getRoutingPolicies().get(distListPolicyName);
    List<org.batfish.datamodel.routing_policy.statement.Statement> statements =
        routingPolicy.getStatements();

    assertThat(
        statements,
        equalTo(
            ImmutableList.of(
                new If(
                    new MatchPrefixSet(
                        DestinationNetwork.instance(), new NamedPrefixSet("filter_1")),
                    ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                    ImmutableList.of(Statements.ExitReject.toStaticStatement())))));

    assertFalse(
        routingPolicy.process(
            OspfIntraAreaRoute.builder().setNetwork(Prefix.parse("1.1.1.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
            null,
            "default",
            Direction.IN));
    assertTrue(
        routingPolicy.process(
            OspfIntraAreaRoute.builder().setNetwork(Prefix.parse("2.2.2.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
            null,
            "default",
            Direction.IN));
  }

  @Test
  public void testIosOspfDistributeListPrefixListGlobal() throws IOException {
    Configuration c = parseConfig("iosOspfDistributeListPrefixListGlobal");
    String distListPoicyName = "~OSPF_DIST_LIST_default_1_GigabitEthernet0/0~";

    assertThat(c.getRoutingPolicies(), hasKey(distListPoicyName));

    assertThat(
        c.getAllInterfaces().get("GigabitEthernet0/0").getOspfInboundDistributeListPolicy(),
        equalTo(distListPoicyName));

    RoutingPolicy routingPolicy = c.getRoutingPolicies().get(distListPoicyName);
    List<org.batfish.datamodel.routing_policy.statement.Statement> statements =
        routingPolicy.getStatements();

    assertThat(
        statements,
        equalTo(
            ImmutableList.of(
                new If(
                    new MatchPrefixSet(
                        DestinationNetwork.instance(), new NamedPrefixSet("filter_2")),
                    ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                    ImmutableList.of(Statements.ExitReject.toStaticStatement())))));

    assertTrue(
        routingPolicy.process(
            OspfIntraAreaRoute.builder().setNetwork(Prefix.parse("1.1.1.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
            null,
            "default",
            Direction.IN));
    assertFalse(
        routingPolicy.process(
            OspfIntraAreaRoute.builder().setNetwork(Prefix.parse("2.2.2.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
            null,
            "default",
            Direction.IN));
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
    assertThat(
        manual.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(), equalTo(10e6d));

    Configuration defaults = parseConfig("iosOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_IOS)));
  }

  @Test
  public void testIosOspfStubSettings() throws IOException {
    Configuration c = parseConfig("ios-ospf-stub-settings");

    // Check correct stub types are assigned
    assertThat(c, hasDefaultVrf(hasOspfProcess("1", hasArea(0L, hasStubType(StubType.NONE)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess("1", hasArea(1L, hasStubType(StubType.NSSA)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess("1", hasArea(2L, hasStubType(StubType.NSSA)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess("1", hasArea(3L, hasStubType(StubType.STUB)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess("1", hasArea(4L, hasStubType(StubType.STUB)))));
    assertThat(c, hasDefaultVrf(hasOspfProcess("1", hasArea(5L, hasStubType(StubType.NONE)))));

    // Check for stub subtype settings
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                "1",
                hasArea(
                    1L, hasNssa(hasDefaultOriginateType(OspfDefaultOriginateType.INTER_AREA))))));
    assertThat(
        c, hasDefaultVrf(hasOspfProcess("1", hasArea(1L, hasNssa(hasSuppressType3(false))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                "1",
                hasArea(2L, hasNssa(hasDefaultOriginateType(OspfDefaultOriginateType.NONE))))));
    assertThat(c, hasDefaultVrf(hasOspfProcess("1", hasArea(2L, hasNssa(hasSuppressType3())))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                "1", hasArea(3L, hasStub(StubSettingsMatchers.hasSuppressType3(false))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess("1", hasArea(4L, hasStub(StubSettingsMatchers.hasSuppressType3())))));
  }

  @Test
  public void testIosBgpDistributeList() throws IOException {
    /*
         r1 -- advertiser -- r2
    The advertiser redistributes static routes 1.2.3.4 and 5.6.7.8 into BGP, but has outbound
    distribute-lists configured for both r1 and r2:
    - Routes to r1 are filtered by standard access-list 1, which permits everything but 1.2.3.4
    - Routes to r2 are filtered by extended access-list 100, which denies everything but 5.6.7.8
    */
    String testrigName = "bgp-distribute-list";
    String advertiserName = "advertiser";
    String r1Name = "r1";
    String r2Name = "r2";
    List<String> configurationNames = ImmutableList.of(advertiserName, r1Name, r2Name);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    /* Confirm access list uses are counted correctly */
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    String filename = "configs/" + advertiserName;
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "1", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "100", 1));

    /* Ensure both neighbors have 5.6.7.8 but not 1.2.3.4 */
    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> r1Routes = dp.getRibs().get(r1Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> r2Routes = dp.getRibs().get(r2Name).get(DEFAULT_VRF_NAME).getRoutes();
    assertThat(r1Routes, not(hasItem(hasPrefix(Prefix.parse("1.2.3.4/32")))));
    assertThat(r2Routes, not(hasItem(hasPrefix(Prefix.parse("1.2.3.4/32")))));
    assertThat(r1Routes, hasItem(hasPrefix(Prefix.parse("5.6.7.8/32"))));
    assertThat(r2Routes, hasItem(hasPrefix(Prefix.parse("5.6.7.8/32"))));
  }

  @Test
  public void testIosBgpPrefixList() throws IOException {
    String hostname = "ios-prefix-list";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm prefix list uses are counted correctly */
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "pre_list", 3));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "pre_list_unused", 0));

    /* Confirm undefined prefix lists are detected in different contexts */
    /* Bgp neighbor context */
    assertThat(ccae, hasUndefinedReference(filename, PREFIX_LIST, "pre_list_undef1"));
    /* Route-map match context */
    assertThat(ccae, hasUndefinedReference(filename, PREFIX_LIST, "pre_list_undef2"));

    /*
     Neighbor 1.2.3.4 uses pre_list to filter both inbound and outbound routes. Test that both
     generated policies permit 10.1.1.0/24 and not other routes.
    */
    Ip peerAddress = Ip.parse("1.2.3.4");
    Configuration c = batfish.loadConfigurations().get(hostname);
    String generatedImportPolicyName =
        computeBgpPeerImportPolicyName(DEFAULT_VRF_NAME, peerAddress.toString());
    String generatedExportPolicyName =
        computeBgpPeerExportPolicyName(DEFAULT_VRF_NAME, peerAddress.toString());
    RoutingPolicy importPolicy = c.getRoutingPolicies().get(generatedImportPolicyName);
    RoutingPolicy exportPolicy = c.getRoutingPolicies().get(generatedExportPolicyName);
    assertThat(importPolicy, notNullValue());
    assertThat(exportPolicy, notNullValue());

    Prefix permittedPrefix = Prefix.parse("10.1.1.0/24");
    Bgpv4Route.Builder r =
        Bgpv4Route.builder()
            .setOriginatorIp(peerAddress)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.IBGP);
    Bgpv4Route permittedRoute = r.setNetwork(permittedPrefix).build();
    Bgpv4Route unmatchedRoute = r.setNetwork(Prefix.parse("10.1.0.0/16")).build();
    assertThat(
        importPolicy.process(
            permittedRoute,
            permittedRoute.toBuilder(),
            peerAddress,
            DEFAULT_VRF_NAME,
            Direction.IN),
        equalTo(true));
    assertThat(
        exportPolicy.process(
            permittedRoute,
            permittedRoute.toBuilder(),
            peerAddress,
            DEFAULT_VRF_NAME,
            Direction.OUT),
        equalTo(true));
    assertThat(
        importPolicy.process(
            unmatchedRoute,
            unmatchedRoute.toBuilder(),
            peerAddress,
            DEFAULT_VRF_NAME,
            Direction.IN),
        equalTo(false));
    assertThat(
        exportPolicy.process(
            unmatchedRoute,
            unmatchedRoute.toBuilder(),
            peerAddress,
            DEFAULT_VRF_NAME,
            Direction.OUT),
        equalTo(false));
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
  public void testIosRouteMapSetWeight() throws IOException {
    // Config contains a route-map SET_WEIGHT with one line, "set weight 20"
    String hostname = "ios-route-map-set-weight";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    RoutingPolicy setWeightPolicy =
        batfish.loadConfigurations().get(hostname).getRoutingPolicies().get("SET_WEIGHT");
    Bgpv4Route r =
        Bgpv4Route.builder()
            .setWeight(1)
            .setNetwork(Prefix.ZERO)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    Bgpv4Route.Builder transformedRoute = r.toBuilder();

    assertThat(
        setWeightPolicy.process(r, transformedRoute, Ip.ZERO, DEFAULT_VRF_NAME, Direction.IN),
        equalTo(true));
    assertThat(transformedRoute.build(), hasWeight(20));
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
  public void testIosXrPrefixSet() throws IOException {
    String hostname = "ios-xr-prefix-set";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasConfigurationFormat(ConfigurationFormat.CISCO_IOS_XR));

    Prefix permittedPrefix = Prefix.parse("1.2.3.4/32");
    Prefix permittedPrefix2 = Prefix.parse("1.2.3.5/32");
    Prefix rejectedPrefix = Prefix.parse("2.0.0.0/8");

    StaticRoute permittedRoute =
        StaticRoute.builder().setAdministrativeCost(1).setNetwork(permittedPrefix).build();
    StaticRoute permittedRoute2 =
        StaticRoute.builder().setAdministrativeCost(1).setNetwork(permittedPrefix2).build();
    StaticRoute rejectedRoute =
        StaticRoute.builder().setAdministrativeCost(1).setNetwork(rejectedPrefix).build();

    // The route-policy accepts and rejects the same prefixes.
    RoutingPolicy rp = c.getRoutingPolicies().get("rp_ip");
    assertThat(rp, notNullValue());
    assertTrue(
        rp.process(permittedRoute, Bgpv4Route.builder(), null, DEFAULT_VRF_NAME, Direction.OUT));
    assertTrue(
        rp.process(permittedRoute2, Bgpv4Route.builder(), null, DEFAULT_VRF_NAME, Direction.OUT));
    assertFalse(
        rp.process(rejectedRoute, Bgpv4Route.builder(), null, DEFAULT_VRF_NAME, Direction.OUT));

    // The BGP peer export policy also accepts and rejects the same prefixes.
    BgpActivePeerConfig bgpCfg =
        c.getDefaultVrf().getBgpProcess().getActiveNeighbors().get(Prefix.parse("10.1.1.1/32"));
    assertThat(bgpCfg, notNullValue());
    RoutingPolicy bgpRpOut =
        c.getRoutingPolicies().get(bgpCfg.getIpv4UnicastAddressFamily().getExportPolicy());
    assertThat(bgpRpOut, notNullValue());

    assertTrue(
        bgpRpOut.process(
            permittedRoute, Bgpv4Route.builder(), null, DEFAULT_VRF_NAME, Direction.OUT));
    assertTrue(
        bgpRpOut.process(
            permittedRoute2, Bgpv4Route.builder(), null, DEFAULT_VRF_NAME, Direction.OUT));
    assertFalse(
        bgpRpOut.process(
            rejectedRoute, Bgpv4Route.builder(), null, DEFAULT_VRF_NAME, Direction.OUT));
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
  public void testIosTrack() throws IOException {
    String hostname = "ios-track";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ccae, hasNumReferrers(filename, TRACK, "1", 1));
    assertThat(ccae, hasNumReferrers(filename, TRACK, "2", 0));
    assertThat(ccae, hasUndefinedReference(filename, TRACK, "3"));
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
    Ip on1Ip = Ip.parse("1.2.3.4");
    Ip on2IpStart = Ip.parse("2.2.2.0");
    Ip on2IpEnd = Ip.parse("2.2.2.255");
    Ip inlineIp = Ip.parse("3.3.3.3");

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
                "1",
                hasArea(
                    1L, hasSummary(Prefix.parse("10.0.0.0/16"), isAdvertised(equalTo(false)))))));
    assertThat(
        manual,
        hasDefaultVrf(
            hasOspfProcess(
                "1", hasArea(1L, hasSummary(Prefix.parse("10.0.0.0/16"), hasMetric(100L))))));

    Configuration defaults = parseConfig("iosOspfCostDefaults");

    assertThat(
        defaults,
        hasDefaultVrf(
            hasOspfProcess(
                "1", hasArea(1L, hasSummary(Prefix.parse("10.0.0.0/16"), isAdvertised())))));
    assertThat(
        defaults,
        hasDefaultVrf(
            hasOspfProcess(
                "1",
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
            .setSrcPort(0)
            .setDstPort(0)
            .build();
    Flow flowInspect =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setTag("")
            .setIpProtocol(IpProtocol.UDP)
            .setSrcPort(0)
            .setDstPort(0)
            .build();
    Flow flowDrop =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setTag("")
            .setIpProtocol(IpProtocol.ICMP)
            .setIcmpType(0)
            .setIcmpCode(0)
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
  public void testIosXrCommunitySet() throws IOException {
    Configuration c = parseConfig("ios-xr-community-set");
    CommunityList list = c.getCommunityLists().get("set1");

    assertThat(
        list,
        CommunityListMatchers.hasLine(
            0,
            CommunityListLineMatchers.hasMatchCondition(
                isRegexCommunitySet(hasRegex("^1234:.*")))));
    assertThat(
        list,
        CommunityListMatchers.hasLine(
            1,
            CommunityListLineMatchers.hasMatchCondition(
                equalTo(new CommunityHalvesExpr(RangeCommunityHalf.ALL, RangeCommunityHalf.ALL)))));
    assertThat(
        list,
        CommunityListMatchers.hasLine(
            2,
            CommunityListLineMatchers.hasMatchCondition(
                equalTo(new LiteralCommunity(StandardCommunity.parse("1:2"))))));
    assertThat(
        list,
        CommunityListMatchers.hasLine(
            3,
            CommunityListLineMatchers.hasMatchCondition(
                equalTo(
                    new CommunityHalvesExpr(
                        RangeCommunityHalf.ALL, new LiteralCommunityHalf(3))))));
    assertThat(
        list,
        CommunityListMatchers.hasLine(
            4,
            CommunityListLineMatchers.hasMatchCondition(
                equalTo(
                    new CommunityHalvesExpr(
                        new LiteralCommunityHalf(4), RangeCommunityHalf.ALL)))));
    assertThat(
        list,
        CommunityListMatchers.hasLine(
            5,
            CommunityListLineMatchers.hasMatchCondition(
                equalTo(
                    new CommunityHalvesExpr(
                        new LiteralCommunityHalf(6),
                        new RangeCommunityHalf(new SubRange(100, 103)))))));
  }

  @Test
  public void testIosXrOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("iosxrOspfCost");
    assertThat(
        manual.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(), equalTo(10e6d));

    Configuration defaults = parseConfig("iosxrOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_IOS_XR)));
  }

  @Test
  public void testNxosOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("nxosOspfCost");
    assertThat(
        manual.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(), equalTo(10e9d));

    Configuration defaults = parseConfig("nxosOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(),
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
    Map<Ip, Set<String>> ipOwners = IpOwners.computeIpNodeOwners(configurations, true);
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        BgpTopologyUtils.initBgpTopology(configurations, ipOwners, false, null).getGraph();

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
    assertThat(aristaEnabled, hasMultipathEbgp(false));
    assertThat(nxosDisabled, hasMultipathEbgp(false));
    assertThat(nxosEnabled, hasMultipathEbgp(false));
  }

  @Test
  public void testBgpProcnum() throws IOException {
    for (String hostname : ImmutableList.of("ios-bgp-procnum-dotted", "ios-bgp-procnum-long")) {
      Configuration c = parseConfig(hostname);
      assertThat(
          hostname,
          c.getVrfs()
              .get(DEFAULT_VRF_NAME)
              .getBgpProcess()
              .getActiveNeighbors()
              .get(Prefix.parse("2.2.2.3/32"))
              .getLocalAs(),
          equalTo(4123456789L));
    }
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
    batfish.computeDataPlane(); // compute and cache the dataPlane

    // Check that 1.1.1.1/32 appears on r3
    SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs =
        batfish.loadDataPlane().getRibs();
    Set<AbstractRoute> r3Routes = ribs.get("r3").get(DEFAULT_VRF_NAME).getRoutes();
    Set<Prefix> r3Prefixes =
        r3Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Prefix r1Loopback = Prefix.parse("1.1.1.1/32");
    assertTrue(r3Prefixes.contains(r1Loopback));

    // check that private AS is present in path in received 1.1.1.1/32 advert on r2
    Set<AbstractRoute> r2Routes = ribs.get("r2").get(DEFAULT_VRF_NAME).getRoutes();
    boolean r2HasPrivate =
        r2Routes.stream()
            .filter(r -> r.getNetwork().equals(r1Loopback))
            .flatMap(r -> ((Bgpv4Route) r).getAsPath().getAsSets().stream())
            .flatMap(asSet -> asSet.getAsns().stream())
            .anyMatch(AsPath::isPrivateAs);
    assertTrue(r2HasPrivate);

    // check that private AS is absent from path in received 1.1.1.1/32 advert on r3
    boolean r3HasPrivate =
        r3Routes.stream()
            .filter(a -> a.getNetwork().equals(r1Loopback))
            .flatMap(r -> ((Bgpv4Route) r).getAsPath().getAsSets().stream())
            .flatMap(asSet -> asSet.getAsns().stream())
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
    Map<String, CommunityList> iosCommunityLists = iosCommunityListConfig.getCommunityLists();

    Configuration eosCommunityListConfig = configurations.get(eosName);
    Map<String, CommunityList> eosCommunityLists = eosCommunityListConfig.getCommunityLists();

    Configuration nxosCommunityListConfig = configurations.get(nxosName);
    Map<String, CommunityList> nxosCommunityLists = nxosCommunityListConfig.getCommunityLists();

    Community iosImpliedStd = communityListToCommunity(iosCommunityLists, "40");
    String iosRegexImpliedExp = communityListToRegex(iosCommunityLists, "400");
    Community iosStdAsnn = communityListToCommunity(iosCommunityLists, "std_as_nn");
    String iosRegexExpAsnn = communityListToRegex(iosCommunityLists, "exp_as_nn");
    Community iosStdGshut = communityListToCommunity(iosCommunityLists, "std_gshut");
    String iosRegexExpGshut = communityListToRegex(iosCommunityLists, "exp_gshut");
    Community iosStdInternet = communityListToCommunity(iosCommunityLists, "std_internet");
    String iosRegexExpInternet = communityListToRegex(iosCommunityLists, "exp_internet");
    Community iosStdLocalAs = communityListToCommunity(iosCommunityLists, "std_local_AS");
    String iosRegexExpLocalAs = communityListToRegex(iosCommunityLists, "exp_local_AS");
    Community iosStdNoAdv = communityListToCommunity(iosCommunityLists, "std_no_advertise");
    String iosRegexExpNoAdv = communityListToRegex(iosCommunityLists, "exp_no_advertise");
    Community iosStdNoExport = communityListToCommunity(iosCommunityLists, "std_no_export");
    String iosRegexExpNoExport = communityListToRegex(iosCommunityLists, "exp_no_export");

    Community eosStd = communityListToCommunity(eosCommunityLists, "eos_std");
    String eosRegexExp = communityListToRegex(eosCommunityLists, "eos_exp");
    Community eosStdGshut = communityListToCommunity(eosCommunityLists, "eos_std_gshut");
    Community eosStdInternet = communityListToCommunity(eosCommunityLists, "eos_std_internet");
    Community eosStdLocalAs = communityListToCommunity(eosCommunityLists, "eos_std_local_AS");
    Community eosStdNoAdv = communityListToCommunity(eosCommunityLists, "eos_std_no_adv");
    Community eosStdNoExport = communityListToCommunity(eosCommunityLists, "eos_std_no_export");
    String eosRegexExpMulti = communityListToRegex(eosCommunityLists, "eos_exp_multi");

    Community nxosStd = communityListToCommunity(nxosCommunityLists, "nxos_std");
    String nxosRegexExp = communityListToRegex(nxosCommunityLists, "nxos_exp");
    Community nxosStdInternet = communityListToCommunity(nxosCommunityLists, "nxos_std_internet");
    Community nxosStdLocalAs = communityListToCommunity(nxosCommunityLists, "nxos_std_local_AS");
    Community nxosStdNoAdv = communityListToCommunity(nxosCommunityLists, "nxos_std_no_adv");
    Community nxosStdNoExport = communityListToCommunity(nxosCommunityLists, "nxos_std_no_export");
    String nxosRegexExpMulti = communityListToRegex(nxosCommunityLists, "nxos_exp_multi");

    // check literal communities
    assertThat(iosImpliedStd, equalTo(StandardCommunity.of(4294967295L)));
    assertThat(iosStdAsnn, equalTo(StandardCommunity.parse("65535:65535")));
    assertThat(eosStd, equalTo(StandardCommunity.parse("0:1")));
    assertThat(nxosStd, equalTo(StandardCommunity.parse("65535:65535")));

    // check regex communities
    assertThat(iosRegexImpliedExp, equalTo("4294967295"));
    assertThat(iosRegexExpAsnn, equalTo("65535:65535"));
    assertThat(eosRegexExp, equalTo("1"));
    /*
     *  TODO: https://github.com/batfish/batfish/issues/1993
     *  (Should be three regexes: '0:1', '0:2, '0:3')
     */
    assertThat(eosRegexExpMulti, equalTo("0:10:20:3"));
    assertThat(nxosRegexExp, equalTo("65535:65535"));
    /*
     *  TODO: https://github.com/batfish/batfish/issues/1993
     *  (Should be three regexes: '0:1', '0:2, '0:3')
     */
    assertThat(nxosRegexExpMulti, equalTo("0:10:20:3"));

    // Check well known community regexes are generated properly
    assertThat(iosStdInternet, equalTo(StandardCommunity.of(WellKnownCommunity.INTERNET)));
    assertThat(iosStdNoAdv, equalTo(StandardCommunity.of(WellKnownCommunity.NO_ADVERTISE)));
    assertThat(iosStdNoExport, equalTo(StandardCommunity.of(WellKnownCommunity.NO_EXPORT)));
    assertThat(iosStdGshut, equalTo(StandardCommunity.of(WellKnownCommunity.GRACEFUL_SHUTDOWN)));
    assertThat(
        iosStdLocalAs, equalTo(StandardCommunity.of(WellKnownCommunity.NO_EXPORT_SUBCONFED)));
    assertThat(eosStdInternet, equalTo(StandardCommunity.of(WellKnownCommunity.INTERNET)));
    assertThat(eosStdNoAdv, equalTo(StandardCommunity.of(WellKnownCommunity.NO_ADVERTISE)));
    assertThat(eosStdNoExport, equalTo(StandardCommunity.of(WellKnownCommunity.NO_EXPORT)));
    assertThat(eosStdGshut, equalTo(StandardCommunity.of(WellKnownCommunity.GRACEFUL_SHUTDOWN)));
    assertThat(
        eosStdLocalAs, equalTo(StandardCommunity.of(WellKnownCommunity.NO_EXPORT_SUBCONFED)));
    // NX-OS does not support gshut
    assertThat(nxosStdInternet, equalTo(StandardCommunity.of(WellKnownCommunity.INTERNET)));
    assertThat(nxosStdNoAdv, equalTo(StandardCommunity.of(WellKnownCommunity.NO_ADVERTISE)));
    assertThat(nxosStdNoExport, equalTo(StandardCommunity.of(WellKnownCommunity.NO_EXPORT)));
    assertThat(
        nxosStdLocalAs, equalTo(StandardCommunity.of(WellKnownCommunity.NO_EXPORT_SUBCONFED)));

    // make sure well known communities in expanded lists are not actually converted
    assertThat(iosRegexExpGshut, equalTo("gshut"));
    assertThat(iosRegexExpInternet, equalTo("internet"));
    assertThat(iosRegexExpLocalAs, equalTo("local-AS"));
    assertThat(iosRegexExpNoAdv, equalTo("no-advertise"));
    assertThat(iosRegexExpNoExport, equalTo("no-export"));

    // check conjunctions of communities are converted correctly
    assertThat(
        ((LiteralCommunityConjunction)
                communityListToMatchCondition(iosCommunityLists, "std_community"))
            .getRequiredCommunities(),
        equalTo(
            ImmutableSet.of(
                StandardCommunity.of(1L), StandardCommunity.of(2L), StandardCommunity.of(3L))));
    assertThat(
        ((LiteralCommunityConjunction)
                communityListToMatchCondition(eosCommunityLists, "eos_std_multi"))
            .getRequiredCommunities(),
        equalTo(
            ImmutableSet.of(
                StandardCommunity.of(1L), StandardCommunity.of(2L), StandardCommunity.of(3L))));
    assertThat(
        ((LiteralCommunityConjunction)
                communityListToMatchCondition(nxosCommunityLists, "nxos_std_multi"))
            .getRequiredCommunities(),
        equalTo(
            ImmutableSet.of(
                StandardCommunity.of(1L), StandardCommunity.of(2L), StandardCommunity.of(3L))));
  }

  @Test
  public void testToIpsecPolicies() throws IOException {
    Configuration c = parseConfig("ios-crypto-map");
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
                            .setSrcIps(IpWildcard.parse("1.1.1.1").toIpSpace())
                            .setDstIps(IpWildcard.parse("2.2.2.2").toIpSpace())
                            .build()))
                .build(),
            IpAccessListLine.accepting()
                .setMatchCondition(
                    new MatchHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(IpWildcard.parse("2.2.2.2").toIpSpace())
                            .setDstIps(IpWildcard.parse("1.1.1.1").toIpSpace())
                            .build()))
                .build());

    assertThat(
        c,
        hasIpsecPeerConfig(
            "~IPSEC_PEER_CONFIG:mymap:20_TenGigabitEthernet0/0~",
            isIpsecStaticPeerConfigThat(
                allOf(
                    hasDestinationAddress(Ip.parse("3.4.5.6")),
                    IpsecPeerConfigMatchers.hasIkePhase1Policy("ISAKMP-PROFILE-MATCHED"),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("~IPSEC_PHASE2_POLICY:mymap:20~"),
                    hasSourceInterface("TenGigabitEthernet0/0"),
                    hasPolicyAccessList(hasLines(equalTo(expectedAclLines))),
                    hasLocalAddress(Ip.parse("2.3.4.6"))))));
    assertThat(
        c,
        hasIpsecPeerConfig(
            "~IPSEC_PEER_CONFIG:mymap:10_TenGigabitEthernet0/0~",
            isIpsecStaticPeerConfigThat(
                allOf(
                    hasDestinationAddress(Ip.parse("1.2.3.4")),
                    IpsecPeerConfigMatchers.hasIkePhase1Policy("ISAKMP-PROFILE"),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("~IPSEC_PHASE2_POLICY:mymap:10~"),
                    hasSourceInterface("TenGigabitEthernet0/0"),
                    hasPolicyAccessList(hasLines(equalTo(expectedAclLines))),
                    hasLocalAddress(Ip.parse("2.3.4.6"))))));

    assertThat(
        c,
        hasIpsecPeerConfig(
            "~IPSEC_PEER_CONFIG:mymap:30:15_TenGigabitEthernet0/0~",
            isIpsecDynamicPeerConfigThat(
                allOf(
                    IpsecPeerConfigMatchers.hasIkePhase1Policies(
                        equalTo(ImmutableList.of("ISAKMP-PROFILE", "ISAKMP-PROFILE-MATCHED"))),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("~IPSEC_PHASE2_POLICY:mymap:30:15~"),
                    hasSourceInterface("TenGigabitEthernet0/0"),
                    hasPolicyAccessList(hasLines(equalTo(expectedAclLines))),
                    hasLocalAddress(Ip.parse("2.3.4.6")),
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
                    hasSourceInterface("TenGigabitEthernet0/0"),
                    hasPolicyAccessList(hasLines(equalTo(expectedAclLines))),
                    hasLocalAddress(Ip.parse("2.3.4.6")),
                    hasTunnelInterface(nullValue())))));

    assertThat(
        c,
        hasIpsecPeerConfig(
            "Tunnel1",
            isIpsecStaticPeerConfigThat(
                allOf(
                    hasDestinationAddress(Ip.parse("1.2.3.4")),
                    IpsecPeerConfigMatchers.hasIkePhase1Policy("ISAKMP-PROFILE"),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("IPSEC-PROFILE1"),
                    hasSourceInterface("TenGigabitEthernet0/0"),
                    hasLocalAddress(Ip.parse("2.3.4.6")),
                    hasTunnelInterface(equalTo("Tunnel1"))))));

    assertThat(
        c,
        hasIpsecPeerConfig(
            "Tunnel2",
            isIpsecStaticPeerConfigThat(
                allOf(
                    hasDestinationAddress(Ip.parse("1.2.3.4")),
                    IpsecPeerConfigMatchers.hasIkePhase1Policy("ISAKMP-PROFILE"),
                    IpsecPeerConfigMatchers.hasIpsecPolicy("IPSEC-PROFILE1"),
                    hasSourceInterface("TenGigabitEthernet0/0"),
                    hasLocalAddress(Ip.parse("2.3.4.6")),
                    hasTunnelInterface(equalTo("Tunnel2"))))));
  }

  @Test
  public void testTunnelMode() {
    CiscoConfiguration c = parseCiscoConfig("ios-tunnel-mode", ConfigurationFormat.CISCO_IOS);

    assertThat(c.getInterfaces().get("Tunnel1").getTunnel().getMode(), equalTo(TunnelMode.GRE));
    assertThat(c.getInterfaces().get("Tunnel2").getTunnel().getMode(), equalTo(TunnelMode.GRE));
    assertThat(c.getInterfaces().get("Tunnel3").getTunnel().getMode(), equalTo(TunnelMode.IPSEC));
  }

  @Test
  public void testGreTunnelConversion() throws IOException {
    Configuration c = parseConfig("ios-tunnel-mode");

    Builder builder = TunnelConfiguration.builder().setSourceAddress(Ip.parse("2.3.4.6"));

    assertThat(
        c.getAllInterfaces().get("Tunnel1").getTunnelConfig(),
        equalTo(builder.setDestinationAddress(Ip.parse("1.2.3.4")).build()));
    assertThat(
        c.getAllInterfaces().get("Tunnel2").getTunnelConfig(),
        equalTo(builder.setDestinationAddress(Ip.parse("1.2.3.5")).build()));
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
  public void testIsakmpKeyIos() throws IOException {
    Configuration c = parseConfig("ios-crypto");

    assertThat(
        c,
        hasIkePhase1Policy(
            "~ISAKMP_KEY_IpWildcardIpSpace{ipWildcard=1.1.1.0/24}~",
            allOf(
                hasIkePhase1Key(
                    allOf(
                        IkePhase1KeyMatchers.hasKeyHash(
                            CommonUtil.sha256Digest("psk1" + CommonUtil.salt())),
                        IkePhase1KeyMatchers.hasRemoteIdentity(
                            IpWildcard.parse("1.1.1.0/24").toIpSpace()),
                        hasKeyType(IkeKeyType.PRE_SHARED_KEY_UNENCRYPTED))),
                hasRemoteIdentity(equalTo(IpWildcard.parse("1.1.1.0/24").toIpSpace())),
                hasIkePhase1Proposals(equalTo(ImmutableList.of("20"))))));

    assertThat(
        c,
        hasIkePhase1Policy(
            "~ISAKMP_KEY_IpWildcardIpSpace{ipWildcard=2.2.2.2}~",
            allOf(
                hasIkePhase1Key(
                    allOf(
                        IkePhase1KeyMatchers.hasKeyHash("FLgBaJHXdYY_AcHZZMgQ_RhTDJXHUBAAB"),
                        IkePhase1KeyMatchers.hasRemoteIdentity(
                            IpWildcard.parse("2.2.2.2").toIpSpace()),
                        hasKeyType(IkeKeyType.PRE_SHARED_KEY_ENCRYPTED))),
                hasRemoteIdentity(equalTo(IpWildcard.parse("2.2.2.2").toIpSpace())),
                hasIkePhase1Proposals(equalTo(ImmutableList.of("20"))))));
  }

  @Test
  public void testIsakmpPolicyIos() throws IOException {
    Configuration c = parseConfig("ios-crypto");
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
    assertThat(
        c,
        hasIkePhase1Proposal(
            "30",
            IkePhase1ProposalMatchers.hasAuthenticationMethod(
                IkeAuthenticationMethod.RSA_ENCRYPTED_NONCES)));
  }

  @Test
  public void testIsakmpProfile() throws IOException {
    Configuration c = parseConfig("ios-crypto");
    // test for IKE phase 1 policy
    assertThat(
        c,
        hasIkePhase1Policy(
            "ISAKMP-PROFILE-ADDRESS",
            allOf(
                hasIkePhase1Key(
                    IkePhase1KeyMatchers.hasKeyHash(
                        CommonUtil.sha256Digest("psk1" + CommonUtil.salt()))),
                hasRemoteIdentity(containsIp(Ip.parse("1.2.3.4"))),
                hasSelfIdentity(equalTo(Ip.parse("2.3.4.6"))),
                hasLocalInterface(equalTo("TenGigabitEthernet0/0")),
                // TODO: filter proposals during conversion so that they match IKE Phase 1 policy's
                // key type
                hasIkePhase1Proposals(equalTo(ImmutableList.of("10", "20", "30"))))));

    assertThat(
        c,
        hasIkePhase1Policy(
            "ISAKMP-PROFILE-INTERFACE",
            allOf(
                hasIkePhase1Key(
                    IkePhase1KeyMatchers.hasKeyHash(
                        CommonUtil.sha256Digest("psk1" + CommonUtil.salt()))),
                hasRemoteIdentity(containsIp(Ip.parse("1.2.3.4"))),
                hasSelfIdentity(equalTo(Ip.parse("2.3.4.6"))),
                hasLocalInterface(equalTo("TenGigabitEthernet0/0")),
                hasIkePhase1Proposals(equalTo(ImmutableList.of("10", "20", "30"))))));
  }

  @Test
  public void testCiscoCryptoRsa() throws IOException {
    Configuration c = parseConfig("ios-crypto-rsa");

    assertThat(
        c,
        hasIkePhase1Policy(
            "~RSA_PUB_testrsa~",
            allOf(
                hasIkePhase1Key(
                    allOf(
                        hasKeyType(IkeKeyType.RSA_PUB_KEY),
                        IkePhase1KeyMatchers.hasRemoteIdentity(Ip.parse("1.2.3.4").toIpSpace()))),
                hasRemoteIdentity(containsIp(Ip.parse("1.2.3.4"))),
                hasLocalInterface(equalTo(UNSET_LOCAL_INTERFACE)),
                hasIkePhase1Proposals(equalTo(ImmutableList.of("10"))))));
  }

  @Test
  public void testCiscoCryptoRsaReferrers() throws IOException {
    String hostname = "ios-crypto-rsa";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    /* Confirm RSA pubkey is referenced */
    assertThat(
        batfish.loadConvertConfigurationAnswerElementOrReparse(),
        hasNumReferrers("configs/" + hostname, NAMED_RSA_PUB_KEY, "testrsa", 1));
  }

  private static CommunitySetExpr communityListToMatchCondition(
      Map<String, CommunityList> communityLists, String communityName) {
    return communityLists.get(communityName).getLines().get(0).getMatchCondition();
  }

  private static Community communityListToCommunity(
      Map<String, CommunityList> communityLists, String communityName) {
    return communityLists
        .get(communityName)
        .getLines()
        .get(0)
        .getMatchCondition()
        .asLiteralCommunities(null)
        .first();
  }

  private static @Nonnull String communityListToRegex(
      Map<String, CommunityList> communityLists, String communityName) {
    return ((RegexCommunitySet)
            communityLists.get(communityName).getLines().get(0).getMatchCondition())
        .getRegex();
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
            hasBgpProcess(
                hasActiveNeighbor(
                    neighborWithRemoteAs,
                    hasIpv4UnicastAddressFamily(
                        hasAddressFamilyCapabilites(hasAllowRemoteAsOut(true)))))));
  }

  @Test
  public void testEosPortChannel() throws IOException {
    String hostname = "eos-port-channel";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);

    assertThat(c, hasInterface("Ethernet0", hasBandwidth(40E9D)));
    assertThat(c, hasInterface("Ethernet0", hasSpeed(40E9D)));
    assertThat(c, hasInterface("Ethernet1", hasBandwidth(40E9D)));
    assertThat(c, hasInterface("Ethernet1", hasSpeed(40E9D)));
    assertThat(c, hasInterface("Ethernet2", hasBandwidth(40E9D)));
    assertThat(c, hasInterface("Ethernet2", hasSpeed(40E9D)));
    assertThat(c, hasInterface("Port-Channel1", hasBandwidth(80E9D)));
    assertThat(c, hasInterface("Port-Channel1", hasSpeed(nullValue())));
    assertThat(c, hasInterface("Port-Channel2", isActive(false)));
    // 0 since no members
    assertThat(c, hasInterface("Port-Channel2", hasBandwidth(0D)));
    assertThat(c, hasInterface("Port-Channel2", hasSpeed(nullValue())));
    assertThat(
        c.getAllInterfaces().get("Port-Channel1").getDependencies(),
        equalTo(
            ImmutableSet.of(
                new Dependency("Ethernet0", DependencyType.AGGREGATE),
                new Dependency("Ethernet1", DependencyType.AGGREGATE))));
  }

  @Test
  public void testEosMlagConfig() throws IOException {
    String hostname = "eos-mlag";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);

    final String mlagName = "MLAG_DOMAIN_ID";
    assertThat(c, hasMlagConfig(mlagName, hasId(mlagName)));
    assertThat(c, hasMlagConfig(mlagName, hasPeerAddress(Ip.parse("1.1.1.3"))));
    assertThat(c, hasMlagConfig(mlagName, hasPeerInterface("Port-Channel1")));
    assertThat(c, hasMlagConfig(mlagName, MlagMatchers.hasLocalInterface("Vlan4094")));

    // Test interface config
    assertThat(c, hasInterface("Port-Channel1", hasMlagId(5)));
  }

  @Test
  public void testEosVxlan() throws IOException {
    String hostnameBase = "eos-vxlan";
    String hostnameNoLoopbackAddr = "eos-vxlan-no-loopback-address";
    String hostnameNoSourceIface = "eos-vxlan-no-source-interface";

    Batfish batfish =
        getBatfishForConfigurationNames(
            hostnameBase, hostnameNoSourceIface, hostnameNoLoopbackAddr);
    // Config with proper loopback iface, VLAN-specific unicast, explicit UDP port
    Configuration configBase = batfish.loadConfigurations().get(hostnameBase);
    assertThat(configBase, hasDefaultVrf(hasVniSettings(hasKey(10002))));
    VniSettings vnisBase = configBase.getDefaultVrf().getVniSettings().get(10002);

    // Config with no loopback address, using multicast, and default UDP port
    Configuration configNoLoopbackAddr = batfish.loadConfigurations().get(hostnameNoLoopbackAddr);
    assertThat(configNoLoopbackAddr, hasDefaultVrf(hasVniSettings(hasKey(10002))));
    VniSettings vnisNoAddr = configNoLoopbackAddr.getDefaultVrf().getVniSettings().get(10002);

    // Config with no source interface and general VXLAN unicast address
    Configuration configNoSourceIface = batfish.loadConfigurations().get(hostnameNoSourceIface);
    assertThat(configNoSourceIface, hasDefaultVrf(hasVniSettings(hasKey(10002))));
    VniSettings vnisNoIface = configNoSourceIface.getDefaultVrf().getVniSettings().get(10002);

    // Confirm VLAN-specific unicast address takes priority over the other addresses
    assertThat(vnisBase, hasBumTransportMethod(equalTo(BumTransportMethod.UNICAST_FLOOD_GROUP)));
    assertThat(vnisBase, hasBumTransportIps(contains(Ip.parse("1.1.1.10"))));
    // Confirm source address is inherited from source interface
    assertThat(vnisBase, hasSourceAddress(equalTo(Ip.parse("1.1.1.4"))));
    // Confirm explicit UDP port is used
    assertThat(vnisBase, hasUdpPort(equalTo(5555)));
    // Confirm VLAN<->VNI mapping is applied
    assertThat(vnisBase, hasVlan(equalTo(2)));

    // Confirm multicast address is present
    assertThat(vnisNoAddr, hasBumTransportMethod(equalTo(BumTransportMethod.MULTICAST_GROUP)));
    assertThat(vnisNoAddr, hasBumTransportIps(contains(Ip.parse("227.10.1.1"))));
    // Confirm no source address is present (no address specified for loopback interface)
    assertThat(vnisNoAddr, hasSourceAddress(nullValue()));
    // Confirm default UDP port is used even though none is supplied
    assertThat(vnisNoAddr, hasUdpPort(equalTo(4789)));

    // Confirm general VXLAN flood addresses are used
    assertThat(vnisNoIface, hasBumTransportMethod(equalTo(BumTransportMethod.UNICAST_FLOOD_GROUP)));
    assertThat(
        vnisNoIface,
        hasBumTransportIps(containsInAnyOrder(Ip.parse("1.1.1.5"), Ip.parse("1.1.1.6"))));
    // Confirm no source address is present (no interface is linked to the VXLAN)
    assertThat(vnisNoIface, hasSourceAddress(nullValue()));
  }

  @Test
  public void testEosVxlanMisconfig() throws IOException {
    String hostname = "eos-vxlan-misconfig";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration config = batfish.loadConfigurations().get(hostname);

    // Make sure that misconfigured VXLAN is still converted into VI model properly
    assertThat(config, hasDefaultVrf(hasVniSettings(hasKey(10002))));
    VniSettings vnisMisconfig = config.getDefaultVrf().getVniSettings().get(10002);

    // No BUM IPs specified
    assertThat(vnisMisconfig, hasBumTransportIps(emptyIterable()));
    // No source interface so no source address
    assertThat(vnisMisconfig, hasSourceAddress(nullValue()));
    // Confirm default UDP port is used
    assertThat(vnisMisconfig, hasUdpPort(equalTo(4789)));
    // Confirm VLAN<->VNI mapping is applied
    assertThat(vnisMisconfig, hasVlan(equalTo(2)));
  }

  @Test
  public void testEosTrunkGroup() throws IOException {
    String hostname = "eos_trunk_group";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);

    assertThat(
        c, hasInterface("Port-Channel1", hasAllowedVlans(IntegerSpace.of(Range.closed(1, 2)))));
    assertThat(c, hasInterface("Port-Channel2", hasAllowedVlans(IntegerSpace.of(99))));
  }

  @Test
  public void testEosVxlanCiscoConfig() {
    String hostname = "eos-vxlan";

    CiscoConfiguration config = parseCiscoConfig(hostname, ConfigurationFormat.ARISTA);

    assertThat(config, notNullValue());
    AristaEosVxlan eosVxlan = config.getEosVxlan();
    assertThat(eosVxlan, notNullValue());

    assertThat(eosVxlan.getDescription(), equalTo("vxlan vti"));
    // Confirm flood address set doesn't contain the removed address
    assertThat(
        eosVxlan.getFloodAddresses(), containsInAnyOrder(Ip.parse("1.1.1.5"), Ip.parse("1.1.1.7")));
    assertThat(eosVxlan.getMulticastGroup(), equalTo(Ip.parse("227.10.1.1")));
    assertThat(eosVxlan.getSourceInterface(), equalTo("Loopback1"));
    assertThat(eosVxlan.getUdpPort(), equalTo(5555));

    assertThat(eosVxlan.getVlanVnis(), hasEntry(equalTo(2), equalTo(10002)));

    // Confirm flood address set was overwritten as expected
    assertThat(
        eosVxlan.getVlanFloodAddresses(), hasEntry(equalTo(2), contains(Ip.parse("1.1.1.10"))));
  }

  @Test
  public void testEosVxlanReference() throws IOException {
    String hostname = "eos-vxlan";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ccae, hasNumReferrers(filename, VXLAN, "Vxlan1", 1));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "Loopback1", 2));
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

    Interface i1 = configurations.get(iosHostname).getAllInterfaces().get(i1Name);
    assertThat(i1, hasDeclaredNames("Ethernet0/0", "e0/0", "Eth0/0", "ether0/0-1"));
  }

  @Test
  public void testIosOspfPassive() throws IOException {
    String testrigName = "ios-ospf-passive";
    String host1name = "ios-ospf-passive1";
    String host2name = "ios-ospf-passive2";
    String iface1Name = "Ethernet1";
    String iface2Name = "Ethernet2";
    List<String> configurationNames = ImmutableList.of(host1name, host2name);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration c1 = configurations.get(host1name);
    Configuration c2 = configurations.get(host2name);

    // in host1, default is active which is overridden for iface1
    assertThat(c1, hasInterface(iface1Name, isOspfPassive(equalTo(true))));
    assertThat(c1, hasInterface(iface2Name, isOspfPassive(equalTo(false))));

    // in host2, default is passive which is overridden for iface1
    assertThat(c2, hasInterface(iface1Name, isOspfPassive(equalTo(false))));
    assertThat(c2, hasInterface(iface2Name, isOspfPassive(equalTo(true))));
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
    ValueGraph<IpsecPeerConfigId, IpsecSession> graph =
        IpsecUtil.initIpsecTopology(configurations).getGraph();

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
    assertThat(c, hasDefaultVrf(hasOspfProcess("1", hasAreas(hasKey(areaNum)))));
    OspfArea area = c.getDefaultVrf().getOspfProcesses().get("1").getAreas().get(areaNum);
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
    assertThat(vrf, hasOspfProcess("1", hasAreas(hasKey(areaNum))));
    OspfArea area = vrf.getOspfProcesses().get("1").getAreas().get(areaNum);
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
    OspfProcess proc = iosMaxMetric.getDefaultVrf().getOspfProcesses().get("1");
    OspfProcess procCustom = iosMaxMetricCustom.getDefaultVrf().getOspfProcesses().get("1");
    OspfProcess procOnStartup = iosMaxMetricOnStartup.getDefaultVrf().getOspfProcesses().get("1");
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
  public void testOspfNoRedistribution() throws IOException {
    /*   ________      ________      ________
        |   R1   |    |        |    |   R2   |
        | Area 0 |----|  ABR   |----| Area 1 |
        |        |    |        |    |  NSSA  |
         --------      --------      --------
      Will run this setup with two versions of the ABR, one where it has no-redistribution
      configured for area 1 and one where it doesn't. In both cases, the ABR is configured to
      redistribute connected subnets, so we should always see its loopback prefix on R1 and should
      also see it on R2 when no-redistribution is not configured.
    */

    // First snapshot: no-redistribution is not configured
    String testrigName = "ospf-no-redistribution";
    String area0Name = "ios-area-0";
    String area1NssaName = "ios-area-1-nssa";
    String abrName = "ios-abr";
    Prefix abrLoopbackPrefix = Prefix.parse("10.10.10.10/32");
    List<String> configurationNames = ImmutableList.of(area0Name, area1NssaName, abrName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration abr = configurations.get(abrName);

    // Sanity check: ensure the ABR does not have suppressType7 set for area 1
    OspfArea abrToArea1 =
        abr.getVrfs().get(DEFAULT_VRF_NAME).getInterfaces().get("Ethernet1").getOspfArea();
    assertThat(abrToArea1.getNssa(), hasSuppressType7(false));

    batfish.computeDataPlane();
    DataPlane dp = batfish.loadDataPlane();
    Set<AbstractRoute> area0Routes = dp.getRibs().get(area0Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> area1NssaRoutes =
        dp.getRibs().get(area1NssaName).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> abrRoutes = dp.getRibs().get(abrName).get(DEFAULT_VRF_NAME).getRoutes();

    // All the devices should have a route to the ABR's loopback
    assertThat(
        area0Routes,
        hasItem(allOf(hasPrefix(abrLoopbackPrefix), hasProtocol(RoutingProtocol.OSPF_E2))));
    assertThat(
        area1NssaRoutes,
        hasItem(allOf(hasPrefix(abrLoopbackPrefix), hasProtocol(RoutingProtocol.OSPF_E2))));
    assertThat(abrRoutes, hasItem(hasPrefix(abrLoopbackPrefix)));

    // Second snapshot: run the same song and dance with no-redistribution configured on the ABR
    abrName = "ios-abr-no-redistribution";
    configurationNames = ImmutableList.of(area0Name, area1NssaName, abrName);
    batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    configurations = batfish.loadConfigurations();
    abr = configurations.get(abrName);

    // This time the ABR should have suppressType7 set for area 1
    abrToArea1 = abr.getVrfs().get(DEFAULT_VRF_NAME).getInterfaces().get("Ethernet1").getOspfArea();
    assertThat(abrToArea1.getNssa(), hasSuppressType7(true));

    batfish.computeDataPlane();
    dp = batfish.loadDataPlane();
    area0Routes = dp.getRibs().get(area0Name).get(DEFAULT_VRF_NAME).getRoutes();
    area1NssaRoutes = dp.getRibs().get(area1NssaName).get(DEFAULT_VRF_NAME).getRoutes();
    abrRoutes = dp.getRibs().get(abrName).get(DEFAULT_VRF_NAME).getRoutes();

    // Now the device in area 1 should not have a route to the ABR's loopback
    assertThat(
        area0Routes,
        hasItem(allOf(hasPrefix(abrLoopbackPrefix), hasProtocol(RoutingProtocol.OSPF_E2))));
    assertThat(area1NssaRoutes, not(hasItem(hasPrefix(abrLoopbackPrefix))));
    assertThat(abrRoutes, hasItem(hasPrefix(abrLoopbackPrefix)));
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
    Interface e0Sub0 = iosMaxMetric.getAllInterfaces().get("Ethernet0/0");
    Interface e0Sub1 = iosMaxMetric.getAllInterfaces().get("Ethernet0/1");

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
    Map<String, Interface> iosRecoveryInterfaces = iosRecovery.getAllInterfaces();
    Set<String> iosRecoveryInterfaceNames = iosRecoveryInterfaces.keySet();

    assertThat("Loopback0", in(iosRecoveryInterfaceNames));
    assertThat("Loopback1", in(iosRecoveryInterfaceNames));
    assertThat("Loopback2", not(in(iosRecoveryInterfaceNames)));
    assertThat("Loopback3", in(iosRecoveryInterfaceNames));

    Set<ConcreteInterfaceAddress> l3Prefixes =
        iosRecoveryInterfaces.get("Loopback3").getAllConcreteAddresses();
    Set<ConcreteInterfaceAddress> l4Prefixes =
        iosRecoveryInterfaces.get("Loopback4").getAllConcreteAddresses();

    assertThat(ConcreteInterfaceAddress.parse("10.0.0.1/32"), not(in(l3Prefixes)));
    assertThat(ConcreteInterfaceAddress.parse("10.0.0.2/32"), in(l3Prefixes));
    assertThat("Loopback4", in(iosRecoveryInterfaceNames));
    assertThat(ConcreteInterfaceAddress.parse("10.0.0.3/32"), not(in(l4Prefixes)));
    assertThat(ConcreteInterfaceAddress.parse("10.0.0.4/32"), in(l4Prefixes));
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
    return parseTextConfigs(hostname).get(hostname.toLowerCase());
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
      Configuration configuration = configurations.get(configurationNames[i].toLowerCase());
      assertThat(configuration.getVrfs().size(), equalTo(1));
      for (Vrf vrf : configuration.getVrfs().values()) {
        assertThat(vrf.getOspfProcesses().get("1").getRfc1583Compatible(), is(expectedResults[i]));
      }
    }
  }

  @Test
  public void testAristaSubinterfaceMtu() throws IOException {
    Configuration c = parseConfig("aristaInterface");

    assertThat(c, hasInterface("Ethernet3/2/1.4", hasMtu(9000)));
  }

  @Test
  public void testAristaDefaultSwitchPort() throws IOException {
    Configuration cUndeclared = parseConfig("eos_switchport_default_mode_undeclared");
    Configuration cDefaultAccess = parseConfig("eos_switchport_default_mode_access");
    Configuration cDefaultRouted = parseConfig("eos_switchport_default_mode_routed");

    String l0Name = "Loopback0";
    String e0Name = "Ethernet0/0";
    String e1Name = "Ethernet0/1";
    String e2Name = "Ethernet0/2";
    String e3Name = "Ethernet0/3";
    String e4Name = "Ethernet0/4";
    String p0Name = "Port-Channel0";

    Interface l0Undeclared = cUndeclared.getAllInterfaces().get(l0Name);
    Interface e0Undeclared = cUndeclared.getAllInterfaces().get(e0Name);
    Interface e1Undeclared = cUndeclared.getAllInterfaces().get(e1Name);
    Interface e2Undeclared = cUndeclared.getAllInterfaces().get(e2Name);
    Interface e3Undeclared = cUndeclared.getAllInterfaces().get(e3Name);
    Interface e4Undeclared = cUndeclared.getAllInterfaces().get(e4Name);
    Interface p0Undeclared = cUndeclared.getAllInterfaces().get(p0Name);

    Interface l0DefaultAccess = cDefaultAccess.getAllInterfaces().get(l0Name);
    Interface e0DefaultAccess = cDefaultAccess.getAllInterfaces().get(e0Name);
    Interface e1DefaultAccess = cDefaultAccess.getAllInterfaces().get(e1Name);
    Interface e2DefaultAccess = cDefaultAccess.getAllInterfaces().get(e2Name);
    Interface e3DefaultAccess = cDefaultAccess.getAllInterfaces().get(e3Name);
    Interface e4DefaultAccess = cDefaultAccess.getAllInterfaces().get(e4Name);
    Interface p0DefaultAccess = cDefaultAccess.getAllInterfaces().get(p0Name);

    Interface l0DefaultRouted = cDefaultRouted.getAllInterfaces().get(l0Name);
    Interface e0DefaultRouted = cDefaultRouted.getAllInterfaces().get(e0Name);
    Interface e1DefaultRouted = cDefaultRouted.getAllInterfaces().get(e1Name);
    Interface e2DefaultRouted = cDefaultRouted.getAllInterfaces().get(e2Name);
    Interface e3DefaultRouted = cDefaultRouted.getAllInterfaces().get(e3Name);
    Interface e4DefaultRouted = cDefaultRouted.getAllInterfaces().get(e4Name);
    Interface p0DefaultRouted = cDefaultRouted.getAllInterfaces().get(p0Name);

    assertThat(l0Undeclared, isSwitchport(false));
    assertThat(l0Undeclared, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e0Undeclared, isSwitchport(true));
    assertThat(e0Undeclared, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e1Undeclared, isSwitchport(true));
    assertThat(e1Undeclared, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e2Undeclared, isSwitchport(true));
    assertThat(e2Undeclared, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e3Undeclared, isSwitchport(false));
    assertThat(e3Undeclared, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e4Undeclared, isSwitchport(true));
    assertThat(e4Undeclared, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(p0Undeclared, isSwitchport(true));
    assertThat(p0Undeclared, hasSwitchPortMode(SwitchportMode.ACCESS));

    assertThat(l0DefaultAccess, isSwitchport(false));
    assertThat(l0DefaultAccess, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e0DefaultAccess, isSwitchport(true));
    assertThat(e0DefaultAccess, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e1DefaultAccess, isSwitchport(true));
    assertThat(e1DefaultAccess, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e2DefaultAccess, isSwitchport(true));
    assertThat(e2DefaultAccess, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e3DefaultAccess, isSwitchport(false));
    assertThat(e3DefaultAccess, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e4DefaultAccess, isSwitchport(true));
    assertThat(e4DefaultAccess, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(p0DefaultAccess, isSwitchport(true));
    assertThat(p0DefaultAccess, hasSwitchPortMode(SwitchportMode.ACCESS));

    assertThat(l0DefaultRouted, isSwitchport(false));
    assertThat(l0DefaultRouted, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e0DefaultRouted, isSwitchport(false));
    assertThat(e0DefaultRouted, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e1DefaultRouted, isSwitchport(true));
    assertThat(e1DefaultRouted, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e2DefaultRouted, isSwitchport(true));
    assertThat(e2DefaultRouted, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e3DefaultRouted, isSwitchport(false));
    assertThat(e3DefaultRouted, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e4DefaultRouted, isSwitchport(true));
    assertThat(e4DefaultRouted, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(p0DefaultRouted, isSwitchport(false));
    assertThat(p0DefaultRouted, hasSwitchPortMode(SwitchportMode.NONE));
  }

  @Test
  public void testAristaDynamicSourceNat() throws IOException {
    Configuration c = parseConfig("arista-dynamic-source-nat");
    Interface iface = c.getAllInterfaces().get("Ethernet1");
    assertThat(iface.getIncomingTransformation(), nullValue());
    assertThat(
        iface.getOutgoingTransformation(),
        equalTo(
            when(permittedByAcl("acl1"))
                .apply(assignSourceIp(Ip.parse("1.1.1.1"), Ip.parse("1.1.1.2")))
                .build()));

    iface = c.getAllInterfaces().get("Ethernet2");
    assertThat(iface.getIncomingTransformation(), nullValue());
    assertThat(
        iface.getOutgoingTransformation(),
        equalTo(
            when(permittedByAcl("acl1"))
                .apply(assignSourceIp(Ip.parse("1.1.1.1"), Ip.parse("1.1.1.2")))
                .setOrElse(
                    when(permittedByAcl("acl2"))
                        .apply(assignSourceIp(Ip.parse("2.2.2.2"), Ip.parse("2.2.2.3")))
                        .build())
                .build()));

    iface = c.getAllInterfaces().get("Ethernet3");
    assertThat(iface.getIncomingTransformation(), nullValue());
    assertThat(
        iface.getOutgoingTransformation(),
        equalTo(
            when(permittedByAcl("acl1"))
                // due to the subnet mask, shift the pool IPs to avoid network/bcast IPs.
                .apply(assignSourceIp(Ip.parse("2.0.0.1"), Ip.parse("2.0.0.6")))
                .build()));

    iface = c.getAllInterfaces().get("Ethernet4");
    assertThat(iface.getIncomingTransformation(), nullValue());
    assertThat(
        iface.getOutgoingTransformation(),
        equalTo(
            when(permittedByAcl("acl1"))
                // overload rule, so use the interface IP
                .apply(assignSourceIp(Ip.parse("8.8.8.8"), Ip.parse("8.8.8.8")))
                .build()));
  }

  @Test
  public void testIosDynamicNat() throws IOException {
    Configuration c = parseConfig("ios-nat-dynamic");
    String insideIntf = "Ethernet1";
    String outsideIntf = "Ethernet2";
    Ip nat1PoolFirst = Ip.parse("3.3.3.1");
    Ip nat1PoolLast = Ip.parse("3.3.3.254");
    Ip nat2PoolFirst = Ip.parse("3.3.4.1");
    Ip nat2PoolLast = Ip.parse("3.3.4.254");
    Ip nat3PoolFirst = Ip.parse("4.4.4.1");
    Ip nat3PoolLast = Ip.parse("4.4.4.254");
    String nat1AclName = "10";
    String nat2AclName = computeDynamicDestinationNatAclName("11");
    String nat3AclName = "22";

    assertThat(c, hasInterface(insideIntf, notNullValue()));
    assertThat(c, hasInterface(outsideIntf, notNullValue()));

    Interface inside = c.getAllInterfaces().get(insideIntf);
    assertThat(inside.getIncomingTransformation(), nullValue());
    assertThat(inside.getOutgoingTransformation(), nullValue());

    MatchSrcInterface matchIface = matchSrcInterface(insideIntf);

    Interface outside = c.getAllInterfaces().get(outsideIntf);

    Transformation inTransformation =
        when(permittedByAcl(nat3AclName))
            .apply(assignSourceIp(nat3PoolFirst, nat3PoolLast))
            .build();

    assertThat(outside.getIncomingTransformation(), equalTo(inTransformation));

    Transformation destTransformation =
        when(and(matchIface, permittedByAcl(nat2AclName)))
            .apply(assignDestinationIp(nat2PoolFirst, nat2PoolLast))
            .build();

    Transformation outTransformation =
        when(and(matchIface, permittedByAcl(nat1AclName)))
            .apply(assignSourceIp(nat1PoolFirst, nat1PoolLast))
            .setAndThen(destTransformation)
            .setOrElse(destTransformation)
            .build();

    assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
  }

  @Test
  public void testIosStaticNat() throws IOException {
    Configuration c = parseConfig("ios-nat-static");
    String insideIntf = "Ethernet1";
    String outsideIntf = "Ethernet2";
    Prefix nat1Local = Prefix.parse("1.1.1.1/32");
    Prefix nat3Local = Prefix.parse("1.1.3.0/24");
    Prefix nat2Local = Prefix.parse("1.1.2.0/14");
    Prefix nat4Local = Prefix.parse("7.7.7.7/32");
    Prefix nat1Global = Prefix.parse("2.2.2.2/32");
    Prefix nat2Global = Prefix.parse("2.2.2.0/14");
    Prefix nat4Global = Prefix.parse("6.6.6.6/32");
    Prefix nat3Global = Prefix.parse("2.2.3.0/24");

    assertThat(c, hasInterface(insideIntf, notNullValue()));
    assertThat(c, hasInterface(outsideIntf, notNullValue()));

    Interface inside = c.getAllInterfaces().get(insideIntf);
    assertThat(inside.getIncomingTransformation(), nullValue());
    assertThat(inside.getOutgoingTransformation(), nullValue());

    Interface outside = c.getAllInterfaces().get(outsideIntf);
    assertThat(outside.getIncomingTransformation(), notNullValue());
    assertThat(outside.getOutgoingTransformation(), notNullValue());

    MatchSrcInterface matchIface = matchSrcInterface(insideIntf);
    Transformation inDestinationTransformation =
        when(matchDst(nat1Global))
            .apply(shiftDestinationIp(nat1Local))
            .setOrElse(
                when(matchDst(nat3Global))
                    .apply(shiftDestinationIp(nat3Local))
                    .setOrElse(
                        when(matchDst(nat2Global)).apply(shiftDestinationIp(nat2Local)).build())
                    .build())
            .build();

    Transformation inTransformation =
        when(matchSrc(nat4Global))
            .apply(shiftSourceIp(nat4Local))
            .setAndThen(inDestinationTransformation)
            .setOrElse(inDestinationTransformation)
            .build();

    assertThat(outside.getIncomingTransformation(), equalTo(inTransformation));

    Transformation outDestinationTransformation =
        when(and(matchDst(nat4Local), matchIface)).apply(shiftDestinationIp(nat4Global)).build();

    Transformation outTransformation =
        when(and(matchSrc(nat1Local), matchIface))
            .apply(shiftSourceIp(nat1Global))
            .setAndThen(outDestinationTransformation)
            .setOrElse(
                when(and(matchSrc(nat3Local), matchIface))
                    .apply(shiftSourceIp(nat3Global))
                    .setAndThen(outDestinationTransformation)
                    .setOrElse(
                        when(and(matchSrc(nat2Local), matchIface))
                            .apply(shiftSourceIp(nat2Global))
                            .setAndThen(outDestinationTransformation)
                            .setOrElse(outDestinationTransformation)
                            .build())
                    .build())
            .build();

    assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
  }

  @Test
  public void testIosMixedNat() throws IOException {
    Configuration c = parseConfig("ios-nat-mixed");
    String insideIntf = "Ethernet1";
    String outsideIntf = "Ethernet2";
    Prefix staticNatLocal = Prefix.parse("1.1.3.0/24");
    Prefix staticNatGlobal = Prefix.parse("2.2.3.0/24");
    String dynamicNatAcl = "10";
    Ip dynamicNatStart = Ip.parse("3.3.3.1");
    Ip dynamicNatEnd = Ip.parse("3.3.3.254");

    Interface outside = c.getAllInterfaces().get(outsideIntf);
    MatchSrcInterface matchIface = matchSrcInterface(insideIntf);

    // Check that the inside-to-outside transformation evaluates the static NAT first
    Transformation outTransformation =
        when(and(matchIface, matchSrc(staticNatLocal)))
            .apply(shiftSourceIp(staticNatGlobal))
            .setOrElse(
                when(and(matchIface, permittedByAcl(dynamicNatAcl)))
                    .apply(assignSourceIp(dynamicNatStart, dynamicNatEnd))
                    .build())
            .build();
    assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
  }

  @Test
  public void testIosSwitchportMode() throws IOException {
    Configuration c = parseConfig("ios_switchport_mode");

    Interface e0 = c.getAllInterfaces().get("Ethernet0/0");
    Interface e1 = c.getAllInterfaces().get("Ethernet0/1");
    Interface e2 = c.getAllInterfaces().get("Ethernet0/2");
    Interface e3 = c.getAllInterfaces().get("Ethernet0/3");
    Interface e4 = c.getAllInterfaces().get("Ethernet0/4");
    Interface e5 = c.getAllInterfaces().get("Ethernet0/5");
    Interface e6 = c.getAllInterfaces().get("Ethernet0/6");

    assertThat(e0, isSwitchport(false));
    assertThat(e0, hasSwitchPortMode(SwitchportMode.NONE));

    assertThat(e1, isSwitchport(true));
    assertThat(e1, hasSwitchPortMode(SwitchportMode.DYNAMIC_AUTO));

    assertThat(e2, isSwitchport(true));
    assertThat(e2, hasSwitchPortMode(SwitchportMode.ACCESS));

    assertThat(e3, isSwitchport(true));
    assertThat(e3, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(e3, hasSwitchPortEncapsulation(SwitchportEncapsulationType.DOT1Q));

    assertThat(e4, isSwitchport(true));
    assertThat(e4, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(e4, hasSwitchPortEncapsulation(SwitchportEncapsulationType.DOT1Q));
    assertThat(e4.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 2))));

    assertThat(e5, isSwitchport(true));
    assertThat(e5, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(e5, hasSwitchPortEncapsulation(SwitchportEncapsulationType.ISL));
    assertThat(e5.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(3, 4))));

    assertThat(e6, isSwitchport(true));
    assertThat(e6, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(e6, hasSwitchPortEncapsulation(SwitchportEncapsulationType.NEGOTIATE));
    assertThat(e6.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(5, 6))));
  }

  @Test
  public void testNxosSwitchportMode() throws IOException {
    Configuration c = parseConfig("nxos_switchport_mode");

    Interface e0 = c.getAllInterfaces().get("Ethernet0/0");
    Interface e1 = c.getAllInterfaces().get("Ethernet0/1");
    Interface e2 = c.getAllInterfaces().get("Ethernet0/2");
    Interface e3 = c.getAllInterfaces().get("Ethernet0/3");

    assertThat(e0, isSwitchport(false));
    assertThat(e0, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e1, isSwitchport(true));
    assertThat(e1, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e2, isSwitchport(true));
    assertThat(e2, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e3, isSwitchport(true));
    assertThat(e3, hasSwitchPortMode(SwitchportMode.TRUNK));
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
                    allOf(
                        hasRemoteAs(2L),
                        hasLocalAs(1L),
                        hasIpv4UnicastAddressFamily(
                            hasAddressFamilyCapabilites(hasAllowRemoteAsOut(true))))))));
    assertThat(
        c,
        ConfigurationMatchers.hasVrf(
            "bar",
            hasBgpProcess(
                hasActiveNeighbor(
                    Prefix.parse("3.3.3.3/32"),
                    hasIpv4UnicastAddressFamily(
                        hasAddressFamilyCapabilites(hasAllowRemoteAsOut(false)))))));
  }

  @Test
  public void testArista100gfullInterface() throws IOException {
    Configuration c = parseConfig("arista100gfull");
    assertThat(
        c,
        hasInterface(
            "Ethernet1/1",
            hasAllAddresses(containsInAnyOrder(ConcreteInterfaceAddress.parse("10.20.0.3/31")))));
  }

  @Test
  public void testAsaInterface() throws IOException {
    String hostname = "asa-interface";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm interface's address is extracted properly
    assertThat(
        c,
        hasInterface(
            "ifname",
            hasAllAddresses(containsInAnyOrder(ConcreteInterfaceAddress.parse("3.0.0.2/24")))));

    // Confirm that interface MTU is set correctly
    assertThat(c, hasInterface("ifname", hasMtu(1400)));

    // Confirm interface definition is tracked for the alias name
    assertThat(ccae, hasDefinedStructure(filename, INTERFACE, "ifname"));
  }

  // https://github.com/batfish/batfish/issues/4124
  @Test
  public void testAsaInterfaceOspfWithInheritance() throws IOException {
    String hostname = "asa-interface-ospf";
    Configuration c = parseConfig(hostname);
    assertThat(
        c,
        hasInterface(
            "LAB-INT",
            allOf(
                hasBandwidth(0.0d),
                hasOspfAreaName(0L),
                hasInterfaceType(InterfaceType.AGGREGATE_CHILD))));
  }

  @Test
  public void testAsaInterfaceEncapsulationVlan() throws IOException {
    String hostname = "asa_interface_encapsulation_vlan";
    Configuration c = parseConfig(hostname);

    // encapsulation vlan should be read in s_interface context, and so later IP address should be
    // correctly extracted
    assertThat(
        c,
        hasInterface(
            "ifname",
            both(hasEncapsulationVlan(100))
                .and(hasAddress(ConcreteInterfaceAddress.parse("192.0.2.1/24")))));
  }

  @Test
  public void testAsaSecurityLevel() throws IOException {
    Configuration c = parseConfig("asa-security-level");
    String explicit100Interface = "all-trust";
    String insideInterface = "inside";
    String explicit45Interface = "some-trust";
    String outsideInterface = "outside";

    Flow newFlow = createFlow(IpProtocol.IP, 0, 0, FlowState.NEW);
    Flow establishedFlow = createFlow(IpProtocol.IP, 0, 0, FlowState.ESTABLISHED);

    // Confirm zones are created for each level
    assertThat(c, hasZone(computeSecurityLevelZoneName(100), hasMemberInterfaces(hasSize(2))));
    assertThat(c, hasZone(computeSecurityLevelZoneName(45), hasMemberInterfaces(hasSize(1))));
    assertThat(c, hasZone(computeSecurityLevelZoneName(1), hasMemberInterfaces(hasSize(1))));

    // No traffic in and out of the same interface
    assertThat(
        c,
        hasInterface(
            explicit100Interface,
            hasPreTransformationOutgoingFilter(rejects(newFlow, explicit100Interface, c))));
    assertThat(
        c,
        hasInterface(
            insideInterface,
            hasPreTransformationOutgoingFilter(rejects(newFlow, insideInterface, c))));
    assertThat(
        c,
        hasInterface(
            explicit45Interface,
            hasPreTransformationOutgoingFilter(rejects(newFlow, explicit45Interface, c))));
    assertThat(
        c,
        hasInterface(
            outsideInterface,
            hasPreTransformationOutgoingFilter(rejects(newFlow, outsideInterface, c))));

    // No traffic between interfaces with same level
    assertThat(
        c,
        hasInterface(
            insideInterface,
            hasPreTransformationOutgoingFilter(rejects(newFlow, explicit100Interface, c))));
    assertThat(
        c,
        hasInterface(
            explicit100Interface,
            hasPreTransformationOutgoingFilter(rejects(newFlow, insideInterface, c))));

    // Allow traffic from 100 to others
    assertThat(
        c,
        hasInterface(
            explicit45Interface,
            hasPreTransformationOutgoingFilter(accepts(newFlow, insideInterface, c))));
    assertThat(
        c,
        hasInterface(
            outsideInterface,
            hasPreTransformationOutgoingFilter(accepts(newFlow, insideInterface, c))));

    // Mid level is accepted by lower, but not higher
    assertThat(
        c,
        hasInterface(
            insideInterface,
            hasPreTransformationOutgoingFilter(rejects(newFlow, explicit45Interface, c))));
    assertThat(
        c,
        hasInterface(
            outsideInterface,
            hasPreTransformationOutgoingFilter(accepts(newFlow, explicit45Interface, c))));

    // No traffic from outside
    assertThat(
        c,
        hasInterface(
            insideInterface,
            hasPreTransformationOutgoingFilter(rejects(newFlow, outsideInterface, c))));
    assertThat(
        c,
        hasInterface(
            explicit45Interface,
            hasPreTransformationOutgoingFilter(rejects(newFlow, outsideInterface, c))));

    // All established flows are accepted
    assertThat(
        c,
        hasInterface(
            explicit45Interface,
            hasPreTransformationOutgoingFilter(accepts(establishedFlow, outsideInterface, c))));
    assertThat(
        c,
        hasInterface(
            insideInterface,
            hasPreTransformationOutgoingFilter(accepts(establishedFlow, outsideInterface, c))));
    assertThat(
        c,
        hasInterface(
            insideInterface,
            hasPreTransformationOutgoingFilter(accepts(establishedFlow, explicit45Interface, c))));
    assertThat(
        c,
        hasInterface(
            insideInterface,
            hasPreTransformationOutgoingFilter(accepts(establishedFlow, explicit100Interface, c))));
  }

  @Test
  public void testAsaSecurityLevelPermitBoth() throws IOException {
    Configuration c = parseConfig("asa-security-level-permit-both");
    String ifaceAlias1 = "name1";
    String ifaceAlias2 = "name2";
    Flow newFlow = createFlow(IpProtocol.IP, 0, 0, FlowState.NEW);

    // Allow traffic in and out of the same interface
    assertThat(
        c,
        hasInterface(
            ifaceAlias1, hasPreTransformationOutgoingFilter(accepts(newFlow, ifaceAlias1, c))));
    assertThat(
        c,
        hasInterface(
            ifaceAlias2, hasPreTransformationOutgoingFilter(accepts(newFlow, ifaceAlias2, c))));

    // Allow traffic between interfaces with same level
    assertThat(
        c,
        hasInterface(
            ifaceAlias1, hasPreTransformationOutgoingFilter(accepts(newFlow, ifaceAlias2, c))));
    assertThat(
        c,
        hasInterface(
            ifaceAlias2, hasPreTransformationOutgoingFilter(accepts(newFlow, ifaceAlias1, c))));
  }

  @Test
  public void testAsaSecurityLevelPermitInter() throws IOException {
    Configuration c = parseConfig("asa-security-level-permit-inter");
    String ifaceAlias1 = "name1";
    String ifaceAlias2 = "name2";
    Flow newFlow = createFlow(IpProtocol.IP, 0, 0, FlowState.NEW);

    // No traffic in and out of the same interface
    assertThat(
        c,
        hasInterface(
            ifaceAlias1, hasPreTransformationOutgoingFilter(rejects(newFlow, ifaceAlias1, c))));
    assertThat(
        c,
        hasInterface(
            ifaceAlias2, hasPreTransformationOutgoingFilter(rejects(newFlow, ifaceAlias2, c))));

    // Allow traffic between interfaces with same level
    assertThat(
        c,
        hasInterface(
            ifaceAlias1, hasPreTransformationOutgoingFilter(accepts(newFlow, ifaceAlias2, c))));
    assertThat(
        c,
        hasInterface(
            ifaceAlias2, hasPreTransformationOutgoingFilter(accepts(newFlow, ifaceAlias1, c))));
  }

  @Test
  public void testAsaSecurityLevelPermitIntra() throws IOException {
    Configuration c = parseConfig("asa-security-level-permit-intra");
    String ifaceAlias1 = "name1";
    String ifaceAlias2 = "name2";
    Flow newFlow = createFlow(IpProtocol.IP, 0, 0, FlowState.NEW);

    // Allow traffic in and out of the same interface
    assertThat(
        c,
        hasInterface(
            ifaceAlias1, hasPreTransformationOutgoingFilter(accepts(newFlow, ifaceAlias1, c))));
    assertThat(
        c,
        hasInterface(
            ifaceAlias2, hasPreTransformationOutgoingFilter(accepts(newFlow, ifaceAlias2, c))));

    // No traffic between interfaces with same level
    assertThat(
        c,
        hasInterface(
            ifaceAlias1, hasPreTransformationOutgoingFilter(rejects(newFlow, ifaceAlias2, c))));
    assertThat(
        c,
        hasInterface(
            ifaceAlias2, hasPreTransformationOutgoingFilter(rejects(newFlow, ifaceAlias1, c))));
  }

  @Test
  public void testAsaStaticRoute() throws IOException {
    Configuration c = parseConfig("asa-static-route");

    // Confirm static route is extracted properly
    assertThat(
        c,
        ConfigurationMatchers.hasVrf(
            "default",
            hasStaticRoutes(
                equalTo(
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNextHopIp(Ip.parse("3.0.0.1"))
                            .setNetwork(Prefix.parse("0.0.0.0/0"))
                            .setNextHopInterface("ifname")
                            .setAdministrativeCost(2)
                            .build(),
                        StaticRoute.builder()
                            .setNextHopIp(Ip.parse("3.0.0.2"))
                            .setNetwork(Prefix.parse("1.0.0.0/8"))
                            .setNextHopInterface("ifname")
                            .setAdministrativeCost(3)
                            .build())))));
  }

  @Test
  public void testAsaObjectNatDynamic() throws IOException {
    // Test Transformations from ASA dynamic object NATs
    String hostname = "asa-nat-object-dynamic";
    Configuration config = parseConfig(hostname);

    Transformation.Builder nat1 =
        when(and(
                matchSrc(
                    new IpSpaceReference("source-real-1", "Match network object: 'source-real-1'")),
                matchSrcInterface("inside")))
            .apply(assignSourceIp(Ip.parse("192.168.1.1"), Ip.parse("192.168.1.10")));
    Transformation.Builder nat2 =
        when(and(
                matchSrc(
                    new IpSpaceReference("source-real-2", "Match network object: 'source-real-2'")),
                matchSrcInterface("outside")))
            .apply(assignSourceIp(Ip.parse("192.168.2.1"), Ip.parse("192.168.2.10")));
    Transformation.Builder nat3 =
        when(and(
                matchSrc(
                    new IpSpaceReference("source-real-3", "Match network object: 'source-real-3'")),
                matchSrcInterface("inside")))
            .apply(assignSourceIp(Ip.parse("192.168.3.1"), Ip.parse("192.168.3.10")));
    Transformation.Builder nat4 =
        when(matchSrc(
                new IpSpaceReference("source-real-4", "Match network object: 'source-real-4'")))
            .apply(assignSourceIp(Ip.parse("192.168.4.1"), Ip.parse("192.168.4.10")));

    Transformation nat = config.getAllInterfaces().get("inside").getIncomingTransformation();
    assertThat(nat, nullValue());

    // 'inside' is the mapped interface for nat rules 2, 3, and 4. Sorted as 2->3->4
    nat = config.getAllInterfaces().get("inside").getOutgoingTransformation();
    assertThat(nat, equalTo(nat2.setOrElse(nat3.setOrElse(nat4.build()).build()).build()));

    nat = config.getAllInterfaces().get("outside").getIncomingTransformation();
    assertThat(nat, nullValue());

    // 'outside' is the mapped interface for nat rules 1, 3, and 4. Sorted as 1->3->4
    nat = config.getAllInterfaces().get("outside").getOutgoingTransformation();
    assertThat(nat, equalTo(nat1.setOrElse(nat3.setOrElse(nat4.build()).build()).build()));

    nat = config.getAllInterfaces().get("other").getIncomingTransformation();
    assertThat(nat, nullValue());

    // 'other' is the mapped interface for nat rules 3 and 4. Sorted as 3->4
    nat = config.getAllInterfaces().get("other").getOutgoingTransformation();
    assertThat(nat, equalTo(nat3.setOrElse(nat4.build()).build()));
  }

  @Test
  public void testAsaObjectNatReferrers() throws IOException {
    String hostname = "asa-nat-object-refs";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // check expected references
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "inside", 4));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "outside", 3));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "other", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-mapped-1", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-mapped-2", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-real-1", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-real-2", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-real-3", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-real-4", 1));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-real-invalid", 1));

    assertThat(ccae, hasUndefinedReference(filename, INTERFACE, "dmz"));
    assertThat(ccae, hasUndefinedReference(filename, INTERFACE, "mgmt"));
    assertThat(ccae, hasUndefinedReference(filename, NETWORK_OBJECT, "source-mapped-undef"));
    assertThat(ccae, not(hasUndefinedReference(filename, INTERFACE, "inside")));
    assertThat(ccae, not(hasUndefinedReference(filename, INTERFACE, "outside")));
    assertThat(ccae, not(hasUndefinedReference(filename, INTERFACE, "other")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-mapped-1")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-mapped-2")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-real-1")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-real-2")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-real-3")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-real-4")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-real-invalid")));
  }

  @Test
  public void testAsaObjectNatStatic() throws IOException {
    // Test Transformations from ASA static object NATs
    String hostname = "asa-nat-object-static";
    Configuration config = parseConfig(hostname);

    Prefix realSource1 = Prefix.parse("1.1.1.1/32");
    Prefix realSource2 = Prefix.parse("2.2.2.0/29");
    Prefix realSource3 = Prefix.parse("3.3.3.3/32");
    Prefix realSource4 = Prefix.parse("4.4.4.0/24");
    Prefix mappedSource1 = Prefix.parse("192.168.1.1/32");
    Prefix mappedSource2 = Prefix.parse("192.168.2.0/29");
    Prefix mappedSource3 = Prefix.parse("192.168.3.0/32");
    Prefix mappedSource4 = Prefix.parse("192.168.4.0/24");

    Transformation.Builder natIn1 =
        when(matchDst(mappedSource1)).apply(shiftDestinationIp(realSource1));
    Transformation.Builder natIn2 =
        when(matchDst(mappedSource2)).apply(shiftDestinationIp(realSource2));
    Transformation.Builder natIn3 =
        when(matchDst(mappedSource3)).apply(shiftDestinationIp(realSource3));
    Transformation.Builder natIn4 =
        when(matchDst(mappedSource4)).apply(shiftDestinationIp(realSource4));
    Transformation.Builder natOut1 =
        when(and(matchSrc(realSource1), matchSrcInterface("inside")))
            .apply(shiftSourceIp(mappedSource1));
    Transformation.Builder natOut2 =
        when(and(matchSrc(realSource2), matchSrcInterface("outside")))
            .apply(shiftSourceIp(mappedSource2));
    Transformation.Builder natOut3 =
        when(and(matchSrc(realSource3), matchSrcInterface("inside")))
            .apply(shiftSourceIp(mappedSource3));
    Transformation.Builder natOut4 =
        when(matchSrc(realSource4)).apply(shiftSourceIp(mappedSource4));

    // 'inside' is the mapped interface for nat rules 2, 3, and 4. Sorted as 3->2->4.
    Transformation nat = config.getAllInterfaces().get("inside").getIncomingTransformation();
    assertThat(nat, equalTo(natIn3.setOrElse(natIn2.setOrElse(natIn4.build()).build()).build()));

    nat = config.getAllInterfaces().get("inside").getOutgoingTransformation();
    assertThat(nat, equalTo(natOut3.setOrElse(natOut2.setOrElse(natOut4.build()).build()).build()));

    // 'outside' is the mapped interface for nat rules 1, 3, and 4. Sorted as 1->3->4
    nat = config.getAllInterfaces().get("outside").getIncomingTransformation();
    assertThat(nat, equalTo(natIn1.setOrElse(natIn3.setOrElse(natIn4.build()).build()).build()));

    nat = config.getAllInterfaces().get("outside").getOutgoingTransformation();
    assertThat(nat, equalTo(natOut1.setOrElse(natOut3.setOrElse(natOut4.build()).build()).build()));

    // 'other' is the mapped interface for nat rules 3 and 4. Sorted as 3->4
    nat = config.getAllInterfaces().get("other").getIncomingTransformation();
    assertThat(nat, equalTo(natIn3.setOrElse(natIn4.build()).build()));

    nat = config.getAllInterfaces().get("other").getOutgoingTransformation();
    assertThat(nat, equalTo(natOut3.setOrElse(natOut4.build()).build()));
  }

  @Test
  public void testAsaNatOrder() {
    String hostname = "asa-nat-mixed";
    CiscoConfiguration config = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_ASA);

    // Finishes extraction, necessary for object NATs
    config.setWarnings(new Warnings());
    config.setAnswerElement(new ConvertConfigurationAnswerElement());
    config.toVendorIndependentConfigurations();

    List<CiscoAsaNat> nats = config.getCiscoAsaNats();

    // CiscoAsaNats are comparable
    Collections.sort(nats);

    // Check that NATs are sorted by section
    assertThat(
        nats.stream().map(CiscoAsaNat::getSection).collect(Collectors.toList()),
        contains(
            Section.BEFORE,
            Section.BEFORE,
            Section.OBJECT,
            Section.OBJECT,
            Section.OBJECT,
            Section.OBJECT,
            Section.OBJECT,
            Section.OBJECT,
            Section.AFTER,
            Section.AFTER));

    // Check that twice NATs are sorted by line after section
    assertThat(
        nats.stream()
            .filter(nat2 -> nat2.getSection().equals(Section.BEFORE))
            .map(CiscoAsaNat::getLine)
            .collect(Collectors.toList()),
        contains(2, 3));
    assertThat(
        nats.stream()
            .filter(nat1 -> nat1.getSection().equals(Section.AFTER))
            .map(CiscoAsaNat::getLine)
            .collect(Collectors.toList()),
        contains(1, 4));

    List<CiscoAsaNat> objectNats =
        nats.stream()
            .filter(nat -> nat.getSection().equals(Section.OBJECT))
            .collect(Collectors.toList());

    // Check that object NATs are sorted static and then dynamic
    assertThat(
        objectNats.stream().map(CiscoAsaNat::getDynamic).collect(Collectors.toList()),
        contains(false, false, false, false, false, true));

    // Check that object NATs of a particular type (static) are sorted by their network objects
    // See CiscoAsaNat.compareTo or
    // https://www.cisco.com/c/en/us/td/docs/security/asa/asa910/configuration/firewall/asa-910-firewall-config/nat-basics.html#ID-2090-00000065
    assertThat(
        objectNats.stream()
            .filter(nat -> !nat.getDynamic())
            .map(CiscoAsaNat::getRealSourceObject)
            .map(NetworkObject::getName)
            .collect(Collectors.toList()),
        contains(
            "alphabetical",
            "source-static",
            "source-subnet29",
            "source-subnet29-b",
            "source-subnet24"));
  }

  @Test
  public void testAsaTwiceNatDynamic() {
    // Test vendor-specific parsing of ASA dynamic twice NATs
    String hostname = "asa-nat-twice-dynamic";
    CiscoConfiguration config = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_ASA);

    MatchHeaderSpace matchSourceSubnet =
        matchSrc(
            new IpSpaceReference("source-subnet", "Match network object-group: 'source-subnet'"));
    MatchHeaderSpace matchSourceGroup =
        matchSrc(
            new IpSpaceReference("source-group", "Match network object-group: 'source-group'"));
    AssignIpAddressFromPool assignSourceRange =
        assignSourceIp(Ip.parse("2.2.2.2"), Ip.parse("2.2.2.10"));
    Prefix mappedDestination = Prefix.parse("3.3.3.3/32");
    Prefix realDestination = Prefix.parse("4.4.4.4/32");

    List<CiscoAsaNat> nats = config.getCiscoAsaNats();
    assertThat(nats, hasSize(4));

    // dynamic source NAT, subnet -> range
    CiscoAsaNat nat = config.getCiscoAsaNats().get(0);
    Optional<Transformation.Builder> builder =
        nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    Transformation twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(matchSrcInterface("inside"), matchSourceSubnet))
                .apply(assignSourceRange)
                .build()));

    // dynamic source NAT and static destination NAT
    nat = config.getCiscoAsaNats().get(1);
    builder = nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(
                    and(matchSrcInterface("inside"), matchSourceGroup),
                    matchDst(mappedDestination)))
                .apply(ImmutableList.of(assignSourceRange, shiftDestinationIp(realDestination)))
                .build()));

    // dynamic source NAT, host -> range, no interfaces specified
    nat = config.getCiscoAsaNats().get(2);
    builder = nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(twice, equalTo(when(matchSourceGroup).apply(assignSourceRange).build()));

    // dynamic source NAT and static destination NAT, no interface specified
    nat = config.getCiscoAsaNats().get(3);
    builder = nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(matchSourceSubnet, matchDst(mappedDestination)))
                .apply(ImmutableList.of(assignSourceRange, shiftDestinationIp(realDestination)))
                .build()));
  }

  @Test
  public void testAsaTwiceNatStatic() {
    // Test vendor-specific parsing of ASA static twice NATs
    String hostname = "asa-nat-twice-static";
    CiscoConfiguration config = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_ASA);

    List<CiscoAsaNat> nats = config.getCiscoAsaNats();
    assertThat(nats, hasSize(9));

    CiscoAsaNat nat = nats.get(0);
    assertThat(nat.getDynamic(), equalTo(false));
    assertThat(nat.getInsideInterface(), equalTo("inside"));
    assertThat(
        nat.getMappedDestination(), equalTo(new NetworkObjectAddressSpecifier("dest-mapped")));
    assertThat(nat.getMappedSource(), equalTo(new NetworkObjectAddressSpecifier("source-mapped")));
    assertThat(nat.getOutsideInterface(), equalTo("outside"));
    assertThat(nat.getRealDestination(), equalTo(new NetworkObjectAddressSpecifier("dest-real")));
    assertThat(nat.getRealSource(), equalTo(new NetworkObjectAddressSpecifier("source-real")));
    assertThat(nat.getTwice(), equalTo(true));

    nat = nats.get(1);
    assertThat(nat.getDynamic(), equalTo(false));
    assertThat(nat.getInsideInterface(), equalTo("inside"));
    assertThat(nat.getMappedSource(), equalTo(new NetworkObjectAddressSpecifier("source-mapped")));
    assertThat(nat.getOutsideInterface(), equalTo("outside"));
    assertThat(nat.getRealSource(), equalTo(new NetworkObjectAddressSpecifier("source-real")));
    assertThat(nat.getTwice(), equalTo(false));

    nat = nats.get(2);
    assertThat(nat.getDynamic(), equalTo(false));
    assertThat(nat.getTwice(), equalTo(false));
    assertThat(nat.getInsideInterface(), equalTo(CiscoAsaNat.ANY_INTERFACE));
    assertThat(nat.getOutsideInterface(), equalTo(CiscoAsaNat.ANY_INTERFACE));
    assertThat(
        nat.getRealSource(), equalTo(new NetworkObjectGroupAddressSpecifier("source-real-group")));
    assertThat(
        nat.getMappedSource(),
        equalTo(new NetworkObjectGroupAddressSpecifier("source-mapped-group")));

    nat = nats.get(3);
    assertTrue("NAT is active", nat.getInactive());
    assertThat(nat.getInsideInterface(), equalTo("inside"));
    assertThat(nat.getOutsideInterface(), equalTo(CiscoAsaNat.ANY_INTERFACE));

    nat = nats.get(4);
    assertThat(nat.getInsideInterface(), equalTo(CiscoAsaNat.ANY_INTERFACE));
    assertThat(nat.getOutsideInterface(), equalTo("outside"));

    nat = nats.get(5);
    assertThat(nat.getInsideInterface(), equalTo("outside"));
    assertThat(nat.getOutsideInterface(), equalTo("inside"));
    assertThat(nat.getRealSource(), equalTo(new WildcardAddressSpecifier(IpWildcard.ANY)));
    assertThat(nat.getMappedSource(), equalTo(new WildcardAddressSpecifier(IpWildcard.ANY)));

    nat = nats.get(6);
    assertThat(
        nat.getRealSource(), equalTo(new NetworkObjectAddressSpecifier("source-real-subnet")));
    assertThat(
        nat.getMappedSource(), equalTo(new NetworkObjectAddressSpecifier("source-mapped-subnet")));
  }

  @Test
  public void testAsaTwiceNatStaticSource() {
    // Test ASA twice NAT with source only
    String hostname = "asa-nat-twice-static";
    CiscoConfiguration config = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_ASA);

    Prefix realSourceHost = Prefix.parse("1.1.1.1/32");
    Prefix mappedSourceHost = Prefix.parse("2.2.2.2/32");
    Prefix mappedDestHost = Prefix.parse("3.3.3.3/32");
    Prefix realDestHost = Prefix.parse("4.4.4.4/32");
    Prefix realSourceSubnet = Prefix.parse("5.5.5.0/24");
    Prefix mappedSourceSubnet = Prefix.parse("6.6.6.0/24");

    // Host source NAT outgoing
    CiscoAsaNat nat = config.getCiscoAsaNats().get(1);
    Optional<Transformation.Builder> builder =
        nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    Transformation twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(matchSrcInterface("inside"), matchSrc(realSourceHost)))
                .apply(shiftSourceIp(mappedSourceHost))
                .build()));

    // Host source NAT incoming
    builder = nat.toIncomingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No incoming transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(matchDst(mappedSourceHost)).apply(shiftDestinationIp(realSourceHost)).build()));

    // Identity NAT
    nat = config.getCiscoAsaNats().get(5);
    builder = nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(matchSrcInterface("outside"), matchSrc(Prefix.ZERO)))
                .apply(shiftSourceIp(Prefix.ZERO))
                .build()));

    // Subnet source NAT
    nat = config.getCiscoAsaNats().get(6);
    builder = nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(matchSrcInterface("inside"), matchSrc(realSourceSubnet)))
                .apply(shiftSourceIp(mappedSourceSubnet))
                .build()));

    // Source NAT with no interfaces specified
    nat = config.getCiscoAsaNats().get(7);
    builder = nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(matchSrc(realSourceSubnet)))
                .apply(shiftSourceIp(mappedSourceSubnet))
                .build()));
    builder = nat.toIncomingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No incoming transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(matchDst(mappedSourceSubnet))
                .apply(shiftDestinationIp(realSourceSubnet))
                .build()));

    // Source + destination NAT with no interfaces specified
    nat = config.getCiscoAsaNats().get(8);
    builder = nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(matchDst(mappedDestHost), matchSrc(realSourceSubnet)))
                .apply(
                    ImmutableList.of(
                        shiftSourceIp(mappedSourceSubnet), shiftDestinationIp(realDestHost)))
                .build()));
    builder = nat.toIncomingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No incoming transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(matchSrc(realDestHost), matchDst(mappedSourceSubnet)))
                .apply(
                    ImmutableList.of(
                        shiftDestinationIp(realSourceSubnet), shiftSourceIp(mappedDestHost)))
                .build()));
  }

  @Test
  public void testAsaTwiceNatStaticSourceAndDestination() {
    // Test ASA twice NAT with source and destination
    String hostname = "asa-nat-twice-static";
    CiscoConfiguration config = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_ASA);

    Prefix realSource = Prefix.parse("1.1.1.1/32");
    Prefix mappedSource = Prefix.parse("2.2.2.2/32");
    Prefix mappedDestination = Prefix.parse("3.3.3.3/32");
    Prefix realDestination = Prefix.parse("4.4.4.4/32");

    CiscoAsaNat nat = config.getCiscoAsaNats().get(0);
    Optional<Transformation.Builder> builder =
        nat.toOutgoingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No outgoing transformation", builder.isPresent(), equalTo(true));
    Transformation twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(
                    and(matchSrcInterface("inside"), matchSrc(realSource)),
                    matchDst(mappedDestination)))
                .apply(
                    ImmutableList.of(
                        shiftSourceIp(mappedSource), shiftDestinationIp(realDestination)))
                .build()));

    builder = nat.toIncomingTransformation(config.getNetworkObjects(), new Warnings());
    assertThat("No incoming transformation", builder.isPresent(), equalTo(true));
    twice = builder.get().build();
    assertThat(
        twice,
        equalTo(
            when(and(matchDst(mappedSource), matchSrc(realDestination)))
                .apply(
                    ImmutableList.of(
                        shiftDestinationIp(realSource), shiftSourceIp(mappedDestination)))
                .build()));
  }

  @Test
  public void testAsaTwiceNatReferrers() throws IOException {
    String hostname = "asa-nat-twice-static";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // check expected references
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "inside", 6));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "outside", 6));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "dest-mapped", 2));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "dest-real", 2));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-mapped", 3));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-mapped-subnet", 3));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-real", 3));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT, "source-real-subnet", 3));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "source-mapped-group", 2));
    assertThat(ccae, hasNumReferrers(filename, NETWORK_OBJECT_GROUP, "source-real-group", 2));

    assertThat(ccae, not(hasUndefinedReference(filename, INTERFACE, "inside")));
    assertThat(ccae, not(hasUndefinedReference(filename, INTERFACE, "outside")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "dest-mapped")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "dest-real")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-mapped")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-mapped-subnet")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-real")));
    assertThat(ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT, "source-real-subnet")));
    assertThat(
        ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT_GROUP, "source-mapped-group")));
    assertThat(
        ccae, not(hasUndefinedReference(filename, NETWORK_OBJECT_GROUP, "source-real-group")));
    assertThat(ccae, hasUndefinedReference(filename, NETWORK_OBJECT, "undef-source-mapped"));
    assertThat(ccae, hasUndefinedReference(filename, NETWORK_OBJECT, "undef-source-real"));
  }

  /**
   * Ensure that Arista redistributes a static default route even though default-originate is not
   * explicitly specified in the config
   */
  @Test
  public void testAristaDefaultRouteRedistribution() throws IOException {
    final String receiver = "ios_receiver";
    final String advertiser = "arista_advertiser";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    TESTRIGS_PREFIX + "arista-redistribute-default-route",
                    ImmutableList.of(advertiser, receiver))
                .build(),
            _folder);

    batfish.computeDataPlane();
    Set<AbstractRoute> routes =
        batfish.loadDataPlane().getRibs().get(receiver).get(DEFAULT_VRF_NAME).getRoutes();

    assertThat(
        routes,
        hasItem(
            OspfExternalType2Route.builder()
                .setNetwork(Prefix.ZERO)
                .setNextHopIp(Ip.parse("1.2.3.5"))
                .setArea(1)
                .setCostToAdvertiser(1)
                .setAdvertiser(advertiser)
                .setAdmin(110)
                .setMetric(1)
                .setLsaMetric(1)
                .build()));
  }

  @Test
  public void testPortchannelSubinterfaceIsUp() throws IOException {
    Configuration config = parseConfig("ios-portchannel-subinterface");
    double eth1Bandwidth = 1E7;
    assertThat(config, hasInterface("Ethernet1", hasBandwidth(eth1Bandwidth)));
    assertThat(config, hasInterface("Port-Channel1", hasBandwidth(eth1Bandwidth)));
    assertThat(
        config,
        hasInterface(
            "Port-Channel1.1",
            allOf(
                hasInterfaceType(InterfaceType.AGGREGATE_CHILD),
                isActive(true),
                hasBandwidth(eth1Bandwidth))));
  }

  @Test
  public void testAristaAdvertiseInactive() throws IOException {
    Configuration config = parseConfig("arista-advertise-inactive");
    assertTrue(
        config
            .getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Prefix.parse("1.1.1.1/32"))
            .getIpv4UnicastAddressFamily()
            .getAddressFamilyCapabilities()
            .getAdvertiseInactive());
  }

  @Test
  public void testAristaNoAdvertiseInactive() throws IOException {
    Configuration config = parseConfig("arista-no-advertise-inactive");
    assertFalse(
        config
            .getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Prefix.parse("1.1.1.1/32"))
            .getIpv4UnicastAddressFamily()
            .getAddressFamilyCapabilities()
            .getAdvertiseInactive());
  }

  @Test
  public void testIosAdvertiseInactive() throws IOException {
    Configuration config = parseConfig("ios-advertise-inactive");
    assertTrue(
        config
            .getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Prefix.parse("1.1.1.1/32"))
            .getIpv4UnicastAddressFamily()
            .getAddressFamilyCapabilities()
            .getAdvertiseInactive());
  }

  @Test
  public void testTunnelTopologyNoReachability() throws IOException {
    String snapshot = "ios-tunnels";

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    TESTRIGS_PREFIX + snapshot,
                    ImmutableList.of("n1-no-static-route", "n2-no-static-route"))
                .build(),
            _folder);

    Edge overlayEdge = Edge.of("n1-no-static-route", "Tunnel1", "n2-no-static-route", "Tunnel1");

    // Overlay edge present in initial tunnel topology
    assertThat(
        batfish
            .getTopologyProvider()
            .getInitialTunnelTopology(batfish.getNetworkSnapshot())
            .asEdgeSet(),
        containsInAnyOrder(overlayEdge, overlayEdge.reverse()));

    batfish.computeDataPlane();
    // NO overlay edge in final L3 topology
    assertThat(
        batfish.getTopologyProvider().getLayer3Topology(batfish.getNetworkSnapshot()).getEdges(),
        empty());
  }

  @Test
  public void testTunnelTopologyWithReachability() throws IOException {
    String snapshot = "ios-tunnels";

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(
                    TESTRIGS_PREFIX + snapshot,
                    ImmutableList.of("n1-static-route", "n2-static-route"))
                .build(),
            _folder);

    Edge underlayEdge =
        Edge.of(
            "n1-static-route", "TenGigabitEthernet0/1", "n2-static-route", "TenGigabitEthernet0/1");
    Edge overlayEdge = Edge.of("n1-static-route", "Tunnel1", "n2-static-route", "Tunnel1");

    // Overlay edge present in initial tunnel topology
    assertThat(
        batfish
            .getTopologyProvider()
            .getInitialTunnelTopology(batfish.getNetworkSnapshot())
            .asEdgeSet(),
        containsInAnyOrder(overlayEdge, overlayEdge.reverse()));

    batfish.computeDataPlane();
    // overlay edge in final L3 topology as well
    assertThat(
        batfish.getTopologyProvider().getLayer3Topology(batfish.getNetworkSnapshot()).getEdges(),
        containsInAnyOrder(
            overlayEdge, overlayEdge.reverse(), underlayEdge, underlayEdge.reverse()));
  }
}

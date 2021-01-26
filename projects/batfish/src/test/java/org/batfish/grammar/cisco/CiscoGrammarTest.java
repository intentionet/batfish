package org.batfish.grammar.cisco;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.CommonUtil.sha256Digest;
import static org.batfish.common.util.Resources.readResource;
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
import static org.batfish.datamodel.BgpRoute.DEFAULT_LOCAL_PREFERENCE;
import static org.batfish.datamodel.Flow.builder;
import static org.batfish.datamodel.Interface.UNSET_LOCAL_INTERFACE;
import static org.batfish.datamodel.Names.generatedBgpCommonExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerExportPolicyName;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrc;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.datamodel.acl.TraceElements.matchedByAclLine;
import static org.batfish.datamodel.matchers.AaaAuthenticationLoginListMatchers.hasMethod;
import static org.batfish.datamodel.matchers.AaaAuthenticationLoginMatchers.hasListForKey;
import static org.batfish.datamodel.matchers.AaaAuthenticationMatchers.hasLogin;
import static org.batfish.datamodel.matchers.AaaMatchers.hasAuthentication;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHop;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasProtocol;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasTag;
import static org.batfish.datamodel.matchers.AclLineMatchers.isExprAclLineThat;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.hasConjuncts;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.isAndMatchExprThat;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEbgp;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEquivalentAsPathMatchMode;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasRouterId;
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
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVendorFamily;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrf;
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
import static org.batfish.datamodel.matchers.DataModelMatchers.hasParseWarning;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasPostTransformationIncomingFilter;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasPreTransformationOutgoingFilter;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRedFlagWarning;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasReferencedStructure;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRoute6FilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterList;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasZone;
import static org.batfish.datamodel.matchers.DataModelMatchers.isIpSpaceReferenceThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.isPermittedByAclThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.permits;
import static org.batfish.datamodel.matchers.EigrpMetricMatchers.hasDelay;
import static org.batfish.datamodel.matchers.EigrpRouteMatchers.hasEigrpMetric;
import static org.batfish.datamodel.matchers.ExprAclLineMatchers.hasMatchCondition;
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
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDeclaredNames;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDescription;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasEigrp;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasEncapsulationVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasHsrpGroup;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasHsrpVersion;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasInterfaceType;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasIsis;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasNativeVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfAreaName;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfNetworkType;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSpeed;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortEncapsulation;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPassive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPointToPoint;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isProxyArp;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isSwitchport;
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
import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.hasHeaderSpace;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.isMatchHeaderSpaceThat;
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
import static org.batfish.datamodel.matchers.SnmpServerMatchers.hasCommunities;
import static org.batfish.datamodel.matchers.TraceTreeMatchers.isTraceTree;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasEigrpProcesses;
import static org.batfish.datamodel.matchers.VrfMatchers.hasOspfProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasSnmpServer;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
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
import static org.batfish.main.BatfishTestUtils.TEST_SNAPSHOT;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.cisco.CiscoConfiguration.DEFAULT_STATIC_ROUTE_DISTANCE;
import static org.batfish.representation.cisco.CiscoConfiguration.DENY_SAME_SECURITY_TRAFFIC_INTER_TRACE_ELEMENT;
import static org.batfish.representation.cisco.CiscoConfiguration.DENY_SAME_SECURITY_TRAFFIC_INTRA_TRACE_ELEMENT;
import static org.batfish.representation.cisco.CiscoConfiguration.PERMIT_SAME_SECURITY_TRAFFIC_INTER_TRACE_ELEMENT;
import static org.batfish.representation.cisco.CiscoConfiguration.PERMIT_SAME_SECURITY_TRAFFIC_INTRA_TRACE_ELEMENT;
import static org.batfish.representation.cisco.CiscoConfiguration.PERMIT_TRAFFIC_FROM_DEVICE;
import static org.batfish.representation.cisco.CiscoConfiguration.asaDeniedByOutputFilterTraceElement;
import static org.batfish.representation.cisco.CiscoConfiguration.asaPermitHigherSecurityLevelTrafficTraceElement;
import static org.batfish.representation.cisco.CiscoConfiguration.asaPermitLowerSecurityLevelTraceElement;
import static org.batfish.representation.cisco.CiscoConfiguration.asaPermittedByOutputFilterTraceElement;
import static org.batfish.representation.cisco.CiscoConfiguration.asaRejectLowerSecurityLevelTraceElement;
import static org.batfish.representation.cisco.CiscoConfiguration.computeASASecurityLevelZoneName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeBgpPeerImportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeCombinedOutgoingAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeIcmpObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeInspectClassMapAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeInspectPolicyMapAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeProtocolObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeServiceObjectAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeServiceObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeZonePairAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.eigrpNeighborExportPolicyName;
import static org.batfish.representation.cisco.CiscoConfiguration.eigrpNeighborImportPolicyName;
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
import static org.batfish.representation.cisco.CiscoStructureType.NAT_POOL;
import static org.batfish.representation.cisco.CiscoStructureType.NETWORK_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureType.NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.POLICY_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX6_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.PROTOCOL_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.SECURITY_ZONE;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.TRACK;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_EIGRP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.INSPECT_POLICY_MAP_INSPECT_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_BFD_TEMPLATE;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
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
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.common.bdd.BDDMatchers;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.IpsecUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.Bgpv4Route;
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
import org.batfish.datamodel.EigrpRoute;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.Flow;
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
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TunnelConfiguration;
import org.batfish.datamodel.TunnelConfiguration.Builder;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.VrfLeakingConfig;
import org.batfish.datamodel.VrfLeakingConfig.BgpLeakConfig;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclTracer;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.bgp.BgpConfederation;
import org.batfish.datamodel.bgp.BgpTopologyUtils;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.eigrp.ClassicMetric;
import org.batfish.datamodel.eigrp.EigrpInterfaceSettings;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.eigrp.EigrpMetricValues;
import org.batfish.datamodel.eigrp.EigrpMetricVersion;
import org.batfish.datamodel.eigrp.EigrpNeighborConfig;
import org.batfish.datamodel.eigrp.EigrpProcessMode;
import org.batfish.datamodel.eigrp.WideMetric;
import org.batfish.datamodel.matchers.BgpRouteMatchers;
import org.batfish.datamodel.matchers.ConfigurationMatchers;
import org.batfish.datamodel.matchers.EigrpInterfaceSettingsMatchers;
import org.batfish.datamodel.matchers.EigrpMetricMatchers;
import org.batfish.datamodel.matchers.HsrpGroupMatchers;
import org.batfish.datamodel.matchers.IkePhase1KeyMatchers;
import org.batfish.datamodel.matchers.IkePhase1ProposalMatchers;
import org.batfish.datamodel.matchers.IpAccessListMatchers;
import org.batfish.datamodel.matchers.IpsecPeerConfigMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2PolicyMatchers;
import org.batfish.datamodel.matchers.IpsecPhase2ProposalMatchers;
import org.batfish.datamodel.matchers.Route6FilterListMatchers;
import org.batfish.datamodel.matchers.RouteFilterListMatchers;
import org.batfish.datamodel.matchers.StubSettingsMatchers;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.route.nh.NextHopDiscard;
import org.batfish.datamodel.route.nh.NextHopIp;
import org.batfish.datamodel.route.nh.NextHopVrf;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunityConjunction;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetEigrpMetric;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.trace.TraceTree;
import org.batfish.datamodel.tracking.DecrementPriority;
import org.batfish.datamodel.tracking.TrackInterface;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.dataplane.protocols.BgpProtocolHelper;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.cisco.BgpRedistributionPolicy;
import org.batfish.representation.cisco.CiscoAsaNat;
import org.batfish.representation.cisco.CiscoAsaNat.Section;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.batfish.representation.cisco.CiscoIosDynamicNat;
import org.batfish.representation.cisco.CiscoIosNat;
import org.batfish.representation.cisco.CiscoIosNat.RuleAction;
import org.batfish.representation.cisco.DistributeList;
import org.batfish.representation.cisco.DistributeList.DistributeListFilterType;
import org.batfish.representation.cisco.EigrpProcess;
import org.batfish.representation.cisco.NetworkObject;
import org.batfish.representation.cisco.NetworkObjectAddressSpecifier;
import org.batfish.representation.cisco.NetworkObjectGroupAddressSpecifier;
import org.batfish.representation.cisco.OspfNetworkType;
import org.batfish.representation.cisco.Tunnel.TunnelMode;
import org.batfish.representation.cisco.VrfAddressFamily;
import org.batfish.representation.cisco.WildcardAddressSpecifier;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link CiscoParser} and {@link CiscoControlPlaneExtractor}. */
public final class CiscoGrammarTest {

  // TODO: confirm, link
  private static final String DEFAULT_VRF_NAME = "default";

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco/testconfigs/";
  private static final String TESTRIGS_PREFIX = "org/batfish/grammar/cisco/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private @Nonnull Bgpv4Route processRouteIn(RoutingPolicy routingPolicy, Bgpv4Route route) {
    Bgpv4Route.Builder builder = route.toBuilder();
    assertTrue(routingPolicy.process(route, builder, Direction.IN));
    return builder.build();
  }

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private Flow createFlow(IpProtocol protocol, int srcPort, int dstPort) {
    return Flow.builder()
        .setIngressNode("")
        .setIpProtocol(protocol)
        .setSrcPort(srcPort)
        .setDstPort(dstPort)
        .build();
  }

  private Flow createIcmpFlow(Integer icmpType) {
    return Flow.builder()
        .setIngressNode("")
        .setIpProtocol(IpProtocol.ICMP)
        .setIcmpType(icmpType)
        .setIcmpCode(0)
        .build();
  }

  private CiscoConfiguration parseCiscoConfig(String hostname, ConfigurationFormat format) {
    String src = readResource(TESTCONFIGS_PREFIX + hostname, UTF_8);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    CiscoCombinedParser ciscoParser = new CiscoCombinedParser(src, settings, format);
    CiscoControlPlaneExtractor extractor =
        new CiscoControlPlaneExtractor(src, ciscoParser, format, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(
            ciscoParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(TEST_SNAPSHOT, tree);
    CiscoConfiguration vendorConfiguration =
        (CiscoConfiguration) extractor.getVendorConfiguration();
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + hostname);
    return SerializationUtils.clone(vendorConfiguration);
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    SortedMap<String, SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>>>
        undefinedReferences =
            batfish
                .loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot())
                .getUndefinedReferences();

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
  public void testArubaConfigurationFormat() throws IOException {
    Configuration arubaConfig = parseConfig("arubaConfiguration");

    assertThat(arubaConfig, hasConfigurationFormat(equalTo(ConfigurationFormat.ARUBAOS)));
  }

  @Test
  public void testAsaAclObject() throws IOException {
    String hostname = "asa-acl-object";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
  public void testAsaBanner() throws IOException {
    Configuration c = parseConfig("asa_banner");
    assertThat(
        c.getVendorFamily().getCisco().getBanners().get("exec"),
        equalTo("^C\nAbove not actually a delimiter. This text is added."));
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
  public void testAsaFailoverExtraction() {
    CiscoConfiguration config = parseCiscoConfig("asa-failover", ConfigurationFormat.CISCO_ASA);
    assertThat(config.getFailover(), equalTo(true));
    assertThat(config.getFailoverSecondary(), equalTo(false));
    assertThat(config.getFailoverCommunicationInterface(), equalTo("GigabitEthernet0/2"));
    assertThat(config.getFailoverCommunicationInterfaceAlias(), equalTo("FAILOVER"));
    assertThat(config.getFailoverStatefulSignalingInterface(), equalTo("GigabitEthernet0/3"));
    assertThat(config.getFailoverStatefulSignalingInterfaceAlias(), equalTo("REPLICATION"));
    assertThat(
        config.getFailoverPrimaryAddresses(),
        hasEntry("FAILOVER", ConcreteInterfaceAddress.parse("172.16.1.1/30")));
    assertThat(
        config.getFailoverStandbyAddresses(),
        hasEntry("FAILOVER", ConcreteInterfaceAddress.parse("172.16.1.2/30")));
  }

  @Test
  public void testAsaFailoverConversion() throws IOException {
    Configuration config = parseConfig("asa-failover");
    assertThat(
        config,
        hasInterface(
            "GigabitEthernet0/2", hasAddress(ConcreteInterfaceAddress.parse("172.16.1.1/30"))));
  }

  @Test
  public void testAsaInterfaceRedundantExtraction() {
    CiscoConfiguration config =
        parseCiscoConfig("asa-interface-redundant", ConfigurationFormat.CISCO_ASA);

    assertThat(
        config.getInterfaces(),
        hasKeys(
            "GigabitEthernet0/1",
            "GigabitEthernet0/2",
            "Redundant1",
            "Redundant1.2",
            "Redundant2",
            "Redundant2.2"));
    {
      org.batfish.representation.cisco.Interface iface = config.getInterfaces().get("Redundant1");
      assertThat(
          iface.getMemberInterfaces(),
          containsInAnyOrder("GigabitEthernet0/1", "GigabitEthernet0/2"));
    }
    {
      org.batfish.representation.cisco.Interface iface = config.getInterfaces().get("Redundant2");
      assertThat(iface.getMemberInterfaces(), empty());
    }
  }

  @Test
  public void testAsaInterfaceRedundantConversion() throws IOException {
    Configuration config = parseConfig("asa-interface-redundant");

    assertThat(
        config.getAllInterfaces(),
        hasKeys(
            "GigabitEthernet0/1",
            "GigabitEthernet0/2",
            "Redundant1",
            "redundant1sub",
            "Redundant2",
            "redundant2sub"));
    {
      Interface iface = config.getAllInterfaces().get("Redundant1");
      assertThat(
          iface.getDependencies(),
          containsInAnyOrder(
              new Dependency("GigabitEthernet0/1", DependencyType.AGGREGATE),
              new Dependency("GigabitEthernet0/2", DependencyType.AGGREGATE)));
      assertThat(iface.getBandwidth(), equalTo(1E9D));
      assertTrue(iface.getActive());
    }
    {
      Interface iface = config.getAllInterfaces().get("redundant1sub");
      assertThat(iface.getBandwidth(), equalTo(1E9D));
      assertTrue(iface.getActive());
    }
    {
      Interface iface = config.getAllInterfaces().get("Redundant2");
      assertThat(iface.getDependencies(), empty());
      assertThat(iface.getBandwidth(), equalTo(0.0D));
      assertFalse(iface.getActive());
    }
    {
      Interface iface = config.getAllInterfaces().get("redundant2sub");
      assertThat(iface.getBandwidth(), equalTo(0.0D));
      assertFalse(iface.getActive());
    }
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

    // FILTER_IN is applied post-transformation
    assertThat(
        c,
        hasInterface(
            ifaceAlias,
            hasPostTransformationIncomingFilter(IpAccessListMatchers.hasName("FILTER_IN"))));
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
    Flow anyFlow = createFlow(IpProtocol.OSPF, 0, 0);

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
  public void testAsaGh5875() throws IOException {
    Configuration c = parseConfig("asa-gh-5875");
    BDDPacket p = new BDDPacket();

    {
      String aclName = computeServiceObjectGroupAclName("IP_GROUP");
      assertThat(c, hasIpAccessList(aclName));
      IpAccessList acl = c.getIpAccessLists().get(aclName);
      assertThat(IpAccessListToBdd.toBDD(p, acl), BDDMatchers.isOne());
    }
    {
      String aclName = computeServiceObjectGroupAclName("TCP_GROUP");
      assertThat(c, hasIpAccessList(aclName));
      IpAccessList acl = c.getIpAccessLists().get(aclName);
      assertThat(IpAccessListToBdd.toBDD(p, acl), equalTo(p.getIpProtocol().value(IpProtocol.TCP)));
    }
    {
      String aclName = computeServiceObjectGroupAclName("AH_GROUP");
      assertThat(c, hasIpAccessList(aclName));
      IpAccessList acl = c.getIpAccessLists().get(aclName);
      assertThat(IpAccessListToBdd.toBDD(p, acl), equalTo(p.getIpProtocol().value(IpProtocol.AHP)));
    }
  }

  @Test
  public void testAsaGh6042() throws IOException {
    String hostname = "asa-gh-6042";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(
        ccae, hasNumReferrers("configs/" + hostname, IPV4_ACCESS_LIST_EXTENDED, "acl_name", 1));
  }

  @Test
  public void testAsaServiceObject() throws IOException {
    String hostname = "asa-service-object";
    String filename = "configs/" + hostname;
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    String osIcmpAclName = computeServiceObjectAclName("OS_ICMP");
    String osTcpAclName = computeServiceObjectAclName("OS_TCPUDP");
    String ogsAclName = computeServiceObjectGroupAclName("OGS1");
    String ogsUndefOsName = computeServiceObjectGroupAclName("OGS_UNDEF_OS");
    String ogsUndefOgsName = computeServiceObjectGroupAclName("OGS_UNDEF_OGS");

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
    assertThat(ccae, hasUndefinedReference(filename, SERVICE_OBJECT_GROUP, "OGS_UNDEFINED"));
    /* Confirm reference to builtin does not result in undefined reference. */
    assertThat(ccae, not(hasUndefinedReference(filename, SERVICE_OBJECT, "icmp")));

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

    /* Confirm undefined references reject and do not cause crash */
    assertThat(c, hasIpAccessList(ogsUndefOsName, not(accepts(flowUdpFail, null, c))));
    assertThat(c, hasIpAccessList(ogsUndefOgsName, not(accepts(flowUdpFail, null, c))));
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
  public void testAsaSnmp() throws IOException {
    Configuration c = parseConfig("asa_snmp");
    assertThat(c.getSnmpSourceInterface(), equalTo("inside"));
    assertThat(c.getSnmpTrapServers(), contains("1.2.3.4"));
  }

  @Test
  public void testCadantBanner() throws IOException {
    Configuration c = parseConfig("cadant_banner");
    assertThat(
        c.getVendorFamily().getCisco().getBanners().get("login"),
        equalTo("Some text\nSome more text\n"));
  }

  @Test
  public void testIosUnicodeConversion() throws IOException {
    Configuration c = parseConfig("ios-unicode");
    assertThat(c, hasInterface("GigabitEthernet0/0", hasAddress("10.0.0.1/24")));
  }

  @Test
  public void testIosUnicodeBomConversion() throws IOException {
    Configuration c = parseConfig("ios-unicode-bom");
    assertThat(c, hasInterface("GigabitEthernet0/0", hasAddress("10.0.0.1/24")));
  }

  @Test
  public void testIosLineParsing() {
    assertNotNull(parseCiscoConfig("ios-line", null));
  }

  @Test
  public void testIosEncapsulationVlanExtraction() {
    CiscoConfiguration vc =
        parseCiscoConfig("ios-encapsulation-vlan", ConfigurationFormat.CISCO_IOS);
    assertThat(
        vc.getInterfaces().get("GigabitEthernet0/1.203").getEncapsulationVlan(), equalTo(203));
  }

  @Test
  public void testIosEncapsulationVlanConversion() throws IOException {
    Configuration c = parseConfig("ios-encapsulation-vlan");
    assertThat(c, hasInterface("GigabitEthernet0/1.203", hasEncapsulationVlan(203)));
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
    // NB this route-map permits /7 because matching standard ip access-list on a network
    // is just matching the network address against the src IP.
    assertThat(c, hasRouteFilterList("10", permits(Prefix.parse("10.0.0.0/7"))));
    assertThat(
        c, hasRouteFilterList("10", RouteFilterListMatchers.rejects(Prefix.parse("11.0.0.0/8"))));
    assertThat(
        c,
        hasIpAccessList(
            "10",
            accepts(
                Flow.builder()
                    .setSrcIp(Ip.parse("10.1.1.1"))
                    .setDstIp(Ip.parse("11.1.1.1"))
                    .setIngressNode(hostname)
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
                    .build(),
                "Ethernet1",
                c)));
    // Check Ipv6 as well
    assertThat(c, hasRoute6FilterList("v6list", permits(Prefix6.parse("::FFFF:10.0.0.0/105"))));
    assertThat(
        c,
        hasRoute6FilterList(
            "v6list", Route6FilterListMatchers.rejects(Prefix6.parse("::FFFF:10.0.0.0/103"))));
  }

  @Test
  public void testIosBanner() throws IOException {
    Configuration c = parseConfig("ios_banner");
    assertThat(
        c.getVendorFamily().getCisco().getBanners(),
        equalTo(
            ImmutableMap.of(
                "exec",
                "First line.\nSecond line, with no ignored text.",
                "login",
                "First line.\nSecond line.")));
  }

  @Test
  public void testIosRipPassive() throws IOException {
    Configuration c = parseConfig("ios-rip-passive");

    String iface1 = "Ethernet0";
    String iface2 = "Ethernet1";
    Map<String, Interface> ifaces = c.getAllInterfaces();
    assertThat(ifaces, hasKeys(iface1, iface2));

    assertThat(ifaces.get(iface1).getRipPassive(), equalTo(true));
    assertThat(ifaces.get(iface2).getRipPassive(), equalTo(false));
  }

  @Test
  public void testIosAclObjectGroup() throws IOException {
    String hostname = "ios-acl-object-group";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // Confirm reference counts are correct for ACLs
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "AL", 2));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "AL_IF", 2));
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
  }

  @Test
  public void testIosBfdTemplate() throws IOException {
    String hostname = "ios-bfd-template";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(ccae, hasNumReferrers(filename, BFD_TEMPLATE, "bfd-template-unused", 0));
    assertThat(ccae, hasNumReferrers(filename, BFD_TEMPLATE, "bfd-template-used", 1));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, BFD_TEMPLATE, "bfd-template-undefined", INTERFACE_BFD_TEMPLATE));
  }

  @Test
  public void testIosBgpConfedExtraction() {
    CiscoConfiguration c = parseCiscoConfig("ios-bgp-confed", ConfigurationFormat.CISCO_IOS);
    org.batfish.representation.cisco.BgpProcess p = c.getDefaultVrf().getBgpProcess();
    assertThat(p, notNullValue());
    assertThat(p.getConfederation(), equalTo(65100L));
    assertThat(p.getConfederationMembers(), contains(65134L));
  }

  @Test
  public void testIosBgpConfedConversion() throws IOException {
    Configuration c = parseConfig("ios-bgp-confed");
    BgpProcess p = c.getDefaultVrf().getBgpProcess();
    assertThat(p, notNullValue());
    assertThat(p.getConfederation(), equalTo(new BgpConfederation(65100, ImmutableSet.of(65134L))));
    {
      assertThat(p.getActiveNeighbors(), hasKey(Prefix.parse("192.168.123.2/32")));
      BgpActivePeerConfig neighbor = p.getActiveNeighbors().get(Prefix.parse("192.168.123.2/32"));
      assertThat(neighbor.getConfederationAsn(), equalTo(65100L));
      assertThat(neighbor.getLocalAs(), equalTo(65112L));
      assertThat(neighbor.getRemoteAsns().enumerate(), contains(65112L));
    }
    {
      assertThat(p.getActiveNeighbors(), hasKey(Prefix.parse("192.168.123.3/32")));
      BgpActivePeerConfig neighbor = p.getActiveNeighbors().get(Prefix.parse("192.168.123.3/32"));
      assertThat(neighbor.getConfederationAsn(), equalTo(65100L));
      assertThat(neighbor.getLocalAs(), equalTo(65112L));
      assertThat(neighbor.getRemoteAsns().enumerate(), contains(65134L));
    }
  }

  @Test
  public void testIosBgpEnforceAsExtraction() {
    {
      CiscoConfiguration c =
          parseCiscoConfig("ios-bgp-enforce-first-as-disabled", ConfigurationFormat.CISCO_IOS);
      org.batfish.representation.cisco.BgpProcess p = c.getDefaultVrf().getBgpProcess();
      assertThat(p, notNullValue());
      assertThat(p.getEnforceFirstAs(), equalTo(Boolean.FALSE));
    }
    {
      CiscoConfiguration c =
          parseCiscoConfig("ios-bgp-enforce-first-as-explicit", ConfigurationFormat.CISCO_IOS);
      org.batfish.representation.cisco.BgpProcess p = c.getDefaultVrf().getBgpProcess();
      assertThat(p, notNullValue());
      assertThat(p.getEnforceFirstAs(), equalTo(Boolean.TRUE));
    }
    {
      CiscoConfiguration c =
          parseCiscoConfig("ios-bgp-enforce-first-as-default", ConfigurationFormat.CISCO_IOS);
      org.batfish.representation.cisco.BgpProcess p = c.getDefaultVrf().getBgpProcess();
      assertThat(p, notNullValue());
      assertThat(p.getEnforceFirstAs(), nullValue());
    }
  }

  @Test
  public void testIosBgpEnforceAsConversion() throws IOException {
    Prefix peer = Prefix.parse("1.2.3.4/32");
    {
      Configuration c = parseConfig("ios-bgp-enforce-first-as-disabled");
      BgpProcess p = c.getDefaultVrf().getBgpProcess();
      assertThat(p, notNullValue());
      assertThat(p.getActiveNeighbors(), hasKeys(peer));
      BgpActivePeerConfig n = p.getActiveNeighbors().get(peer);
      assertFalse(n.getEnforceFirstAs());
    }
    {
      Configuration c = parseConfig("ios-bgp-enforce-first-as-explicit");
      BgpProcess p = c.getDefaultVrf().getBgpProcess();
      assertThat(p, notNullValue());
      assertThat(p.getActiveNeighbors(), hasKeys(peer));
      BgpActivePeerConfig n = p.getActiveNeighbors().get(peer);
      assertTrue(n.getEnforceFirstAs());
    }
    {
      Configuration c = parseConfig("ios-bgp-enforce-first-as-default");
      BgpProcess p = c.getDefaultVrf().getBgpProcess();
      assertThat(p, notNullValue());
      assertThat(p.getActiveNeighbors(), hasKeys(peer));
      BgpActivePeerConfig n = p.getActiveNeighbors().get(peer);
      assertTrue(n.getEnforceFirstAs());
    }
  }

  @Test
  public void testIosBgpPrefixListReferences() throws IOException {
    String hostname = "ios_bgp_prefix_list_references";
    String filename = String.format("configs/%s", hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    // Confirm that BGP peer on r1 is missing its local IP, as expected
    Prefix r1NeighborPeerAddress = Prefix.parse("2.2.2.2/32");
    Configuration r1 = batfish.loadConfigurations(batfish.getSnapshot()).get("r1");
    SortedMap<Prefix, BgpActivePeerConfig> r1Peers =
        r1.getVrfs().get(DEFAULT_VRF_NAME).getBgpProcess().getActiveNeighbors();
    assertTrue(r1Peers.containsKey(r1NeighborPeerAddress));
    assertThat(r1Peers.get(r1NeighborPeerAddress).getLocalIp(), nullValue());

    /*
    r1 has a static route to 7.7.7.7/32; r2 has a static route to 8.8.8.8/32. Confirm that both
    received a BGP route to the other's static route.
    */
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    Set<AbstractRoute> r1Routes = dp.getRibs().get("r1").get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> r2Routes = dp.getRibs().get("r2").get(DEFAULT_VRF_NAME).getRoutes();
    assertThat(r1Routes, hasItem(isBgpv4RouteThat(hasPrefix(Prefix.parse("8.8.8.8/32")))));
    assertThat(r2Routes, hasItem(isBgpv4RouteThat(hasPrefix(Prefix.parse("7.7.7.7/32")))));
  }

  @Test
  public void testIosEigrpFilterList() throws IOException {
    String testrigName = "eigrp-distribute-list-wildcard";
    List<String> configurationNames = ImmutableList.of("sender", "receiver");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    Set<Prefix> receivedEigrpRoutes =
        dp.getRibs().get("receiver").get(DEFAULT_VRF_NAME).getRoutes().stream()
            .filter(r -> r instanceof EigrpRoute)
            .map(AbstractRoute::getNetwork)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(
        receivedEigrpRoutes,
        containsInAnyOrder(
            Prefix.parse("128.0.64.0/18"),
            Prefix.parse("128.0.64.0/19"),
            Prefix.parse("128.0.64.0/20"),
            Prefix.parse("128.0.64.0/21"),
            Prefix.parse("128.0.64.0/22"),
            Prefix.parse("128.0.64.0/23"),
            Prefix.parse("128.0.64.0/24"),
            Prefix.parse("128.0.64.1/32"),
            Prefix.parse("128.0.64.2/31"),
            Prefix.parse("128.0.64.4/30"),
            Prefix.parse("128.0.64.8/29"),
            Prefix.parse("128.0.64.16/28"),
            Prefix.parse("128.0.64.32/27"),
            Prefix.parse("128.0.64.64/26"),
            Prefix.parse("128.0.64.128/25"),
            Prefix.parse("128.1.64.0/32")));
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
    assertThat(c, hasVrf("vrf-name", hasEigrpProcesses(hasKey(2L))));

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
    assertThat(c, hasVrf("vrf-name", hasEigrpProcesses(hasKey(2L))));

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
    assertThat(c, hasVrf("vrf-name", hasEigrpProcesses(hasKey(2L))));
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

  /** Test named EIGRP process with passive interfaces */
  @Test
  public void testIosPortChannelEigrpMetric() throws IOException {
    Configuration c = parseConfig("ios-portchannel-eigrp");

    /* Port-channel23 should have EIGRP bandwidth based on member interfaces' bandwidths */
    long expectedBw = (long) 2e9;
    Interface portChannel23 = c.getAllInterfaces().get("Port-channel23");
    assertThat(portChannel23, hasBandwidth(expectedBw));
    assertThat(
        portChannel23,
        hasEigrp(
            EigrpInterfaceSettingsMatchers.hasEigrpMetric(
                EigrpMetricMatchers.hasBandwidth(expectedBw / 1000)))); // scale to kbps
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
        c.getVrfs().get(DEFAULT_VRF_NAME).getEigrpProcesses().get(2L).getRedistributionPolicy();
    assertThat(exportPolicyName, notNullValue());
    RoutingPolicy routingPolicy = c.getRoutingPolicies().get(exportPolicyName);
    assertThat(routingPolicy, notNullValue());

    EigrpExternalRoute.Builder outputRouteBuilder =
        EigrpExternalRoute.testBuilder().setEigrpMetricVersion(EigrpMetricVersion.V1);
    outputRouteBuilder
        .setDestinationAsn(1L)
        .setNetwork(Prefix.parse("1.0.0.0/32"))
        .setProcessAsn(2L);

    EigrpMetric originalMetric =
        ClassicMetric.builder()
            .setValues(EigrpMetricValues.builder().setBandwidth(2e9).setDelay(4e5).build())
            .build();

    // VirtualEigrpProcess sets metric to route metric by default
    outputRouteBuilder.setEigrpMetric(originalMetric);

    EigrpInternalRoute originalRoute =
        EigrpInternalRoute.testBuilder()
            .setAdmin(90)
            .setEigrpMetric(originalMetric)
            .setEigrpMetricVersion(EigrpMetricVersion.V1)
            .setNetwork(outputRouteBuilder.getNetwork())
            .setProcessAsn(1L)
            .build();
    assertNotNull(originalRoute);

    // Check if routingPolicy accepts EIGRP route and sets correct metric from original route
    assertTrue(routingPolicy.process(originalRoute, outputRouteBuilder, Direction.OUT));
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
    org.batfish.datamodel.eigrp.EigrpProcess eigrpProcess =
        c.getVrfs().get(DEFAULT_VRF_NAME).getEigrpProcesses().get(asn);
    String exportPolicyName = eigrpProcess.getRedistributionPolicy();
    assertThat(exportPolicyName, notNullValue());
    RoutingPolicy routingPolicy = c.getRoutingPolicies().get(exportPolicyName);
    assertThat(routingPolicy, notNullValue());

    EigrpExternalRoute.Builder outputRouteBuilder =
        EigrpExternalRoute.testBuilder().setEigrpMetricVersion(EigrpMetricVersion.V1);
    outputRouteBuilder
        .setDestinationAsn(asn)
        .setNetwork(Prefix.parse("1.0.0.0/32"))
        .setProcessAsn(asn);
    ClassicMetric.Builder metricBuilder = ClassicMetric.builder();

    // Check if routingPolicy accepts connected route and sets correct metric
    assertTrue(
        routingPolicy.process(
            new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "Loopback0"),
            outputRouteBuilder,
            eigrpProcess,
            Direction.OUT));
    assertThat(
        outputRouteBuilder.build(),
        hasEigrpMetric(
            metricBuilder
                .setValues(
                    EigrpMetricValues.builder().setBandwidth(100).setDelay(100_000_000).build())
                .build()));

    // Check if routingPolicy rejects RIP route
    assertFalse(
        routingPolicy.process(
            RipInternalRoute.builder()
                .setNetwork(Prefix.parse("2.2.2.2/32"))
                .setNextHop(NextHopIp.of(Ip.parse("3.3.3.3")))
                .setAdmin(1)
                .setMetric(1)
                .build(),
            outputRouteBuilder,
            eigrpProcess,
            Direction.OUT));

    // Check if routingPolicy accepts OSPF route and sets correct default metric
    assertTrue(
        routingPolicy.process(
            OspfIntraAreaRoute.builder()
                .setNextHop(NextHopIp.of(Ip.parse("5.5.5.5")))
                .setNetwork(Prefix.parse("4.4.4.4/32"))
                .setAdmin(1)
                .setMetric(1)
                .setArea(1L)
                .build(),
            outputRouteBuilder,
            eigrpProcess,
            Direction.OUT));
    assertThat(
        outputRouteBuilder.build(),
        hasEigrpMetric(
            metricBuilder
                .setValues(
                    EigrpMetricValues.builder().setBandwidth(200).setDelay(200_000_000L).build())
                .build()));
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
            .build();
    assertThat(eth2Acl, rejects(deniedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth2Acl, rejects(deniedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, rejects(deniedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, rejects(deniedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testIosInterfaceDelay() throws IOException {
    Configuration c = parseConfig("ios-interface-delay");

    // All delays in picoseconds. For table of values based on bandwidths see
    // https://tools.ietf.org/html/rfc7868#section-5.6.1.2
    assertThat(
        c,
        hasInterface(
            "GigabitEthernet0/0",
            hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(10_000_000L)))));
    assertThat(
        c,
        hasInterface(
            "GigabitEthernet0/1",
            hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(10_000_000_000L)))));
    assertThat(
        c,
        hasInterface(
            "FastEthernet0/1",
            hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(100_000_000L)))));
    assertThat(
        c,
        hasInterface(
            "Loopback0",
            hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(5_000_000_000L)))));
    assertThat(
        c,
        hasInterface(
            "Tunnel0",
            hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(50_000_000_000L)))));
    assertThat(
        c,
        hasInterface(
            "Port-channel1",
            hasEigrp(EigrpInterfaceSettingsMatchers.hasEigrpMetric(hasDelay(10_000_000L)))));
  }

  @Test
  public void testIosInterfaceSpeed() throws IOException {
    Configuration c = parseConfig("ios-interface-speed");

    assertThat(
        c,
        hasInterfaces(
            hasKeys(
                "GigabitEthernet0/0",
                "GigabitEthernet0/1",
                "GigabitEthernet0/2",
                "Loopback0",
                "Tunnel0",
                "TwoGigabitEthernet1",
                "TwoGigabitEthernet2",
                "TwentyFiveGigE3",
                "TwentyFiveGigE4")));
    assertThat(c, hasInterface("GigabitEthernet0/0", hasBandwidth(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/0", hasSpeed(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/1", hasBandwidth(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/1", hasSpeed(1E9D)));
    assertThat(c, hasInterface("GigabitEthernet0/2", hasBandwidth(100E6D)));
    assertThat(c, hasInterface("GigabitEthernet0/2", hasSpeed(100E6D)));
    assertThat(c, hasInterface("Loopback0", hasBandwidth(8E9D)));
    assertThat(c, hasInterface("Tunnel0", hasBandwidth(1E5D)));
    assertThat(c, hasInterface("TwoGigabitEthernet1", hasSpeed(2.5e9D)));
    assertThat(c, hasInterface("TwoGigabitEthernet2", hasSpeed(2.5e9D)));
    assertThat(c, hasInterface("TwentyFiveGigE3", hasSpeed(25e9D)));
    assertThat(c, hasInterface("TwentyFiveGigE4", hasSpeed(25e9D)));
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
                            StaticRoute.testBuilder()
                                .setAdministrativeCost(1)
                                .setNetwork(Prefix.ZERO)
                                .setNextHopIp(Ip.parse("1.2.3.4"))
                                .build()))))));
  }

  /** Tests that Cisco parser doesn't crash in conversion for incomplete ipsec profile. */
  @Test
  public void testIosIpsecGh5849() throws IOException {
    // doesn't crash.
    parseConfig("ios-ipsec-gh-5849");
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
                .setConfigurationFiles(
                    TESTRIGS_PREFIX + testrigName,
                    ImmutableList.of(originatorName, l1Name, l2Name, l3Name))
                .build(),
            _folder);

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    Set<AbstractRoute> l1Routes = dp.getRibs().get(l1Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> l2Routes = dp.getRibs().get(l2Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> l3Routes = dp.getRibs().get(l3Name).get(DEFAULT_VRF_NAME).getRoutes();

    // Listener 1
    Ip originatorId = Ip.parse("1.1.1.1");
    Ip originatorIp = Ip.parse("10.1.1.1");
    long originatorAs = 1L;
    Bgpv4Route expected =
        Bgpv4Route.testBuilder()
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
                .setConfigurationFiles(
                    TESTRIGS_PREFIX + testrigName, ImmutableList.of(originatorName, l1Name, l2Name))
                .build(),
            _folder);

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    Set<AbstractRoute> l1Routes = dp.getRibs().get(l1Name).get(DEFAULT_VRF_NAME).getRoutes();
    Set<AbstractRoute> l2Routes = dp.getRibs().get(l2Name).get(DEFAULT_VRF_NAME).getRoutes();

    Ip originatorId = Ip.parse("1.1.1.1");
    Ip originatorIp = Ip.parse("10.1.1.1");
    long originatorAs = 1L;
    Bgpv4Route redistributedStaticRoute =
        Bgpv4Route.testBuilder()
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
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
            .setIngressNode("")
            .setIpProtocol(IpProtocol.ICMP)
            .setIcmpCode(0)
            .setIcmpType(0)
            .build();
    Flow tcpFlow =
        Flow.builder()
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
                isExprAclLineThat(
                    hasMatchCondition(
                        isOrMatchExprThat(
                            hasDisjuncts(
                                contains(
                                    isMatchHeaderSpaceThat(
                                        hasHeaderSpace(
                                            hasIpProtocols(contains(IpProtocol.ICMP))))))))))));
    /* og-tcp */
    assertThat(
        c,
        hasIpAccessList(
            computeServiceObjectGroupAclName("og-tcp"),
            hasLines(
                isExprAclLineThat(
                    hasMatchCondition(
                        isOrMatchExprThat(
                            hasDisjuncts(
                                containsInAnyOrder(
                                    ImmutableList.of(
                                        isMatchHeaderSpaceThat(
                                            hasHeaderSpace(
                                                allOf(
                                                    hasIpProtocols(contains(IpProtocol.TCP)),
                                                    hasDstPorts(hasItem(new SubRange(65500)))))),
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
                                                                    .number())))))))))))))));
    /* og-udp */
    assertThat(
        c,
        hasIpAccessList(
            computeServiceObjectGroupAclName("og-udp"),
            hasLines(
                isExprAclLineThat(
                    hasMatchCondition(
                        isOrMatchExprThat(
                            hasDisjuncts(
                                containsInAnyOrder(
                                    ImmutableList.of(
                                        isMatchHeaderSpaceThat(
                                            hasHeaderSpace(
                                                allOf(
                                                    hasIpProtocols(contains(IpProtocol.UDP)),
                                                    hasDstPorts(hasItem(new SubRange(65501)))))),
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
                                                                NamedPort.SNMPTRAP.number())))))),
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
                                                                    .number())))))))))))))));
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    Configuration abr = configurations.get(originatorName);

    // Sanity check: ensure the ABR has a generated default route in its OSPF process
    Set<GeneratedRoute> abrOspfGeneratedRoutes =
        abr.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    assertThat(abrOspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    Configuration abr = configurations.get(originatorName);

    // Sanity check: ensure the ABR has a generated default route in its OSPF process
    Set<GeneratedRoute> abrOspfGeneratedRoutes =
        abr.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    assertThat(abrOspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    Configuration abr = configurations.get(originatorName);

    // Sanity check: ensure the ABR has a generated default route in its OSPF process
    Set<GeneratedRoute> abrOspfGeneratedRoutes =
        abr.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    assertThat(abrOspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    NetworkSnapshot snapshot = batfish.getSnapshot();

    // Sanity check: both devices have a generated default route in their OSPF processes
    Map<String, Configuration> configurations = batfish.loadConfigurations(snapshot);
    Configuration r1 = configurations.get(r1Name);
    Configuration r2 = configurations.get(r2Name);
    Set<GeneratedRoute> r1OspfGeneratedRoutes =
        r1.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    Set<GeneratedRoute> r2OspfGeneratedRoutes =
        r2.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getGeneratedRoutes();
    assertThat(r1OspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));
    assertThat(r2OspfGeneratedRoutes, contains(hasPrefix(Prefix.ZERO)));

    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
  public void testIosEigrpDistributeListConversion() throws IOException {
    Configuration c = parseConfig("ios-eigrp-distribute-list");

    String distListPolicyName = "~EIGRP_EXPORT_POLICY_default_1_GigabitEthernet0/0~";

    org.batfish.datamodel.eigrp.EigrpProcess eigrpProcess1 =
        c.getDefaultVrf().getEigrpProcesses().get(1L);
    assertThat(
        eigrpProcess1.getNeighbors().get("GigabitEthernet0/0").getExportPolicy(),
        equalTo(distListPolicyName));

    EigrpMetric metric =
        WideMetric.builder()
            .setValues(EigrpMetricValues.builder().setBandwidth(1d).setDelay(1d).build())
            .build();

    // Test redistribution policy allows redistribution from router EIGRP 2
    RoutingPolicy redistrPolicy =
        c.getRoutingPolicies().get(eigrpProcess1.getRedistributionPolicy());
    assertTrue(
        redistrPolicy.process(
            EigrpExternalRoute.testBuilder()
                .setNetwork(Prefix.parse("172.21.30.0/24"))
                .setEigrpMetric(metric)
                .setEigrpMetricVersion(EigrpMetricVersion.V1)
                .setProcessAsn(2L)
                .setDestinationAsn(5L)
                .build(),
            EigrpExternalRoute.testBuilder(),
            eigrpProcess1,
            Direction.IN));

    RoutingPolicy routingPolicy = c.getRoutingPolicies().get(distListPolicyName);

    // a route (previously) redistributed from router EIGRP 2 and allowed by distribute list
    assertTrue(
        routingPolicy.process(
            EigrpExternalRoute.testBuilder()
                .setNetwork(Prefix.parse("172.21.30.0/24"))
                .setEigrpMetric(metric)
                .setEigrpMetricVersion(EigrpMetricVersion.V1)
                .setProcessAsn(1L)
                .setDestinationAsn(5L)
                .build(),
            EigrpExternalRoute.testBuilder(),
            Direction.OUT));
    // a route (previously) redistributed from router EIGRP 2 and denied by distribute list
    assertFalse(
        routingPolicy.process(
            EigrpExternalRoute.testBuilder()
                .setNetwork(Prefix.parse("172.21.31.0/24"))
                .setEigrpMetric(metric)
                .setEigrpMetricVersion(EigrpMetricVersion.V1)
                .setProcessAsn(1L)
                .setDestinationAsn(5L)
                .build(),
            EigrpExternalRoute.testBuilder(),
            Direction.OUT));
    // an internal route sent from router EIGRP 1 and allowed by distribute list
    assertTrue(
        routingPolicy.process(
            EigrpInternalRoute.testBuilder()
                .setNetwork(Prefix.parse("172.21.30.0/24"))
                .setEigrpMetric(metric)
                .setEigrpMetricVersion(EigrpMetricVersion.V1)
                .setProcessAsn(1L)
                .build(),
            EigrpExternalRoute.testBuilder(),
            Direction.OUT));
    // a route matching distribute list but does not have the correct ASN so falls through till the
    // end and gets rejected
    assertFalse(
        routingPolicy.process(
            EigrpExternalRoute.testBuilder()
                .setNetwork(Prefix.parse("172.21.30.0/24"))
                .setEigrpMetric(metric)
                .setEigrpMetricVersion(EigrpMetricVersion.V1)
                .setProcessAsn(3L)
                .setDestinationAsn(5L)
                .build(),
            EigrpExternalRoute.testBuilder(),
            Direction.OUT));
  }

  @Test
  public void testIosEigrpMarkForRouting() throws IOException {
    Configuration c = parseConfig("ios-eigrp-distribute-list");

    assertThat(c.getRouteFilterLists(), hasKey("2"));
  }

  @Test
  public void testIosEigrpDistributeListRefsAndWarnings() throws IOException {
    String hostname = "ios-eigrp-distribute-list-refs-and-warnings";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    String filename = "configs/" + hostname;

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "1", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "2", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "3", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "4", 1));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "GigabitEthernet0/0", 9));

    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "PL_IN", 3));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "PL_OUT", 3));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "PL_GW_IN", 3));
    assertThat(ccae, hasNumReferrers(filename, PREFIX_LIST, "PL_GW_OUT", 3));

    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "RM_IN", 2));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "RM_OUT", 2));

    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(
        pvcae,
        hasParseWarning(
            filename,
            containsString("Gateway prefix lists in distribute-list are not supported for EIGRP")));
    assertThat(
        pvcae,
        hasParseWarning(
            filename, containsString("Gateways in distribute-list are not supported for EIGRP")));

    // Also check that all ACLs used as distribute lists are converted
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    assertThat(c, notNullValue());
    assertThat(
        c.getRouteFilterLists(),
        hasKeys("1", "2", "3", "4", "PL_GW_IN", "PL_GW_OUT", "PL_IN", "PL_OUT"));
  }

  @Test
  public void testIosEigrpDistributeListWithPrefixListExtraction() {
    String hostname = "ios-eigrp-distribute-list-prefix-list";
    CiscoConfiguration vc = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_IOS);
    EigrpProcess proc = vc.getDefaultVrf().getEigrpProcesses().get(1L);
    assertThat(
        proc.getInboundGlobalDistributeList(),
        equalTo(new DistributeList("PL_IN", DistributeListFilterType.PREFIX_LIST)));
    assertThat(
        proc.getOutboundGlobalDistributeList(),
        equalTo(new DistributeList("PL_OUT", DistributeListFilterType.PREFIX_LIST)));
    String ifaceName = "GigabitEthernet0/0";
    assertThat(
        proc.getInboundInterfaceDistributeLists(),
        hasEntry(
            ifaceName, new DistributeList("PL_IN_IFACE", DistributeListFilterType.PREFIX_LIST)));
    assertThat(
        proc.getOutboundInterfaceDistributeLists(),
        hasEntry(
            ifaceName, new DistributeList("PL_OUT_IFACE", DistributeListFilterType.PREFIX_LIST)));
  }

  @Test
  public void testIosEigrpDistributeListWithPrefixListConversion() throws IOException {
    String hostname = "ios-eigrp-distribute-list-prefix-list";
    Configuration c = parseConfig(hostname);
    Map<String, RoutingPolicy> policies = c.getRoutingPolicies();
    // helper builder
    EigrpInternalRoute.Builder builder =
        EigrpInternalRoute.testBuilder()
            .setAdmin(90)
            .setEigrpMetric(
                ClassicMetric.builder()
                    .setValues(EigrpMetricValues.builder().setBandwidth(2e9).setDelay(4e5).build())
                    .build())
            .setEigrpMetricVersion(EigrpMetricVersion.V1)
            .setProcessAsn(1L);
    {
      String ifaceName = "GigabitEthernet0/0";
      EigrpInterfaceSettings eigrpSettings = c.getAllInterfaces().get(ifaceName).getEigrp();
      String importPolicyName = eigrpNeighborImportPolicyName(ifaceName, DEFAULT_VRF_NAME, 1L);
      assertThat(eigrpSettings.getImportPolicy(), equalTo(importPolicyName));
      String exportPolicyName = eigrpNeighborExportPolicyName(ifaceName, DEFAULT_VRF_NAME, 1L);
      assertThat(eigrpSettings.getExportPolicy(), equalTo(exportPolicyName));

      RoutingPolicy importPolicy = policies.get(importPolicyName);
      // Allow routes permitted by both prefix lists
      assertTrue(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("1.1.1.1/31")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));
      // Block others
      assertFalse(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("1.1.1.1/26")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));
      assertFalse(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/31")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));

      RoutingPolicy exportPolicy = policies.get(exportPolicyName);
      // Allow routes permitted by both prefix lists
      assertTrue(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("2.2.2.2/30")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
      // Block others
      assertFalse(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("2.2.2.2/26")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
      assertFalse(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/30")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
    }
    {
      // This interface has no iface-specific distribute lists.
      String ifaceName = "GigabitEthernet1/0";
      EigrpInterfaceSettings eigrpSettings = c.getAllInterfaces().get(ifaceName).getEigrp();
      String importPolicyName = eigrpNeighborImportPolicyName(ifaceName, DEFAULT_VRF_NAME, 1L);
      assertThat(eigrpSettings.getImportPolicy(), equalTo(importPolicyName));
      String exportPolicyName = eigrpNeighborExportPolicyName(ifaceName, DEFAULT_VRF_NAME, 1L);
      assertThat(eigrpSettings.getExportPolicy(), equalTo(exportPolicyName));

      RoutingPolicy importPolicy = policies.get(importPolicyName);
      // Allow routes permitted by global prefix list
      assertTrue(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("1.1.1.1/26")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));
      // Block others
      assertFalse(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/31")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));

      RoutingPolicy exportPolicy = policies.get(exportPolicyName);
      // Allow routes permitted by global list
      assertTrue(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("2.2.2.2/26")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
      // Block others
      assertFalse(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/30")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
    }
  }

  @Test
  public void testIosEigrpDistributeListWithRouteMapExtraction() {
    String hostname = "ios-eigrp-distribute-list-routemap";
    CiscoConfiguration vc = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_IOS);
    EigrpProcess proc = vc.getDefaultVrf().getEigrpProcesses().get(1L);
    assertThat(
        proc.getInboundGlobalDistributeList(),
        equalTo(new DistributeList("RM_IN", DistributeListFilterType.ROUTE_MAP)));
    assertThat(
        proc.getOutboundGlobalDistributeList(),
        equalTo(new DistributeList("RM_OUT", DistributeListFilterType.ROUTE_MAP)));
    String ifaceName = "GigabitEthernet0/0";
    assertThat(
        proc.getInboundInterfaceDistributeLists(),
        hasEntry(ifaceName, new DistributeList("RM_IN_IFACE", DistributeListFilterType.ROUTE_MAP)));
    assertThat(
        proc.getOutboundInterfaceDistributeLists(),
        hasEntry(
            ifaceName, new DistributeList("RM_OUT_IFACE", DistributeListFilterType.ROUTE_MAP)));
  }

  @Test
  public void testIosEigrpDistributeListWithRouteMapConversion() throws IOException {
    String hostname = "ios-eigrp-distribute-list-routemap";
    Configuration c = parseConfig(hostname);
    Map<String, RoutingPolicy> policies = c.getRoutingPolicies();
    // helper builder
    EigrpInternalRoute.Builder builder =
        EigrpInternalRoute.testBuilder()
            .setAdmin(90)
            .setEigrpMetric(
                ClassicMetric.builder()
                    .setValues(EigrpMetricValues.builder().setBandwidth(2e9).setDelay(4e5).build())
                    .build())
            .setEigrpMetricVersion(EigrpMetricVersion.V1)
            .setProcessAsn(1L);
    {
      String ifaceName = "GigabitEthernet0/0";
      EigrpInterfaceSettings eigrpSettings = c.getAllInterfaces().get(ifaceName).getEigrp();
      String importPolicyName = eigrpNeighborImportPolicyName(ifaceName, DEFAULT_VRF_NAME, 1L);
      assertThat(eigrpSettings.getImportPolicy(), equalTo(importPolicyName));
      String exportPolicyName = eigrpNeighborExportPolicyName(ifaceName, DEFAULT_VRF_NAME, 1L);
      assertThat(eigrpSettings.getExportPolicy(), equalTo(exportPolicyName));

      RoutingPolicy importPolicy = policies.get(importPolicyName);
      // Allow routes permitted by both prefix lists
      assertTrue(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("1.1.1.1/31")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));
      // Block others
      assertFalse(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("1.1.1.1/26")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));
      assertFalse(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/31")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));

      RoutingPolicy exportPolicy = policies.get(exportPolicyName);
      // Allow routes permitted by both prefix lists
      assertTrue(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("2.2.2.2/30")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
      // Block others
      assertFalse(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("2.2.2.2/26")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
      assertFalse(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/30")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
    }
    {
      // This interface has no iface-specific distribute lists.
      String ifaceName = "GigabitEthernet1/0";
      EigrpInterfaceSettings eigrpSettings = c.getAllInterfaces().get(ifaceName).getEigrp();
      String importPolicyName = eigrpNeighborImportPolicyName(ifaceName, DEFAULT_VRF_NAME, 1L);
      assertThat(eigrpSettings.getImportPolicy(), equalTo(importPolicyName));
      String exportPolicyName = eigrpNeighborExportPolicyName(ifaceName, DEFAULT_VRF_NAME, 1L);
      assertThat(eigrpSettings.getExportPolicy(), equalTo(exportPolicyName));

      RoutingPolicy importPolicy = policies.get(importPolicyName);
      // Allow routes permitted by global prefix list
      assertTrue(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("1.1.1.1/26")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));
      // Block others
      assertFalse(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/31")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.IN));

      RoutingPolicy exportPolicy = policies.get(exportPolicyName);
      // Allow routes permitted by global list
      assertTrue(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("2.2.2.2/26")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
      // Block others
      assertFalse(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/30")).build(),
              EigrpInternalRoute.testBuilder(),
              Direction.OUT));
    }
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
                        .setAsn(1L)
                        .setInterfaceName("Ethernet0")
                        .setExportPolicy("~EIGRP_EXPORT_POLICY_default_1_Ethernet0~")
                        .setPassive(false)
                        .setHostname("ios-eigrp-classic")
                        .setVrfName("default")
                        .setIp(Ip.parse("10.0.0.1"))
                        .build(),
                "Ethernet1",
                    EigrpNeighborConfig.builder()
                        .setAsn(1L)
                        .setInterfaceName("Ethernet1")
                        .setExportPolicy("~EIGRP_EXPORT_POLICY_default_1_Ethernet1~")
                        .setPassive(false)
                        .setHostname("ios-eigrp-classic")
                        .setVrfName("default")
                        .setIp(Ip.parse("10.0.1.1"))
                        .build(),
                "Ethernet2",
                    EigrpNeighborConfig.builder()
                        .setAsn(1L)
                        .setInterfaceName("Ethernet2")
                        .setExportPolicy("~EIGRP_EXPORT_POLICY_default_1_Ethernet2~")
                        .setPassive(true)
                        .setHostname("ios-eigrp-classic")
                        .setVrfName("default")
                        .setIp(Ip.parse("10.0.2.1"))
                        .build())));
  }

  @Test
  public void testIosEigrpStub() throws IOException {
    String hostname = "ios-eigrp-stub";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    String filename = "configs/" + hostname;

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "RT_MAP", 3));

    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(
        pvcae, hasParseWarning(filename, containsString("EIGRP stub is not currently supported")));
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

    OspfIntraAreaRoute.Builder builder =
        OspfIntraAreaRoute.builder().setNextHop(NextHopDiscard.instance());
    assertFalse(
        routingPolicy0.process(
            builder.setNetwork(Prefix.parse("1.1.1.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
            Direction.IN));
    assertFalse(
        routingPolicy0.process(
            builder.setNetwork(Prefix.parse("2.2.2.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
            Direction.IN));
    assertTrue(
        routingPolicy0.process(
            builder.setNetwork(Prefix.parse("3.3.3.0/24")).setArea(1L).build(),
            OspfIntraAreaRoute.builder(),
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
            OspfIntraAreaRoute.builder()
                .setNetwork(Prefix.parse("1.1.1.0/24"))
                .setNextHop(NextHopDiscard.instance())
                .setNextHop(NextHopDiscard.instance())
                .setArea(1L)
                .build(),
            OspfIntraAreaRoute.builder(),
            Direction.IN));
    assertTrue(
        routingPolicy.process(
            OspfIntraAreaRoute.builder()
                .setNetwork(Prefix.parse("2.2.2.0/24"))
                .setNextHop(NextHopDiscard.instance())
                .setArea(1L)
                .build(),
            OspfIntraAreaRoute.builder(),
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
            OspfIntraAreaRoute.builder()
                .setNetwork(Prefix.parse("1.1.1.0/24"))
                .setNextHop(NextHopDiscard.instance())
                .setArea(1L)
                .build(),
            OspfIntraAreaRoute.builder(),
            Direction.IN));
    assertFalse(
        routingPolicy.process(
            OspfIntraAreaRoute.builder()
                .setNetwork(Prefix.parse("2.2.2.0/24"))
                .setNextHop(NextHopDiscard.instance())
                .setArea(1L)
                .build(),
            OspfIntraAreaRoute.builder(),
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
  public void testIosOrphanInterfaceOspfSettings() throws IOException {
    Configuration c = parseConfig("ios-orphan-interface-ospf-settings");

    // Confirm interface associated with an OSPF area has expected OSPF properties
    assertThat(
        c,
        hasInterface(
            "Ethernet0/0",
            hasOspfNetworkType(
                equalTo(org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT))));

    // Confirm interface NOT associated with an OSPF area still has expected OSPF properties
    assertThat(
        c,
        hasInterface(
            "Ethernet0/1",
            hasOspfNetworkType(equalTo(org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST))));
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
  public void testIosOspfCostLoopback() throws IOException {
    Configuration c = parseConfig("ios-ospf-cost-loopback");

    assertThat(c.getAllInterfaces().get("Loopback61").getOspfSettings(), not(nullValue()));
    assertThat(c.getAllInterfaces().get("Loopback61").getOspfSettings().getCost(), equalTo(1));
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    /* Confirm access list uses are counted correctly */
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    String filename = "configs/" + advertiserName;
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "1", 1));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_EXTENDED, "100", 1));

    /* Ensure both neighbors have 5.6.7.8 but not 1.2.3.4 */
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    String generatedImportPolicyName =
        computeBgpPeerImportPolicyName(DEFAULT_VRF_NAME, peerAddress.toString());
    String generatedExportPolicyName =
        generatedBgpPeerExportPolicyName(DEFAULT_VRF_NAME, peerAddress.toString());
    RoutingPolicy importPolicy = c.getRoutingPolicies().get(generatedImportPolicyName);
    RoutingPolicy exportPolicy = c.getRoutingPolicies().get(generatedExportPolicyName);
    assertThat(importPolicy, notNullValue());
    assertThat(exportPolicy, notNullValue());

    Prefix permittedPrefix = Prefix.parse("10.1.1.0/24");
    Bgpv4Route.Builder r =
        Bgpv4Route.testBuilder()
            .setOriginatorIp(peerAddress)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.IBGP);
    Bgpv4Route permittedRoute = r.setNetwork(permittedPrefix).build();
    Bgpv4Route unmatchedRoute = r.setNetwork(Prefix.parse("10.1.0.0/16")).build();
    assertThat(
        importPolicy.process(permittedRoute, permittedRoute.toBuilder(), Direction.IN),
        equalTo(true));
    assertThat(
        exportPolicy.process(permittedRoute, permittedRoute.toBuilder(), Direction.OUT),
        equalTo(true));
    assertThat(
        importPolicy.process(unmatchedRoute, unmatchedRoute.toBuilder(), Direction.IN),
        equalTo(false));
    assertThat(
        exportPolicy.process(unmatchedRoute, unmatchedRoute.toBuilder(), Direction.OUT),
        equalTo(false));
  }

  @Test
  public void testIosRouteMap() throws IOException {
    String hostname = "ios-route-map";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    /* Confirm route map uses are counted correctly */
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rm_if", 1));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rm_ospf", 4));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rm_bgp", 9));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "rm_unused", 0));

    /* Confirm undefined route-map is detected */
    assertThat(ccae, hasUndefinedReference(filename, ROUTE_MAP, "rm_undef"));
  }

  @Test
  public void testIosRouteMapLocalPreference() throws IOException {
    String hostname = "ios-route-map-set-local-preference";
    Configuration c = parseConfig(hostname);
    RoutingPolicy setWeightPolicy = c.getRoutingPolicies().get("SET_LOCAL_PREFERENCE");
    Bgpv4Route r =
        Bgpv4Route.testBuilder()
            .setWeight(1)
            .setNetwork(Prefix.ZERO)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    Bgpv4Route.Builder transformedRoute = r.toBuilder();

    assertThat(setWeightPolicy.process(r, transformedRoute, Direction.IN), equalTo(true));
    assertThat(transformedRoute.build().getLocalPreference(), equalTo((1L << 32) - 1));
  }

  @Test
  public void testIosRouteMapSetTag() throws IOException {
    String hostname = "ios-route-map-set-tag";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    RoutingPolicy setTagPolicy =
        batfish
            .loadConfigurations(batfish.getSnapshot())
            .get(hostname)
            .getRoutingPolicies()
            .get("SET_TAG");
    Bgpv4Route r =
        Bgpv4Route.testBuilder()
            .setWeight(1)
            .setNetwork(Prefix.ZERO)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    Bgpv4Route.Builder transformedRoute = r.toBuilder();

    assertThat(setTagPolicy.process(r, transformedRoute, Direction.IN), equalTo(true));
    assertThat(transformedRoute.build(), hasTag(20));
  }

  @Test
  public void testIosRouteMapSetWeight() throws IOException {
    // Config contains a route-map SET_WEIGHT with one line, "set weight 20"
    String hostname = "ios-route-map-set-weight";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    RoutingPolicy setWeightPolicy =
        batfish
            .loadConfigurations(batfish.getSnapshot())
            .get(hostname)
            .getRoutingPolicies()
            .get("SET_WEIGHT");
    Bgpv4Route r =
        Bgpv4Route.testBuilder()
            .setWeight(1)
            .setNetwork(Prefix.ZERO)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    Bgpv4Route.Builder transformedRoute = r.toBuilder();

    assertThat(setWeightPolicy.process(r, transformedRoute, Direction.IN), equalTo(true));
    assertThat(transformedRoute.build(), hasWeight(20));
  }

  @Test
  public void testIosSnmpCommunityString() throws IOException {
    String hostname = "ios-snmp-community-string";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(ccae, hasNumReferrers(filename, TRACK, "1", 1));
    assertThat(ccae, hasNumReferrers(filename, TRACK, "2", 0));
    assertThat(ccae, hasUndefinedReference(filename, TRACK, "3"));

    assertThat(ccae, hasNumReferrers(filename, TRACK, "4", 1));
    assertThat(ccae, hasNumReferrers(filename, TRACK, "5", 0));
    assertThat(ccae, hasNumReferrers(filename, TRACK, "6", 0));
    assertThat(ccae, hasNumReferrers(filename, TRACK, "7", 0));
  }

  @Test
  public void testIosTrackList() throws IOException {
    String hostname = "ios-track-list";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(ccae, hasNumReferrers(filename, TRACK, "1", 3));
    assertThat(ccae, hasNumReferrers(filename, TRACK, "2", 3));
  }

  @Test
  public void testIosVrfdAddressFamilyExportMap() throws IOException {
    String hostname = "ios-vrfd-address-family-export-map";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    String filename = "configs/" + hostname;

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "RT_MAP", 2));

    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(
        pvcae,
        hasParseWarning(
            filename, containsString("Export maps for VRFs are not currently supported")));
  }

  @Test
  public void testIosVrfDefinition() {
    CiscoConfiguration vc = parseCiscoConfig("ios-vrf-definition", ConfigurationFormat.CISCO_IOS);
    {
      org.batfish.representation.cisco.Vrf vrf = vc.getVrfs().get("vrf1");
      assertThat(vrf.getRouteDistinguisher(), equalTo(RouteDistinguisher.from(1111, 11L)));
      assertThat(
          vrf.getGenericAddressFamilyConfig().getRouteTargetImport(),
          contains(ExtendedCommunity.target(2222, 22)));
      assertThat(
          vrf.getGenericAddressFamilyConfig().getRouteTargetExport(),
          contains(ExtendedCommunity.target(2222, 23)));
      VrfAddressFamily ipv4UnicastAddressFamily = vrf.getIpv4UnicastAddressFamily();
      assertThat(
          ipv4UnicastAddressFamily.getRouteTargetImport(),
          containsInAnyOrder(
              ExtendedCommunity.target(3333, 31), ExtendedCommunity.target(3333, 32)));
      assertThat(
          ipv4UnicastAddressFamily.getRouteTargetExport(),
          containsInAnyOrder(
              ExtendedCommunity.target(3333, 31), ExtendedCommunity.target(3333, 33)));
      assertThat(ipv4UnicastAddressFamily.getImportMap(), equalTo("RT_MAP"));
    }
    {
      org.batfish.representation.cisco.Vrf vrf = vc.getVrfs().get("vrf2");
      assertThat(vrf.getRouteDistinguisher(), equalTo(RouteDistinguisher.from(2222, 22L)));
      VrfAddressFamily genericAF = vrf.getGenericAddressFamilyConfig();
      ExtendedCommunity rt = ExtendedCommunity.target(2222, 77);
      assertThat(genericAF.getRouteTargetImport(), contains(rt));
      assertThat(genericAF.getRouteTargetExport(), contains(rt));
      VrfAddressFamily ipv4 = vrf.getIpv4UnicastAddressFamily();
      assertThat(ipv4.getRouteTargetImport(), empty());
      assertThat(ipv4.getRouteTargetExport(), empty());
      // Test inheritance
      ipv4.inherit(genericAF);
      assertThat(ipv4.getRouteTargetImport(), contains(rt));
      assertThat(ipv4.getRouteTargetExport(), contains(rt));
    }
  }

  @Test
  public void testIosZoneSecurity() throws IOException {
    String hostname = "ios-zone-security";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
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

    Flow flow = builder().setIngressNode("").build();

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
        builder()
            .setIngressNode(c.getHostname())
            .setIpProtocol(IpProtocol.TCP)
            .setSrcPort(0)
            .setDstPort(0)
            .build();
    Flow flowInspect =
        builder()
            .setIngressNode(c.getHostname())
            .setIpProtocol(IpProtocol.UDP)
            .setSrcPort(0)
            .setDstPort(0)
            .build();
    Flow flowDrop =
        builder()
            .setIngressNode(c.getHostname())
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
  public void testIosXeEigrpToBgpRedistExtraction() throws IOException {
    String hostname = "ios-xe-eigrp-to-bgp";
    String redistRmName = "redist_eigrp";
    CiscoConfiguration vc = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_IOS);
    org.batfish.representation.cisco.BgpProcess bgpProc = vc.getDefaultVrf().getBgpProcess();
    assert bgpProc != null;
    BgpRedistributionPolicy eigrpRedist =
        bgpProc.getRedistributionPolicies().get(RoutingProtocol.EIGRP);
    assert eigrpRedist != null;
    assertThat(eigrpRedist.getRouteMap(), equalTo(redistRmName));

    /* Confirm route-map was referenced from correct context */
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(
        ccae,
        hasReferencedStructure(filename, ROUTE_MAP, redistRmName, BGP_REDISTRIBUTE_EIGRP_MAP));
  }

  @Test
  public void testIosXeEigrpToBgpRedistConversion() throws IOException {
    // BGP redistributes EIGRP with route-map redist_eigrp, which permits 5.5.5.0/24
    Configuration c = parseConfig("ios-xe-eigrp-to-bgp");
    RoutingPolicy bgpExportPolicy =
        c.getRoutingPolicies().get(generatedBgpCommonExportPolicyName(DEFAULT_VRF_NAME));
    int ebgpAdmin = RoutingProtocol.BGP.getDefaultAdministrativeCost(c.getConfigurationFormat());
    int ibgpAdmin = RoutingProtocol.IBGP.getDefaultAdministrativeCost(c.getConfigurationFormat());
    Prefix matchRm = Prefix.parse("5.5.5.0/24");
    Prefix noMatchRm = Prefix.parse("5.5.5.0/30");
    Ip bgpRouterId = Ip.parse("1.1.1.1");
    Ip bgpPeerId = Ip.parse("2.2.2.2");
    Ip nextHopIp = Ip.parse("3.3.3.3"); // not actually in config, just made up
    BgpSessionProperties.Builder spb =
        BgpSessionProperties.builder().setTailAs(1L).setTailIp(bgpPeerId).setHeadIp(nextHopIp);
    BgpSessionProperties ibgpSessionProps =
        spb.setHeadAs(1L).setSessionType(SessionType.IBGP).build();
    BgpSessionProperties ebgpSessionProps =
        spb.setHeadAs(2L).setSessionType(SessionType.EBGP_SINGLEHOP).build();

    // Create eigrp routes to redistribute
    EigrpInternalRoute.Builder internalRb =
        EigrpInternalRoute.testBuilder()
            .setProcessAsn(1L)
            .setEigrpMetric(
                ClassicMetric.builder()
                    .setValues(EigrpMetricValues.builder().setDelay(1).setBandwidth(1).build())
                    .build())
            .setEigrpMetricVersion(EigrpMetricVersion.V1);
    EigrpRoute matchEigrp = internalRb.setNetwork(matchRm).build();
    EigrpRoute noMatchEigrp = internalRb.setNetwork(noMatchRm).build();

    {
      // Redistribute matching EIGRP route into EBGP
      Bgpv4Route.Builder rb =
          BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
              matchEigrp, bgpRouterId, nextHopIp, ebgpAdmin, RoutingProtocol.BGP);
      assertTrue(bgpExportPolicy.processBgpRoute(matchEigrp, rb, ebgpSessionProps, Direction.OUT));
      assertThat(
          rb.build(),
          equalTo(
              Bgpv4Route.testBuilder()
                  .setNetwork(matchRm)
                  .setProtocol(RoutingProtocol.BGP)
                  .setAdmin(ebgpAdmin)
                  .setLocalPreference(DEFAULT_LOCAL_PREFERENCE)
                  .setMetric(matchEigrp.getMetric())
                  .setNextHopIp(nextHopIp)
                  .setReceivedFromIp(Ip.ZERO)
                  .setOriginatorIp(bgpRouterId)
                  .setOriginType(OriginType.INCOMPLETE)
                  .setSrcProtocol(RoutingProtocol.EIGRP)
                  .build()));
    }
    {
      // Redistribute nonmatching EIGRP route to EBGP
      Bgpv4Route.Builder rb =
          BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
              noMatchEigrp, bgpRouterId, nextHopIp, ebgpAdmin, RoutingProtocol.BGP);
      assertFalse(
          bgpExportPolicy.processBgpRoute(noMatchEigrp, rb, ebgpSessionProps, Direction.OUT));
    }
    {
      // Redistribute matching EIGRP route to IBGP
      Bgpv4Route.Builder rb =
          BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
              matchEigrp, bgpRouterId, nextHopIp, ibgpAdmin, RoutingProtocol.IBGP);
      assertTrue(bgpExportPolicy.processBgpRoute(matchEigrp, rb, ibgpSessionProps, Direction.OUT));
      assertThat(
          rb.build(),
          equalTo(
              Bgpv4Route.testBuilder()
                  .setNetwork(matchRm)
                  .setProtocol(RoutingProtocol.IBGP)
                  .setAdmin(ibgpAdmin)
                  .setLocalPreference(DEFAULT_LOCAL_PREFERENCE)
                  .setMetric(matchEigrp.getMetric())
                  .setNextHopIp(nextHopIp)
                  .setReceivedFromIp(Ip.ZERO)
                  .setOriginatorIp(bgpRouterId)
                  .setOriginType(OriginType.INCOMPLETE)
                  .setSrcProtocol(RoutingProtocol.EIGRP)
                  .build()));
    }
    {
      // Redistribute nonmatching EIGRP route to IBGP
      Bgpv4Route.Builder rb =
          BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
              noMatchEigrp, bgpRouterId, nextHopIp, ibgpAdmin, RoutingProtocol.IBGP);
      assertFalse(
          bgpExportPolicy.processBgpRoute(noMatchEigrp, rb, ibgpSessionProps, Direction.OUT));
    }
    {
      // Ensure external EIGRP route can also match routing policy
      EigrpRoute matchEigrpEx =
          EigrpExternalRoute.testBuilder()
              .setProcessAsn(1L)
              .setDestinationAsn(2L)
              .setEigrpMetric(
                  ClassicMetric.builder()
                      .setValues(EigrpMetricValues.builder().setDelay(1).setBandwidth(1).build())
                      .build())
              .setEigrpMetricVersion(EigrpMetricVersion.V1)
              .setNetwork(matchRm)
              .build();
      Bgpv4Route.Builder rb =
          BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
              matchEigrpEx, bgpRouterId, nextHopIp, ebgpAdmin, RoutingProtocol.BGP);
      assertTrue(
          bgpExportPolicy.processBgpRoute(matchEigrpEx, rb, ebgpSessionProps, Direction.OUT));
      assertThat(
          rb.build(),
          equalTo(
              Bgpv4Route.testBuilder()
                  .setNetwork(matchRm)
                  .setProtocol(RoutingProtocol.BGP)
                  .setAdmin(ebgpAdmin)
                  .setLocalPreference(DEFAULT_LOCAL_PREFERENCE)
                  .setMetric(matchEigrpEx.getMetric())
                  .setNextHopIp(nextHopIp)
                  .setReceivedFromIp(Ip.ZERO)
                  .setOriginatorIp(bgpRouterId)
                  .setOriginType(OriginType.INCOMPLETE)
                  .setSrcProtocol(RoutingProtocol.EIGRP_EX)
                  .build()));
    }
  }

  @Test
  public void testIosXeZoneDefaultBehavior() throws IOException {
    Configuration c = parseConfig("ios-xe-zone-default-behavior");

    /* Ethernet1 and Ethernet2 are in zone z12 */
    String e1Name = "Ethernet1";
    String e2Name = "Ethernet2";

    /* Ethernet3 is in zone z3 */
    String e3Name = "Ethernet3";

    Flow flow = builder().setIngressNode(c.getHostname()).build();

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
  public void testBgpLocalAs() throws IOException {
    String testrigName = "bgp-local-as";
    List<String> configurationNames = ImmutableList.of("r1", "r2");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    Map<Ip, Map<String, Set<String>>> ipOwners =
        batfish.getTopologyProvider().getIpOwners(batfish.getSnapshot()).getIpVrfOwners();
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
    List<String> configurationNames = ImmutableList.of("arista_disabled", "arista_enabled");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
    org.batfish.datamodel.BgpProcess aristaDisabled =
        configurations.get("arista_disabled").getDefaultVrf().getBgpProcess();
    org.batfish.datamodel.BgpProcess aristaEnabled =
        configurations.get("arista_enabled").getDefaultVrf().getBgpProcess();

    assertThat(
        aristaDisabled,
        hasMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.EXACT_PATH));
    assertThat(
        aristaEnabled,
        hasMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));

    assertThat(aristaDisabled, hasMultipathEbgp(false));
    assertThat(aristaEnabled, hasMultipathEbgp(false));
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
  public void testBgpMultipleRouters() throws IOException {
    parseConfig("ios-bgp-multiple-routers");
    // Don't crash.
  }

  /** Tests that we can append more BGP config at the bottom of a file. */
  @Test
  public void testBgpReentrantVrf() {
    CiscoConfiguration c = parseCiscoConfig("ios-bgp-reentrant-vrf", ConfigurationFormat.CISCO_IOS);
    // Simple test that default VRF was parsed
    org.batfish.representation.cisco.BgpProcess defBgp = c.getDefaultVrf().getBgpProcess();
    assertThat(defBgp.getProcnum(), equalTo(1L));

    // VRF keeps local-as from first declaration and overriden router-id from second.
    assertThat(c.getVrfs(), hasKey("a"));
    org.batfish.representation.cisco.BgpProcess vrfBgp = c.getVrfs().get("a").getBgpProcess();
    assertThat(vrfBgp.getMasterBgpPeerGroup().getLocalAs(), equalTo(5L));
    assertThat(vrfBgp.getRouterId(), equalTo(Ip.parse("1.2.3.5")));
  }

  @Test
  public void testBgpUndeclaredPeer() throws IOException {
    parseConfig("ios-bgp-undeclared-peer");
    // Don't crash.
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    // Check that 1.1.1.1/32 appears on r3
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs =
        dp.getRibs();
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
    String eosName = "eos";
    List<String> configurationNames = ImmutableList.of(iosName, eosName);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

    Configuration iosCommunityListConfig = configurations.get(iosName);
    Map<String, CommunityList> iosCommunityLists = iosCommunityListConfig.getCommunityLists();

    Configuration eosCommunityListConfig = configurations.get(eosName);
    Map<String, CommunityList> eosCommunityLists = eosCommunityListConfig.getCommunityLists();

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

    // check literal communities
    assertThat(iosImpliedStd, equalTo(StandardCommunity.of(4294967295L)));
    assertThat(iosStdAsnn, equalTo(StandardCommunity.parse("65535:65535")));
    assertThat(eosStd, equalTo(StandardCommunity.parse("0:1")));

    // check regex communities
    assertThat(iosRegexImpliedExp, equalTo("4294967295"));
    assertThat(iosRegexExpAsnn, equalTo("65535:65535"));
    assertThat(eosRegexExp, equalTo("1"));
    /*
     *  TODO: https://github.com/batfish/batfish/issues/1993
     *  (Should be three regexes: '0:1', '0:2, '0:3')
     */
    assertThat(eosRegexExpMulti, equalTo("0:10:20:3"));

    // Check well known community regexes are generated properly
    assertThat(iosStdInternet, equalTo(StandardCommunity.INTERNET));
    assertThat(iosStdNoAdv, equalTo(StandardCommunity.NO_ADVERTISE));
    assertThat(iosStdNoExport, equalTo(StandardCommunity.NO_EXPORT));
    assertThat(iosStdGshut, equalTo(StandardCommunity.GRACEFUL_SHUTDOWN));
    assertThat(iosStdLocalAs, equalTo(StandardCommunity.NO_EXPORT_SUBCONFED));
    assertThat(eosStdInternet, equalTo(StandardCommunity.INTERNET));
    assertThat(eosStdNoAdv, equalTo(StandardCommunity.NO_ADVERTISE));
    assertThat(eosStdNoExport, equalTo(StandardCommunity.NO_EXPORT));
    assertThat(eosStdGshut, equalTo(StandardCommunity.GRACEFUL_SHUTDOWN));
    assertThat(eosStdLocalAs, equalTo(StandardCommunity.NO_EXPORT_SUBCONFED));

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

    List<ExprAclLine> expectedAclLines =
        ImmutableList.of(
            ExprAclLine.accepting()
                .setName("permit ip 1.1.1.1 0.0.0.0 2.2.2.2 0.0.0.0")
                .setMatchCondition(
                    new MatchHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(IpWildcard.parse("1.1.1.1").toIpSpace())
                            .setDstIps(IpWildcard.parse("2.2.2.2").toIpSpace())
                            .build()))
                .build(),
            ExprAclLine.accepting()
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

    assertThat(
        c.getInterfaces().get("Tunnel1").getTunnel().getMode(), equalTo(TunnelMode.GRE_MULTIPOINT));
    assertThat(
        c.getInterfaces().get("Tunnel2").getTunnel().getMode(), equalTo(TunnelMode.GRE_MULTIPOINT));
    assertThat(
        c.getInterfaces().get("Tunnel3").getTunnel().getMode(), equalTo(TunnelMode.IPSEC_IPV4));
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                "Interface TenGigabitEthernet0/1 with declared crypto-map mymap has no"
                    + " ip-address")));
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot()),
        hasNumReferrers("configs/" + hostname, NAMED_RSA_PUB_KEY, "testrsa", 1));
  }

  private static org.batfish.datamodel.routing_policy.expr.CommunitySetExpr
      communityListToMatchCondition(
          Map<String, CommunityList> communityLists, String communityName) {
    return communityLists.get(communityName).getLines().get(0).getMatchCondition();
  }

  private static Community communityListToCommunity(
      Map<String, CommunityList> communityLists, String communityName) {
    return Iterables.getOnlyElement(
        communityLists
            .get(communityName)
            .getLines()
            .get(0)
            .getMatchCondition()
            .asLiteralCommunities(null));
  }

  private static @Nonnull String communityListToRegex(
      Map<String, CommunityList> communityLists, String communityName) {
    return ((RegexCommunitySet)
            communityLists.get(communityName).getLines().get(0).getMatchCondition())
        .getRegex();
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

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
  public void testOspfProcessInference() throws IOException {
    Configuration c = parseConfig("ios-ospf-process-inference");
    String iface1Name = "Ethernet1";
    String iface2Name = "Ethernet2";
    String iface3Name = "Ethernet3";
    String iface4Name = "Ethernet4";
    Map<String, Interface> ifaces = c.getAllInterfaces();
    assertThat(ifaces.keySet(), containsInAnyOrder(iface1Name, iface2Name, iface3Name, iface4Name));

    // Confirm the correct OSPF process was inferred for each interface
    assertThat(ifaces.get(iface1Name).getOspfProcess(), equalTo("1"));
    assertThat(ifaces.get(iface2Name).getOspfProcess(), equalTo("2"));
    assertThat(ifaces.get(iface3Name).getOspfProcess(), equalTo("2"));
    // Should not infer an OSPF process for the interface not overlapping with an OSPF network
    assertThat(ifaces.get(iface4Name).getOspfProcess(), nullValue());
  }

  @Test
  public void testIosOspfPrefixPriority() throws IOException {
    String hostname = "ios-ospf-prefix-priority";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(ccae, hasNumReferrers("configs/" + hostname, ROUTE_MAP, "OSPF-MAP", 1));
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    NetworkSnapshot snapshot = batfish.getSnapshot();
    Map<String, Configuration> configurations = batfish.loadConfigurations(snapshot);
    Configuration abr = configurations.get(abrName);

    // Sanity check: ensure the ABR does not have suppressType7 set for area 1
    Long areaNum = abr.getAllInterfaces().get("Ethernet1").getOspfAreaName();
    OspfArea abrToArea1 =
        abr.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getAreas().get(areaNum);
    assertThat(abrToArea1.getNssa(), hasSuppressType7(false));

    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    snapshot = batfish.getSnapshot();
    configurations = batfish.loadConfigurations(snapshot);
    abr = configurations.get(abrName);

    // This time the ABR should have suppressType7 set for area 1
    areaNum = abr.getAllInterfaces().get("Ethernet1").getOspfAreaName();
    abrToArea1 =
        abr.getVrfs().get(DEFAULT_VRF_NAME).getOspfProcesses().get("1").getAreas().get(areaNum);
    assertThat(abrToArea1.getNssa(), hasSuppressType7(true));

    batfish.computeDataPlane(snapshot);
    dp = batfish.loadDataPlane(snapshot);
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
  public void testOspfNetworkTypes() throws IOException {
    String testrigName = "ospf-network-types";
    String iosOspfPointToPoint = "ios-ospf-network-types";
    List<String> configurationNames = ImmutableList.of(iosOspfPointToPoint);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

    String eth0 = "Ethernet0/0";
    String eth1 = "Ethernet0/1";
    String eth2 = "Ethernet0/2";
    String eth3 = "Ethernet0/3";
    String eth4 = "Ethernet0/4";
    Configuration config = configurations.get(iosOspfPointToPoint);
    Map<String, Interface> ifaces = config.getAllInterfaces();
    assertThat(ifaces.keySet(), containsInAnyOrder(eth0, eth1, eth2, eth3, eth4));

    assertThat(ifaces.get(eth0).getOspfNetworkType(), nullValue());
    assertThat(
        ifaces.get(eth1).getOspfNetworkType(),
        equalTo(org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT));
    assertThat(
        ifaces.get(eth2).getOspfNetworkType(),
        equalTo(org.batfish.datamodel.ospf.OspfNetworkType.BROADCAST));
    assertThat(
        ifaces.get(eth3).getOspfNetworkType(),
        equalTo(org.batfish.datamodel.ospf.OspfNetworkType.NON_BROADCAST_MULTI_ACCESS));
    assertThat(
        ifaces.get(eth4).getOspfNetworkType(),
        equalTo(org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_MULTIPOINT));
  }

  @Test
  public void testCiscoOspfNetworkTypes() {
    CiscoConfiguration config =
        parseCiscoConfig("ospf-network-types", ConfigurationFormat.CISCO_IOS);

    String eth0 = "Ethernet0/0";
    String eth1 = "Ethernet0/1";
    String eth2 = "Ethernet0/2";
    String eth3 = "Ethernet0/3";
    String eth4 = "Ethernet0/4";
    String eth5 = "Ethernet0/5";

    Map<String, org.batfish.representation.cisco.Interface> ifaces = config.getInterfaces();
    assertThat(ifaces.keySet(), containsInAnyOrder(eth0, eth1, eth2, eth3, eth4, eth5));
    // No network set should result in a null network type
    assertThat(ifaces.get(eth0).getOspfNetworkType(), nullValue());
    // Confirm explicitly set network types show up as expected in the VS model
    assertThat(ifaces.get(eth1).getOspfNetworkType(), equalTo(OspfNetworkType.POINT_TO_POINT));
    assertThat(ifaces.get(eth2).getOspfNetworkType(), equalTo(OspfNetworkType.BROADCAST));
    assertThat(ifaces.get(eth3).getOspfNetworkType(), equalTo(OspfNetworkType.NON_BROADCAST));
    assertThat(ifaces.get(eth4).getOspfNetworkType(), equalTo(OspfNetworkType.POINT_TO_MULTIPOINT));
    assertThat(
        ifaces.get(eth5).getOspfNetworkType(),
        equalTo(OspfNetworkType.POINT_TO_MULTIPOINT_NON_BROADCAST));
  }

  @Test
  public void testCiscoOspfIntervals() {
    CiscoConfiguration config = parseCiscoConfig("ospf-intervals", ConfigurationFormat.CISCO_IOS);

    String eth0 = "Ethernet0/0";
    String eth1 = "Ethernet0/1";
    String eth2 = "Ethernet0/2";
    String eth3 = "Ethernet0/3";

    Map<String, org.batfish.representation.cisco.Interface> ifaces = config.getInterfaces();
    assertThat(ifaces, hasKeys(eth0, eth1, eth2, eth3));

    // Confirm explicitly set hello and dead intervals show up in the VS model
    // Also confirm intervals that are not set show up as nulls in the VS model
    assertThat(ifaces.get(eth0).getOspfDeadInterval(), nullValue());
    assertThat(ifaces.get(eth0).getOspfHelloInterval(), nullValue());

    assertThat(ifaces.get(eth1).getOspfDeadInterval(), nullValue());
    assertThat(ifaces.get(eth1).getOspfHelloInterval(), equalTo(11));

    assertThat(ifaces.get(eth2).getOspfDeadInterval(), equalTo(36));
    assertThat(ifaces.get(eth2).getOspfHelloInterval(), equalTo(12));

    assertThat(ifaces.get(eth3).getOspfDeadInterval(), equalTo(42));
    assertThat(ifaces.get(eth3).getOspfHelloInterval(), nullValue());
  }

  @Test
  public void testParsingRecovery() throws IOException {
    String testrigName = "parsing-recovery";
    String hostname = "ios-recovery";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

    Configuration iosRecovery = configurations.get(hostname);
    assertThat(iosRecovery, allOf(Matchers.notNullValue(), hasInterface("Loopback0", anything())));
  }

  @Test
  public void testShutdownOspfInterface() throws IOException {
    Configuration c = parseConfig("ios-shutdown-ospf-interface");

    String eth0 = "Ethernet0/0";
    Map<String, Interface> ifaces = c.getAllInterfaces();
    assertThat(ifaces.keySet(), contains(eth0));
    Interface iface = ifaces.get(eth0);

    // Confirm OSPF settings are associated with interface even though it is shutdown
    assertThat(iface.getOspfEnabled(), equalTo(false));
    assertThat(iface.getOspfAreaName(), equalTo(0L));
    assertThat(iface.getOspfProcess(), equalTo("1"));
    assertThat(iface.getOspfPassive(), equalTo(true));
  }

  @Test
  public void testParsingRecoveryNoInfiniteLoopDuringAdaptivePredictionAtEof() throws IOException {
    String testrigName = "parsing-recovery";
    String hostname = "ios-blankish-file";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

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
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    batfish.getSettings().setThrowOnParserError(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations(batfish.getSnapshot());

    /* Parser should not crash, and configuration with hostname from file should be generated */
    assertThat(configurations, hasKey(hostname));
  }

  private Configuration parseConfig(String hostname) throws IOException {
    Map<String, Configuration> configs = parseTextConfigs(hostname);
    assertThat(configs, hasKey(hostname.toLowerCase()));
    Configuration c = configs.get(hostname.toLowerCase());
    // Ensure that we used the Cisco parser.
    assertThat(c.getVendorFamily().getCisco(), notNullValue());
    return c;
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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
  public void testIosDynamicNatExtraction() throws IOException {
    CiscoConfiguration c = parseCiscoConfig("ios-nat-dynamic", ConfigurationFormat.CISCO_IOS);
    assertThat(c.getCiscoIosNats(), hasSize(6));
    List<CiscoIosNat> nats = c.getCiscoIosNats();
    {
      assertThat(nats.get(0), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(0);
      assertThat(nat.getAclName(), equalTo("10"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_INSIDE));
      assertThat(nat.getInterface(), nullValue());
      assertThat(nat.getNatPool(), equalTo("in-src-nat-pool"));
      assertThat(nat.getOverload(), equalTo(true));
      assertThat(nat.getVrf(), nullValue());
    }
    {
      assertThat(nats.get(1), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(1);
      assertThat(nat.getAclName(), equalTo("13"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_INSIDE));
      assertThat(nat.getInterface(), equalTo("Ethernet10"));
      assertThat(nat.getNatPool(), nullValue());
      assertFalse(nat.getOverload());
      assertThat(nat.getVrf(), nullValue());
    }
    {
      assertThat(nats.get(2), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(2);
      assertThat(nat.getAclName(), equalTo("11"));
      assertThat(nat.getAction(), equalTo(RuleAction.DESTINATION_INSIDE));
      assertThat(nat.getInterface(), nullValue());
      assertThat(nat.getNatPool(), equalTo("in-dst-nat-pool"));
      assertFalse(nat.getOverload());
      assertThat(nat.getVrf(), nullValue());
    }
    {
      assertThat(nats.get(3), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(3);
      assertThat(nat.getAclName(), equalTo("22"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_OUTSIDE));
      assertThat(nat.getInterface(), nullValue());
      assertThat(nat.getNatPool(), equalTo("out-src-nat-pool"));
      assertFalse(nat.getOverload());
      assertThat(nat.getVrf(), nullValue());
    }
    {
      assertThat(nats.get(4), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(4);
      assertThat(nat.getAclName(), equalTo("12"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_INSIDE));
      assertThat(nat.getInterface(), nullValue());
      assertThat(nat.getNatPool(), equalTo("vrf-in-src-nat-pool"));
      assertFalse(nat.getOverload());
      assertThat(nat.getVrf(), equalTo("vrf1"));
    }
    {
      assertThat(nats.get(5), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(5);
      assertThat(nat.getAclName(), equalTo("23"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_OUTSIDE));
      assertThat(nat.getInterface(), nullValue());
      assertThat(nat.getNatPool(), equalTo("vrf-out-src-nat-pool"));
      assertFalse(nat.getOverload());
      assertThat(nat.getVrf(), equalTo("vrf1"));
    }
  }

  @Test
  public void testIosDynamicNatConversion() throws IOException {
    Configuration c = parseConfig("ios-nat-dynamic");
    String insideIntf = "Ethernet1";
    String outsideIntf = "Ethernet2";
    String vrfInsideIntf = "Ethernet3";
    String vrfOutsideIntf = "Ethernet4";
    assertThat(c, hasInterface(insideIntf, notNullValue()));
    assertThat(c, hasInterface(outsideIntf, notNullValue()));
    assertThat(c, hasInterface(vrfInsideIntf, notNullValue()));
    assertThat(c, hasInterface(vrfOutsideIntf, notNullValue()));
    MatchSrcInterface matchSrcInside = matchSrcInterface(insideIntf, vrfInsideIntf);

    {
      // NAT in default VRF
      Ip insideSrcPoolFirst = Ip.parse("3.3.3.1");
      Ip insideSrcPoolLast = Ip.parse("3.3.3.254");
      Ip insideDstPoolFirst = Ip.parse("3.3.4.1");
      Ip insideDstPoolLast = Ip.parse("3.3.4.254");
      Ip outsideSrcPoolFirst = Ip.parse("4.4.4.1");
      Ip outsideSrcPoolLast = Ip.parse("4.4.4.254");
      Ip insideSrcIfaceAddr = Ip.parse("1.1.1.1");
      String insideSrcPoolAcl = "10";
      String insideSrcIfaceAcl = "13";
      String insideDstPoolAcl = computeDynamicDestinationNatAclName("11");
      String outsideSrcPoolAcl = "22";

      Interface inside = c.getAllInterfaces().get(insideIntf);
      assertThat(inside.getIncomingTransformation(), nullValue());
      assertThat(inside.getOutgoingTransformation(), nullValue());

      Interface outside = c.getAllInterfaces().get(outsideIntf);

      Transformation inTransformation =
          when(permittedByAcl(outsideSrcPoolAcl))
              .apply(assignSourceIp(outsideSrcPoolFirst, outsideSrcPoolLast))
              .build();

      assertThat(outside.getIncomingTransformation(), equalTo(inTransformation));

      Transformation destTransformation =
          when(and(permittedByAcl(insideDstPoolAcl), matchSrcInside))
              .apply(assignDestinationIp(insideDstPoolFirst, insideDstPoolLast))
              .build();

      Transformation outTransformation =
          when(and(permittedByAcl(insideSrcPoolAcl), matchSrcInside))
              .apply(assignSourceIp(insideSrcPoolFirst, insideSrcPoolLast))
              .setAndThen(destTransformation)
              .setOrElse(
                  when(and(permittedByAcl(insideSrcIfaceAcl), matchSrcInside))
                      .apply(assignSourceIp(insideSrcIfaceAddr, insideSrcIfaceAddr))
                      .setAndThen(destTransformation)
                      .setOrElse(destTransformation)
                      .build())
              .build();

      assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
    }
    {
      // NAT in default VRF
      Ip insidePoolFirst = Ip.parse("5.5.5.1");
      Ip insidePoolLast = Ip.parse("5.5.5.254");
      Ip outsidePoolFirst = Ip.parse("6.6.6.1");
      Ip outsidePoolLast = Ip.parse("6.6.6.254");
      String insideAclName = "12";
      String outsideAclName = "23";

      Interface inside = c.getAllInterfaces().get(vrfInsideIntf);
      assertThat(inside.getIncomingTransformation(), nullValue());
      assertThat(inside.getOutgoingTransformation(), nullValue());

      Interface outside = c.getAllInterfaces().get(vrfOutsideIntf);

      Transformation inTransformation =
          when(permittedByAcl(outsideAclName))
              .apply(assignSourceIp(outsidePoolFirst, outsidePoolLast))
              .build();

      assertThat(outside.getIncomingTransformation(), equalTo(inTransformation));

      Transformation outTransformation =
          when(and(permittedByAcl(insideAclName), matchSrcInside))
              .apply(assignSourceIp(insidePoolFirst, insidePoolLast))
              .build();

      assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
    }
  }

  @Test
  public void testIosDynamicNatRouteMapsExtraction() throws IOException {
    CiscoConfiguration c =
        parseCiscoConfig("ios-nat-dynamic-route-maps", ConfigurationFormat.CISCO_IOS);
    assertThat(c.getCiscoIosNats(), hasSize(6));
    List<CiscoIosNat> nats = c.getCiscoIosNats();
    {
      assertThat(nats.get(0), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(0);
      assertNull(nat.getAclName());
      assertThat(nat.getRouteMap(), equalTo("10"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_INSIDE));
      assertThat(nat.getInterface(), nullValue());
      assertThat(nat.getNatPool(), equalTo("in-src-nat-pool"));
      assertThat(nat.getOverload(), equalTo(true));
      assertThat(nat.getVrf(), nullValue());
    }
    {
      assertThat(nats.get(1), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(1);
      assertNull(nat.getAclName());
      assertThat(nat.getRouteMap(), equalTo("13"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_INSIDE));
      assertThat(nat.getInterface(), equalTo("Ethernet10"));
      assertThat(nat.getNatPool(), nullValue());
      assertFalse(nat.getOverload());
      assertThat(nat.getVrf(), nullValue());
    }
    // note, the indices skip 2 because there's an inside dest rule that's only present to test how
    // it gets incorporated in the converted transformations
    {
      assertThat(nats.get(3), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(3);
      assertNull(nat.getAclName());
      assertThat(nat.getRouteMap(), equalTo("22"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_OUTSIDE));
      assertThat(nat.getInterface(), nullValue());
      assertThat(nat.getNatPool(), equalTo("out-src-nat-pool"));
      assertFalse(nat.getOverload());
      assertThat(nat.getVrf(), nullValue());
    }
    {
      assertThat(nats.get(4), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(4);
      assertNull(nat.getAclName());
      assertThat(nat.getRouteMap(), equalTo("12"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_INSIDE));
      assertThat(nat.getInterface(), nullValue());
      assertThat(nat.getNatPool(), equalTo("vrf-in-src-nat-pool"));
      assertFalse(nat.getOverload());
      assertThat(nat.getVrf(), equalTo("vrf1"));
    }
    {
      assertThat(nats.get(5), instanceOf(CiscoIosDynamicNat.class));
      CiscoIosDynamicNat nat = (CiscoIosDynamicNat) nats.get(5);
      assertNull(nat.getAclName());
      assertThat(nat.getRouteMap(), equalTo("23"));
      assertThat(nat.getAction(), equalTo(RuleAction.SOURCE_OUTSIDE));
      assertThat(nat.getInterface(), nullValue());
      assertThat(nat.getNatPool(), equalTo("vrf-out-src-nat-pool"));
      assertFalse(nat.getOverload());
      assertThat(nat.getVrf(), equalTo("vrf1"));
    }
  }

  @Test
  public void testIosDynamicNatRouteMapsConversion() throws IOException {
    Configuration c = parseConfig("ios-nat-dynamic-route-maps");
    String insideIntf = "Ethernet1";
    String outsideIntf = "Ethernet2";
    String vrfInsideIntf = "Ethernet3";
    String vrfOutsideIntf = "Ethernet4";
    assertThat(c, hasInterface(insideIntf, notNullValue()));
    assertThat(c, hasInterface(outsideIntf, notNullValue()));
    assertThat(c, hasInterface(vrfInsideIntf, notNullValue()));
    assertThat(c, hasInterface(vrfOutsideIntf, notNullValue()));
    MatchSrcInterface matchSrcInside = matchSrcInterface(insideIntf, vrfInsideIntf);

    {
      // NAT in default VRF
      Ip insideSrcPoolFirst = Ip.parse("3.3.3.1");
      Ip insideSrcPoolLast = Ip.parse("3.3.3.254");
      Ip insideDstPoolFirst = Ip.parse("3.3.4.1");
      Ip insideDstPoolLast = Ip.parse("3.3.4.254");
      Ip outsideSrcPoolFirst = Ip.parse("4.4.4.1");
      Ip outsideSrcPoolLast = Ip.parse("4.4.4.254");
      Ip insideSrcIfaceAddr = Ip.parse("1.1.1.1");
      String insideSrcPoolAcl = "10";
      String insideSrcIfaceAcl = "13";
      String insideDstPoolAcl = computeDynamicDestinationNatAclName("11");
      String outsideSrcPoolAcl = "22";

      Interface inside = c.getAllInterfaces().get(insideIntf);
      assertThat(inside.getIncomingTransformation(), nullValue());
      assertThat(inside.getOutgoingTransformation(), nullValue());

      Interface outside = c.getAllInterfaces().get(outsideIntf);

      Transformation inTransformation =
          when(permittedByAcl(outsideSrcPoolAcl))
              .apply(assignSourceIp(outsideSrcPoolFirst, outsideSrcPoolLast))
              .build();

      assertThat(outside.getIncomingTransformation(), equalTo(inTransformation));

      Transformation destTransformation =
          when(and(permittedByAcl(insideDstPoolAcl), matchSrcInside))
              .apply(assignDestinationIp(insideDstPoolFirst, insideDstPoolLast))
              .build();

      Transformation outTransformation =
          when(and(permittedByAcl(insideSrcPoolAcl), matchSrcInside))
              .apply(assignSourceIp(insideSrcPoolFirst, insideSrcPoolLast))
              .setAndThen(destTransformation)
              .setOrElse(
                  when(and(permittedByAcl(insideSrcIfaceAcl), matchSrcInside))
                      .apply(assignSourceIp(insideSrcIfaceAddr, insideSrcIfaceAddr))
                      .setAndThen(destTransformation)
                      .setOrElse(destTransformation)
                      .build())
              .build();

      assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
    }
    {
      // NAT in vrf1
      Ip insidePoolFirst = Ip.parse("5.5.5.1");
      Ip insidePoolLast = Ip.parse("5.5.5.254");
      Ip outsidePoolFirst = Ip.parse("6.6.6.1");
      Ip outsidePoolLast = Ip.parse("6.6.6.254");
      String insideAclName = "12";
      String outsideAclName = "23";

      Interface inside = c.getAllInterfaces().get(vrfInsideIntf);
      assertThat(inside.getIncomingTransformation(), nullValue());
      assertThat(inside.getOutgoingTransformation(), nullValue());

      Interface outside = c.getAllInterfaces().get(vrfOutsideIntf);

      Transformation inTransformation =
          when(permittedByAcl(outsideAclName))
              .apply(assignSourceIp(outsidePoolFirst, outsidePoolLast))
              .build();

      assertThat(outside.getIncomingTransformation(), equalTo(inTransformation));

      Transformation outTransformation =
          when(and(permittedByAcl(insideAclName), matchSrcInside))
              .apply(assignSourceIp(insidePoolFirst, insidePoolLast))
              .build();

      assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
    }
  }

  @Test
  public void testIosStaticNat() throws IOException {
    Configuration c = parseConfig("ios-nat-static");
    String insideIntf = "Ethernet1";
    String outsideIntf = "Ethernet2";
    String vrfInsideIntf = "Ethernet3";
    String vrfOutsideIntf = "Ethernet4";
    assertThat(c, hasInterface(vrfInsideIntf, notNullValue()));
    assertThat(c, hasInterface(insideIntf, notNullValue()));
    assertThat(c, hasInterface(outsideIntf, notNullValue()));
    assertThat(c, hasInterface(vrfOutsideIntf, notNullValue()));
    MatchSrcInterface matchSrcIfaceInside = matchSrcInterface(insideIntf, vrfInsideIntf);

    {
      // NAT in default VRF
      Prefix nat1Local = Prefix.parse("1.1.1.1/32");
      Prefix nat3Local = Prefix.parse("1.1.3.0/24");
      Prefix nat2Local = Prefix.parse("1.1.2.0/14");
      Prefix nat4Local = Prefix.parse("7.7.7.7/32");
      Prefix nat1Global = Prefix.parse("2.2.2.2/32");
      Prefix nat2Global = Prefix.parse("2.2.2.0/14");
      Prefix nat4Global = Prefix.parse("6.6.6.6/32");
      Prefix nat3Global = Prefix.parse("2.2.3.0/24");
      String nat1Acl = "10"; // ACL referenced by NAT 1's route-map

      Interface inside = c.getAllInterfaces().get(insideIntf);
      assertThat(inside.getIncomingTransformation(), nullValue());
      assertThat(inside.getOutgoingTransformation(), nullValue());

      Interface outside = c.getAllInterfaces().get(outsideIntf);

      Transformation inDestinationTransformation =
          when(and(matchDst(nat1Global), permittedByAcl(nat1Acl)))
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
          when(and(matchDst(nat4Local), matchSrcIfaceInside))
              .apply(shiftDestinationIp(nat4Global))
              .build();

      Transformation outTransformation =
          when(and(matchSrc(nat1Local), permittedByAcl(nat1Acl), matchSrcIfaceInside))
              .apply(shiftSourceIp(nat1Global))
              .setAndThen(outDestinationTransformation)
              .setOrElse(
                  when(and(matchSrc(nat3Local), matchSrcIfaceInside))
                      .apply(shiftSourceIp(nat3Global))
                      .setAndThen(outDestinationTransformation)
                      .setOrElse(
                          when(and(matchSrc(nat2Local), matchSrcIfaceInside))
                              .apply(shiftSourceIp(nat2Global))
                              .setAndThen(outDestinationTransformation)
                              .setOrElse(outDestinationTransformation)
                              .build())
                      .build())
              .build();

      assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
    }
    {
      // NAT on vrf1 interfaces
      Prefix insideLocal = Prefix.parse("3.3.3.3/32");
      Prefix insideGlobal = Prefix.parse("4.4.4.4/32");
      Prefix outsideLocal = Prefix.parse("9.9.9.9/32");
      Prefix outsideGlobal = Prefix.parse("8.8.8.8/32");

      Interface inside = c.getAllInterfaces().get(vrfInsideIntf);
      assertThat(inside.getIncomingTransformation(), nullValue());
      assertThat(inside.getOutgoingTransformation(), nullValue());

      Interface outside = c.getAllInterfaces().get(vrfOutsideIntf);
      assertThat(outside.getIncomingTransformation(), notNullValue());
      assertThat(outside.getOutgoingTransformation(), notNullValue());

      Transformation inDestinationTransformation =
          when(matchDst(insideGlobal)).apply(shiftDestinationIp(insideLocal)).build();

      Transformation inTransformation =
          when(matchSrc(outsideGlobal))
              .apply(shiftSourceIp(outsideLocal))
              .setAndThen(inDestinationTransformation)
              .setOrElse(inDestinationTransformation)
              .build();

      assertThat(outside.getIncomingTransformation(), equalTo(inTransformation));

      Transformation outDestinationTransformation =
          when(and(matchDst(outsideLocal), matchSrcIfaceInside))
              .apply(shiftDestinationIp(outsideGlobal))
              .build();

      Transformation outTransformation =
          when(and(matchSrc(insideLocal), matchSrcIfaceInside))
              .apply(shiftSourceIp(insideGlobal))
              .setAndThen(outDestinationTransformation)
              .setOrElse(outDestinationTransformation)
              .build();

      assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
    }
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
        when(and(matchSrc(staticNatLocal), matchIface))
            .apply(shiftSourceIp(staticNatGlobal))
            .setOrElse(
                when(and(permittedByAcl(dynamicNatAcl), matchIface))
                    .apply(assignSourceIp(dynamicNatStart, dynamicNatEnd))
                    .build())
            .build();
    assertThat(outside.getOutgoingTransformation(), equalTo(outTransformation));
  }

  @Test
  public void testIosNatAddRoute() throws IOException {
    String hostname = "ios-nat-add-route";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dp = batfish.loadDataPlane(batfish.getSnapshot());

    // ip nat outside source static 10.10.10.10 1.1.1.1 add-route
    // ip nat outside source static 2.2.2.2 3.3.3.3 add-route
    Prefix global1 = Prefix.parse("10.10.10.10/32");
    Prefix local1 = Prefix.parse("1.1.1.1/32");
    Prefix global2 = Prefix.parse("2.2.2.2/32");
    Prefix local2 = Prefix.parse("3.3.3.3/32");
    StaticRoute.Builder rb =
        StaticRoute.builder().setAdmin(DEFAULT_STATIC_ROUTE_DISTANCE).setTag(-1L);
    StaticRoute rule1Route = rb.setNetwork(local1).setNextHopIp(global1.getStartIp()).build();
    StaticRoute rule2Route = rb.setNetwork(local2).setNextHopIp(global2.getStartIp()).build();
    Set<StaticRoute> defaultVrfStaticRoutes = c.getDefaultVrf().getStaticRoutes();
    assertThat(defaultVrfStaticRoutes, allOf(hasItem(rule1Route), hasItem(rule2Route)));

    // Rule 1's global IP is routable (via iface Ethernet2), but rule 2's global IP isn't.
    // The RIB should therefore have a route corresponding to rule 1, but none for rule 2.
    Set<AbstractRoute> routes =
        dp.getRibs().get(hostname).get(Configuration.DEFAULT_VRF_NAME).getRoutes();
    assertThat(routes, allOf(hasItem(rule1Route), not(hasItem(rule2Route))));

    // Should see that add-route has no effect outside the default VRF.
    // ip nat outside source static 11.11.11.11 4.4.4.4 vrf vrf1 add-route
    Set<StaticRoute> vrf1StaticRoutes = c.getVrfs().get("vrf1").getStaticRoutes();
    assertThat(vrf1StaticRoutes, not(hasItem(hasPrefix(Prefix.parse("4.4.4.4/32")))));
  }

  /** Tests for the syntactic variants we parse and that we link references. */
  @Test
  public void testIosNatParsedVariants() throws IOException {
    String hostname = "ios-nat-parsed-variants";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "ipniss", 4));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "ipnisr", 8));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "GigabitEthernet0/0", 7));
    assertThat(ccae, hasNumReferrers(filename, ROUTE_MAP, "ipnosr", 4));
    assertThat(ccae, hasNumReferrers(filename, NAT_POOL, "p1", 12));
    assertThat(ccae, hasNumReferrers(filename, IPV4_ACCESS_LIST_STANDARD, "10", 6));
  }

  @Test
  public void testIosBgpShowRunAll() throws IOException {
    Configuration c = parseConfig("ios_bgp_show_run_all");
    assertThat(c.getVrfs(), hasKeys("default", "VRF2", "VRF3", "VRF4"));
    assertThat(c.getDefaultVrf().getBgpProcess(), hasRouterId(Ip.parse("1.1.1.1")));
    assertThat(c.getVrfs().get("VRF2").getBgpProcess(), hasRouterId(Ip.parse("1.1.1.2")));
    assertThat(c.getVrfs().get("VRF3").getBgpProcess(), hasRouterId(Ip.parse("1.1.1.3")));
    assertThat(c.getVrfs().get("VRF4").getBgpProcess(), hasRouterId(Ip.parse("1.1.1.4")));
  }

  @Test
  public void testIosInterfaceShowRunAll() throws IOException {
    Configuration c = parseConfig("ios_interface_show_run_all");
    assertThat(
        c.getAllInterfaces(),
        hasKeys(
            "GigabitEthernet0/0/0",
            "Loopback1",
            "Port-channel1",
            "Port-channel1.10",
            "vasileft1",
            "vasiright1"));
    assertThat(
        c, hasInterface("GigabitEthernet0/0/0", hasDescription("GigabitEthernet0/0/0 desc")));
    assertThat(c, hasInterface("Loopback1", hasDescription("Loopback1 desc")));
    assertThat(c, hasInterface("Port-channel1", hasDescription("Port-channel1 desc")));
    assertThat(c, hasInterface("Port-channel1.10", hasDescription("Port-channel1.10 desc")));
    assertThat(c, hasInterface("vasileft1", hasDescription("vasileft1 desc")));
    assertThat(c, hasInterface("vasiright1", hasDescription("vasiright1 desc")));
  }

  @Test
  public void testIosVrfdShowRunAll() {
    CiscoConfiguration c = parseCiscoConfig("ios_vrfd_show_run_all", ConfigurationFormat.CISCO_IOS);
    assertThat(c.getVrfs(), hasKeys("default", "VRF2"));
    assertThat(
        c.getVrfs().get("VRF2").getRouteDistinguisher(),
        equalTo(RouteDistinguisher.parse("65000:1")));
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
  public void testAsaInterface() throws IOException {
    String hostname = "asa-interface";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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

    Flow newFlow = createFlow(IpProtocol.OSPF, 0, 0);

    // Confirm zones are created for each level
    assertThat(c, hasZone(computeASASecurityLevelZoneName(100), hasMemberInterfaces(hasSize(2))));
    assertThat(c, hasZone(computeASASecurityLevelZoneName(45), hasMemberInterfaces(hasSize(1))));
    assertThat(c, hasZone(computeASASecurityLevelZoneName(1), hasMemberInterfaces(hasSize(1))));

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
  }

  @Test
  public void testAsaSecurityLevel_tracing() throws IOException {
    Configuration c = parseConfig("asa-security-level");
    String explicit100Interface = "all-trust";
    String insideInterface = "inside";

    Flow newFlow = createFlow(IpProtocol.OSPF, 0, 0);

    BiFunction<String, String, List<TraceTree>> trace =
        (fromIface, toIface) ->
            AclTracer.trace(
                c.getAllInterfaces().get(toIface).getPreTransformationOutgoingFilter(),
                newFlow,
                fromIface,
                c.getIpAccessLists(),
                c.getIpSpaces(),
                c.getIpSpaceMetadata());

    // from device
    {
      List<TraceTree> traces = trace.apply(null, insideInterface);
      assertThat(traces, contains(isTraceTree(PERMIT_TRAFFIC_FROM_DEVICE)));
    }

    // intra-security-level, but from/to different interfaces
    {
      List<TraceTree> traces = trace.apply(explicit100Interface, insideInterface);
      assertThat(traces, contains(isTraceTree(DENY_SAME_SECURITY_TRAFFIC_INTER_TRACE_ELEMENT)));
    }

    // hairpinning
    {
      List<TraceTree> traces = trace.apply(explicit100Interface, explicit100Interface);
      assertThat(traces, contains(isTraceTree(DENY_SAME_SECURITY_TRAFFIC_INTRA_TRACE_ELEMENT)));
    }
  }

  @Test
  public void testAsaSecurityLevelPermitBoth() throws IOException {
    Configuration c = parseConfig("asa-security-level-permit-both");
    String ifaceAlias1 = "name1";
    String ifaceAlias2 = "name2";
    Flow newFlow = createFlow(IpProtocol.OSPF, 0, 0);

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
  public void testAsaSecurityLevelPermitBoth_tracing() throws IOException {
    Configuration c = parseConfig("asa-security-level-permit-both");
    String ifaceAlias1 = "name1";
    String ifaceAlias2 = "name2";
    Flow newFlow = createFlow(IpProtocol.OSPF, 0, 0);

    BiFunction<String, String, List<TraceTree>> trace =
        (fromIface, toIface) ->
            AclTracer.trace(
                c.getAllInterfaces().get(toIface).getPreTransformationOutgoingFilter(),
                newFlow,
                fromIface,
                c.getIpAccessLists(),
                c.getIpSpaces(),
                c.getIpSpaceMetadata());

    // intra-security-level, but from/to different interfaces
    {
      List<TraceTree> traces = trace.apply(ifaceAlias1, ifaceAlias2);
      assertThat(traces, contains(isTraceTree(PERMIT_SAME_SECURITY_TRAFFIC_INTER_TRACE_ELEMENT)));
    }

    // hairpinning
    {
      List<TraceTree> traces = trace.apply(ifaceAlias1, ifaceAlias1);
      assertThat(traces, contains(isTraceTree(PERMIT_SAME_SECURITY_TRAFFIC_INTRA_TRACE_ELEMENT)));
    }
  }

  /**
   * Test the traces AclTracer produces for ASA security-levels, when the security-level policy
   * permits, and there is an out filter on the out interface.
   */
  @Test
  public void testAsaSecurityLevelPermitTracing() throws IOException {
    Configuration c = parseConfig("asa-security-level-permit-tracing");
    String out = "out"; // security-level 50, has out filter
    String inSameLevel = "inSameLevel"; // security-level 50
    String inLowFiltered = "inLowFiltered"; // security-level 10 with ingress filter
    String inHigh = "inHigh"; // security-level 100

    Flow permitFlow = createFlow(IpProtocol.TCP, 0, 123);
    Flow defaultDenyFlow = createFlow(IpProtocol.TCP, 0, 80);
    Flow explicitDenyFlow = createFlow(IpProtocol.TCP, 0, 22);

    // out is always the egress interface
    IpAccessList filter = c.getAllInterfaces().get(out).getPreTransformationOutgoingFilter();
    BiFunction<String, Flow, List<TraceTree>> trace =
        (inIface, flow) ->
            AclTracer.trace(
                filter,
                flow,
                inIface,
                c.getIpAccessLists(),
                c.getIpSpaces(),
                c.getIpSpaceMetadata());

    IpAccessList filterOut = c.getIpAccessLists().get("FILTER_OUT");

    // permitted, intra-interface
    {
      List<TraceTree> traces = trace.apply(out, permitFlow);
      assertThat(
          traces,
          contains(
              isTraceTree(PERMIT_SAME_SECURITY_TRAFFIC_INTRA_TRACE_ELEMENT),
              isTraceTree(
                  asaPermittedByOutputFilterTraceElement(
                      "configs/asa-security-level-permit-tracing", filterOut),
                  isTraceTree(matchedByAclLine(filterOut, 0)))));
    }

    // default-denied, intra-interface
    {
      List<TraceTree> traces = trace.apply(out, defaultDenyFlow);
      assertThat(
          traces,
          contains(
              isTraceTree(PERMIT_SAME_SECURITY_TRAFFIC_INTRA_TRACE_ELEMENT),
              isTraceTree(
                  asaDeniedByOutputFilterTraceElement(
                      "configs/asa-security-level-permit-tracing", filterOut))));
    }

    // explicitly denied, intra-interface
    {
      List<TraceTree> traces = trace.apply(out, explicitDenyFlow);
      assertThat(
          traces,
          contains(
              isTraceTree(PERMIT_SAME_SECURITY_TRAFFIC_INTRA_TRACE_ELEMENT),
              isTraceTree(
                  asaDeniedByOutputFilterTraceElement(
                      "configs/asa-security-level-permit-tracing", filterOut),
                  isTraceTree(matchedByAclLine("deny tcp any any eq 22")))));
    }

    // permitted, inter-interface
    {
      List<TraceTree> traces = trace.apply(inSameLevel, permitFlow);
      assertThat(
          traces,
          contains(
              isTraceTree(PERMIT_SAME_SECURITY_TRAFFIC_INTER_TRACE_ELEMENT),
              isTraceTree(
                  asaPermittedByOutputFilterTraceElement(
                      "configs/asa-security-level-permit-tracing", filterOut),
                  isTraceTree(matchedByAclLine(filterOut, 0)))));
    }

    // denied, inter-interface
    {
      List<TraceTree> traces = trace.apply(inSameLevel, defaultDenyFlow);
      assertThat(
          traces,
          contains(
              isTraceTree(PERMIT_SAME_SECURITY_TRAFFIC_INTER_TRACE_ELEMENT),
              isTraceTree(
                  asaDeniedByOutputFilterTraceElement(
                      "configs/asa-security-level-permit-tracing", filterOut))));
    }

    // permitted, low-to-high (low has ingress filter)
    {
      List<TraceTree> traces = trace.apply(inLowFiltered, permitFlow);
      assertThat(
          traces,
          contains(
              isTraceTree(asaPermitLowerSecurityLevelTraceElement(10)),
              isTraceTree(
                  asaPermittedByOutputFilterTraceElement(
                      "configs/asa-security-level-permit-tracing", filterOut),
                  isTraceTree(matchedByAclLine(filterOut, 0)))));
    }

    // denied, low-to-high (low has ingress filter)
    {
      List<TraceTree> traces = trace.apply(inLowFiltered, defaultDenyFlow);
      assertThat(
          traces,
          contains(
              isTraceTree(asaPermitLowerSecurityLevelTraceElement(10)),
              isTraceTree(
                  asaDeniedByOutputFilterTraceElement(
                      "configs/asa-security-level-permit-tracing", filterOut))));
    }

    // permitted, high-to-low
    {
      List<TraceTree> traces = trace.apply(inHigh, permitFlow);
      assertThat(
          traces,
          contains(
              isTraceTree(asaPermitHigherSecurityLevelTrafficTraceElement(100)),
              isTraceTree(
                  asaPermittedByOutputFilterTraceElement(
                      "configs/asa-security-level-permit-tracing", filterOut),
                  isTraceTree(matchedByAclLine(filterOut, 0)))));
    }

    // denied, high-to-low
    {
      List<TraceTree> traces = trace.apply(inHigh, defaultDenyFlow);
      assertThat(
          traces,
          contains(
              isTraceTree(asaPermitHigherSecurityLevelTrafficTraceElement(100)),
              isTraceTree(
                  asaDeniedByOutputFilterTraceElement(
                      "configs/asa-security-level-permit-tracing", filterOut))));
    }
  }

  /**
   * Test the traces AclTracer produces for ASA security-levels, when the security-level policy
   * denies
   */
  @Test
  public void testAsaSecurityLevelDenyTracing() throws IOException {
    Configuration c = parseConfig("asa-security-level-deny-tracing");
    String out = "out";
    String inSameLevel = "inSameLevel";
    String inLowUnfiltered = "inLowUnfiltered";

    Flow flow = createFlow(IpProtocol.TCP, 0, 123);

    // out is always the egress interface
    IpAccessList filter = c.getAllInterfaces().get(out).getPreTransformationOutgoingFilter();
    Function<String, List<TraceTree>> trace =
        (inIface) ->
            AclTracer.trace(
                filter,
                flow,
                inIface,
                c.getIpAccessLists(),
                c.getIpSpaces(),
                c.getIpSpaceMetadata());

    // same security level, intra-interface (hairpinning)
    {
      List<TraceTree> traces = trace.apply(out);
      assertThat(traces, contains(isTraceTree(DENY_SAME_SECURITY_TRAFFIC_INTRA_TRACE_ELEMENT)));
    }

    // same security level, inter-interface
    {
      List<TraceTree> traces = trace.apply(inSameLevel);
      assertThat(traces, contains(isTraceTree(DENY_SAME_SECURITY_TRAFFIC_INTER_TRACE_ELEMENT)));
    }

    // lower security level, no ingress filter
    {
      List<TraceTree> traces = trace.apply(inLowUnfiltered);
      assertThat(traces, contains(isTraceTree(asaRejectLowerSecurityLevelTraceElement(10))));
    }
  }

  @Test
  public void testAsaSecurityLevelPermitInter() throws IOException {
    Configuration c = parseConfig("asa-security-level-permit-inter");
    String ifaceAlias1 = "name1";
    String ifaceAlias2 = "name2";
    Flow newFlow = createFlow(IpProtocol.OSPF, 0, 0);

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
    Flow newFlow = createFlow(IpProtocol.OSPF, 0, 0);

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
        hasVrf(
            "default",
            hasStaticRoutes(
                equalTo(
                    ImmutableSet.of(
                        StaticRoute.testBuilder()
                            .setNextHopIp(Ip.parse("3.0.0.1"))
                            .setNetwork(Prefix.parse("0.0.0.0/0"))
                            .setNextHopInterface("ifname")
                            .setAdministrativeCost(2)
                            .build(),
                        StaticRoute.testBuilder()
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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

    AclLineMatchExpr matchSourceSubnet =
        matchSrc(new IpSpaceReference("source-subnet", "Match network object: 'source-subnet'"));
    AclLineMatchExpr matchSourceGroup =
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
            when(and(matchSourceSubnet, matchSrcInterface("inside")))
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
                    and(matchSourceGroup, matchSrcInterface("inside")),
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
            when(and(matchSrc(realSourceHost), matchSrcInterface("inside")))
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
            when(and(matchSrc(Prefix.ZERO), matchSrcInterface("outside")))
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
            when(and(matchSrc(realSourceSubnet), matchSrcInterface("inside")))
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
            when(and(matchSrc(realSourceSubnet), matchDst(mappedDestHost)))
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
            when(and(matchDst(mappedSourceSubnet), matchSrc(realDestHost)))
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
                    and(matchSrc(realSource), matchSrcInterface("inside")),
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
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

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

  @Test
  public void testPortchannelSubinterfaceIsUp() throws IOException {
    Configuration config = parseConfig("ios-portchannel-subinterface");
    double eth1Bandwidth = 1E7;
    assertThat(config, hasInterface("Ethernet1", hasBandwidth(eth1Bandwidth)));
    assertThat(config, hasInterface("Port-channel1", hasBandwidth(eth1Bandwidth)));
    assertThat(
        config,
        hasInterface(
            "Port-channel1.1",
            allOf(
                hasInterfaceType(InterfaceType.AGGREGATE_CHILD),
                isActive(true),
                hasBandwidth(eth1Bandwidth))));
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
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(
                    TESTRIGS_PREFIX + "ios-tunnels",
                    ImmutableList.of("n1-no-static-route", "n2-no-static-route"))
                .build(),
            _folder);

    Edge overlayEdge = Edge.of("n1-no-static-route", "Tunnel1", "n2-no-static-route", "Tunnel1");

    // Overlay edge present in initial tunnel topology
    NetworkSnapshot snapshot = batfish.getSnapshot();
    assertThat(
        batfish.getTopologyProvider().getInitialTunnelTopology(snapshot).asEdgeSet(),
        containsInAnyOrder(overlayEdge, overlayEdge.reverse()));

    // NO overlay edge in final L3 topology
    batfish.computeDataPlane(snapshot);
    assertThat(batfish.getTopologyProvider().getLayer3Topology(snapshot).getEdges(), empty());
  }

  @Test
  public void testTunnelTopologyWithReachability() throws IOException {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(
                    TESTRIGS_PREFIX + "ios-tunnels",
                    ImmutableList.of("n1-static-route", "n2-static-route"))
                .build(),
            _folder);

    Edge underlayEdge =
        Edge.of(
            "n1-static-route", "TenGigabitEthernet0/1", "n2-static-route", "TenGigabitEthernet0/1");
    Edge overlayEdge = Edge.of("n1-static-route", "Tunnel1", "n2-static-route", "Tunnel1");

    // Overlay edge present in initial tunnel topology
    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    assertThat(
        batfish.getTopologyProvider().getInitialTunnelTopology(snapshot).asEdgeSet(),
        containsInAnyOrder(overlayEdge, overlayEdge.reverse()));

    // overlay edge in final L3 topology as well
    assertThat(
        batfish.getTopologyProvider().getLayer3Topology(snapshot).getEdges(),
        containsInAnyOrder(
            overlayEdge, overlayEdge.reverse(), underlayEdge, underlayEdge.reverse()));
  }

  @Test
  public void testRenterBgpStanza() throws IOException {
    Configuration c = parseConfig("ios-bgp-reenter-process");
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(hasActiveNeighbor(Prefix.parse("2.2.2.3/32"), hasRemoteAs(3L)))));
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(hasActiveNeighbor(Prefix.parse("2.2.2.4/32"), hasRemoteAs(4L)))));
  }

  @Test
  public void testRouteMapMatchAcl() throws IOException {
    Configuration c = parseConfig("ios-route-map-match-acl");
    Bgpv4Route.Builder builder =
        Bgpv4Route.testBuilder()
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP);

    Prefix prefix10 = Prefix.parse("10.0.0.0/8");
    Prefix prefix11 = Prefix.parse("11.0.0.0/8");
    Bgpv4Route route10 = builder.setNetwork(prefix10).build();
    Bgpv4Route route11 = builder.setNetwork(prefix11).build();

    assertThat(
        c,
        hasRouteFilterList(
            "ACL_PERMIT", allOf(permits(prefix10), RouteFilterListMatchers.rejects(prefix11))));
    assertThat(
        c,
        hasRouteFilterList(
            "ACL_DENY", allOf(permits(prefix11), RouteFilterListMatchers.rejects(prefix10))));

    assertTrue(
        "Route 10/8 permitted",
        c.getRoutingPolicies()
            .get("rm_standard_permit_permit")
            .process(route10, Bgpv4Route.testBuilder(), Direction.OUT));
    assertFalse(
        "Route 11/8 denied",
        c.getRoutingPolicies()
            .get("rm_standard_permit_permit")
            .process(route11, Bgpv4Route.testBuilder(), Direction.OUT));

    assertFalse(
        "Route 10/8 denied",
        c.getRoutingPolicies()
            .get("rm_standard_deny_permit")
            .process(route10, Bgpv4Route.testBuilder(), Direction.OUT));
    assertFalse(
        "Route 11/8 denied",
        c.getRoutingPolicies()
            .get("rm_standard_permit_permit")
            .process(route11, Bgpv4Route.testBuilder(), Direction.OUT));

    assertFalse(
        "Route 10/8 denied",
        c.getRoutingPolicies()
            .get("rm_standard_permit_deny")
            .process(route10, Bgpv4Route.testBuilder(), Direction.OUT));
    assertFalse(
        "Route 11/8 denied",
        c.getRoutingPolicies()
            .get("rm_standard_permit_permit")
            .process(route11, Bgpv4Route.testBuilder(), Direction.OUT));

    assertFalse(
        "Route 10/8 denied",
        c.getRoutingPolicies()
            .get("rm_standard_deny_deny")
            .process(route10, Bgpv4Route.testBuilder(), Direction.OUT));
    assertFalse(
        "Route 11/8 denied",
        c.getRoutingPolicies()
            .get("rm_standard_permit_permit")
            .process(route11, Bgpv4Route.testBuilder(), Direction.OUT));
  }

  @Test
  public void testRouteMapMatchInterface() throws IOException {
    String hostname = "ios-route-map-match-interface";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    // Ensure warning was filed regarding lack of match interface support
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                "Route-map match interface is not fully supported. Batfish will ignore clauses"
                    + " using match interface for route filtering.")));

    // Ensure the converted route-map ignores the match-interface clause
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    RoutingPolicy rm = c.getRoutingPolicies().get("rm");
    assertThat(
        rm.getStatements(), contains(Statements.ReturnLocalDefaultAction.toStaticStatement()));
  }

  @Test
  public void testSetMetricEigrp() throws IOException {
    Configuration c = parseConfig("ios-route-map-set-metric-eigrp");

    assertThat(
        c.getRoutingPolicies().get("rm_set_metric").getStatements(),
        // Being intentionally lax here because pretty sure conversion is busted.
        // TODO: update when convinced eigrp settings have correct values
        hasItem(instanceOf(SetEigrpMetric.class)));
  }

  @Test
  public void testEigrpOverTunnels() throws IOException {
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(
                    TESTRIGS_PREFIX + "ios-tunnels-eigrp",
                    ImmutableList.of("advertiser", "receiver"))
                .build(),
            _folder);

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    Set<AbstractRoute> routes = dp.getRibs().get("receiver").get(DEFAULT_VRF_NAME).getRoutes();
    assertThat(routes, hasItem(hasPrefix(Prefix.parse("99.99.99.99/32"))));
  }

  @Test
  public void testReenterInterfaceStanza() throws IOException {
    Configuration c = parseConfig("ios-interface-reenter");
    assertThat(c, hasInterface("FastEthernet0/0", hasMtu(2000)));
    assertThat(c, hasInterface("FastEthernet0/0", hasAddress("1.1.1.1/31")));
    assertThat(c, hasInterface("Ethernet0", hasMtu(3000)));
    assertThat(c, hasInterface("Ethernet0", hasAddress("1.1.1.2/31")));
  }

  @Test
  public void testInterfaceUnshut() throws IOException {
    Configuration c = parseConfig("ios-interface-unshut");
    assertThat(c, hasInterface("Ethernet0", isActive(true)));
    assertThat(c, hasInterface("Ethernet1", isActive(false)));
  }

  @Test
  public void testIosEigrpAclUsedForRouting() throws IOException {
    Configuration c = parseConfig("ios-eigrp-match-acl");
    assertThat(c, hasRouteFilterList("ACL", anything()));
    assertTrue(
        c.getRoutingPolicies()
            .get("REDISTRIBUTE_MAP")
            .process(
                new ConnectedRoute(Prefix.ZERO, "dummy", 0),
                EigrpExternalRoute.testBuilder(),
                org.batfish.datamodel.eigrp.EigrpProcess.builder()
                    .setAsNumber(1)
                    .setMode(EigrpProcessMode.CLASSIC)
                    .setMetricVersion(EigrpMetricVersion.V1)
                    .setRouterId(Ip.ZERO)
                    .build(),
                Direction.OUT));
  }

  @Test
  public void testAsaSession() throws IOException {
    Configuration c = parseConfig("asa-session");
    assertThat(c.getAllInterfaces().keySet(), containsInAnyOrder("inside", "outside"));
    FirewallSessionInterfaceInfo inside =
        c.getAllInterfaces().get("inside").getFirewallSessionInterfaceInfo();
    FirewallSessionInterfaceInfo outside =
        c.getAllInterfaces().get("outside").getFirewallSessionInterfaceInfo();
    // Confirm that each interface has the correct, interface-name-specific firewall session info
    // attached to it
    assertThat(
        inside,
        equalTo(new FirewallSessionInterfaceInfo(true, ImmutableSet.of("inside"), null, null)));
    assertThat(
        outside,
        equalTo(new FirewallSessionInterfaceInfo(true, ImmutableSet.of("outside"), null, null)));
  }

  @Test
  public void testVasiInterface() throws IOException {
    Configuration c = parseConfig("iosxe-vasi-interface");
    assertThat(c, hasInterface("vasileft1", hasAddress("1.1.1.2/31")));
    assertThat(c, hasInterface("vasiright1", hasAddress("1.1.1.3/31")));
  }

  @Test
  public void testBgpRedistributeOspfExtraction() {
    {
      String hostname = "ios-bgp-redistribute-ospf";
      CiscoConfiguration vc = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_IOS);
      BgpRedistributionPolicy redistributionPolicy =
          vc.getDefaultVrf().getBgpProcess().getRedistributionPolicies().get(RoutingProtocol.OSPF);
      assertThat(redistributionPolicy.getRouteMap(), nullValue());
      assertThat(redistributionPolicy.getMetric(), nullValue());
      assertThat(
          redistributionPolicy.getSpecialAttributes().get(BgpRedistributionPolicy.OSPF_ROUTE_TYPES),
          nullValue());
    }
    {
      String hostname = "ios-bgp-redistribute-ospf-match-various";
      CiscoConfiguration vc = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_IOS);
      BgpRedistributionPolicy redistributionPolicy =
          vc.getDefaultVrf().getBgpProcess().getRedistributionPolicies().get(RoutingProtocol.OSPF);
      assertThat(redistributionPolicy.getRouteMap(), equalTo("ospf2bgp"));
      assertThat(redistributionPolicy.getMetric(), equalTo(10000L));
      assertThat(
          redistributionPolicy.getSpecialAttributes().get(BgpRedistributionPolicy.OSPF_ROUTE_TYPES),
          equalTo(
              new MatchProtocol(
                  RoutingProtocol.OSPF,
                  RoutingProtocol.OSPF_IA,
                  RoutingProtocol.OSPF_E1,
                  RoutingProtocol.OSPF_E2)));
    }
    {
      String hostname = "ios-bgp-redistribute-ospf-match-internal";
      CiscoConfiguration vc = parseCiscoConfig(hostname, ConfigurationFormat.CISCO_IOS);
      BgpRedistributionPolicy redistributionPolicy =
          vc.getDefaultVrf().getBgpProcess().getRedistributionPolicies().get(RoutingProtocol.OSPF);
      assertThat(redistributionPolicy.getRouteMap(), nullValue());
      assertThat(redistributionPolicy.getMetric(), nullValue());
      assertThat(
          redistributionPolicy.getSpecialAttributes().get(BgpRedistributionPolicy.OSPF_ROUTE_TYPES),
          equalTo(new MatchProtocol(RoutingProtocol.OSPF, RoutingProtocol.OSPF_IA)));
    }
  }

  @Test
  public void testEigrpShutdownExtraction() {
    CiscoConfiguration vc = parseCiscoConfig("ios-eigrp-shutdown", ConfigurationFormat.CISCO_IOS);
    Map<Long, EigrpProcess> procs = vc.getDefaultVrf().getEigrpProcesses();
    assertThat(procs.get(1L).getShutdown(), nullValue());
    assertFalse(procs.get(2L).getShutdown());
    assertTrue(procs.get(3L).getShutdown());
  }

  @Test
  public void testEigrpShutdownConversion() throws IOException {
    Configuration c = parseConfig("ios-eigrp-shutdown");
    Map<Long, org.batfish.datamodel.eigrp.EigrpProcess> procs =
        c.getDefaultVrf().getEigrpProcesses();
    assertThat(procs, hasKeys(1L, 2L));
  }

  @Test
  public void testEigrpRouterIdExtraction() {
    CiscoConfiguration vc =
        parseCiscoConfig("ios-eigrp-classic-routerid", ConfigurationFormat.CISCO_IOS);
    Map<Long, EigrpProcess> procs = vc.getDefaultVrf().getEigrpProcesses();
    assertThat(procs.get(1L).getRouterId(), equalTo(Ip.parse("1.1.1.1")));
    assertThat(procs.get(2L).getRouterId(), nullValue());
  }

  @Test
  public void testIosServicePolicyInner() throws IOException {
    String hostname = "ios-service-policy-inner";
    Batfish batfish = getBatfishForConfigurationNames(hostname);

    String filename = "configs/" + hostname;

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(ccae, hasNumReferrers(filename, POLICY_MAP, "PM", 2));
  }

  @Test
  public void testIosVrfLeakingConversion() throws IOException {
    String hostname = "ios-vrf-leaking";
    Configuration c = parseConfig(hostname);
    VrfLeakingConfig.Builder builder =
        VrfLeakingConfig.builder()
            .setBgpLeakConfig(new BgpLeakConfig(ExtendedCommunity.target(65003, 11)));
    assertThat(
        c.getVrfs().get("DST_VRF").getVrfLeakConfigs(),
        contains(builder.setImportFromVrf("SRC_VRF").setImportPolicy("IMPORT_MAP").build()));
    assertThat(
        c.getVrfs().get("NOT_UNDER_ROUTER_BGP").getVrfLeakConfigs(),
        contains(builder.setImportFromVrf("SRC_VRF").setImportPolicy(null).build()));
    assertThat(
        c.getVrfs().get("DST_IMPOSSIBLE").getVrfLeakConfigs(),
        contains(
            builder.setImportFromVrf("SRC_VRF").setImportPolicy("UNDEFINED~undefined").build()));
  }

  @Test
  public void testIosVrfLeakingRoutes() throws IOException {
    String hostname = "ios-vrf-leaking";

    Batfish batfish = getBatfishForConfigurationNames(hostname);

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>> ribs = dp.getRibs().get(hostname);
    Set<AbstractRoute> dstVrfRoutes = ribs.get("DST_VRF").getRoutes();
    Matcher<AbstractRoute> leakedRouteMatcher2220 =
        isBgpv4RouteThat(
            allOf(
                hasPrefix(Prefix.parse("2.2.2.0/24")),
                hasProtocol(RoutingProtocol.BGP),
                hasNextHop(NextHopVrf.of("SRC_VRF")),
                BgpRouteMatchers.hasCommunities(contains(ExtendedCommunity.target(65003, 11)))));
    assertThat(
        dstVrfRoutes,
        // 1.1.1.1/32 is denied by import map, only 2.2.2.0/24 is expected to be leaked.
        contains(leakedRouteMatcher2220));
    // VRF not defined under router bgp should still have a process, and get both routes
    // (because no import map is defined)
    assertThat(
        batfish
            .loadConfigurations(snapshot)
            .get(hostname)
            .getVrfs()
            .get("NOT_UNDER_ROUTER_BGP")
            .getBgpProcess(),
        notNullValue());
    assertThat(
        ribs.get("NOT_UNDER_ROUTER_BGP").getRoutes(),
        containsInAnyOrder(
            leakedRouteMatcher2220,
            isBgpv4RouteThat(
                allOf(
                    hasPrefix(Prefix.parse("1.1.1.1/32")),
                    hasProtocol(RoutingProtocol.BGP),
                    hasNextHop(NextHopVrf.of("SRC_VRF")),
                    BgpRouteMatchers.hasCommunities(
                        contains(ExtendedCommunity.target(65003, 11)))))));
    assertThat(ribs.get("DST_IMPOSSIBLE").getRoutes(), empty());
  }

  @Test
  public void testNatMalformedNatPool() throws IOException {
    String hostname = "ios-nat-malformed-pool";
    // Do not crash
    parseConfig(hostname);
  }
}

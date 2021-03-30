package org.batfish.grammar.cisco_nxos;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.matchers.WarningMatchers.hasText;
import static org.batfish.common.matchers.WarningsMatchers.hasRedFlags;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.datamodel.BgpRoute.DEFAULT_LOCAL_PREFERENCE;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.datamodel.IpWildcard.ipWithWildcardMask;
import static org.batfish.datamodel.Names.generatedBgpCommonExportPolicyName;
import static org.batfish.datamodel.Route.UNSET_NEXT_HOP_INTERFACE;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;
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
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasMetric;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopInterface;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopIp;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasTag;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasAllowLocalAsIn;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasSendCommunity;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasSendExtendedCommunity;
import static org.batfish.datamodel.matchers.AddressFamilyMatchers.hasAddressFamilyCapabilites;
import static org.batfish.datamodel.matchers.AddressFamilyMatchers.hasExportPolicy;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasCommunities;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRoute6FilterLists;
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
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasBumTransportIps;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasBumTransportMethod;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasSourceAddress;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasUdpPort;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasVni;
import static org.batfish.datamodel.matchers.VrfMatchers.hasL2VniSettings;
import static org.batfish.datamodel.vendor_family.cisco_nxos.NexusPlatform.NEXUS_3000;
import static org.batfish.datamodel.vendor_family.cisco_nxos.NexusPlatform.NEXUS_5000;
import static org.batfish.datamodel.vendor_family.cisco_nxos.NexusPlatform.NEXUS_6000;
import static org.batfish.datamodel.vendor_family.cisco_nxos.NexusPlatform.NEXUS_7000;
import static org.batfish.datamodel.vendor_family.cisco_nxos.NexusPlatform.NEXUS_9000;
import static org.batfish.datamodel.vendor_family.cisco_nxos.NxosMajorVersion.NXOS5;
import static org.batfish.datamodel.vendor_family.cisco_nxos.NxosMajorVersion.NXOS6;
import static org.batfish.datamodel.vxlan.Layer2Vni.DEFAULT_UDP_PORT;
import static org.batfish.grammar.cisco_nxos.CiscoNxosControlPlaneExtractor.PACKET_LENGTH_RANGE;
import static org.batfish.grammar.cisco_nxos.CiscoNxosControlPlaneExtractor.TCP_PORT_RANGE;
import static org.batfish.grammar.cisco_nxos.CiscoNxosControlPlaneExtractor.UDP_PORT_RANGE;
import static org.batfish.main.BatfishTestUtils.TEST_SNAPSHOT;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.DEFAULT_VRF_ID;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.DEFAULT_VRF_NAME;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.MANAGEMENT_VRF_NAME;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.computeRoutingPolicyName;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.eigrpNeighborExportPolicyName;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.eigrpNeighborImportPolicyName;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.eigrpRedistributionPolicyName;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.toJavaRegex;
import static org.batfish.representation.cisco_nxos.CiscoNxosStructureType.OBJECT_GROUP_IP_ADDRESS;
import static org.batfish.representation.cisco_nxos.Interface.defaultDelayTensOfMicroseconds;
import static org.batfish.representation.cisco_nxos.Interface.getDefaultBandwidth;
import static org.batfish.representation.cisco_nxos.Interface.getDefaultSpeed;
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
import static org.hamcrest.Matchers.hasSize;
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
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.plugin.IBatfish;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.BddTestbed;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.ConnectedRouteMetadata;
import org.batfish.datamodel.DscpType;
import org.batfish.datamodel.EigrpExternalRoute;
import org.batfish.datamodel.EigrpInternalRoute;
import org.batfish.datamodel.EigrpRoute;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Flow.Builder;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.IcmpCode;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Route6FilterLine;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SubRange;
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
import org.batfish.datamodel.eigrp.ClassicMetric;
import org.batfish.datamodel.eigrp.EigrpInterfaceSettings;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.eigrp.EigrpMetricValues;
import org.batfish.datamodel.eigrp.EigrpMetricVersion;
import org.batfish.datamodel.eigrp.EigrpProcess;
import org.batfish.datamodel.eigrp.EigrpProcessMode;
import org.batfish.datamodel.matchers.HsrpGroupMatchers;
import org.batfish.datamodel.matchers.IpAccessListMatchers;
import org.batfish.datamodel.matchers.NssaSettingsMatchers;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.datamodel.matchers.Route6FilterListMatchers;
import org.batfish.datamodel.matchers.RouteFilterListMatchers;
import org.batfish.datamodel.matchers.StubSettingsMatchers;
import org.batfish.datamodel.matchers.VniSettingsMatchers;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfAreaSummary.SummaryRouteBehavior;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfNetworkType;
import org.batfish.datamodel.packet_policy.FibLookup;
import org.batfish.datamodel.packet_policy.FibLookupOverrideLookupIp;
import org.batfish.datamodel.packet_policy.FlowEvaluator;
import org.batfish.datamodel.packet_policy.IngressInterfaceVrf;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.route.nh.NextHopDiscard;
import org.batfish.datamodel.route.nh.NextHopInterface;
import org.batfish.datamodel.route.nh.NextHopIp;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.CommunityContext;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExprEvaluator;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExprEvaluator;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.tracking.DecrementPriority;
import org.batfish.datamodel.vendor_family.cisco_nxos.NexusPlatform;
import org.batfish.dataplane.protocols.BgpProtocolHelper;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.ParserBatfishException;
import org.batfish.main.TestrigText;
import org.batfish.representation.cisco_nxos.ActionIpAccessListLine;
import org.batfish.representation.cisco_nxos.AddrGroupIpAddressSpec;
import org.batfish.representation.cisco_nxos.AddressFamily;
import org.batfish.representation.cisco_nxos.BgpGlobalConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfIpv4AddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfIpv6AddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfL2VpnEvpnAddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfL2VpnEvpnAddressFamilyConfiguration.RetainRouteType;
import org.batfish.representation.cisco_nxos.BgpVrfNeighborAddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.BgpVrfNeighborConfiguration;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType;
import org.batfish.representation.cisco_nxos.CiscoNxosStructureType;
import org.batfish.representation.cisco_nxos.DefaultVrfOspfProcess;
import org.batfish.representation.cisco_nxos.DistributeList;
import org.batfish.representation.cisco_nxos.DistributeList.DistributeListFilterType;
import org.batfish.representation.cisco_nxos.EigrpProcessConfiguration;
import org.batfish.representation.cisco_nxos.EigrpVrfConfiguration;
import org.batfish.representation.cisco_nxos.EigrpVrfIpv4AddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.EigrpVrfIpv6AddressFamilyConfiguration;
import org.batfish.representation.cisco_nxos.Evpn;
import org.batfish.representation.cisco_nxos.EvpnVni;
import org.batfish.representation.cisco_nxos.ExtendedCommunityOrAuto;
import org.batfish.representation.cisco_nxos.FragmentsBehavior;
import org.batfish.representation.cisco_nxos.HsrpGroupIpv4;
import org.batfish.representation.cisco_nxos.IcmpOptions;
import org.batfish.representation.cisco_nxos.Interface;
import org.batfish.representation.cisco_nxos.InterfaceAddressWithAttributes;
import org.batfish.representation.cisco_nxos.InterfaceHsrp;
import org.batfish.representation.cisco_nxos.InterfaceIpv6AddressWithAttributes;
import org.batfish.representation.cisco_nxos.IpAccessList;
import org.batfish.representation.cisco_nxos.IpAccessListLine;
import org.batfish.representation.cisco_nxos.IpAddressSpec;
import org.batfish.representation.cisco_nxos.IpAsPathAccessList;
import org.batfish.representation.cisco_nxos.IpAsPathAccessListLine;
import org.batfish.representation.cisco_nxos.IpCommunityListExpanded;
import org.batfish.representation.cisco_nxos.IpCommunityListExpandedLine;
import org.batfish.representation.cisco_nxos.IpCommunityListStandard;
import org.batfish.representation.cisco_nxos.IpCommunityListStandardLine;
import org.batfish.representation.cisco_nxos.IpPrefixList;
import org.batfish.representation.cisco_nxos.IpPrefixListLine;
import org.batfish.representation.cisco_nxos.Ipv6PrefixList;
import org.batfish.representation.cisco_nxos.Ipv6PrefixListLine;
import org.batfish.representation.cisco_nxos.Lacp;
import org.batfish.representation.cisco_nxos.Layer3Options;
import org.batfish.representation.cisco_nxos.LiteralIpAddressSpec;
import org.batfish.representation.cisco_nxos.LiteralPortSpec;
import org.batfish.representation.cisco_nxos.NameServer;
import org.batfish.representation.cisco_nxos.NtpServer;
import org.batfish.representation.cisco_nxos.Nve;
import org.batfish.representation.cisco_nxos.Nve.HostReachabilityProtocol;
import org.batfish.representation.cisco_nxos.Nve.IngressReplicationProtocol;
import org.batfish.representation.cisco_nxos.NveVni;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddress;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddressLine;
import org.batfish.representation.cisco_nxos.ObjectGroupIpPort;
import org.batfish.representation.cisco_nxos.ObjectGroupIpPortLine;
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
import org.batfish.representation.cisco_nxos.RedistributionPolicy;
import org.batfish.representation.cisco_nxos.RouteDistinguisherOrAuto;
import org.batfish.representation.cisco_nxos.RouteMap;
import org.batfish.representation.cisco_nxos.RouteMapEntry;
import org.batfish.representation.cisco_nxos.RouteMapMatchAsNumber;
import org.batfish.representation.cisco_nxos.RouteMapMatchAsPath;
import org.batfish.representation.cisco_nxos.RouteMapMatchCommunity;
import org.batfish.representation.cisco_nxos.RouteMapMatchInterface;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpAddress;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpAddressPrefixList;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpv6Address;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpv6AddressPrefixList;
import org.batfish.representation.cisco_nxos.RouteMapMatchMetric;
import org.batfish.representation.cisco_nxos.RouteMapMatchRouteType;
import org.batfish.representation.cisco_nxos.RouteMapMatchRouteType.Type;
import org.batfish.representation.cisco_nxos.RouteMapMatchSourceProtocol;
import org.batfish.representation.cisco_nxos.RouteMapMatchTag;
import org.batfish.representation.cisco_nxos.RouteMapMatchVlan;
import org.batfish.representation.cisco_nxos.RouteMapMetricType;
import org.batfish.representation.cisco_nxos.RouteMapSetAsPathPrependLastAs;
import org.batfish.representation.cisco_nxos.RouteMapSetAsPathPrependLiteralAs;
import org.batfish.representation.cisco_nxos.RouteMapSetCommListDelete;
import org.batfish.representation.cisco_nxos.RouteMapSetCommunity;
import org.batfish.representation.cisco_nxos.RouteMapSetIpNextHopLiteral;
import org.batfish.representation.cisco_nxos.RouteMapSetIpNextHopUnchanged;
import org.batfish.representation.cisco_nxos.RouteMapSetLocalPreference;
import org.batfish.representation.cisco_nxos.RouteMapSetMetric;
import org.batfish.representation.cisco_nxos.RouteMapSetMetricEigrp;
import org.batfish.representation.cisco_nxos.RouteMapSetMetricType;
import org.batfish.representation.cisco_nxos.RouteMapSetOrigin;
import org.batfish.representation.cisco_nxos.RouteMapSetTag;
import org.batfish.representation.cisco_nxos.RouteMapSetWeight;
import org.batfish.representation.cisco_nxos.RoutingProtocolInstance;
import org.batfish.representation.cisco_nxos.SnmpCommunity;
import org.batfish.representation.cisco_nxos.StaticRoute;
import org.batfish.representation.cisco_nxos.SwitchportMode;
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

  private final BddTestbed _bddTestbed = new BddTestbed(ImmutableMap.of(), ImmutableMap.of());
  private final IpAccessListToBdd _aclToBdd = _bddTestbed.getAclToBdd();
  private final IpSpaceToBDD _dstIpBdd = _bddTestbed.getDstIpBdd();
  private final IpSpaceToBDD _srcIpBdd = _bddTestbed.getSrcIpBdd();

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco_nxos/testconfigs/";
  private static final String SNAPSHOTS_PREFIX = "org/batfish/grammar/cisco_nxos/snapshots/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static @Nonnull OspfExternalRoute.Builder ospfExternalRouteBuilder() {
    return OspfExternalRoute.builder()
        .setAdvertiser("dummy")
        .setArea(0L)
        .setCostToAdvertiser(0L)
        .setNextHop(NextHopIp.of(Ip.parse("3.3.3.3")))
        .setLsaMetric(0L);
  }

  private static @Nonnull OspfIntraAreaRoute.Builder ospfRouteBuilder() {
    return OspfIntraAreaRoute.builder().setNextHop(NextHopIp.of(Ip.parse("3.3.3.3"))).setArea(0L);
  }

  private static @Nonnull OspfInterAreaRoute.Builder ospfIARouteBuilder() {
    return OspfInterAreaRoute.builder().setNextHop(NextHopIp.of(Ip.parse("3.3.3.3"))).setArea(0L);
  }

  private @Nonnull BDD toBDD(AclLineMatchExpr aclLineMatchExpr) {
    return _aclToBdd.toBdd(aclLineMatchExpr);
  }

  private @Nonnull BDD toMatchBDD(AclLine aclLine) {
    return _aclToBdd.toPermitAndDenyBdds(aclLine).getMatchBdd();
  }

  private @Nonnull BDD toIcmpIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toIfBDD(
        AclLineMatchExprs.and(
            matchFragmentOffset(0), matchIpProtocol(IpProtocol.ICMP), aclLineMatchExpr));
  }

  private @Nonnull BDD toIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toBDD(AclLineMatchExprs.and(matchFragmentOffset(0), aclLineMatchExpr));
  }

  private @Nonnull BDD toIfBDD(AclLine aclLine) {
    return toMatchBDD(aclLine).and(toBDD(matchFragmentOffset(0)));
  }

  private @Nonnull BDD toNonIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toBDD(
        AclLineMatchExprs.and(
            matchFragmentOffset(IntegerSpace.of(Range.closed(1, 8191))), aclLineMatchExpr));
  }

  private @Nonnull BDD toNonIfBDD(AclLine aclLine) {
    return toMatchBDD(aclLine)
        .and(toBDD(matchFragmentOffset(IntegerSpace.of(Range.closed(1, 8191)))));
  }

  private @Nonnull BDD toTcpIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toIfBDD(
        AclLineMatchExprs.and(
            matchFragmentOffset(0), matchIpProtocol(IpProtocol.TCP), aclLineMatchExpr));
  }

  private @Nonnull BDD toUdpIfBDD(AclLineMatchExpr aclLineMatchExpr) {
    return toIfBDD(
        AclLineMatchExprs.and(
            matchFragmentOffset(0), matchIpProtocol(IpProtocol.UDP), aclLineMatchExpr));
  }

  private @Nonnull Batfish getBatfishForConfigurationNames(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    Batfish batfish = BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
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
    IBatfish iBatfish = getBatfishForConfigurationNames(configurationNames);
    return iBatfish.loadConfigurations(iBatfish.getSnapshot());
  }

  private @Nonnull CiscoNxosConfiguration parseVendorConfig(String hostname) {
    String src = readResource(TESTCONFIGS_PREFIX + hostname, UTF_8);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    CiscoNxosCombinedParser ciscoNxosParser = new CiscoNxosCombinedParser(src, settings);
    NxosControlPlaneExtractor extractor =
        new NxosControlPlaneExtractor(src, ciscoNxosParser, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(
            ciscoNxosParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(TEST_SNAPSHOT, tree);
    CiscoNxosConfiguration vendorConfiguration =
        (CiscoNxosConfiguration) extractor.getVendorConfiguration();
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + hostname);
    // crash if not serializable
    return SerializationUtils.clone(vendorConfiguration);
  }

  private void assertRoutingPolicyDeniesRoute(RoutingPolicy routingPolicy, AbstractRoute route) {
    assertFalse(
        routingPolicy.process(
            route, Bgpv4Route.testBuilder().setNetwork(route.getNetwork()), Direction.OUT));
  }

  private void assertRoutingPolicyPermitsRoute(RoutingPolicy routingPolicy, AbstractRoute route) {
    assertTrue(
        routingPolicy.process(
            route, Bgpv4Route.testBuilder().setNetwork(route.getNetwork()), Direction.OUT));
  }

  private @Nonnull Bgpv4Route processRouteIn(RoutingPolicy routingPolicy, Bgpv4Route route) {
    Bgpv4Route.Builder builder = route.toBuilder();
    assertTrue(routingPolicy.process(route, builder, Direction.IN));
    return builder.build();
  }

  private @Nonnull OspfExternalRoute processRouteRedistributeOspf(
      RoutingPolicy routingPolicy, Bgpv4Route route) {
    OspfExternalRoute.Builder builder =
        OspfExternalRoute.builder()
            .setNetwork(route.getNetwork())
            .setNextHop(NextHopIp.of(Ip.parse("3.3.3.3")))
            .setLsaMetric(123L)
            .setArea(456L)
            .setCostToAdvertiser(789L)
            .setAdvertiser("n1");
    assertTrue(routingPolicy.process(route, builder, Direction.OUT));
    return builder.build();
  }

  @Test
  public void testImplicitShutdownSwitchportInference() throws IOException {
    for (String hostname :
        ImmutableList.of(
            "shutdown-switchport-inference-defaultl2", "shutdown-switchport-inference-defaultl3")) {
      Configuration c = parseConfig(hostname);
      assertThat(
          c,
          hasInterface(
              "Ethernet1/1",
              allOf(isActive(), hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE))));
      assertThat(
          c,
          hasInterface(
              "Ethernet1/2",
              allOf(
                  isActive(false), hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE))));
      assertThat(
          c,
          hasInterface(
              "Ethernet1/3",
              allOf(isActive(), hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.ACCESS))));
      assertThat(
          c,
          hasInterface(
              "Ethernet1/4",
              allOf(
                  isActive(false),
                  hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.ACCESS))));
    }
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

  /** See: https://github.com/batfish/batfish/issues/5081 */
  @Test
  public void testBgpPeerTemplateGH5081Extraction() {
    String peerName = "peer-rr-overlay";

    CiscoNxosConfiguration vc = parseVendorConfig("nxos_bgp_peer_template");
    BgpGlobalConfiguration bgp = vc.getBgpGlobalConfiguration();
    assertThat(bgp.getTemplatePeers(), hasKeys(peerName));
    BgpVrfNeighborConfiguration peer = bgp.getOrCreateTemplatePeer(peerName);
    assertThat(peer.getLocalAs(), equalTo(64603L));
    assertThat(peer.getL2VpnEvpnAddressFamily(), notNullValue());
    BgpVrfNeighborAddressFamilyConfiguration l2vpn = peer.getL2VpnEvpnAddressFamily();
    assertThat(l2vpn.getSendCommunityStandard(), equalTo(Boolean.TRUE));
    assertThat(l2vpn.getSendCommunityExtended(), equalTo(Boolean.TRUE));
    assertThat(l2vpn.getOutboundRouteMap(), equalTo("rm_rr_overlay_out"));

    Ip nIp = Ip.parse("10.0.0.1");
    BgpVrfConfiguration vrf = bgp.getVrfs().get(DEFAULT_VRF_NAME);
    assertThat(vrf.getNeighbors(), hasKeys(nIp));
    BgpVrfNeighborConfiguration neighbor = vrf.getNeighbors().get(nIp);
    assertThat(neighbor.getInheritPeer(), equalTo(peerName));
    assertThat(neighbor.getShutdown(), equalTo(Boolean.FALSE));
    assertThat(neighbor.getRemoteAs(), equalTo(64602L));
    assertThat(neighbor.getUpdateSource(), equalTo("Ethernet1/10"));
  }

  /** See: https://github.com/batfish/batfish/issues/5081 */
  @Test
  public void testBgpPeerTemplateGH5081Conversion() throws IOException {
    Configuration c = parseConfig("nxos_bgp_peer_template");
    assertThat(c, notNullValue());
  }

  @Test
  public void testClassMapParsing() {
    // TODO: make into an extraction test
    assertThat(parseVendorConfig("nxos_class_map"), notNullValue());
  }

  @Test
  public void testCryptoParsing() {
    // TODO: make into an extraction test
    assertThat(parseVendorConfig("nxos_crypto"), notNullValue());
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
      assertThat(ipv4u.getRedistributionPolicies(), hasSize(8));
      assertThat(
          ipv4u.getRedistributionPolicy(RoutingProtocolInstance.direct()),
          equalTo(new RedistributionPolicy(RoutingProtocolInstance.direct(), "DIR_MAP")));
      assertThat(
          ipv4u.getRedistributionPolicy(RoutingProtocolInstance.ospf("ospf_proc")),
          equalTo(new RedistributionPolicy(RoutingProtocolInstance.ospf("ospf_proc"), "OSPF_MAP")));
      assertThat(
          ipv4u.getRedistributionPolicy(RoutingProtocolInstance.ospf("OSPF_PROC2")),
          equalTo(
              new RedistributionPolicy(RoutingProtocolInstance.ospf("OSPF_PROC2"), "OSPF_MAP2")));
      assertThat(
          ipv4u.getRedistributionPolicy(RoutingProtocolInstance.eigrp("EIGRP")),
          equalTo(new RedistributionPolicy(RoutingProtocolInstance.eigrp("EIGRP"), "EIGRP_MAP")));

      BgpVrfIpv6AddressFamilyConfiguration ipv6u = vrf.getIpv6UnicastAddressFamily();
      assertThat(ipv6u, notNullValue());
      assertThat(ipv6u.getRedistributionPolicies(), hasSize(1));
      assertThat(
          ipv6u.getRedistributionPolicy(RoutingProtocolInstance.ospfv3("OSPFv3_PROC")),
          equalTo(
              new RedistributionPolicy(
                  RoutingProtocolInstance.ospfv3("OSPFv3_PROC"), "OSPFv3_MAP")));

      BgpVrfL2VpnEvpnAddressFamilyConfiguration l2vpn = vrf.getL2VpnEvpnAddressFamily();
      assertThat(l2vpn, notNullValue());
      assertThat(l2vpn.getMaximumPathsIbgp(), equalTo(64));
      assertThat(l2vpn.getMaximumPathsEbgp(), equalTo(1));
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
  public void testBgpNextHopUnchanged() throws IOException {
    Configuration c = parseConfig("nxos_bgp_nh_unchanged");
    RoutingPolicy nhipUnchangedPolicy = c.getRoutingPolicies().get("NHIP-UNCHANGED");

    // assert on the behavior of routing policy
    Ip originalNhip = Ip.parse("12.12.12.12");
    Bgpv4Route originalRoute =
        Bgpv4Route.testBuilder()
            .setNetwork(Prefix.parse("1.2.3.4/31"))
            .setNextHopIp(originalNhip)
            .setAdmin(1)
            .setOriginatorIp(Ip.parse("9.8.7.6"))
            .setOriginType(OriginType.EGP)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    Bgpv4Route.Builder outputRouteBuilder =
        Bgpv4Route.testBuilder().setNextHopIp(UNSET_ROUTE_NEXT_HOP_IP);

    Ip sessionPropsHeadIp = Ip.parse("1.1.1.1");
    BgpSessionProperties.Builder sessionProps =
        BgpSessionProperties.builder()
            .setHeadAs(1L)
            .setTailAs(1L)
            .setHeadIp(sessionPropsHeadIp)
            .setTailIp(Ip.parse("2.2.2.2"));
    BgpSessionProperties ibgpSession = sessionProps.setSessionType(SessionType.IBGP).build();
    BgpSessionProperties ebgpSession =
        sessionProps.setTailAs(2L).setSessionType(SessionType.EBGP_SINGLEHOP).build();

    // No operation for IBGP
    boolean shouldExportToIbgp =
        nhipUnchangedPolicy.processBgpRoute(
            originalRoute, outputRouteBuilder, ibgpSession, Direction.OUT, null);
    assertTrue(shouldExportToIbgp);
    assertThat(outputRouteBuilder.getNextHopIp(), equalTo(UNSET_ROUTE_NEXT_HOP_IP));

    // Preserves original route's next hop IP for EBGP
    boolean shouldExportToEbgp =
        nhipUnchangedPolicy.processBgpRoute(
            originalRoute, outputRouteBuilder, ebgpSession, Direction.OUT, null);
    assertTrue(shouldExportToEbgp);
    assertThat(outputRouteBuilder.getNextHopIp(), equalTo(originalNhip));

    // Original route has unset next hop IP: sets output route nhip to head IP of session props
    outputRouteBuilder.setNextHopIp(UNSET_ROUTE_NEXT_HOP_IP);
    Bgpv4Route noNhipRoute =
        originalRoute.toBuilder().setNextHop(NextHopDiscard.instance()).build();
    boolean shouldExportToEbgpUnsetNextHop =
        nhipUnchangedPolicy.processBgpRoute(
            noNhipRoute, outputRouteBuilder, ebgpSession, Direction.OUT, null);
    assertTrue(shouldExportToEbgpUnsetNextHop);
    assertThat(outputRouteBuilder.getNextHopIp(), equalTo(sessionPropsHeadIp));
  }

  @Test
  public void testBgpDefaultOriginateExtraction() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_bgp_default_originate");
    assertFalse(
        vc.getBgpGlobalConfiguration()
            .getVrfs()
            .get(DEFAULT_VRF_NAME)
            .getNeighbors()
            .get(Ip.parse("1.2.3.0"))
            .getIpv4UnicastAddressFamily()
            .getDefaultOriginate());
  }

  @Test
  public void testBgpRedistFromEigrpConversion() throws IOException {
    // BGP redistributes EIGRP with route-map redist_eigrp, which permits 5.5.5.0/24
    Configuration c = parseConfig("nxos_bgp_eigrp_redistribution");
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
            .setEigrpMetricVersion(EigrpMetricVersion.V2)
            .setEigrpMetric(
                ClassicMetric.builder()
                    .setValues(EigrpMetricValues.builder().setDelay(1).setBandwidth(1).build())
                    .build());
    EigrpRoute matchEigrp =
        internalRb.setNextHop(NextHopDiscard.instance()).setNetwork(matchRm).build();
    EigrpRoute noMatchEigrp =
        internalRb.setNextHop(NextHopDiscard.instance()).setNetwork(noMatchRm).build();

    {
      // Redistribute matching EIGRP route into EBGP
      Bgpv4Route.Builder rb =
          BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
              matchEigrp, bgpRouterId, nextHopIp, ebgpAdmin, RoutingProtocol.BGP);
      assertTrue(
          bgpExportPolicy.processBgpRoute(matchEigrp, rb, ebgpSessionProps, Direction.OUT, null));
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
          bgpExportPolicy.processBgpRoute(noMatchEigrp, rb, ebgpSessionProps, Direction.OUT, null));
    }
    {
      // Redistribute matching EIGRP route to IBGP
      Bgpv4Route.Builder rb =
          BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
              matchEigrp, bgpRouterId, nextHopIp, ibgpAdmin, RoutingProtocol.IBGP);
      assertTrue(
          bgpExportPolicy.processBgpRoute(matchEigrp, rb, ibgpSessionProps, Direction.OUT, null));
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
          bgpExportPolicy.processBgpRoute(noMatchEigrp, rb, ibgpSessionProps, Direction.OUT, null));
    }
    {
      // Ensure external EIGRP route can also match routing policy
      EigrpRoute matchEigrpEx =
          EigrpExternalRoute.testBuilder()
              .setProcessAsn(1L)
              .setDestinationAsn(2L)
              .setEigrpMetricVersion(EigrpMetricVersion.V2)
              .setEigrpMetric(
                  ClassicMetric.builder()
                      .setValues(EigrpMetricValues.builder().setDelay(1).setBandwidth(1).build())
                      .build())
              .setNetwork(matchRm)
              .build();
      Bgpv4Route.Builder rb =
          BgpProtocolHelper.convertNonBgpRouteToBgpRoute(
              matchEigrpEx, bgpRouterId, nextHopIp, ebgpAdmin, RoutingProtocol.BGP);
      assertTrue(
          bgpExportPolicy.processBgpRoute(matchEigrpEx, rb, ebgpSessionProps, Direction.OUT, null));
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
  public void testBootKickstartConversion() throws IOException {
    String hostname = "nxos_boot_kickstart";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVendorFamily().getCiscoNxos().getPlatform(), equalTo(NEXUS_7000));
  }

  @Test
  public void testBootKickstartExtraction() {
    String hostname = "nxos_boot_kickstart";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    String expectedImage = "bootflash:/n7000-s2-kickstart.6.2.16.bin";
    assertThat(vc.getBootKickstartSup1(), equalTo(expectedImage));
    assertThat(vc.getBootKickstartSup2(), equalTo(expectedImage));
  }

  @Test
  public void testBootKickstartSupExtraction() {
    String hostname = "nxos_boot_kickstart_sup";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getBootKickstartSup1(), equalTo("bootflash:/titanium-d1-kickstart.7.3.0.D1.1.bin"));
    assertThat(vc.getBootKickstartSup2(), equalTo("bootflash:/n7000-s2-kickstart.6.2.16.bin"));
  }

  @Test
  public void testBootNxosConversion() throws IOException {
    String hostname = "nxos_boot_nxos";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVendorFamily().getCiscoNxos().getPlatform(), equalTo(NEXUS_9000));
  }

  @Test
  public void testBootNxosExtraction() {
    String hostname = "nxos_boot_nxos";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    String expectedImage = "bootflash:/nxos.9.2.3.bin";
    assertThat(vc.getBootNxosSup1(), equalTo(expectedImage));
    assertThat(vc.getBootNxosSup2(), equalTo(expectedImage));
  }

  @Test
  public void testBootNxosSupExtraction() {
    String hostname = "nxos_boot_nxos_sup";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getBootNxosSup1(), equalTo("bootflash:/nxos.9.2.3.bin"));
    assertThat(vc.getBootNxosSup2(), equalTo("bootflash:/nxos.9.2.4.bin"));
  }

  @Test
  public void testBootSystemConversion() throws IOException {
    String hostname = "nxos_boot_system";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVendorFamily().getCiscoNxos().getPlatform(), equalTo(NEXUS_7000));
  }

  @Test
  public void testBootSystemExtraction() {
    String hostname = "nxos_boot_system";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    String expectedImage = "bootflash:/titanium-d1.7.3.0.D1.1.bin";
    assertThat(vc.getBootSystemSup1(), equalTo(expectedImage));
    assertThat(vc.getBootSystemSup2(), equalTo(expectedImage));
  }

  @Test
  public void testBootSystemSupExtraction() {
    String hostname = "nxos_boot_system_sup";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getBootSystemSup1(), equalTo("bootflash:/n7000-s2-dk9.6.2.16.bin"));
    assertThat(vc.getBootSystemSup2(), equalTo("bootflash:/titanium-d1.7.3.0.D1.1.bin"));
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
  public void testSystemDefaultSwitchportShutdownConversion() throws IOException {
    String hostname = "nxos_system_default_switchport_shutdown";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/6");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/7");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/8");
      assertThat(iface, isActive());
    }
  }

  @Test
  public void testSystemDefaultSwitchportShutdownExtraction() {
    String hostname = "nxos_system_default_switchport_shutdown";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertTrue(vc.getSystemDefaultSwitchportShutdown());
    assertThat(
        vc.getInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(iface.getShutdown(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertThat(iface.getShutdown(), equalTo(false));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertThat(iface.getShutdown(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/7");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/8");
      assertThat(iface.getShutdown(), equalTo(false));
    }
  }

  @Test
  public void testNoSystemDefaultSwitchportShutdownConversion() throws IOException {
    String hostname = "nxos_no_system_default_switchport_shutdown";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(iface, isActive(true));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/6");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/7");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/8");
      assertThat(iface, isActive());
    }
  }

  @Test
  public void testNoSystemDefaultSwitchportShutdownExtraction() {
    String hostname = "nxos_no_system_default_switchport_shutdown";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertFalse(vc.getSystemDefaultSwitchportShutdown());
    assertThat(
        vc.getInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(iface.getShutdown(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertThat(iface.getShutdown(), equalTo(false));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertThat(iface.getShutdown(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/7");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/8");
      assertThat(iface.getShutdown(), equalTo(false));
    }
  }

  @Test
  public void testSystemDefaultSwitchportConversion() throws IOException {
    String hostname = "nxos_system_default_switchport";
    Configuration c = parseConfig(hostname);

    assertThat(c.getAllInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3"));
    assertThat(
        c,
        hasInterface(
            "Ethernet1/1", hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.ACCESS)));
    assertThat(
        c,
        hasInterface("Ethernet1/2", hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE)));
    assertThat(
        c,
        hasInterface(
            "Ethernet1/3", hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.ACCESS)));
  }

  @Test
  public void testSystemDefaultSwitchportExtraction() {
    String hostname = "nxos_system_default_switchport";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertTrue(vc.getSystemDefaultSwitchport());
    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3"));
    assertThat(vc.getInterfaces().get("Ethernet1/1").getSwitchportMode(), nullValue());
    assertThat(
        vc.getInterfaces().get("Ethernet1/2").getSwitchportMode(), equalTo(SwitchportMode.NONE));
    assertThat(
        vc.getInterfaces().get("Ethernet1/3").getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
  }

  @Test
  public void testNoSystemDefaultSwitchportConversion() throws IOException {
    String hostname = "nxos_no_system_default_switchport";
    Configuration c = parseConfig(hostname);

    assertThat(c.getAllInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3"));
    assertThat(
        c,
        hasInterface("Ethernet1/1", hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE)));
    assertThat(
        c,
        hasInterface("Ethernet1/2", hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE)));
    assertThat(
        c,
        hasInterface(
            "Ethernet1/3", hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.ACCESS)));
  }

  @Test
  public void testNoSystemDefaultSwitchportExtraction() {
    String hostname = "nxos_no_system_default_switchport";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertFalse(vc.getSystemDefaultSwitchport());
    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3"));
    assertThat(vc.getInterfaces().get("Ethernet1/1").getSwitchportMode(), nullValue());
    assertThat(
        vc.getInterfaces().get("Ethernet1/2").getSwitchportMode(), equalTo(SwitchportMode.NONE));
    assertThat(
        vc.getInterfaces().get("Ethernet1/3").getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
  }

  @Test
  public void testTacacsServerConversion() throws IOException {
    String hostname = "nxos_tacacs_server";
    Configuration c = parseConfig(hostname);

    assertThat(c.getTacacsServers(), containsInAnyOrder("192.0.2.1", "192.0.2.2"));
    assertThat(c.getTacacsSourceInterface(), equalTo("mgmt0"));
  }

  @Test
  public void testTacacsServerExtraction() {
    String hostname = "nxos_tacacs_server";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getTacacsServers(), hasKeys("192.0.2.1", "192.0.2.2"));
    assertThat(vc.getTacacsSourceInterface(), equalTo("mgmt0"));
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

    String tenantVrfName = "tenant1";
    Ip routerId = Ip.parse("10.1.1.1");
    // All defined VXLAN Vnis
    ImmutableSortedSet<Layer2VniConfig> expectedL2Vnis =
        ImmutableSortedSet.of(
            Layer2VniConfig.builder()
                .setVni(1111)
                .setVrf(tenantVrfName)
                .setRouteDistinguisher(RouteDistinguisher.from(routerId, 32768))
                .setRouteTarget(ExtendedCommunity.target(1, 1111))
                .setImportRouteTarget(ExtendedCommunity.target(1, 1111).matchString())
                .build(),
            Layer2VniConfig.builder()
                .setVni(2222)
                .setVrf(DEFAULT_VRF_NAME)
                .setRouteDistinguisher(RouteDistinguisher.from(routerId, 32769))
                .setRouteTarget(ExtendedCommunity.target(1, 2222))
                .setImportRouteTarget(ExtendedCommunity.target(1, 2222).matchString())
                .build());
    ImmutableSortedSet<Layer3VniConfig> expectedL3Vnis =
        ImmutableSortedSet.of(
            Layer3VniConfig.builder()
                .setVni(3333)
                .setVrf(tenantVrfName)
                .setAdvertiseV4Unicast(true)
                .setRouteDistinguisher(RouteDistinguisher.from(routerId, 3))
                .setRouteTarget(ExtendedCommunity.target(1, 3333))
                .setImportRouteTarget(ExtendedCommunity.target(1, 3333).matchString())
                .build());
    BgpPeerConfig peer =
        c.getDefaultVrf().getBgpProcess().getActiveNeighbors().get(Prefix.parse("1.1.1.1/32"));
    assertThat(peer.getEvpnAddressFamily(), notNullValue());
    assertThat(peer.getEvpnAddressFamily().getL2VNIs(), equalTo(expectedL2Vnis));
    assertThat(peer.getEvpnAddressFamily().getL3VNIs(), equalTo(expectedL3Vnis));
    assertThat(c.getVrfs().get(tenantVrfName).getBgpProcess(), notNullValue());
  }

  @Test
  public void testTrackExtraction() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_track"), notNullValue());
  }

  @Test
  public void testTrackConversion() throws IOException {
    // TODO: make into conversion test
    assertThat(parseConfig("nxos_track"), notNullValue());
  }

  @Test
  public void testEigrpExtraction() {
    String hostname = "nxos_eigrp";
    CiscoNxosConfiguration c = parseVendorConfig(hostname);
    assertThat(c.getEigrpProcesses(), hasKeys("EIGRP1234", "123"));
    assertThat(c.getVrfs(), hasKeys(DEFAULT_VRF_NAME, MANAGEMENT_VRF_NAME, "VRF"));
    {
      EigrpProcessConfiguration proc = c.getOrCreateEigrpProcess("EIGRP1234");
      assertThat(proc, notNullValue());
      assertTrue(proc.getIsolate());
      assertThat(proc.getVrfs(), hasKeys(DEFAULT_VRF_NAME, "VRF", "NON_EXISTENT"));
      {
        EigrpVrfConfiguration vrf = proc.getVrf(DEFAULT_VRF_NAME);
        assertThat(vrf, notNullValue());
        assertThat(vrf.getAsn(), nullValue());
        assertThat(vrf.getDistanceInternal(), nullValue());
        assertThat(vrf.getDistanceExternal(), nullValue());
        assertThat(vrf.getRouterId(), equalTo(Ip.parse("5.5.5.5")));

        assertThat(vrf.getV4AddressFamily(), nullValue());
        assertThat(vrf.getV6AddressFamily(), nullValue());
        EigrpVrfIpv4AddressFamilyConfiguration vrfV4 = vrf.getVrfIpv4AddressFamily();
        assertThat(vrfV4, notNullValue());
        assertThat(
            vrfV4.getDefaultMetric(),
            equalTo(new org.batfish.representation.cisco_nxos.EigrpMetric(1, 2, 3, 4, 5)));
        assertThat(vrfV4.getRedistributionPolicies(), hasSize(8));
      }
      {
        EigrpVrfConfiguration vrf = proc.getVrf("VRF");
        assertThat(vrf, notNullValue());
        assertThat(vrf.getAsn(), equalTo(12345));
        assertThat(vrf.getDistanceInternal(), equalTo(20));
        assertThat(vrf.getDistanceExternal(), equalTo(22));
        assertThat(vrf.getRouterId(), nullValue());

        EigrpVrfIpv4AddressFamilyConfiguration v4 = vrf.getV4AddressFamily();
        assertThat(v4, notNullValue());
        assertThat(
            v4.getDefaultMetric(),
            equalTo(new org.batfish.representation.cisco_nxos.EigrpMetric(5, 4, 3, 2, 1)));
        assertThat(v4.getRedistributionPolicies(), hasSize(4));

        EigrpVrfIpv6AddressFamilyConfiguration v6 = vrf.getV6AddressFamily();
        assertThat(v6, notNullValue());
        assertThat(
            v6.getRedistributionPolicies(),
            contains(new RedistributionPolicy(RoutingProtocolInstance.ospfv3("OSPFv3"), "RMV6")));

        assertThat(vrf.getVrfIpv4AddressFamily(), notNullValue());
        assertThat(vrf.getVrfIpv4AddressFamily().getRedistributionPolicies(), empty());
      }
    }
    {
      EigrpProcessConfiguration proc = c.getOrCreateEigrpProcess("123");
      assertThat(proc, notNullValue());
      assertFalse(proc.getIsolate());
      assertThat(proc.getVrfs(), hasKeys(DEFAULT_VRF_NAME));
      EigrpVrfConfiguration vrf = proc.getVrf(DEFAULT_VRF_NAME);
      assertThat(vrf, notNullValue());
      assertThat(vrf.getAsn(), nullValue()); // extraction is null, will be set in conversion.
      assertThat(vrf.getRouterId(), equalTo(Ip.parse("1.2.3.5")));
    }
  }

  @Test
  public void testEigrpConversion() throws Exception {
    String hostname = "nxos_eigrp";
    Configuration c = parseConfig(hostname);
    assertThat(c.getVrfs(), hasKeys(DEFAULT_VRF_NAME, MANAGEMENT_VRF_NAME, "VRF"));
    {
      org.batfish.datamodel.Vrf v = c.getVrfs().get(DEFAULT_VRF_NAME);
      assertThat(v.getEigrpProcesses(), hasKeys(123L));
      EigrpProcess p123 = v.getEigrpProcesses().get(123L);
      assertThat(p123.getRouterId(), equalTo(Ip.parse("1.2.3.5")));
      assertThat(
          p123.getInternalAdminCost(),
          equalTo(EigrpProcessConfiguration.DEFAULT_DISTANCE_INTERNAL));
      assertThat(
          p123.getExternalAdminCost(),
          equalTo(EigrpProcessConfiguration.DEFAULT_DISTANCE_EXTERNAL));
      assertThat(p123.getMetricVersion(), equalTo(EigrpMetricVersion.V2));
    }
    {
      org.batfish.datamodel.Vrf v = c.getVrfs().get(MANAGEMENT_VRF_NAME);
      assertThat(v.getEigrpProcesses(), anEmptyMap());
    }
    {
      org.batfish.datamodel.Vrf v = c.getVrfs().get("VRF");
      assertThat(v.getEigrpProcesses(), hasKeys(12345L));
      EigrpProcess p12345 = v.getEigrpProcesses().get(12345L);
      assertThat(p12345.getRouterId(), equalTo(Ip.parse("98.98.98.98")));
      assertThat(p12345.getInternalAdminCost(), equalTo(20));
      assertThat(p12345.getExternalAdminCost(), equalTo(22));
      assertThat(p12345.getMetricVersion(), equalTo(EigrpMetricVersion.V2));
    }
  }

  @Test
  public void testEigrpRedistributionPolicy() throws Exception {
    String hostname = "nxos_eigrp_redist";
    Configuration c = parseConfig(hostname);
    RoutingPolicy redistPolicy =
        c.getRoutingPolicies().get(eigrpRedistributionPolicyName("default", 1));
    EigrpProcess eigrpProc = c.getDefaultVrf().getEigrpProcesses().get(1L);
    // vrf1 config is the same except it has a default-metric set
    RoutingPolicy vrfRedistPolicy =
        c.getRoutingPolicies().get(eigrpRedistributionPolicyName("vrf1", 1));
    EigrpProcess vrfEigrpProc = c.getVrfs().get("vrf1").getEigrpProcesses().get(1L);

    /*
    Redistribution policy should permit:
    - static routes to 1.1.1.1/32
    - BGP routes to 2.2.2.2/32
    - direct (connected) routes to 3.3.3.3/32
    */
    Prefix staticPermittedPrefix = Prefix.parse("1.1.1.1/32");
    Prefix bgpPermittedPrefix = Prefix.parse("2.2.2.2/32");
    Prefix connectedPermittedPrefix = Prefix.parse("3.3.3.3/32");
    org.batfish.datamodel.StaticRoute.Builder staticRb =
        org.batfish.datamodel.StaticRoute.testBuilder().setNextHopInterface("foo").setAdmin(1);
    org.batfish.datamodel.StaticRoute staticDenied =
        staticRb.setNetwork(bgpPermittedPrefix).build();
    org.batfish.datamodel.StaticRoute staticPermitted =
        staticRb.setNetwork(staticPermittedPrefix).build();
    Bgpv4Route.Builder bgpRb =
        Bgpv4Route.testBuilder()
            .setAdmin(1)
            .setOriginatorIp(Ip.parse("3.3.3.3"))
            .setOriginType(OriginType.IGP)
            .setProtocol(RoutingProtocol.IBGP);
    Bgpv4Route bgpDenied = bgpRb.setNetwork(staticPermittedPrefix).build();
    Bgpv4Route bgpPermitted = bgpRb.setNetwork(bgpPermittedPrefix).build();
    ConnectedRoute connectedDenied =
        ConnectedRoute.builder().setNetwork(Prefix.ZERO).setNextHopInterface("Ethernet1").build();
    ConnectedRoute connectedPermitted =
        connectedDenied.toBuilder().setNetwork(connectedPermittedPrefix).build();
    NextHopIp nh = NextHopIp.of(Ip.parse("5.5.5.5"));

    // Redistributed routes should have default EIGRP metric: bw 100000 kbps, delay 1E9 ps.
    EigrpMetric defaultMetric =
        ClassicMetric.builder()
            .setValues(EigrpMetricValues.builder().setBandwidth(100000).setDelay(1e9).build())
            .build();
    {
      // Redistribution policy denies static route that doesn't match static_redist route-map
      EigrpExternalRoute.Builder rb =
          EigrpExternalRoute.testBuilder()
              .setNextHop(nh)
              .setEigrpMetricVersion(EigrpMetricVersion.V2);
      assertFalse(redistPolicy.process(staticDenied, rb, eigrpProc, Direction.OUT));
    }
    {
      // Redistribution policy permits static route that matches static_redist route-map
      EigrpExternalRoute.Builder rb =
          EigrpExternalRoute.testBuilder()
              .setNextHop(nh)
              .setEigrpMetricVersion(EigrpMetricVersion.V2);
      assertTrue(redistPolicy.process(staticPermitted, rb, eigrpProc, Direction.OUT));
      // Policy should set default EIGRP metric. To check route's EIGRP metric, it needs to be
      // built, so first set other required fields.
      rb.setNetwork(staticPermittedPrefix).setProcessAsn(1L).setDestinationAsn(2L);
      assertThat(rb.build().getEigrpMetric(), equalTo(defaultMetric));
    }
    {
      // Redistribution policy denies BGP route that doesn't match bgp_redist route-map
      EigrpExternalRoute.Builder rb =
          EigrpExternalRoute.testBuilder()
              .setNextHop(nh)
              .setEigrpMetricVersion(EigrpMetricVersion.V2);
      assertFalse(redistPolicy.process(bgpDenied, rb, eigrpProc, Direction.OUT));
    }
    {
      // Redistribution policy permits BGP route that matches bgp_redist route-map
      EigrpExternalRoute.Builder rb =
          EigrpExternalRoute.testBuilder()
              .setNextHop(nh)
              .setEigrpMetricVersion(EigrpMetricVersion.V2);
      assertTrue(redistPolicy.process(bgpPermitted, rb, eigrpProc, Direction.OUT));
      // Policy should set default EIGRP metric. To check route's EIGRP metric, it needs to be
      // built, so first set other required fields.
      rb.setNetwork(bgpPermittedPrefix).setProcessAsn(1L).setDestinationAsn(2L);
      assertThat(rb.build().getEigrpMetric(), equalTo(defaultMetric));
    }
    {
      // Redistribution policy correctly denies/permits connected routes
      assertFalse(
          redistPolicy.process(
              connectedDenied,
              EigrpExternalRoute.testBuilder()
                  .setNextHop(nh)
                  .setEigrpMetricVersion(EigrpMetricVersion.V2),
              eigrpProc,
              Direction.OUT));
      assertTrue(
          redistPolicy.process(
              connectedPermitted,
              EigrpExternalRoute.testBuilder()
                  .setNextHop(nh)
                  .setEigrpMetricVersion(EigrpMetricVersion.V2),
              eigrpProc,
              Direction.OUT));
    }
    {
      // Make sure VRF redistribution policy correctly applies default-metric 1 2 3 4 5
      EigrpExternalRoute.Builder rb =
          EigrpExternalRoute.testBuilder().setEigrpMetricVersion(EigrpMetricVersion.V2);
      assertTrue(vrfRedistPolicy.process(staticPermitted, rb, vrfEigrpProc, Direction.OUT));
      // Policy should set default EIGRP metric. To check route's EIGRP metric, it needs to be
      // built, so first set other required fields.
      rb.setNetwork(staticPermittedPrefix).setProcessAsn(1L).setNextHop(nh).setDestinationAsn(2L);
      EigrpMetric expectedMetric =
          ClassicMetric.builder()
              .setValues(
                  EigrpMetricValues.builder()
                      .setBandwidth(1)
                      .setDelay(2e7)
                      .setReliability(3)
                      .setEffectiveBandwidth(4)
                      .setMtu(5)
                      .build())
              .build();
      assertThat(rb.build().getEigrpMetric(), equalTo(expectedMetric));
    }
    {
      // Make sure VRF redistribution policy does not redistribute BGP routes, since no BGP process
      // has ASN 2 (and vrf1 EIGRP config has `redistribute bgp 2 route-map bgp_redist`)
      EigrpExternalRoute.Builder rb = EigrpExternalRoute.testBuilder();
      assertFalse(vrfRedistPolicy.process(bgpPermitted, rb, vrfEigrpProc, Direction.OUT));
    }
  }

  @Test
  public void testEigrpDualProcess() throws Exception {
    // Don't crash.
    parseConfig("nxos_eigrp_double");
    // TODO: real test
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
  public void testFexParsing() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_fex"), notNullValue());
  }

  @Test
  public void testFlowExtraction() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_flow"), notNullValue());
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
  public void testInterfaceBreakoutExtraction() {
    // TODO: make into an extraction test
    assertThat(parseVendorConfig("nxos_interface_breakout"), notNullValue());
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
      assertThat(iface, hasHsrpGroup(3, hasPreempt(false)));
      // TODO: convert and test ip secondary
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
      assertThat(hsrp.getDelayMinimumSeconds(), equalTo(59));
      assertThat(hsrp.getDelayReloadSeconds(), equalTo(60));
      assertThat(hsrp.getVersion(), equalTo(2));
      assertThat(hsrp.getIpv4Groups(), hasKeys(2, 3));
      {
        HsrpGroupIpv4 group = hsrp.getIpv4Groups().get(2);
        assertThat(group.getIp(), equalTo(Ip.parse("192.0.2.1")));
        assertThat(
            group.getIpSecondaries(),
            containsInAnyOrder(Ip.parse("192.168.0.1"), Ip.parse("192.168.1.1")));
        assertThat(group.getName(), equalTo("hsrp-some-named-thing"));
        assertTrue(group.getPreempt());
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
      {
        HsrpGroupIpv4 group = hsrp.getIpv4Groups().get(3);
        assertNull((group.getName()));
        assertFalse(group.getPreempt());
        assertNull(group.getPreemptDelayMinimumSeconds());
        assertNull(group.getPreemptDelayReloadSeconds());
        assertNull(group.getPreemptDelaySyncSeconds());
        assertNull(group.getPriority());
        assertNull(group.getHelloIntervalMs());
        assertNull(group.getHoldTimeMs());
        assertThat(group.getTracks(), anEmptyMap());
      }
    }
  }

  @Test
  public void testInterfaceHsrpIpv6Extraction() {
    // TODO: turn into extraction test
    assertThat(parseVendorConfig("nxos_interface_hsrp_ipv6"), notNullValue());
  }

  @Test
  public void testInterfaceIpAddressConversion() throws IOException {
    String hostname = "nxos_interface_ip_address";
    Configuration c = parseConfig(hostname);

    assertThat(c.getAllInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(
          iface,
          allOf(
              hasAddress(ConcreteInterfaceAddress.parse("10.0.0.1/24")),
              hasAllAddresses(
                  containsInAnyOrder(
                      ConcreteInterfaceAddress.parse("10.0.0.1/24"),
                      ConcreteInterfaceAddress.parse("10.0.0.2/24"),
                      ConcreteInterfaceAddress.parse("10.0.0.3/24"),
                      ConcreteInterfaceAddress.parse("10.50.0.2/16")))));
      assertThat(
          iface.getAddressMetadata(),
          allOf(
              hasEntry(
                  ConcreteInterfaceAddress.parse("10.0.0.1/24"),
                  ConnectedRouteMetadata.builder()
                      .setAdmin(0)
                      .setTag(0)
                      .setGenerateLocalRoute(true)
                      .build()),
              hasEntry(
                  ConcreteInterfaceAddress.parse("10.0.0.2/24"),
                  ConnectedRouteMetadata.builder()
                      .setAdmin(0)
                      .setTag(0)
                      .setGenerateLocalRoute(true)
                      .build()),
              hasEntry(
                  ConcreteInterfaceAddress.parse("10.0.0.3/24"),
                  ConnectedRouteMetadata.builder()
                      .setAdmin(5)
                      .setTag(3)
                      .setGenerateLocalRoute(true)
                      .build())));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      // TODO: Instead, expect something like DhcpInterfaceAddress as sole address
      assertThat(iface, allOf(hasAddress(nullValue()), hasAllAddresses(empty())));
    }
  }

  @Test
  public void testInterfaceIpAddressExtraction() {
    String hostname = "nxos_interface_ip_address";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      InterfaceAddressWithAttributes primary =
          new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.0.0.1/24"));
      InterfaceAddressWithAttributes secondary2 =
          new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.0.0.2/24"));
      InterfaceAddressWithAttributes secondary3 =
          new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.0.0.3/24"));
      InterfaceAddressWithAttributes secondary50 =
          new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.50.0.2/16"));
      secondary3.setTag(3L);
      secondary3.setRoutePreference(5);
      assertThat(iface.getAddress(), equalTo(primary));
      assertThat(
          iface.getSecondaryAddresses(), containsInAnyOrder(secondary2, secondary3, secondary50));
      assertFalse(iface.getIpAddressDhcp());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertThat(iface.getAddress(), nullValue());
      assertThat(iface.getSecondaryAddresses(), empty());
      assertTrue(iface.getIpAddressDhcp());
    }
  }

  @Test
  public void testInterfaceIpv6AddressExtraction() {
    String hostname = "nxos_interface_ipv6_address";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1", "Ethernet1/2"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      InterfaceIpv6AddressWithAttributes primary =
          new InterfaceIpv6AddressWithAttributes(Ip6.parse("10::1"), 120);
      InterfaceIpv6AddressWithAttributes secondary2 =
          new InterfaceIpv6AddressWithAttributes(Ip6.parse("10::2"), 120);
      InterfaceIpv6AddressWithAttributes secondary3 =
          new InterfaceIpv6AddressWithAttributes(Ip6.parse("10::3"), 120);
      secondary3.setTag(3L);
      assertThat(iface.getIpv6Address(), equalTo(primary));
      assertThat(iface.getIpv6AddressSecondaries(), containsInAnyOrder(secondary2, secondary3));
      assertFalse(iface.getIpv6AddressDhcp());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertThat(iface.getIpv6Address(), nullValue());
      assertThat(iface.getIpv6AddressSecondaries(), empty());
      assertTrue(iface.getIpv6AddressDhcp());
    }
  }

  @Test
  public void testInterfaceIpv6DhcpRelayExtraction() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_interface_ipv6_dhcp_relay"), notNullValue());
  }

  @Test
  public void testInterfaceIpv6NdExtraction() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_interface_ipv6_nd"), notNullValue());
  }

  @Test
  public void testInterfaceMulticastParsing() throws IOException {
    // TODO: make into extraction test
    parseConfig("nxos_interface_multicast");
    // don't crash.
  }

  @Test
  public void testInterfacePbrExtraction() {
    CiscoNxosConfiguration c = parseVendorConfig("nxos_interface_ip_policy");
    assertThat(c.getInterfaces().get("Ethernet1/1").getPbrPolicy(), equalTo("PBR_POLICY"));
  }

  @Test
  public void testInterfacePbrConversion() throws IOException {
    String hostname = "nxos_interface_ip_policy";
    Configuration c = parseConfig(hostname);
    String policyName = "PBR_POLICY";
    PacketPolicy policy = c.getPacketPolicies().get(policyName);
    assertThat(policy, notNullValue());
    assertThat(c.getAllInterfaces().get("Ethernet1/1").getRoutingPolicyName(), equalTo(policyName));
    Builder builder =
        Flow.builder()
            .setIngressNode(hostname)
            .setIngressInterface("eth0")
            .setSrcIp(Ip.parse("8.8.8.8"))
            .setSrcPort(22222)
            .setDstPort(22);
    Flow acceptedFlow = builder.setDstIp(Ip.parse("1.1.1.100")).build();
    Flow rejectedFlow = builder.setDstIp(Ip.parse("3.3.3.3")).build();
    FibLookup regularFibLookup = new FibLookup(IngressInterfaceVrf.instance());
    // Accepted flow sent to 2.2.2.2
    assertThat(
        FlowEvaluator.evaluate(
                acceptedFlow,
                "eth0",
                DEFAULT_VRF_NAME,
                policy,
                c.getIpAccessLists(),
                ImmutableMap.of(),
                ImmutableMap.of())
            .getAction(),
        equalTo(
            FibLookupOverrideLookupIp.builder()
                .setIps(ImmutableList.of(Ip.parse("2.2.2.2")))
                .setVrfExpr(IngressInterfaceVrf.instance())
                .setDefaultAction(regularFibLookup)
                .setRequireConnected(true)
                .build()));

    // Rejected flow delegated to regular FIB lookup
    assertThat(
        FlowEvaluator.evaluate(
                rejectedFlow,
                "eth0",
                DEFAULT_VRF_NAME,
                policy,
                c.getIpAccessLists(),
                ImmutableMap.of(),
                ImmutableMap.of())
            .getAction(),
        equalTo(regularFibLookup));
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
  public void testInterfaceShowRunAll1() throws IOException {
    String hostname = "nxos_interface_show_all_1";
    Configuration c = parseConfig(hostname);
    assertThat(
        c, hasInterface("Ethernet1/21", hasDescription("Made it to the end of Ethernet1/21")));
  }

  @Test
  public void testInterfaceShutdownConversion() throws IOException {
    String hostname = "nxos_interface_shutdown";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8"));
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
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/6");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/7");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/8");
      assertThat(iface, isActive());
    }
  }

  @Test
  public void testInterfaceShutdownExtraction() {
    String hostname = "nxos_interface_shutdown";
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
            "Ethernet1/7",
            "Ethernet1/8"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(iface.getShutdown(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertThat(iface.getShutdown(), equalTo(false));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertThat(iface.getShutdown(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/7");
      // TODO: more interesting test of regular shutdown vs force shutdown
      assertThat(iface.getShutdown(), equalTo(true));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/8");
      assertThat(iface.getShutdown(), equalTo(false));
    }
  }

  @Test
  public void testInterfaceSpanningTreeParsing() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_interface_spanning_tree"), notNullValue());
  }

  @Test
  public void testInterfaceRuntimeSpeedConversion() throws IOException {
    String snapshotName = "runtime_data";
    String hostname = "c1";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(SNAPSHOTS_PREFIX + snapshotName, ImmutableSet.of(hostname))
                .setRuntimeDataPrefix(SNAPSHOTS_PREFIX + snapshotName)
                .build(),
            _folder);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    Map<String, org.batfish.datamodel.Interface> interfaces = c.getAllInterfaces();

    // Get name-based default guess for speed and ensure it does not match configured/runtime values
    double defaultSpeed = getDefaultSpeed(CiscoNxosInterfaceType.ETHERNET);
    assertTrue(defaultSpeed != 1E8 && defaultSpeed != 2E8);

    List<Double> expectedSpeeds =
        ImmutableList.of(
            // Ethernet1/0 has both configured and runtime speed 1E8
            1E8,
            // Ethernet1/1 has configured speed 1E8 but runtime speed 2E8; configured should win
            1E8,
            // Ethernet1/2 has configured speed 1E8 and null runtime speed
            1E8,
            // Ethernet1/3 has configured speed 1E8 and no runtime data
            1E8,
            // Ethernet1/4 has no configured speed and runtime speed 2E8
            2E8,
            // Ethernet1/5 has no configured speed and null runtime speed
            defaultSpeed,
            // Ethernet1/6 has no configured speed and no runtime data
            defaultSpeed);

    for (int i = 0; i < expectedSpeeds.size(); i++) {
      String ifaceName = "Ethernet1/" + i;
      Double expectedSpeed = expectedSpeeds.get(i);

      // Assert on both speed and bandwidth, which should equal speed since no bandwidths are set
      assertThat(
          String.format("Unexpected value for %s", ifaceName),
          interfaces.get(ifaceName),
          allOf(hasSpeed(expectedSpeed), hasBandwidth(expectedSpeed)));
    }

    // Should see a single conversion warning for Ethernet1/1's conflicting speeds
    Warnings warnings =
        batfish
            .loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot())
            .getWarnings()
            .get(hostname);
    assertThat(
        warnings,
        hasRedFlags(
            contains(
                hasText(
                    "Interface c1:Ethernet1/1 has configured speed 100000000 bps but runtime data"
                        + " shows speed 200000000 bps. Configured value will be used."))));
  }

  @Test
  public void testInterfaceRuntimeBandwidthConversion() throws IOException {
    String snapshotName = "runtime_data";
    String hostname = "c2";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(SNAPSHOTS_PREFIX + snapshotName, ImmutableSet.of(hostname))
                .setRuntimeDataPrefix(SNAPSHOTS_PREFIX + snapshotName)
                .build(),
            _folder);
    Map<String, org.batfish.datamodel.Interface> interfaces =
        batfish.loadConfigurations(batfish.getSnapshot()).get(hostname).getAllInterfaces();

    // Get name-based default guess for bw and ensure it does not match configured/runtime values
    double defaultBandwidth = getDefaultBandwidth(CiscoNxosInterfaceType.ETHERNET);
    assertTrue(defaultBandwidth != 1E8 && defaultBandwidth != 2E8);

    List<Double> expectedBandwidths =
        ImmutableList.of(
            // Ethernet1/0 has both configured and runtime bw 1E8
            1E8,
            // Ethernet1/1 has configured bw 1E8 but runtime bw 2E8; configured should win
            1E8,
            // Ethernet1/2 has configured bw 1E8 and null runtime bw
            1E8,
            // Ethernet1/3 has configured bw 1E8 and no runtime data
            1E8,
            // Ethernet1/4 has no configured bw and runtime bw 2E8
            2E8,
            // Ethernet1/5 has no configured bw and null runtime vw
            defaultBandwidth,
            // Ethernet1/6 has no configured bw and no runtime data
            defaultBandwidth);

    for (int i = 0; i < expectedBandwidths.size(); i++) {
      String ifaceName = "Ethernet1/" + i;
      Double expectedBandwidth = expectedBandwidths.get(i);
      assertThat(
          String.format("Unexpected value for %s", ifaceName),
          interfaces.get(ifaceName),
          hasBandwidth(expectedBandwidth));
    }

    // Should see a single conversion warning for Ethernet1/1's conflicting bandwidths
    Warnings warnings =
        batfish
            .loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot())
            .getWarnings()
            .get(hostname);
    assertThat(
        warnings,
        hasRedFlags(
            contains(
                hasText(
                    "Interface c2:Ethernet1/1 has configured bandwidth 100000000 bps but runtime"
                        + " data shows bandwidth 200000000 bps. Configured value will be used."))));
  }

  @Test
  public void testInterfaceRuntimeBandwidthAndSpeedConversion() throws IOException {
    // For testing interaction between configured and runtime speeds and bandwidths
    String snapshotName = "runtime_data";
    String hostname = "c3";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(SNAPSHOTS_PREFIX + snapshotName, ImmutableSet.of(hostname))
                .setRuntimeDataPrefix(SNAPSHOTS_PREFIX + snapshotName)
                .build(),
            _folder);
    Map<String, org.batfish.datamodel.Interface> interfaces =
        batfish.loadConfigurations(batfish.getSnapshot()).get(hostname).getAllInterfaces();

    // Ethernet1/0 has configured & runtime bw 2E8, configured & runtime speed 1E8.
    assertThat(interfaces.get("Ethernet1/0"), allOf(hasBandwidth(2E8), hasSpeed(1E8)));

    // Ethernet1/1 has configured speed 1E8 and runtime bw 2E8. Speed should be 1E8, and bandwidth
    // should also be 1E8 because configured speed takes precedence over runtime bw.
    assertThat(interfaces.get("Ethernet1/1"), allOf(hasBandwidth(1E8), hasSpeed(1E8)));

    // Ethernet1/2 has configured bw 1E8 and runtime speed 2E8. Runtime speed should not affect bw.
    assertThat(interfaces.get("Ethernet1/2"), allOf(hasBandwidth(1E8), hasSpeed(2E8)));

    // Ethernet1/3 has runtime bw 2E8 and runtime speed 1E8. Neither should affect the other.
    assertThat(interfaces.get("Ethernet1/3"), allOf(hasBandwidth(2E8), hasSpeed(1E8)));

    // No warnings
    assertNull(
        batfish
            .loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot())
            .getWarnings()
            .get(hostname));
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
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("loopback0");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("mgmt0");
      // Management interfaces are blacklisted in post-processing
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel1");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2.1");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2.2");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), equalTo(2));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel2");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel2.1");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(2));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(2));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/6");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/7");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/8");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3966))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/9");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/10");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 2))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/11");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/12");
      assertThat(iface, isActive());
      assertThat(
          iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.DOT1Q_TUNNEL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/13");
      assertThat(iface, isActive());
      assertThat(
          iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.FEX_FABRIC));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/14");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/15");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(org.batfish.datamodel.SwitchportMode.MONITOR));
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
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("loopback0");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("mgmt0");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel1");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2.1");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2.2");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getEncapsulationVlan(), equalTo(2));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel2.1");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(2));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(2));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/7");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/8");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3966))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/9");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/10");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 2))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/11");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/12");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.DOT1Q_TUNNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/13");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.FEX_FABRIC));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/14");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/15");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.MONITOR));
      assertTrue(iface.getSwitchportMonitor());
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
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2.1");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
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
                "here is a description with punctuation! and IP address 1.2.3.4/24 and trailing"
                    + " whitespace"),
            hasMtu(9216)));
    assertTrue(eth11.getAutoState());
    assertThat(eth11.getDhcpRelayAddresses(), contains(Ip.parse("1.2.3.4"), Ip.parse("1.2.3.5")));
    assertThat(eth11.getIncomingFilter(), IpAccessListMatchers.hasName("acl_in"));
    assertThat(eth11.getOutgoingFilter(), IpAccessListMatchers.hasName("acl_out"));
    // TODO: convert and test delay

    assertThat(c, hasInterface("mgmt0", hasBandwidth(1e9)));
  }

  @Test
  public void testInterfacePropertiesExtraction() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_interface_properties");
    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3", "Ethernet100/100", "mgmt0"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(iface.getDelayTensOfMicroseconds(), equalTo(10));
      assertThat(
          iface.getDescription(),
          equalTo(
              "here is a description with punctuation! and IP address 1.2.3.4/24 and trailing"
                  + " whitespace"));
      assertThat(iface.getDhcpRelayAddresses(), contains(Ip.parse("1.2.3.4"), Ip.parse("1.2.3.5")));
      assertThat(iface.getEigrp(), equalTo("100"));
      assertThat(iface.getIpAccessGroupIn(), equalTo("acl_in"));
      assertThat(iface.getIpAccessGroupOut(), equalTo("acl_out"));
      assertThat(iface.getIpForward(), equalTo(Boolean.TRUE));
      assertThat(iface.getIpProxyArp(), equalTo(Boolean.TRUE));
      assertThat(iface.getMtu(), equalTo(9216));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertTrue(iface.getAutostate());
      assertThat(iface.getDescription(), nullValue());
      assertThat(iface.getEigrp(), nullValue());
      assertThat(iface.getIpForward(), equalTo(Boolean.FALSE));
      assertThat(iface.getIpProxyArp(), equalTo(Boolean.FALSE));
      assertThat(iface.getAccessVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getAutostate());
      assertThat(iface.getIpForward(), nullValue());
      assertThat(iface.getIpProxyArp(), nullValue());
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
          acl.getLines().stream().map(this::toMatchBDD).collect(ImmutableList.toImmutableList()),
          contains(
              toBDD(matchIpProtocol(1)),
              toBDD(matchIpProtocol(4)),
              toBDD(matchIpProtocol(2)),
              toBDD(matchIpProtocol(3))));
    }
    {
      org.batfish.datamodel.IpAccessList acl = c.getIpAccessLists().get("acl_simple_protocols");
      assertThat(
          acl.getLines().stream().map(this::toMatchBDD).collect(ImmutableList.toImmutableList()),
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
          acl.getLines().stream().map(tb::toBDD).collect(ImmutableList.toImmutableList()),
          contains(
              tb.toBDD(matchDst(ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.0.0.255")))),
              tb.toBDD(matchDst(Prefix.parse("10.0.1.0/24"))),
              tb.toBDD(matchDst(Ip.parse("10.0.5.5"))),
              tb.toBDD(matchDst(Ip.parse("10.0.2.2"))),
              tb.toBDD(AclLineMatchExprs.TRUE)));
      // test that we preserve the content of the line
      assertThat(acl.getLines().get(0).getName(), equalTo("permit ip any 10.0.0.0 0.0.0.255"));
    }
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_common_ip_options_source_ip");
      assertThat(
          acl.getLines().stream().map(tb::toBDD).collect(ImmutableList.toImmutableList()),
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
          acl.getLines().stream().map(this::toMatchBDD).collect(ImmutableList.toImmutableList()),
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
          acl.getLines().stream().map(this::toMatchBDD).collect(ImmutableList.toImmutableList()),
          contains(
              toBDD(matchPacketLength(100)),
              toBDD(matchPacketLength(IntegerSpace.of(Range.closed(20, 199)))),
              toBDD(
                  matchPacketLength(
                      IntegerSpace.of(Range.closed(301, Integer.MAX_VALUE))
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
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
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
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
          contains(
              toTcpIfBDD(matchDstPort(1)),
              toTcpIfBDD(
                  matchDstPort(
                      TCP_PORT_RANGE.intersection(
                          IntegerSpace.of(Range.closed(6, Integer.MAX_VALUE))))),
              toTcpIfBDD(matchDstPort(IntegerSpace.of(Range.closed(0, 9)))),
              toTcpIfBDD(matchDstPort(TCP_PORT_RANGE.difference(IntegerSpace.of(15)))),
              toTcpIfBDD(matchDstPort(65432)),
              toTcpIfBDD(matchDstPort(IntegerSpace.of(Range.closed(20, 25))))));
    }
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_tcp_destination_ports_named");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
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
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
          contains(
              toTcpIfBDD(matchSrcPort(1)),
              toTcpIfBDD(
                  matchSrcPort(
                      TCP_PORT_RANGE.intersection(
                          IntegerSpace.of(Range.closed(6, Integer.MAX_VALUE))))),
              toTcpIfBDD(matchSrcPort(IntegerSpace.of(Range.closed(0, 9)))),
              toTcpIfBDD(matchSrcPort(TCP_PORT_RANGE.difference(IntegerSpace.of(15)))),
              toTcpIfBDD(matchSrcPort(54321)),
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
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
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
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
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
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
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
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
          contains(
              toUdpIfBDD(matchDstPort(1)),
              toUdpIfBDD(
                  matchDstPort(
                      UDP_PORT_RANGE.intersection(
                          IntegerSpace.of(Range.closed(6, Integer.MAX_VALUE))))),
              toUdpIfBDD(matchDstPort(IntegerSpace.of(Range.closed(0, 9)))),
              toUdpIfBDD(matchDstPort(UDP_PORT_RANGE.difference(IntegerSpace.of(15)))),
              toUdpIfBDD(matchDstPort(65432)),
              toUdpIfBDD(matchDstPort(IntegerSpace.of(Range.closed(20, 25))))));
    }
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_udp_destination_ports_named");
      // check behavior for initial fragments only
      assertThat(
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
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
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
          contains(
              toUdpIfBDD(matchSrcPort(1)),
              toUdpIfBDD(
                  matchSrcPort(
                      UDP_PORT_RANGE.intersection(
                          IntegerSpace.of(Range.closed(6, Integer.MAX_VALUE))))),
              toUdpIfBDD(matchSrcPort(IntegerSpace.of(Range.closed(0, 9)))),
              toUdpIfBDD(matchSrcPort(UDP_PORT_RANGE.difference(IntegerSpace.of(15)))),
              toUdpIfBDD(matchSrcPort(54321)),
              toUdpIfBDD(matchSrcPort(IntegerSpace.of(Range.closed(20, 25))))));
    }
    // TODO: support UDP VXLAN matching
    {
      org.batfish.datamodel.IpAccessList acl =
          c.getIpAccessLists().get("acl_l4_fragments_semantics");
      // check behavior for initial fragments
      assertThat(
          acl.getLines().stream().map(this::toIfBDD).collect(ImmutableList.toImmutableList()),
          contains(
              toIcmpIfBDD(AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIcmpType(0))),
              toIcmpIfBDD(AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIcmpType(1))),
              toIfBDD(AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIpProtocol(2))),
              toIfBDD(AclLineMatchExprs.and(matchSrc(Ip.parse("192.0.2.1")), matchIpProtocol(3))),
              toBDD(AclLineMatchExprs.FALSE),
              toBDD(AclLineMatchExprs.FALSE)));

      // check behavior for non-initial fragments
      assertThat(
          acl.getLines().stream().map(this::toNonIfBDD).collect(ImmutableList.toImmutableList()),
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
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(_dstIpBdd),
          equalTo(
              ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.0.0.255"))
                  .toIpSpace()
                  .accept(_dstIpBdd)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(_dstIpBdd),
          equalTo(Prefix.parse("10.0.1.0/24").toIpSpace().accept(_dstIpBdd)));

      spec = specs.next();
      assertThat(((AddrGroupIpAddressSpec) spec).getName(), equalTo("mydstaddrgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(_dstIpBdd),
          equalTo(Ip.parse("10.0.2.2").toIpSpace().accept(_dstIpBdd)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(_dstIpBdd),
          equalTo(UniverseIpSpace.INSTANCE.accept(_dstIpBdd)));
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
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(_srcIpBdd),
          equalTo(
              ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.0.0.255"))
                  .toIpSpace()
                  .accept(_srcIpBdd)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(_srcIpBdd),
          equalTo(Prefix.parse("10.0.1.0/24").toIpSpace().accept(_srcIpBdd)));

      spec = specs.next();
      assertThat(((AddrGroupIpAddressSpec) spec).getName(), equalTo("mysrcaddrgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(_srcIpBdd),
          equalTo(Ip.parse("10.0.2.2").toIpSpace().accept(_srcIpBdd)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(_srcIpBdd),
          equalTo(UniverseIpSpace.INSTANCE.accept(_srcIpBdd)));
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
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(_srcIpBdd),
          equalTo(_srcIpBdd.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.ICMP));

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(_srcIpBdd),
          equalTo(_srcIpBdd.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.ICMP));

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(_srcIpBdd),
          equalTo(_srcIpBdd.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.fromNumber(2)));

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(_srcIpBdd),
          equalTo(_srcIpBdd.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.fromNumber(3)));

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(_srcIpBdd),
          equalTo(_srcIpBdd.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.fromNumber(4)));
      assertTrue(line.getFragments());

      line = (ActionIpAccessListLine) lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(
          ((LiteralIpAddressSpec) line.getSrcAddressSpec()).getIpSpace().accept(_srcIpBdd),
          equalTo(_srcIpBdd.toBDD(Ip.parse("192.0.2.1"))));
      assertThat(line.getProtocol(), equalTo(IpProtocol.fromNumber(5)));
      assertTrue(line.getFragments());
    }
  }

  @Test
  public void testIpAccessListReferences() throws IOException {
    String hostname = "nxos_ip_access_list_references";
    String filename = String.format("configs/%s", hostname);
    Batfish bf = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        bf.loadConvertConfigurationAnswerElementOrReparse(bf.getSnapshot());

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
  public void testIpCommunityListExpandedConversion() throws IOException {
    String hostname = "nxos_ip_community_list_expanded";
    Configuration c = parseConfig(hostname);
    CommunityContext ctx = CommunityContext.builder().build();

    // Each list should be converted to both a CommunityMatchExpr and a CommunitySetMatchExpr.
    {
      // Test CommunityMatchExpr conversion
      assertThat(c.getCommunityMatchExprs(), hasKeys("cl_seq", "cl_test"));
      CommunityMatchExprEvaluator eval = ctx.getCommunityMatchExprEvaluator();
      {
        CommunityMatchExpr expr = c.getCommunityMatchExprs().get("cl_seq");

        // permit regex 1:1
        assertTrue(expr.accept(eval, StandardCommunity.of(1, 1)));
        assertTrue(expr.accept(eval, StandardCommunity.of(91, 19)));
        // permit regex 5:5
        assertTrue(expr.accept(eval, StandardCommunity.of(5, 5)));
        // permit regex 10:10
        assertTrue(expr.accept(eval, StandardCommunity.of(10, 10)));
        // permit regex 11:11
        assertTrue(expr.accept(eval, StandardCommunity.of(11, 11)));
      }
      {
        CommunityMatchExpr expr = c.getCommunityMatchExprs().get("cl_test");

        // no single community matched by regex _1:1.*2:2_, so deny line is NOP

        // permit regex _1:1_
        assertTrue(expr.accept(eval, StandardCommunity.of(1, 1)));
        assertFalse(expr.accept(eval, StandardCommunity.of(11, 11)));

        // permit regex _2:2_
        assertTrue(expr.accept(eval, StandardCommunity.of(2, 2)));
      }
    }
    {
      // Test CommunitySetMatchExpr conversion
      assertThat(c.getCommunitySetMatchExprs(), hasKeys("cl_seq", "cl_test"));
      CommunitySetMatchExprEvaluator eval = ctx.getCommunitySetMatchExprEvaluator();
      {
        CommunitySetMatchExpr expr = c.getCommunitySetMatchExprs().get("cl_seq");

        // permit regex 1:1
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(1, 1))));
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(91, 19))));
        assertTrue(
            expr.accept(
                eval, CommunitySet.of(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2))));
        // permit regex 5:5
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(5, 5))));
        // permit regex 10:10
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(10, 10))));
        // permit regex 11:11
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(11, 11))));
      }
      {
        CommunitySetMatchExpr expr = c.getCommunitySetMatchExprs().get("cl_test");

        // deny regex _1:1.*2:2_
        assertFalse(
            expr.accept(
                eval, CommunitySet.of(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2))));
        assertFalse(
            expr.accept(
                eval,
                CommunitySet.of(
                    StandardCommunity.of(1, 1),
                    StandardCommunity.of(2, 2),
                    StandardCommunity.of(3, 3))));

        // permit regex _1:1_
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(1, 1))));
        assertTrue(
            expr.accept(
                eval, CommunitySet.of(StandardCommunity.of(1, 1), StandardCommunity.of(3, 3))));
        assertFalse(expr.accept(eval, CommunitySet.of(StandardCommunity.of(11, 11))));

        // permit regex _2:2_
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(2, 2))));
      }
    }
  }

  @Test
  public void testIpCommunityListExpandedExtraction() {
    String hostname = "nxos_ip_community_list_expanded";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpCommunityLists(), hasKeys("cl_seq", "cl_test"));
    {
      IpCommunityListExpanded cl = (IpCommunityListExpanded) vc.getIpCommunityLists().get("cl_seq");
      Iterator<IpCommunityListExpandedLine> lines = cl.getLines().values().iterator();
      IpCommunityListExpandedLine line;

      line = lines.next();
      assertThat(line.getLine(), equalTo(1L));
      assertThat(line.getRegex(), equalTo("1:1"));

      line = lines.next();
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getRegex(), equalTo("5:5"));

      line = lines.next();
      assertThat(line.getLine(), equalTo(10L));
      assertThat(line.getRegex(), equalTo("10:10"));

      line = lines.next();
      assertThat(line.getLine(), equalTo(11L));
      assertThat(line.getRegex(), equalTo("11:11"));

      assertFalse(lines.hasNext());
    }
    {
      IpCommunityListExpanded cl =
          (IpCommunityListExpanded) vc.getIpCommunityLists().get("cl_test");
      Iterator<IpCommunityListExpandedLine> lines = cl.getLines().values().iterator();
      IpCommunityListExpandedLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getRegex(), equalTo("_1:1.*2:2_"));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo("_1:1_"));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo("_2:2_"));

      assertFalse(lines.hasNext());
    }
  }

  @Test
  public void testIpCommunityListStandardConversion() throws IOException {
    String hostname = "nxos_ip_community_list_standard";
    Configuration c = parseConfig(hostname);
    CommunityContext ctx = CommunityContext.builder().build();

    // Each list should be converted to both a CommunityMatchExpr and a CommunitySetMatchExpr.
    {
      // Test CommunityMatchExpr conversion
      assertThat(c.getCommunityMatchExprs(), hasKeys("cl_seq", "cl_values", "cl_test"));
      CommunityMatchExprEvaluator eval = ctx.getCommunityMatchExprEvaluator();
      {
        CommunityMatchExpr expr = c.getCommunityMatchExprs().get("cl_seq");

        // permit 1:1
        assertTrue(expr.accept(eval, StandardCommunity.of(1, 1)));
        // permit 5:5
        assertTrue(expr.accept(eval, StandardCommunity.of(5, 5)));
        // permit 10:10
        assertTrue(expr.accept(eval, StandardCommunity.of(10, 10)));
        // permit 11:11
        assertTrue(expr.accept(eval, StandardCommunity.of(11, 11)));
      }
      {
        CommunityMatchExpr expr = c.getCommunityMatchExprs().get("cl_values");

        // permit 1:1
        assertTrue(expr.accept(eval, StandardCommunity.of(1, 1)));
        // permit internet
        assertTrue(expr.accept(eval, StandardCommunity.INTERNET));
        // permit local-AS
        assertTrue(expr.accept(eval, StandardCommunity.NO_EXPORT_SUBCONFED));
        // permit no-advertise
        assertTrue(expr.accept(eval, StandardCommunity.NO_ADVERTISE));
        // permit no-export
        assertTrue(expr.accept(eval, StandardCommunity.NO_EXPORT));
      }
      {
        CommunityMatchExpr expr = c.getCommunityMatchExprs().get("cl_test");

        // no single community matched by 1:1 2:2, so deny line is NOP

        // permit 1:1
        assertTrue(expr.accept(eval, StandardCommunity.of(1, 1)));
        // permit 2:2
        assertTrue(expr.accept(eval, StandardCommunity.of(2, 2)));
      }
    }
    {
      // Test CommunitySetMatchExpr conversion
      assertThat(c.getCommunitySetMatchExprs(), hasKeys("cl_seq", "cl_values", "cl_test"));
      CommunitySetMatchExprEvaluator eval = ctx.getCommunitySetMatchExprEvaluator();
      {
        CommunitySetMatchExpr expr = c.getCommunitySetMatchExprs().get("cl_seq");

        // permit 1:1
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(1, 1))));
        assertTrue(
            expr.accept(
                eval, CommunitySet.of(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2))));
        // permit 5:5
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(5, 5))));
        // permit 10:10
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(10, 10))));
        // permit 11:11
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(11, 11))));
      }
      {
        CommunitySetMatchExpr expr = c.getCommunitySetMatchExprs().get("cl_values");

        // permit 1:1
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(1, 1))));
        // permit internet
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.INTERNET)));
        // permit local-AS
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.NO_EXPORT_SUBCONFED)));
        // permit no-advertise
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.NO_ADVERTISE)));
        // permit no-export
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.NO_EXPORT)));
      }
      {
        CommunitySetMatchExpr expr = c.getCommunitySetMatchExprs().get("cl_test");

        // deny 1:1 2:2
        assertFalse(
            expr.accept(
                eval, CommunitySet.of(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2))));
        assertFalse(
            expr.accept(
                eval,
                CommunitySet.of(
                    StandardCommunity.of(1, 1),
                    StandardCommunity.of(2, 2),
                    StandardCommunity.of(3, 3))));

        // permit 1:1
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(1, 1))));
        assertTrue(
            expr.accept(
                eval, CommunitySet.of(StandardCommunity.of(1, 1), StandardCommunity.of(3, 3))));

        // permit 2:2
        assertTrue(expr.accept(eval, CommunitySet.of(StandardCommunity.of(2, 2))));
      }
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
              StandardCommunity.INTERNET,
              StandardCommunity.NO_EXPORT_SUBCONFED,
              StandardCommunity.NO_ADVERTISE,
              StandardCommunity.NO_EXPORT));
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
  public void testIpDomainNameConversion() throws IOException {
    String hostname = "nxos_ip_domain_name";
    Configuration c = parseConfig(hostname);

    assertThat(c.getDomainName(), equalTo("example.com"));
  }

  @Test
  public void testIpDomainNameExtraction() {
    String hostname = "nxos_ip_domain_name";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpDomainName(), equalTo("example.com"));
  }

  @Test
  public void testIpNameServerConversion() throws IOException {
    String hostname = "nxos_ip_name_server";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getDnsServers(),
        containsInAnyOrder(
            "192.0.2.1", "192.0.2.2", "192.0.2.3", "dead:beef::1", "192.0.2.99", "192.0.2.100"));
  }

  @Test
  public void testIpNameServerExtraction() {
    String hostname = "nxos_ip_name_server";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getDefaultVrf().getNameServers(),
        contains(
            new NameServer("192.0.2.2", null),
            new NameServer("192.0.2.1", null),
            new NameServer("dead:beef::1", null),
            new NameServer("192.0.2.3", MANAGEMENT_VRF_NAME)));
    assertThat(
        vc.getVrfs().get("other_vrf").getNameServers(),
        contains(
            new NameServer("192.0.2.99", MANAGEMENT_VRF_NAME),
            new NameServer("192.0.2.100", null)));
    assertThat(vc.getVrfs().get(MANAGEMENT_VRF_NAME).getNameServers(), empty());
  }

  @Test
  public void testIpPimParsing() throws IOException {
    // Assert that it parses.
    parseConfig("nxos_ip_pim");
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
      assertThat(line.getLengthRange(), equalTo(SubRange.singleton(16)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(SubRange.singleton(24)));
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
      assertThat(line.getLengthRange(), equalTo(SubRange.singleton(16)));
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(SubRange.singleton(24)));
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
  public void testIpSla() {
    String hostname = "nxos_ip_sla";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);
    assertThat(vc, notNullValue());
    // TODO: test ip sla extraction
  }

  @Test
  public void testIpv6AccessListConversion() throws IOException {
    Configuration c = parseConfig("nxos_ipv6_access_list");

    assertThat(c.getIp6AccessLists(), hasKeys("v6acl1"));
    // TODO: convert lines
  }

  @Test
  public void testIpv6AccessListExtraction() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_ipv6_access_list");

    assertThat(vc.getIpv6AccessLists(), hasKeys("v6acl1"));
    // TODO: extract lines
  }

  @Test
  public void testIpv6PrefixListConversion() throws IOException {
    String hostname = "nxos_ipv6_prefix_list";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasRoute6FilterLists(hasKeys("pl_empty", "pl_test", "pl_range")));
    {
      Route6FilterList r = c.getRoute6FilterLists().get("pl_empty");
      assertThat(r, Route6FilterListMatchers.hasLines(empty()));
    }
    {
      Route6FilterList r = c.getRoute6FilterLists().get("pl_test");
      Iterator<Route6FilterLine> lines = r.getLines().iterator();
      Route6FilterLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix6.parse("10::3:0/120")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix6.parse("10::1:0/120")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix6.parse("10::2:0/120")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix6.parse("10::4:0/120")));
    }
    {
      Route6FilterList r = c.getRoute6FilterLists().get("pl_range");
      Iterator<Route6FilterLine> lines = r.getLines().iterator();
      Route6FilterLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(112, Prefix6.MAX_PREFIX_LENGTH)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix6.parse("10:10::/112")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(SubRange.singleton(120)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix6.parse("10:10::/112")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(104, Prefix6.MAX_PREFIX_LENGTH)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix6.parse("10:10::/112")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(116, 120)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix6.parse("10:10::/112")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(112, 120)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix6.parse("10:10::/112")));
    }
  }

  @Test
  public void testIpv6PrefixListExtraction() {
    String hostname = "nxos_ipv6_prefix_list";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpv6PrefixLists(), hasKeys("pl_empty", "pl_test", "pl_range"));

    {
      Ipv6PrefixList pl = vc.getIpv6PrefixLists().get("pl_empty");
      assertThat(pl.getDescription(), equalTo("An empty prefix-list"));
      assertThat(pl.getLines(), anEmptyMap());
    }
    {
      Ipv6PrefixList pl = vc.getIpv6PrefixLists().get("pl_test");
      assertThat(pl.getDescription(), nullValue());
      assertThat(pl.getLines().keySet(), contains(3L, 5L, 10L, 15L));
      Iterator<Ipv6PrefixListLine> lines = pl.getLines().values().iterator();
      Ipv6PrefixListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLine(), equalTo(3L));
      assertThat(line.getPrefix6(), equalTo(Prefix6.parse("10::3:0/120")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getPrefix6(), equalTo(Prefix6.parse("10::1:0/120")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getLine(), equalTo(10L));
      assertThat(line.getPrefix6(), equalTo(Prefix6.parse("10::2:0/120")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLine(), equalTo(15L));
      assertThat(line.getPrefix6(), equalTo(Prefix6.parse("10::4:0/120")));
    }
    {
      Ipv6PrefixList pl = vc.getIpv6PrefixLists().get("pl_range");
      assertThat(pl.getDescription(), nullValue());
      assertThat(pl.getLines().keySet(), contains(5L, 10L, 15L, 20L, 25L));
      Iterator<Ipv6PrefixListLine> lines = pl.getLines().values().iterator();
      Ipv6PrefixListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(112, Prefix6.MAX_PREFIX_LENGTH)));
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getPrefix6(), equalTo(Prefix6.parse("10:10::/112")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(SubRange.singleton(120)));
      assertThat(line.getLine(), equalTo(10L));
      assertThat(line.getPrefix6(), equalTo(Prefix6.parse("10:10::/112")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(104, Prefix6.MAX_PREFIX_LENGTH)));
      assertThat(line.getLine(), equalTo(15L));
      assertThat(line.getPrefix6(), equalTo(Prefix6.parse("10:10::/112")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(116, 120)));
      assertThat(line.getLine(), equalTo(20L));
      assertThat(line.getPrefix6(), equalTo(Prefix6.parse("10:10::/112")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(112, 120)));
      assertThat(line.getLine(), equalTo(25L));
      assertThat(line.getPrefix6(), equalTo(Prefix6.parse("10:10::/112")));
    }
  }

  @Test
  public void testLineParsing() throws IOException {
    parseConfig("nxos_line");
    // Don't crash.
  }

  @Test
  public void testLoggingConversion() throws IOException {
    String hostname = "nxos_logging";
    Configuration c = parseConfig(hostname);

    assertThat(c.getLoggingServers(), containsInAnyOrder("192.0.2.1", "192.0.2.2", "192.0.2.3"));
    assertThat(c.getLoggingSourceInterface(), equalTo("loopback0"));
  }

  @Test
  public void testLoggingExtraction() {
    String hostname = "nxos_logging";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getLoggingServers(), hasKeys("192.0.2.1", "192.0.2.2", "192.0.2.3"));
    assertThat(vc.getLoggingSourceInterface(), equalTo("loopback0"));
  }

  @Test
  public void testMacParsing() {
    parseVendorConfig("nxos_mac");
    // Don't crash.
  }

  @Test
  public void testMonitorParsing() {
    String hostname = "nxos_monitor";
    assertThat(parseVendorConfig(hostname), notNullValue()); // todo: move beyond parsing
  }

  @Test
  public void testNexus3000DefaultsConversion() throws IOException {
    String hostname = "nxos_nexus_3000_defaults";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVendorFamily().getCiscoNxos().getMajorVersion(), equalTo(NXOS6));
    assertThat(c.getVendorFamily().getCiscoNxos().getPlatform(), equalTo(NEXUS_3000));
    assertThat(c, hasInterface("Ethernet1/1", isActive()));
  }

  @Test
  public void testNexus3000DefaultsExtraction() {
    String hostname = "nxos_nexus_3000_defaults";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getBootSystemSup1(), equalTo("bootflash:/n3000-uk9.6.0.2.U3.2.bin"));
    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1"));
    assertThat(vc.getInterfaces().get("Ethernet1/1").getShutdown(), nullValue());
  }

  @Test
  public void testNexus5000DefaultsConversion() throws IOException {
    String hostname = "nxos_nexus_5000_defaults";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVendorFamily().getCiscoNxos().getMajorVersion(), equalTo(NXOS5));
    assertThat(c.getVendorFamily().getCiscoNxos().getPlatform(), equalTo(NEXUS_5000));
    assertThat(c, hasInterface("Ethernet1/1", isActive()));
  }

  @Test
  public void testNexus5000DefaultsExtraction() {
    String hostname = "nxos_nexus_5000_defaults";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getBootSystemSup1(), equalTo("bootflash:/n5000-uk9.5.1.3.N1.1a.bin"));
    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1"));
    assertThat(vc.getInterfaces().get("Ethernet1/1").getShutdown(), nullValue());
  }

  @Test
  public void testNexus6000DefaultsConversion() throws IOException {
    String hostname = "nxos_nexus_6000_defaults";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVendorFamily().getCiscoNxos().getMajorVersion(), equalTo(NXOS6));
    assertThat(c.getVendorFamily().getCiscoNxos().getPlatform(), equalTo(NEXUS_6000));
    assertThat(c, hasInterface("Ethernet1/1", isActive()));
  }

  @Test
  public void testNexus6000DefaultsExtraction() {
    String hostname = "nxos_nexus_6000_defaults";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getBootSystemSup1(), equalTo("bootflash:/n6000-uk9.6.0.2.N2.3.bin"));
    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1"));
    assertThat(vc.getInterfaces().get("Ethernet1/1").getShutdown(), nullValue());
  }

  @Test
  public void testNexusUnknownDefaultsConversion() throws IOException {
    String hostname = "nxos_nexus_unknown_defaults";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVendorFamily().getCiscoNxos().getPlatform(), equalTo(NexusPlatform.UNKNOWN));
    assertThat(c, hasInterface("Ethernet1/1", isActive(true)));
  }

  @Test
  public void testNexusUnknownDefaultsExtraction() {
    String hostname = "nxos_nexus_unknown_defaults";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getBootSystemSup1(), nullValue());
    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1"));
    assertThat(vc.getInterfaces().get("Ethernet1/1").getShutdown(), nullValue());
  }

  @Test
  public void testNtpConversion() throws IOException {
    Configuration c = parseConfig("nxos_ntp");

    assertThat(
        c.getNtpServers(), containsInAnyOrder("192.0.2.1", "192.0.2.2", "192.0.2.3", "192.0.2.4"));
    assertThat(c.getNtpSourceInterface(), equalTo("mgmt0"));
  }

  @Test
  public void testNtpExtraction() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_ntp");

    assertThat(vc.getNtpServers(), hasKeys("192.0.2.1", "192.0.2.2", "192.0.2.3", "192.0.2.4"));
    {
      NtpServer ntpServer = vc.getNtpServers().get("192.0.2.1");
      assertFalse(ntpServer.getPrefer());
      assertThat(ntpServer.getUseVrf(), nullValue());
    }
    {
      NtpServer ntpServer = vc.getNtpServers().get("192.0.2.2");
      assertFalse(ntpServer.getPrefer());
      assertThat(ntpServer.getUseVrf(), nullValue());
    }
    {
      NtpServer ntpServer = vc.getNtpServers().get("192.0.2.3");
      assertFalse(ntpServer.getPrefer());
      assertThat(ntpServer.getUseVrf(), equalTo("management"));
    }
    {
      NtpServer ntpServer = vc.getNtpServers().get("192.0.2.4");
      assertTrue(ntpServer.getPrefer());
      assertThat(ntpServer.getUseVrf(), nullValue());
    }

    assertThat(vc.getNtpSourceInterface(), equalTo("mgmt0"));
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

    assertThat(c, hasDefaultVrf(hasL2VniSettings(hasKey(10001))));
    assertThat(
        c.getDefaultVrf().getLayer2Vnis().get(10001),
        allOf(
            hasBumTransportIps(equalTo(ImmutableSortedSet.of(Ip.parse("235.0.0.0")))),
            hasBumTransportMethod(equalTo(BumTransportMethod.MULTICAST_GROUP)),
            hasSourceAddress(nullValue()),
            hasUdpPort(equalTo(DEFAULT_UDP_PORT)),
            VniSettingsMatchers.hasVlan(equalTo(2)),
            hasVni(10001)));

    String tenant1 = "tenant1"; // 20001 is an L3 VNI so it should be mapped to a VRF
    assertThat(c.getVrfs().get(tenant1).getLayer3Vnis().get(20001), notNullValue());
    assertThat(
        c.getVrfs().get(tenant1).getLayer3Vnis().get(20001),
        allOf(
            // L3 mcast IP
            hasBumTransportIps(equalTo(ImmutableSortedSet.of(Ip.parse("234.0.0.0")))),
            hasBumTransportMethod(equalTo(BumTransportMethod.MULTICAST_GROUP)),
            hasSourceAddress(nullValue()),
            hasUdpPort(equalTo(DEFAULT_UDP_PORT)),
            hasVni(20001)));

    assertThat(c, hasDefaultVrf(hasL2VniSettings(hasKey(30001))));
    assertThat(
        c.getDefaultVrf().getLayer2Vnis().get(30001),
        allOf(
            // L2 mcast IP
            hasBumTransportIps(equalTo(ImmutableSortedSet.of(Ip.parse("233.0.0.0")))),
            hasBumTransportMethod(equalTo(BumTransportMethod.MULTICAST_GROUP)),
            hasSourceAddress(nullValue()),
            hasUdpPort(equalTo(DEFAULT_UDP_PORT)),
            VniSettingsMatchers.hasVlan(equalTo(4)),
            hasVni(30001)));

    assertThat(c, hasDefaultVrf(hasL2VniSettings(hasKey(40001))));
    assertThat(
        c.getDefaultVrf().getLayer2Vnis().get(40001),
        allOf(
            hasBumTransportIps(equalTo(ImmutableSortedSet.of(Ip.parse("4.0.0.1")))),
            hasBumTransportMethod(equalTo(BumTransportMethod.UNICAST_FLOOD_GROUP)),
            hasSourceAddress(equalTo(Ip.parse("1.1.1.1"))),
            hasUdpPort(equalTo(DEFAULT_UDP_PORT)),
            VniSettingsMatchers.hasVlan(equalTo(5)),
            hasVni(40001)));

    // VLAN for VNI 500001 is shutdown
    assertThat(c, not(hasDefaultVrf(hasL2VniSettings(hasKey(50001)))));
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
      assertThat(line.getIpWildcard(), equalTo(IpWildcard.parse("10.0.0.1")));

      line = lines.next();
      assertThat(
          line.getIpWildcard(),
          equalTo(ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.255.0.255"))));

      line = lines.next();
      assertThat(line.getIpWildcard(), equalTo(IpWildcard.parse("10.0.0.0/24")));
    }
  }

  @Test
  public void testObjectGroupIpAddressReferences() throws IOException {
    String hostname = "nxos_object_group_ip_address_references";
    String filename = String.format("configs/%s", hostname);
    Batfish bf = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        bf.loadConvertConfigurationAnswerElementOrReparse(bf.getSnapshot());
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
          ipSpace.accept(_srcIpBdd),
          equalTo(
              AclIpSpace.union(
                      Ip.parse("10.0.0.1").toIpSpace(),
                      ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.255.0.255")).toIpSpace(),
                      Prefix.parse("10.0.0.0/24").toIpSpace())
                  .accept(_srcIpBdd)));
    }
  }

  @Test
  public void testObjectGroupIpPortExtraction() {
    String hostname = "nxos_object_group_ip_port";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getObjectGroups(), hasKeys("og_indices", "og_syntax"));
    {
      ObjectGroupIpPort group = (ObjectGroupIpPort) vc.getObjectGroups().get("og_indices");
      assertThat(group.getLines(), hasKeys(10L, 13L, 15L, 25L));
      assertThat(
          group.getLines().values().stream()
              .map(ObjectGroupIpPortLine::getLine)
              .collect(ImmutableList.toImmutableList()),
          contains(10L, 13L, 15L, 25L));
    }
    {
      ObjectGroupIpPort group = (ObjectGroupIpPort) vc.getObjectGroups().get("og_syntax");
      Iterator<ObjectGroupIpPortLine> lines = group.getLines().values().iterator();
      ObjectGroupIpPortLine line = lines.next();
      assertThat(line.getPorts(), equalTo(IntegerSpace.of(10)));
      line = lines.next();
      assertThat(line.getPorts(), equalTo(IntegerSpace.of(Range.closed(0, 49))));
      line = lines.next();
      assertThat(line.getPorts(), equalTo(IntegerSpace.of(Range.closed(51, 65535))));
      line = lines.next();
      assertThat(
          line.getPorts(),
          equalTo(IntegerSpace.of(Range.closed(0, 65535)).difference(IntegerSpace.of(7))));
      line = lines.next();
      assertThat(line.getPorts(), equalTo(IntegerSpace.of(Range.closed(5, 7))));
      line = lines.next();
      assertThat(line.getPorts(), equalTo(IntegerSpace.of(Range.closed(6, 8))));
      assertFalse(lines.hasNext());
    }
  }

  @Test
  public void testObjectGroupIpPortConversion() throws IOException {
    parseConfig("nxos_object_group_ip_port");
    // Conversion is actually tested in the IP Access List tests, just make sure no crashes.
  }

  @Test
  public void testNxosOspfCostLoopback() throws IOException {
    Configuration c = parseConfig("nxos-ospf-cost-loopback");

    assertThat(c.getAllInterfaces().get("loopback61").getOspfSettings(), not(nullValue()));
    assertThat(c.getAllInterfaces().get("loopback61").getOspfSettings().getCost(), equalTo(1));
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
            "a_nssa_dio",
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
            "distance",
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
            "parse_only",
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
    // TODO: convert and test "a_nssa_dio" - OSPF NSSA default-information-originate
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
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("a_r");
      Prefix prefix = Prefix.parse("1.1.1.1/32");
      Map<Prefix, OspfAreaSummary> summaries = proc.getAreas().get(0L).getSummaries();
      assertThat(summaries, hasKeys(prefix));

      OspfAreaSummary summary = summaries.get(prefix);
      assertThat(summary.getBehavior(), is(SummaryRouteBehavior.ADVERTISE_AND_INSTALL_DISCARD));
      assertThat(summary.getMetric(), nullValue());
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("a_r_cost");
      Prefix prefix = Prefix.parse("1.1.1.1/32");
      Map<Prefix, OspfAreaSummary> summaries = proc.getAreas().get(0L).getSummaries();
      assertThat(summaries, hasKeys(prefix));

      OspfAreaSummary summary = summaries.get(prefix);
      assertThat(summary.getBehavior(), is(SummaryRouteBehavior.ADVERTISE_AND_INSTALL_DISCARD));
      assertThat(summary.getMetric(), equalTo(5L));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc =
          defaultVrf.getOspfProcesses().get("a_r_not_advertise");
      Prefix prefix = Prefix.parse("1.1.1.1/32");
      Map<Prefix, OspfAreaSummary> summaries = proc.getAreas().get(0L).getSummaries();
      assertThat(summaries, hasKeys(prefix));

      OspfAreaSummary summary = summaries.get(prefix);
      assertThat(summary.getBehavior(), is(SummaryRouteBehavior.NOT_ADVERTISE_AND_NO_DISCARD));
      assertThat(summary.getMetric(), nullValue());
    }
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
          org.batfish.datamodel.StaticRoute.testBuilder()
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
                .process(staticInputRoute, outputRoute, Direction.OUT));
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
                .process(staticInputRoute, OspfExternalRoute.builder(), Direction.OUT));
        // accept generated route
        assertTrue(
            c.getRoutingPolicies()
                .get(proc.getExportPolicy())
                .process(generatedInputRoute, outputRoute, Direction.OUT));
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
                .process(staticInputRoute, outputRoute, Direction.OUT));
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
                .process(staticInputRoute, OspfExternalRoute.builder(), Direction.OUT));
        // accept generated route
        assertTrue(
            c.getRoutingPolicies()
                .get(proc.getExportPolicy())
                .process(generatedInputRoute, outputRoute, Direction.OUT));
        // assign E2 metric-type from route-map
        assertThat(outputRoute.build().getOspfMetricType(), equalTo(OspfMetricType.E2));
      }
      {
        org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("distance");
        assertTrue(proc.getAdminCosts().values().stream().allMatch(i -> i.equals(243)));
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
              0,
              OspfAreaMatchers.hasInterfaces(
                  containsInAnyOrder("Ethernet1/2", "Ethernet1/3", "Ethernet1/5"))));
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
              .process(inputRoute, outputRoute, Direction.OUT));
      assertThat(outputRoute.build().getOspfMetricType(), equalTo(OspfMetricType.E1));
    }
    // TODO: convert and test OSPF redistribute maximum-prefix
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("r_static");
      // can redistribute static default route on NX-OS
      org.batfish.datamodel.StaticRoute inputRoute =
          org.batfish.datamodel.StaticRoute.testBuilder()
              .setAdmin(1)
              .setNetwork(Prefix.ZERO)
              .setNextHopInterface("dummy")
              .build();
      OspfExternalRoute.Builder outputRoute = ospfExternalRouteBuilder().setNetwork(Prefix.ZERO);
      assertTrue(
          c.getRoutingPolicies()
              .get(proc.getExportPolicy())
              .process(inputRoute, outputRoute, Direction.OUT));
      assertThat(outputRoute.build().getOspfMetricType(), equalTo(OspfMetricType.E1));
    }
    {
      org.batfish.datamodel.ospf.OspfProcess proc = defaultVrf.getOspfProcesses().get("router_id");
      assertThat(proc.getRouterId(), equalTo(Ip.parse("192.0.2.1")));
      assertThat(proc, hasArea(0L, OspfAreaMatchers.hasInterfaces(contains("Ethernet1/4"))));
    }
    // TODO: convert and test "sa" - OSPF summary-address
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
        c.getAllInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3", "Ethernet1/4", "Ethernet1/5"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(iface.getOspfCost(), equalTo(12));
      assertTrue(iface.getOspfEnabled());
      assertThat(iface.getOspfAreaName(), equalTo(0L));
      // TODO: convert and test bfd
      assertTrue(iface.getOspfPassive());
      assertThat(iface.getOspfNetworkType(), nullValue());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertTrue(iface.getOspfEnabled());
      assertThat(iface.getOspfAreaName(), equalTo(0L));
      assertFalse(iface.getOspfPassive());
      assertThat(iface.getOspfNetworkType(), equalTo(OspfNetworkType.BROADCAST));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      assertTrue(iface.getOspfEnabled());
      assertThat(iface.getOspfAreaName(), equalTo(0L));
      assertFalse(iface.getOspfPassive());
      assertThat(iface.getOspfNetworkType(), equalTo(OspfNetworkType.POINT_TO_POINT));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertTrue(iface.getOspfEnabled());
      assertThat(iface.getOspfAreaName(), equalTo(0L));
      assertTrue(iface.getOspfPassive());
      assertThat(iface.getOspfNetworkType(), nullValue());
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
            "a_nssa_dio",
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
            "distance",
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
            "parse_only",
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
      assertFalse(nssa.getDefaultInformationOriginate());
      assertFalse(nssa.getNoRedistribution());
      assertFalse(nssa.getNoSummary());
      assertThat(nssa.getDefaultInformationOriginateMap(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_nssa_dio");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaNssa nssa = (OspfAreaNssa) proc.getAreas().get(1L).getTypeSettings();
      assertThat(nssa, notNullValue());
      assertTrue(nssa.getDefaultInformationOriginate());
      assertFalse(nssa.getNoRedistribution());
      assertFalse(nssa.getNoSummary());
      assertThat(nssa.getDefaultInformationOriginateMap(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_nssa_no_r");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaNssa nssa = (OspfAreaNssa) proc.getAreas().get(1L).getTypeSettings();
      assertThat(nssa, notNullValue());
      assertFalse(nssa.getDefaultInformationOriginate());
      assertTrue(nssa.getNoRedistribution());
      assertFalse(nssa.getNoSummary());
      assertThat(nssa.getDefaultInformationOriginateMap(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_nssa_no_s");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaNssa nssa = (OspfAreaNssa) proc.getAreas().get(1L).getTypeSettings();
      assertThat(nssa, notNullValue());
      assertFalse(nssa.getDefaultInformationOriginate());
      assertFalse(nssa.getNoRedistribution());
      assertTrue(nssa.getNoSummary());
      assertThat(nssa.getDefaultInformationOriginateMap(), nullValue());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("a_nssa_rm");
      assertThat(proc.getAreas(), hasKeys(1L));
      OspfAreaNssa nssa = (OspfAreaNssa) proc.getAreas().get(1L).getTypeSettings();
      assertThat(nssa, notNullValue());
      assertTrue(nssa.getDefaultInformationOriginate());
      assertFalse(nssa.getNoRedistribution());
      assertFalse(nssa.getNoSummary());
      assertThat(nssa.getDefaultInformationOriginateMap(), equalTo("rm1"));
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
    {
      OspfProcess proc = vc.getOspfProcesses().get("distance");
      assertThat(proc.getDistance(), equalTo(243));
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
                  0L,
                  IpWildcard.create(Prefix.strict("172.16.2.0/24")),
                  0L)));
      assertFalse(proc.getPassiveInterfaceDefault());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("pi_d");
      assertTrue(proc.getPassiveInterfaceDefault());
    }
    {
      OspfProcess proc = vc.getOspfProcesses().get("r_direct");
      assertThat(proc.getRedistributionPolicies(), hasSize(1));
      assertThat(
          proc.getRedistributionPolicy(RoutingProtocolInstance.direct()),
          equalTo(new RedistributionPolicy(RoutingProtocolInstance.direct(), "rm1")));
    }
    // TODO: extract and test redistribute maximum-prefix
    {
      OspfProcess proc = vc.getOspfProcesses().get("r_static");
      assertThat(proc.getRedistributionPolicies(), hasSize(1));
      assertThat(
          proc.getRedistributionPolicy(RoutingProtocolInstance.staticc()),
          equalTo(new RedistributionPolicy(RoutingProtocolInstance.staticc(), "rm1")));
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
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3", "Ethernet1/4", "Ethernet1/5"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      OspfInterface ospf = iface.getOspf();
      assertThat(ospf, notNullValue());
      // TODO: extract and test authentication message-digest
      // TODO: extract and test message-digest-key
      assertTrue(ospf.getBfd());
      assertThat(ospf.getCost(), equalTo(12));
      assertThat(ospf.getDeadIntervalS(), equalTo(10));
      assertThat(ospf.getHelloIntervalS(), equalTo(20));
      assertThat(ospf.getPassive(), nullValue());
      assertThat(ospf.getPriority(), equalTo(10));
      assertThat(ospf.getProcess(), equalTo("pi_d"));
      assertThat(ospf.getArea(), equalTo(0L));
      assertThat(ospf.getNetwork(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      OspfInterface ospf = iface.getOspf();
      assertThat(ospf, notNullValue());
      // TODO: extract and test authentication key-chain
      assertFalse(ospf.getBfd());
      assertThat(ospf.getCost(), nullValue());
      assertThat(ospf.getDeadIntervalS(), nullValue());
      assertThat(ospf.getHelloIntervalS(), nullValue());
      assertThat(ospf.getPassive(), nullValue());
      assertThat(ospf.getPriority(), nullValue());
      assertThat(ospf.getProcess(), nullValue());
      assertThat(ospf.getArea(), nullValue());
      assertThat(ospf.getNetwork(), equalTo(BROADCAST));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      OspfInterface ospf = iface.getOspf();
      // TODO: extract and test authentication null (disabled)
      assertThat(ospf, notNullValue());
      assertThat(ospf.getPassive(), equalTo(false));
      assertThat(ospf.getNetwork(), equalTo(POINT_TO_POINT));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      OspfInterface ospf = iface.getOspf();
      assertThat(ospf, notNullValue());
      // TODO: discover semantics, extract and test 'authentication' with no params
      assertThat(ospf.getPassive(), equalTo(true));
      assertThat(ospf.getNetwork(), nullValue());
    }
  }

  @Test
  public void testOspfv3Extraction() {
    // TODO: make into extraction test
    assertThat(parseVendorConfig("nxos_ospfv3"), notNullValue());
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
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getChannelGroup(), equalTo("port-channel3"));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertThat(iface.getShutdown(), nullValue());
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
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
      Lacp lacp = iface.getLacp();
      assertThat(lacp, notNullValue());
      assertThat(lacp.getMinLinks(), equalTo(2));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel2");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel3");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel4");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel5");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel6");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getSwitchportMode(), nullValue());
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
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertThat(iface.getShutdown(), nullValue());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
  }

  @Test
  public void testPortChannelReferences() throws IOException {
    String hostname = "nxos_port_channel_references";
    String filename = String.format("configs/%s", hostname);
    Batfish bf = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        bf.loadConvertConfigurationAnswerElementOrReparse(bf.getSnapshot());
    assertThat(
        ans, hasNumReferrers(filename, CiscoNxosStructureType.PORT_CHANNEL, "port-channel1", 1));
    assertThat(
        ans, hasNumReferrers(filename, CiscoNxosStructureType.PORT_CHANNEL, "port-channel2", 0));
  }

  @Test
  public void testPortChannelSubinterfaceConversion() throws IOException {
    String hostname = "port_channel_subinterface";
    Configuration c = parseConfig(hostname);
    org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel1.1");
    assertThat(iface, isActive());
    assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATE_CHILD));
    // Should inherit bandwidth from parent portchannel
    assertThat(iface, hasBandwidth(200E6));
    assertThat(
        iface, hasDependencies(contains(new Dependency("port-channel1", DependencyType.BIND))));
    assertThat(iface, hasChannelGroupMembers(empty()));
  }

  @Test
  public void testRipParsing() throws IOException {
    parseConfig("nxos_rip");
    // don't crash.
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
            "empty_pbr_statistics", // really empty, it has no first term
            "empty_deny",
            computeRoutingPolicyName("empty_deny", 10),
            "empty_permit",
            computeRoutingPolicyName("empty_permit", 10),
            "match_as_number",
            computeRoutingPolicyName("match_as_number", 10),
            "match_as_path",
            computeRoutingPolicyName("match_as_path", 10),
            "match_community_standard",
            computeRoutingPolicyName("match_community_standard", 10),
            "match_community_expanded",
            computeRoutingPolicyName("match_community_expanded", 10),
            "match_interface",
            computeRoutingPolicyName("match_interface", 10),
            "match_ip_address",
            computeRoutingPolicyName("match_ip_address", 10),
            "match_ip_address_prefix_list",
            computeRoutingPolicyName("match_ip_address_prefix_list", 10),
            "match_ipv6_address",
            computeRoutingPolicyName("match_ipv6_address", 10),
            "match_ipv6_address_prefix_list",
            computeRoutingPolicyName("match_ipv6_address_prefix_list", 10),
            "match_metric",
            computeRoutingPolicyName("match_metric", 10),
            "match_route_type_external",
            computeRoutingPolicyName("match_route_type_external", 10),
            "match_route_type_internal",
            computeRoutingPolicyName("match_route_type_internal", 10),
            "match_route_type_local",
            computeRoutingPolicyName("match_route_type_local", 10),
            "match_route_type_nssa_external",
            computeRoutingPolicyName("match_route_type_nssa_external", 10),
            "match_route_type_type_1",
            computeRoutingPolicyName("match_route_type_type_1", 10),
            "match_route_type_type_2",
            computeRoutingPolicyName("match_route_type_type_2", 10),
            "match_route_types",
            computeRoutingPolicyName("match_route_types", 10),
            "match_route_types_unsupported",
            computeRoutingPolicyName("match_route_types_unsupported", 10),
            "match_source_protocol_connected",
            computeRoutingPolicyName("match_source_protocol_connected", 10),
            "match_source_protocol_static",
            computeRoutingPolicyName("match_source_protocol_static", 10),
            "match_tag",
            computeRoutingPolicyName("match_tag", 10),
            "match_vlan",
            computeRoutingPolicyName("match_vlan", 10),
            "set_as_path_prepend_last_as",
            computeRoutingPolicyName("set_as_path_prepend_last_as", 10),
            "set_as_path_prepend_literal_as",
            computeRoutingPolicyName("set_as_path_prepend_literal_as", 10),
            "set_comm_list_expanded",
            computeRoutingPolicyName("set_comm_list_expanded", 10),
            "set_comm_list_standard",
            computeRoutingPolicyName("set_comm_list_standard", 10),
            "set_comm_list_standard_single",
            computeRoutingPolicyName("set_comm_list_standard_single", 10),
            "set_community",
            computeRoutingPolicyName("set_community", 10),
            "set_community_additive",
            computeRoutingPolicyName("set_community_additive", 10),
            "set_ip_next_hop_literal",
            computeRoutingPolicyName("set_ip_next_hop_literal", 10),
            "set_ip_next_hop_literal2",
            computeRoutingPolicyName("set_ip_next_hop_literal2", 10),
            "set_ip_next_hop_unchanged",
            computeRoutingPolicyName("set_ip_next_hop_unchanged", 10),
            "set_ipv6_next_hop_unchanged",
            computeRoutingPolicyName("set_ipv6_next_hop_unchanged", 10),
            "set_local_preference",
            computeRoutingPolicyName("set_local_preference", 10),
            "set_metric",
            computeRoutingPolicyName("set_metric", 10),
            "set_metric_eigrp",
            computeRoutingPolicyName("set_metric_eigrp", 10),
            "set_metric_type_external",
            computeRoutingPolicyName("set_metric_type_external", 10),
            "set_metric_type_internal",
            computeRoutingPolicyName("set_metric_type_internal", 10),
            "set_metric_type_type_1",
            computeRoutingPolicyName("set_metric_type_type_1", 10),
            "set_metric_type_type_2",
            computeRoutingPolicyName("set_metric_type_type_2", 10),
            "set_origin_egp",
            computeRoutingPolicyName("set_origin_egp", 10),
            "set_origin_igp",
            computeRoutingPolicyName("set_origin_igp", 10),
            "set_origin_incomplete",
            computeRoutingPolicyName("set_origin_incomplete", 10),
            "set_tag",
            computeRoutingPolicyName("set_tag", 10),
            "set_weight",
            computeRoutingPolicyName("set_weight", 10),
            "match_undefined_access_list",
            computeRoutingPolicyName("match_undefined_access_list", 10),
            "match_undefined_community_list",
            computeRoutingPolicyName("match_undefined_community_list", 10),
            "match_undefined_prefix_list",
            computeRoutingPolicyName("match_undefined_prefix_list", 10),
            "continue_skip_deny",
            computeRoutingPolicyName("continue_skip_deny", 10),
            computeRoutingPolicyName("continue_skip_deny", 30),
            "continue_from_deny_to_permit",
            computeRoutingPolicyName("continue_from_deny_to_permit", 10),
            computeRoutingPolicyName("continue_from_deny_to_permit", 20),
            "continue_from_permit_to_fall_off",
            computeRoutingPolicyName("continue_from_permit_to_fall_off", 10),
            computeRoutingPolicyName("continue_from_permit_to_fall_off", 20),
            "continue_from_permit_and_set_to_fall_off",
            computeRoutingPolicyName("continue_from_permit_and_set_to_fall_off", 10),
            computeRoutingPolicyName("continue_from_permit_and_set_to_fall_off", 20),
            "continue_from_set_to_match_on_set_field",
            computeRoutingPolicyName("continue_from_set_to_match_on_set_field", 10),
            computeRoutingPolicyName("continue_from_set_to_match_on_set_field", 20),
            "reach_continue_target_without_match",
            computeRoutingPolicyName("reach_continue_target_without_match", 10),
            computeRoutingPolicyName("reach_continue_target_without_match", 30)));
    Ip origNextHopIp = Ip.parse("192.0.2.254");
    Bgpv4Route base =
        Bgpv4Route.testBuilder()
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
      RoutingPolicy rp = c.getRoutingPolicies().get("match_as_number");
      assertRoutingPolicyPermitsRoute(rp, base);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_as_path");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route route = base.toBuilder().setAsPath(AsPath.ofSingletonAsSets(1L)).build();
      assertRoutingPolicyPermitsRoute(rp, route);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_community_standard");
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
      RoutingPolicy rp = c.getRoutingPolicies().get("match_community_expanded");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route routeOnlyOneCommunity =
          base.toBuilder()
              .setCommunities(ImmutableSet.of(StandardCommunity.of(64512, 39999)))
              .build();
      assertRoutingPolicyPermitsRoute(rp, routeOnlyOneCommunity);
      // TODO: fix community matching semantics and test matching of regex involving multiple
      // communities
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_interface");
      // TODO Should deny base route after implementing next hop matching (right now permits all).
      //  https://github.com/batfish/batfish/issues/6502
      //      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route routeNextHopIp = base.toBuilder().setNextHopIp(Ip.parse("192.0.2.1")).build();
      assertRoutingPolicyPermitsRoute(rp, routeNextHopIp);
      Bgpv4Route routeNextHopIface = base.toBuilder().setNextHopInterface("loopback0").build();
      assertRoutingPolicyPermitsRoute(rp, routeNextHopIface);
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
      RoutingPolicy rp = c.getRoutingPolicies().get("match_route_type_external");
      assertRoutingPolicyDeniesRoute(rp, new ConnectedRoute(Prefix.ZERO, "dummy"));
      assertRoutingPolicyPermitsRoute(
          rp,
          ospfExternalRouteBuilder()
              .setNetwork(Prefix.ZERO)
              .setOspfMetricType(OspfMetricType.E1)
              .build());
      assertRoutingPolicyPermitsRoute(
          rp,
          ospfExternalRouteBuilder()
              .setNetwork(Prefix.ZERO)
              .setOspfMetricType(OspfMetricType.E2)
              .build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_route_type_internal");
      assertRoutingPolicyDeniesRoute(rp, new ConnectedRoute(Prefix.ZERO, "dummy"));
      assertRoutingPolicyPermitsRoute(rp, ospfRouteBuilder().setNetwork(Prefix.ZERO).build());
      assertRoutingPolicyPermitsRoute(rp, ospfIARouteBuilder().setNetwork(Prefix.ZERO).build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_route_type_local");
      assertRoutingPolicyDeniesRoute(rp, new ConnectedRoute(Prefix.ZERO, "dummy"));
      assertRoutingPolicyPermitsRoute(
          rp,
          LocalRoute.builder()
              .setNextHop(NextHopInterface.of("iface"))
              .setNetwork(Prefix.parse("1.2.3.4/32"))
              .setSourcePrefixLength(24)
              .build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_route_type_nssa_external");
      // Should be FALSE.
      assertRoutingPolicyDeniesRoute(rp, new ConnectedRoute(Prefix.ZERO, "dummy"));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_route_type_type_1");
      assertRoutingPolicyDeniesRoute(rp, new ConnectedRoute(Prefix.ZERO, "dummy"));
      assertRoutingPolicyPermitsRoute(
          rp,
          ospfExternalRouteBuilder()
              .setNetwork(Prefix.ZERO)
              .setOspfMetricType(OspfMetricType.E1)
              .build());
      assertRoutingPolicyDeniesRoute(
          rp,
          ospfExternalRouteBuilder()
              .setNetwork(Prefix.ZERO)
              .setOspfMetricType(OspfMetricType.E2)
              .build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_route_type_type_2");
      assertRoutingPolicyDeniesRoute(rp, new ConnectedRoute(Prefix.ZERO, "dummy"));
      assertRoutingPolicyDeniesRoute(
          rp,
          ospfExternalRouteBuilder()
              .setNetwork(Prefix.ZERO)
              .setOspfMetricType(OspfMetricType.E1)
              .build());
      assertRoutingPolicyPermitsRoute(
          rp,
          ospfExternalRouteBuilder()
              .setNetwork(Prefix.ZERO)
              .setOspfMetricType(OspfMetricType.E2)
              .build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_route_types");
      assertRoutingPolicyDeniesRoute(rp, new ConnectedRoute(Prefix.ZERO, "dummy"));
      assertRoutingPolicyDeniesRoute(rp, ospfRouteBuilder().setNetwork(Prefix.ZERO).build());
      assertRoutingPolicyDeniesRoute(rp, ospfIARouteBuilder().setNetwork(Prefix.ZERO).build());
      assertRoutingPolicyPermitsRoute(
          rp,
          ospfExternalRouteBuilder()
              .setNetwork(Prefix.ZERO)
              .setOspfMetricType(OspfMetricType.E1)
              .build());
      assertRoutingPolicyPermitsRoute(
          rp,
          ospfExternalRouteBuilder()
              .setNetwork(Prefix.ZERO)
              .setOspfMetricType(OspfMetricType.E2)
              .build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_route_types_unsupported");
      // Even though the policy has type-1, since it also has nssa-external it's unmatchable
      assertRoutingPolicyDeniesRoute(
          rp,
          ospfExternalRouteBuilder()
              .setNetwork(Prefix.ZERO)
              .setOspfMetricType(OspfMetricType.E1)
              .build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_source_protocol_connected");
      assertRoutingPolicyDeniesRoute(
          rp,
          org.batfish.datamodel.StaticRoute.testBuilder()
              .setAdmin(1)
              .setNetwork(Prefix.ZERO)
              .setNextHopIp(Ip.ZERO)
              .build());
      assertRoutingPolicyPermitsRoute(rp, new ConnectedRoute(Prefix.ZERO, "dummy"));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_source_protocol_static");
      assertRoutingPolicyDeniesRoute(rp, new ConnectedRoute(Prefix.ZERO, "dummy"));
      assertRoutingPolicyPermitsRoute(
          rp,
          org.batfish.datamodel.StaticRoute.testBuilder()
              .setAdmin(1)
              .setNetwork(Prefix.ZERO)
              .setNextHopIp(Ip.ZERO)
              .build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("match_tag");
      assertRoutingPolicyDeniesRoute(rp, base);
      Bgpv4Route route = base.toBuilder().setTag(1L).build();
      assertRoutingPolicyPermitsRoute(rp, route);
    }
    {
      // match vlan is for pbr, doesn't apply for toBooleanExpr
      RoutingPolicy rp = c.getRoutingPolicies().get("match_vlan");
      assertRoutingPolicyPermitsRoute(rp, base);
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
      RoutingPolicy rp = c.getRoutingPolicies().get("set_comm_list_expanded");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.of(64512, 30000)))
              .build();

      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(route.getCommunities().getCommunities(), contains(StandardCommunity.of(1, 1)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_comm_list_standard");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2)))
              .build();

      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities().getCommunities(),
          contains(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_comm_list_standard_single");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2)))
              .build();

      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(route.getCommunities().getCommunities(), contains(StandardCommunity.of(2, 2)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_community");
      Bgpv4Route inRoute =
          base.toBuilder()
              .setCommunities(
                  ImmutableSet.of(StandardCommunity.of(3, 3), ExtendedCommunity.target(1L, 1L)))
              .build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      // Standard communities should be replaced, while extended communities should be preserved.
      assertThat(
          route,
          hasCommunities(
              StandardCommunity.of(1, 1),
              StandardCommunity.of(1, 2),
              ExtendedCommunity.target(1L, 1L)));
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_community_additive");
      Bgpv4Route inRoute =
          base.toBuilder().setCommunities(ImmutableSet.of(StandardCommunity.of(3, 3))).build();
      Bgpv4Route route = processRouteIn(rp, inRoute);
      assertThat(
          route.getCommunities().getCommunities(),
          containsInAnyOrder(
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
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_metric_eigrp");

      EigrpMetric originalMetric =
          ClassicMetric.builder()
              .setValues(EigrpMetricValues.builder().setBandwidth(2e9).setDelay(4e5).build())
              .build();
      EigrpRoute routeBefore =
          EigrpInternalRoute.testBuilder()
              .setAdmin(90)
              .setNetwork(Prefix.ZERO)
              .setEigrpMetric(originalMetric)
              .setEigrpMetricVersion(EigrpMetricVersion.V2)
              .setProcessAsn(1L)
              .build();
      EigrpExternalRoute.Builder builder =
          EigrpExternalRoute.testBuilder()
              .setAdmin(90)
              .setNetwork(Prefix.ZERO)
              .setEigrpMetricVersion(EigrpMetricVersion.V2)
              .setDestinationAsn(1L)
              .setProcessAsn(1L)
              .setNetwork(Prefix.ZERO);
      assertTrue(
          rp.process(
              routeBefore,
              builder,
              EigrpProcess.builder()
                  .setAsNumber(1L)
                  .setMode(EigrpProcessMode.CLASSIC)
                  .setMetricVersion(EigrpMetricVersion.V2)
                  .setRouterId(Ip.ZERO)
                  .build(),
              Direction.IN));
      EigrpExternalRoute routAfter = builder.build();
      assertThat(routAfter.getEigrpMetric().getValues().getBandwidth(), equalTo(1L));
      assertThat(routAfter.getEigrpMetric().getValues().getDelay(), equalTo((long) 2e7));
      assertThat(routAfter.getEigrpMetric().getValues().getReliability(), equalTo(3));
      assertThat(routAfter.getEigrpMetric().getValues().getEffectiveBandwidth(), equalTo(4));
      assertThat(routAfter.getEigrpMetric().getValues().getMtu(), equalTo(5L));
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
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("set_weight");
      Bgpv4Route route = processRouteIn(rp, base);
      assertThat(route.getWeight(), equalTo(1));
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

    // continue route-maps
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("continue_skip_deny");
      // should permit everything
      assertRoutingPolicyPermitsRoute(rp, base);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("continue_from_deny_to_permit");
      // TODO: verify
      // should permit everything
      assertRoutingPolicyPermitsRoute(rp, base);
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("continue_from_permit_to_fall_off");
      // should permit everything without tag 10 or 11
      assertRoutingPolicyPermitsRoute(rp, base);
      assertRoutingPolicyDeniesRoute(rp, base.toBuilder().setTag(10L).build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("continue_from_permit_and_set_to_fall_off");
      // should permit everything that does not have tag 10
      assertThat(processRouteIn(rp, base), hasMetric(10L));
      assertRoutingPolicyDeniesRoute(rp, base.toBuilder().setTag(10L).build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("continue_from_set_to_match_on_set_field");
      // TODO: verify
      // should deny everything without STARTING metric 10
      assertRoutingPolicyDeniesRoute(rp, base);
      assertRoutingPolicyPermitsRoute(rp, base.toBuilder().setMetric(10L).build());
    }
    {
      RoutingPolicy rp = c.getRoutingPolicies().get("reach_continue_target_without_match");
      // should permit everything
      assertRoutingPolicyPermitsRoute(rp, base);
    }
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
            "match_as_number",
            "match_as_path",
            "match_community_standard",
            "match_community_expanded",
            "match_interface",
            "match_ip_address",
            "match_ip_address_prefix_list",
            "match_ipv6_address",
            "match_ipv6_address_prefix_list",
            "match_metric",
            "match_route_type_external",
            "match_route_type_internal",
            "match_route_type_local",
            "match_route_type_nssa_external",
            "match_route_type_type_1",
            "match_route_type_type_2",
            "match_route_types",
            "match_route_types_unsupported",
            "match_source_protocol_connected",
            "match_source_protocol_static",
            "match_tag",
            "match_vlan",
            "set_as_path_prepend_last_as",
            "set_as_path_prepend_literal_as",
            "set_comm_list_expanded",
            "set_comm_list_standard",
            "set_comm_list_standard_single",
            "set_community",
            "set_community_additive",
            "set_ip_next_hop_literal",
            "set_ip_next_hop_literal2",
            "set_ip_next_hop_unchanged",
            "set_ipv6_next_hop_unchanged",
            "set_local_preference",
            "set_metric",
            "set_metric_eigrp",
            "set_metric_type_external",
            "set_metric_type_internal",
            "set_metric_type_type_1",
            "set_metric_type_type_2",
            "set_origin_egp",
            "set_origin_igp",
            "set_origin_incomplete",
            "set_tag",
            "set_weight",
            "match_undefined_access_list",
            "match_undefined_community_list",
            "match_undefined_prefix_list",
            "continue_skip_deny",
            "continue_from_deny_to_permit",
            "continue_from_permit_to_fall_off",
            "continue_from_permit_and_set_to_fall_off",
            "continue_from_set_to_match_on_set_field",
            "reach_continue_target_without_match"));
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
      RouteMap rm = vc.getRouteMaps().get("match_as_number");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchAsNumber match = entry.getMatchAsNumber();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(
          match.getAsns(),
          equalTo(
              LongSpace.builder()
                  .including(64496L)
                  .including(Range.closed(64498L, 64510L))
                  .including(3000000000L)
                  .build()));
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
      RouteMap rm = vc.getRouteMaps().get("match_community_standard");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchCommunity match = entry.getMatchCommunity();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("community_list_standard"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_community_expanded");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchCommunity match = entry.getMatchCommunity();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("community_list_expanded"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_interface");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchInterface match = entry.getMatchInterface();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("loopback0", "loopback1"));
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
      RouteMap rm = vc.getRouteMaps().get("match_ipv6_address");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchIpv6Address match = entry.getMatchIpv6Address();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getName(), equalTo("ipv6_access_list1"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_ipv6_address_prefix_list");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchIpv6AddressPrefixList match = entry.getMatchIpv6AddressPrefixList();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("ipv6_prefix_list1"));
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
      RouteMap rm = vc.getRouteMaps().get("match_route_type_external");
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      RouteMapMatchRouteType match = entry.getMatchRouteType();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTypes(), equalTo(ImmutableSet.of(Type.EXTERNAL)));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_route_type_internal");
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      RouteMapMatchRouteType match = entry.getMatchRouteType();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTypes(), equalTo(ImmutableSet.of(Type.INTERNAL)));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_route_type_local");
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      RouteMapMatchRouteType match = entry.getMatchRouteType();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTypes(), equalTo(ImmutableSet.of(Type.LOCAL)));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_route_type_nssa_external");
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      RouteMapMatchRouteType match = entry.getMatchRouteType();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTypes(), equalTo(ImmutableSet.of(Type.NSSA_EXTERNAL)));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_route_type_type_1");
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      RouteMapMatchRouteType match = entry.getMatchRouteType();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTypes(), equalTo(ImmutableSet.of(Type.TYPE_1)));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_route_type_type_2");
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      RouteMapMatchRouteType match = entry.getMatchRouteType();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTypes(), equalTo(ImmutableSet.of(Type.TYPE_2)));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_route_types");
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      RouteMapMatchRouteType match = entry.getMatchRouteType();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTypes(), equalTo(ImmutableSet.of(Type.TYPE_1, Type.TYPE_2)));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_route_types_unsupported");
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      RouteMapMatchRouteType match = entry.getMatchRouteType();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTypes(), equalTo(ImmutableSet.of(Type.TYPE_1, Type.NSSA_EXTERNAL)));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_source_protocol_connected");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchSourceProtocol match = entry.getMatchSourceProtocol();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getSourceProtocol(), equalTo("connected"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_source_protocol_static");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchSourceProtocol match = entry.getMatchSourceProtocol();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getSourceProtocol(), equalTo("static"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_tag");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchTag match = entry.getMatchTag();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTags(), contains(1L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_vlan");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchVlan match = entry.getMatchVlan();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getVlans(), equalTo(IntegerSpace.builder().including(1, 3, 4).build()));
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
      RouteMap rm = vc.getRouteMaps().get("set_comm_list_expanded");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetCommListDelete set = entry.getSetCommListDelete();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getName(), equalTo("community_list_expanded"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_comm_list_standard");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetCommListDelete set = entry.getSetCommListDelete();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getName(), equalTo("community_list_standard"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_comm_list_standard_single");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetCommListDelete set = entry.getSetCommListDelete();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getName(), equalTo("community_list_standard_single"));
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
      RouteMap rm = vc.getRouteMaps().get("set_metric_eigrp");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetricEigrp set = entry.getSetMetricEigrp();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(
          set.getMetric(),
          equalTo(new org.batfish.representation.cisco_nxos.EigrpMetric(1, 2, 3, 4, 5)));
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
    {
      RouteMap rm = vc.getRouteMaps().get("set_weight");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetWeight set = entry.getSetWeight();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getWeight(), equalTo(1));
    }

    // continue extraction
    {
      RouteMap rm = vc.getRouteMaps().get("continue_skip_deny");
      assertThat(rm.getEntries().keySet(), contains(10, 20, 30));
      assertThat(rm.getEntries().get(10).getAction(), equalTo(LineAction.PERMIT));
      assertThat(rm.getEntries().get(10).getContinue(), equalTo(30));
      assertThat(rm.getEntries().get(20).getAction(), equalTo(LineAction.DENY));
      assertThat(rm.getEntries().get(20).getContinue(), nullValue());
      assertThat(rm.getEntries().get(30).getAction(), equalTo(LineAction.PERMIT));
      assertThat(rm.getEntries().get(30).getContinue(), nullValue());
    }
    {
      RouteMap rm = vc.getRouteMaps().get("continue_from_deny_to_permit");
      assertThat(rm.getEntries().keySet(), contains(10, 20));
      assertThat(rm.getEntries().get(10).getAction(), equalTo(LineAction.DENY));
      assertThat(rm.getEntries().get(10).getContinue(), equalTo(20));
      assertThat(rm.getEntries().get(20).getAction(), equalTo(LineAction.PERMIT));
      assertThat(rm.getEntries().get(20).getContinue(), nullValue());
    }
    {
      RouteMap rm = vc.getRouteMaps().get("continue_from_permit_to_fall_off");
      assertThat(rm.getEntries().keySet(), contains(10, 20));
      assertThat(rm.getEntries().get(10).getAction(), equalTo(LineAction.PERMIT));
      assertThat(rm.getEntries().get(10).getContinue(), equalTo(20));
      assertThat(rm.getEntries().get(20).getAction(), equalTo(LineAction.DENY));
      assertThat(rm.getEntries().get(20).getContinue(), nullValue());
      assertThat(rm.getEntries().get(20).getMatchTag().getTags(), contains(10L, 11L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("continue_from_permit_and_set_to_fall_off");
      assertThat(rm.getEntries().keySet(), contains(10, 20));
      assertThat(rm.getEntries().get(10).getAction(), equalTo(LineAction.PERMIT));
      assertThat(rm.getEntries().get(10).getContinue(), equalTo(20));
      assertThat(rm.getEntries().get(10).getSetMetric().getMetric(), equalTo(10L));
      assertThat(rm.getEntries().get(20).getAction(), equalTo(LineAction.DENY));
      assertThat(rm.getEntries().get(20).getContinue(), nullValue());
      assertThat(rm.getEntries().get(20).getMatchTag().getTags(), contains(10L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("continue_from_set_to_match_on_set_field");
      assertThat(rm.getEntries().keySet(), contains(10, 20));
      assertThat(rm.getEntries().get(10).getAction(), equalTo(LineAction.DENY));
      assertThat(rm.getEntries().get(10).getContinue(), equalTo(20));
      assertThat(rm.getEntries().get(10).getSetMetric().getMetric(), equalTo(10L));
      assertThat(rm.getEntries().get(20).getAction(), equalTo(LineAction.PERMIT));
      assertThat(rm.getEntries().get(20).getContinue(), nullValue());
      assertThat(rm.getEntries().get(20).getMatchMetric().getMetric(), equalTo(10L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("reach_continue_target_without_match");
      assertThat(rm.getEntries().keySet(), contains(10, 20, 30));
      assertThat(rm.getEntries().get(10).getAction(), equalTo(LineAction.PERMIT));
      assertThat(rm.getEntries().get(10).getContinue(), equalTo(30));
      assertThat(rm.getEntries().get(10).getMatchTag().getTags(), contains(10L));
      assertThat(rm.getEntries().get(20).getAction(), equalTo(LineAction.DENY));
      assertThat(rm.getEntries().get(20).getContinue(), nullValue());
      assertThat(rm.getEntries().get(20).getMatchTag().getTags(), contains(10L));
      assertThat(rm.getEntries().get(30).getAction(), equalTo(LineAction.PERMIT));
      assertThat(rm.getEntries().get(30).getContinue(), nullValue());
    }
  }

  @Test
  public void testRouteMapExhaustive() throws IOException {
    Configuration c = parseConfig("nxos_route_map_exhaustive");
    assertThat(c.getRoutingPolicies(), hasKey("RM"));
    RoutingPolicy rm = c.getRoutingPolicies().get("RM");
    Bgpv4Route base =
        Bgpv4Route.testBuilder()
            .setTag(0L)
            .setSrcProtocol(RoutingProtocol.BGP)
            .setMetric(0L) // 30 match metric 3
            .setAsPath(AsPath.ofSingletonAsSets(2L))
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setNextHopIp(Ip.parse("192.0.2.254"))
            .setNetwork(Prefix.ZERO)
            .build();
    // There are 8 paths through the route-map, let's test them all.
    // 10 deny tag 1, continue                OR    fall-through
    // 20 permit community 0:2, continue      OR    fall-through
    // 30 deny metric 3, terminate            OR   40 terminate
    {
      // false false false -> 40 only
      Bgpv4Route after = processRouteIn(rm, base);
      assertThat(after.getTag(), not(equalTo(10L)));
      assertThat(after.getCommunities(), not(equalTo(CommunitySet.of(StandardCommunity.of(20)))));
      assertThat(after.getMetric(), not(equalTo(30L)));
      assertThat(after.getLocalPreference(), equalTo(40L));
    }
    {
      // false false true -> 30 only
      assertRoutingPolicyDeniesRoute(rm, base.toBuilder().setMetric(3).build());
    }
    {
      // false true false -> 20, 40
      Bgpv4Route after =
          processRouteIn(
              rm,
              base.toBuilder().setCommunities(CommunitySet.of(StandardCommunity.of(2))).build());
      assertThat(after.getTag(), not(equalTo(10L)));
      assertThat(after.getCommunities(), equalTo(CommunitySet.of(StandardCommunity.of(20))));
      assertThat(after.getMetric(), not(equalTo(30L)));
      assertThat(after.getLocalPreference(), equalTo(40L));
    }
    {
      // false true true -> 20, 30
      assertRoutingPolicyDeniesRoute(
          rm,
          base.toBuilder()
              .setCommunities(CommunitySet.of(StandardCommunity.of(2)))
              .setMetric(3)
              .build());
    }
    {
      // true false false -> 10, 40
      Bgpv4Route after = processRouteIn(rm, base.toBuilder().setTag(1L).build());
      assertThat(after.getTag(), equalTo(10L));
      assertThat(after.getCommunities(), not(equalTo(CommunitySet.of(StandardCommunity.of(20)))));
      assertThat(after.getMetric(), not(equalTo(30L)));
      assertThat(after.getLocalPreference(), equalTo(40L));
    }
    {
      // true false true -> 10, 30
      assertRoutingPolicyDeniesRoute(rm, base.toBuilder().setTag(1L).setMetric(3).build());
    }
    {
      // true true false -> 10, 20, 40
      Bgpv4Route after =
          processRouteIn(
              rm,
              base.toBuilder()
                  .setTag(1L)
                  .setCommunities(CommunitySet.of(StandardCommunity.of(2)))
                  .build());
      assertThat(after.getTag(), equalTo(10L));
      assertThat(after.getCommunities(), equalTo(CommunitySet.of(StandardCommunity.of(20))));
      assertThat(after.getMetric(), not(equalTo(30L)));
      assertThat(after.getLocalPreference(), equalTo(40L));
    }
    {
      // true true true -> 10, 20, 30
      assertRoutingPolicyDeniesRoute(
          rm,
          base.toBuilder()
              .setTag(1L)
              .setCommunities(CommunitySet.of(StandardCommunity.of(2)))
              .setMetric(3)
              .build());
    }
  }

  @Test
  public void testRouteMapMultipleChainedContinueEntriesConversion() throws IOException {
    Configuration c = parseConfig("nxos_route_map_multiple_chained_continue_entries");
    RoutingPolicy exportPolicy =
        c.getRoutingPolicies()
            .get(
                c.getDefaultVrf()
                    .getBgpProcess()
                    .getActiveNeighbors()
                    .get(Prefix.strict("192.0.2.2/32"))
                    .getIpv4UnicastAddressFamily()
                    .getExportPolicy());
    assertRoutingPolicyPermitsRoute(
        exportPolicy, new ConnectedRoute(Prefix.strict("10.10.10.10/32"), "loopback0"));
  }

  @Test
  public void testSnmpServerConversion() throws IOException {
    String hostname = "nxos_snmp_server";
    Configuration c = parseConfig(hostname);

    assertThat(c.getSnmpTrapServers(), containsInAnyOrder("192.0.2.1", "192.0.2.2"));
    assertThat(c.getSnmpSourceInterface(), equalTo("mgmt0"));
    Map<String, org.batfish.datamodel.SnmpCommunity> communities =
        c.getDefaultVrf().getSnmpServer().getCommunities();
    assertThat(communities, hasKeys("SECRETcommunity1", "SECRETcommunity2"));
    assertThat(communities.get("SECRETcommunity1").getClientIps(), nullValue());
    assertThat(
        communities.get("SECRETcommunity2").getClientIps(),
        equalTo(
            AclIpSpace.rejecting(Ip.parse("1.2.3.4").toIpSpace())
                .thenPermitting(Prefix.parse("1.2.3.0/24").toIpSpace())
                .thenPermitting(Prefix.parse("2.0.0.0/8").toIpSpace())
                .build()));
  }

  @Test
  public void testSnmpServerExtraction() {
    String hostname = "nxos_snmp_server";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getSnmpServers(), hasKeys("192.0.2.1", "192.0.2.2"));
    assertThat(vc.getSnmpSourceInterface(), equalTo("mgmt0"));

    assertThat(vc.getSnmpCommunities(), hasKeys("SECRETcommunity1", "SECRETcommunity2"));
    SnmpCommunity c1 = vc.getSnmpCommunities().get("SECRETcommunity1");
    assertThat(c1.getAclName(), nullValue());
    assertThat(c1.getAclNameV4(), nullValue());
    assertThat(c1.getAclNameV6(), nullValue());
    SnmpCommunity c2 = vc.getSnmpCommunities().get("SECRETcommunity2");
    assertThat(c2.getAclName(), equalTo("snmp_acl1"));
    assertThat(c2.getAclNameV4(), equalTo("snmp_acl4"));
    assertThat(c2.getAclNameV6(), equalTo("snmp_acl6"));
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

    assertThat(c.getVrfs(), hasKeys(DEFAULT_VRF_NAME, MANAGEMENT_VRF_NAME, "vrf1"));
    assertThat(
        c.getDefaultVrf().getStaticRoutes(),
        containsInAnyOrder(
            hasPrefix(Prefix.strict("10.0.0.0/24")),
            hasPrefix(Prefix.strict("10.0.1.0/24")),
            hasPrefix(Prefix.strict("10.0.2.0/24")),
            hasPrefix(Prefix.strict("10.0.13.0/24")),
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
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.13.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(UNSET_ROUTE_NEXT_HOP_IP));
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

    assertThat(vc.getVrfs(), hasKeys(DEFAULT_VRF_NAME, MANAGEMENT_VRF_NAME, "vrf1"));
    assertThat(
        vc.getDefaultVrf().getStaticRoutes().asMap(),
        hasKeys(
            Prefix.strict("10.0.0.0/24"),
            Prefix.strict("10.0.1.0/24"),
            Prefix.strict("10.0.2.0/24"),
            Prefix.strict("10.0.13.0/24"),
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
      assertThat(route.getPreference(), equalTo(1));
      assertThat(route.getTag(), equalTo(0L));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.1.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo(1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.2.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo(1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.13.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo(1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), nullValue());
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.3.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo(1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.4.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo(1));
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
      assertThat(route.getPreference(), equalTo(1));
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
      assertThat(route.getPreference(), equalTo(5));
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
      assertThat(route.getPreference(), equalTo(5));
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
      assertThat(route.getPreference(), equalTo(5));
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
      assertThat(route.getPreference(), equalTo(5));
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
      assertThat(route.getPreference(), equalTo(5));
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
      assertThat(route.getPreference(), equalTo(1));
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
      assertThat(route.getPreference(), equalTo(1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.2.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/100"));
      assertThat(route.getNextHopVrf(), nullValue());
    }
    // TODO: extract and test bfd settings for static routes
  }

  @Test
  public void testStaticRouteReferences() throws IOException {
    String hostname = "nxos_static_route_references";
    String filename = String.format("configs/%s", hostname);
    Batfish bf = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        bf.loadConvertConfigurationAnswerElementOrReparse(bf.getSnapshot());
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.INTERFACE, "Ethernet1/1", 2));
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VRF, "vrf1", 2));
  }

  @Test
  public void testVdcParsing() {
    // don't throw
    parseVendorConfig("nxos_vdc");
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
        hasKeys("Ethernet1/1", "Vlan1", "Vlan2", "Vlan3", "Vlan4", "Vlan6", "Vlan7", "Vlan1000"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan1");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(1));
      assertThat(iface, hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan2");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(2));
      assertThat(iface, hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan3");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState(false));
      assertThat(iface, hasVlan(3));
      assertThat(iface, hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan4");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(4));
      assertThat(iface, hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan6");
      assertThat(iface, isActive());
      assertThat(iface, isAutoState(false));
      assertThat(iface, hasVlan(6));
      assertThat(iface, hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan7");
      assertThat(iface, isActive());
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(7));
      assertThat(iface, hasSwitchPortMode(org.batfish.datamodel.SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
  }

  @Test
  public void testVlanExtraction() {
    String hostname = "nxos_vlan";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Vlan1", "Vlan2", "Vlan3", "Vlan4", "Vlan6", "Vlan7", "Vlan1000"));
    {
      Interface iface = vc.getInterfaces().get("Vlan1");
      assertThat(iface.getShutdown(), nullValue());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(1));
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan2");
      assertThat(iface.getShutdown(), nullValue());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(2));
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan3");
      assertFalse(iface.getShutdown());
      assertFalse(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(3));
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan4");
      assertFalse(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(4));
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan6");
      assertFalse(iface.getShutdown());
      assertFalse(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(6));
      assertThat(iface.getSwitchportMode(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan7");
      assertFalse(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(7));
      assertThat(iface.getSwitchportMode(), nullValue());
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
    Batfish bf = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        bf.loadConvertConfigurationAnswerElementOrReparse(bf.getSnapshot());
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VLAN, "1", 1));
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VLAN, "2", 0));
    assertThat(ans, hasUndefinedReference(filename, CiscoNxosStructureType.VLAN, "3"));
  }

  @Test
  public void testVrfConversion() throws IOException {
    String hostname = "nxos_vrf";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVrfs(), hasKeys(DEFAULT_VRF_NAME, MANAGEMENT_VRF_NAME, "Vrf1", "vrf3"));
    {
      Map<String, org.batfish.datamodel.Interface> vrfIfaces = c.getAllInterfaces(DEFAULT_VRF_NAME);
      assertThat(vrfIfaces, hasKey("Ethernet1/2"));
    }
    {
      Map<String, org.batfish.datamodel.Interface> vrfIfaces = c.getAllInterfaces("Vrf1");
      assertThat(vrfIfaces, hasKeys("Ethernet1/1", "Ethernet1/3", "Ethernet1/4"));
    }
    {
      Map<String, org.batfish.datamodel.Interface> vrfIfaces = c.getAllInterfaces("vrf3");
      assertThat(vrfIfaces, hasKeys("Ethernet1/5"));
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

    assertThat(vc.getDefaultVrf(), not(nullValue()));
    assertThat(vc.getDefaultVrf().getId(), equalTo(DEFAULT_VRF_ID));
    assertThat(vc.getVrfs(), hasKeys(DEFAULT_VRF_NAME, MANAGEMENT_VRF_NAME, "Vrf1", "vrf3"));
    {
      Vrf vrf = vc.getDefaultVrf();
      assertFalse(vrf.getShutdown());
    }
    {
      Vrf vrf = vc.getVrfs().get("Vrf1");
      assertFalse(vrf.getShutdown());
      assertThat(vrf.getId(), equalTo(3));
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
      assertThat(vrf.getId(), equalTo(4));
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

    assertThat(vc.getVrfs(), hasKeys(DEFAULT_VRF_NAME, MANAGEMENT_VRF_NAME, "vrf1"));
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
    Batfish bf = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ans =
        bf.loadConvertConfigurationAnswerElementOrReparse(bf.getSnapshot());
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VRF, "vrf_used", 1));
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VRF, "vrf_unused", 0));
    assertThat(ans, hasUndefinedReference(filename, CiscoNxosStructureType.VRF, "vrf_undefined"));
  }

  @Test
  public void testWordLexing() {
    assertThat(parseVendorConfig("nxos_word"), notNullValue());
  }

  @Test
  public void testBgpAllowAsInExtraction() {
    String hostname = "nxos_bgp_allowas_in";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);
    assertThat(
        vc.getBgpGlobalConfiguration()
            .getVrfs()
            .get(DEFAULT_VRF_NAME)
            .getNeighbors()
            .get(Ip.parse("1.1.1.1"))
            .getIpv4UnicastAddressFamily()
            .getAllowAsIn(),
        equalTo(3));
    assertThat(
        vc.getBgpGlobalConfiguration()
            .getVrfs()
            .get(DEFAULT_VRF_NAME)
            .getNeighbors()
            .get(Ip.parse("2.2.2.2"))
            .getIpv4UnicastAddressFamily()
            .getAllowAsIn(),
        equalTo(2));
    assertThat(
        vc.getBgpGlobalConfiguration()
            .getVrfs()
            .get(DEFAULT_VRF_NAME)
            .getNeighbors()
            .get(Ip.parse("3.3.3.3"))
            .getIpv4UnicastAddressFamily()
            .getAllowAsIn(),
        nullValue());
  }

  @Test
  public void testInterfaceEigrpPropertiesExtraction() {
    String hostname = "nxos_interface_eigrp";
    // TODO: test other properties (hold time, hello interval)
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);
    {
      // Interface with custom EIGRP BW, delay, passive-interface
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(iface.getEigrp(), equalTo("1"));
      assertThat(iface.getEigrpBandwidth(), equalTo(300));
      assertThat(iface.getEigrpDelay(), equalTo(400));
      assertTrue(iface.getEigrpPassive());
    }
    {
      // Interface with no custom EIGRP configurations
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertThat(iface.getEigrp(), equalTo("EIGRP2"));
      assertNull(iface.getEigrpBandwidth());
      assertNull(iface.getEigrpDelay());
      assertFalse(iface.getEigrpPassive());
    }
  }

  @Test
  public void testInterfaceEigrpPropertiesConversion() throws IOException {
    String hostname = "nxos_interface_eigrp";
    Configuration c = parseConfig(hostname);
    {
      /*
      interface Ethernet1/1
        vrf member VRF
        ip address 192.0.2.2/24
        ip router eigrp 1
        ip bandwidth eigrp 1 300
        ip delay eigrp 1 400
        ip hold-time eigrp 1 100
        ip hello-interval eigrp 1 200
        ip passive-interface eigrp 1
       */
      String ifaceName = "Ethernet1/1";
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
      assertThat(
          iface.getBandwidth(), equalTo(getDefaultBandwidth(CiscoNxosInterfaceType.ETHERNET)));
      EigrpInterfaceSettings eigrp = iface.getEigrp();
      assertNotNull(eigrp);
      assertThat(eigrp.getMetric().getValues().getBandwidth(), equalTo(300L));
      // EIGRP metric values have delay in ps (1e-12); config has it in tens of µs (1e-5)
      assertThat(eigrp.getMetric().getValues().getDelay(), equalTo((long) (400 * 1e7)));
      assertTrue(eigrp.getPassive());
    }
    {
      /*
      router eigrp EIGRP2
        autonomous-system 2
      interface Ethernet1/2
        ip address 192.0.3.2/24
        ip router eigrp EIGRP2
       */
      String ifaceName = "Ethernet1/2";
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
      Double defaultBw = getDefaultBandwidth(CiscoNxosInterfaceType.ETHERNET);
      assertThat(iface.getBandwidth(), equalTo(defaultBw));
      EigrpInterfaceSettings eigrp = iface.getEigrp();
      assertNotNull(eigrp);
      assertThat(eigrp.getAsn(), equalTo(2L));
      // EIGRP metric values have bandwidth in kb/s; VI config has it in bits/s
      assertThat(
          eigrp.getMetric().getValues().getBandwidth(), equalTo(defaultBw.longValue() / 1000));
      assertThat(
          eigrp.getMetric().getValues().getDelay(),
          equalTo((long) (defaultDelayTensOfMicroseconds(CiscoNxosInterfaceType.ETHERNET) * 1e7)));
      assertFalse(eigrp.getPassive());
    }
    {
      /*
      router eigrp 3
        autonomous-system 4
      interface Ethernet1/3
        ip address 192.0.3.3/24
        ip router eigrp 3
       */
      // Since Ethernet1/3 is in the default vrf, autononomous-system 4 should override process tag
      String ifaceName = "Ethernet1/3";
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
      assertThat(iface.getEigrp().getAsn(), equalTo(4L));
    }
    {
      /*
      router eigrp 3
        autonomous-system 4
      interface Ethernet1/4
        vrf member VRF
        ip address 192.0.3.4/24
        ip router eigrp 3
       */
      // Since Ethernet1/4 is not in the default vrf, autononomous-system 4 should be ignored
      String ifaceName = "Ethernet1/4";
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
      assertThat(iface.getEigrp().getAsn(), equalTo(3L));
    }
  }

  @Test
  public void testEigrpNetworkStatements() throws IOException {
    /*
    Tests undocumented-but-permitted IOS-like syntax for declaring networks in router eigrp stanza:
    router eigrp 1
      network 1.1.1.0/24
     */
    String hostname = "nxos_eigrp_network_statements";
    Configuration c = parseConfig(hostname);
    /*
    Interfaces Ethernet1/1 - Ethernet1/4 should match EIGRP processes 1-4.
    - Ethernet1/1 has network 10.10.10.1/24 in default VRF
      - Process 1 has network 10.10.10.0/24 at top level
    - Ethernet1/2 has network 11.11.11.1/24 in default VRF
      - Process 2 has network 11.11.0.0/16 in ipv4 address family stanza
    - Ethernet1/3 has network 12.12.12.1/24 in vrf VRF1
      - Process 3 has network 12.12.12.0/30 in VRF1 stanza
    - Ethernet1/4 has network 13.13.13.1/24 in vrf VRF1
      - Process 4 has network 13.13.13.1/32 in VRF1 ipv4 address family stanza
     */
    for (int i = 1; i < 5; i++) {
      String ifaceName = "Ethernet1/" + i;
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
      EigrpInterfaceSettings eigrp = iface.getEigrp();
      assertNotNull(eigrp);
      assertThat(eigrp.getAsn(), equalTo((long) i));
    }
    {
      /*
      Interface Ethernet1/5 should NOT match process 5. It has network 14.14.14.1/24 in vrf VRF.
      Process 5 has:
      - matching network 14.14.14.0/24 at top level (matches default vrf only)
      - matching network 14.14.14.0/24 for vrf VRF2
      - non-matching network 14.14.14.2/32 for vrf VRF1
       */
      String ifaceName = "Ethernet1/5";
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get(ifaceName);
      assertNull(iface.getEigrp());
    }
  }

  @Test
  public void testEigrpDistributeListWithPrefixListExtraction() {
    String hostname = "eigrp_distribute_list_prefix_list";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);
    {
      // Interface with custom EIGRP BW, delay, distribute lists
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(
          iface.getEigrpInboundDistributeList(),
          equalTo(new DistributeList("PL_IN", DistributeListFilterType.PREFIX_LIST)));
      assertThat(
          iface.getEigrpOutboundDistributeList(),
          equalTo(new DistributeList("PL_OUT", DistributeListFilterType.PREFIX_LIST)));
    }
    {
      // Interface with no custom EIGRP configurations
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertNull(iface.getEigrpInboundDistributeList());
      assertNull(iface.getEigrpOutboundDistributeList());
    }
  }

  @Test
  public void testEigrpDistributeListWithPrefixListConversion() throws IOException {
    String hostname = "eigrp_distribute_list_prefix_list";
    Configuration c = parseConfig(hostname);
    Map<String, RoutingPolicy> policies = c.getRoutingPolicies();
    // helper builder
    EigrpInternalRoute.Builder builder =
        EigrpInternalRoute.testBuilder()
            .setAdmin(90)
            .setEigrpMetricVersion(EigrpMetricVersion.V2)
            .setNextHop(NextHopDiscard.instance())
            .setEigrpMetric(
                ClassicMetric.builder()
                    .setValues(EigrpMetricValues.builder().setBandwidth(2e9).setDelay(4e5).build())
                    .build())
            .setProcessAsn(1L);
    {
      String ifaceName = "Ethernet1/1";
      String importPolicyName = eigrpNeighborImportPolicyName(ifaceName, "VRF", 1);
      String exportPolicyName = eigrpNeighborExportPolicyName(ifaceName, "VRF", 1);
      EigrpInterfaceSettings eigrpSettings = c.getAllInterfaces().get(ifaceName).getEigrp();
      assertThat(eigrpSettings.getImportPolicy(), equalTo(importPolicyName));
      assertThat(eigrpSettings.getExportPolicy(), equalTo(exportPolicyName));

      RoutingPolicy importPolicy = policies.get(importPolicyName);
      // Allow only routes permitted by prefix list
      assertTrue(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("1.1.1.1/26")).build(),
              EigrpInternalRoute.testBuilder().setEigrpMetricVersion(EigrpMetricVersion.V2),
              Direction.IN));
      assertFalse(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/31")).build(),
              EigrpInternalRoute.testBuilder().setEigrpMetricVersion(EigrpMetricVersion.V2),
              Direction.IN));

      RoutingPolicy exportPolicy = policies.get(exportPolicyName);
      // Allow only routes permitted by prefix list
      assertTrue(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("2.2.2.2/26")).build(),
              EigrpInternalRoute.testBuilder().setEigrpMetricVersion(EigrpMetricVersion.V2),
              Direction.OUT));
      assertFalse(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/30")).build(),
              EigrpInternalRoute.testBuilder().setEigrpMetricVersion(EigrpMetricVersion.V2),
              Direction.OUT));
    }
    {
      // This interface has no distribute lists; should permit all traffic.
      String ifaceName = "Ethernet1/2";
      String importPolicyName = eigrpNeighborImportPolicyName(ifaceName, "VRF", 1);
      String exportPolicyName = eigrpNeighborExportPolicyName(ifaceName, "VRF", 1);
      EigrpInterfaceSettings eigrpSettings = c.getAllInterfaces().get(ifaceName).getEigrp();
      assertThat(eigrpSettings.getImportPolicy(), equalTo(importPolicyName));
      assertThat(eigrpSettings.getExportPolicy(), equalTo(exportPolicyName));

      RoutingPolicy importPolicy = policies.get(importPolicyName);
      assertTrue(
          importPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/31")).build(),
              EigrpInternalRoute.testBuilder().setEigrpMetricVersion(EigrpMetricVersion.V2),
              Direction.IN));

      RoutingPolicy exportPolicy = policies.get(exportPolicyName);
      assertTrue(
          exportPolicy.process(
              builder.setNetwork(Prefix.parse("5.5.5.5/30")).build(),
              EigrpInternalRoute.testBuilder().setEigrpMetricVersion(EigrpMetricVersion.V2),
              Direction.OUT));
    }
  }
}

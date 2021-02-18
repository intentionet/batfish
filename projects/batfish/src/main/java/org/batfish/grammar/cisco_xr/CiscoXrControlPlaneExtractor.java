package org.batfish.grammar.cisco_xr;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toCollection;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.AS_PATH_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.BFD_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.BGP_AF_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.BGP_NEIGHBOR_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.BGP_PEER_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.BGP_SESSION_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.BGP_TEMPLATE_PEER_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.BGP_TEMPLATE_PEER_SESSION;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.CLASS_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.COMMUNITY_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.CRYPTO_DYNAMIC_MAP_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.CRYPTO_MAP_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.DYNAMIC_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.EXTCOMMUNITY_SET_RT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.ICMP_TYPE_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.INSPECT_CLASS_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.IPSEC_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.IPSEC_TRANSFORM_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.IPV6_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.IP_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.ISAKMP_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.ISAKMP_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.KEYRING;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.L2TP_CLASS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.MAC_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.NAMED_RSA_PUB_KEY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.NETWORK_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.POLICY_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PREFIX6_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PREFIX_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PREFIX_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PROTOCOL_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.SERVICE_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.SERVICE_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.TRACK;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_ADDITIONAL_PATHS_SELECTION_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_AGGREGATE_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_INBOUND_PREFIX6_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_INBOUND_PREFIX_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_INHERITED_PEER_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_INHERITED_SESSION;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_LISTEN_RANGE_PEER_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_PEER_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_ROUTE_POLICY_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_ROUTE_POLICY_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_OUTBOUND_PREFIX6_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_OUTBOUND_PREFIX_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_UPDATE_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_USE_AF_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_USE_NEIGHBOR_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_USE_SESSION_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CLASS_MAP_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CLASS_MAP_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CLASS_MAP_ACTIVATED_SERVICE_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CLASS_MAP_SERVICE_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CONTROL_PLANE_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CONTROL_PLANE_SERVICE_POLICY_INPUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CONTROL_PLANE_SERVICE_POLICY_OUTPUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.COPS_LISTENER_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_DYNAMIC_MAP_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_DYNAMIC_MAP_ISAKMP_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_DYNAMIC_MAP_TRANSFORM_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_CRYPTO_DYNAMIC_MAP_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ISAKMP_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_TRANSFORM_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.DOMAIN_LOOKUP_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EIGRP_AF_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EIGRP_PASSIVE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.FAILOVER_LAN_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.FAILOVER_LINK_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ICMP_TYPE_OBJECT_GROUP_GROUP_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INSPECT_CLASS_MAP_MATCH_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_BFD_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IGMP_ACCESS_GROUP_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IGMP_HOST_PROXY_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IGMP_STATIC_GROUP_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_INCOMING_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IP_INBAND_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IP_VERIFY_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_OUTGOING_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_PIM_NEIGHBOR_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_SELF_REF;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_SERVICE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_STANDBY_TRACK;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.IPSEC_PROFILE_ISAKMP_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.IPSEC_PROFILE_TRANSFORM_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.IP_DOMAIN_LOOKUP_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.IP_ROUTE_NHINT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.IP_TACACS_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ISAKMP_POLICY_SELF_REF;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ISAKMP_PROFILE_KEYRING;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ISAKMP_PROFILE_SELF_REF;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.LINE_ACCESS_CLASS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.LINE_ACCESS_CLASS_LIST6;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MANAGEMENT_SSH_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MANAGEMENT_TELNET_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MSDP_PEER_SA_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NAMED_RSA_PUB_KEY_SELF_REF;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NETWORK_OBJECT_GROUP_GROUP_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NETWORK_OBJECT_GROUP_NETWORK_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_AREA_FILTER_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_AREA_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_DISTRIBUTE_LIST_PREFIX_LIST_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_DISTRIBUTE_LIST_PREFIX_LIST_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.PIM_ACCEPT_REGISTER_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.PIM_ACCEPT_RP_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.PIM_RP_ADDRESS_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.PIM_RP_ANNOUNCE_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.PIM_RP_CANDIDATE_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.PIM_SEND_RP_ANNOUNCE_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.PIM_SPT_THRESHOLD_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.POLICY_MAP_EVENT_CLASS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.POLICY_MAP_EVENT_CLASS_ACTIVATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.PROTOCOL_OBJECT_GROUP_GROUP_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.RIP_DISTRIBUTE_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_ISIS_DISTRIBUTE_LIST_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_STATIC_ROUTE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_VRRP_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_APPLY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_AS_PATH_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_COMMUNITY_MATCHES_ANY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_COMMUNITY_MATCHES_EVERY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_DELETE_COMMUNITY_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_PREFIX_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_SET_COMMUNITY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_SET_EXTCOMMUNITY_RT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SERVICE_OBJECT_GROUP_SERVICE_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SERVICE_POLICY_GLOBAL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_COMMUNITY_ACL4;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_COMMUNITY_ACL6;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_FILE_TRANSFER_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_TFTP_SERVER_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_TRAP_SOURCE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SSH_IPV4_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SSH_IPV6_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SYSTEM_SERVICE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.TACACS_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.TRACK_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.TUNNEL_PROTECTION_IPSEC_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.TUNNEL_SOURCE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.WCCP_GROUP_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.WCCP_REDIRECT_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.WCCP_SERVICE_LIST;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.batfish.common.BatfishException;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AaaAuthenticationLoginList;
import org.batfish.datamodel.AuthenticationMethod;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.DscpType;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.IcmpCode;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.IkeKeyType;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.IntegerSpace.Builder;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Ip6Wildcard;
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
import org.batfish.datamodel.LineType;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.Prefix6Range;
import org.batfish.datamodel.Prefix6Space;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SnmpCommunity;
import org.batfish.datamodel.SnmpHost;
import org.batfish.datamodel.SnmpServer;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.TcpFlagsMatchConditions;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.eigrp.ClassicMetric;
import org.batfish.datamodel.eigrp.EigrpMetric;
import org.batfish.datamodel.eigrp.EigrpMetricValues;
import org.batfish.datamodel.eigrp.EigrpProcessMode;
import org.batfish.datamodel.eigrp.WideMetric;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisMetricType;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.routing_policy.expr.AsExpr;
import org.batfish.datamodel.routing_policy.expr.AsPathSetElem;
import org.batfish.datamodel.routing_policy.expr.AsPathSetExpr;
import org.batfish.datamodel.routing_policy.expr.AutoAs;
import org.batfish.datamodel.routing_policy.expr.DecrementLocalPreference;
import org.batfish.datamodel.routing_policy.expr.DecrementMetric;
import org.batfish.datamodel.routing_policy.expr.ExplicitAs;
import org.batfish.datamodel.routing_policy.expr.ExplicitAsPathSet;
import org.batfish.datamodel.routing_policy.expr.IgpCost;
import org.batfish.datamodel.routing_policy.expr.IncrementLocalPreference;
import org.batfish.datamodel.routing_policy.expr.IncrementMetric;
import org.batfish.datamodel.routing_policy.expr.IntComparator;
import org.batfish.datamodel.routing_policy.expr.IntExpr;
import org.batfish.datamodel.routing_policy.expr.IsisLevelExpr;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.LiteralIsisLevel;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.LiteralRouteType;
import org.batfish.datamodel.routing_policy.expr.LongExpr;
import org.batfish.datamodel.routing_policy.expr.NamedAsPathSet;
import org.batfish.datamodel.routing_policy.expr.OriginExpr;
import org.batfish.datamodel.routing_policy.expr.RegexAsPathSetElem;
import org.batfish.datamodel.routing_policy.expr.RouteType;
import org.batfish.datamodel.routing_policy.expr.RouteTypeExpr;
import org.batfish.datamodel.routing_policy.expr.SubRangeExpr;
import org.batfish.datamodel.routing_policy.expr.VarAs;
import org.batfish.datamodel.routing_policy.expr.VarAsPathSet;
import org.batfish.datamodel.routing_policy.expr.VarInt;
import org.batfish.datamodel.routing_policy.expr.VarIsisLevel;
import org.batfish.datamodel.routing_policy.expr.VarLong;
import org.batfish.datamodel.routing_policy.expr.VarOrigin;
import org.batfish.datamodel.routing_policy.expr.VarRouteType;
import org.batfish.datamodel.tracking.DecrementPriority;
import org.batfish.datamodel.tracking.TrackAction;
import org.batfish.datamodel.tracking.TrackInterface;
import org.batfish.datamodel.vendor_family.cisco_xr.Aaa;
import org.batfish.datamodel.vendor_family.cisco_xr.AaaAccounting;
import org.batfish.datamodel.vendor_family.cisco_xr.AaaAccountingCommands;
import org.batfish.datamodel.vendor_family.cisco_xr.AaaAccountingDefault;
import org.batfish.datamodel.vendor_family.cisco_xr.AaaAuthentication;
import org.batfish.datamodel.vendor_family.cisco_xr.AaaAuthenticationLogin;
import org.batfish.datamodel.vendor_family.cisco_xr.Buffered;
import org.batfish.datamodel.vendor_family.cisco_xr.L2tpClass;
import org.batfish.datamodel.vendor_family.cisco_xr.Logging;
import org.batfish.datamodel.vendor_family.cisco_xr.LoggingHost;
import org.batfish.datamodel.vendor_family.cisco_xr.LoggingType;
import org.batfish.datamodel.vendor_family.cisco_xr.Ntp;
import org.batfish.datamodel.vendor_family.cisco_xr.NtpServer;
import org.batfish.datamodel.vendor_family.cisco_xr.Service;
import org.batfish.datamodel.vendor_family.cisco_xr.Sntp;
import org.batfish.datamodel.vendor_family.cisco_xr.SntpServer;
import org.batfish.datamodel.vendor_family.cisco_xr.SshSettings;
import org.batfish.datamodel.vendor_family.cisco_xr.User;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_accountingContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_accounting_commands_lineContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_accounting_defaultContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_accounting_default_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_accounting_default_localContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_authenticationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_authentication_list_methodContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_authentication_loginContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_authentication_login_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_authentication_login_privilege_modeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aaa_new_modelContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Access_list_actionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Access_list_ip6_rangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Access_list_ip_rangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Activate_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Additional_paths_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Additional_paths_selection_xr_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Address_family_headerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Address_family_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Af_group_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aggregate_address_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Allowas_in_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Always_compare_med_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Apply_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_path_multipath_relax_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_path_set_elemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_path_set_inlineContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_path_set_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspse_ios_regexContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Auto_summary_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_advertise_inactive_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_asnContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_confederation_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_listen_range_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_redistribute_internal_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_and_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_apply_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_in_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_is_local_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_neighbor_is_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_originates_from_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_passes_through_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_community_matches_any_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_community_matches_every_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_destination_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_local_preference_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_med_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_next_hop_in_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_not_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_rib_has_route_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_route_type_is_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_simple_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_tag_is_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cd_match_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cd_set_isakmp_profileContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cd_set_peerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cd_set_pfsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cd_set_transform_setContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cip_profileContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cip_transform_setContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cipprf_set_isakmp_profileContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cipprf_set_pfsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cipprf_set_transform_setContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cipt_modeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cis_keyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cis_policyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cis_profileContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cisco_xr_configurationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cispol_authenticationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cispol_encryptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cispol_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cispol_hashContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cispol_lifetimeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cisprf_keyringContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cisprf_local_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cisprf_matchContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cisprf_self_identityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ckp_named_keyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ckpn_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ckpn_key_stringContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ckr_local_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ckr_pskContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cluster_id_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cm_ios_inspectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cm_iosi_matchContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cm_matchContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cmm_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cmm_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cmm_activated_service_templateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cmm_service_templateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.CommunityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_elemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_elem_halfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Compare_routerid_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Copsl_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cp_ip_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cp_service_policyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Crypto_dynamic_mapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Crypto_keyringContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Crypto_mapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Crypto_map_t_ii_match_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Crypto_map_t_ii_set_isakmp_profileContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Crypto_map_t_ii_set_peerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Crypto_map_t_ii_set_pfsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Crypto_map_t_ii_set_transform_setContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Crypto_map_t_ipsec_isakmpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Default_information_originate_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Default_metric_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Default_originate_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Default_shutdown_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Delete_community_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Description_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Description_lineContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Dh_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Disposition_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Distribute_list_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Distribute_list_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Domain_lookupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Domain_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Domain_name_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Dscp_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ebgp_multihop_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Eigrp_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Else_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Elseif_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Empty_neighbor_block_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Enable_secretContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extcommunity_set_rtContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extcommunity_set_rt_elemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extcommunity_set_rt_elem_16Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extcommunity_set_rt_elem_32Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extcommunity_set_rt_elem_as_dot_colonContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extcommunity_set_rt_elem_colonContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extcommunity_set_rt_elem_linesContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extended_access_list_additional_featureContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extended_access_list_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Extended_ipv6_access_list_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Failover_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Failover_linkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Flan_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Flan_unitContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Hash_commentContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Icmp_object_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_autostateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_bandwidthContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_bfd_templateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_bundle_idContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_crypto_mapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_delayContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_descriptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_address_secondaryContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_forwardContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_helper_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_igmpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_inband_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_ospf_areaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_ospf_costContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_ospf_dead_intervalContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_ospf_dead_interval_minimalContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_ospf_hello_intervalContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_ospf_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_ospf_passive_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_ospf_shutdownContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_pim_neighbor_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_proxy_arpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_router_isisContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_router_ospf_areaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_summary_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_verifyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_vrf_forwardingContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ipv6_traffic_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_isis_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_mtuContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_service_policyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_shutdownContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_spanning_treeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_speed_iosContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_st_portfastContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_standbyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_switchportContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_switchport_accessContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_switchport_modeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_switchport_trunk_allowedContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_switchport_trunk_encapsulationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_switchport_trunk_nativeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_vlanContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_vrf_memberContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_vrrpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifdhcpr_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifdhcpr_clientContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifigmp_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifigmphp_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifigmpsg_aclContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Iftunnel_destinationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Iftunnel_modeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Iftunnel_protectionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Iftunnel_sourceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifvrrp_authenticationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifvrrp_ipContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifvrrp_preemptContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifvrrp_priorityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ike_encryptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Inherit_peer_policy_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Inherit_peer_session_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Inspect_protocolContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Int_compContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Int_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Interface_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Interface_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ios_banner_headerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ios_delimited_bannerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_dhcp_relay_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_domain_lookupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_domain_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_hostnameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_prefix_list_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_prefix_list_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_route_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_route_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_ssh_versionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipsec_authenticationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipsec_encryptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv4_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_prefix_list_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_prefix_list_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Is_type_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Isis_levelContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Isis_level_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.L_access_classContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.L_exec_timeoutContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.L_login_authenticationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.L_transportContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Local_as_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_bufferedContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_consoleContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_hostContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_onContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_severityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_source_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_trapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Management_ssh_ip_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Management_telnet_ip_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Match_semanticsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Maximum_paths_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Maximum_peers_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Neighbor_block_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Neighbor_block_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Neighbor_group_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Net_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Network6_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Network_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Next_hop_self_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.No_ip_prefix_list_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.No_ipv4_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.No_ipv6_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.No_neighbor_activate_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.No_neighbor_shutdown_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.No_redistribute_connected_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.No_shutdown_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ntp_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ntp_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ntp_source_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Null_as_path_regexContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.O_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.O_serviceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Og_icmp_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Og_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Og_protocolContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Og_serviceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogg_icmp_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogg_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogg_protocolContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogg_serviceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Oggit_group_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Oggn_group_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Oggp_group_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Oggs_group_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogit_group_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogit_icmp_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogn_group_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogn_host_ipContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogn_ip_with_maskContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogn_network_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogp_group_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogp_protocol_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogs_group_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogs_icmpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogs_port_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogs_service_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogs_tcpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogs_udpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.On_descriptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.On_fqdnContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.On_hostContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.On_rangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.On_subnetContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Origin_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Origin_expr_literalContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Os_descriptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Passive_iis_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Passive_interface_default_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Passive_interface_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Peer_group_assignment_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Peer_group_creation_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Peer_sa_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pim_accept_registerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pim_accept_rpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pim_rp_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pim_rp_announce_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pim_rp_candidateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pim_send_rp_announceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pim_spt_thresholdContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pm_type_accountingContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pm_type_control_subscriberContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pm_type_pbrContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pm_type_performance_trafficContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pm_type_qosContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pm_type_redirectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pm_type_trafficContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pmtcse_classContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pmtcsec_activateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.PortContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Port_specifierContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Prefix_list_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Prefix_set_elemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Prefix_set_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Prepend_as_path_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.ProtocolContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.RangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_autonomous_systemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_classicContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_default_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_distribute_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_eigrp_router_idContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_passive_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_passive_interface_defaultContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_redistribute_bgpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_redistribute_connectedContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_redistribute_eigrpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_redistribute_isisContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_redistribute_ospfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_redistribute_ripContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_redistribute_staticContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Reaf_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Reaf_interface_defaultContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Reafi_passive_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rec_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rec_metric_weightsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Redistribute_aggregate_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Redistribute_connected_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Redistribute_connected_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Redistribute_ospf_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Redistribute_rip_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Redistribute_static_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Redistribute_static_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Remote_as_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Remove_private_as_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ren_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ren_metric_weightsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro6_distribute_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_areaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_area_filterlistContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_area_nssaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_area_stubContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_auto_costContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_default_informationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_default_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_distribute_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_max_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_maximum_pathsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_passive_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_passive_interface_defaultContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_redistribute_bgp_cisco_xrContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_redistribute_connectedContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_redistribute_eigrpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_redistribute_ripContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_redistribute_staticContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_rfc1583_compatibilityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_router_idContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roa_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roa_rangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roi_costContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roi_passiveContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_distinguisherContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_policy_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_policy_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_reflector_client_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_targetContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Router_bgp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Router_id_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Router_isis_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_community_setContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_extcommunity_set_rtContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_isis_metric_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_metric_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_ospf_metric_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_prefix_setContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_route_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_subrangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rr_distribute_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rr_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rr_passive_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rr_passive_interface_defaultContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rs_routeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rs_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_aaaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_access_lineContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_banner_iosContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_bfd_templateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_class_mapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_domain_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_hostnameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ip_default_gatewayContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ip_dhcpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ip_domainContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ip_domain_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ip_name_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ip_pimContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ip_source_routeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ip_sshContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ip_tacacs_source_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_l2tp_classContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_lineContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_loggingContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_mac_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_mac_access_list_extendedContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ntpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_router_ospfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_router_ripContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_serviceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_service_policy_globalContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_service_templateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_snmp_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_sntpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_spanning_treeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_switchportContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_system_service_policyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_tacacs_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_trackContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_usernameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_vrf_definitionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Sd_switchport_blankContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Sd_switchport_shutdownContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Send_community_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Service_group_protocolContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Service_specifier_icmpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Service_specifier_protocolContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Service_specifier_tcp_udpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Session_group_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_community_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_extcommunity_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_extcommunity_rtContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_isis_metric_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_level_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_local_preference_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_med_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_metric_type_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_next_hop_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_next_hop_self_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_origin_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_tag_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_weight_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Shutdown_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Sntp_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Spanning_tree_portfastContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_communityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_enable_trapsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_file_transferContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_hostContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_source_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_tftp_server_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_trap_sourceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ssc_access_controlContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ssc_use_ipv4_aclContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ssh_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ssh_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Standby_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Standby_group_authenticationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Standby_group_ipContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Standby_group_preemptContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Standby_group_priorityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Standby_group_timersContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Standby_group_trackContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Standby_versionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.SubrangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Summary_address_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Suppressed_iis_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Switching_mode_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Switchport_trunk_encapsulationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.T_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.T_source_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Template_peer_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Template_peer_policy_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Template_peer_session_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Track_actionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Track_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ts_hostContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.U_passwordContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.U_roleContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Uint16Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Uint32Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Update_source_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Use_af_group_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Use_neighbor_group_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Use_session_group_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.VariableContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Variable_group_idContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Variable_permissiveContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Viaf_vrrpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Viafv_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Viafv_preemptContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Viafv_priorityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vlan_idContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_block_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrfc_rdContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrfc_route_targetContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrfc_shutdownContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrfc_vniContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrfd_descriptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrrp_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Wccp_idContext;
import org.batfish.representation.cisco_xr.AccessListAddressSpecifier;
import org.batfish.representation.cisco_xr.AccessListServiceSpecifier;
import org.batfish.representation.cisco_xr.AsPathSet;
import org.batfish.representation.cisco_xr.BgpAggregateIpv4Network;
import org.batfish.representation.cisco_xr.BgpAggregateIpv6Network;
import org.batfish.representation.cisco_xr.BgpNetwork;
import org.batfish.representation.cisco_xr.BgpNetwork6;
import org.batfish.representation.cisco_xr.BgpPeerGroup;
import org.batfish.representation.cisco_xr.BgpProcess;
import org.batfish.representation.cisco_xr.BgpRedistributionPolicy;
import org.batfish.representation.cisco_xr.CiscoXrConfiguration;
import org.batfish.representation.cisco_xr.CiscoXrStructureType;
import org.batfish.representation.cisco_xr.CiscoXrStructureUsage;
import org.batfish.representation.cisco_xr.CryptoMapEntry;
import org.batfish.representation.cisco_xr.CryptoMapSet;
import org.batfish.representation.cisco_xr.DistributeList;
import org.batfish.representation.cisco_xr.DistributeList.DistributeListFilterType;
import org.batfish.representation.cisco_xr.DynamicIpBgpPeerGroup;
import org.batfish.representation.cisco_xr.DynamicIpv6BgpPeerGroup;
import org.batfish.representation.cisco_xr.EigrpProcess;
import org.batfish.representation.cisco_xr.EigrpRedistributionPolicy;
import org.batfish.representation.cisco_xr.ExtcommunitySetRt;
import org.batfish.representation.cisco_xr.ExtcommunitySetRtElem;
import org.batfish.representation.cisco_xr.ExtcommunitySetRtElemAsColon;
import org.batfish.representation.cisco_xr.ExtcommunitySetRtElemAsDotColon;
import org.batfish.representation.cisco_xr.ExtcommunitySetRtExpr;
import org.batfish.representation.cisco_xr.ExtcommunitySetRtReference;
import org.batfish.representation.cisco_xr.FqdnNetworkObject;
import org.batfish.representation.cisco_xr.HostNetworkObject;
import org.batfish.representation.cisco_xr.HsrpGroup;
import org.batfish.representation.cisco_xr.IcmpServiceObjectGroupLine;
import org.batfish.representation.cisco_xr.IcmpTypeGroupReferenceLine;
import org.batfish.representation.cisco_xr.IcmpTypeGroupTypeLine;
import org.batfish.representation.cisco_xr.IcmpTypeObjectGroup;
import org.batfish.representation.cisco_xr.InlineExtcommunitySetRt;
import org.batfish.representation.cisco_xr.InspectClassMap;
import org.batfish.representation.cisco_xr.InspectClassMapMatch;
import org.batfish.representation.cisco_xr.InspectClassMapMatchAccessGroup;
import org.batfish.representation.cisco_xr.InspectClassMapMatchProtocol;
import org.batfish.representation.cisco_xr.InspectClassMapProtocol;
import org.batfish.representation.cisco_xr.Interface;
import org.batfish.representation.cisco_xr.IpBgpPeerGroup;
import org.batfish.representation.cisco_xr.IpsecProfile;
import org.batfish.representation.cisco_xr.IpsecTransformSet;
import org.batfish.representation.cisco_xr.Ipv4AccessList;
import org.batfish.representation.cisco_xr.Ipv4AccessListLine;
import org.batfish.representation.cisco_xr.Ipv6AccessList;
import org.batfish.representation.cisco_xr.Ipv6AccessListLine;
import org.batfish.representation.cisco_xr.Ipv6BgpPeerGroup;
import org.batfish.representation.cisco_xr.IsakmpKey;
import org.batfish.representation.cisco_xr.IsakmpPolicy;
import org.batfish.representation.cisco_xr.IsakmpProfile;
import org.batfish.representation.cisco_xr.IsisProcess;
import org.batfish.representation.cisco_xr.IsisRedistributionPolicy;
import org.batfish.representation.cisco_xr.Keyring;
import org.batfish.representation.cisco_xr.LiteralUint16;
import org.batfish.representation.cisco_xr.LiteralUint16Range;
import org.batfish.representation.cisco_xr.LiteralUint32;
import org.batfish.representation.cisco_xr.MacAccessList;
import org.batfish.representation.cisco_xr.MasterBgpPeerGroup;
import org.batfish.representation.cisco_xr.MatchSemantics;
import org.batfish.representation.cisco_xr.NamedBgpPeerGroup;
import org.batfish.representation.cisco_xr.NamedRsaPubKey;
import org.batfish.representation.cisco_xr.NetworkObjectAddressSpecifier;
import org.batfish.representation.cisco_xr.NetworkObjectGroup;
import org.batfish.representation.cisco_xr.NetworkObjectGroupAddressSpecifier;
import org.batfish.representation.cisco_xr.NetworkObjectInfo;
import org.batfish.representation.cisco_xr.NssaSettings;
import org.batfish.representation.cisco_xr.OspfNetwork;
import org.batfish.representation.cisco_xr.OspfNetworkType;
import org.batfish.representation.cisco_xr.OspfProcess;
import org.batfish.representation.cisco_xr.OspfRedistributionPolicy;
import org.batfish.representation.cisco_xr.OspfWildcardNetwork;
import org.batfish.representation.cisco_xr.Prefix6List;
import org.batfish.representation.cisco_xr.Prefix6ListLine;
import org.batfish.representation.cisco_xr.PrefixList;
import org.batfish.representation.cisco_xr.PrefixListLine;
import org.batfish.representation.cisco_xr.ProtocolObjectGroup;
import org.batfish.representation.cisco_xr.ProtocolObjectGroupProtocolLine;
import org.batfish.representation.cisco_xr.ProtocolObjectGroupReferenceLine;
import org.batfish.representation.cisco_xr.ProtocolOrServiceObjectGroupServiceSpecifier;
import org.batfish.representation.cisco_xr.RangeNetworkObject;
import org.batfish.representation.cisco_xr.RipProcess;
import org.batfish.representation.cisco_xr.RoutePolicy;
import org.batfish.representation.cisco_xr.RoutePolicyApplyStatement;
import org.batfish.representation.cisco_xr.RoutePolicyBoolean;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAnd;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanApply;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathIn;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathIsLocal;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathNeighborIs;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathOriginatesFrom;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathPassesThrough;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanDestination;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanLocalPreference;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanMed;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanNextHopIn;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanNot;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanOr;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanRibHasRoute;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanRouteTypeIs;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanTagIs;
import org.batfish.representation.cisco_xr.RoutePolicyComment;
import org.batfish.representation.cisco_xr.RoutePolicyDispositionStatement;
import org.batfish.representation.cisco_xr.RoutePolicyDispositionType;
import org.batfish.representation.cisco_xr.RoutePolicyElseBlock;
import org.batfish.representation.cisco_xr.RoutePolicyElseIfBlock;
import org.batfish.representation.cisco_xr.RoutePolicyIfStatement;
import org.batfish.representation.cisco_xr.RoutePolicyInlinePrefix6Set;
import org.batfish.representation.cisco_xr.RoutePolicyInlinePrefixSet;
import org.batfish.representation.cisco_xr.RoutePolicyNextHop;
import org.batfish.representation.cisco_xr.RoutePolicyNextHopDiscard;
import org.batfish.representation.cisco_xr.RoutePolicyNextHopIP6;
import org.batfish.representation.cisco_xr.RoutePolicyNextHopIp;
import org.batfish.representation.cisco_xr.RoutePolicyNextHopPeerAddress;
import org.batfish.representation.cisco_xr.RoutePolicyNextHopSelf;
import org.batfish.representation.cisco_xr.RoutePolicyPrefixSet;
import org.batfish.representation.cisco_xr.RoutePolicyPrefixSetName;
import org.batfish.representation.cisco_xr.RoutePolicyPrependAsPath;
import org.batfish.representation.cisco_xr.RoutePolicySetExtcommunityRt;
import org.batfish.representation.cisco_xr.RoutePolicySetIsisMetric;
import org.batfish.representation.cisco_xr.RoutePolicySetIsisMetricType;
import org.batfish.representation.cisco_xr.RoutePolicySetLevel;
import org.batfish.representation.cisco_xr.RoutePolicySetLocalPref;
import org.batfish.representation.cisco_xr.RoutePolicySetMed;
import org.batfish.representation.cisco_xr.RoutePolicySetNextHop;
import org.batfish.representation.cisco_xr.RoutePolicySetOrigin;
import org.batfish.representation.cisco_xr.RoutePolicySetOspfMetricType;
import org.batfish.representation.cisco_xr.RoutePolicySetTag;
import org.batfish.representation.cisco_xr.RoutePolicySetVarMetricType;
import org.batfish.representation.cisco_xr.RoutePolicySetWeight;
import org.batfish.representation.cisco_xr.RoutePolicyStatement;
import org.batfish.representation.cisco_xr.ServiceObject;
import org.batfish.representation.cisco_xr.ServiceObjectGroup;
import org.batfish.representation.cisco_xr.ServiceObjectGroup.ServiceProtocol;
import org.batfish.representation.cisco_xr.ServiceObjectGroupLine;
import org.batfish.representation.cisco_xr.ServiceObjectGroupReferenceServiceObjectGroupLine;
import org.batfish.representation.cisco_xr.ServiceObjectReferenceServiceObjectGroupLine;
import org.batfish.representation.cisco_xr.ServiceObjectServiceSpecifier;
import org.batfish.representation.cisco_xr.SimpleExtendedAccessListServiceSpecifier;
import org.batfish.representation.cisco_xr.StaticRoute;
import org.batfish.representation.cisco_xr.StubSettings;
import org.batfish.representation.cisco_xr.SubnetNetworkObject;
import org.batfish.representation.cisco_xr.TcpServiceObjectGroupLine;
import org.batfish.representation.cisco_xr.TcpUdpServiceObjectGroupLine;
import org.batfish.representation.cisco_xr.Tunnel;
import org.batfish.representation.cisco_xr.Tunnel.TunnelMode;
import org.batfish.representation.cisco_xr.UdpServiceObjectGroupLine;
import org.batfish.representation.cisco_xr.Uint16RangeExpr;
import org.batfish.representation.cisco_xr.Uint16Reference;
import org.batfish.representation.cisco_xr.Uint32RangeExpr;
import org.batfish.representation.cisco_xr.UnimplementedAccessListServiceSpecifier;
import org.batfish.representation.cisco_xr.Vrf;
import org.batfish.representation.cisco_xr.VrrpGroup;
import org.batfish.representation.cisco_xr.VrrpInterface;
import org.batfish.representation.cisco_xr.WildcardAddressSpecifier;
import org.batfish.representation.cisco_xr.XrCommunitySet;
import org.batfish.representation.cisco_xr.XrCommunitySetElem;
import org.batfish.representation.cisco_xr.XrCommunitySetExpr;
import org.batfish.representation.cisco_xr.XrCommunitySetHighLowRangeExprs;
import org.batfish.representation.cisco_xr.XrCommunitySetIosRegex;
import org.batfish.representation.cisco_xr.XrCommunitySetReference;
import org.batfish.representation.cisco_xr.XrInlineCommunitySet;
import org.batfish.representation.cisco_xr.XrRoutePolicyBooleanCommunityMatchesAny;
import org.batfish.representation.cisco_xr.XrRoutePolicyBooleanCommunityMatchesEvery;
import org.batfish.representation.cisco_xr.XrRoutePolicyDeleteAllStatement;
import org.batfish.representation.cisco_xr.XrRoutePolicyDeleteCommunityStatement;
import org.batfish.representation.cisco_xr.XrRoutePolicySetCommunity;
import org.batfish.vendor.VendorConfiguration;

public class CiscoXrControlPlaneExtractor extends CiscoXrParserBaseListener
    implements ControlPlaneExtractor {

  private static final int DEFAULT_STATIC_ROUTE_DISTANCE = 1;

  private static final String INLINE_SERVICE_OBJECT_NAME = "~INLINE_SERVICE_OBJECT~";

  @Override
  public void exitIf_ip_ospf_network(If_ip_ospf_networkContext ctx) {
    for (Interface iface : _currentInterfaces) {
      if (ctx.POINT_TO_POINT() != null) {
        iface.setOspfNetworkType(OspfNetworkType.POINT_TO_POINT);
      } else if (ctx.BROADCAST() != null) {
        iface.setOspfNetworkType(OspfNetworkType.BROADCAST);
      } else if (ctx.POINT_TO_MULTIPOINT() != null) {
        if (ctx.NON_BROADCAST() != null) {
          iface.setOspfNetworkType(OspfNetworkType.POINT_TO_MULTIPOINT_NON_BROADCAST);
        } else {
          iface.setOspfNetworkType(OspfNetworkType.POINT_TO_MULTIPOINT);
        }
      } else if (ctx.NON_BROADCAST() != null) {
        iface.setOspfNetworkType(OspfNetworkType.NON_BROADCAST);
      } else {
        warn(ctx, "Cannot determine OSPF network type.");
      }
    }
  }

  private static String getDescription(Description_lineContext ctx) {
    return ctx.text != null ? ctx.text.getText().trim() : "";
  }

  private static Ip6 getIp(Access_list_ip6_rangeContext ctx) {
    if (ctx.ip != null) {
      return toIp6(ctx.ip);
    } else if (ctx.ipv6_prefix != null) {
      return Prefix6.parse(ctx.ipv6_prefix.getText()).getAddress();
    } else {
      return Ip6.ZERO;
    }
  }

  private static int toInteger(TerminalNode t) {
    return Integer.parseInt(t.getText());
  }

  private static int toInteger(Token t) {
    return Integer.parseInt(t.getText());
  }

  private static int toInteger(Vlan_idContext ctx) {
    return Integer.parseInt(ctx.getText(), 10);
  }

  private static String toInterfaceName(Interface_nameContext ctx) {
    StringBuilder name =
        new StringBuilder(
            CiscoXrConfiguration.getCanonicalInterfaceNamePrefix(ctx.name_prefix_alpha.getText()));
    for (Token part : ctx.name_middle_parts) {
      name.append(part.getText());
    }
    if (ctx.range().range_list.size() != 1) {
      throw new BatfishException(
          "got interface range where single interface was expected: '" + ctx.getText() + "'");
    }
    name.append(ctx.range().getText());
    return name.toString();
  }

  private static long toAsNum(Bgp_asnContext ctx) {
    if (ctx.asn != null) {
      return toLong(ctx.asn);
    }
    String[] parts = ctx.asn4b.getText().split("\\.");
    return (Long.parseLong(parts[0]) << 16) + Long.parseLong(parts[1]);
  }

  private static Ip toIp(TerminalNode t) {
    return Ip.parse(t.getText());
  }

  private static Ip toIp(Token t) {
    return Ip.parse(t.getText());
  }

  private static Ip6 toIp6(TerminalNode t) {
    return Ip6.parse(t.getText());
  }

  private static Ip6 toIp6(Token t) {
    return Ip6.parse(t.getText());
  }

  private static long toLong(TerminalNode t) {
    return Long.parseLong(t.getText());
  }

  private static long toLong(Token t) {
    return Long.parseLong(t.getText());
  }

  private static List<SubRange> toRange(RangeContext ctx) {
    List<SubRange> range = new ArrayList<>();
    for (SubrangeContext sc : ctx.range_list) {
      SubRange sr = toSubRange(sc);
      range.add(sr);
    }
    return range;
  }

  private static SubRange toSubRange(SubrangeContext ctx) {
    int low = toInteger(ctx.low);
    if (ctx.DASH() != null) {
      int high = toInteger(ctx.high);
      return new SubRange(low, high);
    } else {
      return SubRange.singleton(low);
    }
  }

  private static String unquote(String text) {
    if (text.length() == 0) {
      return text;
    }
    char firstChar = text.charAt(0);
    if (firstChar != '"' && firstChar != '\'') {
      return text;
    }
    char lastChar = text.charAt(text.length() - 1);
    if ((firstChar == '"' && lastChar == '"') || (firstChar == '\'' && lastChar == '\'')) {
      return text.substring(1, text.length() - 1);
    } else {
      throw new BatfishException("Improperly-quoted string");
    }
  }

  private CiscoXrConfiguration _configuration;

  @SuppressWarnings("unused")
  private List<AaaAccountingCommands> _currentAaaAccountingCommands;

  private AaaAuthenticationLoginList _currentAaaAuthenticationLoginList;

  private final Set<String> _currentBlockNeighborAddressFamilies;

  private CryptoMapEntry _currentCryptoMapEntry;

  private String _currentCryptoMapName;

  private Integer _currentCryptoMapSequenceNum;

  private NamedRsaPubKey _currentNamedRsaPubKey;

  private DynamicIpBgpPeerGroup _currentDynamicIpPeerGroup;

  private DynamicIpv6BgpPeerGroup _currentDynamicIpv6PeerGroup;

  @Nullable private String _currentEigrpInterface;

  @Nullable private EigrpProcess _currentEigrpProcess;

  private ExtcommunitySetRt _currentExtcommunitySetRt;

  private List<Interface> _currentInterfaces;

  private Ipv4AccessList _currentIpv4Acl;

  private Ipv6AccessList _currentIpv6Acl;

  private IsakmpPolicy _currentIsakmpPolicy;

  private IsakmpProfile _currentIsakmpProfile;

  private IpBgpPeerGroup _currentIpPeerGroup;

  private IpsecTransformSet _currentIpsecTransformSet;

  private IpsecProfile _currentIpsecProfile;

  private Ipv6BgpPeerGroup _currentIpv6PeerGroup;

  private Interface _currentIsisInterface;

  private IsisProcess _currentIsisProcess;

  private Keyring _currentKeyring;

  private List<String> _currentLineNames;

  @SuppressWarnings("unused")
  private MacAccessList _currentMacAccessList;

  private NamedBgpPeerGroup _currentNamedPeerGroup;

  private Long _currentOspfArea;

  private String _currentOspfInterface;

  private OspfProcess _currentOspfProcess;

  private BgpPeerGroup _currentPeerGroup;

  private NamedBgpPeerGroup _currentPeerSession;

  private Prefix6List _currentPrefix6List;

  private PrefixList _currentPrefixList;

  private String _currentPrefixSetName;

  private RipProcess _currentRipProcess;

  private RoutePolicy _currentRoutePolicy;

  private ServiceObject _currentServiceObject;

  private SnmpCommunity _currentSnmpCommunity;

  @SuppressWarnings("unused")
  private SnmpHost _currentSnmpHost;

  private User _currentUser;

  private String _currentVrf;

  private VrrpGroup _currentVrrpGroup;

  private Integer _currentVrrpGroupNum;

  private String _currentVrrpInterface;

  private final @Nonnull BgpPeerGroup _dummyPeerGroup = new MasterBgpPeerGroup();

  private final ConfigurationFormat _format;

  private boolean _inBlockNeighbor;

  private boolean _inIpv6BgpPeer;

  private boolean _no;

  @Nullable private EigrpProcess _parentEigrpProcess;

  private final CiscoXrCombinedParser _parser;

  private final List<BgpPeerGroup> _peerGroupStack;

  private final String _text;

  private final Warnings _w;

  private NetworkObjectGroup _currentNetworkObjectGroup;

  private String _currentNetworkObjectName;

  private IcmpTypeObjectGroup _currentIcmpTypeObjectGroup;

  private ProtocolObjectGroup _currentProtocolObjectGroup;

  private ServiceObjectGroup _currentServiceObjectGroup;

  private InspectClassMap _currentInspectClassMap;

  private Integer _currentHsrpGroup;

  private String _currentTrackingGroup;

  /* Set this when moving to different stanzas (e.g., ro_vrf) inside "router ospf" stanza
   * to correctly retrieve the OSPF process that was being configured prior to switching stanzas
   */
  private String _lastKnownOspfProcess;

  public CiscoXrControlPlaneExtractor(
      String text, CiscoXrCombinedParser parser, ConfigurationFormat format, Warnings warnings) {
    _text = text;
    _parser = parser;
    _format = format;
    _w = warnings;
    _peerGroupStack = new ArrayList<>();
    _currentBlockNeighborAddressFamilies = new HashSet<>();
  }

  private Interface addInterface(String name, Interface_nameContext ctx, boolean explicit) {
    Interface newInterface = _configuration.getInterfaces().get(name);
    if (newInterface == null) {
      newInterface = new Interface(name, _configuration);
      _configuration.getInterfaces().put(name, newInterface);
      initInterface(newInterface, ctx);
    } else {
      _w.pedantic("Interface: '" + name + "' altered more than once");
    }
    newInterface.setDeclaredNames(
        new ImmutableSortedSet.Builder<String>(naturalOrder())
            .addAll(newInterface.getDeclaredNames())
            .add(ctx.getText())
            .build());
    if (explicit) {
      _currentInterfaces.add(newInterface);
    }
    return newInterface;
  }

  private Interface getOrAddInterface(Interface_nameContext ctx) {
    String canonicalIfaceName = getCanonicalInterfaceName(ctx.getText());
    Interface iface = _configuration.getInterfaces().get(canonicalIfaceName);
    if (iface == null) {
      iface = addInterface(canonicalIfaceName, ctx, false);
    }
    return iface;
  }

  private String convErrorMessage(Class<?> type, ParserRuleContext ctx) {
    return String.format("Could not convert to %s: %s", type.getSimpleName(), getFullText(ctx));
  }

  private BatfishException convError(Class<?> type, ParserRuleContext ctx) {
    return new BatfishException(convErrorMessage(type, ctx));
  }

  private Vrf currentVrf() {
    return initVrf(_currentVrf);
  }

  @Override
  public void enterAaa_accounting(Aaa_accountingContext ctx) {
    if (_configuration.getCf().getAaa().getAccounting() == null) {
      _configuration.getCf().getAaa().setAccounting(new AaaAccounting());
    }
  }

  @Override
  public void enterAaa_accounting_commands_line(Aaa_accounting_commands_lineContext ctx) {
    Map<String, AaaAccountingCommands> commands =
        _configuration.getCf().getAaa().getAccounting().getCommands();
    Set<String> levels = new TreeSet<>();
    if (ctx.levels != null) {
      List<SubRange> range = toRange(ctx.levels);
      for (SubRange subRange : range) {
        for (int i = subRange.getStart(); i <= subRange.getEnd(); i++) {
          String level = Integer.toString(i);
          levels.add(level);
        }
      }
    } else {
      levels.add(AaaAccounting.DEFAULT_COMMANDS);
    }
    List<AaaAccountingCommands> currentAaaAccountingCommands = new ArrayList<>();
    for (String level : levels) {
      AaaAccountingCommands c = commands.computeIfAbsent(level, k -> new AaaAccountingCommands());
      currentAaaAccountingCommands.add(c);
    }
    _currentAaaAccountingCommands = currentAaaAccountingCommands;
  }

  @Override
  public void enterAaa_accounting_default(Aaa_accounting_defaultContext ctx) {
    AaaAccounting accounting = _configuration.getCf().getAaa().getAccounting();
    if (accounting.getDefault() == null) {
      accounting.setDefault(new AaaAccountingDefault());
    }
  }

  @Override
  public void enterAaa_authentication(Aaa_authenticationContext ctx) {
    if (_configuration.getCf().getAaa().getAuthentication() == null) {
      _configuration.getCf().getAaa().setAuthentication(new AaaAuthentication());
    }
  }

  @Override
  public void enterAaa_authentication_login(Aaa_authentication_loginContext ctx) {
    if (_configuration.getCf().getAaa().getAuthentication().getLogin() == null) {
      _configuration.getCf().getAaa().getAuthentication().setLogin(new AaaAuthenticationLogin());
    }
  }

  @Override
  public void enterAaa_authentication_login_list(Aaa_authentication_login_listContext ctx) {
    AaaAuthenticationLogin login = _configuration.getCf().getAaa().getAuthentication().getLogin();
    String name;
    if (ctx.DEFAULT() != null) {
      name = AaaAuthenticationLogin.DEFAULT_LIST_NAME;
    } else if (ctx.variable() != null) {
      name = ctx.variable().getText();
    } else {
      throw new BatfishException("Unsupported mode");
    }

    List<AuthenticationMethod> methods = new ArrayList<>();

    for (Aaa_authentication_list_methodContext method : ctx.aaa_authentication_list_method()) {
      methods.add(AuthenticationMethod.toAuthenticationMethod(method.getText()));
    }

    _currentAaaAuthenticationLoginList =
        login.getLists().computeIfAbsent(name, k -> new AaaAuthenticationLoginList(methods));

    // apply the list to each line
    SortedMap<String, Line> lines = _configuration.getCf().getLines();
    for (Line line : lines.values()) {
      if (name.equals(AaaAuthenticationLogin.DEFAULT_LIST_NAME)) {
        // only apply default list if no other list applied
        if (line.getAaaAuthenticationLoginList() == null) {
          line.setAaaAuthenticationLoginList(_currentAaaAuthenticationLoginList);
          line.setLoginAuthentication(name);
        }
      } else if (line.getLoginAuthentication() != null
          && line.getLoginAuthentication().equals(name)) {
        // if not the default list, apply it to lines that have specified this list as it's login
        // list
        line.setAaaAuthenticationLoginList(_currentAaaAuthenticationLoginList);
      }
    }
  }

  @Override
  public void enterIpv4_access_list(Ipv4_access_listContext ctx) {
    String name = ctx.name.getText();
    _currentIpv4Acl = _configuration.getIpv4Acls().computeIfAbsent(name, Ipv4AccessList::new);
    _configuration.defineStructure(IPV4_ACCESS_LIST, name, ctx);
  }

  @Override
  public void exitIpv4_access_list(Ipv4_access_listContext ctx) {
    _currentIpv4Acl = null;
  }

  @Override
  public void enterIpv6_access_list(Ipv6_access_listContext ctx) {
    String name = ctx.name.getText();
    _currentIpv6Acl = _configuration.getIpv6Acls().computeIfAbsent(name, Ipv6AccessList::new);
    _configuration.defineStructure(IPV6_ACCESS_LIST, name, ctx);
  }

  @Override
  public void exitIpv6_access_list(Ipv6_access_listContext ctx) {
    _currentIpv6Acl = null;
  }

  @Override
  public void enterRec_address_family(Rec_address_familyContext ctx) {
    // Step into a new address family. This results in a new EIGRP process with a specified VRF and
    // AS number

    // There may not be an ASN specified here, but it will be specified in this AF context
    Long asn = ctx.asnum == null ? null : toLong(ctx.asnum);

    EigrpProcess proc = new EigrpProcess(asn, EigrpProcessMode.CLASSIC, ctx.vrf.getText());

    _parentEigrpProcess = _currentEigrpProcess;
    _currentEigrpProcess = proc;
  }

  @Override
  public void enterRen_address_family(Ren_address_familyContext ctx) {
    // Step into a new address family. This results in a new EIGRP process with a specified VRF and
    // AS number

    long asn = toLong(ctx.asnum);

    if (ctx.IPV6() != null) {
      todo(ctx);
    }
    if (ctx.MULTICAST() != null) {
      todo(ctx);
    }

    String vrfName = ctx.vrf == null ? _currentVrf : ctx.vrf.getText();
    _currentEigrpProcess = new EigrpProcess(asn, EigrpProcessMode.NAMED, vrfName);
  }

  @Override
  public void enterAddress_family_header(Address_family_headerContext ctx) {
    String addressFamilyStr = ctx.addressFamilyStr;
    if (_inBlockNeighbor) {
      if (_currentBlockNeighborAddressFamilies.contains(addressFamilyStr)) {
        popPeer();
        _inBlockNeighbor = false;
        _currentBlockNeighborAddressFamilies.clear();
      } else {
        _currentBlockNeighborAddressFamilies.add(addressFamilyStr);
      }
    }
    Bgp_address_familyContext af = ctx.af;
    if (af.VPNV4() != null || af.VPNV6() != null || af.MDT() != null || af.MULTICAST() != null) {
      pushPeer(_dummyPeerGroup);
    } else {
      pushPeer(_currentPeerGroup);
    }
    if (af.IPV6() != null || af.VPNV6() != null) {
      _inIpv6BgpPeer = true;
    }
  }

  @Override
  public void enterAf_group_rb_stanza(Af_group_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String name = ctx.name.getText();
    NamedBgpPeerGroup afGroup = proc.getAfGroups().get(name);
    if (afGroup == null) {
      afGroup = proc.addNamedPeerGroup(name);
    }
    pushPeer(afGroup);
    _currentNamedPeerGroup = afGroup;
    _configuration.defineStructure(BGP_AF_GROUP, name, ctx);
  }

  @Override
  public void enterBgp_confederation_rb_stanza(Bgp_confederation_rb_stanzaContext ctx) {
    todo(ctx);
  }

  @Override
  public void enterCip_profile(Cip_profileContext ctx) {
    if (_currentIpsecProfile != null) {
      throw new BatfishException("IpsecProfile should be null!");
    }
    _currentIpsecProfile = new IpsecProfile(ctx.name.getText());
    _configuration.defineStructure(IPSEC_PROFILE, ctx.name.getText(), ctx);
  }

  @Override
  public void enterCip_transform_set(Cip_transform_setContext ctx) {
    if (_currentIpsecTransformSet != null) {
      throw new BatfishException("IpsecTransformSet should be null!");
    }
    _currentIpsecTransformSet = new IpsecTransformSet(ctx.name.getText());
    _configuration.defineStructure(IPSEC_TRANSFORM_SET, ctx.name.getText(), ctx);
    if (ctx.ipsec_encryption() != null) {
      _currentIpsecTransformSet.setEncryptionAlgorithm(
          toEncryptionAlgorithm(ctx.ipsec_encryption()));
    }
    // If any encryption algorithm was set then ESP protocol is used
    if (_currentIpsecTransformSet.getEncryptionAlgorithm() != null) {
      _currentIpsecTransformSet.getProtocols().add(IpsecProtocol.ESP);
    }

    if (ctx.ipsec_authentication() != null) {
      _currentIpsecTransformSet.setAuthenticationAlgorithm(
          toIpsecAuthenticationAlgorithm(ctx.ipsec_authentication()));
      _currentIpsecTransformSet.getProtocols().add(toProtocol(ctx.ipsec_authentication()));
    }
  }

  @Override
  public void enterCkp_named_key(Ckp_named_keyContext ctx) {
    String keyName = ctx.name.getText();
    _currentNamedRsaPubKey =
        _configuration.getCryptoNamedRsaPubKeys().computeIfAbsent(keyName, NamedRsaPubKey::new);
    _configuration.defineStructure(NAMED_RSA_PUB_KEY, keyName, ctx);
    /* RSA pub keys are dynamically matched and not explicitly referenced, so adding a
    self-reference here */
    _configuration.referenceStructure(
        NAMED_RSA_PUB_KEY, keyName, NAMED_RSA_PUB_KEY_SELF_REF, ctx.name.start.getLine());
  }

  @Override
  public void enterCkpn_address(Ckpn_addressContext ctx) {
    if (ctx.NO() != null) {
      return;
    }
    _currentNamedRsaPubKey.setAddress(toIp(ctx.ip_address));
  }

  @Override
  public void enterCkpn_key_string(Ckpn_key_stringContext ctx) {
    if (ctx.NO() != null) {
      return;
    }
    _currentNamedRsaPubKey.setKey(
        CommonUtil.sha256Digest(ctx.certificate().getText() + CommonUtil.salt()));
  }

  @Override
  public void exitCkp_named_key(Ckp_named_keyContext ctx) {
    _currentNamedRsaPubKey = null;
  }

  private IpsecProtocol toProtocol(Ipsec_authenticationContext ctx) {
    if (ctx.ESP_MD5_HMAC() != null
        || ctx.ESP_SHA256_HMAC() != null
        || ctx.ESP_SHA512_HMAC() != null
        || ctx.ESP_SHA_HMAC() != null) {
      return IpsecProtocol.ESP;
    } else if (ctx.AH_SHA_HMAC() != null || ctx.AH_MD5_HMAC() != null) {
      return IpsecProtocol.AH;
    } else {
      throw convError(IpsecProtocol.class, ctx);
    }
  }

  @Override
  public void exitCipprf_set_isakmp_profile(Cipprf_set_isakmp_profileContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _currentIpsecProfile.setIsakmpProfile(name);
    _configuration.referenceStructure(ISAKMP_PROFILE, name, IPSEC_PROFILE_ISAKMP_PROFILE, line);
  }

  @Override
  public void exitCipprf_set_pfs(Cipprf_set_pfsContext ctx) {
    _currentIpsecProfile.setPfsGroup(toDhGroup(ctx.dh_group()));
  }

  @Override
  public void exitCipprf_set_transform_set(Cipprf_set_transform_setContext ctx) {
    for (VariableContext transform : ctx.transforms) {
      int line = transform.getStart().getLine();
      String name = transform.getText();
      _currentIpsecProfile.getTransformSets().add(name);
      _configuration.referenceStructure(
          IPSEC_TRANSFORM_SET, name, IPSEC_PROFILE_TRANSFORM_SET, line);
    }
  }

  @Override
  public void exitCipt_mode(Cipt_modeContext ctx) {
    if (ctx.TRANSPORT() != null) {
      _currentIpsecTransformSet.setIpsecEncapsulationMode(IpsecEncapsulationMode.TRANSPORT);
    } else if (ctx.TUNNEL() != null) {
      _currentIpsecTransformSet.setIpsecEncapsulationMode(IpsecEncapsulationMode.TUNNEL);
    } else {
      throw new BatfishException("Unsupported mode " + ctx.getText());
    }
  }

  @Override
  public void enterCis_key(Cis_keyContext ctx) {
    int encType = ctx.DEC() != null ? toInteger(ctx.DEC()) : 0;
    IkeKeyType ikeKeyType;
    if (encType == 0) {
      ikeKeyType = IkeKeyType.PRE_SHARED_KEY_UNENCRYPTED;
    } else if (encType == 6) {
      ikeKeyType = IkeKeyType.PRE_SHARED_KEY_ENCRYPTED;
    } else {
      _w.addWarning(
          ctx,
          getFullText(ctx),
          _parser,
          String.format("%s is not a valid encryption type", encType));
      return;
    }
    String psk = ctx.key.getText();
    Ip wildCardMask = ctx.wildcard_mask == null ? Ip.MAX : toIp(ctx.wildcard_mask);
    _configuration
        .getIsakmpKeys()
        .add(
            new IsakmpKey(
                IpWildcard.ipWithWildcardMask(toIp(ctx.ip_address), wildCardMask.inverted())
                    .toIpSpace(),
                ikeKeyType == IkeKeyType.PRE_SHARED_KEY_UNENCRYPTED
                    ? CommonUtil.sha256Digest(psk + CommonUtil.salt())
                    : psk,
                ikeKeyType));
  }

  @Override
  public void enterCis_policy(Cis_policyContext ctx) {
    Integer priority = toInteger(ctx.priority);
    _currentIsakmpPolicy = new IsakmpPolicy(priority);
    _currentIsakmpPolicy.setHashAlgorithm(IkeHashingAlgorithm.SHA1);
    _currentIsakmpPolicy.setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC);
    _currentIsakmpPolicy.setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS);
    _currentIsakmpPolicy.setDiffieHellmanGroup(DiffieHellmanGroup.GROUP2);

    _currentIsakmpPolicy.setLifetimeSeconds(86400);
    _configuration.defineStructure(ISAKMP_POLICY, priority.toString(), ctx);
    /* Isakmp policies are checked in order not explicitly referenced, so add a self-reference
    here */
    _configuration.referenceStructure(
        ISAKMP_POLICY, priority.toString(), ISAKMP_POLICY_SELF_REF, ctx.priority.getLine());
  }

  @Override
  public void enterCis_profile(Cis_profileContext ctx) {
    _currentIsakmpProfile = new IsakmpProfile(ctx.name.getText());
    _configuration.defineStructure(ISAKMP_PROFILE, ctx.name.getText(), ctx);
    /* Isakmp profiles are checked against for matches not explicitly referenced, so add a
    self-reference here */
    _configuration.referenceStructure(
        ISAKMP_PROFILE, ctx.name.getText(), ISAKMP_PROFILE_SELF_REF, ctx.name.start.getLine());
  }

  @Override
  public void enterCisco_xr_configuration(Cisco_xr_configurationContext ctx) {
    _configuration = new CiscoXrConfiguration();
    _configuration.setVendor(_format);
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
    if (_format == CISCO_IOS) {
      Logging logging = new Logging();
      logging.setOn(true);
      _configuration.getCf().setLogging(logging);
    }
  }

  @Override
  public void exitCispol_authentication(Cispol_authenticationContext ctx) {
    if (ctx.PRE_SHARE() != null) {
      _currentIsakmpPolicy.setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS);
    } else if (ctx.RSA_ENCR() != null) {
      _currentIsakmpPolicy.setAuthenticationMethod(IkeAuthenticationMethod.RSA_ENCRYPTED_NONCES);
    } else if (ctx.RSA_SIG() != null) {
      _currentIsakmpPolicy.setAuthenticationMethod(IkeAuthenticationMethod.RSA_SIGNATURES);
    } else {
      throw new BatfishException("Unsupported authentication method in " + ctx.getText());
    }
  }

  @Override
  public void exitCispol_encryption(Cispol_encryptionContext ctx) {
    _currentIsakmpPolicy.setEncryptionAlgorithm(toEncryptionAlgorithm(ctx.ike_encryption()));
  }

  @Override
  public void exitCispol_group(Cispol_groupContext ctx) {
    int group = Integer.parseInt(ctx.DEC().getText());
    _currentIsakmpPolicy.setDiffieHellmanGroup(DiffieHellmanGroup.fromGroupNumber(group));
  }

  @Override
  public void exitCispol_hash(Cispol_hashContext ctx) {
    if (ctx.MD5() != null) {
      _currentIsakmpPolicy.setHashAlgorithm(IkeHashingAlgorithm.MD5);
    } else if (ctx.SHA() != null) {
      _currentIsakmpPolicy.setHashAlgorithm(IkeHashingAlgorithm.SHA1);
    } else if (ctx.SHA2_256_128() != null) {
      _currentIsakmpPolicy.setHashAlgorithm(IkeHashingAlgorithm.SHA_256);
    } else {
      throw new BatfishException("Unsupported authentication method in " + ctx.getText());
    }
  }

  @Override
  public void exitCispol_lifetime(Cispol_lifetimeContext ctx) {
    _currentIsakmpPolicy.setLifetimeSeconds(Integer.parseInt(ctx.DEC().getText()));
  }

  @Override
  public void exitCisprf_keyring(Cisprf_keyringContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _currentIsakmpProfile.setKeyring(name);
    _configuration.referenceStructure(KEYRING, name, ISAKMP_PROFILE_KEYRING, line);
  }

  @Override
  public void exitCisprf_match(Cisprf_matchContext ctx) {
    Ip mask = (ctx.mask == null) ? Ip.parse("255.255.255.255") : toIp(ctx.mask);
    _currentIsakmpProfile.setMatchIdentity(
        IpWildcard.ipWithWildcardMask(toIp(ctx.address), mask.inverted()));
  }

  @Override
  public void exitCisprf_self_identity(Cisprf_self_identityContext ctx) {
    _currentIsakmpProfile.setSelfIdentity(toIp(ctx.IP_ADDRESS()));
  }

  @Override
  public void exitCisprf_local_address(Cisprf_local_addressContext ctx) {
    if (ctx.IP_ADDRESS() != null) {
      _currentIsakmpProfile.setLocalAddress(toIp(ctx.IP_ADDRESS()));
    } else {
      _currentIsakmpProfile.setLocalInterfaceName(ctx.iname.getText());
    }
  }

  @Override
  public void exitCkr_local_address(Ckr_local_addressContext ctx) {
    if (ctx.IP_ADDRESS() != null) {
      _currentKeyring.setLocalAddress(toIp(ctx.IP_ADDRESS()));
    } else {
      _currentKeyring.setLocalInterfaceName(ctx.iname.getText());
    }
  }

  @Override
  public void exitCkr_psk(Ckr_pskContext ctx) {
    Ip wildCardMask = ctx.wildcard_mask == null ? Ip.MAX : toIp(ctx.wildcard_mask);
    _currentKeyring.setKey(
        CommonUtil.sha256Digest(ctx.variable_permissive().getText() + CommonUtil.salt()));
    _currentKeyring.setRemoteIdentity(
        IpWildcard.ipWithWildcardMask(toIp(ctx.ip_address), wildCardMask.inverted()));
  }

  @Override
  public void enterCommunity_set_stanza(Community_set_stanzaContext ctx) {
    String name = ctx.name.getText();
    _configuration.defineStructure(COMMUNITY_SET, name, ctx);
    _configuration
        .getCommunitySets()
        .computeIfAbsent(
            name,
            n ->
                new XrCommunitySet(
                    ctx.community_set_elem_list().elems.stream()
                        .map(this::toCommunitySetElemExpr)
                        .filter(Objects::nonNull)
                        .collect(ImmutableList.toImmutableList())));
  }

  @Override
  public void enterExtcommunity_set_rt(Extcommunity_set_rtContext ctx) {
    String name = ctx.name.getText();
    _configuration.defineStructure(EXTCOMMUNITY_SET_RT, name, ctx);
    _currentExtcommunitySetRt =
        _configuration.getExtcommunitySetRts().computeIfAbsent(name, n -> new ExtcommunitySetRt());
  }

  @Override
  public void exitExtcommunity_set_rt(Extcommunity_set_rtContext ctx) {
    _currentExtcommunitySetRt = null;
  }

  @Override
  public void exitExtcommunity_set_rt_elem_lines(Extcommunity_set_rt_elem_linesContext ctx) {
    for (Extcommunity_set_rt_elemContext elem : ctx.elems) {
      @Nullable ExtcommunitySetRtElem rtElem = toExtcommunitySetRtElemExpr(elem);
      if (rtElem == null) {
        continue;
      }
      _currentExtcommunitySetRt.addElement(rtElem);
    }
  }

  @Override
  public void enterCrypto_keyring(Crypto_keyringContext ctx) {
    if (_currentKeyring != null) {
      throw new BatfishException("Keyring should be null!");
    }
    _currentKeyring = new Keyring(ctx.name.getText());
    _configuration.defineStructure(KEYRING, ctx.name.getText(), ctx);
  }

  @Override
  public void enterCrypto_map(Crypto_mapContext ctx) {
    _currentCryptoMapName = ctx.name.getText();
    if (ctx.seq_num != null) {
      _currentCryptoMapSequenceNum = toInteger(ctx.seq_num);
    }
  }

  @Override
  public void enterCrypto_dynamic_map(Crypto_dynamic_mapContext ctx) {
    String name = ctx.name.getText();

    _currentCryptoMapEntry = new CryptoMapEntry(name, toInteger(ctx.seq_num));
    _currentCryptoMapEntry.setDynamic(true);

    CryptoMapSet cryptoMapSet = _configuration.getCryptoMapSets().get(name);
    // if this is the first crypto map entry in the crypto map set
    if (cryptoMapSet == null) {
      cryptoMapSet = new CryptoMapSet();
      cryptoMapSet.setDynamic(true);
      _configuration.getCryptoMapSets().put(name, cryptoMapSet);
      _configuration.defineStructure(CRYPTO_DYNAMIC_MAP_SET, name, ctx);
    } else if (!cryptoMapSet.getDynamic()) {
      warn(
          ctx,
          String.format("Cannot add dynamic crypto map entry %s to a static crypto map set", name));
      return;
    }
    cryptoMapSet.getCryptoMapEntries().add(_currentCryptoMapEntry);
  }

  @Override
  public void enterCrypto_map_t_ipsec_isakmp(Crypto_map_t_ipsec_isakmpContext ctx) {
    _currentCryptoMapEntry =
        new CryptoMapEntry(_currentCryptoMapName, _currentCryptoMapSequenceNum);

    CryptoMapSet cryptoMapSet = _configuration.getCryptoMapSets().get(_currentCryptoMapName);
    // if this is the first crypto map entry in the crypto map set
    if (cryptoMapSet == null) {
      cryptoMapSet = new CryptoMapSet();
      _configuration.getCryptoMapSets().put(_currentCryptoMapName, cryptoMapSet);
      _configuration.defineStructure(CRYPTO_MAP_SET, _currentCryptoMapName, ctx);
    } else if (cryptoMapSet.getDynamic()) {
      warn(
          ctx,
          String.format(
              "Cannot add static crypto map entry %s to a dynamic crypto map set",
              _currentCryptoMapName));
      return;
    }

    if (ctx.crypto_dynamic_map_name != null) {
      String name = ctx.crypto_dynamic_map_name.getText();
      _currentCryptoMapEntry.setReferredDynamicMapSet(name);
      _configuration.referenceStructure(
          CRYPTO_DYNAMIC_MAP_SET,
          name,
          CRYPTO_MAP_IPSEC_ISAKMP_CRYPTO_DYNAMIC_MAP_SET,
          ctx.getStart().getLine());
    }

    cryptoMapSet.getCryptoMapEntries().add(_currentCryptoMapEntry);
  }

  @Override
  public void enterIf_description(If_descriptionContext ctx) {
    String description = getDescription(ctx.description_line());
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setDescription(description);
    }
  }

  @Override
  public void enterIf_ip_forward(If_ip_forwardContext ctx) {
    if (ctx.NO() != null) {
      todo(ctx);
    }
  }

  @Override
  public void enterIf_ip_igmp(If_ip_igmpContext ctx) {
    _no = (ctx.NO() != null);
  }

  @Override
  public void enterIf_spanning_tree(If_spanning_treeContext ctx) {
    _no = ctx.NO() != null;
  }

  @Override
  public void enterIf_standby(If_standbyContext ctx) {
    _no = ctx.NO() != null;
  }

  @Override
  public void exitIf_standby(If_standbyContext ctx) {
    _no = false;
  }

  @Override
  public void enterStandby_group(Standby_groupContext ctx) {
    int group = toInteger(ctx.group);
    _currentHsrpGroup = group;
    _currentInterfaces.forEach(i -> i.getHsrpGroups().computeIfAbsent(group, HsrpGroup::new));
  }

  @Override
  public void exitStandby_group(Standby_groupContext ctx) {
    _currentHsrpGroup = null;
  }

  @Override
  public void exitStandby_version(Standby_versionContext ctx) {
    if (!_no) {
      _currentInterfaces.forEach(i -> i.setHsrpVersion(ctx.version.getText()));
    } else {
      _currentInterfaces.forEach(i -> i.setHsrpVersion(null));
    }
  }

  @Override
  public void exitStandby_group_authentication(Standby_group_authenticationContext ctx) {
    String rawAuthenticationString = ctx.auth.getText();
    _currentInterfaces.forEach(
        i ->
            i.getHsrpGroups()
                .get(_currentHsrpGroup)
                .setAuthentication(CommonUtil.sha256Digest(rawAuthenticationString)));
  }

  @Override
  public void exitStandby_group_ip(Standby_group_ipContext ctx) {
    Ip ip = toIp(ctx.ip);
    _currentInterfaces.forEach(i -> i.getHsrpGroups().get(_currentHsrpGroup).setIp(ip));
  }

  @Override
  public void exitStandby_group_preempt(Standby_group_preemptContext ctx) {
    _currentInterfaces.forEach(i -> i.getHsrpGroups().get(_currentHsrpGroup).setPreempt(!_no));
  }

  @Override
  public void exitStandby_group_priority(Standby_group_priorityContext ctx) {
    int priority =
        _no ? org.batfish.datamodel.hsrp.HsrpGroup.DEFAULT_PRIORITY : toInteger(ctx.priority);
    _currentInterfaces.forEach(i -> i.getHsrpGroups().get(_currentHsrpGroup).setPriority(priority));
  }

  @Override
  public void exitStandby_group_timers(Standby_group_timersContext ctx) {
    int helloTime;
    int holdTime;
    if (_no) {
      helloTime = org.batfish.datamodel.hsrp.HsrpGroup.DEFAULT_HELLO_TIME;
      holdTime = org.batfish.datamodel.hsrp.HsrpGroup.DEFAULT_HOLD_TIME;
    } else {
      helloTime =
          ctx.hello_ms != null ? toInteger(ctx.hello_ms) : (toInteger(ctx.hello_sec) * 1000);
      holdTime = ctx.hold_ms != null ? toInteger(ctx.hold_ms) : (toInteger(ctx.hold_sec) * 1000);
    }
    _currentInterfaces.forEach(
        i -> {
          HsrpGroup hsrpGroup = i.getHsrpGroups().get(_currentHsrpGroup);
          hsrpGroup.setHelloTime(helloTime);
          hsrpGroup.setHoldTime(holdTime);
        });
  }

  @Override
  public void exitStandby_group_track(Standby_group_trackContext ctx) {
    String trackingGroup = ctx.group.getText();
    _configuration.referenceStructure(
        TRACK, trackingGroup, INTERFACE_STANDBY_TRACK, ctx.group.getLine());
    TrackAction trackAction = toTrackAction(ctx.track_action());
    if (trackAction == null) {
      return;
    }
    _currentInterfaces.stream()
        .map(i -> i.getHsrpGroups().get(_currentHsrpGroup).getTrackActions())
        .forEach(
            trackActions -> {
              if (_no) {
                // 'no' version of command only operates if rest of line matches existing setting
                if (trackAction.equals(trackActions.get(trackingGroup))) {
                  trackActions.remove(trackingGroup);
                }
              } else {
                trackActions.put(trackingGroup, trackAction);
              }
            });
  }

  private @Nullable TrackAction toTrackAction(Track_actionContext ctx) {
    if (ctx.track_action_decrement() != null) {
      int subtrahend = toInteger(ctx.track_action_decrement().subtrahend);
      return new DecrementPriority(subtrahend);
    } else {
      return convProblem(TrackAction.class, ctx, null);
    }
  }

  @Override
  public void enterIf_vrrp(If_vrrpContext ctx) {
    _currentVrrpGroupNum = toInteger(ctx.groupnum);
  }

  @Override
  public void enterInterface_is_stanza(Interface_is_stanzaContext ctx) {
    Interface iface = getOrAddInterface(ctx.iname);
    iface.setIsisInterfaceMode(IsisInterfaceMode.ACTIVE);
    _currentIsisInterface = iface;
  }

  @Override
  public void enterIp_prefix_list_stanza(Ip_prefix_list_stanzaContext ctx) {
    String name = ctx.name.getText();
    _currentPrefixList = _configuration.getPrefixLists().computeIfAbsent(name, PrefixList::new);
    _configuration.defineStructure(PREFIX_LIST, name, ctx);
  }

  @Override
  public void enterIp_route_stanza(Ip_route_stanzaContext ctx) {
    if (ctx.vrf != null) {
      _currentVrf = ctx.vrf.getText();
    }
    if (ctx.MANAGEMENT() != null) {
      _currentVrf = CiscoXrConfiguration.MANAGEMENT_VRF_NAME;
    }
  }

  @Override
  public void enterIpv6_prefix_list_stanza(Ipv6_prefix_list_stanzaContext ctx) {
    String name = ctx.name.getText();
    _currentPrefix6List = _configuration.getPrefix6Lists().computeIfAbsent(name, Prefix6List::new);
    _configuration.defineStructure(PREFIX6_LIST, name, ctx);
  }

  @Override
  public void enterIs_type_is_stanza(Is_type_is_stanzaContext ctx) {
    IsisProcess proc = currentVrf().getIsisProcess();
    if (ctx.LEVEL_1() != null) {
      proc.setLevel(IsisLevel.LEVEL_1);
    } else if (ctx.LEVEL_2_ONLY() != null || ctx.LEVEL_2() != null) {
      proc.setLevel(IsisLevel.LEVEL_2);
    } else {
      throw new BatfishException("Unsupported is-type");
    }
  }

  @Override
  public void enterLogging_address(Logging_addressContext ctx) {
    if (_no) {
      return;
    }
    Logging logging = _configuration.getCf().getLogging();
    String hostname = ctx.hostname.getText();
    LoggingHost host = new LoggingHost(hostname);
    logging.getHosts().put(hostname, host);
  }

  @Override
  public void enterNeighbor_block_rb_stanza(Neighbor_block_rb_stanzaContext ctx) {
    _currentBlockNeighborAddressFamilies.clear();
    _inBlockNeighbor = true;
    // do no further processing for unsupported address families / containers
    if (_currentPeerGroup == _dummyPeerGroup) {
      pushPeer(_dummyPeerGroup);
      return;
    }
    BgpProcess proc = currentVrf().getBgpProcess();
    if (ctx.ip_address != null) {
      Ip ip = toIp(ctx.ip_address);
      _currentIpPeerGroup = proc.getIpPeerGroups().get(ip);
      if (_currentIpPeerGroup == null) {
        _currentIpPeerGroup = proc.addIpPeerGroup(ip);
      } else {
        warn(ctx, "Duplicate IP peer group in neighbor config.");
      }
      pushPeer(_currentIpPeerGroup);
    } else if (ctx.ip_prefix != null) {
      Prefix prefix = Prefix.parse(ctx.ip_prefix.getText());
      _currentDynamicIpPeerGroup = proc.getDynamicIpPeerGroups().get(prefix);
      if (_currentDynamicIpPeerGroup == null) {
        _currentDynamicIpPeerGroup = proc.addDynamicIpPeerGroup(prefix);
      } else {
        warn(ctx, "Duplicate DynamicIP peer group neighbor config.");
      }
      pushPeer(_currentDynamicIpPeerGroup);
    } else if (ctx.ipv6_address != null) {
      Ip6 ip6 = toIp6(ctx.ipv6_address);
      Ipv6BgpPeerGroup pg = proc.getIpv6PeerGroups().get(ip6);
      if (pg == null) {
        pg = proc.addIpv6PeerGroup(ip6);
      } else {
        warn(ctx, "Duplicate IPV6 peer group in neighbor config.");
      }
      pushPeer(pg);
      _currentIpv6PeerGroup = pg;
    } else if (ctx.ipv6_prefix != null) {
      Prefix6 prefix6 = Prefix6.parse(ctx.ipv6_prefix.getText());
      DynamicIpv6BgpPeerGroup pg = proc.getDynamicIpv6PeerGroups().get(prefix6);
      if (pg == null) {
        pg = proc.addDynamicIpv6PeerGroup(prefix6);
      } else {
        warn(ctx, "Duplicate Dynamic Ipv6 peer group neighbor config.");
      }
      pushPeer(pg);
      _currentDynamicIpv6PeerGroup = pg;
    }
    if (ctx.bgp_asn() != null) {
      long remoteAs = toAsNum(ctx.bgp_asn());
      _currentPeerGroup.setRemoteAs(remoteAs);
    }
    _currentPeerGroup.setActive(true);
    _currentPeerGroup.setShutdown(false);
  }

  @Override
  public void enterNeighbor_group_rb_stanza(Neighbor_group_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String name = ctx.name.getText();
    _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
    if (_currentNamedPeerGroup == null) {
      _currentNamedPeerGroup = proc.addNamedPeerGroup(name);
    }
    pushPeer(_currentNamedPeerGroup);
    _configuration.defineStructure(BGP_NEIGHBOR_GROUP, name, ctx);
  }

  @Override
  public void enterNet_is_stanza(Net_is_stanzaContext ctx) {
    IsisProcess proc = currentVrf().getIsisProcess();
    IsoAddress isoAddress = new IsoAddress(ctx.ISO_ADDRESS().getText());
    proc.setNetAddress(isoAddress);
  }

  @Override
  public void exitNo_ipv4_access_list(No_ipv4_access_listContext ctx) {
    String name = ctx.name.getText();
    _configuration.getIpv4Acls().remove(name);
  }

  @Override
  public void exitNo_ipv6_access_list(No_ipv6_access_listContext ctx) {
    String name = ctx.name.getText();
    _configuration.getIpv6Acls().remove(name);
  }

  @Override
  public void enterO_network(O_networkContext ctx) {
    _currentNetworkObjectName = ctx.name.getText();
    _configuration
        .getNetworkObjectInfos()
        .putIfAbsent(_currentNetworkObjectName, new NetworkObjectInfo(_currentNetworkObjectName));

    _configuration.defineStructure(NETWORK_OBJECT, _currentNetworkObjectName, ctx);
  }

  @Override
  public void exitO_network(O_networkContext ctx) {
    _currentNetworkObjectName = null;
  }

  @Override
  public void enterO_service(O_serviceContext ctx) {
    String name = ctx.name.getText();
    _currentServiceObject =
        _configuration.getServiceObjects().computeIfAbsent(name, ServiceObject::new);
    _configuration.defineStructure(SERVICE_OBJECT, name, ctx);
  }

  @Override
  public void exitO_service(O_serviceContext ctx) {
    _currentServiceObject = null;
  }

  @Override
  public void enterOg_icmp_type(Og_icmp_typeContext ctx) {
    String name = ctx.name.getText();
    if (_configuration.getObjectGroups().containsKey(name)) {
      _currentIcmpTypeObjectGroup = new IcmpTypeObjectGroup(name);
      warnObjectGroupRedefinition(ctx.name);
    } else {
      _currentIcmpTypeObjectGroup =
          _configuration.getIcmpTypeObjectGroups().computeIfAbsent(name, IcmpTypeObjectGroup::new);
      _configuration.getObjectGroups().put(name, _currentIcmpTypeObjectGroup);
      _configuration.defineStructure(ICMP_TYPE_OBJECT_GROUP, name, ctx);
    }
  }

  @Override
  public void exitOgit_icmp_object(Ogit_icmp_objectContext ctx) {
    _currentIcmpTypeObjectGroup
        .getLines()
        .add(new IcmpTypeGroupTypeLine(toIcmpType(ctx.icmp_object_type())));
  }

  @Override
  public void exitOgit_group_object(Ogit_group_objectContext ctx) {
    addIcmpTypeGroupReference(ctx.name);
  }

  @Override
  public void exitOg_icmp_type(Og_icmp_typeContext ctx) {
    _currentIcmpTypeObjectGroup = null;
  }

  @Override
  public void enterOgg_icmp_type(Ogg_icmp_typeContext ctx) {
    String name = ctx.name.getText();
    _currentIcmpTypeObjectGroup = new IcmpTypeObjectGroup(name);
    if (_configuration.getObjectGroups().containsKey(name)) {
      warnObjectGroupRedefinition(ctx.name);
    } else {
      _configuration.getIcmpTypeObjectGroups().put(name, _currentIcmpTypeObjectGroup);
      _configuration.getObjectGroups().put(name, _currentIcmpTypeObjectGroup);
      _configuration.defineStructure(ICMP_TYPE_OBJECT_GROUP, name, ctx);
    }
  }

  @Override
  public void exitOggit_group_object(Oggit_group_objectContext ctx) {
    addIcmpTypeGroupReference(ctx.name);
  }

  @Override
  public void exitOgg_icmp_type(Ogg_icmp_typeContext ctx) {
    _currentIcmpTypeObjectGroup = null;
  }

  private void addIcmpTypeGroupReference(Variable_permissiveContext nameCtx) {
    String name = nameCtx.getText();
    _currentIcmpTypeObjectGroup.getLines().add(new IcmpTypeGroupReferenceLine(name));
    _configuration.referenceStructure(
        ICMP_TYPE_OBJECT_GROUP, name, ICMP_TYPE_OBJECT_GROUP_GROUP_OBJECT, nameCtx.start.getLine());
  }

  @Override
  public void enterOgg_network(Ogg_networkContext ctx) {
    String name = ctx.name.getText();
    _currentNetworkObjectGroup = new NetworkObjectGroup(name);
    if (_configuration.getObjectGroups().containsKey(name)) {
      warnObjectGroupRedefinition(ctx.name);
    } else {
      _configuration.getNetworkObjectGroups().put(name, _currentNetworkObjectGroup);
      _configuration.getObjectGroups().put(name, _currentNetworkObjectGroup);
      _configuration.defineStructure(NETWORK_OBJECT_GROUP, name, ctx);
    }
  }

  @Override
  public void exitOggn_group_object(Oggn_group_objectContext ctx) {
    String name = ctx.name.getText();
    _currentNetworkObjectGroup.getLines().add(new IpSpaceReference(name));
    _configuration.referenceStructure(
        NETWORK_OBJECT_GROUP, name, NETWORK_OBJECT_GROUP_GROUP_OBJECT, ctx.name.start.getLine());
  }

  @Override
  public void exitOgg_network(Ogg_networkContext ctx) {
    _currentNetworkObjectGroup = null;
  }

  @Override
  public void enterOgg_protocol(Ogg_protocolContext ctx) {
    String name = ctx.name.getText();
    _currentProtocolObjectGroup = new ProtocolObjectGroup(name);
    if (_configuration.getObjectGroups().containsKey(name)) {
      warnObjectGroupRedefinition(ctx.name);
    } else {
      _configuration.getProtocolObjectGroups().put(name, _currentProtocolObjectGroup);
      _configuration.getObjectGroups().put(name, _currentProtocolObjectGroup);
      _configuration.defineStructure(PROTOCOL_OBJECT_GROUP, name, ctx);
    }
  }

  @Override
  public void exitOggp_group_object(Oggp_group_objectContext ctx) {
    String name = ctx.name.getText();
    _currentProtocolObjectGroup.getLines().add(new ProtocolObjectGroupReferenceLine(name));
    _configuration.referenceStructure(
        PROTOCOL_OBJECT_GROUP,
        name,
        EXTENDED_ACCESS_LIST_PROTOCOL_OBJECT_GROUP,
        ctx.name.start.getLine());
  }

  @Override
  public void exitOgg_protocol(Ogg_protocolContext ctx) {
    _currentProtocolObjectGroup = null;
  }

  @Override
  public void enterOgg_service(Ogg_serviceContext ctx) {
    String name = ctx.name.getText();
    ServiceProtocol protocol = toServiceProtocol(ctx.protocol_type);
    _currentServiceObjectGroup = new ServiceObjectGroup(name, protocol);
    if (_configuration.getObjectGroups().containsKey(name)) {
      warnObjectGroupRedefinition(ctx.name);
    } else {
      _configuration.getServiceObjectGroups().put(name, _currentServiceObjectGroup);
      _configuration.getObjectGroups().put(name, _currentServiceObjectGroup);
      _configuration.defineStructure(SERVICE_OBJECT_GROUP, name, ctx);
    }
  }

  @Override
  public void exitOggs_group_object(Oggs_group_objectContext ctx) {
    addServiceGroupReference(ctx.name);
  }

  @Override
  public void exitOgg_service(Ogg_serviceContext ctx) {
    _currentServiceObjectGroup = null;
  }

  @Override
  public void enterOg_network(Og_networkContext ctx) {
    String name = ctx.name.getText();
    // If there is a conflict, create a dummy object group
    if (_configuration.getObjectGroups().get(name) != null) {
      _currentNetworkObjectGroup = new NetworkObjectGroup(name);
      warnObjectGroupRedefinition(ctx.name);
    } else {
      _currentNetworkObjectGroup =
          _configuration.getNetworkObjectGroups().computeIfAbsent(name, NetworkObjectGroup::new);
      _configuration.getObjectGroups().put(name, _currentNetworkObjectGroup);
      _configuration.defineStructure(NETWORK_OBJECT_GROUP, name, ctx);
    }
  }

  @Override
  public void exitOg_network(Og_networkContext ctx) {
    _currentNetworkObjectGroup = null;
  }

  @Override
  public void enterOg_service(Og_serviceContext ctx) {
    String name = ctx.name.getText();
    ServiceProtocol protocol = toServiceProtocol(ctx.protocol_type);
    // If there is a conflict, create a dummy object group
    if (_configuration.getObjectGroups().get(name) != null) {
      _currentServiceObjectGroup = new ServiceObjectGroup(name, protocol);
      warnObjectGroupRedefinition(ctx.name);
    } else {
      _currentServiceObjectGroup =
          _configuration
              .getServiceObjectGroups()
              .computeIfAbsent(name, (groupName) -> new ServiceObjectGroup(groupName, protocol));
      _configuration.getObjectGroups().put(name, _currentServiceObjectGroup);
      _configuration.defineStructure(SERVICE_OBJECT_GROUP, name, ctx);
    }
  }

  @Override
  public void exitOg_service(Og_serviceContext ctx) {
    _currentServiceObjectGroup = null;
  }

  @Override
  public void enterOg_protocol(Og_protocolContext ctx) {
    String name = ctx.name.getText();
    // If there is a conflict, create a dummy object group
    if (_configuration.getObjectGroups().get(name) != null) {
      _currentProtocolObjectGroup = new ProtocolObjectGroup(name);
      warnObjectGroupRedefinition(ctx.name);
    } else {
      _currentProtocolObjectGroup =
          _configuration.getProtocolObjectGroups().computeIfAbsent(name, ProtocolObjectGroup::new);
      _configuration.getObjectGroups().put(name, _currentProtocolObjectGroup);
      _configuration.defineStructure(PROTOCOL_OBJECT_GROUP, name, ctx);
    }
  }

  @Override
  public void exitOg_protocol(Og_protocolContext ctx) {
    _currentProtocolObjectGroup = null;
  }

  @Override
  public void exitOgn_group_object(Ogn_group_objectContext ctx) {
    String name = ctx.name.getText();
    _currentNetworkObjectGroup.getLines().add(new IpSpaceReference(name));
    _configuration.referenceStructure(
        NETWORK_OBJECT_GROUP, name, NETWORK_OBJECT_GROUP_GROUP_OBJECT, ctx.name.start.getLine());
  }

  @Override
  public void exitOgn_host_ip(Ogn_host_ipContext ctx) {
    _currentNetworkObjectGroup.getLines().add(toIp(ctx.ip).toIpSpace());
  }

  @Override
  public void exitOgn_ip_with_mask(Ogn_ip_with_maskContext ctx) {
    Ip ip = toIp(ctx.ip);
    Ip mask = toIp(ctx.mask);
    _currentNetworkObjectGroup.getLines().add(Prefix.create(ip, mask).toIpSpace());
  }

  @Override
  public void exitOgn_network_object(Ogn_network_objectContext ctx) {
    IpSpace ipSpace = null;
    if (ctx.prefix != null) {
      ipSpace = IpWildcard.parse(ctx.prefix.getText()).toIpSpace();
    } else if (ctx.wildcard_address != null && ctx.wildcard_mask != null) {
      // Mask needs to be inverted since zeros are don't-cares in this context
      ipSpace =
          IpWildcard.ipWithWildcardMask(
                  toIp(ctx.wildcard_address), toIp(ctx.wildcard_mask).inverted())
              .toIpSpace();
    } else if (ctx.address != null) {
      ipSpace = IpWildcard.parse(ctx.address.getText()).toIpSpace();
    } else if (ctx.name != null) {
      String name = ctx.name.getText();
      ipSpace = new IpSpaceReference(name);
      _configuration.referenceStructure(
          NETWORK_OBJECT, name, NETWORK_OBJECT_GROUP_NETWORK_OBJECT, ctx.name.start.getLine());
    }
    if (ipSpace == null) {
      warn(ctx, "Unimplemented object-group network line.");
    } else {
      _currentNetworkObjectGroup.getLines().add(ipSpace);
    }
  }

  @Override
  public void exitOgp_protocol_object(Ogp_protocol_objectContext ctx) {
    _currentProtocolObjectGroup
        .getLines()
        .add(new ProtocolObjectGroupProtocolLine(toIpProtocol(ctx.protocol())));
  }

  @Override
  public void exitOgp_group_object(Ogp_group_objectContext ctx) {
    String name = ctx.name.getText();
    _currentProtocolObjectGroup.getLines().add(new ProtocolObjectGroupReferenceLine(name));
    _configuration.referenceStructure(
        PROTOCOL_OBJECT_GROUP, name, PROTOCOL_OBJECT_GROUP_GROUP_OBJECT, ctx.name.start.getLine());
  }

  @Override
  public void exitOgs_icmp(Ogs_icmpContext ctx) {
    _currentServiceObjectGroup.getLines().add(new IcmpServiceObjectGroupLine());
  }

  @Override
  public void enterOgs_service_object(Ogs_service_objectContext ctx) {
    if (ctx.service_specifier() != null) {
      _currentServiceObject = new ServiceObject(INLINE_SERVICE_OBJECT_NAME);
    }
  }

  @Override
  public void exitOgs_service_object(Ogs_service_objectContext ctx) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      _currentServiceObjectGroup
          .getLines()
          .add(new ServiceObjectReferenceServiceObjectGroupLine(name));
      _configuration.referenceStructure(
          SERVICE_OBJECT, name, SERVICE_OBJECT_GROUP_SERVICE_OBJECT, ctx.name.getStart().getLine());
    } else if (ctx.service_specifier() != null) {
      _currentServiceObjectGroup.getLines().add(_currentServiceObject);
      _currentServiceObject = null;
    }
  }

  @Override
  public void exitOgs_group_object(Ogs_group_objectContext ctx) {
    addServiceGroupReference(ctx.name);
  }

  private void addServiceGroupReference(Variable_group_idContext nameCtx) {
    String name = nameCtx.getText();
    _currentServiceObjectGroup
        .getLines()
        .add(new ServiceObjectGroupReferenceServiceObjectGroupLine(name));
    _configuration.referenceStructure(
        SERVICE_OBJECT_GROUP,
        name,
        EXTENDED_ACCESS_LIST_SERVICE_OBJECT_GROUP,
        nameCtx.start.getLine());
  }

  @Override
  public void exitOgs_tcp(Ogs_tcpContext ctx) {
    _currentServiceObjectGroup.getLines().add(new TcpServiceObjectGroupLine(toPortRanges(ctx.ps)));
  }

  @Override
  public void exitOgs_udp(Ogs_udpContext ctx) {
    _currentServiceObjectGroup.getLines().add(new UdpServiceObjectGroupLine(toPortRanges(ctx.ps)));
  }

  @Override
  public void exitOgs_port_object(Ogs_port_objectContext ctx) {
    List<SubRange> ranges = toPortRanges(ctx.ps);
    ServiceProtocol protocol = _currentServiceObjectGroup.getProtocol();
    ServiceObjectGroupLine line;
    if (protocol == null || protocol == ServiceProtocol.TCP_UDP) {
      line = new TcpUdpServiceObjectGroupLine(ranges);
    } else if (protocol == ServiceProtocol.TCP) {
      line = new TcpServiceObjectGroupLine(ranges);
    } else if (protocol == ServiceProtocol.UDP) {
      line = new UdpServiceObjectGroupLine(ranges);
    } else {
      throw new IllegalStateException(
          "Unexpected service object group protocol: '" + protocol + "'");
    }
    _currentServiceObjectGroup.getLines().add(line);
  }

  @Override
  public void exitOn_description(On_descriptionContext ctx) {
    _configuration
        .getNetworkObjectInfos()
        .get(_currentNetworkObjectName)
        .setDescription(getDescription(ctx.description_line()));
  }

  @Override
  public void exitOn_fqdn(On_fqdnContext ctx) {
    _configuration.getNetworkObjects().put(_currentNetworkObjectName, new FqdnNetworkObject());
    warn(ctx, "Unknown how to resolve domain name to IP address");
  }

  @Override
  public void exitOn_host(On_hostContext ctx) {
    if (ctx.address != null) {
      _configuration
          .getNetworkObjects()
          .put(_currentNetworkObjectName, new HostNetworkObject(Ip.parse(ctx.address.getText())));
    } else {
      // IPv6
      warn(ctx, "Unimplemented network object line");
    }
  }

  @Override
  public void exitOn_range(On_rangeContext ctx) {
    _configuration
        .getNetworkObjects()
        .put(
            _currentNetworkObjectName,
            new RangeNetworkObject(Ip.parse(ctx.start.getText()), Ip.parse(ctx.end.getText())));
  }

  @Override
  public void exitOn_subnet(On_subnetContext ctx) {
    if (ctx.address != null) {
      _configuration
          .getNetworkObjects()
          .put(
              _currentNetworkObjectName,
              new SubnetNetworkObject(
                  Prefix.create(Ip.parse(ctx.address.getText()), Ip.parse(ctx.mask.getText()))));
    } else {
      // IPv6
      warn(ctx, "Unimplemented network object line");
    }
  }

  @Override
  public void exitOs_description(Os_descriptionContext ctx) {
    _currentServiceObject.setDescription(getDescription(ctx.description_line()));
  }

  @Override
  public void enterPrefix_set_stanza(Prefix_set_stanzaContext ctx) {
    _currentPrefixSetName = ctx.name.getText();
    _configuration.defineStructure(PREFIX_SET, _currentPrefixSetName, ctx);
  }

  @Override
  public void enterPm_type_accounting(Pm_type_accountingContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.defineStructure(POLICY_MAP, name, ctx);
    warn(ctx, "Policy map of type accounting is not supported");
  }

  @Override
  public void enterPm_type_control_subscriber(Pm_type_control_subscriberContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.defineStructure(POLICY_MAP, name, ctx);
  }

  @Override
  public void enterPm_type_pbr(Pm_type_pbrContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.defineStructure(POLICY_MAP, name, ctx);
    warn(ctx, "Policy map of type pbr is not supported");
  }

  @Override
  public void enterPm_type_performance_traffic(Pm_type_performance_trafficContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.defineStructure(POLICY_MAP, name, ctx);
    warn(ctx, "Policy map of type performance-traffic is not supported");
  }

  @Override
  public void enterPm_type_qos(Pm_type_qosContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.defineStructure(POLICY_MAP, name, ctx);
    warn(ctx, "Policy map of type qos is not supported");
  }

  @Override
  public void enterPm_type_redirect(Pm_type_redirectContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.defineStructure(POLICY_MAP, name, ctx);
    warn(ctx, "Policy map of type redirect is not supported");
  }

  @Override
  public void enterPm_type_traffic(Pm_type_trafficContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.defineStructure(POLICY_MAP, name, ctx);
    warn(ctx, "Policy map of type traffic is not supported");
  }

  @Override
  public void enterRo_area(Ro_areaContext ctx) {
    long area;
    if (ctx.area_int != null) {
      area = toLong(ctx.area_int);
    } else if (ctx.area_ip != null) {
      area = toIp(ctx.area_ip).asLong();
    } else {
      throw new BatfishException("Missing area");
    }
    _currentOspfArea = area;
  }

  @Override
  public void enterRo_vrf(Ro_vrfContext ctx) {
    Ip routerId = _currentOspfProcess.getRouterId();
    _lastKnownOspfProcess = _currentOspfProcess.getName();
    _currentVrf = ctx.name.getText();
    _currentOspfProcess =
        currentVrf()
            .getOspfProcesses()
            .computeIfAbsent(
                _currentOspfProcess.getName(),
                (procName) -> {
                  OspfProcess p = new OspfProcess(procName);
                  p.setRouterId(routerId);
                  return p;
                });
  }

  @Override
  public void enterRoa_interface(Roa_interfaceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
    _configuration.referenceStructure(
        INTERFACE, ifaceName, OSPF_AREA_INTERFACE, ctx.iname.getStart().getLine());
    Interface iface = _configuration.getInterfaces().get(ifaceName);
    if (iface == null) {
      warn(
          ctx.iname,
          String.format("OSPF interface %s not declared before OSPF process", ifaceName));
      iface = addInterface(ifaceName, ctx.iname, false);
    }
    // might cause problems if interfaces are declared after ospf, but
    // whatever
    for (ConcreteInterfaceAddress address : iface.getAllAddresses()) {
      Prefix prefix = address.getPrefix();
      OspfNetwork network = new OspfNetwork(prefix, _currentOspfArea);
      _currentOspfProcess.getNetworks().add(network);
    }
    iface.setOspfArea(_currentOspfArea);
    iface.setOspfProcess(_currentOspfProcess.getName());
    _currentOspfInterface = iface.getName();
  }

  @Override
  public void enterRoute_policy_stanza(Route_policy_stanzaContext ctx) {
    String name = ctx.name.getText();
    _currentRoutePolicy = _configuration.getRoutePolicies().computeIfAbsent(name, RoutePolicy::new);

    List<RoutePolicyStatement> stmts = _currentRoutePolicy.getStatements();

    stmts.addAll(toRoutePolicyStatementList(ctx.stanzas));
    _configuration.defineStructure(ROUTE_POLICY, name, ctx);
  }

  @Override
  public void enterRouter_bgp_stanza(Router_bgp_stanzaContext ctx) {
    long procNum = ctx.bgp_asn() == null ? 0 : toAsNum(ctx.bgp_asn());
    Vrf vrf = _configuration.getVrfs().get(Configuration.DEFAULT_VRF_NAME);
    if (vrf.getBgpProcess() == null) {
      BgpProcess proc = new BgpProcess(procNum);
      vrf.setBgpProcess(proc);
    }
    BgpProcess proc = vrf.getBgpProcess();
    if (proc.getProcnum() != procNum && procNum != 0) {
      warn(ctx, "Cannot have multiple BGP processes with different ASNs");
      pushPeer(_dummyPeerGroup);
      return;
    }
    pushPeer(proc.getMasterBgpPeerGroup());
  }

  @Override
  public void exitRouter_bgp_stanza(Router_bgp_stanzaContext ctx) {
    popPeer();
  }

  @Override
  public void enterRe_classic(Re_classicContext ctx) {
    // Create a classic EIGRP process with ASN
    long asn = toLong(ctx.asnum);
    _currentEigrpProcess =
        new EigrpProcess(asn, EigrpProcessMode.CLASSIC, Configuration.DEFAULT_VRF_NAME);
  }

  @Override
  public void enterReaf_interface(Reaf_interfaceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
    _configuration.referenceStructure(
        INTERFACE, ifaceName, EIGRP_AF_INTERFACE, ctx.iname.getStart().getLine());
    _currentEigrpInterface = ifaceName;
  }

  @Override
  public void enterReaf_interface_default(Reaf_interface_defaultContext ctx) {
    _currentEigrpInterface = "default";
  }

  @Override
  public void enterRouter_isis_stanza(Router_isis_stanzaContext ctx) {
    IsisProcess isisProcess = new IsisProcess();
    isisProcess.setLevel(IsisLevel.LEVEL_1_2);
    currentVrf().setIsisProcess(isisProcess);
    _currentIsisProcess = isisProcess;
  }

  @Override
  public void enterRs_vrf(Rs_vrfContext ctx) {
    _currentVrf = ctx.name.getText();
  }

  @Override
  public void enterS_aaa(S_aaaContext ctx) {
    _no = ctx.NO() != null;
    if (_configuration.getCf().getAaa() == null) {
      _configuration.getCf().setAaa(new Aaa());
    }
  }

  @Override
  public void enterS_access_line(S_access_lineContext ctx) {
    String name = ctx.linetype.getText();
    _configuration.getCf().getLines().computeIfAbsent(name, Line::new);
  }

  @Override
  public void enterS_bfd_template(S_bfd_templateContext ctx) {
    _configuration.defineStructure(BFD_TEMPLATE, ctx.name.getText(), ctx);
  }

  @Override
  public void enterS_class_map(S_class_mapContext ctx) {
    // TODO: do something with this.
    String name = ctx.name.getText();
    _configuration.defineStructure(CLASS_MAP, name, ctx);
  }

  @Override
  public void enterS_interface(S_interfaceContext ctx) {
    String nameAlpha = ctx.iname.name_prefix_alpha.getText();
    String canonicalNamePrefix;
    try {
      canonicalNamePrefix = CiscoXrConfiguration.getCanonicalInterfaceNamePrefix(nameAlpha);
    } catch (BatfishException e) {
      warn(ctx, "Error fetching interface name: " + e.getMessage());
      _currentInterfaces = ImmutableList.of();
      return;
    }
    StringBuilder namePrefix = new StringBuilder(canonicalNamePrefix);
    for (Token part : ctx.iname.name_middle_parts) {
      namePrefix.append(part.getText());
    }
    _currentInterfaces = new ArrayList<>();
    if (ctx.iname.range() != null) {
      List<SubRange> ranges = toRange(ctx.iname.range());
      for (SubRange range : ranges) {
        for (int i = range.getStart(); i <= range.getEnd(); i++) {
          String name = namePrefix.toString() + i;
          addInterface(name, ctx.iname, true);
          _configuration.defineStructure(INTERFACE, name, ctx);
          _configuration.referenceStructure(
              INTERFACE, name, INTERFACE_SELF_REF, ctx.getStart().getLine());
        }
      }
    } else {
      addInterface(namePrefix.toString(), ctx.iname, true);
    }
    if (ctx.MULTIPOINT() != null) {
      todo(ctx);
    }
  }

  @Override
  public void enterS_ip_dhcp(S_ip_dhcpContext ctx) {
    _no = (ctx.NO() != null);
  }

  @Override
  public void enterS_ip_domain(S_ip_domainContext ctx) {
    _no = ctx.NO() != null;
  }

  @Override
  public void enterS_ip_pim(S_ip_pimContext ctx) {
    _no = ctx.NO() != null;
  }

  @Override
  public void enterS_ip_ssh(S_ip_sshContext ctx) {
    if (_configuration.getCf().getSsh() == null) {
      _configuration.getCf().setSsh(new SshSettings());
    }
  }

  @Override
  public void enterS_l2tp_class(S_l2tp_classContext ctx) {
    String name = ctx.name.getText();
    _configuration.getCf().getL2tpClasses().computeIfAbsent(name, L2tpClass::new);
    _configuration.defineStructure(L2TP_CLASS, name, ctx);
  }

  @Override
  public void enterS_line(S_lineContext ctx) {
    String lineType = ctx.line_type().getText();
    if (lineType.equals("")) {
      lineType = "<UNNAMED>";
    }
    String nameBase = lineType;
    Integer slot1 = null;
    Integer slot2 = null;
    Integer port1 = null;
    Integer port2 = null;
    List<String> names = new ArrayList<>();
    if (ctx.first != null) {
      if (ctx.slot1 != null) {
        slot1 = toInteger(ctx.slot1);
        slot2 = slot1;
        if (ctx.port1 != null) {
          port1 = toInteger(ctx.port1);
          port2 = port1;
        }
      }
      int first = toInteger(ctx.first);
      int last;
      if (ctx.last != null) {
        if (ctx.slot2 != null) {
          slot2 = toInteger(ctx.slot2);
          if (ctx.port2 != null) {
            port2 = toInteger(ctx.port2);
          }
        }
        last = toInteger(ctx.last);
      } else {
        last = first;
      }
      if (last < first) {
        throw new BatfishException("Do not support decreasing line range: " + first + " " + last);
      }
      if (slot1 != null && port1 != null) {
        for (int s = slot1; s <= slot2; s++) {
          for (int p = port1; p <= port2; p++) {
            for (int i = first; i <= last; i++) {
              String name = nameBase + s + "/" + p + "/" + i;
              names.add(name);
            }
          }
        }
      } else if (slot1 != null) {
        for (int s = slot1; s <= slot2; s++) {
          for (int i = first; i <= last; i++) {
            String name = nameBase + s + "/" + i;
            names.add(name);
          }
        }
      } else {
        for (int i = first; i <= last; i++) {
          String name = nameBase + i;
          names.add(name);
        }
      }
    } else {
      names.add(nameBase);
    }

    // get the default list or null if Aaa, AaaAuthentication, or AaaAuthenticationLogin is null or
    // default list is undefined
    AaaAuthenticationLoginList defaultList =
        Optional.ofNullable(_configuration.getCf().getAaa())
            .map(Aaa::getAuthentication)
            .map(AaaAuthentication::getLogin)
            .map(AaaAuthenticationLogin::getLists)
            .map(lists -> lists.get(AaaAuthenticationLogin.DEFAULT_LIST_NAME))
            .orElse(null);

    for (String name : names) {
      if (_configuration.getCf().getLines().get(name) == null) {
        Line line = new Line(name);
        if (defaultList != null) {
          // if default list defined, apply it to all lines
          line.setAaaAuthenticationLoginList(defaultList);
          line.setLoginAuthentication(AaaAuthenticationLogin.DEFAULT_LIST_NAME);
        } else if (_configuration.getCf().getAaa() != null
            && _configuration.getCf().getAaa().getNewModel()
            && line.getLineType() != LineType.CON) {
          // if default list not defined but aaa new-model, apply to all lines except con0
          line.setAaaAuthenticationLoginList(
              new AaaAuthenticationLoginList(
                  Collections.singletonList(AuthenticationMethod.LOCAL)));
          line.setLoginAuthentication(AaaAuthenticationLogin.DEFAULT_LIST_NAME);
        }
        _configuration.getCf().getLines().put(name, line);
      }
    }
    _currentLineNames = names;
  }

  @Override
  public void enterS_logging(S_loggingContext ctx) {
    if (_configuration.getCf().getLogging() == null) {
      _configuration.getCf().setLogging(new Logging());
    }
    if (ctx.NO() != null) {
      _no = true;
    }
  }

  @Override
  public void enterS_mac_access_list(S_mac_access_listContext ctx) {
    String name = ctx.num.getText();
    _currentMacAccessList =
        _configuration.getMacAccessLists().computeIfAbsent(name, MacAccessList::new);
    _configuration.defineStructure(MAC_ACCESS_LIST, name, ctx);
  }

  @Override
  public void enterS_mac_access_list_extended(S_mac_access_list_extendedContext ctx) {
    String name;
    if (ctx.num != null) {
      name = ctx.num.getText();

    } else if (ctx.name != null) {
      name = ctx.name.getText();
    } else {
      throw new BatfishException("Could not determine name of extended mac access-list");
    }
    _currentMacAccessList =
        _configuration.getMacAccessLists().computeIfAbsent(name, MacAccessList::new);
    _configuration.defineStructure(MAC_ACCESS_LIST, name, ctx);
  }

  @Override
  public void enterS_ntp(S_ntpContext ctx) {
    if (_configuration.getCf().getNtp() == null) {
      _configuration.getCf().setNtp(new Ntp());
    }
  }

  @Override
  public void enterS_router_ospf(S_router_ospfContext ctx) {
    String procName = ctx.name.getText();
    if (ctx.vrf != null) {
      _currentVrf = ctx.vrf.getText();
    }
    _currentOspfProcess =
        currentVrf()
            .getOspfProcesses()
            .computeIfAbsent(procName, (pName) -> new OspfProcess(pName));
  }

  @Override
  public void enterS_service_template(S_service_templateContext ctx) {
    // TODO: do something with this.
    String name = ctx.name.getText();
    _configuration.defineStructure(SERVICE_TEMPLATE, name, ctx);
  }

  @Override
  public void exitRo_auto_cost(Ro_auto_costContext ctx) {
    long referenceBandwidthDec = Long.parseLong(ctx.DEC().getText());
    long referenceBandwidth;
    if (ctx.MBPS() != null) {
      referenceBandwidth = referenceBandwidthDec * 1_000_000;
    } else if (ctx.GBPS() != null) {
      referenceBandwidth = referenceBandwidthDec * 1_000_000_000;
    } else {
      referenceBandwidth = referenceBandwidthDec * 1_000_000;
    }
    _currentOspfProcess.setReferenceBandwidth(referenceBandwidth);
  }

  @Override
  public void exitRo_max_metric(Ro_max_metricContext ctx) {
    if (ctx.on_startup != null || !ctx.WAIT_FOR_BGP().isEmpty()) {
      return;
    }
    _currentOspfProcess.setMaxMetricRouterLsa(true);
    _currentOspfProcess.setMaxMetricIncludeStub(ctx.stub != null);
    if (ctx.external_lsa != null) {
      _currentOspfProcess.setMaxMetricExternalLsa(
          ctx.external != null
              ? toLong(ctx.external)
              : OspfProcess.DEFAULT_MAX_METRIC_EXTERNAL_LSA);
    }
    if (ctx.summary_lsa != null) {
      _currentOspfProcess.setMaxMetricSummaryLsa(
          ctx.summary != null ? toLong(ctx.summary) : OspfProcess.DEFAULT_MAX_METRIC_SUMMARY_LSA);
    }
  }

  @Override
  public void enterS_router_rip(S_router_ripContext ctx) {
    RipProcess proc = new RipProcess();
    currentVrf().setRipProcess(proc);
    _currentRipProcess = proc;
  }

  @Override
  public void enterS_snmp_server(S_snmp_serverContext ctx) {
    if (_configuration.getSnmpServer() == null) {
      SnmpServer snmpServer = new SnmpServer();
      snmpServer.setVrf(Configuration.DEFAULT_VRF_NAME);
      _configuration.setSnmpServer(snmpServer);
    }
  }

  @Override
  public void enterS_sntp(S_sntpContext ctx) {
    if (_configuration.getCf().getSntp() == null) {
      _configuration.getCf().setSntp(new Sntp());
    }
  }

  @Override
  public void enterS_spanning_tree(S_spanning_treeContext ctx) {
    _no = ctx.NO() != null;
  }

  @Override
  public void enterS_tacacs_server(S_tacacs_serverContext ctx) {
    _no = ctx.NO() != null;
  }

  @Override
  public void enterS_track(S_trackContext ctx) {
    String name = ctx.name.getText();
    _currentTrackingGroup = name;
    _configuration.defineStructure(TRACK, name, ctx);
  }

  @Override
  public void exitS_track(S_trackContext ctx) {
    _currentTrackingGroup = null;
  }

  @Override
  public void exitTrack_interface(Track_interfaceContext ctx) {
    String name = toInterfaceName(ctx.interface_name());
    _configuration.referenceStructure(
        INTERFACE, name, TRACK_INTERFACE, ctx.interface_name().getStart().getLine());
    _configuration.getTrackingGroups().put(_currentTrackingGroup, new TrackInterface(name));
  }

  @Override
  public void enterS_username(S_usernameContext ctx) {
    String username;
    if (ctx.user != null) {
      username = ctx.user.getText();
    } else {
      username = unquote(ctx.quoted_user.getText());
    }
    _currentUser = _configuration.getCf().getUsers().computeIfAbsent(username, User::new);
  }

  @Override
  public void exitVrfc_rd(Vrfc_rdContext ctx) {
    if (ctx.AUTO() == null) {
      currentVrf().setRouteDistinguisher(toRouteDistinguisher(ctx.route_distinguisher()));
    }
  }

  @Override
  public void exitVrfc_route_target(Vrfc_route_targetContext ctx) {
    ExtendedCommunity rt = ctx.AUTO() != null ? null : toRouteTarget(ctx.route_target());
    if (ctx.IMPORT() != null || ctx.BOTH() != null) {
      currentVrf().setRouteImportTarget(rt);
    }
    if (ctx.EXPORT() != null || ctx.BOTH() != null) {
      currentVrf().setRouteExportTarget(rt);
    }
  }

  @Override
  public void exitVrfc_shutdown(Vrfc_shutdownContext ctx) {
    if (ctx.NO() == null) {
      todo(ctx);
    }
    currentVrf().setShutdown(ctx.NO() != null);
  }

  @Override
  public void exitVrfc_vni(Vrfc_vniContext ctx) {
    currentVrf().setVni(toInteger(ctx.vni));
  }

  @Override
  public void enterS_vrf_definition(S_vrf_definitionContext ctx) {
    _currentVrf = ctx.name.getText();
    initVrf(_currentVrf);
  }

  @Override
  public void exitService_specifier_icmp(Service_specifier_icmpContext ctx) {
    _currentServiceObject.addProtocol(IpProtocol.ICMP);
    if (ctx.icmp_object_type() != null) {
      _currentServiceObject.setIcmpType(toIcmpType(ctx.icmp_object_type()));
    }
  }

  @Override
  public void exitService_specifier_protocol(Service_specifier_protocolContext ctx) {
    _currentServiceObject.addProtocol(toIpProtocol(ctx.protocol()));
  }

  @Override
  public void exitService_specifier_tcp_udp(Service_specifier_tcp_udpContext ctx) {
    if (ctx.TCP() != null || ctx.TCP_UDP() != null) {
      _currentServiceObject.addProtocol(IpProtocol.TCP);
    }
    if (ctx.TCP_UDP() != null || ctx.UDP() != null) {
      _currentServiceObject.addProtocol(IpProtocol.UDP);
    }
    if (ctx.dst_ps != null) {
      _currentServiceObject.addDstPorts(toPortRanges(ctx.dst_ps));
    }
    if (ctx.src_ps != null) {
      _currentServiceObject.addSrcPorts(toPortRanges(ctx.src_ps));
    }
  }

  @Override
  public void enterSession_group_rb_stanza(Session_group_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    BgpProcess proc = currentVrf().getBgpProcess();
    _currentPeerSession = proc.getPeerSessions().get(name);
    if (_currentPeerSession == null) {
      _currentPeerSession = proc.addPeerSession(name);
    }
    _configuration.defineStructure(BGP_SESSION_GROUP, name, ctx);
    pushPeer(_currentPeerSession);
  }

  @Override
  public void enterSs_community(Ss_communityContext ctx) {
    String name = ctx.name.getText();
    Map<String, SnmpCommunity> communities = _configuration.getSnmpServer().getCommunities();
    _currentSnmpCommunity = communities.computeIfAbsent(name, SnmpCommunity::new);
  }

  @Override
  public void enterSs_host(Ss_hostContext ctx) {
    String hostname;
    if (ctx.ip4 != null) {
      hostname = ctx.ip4.getText();
    } else if (ctx.ip6 != null) {
      hostname = ctx.ip6.getText();
    } else if (ctx.host != null) {
      hostname = ctx.host.getText();
    } else {
      throw new BatfishException("Invalid host");
    }
    Map<String, SnmpHost> hosts = _configuration.getSnmpServer().getHosts();
    _currentSnmpHost = hosts.computeIfAbsent(hostname, SnmpHost::new);
  }

  @Override
  public void enterTemplate_peer_policy_rb_stanza(Template_peer_policy_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    BgpProcess proc = currentVrf().getBgpProcess();
    _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
    if (_currentNamedPeerGroup == null) {
      _currentNamedPeerGroup = proc.addNamedPeerGroup(name);
    }
    pushPeer(_currentNamedPeerGroup);
    _configuration.defineStructure(BGP_TEMPLATE_PEER_POLICY, name, ctx);
  }

  @Override
  public void enterTemplate_peer_session_rb_stanza(Template_peer_session_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    BgpProcess proc = currentVrf().getBgpProcess();
    _currentPeerSession = proc.getPeerSessions().get(name);
    if (_currentPeerSession == null) {
      _currentPeerSession = proc.addPeerSession(name);
    }
    _configuration.defineStructure(BGP_TEMPLATE_PEER_SESSION, name, ctx);
    pushPeer(_currentPeerSession);
  }

  @Override
  public void enterTs_host(Ts_hostContext ctx) {
    String hostname = ctx.hostname.getText();
    if (!_no) {
      _configuration.getTacacsServers().add(hostname);
    }
  }

  @Override
  public void enterViaf_vrrp(Viaf_vrrpContext ctx) {
    int groupNum = toInteger(ctx.groupnum);
    _currentVrrpGroup =
        _configuration
            .getVrrpGroups()
            .computeIfAbsent(_currentVrrpInterface, name -> new VrrpInterface())
            .getVrrpGroups()
            .computeIfAbsent(groupNum, VrrpGroup::new);
  }

  @Override
  public void enterVrf_block_rb_stanza(Vrf_block_rb_stanzaContext ctx) {
    _currentVrf = ctx.name.getText();
    long procNum =
        _configuration.getVrfs().get(Configuration.DEFAULT_VRF_NAME).getBgpProcess().getProcnum();
    BgpProcess proc = new BgpProcess(procNum);
    currentVrf().setBgpProcess(proc);
    pushPeer(proc.getMasterBgpPeerGroup());
    _currentBlockNeighborAddressFamilies.clear();
    _inBlockNeighbor = false;
  }

  @Override
  public void enterVrrp_interface(Vrrp_interfaceContext ctx) {
    _currentVrrpInterface = getCanonicalInterfaceName(ctx.iface.getText());
    _configuration.referenceStructure(
        INTERFACE, _currentVrrpInterface, ROUTER_VRRP_INTERFACE, ctx.iface.getStart().getLine());
  }

  @Override
  public void exitAaa_accounting_commands_line(Aaa_accounting_commands_lineContext ctx) {
    _currentAaaAccountingCommands = null;
  }

  @Override
  public void exitAaa_accounting_default_group(Aaa_accounting_default_groupContext ctx) {
    List<String> groups =
        ctx.groups.stream().map(RuleContext::getText).collect(Collectors.toList());
    _configuration.getCf().getAaa().getAccounting().getDefault().setGroups(groups);
  }

  @Override
  public void exitAaa_accounting_default_local(Aaa_accounting_default_localContext ctx) {
    _configuration.getCf().getAaa().getAccounting().getDefault().setLocal(true);
  }

  @Override
  public void exitAaa_authentication_login_list(Aaa_authentication_login_listContext ctx) {
    _currentAaaAuthenticationLoginList = null;
  }

  @Override
  public void exitAaa_authentication_login_privilege_mode(
      Aaa_authentication_login_privilege_modeContext ctx) {
    _configuration.getCf().getAaa().getAuthentication().getLogin().setPrivilegeMode(true);
  }

  @Override
  public void exitAaa_new_model(Aaa_new_modelContext ctx) {
    _configuration.getCf().getAaa().setNewModel(!_no);
  }

  @Override
  public void exitActivate_bgp_tail(Activate_bgp_tailContext ctx) {
    if (_currentPeerGroup == null) {
      return;
    }
    BgpProcess proc = currentVrf().getBgpProcess();
    if (_currentPeerGroup != proc.getMasterBgpPeerGroup()) {
      _currentPeerGroup.setActive(true);
    } else {
      throw new BatfishException("no peer or peer group to activate in this context");
    }
  }

  @Override
  public void exitAdditional_paths_rb_stanza(Additional_paths_rb_stanzaContext ctx) {
    if (ctx.SELECT() != null && ctx.ALL() != null) {
      _currentPeerGroup.setAdditionalPathsSelectAll(true);
    } else {
      if (ctx.RECEIVE() != null) {
        _currentPeerGroup.setAdditionalPathsReceive(true);
      }
      if (ctx.SEND() != null) {
        _currentPeerGroup.setAdditionalPathsSend(true);
      }
    }
  }

  @Override
  public void exitAdditional_paths_selection_xr_rb_stanza(
      Additional_paths_selection_xr_rb_stanzaContext ctx) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      _configuration.referenceStructure(
          ROUTE_POLICY,
          name,
          BGP_ADDITIONAL_PATHS_SELECTION_ROUTE_POLICY,
          ctx.name.getStart().getLine());
    }
  }

  @Override
  public void exitAddress_family_rb_stanza(Address_family_rb_stanzaContext ctx) {
    popPeer();
  }

  @Override
  public void exitAf_group_rb_stanza(Af_group_rb_stanzaContext ctx) {
    resetPeerGroups();
    popPeer();
  }

  @Override
  public void exitAggregate_address_rb_stanza(Aggregate_address_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    // Intentional identity comparison
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      boolean summaryOnly = ctx.summary_only != null;
      boolean asSet = ctx.as_set != null;
      if (ctx.network != null || ctx.prefix != null) {
        // ipv4
        Prefix prefix;
        if (ctx.network != null) {
          Ip network = toIp(ctx.network);
          Ip subnet = toIp(ctx.subnet);
          int prefixLength = subnet.numSubnetBits();
          prefix = Prefix.create(network, prefixLength);
        } else {
          // ctx.prefix != null
          prefix = Prefix.parse(ctx.prefix.getText());
        }
        BgpAggregateIpv4Network net = new BgpAggregateIpv4Network(prefix);
        net.setAsSet(asSet);
        net.setSummaryOnly(summaryOnly);
        if (ctx.rp != null) {
          String policyName = ctx.rp.getText();
          net.setAttributeMap(policyName);
          _configuration.referenceStructure(
              ROUTE_POLICY, policyName, BGP_AGGREGATE_ROUTE_POLICY, ctx.rp.getStart().getLine());
        }
        proc.getAggregateNetworks().put(prefix, net);
      } else if (ctx.ipv6_prefix != null) {
        // ipv6
        Prefix6 prefix6 = Prefix6.parse(ctx.ipv6_prefix.getText());
        BgpAggregateIpv6Network net = new BgpAggregateIpv6Network(prefix6);
        net.setAsSet(asSet);
        net.setSummaryOnly(summaryOnly);
        proc.getAggregateIpv6Networks().put(prefix6, net);
      }
    } else if (_currentIpPeerGroup != null
        || _currentIpv6PeerGroup != null
        || _currentDynamicIpPeerGroup != null
        || _currentDynamicIpv6PeerGroup != null
        || _currentNamedPeerGroup != null) {
      throw new BatfishException("unexpected occurrence in peer group/neighbor context");
    }
  }

  @Override
  public void exitAllowas_in_bgp_tail(Allowas_in_bgp_tailContext ctx) {
    _currentPeerGroup.setAllowAsIn(true);
    if (ctx.num != null) {
      todo(ctx);
    }
  }

  @Override
  public void exitAlways_compare_med_rb_stanza(Always_compare_med_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.setAlwaysCompareMed(true);
  }

  @Override
  public void exitAs_path_multipath_relax_rb_stanza(As_path_multipath_relax_rb_stanzaContext ctx) {
    currentVrf().getBgpProcess().setAsPathMultipathRelax(ctx.NO() == null);
  }

  @Override
  public void exitAs_path_set_stanza(As_path_set_stanzaContext ctx) {
    String name = ctx.name.getText();
    AsPathSet asPathSet = _configuration.getAsPathSets().get(name);
    if (asPathSet != null) {
      warn(ctx, "Redeclaration of as-path-set: '" + name + "'.");
    }
    asPathSet = new AsPathSet(name);
    _configuration.getAsPathSets().put(name, asPathSet);
    for (As_path_set_elemContext elemCtx : ctx.elems) {
      toAsPathSetElem(elemCtx).ifPresent(asPathSet.getElements()::add);
    }
    _configuration.defineStructure(AS_PATH_SET, name, ctx);
  }

  @Override
  public void exitAuto_summary_bgp_tail(Auto_summary_bgp_tailContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitS_banner_ios(S_banner_iosContext ctx) {
    String bannerType = toBannerType(ctx.banner_header);
    if (bannerType == null) {
      warn(ctx, String.format("Unsupported IOS banner header: %s", ctx.banner_header.getText()));
      return;
    }
    String body = toString(ctx.banner);
    _configuration.getCf().getBanners().put(bannerType, body);
  }

  private static @Nonnull String toString(Ios_delimited_bannerContext ctx) {
    return ctx.body != null ? ctx.body.getText() : "";
  }

  private static @Nullable String toBannerType(Ios_banner_headerContext ctx) {
    if (ctx.BANNER_IOS() != null) {
      return "";
    } else if (ctx.BANNER_CONFIG_SAVE_IOS() != null) {
      return "config-save";
    } else if (ctx.BANNER_EXEC_IOS() != null) {
      return "exec";
    } else if (ctx.BANNER_INCOMING_IOS() != null) {
      return "incoming";
    } else if (ctx.BANNER_LOGIN_IOS() != null) {
      return "login";
    } else if (ctx.BANNER_MOTD_IOS() != null) {
      return "motd";
    } else if (ctx.BANNER_PROMPT_TIMEOUT_IOS() != null) {
      return "prompt-timeout";
    } else if (ctx.BANNER_SLIP_PPP_IOS() != null) {
      return "slip-ppp";
    } else {
      return null;
    }
  }

  @Override
  public void exitBgp_advertise_inactive_rb_stanza(Bgp_advertise_inactive_rb_stanzaContext ctx) {
    _currentPeerGroup.setAdvertiseInactive(true);
  }

  @Override
  public void exitBgp_listen_range_rb_stanza(Bgp_listen_range_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    _configuration.referenceStructure(
        BGP_PEER_GROUP, name, BGP_LISTEN_RANGE_PEER_GROUP, ctx.name.getStart().getLine());
    BgpProcess proc = currentVrf().getBgpProcess();
    if (ctx.IP_PREFIX() != null) {
      Prefix prefix = Prefix.parse(ctx.IP_PREFIX().getText());
      DynamicIpBgpPeerGroup pg = proc.addDynamicIpPeerGroup(prefix);
      pg.setGroupName(name);
      if (ctx.bgp_asn() != null) {
        long remoteAs = toAsNum(ctx.bgp_asn());
        pg.setRemoteAs(remoteAs);
      }
    } else if (ctx.IPV6_PREFIX() != null) {
      Prefix6 prefix6 = Prefix6.parse(ctx.IPV6_PREFIX().getText());
      DynamicIpv6BgpPeerGroup pg = proc.addDynamicIpv6PeerGroup(prefix6);
      pg.setGroupName(name);
      if (ctx.bgp_asn() != null) {
        long remoteAs = toAsNum(ctx.bgp_asn());
        pg.setRemoteAs(remoteAs);
      }
    }
  }

  @Override
  public void exitBgp_redistribute_internal_rb_stanza(
      Bgp_redistribute_internal_rb_stanzaContext ctx) {
    todo(ctx); // TODO(https://github.com/batfish/batfish/issues/3230)
  }

  @Override
  public void exitCip_profile(Cip_profileContext ctx) {
    _configuration.getIpsecProfiles().put(_currentIpsecProfile.getName(), _currentIpsecProfile);
    _currentIpsecProfile = null;
  }

  @Override
  public void exitCip_transform_set(Cip_transform_setContext ctx) {
    _configuration
        .getIpsecTransformSets()
        .put(_currentIpsecTransformSet.getName(), _currentIpsecTransformSet);
    _currentIpsecTransformSet = null;
  }

  @Override
  public void exitCis_policy(Cis_policyContext ctx) {
    _configuration.getIsakmpPolicies().put(_currentIsakmpPolicy.getName(), _currentIsakmpPolicy);
    _currentIsakmpPolicy = null;
  }

  @Override
  public void exitCis_profile(Cis_profileContext ctx) {
    _configuration.getIsakmpProfiles().put(_currentIsakmpProfile.getName(), _currentIsakmpProfile);
    _currentIsakmpProfile = null;
  }

  @Override
  public void exitCluster_id_bgp_tail(Cluster_id_bgp_tailContext ctx) {
    Ip clusterId = null;
    if (ctx.DEC() != null) {
      long ipAsLong = toLong(ctx.DEC());
      clusterId = Ip.create(ipAsLong);
    } else if (ctx.IP_ADDRESS() != null) {
      clusterId = toIp(ctx.IP_ADDRESS());
    }
    _currentPeerGroup.setClusterId(clusterId);
  }

  @Override
  public void enterCm_ios_inspect(Cm_ios_inspectContext ctx) {
    String name = ctx.name.getText();
    _currentInspectClassMap =
        _configuration.getInspectClassMaps().computeIfAbsent(name, InspectClassMap::new);
    _configuration.defineStructure(INSPECT_CLASS_MAP, name, ctx);
    MatchSemantics matchSemantics =
        ctx.match_semantics() != null
            ? toMatchSemantics(ctx.match_semantics())
            : MatchSemantics.MATCH_ALL;
    _currentInspectClassMap.setMatchSemantics(matchSemantics);
  }

  private MatchSemantics toMatchSemantics(Match_semanticsContext ctx) {
    if (ctx.MATCH_ALL() != null) {
      return MatchSemantics.MATCH_ALL;
    } else if (ctx.MATCH_ANY() != null) {
      return MatchSemantics.MATCH_ANY;
    } else {
      throw convError(MatchSemantics.class, ctx);
    }
  }

  @Override
  public void exitCd_match_address(Cd_match_addressContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _currentCryptoMapEntry.setAccessList(name);
    _configuration.referenceStructure(IP_ACCESS_LIST, name, CRYPTO_DYNAMIC_MAP_ACL, line);
  }

  @Override
  public void exitCd_set_isakmp_profile(Cd_set_isakmp_profileContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _currentCryptoMapEntry.setIsakmpProfile(name);
    _configuration.referenceStructure(
        ISAKMP_PROFILE, name, CRYPTO_DYNAMIC_MAP_ISAKMP_PROFILE, line);
  }

  @Override
  public void exitCd_set_peer(Cd_set_peerContext ctx) {
    _currentCryptoMapEntry.setPeer(toIp(ctx.address));
  }

  @Override
  public void exitCd_set_pfs(Cd_set_pfsContext ctx) {
    _currentCryptoMapEntry.setPfsKeyGroup(toDhGroup(ctx.dh_group()));
  }

  @Override
  public void exitCd_set_transform_set(Cd_set_transform_setContext ctx) {
    for (VariableContext transform : ctx.transforms) {
      int line = transform.getStart().getLine();
      String name = transform.getText();
      _currentCryptoMapEntry.getTransforms().add(name);
      _configuration.referenceStructure(
          IPSEC_TRANSFORM_SET, name, CRYPTO_DYNAMIC_MAP_TRANSFORM_SET, line);
    }
  }

  @Override
  public void exitCm_ios_inspect(Cm_ios_inspectContext ctx) {
    _currentInspectClassMap = null;
  }

  @Override
  public void exitCm_iosi_match(Cm_iosi_matchContext ctx) {
    InspectClassMapMatch match = toInspectClassMapMatch(ctx);
    if (match != null) {
      _currentInspectClassMap.getMatches().add(match);
    }
  }

  private InspectClassMapMatch toInspectClassMapMatch(Cm_iosi_matchContext ctx) {
    if (ctx.cm_iosim_access_group() != null) {
      String name = ctx.cm_iosim_access_group().name.getText();
      int line = ctx.cm_iosim_access_group().name.getStart().getLine();
      _configuration.referenceStructure(
          IP_ACCESS_LIST, name, INSPECT_CLASS_MAP_MATCH_ACCESS_GROUP, line);
      return new InspectClassMapMatchAccessGroup(name);
    } else if (ctx.cm_iosim_protocol() != null) {
      return new InspectClassMapMatchProtocol(
          toInspectClassMapProtocol(ctx.cm_iosim_protocol().inspect_protocol()));
    } else {
      warn(ctx, "Class-map match unsupported");
      return null;
    }
  }

  private InspectClassMapProtocol toInspectClassMapProtocol(Inspect_protocolContext ctx) {
    if (ctx.HTTP() != null) {
      return InspectClassMapProtocol.HTTP;
    } else if (ctx.HTTPS() != null) {
      return InspectClassMapProtocol.HTTPS;
    } else if (ctx.ICMP() != null) {
      return InspectClassMapProtocol.ICMP;
    } else if (ctx.TCP() != null) {
      return InspectClassMapProtocol.TCP;
    } else if (ctx.TFTP() != null) {
      return InspectClassMapProtocol.TFTP;
    } else if (ctx.UDP() != null) {
      return InspectClassMapProtocol.UDP;
    } else {
      throw convError(InspectClassMapProtocol.class, ctx);
    }
  }

  @Override
  public void exitCm_match(Cm_matchContext ctx) {
    if (ctx.NOT() != null) {
      // TODO: https://github.com/batfish/batfish/issues/1835
      todo(ctx);
    }
  }

  @Override
  public void exitCmm_access_group(Cmm_access_groupContext ctx) {
    String name;
    int line;
    if (ctx.name != null) {
      name = ctx.name.getText();
      line = ctx.name.getStart().getLine();
    } else {
      name = ctx.num.getText();
      line = ctx.num.getLine();
    }
    _configuration.referenceStructure(ACCESS_LIST, name, CLASS_MAP_ACCESS_GROUP, line);
  }

  @Override
  public void exitCmm_access_list(Cmm_access_listContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(ACCESS_LIST, name, CLASS_MAP_ACCESS_LIST, line);
  }

  @Override
  public void exitCmm_activated_service_template(Cmm_activated_service_templateContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(
        SERVICE_TEMPLATE, name, CLASS_MAP_ACTIVATED_SERVICE_TEMPLATE, line);
  }

  @Override
  public void exitCmm_service_template(Cmm_service_templateContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(SERVICE_TEMPLATE, name, CLASS_MAP_SERVICE_TEMPLATE, line);
  }

  @Override
  public void exitCompare_routerid_rb_stanza(Compare_routerid_rb_stanzaContext ctx) {
    currentVrf().getBgpProcess().setTieBreaker(BgpTieBreaker.ROUTER_ID);
  }

  @Override
  public void exitCopsl_access_list(Copsl_access_listContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _configuration.referenceStructure(IP_ACCESS_LIST, name, COPS_LISTENER_ACCESS_LIST, line);
  }

  @Override
  public void exitCp_ip_access_group(Cp_ip_access_groupContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, CONTROL_PLANE_ACCESS_GROUP, line);
  }

  @Override
  public void exitCp_service_policy(Cp_service_policyContext ctx) {
    CiscoXrStructureUsage usage =
        ctx.INPUT() != null
            ? CONTROL_PLANE_SERVICE_POLICY_INPUT
            : CONTROL_PLANE_SERVICE_POLICY_OUTPUT;
    _configuration.referenceStructure(
        POLICY_MAP, ctx.name.getText(), usage, ctx.name.getStart().getLine());
  }

  @Override
  public void exitCrypto_keyring(Crypto_keyringContext ctx) {
    _configuration.getKeyrings().put(_currentKeyring.getName(), _currentKeyring);
    _currentKeyring = null;
  }

  @Override
  public void exitCrypto_map_t_ii_match_address(Crypto_map_t_ii_match_addressContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _currentCryptoMapEntry.setAccessList(name);
    _configuration.referenceStructure(IP_ACCESS_LIST, name, CRYPTO_MAP_IPSEC_ISAKMP_ACL, line);
  }

  @Override
  public void exitCrypto_map_t_ii_set_isakmp_profile(
      Crypto_map_t_ii_set_isakmp_profileContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _currentCryptoMapEntry.setIsakmpProfile(name);
    _configuration.referenceStructure(
        ISAKMP_PROFILE, name, CRYPTO_MAP_IPSEC_ISAKMP_ISAKMP_PROFILE, line);
  }

  @Override
  public void exitCrypto_map_t_ii_set_peer(Crypto_map_t_ii_set_peerContext ctx) {
    _currentCryptoMapEntry.setPeer(toIp(ctx.address));
  }

  @Override
  public void exitCrypto_map_t_ii_set_pfs(Crypto_map_t_ii_set_pfsContext ctx) {
    _currentCryptoMapEntry.setPfsKeyGroup(toDhGroup(ctx.dh_group()));
  }

  @Override
  public void exitCrypto_map_t_ii_set_transform_set(Crypto_map_t_ii_set_transform_setContext ctx) {
    for (VariableContext transform : ctx.transforms) {
      int line = transform.getStart().getLine();
      String name = transform.getText();
      _currentCryptoMapEntry.getTransforms().add(name);
      _configuration.referenceStructure(
          IPSEC_TRANSFORM_SET, name, CRYPTO_MAP_IPSEC_ISAKMP_TRANSFORM_SET, line);
    }
  }

  @Override
  public void exitCrypto_map_t_ipsec_isakmp(Crypto_map_t_ipsec_isakmpContext ctx) {
    _currentCryptoMapName = null;
    _currentCryptoMapSequenceNum = null;
  }

  @Override
  public void exitDefault_information_originate_rb_stanza(
      Default_information_originate_rb_stanzaContext ctx) {
    _currentPeerGroup.setDefaultOriginate(true);
  }

  @Override
  public void exitDefault_metric_bgp_tail(Default_metric_bgp_tailContext ctx) {
    int metric = toInteger(ctx.metric);
    _currentPeerGroup.setDefaultMetric(metric);
  }

  @Override
  public void exitDefault_originate_bgp_tail(Default_originate_bgp_tailContext ctx) {
    _currentPeerGroup.setDefaultOriginate(true);
  }

  @Override
  public void exitDefault_shutdown_bgp_tail(Default_shutdown_bgp_tailContext ctx) {
    _currentPeerGroup.setShutdown(true);
  }

  @Override
  public void exitDescription_bgp_tail(Description_bgp_tailContext ctx) {
    _currentPeerGroup.setDescription(getDescription(ctx.description_line()));
  }

  @Override
  public void exitDistribute_list_bgp_tail(Distribute_list_bgp_tailContext ctx) {
    // Note: Mutually exclusive with Prefix_list_bgp_tail
    // https://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/5816-bgpfaq-5816.html
    String name = ctx.list_name.getText();
    int line = ctx.list_name.getStart().getLine();
    CiscoXrStructureUsage usage;
    if (_inIpv6BgpPeer) {
      // TODO Support IPv6 access lists in BGP distribute-lists
      if (ctx.IN() != null) {
        usage = BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_IN;
      } else if (ctx.OUT() != null) {
        usage = BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_OUT;
      } else {
        throw new BatfishException("Invalid direction for BGP distribute-list");
      }
    } else {
      if (ctx.IN() != null) {
        _currentPeerGroup.setInboundIpAccessList(name);
        usage = BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_IN;
      } else if (ctx.OUT() != null) {
        _currentPeerGroup.setOutboundIpAccessList(name);
        usage = BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_OUT;
      } else {
        throw new BatfishException("Invalid direction for BGP distribute-list");
      }
    }
    CiscoXrStructureType type = _inIpv6BgpPeer ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    _configuration.referenceStructure(type, name, usage, line);
  }

  @Override
  public void exitDistribute_list_is_stanza(Distribute_list_is_stanzaContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _configuration.referenceStructure(IP_ACCESS_LIST, name, ROUTER_ISIS_DISTRIBUTE_LIST_ACL, line);
  }

  @Override
  public void exitDomain_lookup(Domain_lookupContext ctx) {
    if (ctx.iname != null) {
      String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(
          INTERFACE, ifaceName, DOMAIN_LOOKUP_SOURCE_INTERFACE, ctx.iname.getStart().getLine());
      _configuration.setDnsSourceInterface(ifaceName);
    }
  }

  @Override
  public void exitDomain_name(Domain_nameContext ctx) {
    String domainName = ctx.hostname.getText();
    _configuration.setDomainName(domainName);
  }

  @Override
  public void exitDomain_name_server(Domain_name_serverContext ctx) {
    Set<String> dnsServers = _configuration.getDnsServers();
    String hostname = ctx.hostname.getText();
    dnsServers.add(hostname);
  }

  @Override
  public void exitEbgp_multihop_bgp_tail(Ebgp_multihop_bgp_tailContext ctx) {
    _currentPeerGroup.setEbgpMultihop(true);
  }

  private void exitEigrpProcess(ParserRuleContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    EigrpProcess proc = _currentEigrpProcess;
    if (proc.getAsn() == null) {
      /*
       * This will happen with the following configuration:
       *  address-family ... autonomous-system 1
       *   autonomous-system 2
       *   no autonomous-system
       * The result should be a process with ASN 1, but instead the result is an invalid EIGRP
       * process with null ASN.
       */
      warn(ctx, "No EIGRP ASN configured");
      return;
    }
    proc.computeNetworks(_configuration.getInterfaces().values());

    // Check for duplicates in this VRF
    _currentVrf = proc.getVrf();
    Map<Long, EigrpProcess> eigrpProcesses = currentVrf().getEigrpProcesses();
    boolean duplicate = eigrpProcesses.containsKey(proc.getAsn());
    if (duplicate) {
      warn(ctx, "Duplicate EIGRP router ASN");
    } else {
      eigrpProcesses.put(proc.getAsn(), proc);
    }

    // Pop process if nested
    _currentEigrpProcess = _parentEigrpProcess;
    _parentEigrpProcess = null;
    _currentVrf =
        _currentEigrpProcess != null
            ? _currentEigrpProcess.getVrf()
            : Configuration.DEFAULT_VRF_NAME;
  }

  @Override
  public void exitEmpty_neighbor_block_address_family(
      Empty_neighbor_block_address_familyContext ctx) {
    popPeer();
  }

  @Override
  public void exitEnable_secret(Enable_secretContext ctx) {
    String password;
    if (ctx.double_quoted_string() != null) {
      password = unquote(ctx.double_quoted_string().getText());
    } else {
      password = ctx.pass.getText() + CommonUtil.salt();
    }
    String passwordRehash = CommonUtil.sha256Digest(password);
    _configuration.getCf().setEnableSecret(passwordRehash);
  }

  @Override
  public void exitExtended_access_list_tail(Extended_access_list_tailContext ctx) {
    LineAction action = toLineAction(ctx.ala);
    AccessListAddressSpecifier srcAddressSpecifier = toAccessListAddressSpecifier(ctx.srcipr);
    AccessListAddressSpecifier dstAddressSpecifier = toAccessListAddressSpecifier(ctx.dstipr);
    AccessListServiceSpecifier serviceSpecifier = computeExtendedAccessListServiceSpecifier(ctx);
    String name = getFullText(ctx).trim();
    Ipv4AccessListLine line =
        Ipv4AccessListLine.builder()
            .setAction(action)
            .setDstAddressSpecifier(dstAddressSpecifier)
            .setName(name)
            .setServiceSpecifier(serviceSpecifier)
            .setSrcAddressSpecifier(srcAddressSpecifier)
            .build();
    _currentIpv4Acl.addLine(line);
  }

  private AccessListServiceSpecifier computeExtendedAccessListServiceSpecifier(
      Extended_access_list_tailContext ctx) {
    if (ctx.prot != null) {
      @Nullable IpProtocol protocol = toIpProtocol(ctx.prot);
      List<SubRange> srcPortRanges =
          ctx.alps_src != null ? toPortRanges(ctx.alps_src) : Collections.emptyList();
      List<SubRange> dstPortRanges =
          ctx.alps_dst != null ? toPortRanges(ctx.alps_dst) : Collections.emptyList();
      Integer icmpType = null;
      Integer icmpCode = null;
      List<TcpFlagsMatchConditions> tcpFlags = new ArrayList<>();
      Set<Integer> dscps = new TreeSet<>();
      Set<Integer> ecns = new TreeSet<>();
      for (Extended_access_list_additional_featureContext feature : ctx.features) {
        if (feature.ACK() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setAck(true).build())
                  .setUseAck(true)
                  .build());
        } else if (feature.ADMINISTRATIVELY_PROHIBITED() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.COMMUNICATION_ADMINISTRATIVELY_PROHIBITED;
        } else if (feature.ALTERNATE_ADDRESS() != null) {
          icmpType = IcmpType.ALTERNATE_ADDRESS;
        } else if (feature.CWR() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setCwr(true).build())
                  .setUseCwr(true)
                  .build());
        } else if (feature.DOD_HOST_PROHIBITED() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_HOST_PROHIBITED;
        } else if (feature.DOD_NET_PROHIBITED() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_NETWORK_PROHIBITED;
        } else if (feature.DSCP() != null) {
          int dscpType = toDscpType(feature.dscp_type());
          dscps.add(dscpType);
        } else if (feature.ECE() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setEce(true).build())
                  .setUseEce(true)
                  .build());
        } else if (feature.ECHO_REPLY() != null) {
          icmpType = IcmpType.ECHO_REPLY;
        } else if (feature.ECHO() != null) {
          icmpType = IcmpType.ECHO_REQUEST;
        } else if (feature.ECN() != null) {
          int ecn = toInteger(feature.ecn);
          ecns.add(ecn);
        } else if (feature.ESTABLISHED() != null) {
          // must contain ACK or RST
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setAck(true).build())
                  .setUseAck(true)
                  .build());
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setRst(true).build())
                  .setUseRst(true)
                  .build());
        } else if (feature.FIN() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setFin(true).build())
                  .setUseFin(true)
                  .build());
        } else if (feature.GENERAL_PARAMETER_PROBLEM() != null) {
          icmpType = IcmpType.PARAMETER_PROBLEM;
          icmpCode = IcmpCode.INVALID_IP_HEADER;
        } else if (feature.HOST_ISOLATED() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.SOURCE_HOST_ISOLATED;
        } else if (feature.HOST_PRECEDENCE_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.HOST_PRECEDENCE_VIOLATION;
        } else if (feature.HOST_REDIRECT() != null) {
          icmpType = IcmpType.REDIRECT_MESSAGE;
          icmpCode = IcmpCode.HOST_ERROR;
        } else if (feature.HOST_TOS_REDIRECT() != null) {
          icmpType = IcmpType.REDIRECT_MESSAGE;
          icmpCode = IcmpCode.TOS_AND_HOST_ERROR;
        } else if (feature.HOST_TOS_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.HOST_UNREACHABLE_FOR_TOS;
        } else if (feature.HOST_UNKNOWN() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_HOST_UNKNOWN;
        } else if (feature.HOST_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.HOST_UNREACHABLE;
        } else if (feature.ICMP_OFF() != null) {
          // This means "do not send ICMP replies when denying because of this line".
          // Do nothing.
        } else if (feature.INFORMATION_REPLY() != null) {
          icmpType = IcmpType.INFO_REPLY;
        } else if (feature.INFORMATION_REQUEST() != null) {
          icmpType = IcmpType.INFO_REQUEST;
        } else if (feature.LOG() != null) {
          // Do nothing.
        } else if (feature.MASK_REPLY() != null) {
          icmpType = IcmpType.MASK_REPLY;
        } else if (feature.MASK_REQUEST() != null) {
          icmpType = IcmpType.MASK_REQUEST;
        } else if (feature.MOBILE_HOST_REDIRECT() != null) {
          icmpType = IcmpType.MOBILE_REDIRECT;
        } else if (feature.NET_REDIRECT() != null) {
          icmpType = IcmpType.REDIRECT_MESSAGE;
          icmpCode = IcmpCode.NETWORK_ERROR;
        } else if (feature.NET_TOS_REDIRECT() != null) {
          icmpType = IcmpType.REDIRECT_MESSAGE;
          icmpCode = IcmpCode.TOS_AND_NETWORK_ERROR;
        } else if (feature.NET_TOS_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.NETWORK_UNREACHABLE_FOR_TOS;
        } else if (feature.NET_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.NETWORK_UNREACHABLE;
        } else if (feature.NETWORK_UNKNOWN() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_NETWORK_UNKNOWN;
        } else if (feature.NO_ROOM_FOR_OPTION() != null) {
          icmpType = IcmpType.PARAMETER_PROBLEM;
          icmpCode = IcmpCode.BAD_LENGTH;
        } else if (feature.OPTION_MISSING() != null) {
          icmpType = IcmpType.PARAMETER_PROBLEM;
          icmpCode = IcmpCode.REQUIRED_OPTION_MISSING;
        } else if (feature.PACKET_TOO_BIG() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.FRAGMENTATION_NEEDED;
        } else if (feature.PARAMETER_PROBLEM() != null) {
          icmpType = IcmpType.PARAMETER_PROBLEM;
        } else if (feature.PORT_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.PORT_UNREACHABLE;
        } else if (feature.PSH() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setPsh(true).build())
                  .setUsePsh(true)
                  .build());
        } else if (feature.REDIRECT() != null) {
          icmpType = IcmpType.REDIRECT_MESSAGE;
        } else if (feature.ROUTER_ADVERTISEMENT() != null) {
          icmpType = IcmpType.ROUTER_ADVERTISEMENT;
        } else if (feature.ROUTER_SOLICITATION() != null) {
          icmpType = IcmpType.ROUTER_SOLICITATION;
        } else if (feature.RST() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setRst(true).build())
                  .setUseRst(true)
                  .build());
        } else if (feature.SOURCE_QUENCH() != null) {
          icmpType = IcmpType.SOURCE_QUENCH;
        } else if (feature.SOURCE_ROUTE_FAILED() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.SOURCE_ROUTE_FAILED;
        } else if (feature.SYN() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setSyn(true).build())
                  .setUseSyn(true)
                  .build());
        } else if (feature.TIME_EXCEEDED() != null) {
          icmpType = IcmpType.TIME_EXCEEDED;
        } else if (feature.TIMESTAMP_REPLY() != null) {
          icmpType = IcmpType.TIMESTAMP_REPLY;
        } else if (feature.TIMESTAMP_REQUEST() != null) {
          icmpType = IcmpType.TIMESTAMP_REQUEST;
        } else if (feature.TRACEROUTE() != null) {
          icmpType = IcmpType.TRACEROUTE;
        } else if (feature.TTL_EXCEEDED() != null) {
          icmpType = IcmpType.TIME_EXCEEDED;
          icmpCode = IcmpCode.TTL_EQ_ZERO_DURING_TRANSIT;
        } else if (feature.UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
        } else if (feature.URG() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setUrg(true).build())
                  .setUseUrg(true)
                  .build());
        } else if (feature.icmp_message_type != null) {
          icmpType = toInteger(feature.icmp_message_type);
          if (feature.icmp_message_code != null) {
            icmpCode = toInteger(feature.icmp_message_code);
          }
        } else {
          warn(ctx, "Unsupported clause in extended access list: " + feature.getText());
          return UnimplementedAccessListServiceSpecifier.INSTANCE;
        }
      }
      return SimpleExtendedAccessListServiceSpecifier.builder()
          .setDscps(dscps)
          .setDstPortRanges(dstPortRanges)
          .setEcns(ecns)
          .setIcmpCode(icmpCode)
          .setIcmpType(icmpType)
          .setProtocol(protocol)
          .setSrcPortRanges(srcPortRanges)
          .setTcpFlags(tcpFlags)
          .build();
    } else if (ctx.ogs != null) {
      // This object group specifier could be a service or protocol object group
      String name = ctx.ogs.getText();
      int line = ctx.ogs.getStart().getLine();
      _configuration.referenceStructure(
          PROTOCOL_OR_SERVICE_OBJECT_GROUP,
          name,
          EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP,
          line);
      return new ProtocolOrServiceObjectGroupServiceSpecifier(name);
    } else if (ctx.obj != null) {
      String name = ctx.obj.getText();
      int line = ctx.obj.getStart().getLine();
      _configuration.referenceStructure(
          SERVICE_OBJECT, name, EXTENDED_ACCESS_LIST_SERVICE_OBJECT, line);
      return new ServiceObjectServiceSpecifier(name);
    } else {
      return convProblem(
          AccessListServiceSpecifier.class, ctx, UnimplementedAccessListServiceSpecifier.INSTANCE);
    }
  }

  private AccessListAddressSpecifier toAccessListAddressSpecifier(Access_list_ip_rangeContext ctx) {
    if (ctx.ip != null) {
      if (ctx.wildcard != null) {
        // IP and mask
        Ip wildcard = toIp(ctx.wildcard);
        return new WildcardAddressSpecifier(IpWildcard.ipWithWildcardMask(toIp(ctx.ip), wildcard));
      } else {
        // Just IP. Same as if 'host' was specified
        return new WildcardAddressSpecifier(IpWildcard.create(toIp(ctx.ip)));
      }
    } else if (ctx.ANY() != null || ctx.ANY4() != null) {
      return new WildcardAddressSpecifier(IpWildcard.ANY);
    } else if (ctx.prefix != null) {
      return new WildcardAddressSpecifier(IpWildcard.create(Prefix.parse(ctx.prefix.getText())));
    } else if (ctx.address_group != null) {
      todo(ctx);
      return new WildcardAddressSpecifier(IpWildcard.ANY);
    } else if (ctx.iface != null) {
      todo(ctx);
      return new WildcardAddressSpecifier(IpWildcard.ANY);
    } else if (ctx.obj != null) {
      String name = ctx.obj.getText();
      int line = ctx.obj.getStart().getLine();
      _configuration.referenceStructure(
          NETWORK_OBJECT, name, EXTENDED_ACCESS_LIST_NETWORK_OBJECT, line);
      return new NetworkObjectAddressSpecifier(name);
    } else if (ctx.og != null) {
      String name = ctx.og.getText();
      int line = ctx.og.getStart().getLine();
      _configuration.referenceStructure(
          NETWORK_OBJECT_GROUP, name, EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP, line);
      return new NetworkObjectGroupAddressSpecifier(name);
    } else {
      throw convError(AccessListAddressSpecifier.class, ctx);
    }
  }

  @Override
  public void exitExtended_ipv6_access_list_tail(Extended_ipv6_access_list_tailContext ctx) {
    LineAction action = toLineAction(ctx.ala);
    IpProtocol protocol = toIpProtocol(ctx.prot);
    Ip6 srcIp = getIp(ctx.srcipr);
    Ip6 srcWildcard = getWildcard(ctx.srcipr);
    Ip6 dstIp = getIp(ctx.dstipr);
    Ip6 dstWildcard = getWildcard(ctx.dstipr);
    String srcAddressGroup = getAddressGroup(ctx.srcipr);
    String dstAddressGroup = getAddressGroup(ctx.dstipr);
    List<SubRange> srcPortRanges =
        ctx.alps_src != null ? toPortRanges(ctx.alps_src) : Collections.emptyList();
    List<SubRange> dstPortRanges =
        ctx.alps_dst != null ? toPortRanges(ctx.alps_dst) : Collections.emptyList();
    Integer icmpType = null;
    Integer icmpCode = null;
    List<TcpFlagsMatchConditions> tcpFlags = new ArrayList<>();
    Set<Integer> dscps = new TreeSet<>();
    Set<Integer> ecns = new TreeSet<>();
    for (Extended_access_list_additional_featureContext feature : ctx.features) {
      if (feature.ACK() != null) {
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setAck(true).build())
                .setUseAck(true)
                .build());
      } else if (feature.DSCP() != null) {
        int dscpType = toDscpType(feature.dscp_type());
        dscps.add(dscpType);
      } else if (feature.ECE() != null) {
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setEce(true).build())
                .setUseEce(true)
                .build());
      } else if (feature.ECHO_REPLY() != null) {
        icmpType = IcmpType.ECHO_REPLY;
        icmpCode = 0; /* Forced to 0 by RFC-792. */
      } else if (feature.ECHO() != null) {
        icmpType = IcmpType.ECHO_REQUEST;
        icmpCode = 0; /* Forced to 0 by RFC-792. */
      } else if (feature.ECN() != null) {
        int ecn = toInteger(feature.ecn);
        ecns.add(ecn);
      } else if (feature.ESTABLISHED() != null) {
        // must contain ACK or RST
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setAck(true).build())
                .setUseAck(true)
                .build());
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setRst(true).build())
                .setUseRst(true)
                .build());
      } else if (feature.FIN() != null) {
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setFin(true).build())
                .setUseFin(true)
                .build());
      } else if (feature.HOST_UNKNOWN() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.DESTINATION_HOST_UNKNOWN;
      } else if (feature.HOST_UNREACHABLE() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.HOST_UNREACHABLE;
      } else if (feature.LOG() != null) {
        // Do nothing.
      } else if (feature.NETWORK_UNKNOWN() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.DESTINATION_NETWORK_UNKNOWN;
      } else if (feature.NET_UNREACHABLE() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.NETWORK_UNREACHABLE;
      } else if (feature.PARAMETER_PROBLEM() != null) {
        icmpType = IcmpType.PARAMETER_PROBLEM;
      } else if (feature.PORT_UNREACHABLE() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.PORT_UNREACHABLE;
      } else if (feature.PSH() != null) {
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setPsh(true).build())
                .setUsePsh(true)
                .build());
      } else if (feature.REDIRECT() != null) {
        icmpType = IcmpType.REDIRECT_MESSAGE;
      } else if (feature.RST() != null) {
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setRst(true).build())
                .setUseRst(true)
                .build());
      } else if (feature.SOURCE_QUENCH() != null) {
        icmpType = IcmpType.SOURCE_QUENCH;
        icmpCode = 0; /* Forced to 0 by RFC 792. */
      } else if (feature.SYN() != null) {
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setSyn(true).build())
                .setUseSyn(true)
                .build());
      } else if (feature.TIME_EXCEEDED() != null) {
        icmpType = IcmpType.TIME_EXCEEDED;
      } else if (feature.TTL_EXCEEDED() != null) {
        icmpType = IcmpType.TIME_EXCEEDED;
      } else if (feature.TRACEROUTE() != null) {
        icmpType = IcmpType.TRACEROUTE;
      } else if (feature.UNREACHABLE() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
      } else if (feature.URG() != null) {
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setUrg(true).build())
                .setUseUrg(true)
                .build());
      } else {
        // warn(ctx, "Unsupported clause in IPv6 extended access list: " + feature.getText());
      }
    }
    String name = getFullText(ctx).trim();
    Ipv6AccessListLine line =
        new Ipv6AccessListLine(
            name,
            action,
            protocol,
            new Ip6Wildcard(srcIp, srcWildcard),
            srcAddressGroup,
            new Ip6Wildcard(dstIp, dstWildcard),
            dstAddressGroup,
            srcPortRanges,
            dstPortRanges,
            dscps,
            ecns,
            icmpType,
            icmpCode,
            tcpFlags);
    _currentIpv6Acl.addLine(line);
  }

  @Override
  public void exitFailover_interface(Failover_interfaceContext ctx) {
    String name = ctx.name.getText();
    Ip primaryIp = toIp(ctx.pip);
    Ip primaryMask = toIp(ctx.pmask);
    Ip standbyIp = toIp(ctx.sip);
    ConcreteInterfaceAddress primaryAddress =
        ConcreteInterfaceAddress.create(primaryIp, primaryMask);
    ConcreteInterfaceAddress standbyAddress =
        ConcreteInterfaceAddress.create(standbyIp, primaryMask);
    _configuration.getFailoverPrimaryAddresses().put(name, primaryAddress);
    _configuration.getFailoverStandbyAddresses().put(name, standbyAddress);
  }

  @Override
  public void exitFailover_link(Failover_linkContext ctx) {
    String alias = ctx.name.getText();
    String ifaceName = getCanonicalInterfaceName(ctx.iface.getText());
    _configuration.referenceStructure(
        INTERFACE, ifaceName, FAILOVER_LINK_INTERFACE, ctx.iface.getStart().getLine());
    _configuration.getFailoverInterfaces().put(alias, ifaceName);
    _configuration.setFailoverStatefulSignalingInterfaceAlias(alias);
    _configuration.setFailoverStatefulSignalingInterface(ifaceName);
  }

  @Override
  public void exitFlan_interface(Flan_interfaceContext ctx) {
    String alias = ctx.name.getText();
    String ifaceName = getCanonicalInterfaceName(ctx.iface.getText());
    _configuration.referenceStructure(
        INTERFACE, ifaceName, FAILOVER_LAN_INTERFACE, ctx.iface.getStart().getLine());
    _configuration.getFailoverInterfaces().put(alias, ifaceName);
    _configuration.setFailoverCommunicationInterface(ifaceName);
    _configuration.setFailoverCommunicationInterfaceAlias(alias);
  }

  @Override
  public void exitFlan_unit(Flan_unitContext ctx) {
    if (ctx.PRIMARY() != null) {
      _configuration.setFailoverSecondary(false);
    } else if (ctx.SECONDARY() != null) {
      _configuration.setFailoverSecondary(true);
      _configuration.setHostname(_configuration.getHostname() + "-FAILOVER-SECONDARY");
    }
    _configuration.setFailover(true);
  }

  @Override
  public void exitIf_autostate(If_autostateContext ctx) {
    if (ctx.NO() != null) {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setAutoState(false);
      }
    }
  }

  @Override
  public void exitIf_bandwidth(If_bandwidthContext ctx) {
    Double newBandwidthBps;
    if (ctx.NO() != null) {
      newBandwidthBps = null;
    } else {
      newBandwidthBps = toLong(ctx.DEC()) * 1000.0D;
    }
    _currentInterfaces.forEach(i -> i.setBandwidth(newBandwidthBps));
  }

  @Override
  public void exitIf_bfd_template(If_bfd_templateContext ctx) {
    _configuration.referenceStructure(
        BFD_TEMPLATE, ctx.name.getText(), INTERFACE_BFD_TEMPLATE, ctx.name.getStart().getLine());
  }

  @Override
  public void exitIf_bundle_id(If_bundle_idContext ctx) {
    int id = toInteger(ctx.id);
    _currentInterfaces.forEach(i -> i.setBundleId(id));
  }

  @Override
  public void exitIf_crypto_map(If_crypto_mapContext ctx) {
    _currentInterfaces.forEach(i -> i.setCryptoMap(ctx.name.getText()));
  }

  @Override
  public void exitIf_delay(If_delayContext ctx) {
    Long newDelayPs;
    if (ctx.NO() != null) {
      newDelayPs = null;
    } else {
      newDelayPs = toLong(ctx.DEC()) * 10_000_000;
    }
    _currentInterfaces.forEach(i -> i.setDelay(newDelayPs));
  }

  @Override
  public void exitIf_ip_access_group(If_ip_access_groupContext ctx) {
    String name = ctx.name.getText();
    CiscoXrStructureUsage usage = null;
    if (ctx.IN() != null || ctx.INGRESS() != null) {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setIncomingFilter(name);
        usage = INTERFACE_INCOMING_FILTER;
      }
    } else if (ctx.OUT() != null || ctx.EGRESS() != null) {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setOutgoingFilter(name);
        usage = INTERFACE_OUTGOING_FILTER;
      }
    } else {
      throw new BatfishException("bad direction");
    }
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, usage, ctx.name.getStart().getLine());
  }

  @Override
  public void exitIf_ip_address(If_ip_addressContext ctx) {
    ConcreteInterfaceAddress address;
    if (ctx.prefix != null) {
      address = ConcreteInterfaceAddress.parse(ctx.prefix.getText());
    } else {
      Ip ip = toIp(ctx.ip);
      Ip mask = toIp(ctx.subnet);
      address = ConcreteInterfaceAddress.create(ip, mask);
    }
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setAddress(address);
    }
    if (ctx.STANDBY() != null) {
      Ip standbyIp = toIp(ctx.standby_address);
      ConcreteInterfaceAddress standbyAddress =
          ConcreteInterfaceAddress.create(standbyIp, address.getNetworkBits());
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setStandbyAddress(standbyAddress);
      }
    }
    if (ctx.ROUTE_PREFERENCE() != null) {
      warn(ctx, "Unsupported: route-preference declared in interface IP address");
    }
    if (ctx.TAG() != null) {
      warn(ctx, "Unsupported: tag declared in interface IP address");
    }
  }

  @Override
  public void exitIf_ip_address_secondary(If_ip_address_secondaryContext ctx) {
    Ip ip;
    Ip mask;
    ConcreteInterfaceAddress address;
    if (ctx.prefix != null) {
      address = ConcreteInterfaceAddress.parse(ctx.prefix.getText());
    } else {
      ip = toIp(ctx.ip);
      mask = toIp(ctx.subnet);
      address = ConcreteInterfaceAddress.create(ip, mask.numSubnetBits());
    }
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.getSecondaryAddresses().add(address);
    }
  }

  @Override
  public void exitIf_ip_helper_address(If_ip_helper_addressContext ctx) {
    for (Interface iface : _currentInterfaces) {
      Ip dhcpRelayAddress = toIp(ctx.address);
      iface.getDhcpRelayAddresses().add(dhcpRelayAddress);
    }
  }

  @Override
  public void exitIf_ip_igmp(If_ip_igmpContext ctx) {
    _no = false;
  }

  @Override
  public void exitIf_ip_inband_access_group(If_ip_inband_access_groupContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _configuration.referenceStructure(IP_ACCESS_LIST, name, INTERFACE_IP_INBAND_ACCESS_GROUP, line);
  }

  @Override
  public void exitIf_ip_ospf_area(If_ip_ospf_areaContext ctx) {
    long area;
    if (ctx.area_dec != null) {
      area = toInteger(ctx.area_dec);
    } else {
      assert ctx.area_ip != null;
      area = toIp(ctx.area_ip).asLong();
    }
    String ospfProcessName = ctx.procname.getText();
    for (Interface iface : _currentInterfaces) {
      iface.setOspfArea(area);
      iface.setOspfProcess(ospfProcessName);
    }
  }

  @Override
  public void exitIf_ip_ospf_cost(If_ip_ospf_costContext ctx) {
    int cost = toInteger(ctx.cost);
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setOspfCost(cost);
    }
  }

  @Override
  public void exitIf_ip_ospf_dead_interval(If_ip_ospf_dead_intervalContext ctx) {
    int seconds = toInteger(ctx.seconds);
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setOspfDeadInterval(seconds);
      currentInterface.setOspfHelloMultiplier(0);
    }
  }

  @Override
  public void exitIf_ip_ospf_dead_interval_minimal(If_ip_ospf_dead_interval_minimalContext ctx) {
    int multiplier = toInteger(ctx.mult);
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setOspfDeadInterval(1);
      currentInterface.setOspfHelloMultiplier(multiplier);
    }
  }

  @Override
  public void exitIf_ip_ospf_hello_interval(If_ip_ospf_hello_intervalContext ctx) {
    int seconds = toInteger(ctx.seconds);
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setOspfHelloInterval(seconds);
    }
  }

  @Override
  public void exitIf_ip_ospf_passive_interface(If_ip_ospf_passive_interfaceContext ctx) {
    boolean passive = ctx.NO() == null;
    for (Interface iface : _currentInterfaces) {
      iface.setOspfPassive(passive);
    }
  }

  @Override
  public void exitIf_ip_ospf_shutdown(If_ip_ospf_shutdownContext ctx) {
    for (Interface iface : _currentInterfaces) {
      iface.setOspfShutdown(ctx.NO() == null);
    }
  }

  @Override
  public void exitIf_ip_pim_neighbor_filter(If_ip_pim_neighbor_filterContext ctx) {
    String acl = ctx.acl.getText();
    int line = ctx.acl.getStart().getLine();
    _configuration.referenceStructure(IPV4_ACCESS_LIST, acl, INTERFACE_PIM_NEIGHBOR_FILTER, line);
  }

  @Override
  public void exitIf_ip_proxy_arp(If_ip_proxy_arpContext ctx) {
    boolean enabled = ctx.NO() == null;
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setProxyArp(enabled);
    }
  }

  @Override
  public void exitIf_ip_router_isis(If_ip_router_isisContext ctx) {
    for (Interface iface : _currentInterfaces) {
      iface.setIsisInterfaceMode(IsisInterfaceMode.ACTIVE);
    }
  }

  @Override
  public void exitIf_ip_router_ospf_area(If_ip_router_ospf_areaContext ctx) {
    long area;
    if (ctx.area_dec != null) {
      area = toInteger(ctx.area_dec);
    } else {
      assert ctx.area_ip != null;
      area = toIp(ctx.area_ip).asLong();
    }
    String ospfProcessName = ctx.procname.getText();
    for (Interface iface : _currentInterfaces) {
      iface.setOspfArea(area);
      iface.setOspfProcess(ospfProcessName);
    }
  }

  @Override
  public void exitIf_ip_summary_address(If_ip_summary_addressContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitIf_ip_verify(If_ip_verifyContext ctx) {
    if (ctx.acl != null) {
      String acl = ctx.acl.getText();
      int line = ctx.acl.getLine();
      _configuration.referenceStructure(
          IPV4_ACCESS_LIST, acl, INTERFACE_IP_VERIFY_ACCESS_LIST, line);
    }
  }

  @Override
  public void exitIf_ip_vrf_forwarding(If_ip_vrf_forwardingContext ctx) {
    String name = ctx.vrf.getText();
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setVrf(name);
      initVrf(name);
    }
  }

  @Override
  public void exitIf_ipv6_traffic_filter(If_ipv6_traffic_filterContext ctx) {
    CiscoXrStructureUsage usage =
        ctx.IN() != null ? INTERFACE_IPV6_TRAFFIC_FILTER_IN : INTERFACE_IPV6_TRAFFIC_FILTER_OUT;
    _configuration.referenceStructure(
        IPV6_ACCESS_LIST, ctx.acl.getText(), usage, ctx.acl.getStart().getLine());
  }

  @Override
  public void exitIf_isis_metric(If_isis_metricContext ctx) {
    long metric = toLong(ctx.metric);
    for (Interface iface : _currentInterfaces) {
      iface.setIsisCost(metric);
    }
  }

  @Override
  public void exitIf_mtu(If_mtuContext ctx) {
    int mtu = toInteger(ctx.DEC());
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setMtu(mtu);
    }
  }

  @Override
  public void exitIf_service_policy(If_service_policyContext ctx) {
    // TODO: do something with this.
    String mapname = ctx.policy_map.getText();
    _configuration.referenceStructure(
        POLICY_MAP, mapname, INTERFACE_SERVICE_POLICY, ctx.getStart().getLine());
  }

  @Override
  public void exitIf_shutdown(If_shutdownContext ctx) {
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setActive(ctx.NO() != null);
    }
  }

  @Override
  public void exitIf_spanning_tree(If_spanning_treeContext ctx) {
    _no = false;
  }

  @Override
  public void exitIf_speed_ios(If_speed_iosContext ctx) {
    int mbits = toInteger(ctx.mbits);
    double speed = mbits * 1E6D;
    _currentInterfaces.forEach(i -> i.setSpeed(speed));
  }

  @Override
  public void exitIf_st_portfast(If_st_portfastContext ctx) {
    if (!_no) {
      boolean spanningTreePortfast = ctx.disable == null;
      for (Interface iface : _currentInterfaces) {
        iface.setSpanningTreePortfast(spanningTreePortfast);
      }
    }
  }

  @Override
  public void exitIf_switchport(If_switchportContext ctx) {
    if (ctx.NO() != null) {
      for (Interface iface : _currentInterfaces) {
        iface.setSwitchportMode(SwitchportMode.NONE);
        iface.setSwitchport(false);
      }
    } else {
      for (Interface iface : _currentInterfaces) {
        iface.setSwitchport(true);
        // setting the switch port mode only if it is not already set
        if (iface.getSwitchportMode() == null || iface.getSwitchportMode() == SwitchportMode.NONE) {
          SwitchportMode defaultSwitchportMode = _configuration.getCf().getDefaultSwitchportMode();
          iface.setSwitchportMode(
              (defaultSwitchportMode == SwitchportMode.NONE || defaultSwitchportMode == null)
                  ? SwitchportMode.ACCESS
                  : defaultSwitchportMode);
        }
      }
    }
  }

  @Override
  public void exitIf_switchport_access(If_switchport_accessContext ctx) {
    if (ctx.vlan != null) {
      int vlan = toInteger(ctx.vlan);
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setSwitchport(true);
        currentInterface.setSwitchportMode(SwitchportMode.ACCESS);
        currentInterface.setAccessVlan(vlan);
      }
    } else {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setSwitchport(true);
        currentInterface.setSwitchportMode(SwitchportMode.ACCESS);
        currentInterface.setSwitchportAccessDynamic(true);
      }
    }
  }

  @Override
  public void exitIf_switchport_mode(If_switchport_modeContext ctx) {
    if (ctx.if_switchport_mode_monitor() != null) {
      // This does not actually change the switchport mode, rather it just
      // configures buffer settings. See, e.g.,
      // https://www.cisco.com/c/en/us/td/docs/switches/datacenter/nexus3000/sw/system_mgmt/503_U5_1/b_3k_System_Mgmt_Config_503_u5_1/b_3k_System_Mgmt_Config_503_u5_1_chapter_010000.html
      return;
    }

    SwitchportMode mode;
    if (ctx.ACCESS() != null) {
      mode = SwitchportMode.ACCESS;
    } else if (ctx.DOT1Q_TUNNEL() != null) {
      mode = SwitchportMode.DOT1Q_TUNNEL;
    } else if (ctx.DYNAMIC() != null && ctx.AUTO() != null) {
      mode = SwitchportMode.DYNAMIC_AUTO;
    } else if (ctx.DYNAMIC() != null && ctx.DESIRABLE() != null) {
      mode = SwitchportMode.DYNAMIC_DESIRABLE;
    } else if (ctx.FEX_FABRIC() != null) {
      mode = SwitchportMode.FEX_FABRIC;
    } else if (ctx.TAP() != null) {
      mode = SwitchportMode.TAP;
    } else if (ctx.TRUNK() != null) {
      mode = SwitchportMode.TRUNK;
    } else if (ctx.TOOL() != null) {
      mode = SwitchportMode.TOOL;
    } else {
      throw new BatfishException("Unhandled switchport mode");
    }
    _currentInterfaces.forEach(iface -> iface.setSwitchport(true));
    _currentInterfaces.forEach(iface -> iface.setSwitchportMode(mode));
  }

  @Override
  public void exitIf_switchport_trunk_allowed(If_switchport_trunk_allowedContext ctx) {
    if (ctx.NONE() != null) {
      _currentInterfaces.forEach(iface -> iface.setAllowedVlans(IntegerSpace.EMPTY));
      return;
    }
    IntegerSpace allowed = IntegerSpace.builder().includingAllSubRanges(toRange(ctx.r)).build();
    for (Interface currentInterface : _currentInterfaces) {
      if (ctx.ADD() != null) {
        currentInterface.setAllowedVlans(
            IntegerSpace.builder()
                .including(allowed)
                .including(firstNonNull(currentInterface.getAllowedVlans(), IntegerSpace.EMPTY))
                .build());
      } else {
        currentInterface.setAllowedVlans(allowed);
      }
    }
  }

  @Override
  public void exitIf_switchport_trunk_encapsulation(If_switchport_trunk_encapsulationContext ctx) {
    SwitchportEncapsulationType type = toEncapsulation(ctx.e);
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setSwitchportMode(SwitchportMode.TRUNK);
      currentInterface.setSwitchportTrunkEncapsulation(type);
    }
  }

  @Override
  public void exitIf_switchport_trunk_native(If_switchport_trunk_nativeContext ctx) {
    int vlan = toInteger(ctx.vlan);
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setNativeVlan(vlan);
    }
  }

  @Override
  public void exitIf_vlan(If_vlanContext ctx) {
    int vlan = toInteger(ctx.vlan);
    _currentInterfaces.forEach(iface -> iface.setEncapsulationVlan(vlan));
  }

  @Override
  public void exitIf_vrf(If_vrfContext ctx) {
    String name = ctx.name.getText();
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setVrf(name);
      initVrf(name);
    }
  }

  @Override
  public void exitIf_vrf_member(If_vrf_memberContext ctx) {
    String name = ctx.name.getText();
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setVrf(name);
      initVrf(name);
    }
  }

  @Override
  public void exitIf_vrrp(If_vrrpContext ctx) {
    _currentVrrpGroupNum = null;
  }

  @Override
  public void exitIfdhcpr_address(Ifdhcpr_addressContext ctx) {
    for (Interface iface : _currentInterfaces) {
      Ip address = toIp(ctx.address);
      iface.getDhcpRelayAddresses().add(address);
    }
  }

  @Override
  public void exitIfdhcpr_client(Ifdhcpr_clientContext ctx) {
    for (Interface iface : _currentInterfaces) {
      iface.setDhcpRelayClient(true);
    }
  }

  @Override
  public void exitIfigmp_access_group(Ifigmp_access_groupContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _configuration.referenceStructure(IP_ACCESS_LIST, name, INTERFACE_IGMP_ACCESS_GROUP_ACL, line);
  }

  @Override
  public void exitIfigmphp_access_list(Ifigmphp_access_listContext ctx) {
    _configuration.referenceStructure(
        IP_ACCESS_LIST,
        ctx.name.getText(),
        INTERFACE_IGMP_HOST_PROXY_ACCESS_LIST,
        ctx.getStart().getLine());
  }

  @Override
  public void exitIfigmpsg_acl(Ifigmpsg_aclContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST, name, INTERFACE_IGMP_STATIC_GROUP_ACL, line);
  }

  @Override
  public void exitIftunnel_destination(Iftunnel_destinationContext ctx) {
    Ip destination = toIp(ctx.IP_ADDRESS());
    for (Interface iface : _currentInterfaces) {
      iface.getTunnelInitIfNull().setDestination(destination);
    }
  }

  @Override
  public void exitIftunnel_mode(Iftunnel_modeContext ctx) {
    for (Interface iface : _currentInterfaces) {
      Tunnel tunnel = iface.getTunnelInitIfNull();
      if (ctx.gre_ipv4 != null) {
        tunnel.setMode(TunnelMode.GRE_MULTIPOINT);
        todo(ctx);
      } else if (ctx.gre_multipoint != null) {
        tunnel.setMode(TunnelMode.GRE_MULTIPOINT);
        todo(ctx);
      } else if (ctx.ipsec_ipv4 != null) {
        tunnel.setMode(TunnelMode.IPSEC_IPV4);
      } else if (ctx.ipv6ip != null) {
        tunnel.setMode(TunnelMode.IPV6_IP);
        todo(ctx);
      } else {
        todo(ctx);
      }
    }
  }

  @Override
  public void exitIftunnel_protection(Iftunnel_protectionContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _configuration.referenceStructure(IPSEC_PROFILE, name, TUNNEL_PROTECTION_IPSEC_PROFILE, line);
    for (Interface iface : _currentInterfaces) {
      Tunnel tunnel = iface.getTunnelInitIfNull();
      tunnel.setIpsecProfileName(name);
      tunnel.setMode(TunnelMode.IPSEC_IPV4);
    }
  }

  @Override
  public void exitIftunnel_source(Iftunnel_sourceContext ctx) {
    if (ctx.IP_ADDRESS() != null) {
      Ip sourceAddress = toIp(ctx.IP_ADDRESS());
      for (Interface iface : _currentInterfaces) {
        iface.getTunnelInitIfNull().setSourceAddress(sourceAddress);
      }
    } else if (ctx.iname != null) {
      String sourceInterfaceName = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(
          INTERFACE, sourceInterfaceName, TUNNEL_SOURCE, ctx.iname.getStart().getLine());
      for (Interface iface : _currentInterfaces) {
        iface.getTunnelInitIfNull().setSourceInterfaceName(sourceInterfaceName);
      }
    } else if (ctx.DYNAMIC() != null) {
      for (Interface iface : _currentInterfaces) {
        iface.getTunnelInitIfNull().setSourceInterfaceName(null);
      }
    }
  }

  @Override
  public void exitIfvrrp_authentication(Ifvrrp_authenticationContext ctx) {
    String hashedAuthenticationText =
        CommonUtil.sha256Digest(ctx.text.getText() + CommonUtil.salt());
    for (Interface iface : _currentInterfaces) {
      String ifaceName = iface.getName();
      VrrpGroup vrrpGroup =
          _configuration
              .getVrrpGroups()
              .computeIfAbsent(ifaceName, n -> new VrrpInterface())
              .getVrrpGroups()
              .computeIfAbsent(_currentVrrpGroupNum, VrrpGroup::new);
      vrrpGroup.setAuthenticationTextHash(hashedAuthenticationText);
    }
  }

  @Override
  public void exitIfvrrp_ip(Ifvrrp_ipContext ctx) {
    Ip ip = toIp(ctx.ip);
    for (Interface iface : _currentInterfaces) {
      String ifaceName = iface.getName();
      VrrpGroup vrrpGroup =
          _configuration
              .getVrrpGroups()
              .computeIfAbsent(ifaceName, n -> new VrrpInterface())
              .getVrrpGroups()
              .computeIfAbsent(_currentVrrpGroupNum, VrrpGroup::new);
      vrrpGroup.setVirtualAddress(ip);
    }
  }

  @Override
  public void exitIfvrrp_preempt(Ifvrrp_preemptContext ctx) {
    for (Interface iface : _currentInterfaces) {
      String ifaceName = iface.getName();
      VrrpGroup vrrpGroup =
          _configuration
              .getVrrpGroups()
              .computeIfAbsent(ifaceName, n -> new VrrpInterface())
              .getVrrpGroups()
              .computeIfAbsent(_currentVrrpGroupNum, VrrpGroup::new);
      vrrpGroup.setPreempt(true);
    }
  }

  @Override
  public void exitIfvrrp_priority(Ifvrrp_priorityContext ctx) {
    int priority = toInteger(ctx.priority);
    for (Interface iface : _currentInterfaces) {
      String ifaceName = iface.getName();
      VrrpGroup vrrpGroup =
          _configuration
              .getVrrpGroups()
              .computeIfAbsent(ifaceName, n -> new VrrpInterface())
              .getVrrpGroups()
              .computeIfAbsent(_currentVrrpGroupNum, VrrpGroup::new);
      vrrpGroup.setPriority(priority);
    }
  }

  @Override
  public void exitInherit_peer_policy_bgp_tail(Inherit_peer_policy_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String groupName = ctx.name.getText();
    _configuration.referenceStructure(
        BGP_TEMPLATE_PEER_POLICY,
        groupName,
        BGP_INHERITED_PEER_POLICY,
        ctx.name.getStart().getLine());
    if (_currentIpPeerGroup != null) {
      _currentIpPeerGroup.setGroupName(groupName);
    } else if (_currentNamedPeerGroup != null) {
      // May not hit this since parser for peer-policy does not have
      // recursion.
      _currentNamedPeerGroup.setGroupName(groupName);
    } else if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      // Intentional identity comparison above
      throw new BatfishException("Invalid peer context for inheritance");
    } else {
      todo(ctx);
    }
  }

  @Override
  public void exitInherit_peer_session_bgp_tail(Inherit_peer_session_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String sessionName = ctx.name.getText();
    _configuration.referenceStructure(
        BGP_TEMPLATE_PEER_SESSION,
        sessionName,
        BGP_INHERITED_SESSION,
        ctx.name.getStart().getLine());
    if (_currentIpPeerGroup != null) {
      _currentIpPeerGroup.setPeerSession(sessionName);
    } else if (_currentNamedPeerGroup != null) {
      _currentNamedPeerGroup.setPeerSession(sessionName);
    } else if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      // Intentional identity comparison above
      throw new BatfishException("Invalid peer context for inheritance");
    } else {
      todo(ctx);
    }
  }

  @Override
  public void exitInterface_is_stanza(Interface_is_stanzaContext ctx) {
    _currentIsisInterface = null;
  }

  @Override
  public void exitIp_dhcp_relay_server(Ip_dhcp_relay_serverContext ctx) {
    if (!_no && ctx.ip != null) {
      Ip ip = toIp(ctx.ip);
      _configuration.getDhcpRelayServers().add(ip);
    }
  }

  @Override
  public void exitIp_domain_lookup(Ip_domain_lookupContext ctx) {
    if (ctx.iname != null) {
      String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(
          INTERFACE,
          ifaceName,
          IP_DOMAIN_LOOKUP_INTERFACE,
          ctx.interface_name().getStart().getLine());
      _configuration.setDnsSourceInterface(ifaceName);
    }
  }

  @Override
  public void exitIp_domain_name(Ip_domain_nameContext ctx) {
    if (!_no) {
      String domainName = ctx.hostname.getText();
      _configuration.setDomainName(domainName);
    } else {
      _configuration.setDomainName(null);
    }
  }

  @Override
  public void exitIp_prefix_list_stanza(Ip_prefix_list_stanzaContext ctx) {
    _currentPrefixList = null;
  }

  @Override
  public void exitIp_prefix_list_tail(Ip_prefix_list_tailContext ctx) {
    LineAction action = toLineAction(ctx.action);
    Prefix prefix = Prefix.parse(ctx.prefix.getText());
    int prefixLength = prefix.getPrefixLength();
    int minLen = prefixLength;
    int maxLen = prefixLength;
    if (ctx.minpl != null) {
      minLen = toInteger(ctx.minpl);
      maxLen = Prefix.MAX_PREFIX_LENGTH;
    }
    if (ctx.maxpl != null) {
      maxLen = toInteger(ctx.maxpl);
    }
    if (ctx.eqpl != null) {
      minLen = toInteger(ctx.eqpl);
      maxLen = toInteger(ctx.eqpl);
    }
    SubRange lengthRange = new SubRange(minLen, maxLen);
    PrefixListLine line = new PrefixListLine(action, prefix, lengthRange);
    _currentPrefixList.addLine(line);
  }

  @Override
  public void exitIp_route_stanza(Ip_route_stanzaContext ctx) {
    if (ctx.vrf != null || ctx.MANAGEMENT() != null) {
      _currentVrf = Configuration.DEFAULT_VRF_NAME;
    }
  }

  @Override
  public void exitIp_route_tail(Ip_route_tailContext ctx) {
    Prefix prefix;
    if (ctx.prefix != null) {
      prefix = Prefix.parse(ctx.prefix.getText());
    } else {
      Ip address = toIp(ctx.address);
      Ip mask = toIp(ctx.mask);
      int prefixLength = mask.numSubnetBits();
      prefix = Prefix.create(address, prefixLength);
    }
    Ip nextHopIp = Route.UNSET_ROUTE_NEXT_HOP_IP;
    String nextHopInterface = null;
    int distance = DEFAULT_STATIC_ROUTE_DISTANCE;
    Long tag = null;
    Integer track = null;
    boolean permanent = ctx.perm != null;
    if (ctx.nexthopip != null) {
      nextHopIp = toIp(ctx.nexthopip);
    } else if (ctx.nexthopprefix != null) {
      Prefix nextHopPrefix = Prefix.parse(ctx.nexthopprefix.getText());
      nextHopIp = nextHopPrefix.getStartIp();
    }
    if (ctx.nexthopint != null) {
      try {
        nextHopInterface = getCanonicalInterfaceName(ctx.nexthopint.getText());
        _configuration.referenceStructure(
            INTERFACE, nextHopInterface, IP_ROUTE_NHINT, ctx.nexthopint.getStart().getLine());
      } catch (BatfishException e) {
        warn(ctx, "Error fetching interface name: " + e.getMessage());
        _currentInterfaces = ImmutableList.of();
        return;
      }
    }
    if (ctx.distance != null) {
      distance = toInteger(ctx.distance);
    }
    if (ctx.tag != null) {
      tag = toLong(ctx.tag);
    }
    if (ctx.track != null) {
      track = toInteger(ctx.track);
    }
    StaticRoute route =
        new StaticRoute(prefix, nextHopIp, nextHopInterface, distance, tag, track, permanent);
    currentVrf().getStaticRoutes().add(route);
  }

  @Override
  public void exitIp_ssh_version(Ip_ssh_versionContext ctx) {
    int version = toInteger(ctx.version);
    if (version < 1 || version > 2) {
      throw new BatfishException("Invalid ssh version: " + version);
    }
    _configuration.getCf().getSsh().setVersion(version);
  }

  @Override
  public void exitIpv6_prefix_list_stanza(Ipv6_prefix_list_stanzaContext ctx) {
    _currentPrefix6List = null;
  }

  @Override
  public void exitIpv6_prefix_list_tail(Ipv6_prefix_list_tailContext ctx) {
    LineAction action = toLineAction(ctx.action);
    Prefix6 prefix6 = Prefix6.parse(ctx.prefix6.getText());
    int prefixLength = prefix6.getPrefixLength();
    int minLen = prefixLength;
    int maxLen = prefixLength;
    if (ctx.minpl != null) {
      minLen = toInteger(ctx.minpl);
      maxLen = Prefix6.MAX_PREFIX_LENGTH;
    }
    if (ctx.maxpl != null) {
      maxLen = toInteger(ctx.maxpl);
    }
    if (ctx.eqpl != null) {
      minLen = toInteger(ctx.eqpl);
      maxLen = toInteger(ctx.eqpl);
    }
    SubRange lengthRange = new SubRange(minLen, maxLen);
    Prefix6ListLine line = new Prefix6ListLine(action, prefix6, lengthRange);
    _currentPrefix6List.addLine(line);
  }

  @Override
  public void exitL_access_class(L_access_classContext ctx) {
    boolean ipv6 = (ctx.IPV6() != null);
    String name = ctx.name.getText();
    int nameLine = ctx.name.getStart().getLine();
    BiConsumer<Line, String> setter;
    CiscoXrStructureType structureType;
    CiscoXrStructureUsage structureUsage;
    if (ctx.OUT() != null || ctx.EGRESS() != null) {
      if (ipv6) {
        setter = Line::setOutputIpv6AccessList;
        structureType = IPV6_ACCESS_LIST;
        structureUsage = LINE_ACCESS_CLASS_LIST6;
      } else {
        setter = Line::setOutputAccessList;
        structureType = IPV4_ACCESS_LIST;
        structureUsage = LINE_ACCESS_CLASS_LIST;
      }

    } else {
      if (ipv6) {
        setter = Line::setInputIpv6AccessList;
        structureType = IPV6_ACCESS_LIST;
        structureUsage = LINE_ACCESS_CLASS_LIST6;
      } else {
        setter = Line::setInputAccessList;
        structureType = IPV4_ACCESS_LIST;
        structureUsage = LINE_ACCESS_CLASS_LIST;
      }
    }
    _configuration.referenceStructure(structureType, name, structureUsage, nameLine);
    for (String currentName : _currentLineNames) {
      Line line = _configuration.getCf().getLines().get(currentName);
      setter.accept(line, name);
    }
  }

  @Override
  public void exitL_exec_timeout(L_exec_timeoutContext ctx) {
    int minutes = toInteger(ctx.minutes);
    int seconds = ctx.seconds != null ? toInteger(ctx.seconds) : 0;
    for (String lineName : _currentLineNames) {
      Line line = _configuration.getCf().getLines().get(lineName);
      line.setExecTimeoutMinutes(minutes);
      line.setExecTimeoutSeconds(seconds);
    }
  }

  @Override
  public void exitL_login_authentication(L_login_authenticationContext ctx) {
    String list;
    if (ctx.DEFAULT() != null) {
      list = ctx.DEFAULT().getText();
    } else if (ctx.name != null) {
      list = ctx.name.getText();
    } else {
      throw new BatfishException("Invalid list name");
    }

    // get the authentication list or null if Aaa, AaaAuthentication, or AaaAuthenticationLogin is
    // null or the list is not defined
    AaaAuthenticationLoginList authList =
        Optional.ofNullable(_configuration.getCf().getAaa())
            .map(Aaa::getAuthentication)
            .map(AaaAuthentication::getLogin)
            .map(AaaAuthenticationLogin::getLists)
            .map(lists -> lists.get(list))
            .orElse(null);

    // if the authentication list has been defined, apply it to all lines in _currentLineNames
    for (String line : _currentLineNames) {
      if (authList != null) {
        _configuration.getCf().getLines().get(line).setAaaAuthenticationLoginList(authList);
      }
      // set the name of the login list even if the list hasn't been defined yet because it may be
      // defined later
      _configuration.getCf().getLines().get(line).setLoginAuthentication(list);
    }
  }

  @Override
  public void exitL_transport(L_transportContext ctx) {
    SortedSet<String> protocols =
        ctx.prot.stream().map(RuleContext::getText).collect(toCollection(TreeSet::new));
    BiConsumer<Line, SortedSet<String>> setter;
    if (ctx.INPUT() != null) {
      setter = Line::setTransportInput;
    } else if (ctx.OUTPUT() != null) {
      setter = Line::setTransportOutput;
    } else if (ctx.PREFERRED() != null) {
      setter = Line::setTransportPreferred;
    } else {
      throw new BatfishException("Invalid or unsupported line transport type");
    }
    for (String currentName : _currentLineNames) {
      Line line = _configuration.getCf().getLines().get(currentName);
      setter.accept(line, protocols);
    }
  }

  @Override
  public void exitLocal_as_bgp_tail(Local_as_bgp_tailContext ctx) {
    long as = toAsNum(ctx.bgp_asn());
    _currentPeerGroup.setLocalAs(as);
  }

  @Override
  public void exitLogging_buffered(Logging_bufferedContext ctx) {
    if (_no) {
      return;
    }
    Integer size = null;
    Integer severityNum = null;
    String severity = null;
    if (ctx.size != null) {
      // something was parsed as buffer size but it could be logging severity
      // as well
      // it is buffer size if the value is greater than min buffer size
      // otherwise, it is logging severity
      int sizeRawNum = toInteger(ctx.size);
      if (sizeRawNum > Logging.MAX_LOGGING_SEVERITY) {
        size = sizeRawNum;
      } else {
        if (ctx.logging_severity() != null) {
          // if we have explicity severity as well; we've messed up
          throw new BatfishException("Ambiguous parsing of logging buffered");
        }
        severityNum = sizeRawNum;
        severity = toLoggingSeverity(severityNum);
      }
    } else if (ctx.logging_severity() != null) {
      severityNum = toLoggingSeverityNum(ctx.logging_severity());
      severity = toLoggingSeverity(ctx.logging_severity());
    }
    Logging logging = _configuration.getCf().getLogging();
    Buffered buffered = logging.getBuffered();
    if (buffered == null) {
      buffered = new Buffered();
      logging.setBuffered(buffered);
    }
    buffered.setSeverity(severity);
    buffered.setSeverityNum(severityNum);
    buffered.setSize(size);
  }

  @Override
  public void exitLogging_console(Logging_consoleContext ctx) {
    if (_no) {
      return;
    }
    Integer severityNum = null;
    String severity = null;
    if (ctx.logging_severity() != null) {
      severityNum = toLoggingSeverityNum(ctx.logging_severity());
      severity = toLoggingSeverity(ctx.logging_severity());
    }
    Logging logging = _configuration.getCf().getLogging();
    LoggingType console = logging.getConsole();
    if (console == null) {
      console = new LoggingType();
      logging.setConsole(console);
    }
    console.setSeverity(severity);
    console.setSeverityNum(severityNum);
  }

  @Override
  public void exitLogging_host(Logging_hostContext ctx) {
    if (_no) {
      return;
    }
    Logging logging = _configuration.getCf().getLogging();
    String hostname = ctx.hostname.getText();
    LoggingHost host = new LoggingHost(hostname);
    logging.getHosts().put(hostname, host);
  }

  @Override
  public void exitLogging_on(Logging_onContext ctx) {
    Logging logging = _configuration.getCf().getLogging();
    logging.setOn(!_no);
  }

  @Override
  public void exitLogging_server(Logging_serverContext ctx) {
    if (_no) {
      return;
    }
    Logging logging = _configuration.getCf().getLogging();
    String hostname = ctx.hostname.getText();
    LoggingHost host = new LoggingHost(hostname);
    logging.getHosts().put(hostname, host);
  }

  @Override
  public void exitLogging_source_interface(Logging_source_interfaceContext ctx) {
    if (_no) {
      return;
    }
    Logging logging = _configuration.getCf().getLogging();
    String sourceInterface = toInterfaceName(ctx.interface_name());
    logging.setSourceInterface(sourceInterface);
  }

  @Override
  public void exitLogging_trap(Logging_trapContext ctx) {
    if (_no) {
      return;
    }
    Integer severityNum = null;
    String severity = null;
    if (ctx.logging_severity() != null) {
      severityNum = toLoggingSeverityNum(ctx.logging_severity());
      severity = toLoggingSeverity(ctx.logging_severity());
    }
    Logging logging = _configuration.getCf().getLogging();
    LoggingType trap = logging.getTrap();
    if (trap == null) {
      trap = new LoggingType();
      logging.setTrap(trap);
    }
    trap.setSeverity(severity);
    trap.setSeverityNum(severityNum);
  }

  @Override
  public void exitManagement_ssh_ip_access_group(Management_ssh_ip_access_groupContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, MANAGEMENT_SSH_ACCESS_GROUP, line);
  }

  @Override
  public void exitManagement_telnet_ip_access_group(Management_telnet_ip_access_groupContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, MANAGEMENT_TELNET_ACCESS_GROUP, line);
  }

  @Override
  public void exitMaximum_paths_bgp_tail(Maximum_paths_bgp_tailContext ctx) {
    int maximumPaths = toInteger(ctx.paths);
    BgpProcess proc = currentVrf().getBgpProcess();
    if (ctx.EBGP() != null) {
      proc.setMaximumPathsEbgp(maximumPaths);
    } else if (ctx.IBGP() != null) {
      proc.setMaximumPathsIbgp(maximumPaths);
    } else if (ctx.EIBGP() != null) {
      proc.setMaximumPathsEibgp(maximumPaths);
    } else {
      proc.setMaximumPaths(maximumPaths);
    }
  }

  @Override
  public void exitMaximum_peers_bgp_tail(Maximum_peers_bgp_tailContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitNeighbor_block_address_family(Neighbor_block_address_familyContext ctx) {
    if (_inBlockNeighbor) {
      popPeer();
    } else {
      _currentBlockNeighborAddressFamilies.clear();
    }
  }

  @Override
  public void exitNeighbor_block_rb_stanza(Neighbor_block_rb_stanzaContext ctx) {
    resetPeerGroups();
    if (_inBlockNeighbor) {
      _inBlockNeighbor = false;
      popPeer();
    }
  }

  @Override
  public void exitNeighbor_group_rb_stanza(Neighbor_group_rb_stanzaContext ctx) {
    resetPeerGroups();
    popPeer();
  }

  @Override
  public void exitNetwork_bgp_tail(Network_bgp_tailContext ctx) {
    Prefix prefix;
    if (ctx.prefix != null) {
      prefix = Prefix.parse(ctx.prefix.getText());
    } else {
      Ip address = toIp(ctx.ip);
      Ip mask = (ctx.mask != null) ? toIp(ctx.mask) : address.getClassMask();
      int prefixLength = mask.numSubnetBits();
      prefix = Prefix.create(address, prefixLength);
    }
    String map = null;
    BgpNetwork bgpNetwork = new BgpNetwork(map);
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.getIpNetworks().put(prefix, bgpNetwork);
  }

  @Override
  public void exitNetwork6_bgp_tail(Network6_bgp_tailContext ctx) {
    Prefix6 prefix6 = Prefix6.parse(ctx.prefix.getText());
    String map = null;
    BgpProcess proc = currentVrf().getBgpProcess();
    BgpNetwork6 bgpNetwork6 = new BgpNetwork6(map);
    proc.getIpv6Networks().put(prefix6, bgpNetwork6);
  }

  @Override
  public void exitRe_autonomous_system(Re_autonomous_systemContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }

    Long asn = (ctx.NO() == null) ? toLong(ctx.asnum) : null;
    _currentEigrpProcess.setAsn(asn);
  }

  @Override
  public void exitRe_default_metric(Re_default_metricContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    if (ctx.NO() == null) {
      EigrpMetric metric = toEigrpMetric(ctx.metric, _currentEigrpProcess.getMode());
      _currentEigrpProcess.setDefaultMetric(metric);
    } else {
      _currentEigrpProcess.setDefaultMetric(null);
    }
  }

  @Override
  public void exitRe_eigrp_router_id(Re_eigrp_router_idContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    if (ctx.NO() == null) {
      Ip routerId = toIp(ctx.id);
      _currentEigrpProcess.setRouterId(routerId);
    } else {
      _currentEigrpProcess.setRouterId(null);
    }
  }

  @Override
  public void exitRe_network(Re_networkContext ctx) {
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    // In process context
    Ip address = toIp(ctx.address);
    Ip mask = (ctx.mask != null) ? toIp(ctx.mask) : address.getClassMask().inverted();
    _currentEigrpProcess.getWildcardNetworks().add(IpWildcard.ipWithWildcardMask(address, mask));
  }

  @Override
  public void exitRe_passive_interface(Re_passive_interfaceContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    boolean passive = (ctx.NO() == null);
    String interfaceName = getCanonicalInterfaceName(ctx.i.getText());
    _currentEigrpProcess.getInterfacePassiveStatus().put(interfaceName, passive);
    _configuration.referenceStructure(
        INTERFACE, interfaceName, EIGRP_PASSIVE_INTERFACE, ctx.i.getStart().getLine());
  }

  @Override
  public void exitRe_passive_interface_default(Re_passive_interface_defaultContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    boolean passive = (ctx.NO() == null);
    _currentEigrpProcess.setPassiveInterfaceDefault(passive);
  }

  @Override
  public void exitRe_redistribute_bgp(Re_redistribute_bgpContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    RoutingProtocol sourceProtocol = RoutingProtocol.BGP;
    EigrpRedistributionPolicy r = new EigrpRedistributionPolicy(sourceProtocol);
    _currentEigrpProcess.getRedistributionPolicies().put(sourceProtocol, r);
    long as = toAsNum(ctx.asn);
    r.getSpecialAttributes().put(EigrpRedistributionPolicy.BGP_AS, as);

    if (ctx.metric != null) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }
  }

  @Override
  public void exitRe_redistribute_connected(Re_redistribute_connectedContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    RoutingProtocol sourceProtocol = RoutingProtocol.CONNECTED;
    EigrpRedistributionPolicy r = new EigrpRedistributionPolicy(sourceProtocol);
    _currentEigrpProcess.getRedistributionPolicies().put(sourceProtocol, r);

    if (ctx.metric != null) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }
  }

  @Override
  public void exitRe_redistribute_eigrp(Re_redistribute_eigrpContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    RoutingProtocol sourceProtocol = RoutingProtocol.EIGRP;
    EigrpRedistributionPolicy r = new EigrpRedistributionPolicy(sourceProtocol);
    _currentEigrpProcess.getRedistributionPolicies().put(sourceProtocol, r);
    long asn = toLong(ctx.asn);
    r.getSpecialAttributes().put(EigrpRedistributionPolicy.EIGRP_AS_NUMBER, asn);

    if (ctx.metric != null) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }
  }

  @Override
  public void exitRe_redistribute_isis(Re_redistribute_isisContext ctx) {
    _w.addWarning(
        ctx, getFullText(ctx), _parser, "ISIS redistribution in EIGRP is not implemented");
  }

  @Override
  public void exitRe_redistribute_ospf(Re_redistribute_ospfContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    RoutingProtocol sourceProtocol = RoutingProtocol.OSPF;
    EigrpRedistributionPolicy r = new EigrpRedistributionPolicy(sourceProtocol);
    _currentEigrpProcess.getRedistributionPolicies().put(sourceProtocol, r);
    int procNum = toInteger(ctx.proc);
    r.getSpecialAttributes().put(EigrpRedistributionPolicy.OSPF_PROCESS_NUMBER, procNum);

    if (ctx.MATCH() != null) {
      todo(ctx);
    }

    if (!ctx.METRIC().isEmpty()) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }
  }

  @Override
  public void exitRe_redistribute_rip(Re_redistribute_ripContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    RoutingProtocol sourceProtocol = RoutingProtocol.RIP;
    EigrpRedistributionPolicy r = new EigrpRedistributionPolicy(sourceProtocol);
    _currentEigrpProcess.getRedistributionPolicies().put(sourceProtocol, r);

    if (ctx.metric != null) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }
  }

  @Override
  public void exitRe_redistribute_static(Re_redistribute_staticContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    RoutingProtocol sourceProtocol = RoutingProtocol.STATIC;
    EigrpRedistributionPolicy r = new EigrpRedistributionPolicy(sourceProtocol);
    _currentEigrpProcess.getRedistributionPolicies().put(sourceProtocol, r);

    if (ctx.metric != null) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }
  }

  @Override
  public void exitReaf_interface(Reaf_interfaceContext ctx) {
    _currentEigrpInterface = null;
  }

  @Override
  public void exitReafi_passive_interface(Reafi_passive_interfaceContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    // In interface context
    if (_currentEigrpInterface == null) {
      warn(ctx, "No EIGRP interface available");
      return;
    }

    boolean passive = (ctx.NO() == null);
    if (_currentEigrpInterface.equals("default")) {
      _currentEigrpProcess.setPassiveInterfaceDefault(passive);
    } else {
      _currentEigrpProcess.getInterfacePassiveStatus().put(_currentEigrpInterface, passive);
    }
  }

  @Override
  public void exitRec_address_family(Rec_address_familyContext ctx) {
    exitEigrpProcess(ctx);
  }

  @Override
  public void exitRec_metric_weights(Rec_metric_weightsContext ctx) {
    // See https://github.com/batfish/batfish/issues/1946
    todo(ctx);
  }

  @Override
  public void exitRen_address_family(Ren_address_familyContext ctx) {
    exitEigrpProcess(ctx);
  }

  @Override
  public void exitRen_metric_weights(Ren_metric_weightsContext ctx) {
    // See https://github.com/batfish/batfish/issues/1946
    todo(ctx);
  }

  @Override
  public void exitNext_hop_self_bgp_tail(Next_hop_self_bgp_tailContext ctx) {
    boolean val = ctx.NO() == null;
    _currentPeerGroup.setNextHopSelf(val);
  }

  @Override
  public void exitNo_ip_prefix_list_stanza(No_ip_prefix_list_stanzaContext ctx) {
    String prefixListName = ctx.name.getText();
    _configuration.getPrefixLists().remove(prefixListName);
  }

  @Override
  public void exitNo_neighbor_activate_rb_stanza(No_neighbor_activate_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    if (ctx.ip != null) {
      Ip ip = toIp(ctx.ip);
      IpBgpPeerGroup pg = proc.getIpPeerGroups().get(ip);
      if (pg == null) {
        warn(ctx, "ignoring attempt to activate undefined ip peer group: " + ip);
      } else {
        pg.setActive(false);
      }
    } else if (ctx.ip6 != null) {
      Ip6 ip6 = toIp6(ctx.ip6);
      Ipv6BgpPeerGroup pg = proc.getIpv6PeerGroups().get(ip6);
      if (pg == null) {
        warn(ctx, "ignoring attempt to activate undefined ipv6 peer group: " + ip6);
      } else {
        pg.setActive(false);
      }
    } else if (ctx.peergroup != null) {
      String pgName = ctx.peergroup.getText();
      NamedBgpPeerGroup npg = proc.getNamedPeerGroups().get(pgName);
      npg.setActive(false);
      for (IpBgpPeerGroup ipg : proc.getIpPeerGroups().values()) {
        String currentGroupName = ipg.getGroupName();
        if (currentGroupName != null && currentGroupName.equals(pgName)) {
          ipg.setActive(false);
        }
      }
      for (Ipv6BgpPeerGroup ipg : proc.getIpv6PeerGroups().values()) {
        String currentGroupName = ipg.getGroupName();
        if (currentGroupName != null && currentGroupName.equals(pgName)) {
          ipg.setActive(false);
        }
      }
    }
  }

  @Override
  public void exitNo_neighbor_shutdown_rb_stanza(No_neighbor_shutdown_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    if (ctx.ip != null) {
      Ip ip = toIp(ctx.ip);
      IpBgpPeerGroup pg = proc.getIpPeerGroups().get(ip);
      // TODO: see if it is always ok to set active on 'no shutdown'
      if (pg == null) {
        warn(ctx, "ignoring attempt to shut down to undefined ip peer group: " + ip);
      } else {
        pg.setActive(true);
        pg.setShutdown(false);
      }
    } else if (ctx.ip6 != null) {
      Ip6 ip6 = toIp6(ctx.ip6);
      Ipv6BgpPeerGroup pg = proc.getIpv6PeerGroups().get(ip6);
      // TODO: see if it is always ok to set active on 'no shutdown'
      if (pg == null) {
        warn(ctx, "ignoring attempt to shut down undefined ipv6 peer group: " + ip6);
      } else {
        pg.setActive(true);
        pg.setShutdown(false);
      }
    } else if (ctx.peergroup != null) {
      warn(ctx, "'no shutdown' of  peer group unsupported");
    }
  }

  @Override
  public void exitNo_redistribute_connected_rb_stanza(
      No_redistribute_connected_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    // Intentional identity comparison
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      RoutingProtocol sourceProtocol = RoutingProtocol.CONNECTED;
      proc.getRedistributionPolicies().remove(sourceProtocol);
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
    }
  }

  @Override
  public void exitNo_shutdown_rb_stanza(No_shutdown_rb_stanzaContext ctx) {
    // TODO: see if it is always ok to set active on 'no shutdown'
    _currentPeerGroup.setShutdown(false);
    _currentPeerGroup.setActive(true);
  }

  @Override
  public void exitNtp_access_group(Ntp_access_groupContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    if (!ctx.IPV6().isEmpty()) {
      _configuration.referenceStructure(IPV6_ACCESS_LIST, name, NTP_ACCESS_GROUP, line);
    } else {
      // IPv4 unless IPv6 explicit.
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, NTP_ACCESS_GROUP, line);
    }
  }

  @Override
  public void exitNtp_server(Ntp_serverContext ctx) {
    Ntp ntp = _configuration.getCf().getNtp();
    String hostname = ctx.hostname.getText();
    NtpServer server = ntp.getServers().computeIfAbsent(hostname, NtpServer::new);
    if (ctx.vrf != null) {
      String vrfName = ctx.vrf.getText();
      server.setVrf(vrfName);
      initVrf(vrfName);
    }
    if (ctx.PREFER() != null) {
      // TODO: implement
    }
  }

  @Override
  public void exitNtp_source_interface(Ntp_source_interfaceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
    _configuration.setNtpSourceInterface(ifaceName);
    _configuration.referenceStructure(
        INTERFACE, ifaceName, NTP_SOURCE_INTERFACE, ctx.iname.getStart().getLine());
  }

  @Override
  public void exitNull_as_path_regex(Null_as_path_regexContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitPassive_iis_stanza(Passive_iis_stanzaContext ctx) {
    _currentIsisInterface.setIsisInterfaceMode(IsisInterfaceMode.PASSIVE);
  }

  @Override
  public void exitPassive_interface_default_is_stanza(
      Passive_interface_default_is_stanzaContext ctx) {
    for (Interface iface : _configuration.getInterfaces().values()) {
      iface.setIsisInterfaceMode(IsisInterfaceMode.PASSIVE);
    }
  }

  @Override
  public void exitPassive_interface_is_stanza(Passive_interface_is_stanzaContext ctx) {
    Interface iface = getOrAddInterface(ctx.name);
    if (ctx.NO() == null) {
      iface.setIsisInterfaceMode(IsisInterfaceMode.PASSIVE);
    } else {
      iface.setIsisInterfaceMode(IsisInterfaceMode.ACTIVE);
    }
  }

  @Override
  public void exitPeer_group_assignment_rb_stanza(Peer_group_assignment_rb_stanzaContext ctx) {
    String peerGroupName = ctx.name.getText();
    BgpProcess proc = currentVrf().getBgpProcess();
    if (ctx.address != null) {
      Ip address = toIp(ctx.address);
      IpBgpPeerGroup ipPeerGroup = proc.getIpPeerGroups().get(address);
      if (ipPeerGroup == null) {
        ipPeerGroup = proc.addIpPeerGroup(address);
      }
      ipPeerGroup.setGroupName(peerGroupName);
    } else if (ctx.address6 != null) {
      Ip6 address6 = toIp6(ctx.address6);
      Ipv6BgpPeerGroup ipv6PeerGroup = proc.getIpv6PeerGroups().get(address6);
      if (ipv6PeerGroup == null) {
        ipv6PeerGroup = proc.addIpv6PeerGroup(address6);
      }
      ipv6PeerGroup.setGroupName(peerGroupName);
    }
    _configuration.referenceStructure(
        BGP_PEER_GROUP, peerGroupName, BGP_NEIGHBOR_PEER_GROUP, ctx.name.getStart().getLine());
  }

  @Override
  public void exitPeer_group_creation_rb_stanza(Peer_group_creation_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    BgpProcess proc = currentVrf().getBgpProcess();
    if (proc.getNamedPeerGroups().get(name) == null) {
      NamedBgpPeerGroup npg = proc.addNamedPeerGroup(name);
      if (ctx.PASSIVE() != null) {
        // dell: won't otherwise specify activation so just activate here
        npg.setActive(true);
      }
    }
    _configuration.defineStructure(BGP_PEER_GROUP, name, ctx);
  }

  @Override
  public void exitPeer_sa_filter(Peer_sa_filterContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, MSDP_PEER_SA_LIST, line);
  }

  @Override
  public void exitPim_accept_register(Pim_accept_registerContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, PIM_ACCEPT_REGISTER_ACL, line);
  }

  @Override
  public void exitPim_accept_rp(Pim_accept_rpContext ctx) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      int line = ctx.name.getStart().getLine();
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, PIM_ACCEPT_RP_ACL, line);
    }
  }

  @Override
  public void exitPim_rp_address(Pim_rp_addressContext ctx) {
    if (!_no && ctx.name != null) {
      String name = ctx.name.getText();
      int line = ctx.name.getStart().getLine();
      _configuration.referenceStructure(
          IPV4_ACCESS_LIST, name,
          PIM_RP_ADDRESS_ACL, line);
    }
  }

  @Override
  public void exitPim_rp_announce_filter(Pim_rp_announce_filterContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, PIM_RP_ANNOUNCE_FILTER, line);
  }

  @Override
  public void exitPim_rp_candidate(Pim_rp_candidateContext ctx) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      int line = ctx.name.getStart().getLine();
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, PIM_RP_CANDIDATE_ACL, line);
    }
  }

  @Override
  public void exitPim_send_rp_announce(Pim_send_rp_announceContext ctx) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      int line = ctx.name.getStart().getLine();
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, PIM_SEND_RP_ANNOUNCE_ACL, line);
    }
  }

  @Override
  public void exitPim_spt_threshold(Pim_spt_thresholdContext ctx) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      int line = ctx.name.getStart().getLine();
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, PIM_SPT_THRESHOLD_ACL, line);
    }
  }

  @Override
  public void exitPmtcse_class(Pmtcse_classContext ctx) {
    if (ctx.classname != null) {
      _configuration.referenceStructure(
          CLASS_MAP, ctx.classname.getText(), POLICY_MAP_EVENT_CLASS, ctx.getStart().getLine());
    }
  }

  @Override
  public void exitPmtcsec_activate(Pmtcsec_activateContext ctx) {
    _configuration.referenceStructure(
        DYNAMIC_TEMPLATE,
        ctx.dtname.getText(),
        POLICY_MAP_EVENT_CLASS_ACTIVATE,
        ctx.dtname.getStart().getLine());
  }

  @Override
  public void exitPrefix_list_bgp_tail(Prefix_list_bgp_tailContext ctx) {
    String listName = ctx.list_name.getText();
    int line = ctx.list_name.getLine();
    CiscoXrStructureUsage usage;
    if (_inIpv6BgpPeer) {
      // TODO Support IPv6 prefix-lists in BGP
      if (ctx.IN() != null) {
        usage = BGP_INBOUND_PREFIX6_LIST;
      } else if (ctx.OUT() != null) {
        usage = BGP_OUTBOUND_PREFIX6_LIST;
      } else {
        throw new BatfishException("Invalid direction for BGP prefix-list");
      }
    } else {
      if (ctx.IN() != null) {
        _currentPeerGroup.setInboundPrefixList(listName);
        usage = BGP_INBOUND_PREFIX_LIST;
      } else if (ctx.OUT() != null) {
        _currentPeerGroup.setOutboundPrefixList(listName);
        usage = BGP_OUTBOUND_PREFIX_LIST;
      } else {
        throw new BatfishException("Invalid direction for BGP prefix-list");
      }
    }
    CiscoXrStructureType type = _inIpv6BgpPeer ? PREFIX6_LIST : PREFIX_LIST;
    _configuration.referenceStructure(type, listName, usage, line);
  }

  @Override
  public void exitPrefix_set_elem(Prefix_set_elemContext ctx) {
    String name = _currentPrefixSetName;
    if (name != null) {
      if (ctx.ipa != null || ctx.prefix != null) {
        PrefixList pl = _configuration.getPrefixLists().computeIfAbsent(name, PrefixList::new);
        Prefix prefix;
        if (ctx.ipa != null) {
          prefix = Prefix.create(toIp(ctx.ipa), Prefix.MAX_PREFIX_LENGTH);
        } else {
          prefix = Prefix.parse(ctx.prefix.getText());
        }
        int prefixLength = prefix.getPrefixLength();
        int minLen = prefixLength;
        int maxLen = prefixLength;
        if (ctx.minpl != null) {
          minLen = toInteger(ctx.minpl);
          maxLen = Prefix.MAX_PREFIX_LENGTH;
        }
        if (ctx.maxpl != null) {
          maxLen = toInteger(ctx.maxpl);
        }
        if (ctx.eqpl != null) {
          minLen = toInteger(ctx.eqpl);
          maxLen = toInteger(ctx.eqpl);
        }
        SubRange lengthRange = new SubRange(minLen, maxLen);
        PrefixListLine line = new PrefixListLine(LineAction.PERMIT, prefix, lengthRange);
        pl.addLine(line);
      } else {
        Prefix6List pl = _configuration.getPrefix6Lists().computeIfAbsent(name, Prefix6List::new);
        Prefix6 prefix6;
        if (ctx.ipv6a != null) {
          prefix6 = new Prefix6(toIp6(ctx.ipv6a), Prefix6.MAX_PREFIX_LENGTH);
        } else {
          prefix6 = Prefix6.parse(ctx.ipv6_prefix.getText());
        }
        int prefixLength = prefix6.getPrefixLength();
        int minLen = prefixLength;
        int maxLen = prefixLength;
        if (ctx.minpl != null) {
          minLen = toInteger(ctx.minpl);
          maxLen = Prefix6.MAX_PREFIX_LENGTH;
        }
        if (ctx.maxpl != null) {
          maxLen = toInteger(ctx.maxpl);
        }
        if (ctx.eqpl != null) {
          minLen = toInteger(ctx.eqpl);
          maxLen = toInteger(ctx.eqpl);
        }
        SubRange lengthRange = new SubRange(minLen, maxLen);
        Prefix6ListLine line = new Prefix6ListLine(LineAction.PERMIT, prefix6, lengthRange);
        pl.addLine(line);
      }
    }
  }

  @Override
  public void exitPrefix_set_stanza(Prefix_set_stanzaContext ctx) {
    _currentPrefixSetName = null;
  }

  @Override
  public void exitRedistribute_aggregate_bgp_tail(Redistribute_aggregate_bgp_tailContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitRedistribute_connected_bgp_tail(Redistribute_connected_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    // Intentional identity comparison
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      RoutingProtocol sourceProtocol = RoutingProtocol.CONNECTED;
      BgpRedistributionPolicy r = new BgpRedistributionPolicy(sourceProtocol);
      proc.getRedistributionPolicies().put(sourceProtocol, r);
      if (ctx.metric != null) {
        int metric = toInteger(ctx.metric);
        r.setMetric(metric);
      }
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
    }
  }

  @Override
  public void exitRedistribute_connected_is_stanza(Redistribute_connected_is_stanzaContext ctx) {
    IsisProcess proc = currentVrf().getIsisProcess();
    RoutingProtocol sourceProtocol = RoutingProtocol.CONNECTED;
    IsisRedistributionPolicy r = new IsisRedistributionPolicy(sourceProtocol);
    proc.getRedistributionPolicies().put(sourceProtocol, r);
    if (ctx.metric != null) {
      int metric = toInteger(ctx.metric);
      r.setMetric(metric);
    }
    if (ctx.LEVEL_1() != null) {
      r.setLevel(IsisLevel.LEVEL_1);
    } else if (ctx.LEVEL_2() != null) {
      r.setLevel(IsisLevel.LEVEL_2);
    } else if (ctx.LEVEL_1_2() != null) {
      r.setLevel(IsisLevel.LEVEL_1_2);
    } else {
      r.setLevel(IsisRedistributionPolicy.DEFAULT_LEVEL);
    }
  }

  @Override
  public void exitRedistribute_ospf_bgp_tail(Redistribute_ospf_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    // Intentional identity comparison
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      RoutingProtocol sourceProtocol = RoutingProtocol.OSPF;
      BgpRedistributionPolicy r = new BgpRedistributionPolicy(sourceProtocol);
      proc.getRedistributionPolicies().put(sourceProtocol, r);
      if (ctx.metric != null) {
        int metric = toInteger(ctx.metric);
        r.setMetric(metric);
      }
      if (ctx.MATCH() != null) {
        todo(ctx);
      }
      if (ctx.procname != null) {
        r.getSpecialAttributes()
            .put(BgpRedistributionPolicy.OSPF_PROCESS_NUMBER, ctx.procname.getText());
      }
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
    }
  }

  @Override
  public void exitRedistribute_rip_bgp_tail(Redistribute_rip_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    // Intentional identity comparison
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      RoutingProtocol sourceProtocol = RoutingProtocol.RIP;
      BgpRedistributionPolicy r = new BgpRedistributionPolicy(sourceProtocol);
      proc.getRedistributionPolicies().put(sourceProtocol, r);
      if (ctx.metric != null) {
        int metric = toInteger(ctx.metric);
        r.setMetric(metric);
      }
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
    }
  }

  @Override
  public void exitRedistribute_static_bgp_tail(Redistribute_static_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    // Intentional identity comparison
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      RoutingProtocol sourceProtocol = RoutingProtocol.STATIC;
      BgpRedistributionPolicy r = new BgpRedistributionPolicy(sourceProtocol);
      proc.getRedistributionPolicies().put(sourceProtocol, r);
      if (ctx.metric != null) {
        long metric = toLong(ctx.metric);
        r.setMetric(metric);
      }
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
    }
  }

  @Override
  public void exitRedistribute_static_is_stanza(Redistribute_static_is_stanzaContext ctx) {
    IsisProcess proc = currentVrf().getIsisProcess();
    RoutingProtocol sourceProtocol = RoutingProtocol.STATIC;
    IsisRedistributionPolicy r = new IsisRedistributionPolicy(sourceProtocol);
    proc.getRedistributionPolicies().put(sourceProtocol, r);
    if (ctx.metric != null) {
      int metric = toInteger(ctx.metric);
      r.setMetric(metric);
    }
    if (ctx.LEVEL_1() != null) {
      r.setLevel(IsisLevel.LEVEL_1);
    } else if (ctx.LEVEL_2() != null) {
      r.setLevel(IsisLevel.LEVEL_2);
    } else if (ctx.LEVEL_1_2() != null) {
      r.setLevel(IsisLevel.LEVEL_1_2);
    } else {
      r.setLevel(IsisRedistributionPolicy.DEFAULT_LEVEL);
    }
  }

  @Override
  public void exitRemote_as_bgp_tail(Remote_as_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      throw new BatfishException(
          "no peer or peer group in context: " + getLocation(ctx) + getFullText(ctx));
    }
    long as = toAsNum(ctx.remote);
    _currentPeerGroup.setRemoteAs(as);
    if (ctx.alt_ases != null) {
      _currentPeerGroup.setAlternateAs(
          ctx.alt_ases.stream()
              .map(CiscoXrControlPlaneExtractor::toAsNum)
              .collect(ImmutableSet.toImmutableSet()));
    }
  }

  @Override
  public void exitRemove_private_as_bgp_tail(Remove_private_as_bgp_tailContext ctx) {
    _currentPeerGroup.setRemovePrivateAs(true);
  }

  @Override
  public void exitRo_area(Ro_areaContext ctx) {
    _currentOspfArea = null;
  }

  @Override
  public void exitRo_area_filterlist(Ro_area_filterlistContext ctx) {
    String prefixListName = ctx.list.getText();
    _configuration.referenceStructure(
        PREFIX_LIST, prefixListName, OSPF_AREA_FILTER_LIST, ctx.list.getStart().getLine());
    todo(ctx);
  }

  @Override
  public void exitRo_area_nssa(Ro_area_nssaContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    long area = (ctx.area_int != null) ? toLong(ctx.area_int) : toIp(ctx.area_ip).asLong();
    NssaSettings settings = proc.getNssas().computeIfAbsent(area, a -> new NssaSettings());
    if (ctx.default_information_originate != null) {
      settings.setDefaultInformationOriginate(true);
    }
    if (ctx.no_redistribution != null) {
      settings.setNoRedistribution(true);
    }
    if (ctx.no_summary != null) {
      settings.setNoSummary(true);
    }
  }

  @Override
  public void exitRo_area_range(CiscoXrParser.Ro_area_rangeContext ctx) {
    long areaNum = (ctx.area_int != null) ? toLong(ctx.area_int) : toIp(ctx.area_ip).asLong();
    Prefix prefix;
    if (ctx.area_prefix != null) {
      prefix = Prefix.parse(ctx.area_prefix.getText());
    } else {
      prefix = Prefix.create(toIp(ctx.area_ip), toIp(ctx.area_subnet));
    }
    boolean advertise = ctx.NOT_ADVERTISE() == null;
    Long cost = ctx.cost == null ? null : toLong(ctx.cost);

    Map<Prefix, OspfAreaSummary> area =
        _currentOspfProcess.getSummaries().computeIfAbsent(areaNum, k -> new TreeMap<>());
    area.put(prefix, new OspfAreaSummary(advertise, cost));
  }

  @Override
  public void exitRo_area_stub(Ro_area_stubContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    long area = (ctx.area_int != null) ? toLong(ctx.area_int) : toIp(ctx.area_ip).asLong();
    StubSettings settings = proc.getStubs().computeIfAbsent(area, a -> new StubSettings());
    if (ctx.no_summary != null) {
      settings.setNoSummary(true);
    }
  }

  @Override
  public void exitRo_default_information(Ro_default_informationContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    proc.setDefaultInformationOriginate(true);
    boolean always = ctx.ALWAYS().size() > 0;
    proc.setDefaultInformationOriginateAlways(always);
    if (ctx.metric != null) {
      int metric = toInteger(ctx.metric);
      proc.setDefaultInformationMetric(metric);
    }
    if (ctx.metric_type != null) {
      int metricTypeInt = toInteger(ctx.metric_type);
      OspfMetricType metricType = OspfMetricType.fromInteger(metricTypeInt);
      proc.setDefaultInformationMetricType(metricType);
    }
  }

  @Override
  public void exitRo_default_metric(Ro_default_metricContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    if (ctx.NO() != null) {
      proc.setDefaultMetric(null);
    } else {
      long metric = toLong(ctx.metric);
      proc.setDefaultMetric(metric);
    }
  }

  @Override
  public void exitRe_distribute_list(Re_distribute_listContext ctx) {
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    if (ctx.iname == null) {
      _w.addWarning(
          ctx, getFullText(ctx), _parser, "Global distribute-list not supported for EIGRP");
      return;
    }
    String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
    String filterName = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(
        IP_ACCESS_LIST, filterName, EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_OUT, line);
    _configuration.referenceStructure(
        INTERFACE, ifaceName, EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_OUT, line);
    _currentEigrpProcess
        .getOutboundInterfaceDistributeLists()
        .put(ifaceName, new DistributeList(filterName, DistributeListFilterType.ACCESS_LIST));
  }

  @Override
  public void exitRo_distribute_list(Ro_distribute_listContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    boolean in = ctx.IN() != null;
    CiscoXrStructureType type;
    CiscoXrStructureUsage usage;
    DistributeListFilterType filterType;
    if (ctx.PREFIX() != null) {
      type = PREFIX_LIST;
      filterType = DistributeListFilterType.PREFIX_LIST;
      usage = in ? OSPF_DISTRIBUTE_LIST_PREFIX_LIST_IN : OSPF_DISTRIBUTE_LIST_PREFIX_LIST_OUT;
    } else {
      filterType = DistributeListFilterType.ACCESS_LIST;
      type = IP_ACCESS_LIST;
      usage = in ? OSPF_DISTRIBUTE_LIST_ACCESS_LIST_IN : OSPF_DISTRIBUTE_LIST_ACCESS_LIST_OUT;
      todo(ctx);
    }
    _configuration.referenceStructure(type, name, usage, line);

    DistributeList distributeList = new DistributeList(name, filterType);
    if (ctx.iname != null) {
      String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(INTERFACE, ifaceName, usage, line);
      if (in) {
        _currentOspfProcess.getInboundInterfaceDistributeLists().put(ifaceName, distributeList);
      } else {
        _currentOspfProcess.getOutboundInterfaceDistributeLists().put(ifaceName, distributeList);
      }
    } else {
      if (in) {
        _currentOspfProcess.setInboundGlobalDistributeList(distributeList);
      } else {
        _currentOspfProcess.setOutboundGlobalDistributeList(distributeList);
      }
    }
  }

  @Override
  public void exitRo_maximum_paths(Ro_maximum_pathsContext ctx) {
    todo(ctx);
    /*
     * Note that this is very difficult to enforce, and may not help the
     * analysis without major changes
     */
  }

  @Override
  public void exitRo_network(Ro_networkContext ctx) {
    Ip address;
    Ip wildcard;
    if (ctx.prefix != null) {
      Prefix prefix = Prefix.parse(ctx.prefix.getText());
      address = prefix.getStartIp();
      wildcard = prefix.getPrefixWildcard();
    } else {
      address = toIp(ctx.ip);
      wildcard = toIp(ctx.wildcard);
    }
    long area;
    if (ctx.area_int != null) {
      area = toLong(ctx.area_int);
    } else if (ctx.area_ip != null) {
      area = toIp(ctx.area_ip).asLong();
    } else {
      throw new BatfishException("bad area");
    }
    OspfWildcardNetwork network = new OspfWildcardNetwork(address, wildcard, area);
    _currentOspfProcess.getWildcardNetworks().add(network);
  }

  @Override
  public void exitRo_passive_interface(Ro_passive_interfaceContext ctx) {
    boolean passive = ctx.NO() == null;
    String iname = toInterfaceName(ctx.i);
    OspfProcess proc = _currentOspfProcess;
    if (passive ^ proc.getPassiveInterfaceDefault()) {
      proc.getNonDefaultInterfaces().add(iname);
    }
  }

  @Override
  public void exitRo_passive_interface_default(Ro_passive_interface_defaultContext ctx) {
    boolean passive = ctx.NO() == null;
    OspfProcess proc = _currentOspfProcess;
    proc.setPassiveInterfaceDefault(passive);
    if (_configuration.getVendor() == CISCO_IOS) {
      proc.getNonDefaultInterfaces().clear();
    }
  }

  @Override
  public void exitRo_redistribute_bgp_cisco_xr(Ro_redistribute_bgp_cisco_xrContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    RoutingProtocol sourceProtocol = RoutingProtocol.BGP;
    OspfRedistributionPolicy r = new OspfRedistributionPolicy(sourceProtocol);
    proc.getRedistributionPolicies().put(sourceProtocol, r);
    long as = toAsNum(ctx.bgp_asn());
    r.getSpecialAttributes().put(OspfRedistributionPolicy.BGP_AS, as);
    if (ctx.metric != null) {
      int metric = toInteger(ctx.metric);
      r.setMetric(metric);
    }
    if (ctx.type != null) {
      int typeInt = toInteger(ctx.type);
      OspfMetricType type = OspfMetricType.fromInteger(typeInt);
      r.setOspfMetricType(type);
    } else {
      r.setOspfMetricType(OspfRedistributionPolicy.DEFAULT_METRIC_TYPE);
    }
    if (ctx.tag != null) {
      long tag = toLong(ctx.tag);
      r.setTag(tag);
    }
  }

  @Override
  public void exitRo_redistribute_connected(Ro_redistribute_connectedContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    RoutingProtocol sourceProtocol = RoutingProtocol.CONNECTED;
    OspfRedistributionPolicy r = new OspfRedistributionPolicy(sourceProtocol);
    proc.getRedistributionPolicies().put(sourceProtocol, r);
    if (ctx.metric != null) {
      int metric = toInteger(ctx.metric);
      r.setMetric(metric);
    }
    if (ctx.type != null) {
      int typeInt = toInteger(ctx.type);
      OspfMetricType type = OspfMetricType.fromInteger(typeInt);
      r.setOspfMetricType(type);
    } else {
      r.setOspfMetricType(OspfRedistributionPolicy.DEFAULT_METRIC_TYPE);
    }
    if (ctx.tag != null) {
      long tag = toLong(ctx.tag);
      r.setTag(tag);
    }
  }

  @Override
  public void exitRo_redistribute_eigrp(Ro_redistribute_eigrpContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    RoutingProtocol sourceProtocol = RoutingProtocol.EIGRP;
    OspfRedistributionPolicy r = new OspfRedistributionPolicy(sourceProtocol);
    proc.getRedistributionPolicies().put(sourceProtocol, r);
    long asn = toLong(ctx.tag);
    r.getSpecialAttributes().put(OspfRedistributionPolicy.EIGRP_AS_NUMBER, asn);
    if (ctx.metric != null) {
      int metric = toInteger(ctx.metric);
      r.setMetric(metric);
    }
    if (ctx.type != null) {
      int typeInt = toInteger(ctx.type);
      OspfMetricType type = OspfMetricType.fromInteger(typeInt);
      r.setOspfMetricType(type);
    } else {
      r.setOspfMetricType(OspfRedistributionPolicy.DEFAULT_METRIC_TYPE);
    }
  }

  @Override
  public void exitRo_redistribute_rip(Ro_redistribute_ripContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitRo_redistribute_static(Ro_redistribute_staticContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    RoutingProtocol sourceProtocol = RoutingProtocol.STATIC;
    OspfRedistributionPolicy r = new OspfRedistributionPolicy(sourceProtocol);
    proc.getRedistributionPolicies().put(sourceProtocol, r);
    if (ctx.metric != null) {
      int metric = toInteger(ctx.metric);
      r.setMetric(metric);
    }
    if (ctx.type != null) {
      int typeInt = toInteger(ctx.type);
      OspfMetricType type = OspfMetricType.fromInteger(typeInt);
      r.setOspfMetricType(type);
    } else {
      r.setOspfMetricType(OspfRedistributionPolicy.DEFAULT_METRIC_TYPE);
    }
    if (ctx.tag != null) {
      long tag = toLong(ctx.tag);
      r.setTag(tag);
    }
  }

  @Override
  public void exitRo_rfc1583_compatibility(Ro_rfc1583_compatibilityContext ctx) {
    _currentOspfProcess.setRfc1583Compatible(ctx.NO() == null);
  }

  @Override
  public void exitRo_router_id(Ro_router_idContext ctx) {
    Ip routerId = toIp(ctx.ip);
    _currentOspfProcess.setRouterId(routerId);
  }

  @Override
  public void exitRo_vrf(Ro_vrfContext ctx) {
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
    _currentOspfProcess = currentVrf().getOspfProcesses().get(_lastKnownOspfProcess);
    _lastKnownOspfProcess = null;
  }

  @Override
  public void exitRo6_distribute_list(Ro6_distribute_listContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    boolean in = ctx.IN() != null;
    CiscoXrStructureUsage usage =
        in ? OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_IN : OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_OUT;
    _configuration.referenceStructure(PREFIX6_LIST, name, usage, line);

    if (ctx.iname != null) {
      String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(INTERFACE, ifaceName, usage, line);
    }
  }

  @Override
  public void exitRoa_interface(Roa_interfaceContext ctx) {
    _currentOspfInterface = null;
  }

  @Override
  public void exitRoa_range(Roa_rangeContext ctx) {
    Prefix prefix = Prefix.parse(ctx.prefix.getText());
    boolean advertise = ctx.NOT_ADVERTISE() == null;
    Long cost = ctx.cost == null ? null : toLong(ctx.cost);

    Map<Prefix, OspfAreaSummary> area =
        _currentOspfProcess.getSummaries().computeIfAbsent(_currentOspfArea, k -> new TreeMap<>());
    area.put(prefix, new OspfAreaSummary(advertise, cost));
  }

  @Override
  public void exitRoi_cost(Roi_costContext ctx) {
    Interface iface = _configuration.getInterfaces().get(_currentOspfInterface);
    int cost = toInteger(ctx.cost);
    iface.setOspfCost(cost);
  }

  @Override
  public void exitRoi_passive(Roi_passiveContext ctx) {
    if (ctx.ENABLE() != null) {
      _currentOspfProcess.getPassiveInterfaces().add(_currentOspfInterface);
    }
  }

  @Override
  public void exitRoute_policy_bgp_tail(Route_policy_bgp_tailContext ctx) {
    String name = ctx.name.getText();
    CiscoXrStructureUsage usage;
    if (ctx.IN() != null) {
      _currentPeerGroup.setInboundRouteMap(name);
      usage = BGP_NEIGHBOR_ROUTE_POLICY_IN;
    } else {
      _currentPeerGroup.setOutboundRouteMap(name);
      usage = BGP_NEIGHBOR_ROUTE_POLICY_OUT;
    }
    _configuration.referenceStructure(ROUTE_POLICY, name, usage, ctx.name.getStart().getLine());

    if (ctx.route_policy_params_list() != null && !ctx.route_policy_params_list().isEmpty()) {
      todo(ctx);
    }
  }

  @Override
  public void exitRoute_policy_stanza(Route_policy_stanzaContext ctx) {
    _currentRoutePolicy = null;
  }

  @Override
  public void exitRoute_reflector_client_bgp_tail(Route_reflector_client_bgp_tailContext ctx) {
    _currentPeerGroup.setRouteReflectorClient(true);
  }

  @Override
  public void exitRoute_tail(Route_tailContext ctx) {
    String nextHopInterface = ctx.iface.getText();
    Prefix prefix = Prefix.create(toIp(ctx.destination), toIp(ctx.mask));
    Ip nextHopIp = toIp(ctx.gateway);

    int distance = DEFAULT_STATIC_ROUTE_DISTANCE;
    if (ctx.distance != null) {
      distance = toInteger(ctx.distance);
    }

    Integer track = null;
    if (ctx.track != null) {
      track = toInteger(ctx.track);
    }

    if (ctx.TUNNELED() != null) {
      warn(ctx, "Interface default tunnel gateway option not yet supported.");
    }

    StaticRoute route =
        new StaticRoute(prefix, nextHopIp, nextHopInterface, distance, null, track, false);
    currentVrf().getStaticRoutes().add(route);
  }

  @Override
  public void exitRouter_id_bgp_tail(Router_id_bgp_tailContext ctx) {
    Ip routerId = toIp(ctx.routerid);
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.setRouterId(routerId);
  }

  @Override
  public void exitRe_classic(Re_classicContext ctx) {
    exitEigrpProcess(ctx);
  }

  @Override
  public void exitRouter_isis_stanza(Router_isis_stanzaContext ctx) {
    _currentIsisProcess = null;
  }

  @Override
  public void exitRr_distribute_list(Rr_distribute_listContext ctx) {
    RipProcess proc = _currentRipProcess;
    int line = ctx.getStart().getLine();
    boolean in = ctx.IN() != null;
    String name;
    boolean acl;
    if (ctx.acl != null) {
      name = ctx.acl.getText();
      acl = true;
      _configuration.referenceStructure(IP_ACCESS_LIST, name, RIP_DISTRIBUTE_LIST, line);
    } else {
      name = ctx.prefix_list.getText();
      acl = false;
      _configuration.referenceStructure(PREFIX_LIST, name, RIP_DISTRIBUTE_LIST, line);
    }
    if (in) {
      proc.setDistributeListIn(name);
      proc.setDistributeListInAcl(acl);
      proc.setDistributeListInLine(line);
    } else {
      proc.setDistributeListOut(name);
      proc.setDistributeListOutAcl(acl);
      proc.setDistributeListOutLine(line);
    }
  }

  @Override
  public void exitRr_network(Rr_networkContext ctx) {
    Ip networkAddress = toIp(ctx.network);
    Ip mask = networkAddress.getClassMask();
    Prefix network = Prefix.create(networkAddress, mask);
    _currentRipProcess.getNetworks().add(network);
  }

  @Override
  public void exitRr_passive_interface(Rr_passive_interfaceContext ctx) {
    boolean passive = ctx.NO() == null;
    String iname = ctx.iname.getText();
    RipProcess proc = _currentRipProcess;
    if (passive) {
      proc.getPassiveInterfaceList().add(iname);
    } else {
      proc.getActiveInterfaceList().add(iname);
    }
  }

  @Override
  public void exitRr_passive_interface_default(Rr_passive_interface_defaultContext ctx) {
    boolean no = ctx.NO() != null;
    _currentRipProcess.setPassiveInterfaceDefault(!no);
  }

  @Override
  public void exitRs_route(Rs_routeContext ctx) {
    if (ctx.prefix != null) {
      Prefix prefix = Prefix.parse(ctx.prefix.getText());
      Ip nextHopIp = Route.UNSET_ROUTE_NEXT_HOP_IP;
      String nextHopInterface = null;
      if (ctx.nhip != null) {
        nextHopIp = toIp(ctx.nhip);
      }
      if (ctx.nhint != null) {
        nextHopInterface = getCanonicalInterfaceName(ctx.nhint.getText());
        _configuration.referenceStructure(
            INTERFACE, nextHopInterface, ROUTER_STATIC_ROUTE, ctx.nhint.getStart().getLine());
      }
      int distance = DEFAULT_STATIC_ROUTE_DISTANCE;
      if (ctx.distance != null) {
        distance = toInteger(ctx.distance);
      }
      Long tag = null;
      if (ctx.tag != null) {
        tag = toLong(ctx.tag);
      }

      boolean permanent = ctx.PERMANENT() != null;
      Integer track = null;
      if (ctx.track != null) {
        // TODO: handle named instead of numbered track
      }
      StaticRoute route =
          new StaticRoute(prefix, nextHopIp, nextHopInterface, distance, tag, track, permanent);
      currentVrf().getStaticRoutes().add(route);
    } else if (ctx.prefix6 != null) {
      // TODO: ipv6 static route
    }
  }

  @Override
  public void exitRs_vrf(Rs_vrfContext ctx) {
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
  }

  @Override
  public void exitS_aaa(S_aaaContext ctx) {
    _no = false;
  }

  @Override
  public void exitS_domain_name(S_domain_nameContext ctx) {
    String domainName = ctx.hostname.getText();
    _configuration.setDomainName(domainName);
  }

  @Override
  public void exitS_hostname(S_hostnameContext ctx) {
    String hostname;
    if (ctx.quoted_name != null) {
      hostname = unquote(ctx.quoted_name.getText());
    } else {
      StringBuilder sb = new StringBuilder();
      for (Token namePart : ctx.name_parts) {
        sb.append(namePart.getText());
      }
      hostname = sb.toString();
    }
    _configuration.setHostname(hostname);
    _configuration.getCf().setHostname(hostname);
  }

  @Override
  public void exitS_interface(S_interfaceContext ctx) {
    _currentInterfaces = null;
  }

  @Override
  public void exitS_ip_default_gateway(S_ip_default_gatewayContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitS_ip_dhcp(S_ip_dhcpContext ctx) {
    _no = false;
  }

  @Override
  public void exitS_ip_domain(S_ip_domainContext ctx) {
    _no = false;
  }

  @Override
  public void exitS_ip_domain_name(S_ip_domain_nameContext ctx) {
    String domainName = ctx.hostname.getText();
    _configuration.setDomainName(domainName);
  }

  @Override
  public void exitS_ip_name_server(S_ip_name_serverContext ctx) {
    Set<String> dnsServers = _configuration.getDnsServers();
    for (Ip_hostnameContext ipCtx : ctx.hostnames) {
      String domainName = ipCtx.getText();
      dnsServers.add(domainName);
    }
  }

  @Override
  public void exitS_ip_pim(S_ip_pimContext ctx) {
    _no = false;
  }

  @Override
  public void exitS_ip_source_route(S_ip_source_routeContext ctx) {
    boolean enabled = ctx.NO() == null;
    _configuration.getCf().setSourceRoute(enabled);
  }

  @Override
  public void exitS_ip_tacacs_source_interface(S_ip_tacacs_source_interfaceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
    _configuration.setTacacsSourceInterface(ifaceName);
    _configuration.referenceStructure(
        INTERFACE, ifaceName, IP_TACACS_SOURCE_INTERFACE, ctx.iname.getStart().getLine());
  }

  @Override
  public void exitS_line(S_lineContext ctx) {
    _currentLineNames = null;
  }

  @Override
  public void exitS_logging(S_loggingContext ctx) {
    _no = false;
  }

  @Override
  public void exitS_router_ospf(S_router_ospfContext ctx) {
    _currentOspfProcess.computeNetworks(_configuration.getInterfaces().values());
    _currentOspfProcess = null;
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
  }

  @Override
  public void exitS_router_rip(S_router_ripContext ctx) {
    _currentRipProcess = null;
  }

  @Override
  public void exitS_service(S_serviceContext ctx) {
    List<String> words = ctx.words.stream().map(RuleContext::getText).collect(Collectors.toList());
    boolean enabled = ctx.NO() == null;
    Iterator<String> i = words.iterator();
    SortedMap<String, Service> currentServices = _configuration.getCf().getServices();
    while (i.hasNext()) {
      String name = i.next();
      Service s = currentServices.computeIfAbsent(name, k -> new Service());
      if (enabled) {
        s.setEnabled(true);
      } else if (!i.hasNext()) {
        s.disable();
      }
      currentServices = s.getSubservices();
    }
  }

  @Override
  public void exitS_service_policy_global(S_service_policy_globalContext ctx) {
    _configuration.referenceStructure(
        POLICY_MAP, ctx.name.getText(), SERVICE_POLICY_GLOBAL, ctx.name.getStart().getLine());
  }

  @Override
  public void exitS_spanning_tree(S_spanning_treeContext ctx) {
    _no = false;
  }

  @Override
  public void exitS_system_service_policy(S_system_service_policyContext ctx) {
    _configuration.referenceStructure(
        POLICY_MAP,
        ctx.policy_map.getText(),
        SYSTEM_SERVICE_POLICY,
        ctx.policy_map.getStart().getLine());
  }

  @Override
  public void exitS_switchport(S_switchportContext ctx) {
    if (ctx.ACCESS() != null) {
      _configuration.getCf().setDefaultSwitchportMode(SwitchportMode.ACCESS);
    } else if (ctx.ROUTED() != null) {
      _configuration.getCf().setDefaultSwitchportMode(SwitchportMode.NONE);
    } else {
      throw new BatfishException("Unsupported top-level switchport statement: " + ctx.getText());
    }
  }

  @Override
  public void exitS_tacacs_server(S_tacacs_serverContext ctx) {
    _no = false;
  }

  @Override
  public void exitS_username(S_usernameContext ctx) {
    _currentUser = null;
  }

  @Override
  public void exitS_vrf_definition(S_vrf_definitionContext ctx) {
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
  }

  @Override
  public void exitSd_switchport_blank(Sd_switchport_blankContext ctx) {
    _configuration.getCf().setDefaultSwitchportMode(SwitchportMode.ACCESS);
  }

  @Override
  public void exitSd_switchport_shutdown(Sd_switchport_shutdownContext ctx) {
    _configuration.getCf().setDefaultSwitchportMode(SwitchportMode.NONE);
  }

  @Override
  public void exitSend_community_bgp_tail(Send_community_bgp_tailContext ctx) {
    boolean extended = false;
    boolean standard = false;
    if (ctx.SEND_COMMUNITY() != null) {
      if (ctx.BOTH() != null) {
        extended = true;
        standard = true;
      } else if (ctx.EXTENDED() != null) {
        extended = true;
      } else {
        standard = true;
      }
    } else if (ctx.SEND_COMMUNITY_EBGP() != null) {
      standard = true;
    } else if (ctx.SEND_EXTENDED_COMMUNITY_EBGP() != null) {
      extended = true;
    }
    if (standard) {
      _currentPeerGroup.setSendCommunity(true);
    }
    if (extended) {
      _currentPeerGroup.setSendExtendedCommunity(true);
    }
  }

  @Override
  public void exitSession_group_rb_stanza(Session_group_rb_stanzaContext ctx) {
    _currentIpPeerGroup = null;
    _currentIpv6PeerGroup = null;
    _currentNamedPeerGroup = null;
    _currentPeerSession = null;
    popPeer();
  }

  @Override
  public void exitShutdown_bgp_tail(Shutdown_bgp_tailContext ctx) {
    if (_currentPeerGroup == null) {
      return;
    }
    _currentPeerGroup.setShutdown(true);
  }

  @Override
  public void exitSntp_server(Sntp_serverContext ctx) {
    Sntp sntp = _configuration.getCf().getSntp();
    String hostname = ctx.hostname.getText();
    SntpServer server = sntp.getServers().computeIfAbsent(hostname, SntpServer::new);
    if (ctx.version != null) {
      int version = toInteger(ctx.version);
      server.setVersion(version);
    }
  }

  @Override
  public void exitSpanning_tree_portfast(Spanning_tree_portfastContext ctx) {
    if (ctx.defaultLiteral != null) {
      _configuration.setSpanningTreePortfastDefault(true);
    }
  }

  @Override
  public void exitSs_community(Ss_communityContext ctx) {
    _currentSnmpCommunity = null;
  }

  @Override
  public void exitSs_enable_traps(Ss_enable_trapsContext ctx) {
    if (ctx.snmp_trap_type != null) {
      String trapName = ctx.snmp_trap_type.getText();
      SortedSet<String> subfeatureNames =
          ctx.subfeature.stream().map(RuleContext::getText).collect(toCollection(TreeSet::new));
      SortedMap<String, SortedSet<String>> traps = _configuration.getSnmpServer().getTraps();
      SortedSet<String> subfeatures = traps.get(trapName);
      if (subfeatures == null) {
        traps.put(trapName, subfeatureNames);
      } else {
        subfeatures.addAll(subfeatureNames);
      }
    }
  }

  @Override
  public void exitSs_file_transfer(Ss_file_transferContext ctx) {
    String acl = ctx.acl.getText();
    int line = ctx.acl.getStart().getLine();
    _configuration.referenceStructure(IP_ACCESS_LIST, acl, SNMP_SERVER_FILE_TRANSFER_ACL, line);
  }

  @Override
  public void exitSs_host(Ss_hostContext ctx) {
    _currentSnmpHost = null;
  }

  @Override
  public void exitSs_source_interface(Ss_source_interfaceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
    _configuration.setSnmpSourceInterface(ifaceName);
    _configuration.referenceStructure(
        INTERFACE, ifaceName, SNMP_SERVER_SOURCE_INTERFACE, ctx.iname.getStart().getLine());
  }

  @Override
  public void exitSs_tftp_server_list(Ss_tftp_server_listContext ctx) {
    String acl = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IP_ACCESS_LIST, acl, SNMP_SERVER_TFTP_SERVER_LIST, line);
  }

  @Override
  public void exitSs_trap_source(Ss_trap_sourceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
    _configuration.setSnmpSourceInterface(ifaceName);
    _configuration.referenceStructure(
        IP_ACCESS_LIST, ifaceName, SNMP_SERVER_TRAP_SOURCE, ctx.iname.getStart().getLine());
  }

  @Override
  public void exitSsc_access_control(Ssc_access_controlContext ctx) {
    int line;
    if (ctx.acl4 != null) {
      String name = ctx.acl4.getText();
      line = ctx.acl4.getStart().getLine();
      _currentSnmpCommunity.setAccessList(name);
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, SNMP_SERVER_COMMUNITY_ACL4, line);
    }
    if (ctx.acl6 != null) {
      String name = ctx.acl6.getText();
      line = ctx.acl6.getStart().getLine();
      _currentSnmpCommunity.setAccessList6(name);
      _configuration.referenceStructure(IPV6_ACCESS_LIST, name, SNMP_SERVER_COMMUNITY_ACL6, line);
    }
    if (ctx.readonly != null) {
      _currentSnmpCommunity.setRo(true);
    }
    if (ctx.readwrite != null) {
      _currentSnmpCommunity.setRw(true);
    }
  }

  @Override
  public void exitSsc_use_ipv4_acl(Ssc_use_ipv4_aclContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _currentSnmpCommunity.setAccessList(name);
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, SNMP_SERVER_COMMUNITY_ACL4, line);
  }

  @Override
  public void exitSsh_access_group(Ssh_access_groupContext ctx) {
    String acl = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    if (ctx.IPV6() != null) {
      _configuration.referenceStructure(IPV6_ACCESS_LIST, acl, SSH_IPV6_ACL, line);
    } else {
      _configuration.referenceStructure(IPV4_ACCESS_LIST, acl, SSH_IPV4_ACL, line);
    }
  }

  @Override
  public void exitSsh_server(Ssh_serverContext ctx) {
    if (ctx.acl != null) {
      String acl = ctx.acl.getText();
      int line = ctx.acl.getStart().getLine();
      _configuration.referenceStructure(IPV4_ACCESS_LIST, acl, SSH_IPV4_ACL, line);
    }
    if (ctx.acl6 != null) {
      String acl6 = ctx.acl6.getText();
      int line = ctx.acl6.getStart().getLine();
      _configuration.referenceStructure(IPV6_ACCESS_LIST, acl6, SSH_IPV6_ACL, line);
    }
  }

  @Override
  public void exitSummary_address_is_stanza(Summary_address_is_stanzaContext ctx) {
    Ip ip = toIp(ctx.ip);
    Ip mask = toIp(ctx.mask);
    Prefix prefix = Prefix.create(ip, mask);
    RoutingProtocol sourceProtocol = RoutingProtocol.ISIS_L1;
    IsisRedistributionPolicy r = new IsisRedistributionPolicy(sourceProtocol);
    r.setSummaryPrefix(prefix);
    _currentIsisProcess.getRedistributionPolicies().put(sourceProtocol, r);
    if (ctx.metric != null) {
      int metric = toInteger(ctx.metric);
      r.setMetric(metric);
    }
    if (!ctx.LEVEL_1().isEmpty()) {
      r.setLevel(IsisLevel.LEVEL_1);
    } else if (!ctx.LEVEL_2().isEmpty()) {
      r.setLevel(IsisLevel.LEVEL_2);
    } else if (!ctx.LEVEL_1_2().isEmpty()) {
      r.setLevel(IsisLevel.LEVEL_1_2);
    } else {
      r.setLevel(IsisRedistributionPolicy.DEFAULT_LEVEL);
    }
  }

  @Override
  public void exitSuppressed_iis_stanza(Suppressed_iis_stanzaContext ctx) {
    if (ctx.NO() != null) {
      _currentIsisInterface.setIsisInterfaceMode(IsisInterfaceMode.SUPPRESSED);
    }
  }

  @Override
  public void exitSwitching_mode_stanza(Switching_mode_stanzaContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitT_server(T_serverContext ctx) {
    String hostname = ctx.hostname.getText();
    _configuration.getTacacsServers().add(hostname);
  }

  @Override
  public void exitT_source_interface(T_source_interfaceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
    _configuration.setTacacsSourceInterface(ifaceName);
    _configuration.referenceStructure(
        INTERFACE, ifaceName, TACACS_SOURCE_INTERFACE, ctx.iname.getStart().getLine());
  }

  @Override
  public void exitTemplate_peer_address_family(Template_peer_address_familyContext ctx) {
    popPeer();
  }

  @Override
  public void exitTemplate_peer_policy_rb_stanza(Template_peer_policy_rb_stanzaContext ctx) {
    _currentIpPeerGroup = null;
    _currentIpv6PeerGroup = null;
    _currentNamedPeerGroup = null;
    popPeer();
  }

  @Override
  public void exitTemplate_peer_session_rb_stanza(Template_peer_session_rb_stanzaContext ctx) {
    _currentIpPeerGroup = null;
    _currentIpv6PeerGroup = null;
    _currentNamedPeerGroup = null;
    _currentPeerSession = null;
    popPeer();
  }

  @Override
  public void exitU_password(U_passwordContext ctx) {
    String passwordString;
    if (ctx.up_cisco_xr() != null) {
      passwordString = ctx.up_cisco_xr().up_cisco_xr_tail().pass.getText();
    } else if (ctx.NOPASSWORD() != null) {
      passwordString = "";
    } else {
      throw new BatfishException("Missing username password handling");
    }
    String passwordRehash = CommonUtil.sha256Digest(passwordString + CommonUtil.salt());
    _currentUser.setPassword(passwordRehash);
  }

  @Override
  public void exitU_role(U_roleContext ctx) {
    String role = ctx.role.getText();
    _currentUser.setRole(role);
  }

  @Override
  public void exitUpdate_source_bgp_tail(Update_source_bgp_tailContext ctx) {
    String source = toInterfaceName(ctx.source);
    _configuration.referenceStructure(
        INTERFACE, source, BGP_UPDATE_SOURCE_INTERFACE, ctx.getStart().getLine());

    if (_currentPeerGroup != null) {
      _currentPeerGroup.setUpdateSource(source);
    }
  }

  @Override
  public void exitUse_neighbor_group_bgp_tail(Use_neighbor_group_bgp_tailContext ctx) {
    String groupName = ctx.name.getText();
    _configuration.referenceStructure(
        BGP_NEIGHBOR_GROUP, groupName, BGP_USE_NEIGHBOR_GROUP, ctx.name.getStart().getLine());
    if (_currentIpPeerGroup != null) {
      _currentIpPeerGroup.setGroupName(groupName);
    } else if (_currentIpv6PeerGroup != null) {
      _currentIpv6PeerGroup.setGroupName(groupName);
    } else if (_currentNamedPeerGroup != null) {
      _currentNamedPeerGroup.setGroupName(groupName);
    } else {
      throw new BatfishException("Unexpected context for use neighbor group");
    }
  }

  @Override
  public void exitUse_af_group_bgp_tail(Use_af_group_bgp_tailContext ctx) {
    String groupName = ctx.name.getText();
    _configuration.referenceStructure(
        BGP_AF_GROUP, groupName, BGP_USE_AF_GROUP, ctx.name.getStart().getLine());
    todo(ctx);
  }

  @Override
  public void exitUse_session_group_bgp_tail(Use_session_group_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String groupName = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(BGP_SESSION_GROUP, groupName, BGP_USE_SESSION_GROUP, line);
    if (_currentIpPeerGroup != null) {
      _currentIpPeerGroup.setPeerSession(groupName);
    } else if (_currentNamedPeerGroup != null) {
      _currentNamedPeerGroup.setPeerSession(groupName);
    } else if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      // Intentional identity comparison above
      throw new BatfishException("Invalid peer context for inheritance");
    } else {
      todo(ctx);
    }
  }

  @Override
  public void exitViaf_vrrp(Viaf_vrrpContext ctx) {
    _currentVrrpGroup = null;
  }

  @Override
  public void exitViafv_address(Viafv_addressContext ctx) {
    Ip address = toIp(ctx.address);
    _currentVrrpGroup.setVirtualAddress(address);
  }

  @Override
  public void exitViafv_preempt(Viafv_preemptContext ctx) {
    _currentVrrpGroup.setPreempt(true);
  }

  @Override
  public void exitViafv_priority(Viafv_priorityContext ctx) {
    int priority = toInteger(ctx.priority);
    _currentVrrpGroup.setPriority(priority);
  }

  @Override
  public void exitVrf_block_rb_stanza(Vrf_block_rb_stanzaContext ctx) {
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
    popPeer();
  }

  @Override
  public void exitVrfd_description(Vrfd_descriptionContext ctx) {
    currentVrf().setDescription(getDescription(ctx.description_line()));
  }

  @Override
  public void exitVrrp_interface(Vrrp_interfaceContext ctx) {
    _currentVrrpInterface = null;
  }

  @Override
  public void exitWccp_id(Wccp_idContext ctx) {
    if (ctx.group_list != null) {
      String name = ctx.group_list.getText();
      int line = ctx.group_list.getStart().getLine();
      _configuration.referenceStructure(IP_ACCESS_LIST, name, WCCP_GROUP_LIST, line);
    }
    if (ctx.redirect_list != null) {
      String name = ctx.redirect_list.getText();
      int line = ctx.redirect_list.getStart().getLine();
      _configuration.referenceStructure(IP_ACCESS_LIST, name, WCCP_REDIRECT_LIST, line);
    }
    if (ctx.service_list != null) {
      String name = ctx.service_list.getText();
      int line = ctx.service_list.getStart().getLine();
      _configuration.referenceStructure(IP_ACCESS_LIST, name, WCCP_SERVICE_LIST, line);
    }
  }

  @Nullable
  private String getAddressGroup(Access_list_ip6_rangeContext ctx) {
    if (ctx.address_group != null) {
      return ctx.address_group.getText();
    } else {
      return null;
    }
  }

  private String getCanonicalInterfaceName(String ifaceName) {
    return _configuration.canonicalizeInterfaceName(ifaceName);
  }

  private String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    return _text.substring(start, end + 1);
  }

  private String getLocation(ParserRuleContext ctx) {
    return ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ": ";
  }

  private int getPortNumber(PortContext ctx) {
    if (ctx.DEC() != null) {
      return toInteger(ctx.DEC());
    } else {
      NamedPort namedPort = toNamedPort(ctx);
      return namedPort.number();
    }
  }

  @Override
  public VendorConfiguration getVendorConfiguration() {
    return _configuration;
  }

  private Ip6 getWildcard(Access_list_ip6_rangeContext ctx) {
    if (ctx.wildcard != null) {
      return toIp6(ctx.wildcard);
    } else if (ctx.ANY() != null || ctx.ANY6() != null || ctx.address_group != null) {
      return Ip6.MAX;
    } else if (ctx.HOST() != null) {
      return Ip6.ZERO;
    } else if (ctx.ipv6_prefix != null) {
      return Prefix6.parse(ctx.ipv6_prefix.getText()).getPrefixWildcard();
    } else if (ctx.ip != null) {
      // basically same as host
      return Ip6.ZERO;
    } else {
      throw convError(Ip.class, ctx);
    }
  }

  private void initInterface(Interface iface, Interface_nameContext ctx) {
    String nameAlpha = ctx.name_prefix_alpha.getText();
    String canonicalNamePrefix = CiscoXrConfiguration.getCanonicalInterfaceNamePrefix(nameAlpha);
    String vrf =
        canonicalNamePrefix.equals(CiscoXrConfiguration.MANAGEMENT_INTERFACE_PREFIX)
            ? CiscoXrConfiguration.MANAGEMENT_VRF_NAME
            : Configuration.DEFAULT_VRF_NAME;
    int mtu = Interface.getDefaultMtu();
    iface.setVrf(vrf);
    initVrf(vrf);
    iface.setMtu(mtu);
  }

  private Vrf initVrf(String vrfName) {
    return _configuration.getVrfs().computeIfAbsent(vrfName, Vrf::new);
  }

  private void popPeer() {
    int index = _peerGroupStack.size() - 1;
    _currentPeerGroup = _peerGroupStack.get(index);
    _peerGroupStack.remove(index);
    _inIpv6BgpPeer = false;
  }

  @Override
  public void processParseTree(NetworkSnapshot snapshot, ParserRuleContext tree) {
    ParseTreeWalker walker = new BatfishParseTreeWalker(_parser);
    walker.walk(this, tree);
  }

  private void pushPeer(BgpPeerGroup pg) {
    _peerGroupStack.add(_currentPeerGroup);
    _currentPeerGroup = pg;
  }

  private void resetPeerGroups() {
    _currentDynamicIpPeerGroup = null;
    _currentIpPeerGroup = null;
    _currentIpv6PeerGroup = null;
    _currentNamedPeerGroup = null;
  }

  private AsExpr toAsExpr(As_exprContext ctx) {
    if (ctx.DEC() != null) {
      int as = toInteger(ctx.DEC());
      return new ExplicitAs(as);
    } else if (ctx.AUTO() != null) {
      return AutoAs.instance();
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarAs(ctx.RP_VARIABLE().getText());
    } else {
      throw convError(AsExpr.class, ctx);
    }
  }

  private Optional<AsPathSetElem> toAsPathSetElem(As_path_set_elemContext ctx) {
    if (ctx.ios_regex != null) {
      return Optional.of(toAsPathSetElem(ctx.ios_regex));
    } else {
      warn(ctx, convErrorMessage(AsPathSetElem.class, ctx));
      return Optional.empty();
    }
  }

  private AsPathSetElem toAsPathSetElem(Aspse_ios_regexContext ctx) {
    String withQuotes = ctx.AS_PATH_SET_REGEX().getText();
    String iosRegex = withQuotes.substring(1, withQuotes.length() - 1);
    String regex = toJavaRegex(iosRegex);
    return new RegexAsPathSetElem(regex);
  }

  private AsPathSetExpr toAsPathSetExpr(As_path_set_inlineContext ctx) {
    List<AsPathSetElem> elems =
        ctx.elems.stream()
            .map(this::toAsPathSetElem)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    return new ExplicitAsPathSet(elems);
  }

  private EigrpMetricValues toEigrpMetricValues(Eigrp_metricContext ctx) {
    return EigrpMetricValues.builder()
        .setBandwidth(toLong(ctx.bw_kbps))
        // Scale to picoseconds
        .setDelay(toLong(ctx.delay_10us) * 10_000_000)
        .build();
  }

  @Nonnull
  private EigrpMetric toEigrpMetric(Eigrp_metricContext ctx, EigrpProcessMode mode) {
    /*
     * The other three metrics (reliability, load, and MTU) may be non-zero but are only used if
     * the K constants are configured.
     * See https://github.com/batfish/batfish/issues/1946
     */
    if (mode == EigrpProcessMode.CLASSIC) {
      return ClassicMetric.builder().setValues(toEigrpMetricValues(ctx)).build();
    } else if (mode == EigrpProcessMode.NAMED) {
      return WideMetric.builder().setValues(toEigrpMetricValues(ctx)).build();
    } else {
      throw new IllegalArgumentException("Invalid EIGRP process mode: " + mode);
    }
  }

  private IpsecAuthenticationAlgorithm toIpsecAuthenticationAlgorithm(
      Ipsec_authenticationContext ctx) {
    if (ctx.ESP_MD5_HMAC() != null || ctx.AH_MD5_HMAC() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_MD5_96;
    } else if (ctx.ESP_SHA_HMAC() != null || ctx.AH_SHA_HMAC() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA1_96;
    } else if (ctx.ESP_SHA256_HMAC() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA_256_128;
    } else if (ctx.ESP_SHA512_HMAC() != null) {
      return IpsecAuthenticationAlgorithm.HMAC_SHA_512;
    } else {
      throw convError(IpsecAuthenticationAlgorithm.class, ctx);
    }
  }

  private IntExpr toCommonIntExpr(Int_exprContext ctx) {
    if (ctx.DEC() != null && ctx.PLUS() == null && ctx.DASH() == null) {
      int val = toInteger(ctx.DEC());
      return new LiteralInt(val);
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarInt(ctx.RP_VARIABLE().getText());
    } else {
      /*
       * Unsupported static integer expression - do not add cases unless you
       * know what you are doing
       */
      throw convError(IntExpr.class, ctx);
    }
  }

  private LongExpr toCommonLongExpr(Int_exprContext ctx) {
    if (ctx.DEC() != null && ctx.PLUS() == null && ctx.DASH() == null) {
      long val = toLong(ctx.DEC());
      return new LiteralLong(val);
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarLong(ctx.RP_VARIABLE().getText());
    } else {
      /*
       * Unsupported static long expression - do not add cases unless you
       * know what you are doing
       */
      throw convError(LongExpr.class, ctx);
    }
  }

  private @Nullable XrCommunitySetElem toCommunitySetElemExpr(Community_set_elemContext ctx) {
    if (ctx.prefix != null) {
      Uint16RangeExpr prefix = toCommunitySetElemHalfExpr(ctx.prefix);
      Uint16RangeExpr suffix = toCommunitySetElemHalfExpr(ctx.suffix);
      return new XrCommunitySetHighLowRangeExprs(prefix, suffix);
    } else if (ctx.community() != null) {
      Long value = toLong(ctx.community());
      if (value == null) {
        warn(ctx, String.format("Invalid standard community: '%s'.", ctx.community().getText()));
        return convProblem(XrCommunitySetElem.class, ctx, null);
      }
      return XrCommunitySetHighLowRangeExprs.of(value);
    } else if (ctx.IOS_REGEX() != null) {
      return new XrCommunitySetIosRegex(unquote(ctx.COMMUNITY_SET_REGEX().getText()));
    } else {
      return convProblem(XrCommunitySetElem.class, ctx, null);
    }
  }

  private Uint16RangeExpr toCommunitySetElemHalfExpr(Community_set_elem_halfContext ctx) {
    if (ctx.value != null) {
      int value = toInteger(ctx.value);
      return new LiteralUint16(value);
    } else if (ctx.var != null) {
      String var = ctx.var.getText();
      return new Uint16Reference(var);
    } else if (ctx.first != null) {
      int first = toInteger(ctx.first);
      int last = toInteger(ctx.last);
      SubRange range = new SubRange(first, last);
      return new LiteralUint16Range(range);
    } else if (ctx.ASTERISK() != null) {
      return new LiteralUint16Range(new SubRange(0, 65535));
    } else {
      // For an unhandled expression, treat it as matching everything.
      return convProblem(
          Uint16RangeExpr.class, ctx, new LiteralUint16Range(new SubRange(0, 65535)));
    }
  }

  private @Nullable ExtcommunitySetRtElem toExtcommunitySetRtElemExpr(
      Extcommunity_set_rt_elemContext ctx) {
    if (ctx.extcommunity_set_rt_elem_as_dot_colon() != null) {
      return toExtcommunitySetRtElemExpr(ctx.extcommunity_set_rt_elem_as_dot_colon());
    } else if (ctx.extcommunity_set_rt_elem_colon() != null) {
      return toExtcommunitySetRtElemExpr(ctx.extcommunity_set_rt_elem_colon());
    } else {
      return convProblem(ExtcommunitySetRtElem.class, ctx, null);
    }
  }

  private ExtcommunitySetRtElem toExtcommunitySetRtElemExpr(
      Extcommunity_set_rt_elem_colonContext ctx) {
    return new ExtcommunitySetRtElemAsColon(
        toUint32RangeExpr(ctx.high), toUint16RangeExpr(ctx.low));
  }

  private @Nonnull Uint32RangeExpr toUint32RangeExpr(Extcommunity_set_rt_elem_32Context ctx) {
    // TODO: support other 32-bit range expressions
    return new LiteralUint32(toLong(ctx.uint32()));
  }

  private static @Nonnull ExtcommunitySetRtElem toExtcommunitySetRtElemExpr(
      Extcommunity_set_rt_elem_as_dot_colonContext ctx) {
    return new ExtcommunitySetRtElemAsDotColon(
        toUint16RangeExpr(ctx.high), toUint16RangeExpr(ctx.middle), toUint16RangeExpr(ctx.low));
  }

  private static @Nonnull Uint16RangeExpr toUint16RangeExpr(
      Extcommunity_set_rt_elem_16Context ctx) {
    // TODO: support other 16-bit range expressions
    return new LiteralUint16(toInteger(ctx.uint16()));
  }

  private static int toInteger(Uint16Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private void todo(ParserRuleContext ctx) {
    _w.todo(ctx, getFullText(ctx), _parser);
  }

  private void warn(ParserRuleContext ctx, String message) {
    _w.addWarning(ctx, getFullText(ctx), _parser, message);
  }

  private DiffieHellmanGroup toDhGroup(Dh_groupContext ctx) {
    if (ctx.GROUP1() != null) {
      return DiffieHellmanGroup.GROUP1;
    } else if (ctx.GROUP14() != null) {
      return DiffieHellmanGroup.GROUP14;
    } else if (ctx.GROUP15() != null) {
      return DiffieHellmanGroup.GROUP15;
    } else if (ctx.GROUP16() != null) {
      return DiffieHellmanGroup.GROUP16;
    } else if (ctx.GROUP19() != null) {
      return DiffieHellmanGroup.GROUP19;
    } else if (ctx.GROUP2() != null) {
      return DiffieHellmanGroup.GROUP2;
    } else if (ctx.GROUP21() != null) {
      return DiffieHellmanGroup.GROUP21;
    } else if (ctx.GROUP24() != null) {
      return DiffieHellmanGroup.GROUP24;
    } else if (ctx.GROUP5() != null) {
      return DiffieHellmanGroup.GROUP5;
    } else {
      throw convError(DiffieHellmanGroup.class, ctx);
    }
  }

  private int toDscpType(Dscp_typeContext ctx) {
    int val;
    if (ctx.DEC() != null) {
      val = toInteger(ctx.DEC());
    } else if (ctx.AF11() != null) {
      val = DscpType.AF11.number();
    } else if (ctx.AF12() != null) {
      val = DscpType.AF12.number();
    } else if (ctx.AF13() != null) {
      val = DscpType.AF13.number();
    } else if (ctx.AF21() != null) {
      val = DscpType.AF21.number();
    } else if (ctx.AF22() != null) {
      val = DscpType.AF22.number();
    } else if (ctx.AF23() != null) {
      val = DscpType.AF23.number();
    } else if (ctx.AF31() != null) {
      val = DscpType.AF31.number();
    } else if (ctx.AF32() != null) {
      val = DscpType.AF32.number();
    } else if (ctx.AF33() != null) {
      val = DscpType.AF33.number();
    } else if (ctx.AF41() != null) {
      val = DscpType.AF41.number();
    } else if (ctx.AF42() != null) {
      val = DscpType.AF42.number();
    } else if (ctx.AF43() != null) {
      val = DscpType.AF43.number();
    } else if (ctx.CS1() != null) {
      val = DscpType.CS1.number();
    } else if (ctx.CS2() != null) {
      val = DscpType.CS2.number();
    } else if (ctx.CS3() != null) {
      val = DscpType.CS3.number();
    } else if (ctx.CS4() != null) {
      val = DscpType.CS4.number();
    } else if (ctx.CS5() != null) {
      val = DscpType.CS5.number();
    } else if (ctx.CS6() != null) {
      val = DscpType.CS6.number();
    } else if (ctx.CS7() != null) {
      val = DscpType.CS7.number();
    } else if (ctx.DEFAULT() != null) {
      val = DscpType.DEFAULT.number();
    } else if (ctx.EF() != null) {
      val = DscpType.EF.number();
    } else {
      throw convError(DscpType.class, ctx);
    }
    return val;
  }

  private SwitchportEncapsulationType toEncapsulation(Switchport_trunk_encapsulationContext ctx) {
    if (ctx.DOT1Q() != null) {
      return SwitchportEncapsulationType.DOT1Q;
    } else if (ctx.ISL() != null) {
      return SwitchportEncapsulationType.ISL;
    } else if (ctx.NEGOTIATE() != null) {
      return SwitchportEncapsulationType.NEGOTIATE;
    } else {
      throw convError(SwitchportEncapsulationType.class, ctx);
    }
  }

  private EncryptionAlgorithm toEncryptionAlgorithm(Ike_encryptionContext ctx) {
    if (ctx.AES() != null) {
      int strength = ctx.strength == null ? 128 : toInteger(ctx.strength);
      switch (strength) {
        case 128:
          return EncryptionAlgorithm.AES_128_CBC;
        case 192:
          return EncryptionAlgorithm.AES_192_CBC;
        case 256:
          return EncryptionAlgorithm.AES_256_CBC;
        default:
          throw convError(EncryptionAlgorithm.class, ctx);
      }
    } else if (ctx.DES() != null) {
      return EncryptionAlgorithm.DES_CBC;
    } else if (ctx.THREE_DES() != null) {
      return EncryptionAlgorithm.THREEDES_CBC;
    } else {
      throw convError(EncryptionAlgorithm.class, ctx);
    }
  }

  private EncryptionAlgorithm toEncryptionAlgorithm(Ipsec_encryptionContext ctx) {
    if (ctx.ESP_AES() != null) {
      int strength = ctx.strength == null ? 128 : toInteger(ctx.strength);
      switch (strength) {
        case 128:
          return EncryptionAlgorithm.AES_128_CBC;
        case 192:
          return EncryptionAlgorithm.AES_192_CBC;
        case 256:
          return EncryptionAlgorithm.AES_256_CBC;
        default:
          throw convError(EncryptionAlgorithm.class, ctx);
      }
    } else if (ctx.ESP_DES() != null) {
      return EncryptionAlgorithm.DES_CBC;
    } else if (ctx.ESP_3DES() != null) {
      return EncryptionAlgorithm.THREEDES_CBC;
    } else if (ctx.ESP_GCM() != null) {
      int strength = ctx.strength == null ? 128 : toInteger(ctx.strength);
      switch (strength) {
        case 128:
          return EncryptionAlgorithm.AES_128_GCM;
        case 192:
          return EncryptionAlgorithm.AES_192_GCM;
        case 256:
          return EncryptionAlgorithm.AES_256_GCM;
        default:
          throw convError(EncryptionAlgorithm.class, ctx);
      }
    } else if (ctx.ESP_GMAC() != null) {
      int strength = ctx.strength == null ? 128 : toInteger(ctx.strength);
      switch (strength) {
        case 128:
          return EncryptionAlgorithm.AES_128_GMAC;
        case 192:
          return EncryptionAlgorithm.AES_192_GMAC;
        case 256:
          return EncryptionAlgorithm.AES_256_GMAC;
        default:
          throw convError(EncryptionAlgorithm.class, ctx);
      }
    } else if (ctx.ESP_NULL() != null) {
      return EncryptionAlgorithm.NULL;
    } else if (ctx.ESP_SEAL() != null) {
      return EncryptionAlgorithm.SEAL_160;
    } else {
      throw convError(EncryptionAlgorithm.class, ctx);
    }
  }

  private Integer toIcmpType(Icmp_object_typeContext ctx) {
    if (ctx.ALTERNATE_ADDRESS() != null) {
      return IcmpType.ALTERNATE_ADDRESS;
    } else if (ctx.CONVERSION_ERROR() != null) {
      return IcmpType.CONVERSION_ERROR;
    } else if (ctx.ECHO() != null) {
      return IcmpType.ECHO_REQUEST;
    } else if (ctx.ECHO_REPLY() != null) {
      return IcmpType.ECHO_REPLY;
    } else if (ctx.INFORMATION_REPLY() != null) {
      return IcmpType.INFO_REPLY;
    } else if (ctx.INFORMATION_REQUEST() != null) {
      return IcmpType.INFO_REQUEST;
    } else if (ctx.MASK_REPLY() != null) {
      return IcmpType.MASK_REPLY;
    } else if (ctx.MASK_REQUEST() != null) {
      return IcmpType.MASK_REQUEST;
    } else if (ctx.MOBILE_REDIRECT() != null) {
      return IcmpType.MOBILE_REDIRECT;
    } else if (ctx.PARAMETER_PROBLEM() != null) {
      return IcmpType.PARAMETER_PROBLEM;
    } else if (ctx.REDIRECT() != null) {
      return IcmpType.REDIRECT_MESSAGE;
    } else if (ctx.ROUTER_ADVERTISEMENT() != null) {
      return IcmpType.ROUTER_ADVERTISEMENT;
    } else if (ctx.ROUTER_SOLICITATION() != null) {
      return IcmpType.ROUTER_SOLICITATION;
    } else if (ctx.SOURCE_QUENCH() != null) {
      return IcmpType.SOURCE_QUENCH;
    } else if (ctx.TIME_EXCEEDED() != null) {
      return IcmpType.TIME_EXCEEDED;
    } else if (ctx.TIMESTAMP_REPLY() != null) {
      return IcmpType.TIMESTAMP_REPLY;
    } else if (ctx.TIMESTAMP_REQUEST() != null) {
      return IcmpType.TIMESTAMP_REQUEST;
    } else if (ctx.TRACEROUTE() != null) {
      return IcmpType.TRACEROUTE;
    } else if (ctx.UNREACHABLE() != null) {
      return IcmpType.DESTINATION_UNREACHABLE;
    } else if (ctx.UNSET() != null) {
      return IcmpType.UNSET;
    } else {
      throw convError(IcmpType.class, ctx);
    }
  }

  private IntComparator toIntComparator(Int_compContext ctx) {
    if (ctx.EQ() != null || ctx.IS() != null) {
      return IntComparator.EQ;
    } else if (ctx.GE() != null) {
      return IntComparator.GE;
    } else if (ctx.LE() != null) {
      return IntComparator.LE;
    } else {
      throw convError(IntComparator.class, ctx);
    }
  }

  private IntExpr toIntExpr(Int_exprContext ctx) {
    if (ctx.DEC() != null && ctx.PLUS() == null && ctx.DASH() == null) {
      int val = toInteger(ctx.DEC());
      return new LiteralInt(val);
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarInt(ctx.RP_VARIABLE().getText());
    } else {
      throw convError(IntExpr.class, ctx);
    }
  }

  /** Returns the given IPv4 protocol, or {@code null} if none is specified. */
  private @Nullable IpProtocol toIpProtocol(ProtocolContext ctx) {
    if (ctx.DEC() != null) {
      int num = toInteger(ctx.DEC());
      if (num < 0 || num > 255) {
        return convProblem(IpProtocol.class, ctx, null);
      }
      return IpProtocol.fromNumber(num);
    } else if (ctx.AH() != null || ctx.AHP() != null) {
      // Different CiscoXr variants use `ahp` or `ah` to mean the IPSEC authentication header
      // protocol
      return IpProtocol.AHP;
    } else if (ctx.EIGRP() != null) {
      return IpProtocol.EIGRP;
    } else if (ctx.ESP() != null) {
      return IpProtocol.ESP;
    } else if (ctx.GRE() != null) {
      return IpProtocol.GRE;
    } else if (ctx.ICMP() != null) {
      return IpProtocol.ICMP;
    } else if (ctx.ICMP6() != null || ctx.ICMPV6() != null) {
      return IpProtocol.IPV6_ICMP;
    } else if (ctx.IGMP() != null) {
      return IpProtocol.IGMP;
    } else if (ctx.IP() != null) {
      return null;
    } else if (ctx.IPINIP() != null) {
      return IpProtocol.IPINIP;
    } else if (ctx.IPV4() != null) {
      return null;
    } else if (ctx.IPV6() != null) {
      return IpProtocol.IPV6;
    } else if (ctx.ND() != null) {
      return IpProtocol.IPV6_ICMP;
    } else if (ctx.OSPF() != null) {
      return IpProtocol.OSPF;
    } else if (ctx.PIM() != null) {
      return IpProtocol.PIM;
    } else if (ctx.SCTP() != null) {
      return IpProtocol.SCTP;
    } else if (ctx.TCP() != null) {
      return IpProtocol.TCP;
    } else if (ctx.UDP() != null) {
      return IpProtocol.UDP;
    } else if (ctx.VRRP() != null) {
      return IpProtocol.VRRP;
    } else {
      throw convError(IpProtocol.class, ctx);
    }
  }

  private IsisLevel toIsisLevel(Isis_levelContext ctx) {
    if (ctx.LEVEL_1() != null) {
      return IsisLevel.LEVEL_1;
    } else if (ctx.LEVEL_1_2() != null) {
      return IsisLevel.LEVEL_1_2;
    } else if (ctx.LEVEL_2() != null) {
      return IsisLevel.LEVEL_2;
    } else {
      throw convError(IsisLevel.class, ctx);
    }
  }

  private IsisLevelExpr toIsisLevelExpr(Isis_level_exprContext ctx) {
    if (ctx.isis_level() != null) {
      IsisLevel level = toIsisLevel(ctx.isis_level());
      return new LiteralIsisLevel(level);
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarIsisLevel(ctx.RP_VARIABLE().getText());
    } else {
      throw convError(IsisLevelExpr.class, ctx);
    }
  }

  private IsisMetricType toIsisMetricType(Rp_isis_metric_typeContext ctx) {
    if (ctx.EXTERNAL() != null) {
      return IsisMetricType.EXTERNAL;
    } else if (ctx.INTERNAL() != null) {
      return IsisMetricType.INTERNAL;
    } else if (ctx.RIB_METRIC_AS_EXTERNAL() != null) {
      return IsisMetricType.RIB_METRIC_AS_EXTERNAL;
    } else if (ctx.RIB_METRIC_AS_INTERNAL() != null) {
      return IsisMetricType.RIB_METRIC_AS_INTERNAL;
    } else {
      throw convError(IsisMetricType.class, ctx);
    }
  }

  private String toJavaRegex(String rawRegex) {
    // TODO: fix so it actually works
    return rawRegex;
  }

  private LineAction toLineAction(Access_list_actionContext ctx) {
    if (ctx.PERMIT() != null) {
      return LineAction.PERMIT;
    } else if (ctx.DENY() != null) {
      return LineAction.DENY;
    } else {
      throw convError(LineAction.class, ctx);
    }
  }

  private LongExpr toLocalPreferenceLongExpr(Int_exprContext ctx) {
    if (ctx.DEC() != null) {
      int val = toInteger(ctx.DEC());
      if (ctx.PLUS() != null) {
        return new IncrementLocalPreference(val);
      } else if (ctx.DASH() != null) {
        return new DecrementLocalPreference(val);
      } else {
        return new LiteralLong(val);
      }
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarLong(ctx.RP_VARIABLE().getText());
    } else {
      /*
       * Unsupported local-preference integer expression - do not add cases
       * unless you know what you are doing
       */
      throw convError(IntExpr.class, ctx);
    }
  }

  private String toLoggingSeverity(int severityNum) {
    switch (severityNum) {
      case 0:
        return Logging.SEVERITY_EMERGENCIES;
      case 1:
        return Logging.SEVERITY_ALERTS;
      case 2:
        return Logging.SEVERITY_CRITICAL;
      case 3:
        return Logging.SEVERITY_ERRORS;
      case 4:
        return Logging.SEVERITY_WARNINGS;
      case 5:
        return Logging.SEVERITY_NOTIFICATIONS;
      case 6:
        return Logging.SEVERITY_INFORMATIONAL;
      case 7:
        return Logging.SEVERITY_DEBUGGING;
      default:
        throw new BatfishException("Invalid logging severity: " + severityNum);
    }
  }

  private String toLoggingSeverity(Logging_severityContext ctx) {
    if (ctx.DEC() != null) {
      int severityNum = toInteger(ctx.DEC());
      return toLoggingSeverity(severityNum);
    } else {
      return ctx.getText();
    }
  }

  private Integer toLoggingSeverityNum(Logging_severityContext ctx) {
    if (ctx.DEC() != null) {
      return toInteger(ctx.DEC());
    } else if (ctx.EMERGENCIES() != null) {
      return 0;
    } else if (ctx.ALERTS() != null) {
      return 1;
    } else if (ctx.CRITICAL() != null) {
      return 2;
    } else if (ctx.ERRORS() != null) {
      return 3;
    } else if (ctx.WARNINGS() != null) {
      return 4;
    } else if (ctx.NOTIFICATIONS() != null) {
      return 5;
    } else if (ctx.INFORMATIONAL() != null) {
      return 6;
    } else if (ctx.DEBUGGING() != null) {
      return 7;
    } else {
      throw new BatfishException("Invalid logging severity: " + ctx.getText());
    }
  }

  private @Nullable Long toLong(CommunityContext ctx) {
    if (ctx.ACCEPT_OWN() != null) {
      return WellKnownCommunity.ACCEPT_OWN;
    } else if (ctx.STANDARD_COMMUNITY() != null) {
      return StandardCommunity.parse(ctx.getText()).asLong();
    } else if (ctx.uint32() != null) {
      return toLong(ctx.uint32());
    } else if (ctx.INTERNET() != null) {
      return WellKnownCommunity.INTERNET;
    } else if (ctx.GSHUT() != null) {
      return WellKnownCommunity.GRACEFUL_SHUTDOWN;
    } else if (ctx.LOCAL_AS() != null) {
      // CiscoXr LOCAL_AS is interpreted as RFC1997 NO_EXPORT_SUBCONFED: internet forums.
      return WellKnownCommunity.NO_EXPORT_SUBCONFED;
    } else if (ctx.NO_ADVERTISE() != null) {
      return WellKnownCommunity.NO_ADVERTISE;
    } else if (ctx.NO_EXPORT() != null) {
      return WellKnownCommunity.NO_EXPORT;
    } else {
      return convProblem(Long.class, ctx, null);
    }
  }

  private @Nullable Long toLong(Uint32Context ctx) {
    try {
      long val = Long.parseLong(ctx.getText(), 10);
      checkArgument(0 <= val && val <= 0xFFFFFFFFL);
      return val;
    } catch (IllegalArgumentException e) {
      return convProblem(Long.class, ctx, null);
    }
  }

  private LongExpr toMetricLongExpr(Int_exprContext ctx) {
    if (ctx.DEC() != null) {
      long val = toLong(ctx.DEC());
      if (ctx.PLUS() != null) {
        return new IncrementMetric(val);
      } else if (ctx.DASH() != null) {
        return new DecrementMetric(val);
      } else {
        return new LiteralLong(val);
      }
    } else if (ctx.IGP_COST() != null) {
      return new IgpCost();
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarLong(ctx.RP_VARIABLE().getText());
    } else {
      /*
       * Unsupported metric long expression - do not add cases unless you
       * know what you are doing
       */
      throw convError(LongExpr.class, ctx);
    }
  }

  private NamedPort toNamedPort(PortContext ctx) {
    if (ctx.ACAP() != null) {
      return NamedPort.ACAP;
    } else if (ctx.ACR_NEMA() != null) {
      return NamedPort.ACR_NEMA;
    } else if (ctx.AFPOVERTCP() != null) {
      return NamedPort.AFPOVERTCP;
    } else if (ctx.AOL() != null) {
      return NamedPort.AOL;
    } else if (ctx.ARNS() != null) {
      return NamedPort.ARNS;
    } else if (ctx.ASF_RMCP() != null) {
      return NamedPort.ASF_RMCP;
    } else if (ctx.ASIP_WEBADMIN() != null) {
      return NamedPort.ASIP_WEBADMIN;
    } else if (ctx.AT_RTMP() != null) {
      return NamedPort.AT_RTMP;
    } else if (ctx.AURP() != null) {
      return NamedPort.AURP;
    } else if (ctx.AUTH() != null) {
      return NamedPort.IDENT;
    } else if (ctx.BFD() != null) {
      return NamedPort.BFD_CONTROL;
    } else if (ctx.BFD_ECHO() != null) {
      return NamedPort.BFD_ECHO;
    } else if (ctx.BFTP() != null) {
      return NamedPort.BFTP;
    } else if (ctx.BGMP() != null) {
      return NamedPort.BGMP;
    } else if (ctx.BGP() != null) {
      return NamedPort.BGP;
    } else if (ctx.BIFF() != null) {
      return NamedPort.BIFFudp_OR_EXECtcp;
    } else if (ctx.BOOTPC() != null) {
      return NamedPort.BOOTPC;
    } else if (ctx.BOOTPS() != null) {
      return NamedPort.BOOTPS_OR_DHCP;
    } else if (ctx.CHARGEN() != null) {
      return NamedPort.CHARGEN;
    } else if (ctx.CIFS() != null) {
      return NamedPort.CIFS;
    } else if (ctx.CISCO_TDP() != null) {
      return NamedPort.CISCO_TDP;
    } else if (ctx.CITADEL() != null) {
      return NamedPort.CITADEL;
    } else if (ctx.CITRIX_ICA() != null) {
      return NamedPort.CITRIX_ICA;
    } else if (ctx.CLEARCASE() != null) {
      return NamedPort.CLEARCASE;
    } else if (ctx.CMD() != null) {
      return NamedPort.CMDtcp_OR_SYSLOGudp;
    } else if (ctx.COMMERCE() != null) {
      return NamedPort.COMMERCE;
    } else if (ctx.CSNET_NS() != null) {
      return NamedPort.CSNET_NS;
    } else if (ctx.CTIQBE() != null) {
      return NamedPort.CTIQBE;
    } else if (ctx.DAYTIME() != null) {
      return NamedPort.DAYTIME;
    } else if (ctx.DHCP_FAILOVER2() != null) {
      return NamedPort.DHCP_FAILOVER2;
    } else if (ctx.DHCPV6_CLIENT() != null) {
      return NamedPort.DHCPV6_CLIENT;
    } else if (ctx.DHCPV6_SERVER() != null) {
      return NamedPort.DHCPV6_SERVER;
    } else if (ctx.DISCARD() != null) {
      return NamedPort.DISCARD;
    } else if (ctx.DNSIX() != null) {
      return NamedPort.DNSIX;
    } else if (ctx.DOMAIN() != null) {
      return NamedPort.DOMAIN;
    } else if (ctx.DSP() != null) {
      return NamedPort.DSP;
    } else if (ctx.ECHO() != null) {
      return NamedPort.ECHO;
    } else if (ctx.EFS() != null) {
      return NamedPort.EFStcp_OR_RIPudp;
    } else if (ctx.EPP() != null) {
      return NamedPort.EPP;
    } else if (ctx.ESRO_GEN() != null) {
      return NamedPort.ESRO_GEN;
    } else if (ctx.EXEC() != null) {
      return NamedPort.BIFFudp_OR_EXECtcp;
    } else if (ctx.FINGER() != null) {
      return NamedPort.FINGER;
    } else if (ctx.FTP() != null) {
      return NamedPort.FTP;
    } else if (ctx.FTP_DATA() != null) {
      return NamedPort.FTP_DATA;
    } else if (ctx.FTPS() != null) {
      return NamedPort.FTPS;
    } else if (ctx.FTPS_DATA() != null) {
      return NamedPort.FTPS_DATA;
    } else if (ctx.GODI() != null) {
      return NamedPort.GODI;
    } else if (ctx.GOPHER() != null) {
      return NamedPort.GOPHER;
    } else if (ctx.GTP_C() != null) {
      return NamedPort.GPRS_GTP_C;
    } else if (ctx.GTP_PRIME() != null) {
      return NamedPort.GPRS_GTP_V0;
    } else if (ctx.GTP_U() != null) {
      return NamedPort.GPRS_GTP_U;
    } else if (ctx.H323() != null) {
      return NamedPort.H323;
    } else if (ctx.HA_CLUSTER() != null) {
      return NamedPort.HA_CLUSTER;
    } else if (ctx.HOSTNAME() != null) {
      return NamedPort.HOSTNAME;
    } else if (ctx.HP_ALARM_MGR() != null) {
      return NamedPort.HP_ALARM_MGR;
    } else if (ctx.HTTP() != null) {
      return NamedPort.HTTP;
    } else if (ctx.HTTP_ALT() != null) {
      return NamedPort.HTTP_ALT;
    } else if (ctx.HTTP_MGMT() != null) {
      return NamedPort.HTTP_MGMT;
    } else if (ctx.HTTP_RPC_EPMAP() != null) {
      return NamedPort.HTTP_RPC_EPMAP;
    } else if (ctx.HTTPS() != null) {
      return NamedPort.HTTPS;
    } else if (ctx.IDENT() != null) {
      return NamedPort.IDENT;
    } else if (ctx.IMAP() != null) {
      return NamedPort.IMAP;
    } else if (ctx.IMAP3() != null) {
      return NamedPort.IMAP3;
    } else if (ctx.IMAP4() != null) {
      return NamedPort.IMAP;
    } else if (ctx.IMAPS() != null) {
      return NamedPort.IMAPS;
    } else if (ctx.IPP() != null) {
      return NamedPort.IPP;
    } else if (ctx.IPX() != null) {
      return NamedPort.IPX;
    } else if (ctx.IRC() != null) {
      return NamedPort.IRC;
    } else if (ctx.IRIS_BEEP() != null) {
      return NamedPort.IRIS_BEEP;
    } else if (ctx.ISAKMP() != null) {
      return NamedPort.ISAKMP;
    } else if (ctx.ISCSI() != null) {
      return NamedPort.ISCSI;
    } else if (ctx.ISI_GL() != null) {
      return NamedPort.ISI_GL;
    } else if (ctx.ISO_TSAP() != null) {
      return NamedPort.ISO_TSAP;
    } else if (ctx.KERBEROS() != null) {
      return NamedPort.KERBEROS;
    } else if (ctx.KERBEROS_ADM() != null) {
      return NamedPort.KERBEROS_ADM;
    } else if (ctx.KLOGIN() != null) {
      return NamedPort.KLOGIN;
    } else if (ctx.KPASSWD() != null) {
      return NamedPort.KPASSWDV5;
    } else if (ctx.KSHELL() != null) {
      return NamedPort.KSHELL;
    } else if (ctx.L2TP() != null) {
      return NamedPort.L2TP;
    } else if (ctx.LA_MAINT() != null) {
      return NamedPort.LA_MAINT;
    } else if (ctx.LANZ() != null) {
      return NamedPort.LANZ;
    } else if (ctx.LDAP() != null) {
      return NamedPort.LDAP;
    } else if (ctx.LDAPS() != null) {
      return NamedPort.LDAPS;
    } else if (ctx.LDP() != null) {
      return NamedPort.LDP;
    } else if (ctx.LMP() != null) {
      return NamedPort.LMP;
    } else if (ctx.LPD() != null) {
      return NamedPort.LPD;
    } else if (ctx.LOGIN() != null) {
      return NamedPort.LOGINtcp_OR_WHOudp;
    } else if (ctx.LOTUSNOTES() != null) {
      return NamedPort.LOTUSNOTES;
    } else if (ctx.MAC_SRVR_ADMIN() != null) {
      return NamedPort.MAC_SRVR_ADMIN;
    } else if (ctx.MATIP_TYPE_A() != null) {
      return NamedPort.MATIP_TYPE_A;
    } else if (ctx.MATIP_TYPE_B() != null) {
      return NamedPort.MATIP_TYPE_B;
    } else if (ctx.MICRO_BFD() != null) {
      return NamedPort.BFD_LAG;
    } else if (ctx.MICROSOFT_DS() != null) {
      return NamedPort.MICROSOFT_DS;
    } else if (ctx.MLAG() != null) {
      return NamedPort.MLAG;
    } else if (ctx.MOBILE_IP() != null) {
      return NamedPort.MOBILE_IP_AGENT;
    } else if (ctx.MPP() != null) {
      return NamedPort.MPP;
    } else if (ctx.MS_SQL_M() != null) {
      return NamedPort.MS_SQL_M;
    } else if (ctx.MS_SQL_S() != null) {
      return NamedPort.MS_SQL;
    } else if (ctx.MSDP() != null) {
      return NamedPort.MSDP;
    } else if (ctx.MSEXCH_ROUTING() != null) {
      return NamedPort.MSEXCH_ROUTING;
    } else if (ctx.MSG_ICP() != null) {
      return NamedPort.MSG_ICP;
    } else if (ctx.MSP() != null) {
      return NamedPort.MSP;
    } else if (ctx.MSRPC() != null) {
      return NamedPort.MSRPC;
    } else if (ctx.NAS() != null) {
      return NamedPort.NAS;
    } else if (ctx.NAT() != null) {
      return NamedPort.NAT;
    } else if (ctx.NAMESERVER() != null) {
      return NamedPort.NAMESERVER;
    } else if (ctx.NCP() != null) {
      return NamedPort.NCP;
    } else if (ctx.NETBIOS_DGM() != null) {
      return NamedPort.NETBIOS_DGM;
    } else if (ctx.NETBIOS_NS() != null) {
      return NamedPort.NETBIOS_NS;
    } else if (ctx.NETBIOS_SS() != null) {
      return NamedPort.NETBIOS_SSN;
    } else if (ctx.NETBIOS_SSN() != null) {
      return NamedPort.NETBIOS_SSN;
    } else if (ctx.NETRJS_1() != null) {
      return NamedPort.NETRJS_1;
    } else if (ctx.NETRJS_2() != null) {
      return NamedPort.NETRJS_2;
    } else if (ctx.NETRJS_3() != null) {
      return NamedPort.NETRJS_3;
    } else if (ctx.NETRJS_4() != null) {
      return NamedPort.NETRJS_4;
    } else if (ctx.NETWALL() != null) {
      return NamedPort.NETWALL;
    } else if (ctx.NETWNEWS() != null) {
      return NamedPort.NETWNEWS;
    } else if (ctx.NEW_RWHO() != null) {
      return NamedPort.NEW_RWHO;
    } else if (ctx.NFS() != null) {
      return NamedPort.NFSD;
    } else if (ctx.NNTP() != null) {
      return NamedPort.NNTP;
    } else if (ctx.NNTPS() != null) {
      return NamedPort.NNTPS;
    } else if (ctx.NON500_ISAKMP() != null) {
      return NamedPort.NON500_ISAKMP;
    } else if (ctx.NSW_FE() != null) {
      return NamedPort.NSW_FE;
    } else if (ctx.NTP() != null) {
      return NamedPort.NTP;
    } else if (ctx.ODMR() != null) {
      return NamedPort.ODMR;
    } else if (ctx.OPENVPN() != null) {
      return NamedPort.OPENVPN;
    } else if (ctx.PCANYWHERE_DATA() != null) {
      return NamedPort.PCANYWHERE_DATA;
    } else if (ctx.PCANYWHERE_STATUS() != null) {
      return NamedPort.PCANYWHERE_STATUS;
    } else if (ctx.PIM_AUTO_RP() != null) {
      return NamedPort.PIM_AUTO_RP;
    } else if (ctx.PKIX_TIMESTAMP() != null) {
      return NamedPort.PKIX_TIMESTAMP;
    } else if (ctx.PKT_KRB_IPSEC() != null) {
      return NamedPort.IPSEC;
    } else if (ctx.OLSR() != null) {
      return NamedPort.OLSR;
    } else if (ctx.POP2() != null) {
      return NamedPort.POP2;
    } else if (ctx.POP3() != null) {
      return NamedPort.POP3;
    } else if (ctx.POP3S() != null) {
      return NamedPort.POP3S;
    } else if (ctx.PPTP() != null) {
      return NamedPort.PPTP;
    } else if (ctx.PRINT_SRV() != null) {
      return NamedPort.PRINT_SRV;
    } else if (ctx.PTP_EVENT() != null) {
      return NamedPort.PTP_EVENT;
    } else if (ctx.PTP_GENERAL() != null) {
      return NamedPort.PTP_GENERAL;
    } else if (ctx.QMTP() != null) {
      return NamedPort.QMTP;
    } else if (ctx.QOTD() != null) {
      return NamedPort.QOTD;
    } else if (ctx.RADIUS() != null) {
      return NamedPort.RADIUS_1_AUTH;
    } else if (ctx.RADIUS_ACCT() != null) {
      return NamedPort.RADIUS_1_ACCT;
    } else if (ctx.RE_MAIL_CK() != null) {
      return NamedPort.RE_MAIL_CK;
    } else if (ctx.REMOTEFS() != null) {
      return NamedPort.REMOTEFS;
    } else if (ctx.REPCMD() != null) {
      return NamedPort.REPCMD;
    } else if (ctx.RIP() != null) {
      return NamedPort.EFStcp_OR_RIPudp;
    } else if (ctx.RJE() != null) {
      return NamedPort.RJE;
    } else if (ctx.RLP() != null) {
      return NamedPort.RLP;
    } else if (ctx.RLZDBASE() != null) {
      return NamedPort.RLZDBASE;
    } else if (ctx.RMC() != null) {
      return NamedPort.RMC;
    } else if (ctx.RMONITOR() != null) {
      return NamedPort.RMONITOR;
    } else if (ctx.RPC2PORTMAP() != null) {
      return NamedPort.RPC2PORTMAP;
    } else if (ctx.RSH() != null) {
      return NamedPort.CMDtcp_OR_SYSLOGudp;
    } else if (ctx.RSYNC() != null) {
      return NamedPort.RSYNC;
    } else if (ctx.RTELNET() != null) {
      return NamedPort.RTELNET;
    } else if (ctx.RTSP() != null) {
      return NamedPort.RTSP;
    } else if (ctx.SECUREID_UDP() != null) {
      return NamedPort.SECUREID_UDP;
    } else if (ctx.SGMP() != null) {
      return NamedPort.SGMP;
    } else if (ctx.SILC() != null) {
      return NamedPort.SILC;
    } else if (ctx.SIP() != null) {
      return NamedPort.SIP_5060;
    } else if (ctx.SMTP() != null) {
      return NamedPort.SMTP;
    } else if (ctx.SMUX() != null) {
      return NamedPort.SMUX;
    } else if (ctx.SNAGAS() != null) {
      return NamedPort.SNAGAS;
    } else if (ctx.SNMP() != null) {
      return NamedPort.SNMP;
    } else if (ctx.SNMP_TRAP() != null) {
      return NamedPort.SNMPTRAP;
    } else if (ctx.SNMPTRAP() != null) {
      return NamedPort.SNMPTRAP;
    } else if (ctx.SNPP() != null) {
      return NamedPort.SNPP;
    } else if (ctx.SQLNET() != null) {
      return NamedPort.SQLNET;
    } else if (ctx.SQLSERV() != null) {
      return NamedPort.SQLSERV;
    } else if (ctx.SQLSRV() != null) {
      return NamedPort.SQLSRV;
    } else if (ctx.SSH() != null) {
      return NamedPort.SSH;
    } else if (ctx.SUBMISSION() != null) {
      return NamedPort.SUBMISSION;
    } else if (ctx.SUNRPC() != null) {
      return NamedPort.SUNRPC;
    } else if (ctx.SVRLOC() != null) {
      return NamedPort.SVRLOC;
    } else if (ctx.SYSLOG() != null) {
      return NamedPort.CMDtcp_OR_SYSLOGudp;
    } else if (ctx.SYSTAT() != null) {
      return NamedPort.SYSTAT;
    } else if (ctx.TACACS() != null) {
      return NamedPort.TACACS;
    } else if (ctx.TACACS_DS() != null) {
      return NamedPort.TACACS_DS;
    } else if (ctx.TALK() != null) {
      return NamedPort.TALK;
    } else if (ctx.TBRPF() != null) {
      return NamedPort.TBRPF;
    } else if (ctx.TCPMUX() != null) {
      return NamedPort.TCPMUX;
    } else if (ctx.TCPNETHASPSRV() != null) {
      return NamedPort.TCPNETHASPSRV;
    } else if (ctx.TELNET() != null) {
      return NamedPort.TELNET;
    } else if (ctx.TFTP() != null) {
      return NamedPort.TFTP;
    } else if (ctx.TIME() != null) {
      return NamedPort.TIME;
    } else if (ctx.TIMED() != null) {
      return NamedPort.TIMED;
    } else if (ctx.TUNNEL() != null) {
      return NamedPort.TUNNEL;
    } else if (ctx.UPS() != null) {
      return NamedPort.UPS;
    } else if (ctx.UUCP() != null) {
      return NamedPort.UUCP;
    } else if (ctx.UUCP_PATH() != null) {
      return NamedPort.UUCP_PATH;
    } else if (ctx.VMNET() != null) {
      return NamedPort.VMNET;
    } else if (ctx.VXLAN() != null) {
      return NamedPort.VXLAN;
    } else if (ctx.WHO() != null) {
      return NamedPort.LOGINtcp_OR_WHOudp;
    } else if (ctx.WHOIS() != null) {
      return NamedPort.WHOIS;
    } else if (ctx.WWW() != null) {
      return NamedPort.HTTP;
    } else if (ctx.XDMCP() != null) {
      return NamedPort.XDMCP;
    } else if (ctx.XNS_CH() != null) {
      return NamedPort.XNS_CH;
    } else if (ctx.XNS_MAIL() != null) {
      return NamedPort.XNS_MAIL;
    } else if (ctx.XNS_TIME() != null) {
      return NamedPort.XNS_TIME;
    } else if (ctx.Z39_50() != null) {
      return NamedPort.Z39_50;
    } else {
      throw convError(NamedPort.class, ctx);
    }
  }

  private OriginExpr toOriginExpr(Origin_expr_literalContext ctx) {
    OriginType originType;
    Long asNum = null;
    LiteralOrigin originExpr;
    if (ctx.IGP() != null) {
      originType = OriginType.IGP;
    } else if (ctx.INCOMPLETE() != null) {
      originType = OriginType.INCOMPLETE;
    } else if (ctx.bgp_asn() != null) {
      asNum = toAsNum(ctx.bgp_asn());
      originType = OriginType.IGP;
    } else {
      throw convError(OriginExpr.class, ctx);
    }
    originExpr = new LiteralOrigin(originType, asNum);
    return originExpr;
  }

  private OriginExpr toOriginExpr(Origin_exprContext ctx) {
    if (ctx.origin_expr_literal() != null) {
      return toOriginExpr(ctx.origin_expr_literal());
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarOrigin(ctx.RP_VARIABLE().getText());
    } else {
      throw convError(OriginExpr.class, ctx);
    }
  }

  private OspfMetricType toOspfMetricType(Rp_ospf_metric_typeContext ctx) {
    if (ctx.TYPE_1() != null) {
      return OspfMetricType.E1;
    } else if (ctx.TYPE_2() != null) {
      return OspfMetricType.E2;
    } else {
      throw convError(OspfMetricType.class, ctx);
    }
  }

  private List<SubRange> toPortRanges(Port_specifierContext ps) {
    Builder builder = IntegerSpace.builder();
    if (ps.EQ() != null) {
      ps.args.forEach(p -> builder.including(getPortNumber(p)));
    } else if (ps.GT() != null) {
      int port = getPortNumber(ps.arg);
      builder.including(new SubRange(port + 1, 65535));
    } else if (ps.NEQ() != null) {
      builder.including(IntegerSpace.PORTS);
      ps.args.forEach(p -> builder.excluding(getPortNumber(p)));
    } else if (ps.LT() != null) {
      int port = getPortNumber(ps.arg);
      builder.including(new SubRange(0, port - 1));
    } else if (ps.RANGE() != null) {
      int lowPort = getPortNumber(ps.arg1);
      int highPort = getPortNumber(ps.arg2);
      builder.including(new SubRange(lowPort, highPort));
    } else {
      throw convError(List.class, ps);
    }
    return builder.build().getSubRanges().stream()
        .sorted() // for output/ref determinism
        .collect(ImmutableList.toImmutableList());
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_and_rp_stanzaContext ctx) {
    if (ctx.AND() == null) {
      return toRoutePolicyBoolean(ctx.boolean_not_rp_stanza());
    } else {
      return new RoutePolicyBooleanAnd(
          toRoutePolicyBoolean(ctx.boolean_and_rp_stanza()),
          toRoutePolicyBoolean(ctx.boolean_not_rp_stanza()));
    }
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_apply_rp_stanzaContext ctx) {
    String name = ctx.name.getText();
    _configuration.referenceStructure(
        ROUTE_POLICY, name, ROUTE_POLICY_APPLY, ctx.name.getStart().getLine());
    return new RoutePolicyBooleanApply(name);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_as_path_in_rp_stanzaContext ctx) {
    AsPathSetExpr asPathSetExpr;
    if (ctx.expr.named != null) {
      String name = ctx.expr.named.getText();
      asPathSetExpr = new NamedAsPathSet(name);
      _configuration.referenceStructure(
          AS_PATH_SET, name, ROUTE_POLICY_AS_PATH_IN, ctx.expr.named.getStart().getLine());
    } else if (ctx.expr.rpvar != null) {
      asPathSetExpr = new VarAsPathSet(ctx.expr.rpvar.getText());
    } else if (ctx.expr.inline != null) {
      asPathSetExpr = toAsPathSetExpr(ctx.expr.inline);
    } else {
      throw convError(AsPathSetExpr.class, ctx.expr);
    }
    return new RoutePolicyBooleanAsPathIn(asPathSetExpr);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_as_path_neighbor_is_rp_stanzaContext ctx) {
    List<SubRangeExpr> range =
        ctx.as_range_expr().subranges.stream()
            .map(this::toSubRangeExpr)
            .collect(Collectors.toList());
    boolean exact = ctx.as_range_expr().EXACT() != null;
    return new RoutePolicyBooleanAsPathNeighborIs(range, exact);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_as_path_originates_from_rp_stanzaContext ctx) {
    List<SubRangeExpr> range =
        ctx.as_range_expr().subranges.stream()
            .map(this::toSubRangeExpr)
            .collect(Collectors.toList());
    boolean exact = ctx.as_range_expr().EXACT() != null;
    return new RoutePolicyBooleanAsPathOriginatesFrom(range, exact);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_as_path_passes_through_rp_stanzaContext ctx) {
    List<SubRangeExpr> range =
        ctx.as_range_expr().subranges.stream()
            .map(this::toSubRangeExpr)
            .collect(Collectors.toList());
    boolean exact = ctx.as_range_expr().EXACT() != null;
    return new RoutePolicyBooleanAsPathPassesThrough(range, exact);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_community_matches_any_rp_stanzaContext ctx) {
    XrCommunitySetExpr communitySet =
        toRoutePolicyCommunitySet(ctx.rp_community_set(), ROUTE_POLICY_COMMUNITY_MATCHES_ANY);
    return new XrRoutePolicyBooleanCommunityMatchesAny(communitySet);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_community_matches_every_rp_stanzaContext ctx) {
    XrCommunitySetExpr communitySet =
        toRoutePolicyCommunitySet(ctx.rp_community_set(), ROUTE_POLICY_COMMUNITY_MATCHES_EVERY);
    return new XrRoutePolicyBooleanCommunityMatchesEvery(communitySet);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_destination_rp_stanzaContext ctx) {
    RoutePolicyPrefixSet prefixSet = toRoutePolicyPrefixSet(ctx.rp_prefix_set());
    return new RoutePolicyBooleanDestination(prefixSet);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_local_preference_rp_stanzaContext ctx) {
    IntComparator cmp = toIntComparator(ctx.int_comp());
    LongExpr rhs = toCommonLongExpr(ctx.rhs);
    return new RoutePolicyBooleanLocalPreference(cmp, rhs);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_med_rp_stanzaContext ctx) {
    IntComparator cmp = toIntComparator(ctx.int_comp());
    LongExpr rhs = toCommonLongExpr(ctx.rhs);
    return new RoutePolicyBooleanMed(cmp, rhs);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_next_hop_in_rp_stanzaContext ctx) {
    RoutePolicyPrefixSet prefixSet = toRoutePolicyPrefixSet(ctx.rp_prefix_set());
    return new RoutePolicyBooleanNextHopIn(prefixSet);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_not_rp_stanzaContext ctx) {
    if (ctx.NOT() == null) {
      return toRoutePolicyBoolean(ctx.boolean_simple_rp_stanza());
    } else {
      return new RoutePolicyBooleanNot(toRoutePolicyBoolean(ctx.boolean_simple_rp_stanza()));
    }
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_rib_has_route_rp_stanzaContext ctx) {
    return new RoutePolicyBooleanRibHasRoute(toRoutePolicyPrefixSet(ctx.rp_prefix_set()));
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_route_type_is_rp_stanzaContext ctx) {
    RouteTypeExpr type = toRouteType(ctx.type);
    return new RoutePolicyBooleanRouteTypeIs(type);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_rp_stanzaContext ctx) {
    if (ctx.OR() == null) {
      return toRoutePolicyBoolean(ctx.boolean_and_rp_stanza());
    } else {
      return new RoutePolicyBooleanOr(
          toRoutePolicyBoolean(ctx.boolean_rp_stanza()),
          toRoutePolicyBoolean(ctx.boolean_and_rp_stanza()));
    }
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_simple_rp_stanzaContext ctx) {
    Boolean_rp_stanzaContext bctx = ctx.boolean_rp_stanza();
    if (bctx != null) {
      return toRoutePolicyBoolean(bctx);
    }

    Boolean_apply_rp_stanzaContext actx = ctx.boolean_apply_rp_stanza();
    if (actx != null) {
      return toRoutePolicyBoolean(actx);
    }

    Boolean_as_path_in_rp_stanzaContext aictx = ctx.boolean_as_path_in_rp_stanza();
    if (aictx != null) {
      return toRoutePolicyBoolean(aictx);
    }

    Boolean_as_path_is_local_rp_stanzaContext alctx = ctx.boolean_as_path_is_local_rp_stanza();
    if (alctx != null) {
      return new RoutePolicyBooleanAsPathIsLocal();
    }

    Boolean_as_path_neighbor_is_rp_stanzaContext anctx =
        ctx.boolean_as_path_neighbor_is_rp_stanza();
    if (anctx != null) {
      return toRoutePolicyBoolean(anctx);
    }

    Boolean_as_path_originates_from_rp_stanzaContext aoctx =
        ctx.boolean_as_path_originates_from_rp_stanza();
    if (aoctx != null) {
      return toRoutePolicyBoolean(aoctx);
    }

    Boolean_as_path_passes_through_rp_stanzaContext apctx =
        ctx.boolean_as_path_passes_through_rp_stanza();
    if (apctx != null) {
      return toRoutePolicyBoolean(apctx);
    }

    Boolean_community_matches_any_rp_stanzaContext cmactx =
        ctx.boolean_community_matches_any_rp_stanza();
    if (cmactx != null) {
      return toRoutePolicyBoolean(cmactx);
    }

    Boolean_community_matches_every_rp_stanzaContext cmectx =
        ctx.boolean_community_matches_every_rp_stanza();
    if (cmectx != null) {
      return toRoutePolicyBoolean(cmectx);
    }

    Boolean_destination_rp_stanzaContext dctx = ctx.boolean_destination_rp_stanza();
    if (dctx != null) {
      return toRoutePolicyBoolean(dctx);
    }

    Boolean_local_preference_rp_stanzaContext lctx = ctx.boolean_local_preference_rp_stanza();
    if (lctx != null) {
      return toRoutePolicyBoolean(lctx);
    }

    Boolean_med_rp_stanzaContext mctx = ctx.boolean_med_rp_stanza();
    if (mctx != null) {
      return toRoutePolicyBoolean(mctx);
    }

    Boolean_next_hop_in_rp_stanzaContext nctx = ctx.boolean_next_hop_in_rp_stanza();
    if (nctx != null) {
      return toRoutePolicyBoolean(nctx);
    }

    Boolean_rib_has_route_rp_stanzaContext ribctx = ctx.boolean_rib_has_route_rp_stanza();
    if (ribctx != null) {
      return toRoutePolicyBoolean(ribctx);
    }

    Boolean_route_type_is_rp_stanzaContext routectx = ctx.boolean_route_type_is_rp_stanza();
    if (routectx != null) {
      return toRoutePolicyBoolean(routectx);
    }

    Boolean_tag_is_rp_stanzaContext tctx = ctx.boolean_tag_is_rp_stanza();
    if (tctx != null) {
      return toRoutePolicyBoolean(tctx);
    }

    throw convError(RoutePolicyBoolean.class, ctx);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_tag_is_rp_stanzaContext ctx) {
    IntComparator cmp = toIntComparator(ctx.int_comp());
    LongExpr rhs = toTagLongExpr(ctx.int_expr());
    return new RoutePolicyBooleanTagIs(cmp, rhs);
  }

  private XrCommunitySetExpr toRoutePolicyCommunitySet(
      Rp_community_setContext ctx, CiscoXrStructureUsage usage) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      _configuration.referenceStructure(COMMUNITY_SET, name, usage, ctx.name.getStart().getLine());
      return new XrCommunitySetReference(name);
    } else {
      // inline
      return new XrInlineCommunitySet(
          new XrCommunitySet(
              ctx.elems.stream()
                  .map(this::toCommunitySetElemExpr)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList())));
    }
  }

  private RoutePolicyElseBlock toRoutePolicyElseBlock(Else_rp_stanzaContext ctx) {
    List<RoutePolicyStatement> stmts = toRoutePolicyStatementList(ctx.rp_stanza());
    return new RoutePolicyElseBlock(stmts);
  }

  private RoutePolicyElseIfBlock toRoutePolicyElseIfBlock(Elseif_rp_stanzaContext ctx) {
    RoutePolicyBoolean b = toRoutePolicyBoolean(ctx.boolean_rp_stanza());
    List<RoutePolicyStatement> stmts = toRoutePolicyStatementList(ctx.rp_stanza());
    return new RoutePolicyElseIfBlock(b, stmts);
  }

  private RoutePolicyPrefixSet toRoutePolicyPrefixSet(Rp_prefix_setContext ctx) {
    if (ctx.name != null) {
      // named
      String name = ctx.name.getText();
      _configuration.referenceStructure(
          PREFIX_SET, name, ROUTE_POLICY_PREFIX_SET, ctx.name.getStart().getLine());
      return new RoutePolicyPrefixSetName(name);
    } else {
      // inline
      PrefixSpace prefixSpace = new PrefixSpace();
      Prefix6Space prefix6Space = new Prefix6Space();
      boolean ipv6 = false;
      for (Prefix_set_elemContext pctxt : ctx.elems) {
        int lower;
        int upper;
        Prefix prefix = null;
        Prefix6 prefix6 = null;
        if (pctxt.prefix != null) {
          prefix = Prefix.parse(pctxt.prefix.getText());
          lower = prefix.getPrefixLength();
          upper = Prefix.MAX_PREFIX_LENGTH;
        } else if (pctxt.ipa != null) {
          prefix = Prefix.create(toIp(pctxt.ipa), Prefix.MAX_PREFIX_LENGTH);
          lower = prefix.getPrefixLength();
          upper = Prefix.MAX_PREFIX_LENGTH;
        } else if (pctxt.ipv6a != null) {
          prefix6 = new Prefix6(toIp6(pctxt.ipv6a), Prefix6.MAX_PREFIX_LENGTH);
          lower = prefix6.getPrefixLength();
          upper = Prefix6.MAX_PREFIX_LENGTH;
        } else if (pctxt.ipv6_prefix != null) {
          prefix6 = Prefix6.parse(pctxt.ipv6_prefix.getText());
          lower = prefix6.getPrefixLength();
          upper = Prefix6.MAX_PREFIX_LENGTH;
        } else {
          throw new BatfishException("Unhandled alternative");
        }
        if (pctxt.minpl != null) {
          lower = toInteger(pctxt.minpl);
        }
        if (pctxt.maxpl != null) {
          upper = toInteger(pctxt.maxpl);
        }
        if (pctxt.eqpl != null) {
          lower = toInteger(pctxt.eqpl);
          upper = lower;
        }
        if (prefix != null) {
          prefixSpace.addPrefixRange(new PrefixRange(prefix, new SubRange(lower, upper)));
        } else {
          prefix6Space.addPrefix6Range(new Prefix6Range(prefix6, new SubRange(lower, upper)));
          ipv6 = true;
        }
      }
      if (ipv6) {
        return new RoutePolicyInlinePrefix6Set(prefix6Space);
      } else {
        return new RoutePolicyInlinePrefixSet(prefixSpace);
      }
    }
  }

  private RoutePolicyApplyStatement toRoutePolicyStatement(Apply_rp_stanzaContext ctx) {
    return new RoutePolicyApplyStatement(ctx.name.getText());
  }

  private RoutePolicyStatement toRoutePolicyStatement(Delete_community_rp_stanzaContext ctx) {
    if (ctx.ALL() != null) {
      return XrRoutePolicyDeleteAllStatement.instance();
    } else {
      boolean negated = (ctx.NOT() != null);
      return new XrRoutePolicyDeleteCommunityStatement(
          negated,
          toRoutePolicyCommunitySet(ctx.rp_community_set(), ROUTE_POLICY_DELETE_COMMUNITY_IN));
    }
  }

  private RoutePolicyDispositionStatement toRoutePolicyStatement(Disposition_rp_stanzaContext ctx) {
    RoutePolicyDispositionType t = null;
    if (ctx.DONE() != null) {
      t = RoutePolicyDispositionType.DONE;
    } else if (ctx.DROP() != null) {
      t = RoutePolicyDispositionType.DROP;
    } else if (ctx.PASS() != null) {
      t = RoutePolicyDispositionType.PASS;
    } else if (ctx.UNSUPPRESS_ROUTE() != null) {
      t = RoutePolicyDispositionType.UNSUPPRESS_ROUTE;
    }
    return new RoutePolicyDispositionStatement(t);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Hash_commentContext ctx) {
    String text = ctx.RAW_TEXT().getText();
    return new RoutePolicyComment(text);
  }

  private RoutePolicyIfStatement toRoutePolicyStatement(If_rp_stanzaContext ctx) {
    RoutePolicyBoolean b = toRoutePolicyBoolean(ctx.boolean_rp_stanza());
    List<RoutePolicyStatement> stmts = toRoutePolicyStatementList(ctx.rp_stanza());
    List<RoutePolicyElseIfBlock> elseIfs = new ArrayList<>();
    for (Elseif_rp_stanzaContext ectxt : ctx.elseif_rp_stanza()) {
      elseIfs.add(toRoutePolicyElseIfBlock(ectxt));
    }
    RoutePolicyElseBlock els = null;
    Else_rp_stanzaContext elctxt = ctx.else_rp_stanza();
    if (elctxt != null) {
      els = toRoutePolicyElseBlock(elctxt);
    }
    return new RoutePolicyIfStatement(b, stmts, elseIfs, els);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Prepend_as_path_rp_stanzaContext ctx) {
    AsExpr expr = toAsExpr(ctx.as);
    IntExpr number = null;
    if (ctx.number != null) {
      number = toIntExpr(ctx.number);
    }
    return new RoutePolicyPrependAsPath(expr, number);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Rp_stanzaContext ctx) {
    Apply_rp_stanzaContext actx = ctx.apply_rp_stanza();
    if (actx != null) {
      return toRoutePolicyStatement(actx);
    }

    Delete_community_rp_stanzaContext dectx = ctx.delete_community_rp_stanza();
    if (dectx != null) {
      return toRoutePolicyStatement(dectx);
    }

    Disposition_rp_stanzaContext dictx = ctx.disposition_rp_stanza();
    if (dictx != null) {
      return toRoutePolicyStatement(dictx);
    }

    Hash_commentContext hctx = ctx.hash_comment();
    if (hctx != null) {
      return toRoutePolicyStatement(hctx);
    }

    If_rp_stanzaContext ictx = ctx.if_rp_stanza();
    if (ictx != null) {
      return toRoutePolicyStatement(ictx);
    }

    Set_rp_stanzaContext sctx = ctx.set_rp_stanza();
    if (sctx != null) {
      return toRoutePolicyStatement(sctx);
    }

    throw convError(RoutePolicyStatement.class, ctx);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_community_rp_stanzaContext ctx) {
    XrCommunitySetExpr cset =
        toRoutePolicyCommunitySet(ctx.rp_community_set(), ROUTE_POLICY_SET_COMMUNITY);
    boolean additive = (ctx.ADDITIVE() != null);
    return new XrRoutePolicySetCommunity(cset, additive);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_extcommunity_rp_stanzaContext ctx) {
    if (ctx.set_extcommunity_rt() != null) {
      return toRoutePolicyStatement(ctx.set_extcommunity_rt());
    } else {
      throw convError(RoutePolicyStatement.class, ctx);
    }
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_extcommunity_rtContext ctx) {
    ExtcommunitySetRtExpr expr =
        toExtcommunitySetRtExpr(ctx.rp_extcommunity_set_rt(), ROUTE_POLICY_SET_EXTCOMMUNITY_RT);
    boolean additive = (ctx.ADDITIVE() != null);
    return new RoutePolicySetExtcommunityRt(expr, additive);
  }

  private ExtcommunitySetRtExpr toExtcommunitySetRtExpr(
      Rp_extcommunity_set_rtContext ctx, CiscoXrStructureUsage usage) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      _configuration.referenceStructure(
          EXTCOMMUNITY_SET_RT, name, usage, ctx.name.getStart().getLine());
      return new ExtcommunitySetRtReference(name);
    } else {
      // inline
      return new InlineExtcommunitySetRt(
          new ExtcommunitySetRt(
              ctx.elems.stream()
                  .map(this::toExtcommunitySetRtElemExpr)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList())));
    }
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_isis_metric_rp_stanzaContext ctx) {
    LongExpr metric = toCommonLongExpr(ctx.int_expr());
    return new RoutePolicySetIsisMetric(metric);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_level_rp_stanzaContext ctx) {
    return new RoutePolicySetLevel(toIsisLevelExpr(ctx.isis_level_expr()));
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_local_preference_rp_stanzaContext ctx) {
    return new RoutePolicySetLocalPref(toLocalPreferenceLongExpr(ctx.pref));
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_med_rp_stanzaContext ctx) {
    return new RoutePolicySetMed(toMetricLongExpr(ctx.med));
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_metric_type_rp_stanzaContext ctx) {
    Rp_metric_typeContext t = ctx.type;
    if (t.rp_ospf_metric_type() != null) {
      return new RoutePolicySetOspfMetricType(toOspfMetricType(t.rp_ospf_metric_type()));
    } else if (t.rp_isis_metric_type() != null) {
      return new RoutePolicySetIsisMetricType(toIsisMetricType(t.rp_isis_metric_type()));
    } else if (t.RP_VARIABLE() != null) {
      return new RoutePolicySetVarMetricType(t.RP_VARIABLE().getText());
    } else {
      throw convError(RoutePolicyStatement.class, ctx);
    }
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_next_hop_rp_stanzaContext ctx) {
    RoutePolicyNextHop hop = null;
    if (ctx.DISCARD() != null) {
      hop = new RoutePolicyNextHopDiscard();
    } else if (ctx.IP_ADDRESS() != null) {
      hop = new RoutePolicyNextHopIp(toIp(ctx.IP_ADDRESS()));
    } else if (ctx.IPV6_ADDRESS() != null) {
      hop = new RoutePolicyNextHopIP6(toIp6(ctx.IPV6_ADDRESS()));
    } else if (ctx.PEER_ADDRESS() != null) {
      hop = new RoutePolicyNextHopPeerAddress();
    }
    boolean destVrf = (ctx.DESTINATION_VRF() != null);
    if (destVrf) {
      warn(ctx, "Unimplemented 'destination-vrf' directive.");
    }
    return new RoutePolicySetNextHop(hop, destVrf);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_origin_rp_stanzaContext ctx) {
    OriginExpr origin = toOriginExpr(ctx.origin_expr());
    return new RoutePolicySetOrigin(origin);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_rp_stanzaContext ctx) {
    Prepend_as_path_rp_stanzaContext pasctx = ctx.prepend_as_path_rp_stanza();
    if (pasctx != null) {
      return toRoutePolicyStatement(pasctx);
    }

    Set_community_rp_stanzaContext cctx = ctx.set_community_rp_stanza();
    if (cctx != null) {
      return toRoutePolicyStatement(cctx);
    }

    {
      Set_extcommunity_rp_stanzaContext child = ctx.set_extcommunity_rp_stanza();
      if (child != null) {
        return toRoutePolicyStatement(child);
      }
    }

    Set_isis_metric_rp_stanzaContext ictx = ctx.set_isis_metric_rp_stanza();
    if (ictx != null) {
      return toRoutePolicyStatement(ictx);
    }

    Set_level_rp_stanzaContext lctx = ctx.set_level_rp_stanza();
    if (lctx != null) {
      return toRoutePolicyStatement(lctx);
    }

    Set_local_preference_rp_stanzaContext lpctx = ctx.set_local_preference_rp_stanza();
    if (lpctx != null) {
      return toRoutePolicyStatement(lpctx);
    }

    Set_med_rp_stanzaContext medctx = ctx.set_med_rp_stanza();
    if (medctx != null) {
      return toRoutePolicyStatement(medctx);
    }

    Set_metric_type_rp_stanzaContext mctx = ctx.set_metric_type_rp_stanza();
    if (mctx != null) {
      return toRoutePolicyStatement(mctx);
    }

    Set_next_hop_rp_stanzaContext nhctx = ctx.set_next_hop_rp_stanza();
    if (nhctx != null) {
      return toRoutePolicyStatement(nhctx);
    }

    Set_next_hop_self_rp_stanzaContext nhsctx = ctx.set_next_hop_self_rp_stanza();
    if (nhsctx != null) {
      return new RoutePolicySetNextHop(new RoutePolicyNextHopSelf(), false);
    }

    Set_origin_rp_stanzaContext octx = ctx.set_origin_rp_stanza();
    if (octx != null) {
      return toRoutePolicyStatement(octx);
    }

    Set_tag_rp_stanzaContext tctx = ctx.set_tag_rp_stanza();
    if (tctx != null) {
      return toRoutePolicyStatement(tctx);
    }

    Set_weight_rp_stanzaContext wctx = ctx.set_weight_rp_stanza();
    if (wctx != null) {
      return toRoutePolicyStatement(wctx);
    }

    return convProblem(
        RoutePolicyStatement.class,
        ctx,
        new RoutePolicyComment(
            String.format("NOP: unsupported route-policy statement: '%s'", getFullText(ctx))));
  }

  private <T, U extends T> T convProblem(
      Class<T> returnType, ParserRuleContext ctx, U defaultReturnValue) {
    warn(ctx, convErrorMessage(returnType, ctx));
    return defaultReturnValue;
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_tag_rp_stanzaContext ctx) {
    LongExpr tag = toTagLongExpr(ctx.tag);
    return new RoutePolicySetTag(tag);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_weight_rp_stanzaContext wctx) {
    IntExpr weight = toCommonIntExpr(wctx.weight);
    return new RoutePolicySetWeight(weight);
  }

  private List<RoutePolicyStatement> toRoutePolicyStatementList(List<Rp_stanzaContext> ctxts) {
    return ctxts.stream().map(this::toRoutePolicyStatement).collect(Collectors.toList());
  }

  @Nonnull
  private RouteDistinguisher toRouteDistinguisher(Route_distinguisherContext ctx) {
    long dec = toLong(ctx.DEC());
    if (ctx.IP_ADDRESS() != null) {
      checkArgument(dec <= 0xFFFFL, "Invalid route distinguisher %s", ctx.getText());
      return RouteDistinguisher.from(toIp(ctx.IP_ADDRESS()), (int) dec);
    }
    return RouteDistinguisher.from(toAsNum(ctx.bgp_asn()), dec);
  }

  @Nonnull
  private ExtendedCommunity toRouteTarget(Route_targetContext ctx) {
    long la = toLong(ctx.DEC());
    if (ctx.IP_ADDRESS() != null) {
      return ExtendedCommunity.target(toIp(ctx.IP_ADDRESS()).asLong(), la);
    }
    return ExtendedCommunity.target(toAsNum(ctx.bgp_asn()), la);
  }

  private RouteTypeExpr toRouteType(Rp_route_typeContext ctx) {
    if (ctx.INTERAREA() != null) {
      return new LiteralRouteType(RouteType.INTERAREA);
    } else if (ctx.INTERNAL() != null) {
      return new LiteralRouteType(RouteType.INTERNAL);
    } else if (ctx.LEVEL_1() != null) {
      return new LiteralRouteType(RouteType.LEVEL_1);
    } else if (ctx.LEVEL_1_2() != null) {
      return new LiteralRouteType(RouteType.INTERAREA); // not a typo
    } else if (ctx.LEVEL_2() != null) {
      return new LiteralRouteType(RouteType.LEVEL_2);
    } else if (ctx.LOCAL() != null) {
      return new LiteralRouteType(RouteType.LOCAL);
    } else if (ctx.OSPF_EXTERNAL_TYPE_1() != null) {
      return new LiteralRouteType(RouteType.OSPF_EXTERNAL_TYPE_1);
    } else if (ctx.OSPF_EXTERNAL_TYPE_2() != null) {
      return new LiteralRouteType(RouteType.OSPF_EXTERNAL_TYPE_2);
    } else if (ctx.OSPF_INTER_AREA() != null) {
      return new LiteralRouteType(RouteType.OSPF_INTER_AREA);
    } else if (ctx.OSPF_INTRA_AREA() != null) {
      return new LiteralRouteType(RouteType.OSPF_INTRA_AREA);
    } else if (ctx.OSPF_NSSA_TYPE_1() != null) {
      return new LiteralRouteType(RouteType.OSPF_NSSA_TYPE_1);
    } else if (ctx.OSPF_NSSA_TYPE_2() != null) {
      return new LiteralRouteType(RouteType.OSPF_NSSA_TYPE_2);
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarRouteType(ctx.RP_VARIABLE().getText());
    } else if (ctx.TYPE_1() != null) {
      return new LiteralRouteType(RouteType.TYPE_1);
    } else if (ctx.TYPE_2() != null) {
      return new LiteralRouteType(RouteType.TYPE_2);
    } else {
      throw convError(RouteTypeExpr.class, ctx);
    }
  }

  private SubRangeExpr toSubRangeExpr(Rp_subrangeContext ctx) {
    IntExpr first = toCommonIntExpr(ctx.first);
    IntExpr last = first;
    if (ctx.last != null) {
      last = toCommonIntExpr(ctx.first);
    }
    return new SubRangeExpr(first, last);
  }

  private LongExpr toTagLongExpr(Int_exprContext ctx) {
    if (ctx.DEC() != null && ctx.DASH() == null && ctx.PLUS() == null) {
      long val = toLong(ctx.DEC());
      return new LiteralLong(val);
    } else if (ctx.RP_VARIABLE() != null) {
      String var = ctx.RP_VARIABLE().getText();
      return new VarLong(var);
    } else {
      throw convError(LongExpr.class, ctx);
    }
  }

  private void warnObjectGroupRedefinition(ParserRuleContext name) {
    ParserRuleContext outer = firstNonNull(name.getParent(), name);
    warn(outer, "Object group defined multiple times: '" + name.getText() + "'.");
  }

  @Nullable
  private ServiceObjectGroup.ServiceProtocol toServiceProtocol(
      Service_group_protocolContext protocol) {
    if (protocol == null) {
      return null;
    }
    switch (protocol.getText()) {
      case "tcp":
        return ServiceProtocol.TCP;
      case "udp":
        return ServiceProtocol.UDP;
      case "tcp-udp":
        return ServiceProtocol.TCP_UDP;
      default:
        warn(protocol, "Unexpected service protocol type.");
        return null;
    }
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    int line = token.getLine();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    _configuration.setUnrecognized(true);

    if (token instanceof UnrecognizedLineToken) {
      UnrecognizedLineToken unrecToken = (UnrecognizedLineToken) token;
      _w.getParseWarnings()
          .add(
              new ParseWarning(
                  line, lineText, unrecToken.getParserContext(), "This syntax is unrecognized"));
    } else {
      String msg = String.format("Unrecognized Line: %d: %s", line, lineText);
      _w.redFlag(msg + " SUBSEQUENT LINES MAY NOT BE PROCESSED CORRECTLY");
    }
  }
}

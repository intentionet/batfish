package org.batfish.grammar.cisco;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toCollection;
import static org.batfish.datamodel.ConfigurationFormat.ARISTA;
import static org.batfish.datamodel.ConfigurationFormat.ARUBAOS;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_ASA;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_NX;
import static org.batfish.representation.cisco.CiscoStructureType.ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.AS_PATH_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.AS_PATH_SET;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_TEMPLATE_PEER;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_TEMPLATE_PEER_POLICY;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_TEMPLATE_PEER_SESSION;
import static org.batfish.representation.cisco.CiscoStructureType.CLASS_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.COMMUNITY_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.COMMUNITY_LIST_EXPANDED;
import static org.batfish.representation.cisco.CiscoStructureType.COMMUNITY_LIST_STANDARD;
import static org.batfish.representation.cisco.CiscoStructureType.COMMUNITY_SET;
import static org.batfish.representation.cisco.CiscoStructureType.CRYPTO_DYNAMIC_MAP_SET;
import static org.batfish.representation.cisco.CiscoStructureType.CRYPTO_MAP_SET;
import static org.batfish.representation.cisco.CiscoStructureType.DEPI_CLASS;
import static org.batfish.representation.cisco.CiscoStructureType.DEPI_TUNNEL;
import static org.batfish.representation.cisco.CiscoStructureType.DOCSIS_POLICY;
import static org.batfish.representation.cisco.CiscoStructureType.DOCSIS_POLICY_RULE;
import static org.batfish.representation.cisco.CiscoStructureType.INSPECT_CLASS_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.INSPECT_POLICY_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureType.IPSEC_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureType.IPSEC_TRANSFORM_SET;
import static org.batfish.representation.cisco.CiscoStructureType.IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.IPV4_ACCESS_LIST_EXTENDED;
import static org.batfish.representation.cisco.CiscoStructureType.IPV4_ACCESS_LIST_STANDARD;
import static org.batfish.representation.cisco.CiscoStructureType.IPV6_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.IPV6_ACCESS_LIST_EXTENDED;
import static org.batfish.representation.cisco.CiscoStructureType.IPV6_ACCESS_LIST_STANDARD;
import static org.batfish.representation.cisco.CiscoStructureType.IP_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.ISAKMP_POLICY;
import static org.batfish.representation.cisco.CiscoStructureType.ISAKMP_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureType.KEYRING;
import static org.batfish.representation.cisco.CiscoStructureType.L2TP_CLASS;
import static org.batfish.representation.cisco.CiscoStructureType.MAC_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.NAT_POOL;
import static org.batfish.representation.cisco.CiscoStructureType.NETWORK_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureType.NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.POLICY_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX6_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.PREFIX_SET;
import static org.batfish.representation.cisco.CiscoStructureType.PROTOCOL_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.ROUTE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureType.SECURITY_ZONE;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_CLASS;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ADDITIONAL_PATHS_SELECTION_ROUTE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ADVERTISE_MAP_EXIST_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_AGGREGATE_ATTRIBUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_AGGREGATE_ROUTE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_DEFAULT_ORIGINATE_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_FILTER6_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_FILTER_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_PREFIX6_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_ROUTE6_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INHERITED_PEER;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INHERITED_PEER_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INHERITED_SESSION;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_FILTER_AS_PATH_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_REMOTE_AS_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_ROUTE_POLICY_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_ROUTE_POLICY_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NETWORK6_ORIGINATION_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NETWORK_ORIGINATION_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_FILTER6_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_FILTER_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_PREFIX6_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_ROUTE6_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_CONNECTED_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_EIGRP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_ISIS_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_LISP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_OSPFV3_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_OSPF_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_RIP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_STATIC_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ROUTE_MAP_ADVERTISE;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ROUTE_MAP_ATTRIBUTE;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ROUTE_MAP_OTHER;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ROUTE_MAP_SUPPRESS;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ROUTE_MAP_UNSUPPRESS;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_TABLE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_UPDATE_SOURCE_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_VRF_AGGREGATE_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.CLASS_MAP_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.CLASS_MAP_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.CLASS_MAP_ACTIVATED_SERVICE_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureUsage.CLASS_MAP_SERVICE_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureUsage.CONTROLLER_DEPI_TUNNEL;
import static org.batfish.representation.cisco.CiscoStructureUsage.CONTROL_PLANE_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.COPS_LISTENER_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_DYNAMIC_MAP_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_DYNAMIC_MAP_ISAKMP_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_DYNAMIC_MAP_TRANSFORM_SET;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_CRYPTO_DYNAMIC_MAP_SET;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ISAKMP_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_TRANSFORM_SET;
import static org.batfish.representation.cisco.CiscoStructureUsage.DEPI_TUNNEL_DEPI_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.DEPI_TUNNEL_L2TP_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.DEPI_TUNNEL_PROTECT_TUNNEL;
import static org.batfish.representation.cisco.CiscoStructureUsage.DOCSIS_GROUP_DOCSIS_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.DOCSIS_POLICY_DOCSIS_POLICY_RULE;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_BGP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_CONNECTED_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_EIGRP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_ISIS_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_OSPF_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_RIP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_STATIC_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INSPECT_CLASS_MAP_MATCH_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INSPECT_POLICY_MAP_INSPECT_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IGMP_ACCESS_GROUP_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IGMP_STATIC_GROUP_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_INCOMING_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IP_INBAND_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IP_VERIFY_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IP_VRF_SITEMAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_OUTGOING_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_PIM_NEIGHBOR_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_POLICY_ROUTING_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_SELF_REF;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_SERVICE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_SERVICE_POLICY_CONTROL_SUBSCRIBER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_SUMMARY_ADDRESS_EIGRP_LEAK_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_ZONE_MEMBER;
import static org.batfish.representation.cisco.CiscoStructureUsage.IPSEC_PROFILE_ISAKMP_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureUsage.IPSEC_PROFILE_TRANSFORM_SET;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_DESTINATION_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_SOURCE_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_SOURCE_POOL;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISAKMP_POLICY_SELF_REF;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISAKMP_PROFILE_KEYRING;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISAKMP_PROFILE_SELF_REF;
import static org.batfish.representation.cisco.CiscoStructureUsage.LINE_ACCESS_CLASS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.LINE_ACCESS_CLASS_LIST6;
import static org.batfish.representation.cisco.CiscoStructureUsage.MANAGEMENT_SSH_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.MANAGEMENT_TELNET_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.MSDP_PEER_SA_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.NETWORK_OBJECT_GROUP_GROUP_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.NETWORK_OBJECT_GROUP_NETWORK_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.NTP_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_AREA_FILTER_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_DEFAULT_ORIGINATE_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_REDISTRIBUTE_BGP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_REDISTRIBUTE_CONNECTED_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_REDISTRIBUTE_EIGRP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_REDISTRIBUTE_STATIC_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.PIM_ACCEPT_REGISTER_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.PIM_ACCEPT_REGISTER_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.PIM_ACCEPT_RP_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.PIM_RP_ADDRESS_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.PIM_RP_ANNOUNCE_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.PIM_RP_CANDIDATE_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.PIM_SEND_RP_ANNOUNCE_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.PIM_SPT_THRESHOLD_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.POLICY_MAP_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.POLICY_MAP_CLASS_SERVICE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.POLICY_MAP_EVENT_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.POLICY_MAP_EVENT_CLASS_ACTIVATE;
import static org.batfish.representation.cisco.CiscoStructureUsage.QOS_ENFORCE_RULE_SERVICE_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.RIP_DISTRIBUTE_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTER_ISIS_DISTRIBUTE_LIST_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTER_VRRP_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_ADD_COMMUNITY;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_DELETE_COMMUNITY;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_AS_PATH_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_COMMUNITY_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV4_PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_SET_COMMUNITY;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_POLICY_AS_PATH_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_POLICY_COMMUNITY_MATCHES_ANY;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_POLICY_COMMUNITY_MATCHES_EVERY;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_POLICY_DELETE_COMMUNITY_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_POLICY_PREFIX_SET;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_POLICY_SET_COMMUNITY;
import static org.batfish.representation.cisco.CiscoStructureUsage.SERVICE_POLICY_GLOBAL;
import static org.batfish.representation.cisco.CiscoStructureUsage.SERVICE_POLICY_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.SERVICE_POLICY_INTERFACE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL4;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL6;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_FILE_TRANSFER_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_TFTP_SERVER_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.SSH_IPV4_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.SSH_IPV6_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.TUNNEL_PROTECTION_IPSEC_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureUsage.TUNNEL_SOURCE;
import static org.batfish.representation.cisco.CiscoStructureUsage.WCCP_GROUP_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.WCCP_REDIRECT_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.WCCP_SERVICE_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ZONE_PAIR_DESTINATION_ZONE;
import static org.batfish.representation.cisco.CiscoStructureUsage.ZONE_PAIR_INSPECT_SERVICE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.ZONE_PAIR_SOURCE_ZONE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import javax.annotation.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.batfish.common.BatfishException;
import org.batfish.common.RedFlagBatfishException;
import org.batfish.common.Warnings;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AaaAuthenticationLoginList;
import org.batfish.datamodel.AuthenticationMethod;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.DscpType;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.IcmpCode;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IkeAuthenticationMethod;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.InterfaceAddress;
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
import org.batfish.datamodel.State;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.eigrp.EigrpProcessMode;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisMetricType;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.routing_policy.expr.AsExpr;
import org.batfish.datamodel.routing_policy.expr.AsPathSetElem;
import org.batfish.datamodel.routing_policy.expr.AsPathSetExpr;
import org.batfish.datamodel.routing_policy.expr.AutoAs;
import org.batfish.datamodel.routing_policy.expr.CommunitySetElem;
import org.batfish.datamodel.routing_policy.expr.CommunitySetElemHalfExpr;
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
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySetElemHalf;
import org.batfish.datamodel.routing_policy.expr.LiteralInt;
import org.batfish.datamodel.routing_policy.expr.LiteralIsisLevel;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.LiteralRouteType;
import org.batfish.datamodel.routing_policy.expr.LongExpr;
import org.batfish.datamodel.routing_policy.expr.NamedAsPathSet;
import org.batfish.datamodel.routing_policy.expr.OriginExpr;
import org.batfish.datamodel.routing_policy.expr.RangeCommunitySetElemHalf;
import org.batfish.datamodel.routing_policy.expr.RegexAsPathSetElem;
import org.batfish.datamodel.routing_policy.expr.RouteType;
import org.batfish.datamodel.routing_policy.expr.RouteTypeExpr;
import org.batfish.datamodel.routing_policy.expr.SubRangeExpr;
import org.batfish.datamodel.routing_policy.expr.VarAs;
import org.batfish.datamodel.routing_policy.expr.VarAsPathSet;
import org.batfish.datamodel.routing_policy.expr.VarCommunitySetElemHalf;
import org.batfish.datamodel.routing_policy.expr.VarInt;
import org.batfish.datamodel.routing_policy.expr.VarIsisLevel;
import org.batfish.datamodel.routing_policy.expr.VarLong;
import org.batfish.datamodel.routing_policy.expr.VarOrigin;
import org.batfish.datamodel.routing_policy.expr.VarRouteType;
import org.batfish.datamodel.vendor_family.cisco.Aaa;
import org.batfish.datamodel.vendor_family.cisco.AaaAccounting;
import org.batfish.datamodel.vendor_family.cisco.AaaAccountingCommands;
import org.batfish.datamodel.vendor_family.cisco.AaaAccountingDefault;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthentication;
import org.batfish.datamodel.vendor_family.cisco.AaaAuthenticationLogin;
import org.batfish.datamodel.vendor_family.cisco.Buffered;
import org.batfish.datamodel.vendor_family.cisco.Cable;
import org.batfish.datamodel.vendor_family.cisco.DepiClass;
import org.batfish.datamodel.vendor_family.cisco.DepiTunnel;
import org.batfish.datamodel.vendor_family.cisco.DocsisPolicy;
import org.batfish.datamodel.vendor_family.cisco.DocsisPolicyRule;
import org.batfish.datamodel.vendor_family.cisco.L2tpClass;
import org.batfish.datamodel.vendor_family.cisco.Logging;
import org.batfish.datamodel.vendor_family.cisco.LoggingHost;
import org.batfish.datamodel.vendor_family.cisco.LoggingType;
import org.batfish.datamodel.vendor_family.cisco.Ntp;
import org.batfish.datamodel.vendor_family.cisco.NtpServer;
import org.batfish.datamodel.vendor_family.cisco.Service;
import org.batfish.datamodel.vendor_family.cisco.ServiceClass;
import org.batfish.datamodel.vendor_family.cisco.Sntp;
import org.batfish.datamodel.vendor_family.cisco.SntpServer;
import org.batfish.datamodel.vendor_family.cisco.SshSettings;
import org.batfish.datamodel.vendor_family.cisco.User;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accountingContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accounting_commands_lineContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accounting_defaultContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accounting_default_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accounting_default_localContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_authenticationContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_authentication_asaContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_authentication_list_methodContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_authentication_loginContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_authentication_login_listContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_authentication_login_privilege_modeContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_new_modelContext;
import org.batfish.grammar.cisco.CiscoParser.Access_list_actionContext;
import org.batfish.grammar.cisco.CiscoParser.Access_list_ip6_rangeContext;
import org.batfish.grammar.cisco.CiscoParser.Access_list_ip_rangeContext;
import org.batfish.grammar.cisco.CiscoParser.Activate_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Additional_paths_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Additional_paths_selection_xr_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Address_family_headerContext;
import org.batfish.grammar.cisco.CiscoParser.Address_family_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Advertise_map_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Af_group_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Aggregate_address_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Allowas_in_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Always_compare_med_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Apply_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.As_exprContext;
import org.batfish.grammar.cisco.CiscoParser.As_path_multipath_relax_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.As_path_set_elemContext;
import org.batfish.grammar.cisco.CiscoParser.As_path_set_inlineContext;
import org.batfish.grammar.cisco.CiscoParser.As_path_set_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Auto_summary_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Banner_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_advertise_inactive_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_asnContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_confederation_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_listen_range_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_and_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_apply_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_as_path_in_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_as_path_is_local_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_as_path_neighbor_is_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_as_path_originates_from_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_as_path_passes_through_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_community_matches_any_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_community_matches_every_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_destination_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_local_preference_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_med_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_next_hop_in_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_not_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_rib_has_route_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_route_type_is_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_simple_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Boolean_tag_is_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Cadant_stdacl_nameContext;
import org.batfish.grammar.cisco.CiscoParser.Cd_match_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Cd_set_isakmp_profileContext;
import org.batfish.grammar.cisco.CiscoParser.Cd_set_peerContext;
import org.batfish.grammar.cisco.CiscoParser.Cd_set_pfsContext;
import org.batfish.grammar.cisco.CiscoParser.Cd_set_transform_setContext;
import org.batfish.grammar.cisco.CiscoParser.Cip_profileContext;
import org.batfish.grammar.cisco.CiscoParser.Cip_transform_setContext;
import org.batfish.grammar.cisco.CiscoParser.Cipprf_set_isakmp_profileContext;
import org.batfish.grammar.cisco.CiscoParser.Cipprf_set_pfsContext;
import org.batfish.grammar.cisco.CiscoParser.Cipprf_set_transform_setContext;
import org.batfish.grammar.cisco.CiscoParser.Cipt_modeContext;
import org.batfish.grammar.cisco.CiscoParser.Cis_policyContext;
import org.batfish.grammar.cisco.CiscoParser.Cis_profileContext;
import org.batfish.grammar.cisco.CiscoParser.Cisco_configurationContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_authenticationContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_encrContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_encryptionContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_hashContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_lifetimeContext;
import org.batfish.grammar.cisco.CiscoParser.Cisprf_keyringContext;
import org.batfish.grammar.cisco.CiscoParser.Cisprf_local_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Cisprf_matchContext;
import org.batfish.grammar.cisco.CiscoParser.Cisprf_self_identityContext;
import org.batfish.grammar.cisco.CiscoParser.Ckr_local_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Ckr_pskContext;
import org.batfish.grammar.cisco.CiscoParser.Clb_docsis_policyContext;
import org.batfish.grammar.cisco.CiscoParser.Clb_ruleContext;
import org.batfish.grammar.cisco.CiscoParser.Clbdg_docsis_policyContext;
import org.batfish.grammar.cisco.CiscoParser.Cluster_id_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Cm_ios_inspectContext;
import org.batfish.grammar.cisco.CiscoParser.Cm_iosi_matchContext;
import org.batfish.grammar.cisco.CiscoParser.Cm_matchContext;
import org.batfish.grammar.cisco.CiscoParser.Cmm_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Cmm_access_listContext;
import org.batfish.grammar.cisco.CiscoParser.Cmm_activated_service_templateContext;
import org.batfish.grammar.cisco.CiscoParser.Cmm_service_templateContext;
import org.batfish.grammar.cisco.CiscoParser.Cntlr_rf_channelContext;
import org.batfish.grammar.cisco.CiscoParser.Cntlrrfc_depi_tunnelContext;
import org.batfish.grammar.cisco.CiscoParser.CommunityContext;
import org.batfish.grammar.cisco.CiscoParser.Community_set_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Compare_routerid_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Continue_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Copsl_access_listContext;
import org.batfish.grammar.cisco.CiscoParser.Cp_ip_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Cqer_service_classContext;
import org.batfish.grammar.cisco.CiscoParser.Crypto_dynamic_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Crypto_keyringContext;
import org.batfish.grammar.cisco.CiscoParser.Crypto_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Crypto_map_t_ii_match_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Crypto_map_t_ii_set_isakmp_profileContext;
import org.batfish.grammar.cisco.CiscoParser.Crypto_map_t_ii_set_peerContext;
import org.batfish.grammar.cisco.CiscoParser.Crypto_map_t_ii_set_pfsContext;
import org.batfish.grammar.cisco.CiscoParser.Crypto_map_t_ii_set_transform_setContext;
import org.batfish.grammar.cisco.CiscoParser.Crypto_map_t_ipsec_isakmpContext;
import org.batfish.grammar.cisco.CiscoParser.Cs_classContext;
import org.batfish.grammar.cisco.CiscoParser.Csc_nameContext;
import org.batfish.grammar.cisco.CiscoParser.Default_information_originate_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Default_metric_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Default_originate_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Default_shutdown_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Delete_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Description_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Dh_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Disable_peer_as_check_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Disposition_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Distribute_list_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Distribute_list_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Domain_lookupContext;
import org.batfish.grammar.cisco.CiscoParser.Domain_nameContext;
import org.batfish.grammar.cisco.CiscoParser.Domain_name_serverContext;
import org.batfish.grammar.cisco.CiscoParser.Dscp_typeContext;
import org.batfish.grammar.cisco.CiscoParser.Dt_depi_classContext;
import org.batfish.grammar.cisco.CiscoParser.Dt_l2tp_classContext;
import org.batfish.grammar.cisco.CiscoParser.Dt_protect_tunnelContext;
import org.batfish.grammar.cisco.CiscoParser.Ebgp_multihop_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Else_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Elseif_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Empty_neighbor_block_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Enable_secretContext;
import org.batfish.grammar.cisco.CiscoParser.Eos_bandwidth_specifierContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_access_list_additional_featureContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_access_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_access_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_ipv6_access_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_ipv6_access_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Failover_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Failover_linkContext;
import org.batfish.grammar.cisco.CiscoParser.Filter_list_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Flan_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Flan_unitContext;
import org.batfish.grammar.cisco.CiscoParser.Hash_commentContext;
import org.batfish.grammar.cisco.CiscoParser.Icmp_object_typeContext;
import org.batfish.grammar.cisco.CiscoParser.If_autostateContext;
import org.batfish.grammar.cisco.CiscoParser.If_bandwidthContext;
import org.batfish.grammar.cisco.CiscoParser.If_channel_groupContext;
import org.batfish.grammar.cisco.CiscoParser.If_crypto_mapContext;
import org.batfish.grammar.cisco.CiscoParser.If_delayContext;
import org.batfish.grammar.cisco.CiscoParser.If_descriptionContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_addressContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_address_secondaryContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_helper_addressContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_igmpContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_inband_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_nat_destinationContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_nat_sourceContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_areaContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_costContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_dead_intervalContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_dead_interval_minimalContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_networkContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_passive_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_shutdownContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_pim_neighbor_filterContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_policyContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_proxy_arpContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_router_isisContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_router_ospf_areaContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_summary_addressContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_verifyContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_vrf_forwardingContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_vrf_sitemapContext;
import org.batfish.grammar.cisco.CiscoParser.If_isis_metricContext;
import org.batfish.grammar.cisco.CiscoParser.If_mtuContext;
import org.batfish.grammar.cisco.CiscoParser.If_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.If_service_policyContext;
import org.batfish.grammar.cisco.CiscoParser.If_service_policy_control_subscriberContext;
import org.batfish.grammar.cisco.CiscoParser.If_shutdownContext;
import org.batfish.grammar.cisco.CiscoParser.If_spanning_treeContext;
import org.batfish.grammar.cisco.CiscoParser.If_speed_eosContext;
import org.batfish.grammar.cisco.CiscoParser.If_speed_iosContext;
import org.batfish.grammar.cisco.CiscoParser.If_st_portfastContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchportContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_accessContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_modeContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_trunk_allowedContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_trunk_encapsulationContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_trunk_nativeContext;
import org.batfish.grammar.cisco.CiscoParser.If_vrf_memberContext;
import org.batfish.grammar.cisco.CiscoParser.If_vrrpContext;
import org.batfish.grammar.cisco.CiscoParser.If_zone_memberContext;
import org.batfish.grammar.cisco.CiscoParser.Ifdhcpr_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Ifdhcpr_clientContext;
import org.batfish.grammar.cisco.CiscoParser.Ifigmp_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Ifigmpsg_aclContext;
import org.batfish.grammar.cisco.CiscoParser.Iftunnel_destinationContext;
import org.batfish.grammar.cisco.CiscoParser.Iftunnel_modeContext;
import org.batfish.grammar.cisco.CiscoParser.Iftunnel_protectionContext;
import org.batfish.grammar.cisco.CiscoParser.Iftunnel_sourceContext;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_authenticationContext;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_ipContext;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_preemptContext;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_priorityContext;
import org.batfish.grammar.cisco.CiscoParser.Ike_encryptionContext;
import org.batfish.grammar.cisco.CiscoParser.Ike_encryption_arubaContext;
import org.batfish.grammar.cisco.CiscoParser.Inherit_peer_policy_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Inherit_peer_session_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Inspect_protocolContext;
import org.batfish.grammar.cisco.CiscoParser.Int_compContext;
import org.batfish.grammar.cisco.CiscoParser.Int_exprContext;
import org.batfish.grammar.cisco.CiscoParser.Interface_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Interface_nameContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_as_path_access_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_as_path_access_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_community_list_expanded_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_community_list_expanded_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_community_list_standard_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_community_list_standard_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_dhcp_relay_serverContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_domain_lookupContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_domain_nameContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_hostnameContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_nat_poolContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_nat_pool_rangeContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_prefix_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_prefix_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_route_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_route_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_ssh_versionContext;
import org.batfish.grammar.cisco.CiscoParser.Ipsec_authenticationContext;
import org.batfish.grammar.cisco.CiscoParser.Ipsec_encryptionContext;
import org.batfish.grammar.cisco.CiscoParser.Ipsec_encryption_arubaContext;
import org.batfish.grammar.cisco.CiscoParser.Ipv6_prefix_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ipv6_prefix_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Is_type_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Isis_levelContext;
import org.batfish.grammar.cisco.CiscoParser.Isis_level_exprContext;
import org.batfish.grammar.cisco.CiscoParser.L_access_classContext;
import org.batfish.grammar.cisco.CiscoParser.L_exec_timeoutContext;
import org.batfish.grammar.cisco.CiscoParser.L_login_authenticationContext;
import org.batfish.grammar.cisco.CiscoParser.L_transportContext;
import org.batfish.grammar.cisco.CiscoParser.Local_as_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Logging_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Logging_bufferedContext;
import org.batfish.grammar.cisco.CiscoParser.Logging_consoleContext;
import org.batfish.grammar.cisco.CiscoParser.Logging_hostContext;
import org.batfish.grammar.cisco.CiscoParser.Logging_onContext;
import org.batfish.grammar.cisco.CiscoParser.Logging_serverContext;
import org.batfish.grammar.cisco.CiscoParser.Logging_severityContext;
import org.batfish.grammar.cisco.CiscoParser.Logging_source_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Logging_trapContext;
import org.batfish.grammar.cisco.CiscoParser.Management_ssh_ip_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Management_telnet_ip_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Match_as_path_access_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_community_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_ip_access_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_ip_prefix_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_ipv6_access_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_ipv6_prefix_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_semanticsContext;
import org.batfish.grammar.cisco.CiscoParser.Match_source_protocol_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_tag_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Maximum_paths_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Maximum_peers_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Neighbor_block_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Neighbor_block_inheritContext;
import org.batfish.grammar.cisco.CiscoParser.Neighbor_block_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Neighbor_flat_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Neighbor_group_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Net_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Network6_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Network_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Next_hop_self_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.No_ip_prefix_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_neighbor_activate_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_neighbor_shutdown_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_redistribute_connected_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_route_map_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_shutdown_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ntp_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Ntp_serverContext;
import org.batfish.grammar.cisco.CiscoParser.Ntp_source_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Null_as_path_regexContext;
import org.batfish.grammar.cisco.CiscoParser.O_networkContext;
import org.batfish.grammar.cisco.CiscoParser.O_serviceContext;
import org.batfish.grammar.cisco.CiscoParser.Og_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Og_protocolContext;
import org.batfish.grammar.cisco.CiscoParser.Og_serviceContext;
import org.batfish.grammar.cisco.CiscoParser.Ogn_group_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogn_host_ipContext;
import org.batfish.grammar.cisco.CiscoParser.Ogn_ip_with_maskContext;
import org.batfish.grammar.cisco.CiscoParser.Ogn_network_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogp_protocol_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_icmpContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_service_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_tcpContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_udpContext;
import org.batfish.grammar.cisco.CiscoParser.On_descriptionContext;
import org.batfish.grammar.cisco.CiscoParser.On_fqdnContext;
import org.batfish.grammar.cisco.CiscoParser.On_hostContext;
import org.batfish.grammar.cisco.CiscoParser.On_rangeContext;
import org.batfish.grammar.cisco.CiscoParser.On_subnetContext;
import org.batfish.grammar.cisco.CiscoParser.Origin_exprContext;
import org.batfish.grammar.cisco.CiscoParser.Origin_expr_literalContext;
import org.batfish.grammar.cisco.CiscoParser.Os_descriptionContext;
import org.batfish.grammar.cisco.CiscoParser.Passive_iis_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Passive_interface_default_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Passive_interface_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Peer_group_assignment_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Peer_group_creation_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Peer_sa_filterContext;
import org.batfish.grammar.cisco.CiscoParser.Pi_iosicd_dropContext;
import org.batfish.grammar.cisco.CiscoParser.Pi_iosicd_passContext;
import org.batfish.grammar.cisco.CiscoParser.Pim_accept_registerContext;
import org.batfish.grammar.cisco.CiscoParser.Pim_accept_rpContext;
import org.batfish.grammar.cisco.CiscoParser.Pim_rp_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Pim_rp_announce_filterContext;
import org.batfish.grammar.cisco.CiscoParser.Pim_rp_candidateContext;
import org.batfish.grammar.cisco.CiscoParser.Pim_send_rp_announceContext;
import org.batfish.grammar.cisco.CiscoParser.Pim_spt_thresholdContext;
import org.batfish.grammar.cisco.CiscoParser.Pm_classContext;
import org.batfish.grammar.cisco.CiscoParser.Pm_event_classContext;
import org.batfish.grammar.cisco.CiscoParser.Pm_ios_inspectContext;
import org.batfish.grammar.cisco.CiscoParser.Pm_iosi_class_type_inspectContext;
import org.batfish.grammar.cisco.CiscoParser.Pm_iosict_dropContext;
import org.batfish.grammar.cisco.CiscoParser.Pm_iosict_inspectContext;
import org.batfish.grammar.cisco.CiscoParser.Pm_iosict_passContext;
import org.batfish.grammar.cisco.CiscoParser.Pmc_service_policyContext;
import org.batfish.grammar.cisco.CiscoParser.PortContext;
import org.batfish.grammar.cisco.CiscoParser.Port_specifierContext;
import org.batfish.grammar.cisco.CiscoParser.Prefix_list_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Prefix_set_elemContext;
import org.batfish.grammar.cisco.CiscoParser.Prefix_set_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Prepend_as_path_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.ProtocolContext;
import org.batfish.grammar.cisco.CiscoParser.RangeContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_aa_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_additional_pathsContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_aggregate_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_client_to_clientContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_dampeningContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_default_informationContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_default_metricContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_distanceContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_inject_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_maximum_pathsContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_nexthop_route_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_redistribute_directContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_redistribute_eigrpContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_redistribute_isisContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_redistribute_lispContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_redistribute_ospfContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_redistribute_ospfv3Context;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_redistribute_ripContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_redistribute_staticContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_suppress_inactiveContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_af_table_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_bestpathContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_cluster_idContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_confederation_identifierContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_confederation_peersContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_enforce_first_asContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_log_neighbor_changesContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_maxas_limitContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_advertise_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_allowas_inContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_as_overrideContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_default_originateContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_disable_peer_as_checkContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_filter_listContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_inheritContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_next_hop_selfContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_next_hop_third_partyContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_prefix_listContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_route_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_route_reflector_clientContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_send_communityContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_suppress_inactiveContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_af_unsuppress_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_descriptionContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_ebgp_multihopContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_inheritContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_local_asContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_no_shutdownContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_remote_asContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_remove_private_asContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_shutdownContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_n_update_sourceContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_neighborContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_no_enforce_first_asContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_router_idContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_template_peerContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_template_peer_policyContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_template_peer_sessionContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_v_local_asContext;
import org.batfish.grammar.cisco.CiscoParser.Rbnx_vrfContext;
import org.batfish.grammar.cisco.CiscoParser.Re_classicContext;
import org.batfish.grammar.cisco.CiscoParser.Re_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Re_passive_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Re_passive_interface_defaultContext;
import org.batfish.grammar.cisco.CiscoParser.Re_redistribute_bgpContext;
import org.batfish.grammar.cisco.CiscoParser.Re_redistribute_connectedContext;
import org.batfish.grammar.cisco.CiscoParser.Re_redistribute_eigrpContext;
import org.batfish.grammar.cisco.CiscoParser.Re_redistribute_isisContext;
import org.batfish.grammar.cisco.CiscoParser.Re_redistribute_ospfContext;
import org.batfish.grammar.cisco.CiscoParser.Re_redistribute_ripContext;
import org.batfish.grammar.cisco.CiscoParser.Re_redistribute_staticContext;
import org.batfish.grammar.cisco.CiscoParser.Reaf_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Reafi_passive_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Rec_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_aggregate_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_connected_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_connected_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_ospf_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_ospfv3_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_rip_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_static_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_static_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Remote_as_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Remove_private_as_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ren_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_areaContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_area_filterlistContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_area_nssaContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_area_stubContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_auto_costContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_default_informationContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_default_metricContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_max_metricContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_maximum_pathsContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_passive_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_passive_interface_defaultContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_bgpContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_connectedContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_eigrpContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_ripContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_staticContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_rfc1583_compatibilityContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_router_idContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_vrfContext;
import org.batfish.grammar.cisco.CiscoParser.Roa_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Roa_rangeContext;
import org.batfish.grammar.cisco.CiscoParser.Roi_costContext;
import org.batfish.grammar.cisco.CiscoParser.Roi_passiveContext;
import org.batfish.grammar.cisco.CiscoParser.Route_map_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Route_map_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Route_policy_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Route_policy_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Route_reflector_client_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Router_bgp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Router_id_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Router_isis_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_community_setContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_community_set_elemContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_community_set_elem_halfContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_isis_metric_typeContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_metric_typeContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_ospf_metric_typeContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_prefix_setContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_route_typeContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Rp_subrangeContext;
import org.batfish.grammar.cisco.CiscoParser.Rr_distribute_listContext;
import org.batfish.grammar.cisco.CiscoParser.Rr_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Rr_passive_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Rr_passive_interface_defaultContext;
import org.batfish.grammar.cisco.CiscoParser.Rs_routeContext;
import org.batfish.grammar.cisco.CiscoParser.Rs_vrfContext;
import org.batfish.grammar.cisco.CiscoParser.S_aaaContext;
import org.batfish.grammar.cisco.CiscoParser.S_access_lineContext;
import org.batfish.grammar.cisco.CiscoParser.S_cableContext;
import org.batfish.grammar.cisco.CiscoParser.S_class_mapContext;
import org.batfish.grammar.cisco.CiscoParser.S_depi_classContext;
import org.batfish.grammar.cisco.CiscoParser.S_depi_tunnelContext;
import org.batfish.grammar.cisco.CiscoParser.S_domain_nameContext;
import org.batfish.grammar.cisco.CiscoParser.S_featureContext;
import org.batfish.grammar.cisco.CiscoParser.S_hostnameContext;
import org.batfish.grammar.cisco.CiscoParser.S_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.S_ip_default_gatewayContext;
import org.batfish.grammar.cisco.CiscoParser.S_ip_dhcpContext;
import org.batfish.grammar.cisco.CiscoParser.S_ip_domainContext;
import org.batfish.grammar.cisco.CiscoParser.S_ip_domain_nameContext;
import org.batfish.grammar.cisco.CiscoParser.S_ip_name_serverContext;
import org.batfish.grammar.cisco.CiscoParser.S_ip_pimContext;
import org.batfish.grammar.cisco.CiscoParser.S_ip_source_routeContext;
import org.batfish.grammar.cisco.CiscoParser.S_ip_sshContext;
import org.batfish.grammar.cisco.CiscoParser.S_ip_tacacs_source_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.S_l2tp_classContext;
import org.batfish.grammar.cisco.CiscoParser.S_lineContext;
import org.batfish.grammar.cisco.CiscoParser.S_loggingContext;
import org.batfish.grammar.cisco.CiscoParser.S_mac_access_listContext;
import org.batfish.grammar.cisco.CiscoParser.S_mac_access_list_extendedContext;
import org.batfish.grammar.cisco.CiscoParser.S_no_access_list_extendedContext;
import org.batfish.grammar.cisco.CiscoParser.S_no_access_list_standardContext;
import org.batfish.grammar.cisco.CiscoParser.S_ntpContext;
import org.batfish.grammar.cisco.CiscoParser.S_policy_mapContext;
import org.batfish.grammar.cisco.CiscoParser.S_router_ospfContext;
import org.batfish.grammar.cisco.CiscoParser.S_router_ripContext;
import org.batfish.grammar.cisco.CiscoParser.S_serviceContext;
import org.batfish.grammar.cisco.CiscoParser.S_service_policy_globalContext;
import org.batfish.grammar.cisco.CiscoParser.S_service_policy_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.S_service_templateContext;
import org.batfish.grammar.cisco.CiscoParser.S_snmp_serverContext;
import org.batfish.grammar.cisco.CiscoParser.S_sntpContext;
import org.batfish.grammar.cisco.CiscoParser.S_spanning_treeContext;
import org.batfish.grammar.cisco.CiscoParser.S_switchportContext;
import org.batfish.grammar.cisco.CiscoParser.S_tacacs_serverContext;
import org.batfish.grammar.cisco.CiscoParser.S_usernameContext;
import org.batfish.grammar.cisco.CiscoParser.S_vrf_contextContext;
import org.batfish.grammar.cisco.CiscoParser.S_vrf_definitionContext;
import org.batfish.grammar.cisco.CiscoParser.S_zoneContext;
import org.batfish.grammar.cisco.CiscoParser.S_zone_pairContext;
import org.batfish.grammar.cisco.CiscoParser.Sd_switchport_blankContext;
import org.batfish.grammar.cisco.CiscoParser.Sd_switchport_shutdownContext;
import org.batfish.grammar.cisco.CiscoParser.Send_community_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Service_specifier_icmpContext;
import org.batfish.grammar.cisco.CiscoParser.Service_specifier_protocolContext;
import org.batfish.grammar.cisco.CiscoParser.Service_specifier_tcp_udpContext;
import org.batfish.grammar.cisco.CiscoParser.Session_group_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_as_path_prepend_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_comm_list_delete_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_community_additive_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_community_list_additive_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_community_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_community_none_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_community_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_community_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_isis_metric_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_level_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_local_preference_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_local_preference_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_med_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_metric_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_metric_type_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_metric_type_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_next_hop_peer_address_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_next_hop_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_next_hop_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_next_hop_self_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_origin_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_origin_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_tag_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_weight_rp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Shutdown_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Sntp_serverContext;
import org.batfish.grammar.cisco.CiscoParser.Spanning_tree_portfastContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_communityContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_enable_trapsContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_file_transferContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_hostContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_source_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_tftp_server_listContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_trap_sourceContext;
import org.batfish.grammar.cisco.CiscoParser.Ssc_access_controlContext;
import org.batfish.grammar.cisco.CiscoParser.Ssc_use_ipv4_aclContext;
import org.batfish.grammar.cisco.CiscoParser.Ssh_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Ssh_serverContext;
import org.batfish.grammar.cisco.CiscoParser.Standard_access_list_additional_featureContext;
import org.batfish.grammar.cisco.CiscoParser.Standard_access_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Standard_access_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Standard_ipv6_access_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Standard_ipv6_access_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.SubrangeContext;
import org.batfish.grammar.cisco.CiscoParser.Summary_address_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Suppressed_iis_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Switching_mode_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Switchport_trunk_encapsulationContext;
import org.batfish.grammar.cisco.CiscoParser.T_serverContext;
import org.batfish.grammar.cisco.CiscoParser.T_source_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Template_peer_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Template_peer_policy_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Template_peer_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Template_peer_session_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ts_hostContext;
import org.batfish.grammar.cisco.CiscoParser.U_passwordContext;
import org.batfish.grammar.cisco.CiscoParser.U_roleContext;
import org.batfish.grammar.cisco.CiscoParser.Unsuppress_map_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Update_source_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Use_neighbor_group_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Use_session_group_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.VariableContext;
import org.batfish.grammar.cisco.CiscoParser.Variable_access_listContext;
import org.batfish.grammar.cisco.CiscoParser.Viaf_vrrpContext;
import org.batfish.grammar.cisco.CiscoParser.Viafv_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Viafv_preemptContext;
import org.batfish.grammar.cisco.CiscoParser.Viafv_priorityContext;
import org.batfish.grammar.cisco.CiscoParser.Vrf_block_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfc_ip_routeContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfd_descriptionContext;
import org.batfish.grammar.cisco.CiscoParser.Vrrp_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Wccp_idContext;
import org.batfish.grammar.cisco.CiscoParser.Zp_service_policy_inspectContext;
import org.batfish.representation.cisco.AccessListAddressSpecifier;
import org.batfish.representation.cisco.AccessListServiceSpecifier;
import org.batfish.representation.cisco.AsPathSet;
import org.batfish.representation.cisco.BgpAggregateIpv4Network;
import org.batfish.representation.cisco.BgpAggregateIpv6Network;
import org.batfish.representation.cisco.BgpNetwork;
import org.batfish.representation.cisco.BgpNetwork6;
import org.batfish.representation.cisco.BgpPeerGroup;
import org.batfish.representation.cisco.BgpProcess;
import org.batfish.representation.cisco.BgpRedistributionPolicy;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.batfish.representation.cisco.CiscoSourceNat;
import org.batfish.representation.cisco.CiscoStructureType;
import org.batfish.representation.cisco.CiscoStructureUsage;
import org.batfish.representation.cisco.CryptoMapEntry;
import org.batfish.representation.cisco.CryptoMapSet;
import org.batfish.representation.cisco.DynamicIpBgpPeerGroup;
import org.batfish.representation.cisco.DynamicIpv6BgpPeerGroup;
import org.batfish.representation.cisco.EigrpProcess;
import org.batfish.representation.cisco.ExpandedCommunityList;
import org.batfish.representation.cisco.ExpandedCommunityListLine;
import org.batfish.representation.cisco.ExtendedAccessList;
import org.batfish.representation.cisco.ExtendedAccessListLine;
import org.batfish.representation.cisco.ExtendedIpv6AccessList;
import org.batfish.representation.cisco.ExtendedIpv6AccessListLine;
import org.batfish.representation.cisco.IcmpServiceObjectGroupLine;
import org.batfish.representation.cisco.InspectClassMap;
import org.batfish.representation.cisco.InspectClassMapMatch;
import org.batfish.representation.cisco.InspectClassMapMatchAccessGroup;
import org.batfish.representation.cisco.InspectClassMapMatchProtocol;
import org.batfish.representation.cisco.InspectClassMapProtocol;
import org.batfish.representation.cisco.InspectPolicyMap;
import org.batfish.representation.cisco.InspectPolicyMapInspectClass;
import org.batfish.representation.cisco.Interface;
import org.batfish.representation.cisco.IpAsPathAccessList;
import org.batfish.representation.cisco.IpAsPathAccessListLine;
import org.batfish.representation.cisco.IpBgpPeerGroup;
import org.batfish.representation.cisco.IpsecProfile;
import org.batfish.representation.cisco.IpsecTransformSet;
import org.batfish.representation.cisco.Ipv6BgpPeerGroup;
import org.batfish.representation.cisco.IsakmpPolicy;
import org.batfish.representation.cisco.IsakmpProfile;
import org.batfish.representation.cisco.IsisProcess;
import org.batfish.representation.cisco.IsisRedistributionPolicy;
import org.batfish.representation.cisco.Keyring;
import org.batfish.representation.cisco.MacAccessList;
import org.batfish.representation.cisco.MasterBgpPeerGroup;
import org.batfish.representation.cisco.MatchSemantics;
import org.batfish.representation.cisco.NamedBgpPeerGroup;
import org.batfish.representation.cisco.NatPool;
import org.batfish.representation.cisco.NetworkObject;
import org.batfish.representation.cisco.NetworkObjectGroup;
import org.batfish.representation.cisco.NetworkObjectGroupAddressSpecifier;
import org.batfish.representation.cisco.NssaSettings;
import org.batfish.representation.cisco.OspfNetwork;
import org.batfish.representation.cisco.OspfProcess;
import org.batfish.representation.cisco.OspfRedistributionPolicy;
import org.batfish.representation.cisco.OspfWildcardNetwork;
import org.batfish.representation.cisco.PolicyMapClassAction;
import org.batfish.representation.cisco.Prefix6List;
import org.batfish.representation.cisco.Prefix6ListLine;
import org.batfish.representation.cisco.PrefixList;
import org.batfish.representation.cisco.PrefixListLine;
import org.batfish.representation.cisco.ProtocolObjectGroup;
import org.batfish.representation.cisco.ProtocolObjectGroupProtocolLine;
import org.batfish.representation.cisco.ProtocolOrServiceObjectGroupServiceSpecifier;
import org.batfish.representation.cisco.RipProcess;
import org.batfish.representation.cisco.RouteMap;
import org.batfish.representation.cisco.RouteMapClause;
import org.batfish.representation.cisco.RouteMapContinue;
import org.batfish.representation.cisco.RouteMapMatchAsPathAccessListLine;
import org.batfish.representation.cisco.RouteMapMatchCommunityListLine;
import org.batfish.representation.cisco.RouteMapMatchIpAccessListLine;
import org.batfish.representation.cisco.RouteMapMatchIpPrefixListLine;
import org.batfish.representation.cisco.RouteMapMatchIpv6AccessListLine;
import org.batfish.representation.cisco.RouteMapMatchIpv6PrefixListLine;
import org.batfish.representation.cisco.RouteMapMatchTagLine;
import org.batfish.representation.cisco.RouteMapSetAdditiveCommunityLine;
import org.batfish.representation.cisco.RouteMapSetAdditiveCommunityListLine;
import org.batfish.representation.cisco.RouteMapSetAsPathPrependLine;
import org.batfish.representation.cisco.RouteMapSetCommunityLine;
import org.batfish.representation.cisco.RouteMapSetCommunityListLine;
import org.batfish.representation.cisco.RouteMapSetCommunityNoneLine;
import org.batfish.representation.cisco.RouteMapSetDeleteCommunityLine;
import org.batfish.representation.cisco.RouteMapSetLine;
import org.batfish.representation.cisco.RouteMapSetLocalPreferenceLine;
import org.batfish.representation.cisco.RouteMapSetMetricLine;
import org.batfish.representation.cisco.RouteMapSetNextHopLine;
import org.batfish.representation.cisco.RouteMapSetNextHopPeerAddress;
import org.batfish.representation.cisco.RouteMapSetOriginTypeLine;
import org.batfish.representation.cisco.RoutePolicy;
import org.batfish.representation.cisco.RoutePolicyApplyStatement;
import org.batfish.representation.cisco.RoutePolicyBoolean;
import org.batfish.representation.cisco.RoutePolicyBooleanAnd;
import org.batfish.representation.cisco.RoutePolicyBooleanApply;
import org.batfish.representation.cisco.RoutePolicyBooleanAsPathIn;
import org.batfish.representation.cisco.RoutePolicyBooleanAsPathIsLocal;
import org.batfish.representation.cisco.RoutePolicyBooleanAsPathNeighborIs;
import org.batfish.representation.cisco.RoutePolicyBooleanAsPathOriginatesFrom;
import org.batfish.representation.cisco.RoutePolicyBooleanAsPathPassesThrough;
import org.batfish.representation.cisco.RoutePolicyBooleanCommunityMatchesAny;
import org.batfish.representation.cisco.RoutePolicyBooleanCommunityMatchesEvery;
import org.batfish.representation.cisco.RoutePolicyBooleanDestination;
import org.batfish.representation.cisco.RoutePolicyBooleanLocalPreference;
import org.batfish.representation.cisco.RoutePolicyBooleanMed;
import org.batfish.representation.cisco.RoutePolicyBooleanNextHopIn;
import org.batfish.representation.cisco.RoutePolicyBooleanNot;
import org.batfish.representation.cisco.RoutePolicyBooleanOr;
import org.batfish.representation.cisco.RoutePolicyBooleanRibHasRoute;
import org.batfish.representation.cisco.RoutePolicyBooleanRouteTypeIs;
import org.batfish.representation.cisco.RoutePolicyBooleanTagIs;
import org.batfish.representation.cisco.RoutePolicyComment;
import org.batfish.representation.cisco.RoutePolicyCommunitySet;
import org.batfish.representation.cisco.RoutePolicyCommunitySetInline;
import org.batfish.representation.cisco.RoutePolicyCommunitySetName;
import org.batfish.representation.cisco.RoutePolicyDeleteAllStatement;
import org.batfish.representation.cisco.RoutePolicyDeleteCommunityStatement;
import org.batfish.representation.cisco.RoutePolicyDispositionStatement;
import org.batfish.representation.cisco.RoutePolicyDispositionType;
import org.batfish.representation.cisco.RoutePolicyElseBlock;
import org.batfish.representation.cisco.RoutePolicyElseIfBlock;
import org.batfish.representation.cisco.RoutePolicyIfStatement;
import org.batfish.representation.cisco.RoutePolicyInlinePrefix6Set;
import org.batfish.representation.cisco.RoutePolicyInlinePrefixSet;
import org.batfish.representation.cisco.RoutePolicyNextHop;
import org.batfish.representation.cisco.RoutePolicyNextHopDiscard;
import org.batfish.representation.cisco.RoutePolicyNextHopIP6;
import org.batfish.representation.cisco.RoutePolicyNextHopIp;
import org.batfish.representation.cisco.RoutePolicyNextHopPeerAddress;
import org.batfish.representation.cisco.RoutePolicyNextHopSelf;
import org.batfish.representation.cisco.RoutePolicyPrefixSet;
import org.batfish.representation.cisco.RoutePolicyPrefixSetName;
import org.batfish.representation.cisco.RoutePolicyPrependAsPath;
import org.batfish.representation.cisco.RoutePolicySetCommunity;
import org.batfish.representation.cisco.RoutePolicySetIsisMetric;
import org.batfish.representation.cisco.RoutePolicySetIsisMetricType;
import org.batfish.representation.cisco.RoutePolicySetLevel;
import org.batfish.representation.cisco.RoutePolicySetLocalPref;
import org.batfish.representation.cisco.RoutePolicySetMed;
import org.batfish.representation.cisco.RoutePolicySetNextHop;
import org.batfish.representation.cisco.RoutePolicySetOrigin;
import org.batfish.representation.cisco.RoutePolicySetOspfMetricType;
import org.batfish.representation.cisco.RoutePolicySetTag;
import org.batfish.representation.cisco.RoutePolicySetVarMetricType;
import org.batfish.representation.cisco.RoutePolicySetWeight;
import org.batfish.representation.cisco.RoutePolicyStatement;
import org.batfish.representation.cisco.SecurityZone;
import org.batfish.representation.cisco.SecurityZonePair;
import org.batfish.representation.cisco.ServiceObject;
import org.batfish.representation.cisco.ServiceObjectGroup;
import org.batfish.representation.cisco.ServiceObjectReferenceServiceObjectGroupLine;
import org.batfish.representation.cisco.SimpleExtendedAccessListServiceSpecifier;
import org.batfish.representation.cisco.StandardAccessList;
import org.batfish.representation.cisco.StandardAccessListLine;
import org.batfish.representation.cisco.StandardAccessListServiceSpecifier;
import org.batfish.representation.cisco.StandardCommunityList;
import org.batfish.representation.cisco.StandardCommunityListLine;
import org.batfish.representation.cisco.StandardIpv6AccessList;
import org.batfish.representation.cisco.StandardIpv6AccessListLine;
import org.batfish.representation.cisco.StaticRoute;
import org.batfish.representation.cisco.StubSettings;
import org.batfish.representation.cisco.TcpServiceObjectGroupLine;
import org.batfish.representation.cisco.Tunnel;
import org.batfish.representation.cisco.Tunnel.TunnelMode;
import org.batfish.representation.cisco.UdpServiceObjectGroupLine;
import org.batfish.representation.cisco.Vrf;
import org.batfish.representation.cisco.VrrpGroup;
import org.batfish.representation.cisco.VrrpInterface;
import org.batfish.representation.cisco.WildcardAddressSpecifier;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfAddressFamilyConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfNeighborAddressFamilyConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfNeighborConfiguration;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfNeighborConfiguration.RemovePrivateAsMode;
import org.batfish.vendor.StructureType;
import org.batfish.vendor.VendorConfiguration;

public class CiscoControlPlaneExtractor extends CiscoParserBaseListener
    implements ControlPlaneExtractor {

  private static final int DEFAULT_STATIC_ROUTE_DISTANCE = 1;

  private static final String DUPLICATE = "DUPLICATE";

  private static final String F_ALLOWAS_IN_NUMBER =
      "bgp -  allowas-in with number - ignored and effectively infinite for now";

  private static final String F_BGP_AUTO_SUMMARY = "bgp - auto-summary";

  private static final String F_BGP_CONFEDERATION = "bgp confederation";

  private static final String F_BGP_INHERIT_PEER_OTHER =
      "bgp - inherit peer - inheritance not implemented for this peer type";

  private static final String F_BGP_INHERIT_PEER_SESSION_OTHER =
      "bgp - inherit peer-session - inheritance not implemented for this peer type";

  private static final String F_BGP_MAXIMUM_PEERS = "bgp - maximum-peers";

  private static final String F_BGP_NEIGHBOR_DISTRIBUTE_LIST = "bgp - neighbor distribute-list";

  private static final String F_BGP_REDISTRIBUTE_AGGREGATE = "bgp - redistribute aggregate";

  private static final String F_CM_MATCH_NOT = "class-map - match not (criteria)";

  private static final String F_FRAGMENTS = "acl fragments";

  private static final String F_INTERFACE_MULTIPOINT = "interface multipoint";

  private static final String F_IP_DEFAULT_GATEWAY = "ip default-gateway";

  private static final String F_IP_ROUTE_VRF = "ip route vrf / vrf - ip route";

  private static final String F_OSPF_AREA_NSSA_NO_REDISTRIBUTION =
      "ospf - not-so-stubby areas - no-redistribution";

  private static final String F_OSPF_MAXIMUM_PATHS = "ospf - maximum-paths";

  private static final String F_OSPF_REDISTRIBUTE_EIGRP = "ospf - redistribute eigrp";

  private static final String F_OSPF_REDISTRIBUTE_RIP = "ospf - redistribute rip";

  private static final String F_ROUTE_MAP_SET_METRIC_TYPE = "route-map - set metric-type";

  private static final String F_SWITCHING_MODE = "switching-mode";

  private static final String F_TTL = "acl ttl eq number";

  private static final String F_ACL_ADDRESS_GROUP = "acl match address-group";

  private static final String F_ACL_INTERFACE = "acl match interface";

  private static final String F_ACL_OBJECT = "acl match object";

  private static final String INLINE_SERVICE_OBJECT_NAME = "~INLINE_SERVICE_OBJECT~";

  @VisibleForTesting static final String SERIAL_LINE = "serial";

  @Override
  public void exitIf_ip_ospf_network(If_ip_ospf_networkContext ctx) {
    for (Interface iface : _currentInterfaces) {
      iface.setOspfPointToPoint(ctx.POINT_TO_POINT() != null);
    }
  }

  private void defineStructure(StructureType type, String name, ParserRuleContext ctx) {
    for (int i = ctx.getStart().getLine(); i <= ctx.getStop().getLine(); ++i) {
      _configuration.defineStructure(type, name, i);
    }
  }

  private static Ip6 getIp(Access_list_ip6_rangeContext ctx) {
    if (ctx.ip != null) {
      return toIp6(ctx.ip);
    } else if (ctx.ipv6_prefix != null) {
      return new Prefix6(ctx.ipv6_prefix.getText()).getAddress();
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

  private static String toInterfaceName(Interface_nameContext ctx) {
    StringBuilder name =
        new StringBuilder(
            CiscoConfiguration.getCanonicalInterfaceNamePrefix(ctx.name_prefix_alpha.getText()));
    for (Token part : ctx.name_middle_parts) {
      name.append(part.getText());
    }
    if (ctx.range().range_list.size() != 1) {
      throw new RedFlagBatfishException(
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
    return Long.parseLong(parts[0]) << 16 + Long.parseLong(parts[1]);
  }

  private static Ip toIp(TerminalNode t) {
    return new Ip(t.getText());
  }

  private static Ip toIp(Token t) {
    return new Ip(t.getText());
  }

  private static Ip6 toIp6(TerminalNode t) {
    return new Ip6(t.getText());
  }

  private static Ip6 toIp6(Token t) {
    return new Ip6(t.getText());
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
      return new SubRange(low, low);
    }
  }

  private static String unquote(String text) {
    if (text.length() == 0) {
      return text;
    }
    if (text.charAt(0) != '"') {
      return text;
    } else if (text.charAt(text.length() - 1) != '"') {
      throw new BatfishException("Improperly-quoted string");
    } else {
      return text.substring(1, text.length() - 1);
    }
  }

  private CiscoConfiguration _configuration;

  @SuppressWarnings("unused")
  private List<AaaAccountingCommands> _currentAaaAccountingCommands;

  private AaaAuthenticationLoginList _currentAaaAuthenticationLoginList;

  private IpAsPathAccessList _currentAsPathAcl;

  private CiscoNxBgpVrfAddressFamilyConfiguration _currentBgpNxVrfAddressFamily;

  private CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration
      _currentBgpNxVrfAddressFamilyAggregateNetwork;

  private CiscoNxBgpVrfConfiguration _currentBgpNxVrfConfiguration;

  private CiscoNxBgpVrfNeighborConfiguration _currentBgpNxVrfNeighbor;

  private CiscoNxBgpVrfNeighborAddressFamilyConfiguration _currentBgpNxVrfNeighborAddressFamily;

  private final Set<String> _currentBlockNeighborAddressFamilies;

  private CryptoMapEntry _currentCryptoMapEntry;

  private String _currentCryptoMapName;

  private Integer _currentCryptoMapSequenceNum;

  private DynamicIpBgpPeerGroup _currentDynamicIpPeerGroup;

  private DynamicIpv6BgpPeerGroup _currentDynamicIpv6PeerGroup;

  @Nullable private Interface _currentEigrpInterface;

  @Nullable private EigrpProcess _currentEigrpProcess;

  private ExpandedCommunityList _currentExpandedCommunityList;

  private ExtendedAccessList _currentExtendedAcl;

  private ExtendedIpv6AccessList _currentExtendedIpv6Acl;

  private List<Interface> _currentInterfaces;

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

  private NatPool _currentNatPool;

  private Long _currentOspfArea;

  private String _currentOspfInterface;

  private OspfProcess _currentOspfProcess;

  private BgpPeerGroup _currentPeerGroup;

  private NamedBgpPeerGroup _currentPeerSession;

  private Prefix6List _currentPrefix6List;

  private PrefixList _currentPrefixList;

  private String _currentPrefixSetName;

  private RipProcess _currentRipProcess;

  private RouteMap _currentRouteMap;

  private RouteMapClause _currentRouteMapClause;

  private RoutePolicy _currentRoutePolicy;

  private ServiceClass _currentServiceClass;

  private ServiceObject _currentServiceObject;

  private SnmpCommunity _currentSnmpCommunity;

  @SuppressWarnings("unused")
  private SnmpHost _currentSnmpHost;

  private StandardAccessList _currentStandardAcl;

  private StandardCommunityList _currentStandardCommunityList;

  private StandardIpv6AccessList _currentStandardIpv6Acl;

  private User _currentUser;

  private String _currentVrf;

  private VrrpGroup _currentVrrpGroup;

  private Integer _currentVrrpGroupNum;

  private String _currentVrrpInterface;

  private BgpPeerGroup _dummyPeerGroup;

  private ConfigurationFormat _format;

  private boolean _inBlockNeighbor;

  private boolean _inIpv6BgpPeer;

  private boolean _no;

  @Nullable private EigrpProcess _parentEigrpProcess;

  private final CiscoCombinedParser _parser;

  private List<BgpPeerGroup> _peerGroupStack;

  private final String _text;

  private final Set<String> _unimplementedFeatures;

  private final boolean _unrecognizedAsRedFlag;

  private final Warnings _w;

  private NetworkObject _currentNetworkObject;

  private NetworkObjectGroup _currentNetworkObjectGroup;

  private ProtocolObjectGroup _currentProtocolObjectGroup;

  private ServiceObjectGroup _currentServiceObjectGroup;

  private InspectClassMap _currentInspectClassMap;

  private InspectPolicyMap _currentInspectPolicyMap;

  private InspectPolicyMapInspectClass _currentInspectPolicyMapInspectClass;

  private SecurityZonePair _currentSecurityZonePair;

  public CiscoControlPlaneExtractor(
      String text,
      CiscoCombinedParser parser,
      ConfigurationFormat format,
      Warnings warnings,
      boolean unrecognizedAsRedFlag) {
    _text = text;
    _parser = parser;
    _format = format;
    _unimplementedFeatures = new TreeSet<>();
    _w = warnings;
    _peerGroupStack = new ArrayList<>();
    _unrecognizedAsRedFlag = unrecognizedAsRedFlag;
    _currentBlockNeighborAddressFamilies = new HashSet<>();
  }

  private Interface addInterface(String name, Interface_nameContext ctx, boolean explicit) {
    Interface newInterface = _configuration.getInterfaces().get(name);
    if (newInterface == null) {
      newInterface = new Interface(name, _configuration);
      initInterface(newInterface, _configuration.getVendor());
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
  public void enterAaa_authentication_asa(Aaa_authentication_asaContext ctx) {
    if (_configuration.getCf().getAaa().getAuthentication().getLogin() == null) {
      _configuration.getCf().getAaa().getAuthentication().setLogin(new AaaAuthenticationLogin());
    }
    ArrayList<AuthenticationMethod> methods = new ArrayList<>();
    if (ctx.aaa_authentication_asa_console().group != null) {
      methods.add(AuthenticationMethod.GROUP_USER_DEFINED);
    }
    if (ctx.aaa_authentication_asa_console().LOCAL_ASA() != null) {
      methods.add(AuthenticationMethod.LOCAL_CASE);
    }
    if (!methods.isEmpty()) {
      AaaAuthenticationLogin login = _configuration.getCf().getAaa().getAuthentication().getLogin();
      String name = ctx.linetype.getText();
      AaaAuthenticationLoginList authList = new AaaAuthenticationLoginList(methods);

      _configuration
          .getCf()
          .getLines()
          .computeIfAbsent(name, Line::new)
          .setAaaAuthenticationLoginList(authList);

      // not allowed to specify multiple login lists for a given linetype so use computeIfAbsent
      // rather than put so we only accept the first login list
      _currentAaaAuthenticationLoginList = login.getLists().computeIfAbsent(name, k -> authList);
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
  public void enterRec_address_family(Rec_address_familyContext ctx) {
    // Step into a new address family. This results in a new EIGRP process with a specified VRF and
    // AS number

    // There may not be an ASN specified here, but it will be specified in this AF context
    long asn = ctx.asnum == null ? -1 : toLong(ctx.asnum);
    if (asn == -1) {
      todo(ctx, "add support for 'autonomous-system' configured inside address-family");
      _parentEigrpProcess = _currentEigrpProcess;
      return;
    }

    EigrpProcess proc = new EigrpProcess(asn, EigrpProcessMode.CLASSIC);

    _currentVrf = ctx.vrf.getText();
    currentVrf().setEigrpProcess(proc);

    _parentEigrpProcess = _currentEigrpProcess;
    _currentEigrpProcess = proc;
  }

  @Override
  public void enterRen_address_family(Ren_address_familyContext ctx) {
    // Step into a new address family. This results in a new EIGRP process with a specified VRF and
    // AS number

    long asn = toLong(ctx.asnum);

    if (ctx.IPV6() != null) {
      todo(ctx, "add support for ipv6 address family in eigrp");
      return;
    }
    if (ctx.MULTICAST() != null) {
      todo(ctx, "add support for multicast address family in eigrp");
      return;
    }

    EigrpProcess proc = new EigrpProcess(asn, EigrpProcessMode.NAMED);

    if (ctx.vrf != null) {
      _currentVrf = ctx.vrf.getText();
    }
    currentVrf().setEigrpProcess(proc);

    _currentEigrpProcess = proc;
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
    if (af.IPV6() != null) {
      _inIpv6BgpPeer = true;
    }
  }

  @Override
  public void enterAf_group_rb_stanza(Af_group_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String name = ctx.name.getText();
    // String af = ctx.bgp_address_family().getText();
    NamedBgpPeerGroup afGroup = proc.getAfGroups().get(name);
    if (afGroup == null) {
      proc.addNamedPeerGroup(name, ctx.name.getStart().getLine());
      afGroup = proc.getNamedPeerGroups().get(name);
    }
    pushPeer(afGroup);
    _currentNamedPeerGroup = afGroup;
  }

  @Override
  public void enterBgp_confederation_rb_stanza(Bgp_confederation_rb_stanzaContext ctx) {
    todo(ctx, F_BGP_CONFEDERATION);
  }

  @Override
  public void enterCip_profile(Cip_profileContext ctx) {
    if (_currentIpsecProfile != null) {
      throw new BatfishException("IpsecProfile should be null!");
    }
    _currentIpsecProfile = new IpsecProfile(ctx.name.getText());
    defineStructure(IPSEC_PROFILE, ctx.name.getText(), ctx);
  }

  @Override
  public void enterCip_transform_set(Cip_transform_setContext ctx) {
    if (_currentIpsecTransformSet != null) {
      throw new BatfishException("IpsecTransformSet should be null!");
    }
    _currentIpsecTransformSet = new IpsecTransformSet(ctx.name.getText());
    defineStructure(IPSEC_TRANSFORM_SET, ctx.name.getText(), ctx);
    if (ctx.ipsec_encryption() != null) {
      _currentIpsecTransformSet.setEncryptionAlgorithm(
          toEncryptionAlgorithm(ctx.ipsec_encryption()));
    } else if (ctx.ipsec_encryption_aruba() != null) {
      _currentIpsecTransformSet.setEncryptionAlgorithm(
          toEncryptionAlgorithm(ctx.ipsec_encryption_aruba()));
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
  public void enterCis_policy(Cis_policyContext ctx) {
    Integer priority = toInteger(ctx.priority);
    _currentIsakmpPolicy = new IsakmpPolicy(priority);
    _currentIsakmpPolicy.setHashAlgorithm(IkeHashingAlgorithm.SHA1);
    _currentIsakmpPolicy.setEncryptionAlgorithm(EncryptionAlgorithm.THREEDES_CBC);
    _currentIsakmpPolicy.setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS);
    if (_format == ARUBAOS) {
      _currentIsakmpPolicy.setDiffieHellmanGroup(DiffieHellmanGroup.GROUP1);
    } else {
      _currentIsakmpPolicy.setDiffieHellmanGroup(DiffieHellmanGroup.GROUP2);
    }

    _currentIsakmpPolicy.setLifetimeSeconds(86400);
    defineStructure(ISAKMP_POLICY, priority.toString(), ctx);
    /* Isakmp policies are checked in order not explicitly referenced, so add a self-reference
    here */
    _configuration.referenceStructure(
        ISAKMP_POLICY, priority.toString(), ISAKMP_POLICY_SELF_REF, ctx.priority.getLine());
  }

  @Override
  public void enterCis_profile(Cis_profileContext ctx) {
    _currentIsakmpProfile = new IsakmpProfile(ctx.name.getText());
    defineStructure(ISAKMP_PROFILE, ctx.name.getText(), ctx);
    /* Isakmp profiles are checked against for matches not explicitly referenced, so add a
    self-reference here */
    _configuration.referenceStructure(
        ISAKMP_PROFILE, ctx.name.getText(), ISAKMP_PROFILE_SELF_REF, ctx.name.start.getLine());
  }

  @Override
  public void enterCisco_configuration(Cisco_configurationContext ctx) {
    _configuration = new CiscoConfiguration(_unimplementedFeatures);
    _configuration.setVendor(_format);
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
    if (_format == CISCO_IOS) {
      Logging logging = new Logging();
      logging.setOn(true);
      _configuration.getCf().setLogging(logging);
    } else if (_format == CISCO_ASA) {
      // serial line may not be anywhere in the config so add it here to make sure the serial line
      // is in the data model
      _configuration.getCf().getLines().computeIfAbsent(SERIAL_LINE, Line::new);
    }
  }

  @Override
  public void exitCispol_authentication(Cispol_authenticationContext ctx) {
    if (ctx.PRE_SHARE() != null) {
      _currentIsakmpPolicy.setAuthenticationMethod(IkeAuthenticationMethod.PRE_SHARED_KEYS);
    } else if (ctx.RSA_SIG() != null) {
      _currentIsakmpPolicy.setAuthenticationMethod(IkeAuthenticationMethod.RSA_SIGNATURES);
    } else {
      throw new BatfishException("Unsupported authentication method in " + ctx.getText());
    }
  }

  @Override
  public void exitCispol_encr(Cispol_encrContext ctx) {
    _currentIsakmpPolicy.setEncryptionAlgorithm(toEncryptionAlgorithm(ctx.ike_encryption()));
  }

  @Override
  public void exitCispol_encryption(Cispol_encryptionContext ctx) {
    _currentIsakmpPolicy.setEncryptionAlgorithm(toEncryptionAlgorithm(ctx.ike_encryption_aruba()));
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
    _currentIsakmpProfile.setMatchIdentity(
        new IpWildcard(toIp(ctx.address), toIp(ctx.mask).inverted()));
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
      _currentIsakmpProfile.setLocalInterfaceName(ctx.interface_name().getText());
    }
  }

  @Override
  public void exitCkr_local_address(Ckr_local_addressContext ctx) {
    if (ctx.IP_ADDRESS() != null) {
      _currentKeyring.setLocalAddress(toIp(ctx.IP_ADDRESS()));
    } else {
      _currentKeyring.setLocalInterfaceName(ctx.interface_name().getText());
    }
  }

  @Override
  public void exitCkr_psk(Ckr_pskContext ctx) {
    Ip wildCardMask = ctx.wildcard_mask == null ? Ip.MAX : toIp(ctx.wildcard_mask);
    _currentKeyring.setKey(
        CommonUtil.sha256Digest(ctx.variable_permissive().getText() + CommonUtil.salt()));
    _currentKeyring.setRemoteIdentity(
        new IpWildcard(toIp(ctx.ip_address), wildCardMask.inverted()));
  }

  @Override
  public void enterClb_docsis_policy(Clb_docsis_policyContext ctx) {
    String name = ctx.policy.getText();
    String rule = ctx.rulenum.getText();
    DocsisPolicy policy =
        _configuration
            .getCf()
            .getCable()
            .getDocsisPolicies()
            .computeIfAbsent(name, DocsisPolicy::new);
    defineStructure(DOCSIS_POLICY, name, ctx);
    policy.getRules().add(rule);
    _configuration.referenceStructure(
        DOCSIS_POLICY_RULE, rule, DOCSIS_POLICY_DOCSIS_POLICY_RULE, ctx.getStart().getLine());
  }

  @Override
  public void enterClb_rule(Clb_ruleContext ctx) {
    String name = ctx.rulenum.getText();
    _configuration
        .getCf()
        .getCable()
        .getDocsisPolicyRules()
        .computeIfAbsent(name, DocsisPolicyRule::new);
    defineStructure(DOCSIS_POLICY_RULE, name, ctx);
  }

  @Override
  public void enterCntlr_rf_channel(Cntlr_rf_channelContext ctx) {
    _no = (ctx.NO() != null);
  }

  @Override
  public void enterCommunity_set_stanza(Community_set_stanzaContext ctx) {
    String name = ctx.name.getText();
    defineStructure(COMMUNITY_SET, name, ctx);
  }

  @Override
  public void enterCrypto_keyring(Crypto_keyringContext ctx) {
    if (_currentKeyring != null) {
      throw new BatfishException("Keyring should be null!");
    }
    _currentKeyring = new Keyring(ctx.name.getText());
    defineStructure(KEYRING, ctx.name.getText(), ctx);
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
      defineStructure(CRYPTO_DYNAMIC_MAP_SET, name, ctx);
    } else if (!cryptoMapSet.getDynamic()) {
      _w.redFlag(
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
      defineStructure(CRYPTO_MAP_SET, _currentCryptoMapName, ctx);
    } else if (cryptoMapSet.getDynamic()) {
      _w.redFlag(
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
  public void enterCs_class(Cs_classContext ctx) {
    String number = ctx.num.getText();
    _currentServiceClass =
        _configuration
            .getCf()
            .getCable()
            .getServiceClasses()
            .computeIfAbsent(number, ServiceClass::new);
    defineStructure(SERVICE_CLASS, number, ctx);
  }

  @Override
  public void enterDt_depi_class(Dt_depi_classContext ctx) {
    String name = ctx.name.getText();
    _configuration.referenceStructure(
        DEPI_CLASS, name, DEPI_TUNNEL_DEPI_CLASS, ctx.getStart().getLine());
  }

  @Override
  public void enterDt_l2tp_class(Dt_l2tp_classContext ctx) {
    String name = ctx.name.getText();
    _configuration.referenceStructure(
        L2TP_CLASS, name, DEPI_TUNNEL_L2TP_CLASS, ctx.getStart().getLine());
  }

  @Override
  public void enterExtended_access_list_stanza(Extended_access_list_stanzaContext ctx) {
    String name;
    if (ctx.name != null) {
      name = ctx.name.getText();
    } else if (ctx.shortname != null) {
      name = ctx.shortname.getText();
    } else if (ctx.num != null) {
      name = ctx.num.getText();
    } else {
      throw new BatfishException("Could not determine acl name");
    }
    _currentExtendedAcl =
        _configuration.getExtendedAcls().computeIfAbsent(name, ExtendedAccessList::new);
    defineStructure(IPV4_ACCESS_LIST_EXTENDED, name, ctx);
  }

  @Override
  public void enterExtended_ipv6_access_list_stanza(Extended_ipv6_access_list_stanzaContext ctx) {
    String name;
    if (ctx.name != null) {
      name = ctx.name.getText();
    } else {
      throw new BatfishException("Could not determine acl name");
    }
    _currentExtendedIpv6Acl =
        _configuration.getExtendedIpv6Acls().computeIfAbsent(name, ExtendedIpv6AccessList::new);
    defineStructure(IPV6_ACCESS_LIST_EXTENDED, name, ctx);
  }

  @Override
  public void enterIf_description(If_descriptionContext ctx) {
    Token descriptionToken = ctx.description_line().text;
    String description = descriptionToken != null ? descriptionToken.getText().trim() : "";
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setDescription(description);
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
  public void enterIp_as_path_access_list_stanza(Ip_as_path_access_list_stanzaContext ctx) {
    String name = ctx.name.getText();
    _currentAsPathAcl =
        _configuration.getAsPathAccessLists().computeIfAbsent(name, IpAsPathAccessList::new);
    defineStructure(AS_PATH_ACCESS_LIST, name, ctx);
  }

  @Override
  public void enterIp_community_list_expanded_stanza(Ip_community_list_expanded_stanzaContext ctx) {
    String name;
    if (ctx.num != null) {
      name = ctx.num.getText();
    } else if (ctx.name != null) {
      name = ctx.name.getText();
    } else {
      throw new BatfishException("Invalid community-list name");
    }
    _currentExpandedCommunityList =
        _configuration
            .getExpandedCommunityLists()
            .computeIfAbsent(name, ExpandedCommunityList::new);
    defineStructure(COMMUNITY_LIST_EXPANDED, name, ctx);
  }

  @Override
  public void enterIp_community_list_standard_stanza(Ip_community_list_standard_stanzaContext ctx) {
    String name;
    if (ctx.num != null) {
      name = ctx.num.getText();
    } else if (ctx.name != null) {
      name = ctx.name.getText();
    } else {
      throw new BatfishException("Invalid standard community-list name");
    }
    _currentStandardCommunityList =
        _configuration
            .getStandardCommunityLists()
            .computeIfAbsent(name, StandardCommunityList::new);
    defineStructure(COMMUNITY_LIST_STANDARD, name, ctx);
  }

  @Override
  public void enterIp_nat_pool(Ip_nat_poolContext ctx) {
    String name = ctx.name.getText();
    NatPool natPool = new NatPool();
    _configuration.getNatPools().put(name, natPool);
    _currentNatPool = natPool;
    defineStructure(NAT_POOL, name, ctx);
  }

  @Override
  public void enterIp_prefix_list_stanza(Ip_prefix_list_stanzaContext ctx) {
    String name = ctx.name.getText();
    _currentPrefixList = _configuration.getPrefixLists().computeIfAbsent(name, PrefixList::new);
    defineStructure(PREFIX_LIST, name, ctx);
  }

  @Override
  public void enterIp_route_stanza(Ip_route_stanzaContext ctx) {
    if (ctx.vrf != null) {
      _currentVrf = ctx.vrf.getText();
    }
    if (ctx.MANAGEMENT() != null) {
      _currentVrf = CiscoConfiguration.MANAGEMENT_VRF_NAME;
    }
  }

  @Override
  public void enterIpv6_prefix_list_stanza(Ipv6_prefix_list_stanzaContext ctx) {
    String name = ctx.name.getText();
    _currentPrefix6List = _configuration.getPrefix6Lists().computeIfAbsent(name, Prefix6List::new);
    defineStructure(PREFIX6_LIST, name, ctx);
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
        proc.addIpPeerGroup(ip);
        _currentIpPeerGroup = proc.getIpPeerGroups().get(ip);
      } else {
        _w.redFlag(
            "Duplicate IP peer group in neighbor config (line:" + ctx.start.getLine() + ")",
            DUPLICATE);
      }
      pushPeer(_currentIpPeerGroup);
    } else if (ctx.ip_prefix != null) {
      Prefix prefix = Prefix.parse(ctx.ip_prefix.getText());
      _currentDynamicIpPeerGroup = proc.getDynamicIpPeerGroups().get(prefix);
      if (_currentDynamicIpPeerGroup == null) {
        _currentDynamicIpPeerGroup = proc.addDynamicIpPeerGroup(prefix);
      } else {
        _w.redFlag(
            "Duplicate DynamicIP peer group neighbor config (line:" + ctx.start.getLine() + ")",
            DUPLICATE);
      }
      pushPeer(_currentDynamicIpPeerGroup);
    } else if (ctx.ipv6_address != null) {
      Ip6 ip6 = toIp6(ctx.ipv6_address);
      Ipv6BgpPeerGroup pg = proc.getIpv6PeerGroups().get(ip6);
      if (pg == null) {
        proc.addIpv6PeerGroup(ip6);
        pg = proc.getIpv6PeerGroups().get(ip6);
      } else {
        _w.redFlag(
            "Duplicate IPV6 peer group in neighbor config (line:" + ctx.start.getLine() + ")",
            DUPLICATE);
      }
      pushPeer(pg);
      _currentIpv6PeerGroup = pg;
    } else if (ctx.ipv6_prefix != null) {
      Prefix6 prefix6 = new Prefix6(ctx.ipv6_prefix.getText());
      DynamicIpv6BgpPeerGroup pg = proc.getDynamicIpv6PeerGroups().get(prefix6);
      if (pg == null) {
        pg = proc.addDynamicIpv6PeerGroup(prefix6);
      } else {
        _w.redFlag(
            "Duplicate Dynamic Ipv6 peer group neighbor config (line:" + ctx.start.getLine() + ")",
            DUPLICATE);
      }
      pushPeer(pg);
      _currentDynamicIpv6PeerGroup = pg;
    }
    if (ctx.asnum != null) {
      long remoteAs = toLong(ctx.asnum);
      _currentPeerGroup.setRemoteAs(remoteAs);
    }
    if (ctx.mapname != null) {
      String routeMap = ctx.mapname.getText();
      int line = ctx.mapname.getStart().getLine();
      _configuration.referenceStructure(
          ROUTE_MAP, routeMap, BGP_NEIGHBOR_REMOTE_AS_ROUTE_MAP, line);
    }
    // TODO: verify if this is correct for nexus
    _currentPeerGroup.setActive(true);
    _currentPeerGroup.setShutdown(false);
  }

  @Override
  public void enterNeighbor_flat_rb_stanza(Neighbor_flat_rb_stanzaContext ctx) {
    // do no further processing for unsupported address families / containers
    if (_currentPeerGroup == _dummyPeerGroup) {
      pushPeer(_dummyPeerGroup);
      return;
    }
    BgpProcess proc = currentVrf().getBgpProcess();
    // we must create peer group if it does not exist and this is a remote_as
    // declaration
    boolean create =
        ctx.remote_as_bgp_tail() != null || ctx.inherit_peer_session_bgp_tail() != null;
    if (ctx.ip != null) {
      Ip ip = toIp(ctx.ip);
      _currentIpPeerGroup = proc.getIpPeerGroups().get(ip);
      if (_currentIpPeerGroup == null) {
        if (create || _format == ARISTA) {
          proc.addIpPeerGroup(ip);
          _currentIpPeerGroup = proc.getIpPeerGroups().get(ip);
          pushPeer(_currentIpPeerGroup);
        } else {
          String message = "Ignoring reference to undeclared peer group: '" + ip + "'";
          _w.redFlag(message);
          pushPeer(_dummyPeerGroup);
        }
      } else {
        pushPeer(_currentIpPeerGroup);
      }
    } else if (ctx.ip6 != null) {
      Ip6 ip6 = toIp6(ctx.ip6);
      Ipv6BgpPeerGroup pg6 = proc.getIpv6PeerGroups().get(ip6);
      if (pg6 == null) {
        if (create || _format == ARISTA) {
          proc.addIpv6PeerGroup(ip6);
          pg6 = proc.getIpv6PeerGroups().get(ip6);
          pushPeer(pg6);
        } else {
          String message = "Ignoring reference to undeclared peer group: '" + ip6 + "'";
          _w.redFlag(message);
          pushPeer(_dummyPeerGroup);
        }
      } else {
        pushPeer(pg6);
      }
      _currentIpv6PeerGroup = pg6;
    } else if (ctx.peergroup != null) {
      String name = ctx.peergroup.getText();
      int definitionLine = ctx.peergroup.getLine();
      _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
      if (_currentNamedPeerGroup == null) {
        if (create || _format == ARISTA) {
          proc.addNamedPeerGroup(name, definitionLine);
          _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
        } else {
          int line = ctx.peergroup.getLine();
          _configuration.getUndefinedPeerGroups().put(name, line);
          _w.redFlag("reference to undeclared peer group: '" + name + "'");
        }
      }
      pushPeer(_currentNamedPeerGroup);
    } else {
      throw new BatfishException("unknown neighbor type");
    }
  }

  @Override
  public void enterNeighbor_group_rb_stanza(Neighbor_group_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String name = ctx.name.getText();
    int definitionLine = ctx.name.getStart().getLine();
    _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
    if (_currentNamedPeerGroup == null) {
      proc.addNamedPeerGroup(name, definitionLine);
      _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
    }
    pushPeer(_currentNamedPeerGroup);
  }

  @Override
  public void enterNet_is_stanza(Net_is_stanzaContext ctx) {
    IsisProcess proc = currentVrf().getIsisProcess();
    IsoAddress isoAddress = new IsoAddress(ctx.ISO_ADDRESS().getText());
    proc.setNetAddress(isoAddress);
  }

  @Override
  public void enterO_network(O_networkContext ctx) {
    _currentNetworkObject =
        _configuration.getNetworkObjects().computeIfAbsent(ctx.name.getText(), NetworkObject::new);
    defineStructure(NETWORK_OBJECT, ctx.name.getText(), ctx);
  }

  @Override
  public void exitO_network(O_networkContext ctx) {
    _currentNetworkObject = null;
  }

  @Override
  public void enterO_service(O_serviceContext ctx) {
    String name = ctx.name.getText();
    _currentServiceObject =
        _configuration.getServiceObjects().computeIfAbsent(name, ServiceObject::new);
    defineStructure(CiscoStructureType.SERVICE_OBJECT, name, ctx);
  }

  @Override
  public void exitO_service(O_serviceContext ctx) {
    _currentServiceObject = null;
  }

  @Override
  public void enterOg_network(Og_networkContext ctx) {
    String name = ctx.name.getText();
    // If there is a conflict, create a dummy object group
    if (_configuration.getObjectGroups().get(name) != null) {
      _currentNetworkObjectGroup = new NetworkObjectGroup(name);
      warnObjectGroupRedefinition(name);
    } else {
      _currentNetworkObjectGroup =
          _configuration.getNetworkObjectGroups().computeIfAbsent(name, NetworkObjectGroup::new);
      _configuration.getObjectGroups().put(name, _currentNetworkObjectGroup);
      defineStructure(NETWORK_OBJECT_GROUP, name, ctx);
    }
  }

  @Override
  public void exitOg_network(Og_networkContext ctx) {
    _currentNetworkObjectGroup = null;
  }

  @Override
  public void enterOg_service(Og_serviceContext ctx) {
    String name = ctx.name.getText();
    // If there is a conflict, create a dummy object group
    if (_configuration.getObjectGroups().get(name) != null) {
      _currentServiceObjectGroup = new ServiceObjectGroup(name);
      warnObjectGroupRedefinition(name);
    } else {
      _currentServiceObjectGroup =
          _configuration.getServiceObjectGroups().computeIfAbsent(name, ServiceObjectGroup::new);
      _configuration.getObjectGroups().put(name, _currentServiceObjectGroup);
      defineStructure(SERVICE_OBJECT_GROUP, name, ctx);
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
      warnObjectGroupRedefinition(name);
    } else {
      _currentProtocolObjectGroup =
          _configuration.getProtocolObjectGroups().computeIfAbsent(name, ProtocolObjectGroup::new);
      _configuration.getObjectGroups().put(name, _currentProtocolObjectGroup);
      defineStructure(PROTOCOL_OBJECT_GROUP, name, ctx);
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
    _currentNetworkObjectGroup.getLines().add(new IpWildcard(toIp(ctx.ip)).toIpSpace());
  }

  @Override
  public void exitOgn_ip_with_mask(Ogn_ip_with_maskContext ctx) {
    Ip ip = toIp(ctx.ip);
    Ip mask = toIp(ctx.mask);
    _currentNetworkObjectGroup.getLines().add(new IpWildcard(new Prefix(ip, mask)).toIpSpace());
  }

  @Override
  public void exitOgn_network_object(Ogn_network_objectContext ctx) {
    IpSpace ipSpace = null;
    if (ctx.prefix != null) {
      ipSpace = new IpWildcard(ctx.prefix.getText()).toIpSpace();
    } else if (ctx.wildcard_address != null && ctx.wildcard_mask != null) {
      // Mask needs to be inverted since zeros are don't-cares in this context
      ipSpace =
          new IpWildcard(toIp(ctx.wildcard_address), toIp(ctx.wildcard_mask).inverted())
              .toIpSpace();
    } else if (ctx.address != null) {
      ipSpace = new IpWildcard(ctx.address.getText()).toIpSpace();
    } else if (ctx.name != null) {
      String name = ctx.name.getText();
      ipSpace = new IpSpaceReference(name);
      _configuration.referenceStructure(
          NETWORK_OBJECT, name, NETWORK_OBJECT_GROUP_NETWORK_OBJECT, ctx.name.start.getLine());
    }
    if (ipSpace == null) {
      _w.redFlag("Unimplemented object-group network line: " + getFullText(ctx));
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
          CiscoStructureType.SERVICE_OBJECT,
          name,
          CiscoStructureUsage.SERVICE_OBJECT_GROUP_SERVICE_OBJECT,
          ctx.name.getStart().getLine());
    } else if (ctx.service_specifier() != null) {
      _currentServiceObjectGroup.getLines().add(_currentServiceObject);
      _currentServiceObject = null;
    }
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
  public void exitOn_description(On_descriptionContext ctx) {
    _currentNetworkObject.setDescription(ctx.description_line().getText());
  }

  @Override
  public void exitOn_fqdn(On_fqdnContext ctx) {
    _currentNetworkObject.setIpSpace(EmptyIpSpace.INSTANCE);
    _w.redFlag("Unknown how to resolve domain name to IP address: " + getFullText(ctx));
  }

  @Override
  public void exitOn_host(On_hostContext ctx) {
    if (ctx.address != null) {
      _currentNetworkObject.setIpSpace(new Ip(ctx.address.getText()).toIpSpace());
    } else {
      // IPv6
      _w.redFlag("Unimplemented network object line: " + getFullText(ctx));
    }
  }

  @Override
  public void exitOn_range(On_rangeContext ctx) {
    _w.redFlag("Unimplemented network object line: " + getFullText(ctx));
  }

  @Override
  public void exitOn_subnet(On_subnetContext ctx) {
    if (ctx.address != null) {
      _currentNetworkObject.setIpSpace(
          new Prefix(new Ip(ctx.address.getText()), new Ip(ctx.mask.getText())).toIpSpace());
    } else {
      // IPv6
      _w.redFlag("Unimplemented network object line: " + getFullText(ctx));
    }
  }

  @Override
  public void exitOs_description(Os_descriptionContext ctx) {
    _currentServiceObject.setDescription(ctx.description_line().getText());
  }

  @Override
  public void enterPrefix_set_stanza(Prefix_set_stanzaContext ctx) {
    _currentPrefixSetName = ctx.name.getText();
    defineStructure(PREFIX_SET, _currentPrefixSetName, ctx);
  }

  @Override
  public void enterRbnx_address_family(Rbnx_address_familyContext ctx) {
    String familyStr = ctx.first.getText() + '-' + ctx.second.getText();
    _currentBgpNxVrfAddressFamily =
        _currentBgpNxVrfConfiguration.getOrCreateAddressFamily(familyStr);
  }

  @Override
  public void exitRbnx_address_family(Rbnx_address_familyContext ctx) {
    _currentBgpNxVrfAddressFamily = null;
  }

  @Override
  public void enterRbnx_af_aggregate_address(Rbnx_af_aggregate_addressContext ctx) {
    if (ctx.prefix != null) {
      Prefix prefix = Prefix.parse(ctx.prefix.getText());
      _currentBgpNxVrfAddressFamilyAggregateNetwork =
          _currentBgpNxVrfAddressFamily.getOrCreateAggregateNetwork(prefix);
    } else if (ctx.network != null && ctx.subnet != null) {
      Prefix prefix = new Prefix(toIp(ctx.network), toIp(ctx.subnet));
      _currentBgpNxVrfAddressFamilyAggregateNetwork =
          _currentBgpNxVrfAddressFamily.getOrCreateAggregateNetwork(prefix);
    } else if (ctx.prefix6 != null) {
      Prefix6 prefix = new Prefix6(ctx.prefix6.getText());
      _currentBgpNxVrfAddressFamilyAggregateNetwork =
          _currentBgpNxVrfAddressFamily.getOrCreateAggregateNetwork(prefix);
    }
  }

  @Override
  public void exitRbnx_af_aggregate_address(Rbnx_af_aggregate_addressContext ctx) {
    _currentBgpNxVrfAddressFamilyAggregateNetwork = null;
  }

  @Override
  public void exitRbnx_af_aa_tail(Rbnx_af_aa_tailContext ctx) {
    int line = ctx.getStart().getLine();
    if (ctx.ADVERTISE_MAP() != null) {
      String name = ctx.mapname.getText();
      _configuration.referenceStructure(ROUTE_MAP, name, BGP_ROUTE_MAP_ADVERTISE, line);
      _currentBgpNxVrfAddressFamilyAggregateNetwork.setAdvertiseMap(name);
    } else if (ctx.AS_SET() != null) {
      _currentBgpNxVrfAddressFamilyAggregateNetwork.setAsSet(true);
    } else if (ctx.ATTRIBUTE_MAP() != null) {
      String name = ctx.mapname.getText();
      _configuration.referenceStructure(ROUTE_MAP, name, BGP_ROUTE_MAP_ATTRIBUTE, line);
      _currentBgpNxVrfAddressFamilyAggregateNetwork.setAttributeMap(name);
    } else if (ctx.SUMMARY_ONLY() != null) {
      _currentBgpNxVrfAddressFamilyAggregateNetwork.setSummaryOnly(true);
    } else if (ctx.SUPPRESS_MAP() != null) {
      String name = ctx.mapname.getText();
      _configuration.referenceStructure(ROUTE_MAP, name, BGP_ROUTE_MAP_SUPPRESS, line);
      _currentBgpNxVrfAddressFamilyAggregateNetwork.setSuppressMap(name);
    }
  }

  @Override
  public void exitRbnx_af_additional_paths(Rbnx_af_additional_pathsContext ctx) {
    if (ctx.mapname != null) {
      String name = ctx.mapname.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, name, BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());
    }
  }

  @Override
  public void exitRbnx_af_client_to_client(Rbnx_af_client_to_clientContext ctx) {
    _currentBgpNxVrfAddressFamily.setClientToClientReflection(true);
  }

  @Override
  public void exitRbnx_af_dampening(Rbnx_af_dampeningContext ctx) {
    if (ctx.mapname != null) {
      String name = ctx.mapname.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, name, BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());
    }
  }

  @Override
  public void exitRbnx_af_default_metric(Rbnx_af_default_metricContext ctx) {
    long metric = toLong(ctx.metric);
    _currentBgpNxVrfAddressFamily.setDefaultMetric(metric);
  }

  @Override
  public void exitRbnx_af_default_information(Rbnx_af_default_informationContext ctx) {
    _currentBgpNxVrfAddressFamily.setDefaultInformationOriginate(true);
  }

  @Override
  public void exitRbnx_af_distance(Rbnx_af_distanceContext ctx) {
    _currentBgpNxVrfAddressFamily.setDistanceEbgp(toInteger(ctx.ebgp));
    _currentBgpNxVrfAddressFamily.setDistanceIbgp(toInteger(ctx.ibgp));
    _currentBgpNxVrfAddressFamily.setDistanceLocal(toInteger(ctx.local));
  }

  @Override
  public void exitRbnx_af_inject_map(Rbnx_af_inject_mapContext ctx) {
    _configuration.referenceStructure(
        ROUTE_MAP, ctx.injectmap.getText(), BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());

    _configuration.referenceStructure(
        ROUTE_MAP, ctx.existmap.getText(), BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());
  }

  @Override
  public void exitRbnx_af_maximum_paths(Rbnx_af_maximum_pathsContext ctx) {
    int limit = toInteger(ctx.numpaths);
    if (ctx.IBGP() != null) {
      _currentBgpNxVrfAddressFamily.setMaximumPathsIbgp(limit);
    } else if (ctx.EIBGP() != null) {
      _currentBgpNxVrfAddressFamily.setMaximumPathsEbgp(limit);
      _currentBgpNxVrfAddressFamily.setMaximumPathsIbgp(limit);
    } else {
      _currentBgpNxVrfAddressFamily.setMaximumPathsEbgp(limit);
    }
  }

  @Override
  public void exitRbnx_af_network(Rbnx_af_networkContext ctx) {
    String mapname = "";
    if (ctx.mapname != null) {
      mapname = ctx.mapname.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());
    }

    if (ctx.prefix != null) {
      Prefix prefix = Prefix.parse(ctx.prefix.getText());
      _currentBgpNxVrfAddressFamily.addIpNetwork(prefix, mapname);
    } else if (ctx.address != null && ctx.mask != null) {
      Prefix prefix = new Prefix(toIp(ctx.address), toIp(ctx.mask));
      _currentBgpNxVrfAddressFamily.addIpNetwork(prefix, mapname);
    } else if (ctx.prefix6 != null) {
      Prefix6 prefix = new Prefix6(ctx.prefix6.getText());
      _currentBgpNxVrfAddressFamily.addIpv6Network(prefix, mapname);
    }
  }

  @Override
  public void exitRbnx_af_nexthop_route_map(Rbnx_af_nexthop_route_mapContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());
  }

  @Override
  public void exitRbnx_af_redistribute_direct(Rbnx_af_redistribute_directContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_CONNECTED_MAP, ctx.getStart().getLine());
    _currentBgpNxVrfAddressFamily.setRedistributionPolicy(RoutingProtocol.CONNECTED, name, null);
  }

  @Override
  public void exitRbnx_af_redistribute_eigrp(Rbnx_af_redistribute_eigrpContext ctx) {
    String name = ctx.mapname.getText();
    String sourceTag = ctx.source_tag.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_EIGRP_MAP, ctx.getStart().getLine());
    _currentBgpNxVrfAddressFamily.setRedistributionPolicy(RoutingProtocol.EIGRP, name, sourceTag);
  }

  @Override
  public void exitRbnx_af_redistribute_isis(Rbnx_af_redistribute_isisContext ctx) {
    String name = ctx.mapname.getText();
    String sourceTag = ctx.source_tag.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_ISIS_MAP, ctx.getStart().getLine());
    _currentBgpNxVrfAddressFamily.setRedistributionPolicy(RoutingProtocol.ISIS, name, sourceTag);
  }

  @Override
  public void exitRbnx_af_redistribute_lisp(Rbnx_af_redistribute_lispContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_LISP_MAP, ctx.getStart().getLine());
    _currentBgpNxVrfAddressFamily.setRedistributionPolicy(RoutingProtocol.LISP, name, null);
  }

  @Override
  public void exitRbnx_af_redistribute_ospf(Rbnx_af_redistribute_ospfContext ctx) {
    String name = ctx.mapname.getText();
    String sourceTag = ctx.source_tag.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_OSPF_MAP, ctx.getStart().getLine());
    _currentBgpNxVrfAddressFamily.setRedistributionPolicy(RoutingProtocol.OSPF, name, sourceTag);
  }

  @Override
  public void exitRbnx_af_redistribute_ospfv3(Rbnx_af_redistribute_ospfv3Context ctx) {
    String name = ctx.mapname.getText();
    String sourceTag = ctx.source_tag.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_OSPFV3_MAP, ctx.getStart().getLine());
    _currentBgpNxVrfAddressFamily.setRedistributionPolicy(RoutingProtocol.OSPF3, name, sourceTag);
  }

  @Override
  public void exitRbnx_af_redistribute_rip(Rbnx_af_redistribute_ripContext ctx) {
    String name = ctx.mapname.getText();
    String sourceTag = ctx.source_tag.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_RIP_MAP, ctx.getStart().getLine());
    _currentBgpNxVrfAddressFamily.setRedistributionPolicy(RoutingProtocol.RIP, name, sourceTag);
  }

  @Override
  public void exitRbnx_af_redistribute_static(Rbnx_af_redistribute_staticContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_REDISTRIBUTE_STATIC_MAP, ctx.getStart().getLine());
    _currentBgpNxVrfAddressFamily.setRedistributionPolicy(RoutingProtocol.STATIC, name, null);
  }

  @Override
  public void exitRbnx_af_suppress_inactive(Rbnx_af_suppress_inactiveContext ctx) {
    _currentBgpNxVrfAddressFamily.setSuppressInactive(true);
  }

  @Override
  public void exitRbnx_af_table_map(Rbnx_af_table_mapContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.referenceStructure(ROUTE_MAP, name, BGP_TABLE_MAP, ctx.getStart().getLine());
  }

  @Override
  public void exitRbnx_bestpath(Rbnx_bestpathContext ctx) {
    if (ctx.ALWAYS_COMPARE_MED() != null) {
      _currentBgpNxVrfConfiguration.setBestpathAlwaysCompareMed(true);
    } else if (ctx.AS_PATH() != null && ctx.MULTIPATH_RELAX() != null) {
      _currentBgpNxVrfConfiguration.setBestpathAsPathMultipathRelax(true);
    } else if (ctx.COMPARE_ROUTERID() != null) {
      _currentBgpNxVrfConfiguration.setBestpathCompareRouterId(true);
    } else if (ctx.COST_COMMUNITY() != null && ctx.IGNORE() != null) {
      _currentBgpNxVrfConfiguration.setBestpathCostCommunityIgnore(true);
    } else if (ctx.MED() != null && ctx.CONFED() != null) {
      _currentBgpNxVrfConfiguration.setBestpathMedConfed(true);
    } else if (ctx.MED() != null && ctx.MISSING_AS_WORST() != null) {
      _currentBgpNxVrfConfiguration.setBestpathMedMissingAsWorst(true);
    } else if (ctx.MED() != null && ctx.NON_DETERMINISTIC() != null) {
      _currentBgpNxVrfConfiguration.setBestpathMedNonDeterministic(true);
    } else {
      _w.redFlag("Unsupported BGP bestpath configuration: " + ctx.getText());
    }
  }

  @Override
  public void exitRbnx_cluster_id(Rbnx_cluster_idContext ctx) {
    if (ctx.ip != null) {
      _currentBgpNxVrfConfiguration.setClusterId(toIp(ctx.ip));
    } else {
      _currentBgpNxVrfConfiguration.setClusterId(new Ip(toLong(ctx.ip_as_int)));
    }
  }

  @Override
  public void exitRbnx_confederation_identifier(Rbnx_confederation_identifierContext ctx) {
    todo(ctx, F_BGP_CONFEDERATION);
  }

  @Override
  public void exitRbnx_confederation_peers(Rbnx_confederation_peersContext ctx) {
    todo(ctx, F_BGP_CONFEDERATION);
  }

  @Override
  public void exitRbnx_enforce_first_as(Rbnx_enforce_first_asContext ctx) {
    _configuration.getNxBgpGlobalConfiguration().setEnforceFirstAs(true);
  }

  @Override
  public void exitRbnx_log_neighbor_changes(Rbnx_log_neighbor_changesContext ctx) {
    _currentBgpNxVrfConfiguration.setLogNeighborChanges(true);
  }

  @Override
  public void exitRbnx_maxas_limit(Rbnx_maxas_limitContext ctx) {
    int limit = toInteger(ctx.limit);
    _currentBgpNxVrfConfiguration.setMaxasLimit(limit);
  }

  @Override
  public void enterRbnx_neighbor(Rbnx_neighborContext ctx) {
    if (ctx.ip != null) {
      Ip ip = toIp(ctx.ip);
      _currentBgpNxVrfNeighbor = _currentBgpNxVrfConfiguration.getOrCreateNeighbor(ip);
    } else if (ctx.prefix != null) {
      Prefix prefix = Prefix.parse(ctx.prefix.getText());
      _currentBgpNxVrfNeighbor = _currentBgpNxVrfConfiguration.getOrCreatePassiveNeighbor(prefix);
    } else if (ctx.ip6 != null) {
      Ip6 ip = toIp6(ctx.ip6);
      _currentBgpNxVrfNeighbor = _currentBgpNxVrfConfiguration.getOrCreateNeighbor(ip);
    } else if (ctx.prefix6 != null) {
      Prefix6 prefix = new Prefix6(ctx.prefix6.getText());
      _currentBgpNxVrfNeighbor = _currentBgpNxVrfConfiguration.getOrCreatePassiveNeighbor(prefix);
    } else {
      throw new BatfishException(
          "BGP neighbor IP definition not supported in line " + ctx.getText());
    }

    if (ctx.REMOTE_AS() != null && ctx.bgp_asn() != null) {
      long asn = toAsNum(ctx.bgp_asn());
      _currentBgpNxVrfNeighbor.setRemoteAs(asn);
    }

    if (ctx.REMOTE_AS() != null && ctx.ROUTE_MAP() != null) {
      String name = ctx.mapname.getText();
      _currentBgpNxVrfNeighbor.setRemoteAsRouteMap(name);
      _configuration.referenceStructure(
          ROUTE_MAP, name, BGP_NEIGHBOR_REMOTE_AS_ROUTE_MAP, ctx.getStart().getLine());
    }
  }

  @Override
  public void exitRbnx_neighbor(Rbnx_neighborContext ctx) {
    _currentBgpNxVrfNeighbor = null;
  }

  @Override
  public void enterRbnx_n_address_family(Rbnx_n_address_familyContext ctx) {
    String familyStr = ctx.first.getText() + '-' + ctx.second.getText();
    _currentBgpNxVrfNeighborAddressFamily =
        _currentBgpNxVrfNeighbor.getOrCreateAddressFamily(familyStr);
  }

  @Override
  public void exitRbnx_n_address_family(Rbnx_n_address_familyContext ctx) {
    _currentBgpNxVrfNeighborAddressFamily = null;
  }

  @Override
  public void exitRbnx_n_af_advertise_map(Rbnx_n_af_advertise_mapContext ctx) {
    _configuration.referenceStructure(
        ROUTE_MAP, ctx.mapname.getText(), BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());

    if (ctx.EXIST_MAP() != null) {
      _configuration.referenceStructure(
          ROUTE_MAP, ctx.existmap.getText(), BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());
    }

    if (ctx.NON_EXIST_MAP() != null) {
      _configuration.referenceStructure(
          ROUTE_MAP, ctx.nonexistmap.getText(), BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());
    }
  }

  @Override
  public void exitRbnx_n_af_allowas_in(Rbnx_n_af_allowas_inContext ctx) {
    if (ctx.num != null) {
      todo(ctx, F_ALLOWAS_IN_NUMBER);
    }
    _currentBgpNxVrfNeighborAddressFamily.setAllowAsIn(true);
  }

  @Override
  public void exitRbnx_n_af_as_override(Rbnx_n_af_as_overrideContext ctx) {
    _currentBgpNxVrfNeighborAddressFamily.setAsOverride(true);
  }

  @Override
  public void exitRbnx_n_af_default_originate(Rbnx_n_af_default_originateContext ctx) {
    _currentBgpNxVrfNeighborAddressFamily.setDefaultOriginate(true);
    if (ctx.ROUTE_MAP() != null) {
      String name = ctx.mapname.getText();
      _currentBgpNxVrfNeighborAddressFamily.setDefaultOriginateMap(name);
      _configuration.referenceStructure(
          ROUTE_MAP, name, BGP_DEFAULT_ORIGINATE_ROUTE_MAP, ctx.getStart().getLine());
    }
  }

  @Override
  public void exitRbnx_n_af_disable_peer_as_check(Rbnx_n_af_disable_peer_as_checkContext ctx) {
    _currentBgpNxVrfNeighborAddressFamily.setDisablePeerAsCheck(true);
  }

  @Override
  public void exitRbnx_n_af_filter_list(Rbnx_n_af_filter_listContext ctx) {
    String filterList = ctx.name.getText();
    CiscoStructureType type = _inIpv6BgpPeer ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    CiscoStructureUsage usage =
        _inIpv6BgpPeer
            ? ((ctx.IN() != null) ? BGP_INBOUND_FILTER6_LIST : BGP_OUTBOUND_FILTER6_LIST)
            : ((ctx.IN() != null) ? BGP_INBOUND_FILTER_LIST : BGP_OUTBOUND_FILTER_LIST);
    _configuration.referenceStructure(type, filterList, usage, ctx.getStart().getLine());
  }

  @Override
  public void exitRbnx_n_af_inherit(Rbnx_n_af_inheritContext ctx) {
    String name = ctx.template.getText();
    int sequence = toInteger(ctx.seq);
    _currentBgpNxVrfNeighborAddressFamily.setInheritPeerPolicy(sequence, name);
    _configuration.referenceStructure(
        BGP_TEMPLATE_PEER_POLICY, name, BGP_INHERITED_PEER_POLICY, ctx.getStart().getLine());
  }

  @Override
  public void exitRbnx_n_af_next_hop_self(Rbnx_n_af_next_hop_selfContext ctx) {
    _currentBgpNxVrfNeighborAddressFamily.setNextHopSelf(true);
  }

  @Override
  public void exitRbnx_n_af_next_hop_third_party(Rbnx_n_af_next_hop_third_partyContext ctx) {
    _currentBgpNxVrfNeighborAddressFamily.setNextHopThirdParty(true);
  }

  @Override
  public void exitRbnx_n_af_prefix_list(Rbnx_n_af_prefix_listContext ctx) {
    String prefixList = ctx.listname.getText();
    CiscoStructureType type = _inIpv6BgpPeer ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    CiscoStructureUsage usage =
        _inIpv6BgpPeer
            ? ((ctx.IN() != null) ? BGP_INBOUND_PREFIX6_LIST : BGP_OUTBOUND_PREFIX6_LIST)
            : ((ctx.IN() != null) ? BGP_INBOUND_PREFIX_LIST : BGP_OUTBOUND_PREFIX_LIST);
    _configuration.referenceStructure(type, prefixList, usage, ctx.getStart().getLine());
  }

  @Override
  public void exitRbnx_n_af_route_map(Rbnx_n_af_route_mapContext ctx) {
    String name = ctx.mapname.getText();
    CiscoStructureUsage usage;
    if (ctx.IN() != null) {
      usage = BGP_INBOUND_ROUTE_MAP;
      _currentBgpNxVrfNeighborAddressFamily.setInboundRouteMap(name);
    } else {
      usage = BGP_OUTBOUND_ROUTE_MAP;
      _currentBgpNxVrfNeighborAddressFamily.setOutboundRouteMap(name);
    }
    _configuration.referenceStructure(ROUTE_MAP, name, usage, ctx.getStart().getLine());
  }

  @Override
  public void exitRbnx_n_af_route_reflector_client(Rbnx_n_af_route_reflector_clientContext ctx) {
    _currentBgpNxVrfNeighborAddressFamily.setRouteReflectorClient(true);
  }

  @Override
  public void exitRbnx_n_af_send_community(Rbnx_n_af_send_communityContext ctx) {
    if (ctx.BOTH() != null || ctx.EXTENDED() != null) {
      _currentBgpNxVrfNeighborAddressFamily.setSendCommunityExtended(true);
    }
    if (ctx.BOTH() != null || ctx.STANDARD() != null || ctx.EXTENDED() == null) {
      _currentBgpNxVrfNeighborAddressFamily.setSendCommunityStandard(true);
    }
  }

  @Override
  public void exitRbnx_n_af_suppress_inactive(Rbnx_n_af_suppress_inactiveContext ctx) {
    _currentBgpNxVrfNeighborAddressFamily.setSuppressInactive(true);
  }

  @Override
  public void exitRbnx_n_af_unsuppress_map(Rbnx_n_af_unsuppress_mapContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_ROUTE_MAP_OTHER, ctx.getStart().getLine());
  }

  @Override
  public void exitRbnx_n_description(Rbnx_n_descriptionContext ctx) {
    _currentBgpNxVrfNeighbor.setDescription(ctx.desc.getText().trim());
  }

  @Override
  public void exitRbnx_n_ebgp_multihop(Rbnx_n_ebgp_multihopContext ctx) {
    _currentBgpNxVrfNeighbor.setEbgpMultihopTtl(toInteger(ctx.ebgp_ttl));
  }

  @Override
  public void exitRbnx_n_inherit(Rbnx_n_inheritContext ctx) {
    String name = ctx.peer.getText();
    if (ctx.PEER() != null) {
      _currentBgpNxVrfNeighbor.setInheritPeer(name);
      _configuration.referenceStructure(
          BGP_TEMPLATE_PEER, name, BGP_INHERITED_PEER, ctx.getStart().getLine());
    } else {
      _currentBgpNxVrfNeighbor.setInheritPeerSession(name);
      _configuration.referenceStructure(
          BGP_TEMPLATE_PEER_SESSION, name, BGP_INHERITED_SESSION, ctx.getStart().getLine());
    }
  }

  @Override
  public void exitRbnx_n_local_as(Rbnx_n_local_asContext ctx) {
    long asn = toAsNum(ctx.bgp_asn());
    _currentBgpNxVrfNeighbor.setLocalAs(asn);
  }

  @Override
  public void exitRbnx_n_no_shutdown(Rbnx_n_no_shutdownContext ctx) {
    _currentBgpNxVrfNeighbor.setShutdown(false);
  }

  @Override
  public void exitRbnx_n_remote_as(Rbnx_n_remote_asContext ctx) {
    long asn = toAsNum(ctx.bgp_asn());
    _currentBgpNxVrfNeighbor.setRemoteAs(asn);
  }

  @Override
  public void exitRbnx_n_remove_private_as(Rbnx_n_remove_private_asContext ctx) {
    if (ctx.ALL() != null) {
      _currentBgpNxVrfNeighbor.setRemovePrivateAs(RemovePrivateAsMode.ALL);
    } else if (ctx.REPLACE_AS() != null) {
      _currentBgpNxVrfNeighbor.setRemovePrivateAs(RemovePrivateAsMode.REPLACE_AS);
    }
  }

  @Override
  public void exitRbnx_n_shutdown(Rbnx_n_shutdownContext ctx) {
    _currentBgpNxVrfNeighbor.setShutdown(true);
  }

  @Override
  public void exitRbnx_n_update_source(Rbnx_n_update_sourceContext ctx) {
    String name = toInterfaceName(ctx.interface_name());
    _currentBgpNxVrfNeighbor.setUpdateSource(name);
    _configuration.referenceStructure(
        INTERFACE, name, BGP_UPDATE_SOURCE_INTERFACE, ctx.getStart().getLine());
  }

  @Override
  public void exitRbnx_no_enforce_first_as(Rbnx_no_enforce_first_asContext ctx) {
    _configuration.getNxBgpGlobalConfiguration().setEnforceFirstAs(false);
  }

  @Override
  public void exitRbnx_router_id(Rbnx_router_idContext ctx) {
    Ip ip = toIp(ctx.IP_ADDRESS());
    _currentBgpNxVrfConfiguration.setRouterId(ip);
  }

  @Override
  public void enterRbnx_template_peer(Rbnx_template_peerContext ctx) {
    String name = ctx.peer.getText();
    _currentBgpNxVrfNeighbor =
        _configuration.getNxBgpGlobalConfiguration().getOrCreateTemplatePeer(name);
    defineStructure(BGP_TEMPLATE_PEER, name, ctx);
  }

  @Override
  public void exitRbnx_template_peer(Rbnx_template_peerContext ctx) {
    _currentBgpNxVrfNeighbor = null;
  }

  @Override
  public void enterRbnx_template_peer_policy(Rbnx_template_peer_policyContext ctx) {
    String name = ctx.policy.getText();
    _currentBgpNxVrfNeighborAddressFamily =
        _configuration.getNxBgpGlobalConfiguration().getOrCreateTemplatePeerPolicy(name);
    defineStructure(BGP_TEMPLATE_PEER_POLICY, name, ctx);
  }

  @Override
  public void exitRbnx_template_peer_policy(Rbnx_template_peer_policyContext ctx) {
    _currentBgpNxVrfNeighborAddressFamily = null;
  }

  @Override
  public void enterRbnx_template_peer_session(Rbnx_template_peer_sessionContext ctx) {
    String name = ctx.session.getText();
    _currentBgpNxVrfNeighbor =
        _configuration.getNxBgpGlobalConfiguration().getOrCreateTemplatePeerSession(name);
    defineStructure(BGP_TEMPLATE_PEER_SESSION, name, ctx);
  }

  @Override
  public void exitRbnx_template_peer_session(Rbnx_template_peer_sessionContext ctx) {
    _currentBgpNxVrfNeighbor = null;
  }

  @Override
  public void enterRbnx_vrf(Rbnx_vrfContext ctx) {
    String vrfName = ctx.name.getText();
    Vrf vrf = initVrf(vrfName);
    _currentBgpNxVrfConfiguration = vrf.getBgpNxConfig();
    if (_currentBgpNxVrfConfiguration == null) {
      _currentBgpNxVrfConfiguration = new CiscoNxBgpVrfConfiguration();
      vrf.setBgpNxConfig(_currentBgpNxVrfConfiguration);
    }
  }

  @Override
  public void exitRbnx_vrf(Rbnx_vrfContext ctx) {
    _currentBgpNxVrfConfiguration = _configuration.getDefaultVrf().getBgpNxConfig();
  }

  @Override
  public void exitRbnx_v_local_as(Rbnx_v_local_asContext ctx) {
    long asNum = toAsNum(ctx.bgp_asn());
    _currentBgpNxVrfConfiguration.setLocalAs(asNum);
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
    _currentVrf = ctx.name.getText();
    OspfProcess proc = currentVrf().getOspfProcess();
    if (proc == null) {
      proc = new OspfProcess(_currentOspfProcess.getName(), _format);
      currentVrf().setOspfProcess(proc);
      proc.setRouterId(routerId);
    }
    _currentOspfProcess = proc;
  }

  @Override
  public void enterRoa_interface(Roa_interfaceContext ctx) {
    String ifaceName = ctx.iname.getText();
    String canonicalIfaceName = getCanonicalInterfaceName(ifaceName);
    Interface iface = _configuration.getInterfaces().get(canonicalIfaceName);
    if (iface == null) {
      _w.redFlag("OSPF: Interface: '" + ifaceName + "' not declared before OSPF process");
      iface = addInterface(canonicalIfaceName, ctx.iname, false);
    }
    // might cause problems if interfaces are declared after ospf, but
    // whatever
    for (InterfaceAddress address : iface.getAllAddresses()) {
      Prefix prefix = address.getPrefix();
      OspfNetwork network = new OspfNetwork(prefix, _currentOspfArea);
      _currentOspfProcess.getNetworks().add(network);
    }
    _currentOspfInterface = iface.getName();
  }

  @Override
  public void enterRoute_map_stanza(Route_map_stanzaContext ctx) {
    String name = ctx.name.getText();
    RouteMap routeMap = _configuration.getRouteMaps().computeIfAbsent(name, RouteMap::new);
    _currentRouteMap = routeMap;
    int num = toInteger(ctx.num);
    LineAction action = toLineAction(ctx.rmt);
    RouteMapClause clause = _currentRouteMap.getClauses().get(num);
    if (clause == null) {
      clause = new RouteMapClause(action, name, num);
      routeMap.getClauses().put(num, clause);
    } else {
      _w.redFlag(
          "Route map '"
              + _currentRouteMap.getName()
              + "' already contains clause numbered '"
              + num
              + "'. Duplicate clause will be merged with original clause.");
    }
    _currentRouteMapClause = clause;
    defineStructure(ROUTE_MAP, name, ctx);
  }

  @Override
  public void enterRoute_policy_stanza(Route_policy_stanzaContext ctx) {
    String name = ctx.name.getText();
    _currentRoutePolicy = _configuration.getRoutePolicies().computeIfAbsent(name, RoutePolicy::new);

    List<RoutePolicyStatement> stmts = _currentRoutePolicy.getStatements();

    stmts.addAll(toRoutePolicyStatementList(ctx.stanzas));
    defineStructure(ROUTE_POLICY, name, ctx);
  }

  @Override
  public void enterRouter_bgp_stanza(Router_bgp_stanzaContext ctx) {
    int procNum = (ctx.procnum == null) ? 0 : toInteger(ctx.procnum);
    Vrf vrf = _configuration.getVrfs().get(Configuration.DEFAULT_VRF_NAME);

    if (_parser.getParser().isNxos()) {
      _currentBgpNxVrfConfiguration = vrf.getBgpNxConfig();
      if (_currentBgpNxVrfConfiguration == null) {
        _currentBgpNxVrfConfiguration = new CiscoNxBgpVrfConfiguration();
        vrf.setBgpNxConfig(_currentBgpNxVrfConfiguration);
      }
      _configuration.getNxBgpGlobalConfiguration().setLocalAs(procNum);
    } else {
      BgpProcess proc = new BgpProcess(_format, procNum);
      vrf.setBgpProcess(proc);
      _dummyPeerGroup = new MasterBgpPeerGroup();
      pushPeer(proc.getMasterBgpPeerGroup());
    }
  }

  @Override
  public void enterRe_classic(Re_classicContext ctx) {
    // Create a classic EIGRP process with ASN
    long asn = toLong(ctx.asnum);
    EigrpProcess proc = new EigrpProcess(asn, EigrpProcessMode.CLASSIC);
    currentVrf().setEigrpProcess(proc);
    _currentEigrpProcess = proc;
  }

  @Override
  public void enterReaf_interface(Reaf_interfaceContext ctx) {
    _currentEigrpInterface = getOrAddInterface(ctx.iname);
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
  public void enterS_cable(S_cableContext ctx) {
    if (_configuration.getCf().getCable() == null) {
      _configuration.getCf().setCable(new Cable());
    }
  }

  @Override
  public void enterS_class_map(S_class_mapContext ctx) {
    // TODO: do something with this.
    String name = ctx.name.getText();
    defineStructure(CLASS_MAP, name, ctx);
  }

  @Override
  public void enterS_depi_class(S_depi_classContext ctx) {
    String name = ctx.name.getText();
    _configuration.getCf().getDepiClasses().computeIfAbsent(name, DepiClass::new);
    defineStructure(DEPI_CLASS, name, ctx);
  }

  @Override
  public void enterS_depi_tunnel(S_depi_tunnelContext ctx) {
    String name = ctx.name.getText();
    _configuration.getCf().getDepiTunnels().computeIfAbsent(name, DepiTunnel::new);
    defineStructure(DEPI_TUNNEL, name, ctx);
  }

  @Override
  public void enterS_interface(S_interfaceContext ctx) {
    String nameAlpha = ctx.iname.name_prefix_alpha.getText();
    String canonicalNamePrefix;
    try {
      canonicalNamePrefix = CiscoConfiguration.getCanonicalInterfaceNamePrefix(nameAlpha);
    } catch (BatfishException e) {
      _w.redFlag(
          "Error fetching interface name at: "
              + getLocation(ctx)
              + getFullText(ctx)
              + " : "
              + e.getMessage());
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
          defineStructure(INTERFACE, name, ctx);
          _configuration.referenceStructure(
              INTERFACE, name, INTERFACE_SELF_REF, ctx.getStart().getLine());
        }
      }
    } else {
      addInterface(namePrefix.toString(), ctx.iname, true);
    }
    if (ctx.MULTIPOINT() != null) {
      todo(ctx, F_INTERFACE_MULTIPOINT);
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
    defineStructure(L2TP_CLASS, name, ctx);
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
    defineStructure(MAC_ACCESS_LIST, name, ctx);
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
    defineStructure(MAC_ACCESS_LIST, name, ctx);
  }

  @Override
  public void enterS_ntp(S_ntpContext ctx) {
    if (_configuration.getCf().getNtp() == null) {
      _configuration.getCf().setNtp(new Ntp());
    }
  }

  @Override
  public void enterS_policy_map(S_policy_mapContext ctx) {
    // TODO: do something with this.
    if (ctx.variable_policy_map_header() != null) {
      String name = ctx.variable_policy_map_header().getText();
      defineStructure(POLICY_MAP, name, ctx);
    }
  }

  @Override
  public void enterS_router_ospf(S_router_ospfContext ctx) {
    String procName = ctx.name.getText();
    if (ctx.vrf != null) {
      _currentVrf = ctx.vrf.getText();
    }
    OspfProcess proc = new OspfProcess(procName, _format);
    currentVrf().setOspfProcess(proc);
    _currentOspfProcess = proc;
  }

  @Override
  public void enterS_service_template(S_service_templateContext ctx) {
    // TODO: do something with this.
    String name = ctx.name.getText();
    defineStructure(SERVICE_TEMPLATE, name, ctx);
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
      /* Different OSes interpret the units on DEC differently. */
      if (_format == CISCO_NX) {
        referenceBandwidth = referenceBandwidthDec * 1_000_000_000;
      } else {
        referenceBandwidth = referenceBandwidthDec * 1_000_000;
      }
    }
    _currentOspfProcess.setReferenceBandwidth(referenceBandwidth);
  }

  @Override
  public void exitRo_max_metric(Ro_max_metricContext ctx) {
    if (ctx.on_startup != null || ctx.wait_for_bgp != null) {
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
  public void enterS_vrf_context(S_vrf_contextContext ctx) {
    _currentVrf = ctx.name.getText();
  }

  @Override
  public void enterS_vrf_definition(S_vrf_definitionContext ctx) {
    _currentVrf = ctx.name.getText();
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
    int definitionLine = ctx.name.getStart().getLine();
    BgpProcess proc = currentVrf().getBgpProcess();
    _currentPeerSession = proc.getPeerSessions().get(name);
    if (_currentPeerSession == null) {
      proc.addPeerSession(name, definitionLine);
      _currentPeerSession = proc.getPeerSessions().get(name);
    }
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
  public void enterStandard_access_list_stanza(Standard_access_list_stanzaContext ctx) {
    String name;
    if (ctx.name != null) {
      name = ctx.name.getText();
    } else if (ctx.num != null) {
      name = ctx.num.getText();
    } else {
      throw new BatfishException("Invalid standard access-list name");
    }
    _currentStandardAcl =
        _configuration.getStandardAcls().computeIfAbsent(name, StandardAccessList::new);
    defineStructure(IPV4_ACCESS_LIST_STANDARD, name, ctx);
  }

  @Override
  public void enterStandard_ipv6_access_list_stanza(Standard_ipv6_access_list_stanzaContext ctx) {
    String name;
    if (ctx.name != null) {
      name = ctx.name.getText();
    } else {
      throw new BatfishException("Invalid standard access-list name");
    }
    _currentStandardIpv6Acl =
        _configuration.getStandardIpv6Acls().computeIfAbsent(name, StandardIpv6AccessList::new);
    defineStructure(IPV6_ACCESS_LIST_STANDARD, name, ctx);
  }

  @Override
  public void enterTemplate_peer_policy_rb_stanza(Template_peer_policy_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    int definitionLine = ctx.name.getLine();
    BgpProcess proc = currentVrf().getBgpProcess();
    _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
    if (_currentNamedPeerGroup == null) {
      proc.addNamedPeerGroup(name, definitionLine);
      _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
    }
    pushPeer(_currentNamedPeerGroup);
  }

  @Override
  public void enterTemplate_peer_rb_stanza(Template_peer_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    int definitionLine = ctx.name.getLine();
    BgpProcess proc = currentVrf().getBgpProcess();
    _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
    if (_currentNamedPeerGroup == null) {
      proc.addNamedPeerGroup(name, definitionLine);
      _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
    }
    pushPeer(_currentNamedPeerGroup);
  }

  @Override
  public void enterTemplate_peer_session_rb_stanza(Template_peer_session_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    int definitionLine = ctx.name.getLine();
    BgpProcess proc = currentVrf().getBgpProcess();
    _currentPeerSession = proc.getPeerSessions().get(name);
    if (_currentPeerSession == null) {
      proc.addPeerSession(name, definitionLine);
      _currentPeerSession = proc.getPeerSessions().get(name);
    }
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
    int procNum =
        _configuration.getVrfs().get(Configuration.DEFAULT_VRF_NAME).getBgpProcess().getProcnum();
    BgpProcess proc = new BgpProcess(_format, procNum);
    currentVrf().setBgpProcess(proc);
    pushPeer(proc.getMasterBgpPeerGroup());
    _currentBlockNeighborAddressFamilies.clear();
    _inBlockNeighbor = false;
  }

  @Override
  public void enterVrrp_interface(Vrrp_interfaceContext ctx) {
    String ifaceName = ctx.iface.getText();
    _currentVrrpInterface = getCanonicalInterfaceName(ifaceName);
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
  public void exitRec_address_family(Rec_address_familyContext ctx) {
    Objects.requireNonNull(_currentEigrpProcess)
        .computeNetworks(_configuration.getInterfaces().values());
    _currentEigrpProcess = _parentEigrpProcess;
    _parentEigrpProcess = null;
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
  }

  @Override
  public void exitRen_address_family(Ren_address_familyContext ctx) {
    // Check if address family was supported
    if (_currentEigrpProcess != null) {
      _currentEigrpProcess.computeNetworks(_configuration.getInterfaces().values());
    }
    _currentEigrpProcess = null;
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
  }

  @Override
  public void exitAddress_family_rb_stanza(Address_family_rb_stanzaContext ctx) {
    popPeer();
  }

  @Override
  public void exitAdvertise_map_bgp_tail(Advertise_map_bgp_tailContext ctx) {
    // TODO: https://github.com/batfish/batfish/issues/1836
    String advertiseMapName = ctx.am_name.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, advertiseMapName, BGP_ROUTE_MAP_ADVERTISE, ctx.am_name.getStart().getLine());
    if (ctx.em_name != null) {
      String existMapName = ctx.em_name.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, existMapName, BGP_ADVERTISE_MAP_EXIST_MAP, ctx.em_name.getStart().getLine());
    }
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
          prefix = new Prefix(network, prefixLength);
        } else {
          // ctx.prefix != null
          prefix = Prefix.parse(ctx.prefix.getText());
        }
        BgpAggregateIpv4Network net = new BgpAggregateIpv4Network(prefix);
        net.setAsSet(asSet);
        net.setSummaryOnly(summaryOnly);
        if (ctx.mapname != null) {
          String mapName = ctx.mapname.getText();
          net.setAttributeMap(mapName);
          _configuration.referenceStructure(
              ROUTE_MAP, mapName, BGP_AGGREGATE_ATTRIBUTE_MAP, ctx.mapname.getStart().getLine());
        } else if (ctx.rp != null) {
          String policyName = ctx.rp.getText();
          net.setAttributeMap(policyName);
          _configuration.referenceStructure(
              ROUTE_POLICY, policyName, BGP_AGGREGATE_ROUTE_POLICY, ctx.rp.getStart().getLine());
        }
        proc.getAggregateNetworks().put(prefix, net);
      } else if (ctx.ipv6_prefix != null) {
        // ipv6
        Prefix6 prefix6 = new Prefix6(ctx.ipv6_prefix.getText());
        BgpAggregateIpv6Network net = new BgpAggregateIpv6Network(prefix6);
        net.setAsSet(asSet);
        net.setSummaryOnly(summaryOnly);
        if (ctx.mapname != null) {
          String mapName = ctx.mapname.getText();
          net.setAttributeMap(mapName);
          _configuration.referenceStructure(
              ROUTE_MAP, mapName, BGP_AGGREGATE_ATTRIBUTE_MAP, ctx.mapname.getStart().getLine());
        }
        proc.getAggregateIpv6Networks().put(prefix6, net);
      }
    } else if (_currentIpPeerGroup != null
        || _currentIpv6PeerGroup != null
        || _currentDynamicIpPeerGroup != null
        || _currentDynamicIpv6PeerGroup != null
        || _currentNamedPeerGroup != null) {
      throw new BatfishException("unexpected occurrence in peer group/neighbor context");

    } else if (ctx.mapname != null) {
      String map = ctx.mapname.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, map, BGP_VRF_AGGREGATE_ROUTE_MAP, ctx.mapname.getStart().getLine());
    }
  }

  @Override
  public void exitAllowas_in_bgp_tail(Allowas_in_bgp_tailContext ctx) {
    _currentPeerGroup.setAllowAsIn(true);
    if (ctx.num != null) {
      todo(ctx, F_ALLOWAS_IN_NUMBER);
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
      _w.redFlag("Redeclaration of as-path-set: '" + name + "'");
    }
    asPathSet = new AsPathSet(name);
    _configuration.getAsPathSets().put(name, asPathSet);
    for (As_path_set_elemContext elemCtx : ctx.elems) {
      AsPathSetElem elem = toAsPathSetElem(elemCtx);
      asPathSet.getElements().add(elem);
    }
    defineStructure(AS_PATH_SET, name, ctx);
  }

  @Override
  public void exitAuto_summary_bgp_tail(Auto_summary_bgp_tailContext ctx) {
    todo(ctx, F_BGP_AUTO_SUMMARY);
  }

  @Override
  public void exitBanner_stanza(Banner_stanzaContext ctx) {
    String bannerType = ctx.banner_type().getText();
    String message = ctx.banner().getText();
    _configuration
        .getCf()
        .getBanners()
        .compute(bannerType, (k, v) -> v == null ? message : v + "\n" + message);
  }

  @Override
  public void exitBgp_advertise_inactive_rb_stanza(Bgp_advertise_inactive_rb_stanzaContext ctx) {
    _currentPeerGroup.setAdvertiseInactive(true);
  }

  @Override
  public void exitBgp_listen_range_rb_stanza(Bgp_listen_range_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    BgpProcess proc = currentVrf().getBgpProcess();
    if (ctx.IP_PREFIX() != null) {
      Prefix prefix = Prefix.parse(ctx.IP_PREFIX().getText());
      DynamicIpBgpPeerGroup pg = proc.addDynamicIpPeerGroup(prefix);
      pg.setGroupName(name);
      pg.setGroupNameLine(line);
      if (ctx.as != null) {
        long remoteAs = toLong(ctx.as);
        pg.setRemoteAs(remoteAs);
      }
    } else if (ctx.IPV6_PREFIX() != null) {
      Prefix6 prefix6 = new Prefix6(ctx.IPV6_PREFIX().getText());
      DynamicIpv6BgpPeerGroup pg = proc.addDynamicIpv6PeerGroup(prefix6);
      pg.setGroupName(name);
      pg.setGroupNameLine(line);
      if (ctx.as != null) {
        long remoteAs = toLong(ctx.as);
        pg.setRemoteAs(remoteAs);
      }
    }
  }

  @Override
  public void exitCadant_stdacl_name(Cadant_stdacl_nameContext ctx) {
    String name = ctx.name.getText();
    _configuration.getStandardAcls().put(name, _currentStandardAcl);
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
  public void exitClbdg_docsis_policy(Clbdg_docsis_policyContext ctx) {
    String name = ctx.policy.getText();
    _configuration.referenceStructure(
        DOCSIS_POLICY, name, DOCSIS_GROUP_DOCSIS_POLICY, ctx.getStart().getLine());
  }

  @Override
  public void exitCluster_id_bgp_tail(Cluster_id_bgp_tailContext ctx) {
    Ip clusterId = null;
    if (ctx.DEC() != null) {
      long ipAsLong = toLong(ctx.DEC());
      clusterId = new Ip(ipAsLong);
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
    defineStructure(INSPECT_CLASS_MAP, name, ctx);
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
  public void exitS_zone(S_zoneContext ctx) {
    String name = ctx.name.getText();
    _configuration.getSecurityZones().computeIfAbsent(name, SecurityZone::new);
    defineStructure(SECURITY_ZONE, name, ctx);
  }

  @Override
  public void enterS_zone_pair(S_zone_pairContext ctx) {
    String name = ctx.name.getText();
    String srcName = ctx.source.getText();
    int srcLine = ctx.source.getStart().getLine();
    _configuration.referenceStructure(SECURITY_ZONE, srcName, ZONE_PAIR_SOURCE_ZONE, srcLine);
    String dstName = ctx.destination.getText();
    int dstLine = ctx.destination.getStart().getLine();
    _configuration.referenceStructure(SECURITY_ZONE, dstName, ZONE_PAIR_DESTINATION_ZONE, dstLine);
    defineStructure(CiscoStructureType.SECURITY_ZONE_PAIR, name, ctx);
    _currentSecurityZonePair =
        _configuration
            .getSecurityZonePairs()
            .computeIfAbsent(dstName, n -> new TreeMap<>())
            .computeIfAbsent(srcName, n -> new SecurityZonePair(name, srcName, dstName));
  }

  @Override
  public void exitS_zone_pair(S_zone_pairContext ctx) {
    _currentSecurityZonePair = null;
  }

  @Override
  public void exitZp_service_policy_inspect(Zp_service_policy_inspectContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(
        INSPECT_POLICY_MAP, name, ZONE_PAIR_INSPECT_SERVICE_POLICY, line);
    _currentSecurityZonePair.setInspectPolicyMap(name);
  }

  @Override
  public void exitIf_zone_member(If_zone_memberContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(SECURITY_ZONE, name, INTERFACE_ZONE_MEMBER, line);
    _currentInterfaces.forEach(iface -> iface.setSecurityZone(name));
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
      _w.redFlag("Class-map match unsupported " + getFullText(ctx));
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
      todo(ctx, F_CM_MATCH_NOT);
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
  public void exitCntlr_rf_channel(Cntlr_rf_channelContext ctx) {
    _no = false;
  }

  @Override
  public void exitCntlrrfc_depi_tunnel(Cntlrrfc_depi_tunnelContext ctx) {
    if (!_no) {
      String name = ctx.name.getText();
      int line = ctx.getStart().getLine();
      _configuration.referenceStructure(DEPI_TUNNEL, name, CONTROLLER_DEPI_TUNNEL, line);
    }
  }

  @Override
  public void exitCompare_routerid_rb_stanza(Compare_routerid_rb_stanzaContext ctx) {
    currentVrf().getBgpProcess().setTieBreaker(BgpTieBreaker.ROUTER_ID);
  }

  @Override
  public void exitContinue_rm_stanza(Continue_rm_stanzaContext ctx) {
    int statementLine = ctx.getStart().getLine();
    Integer target = null;
    if (ctx.DEC() != null) {
      target = toInteger(ctx.DEC());
    }
    RouteMapContinue continueLine = new RouteMapContinue(target, statementLine);
    _currentRouteMapClause.setContinueLine(continueLine);
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
  public void exitCqer_service_class(Cqer_service_classContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _configuration.referenceStructure(SERVICE_CLASS, name, QOS_ENFORCE_RULE_SERVICE_CLASS, line);
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
  public void exitCs_class(Cs_classContext ctx) {
    _currentServiceClass = null;
  }

  @Override
  public void exitCsc_name(Csc_nameContext ctx) {
    String name = ctx.name.getText();
    _configuration.getCf().getCable().getServiceClassesByName().put(name, _currentServiceClass);
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
    if (ctx.map != null) {
      String mapName = ctx.map.getText();
      int line = ctx.map.getStart().getLine();
      _currentPeerGroup.setDefaultOriginateMap(mapName);
      _currentPeerGroup.setDefaultOriginateMapLine(line);
      _configuration.referenceStructure(ROUTE_MAP, mapName, BGP_DEFAULT_ORIGINATE_ROUTE_MAP, line);
    }
  }

  @Override
  public void exitDefault_shutdown_bgp_tail(Default_shutdown_bgp_tailContext ctx) {
    _currentPeerGroup.setShutdown(true);
  }

  @Override
  public void exitDescription_bgp_tail(Description_bgp_tailContext ctx) {
    String description = ctx.description_line().text.getText().trim();
    _currentPeerGroup.setDescription(description);
  }

  @Override
  public void exitDisable_peer_as_check_bgp_tail(Disable_peer_as_check_bgp_tailContext ctx) {
    _currentPeerGroup.setDisablePeerAsCheck(true);
  }

  @Override
  public void exitDistribute_list_bgp_tail(Distribute_list_bgp_tailContext ctx) {
    todo(ctx, F_BGP_NEIGHBOR_DISTRIBUTE_LIST);
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
      String ifaceName = ctx.iname.getText();
      String canonicalIfaceName = getCanonicalInterfaceName(ifaceName);
      _configuration.setDnsSourceInterface(canonicalIfaceName);
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
  public void exitDt_protect_tunnel(Dt_protect_tunnelContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _configuration.referenceStructure(DEPI_TUNNEL, name, DEPI_TUNNEL_PROTECT_TUNNEL, line);
  }

  @Override
  public void exitEbgp_multihop_bgp_tail(Ebgp_multihop_bgp_tailContext ctx) {
    _currentPeerGroup.setEbgpMultihop(true);
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
  public void exitExtended_access_list_stanza(Extended_access_list_stanzaContext ctx) {
    _currentExtendedAcl = null;
  }

  @Override
  public void exitExtended_access_list_tail(Extended_access_list_tailContext ctx) {
    LineAction action = toLineAction(ctx.ala);
    AccessListAddressSpecifier srcAddressSpecifier = toAccessListAddressSpecifier(ctx.srcipr);
    AccessListAddressSpecifier dstAddressSpecifier = toAccessListAddressSpecifier(ctx.dstipr);
    AccessListServiceSpecifier serviceSpecifier = computeExtendedAccessListServiceSpecifier(ctx);
    String name = getFullText(ctx).trim();
    ExtendedAccessListLine line =
        ExtendedAccessListLine.builder()
            .setAction(action)
            .setDstAddressSpecifier(dstAddressSpecifier)
            .setName(name)
            .setServiceSpecifier(serviceSpecifier)
            .setSrcAddressSpecifier(srcAddressSpecifier)
            .build();
    _currentExtendedAcl.addLine(line);
  }

  private AccessListServiceSpecifier computeExtendedAccessListServiceSpecifier(
      Extended_access_list_tailContext ctx) {
    if (ctx.prot != null) {
      IpProtocol protocol = toIpProtocol(ctx.prot);
      List<SubRange> srcPortRanges =
          ctx.alps_src != null ? toPortRanges(ctx.alps_src) : Collections.emptyList();
      List<SubRange> dstPortRanges =
          ctx.alps_dst != null ? toPortRanges(ctx.alps_dst) : Collections.emptyList();
      Integer icmpType = null;
      Integer icmpCode = null;
      List<TcpFlags> tcpFlags = new ArrayList<>();
      Set<Integer> dscps = new TreeSet<>();
      Set<Integer> ecns = new TreeSet<>();
      Set<State> states = EnumSet.noneOf(State.class);
      for (Extended_access_list_additional_featureContext feature : ctx.features) {
        if (feature.ACK() != null) {
          TcpFlags alt = new TcpFlags();
          alt.setUseAck(true);
          alt.setAck(true);
          tcpFlags.add(alt);
        }
        if (feature.DSCP() != null) {
          int dscpType = toDscpType(feature.dscp_type());
          dscps.add(dscpType);
        }
        if (feature.ECE() != null) {
          TcpFlags alt = new TcpFlags();
          alt.setUseEce(true);
          alt.setEce(true);
          tcpFlags.add(alt);
        }
        if (feature.ECHO_REPLY() != null) {
          icmpType = IcmpType.ECHO_REPLY;
          icmpCode = IcmpCode.ECHO_REPLY;
        }
        if (feature.ECHO() != null) {
          icmpType = IcmpType.ECHO_REQUEST;
          icmpCode = IcmpCode.ECHO_REQUEST;
        }
        if (feature.ECN() != null) {
          int ecn = toInteger(feature.ecn);
          ecns.add(ecn);
        }
        if (feature.ESTABLISHED() != null) {
          // must contain ACK or RST
          TcpFlags alt1 = new TcpFlags();
          TcpFlags alt2 = new TcpFlags();
          alt1.setUseAck(true);
          alt1.setAck(true);
          alt2.setUseRst(true);
          alt2.setRst(true);
          tcpFlags.add(alt1);
          tcpFlags.add(alt2);
        }
        if (feature.FIN() != null) {
          TcpFlags alt = new TcpFlags();
          alt.setUseFin(true);
          alt.setFin(true);
          tcpFlags.add(alt);
        }
        if (feature.FRAGMENTS() != null) {
          todo(ctx, F_FRAGMENTS);
        }
        if (feature.HOST_UNKNOWN() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_HOST_UNKNOWN;
        }
        if (feature.HOST_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_HOST_UNREACHABLE;
        }
        if (feature.NETWORK_UNKNOWN() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_NETWORK_UNKNOWN;
        }
        if (feature.NET_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_NETWORK_UNREACHABLE;
        }
        if (feature.PACKET_TOO_BIG() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.PACKET_TOO_BIG;
        }
        if (feature.PARAMETER_PROBLEM() != null) {
          icmpType = IcmpType.PARAMETER_PROBLEM;
        }
        if (feature.PORT_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_PORT_UNREACHABLE;
        }
        if (feature.PSH() != null) {
          TcpFlags alt = new TcpFlags();
          alt.setUsePsh(true);
          alt.setPsh(true);
          tcpFlags.add(alt);
        }
        if (feature.REDIRECT() != null) {
          icmpType = IcmpType.REDIRECT_MESSAGE;
        }
        if (feature.RST() != null) {
          TcpFlags alt = new TcpFlags();
          alt.setUseRst(true);
          alt.setRst(true);
          tcpFlags.add(alt);
        }
        if (feature.SOURCE_QUENCH() != null) {
          icmpType = IcmpType.SOURCE_QUENCH;
          icmpCode = IcmpCode.SOURCE_QUENCH;
        }
        if (feature.SYN() != null) {
          TcpFlags alt = new TcpFlags();
          alt.setUseSyn(true);
          alt.setSyn(true);
          tcpFlags.add(alt);
        }
        if (feature.TIME_EXCEEDED() != null) {
          icmpType = IcmpType.TIME_EXCEEDED;
        }
        if (feature.TTL() != null) {
          todo(ctx, F_TTL);
        }
        if (feature.TTL_EXCEEDED() != null) {
          icmpType = IcmpType.TIME_EXCEEDED;
          icmpCode = IcmpCode.TTL_EXCEEDED;
        }
        if (feature.TRACEROUTE() != null) {
          icmpType = IcmpType.TRACEROUTE;
          icmpCode = IcmpCode.TRACEROUTE;
        }
        if (feature.TRACKED() != null) {
          states.add(State.ESTABLISHED);
        }
        if (feature.UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
        }
        if (feature.URG() != null) {
          TcpFlags alt = new TcpFlags();
          alt.setUseUrg(true);
          alt.setUrg(true);
          tcpFlags.add(alt);
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
          .setStates(states)
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
    } else {
      throw convError(AccessListServiceSpecifier.class, ctx);
    }
  }

  private AccessListAddressSpecifier toAccessListAddressSpecifier(Access_list_ip_rangeContext ctx) {
    if (ctx.ip != null) {
      if (ctx.wildcard != null) {
        // IP and mask
        Ip wildcard = toIp(ctx.wildcard);
        if (_format == CISCO_ASA) {
          wildcard = wildcard.inverted();
        }
        return new WildcardAddressSpecifier(new IpWildcard(toIp(ctx.ip), wildcard));
      } else {
        // Just IP. Same as if 'host' was specified
        return new WildcardAddressSpecifier(new IpWildcard(toIp(ctx.ip)));
      }
    } else if (ctx.ANY() != null || ctx.ANY4() != null) {
      return new WildcardAddressSpecifier(IpWildcard.ANY);
    } else if (ctx.prefix != null) {
      return new WildcardAddressSpecifier(new IpWildcard(Prefix.parse(ctx.prefix.getText())));
    } else if (ctx.address_group != null) {
      todo(ctx, F_ACL_ADDRESS_GROUP);
      return new WildcardAddressSpecifier(IpWildcard.ANY);
    } else if (ctx.iface != null) {
      todo(ctx, F_ACL_INTERFACE);
      return new WildcardAddressSpecifier(IpWildcard.ANY);
    } else if (ctx.obj != null) {
      todo(ctx, F_ACL_OBJECT);
      return new WildcardAddressSpecifier(IpWildcard.ANY);
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
  public void exitExtended_ipv6_access_list_stanza(Extended_ipv6_access_list_stanzaContext ctx) {
    _currentExtendedIpv6Acl = null;
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
    List<TcpFlags> tcpFlags = new ArrayList<>();
    Set<Integer> dscps = new TreeSet<>();
    Set<Integer> ecns = new TreeSet<>();
    Set<State> states = EnumSet.noneOf(State.class);
    for (Extended_access_list_additional_featureContext feature : ctx.features) {
      if (feature.ACK() != null) {
        TcpFlags alt = new TcpFlags();
        alt.setUseAck(true);
        alt.setAck(true);
        tcpFlags.add(alt);
      }
      if (feature.DSCP() != null) {
        int dscpType = toDscpType(feature.dscp_type());
        dscps.add(dscpType);
      }
      if (feature.ECE() != null) {
        TcpFlags alt = new TcpFlags();
        alt.setUseEce(true);
        alt.setEce(true);
        tcpFlags.add(alt);
      }
      if (feature.ECHO_REPLY() != null) {
        icmpType = IcmpType.ECHO_REPLY;
        icmpCode = IcmpCode.ECHO_REPLY;
      }
      if (feature.ECHO() != null) {
        icmpType = IcmpType.ECHO_REQUEST;
        icmpCode = IcmpCode.ECHO_REQUEST;
      }
      if (feature.ECN() != null) {
        int ecn = toInteger(feature.ecn);
        ecns.add(ecn);
      }
      if (feature.ESTABLISHED() != null) {
        // must contain ACK or RST
        TcpFlags alt1 = new TcpFlags();
        TcpFlags alt2 = new TcpFlags();
        alt1.setUseAck(true);
        alt1.setAck(true);
        alt2.setUseRst(true);
        alt2.setRst(true);
        tcpFlags.add(alt1);
        tcpFlags.add(alt2);
      }
      if (feature.FIN() != null) {
        TcpFlags alt = new TcpFlags();
        alt.setUseFin(true);
        alt.setFin(true);
        tcpFlags.add(alt);
      }
      if (feature.FRAGMENTS() != null) {
        todo(ctx, F_FRAGMENTS);
      }
      if (feature.HOST_UNKNOWN() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.DESTINATION_HOST_UNKNOWN;
      }
      if (feature.HOST_UNREACHABLE() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.DESTINATION_HOST_UNREACHABLE;
      }
      if (feature.NETWORK_UNKNOWN() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.DESTINATION_NETWORK_UNKNOWN;
      }
      if (feature.NET_UNREACHABLE() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.DESTINATION_NETWORK_UNREACHABLE;
      }
      if (feature.PACKET_TOO_BIG() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.PACKET_TOO_BIG;
      }
      if (feature.PARAMETER_PROBLEM() != null) {
        icmpType = IcmpType.PARAMETER_PROBLEM;
      }
      if (feature.PORT_UNREACHABLE() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
        icmpCode = IcmpCode.DESTINATION_PORT_UNREACHABLE;
      }
      if (feature.PSH() != null) {
        TcpFlags alt = new TcpFlags();
        alt.setUsePsh(true);
        alt.setPsh(true);
        tcpFlags.add(alt);
      }
      if (feature.REDIRECT() != null) {
        icmpType = IcmpType.REDIRECT_MESSAGE;
      }
      if (feature.RST() != null) {
        TcpFlags alt = new TcpFlags();
        alt.setUseRst(true);
        alt.setRst(true);
        tcpFlags.add(alt);
      }
      if (feature.SOURCE_QUENCH() != null) {
        icmpType = IcmpType.SOURCE_QUENCH;
        icmpCode = IcmpCode.SOURCE_QUENCH;
      }
      if (feature.SYN() != null) {
        TcpFlags alt = new TcpFlags();
        alt.setUseSyn(true);
        alt.setSyn(true);
        tcpFlags.add(alt);
      }
      if (feature.TIME_EXCEEDED() != null) {
        icmpType = IcmpType.TIME_EXCEEDED;
      }
      if (feature.TTL() != null) {
        todo(ctx, F_TTL);
      }
      if (feature.TTL_EXCEEDED() != null) {
        icmpType = IcmpType.TIME_EXCEEDED;
        icmpCode = IcmpCode.TTL_EXCEEDED;
      }
      if (feature.TRACEROUTE() != null) {
        icmpType = IcmpType.TRACEROUTE;
        icmpCode = IcmpCode.TRACEROUTE;
      }
      if (feature.TRACKED() != null) {
        states.add(State.ESTABLISHED);
      }
      if (feature.UNREACHABLE() != null) {
        icmpType = IcmpType.DESTINATION_UNREACHABLE;
      }
      if (feature.URG() != null) {
        TcpFlags alt = new TcpFlags();
        alt.setUseUrg(true);
        alt.setUrg(true);
        tcpFlags.add(alt);
      }
    }
    String name = getFullText(ctx).trim();
    ExtendedIpv6AccessListLine line =
        new ExtendedIpv6AccessListLine(
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
            states,
            tcpFlags);
    _currentExtendedIpv6Acl.addLine(line);
  }

  @Override
  public void exitFailover_interface(Failover_interfaceContext ctx) {
    String name = ctx.name.getText();
    Ip primaryIp = toIp(ctx.pip);
    Ip primaryMask = toIp(ctx.pmask);
    Ip standbyIp = toIp(ctx.sip);
    InterfaceAddress primaryAddress = new InterfaceAddress(primaryIp, primaryMask);
    InterfaceAddress standbyAddress = new InterfaceAddress(standbyIp, primaryMask);
    _configuration.getFailoverPrimaryAddresses().put(name, primaryAddress);
    _configuration.getFailoverStandbyAddresses().put(name, standbyAddress);
  }

  @Override
  public void exitFailover_link(Failover_linkContext ctx) {
    String alias = ctx.name.getText();
    String ifaceName = ctx.iface.getText();
    _configuration.getFailoverInterfaces().put(alias, ifaceName);
    _configuration.setFailoverStatefulSignalingInterfaceAlias(alias);
    _configuration.setFailoverStatefulSignalingInterface(ifaceName);
  }

  @Override
  public void exitFilter_list_bgp_tail(Filter_list_bgp_tailContext ctx) {
    String filterList = ctx.num.getText();
    _configuration.referenceStructure(
        AS_PATH_ACCESS_LIST,
        filterList,
        BGP_NEIGHBOR_FILTER_AS_PATH_ACCESS_LIST,
        ctx.getStart().getLine());
    // TODO: Handle filter-list in batfish
  }

  @Override
  public void exitFlan_interface(Flan_interfaceContext ctx) {
    String alias = ctx.name.getText();
    String ifaceName = ctx.iface.getText();
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
  public void exitIf_channel_group(If_channel_groupContext ctx) {
    int num = toInteger(ctx.num);
    String name = computeAggregatedInterfaceName(num, _format);
    _currentInterfaces.forEach(i -> i.setChannelGroup(name));
  }

  @Override
  public void exitIf_crypto_map(If_crypto_mapContext ctx) {
    _currentInterfaces.forEach(i -> i.setCryptoMap(ctx.name.getText()));
  }

  @Override
  public void exitIf_delay(If_delayContext ctx) {
    Double newDelayPs;
    if (ctx.NO() != null) {
      newDelayPs = null;
    } else {
      newDelayPs = toLong(ctx.DEC()) * 1E7;
    }
    _currentInterfaces.forEach(i -> i.setDelay(newDelayPs));
  }

  private @Nullable String computeAggregatedInterfaceName(int num, ConfigurationFormat format) {
    switch (format) {
      case CISCO_ASA:
      case ARISTA:
      case FORCE10:
      case CISCO_IOS:
      case CISCO_NX:
        return String.format("Port-Channel%d", num);

      case CISCO_IOS_XR:
        return String.format("Bundle-Ethernet%d", num);

      default:
        _w.redFlag("Don't know how to compute aggregated-interface name for format: " + format);
        return null;
    }
  }

  @Override
  public void exitIf_ip_access_group(If_ip_access_groupContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    CiscoStructureUsage usage = null;
    if (ctx.IN() != null || ctx.INGRESS() != null) {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setIncomingFilter(name);
        currentInterface.setIncomingFilterLine(line);
        usage = INTERFACE_INCOMING_FILTER;
      }
    } else if (ctx.OUT() != null || ctx.EGRESS() != null) {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setOutgoingFilter(name);
        currentInterface.setOutgoingFilterLine(line);
        usage = INTERFACE_OUTGOING_FILTER;
      }
    } else {
      throw new BatfishException("bad direction");
    }
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, usage, line);
  }

  @Override
  public void exitIf_ip_address(If_ip_addressContext ctx) {
    InterfaceAddress address;
    if (ctx.prefix != null) {
      address = new InterfaceAddress(ctx.prefix.getText());
    } else {
      Ip ip = toIp(ctx.ip);
      Ip mask = toIp(ctx.subnet);
      address = new InterfaceAddress(ip, mask);
    }
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setAddress(address);
    }
    if (ctx.STANDBY() != null) {
      Ip standbyIp = toIp(ctx.standby_address);
      InterfaceAddress standbyAddress = new InterfaceAddress(standbyIp, address.getNetworkBits());
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setStandbyAddress(standbyAddress);
      }
    }
  }

  @Override
  public void exitIf_ip_address_secondary(If_ip_address_secondaryContext ctx) {
    Ip ip;
    Ip mask;
    InterfaceAddress address;
    if (ctx.prefix != null) {
      address = new InterfaceAddress(ctx.prefix.getText());
    } else {
      ip = toIp(ctx.ip);
      mask = toIp(ctx.subnet);
      address = new InterfaceAddress(ip, mask.numSubnetBits());
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
  public void exitIf_ip_nat_destination(If_ip_nat_destinationContext ctx) {
    String acl = ctx.acl.getText();
    int line = ctx.acl.getStart().getLine();
    _configuration.referenceStructure(IPV4_ACCESS_LIST, acl, IP_NAT_DESTINATION_ACCESS_LIST, line);
  }

  @Override
  public void exitIf_ip_nat_source(If_ip_nat_sourceContext ctx) {
    CiscoSourceNat nat = new CiscoSourceNat();
    if (ctx.acl != null) {
      String acl = ctx.acl.getText();
      int aclLine = ctx.acl.getStart().getLine();
      nat.setAclName(acl);
      nat.setAclNameLine(aclLine);
      _configuration.referenceStructure(IPV4_ACCESS_LIST, acl, IP_NAT_SOURCE_ACCESS_LIST, aclLine);
    }
    if (ctx.pool != null) {
      String pool = ctx.pool.getText();
      int poolLine = ctx.pool.getStart().getLine();
      nat.setNatPool(pool);
      nat.setNatPoolLine(poolLine);
      _configuration.referenceStructure(NAT_POOL, pool, IP_NAT_SOURCE_POOL, poolLine);
    }

    for (Interface iface : _currentInterfaces) {
      if (iface.getSourceNats() == null) {
        iface.setSourceNats(new ArrayList<>(1));
      }
      iface.getSourceNats().add(nat);
    }
  }

  @Override
  public void exitIf_ip_ospf_area(If_ip_ospf_areaContext ctx) {
    long area = toInteger(ctx.area);
    for (Interface iface : _currentInterfaces) {
      iface.setOspfArea(area);
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
  public void exitIf_ip_ospf_passive_interface(If_ip_ospf_passive_interfaceContext ctx) {
    boolean active = ctx.NO() != null;
    if (active) {
      for (Interface iface : _currentInterfaces) {
        iface.setOspfActive(true);
      }
    } else {
      for (Interface iface : _currentInterfaces) {
        iface.setOspfPassive(true);
      }
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
  public void exitIf_ip_policy(If_ip_policyContext ctx) {
    String policyName = ctx.name.getText();
    int policyLine = ctx.name.getLine();
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setRoutingPolicy(policyName);
      currentInterface.setRoutingPolicyLine(policyLine);
    }
    _configuration.referenceStructure(
        ROUTE_MAP, policyName, INTERFACE_POLICY_ROUTING_MAP, policyLine);
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
    long area = toIp(ctx.area).asLong();
    for (Interface iface : _currentInterfaces) {
      iface.setOspfArea(area);
    }
  }

  @Override
  public void exitIf_ip_summary_address(If_ip_summary_addressContext ctx) {
    if (ctx.LEAK_MAP() != null) {
      _configuration.referenceStructure(
          ROUTE_MAP,
          ctx.mapname.getText(),
          INTERFACE_SUMMARY_ADDRESS_EIGRP_LEAK_MAP,
          ctx.mapname.getStart().getLine());
    }
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
  public void exitIf_ip_vrf_sitemap(If_ip_vrf_sitemapContext ctx) {
    String map = ctx.map.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, map, INTERFACE_IP_VRF_SITEMAP, ctx.map.getStart().getLine());
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
  public void exitIf_service_policy_control_subscriber(
      If_service_policy_control_subscriberContext ctx) {
    // TODO: do something with this.
    String mapname = ctx.policy_map.getText();
    _configuration.referenceStructure(
        POLICY_MAP, mapname, INTERFACE_SERVICE_POLICY_CONTROL_SUBSCRIBER, ctx.getStart().getLine());
  }

  @Override
  public void exitIf_shutdown(If_shutdownContext ctx) {
    if (ctx.NO() == null) {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setActive(false);
      }
    }
  }

  @Override
  public void exitIf_spanning_tree(If_spanning_treeContext ctx) {
    _no = false;
  }

  @Override
  public void exitIf_speed_eos(If_speed_eosContext ctx) {
    double bandwidth = toBandwidth(ctx.eos_bandwidth_specifier());
    _currentInterfaces.forEach(i -> i.setBandwidth(bandwidth));
  }

  private double toBandwidth(Eos_bandwidth_specifierContext ctx) {
    if (ctx.FORTYG_FULL() != null) {
      return 40E9D;
    } else if (ctx.TEN_THOUSAND_FULL() != null) {
      return 10E9D;
    } else if (ctx.ONE_HUNDRED_FULL() != null) {
      return 100E6D;
    } else if (ctx.ONE_THOUSAND_FULL() != null) {
      return 1E9D;
    } else if (ctx.ONE_HUNDREDG_FULL() != null) {
      return 100E9D;
    } else {
      throw convError(Double.class, ctx);
    }
  }

  @Override
  public void exitIf_speed_ios(If_speed_iosContext ctx) {
    int mbits = toInteger(ctx.mbits);
    _currentInterfaces.forEach(i -> i.setBandwidth(mbits * 1E6D));
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
        iface.setSwitchportMode(SwitchportMode.ACCESS);
        iface.setSwitchport(true);
      }
    }
  }

  @Override
  public void exitIf_switchport_access(If_switchport_accessContext ctx) {
    if (ctx.vlan != null) {
      int vlan = toInteger(ctx.vlan);
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setSwitchportMode(SwitchportMode.ACCESS);
        currentInterface.setAccessVlan(vlan);
      }
    } else {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setSwitchportMode(SwitchportMode.ACCESS);
        currentInterface.setSwitchportAccessDynamic(true);
      }
    }
  }

  @Override
  public void exitIf_switchport_mode(If_switchport_modeContext ctx) {
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
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setSwitchportMode(mode);
    }
  }

  @Override
  public void exitIf_switchport_trunk_allowed(If_switchport_trunk_allowedContext ctx) {
    List<SubRange> ranges = toRange(ctx.r);
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.addAllowedRanges(ranges);
    }
  }

  @Override
  public void exitIf_switchport_trunk_encapsulation(If_switchport_trunk_encapsulationContext ctx) {
    SwitchportEncapsulationType type = toEncapsulation(ctx.e);
    for (Interface currentInterface : _currentInterfaces) {
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
      if (ctx.GRE() != null) {
        tunnel.setMode(TunnelMode.GRE);
      } else if (ctx.IPSEC() != null) {
        tunnel.setMode(TunnelMode.IPSEC);
      } else {
        todo(ctx, "unknown tunnel mode");
      }
      if (ctx.IPV4() != null) {
        tunnel.setProtocol(IpProtocol.IP);
      } else if (ctx.IPV6() != null) {
        tunnel.setProtocol(IpProtocol.IPV6);
      } else {
        todo(ctx, "unknown tunnel protocol");
      }
    }
  }

  @Override
  public void exitIftunnel_protection(Iftunnel_protectionContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _configuration.referenceStructure(IPSEC_PROFILE, name, TUNNEL_PROTECTION_IPSEC_PROFILE, line);
    for (Interface iface : _currentInterfaces) {
      iface.getTunnelInitIfNull().setIpsecProfileName(name);
    }
  }

  @Override
  public void exitIftunnel_source(Iftunnel_sourceContext ctx) {
    Ip sourceAddress = null;
    String sourceInterfaceName = null;
    if (ctx.IP_ADDRESS() != null) {
      sourceAddress = toIp(ctx.IP_ADDRESS());
    } else {
      sourceInterfaceName = ctx.interface_name().getText();
      _configuration.referenceStructure(
          INTERFACE, sourceInterfaceName, TUNNEL_SOURCE, ctx.interface_name().getStart().getLine());
    }
    for (Interface iface : _currentInterfaces) {
      if (sourceAddress != null) {
        iface.getTunnelInitIfNull().setSourceAddress(sourceAddress);
      } else {
        iface.getTunnelInitIfNull().setSourceInterfaceName(sourceInterfaceName);
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
    int line = ctx.name.getStart().getLine();
    if (_currentIpPeerGroup != null) {
      _currentIpPeerGroup.setGroupName(groupName);
      _currentIpPeerGroup.setGroupNameLine(line);
    } else if (_currentNamedPeerGroup != null) {
      // May not hit this since parser for peer-policy does not have
      // recursion.
      _currentNamedPeerGroup.setGroupName(groupName);
      _currentNamedPeerGroup.setGroupNameLine(line);
    } else if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      // Intentional identity comparison above
      throw new BatfishException("Invalid peer context for inheritance");
    } else {
      todo(ctx, F_BGP_INHERIT_PEER_SESSION_OTHER);
    }
  }

  @Override
  public void exitInherit_peer_session_bgp_tail(Inherit_peer_session_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String groupName = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    if (_currentIpPeerGroup != null) {
      _currentIpPeerGroup.setPeerSession(groupName);
      _currentIpPeerGroup.setPeerSessionLine(line);
    } else if (_currentNamedPeerGroup != null) {
      _currentNamedPeerGroup.setPeerSession(groupName);
      _currentNamedPeerGroup.setPeerSessionLine(line);
    } else if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      // Intentional identity comparison above
      throw new BatfishException("Invalid peer context for inheritance");
    } else {
      todo(ctx, F_BGP_INHERIT_PEER_SESSION_OTHER);
    }
  }

  @Override
  public void exitInterface_is_stanza(Interface_is_stanzaContext ctx) {
    _currentIsisInterface = null;
  }

  @Override
  public void exitIp_as_path_access_list_stanza(Ip_as_path_access_list_stanzaContext ctx) {
    _currentAsPathAcl = null;
  }

  @Override
  public void exitIp_as_path_access_list_tail(Ip_as_path_access_list_tailContext ctx) {
    LineAction action = toLineAction(ctx.action);
    String regex = ctx.as_path_regex.getText().trim();
    IpAsPathAccessListLine line = new IpAsPathAccessListLine(action, regex);
    _currentAsPathAcl.addLine(line);
  }

  @Override
  public void exitIp_community_list_expanded_stanza(Ip_community_list_expanded_stanzaContext ctx) {
    _currentExpandedCommunityList = null;
  }

  @Override
  public void exitIp_community_list_expanded_tail(Ip_community_list_expanded_tailContext ctx) {
    LineAction action = toLineAction(ctx.ala);
    StringBuilder regex = new StringBuilder();
    for (Token remainder : ctx.remainder) {
      regex.append(remainder.getText());
    }
    ExpandedCommunityListLine line = new ExpandedCommunityListLine(action, regex.toString());
    _currentExpandedCommunityList.addLine(line);
  }

  @Override
  public void exitIp_community_list_standard_stanza(Ip_community_list_standard_stanzaContext ctx) {
    _currentStandardCommunityList = null;
  }

  @Override
  public void exitIp_community_list_standard_tail(Ip_community_list_standard_tailContext ctx) {
    LineAction action = toLineAction(ctx.ala);
    List<Long> communities = new ArrayList<>();
    for (CommunityContext communityCtx : ctx.communities) {
      long community = toLong(communityCtx);
      communities.add(community);
    }
    StandardCommunityListLine line = new StandardCommunityListLine(action, communities);
    _currentStandardCommunityList.getLines().add(line);
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
      String ifaceName = ctx.iname.getText();
      String canonicalIfaceName = getCanonicalInterfaceName(ifaceName);
      _configuration.setDnsSourceInterface(canonicalIfaceName);
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
  public void exitIp_nat_pool(Ip_nat_poolContext ctx) {
    _currentNatPool = null;
  }

  @Override
  public void exitIp_nat_pool_range(Ip_nat_pool_rangeContext ctx) {
    Ip first = toIp(ctx.first);
    _currentNatPool.setFirst(first);
    Ip last = toIp(ctx.last);
    _currentNatPool.setLast(last);
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
      prefix = new Prefix(address, prefixLength);
    }
    Ip nextHopIp = Route.UNSET_ROUTE_NEXT_HOP_IP;
    String nextHopInterface = null;
    int distance = DEFAULT_STATIC_ROUTE_DISTANCE;
    Integer tag = null;
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
      } catch (BatfishException e) {
        _w.redFlag(
            "Error fetching interface name at: "
                + getLocation(ctx)
                + getFullText(ctx)
                + " : "
                + e.getMessage());
        _currentInterfaces = ImmutableList.of();
        return;
      }
    }
    if (ctx.distance != null) {
      distance = toInteger(ctx.distance);
    }
    if (ctx.tag != null) {
      tag = toInteger(ctx.tag);
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
    Prefix6 prefix6 = new Prefix6(ctx.prefix6.getText());
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
    CiscoStructureType structureType;
    CiscoStructureUsage structureUsage;
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
    long as = toLong(ctx.as);
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
  public void exitMatch_as_path_access_list_rm_stanza(
      Match_as_path_access_list_rm_stanzaContext ctx) {
    Set<String> names = new TreeSet<>();
    for (VariableContext name : ctx.name_list) {
      names.add(name.getText());
      _configuration.referenceStructure(
          AS_PATH_ACCESS_LIST, name.getText(),
          ROUTE_MAP_MATCH_AS_PATH_ACCESS_LIST, name.getStart().getLine());
    }
    RouteMapMatchAsPathAccessListLine line = new RouteMapMatchAsPathAccessListLine(names);
    _currentRouteMapClause.addMatchLine(line);
  }

  @Override
  public void exitMatch_community_list_rm_stanza(Match_community_list_rm_stanzaContext ctx) {
    Set<String> names = new TreeSet<>();
    for (VariableContext name : ctx.name_list) {
      names.add(name.getText());
      _configuration.referenceStructure(
          COMMUNITY_LIST,
          name.getText(),
          ROUTE_MAP_MATCH_COMMUNITY_LIST,
          name.getStart().getLine());
    }
    RouteMapMatchCommunityListLine line = new RouteMapMatchCommunityListLine(names);
    _currentRouteMapClause.addMatchLine(line);
  }

  @Override
  public void exitMatch_ip_access_list_rm_stanza(Match_ip_access_list_rm_stanzaContext ctx) {
    Set<String> names = new TreeSet<>();
    for (Variable_access_listContext v : ctx.name_list) {
      names.add(v.getText());
      _configuration.referenceStructure(
          IPV4_ACCESS_LIST, v.getText(),
          ROUTE_MAP_MATCH_IPV4_ACCESS_LIST, v.getStart().getLine());
    }
    RouteMapMatchIpAccessListLine line = new RouteMapMatchIpAccessListLine(names);
    _currentRouteMapClause.addMatchLine(line);
  }

  @Override
  public void exitMatch_ip_prefix_list_rm_stanza(Match_ip_prefix_list_rm_stanzaContext ctx) {
    Set<String> names = new TreeSet<>();
    for (VariableContext t : ctx.name_list) {
      names.add(t.getText());
      _configuration.referenceStructure(
          PREFIX_LIST, t.getText(), ROUTE_MAP_MATCH_IPV4_PREFIX_LIST, t.getStart().getLine());
    }
    RouteMapMatchIpPrefixListLine line = new RouteMapMatchIpPrefixListLine(names);
    _currentRouteMapClause.addMatchLine(line);
  }

  @Override
  public void exitMatch_ipv6_access_list_rm_stanza(Match_ipv6_access_list_rm_stanzaContext ctx) {
    Set<String> names = new TreeSet<>();
    for (Variable_access_listContext v : ctx.name_list) {
      names.add(v.getText());
      _configuration.referenceStructure(
          IPV6_ACCESS_LIST, v.getText(), ROUTE_MAP_MATCH_IPV6_ACCESS_LIST, v.getStart().getLine());
    }
    RouteMapMatchIpv6AccessListLine line = new RouteMapMatchIpv6AccessListLine(names);
    _currentRouteMapClause.addMatchLine(line);
  }

  @Override
  public void exitMatch_ipv6_prefix_list_rm_stanza(Match_ipv6_prefix_list_rm_stanzaContext ctx) {
    Set<String> names = new TreeSet<>();
    for (VariableContext t : ctx.name_list) {
      names.add(t.getText());
      _configuration.referenceStructure(
          PREFIX6_LIST, t.getText(), ROUTE_MAP_MATCH_IPV6_PREFIX_LIST, t.getStart().getLine());
    }
    RouteMapMatchIpv6PrefixListLine line = new RouteMapMatchIpv6PrefixListLine(names);
    _currentRouteMapClause.addMatchLine(line);
  }

  @Override
  public void exitMatch_source_protocol_rm_stanza(Match_source_protocol_rm_stanzaContext ctx) {
    todo(ctx, "match source-protocol");
  }

  @Override
  public void exitMatch_tag_rm_stanza(Match_tag_rm_stanzaContext ctx) {
    Set<Integer> tags = new TreeSet<>();
    for (Token t : ctx.tag_list) {
      tags.add(toInteger(t));
    }
    RouteMapMatchTagLine line = new RouteMapMatchTagLine(tags);
    _currentRouteMapClause.addMatchLine(line);
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
    todo(ctx, F_BGP_MAXIMUM_PEERS);
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
  public void exitNeighbor_block_inherit(Neighbor_block_inheritContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String groupName = ctx.name.getText();
    if (_currentIpPeerGroup != null) {
      _currentIpPeerGroup.setGroupName(groupName);
    } else if (_currentIpv6PeerGroup != null) {
      _currentIpv6PeerGroup.setGroupName(groupName);
    } else if (_currentDynamicIpPeerGroup != null) {
      _currentDynamicIpPeerGroup.setGroupName(groupName);
    } else if (_currentDynamicIpv6PeerGroup != null) {
      _currentDynamicIpv6PeerGroup.setGroupName(groupName);
    } else if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      // Intentional identity comparison above
      throw new BatfishException("Invalid peer context for inheritance");
    } else {
      todo(ctx, F_BGP_INHERIT_PEER_OTHER);
    }
  }

  @Override
  public void exitNeighbor_block_rb_stanza(Neighbor_block_rb_stanzaContext ctx) {
    resetPeerGroups();
    if (_inBlockNeighbor) {
      popPeer();
    }
  }

  @Override
  public void exitNeighbor_flat_rb_stanza(Neighbor_flat_rb_stanzaContext ctx) {
    resetPeerGroups();
    popPeer();
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
      prefix = new Prefix(address, prefixLength);
    }
    String map = null;
    Integer mapLine = null;
    if (ctx.mapname != null) {
      map = ctx.mapname.getText();
      mapLine = ctx.mapname.getStart().getLine();
      _configuration.referenceStructure(ROUTE_MAP, map, BGP_NETWORK_ORIGINATION_ROUTE_MAP, mapLine);
    }
    BgpNetwork bgpNetwork = new BgpNetwork(map, mapLine);
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.getIpNetworks().put(prefix, bgpNetwork);
  }

  @Override
  public void exitNetwork6_bgp_tail(Network6_bgp_tailContext ctx) {
    Prefix6 prefix6 = new Prefix6(ctx.prefix.getText());
    String map = null;
    Integer mapLine = null;
    if (ctx.mapname != null) {
      map = ctx.mapname.getText();
      mapLine = ctx.mapname.getStart().getLine();
      _configuration.referenceStructure(
          ROUTE_MAP, map, BGP_NETWORK6_ORIGINATION_ROUTE_MAP, mapLine);
    }
    BgpProcess proc = currentVrf().getBgpProcess();
    BgpNetwork6 bgpNetwork6 = new BgpNetwork6(map, mapLine);
    proc.getIpv6Networks().put(prefix6, bgpNetwork6);
  }

  @Override
  public void exitRe_network(Re_networkContext ctx) {
    if (_currentEigrpProcess == null) {
      todo(ctx, "network not supported here");
      return;
    }
    Ip address = toIp(ctx.address);
    Ip mask = (ctx.mask != null) ? toIp(ctx.mask) : address.getClassMask().inverted();
    _currentEigrpProcess.getWildcardNetworks().add(new IpWildcard(address, mask));
  }

  @Override
  public void exitRe_passive_interface(Re_passive_interfaceContext ctx) {
    boolean passive = (ctx.NO() == null);
    getOrAddInterface(ctx.i).setEigrpPassive(passive);
  }

  @Override
  public void exitRe_passive_interface_default(Re_passive_interface_defaultContext ctx) {
    if (_currentEigrpProcess == null) {
      todo(ctx, "network not supported here until autonomous-system stanza supported");
      return;
    }
    boolean passive = (ctx.NO() == null);
    _currentEigrpProcess.setPassiveInterfaceDefault(passive);
  }

  @Override
  public void exitRe_redistribute_bgp(Re_redistribute_bgpContext ctx) {
    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_BGP_MAP, ctx.map.getStart().getLine());
    }
  }

  @Override
  public void exitRe_redistribute_connected(Re_redistribute_connectedContext ctx) {
    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_CONNECTED_MAP, ctx.map.getStart().getLine());
    }
  }

  @Override
  public void exitRe_redistribute_eigrp(Re_redistribute_eigrpContext ctx) {
    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_EIGRP_MAP, ctx.map.getStart().getLine());
    }
  }

  @Override
  public void exitRe_redistribute_isis(Re_redistribute_isisContext ctx) {
    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_ISIS_MAP, ctx.map.getStart().getLine());
    }
  }

  @Override
  public void exitRe_redistribute_ospf(Re_redistribute_ospfContext ctx) {
    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_OSPF_MAP, ctx.map.getStart().getLine());
    }
  }

  @Override
  public void exitRe_redistribute_rip(Re_redistribute_ripContext ctx) {
    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_RIP_MAP, ctx.map.getStart().getLine());
    }
  }

  @Override
  public void exitRe_redistribute_static(Re_redistribute_staticContext ctx) {
    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_STATIC_MAP, ctx.map.getStart().getLine());
    }
  }

  @Override
  public void exitReaf_interface(Reaf_interfaceContext ctx) {
    _currentEigrpInterface = null;
  }

  @Override
  public void exitReafi_passive_interface(Reafi_passive_interfaceContext ctx) {
    boolean passive = (ctx.NO() == null);
    if (_currentEigrpInterface == null) {
      // default interface
      Objects.requireNonNull(_currentEigrpProcess).setPassiveInterfaceDefault(passive);
    } else {
      _currentEigrpInterface.setEigrpPassive(passive);
    }
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
        String message = "ignoring attempt to activate undefined ip peer group: " + ip;
        _w.redFlag(message);
      } else {
        pg.setActive(false);
      }
    } else if (ctx.ip6 != null) {
      Ip6 ip6 = toIp6(ctx.ip6);
      Ipv6BgpPeerGroup pg = proc.getIpv6PeerGroups().get(ip6);
      if (pg == null) {
        String message = "ignoring attempt to activate undefined ipv6 peer group: " + ip6;
        _w.redFlag(message);
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
        String message = "ignoring attempt to shut down to undefined ip peer group: " + ip;
        _w.redFlag(message);
      } else {
        pg.setActive(true);
        pg.setShutdown(false);
      }
    } else if (ctx.ip6 != null) {
      Ip6 ip6 = toIp6(ctx.ip6);
      Ipv6BgpPeerGroup pg = proc.getIpv6PeerGroups().get(ip6);
      // TODO: see if it is always ok to set active on 'no shutdown'
      if (pg == null) {
        String message = "ignoring attempt to shut down undefined ipv6 peer group: " + ip6;
        _w.redFlag(message);
      } else {
        pg.setActive(true);
        pg.setShutdown(false);
      }
    } else if (ctx.peergroup != null) {
      _w.redFlag("'no shutdown' of  peer group unsupported");
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
  public void exitNo_route_map_stanza(No_route_map_stanzaContext ctx) {
    String mapName = ctx.name.getText();
    _configuration.getRouteMaps().remove(mapName);
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
    String ifaceName = ctx.iname.getText();
    String canonicalIfaceName = getCanonicalInterfaceName(ifaceName);
    _configuration.setNtpSourceInterface(canonicalIfaceName);
  }

  @Override
  public void exitNull_as_path_regex(Null_as_path_regexContext ctx) {
    _w.redFlag("as-path regexes this complicated are not supported yet");
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
    int line = ctx.name.getStart().getLine();
    BgpProcess proc = currentVrf().getBgpProcess();
    if (ctx.address != null) {
      Ip address = toIp(ctx.address);
      IpBgpPeerGroup ipPeerGroup = proc.getIpPeerGroups().get(address);
      if (ipPeerGroup == null) {
        proc.addIpPeerGroup(address);
        ipPeerGroup = proc.getIpPeerGroups().get(address);
      }
      ipPeerGroup.setGroupName(peerGroupName);
      ipPeerGroup.setGroupNameLine(line);
    } else if (ctx.address6 != null) {
      Ip6 address6 = toIp6(ctx.address6);
      Ipv6BgpPeerGroup ipv6PeerGroup = proc.getIpv6PeerGroups().get(address6);
      if (ipv6PeerGroup == null) {
        proc.addIpv6PeerGroup(address6);
        ipv6PeerGroup = proc.getIpv6PeerGroups().get(address6);
      }
      ipv6PeerGroup.setGroupName(peerGroupName);
      ipv6PeerGroup.setGroupNameLine(line);
    }
  }

  @Override
  public void exitPeer_group_creation_rb_stanza(Peer_group_creation_rb_stanzaContext ctx) {
    String name = ctx.name.getText();
    int definitionLine = ctx.name.getLine();
    BgpProcess proc = currentVrf().getBgpProcess();
    if (proc.getNamedPeerGroups().get(name) == null) {
      proc.addNamedPeerGroup(name, definitionLine);
      if (ctx.PASSIVE() != null) {
        // dell: won't otherwise specify activation so just activate here
        NamedBgpPeerGroup npg = proc.getNamedPeerGroups().get(name);
        npg.setActive(true);
      }
    }
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
    if (ctx.LIST() != null) {
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, PIM_ACCEPT_REGISTER_ACL, line);
    } else if (ctx.ROUTE_MAP() != null) {
      _configuration.referenceStructure(ROUTE_MAP, name, PIM_ACCEPT_REGISTER_ROUTE_MAP, line);
    }
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
  public void enterPm_ios_inspect(Pm_ios_inspectContext ctx) {
    String name = ctx.name.getText();
    _currentInspectPolicyMap =
        _configuration.getInspectPolicyMaps().computeIfAbsent(name, InspectPolicyMap::new);
    defineStructure(INSPECT_POLICY_MAP, name, ctx);
  }

  @Override
  public void enterPm_class(Pm_classContext ctx) {
    // TODO: do something with this.
    // should be something like _currentPolicyMapClass = ...
    String name = ctx.name.getText();
    _configuration.referenceStructure(CLASS_MAP, name, POLICY_MAP_CLASS, ctx.getStart().getLine());
    if ("class-default".equals(name)) {
      // This is a hack because there's an implicit class named "class-default" and we don't want
      // a false positive undefined reference.
      _configuration.defineStructure(CLASS_MAP, "class-default", ctx.getStart().getLine());
    }
  }

  @Override
  public void exitPm_class(Pm_classContext ctx) {
    // TODO: do something with this.
    // should be something like _currentPolicyMapClass = null.
  }

  @Override
  public void exitPm_event_class(Pm_event_classContext ctx) {
    if (ctx.classname != null) {
      _configuration.referenceStructure(
          CLASS_MAP, ctx.classname.getText(), POLICY_MAP_EVENT_CLASS, ctx.getStart().getLine());
    }
    if (ctx.stname != null) {
      _configuration.referenceStructure(
          SERVICE_TEMPLATE,
          ctx.stname.getText(),
          POLICY_MAP_EVENT_CLASS_ACTIVATE,
          ctx.stname.getStart().getLine());
    }
  }

  @Override
  public void exitPm_ios_inspect(Pm_ios_inspectContext ctx) {
    _currentInspectPolicyMap = null;
  }

  @Override
  public void enterPm_iosi_class_type_inspect(Pm_iosi_class_type_inspectContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(
        INSPECT_CLASS_MAP, name, INSPECT_POLICY_MAP_INSPECT_CLASS, line);
    _currentInspectPolicyMapInspectClass =
        _currentInspectPolicyMap
            .getInspectClasses()
            .computeIfAbsent(name, n -> new InspectPolicyMapInspectClass());
  }

  @Override
  public void exitPm_iosi_class_type_inspect(Pm_iosi_class_type_inspectContext ctx) {
    _currentInspectPolicyMapInspectClass = null;
  }

  @Override
  public void exitPi_iosicd_drop(Pi_iosicd_dropContext ctx) {
    _currentInspectPolicyMap.setClassDefaultAction(LineAction.REJECT);
  }

  @Override
  public void exitPi_iosicd_pass(Pi_iosicd_passContext ctx) {
    _currentInspectPolicyMap.setClassDefaultAction(LineAction.ACCEPT);
  }

  @Override
  public void exitPm_iosict_inspect(Pm_iosict_inspectContext ctx) {
    _currentInspectPolicyMapInspectClass.setAction(PolicyMapClassAction.INSPECT);
  }

  @Override
  public void exitPm_iosict_pass(Pm_iosict_passContext ctx) {
    _currentInspectPolicyMapInspectClass.setAction(PolicyMapClassAction.PASS);
  }

  @Override
  public void exitPm_iosict_drop(Pm_iosict_dropContext ctx) {
    _currentInspectPolicyMapInspectClass.setAction(PolicyMapClassAction.DROP);
  }

  @Override
  public void exitPmc_service_policy(Pmc_service_policyContext ctx) {
    _configuration.referenceStructure(
        POLICY_MAP,
        ctx.name.getText(),
        POLICY_MAP_CLASS_SERVICE_POLICY,
        ctx.name.getStart().getLine());
  }

  @Override
  public void exitPrefix_list_bgp_tail(Prefix_list_bgp_tailContext ctx) {
    String listName = ctx.list_name.getText();
    int line = ctx.list_name.getLine();
    CiscoStructureType type = _inIpv6BgpPeer ? PREFIX6_LIST : PREFIX_LIST;
    CiscoStructureUsage usage;
    if (ctx.IN() != null) {
      _currentPeerGroup.setInboundPrefixList(listName);
      usage = _inIpv6BgpPeer ? BGP_INBOUND_PREFIX6_LIST : BGP_INBOUND_PREFIX_LIST;
    } else if (ctx.OUT() != null) {
      _currentPeerGroup.setOutboundPrefixList(listName);
      usage = _inIpv6BgpPeer ? BGP_OUTBOUND_PREFIX6_LIST : BGP_OUTBOUND_PREFIX_LIST;
    } else {
      throw new BatfishException("bad direction");
    }
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
          prefix = new Prefix(toIp(ctx.ipa), Prefix.MAX_PREFIX_LENGTH);
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
        PrefixListLine line = new PrefixListLine(LineAction.ACCEPT, prefix, lengthRange);
        pl.addLine(line);
      } else {
        Prefix6List pl = _configuration.getPrefix6Lists().computeIfAbsent(name, Prefix6List::new);
        Prefix6 prefix6;
        if (ctx.ipv6a != null) {
          prefix6 = new Prefix6(toIp6(ctx.ipv6a), Prefix6.MAX_PREFIX_LENGTH);
        } else {
          prefix6 = new Prefix6(ctx.ipv6_prefix.getText());
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
        Prefix6ListLine line = new Prefix6ListLine(LineAction.ACCEPT, prefix6, lengthRange);
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
    todo(ctx, F_BGP_REDISTRIBUTE_AGGREGATE);
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
      if (ctx.map != null) {
        String map = ctx.map.getText();
        int mapLine = ctx.map.getStart().getLine();
        r.setRouteMap(map);
        r.setRouteMapLine(mapLine);
        _configuration.referenceStructure(ROUTE_MAP, map, BGP_REDISTRIBUTE_CONNECTED_MAP, mapLine);
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
    if (ctx.map != null) {
      String map = ctx.map.getText();
      r.setMap(map);
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
      if (ctx.map != null) {
        String map = ctx.map.getText();
        int mapLine = ctx.map.getStart().getLine();
        r.setRouteMap(map);
        r.setRouteMapLine(mapLine);
        _configuration.referenceStructure(ROUTE_MAP, map, BGP_REDISTRIBUTE_OSPF_MAP, mapLine);
      }
      if (ctx.procnum != null) {
        int procNum = toInteger(ctx.procnum);
        r.getSpecialAttributes().put(BgpRedistributionPolicy.OSPF_PROCESS_NUMBER, procNum);
      }
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
    }
  }

  @Override
  public void exitRedistribute_ospfv3_bgp_tail(Redistribute_ospfv3_bgp_tailContext ctx) {
    if (ctx.map != null) {
      String map = ctx.map.getText();
      int mapLine = ctx.map.getStart().getLine();
      _configuration.referenceStructure(ROUTE_MAP, map, BGP_REDISTRIBUTE_OSPFV3_MAP, mapLine);
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
      if (ctx.map != null) {
        String map = ctx.map.getText();
        int mapLine = ctx.map.getStart().getLine();
        r.setRouteMap(map);
        r.setRouteMapLine(mapLine);
        _configuration.referenceStructure(ROUTE_MAP, map, BGP_REDISTRIBUTE_RIP_MAP, mapLine);
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
      if (ctx.map != null) {
        String map = ctx.map.getText();
        int mapLine = ctx.map.getStart().getLine();
        r.setRouteMap(map);
        r.setRouteMapLine(mapLine);
        _configuration.referenceStructure(ROUTE_MAP, map, BGP_REDISTRIBUTE_STATIC_MAP, mapLine);
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
    if (ctx.map != null) {
      String map = ctx.map.getText();
      r.setMap(map);
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
    long as = toLong(ctx.as);
    if (_currentPeerGroup != proc.getMasterBgpPeerGroup()) {
      _currentPeerGroup.setRemoteAs(as);
    } else {
      throw new BatfishException(
          "no peer or peer group in context: " + getLocation(ctx) + getFullText(ctx));
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
    todo(ctx, "ospf area filter-list");
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
      todo(ctx, F_OSPF_AREA_NSSA_NO_REDISTRIBUTION);
      settings.setNoRedistribution(true);
    }
    if (ctx.no_summary != null) {
      settings.setNoSummary(true);
    }
  }

  @Override
  public void exitRo_area_range(CiscoParser.Ro_area_rangeContext ctx) {
    long areaNum = (ctx.area_int != null) ? toLong(ctx.area_int) : toIp(ctx.area_ip).asLong();
    Prefix prefix;
    if (ctx.area_prefix != null) {
      prefix = Prefix.parse(ctx.area_prefix.getText());
    } else {
      prefix = new Prefix(toIp(ctx.area_ip), toIp(ctx.area_subnet));
    }
    boolean advertise = ctx.NOT_ADVERTISE() == null;
    Long cost = ctx.cost == null ? null : toLong(ctx.cost);

    Map<Prefix, OspfAreaSummary> area =
        currentVrf().getOspfProcess().getSummaries().computeIfAbsent(areaNum, k -> new TreeMap<>());
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
    if (ctx.map != null) {
      String map = ctx.map.getText();
      int line = ctx.map.getLine();
      proc.setDefaultInformationOriginateMap(map);
      proc.setDefaultInformationOriginateMapLine(line);
      _configuration.referenceStructure(ROUTE_MAP, map, OSPF_DEFAULT_ORIGINATE_ROUTE_MAP, line);
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
  public void exitRo_maximum_paths(Ro_maximum_pathsContext ctx) {
    todo(ctx, F_OSPF_MAXIMUM_PATHS);
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
    if (_format == CISCO_ASA) {
      wildcard = wildcard.inverted();
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
    String iname = ctx.i.getText();
    OspfProcess proc = _currentOspfProcess;
    if (passive) {
      proc.getPassiveInterfaceList().add(iname);
    } else {
      proc.getActiveInterfaceList().add(iname);
    }
  }

  @Override
  public void exitRo_passive_interface_default(Ro_passive_interface_defaultContext ctx) {
    _currentOspfProcess.setPassiveInterfaceDefault(true);
  }

  private static boolean ospfRedistributeSubnetsByDefault(ConfigurationFormat format) {
    /*
     * CISCO_IOS requires the subnets keyword or only classful routes will be redistributed.
     * CISCO_NXOS and ARISTA redistribute all subnets.
     *
     * We assume that others use this sane default too. TODO: verify more vendors.
     */
    return format != CISCO_IOS;
  }

  @Override
  public void exitRo_redistribute_bgp(Ro_redistribute_bgpContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    RoutingProtocol sourceProtocol = RoutingProtocol.BGP;
    OspfRedistributionPolicy r = new OspfRedistributionPolicy(sourceProtocol);
    proc.getRedistributionPolicies().put(sourceProtocol, r);
    int as = toInteger(ctx.as);
    r.getSpecialAttributes().put(OspfRedistributionPolicy.BGP_AS, as);
    if (ctx.metric != null) {
      int metric = toInteger(ctx.metric);
      r.setMetric(metric);
    }
    if (ctx.map != null) {
      String map = ctx.map.getText();
      int mapLine = ctx.map.getLine();
      r.setRouteMap(map);
      r.setRouteMapLine(mapLine);
      _configuration.referenceStructure(ROUTE_MAP, map, OSPF_REDISTRIBUTE_BGP_MAP, mapLine);
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
    r.setOnlyClassfulRoutes(ctx.subnets == null && !ospfRedistributeSubnetsByDefault(_format));
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
    if (ctx.map != null) {
      String map = ctx.map.getText();
      int mapLine = ctx.map.getLine();
      r.setRouteMap(map);
      r.setRouteMapLine(mapLine);
      _configuration.referenceStructure(ROUTE_MAP, map, OSPF_REDISTRIBUTE_CONNECTED_MAP, mapLine);
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
    r.setOnlyClassfulRoutes(ctx.subnets == null && !ospfRedistributeSubnetsByDefault(_format));
  }

  @Override
  public void exitRo_redistribute_eigrp(Ro_redistribute_eigrpContext ctx) {
    if (ctx.map != null) {
      _configuration.referenceStructure(
          ROUTE_MAP, ctx.map.getText(), OSPF_REDISTRIBUTE_EIGRP_MAP, ctx.map.getStart().getLine());
    }
    todo(ctx, F_OSPF_REDISTRIBUTE_EIGRP);
  }

  @Override
  public void exitRo_redistribute_rip(Ro_redistribute_ripContext ctx) {
    todo(ctx, F_OSPF_REDISTRIBUTE_RIP);
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
    if (ctx.map != null) {
      String map = ctx.map.getText();
      int mapLine = ctx.map.getLine();
      r.setRouteMap(map);
      r.setRouteMapLine(mapLine);
      _configuration.referenceStructure(ROUTE_MAP, map, OSPF_REDISTRIBUTE_STATIC_MAP, mapLine);
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
    r.setOnlyClassfulRoutes(ctx.subnets == null && !ospfRedistributeSubnetsByDefault(_format));
  }

  @Override
  public void exitRo_rfc1583_compatibility(Ro_rfc1583_compatibilityContext ctx) {
    currentVrf().getOspfProcess().setRfc1583Compatible(ctx.NO() == null);
  }

  @Override
  public void exitRo_router_id(Ro_router_idContext ctx) {
    Ip routerId = toIp(ctx.ip);
    _currentOspfProcess.setRouterId(routerId);
  }

  @Override
  public void exitRo_vrf(Ro_vrfContext ctx) {
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
    _currentOspfProcess = currentVrf().getOspfProcess();
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
        currentVrf()
            .getOspfProcess()
            .getSummaries()
            .computeIfAbsent(_currentOspfArea, k -> new TreeMap<>());
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
      _currentOspfProcess.getPassiveInterfaceList().add(_currentOspfInterface);
    }
  }

  @Override
  public void exitRoute_map_bgp_tail(Route_map_bgp_tailContext ctx) {
    if (_currentPeerGroup == null) {
      return;
    }
    String mapName = ctx.name.getText();
    boolean ipv6 = _inIpv6BgpPeer || _currentIpv6PeerGroup != null;
    CiscoStructureUsage usage;
    if (ctx.IN() != null) {
      if (ipv6) {
        _currentPeerGroup.setInboundRoute6Map(mapName);
        usage = BGP_INBOUND_ROUTE6_MAP;
      } else {
        _currentPeerGroup.setInboundRouteMap(mapName);
        usage = BGP_INBOUND_ROUTE_MAP;
      }
    } else if (ctx.OUT() != null) {
      if (ipv6) {
        _currentPeerGroup.setOutboundRoute6Map(mapName);
        usage = BGP_OUTBOUND_ROUTE6_MAP;
      } else {
        _currentPeerGroup.setOutboundRouteMap(mapName);
        usage = BGP_OUTBOUND_ROUTE_MAP;
      }
    } else {
      throw new BatfishException("bad direction");
    }
    _configuration.referenceStructure(ROUTE_MAP, mapName, usage, ctx.getStart().getLine());
  }

  @Override
  public void exitRoute_map_stanza(Route_map_stanzaContext ctx) {
    _currentRouteMap = null;
    _currentRouteMapClause = null;
  }

  @Override
  public void exitRoute_policy_bgp_tail(Route_policy_bgp_tailContext ctx) {
    String name = ctx.name.getText();
    CiscoStructureUsage usage;
    if (ctx.IN() != null) {
      _currentPeerGroup.setInboundRouteMap(name);
      usage = BGP_NEIGHBOR_ROUTE_POLICY_IN;
    } else {
      _currentPeerGroup.setOutboundRouteMap(name);
      usage = BGP_NEIGHBOR_ROUTE_POLICY_OUT;
    }
    _configuration.referenceStructure(ROUTE_POLICY, name, usage, ctx.name.getStart().getLine());

    if (ctx.route_policy_params_list() != null && !ctx.route_policy_params_list().isEmpty()) {
      _w.redFlag("Unimplemented: IOS-XR RPL functions" + getFullText(ctx));
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
  public void exitRouter_bgp_stanza(Router_bgp_stanzaContext ctx) {
    if (_parser.getParser().isNxos()) {
      _currentBgpNxVrfConfiguration = null;
    } else {
      popPeer();
    }
  }

  @Override
  public void exitRouter_id_bgp_tail(Router_id_bgp_tailContext ctx) {
    Ip routerId = toIp(ctx.routerid);
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.setRouterId(routerId);
  }

  @Override
  public void exitRe_classic(Re_classicContext ctx) {
    Objects.requireNonNull(_currentEigrpProcess)
        .computeNetworks(_configuration.getInterfaces().values());
    _currentEigrpProcess = null;
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
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
    Prefix network = new Prefix(networkAddress, mask);
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
      }
      int distance = DEFAULT_STATIC_ROUTE_DISTANCE;
      if (ctx.distance != null) {
        distance = toInteger(ctx.distance);
      }
      Integer tag = null;
      if (ctx.tag != null) {
        tag = toInteger(ctx.tag);
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
  public void exitS_feature(S_featureContext ctx) {
    List<String> words = ctx.words.stream().map(RuleContext::getText).collect(Collectors.toList());
    boolean enabled = ctx.NO() == null;
    String name = String.join(".", words);
    _configuration.getCf().getFeatures().put(name, enabled);
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
    todo(ctx, F_IP_DEFAULT_GATEWAY);
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
    String ifaceName = ctx.iname.getText();
    String canonicalIfaceName = getCanonicalInterfaceName(ifaceName);
    _configuration.setTacacsSourceInterface(canonicalIfaceName);
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
  public void exitS_no_access_list_extended(S_no_access_list_extendedContext ctx) {
    String name = ctx.ACL_NUM_EXTENDED().getText();
    _configuration.getExtendedAcls().remove(name);
  }

  @Override
  public void exitS_no_access_list_standard(S_no_access_list_standardContext ctx) {
    String name = ctx.ACL_NUM_STANDARD().getText();
    _configuration.getStandardAcls().remove(name);
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
  public void exitS_service_policy_interface(S_service_policy_interfaceContext ctx) {
    _configuration.referenceStructure(
        POLICY_MAP,
        ctx.name.getText(),
        SERVICE_POLICY_INTERFACE_POLICY,
        ctx.name.getStart().getLine());
    String iface = getCanonicalInterfaceName(ctx.iface.getText());
    _configuration.referenceStructure(
        INTERFACE, iface, SERVICE_POLICY_INTERFACE, ctx.iface.getStart().getLine());
  }

  @Override
  public void exitS_spanning_tree(S_spanning_treeContext ctx) {
    _no = false;
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
  public void exitS_vrf_context(S_vrf_contextContext ctx) {
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
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
  public void exitSet_as_path_prepend_rm_stanza(Set_as_path_prepend_rm_stanzaContext ctx) {
    List<AsExpr> asList = new ArrayList<>();
    for (As_exprContext asx : ctx.as_list) {
      AsExpr as = toAsExpr(asx);
      asList.add(as);
    }
    RouteMapSetAsPathPrependLine line = new RouteMapSetAsPathPrependLine(asList);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_comm_list_delete_rm_stanza(Set_comm_list_delete_rm_stanzaContext ctx) {
    String name = ctx.name.getText();
    RouteMapSetDeleteCommunityLine line = new RouteMapSetDeleteCommunityLine(name);
    _currentRouteMapClause.addSetLine(line);
    _configuration.referenceStructure(
        COMMUNITY_LIST, name, ROUTE_MAP_DELETE_COMMUNITY, ctx.getStart().getLine());
  }

  @Override
  public void exitSet_community_additive_rm_stanza(Set_community_additive_rm_stanzaContext ctx) {
    List<Long> commList = new ArrayList<>();
    for (CommunityContext c : ctx.communities) {
      long community = toLong(c);
      commList.add(community);
    }
    RouteMapSetAdditiveCommunityLine line = new RouteMapSetAdditiveCommunityLine(commList);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_community_list_additive_rm_stanza(
      Set_community_list_additive_rm_stanzaContext ctx) {
    Set<String> communityLists = new LinkedHashSet<>();
    for (VariableContext communityListCtx : ctx.comm_lists) {
      String communityList = communityListCtx.getText();
      communityLists.add(communityList);
      _configuration.referenceStructure(
          COMMUNITY_LIST,
          communityList,
          ROUTE_MAP_ADD_COMMUNITY,
          communityListCtx.getStart().getLine());
    }
    RouteMapSetAdditiveCommunityListLine line =
        new RouteMapSetAdditiveCommunityListLine(communityLists);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_community_list_rm_stanza(Set_community_list_rm_stanzaContext ctx) {
    Set<String> communityLists = new LinkedHashSet<>();
    for (VariableContext communityListCtx : ctx.comm_lists) {
      String communityList = communityListCtx.getText();
      communityLists.add(communityList);
      _configuration.referenceStructure(
          COMMUNITY_LIST,
          communityList,
          ROUTE_MAP_SET_COMMUNITY,
          communityListCtx.getStart().getLine());
    }
    RouteMapSetCommunityListLine line = new RouteMapSetCommunityListLine(communityLists);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_community_none_rm_stanza(Set_community_none_rm_stanzaContext ctx) {
    RouteMapSetCommunityNoneLine line = new RouteMapSetCommunityNoneLine();
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_community_rm_stanza(Set_community_rm_stanzaContext ctx) {
    List<Long> commList = new ArrayList<>();
    for (CommunityContext c : ctx.communities) {
      long community = toLong(c);
      commList.add(community);
    }
    RouteMapSetCommunityLine line = new RouteMapSetCommunityLine(commList);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_local_preference_rm_stanza(Set_local_preference_rm_stanzaContext ctx) {
    IntExpr localPreference = toLocalPreferenceIntExpr(ctx.pref);
    RouteMapSetLocalPreferenceLine line = new RouteMapSetLocalPreferenceLine(localPreference);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_metric_rm_stanza(Set_metric_rm_stanzaContext ctx) {
    LongExpr metric = toMetricLongExpr(ctx.metric);
    RouteMapSetMetricLine line = new RouteMapSetMetricLine(metric);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_metric_type_rm_stanza(Set_metric_type_rm_stanzaContext ctx) {
    todo(ctx, F_ROUTE_MAP_SET_METRIC_TYPE);
  }

  @Override
  public void exitSet_next_hop_peer_address_stanza(Set_next_hop_peer_address_stanzaContext ctx) {
    RouteMapSetNextHopPeerAddress line = new RouteMapSetNextHopPeerAddress();
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_next_hop_rm_stanza(Set_next_hop_rm_stanzaContext ctx) {
    List<Ip> nextHops = new ArrayList<>();
    for (Token t : ctx.nexthop_list) {
      Ip nextHop = toIp(t);
      nextHops.add(nextHop);
    }
    RouteMapSetNextHopLine line = new RouteMapSetNextHopLine(nextHops);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_origin_rm_stanza(Set_origin_rm_stanzaContext ctx) {
    OriginExpr originExpr = toOriginExpr(ctx.origin_expr_literal());
    RouteMapSetLine line = new RouteMapSetOriginTypeLine(originExpr);
    _currentRouteMapClause.addSetLine(line);
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
    String ifaceName = ctx.iname.getText();
    String canonicalIfaceName = getCanonicalInterfaceName(ifaceName);
    _configuration.setSnmpSourceInterface(canonicalIfaceName);
  }

  @Override
  public void exitSs_tftp_server_list(Ss_tftp_server_listContext ctx) {
    String acl = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IP_ACCESS_LIST, acl, SNMP_SERVER_TFTP_SERVER_LIST, line);
  }

  @Override
  public void exitSs_trap_source(Ss_trap_sourceContext ctx) {
    String ifaceName = ctx.iname.getText();
    String canonicalIfaceName = getCanonicalInterfaceName(ifaceName);
    _configuration.setSnmpSourceInterface(canonicalIfaceName);
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
  public void exitStandard_access_list_stanza(Standard_access_list_stanzaContext ctx) {
    _currentStandardAcl = null;
  }

  @Override
  public void exitStandard_access_list_tail(Standard_access_list_tailContext ctx) {
    LineAction action = toLineAction(ctx.ala);
    AccessListAddressSpecifier srcAddressSpecifier = toAccessListAddressSpecifier(ctx.ipr);
    StandardAccessListServiceSpecifier serviceSpecifer =
        computeStandardAccessListServiceSpecifier(ctx);
    String name = getFullText(ctx).trim();
    StandardAccessListLine line =
        new StandardAccessListLine(action, name, serviceSpecifer, srcAddressSpecifier);
    _currentStandardAcl.addLine(line);
  }

  private StandardAccessListServiceSpecifier computeStandardAccessListServiceSpecifier(
      Standard_access_list_tailContext ctx) {
    Set<Integer> dscps = new TreeSet<>();
    Set<Integer> ecns = new TreeSet<>();
    for (Standard_access_list_additional_featureContext feature : ctx.features) {
      if (feature.DSCP() != null) {
        int dscpType = toDscpType(feature.dscp_type());
        dscps.add(dscpType);
      } else if (feature.ECN() != null) {
        int ecn = toInteger(feature.ecn);
        ecns.add(ecn);
      }
    }
    return new StandardAccessListServiceSpecifier(dscps, ecns);
  }

  @Override
  public void exitStandard_ipv6_access_list_stanza(Standard_ipv6_access_list_stanzaContext ctx) {
    _currentStandardIpv6Acl = null;
  }

  @Override
  public void exitStandard_ipv6_access_list_tail(Standard_ipv6_access_list_tailContext ctx) {
    LineAction action = toLineAction(ctx.ala);
    Ip6 srcIp = getIp(ctx.ipr);
    Ip6 srcWildcard = getWildcard(ctx.ipr);
    Set<Integer> dscps = new TreeSet<>();
    Set<Integer> ecns = new TreeSet<>();
    for (Standard_access_list_additional_featureContext feature : ctx.features) {
      if (feature.DSCP() != null) {
        int dscpType = toDscpType(feature.dscp_type());
        dscps.add(dscpType);
      } else if (feature.ECN() != null) {
        int ecn = toInteger(feature.ecn);
        ecns.add(ecn);
      }
    }
    String name;
    if (ctx.num != null) {
      name = ctx.num.getText();
    } else {
      name = getFullText(ctx).trim();
    }
    StandardIpv6AccessListLine line =
        new StandardIpv6AccessListLine(
            name, action, new Ip6Wildcard(srcIp, srcWildcard), dscps, ecns);
    _currentStandardIpv6Acl.addLine(line);
  }

  // @Override
  // public void exitSubnet_bgp_tail(Subnet_bgp_tailContext ctx) {
  // BgpProcess proc = currentVrf().getBgpProcess();
  // if (ctx.IP_PREFIX() != null) {
  // Prefix prefix = new Prefix(ctx.IP_PREFIX().getText());
  // NamedBgpPeerGroup namedGroup = _currentNamedPeerGroup;
  // namedGroup.addNeighborIpPrefix(prefix);
  // DynamicIpBgpPeerGroup pg = proc.addDynamicIpPeerGroup(prefix);
  // pg.setGroupName(namedGroup.getName());
  // }
  // else if (ctx.IPV6_PREFIX() != null) {
  // Prefix6 prefix6 = new Prefix6(ctx.IPV6_PREFIX().getText());
  // NamedBgpPeerGroup namedGroup = _currentNamedPeerGroup;
  // namedGroup.addNeighborIpv6Prefix(prefix6);
  // DynamicIpv6BgpPeerGroup pg = proc.addDynamicIpv6PeerGroup(prefix6);
  // pg.setGroupName(namedGroup.getName());
  // }
  // }
  //
  @Override
  public void exitSummary_address_is_stanza(Summary_address_is_stanzaContext ctx) {
    Ip ip = toIp(ctx.ip);
    Ip mask = toIp(ctx.mask);
    Prefix prefix = new Prefix(ip, mask);
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
    todo(ctx, F_SWITCHING_MODE);
  }

  @Override
  public void exitT_server(T_serverContext ctx) {
    String hostname = ctx.hostname.getText();
    _configuration.getTacacsServers().add(hostname);
  }

  @Override
  public void exitT_source_interface(T_source_interfaceContext ctx) {
    String ifaceName = ctx.iname.getText();
    String canonicalIfaceName = getCanonicalInterfaceName(ifaceName);
    _configuration.setTacacsSourceInterface(canonicalIfaceName);
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
  public void exitTemplate_peer_rb_stanza(Template_peer_rb_stanzaContext ctx) {
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
    if (ctx.up_arista_md5() != null) {
      passwordString = ctx.up_arista_md5().pass.getText();
    } else if (ctx.up_arista_sha512() != null) {
      passwordString = ctx.up_arista_sha512().pass.getText();
    } else if (ctx.up_cisco() != null) {
      passwordString = ctx.up_cisco().pass.getText();
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
  public void exitUnsuppress_map_bgp_tail(Unsuppress_map_bgp_tailContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_ROUTE_MAP_UNSUPPRESS, ctx.getStart().getLine());
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
  public void exitUse_session_group_bgp_tail(Use_session_group_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    String groupName = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    if (_currentIpPeerGroup != null) {
      _currentIpPeerGroup.setPeerSession(groupName);
      _currentIpPeerGroup.setPeerSessionLine(line);
    } else if (_currentNamedPeerGroup != null) {
      _currentNamedPeerGroup.setPeerSession(groupName);
      _currentNamedPeerGroup.setPeerSessionLine(line);
    } else if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      // Intentional identity comparison above
      throw new BatfishException("Invalid peer context for inheritance");
    } else {
      todo(ctx, F_BGP_INHERIT_PEER_SESSION_OTHER);
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
  public void exitVrfc_ip_route(Vrfc_ip_routeContext ctx) {
    todo(ctx, F_IP_ROUTE_VRF);
  }

  @Override
  public void exitVrfd_description(Vrfd_descriptionContext ctx) {
    currentVrf().setDescription(ctx.description_line().text.getText());
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
  private String getAddressGroup(Access_list_ip_rangeContext ctx) {
    if (ctx.address_group != null) {
      return ctx.address_group.getText();
    } else {
      return null;
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

  public CiscoConfiguration getConfiguration() {
    return _configuration;
  }

  private String getFullText(ParserRuleContext ctx) {
    int start = ctx.getStart().getStartIndex();
    int end = ctx.getStop().getStopIndex();
    return _text.substring(start, end + 1);
  }

  private String getLocation(ParserRuleContext ctx) {
    return ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ": ";
  }

  public int getPortNumber(PortContext ctx) {
    if (ctx.DEC() != null) {
      return toInteger(ctx.DEC());
    } else {
      NamedPort namedPort = toNamedPort(ctx);
      return namedPort.number();
    }
  }

  public String getText() {
    return _text;
  }

  @Override
  public Set<String> getUnimplementedFeatures() {
    return _unimplementedFeatures;
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
      return new Prefix6(ctx.ipv6_prefix.getText()).getPrefixWildcard();
    } else if (ctx.ip != null) {
      // basically same as host
      return Ip6.ZERO;
    } else {
      throw convError(Ip.class, ctx);
    }
  }

  private void initInterface(Interface iface, ConfigurationFormat format) {
    switch (format) {
      case CISCO_ASA:
      case CISCO_IOS:
        iface.setProxyArp(true);
        break;

      case ARISTA:
        if (iface.getName().startsWith("Ethernet")) {
          iface.setSwitchportMode(SwitchportMode.ACCESS);
        }
        break;

        // $CASES-OMITTED$
      default:
        break;
    }
  }

  private void initInterface(Interface iface, Interface_nameContext ctx) {
    String nameAlpha = ctx.name_prefix_alpha.getText();
    String canonicalNamePrefix = CiscoConfiguration.getCanonicalInterfaceNamePrefix(nameAlpha);
    String vrf =
        canonicalNamePrefix.equals(CiscoConfiguration.NXOS_MANAGEMENT_INTERFACE_PREFIX)
            ? CiscoConfiguration.MANAGEMENT_VRF_NAME
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
  public void processParseTree(ParserRuleContext tree) {
    ParseTreeWalker walker = new ParseTreeWalker();
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
      return new AutoAs();
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarAs(ctx.RP_VARIABLE().getText());
    } else {
      throw convError(AsExpr.class, ctx);
    }
  }

  private AsPathSetElem toAsPathSetElem(As_path_set_elemContext ctx) {
    if (ctx.IOS_REGEX() != null) {
      String withQuotes = ctx.AS_PATH_SET_REGEX().getText();
      String iosRegex = withQuotes.substring(1, withQuotes.length() - 1);
      String regex = toJavaRegex(iosRegex);
      return new RegexAsPathSetElem(regex);
    } else {
      throw convError(AsPathSetElem.class, ctx);
    }
  }

  private AsPathSetExpr toAsPathSetExpr(As_path_set_inlineContext ctx) {
    List<AsPathSetElem> elems =
        ctx.elems.stream().map(this::toAsPathSetElem).collect(Collectors.toList());
    return new ExplicitAsPathSet(elems);
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

  private CommunitySetElem toCommunitySetElemExpr(Rp_community_set_elemContext ctx) {
    if (ctx.prefix != null) {
      CommunitySetElemHalfExpr prefix = toCommunitySetElemHalfExpr(ctx.prefix);
      CommunitySetElemHalfExpr suffix = toCommunitySetElemHalfExpr(ctx.suffix);
      return new CommunitySetElem(prefix, suffix);
    } else if (ctx.community() != null) {
      long value = toLong(ctx.community());
      return new CommunitySetElem(value);
    } else {
      throw convError(CommunitySetElem.class, ctx);
    }
  }

  private CommunitySetElemHalfExpr toCommunitySetElemHalfExpr(
      Rp_community_set_elem_halfContext ctx) {
    if (ctx.value != null) {
      int value = toInteger(ctx.value);
      return new LiteralCommunitySetElemHalf(value);
    } else if (ctx.var != null) {
      String var = ctx.var.getText();
      return new VarCommunitySetElemHalf(var);
    } else if (ctx.first != null) {
      int first = toInteger(ctx.first);
      int last = toInteger(ctx.last);
      SubRange range = new SubRange(first, last);
      return new RangeCommunitySetElemHalf(range);
    } else if (ctx.ASTERISK() != null) {
      return new RangeCommunitySetElemHalf(new SubRange(0, 65535));
    } else {
      // For an unhandled expression, treat it as matching everything.
      return convProblem(
          CommunitySetElemHalfExpr.class,
          ctx,
          new RangeCommunitySetElemHalf(new SubRange(0, 65535)));
    }
  }

  private void todo(ParserRuleContext ctx, String feature) {
    _w.todo(ctx, feature, _parser, _text);
    _unimplementedFeatures.add("Cisco: " + feature);
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

  private EncryptionAlgorithm toEncryptionAlgorithm(Ike_encryption_arubaContext ctx) {
    if (ctx.AES128() != null) {
      return EncryptionAlgorithm.AES_128_CBC;
    } else if (ctx.AES192() != null) {
      return EncryptionAlgorithm.AES_192_CBC;
    } else if (ctx.AES256() != null) {
      return EncryptionAlgorithm.AES_256_CBC;
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

  private EncryptionAlgorithm toEncryptionAlgorithm(Ipsec_encryption_arubaContext ctx) {
    if (ctx.ESP_AES128() != null) {
      return EncryptionAlgorithm.AES_128_CBC;
    } else if (ctx.ESP_AES128_GCM() != null) {
      return EncryptionAlgorithm.AES_128_GCM;
    } else if (ctx.ESP_AES192() != null) {
      return EncryptionAlgorithm.AES_192_CBC;
    } else if (ctx.ESP_AES256() != null) {
      return EncryptionAlgorithm.AES_256_CBC;
    } else if (ctx.ESP_AES256_GCM() != null) {
      return EncryptionAlgorithm.AES_256_GCM;
    } else if (ctx.ESP_DES() != null) {
      return EncryptionAlgorithm.DES_CBC;
    } else if (ctx.ESP_3DES() != null) {
      return EncryptionAlgorithm.THREEDES_CBC;
    } else {
      throw convError(EncryptionAlgorithm.class, ctx);
    }
  }

  private Integer toIcmpType(Icmp_object_typeContext ctx) {
    if (ctx.ECHO() != null) {
      return IcmpType.ECHO_REQUEST;
    } else if (ctx.ECHO_REPLY() != null) {
      return IcmpType.ECHO_REPLY;
    } else if (ctx.PARAMETER_PROBLEM() != null) {
      return IcmpType.PARAMETER_PROBLEM;
    } else if (ctx.REDIRECT() != null) {
      return IcmpType.REDIRECT_MESSAGE;
    } else if (ctx.SOURCE_QUENCH() != null) {
      return IcmpType.SOURCE_QUENCH;
    } else if (ctx.TIME_EXCEEDED() != null) {
      return IcmpType.TIME_EXCEEDED;
    } else if (ctx.TRACEROUTE() != null) {
      return IcmpType.TRACEROUTE;
    } else if (ctx.UNREACHABLE() != null) {
      return IcmpType.DESTINATION_UNREACHABLE;
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

  private IpProtocol toIpProtocol(ProtocolContext ctx) {
    if (ctx.DEC() != null) {
      int num = toInteger(ctx.DEC());
      return IpProtocol.fromNumber(num);
    } else if (ctx.AH() != null || ctx.AHP() != null) {
      // Different Cisco variants use `ahp` or `ah` to mean the IPSEC authentication header protocol
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
      return IpProtocol.IP;
    } else if (ctx.IPINIP() != null) {
      return IpProtocol.IPINIP;
    } else if (ctx.IPV4() != null) {
      return IpProtocol.IP;
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
      return LineAction.ACCEPT;
    } else if (ctx.DENY() != null) {
      return LineAction.REJECT;
    } else {
      throw convError(LineAction.class, ctx);
    }
  }

  private IntExpr toLocalPreferenceIntExpr(Int_exprContext ctx) {
    if (ctx.DEC() != null) {
      int val = toInteger(ctx.DEC());
      if (ctx.PLUS() != null) {
        return new IncrementLocalPreference(val);
      } else if (ctx.DASH() != null) {
        return new DecrementLocalPreference(val);
      } else {
        return new LiteralInt(val);
      }
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarInt(ctx.RP_VARIABLE().getText());
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

  public long toLong(CommunityContext ctx) {
    if (ctx.COMMUNITY_NUMBER() != null) {
      String numberText = ctx.com.getText();
      String[] parts = numberText.split(":");
      String leftStr = parts[0];
      String rightStr = parts[1];
      long left = Long.parseLong(leftStr);
      long right = Long.parseLong(rightStr);
      return (left << 16) | right;
    } else if (ctx.DEC() != null) {
      return toLong(ctx.com);
    } else if (ctx.INTERNET() != null) {
      return WellKnownCommunity.INTERNET;
    } else if (ctx.GSHUT() != null) {
      return WellKnownCommunity.GRACEFUL_SHUTDOWN;
    } else if (ctx.LOCAL_AS() != null) {
      // Cisco LOCAL_AS is interpreted as RFC1997 NO_EXPORT_SUBCONFED: internet forums.
      return WellKnownCommunity.NO_EXPORT_SUBCONFED;
    } else if (ctx.NO_ADVERTISE() != null) {
      return WellKnownCommunity.NO_ADVERTISE;
    } else if (ctx.NO_EXPORT() != null) {
      return WellKnownCommunity.NO_EXPORT;
    } else {
      throw convError(Long.class, ctx);
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
    if (ctx.AOL() != null) {
      return NamedPort.AOL;
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
    } else if (ctx.CITRIX_ICA() != null) {
      return NamedPort.CITRIX_ICA;
    } else if (ctx.CMD() != null) {
      return NamedPort.CMDtcp_OR_SYSLOGudp;
    } else if (ctx.CTIQBE() != null) {
      return NamedPort.CTIQBE;
    } else if (ctx.DAYTIME() != null) {
      return NamedPort.DAYTIME;
    } else if (ctx.DISCARD() != null) {
      return NamedPort.DISCARD;
    } else if (ctx.DNSIX() != null) {
      return NamedPort.DNSIX;
    } else if (ctx.DOMAIN() != null) {
      return NamedPort.DOMAIN;
    } else if (ctx.ECHO() != null) {
      return NamedPort.ECHO;
    } else if (ctx.EXEC() != null) {
      return NamedPort.BIFFudp_OR_EXECtcp;
    } else if (ctx.FINGER() != null) {
      return NamedPort.FINGER;
    } else if (ctx.FTP() != null) {
      return NamedPort.FTP;
    } else if (ctx.FTP_DATA() != null) {
      return NamedPort.FTP_DATA;
    } else if (ctx.GOPHER() != null) {
      return NamedPort.GOPHER;
    } else if (ctx.H323() != null) {
      return NamedPort.H323;
    } else if (ctx.HTTPS() != null) {
      return NamedPort.HTTPS;
    } else if (ctx.HOSTNAME() != null) {
      return NamedPort.HOSTNAME;
    } else if (ctx.IDENT() != null) {
      return NamedPort.IDENT;
    } else if (ctx.IMAP4() != null) {
      return NamedPort.IMAP;
    } else if (ctx.IRC() != null) {
      return NamedPort.IRC;
    } else if (ctx.ISAKMP() != null) {
      return NamedPort.ISAKMP;
    } else if (ctx.KERBEROS() != null) {
      return NamedPort.KERBEROS;
    } else if (ctx.KLOGIN() != null) {
      return NamedPort.KLOGIN;
    } else if (ctx.KSHELL() != null) {
      return NamedPort.KSHELL;
    } else if (ctx.LDAP() != null) {
      return NamedPort.LDAP;
    } else if (ctx.LDAPS() != null) {
      return NamedPort.LDAPS;
    } else if (ctx.LDP() != null) {
      return NamedPort.LDP;
    } else if (ctx.LPD() != null) {
      return NamedPort.LPD;
    } else if (ctx.LOGIN() != null) {
      return NamedPort.LOGINtcp_OR_WHOudp;
    } else if (ctx.LOTUSNOTES() != null) {
      return NamedPort.LOTUSNOTES;
    } else if (ctx.MICROSOFT_DS() != null) {
      return NamedPort.MICROSOFT_DS;
    } else if (ctx.MLAG() != null) {
      return NamedPort.MLAG;
    } else if (ctx.MOBILE_IP() != null) {
      return NamedPort.MOBILE_IP_AGENT;
    } else if (ctx.MSRPC() != null) {
      return NamedPort.MSRPC;
    } else if (ctx.NAMESERVER() != null) {
      return NamedPort.NAMESERVER;
    } else if (ctx.NETBIOS_DGM() != null) {
      return NamedPort.NETBIOS_DGM;
    } else if (ctx.NETBIOS_NS() != null) {
      return NamedPort.NETBIOS_NS;
    } else if (ctx.NETBIOS_SS() != null) {
      return NamedPort.NETBIOS_SSN;
    } else if (ctx.NETBIOS_SSN() != null) {
      return NamedPort.NETBIOS_SSN;
    } else if (ctx.NNTP() != null) {
      return NamedPort.NNTP;
    } else if (ctx.NON500_ISAKMP() != null) {
      return NamedPort.NON500_ISAKMP;
    } else if (ctx.NTP() != null) {
      return NamedPort.NTP;
    } else if (ctx.PCANYWHERE_DATA() != null) {
      return NamedPort.PCANYWHERE_DATA;
    } else if (ctx.PCANYWHERE_STATUS() != null) {
      return NamedPort.PCANYWHERE_STATUS;
    } else if (ctx.PIM_AUTO_RP() != null) {
      return NamedPort.PIM_AUTO_RP;
    } else if (ctx.POP2() != null) {
      return NamedPort.POP2;
    } else if (ctx.POP3() != null) {
      return NamedPort.POP3;
    } else if (ctx.PPTP() != null) {
      return NamedPort.PPTP;
    } else if (ctx.RADIUS() != null) {
      return NamedPort.RADIUS_CISCO;
    } else if (ctx.RADIUS_ACCT() != null) {
      return NamedPort.RADIUS_ACCT_CISCO;
    } else if (ctx.RIP() != null) {
      return NamedPort.RIP;
    } else if (ctx.SECUREID_UDP() != null) {
      return NamedPort.SECUREID_UDP;
    } else if (ctx.SMTP() != null) {
      return NamedPort.SMTP;
    } else if (ctx.SNMP() != null) {
      return NamedPort.SNMP;
    } else if (ctx.SNMP_TRAP() != null) {
      return NamedPort.SNMPTRAP;
    } else if (ctx.SNMPTRAP() != null) {
      return NamedPort.SNMPTRAP;
    } else if (ctx.SQLNET() != null) {
      return NamedPort.SQLNET;
    } else if (ctx.SSH() != null) {
      return NamedPort.SSH;
    } else if (ctx.SUNRPC() != null) {
      return NamedPort.SUNRPC;
    } else if (ctx.SYSLOG() != null) {
      return NamedPort.CMDtcp_OR_SYSLOGudp;
    } else if (ctx.TACACS() != null) {
      return NamedPort.TACACS;
    } else if (ctx.TACACS_DS() != null) {
      return NamedPort.TACACS_DS;
    } else if (ctx.TALK() != null) {
      return NamedPort.TALK;
    } else if (ctx.TELNET() != null) {
      return NamedPort.TELNET;
    } else if (ctx.TFTP() != null) {
      return NamedPort.TFTP;
    } else if (ctx.TIME() != null) {
      return NamedPort.TIME;
    } else if (ctx.UUCP() != null) {
      return NamedPort.UUCP;
    } else if (ctx.WHO() != null) {
      return NamedPort.LOGINtcp_OR_WHOudp;
    } else if (ctx.WHOIS() != null) {
      return NamedPort.WHOIS;
    } else if (ctx.WWW() != null) {
      return NamedPort.HTTP;
    } else if (ctx.XDMCP() != null) {
      return NamedPort.XDMCP;
    } else {
      throw convError(NamedPort.class, ctx);
    }
  }

  private OriginExpr toOriginExpr(Origin_expr_literalContext ctx) {
    OriginType originType;
    Integer asNum = null;
    LiteralOrigin originExpr;
    if (ctx.IGP() != null) {
      originType = OriginType.IGP;
    } else if (ctx.INCOMPLETE() != null) {
      originType = OriginType.INCOMPLETE;
    } else if (ctx.as != null) {
      asNum = toInteger(ctx.as);
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
    List<SubRange> ranges = new ArrayList<>();
    if (ps.EQ() != null) {
      for (PortContext pc : ps.args) {
        int port = getPortNumber(pc);
        ranges.add(new SubRange(port, port));
      }
    } else if (ps.GT() != null) {
      int port = getPortNumber(ps.arg);
      ranges.add(new SubRange(port + 1, 65535));
    } else if (ps.NEQ() != null) {
      int port = getPortNumber(ps.arg);
      SubRange beforeRange = new SubRange(0, port - 1);
      SubRange afterRange = new SubRange(port + 1, 65535);
      ranges.add(beforeRange);
      ranges.add(afterRange);
    } else if (ps.LT() != null) {
      int port = getPortNumber(ps.arg);
      ranges.add(new SubRange(0, port - 1));
    } else if (ps.RANGE() != null) {
      int lowPort = getPortNumber(ps.arg1);
      int highPort = getPortNumber(ps.arg2);
      ranges.add(new SubRange(lowPort, highPort));
    } else {
      throw convError(List.class, ps);
    }
    return ranges;
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
    return new RoutePolicyBooleanApply(ctx.name.getText());
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
        ctx.as_range_expr()
            .subranges
            .stream()
            .map(this::toSubRangeExpr)
            .collect(Collectors.toList());
    boolean exact = ctx.as_range_expr().EXACT() != null;
    return new RoutePolicyBooleanAsPathNeighborIs(range, exact);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_as_path_originates_from_rp_stanzaContext ctx) {
    List<SubRangeExpr> range =
        ctx.as_range_expr()
            .subranges
            .stream()
            .map(this::toSubRangeExpr)
            .collect(Collectors.toList());
    boolean exact = ctx.as_range_expr().EXACT() != null;
    return new RoutePolicyBooleanAsPathOriginatesFrom(range, exact);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_as_path_passes_through_rp_stanzaContext ctx) {
    List<SubRangeExpr> range =
        ctx.as_range_expr()
            .subranges
            .stream()
            .map(this::toSubRangeExpr)
            .collect(Collectors.toList());
    boolean exact = ctx.as_range_expr().EXACT() != null;
    return new RoutePolicyBooleanAsPathPassesThrough(range, exact);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_community_matches_any_rp_stanzaContext ctx) {
    RoutePolicyCommunitySet communitySet =
        toRoutePolicyCommunitySet(ctx.rp_community_set(), ROUTE_POLICY_COMMUNITY_MATCHES_ANY);
    return new RoutePolicyBooleanCommunityMatchesAny(communitySet);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_community_matches_every_rp_stanzaContext ctx) {
    RoutePolicyCommunitySet communitySet =
        toRoutePolicyCommunitySet(ctx.rp_community_set(), ROUTE_POLICY_COMMUNITY_MATCHES_EVERY);
    return new RoutePolicyBooleanCommunityMatchesEvery(communitySet);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_destination_rp_stanzaContext ctx) {
    RoutePolicyPrefixSet prefixSet = toRoutePolicyPrefixSet(ctx.rp_prefix_set());
    return new RoutePolicyBooleanDestination(prefixSet);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_local_preference_rp_stanzaContext ctx) {
    IntComparator cmp = toIntComparator(ctx.int_comp());
    IntExpr rhs = toCommonIntExpr(ctx.rhs);
    return new RoutePolicyBooleanLocalPreference(cmp, rhs);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_med_rp_stanzaContext ctx) {
    IntComparator cmp = toIntComparator(ctx.int_comp());
    IntExpr rhs = toCommonIntExpr(ctx.rhs);
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
    IntExpr rhs = toTagIntExpr(ctx.int_expr());
    return new RoutePolicyBooleanTagIs(cmp, rhs);
  }

  private RoutePolicyCommunitySet toRoutePolicyCommunitySet(
      Rp_community_setContext ctx, CiscoStructureUsage usage) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      _configuration.referenceStructure(COMMUNITY_SET, name, usage, ctx.name.getStart().getLine());
      return new RoutePolicyCommunitySetName(name);
    } else {
      // inline
      return new RoutePolicyCommunitySetInline(
          ctx.elems.stream().map(this::toCommunitySetElemExpr).collect(Collectors.toList()));
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
          prefix = new Prefix(toIp(pctxt.ipa), Prefix.MAX_PREFIX_LENGTH);
          lower = prefix.getPrefixLength();
          upper = Prefix.MAX_PREFIX_LENGTH;
        } else if (pctxt.ipv6a != null) {
          prefix6 = new Prefix6(toIp6(pctxt.ipv6a), Prefix6.MAX_PREFIX_LENGTH);
          lower = prefix6.getPrefixLength();
          upper = Prefix6.MAX_PREFIX_LENGTH;
        } else if (pctxt.ipv6_prefix != null) {
          prefix6 = new Prefix6(pctxt.ipv6_prefix.getText());
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

  private RoutePolicyStatement toRoutePolicyStatement(Delete_rp_stanzaContext ctx) {
    if (ctx.ALL() != null) {
      return new RoutePolicyDeleteAllStatement();
    } else {
      boolean negated = (ctx.NOT() != null);
      return new RoutePolicyDeleteCommunityStatement(
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

    Delete_rp_stanzaContext dectx = ctx.delete_rp_stanza();
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
    RoutePolicyCommunitySet cset =
        toRoutePolicyCommunitySet(ctx.rp_community_set(), ROUTE_POLICY_SET_COMMUNITY);
    boolean additive = (ctx.ADDITIVE() != null);
    return new RoutePolicySetCommunity(cset, additive);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_isis_metric_rp_stanzaContext ctx) {
    LongExpr metric = toCommonLongExpr(ctx.int_expr());
    return new RoutePolicySetIsisMetric(metric);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_level_rp_stanzaContext ctx) {
    return new RoutePolicySetLevel(toIsisLevelExpr(ctx.isis_level_expr()));
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_local_preference_rp_stanzaContext ctx) {
    return new RoutePolicySetLocalPref(toLocalPreferenceIntExpr(ctx.pref));
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
      _w.redFlag("Unimplemented 'destination-vrf' directive in: " + getFullText(ctx));
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
    if (!_unrecognizedAsRedFlag) {
      throw convError(returnType, ctx);
    }
    _w.redFlag(convErrorMessage(returnType, ctx));
    return defaultReturnValue;
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_tag_rp_stanzaContext ctx) {
    IntExpr tag = toTagIntExpr(ctx.tag);
    return new RoutePolicySetTag(tag);
  }

  private RoutePolicyStatement toRoutePolicyStatement(Set_weight_rp_stanzaContext wctx) {
    IntExpr weight = toCommonIntExpr(wctx.weight);
    return new RoutePolicySetWeight(weight);
  }

  private List<RoutePolicyStatement> toRoutePolicyStatementList(List<Rp_stanzaContext> ctxts) {
    return ctxts.stream().map(this::toRoutePolicyStatement).collect(Collectors.toList());
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

  private IntExpr toTagIntExpr(Int_exprContext ctx) {
    if (ctx.DEC() != null && ctx.DASH() == null && ctx.PLUS() == null) {
      int val = toInteger(ctx.DEC());
      return new LiteralInt(val);
    } else if (ctx.RP_VARIABLE() != null) {
      String var = ctx.RP_VARIABLE().getText();
      return new VarInt(var);
    } else {
      throw convError(IntExpr.class, ctx);
    }
  }

  private void warnObjectGroupRedefinition(String name) {
    _w.redFlag("Object group defined multiple times: '" + name + "'");
  }

  @Override
  public void visitErrorNode(ErrorNode errorNode) {
    Token token = errorNode.getSymbol();
    String lineText = errorNode.getText().replace("\n", "").replace("\r", "").trim();
    int line = token.getLine();
    String msg = String.format("Unrecognized Line: %d: %s", line, lineText);
    if (_unrecognizedAsRedFlag) {
      _w.redFlag(msg + " SUBSEQUENT LINES MAY NOT BE PROCESSED CORRECTLY");
      _configuration.setUnrecognized(true);
    } else {
      _parser.getErrors().add(msg);
    }
  }
}

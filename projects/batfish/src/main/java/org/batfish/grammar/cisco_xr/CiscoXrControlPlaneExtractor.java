package org.batfish.grammar.cisco_xr;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toCollection;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.AS_PATH_SET;
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
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.ETHERNET_SERVICES_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.EXTCOMMUNITY_SET_RT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.FLOW_EXPORTER_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.FLOW_MONITOR_MAP;
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
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.NAMED_RSA_PUB_KEY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.NETWORK_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.POLICY_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PREFIX6_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PREFIX_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.PREFIX_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.RD_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.SAMPLER_MAP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.SERVICE_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureType.TRACK;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_ADDITIONAL_PATHS_SELECTION_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_AGGREGATE_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_DEFAULT_ORIGINATE_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_INHERITED_PEER_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_INHERITED_SESSION;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_LISTEN_RANGE_PEER_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_PEER_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_ROUTE_POLICY_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NEIGHBOR_ROUTE_POLICY_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_NETWORK_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_REDISTRIBUTE_CONNECTED_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_REDISTRIBUTE_STATIC_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_UPDATE_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_USE_AF_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_USE_NEIGHBOR_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BGP_USE_SESSION_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BRIDGE_DOMAIN_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.BRIDGE_DOMAIN_ROUTED_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CLASS_MAP_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CLASS_MAP_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CLASS_MAP_ACTIVATED_SERVICE_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CLASS_MAP_SERVICE_TEMPLATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CONTROL_PLANE_SERVICE_POLICY_INPUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CONTROL_PLANE_SERVICE_POLICY_OUTPUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_DYNAMIC_MAP_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_DYNAMIC_MAP_ISAKMP_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_DYNAMIC_MAP_TRANSFORM_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_CRYPTO_DYNAMIC_MAP_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ISAKMP_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_TRANSFORM_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.DOMAIN_LOOKUP_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EIGRP_AF_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.EIGRP_PASSIVE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.FLOW_MONITOR_MAP_EXPORTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV4_MONITOR_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV4_MONITOR_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV4_SAMPLER_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV4_SAMPLER_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV6_MONITOR_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV6_MONITOR_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV6_SAMPLER_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_FLOW_IPV6_SAMPLER_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IGMP_ACCESS_GROUP_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IGMP_HOST_PROXY_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IGMP_STATIC_GROUP_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV4_ACCESS_GROUP_COMMON;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV4_ACCESS_GROUP_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV4_ACCESS_GROUP_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV6_ACCESS_GROUP_COMMON;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV6_ACCESS_GROUP_EGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IPV6_ACCESS_GROUP_INGRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_IP_VERIFY_ACCESS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_PIM_NEIGHBOR_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_SELF_REF;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_SERVICE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.INTERFACE_STANDBY_TRACK;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.IPSEC_PROFILE_ISAKMP_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.IPSEC_PROFILE_TRANSFORM_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ISAKMP_POLICY_SELF_REF;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ISAKMP_PROFILE_KEYRING;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ISAKMP_PROFILE_SELF_REF;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.LINE_ACCESS_CLASS_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.LINE_ACCESS_CLASS_LIST6;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MPLS_LDP_AF_IPV4_DISCOVERY_TARGETED_HELLO_ACCEPT_FROM;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MPLS_LDP_AF_IPV4_REDISTRIBUTE_BGP_ADVERTISE_TO;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MPLS_LDP_AF_IPV6_DISCOVERY_TARGETED_HELLO_ACCEPT_FROM;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MPLS_LDP_AF_IPV6_REDISTRIBUTE_BGP_ADVERTISE_TO;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.MULTICAST_ROUTING_CORE_TREE_PROTOCOL_RSVP_TE_GROUP_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NAMED_RSA_PUB_KEY_SELF_REF;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NETWORK_OBJECT_GROUP_GROUP_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NETWORK_OBJECT_GROUP_NETWORK_OBJECT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_ACCESS_GROUP_PEER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_ACCESS_GROUP_QUERY_ONLY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_ACCESS_GROUP_SERVE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_ACCESS_GROUP_SERVE_ONLY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.NTP_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_AREA_FILTER_LIST;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_AREA_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_DEFAULT_INFORMATION_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_OUT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_DISTRIBUTE_LIST_ROUTE_POLICY_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.OSPF_REDISTRIBUTE_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.POLICY_MAP_EVENT_CLASS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.POLICY_MAP_EVENT_CLASS_ACTIVATE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_IGMP_ACCESS_GROUP;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_IGMP_EXPLICIT_TRACKING;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_IGMP_MAXIMUM_GROUPS_PER_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_IGMP_SSM_MAP_STATIC;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_ISIS_REDISTRIBUTE_CONNECTED_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_ISIS_REDISTRIBUTE_STATIC_ROUTE_POLICY;
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
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_EMBEDDED_RP_RENDEZVOUS_POINT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_INTERFACE_NEIGHBOR_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_MDT_NEIGHBOR_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_MOFRR_FLOW;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_MOFRR_RIB;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_NEIGHBOR_FILTER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_RPF_REDIRECT_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_RPF_TOPOLOGY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_RP_ADDRESS;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_RP_STATIC_DENY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_SG_EXPIRY_TIMER;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_SPT_THRESHOLD;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_PIM_SSM_THRESHOLD_RANGE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_STATIC_ROUTE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTER_VRRP_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_APPLY_EXPR;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_APPLY_STATEMENT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_AS_PATH_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_COMMUNITY_MATCHES_ANY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_COMMUNITY_MATCHES_EVERY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_DELETE_COMMUNITY_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_PREFIX_SET;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_RD_IN;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_SET_COMMUNITY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.ROUTE_POLICY_SET_EXTCOMMUNITY_RT;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SERVICE_POLICY_GLOBAL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_COMMUNITY_ACL4;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_COMMUNITY_ACL6;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SNMP_SERVER_TRAP_SOURCE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SSH_IPV4_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.SSH_IPV6_ACL;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.TACACS_SOURCE_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.TRACK_INTERFACE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.TUNNEL_PROTECTION_IPSEC_PROFILE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.TUNNEL_SOURCE;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.VRF_EXPORT_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.VRF_EXPORT_TO_DEFAULT_VRF_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.VRF_IMPORT_FROM_DEFAULT_VRF_ROUTE_POLICY;
import static org.batfish.representation.cisco_xr.CiscoXrStructureUsage.VRF_IMPORT_ROUTE_POLICY;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
import org.batfish.datamodel.Icmp6Code;
import org.batfish.datamodel.Icmp6Type;
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
import org.batfish.datamodel.LongSpace;
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
import org.batfish.datamodel.ospf.OspfAreaSummary.SummaryRouteBehavior;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.routing_policy.expr.AsExpr;
import org.batfish.datamodel.routing_policy.expr.AutoAs;
import org.batfish.datamodel.routing_policy.expr.DecrementLocalPreference;
import org.batfish.datamodel.routing_policy.expr.DecrementMetric;
import org.batfish.datamodel.routing_policy.expr.ExplicitAs;
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
import org.batfish.datamodel.routing_policy.expr.OriginExpr;
import org.batfish.datamodel.routing_policy.expr.RouteType;
import org.batfish.datamodel.routing_policy.expr.RouteTypeExpr;
import org.batfish.datamodel.routing_policy.expr.VarAs;
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
import org.batfish.datamodel.vendor_family.cisco_xr.User;
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.SilentSyntaxListener;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.Access_list_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Activate_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Additional_paths_selection_xr_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Address_family_headerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Address_family_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Af_group_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aggregate_address_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Allowas_in_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Always_compare_med_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Apply_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_numberContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_number_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_path_lengthContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_path_length_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_path_multipath_relax_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_path_regexContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_path_set_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_rangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_range_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_range_expr_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.As_range_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspse_dfa_regexContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspse_ios_regexContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspse_lengthContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspse_neighbor_isContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspse_originates_fromContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspse_passes_throughContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspse_unique_lengthContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspsee_dfa_regexContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspsee_ios_regexContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspsee_lengthContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspsee_neighbor_isContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspsee_originates_fromContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspsee_passes_throughContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Aspsee_unique_lengthContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Auto_summary_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_advertise_inactive_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_asnContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_confederation_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_listen_range_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_redistribute_internal_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_vrf_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bgp_vrf_rdContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_and_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_apply_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_pathContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_inContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_is_localContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_lengthContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_neighbor_isContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_originates_fromContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_passes_throughContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_as_path_unique_lengthContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_community_matches_any_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_community_matches_every_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_destination_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_local_preference_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_med_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_next_hop_in_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_not_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_rd_in_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_rib_has_route_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_route_type_is_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_simple_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_tag_is_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Boolean_validation_state_is_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bridge_domain_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Bridge_group_nameContext;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cm_matchContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cmm_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cmm_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cmm_activated_service_templateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Cmm_service_templateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_elemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_elem_halfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_expr_elemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_expr_elem_halfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_match_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_match_expr_elemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_match_expr_elem_halfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Community_set_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.ComparatorContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Compare_routerid_rb_stanzaContext;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.Domain_lookupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Domain_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Domain_name_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Dscp_numContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Dscp_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ebgp_multihop_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Eigrp_asnContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Eigrp_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Else_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Elseif_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Empty_neighbor_block_address_familyContext;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.Flow_exporter_mapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Flow_exporter_map_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Flow_monitor_mapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Flow_monitor_map_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Fmm_exporterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Hash_commentContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Host_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_autostateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_bandwidthContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_bundle_idContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_crypto_mapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_delayContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_descriptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_encapsulationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_flowContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_forwardContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_helper_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_igmpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_pim_neighbor_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_proxy_arpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_router_isisContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_router_ospf_areaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_summary_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ip_verifyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ipv4_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ipv4_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_ipv6_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_isis_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_mtuContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_rewrite_ingress_tagContext;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.If_vrrpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifdhcpr_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifdhcpr_clientContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifigmp_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifigmphp_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifigmpsg_aclContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifrit_policyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifrit_popContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ifrit_pop_countContext;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.Int_compContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Int_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Interface_ipv4_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Interface_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Interface_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ios_banner_headerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ios_delimited_bannerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_prefix_list_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ip_prefix_list_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipsec_authenticationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipsec_encryptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv4_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv4_conflict_policyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv4_nexthop1Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv4_nexthop2Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv4_nexthop3Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv4_nexthopContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_access_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_nexthop1Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_nexthop2Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_nexthop3Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_nexthopContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_prefix_list_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ipv6_prefix_list_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Is_type_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Isis_levelContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Isis_level_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.L2vpn_bridge_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.L_access_classContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.L_exec_timeoutContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.L_login_authenticationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.L_transportContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Lbg_bridge_domainContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Lbgbd_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Lbgbd_routed_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Literal_communityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Local_as_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_buffer_sizeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_buffered_buffer_sizeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_buffered_set_severityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_buffered_severityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_console_set_severityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_console_severityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_source_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_trap_set_severityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Logging_trap_severityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Match_semanticsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Maximum_paths_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Maximum_peers_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Mldp_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Mldpafd_targeted_helloContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Mldpafrb_advertise_toContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Mr_ipv6Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Mrv_ipv6Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Mrvaf_core_tree_protocolContext;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.Og_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogn_group_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogn_host_ipContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogn_ip_with_maskContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ogn_network_objectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Origin_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Origin_expr_literalContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ospf_areaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ospf_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ospf_metric_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ospf_network_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.ParameterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Passive_iis_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Passive_interface_default_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Passive_interface_is_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Peer_group_assignment_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Peer_group_creation_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Pint16Context;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.Prefix_set_elemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Prefix_set_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Prepend_as_path_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.ProtocolContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.RangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rd_match_exprContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rd_set_elemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rd_set_elem_32Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rd_set_elem_asdotContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rd_set_elem_lo16Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rd_set_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_autonomous_systemContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_classicContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Re_default_metricContext;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rigmp_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rigmp_explicit_trackingContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rigmpi_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rigmpi_explicit_trackingContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rigmpi_maximumContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rigmpm_groups_per_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rigmps_staticContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rmld_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rmld_explicit_trackingContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rmld_ssm_staticContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rmldi_access_groupContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rmldi_explicit_trackingContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rmldm_groups_per_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rmsdp_cache_sa_stateContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rmsdp_sa_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rmsdpp_sa_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_areaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_area_filterlistContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_area_nssaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_area_stubContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_auto_costContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_default_informationContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_default_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_max_metricContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_maximum_pathsContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_passive_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_passive_interface_defaultContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_rfc1583_compatibilityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_router_idContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ro_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roa_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roa_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roa_rangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rodl_aclContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rodl_route_policyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roi_costContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roi_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Roi_passiveContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ror_routing_instanceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ror_routing_instance_nullContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rorri_protocolContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_distinguisherContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_policy_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_policy_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_policy_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_reflector_client_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_tag_from_0Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Route_targetContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Router_bgp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Router_id_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Router_isis_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_extcommunity_set_rtContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_isis_metric_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_metric_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_ospf_metric_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_prefix_setContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rp_route_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_accept_registerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_allow_rp_group_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_allow_rp_rp_listContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_bsr_candidate_rpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_mdt_neighbor_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_mofrr_flowContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_mofrr_ribContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_neighbor_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_rp_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_rp_static_denyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_rpfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_sg_expiry_timerContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_spt_thresholdContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpim_ssm_threshold_rangeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpimaf4_auto_rp_candidate_rpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpimaf4_rpf_redirectContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpimaf6_embedded_rp_rendezvous_pointContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpimaf_ipv6Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rpimafi_neighbor_filterContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rr_networkContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rr_passive_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rr_passive_interface_defaultContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rs_routeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Rs_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_aaaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_as_path_setContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_banner_iosContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_class_mapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ethernet_servicesContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_hostnameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_interfaceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_l2tp_classContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_lineContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_loggingContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_ntpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_rd_setContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_router_ospfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_router_ripContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_sampler_mapContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_serviceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_service_policy_globalContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_snmp_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_spanning_treeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_tacacs_serverContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_trackContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_usernameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.S_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Sampler_map_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Send_community_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Session_group_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_community_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_extcommunity_rtContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_isis_metric_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_level_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_local_preference_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_med_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_metric_type_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_next_hop_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_next_hop_self_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_origin_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_path_selection_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_tag_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Set_weight_rp_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Shutdown_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Spanning_tree_portfastContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_communityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_hostContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ss_trap_sourceContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ssc_access_controlContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Ssc_use_ipv4_aclContext;
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
import org.batfish.grammar.cisco_xr.CiscoXrParser.Uint8Context;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Uint_legacyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Update_source_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Use_af_group_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Use_neighbor_group_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Use_session_group_bgp_tailContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.VariableContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Viaf_vrrpContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Viafv_addressContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Viafv_preemptContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Viafv_priorityContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vlan_idContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_address_familyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_address_family_typeContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_afe_route_policyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_afe_route_target_valueContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_afet_default_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_afet_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_afi_route_policyContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_afi_route_target_valueContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_afif_default_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_afif_vrfContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_block_rb_stanzaContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_descriptionContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrf_nameContext;
import org.batfish.grammar.cisco_xr.CiscoXrParser.Vrrp_interfaceContext;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.representation.cisco_xr.AccessListAddressSpecifier;
import org.batfish.representation.cisco_xr.AccessListServiceSpecifier;
import org.batfish.representation.cisco_xr.AddressFamilyType;
import org.batfish.representation.cisco_xr.AsPathSet;
import org.batfish.representation.cisco_xr.AsPathSetExpr;
import org.batfish.representation.cisco_xr.AsPathSetReference;
import org.batfish.representation.cisco_xr.AsPathSetVariable;
import org.batfish.representation.cisco_xr.BgpAggregateIpv4Network;
import org.batfish.representation.cisco_xr.BgpAggregateIpv6Network;
import org.batfish.representation.cisco_xr.BgpNetwork;
import org.batfish.representation.cisco_xr.BgpNetwork6;
import org.batfish.representation.cisco_xr.BgpPeerGroup;
import org.batfish.representation.cisco_xr.BgpProcess;
import org.batfish.representation.cisco_xr.BgpRedistributionPolicy;
import org.batfish.representation.cisco_xr.BridgeDomain;
import org.batfish.representation.cisco_xr.BridgeGroup;
import org.batfish.representation.cisco_xr.CiscoXrConfiguration;
import org.batfish.representation.cisco_xr.CiscoXrStructureType;
import org.batfish.representation.cisco_xr.CiscoXrStructureUsage;
import org.batfish.representation.cisco_xr.CryptoMapEntry;
import org.batfish.representation.cisco_xr.CryptoMapSet;
import org.batfish.representation.cisco_xr.DfaRegexAsPathSetElem;
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
import org.batfish.representation.cisco_xr.HsrpGroup;
import org.batfish.representation.cisco_xr.InlineAsPathSet;
import org.batfish.representation.cisco_xr.InlineExtcommunitySetRt;
import org.batfish.representation.cisco_xr.Interface;
import org.batfish.representation.cisco_xr.IosRegexAsPathSetElem;
import org.batfish.representation.cisco_xr.IpBgpPeerGroup;
import org.batfish.representation.cisco_xr.IpsecProfile;
import org.batfish.representation.cisco_xr.IpsecTransformSet;
import org.batfish.representation.cisco_xr.Ipv4AccessList;
import org.batfish.representation.cisco_xr.Ipv4AccessListLine;
import org.batfish.representation.cisco_xr.Ipv4Nexthop;
import org.batfish.representation.cisco_xr.Ipv6AccessList;
import org.batfish.representation.cisco_xr.Ipv6AccessListLine;
import org.batfish.representation.cisco_xr.Ipv6BgpPeerGroup;
import org.batfish.representation.cisco_xr.Ipv6Nexthop;
import org.batfish.representation.cisco_xr.IsakmpKey;
import org.batfish.representation.cisco_xr.IsakmpPolicy;
import org.batfish.representation.cisco_xr.IsakmpProfile;
import org.batfish.representation.cisco_xr.IsisProcess;
import org.batfish.representation.cisco_xr.IsisRedistributionPolicy;
import org.batfish.representation.cisco_xr.Keyring;
import org.batfish.representation.cisco_xr.LengthAsPathSetElem;
import org.batfish.representation.cisco_xr.LiteralUint16;
import org.batfish.representation.cisco_xr.LiteralUint16Range;
import org.batfish.representation.cisco_xr.LiteralUint32;
import org.batfish.representation.cisco_xr.MasterBgpPeerGroup;
import org.batfish.representation.cisco_xr.MatchSemantics;
import org.batfish.representation.cisco_xr.NamedBgpPeerGroup;
import org.batfish.representation.cisco_xr.NamedRsaPubKey;
import org.batfish.representation.cisco_xr.NeighborIsAsPathSetElem;
import org.batfish.representation.cisco_xr.NetworkObjectGroup;
import org.batfish.representation.cisco_xr.NssaSettings;
import org.batfish.representation.cisco_xr.OriginatesFromAsPathSetElem;
import org.batfish.representation.cisco_xr.OspfNetwork;
import org.batfish.representation.cisco_xr.OspfNetworkType;
import org.batfish.representation.cisco_xr.OspfProcess;
import org.batfish.representation.cisco_xr.OspfRedistributionPolicy;
import org.batfish.representation.cisco_xr.PassesThroughAsPathSetElem;
import org.batfish.representation.cisco_xr.PeerAs;
import org.batfish.representation.cisco_xr.Prefix6List;
import org.batfish.representation.cisco_xr.Prefix6ListLine;
import org.batfish.representation.cisco_xr.PrefixList;
import org.batfish.representation.cisco_xr.PrefixListLine;
import org.batfish.representation.cisco_xr.PrivateAs;
import org.batfish.representation.cisco_xr.RdMatchExpr;
import org.batfish.representation.cisco_xr.RdSet;
import org.batfish.representation.cisco_xr.RdSetAsDot;
import org.batfish.representation.cisco_xr.RdSetAsPlain16;
import org.batfish.representation.cisco_xr.RdSetAsPlain32;
import org.batfish.representation.cisco_xr.RdSetDfaRegex;
import org.batfish.representation.cisco_xr.RdSetElem;
import org.batfish.representation.cisco_xr.RdSetIosRegex;
import org.batfish.representation.cisco_xr.RdSetIpAddress;
import org.batfish.representation.cisco_xr.RdSetIpPrefix;
import org.batfish.representation.cisco_xr.RdSetParameterReference;
import org.batfish.representation.cisco_xr.RdSetReference;
import org.batfish.representation.cisco_xr.RipProcess;
import org.batfish.representation.cisco_xr.RoutePolicy;
import org.batfish.representation.cisco_xr.RoutePolicyApplyStatement;
import org.batfish.representation.cisco_xr.RoutePolicyBoolean;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAnd;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanApply;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathIn;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathIsLocal;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathLength;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathNeighborIs;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathOriginatesFrom;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathPassesThrough;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanAsPathUniqueLength;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanDestination;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanLocalPreference;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanMed;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanNextHopIn;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanNot;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanOr;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanRdIn;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanRibHasRoute;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanRouteTypeIs;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanTagIs;
import org.batfish.representation.cisco_xr.RoutePolicyBooleanValidationStateIs;
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
import org.batfish.representation.cisco_xr.RoutePolicySetWeight;
import org.batfish.representation.cisco_xr.RoutePolicyStatement;
import org.batfish.representation.cisco_xr.SimpleExtendedAccessListServiceSpecifier;
import org.batfish.representation.cisco_xr.StaticRoute;
import org.batfish.representation.cisco_xr.StubSettings;
import org.batfish.representation.cisco_xr.TagRewritePolicy;
import org.batfish.representation.cisco_xr.TagRewritePop;
import org.batfish.representation.cisco_xr.Tunnel;
import org.batfish.representation.cisco_xr.Tunnel.TunnelMode;
import org.batfish.representation.cisco_xr.Uint16RangeExpr;
import org.batfish.representation.cisco_xr.Uint16Reference;
import org.batfish.representation.cisco_xr.Uint32RangeExpr;
import org.batfish.representation.cisco_xr.UnimplementedAccessListServiceSpecifier;
import org.batfish.representation.cisco_xr.UnimplementedBoolean;
import org.batfish.representation.cisco_xr.UniqueLengthAsPathSetElem;
import org.batfish.representation.cisco_xr.Vrf;
import org.batfish.representation.cisco_xr.VrfAddressFamily;
import org.batfish.representation.cisco_xr.VrrpGroup;
import org.batfish.representation.cisco_xr.VrrpInterface;
import org.batfish.representation.cisco_xr.WildcardAddressSpecifier;
import org.batfish.representation.cisco_xr.WildcardUint16RangeExpr;
import org.batfish.representation.cisco_xr.WildcardUint32RangeExpr;
import org.batfish.representation.cisco_xr.XrCommunitySet;
import org.batfish.representation.cisco_xr.XrCommunitySetDfaRegex;
import org.batfish.representation.cisco_xr.XrCommunitySetElem;
import org.batfish.representation.cisco_xr.XrCommunitySetExpr;
import org.batfish.representation.cisco_xr.XrCommunitySetHighLowRangeExprs;
import org.batfish.representation.cisco_xr.XrCommunitySetIosRegex;
import org.batfish.representation.cisco_xr.XrCommunitySetParameterReference;
import org.batfish.representation.cisco_xr.XrCommunitySetReference;
import org.batfish.representation.cisco_xr.XrInlineCommunitySet;
import org.batfish.representation.cisco_xr.XrRoutePolicyBooleanCommunityMatchesAny;
import org.batfish.representation.cisco_xr.XrRoutePolicyBooleanCommunityMatchesEvery;
import org.batfish.representation.cisco_xr.XrRoutePolicyDeleteAllStatement;
import org.batfish.representation.cisco_xr.XrRoutePolicyDeleteCommunityStatement;
import org.batfish.representation.cisco_xr.XrRoutePolicySetCommunity;
import org.batfish.representation.cisco_xr.XrWildcardCommunitySetElem;
import org.batfish.vendor.VendorConfiguration;

public class CiscoXrControlPlaneExtractor extends CiscoXrParserBaseListener
    implements ControlPlaneExtractor, SilentSyntaxListener {

  public static final IntegerSpace VLAN_RANGE = IntegerSpace.of(Range.closed(1, 4094));

  private static final LongSpace AS_NUMBER_RANGE = LongSpace.of(Range.closed(1L, 0xFFFFFFFFL));
  private static final IntegerSpace AS_PATH_LENGTH_RANGE = IntegerSpace.of(Range.closed(0, 2047));
  private static final IntegerSpace DSCP_RANGE = IntegerSpace.of(Range.closed(0, 63));
  private static final IntegerSpace EIGRP_ASN_RANGE = IntegerSpace.of(Range.closed(1, 65535));
  private static final IntegerSpace LOGGING_BUFFER_SIZE_RANGE =
      IntegerSpace.of(Range.closed(2097152, 125000000));
  private static final IntegerSpace OSPF_METRIC_RANGE = IntegerSpace.of(Range.closed(1, 16777214));
  private static final IntegerSpace OSPF_METRIC_TYPE_RANGE = IntegerSpace.of(Range.closed(1, 2));
  private static final IntegerSpace PINT16_RANGE = IntegerSpace.of(Range.closed(1, 65535));
  private static final IntegerSpace REWRITE_INGRESS_TAG_POP_RANGE =
      IntegerSpace.of(Range.closed(1, 2));

  private static final int DEFAULT_STATIC_ROUTE_DISTANCE = 1;

  public @Nullable OspfNetworkType toOspfNetworkType(Ospf_network_typeContext ctx) {
    if (ctx.POINT_TO_POINT() != null) {
      return OspfNetworkType.POINT_TO_POINT;
    } else if (ctx.BROADCAST() != null) {
      return OspfNetworkType.BROADCAST;
    } else if (ctx.POINT_TO_MULTIPOINT() != null) {
      if (ctx.NON_BROADCAST() != null) {
        return OspfNetworkType.POINT_TO_MULTIPOINT_NON_BROADCAST;
      } else {
        return OspfNetworkType.POINT_TO_MULTIPOINT;
      }
    } else if (ctx.NON_BROADCAST() != null) {
      return OspfNetworkType.NON_BROADCAST;
    } else {
      warn(ctx, "Cannot determine OSPF network type.");
      return null;
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

  private static int toInteger(Uint_legacyContext ctx) {
    return Integer.parseInt(ctx.getText());
  }

  @Nonnull
  private Optional<Integer> toInteger(ParserRuleContext messageCtx, Vlan_idContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, VLAN_RANGE, "VLAN ID");
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

  private static long toLong(Uint_legacyContext ctx) {
    return Long.parseLong(ctx.getText());
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

  private AsPathSet _currentAsPathSet;

  // TODO: create VS InlineAsPathSet supporting extra functionality
  private AsPathSet _currentInlineAsPathSet;

  // Temporary workaround to allow building as-path route-policy booleans with listener functions.
  // Can be removed when route-policy is no longer computed by recursion.
  private Map<ParserRuleContext, RoutePolicyBoolean> _asPathBooleansByCtx;

  private RoutePolicyBoolean _currentAsPathBoolean;

  @SuppressWarnings("unused")
  private List<AaaAccountingCommands> _currentAaaAccountingCommands;

  private AaaAuthenticationLoginList _currentAaaAuthenticationLoginList;

  private final Set<String> _currentBlockNeighborAddressFamilies;

  private BridgeDomain _currentBridgeDomain;

  private BridgeGroup _currentBridgeGroup;

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

  private Ipv4AccessListLine.Builder _currentIpv4AclLine;

  private Ipv6AccessList _currentIpv6Acl;

  private Ipv6AccessListLine.Builder _currentIpv6AclLine;

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

  private Stack<RoutePolicyIfStatement> _ifs;
  private Stack<RoutePolicyElseIfBlock> _elseIfs;
  private Stack<Consumer<RoutePolicyStatement>> _statementCollectors;

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

  private Boolean _inMplsLdpAddressFamilyIpv4;

  private boolean _no;

  @Nullable private EigrpProcess _parentEigrpProcess;

  private final CiscoXrCombinedParser _parser;

  private final List<BgpPeerGroup> _peerGroupStack;

  private final String _text;

  private final Warnings _w;

  @Nonnull private final SilentSyntaxCollection _silentSyntax;

  private NetworkObjectGroup _currentNetworkObjectGroup;

  private Integer _currentHsrpGroup;

  private String _currentTrackingGroup;

  /* Set this when moving to different stanzas (e.g., ro_vrf) inside "router ospf" stanza
   * to correctly retrieve the OSPF process that was being configured prior to switching stanzas
   */
  private String _lastKnownOspfProcess;

  private boolean _pimIpv6;

  private boolean _multicastRoutingIpv6;

  private VrfAddressFamily _currentVrfAddressFamily;

  public CiscoXrControlPlaneExtractor(
      String text,
      CiscoXrCombinedParser parser,
      ConfigurationFormat format,
      Warnings warnings,
      SilentSyntaxCollection silentSyntax) {
    _text = text;
    _parser = parser;
    _format = format;
    _w = warnings;
    _peerGroupStack = new ArrayList<>();
    _currentBlockNeighborAddressFamilies = new HashSet<>();
    _silentSyntax = silentSyntax;
    _asPathBooleansByCtx = new IdentityHashMap<>();
    _statementCollectors = new Stack<>();
    _ifs = new Stack<>();
    _elseIfs = new Stack<>();
  }

  private void addStatement(RoutePolicyStatement statement) {
    _statementCollectors.peek().accept(statement);
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
  public void enterVrf_address_family(Vrf_address_familyContext ctx) {
    _currentVrfAddressFamily =
        currentVrf()
            .getAddressFamilies()
            .computeIfAbsent(toAddressFamilyType(ctx.af), af -> new VrfAddressFamily());
  }

  @Override
  public void exitVrf_address_family(Vrf_address_familyContext ctx) {
    _currentVrfAddressFamily = null;
  }

  @Nonnull
  private static AddressFamilyType toAddressFamilyType(Vrf_address_family_typeContext ctx) {
    if (ctx.IPV4() != null) {
      if (ctx.FLOWSPEC() != null) {
        return AddressFamilyType.IPV4_FLOWSPEC;
      } else if (ctx.MULTICAST() != null) {
        return AddressFamilyType.IPV4_MULTICAST;
      } else {
        assert ctx.UNICAST() != null;
        return AddressFamilyType.IPV4_UNICAST;
      }
    } else {
      assert ctx.IPV6() != null;
      if (ctx.FLOWSPEC() != null) {
        return AddressFamilyType.IPV6_FLOWSPEC;
      } else if (ctx.MULTICAST() != null) {
        return AddressFamilyType.IPV6_MULTICAST;
      } else {
        assert ctx.UNICAST() != null;
        return AddressFamilyType.IPV6_UNICAST;
      }
    }
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
    String name = toString(ctx.name);
    _currentIpv4Acl = _configuration.getIpv4Acls().computeIfAbsent(name, Ipv4AccessList::new);
    _configuration.defineStructure(IPV4_ACCESS_LIST, name, ctx);
  }

  @Override
  public void exitIpv4_access_list(Ipv4_access_listContext ctx) {
    _currentIpv4Acl = null;
  }

  @Override
  public void enterIpv6_access_list(Ipv6_access_listContext ctx) {
    String name = toString(ctx.name);
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
    int encType = ctx.uint_legacy() != null ? toInteger(ctx.uint_legacy()) : 0;
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
        ISAKMP_POLICY, priority.toString(), ISAKMP_POLICY_SELF_REF, ctx.priority.start.getLine());
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
    int group = Integer.parseInt(ctx.uint_legacy().getText());
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
    _currentIsakmpPolicy.setLifetimeSeconds(Integer.parseInt(ctx.uint_legacy().getText()));
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
                        .map(this::toXrCommunitySetElem)
                        .collect(ImmutableList.toImmutableList())));
  }

  @Override
  public void enterS_rd_set(S_rd_setContext ctx) {
    todo(ctx);
    String name = toString(ctx.name);
    _configuration.defineStructure(RD_SET, name, ctx);
    _configuration
        .getRdSets()
        .computeIfAbsent(
            name,
            n ->
                new RdSet(
                    ctx.rd_set_elem_list().elems.stream()
                        .map(this::toRdSetElem)
                        .filter(Objects::nonNull)
                        .collect(ImmutableList.toImmutableList())));
  }

  @Nullable
  private RdSetElem toRdSetElem(Rd_set_elemContext ctx) {
    if (ctx.rd_set_elem_asdot() != null) {
      @Nullable
      Entry<Uint16RangeExpr, Uint16RangeExpr> as = toUint32HighLowExpr(ctx.rd_set_elem_asdot());
      if (as == null) {
        return null;
      }
      return new RdSetAsDot(as.getKey(), as.getValue(), toUint16RangeExpr(ctx.rd_set_elem_lo16()));
    } else if (ctx.asplain_hi16 != null) {
      assert ctx.rd_set_elem_32() != null;
      return new RdSetAsPlain16(
          new LiteralUint16(toInteger(ctx.asplain_hi16)), toUint32RangeExpr(ctx.rd_set_elem_32()));
    } else if (ctx.asplain_hi32 != null) {
      assert ctx.rd_set_elem_lo16() != null;
      return new RdSetAsPlain32(
          toUint32RangeExpr(ctx.asplain_hi32), toUint16RangeExpr(ctx.rd_set_elem_lo16()));
    } else if (ctx.IP_PREFIX() != null) {
      assert ctx.rd_set_elem_lo16() != null;
      return new RdSetIpPrefix(
          Prefix.parse(ctx.IP_PREFIX().getText()), toUint16RangeExpr(ctx.rd_set_elem_lo16()));
    } else if (ctx.IP_ADDRESS() != null) {
      assert ctx.rd_set_elem_lo16() != null;
      return new RdSetIpAddress(toIp(ctx.IP_ADDRESS()), toUint16RangeExpr(ctx.rd_set_elem_lo16()));
    } else if (ctx.DFA_REGEX() != null) {
      return new RdSetDfaRegex(unquote(ctx.COMMUNITY_SET_REGEX().getText()));
    } else {
      assert ctx.IOS_REGEX() != null;
      return new RdSetIosRegex(unquote(ctx.COMMUNITY_SET_REGEX().getText()));
    }
  }

  @Nonnull
  private Uint32RangeExpr toUint32RangeExpr(Rd_set_elem_32Context ctx) {
    if (ctx.ASTERISK() != null) {
      return WildcardUint32RangeExpr.instance();
    } else {
      assert ctx.uint32() != null;
      return new LiteralUint32(toLong(ctx.uint32()));
    }
  }

  @Nonnull
  private Uint16RangeExpr toUint16RangeExpr(Rd_set_elem_lo16Context ctx) {
    if (ctx.ASTERISK() != null) {
      return WildcardUint16RangeExpr.instance();
    } else {
      assert ctx.uint16() != null;
      return new LiteralUint16(toInteger(ctx.uint16()));
    }
  }

  @Nullable
  private Entry<Uint16RangeExpr, Uint16RangeExpr> toUint32HighLowExpr(
      Rd_set_elem_asdotContext ctx) {
    Optional<Integer> maybeHi;
    Uint16RangeExpr hi;
    Uint16RangeExpr lo;
    if (ctx.hi_wild != null) {
      assert ctx.lo_num != null;
      hi = WildcardUint16RangeExpr.instance();
      lo = new LiteralUint16(toInteger(ctx.lo_num));
    } else if (ctx.lo_wild != null) {
      assert ctx.hi_num != null;
      maybeHi = toInteger(ctx, ctx.hi_num);
      if (!maybeHi.isPresent()) {
        return null;
      }
      hi = new LiteralUint16(maybeHi.get());
      lo = WildcardUint16RangeExpr.instance();
    } else {
      assert ctx.hi_num != null && ctx.lo_num != null;
      maybeHi = toInteger(ctx, ctx.hi_num);
      if (!maybeHi.isPresent()) {
        return null;
      }
      hi = new LiteralUint16(maybeHi.get());
      lo = new LiteralUint16(toInteger(ctx.lo_num));
    }
    return Maps.immutableEntry(hi, lo);
  }

  @Nonnull
  Optional<Integer> toInteger(ParserRuleContext messageCtx, Pint16Context ctx) {
    return toIntegerInSpace(messageCtx, ctx, PINT16_RANGE, "positive 16-bit integer");
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
  public void exitIf_description(If_descriptionContext ctx) {
    String description = getDescription(ctx.description_line());
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setDescription(description);
    }
  }

  @Override
  public void exitIf_encapsulation(If_encapsulationContext ctx) {
    toInteger(ctx, ctx.vlan)
        .ifPresent(vlan -> _currentInterfaces.forEach(i -> i.setEncapsulationVlan(vlan)));
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
        TRACK, trackingGroup, INTERFACE_STANDBY_TRACK, ctx.group.start.getLine());
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
  public void enterMldp_address_family(Mldp_address_familyContext ctx) {
    _inMplsLdpAddressFamilyIpv4 = ctx.IPV4() != null;
  }

  @Override
  public void exitMldp_address_family(Mldp_address_familyContext ctx) {
    _inMplsLdpAddressFamilyIpv4 = null;
  }

  @Override
  public void exitMldpafd_targeted_hello(Mldpafd_targeted_helloContext ctx) {
    assert _inMplsLdpAddressFamilyIpv4 != null;
    CiscoXrStructureType type;
    CiscoXrStructureUsage usage;
    if (_inMplsLdpAddressFamilyIpv4) {
      type = IPV4_ACCESS_LIST;
      usage = MPLS_LDP_AF_IPV4_DISCOVERY_TARGETED_HELLO_ACCEPT_FROM;
    } else {
      type = IPV6_ACCESS_LIST;
      usage = MPLS_LDP_AF_IPV6_DISCOVERY_TARGETED_HELLO_ACCEPT_FROM;
    }
    _configuration.referenceStructure(type, toString(ctx.name), usage, ctx.start.getLine());
  }

  @Override
  public void exitMldpafrb_advertise_to(Mldpafrb_advertise_toContext ctx) {
    assert _inMplsLdpAddressFamilyIpv4 != null;
    CiscoXrStructureType type;
    CiscoXrStructureUsage usage;
    if (_inMplsLdpAddressFamilyIpv4) {
      type = IPV4_ACCESS_LIST;
      usage = MPLS_LDP_AF_IPV4_REDISTRIBUTE_BGP_ADVERTISE_TO;
    } else {
      type = IPV6_ACCESS_LIST;
      usage = MPLS_LDP_AF_IPV6_REDISTRIBUTE_BGP_ADVERTISE_TO;
    }
    _configuration.referenceStructure(type, toString(ctx.name), usage, ctx.start.getLine());
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
    String name = toString(ctx.name);
    _configuration.getIpv4Acls().remove(name);
  }

  @Override
  public void exitNo_ipv6_access_list(No_ipv6_access_listContext ctx) {
    String name = toString(ctx.name);
    _configuration.getIpv6Acls().remove(name);
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
    _currentOspfArea = toLong(ctx.area);
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
  public void exitRoi_network(Roi_networkContext ctx) {
    Interface iface = _configuration.getInterfaces().get(_currentOspfInterface);

    // Guaranteed by roa_interface, should be created if it doesn't exist
    assert iface != null;
    iface.setOspfNetworkType(toOspfNetworkType(ctx.ospf_network_type()));
  }

  @Override
  public void enterRoute_policy_stanza(Route_policy_stanzaContext ctx) {
    String name = toString(ctx.name);
    // always replace entire policy
    RoutePolicy currentRoutePolicy = new RoutePolicy(name);
    _configuration.getRoutePolicies().put(name, currentRoutePolicy);
    _configuration.defineStructure(ROUTE_POLICY, name, ctx);
    assert _elseIfs.empty();
    assert _ifs.empty();
    assert _statementCollectors.empty();
    _statementCollectors.push(currentRoutePolicy::addStatement);
  }

  @Override
  public void exitRoute_policy_stanza(Route_policy_stanzaContext ctx) {
    _statementCollectors.pop();
    assert _elseIfs.empty();
    assert _ifs.empty();
    assert _statementCollectors.empty();
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
    _currentVrf = toString(ctx.name);
  }

  @Override
  public void enterS_aaa(S_aaaContext ctx) {
    _no = ctx.NO() != null;
    if (_configuration.getCf().getAaa() == null) {
      _configuration.getCf().setAaa(new Aaa());
    }
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
          Interface iface = addInterface(name, ctx.iname, true);
          if (ctx.L2TRANSPORT() != null) {
            iface.setL2transport(true);
          }
          _configuration.defineStructure(INTERFACE, name, ctx);
          _configuration.referenceStructure(
              INTERFACE, name, INTERFACE_SELF_REF, ctx.getStart().getLine());
        }
      }
    } else {
      Interface iface = addInterface(namePrefix.toString(), ctx.iname, true);
      if (ctx.L2TRANSPORT() != null) {
        iface.setL2transport(true);
      }
    }
    if (ctx.MULTIPOINT() != null) {
      todo(ctx);
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
  public void exitRo_auto_cost(Ro_auto_costContext ctx) {
    long referenceBandwidthDec = Long.parseLong(ctx.uint_legacy().getText());
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
  public void enterS_vrf(S_vrfContext ctx) {
    _currentVrf = toString(ctx.name);
    initVrf(_currentVrf);
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
    _currentVrf = toString(ctx.name);
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
  public void exitAdditional_paths_selection_xr_rb_stanza(
      Additional_paths_selection_xr_rb_stanzaContext ctx) {
    if (ctx.name != null) {
      String name = toString(ctx.name);
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
  public void exitBgp_vrf_address_family(Bgp_vrf_address_familyContext ctx) {
    popPeer();
  }

  @Override
  public void exitBgp_vrf_rd(Bgp_vrf_rdContext ctx) {
    todo(ctx);
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
          String policyName = toString(ctx.rp);
          net.setRoutePolicy(policyName);
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
  public void enterS_as_path_set(S_as_path_setContext ctx) {
    String name = toString(ctx.name);
    if (_configuration.getAsPathSets().containsKey(name)) {
      warn(ctx, "Redeclaration of as-path-set: '" + name + "'.");
    }
    _currentAsPathSet = new AsPathSet();
    _configuration.defineStructure(AS_PATH_SET, name, ctx);
    _configuration.getAsPathSets().put(name, _currentAsPathSet);
  }

  @Override
  public void exitS_as_path_set(S_as_path_setContext ctx) {
    _currentAsPathSet = null;
  }

  private static @Nonnull String toString(As_path_set_nameContext ctx) {
    return ctx.getText();
  }

  private static @Nonnull String toString(Bridge_domain_nameContext ctx) {
    return ctx.getText();
  }

  private static @Nonnull String toString(Bridge_group_nameContext ctx) {
    return ctx.getText();
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
    if (ctx.uint_legacy() != null) {
      long ipAsLong = toLong(ctx.uint_legacy());
      clusterId = Ip.create(ipAsLong);
    } else if (ctx.IP_ADDRESS() != null) {
      clusterId = toIp(ctx.IP_ADDRESS());
    }
    _currentPeerGroup.setClusterId(clusterId);
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
      line = ctx.num.start.getLine();
    }
    _configuration.referenceStructure(IP_ACCESS_LIST, name, CLASS_MAP_ACCESS_GROUP, line);
  }

  @Override
  public void exitCmm_access_list(Cmm_access_listContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IP_ACCESS_LIST, name, CLASS_MAP_ACCESS_LIST, line);
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
    if (ctx.policy != null) {
      todo(ctx);
      String name = toString(ctx.policy);
      _configuration.referenceStructure(
          ROUTE_POLICY, name, BGP_DEFAULT_ORIGINATE_ROUTE_POLICY, ctx.start.getLine());
    }
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
  public void enterExtended_access_list_tail(Extended_access_list_tailContext ctx) {
    _currentIpv4AclLine = Ipv4AccessListLine.builder();
  }

  @Override
  public void exitExtended_access_list_tail(Extended_access_list_tailContext ctx) {
    LineAction action = toLineAction(ctx.ala);
    AccessListAddressSpecifier srcAddressSpecifier = toAccessListAddressSpecifier(ctx.srcipr);
    AccessListAddressSpecifier dstAddressSpecifier = toAccessListAddressSpecifier(ctx.dstipr);
    AccessListServiceSpecifier serviceSpecifier = computeExtendedAccessListServiceSpecifier(ctx);
    String name = getFullText(ctx).trim();
    Ipv4AccessListLine line =
        _currentIpv4AclLine
            .setAction(action)
            .setDstAddressSpecifier(dstAddressSpecifier)
            .setName(name)
            .setServiceSpecifier(serviceSpecifier)
            .setSrcAddressSpecifier(srcAddressSpecifier)
            .build();
    if (line.getAction() != LineAction.PERMIT && line.getNexthop1() != null) {
      warn(ctx, "ACL based forwarding can only be configured on an ACL line with a permit action");
    } else {
      _currentIpv4Acl.addLine(line);
    }
    _currentIpv4AclLine = null;
  }

  public Ipv4Nexthop toIpv4Nexthop(Ipv4_nexthopContext ctx) {
    String vrfName = ctx.vrf_name() == null ? null : ctx.vrf_name().getText();
    return new Ipv4Nexthop(toIp(ctx.nexthop), vrfName);
  }

  @Override
  public void exitIpv4_nexthop1(Ipv4_nexthop1Context ctx) {
    _currentIpv4AclLine.setNexthop1(toIpv4Nexthop(ctx.ipv4_nexthop()));
  }

  @Override
  public void exitIpv4_nexthop2(Ipv4_nexthop2Context ctx) {
    _currentIpv4AclLine.setNexthop2(toIpv4Nexthop(ctx.ipv4_nexthop()));
  }

  @Override
  public void exitIpv4_nexthop3(Ipv4_nexthop3Context ctx) {
    _currentIpv4AclLine.setNexthop3(toIpv4Nexthop(ctx.ipv4_nexthop()));
  }

  private AccessListServiceSpecifier computeExtendedAccessListServiceSpecifier(
      Extended_access_list_tailContext ctx) {
    // TODO: rewrite, cleanly separate v4/v6/protocol-specific features
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
      boolean fragments = false;
      for (Extended_access_list_additional_featureContext feature : ctx.features) {
        if (feature.ACK() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setAck(true).build())
                  .setUseAck(true)
                  .build());
        } else if (feature.ADDRESS_UNREACHABLE() != null) {
          icmpType = Icmp6Type.DESTINATION_UNREACHABLE;
          icmpCode = Icmp6Code.ADDRESS_UNREACHABLE;
        } else if (feature.ADMINISTRATIVELY_PROHIBITED() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.COMMUNICATION_ADMINISTRATIVELY_PROHIBITED;
        } else if (feature.ALTERNATE_ADDRESS() != null) {
          icmpType = IcmpType.ALTERNATE_ADDRESS;
        } else if (feature.CAPTURE() != null) {
          // Captures matching traffic.
        } else if (feature.CONVERSION_ERROR() != null) {
          icmpType = IcmpType.CONVERSION_ERROR;
        } else if (feature.DOD_HOST_PROHIBITED() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_HOST_PROHIBITED;
        } else if (feature.DOD_NET_PROHIBITED() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.DESTINATION_NETWORK_PROHIBITED;
        } else if (feature.DSCP() != null) {
          toDscpType(ctx, feature.dscp_type()).ifPresent(dscps::add);
        } else if (feature.ECHO() != null) {
          icmpType = IcmpType.ECHO_REQUEST;
        } else if (feature.ECHO_REPLY() != null) {
          icmpType = IcmpType.ECHO_REPLY;
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
        } else if (feature.FRAGMENTS() != null) {
          fragments = true;
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
          // (Optional) Causes an informational logging message about the packet that matches the
          // entry to be sent to the console. (The level of messages logged to the console is
          // controlled by the logging console command.)
        } else if (feature.LOG_INPUT() != null) {
          // (Optional) Provides the same function as the log keyword, except that the logging
          // message also includes the input interface.
        } else if (feature.MASK_REPLY() != null) {
          icmpType = IcmpType.MASK_REPLY;
        } else if (feature.MASK_REQUEST() != null) {
          icmpType = IcmpType.MASK_REQUEST;
        } else if (feature.MOBILE_REDIRECT() != null) {
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
        } else if (feature.PRECEDENCE_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.PRECEDENCE_CUTOFF_IN_EFFECT;
        } else if (feature.PROTOCOL_UNREACHABLE() != null) {
          icmpType = IcmpType.DESTINATION_UNREACHABLE;
          icmpCode = IcmpCode.PROTOCOL_UNREACHABLE;
        } else if (feature.PSH() != null) {
          tcpFlags.add(
              TcpFlagsMatchConditions.builder()
                  .setTcpFlags(TcpFlags.builder().setPsh(true).build())
                  .setUsePsh(true)
                  .build());
        } else if (feature.REASSEMBLY_TIMEOUT() != null) {
          icmpType = IcmpType.TIME_EXCEEDED;
          icmpCode = IcmpCode.TIME_EXCEEDED_DURING_FRAGMENT_REASSEMBLY;
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
          .setFragments(fragments)
          .setIcmpCode(icmpCode)
          .setIcmpType(icmpType)
          .setProtocol(protocol)
          .setSrcPortRanges(srcPortRanges)
          .setTcpFlags(tcpFlags)
          .build();
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
    } else if (ctx.iface != null) {
      todo(ctx);
      return new WildcardAddressSpecifier(IpWildcard.ANY);
    } else {
      throw convError(AccessListAddressSpecifier.class, ctx);
    }
  }

  @Override
  public void enterExtended_ipv6_access_list_tail(Extended_ipv6_access_list_tailContext ctx) {
    _currentIpv6AclLine = Ipv6AccessListLine.builder();
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
    for (Extended_access_list_additional_featureContext feature : ctx.features) {
      if (feature.ACK() != null) {
        tcpFlags.add(
            TcpFlagsMatchConditions.builder()
                .setTcpFlags(TcpFlags.builder().setAck(true).build())
                .setUseAck(true)
                .build());
      } else if (feature.DSCP() != null) {
        toDscpType(ctx, feature.dscp_type()).ifPresent(dscps::add);
      } else if (feature.ECHO_REPLY() != null) {
        icmpType = IcmpType.ECHO_REPLY;
        icmpCode = 0; /* Forced to 0 by RFC-792. */
      } else if (feature.ECHO() != null) {
        icmpType = IcmpType.ECHO_REQUEST;
        icmpCode = 0; /* Forced to 0 by RFC-792. */
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
      } else {
        // warn(ctx, "Unsupported clause in IPv6 extended access list: " + feature.getText());
      }
    }
    String name = getFullText(ctx).trim();
    Ipv6AccessListLine line =
        _currentIpv6AclLine
            .setName(name)
            .setAction(action)
            .setProtocol(protocol)
            .setSrcIpWildcard(new Ip6Wildcard(srcIp, srcWildcard))
            .setSrcAddressGroup(srcAddressGroup)
            .setSrcPortRanges(srcPortRanges)
            .setDstIpWildcard(new Ip6Wildcard(dstIp, dstWildcard))
            .setDstAddressGroup(dstAddressGroup)
            .setDstPortRanges(dstPortRanges)
            .setDscps(dscps)
            .setIcmpCode(icmpCode)
            .setIcmpType(icmpType)
            .setTcpFlags(tcpFlags)
            .build();
    if (line.getAction() != LineAction.PERMIT && line.getNexthop1() != null) {
      warn(ctx, "ACL based forwarding can only be configured on an ACL line with a permit action");
    } else {
      _currentIpv6Acl.addLine(line);
    }
    _currentIpv6AclLine = null;
  }

  public Ipv6Nexthop toIpv6Nexthop(Ipv6_nexthopContext ctx) {
    String vrfName = ctx.vrf_name() == null ? null : ctx.vrf_name().getText();
    return new Ipv6Nexthop(toIp6(ctx.nexthop), vrfName);
  }

  @Override
  public void exitIpv6_nexthop1(Ipv6_nexthop1Context ctx) {
    _currentIpv6AclLine.setNexthop1(toIpv6Nexthop(ctx.ipv6_nexthop()));
  }

  @Override
  public void exitIpv6_nexthop2(Ipv6_nexthop2Context ctx) {
    _currentIpv6AclLine.setNexthop2(toIpv6Nexthop(ctx.ipv6_nexthop()));
  }

  @Override
  public void exitIpv6_nexthop3(Ipv6_nexthop3Context ctx) {
    _currentIpv6AclLine.setNexthop3(toIpv6Nexthop(ctx.ipv6_nexthop()));
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
      newBandwidthBps = toLong(ctx.uint_legacy()) * 1000.0D;
    }
    _currentInterfaces.forEach(i -> i.setBandwidth(newBandwidthBps));
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
      newDelayPs = toLong(ctx.uint_legacy()) * 10_000_000;
    }
    _currentInterfaces.forEach(i -> i.setDelay(newDelayPs));
  }

  @Override
  public void exitIf_ipv4_access_group(If_ipv4_access_groupContext ctx) {
    int line = ctx.start.getLine();
    if (ctx.common_acl != null) {
      String name = toString(ctx.common_acl);
      _configuration.referenceStructure(
          IPV4_ACCESS_LIST, name, INTERFACE_IPV4_ACCESS_GROUP_COMMON, line);
    }
    if (ctx.interface_acl != null) {
      String name = toString(ctx.interface_acl);
      CiscoXrStructureUsage usage = null;
      if (ctx.INGRESS() != null) {
        for (Interface currentInterface : _currentInterfaces) {
          currentInterface.setIncomingFilter(name);
          usage = INTERFACE_IPV4_ACCESS_GROUP_INGRESS;
        }
      } else {
        assert ctx.EGRESS() != null;
        for (Interface currentInterface : _currentInterfaces) {
          currentInterface.setOutgoingFilter(name);
          usage = INTERFACE_IPV4_ACCESS_GROUP_EGRESS;
        }
      }
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, usage, line);
    }
  }

  @Override
  public void exitIf_ipv6_access_group(If_ipv6_access_groupContext ctx) {
    int line = ctx.start.getLine();
    if (ctx.common_acl != null) {
      String name = toString(ctx.common_acl);
      _configuration.referenceStructure(
          IPV6_ACCESS_LIST, name, INTERFACE_IPV6_ACCESS_GROUP_COMMON, line);
    }
    if (ctx.interface_acl != null) {
      String name = toString(ctx.interface_acl);
      CiscoXrStructureUsage usage = null;
      if (ctx.INGRESS() != null) {
        for (Interface currentInterface : _currentInterfaces) {
          currentInterface.setIncomingFilter(name);
          usage = INTERFACE_IPV6_ACCESS_GROUP_INGRESS;
        }
      } else {
        assert ctx.EGRESS() != null;
        for (Interface currentInterface : _currentInterfaces) {
          currentInterface.setOutgoingFilter(name);
          usage = INTERFACE_IPV6_ACCESS_GROUP_EGRESS;
        }
      }
      _configuration.referenceStructure(IPV6_ACCESS_LIST, name, usage, line);
    }
  }

  @Override
  public void exitIf_ipv4_address(If_ipv4_addressContext ctx) {
    Optional<ConcreteInterfaceAddress> maybeAddress =
        toConcreteInterfaceAddress(ctx, ctx.interface_ipv4_address());
    if (!maybeAddress.isPresent()) {
      return;
    }
    ConcreteInterfaceAddress address = maybeAddress.get();
    for (Interface currentInterface : _currentInterfaces) {
      if (ctx.SECONDARY() != null) {
        currentInterface.getSecondaryAddresses().add(address);
      } else {
        currentInterface.setAddress(address);
      }
    }
    if (ctx.tag != null) {
      warn(ctx, "Unsupported: tag declared in interface ipv4 address");
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
      int line = ctx.acl.start.getLine();
      _configuration.referenceStructure(
          IPV4_ACCESS_LIST, acl, INTERFACE_IP_VERIFY_ACCESS_LIST, line);
    }
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
    int mtu = toInteger(ctx.uint_legacy());
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setMtu(mtu);
    }
  }

  @Override
  public void exitIf_rewrite_ingress_tag(If_rewrite_ingress_tagContext ctx) {
    Optional<TagRewritePolicy> policy = toTagRewritePolicy(ctx.ifrit_policy());
    for (Interface currentInterface : _currentInterfaces) {
      if (!currentInterface.getL2transport()) {
        warn(
            ctx,
            "Rewrite policy can only be configured on l2transport interfaces. Ignoring this line.");
        continue;
      }
      policy.ifPresent(currentInterface::setRewriteIngressTag);
    }
  }

  private Optional<TagRewritePolicy> toTagRewritePolicy(Ifrit_policyContext ctx) {
    // This is the only kind of policy Batfish supports so far
    assert ctx.ifrit_pop() != null;

    Ifrit_popContext pop = ctx.ifrit_pop();
    return toInteger(ctx, pop.ifrit_pop_count())
        .map(i -> new TagRewritePop(i, pop.SYMMETRIC() != null));
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
    toInteger(ctx, ctx.vlan)
        .ifPresent(vlan -> _currentInterfaces.forEach(iface -> iface.setEncapsulationVlan(vlan)));
  }

  @Override
  public void exitIf_vrf(If_vrfContext ctx) {
    String name = toString(ctx.name);
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
  public void exitLogging_buffered_set_severity(Logging_buffered_set_severityContext ctx) {
    getOrCreateBuffered().setSeverity(toSeverity(ctx.logging_buffered_severity()));
    getOrCreateBuffered().setSeverityNum(toLoggingSeverityNum(ctx.logging_buffered_severity()));
  }

  @Override
  public void enterL2vpn_bridge_group(L2vpn_bridge_groupContext ctx) {
    String name = toString(ctx.bridge_group_name());
    _currentBridgeGroup =
        _configuration.getBridgeGroups().computeIfAbsent(name, n -> new BridgeGroup(n));
  }

  @Override
  public void exitL2vpn_bridge_group(L2vpn_bridge_groupContext ctx) {
    _currentBridgeGroup = null;
  }

  @Override
  public void enterLbg_bridge_domain(Lbg_bridge_domainContext ctx) {
    String name = toString(ctx.bridge_domain_name());
    _currentBridgeDomain =
        _currentBridgeGroup.getBridgeDomains().computeIfAbsent(name, n -> new BridgeDomain(n));
  }

  @Override
  public void exitLbg_bridge_domain(Lbg_bridge_domainContext ctx) {
    _currentBridgeDomain = null;
  }

  @Override
  public void exitLbgbd_interface(Lbgbd_interfaceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.interface_name().getText());
    _currentBridgeDomain.getInterfaces().add(ifaceName);
    _configuration.referenceStructure(
        INTERFACE, ifaceName, BRIDGE_DOMAIN_INTERFACE, ctx.start.getLine());
  }

  @Override
  public void exitLbgbd_routed_interface(Lbgbd_routed_interfaceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.interface_name().getText());
    _currentBridgeDomain.setRoutedInterface(ifaceName);
    _configuration.referenceStructure(
        INTERFACE, ifaceName, BRIDGE_DOMAIN_ROUTED_INTERFACE, ctx.start.getLine());
  }

  @Nonnull
  private static String toSeverity(Logging_buffered_severityContext ctx) {
    return ctx.getText().toLowerCase();
  }

  @Nonnull
  private static String toSeverity(Logging_console_severityContext ctx) {
    return ctx.getText().toLowerCase();
  }

  @Nonnull
  private static String toSeverity(Logging_trap_severityContext ctx) {
    return ctx.getText().toLowerCase();
  }

  @Override
  public void exitLogging_buffered_buffer_size(Logging_buffered_buffer_sizeContext ctx) {
    toInteger(ctx, ctx.logging_buffer_size()).ifPresent(getOrCreateBuffered()::setSize);
  }

  @Nonnull
  Optional<Integer> toInteger(ParserRuleContext messageCtx, Logging_buffer_sizeContext ctx) {
    return toIntegerInSpace(
        messageCtx, ctx.uint32(), LOGGING_BUFFER_SIZE_RANGE, "logging buffer size");
  }

  @Nonnull
  private Buffered getOrCreateBuffered() {
    Logging logging = _configuration.getCf().getLogging();
    Buffered buffered = logging.getBuffered();
    if (buffered == null) {
      buffered = new Buffered();
      logging.setBuffered(buffered);
    }
    return buffered;
  }

  @Override
  public void exitLogging_console_set_severity(Logging_console_set_severityContext ctx) {
    getOrCreateConsole().setSeverity(toSeverity(ctx.logging_console_severity()));
    getOrCreateConsole().setSeverityNum(toLoggingSeverityNum(ctx.logging_console_severity()));
  }

  private @Nonnull LoggingType getOrCreateConsole() {
    Logging logging = _configuration.getCf().getLogging();
    LoggingType console = logging.getConsole();
    if (console == null) {
      console = new LoggingType();
      logging.setConsole(console);
    }
    return console;
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
  public void exitLogging_trap_set_severity(Logging_trap_set_severityContext ctx) {
    getOrCreateTrap().setSeverity(toSeverity(ctx.logging_trap_severity()));
    getOrCreateTrap().setSeverityNum(toLoggingSeverityNum(ctx.logging_trap_severity()));
  }

  @Nonnull
  private LoggingType getOrCreateTrap() {
    Logging logging = _configuration.getCf().getLogging();
    LoggingType trap = logging.getTrap();
    if (trap == null) {
      trap = new LoggingType();
      logging.setTrap(trap);
    }
    return trap;
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
    if (ctx.policyname != null) {
      todo(ctx);
      String name = toString(ctx.policyname);
      _configuration.referenceStructure(
          ROUTE_POLICY, name, BGP_NETWORK_ROUTE_POLICY, ctx.start.getLine());
    }
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
    if (ctx.policyname != null) {
      todo(ctx);
      String name = toString(ctx.policyname);
      _configuration.referenceStructure(
          ROUTE_POLICY, name, BGP_NETWORK_ROUTE_POLICY, ctx.start.getLine());
    }
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
    String name = toString(ctx.name);
    int line = ctx.name.getStart().getLine();
    CiscoXrStructureUsage usage;
    if (ctx.PEER() != null) {
      usage = NTP_ACCESS_GROUP_PEER;
    } else if (ctx.QUERY_ONLY() != null) {
      usage = NTP_ACCESS_GROUP_QUERY_ONLY;
    } else if (ctx.SERVE() != null) {
      usage = NTP_ACCESS_GROUP_SERVE;
    } else {
      assert ctx.SERVE_ONLY() != null;
      usage = NTP_ACCESS_GROUP_SERVE_ONLY;
    }
    if (ctx.IPV6() != null) {
      _configuration.referenceStructure(IPV6_ACCESS_LIST, name, usage, line);
    } else {
      // IPv4 unless IPv6 explicit.
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, usage, line);
    }
  }

  @Override
  public void exitNtp_server(Ntp_serverContext ctx) {
    Ntp ntp = _configuration.getCf().getNtp();
    String hostname = ctx.hostname.getText();
    NtpServer server = ntp.getServers().computeIfAbsent(hostname, NtpServer::new);
    if (ctx.vrf != null) {
      String vrfName = toString(ctx.vrf);
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
  public void exitRmsdp_cache_sa_state(Rmsdp_cache_sa_stateContext ctx) {
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST,
        name,
        ctx.LIST() != null ? ROUTER_MSDP_CACHE_SA_STATE_LIST : ROUTER_MSDP_CACHE_SA_STATE_RP_LIST,
        line);
  }

  @Override
  public void exitRmsdp_sa_filter(Rmsdp_sa_filterContext ctx) {
    String name = toString(ctx.name);
    int line = ctx.name.getStart().getLine();
    CiscoXrStructureUsage usage;
    if (ctx.IN() != null) {
      if (ctx.LIST() != null) {
        usage = ROUTER_MSDP_SA_FILTER_IN_LIST;
      } else {
        assert ctx.RP_LIST() != null;
        usage = ROUTER_MSDP_SA_FILTER_IN_RP_LIST;
      }
    } else {
      assert ctx.OUT() != null;
      if (ctx.LIST() != null) {
        usage = ROUTER_MSDP_SA_FILTER_OUT_LIST;
      } else {
        assert ctx.RP_LIST() != null;
        usage = ROUTER_MSDP_SA_FILTER_OUT_RP_LIST;
      }
    }
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, usage, line);
  }

  @Override
  public void exitRmsdpp_sa_filter(Rmsdpp_sa_filterContext ctx) {
    String name = toString(ctx.name);
    int line = ctx.name.getStart().getLine();
    CiscoXrStructureUsage usage;
    if (ctx.IN() != null) {
      if (ctx.LIST() != null) {
        usage = ROUTER_MSDP_SA_FILTER_IN_LIST;
      } else {
        assert ctx.RP_LIST() != null;
        usage = ROUTER_MSDP_SA_FILTER_IN_RP_LIST;
      }
    } else {
      assert ctx.OUT() != null;
      if (ctx.LIST() != null) {
        usage = ROUTER_MSDP_SA_FILTER_OUT_LIST;
      } else {
        assert ctx.RP_LIST() != null;
        usage = ROUTER_MSDP_SA_FILTER_OUT_RP_LIST;
      }
    }
    _configuration.referenceStructure(IPV4_ACCESS_LIST, name, usage, line);
  }

  @Override
  public void exitRpim_accept_register(Rpim_accept_registerContext ctx) {
    String name = toString(ctx.name);
    int line = ctx.name.getStart().getLine();
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    _configuration.referenceStructure(type, name, ROUTER_PIM_ACCEPT_REGISTER, line);
  }

  @Override
  public void enterRpimaf_ipv6(Rpimaf_ipv6Context ctx) {
    _pimIpv6 = true;
  }

  @Override
  public void exitRpimaf_ipv6(Rpimaf_ipv6Context ctx) {
    _pimIpv6 = false;
  }

  @Override
  public void exitRpim_allow_rp_group_list(Rpim_allow_rp_group_listContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(type, name, ROUTER_PIM_ALLOW_RP_GROUP_LIST, line);
  }

  @Override
  public void exitRpim_allow_rp_rp_list(Rpim_allow_rp_rp_listContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(type, name, ROUTER_PIM_ALLOW_RP_RP_LIST, line);
  }

  @Override
  public void exitRpim_rp_address(Rpim_rp_addressContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    if (ctx.name != null) {
      String name = toString(ctx.name);
      int line = ctx.name.getStart().getLine();
      _configuration.referenceStructure(
          type, name,
          ROUTER_PIM_RP_ADDRESS, line);
    }
  }

  @Override
  public void exitRpim_bsr_candidate_rp(Rpim_bsr_candidate_rpContext ctx) {
    if (ctx.name != null) {
      CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
      String name = toString(ctx.name);
      int line = ctx.name.getStart().getLine();
      _configuration.referenceStructure(type, name, ROUTER_PIM_BSR_CANDIDATE_RP, line);
    }
  }

  @Override
  public void exitRpim_spt_threshold(Rpim_spt_thresholdContext ctx) {
    if (ctx.name != null) {
      CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
      String name = toString(ctx.name);
      int line = ctx.name.getStart().getLine();
      _configuration.referenceStructure(type, name, ROUTER_PIM_SPT_THRESHOLD, line);
    }
  }

  @Override
  public void exitRpim_mdt_neighbor_filter(Rpim_mdt_neighbor_filterContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(type, name, ROUTER_PIM_MDT_NEIGHBOR_FILTER, line);
  }

  @Override
  public void exitRpim_mofrr_flow(Rpim_mofrr_flowContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(type, name, ROUTER_PIM_MOFRR_FLOW, line);
  }

  @Override
  public void exitRpim_mofrr_rib(Rpim_mofrr_ribContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(type, name, ROUTER_PIM_MOFRR_RIB, line);
  }

  @Override
  public void exitRpim_neighbor_filter(Rpim_neighbor_filterContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(type, name, ROUTER_PIM_NEIGHBOR_FILTER, line);
  }

  @Override
  public void exitRpim_rp_static_deny(Rpim_rp_static_denyContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(type, name, ROUTER_PIM_RP_STATIC_DENY, line);
  }

  @Override
  public void exitRpim_rpf(Rpim_rpfContext ctx) {
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(ROUTE_POLICY, name, ROUTER_PIM_RPF_TOPOLOGY, line);
  }

  @Override
  public void exitRpim_sg_expiry_timer(Rpim_sg_expiry_timerContext ctx) {
    if (ctx.name != null) {
      CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
      String name = toString(ctx.name);
      int line = ctx.start.getLine();
      _configuration.referenceStructure(type, name, ROUTER_PIM_SG_EXPIRY_TIMER, line);
    }
  }

  @Override
  public void exitRpim_ssm_threshold_range(Rpim_ssm_threshold_rangeContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(type, name, ROUTER_PIM_SSM_THRESHOLD_RANGE, line);
  }

  @Override
  public void exitRpimaf4_auto_rp_candidate_rp(Rpimaf4_auto_rp_candidate_rpContext ctx) {
    if (ctx.aclname != null) {
      String name = toString(ctx.aclname);
      int line = ctx.start.getLine();
      _configuration.referenceStructure(
          IPV4_ACCESS_LIST, name, ROUTER_PIM_AUTO_RP_CANDIDATE_RP_GROUP_LIST, line);
    }
  }

  @Override
  public void exitRpimaf4_rpf_redirect(Rpimaf4_rpf_redirectContext ctx) {
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(
        ROUTE_POLICY, name, ROUTER_PIM_RPF_REDIRECT_ROUTE_POLICY, line);
  }

  @Override
  public void exitRpimaf6_embedded_rp_rendezvous_point(
      Rpimaf6_embedded_rp_rendezvous_pointContext ctx) {
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(
        IPV6_ACCESS_LIST, name, ROUTER_PIM_EMBEDDED_RP_RENDEZVOUS_POINT, line);
  }

  @Override
  public void exitRpimafi_neighbor_filter(Rpimafi_neighbor_filterContext ctx) {
    CiscoXrStructureType type = _pimIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
    String name = toString(ctx.name);
    int line = ctx.start.getLine();
    _configuration.referenceStructure(type, name, ROUTER_PIM_INTERFACE_NEIGHBOR_FILTER, line);
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
    String policyName = null;
    if (ctx.policy != null) {
      policyName = toString(ctx.policy);
      _configuration.referenceStructure(
          ROUTE_POLICY, policyName, BGP_REDISTRIBUTE_CONNECTED_ROUTE_POLICY, ctx.start.getLine());
    }
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
      if (policyName != null) {
        r.setRouteMap(policyName);
      }
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
    } else {
      // Warn about other cases we don't handle yet, like multicast address-families
      todo(ctx);
    }
  }

  @Override
  public void exitRedistribute_connected_is_stanza(Redistribute_connected_is_stanzaContext ctx) {
    if (ctx.policy != null) {
      todo(ctx);
      String name = toString(ctx.policy);
      _configuration.referenceStructure(
          ROUTE_POLICY, name, ROUTER_ISIS_REDISTRIBUTE_CONNECTED_ROUTE_POLICY, ctx.start.getLine());
    }
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
    String policyName = null;
    if (ctx.policy != null) {
      policyName = toString(ctx.policy);
      _configuration.referenceStructure(
          ROUTE_POLICY, policyName, BGP_REDISTRIBUTE_STATIC_ROUTE_POLICY, ctx.start.getLine());
    }
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
      if (policyName != null) {
        r.setRouteMap(policyName);
      }
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
    } else {
      // Warn about other cases we don't handle yet, like multicast address-families
      todo(ctx);
    }
  }

  @Override
  public void exitRedistribute_static_is_stanza(Redistribute_static_is_stanzaContext ctx) {
    if (ctx.policy != null) {
      todo(ctx);
      String name = toString(ctx.policy);
      _configuration.referenceStructure(
          ROUTE_POLICY, name, ROUTER_ISIS_REDISTRIBUTE_STATIC_ROUTE_POLICY, ctx.start.getLine());
    }
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
    long area = toLong(ctx.area);
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
    long areaNum = toLong(ctx.area);
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
    area.put(
        prefix,
        new OspfAreaSummary(
            advertise
                ? SummaryRouteBehavior.ADVERTISE_AND_INSTALL_DISCARD
                : SummaryRouteBehavior.NOT_ADVERTISE_AND_NO_DISCARD,
            cost));
  }

  @Override
  public void exitRo_area_stub(Ro_area_stubContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    long area = toLong(ctx.area);
    StubSettings settings = proc.getStubs().computeIfAbsent(area, a -> new StubSettings());
    if (ctx.no_summary != null) {
      settings.setNoSummary(true);
    }
  }

  @Override
  public void exitRo_default_information(Ro_default_informationContext ctx) {
    if (ctx.policy != null) {
      todo(ctx);
      String name = toString(ctx.policy);
      _configuration.referenceStructure(
          ROUTE_POLICY, name, OSPF_DEFAULT_INFORMATION_ROUTE_POLICY, ctx.start.getLine());
    }
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
  public void exitRodl_acl(Rodl_aclContext ctx) {
    String name = toString(ctx.acl);
    DistributeList distributeList = new DistributeList(name, DistributeListFilterType.ACCESS_LIST);

    if (ctx.IN() != null) {
      _configuration.referenceStructure(
          IP_ACCESS_LIST, name, OSPF_DISTRIBUTE_LIST_ACCESS_LIST_IN, ctx.acl.getStart().getLine());
      _currentOspfProcess.setInboundGlobalDistributeList(distributeList);
    } else {
      assert ctx.OUT() != null;
      _configuration.referenceStructure(
          IP_ACCESS_LIST, name, OSPF_DISTRIBUTE_LIST_ACCESS_LIST_OUT, ctx.acl.getStart().getLine());
      _currentOspfProcess.setOutboundGlobalDistributeList(distributeList);
      todo(ctx);
    }
  }

  @Override
  public void exitRodl_route_policy(Rodl_route_policyContext ctx) {
    String name = toString(ctx.rp);
    _configuration.referenceStructure(
        ROUTE_POLICY, name, OSPF_DISTRIBUTE_LIST_ROUTE_POLICY_IN, ctx.rp.getStart().getLine());

    DistributeList distributeList = new DistributeList(name, DistributeListFilterType.ROUTE_POLICY);
    _currentOspfProcess.setInboundGlobalDistributeList(distributeList);
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
    _currentOspfProcess.setDefaultNetworkType(toOspfNetworkType(ctx.ospf_network_type()));
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
  public void exitRor_routing_instance(Ror_routing_instanceContext ctx) {
    OspfRedistributionPolicy r = initOspfRedistributionPolicy(ctx.rorri_protocol());
    OspfProcess proc = _currentOspfProcess;
    proc.getRedistributionPolicies().put(r.getSourceProtocol(), r);
    if (ctx.metric != null) {
      toInteger(ctx, ctx.metric).ifPresent(r::setMetric);
    }
    if (ctx.type != null) {
      toInteger(ctx, ctx.type).map(OspfMetricType::fromInteger).ifPresent(r::setOspfMetricType);
    } else {
      r.setOspfMetricType(OspfRedistributionPolicy.DEFAULT_METRIC_TYPE);
    }
    if (ctx.policy != null) {
      String name = toString(ctx.policy);
      r.setRouteMap(name);
      _configuration.referenceStructure(
          ROUTE_POLICY, name, OSPF_REDISTRIBUTE_ROUTE_POLICY, ctx.start.getLine());
    }
    if (ctx.tag != null) {
      r.setTag(toLong(ctx.tag));
    }
  }

  @Override
  public void exitRor_routing_instance_null(Ror_routing_instance_nullContext ctx) {
    todo(ctx);
  }

  private @Nonnull OspfRedistributionPolicy initOspfRedistributionPolicy(
      Rorri_protocolContext ctx) {
    if (ctx.BGP() != null) {
      OspfRedistributionPolicy r = new OspfRedistributionPolicy(RoutingProtocol.BGP);
      assert ctx.bgp_asn() != null;
      long as = toAsNum(ctx.bgp_asn());
      r.getSpecialAttributes().put(OspfRedistributionPolicy.BGP_AS, as);
      return r;
    } else if (ctx.CONNECTED() != null) {
      return new OspfRedistributionPolicy(RoutingProtocol.CONNECTED);
    } else if (ctx.EIGRP() != null) {
      OspfRedistributionPolicy r = new OspfRedistributionPolicy(RoutingProtocol.EIGRP);
      Optional<Integer> asn = toInteger(ctx, ctx.eigrp_asn());
      assert asn.isPresent();
      r.getSpecialAttributes().put(OspfRedistributionPolicy.EIGRP_AS_NUMBER, asn.get());
      return r;
    } else {
      assert ctx.STATIC() != null;
      return new OspfRedistributionPolicy(RoutingProtocol.STATIC);
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
  public void exitRoa_interface(Roa_interfaceContext ctx) {
    _currentOspfInterface = null;
  }

  @Override
  public void exitRoa_network(Roa_networkContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitRoa_range(Roa_rangeContext ctx) {
    Prefix prefix = Prefix.parse(ctx.prefix.getText());
    boolean advertise = ctx.NOT_ADVERTISE() == null;
    Long cost = ctx.cost == null ? null : toLong(ctx.cost);

    Map<Prefix, OspfAreaSummary> area =
        _currentOspfProcess.getSummaries().computeIfAbsent(_currentOspfArea, k -> new TreeMap<>());
    area.put(
        prefix,
        new OspfAreaSummary(
            advertise
                ? SummaryRouteBehavior.ADVERTISE_AND_INSTALL_DISCARD
                : SummaryRouteBehavior.NOT_ADVERTISE_AND_NO_DISCARD,
            cost));
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
    String name = toString(ctx.name);
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
  public void exitRoute_reflector_client_bgp_tail(Route_reflector_client_bgp_tailContext ctx) {
    _currentPeerGroup.setRouteReflectorClient(true);
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
  public void exitS_hostname(S_hostnameContext ctx) {
    String hostname = toString(ctx.hostname);
    _configuration.setHostname(hostname);
    _configuration.getCf().setHostname(hostname);
  }

  @Override
  public void exitS_interface(S_interfaceContext ctx) {
    _currentInterfaces = null;
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
  public void exitS_tacacs_server(S_tacacs_serverContext ctx) {
    _no = false;
  }

  @Override
  public void exitS_username(S_usernameContext ctx) {
    _currentUser = null;
  }

  @Override
  public void exitS_vrf(S_vrfContext ctx) {
    _currentVrf = Configuration.DEFAULT_VRF_NAME;
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
  public void exitSs_host(Ss_hostContext ctx) {
    _currentSnmpHost = null;
  }

  @Override
  public void exitSs_trap_source(Ss_trap_sourceContext ctx) {
    String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
    _configuration.setSnmpSourceInterface(ifaceName);
    _configuration.referenceStructure(
        INTERFACE, ifaceName, SNMP_SERVER_TRAP_SOURCE, ctx.iname.getStart().getLine());
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
  public void exitSsh_server(Ssh_serverContext ctx) {
    if (ctx.acl != null) {
      String acl = toString(ctx.acl);
      int line = ctx.acl.getStart().getLine();
      _configuration.referenceStructure(IPV4_ACCESS_LIST, acl, SSH_IPV4_ACL, line);
    }
    if (ctx.acl6 != null) {
      String acl6 = toString(ctx.acl6);
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
  public void exitVrf_description(Vrf_descriptionContext ctx) {
    currentVrf().setDescription(getDescription(ctx.description_line()));
  }

  @Override
  public void exitVrrp_interface(Vrrp_interfaceContext ctx) {
    _currentVrrpInterface = null;
  }

  @Override
  public void exitVrf_afi_route_target_value(Vrf_afi_route_target_valueContext ctx) {
    _currentVrfAddressFamily.addRouteTargetImport(toRouteTarget(ctx.route_target()));
  }

  @Override
  public void exitVrf_afe_route_target_value(Vrf_afe_route_target_valueContext ctx) {
    _currentVrfAddressFamily.addRouteTargetExport(toRouteTarget(ctx.route_target()));
  }

  @Override
  public void exitVrf_afe_route_policy(Vrf_afe_route_policyContext ctx) {
    String policy = toString(ctx.policy);
    _configuration.referenceStructure(
        ROUTE_POLICY, policy, VRF_EXPORT_ROUTE_POLICY, ctx.start.getLine());
    _currentVrfAddressFamily.setExportPolicy(policy);
  }

  @Override
  public void exitVrf_afet_default_vrf(Vrf_afet_default_vrfContext ctx) {
    if (ctx.ALLOW_IMPORTED_VPN() != null) {
      todo(ctx);
    }
    String policy = toString(ctx.policy);
    _configuration.referenceStructure(
        ROUTE_POLICY, policy, VRF_EXPORT_TO_DEFAULT_VRF_ROUTE_POLICY, ctx.start.getLine());
    _currentVrfAddressFamily.setExportToDefaultVrfPolicy(policy);
  }

  @Override
  public void exitVrf_afet_vrf(Vrf_afet_vrfContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitVrf_afi_route_policy(Vrf_afi_route_policyContext ctx) {
    String policy = toString(ctx.policy);
    _configuration.referenceStructure(
        ROUTE_POLICY, policy, VRF_IMPORT_ROUTE_POLICY, ctx.start.getLine());
    _currentVrfAddressFamily.setImportPolicy(policy);
  }

  @Override
  public void exitVrf_afif_default_vrf(Vrf_afif_default_vrfContext ctx) {
    if (ctx.ADVERTISE_AS_VPN() != null) {
      todo(ctx);
    }
    String policy = toString(ctx.policy);
    _configuration.referenceStructure(
        ROUTE_POLICY, policy, VRF_IMPORT_FROM_DEFAULT_VRF_ROUTE_POLICY, ctx.start.getLine());
    _currentVrfAddressFamily.setImportFromDefaultVrfPolicy(policy);
  }

  @Override
  public void exitVrf_afif_vrf(Vrf_afif_vrfContext ctx) {
    todo(ctx);
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

  @Nonnull
  @Override
  public String getInputText() {
    return _text;
  }

  @Nonnull
  @Override
  public BatfishCombinedParser<?, ?> getParser() {
    return _parser;
  }

  @Nonnull
  @Override
  public Warnings getWarnings() {
    return _w;
  }

  private String getLocation(ParserRuleContext ctx) {
    return ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ": ";
  }

  private int getPortNumber(PortContext ctx) {
    if (ctx.uint_legacy() != null) {
      return toInteger(ctx.uint_legacy());
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
    if (ctx.uint_legacy() != null) {
      int as = toInteger(ctx.uint_legacy());
      return new ExplicitAs(as);
    } else if (ctx.AUTO() != null) {
      return AutoAs.instance();
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarAs(ctx.RP_VARIABLE().getText());
    } else {
      throw convError(AsExpr.class, ctx);
    }
  }

  @Override
  public void exitAspse_dfa_regex(Aspse_dfa_regexContext ctx) {
    String regex = toString(ctx.as_path_regex());
    _currentAsPathSet.addElement(new DfaRegexAsPathSetElem(regex));
  }

  @Override
  public void exitAspsee_dfa_regex(Aspsee_dfa_regexContext ctx) {
    String regex = toString(ctx.as_path_regex());
    _currentInlineAsPathSet.addElement(new DfaRegexAsPathSetElem(regex));
  }

  @Override
  public void exitAspse_ios_regex(Aspse_ios_regexContext ctx) {
    String regex = toString(ctx.as_path_regex());
    _currentAsPathSet.addElement(new IosRegexAsPathSetElem(regex));
  }

  @Override
  public void exitAspsee_ios_regex(Aspsee_ios_regexContext ctx) {
    String regex = toString(ctx.as_path_regex());
    _currentInlineAsPathSet.addElement(new IosRegexAsPathSetElem(regex));
  }

  @Override
  public void exitAspse_length(Aspse_lengthContext ctx) {
    toInteger(ctx, ctx.as_path_length())
        .ifPresent(
            length ->
                _currentAsPathSet.addElement(
                    new LengthAsPathSetElem(
                        toIntComparator(ctx.comparator()), length, ctx.ALL() != null)));
  }

  @Override
  public void exitAspsee_length(Aspsee_lengthContext ctx) {
    toInteger(ctx, ctx.as_path_length_expr())
        .ifPresent(
            length ->
                _currentInlineAsPathSet.addElement(
                    new LengthAsPathSetElem(
                        toIntComparator(ctx.comparator()), length, ctx.ALL() != null)));
  }

  @Override
  public void exitAspse_neighbor_is(Aspse_neighbor_isContext ctx) {
    toRanges(ctx, ctx.as_range_list())
        .ifPresent(
            ranges ->
                _currentAsPathSet.addElement(
                    new NeighborIsAsPathSetElem(ctx.EXACT() != null, ranges)));
  }

  private @Nonnull Optional<List<Range<Long>>> toRanges(
      ParserRuleContext messageCtx, As_range_listContext ctx) {
    ImmutableList.Builder<Range<Long>> builder = ImmutableList.builder();
    for (As_rangeContext rangeCtx : ctx.as_range()) {
      Optional<Range<Long>> maybeRange = toRange(messageCtx, rangeCtx);
      if (!maybeRange.isPresent()) {
        return Optional.empty();
      }
      builder.add(maybeRange.get());
    }
    return Optional.of(builder.build());
  }

  private @Nonnull Optional<Range<Long>> toRange(
      ParserRuleContext messageCtx, As_rangeContext ctx) {
    Optional<Long> maybeLo = toLong(messageCtx, ctx.lo);
    if (!maybeLo.isPresent()) {
      return Optional.empty();
    }
    if (ctx.hi != null) {
      return toLong(messageCtx, ctx.hi).map(hi -> Range.closed(maybeLo.get(), hi));
    } else {
      return Optional.of(Range.singleton(maybeLo.get()));
    }
  }

  private @Nonnull Optional<Long> toLong(ParserRuleContext messageCtx, As_numberContext ctx) {
    if (ctx.uint32() != null) {
      return toLongInSpace(messageCtx, ctx, AS_NUMBER_RANGE, "AS number");
    } else {
      assert ctx.hi != null && ctx.lo != null;
      long hi = toInteger(ctx.hi);
      long lo = toInteger(ctx.lo);
      long val = (hi << 16) | lo;
      if (!AS_NUMBER_RANGE.contains(val)) {
        warn(
            messageCtx,
            String.format(
                "%s expressed as a single number is %s, which is not in the range of valid ASes:"
                    + " %s",
                getFullText(ctx), val, AS_NUMBER_RANGE));
        return Optional.empty();
      }
      return Optional.of(val);
    }
  }

  @Override
  public void exitAspsee_neighbor_is(Aspsee_neighbor_isContext ctx) {
    toRanges(ctx, ctx.as_range_expr_list())
        .ifPresent(
            ranges ->
                _currentInlineAsPathSet.addElement(
                    new NeighborIsAsPathSetElem(ctx.EXACT() != null, ranges)));
  }

  private @Nonnull Optional<List<Range<Long>>> toRanges(
      ParserRuleContext messageCtx, As_range_expr_listContext ctx) {
    // TODO: change return type to accomodate params
    ImmutableList.Builder<Range<Long>> builder = ImmutableList.builder();
    for (As_range_exprContext rangeCtx : ctx.as_range_expr()) {
      Optional<Range<Long>> maybeRange = toRange(messageCtx, rangeCtx);
      if (!maybeRange.isPresent()) {
        return Optional.empty();
      }
      builder.add(maybeRange.get());
    }
    return Optional.of(builder.build());
  }

  private @Nonnull Optional<Range<Long>> toRange(
      ParserRuleContext messageCtx, As_range_exprContext ctx) {
    Optional<Long> maybeLo = toLong(messageCtx, ctx.lo);
    if (!maybeLo.isPresent()) {
      return Optional.empty();
    }
    if (ctx.hi != null) {
      return toLong(messageCtx, ctx.hi).map(hi -> Range.closed(maybeLo.get(), hi));
    } else {
      return Optional.of(Range.singleton(maybeLo.get()));
    }
  }

  private @Nonnull Optional<Long> toLong(ParserRuleContext messageCtx, As_number_exprContext ctx) {
    if (ctx.param != null) {
      todo(ctx);
      return Optional.empty();
    }
    assert ctx.num != null;
    return toLong(messageCtx, ctx.num);
  }

  @Override
  public void exitAspse_originates_from(Aspse_originates_fromContext ctx) {
    toRanges(ctx, ctx.as_range_list())
        .ifPresent(
            ranges ->
                _currentAsPathSet.addElement(
                    new OriginatesFromAsPathSetElem(ctx.EXACT() != null, ranges)));
  }

  @Override
  public void exitAspsee_originates_from(Aspsee_originates_fromContext ctx) {
    toRanges(ctx, ctx.as_range_expr_list())
        .ifPresent(
            ranges ->
                _currentInlineAsPathSet.addElement(
                    new OriginatesFromAsPathSetElem(ctx.EXACT() != null, ranges)));
  }

  @Override
  public void exitAspse_passes_through(Aspse_passes_throughContext ctx) {
    toRanges(ctx, ctx.as_range_list())
        .ifPresent(
            ranges ->
                _currentAsPathSet.addElement(
                    new PassesThroughAsPathSetElem(ctx.EXACT() != null, ranges)));
  }

  @Override
  public void exitAspsee_passes_through(Aspsee_passes_throughContext ctx) {
    toRanges(ctx, ctx.as_range_expr_list())
        .ifPresent(
            ranges ->
                _currentInlineAsPathSet.addElement(
                    new PassesThroughAsPathSetElem(ctx.EXACT() != null, ranges)));
  }

  @Override
  public void exitAspse_unique_length(Aspse_unique_lengthContext ctx) {
    toInteger(ctx, ctx.as_path_length())
        .ifPresent(
            length ->
                _currentAsPathSet.addElement(
                    new UniqueLengthAsPathSetElem(
                        toIntComparator(ctx.comparator()), length, ctx.ALL() != null)));
  }

  @Override
  public void exitAspsee_unique_length(Aspsee_unique_lengthContext ctx) {
    toInteger(ctx, ctx.as_path_length_expr())
        .ifPresent(
            length ->
                _currentInlineAsPathSet.addElement(
                    new UniqueLengthAsPathSetElem(
                        toIntComparator(ctx.comparator()), length, ctx.ALL() != null)));
  }

  private @Nonnull Optional<Integer> toInteger(ParserRuleContext messageCtx, Eigrp_asnContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, EIGRP_ASN_RANGE, "EIGRP ASN");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Ospf_metricContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, OSPF_METRIC_RANGE, "OSPF metric");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Ospf_metric_typeContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, OSPF_METRIC_TYPE_RANGE, "OSPF metric type");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, Ifrit_pop_countContext ctx) {
    return toIntegerInSpace(
        messageCtx, ctx, REWRITE_INGRESS_TAG_POP_RANGE, "rewrite ingress tag pop range");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, As_path_lengthContext ctx) {
    return toIntegerInSpace(messageCtx, ctx, AS_PATH_LENGTH_RANGE, "as-path length");
  }

  private @Nonnull Optional<Integer> toInteger(
      ParserRuleContext messageCtx, As_path_length_exprContext ctx) {
    if (ctx.parameter() != null) {
      todo(ctx);
      return Optional.empty();
    } else {
      return toInteger(messageCtx, ctx.as_path_length());
    }
  }

  private static @Nonnull IntComparator toIntComparator(ComparatorContext ctx) {
    if (ctx.EQ() != null) {
      return IntComparator.EQ;
    } else if (ctx.GE() != null) {
      return IntComparator.GE;
    } else if (ctx.IS() != null) {
      return IntComparator.EQ;
    } else {
      assert ctx.LE() != null;
      return IntComparator.LE;
    }
  }

  private static @Nonnull String toString(As_path_regexContext ctx) {
    return ctx.AS_PATH_REGEX().getText();
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
    if (ctx.uint_legacy() != null && ctx.PLUS() == null && ctx.DASH() == null) {
      int val = toInteger(ctx.uint_legacy());
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
    if (ctx.uint_legacy() != null && ctx.PLUS() == null && ctx.DASH() == null) {
      long val = toLong(ctx.uint_legacy());
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

  @Nonnull
  private XrCommunitySetElem toXrCommunitySetElem(Community_set_expr_elemContext ctx) {
    if (ctx.literal != null) {
      long value = toLong(ctx.literal);
      return XrCommunitySetHighLowRangeExprs.of(value);
    } else {
      assert ctx.hi != null;
      assert ctx.lo != null;
      Uint16RangeExpr prefix = toUint16RangeExpr(ctx.hi);
      Uint16RangeExpr suffix = toUint16RangeExpr(ctx.lo);
      return new XrCommunitySetHighLowRangeExprs(prefix, suffix);
    }
  }

  @Nonnull
  private XrCommunitySetElem toXrCommunitySetElem(Community_set_match_expr_elemContext ctx) {
    if (ctx.ASTERISK() != null) {
      return XrWildcardCommunitySetElem.instance();
    } else if (ctx.literal_community() != null) {
      long value = toLong(ctx.literal_community());
      return XrCommunitySetHighLowRangeExprs.of(value);
    } else if (ctx.hi != null) {
      assert ctx.lo != null;
      Uint16RangeExpr prefix = toUint16RangeExpr(ctx.hi);
      Uint16RangeExpr suffix = toUint16RangeExpr(ctx.lo);
      return new XrCommunitySetHighLowRangeExprs(prefix, suffix);
    } else if (ctx.DFA_REGEX() != null) {
      todo(ctx);
      return new XrCommunitySetDfaRegex(unquote(ctx.COMMUNITY_SET_REGEX().getText()));
    } else {
      assert ctx.IOS_REGEX() != null;
      return new XrCommunitySetIosRegex(unquote(ctx.COMMUNITY_SET_REGEX().getText()));
    }
  }

  @Nonnull
  private XrCommunitySetElem toXrCommunitySetElem(Community_set_elemContext ctx) {
    if (ctx.ASTERISK() != null) {
      return XrWildcardCommunitySetElem.instance();
    } else if (ctx.literal_community() != null) {
      long value = toLong(ctx.literal_community());
      return XrCommunitySetHighLowRangeExprs.of(value);
    } else if (ctx.hi != null) {
      assert ctx.lo != null;
      Uint16RangeExpr prefix = toUint16RangeExpr(ctx.hi);
      Uint16RangeExpr suffix = toUint16RangeExpr(ctx.lo);
      return new XrCommunitySetHighLowRangeExprs(prefix, suffix);
    } else if (ctx.DFA_REGEX() != null) {
      todo(ctx);
      return new XrCommunitySetDfaRegex(unquote(ctx.COMMUNITY_SET_REGEX().getText()));
    } else {
      assert ctx.IOS_REGEX() != null;
      return new XrCommunitySetIosRegex(unquote(ctx.COMMUNITY_SET_REGEX().getText()));
    }
  }

  @Nonnull
  private Uint16RangeExpr toUint16RangeExpr(Community_set_expr_elem_halfContext ctx) {
    if (ctx.value != null) {
      int value = toInteger(ctx.value);
      return new LiteralUint16(value);
    } else if (ctx.param != null) {
      todo(ctx);
      return new Uint16Reference(toString(ctx.param));
    } else {
      assert ctx.PEERAS() != null;
      todo(ctx);
      return PeerAs.instance();
    }
  }

  @Nonnull
  private Uint16RangeExpr toUint16RangeExpr(Community_set_match_expr_elem_halfContext ctx) {
    if (ctx.ASTERISK() != null) {
      return WildcardUint16RangeExpr.instance();
    } else if (ctx.value != null) {
      int value = toInteger(ctx.value);
      return new LiteralUint16(value);
    } else if (ctx.param != null) {
      todo(ctx);
      return new Uint16Reference(toString(ctx.param));
    } else if (ctx.first != null) {
      assert ctx.last != null;
      int first = toInteger(ctx.first);
      int last = toInteger(ctx.last);
      SubRange range = new SubRange(first, last);
      return new LiteralUint16Range(range);
    } else if (ctx.PEERAS() != null) {
      todo(ctx);
      return PeerAs.instance();
    } else {
      todo(ctx);
      assert ctx.PRIVATE_AS() != null;
      return PrivateAs.instance();
    }
  }

  @Nonnull
  private static String toString(ParameterContext ctx) {
    return ctx.getText();
  }

  @Nonnull
  private Uint16RangeExpr toUint16RangeExpr(Community_set_elem_halfContext ctx) {
    if (ctx.ASTERISK() != null) {
      return WildcardUint16RangeExpr.instance();
    } else if (ctx.value != null) {
      int value = toInteger(ctx.value);
      return new LiteralUint16(value);
    } else if (ctx.first != null) {
      assert ctx.last != null;
      int first = toInteger(ctx.first);
      int last = toInteger(ctx.last);
      SubRange range = new SubRange(first, last);
      return new LiteralUint16Range(range);
    } else {
      todo(ctx);
      assert ctx.PRIVATE_AS() != null;
      return PrivateAs.instance();
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

  private static int toInteger(Uint8Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private static int toInteger(Uint16Context ctx) {
    return Integer.parseInt(ctx.getText());
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

  @Nonnull
  private Optional<Integer> toDscpType(ParserRuleContext messageCtx, Dscp_typeContext ctx) {
    int val;
    if (ctx.dscp_num() != null) {
      return toInteger(messageCtx, ctx.dscp_num());
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
    } else {
      assert ctx.EF() != null;
      val = DscpType.EF.number();
    }
    return Optional.of(val);
  }

  @Nonnull
  private Optional<Integer> toInteger(ParserRuleContext messageCtx, Dscp_numContext ctx) {
    return toIntegerInSpace(messageCtx, ctx.uint8(), DSCP_RANGE, "DSCP number");
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
    if (ctx.uint_legacy() != null && ctx.PLUS() == null && ctx.DASH() == null) {
      int val = toInteger(ctx.uint_legacy());
      return new LiteralInt(val);
    } else if (ctx.RP_VARIABLE() != null) {
      return new VarInt(ctx.RP_VARIABLE().getText());
    } else {
      throw convError(IntExpr.class, ctx);
    }
  }

  /** Returns the given IPv4 protocol, or {@code null} if none is specified. */
  private @Nullable IpProtocol toIpProtocol(ProtocolContext ctx) {
    if (ctx.uint_legacy() != null) {
      int num = toInteger(ctx.uint_legacy());
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
    if (ctx.uint_legacy() != null) {
      int val = toInteger(ctx.uint_legacy());
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

  @Nonnull
  private static String toString(Host_nameContext ctx) {
    return ctx.getText();
  }

  private static int toLoggingSeverityNum(Logging_buffered_severityContext ctx) {
    if (ctx.EMERGENCIES() != null) {
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
    } else {
      assert ctx.DEBUGGING() != null;
      return 7;
    }
  }

  private static int toLoggingSeverityNum(Logging_console_severityContext ctx) {
    if (ctx.EMERGENCIES() != null) {
      return 0;
    } else if (ctx.ALERTS() != null) {
      return 1;
    } else if (ctx.CRITICAL() != null) {
      return 2;
    } else if (ctx.ERRORS() != null) {
      return 3;
    } else if (ctx.WARNING() != null) {
      return 4;
    } else if (ctx.NOTIFICATIONS() != null) {
      return 5;
    } else if (ctx.INFORMATIONAL() != null) {
      return 6;
    } else {
      assert ctx.DEBUGGING() != null;
      return 7;
    }
  }

  private static int toLoggingSeverityNum(Logging_trap_severityContext ctx) {
    if (ctx.EMERGENCIES() != null) {
      return 0;
    } else if (ctx.ALERTS() != null) {
      return 1;
    } else if (ctx.CRITICAL() != null) {
      return 2;
    } else if (ctx.ERRORS() != null) {
      return 3;
    } else if (ctx.WARNING() != null) {
      return 4;
    } else if (ctx.NOTIFICATIONS() != null) {
      return 5;
    } else if (ctx.INFORMATIONAL() != null) {
      return 6;
    } else {
      assert ctx.DEBUGGING() != null;
      return 7;
    }
  }

  @Nonnull
  private static long toLong(Literal_communityContext ctx) {
    if (ctx.ACCEPT_OWN() != null) {
      return WellKnownCommunity.ACCEPT_OWN;
    } else if (ctx.hi != null) {
      assert ctx.lo != null;
      return StandardCommunity.of(toInteger(ctx.hi), toInteger(ctx.lo)).asLong();
    } else if (ctx.INTERNET() != null) {
      return WellKnownCommunity.INTERNET;
    } else if (ctx.GRACEFUL_SHUTDOWN() != null) {
      return WellKnownCommunity.GRACEFUL_SHUTDOWN;
    } else if (ctx.LOCAL_AS() != null) {
      // CiscoXr LOCAL_AS is interpreted as RFC1997 NO_EXPORT_SUBCONFED: internet forums.
      return WellKnownCommunity.NO_EXPORT_SUBCONFED;
    } else if (ctx.NO_ADVERTISE() != null) {
      return WellKnownCommunity.NO_ADVERTISE;
    } else {
      assert ctx.NO_EXPORT() != null;
      return WellKnownCommunity.NO_EXPORT;
    }
  }

  private static long toLong(Route_tag_from_0Context ctx) {
    // All values in uint32 range are valid
    return toLong(ctx.uint32());
  }

  private static long toLong(Uint32Context ctx) {
    return Long.parseLong(ctx.getText());
  }

  private LongExpr toMetricLongExpr(Int_exprContext ctx) {
    if (ctx.uint_legacy() != null) {
      long val = toLong(ctx.uint_legacy());
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
    String name = toString(ctx.name);
    _configuration.referenceStructure(
        ROUTE_POLICY, name, ROUTE_POLICY_APPLY_EXPR, ctx.name.getStart().getLine());
    return new RoutePolicyBooleanApply(name);
  }

  @Override
  public void exitBoolean_as_path(Boolean_as_pathContext ctx) {
    _asPathBooleansByCtx.put(
        ctx, firstNonNull(_currentAsPathBoolean, UnimplementedBoolean.instance()));
    _currentAsPathBoolean = null;
  }

  private @Nullable RoutePolicyBoolean toRoutePolicyBoolean(Boolean_as_pathContext ctx) {
    RoutePolicyBoolean result = _asPathBooleansByCtx.remove(ctx);
    assert result != null;
    return result;
  }

  @Override
  public void enterBoolean_as_path_in(Boolean_as_path_inContext ctx) {
    if (ctx.PAREN_LEFT() != null) {
      // TODO: use structure that supports route-policy params
      _currentInlineAsPathSet = new AsPathSet();
    }
  }

  @Override
  public void exitBoolean_as_path_in(Boolean_as_path_inContext ctx) {
    AsPathSetExpr asPathSetExpr;
    if (ctx.name != null) {
      String name = toString(ctx.name);
      asPathSetExpr = new AsPathSetReference(name);
      _configuration.referenceStructure(
          AS_PATH_SET, name, ROUTE_POLICY_AS_PATH_IN, ctx.name.getStart().getLine());
    } else if (ctx.param != null) {
      todo(ctx);
      asPathSetExpr = new AsPathSetVariable(toString(ctx.param));
    } else {
      assert _currentInlineAsPathSet != null;
      asPathSetExpr = new InlineAsPathSet(_currentInlineAsPathSet);
      _currentInlineAsPathSet = null;
    }
    _currentAsPathBoolean = new RoutePolicyBooleanAsPathIn(asPathSetExpr);
  }

  @Override
  public void exitBoolean_as_path_is_local(Boolean_as_path_is_localContext ctx) {
    _currentAsPathBoolean = RoutePolicyBooleanAsPathIsLocal.instance();
  }

  @Override
  public void exitBoolean_as_path_length(Boolean_as_path_lengthContext ctx) {
    toInteger(ctx, ctx.as_path_length_expr())
        .ifPresent(
            length ->
                _currentAsPathBoolean =
                    new RoutePolicyBooleanAsPathLength(
                        toIntComparator(ctx.comparator()), length, ctx.ALL() != null));
  }

  @Override
  public void exitBoolean_as_path_neighbor_is(Boolean_as_path_neighbor_isContext ctx) {
    toRanges(ctx, ctx.as_range_expr_list())
        .ifPresent(
            ranges ->
                _currentAsPathBoolean =
                    new RoutePolicyBooleanAsPathNeighborIs(ctx.EXACT() != null, ranges));
  }

  @Override
  public void exitBoolean_as_path_originates_from(Boolean_as_path_originates_fromContext ctx) {
    toRanges(ctx, ctx.as_range_expr_list())
        .ifPresent(
            ranges ->
                _currentAsPathBoolean =
                    new RoutePolicyBooleanAsPathOriginatesFrom(ctx.EXACT() != null, ranges));
  }

  @Override
  public void exitBoolean_as_path_passes_through(Boolean_as_path_passes_throughContext ctx) {
    toRanges(ctx, ctx.as_range_expr_list())
        .ifPresent(
            ranges ->
                _currentAsPathBoolean =
                    new RoutePolicyBooleanAsPathPassesThrough(ctx.EXACT() != null, ranges));
  }

  @Override
  public void exitBoolean_as_path_unique_length(Boolean_as_path_unique_lengthContext ctx) {
    toInteger(ctx, ctx.as_path_length_expr())
        .ifPresent(
            length ->
                _currentAsPathBoolean =
                    new RoutePolicyBooleanAsPathUniqueLength(
                        toIntComparator(ctx.comparator()), length, ctx.ALL() != null));
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_community_matches_any_rp_stanzaContext ctx) {
    XrCommunitySetExpr communitySet =
        toXrCommunitySetExpr(ctx.community_set_match_expr(), ROUTE_POLICY_COMMUNITY_MATCHES_ANY);
    return new XrRoutePolicyBooleanCommunityMatchesAny(communitySet);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_community_matches_every_rp_stanzaContext ctx) {
    XrCommunitySetExpr communitySet =
        toXrCommunitySetExpr(ctx.community_set_match_expr(), ROUTE_POLICY_COMMUNITY_MATCHES_EVERY);
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

    Boolean_as_pathContext aspctx = ctx.boolean_as_path();
    if (aspctx != null) {
      return toRoutePolicyBoolean(aspctx);
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

    Boolean_rd_in_rp_stanzaContext rdctx = ctx.boolean_rd_in_rp_stanza();
    if (rdctx != null) {
      return toRoutePolicyBoolean(rdctx);
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

    Boolean_validation_state_is_rp_stanzaContext vsctx =
        ctx.boolean_validation_state_is_rp_stanza();
    if (vsctx != null) {
      return toRoutePolicyBoolean(vsctx);
    }

    throw convError(RoutePolicyBoolean.class, ctx);
  }

  @Nonnull
  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_rd_in_rp_stanzaContext ctx) {
    todo(ctx);
    return new RoutePolicyBooleanRdIn(toRdMatchExpr(ctx.rd_match_expr()));
  }

  @Nonnull
  private RdMatchExpr toRdMatchExpr(Rd_match_exprContext ctx) {
    // TODO: add case for inline rd match expression
    if (ctx.name != null) {
      String name = toString(ctx.name);
      _configuration.referenceStructure(RD_SET, name, ROUTE_POLICY_RD_IN, ctx.start.getLine());
      return new RdSetReference(name);
    } else {
      assert ctx.param != null;
      todo(ctx);
      return new RdSetParameterReference(toString(ctx.param));
    }
  }

  @Nonnull
  private static String toString(Rd_set_nameContext name) {
    return name.getText();
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(Boolean_tag_is_rp_stanzaContext ctx) {
    IntComparator cmp = toIntComparator(ctx.int_comp());
    LongExpr rhs = toTagLongExpr(ctx.int_expr());
    return new RoutePolicyBooleanTagIs(cmp, rhs);
  }

  private RoutePolicyBoolean toRoutePolicyBoolean(
      Boolean_validation_state_is_rp_stanzaContext ctx) {
    boolean valid = ctx.rp_validation_state().VALID() != null;
    return new RoutePolicyBooleanValidationStateIs(valid);
  }

  private XrCommunitySetExpr toXrCommunitySetExpr(
      Community_set_match_exprContext ctx, CiscoXrStructureUsage usage) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      _configuration.referenceStructure(COMMUNITY_SET, name, usage, ctx.name.getStart().getLine());
      return new XrCommunitySetReference(name);
    } else if (ctx.param != null) {
      todo(ctx);
      return new XrCommunitySetParameterReference(toString(ctx.param));
    } else {
      // inline
      assert !ctx.elems.isEmpty();
      return new XrInlineCommunitySet(
          new XrCommunitySet(
              ctx.elems.stream()
                  .map(this::toXrCommunitySetElem)
                  .collect(ImmutableList.toImmutableList())));
    }
  }

  private XrCommunitySetExpr toXrCommunitySetExpr(
      Community_set_exprContext ctx, CiscoXrStructureUsage usage) {
    if (ctx.name != null) {
      String name = ctx.name.getText();
      _configuration.referenceStructure(COMMUNITY_SET, name, usage, ctx.name.getStart().getLine());
      return new XrCommunitySetReference(name);
    } else if (ctx.param != null) {
      todo(ctx);
      return new XrCommunitySetParameterReference(toString(ctx.param));
    } else {
      // inline
      assert !ctx.elems.isEmpty();
      return new XrInlineCommunitySet(
          new XrCommunitySet(
              ctx.elems.stream()
                  .map(this::toXrCommunitySetElem)
                  .collect(ImmutableList.toImmutableList())));
    }
  }

  @Override
  public void enterElse_rp_stanza(Else_rp_stanzaContext ctx) {
    RoutePolicyElseBlock currentElse = new RoutePolicyElseBlock();
    _statementCollectors.push(currentElse::addStatement);
    _ifs.peek().setElseBlock(currentElse);
  }

  @Override
  public void exitElse_rp_stanza(Else_rp_stanzaContext ctx) {
    _statementCollectors.pop();
  }

  @Override
  public void enterElseif_rp_stanza(Elseif_rp_stanzaContext ctx) {
    RoutePolicyElseIfBlock currentElseIf = new RoutePolicyElseIfBlock();
    _elseIfs.push(currentElseIf);
    _statementCollectors.push(currentElseIf::addStatement);
  }

  @Override
  public void exitElseif_rp_stanza(Elseif_rp_stanzaContext ctx) {
    _statementCollectors.pop();
    RoutePolicyElseIfBlock elseIf = _elseIfs.pop();
    _ifs.peek().addElseIfBlock(elseIf);
    // TODO: listener-style creation of guard
    RoutePolicyBoolean b = toRoutePolicyBoolean(ctx.boolean_rp_stanza());
    elseIf.setGuard(b);
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

  @Override
  public void exitApply_rp_stanza(Apply_rp_stanzaContext ctx) {
    String name = toString(ctx.name);
    _configuration.referenceStructure(
        ROUTE_POLICY, name, ROUTE_POLICY_APPLY_STATEMENT, ctx.name.getStart().getLine());
    addStatement(new RoutePolicyApplyStatement(name));
  }

  @Override
  public void exitDelete_community_rp_stanza(Delete_community_rp_stanzaContext ctx) {
    RoutePolicyStatement statement;
    if (ctx.ALL() != null) {
      statement = XrRoutePolicyDeleteAllStatement.instance();
    } else {
      boolean negated = (ctx.NOT() != null);
      statement =
          new XrRoutePolicyDeleteCommunityStatement(
              negated,
              toXrCommunitySetExpr(
                  ctx.community_set_match_expr(), ROUTE_POLICY_DELETE_COMMUNITY_IN));
    }
    addStatement(statement);
  }

  @Override
  public void exitDisposition_rp_stanza(Disposition_rp_stanzaContext ctx) {
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
    addStatement(new RoutePolicyDispositionStatement(t));
  }

  @Override
  public void exitHash_comment(Hash_commentContext ctx) {
    String text = ctx.RAW_TEXT().getText();
    addStatement(new RoutePolicyComment(text));
  }

  @Override
  public void enterIf_rp_stanza(If_rp_stanzaContext ctx) {
    RoutePolicyIfStatement currentIf = new RoutePolicyIfStatement();
    _ifs.push(currentIf);
    _statementCollectors.push(currentIf::addStatement);
  }

  @Override
  public void exitIf_rp_stanza(If_rp_stanzaContext ctx) {
    RoutePolicyIfStatement currentIf = _ifs.pop();
    // TODO: listener-style creation of guard
    RoutePolicyBoolean b = toRoutePolicyBoolean(ctx.boolean_rp_stanza());
    currentIf.setGuard(b);
    _statementCollectors.pop();
    addStatement(currentIf);
  }

  @Override
  public void exitPrepend_as_path_rp_stanza(Prepend_as_path_rp_stanzaContext ctx) {
    AsExpr expr = toAsExpr(ctx.as);
    IntExpr number = null;
    if (ctx.number != null) {
      number = toIntExpr(ctx.number);
    }
    addStatement(new RoutePolicyPrependAsPath(expr, number));
  }

  @Override
  public void exitSet_community_rp_stanza(Set_community_rp_stanzaContext ctx) {
    XrCommunitySetExpr cset =
        toXrCommunitySetExpr(ctx.community_set_expr(), ROUTE_POLICY_SET_COMMUNITY);
    boolean additive = (ctx.ADDITIVE() != null);
    addStatement(new XrRoutePolicySetCommunity(cset, additive));
  }

  @Override
  public void exitSet_extcommunity_rt(Set_extcommunity_rtContext ctx) {
    ExtcommunitySetRtExpr expr =
        toExtcommunitySetRtExpr(ctx.rp_extcommunity_set_rt(), ROUTE_POLICY_SET_EXTCOMMUNITY_RT);
    boolean additive = (ctx.ADDITIVE() != null);
    addStatement(new RoutePolicySetExtcommunityRt(expr, additive));
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

  @Override
  public void exitSet_isis_metric_rp_stanza(Set_isis_metric_rp_stanzaContext ctx) {
    LongExpr metric = toCommonLongExpr(ctx.int_expr());
    addStatement(new RoutePolicySetIsisMetric(metric));
  }

  @Override
  public void exitSet_level_rp_stanza(Set_level_rp_stanzaContext ctx) {
    addStatement(new RoutePolicySetLevel(toIsisLevelExpr(ctx.isis_level_expr())));
  }

  @Override
  public void exitSet_local_preference_rp_stanza(Set_local_preference_rp_stanzaContext ctx) {
    addStatement(new RoutePolicySetLocalPref(toLocalPreferenceLongExpr(ctx.pref)));
  }

  @Override
  public void exitSet_med_rp_stanza(Set_med_rp_stanzaContext ctx) {
    addStatement(new RoutePolicySetMed(toMetricLongExpr(ctx.med)));
  }

  @Override
  public void exitSet_metric_type_rp_stanza(Set_metric_type_rp_stanzaContext ctx) {
    Rp_metric_typeContext t = ctx.type;
    if (t.rp_ospf_metric_type() != null) {
      addStatement(new RoutePolicySetOspfMetricType(toOspfMetricType(t.rp_ospf_metric_type())));
    } else if (t.rp_isis_metric_type() != null) {
      addStatement(new RoutePolicySetIsisMetricType(toIsisMetricType(t.rp_isis_metric_type())));
    } else {
      assert t.RP_VARIABLE() != null;
      todo(ctx);
    }
  }

  @Override
  public void exitSet_next_hop_rp_stanza(Set_next_hop_rp_stanzaContext ctx) {
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
    addStatement(new RoutePolicySetNextHop(hop, destVrf));
  }

  @Override
  public void exitSet_origin_rp_stanza(Set_origin_rp_stanzaContext ctx) {
    OriginExpr origin = toOriginExpr(ctx.origin_expr());
    addStatement(new RoutePolicySetOrigin(origin));
  }

  private <T, U extends T> T convProblem(
      Class<T> returnType, ParserRuleContext ctx, U defaultReturnValue) {
    warn(ctx, convErrorMessage(returnType, ctx));
    return defaultReturnValue;
  }

  @Override
  public void exitSet_tag_rp_stanza(Set_tag_rp_stanzaContext ctx) {
    LongExpr tag = toTagLongExpr(ctx.tag);
    addStatement(new RoutePolicySetTag(tag));
  }

  @Override
  public void exitSet_weight_rp_stanza(Set_weight_rp_stanzaContext ctx) {
    IntExpr weight = toCommonIntExpr(ctx.weight);
    addStatement(new RoutePolicySetWeight(weight));
  }

  @Override
  public void exitSet_next_hop_self_rp_stanza(Set_next_hop_self_rp_stanzaContext ctx) {
    addStatement(new RoutePolicySetNextHop(new RoutePolicyNextHopSelf(), false));
  }

  @Override
  public void exitSet_path_selection_rp_stanza(Set_path_selection_rp_stanzaContext ctx) {
    todo(ctx);
  }

  @Nonnull
  private RouteDistinguisher toRouteDistinguisher(Route_distinguisherContext ctx) {
    long dec = toLong(ctx.uint_legacy());
    if (ctx.IP_ADDRESS() != null) {
      checkArgument(dec <= 0xFFFFL, "Invalid route distinguisher %s", ctx.getText());
      return RouteDistinguisher.from(toIp(ctx.IP_ADDRESS()), (int) dec);
    }
    return RouteDistinguisher.from(toAsNum(ctx.bgp_asn()), dec);
  }

  @Nonnull
  private ExtendedCommunity toRouteTarget(Route_targetContext ctx) {
    long la = toLong(ctx.uint_legacy());
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

  private LongExpr toTagLongExpr(Int_exprContext ctx) {
    if (ctx.uint_legacy() != null && ctx.DASH() == null && ctx.PLUS() == null) {
      long val = toLong(ctx.uint_legacy());
      return new LiteralLong(val);
    } else if (ctx.RP_VARIABLE() != null) {
      String var = ctx.RP_VARIABLE().getText();
      return new VarLong(var);
    } else {
      throw convError(LongExpr.class, ctx);
    }
  }

  @Nonnull
  private static String toString(Vrf_nameContext ctx) {
    return ctx.getText();
  }

  @Nonnull
  private static String toString(Route_policy_nameContext ctx) {
    return ctx.getText();
  }

  @Nonnull
  private static String toString(Access_list_nameContext ctx) {
    return ctx.getText();
  }

  private void warnObjectGroupRedefinition(ParserRuleContext name) {
    ParserRuleContext outer = firstNonNull(name.getParent(), name);
    warn(outer, "Object group defined multiple times: '" + name.getText() + "'.");
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

  /**
   * Convert a {@link ParserRuleContext} whose text is guaranteed to represent a valid signed 32-bit
   * decimal integer to an {@link Integer} if it is contained in the provided {@code space}, or else
   * {@link Optional#empty}.
   */
  private @Nonnull Optional<Integer> toIntegerInSpace(
      ParserRuleContext messageCtx, ParserRuleContext ctx, IntegerSpace space, String name) {
    int num = Integer.parseInt(ctx.getText());
    if (!space.contains(num)) {
      warn(messageCtx, String.format("Expected %s in range %s, but got '%d'", name, space, num));
      return Optional.empty();
    }
    return Optional.of(num);
  }

  /**
   * Convert a {@link ParserRuleContext} whose text is guaranteed to represent a valid signed 32-bit
   * decimal long to a {@link Long} if it is contained in the provided {@code space}, or else {@link
   * Optional#empty}.
   */
  private @Nonnull Optional<Long> toLongInSpace(
      ParserRuleContext messageCtx, ParserRuleContext ctx, LongSpace space, String name) {
    long num = Long.parseLong(ctx.getText());
    if (!space.contains(num)) {
      warn(messageCtx, String.format("Expected %s in range %s, but got '%d'", name, space, num));
      return Optional.empty();
    }
    return Optional.of(num);
  }

  private @Nonnull Optional<ConcreteInterfaceAddress> toConcreteInterfaceAddress(
      ParserRuleContext messageCtx, Interface_ipv4_addressContext ctx) {
    try {
      if (ctx.prefix != null) {
        return Optional.of(ConcreteInterfaceAddress.parse(ctx.prefix.getText()));
      } else if (ctx.address != null) {
        return Optional.of(ConcreteInterfaceAddress.create(toIp(ctx.address), toIp(ctx.mask)));
      }
    } catch (IllegalArgumentException e) {
      warn(messageCtx, "Invalid interface ipv4 address");
    }
    return Optional.empty();
  }

  @Override
  public void exitRigmp_access_group(Rigmp_access_groupContext ctx) {
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST, toString(ctx.name), ROUTER_IGMP_ACCESS_GROUP, ctx.start.getLine());
  }

  @Override
  public void exitRigmpi_access_group(Rigmpi_access_groupContext ctx) {
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST, toString(ctx.name), ROUTER_IGMP_ACCESS_GROUP, ctx.start.getLine());
  }

  @Override
  public void exitRigmp_explicit_tracking(Rigmp_explicit_trackingContext ctx) {
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST, toString(ctx.name), ROUTER_IGMP_EXPLICIT_TRACKING, ctx.start.getLine());
  }

  @Override
  public void exitRigmpi_explicit_tracking(Rigmpi_explicit_trackingContext ctx) {
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST, toString(ctx.name), ROUTER_IGMP_EXPLICIT_TRACKING, ctx.start.getLine());
  }

  @Override
  public void exitRigmpm_groups_per_interface(Rigmpm_groups_per_interfaceContext ctx) {
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST,
        toString(ctx.name),
        ROUTER_IGMP_MAXIMUM_GROUPS_PER_INTERFACE,
        ctx.start.getLine());
  }

  @Override
  public void exitRigmpi_maximum(Rigmpi_maximumContext ctx) {
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST,
        toString(ctx.name),
        ROUTER_IGMP_MAXIMUM_GROUPS_PER_INTERFACE,
        ctx.start.getLine());
  }

  @Override
  public void exitRigmps_static(Rigmps_staticContext ctx) {
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST, toString(ctx.name), ROUTER_IGMP_SSM_MAP_STATIC, ctx.start.getLine());
  }

  @Override
  public void exitRmld_access_group(Rmld_access_groupContext ctx) {
    _configuration.referenceStructure(
        IPV6_ACCESS_LIST, toString(ctx.name), ROUTER_MLD_ACCESS_GROUP, ctx.start.getLine());
  }

  @Override
  public void exitRmldi_access_group(Rmldi_access_groupContext ctx) {
    _configuration.referenceStructure(
        IPV6_ACCESS_LIST, toString(ctx.name), ROUTER_MLD_ACCESS_GROUP, ctx.start.getLine());
  }

  @Override
  public void exitRmld_explicit_tracking(Rmld_explicit_trackingContext ctx) {
    _configuration.referenceStructure(
        IPV6_ACCESS_LIST, toString(ctx.name), ROUTER_MLD_EXPLICIT_TRACKING, ctx.start.getLine());
  }

  @Override
  public void exitRmldi_explicit_tracking(Rmldi_explicit_trackingContext ctx) {
    _configuration.referenceStructure(
        IPV6_ACCESS_LIST, toString(ctx.name), ROUTER_MLD_EXPLICIT_TRACKING, ctx.start.getLine());
  }

  @Override
  public void exitRmldm_groups_per_interface(Rmldm_groups_per_interfaceContext ctx) {
    _configuration.referenceStructure(
        IPV6_ACCESS_LIST,
        toString(ctx.name),
        ROUTER_MLD_MAXIMUM_GROUPS_PER_INTERFACE,
        ctx.start.getLine());
  }

  @Override
  public void exitRmld_ssm_static(Rmld_ssm_staticContext ctx) {
    _configuration.referenceStructure(
        IPV6_ACCESS_LIST, toString(ctx.name), ROUTER_MLD_SSM_MAP_STATIC, ctx.start.getLine());
  }

  @Override
  public void exitMrvaf_core_tree_protocol(Mrvaf_core_tree_protocolContext ctx) {
    _configuration.referenceStructure(
        _multicastRoutingIpv6 ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST,
        toString(ctx.name),
        MULTICAST_ROUTING_CORE_TREE_PROTOCOL_RSVP_TE_GROUP_LIST,
        ctx.start.getLine());
  }

  @Override
  public void enterMr_ipv6(Mr_ipv6Context ctx) {
    _multicastRoutingIpv6 = true;
  }

  @Override
  public void exitMr_ipv6(Mr_ipv6Context ctx) {
    _multicastRoutingIpv6 = false;
  }

  @Override
  public void enterMrv_ipv6(Mrv_ipv6Context ctx) {
    _multicastRoutingIpv6 = true;
  }

  @Override
  public void exitMrv_ipv6(Mrv_ipv6Context ctx) {
    _multicastRoutingIpv6 = false;
  }

  @Override
  public void enterFlow_exporter_map(Flow_exporter_mapContext ctx) {
    _configuration.defineStructure(FLOW_EXPORTER_MAP, toString(ctx.name), ctx);
  }

  @Override
  public void enterFlow_monitor_map(Flow_monitor_mapContext ctx) {
    _configuration.defineStructure(FLOW_MONITOR_MAP, toString(ctx.name), ctx);
  }

  @Override
  public void enterS_sampler_map(S_sampler_mapContext ctx) {
    _configuration.defineStructure(SAMPLER_MAP, toString(ctx.name), ctx);
  }

  @Override
  public void exitFmm_exporter(Fmm_exporterContext ctx) {
    _configuration.referenceStructure(
        FLOW_EXPORTER_MAP, toString(ctx.name), FLOW_MONITOR_MAP_EXPORTER, ctx.start.getLine());
  }

  @Override
  public void exitIf_flow(If_flowContext ctx) {
    CiscoXrStructureUsage mUsage;
    CiscoXrStructureUsage sUsage;
    if (ctx.INGRESS() != null) {
      if (ctx.IPV4() != null) {
        mUsage = INTERFACE_FLOW_IPV4_MONITOR_INGRESS;
        sUsage = INTERFACE_FLOW_IPV4_SAMPLER_INGRESS;
      } else {
        assert ctx.IPV6() != null;
        mUsage = INTERFACE_FLOW_IPV6_MONITOR_INGRESS;
        sUsage = INTERFACE_FLOW_IPV6_SAMPLER_INGRESS;
      }
    } else {
      assert ctx.EGRESS() != null;
      if (ctx.IPV4() != null) {
        mUsage = INTERFACE_FLOW_IPV4_MONITOR_EGRESS;
        sUsage = INTERFACE_FLOW_IPV4_SAMPLER_EGRESS;
      } else {
        assert ctx.IPV6() != null;
        mUsage = INTERFACE_FLOW_IPV6_MONITOR_EGRESS;
        sUsage = INTERFACE_FLOW_IPV6_SAMPLER_EGRESS;
      }
    }
    int line = ctx.start.getLine();
    _configuration.referenceStructure(
        FLOW_MONITOR_MAP, toString(ctx.flow_monitor_map_name()), mUsage, line);
    _configuration.referenceStructure(SAMPLER_MAP, toString(ctx.sampler_map_name()), sUsage, line);
  }

  @Nonnull
  private static String toString(Flow_exporter_map_nameContext ctx) {
    return ctx.getText();
  }

  @Nonnull
  private static String toString(Flow_monitor_map_nameContext ctx) {
    return ctx.getText();
  }

  @Nonnull
  private static String toString(Sampler_map_nameContext ctx) {
    return ctx.getText();
  }

  private static long toLong(Ospf_areaContext ctx) {
    if (ctx.num != null) {
      return toLong(ctx.num);
    } else {
      assert ctx.ip != null;
      return toIp(ctx.ip).asLong();
    }
  }

  @Override
  public void enterS_ethernet_services(S_ethernet_servicesContext ctx) {
    String name = toString(ctx.name);
    _configuration.defineStructure(ETHERNET_SERVICES_ACCESS_LIST, name, ctx);
  }

  @Override
  public void exitIpv4_conflict_policy(Ipv4_conflict_policyContext ctx) {
    todo(ctx);
  }

  @Nonnull
  @Override
  public SilentSyntaxCollection getSilentSyntax() {
    return _silentSyntax;
  }

  @Override
  public void exitEveryRule(ParserRuleContext ctx) {
    tryProcessSilentSyntax(ctx);
  }
}

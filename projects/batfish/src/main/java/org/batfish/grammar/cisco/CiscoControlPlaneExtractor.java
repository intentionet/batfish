package org.batfish.grammar.cisco;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toCollection;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_ASA;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.batfish.representation.cisco.CiscoConfiguration.DEFAULT_STATIC_ROUTE_DISTANCE;
import static org.batfish.representation.cisco.CiscoConfiguration.computeRouteMapClauseName;
import static org.batfish.representation.cisco.CiscoStructureType.ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.AS_PATH_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.BFD_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_AF_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_NEIGHBOR_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_PEER_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_SESSION_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_TEMPLATE_PEER_POLICY;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_TEMPLATE_PEER_SESSION;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_UNDECLARED_PEER;
import static org.batfish.representation.cisco.CiscoStructureType.BGP_UNDECLARED_PEER_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.CLASS_MAP;
import static org.batfish.representation.cisco.CiscoStructureType.COMMUNITY_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.COMMUNITY_LIST_EXPANDED;
import static org.batfish.representation.cisco.CiscoStructureType.COMMUNITY_LIST_STANDARD;
import static org.batfish.representation.cisco.CiscoStructureType.CRYPTO_DYNAMIC_MAP_SET;
import static org.batfish.representation.cisco.CiscoStructureType.CRYPTO_MAP_SET;
import static org.batfish.representation.cisco.CiscoStructureType.DEPI_CLASS;
import static org.batfish.representation.cisco.CiscoStructureType.DEPI_TUNNEL;
import static org.batfish.representation.cisco.CiscoStructureType.DOCSIS_POLICY;
import static org.batfish.representation.cisco.CiscoStructureType.DOCSIS_POLICY_RULE;
import static org.batfish.representation.cisco.CiscoStructureType.EXTCOMMUNITY_LIST;
import static org.batfish.representation.cisco.CiscoStructureType.EXTCOMMUNITY_LIST_EXPANDED;
import static org.batfish.representation.cisco.CiscoStructureType.EXTCOMMUNITY_LIST_STANDARD;
import static org.batfish.representation.cisco.CiscoStructureType.ICMP_TYPE_OBJECT_GROUP;
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
import static org.batfish.representation.cisco.CiscoStructureType.ROUTE_MAP_CLAUSE;
import static org.batfish.representation.cisco.CiscoStructureType.SECURITY_ZONE;
import static org.batfish.representation.cisco.CiscoStructureType.SECURITY_ZONE_PAIR;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_CLASS;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureType.SERVICE_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureType.TRACK;
import static org.batfish.representation.cisco.CiscoStructureType.TRAFFIC_ZONE;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ADVERTISE_MAP_EXIST_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_AGGREGATE_ADVERTISE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_AGGREGATE_ATTRIBUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_AGGREGATE_SUPPRESS_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_DEFAULT_ORIGINATE_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_PREFIX6_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_ROUTE6_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INBOUND_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INHERITED_PEER_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_INHERITED_SESSION;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_LISTEN_RANGE_PEER_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_FILTER_AS_PATH_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_PEER_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_STATEMENT;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NEIGHBOR_WITHOUT_REMOTE_AS;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NETWORK6_ORIGINATION_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_NETWORK_ORIGINATION_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_PREFIX6_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_ROUTE6_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_OUTBOUND_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_PEER_GROUP_REFERENCED_BEFORE_DEFINED;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_CONNECTED_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_EIGRP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_OSPFV3_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_OSPF_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_RIP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_REDISTRIBUTE_STATIC_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ROUTE_MAP_ADVERTISE;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_ROUTE_MAP_UNSUPPRESS;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_UPDATE_SOURCE_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_USE_AF_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_USE_NEIGHBOR_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.BGP_USE_SESSION_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.CLASS_MAP_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.CLASS_MAP_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.CLASS_MAP_ACTIVATED_SERVICE_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureUsage.CLASS_MAP_SERVICE_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureUsage.CONTROLLER_DEPI_TUNNEL;
import static org.batfish.representation.cisco.CiscoStructureUsage.CONTROL_PLANE_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.CONTROL_PLANE_SERVICE_POLICY_INPUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.CONTROL_PLANE_SERVICE_POLICY_OUTPUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.COPS_LISTENER_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_DYNAMIC_MAP_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_DYNAMIC_MAP_ISAKMP_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_DYNAMIC_MAP_TRANSFORM_SET;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_CRYPTO_DYNAMIC_MAP_SET;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_ISAKMP_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_MAP_IPSEC_ISAKMP_TRANSFORM_SET;
import static org.batfish.representation.cisco.CiscoStructureUsage.CRYPTO_MAP_MATCH_ADDRESS;
import static org.batfish.representation.cisco.CiscoStructureUsage.DEPI_TUNNEL_DEPI_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.DEPI_TUNNEL_L2TP_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.DEPI_TUNNEL_PROTECT_TUNNEL;
import static org.batfish.representation.cisco.CiscoStructureUsage.DOCSIS_GROUP_DOCSIS_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.DOCSIS_POLICY_DOCSIS_POLICY_RULE;
import static org.batfish.representation.cisco.CiscoStructureUsage.DOMAIN_LOOKUP_SOURCE_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_AF_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_GATEWAY_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_GATEWAY_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ROUTE_MAP_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_DISTRIBUTE_LIST_ROUTE_MAP_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_PASSIVE_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_BGP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_CONNECTED_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_EIGRP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_ISIS_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_OSPF_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_RIP_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_REDISTRIBUTE_STATIC_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EIGRP_STUB_LEAK_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_PROTOCOL_OR_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.ICMP_TYPE_OBJECT_GROUP_GROUP_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.INSPECT_CLASS_MAP_MATCH_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INSPECT_POLICY_MAP_INSPECT_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_BFD_TEMPLATE;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IGMP_ACCESS_GROUP_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IGMP_HOST_PROXY_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IGMP_STATIC_GROUP_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_INCOMING_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IPV6_TRAFFIC_FILTER_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IP_INBAND_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IP_VERIFY_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_IP_VRF_SITEMAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_OUTGOING_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_PIM_NEIGHBOR_FILTER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_POLICY_ROUTING_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_SELF_REF;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_SERVICE_INSTANCE_SERVICE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_SERVICE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_STANDBY_TRACK;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_SUMMARY_ADDRESS_EIGRP_LEAK_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_TRAFFIC_ZONE_MEMBER;
import static org.batfish.representation.cisco.CiscoStructureUsage.INTERFACE_ZONE_MEMBER;
import static org.batfish.representation.cisco.CiscoStructureUsage.IPSEC_PROFILE_ISAKMP_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureUsage.IPSEC_PROFILE_TRANSFORM_SET;
import static org.batfish.representation.cisco.CiscoStructureUsage.IPV6_LOCAL_POLICY_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_DOMAIN_LOOKUP_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_LOCAL_POLICY_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_DESTINATION_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_DESTINATION_POOL;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_INSIDE_SOURCE;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_INSIDE_SOURCE_STATIC;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_OUTSIDE_SOURCE;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_SOURCE_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_NAT_SOURCE_POOL;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_ROUTE_NHINT;
import static org.batfish.representation.cisco.CiscoStructureUsage.IP_TACACS_SOURCE_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISAKMP_POLICY_SELF_REF;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISAKMP_PROFILE_KEYRING;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISAKMP_PROFILE_SELF_REF;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISIS_REDISTRIBUTE_CONNECTED_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.ISIS_REDISTRIBUTE_STATIC_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.LINE_ACCESS_CLASS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.LINE_ACCESS_CLASS_LIST6;
import static org.batfish.representation.cisco.CiscoStructureUsage.MANAGEMENT_SSH_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.MANAGEMENT_TELNET_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.MSDP_PEER_SA_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.NAMED_RSA_PUB_KEY_SELF_REF;
import static org.batfish.representation.cisco.CiscoStructureUsage.NETWORK_OBJECT_GROUP_GROUP_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.NETWORK_OBJECT_GROUP_NETWORK_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.NTP_ACCESS_GROUP;
import static org.batfish.representation.cisco.CiscoStructureUsage.NTP_SOURCE_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_AREA_FILTER_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_DEFAULT_ORIGINATE_ROUTE_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ACCESS_LIST_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_PREFIX_LIST_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_PREFIX_LIST_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ROUTE_MAP_IN;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_DISTRIBUTE_LIST_ROUTE_MAP_OUT;
import static org.batfish.representation.cisco.CiscoStructureUsage.OSPF_PREFIX_PRIORITY_MAP;
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
import static org.batfish.representation.cisco.CiscoStructureUsage.PROTOCOL_OBJECT_GROUP_GROUP_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.QOS_ENFORCE_RULE_SERVICE_CLASS;
import static org.batfish.representation.cisco.CiscoStructureUsage.RIP_DISTRIBUTE_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTER_ISIS_DISTRIBUTE_LIST_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTER_STATIC_ROUTE;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTER_VRRP_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_CLAUSE_PREV_REF;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_CONTINUE;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_DELETE_COMMUNITY;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_AS_PATH_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_COMMUNITY_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_EXTCOMMUNITY;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV4_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV4_PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_ACCESS_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ROUTE_MAP_MATCH_IPV6_PREFIX_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.SERVICE_OBJECT_GROUP_SERVICE_OBJECT;
import static org.batfish.representation.cisco.CiscoStructureUsage.SERVICE_POLICY_GLOBAL;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL4;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_COMMUNITY_ACL6;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_FILE_TRANSFER_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_GROUP_V3_ACCESS;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_GROUP_V3_ACCESS_IPV6;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_SOURCE_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_TFTP_SERVER_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.SNMP_SERVER_TRAP_SOURCE;
import static org.batfish.representation.cisco.CiscoStructureUsage.SSH_IPV4_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.SSH_IPV6_ACL;
import static org.batfish.representation.cisco.CiscoStructureUsage.STATIC_ROUTE_TRACK;
import static org.batfish.representation.cisco.CiscoStructureUsage.SYSTEM_SERVICE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.TACACS_SOURCE_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.TRACK_INTERFACE;
import static org.batfish.representation.cisco.CiscoStructureUsage.TRACK_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.TUNNEL_PROTECTION_IPSEC_PROFILE;
import static org.batfish.representation.cisco.CiscoStructureUsage.TUNNEL_SOURCE;
import static org.batfish.representation.cisco.CiscoStructureUsage.VRF_DEFINITION_ADDRESS_FAMILY_EXPORT_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.VRF_DEFINITION_ADDRESS_FAMILY_IMPORT_MAP;
import static org.batfish.representation.cisco.CiscoStructureUsage.WCCP_GROUP_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.WCCP_REDIRECT_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.WCCP_SERVICE_LIST;
import static org.batfish.representation.cisco.CiscoStructureUsage.ZONE_PAIR_DESTINATION_ZONE;
import static org.batfish.representation.cisco.CiscoStructureUsage.ZONE_PAIR_INSPECT_SERVICE_POLICY;
import static org.batfish.representation.cisco.CiscoStructureUsage.ZONE_PAIR_SOURCE_ZONE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.LongExpr;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.OriginExpr;
import org.batfish.datamodel.tracking.DecrementPriority;
import org.batfish.datamodel.tracking.TrackAction;
import org.batfish.datamodel.tracking.TrackInterface;
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
import org.batfish.grammar.BatfishCombinedParser;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.ControlPlaneExtractor;
import org.batfish.grammar.SilentSyntaxListener;
import org.batfish.grammar.UnrecognizedLineToken;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accountingContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accounting_commands_lineContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accounting_defaultContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accounting_default_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_accounting_default_localContext;
import org.batfish.grammar.cisco.CiscoParser.Aaa_authenticationContext;
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
import org.batfish.grammar.cisco.CiscoParser.Address_family_headerContext;
import org.batfish.grammar.cisco.CiscoParser.Address_family_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Advertise_map_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Af_group_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Agg_advertise_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Agg_as_setContext;
import org.batfish.grammar.cisco.CiscoParser.Agg_attribute_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Agg_summary_onlyContext;
import org.batfish.grammar.cisco.CiscoParser.Agg_suppress_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Aggregate_address_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Allowas_in_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Always_compare_med_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.As_exprContext;
import org.batfish.grammar.cisco.CiscoParser.As_path_multipath_relax_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Auto_summary_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_asnContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_conf_identifier_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_conf_peers_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_enforce_first_as_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_listen_range_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Bgp_redistribute_internal_rb_stanzaContext;
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
import org.batfish.grammar.cisco.CiscoParser.Cis_keyContext;
import org.batfish.grammar.cisco.CiscoParser.Cis_policyContext;
import org.batfish.grammar.cisco.CiscoParser.Cis_profileContext;
import org.batfish.grammar.cisco.CiscoParser.Cisco_configurationContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_authenticationContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_encryptionContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_hashContext;
import org.batfish.grammar.cisco.CiscoParser.Cispol_lifetimeContext;
import org.batfish.grammar.cisco.CiscoParser.Cisprf_keyringContext;
import org.batfish.grammar.cisco.CiscoParser.Cisprf_local_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Cisprf_matchContext;
import org.batfish.grammar.cisco.CiscoParser.Cisprf_self_identityContext;
import org.batfish.grammar.cisco.CiscoParser.Ckp_named_keyContext;
import org.batfish.grammar.cisco.CiscoParser.Ckpn_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Ckpn_key_stringContext;
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
import org.batfish.grammar.cisco.CiscoParser.Compare_routerid_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Continue_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Copsl_access_listContext;
import org.batfish.grammar.cisco.CiscoParser.Cp_ip_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Cp_service_policyContext;
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
import org.batfish.grammar.cisco.CiscoParser.Crypto_map_t_match_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Cs_classContext;
import org.batfish.grammar.cisco.CiscoParser.Csc_nameContext;
import org.batfish.grammar.cisco.CiscoParser.DecContext;
import org.batfish.grammar.cisco.CiscoParser.Default_information_originate_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Default_metric_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Default_originate_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Default_shutdown_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Description_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Description_lineContext;
import org.batfish.grammar.cisco.CiscoParser.Dh_groupContext;
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
import org.batfish.grammar.cisco.CiscoParser.Ec_ga_la_literalContext;
import org.batfish.grammar.cisco.CiscoParser.Ecgalal_asdot_colonContext;
import org.batfish.grammar.cisco.CiscoParser.Ecgalal_colonContext;
import org.batfish.grammar.cisco.CiscoParser.Ecgalal_ip_colonContext;
import org.batfish.grammar.cisco.CiscoParser.Eigrp_metricContext;
import org.batfish.grammar.cisco.CiscoParser.Enable_secretContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_access_list_additional_featureContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_access_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_access_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_community_route_targetContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_ipv6_access_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Extended_ipv6_access_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Filter_list_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Icmp_inline_object_typeContext;
import org.batfish.grammar.cisco.CiscoParser.Icmp_object_typeContext;
import org.batfish.grammar.cisco.CiscoParser.If_autostateContext;
import org.batfish.grammar.cisco.CiscoParser.If_bandwidthContext;
import org.batfish.grammar.cisco.CiscoParser.If_bfd_templateContext;
import org.batfish.grammar.cisco.CiscoParser.If_channel_groupContext;
import org.batfish.grammar.cisco.CiscoParser.If_crypto_mapContext;
import org.batfish.grammar.cisco.CiscoParser.If_delayContext;
import org.batfish.grammar.cisco.CiscoParser.If_descriptionContext;
import org.batfish.grammar.cisco.CiscoParser.If_encapsulation_dot1qContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_addressContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_address_secondaryContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_forwardContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_helper_addressContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_igmpContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_inband_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_nat_insideContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_nat_outsideContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_areaContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_costContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_dead_intervalContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_dead_interval_minimalContext;
import org.batfish.grammar.cisco.CiscoParser.If_ip_ospf_hello_intervalContext;
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
import org.batfish.grammar.cisco.CiscoParser.If_ipv6_traffic_filterContext;
import org.batfish.grammar.cisco.CiscoParser.If_isis_metricContext;
import org.batfish.grammar.cisco.CiscoParser.If_member_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.If_mtuContext;
import org.batfish.grammar.cisco.CiscoParser.If_service_policyContext;
import org.batfish.grammar.cisco.CiscoParser.If_shutdownContext;
import org.batfish.grammar.cisco.CiscoParser.If_si_service_policyContext;
import org.batfish.grammar.cisco.CiscoParser.If_spanning_treeContext;
import org.batfish.grammar.cisco.CiscoParser.If_speed_iosContext;
import org.batfish.grammar.cisco.CiscoParser.If_st_portfastContext;
import org.batfish.grammar.cisco.CiscoParser.If_standbyContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchportContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_accessContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_modeContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_trunk_allowedContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_trunk_encapsulationContext;
import org.batfish.grammar.cisco.CiscoParser.If_switchport_trunk_nativeContext;
import org.batfish.grammar.cisco.CiscoParser.If_vlanContext;
import org.batfish.grammar.cisco.CiscoParser.If_vrfContext;
import org.batfish.grammar.cisco.CiscoParser.If_vrf_memberContext;
import org.batfish.grammar.cisco.CiscoParser.If_vrrpContext;
import org.batfish.grammar.cisco.CiscoParser.If_zone_memberContext;
import org.batfish.grammar.cisco.CiscoParser.Ifdhcpr_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Ifdhcpr_clientContext;
import org.batfish.grammar.cisco.CiscoParser.Ifigmp_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Ifigmphp_access_listContext;
import org.batfish.grammar.cisco.CiscoParser.Ifigmpsg_aclContext;
import org.batfish.grammar.cisco.CiscoParser.Iftunnel_destinationContext;
import org.batfish.grammar.cisco.CiscoParser.Iftunnel_modeContext;
import org.batfish.grammar.cisco.CiscoParser.Iftunnel_protectionContext;
import org.batfish.grammar.cisco.CiscoParser.Iftunnel_sourceContext;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_authenticationContext;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_ipContext;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_ipv4Context;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_preemptContext;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_priorityContext;
import org.batfish.grammar.cisco.CiscoParser.Ifvrrp_priority_levelContext;
import org.batfish.grammar.cisco.CiscoParser.Ike_encryptionContext;
import org.batfish.grammar.cisco.CiscoParser.Inherit_peer_policy_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Inherit_peer_session_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Inspect_protocolContext;
import org.batfish.grammar.cisco.CiscoParser.Int_exprContext;
import org.batfish.grammar.cisco.CiscoParser.Interface_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Interface_nameContext;
import org.batfish.grammar.cisco.CiscoParser.Ios_banner_headerContext;
import org.batfish.grammar.cisco.CiscoParser.Ios_delimited_bannerContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_as_path_access_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_as_path_access_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_community_list_expanded_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_community_list_expanded_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_community_list_standard_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_community_list_standard_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_dhcp_relay_serverContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_domain_lookupContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_domain_nameContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_extcommunity_list_expandedContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_extcommunity_list_standardContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_hostnameContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_prefix_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_prefix_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_route_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_route_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ip_ssh_versionContext;
import org.batfish.grammar.cisco.CiscoParser.Ipl_policyContext;
import org.batfish.grammar.cisco.CiscoParser.Ipn_insideContext;
import org.batfish.grammar.cisco.CiscoParser.Ipn_outsideContext;
import org.batfish.grammar.cisco.CiscoParser.Ipn_poolContext;
import org.batfish.grammar.cisco.CiscoParser.Ipn_pool_prefixContext;
import org.batfish.grammar.cisco.CiscoParser.Ipn_pool_rangeContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnc_listContext;
import org.batfish.grammar.cisco.CiscoParser.Ipni_destinationContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnios_static_addrContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnios_static_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnios_vrfContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnioss_local_globalContext;
import org.batfish.grammar.cisco.CiscoParser.Ipniossm_extendableContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnis_listContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnis_route_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnis_staticContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnos_listContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnos_route_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnos_staticContext;
import org.batfish.grammar.cisco.CiscoParser.Ipnosm_add_routeContext;
import org.batfish.grammar.cisco.CiscoParser.Ipsec_authenticationContext;
import org.batfish.grammar.cisco.CiscoParser.Ipsec_encryptionContext;
import org.batfish.grammar.cisco.CiscoParser.Ipv6_prefix_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ipv6_prefix_list_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ipv6l_policyContext;
import org.batfish.grammar.cisco.CiscoParser.Is_type_is_stanzaContext;
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
import org.batfish.grammar.cisco.CiscoParser.Match_extcommunity_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_interface_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_ip_access_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_ip_prefix_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_ipv6_access_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_ipv6_prefix_list_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_semanticsContext;
import org.batfish.grammar.cisco.CiscoParser.Match_source_protocol_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Match_tag_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Maximum_paths_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Maximum_peers_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Neighbor_flat_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Neighbor_group_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Net_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Network6_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Network_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Next_hop_self_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.No_bgp_enforce_first_as_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_ip_prefix_list_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_neighbor_activate_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_neighbor_shutdown_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_redistribute_connected_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_route_map_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.No_shutdown_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Ntp_access_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Ntp_serverContext;
import org.batfish.grammar.cisco.CiscoParser.Ntp_source_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.O_networkContext;
import org.batfish.grammar.cisco.CiscoParser.O_serviceContext;
import org.batfish.grammar.cisco.CiscoParser.Og_icmp_typeContext;
import org.batfish.grammar.cisco.CiscoParser.Og_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Og_protocolContext;
import org.batfish.grammar.cisco.CiscoParser.Og_serviceContext;
import org.batfish.grammar.cisco.CiscoParser.Ogg_icmp_typeContext;
import org.batfish.grammar.cisco.CiscoParser.Ogg_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Ogg_protocolContext;
import org.batfish.grammar.cisco.CiscoParser.Ogg_serviceContext;
import org.batfish.grammar.cisco.CiscoParser.Oggit_group_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Oggn_group_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Oggp_group_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Oggs_group_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogit_group_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogit_icmp_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogn_group_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogn_host_ipContext;
import org.batfish.grammar.cisco.CiscoParser.Ogn_ip_with_maskContext;
import org.batfish.grammar.cisco.CiscoParser.Ogn_network_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogp_group_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogp_protocol_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_group_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_icmpContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_port_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_service_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_tcpContext;
import org.batfish.grammar.cisco.CiscoParser.Ogs_udpContext;
import org.batfish.grammar.cisco.CiscoParser.On_descriptionContext;
import org.batfish.grammar.cisco.CiscoParser.On_fqdnContext;
import org.batfish.grammar.cisco.CiscoParser.On_hostContext;
import org.batfish.grammar.cisco.CiscoParser.On_rangeContext;
import org.batfish.grammar.cisco.CiscoParser.On_subnetContext;
import org.batfish.grammar.cisco.CiscoParser.Origin_expr_literalContext;
import org.batfish.grammar.cisco.CiscoParser.Os_descriptionContext;
import org.batfish.grammar.cisco.CiscoParser.Ospf_areaContext;
import org.batfish.grammar.cisco.CiscoParser.Ospf_route_typeContext;
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
import org.batfish.grammar.cisco.CiscoParser.ProtocolContext;
import org.batfish.grammar.cisco.CiscoParser.RangeContext;
import org.batfish.grammar.cisco.CiscoParser.Re_autonomous_systemContext;
import org.batfish.grammar.cisco.CiscoParser.Re_classicContext;
import org.batfish.grammar.cisco.CiscoParser.Re_default_metricContext;
import org.batfish.grammar.cisco.CiscoParser.Re_eigrp_router_idContext;
import org.batfish.grammar.cisco.CiscoParser.Re_eigrp_stubContext;
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
import org.batfish.grammar.cisco.CiscoParser.Re_shutdownContext;
import org.batfish.grammar.cisco.CiscoParser.Reaf_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Reaf_interface_defaultContext;
import org.batfish.grammar.cisco.CiscoParser.Reafi_passive_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Rec_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Rec_metric_weightsContext;
import org.batfish.grammar.cisco.CiscoParser.Recno_eigrp_router_idContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_aggregate_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_connected_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_connected_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_eigrp_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_ospf_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_ospfv3_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_rip_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_static_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Redistribute_static_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Redl_aclContext;
import org.batfish.grammar.cisco.CiscoParser.Redl_gatewayContext;
import org.batfish.grammar.cisco.CiscoParser.Redl_prefixContext;
import org.batfish.grammar.cisco.CiscoParser.Redl_route_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Rees_leak_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Remote_as_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Remove_private_as_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Ren_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Ren_metric_weightsContext;
import org.batfish.grammar.cisco.CiscoParser.Reno_shutdownContext;
import org.batfish.grammar.cisco.CiscoParser.Ro6_distribute_listContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_areaContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_auto_costContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_default_informationContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_default_metricContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_distance_distanceContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_distance_ospfContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_distribute_listContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_max_metricContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_maximum_pathsContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_passive_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_passive_interface_defaultContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_prefix_priorityContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_bgp_ciscoContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_connectedContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_eigrpContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_ripContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_redistribute_staticContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_rfc1583_compatibilityContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_router_idContext;
import org.batfish.grammar.cisco.CiscoParser.Ro_vrfContext;
import org.batfish.grammar.cisco.CiscoParser.Roa_default_costContext;
import org.batfish.grammar.cisco.CiscoParser.Roa_filterlistContext;
import org.batfish.grammar.cisco.CiscoParser.Roa_nssaContext;
import org.batfish.grammar.cisco.CiscoParser.Roa_rangeContext;
import org.batfish.grammar.cisco.CiscoParser.Roa_stubContext;
import org.batfish.grammar.cisco.CiscoParser.Roi_costContext;
import org.batfish.grammar.cisco.CiscoParser.Roi_passiveContext;
import org.batfish.grammar.cisco.CiscoParser.Route_distinguisherContext;
import org.batfish.grammar.cisco.CiscoParser.Route_map_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Route_map_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Route_reflector_client_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Route_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Route_targetContext;
import org.batfish.grammar.cisco.CiscoParser.Router_bgp_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Router_id_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Router_isis_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Rr_distribute_listContext;
import org.batfish.grammar.cisco.CiscoParser.Rr_networkContext;
import org.batfish.grammar.cisco.CiscoParser.Rr_passive_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Rr_passive_interface_defaultContext;
import org.batfish.grammar.cisco.CiscoParser.Rs_routeContext;
import org.batfish.grammar.cisco.CiscoParser.Rs_vrfContext;
import org.batfish.grammar.cisco.CiscoParser.S_aaaContext;
import org.batfish.grammar.cisco.CiscoParser.S_access_lineContext;
import org.batfish.grammar.cisco.CiscoParser.S_banner_iosContext;
import org.batfish.grammar.cisco.CiscoParser.S_bfd_templateContext;
import org.batfish.grammar.cisco.CiscoParser.S_cableContext;
import org.batfish.grammar.cisco.CiscoParser.S_class_mapContext;
import org.batfish.grammar.cisco.CiscoParser.S_depi_classContext;
import org.batfish.grammar.cisco.CiscoParser.S_depi_tunnelContext;
import org.batfish.grammar.cisco.CiscoParser.S_domain_nameContext;
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
import org.batfish.grammar.cisco.CiscoParser.S_service_templateContext;
import org.batfish.grammar.cisco.CiscoParser.S_snmp_serverContext;
import org.batfish.grammar.cisco.CiscoParser.S_sntpContext;
import org.batfish.grammar.cisco.CiscoParser.S_spanning_treeContext;
import org.batfish.grammar.cisco.CiscoParser.S_switchportContext;
import org.batfish.grammar.cisco.CiscoParser.S_system_service_policyContext;
import org.batfish.grammar.cisco.CiscoParser.S_tacacs_serverContext;
import org.batfish.grammar.cisco.CiscoParser.S_trackContext;
import org.batfish.grammar.cisco.CiscoParser.S_usernameContext;
import org.batfish.grammar.cisco.CiscoParser.S_vrf_definitionContext;
import org.batfish.grammar.cisco.CiscoParser.S_zoneContext;
import org.batfish.grammar.cisco.CiscoParser.S_zone_pairContext;
import org.batfish.grammar.cisco.CiscoParser.Sd_switchport_blankContext;
import org.batfish.grammar.cisco.CiscoParser.Sd_switchport_shutdownContext;
import org.batfish.grammar.cisco.CiscoParser.Send_community_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Service_group_protocolContext;
import org.batfish.grammar.cisco.CiscoParser.Service_specifier_icmpContext;
import org.batfish.grammar.cisco.CiscoParser.Service_specifier_protocolContext;
import org.batfish.grammar.cisco.CiscoParser.Service_specifier_tcp_udpContext;
import org.batfish.grammar.cisco.CiscoParser.Session_group_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_as_path_prepend_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_comm_list_delete_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_community_additive_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_community_none_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_community_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_extcommunity_rm_stanza_costContext;
import org.batfish.grammar.cisco.CiscoParser.Set_extcommunity_rm_stanza_rtContext;
import org.batfish.grammar.cisco.CiscoParser.Set_extcommunity_rm_stanza_sooContext;
import org.batfish.grammar.cisco.CiscoParser.Set_extcommunity_rm_stanza_vpn_distinguisherContext;
import org.batfish.grammar.cisco.CiscoParser.Set_local_preference_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_metric_eigrp_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_metric_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_metric_type_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_next_hop_peer_address_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_next_hop_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_origin_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_tag_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Set_weight_rm_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Shutdown_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Sntp_serverContext;
import org.batfish.grammar.cisco.CiscoParser.Spanning_tree_portfastContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_communityContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_enable_trapsContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_file_transferContext;
import org.batfish.grammar.cisco.CiscoParser.Ss_group_v3Context;
import org.batfish.grammar.cisco.CiscoParser.Ss_host_genericContext;
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
import org.batfish.grammar.cisco.CiscoParser.Standby_groupContext;
import org.batfish.grammar.cisco.CiscoParser.Standby_group_authenticationContext;
import org.batfish.grammar.cisco.CiscoParser.Standby_group_ipContext;
import org.batfish.grammar.cisco.CiscoParser.Standby_group_preemptContext;
import org.batfish.grammar.cisco.CiscoParser.Standby_group_priorityContext;
import org.batfish.grammar.cisco.CiscoParser.Standby_group_timersContext;
import org.batfish.grammar.cisco.CiscoParser.Standby_group_trackContext;
import org.batfish.grammar.cisco.CiscoParser.Standby_versionContext;
import org.batfish.grammar.cisco.CiscoParser.SubrangeContext;
import org.batfish.grammar.cisco.CiscoParser.Summary_address_is_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Suppressed_iis_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Switching_mode_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Switchport_trunk_encapsulationContext;
import org.batfish.grammar.cisco.CiscoParser.T_serverContext;
import org.batfish.grammar.cisco.CiscoParser.T_source_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Template_peer_policy_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Template_peer_session_rb_stanzaContext;
import org.batfish.grammar.cisco.CiscoParser.Tl_objectContext;
import org.batfish.grammar.cisco.CiscoParser.Track_actionContext;
import org.batfish.grammar.cisco.CiscoParser.Track_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Ts_hostContext;
import org.batfish.grammar.cisco.CiscoParser.U_passwordContext;
import org.batfish.grammar.cisco.CiscoParser.U_roleContext;
import org.batfish.grammar.cisco.CiscoParser.Uint16Context;
import org.batfish.grammar.cisco.CiscoParser.Uint32Context;
import org.batfish.grammar.cisco.CiscoParser.Uint8Context;
import org.batfish.grammar.cisco.CiscoParser.Unsuppress_map_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Update_source_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Use_af_group_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Use_neighbor_group_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.Use_session_group_bgp_tailContext;
import org.batfish.grammar.cisco.CiscoParser.VariableContext;
import org.batfish.grammar.cisco.CiscoParser.Variable_access_listContext;
import org.batfish.grammar.cisco.CiscoParser.Variable_group_idContext;
import org.batfish.grammar.cisco.CiscoParser.Variable_permissiveContext;
import org.batfish.grammar.cisco.CiscoParser.Viaf_vrrpContext;
import org.batfish.grammar.cisco.CiscoParser.Viafv_addressContext;
import org.batfish.grammar.cisco.CiscoParser.Viafv_preemptContext;
import org.batfish.grammar.cisco.CiscoParser.Viafv_priorityContext;
import org.batfish.grammar.cisco.CiscoParser.Vlan_idContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfd_address_familyContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfd_af_export_nonvpnContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfd_af_export_vpnContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfd_af_import_mapContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfd_af_route_targetContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfd_descriptionContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfd_rdContext;
import org.batfish.grammar.cisco.CiscoParser.Vrfd_route_targetContext;
import org.batfish.grammar.cisco.CiscoParser.Vrrp_interfaceContext;
import org.batfish.grammar.cisco.CiscoParser.Wccp_idContext;
import org.batfish.grammar.cisco.CiscoParser.Zp_service_policy_inspectContext;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.representation.cisco.AccessListAddressSpecifier;
import org.batfish.representation.cisco.AccessListServiceSpecifier;
import org.batfish.representation.cisco.BgpAggregateIpv4Network;
import org.batfish.representation.cisco.BgpAggregateIpv6Network;
import org.batfish.representation.cisco.BgpAggregateNetwork;
import org.batfish.representation.cisco.BgpNetwork;
import org.batfish.representation.cisco.BgpNetwork6;
import org.batfish.representation.cisco.BgpPeerGroup;
import org.batfish.representation.cisco.BgpProcess;
import org.batfish.representation.cisco.BgpRedistributionPolicy;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.batfish.representation.cisco.CiscoIosDynamicNat;
import org.batfish.representation.cisco.CiscoIosNat;
import org.batfish.representation.cisco.CiscoIosNat.Direction;
import org.batfish.representation.cisco.CiscoIosNat.RuleAction;
import org.batfish.representation.cisco.CiscoIosStaticNat;
import org.batfish.representation.cisco.CiscoStructureType;
import org.batfish.representation.cisco.CiscoStructureUsage;
import org.batfish.representation.cisco.CryptoMapEntry;
import org.batfish.representation.cisco.CryptoMapSet;
import org.batfish.representation.cisco.DistributeList;
import org.batfish.representation.cisco.DistributeList.DistributeListFilterType;
import org.batfish.representation.cisco.DynamicIpBgpPeerGroup;
import org.batfish.representation.cisco.DynamicIpv6BgpPeerGroup;
import org.batfish.representation.cisco.EigrpProcess;
import org.batfish.representation.cisco.EigrpRedistributionPolicy;
import org.batfish.representation.cisco.ExpandedCommunityList;
import org.batfish.representation.cisco.ExpandedCommunityListLine;
import org.batfish.representation.cisco.ExtendedAccessList;
import org.batfish.representation.cisco.ExtendedAccessListLine;
import org.batfish.representation.cisco.ExtendedIpv6AccessList;
import org.batfish.representation.cisco.ExtendedIpv6AccessListLine;
import org.batfish.representation.cisco.FqdnNetworkObject;
import org.batfish.representation.cisco.HostNetworkObject;
import org.batfish.representation.cisco.HsrpGroup;
import org.batfish.representation.cisco.IcmpServiceObjectGroupLine;
import org.batfish.representation.cisco.IcmpTypeGroupReferenceLine;
import org.batfish.representation.cisco.IcmpTypeGroupTypeLine;
import org.batfish.representation.cisco.IcmpTypeObjectGroup;
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
import org.batfish.representation.cisco.IsakmpKey;
import org.batfish.representation.cisco.IsakmpPolicy;
import org.batfish.representation.cisco.IsakmpProfile;
import org.batfish.representation.cisco.IsisProcess;
import org.batfish.representation.cisco.IsisRedistributionPolicy;
import org.batfish.representation.cisco.Keyring;
import org.batfish.representation.cisco.MacAccessList;
import org.batfish.representation.cisco.MasterBgpPeerGroup;
import org.batfish.representation.cisco.MatchSemantics;
import org.batfish.representation.cisco.NamedBgpPeerGroup;
import org.batfish.representation.cisco.NamedRsaPubKey;
import org.batfish.representation.cisco.NatPool;
import org.batfish.representation.cisco.NetworkObjectAddressSpecifier;
import org.batfish.representation.cisco.NetworkObjectGroup;
import org.batfish.representation.cisco.NetworkObjectGroupAddressSpecifier;
import org.batfish.representation.cisco.NetworkObjectInfo;
import org.batfish.representation.cisco.NssaSettings;
import org.batfish.representation.cisco.OspfNetworkType;
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
import org.batfish.representation.cisco.ProtocolObjectGroupReferenceLine;
import org.batfish.representation.cisco.ProtocolOrServiceObjectGroupServiceSpecifier;
import org.batfish.representation.cisco.RangeNetworkObject;
import org.batfish.representation.cisco.RipProcess;
import org.batfish.representation.cisco.RouteMap;
import org.batfish.representation.cisco.RouteMapClause;
import org.batfish.representation.cisco.RouteMapContinue;
import org.batfish.representation.cisco.RouteMapMatchAsPathAccessListLine;
import org.batfish.representation.cisco.RouteMapMatchCommunityListLine;
import org.batfish.representation.cisco.RouteMapMatchExtcommunityLine;
import org.batfish.representation.cisco.RouteMapMatchInterfaceLine;
import org.batfish.representation.cisco.RouteMapMatchIpAccessListLine;
import org.batfish.representation.cisco.RouteMapMatchIpPrefixListLine;
import org.batfish.representation.cisco.RouteMapMatchIpv6AccessListLine;
import org.batfish.representation.cisco.RouteMapMatchIpv6PrefixListLine;
import org.batfish.representation.cisco.RouteMapMatchSourceProtocolLine;
import org.batfish.representation.cisco.RouteMapMatchTagLine;
import org.batfish.representation.cisco.RouteMapSetAdditiveCommunityLine;
import org.batfish.representation.cisco.RouteMapSetAsPathPrependLine;
import org.batfish.representation.cisco.RouteMapSetCommunityLine;
import org.batfish.representation.cisco.RouteMapSetCommunityNoneLine;
import org.batfish.representation.cisco.RouteMapSetDeleteCommunityLine;
import org.batfish.representation.cisco.RouteMapSetExtcommunityRtAdditiveLine;
import org.batfish.representation.cisco.RouteMapSetExtcommunityRtLine;
import org.batfish.representation.cisco.RouteMapSetLine;
import org.batfish.representation.cisco.RouteMapSetLocalPreferenceLine;
import org.batfish.representation.cisco.RouteMapSetMetricEigrpLine;
import org.batfish.representation.cisco.RouteMapSetMetricLine;
import org.batfish.representation.cisco.RouteMapSetNextHopLine;
import org.batfish.representation.cisco.RouteMapSetNextHopPeerAddress;
import org.batfish.representation.cisco.RouteMapSetOriginTypeLine;
import org.batfish.representation.cisco.RouteMapSetTagLine;
import org.batfish.representation.cisco.RouteMapSetWeightLine;
import org.batfish.representation.cisco.SecurityZone;
import org.batfish.representation.cisco.SecurityZonePair;
import org.batfish.representation.cisco.ServiceObject;
import org.batfish.representation.cisco.ServiceObjectGroup;
import org.batfish.representation.cisco.ServiceObjectGroup.ServiceProtocol;
import org.batfish.representation.cisco.ServiceObjectGroupLine;
import org.batfish.representation.cisco.ServiceObjectGroupReferenceServiceObjectGroupLine;
import org.batfish.representation.cisco.ServiceObjectReferenceServiceObjectGroupLine;
import org.batfish.representation.cisco.ServiceObjectServiceSpecifier;
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
import org.batfish.representation.cisco.SubnetNetworkObject;
import org.batfish.representation.cisco.TcpServiceObjectGroupLine;
import org.batfish.representation.cisco.TcpUdpServiceObjectGroupLine;
import org.batfish.representation.cisco.Tunnel;
import org.batfish.representation.cisco.Tunnel.TunnelMode;
import org.batfish.representation.cisco.UdpServiceObjectGroupLine;
import org.batfish.representation.cisco.UnimplementedAccessListServiceSpecifier;
import org.batfish.representation.cisco.Vrf;
import org.batfish.representation.cisco.VrfAddressFamily;
import org.batfish.representation.cisco.VrrpGroup;
import org.batfish.representation.cisco.VrrpInterface;
import org.batfish.representation.cisco.WildcardAddressSpecifier;
import org.batfish.vendor.VendorConfiguration;

public class CiscoControlPlaneExtractor extends CiscoParserBaseListener
    implements SilentSyntaxListener, ControlPlaneExtractor {
  private static final String INLINE_SERVICE_OBJECT_NAME = "~INLINE_SERVICE_OBJECT~";

  @VisibleForTesting static final String SERIAL_LINE = "serial";

  @Override
  public String getInputText() {
    return _text;
  }

  @Override
  public BatfishCombinedParser<?, ?> getParser() {
    return _parser;
  }

  @Override
  public Warnings getWarnings() {
    return _w;
  }

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

  private static int toInteger(Uint8Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private static int toInteger(DecContext ctx) {
    return Integer.parseInt(ctx.getText());
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
            CiscoConfiguration.getCanonicalInterfaceNamePrefix(ctx.name_prefix_alpha.getText()));
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

  private static Ip6 toIp6(Token t) {
    return Ip6.parse(t.getText());
  }

  private static long toLong(Ospf_areaContext ctx) {
    if (ctx.num != null) {
      return toLong(ctx.num);
    }
    assert ctx.addr != null;
    return toIp(ctx.addr).asLong();
  }

  private static long toLong(DecContext ctx) {
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

  /** Create a dynamic NAT and reference appropriate config structures. */
  CiscoIosDynamicNat toIosDynamicNat(Ipnc_listContext ctx, RuleAction ruleAction) {
    CiscoIosDynamicNat nat = new CiscoIosDynamicNat();
    String acl = ctx.acl.getText();
    int aclLine = ctx.acl.getStart().getLine();
    nat.setAclName(acl);
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST,
        acl,
        ruleAction == RuleAction.DESTINATION_INSIDE
            ? IP_NAT_DESTINATION_ACCESS_LIST
            : IP_NAT_SOURCE_ACCESS_LIST,
        aclLine);
    String pool = ctx.pool.getText();
    int poolLine = ctx.pool.getStart().getLine();
    nat.setNatPool(pool);
    _configuration.referenceStructure(
        NAT_POOL,
        pool,
        ruleAction == RuleAction.DESTINATION_INSIDE ? IP_NAT_DESTINATION_POOL : IP_NAT_SOURCE_POOL,
        poolLine);
    nat.setAction(ruleAction);
    return nat;
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

  private CiscoConfiguration _configuration;

  private AaaAuthenticationLoginList _currentAaaAuthenticationLoginList;

  private IpAsPathAccessList _currentAsPathAcl;

  private CryptoMapEntry _currentCryptoMapEntry;

  private String _currentCryptoMapName;

  private Integer _currentCryptoMapSequenceNum;

  private NamedRsaPubKey _currentNamedRsaPubKey;

  private DynamicIpBgpPeerGroup _currentDynamicIpPeerGroup;

  @Nullable private String _currentEigrpInterface;

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

  private NamedBgpPeerGroup _currentNamedPeerGroup;

  private CiscoIosNat.Direction _currentIosNatDirection;

  private CiscoIosNat _currentIosSourceNat;

  private String _currentIosNatPoolName;

  private Long _currentOspfArea;

  private String _currentOspfInterface;

  private OspfProcess _currentOspfProcess;

  private BgpPeerGroup _currentPeerGroup;

  private NamedBgpPeerGroup _currentPeerSession;

  private Prefix6List _currentPrefix6List;

  private PrefixList _currentPrefixList;

  private RipProcess _currentRipProcess;

  private RouteMap _currentRouteMap;

  private RouteMapClause _currentRouteMapClause;

  private ServiceClass _currentServiceClass;

  private ServiceObject _currentServiceObject;

  private SnmpCommunity _currentSnmpCommunity;

  private StandardAccessList _currentStandardAcl;

  private StandardCommunityList _currentStandardCommunityList;

  private StandardIpv6AccessList _currentStandardIpv6Acl;

  private User _currentUser;

  private String _currentVrf;

  @Nullable private VrfAddressFamily _currentVrfAddressFamily;

  private VrrpGroup _currentVrrpGroup;

  private Integer _currentVrrpGroupNum;

  private String _currentVrrpInterface;

  private final @Nonnull BgpPeerGroup _dummyPeerGroup = new MasterBgpPeerGroup();

  private final ConfigurationFormat _format;

  private boolean _inIpv6BgpPeer;

  private boolean _no;

  @Nullable private EigrpProcess _parentEigrpProcess;

  private final CiscoCombinedParser _parser;

  private final List<BgpPeerGroup> _peerGroupStack;

  private final String _text;

  private final Warnings _w;

  @Nonnull private final SilentSyntaxCollection _silentSyntax;

  private NetworkObjectGroup _currentNetworkObjectGroup;

  private String _currentNetworkObjectName;

  private IcmpTypeObjectGroup _currentIcmpTypeObjectGroup;

  private ProtocolObjectGroup _currentProtocolObjectGroup;

  private ServiceObjectGroup _currentServiceObjectGroup;

  private InspectClassMap _currentInspectClassMap;

  private InspectPolicyMap _currentInspectPolicyMap;

  private InspectPolicyMapInspectClass _currentInspectPolicyMapInspectClass;

  private SecurityZonePair _currentSecurityZonePair;

  private Integer _currentHsrpGroup;

  private String _currentTrackingGroup;

  /* Set this when moving to different stanzas (e.g., ro_vrf) inside "router ospf" stanza
   * to correctly retrieve the OSPF process that was being configured prior to switching stanzas
   */
  private String _lastKnownOspfProcess;

  private BgpAggregateNetwork _currentAggregate;
  private BgpAggregateIpv4Network _currentIpv4Aggregate;
  private BgpAggregateIpv6Network _currentIpv6Aggregate;

  public CiscoControlPlaneExtractor(
      String text,
      CiscoCombinedParser parser,
      ConfigurationFormat format,
      Warnings warnings,
      SilentSyntaxCollection silentSyntax) {
    _text = text;
    _parser = parser;
    _format = format;
    _w = warnings;
    _peerGroupStack = new ArrayList<>();
    _silentSyntax = silentSyntax;
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

  private void enterBgpVrfAndPushNewPeer(String vrfName) {
    _currentVrf = vrfName;
    Vrf v = currentVrf();
    BgpProcess p = v.getBgpProcess();
    if (p == null) {
      long procNum = _configuration.getDefaultVrf().getBgpProcess().getProcnum();
      p = new BgpProcess(procNum);
      v.setBgpProcess(p);
    }
    pushPeer(p.getMasterBgpPeerGroup());
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
    Bgp_address_familyContext af = ctx.af;
    if (af.VPNV4() != null || af.VPNV6() != null || af.MDT() != null || af.MULTICAST() != null) {
      pushPeer(_dummyPeerGroup);
    } else {
      if (ctx.af.vrf_name != null) {
        enterBgpVrfAndPushNewPeer(ctx.af.vrf_name.getText());
      } else {
        pushPeer(_currentPeerGroup);
      }
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
  public void exitBgp_conf_identifier_rb_stanza(Bgp_conf_identifier_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    long asn = toAsNum(ctx.id);
    proc.setConfederation(asn);
  }

  @Override
  public void exitBgp_conf_peers_rb_stanza(Bgp_conf_peers_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    Set<Long> members = proc.getConfederationMembers();
    for (Bgp_asnContext peer : ctx.peers) {
      members.add(toAsNum(peer));
    }
  }

  @Override
  public void exitBgp_enforce_first_as_stanza(Bgp_enforce_first_as_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.setEnforceFirstAs(true);
  }

  @Override
  public void exitNo_bgp_enforce_first_as_stanza(No_bgp_enforce_first_as_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.setEnforceFirstAs(false);
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
    int encType = ctx.dec() != null ? toInteger(ctx.dec()) : 0;
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
        ISAKMP_POLICY,
        priority.toString(),
        ISAKMP_POLICY_SELF_REF,
        ctx.priority.getStart().getLine());
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
  public void enterCisco_configuration(Cisco_configurationContext ctx) {
    _configuration = new CiscoConfiguration();
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
    int group = Integer.parseInt(ctx.dec().getText());
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
    _currentIsakmpPolicy.setLifetimeSeconds(Integer.parseInt(ctx.dec().getText()));
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
  public void enterClb_docsis_policy(Clb_docsis_policyContext ctx) {
    String name = ctx.policy.getText();
    String rule = ctx.rulenum.getText();
    DocsisPolicy policy =
        _configuration
            .getCf()
            .getCable()
            .getDocsisPolicies()
            .computeIfAbsent(name, DocsisPolicy::new);
    _configuration.defineStructure(DOCSIS_POLICY, name, ctx);
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
    _configuration.defineStructure(DOCSIS_POLICY_RULE, name, ctx);
  }

  @Override
  public void enterCntlr_rf_channel(Cntlr_rf_channelContext ctx) {
    _no = (ctx.NO() != null);
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
  public void enterCs_class(Cs_classContext ctx) {
    String number = ctx.num.getText();
    _currentServiceClass =
        _configuration
            .getCf()
            .getCable()
            .getServiceClasses()
            .computeIfAbsent(number, ServiceClass::new);
    _configuration.defineStructure(SERVICE_CLASS, number, ctx);
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
    _configuration.defineStructure(IPV4_ACCESS_LIST_EXTENDED, name, ctx);
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
    _configuration.defineStructure(IPV6_ACCESS_LIST_EXTENDED, name, ctx);
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
        TRACK, trackingGroup, INTERFACE_STANDBY_TRACK, ctx.group.getStart().getLine());
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
  public void enterIp_as_path_access_list_stanza(Ip_as_path_access_list_stanzaContext ctx) {
    String name = ctx.name.getText();
    _currentAsPathAcl =
        _configuration.getAsPathAccessLists().computeIfAbsent(name, IpAsPathAccessList::new);
    _configuration.defineStructure(AS_PATH_ACCESS_LIST, name, ctx);
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
    _configuration.defineStructure(COMMUNITY_LIST_EXPANDED, name, ctx);
  }

  @Override
  public void enterIp_community_list_standard_stanza(Ip_community_list_standard_stanzaContext ctx) {
    String name;
    if (ctx.num != null) {
      name = ctx.num.getText();
    } else if (ctx.name != null) {
      name = ctx.name.getText();
    } else if (ctx.name_cl != null) {
      name = ctx.name_cl.getText();
    } else {
      throw new BatfishException("Invalid standard community-list name");
    }
    _currentStandardCommunityList =
        _configuration
            .getStandardCommunityLists()
            .computeIfAbsent(name, StandardCommunityList::new);
    _configuration.defineStructure(COMMUNITY_LIST_STANDARD, name, ctx);
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
      _currentVrf = CiscoConfiguration.MANAGEMENT_VRF_NAME;
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
  public void enterNeighbor_flat_rb_stanza(Neighbor_flat_rb_stanzaContext ctx) {
    if (ctx.ip6 != null) {
      // Remember we are in IPv6 context so that structure references are identified accordingly
      _inIpv6BgpPeer = true;
    }
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
        if (create) {
          _currentIpPeerGroup = proc.addIpPeerGroup(ip);
          pushPeer(_currentIpPeerGroup);
        } else {
          _configuration.referenceStructure(
              BGP_UNDECLARED_PEER, ip.toString(), BGP_NEIGHBOR_WITHOUT_REMOTE_AS, ctx.ip.getLine());
          pushPeer(_dummyPeerGroup);
        }
      } else {
        pushPeer(_currentIpPeerGroup);
      }
    } else if (ctx.ip6 != null) {
      Ip6 ip6 = toIp6(ctx.ip6);
      Ipv6BgpPeerGroup pg6 = proc.getIpv6PeerGroups().get(ip6);
      if (pg6 == null) {
        if (create) {
          pg6 = proc.addIpv6PeerGroup(ip6);
          pushPeer(pg6);
        } else {
          _configuration.referenceStructure(
              BGP_UNDECLARED_PEER,
              ip6.toString(),
              BGP_NEIGHBOR_WITHOUT_REMOTE_AS,
              ctx.ip6.getLine());
          pushPeer(_dummyPeerGroup);
        }
      } else {
        pushPeer(pg6);
      }
      _currentIpv6PeerGroup = pg6;
    } else if (ctx.peergroup != null) {
      String name = ctx.peergroup.getText();
      _currentNamedPeerGroup = proc.getNamedPeerGroups().get(name);
      if (_currentNamedPeerGroup == null) {
        if (create) {
          _currentNamedPeerGroup = proc.addNamedPeerGroup(name);
          _configuration.referenceStructure(
              BGP_PEER_GROUP, name, BGP_NEIGHBOR_STATEMENT, ctx.peergroup.getLine());
        } else {
          _configuration.referenceStructure(
              BGP_UNDECLARED_PEER_GROUP,
              name,
              BGP_PEER_GROUP_REFERENCED_BEFORE_DEFINED,
              ctx.peergroup.getLine());
          _currentNamedPeerGroup = new NamedBgpPeerGroup("dummy");
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
  public void enterRo_area(Ro_areaContext ctx) {
    _currentOspfArea = toLong(ctx.ospf_area());
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
                  OspfProcess p = new OspfProcess(procName, _format);
                  p.setRouterId(routerId);
                  return p;
                });
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
      warn(
          ctx,
          String.format(
              "Route map '%s' already contains clause numbered '%d'. Duplicate clause will be"
                  + " merged with original clause.",
              _currentRouteMap.getName(), num));
    }
    _currentRouteMapClause = clause;
    _configuration.defineStructure(ROUTE_MAP, name, ctx);
    String clauseName = computeRouteMapClauseName(name, num);
    _configuration.defineStructure(ROUTE_MAP_CLAUSE, clauseName, ctx);
    _configuration.referenceStructure(
        ROUTE_MAP_CLAUSE, clauseName, ROUTE_MAP_CLAUSE_PREV_REF, ctx.getStart().getLine());
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
  public void enterS_cable(S_cableContext ctx) {
    if (_configuration.getCf().getCable() == null) {
      _configuration.getCf().setCable(new Cable());
    }
  }

  @Override
  public void enterS_class_map(S_class_mapContext ctx) {
    // TODO: do something with this.
    String name = ctx.name.getText();
    _configuration.defineStructure(CLASS_MAP, name, ctx);
  }

  @Override
  public void enterS_depi_class(S_depi_classContext ctx) {
    String name = ctx.name.getText();
    _configuration.getCf().getDepiClasses().computeIfAbsent(name, DepiClass::new);
    _configuration.defineStructure(DEPI_CLASS, name, ctx);
  }

  @Override
  public void enterS_depi_tunnel(S_depi_tunnelContext ctx) {
    String name = ctx.name.getText();
    _configuration.getCf().getDepiTunnels().computeIfAbsent(name, DepiTunnel::new);
    _configuration.defineStructure(DEPI_TUNNEL, name, ctx);
  }

  @Override
  public void enterS_interface(S_interfaceContext ctx) {
    String nameAlpha = ctx.iname.name_prefix_alpha.getText();
    String canonicalNamePrefix;
    try {
      canonicalNamePrefix = CiscoConfiguration.getCanonicalInterfaceNamePrefix(nameAlpha);
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
    List<String> names = getLineNames(ctx);

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

  @Nonnull
  private List<String> getLineNames(S_lineContext ctx) {
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
        _w.addWarning(
            ctx,
            getFullText(ctx),
            _parser,
            String.format("Do not support decreasing line range: %s %s", first, last));
        return ImmutableList.of();
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
    return names;
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
  public void enterS_policy_map(S_policy_mapContext ctx) {
    // TODO: do something with this.
    if (ctx.variable_policy_map_header() != null) {
      String name = ctx.variable_policy_map_header().getText();
      _configuration.defineStructure(POLICY_MAP, name, ctx);
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
            .computeIfAbsent(procName, (pName) -> new OspfProcess(pName, _format));
  }

  @Override
  public void enterS_service_template(S_service_templateContext ctx) {
    // TODO: do something with this.
    String name = ctx.name.getText();
    _configuration.defineStructure(SERVICE_TEMPLATE, name, ctx);
  }

  @Override
  public void exitRo_auto_cost(Ro_auto_costContext ctx) {
    long referenceBandwidthDec = Long.parseLong(ctx.dec().getText());
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
    @Nullable IpProtocol protocol = toIpProtocol(ctx.protocol());
    if (protocol != null) {
      _currentServiceObject.addProtocol(protocol);
    }
    // Else protocol is ip or ipv4, so anything is valid.
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
      proc.addPeerSession(name);
      _currentPeerSession = proc.getPeerSessions().get(name);
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
  public void exitSs_host_generic(Ss_host_genericContext ctx) {
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
    _configuration.getSnmpServer().getHosts().computeIfAbsent(hostname, SnmpHost::new);
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
    _configuration.defineStructure(IPV4_ACCESS_LIST_STANDARD, name, ctx);
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
    _configuration.defineStructure(IPV6_ACCESS_LIST_STANDARD, name, ctx);
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
      proc.addPeerSession(name);
      _currentPeerSession = proc.getPeerSessions().get(name);
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
  public void enterVrrp_interface(Vrrp_interfaceContext ctx) {
    _currentVrrpInterface = getCanonicalInterfaceName(ctx.iface.getText());
    _configuration.referenceStructure(
        INTERFACE, _currentVrrpInterface, ROUTER_VRRP_INTERFACE, ctx.iface.getStart().getLine());
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
  public void exitAddress_family_rb_stanza(Address_family_rb_stanzaContext ctx) {
    if (ctx.address_family_header() != null
        && ctx.address_family_header().af != null
        && ctx.address_family_header().af.vrf_name != null) {
      _currentVrf = Configuration.DEFAULT_VRF_NAME;
    }
    popPeer();
  }

  @Override
  public void exitAdvertise_map_bgp_tail(Advertise_map_bgp_tailContext ctx) {
    // TODO: https://github.com/batfish/batfish/issues/1836
    warn(ctx, "BGP advertise-map is not currently supported");
    String advertiseMapName = ctx.am_name.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, advertiseMapName, BGP_ROUTE_MAP_ADVERTISE, ctx.am_name.getStart().getLine());
    if (ctx.em_name != null) {
      warn(ctx, "BGP exist-map is not currently supported");
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
  public void enterAggregate_address_rb_stanza(Aggregate_address_rb_stanzaContext ctx) {
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
      _currentIpv4Aggregate = new BgpAggregateIpv4Network(prefix);
      _currentAggregate = _currentIpv4Aggregate;
    } else if (ctx.ipv6_prefix != null) {
      // ipv6
      Prefix6 prefix6 = Prefix6.parse(ctx.ipv6_prefix.getText());
      _currentIpv6Aggregate = new BgpAggregateIpv6Network(prefix6);
      _currentAggregate = _currentIpv6Aggregate;
    }
  }

  @Override
  public void exitAgg_advertise_map(Agg_advertise_mapContext ctx) {
    todo(ctx);
    String advertiseMap = ctx.adv_map.getText();
    _currentAggregate.setAdvertiseMap(advertiseMap);
    _configuration.referenceStructure(
        ROUTE_MAP, advertiseMap, BGP_AGGREGATE_ADVERTISE_MAP, ctx.adv_map.getStart().getLine());
  }

  @Override
  public void exitAgg_as_set(Agg_as_setContext ctx) {
    todo(ctx);
    _currentAggregate.setAsSet(true);
  }

  @Override
  public void exitAgg_attribute_map(Agg_attribute_mapContext ctx) {
    String attributeMap = ctx.att_map.getText();
    _currentAggregate.setAttributeMap(attributeMap);
    _configuration.referenceStructure(
        ROUTE_MAP, attributeMap, BGP_AGGREGATE_ATTRIBUTE_MAP, ctx.att_map.getStart().getLine());
  }

  @Override
  public void exitAgg_summary_only(Agg_summary_onlyContext ctx) {
    _currentAggregate.setSummaryOnly(true);
  }

  @Override
  public void exitAgg_suppress_map(Agg_suppress_mapContext ctx) {
    todo(ctx);
    String suppressMap = ctx.sup_map.getText();
    _currentAggregate.setSuppressMap(suppressMap);
    _configuration.referenceStructure(
        ROUTE_MAP, suppressMap, BGP_AGGREGATE_SUPPRESS_MAP, ctx.sup_map.getStart().getLine());
  }

  @Override
  public void exitAggregate_address_rb_stanza(Aggregate_address_rb_stanzaContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    // Intentional identity comparison
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      if (ctx.network != null || ctx.prefix != null) {
        proc.getAggregateNetworks().put(_currentIpv4Aggregate.getPrefix(), _currentIpv4Aggregate);
      } else {
        // ipv6
        assert ctx.ipv6_prefix != null;
        proc.getAggregateIpv6Networks()
            .put(_currentIpv6Aggregate.getPrefix6(), _currentIpv6Aggregate);
      }
    } else if (_currentIpPeerGroup != null
        || _currentIpv6PeerGroup != null
        || _currentDynamicIpPeerGroup != null
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
  public void exitClbdg_docsis_policy(Clbdg_docsis_policyContext ctx) {
    String name = ctx.policy.getText();
    _configuration.referenceStructure(
        DOCSIS_POLICY, name, DOCSIS_GROUP_DOCSIS_POLICY, ctx.getStart().getLine());
  }

  @Override
  public void exitCluster_id_bgp_tail(Cluster_id_bgp_tailContext ctx) {
    Ip clusterId = null;
    if (ctx.dec() != null) {
      long ipAsLong = toLong(ctx.dec());
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
  public void exitS_zone(S_zoneContext ctx) {
    String name = ctx.name.getText();
    if (ctx.SECURITY() != null) {
      _configuration.getSecurityZones().computeIfAbsent(name, SecurityZone::new);
      _configuration.defineStructure(SECURITY_ZONE, name, ctx);
    } else {
      todo(ctx);
      _configuration.defineStructure(TRAFFIC_ZONE, name, ctx);
    }
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
    _configuration.defineStructure(SECURITY_ZONE_PAIR, name, ctx);
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
    if (ctx.SECURITY() != null) {
      _configuration.referenceStructure(SECURITY_ZONE, name, INTERFACE_ZONE_MEMBER, line);
      _currentInterfaces.forEach(iface -> iface.setSecurityZone(name));
    } else {
      _configuration.referenceStructure(TRAFFIC_ZONE, name, INTERFACE_TRAFFIC_ZONE_MEMBER, line);
      todo(ctx);
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
      line = ctx.num.getStart().getLine();
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
    if (ctx.dec() != null) {
      target = toInteger(ctx.dec());
      _configuration.referenceStructure(
          ROUTE_MAP_CLAUSE,
          computeRouteMapClauseName(_currentRouteMap.getName(), target),
          ROUTE_MAP_CONTINUE,
          ctx.getStart().getLine());
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
  public void exitCp_service_policy(Cp_service_policyContext ctx) {
    CiscoStructureUsage usage =
        ctx.INPUT() != null
            ? CONTROL_PLANE_SERVICE_POLICY_INPUT
            : CONTROL_PLANE_SERVICE_POLICY_OUTPUT;
    _configuration.referenceStructure(
        POLICY_MAP, ctx.name.getText(), usage, ctx.name.getStart().getLine());
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
  public void exitCrypto_map_t_match_address(Crypto_map_t_match_addressContext ctx) {
    todo(ctx);
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    _configuration.referenceStructure(IP_ACCESS_LIST, name, CRYPTO_MAP_MATCH_ADDRESS, line);
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
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.setDefaultInformationOriginate(true);
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
      _currentPeerGroup.setDefaultOriginateMap(mapName);
      _configuration.referenceStructure(
          ROUTE_MAP, mapName, BGP_DEFAULT_ORIGINATE_ROUTE_MAP, ctx.map.getStart().getLine());
    }
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
    CiscoStructureUsage usage;
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
    CiscoStructureType type = _inIpv6BgpPeer ? IPV6_ACCESS_LIST : IPV4_ACCESS_LIST;
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
  public void exitDt_protect_tunnel(Dt_protect_tunnelContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.getStart().getLine();
    _configuration.referenceStructure(DEPI_TUNNEL, name, DEPI_TUNNEL_PROTECT_TUNNEL, line);
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
    } else if (ctx.inline_obj != null) {
      // ASA inline service object for a particular protocol
      return SimpleExtendedAccessListServiceSpecifier.builder()
          .setProtocol(toIpProtocol(ctx.inline_obj))
          .build();
    } else if (ctx.inline_obj_icmp != null) {
      // ASA inline service object for a particular ICMP type
      return SimpleExtendedAccessListServiceSpecifier.builder()
          .setProtocol(IpProtocol.ICMP)
          .setIcmpType(toIcmpType(ctx.inline_obj_icmp))
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
        if (_format == CISCO_ASA) {
          wildcard = wildcard.inverted();
        }
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
            tcpFlags);
    _currentExtendedIpv6Acl.addLine(line);
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
    todo(ctx);
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
      newBandwidthBps = toLong(ctx.dec()) * 1000.0D;
    }
    _currentInterfaces.forEach(i -> i.setBandwidth(newBandwidthBps));
  }

  @Override
  public void exitIf_bfd_template(If_bfd_templateContext ctx) {
    _configuration.referenceStructure(
        BFD_TEMPLATE, ctx.name.getText(), INTERFACE_BFD_TEMPLATE, ctx.name.getStart().getLine());
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
    Long newDelayPs;
    if (ctx.NO() != null) {
      newDelayPs = null;
    } else {
      newDelayPs = toLong(ctx.dec()) * 10_000_000;
    }
    _currentInterfaces.forEach(i -> i.setDelay(newDelayPs));
  }

  private @Nullable String computeAggregatedInterfaceName(int num, ConfigurationFormat format) {
    switch (format) {
      case FORCE10:
      case CISCO_IOS:
        return String.format("Port-channel%d", num);

      default:
        _w.redFlag("Don't know how to compute aggregated-interface name for format: " + format);
        return null;
    }
  }

  @Override
  public void exitIf_ip_access_group(If_ip_access_groupContext ctx) {
    String name = ctx.name.getText();
    CiscoStructureUsage usage;
    if (ctx.IN() != null || ctx.INGRESS() != null) {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setIncomingFilter(name);
      }
      usage = INTERFACE_INCOMING_FILTER;
    } else if (ctx.OUT() != null || ctx.EGRESS() != null) {
      for (Interface currentInterface : _currentInterfaces) {
        currentInterface.setOutgoingFilter(name);
      }
      usage = INTERFACE_OUTGOING_FILTER;
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
  public void exitIf_ip_nat_inside(If_ip_nat_insideContext ctx) {
    _configuration
        .getNatInside()
        .addAll(_currentInterfaces.stream().map(Interface::getName).collect(Collectors.toSet()));
  }

  @Override
  public void exitIf_ip_nat_outside(If_ip_nat_outsideContext ctx) {
    _configuration
        .getNatOutside()
        .addAll(_currentInterfaces.stream().map(Interface::getName).collect(Collectors.toSet()));
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
  public void exitIf_ip_policy(If_ip_policyContext ctx) {
    String policyName = ctx.name.getText();
    warn(ctx, "PBR is not supported");
    //    for (Interface currentInterface : _currentInterfaces) {
    //      currentInterface.setRoutingPolicy(policyName);
    //    }
    _configuration.referenceStructure(
        ROUTE_MAP, policyName, INTERFACE_POLICY_ROUTING_MAP, ctx.name.getLine());
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
    if (ctx.LEAK_MAP() != null) {
      _configuration.referenceStructure(
          ROUTE_MAP,
          ctx.mapname.getText(),
          INTERFACE_SUMMARY_ADDRESS_EIGRP_LEAK_MAP,
          ctx.mapname.getStart().getLine());
    }
    todo(ctx);
  }

  @Override
  public void exitIf_ip_verify(If_ip_verifyContext ctx) {
    if (ctx.acl != null) {
      String acl = ctx.acl.getText();
      int line = ctx.acl.getStart().getLine();
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
  public void exitIf_ipv6_traffic_filter(If_ipv6_traffic_filterContext ctx) {
    CiscoStructureUsage usage =
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
    int mtu = toInteger(ctx.dec());
    for (Interface currentInterface : _currentInterfaces) {
      currentInterface.setMtu(mtu);
    }
  }

  @Override
  public void exitIf_member_interface(If_member_interfaceContext ctx) {
    if (_currentInterfaces.size() != 1) {
      warn(ctx, "member-interfaces can only be configured in single-interface context");
      return;
    }
    String member = toInterfaceName(ctx.name);
    _configuration.referenceStructure(
        INTERFACE,
        member,
        CiscoStructureUsage.INTERFACE_MEMBER_INTERFACE,
        ctx.name.getStart().getLine());
    _currentInterfaces.get(0).getMemberInterfaces().add(member);
  }

  @Override
  public void exitIf_service_policy(If_service_policyContext ctx) {
    // TODO: do something with this.
    String mapname = ctx.policy_map.getText();
    _configuration.referenceStructure(
        POLICY_MAP, mapname, INTERFACE_SERVICE_POLICY, ctx.getStart().getLine());
  }

  @Override
  public void exitIf_si_service_policy(If_si_service_policyContext ctx) {
    String mapname = ctx.policy_map.getText();
    _configuration.referenceStructure(
        POLICY_MAP, mapname, INTERFACE_SERVICE_INSTANCE_SERVICE_POLICY, ctx.getStart().getLine());
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
                  ? Interface.getUndeclaredDefaultSwitchportMode(_configuration.getVendor())
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
  public void exitIf_encapsulation_dot1q(If_encapsulation_dot1qContext ctx) {
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
      if (ctx.gre_multipoint != null) {
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
    if (ctx.SECONDARY() != null) {
      todo(ctx); // Batfish VI model does not yet handle secondary VRRP addresses.
      return;
    }
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
  public void exitIfvrrp_ipv4(Ifvrrp_ipv4Context ctx) {
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
  public void exitIfvrrp_priority_level(Ifvrrp_priority_levelContext ctx) {
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
    List<StandardCommunity> communities = new ArrayList<>();
    for (CommunityContext communityCtx : ctx.communities) {
      Long community = toLong(communityCtx);
      if (community == null) {
        warn(ctx, String.format("Invalid standard community: '%s'", communityCtx.getText()));
        return;
      }
      communities.add(StandardCommunity.of(community));
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
  public void enterIp_extcommunity_list_expanded(Ip_extcommunity_list_expandedContext ctx) {
    todo(ctx);
    String name = ctx.name != null ? ctx.name.getText() : Integer.toString(toInteger(ctx.num));
    _configuration.defineStructure(EXTCOMMUNITY_LIST_EXPANDED, name, ctx);
  }

  @Override
  public void enterIp_extcommunity_list_standard(Ip_extcommunity_list_standardContext ctx) {
    todo(ctx);
    String name = ctx.name != null ? ctx.name.getText() : Integer.toString(toInteger(ctx.num));
    _configuration.defineStructure(EXTCOMMUNITY_LIST_STANDARD, name, ctx);
  }

  @Override
  public void exitIpl_policy(Ipl_policyContext ctx) {
    todo(ctx);
    String name = ctx.name.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, IP_LOCAL_POLICY_ROUTE_MAP, ctx.name.start.getLine());
  }

  @Override
  public void exitIpv6l_policy(Ipv6l_policyContext ctx) {
    todo(ctx);
    String name = ctx.name.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, IPV6_LOCAL_POLICY_ROUTE_MAP, ctx.name.start.getLine());
  }

  @Override
  public void enterIpn_pool(Ipn_poolContext ctx) {
    _currentIosNatPoolName = ctx.name.getText();
  }

  @Override
  public void exitIpn_pool(Ipn_poolContext ctx) {
    _currentIosNatPoolName = null;
  }

  @Override
  public void exitIpn_pool_prefix(Ipn_pool_prefixContext ctx) {
    String name = _currentIosNatPoolName;
    Ip first = toIp(ctx.first);
    Ip last = toIp(ctx.last);
    if (first.compareTo(last) > 0) {
      warn(ctx, String.format("Skipping malformed NAT pool %s. First IP > End Ip", name));
      return;
    }
    if (ctx.mask != null) {
      Prefix subnet = IpWildcard.ipWithWildcardMask(first, toIp(ctx.mask).inverted()).toPrefix();
      createNatPool(name, first, last, subnet, ctx);
    } else if (ctx.prefix_length != null) {
      Prefix subnet = Prefix.create(first, Integer.parseInt(ctx.prefix_length.getText()));
      createNatPool(name, first, last, subnet, ctx);
    } else {
      _configuration.getNatPools().put(name, new NatPool(first, last));
    }
    _configuration.defineStructure(NAT_POOL, name, ctx);
  }

  @Override
  public void exitIpn_pool_range(Ipn_pool_rangeContext ctx) {
    String name = _currentIosNatPoolName;
    Ip first = toIp(ctx.first);
    Ip last = toIp(ctx.last);
    if (first.compareTo(last) > 0) {
      warn(ctx, String.format("Skipping malformed NAT pool %s. First IP > End Ip", name));
      return;
    }
    if (ctx.prefix_length != null) {
      Prefix subnet = Prefix.create(first, Integer.parseInt(ctx.prefix_length.getText()));
      createNatPool(name, first, last, subnet, ctx);
    } else {
      _configuration.getNatPools().put(name, new NatPool(first, last));
    }
    _configuration.defineStructure(NAT_POOL, name, ctx);
  }

  /**
   * Check that the pool IPs are contained in the subnet, and warn if not. Then create the pool,
   * while excluding the network/broadcast IPs. This means that if specified first pool IP is
   * numerically less than the first host IP in the subnet, use the first host IP instead.
   * Similarly, if the specified last pool IP is greater than the last host IP in the subnet, use
   * the last host IP instead.
   */
  private void createNatPool(String name, Ip first, Ip last, Prefix subnet, ParserRuleContext ctx) {
    if (!subnet.containsIp(first)) {
      warn(ctx, String.format("Subnet of NAT pool %s does not contain first pool IP", name));
    }
    if (!subnet.containsIp(last)) {
      warn(ctx, String.format("Subnet of NAT pool %s does not contain last pool IP", name));
    }

    Ip firstHostIp = subnet.getFirstHostIp();
    Ip lastHostIp = subnet.getLastHostIp();

    _configuration
        .getNatPools()
        .put(
            name,
            new NatPool(
                first.asLong() < firstHostIp.asLong() ? firstHostIp : first,
                last.asLong() > lastHostIp.asLong() ? lastHostIp : last));
  }

  @Override
  public void enterIpn_inside(Ipn_insideContext ctx) {
    _currentIosNatDirection = Direction.INSIDE;
  }

  @Override
  public void exitIpn_inside(Ipn_insideContext ctx) {
    _currentIosNatDirection = null;
  }

  @Override
  public void enterIpn_outside(Ipn_outsideContext ctx) {
    _currentIosNatDirection = Direction.OUTSIDE;
  }

  @Override
  public void exitIpn_outside(Ipn_outsideContext ctx) {
    _currentIosNatDirection = null;
  }

  @Override
  public void exitIpni_destination(Ipni_destinationContext ctx) {
    _configuration.getCiscoIosNats().add(toIosDynamicNat(ctx.list, RuleAction.DESTINATION_INSIDE));
  }

  @Override
  public void enterIpnis_list(Ipnis_listContext ctx) {
    CiscoIosDynamicNat nat = new CiscoIosDynamicNat();
    nat.setAction(RuleAction.SOURCE_INSIDE);
    String acl = ctx.acl.getText();
    _configuration.referenceStructure(
        IPV4_ACCESS_LIST, acl, IP_NAT_SOURCE_ACCESS_LIST, ctx.getStart().getLine());
    nat.setAclName(ctx.acl.getText());
    if (ctx.iname != null) {
      String iname = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(
          INTERFACE, iname, IP_NAT_INSIDE_SOURCE, ctx.getStart().getLine());
      nat.setInterface(iname);
    } else {
      String pool = ctx.pool.getText();
      _configuration.referenceStructure(
          NAT_POOL, ctx.pool.getText(), IP_NAT_INSIDE_SOURCE, ctx.getStart().getLine());
      nat.setNatPool(pool);
    }
    nat.setOverload(ctx.OVERLOAD() != null);
    _currentIosSourceNat = nat;
    _configuration.getCiscoIosNats().add(_currentIosSourceNat);
  }

  @Override
  public void exitIpnis_list(Ipnis_listContext ctx) {
    _currentIosSourceNat = null;
  }

  @Override
  public void enterIpnos_list(Ipnos_listContext ctx) {
    _currentIosSourceNat = toIosDynamicNat(ctx.acl_pool, RuleAction.SOURCE_OUTSIDE);
    _configuration.getCiscoIosNats().add(_currentIosSourceNat);
  }

  @Override
  public void exitIpnos_list(Ipnos_listContext ctx) {
    _currentIosSourceNat = null;
  }

  /** Return entry containing (local, global) IPs for IOS NAT */
  Entry<Ip, Ip> toIosNatLocalGlobalIps(Ipnioss_local_globalContext ctx) {
    Ip local = toIp(ctx.local);
    Ip global = toIp(ctx.global);
    if (_currentIosNatDirection == Direction.OUTSIDE) {
      // local and global reversed for outside
      Ip tmp = local;
      local = global;
      global = tmp;
    }
    return new SimpleEntry<>(local, global);
  }

  @Override
  public void exitIpnios_static_addr(Ipnios_static_addrContext ctx) {
    assert _currentIosSourceNat instanceof CiscoIosStaticNat;
    CiscoIosStaticNat staticNat = (CiscoIosStaticNat) _currentIosSourceNat;
    Entry<Ip, Ip> e = toIosNatLocalGlobalIps(ctx.ips);
    staticNat.setLocalNetwork(Prefix.create(e.getKey(), Prefix.MAX_PREFIX_LENGTH));
    staticNat.setGlobalNetwork(Prefix.create(e.getValue(), Prefix.MAX_PREFIX_LENGTH));
    _configuration.getCiscoIosNats().add(_currentIosSourceNat);
  }

  @Override
  public void exitIpnios_static_network(Ipnios_static_networkContext ctx) {
    assert _currentIosSourceNat instanceof CiscoIosStaticNat;
    CiscoIosStaticNat staticNat = (CiscoIosStaticNat) _currentIosSourceNat;
    int prefixLength;
    if (ctx.mask != null) {
      Ip mask = toIp(ctx.mask);
      prefixLength = mask.numSubnetBits();
    } else {
      prefixLength = toInteger(ctx.prefix);
    }
    Entry<Ip, Ip> e = toIosNatLocalGlobalIps(ctx.ips);
    staticNat.setLocalNetwork(Prefix.create(e.getKey(), prefixLength));
    staticNat.setGlobalNetwork(Prefix.create(e.getValue(), prefixLength));
    _configuration.getCiscoIosNats().add(_currentIosSourceNat);
  }

  @Override
  public void exitIpnios_vrf(Ipnios_vrfContext ctx) {
    assert _currentIosSourceNat != null;
    _currentIosSourceNat.setVrf(ctx.vrfname.getText());
  }

  @Override
  public void exitIpnosm_add_route(Ipnosm_add_routeContext ctx) {
    assert _currentIosSourceNat != null;
    _currentIosSourceNat.setAddRoute(true);
    if (_currentIosSourceNat instanceof CiscoIosDynamicNat) {
      todo(ctx);
    }
  }

  @Override
  public void exitIpniossm_extendable(Ipniossm_extendableContext ctx) {
    // Translating to multiple public IPs is not currently supported
    // https://networklessons.com/uncategorized/nat-extendable-on-cisco-ios
    todo(ctx);
  }

  @Override
  public void enterIpnis_route_map(Ipnis_route_mapContext ctx) {
    CiscoIosDynamicNat nat = new CiscoIosDynamicNat();
    nat.setAction(RuleAction.SOURCE_INSIDE);
    String routeMap = ctx.mapname.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, ctx.mapname.getText(), IP_NAT_INSIDE_SOURCE, ctx.getStart().getLine());
    nat.setRouteMap(routeMap);
    if (ctx.iname != null) {
      String iname = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(
          INTERFACE, iname, IP_NAT_INSIDE_SOURCE, ctx.getStart().getLine());
      nat.setInterface(iname);
    } else {
      String pool = ctx.pool.getText();
      _configuration.referenceStructure(
          NAT_POOL, ctx.pool.getText(), IP_NAT_INSIDE_SOURCE, ctx.getStart().getLine());
      nat.setNatPool(pool);
    }
    nat.setOverload(ctx.OVERLOAD() != null);
    _currentIosSourceNat = nat;
    _configuration.getCiscoIosNats().add(_currentIosSourceNat);
  }

  @Override
  public void exitIpnis_route_map(Ipnis_route_mapContext ctx) {
    _currentIosSourceNat = null;
  }

  @Override
  public void enterIpnis_static(Ipnis_staticContext ctx) {
    // Note that this NAT is not added to the configuration until its local & global IPs are set
    _currentIosSourceNat = new CiscoIosStaticNat();
    _currentIosSourceNat.setAction(RuleAction.SOURCE_INSIDE);
  }

  @Override
  public void exitIpnis_static(Ipnis_staticContext ctx) {
    if (ctx.ROUTE_MAP() != null) {
      String rmName = ctx.mapname.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, rmName, IP_NAT_INSIDE_SOURCE_STATIC, ctx.getStart().getLine());
      _currentIosSourceNat.setRouteMap(rmName);
    }
    _currentIosSourceNat = null;
  }

  @Override
  public void enterIpnos_route_map(Ipnos_route_mapContext ctx) {
    CiscoIosDynamicNat nat = new CiscoIosDynamicNat();
    nat.setAction(RuleAction.SOURCE_OUTSIDE);
    String routeMap = ctx.mapname.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, ctx.mapname.getText(), IP_NAT_OUTSIDE_SOURCE, ctx.getStart().getLine());
    nat.setRouteMap(routeMap);
    String pool = ctx.pname.getText();
    _configuration.referenceStructure(
        NAT_POOL, pool, IP_NAT_OUTSIDE_SOURCE, ctx.getStart().getLine());
    nat.setNatPool(pool);
    _currentIosSourceNat = nat;
    _configuration.getCiscoIosNats().add(_currentIosSourceNat);
  }

  @Override
  public void exitIpnos_route_map(Ipnos_route_mapContext ctx) {
    _currentIosSourceNat = null;
  }

  @Override
  public void enterIpnos_static(Ipnos_staticContext ctx) {
    // Note that this NAT is not added to the configuration until its local & global IPs are set
    _currentIosSourceNat = new CiscoIosStaticNat();
    _currentIosSourceNat.setAction(RuleAction.SOURCE_OUTSIDE);
  }

  @Override
  public void exitIpnos_static(Ipnos_staticContext ctx) {
    _currentIosSourceNat = null;
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
    long seq = ctx.seqnum != null ? toLong(ctx.seqnum) : _currentPrefixList.getNextSeq();
    SubRange lengthRange = new SubRange(minLen, maxLen);
    PrefixListLine line = new PrefixListLine(seq, action, prefix, lengthRange);
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
      _configuration.referenceStructure(
          TRACK, Integer.toString(track), STATIC_ROUTE_TRACK, ctx.track.getStart().getLine());
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
  public void exitMatch_as_path_access_list_rm_stanza(
      Match_as_path_access_list_rm_stanzaContext ctx) {
    Set<String> names = new TreeSet<>();
    for (VariableContext name : ctx.name_list) {
      names.add(name.getText());
      _configuration.referenceStructure(
          AS_PATH_ACCESS_LIST,
          name.getText(),
          ROUTE_MAP_MATCH_AS_PATH_ACCESS_LIST,
          name.getStart().getLine());
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
  public void exitMatch_extcommunity_rm_stanza(Match_extcommunity_rm_stanzaContext ctx) {
    ImmutableSet.Builder<String> lists = ImmutableSet.builder();
    for (VariableContext v : ctx.name_list) {
      String name = v.getText();
      _configuration.referenceStructure(
          EXTCOMMUNITY_LIST, name, ROUTE_MAP_MATCH_EXTCOMMUNITY, v.getStart().getLine());
      lists.add(name);
    }
    _currentRouteMapClause.addMatchLine(new RouteMapMatchExtcommunityLine(lists.build()));
    todo(ctx);
  }

  @Override
  public void exitMatch_interface_rm_stanza(Match_interface_rm_stanzaContext ctx) {
    Set<String> names = new TreeSet<>();
    for (Interface_nameContext name : ctx.interface_name()) {
      String ifaceName = getCanonicalInterfaceName(name.getText());
      names.add(ifaceName);
      _configuration.referenceStructure(
          INTERFACE, ifaceName, ROUTE_MAP_MATCH_INTERFACE, name.getStart().getLine());
    }
    RouteMapMatchInterfaceLine line = new RouteMapMatchInterfaceLine(names);
    _currentRouteMapClause.addMatchLine(line);
  }

  @Override
  public void exitMatch_ip_access_list_rm_stanza(Match_ip_access_list_rm_stanzaContext ctx) {
    Set<String> names = new TreeSet<>();
    for (Variable_access_listContext v : ctx.name_list) {
      names.add(v.getText());
      _configuration.referenceStructure(
          IPV4_ACCESS_LIST, v.getText(), ROUTE_MAP_MATCH_IPV4_ACCESS_LIST, v.getStart().getLine());
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
    List<RoutingProtocol> rps = new LinkedList<>();
    boolean todo = false;
    if (!ctx.BGP().isEmpty()) {
      todo = true;
    }
    if (!ctx.CONNECTED().isEmpty()) {
      rps.add(RoutingProtocol.CONNECTED);
    }
    if (!ctx.ISIS().isEmpty()) {
      todo = true;
    }
    if (!ctx.OSPF().isEmpty()) {
      todo = true;
    }
    if (!ctx.STATIC().isEmpty()) {
      rps.add(RoutingProtocol.STATIC);
    }
    if (todo) {
      todo(ctx);
      // Do nothing, since we have some unsupported protocols.
      return;
    }
    if (rps.isEmpty()) {
      // This should not be possible.
      warn(ctx, "Unexpected: empty routing protocol list to match against.");
      return;
    }
    _currentRouteMapClause.addMatchLine(new RouteMapMatchSourceProtocolLine(rps));
  }

  @Override
  public void exitMatch_tag_rm_stanza(Match_tag_rm_stanzaContext ctx) {
    Set<Integer> tags = new TreeSet<>();
    for (DecContext t : ctx.tag_list) {
      tags.add(toInteger(t));
    }
    RouteMapMatchTagLine line = new RouteMapMatchTagLine(tags);
    _currentRouteMapClause.addMatchLine(line);
  }

  @Override
  public void exitMaximum_paths_bgp_tail(Maximum_paths_bgp_tailContext ctx) {
    int maximumPaths = toInteger(ctx.paths);
    BgpProcess proc = currentVrf().getBgpProcess();
    assert proc != null;
    if (ctx.EBGP() != null) {
      proc.setMaximumPathsEbgp(maximumPaths);
    } else if (ctx.IBGP() != null) {
      proc.setMaximumPathsIbgp(maximumPaths);
    } else if (ctx.EIBGP() != null) {
      proc.setMaximumPathsEbgp(maximumPaths);
      proc.setMaximumPathsIbgp(maximumPaths);
    } else {
      // On IOS and ASA, no type means EBGP only. For other OS'es handled by this parser, we can add
      // configuration checks.
      proc.setMaximumPathsEbgp(maximumPaths);
    }
  }

  @Override
  public void exitMaximum_peers_bgp_tail(Maximum_peers_bgp_tailContext ctx) {
    todo(ctx);
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
      prefix = Prefix.create(address, prefixLength);
    }
    String map = null;
    if (ctx.mapname != null) {
      map = ctx.mapname.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, map, BGP_NETWORK_ORIGINATION_ROUTE_MAP, ctx.mapname.getStart().getLine());
    }
    BgpNetwork bgpNetwork = new BgpNetwork(map);
    BgpProcess proc = currentVrf().getBgpProcess();
    proc.getIpNetworks().put(prefix, bgpNetwork);
  }

  @Override
  public void exitNetwork6_bgp_tail(Network6_bgp_tailContext ctx) {
    Prefix6 prefix6 = Prefix6.parse(ctx.prefix.getText());
    String map = null;
    if (ctx.mapname != null) {
      map = ctx.mapname.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, map, BGP_NETWORK6_ORIGINATION_ROUTE_MAP, ctx.mapname.getStart().getLine());
    }
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
    Ip routerId = toIp(ctx.id);
    _currentEigrpProcess.setRouterId(routerId);
  }

  @Override
  public void exitRe_eigrp_stub(Re_eigrp_stubContext ctx) {
    // In process context
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    warn(ctx.getParent(), "EIGRP stub is not currently supported");
  }

  @Override
  public void exitRees_leak_map(Rees_leak_mapContext ctx) {
    if (ctx.map != null) {
      _configuration.referenceStructure(
          ROUTE_MAP, ctx.map.getText(), EIGRP_STUB_LEAK_MAP, ctx.map.getStart().getLine());
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
    if (_format == CISCO_ASA) {
      mask = mask.inverted();
    }
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
    String interfaceName = ctx.i.getText(); // Note: Interface alias is not canonicalized for ASA
    if (_format != CISCO_ASA) {
      interfaceName = getCanonicalInterfaceName(interfaceName);
    }
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

    if (!ctx.METRIC().isEmpty()) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }

    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      r.setRouteMap(mapname);
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_BGP_MAP, ctx.map.getStart().getLine());
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

    if (!ctx.METRIC().isEmpty()) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }

    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      r.setRouteMap(mapname);
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_CONNECTED_MAP, ctx.map.getStart().getLine());
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

    if (!ctx.METRIC().isEmpty()) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }

    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      r.setRouteMap(mapname);
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

    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      r.setRouteMap(mapname);
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_OSPF_MAP, ctx.map.getStart().getLine());
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

    if (!ctx.METRIC().isEmpty()) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }

    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      r.setRouteMap(mapname);
      _configuration.referenceStructure(
          ROUTE_MAP, mapname, EIGRP_REDISTRIBUTE_RIP_MAP, ctx.map.getStart().getLine());
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

    if (!ctx.METRIC().isEmpty()) {
      r.setMetric(toEigrpMetricValues(ctx.metric));
    }

    if (ctx.map != null) {
      String mapname = ctx.map.getText();
      r.setRouteMap(mapname);
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
  public void exitRecno_eigrp_router_id(Recno_eigrp_router_idContext ctx) {
    _currentEigrpProcess.setRouterId(null);
  }

  @Override
  public void exitReno_shutdown(Reno_shutdownContext ctx) {
    _currentEigrpProcess.setShutdown(false);
  }

  @Override
  public void exitRe_shutdown(Re_shutdownContext ctx) {
    _currentEigrpProcess.setShutdown(true);
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
      _configuration.referenceStructure(IPV4_ACCESS_LIST, name, PIM_RP_ADDRESS_ACL, line);
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
    _configuration.defineStructure(INSPECT_POLICY_MAP, name, ctx);
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
      _configuration.defineSingleLineStructure(
          CLASS_MAP, "class-default", ctx.getStart().getLine());
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
    _currentInspectPolicyMap.setClassDefaultAction(LineAction.DENY);
  }

  @Override
  public void exitPi_iosicd_pass(Pi_iosicd_passContext ctx) {
    _currentInspectPolicyMap.setClassDefaultAction(LineAction.PERMIT);
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
    CiscoStructureUsage usage;
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
    CiscoStructureType type = _inIpv6BgpPeer ? PREFIX6_LIST : PREFIX_LIST;
    _configuration.referenceStructure(type, listName, usage, line);
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
      if (ctx.map != null) {
        String map = ctx.map.getText();
        r.setRouteMap(map);
        _configuration.referenceStructure(
            ROUTE_MAP, map, BGP_REDISTRIBUTE_CONNECTED_MAP, ctx.map.getStart().getLine());
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
      _configuration.referenceStructure(
          ROUTE_MAP, map, ISIS_REDISTRIBUTE_CONNECTED_MAP, ctx.map.getLine());
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
  public void exitRedistribute_eigrp_bgp_tail(Redistribute_eigrp_bgp_tailContext ctx) {
    BgpProcess proc = currentVrf().getBgpProcess();
    // Intentional identity comparison
    if (_currentPeerGroup == proc.getMasterBgpPeerGroup()) {
      RoutingProtocol sourceProtocol = RoutingProtocol.EIGRP;
      BgpRedistributionPolicy r = new BgpRedistributionPolicy(sourceProtocol);
      proc.getRedistributionPolicies().put(sourceProtocol, r);
      // TODO match specific EIGRP process ID (ctx.id)
      if (ctx.metric != null) {
        int metric = toInteger(ctx.metric);
        r.setMetric(metric);
      }
      if (ctx.map != null) {
        String map = ctx.map.getText();
        r.setRouteMap(map);
        _configuration.referenceStructure(
            ROUTE_MAP, map, BGP_REDISTRIBUTE_EIGRP_MAP, ctx.map.getStart().getLine());
      }
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
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
        r.setRouteMap(map);
        _configuration.referenceStructure(
            ROUTE_MAP, map, BGP_REDISTRIBUTE_OSPF_MAP, ctx.map.getStart().getLine());
      }
      if (!ctx.MATCH().isEmpty()) {
        Set<RoutingProtocol> protocols = new HashSet<>(ctx.ospf_route_type().size());
        for (Ospf_route_typeContext ospf_route_typeContext : ctx.ospf_route_type()) {
          protocols.addAll(toOspfRoutingProtocols(ospf_route_typeContext));
        }
        r.getSpecialAttributes()
            .put(BgpRedistributionPolicy.OSPF_ROUTE_TYPES, new MatchProtocol(protocols));
      }
      if (ctx.procname != null) {
        r.getSpecialAttributes()
            .put(BgpRedistributionPolicy.OSPF_PROCESS_NUMBER, ctx.procname.getText());
      }
    } else if (_currentIpPeerGroup != null || _currentNamedPeerGroup != null) {
      throw new BatfishException("do not currently handle per-neighbor redistribution policies");
    }
  }

  private Set<RoutingProtocol> toOspfRoutingProtocols(Ospf_route_typeContext ctx) {
    if (ctx.INTERNAL() != null) {
      return ImmutableSet.of(RoutingProtocol.OSPF, RoutingProtocol.OSPF_IA);
    }
    // TODO: differentiate between EXTERNAL and NSSA_EXTERNAL, currently they are all E1/2 in VI.
    if (ctx.type != null) {
      int t = toInteger(ctx.type);
      if (t == 1) {
        return ImmutableSet.of(RoutingProtocol.OSPF_E1);
      } else if (t == 2) {
        return ImmutableSet.of(RoutingProtocol.OSPF_E2);
      } else {
        return ImmutableSet.of();
      }
    } else {
      return ImmutableSet.of(RoutingProtocol.OSPF_E1, RoutingProtocol.OSPF_E2);
    }
  }

  @Override
  public void exitRedistribute_ospfv3_bgp_tail(Redistribute_ospfv3_bgp_tailContext ctx) {
    if (ctx.map != null) {
      String map = ctx.map.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, map, BGP_REDISTRIBUTE_OSPFV3_MAP, ctx.map.getStart().getLine());
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
        r.setRouteMap(map);
        _configuration.referenceStructure(
            ROUTE_MAP, map, BGP_REDISTRIBUTE_RIP_MAP, ctx.map.getStart().getLine());
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
        r.setRouteMap(map);
        _configuration.referenceStructure(
            ROUTE_MAP, map, BGP_REDISTRIBUTE_STATIC_MAP, ctx.map.getStart().getLine());
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
      _configuration.referenceStructure(
          ROUTE_MAP, map, ISIS_REDISTRIBUTE_STATIC_MAP, ctx.map.getLine());
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
              .map(CiscoControlPlaneExtractor::toAsNum)
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
  public void exitRoa_default_cost(Roa_default_costContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitRoa_filterlist(Roa_filterlistContext ctx) {
    String prefixListName = ctx.list.getText();
    _configuration.referenceStructure(
        PREFIX_LIST, prefixListName, OSPF_AREA_FILTER_LIST, ctx.list.getStart().getLine());
    todo(ctx);
  }

  @Override
  public void exitRoa_nssa(Roa_nssaContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    NssaSettings settings =
        proc.getNssas().computeIfAbsent(_currentOspfArea, a -> new NssaSettings());
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
  public void exitRoa_range(Roa_rangeContext ctx) {
    Prefix prefix;
    if (ctx.area_prefix != null) {
      prefix = Prefix.parse(ctx.area_prefix.getText());
    } else {
      prefix = Prefix.create(toIp(ctx.area_ip), toIp(ctx.area_subnet));
    }
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
  public void exitRoa_stub(Roa_stubContext ctx) {
    OspfProcess proc = _currentOspfProcess;
    StubSettings settings =
        proc.getStubs().computeIfAbsent(_currentOspfArea, a -> new StubSettings());
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
      proc.setDefaultInformationOriginateMap(map);
      _configuration.referenceStructure(
          ROUTE_MAP, map, OSPF_DEFAULT_ORIGINATE_ROUTE_MAP, ctx.map.getLine());
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
  public void exitRedl_acl(Redl_aclContext ctx) {
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    @Nullable
    String ifaceName = ctx.iname == null ? null : getCanonicalInterfaceName(ctx.iname.getText());
    String filterName = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    DistributeList distributeList =
        new DistributeList(filterName, DistributeListFilterType.ACCESS_LIST);
    if (ctx.IN() != null) {
      _configuration.referenceStructure(
          IP_ACCESS_LIST, filterName, EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_IN, line);
      if (ifaceName != null) {
        _configuration.referenceStructure(
            INTERFACE, ifaceName, EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_IN, line);
        _currentEigrpProcess.getInboundInterfaceDistributeLists().put(ifaceName, distributeList);
      } else {
        _currentEigrpProcess.setInboundGlobalDistributeList(distributeList);
      }
      return;
    }
    _configuration.referenceStructure(
        IP_ACCESS_LIST, filterName, EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_OUT, line);
    if (ifaceName == null) {
      _currentEigrpProcess.setOutboundGlobalDistributeList(distributeList);
    } else {
      _configuration.referenceStructure(
          INTERFACE, ifaceName, EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_OUT, line);
      _currentEigrpProcess.getOutboundInterfaceDistributeLists().put(ifaceName, distributeList);
    }
  }

  @Override
  public void exitRedl_prefix(Redl_prefixContext ctx) {
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    String prefixListName = ctx.name.getText();
    _configuration.referenceStructure(
        PREFIX_LIST,
        prefixListName,
        ctx.IN() == null
            ? EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_OUT
            : EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_IN,
        ctx.name.getStart().getLine());
    if (ctx.gwname != null) {
      _configuration.referenceStructure(
          PREFIX_LIST,
          ctx.gwname.getText(),
          ctx.IN() == null ? EIGRP_DISTRIBUTE_LIST_GATEWAY_OUT : EIGRP_DISTRIBUTE_LIST_GATEWAY_IN,
          ctx.gwname.getStart().getLine());
      warn(ctx.getParent(), "Gateway prefix lists in distribute-list are not supported for EIGRP");
    }

    DistributeList distributeList =
        new DistributeList(prefixListName, DistributeListFilterType.PREFIX_LIST);
    if (ctx.iname != null) {
      String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(
          INTERFACE,
          ifaceName,
          ctx.IN() == null
              ? EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_OUT
              : EIGRP_DISTRIBUTE_LIST_PREFIX_LIST_IN,
          ctx.iname.getStart().getLine());
      if (ctx.IN() == null) {
        _currentEigrpProcess.getOutboundInterfaceDistributeLists().put(ifaceName, distributeList);
      } else {
        _currentEigrpProcess.getInboundInterfaceDistributeLists().put(ifaceName, distributeList);
      }
    } else {
      if (ctx.IN() == null) {
        _currentEigrpProcess.setOutboundGlobalDistributeList(distributeList);
      } else {
        _currentEigrpProcess.setInboundGlobalDistributeList(distributeList);
      }
    }
  }

  @Override
  public void exitRedl_gateway(Redl_gatewayContext ctx) {
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    warn(ctx.getParent(), "Gateways in distribute-list are not supported for EIGRP");
    _configuration.referenceStructure(
        PREFIX_LIST,
        ctx.name.getText(),
        ctx.IN() == null ? EIGRP_DISTRIBUTE_LIST_GATEWAY_OUT : EIGRP_DISTRIBUTE_LIST_GATEWAY_IN,
        ctx.name.getStart().getLine());
    if (ctx.iname != null) {
      _configuration.referenceStructure(
          INTERFACE,
          getCanonicalInterfaceName(ctx.iname.getText()),
          ctx.IN() == null ? EIGRP_DISTRIBUTE_LIST_GATEWAY_OUT : EIGRP_DISTRIBUTE_LIST_GATEWAY_IN,
          ctx.iname.getStart().getLine());
    }
  }

  @Override
  public void exitRedl_route_map(Redl_route_mapContext ctx) {
    if (_currentEigrpProcess == null) {
      warn(ctx, "No EIGRP process available");
      return;
    }
    String routeMapName = ctx.name.getText();
    DistributeList distributeList =
        new DistributeList(routeMapName, DistributeListFilterType.ROUTE_MAP);
    _configuration.referenceStructure(
        ROUTE_MAP,
        routeMapName,
        ctx.IN() == null ? EIGRP_DISTRIBUTE_LIST_ROUTE_MAP_OUT : EIGRP_DISTRIBUTE_LIST_ROUTE_MAP_IN,
        ctx.name.getStart().getLine());
    if (ctx.iname != null) {
      String interfaceName = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(
          INTERFACE,
          interfaceName,
          ctx.IN() == null
              ? EIGRP_DISTRIBUTE_LIST_ROUTE_MAP_OUT
              : EIGRP_DISTRIBUTE_LIST_ROUTE_MAP_IN,
          ctx.iname.getStart().getLine());
      if (ctx.IN() == null) {
        _currentEigrpProcess
            .getOutboundInterfaceDistributeLists()
            .put(interfaceName, distributeList);
      } else {
        _currentEigrpProcess
            .getInboundInterfaceDistributeLists()
            .put(interfaceName, distributeList);
      }
    } else {
      if (ctx.IN() == null) {
        _currentEigrpProcess.setOutboundGlobalDistributeList(distributeList);
      } else {
        _currentEigrpProcess.setInboundGlobalDistributeList(distributeList);
      }
    }
  }

  @Override
  public void exitRo_distance_distance(Ro_distance_distanceContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitRo_distance_ospf(Ro_distance_ospfContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitRo_distribute_list(Ro_distribute_listContext ctx) {
    String name = ctx.name.getText();
    int line = ctx.name.getStart().getLine();
    boolean in = ctx.IN() != null;
    CiscoStructureType type;
    CiscoStructureUsage usage;
    DistributeListFilterType filterType;
    if (ctx.PREFIX() != null) {
      type = PREFIX_LIST;
      filterType = DistributeListFilterType.PREFIX_LIST;
      usage = in ? OSPF_DISTRIBUTE_LIST_PREFIX_LIST_IN : OSPF_DISTRIBUTE_LIST_PREFIX_LIST_OUT;
    } else if (ctx.ROUTE_MAP() != null) {
      filterType = DistributeListFilterType.ROUTE_MAP;
      type = ROUTE_MAP;
      usage = in ? OSPF_DISTRIBUTE_LIST_ROUTE_MAP_IN : OSPF_DISTRIBUTE_LIST_ROUTE_MAP_OUT;
      todo(ctx);
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

  private static boolean ospfRedistributeSubnetsByDefault(ConfigurationFormat format) {
    /*
     * CISCO_IOS requires the subnets keyword or only classful routes will be redistributed.
     *
     * We assume no others does this ridiculous thing. TODO: verify more vendors.
     */
    return format != CISCO_IOS;
  }

  @Override
  public void exitRo_prefix_priority(Ro_prefix_priorityContext ctx) {
    // prefix priority is a feature to help speed up OSPF computation
    // we don't need to implement it but we do need to track references
    if (ctx.map != null) {
      String map = ctx.map.getText();
      _configuration.referenceStructure(
          ROUTE_MAP, map, OSPF_PREFIX_PRIORITY_MAP, ctx.map.getLine());
    }
  }

  @Override
  public void exitRo_redistribute_bgp_cisco(Ro_redistribute_bgp_ciscoContext ctx) {
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
    if (ctx.map != null) {
      String map = ctx.map.getText();
      r.setRouteMap(map);
      _configuration.referenceStructure(
          ROUTE_MAP, map, OSPF_REDISTRIBUTE_BGP_MAP, ctx.map.getLine());
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
      r.setRouteMap(map);
      _configuration.referenceStructure(
          ROUTE_MAP, map, OSPF_REDISTRIBUTE_CONNECTED_MAP, ctx.map.getLine());
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
    if (ctx.map != null) {
      String map = ctx.map.getText();
      r.setRouteMap(map);
      _configuration.referenceStructure(
          ROUTE_MAP, map, OSPF_REDISTRIBUTE_EIGRP_MAP, ctx.map.getStart().getLine());
    }
    r.setOnlyClassfulRoutes(ctx.SUBNETS().isEmpty() && !ospfRedistributeSubnetsByDefault(_format));
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
    if (ctx.map != null) {
      String map = ctx.map.getText();
      r.setRouteMap(map);
      _configuration.referenceStructure(
          ROUTE_MAP, map, OSPF_REDISTRIBUTE_STATIC_MAP, ctx.map.getLine());
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
    CiscoStructureUsage usage =
        in ? OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_IN : OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_OUT;
    _configuration.referenceStructure(PREFIX6_LIST, name, usage, line);

    if (ctx.iname != null) {
      String ifaceName = getCanonicalInterfaceName(ctx.iname.getText());
      _configuration.referenceStructure(INTERFACE, ifaceName, usage, line);
    }
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
      _configuration.referenceStructure(
          TRACK, Integer.toString(track), STATIC_ROUTE_TRACK, ctx.track.getStart().getLine());
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
    } else {
      proc.setDistributeListOut(name);
      proc.setDistributeListOutAcl(acl);
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
  public void exitVrfd_rd(Vrfd_rdContext ctx) {
    currentVrf().setRouteDistinguisher(toRouteDistinguisher(ctx.rd));
  }

  @Override
  public void enterVrfd_address_family(Vrfd_address_familyContext ctx) {
    // NOTE: following IOS XE conventions here, where UNICAST is optional (i.e., ipv4 == ipv4
    // unicast)
    if (ctx.IPV4() != null && ctx.MULTICAST() == null) {
      _currentVrfAddressFamily = currentVrf().getOrCreateIpv4UnicastAddressFamily();
    } else {
      // Everything else (ipv6, multicast) is (so far) unsupported, so make a dummy value
      _currentVrfAddressFamily = new VrfAddressFamily();
    }
  }

  @Override
  public void exitVrfd_address_family(Vrfd_address_familyContext ctx) {
    _currentVrfAddressFamily = null;
  }

  @Override
  public void exitVrfd_route_target(Vrfd_route_targetContext ctx) {
    if (ctx.type.BOTH() != null || ctx.type.IMPORT() != null) {
      currentVrf().getGenericAddressFamilyConfig().addRouteTargetImport(toRouteTarget(ctx.rt));
    }
    if (ctx.type.BOTH() != null || ctx.type.EXPORT() != null) {
      currentVrf().getGenericAddressFamilyConfig().addRouteTargetExport(toRouteTarget(ctx.rt));
    }
  }

  @Override
  public void exitVrfd_af_route_target(Vrfd_af_route_targetContext ctx) {
    assert _currentVrfAddressFamily != null;
    if (ctx.type.BOTH() != null || ctx.type.IMPORT() != null) {
      _currentVrfAddressFamily.addRouteTargetImport(toRouteTarget(ctx.rt));
    }
    if (ctx.type.BOTH() != null || ctx.type.EXPORT() != null) {
      _currentVrfAddressFamily.addRouteTargetExport(toRouteTarget(ctx.rt));
    }
  }

  @Override
  public void exitVrfd_af_import_map(Vrfd_af_import_mapContext ctx) {
    assert _currentVrfAddressFamily != null;
    String name = ctx.name.getText();
    _currentVrfAddressFamily.setImportMap(name);
    _configuration.referenceStructure(
        ROUTE_MAP, name, VRF_DEFINITION_ADDRESS_FAMILY_IMPORT_MAP, ctx.getStart().getLine());
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
    ImmutableList.Builder<StandardCommunity> builder = ImmutableList.builder();
    for (CommunityContext c : ctx.communities) {
      Long community = toLong(c);
      if (community == null) {
        warn(ctx, String.format("Invalid standard community: '%s'.", c.getText()));
        return;
      }
      builder.add(StandardCommunity.of(community));
    }
    RouteMapSetAdditiveCommunityLine line = new RouteMapSetAdditiveCommunityLine(builder.build());
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
      Long community = toLong(c);
      if (community == null) {
        warn(ctx, String.format("Invalid standard community: '%s'.", c.getText()));
        return;
      }
      commList.add(community);
    }
    RouteMapSetCommunityLine line = new RouteMapSetCommunityLine(commList);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_extcommunity_rm_stanza_cost(Set_extcommunity_rm_stanza_costContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitSet_extcommunity_rm_stanza_rt(Set_extcommunity_rm_stanza_rtContext ctx) {
    List<ExtendedCommunity> communities = toExtendedCommunities(ctx.communities);
    RouteMapSetLine line =
        ctx.ADDITIVE() != null
            ? new RouteMapSetExtcommunityRtAdditiveLine(communities)
            : new RouteMapSetExtcommunityRtLine(communities);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_extcommunity_rm_stanza_soo(Set_extcommunity_rm_stanza_sooContext ctx) {
    todo(ctx);
  }

  @Override
  public void exitSet_extcommunity_rm_stanza_vpn_distinguisher(
      Set_extcommunity_rm_stanza_vpn_distinguisherContext ctx) {
    todo(ctx);
  }

  private @Nonnull List<ExtendedCommunity> toExtendedCommunities(
      List<Extended_community_route_targetContext> communities) {
    ImmutableList.Builder<ExtendedCommunity> builder =
        ImmutableList.builderWithExpectedSize(communities.size());
    for (Extended_community_route_targetContext communityCtx : communities) {
      builder.add(toExtendedCommunity(communityCtx));
    }
    return builder.build();
  }

  private @Nonnull ExtendedCommunity toExtendedCommunity(
      Extended_community_route_targetContext ctx) {
    assert ctx.ec_ga_la_literal() != null;
    return toExtendedCommunity(ctx.ec_ga_la_literal());
  }

  private @Nonnull ExtendedCommunity toExtendedCommunity(Ec_ga_la_literalContext ctx) {
    if (ctx.ecgalal_asdot_colon() != null) {
      return toExtendedCommunity(ctx.ecgalal_asdot_colon());
    } else if (ctx.ecgalal_colon() != null) {
      return toExtendedCommunity(ctx.ecgalal_colon());
    } else {
      assert ctx.ecgalal_ip_colon() != null;
      return toExtendedCommunity(ctx.ecgalal_ip_colon());
    }
  }

  private @Nonnull ExtendedCommunity toExtendedCommunity(Ecgalal_asdot_colonContext ctx) {
    // Upcast GA to long so we can shift and combine
    long gaHi16 = toUint16(ctx.ga_high16);
    long gaLo16 = toUint16(ctx.ga_low16);
    long ga = (gaHi16 << 16) | gaLo16;
    int la = toUint16(ctx.la);
    return ExtendedCommunity.target(ga, la);
  }

  private @Nonnull ExtendedCommunity toExtendedCommunity(Ecgalal_colonContext ctx) {
    long ga = toUint32(ctx.ga);
    int la = toUint16(ctx.la);
    return ExtendedCommunity.target(ga, la);
  }

  private @Nonnull ExtendedCommunity toExtendedCommunity(Ecgalal_ip_colonContext ctx) {
    long ga = toIp(ctx.ga).asLong();
    int la = toUint16(ctx.la);
    return ExtendedCommunity.target(ga, la);
  }

  private int toUint16(Uint16Context ctx) {
    return Integer.parseInt(ctx.getText());
  }

  private long toUint32(Uint32Context ctx) {
    return Long.parseLong(ctx.getText());
  }

  @Override
  public void exitSet_local_preference_rm_stanza(Set_local_preference_rm_stanzaContext ctx) {
    LongExpr localPreference = toLocalPreferenceLongExpr(ctx.pref);
    RouteMapSetLocalPreferenceLine line = new RouteMapSetLocalPreferenceLine(localPreference);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void enterSet_metric_eigrp_rm_stanza(Set_metric_eigrp_rm_stanzaContext ctx) {
    EigrpMetricValues metric = toEigrpMetricValues(ctx.metric);
    _currentRouteMapClause.addSetLine(new RouteMapSetMetricEigrpLine(metric));
  }

  @Override
  public void exitSet_metric_rm_stanza(Set_metric_rm_stanzaContext ctx) {
    LongExpr metric = toMetricLongExpr(ctx.metric);
    RouteMapSetMetricLine line = new RouteMapSetMetricLine(metric);
    _currentRouteMapClause.addSetLine(line);
  }

  @Override
  public void exitSet_metric_type_rm_stanza(Set_metric_type_rm_stanzaContext ctx) {
    todo(ctx);
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
  public void exitSet_tag_rm_stanza(Set_tag_rm_stanzaContext ctx) {
    long tag = toLong(ctx.tag);
    _currentRouteMapClause.addSetLine(new RouteMapSetTagLine(tag));
  }

  @Override
  public void exitSet_weight_rm_stanza(Set_weight_rm_stanzaContext ctx) {
    RouteMapSetWeightLine line = new RouteMapSetWeightLine(toInteger(ctx.dec()));
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
  public void exitSs_group_v3(Ss_group_v3Context ctx) {
    if (ctx.v4acl != null) {
      _configuration.referenceStructure(
          IPV4_ACCESS_LIST,
          ctx.v4acl.getText(),
          SNMP_SERVER_GROUP_V3_ACCESS,
          ctx.v4acl.getStart().getLine());
    }
    if (ctx.v6acl != null) {
      _configuration.referenceStructure(
          IPV6_ACCESS_LIST,
          ctx.v6acl.getText(),
          SNMP_SERVER_GROUP_V3_ACCESS_IPV6,
          ctx.v6acl.getStart().getLine());
    }
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
  // Prefix6 prefix6 = Prefix6.parse(ctx.IPV6_PREFIX().getText());
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
  public void exitTl_object(Tl_objectContext ctx) {
    _configuration.referenceStructure(
        TRACK, ctx.name.getText(), TRACK_LIST, ctx.getStart().getLine());
  }

  @Override
  public void exitU_password(U_passwordContext ctx) {
    String passwordString;
    if (ctx.up_cisco() != null) {
      passwordString = ctx.up_cisco().up_cisco_tail().pass.getText();
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
  public void exitUnsuppress_map_bgp_tail(Unsuppress_map_bgp_tailContext ctx) {
    String name = ctx.mapname.getText();
    _configuration.referenceStructure(
        ROUTE_MAP, name, BGP_ROUTE_MAP_UNSUPPRESS, ctx.getStart().getLine());
    warn(ctx, "BGP unsuppress-map is not currently supported");
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
  public void exitVrfd_af_export_nonvpn(Vrfd_af_export_nonvpnContext ctx) {
    warn(ctx, "Export into non-vpnv4/6-address-family RIBs is not currently supported");
    _configuration.referenceStructure(
        ROUTE_MAP,
        ctx.name.getText(),
        VRF_DEFINITION_ADDRESS_FAMILY_EXPORT_MAP,
        ctx.getStart().getLine());
  }

  @Override
  public void exitVrfd_af_export_vpn(Vrfd_af_export_vpnContext ctx) {
    assert _currentVrfAddressFamily != null;
    String name = ctx.name.getText();
    _currentVrfAddressFamily.setExportMap(name);
    _configuration.referenceStructure(
        ROUTE_MAP, name, VRF_DEFINITION_ADDRESS_FAMILY_EXPORT_MAP, ctx.getStart().getLine());
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

  public CiscoConfiguration getConfiguration() {
    return _configuration;
  }

  private String getLocation(ParserRuleContext ctx) {
    return ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ": ";
  }

  private int getPortNumber(PortContext ctx) {
    if (ctx.dec() != null) {
      return toInteger(ctx.dec());
    } else {
      NamedPort namedPort = toNamedPort(ctx);
      return namedPort.number();
    }
  }

  public String getText() {
    return _text;
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
    String canonicalNamePrefix = CiscoConfiguration.getCanonicalInterfaceNamePrefix(nameAlpha);
    String vrf =
        canonicalNamePrefix.equals(CiscoConfiguration.MANAGEMENT_INTERFACE_PREFIX)
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
  public void processParseTree(NetworkSnapshot snapshot, ParserRuleContext tree) {
    ParseTreeWalker walker = new BatfishParseTreeWalker(_parser);
    walker.walk(this, tree);
  }

  private void pushPeer(@Nonnull BgpPeerGroup pg) {
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
    if (ctx.dec() != null) {
      int as = toInteger(ctx.dec());
      return new ExplicitAs(as);
    } else if (ctx.AUTO() != null) {
      return AutoAs.instance();
    } else {
      throw convError(AsExpr.class, ctx);
    }
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
    if (ctx.dec() != null) {
      val = toInteger(ctx.dec());
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

  // Handle ASA style ICMP codes
  private Integer toIcmpType(Icmp_inline_object_typeContext ctx) {
    if (ctx.ICMP_ALTERNATE_ADDRESS() != null) {
      return IcmpType.ALTERNATE_ADDRESS;
    } else if (ctx.ICMP_CONVERSION_ERROR() != null) {
      return IcmpType.CONVERSION_ERROR;
    } else if (ctx.ICMP_ECHO() != null) {
      return IcmpType.ECHO_REQUEST;
    } else if (ctx.ICMP_ECHO_REPLY() != null) {
      return IcmpType.ECHO_REPLY;
    } else if (ctx.ICMP_INFORMATION_REPLY() != null) {
      return IcmpType.INFO_REPLY;
    } else if (ctx.ICMP_INFORMATION_REQUEST() != null) {
      return IcmpType.INFO_REQUEST;
    } else if (ctx.ICMP_MASK_REPLY() != null) {
      return IcmpType.MASK_REPLY;
    } else if (ctx.ICMP_MASK_REQUEST() != null) {
      return IcmpType.MASK_REQUEST;
    } else if (ctx.ICMP_MOBILE_REDIRECT() != null) {
      return IcmpType.MOBILE_REDIRECT;
    } else if (ctx.ICMP_PARAMETER_PROBLEM() != null) {
      return IcmpType.PARAMETER_PROBLEM;
    } else if (ctx.ICMP_REDIRECT() != null) {
      return IcmpType.REDIRECT_MESSAGE;
    } else if (ctx.ICMP_ROUTER_ADVERTISEMENT() != null) {
      return IcmpType.ROUTER_ADVERTISEMENT;
    } else if (ctx.ICMP_ROUTER_SOLICITATION() != null) {
      return IcmpType.ROUTER_SOLICITATION;
    } else if (ctx.ICMP_SOURCE_QUENCH() != null) {
      return IcmpType.SOURCE_QUENCH;
    } else if (ctx.ICMP_TIME_EXCEEDED() != null) {
      return IcmpType.TIME_EXCEEDED;
    } else if (ctx.ICMP_TIMESTAMP_REPLY() != null) {
      return IcmpType.TIMESTAMP_REPLY;
    } else if (ctx.ICMP_TIMESTAMP_REQUEST() != null) {
      return IcmpType.TIMESTAMP_REQUEST;
    } else if (ctx.ICMP_TRACEROUTE() != null) {
      return IcmpType.TRACEROUTE;
    } else if (ctx.ICMP_UNREACHABLE() != null) {
      return IcmpType.DESTINATION_UNREACHABLE;
    } else {
      throw convError(IcmpType.class, ctx);
    }
  }

  /** Returns the given IPv4 protocol, or {@code null} if none is specified. */
  private @Nullable IpProtocol toIpProtocol(ProtocolContext ctx) {
    if (ctx.num != null) {
      int num = toInteger(ctx.num);
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
    } else if (ctx.IGRP() != null) {
      // Wikipedia: 9 Interior Gateway Protocol (any private interior gateway, for example Cisco's
      // IGRP)
      assert IpProtocol.IGP.number() == 9;
      return IpProtocol.IGP;
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
    } else if (ctx.NOS() != null) {
      // Wikipedia: 94  KA9Q NOS compatible IP over IP tunneling
      assert IpProtocol.IPIP.number() == 94;
      return IpProtocol.IPIP;
    } else if (ctx.OSPF() != null) {
      return IpProtocol.OSPF;
    } else if (ctx.PIM() != null) {
      return IpProtocol.PIM;
    } else if (ctx.SCTP() != null) {
      return IpProtocol.SCTP;
    } else if (ctx.SNP() != null) {
      return IpProtocol.SNP;
    } else if (ctx.TCP() != null) {
      return IpProtocol.TCP;
    } else if (ctx.UDP() != null) {
      return IpProtocol.UDP;
    } else if (ctx.VRRP() != null) {
      return IpProtocol.VRRP;
    } else {
      return convProblem(IpProtocol.class, ctx, null);
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
    if (ctx.dec() != null) {
      long val = toLong(ctx.dec());
      if (ctx.PLUS() != null) {
        return new IncrementLocalPreference(val);
      } else if (ctx.DASH() != null) {
        return new DecrementLocalPreference(val);
      } else {
        return new LiteralLong(val);
      }
    } else {
      /*
       * Unsupported local-preference integer expression - do not add cases
       * unless you know what you are doing
       */
      throw convError(LongExpr.class, ctx);
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
    if (ctx.dec() != null) {
      int severityNum = toInteger(ctx.dec());
      return toLoggingSeverity(severityNum);
    } else {
      return ctx.getText();
    }
  }

  private Integer toLoggingSeverityNum(Logging_severityContext ctx) {
    if (ctx.dec() != null) {
      return toInteger(ctx.dec());
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
      // Cisco LOCAL_AS is interpreted as RFC1997 NO_EXPORT_SUBCONFED: internet forums.
      return WellKnownCommunity.NO_EXPORT_SUBCONFED;
    } else if (ctx.NO_ADVERTISE() != null) {
      return WellKnownCommunity.NO_ADVERTISE;
    } else if (ctx.NO_EXPORT() != null) {
      return WellKnownCommunity.NO_EXPORT;
    } else {
      return convProblem(Long.class, ctx, null);
    }
  }

  private static long toLong(Uint32Context ctx) {
    return Long.parseLong(ctx.getText());
  }

  private LongExpr toMetricLongExpr(Int_exprContext ctx) {
    if (ctx.dec() != null) {
      long val = toLong(ctx.dec());
      if (ctx.PLUS() != null) {
        return new IncrementMetric(val);
      } else if (ctx.DASH() != null) {
        return new DecrementMetric(val);
      } else {
        return new LiteralLong(val);
      }
    } else if (ctx.IGP_COST() != null) {
      return new IgpCost();
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

  private <T, U extends T> T convProblem(
      Class<T> returnType, ParserRuleContext ctx, U defaultReturnValue) {
    warn(ctx, convErrorMessage(returnType, ctx));
    return defaultReturnValue;
  }

  @Nonnull
  private RouteDistinguisher toRouteDistinguisher(Route_distinguisherContext ctx) {
    long dec = toLong(ctx.dec());
    if (ctx.IP_ADDRESS() != null) {
      checkArgument(dec <= 0xFFFFL, "Invalid route distinguisher %s", ctx.getText());
      return RouteDistinguisher.from(toIp(ctx.IP_ADDRESS()), (int) dec);
    }
    return RouteDistinguisher.from(toAsNum(ctx.bgp_asn()), dec);
  }

  @Nonnull
  private ExtendedCommunity toRouteTarget(Route_targetContext ctx) {
    long la = toLong(ctx.dec());
    if (ctx.IP_ADDRESS() != null) {
      return ExtendedCommunity.target(toIp(ctx.IP_ADDRESS()).asLong(), la);
    }
    return ExtendedCommunity.target(toAsNum(ctx.bgp_asn()), la);
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

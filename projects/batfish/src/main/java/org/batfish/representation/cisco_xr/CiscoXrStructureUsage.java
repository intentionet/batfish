package org.batfish.representation.cisco_xr;

import org.batfish.vendor.StructureUsage;

public enum CiscoXrStructureUsage implements StructureUsage {
  ACCESS_GROUP_GLOBAL_FILTER("access-group global filter"),
  BGP_ADDITIONAL_PATHS_SELECTION_ROUTE_POLICY("bgp additional-paths selection route-policy"),
  BGP_AGGREGATE_ROUTE_POLICY("aggregate-address route-policy"),
  BGP_DEFAULT_ORIGINATE_ROUTE_POLICY("bgp default-originate route-policy"),
  BGP_INBOUND_PREFIX_LIST("bgp inbound prefix-list"),
  BGP_INBOUND_PREFIX6_LIST("bgp inbound ipv6 prefix-list"),
  BGP_INHERITED_PEER_POLICY("inherited BGP peer-policy"),
  BGP_INHERITED_SESSION("inherited BGP peer-session"),
  BGP_LISTEN_RANGE_PEER_GROUP("bgp listen range peer-group"),
  BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_IN("bgp neighbor distribute-list access-list in"),
  BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS_LIST_OUT("bgp neighbor distribute-list access-list out"),
  BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_IN("bgp neighbor distribute-list ipv6 access-list in"),
  BGP_NEIGHBOR_DISTRIBUTE_LIST_ACCESS6_LIST_OUT(
      "bgp neighbor distribute-list ipv6 access-list out"),
  BGP_NEIGHBOR_PEER_GROUP("bgp neighbor peer-group"),
  BGP_NEIGHBOR_ROUTE_POLICY_IN("bgp neighbor route-policy in"),
  BGP_NEIGHBOR_ROUTE_POLICY_OUT("bgp neighbor route-policy out"),
  BGP_NETWORK_ROUTE_POLICY("bgp network route-policy"),
  BGP_OUTBOUND_PREFIX_LIST("bgp outbound prefix-list"),
  BGP_OUTBOUND_PREFIX6_LIST("bgp outbound ipv6 prefix-list"),
  BGP_REDISTRIBUTE_CONNECTED_ROUTE_POLICY("bgp redistribute connected route-policy"),
  BGP_REDISTRIBUTE_STATIC_ROUTE_POLICY("bgp redistribute static route-policy"),
  BGP_UPDATE_SOURCE_INTERFACE("update-source interface"),
  BGP_USE_AF_GROUP("bgp use af-group"),
  BGP_USE_NEIGHBOR_GROUP("bgp use neighbor-group"),
  BGP_USE_SESSION_GROUP("bgp use session-group"),
  CLASS_MAP_ACCESS_GROUP("class-map access-group"),
  CLASS_MAP_ACCESS_LIST("class-map access-list"),
  CLASS_MAP_ACTIVATED_SERVICE_TEMPLATE("class-map activate-service-template"),
  CLASS_MAP_SERVICE_TEMPLATE("class-map service-template"),
  CONTROL_PLANE_ACCESS_GROUP("control-plane ip access-group"),
  CONTROL_PLANE_SERVICE_POLICY_INPUT("control-plane service-policy input"),
  CONTROL_PLANE_SERVICE_POLICY_OUTPUT("control-plane service-policy output"),
  COPS_LISTENER_ACCESS_LIST("cops listener access-list"),
  CRYPTO_DYNAMIC_MAP_ACL("crypto dynamic-map acl"),
  CRYPTO_DYNAMIC_MAP_ISAKMP_PROFILE("crypto dynamic-map isakmp-profile"),
  CRYPTO_DYNAMIC_MAP_TRANSFORM_SET("crypto dynamic-map transform-set"),
  CRYPTO_MAP_IPSEC_ISAKMP_ACL("crypto map ipsec-isakmp acl"),
  CRYPTO_MAP_IPSEC_ISAKMP_CRYPTO_DYNAMIC_MAP_SET("crypto map ipsec-isakmp crypto-dynamic-map-set"),
  CRYPTO_MAP_IPSEC_ISAKMP_ISAKMP_PROFILE("crypto map ipsec-isakmp isakmp-profile"),
  CRYPTO_MAP_IPSEC_ISAKMP_TRANSFORM_SET("crypto map ipsec-isakmp transform-set"),
  DOMAIN_LOOKUP_SOURCE_INTERFACE("domain lookup source-interface"),
  EIGRP_AF_INTERFACE("eigrp address-family af-interface"),
  EIGRP_DISTRIBUTE_LIST_ACCESS_LIST_OUT("router eigrp distribute-list out"),
  EIGRP_PASSIVE_INTERFACE("eigrp passive-interface"),
  FAILOVER_LAN_INTERFACE("failover lan interface"),
  FAILOVER_LINK_INTERFACE("failover link interface"),
  INSPECT_CLASS_MAP_MATCH_ACCESS_GROUP("class-map type inspect match access-group"),
  INTERFACE_BFD_TEMPLATE("interface bfd template"),
  INTERFACE_IGMP_ACCESS_GROUP_ACL("interface igmp access-group acl"),
  INTERFACE_IGMP_HOST_PROXY_ACCESS_LIST("interface igmp host-proxy access-list"),
  INTERFACE_IGMP_STATIC_GROUP_ACL("interface igmp static-group acl"),
  INTERFACE_INCOMING_FILTER("interface incoming ip access-list"),
  INTERFACE_IP_INBAND_ACCESS_GROUP("interface ip inband access-group"),
  INTERFACE_IP_VERIFY_ACCESS_LIST("interface ip verify access-list"),
  INTERFACE_IPV6_TRAFFIC_FILTER_IN("interface ipv6 traffic-filter in"),
  INTERFACE_IPV6_TRAFFIC_FILTER_OUT("interface ipv6 traffic-filter out"),
  INTERFACE_OUTGOING_FILTER("interface outgoing ip access-list"),
  INTERFACE_PIM_NEIGHBOR_FILTER("interface ip pim neighbor-filter"),
  INTERFACE_SELF_REF("interface"),
  INTERFACE_SERVICE_POLICY("interface service-policy"),
  INTERFACE_STANDBY_TRACK("interface standby track"),
  IP_DOMAIN_LOOKUP_INTERFACE("ip domain lookup interface"),
  IP_ROUTE_NHINT("ip route next-hop interface"),
  IP_TACACS_SOURCE_INTERFACE("ip tacacs source-interface"),
  IPSEC_PROFILE_ISAKMP_PROFILE("ipsec profile set isakmp-profile"),
  IPSEC_PROFILE_TRANSFORM_SET("ipsec profile set transform-set"),
  ISAKMP_POLICY_SELF_REF("isakmp policy"),
  ISAKMP_PROFILE_KEYRING("isakmp profile keyring"),
  ISAKMP_PROFILE_SELF_REF("isakmp profile"),
  LINE_ACCESS_CLASS_LIST("line access-class list"),
  LINE_ACCESS_CLASS_LIST6("line access-class ipv6 list"),
  MANAGEMENT_SSH_ACCESS_GROUP("management ssh ip access-group"),
  MANAGEMENT_TELNET_ACCESS_GROUP("management telnet ip access-group"),
  MSDP_PEER_SA_LIST("msdp peer sa-list"),
  NAMED_RSA_PUB_KEY_SELF_REF("named rsa pubkey"),
  NETWORK_OBJECT_GROUP_GROUP_OBJECT("object-group network group-object"),
  NETWORK_OBJECT_GROUP_NETWORK_OBJECT("object-group network network-object object"),
  NTP_ACCESS_GROUP("ntp access-group"),
  NTP_SOURCE_INTERFACE("ntp source-interface"),
  OSPF_AREA_FILTER_LIST("ospf area filter-list"),
  OSPF_AREA_INTERFACE("router ospf area interface"),
  OSPF_DEFAULT_INFORMATION_ROUTE_POLICY("router ospf default-information route-policy"),
  OSPF_DISTRIBUTE_LIST_ACCESS_LIST_IN("router ospf distribute-list in"),
  OSPF_DISTRIBUTE_LIST_ACCESS_LIST_OUT("router ospf distribute-list out"),
  OSPF_DISTRIBUTE_LIST_PREFIX_LIST_IN("router ospf distribute-list prefix in"),
  OSPF_DISTRIBUTE_LIST_PREFIX_LIST_OUT("router ospf distribute-list prefix out"),
  OSPF_REDISTRIBUTE_CONNECTED_ROUTE_POLICY("router ospf redistribute connected route-policy"),
  OSPF_REDISTRIBUTE_STATIC_ROUTE_POLICY("router ospf redistribute static route-policy"),
  OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_IN("ipv6 router ospf distribute-list prefix-list in"),
  OSPF6_DISTRIBUTE_LIST_PREFIX_LIST_OUT("ipv6 router ospf distribute-list prefix-list out"),
  PIM_ACCEPT_REGISTER_ACL("pim accept-register acl"),
  PIM_ACCEPT_RP_ACL("pim accept-rp acl"),
  PIM_RP_ADDRESS_ACL("pim rp-address"),
  PIM_RP_ANNOUNCE_FILTER("pim rp announce filter"),
  PIM_RP_CANDIDATE_ACL("pim rp candidate acl"),
  PIM_SEND_RP_ANNOUNCE_ACL("pim send rp announce acl"),
  PIM_SPT_THRESHOLD_ACL("pim spt threshold acl"),
  POLICY_MAP_CLASS("policy-map class"),
  POLICY_MAP_CLASS_SERVICE_POLICY("policy-map class service-policy"),
  POLICY_MAP_EVENT_CLASS("policy-map event class"),
  POLICY_MAP_EVENT_CLASS_ACTIVATE("policy-map event class activate"),
  RIP_DISTRIBUTE_LIST("router rip distribute-list"),
  ROUTE_POLICY_APPLY_EXPR("route-policy apply (boolean expression)"),
  ROUTE_POLICY_APPLY_STATEMENT("route-policy apply (statement)"),
  ROUTE_POLICY_AS_PATH_IN("route-policy as-path in"),
  ROUTE_POLICY_COMMUNITY_MATCHES_ANY("route-policy community matches-any"),
  ROUTE_POLICY_COMMUNITY_MATCHES_EVERY("route-policy community matches-every"),
  ROUTE_POLICY_DELETE_COMMUNITY_IN("route-policy delete community [not] in"),
  ROUTE_POLICY_EXTCOMMUNITY_RT_MATCHES_ANY("route-policy extcommunity rt matches-any"),
  ROUTE_POLICY_EXTCOMMUNITY_RT_MATCHES_EVERY("route-policy extcommunity rt matches-every"),
  ROUTE_POLICY_PREFIX_SET("route-policy prefix-set"),
  ROUTE_POLICY_SET_COMMUNITY("route-policy set community"),
  ROUTE_POLICY_SET_EXTCOMMUNITY_RT("route-policy set extcommunity rt"),
  ROUTER_ISIS_DISTRIBUTE_LIST_ACL("router isis distribute-list acl"),
  ROUTER_ISIS_REDISTRIBUTE_CONNECTED_ROUTE_POLICY(
      "router isis redistribute connected route-policy"),
  ROUTER_ISIS_REDISTRIBUTE_STATIC_ROUTE_POLICY("router isis redistribute static route-policy"),
  ROUTER_STATIC_ROUTE("router static route"),
  ROUTER_VRRP_INTERFACE("router vrrp interface"),
  SERVICE_OBJECT_GROUP_SERVICE_OBJECT("object-group service service-object object"),
  SERVICE_POLICY_GLOBAL("service-policy global"),
  SNMP_SERVER_COMMUNITY_ACL("snmp server community acl"),
  SNMP_SERVER_COMMUNITY_ACL4("snmp server community ipv4 acl"),
  SNMP_SERVER_COMMUNITY_ACL6("snmp server community ipv6 acl"),
  SNMP_SERVER_FILE_TRANSFER_ACL("snmp server file transfer acl"),
  SNMP_SERVER_SOURCE_INTERFACE("snmp-server source-interface"),
  SNMP_SERVER_TFTP_SERVER_LIST("snmp server tftp-server list"),
  SNMP_SERVER_TRAP_SOURCE("snmp-server trap-source"),
  SYSTEM_SERVICE_POLICY("system service-policy"),
  SSH_ACL("ssh acl"),
  SSH_IPV4_ACL("ssh ipv4 access-list"),
  SSH_IPV6_ACL("ssh ipv6 access-list"),
  TACACS_SOURCE_INTERFACE("tacacs source-interface"),
  TRACK_INTERFACE("track interface"),
  TUNNEL_PROTECTION_IPSEC_PROFILE("interface TunnelX tunnel protection ipsec profile"),
  TUNNEL_SOURCE("tunnel source"),
  WCCP_GROUP_LIST("ip wccp group-list"),
  WCCP_REDIRECT_LIST("ip wccp redirect-list"),
  WCCP_SERVICE_LIST("ip wccp service-list");

  private final String _description;

  CiscoXrStructureUsage(String description) {
    _description = description;
  }

  @Override
  public String getDescription() {
    return _description;
  }
}

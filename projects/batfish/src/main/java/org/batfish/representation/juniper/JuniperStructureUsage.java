package org.batfish.representation.juniper;

import org.batfish.vendor.StructureUsage;

public enum JuniperStructureUsage implements StructureUsage {
  ADDRESS_BOOK_ATTACH_ZONE("address-book attach zone"),
  AGGREGATE_ROUTE_POLICY("aggregate route policy"),
  APPLICATION_SET_MEMBER_APPLICATION("application-set member application"),
  APPLICATION_SET_MEMBER_APPLICATION_SET("application-set member application-set"),
  AS_PATH_GROUP_AS_PATH_SELF_REFERENCE("as-path-group as-path"),
  AUTHENTICATION_KEY_CHAINS_POLICY("authentication-key-chains policy"),
  BGP_ALLOW("bgp group allow"),
  BGP_EXPORT_POLICY("bgp export policy-statement"),
  BGP_IMPORT_POLICY("bgp import policy-statement"),
  BGP_NEIGHBOR("bgp group neighbor"),
  BGP_FAMILY_INET_UNICAST("bgp family inet unicast"),
  DHCP_RELAY_GROUP_ACTIVE_SERVER_GROUP("dhcp relay group active-server-group"),
  FIREWALL_FILTER_DESTINATION_PREFIX_LIST("firewall filter destination prefix-list"),
  FIREWALL_FILTER_PREFIX_LIST("firewall filter prefix-list"),
  FIREWALL_FILTER_SOURCE_PREFIX_LIST("firewall filter source prefix-list"),
  FORWARDING_OPTIONS_DHCP_RELAY_GROUP_INTERFACE("fowarding-options dhcp-relay group interface"),
  FORWARDING_TABLE_EXPORT_POLICY("forwarding-table export policy-statement"),
  GENERATED_ROUTE_POLICY("generated route policy-statement"),
  IKE_GATEWAY_EXTERNAL_INTERFACE("ike gateway external-interface"),
  IKE_GATEWAY_IKE_POLICY("ike gateway ike policy"),
  IKE_POLICY_IKE_PROPOSAL("ike policy ike proposal"),
  INTERFACE_FILTER("interface firewall filter"),
  INTERFACE_INCOMING_FILTER("interface incoming firewall filter"),
  INTERFACE_OUTGOING_FILTER("interface outgoing firewall filter"),
  INTERFACE_ROUTING_OPTIONS("routing-options interface-routes"),
  INTERFACE_SELF_REFERENCE("interface"),
  INTERFACE_VLAN("interface vlan"),
  IPSEC_POLICY_IPSEC_PROPOSAL("ipsec policy ipsec proposal"),
  IPSEC_VPN_BIND_INTERFACE("ipsec vpn bind-interface"),
  IPSEC_VPN_IKE_GATEWAY("ipsec vpn ike gateway"),
  IPSEC_VPN_IPSEC_POLICY("ipsec vpn ipsec policy"),
  ISIS_INTERFACE("isis interface"),
  NAT_DESTINATINATION_RULE_SET_RULE_THEN("nat destination rule-set rule then pool"),
  NAT_SOURCE_RULE_SET_RULE_THEN("nat source rule-set rule then pool"),
  NAT_STATIC_RULE_SET_RULE_THEN("nat static rule-set rule then pool"),
  OSPF_AREA_INTERFACE("ospf area interface"),
  OSPF_EXPORT_POLICY("ospf export policy-statement"),
  POLICY_STATEMENT_FROM_AS_PATH_GROUP("policy-statement from as-path-group"),
  POLICY_STATEMENT_FROM_INTERFACE("policy-statement from interface"),
  POLICY_STATEMENT_POLICY("policy-statement policy"),
  POLICY_STATEMENT_PREFIX_LIST("policy-statement prefix-list"),
  POLICY_STATEMENT_PREFIX_LIST_FILTER("policy-statement prefix-list-filter"),
  ROUTING_INSTANCE_INTERFACE("routing-instance interface"),
  ROUTING_INSTANCE_VRF_EXPORT("routing-instance vrf-export"),
  ROUTING_INSTANCE_VRF_IMPORT("routing-instance vrf-import"),
  SECURITY_POLICY_MATCH_APPLICATION("security policy match application"),
  SECURITY_PROFILE_LOGICAL_SYSTEM("security-profile logical-system"),
  SECURITY_ZONES_SECURITY_ZONES_INTERFACE("security zones security-zone interfaces"),
  SNMP_COMMUNITY_PREFIX_LIST("snmp community prefix-list"),
  STATIC_ROUTE_NEXT_HOP_INTERFACE("static route next-hop"),
  VTEP_SOURCE_INTERFACE("routing-instances vtep-source-interface");

  private final String _description;

  JuniperStructureUsage(String description) {
    _description = description;
  }

  @Override
  public String getDescription() {
    return _description;
  }
}

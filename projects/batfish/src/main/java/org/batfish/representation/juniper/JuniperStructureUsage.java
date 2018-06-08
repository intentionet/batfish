package org.batfish.representation.juniper;

import org.batfish.vendor.StructureUsage;

public enum JuniperStructureUsage implements StructureUsage {
  APPLICATION_SET_MEMBER_APPLICATION("application-set member application"),
  APPLICATION_SET_MEMBER_APPLICATION_SET("application-set member application-set"),
  AUTHENTICATION_KEY_CHAINS_POLICY("authentication-key-chains policy"),
  BGP_ALLOW("bgp group allow"),
  BGP_EXPORT_POLICY("bgp export policy-statement"),
  BGP_IMPORT_POLICY("bgp import policy-statement"),
  BGP_NEIGHBOR("bgp group neighbor"),
  DHCP_RELAY_GROUP_ACTIVE_SERVER_GROUP("dhcp relay group active-server-group"),
  FIREWALL_FILTER_DESTINATION_PREFIX_LIST("firewall filter destination prefix-list"),
  FIREWALL_FILTER_PREFIX_LIST("firewall filter prefix-list"),
  FIREWALL_FILTER_SOURCE_PREFIX_LIST("firewall filter source prefix-list"),
  FORWARDING_TABLE_EXPORT_POLICY("forwarding-table export policy-statement"),
  GENERATED_ROUTE_POLICY("generated route policy-statement"),
  IKE_GATEWAY_EXTERNAL_INTERFACE("ike gateway external-interface"),
  IKE_GATEWAY_IKE_POLICY("ike gateway ike policy"),
  IKE_POLICY_IKE_PROPOSAL("ike policy ike proposal"),
  INTERFACE_FILTER("interface firewall filter"),
  INTERFACE_INCOMING_FILTER("interface incoming firewall filter"),
  INTERFACE_OUTGOING_FILTER("interface outgoing firewall filter"),
  INTERFACE_VLAN("interface vlan"),
  IPSEC_POLICY_IPSEC_PROPOSAL("ipsec policy ipsec proposal"),
  IPSEC_VPN_BIND_INTERFACE("ipsec vpn bind-interface"),
  IPSEC_VPN_IKE_GATEWAY("ipsec vpn ike gateway"),
  IPSEC_VPN_IPSEC_POLICY("ipsec vpn ipsec policy"),
  OSPF_EXPORT_POLICY("ospf export policy-statement"),
  POLICY_STATEMENT_PREFIX_LIST("policy-statement prefix-list"),
  POLICY_STATEMENT_PREFIX_LIST_FILTER("policy-statement prefix-list-filter"),
  SECURITY_POLICY_MATCH_APPLICATION("security policy match application"),
  SNMP_COMMUNITY_PREFIX_LIST("snmp community prefix-list");

  private final String _description;

  JuniperStructureUsage(String description) {
    _description = description;
  }

  @Override
  public String getDescription() {
    return _description;
  }
}

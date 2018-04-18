package org.batfish.representation.cisco;

import org.batfish.vendor.StructureType;

public enum CiscoStructureType implements StructureType {
  ACCESS_LIST("acl"),
  AS_PATH_ACCESS_LIST("as-path acl"),
  AS_PATH_SET("as-path-set"),
  BGP_PEER_GROUP("bgp group"),
  BGP_PEER_SESSION("bgp session"),
  COMMUNITY_LIST("community-list"),
  COMMUNITY_LIST_EXPANDED("expanded community-list"),
  COMMUNITY_LIST_STANDARD("standard community-list"),
  DEPI_CLASS("depi-class"),
  DEPI_TUNNEL("depi-tunnel"),
  DOCSIS_POLICY("docsis-policy"),
  DOCSIS_POLICY_RULE("docsis-policy-rule"),
  INTERFACE("interface"),
  IP_ACCESS_LIST("ipv4/6 acl"),
  IP_ACCESS_LIST_EXTENDED("extended ip access-list"),
  IP_ACCESS_LIST_STANDARD("standard ip access-list"),
  IPSEC_PROFILE("crypto ipsec profile"),
  IPSEC_TRANSFORM_SET("crypto ipsec transform-set"),
  IPV4_ACCESS_LIST("ipv4 acl"),
  IPV6_ACCESS_LIST("ipv6 acl"),
  IPV6_ACCESS_LIST_EXTENDED("extended ipv6 access-list"),
  IPV6_ACCESS_LIST_STANDARD("standard ipv6 access-list"),
  KEYRING("crypto keyring"),
  L2TP_CLASS("l2tp-class"),
  MAC_ACCESS_LIST("mac acl"),
  NAT_POOL("nat pool"),
  NETWORK_OBJECT_GROUP("object-group network"),
  PREFIX_LIST("ipv4 prefix-list"),
  PREFIX6_LIST("ipv6 prefix-list"),
  ROUTE_MAP("route-map"),
  ROUTE_MAP_CLAUSE("route-map-clause"),
  SERVICE_CLASS("cable service-class");

  private final String _description;

  CiscoStructureType(String description) {
    _description = description;
  }

  @Override
  public String getDescription() {
    return _description;
  }
}

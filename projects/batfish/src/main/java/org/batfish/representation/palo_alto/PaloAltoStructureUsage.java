package org.batfish.representation.palo_alto;

import org.batfish.vendor.StructureUsage;

public enum PaloAltoStructureUsage implements StructureUsage {
  ADDRESS_GROUP_STATIC("address-group static"),
  APPLICATION_GROUP_MEMBERS("application-group members"),
  BGP_PEER_LOCAL_ADDRESS_INTERFACE("bgp peer local-address interface"),
  IMPORT_INTERFACE("import network interface"),
  LAYER2_INTERFACE_ZONE("zone network layer2"),
  LAYER3_INTERFACE_ZONE("zone network layer3"),
  NAT_RULE_DESTINATION("rulebase nat rules destination"),
  NAT_RULE_FROM_ZONE("rulebase nat rules from"),
  NAT_RULE_SOURCE("rulebase nat rules source"),
  NAT_RULE_SELF_REF("rulebase nat rules"),
  NAT_RULE_TO_ZONE("rulebase nat rules to"),
  SECURITY_RULE_APPLICATION("rulebase security rules application"),
  SECURITY_RULE_DESTINATION("rulebase security rules destination"),
  SECURITY_RULE_FROM_ZONE("rulebase security rules from"),
  SECURITY_RULE_SOURCE("rulebase security rules source"),
  SECURITY_RULE_TO_ZONE("rulebase security rules to"),
  SECURITY_RULE_SERVICE("rulebase security rules service"),
  SECURITY_RULE_SELF_REF("rulebase security rules"),
  SERVICE_GROUP_MEMBER("service-group members"),
  STATIC_ROUTE_INTERFACE("static-route interface"),
  STATIC_ROUTE_NEXT_VR("static-route nexthop next-vr"),
  TAP_INTERFACE_ZONE("zone network tap"),
  VIRTUAL_ROUTER_INTERFACE("virtual-router interface"),
  VIRTUAL_ROUTER_SELF_REFERENCE("virtual-router self-reference"),
  VIRTUAL_WIRE_INTERFACE_ZONE("zone network virtual-wire"),
  VSYS_IMPORT_INTERFACE("vsys import interface"),
  ZONE_INTERFACE("zone network layer3");

  private final String _description;

  PaloAltoStructureUsage(String description) {
    _description = description;
  }

  @Override
  public String getDescription() {
    return _description;
  }
}

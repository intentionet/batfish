package org.batfish.representation.palo_alto;

import org.batfish.vendor.StructureUsage;

public enum PaloAltoStructureUsage implements StructureUsage {
  ADDRESS_GROUP_STATIC("address-group static"),
  RULE_APPLICATION("rulebase security rules application"),
  RULE_DESTINATION("rulebase security rules destination"),
  RULE_FROM_ZONE("rulebase security rules from"),
  RULE_SELF_REF("rulebase security rules"),
  RULE_SOURCE("rulebase security rules source"),
  RULE_TO_ZONE("rulebase security rules to"),
  RULEBASE_SERVICE("rulebase security rules service"),
  SERVICE_GROUP_MEMBER("service-group members"),
  STATIC_ROUTE_INTERFACE("static-route interface"),
  VIRTUAL_ROUTER_INTERFACE("virtual-router interface"),
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

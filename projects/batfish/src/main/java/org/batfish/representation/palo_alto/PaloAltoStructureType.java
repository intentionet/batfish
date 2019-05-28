package org.batfish.representation.palo_alto;

import org.batfish.vendor.StructureType;

public enum PaloAltoStructureType implements StructureType {
  ADDRESS_GROUP("address-group"),
  ADDRESS_GROUP_OR_ADDRESS_OBJECT("address-group or address object"),
  // Special abstract structure type to handle the fact that some things that look like references
  // may actually refer to constants like an IP addresses
  ADDRESS_GROUP_OR_ADDRESS_OBJECT_OR_NONE("address-group or address object or none"),
  ADDRESS_OBJECT("address object"),
  APPLICATION("application"),
  APPLICATION_GROUP("application-group"),
  APPLICATION_GROUP_OR_APPLICATION("application-group or application"),
  GLOBAL_PROTECT_APP_CRYPTO_PROFILE("global-protect-app-crypto-profile"),
  IKE_CRYPTO_PROFILE("ike-crypto-profile"),
  IPSEC_CRYPTO_PROFILE("ipsec-crypto-profile"),
  INTERFACE("interface"),
  RULE("rule"),
  SERVICE("service"),
  SERVICE_GROUP("service-group"),
  SERVICE_OR_SERVICE_GROUP("service or service-group"),
  SERVICE_OR_SERVICE_GROUP_OR_NONE("service or service-group or none"),
  ZONE("zone");

  private final String _description;

  PaloAltoStructureType(String description) {
    _description = description;
  }

  @Override
  public String getDescription() {
    return _description;
  }
}

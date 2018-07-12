package org.batfish.representation.palo_alto;

import org.batfish.vendor.StructureType;

public enum PaloAltoStructureType implements StructureType {
  INTERFACE("interface"),
  SERVICE("service"),
  SERVICE_GROUP("service-group"),
  SERVICE_OR_SERVICE_GROUP("service or service-group"),
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

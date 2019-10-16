package org.batfish.representation.palo_alto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/** Represents a rulebase within a {@link Vsys} or in panorama policy */
public class Rulebase implements Serializable {

  // Note: these are LinkedHashMaps to preserve insertion order.
  @Nonnull private final Map<String, NatRule> _natRules;
  @Nonnull private final Map<String, SecurityRule> _securityRules;

  public Rulebase() {
    _natRules = new LinkedHashMap<>();
    _securityRules = new LinkedHashMap<>();
  }

  @Nonnull
  public Map<String, NatRule> getNatRules() {
    return _natRules;
  }

  @Nonnull
  public Map<String, SecurityRule> getSecurityRules() {
    return _securityRules;
  }
}

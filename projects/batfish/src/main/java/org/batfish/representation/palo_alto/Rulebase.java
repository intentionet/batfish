package org.batfish.representation.palo_alto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Rulebase implements Serializable {

  // Note: these are LinkedHashMaps to preserve insertion order.
  @Nonnull private final LinkedHashMap<String, NatRule> _natRules;
  @Nonnull private final LinkedHashMap<String, SecurityRule> _securityRules;

  public Rulebase() {
    _natRules = new LinkedHashMap<>();
    _securityRules = new LinkedHashMap<>();
  }

  @Nonnull
  public LinkedHashMap<String, NatRule> getNatRules() {
    return _natRules;
  }

  @Nonnull
  public LinkedHashMap<String, SecurityRule> getSecurityRules() {
    return _securityRules;
  }
}

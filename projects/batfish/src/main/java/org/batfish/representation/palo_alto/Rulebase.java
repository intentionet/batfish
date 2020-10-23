package org.batfish.representation.palo_alto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.batfish.datamodel.collections.InsertOrderedMap;

/** Represents a rulebase within a {@link Vsys} or in panorama policy */
public class Rulebase implements Serializable {

  // Note: these are LinkedHashMaps to preserve insertion order.
  @Nonnull private final Map<String, NatRule> _natRules;
  @Nonnull private final Map<String, SecurityRule> _securityRules;

  public Rulebase() {
    _natRules = new LinkedHashMap<>();
    _securityRules = new InsertOrderedMap<>();
  }

  /** Get map of {@code NatRule} name to {@code NatRule}; preserves insertion order. */
  @Nonnull
  public Map<String, NatRule> getNatRules() {
    return _natRules;
  }

  /** Get map of {@code SecurityRule} name to {@code SecurityRule}; preserves insertion order. */
  @Nonnull
  public Map<String, SecurityRule> getSecurityRules() {
    return _securityRules;
  }
}

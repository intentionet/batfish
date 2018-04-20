package org.batfish.representation.cisco.nx;

import java.io.Serializable;

/**
 * Represents the global BGP configuration for Cisco NX-OS.
 *
 * <p>Configuration commands entered at the {@code config-router} level that cannot also be run in a
 * {@code config-router-vrf} level are global to the BGP configuration at the device level.
 */
public final class CiscoNxBgpGlobalConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public CiscoNxBgpGlobalConfiguration() {
    this._enforceFirstAs = false; // disabled by default
  }

  public boolean getEnforceFirstAs() {
    return _enforceFirstAs;
  }

  public void setEnforceFirstAs(boolean enforceFirstAs) {
    this._enforceFirstAs = enforceFirstAs;
  }

  private boolean _enforceFirstAs;
}

package org.batfish.representation.cumulus;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Configuration;

/** Class for ospf process */
public class OspfProcess implements Serializable {

  private final @Nonnull OspfVrf _defaultVrf;
  private boolean _defaultPassiveInterface;
  private final @Nonnull Map<String, OspfVrf> _vrfs;
  public static final double DEFAULT_REFERENCE_BANDWIDTH =
      100E9D; // https://docs.cumulusnetworks.com/cumulus-linux/Layer-3/Open-Shortest-Path-First-OSPF/#auto-cost-reference-bandwidth
  public static String DEFAULT_OSPF_PROCESS_NAME = "default";

  private Map<CumulusRoutingProtocol, RedistributionPolicy> _redistributionPolicies;
  private @Nullable Boolean _maxMetricRouterLsa;

  public OspfProcess() {
    _defaultVrf = new OspfVrf(Configuration.DEFAULT_VRF_NAME);
    // default value
    _defaultPassiveInterface = false;
    _vrfs = new HashMap<>();
    _redistributionPolicies = new EnumMap<>(CumulusRoutingProtocol.class);
  }

  public @Nonnull OspfVrf getDefaultVrf() {
    return _defaultVrf;
  }

  public @Nonnull Map<String, OspfVrf> getVrfs() {
    return _vrfs;
  }

  public boolean getDefaultPassiveInterface() {
    return _defaultPassiveInterface;
  }

  public void setDefaultPassiveInterface(boolean defaultPassiveInterface) {
    _defaultPassiveInterface = defaultPassiveInterface;
  }

  public Map<CumulusRoutingProtocol, RedistributionPolicy> getRedistributionPolicies() {
    return _redistributionPolicies;
  }

  public Boolean getMaxMetricRouterLsa() {
    return _maxMetricRouterLsa;
  }

  public void setMaxMetricRouterLsa(Boolean maxMetricRouterLsa) {
    _maxMetricRouterLsa = maxMetricRouterLsa;
  }
}

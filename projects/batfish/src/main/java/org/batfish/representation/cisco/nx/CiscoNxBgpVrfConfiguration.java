package org.batfish.representation.cisco.nx;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;

/**
 * Represents the top-level configuration of a VRF in a BGP process for Cisco NX-OS.
 *
 * <p>Configuration commands that can be entered at the {@code config-router} (for default VRF) or
 * {@code config-router-vrf} levels are VRF-specific BGP configuration.
 */
public final class CiscoNxBgpVrfConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public CiscoNxBgpVrfConfiguration() {
    this._addressFamilies = new TreeMap<>(); // all address families disabled by default
    this._clusterId = null; // route reflection is disabled by default.
    this._logNeighborChanges = false; // disabled by default
    this._maxAsLimit = null; // default no limit
    this._routerId = null; // use device's default router id unless overridden.
  }

  @Nullable
  public CiscoNxBgpVrfAddressFamilyConfiguration getIpv4UnicastAddressFamily() {
    return _addressFamilies.get("ipv4-unicast");
  }

  @Nullable
  public CiscoNxBgpVrfAddressFamilyConfiguration getIpv6UnicastAddressFamily() {
    return _addressFamilies.get("ipv6-unicast");
  }

  public CiscoNxBgpVrfAddressFamilyConfiguration getOrCreateAddressFamily(String af) {
    return _addressFamilies.computeIfAbsent(af, a -> new CiscoNxBgpVrfAddressFamilyConfiguration());
  }

  public boolean getBestpathAlwaysCompareMed() {
    return _bestpathAlwaysCompareMed;
  }

  public void setBestpathAlwaysCompareMed(boolean bestpathAlwaysCompareMed) {
    this._bestpathAlwaysCompareMed = bestpathAlwaysCompareMed;
  }

  public boolean getBestpathAsPathMultipathRelax() {
    return _bestpathAsPathMultipathRelax;
  }

  public void setBestpathAsPathMultipathRelax(boolean bestpathAsPathMultipathRelax) {
    this._bestpathAsPathMultipathRelax = bestpathAsPathMultipathRelax;
  }

  public boolean getBestpathCompareRouterId() {
    return _bestpathCompareRouterId;
  }

  public void setBestpathCompareRouterId(boolean bestpathCompareRouterId) {
    this._bestpathCompareRouterId = bestpathCompareRouterId;
  }

  public boolean getBestpathCostCommunityIgnore() {
    return _bestpathCostCommunityIgnore;
  }

  public void setBestpathCostCommunityIgnore(boolean bestpathCostCommunityIgnore) {
    this._bestpathCostCommunityIgnore = bestpathCostCommunityIgnore;
  }

  public boolean getBestpathMedConfed() {
    return _bestpathMedConfed;
  }

  public void setBestpathMedConfed(boolean bestpathMedConfed) {
    this._bestpathMedConfed = bestpathMedConfed;
  }

  public boolean getBestpathMedMissingAsWorst() {
    return _bestpathMedMissingAsWorst;
  }

  public void setBestpathMedMissingAsWorst(boolean bestpathMedMissingAsWorst) {
    this._bestpathMedMissingAsWorst = bestpathMedMissingAsWorst;
  }

  public boolean getBestpathMedNonDeterministic() {
    return _bestpathMedNonDeterministic;
  }

  public void setBestpathMedNonDeterministic(boolean bestpathMedNonDeterministic) {
    this._bestpathMedNonDeterministic = bestpathMedNonDeterministic;
  }

  @Nullable
  public Long getLocalAs() {
    return _localAs;
  }

  public void setLocalAs(long localAs) {
    _localAs = localAs;
  }

  @Nullable
  public Ip getClusterId() {
    return _clusterId;
  }

  public void setClusterId(Ip clusterId) {
    _clusterId = clusterId;
  }

  public boolean getLogNeighborChanges() {
    return _logNeighborChanges;
  }

  public void setLogNeighborChanges(boolean log) {
    _logNeighborChanges = log;
  }

  @Nullable
  public Integer getMaxasLimit() {
    return _maxAsLimit;
  }

  public void setMaxasLimit(@Nullable Integer maxAsLimit) {
    this._maxAsLimit = maxAsLimit;
  }

  @Nullable
  public Ip getRouterId() {
    return _routerId;
  }

  public void setRouterId(Ip routerId) {
    _routerId = routerId;
  }

  private final Map<String, CiscoNxBgpVrfAddressFamilyConfiguration> _addressFamilies;
  private boolean _bestpathAlwaysCompareMed;
  private boolean _bestpathAsPathMultipathRelax;
  private boolean _bestpathCompareRouterId;
  private boolean _bestpathCostCommunityIgnore;
  private boolean _bestpathMedConfed;
  private boolean _bestpathMedMissingAsWorst;
  private boolean _bestpathMedNonDeterministic;
  @Nullable private Long _localAs;
  private boolean _logNeighborChanges;
  @Nullable private Ip _clusterId;
  @Nullable private Integer _maxAsLimit;
  @Nullable private Ip _routerId;
}

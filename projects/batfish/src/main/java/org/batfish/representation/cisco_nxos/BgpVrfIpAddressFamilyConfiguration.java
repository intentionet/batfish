package org.batfish.representation.cisco_nxos;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Represents the BGP configuration for IPv4 or IPv6 address families at the VRF level.
 *
 * <p>Child classes such as {@link BgpVrfIpv4AddressFamilyConfiguration} contain the v4/v6-specific
 * code.
 */
public abstract class BgpVrfIpAddressFamilyConfiguration extends BgpVrfAddressFamilyConfiguration {

  public BgpVrfIpAddressFamilyConfiguration() {
    _clientToClientReflection = false; // disabled by default
    _defaultMetric = null; // there is no default, and metric must be set to redistribute routes
    _defaultInformationOriginate = false; // disabled by default
    _distanceEbgp = 20; // documented under "address-family (BGP router)" and NX-OS CLI
    _distanceIbgp = 200; // documented under "address-family (BGP router)" and NX-OS CLI
    _distanceLocal = 220; // documented under "address-family (BGP router)" and NX-OS CLI
    _maximumPathsEbgp = 1; // multipath disabled by default
    _maximumPathsIbgp = 1; // multipath disabled by default
    _redistributionPolicies = new HashMap<>();
    _suppressInactive = false; // inactive routes not suppressed by default
  }

  public final boolean getClientToClientReflection() {
    return _clientToClientReflection;
  }

  public final void setClientToClientReflection(boolean clientToClientReflection) {
    this._clientToClientReflection = clientToClientReflection;
  }

  public final boolean getDefaultInformationOriginate() {
    return _defaultInformationOriginate;
  }

  public final void setDefaultInformationOriginate(boolean defaultInformationOriginate) {
    this._defaultInformationOriginate = defaultInformationOriginate;
  }

  @Nullable
  public final Long getDefaultMetric() {
    return _defaultMetric;
  }

  public final void setDefaultMetric(@Nullable Long defaultMetric) {
    this._defaultMetric = defaultMetric;
  }

  public final int getDistanceEbgp() {
    return _distanceEbgp;
  }

  public final void setDistanceEbgp(int distanceEbgp) {
    this._distanceEbgp = distanceEbgp;
  }

  public final int getDistanceIbgp() {
    return _distanceIbgp;
  }

  public final void setDistanceIbgp(int distanceIbgp) {
    this._distanceIbgp = distanceIbgp;
  }

  public final int getDistanceLocal() {
    return _distanceLocal;
  }

  public final void setDistanceLocal(int distanceLocal) {
    this._distanceLocal = distanceLocal;
  }

  public final int getMaximumPathsEbgp() {
    return _maximumPathsEbgp;
  }

  public final void setMaximumPathsEbgp(int maximumPathsEbgp) {
    this._maximumPathsEbgp = maximumPathsEbgp;
  }

  public final int getMaximumPathsIbgp() {
    return _maximumPathsIbgp;
  }

  public final void setMaximumPathsIbgp(int maximumPathsIbgp) {
    this._maximumPathsIbgp = maximumPathsIbgp;
  }

  @Nullable
  public final BgpRedistributionPolicy getRedistributionPolicy(NxosRoutingProtocol protocol) {
    return _redistributionPolicies.get(protocol);
  }

  public final void setRedistributionPolicy(
      NxosRoutingProtocol protocol, String routeMap, @Nullable String sourceTag) {
    BgpRedistributionPolicy policy = new BgpRedistributionPolicy(routeMap, sourceTag);
    _redistributionPolicies.put(protocol, policy);
  }

  public final boolean getSuppressInactive() {
    return _suppressInactive;
  }

  public final void setSuppressInactive(boolean suppressInactive) {
    _suppressInactive = suppressInactive;
  }

  private boolean _clientToClientReflection;
  @Nullable private Long _defaultMetric;
  private boolean _defaultInformationOriginate;
  private int _distanceEbgp;
  private int _distanceIbgp;
  private int _distanceLocal;
  private int _maximumPathsEbgp;
  private int _maximumPathsIbgp;
  private final Map<NxosRoutingProtocol, BgpRedistributionPolicy> _redistributionPolicies;
  private boolean _suppressInactive;
}

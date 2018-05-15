package org.batfish.representation.cisco.nx;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.RoutingProtocol;

/**
 * Represents the BGP configuration for a single address family at the VRF level.
 *
 * <p>Configuration commands entered at the CLI {@code config-router-af} or {@code
 * config-router-vrf-af} levels.
 */
public class CiscoNxBgpVrfAddressFamilyConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public CiscoNxBgpVrfAddressFamilyConfiguration() {
    _aggregateNetworks = new HashMap<>();
    _aggregateNetworks6 = new HashMap<>();
    _clientToClientReflection = false; // disabled by default
    _defaultMetric = null; // there is no default, and metric must be set to redistribute routes
    _defaultInformationOriginate = false; // disabled by default
    _distanceEbgp = 20; // documented under "address-family (BGP router)" and NX-OS CLI
    _distanceIbgp = 200; // documented under "address-family (BGP router)" and NX-OS CLI
    _distanceLocal = 220; // documented under "address-family (BGP router)" and NX-OS CLI
    _ipNetworks = new HashMap<>();
    _ipv6Networks = new HashMap<>();
    _maximumPathsEbgp = 1; // multipath disabled by default
    _maximumPathsIbgp = 1; // multipath disabled by default
    _redistributionPolicies = new HashMap<>();
    _suppressInactive = false; // inactive routes not suppressed by default
  }

  public Map<Prefix, CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration>
      getAggregateNetworks() {
    return Collections.unmodifiableMap(_aggregateNetworks);
  }

  public CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration getOrCreateAggregateNetwork(
      Prefix prefix) {
    return _aggregateNetworks.computeIfAbsent(
        prefix, p -> new CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration());
  }

  public CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration getOrCreateAggregateNetwork(
      Prefix6 prefix) {
    return _aggregateNetworks6.computeIfAbsent(
        prefix, p -> new CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration());
  }

  public boolean getClientToClientReflection() {
    return _clientToClientReflection;
  }

  public void setClientToClientReflection(boolean clientToClientReflection) {
    this._clientToClientReflection = clientToClientReflection;
  }

  public boolean getDefaultInformationOriginate() {
    return _defaultInformationOriginate;
  }

  public void setDefaultInformationOriginate(boolean defaultInformationOriginate) {
    this._defaultInformationOriginate = defaultInformationOriginate;
  }

  @Nullable
  public Long getDefaultMetric() {
    return _defaultMetric;
  }

  public void setDefaultMetric(@Nullable Long defaultMetric) {
    this._defaultMetric = defaultMetric;
  }

  public int getDistanceEbgp() {
    return _distanceEbgp;
  }

  public void setDistanceEbgp(int distanceEbgp) {
    this._distanceEbgp = distanceEbgp;
  }

  public int getDistanceIbgp() {
    return _distanceIbgp;
  }

  public void setDistanceIbgp(int distanceIbgp) {
    this._distanceIbgp = distanceIbgp;
  }

  public int getDistanceLocal() {
    return _distanceLocal;
  }

  public void setDistanceLocal(int distanceLocal) {
    this._distanceLocal = distanceLocal;
  }

  public Map<Prefix, String> getIpNetworks() {
    return Collections.unmodifiableMap(_ipNetworks);
  }

  public void addIpNetwork(Prefix prefix, String routeMapNameOrEmpty) {
    _ipNetworks.put(prefix, routeMapNameOrEmpty);
  }

  public Map<Prefix6, String> getIpv6Networks() {
    return Collections.unmodifiableMap(_ipv6Networks);
  }

  public void addIpv6Network(Prefix6 prefix, String routeMapNameOrEmpty) {
    _ipv6Networks.put(prefix, routeMapNameOrEmpty);
  }

  public int getMaximumPathsEbgp() {
    return _maximumPathsEbgp;
  }

  public void setMaximumPathsEbgp(int maximumPathsEbgp) {
    this._maximumPathsEbgp = maximumPathsEbgp;
  }

  public int getMaximumPathsIbgp() {
    return _maximumPathsIbgp;
  }

  public void setMaximumPathsIbgp(int maximumPathsIbgp) {
    this._maximumPathsIbgp = maximumPathsIbgp;
  }

  @Nullable
  public CiscoNxBgpRedistributionPolicy getRedistributionPolicy(RoutingProtocol protocol) {
    return _redistributionPolicies.get(protocol);
  }

  public void setRedistributionPolicy(
      RoutingProtocol protocol, String routeMap, @Nullable String sourceTag) {
    CiscoNxBgpRedistributionPolicy policy = new CiscoNxBgpRedistributionPolicy(routeMap);
    policy.setSourceTag(sourceTag);
    _redistributionPolicies.put(protocol, policy);
  }

  public boolean getSuppressInactive() {
    return _suppressInactive;
  }

  public void setSuppressInactive(boolean suppressInactive) {
    _suppressInactive = suppressInactive;
  }

  private final Map<Prefix, CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration>
      _aggregateNetworks;
  private final Map<Prefix6, CiscoNxBgpVrfAddressFamilyAggregateNetworkConfiguration>
      _aggregateNetworks6;
  private boolean _clientToClientReflection;
  @Nullable private Long _defaultMetric;
  private boolean _defaultInformationOriginate;
  private int _distanceEbgp;
  private int _distanceIbgp;
  private int _distanceLocal;
  private final Map<Prefix, String> _ipNetworks;
  private final Map<Prefix6, String> _ipv6Networks;
  private int _maximumPathsEbgp;
  private int _maximumPathsIbgp;
  private final Map<RoutingProtocol, CiscoNxBgpRedistributionPolicy> _redistributionPolicies;
  private boolean _suppressInactive;
}

package org.batfish.representation.cisco.nx;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;

/**
 * Represents the BGP configuration for a single address family at the VRF level.
 *
 * <p>Configuration commands entered at the CLI {@code config-router-af} or {@code
 * config-router-vrf-af} levels.
 */
public class CiscoNxBgpVrfAddressFamilyConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public CiscoNxBgpVrfAddressFamilyConfiguration() {
    _clientToClientReflection = false; // disabled by default
    _defaultMetric = null; // there is no default, and metric must be set to redistribute routes
    _defaultOriginate = false; // disabled by default
    _distanceEbgp = 20; // documented under "address-family (BGP router)" and NX-OS CLI
    _distanceIbgp = 200; // documented under "address-family (BGP router)" and NX-OS CLI
    _distanceLocal = 220; // documented under "address-family (BGP router)" and NX-OS CLI
    _ipNetworks = new TreeMap<>();
    _ipv6Networks = new TreeMap<>();
    _maximumPathsEbgp = 1; // multipath disabled by default
    _maximumPathsIbgp = 1; // multipath disabled by default
  }

  public boolean getClientToClientReflection() {
    return _clientToClientReflection;
  }

  public void setClientToClientReflection(boolean clientToClientReflection) {
    this._clientToClientReflection = clientToClientReflection;
  }

  @Nullable
  public Long getDefaultMetric() {
    return _defaultMetric;
  }

  public void setDefaultMetric(@Nullable Long defaultMetric) {
    this._defaultMetric = defaultMetric;
  }

  public boolean getDefaultOriginate() {
    return _defaultOriginate;
  }

  public void setDefaultOriginate(boolean defaultOriginate) {
    this._defaultOriginate = defaultOriginate;
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

  public void addIpNetwork(Prefix prefix, String routeMapNameOrEmpty) {
    _ipNetworks.put(prefix, routeMapNameOrEmpty);
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

  private boolean _clientToClientReflection;
  @Nullable private Long _defaultMetric;
  private boolean _defaultOriginate;
  private int _distanceEbgp;
  private int _distanceIbgp;
  private int _distanceLocal;
  private Map<Prefix, String> _ipNetworks;
  private Map<Prefix6, String> _ipv6Networks;
  private int _maximumPathsEbgp;
  private int _maximumPathsIbgp;
}

package org.batfish.representation.cisco.eos;

import javax.annotation.Nullable;

/**
 * Per neighbor settings for IPv4 unicast address family. See also {@link
 * AristaBgpNeighborAddressFamily}
 */
public final class AristaBgpNeighborIpv4UnicastAddressFamily
    extends AristaBgpNeighborAddressFamily {

  @Nullable private AristaBgpDefaultOriginate _defaultOriginate;
  @Nullable private String _prefixListIn;
  @Nullable private String _prefixListOut;
  // Route Map in/out are inherited
  @Nullable private Integer _weight;

  public void inherit(AristaBgpVrfIpv4UnicastAddressFamily af) {
    if (_additionalPaths != null) {
      _additionalPaths = af.getAdditionalPaths();
    }
    if (_nextHopUnchanged != null) {
      _nextHopUnchanged = af.getNextHopUnchanged();
    }
  }

  @Nullable
  public AristaBgpDefaultOriginate getDefaultOriginate() {
    return _defaultOriginate;
  }

  public void setDefaultOriginate(@Nullable AristaBgpDefaultOriginate defaultOriginate) {
    _defaultOriginate = defaultOriginate;
  }

  @Nullable
  public String getPrefixListIn() {
    return _prefixListIn;
  }

  public void setPrefixListIn(@Nullable String prefixListIn) {
    _prefixListIn = prefixListIn;
  }

  @Nullable
  public String getPrefixListOut() {
    return _prefixListOut;
  }

  public void setPrefixListOut(@Nullable String prefixListOut) {
    _prefixListOut = prefixListOut;
  }

  @Override
  @Nullable
  public String getRouteMapIn() {
    return _routeMapIn;
  }

  @Override
  public void setRouteMapIn(@Nullable String routeMapIn) {
    _routeMapIn = routeMapIn;
  }

  @Override
  @Nullable
  public String getRouteMapOut() {
    return _routeMapOut;
  }

  @Override
  public void setRouteMapOut(@Nullable String routeMapOut) {
    _routeMapOut = routeMapOut;
  }

  @Override
  @Nullable
  public Integer getWeight() {
    return _weight;
  }

  @Override
  public void setWeight(@Nullable Integer weight) {
    _weight = weight;
  }
}

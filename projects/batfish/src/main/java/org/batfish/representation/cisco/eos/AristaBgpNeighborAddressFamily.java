package org.batfish.representation.cisco.eos;

import java.io.Serializable;
import javax.annotation.Nullable;

/** Per-neighbor settings, common to all address families */
public abstract class AristaBgpNeighborAddressFamily implements Serializable {
  @Nullable protected Boolean _activate;
  @Nullable protected AristaBgpAdditionalPathsConfig _additionalPaths;
  @Nullable protected Boolean _nextHopUnchanged;
  @Nullable protected String _routeMapIn;
  @Nullable protected String _routeMapOut;
  @Nullable protected Integer _weight;

  @Nullable
  public Boolean getActivate() {
    return _activate;
  }

  public void setActivate(@Nullable Boolean activate) {
    _activate = activate;
  }

  @Nullable
  public AristaBgpAdditionalPathsConfig getAdditionalPaths() {
    return _additionalPaths;
  }

  public void setAdditionalPaths(@Nullable AristaBgpAdditionalPathsConfig additionalPaths) {
    _additionalPaths = additionalPaths;
  }

  @Nullable
  public Boolean getNextHopUnchanged() {
    return _nextHopUnchanged;
  }

  public void setNextHopUnchanged(@Nullable Boolean nextHopUnchanged) {
    _nextHopUnchanged = nextHopUnchanged;
  }

  @Nullable
  public String getRouteMapIn() {
    return _routeMapIn;
  }

  public void setRouteMapIn(@Nullable String routeMapIn) {
    _routeMapIn = routeMapIn;
  }

  @Nullable
  public String getRouteMapOut() {
    return _routeMapOut;
  }

  public void setRouteMapOut(@Nullable String routeMapOut) {
    _routeMapOut = routeMapOut;
  }

  @Nullable
  public Integer getWeight() {
    return _weight;
  }

  public void setWeight(@Nullable Integer weight) {
    _weight = weight;
  }
}

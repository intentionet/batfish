package org.batfish.representation.cisco_nxos;

import javax.annotation.Nullable;

public class OspfAreaNssa implements OspfAreaTypeSettings {

  @Override
  public <T> T accept(OspfAreaTypeSettingsVisitor<T> visitor) {
    return visitor.visitOspfAreaNssa(this);
  }

  public @Nullable OspfDefaultOriginate getDefaultOriginate() {
    return _defaultOriginate;
  }

  public void setDefaultOriginate(@Nullable OspfDefaultOriginate defaultOriginate) {
    _defaultOriginate = defaultOriginate;
  }

  public boolean getNoRedistribution() {
    return _noRedistribution;
  }

  public void setNoRedistribution(boolean noRedistribution) {
    _noRedistribution = noRedistribution;
  }

  public boolean getNoSummary() {
    return _noSummary;
  }

  public void setNoSummary(boolean noSummary) {
    _noSummary = noSummary;
  }

  public @Nullable String getRouteMap() {
    return _routeMap;
  }

  public void setRouteMap(@Nullable String routeMap) {
    _routeMap = routeMap;
  }

  //////////////////////////////////////////
  ///// Private implementation details /////
  //////////////////////////////////////////

  private @Nullable OspfDefaultOriginate _defaultOriginate;
  private boolean _noRedistribution;
  private boolean _noSummary;
  private @Nullable String _routeMap;
}

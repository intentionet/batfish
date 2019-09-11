package org.batfish.representation.cisco.eos;

import javax.annotation.Nullable;

/** Configuration for origination of default routes */
public final class AristaBgpDefaultOriginate {
  @Nullable private final Boolean _always;
  @Nullable private final String _routeMap;

  public AristaBgpDefaultOriginate(@Nullable Boolean always, @Nullable String routeMap) {
    _always = always;
    _routeMap = routeMap;
  }

  @Nullable
  public Boolean getAlways() {
    return _always;
  }

  @Nullable
  public String getRouteMap() {
    return _routeMap;
  }
}

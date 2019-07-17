package org.batfish.representation.cisco_nxos;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.bgp.RouteDistinguisher;

/** Either {@code auto} or an explicit {@link RouteDistinguisher}. */
public final class RouteDistinguisherOrAuto implements Serializable {

  public static RouteDistinguisherOrAuto auto() {
    return new RouteDistinguisherOrAuto(null);
  }

  public static RouteDistinguisherOrAuto of(@Nonnull RouteDistinguisher rd) {
    return new RouteDistinguisherOrAuto(rd);
  }

  public boolean isAuto() {
    return _routeDistinguisher == null;
  }

  @Nullable
  public RouteDistinguisher getRouteDistinguisher() {
    return _routeDistinguisher;
  }

  //////////////////////////////////////////
  ///// Private implementation details /////
  //////////////////////////////////////////

  private RouteDistinguisherOrAuto(@Nullable RouteDistinguisher rd) {
    _routeDistinguisher = rd;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof RouteDistinguisherOrAuto)) {
      return false;
    }
    RouteDistinguisherOrAuto that = (RouteDistinguisherOrAuto) o;
    return Objects.equals(_routeDistinguisher, that._routeDistinguisher);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(_routeDistinguisher);
  }

  @Nullable private final RouteDistinguisher _routeDistinguisher;
}

package org.batfish.dataplane.rib;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.batfish.datamodel.AbstractRoute;

public class RouteAdvertisement<R extends AbstractRoute> {
  protected R _route;
  protected boolean _withdraw;
  public Reason _reason;

  public enum Reason {
    ADD,
    REPLACE,
    WITHDRAW
  }

  /**
   * Create a new route advertisement, optionally allowing this to be a withdrawal advertisement
   *
   * @param route route that is advertised
   * @param withdraw whether the route is being withdrawn
   */
  public RouteAdvertisement(@Nonnull R route, boolean withdraw, Reason reason) {
    _route = route;
    _withdraw = withdraw;
    _reason = reason;
  }

  /**
   * Create a new route advertisement
   *
   * @param route route that is advertised
   */
  public RouteAdvertisement(@Nonnull R route) {
    _route = route;
    _withdraw = false;
    _reason = Reason.ADD;
  }

  /** Get the underlying route that's being advertised (or withdrawn) */
  public R getRoute() {
    return _route;
  }

  public Reason getReason() {
    return _reason;
  }

  /**
   * Check if this route is being withdrawn
   *
   * @return true if the route is being withdrawn
   */
  public boolean isWithdrawn() {
    return _withdraw;
  }

  @Override
  public int hashCode() {
    return _route.hashCode() + Boolean.hashCode(_withdraw) + Objects.hash(_reason.ordinal());
  }

  @Override
  public boolean equals(Object obj) {
    return (this == obj)
        || (obj instanceof RouteAdvertisement
            && _withdraw == ((RouteAdvertisement<?>) obj)._withdraw
            && _reason == ((RouteAdvertisement<?>) obj)._reason
            && _route.equals(((RouteAdvertisement<?>) obj)._route));
  }

  @Override
  public String toString() {
    return _route + ", withdraw=" + _withdraw + ", reason=" + _reason;
  }
}

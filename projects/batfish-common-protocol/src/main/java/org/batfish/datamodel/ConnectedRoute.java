package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;

public class ConnectedRoute extends AbstractRoute {

  private static final long serialVersionUID = 1L;

  private final String _nextHopInterface;

  @JsonCreator
  public ConnectedRoute(
      @JsonProperty(PROP_NETWORK) Prefix network,
      @JsonProperty(PROP_NEXT_HOP_INTERFACE) String nextHopInterface) {
    super(network);
    _nextHopInterface = firstNonNull(nextHopInterface, Route.UNSET_NEXT_HOP_INTERFACE);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof ConnectedRoute)) {
      return false;
    }
    ConnectedRoute rhs = (ConnectedRoute) o;
    boolean res = _network.equals(rhs._network);
    return res && _nextHopInterface.equals(rhs._nextHopInterface);
  }

  @Override
  public int getAdministrativeCost() {
    return 0;
  }

  @Override
  public Long getMetric() {
    return 0L;
  }

  @Nonnull
  @JsonIgnore(false)
  @JsonProperty(PROP_NEXT_HOP_INTERFACE)
  @Override
  public String getNextHopInterface() {
    return _nextHopInterface;
  }

  @Nonnull
  @Override
  public Ip getNextHopIp() {
    return Route.UNSET_ROUTE_NEXT_HOP_IP;
  }

  @Override
  public RoutingProtocol getProtocol() {
    return RoutingProtocol.CONNECTED;
  }

  @Override
  public int getTag() {
    return NO_TAG;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _network.hashCode();
    result = prime * result + _nextHopInterface.hashCode();
    return result;
  }

  @Override
  protected String protocolRouteString() {
    return "";
  }

  @Override
  public int routeCompare(@Nonnull AbstractRoute rhs) {
    return 0;
  }
}

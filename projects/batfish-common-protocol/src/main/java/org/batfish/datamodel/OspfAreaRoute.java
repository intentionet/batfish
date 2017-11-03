package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;

public abstract class OspfAreaRoute extends OspfRoute {
  protected static final String PROP_AREA = "area";
  /** */
  private static final long serialVersionUID = 1L;

  protected final long _area;

  public OspfAreaRoute(
      Prefix network, Ip nextHopIp, int admin, long metric, @JsonProperty(PROP_AREA) long area) {
    super(network, nextHopIp, admin, metric);
    _area = area;
  }

  @JsonProperty(PROP_AREA)
  public long getArea() {
    return _area;
  }

  @Nonnull
  @Override
  public String getNextHopInterface() {
    return Route.UNSET_NEXT_HOP_INTERFACE;
  }

  @Override
  public abstract RoutingProtocol getProtocol();

  @Override
  public int getTag() {
    return NO_TAG;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _admin;
    result = prime * result + (int) (_area ^ (_area >>> 32));
    result = prime * result + Long.hashCode(_metric);
    result = prime * result + _network.hashCode();
    result = prime * result + (_nextHopIp == null ? 0 : _nextHopIp.hashCode());
    return result;
  }

  @Override
  protected final String protocolRouteString() {
    return " area:" + _area;
  }
}

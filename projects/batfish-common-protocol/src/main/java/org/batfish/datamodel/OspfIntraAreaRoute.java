package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OspfIntraAreaRoute extends OspfInternalRoute {

  private static final long serialVersionUID = 1L;

  @JsonCreator
  public OspfIntraAreaRoute(
      @JsonProperty(PROP_NETWORK) Prefix network,
      @Nullable @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int admin,
      @JsonProperty(PROP_METRIC) long metric,
      @JsonProperty(PROP_AREA) long area) {
    super(network, nextHopIp, admin, metric, area);
  }

  @Override
  public RoutingProtocol getProtocol() {
    return RoutingProtocol.OSPF;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof OspfIntraAreaRoute)) {
      return false;
    }
    OspfIntraAreaRoute other = (OspfIntraAreaRoute) o;
    if (_nextHopIp == null) {
      if (other._nextHopIp != null) {
        return false;
      }
    } else if (!_nextHopIp.equals(other._nextHopIp)) {
      return false;
    }
    if (_admin != other._admin) {
      return false;
    }
    if (_area != other._area) {
      return false;
    }
    if (_metric != other._metric) {
      return false;
    }
    return _network.equals(other._network);
  }

  @Override
  public int routeCompare(@Nonnull AbstractRoute rhs) {
    if (getClass() != rhs.getClass()) {
      return 0;
    }
    OspfIntraAreaRoute castRhs = (OspfIntraAreaRoute) rhs;
    int ret = Long.compare(_area, castRhs._area);
    return ret;
  }
}

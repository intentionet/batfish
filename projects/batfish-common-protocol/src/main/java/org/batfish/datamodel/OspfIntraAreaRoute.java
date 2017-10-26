package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;

public class OspfIntraAreaRoute extends OspfAreaRoute {

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

  @Override public RoutingProtocol getProtocol() {
    return RoutingProtocol.OSPF;
  }

  @Override
  public int routeCompare(AbstractRoute rhs) {
    if (getClass() != rhs.getClass()) {
      return 0;
    }
    OspfIntraAreaRoute castRhs = (OspfIntraAreaRoute) rhs;
    int ret = Long.compare(_area, castRhs._area);
    return ret;
  }
}

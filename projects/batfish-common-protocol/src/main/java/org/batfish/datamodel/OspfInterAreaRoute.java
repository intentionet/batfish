package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OspfInterAreaRoute extends OspfAreaRoute {

  private static final long serialVersionUID = 1L;

  @JsonCreator
  public OspfInterAreaRoute(
      @JsonProperty(PROP_NETWORK) Prefix network,
      @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int admin,
      @JsonProperty(PROP_METRIC) long metric,
      @JsonProperty(PROP_AREA) long area) {
    super(network, nextHopIp, admin, metric, area);
  }

  @Override
  public RoutingProtocol getProtocol() {
    return RoutingProtocol.OSPF_IA;
  }

  @Override
  public int routeCompare(AbstractRoute rhs) {
    if (getClass() != rhs.getClass()) {
      return 0;
    }
    OspfInterAreaRoute castRhs = (OspfInterAreaRoute) rhs;
    int ret = Long.compare(_area, castRhs._area);
    return ret;
  }
}

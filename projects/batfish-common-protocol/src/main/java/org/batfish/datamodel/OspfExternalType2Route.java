package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import org.batfish.datamodel.ospf.OspfMetricType;

public class OspfExternalType2Route extends OspfExternalRoute {

  /** */
  private static final long serialVersionUID = 1L;

  @JsonCreator
  public OspfExternalType2Route(
      @JsonProperty(PROP_NETWORK) Prefix network,
      @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int admin,
      @JsonProperty(PROP_METRIC) long metric,
      @JsonProperty(PROP_LSA_METRIC) long lsaMetric,
      @JsonProperty(PROP_AREA) long area,
      @JsonProperty(PROP_COST_TO_ADVERTISER) long costToAdvertiser,
      @JsonProperty(PROP_ADVERTISER) String advertiser) {
    super(network, nextHopIp, admin, metric, lsaMetric, area, advertiser, costToAdvertiser);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof OspfExternalType2Route)) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    OspfExternalType2Route other = (OspfExternalType2Route) obj;
    return getCostToAdvertiser() == other.getCostToAdvertiser();
  }

  @Override
  public OspfMetricType getOspfMetricType() {
    return OspfMetricType.E2;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Long.hashCode(getCostToAdvertiser());
    return result;
  }

  @Override
  protected final String ospfExternalRouteString() {
    return " costToAdvertiser:" + getCostToAdvertiser();
  }

  @Override
  public int routeCompare(@Nonnull AbstractRoute rhs) {
    if (getClass() != rhs.getClass()) {
      return 0;
    }
    OspfExternalType2Route castRhs = (OspfExternalType2Route) rhs;
    int ret = Long.compare(getCostToAdvertiser(), castRhs.getCostToAdvertiser());
    return ret;
  }
}

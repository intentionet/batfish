package org.batfish.representation.cisco;

import org.batfish.datamodel.OspfMetricType;
import org.batfish.datamodel.RoutingProtocol;

public class OspfRedistributionPolicy extends RedistributionPolicy {

  public static final String BGP_AS = "BGP_AS";

  public static final OspfMetricType DEFAULT_METRIC_TYPE = OspfMetricType.E2;

  private static final long serialVersionUID = 1L;

  private Long _metric;

  private OspfMetricType _metricType;

  private boolean _subnets;

  private Long _tag;

  public OspfRedistributionPolicy(RoutingProtocol sourceProtocol) {
    super(sourceProtocol, RoutingProtocol.OSPF);
  }

  public Long getMetric() {
    return _metric;
  }

  public OspfMetricType getMetricType() {
    return _metricType;
  }

  public boolean getSubnets() {
    return _subnets;
  }

  public Long getTag() {
    return _tag;
  }

  public void setMetric(long metric) {
    _metric = metric;
  }

  public void setOspfMetricType(OspfMetricType type) {
    _metricType = type;
  }

  public void setSubnets(boolean b) {
    _subnets = b;
  }

  public void setTag(long tag) {
    _tag = tag;
  }
}

package org.batfish.representation.cisco;

import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.isis.IsisLevel;

public class IsisRedistributionPolicy extends RedistributionPolicy {

  public static final IsisLevel DEFAULT_LEVEL = IsisLevel.LEVEL_2;

  public static final Integer DEFAULT_REDISTRIBUTE_CONNECTED_METRIC = 10;

  public static final Integer DEFAULT_REDISTRIBUTE_STATIC_METRIC = 10;

  /** */
  private static final long serialVersionUID = 1L;

  private IsisLevel _level;

  private String _map;

  private Long _metric;

  private Prefix _summaryPrefix;

  public IsisRedistributionPolicy(RoutingProtocol sourceProtocol) {
    super(sourceProtocol, RoutingProtocol.ISIS);
  }

  public IsisLevel getLevel() {
    return _level;
  }

  public String getMap() {
    return _map;
  }

  public Long getMetric() {
    return _metric;
  }

  public Prefix getSummaryPrefix() {
    return _summaryPrefix;
  }

  public void setLevel(IsisLevel level) {
    _level = level;
  }

  public void setMap(String map) {
    _map = map;
  }

  public void setMetric(long metric) {
    _metric = metric;
  }

  public void setSummaryPrefix(Prefix prefix) {
    _summaryPrefix = prefix;
  }
}

package org.batfish.representation.juniper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

public class StaticRoute implements Serializable {

  /** */
  private static final long serialVersionUID = 1L;

  /* https://www.juniper.net/documentation/en_US/junos/topics/reference/general/routing-protocols-default-route-preference-values.html */
  private static final int DEFAULT_ADMIN_DISTANCE = 5;

  private int _distance;

  private boolean _drop;

  private int _metric;

  private String _nextHopInterface;

  private Ip _nextHopIp;

  private List<String> _policies;

  private Prefix _prefix;

  private Integer _tag;

  private Boolean _noInstall;

  public StaticRoute(Prefix prefix) {
    _prefix = prefix;
    _policies = new ArrayList<>();
    // default admin costs for static routes in Juniper
    _distance = DEFAULT_ADMIN_DISTANCE;
  }

  public int getDistance() {
    return _distance;
  }

  public boolean getDrop() {
    return _drop;
  }

  public int getMetric() {
    return _metric;
  }

  public String getNextHopInterface() {
    return _nextHopInterface;
  }

  public Ip getNextHopIp() {
    return _nextHopIp;
  }

  @Nullable
  public Boolean getNoInstall() {
    return _noInstall;
  }

  public List<String> getPolicies() {
    return _policies;
  }

  public Prefix getPrefix() {
    return _prefix;
  }

  public Integer getTag() {
    return _tag;
  }

  public void setDistance(int distance) {
    _distance = distance;
  }

  public void setDrop(boolean drop) {
    _drop = true;
  }

  public void setMetric(int metric) {
    _metric = metric;
  }

  public void setNextHopInterface(String nextHopInterface) {
    _nextHopInterface = nextHopInterface;
  }

  public void setNextHopIp(Ip nextHopIp) {
    _nextHopIp = nextHopIp;
  }

  public void setNoInstall(@Nullable Boolean noInstall) {
    _noInstall = noInstall;
  }

  public void setTag(int tag) {
    _tag = tag;
  }
}

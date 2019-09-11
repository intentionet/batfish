package org.batfish.representation.cisco.eos;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;

/** BGP configuration for a single VRF */
public final class AristaBgpVrf implements Serializable {

  @Nullable private Long _defaultMetric;
  @Nullable private Integer _defaultEbgpAdminDistance;
  @Nullable private Integer _defaultIbgpAdminDistance;
  @Nullable private Integer _defaultLocalAdminDistance;
  @Nullable private ExtendedCommunity _exportRouteTarget;
  @Nullable private ExtendedCommunity _importRouteTarget;
  @Nullable private Integer _holdTimer;
  @Nullable private Integer _keepAliveTimer;
  @Nullable private Long _localAs;
  @Nonnull private final String _name;
  @Nullable private RouteDistinguisher _routeDistinguisher;
  @Nullable private Ip _routerId;
  @Nullable private Boolean _shutdown;
  @Nonnull private final Map<Prefix, AristaBgpAggregateNetwork> _v4aggregates;
  @Nonnull private final Map<Ip, AristaBgpV4Neighbor> _v4neighbors;
  @Nullable private AristaBgpVrfIpv4UnicastAddressFamily _v4unicastAf;
  @Nullable private AristaBgpVrfEvpnAddressFamily _evpnAf;

  public AristaBgpVrf(String name) {
    _name = name;
    _v4aggregates = new HashMap<>(0);
    _v4neighbors = new HashMap<>(0);
    // V4 unicast is enabled by default
    _v4unicastAf = new AristaBgpVrfIpv4UnicastAddressFamily();
  }

  @Nullable
  public Long getDefaultMetric() {
    return _defaultMetric;
  }

  public void setDefaultMetric(@Nullable Long defaultMetric) {
    _defaultMetric = defaultMetric;
  }

  @Nullable
  public Integer getDefaultEbgpAdminDistance() {
    return _defaultEbgpAdminDistance;
  }

  public void setDefaultEbgpAdminDistance(@Nullable Integer defaultEbgpAdminDistance) {
    _defaultEbgpAdminDistance = defaultEbgpAdminDistance;
  }

  @Nullable
  public Integer getDefaultIbgpAdminDistance() {
    return _defaultIbgpAdminDistance;
  }

  public void setDefaultIbgpAdminDistance(@Nullable Integer defaultIbgpAdminDistance) {
    _defaultIbgpAdminDistance = defaultIbgpAdminDistance;
  }

  @Nullable
  public Integer getDefaultLocalAdminDistance() {
    return _defaultLocalAdminDistance;
  }

  public void setDefaultLocalAdminDistance(@Nullable Integer defaultLocalAdminDistance) {
    _defaultLocalAdminDistance = defaultLocalAdminDistance;
  }

  @Nullable
  public ExtendedCommunity getExportRouteTarget() {
    return _exportRouteTarget;
  }

  public void setExportRouteTarget(@Nullable ExtendedCommunity exportRouteTarget) {
    _exportRouteTarget = exportRouteTarget;
  }

  @Nullable
  public AristaBgpVrfEvpnAddressFamily getEvpnAf() {
    return _evpnAf;
  }

  public void setEvpnAf(@Nullable AristaBgpVrfEvpnAddressFamily evpnAf) {
    _evpnAf = evpnAf;
  }

  @Nullable
  public ExtendedCommunity getImportRouteTarget() {
    return _importRouteTarget;
  }

  public void setImportRouteTarget(@Nullable ExtendedCommunity importRouteTarget) {
    _importRouteTarget = importRouteTarget;
  }

  /** Hold timer, in seconds */
  @Nullable
  public Integer getHoldTimer() {
    return _holdTimer;
  }

  /** Set the Hold timer, in seconds */
  public void setHoldTimer(@Nullable Integer holdTimer) {
    _holdTimer = holdTimer;
  }

  /** Keepalive timer, in seconds */
  @Nullable
  public Integer getKeepAliveTimer() {
    return _keepAliveTimer;
  }

  @Nonnull
  public String getName() {
    return _name;
  }

  /** Set the Keepalive timer, in seconds */
  public void setKeepAliveTimer(@Nullable Integer keepAliveTimer) {
    _keepAliveTimer = keepAliveTimer;
  }

  @Nullable
  public Long getLocalAs() {
    return _localAs;
  }

  public void setLocalAs(@Nullable Long localAs) {
    _localAs = localAs;
  }

  @Nullable
  public RouteDistinguisher getRouteDistinguisher() {
    return _routeDistinguisher;
  }

  public void setRouteDistinguisher(@Nullable RouteDistinguisher routeDistinguisher) {
    _routeDistinguisher = routeDistinguisher;
  }

  @Nullable
  public Ip getRouterId() {
    return _routerId;
  }

  public void setRouterId(@Nullable Ip routerId) {
    _routerId = routerId;
  }

  @Nullable
  public Boolean getShutdown() {
    return _shutdown;
  }

  public void setShutdown(@Nullable Boolean shutdown) {
    _shutdown = shutdown;
  }

  @Nonnull
  public Map<Prefix, AristaBgpAggregateNetwork> getV4aggregates() {
    return _v4aggregates;
  }

  @Nonnull
  public Map<Ip, AristaBgpV4Neighbor> getV4neighbors() {
    return _v4neighbors;
  }

  @Nullable
  public AristaBgpVrfIpv4UnicastAddressFamily getV4unicastAf() {
    return _v4unicastAf;
  }

  public void setV4unicastAf(@Nullable AristaBgpVrfIpv4UnicastAddressFamily v4unicastAf) {
    _v4unicastAf = v4unicastAf;
  }
}

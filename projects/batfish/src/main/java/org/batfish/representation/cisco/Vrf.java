package org.batfish.representation.cisco;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfConfiguration;

public final class Vrf implements Serializable {

  private static final long serialVersionUID = 1L;

  @Nonnull private final Map<Long, EigrpProcess> _eigrpProcesses;
  @Nullable private CiscoNxBgpVrfConfiguration _bgpNxConfig;
  @Nullable private BgpProcess _bgpProcess;
  @Nullable private String _description;
  @Nullable private IsisProcess _isisProcess;
  @Nonnull private final String _name;
  @Nonnull private Map<String, OspfProcess> _ospfProcesses;
  @Nullable private RipProcess _ripProcess;
  @Nullable private RouteDistinguisher _routeDistinguisher;
  @Nullable private ExtendedCommunity _routeExportTarget;
  @Nullable private ExtendedCommunity _routeImportTarget;
  private boolean _shutdown;
  @Nonnull private final Set<StaticRoute> _staticRoutes;
  @Nullable private Integer _vni;

  public Vrf(@Nonnull String name) {
    _eigrpProcesses = new TreeMap<>();
    _name = name;
    // Ensure that processes are in insertion order.
    _ospfProcesses = new LinkedHashMap<>(0);
    _staticRoutes = new HashSet<>();
  }

  @Nullable
  public CiscoNxBgpVrfConfiguration getBgpNxConfig() {
    return _bgpNxConfig;
  }

  @Nullable
  public BgpProcess getBgpProcess() {
    return _bgpProcess;
  }

  @Nullable
  public String getDescription() {
    return _description;
  }

  @Nonnull
  public Map<Long, EigrpProcess> getEigrpProcesses() {
    return _eigrpProcesses;
  }

  @Nullable
  public IsisProcess getIsisProcess() {
    return _isisProcess;
  }

  @Nonnull
  public String getName() {
    return _name;
  }

  /** Return OSPF processes defined on this VRF. Guaranteed to be in insertion order */
  @Nonnull
  public Map<String, OspfProcess> getOspfProcesses() {
    return _ospfProcesses;
  }

  @Nullable
  public RipProcess getRipProcess() {
    return _ripProcess;
  }

  /**
   * The route distinguisher to attach to VPN originating from this VRF. Will be {@code null} if it
   * must be auto-derived.
   */
  @Nullable
  public RouteDistinguisher getRouteDistinguisher() {
    return _routeDistinguisher;
  }

  /**
   * The route target value to attach to VPN routes originating from this VRF. Will be {@code null}
   * if it must be auto-derived.
   */
  @Nullable
  public ExtendedCommunity getRouteExportTarget() {
    return _routeExportTarget;
  }

  /**
   * Routes that contain this route target community should be merged into this VRF. Will be {@code
   * null} if it must be auto-derived.
   */
  @Nullable
  public ExtendedCommunity getRouteImportTarget() {
    return _routeImportTarget;
  }

  /** Is this VRF shutdown (not used for routing/forwarding) */
  public boolean isShutdown() {
    return _shutdown;
  }

  /** Layer 3 VNI number associated with this VRF */
  @Nullable
  public Integer getVni() {
    return _vni;
  }

  @Nonnull
  public Set<StaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  public void setBgpNxConfig(@Nullable CiscoNxBgpVrfConfiguration bgpNxConfig) {
    _bgpNxConfig = bgpNxConfig;
  }

  public void setBgpProcess(@Nullable BgpProcess bgpProcess) {
    _bgpProcess = bgpProcess;
  }

  public void setDescription(@Nullable String description) {
    _description = description;
  }

  public void setIsisProcess(@Nullable IsisProcess isisProcess) {
    _isisProcess = isisProcess;
  }

  public void setRipProcess(@Nullable RipProcess ripProcess) {
    _ripProcess = ripProcess;
  }

  public void setRouteDistinguisher(@Nullable RouteDistinguisher routeDistinguisher) {
    _routeDistinguisher = routeDistinguisher;
  }

  public void setRouteExportTarget(@Nullable ExtendedCommunity routeExportTarget) {
    _routeExportTarget = routeExportTarget;
  }

  public void setRouteImportTarget(@Nullable ExtendedCommunity routeImportTarget) {
    _routeImportTarget = routeImportTarget;
  }

  public void setShutdown(boolean shutdown) {
    _shutdown = shutdown;
  }

  public void setVni(@Nullable Integer vni) {
    _vni = vni;
  }
}

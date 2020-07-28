package org.batfish.representation.cumulus;

import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

/** BGP configuration for a particular VRF. */
public class BgpVrf implements Serializable {

  private boolean _defaultIpv4Unicast;
  private @Nullable Long _autonomousSystem;
  private @Nullable Boolean _asPathMultipathRelax;
  private final @Nonnull Map<String, BgpNeighbor> _neighbors;
  private @Nonnull Map<Prefix, BgpNetwork> _networks;
  private @Nullable BgpIpv4UnicastAddressFamily _ipv4Unicast;
  private @Nullable BgpL2vpnEvpnAddressFamily _l2VpnEvpn;
  private @Nullable Ip _routerId;
  private @Nullable Ip _clusterId;
  private final @Nonnull String _vrfName;
  private @Nullable Long _confederationId;
  private final @Nonnull Map<CumulusRoutingProtocol, BgpRedistributionPolicy>
      _redistributionPolicies;

  public BgpVrf(String vrfName) {
    // the default is true unless explicitly disabled (via "no bgp default ipv4-unicast")
    _defaultIpv4Unicast = true;
    _vrfName = vrfName;
    _neighbors = new HashMap<>();
    _networks = ImmutableMap.of();
    _redistributionPolicies = new TreeMap<>();
  }

  public boolean isIpv4UnicastActive() {
    return _defaultIpv4Unicast || (_ipv4Unicast != null);
  }

  public boolean getDefaultIpv4Unicast() {
    return _defaultIpv4Unicast;
  }

  public void setDefaultIpv4Unicast(boolean defaultIpv4Unicast) {
    _defaultIpv4Unicast = defaultIpv4Unicast;
  }

  @Nullable
  public Boolean getAsPathMultipathRelax() {
    return _asPathMultipathRelax;
  }

  public void setAsPathMultipathRelax(@Nullable Boolean asPathMultipathRelax) {
    _asPathMultipathRelax = asPathMultipathRelax;
  }

  public @Nullable Long getAutonomousSystem() {
    return _autonomousSystem;
  }

  public @Nonnull Map<String, BgpNeighbor> getNeighbors() {
    return _neighbors;
  }

  public @Nullable BgpIpv4UnicastAddressFamily getIpv4Unicast() {
    return _ipv4Unicast;
  }

  public @Nullable BgpL2vpnEvpnAddressFamily getL2VpnEvpn() {
    return _l2VpnEvpn;
  }

  public @Nullable Ip getRouterId() {
    return _routerId;
  }

  public @Nullable Ip getClusterId() {
    return _clusterId;
  }

  public @Nonnull String getVrfName() {
    return _vrfName;
  }

  public void setAutonomousSystem(@Nullable Long autonomousSystem) {
    _autonomousSystem = autonomousSystem;
  }

  public void setIpv4Unicast(@Nullable BgpIpv4UnicastAddressFamily ipv4Unicast) {
    _ipv4Unicast = ipv4Unicast;
  }

  public void setL2VpnEvpn(@Nullable BgpL2vpnEvpnAddressFamily l2VpnEvpn) {
    _l2VpnEvpn = l2VpnEvpn;
  }

  public void setRouterId(@Nullable Ip routerId) {
    _routerId = routerId;
  }

  public void setClusterId(@Nullable Ip clusterId) {
    _clusterId = clusterId;
  }

  @Nullable
  public Long getConfederationId() {
    return _confederationId;
  }

  public void setConfederationId(@Nullable Long confederationId) {
    _confederationId = confederationId;
  }

  @Nonnull
  public Map<Prefix, BgpNetwork> getNetworks() {
    return _networks;
  }

  public void addNetwork(Prefix network) {
    _networks =
        ImmutableMap.<Prefix, BgpNetwork>builder()
            .putAll(_networks)
            .put(network, new BgpNetwork(network))
            .build();
  }

  public @Nonnull Map<CumulusRoutingProtocol, BgpRedistributionPolicy> getRedistributionPolicies() {
    return _redistributionPolicies;
  }
}

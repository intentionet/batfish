package org.batfish.datamodel;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.eigrp.EigrpNeighborConfig;
import org.batfish.datamodel.eigrp.EigrpNeighborConfigId;
import org.batfish.datamodel.eigrp.EigrpProcess;
import org.batfish.datamodel.ospf.OspfNeighborConfig;
import org.batfish.datamodel.ospf.OspfNeighborConfigId;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.vxlan.Layer2Vni;

/**
 * Represents a set of configurations in a network. Has helper methods to "walk the configuration
 * tree" and extract specific objects based on their keys.
 */
@ParametersAreNonnullByDefault
public final class NetworkConfigurations {
  @Nonnull private final Map<String, Configuration> _configurations;

  /** Wrap a configurations map */
  private NetworkConfigurations(Map<String, Configuration> configurations) {
    _configurations = ImmutableMap.copyOf(requireNonNull(configurations));
  }

  @Nonnull
  public Map<String, Configuration> getMap() {
    return _configurations;
  }

  @Nonnull
  public Collection<Configuration> all() {
    return _configurations.values();
  }

  @Nonnull
  public Optional<Configuration> get(String hostname) {
    return Optional.ofNullable(_configurations.get(hostname));
  }

  /**
   * Returns a {@link BgpPeerConfig} matching the given {@code id} if one exists, otherwise {@code
   * null}.
   */
  @Nullable
  public BgpPeerConfig getBgpPeerConfig(BgpPeerConfigId id) {
    switch (id.getType()) {
      case ACTIVE:
        return getBgpPointToPointPeerConfig(id);
      case DYNAMIC:
        return getBgpDynamicPeerConfig(id);
      case UNNUMBERED:
        return getBgpUnnumberedPeerConfig(id);
      default:
        throw new IllegalArgumentException(String.format("Unrecognized peer type: %s", id));
    }
  }

  /**
   * Returns a {@link BgpPassivePeerConfig} matching the given {@code id} if one exists, otherwise
   * {@code null}.
   */
  @Nullable
  public BgpPassivePeerConfig getBgpDynamicPeerConfig(BgpPeerConfigId id) {
    if (id.getRemotePeerPrefix() == null) {
      return null;
    }
    return getVrf(id.getHostname(), id.getVrfName())
        .map(Vrf::getBgpProcess)
        .map(BgpProcess::getPassiveNeighbors)
        .map(m -> m.get(id.getRemotePeerPrefix()))
        .orElse(null);
  }

  /**
   * Returns a {@link BgpActivePeerConfig} matching the given {@code id} if one exists, otherwise
   * {@code null}.
   */
  @Nullable
  public BgpActivePeerConfig getBgpPointToPointPeerConfig(BgpPeerConfigId id) {
    if (id.getRemotePeerPrefix() == null) {
      return null;
    }
    return getVrf(id.getHostname(), id.getVrfName())
        .map(Vrf::getBgpProcess)
        .map(BgpProcess::getActiveNeighbors)
        .map(m -> m.get(id.getRemotePeerPrefix()))
        .orElse(null);
  }

  /**
   * Returns a {@link BgpUnnumberedPeerConfig} matching the given {@code id} if one exists,
   * otherwise {@code null}.
   */
  @Nullable
  public BgpUnnumberedPeerConfig getBgpUnnumberedPeerConfig(BgpPeerConfigId id) {
    if (id.getPeerInterface() == null) {
      return null;
    }
    return getVrf(id.getHostname(), id.getVrfName())
        .map(Vrf::getBgpProcess)
        .map(proc -> proc.getInterfaceNeighbors().get(id.getPeerInterface()))
        .orElse(null);
  }

  /**
   * Given {@link EigrpNeighborConfigId} returns an {@link Optional} of {@link EigrpNeighborConfig}
   *
   * @param eigrpConfigId {@link EigrpNeighborConfigId} for which {@link EigrpNeighborConfig} is
   *     needed
   * @return {@link EigrpNeighborConfig}
   */
  public Optional<EigrpNeighborConfig> getEigrpNeighborConfig(EigrpNeighborConfigId eigrpConfigId) {
    return getVrf(eigrpConfigId.getHostname(), eigrpConfigId.getVrf())
        .map(vrf -> vrf.getEigrpProcesses().get(eigrpConfigId.getAsn()))
        .map(EigrpProcess::getNeighbors)
        .map(eigrpNeighbors -> eigrpNeighbors.get(eigrpConfigId.getInterfaceName()));
  }

  /** Return an interface identified by hostname and interface name */
  @Nonnull
  public Optional<Interface> getInterface(String hostname, String interfaceName) {
    return get(hostname).map(Configuration::getAllInterfaces).map(m -> m.get(interfaceName));
  }

  @Nullable
  public IpsecPeerConfig getIpsecPeerConfig(IpsecPeerConfigId ipsecPeerConfigId) {
    return get(ipsecPeerConfigId.getHostName())
        .map(Configuration::getIpsecPeerConfigs)
        .map(m -> m.get(ipsecPeerConfigId.getIpsecPeerConfigName()))
        .orElse(null);
  }

  /** Return the {@link Mlag} configuration identified by a given ID. */
  @Nonnull
  public Optional<Mlag> getMlagConfig(String hostname, String id) {
    return get(hostname).map(Configuration::getMlags).map(m -> m.get(id));
  }

  public @Nonnull Optional<OspfProcess> getOspfProcess(String hostname, String interfaceName) {
    return getInterface(hostname, interfaceName)
        .map(
            iface ->
                iface.getOspfProcess() != null
                    ? iface.getVrf().getOspfProcesses().get(iface.getOspfProcess())
                    : null);
  }

  public @Nonnull Optional<OspfProcess> getOspfProcess(OspfNeighborConfigId ospfConfigId) {
    return getVrf(ospfConfigId.getHostname(), ospfConfigId.getVrfName())
        .map(vrf -> vrf.getOspfProcesses().get(ospfConfigId.getProcName()));
  }

  public Optional<OspfNeighborConfig> getOspfNeighborConfig(OspfNeighborConfigId ospfConfigId) {
    return getOspfProcess(ospfConfigId)
        .map(OspfProcess::getOspfNeighborConfigs)
        .map(oc -> oc.get(ospfConfigId));
  }

  /** Return {@link Layer2Vni} identificated by {@code hostname} and {@code vni} number. */
  @Nonnull
  public Optional<Layer2Vni> getVniSettings(String hostname, int vni) {
    // implementation assumes a given VNI can be present in at most one VRF.
    return get(hostname)
        .map(Configuration::getVrfs)
        .map(
            vrfs ->
                vrfs.values().stream()
                    .map(Vrf::getLayer2Vnis)
                    .map(vniSettingsMap -> vniSettingsMap.get(vni))
                    .filter(Objects::nonNull)
                    .findAny()
                    .orElse(null));
  }

  /** Return a VRF identified by hostname and VRF name */
  public Optional<Vrf> getVrf(String hostname, String vrfName) {
    return get(hostname).map(Configuration::getVrfs).map(m -> m.get(vrfName));
  }

  /** Wrap a configurations map */
  public static NetworkConfigurations of(Map<String, Configuration> configurations) {
    return new NetworkConfigurations(configurations);
  }

  /** Get OSPF neighbor config IDs for a given hostname and interface name. */
  public @Nonnull Optional<Stream<OspfNeighborConfigId>> getOspfNeighborConfigs(
      String hostname, String interfaceName) {
    return getOspfProcess(hostname, interfaceName)
        .map(
            proc ->
                proc.getOspfNeighborConfigs().keySet().stream()
                    .filter(id -> id.getInterfaceName().equals(interfaceName)));
  }
}

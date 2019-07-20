package org.batfish.datamodel.eigrp;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.NetworkConfigurations;

/**
 * Uniquely identifies an {@link EigrpNeighborConfig EIGRP neighbor configuration} in the network.
 */
@ParametersAreNonnullByDefault
public class EigrpNeighborConfigId implements Serializable, Comparable<EigrpNeighborConfigId> {

  private static final String PROP_HOSTNAME = "hostname";
  private static final String PROP_INTERFACE = "interface";
  private static final String PROP_VRF = "vrf";

  @Nonnull private final String _hostname;
  @Nonnull private final String _interfaceName;
  @Nonnull private final String _vrfName;

  public EigrpNeighborConfigId(String hostname, String interfaceName, String vrfName) {
    _hostname = hostname;
    _interfaceName = interfaceName;
    _vrfName = vrfName;
  }

  @JsonCreator
  private static EigrpNeighborConfigId create(
      @Nullable @JsonProperty(PROP_HOSTNAME) String hostname,
      @Nullable @JsonProperty(PROP_INTERFACE) String iface,
      @Nullable @JsonProperty(PROP_VRF) String vrf) {
    checkArgument(hostname != null, "Missing %s", PROP_HOSTNAME);
    checkArgument(iface != null, "Missing %s", PROP_INTERFACE);
    checkArgument(vrf != null, "Missing %s", PROP_VRF);
    return new EigrpNeighborConfigId(hostname, iface, vrf);
  }

  public EigrpNeighborConfigId(String hostname, Interface iface) {
    this(hostname, iface.getName(), iface.getVrfName());
  }

  @Override
  public int compareTo(EigrpNeighborConfigId o) {
    return Comparator.comparing(EigrpNeighborConfigId::getHostname)
        .thenComparing(EigrpNeighborConfigId::getInterfaceName)
        .thenComparing(EigrpNeighborConfigId::getVrf)
        .compare(this, o);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EigrpNeighborConfigId)) {
      return false;
    }
    EigrpNeighborConfigId rhs = (EigrpNeighborConfigId) o;
    return _hostname.equals(rhs._hostname)
        && _interfaceName.equals(rhs._interfaceName)
        && _vrfName.equals(rhs._vrfName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostname, _interfaceName, _vrfName);
  }

  @Nonnull
  @JsonProperty(PROP_HOSTNAME)
  public String getHostname() {
    return _hostname;
  }

  @Nonnull
  @JsonProperty(PROP_INTERFACE)
  public String getInterfaceName() {
    return _interfaceName;
  }

  @Nonnull
  @JsonProperty(PROP_VRF)
  public String getVrf() {
    return _vrfName;
  }

  @Nonnull
  @JsonIgnore
  public Interface getInterface(NetworkConfigurations nc) {
    return nc.getInterface(_hostname, _interfaceName).get();
  }

  @Nonnull
  @JsonIgnore
  public EigrpInterfaceSettings getInterfaceSettings(NetworkConfigurations nc) {
    return requireNonNull(getInterface(nc).getEigrp());
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .add("hostname", _hostname)
        .add("interfaceName", _interfaceName)
        .add("vrfName", _vrfName)
        .toString();
  }
}

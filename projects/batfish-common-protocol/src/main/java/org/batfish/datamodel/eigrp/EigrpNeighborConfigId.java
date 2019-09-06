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
import org.batfish.datamodel.collections.NodeInterfacePair;

/**
 * Uniquely identifies an {@link EigrpNeighborConfig EIGRP neighbor configuration} in the network.
 */
@ParametersAreNonnullByDefault
public class EigrpNeighborConfigId implements Serializable, Comparable<EigrpNeighborConfigId> {

  private static final String PROP_ASN = "asn";
  private static final String PROP_HOSTNAME = "hostname";
  private static final String PROP_INTERFACE = "interface";
  private static final String PROP_VRF = "vrf";

  private final long _asn;
  @Nonnull private final String _hostname;
  @Nonnull private final String _interfaceName;
  @Nonnull private final String _vrfName;

  public EigrpNeighborConfigId(long asn, String hostname, String interfaceName, String vrfName) {
    _asn = asn;
    _hostname = hostname;
    _interfaceName = interfaceName;
    _vrfName = vrfName;
  }

  @JsonCreator
  private static EigrpNeighborConfigId create(
      @Nullable @JsonProperty(PROP_ASN) Long asn,
      @Nullable @JsonProperty(PROP_HOSTNAME) String hostname,
      @Nullable @JsonProperty(PROP_INTERFACE) String iface,
      @Nullable @JsonProperty(PROP_VRF) String vrf) {
    checkArgument(asn != null, "Missing %s", PROP_ASN);
    checkArgument(hostname != null, "Missing %s", PROP_HOSTNAME);
    checkArgument(iface != null, "Missing %s", PROP_INTERFACE);
    checkArgument(vrf != null, "Missing %s", PROP_VRF);
    return new EigrpNeighborConfigId(asn, hostname, iface, vrf);
  }

  public EigrpNeighborConfigId(long asn, String hostname, Interface iface) {
    this(asn, hostname, iface.getName(), iface.getVrfName());
  }

  @Override
  public int compareTo(EigrpNeighborConfigId o) {
    return Comparator.comparing(EigrpNeighborConfigId::getHostname)
        .thenComparing(EigrpNeighborConfigId::getInterfaceName)
        .thenComparing(EigrpNeighborConfigId::getVrf)
        .thenComparing(EigrpNeighborConfigId::getAsn)
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
    return _asn == rhs._asn
        && _hostname.equals(rhs._hostname)
        && _interfaceName.equals(rhs._interfaceName)
        && _vrfName.equals(rhs._vrfName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_asn, _hostname, _interfaceName, _vrfName);
  }

  @JsonProperty(PROP_ASN)
  public long getAsn() {
    return _asn;
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
  public NodeInterfacePair getNodeInterfacePair() {
    return NodeInterfacePair.of(getHostname(), getInterfaceName());
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
        .add("vrfName", _vrfName)
        .add("asn", _asn)
        .add("interfaceName", _interfaceName)
        .toString();
  }
}

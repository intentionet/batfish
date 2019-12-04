package org.batfish.datamodel.vxlan;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Ip;

/** Settings for a VXLAN segment identified by a L2 VNI */
public final class Layer2Vni implements Vni {

  /** Builder for {@link Layer2Vni} */
  public static final class Builder {

    @Nonnull private Set<Ip> _bumTransportIps = ImmutableSet.of();
    @Nullable private BumTransportMethod _bumTransportMethod;
    @Nullable private Ip _sourceAddress;
    @Nullable private Integer _udpPort;
    @Nullable private Integer _vlan;
    @Nullable private Integer _vni;

    private Builder() {}

    public @Nonnull Layer2Vni build() {
      checkArgument(_vni != null, "VNI must not be null.");
      checkArgument(_vlan != null, "VLAN must not be null.");
      checkArgument(_bumTransportMethod != null, "BumTransportMethod must not be null.");
      checkArgument(
          _bumTransportMethod != BumTransportMethod.MULTICAST_GROUP || _bumTransportIps.size() <= 1,
          "Cannot specify more than one multicast group.");
      return new Layer2Vni(
          _bumTransportIps,
          _bumTransportMethod,
          _sourceAddress,
          firstNonNull(_udpPort, DEFAULT_UDP_PORT),
          _vlan,
          _vni);
    }

    public @Nonnull Builder setBumTransportIps(Collection<Ip> bumTransportIps) {
      _bumTransportIps = ImmutableSortedSet.copyOf(bumTransportIps);
      return this;
    }

    public @Nonnull Builder setBumTransportMethod(BumTransportMethod bumTransportMethod) {
      _bumTransportMethod = bumTransportMethod;
      return this;
    }

    public @Nonnull Builder setSourceAddress(@Nullable Ip sourceAddress) {
      _sourceAddress = sourceAddress;
      return this;
    }

    public @Nonnull Builder setUdpPort(Integer udpPort) {
      _udpPort = udpPort;
      return this;
    }

    public @Nonnull Builder setVlan(Integer vlan) {
      _vlan = vlan;
      return this;
    }

    public @Nonnull Builder setVni(int vni) {
      _vni = vni;
      return this;
    }
  }

  @Nonnull private final Set<Ip> _bumTransportIps;
  @Nonnull private final BumTransportMethod _bumTransportMethod;
  @Nullable private final Ip _sourceAddress;
  @Nonnull private final Integer _udpPort;
  private final int _vlan;
  private final int _vni;

  public static @Nonnull Builder builder() {
    return new Builder();
  }

  private Layer2Vni(
      Set<Ip> bumTransportIps,
      BumTransportMethod bumTransportMethod,
      @Nullable Ip sourceAddress,
      Integer udpPort,
      int vlan,
      int vni) {
    _bumTransportIps = bumTransportIps;
    _bumTransportMethod = bumTransportMethod;
    _sourceAddress = sourceAddress;
    _udpPort = udpPort;
    _vlan = vlan;
    _vni = vni;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Layer2Vni)) {
      return false;
    }
    Layer2Vni rhs = (Layer2Vni) obj;
    return Objects.equals(_bumTransportMethod, rhs._bumTransportMethod)
        && Objects.equals(_bumTransportIps, rhs._bumTransportIps)
        && Objects.equals(_sourceAddress, rhs._sourceAddress)
        && Objects.equals(_udpPort, rhs._udpPort)
        && Objects.equals(_vlan, rhs._vlan)
        && _vni == rhs._vni;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _bumTransportMethod, _bumTransportIps, _sourceAddress, _udpPort, _vlan, _vni);
  }

  @Override
  public @Nullable Ip getMulticastGroup() {
    return _bumTransportMethod == BumTransportMethod.MULTICAST_GROUP
        ? Iterables.getOnlyElement(_bumTransportIps)
        : null;
  }

  @Override
  public @Nonnull Set<Ip> getBumTransportIps() {
    return _bumTransportIps;
  }

  @Override
  public @Nonnull BumTransportMethod getBumTransportMethod() {
    return _bumTransportMethod;
  }

  @Override
  @Nullable
  public Ip getSourceAddress() {
    return _sourceAddress;
  }

  @Override
  public @Nonnull Integer getUdpPort() {
    return _udpPort;
  }

  public int getVlan() {
    return _vlan;
  }

  @Override
  public int getVni() {
    return _vni;
  }

  @Nonnull
  public Builder toBuilder() {
    return builder()
        .setBumTransportMethod(_bumTransportMethod)
        .setSourceAddress(_sourceAddress)
        .setVni(_vni)
        .setVlan(_vlan)
        .setUdpPort(_udpPort)
        .setBumTransportIps(_bumTransportIps);
  }

  /** Return a new {@link Layer2Vni} with a flood list that includes a given {@code ip} */
  @Nonnull
  public Layer2Vni addToFloodList(Ip ip) {
    checkArgument(
        _bumTransportMethod == BumTransportMethod.UNICAST_FLOOD_GROUP,
        "Cannot add new IPs if the transport method is not %s",
        BumTransportMethod.UNICAST_FLOOD_GROUP);
    return toBuilder()
        .setBumTransportIps(ImmutableSet.<Ip>builder().addAll(_bumTransportIps).add(ip).build())
        .build();
  }
}

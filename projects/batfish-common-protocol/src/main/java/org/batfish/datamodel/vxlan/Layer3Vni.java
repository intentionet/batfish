package org.batfish.datamodel.vxlan;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Ip;

/** Configuration for an L3 VXLAN VNI */
public final class Layer3Vni implements Vni {
  @Nonnull private final Set<Ip> _bumTransportIps;
  @Nonnull private final BumTransportMethod _bumTransportMethod;
  @Nullable private final Ip _sourceAddress;
  @Nonnull private final Integer _udpPort;
  private final int _vni;

  private Layer3Vni(
      Set<Ip> bumTransportIps,
      BumTransportMethod bumTransportMethod,
      @Nullable Ip sourceAddress,
      Integer udpPort,
      int vni) {
    _bumTransportIps = ImmutableSet.copyOf(bumTransportIps);
    _bumTransportMethod = bumTransportMethod;
    _sourceAddress = sourceAddress;
    _udpPort = udpPort;
    _vni = vni;
  }

  @Override
  @Nonnull
  public Set<Ip> getBumTransportIps() {
    return _bumTransportIps;
  }

  @Override
  @Nonnull
  public BumTransportMethod getBumTransportMethod() {
    return _bumTransportMethod;
  }

  @Nullable
  @Override
  public Ip getMulticastGroup() {
    return _bumTransportMethod == BumTransportMethod.MULTICAST_GROUP
        ? Iterables.getOnlyElement(_bumTransportIps)
        : null;
  }

  @Override
  @Nullable
  public Ip getSourceAddress() {
    return _sourceAddress;
  }

  @Override
  @Nonnull
  public Integer getUdpPort() {
    return _udpPort;
  }

  @Override
  public int getVni() {
    return _vni;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Layer3Vni)) {
      return false;
    }
    Layer3Vni layer3Vni = (Layer3Vni) o;
    return _vni == layer3Vni._vni
        && _bumTransportIps.equals(layer3Vni._bumTransportIps)
        && _bumTransportMethod == layer3Vni._bumTransportMethod
        && Objects.equals(_sourceAddress, layer3Vni._sourceAddress)
        && _udpPort.equals(layer3Vni._udpPort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_bumTransportIps, _bumTransportMethod, _sourceAddress, _udpPort, _vni);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder for {@link Layer3Vni} */
  public static final class Builder {
    @Nonnull private Set<Ip> _bumTransportIps = ImmutableSet.of();
    @Nullable private BumTransportMethod _bumTransportMethod;
    @Nullable private Ip _sourceAddress;
    @Nullable private Integer _udpPort = Vni.DEFAULT_UDP_PORT;
    @Nullable private Integer _vni;

    private Builder() {}

    @Nonnull
    public Builder setBumTransportIps(Set<Ip> bumTransportIps) {
      _bumTransportIps = bumTransportIps;
      return this;
    }

    @Nonnull
    public Builder setBumTransportMethod(BumTransportMethod bumTransportMethod) {
      _bumTransportMethod = bumTransportMethod;
      return this;
    }

    @Nonnull
    public Builder setSourceAddress(@Nullable Ip sourceAddress) {
      _sourceAddress = sourceAddress;
      return this;
    }

    @Nonnull
    public Builder setUdpPort(Integer udpPort) {
      _udpPort = udpPort;
      return this;
    }

    @Nonnull
    public Builder setVni(int vni) {
      _vni = vni;
      return this;
    }

    @Nonnull
    public Layer3Vni build() {
      checkArgument(_vni != null, "VNI must not be null.");
      checkArgument(_bumTransportMethod != null, "BumTransportMethod must not be null.");
      checkArgument(
          _bumTransportMethod != BumTransportMethod.MULTICAST_GROUP || _bumTransportIps.size() <= 1,
          "Cannot specify more than one multicast group.");
      return new Layer3Vni(
          _bumTransportIps,
          _bumTransportMethod,
          _sourceAddress,
          firstNonNull(_udpPort, DEFAULT_UDP_PORT),
          _vni);
    }
  }
}

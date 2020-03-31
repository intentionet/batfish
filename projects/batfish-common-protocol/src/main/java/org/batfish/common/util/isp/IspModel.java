package org.batfish.common.util.isp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.common.util.isp.IspModelingUtils.LINK_LOCAL_IP;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.isp_configuration.traffic_filtering.IspTrafficFiltering;

/** Contains the information required to model one ISP node */
@ParametersAreNonnullByDefault
final class IspModel {
  static final class Builder {
    IspModel build() {
      checkArgument(_asn != null, "Missing ASN");
      checkArgument(_name != null, "Missing name");
      return new IspModel(
          _asn,
          firstNonNull(_remotes, ImmutableList.of()),
          _name,
          firstNonNull(_additionalPrefixesToInternet, ImmutableSet.of()),
          firstNonNull(_trafficFiltering, IspTrafficFiltering.none()));
    }

    public Builder setAsn(long asn) {
      _asn = asn;
      return this;
    }

    public Builder setName(@Nullable String name) {
      _name = name;
      return this;
    }

    public Builder setAdditionalPrefixesToInternet(
        @Nullable Iterable<Prefix> additionalPrefixesToInternet) {
      _additionalPrefixesToInternet =
          additionalPrefixesToInternet == null
              ? null
              : ImmutableSet.copyOf(additionalPrefixesToInternet);
      return this;
    }

    public Builder setAdditionalPrefixesToInternet(
        @Nonnull Prefix... additionalPrefixesToInternet) {
      return setAdditionalPrefixesToInternet(Arrays.asList(additionalPrefixesToInternet));
    }

    public Builder setRemotes(@Nullable Iterable<Remote> remotes) {
      _remotes = remotes == null ? null : ImmutableList.copyOf(remotes);
      return this;
    }

    public Builder setRemotes(@Nonnull Remote... remotes) {
      return setRemotes(Arrays.asList(remotes));
    }

    public Builder setTrafficFiltering(@Nullable IspTrafficFiltering trafficFiltering) {
      _trafficFiltering = trafficFiltering;
      return this;
    }

    private @Nullable Long _asn;
    private @Nullable String _name;
    private @Nullable List<Remote> _remotes;
    private @Nullable Set<Prefix> _additionalPrefixesToInternet;
    private @Nullable IspTrafficFiltering _trafficFiltering;
  }

  public static @Nonnull Builder builder() {
    return new Builder();
  }

  /** Represents one remote end of the ISP node */
  static final class Remote {

    private @Nonnull final String _remoteHostname;
    private @Nonnull final String _remoteIfaceName;
    private @Nonnull final InterfaceAddress _ispIfaceAddress;
    private @Nonnull final BgpPeerConfig _remoteBgpPeerConfig;

    Remote(
        String remoteHostname,
        String remoteIfaceName,
        InterfaceAddress ispIfaceAddress,
        BgpPeerConfig remoteBgpPeerConfig) {
      _remoteHostname = remoteHostname;
      _remoteIfaceName = remoteIfaceName;
      _ispIfaceAddress = ispIfaceAddress;
      _remoteBgpPeerConfig = remoteBgpPeerConfig;
    }

    /** Returns what should be the Ip for the ISP interface that peers with this remote node */
    public Ip getIspIfaceIp() {
      if (_remoteBgpPeerConfig instanceof BgpActivePeerConfig) {
        return ((BgpActivePeerConfig) _remoteBgpPeerConfig).getPeerAddress();
      } else {
        return LINK_LOCAL_IP;
      }
    }

    @Nonnull
    public String getRemoteHostname() {
      return _remoteHostname;
    }

    @Nonnull
    public String getRemoteIfaceName() {
      return _remoteIfaceName;
    }

    @Nonnull
    public InterfaceAddress getIspIfaceAddress() {
      return _ispIfaceAddress;
    }

    @Nonnull
    public BgpPeerConfig getRemoteBgpPeerConfig() {
      return _remoteBgpPeerConfig;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Remote)) {
        return false;
      }
      Remote neighbor = (Remote) o;
      return _remoteHostname.equals(neighbor._remoteHostname)
          && _remoteIfaceName.equals(neighbor._remoteIfaceName)
          && _ispIfaceAddress.equals(neighbor._ispIfaceAddress)
          && _remoteBgpPeerConfig.equals(neighbor._remoteBgpPeerConfig);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          _remoteHostname, _remoteIfaceName, _ispIfaceAddress, _remoteBgpPeerConfig);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("remoteHostname", _remoteHostname)
          .add("remoteIfaceName", _remoteIfaceName)
          .add("ispInterfaceAddress", _ispIfaceAddress)
          .add("remoteBgpActivePeerConfig", _remoteBgpPeerConfig)
          .toString();
    }
  }

  private final long _asn;
  private final @Nonnull String _name;
  private @Nonnull List<Remote> _remotes;
  private final @Nonnull Set<Prefix> _additionalPrefixesToInternet;
  private final @Nonnull IspTrafficFiltering _trafficFiltering;

  private IspModel(
      long asn,
      List<Remote> remotes,
      String name,
      Set<Prefix> additionalPrefixesToInternet,
      IspTrafficFiltering trafficFiltering) {
    _asn = asn;
    _remotes = remotes;
    _name = name;
    _additionalPrefixesToInternet = ImmutableSet.copyOf(additionalPrefixesToInternet);
    _trafficFiltering = trafficFiltering;
  }

  void addNeighbor(Remote neighbor) {
    _remotes =
        ImmutableList.<Remote>builderWithExpectedSize(1 + _remotes.size())
            .addAll(_remotes)
            .add(neighbor)
            .build();
  }

  @Nonnull
  List<Remote> getRemotes() {
    return ImmutableList.copyOf(_remotes);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("asn", _asn)
        .add("name", _name)
        .add("neighbors", _remotes)
        .add("additionalPrefixes", _additionalPrefixesToInternet)
        .add("trafficFiltering", _trafficFiltering)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IspModel)) {
      return false;
    }
    IspModel ispInfo = (IspModel) o;
    return _asn == ispInfo._asn
        && _remotes.equals(ispInfo._remotes)
        && _name.equals(ispInfo._name)
        && _additionalPrefixesToInternet.equals(ispInfo._additionalPrefixesToInternet)
        && _trafficFiltering.equals(ispInfo._trafficFiltering);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_asn, _remotes, _name, _additionalPrefixesToInternet, _trafficFiltering);
  }

  public long getAsn() {
    return _asn;
  }

  @Nonnull
  public String getName() {
    return _name;
  }

  /**
   * Returns the prefixes that the ISP should announce to the Internet over the BGP connection
   * (beyond just passing along what it hears from other connected nodes)
   */
  @Nonnull
  public Set<Prefix> getAdditionalPrefixesToInternet() {
    return _additionalPrefixesToInternet;
  }

  /** Returns the {@link IspTrafficFiltering traffic filtering policy} of this ISP. */
  @Nonnull
  public IspTrafficFiltering getTrafficFiltering() {
    return _trafficFiltering;
  }
}

package org.batfish.representation.cumulus;

import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Parent for all BGP neighbors. */
public abstract class BgpNeighbor implements Serializable {

  private final @Nonnull String _name;
  private @Nullable String _description;
  private @Nullable String _peerGroup;

  // Inheritable properties
  private @Nullable Long _remoteAs;
  private @Nullable RemoteAsType _remoteAsType;
  private @Nullable BgpNeighborIpv4UnicastAddressFamily _ipv4UnicastAddressFamily;
  private @Nullable BgpNeighborL2vpnEvpnAddressFamily _l2vpnEvpnAddressFamily;

  // Whether this configuration has inherited from its parent.
  private boolean _inherited = false;

  public BgpNeighbor(String name) {
    _name = name;
  }

  public @Nonnull String getName() {
    return _name;
  }

  @Nullable
  public String getDescription() {
    return _description;
  }

  public void setDescription(@Nullable String description) {
    _description = description;
  }

  @Nullable
  public BgpNeighborIpv4UnicastAddressFamily getIpv4UnicastAddressFamily() {
    return _ipv4UnicastAddressFamily;
  }

  public BgpNeighbor setIpv4UnicastAddressFamily(
      @Nullable BgpNeighborIpv4UnicastAddressFamily ipv4UnicastAddressFamily) {
    _ipv4UnicastAddressFamily = ipv4UnicastAddressFamily;
    return this;
  }

  @Nullable
  public BgpNeighborL2vpnEvpnAddressFamily getL2vpnEvpnAddressFamily() {
    return _l2vpnEvpnAddressFamily;
  }

  public BgpNeighbor setL2vpnEvpnAddressFamily(
      @Nullable BgpNeighborL2vpnEvpnAddressFamily l2vpnEvpnAddressFamily) {
    _l2vpnEvpnAddressFamily = l2vpnEvpnAddressFamily;
    return this;
  }

  @Nullable
  public String getPeerGroup() {
    return _peerGroup;
  }

  public void setPeerGroup(@Nullable String peerGroup) {
    _peerGroup = peerGroup;
  }

  /**
   * Returns explicit remote-as number when {@link #getRemoteAsType} is {@link
   * RemoteAsType#EXPLICIT}, or else {@code null}.
   */
  public @Nullable Long getRemoteAs() {
    return _remoteAs;
  }

  public @Nullable RemoteAsType getRemoteAsType() {
    return _remoteAsType;
  }

  public void setRemoteAs(@Nullable Long remoteAs) {
    _remoteAs = remoteAs;
  }

  public void setRemoteAsType(@Nullable RemoteAsType remoteAsType) {
    _remoteAsType = remoteAsType;
  }

  protected void inheritFrom(@Nonnull Map<String, BgpNeighbor> peers) {
    if (_inherited) {
      return;
    }
    _inherited = true;

    @Nullable BgpNeighbor other = _peerGroup == null ? null : peers.get(_peerGroup);
    if (other == null) {
      return;
    }

    // Do not inherit description.
    // Do not inherit name.
    // Do not inherit peer group.

    if (_remoteAsType == null) {
      // These properties are coupled, but remoteAsType will be non-null if they have been set.
      _remoteAs = other.getRemoteAs();
      _remoteAsType = other.getRemoteAsType();
    }

    if (_ipv4UnicastAddressFamily == null) {
      _ipv4UnicastAddressFamily = other.getIpv4UnicastAddressFamily();
    } else if (other.getIpv4UnicastAddressFamily() != null) {
      _ipv4UnicastAddressFamily.inheritFrom(other.getIpv4UnicastAddressFamily());
    }

    if (_l2vpnEvpnAddressFamily == null) {
      _l2vpnEvpnAddressFamily = other.getL2vpnEvpnAddressFamily();
    } else if (other.getL2vpnEvpnAddressFamily() != null) {
      _l2vpnEvpnAddressFamily.inheritFrom(other.getL2vpnEvpnAddressFamily());
    }
  }
}

package org.batfish.representation.cisco.eos;

/** EVPN address family settings at the neighbor level */
public final class AristaBgpNeighborEvpnAddressFamily extends AristaBgpNeighborAddressFamily {

  /** Inherit the settings from VRF level */
  public void inheritFrom(AristaBgpVrfEvpnAddressFamily af) {
    if (_additionalPaths != null) {
      _additionalPaths = af.getAdditionalPaths();
    }
    if (_nextHopUnchanged != null) {
      _nextHopUnchanged = af.getNextHopUnchanged();
    }
  }
}

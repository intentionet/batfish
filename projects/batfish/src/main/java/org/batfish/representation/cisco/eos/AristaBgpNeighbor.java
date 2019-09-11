package org.batfish.representation.cisco.eos;

import java.io.Serializable;
import javax.annotation.Nullable;

/** Base class for all Arista BGP neighbors */
public abstract class AristaBgpNeighbor implements Serializable {
  @Nullable private Integer _allowAsIn;
  @Nullable private Boolean _autoLocalAddr;
  @Nullable private String _description;
  @Nullable private Boolean _dontCapabilityNegotiate;
  @Nullable private Integer _ebgpMultihop;
  @Nullable private AristaBgpNeighborEvpnAddressFamily _evpnAf;
  @Nullable private Boolean _enforceFirstAs;
  @Nullable private Long _localAs;
  @Nullable private Boolean _nextHopSelf;
  @Nullable private Long _remoteAs;
  @Nullable private Boolean _sendCommunity;
  @Nullable private AristaBgpNeighborIpv4UnicastAddressFamily _v4UnicastAf;

  protected AristaBgpNeighbor() {
    // By default, all neighbors have IPv4 unicast as their default address family
    _v4UnicastAf = new AristaBgpNeighborIpv4UnicastAddressFamily();
  }

  @Nullable
  public Integer getAllowAsIn() {
    return _allowAsIn;
  }

  public void setAllowAsIn(@Nullable Integer allowAsIn) {
    _allowAsIn = allowAsIn;
  }

  @Nullable
  public Boolean getAutoLocalAddr() {
    return _autoLocalAddr;
  }

  public void setAutoLocalAddr(@Nullable Boolean autoLocalAddr) {
    _autoLocalAddr = autoLocalAddr;
  }

  @Nullable
  public String getDescription() {
    return _description;
  }

  public void setDescription(@Nullable String description) {
    _description = description;
  }

  @Nullable
  public Boolean getDontCapabilityNegotiate() {
    return _dontCapabilityNegotiate;
  }

  public void setDontCapabilityNegotiate(@Nullable Boolean dontCapabilityNegotiate) {
    _dontCapabilityNegotiate = dontCapabilityNegotiate;
  }

  @Nullable
  public Integer getEbgpMultihop() {
    return _ebgpMultihop;
  }

  public void setEbgpMultihop(@Nullable Integer ebgpMultihop) {
    _ebgpMultihop = ebgpMultihop;
  }

  @Nullable
  public Boolean getEnforceFirstAs() {
    return _enforceFirstAs;
  }

  public void setEnforceFirstAs(@Nullable Boolean enforceFirstAs) {
    _enforceFirstAs = enforceFirstAs;
  }

  @Nullable
  public AristaBgpNeighborEvpnAddressFamily getEvpnAf() {
    return _evpnAf;
  }

  public void setEvpnAf(@Nullable AristaBgpNeighborEvpnAddressFamily evpnAf) {
    _evpnAf = evpnAf;
  }

  @Nullable
  public Long getLocalAs() {
    return _localAs;
  }

  public void setLocalAs(@Nullable Long localAs) {
    _localAs = localAs;
  }

  @Nullable
  public Boolean getNextHopSelf() {
    return _nextHopSelf;
  }

  public void setNextHopSelf(@Nullable Boolean nextHopSelf) {
    _nextHopSelf = nextHopSelf;
  }

  @Nullable
  public Long getRemoteAs() {
    return _remoteAs;
  }

  public void setRemoteAs(@Nullable Long remoteAs) {
    _remoteAs = remoteAs;
  }

  @Nullable
  public Boolean getSendCommunity() {
    return _sendCommunity;
  }

  public void setSendCommunity(@Nullable Boolean sendCommunity) {
    _sendCommunity = sendCommunity;
  }

  @Nullable
  public AristaBgpNeighborIpv4UnicastAddressFamily getV4UnicastAf() {
    return _v4UnicastAf;
  }

  public void setV4UnicastAf(@Nullable AristaBgpNeighborIpv4UnicastAddressFamily v4UnicastAf) {
    _v4UnicastAf = v4UnicastAf;
  }
}

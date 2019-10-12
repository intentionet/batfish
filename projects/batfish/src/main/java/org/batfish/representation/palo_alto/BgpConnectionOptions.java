package org.batfish.representation.palo_alto;

import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * Configuration of a BGP Peer Connection {@code network virtual-router NAME protocol bgp peer-group
 * NAME peer NAME connection-options}.
 */
public final class BgpConnectionOptions implements Serializable {
  @Nullable
  public Boolean getIncomingAllow() {
    return _incomingAllow;
  }

  public void setIncomingAllow(Boolean incomingAllow) {
    _incomingAllow = incomingAllow;
  }

  @Nullable
  public Integer getLocalPort() {
    return _localPort;
  }

  public void setLocalPort(Integer localPort) {
    _localPort = localPort;
  }

  @Nullable
  public Boolean getOutgoingAllow() {
    return _outgoingAllow;
  }

  public void setOutgoingAllow(Boolean outgoingAllow) {
    _outgoingAllow = outgoingAllow;
  }

  @Nullable
  public Integer getRemotePort() {
    return _remotePort;
  }

  public void setRemotePort(Integer remotePort) {
    _remotePort = remotePort;
  }

  // private implementation details

  private @Nullable Boolean _incomingAllow;
  private @Nullable Integer _remotePort;
  private @Nullable Boolean _outgoingAllow;
  private @Nullable Integer _localPort;
}

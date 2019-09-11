package org.batfish.representation.cisco.eos;

import javax.annotation.Nullable;

/** Configuration for BGP "additional paths" setting */
public class AristaBgpAdditionalPathsConfig {
  public enum SendType {
    ANY
  }

  @Nullable private Boolean _receive;
  @Nullable private SendType _send;

  public AristaBgpAdditionalPathsConfig() {}

  @Nullable
  public Boolean getReceive() {
    return _receive;
  }

  public void setReceive(@Nullable Boolean receive) {
    _receive = receive;
  }

  @Nullable
  public SendType getSend() {
    return _send;
  }

  public void setSend(@Nullable SendType send) {
    _send = send;
  }
}

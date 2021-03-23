package org.batfish.representation.fortios;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;

/** FortiOS datamodel component containing BGP neighbor configuration */
public class BgpNeighbor implements Serializable {
  private final @Nonnull Ip _ip;
  private @Nullable Long _remoteAs;

  public BgpNeighbor(Ip ip) {
    _ip = ip;
  }

  public @Nonnull Ip getIp() {
    return _ip;
  }

  public @Nullable Long getRemoteAs() {
    return _remoteAs;
  }

  public void setRemoteAs(Long remoteAs) {
    _remoteAs = remoteAs;
  }
}

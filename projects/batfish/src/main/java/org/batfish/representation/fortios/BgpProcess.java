package org.batfish.representation.fortios;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;

/** FortiOS datamodel component containing BGP configuration */
public final class BgpProcess implements Serializable {
  private @Nullable Long _as;
  private @Nullable Ip _routerId;
  private final Map<Ip, BgpNeighbor> _neighbors;

  public BgpProcess() {
    _neighbors = new HashMap<>();
  }

  public @Nullable Long getAs() {
    return _as;
  }

  public @Nonnull Map<Ip, BgpNeighbor> getNeighbors() {
    return _neighbors;
  }

  public @Nullable Ip getRouterId() {
    return _routerId;
  }

  public void setAs(Long as) {
    _as = as;
  }

  public void setRouterId(Ip routerId) {
    _routerId = routerId;
  }
}

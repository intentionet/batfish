package org.batfish.representation.cisco.eos;

import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * Address family settings that are common to all address families and can be set at the VRF level
 */
public abstract class AristaBgpVrfAddressFamily implements Serializable {
  @Nullable protected AristaBgpAdditionalPathsConfig _additionalPaths;
  @Nullable protected Boolean _nextHopUnchanged;

  @Nullable
  public AristaBgpAdditionalPathsConfig getAdditionalPaths() {
    return _additionalPaths;
  }

  public void setAdditionalPaths(@Nullable AristaBgpAdditionalPathsConfig additionalPaths) {
    _additionalPaths = additionalPaths;
  }

  @Nullable
  public Boolean getNextHopUnchanged() {
    return _nextHopUnchanged;
  }

  public void setNextHopUnchanged(@Nullable Boolean nextHopUnchanged) {
    _nextHopUnchanged = nextHopUnchanged;
  }
}

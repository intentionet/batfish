package org.batfish.representation.palo_alto;

import java.io.Serializable;
import javax.annotation.Nullable;

/** Represents source-translation clause in Palo Alto NAT rule */
public class SourceTranslation implements Serializable {

  @Nullable private DynamicIpAndPort _dynamicIpAndPort;

  public SourceTranslation() {}

  @Nullable
  public DynamicIpAndPort getDynamicIpAndPort() {
    return _dynamicIpAndPort;
  }

  public void setDynamicIpAndPort(@Nullable DynamicIpAndPort dynamicIpAndPort) {
    _dynamicIpAndPort = dynamicIpAndPort;
  }
}

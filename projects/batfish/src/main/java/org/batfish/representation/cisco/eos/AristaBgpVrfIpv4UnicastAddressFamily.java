package org.batfish.representation.cisco.eos;

import javax.annotation.Nullable;

/** Settings specific to IPv4 unicast address family, which can be set at the VRF level. */
public class AristaBgpVrfIpv4UnicastAddressFamily extends AristaBgpVrfAddressFamily {
  @Nullable private String _installMap;
  // TODO: "bgp next-hop address-family ipv6"
  @Nullable private Boolean _redistributeInternal;

  public AristaBgpVrfIpv4UnicastAddressFamily() {}

  @Nullable
  public String getInstallMap() {
    return _installMap;
  }

  public void setInstallMap(@Nullable String installMap) {
    _installMap = installMap;
  }

  @Nullable
  public Boolean getRedistributeInternal() {
    return _redistributeInternal;
  }

  public void setRedistributeInternal(@Nullable Boolean redistributeInternal) {
    _redistributeInternal = redistributeInternal;
  }
}

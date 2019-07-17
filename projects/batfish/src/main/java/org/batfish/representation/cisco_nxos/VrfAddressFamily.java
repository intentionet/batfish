package org.batfish.representation.cisco_nxos;

import java.io.Serializable;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class VrfAddressFamily implements Serializable {
  public VrfAddressFamily(AddressFamily type) {
    _type = type;
  }

  public @Nullable RouteDistinguisherOrAuto getExportRt() {
    return _exportRt;
  }

  public void setExportRt(@Nullable RouteDistinguisherOrAuto exportRt) {
    _exportRt = exportRt;
  }

  public @Nullable RouteDistinguisherOrAuto getExportRtEvpn() {
    return _exportRtEvpn;
  }

  public void setExportRtEvpn(@Nullable RouteDistinguisherOrAuto exportRtEvpn) {
    _exportRtEvpn = exportRtEvpn;
  }

  @Nullable
  public RouteDistinguisherOrAuto getImportRt() {
    return _importRt;
  }

  public void setImportRt(@Nullable RouteDistinguisherOrAuto importRt) {
    _importRt = importRt;
  }

  @Nullable
  public RouteDistinguisherOrAuto getImportRtEvpn() {
    return _importRtEvpn;
  }

  public void setImportRtEvpn(@Nullable RouteDistinguisherOrAuto importRtEvpn) {
    _importRtEvpn = importRtEvpn;
  }

  public AddressFamily getType() {
    return _type;
  }

  //////////////////////////////////////////
  ///// Private implementation details /////
  //////////////////////////////////////////

  private final AddressFamily _type;
  private @Nullable RouteDistinguisherOrAuto _exportRt;
  private @Nullable RouteDistinguisherOrAuto _exportRtEvpn;
  private @Nullable RouteDistinguisherOrAuto _importRt;
  private @Nullable RouteDistinguisherOrAuto _importRtEvpn;
}

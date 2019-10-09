package org.batfish.representation.palo_alto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.Ip;

public class OspfArea {

  public OspfArea(@Nonnull Ip areaId) {
    _areaId = areaId;
  }

  @Nullable
  public OspfAreaTypeSettings getTypeSettings() {
    return _typeSettings;
  }

  public void setTypeSettings(@Nullable OspfAreaTypeSettings typeSettings) {
    _typeSettings = typeSettings;
  }

  @Nonnull
  public Ip getAreaId() {
    return _areaId;
  }

  public void setAreaId(Ip areaId) {
    _areaId = areaId;
  }

  private @Nonnull Ip _areaId;
  private @Nullable OspfAreaTypeSettings _typeSettings;
}

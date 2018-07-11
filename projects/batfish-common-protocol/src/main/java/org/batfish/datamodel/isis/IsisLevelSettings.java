package org.batfish.datamodel.isis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;

public class IsisLevelSettings implements Serializable {

  public static class Builder {

    private boolean _wideMetricsOnly;

    public @Nonnull IsisLevelSettings build() {
      return new IsisLevelSettings(_wideMetricsOnly);
    }

    public @Nonnull Builder setWideMetricsOnly(boolean wideMetricsOnly) {
      _wideMetricsOnly = wideMetricsOnly;
      return this;
    }
  }

  private static final String PROP_WIDE_METRICS_ONLY = "wideMetricsOnly";

  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  private boolean _wideMetricsOnly;

  private IsisLevelSettings(boolean wideMetricsOnly) {
    _wideMetricsOnly = wideMetricsOnly;
  }

  public IsisLevelSettings() {
    _wideMetricsOnly = false;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof IsisLevelSettings)) {
      return false;
    }
    IsisLevelSettings rhs = (IsisLevelSettings) obj;
    return _wideMetricsOnly == rhs._wideMetricsOnly;
  }

  @JsonProperty(PROP_WIDE_METRICS_ONLY)
  public boolean getWideMetricsOnly() {
    return _wideMetricsOnly;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_wideMetricsOnly);
  }

  @JsonProperty(PROP_WIDE_METRICS_ONLY)
  public void setWideMetricsOnly(boolean wideMetricsOnly) {
    _wideMetricsOnly = wideMetricsOnly;
  }
}

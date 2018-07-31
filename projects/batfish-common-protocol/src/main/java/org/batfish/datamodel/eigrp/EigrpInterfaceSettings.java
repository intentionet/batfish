package org.batfish.datamodel.eigrp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;

/** Settings for a {@link org.batfish.datamodel.Interface} with an {@link EigrpProcess}. */
public class EigrpInterfaceSettings implements Serializable {

  private static final String PROP_ASN = "asn";
  private static final String PROP_BANDWIDTH = "bandwidth";
  private static final String PROP_DELAY = "delay";
  private static final String PROP_ENABLED = "enabled";
  private static final String PROP_METRIC = "metric";
  private static final String PROP_PASSIVE = "passive";
  private static final long serialVersionUID = 1L;
  @Nullable private final Long _asn;
  @Nullable private final Double _bandwidth;
  @Nullable private final Double _delay;
  private final boolean _enabled;
  @Nullable private final EigrpMetric _metric;
  private final boolean _passive;

  @JsonCreator
  private EigrpInterfaceSettings(
      @Nullable @JsonProperty(PROP_ASN) Long asn,
      @Nullable @JsonProperty(PROP_BANDWIDTH) Double bandwidth,
      @Nullable @JsonProperty(PROP_DELAY) Double delay,
      @JsonProperty(PROP_ENABLED) boolean enabled,
      @Nullable @JsonProperty(PROP_METRIC) EigrpMetric metric,
      @JsonProperty(PROP_PASSIVE) boolean passive) {
    _asn = asn;
    _bandwidth = bandwidth;
    _delay = delay;
    _enabled = enabled;
    _metric = metric;
    _passive = passive;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof EigrpInterfaceSettings)) {
      return false;
    }
    EigrpInterfaceSettings rhs = (EigrpInterfaceSettings) obj;
    return Objects.equals(_asn, rhs._asn)
        && Objects.equals(_bandwidth, rhs._bandwidth)
        && Objects.equals(_delay, rhs._delay)
        && (_enabled == rhs._enabled)
        && Objects.equals(_metric, rhs._metric)
        && _passive == rhs._passive;
  }

  /** @return The AS number for this interface */
  @JsonProperty(PROP_ASN)
  @Nullable
  public Long getAsNumber() {
    return _asn;
  }

  /** @return The bandwidth for this interface */
  @JsonProperty(PROP_BANDWIDTH)
  @Nullable
  public Double getBandwidth() {
    return _bandwidth;
  }

  /** @return The delay for this interface */
  @JsonProperty(PROP_DELAY)
  @Nullable
  public Double getDelay() {
    if (_delay != null) {
      return _delay;
    }
    if (_metric != null) {
      return _metric.getDelay();
    }
    return null;
  }

  /** @return Whether EIGRP is enabled on this interface */
  @JsonProperty(PROP_ENABLED)
  public boolean getEnabled() {
    return _enabled;
  }

  /** @return The interface metric */
  @JsonProperty(PROP_METRIC)
  @Nullable
  public EigrpMetric getMetric() {
    return _metric;
  }

  /** @return Whether interface is passive */
  @JsonProperty(PROP_PASSIVE)
  public boolean getPassive() {
    return _passive;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_asn, _bandwidth, _delay, _enabled, _metric, _passive);
  }

  public static class Builder {

    @Nullable private Long _asn;

    @Nullable private Double _bandwidth;

    @Nullable private Double _delay;

    private boolean _enabled;

    @Nullable private EigrpMetric _metric;

    private boolean _passive;

    private Builder() {}

    public EigrpInterfaceSettings build() {
      return new EigrpInterfaceSettings(_asn, _bandwidth, _delay, _enabled, _metric, _passive);
    }

    public Builder setAsn(@Nullable Long asn) {
      _asn = asn;
      return this;
    }

    public Builder setDelay(@Nullable Double delay) {
      _delay = delay;
      return this;
    }

    public Builder setEnabled(boolean enabled) {
      _enabled = enabled;
      return this;
    }

    public Builder setMetric(@Nullable EigrpMetric metric) {
      _metric = metric;
      return this;
    }

    public Builder setBandwidth(@Nullable Double bandwidth) {
      _bandwidth = bandwidth;
      return this;
    }

    public Builder setPassive(boolean passive) {
      _passive = passive;
      return this;
    }
  }
}

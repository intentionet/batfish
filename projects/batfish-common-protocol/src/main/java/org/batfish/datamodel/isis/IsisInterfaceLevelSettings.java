package org.batfish.datamodel.isis;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Objects.requireNonNull;
import static org.batfish.datamodel.isis.IsisInterfaceMode.UNSET;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IsisInterfaceLevelSettings implements Serializable {

  public static class Builder {

    private Long _cost;

    private String _helloAuthenticationKey;

    private IsisHelloAuthenticationType _helloAuthenticationType;

    private Integer _helloInterval;

    private Integer _holdTime;

    private IsisInterfaceMode _mode;

    public IsisInterfaceLevelSettings build() {
      return new IsisInterfaceLevelSettings(
          _cost,
          _helloAuthenticationKey,
          _helloAuthenticationType,
          _helloInterval,
          _holdTime,
          requireNonNull(_mode));
    }

    public @Nonnull Builder setCost(@Nullable Long cost) {
      _cost = cost;
      return this;
    }

    public @Nonnull Builder setHelloAuthenticationKey(@Nullable String helloAuthenticationKey) {
      _helloAuthenticationKey = helloAuthenticationKey;
      return this;
    }

    public @Nonnull Builder setHelloAuthenticationType(
        @Nullable IsisHelloAuthenticationType helloAuthenticationType) {
      _helloAuthenticationType = helloAuthenticationType;
      return this;
    }

    public @Nonnull Builder setHelloInterval(@Nullable Integer helloInterval) {
      _helloInterval = helloInterval;
      return this;
    }

    public @Nonnull Builder setHoldTime(@Nullable Integer holdTime) {
      _holdTime = holdTime;
      return this;
    }

    public @Nonnull Builder setMode(@Nullable IsisInterfaceMode mode) {
      _mode = mode;
      return this;
    }
  }

  private static final String PROP_COST = "cost";

  private static final String PROP_HELLO_AUTHENTICATION_KEY = "helloAuthenticationKey";

  private static final String PROP_HELLO_AUTHENTICATION_TYPE = "helloAuthenticationType";

  private static final String PROP_HELLO_INTERVAL = "helloInterval";

  private static final String PROP_HOLD_TIME = "holdTime";

  private static final String PROP_MODE = "mode";

  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  @JsonCreator
  private static @Nonnull IsisInterfaceLevelSettings create(
      @JsonProperty(PROP_COST) Long cost,
      @JsonProperty(PROP_HELLO_AUTHENTICATION_KEY) String helloAuthenticationKey,
      @JsonProperty(PROP_HELLO_AUTHENTICATION_TYPE)
          IsisHelloAuthenticationType helloAuthenticationType,
      @JsonProperty(PROP_HELLO_INTERVAL) Integer helloInterval,
      @JsonProperty(PROP_HOLD_TIME) Integer holdTime,
      @JsonProperty(PROP_MODE) IsisInterfaceMode mode) {
    return new IsisInterfaceLevelSettings(
        cost,
        helloAuthenticationKey,
        helloAuthenticationType,
        helloInterval,
        holdTime,
        firstNonNull(mode, UNSET));
  }

  @Nullable private final Long _cost;

  @Nullable private final String _helloAuthenticationKey;

  @Nullable private final IsisHelloAuthenticationType _helloAuthenticationType;

  @Nullable private final Integer _helloInterval;

  @Nullable private final Integer _holdTime;

  @Nonnull private final IsisInterfaceMode _mode;

  private IsisInterfaceLevelSettings(
      @Nullable Long cost,
      @Nullable String helloAuthenticationKey,
      @Nullable IsisHelloAuthenticationType helloAuthenticationType,
      @Nullable Integer helloInterval,
      @Nullable Integer holdTime,
      @Nonnull IsisInterfaceMode mode) {
    _cost = cost;
    _helloAuthenticationKey = helloAuthenticationKey;
    _helloAuthenticationType = helloAuthenticationType;
    _helloInterval = helloInterval;
    _holdTime = holdTime;
    _mode = mode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof IsisInterfaceLevelSettings)) {
      return false;
    }
    IsisInterfaceLevelSettings rhs = (IsisInterfaceLevelSettings) obj;
    return Objects.equals(_cost, rhs._cost)
        && Objects.equals(_helloAuthenticationKey, rhs._helloAuthenticationKey)
        && Objects.equals(_helloAuthenticationType, rhs._helloAuthenticationType)
        && Objects.equals(_helloInterval, rhs._helloInterval)
        && Objects.equals(_holdTime, rhs._holdTime)
        && Objects.equals(_mode, rhs._mode);
  }

  @JsonProperty(PROP_COST)
  public @Nullable Long getCost() {
    return _cost;
  }

  @JsonProperty(PROP_HELLO_AUTHENTICATION_KEY)
  public @Nullable String getHelloAuthenticationKey() {
    return _helloAuthenticationKey;
  }

  @JsonProperty(PROP_HELLO_AUTHENTICATION_TYPE)
  public @Nullable IsisHelloAuthenticationType getHelloAuthenticationType() {
    return _helloAuthenticationType;
  }

  @JsonProperty(PROP_HELLO_INTERVAL)
  public @Nullable Integer getHelloInterval() {
    return _helloInterval;
  }

  @JsonProperty(PROP_HOLD_TIME)
  public @Nullable Integer getHoldTime() {
    return _holdTime;
  }

  @JsonProperty(PROP_MODE)
  public @Nullable IsisInterfaceMode getMode() {
    return _mode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _cost,
        _helloAuthenticationKey,
        _helloAuthenticationType,
        _helloInterval,
        _holdTime,
        _mode.ordinal());
  }
}

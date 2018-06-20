package org.batfish.datamodel;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;

public class IsisRoute extends AbstractRoute {

  public static class Builder extends AbstractRouteBuilder<Builder, IsisRoute> {

    private String _area;

    private boolean _down;

    private IsisLevel _level;

    private RoutingProtocol _protocol;

    @Override
    public IsisRoute build() {
      return new IsisRoute(
          getAdmin(),
          requireNonNull(_area),
          _down,
          requireNonNull(_level),
          getMetric(),
          requireNonNull(getNetwork()),
          requireNonNull(getNextHopIp()),
          requireNonNull(_protocol));
    }

    @Override
    protected Builder getThis() {
      return this;
    }

    public Builder setArea(@Nonnull String area) {
      _area = area;
      return this;
    }

    public Builder setDown(boolean down) {
      _down = down;
      return this;
    }

    public Builder setLevel(@Nonnull IsisLevel level) {
      _level = level;
      return this;
    }

    public Builder setProtocol(@Nonnull RoutingProtocol protocol) {
      _protocol = protocol;
      return this;
    }
  }

  public static final long DEFAULT_METRIC = 10L;

  private static final String PROP_AREA = "area";

  private static final String PROP_DOWN = "down";

  private static final String PROP_LEVEL = "level";

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static @Nonnull IsisRoute createIsisRoute(
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int administrativeCost,
      @JsonProperty(PROP_AREA) String area,
      @JsonProperty(PROP_DOWN) boolean down,
      @JsonProperty(PROP_LEVEL) IsisLevel level,
      @JsonProperty(PROP_METRIC) long metric,
      @JsonProperty(PROP_NETWORK) Prefix network,
      @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @JsonProperty(PROP_PROTOCOL) RoutingProtocol protocol) {
    return new IsisRoute(
        administrativeCost,
        requireNonNull(area),
        down,
        requireNonNull(level),
        metric,
        requireNonNull(network),
        requireNonNull(nextHopIp),
        requireNonNull(protocol));
  }

  private final int _administrativeCost;

  private final String _area;

  private final boolean _down;

  private final IsisLevel _level;

  private final long _metric;

  private final Ip _nextHopIp;

  private final RoutingProtocol _protocol;

  private IsisRoute(
      int administrativeCost,
      @Nonnull String area,
      boolean down,
      @Nonnull IsisLevel level,
      long metric,
      @Nonnull Prefix network,
      @Nonnull Ip nextHopIp,
      @Nonnull RoutingProtocol protocol) {
    super(network);
    _administrativeCost = administrativeCost;
    _area = area;
    _down = down;
    _level = level;
    _metric = metric;
    _nextHopIp = nextHopIp;
    _protocol = protocol;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IsisRoute)) {
      return false;
    }
    IsisRoute rhs = (IsisRoute) o;
    return _administrativeCost == rhs._administrativeCost
        && _area == rhs._area
        && _down == rhs._down
        && _level == rhs._level
        && _metric == rhs._metric
        && _network.equals(rhs._network)
        && _nextHopIp.equals(rhs._nextHopIp)
        && _protocol == rhs._protocol;
  }

  @JsonIgnore(false)
  @JsonProperty(PROP_ADMINISTRATIVE_COST)
  @Override
  public int getAdministrativeCost() {
    return _administrativeCost;
  }

  @JsonProperty(PROP_AREA)
  public @Nonnull String getArea() {
    return _area;
  }

  @JsonProperty(PROP_DOWN)
  public boolean getDown() {
    return _down;
  }

  @JsonProperty(PROP_LEVEL)
  public @Nonnull IsisLevel getLevel() {
    return _level;
  }

  @JsonIgnore(false)
  @JsonProperty(PROP_METRIC)
  @Override
  public @Nonnull Long getMetric() {
    return _metric;
  }

  @Override
  public String getNextHopInterface() {
    return Route.UNSET_NEXT_HOP_INTERFACE;
  }

  @Override
  public @Nonnull Ip getNextHopIp() {
    return _nextHopIp;
  }

  @JsonIgnore(false)
  @JsonProperty(PROP_PROTOCOL)
  @Override
  public @Nonnull RoutingProtocol getProtocol() {
    return _protocol;
  }

  @Override
  public int getTag() {
    return NO_TAG;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _administrativeCost,
        _area,
        _down,
        _level.ordinal(),
        _metric,
        _nextHopIp,
        _protocol.ordinal());
  }

  @Override
  protected String protocolRouteString() {
    return String.format(
        " %s:%s %s:%s %s:%s", PROP_AREA, _area, PROP_DOWN, _down, PROP_LEVEL, _level);
  }

  @Override
  public int routeCompare(AbstractRoute rhs) {
    if (getClass() != rhs.getClass()) {
      return 0;
    }
    IsisRoute castRhs = (IsisRoute) rhs;
    return Comparator.comparing(IsisRoute::getArea)
        .thenComparing(IsisRoute::getDown)
        .thenComparing(IsisRoute::getLevel)
        .compare(this, castRhs);
  }
}

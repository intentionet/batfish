package org.batfish.datamodel;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.batfish.datamodel.isis.IsisLevel;

public class IsisRoute extends AbstractRoute {

  public static class Builder extends AbstractRouteBuilder<Builder, IsisRoute> {

    private String _area;

    private boolean _attach;

    private boolean _down;

    private IsisLevel _level;

    private RoutingProtocol _protocol;

    private String _systemId;

    @Override
    public IsisRoute build() {
      return new IsisRoute(
          getAdmin(),
          requireNonNull(_area),
          _attach,
          _down,
          requireNonNull(_level),
          getMetric(),
          requireNonNull(getNetwork()),
          requireNonNull(getNextHopIp()),
          requireNonNull(_protocol),
          requireNonNull(_systemId));
    }

    @Override
    protected Builder getThis() {
      return this;
    }

    public Builder setArea(@Nonnull String area) {
      _area = area;
      return this;
    }

    public Builder setAttach(boolean attach) {
      _attach = attach;
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

    public Builder setSystemId(@Nonnull String systemId) {
      _systemId = systemId;
      return this;
    }
  }

  public static final long DEFAULT_METRIC = 10L;

  private static final String PROP_AREA = "area";

  private static final String PROP_ATTACH = "attach";

  private static final String PROP_DOWN = "down";

  private static final String PROP_LEVEL = "level";

  private static final String PROP_SYSTEM_ID = "systemId";

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static @Nonnull IsisRoute createIsisRoute(
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int administrativeCost,
      @JsonProperty(PROP_AREA) String area,
      @JsonProperty(PROP_ATTACH) boolean attach,
      @JsonProperty(PROP_DOWN) boolean down,
      @JsonProperty(PROP_LEVEL) IsisLevel level,
      @JsonProperty(PROP_METRIC) long metric,
      @JsonProperty(PROP_NETWORK) Prefix network,
      @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @JsonProperty(PROP_PROTOCOL) RoutingProtocol protocol,
      @JsonProperty(PROP_SYSTEM_ID) String systemId) {
    return new IsisRoute(
        administrativeCost,
        requireNonNull(area),
        attach,
        down,
        requireNonNull(level),
        metric,
        requireNonNull(network),
        requireNonNull(nextHopIp),
        requireNonNull(protocol),
        requireNonNull(systemId));
  }

  private final int _administrativeCost;

  private final String _area;

  private final boolean _attach;

  private final boolean _down;

  private final IsisLevel _level;

  private final long _metric;

  private final Ip _nextHopIp;

  private final RoutingProtocol _protocol;

  private final String _systemId;

  private IsisRoute(
      int administrativeCost,
      @Nonnull String area,
      boolean attach,
      boolean down,
      @Nonnull IsisLevel level,
      long metric,
      @Nonnull Prefix network,
      @Nonnull Ip nextHopIp,
      @Nonnull RoutingProtocol protocol,
      @Nonnull String systemId) {
    super(network);
    _administrativeCost = administrativeCost;
    _area = area;
    _attach = attach;
    _down = down;
    _level = level;
    _metric = metric;
    _nextHopIp = nextHopIp;
    _protocol = protocol;
    _systemId = systemId;
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
        && _area.equals(rhs._area)
        && _attach == rhs._attach
        && _down == rhs._down
        && _level == rhs._level
        && _metric == rhs._metric
        && _network.equals(rhs._network)
        && _nextHopIp.equals(rhs._nextHopIp)
        && _protocol == rhs._protocol
        && _systemId.equals(rhs._systemId);
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

  /** Attach bit is set on default route originated by L1L2 routers to L1 neighbors. */
  @JsonProperty(PROP_ATTACH)
  public boolean getAttach() {
    return _attach;
  }

  /**
   * A "down" bit indicating that this route has already been leaked from level 2 down to level 1.
   */
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

  @Nonnull
  @JsonProperty(PROP_SYSTEM_ID)
  public String getSystemId() {
    return _systemId;
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
        _attach,
        _down,
        _level.ordinal(),
        _metric,
        _nextHopIp,
        _protocol.ordinal(),
        _systemId);
  }

  @Override
  protected String protocolRouteString() {
    return String.format(
        " %s:%s %s:%s %s:%s %s:%s %s:%s",
        PROP_AREA,
        _area,
        PROP_ATTACH,
        _attach,
        PROP_DOWN,
        _down,
        PROP_LEVEL,
        _level,
        PROP_SYSTEM_ID,
        _systemId);
  }

  @Override
  public int routeCompare(AbstractRoute rhs) {
    if (getClass() != rhs.getClass()) {
      return 0;
    }
    IsisRoute castRhs = (IsisRoute) rhs;
    return Comparator.comparing(IsisRoute::getArea)
        .thenComparing(IsisRoute::getAttach)
        .thenComparing(IsisRoute::getDown)
        .thenComparing(IsisRoute::getLevel)
        .thenComparing(IsisRoute::getSystemId)
        .compare(this, castRhs);
  }
}

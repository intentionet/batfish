package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StaticRoute extends AbstractRoute {

  private static final String PROP_NEXT_HOP_INTERFACE = "nextHopInterface";

  private static final long serialVersionUID = 1L;

  private final int _administrativeCost;

  @Nonnull private final String _nextHopInterface;

  @Nonnull private final Ip _nextHopIp;

  private final int _tag;

  @JsonCreator
  public StaticRoute(
      @JsonProperty(PROP_NETWORK) Prefix network,
      @Nullable @JsonProperty(PROP_NEXT_HOP_IP) Ip nextHopIp,
      @Nullable @JsonProperty(PROP_NEXT_HOP_INTERFACE) String nextHopInterface,
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int administrativeCost,
      @JsonProperty(PROP_TAG) int tag) {
    super(network);
    _administrativeCost = administrativeCost;
    _nextHopInterface = firstNonNull(nextHopInterface, Route.UNSET_NEXT_HOP_INTERFACE);
    _nextHopIp = firstNonNull(nextHopIp, Route.UNSET_ROUTE_NEXT_HOP_IP);
    _tag = tag;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof StaticRoute)) {
      return false;
    }
    StaticRoute rhs = (StaticRoute) o;
    boolean res = _network.equals(rhs._network);
    res = res && _administrativeCost == rhs._administrativeCost;
    res = res && _nextHopIp.equals(rhs._nextHopIp);
    res = res && _nextHopInterface.equals(rhs._nextHopInterface);
    return res && _tag == rhs._tag;
  }

  @Override
  @JsonIgnore(false)
  @JsonProperty(PROP_ADMINISTRATIVE_COST)
  public int getAdministrativeCost() {
    return _administrativeCost;
  }

  @Override
  @JsonIgnore
  public Long getMetric() {
    return 0L;
  }

  @Nonnull
  @Override
  @JsonIgnore(false)
  @JsonProperty(PROP_NEXT_HOP_INTERFACE)
  public String getNextHopInterface() {
    return _nextHopInterface;
  }

  @Nonnull
  @JsonIgnore(false)
  @JsonProperty(PROP_NEXT_HOP_IP)
  @Override
  public Ip getNextHopIp() {
    return _nextHopIp;
  }

  @Override
  public RoutingProtocol getProtocol() {
    return RoutingProtocol.STATIC;
  }

  @Override
  @JsonIgnore(false)
  @JsonProperty(PROP_TAG)
  public int getTag() {
    return _tag;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _network.hashCode();
    result = prime * result + _administrativeCost;
    result = prime * result + _nextHopInterface.hashCode();
    result = prime * result + _nextHopIp.hashCode();
    result = prime * result + _tag;
    return result;
  }

  @Override
  protected final String protocolRouteString() {
    return " tag:" + _tag;
  }

  @Override
  public int routeCompare(AbstractRoute rhs) {
    return 0;
  }

  public static class Builder extends AbstractRouteBuilder<Builder, StaticRoute> {

    private int _administrativeCost = Route.UNSET_ROUTE_ADMIN;
    private String _nextHopInterface = Route.UNSET_NEXT_HOP_INTERFACE;

    private Builder() {}

    @Override
    public StaticRoute build() {
      return new StaticRoute(
          getNetwork(), getNextHopIp(), _nextHopInterface, _administrativeCost, getTag());
    }

    @Override
    protected Builder getThis() {
      return this;
    }

    public Builder setAdministrativeCost(int administrativeCost) {
      _administrativeCost = administrativeCost;
      return this;
    }

    public Builder setNextHopInterface(String nextHopInterface) {
      _nextHopInterface = nextHopInterface;
      return this;
    }
  }
}

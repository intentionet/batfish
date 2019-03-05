package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/** Non-routing virtual route used for advertisement */
@ParametersAreNonnullByDefault
public final class KernelRoute extends AbstractRoute {

  /** Builder for {@link KernelRoute} */
  public static final class Builder extends AbstractRouteBuilder<Builder, KernelRoute> {
    @Nonnull
    @Override
    public KernelRoute build() {
      return new KernelRoute(getNetwork());
    }

    @Nonnull
    @Override
    protected Builder getThis() {
      return this;
    }
  }

  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  @JsonCreator
  private static KernelRoute create(
      @Nullable @JsonProperty(PROP_NETWORK) Prefix network,
      @Nullable @JsonProperty(PROP_NEXT_HOP_INTERFACE) String nextHopInterface,
      @JsonProperty(PROP_ADMINISTRATIVE_COST) int adminCost) {
    checkArgument(network != null, "Cannot create kernel route: missing %s", PROP_NETWORK);
    return new KernelRoute(network);
  }

  public KernelRoute(Prefix network) {
    super(network, 0, false, true);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof KernelRoute)) {
      return false;
    }
    KernelRoute rhs = (KernelRoute) o;
    return _network.equals(rhs._network);
  }

  @Override
  public int hashCode() {
    return _network.hashCode();
  }

  @Override
  public Long getMetric() {
    return 0L;
  }

  @JsonProperty(PROP_NEXT_HOP_INTERFACE)
  @Nullable
  @Override
  public String getNextHopInterface() {
    return Route.UNSET_NEXT_HOP_INTERFACE;
  }

  @Nonnull
  @Override
  public Ip getNextHopIp() {
    return Route.UNSET_ROUTE_NEXT_HOP_IP;
  }

  @Override
  public RoutingProtocol getProtocol() {
    return RoutingProtocol.KERNEL;
  }

  @Override
  public int getTag() {
    return NO_TAG;
  }

  @Override
  public int routeCompare(@Nonnull AbstractRoute rhs) {
    return 0;
  }

  @Override
  public Builder toBuilder() {
    return builder().setNetwork(getNetwork());
  }
}

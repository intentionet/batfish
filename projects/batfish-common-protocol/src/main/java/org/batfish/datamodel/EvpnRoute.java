package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.BgpRoute.Builder;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;

/**
 * A generic EVPN route containing the common properties among the different types of EVPN routes
 */
@ParametersAreNonnullByDefault
public abstract class EvpnRoute<B extends Builder<B, R>, R extends BgpRoute<B, R>>
    extends BgpRoute<B, R> {

  /** Builder for {@link EvpnRoute} */
  @ParametersAreNonnullByDefault
  public abstract static class Builder<B extends Builder<B, R>, R extends EvpnRoute<B, R>>
      extends BgpRoute.Builder<B, R> {

    @Nullable protected RouteDistinguisher _routeDistinguisher;

    @Nullable
    public RouteDistinguisher getRouteDistinguisher() {
      return _routeDistinguisher;
    }

    public B setRouteDistinguisher(RouteDistinguisher routeDistinguisher) {
      _routeDistinguisher = routeDistinguisher;
      return getThis();
    }

    @Nonnull
    @Override
    public abstract R build();
  }

  static final String PROP_ROUTE_DISTINGUISHER = "routeDistinguisher";
  @Nonnull protected final RouteDistinguisher _routeDistinguisher;

  protected EvpnRoute(
      Prefix network,
      Ip nextHopIp,
      int admin,
      AsPath asPath,
      Set<Community> communities,
      boolean discard,
      long localPreference,
      long med,
      String nextHopInterface,
      Ip originatorIp,
      Set<Long> clusterList,
      boolean receivedFromRouteReflectorClient,
      OriginType originType,
      RoutingProtocol protocol,
      @Nullable Ip receivedFromIp,
      @Nullable RoutingProtocol srcProtocol,
      long tag,
      int weight,
      boolean nonForwarding,
      boolean nonRouting,
      RouteDistinguisher routeDistinguisher) {
    super(
        network,
        nextHopIp,
        admin,
        asPath,
        communities,
        discard,
        localPreference,
        med,
        nextHopInterface,
        originatorIp,
        clusterList,
        receivedFromRouteReflectorClient,
        originType,
        protocol,
        receivedFromIp,
        srcProtocol,
        tag,
        weight,
        nonForwarding,
        nonRouting);
    _routeDistinguisher = routeDistinguisher;
  }

  @Nonnull
  @JsonProperty(PROP_ROUTE_DISTINGUISHER)
  public RouteDistinguisher getRouteDistinguisher() {
    return _routeDistinguisher;
  }

  /** Return extended communities that are route targets for this route */
  @JsonIgnore
  public Set<ExtendedCommunity> getRouteTargets() {
    return _communities.stream()
        .filter(c -> c instanceof ExtendedCommunity)
        .map(ExtendedCommunity.class::cast)
        .filter(ExtendedCommunity::isRouteTarget)
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public abstract B toBuilder();
}

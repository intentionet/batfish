package org.batfish.dataplane.protocols;

import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AbstractRouteDecorator;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Bgpv4Route.Builder;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.bgp.AddressFamily;
import org.batfish.datamodel.bgp.AddressFamily.Type;
import org.batfish.datamodel.bgp.BgpTopologyUtils.ConfedSessionType;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;

@ParametersAreNonnullByDefault
public final class BgpProtocolHelper {

  /**
   * Perform BGP export transformations on a given route when sending an advertisement from {@code
   * localNeighbor} to {@code remoteNeighbor} before export policy is applied.
   *
   * @param localNeighbor {@link BgpPeerConfig} exporting {@code route}
   * @param remoteNeighbor {@link BgpPeerConfig} to which to export {@code route}
   * @param sessionProperties {@link BgpSessionProperties} representing the <em>incoming</em> edge:
   *     i.e. the edge from {@code remoteNeighbor} to {@code localNeighbor}
   * @param afType {@link AddressFamily.Type} the address family for which to look up the settings
   */
  @Nullable
  public static <R extends BgpRoute<B, R>, B extends BgpRoute.Builder<B, R>>
      B transformBgpRoutePreExport(
          BgpPeerConfig localNeighbor,
          BgpPeerConfig remoteNeighbor,
          BgpSessionProperties sessionProperties,
          BgpProcess localBgpProcess,
          BgpProcess remoteBgpProcess,
          BgpRoute<B, R> route,
          Type afType) {

    // Make a new builder
    B builder = route.toBuilder();
    // this will be set later during export policy transformation or after it is exported
    builder.setNextHopIp(UNSET_ROUTE_NEXT_HOP_IP);

    // sessionProperties represents incoming edge, so localNeighbor's IP is its headIp
    Ip fromNeighborIp = sessionProperties.getHeadIp();
    RoutingProtocol routeProtocol = route.getProtocol();

    builder.setReceivedFromIp(fromNeighborIp);
    RoutingProtocol outgoingProtocol =
        sessionProperties.isEbgp() ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
    builder.setProtocol(outgoingProtocol);
    builder.setSrcProtocol(route.getProtocol());

    // Clear a bunch of non-transitive attributes
    builder.setWeight(0);
    builder.setDiscard(false);
    builder.setNonRouting(false);
    builder.setNonForwarding(false);
    builder.setAdmin(remoteBgpProcess.getAdminCost(outgoingProtocol));
    builder.setTag(null);

    // Set originatorIP
    if (sessionProperties.isEbgp() || !routeProtocol.equals(RoutingProtocol.IBGP)) {
      // eBGP session and not iBGP route: override the originator
      builder.setOriginatorIp(localBgpProcess.getRouterId());
    }

    // note whether new route is received from route reflector client
    AddressFamily toNeighborAf = remoteNeighbor.getAddressFamily(afType);
    assert toNeighborAf
        != null; // invariant of proper queue setup and route exchange for this AF type
    builder.setReceivedFromRouteReflectorClient(
        !sessionProperties.isEbgp() && toNeighborAf.getRouteReflectorClient());

    AddressFamily af = localNeighbor.getAddressFamily(afType);
    assert af != null;

    // Do not export route if it has NO_ADVERTISE community, or if its AS path contains the remote
    // peer's AS and local peer has not set getAllowRemoteOut
    if (route.getCommunities().getCommunities().contains(StandardCommunity.NO_ADVERTISE)
        || (sessionProperties.isEbgp()
            && route.getAsPath().containsAs(sessionProperties.getTailAs())
            && !af.getAddressFamilyCapabilities().getAllowRemoteAsOut())) {
      return null;
    }
    // Also do not export if route has NO_EXPORT community and this is a true ebgp session
    if (route.getCommunities().getCommunities().contains(StandardCommunity.NO_EXPORT)
        && sessionProperties.isEbgp()
        && sessionProperties.getConfedSessionType() != ConfedSessionType.WITHIN_CONFED) {
      return null;
    }

    // Set transformed route's communities
    builder.setCommunities(ImmutableSet.of());
    if (af.getAddressFamilyCapabilities().getSendCommunity()) {
      builder.addCommunities(route.getStandardCommunities());
    }
    if (af.getAddressFamilyCapabilities().getSendExtendedCommunity()) {
      builder.addCommunities(route.getExtendedCommunities());
    }

    /*
     *  iBGP speaker should not send out routes to iBGP neighbor whose router-id is
     *  same as originator id of advertisement
     */
    if (!sessionProperties.isEbgp()
        && remoteBgpProcess.getRouterId().equals(route.getOriginatorIp())) {
      return null;
    }

    builder.setClusterList(ImmutableSet.of());
    if (routeProtocol.equals(RoutingProtocol.IBGP) && !sessionProperties.isEbgp()) {
      /*
       * The remote route is iBGP. The session is iBGP. We consider whether to reflect, and
       * modify the outgoing route as appropriate.
       *
       * For route reflection: reflect everything received from
       * clients to clients and non-clients. reflect everything
       * received from non-clients to clients. Do not reflect to
       * originator
       */
      boolean remoteRouteReceivedFromRouteReflectorClient =
          route.getReceivedFromRouteReflectorClient();
      boolean sendingToRouteReflectorClient = af.getRouteReflectorClient();
      Ip remoteReceivedFromIp = route.getReceivedFromIp();
      boolean remoteRouteOriginatedByRemoteNeighbor = Ip.ZERO.equals(remoteReceivedFromIp);
      if (!remoteRouteReceivedFromRouteReflectorClient
          && !sendingToRouteReflectorClient
          && !remoteRouteOriginatedByRemoteNeighbor) {
        /*
         * Neither reflecting nor originating this iBGP route, so don't send
         */
        return null;
      }
      builder.addClusterList(route.getClusterList());
      if (!remoteRouteOriginatedByRemoteNeighbor) {
        // we are reflecting, so we need to get the clusterid associated with the
        // remoteRoute
        Long newClusterId = localNeighbor.getClusterId();
        if (newClusterId != null) {
          builder.addToClusterList(newClusterId);
        }
      }
      Set<Long> localClusterIds = remoteBgpProcess.getClusterIds();
      Set<Long> outgoingClusterList = builder.getClusterList();
      if (localClusterIds.stream().anyMatch(outgoingClusterList::contains)) {
        /*
         *  receiver will reject new route if it contains any of its local cluster ids
         */
        return null;
      }
    }

    // Outgoing metric (MED) is preserved only if advertising to IBGP peer or within a confederation
    if (!sessionProperties.advertiseUnchangedMed()) {
      builder.setMetric(0);
    }

    // Local preference: only transitive for iBGP or within a confederation
    builder.setLocalPreference(
        sessionProperties.advertiseUnchangedLocalPref()
            ? route.getLocalPreference()
            : BgpRoute.DEFAULT_LOCAL_PREFERENCE);

    return builder;
  }

  /**
   * Perform BGP import transformations on a given route after receiving an advertisement.
   *
   * <p>Return {@code null} if the route should not be imported.
   */
  @Nullable
  public static <R extends BgpRoute<B, R>, B extends BgpRoute.Builder<B, R>>
      B transformBgpRouteOnImport(
          BgpRoute<B, R> route,
          long localAs,
          boolean allowLocalAsIn,
          boolean isEbgp,
          BgpProcess toProcess,
          @Nullable String peerInterface) {
    // skip routes containing peer's AS unless explicitly allowed
    if (!allowLocalAsIn && route.getAsPath().containsAs(localAs)) {
      return null;
    }

    RoutingProtocol targetProtocol = isEbgp ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
    B builder = route.toBuilder();

    if (peerInterface != null) {
      builder.setNextHopInterface(peerInterface);
    } else {
      builder.setNextHopInterface(Route.UNSET_NEXT_HOP_INTERFACE);
    }
    builder.setAdmin(toProcess.getAdminCost(targetProtocol));
    builder.setProtocol(targetProtocol);
    builder.setSrcProtocol(targetProtocol);

    return builder;
  }

  /**
   * Convert an aggregate/generated route to a BGP route.
   *
   * @param generatedRoute a {@link GeneratedRoute} to convert to a {@link Bgpv4Route}.
   * @param attributePolicy a {@link RoutingPolicy} to use to set BGP route attributes after
   *     conversion
   * @param routerId Router ID to set as the originatorIp for the resulting BGP route.
   * @param nonRouting Whether to mark the Bgpv4Route as non-routing
   */
  @Nonnull
  public static Bgpv4Route convertGeneratedRouteToBgp(
      GeneratedRoute generatedRoute,
      @Nullable RoutingPolicy attributePolicy,
      Ip routerId,
      Ip nextHopIp,
      boolean nonRouting) {
    Builder builder = convertGeneratedRouteToBgp(generatedRoute, routerId, nextHopIp, nonRouting);
    if (attributePolicy == null) {
      return builder.build();
    }
    builder.setNextHopIp(UNSET_ROUTE_NEXT_HOP_IP);
    boolean accepted = attributePolicy.process(builder.build(), builder, Direction.OUT);
    assert accepted;
    return builder.setNextHopIp(nextHopIp).build();
  }

  /**
   * Convert an aggregate/generated route to a BGP route builder.
   *
   * @param generatedRoute a {@link GeneratedRoute} to convert to a {@link Bgpv4Route}.
   * @param routerId Router ID to set as the originatorIp for the resulting BGP route.
   * @param nonRouting Whether to mark the Bgpv4Route as non-routing
   */
  @Nonnull
  @VisibleForTesting
  static Builder convertGeneratedRouteToBgp(
      GeneratedRoute generatedRoute, Ip routerId, Ip nextHopIp, boolean nonRouting) {
    return Bgpv4Route.builder()
        .setAdmin(generatedRoute.getAdministrativeCost())
        .setAsPath(generatedRoute.getAsPath())
        .setCommunities(generatedRoute.getCommunities())
        .setMetric(generatedRoute.getMetric())
        .setSrcProtocol(RoutingProtocol.AGGREGATE)
        .setProtocol(RoutingProtocol.AGGREGATE)
        .setNextHopIp(nextHopIp)
        .setNetwork(generatedRoute.getNetwork())
        .setLocalPreference(Bgpv4Route.DEFAULT_LOCAL_PREFERENCE)
        /*
         * Note: Origin type and originator IP should get overwritten by export policy,
         * but are needed initially
         */
        .setOriginatorIp(routerId)
        .setOriginType(OriginType.INCOMPLETE)
        .setReceivedFromIp(nextHopIp)
        .setNonRouting(nonRouting);
  }

  /**
   * Convert a route that is neither a {@link BgpRoute} nor a {@link GeneratedRoute} to a {@link
   * Bgpv4Route.Builder}.
   *
   * <p>Intended for converting main RIB routes into their BGP equivalents before passing {@code
   * routeDecorator} to the export policy
   *
   * <p>The builder returned will have default local preference, incomplete origin type, and most
   * other fields unset.
   */
  @Nonnull
  public static Bgpv4Route.Builder convertNonBgpRouteToBgpRoute(
      AbstractRouteDecorator routeDecorator,
      Ip routerId,
      Ip nextHopIp,
      int adminDistance,
      RoutingProtocol protocol) {
    assert protocol == RoutingProtocol.BGP || protocol == RoutingProtocol.IBGP;
    assert !(routeDecorator.getAbstractRoute() instanceof BgpRoute);
    AbstractRoute route = routeDecorator.getAbstractRoute();
    return Bgpv4Route.builder()
        .setNetwork(route.getNetwork())
        .setAdmin(adminDistance)
        .setOriginatorIp(routerId)
        .setProtocol(protocol)
        .setSrcProtocol(route.getProtocol())
        .setOriginType(OriginType.INCOMPLETE)
        // TODO: support customization of route preference
        .setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE)
        .setReceivedFromIp(protocol == RoutingProtocol.BGP ? nextHopIp : Ip.ZERO)
        .setNextHopIp(nextHopIp)
        .setMetric(route.getMetric())
        .setTag(routeDecorator.getAbstractRoute().getTag());
    // Let everything else default to unset/empty/etc.
  }

  /**
   * Perform BGP export transformations on a given route <em>after</em> export policy has been *
   * applied to the route, route was accepted, but before route is sent "onto the wire".
   *
   * @param routeBuilder Builder for the output (exported) route
   * @param isEbgp true for ebgp sessions
   * @param confedSessionType type of confederation session, if any
   * @param localAs local AS
   * @param localIp IP of the neighbor which is exporting the route
   * @param originalRouteNhip Next hop IP of the original route
   */
  public static <R extends BgpRoute<B, R>, B extends BgpRoute.Builder<B, R>>
      void transformBgpRoutePostExport(
          B routeBuilder,
          boolean isEbgp,
          ConfedSessionType confedSessionType,
          long localAs,
          Ip localIp,
          Ip originalRouteNhip) {
    // if eBGP, prepend as-path sender's as-path number
    if (isEbgp) {
      AsSet asSetToPrepend =
          confedSessionType == ConfedSessionType.WITHIN_CONFED
              ? AsSet.confed(localAs)
              : AsSet.of(localAs);

      // Remove any confederations if propagating route outside of the confederation border
      AsPath routeAsPath = routeBuilder.getAsPath();
      if (confedSessionType.equals(ConfedSessionType.ACROSS_CONFED_BORDER)) {
        routeAsPath = routeAsPath.removeConfederations();
      }

      routeBuilder.setAsPath(
          AsPath.of(
              ImmutableList.<AsSet>builder()
                  .add(asSetToPrepend)
                  .addAll(routeAsPath.getAsSets())
                  .build()));
      // Tags are non-transitive
      routeBuilder.setTag(null);
    }

    // Skip setting our own next hop if it has already been set by the routing policy
    if (routeBuilder.getNextHopIp().equals(UNSET_ROUTE_NEXT_HOP_IP)) {
      if (isEbgp) {
        routeBuilder.setNextHopIp(localIp);
      } else { // iBGP session
        /*
        Note: implementation of next-hop-self in the general case is delegated to routing
        policy.
        If original route has next-hop ip, preserve it. If not, set our own.
        */
        routeBuilder.setNextHopIp(
            originalRouteNhip.equals(UNSET_ROUTE_NEXT_HOP_IP) ? localIp : originalRouteNhip);
      }
    }

    /*
    Routes can be aggregate only when generated locally. When transiting across nodes they must be BGP or
    IBGP.

    This is a bit of a hack since we currently overload AGGREGATE to mean aggregate protocol (i.e., just
    like on Juniper) AND to mean "locally generated". :(
    */
    if (routeBuilder.getProtocol() == RoutingProtocol.AGGREGATE) {
      routeBuilder.setProtocol(isEbgp ? RoutingProtocol.BGP : RoutingProtocol.IBGP);
    }
  }

  private BgpProtocolHelper() {}
}

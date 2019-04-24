package org.batfish.dataplane.protocols;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.WellKnownCommunity;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.dataplane.exceptions.BgpRoutePropagationException;

public class BgpProtocolHelper {

  /**
   * Perform BGP export transformations on a given route when sending an advertisement from {@code
   * fromNeighbor} to {@code toNeighbor} before export policy is applied.
   *
   * @param fromNeighbor {@link BgpPeerConfig} exporting {@code route}
   * @param toNeighbor {@link BgpPeerConfig} to which to export {@code route}
   * @param sessionProperties {@link BgpSessionProperties} representing the <em>incoming</em> edge:
   *     i.e. the edge from {@code toNeighbor} to {@code fromNeighbor}
   */
  @Nullable
  public static BgpRoute.Builder transformBgpRoutePreExport(
      BgpPeerConfig fromNeighbor,
      BgpPeerConfig toNeighbor,
      BgpSessionProperties sessionProperties,
      Vrf fromVrf,
      Vrf toVrf,
      AbstractRoute route)
      throws BgpRoutePropagationException {

    BgpRoute.Builder transformedOutgoingRouteBuilder = new BgpRoute.Builder();

    // sessionProperties represents incoming edge, so fromNeighbor's IP is its headIp
    Ip fromNeighborIp = sessionProperties.getHeadIp();

    // Set the tag
    transformedOutgoingRouteBuilder.setTag(route.getTag());

    transformedOutgoingRouteBuilder.setReceivedFromIp(fromNeighborIp);
    RoutingProtocol remoteRouteProtocol = route.getProtocol();

    boolean remoteRouteIsBgp =
        remoteRouteProtocol == RoutingProtocol.IBGP || remoteRouteProtocol == RoutingProtocol.BGP;

    // Set originatorIP
    Ip originatorIp;
    if (!sessionProperties.isEbgp() && remoteRouteProtocol.equals(RoutingProtocol.IBGP)) {
      BgpRoute bgpRemoteRoute = (BgpRoute) route;
      originatorIp = bgpRemoteRoute.getOriginatorIp();
    } else {
      originatorIp = fromVrf.getBgpProcess().getRouterId();
    }
    transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);

    // note whether new route is received from route reflector client
    transformedOutgoingRouteBuilder.setReceivedFromRouteReflectorClient(
        !sessionProperties.isEbgp() && toNeighbor.getRouteReflectorClient());

    // Extract original route's asPath and communities if it had them
    AsPath originalAsPath = AsPath.empty();
    SortedSet<Community> originalCommunities = ImmutableSortedSet.of();
    if (route instanceof BgpRoute) {
      // Includes all routes with protocols BGP and IBGP, plus some with protocol AGGREGATE
      BgpRoute bgpRemoteRoute = (BgpRoute) route;
      originalAsPath = bgpRemoteRoute.getAsPath();
      originalCommunities = bgpRemoteRoute.getCommunities();
    } else if (route instanceof GeneratedRoute) {
      // Includes all other AGGREGATE routes
      GeneratedRoute gr = (GeneratedRoute) route;
      originalAsPath = gr.getAsPath();
      originalCommunities = gr.getCommunities();
    }
    // Do not export route if it has NO_ADVERTISE community, or if its AS path contains the remote
    // peer's AS and local peer has not set getAllowRemoteOut
    if (originalCommunities.contains(StandardCommunity.of(WellKnownCommunity.NO_ADVERTISE))
        || (sessionProperties.isEbgp()
            && originalAsPath.containsAs(toNeighbor.getLocalAs())
            && !fromNeighbor.getAllowRemoteAsOut())) {
      return null;
    }
    // Set transformed route's AS path and communities
    transformedOutgoingRouteBuilder.setAsPath(originalAsPath);
    if (fromNeighbor.getSendCommunity()) {
      transformedOutgoingRouteBuilder.addCommunities(originalCommunities);
    }

    // clusterList, receivedFromRouteReflectorClient, (originType for bgp remote route)
    if (remoteRouteIsBgp) {
      BgpRoute bgpRemoteRoute = (BgpRoute) route;

      transformedOutgoingRouteBuilder.setOriginType(bgpRemoteRoute.getOriginType());
      /*
       * route reflection: reflect everything received from
       * clients to clients and non-clients. reflect everything
       * received from non-clients to clients. Do not reflect to
       * originator
       */

      Ip remoteOriginatorIp = bgpRemoteRoute.getOriginatorIp();
      /*
       *  iBGP speaker should not send out routes to iBGP neighbor whose router-id is
       *  same as originator id of advertisement
       */
      if (!sessionProperties.isEbgp()
          && toVrf.getBgpProcess().getRouterId().equals(remoteOriginatorIp)) {
        return null;
      }
      if (remoteRouteProtocol.equals(RoutingProtocol.IBGP) && !sessionProperties.isEbgp()) {
        /*
         *  The remote route is iBGP. The session is iBGP. We consider whether to reflect, and
         *  modify the outgoing route as appropriate.
         */
        boolean remoteRouteReceivedFromRouteReflectorClient =
            bgpRemoteRoute.getReceivedFromRouteReflectorClient();
        boolean sendingToRouteReflectorClient = fromNeighbor.getRouteReflectorClient();
        Ip remoteReceivedFromIp = bgpRemoteRoute.getReceivedFromIp();
        boolean remoteRouteOriginatedByRemoteNeighbor = remoteReceivedFromIp.equals(Ip.ZERO);
        if (!remoteRouteReceivedFromRouteReflectorClient
            && !sendingToRouteReflectorClient
            && !remoteRouteOriginatedByRemoteNeighbor) {
          /*
           * Neither reflecting nor originating this iBGP route, so don't send
           */
          return null;
        }
        transformedOutgoingRouteBuilder.addClusterList(bgpRemoteRoute.getClusterList());
        if (!remoteRouteOriginatedByRemoteNeighbor) {
          // we are reflecting, so we need to get the clusterid associated with the
          // remoteRoute
          BgpPeerConfig remoteReceivedFromSession =
              fromVrf
                  .getBgpProcess()
                  .getActiveNeighbors()
                  .get(Prefix.create(remoteReceivedFromIp, Prefix.MAX_PREFIX_LENGTH));
          long newClusterId = remoteReceivedFromSession.getClusterId();
          transformedOutgoingRouteBuilder.addToClusterList(newClusterId);
        }
        Set<Long> localClusterIds = toVrf.getBgpProcess().getClusterIds();
        Set<Long> outgoingClusterList = transformedOutgoingRouteBuilder.getClusterList();
        if (localClusterIds.stream().anyMatch(outgoingClusterList::contains)) {
          /*
           *  receiver will reject new route if it contains any of its local cluster ids
           */
          return null;
        }
      }
    }

    // Outgoing protocol
    transformedOutgoingRouteBuilder.setProtocol(
        sessionProperties.isEbgp() ? RoutingProtocol.BGP : RoutingProtocol.IBGP);
    transformedOutgoingRouteBuilder.setNetwork(route.getNetwork());

    // Outgoing metric (MED) is preserved only if advertising to IBGP peer.
    if (remoteRouteIsBgp & !sessionProperties.isEbgp()) {
      transformedOutgoingRouteBuilder.setMetric(route.getMetric());
    }

    // Outgoing nextHopIp & localPreference
    Ip nextHopIp;
    long localPreference;
    if (sessionProperties.isEbgp() || !remoteRouteIsBgp) {
      nextHopIp = fromNeighborIp;
      localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
    } else {
      nextHopIp = route.getNextHopIp();
      BgpRoute remoteIbgpRoute = (BgpRoute) route;
      localPreference = remoteIbgpRoute.getLocalPreference();
    }
    if (Route.UNSET_ROUTE_NEXT_HOP_IP.equals(nextHopIp)) {
      // should only happen for ibgp or dynamic bgp
      if (fromNeighbor instanceof BgpPassivePeerConfig) {
        nextHopIp = ((BgpActivePeerConfig) toNeighbor).getPeerAddress();
      } else {
        String nextHopInterface = route.getNextHopInterface();
        InterfaceAddress nextHopAddress =
            fromVrf.getInterfaces().get(nextHopInterface).getAddress();
        if (nextHopAddress == null) {
          throw new BgpRoutePropagationException("Route's nextHopInterface has no address");
        }
        nextHopIp = nextHopAddress.getIp();
      }
    }
    transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
    transformedOutgoingRouteBuilder.setLocalPreference(localPreference);

    // Outgoing srcProtocol
    transformedOutgoingRouteBuilder.setSrcProtocol(route.getProtocol());
    return transformedOutgoingRouteBuilder;
  }

  /** Perform BGP import transformations on a given route after receiving an advertisement */
  @Nullable
  public static BgpRoute.Builder transformBgpRouteOnImport(
      BgpPeerConfig toNeighbor,
      BgpSessionProperties sessionProperties,
      BgpRoute route,
      ConfigurationFormat configFormat) {

    if (route.getAsPath().containsAs(requireNonNull(toNeighbor.getLocalAs()))
        && !toNeighbor.getAllowLocalAsIn()) {
      // skip routes containing peer's AS unless
      // disable-peer-as-check (getAllowRemoteAsOut) is set
      return null;
    }
    RoutingProtocol targetProtocol =
        sessionProperties.isEbgp() ? RoutingProtocol.BGP : RoutingProtocol.IBGP;

    BgpRoute.Builder transformedIncomingRouteBuilder = new BgpRoute.Builder();
    transformedIncomingRouteBuilder.setOriginatorIp(route.getOriginatorIp());
    transformedIncomingRouteBuilder.setReceivedFromIp(route.getReceivedFromIp());
    transformedIncomingRouteBuilder.addClusterList(route.getClusterList());
    transformedIncomingRouteBuilder.setReceivedFromRouteReflectorClient(
        route.getReceivedFromRouteReflectorClient());
    transformedIncomingRouteBuilder.setAsPath(route.getAsPath());
    transformedIncomingRouteBuilder.addCommunities(route.getCommunities());
    transformedIncomingRouteBuilder.setProtocol(targetProtocol);
    transformedIncomingRouteBuilder.setNetwork(route.getNetwork());
    transformedIncomingRouteBuilder.setNextHopIp(route.getNextHopIp());
    transformedIncomingRouteBuilder.setLocalPreference(route.getLocalPreference());
    transformedIncomingRouteBuilder.setAdmin(
        targetProtocol.getDefaultAdministrativeCost(configFormat));
    transformedIncomingRouteBuilder.setMetric(route.getMetric());
    transformedIncomingRouteBuilder.setOriginType(route.getOriginType());
    transformedIncomingRouteBuilder.setSrcProtocol(targetProtocol);

    return transformedIncomingRouteBuilder;
  }

  /**
   * Convert an aggregate/generated route to a BGP route.
   *
   * @param generatedRoute a {@link GeneratedRoute} to convert to a {@link BgpRoute}.
   * @param routerId Router ID to set as the originatorIp for the resulting BGP route.
   * @param nonRouting Whether to mark the BgpRoute as non-routing
   */
  public static BgpRoute convertGeneratedRouteToBgp(
      GeneratedRoute generatedRoute, Ip routerId, boolean nonRouting) {
    return BgpRoute.builder()
        .setAdmin(generatedRoute.getAdministrativeCost())
        .setAsPath(generatedRoute.getAsPath())
        .setCommunities(generatedRoute.getCommunities())
        .setMetric(generatedRoute.getMetric())
        .setSrcProtocol(RoutingProtocol.AGGREGATE)
        .setProtocol(RoutingProtocol.AGGREGATE)
        .setNetwork(generatedRoute.getNetwork())
        .setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE)
        /*
         * Note: Origin type and originator IP should get overwritten by export policy,
         * but are needed initially
         */
        .setOriginatorIp(routerId)
        .setOriginType(OriginType.INCOMPLETE)
        .setReceivedFromIp(Ip.ZERO)
        .setNonRouting(nonRouting)
        .build();
  }

  /**
   * Perform BGP export transformations on a given route when sending an advertisement from {@code
   * fromNeighbor} to {@code toNeighbor} after export policy as applied and route is accepted, but
   * before route is sent onto the wire.
   */
  public static void transformBgpRoutePostExport(
      @Nonnull BgpRoute.Builder routeBuilder,
      @Nonnull BgpPeerConfig fromNeighbor,
      @Nonnull BgpSessionProperties sessionProperties) {
    if (sessionProperties.isEbgp()) {
      // if eBGP, prepend as-path sender's as-path number
      routeBuilder.setAsPath(
          AsPath.of(
              ImmutableList.<AsSet>builder()
                  .add(AsSet.of(fromNeighbor.getLocalAs()))
                  .addAll(routeBuilder.getAsPath().getAsSets())
                  .build()));
    }
  }
}

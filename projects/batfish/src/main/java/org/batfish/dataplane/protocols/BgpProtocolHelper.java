package org.batfish.dataplane.protocols;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.Vrf;
import org.batfish.dataplane.exceptions.BgpRoutePropagationException;

public class BgpProtocolHelper {

  /**
   * Perform BGP export transformations on a given route when preparing to send an advertisement
   * from {@code fromNeighbor} to {@code toNeighbor}
   *
   * @param fromNeighbor
   * @param toNeighbor
   * @return
   */
  @Nullable
  public static BgpRoute.Builder exportBgpRoute(
      BgpNeighbor fromNeighbor, BgpNeighbor toNeighbor, Vrf fromVrf, Vrf toVrf, AbstractRoute route)
      throws BgpRoutePropagationException {

    BgpRoute.Builder transformedOutgoingRouteBuilder = new BgpRoute.Builder();
    transformedOutgoingRouteBuilder.setReceivedFromIp(fromNeighbor.getLocalIp());
    RoutingProtocol remoteRouteProtocol = route.getProtocol();
    boolean ebgpSession = !fromNeighbor.getLocalAs().equals(fromNeighbor.getRemoteAs());
    boolean remoteRouteIsBgp =
        remoteRouteProtocol == RoutingProtocol.IBGP || remoteRouteProtocol == RoutingProtocol.BGP;
    RoutingProtocol targetProtocol = ebgpSession ? RoutingProtocol.BGP : RoutingProtocol.IBGP;

    // Set originatorIP
    Ip originatorIp;
    if (!ebgpSession && remoteRouteProtocol.equals(RoutingProtocol.IBGP)) {
      BgpRoute bgpRemoteRoute = (BgpRoute) route;
      originatorIp = bgpRemoteRoute.getOriginatorIp();
    } else {
      originatorIp = fromVrf.getBgpProcess().getRouterId();
    }
    transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);

    // note whether new route is received from route reflector client
    transformedOutgoingRouteBuilder.setReceivedFromRouteReflectorClient(
        !ebgpSession && toNeighbor.getRouteReflectorClient());

    // clusterList, receivedFromRouteReflectorClient, (originType
    // for bgp remote route)
    if (remoteRouteIsBgp) {
      BgpRoute bgpRemoteRoute = (BgpRoute) route;
      transformedOutgoingRouteBuilder.setOriginType(bgpRemoteRoute.getOriginType());
      if (ebgpSession
          && bgpRemoteRoute.getAsPath().containsAs(fromNeighbor.getRemoteAs())
          && !fromNeighbor.getAllowRemoteAsOut()) {
        // skip routes containing peer's AS unless
        // disable-peer-as-check (getAllowRemoteAsOut) is set
        return null;
      }
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
      if (!ebgpSession && toVrf.getBgpProcess().getRouterId().equals(remoteOriginatorIp)) {
        return null;
      }
      if (remoteRouteProtocol.equals(RoutingProtocol.IBGP) && !ebgpSession) {
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
        transformedOutgoingRouteBuilder.getClusterList().addAll(bgpRemoteRoute.getClusterList());
        if (!remoteRouteOriginatedByRemoteNeighbor) {
          // we are reflecting, so we need to get the clusterid associated with the
          // remoteRoute
          BgpNeighbor remoteReceivedFromSession =
              fromVrf
                  .getBgpProcess()
                  .getNeighbors()
                  .get(new Prefix(remoteReceivedFromIp, Prefix.MAX_PREFIX_LENGTH));
          long newClusterId = remoteReceivedFromSession.getClusterId();
          transformedOutgoingRouteBuilder.getClusterList().add(newClusterId);
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

    // Outgoing asPath
    // Outgoing communities
    if (remoteRouteIsBgp) {
      BgpRoute bgpRemoteRoute = (BgpRoute) route;
      transformedOutgoingRouteBuilder.setAsPath(bgpRemoteRoute.getAsPath().getAsSets());
      if (fromNeighbor.getSendCommunity()) {
        transformedOutgoingRouteBuilder.getCommunities().addAll(bgpRemoteRoute.getCommunities());
      }
    }
    if (ebgpSession) {
      SortedSet<Integer> newAsPathElement = new TreeSet<>();
      newAsPathElement.add(fromNeighbor.getLocalAs());
      transformedOutgoingRouteBuilder.getAsPath().add(0, newAsPathElement);
    }

    // Outgoing protocol
    transformedOutgoingRouteBuilder.setProtocol(targetProtocol);
    transformedOutgoingRouteBuilder.setNetwork(route.getNetwork());

    // Outgoing metric
    if (remoteRouteIsBgp) {
      transformedOutgoingRouteBuilder.setMetric(route.getMetric());
    }

    // Outgoing nextHopIp
    // Outgoing localPreference
    Ip nextHopIp;
    int localPreference;
    if (ebgpSession || !remoteRouteIsBgp) {
      nextHopIp = fromNeighbor.getLocalIp();
      localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
    } else {
      nextHopIp = route.getNextHopIp();
      BgpRoute remoteIbgpRoute = (BgpRoute) route;
      localPreference = remoteIbgpRoute.getLocalPreference();
    }
    if (nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
      // should only happen for ibgp
      String nextHopInterface = route.getNextHopInterface();
      InterfaceAddress nextHopAddress = fromVrf.getInterfaces().get(nextHopInterface).getAddress();
      if (nextHopAddress == null) {
        throw new BgpRoutePropagationException("Route's nextHopInterface has no address");
      }
      nextHopIp = nextHopAddress.getIp();
    }
    transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
    transformedOutgoingRouteBuilder.setLocalPreference(localPreference);

    // Outgoing srcProtocol
    transformedOutgoingRouteBuilder.setSrcProtocol(route.getProtocol());
    return transformedOutgoingRouteBuilder;
  }
}

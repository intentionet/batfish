package org.batfish.datamodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;

public class FibImpl implements Fib {

  private static final int MAX_DEPTH = 10;

  private static final long serialVersionUID = 1L;

  private final @Nonnull Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>
      _nextHopInterfaces;

  private final @Nonnull GenericRib<AbstractRoute> _rib;

  public FibImpl(@Nonnull GenericRib<AbstractRoute> rib) {
    _rib = rib;
    _nextHopInterfaces = new HashMap<>();
    for (AbstractRoute route : rib.getRoutes()) {
      Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfaces =
          collectNextHopInterfaces(_rib, route);
      _nextHopInterfaces.put(route, nextHopInterfaces);
    }
  }

  /**
   * Attempt to resolve a RIB route down to an interface route.
   *
   * @param rib {@link GenericRib} for which to do the resolution.
   * @param route {@link AbstractRoute} with a next hop IP to be resolved.
   * @return A map (interface name -&gt; last hop IP -&gt; last taken route) for
   * @throws BatfishException if resolution depth is exceeded (high likelihood of a routing loop) OR
   *     an invalid route in the RIB has been encountered.
   */
  private static Map<String, Map<Ip, Set<AbstractRoute>>> collectNextHopInterfaces(
      GenericRib<AbstractRoute> rib, AbstractRoute route) {
    Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfaces = new HashMap<>();
    collectNextHopInterfaces(
        rib,
        route,
        Route.UNSET_ROUTE_NEXT_HOP_IP,
        nextHopInterfaces,
        new HashSet<>(),
        0,
        Prefix.MAX_PREFIX_LENGTH,
        null);
    return ImmutableMap.copyOf(nextHopInterfaces);
  }

  private static void collectNextHopInterfaces(
      GenericRib<AbstractRoute> rib,
      AbstractRoute route,
      Ip mostRecentNextHopIp,
      Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfaces,
      Set<Prefix> seenNetworks,
      int depth,
      int maxPrefixLength,
      @Nullable AbstractRoute parentRoute) {
    Prefix network = route.getNetwork();
    if (seenNetworks.contains(network)) {
      return;
    }
    Set<Prefix> newSeenNetworks = new HashSet<>(seenNetworks);
    newSeenNetworks.add(network);
    if (depth > MAX_DEPTH) {
      // TODO: Declare this a loop using some warning mechanism
      // https://github.com/batfish/batfish/issues/1469
      return;
    }

    // For non-forwarding routes, try to find a less specific route
    if (route.getNonForwarding()) {
      if (parentRoute == null) {
        return;
      } else {
        seenNetworks.remove(parentRoute.getNetwork());
        collectNextHopInterfaces(
            rib,
            parentRoute,
            mostRecentNextHopIp,
            nextHopInterfaces,
            seenNetworks,
            depth + 1,
            maxPrefixLength - 1,
            null);
        return;
      }
    }

    /* For BGP next-hop-discard routes, ignore next-hop-ip and exit early */
    if (route instanceof BgpRoute && ((BgpRoute) route).getDiscard()) {
      Map<Ip, Set<AbstractRoute>> nextHopInterfaceRoutesByFinalNextHopIp =
          nextHopInterfaces.computeIfAbsent(Interface.NULL_INTERFACE_NAME, k -> new HashMap<>());
      Set<AbstractRoute> nextHopInterfaceRoutes =
          nextHopInterfaceRoutesByFinalNextHopIp.computeIfAbsent(
              Route.UNSET_ROUTE_NEXT_HOP_IP, k -> new TreeSet<>());
      nextHopInterfaceRoutes.add(route);
      return;
    }

    String nextHopInterface = route.getNextHopInterface();
    if (!Route.UNSET_NEXT_HOP_INTERFACE.equals(nextHopInterface)) {
      Ip finalNextHopIp =
          route.getNextHopIp().equals(Route.UNSET_ROUTE_NEXT_HOP_IP)
              ? mostRecentNextHopIp
              : route.getNextHopIp();
      Map<Ip, Set<AbstractRoute>> nextHopInterfaceRoutesByFinalNextHopIp =
          nextHopInterfaces.computeIfAbsent(nextHopInterface, k -> new HashMap<>());
      Set<AbstractRoute> nextHopInterfaceRoutes =
          nextHopInterfaceRoutesByFinalNextHopIp.computeIfAbsent(
              finalNextHopIp, k -> new TreeSet<>());
      nextHopInterfaceRoutes.add(route);
    } else {
      Ip nextHopIp = route.getNextHopIp();
      if (!nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
        Set<AbstractRoute> nextHopLongestPrefixMatchRoutes =
            rib.longestPrefixMatch(nextHopIp, maxPrefixLength);

        /* Filter out any non-forwarding routes from the matches */
        Set<AbstractRoute> forwardingRoutes =
            nextHopLongestPrefixMatchRoutes.stream()
                .filter(r -> !r.getNonForwarding())
                .collect(ImmutableSet.toImmutableSet());

        if (forwardingRoutes.isEmpty()) {
          // Re-resolve *this route* with less specific prefix match
          seenNetworks.remove(route.getNetwork());
          collectNextHopInterfaces(
              rib,
              route,
              mostRecentNextHopIp,
              nextHopInterfaces,
              seenNetworks,
              depth + 1,
              maxPrefixLength - 1,
              parentRoute);
        } else {
          // We have at least one valid longest-prefix match
          for (AbstractRoute nextHopLongestPrefixMatchRoute : forwardingRoutes) {
            collectNextHopInterfaces(
                rib,
                nextHopLongestPrefixMatchRoute,
                nextHopIp,
                nextHopInterfaces,
                newSeenNetworks,
                depth + 1,
                Prefix.MAX_PREFIX_LENGTH,
                route);
          }
        }
      } else {
        // TODO: Declare this using some warning mechanism
        // https://github.com/batfish/batfish/issues/1469
        return;
      }
    }
  }

  /** Mapping: route -&gt; nextHopInterface -&gt; resolved nextHopIp -&gt; interfaceRoutes */
  @Override
  public @Nonnull Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>
      getNextHopInterfaces() {
    return _nextHopInterfaces;
  }

  @Override
  public @Nonnull Set<String> getNextHopInterfaces(Ip ip) {
    return _rib.longestPrefixMatch(ip).stream()
        .flatMap(nextHopRoute -> _nextHopInterfaces.get(nextHopRoute).keySet().stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public @Nonnull Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>
      getNextHopInterfacesByRoute(Ip dstIp) {
    return _rib.longestPrefixMatch(dstIp).stream()
        .collect(ImmutableMap.toImmutableMap(Function.identity(), _nextHopInterfaces::get));
  }

  @Override
  public @Nonnull Map<String, Set<AbstractRoute>> getRoutesByNextHopInterface() {
    Map<String, ImmutableSet.Builder<AbstractRoute>> routesByNextHopInterface = new HashMap<>();
    _nextHopInterfaces.forEach(
        (route, nextHopInterfaceMap) ->
            nextHopInterfaceMap
                .keySet()
                .forEach(
                    nextHopInterface ->
                        routesByNextHopInterface
                            .computeIfAbsent(nextHopInterface, n -> ImmutableSet.builder())
                            .add(route)));
    return routesByNextHopInterface.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* interfaceName */,
                routesByNextHopInterfaceEntry -> routesByNextHopInterfaceEntry.getValue().build()));
  }
}

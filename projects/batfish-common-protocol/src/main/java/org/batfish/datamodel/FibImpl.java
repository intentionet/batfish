package org.batfish.datamodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
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
   * @return A map (interface name -> last hop IP -> last taken route) for
   * @throws BatfishException if resolution depth is exceeded (high likelihood of a routing loop) OR
   *     an invalid route in the RIB has been encountered.
   */
  public static Map<String, Map<Ip, Set<AbstractRoute>>> collectNextHopInterfaces(
      GenericRib<AbstractRoute> rib, AbstractRoute route) {
    Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfaces = new HashMap<>();
    collectNextHopInterfaces(
        rib, route, Route.UNSET_ROUTE_NEXT_HOP_IP, nextHopInterfaces, new HashSet<>(), 0);
    return ImmutableMap.copyOf(nextHopInterfaces);
  }

  private static void collectNextHopInterfaces(
      GenericRib<AbstractRoute> rib,
      AbstractRoute route,
      Ip mostRecentNextHopIp,
      Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfaces,
      Set<Prefix> seenNetworks,
      int depth) {
    Prefix network = route.getNetwork();
    if (seenNetworks.contains(network)) {
      return;
    }
    Set<Prefix> newSeenNetworks = new HashSet<>(seenNetworks);
    newSeenNetworks.add(network);
    if (depth > MAX_DEPTH) {
      throw new BatfishException("Exceeded max route recursion depth: " + MAX_DEPTH);
    }
    Ip nextHopIp = route.getNextHopIp();
    if (!nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
      Set<AbstractRoute> nextHopLongestPrefixMatchRoutes = rib.longestPrefixMatch(nextHopIp);
      for (AbstractRoute nextHopLongestPrefixMatchRoute : nextHopLongestPrefixMatchRoutes) {
        collectNextHopInterfaces(
            rib,
            nextHopLongestPrefixMatchRoute,
            nextHopIp,
            nextHopInterfaces,
            newSeenNetworks,
            depth + 1);
      }
    } else {
      String nextHopInterface = route.getNextHopInterface();
      if (!Route.UNSET_NEXT_HOP_INTERFACE.equals(nextHopInterface)) {
        Map<Ip, Set<AbstractRoute>> nextHopInterfaceRoutesByFinalNextHopIp =
            nextHopInterfaces.computeIfAbsent(nextHopInterface, k -> new HashMap<>());
        Set<AbstractRoute> nextHopInterfaceRoutes =
            nextHopInterfaceRoutesByFinalNextHopIp.computeIfAbsent(
                mostRecentNextHopIp, k -> new TreeSet<>());
        nextHopInterfaceRoutes.add(route);
      } else {
        throw new BatfishException("Encountered route with neither nextHopIp nor nextHopInterface");
      }
    }
  }

  /** Mapping: route -> nextHopInterface -> nextHopIp -> interfaceRoutes */
  @Override
  public @Nonnull Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>
      getNextHopInterfaces() {
    return _nextHopInterfaces;
  }

  @Override
  public @Nonnull Map<String, Map<Ip, Set<AbstractRoute>>> getNextHopInterfaces(Ip ip) {
    Map<String, Map<Ip, Set<AbstractRoute>>> outputNextHopInterfaces = new TreeMap<>();
    Set<AbstractRoute> nextHopRoutes = _rib.longestPrefixMatch(ip);
    for (AbstractRoute nextHopRoute : nextHopRoutes) {
      Map<String, Map<Ip, Set<AbstractRoute>>> currentNextHopInterfaces =
          _nextHopInterfaces.get(nextHopRoute);
      currentNextHopInterfaces.forEach(
          (nextHopInterface, nextHopInterfaceRoutesByFinalNextHopIp) -> {
            Map<Ip, Set<AbstractRoute>> outputNextHopInterfaceRoutesByFinalNextHopIp =
                outputNextHopInterfaces.computeIfAbsent(nextHopInterface, k -> new TreeMap<>());
            outputNextHopInterfaceRoutesByFinalNextHopIp.putAll(
                nextHopInterfaceRoutesByFinalNextHopIp);
          });
    }
    return outputNextHopInterfaces;
  }

  @Override
  public @Nonnull Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>
      getNextHopInterfacesByRoute(Ip dstIp) {
    Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> nextHopInterfacesByRoute =
        new HashMap<>();
    Set<AbstractRoute> nextHopRoutes = _rib.longestPrefixMatch(dstIp);
    for (AbstractRoute nextHopRoute : nextHopRoutes) {
      Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfaces =
          _nextHopInterfaces.get(nextHopRoute);
      nextHopInterfacesByRoute.put(nextHopRoute, nextHopInterfaces);
    }
    return nextHopInterfacesByRoute;
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
    return routesByNextHopInterface
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* interfaceName */,
                routesByNextHopInterfaceEntry -> routesByNextHopInterfaceEntry.getValue().build()));
  }
}

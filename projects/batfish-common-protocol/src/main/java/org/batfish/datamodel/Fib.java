package org.batfish.datamodel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.common.BatfishException;

public class Fib implements Serializable {

  private static final int MAX_DEPTH = 10;

  private static final long serialVersionUID = 1L;

  /** route -> nexthopinterface -> nextHopIp -> interfaceRoutes */
  private final Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> _nextHopInterfaces;

  public Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> getNextHopInterfaces() {
    return _nextHopInterfaces;
  }

  private final GenericRib<AbstractRoute> _rib;

  public Fib(GenericRib<AbstractRoute> rib) {
    _rib = rib;
    _nextHopInterfaces = new HashMap<>();
    for (AbstractRoute route : rib.getRoutes()) {
      Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfaces = new TreeMap<>();
      collectNextHopInterfaces(
          route, Route.UNSET_ROUTE_NEXT_HOP_IP, nextHopInterfaces, new HashSet<>(), 0);
      _nextHopInterfaces.put(route, nextHopInterfaces);
    }
  }

  private void collectNextHopInterfaces(
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
      Set<AbstractRoute> nextHopLongestPrefixMatchRoutes = _rib.longestPrefixMatch(nextHopIp);
      for (AbstractRoute nextHopLongestPrefixMatchRoute : nextHopLongestPrefixMatchRoutes) {
        collectNextHopInterfaces(
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

  public Map<String, Map<Ip, Set<AbstractRoute>>> getNextHopInterfaces(Ip ip) {
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

  public Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> getNextHopInterfacesByRoute(
      Ip dstIp) {
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
}

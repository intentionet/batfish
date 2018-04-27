package org.batfish.dataplane.rib;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.ibdp.VirtualRouter;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;

/**
 * Implements general RIB (Routing Information Base) semantics. RIB stores routes for different
 * network prefixes and supports retrieving them based on the longest prefix match between a given
 * IP address and the route's IP prefix.
 *
 * @param <R> Type of route that this RIB will be storing. Required for properly comparing route
 *     preferences.
 */
public abstract class AbstractRib<R extends AbstractRoute> implements GenericRib<R> {

  private static final long serialVersionUID = 1L;

  /**
   * This logical clock helps us keep track when routes were merged into the RIB to determine their
   * age. It's incremented each time a route is merged into the RIB.
   */
  protected long _logicalClock;

  /** Map to keep track when routes were merged in. */
  protected Map<R, Long> _logicalArrivalTime;

  @Nullable protected VirtualRouter _owner;

  private RibTree<R> _tree;

  @Nullable private Set<R> _allRoutes;

  /**
   * Keep a Sorted Set of alternative routes. Used to update the RIB if best routes are withdrawn
   */
  @Nullable final Map<Prefix, SortedSet<R>> _backupRoutes;

  public AbstractRib(
      @Nullable VirtualRouter owner, @Nullable Map<Prefix, SortedSet<R>> backupRoutes) {
    _allRoutes = ImmutableSet.of();
    _backupRoutes = backupRoutes;
    _logicalArrivalTime = new HashMap<>();
    _logicalClock = 0;
    _owner = owner;
    _tree = new RibTree<>(this);
  }

  /**
   * Import routes from one RIB into another
   *
   * @param importingRib the RIB that imports routes
   * @param exportingRib the RIB that exports routes
   * @param <U> type of route
   * @param <T> type of route (must be more specific than {@link U}
   * @return a {@link RibDelta}
   */
  @Nullable
  public static <U extends AbstractRoute, T extends U> RibDelta<U> importRib(
      AbstractRib<U> importingRib, AbstractRib<T> exportingRib) {
    RibDelta.Builder<U> builder = new RibDelta.Builder<>(importingRib);
    for (T route : exportingRib.getRoutes()) {
      builder.from(importingRib.mergeRouteGetDelta(route));
    }
    return builder.build();
  }

  public final boolean containsRoute(R route) {
    return _tree.containsRoute(route);
  }

  @Override
  public final SortedSet<Prefix> getPrefixes() {
    SortedSet<Prefix> prefixes = new TreeSet<>();
    Set<R> routes = getRoutes();
    for (R route : routes) {
      prefixes.add(route.getNetwork());
    }
    return prefixes;
  }

  @Override
  public final Set<R> getRoutes() {
    if (_allRoutes == null) {
      _allRoutes = ImmutableSet.copyOf(_tree.getRoutes());
    }
    return _allRoutes;
  }

  public final Set<R> getRoutes(Prefix p) {
    // Collect routes that match the prefix
    return getRoutes()
        .stream()
        .filter(r -> r.getNetwork().equals(p))
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public abstract int comparePreference(R lhs, R rhs);

  @Override
  public Set<R> longestPrefixMatch(Ip address) {
    return _tree.getLongestPrefixMatch(address);
  }

  /**
   * Add a new route to the RIB.
   *
   * @param route the route to add
   * @return @{link RibDelta} if the route was added. {@code null} if the route already existed or
   *     was discarded due to preference comparisons.
   */
  @Nullable
  public RibDelta<R> mergeRouteGetDelta(R route) {
    RibDelta<R> delta = _tree.mergeRoute(route);
    if (delta != null) {
      // A change to routes has been made
      _allRoutes = null;
      _logicalArrivalTime.put(route, _logicalClock);
      _logicalClock++;
    }
    return delta;
  }

  /**
   * Add a new route to the RIB.
   *
   * @param route the route to add
   * @return true if the route was added. False if the route already existed or was discarded due to
   *     preference comparisons.
   */
  @Override
  public boolean mergeRoute(R route) {
    return mergeRouteGetDelta(route) != null;
  }

  /**
   * Remove given route from the RIB
   *
   * @param route route to remove
   * @param reason The reason for route removal (will propagate to the returned delta)
   * @return a {@link RibDelta} object indicating that the route was removed or @{code null} if the
   *     route was not present in the RIB
   */
  @Nullable
  public RibDelta<R> removeRouteGetDelta(R route, Reason reason) {
    RibDelta<R> delta = _tree.removeRouteGetDelta(route, reason);
    if (delta != null && delta.getActions() != null) {
      // A change to routes has been made
      _allRoutes = null;
      delta
          .getActions()
          .forEach(
              a -> {
                if (a.isWithdrawn()) {
                  _logicalArrivalTime.remove(a.getRoute());
                }
              });
    }
    return delta;
  }

  @Nullable
  public RibDelta<R> removeRouteGetDelta(R route) {
    return removeRouteGetDelta(route, Reason.WITHDRAW);
  }

  /**
   * Remove given route from the RIB
   *
   * @param route route to remove
   * @return True if the route was located and removed
   */
  public boolean removeRoute(R route) {
    return removeRouteGetDelta(route, Reason.WITHDRAW) != null;
  }

  /**
   * Clear the routes for a given prefix.
   *
   * <p><b>Only routes with exact prefix matches are cleared!</b>
   *
   * <p>The returning {@link RibDelta} will specify {@link Reason#REPLACE} as the reason for route
   * removal.
   *
   * @param prefix the {@link Prefix} for which the routes should be cleared.
   */
  public RibDelta<R> clearRoutes(Prefix prefix) {
    RibDelta<R> d = _tree.clearRoutes(prefix);
    if (d != null) {
      _allRoutes = null;
    }
    return d;
  }

  @Override
  public final Map<Prefix, Set<Ip>> nextHopIpsByPrefix() {
    Map<Prefix, Set<Ip>> map = new TreeMap<>();
    for (AbstractRoute route : getRoutes()) {
      Prefix prefix = route.getNetwork();
      Ip nextHopIp = route.getNextHopIp();
      Set<Ip> nextHopIps = map.computeIfAbsent(prefix, k -> new TreeSet<>());
      nextHopIps.add(nextHopIp);
    }
    return map;
  }

  /**
   * Check if two RIBs have exactly same sets of routes.
   *
   * <p>Designed to be faster (in an average case) than doing two calls to {@link #getRoutes} and
   * then testing the sets for equality.
   *
   * @param other the other RIB
   * @return True if both ribs contain identical routes
   */
  @Override
  public boolean equals(Object other) {
    return (this == other)
        || (other instanceof AbstractRib<?> && _tree.equals(((AbstractRib<?>) other)._tree));
  }

  @Override
  public int hashCode() {
    return Objects.hash(_tree);
  }

  @Override
  public final Map<Prefix, IpSpace> getMatchingIps() {
    return _tree
        .getMatchingIps()
        .entrySet()
        .stream()
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> (IpSpace) e.getValue()));
  }

  @Override
  public final IpSpace getRoutableIps() {
    return _tree.getRoutableIps();
  }
}

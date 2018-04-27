package org.batfish.dataplane.rib;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;

/**
 * Represents a change in RIB state
 *
 * @param <R> route type
 */
public class RibDelta<R extends AbstractRoute> {

  private ImmutableMap<Prefix, ImmutableList<RouteAdvertisement<R>>> _actions;
  /** The "owner"/parent RIB */
  @Nullable private AbstractRib<R> _rib;

  private RibDelta(
      @Nullable AbstractRib<R> rib, Map<Prefix, ImmutableList<RouteAdvertisement<R>>> actions) {
    this._actions = ImmutableMap.copyOf(actions);
    this._rib = rib;
  }

  /** Get the owner RIB of this RibDelta */
  @Nullable
  public AbstractRib<R> getRib() {
    return _rib;
  }

  /**
   * Return all the RIB actions that need to be applied (in order)
   *
   * @param p a particular {@link Prefix} to retrieve the actions for. If {@code null}, all actions
   *     are returned
   * @return a list of {@link RouteAdvertisement}
   */
  public List<RouteAdvertisement<R>> getActions(@Nullable Prefix p) {
    if (p == null) {
      return _actions
          .values()
          .stream()
          .flatMap(List::stream)
          .collect(ImmutableList.toImmutableList());
    }
    List<RouteAdvertisement<R>> r = _actions.get(p);
    return r == null ? ImmutableList.of() : r;
  }

  /**
   * Return the set of prefixes this delta has modifications for
   *
   * @return a set of {@link Prefix}
   */
  public Set<Prefix> getPrefixes() {
    return _actions.keySet();
  }

  /**
   * Return all the RIB actions that need to be applied (in order)
   *
   * @return a list of {@link RouteAdvertisement}
   */
  public List<RouteAdvertisement<R>> getActions() {
    return getActions(null);
  }

  /**
   * Helper method: retrieves all routes affected by this delta.
   *
   * @return List of routes
   */
  public List<R> getRoutes() {
    return _actions
        .values()
        .stream()
        .flatMap(List::stream)
        .map(RouteAdvertisement::getRoute)
        .collect(ImmutableList.toImmutableList());
  }

  public static class Builder<R extends AbstractRoute> {

    private Map<Prefix, LinkedHashMap<R, RouteAdvertisement<R>>> _actions;

    private AbstractRib<R> _rib;

    /** Initialize a new RibDelta builder */
    public Builder(@Nullable AbstractRib<R> rib) {
      _actions = new LinkedHashMap<>();
      _rib = rib;
    }

    /**
     * Indicate that a route was added to the RIB
     *
     * @param route that was added
     */
    public Builder<R> add(@Nonnull R route) {
      LinkedHashMap<R, RouteAdvertisement<R>> l =
          _actions.computeIfAbsent(route.getNetwork(), p -> new LinkedHashMap<>(10, 1, true));
      l.put(route, new RouteAdvertisement<>(route));
      return this;
    }

    /**
     * Indicate that multiple routes have been added to the RIB
     *
     * @param routes a collection of routes
     */
    public Builder<R> add(@Nonnull Collection<? extends R> routes) {
      for (R route : routes) {
        LinkedHashMap<R, RouteAdvertisement<R>> l =
            _actions.computeIfAbsent(route.getNetwork(), p -> new LinkedHashMap<>(10, 1, true));
        l.put(route, new RouteAdvertisement<>(route));
      }
      return this;
    }

    /**
     * Indicate that a route was removed from the RIB
     *
     * @param route that was removed
     */
    public Builder<R> remove(R route, Reason reason) {
      LinkedHashMap<R, RouteAdvertisement<R>> l =
          _actions.computeIfAbsent(route.getNetwork(), p -> new LinkedHashMap<>(10, 1, true));
      l.put(route, new RouteAdvertisement<>(route, true, reason));
      return this;
    }

    /**
     * Indicate that multiple routes were removed from the RIB
     *
     * @param routes that were removed
     */
    public Builder<R> remove(Collection<R> routes, Reason reason) {
      for (R route : routes) {
        LinkedHashMap<R, RouteAdvertisement<R>> l =
            _actions.computeIfAbsent(route.getNetwork(), p -> new LinkedHashMap<>(10, 1, true));
        l.put(route, new RouteAdvertisement<>(route, true, reason));
      }
      return this;
    }

    /**
     * Create a new RIB delta. Returns {@code null} if no changes were made
     *
     * @return A new {@link RibDelta}
     */
    @Nullable
    public RibDelta<R> build() {
      if (_actions.isEmpty()) {
        return null;
      }
      return new RibDelta<>(
          _rib,
          _actions
              .entrySet()
              .stream()
              .collect(
                  ImmutableMap.toImmutableMap(
                      Entry::getKey,
                      e ->
                          e.getValue()
                              .values()
                              .stream()
                              .collect(ImmutableList.toImmutableList()))));
    }

    /** Process all added and removed routes from a collection of deltas */
    public Builder<R> from(Collection<RibDelta<R>> deltas) {
      for (RibDelta<R> d : deltas) {
        from(d);
      }
      return this;
    }

    /** Process all added and removed routes from a given delta */
    public <T extends R> Builder<R> from(@Nullable RibDelta<T> delta) {
      if (delta != null) {
        for (RouteAdvertisement<T> a : delta.getActions()) {
          LinkedHashMap<R, RouteAdvertisement<R>> l =
              _actions.computeIfAbsent(
                  a.getRoute().getNetwork(), p -> new LinkedHashMap<>(10, 1, true));
          l.put(
              a.getRoute(), new RouteAdvertisement<>(a.getRoute(), a.isWithdrawn(), a.getReason()));
        }
      }
      return this;
    }
  }

  /**
   * Apply a {@link RibDelta} to a given RIB
   *
   * @param importingRib the RIB to apply the delta to
   * @param delta the delta to apply
   * @param <U> Type of route in the RIB
   * @param <T> type of route in the delta (must be more specific than {@link U}
   * @return the {@link RibDelta} that results from modifying {@code importingRib}
   */
  @VisibleForTesting
  @Nullable
  public static <U extends AbstractRoute, T extends U> RibDelta<U> importRibDelta(
      @Nonnull AbstractRib<U> importingRib, @Nullable RibDelta<T> delta) {
    if (delta == null) {
      return null;
    }
    Builder<U> builder = new Builder<>(importingRib);
    List<RouteAdvertisement<T>> aa = delta.getActions();
    for (RouteAdvertisement<T> routeAdvertisement : aa) {
      if (routeAdvertisement.isWithdrawn()) {
        builder.from(
            importingRib.removeRouteGetDelta(
                routeAdvertisement.getRoute(), routeAdvertisement.getReason()));
      } else {
        RibDelta<U> tmp = importingRib.mergeRouteGetDelta(routeAdvertisement.getRoute());
        builder.from(tmp);
      }
    }
    return builder.build();
  }

  /**
   * Apply a {@link RibDelta} to a given RIB, but only for a particular {@link Prefix} {@code p}
   *
   * @param importingRib the RIB to apply the delta to
   * @param delta the delta to apply
   * @param <U> Type of route in the RIB
   * @param <T> type of route in the delta (must be more specific than {@link U}
   * @return the {@link RibDelta} that results from modifying {@code importingRib}
   */
  @VisibleForTesting
  @Nullable
  public static <U extends AbstractRoute, T extends U> RibDelta<U> importRibDelta(
      @Nonnull AbstractRib<U> importingRib, @Nullable RibDelta<T> delta, Prefix p) {
    if (delta == null || delta.getActions(p) == null) {
      return null;
    }
    Builder<U> builder = new Builder<>(importingRib);
    List<RouteAdvertisement<T>> actions = delta.getActions(p);
    if (actions == null) {
      return null;
    }
    for (RouteAdvertisement<T> routeAdvertisement : actions) {
      if (!routeAdvertisement.isWithdrawn()) {
        builder.from(importingRib.mergeRouteGetDelta(routeAdvertisement.getRoute()));
      } else {
        builder.from(
            importingRib.removeRouteGetDelta(
                routeAdvertisement.getRoute(), routeAdvertisement.getReason()));
      }
    }
    return builder.build();
  }
}

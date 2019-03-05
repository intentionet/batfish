package org.batfish.dataplane.rib;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;

/** BGP-specific RIB implementation */
@ParametersAreNonnullByDefault
public class BgpRib extends AbstractRib<BgpRoute> {

  private static final long serialVersionUID = 1L;

  /** Main RIB to use for IGP cost estimation */
  @Nullable private final AnnotatedRib<AbstractRoute> _mainRib;

  /** Tie breaker to use if all route attributes appear to be equal */
  @Nonnull private final BgpTieBreaker _tieBreaker;

  /** Maximum number of paths to install. Unconstrained (infinite) if {@code null} */
  @Nullable private final Integer _maxPaths;

  /**
   * For multipath: how strict should the comparison of AS Path be for paths to be considered equal
   */
  @Nullable private final MultipathEquivalentAsPathMatchMode _multipathEquivalentAsPathMatchMode;

  // Best BGP paths. Invariant: must be re-evaluated (per prefix) each time a route is added or
  // evicted
  @Nonnull private final Map<Prefix, BgpRoute> _bestPaths;

  public BgpRib(
      @Nullable Map<Prefix, SortedSet<BgpRoute>> backupRoutes,
      @Nullable Rib mainRib,
      BgpTieBreaker tieBreaker,
      @Nullable Integer maxPaths,
      @Nullable MultipathEquivalentAsPathMatchMode multipathEquivalentAsPathMatchMode) {
    super(backupRoutes);
    _mainRib = mainRib;
    _tieBreaker = tieBreaker;
    checkArgument(maxPaths == null || maxPaths > 0, "Invalid max-paths value %s", maxPaths);
    if (maxPaths != null && maxPaths > 1) {
      /*
       * Essentially make this infinite. This is a design choice to enable more comprehensive path
       * analysis up the stack (e.g., reachability, multipath consistency, etc.)
       */
      _maxPaths = null;
    } else {
      _maxPaths = maxPaths;
    }
    checkArgument(
        Integer.valueOf(1).equals(maxPaths) || multipathEquivalentAsPathMatchMode != null,
        "Multipath AS-Path-Match-mode must be specified for a multipath BGP RIB");
    _multipathEquivalentAsPathMatchMode = multipathEquivalentAsPathMatchMode;
    _bestPaths = new HashMap<>(0);
  }

  /*
   * Resources for understanding this logic and the missing features:
   *
   * - https://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/13753-25.html
   * - https://www.juniper.net/documentation/en_US/junos/topics/reference/general/routing-protocols-address-representation.html
   */
  @Override
  public int comparePreference(BgpRoute lhs, BgpRoute rhs) {
    int multipathCompare =
        Comparator
            // Prefer higher Weight (cisco only)
            .comparing(BgpRoute::getWeight)
            // Prefer higher LocalPref
            .thenComparing(BgpRoute::getLocalPreference)
            // NOTE: Accumulated interior gateway protocol (AIGP) is not supported
            // Aggregates (for non-juniper devices, won't appear on Juniper)
            .thenComparing(r -> getAggregatePreference(r.getProtocol()))
            // AS path: prefer shorter
            // NOTE: if BGP confederations were supported, confederation segments have a length of 0
            // TODO: support `bestpath as-path ignore` (both cisco, juniper)
            .thenComparing(r -> r.getAsPath().size(), Comparator.reverseOrder())
            // Prefer certain origin type Internal over External over Incomplete
            .thenComparing(r -> r.getOriginType().getPreference())
            // Prefer eBGP over iBGP
            .thenComparing(r -> getTypeCost(r.getProtocol()))
            /*
             * Prefer lower Multi-Exit Discriminator (MED)
             * TODO: better support for MED rules
             * Most rules are currently not supported:
             *    - normally this comparison is done only if the first AS is the same in both AS Paths
             *    - `always-compare-med` -- overrides above
             *    - there are additional confederation rules
             *    - On Juniper `path-selection cisco-nondeterministic` changes behavior
             *    - On Cisco `bgp bestpath med missing-as-worst` changes missing MED values from 0 to MAX_INT
             */
            .thenComparing(BgpRoute::getMetric, Comparator.reverseOrder())
            // Prefer next hop IPs with the lowest IGP metric
            .thenComparing(this::getIgpCostToNextHopIp, Comparator.reverseOrder())
            // Evaluate AS path compatibility for multipath
            .thenComparing(this::compareRouteAsPath)
            .compare(lhs, rhs);
    if (multipathCompare != 0 || isMultipath()) {
      return multipathCompare;
    } else {
      return Comparator.comparing(Function.identity(), this::bestPathComparator).compare(lhs, rhs);
    }
  }

  @Nonnull
  @Override
  public RibDelta<BgpRoute> mergeRouteGetDelta(BgpRoute route) {
    RibDelta<BgpRoute> delta = super.mergeRouteGetDelta(route);
    if (!delta.isEmpty()) {
      delta.getPrefixes().forEach(this::selectBestPath);
    }
    return delta;
  }

  @Nonnull
  @Override
  public RibDelta<BgpRoute> removeRouteGetDelta(BgpRoute route, Reason reason) {
    RibDelta<BgpRoute> delta = super.removeRouteGetDelta(route, reason);
    if (!delta.isEmpty()) {
      delta.getPrefixes().forEach(this::selectBestPath);
    }
    return delta;
  }

  @Override
  public final Set<BgpRoute> getTypedRoutes() {
    if (isMultipath()) {
      return super.getTypedRoutes();
    } else {
      return getBestPathRoutes();
    }
  }

  public Set<BgpRoute> getBestPathRoutes() {
    return ImmutableSet.copyOf(_bestPaths.values());
  }

  private int compareRouteAsPath(BgpRoute lhs, BgpRoute rhs) {
    return compareAsPath(lhs.getAsPath(), rhs.getAsPath());
  }

  private int compareAsPath(AsPath lhs, AsPath rhs) {
    if (_multipathEquivalentAsPathMatchMode == null) {
      // Nothing to do; defer to best-path selection
      return 0;
    }
    switch (_multipathEquivalentAsPathMatchMode) {
      case EXACT_PATH:
        return lhs.equals(rhs) ? 0 : -1;
      case FIRST_AS:
        AsSet lhsFirstAsSet = lhs.getAsSets().isEmpty() ? AsSet.empty() : lhs.getAsSets().get(0);
        AsSet rhsFirstAsSet = rhs.getAsSets().isEmpty() ? AsSet.empty() : rhs.getAsSets().get(0);
        return lhsFirstAsSet.equals(rhsFirstAsSet) ? 0 : -1;
      case PATH_LENGTH:
        return Integer.compare(lhs.size(), rhs.size());
      default:
        throw new IllegalStateException(
            String.format(
                "Unsupported AS PATH comparison mode: %s", _multipathEquivalentAsPathMatchMode));
    }
  }

  private void selectBestPath(Prefix prefix) {
    Optional<BgpRoute> s = extractRoutes(prefix).stream().max(this::bestPathComparator);
    if (!s.isPresent()) {
      // Remove best path and return
      _bestPaths.remove(prefix);
      return;
    }
    _bestPaths.put(prefix, s.get());
  }

  /**
   * Compare two routes for best path selection. Assumes that given routes have already been checked
   * for multipath equivalence, and thus have the same protocol (e/iBGP, same as path length, etc)
   */
  @VisibleForTesting
  private int bestPathComparator(BgpRoute lhs, BgpRoute rhs) {
    int result = 0;

    // Skip arrival order unless requested
    if (_tieBreaker == BgpTieBreaker.ARRIVAL_ORDER) {
      result =
          Comparator.<BgpRoute, Long>comparing(
                  r -> _logicalArrivalTime.getOrDefault(r, _logicalClock),
                  Comparator.reverseOrder())
              .compare(lhs, rhs);
    }
    if (result != 0) {
      return result;
    }

    // Continue with remaining tie breakers
    return Comparator.nullsFirst(
            // Prefer lower originator router ID
            Comparator.comparing(BgpRoute::getOriginatorIp, Comparator.reverseOrder())
                // Prefer lower cluster list length. Only applicable to iBGP
                .thenComparing(r -> r.getClusterList().size(), Comparator.reverseOrder())
                // Prefer lower neighbor IP
                .thenComparing(BgpRoute::getReceivedFromIp, Comparator.reverseOrder()))
        .compare(lhs, rhs);
  }

  /** Returns {@code true} iff this RIB is configured to be a BGP multipath RIB. */
  public boolean isMultipath() {
    return _maxPaths == null || _maxPaths > 1;
  }

  /** Establish total ordering: Prefer BGP aggregate routes */
  private static int getAggregatePreference(RoutingProtocol protocol) {
    if (protocol == RoutingProtocol.AGGREGATE) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * Establish total ordering: BGP aggregate routes preferred over eBGP, which is preferred over
   * iBGP
   */
  private static int getTypeCost(RoutingProtocol protocol) {
    switch (protocol) {
      case AGGREGATE:
        return 2;
      case BGP: // eBGP
        return 1;
      case IBGP:
        return 0;
      default:
        throw new IllegalArgumentException(String.format("Invalid BGP protocol: %s", protocol));
    }
  }

  /**
   * Attempt to calculate the cost to reach given routes next hop IP.
   *
   * @param route bgp route
   * @return if next hop IP matches a route we have, returns the metric for that route; otherwise
   *     {@link Long#MAX_VALUE}
   */
  private long getIgpCostToNextHopIp(BgpRoute route) {
    if (_mainRib == null) {
      return Long.MAX_VALUE;
    }

    Ip nextHopIp = route.getNextHopIp();
    if (nextHopIp == Ip.AUTO) {
      return Long.MAX_VALUE;
    }
    Set<AnnotatedRoute<AbstractRoute>> s = _mainRib.longestPrefixMatch(nextHopIp);
    return s.isEmpty() ? Long.MAX_VALUE : s.iterator().next().getAbstractRoute().getMetric();
  }
}

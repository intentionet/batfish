package org.batfish.dataplane.rib;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.dataplane.exceptions.BestPathSelectionException;
import org.batfish.dataplane.ibdp.VirtualRouter;

public class BgpBestPathRib extends AbstractRib<BgpRoute> {

  private static final long serialVersionUID = 1L;

  private final BgpTieBreaker _tieBreaker;

  /**
   * Construct an instance with given owner and previous instance.
   *
   * @param owner The virtual router context for this RIB
   * @param backupRoutes a set of alternative (non-best routes)
   */
  public BgpBestPathRib(
      VirtualRouter owner,
      BgpTieBreaker tieBreaker,
      @Nullable Map<Prefix, SortedSet<BgpRoute>> backupRoutes) {
    super(owner, backupRoutes);
    _tieBreaker = tieBreaker;
  }

  /**
   * Construct an initial best-path RIB with no age information for tie-breaking
   *
   * @param owner The virtual router context for this RIB
   * @param backupRoutes a set of alternative (non-best routes)
   * @return A new instance
   */
  public static BgpBestPathRib initial(
      VirtualRouter owner, @Nullable Map<Prefix, SortedSet<BgpRoute>> backupRoutes) {
    return new BgpBestPathRib(owner, BgpTieBreaker.ARRIVAL_ORDER, backupRoutes);
  }

  @Override
  public int comparePreference(BgpRoute lhs, BgpRoute rhs) {

    int res;

    /*
     * first compare local preference
     */
    res = Integer.compare(lhs.getLocalPreference(), rhs.getLocalPreference());
    if (res != 0) {
      return res;
    }

    /*
     * on non-juniper, prefer aggregates (these routes won't appear on
     * juniper)
     */

    res =
        Integer.compare(
            getAggregatePreference(lhs.getProtocol()), getAggregatePreference(rhs.getProtocol()));
    if (res != 0) {
      return res;
    }

    /*
     * then compare as path size (shorter is better, hence reversal)
     */
    res = Integer.compare(rhs.getAsPath().size(), lhs.getAsPath().size());
    if (res != 0) {
      return res;
    }

    /*
     * origin type (IGP better than EGP, which is better than INCOMPLETE)
     */
    res = Integer.compare(lhs.getOriginType().getPreference(), rhs.getOriginType().getPreference());
    if (res != 0) {
      return res;
    }

    /*
     * then compare MED
     *
     * TODO: handle presence/absence of always-compare-med, noting that
     * normally we only do this comparison if the first AS is the same in the
     * paths for both routes
     */

    /*
     * next prefer eBGP over iBGP
     */
    res = Integer.compare(getTypeCost(rhs.getProtocol()), getTypeCost(lhs.getProtocol()));
    if (res != 0) {
      return res;
    }

    /*
     * Prefer the path with the lowest IGP metric to the BGP next hop
     */
    res = Long.compare(getIgpCostToNextHopIp(rhs), getIgpCostToNextHopIp(lhs));
    if (res != 0) {
      return res;
    }

    /*
     * The remaining criteria are used for tie-breaking to end up with a
     * single best-path.
     */

    /*
     * Break tie with process's chosen tie-breaking mechanism
     */
    boolean bothEbgp =
        lhs.getProtocol() == RoutingProtocol.BGP && rhs.getProtocol() == RoutingProtocol.BGP;
    switch (_tieBreaker) {
      case ARRIVAL_ORDER:
        if (!bothEbgp) {
          break;
        }
        /* Flip compare because older is better */
        res =
            Long.compare(
                _logicalArrivalTime.getOrDefault(rhs, _logicalClock),
                _logicalArrivalTime.getOrDefault(lhs, _logicalClock));
        if (res != 0) {
          return res;
        }
        break;

      case ROUTER_ID:
        if (!bothEbgp) {
          break;
        }
        /* Prefer the route that comes from the BGP router with the lowest router ID. */
        res = rhs.getOriginatorIp().compareTo(lhs.getOriginatorIp());
        if (res != 0) {
          return res;
        }
        break;

      case CLUSTER_LIST_LENGTH:
        /* Prefer the path with the minimum cluster list length. lhs/rhs flipped because lower
         * length is preferred.
         */
        res = Integer.compare(rhs.getClusterList().size(), lhs.getClusterList().size());
        if (res != 0) {
          return res;
        }
        break;
      default:
        throw new BestPathSelectionException("Unhandled tie-breaker: " + _tieBreaker);
    }

    /* Prefer the route that comes from the BGP router with the lowest router ID. */
    res = rhs.getOriginatorIp().compareTo(lhs.getOriginatorIp());
    if (res != 0) {
      return res;
    }

    /* Prefer the path with the minimum cluster list length. lhs/rhs flipped because lower
     * length is preferred.
     */
    res = Integer.compare(rhs.getClusterList().size(), lhs.getClusterList().size());
    if (res != 0) {
      return res;
    }

    /* Prefer the path that comes from the lowest neighbor address.
     * Flipped because lower address is preferred.
     */
    res = rhs.getReceivedFromIp().compareTo(lhs.getReceivedFromIp());
    if (res != 0) {
      return res;
    }
    if (lhs.equals(rhs)) {
      // This is ok, because routes are stored in sets
      return 0;
    } else {
      return 1;
    }
  }

  private static int getAggregatePreference(RoutingProtocol protocol) {
    if (protocol == RoutingProtocol.AGGREGATE) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * Return the map of route prefixes to best AS path.
   *
   * @return an immutable map
   */
  public Map<Prefix, AsPath> getBestAsPaths() {
    return getRoutes()
        .stream()
        .collect(ImmutableMap.toImmutableMap(AbstractRoute::getNetwork, BgpRoute::getAsPath));
  }

  private static int getTypeCost(RoutingProtocol protocol) {
    switch (protocol) {
      case AGGREGATE:
        return 0;
      case BGP: // eBGP
        return 1;
      case IBGP:
        return 2;
        // $CASES-OMITTED$
      default:
        throw new BatfishException("Invalid BGP protocol: '" + protocol + "'");
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
    if (_owner == null || _owner.getMainRib() == null) {
      return Long.MAX_VALUE;
    }
    Set<AbstractRoute> s = _owner.getMainRib().longestPrefixMatch(route.getNextHopIp());
    return s == null || s.isEmpty() ? Long.MAX_VALUE : s.iterator().next().getMetric();
  }
}

package org.batfish.dataplane.rib;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.dataplane.ibdp.VirtualRouter;

public class BgpMultipathRib extends AbstractRib<BgpRoute> {

  /** */
  private static final long serialVersionUID = 1L;

  private Map<Prefix, AsPath> _bestAsPaths;

  private MultipathEquivalentAsPathMatchMode _multipathEquivalentAsPathMatchMode;

  public BgpMultipathRib(VirtualRouter owner, MultipathEquivalentAsPathMatchMode meapmm) {
    super(owner, null);
    _multipathEquivalentAsPathMatchMode = meapmm;
  }

  @Override
  public int comparePreference(@Nonnull BgpRoute lhs, @Nonnull BgpRoute rhs) {

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
     * AS path size is same. Now compare to best asPath (if available). Note we do not necessarily
     * guarantee existing rhs route is better than best path, since rhs may have been merged before
     * best as paths map was supplied.
     */
    if (_bestAsPaths != null) {
      AsPath bestAsPath = _bestAsPaths.get(lhs.getNetwork());
      AsPath lhsAsPath = lhs.getAsPath();
      AsPath rhsAsPath = rhs.getAsPath();
      switch (_multipathEquivalentAsPathMatchMode) {
        case EXACT_PATH:
          if (bestAsPath.equals(lhsAsPath)) {
            if (!bestAsPath.equals(rhsAsPath)) {
              return 1;
            }
          } else if (bestAsPath.equals(rhsAsPath)) {
            return -1;
          }
          break;

        case FIRST_AS:
          SortedSet<Integer> lhsFirstAsSet =
              lhsAsPath.getAsSets().isEmpty()
                  ? Collections.emptySortedSet()
                  : lhsAsPath.getAsSets().get(0);
          SortedSet<Integer> rhsFirstAsSet =
              rhsAsPath.getAsSets().isEmpty()
                  ? Collections.emptySortedSet()
                  : rhsAsPath.getAsSets().get(0);
          SortedSet<Integer> bestFirstAsSet =
              bestAsPath.getAsSets().isEmpty()
                  ? Collections.emptySortedSet()
                  : bestAsPath.getAsSets().get(0);

          if (bestFirstAsSet.equals(lhsFirstAsSet)) {
            if (!bestFirstAsSet.equals(rhsFirstAsSet)) {
              return 1;
            }
          } else if (bestFirstAsSet.equals(rhsFirstAsSet)) {
            return -1;
          }
          break;

        case PATH_LENGTH:
          // Skip since already compared
          break;
        default:
          throw new BatfishException(
              String.format(
                  "Unsupported %s: %s",
                  MultipathEquivalentAsPathMatchMode.class.getName(),
                  _multipathEquivalentAsPathMatchMode));
      }
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
    res = Long.compare(rhs.getMetric(), lhs.getMetric());
    if (res != 0) {
      return res;
    }

    /*
     * next prefer eBGP over iBGP
     */
    res = Integer.compare(getTypeCost(rhs.getProtocol()), getTypeCost(lhs.getProtocol()));
    if (res != 0) {
      return res;
    }

    /*
     * The remaining criteria only apply in non-multipath environments. So we
     * end here.
     */
    return res;
  }

  private int getAggregatePreference(RoutingProtocol protocol) {
    if (protocol == RoutingProtocol.AGGREGATE) {
      return 1;
    } else {
      return 0;
    }
  }

  private int getTypeCost(RoutingProtocol protocol) {
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

  public void setBestAsPaths(Map<Prefix, AsPath> bestAsPaths) {
    _bestAsPaths = bestAsPaths;
  }
}

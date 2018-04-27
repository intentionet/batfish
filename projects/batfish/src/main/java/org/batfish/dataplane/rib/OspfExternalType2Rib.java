package org.batfish.dataplane.rib;

import java.util.Map;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.ibdp.VirtualRouter;

public class OspfExternalType2Rib extends AbstractRib<OspfExternalType2Route> {

  private static final long serialVersionUID = 1L;

  public OspfExternalType2Rib(
      VirtualRouter owner, Map<Prefix, SortedSet<OspfExternalType2Route>> backupRoutes) {
    super(owner, backupRoutes);
  }

  @Override
  public int comparePreference(OspfExternalType2Route lhs, OspfExternalType2Route rhs) {
    // reversed on purpose
    int costComparison = Long.compare(rhs.getMetric(), lhs.getMetric());
    if (costComparison != 0) {
      return costComparison;
    }
    return Long.compare(rhs.getCostToAdvertiser(), lhs.getCostToAdvertiser());
  }

  @Override
  @Nullable
  public RibDelta<OspfExternalType2Route> mergeRouteGetDelta(OspfExternalType2Route route) {
    String advertiser = route.getAdvertiser();
    if (route.getCostToAdvertiser() != 0
        && _owner.getConfiguration().getHostname().equals(advertiser)) {
      return null;
    } else {
      return super.mergeRouteGetDelta(route);
    }
  }
}

package org.batfish.dataplane.rib;

import java.util.Map;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.ibdp.VirtualRouter;

public class OspfExternalType1Rib extends AbstractRib<OspfExternalType1Route> {

  /** */
  private static final long serialVersionUID = 1L;

  public OspfExternalType1Rib(
      VirtualRouter owner, @Nullable Map<Prefix, SortedSet<OspfExternalType1Route>> backupRoutes) {
    super(owner, backupRoutes);
  }

  @Override
  public int comparePreference(OspfExternalType1Route lhs, OspfExternalType1Route rhs) {
    // reversed on purpose
    return Long.compare(rhs.getMetric(), lhs.getMetric());
  }

  @Override
  @Nullable
  public RibDelta<OspfExternalType1Route> mergeRouteGetDelta(OspfExternalType1Route route) {
    String advertiser = route.getAdvertiser();
    if (!route.getNonRouting() && _owner.getConfiguration().getHostname().equals(advertiser)) {
      return null;
    } else {
      return super.mergeRouteGetDelta(route);
    }
  }
}

package org.batfish.dataplane.rib;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.dataplane.ibdp.VirtualRouter;

public class Rib extends AbstractRib<AbstractRoute> implements Serializable {

  private static final long serialVersionUID = 1L;

  public Rib(VirtualRouter owner) {
    super(owner, null);
  }

  @Override
  public int comparePreference(AbstractRoute lhs, AbstractRoute rhs) {
    int res;
    // Flipped rhs & lhs because lower value is more preferrable.
    res = Integer.compare(rhs.getAdministrativeCost(), lhs.getAdministrativeCost());
    if (res != 0) {
      return res;
    }
    return Long.compare(rhs.getMetric(), lhs.getMetric());
  }

  @Override
  @Nullable
  public RibDelta<AbstractRoute> mergeRouteGetDelta(AbstractRoute route) {
    if (!route.getNonRouting()) {
      return super.mergeRouteGetDelta(route);
    } else {
      return null;
    }
  }
}

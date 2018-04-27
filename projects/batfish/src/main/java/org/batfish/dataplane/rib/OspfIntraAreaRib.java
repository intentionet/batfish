package org.batfish.dataplane.rib;

import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.dataplane.ibdp.VirtualRouter;

public class OspfIntraAreaRib extends AbstractRib<OspfIntraAreaRoute> {

  /** */
  private static final long serialVersionUID = 1L;

  public OspfIntraAreaRib(VirtualRouter owner) {
    super(owner, null);
  }

  @Override
  public int comparePreference(OspfIntraAreaRoute lhs, OspfIntraAreaRoute rhs) {
    // reversed on purpose
    return Long.compare(rhs.getMetric(), lhs.getMetric());
  }
}

package org.batfish.dataplane.rib;

import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.dataplane.ibdp.VirtualRouter;

public class OspfInterAreaRib extends AbstractRib<OspfInterAreaRoute> {

  /** */
  private static final long serialVersionUID = 1L;

  public OspfInterAreaRib(VirtualRouter owner) {
    super(owner, null);
  }

  @Override
  public int comparePreference(OspfInterAreaRoute lhs, OspfInterAreaRoute rhs) {
    // reversed on purpose
    return Long.compare(rhs.getMetric(), lhs.getMetric());
  }
}

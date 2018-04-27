package org.batfish.dataplane.rib;

import org.batfish.datamodel.StaticRoute;
import org.batfish.dataplane.ibdp.VirtualRouter;

public class StaticRib extends AbstractRib<StaticRoute> {

  /** */
  private static final long serialVersionUID = 1L;

  public StaticRib(VirtualRouter owner) {
    super(owner, null);
  }

  @Override
  public int comparePreference(StaticRoute lhs, StaticRoute rhs) {
    /** TODO: see if this needs to be changed */
    return 0;
  }
}

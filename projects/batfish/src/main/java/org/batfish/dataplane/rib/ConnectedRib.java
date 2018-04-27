package org.batfish.dataplane.rib;

import org.batfish.datamodel.ConnectedRoute;
import org.batfish.dataplane.ibdp.VirtualRouter;

public class ConnectedRib extends AbstractRib<ConnectedRoute> {

  /** */
  private static final long serialVersionUID = 1L;

  public ConnectedRib(VirtualRouter owner) {
    super(owner, null);
  }

  @Override
  public int comparePreference(ConnectedRoute lhs, ConnectedRoute rhs) {
    return 0;
  }
}

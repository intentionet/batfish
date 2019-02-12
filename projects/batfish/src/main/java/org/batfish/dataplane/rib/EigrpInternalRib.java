package org.batfish.dataplane.rib;

import java.util.Comparator;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.EigrpInternalRoute;

/** Rib that stores internal EIGRP routes */
public class EigrpInternalRib extends AbstractRib<EigrpInternalRoute> {

  private static final long serialVersionUID = 1L;

  public EigrpInternalRib() {
    super(null);
  }

  @Override
  public int comparePreference(EigrpInternalRoute lhs, EigrpInternalRoute rhs) {
    return Comparator.comparing(EigrpInternalRoute::getCompositeCost)
        // TODO compare MTU
        // https://github.com/batfish/batfish/issues/1946
        .thenComparing(EigrpInternalRoute::getProcessAsn)
        .compare(rhs, lhs);
  }

  @Override
  public AbstractRoute getAbstractRoute(EigrpInternalRoute route) {
    return route;
  }
}

package org.batfish.dataplane.rib;

import java.io.Serializable;
import java.util.Comparator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;

/**
 * Represents a general RIB, capable of storing routes across different protocols. Uses
 * administrative cost (a.k.a admin distance) to determine route preference.
 */
public class Rib extends AbstractRib<AbstractRoute> implements Serializable {

  private static final long serialVersionUID = 1L;

  /** Create a new empty RIB. */
  public Rib() {
    super(null);
  }

  @Override
  public int comparePreference(@Nonnull AbstractRoute lhs, @Nonnull AbstractRoute rhs) {
    // Flipped rhs & lhs because lower values are preferable.
    return Comparator.comparing(AbstractRoute::getAdministrativeCost)
        .thenComparing(AbstractRoute::getMetric)
        .compare(rhs, lhs);
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

package org.batfish.datamodel.acl;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * This class pairs an {@link AclLineMatchExpr}, which represents a single ACL line, with a set that
 * represents its provenance -- the set of all items that the line was derived from. We sometimes
 * want different kinds of provenance information, so the item type T is a parameter.
 */
public class AclLineMatchExprWithProvenance<T>
    implements Comparable<AclLineMatchExprWithProvenance<T>> {

  @Nonnull private AclLineMatchExpr _matchExpr;

  @Nonnull private IdentityHashMap<AclLineMatchExpr, Set<T>> _provenance;

  public AclLineMatchExprWithProvenance(
      @Nonnull AclLineMatchExpr matchExpr,
      @Nonnull IdentityHashMap<AclLineMatchExpr, Set<T>> provenance) {
    _matchExpr = matchExpr;
    _provenance = provenance;
  }

  @Override
  public final int compareTo(AclLineMatchExprWithProvenance<T> o) {
    if (this == o) {
      return 0;
    }
    return Comparator.comparing(AclLineMatchExprWithProvenance<T>::getMatchExpr).compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(getClass() == o.getClass())) {
      return false;
    }
    return Objects.equals(_matchExpr, ((AclLineMatchExprWithProvenance<?>) o)._matchExpr);
  }

  @Nonnull
  public AclLineMatchExpr getMatchExpr() {
    return _matchExpr;
  }

  @Nonnull
  public IdentityHashMap<AclLineMatchExpr, Set<T>> getProvenance() {
    return _provenance;
  }

  @Override
  public int hashCode() {
    return _matchExpr.hashCode();
  }
}

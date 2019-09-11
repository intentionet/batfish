package org.batfish.datamodel.routing_policy.communities;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Matches a {@link CommunitySet} if it is matched by any of the provided {@link
 * CommunitySetMatchExpr}s. If empty, never matches.
 */
public final class CommunitySetMatchAny extends CommunitySetMatchExpr {

  public CommunitySetMatchAny(Iterable<CommunitySetMatchExpr> exprs) {
    _exprs = ImmutableSet.copyOf(exprs);
  }

  public @Nonnull Set<CommunitySetMatchExpr> getExprs() {
    return _exprs;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CommunitySetMatchAny)) {
      return false;
    }
    return _exprs.equals(((CommunitySetMatchAny) obj)._exprs);
  }

  @Override
  public int hashCode() {
    return _exprs.hashCode();
  }

  @Override
  protected <T> T accept(CommunitySetMatchExprVisitor<T> visitor) {
    return visitor.visitCommunitySetMatchAny(this);
  }

  private static final String PROP_EXPRS = "exprs";

  @JsonCreator
  private static @Nonnull CommunitySetMatchAny create(
      @JsonProperty(PROP_EXPRS) @Nullable Iterable<CommunitySetMatchExpr> exprs) {
    return new CommunitySetMatchAny(ImmutableSet.copyOf(firstNonNull(exprs, ImmutableSet.of())));
  }

  private final @Nonnull Set<CommunitySetMatchExpr> _exprs;

  @JsonProperty(PROP_EXPRS)
  private @Nonnull SortedSet<CommunitySetMatchExpr> getExprsSorted() {
    // sorted for refs
    return ImmutableSortedSet.copyOf(_exprs);
  }
}

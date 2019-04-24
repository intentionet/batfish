package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;
import org.batfish.datamodel.visitors.VoidCommunitySetExprVisitor;

/** A {@link CommunitySetExpr} matching all community-sets. */
public class EmptyCommunitySetExpr extends CommunitySetExpr {

  public static final EmptyCommunitySetExpr INSTANCE = new EmptyCommunitySetExpr();

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static @Nonnull EmptyCommunitySetExpr create() {
    return INSTANCE;
  }

  private EmptyCommunitySetExpr() {}

  @Override
  public <T> T accept(CommunitySetExprVisitor<T> visitor) {
    return visitor.visitEmptyCommunitySetExpr(this);
  }

  @Override
  public void accept(VoidCommunitySetExprVisitor visitor) {
    visitor.visitEmptyCommunitySetExpr(this);
  }

  /**
   * When treated as a literal set of communities, {@link EmptyCommunitySetExpr} represents the
   * empty-set.
   */
  @Nonnull
  @Override
  public SortedSet<Community> asLiteralCommunities(@Nonnull Environment environment) {
    return ImmutableSortedSet.of();
  }

  @Override
  public boolean dynamicMatchCommunity() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof EmptyCommunitySetExpr;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean matchCommunities(Environment environment, Set<Community> communitySetCandidate) {
    return false;
  }

  @Override
  public boolean matchCommunity(Environment environment, Community community) {
    return false;
  }

  @Override
  public boolean reducible() {
    return true;
  }
}

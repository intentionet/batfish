package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;
import org.batfish.datamodel.visitors.VoidCommunitySetExprVisitor;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class CommunitySetExpr implements Serializable {

  private static final long serialVersionUID = 1L;

  public abstract <T> T accept(CommunitySetExprVisitor<T> visitor);

  public abstract void accept(VoidCommunitySetExprVisitor visitor);

  /**
   * Returns the set of all literal communities represented by this {@link CommunitySetExpr}.<br>
   * {@code environment} is used to resolve references to named {@link
   * org.batfish.datamodel.CommunityList}s.
   *
   * @throws UnsupportedOperationException if this {@link CommunitySetExpr} does not represent a set
   *     of literal communities.
   */
  public abstract @Nonnull SortedSet<Community> asLiteralCommunities(
      @Nonnull Environment environment);

  /**
   * Whether membership of a single community in this {@link CommunitySetExpr} cannot be statically
   * determined.
   */
  public abstract boolean dynamicMatchCommunity();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  /**
   * Returns true iff any of the given {@code communityCandidates} is matched by this {@link
   * CommunitySetExpr} under the provided {@code environment}.
   */
  public boolean matchAnyCommunity(Environment environment, Set<Community> communityCandidates) {
    return communityCandidates.stream()
        .anyMatch(communityCandidate -> matchCommunity(environment, communityCandidate));
  }

  /**
   * Returns true iff the given {@code communitySetCandidate} as a whole is matched by this {@link
   * CommunitySetExpr} under the provided {@code environment}.
   */
  public abstract boolean matchCommunities(
      Environment environment, Set<Community> communitySetCandidate);

  /**
   * Returns true iff the given {@code community} is matched by this {@link CommunitySetExpr} under
   * the provided {@code environment}.
   */
  public abstract boolean matchCommunity(Environment environment, Community community);

  /**
   * Returns the subset of the given {@code communityCandidates} matched by this {@link
   * CommunitySetExpr} under the provided {@code environment}.
   */
  public SortedSet<Community> matchedCommunities(
      @Nullable Environment environment, Set<Community> communityCandidates) {
    return communityCandidates.stream()
        .filter(communityCandidate -> matchCommunity(environment, communityCandidate))
        .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
  }

  /**
   * Whether checking membership of a candidate set of communities as a whole in this {@link
   * CommunitySetExpr} can be reduced to checking individual membership of any community from the
   * candidate set in. This must be {@code false} e.g. if this {@link CommunitySetExpr} only matches
   * community-sets with at least two elements.
   */
  public abstract boolean reducible();
}

package org.batfish.datamodel.routing_policy.expr;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.MoreObjects.toStringHelper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;
import org.batfish.datamodel.visitors.VoidCommunitySetExprVisitor;

/**
 * A {@link CommunitySetExpr} matching community-sets containing ANY of the communites returned by
 * {@link #getCommunities()}.
 */
public class LiteralCommunitySet extends CommunitySetExpr {

  private static final String PROP_COMMUNITIES = "communities";

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static @Nonnull LiteralCommunitySet create(
      @JsonProperty(PROP_COMMUNITIES) SortedSet<Community> communities) {
    return new LiteralCommunitySet(firstNonNull(communities, ImmutableSortedSet.of()));
  }

  private final SortedSet<Community> _communities;

  public LiteralCommunitySet(@Nonnull Collection<? extends Community> communities) {
    _communities = ImmutableSortedSet.copyOf(communities);
  }

  @Override
  public <T> T accept(CommunitySetExprVisitor<T> visitor) {
    return visitor.visitLiteralCommunitySet(this);
  }

  @Override
  public void accept(VoidCommunitySetExprVisitor visitor) {
    visitor.visitLiteralCommunitySet(this);
  }

  /**
   * When treated as a literal set of communities, represents exactly the set returned by {@link
   * #getCommunities()}.
   */
  @Nonnull
  @Override
  public SortedSet<Community> asLiteralCommunities(@Nonnull Environment environment) {
    return _communities;
  }

  @Override
  public boolean dynamicMatchCommunity() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof LiteralCommunitySet)) {
      return false;
    }
    return _communities.equals(((LiteralCommunitySet) obj)._communities);
  }

  @JsonProperty(PROP_COMMUNITIES)
  public @Nonnull SortedSet<Community> getCommunities() {
    return _communities;
  }

  @Override
  public int hashCode() {
    return _communities.hashCode();
  }

  @Override
  public boolean matchCommunities(Environment environment, Set<Community> communitySetCandidate) {
    return communitySetCandidate.stream().anyMatch(_communities::contains);
  }

  @Override
  public boolean matchCommunity(Environment environment, Community community) {
    return _communities.contains(community);
  }

  @Override
  public boolean reducible() {
    return true;
  }

  @Override
  public String toString() {
    return toStringHelper(getClass()).add(PROP_COMMUNITIES, _communities).toString();
  }
}

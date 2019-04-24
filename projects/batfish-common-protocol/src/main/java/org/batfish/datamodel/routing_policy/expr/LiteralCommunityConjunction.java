package org.batfish.datamodel.routing_policy.expr;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.MoreObjects.toStringHelper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;
import org.batfish.datamodel.visitors.VoidCommunitySetExprVisitor;

/**
 * A {@link CommunitySetExpr} matching only community-sets that contain ALL of the communities
 * returned by {@link #getRequiredCommunities()}.
 */
public final class LiteralCommunityConjunction extends CommunitySetExpr {

  private static final String PROP_REQUIRED_COMMUNITIES = "requiredCommunities";

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static @Nonnull LiteralCommunityConjunction create(
      @JsonProperty(PROP_REQUIRED_COMMUNITIES) SortedSet<Community> requiredCommunities) {
    return new LiteralCommunityConjunction(
        firstNonNull(requiredCommunities, ImmutableSortedSet.of()));
  }

  private SortedSet<Community> _requiredCommunities;

  public LiteralCommunityConjunction(@Nonnull Collection<Community> requiredCommunities) {
    _requiredCommunities = ImmutableSortedSet.copyOf(requiredCommunities);
  }

  @Override
  public <T> T accept(CommunitySetExprVisitor<T> visitor) {
    return visitor.visitLiteralCommunityConjunction(this);
  }

  @Override
  public void accept(VoidCommunitySetExprVisitor visitor) {
    visitor.visitLiteralCommunityConjunction(this);
  }

  @Nonnull
  @Override
  public SortedSet<Community> asLiteralCommunities(@Nonnull Environment environment) {
    throw new UnsupportedOperationException(
        "Cannot be represented as a set of literal communities");
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
    if (!(obj instanceof LiteralCommunityConjunction)) {
      return false;
    }
    return _requiredCommunities.equals(((LiteralCommunityConjunction) obj)._requiredCommunities);
  }

  @JsonProperty(PROP_REQUIRED_COMMUNITIES)
  public SortedSet<Community> getRequiredCommunities() {
    return _requiredCommunities;
  }

  @Override
  public int hashCode() {
    return _requiredCommunities.hashCode();
  }

  @Override
  public boolean matchCommunities(Environment environment, Set<Community> communitySetCandidate) {
    return Sets.intersection(_requiredCommunities, communitySetCandidate).size()
        == _requiredCommunities.size();
  }

  @Override
  public boolean matchCommunity(Environment environment, Community community) {
    throw new UnsupportedOperationException("Can only be used to match a set of communities");
  }

  @Override
  public boolean reducible() {
    return false;
  }

  @Override
  public String toString() {
    return toStringHelper(getClass())
        .add(PROP_REQUIRED_COMMUNITIES, _requiredCommunities)
        .toString();
  }
}

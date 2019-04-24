package org.batfish.datamodel.routing_policy.expr;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;
import org.batfish.datamodel.visitors.VoidCommunitySetExprVisitor;

/**
 * A {@link CommunitySetExpr} matching community-sets that contain at least the community returned
 * by {@link #getCommunity()}.
 */
@ParametersAreNonnullByDefault
public class LiteralCommunity extends CommunitySetExpr {

  private static final String PROP_COMMUNITY = "community";

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static @Nonnull LiteralCommunity create(
      @Nullable @JsonProperty(PROP_COMMUNITY) Community community) {
    checkArgument(community != null);
    return new LiteralCommunity(community);
  }

  private final Community _community;

  public LiteralCommunity(Community community) {
    _community = community;
  }

  @Override
  public <T> T accept(CommunitySetExprVisitor<T> visitor) {
    return visitor.visitLiteralCommunity(this);
  }

  @Override
  public void accept(VoidCommunitySetExprVisitor visitor) {
    visitor.visitLiteralCommunity(this);
  }

  /**
   * When treated as a literal set of communities, {@link LiteralCommunity} represents the singleton
   * set of the community returned by {@link #getCommunity}.
   */
  @Nonnull
  @Override
  public SortedSet<Community> asLiteralCommunities(Environment environment) {
    return ImmutableSortedSet.of(_community);
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
    if (!(obj instanceof LiteralCommunity)) {
      return false;
    }
    return _community.equals(((LiteralCommunity) obj)._community);
  }

  @JsonProperty(PROP_COMMUNITY)
  public Community getCommunity() {
    return _community;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_community);
  }

  @Override
  public boolean matchCommunities(Environment environment, Set<Community> communitySetCandidate) {
    return communitySetCandidate.contains(_community);
  }

  @Override
  public boolean matchCommunity(Environment environment, Community community) {
    return _community.equals(community);
  }

  @Override
  public boolean reducible() {
    return true;
  }

  @Override
  public String toString() {
    return toStringHelper(getClass()).add(PROP_COMMUNITY, _community).toString();
  }
}

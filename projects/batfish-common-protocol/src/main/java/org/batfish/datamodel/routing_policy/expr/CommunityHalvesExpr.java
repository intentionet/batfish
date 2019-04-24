package org.batfish.datamodel.routing_policy.expr;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;
import org.batfish.datamodel.visitors.VoidCommunitySetExprVisitor;

/**
 * A {@link CommunitySetExpr} that matches 32-bit community values via two separate {@link
 * CommunityHalfExpr} matchers for the value's upper and lower 16-bits, retrievable via {@link
 * #getLeft()} and {@link #getRight()} respectively.
 */
public class CommunityHalvesExpr extends CommunitySetExpr {

  private static final String PROP_LEFT = "left";
  private static final String PROP_RIGHT = "right";

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static @Nonnull CommunityHalvesExpr create(
      @JsonProperty(PROP_LEFT) CommunityHalfExpr left,
      @JsonProperty(PROP_RIGHT) CommunityHalfExpr right) {
    return new CommunityHalvesExpr(requireNonNull(left), requireNonNull(right));
  }

  private final CommunityHalfExpr _left;
  private final CommunityHalfExpr _right;

  public CommunityHalvesExpr(@Nonnull CommunityHalfExpr left, @Nonnull CommunityHalfExpr right) {
    _left = left;
    _right = right;
  }

  @Override
  public <T> T accept(CommunitySetExprVisitor<T> visitor) {
    return visitor.visitCommunityHalvesExpr(this);
  }

  @Override
  public void accept(VoidCommunitySetExprVisitor visitor) {
    visitor.visitCommunityHalvesExpr(this);
  }

  @Nonnull
  @Override
  public SortedSet<Community> asLiteralCommunities(@Nonnull Environment environment) {
    throw new UnsupportedOperationException(
        "Cannot be represented as a list of literal communities");
  }

  @Override
  public boolean dynamicMatchCommunity() {
    return _left.dynamicMatchCommunity() || _right.dynamicMatchCommunity();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CommunityHalvesExpr)) {
      return false;
    }
    CommunityHalvesExpr rhs = (CommunityHalvesExpr) obj;
    return _left.equals(rhs._left) && _right.equals(rhs._right);
  }

  @JsonProperty(PROP_LEFT)
  public @Nonnull CommunityHalfExpr getLeft() {
    return _left;
  }

  @JsonProperty(PROP_RIGHT)
  public @Nonnull CommunityHalfExpr getRight() {
    return _right;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_left, _right);
  }

  @Override
  public boolean matchCommunities(Environment environment, Set<Community> communitySetCandidate) {
    return communitySetCandidate.stream()
        .anyMatch(communityCandidate -> matchCommunity(environment, communityCandidate));
  }

  @Override
  public boolean matchCommunity(Environment environment, Community community) {
    if (!(community instanceof StandardCommunity)) {
      return false;
    }
    return _left.matches(((StandardCommunity) community).high())
        && _right.matches(((StandardCommunity) community).low());
  }

  @Override
  public boolean reducible() {
    return true;
  }

  @Override
  public String toString() {
    return toStringHelper(getClass()).add(PROP_LEFT, _left).add(PROP_RIGHT, _right).toString();
  }
}

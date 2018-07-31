package org.batfish.datamodel.routing_policy.expr;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.routing_policy.Environment;

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
  public SortedSet<Long> asLiteralCommunities(Environment environment) {
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
    if (!(obj instanceof RegexCommunitySet)) {
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
  public boolean matchCommunities(Environment environment, Set<Long> communitySetCandidate) {
    return communitySetCandidate.stream().anyMatch(communityCandidate -> matchCommunity(environment, communityCandidate));
  }

  @Override
  public boolean matchCommunity(Environment environment, long community) {
    return _left.matches((int)(community >> 16)) && _right.matches((int)(community & 0xFFFFL));
  }

  @Override
  public boolean reducible() {
    return true;
  }
}

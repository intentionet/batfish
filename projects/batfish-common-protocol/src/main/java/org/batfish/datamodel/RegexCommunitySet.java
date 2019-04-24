package org.batfish.datamodel;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.io.Serializable;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;
import org.batfish.datamodel.visitors.VoidCommunitySetExprVisitor;

/**
 * A {@link CommunitySetExpr} that matches communities via a regular expression applied to the
 * string representation of the community's 32-bit value. This string comprises two unsigned 16-bit
 * numbers written in decimal separated by a colon, e.g. '1234:65535'.
 */
public final class RegexCommunitySet extends CommunitySetExpr {

  private final class PatternSupplier implements Supplier<Pattern>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public Pattern get() {
      return Pattern.compile(_regex);
    }
  }

  private static final String PROP_REGEX = "regex";

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static @Nonnull RegexCommunitySet create(@JsonProperty(PROP_REGEX) String regex) {
    return new RegexCommunitySet(requireNonNull(regex));
  }

  private final Supplier<Pattern> _pattern;

  private final String _regex;

  public RegexCommunitySet(@Nonnull String regex) {
    _regex = regex;
    _pattern = Suppliers.memoize(new PatternSupplier());
  }

  @Override
  public <T> T accept(CommunitySetExprVisitor<T> visitor) {
    return visitor.visitRegexCommunitySet(this);
  }

  @Override
  public void accept(VoidCommunitySetExprVisitor visitor) {
    visitor.visitRegexCommunitySet(this);
  }

  @Nonnull
  @Override
  public SortedSet<Community> asLiteralCommunities(@Nonnull Environment environment) {
    throw new UnsupportedOperationException(
        "Cannot be represented as a list of literal communities");
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
    if (!(obj instanceof RegexCommunitySet)) {
      return false;
    }
    return _regex.equals(((RegexCommunitySet) obj)._regex);
  }

  @JsonProperty(PROP_REGEX)
  public @Nonnull String getRegex() {
    return _regex;
  }

  @Override
  public int hashCode() {
    return _regex.hashCode();
  }

  @Override
  public boolean matchCommunities(Environment environment, Set<Community> communitySetCandidate) {
    return communitySetCandidate.stream()
        .anyMatch(community -> matchCommunity(environment, community));
  }

  @Override
  public boolean matchCommunity(Environment environment, Community community) {
    return _pattern.get().matcher(community.matchString()).find();
  }

  @Override
  public boolean reducible() {
    return true;
  }
}

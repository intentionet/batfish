package org.batfish.datamodel.routing_policy.expr;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.visitors.CommunitySetExprVisitor;
import org.batfish.datamodel.visitors.VoidCommunitySetExprVisitor;

public class NamedCommunitySet extends CommunitySetExpr {

  private static final String PROP_NAME = "name";

  private static final long serialVersionUID = 1L;

  @JsonCreator
  private static NamedCommunitySet create(@JsonProperty(PROP_NAME) String name) {
    return new NamedCommunitySet(requireNonNull(name));
  }

  private final String _name;

  public NamedCommunitySet(@Nonnull String name) {
    _name = name;
  }

  @Override
  public <T> T accept(CommunitySetExprVisitor<T> visitor) {
    return visitor.visitNamedCommunitySet(this);
  }

  @Override
  public void accept(VoidCommunitySetExprVisitor visitor) {
    visitor.visitNamedCommunitySet(this);
  }

  @Nonnull
  @Override
  public SortedSet<Community> asLiteralCommunities(@Nonnull Environment environment) {
    return resolve(environment).asLiteralCommunities(environment);
  }

  @Override
  public final boolean dynamicMatchCommunity() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof NamedCommunitySet)) {
      return false;
    }
    return _name.equals(((NamedCommunitySet) obj)._name);
  }

  @JsonProperty(PROP_NAME)
  public String getName() {
    return _name;
  }

  @Override
  public int hashCode() {
    return _name.hashCode();
  }

  @Override
  public boolean matchAnyCommunity(Environment environment, Set<Community> communityCandidates) {
    return resolve(environment).matchAnyCommunity(environment, communityCandidates);
  }

  @Override
  public boolean matchCommunities(Environment environment, Set<Community> communitySetCandidate) {
    return resolve(environment).matchCommunities(environment, communitySetCandidate);
  }

  @Override
  public boolean matchCommunity(Environment environment, Community community) {
    return environment.getCommunityLists().get(_name).matchCommunity(environment, community);
  }

  @Override
  public SortedSet<Community> matchedCommunities(
      Environment environment, Set<Community> communityCandidates) {
    return resolve(environment).matchedCommunities(environment, communityCandidates);
  }

  @Override
  public boolean reducible() {
    return false;
  }

  private @Nonnull CommunitySetExpr resolve(@Nonnull Environment environment) {
    return requireNonNull(environment.getCommunityLists().get(_name));
  }
}

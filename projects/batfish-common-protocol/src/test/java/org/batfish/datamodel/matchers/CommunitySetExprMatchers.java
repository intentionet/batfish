package org.batfish.datamodel.matchers;

import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public final class CommunitySetExprMatchers {

  private static final class AsLiteralCommunities
      extends TypeSafeDiagnosingMatcher<CommunitySetExpr> {

    private final Environment _environment;

    private final Matcher<? super Set<Long>> _subMatcher;

    private AsLiteralCommunities(
        @Nullable Environment environment, @Nonnull Matcher<? super Set<Long>> subMatcher) {
      _environment = environment;
      _subMatcher = subMatcher;
    }

    @Override
    public void describeTo(Description description) {
      description
          .appendText(
              "A CommunitySetExpr whose representation as a set of literal communities matches:")
          .appendDescriptionOf(_subMatcher);
    }

    @Override
    protected boolean matchesSafely(CommunitySetExpr item, Description mismatchDescription) {
      Set<Community> asLiteralCommunities = item.asLiteralCommunities(_environment);
      if (!_subMatcher.matches(asLiteralCommunities)) {
        mismatchDescription.appendText(
            String.format(
                "CommunitySetExpr: '%s' as literal communities was: %s",
                item, CommunitySetExprMatchers.toString(asLiteralCommunities)));
        return false;
      }
      return true;
    }
  }

  private static final class MatchAnyCommunity extends TypeSafeDiagnosingMatcher<CommunitySetExpr> {

    private final Set<Community> _communityCandidates;

    private final Environment _environment;

    private MatchAnyCommunity(
        @Nullable Environment environment, @Nonnull Set<Community> communityCandidates) {
      _environment = environment;
      _communityCandidates = communityCandidates;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(
          String.format(
              "A CommunitySetExpr matching any community in the candidate set: '%s'",
              CommunitySetExprMatchers.toString(_communityCandidates)));
    }

    @Override
    protected boolean matchesSafely(CommunitySetExpr item, Description mismatchDescription) {
      if (!item.matchAnyCommunity(_environment, _communityCandidates)) {
        mismatchDescription.appendText(
            String.format(
                "CommunitySetExpr: '%s' did not match any community in: %s",
                item, CommunitySetExprMatchers.toString(_communityCandidates)));
        return false;
      }
      return true;
    }
  }

  private static final class MatchCommunities extends TypeSafeDiagnosingMatcher<CommunitySetExpr> {

    private final Set<Community> _communitySetCandidate;

    private final Environment _environment;

    private MatchCommunities(
        @Nullable Environment environment, @Nonnull Set<Community> communitySetCandidate) {
      _environment = environment;
      _communitySetCandidate = communitySetCandidate;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(
          String.format(
              "A CommunitySetExpr matching as a whole the candidate comunity-set: %s",
              CommunitySetExprMatchers.toString(_communitySetCandidate)));
    }

    @Override
    protected boolean matchesSafely(CommunitySetExpr item, Description mismatchDescription) {
      boolean match = item.matchCommunities(_environment, _communitySetCandidate);
      if (!match) {
        mismatchDescription.appendText(
            String.format(
                "CommunitySetExpr: '%s' did not match the community-set: '%s'",
                item, CommunitySetExprMatchers.toString(_communitySetCandidate)));
        return false;
      }
      return true;
    }
  }

  private static final class MatchCommunity extends TypeSafeDiagnosingMatcher<CommunitySetExpr> {

    private final Community _community;

    private final Environment _environment;

    private MatchCommunity(@Nullable Environment environment, Community community) {
      _environment = environment;
      _community = community;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(
          String.format(
              "A CommunitySetExpr matching the individual community: %s", _community.toString()));
    }

    @Override
    protected boolean matchesSafely(CommunitySetExpr item, Description mismatchDescription) {
      boolean match = item.matchCommunity(_environment, _community);
      if (!match) {
        mismatchDescription.appendText(
            String.format(
                "CommunitySetExpr: '%s' did not match the community: '%s'",
                item, _community.toString()));
        return false;
      }
      return true;
    }
  }

  private static final class MatchedCommunities
      extends TypeSafeDiagnosingMatcher<CommunitySetExpr> {

    private final Set<Community> _communityCandidates;

    private final Environment _environment;

    private final Matcher<? super Set<Community>> _subMatcher;

    private MatchedCommunities(
        @Nullable Environment environment,
        @Nonnull Set<Community> communityCandidates,
        @Nonnull Matcher<? super Set<Community>> subMatcher) {
      _environment = environment;
      _communityCandidates = communityCandidates;
      _subMatcher = subMatcher;
    }

    @Override
    public void describeTo(Description description) {
      description
          .appendText(
              String.format(
                  "A CommunitySetExpr whose set of matching communities for the candidate set: '%s' matches:",
                  CommunitySetExprMatchers.toString(_communityCandidates)))
          .appendDescriptionOf(_subMatcher);
    }

    @Override
    protected boolean matchesSafely(CommunitySetExpr item, Description mismatchDescription) {
      Set<Community> matchedCommunities =
          item.matchedCommunities(_environment, _communityCandidates);
      if (!_subMatcher.matches(matchedCommunities)) {
        mismatchDescription.appendText(
            String.format(
                "CommunitySetExpr: '%s' matched the subset: %s",
                item, CommunitySetExprMatchers.toString(matchedCommunities)));
        return false;
      }
      return true;
    }
  }

  /**
   * Provides a matcher that matches when the provided {@code subMatcher} matches the {@link
   * CommunitySetExpr}'s representation as a set of literal communities under the provided {@code
   * environment}.
   */
  public static @Nonnull Matcher<CommunitySetExpr> asLiteralCommunities(
      @Nullable Environment environment, @Nonnull Matcher<? super Set<Long>> subMatcher) {
    return new AsLiteralCommunities(environment, subMatcher);
  }

  /**
   * Provides a matcher that matches when any of the provided {@code communityCandidates} is matched
   * by the {link CommunitySetExpr} under the provided {@code environment}.
   */
  public static @Nonnull Matcher<CommunitySetExpr> matchAnyCommunity(
      @Nullable Environment environment, @Nonnull Set<Community> communityCandidates) {
    return new MatchAnyCommunity(environment, communityCandidates);
  }

  /**
   * Provides a matcher that matches when the provided {@code communitySetCandidate} as a whole is
   * matched by the {link CommunitySetExpr} under the provided {@code environment}.
   */
  public static @Nonnull Matcher<CommunitySetExpr> matchCommunities(
      @Nullable Environment environment, @Nonnull Set<Community> communitySetCandidate) {
    return new MatchCommunities(environment, communitySetCandidate);
  }

  /**
   * Provides a matcher that matches when the provided {@code community} is matched by the {link
   * CommunitySetExpr} under the provided {@code environment}.
   */
  public static @Nonnull Matcher<CommunitySetExpr> matchCommunity(
      @Nullable Environment environment, Community community) {
    return new MatchCommunity(environment, community);
  }

  /**
   * Provides a matcher that matches when the subset of the given {@code communityCandidates}
   * matched by the {@link CommunitySetExpr} is equal to the {@code exprectedMatchedCommunities}
   * under the provided {@code environment}.
   */
  public static @Nonnull Matcher<CommunitySetExpr> matchedCommunities(
      @Nullable Environment environment,
      @Nonnull Set<Community> communityCandidates,
      @Nonnull Set<Community> expectedMatchedCommunities) {
    return new MatchedCommunities(
        environment, communityCandidates, equalTo(expectedMatchedCommunities));
  }

  private static @Nonnull String toString(Set<Community> communitySet) {
    return communitySet.stream()
        .map(Community::toString)
        .collect(ImmutableSet.toImmutableSet())
        .toString();
  }

  private CommunitySetExprMatchers() {}
}

package org.batfish.datamodel.routing_policy.expr;

import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.matchCommunities;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.util.Set;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class LiteralCommunityConjunctionTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testAsLiteralCommunities() {
    LiteralCommunityConjunction l =
        new LiteralCommunityConjunction(
            ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(2L)));

    _thrown.expect(UnsupportedOperationException.class);
    l.asLiteralCommunities(null);
  }

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new LiteralCommunityConjunction(
                ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(2L))),
            new LiteralCommunityConjunction(
                ImmutableSet.of(StandardCommunity.of(2L), StandardCommunity.of(1L))))
        .addEqualityGroup(
            new LiteralCommunityConjunction(ImmutableSet.of(StandardCommunity.of(2L))))
        .testEquals();
  }

  @Test
  public void testMatchAnyCommunity() {
    LiteralCommunityConjunction l =
        new LiteralCommunityConjunction(
            ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(2L)));

    _thrown.expect(UnsupportedOperationException.class);
    l.matchAnyCommunity(null, ImmutableSet.of(StandardCommunity.of(1L)));
  }

  @Test
  public void testMatchCommunities() {
    Set<Community> matchingCommunitySetCandidate1 =
        ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(3L));
    Set<Community> matchingCommunitySetCandidate2 =
        ImmutableSet.of(
            StandardCommunity.of(1L), StandardCommunity.of(2L), StandardCommunity.of(3L));
    Set<Community> nonMatchingCommunitySetCandidate1 = ImmutableSet.of(StandardCommunity.of(1L));
    Set<Community> nonMatchingCommunitySetCandidate2 = ImmutableSet.of(StandardCommunity.of(3L));

    LiteralCommunityConjunction l =
        new LiteralCommunityConjunction(
            ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(3L)));

    assertThat(l, matchCommunities(null, matchingCommunitySetCandidate1));
    assertThat(l, matchCommunities(null, matchingCommunitySetCandidate2));
    assertThat(l, not(matchCommunities(null, nonMatchingCommunitySetCandidate1)));
    assertThat(l, not(matchCommunities(null, nonMatchingCommunitySetCandidate2)));
  }

  @Test
  public void testMatchCommunity() {
    LiteralCommunityConjunction l =
        new LiteralCommunityConjunction(
            ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(3L)));

    _thrown.expect(UnsupportedOperationException.class);
    l.matchCommunity(null, StandardCommunity.of(1L));
  }

  @Test
  public void testMatchedCommunities() {
    LiteralCommunityConjunction l =
        new LiteralCommunityConjunction(
            ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(3L)));

    _thrown.expect(UnsupportedOperationException.class);
    l.matchedCommunities(null, ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(2L)));
  }
}

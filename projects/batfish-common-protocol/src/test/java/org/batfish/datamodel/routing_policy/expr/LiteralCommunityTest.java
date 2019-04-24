package org.batfish.datamodel.routing_policy.expr;

import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.asLiteralCommunities;
import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.matchAnyCommunity;
import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.matchCommunities;
import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.matchCommunity;
import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.matchedCommunities;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.util.Set;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.junit.Test;

public final class LiteralCommunityTest {

  @Test
  public void testAsLiteralCommunities() {
    StandardCommunity val = StandardCommunity.of(1L);
    LiteralCommunity l = new LiteralCommunity(val);

    assertThat(l, asLiteralCommunities(null, equalTo(ImmutableSet.of(val))));
  }

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new LiteralCommunity(StandardCommunity.of(1L)),
            new LiteralCommunity(StandardCommunity.of(1L)))
        .addEqualityGroup(new LiteralCommunity(StandardCommunity.of(2L)))
        .testEquals();
  }

  @Test
  public void testMatchAnyCommunity() {
    LiteralCommunity l = new LiteralCommunity(StandardCommunity.of(1L));
    Set<Community> communityCandidates =
        ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(2L));

    assertThat(l, matchAnyCommunity(null, communityCandidates));
  }

  @Test
  public void testMatchCommunities() {
    Set<Community> matchingCommunitySetCandidate1 =
        ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(2L));
    Set<Community> matchingCommunitySetCandidate2 = ImmutableSet.of(StandardCommunity.of(1L));
    Set<Community> nonMatchingCommunitySetCandidate = ImmutableSet.of(StandardCommunity.of(2L));
    LiteralCommunity l = new LiteralCommunity(StandardCommunity.of(1L));

    assertThat(l, matchCommunities(null, matchingCommunitySetCandidate1));
    assertThat(l, matchCommunities(null, matchingCommunitySetCandidate2));
    assertThat(l, not(matchCommunities(null, nonMatchingCommunitySetCandidate)));
  }

  @Test
  public void testMatchCommunity() {
    LiteralCommunity l = new LiteralCommunity(StandardCommunity.of(1L));

    assertThat(l, matchCommunity(null, StandardCommunity.of(1L)));
    assertThat(l, not(matchCommunity(null, StandardCommunity.of(2L))));
  }

  @Test
  public void testMatchedCommunities() {
    LiteralCommunity l = new LiteralCommunity(StandardCommunity.of(1L));
    Set<Community> communityCandidates =
        ImmutableSet.of(StandardCommunity.of(1L), StandardCommunity.of(2L));

    assertThat(
        l,
        matchedCommunities(null, communityCandidates, ImmutableSet.of(StandardCommunity.of(1L))));
  }
}

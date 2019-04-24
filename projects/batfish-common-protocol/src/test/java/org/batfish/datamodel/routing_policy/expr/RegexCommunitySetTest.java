package org.batfish.datamodel.routing_policy.expr;

import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.matchAnyCommunity;
import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.matchCommunities;
import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.matchCommunity;
import static org.batfish.datamodel.matchers.CommunitySetExprMatchers.matchedCommunities;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.util.Set;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RegexCommunitySetTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void testAsLiteralCommunities() {
    RegexCommunitySet r = new RegexCommunitySet("^1:1$");

    _thrown.expect(UnsupportedOperationException.class);
    r.asLiteralCommunities(null);
  }

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(
            new RegexCommunitySet("1:1"), new RegexCommunitySet(String.format("1:%s", "1")))
        .addEqualityGroup(new RegexCommunitySet("^2:2"))
        .testEquals();
  }

  @Test
  public void testMatchAnyCommunity() {
    RegexCommunitySet l = new RegexCommunitySet("1:1");
    Set<Community> communityCandidates =
        ImmutableSet.of(StandardCommunity.parse("1:1"), StandardCommunity.of(2L));

    assertThat(l, matchAnyCommunity(null, communityCandidates));
  }

  @Test
  public void testMatchCommunities() {
    Set<Community> matchingCommunitySetCandidate1 =
        ImmutableSet.of(StandardCommunity.parse("1:1"), StandardCommunity.of(2L));
    Set<Community> matchingCommunitySetCandidate2 =
        ImmutableSet.of(StandardCommunity.parse("11:11"));
    Set<Community> nonMatchingCommunitySetCandidate = ImmutableSet.of(StandardCommunity.of(2L));
    RegexCommunitySet r = new RegexCommunitySet("1:1");

    assertThat(r, matchCommunities(null, matchingCommunitySetCandidate1));
    assertThat(r, matchCommunities(null, matchingCommunitySetCandidate2));
    assertThat(r, not(matchCommunities(null, nonMatchingCommunitySetCandidate)));
  }

  @Test
  public void testMatchCommunity() {
    RegexCommunitySet l = new RegexCommunitySet("1:1");

    assertThat(l, matchCommunity(null, StandardCommunity.parse("1:1")));
    assertThat(l, not(matchCommunity(null, StandardCommunity.of(2L))));
  }

  @Test
  public void testMatchedCommunities() {
    RegexCommunitySet l = new RegexCommunitySet("1:1");
    Set<Community> communityCandidates =
        ImmutableSet.of(StandardCommunity.parse("1:1"), StandardCommunity.of(2L));

    assertThat(
        l,
        matchedCommunities(
            null, communityCandidates, ImmutableSet.of(StandardCommunity.parse("1:1"))));
  }
}

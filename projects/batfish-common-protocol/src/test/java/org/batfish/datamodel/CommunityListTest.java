package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunity;
import org.junit.Test;

public class CommunityListTest {

  private static final StandardCommunity COMMUNITY1 = StandardCommunity.of(1L);

  private static final String NAME = "cl";

  @Test
  public void testDefaultReject() {
    CommunityList cl = new CommunityList(NAME, ImmutableList.of(), false);

    assertThat(cl.matchCommunity(null, COMMUNITY1), equalTo(false));
  }

  @Test
  public void testPermit() {
    CommunityList cl =
        new CommunityList(
            NAME,
            ImmutableList.of(
                new CommunityListLine(LineAction.PERMIT, new LiteralCommunity(COMMUNITY1))),
            false);

    assertThat(cl.matchCommunity(null, COMMUNITY1), equalTo(true));
  }

  @Test
  public void testReject() {
    CommunityList cl =
        new CommunityList(
            NAME,
            ImmutableList.of(
                new CommunityListLine(LineAction.DENY, new LiteralCommunity(COMMUNITY1))),
            false);

    assertThat(cl.matchCommunity(null, COMMUNITY1), equalTo(false));
  }
}

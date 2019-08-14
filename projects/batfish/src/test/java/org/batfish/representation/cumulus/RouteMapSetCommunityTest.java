package org.batfish.representation.cumulus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.statement.SetCommunity;
import org.junit.Test;

public class RouteMapSetCommunityTest {

  @Test
  public void toStatements() {
    List<StandardCommunity> communities =
        ImmutableList.of(StandardCommunity.parse("65000:1"), StandardCommunity.parse("65000:2"));
    RouteMapSetCommunity set = new RouteMapSetCommunity(communities);

    assertThat(
        set.toStatements(null, null, null).collect(ImmutableList.toImmutableList()),
        equalTo(ImmutableList.of(new SetCommunity(new LiteralCommunitySet(communities)))));
  }
}

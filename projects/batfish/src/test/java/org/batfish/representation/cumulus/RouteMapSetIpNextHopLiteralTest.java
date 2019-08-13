package org.batfish.representation.cumulus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.routing_policy.expr.IpNextHop;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.junit.Test;

public class RouteMapSetIpNextHopLiteralTest {

  private static ImmutableList<Ip> IPS;

  @Test
  public void testGetNextHops() {
    RouteMapSetIpNextHopLiteral set = new RouteMapSetIpNextHopLiteral(IPS);
    assertThat(set.getNextHops(), equalTo(IPS));
  }

  @Test
  public void testToStatements() {
    IPS = ImmutableList.of(Ip.parse("10.0.0.1"), Ip.parse("10.0.0.2"));
    RouteMapSetIpNextHopLiteral set = new RouteMapSetIpNextHopLiteral(IPS);

    ImmutableList<Statement> result =
        set.toStatements(null, null, null).collect(ImmutableList.toImmutableList());
    assertThat(result, equalTo(ImmutableList.of(new SetNextHop(new IpNextHop(IPS), false))));
  }
}

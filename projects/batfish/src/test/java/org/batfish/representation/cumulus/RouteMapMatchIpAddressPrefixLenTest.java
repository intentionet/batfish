package org.batfish.representation.cumulus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.junit.Test;

public class RouteMapMatchIpAddressPrefixLenTest {

  @Test
  public void testConstructionAndGetter() {
    RouteMapMatchIpAddressPrefixLen match = new RouteMapMatchIpAddressPrefixLen(7);
    assertThat(match.getLen(), equalTo(7));
  }

  @Test
  public void testToBooleanExpr() {
    RouteMapMatchIpAddressPrefixLen match = new RouteMapMatchIpAddressPrefixLen(7);
    CumulusNcluConfiguration config = new CumulusNcluConfiguration();
    Configuration c = Configuration.builder().setHostname("c").build();
    BooleanExpr expr = match.toBooleanExpr(c, config, new Warnings());
    for (Prefix p :
        new Prefix[] {Prefix.ZERO, Prefix.parse("10.0.0.0/6"), Prefix.parse("10.0.0.0/8")}) {
      StaticRoute route = StaticRoute.testBuilder().setNetwork(p).build();
      assertThat(
          expr.evaluate(Environment.builder(c).setOriginalRoute(route).build()),
          equalTo(Result.builder().setBooleanValue(false).build()));
    }
    for (Prefix p : new Prefix[] {Prefix.parse("10.0.0.0/7"), Prefix.parse("20.0.0.0/7")}) {
      StaticRoute route = StaticRoute.testBuilder().setNetwork(p).build();
      assertThat(
          expr.evaluate(Environment.builder(c).setOriginalRoute(route).build()),
          equalTo(Result.builder().setBooleanValue(true).build()));
    }
  }
}

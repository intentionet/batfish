package org.batfish.datamodel.routing_policy.expr;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import java.util.NavigableMap;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Configuration.Builder;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link FirstMatchChain} */
@RunWith(JUnit4.class)
public class FirstMatchChainTest {

  private static RoutingPolicy P1 =
      RoutingPolicy.builder()
          .setName("p1")
          .setStatements(
              ImmutableList.of(
                  new If(
                      new MatchTag(IntComparator.EQ, new LiteralInt(1)),
                      ImmutableList.of(Statements.ReturnTrue.toStaticStatement()))))
          .build();
  private static RoutingPolicy P2 =
      RoutingPolicy.builder()
          .setName("p2")
          .setStatements(
              ImmutableList.of(
                  new If(
                      new MatchTag(IntComparator.EQ, new LiteralInt(2)),
                      ImmutableList.of(Statements.ReturnFalse.toStaticStatement()))))
          .build();
  private static RoutingPolicy DEFAULT_POLICY =
      RoutingPolicy.builder()
          .setName("default-policy")
          .setStatements(
              ImmutableList.of(
                  new If(
                      BooleanExprs.TRUE, ImmutableList.of(new SetMetric(new LiteralLong(500L))))))
          .build();

  private Environment buildEnvironment(
      NavigableMap<String, RoutingPolicy> policies, String defaultPolicy, AbstractRoute route) {
    NetworkFactory nf = new NetworkFactory();
    Builder cb = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.JUNIPER);
    Configuration c = cb.build();
    c.setVrfs(
        ImmutableMap.of(Configuration.DEFAULT_VRF_NAME, new Vrf(Configuration.DEFAULT_VRF_NAME)));
    c.setRoutingPolicies(policies);
    return Environment.builder(c, Configuration.DEFAULT_VRF_NAME)
        .setDefaultPolicy(defaultPolicy)
        .setOutputRoute(route.toBuilder())
        .build();
  }

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(new Object())
        .addEqualityGroup(
            new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE)),
            new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE)))
        .addEqualityGroup(ImmutableList.of())
        .addEqualityGroup(ImmutableList.of(BooleanExprs.FALSE))
        .testEquals();
  }

  @Test
  public void testJavaSerialization() {
    FirstMatchChain fmc = new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE));
    assertThat(SerializationUtils.clone(fmc), equalTo(fmc));
  }

  @Test
  public void testJsonSerialization() throws IOException {
    FirstMatchChain fmc = new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE));
    assertThat(BatfishObjectMapper.clone(fmc, FirstMatchChain.class), equalTo(fmc));
  }

  @Test
  public void testEvaluate() {
    // Test that first match is used
    FirstMatchChain fmc =
        new FirstMatchChain(ImmutableList.of(BooleanExprs.FALSE, BooleanExprs.TRUE));
    Result result =
        fmc.evaluate(
            Environment.builder(new Configuration("host", ConfigurationFormat.JUNIPER)).build());
    assertThat(result, equalTo(new Result(false, false, false, false)));

    fmc = new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE, BooleanExprs.FALSE));
    result =
        fmc.evaluate(
            Environment.builder(new Configuration("host", ConfigurationFormat.JUNIPER)).build());
    assertThat(result, equalTo(new Result(true, false, false, false)));
  }

  @Test
  public void testWithCallExprs() {
    /*
    Two policies in chain, plus a default policy:
    - P1 accepts routes with tag 1
    - P2 rejects routes with tag 2
    - Default policy sets route's metric to 500 (so we can ascertain whether a route has hit it)
     */
    FirstMatchChain policiesChain =
        new FirstMatchChain(
            ImmutableList.of(new CallExpr(P1.getName()), new CallExpr(P2.getName())));
    NavigableMap<String, RoutingPolicy> policiesMap =
        ImmutableSortedMap.of(
            P1.getName(), P1, P2.getName(), P2, DEFAULT_POLICY.getName(), DEFAULT_POLICY);
    StaticRoute.Builder srb =
        StaticRoute.builder().setNetwork(Prefix.parse("1.1.1.0/24")).setAdmin(5).setMetric(10L);

    // Route with tag 1 should be accepted by policy 1, no metric change
    Environment environment =
        buildEnvironment(policiesMap, DEFAULT_POLICY.getName(), srb.setTag(1).build());
    Result result = policiesChain.evaluate(environment);
    assertThat(environment.getOutputRoute().getMetric(), equalTo(10L));
    assertThat(result, equalTo(new Result(true, false, false, false)));

    // Route with tag 2 should be rejected by policy 2, no metric change
    environment = buildEnvironment(policiesMap, DEFAULT_POLICY.getName(), srb.setTag(2).build());
    result = policiesChain.evaluate(environment);
    assertThat(environment.getOutputRoute().getMetric(), equalTo(10L));
    assertThat(result, equalTo(new Result(false, false, false, false)));

    // Route with tag 3 should fall through policies 1 and 2, hit default policy, and update metric
    environment = buildEnvironment(policiesMap, DEFAULT_POLICY.getName(), srb.setTag(3).build());
    result = policiesChain.evaluate(environment);
    assertThat(environment.getOutputRoute().getMetric(), equalTo(500L));
    assertThat(result, equalTo(new Result(false, false, true, false)));
  }

  @Test
  public void testToString() {
    FirstMatchChain fmc = new FirstMatchChain(ImmutableList.of(BooleanExprs.TRUE));
    assertThat(fmc.toString(), equalTo("FirstMatchChain{subroutines=[True]}"));
  }
}

package org.batfish.datamodel.routing_policy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.ConjunctionChain;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.FirstMatchChain;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.BufferedStatement;
import org.batfish.datamodel.routing_policy.statement.CallStatement;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests reference tracking in {@link RoutingPolicy}. */
@RunWith(JUnit4.class)
public class RoutingPolicyReferenceTest {

  private static Configuration _c;

  private static RoutingPolicy.Builder _rpb;

  private static Warnings _w;

  @Before
  public void initializeFactory() {
    NetworkFactory nf = new NetworkFactory();
    _c = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    _rpb = nf.routingPolicyBuilder().setOwner(_c);
    _w = new Warnings(true, true, true);
  }

  /** Policy with actual circular reference as statement */
  @Test
  public void testRoutingPolicyCircularReference() {
    String parentPolicyName = "parent";
    _rpb.setName(parentPolicyName)
        .setStatements(ImmutableList.of(new CallStatement(parentPolicyName)))
        .build();
    _c.computeRoutingPolicySources(_w);

    /*
     * A circular reference warning should be emitted containing the name of the circularly
     * referenced policy.
     */
    assertThat(_w.getRedFlagWarnings(), not(empty()));
    assertThat(
        _w.getRedFlagWarnings().iterator().next().getText(), containsString(parentPolicyName));
  }

  /** Policy with actual circular reference as expr */
  @Test
  public void testRoutingPolicyCircularReferenceExpr() {
    String parentPolicyName = "parent";

    CallExpr callExpr = new CallExpr(parentPolicyName);
    If ifStatement = new If();
    ifStatement.setGuard(callExpr);

    _rpb.setName(parentPolicyName).setStatements(ImmutableList.of(ifStatement)).build();
    _c.computeRoutingPolicySources(_w);

    /*
     * A circular reference warning should be emitted containing the name of the circularly
     * referenced policy.
     */
    assertThat(_w.getRedFlagWarnings(), not(empty()));
    assertThat(
        _w.getRedFlagWarnings().iterator().next().getText(), containsString(parentPolicyName));
  }

  /** Policy with actual circular reference deep in the policy */
  @Test
  public void testRoutingPolicyDeepCircularReference() {
    String parentPolicyName = "parent";

    CallStatement callStatement = new CallStatement(parentPolicyName);

    BufferedStatement bs = new BufferedStatement(callStatement);

    WithEnvironmentExpr we1 = new WithEnvironmentExpr();
    we1.setPostStatements(ImmutableList.of(bs));

    If if1 = new If();
    if1.setGuard(we1);

    WithEnvironmentExpr we2 = new WithEnvironmentExpr();
    we2.setPostTrueStatements(ImmutableList.of(if1));

    If if2 = new If();
    if2.setGuard(we2);

    If if3 = new If();
    if3.setTrueStatements(ImmutableList.of(if2));

    If if4 = new If();
    if4.setFalseStatements(ImmutableList.of(if3));

    WithEnvironmentExpr we3 = new WithEnvironmentExpr();
    we3.setPreStatements(ImmutableList.of(if4));

    WithEnvironmentExpr we4 = new WithEnvironmentExpr();
    we4.setExpr(we3);

    Conjunction conj = new Conjunction();
    conj.setConjuncts(ImmutableList.of(we4));

    ConjunctionChain conjunctionChain = new ConjunctionChain(ImmutableList.of(conj));

    Disjunction disjunction = new Disjunction();
    disjunction.setDisjuncts(ImmutableList.of(conjunctionChain));

    FirstMatchChain firstMatchChain = new FirstMatchChain(ImmutableList.of(disjunction));

    Not not = new Not(firstMatchChain);

    If if5 = new If();
    if5.setGuard(not);

    _rpb.setName(parentPolicyName).setStatements(ImmutableList.of(if5)).build();
    _c.computeRoutingPolicySources(_w);

    /*
     * A circular reference warning should be emitted containing the name of the circularly
     * referenced policy.
     */
    assertThat(_w.getRedFlagWarnings(), not(empty()));
    assertThat(
        _w.getRedFlagWarnings().iterator().next().getText(), containsString(parentPolicyName));
  }

  /** Policy with two copies of same call statement - should not contain circular reference */
  @Test
  public void testRoutingPolicyTwoCopiesCallStatement() {
    RoutingPolicy calledPolicy = _rpb.build();
    Statement callStatement = new CallStatement(calledPolicy.getName());
    _rpb.setStatements(ImmutableList.of(callStatement, callStatement)).build();
    _c.computeRoutingPolicySources(_w);

    // No circular reference warnings should be emitted
    assertThat(_w.getRedFlagWarnings(), empty());
  }

  /**
   * Policy with two different call statements for same policy - should not contain circular
   * reference
   */
  @Test
  public void testRoutingPolicyTwoDifferentCallStatementsSamePolicy() {
    RoutingPolicy calledPolicy = _rpb.build();
    Statement callStatement1 = new CallStatement(calledPolicy.getName());
    Statement callStatement2 = new CallStatement(calledPolicy.getName());
    _rpb.setStatements(ImmutableList.of(callStatement1, callStatement2)).build();
    _c.computeRoutingPolicySources(_w);

    // No circular reference warnings should be emitted
    assertThat(_w.getRedFlagWarnings(), empty());
  }
}

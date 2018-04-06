package org.batfish.datamodel.acl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.matchers.AclLineMatchExprMatchers;
import org.junit.Test;

public class AndExprTest {
  private static Flow createFlow() {
    Flow.Builder b = new Flow.Builder();
    b.setIngressNode("ingressNode");
    b.setTag("empty");
    return b.build();
  }

  @Test
  public void testSingleExpr() {
    // Test that if and only if the only ACL line is a match, the AndMatchExpr indicates a match

    TrueExpr eTrue = new TrueExpr();
    FalseExpr eFalse = new FalseExpr();

    // Setup simple expression with a single true boolean expr
    Set<AclLineMatchExpr> setTrue = new HashSet<>();
    setTrue.add(eTrue);
    AndMatchExpr exprTrue = new AndMatchExpr(setTrue);

    // Setup simple expression with a single false boolean expr
    Set<AclLineMatchExpr> setFalse = new HashSet<>();
    setFalse.add(eFalse);
    AndMatchExpr exprFalse = new AndMatchExpr(setFalse);

    // Confirm true boolean expr matches
    assertThat(exprTrue, AclLineMatchExprMatchers.matches(createFlow(), "", new HashMap<>()));
    // Confirm false boolean expr does not match
    assertThat(exprFalse, not(AclLineMatchExprMatchers.matches(createFlow(), "", new HashMap<>())));
  }

  @Test
  public void testMultipleExprs() {
    // Test that if and only if all ACL lines are a match, the AndMatchExpr returns a match

    TrueExpr eTrue = new TrueExpr();
    FalseExpr eFalse = new FalseExpr();

    // Setup
    Set<AclLineMatchExpr> setTrueTrue = new HashSet<>();
    setTrueTrue.add(eTrue);
    setTrueTrue.add(eTrue);
    AndMatchExpr exprTrueTrue = new AndMatchExpr(setTrueTrue);

    Set<AclLineMatchExpr> setTrueFalse = new HashSet<>();
    setTrueFalse.add(eTrue);
    setTrueFalse.add(eFalse);
    AndMatchExpr exprTrueFalse = new AndMatchExpr(setTrueFalse);

    Set<AclLineMatchExpr> setFalseFalse = new HashSet<>();
    setFalseFalse.add(eFalse);
    setFalseFalse.add(eFalse);
    AndMatchExpr exprFalseFalse = new AndMatchExpr(setFalseFalse);

    // Confirm boolean expr true AND true = true
    assertThat(exprTrueTrue, AclLineMatchExprMatchers.matches(createFlow(), "", new HashMap<>()));
    // Confirm boolean expr true AND false = false
    assertThat(
        exprTrueFalse, not(AclLineMatchExprMatchers.matches(createFlow(), "", new HashMap<>())));
    // Confirm boolean expr false AND false = false
    assertThat(
        exprFalseFalse, not(AclLineMatchExprMatchers.matches(createFlow(), "", new HashMap<>())));
  }
}

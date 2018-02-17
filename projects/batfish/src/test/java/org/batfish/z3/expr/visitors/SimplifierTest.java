package org.batfish.z3.expr.visitors;

import static com.google.common.collect.ImmutableList.of;
import static org.batfish.z3.expr.visitors.Simplifier.simplifyBooleanExpr;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.Prefix;
import org.batfish.z3.BasicHeaderField;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.CurrentIsOriginalExpr;
import org.batfish.z3.expr.EqExpr;
import org.batfish.z3.expr.FalseExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.IfExpr;
import org.batfish.z3.expr.IntExpr;
import org.batfish.z3.expr.LitIntExpr;
import org.batfish.z3.expr.NotExpr;
import org.batfish.z3.expr.OrExpr;
import org.batfish.z3.expr.PrefixMatchExpr;
import org.batfish.z3.expr.RangeMatchExpr;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.expr.TrueExpr;
import org.batfish.z3.expr.VarIntExpr;
import org.batfish.z3.state.NumberedQuery;
import org.junit.Test;

public class SimplifierTest {

  private int _atomCounter;

  private BooleanExpr newAtom() {
    return new NumberedQuery(_atomCounter++);
  }

  /** Test that we keep simplifying until no more simplifications are possible. */
  @Test
  public void testChainedSimplifications() {
    OrExpr or = new OrExpr(of(new OrExpr(of(new OrExpr(of())))));

    assertThat(simplifyBooleanExpr(or), equalTo(FalseExpr.INSTANCE));
  }

  /** Test that NOT NOT P == P. */
  @Test
  public void testSimplfyNotDoubleNegation() {
    BooleanExpr p1 = newAtom();

    assertThat(simplifyBooleanExpr(new NotExpr(new NotExpr(p1))), equalTo(p1));
  }

  /**
   * Test that an AND node with a single child (other than TRUE or FALSE) simplifies to that child.
   */
  @Test
  public void testSimplifyAnd1() {
    BooleanExpr p1 = newAtom();
    AndExpr and = new AndExpr(of(p1));

    assertThat(simplifyBooleanExpr(and), equalTo(p1));
  }

  /** Test simplifications: false AND E --> false E AND false --> false. */
  @Test
  public void testSimplifyAndExprFalseConjunct() {
    BooleanExpr p1 = newAtom();
    AndExpr leftFalse = new AndExpr(of(FalseExpr.INSTANCE, p1));
    AndExpr rightFalse = new AndExpr(of(p1, FalseExpr.INSTANCE));

    assertThat(simplifyBooleanExpr(leftFalse), equalTo(FalseExpr.INSTANCE));
    assertThat(simplifyBooleanExpr(rightFalse), equalTo(FalseExpr.INSTANCE));
  }

  /** Test that nested ANDs get flattened into one AND. */
  @Test
  public void testSimplifyAndExprNestedConjuncts() {
    BooleanExpr p1 = newAtom();
    BooleanExpr p2 = newAtom();
    BooleanExpr p3 = newAtom();
    AndExpr leftNested = new AndExpr(of(new AndExpr(of(p1, p2)), p3));
    AndExpr rightNested = new AndExpr(of(p1, new AndExpr(of(p2, p3))));

    assertThat(leftNested, not(equalTo(rightNested)));
    assertThat(simplifyBooleanExpr(leftNested), equalTo(new AndExpr(of(p1, p2, p3))));
    assertThat(simplifyBooleanExpr(rightNested), equalTo(new AndExpr(of(p1, p2, p3))));
  }

  /** Test that an unsimplifiable expression is unchanged by simplification. */
  @Test
  public void testSimplifyAndExprPreserveUnalteredInstance() {
    BooleanExpr p1 = newAtom();
    BooleanExpr p2 = newAtom();
    AndExpr and = new AndExpr(of(p1, p2));

    assertThat(simplifyBooleanExpr(and), sameInstance(and));
  }

  /** Test that any TRUE children of an AND node are removed. */
  @Test
  public void testSimplifyAndTrue() {
    BooleanExpr p1 = newAtom();
    BooleanExpr p2 = newAtom();

    AndExpr and = new AndExpr(of(TrueExpr.INSTANCE, p1, TrueExpr.INSTANCE, p2));

    assertThat(simplifyBooleanExpr(and), equalTo(new AndExpr(of(p1, p2))));
  }

  /** Test that an empty AND node is simplified to true (the identity under AND). */
  @Test
  public void testSimplifyEmptyAnd() {
    AndExpr and = new AndExpr(of());

    assertThat(simplifyBooleanExpr(and), equalTo(TrueExpr.INSTANCE));
  }

  /** Test that an empty OR node is simplified to false (the identity under OR). */
  @Test
  public void testSimplifyEmptyOr() {
    OrExpr or = new OrExpr(of());

    assertThat(simplifyBooleanExpr(or), equalTo(FalseExpr.INSTANCE));
  }

  /**
   * Test that an EQ node with LHS and RHS not statically determinable is unchanged by
   * simplification.
   */
  @Test
  public void testSimplifyEqPreserveUnalteredInstance() {
    IntExpr i1 = new VarIntExpr(BasicHeaderField.DST_IP);
    IntExpr i2 = new VarIntExpr(BasicHeaderField.SRC_IP);
    EqExpr eq = new EqExpr(i1, i2);

    assertThat(simplifyBooleanExpr(eq), sameInstance(eq));
  }

  /** Test that an EQ node with syntactically equal LHS and RHS simplifies to TRUE. */
  @Test
  public void testSimplifyEqSame() {
    IntExpr i1 = new VarIntExpr(BasicHeaderField.DST_IP);
    EqExpr eq = new EqExpr(i1, i1);

    assertThat(simplifyBooleanExpr(eq), equalTo(TrueExpr.INSTANCE));
  }

  /**
   * Test that an EQ node with LHS and RHS statically determinable to be unequal simplifies to
   * FALSE.
   */
  @Test
  public void testSimplifyEqStaticallyFalse() {
    IntExpr i1 = new LitIntExpr(Ip.ZERO);
    IntExpr i2 = new LitIntExpr(Ip.MAX);
    EqExpr eq = new EqExpr(i1, i2);

    assertThat(simplifyBooleanExpr(eq), equalTo(FalseExpr.INSTANCE));
  }

  /**
   * Test that an IF with antecedent statically determinable to be TRUE simplifies to the
   * consequent.
   */
  @Test
  public void testSimplifyIfAntecedentStaticallyConsequent() {
    BooleanExpr p1 = newAtom();

    assertThat(simplifyBooleanExpr(new IfExpr(TrueExpr.INSTANCE, p1)), equalTo(p1));
    assertThat(
        simplifyBooleanExpr(new IfExpr(new NotExpr(new NotExpr(TrueExpr.INSTANCE)), p1)),
        equalTo(p1));
  }

  /**
   * Test that an IF with antecedent statically FALSE, or consequent statically TRUE, or antecedent
   * statically equal to consequent simplifies to TRUE.
   */
  @Test
  public void testSimplifyIfStaticallyTrue() {
    BooleanExpr p1 = newAtom();

    // Antecedent is false
    assertThat(
        simplifyBooleanExpr(new IfExpr(FalseExpr.INSTANCE, newAtom())), equalTo(TrueExpr.INSTANCE));
    assertThat(
        simplifyBooleanExpr(new IfExpr(new NotExpr(new NotExpr(FalseExpr.INSTANCE)), newAtom())),
        equalTo(TrueExpr.INSTANCE));

    // Consequent is true
    assertThat(
        simplifyBooleanExpr(new IfExpr(newAtom(), TrueExpr.INSTANCE)), equalTo(TrueExpr.INSTANCE));
    assertThat(
        simplifyBooleanExpr(new IfExpr(newAtom(), new NotExpr(FalseExpr.INSTANCE))),
        equalTo(TrueExpr.INSTANCE));

    // Antecedent == Consequent
    assertThat(simplifyBooleanExpr(new IfExpr(p1, p1)), equalTo(TrueExpr.INSTANCE));
    assertThat(
        simplifyBooleanExpr(new IfExpr(new NotExpr(new NotExpr(p1)), p1)),
        equalTo(TrueExpr.INSTANCE));
  }

  /**
   * Test that an IF with unsimplifiable antecedent and consequent is unchanged by simplification.
   */
  @Test
  public void testSimplifyIfUnchanged() {
    BooleanExpr p1 = newAtom();
    BooleanExpr p2 = newAtom();
    BooleanExpr ifExpr = new IfExpr(p1, p2);

    assertThat(simplifyBooleanExpr(ifExpr), sameInstance(ifExpr));
  }

  /** Test that NOT FALSE == TRUE. */
  @Test
  public void testSimplifyNotFalse() {
    assertThat(simplifyBooleanExpr(new NotExpr(FalseExpr.INSTANCE)), equalTo(TrueExpr.INSTANCE));
  }

  /** Test that NOT TRUE == FALSE. */
  @Test
  public void testSimplifyNotTrue() {
    assertThat(simplifyBooleanExpr(new NotExpr(TrueExpr.INSTANCE)), equalTo(FalseExpr.INSTANCE));
  }

  /** Test that NOT with unsimplifiable argument is unchanged by simplification. */
  @Test
  public void testSimplifyNotUnchanged() {
    NotExpr not = new NotExpr(newAtom());

    assertThat(simplifyBooleanExpr(not), sameInstance(not));
  }

  /**
   * Test that an OR node with a single child (other than TRUE or FALSE) simplifies to that child.
   */
  @Test
  public void testSimplifyOr1() {
    BooleanExpr p1 = newAtom();
    OrExpr or = new OrExpr(of(p1));

    assertThat(simplifyBooleanExpr(or), equalTo(p1));
  }

  /** Test that any FALSE children of an OR node are removed. */
  @Test
  public void testSimplifyOrFalse() {
    BooleanExpr p1 = newAtom();
    BooleanExpr p2 = newAtom();
    OrExpr and = new OrExpr(of(FalseExpr.INSTANCE, p1, FalseExpr.INSTANCE, p2));

    assertThat(simplifyBooleanExpr(and), equalTo(new OrExpr(of(p1, p2))));
  }

  /** Test that an OR node with a TRUE child simplifies to TRUE. */
  @Test
  public void testSimplifyOrTrue() {
    BooleanExpr p1 = newAtom();
    OrExpr or = new OrExpr(of(p1, TrueExpr.INSTANCE));

    assertThat(simplifyBooleanExpr(or), equalTo(TrueExpr.INSTANCE));
  }

  /** Test that wrapper expressions are changed by simplification */
  @Test
  public void testSimplifyWrappers() {
    BooleanExpr headerSpaceMatchExpr = new HeaderSpaceMatchExpr(IpAccessListLine.builder().build());
    BooleanExpr prefixMatchExpr = new PrefixMatchExpr(BasicHeaderField.DST_IP, Prefix.ZERO);
    BooleanExpr rangeMatchExpr =
        RangeMatchExpr.greaterThanOrEqualTo(BasicHeaderField.DST_IP, 123456L, 10);

    assertThat(
        simplifyBooleanExpr(CurrentIsOriginalExpr.INSTANCE),
        not(equalTo(CurrentIsOriginalExpr.INSTANCE)));
    assertThat(simplifyBooleanExpr(headerSpaceMatchExpr), not(equalTo(headerSpaceMatchExpr)));
    assertThat(simplifyBooleanExpr(prefixMatchExpr), not(equalTo(prefixMatchExpr)));
    assertThat(simplifyBooleanExpr(rangeMatchExpr), not(equalTo(rangeMatchExpr)));
    assertThat(simplifyBooleanExpr(SaneExpr.INSTANCE), not(equalTo(SaneExpr.INSTANCE)));
  }
}

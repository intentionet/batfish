package org.batfish.z3.expr;

/** A @{link RuleStatement} whose target state is a @{link BasicStateExpr} */
public class BasicRuleStatement extends RuleStatement {

  public BasicRuleStatement(BasicStateExpr subExpression) {
    super(subExpression);
  }

  public BasicRuleStatement(BooleanExpr antecedent, BasicStateExpr consequent) {
    super(antecedent, consequent);
  }

  @Override
  public <T> T accept(GenericStatementVisitor<T> visitor) {
    return visitor.visitBasicRuleStatement(this);
  }

  @Override
  public void accept(VoidStatementVisitor visitor) {
    visitor.visitBasicRuleStatement(this);
  }
}

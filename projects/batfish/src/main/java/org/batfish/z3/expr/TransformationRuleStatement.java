package org.batfish.z3.expr;

public class TransformationRuleStatement extends RuleStatement {

  public TransformationRuleStatement(BooleanExpr antecedent, TransformationStateExpr consequent) {
    super(antecedent, consequent);
  }

  public TransformationRuleStatement(TransformationStateExpr subExpression) {
    super(subExpression);
  }

  @Override
  public <T> T accept(GenericStatementVisitor<T> visitor) {
    return visitor.visitTransformationRuleStatement(this);
  }

  @Override
  public void accept(VoidStatementVisitor visitor) {
    visitor.visitTransformationRuleStatement(this);
  }
}

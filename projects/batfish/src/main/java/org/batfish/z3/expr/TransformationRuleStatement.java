package org.batfish.z3.expr;

import java.util.Objects;
import java.util.Set;

/**
 * A @{link RuleStatement} by which a postcondition state reached by a pair of a header and a
 * transformed version it is produced by a set of precondition state(s) reached by the untransformed
 * header; a set of precondition state(s) reached by the transformed header; a set of precondition
 * states reached by the pair; and state-independent constraints
 */
public class TransformationRuleStatement extends RuleStatement {

  private final TransformationStateExpr _postconditionTransformationState;

  private final Set<BasicStateExpr> _preconditionPreTransformationStates;

  private final BooleanExpr _preconditionStateIndependent;

  private final Set<TransformationStateExpr> _preconditionTransformationStates;

  public TransformationRuleStatement(
      TransformationStateExpr postconditionTransformationState,
      Set<BasicStateExpr> preconditionPreTransformationStates,
      BooleanExpr preconditionStateIndependent,
      Set<TransformationStateExpr> preconditionTransformationStates) {
    _postconditionTransformationState = postconditionTransformationState;
    _preconditionPreTransformationStates = preconditionPreTransformationStates;
    _preconditionStateIndependent = preconditionStateIndependent;
    _preconditionTransformationStates = preconditionTransformationStates;
  }

  @Override
  public <T> T accept(GenericStatementVisitor<T> visitor) {
    return visitor.visitTransformationRuleStatement(this);
  }

  @Override
  public void accept(VoidStatementVisitor visitor) {
    visitor.visitTransformationRuleStatement(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _postconditionTransformationState,
        _preconditionPreTransformationStates,
        _preconditionStateIndependent,
        _preconditionTransformationStates);
  }

  @Override
  public boolean statementEquals(Statement e) {
    TransformationRuleStatement rhs = (TransformationRuleStatement) e;
    return Objects.equals(_postconditionTransformationState, rhs._postconditionTransformationState)
        && Objects.equals(
            _preconditionPreTransformationStates, rhs._postconditionTransformationState)
        && Objects.equals(_preconditionStateIndependent, rhs._preconditionStateIndependent)
        && Objects.equals(_preconditionTransformationStates, rhs._preconditionTransformationStates);
  }
}

package org.batfish.z3.state.visitors;

import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.expr.TransformationStateExpr;

public interface GeneralStateVisitor {

  void visitBasicState(BasicStateExpr.State basicState);

  void visitTransformationState(TransformationStateExpr.State transformationState);
}

package org.batfish.z3.expr;

public interface GenericStatementVisitor<T> {
  T visitBasicRuleStatement(BasicRuleStatement basicRuleStatement);

  T visitComment(Comment comment);

  T visitDeclareRelStatement(DeclareRelStatement declareRelStatement);

  T visitDeclareVarStatement(DeclareVarStatement declareVarStatement);

  T visitQueryStatement(QueryStatement queryStatement);

  T visitTransformationRuleStatement(TransformationRuleStatement transformationRuleStatement);
}

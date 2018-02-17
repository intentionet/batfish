package org.batfish.z3.expr;

public interface VoidStatementVisitor {

  void visitBasicRuleStatement(BasicRuleStatement basicRuleStatement);

  void visitComment(Comment comment);

  void visitDeclareRelStatement(DeclareRelStatement declareRelStatement);

  void visitDeclareVarStatement(DeclareVarStatement declareVarStatement);

  void visitQueryStatement(QueryStatement queryStatement);

  void visitTransformationRuleStatement(TransformationRuleStatement transformationRuleStatement);
}

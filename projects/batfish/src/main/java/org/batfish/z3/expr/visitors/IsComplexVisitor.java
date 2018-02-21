package org.batfish.z3.expr.visitors;

import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.expr.BitVecExpr;
import org.batfish.z3.expr.CurrentIsOriginalExpr;
import org.batfish.z3.expr.EqExpr;
import org.batfish.z3.expr.Expr;
import org.batfish.z3.expr.ExtractExpr;
import org.batfish.z3.expr.FalseExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.IdExpr;
import org.batfish.z3.expr.IfExpr;
import org.batfish.z3.expr.ListExpr;
import org.batfish.z3.expr.LitIntExpr;
import org.batfish.z3.expr.NotExpr;
import org.batfish.z3.expr.OrExpr;
import org.batfish.z3.expr.PrefixMatchExpr;
import org.batfish.z3.expr.RangeMatchExpr;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.expr.TransformationStateExpr;
import org.batfish.z3.expr.TransformedExpr;
import org.batfish.z3.expr.TrueExpr;
import org.batfish.z3.expr.VarIntExpr;

public class IsComplexVisitor implements ExprVisitor {

  public static boolean isComplexExpr(Expr expr) {
    IsComplexVisitor visitor = new IsComplexVisitor();
    expr.accept(visitor);
    return visitor._isComplex;
  }

  private boolean _isComplex;

  @Override
  public void visitAndExpr(AndExpr andExpr) {
    _isComplex = true;
  }

  @Override
  public void visitBasicStateExpr(BasicStateExpr basicStateExpr) {
    visitStateExpr(basicStateExpr);
  }

  @Override
  public void visitBitVecExpr(BitVecExpr bitVecExpr) {
    _isComplex = true;
  }

  @Override
  public void visitCurrentIsOriginal(CurrentIsOriginalExpr currentIsOriginalExpr) {
    currentIsOriginalExpr.getExpr().accept(this);
  }

  @Override
  public void visitEqExpr(EqExpr eqExpr) {
    _isComplex = true;
  }

  @Override
  public void visitExtractExpr(ExtractExpr extractExpr) {
    _isComplex = true;
  }

  @Override
  public void visitFalseExpr(FalseExpr falseExpr) {
    _isComplex = false;
  }

  @Override
  public void visitHeaderSpaceMatchExpr(HeaderSpaceMatchExpr headerSpaceMatchExpr) {
    headerSpaceMatchExpr.getExpr().accept(this);
  }

  @Override
  public void visitIdExpr(IdExpr idExpr) {
    _isComplex = false;
  }

  @Override
  public void visitIfExpr(IfExpr ifExpr) {
    _isComplex = true;
  }

  @Override
  public void visitListExpr(ListExpr listExpr) {
    _isComplex = false;
  }

  @Override
  public void visitLitIntExpr(LitIntExpr litIntExpr) {
    _isComplex = false;
  }

  @Override
  public void visitNotExpr(NotExpr notExpr) {
    _isComplex = true;
  }

  @Override
  public void visitOrExpr(OrExpr orExpr) {
    _isComplex = true;
  }

  @Override
  public void visitPrefixMatchExpr(PrefixMatchExpr prefixMatchExpr) {
    prefixMatchExpr.getExpr().accept(this);
  }

  @Override
  public void visitRangeMatchExpr(RangeMatchExpr rangeMatchExpr) {
    rangeMatchExpr.getExpr().accept(this);
  }

  @Override
  public void visitSaneExpr(SaneExpr saneExpr) {
    saneExpr.getExpr().accept(this);
  }

  private void visitStateExpr(StateExpr stateExpr) {
    _isComplex = true;
  }

  @Override
  public void visitTransformationStateExpr(TransformationStateExpr transformationStateExpr) {
    visitStateExpr(transformationStateExpr);
  }

  @Override
  public void visitTransformedExpr(TransformedExpr transformedExpr) {
    _isComplex = true;
  }

  @Override
  public void visitTrueExpr(TrueExpr trueExpr) {
    _isComplex = false;
  }

  @Override
  public void visitVarIntExpr(VarIntExpr varIntExpr) {
    _isComplex = false;
  }
}

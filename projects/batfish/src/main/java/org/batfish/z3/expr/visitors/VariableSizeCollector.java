package org.batfish.z3.expr.visitors;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.batfish.z3.BasicHeaderField;
import org.batfish.z3.HeaderField;
import org.batfish.z3.SynthesizerInput;
import org.batfish.z3.TransformationHeaderField;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BasicRuleStatement;
import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.expr.BitVecExpr;
import org.batfish.z3.expr.Comment;
import org.batfish.z3.expr.CurrentIsOriginalExpr;
import org.batfish.z3.expr.DeclareRelStatement;
import org.batfish.z3.expr.DeclareVarStatement;
import org.batfish.z3.expr.EqExpr;
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
import org.batfish.z3.expr.QueryStatement;
import org.batfish.z3.expr.RangeMatchExpr;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.expr.Statement;
import org.batfish.z3.expr.TransformationRuleStatement;
import org.batfish.z3.expr.TransformationStateExpr;
import org.batfish.z3.expr.TrueExpr;
import org.batfish.z3.expr.VarIntExpr;
import org.batfish.z3.expr.VoidStatementVisitor;

public class VariableSizeCollector implements ExprVisitor, VoidStatementVisitor {

  private static final Supplier<Set<Entry<String, Integer>>> BASIC_STATE_VARIABLE_SIZES =
      () ->
          Arrays.stream(BasicHeaderField.values())
              .map(hf -> Maps.immutableEntry(hf.getName(), hf.getSize()))
              .collect(ImmutableSet.toImmutableSet());

  private static final Supplier<Set<Entry<String, Integer>>> TRANSFORMATION_STATE_VARIABLE_SIZES =
      () ->
          Streams.concat(
                  Arrays.stream(BasicHeaderField.values()),
                  Arrays.stream(TransformationHeaderField.values()))
              .map(hf -> Maps.immutableEntry(hf.getName(), hf.getSize()))
              .collect(ImmutableSet.toImmutableSet());

  public static Map<String, Integer> collectVariableSizes(SynthesizerInput input, Statement s) {
    VariableSizeCollector variableSizeCollector = new VariableSizeCollector(input);
    s.accept(variableSizeCollector);
    return variableSizeCollector
        ._variableSizes
        .build()
        .stream()
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  @SuppressWarnings("unused")
  private final SynthesizerInput _input;

  private final ImmutableSet.Builder<Entry<String, Integer>> _variableSizes;

  private VariableSizeCollector(SynthesizerInput input) {
    _input = input;
    _variableSizes = ImmutableSet.builder();
  }

  @Override
  public void visitAndExpr(AndExpr andExpr) {
    andExpr.getConjuncts().forEach(conjunct -> conjunct.accept(this));
  }

  @Override
  public void visitBasicRuleStatement(BasicRuleStatement basicRuleStatement) {
    basicRuleStatement.getSubExpression().accept(this);
  }

  @Override
  public void visitBasicStateExpr(BasicStateExpr basicStateExpr) {
    _variableSizes.addAll(BASIC_STATE_VARIABLE_SIZES.get());
  }

  @Override
  public void visitBitVecExpr(BitVecExpr bitVecExpr) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void visitComment(Comment comment) {}

  @Override
  public void visitCurrentIsOriginal(CurrentIsOriginalExpr currentIsOriginalExpr) {
    currentIsOriginalExpr.getExpr().accept(this);
  }

  @Override
  public void visitDeclareRelStatement(DeclareRelStatement declareRelStatement) {}

  @Override
  public void visitDeclareVarStatement(DeclareVarStatement declareVarStatement) {}

  @Override
  public void visitEqExpr(EqExpr eqExpr) {
    eqExpr.getLhs().accept(this);
    eqExpr.getRhs().accept(this);
  }

  @Override
  public void visitExtractExpr(ExtractExpr extractExpr) {
    visitVarIntExpr(extractExpr.getVar());
  }

  @Override
  public void visitFalseExpr(FalseExpr falseExpr) {}

  @Override
  public void visitHeaderSpaceMatchExpr(HeaderSpaceMatchExpr headerSpaceMatchExpr) {
    headerSpaceMatchExpr.getExpr().accept(this);
  }

  @Override
  public void visitIdExpr(IdExpr idExpr) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void visitIfExpr(IfExpr ifExpr) {
    ifExpr.getAntecedent().accept(this);
    ifExpr.getConsequent().accept(this);
  }

  @Override
  public void visitListExpr(ListExpr listExpr) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public void visitLitIntExpr(LitIntExpr litIntExpr) {}

  @Override
  public void visitNotExpr(NotExpr notExpr) {
    notExpr.getArg().accept(this);
  }

  @Override
  public void visitOrExpr(OrExpr orExpr) {
    orExpr.getDisjuncts().forEach(disjunct -> disjunct.accept(this));
  }

  @Override
  public void visitPrefixMatchExpr(PrefixMatchExpr prefixMatchExpr) {
    prefixMatchExpr.getExpr().accept(this);
  }

  @Override
  public void visitQueryStatement(QueryStatement queryStatement) {
    queryStatement.getSubExpression().accept(this);
  }

  @Override
  public void visitRangeMatchExpr(RangeMatchExpr rangeMatchExpr) {
    rangeMatchExpr.getExpr().accept(this);
  }

  @Override
  public void visitSaneExpr(SaneExpr saneExpr) {
    saneExpr.getExpr().accept(this);
  }

  @Override
  public void visitTransformationRuleStatement(
      TransformationRuleStatement transformationRuleStatement) {
    transformationRuleStatement.getSubExpression().accept(this);
  }

  @Override
  public void visitTransformationStateExpr(TransformationStateExpr transformationStateExpr) {
    _variableSizes.addAll(TRANSFORMATION_STATE_VARIABLE_SIZES.get());
  }

  @Override
  public void visitTrueExpr(TrueExpr trueExpr) {}

  @Override
  public void visitVarIntExpr(VarIntExpr varIntExpr) {
    HeaderField headerField = varIntExpr.getHeaderField();
    _variableSizes.add(Maps.immutableEntry(headerField.getName(), headerField.getSize()));
  }
}

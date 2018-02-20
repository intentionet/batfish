package org.batfish.z3.expr.visitors;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Streams;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import java.util.Arrays;
import java.util.Set;
import org.batfish.z3.BasicHeaderField;
import org.batfish.z3.NodContext;
import org.batfish.z3.SynthesizerInput;
import org.batfish.z3.TransformationHeaderField;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.CurrentIsOriginalExpr;
import org.batfish.z3.expr.DelegateBooleanExpr;
import org.batfish.z3.expr.EqExpr;
import org.batfish.z3.expr.FalseExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.IfExpr;
import org.batfish.z3.expr.NotExpr;
import org.batfish.z3.expr.OrExpr;
import org.batfish.z3.expr.PrefixMatchExpr;
import org.batfish.z3.expr.RangeMatchExpr;
import org.batfish.z3.expr.SaneExpr;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.expr.TransformationStateExpr;
import org.batfish.z3.expr.TrueExpr;
import org.batfish.z3.expr.VarIntExpr;
import org.batfish.z3.state.StateParameter.Type;
import org.batfish.z3.state.visitors.Parameterizer;

public class BoolExprTransformer implements BooleanExprVisitor {

  public static String getNodName(Set<Type> vectorizedParameters, StateExpr stateExpr) {
    StringBuilder name = new StringBuilder();
    name.append(String.format("S_%s", stateExpr.getClass().getSimpleName()));
    Parameterizer.getParameters(stateExpr)
        .stream()
        .filter(parameter -> !vectorizedParameters.contains(parameter.getType()))
        .forEach(parameter -> name.append(String.format("_%s", parameter.getId())));
    return name.toString();
  }

  public static String getNodName(SynthesizerInput input, StateExpr stateExpr) {
    return getNodName(input.getVectorizedParameters(), stateExpr);
  }

  public static BoolExpr toBoolExpr(
      BooleanExpr booleanExpr, SynthesizerInput input, NodContext nodContext) {
    BoolExprTransformer boolExprTransformer = new BoolExprTransformer(input, nodContext);
    booleanExpr.accept(boolExprTransformer);
    return boolExprTransformer._boolExpr;
  }

  private final Supplier<Expr[]> _basicArguments;

  private BoolExpr _boolExpr;

  private final SynthesizerInput _input;

  private final NodContext _nodContext;

  private final Supplier<Expr[]> _transformationArguments;

  private BoolExprTransformer(SynthesizerInput input, NodContext nodContext) {
    _input = input;
    _nodContext = nodContext;
    _basicArguments =
        Suppliers.memoize(
            () ->
                Arrays.stream(BasicHeaderField.values())
                    .map(VarIntExpr::new)
                    .map(e -> BitVecExprTransformer.toBitVecExpr(e, _nodContext))
                    .toArray(Expr[]::new));
    _transformationArguments =
        Suppliers.memoize(
            () ->
                Streams.concat(
                        Arrays.stream(_basicArguments.get()),
                        Arrays.stream(TransformationHeaderField.values())
                            .map(VarIntExpr::new)
                            .map(e -> BitVecExprTransformer.toBitVecExpr(e, _nodContext)))
                    .toArray(Expr[]::new));
  }

  private Expr[] getBasicRelationArgs(SynthesizerInput input, BasicStateExpr stateExpr) {
    /* TODO: support vectorized state parameters */
    return _basicArguments.get();
  }

  private Expr[] getTransformationRelationArgs(
      SynthesizerInput input, TransformationStateExpr stateExpr) {
    /* TODO: support vectorized state parameters */
    return _transformationArguments.get();
  }

  @Override
  public void visitAndExpr(AndExpr andExpr) {
    _boolExpr =
        _nodContext
            .getContext()
            .mkAnd(
                andExpr
                    .getConjuncts()
                    .stream()
                    .map(conjunct -> toBoolExpr(conjunct, _input, _nodContext))
                    .toArray(BoolExpr[]::new));
  }

  @Override
  public void visitBasicStateExpr(BasicStateExpr basicStateExpr) {
    /* TODO: allow vectorized variables */
    _boolExpr =
        (BoolExpr)
            _nodContext
                .getContext()
                .mkApp(
                    _nodContext.getRelationDeclarations().get(getNodName(_input, basicStateExpr)),
                    getBasicRelationArgs(_input, basicStateExpr));
  }

  @Override
  public void visitCurrentIsOriginal(CurrentIsOriginalExpr currentIsOriginalExpr) {
    currentIsOriginalExpr.getExpr().accept(this);
  }

  public void visitDelegateBooleanExpr(DelegateBooleanExpr delegateBooleanExpr) {
    _boolExpr = delegateBooleanExpr.acceptBoolExprTransformer(_basicArguments, _input, _nodContext);
  }

  @Override
  public void visitEqExpr(EqExpr eqExpr) {
    _boolExpr =
        _nodContext
            .getContext()
            .mkEq(
                BitVecExprTransformer.toBitVecExpr(eqExpr.getLhs(), _nodContext),
                BitVecExprTransformer.toBitVecExpr(eqExpr.getRhs(), _nodContext));
  }

  @Override
  public void visitFalseExpr(FalseExpr falseExpr) {
    _boolExpr = _nodContext.getContext().mkFalse();
  }

  @Override
  public void visitHeaderSpaceMatchExpr(HeaderSpaceMatchExpr headerSpaceMatchExpr) {
    headerSpaceMatchExpr.getExpr().accept(this);
  }

  @Override
  public void visitIfExpr(IfExpr ifExpr) {
    _boolExpr =
        _nodContext
            .getContext()
            .mkImplies(
                BoolExprTransformer.toBoolExpr(ifExpr.getAntecedent(), _input, _nodContext),
                BoolExprTransformer.toBoolExpr(ifExpr.getConsequent(), _input, _nodContext));
  }

  @Override
  public void visitNotExpr(NotExpr notExpr) {
    _boolExpr = _nodContext.getContext().mkNot(toBoolExpr(notExpr.getArg(), _input, _nodContext));
  }

  @Override
  public void visitOrExpr(OrExpr orExpr) {
    _boolExpr =
        _nodContext
            .getContext()
            .mkOr(
                orExpr
                    .getDisjuncts()
                    .stream()
                    .map(disjunct -> toBoolExpr(disjunct, _input, _nodContext))
                    .toArray(BoolExpr[]::new));
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

  @Override
  public void visitTransformationStateExpr(TransformationStateExpr transformationStateExpr) {
    /* TODO: allow vectorized variables */
    _boolExpr =
        (BoolExpr)
            _nodContext
                .getContext()
                .mkApp(
                    _nodContext
                        .getRelationDeclarations()
                        .get(getNodName(_input, transformationStateExpr)),
                    getTransformationRelationArgs(_input, transformationStateExpr));
  }

  @Override
  public void visitTrueExpr(TrueExpr trueExpr) {
    _boolExpr = _nodContext.getContext().mkTrue();
  }
}

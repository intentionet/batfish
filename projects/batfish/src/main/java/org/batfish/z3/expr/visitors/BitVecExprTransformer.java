package org.batfish.z3.expr.visitors;

import com.microsoft.z3.BitVecExpr;
import org.batfish.common.BatfishException;
import org.batfish.z3.NodContext;
import org.batfish.z3.expr.DelegateIntExpr;
import org.batfish.z3.expr.ExtractExpr;
import org.batfish.z3.expr.IntExpr;
import org.batfish.z3.expr.LitIntExpr;
import org.batfish.z3.expr.VarIntExpr;

public class BitVecExprTransformer implements IntExprVisitor {

  public static BitVecExpr toBitVecExpr(IntExpr intExpr, NodContext nodContext) {
    BitVecExprTransformer bitVecExprTransformer = new BitVecExprTransformer(nodContext);
    intExpr.accept(bitVecExprTransformer);
    return bitVecExprTransformer._bitVecExpr;
  }

  private BitVecExpr _bitVecExpr;

  private final NodContext _nodContext;

  private BitVecExprTransformer(NodContext nodContext) {
    _nodContext = nodContext;
  }

  public void visitDelegateIntExpr(DelegateIntExpr delegateIntExpr) {
    _bitVecExpr = delegateIntExpr.acceptBitVecExprTransformer(_nodContext);
  }

  @Override
  public void visitExtractExpr(ExtractExpr extractExpr) {
    _bitVecExpr =
        _nodContext
            .getContext()
            .mkExtract(
                extractExpr.getHigh(),
                extractExpr.getLow(),
                toBitVecExpr(extractExpr.getVar(), _nodContext));
  }

  @Override
  public void visitLitIntExpr(LitIntExpr litIntExpr) {
    _bitVecExpr = _nodContext.getContext().mkBV(litIntExpr.getNum(), litIntExpr.getBits());
  }

  @Override
  public void visitVarIntExpr(VarIntExpr varIntExpr) {
    String headerField = varIntExpr.getHeaderField().getName();
    _bitVecExpr = _nodContext.getVariables().get(headerField);
    if (_bitVecExpr == null) {
      throw new BatfishException("nodContext missing mapping for variable: '" + headerField + "'");
    }
  }
}

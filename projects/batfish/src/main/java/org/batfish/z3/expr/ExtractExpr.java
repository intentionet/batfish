package org.batfish.z3.expr;

import java.util.Objects;
import org.batfish.z3.HeaderField;
import org.batfish.z3.expr.visitors.ExprVisitor;
import org.batfish.z3.expr.visitors.IntExprVisitor;

public class ExtractExpr extends IntExpr {

  public static IntExpr newExtractExpr(HeaderField var, int low, int high) {
    int varSize = var.getSize();
    return newExtractExpr(var, varSize, low, high);
  }

  private static IntExpr newExtractExpr(HeaderField var, int varSize, int low, int high) {
    if (low == 0 && high == varSize - 1) {
      return new VarIntExpr(var);
    } else {
      return new ExtractExpr(var, low, high);
    }
  }

  private final int _high;

  private final int _low;

  private final VarIntExpr _var;

  private ExtractExpr(HeaderField var, int low, int high) {
    _low = low;
    _high = high;
    _var = new VarIntExpr(var);
  }

  @Override
  public void accept(ExprVisitor visitor) {
    visitor.visitExtractExpr(this);
  }

  @Override
  public void accept(IntExprVisitor visitor) {
    visitor.visitExtractExpr(this);
  }

  @Override
  public boolean exprEquals(Expr e) {
    ExtractExpr other = (ExtractExpr) e;
    return Objects.equals(_high, other._high)
        && Objects.equals(_low, other._low)
        && Objects.equals(_var, other._var);
  }

  public int getHigh() {
    return _high;
  }

  public int getLow() {
    return _low;
  }

  public VarIntExpr getVar() {
    return _var;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_high, _low, _var);
  }

  @Override
  public int numBits() {
    return _high - _low + 1;
  }
}

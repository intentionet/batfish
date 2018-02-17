package org.batfish.z3.expr;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Objects;
import org.batfish.z3.TransformationHeaderField;
import org.batfish.z3.expr.visitors.BooleanExprVisitor;
import org.batfish.z3.expr.visitors.ExprVisitor;

public class CurrentIsOriginalExpr extends BooleanExpr {

  public static final CurrentIsOriginalExpr INSTANCE = new CurrentIsOriginalExpr();

  private final BooleanExpr _expr;

  private CurrentIsOriginalExpr() {
    _expr =
        new AndExpr(
            Arrays.stream(TransformationHeaderField.values())
                .map(
                    thf ->
                        new EqExpr(
                            new VarIntExpr(thf.getOriginal()), new VarIntExpr(thf.getCurrent())))
                .collect(ImmutableList.toImmutableList()));
  }

  @Override
  public void accept(BooleanExprVisitor visitor) {
    visitor.visitCurrentIsOriginal(this);
  }

  @Override
  public void accept(ExprVisitor visitor) {
    visitor.visitCurrentIsOriginal(this);
  }

  @Override
  public boolean exprEquals(Expr e) {
    return Objects.equals(_expr, ((CurrentIsOriginalExpr) e)._expr);
  }

  public BooleanExpr getExpr() {
    return _expr;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_expr);
  }
}

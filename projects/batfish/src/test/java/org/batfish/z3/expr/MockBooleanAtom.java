package org.batfish.z3.expr;

import java.util.Objects;
import org.batfish.z3.expr.visitors.ExprPrinter;
import org.batfish.z3.expr.visitors.ExprVisitor;
import org.batfish.z3.expr.visitors.GenericBooleanExprVisitor;
import org.batfish.z3.expr.visitors.IsComplexVisitor;
import org.batfish.z3.expr.visitors.Simplifier;

public class MockBooleanAtom extends BooleanExpr {

  private final String _name;

  public MockBooleanAtom(int i) {
    _name = String.format("BoolConst%d", i);
  }

  @Override
  public void accept(ExprVisitor visitor) {
    if (visitor instanceof ExprPrinter) {
      visitor.visitIdExpr(new IdExpr(_name));

    } else if (visitor instanceof IsComplexVisitor) {
      visitor.visitIdExpr(new IdExpr(_name));
    } else {
      throw new UnsupportedOperationException(
          String.format(
              "No implementation for %s: %s",
              ExprVisitor.class.getSimpleName(), visitor.getClass().getSimpleName()));
    }
  }

  @Override
  public <R> R accept(GenericBooleanExprVisitor<R> visitor) {
    if (visitor instanceof Simplifier) {
      return visitor.castToGenericBooleanExprVisitorReturnType(this);
    } else {
      throw new UnsupportedOperationException(
          String.format(
              "No implementation for %s: %s",
              GenericBooleanExprVisitor.class.getSimpleName(), visitor.getClass().getSimpleName()));
    }
  }

  @Override
  public boolean exprEquals(Expr e) {
    return Objects.equals(_name, ((MockBooleanAtom) e)._name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name);
  }
}

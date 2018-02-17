package org.batfish.z3.expr;

import com.google.common.base.Supplier;
import com.microsoft.z3.BoolExpr;
import java.util.Objects;
import org.batfish.z3.NodContext;
import org.batfish.z3.SynthesizerInput;

public class TestBooleanAtom extends DelegateBooleanExpr {

  private final String _name;

  public TestBooleanAtom(int i) {
    _name = String.format("BoolConst%d", i);
  }

  @Override
  public BoolExpr acceptBoolExprTransformer(
      Supplier<com.microsoft.z3.Expr[]> headerFieldArgs,
      SynthesizerInput input,
      NodContext nodContext) {
    return nodContext.getContext().mkBoolConst(_name);
  }

  @Override
  public boolean exprEquals(Expr e) {
    return Objects.equals(_name, ((TestBooleanAtom) e)._name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name);
  }
}

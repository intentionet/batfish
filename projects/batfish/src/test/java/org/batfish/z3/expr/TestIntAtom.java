package org.batfish.z3.expr;

import com.microsoft.z3.BitVecExpr;
import java.util.Objects;
import org.batfish.z3.NodContext;

public class TestIntAtom extends DelegateIntExpr {

  private final String _name;

  private final int _numBits;

  public TestIntAtom(int i, int numBits) {
    _name = String.format("BVConst%d", i);
    _numBits = numBits;
  }

  @Override
  public BitVecExpr acceptBitVecExprTransformer(NodContext nodContext) {
    return nodContext.getContext().mkBVConst(_name, _numBits);
  }

  @Override
  public boolean exprEquals(Expr e) {
    return Objects.equals(_name, ((TestIntAtom) e)._name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name);
  }

  @Override
  public int numBits() {
    return _numBits;
  }
}

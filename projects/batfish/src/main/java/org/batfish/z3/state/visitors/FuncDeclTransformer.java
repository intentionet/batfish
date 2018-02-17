package org.batfish.z3.state.visitors;

import com.google.common.base.Supplier;
import com.google.common.collect.Streams;
import com.microsoft.z3.BitVecSort;
import com.microsoft.z3.Context;
import com.microsoft.z3.FuncDecl;
import java.util.Arrays;
import org.batfish.z3.BasicHeaderField;
import org.batfish.z3.HeaderField;
import org.batfish.z3.TransformationHeaderField;
import org.batfish.z3.expr.BasicStateExpr;
import org.batfish.z3.expr.StateExpr.State;
import org.batfish.z3.expr.TransformationStateExpr;

public class FuncDeclTransformer implements GeneralStateVisitor {

  private final Supplier<BitVecSort[]> _basicParameterTypes;

  private final Context _ctx;

  private FuncDecl _funcDecl;

  private String _name;

  private final Supplier<BitVecSort[]> _transformationParameterTypes;

  public FuncDeclTransformer(Context ctx) {
    _ctx = ctx;
    _basicParameterTypes =
        () ->
            Arrays.stream(BasicHeaderField.values())
                .map(HeaderField::getSize)
                .map(_ctx::mkBitVecSort)
                .toArray(BitVecSort[]::new);
    _transformationParameterTypes =
        () ->
            Streams.concat(
                    Arrays.stream(_basicParameterTypes.get()),
                    Arrays.stream(TransformationHeaderField.values())
                        .map(HeaderField::getSize)
                        .map(_ctx::mkBitVecSort))
                .toArray(BitVecSort[]::new);
  }

  public FuncDecl toFuncDecl(String name, State state) {
    _name = name;
    state.accept(this);
    return _funcDecl;
  }

  @Override
  public void visitBasicState(BasicStateExpr.State basicState) {
    _funcDecl = _ctx.mkFuncDecl(_name, _basicParameterTypes.get(), _ctx.mkBoolSort());
  }

  @Override
  public void visitTransformationState(TransformationStateExpr.State transformationState) {
    _funcDecl = _ctx.mkFuncDecl(_name, _transformationParameterTypes.get(), _ctx.mkBoolSort());
  }
}

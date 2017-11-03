package org.batfish.symbolic;

import com.microsoft.z3.Expr;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.batfish.common.Pair;
import org.batfish.symbolic.collections.PList;

public class TransferResult<U, T> {

  private PList<Pair<String, Expr>> _changedVariables; // should be a map

  private U _returnValue;

  private T _fallthroughValue;

  private T _returnAssignedValue;

  public TransferResult() {
    this._changedVariables = PList.empty();
    this._returnValue = null;
    this._fallthroughValue = null;
    this._returnAssignedValue = null;
  }

  private TransferResult(TransferResult<U, T> other) {
    this._changedVariables = other._changedVariables;
    this._returnValue = other._returnValue;
    this._fallthroughValue = other._fallthroughValue;
    this._returnAssignedValue = other._returnAssignedValue;
  }

  @Nullable
  private Expr find(PList<Pair<String, Expr>> vals, String s) {
    for (Pair<String, Expr> pair : vals) {
      if (pair.getFirst().equals(s)) {
        return pair.getSecond();
      }
    }
    return null;
  }

  // TODO: this really needs to use persistent set data types
  public PList<Pair<String, Pair<Expr, Expr>>> mergeChangedVariables(TransferResult<U, T> other) {
    Set<String> seen = new HashSet<>();
    PList<Pair<String, Pair<Expr, Expr>>> vars = PList.empty();

    for (Pair<String, Expr> cv1 : this._changedVariables) {
      String s = cv1.getFirst();
      Expr x = cv1.getSecond();
      if (!seen.contains(s)) {
        seen.add(s);
        Expr e = find(other._changedVariables, s);
        Pair<Expr, Expr> pair = new Pair<>(x, e);
        vars = vars.plus(new Pair<>(s, pair));
      }
    }

    for (Pair<String, Expr> cv1 : other._changedVariables) {
      String s = cv1.getFirst();
      Expr x = cv1.getSecond();
      if (!seen.contains(s)) {
        seen.add(s);
        Expr e = find(this._changedVariables, s);
        assert (e != null); // invariant
        Pair<Expr, Expr> pair = new Pair<>(e, x); // preserve order
        vars = vars.plus(new Pair<>(s, pair));
      }
    }

    return vars;
  }

  public PList<Pair<String, Expr>> getChangedVariables() {
    return _changedVariables;
  }

  public U getReturnValue() {
    return _returnValue;
  }

  public T getFallthroughValue() {
    return _fallthroughValue;
  }

  public T getReturnAssignedValue() {
    return _returnAssignedValue;
  }

  public TransferResult<U, T> addChangedVariable(String s, Expr x) {
    TransferResult<U, T> ret = new TransferResult<>(this);
    ret._changedVariables = ret._changedVariables.plus(new Pair<>(s, x));
    return ret;
  }

  public TransferResult<U, T> addChangedVariables(TransferResult<U, T> other) {
    TransferResult<U, T> ret = new TransferResult<>(this);
    ret._changedVariables = ret._changedVariables.plusAll(other._changedVariables);
    return ret;
  }

  public boolean isChanged(String s) {
    for (Pair<String, Expr> pair : this._changedVariables) {
      if (pair.getFirst().equals(s)) {
        return true;
      }
    }
    return false;
  }

  public TransferResult<U, T> clearChanged() {
    TransferResult<U, T> ret = new TransferResult<>(this);
    ret._changedVariables = PList.empty();
    return ret;
  }

  public TransferResult<U, T> setReturnValue(U x) {
    TransferResult<U, T> ret = new TransferResult<>(this);
    ret._returnValue = x;
    return ret;
  }

  public TransferResult<U, T> setFallthroughValue(T x) {
    TransferResult<U, T> ret = new TransferResult<>(this);
    ret._fallthroughValue = x;
    return ret;
  }

  public TransferResult<U, T> setReturnAssignedValue(T x) {
    TransferResult<U, T> ret = new TransferResult<>(this);
    ret._returnAssignedValue = x;
    return ret;
  }
}

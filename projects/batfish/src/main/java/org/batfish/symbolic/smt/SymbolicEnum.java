package org.batfish.symbolic.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a symbolic variable for a small, finite number of choices. For optimization purposes,
 * we use a small domain bitvector to represent the possble choices.
 *
 * @param <T> The underlying domain of values
 * @author Ryan Beckett
 */
class SymbolicEnum<T> {

  EncoderSlice _enc;

  BitVecExpr _bitvec;

  int _numBits;

  private List<T> _values;

  private Map<T, BitVecExpr> _valueMap;

  SymbolicEnum(EncoderSlice slice, List<T> values, String name) {
    _enc = slice;
    _numBits = numBits(values);
    int size = values.size();
    initValues(values);

    if (_numBits == 0) {
      _bitvec = null;
    } else {

      _bitvec = _enc.getCtx().mkBVConst(name, _numBits);

      if (name != null) {
        slice.getAllVariables().put(_bitvec.toString(), _bitvec);
      }

      if (!isPowerOfTwo(size)) {
        BitVecExpr maxValue = slice.getCtx().mkBV(size - 1, _numBits);
        BoolExpr constraint = slice.getCtx().mkBVULE(_bitvec, maxValue);
        slice.add(constraint);
      }
    }
  }

  SymbolicEnum(SymbolicEnum<T> other) {
    _enc = other._enc;
    _bitvec = other._bitvec;
    _numBits = other._numBits;
    _values = other._values;
    _valueMap = other._valueMap;
  }

  SymbolicEnum(EncoderSlice slice, List<T> values, T value) {
    _enc = slice;
    int idx = values.indexOf(value);
    _numBits = numBits(values);

    if (_numBits == 0) {
      _bitvec = null;
    } else {
      _bitvec = _enc.getCtx().mkBV(idx, _numBits);
    }

    initValues(values);
  }

  private int numBits(List<T> values) {
    int size = values.size();
    double log = Math.log((double) size);
    double base = Math.log((double) 2);
    if (size == 0) {
      return 0;
    } else {
      return (int) Math.ceil(log / base);
    }
  }

  private void initValues(List<T> values) {
    int i = 0;
    _values = new ArrayList<>();
    _valueMap = new HashMap<>();
    for (T value : values) {
      _values.add(value);
      if (_numBits > 0) {
        _valueMap.put(value, _enc.getCtx().mkBV(i, _numBits));
      }
      i++;
    }
  }

  private boolean isPowerOfTwo(int x) {
    return (x & -x) == x;
  }

  void setBitVec(BitVecExpr bv) {
    this._bitvec = bv;
  }

  BoolExpr mkEq(SymbolicEnum<T> other) {
    if (_bitvec == null || other._bitvec == null) {
      return _enc.mkTrue();
    }
    return _enc.mkEq(_bitvec, other._bitvec);
  }

  BoolExpr checkIfValue(T p) {
    if (_bitvec == null) {
      T q = _values.get(0);
      return _enc.mkBool(p == q);
    }

    BitVecExpr bv = _valueMap.get(p);
    if (bv == null) {
      return _enc.mkFalse();
    }

    return _enc.mkEq(_bitvec, bv);
  }

  BoolExpr isDefaultValue() {
    if (_bitvec == null) {
      return _enc.mkTrue();
    }
    return _enc.mkEq(_bitvec, _enc.getCtx().mkBV(0, _numBits));
  }

  T value(int i) {
    return _values.get(i);
  }

  BitVecExpr getBitVec() {
    return _bitvec;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SymbolicEnum<?>)) {
      return false;
    }
    SymbolicEnum<?> other = (SymbolicEnum<?>) o;
    return _numBits == other._numBits && Objects.equals(_bitvec, other._bitvec);
  }

  @Override
  public int hashCode() {
    int result = _bitvec != null ? _bitvec.hashCode() : 0;
    result = 31 * result + _numBits;
    return result;
  }
}

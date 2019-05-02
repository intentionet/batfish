package org.batfish.common.bdd;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDException;
import net.sf.javabdd.BDDFactory;

public class BDDInteger {

  private BDDFactory _factory;

  private BDD[] _bitvec;

  /*
   * Create an integer, but don't initialize its bit values
   */
  private BDDInteger(BDDFactory factory, int length) {
    _factory = factory;
    _bitvec = new BDD[length];
  }

  public BDDInteger(BDDInteger other) {
    _factory = other._factory;
    _bitvec = new BDD[other._bitvec.length];
    for (int i = 0; i < _bitvec.length; i++) {
      _bitvec[i] = other._bitvec[i].id();
    }
  }

  /*
   * Create an integer, and initialize its values as "don't care"
   * This requires knowing the start index variables the bitvector
   * will use.
   */
  public static BDDInteger makeFromIndex(
      BDDFactory factory, int length, int start, boolean reverse) {
    assert factory.varNum() >= start + length;

    BDDInteger bdd = new BDDInteger(factory, length);
    for (int i = 0; i < length; i++) {
      int idx;
      if (reverse) {
        idx = start + length - i - 1;
      } else {
        idx = start + i;
      }
      bdd._bitvec[i] = bdd._factory.ithVar(idx);
    }
    return bdd;
  }

  /** Find a representative value of the represented integer that satisfies a given constraint. */
  public Optional<Long> getValueSatisfying(BDD bdd) {
    BDD satAssignment = bdd.fullSatOne();
    return satAssignment.isZero()
        ? Optional.empty()
        : Optional.of(satAssignmentToLong(satAssignment));
  }

  /** @param satAssignment a satisfying assignment (i.e. produced by fullSat, allSat, etc) */
  public Long satAssignmentToLong(BDD satAssignment) {
    if (_bitvec.length > Long.SIZE) {
      throw new IllegalArgumentException(
          "Can't get a representative of a BDDInteger with more than Long.SIZE bits");
    }

    long value = 0;
    for (int i = 0; i < _bitvec.length; i++) {
      BDD bitBDD = _bitvec[_bitvec.length - i - 1];
      if (!satAssignment.and(bitBDD).isZero()) {
        value |= ((long) 1) << i;
      }
    }
    return value;
  }

  /**
   * Return a list of values satisfying the input {@link BDD}, up to some maximum number.
   *
   * @param bdd A constraint on this.
   * @param max The maximum number of values desired.
   * @return The satisfying values.
   */
  public List<Long> getValuesSatisfying(BDD bdd, int max) {
    ImmutableList.Builder<Long> values = new ImmutableList.Builder<>();

    checkArgument(max > 0, "max must be > 0");

    int num = 0;
    BDD pred = bdd;
    while (num < max) {
      BDD satAssignment = pred.fullSatOne();
      if (satAssignment.isZero()) {
        break;
      }

      Long val = satAssignmentToLong(satAssignment);
      values.add(val);
      pred = pred.diff(value(val));
      num++;
    }
    return values.build();
  }

  /*
   * Create an integer and initialize it to a concrete value
   */
  public static BDDInteger makeFromValue(BDDFactory factory, int length, long value) {
    BDDInteger bdd = new BDDInteger(factory, length);
    bdd.setValue(value);
    return bdd;
  }

  /*
   * Map an if-then-else over each bit in the bitvector
   */
  public BDDInteger ite(BDD b, BDDInteger other) {
    BDDInteger val = new BDDInteger(this);
    for (int i = 0; i < _bitvec.length; i++) {
      val._bitvec[i] = b.ite(_bitvec[i], other._bitvec[i]);
    }
    return val;
  }

  /*
   * Create a BDD representing the exact value
   */
  public BDD value(long val) {
    checkArgument(val >= 0, "value is negative");
    checkArgument(val < (1L << _bitvec.length), "value %s is out of range", val);
    long currentVal = val;
    BDD bdd = _factory.one();
    for (int i = this._bitvec.length - 1; i >= 0; i--) {
      BDD b = this._bitvec[i];
      if ((currentVal & 1) != 0) {
        bdd = bdd.and(b);
      } else {
        bdd = bdd.diff(b);
      }
      currentVal >>= 1;
    }
    return bdd;
  }

  // Helper function to compute leq on the last N bits of the input value.
  private BDD leqN(long val, int n) {
    assert n <= _bitvec.length;
    long currentVal = val;
    BDD acc = _factory.one(); // whether the suffix of BDD is leq suffix of val.
    for (int i = 0; i < n; ++i) {
      BDD bit = _bitvec[_bitvec.length - i - 1];
      if ((currentVal & 1) != 0) {
        // since this bit of val is 1: 0 implies lt OR 1 and suffix leq. ('1 and' is redundant).
        acc = bit.imp(acc); // "not i or acc" rewritten "i implies acc".
      } else {
        // since this bit of val is 0: must be 0 and have leq suffix.
        acc = bit.less(acc); // "not i and acc" rewritten "i less acc"
      }
      currentVal >>= 1;
    }
    return acc;
  }

  /*
   * Less than or equal to on integers
   */
  public BDD leq(long val) {
    checkArgument(val >= 0, "value is negative");
    checkArgument(val < (1L << _bitvec.length), "value %s is out of range", val);
    return leqN(val, _bitvec.length);
  }

  // Helper function to compute geq on the last N bits of the input value.
  private BDD geqN(long val, int n) {
    assert n <= _bitvec.length;
    long currentVal = val;
    BDD acc = _factory.one(); // whether the suffix of BDD is geq suffix of val.
    for (int i = 0; i < n; ++i) {
      BDD bit = _bitvec[_bitvec.length - i - 1];
      if ((currentVal & 1) != 0) {
        // since this bit of val is 1: must be 1 and have geq suffix.
        acc = bit.and(acc);
      } else {
        // since this bit of val is 0: 1 implies gt OR 0 and suffix geq. ('0 and' is redundant.)
        acc = bit.or(acc);
      }
      currentVal >>= 1;
    }
    return acc;
  }

  /*
   * Greater than or equal to on integers
   */
  public BDD geq(long val) {
    checkArgument(val >= 0, "value is negative");
    checkArgument(val < (1L << _bitvec.length), "value %s is out of range", val);
    return geqN(val, _bitvec.length);
  }

  /*
   * Integers in the given range, inclusive, where {@code a} is less than or equal to {@code b}.
   */
  // This is basically this.geq(a).and(this.leq(b)). Two differences:
  //   1. Short-circuit a == b
  //   2. Save work in the case where a and b have a common prefix.
  public BDD range(long a, long b) {
    checkArgument(a <= b, "range is not ordered correctly");
    checkArgument(a >= 0, "value is negative");
    checkArgument(b < (1L << _bitvec.length), "value %s is out of range", b);

    if (a == b) {
      return value(a);
    }

    long bitOfFirstDifference = Long.highestOneBit(a ^ b);
    int sizeOfDifferentSuffix = Long.numberOfTrailingZeros(bitOfFirstDifference) + 1;
    BDD geqA = geqN(a, sizeOfDifferentSuffix);
    BDD leqB = leqN(b, sizeOfDifferentSuffix);

    BDD between = geqA.and(leqB);
    long currentVal = a >> sizeOfDifferentSuffix;
    for (int i = sizeOfDifferentSuffix; i < _bitvec.length; ++i) {
      BDD bit = _bitvec[_bitvec.length - i - 1];
      if ((currentVal & 1) != 0) {
        between = bit.and(between);
      } else {
        between = bit.less(between); // "not i and x" rewritten "i less x"
      }
      currentVal >>= 1;
    }
    return between;
  }

  /*
   * Set this BDD to have an exact value
   */
  public void setValue(long val) {
    long currentVal = val;
    for (int i = this._bitvec.length - 1; i >= 0; i--) {
      if ((currentVal & 1) != 0) {
        this._bitvec[i] = _factory.one();
      } else {
        this._bitvec[i] = _factory.zero();
      }
      currentVal >>= 1;
    }
  }

  /*
   * Set this BDD to be equal to another BDD
   */
  public void setValue(BDDInteger other) {
    for (int i = 0; i < this._bitvec.length; ++i) {
      this._bitvec[i] = other._bitvec[i].id();
    }
  }

  /*
   * Add two BDDs bitwise to create a new BDD
   */
  public BDDInteger add(BDDInteger other) {
    BDD[] as = this._bitvec;
    BDD[] bs = other._bitvec;

    checkArgument(as.length > 0, "Cannot add BDDIntegers of length 0");
    checkArgument(as.length == bs.length, "Cannot add BDDIntegers of different length");

    BDD carry = _factory.zero();
    BDDInteger sum = new BDDInteger(_factory, as.length);
    BDD[] cs = sum._bitvec;
    for (int i = cs.length - 1; i > 0; --i) {
      cs[i] = as[i].xor(bs[i]).xor(carry);
      carry = as[i].and(bs[i]).or(carry.and(as[i].or(bs[i])));
    }
    cs[0] = as[0].xor(bs[0]).xor(carry);
    return sum;
  }

  /*
   * Subtract one BDD from another bitwise to create a new BDD
   */
  public BDDInteger sub(BDDInteger var1) {
    if (this._bitvec.length != var1._bitvec.length) {
      throw new BDDException();
    } else {
      BDD var3 = _factory.zero();
      BDDInteger var4 = new BDDInteger(_factory, this._bitvec.length);
      for (int var5 = var4._bitvec.length - 1; var5 >= 0; --var5) {
        var4._bitvec[var5] = this._bitvec[var5].xor(var1._bitvec[var5]);
        var4._bitvec[var5] = var4._bitvec[var5].xor(var3.id());
        BDD var6 = var1._bitvec[var5].or(var3);
        BDD var7 = this._bitvec[var5].apply(var6, BDDFactory.less);
        var6.free();
        var6 = this._bitvec[var5].and(var1._bitvec[var5]);
        var6 = var6.and(var3);
        var6 = var6.or(var7);
        var3 = var6;
      }
      var3.free();
      return var4;
    }
  }

  public BDD[] getBitvec() {
    return _bitvec;
  }

  public BDDFactory getFactory() {
    return _factory;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BDDInteger)) {
      return false;
    }
    BDDInteger other = (BDDInteger) o;
    return Arrays.equals(_bitvec, other._bitvec);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(_bitvec);
  }
}

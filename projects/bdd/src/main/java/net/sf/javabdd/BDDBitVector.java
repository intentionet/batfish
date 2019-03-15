// BDDBitVector.java, created Jul 14, 2003 9:50:57 PM by jwhaley
// Copyright (C) 2003 John Whaley
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd;

import java.math.BigInteger;

/**
 * Bit vector implementation for BDDs.
 *
 * @author John Whaley
 * @version $Id: BDDBitVector.java,v 1.2 2004/10/18 09:35:20 joewhaley Exp $
 */
public abstract class BDDBitVector {

  protected BDD[] bitvec;

  protected BDDBitVector(int bitnum) {
    bitvec = new BDD[bitnum];
  }

  protected void initialize(boolean isTrue) {
    BDDFactory bdd = getFactory();
    for (int n = 0; n < bitvec.length; n++) {
      if (isTrue) {
        bitvec[n] = bdd.one();
      } else {
        bitvec[n] = bdd.zero();
      }
    }
  }

  protected void initialize(int val) {
    BDDFactory bdd = getFactory();
    for (int n = 0; n < bitvec.length; n++) {
      if ((val & 0x1) != 0) {
        bitvec[n] = bdd.one();
      } else {
        bitvec[n] = bdd.zero();
      }
      val >>= 1;
    }
  }

  protected void initialize(long val) {
    BDDFactory bdd = getFactory();
    for (int n = 0; n < bitvec.length; n++) {
      if ((val & 0x1) != 0) {
        bitvec[n] = bdd.one();
      } else {
        bitvec[n] = bdd.zero();
      }
      val >>= 1;
    }
  }

  protected void initialize(BigInteger val) {
    BDDFactory bdd = getFactory();
    for (int n = 0; n < bitvec.length; n++) {
      if (val.testBit(0)) {
        bitvec[n] = bdd.one();
      } else {
        bitvec[n] = bdd.zero();
      }
      val = val.shiftRight(1);
    }
  }

  protected void initialize(int offset, int step) {
    BDDFactory bdd = getFactory();
    for (int n = 0; n < bitvec.length; n++) {
      bitvec[n] = bdd.ithVar(offset + n * step);
    }
  }

  protected void initialize(BDDDomain d) {
    initialize(d.vars());
  }

  protected void initialize(int[] var) {
    BDDFactory bdd = getFactory();
    for (int n = 0; n < bitvec.length; n++) {
      bitvec[n] = bdd.ithVar(var[n]);
    }
  }

  public abstract BDDFactory getFactory();

  public BDDBitVector copy() {
    BDDFactory bdd = getFactory();
    BDDBitVector dst = bdd.createBitVector(bitvec.length);

    for (int n = 0; n < bitvec.length; n++) {
      dst.bitvec[n] = bitvec[n].id();
    }

    return dst;
  }

  public BDDBitVector coerce(int bitnum) {
    BDDFactory bdd = getFactory();
    BDDBitVector dst = bdd.createBitVector(bitnum);
    int minnum = Math.min(bitnum, bitvec.length);
    int n;
    for (n = 0; n < minnum; n++) {
      dst.bitvec[n] = bitvec[n].id();
    }
    for (; n < minnum; n++) {
      dst.bitvec[n] = bdd.zero();
    }
    return dst;
  }

  public boolean isConst() {
    for (int n = 0; n < bitvec.length; n++) {
      BDD b = bitvec[n];
      if (!b.isOne() && !b.isZero()) {
        return false;
      }
    }
    return true;
  }

  public int val() {
    int n, val = 0;

    for (n = bitvec.length - 1; n >= 0; n--) {
      if (bitvec[n].isOne()) {
        val = (val << 1) | 1;
      } else if (bitvec[n].isZero()) {
        val = val << 1;
      } else {
        return 0;
      }
    }

    return val;
  }

  public void free() {
    for (int n = 0; n < bitvec.length; n++) {
      bitvec[n].free();
    }
    bitvec = null;
  }

  public BDDBitVector map2(BDDBitVector that, BDDFactory.BDDOp op) {
    if (bitvec.length != that.bitvec.length) {
      throw new BDDException();
    }

    BDDFactory bdd = getFactory();
    BDDBitVector res = bdd.createBitVector(bitvec.length);
    for (int n = 0; n < bitvec.length; n++) {
      res.bitvec[n] = bitvec[n].apply(that.bitvec[n], op);
    }

    return res;
  }

  public BDDBitVector add(BDDBitVector that) {

    if (bitvec.length != that.bitvec.length) {
      throw new BDDException();
    }

    BDDFactory bdd = getFactory();

    BDD c = bdd.zero();
    BDDBitVector res = bdd.createBitVector(bitvec.length);

    for (int n = 0; n < res.bitvec.length; n++) {
      /* bitvec[n] = l[n] ^ r[n] ^ c; */
      res.bitvec[n] = bitvec[n].xor(that.bitvec[n]);
      res.bitvec[n].xorWith(c.id());

      /* c = (l[n] & r[n]) | (c & (l[n] | r[n])); */
      BDD tmp1 = bitvec[n].or(that.bitvec[n]);
      tmp1.andWith(c);
      BDD tmp2 = bitvec[n].and(that.bitvec[n]);
      tmp2.orWith(tmp1);
      c = tmp2;
    }
    c.free();

    return res;
  }

  public BDDBitVector sub(BDDBitVector that) {

    if (bitvec.length != that.bitvec.length) {
      throw new BDDException();
    }

    BDDFactory bdd = getFactory();

    BDD c = bdd.zero();
    BDDBitVector res = bdd.createBitVector(bitvec.length);

    for (int n = 0; n < res.bitvec.length; n++) {
      /* bitvec[n] = l[n] ^ r[n] ^ c; */
      res.bitvec[n] = bitvec[n].xor(that.bitvec[n]);
      res.bitvec[n].xorWith(c.id());

      /* c = (l[n] & r[n] & c) | (!l[n] & (r[n] | c)); */
      BDD tmp1 = that.bitvec[n].or(c);
      BDD tmp2 = this.bitvec[n].apply(tmp1, BDDFactory.less);
      tmp1.free();
      tmp1 = this.bitvec[n].and(that.bitvec[n]);
      tmp1.andWith(c);
      tmp1.orWith(tmp2);

      c = tmp1;
    }
    c.free();

    return res;
  }

  BDD lte(BDDBitVector r) {
    if (this.bitvec.length != r.bitvec.length) {
      throw new BDDException();
    }

    BDDFactory bdd = getFactory();
    BDD p = bdd.one();
    for (int n = 0; n < bitvec.length; n++) {
      /* p = (!l[n] & r[n]) |
       *     bdd_apply(l[n], r[n], bddop_biimp) & p; */

      BDD tmp1 = bitvec[n].apply(r.bitvec[n], BDDFactory.less);
      BDD tmp2 = bitvec[n].apply(r.bitvec[n], BDDFactory.biimp);
      tmp2.andWith(p);
      tmp1.orWith(tmp2);
      p = tmp1;
    }
    return p;
  }

  static void div_rec(BDDBitVector divisor, BDDBitVector remainder, BDDBitVector result, int step) {
    BDD isSmaller = divisor.lte(remainder);
    BDDBitVector newResult = result.shl(1, isSmaller);
    BDDFactory bdd = divisor.getFactory();
    BDDBitVector zero = bdd.buildVector(divisor.bitvec.length, false);
    BDDBitVector sub = bdd.buildVector(divisor.bitvec.length, false);

    for (int n = 0; n < divisor.bitvec.length; n++) {
      sub.bitvec[n] = isSmaller.ite(divisor.bitvec[n], zero.bitvec[n]);
    }

    BDDBitVector tmp = remainder.sub(sub);
    BDDBitVector newRemainder = tmp.shl(1, result.bitvec[divisor.bitvec.length - 1]);

    if (step > 1) {
      div_rec(divisor, newRemainder, newResult, step - 1);
    }

    tmp.free();
    sub.free();
    zero.free();
    isSmaller.free();

    result.replaceWith(newResult);
    remainder.replaceWith(newRemainder);
  }

  public void replaceWith(BDDBitVector that) {
    if (bitvec.length != that.bitvec.length) {
      throw new BDDException();
    }
    free();
    this.bitvec = that.bitvec;
    that.bitvec = null;
  }

  public BDDBitVector shl(int pos, BDD c) {
    int minnum = Math.min(bitvec.length, pos);
    if (minnum < 0) {
      throw new BDDException();
    }

    BDDFactory bdd = getFactory();
    BDDBitVector res = bdd.createBitVector(bitvec.length);

    int n;
    for (n = 0; n < minnum; n++) {
      res.bitvec[n] = c.id();
    }

    for (n = minnum; n < bitvec.length; n++) {
      res.bitvec[n] = bitvec[n - pos].id();
    }

    return res;
  }

  BDDBitVector shr(int pos, BDD c) {
    int maxnum = Math.max(0, bitvec.length - pos);
    if (maxnum < 0) {
      throw new BDDException();
    }

    BDDFactory bdd = getFactory();
    BDDBitVector res = bdd.createBitVector(bitvec.length);

    int n;
    for (n = maxnum; n < bitvec.length; n++) {
      res.bitvec[n] = c.id();
    }

    for (n = 0; n < maxnum; n++) {
      res.bitvec[n] = bitvec[n + pos].id();
    }

    return res;
  }

  public BDDBitVector divmod(long c, boolean which) {
    if (c <= 0L) {
      throw new BDDException();
    }
    BDDFactory bdd = getFactory();
    BDDBitVector divisor = bdd.constantVector(bitvec.length, c);
    BDDBitVector tmp = bdd.buildVector(bitvec.length, false);
    BDDBitVector tmpremainder = tmp.shl(1, bitvec[bitvec.length - 1]);
    BDDBitVector result = this.shl(1, bdd.zero());

    BDDBitVector remainder;

    div_rec(divisor, tmpremainder, result, divisor.bitvec.length);
    remainder = tmpremainder.shr(1, bdd.zero());

    tmp.free();
    tmpremainder.free();
    divisor.free();

    if (which) {
      remainder.free();
      return result;
    } else {
      result.free();
      return remainder;
    }
  }

  public int size() {
    return bitvec.length;
  }

  public BDD getBit(int n) {
    return bitvec[n];
  }
}

/*
 * Note: We obtained permission from the author of Javabdd, John Whaley, to use
 * the library with Batfish under the MIT license. The email exchange is included
 * in LICENSE.email file.
 *
 * MIT License
 *
 * Copyright (c) 2013-2017 John Whaley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package net.sf.javabdd;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Binary Decision Diagrams (BDDs) are used for efficient computation of many common problems. This
 * is done by giving a compact representation and a set of efficient operations on boolean functions
 * f: {0,1}^n --> {0,1}.
 *
 * <p>Use an implementation of BDDFactory to create BDD objects.
 *
 * <p>Some methods, namely <tt>exist()</tt>, <tt>forall()</tt>, <tt>unique()</tt>,
 * <tt>relprod()</tt>, <tt>applyAll()</tt>, <tt>applyEx()</tt>, <tt>applyUni()</tt>, and
 * <tt>satCount()</tt> take a 'set of variables' argument that is also of type BDD. Those BDDs must
 * be a boolean function that represents the all-true minterm of the BDD variables of interest. They
 * only serve to identify the set of variables of interest, however. For example, for a given
 * BDDDomain, a BDD var set representing all BDD variables of that domain can be obtained by calling
 * <tt>BDDDomain.set()</tt>.
 *
 * @see net.sf.javabdd.BDDFactory
 * @see net.sf.javabdd.BDDDomain#set()
 * @author John Whaley
 * @version $Id: BDD.java,v 1.13 2005/06/03 20:20:16 joewhaley Exp $
 */
public abstract class BDD {

  /**
   * Returns the factory that created this BDD.
   *
   * @return factory that created this BDD
   */
  public abstract BDDFactory getFactory();

  /**
   * Returns true if this BDD is the zero (false) BDD.
   *
   * @return true if this BDD is the zero (false) BDD
   */
  public abstract boolean isZero();

  /**
   * Returns true if this BDD is the one (true) BDD.
   *
   * @return true if this BDD is the one (true) BDD
   */
  public abstract boolean isOne();

  /**
   * Gets the variable labeling the BDD.
   *
   * <p>Compare to bdd_var.
   *
   * @return the index of the variable labeling the BDD
   */
  public abstract int var();

  /**
   * Gets the level of this BDD.
   *
   * <p>Compare to LEVEL() macro.
   *
   * @return the level of this BDD
   */
  public int level() {
    if (isZero() || isOne()) {
      return getFactory().varNum();
    }
    return getFactory().var2Level(var());
  }

  /**
   * Gets the true branch of this BDD.
   *
   * <p>Compare to bdd_high.
   *
   * @return true branch of this BDD
   */
  public abstract BDD high();

  /**
   * Gets the false branch of this BDD.
   *
   * <p>Compare to bdd_low.
   *
   * @return false branch of this BDD
   */
  public abstract BDD low();

  /**
   * Identity function. Returns a copy of this BDD. Use as the argument to the "xxxWith" style
   * operators when you do not want to have the argument consumed.
   *
   * <p>Compare to bdd_addref.
   *
   * @return copy of this BDD
   */
  public abstract BDD id();

  /**
   * Negates this BDD by exchanging all references to the zero-terminal with references to the
   * one-terminal and vice-versa.
   *
   * <p>Compare to bdd_not.
   *
   * @return the negated BDD
   */
  public abstract BDD not();

  /**
   * Returns the logical 'and' of two BDDs. This is a shortcut for calling "apply" with the "and"
   * operator.
   *
   * <p>Compare to bdd_and.
   *
   * @param that BDD to 'and' with
   * @return the logical 'and' of two BDDs
   */
  public BDD and(BDD that) {
    return this.apply(that, BDDFactory.and);
  }

  /**
   * Makes this BDD be the logical 'and' of two BDDs. The "that" BDD is consumed, and can no longer
   * be used. This is a shortcut for calling "applyWith" with the "and" operator.
   *
   * <p>Compare to bdd_and and bdd_delref.
   *
   * @param that the BDD to 'and' with
   */
  public BDD andWith(BDD that) {
    return this.applyWith(that, BDDFactory.and);
  }

  /**
   * Returns the logical 'or' of two BDDs. This is a shortcut for calling "apply" with the "or"
   * operator.
   *
   * <p>Compare to bdd_or.
   *
   * @param that the BDD to 'or' with
   * @return the logical 'or' of two BDDs
   */
  public BDD or(BDD that) {
    return this.apply(that, BDDFactory.or);
  }

  /**
   * Makes this BDD be the logical 'or' of two BDDs. The "that" BDD is consumed, and can no longer
   * be used. This is a shortcut for calling "applyWith" with the "or" operator.
   *
   * <p>Compare to bdd_or and bdd_delref.
   *
   * @param that the BDD to 'or' with
   */
  public BDD orWith(BDD that) {
    return this.applyWith(that, BDDFactory.or);
  }

  /**
   * Returns the logical 'xor' of two BDDs. This is a shortcut for calling "apply" with the "xor"
   * operator.
   *
   * <p>Compare to bdd_xor.
   *
   * @param that the BDD to 'xor' with
   * @return the logical 'xor' of two BDDs
   */
  public BDD xor(BDD that) {
    return this.apply(that, BDDFactory.xor);
  }

  /**
   * Makes this BDD be the logical 'xor' of two BDDs. The "that" BDD is consumed, and can no longer
   * be used. This is a shortcut for calling "applyWith" with the "xor" operator.
   *
   * <p>Compare to bdd_xor and bdd_delref.
   *
   * @param that the BDD to 'xor' with
   */
  public BDD xorWith(BDD that) {
    return this.applyWith(that, BDDFactory.xor);
  }

  /**
   * Returns the logical 'implication' of two BDDs. This is a shortcut for calling "apply" with the
   * "imp" operator.
   *
   * <p>Compare to bdd_imp.
   *
   * @param that the BDD to 'implication' with
   * @return the logical 'implication' of two BDDs
   */
  public BDD imp(BDD that) {
    return this.apply(that, BDDFactory.imp);
  }

  /**
   * Makes this BDD be the logical 'implication' of two BDDs. The "that" BDD is consumed, and can no
   * longer be used. This is a shortcut for calling "applyWith" with the "imp" operator.
   *
   * <p>Compare to bdd_imp and bdd_delref.
   *
   * @param that the BDD to 'implication' with
   */
  public BDD impWith(BDD that) {
    return this.applyWith(that, BDDFactory.imp);
  }

  /**
   * Returns the logical 'bi-implication' of two BDDs. This is a shortcut for calling "apply" with
   * the "biimp" operator.
   *
   * <p>Compare to bdd_biimp.
   *
   * @param that the BDD to 'bi-implication' with
   * @return the logical 'bi-implication' of two BDDs
   */
  public BDD biimp(BDD that) {
    return this.apply(that, BDDFactory.biimp);
  }

  /**
   * Makes this BDD be the logical 'bi-implication' of two BDDs. The "that" BDD is consumed, and can
   * no longer be used. This is a shortcut for calling "applyWith" with the "biimp" operator.
   *
   * <p>Compare to bdd_biimp and bdd_delref.
   *
   * @param that the BDD to 'bi-implication' with
   */
  public BDD biimpWith(BDD that) {
    return this.applyWith(that, BDDFactory.biimp);
  }

  /**
   * Returns the logical 'difference' of two BDDs, equivalent to {@code this.and(that.not())}. This
   * is a shortcut for calling "apply" with the "diff" operator.
   *
   * @param that the BDD to 'subtract' from this BDD
   * @return the logical 'difference' of two BDDs
   */
  public BDD diff(BDD that) {
    return this.apply(that, BDDFactory.diff);
  }

  /**
   * Makes this BDD be the logical 'difference' of two BDDs, equivalent to {@code
   * this.and(that.not())}. The "that" BDD is consumed, and can no longer be used. This is a
   * shortcut for calling "applyWith" with the "less" operator.
   *
   * @param that the BDD to 'subtract' from this BDD
   * @return the logical 'difference' of two BDDs
   */
  public BDD diffWith(BDD that) {
    return this.applyWith(that, BDDFactory.diff);
  }

  /**
   * if-then-else operator.
   *
   * <p>Compare to bdd_ite.
   *
   * @param thenBDD the 'then' BDD
   * @param elseBDD the 'else' BDD
   * @return the result of the if-then-else operator on the three BDDs
   */
  public abstract BDD ite(BDD thenBDD, BDD elseBDD);

  /**
   * Returns the logical 'less-than' of two BDDs, equivalent to {@code this.not().and(that)}. This
   * is a shortcut for calling "apply" with the "less" operator.
   *
   * @param that the BDD from which this BDD will be subtracted
   * @return the logical 'less-than' of two BDDs
   */
  public BDD less(BDD that) {
    return this.apply(that, BDDFactory.less);
  }

  /**
   * Makes this BDD be the logical 'less-than' of two BDDs, equivalent to {@code
   * this.not().and(that)}. The "that" BDD is consumed, and can no longer be used. This is a
   * shortcut for calling "applyWith" with the "less" operator.
   *
   * @param that the BDD from which this BDD will be subtracted
   * @return the logical 'less-than' of two BDDs
   */
  public BDD lessWith(BDD that) {
    return this.applyWith(that, BDDFactory.less);
  }

  /**
   * Relational product. Calculates the relational product of the two BDDs as this AND that with the
   * variables in var quantified out afterwards. Identical to applyEx(that, and, var).
   *
   * <p>Compare to bdd_relprod.
   *
   * @param that the BDD to 'and' with
   * @param var the BDD to existentially quantify with
   * @return the result of the relational product
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public abstract BDD relprod(BDD that, BDD var);

  /**
   * Functional composition. Substitutes the variable var with the BDD that in this BDD: result =
   * f[g/var].
   *
   * <p>Compare to bdd_compose.
   *
   * @param g the function to use to replace
   * @param var the variable number to replace
   * @return the result of the functional composition
   */
  public abstract BDD compose(BDD g, int var);

  /**
   * Simultaneous functional composition. Uses the pairs of variables and BDDs in pair to make the
   * simultaneous substitution: f [g1/V1, ... gn/Vn]. In this way one or more BDDs may be
   * substituted in one step. The BDDs in pair may depend on the variables they are substituting.
   * BDD.compose() may be used instead of BDD.replace() but is not as efficient when gi is a single
   * variable, the same applies to BDD.restrict(). Note that simultaneous substitution is not
   * necessarily the same as repeated substitution.
   *
   * <p>Compare to bdd_veccompose.
   *
   * @param pair the pairing of variables to functions
   * @return BDD the result of the simultaneous functional composition
   */
  public abstract BDD veccompose(BDDPairing pair);

  /**
   * Generalized cofactor. Computes the generalized cofactor of this BDD with respect to the given
   * BDD.
   *
   * <p>Compare to bdd_constrain.
   *
   * @param that the BDD with which to compute the generalized cofactor
   * @return the result of the generalized cofactor
   */
  public abstract BDD constrain(BDD that);

  /**
   * Existential quantification of variables. Removes all occurrences of this BDD in variables in
   * the set var by existential quantification.
   *
   * <p>Compare to bdd_exist.
   *
   * @param var BDD containing the variables to be existentially quantified
   * @return the result of the existential quantification
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public abstract BDD exist(BDD var);

  /**
   * Universal quantification of variables. Removes all occurrences of this BDD in variables in the
   * set var by universal quantification.
   *
   * <p>Compare to bdd_forall.
   *
   * @param var BDD containing the variables to be universally quantified
   * @return the result of the universal quantification
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public abstract BDD forAll(BDD var);

  /**
   * Unique quantification of variables. This type of quantification uses a XOR operator instead of
   * an OR operator as in the existential quantification.
   *
   * <p>Compare to bdd_unique.
   *
   * @param var BDD containing the variables to be uniquely quantified
   * @return the result of the unique quantification
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public abstract BDD unique(BDD var);

  /**
   * Restrict a set of variables to constant values. Restricts the variables in this BDD to constant
   * true if they are included in their positive form in var, and constant false if they are
   * included in their negative form.
   *
   * <p><i>Note that this is quite different than Coudert and Madre's restrict function.</i>
   *
   * <p>Compare to bdd_restrict.
   *
   * @param var BDD containing the variables to be restricted
   * @return the result of the restrict operation
   * @see net.sf.javabdd.BDD#simplify(BDD)
   */
  public abstract BDD restrict(BDD var);

  /**
   * Mutates this BDD to restrict a set of variables to constant values. Restricts the variables in
   * this BDD to constant true if they are included in their positive form in var, and constant
   * false if they are included in their negative form. The "that" BDD is consumed, and can no
   * longer be used.
   *
   * <p><i>Note that this is quite different than Coudert and Madre's restrict function.</i>
   *
   * <p>Compare to bdd_restrict and bdd_delref.
   *
   * @param var BDD containing the variables to be restricted
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public abstract BDD restrictWith(BDD var);

  /**
   * Coudert and Madre's restrict function. Tries to simplify the BDD f by restricting it to the
   * domain covered by d. No checks are done to see if the result is actually smaller than the
   * input. This can be done by the user with a call to nodeCount().
   *
   * <p>Compare to bdd_simplify.
   *
   * @param d BDD containing the variables in the domain
   * @return the result of the simplify operation
   */
  public abstract BDD simplify(BDD d);

  /**
   * Returns the variable support of this BDD. The support is all the variables that this BDD
   * depends on.
   *
   * <p>Compare to bdd_support.
   *
   * @return the variable support of this BDD
   */
  public abstract BDD support();

  /**
   * Returns the result of applying the binary operator <tt>opr</tt> to the two BDDs.
   *
   * <p>Compare to bdd_apply.
   *
   * @param that the BDD to apply the operator on
   * @param opr the operator to apply
   * @return the result of applying the operator
   */
  public abstract BDD apply(BDD that, BDDFactory.BDDOp opr);

  /**
   * Makes this BDD be the result of the binary operator <tt>opr</tt> of two BDDs. The "that" BDD is
   * consumed, and can no longer be used. Attempting to use the passed in BDD again will result in
   * an exception being thrown.
   *
   * <p>Compare to bdd_apply and bdd_delref.
   *
   * @param that the BDD to apply the operator on
   * @param opr the operator to apply
   */
  public abstract BDD applyWith(BDD that, BDDFactory.BDDOp opr);

  /**
   * Applies the binary operator <tt>opr</tt> to two BDDs and then performs a universal
   * quantification of the variables from the variable set <tt>var</tt>.
   *
   * <p>Compare to bdd_appall.
   *
   * @param that the BDD to apply the operator on
   * @param opr the operator to apply
   * @param var BDD containing the variables to quantify
   * @return the result
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public abstract BDD applyAll(BDD that, BDDFactory.BDDOp opr, BDD var);

  /**
   * Applies the binary operator <tt>opr</tt> to two BDDs and then performs an existential
   * quantification of the variables from the variable set <tt>var</tt>.
   *
   * <p>Compare to bdd_appex.
   *
   * @param that the BDD to apply the operator on
   * @param opr the operator to apply
   * @param var BDD containing the variables to quantify
   * @return the result
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public abstract BDD applyEx(BDD that, BDDFactory.BDDOp opr, BDD var);

  /**
   * Applies the binary operator <tt>opr</tt> to two BDDs and then performs a unique quantification
   * of the variables from the variable set <tt>var</tt>.
   *
   * <p>Compare to bdd_appuni.
   *
   * @param that the BDD to apply the operator on
   * @param opr the operator to apply
   * @param var BDD containing the variables to quantify
   * @return the result
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public abstract BDD applyUni(BDD that, BDDFactory.BDDOp opr, BDD var);

  /**
   * Finds one satisfying variable assignment. Finds a BDD with at most one variable at each level.
   * The new BDD implies this BDD and is not false unless this BDD is false.
   *
   * <p>Compare to bdd_satone.
   *
   * @return one satisfying variable assignment
   */
  public abstract BDD satOne();

  /**
   * Finds one satisfying variable assignment. Finds a BDD with exactly one variable at all levels.
   * The new BDD implies this BDD and is not false unless this BDD is false.
   *
   * <p>Compare to bdd_fullsatone.
   *
   * @return one satisfying variable assignment
   */
  public abstract BDD fullSatOne();

  /**
   * Finds one satisfying variable assignment. Finds a minterm in this BDD. The <tt>var</tt>
   * argument is a set of variables that must be mentioned in the result. The polarity of these
   * variables in the result - in case they are undefined in this BDD - are defined by the
   * <tt>pol</tt> parameter. If <tt>pol</tt> is false, then all variables will be in negative form.
   * Otherwise they will be in positive form.
   *
   * <p>Compare to bdd_satoneset.
   *
   * @param var BDD containing the set of variables that must be mentioned in the result
   * @param pol the polarity of the result
   * @return one satisfying variable assignment
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public abstract BDD satOne(BDD var, boolean pol);

  /**
   * Finds all satisfying variable assignments.
   *
   * <p>Compare to bdd_allsat.
   *
   * @return all satisfying variable assignments
   */
  public AllSatIterator allsat() {
    return new AllSatIterator(this);
  }

  /**
   * Iterator that returns all satisfying assignments as byte arrays. In the byte arrays, -1 means
   * dont-care, 0 means 0, and 1 means 1.
   */
  public static class AllSatIterator implements Iterator<byte[]> {

    protected final BDDFactory f;
    protected LinkedList<BDD> loStack, hiStack;
    protected byte[] allsatProfile;
    protected final boolean useLevel;

    /**
     * Constructs a satisfying-assignment iterator on the given BDD. next() returns a byte array
     * indexed by BDD variable number.
     *
     * @param r BDD to iterate over
     */
    public AllSatIterator(BDD r) {
      this(r, false);
    }

    /**
     * Constructs a satisfying-assignment iterator on the given BDD. If lev is true, next() will
     * returns a byte array indexed by level. If lev is false, the byte array will be indexed by BDD
     * variable number.
     *
     * @param r BDD to iterate over
     * @param lev whether to index byte array by level instead of var
     */
    public AllSatIterator(BDD r, boolean lev) {
      f = r.getFactory();
      useLevel = lev;
      if (r.isZero()) {
        return;
      }
      allsatProfile = new byte[f.varNum()];
      Arrays.fill(allsatProfile, (byte) -1);
      loStack = new LinkedList<>();
      hiStack = new LinkedList<>();
      if (!r.isOne()) {
        loStack.addLast(r.id());
        if (!gotoNext()) {
          allsatProfile = null;
        }
      }
    }

    private boolean gotoNext() {
      BDD r;
      for (; ; ) {
        boolean lo_empty = loStack.isEmpty();
        if (lo_empty) {
          if (hiStack.isEmpty()) {
            return false;
          }
          r = hiStack.removeLast();
        } else {
          r = loStack.removeLast();
        }
        int LEVEL_r = r.level();
        allsatProfile[useLevel ? LEVEL_r : f.level2Var(LEVEL_r)] = lo_empty ? (byte) 1 : (byte) 0;
        BDD rn = lo_empty ? r.high() : r.low();
        for (int v = rn.level() - 1; v > LEVEL_r; --v) {
          allsatProfile[useLevel ? v : f.level2Var(v)] = -1;
        }
        if (!lo_empty) {
          hiStack.addLast(r);
        } else {
          r.free();
        }
        if (rn.isOne()) {
          rn.free();
          return true;
        }
        if (rn.isZero()) {
          rn.free();
          continue;
        }
        loStack.addLast(rn);
      }
    }

    @Override
    public boolean hasNext() {
      return allsatProfile != null;
    }

    @Override
    public byte[] next() {
      if (allsatProfile == null) {
        throw new NoSuchElementException();
      }
      byte[] b = new byte[allsatProfile.length];
      System.arraycopy(allsatProfile, 0, b, 0, b.length);
      if (!gotoNext()) {
        allsatProfile = null;
      }
      return b;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Scans this BDD to find all occurrences of BDD variables and returns an array that contains the
   * indices of the possible found BDD variables.
   *
   * <p>Compare to bdd_scanset.
   *
   * @return int[] containing indices of the possible found BDD variables
   */
  public int[] scanSet() {
    if (isOne() || isZero()) {
      return null;
    }

    int num = 0;
    BDD n = this.id();
    do {
      num++;
      BDD n2 = n.high();
      n.free();
      n = n2;
    } while (!n.isZero() && !n.isOne());

    int[] varset = new int[num];

    num = 0;
    n = this.id();
    do {
      varset[num++] = n.var();
      BDD n2 = n.high();
      n.free();
      n = n2;
    } while (!n.isZero() && !n.isOne());

    return varset;
  }

  /**
   * Scans this BDD and copies the stored variables into a integer array of BDDDomain variable
   * numbers. The numbers returned are guaranteed to be in ascending order.
   *
   * <p>Compare to fdd_scanset.
   *
   * @return int[]
   */
  public int[] scanSetDomains() {
    int[] fv;
    int[] varset;
    int fn;
    int num, n, m, i;

    fv = this.scanSet();
    if (fv == null) {
      return null;
    }
    fn = fv.length;

    BDDFactory factory = getFactory();

    for (n = 0, num = 0; n < factory.numberOfDomains(); n++) {
      BDDDomain dom = factory.getDomain(n);
      int[] ivar = dom.vars();
      boolean found = false;
      for (m = 0; m < dom.varNum() && !found; m++) {
        for (i = 0; i < fn && !found; i++) {
          if (ivar[m] == fv[i]) {
            num++;
            found = true;
          }
        }
      }
    }

    varset = new int[num];

    for (n = 0, num = 0; n < factory.numberOfDomains(); n++) {
      BDDDomain dom = factory.getDomain(n);
      int[] ivar = dom.vars();
      boolean found = false;
      for (m = 0; m < dom.varNum() && !found; m++) {
        for (i = 0; i < fn && !found; i++) {
          if (ivar[m] == fv[i]) {
            varset[num++] = n;
            found = true;
          }
        }
      }
    }

    return varset;
  }

  /**
   * Finds one satisfying assignment of the domain <tt>d</tt> in this BDD and returns that value.
   *
   * <p>Compare to fdd_scanvar.
   *
   * @param d domain to scan
   * @return one satisfying assignment for that domain
   */
  public BigInteger scanVar(BDDDomain d) {
    if (this.isZero()) {
      return BigInteger.valueOf(-1);
    }
    BigInteger[] allvar = this.scanAllVar();
    BigInteger res = allvar[d.getIndex()];
    return res;
  }

  /**
   * Finds one satisfying assignment in this BDD of all the defined BDDDomain's. Each value is
   * stored in an array which is returned. The size of this array is exactly the number of
   * BDDDomain's defined.
   *
   * <p>Compare to fdd_scanallvar.
   *
   * @return array containing one satisfying assignment of all the defined domains
   */
  public BigInteger[] scanAllVar() {
    int n;
    boolean[] store;
    BigInteger[] res;

    if (this.isZero()) {
      return null;
    }

    BDDFactory factory = getFactory();

    int bddvarnum = factory.varNum();
    store = new boolean[bddvarnum];

    BDD p = this.id();
    while (!p.isOne() && !p.isZero()) {
      BDD lo = p.low();
      if (!lo.isZero()) {
        store[p.var()] = false;
        BDD p2 = p.low();
        p.free();
        p = p2;
      } else {
        store[p.var()] = true;
        BDD p2 = p.high();
        p.free();
        p = p2;
      }
      lo.free();
    }

    int fdvarnum = factory.numberOfDomains();
    res = new BigInteger[fdvarnum];

    for (n = 0; n < fdvarnum; n++) {
      BDDDomain dom = factory.getDomain(n);
      int[] ivar = dom.vars();

      BigInteger val = BigInteger.ZERO;
      for (int m = dom.varNum() - 1; m >= 0; m--) {
        val = val.shiftLeft(1);
        if (store[ivar[m]]) {
          val = val.add(BigInteger.ONE);
        }
      }

      res[n] = val;
    }

    return res;
  }

  /**
   * Utility function to convert from a BDD varset to an array of levels.
   *
   * @param r BDD varset
   * @return array of levels
   */
  private static int[] varset2levels(BDD r) {
    int size = 0;
    BDD p = r.id();
    while (!p.isOne() && !p.isZero()) {
      ++size;
      BDD p2 = p.high();
      p.free();
      p = p2;
    }
    p.free();
    int[] result = new int[size];
    size = -1;
    p = r.id();
    while (!p.isOne() && !p.isZero()) {
      result[++size] = p.level();
      BDD p2 = p.high();
      p.free();
      p = p2;
    }
    p.free();
    return result;
  }

  /**
   * Returns an iteration of the satisfying assignments of this BDD. Returns an iteration of
   * minterms. The <tt>var</tt> argument is the set of variables that will be mentioned in the
   * result.
   *
   * @param var set of variables to mention in result
   * @return an iteration of minterms
   * @see net.sf.javabdd.BDDDomain#set()
   */
  public BDDIterator iterator(final BDD var) {
    return new BDDIterator(this, var);
  }

  /**
   * BDDIterator is used to iterate through the satisfying assignments of a BDD. It includes the
   * ability to check if bits are dont-cares and skip them.
   *
   * @author jwhaley
   * @version $Id: BDD.java,v 1.13 2005/06/03 20:20:16 joewhaley Exp $
   */
  public static class BDDIterator implements Iterator<BDD> {
    final BDDFactory f;
    final AllSatIterator i;
    // Reference to the initial BDD object, used to support the remove() operation.
    final BDD initialBDD;
    // List of levels that we care about.
    final int[] v;
    // Current bit assignment, indexed by indices of v.
    final boolean[] b;
    // Latest result from allsat iterator.
    byte[] a;
    // Last BDD returned.  Used to support the remove() operation.
    BDD lastReturned;

    /**
     * Construct a new BDDIterator on the given BDD. The var argument is the set of variables that
     * will be mentioned in the result.
     *
     * @param bdd BDD to iterate over
     * @param var variable set to mention in result
     */
    public BDDIterator(BDD bdd, BDD var) {
      initialBDD = bdd;
      f = bdd.getFactory();
      i = new AllSatIterator(bdd, true);
      // init v[]
      int n = 0;
      BDD p = var.id();
      while (!p.isOne()) {
        ++n;
        BDD q = p.high();
        p.free();
        p = q;
      }
      p.free();
      v = new int[n];
      n = 0;
      p = var.id();
      while (!p.isOne()) {
        v[n++] = p.level();
        BDD q = p.high();
        p.free();
        p = q;
      }
      p.free();
      // init b[]
      b = new boolean[n];
      gotoNext();
    }

    protected void gotoNext() {
      if (i.hasNext()) {
        a = i.next();
      } else {
        a = null;
        return;
      }
      for (int i = 0; i < v.length; ++i) {
        int vi = v[i];
        b[i] = a[vi] == 1;
      }
    }

    protected boolean gotoNextA() {
      for (int i = v.length - 1; i >= 0; --i) {
        int vi = v[i];
        if (a[vi] != -1) {
          continue;
        }
        if (b[i] == false) {
          b[i] = true;
          return true;
        }
        b[i] = false;
      }
      return false;
    }

    @Override
    public boolean hasNext() {
      return a != null;
    }

    @Override
    public BDD next() {
      if (a == null) {
        throw new NoSuchElementException();
      }
      // if (lastReturned != null) lastReturned.free();
      lastReturned = f.one();
      // for (int i = 0; i < v.length; ++i) {
      for (int i1 = v.length - 1; i1 >= 0; --i1) {
        int li = v[i1];
        int vi = f.level2Var(li);
        if (b[i1] == true) {
          lastReturned.andWith(f.ithVar(vi));
        } else {
          lastReturned.andWith(f.nithVar(vi));
        }
      }
      if (!gotoNextA()) {
        gotoNext();
      }
      return lastReturned;
    }

    public BigInteger nextValue(BDDDomain dom) {
      if (a == null) {
        throw new NoSuchElementException();
      }
      lastReturned = null;
      BigInteger val = BigInteger.ZERO;
      int[] ivar = dom.vars();
      for (int m = dom.varNum() - 1; m >= 0; m--) {
        val = val.shiftLeft(1);
        int level = f.var2Level(ivar[m]);
        int k = Arrays.binarySearch(v, level);
        if (k < 0) {
          val = null;
          break;
        }
        if (b[k]) {
          val = val.add(BigInteger.ONE);
        }
      }
      if (!gotoNextA()) {
        gotoNext();
      }
      return val;
    }

    /**
     * Return the next tuple of domain values in the iteration.
     *
     * @return the next tuple of domain values in the iteration.
     */
    public BigInteger[] nextTuple() {
      if (a == null) {
        throw new NoSuchElementException();
      }
      lastReturned = null;
      BigInteger[] result = new BigInteger[f.numberOfDomains()];
      for (int i = 0; i < result.length; ++i) {
        BDDDomain dom = f.getDomain(i);
        int[] ivar = dom.vars();
        BigInteger val = BigInteger.ZERO;
        for (int m = dom.varNum() - 1; m >= 0; m--) {
          val = val.shiftLeft(1);
          int level = f.var2Level(ivar[m]);
          int k = Arrays.binarySearch(v, level);
          if (k < 0) {
            val = null;
            break;
          }
          if (b[k]) {
            val = val.add(BigInteger.ONE);
          }
        }
        result[i] = val;
      }
      if (!gotoNextA()) {
        gotoNext();
      }
      return result;
    }

    /**
     * An alternate implementation of nextTuple(). This may be slightly faster than the default if
     * there are many domains.
     *
     * @return the next tuple of domain values in the iteration.
     */
    public BigInteger[] nextTuple2() {
      boolean[] store = nextSat();
      BigInteger[] result = new BigInteger[f.numberOfDomains()];
      for (int i = 0; i < result.length; ++i) {
        BDDDomain dom = f.getDomain(i);
        int[] ivar = dom.vars();
        BigInteger val = BigInteger.ZERO;
        for (int m = dom.varNum() - 1; m >= 0; m--) {
          val = val.shiftLeft(1);
          if (store[ivar[m]]) {
            val = val.add(BigInteger.ONE);
          }
        }
        result[i] = val;
      }
      return result;
    }

    /**
     * Return the next single satisfying assignment in the iteration.
     *
     * @return the next single satisfying assignment in the iteration.
     */
    public boolean[] nextSat() {
      if (a == null) {
        throw new NoSuchElementException();
      }
      lastReturned = null;
      boolean[] result = new boolean[f.varNum()];
      for (int i = 0; i < b.length; ++i) {
        result[f.level2Var(v[i])] = b[i];
      }
      if (!gotoNextA()) {
        gotoNext();
      }
      return result;
    }

    @Override
    public void remove() {
      if (lastReturned == null) {
        throw new IllegalStateException();
      }
      initialBDD.applyWith(lastReturned.id(), BDDFactory.diff);
      lastReturned = null;
    }

    /**
     * Returns true if the given BDD variable number is a dont-care. <tt>var</tt> must be a variable
     * in the iteration set.
     *
     * @param var variable number to check
     * @return if the given variable is a dont-care
     */
    public boolean isDontCare(int var) {
      if (a == null) {
        return false;
      }
      int level = f.var2Level(var);
      return a[level] == -1;
    }

    /**
     * Returns true if the BDD variables in the given BDD domain are all dont-care's.
     *
     * <p>
     *
     * @param d domain to check
     * @return if the variables are all dont-cares
     * @throws BDDException if d is not in the iteration set
     */
    public boolean isDontCare(BDDDomain d) {
      if (a == null) {
        return false;
      }
      int[] vars = d.vars();
      for (int var : vars) {
        if (!isDontCare(var)) {
          return false;
        }
      }
      return true;
    }

    /**
     * Fast-forward the iteration such that the given variable number is true.
     *
     * @param var number of variable
     */
    public void fastForward(int var) {
      if (a == null) {
        throw new BDDException();
      }
      int level = f.var2Level(var);
      int i = Arrays.binarySearch(v, level);
      if (i < 0 || a[i] != -1) {
        throw new BDDException();
      }
      b[i] = true;
    }

    /**
     * Fast-forward the iteration such that the given set of variables are true.
     *
     * @param vars set of variable indices
     */
    public void fastForward(int[] vars) {
      for (int var : vars) {
        fastForward(var);
      }
    }

    /**
     * Assuming <tt>d</tt> is a dont-care, skip to the end of the iteration for <tt>d</tt>
     *
     * @param d BDD domain to fast-forward past
     */
    public void skipDontCare(BDDDomain d) {
      int[] vars = d.vars();
      fastForward(vars);
      if (!gotoNextA()) {
        gotoNext();
      }
    }
  }

  /**
   * Returns a BDD where all variables are replaced with the variables defined by pair. Each entry
   * in pair consists of a old and a new variable. Whenever the old variable is found in this BDD
   * then a new node with the new variable is inserted instead.
   *
   * <p>Compare to bdd_replace.
   *
   * @param pair pairing of variables to the BDDs that replace those variables
   * @return result of replace
   */
  public abstract BDD replace(BDDPairing pair);

  /**
   * Replaces all variables in this BDD with the variables defined by pair. Each entry in pair
   * consists of a old and a new variable. Whenever the old variable is found in this BDD then a new
   * node with the new variable is inserted instead. Mutates the current BDD.
   *
   * <p>Compare to bdd_replace and bdd_delref.
   *
   * @param pair pairing of variables to the BDDs that replace those variables
   */
  public abstract BDD replaceWith(BDDPairing pair);

  /**
   * Prints the set of truth assignments specified by this BDD.
   *
   * <p>Compare to bdd_printset.
   */
  public void printSet() {
    System.out.println(this.toString());
  }

  /**
   * Prints this BDD using a set notation as in printSet() but with the index of the finite domain
   * blocks included instead of the BDD variables.
   *
   * <p>Compare to fdd_printset.
   */
  public void printSetWithDomains() {
    System.out.println(toStringWithDomains());
  }

  /**
   * Prints this BDD in dot graph notation.
   *
   * <p>Compare to bdd_printdot.
   */
  public void printDot() {
    PrintStream out = System.out;
    out.println("digraph G {");
    out.println("0 [shape=box, label=\"0\", style=filled, shape=box, height=0.3, width=0.3];");
    out.println("1 [shape=box, label=\"1\", style=filled, shape=box, height=0.3, width=0.3];");

    boolean[] visited = new boolean[nodeCount() + 2];
    visited[0] = true;
    visited[1] = true;
    HashMap<BDD, Integer> map = new HashMap<>();
    map.put(getFactory().zero(), 0);
    map.put(getFactory().one(), 1);
    printdot_rec(out, 1, visited, map);

    for (Object o : map.keySet()) {
      BDD b = (BDD) o;
      b.free();
    }
    out.println("}");
  }

  protected int printdot_rec(
      PrintStream out, int current, boolean[] visited, HashMap<BDD, Integer> map) {
    Integer ri = map.get(this);
    if (ri == null) {
      map.put(this.id(), ri = ++current);
    }
    int r = ri;
    if (visited[r]) {
      return current;
    }
    visited[r] = true;

    // TODO: support labelling of vars.
    out.println(r + " [label=\"" + this.var() + "\"];");

    BDD l = this.low(), h = this.high();
    Integer li = map.get(l);
    if (li == null) {
      map.put(l.id(), li = ++current);
    }
    int low = li;
    Integer hi = map.get(h);
    if (hi == null) {
      map.put(h.id(), hi = ++current);
    }
    int high = hi;

    out.println(r + " -> " + low + " [style=dotted];");
    out.println(r + " -> " + high + " [style=filled];");

    current = l.printdot_rec(out, current, visited, map);
    l.free();
    current = h.printdot_rec(out, current, visited, map);
    h.free();
    return current;
  }

  /**
   * Counts the number of distinct nodes used for this BDD.
   *
   * <p>Compare to bdd_nodecount.
   *
   * @return the number of distinct nodes used for this BDD
   */
  public abstract int nodeCount();

  /**
   * Counts the number of paths leading to the true terminal.
   *
   * <p>Compare to bdd_pathcount.
   *
   * @return the number of paths leading to the true terminal
   */
  public abstract double pathCount();

  /**
   * Calculates the number of satisfying variable assignments.
   *
   * <p>Compare to bdd_satcount.
   *
   * @return the number of satisfying variable assignments
   */
  public abstract double satCount();

  /**
   * Calculates the number of satisfying variable assignments to the variables in the given varset.
   * ASSUMES THAT THE BDD DOES NOT HAVE ANY ASSIGNMENTS TO VARIABLES THAT ARE NOT IN VARSET. You
   * will need to quantify out the other variables first.
   *
   * <p>Compare to bdd_satcountset.
   *
   * @return the number of satisfying variable assignments
   */
  public double satCount(BDD varset) {
    BDDFactory factory = getFactory();
    double unused = factory.varNum();

    if (varset.isZero() || varset.isOne() || isZero()) /* empty set */ {
      return 0.;
    }

    BDD n = varset.id();
    do {
      BDD n2 = n.high();
      n.free();
      n = n2;
      unused--;
    } while (!n.isOne() && !n.isZero());
    n.free();

    unused = satCount() / Math.pow(2.0, unused);

    return unused >= 1.0 ? unused : 1.0;
  }

  /**
   * Calculates the logarithm of the number of satisfying variable assignments.
   *
   * <p>Compare to bdd_satcount.
   *
   * @return the logarithm of the number of satisfying variable assignments
   */
  public double logSatCount() {
    return Math.log(satCount());
  }

  /**
   * Calculates the logarithm of the number of satisfying variable assignments to the variables in
   * the given varset.
   *
   * <p>Compare to bdd_satcountset.
   *
   * @return the logarithm of the number of satisfying variable assignments
   */
  public double logSatCount(BDD varset) {
    return Math.log(satCount(varset));
  }

  /**
   * Counts the number of times each variable occurs in this BDD. The result is stored and returned
   * in an integer array where the i'th position stores the number of times the i'th printing
   * variable occurred in the BDD.
   *
   * <p>Compare to bdd_varprofile.
   */
  public abstract int[] varProfile();

  /**
   * Returns true if this BDD equals that BDD, false otherwise.
   *
   * @param that the BDD to compare with
   * @return true iff the two BDDs are equal
   */
  public abstract boolean equals(BDD that);

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BDD)) {
      return false;
    }
    return this.equals((BDD) o);
  }

  @Override
  public abstract int hashCode();

  @Override
  public String toString() {
    BDDFactory f = this.getFactory();
    int[] set = new int[f.varNum()];
    StringBuffer sb = new StringBuffer();
    bdd_printset_rec(f, sb, this, set);
    return sb.toString();
  }

  private static void bdd_printset_rec(BDDFactory f, StringBuffer sb, BDD r, int[] set) {
    int n;
    boolean first;

    if (r.isZero()) {
      return;
    } else if (r.isOne()) {
      sb.append('<');
      first = true;

      for (n = 0; n < set.length; n++) {
        if (set[n] > 0) {
          if (!first) {
            sb.append(", ");
          }
          first = false;
          sb.append(f.level2Var(n));
          sb.append(':');
          sb.append((set[n] == 2 ? 1 : 0));
        }
      }
      sb.append('>');
    } else {
      set[f.var2Level(r.var())] = 1;
      BDD rl = r.low();
      bdd_printset_rec(f, sb, rl, set);
      rl.free();

      set[f.var2Level(r.var())] = 2;
      BDD rh = r.high();
      bdd_printset_rec(f, sb, rh, set);
      rh.free();

      set[f.var2Level(r.var())] = 0;
    }
  }

  /**
   * Returns a string representation of this BDD using the defined domains.
   *
   * @return string representation of this BDD using the defined domains
   */
  public String toStringWithDomains() {
    return toStringWithDomains(BDDToString.INSTANCE);
  }

  /**
   * Returns a string representation of this BDD on the defined domains, using the given BDDToString
   * converter.
   *
   * @see net.sf.javabdd.BDD.BDDToString
   * @return string representation of this BDD using the given BDDToString converter
   */
  public String toStringWithDomains(BDDToString ts) {
    if (this.isZero()) {
      return "F";
    }
    if (this.isOne()) {
      return "T";
    }

    BDDFactory bdd = getFactory();
    StringBuffer sb = new StringBuffer();
    int[] set = new int[bdd.varNum()];
    fdd_printset_rec(bdd, sb, ts, this, set);
    return sb.toString();
  }

  private static class OutputBuffer {
    BDDToString ts;
    StringBuffer sb;
    int domain;
    BigInteger lastLow;
    BigInteger lastHigh;
    boolean done;

    static final BigInteger MINUS2 = BigInteger.valueOf(-2);

    OutputBuffer(BDDToString ts, StringBuffer sb, int domain) {
      this.ts = ts;
      this.sb = sb;
      this.lastHigh = MINUS2;
      this.domain = domain;
    }

    void append(BigInteger low, BigInteger high) {
      if (low.equals(lastHigh.add(BigInteger.ONE))) {
        lastHigh = high;
      } else {
        finish();
        lastLow = low;
        lastHigh = high;
      }
    }

    StringBuffer finish() {
      if (!lastHigh.equals(MINUS2)) {
        if (done) {
          sb.append('/');
        }
        if (lastLow.equals(lastHigh)) {
          sb.append(ts.elementName(domain, lastHigh));
        } else {
          sb.append(ts.elementNames(domain, lastLow, lastHigh));
        }
        lastHigh = MINUS2;
      }
      done = true;
      return sb;
    }

    void append(BigInteger low) {
      append(low, low);
    }
  }

  private static void fdd_printset_helper(
      OutputBuffer sb, BigInteger value, int i, int[] set, int[] var, int maxSkip) {
    if (i == maxSkip) {
      // _assert(set[var[i]] == 0);
      BigInteger maxValue = value.or(BigInteger.ONE.shiftLeft(i + 1).subtract(BigInteger.ONE));
      sb.append(value, maxValue);
      return;
    }
    int val = set[var[i]];
    if (val == 0) {
      BigInteger temp = value.setBit(i);
      fdd_printset_helper(sb, temp, i - 1, set, var, maxSkip);
    }
    fdd_printset_helper(sb, value, i - 1, set, var, maxSkip);
  }

  private static void fdd_printset_rec(
      BDDFactory bdd, StringBuffer sb, BDDToString ts, BDD r, int[] set) {
    int fdvarnum = bdd.numberOfDomains();

    int n, m, i;
    boolean used = false;
    int[] var;
    boolean first;

    if (r.isZero()) {
      return;
    } else if (r.isOne()) {
      sb.append('<');
      first = true;

      for (n = 0; n < fdvarnum; n++) {
        used = false;

        BDDDomain domain_n = bdd.getDomain(n);

        int[] domain_n_ivar = domain_n.vars();
        int domain_n_varnum = domain_n_ivar.length;
        for (m = 0; m < domain_n_varnum; m++) {
          if (set[domain_n_ivar[m]] != 0) {
            used = true;
          }
        }

        if (used) {
          if (!first) {
            sb.append(", ");
          }
          first = false;
          sb.append(domain_n.getName());
          sb.append(':');

          var = domain_n_ivar;

          BigInteger pos = BigInteger.ZERO;
          int maxSkip = -1;
          boolean hasDontCare = false;
          for (i = 0; i < domain_n_varnum; ++i) {
            int val = set[var[i]];
            if (val == 0) {
              hasDontCare = true;
              if (maxSkip == i - 1) {
                maxSkip = i;
              }
            }
          }
          for (i = domain_n_varnum - 1; i >= 0; --i) {
            pos = pos.shiftLeft(1);
            int val = set[var[i]];
            if (val == 2) {
              pos = pos.setBit(0);
            }
          }
          if (!hasDontCare) {
            sb.append(ts.elementName(n, pos));
          } else {
            OutputBuffer ob = new OutputBuffer(ts, sb, n);
            fdd_printset_helper(ob, pos, domain_n_varnum - 1, set, var, maxSkip);
            ob.finish();
          }
        }
      }

      sb.append('>');
    } else {
      set[r.var()] = 1;
      BDD lo = r.low();
      fdd_printset_rec(bdd, sb, ts, lo, set);
      lo.free();

      set[r.var()] = 2;
      BDD hi = r.high();
      fdd_printset_rec(bdd, sb, ts, hi, set);
      hi.free();

      set[r.var()] = 0;
    }
  }

  /**
   * BDDToString is used to specify the printing behavior of BDDs with domains. Subclass this type
   * and pass it as an argument to toStringWithDomains to have the toStringWithDomains function use
   * your domain names and element names, instead of just numbers.
   */
  public static class BDDToString {
    /**
     * Singleton instance that does the default behavior: domains and elements are printed as their
     * numbers.
     */
    public static final BDDToString INSTANCE = new BDDToString();

    /** Protected constructor. */
    protected BDDToString() {}

    /**
     * Given a domain index and an element index, return the element's name. Called by the
     * toStringWithDomains() function.
     *
     * @param i the domain number
     * @param j the element number
     * @return the string representation of that element
     */
    public String elementName(int i, BigInteger j) {
      return j.toString();
    }

    /**
     * Given a domain index and an inclusive range of element indices, return the names of the
     * elements in that range. Called by the toStringWithDomains() function.
     *
     * @param i the domain number
     * @param lo the low range of element numbers, inclusive
     * @param hi the high range of element numbers, inclusive
     * @return the string representation of the elements in the range
     */
    public String elementNames(int i, BigInteger lo, BigInteger hi) {
      return lo.toString() + "-" + hi.toString();
    }
  }

  /** Frees this BDD. Further use of this BDD will result in an exception being thrown. */
  public abstract void free();

  /** Protected constructor. */
  protected BDD() {}
}

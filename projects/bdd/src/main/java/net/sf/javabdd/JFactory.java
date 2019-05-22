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

import com.google.common.primitives.Ints;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import javax.annotation.Nonnull;

/**
 * This is a 100% Java implementation of the BDD factory. It is based on the C source code for
 * BuDDy. As such, the implementation is very ugly, but it works. Like BuDDy, it uses a reference
 * counting scheme for garbage collection.
 *
 * @author John Whaley
 * @version $Id: JFactory.java,v 1.28 2005/09/27 22:56:18 joewhaley Exp $
 */
public final class JFactory extends BDDFactory {
  /** Whether to maintain (and in some cases print) statistics about the cache use. */
  private static final boolean CACHESTATS = false;

  /**
   * Whether to flush (clear completely) the cache when live BDD nodes are garbage collected. If
   * {@code false}, the cache will be attempted to be cleaned and maintain existing valid cache
   * entries.
   */
  // Warning: we've never tried with this flag false.
  private static final boolean FLUSH_CACHE_ON_GC = true;

  /**
   * If true, all BDDs will be created with a finalizer that attempts to free them if the user has
   * not done so. This flag implies non-trivial runtime overhead but defers BDD garbage collection
   * to Java, rather than manual user control of reference counting.
   */
  private static final boolean USE_FINALIZER = false;

  /**
   * When {@link #USE_FINALIZER} is true, setting this flag to true enables debug print messages
   * whenever BDDs are freed by the finalizer instead of by the calling code.
   */
  private static final boolean DEBUG_FINALIZER = false;

  /**
   * If set, assertions will be made on BDD internal computations. Used in developing the factory.
   */
  private static final boolean VERIFY_ASSERTIONS = false;

  private static final String REVISION = "$Revision: 1.28 $";

  @Override
  public String getVersion() {
    return "JFactory " + REVISION.substring(11, REVISION.length() - 2);
  }

  private JFactory() {
    supportSet = new int[0];
  }

  public static BDDFactory init(int nodenum, int cachesize) {
    BDDFactory f = new JFactory();
    f.initialize(nodenum, cachesize);
    return f;
  }

  /** Private helper function to create BDD objects. */
  private BDDImpl makeBDD(int id) {
    if (USE_FINALIZER) {
      return new BDDImplWithFinalizer(id);
    } else {
      return new BDDImpl(id);
    }
  }

  /** Wrapper for the BDD index number used internally in the representation. */
  private class BDDImpl extends BDD {
    int _index;

    BDDImpl(int index) {
      this._index = index;
      bdd_addref(_index);
    }

    @Override
    public BDDFactory getFactory() {
      return JFactory.this;
    }

    @Override
    public boolean isZero() {
      return _index == BDDZERO;
    }

    @Override
    public boolean isOne() {
      return _index == BDDONE;
    }

    @Override
    public int var() {
      return bdd_var(_index);
    }

    @Override
    public BDD high() {
      return makeBDD(HIGH(_index));
    }

    @Override
    public BDD low() {
      return makeBDD(LOW(_index));
    }

    @Override
    public BDD id() {
      return makeBDD(_index);
    }

    @Override
    public BDD not() {
      return makeBDD(bdd_not(_index));
    }

    @Override
    public BDD ite(BDD thenBDD, BDD elseBDD) {
      int x = _index;
      int y = ((BDDImpl) thenBDD)._index;
      int z = ((BDDImpl) elseBDD)._index;
      return makeBDD(bdd_ite(x, y, z));
    }

    @Override
    public BDD relprod(BDD that, BDD var) {
      int x = _index;
      int y = ((BDDImpl) that)._index;
      int z = ((BDDImpl) var)._index;
      return makeBDD(bdd_relprod(x, y, z));
    }

    @Override
    public BDD compose(BDD g, int var) {
      int x = _index;
      int y = ((BDDImpl) g)._index;
      return makeBDD(bdd_compose(x, y, var));
    }

    @Override
    public BDD veccompose(BDDPairing pair) {
      int x = _index;
      return makeBDD(bdd_veccompose(x, (bddPair) pair));
    }

    @Override
    public BDD constrain(BDD that) {
      int x = _index;
      int y = ((BDDImpl) that)._index;
      return makeBDD(bdd_constrain(x, y));
    }

    @Override
    public BDD exist(BDD var) {
      int x = _index;
      int y = ((BDDImpl) var)._index;
      return makeBDD(bdd_exist(x, y));
    }

    @Override
    public BDD forAll(BDD var) {
      int x = _index;
      int y = ((BDDImpl) var)._index;
      return makeBDD(bdd_forall(x, y));
    }

    @Override
    public BDD unique(BDD var) {
      int x = _index;
      int y = ((BDDImpl) var)._index;
      return makeBDD(bdd_unique(x, y));
    }

    @Override
    public BDD restrict(BDD var) {
      int x = _index;
      int y = ((BDDImpl) var)._index;
      return makeBDD(bdd_restrict(x, y));
    }

    @Override
    public BDD restrictWith(BDD that) {
      int x = _index;
      int y = ((BDDImpl) that)._index;
      int a = bdd_restrict(x, y);
      bdd_delref(x);
      if (this != that) {
        that.free();
      }
      bdd_addref(a);
      this._index = a;
      return this;
    }

    @Override
    public BDD simplify(BDD d) {
      int x = _index;
      int y = ((BDDImpl) d)._index;
      return makeBDD(bdd_simplify(x, y));
    }

    @Override
    public BDD support() {
      int x = _index;
      return makeBDD(bdd_support(x));
    }

    @Override
    public BDD apply(BDD that, BDDOp opr) {
      int x = _index;
      int y = ((BDDImpl) that)._index;
      int z = opr.id;
      return makeBDD(bdd_apply(x, y, z));
    }

    @Override
    public BDD applyWith(BDD that, BDDOp opr) {
      int x = _index;
      int y = ((BDDImpl) that)._index;
      int z = opr.id;
      int a = bdd_apply(x, y, z);
      bdd_delref(x);
      if (this != that) {
        that.free();
      }
      bdd_addref(a);
      this._index = a;
      return this;
    }

    @Override
    public BDD applyAll(BDD that, BDDOp opr, BDD var) {
      int x = _index;
      int y = ((BDDImpl) that)._index;
      int z = opr.id;
      int a = ((BDDImpl) var)._index;
      return makeBDD(bdd_appall(x, y, z, a));
    }

    @Override
    public BDD applyEx(BDD that, BDDOp opr, BDD var) {
      int x = _index;
      int y = ((BDDImpl) that)._index;
      int z = opr.id;
      int a = ((BDDImpl) var)._index;
      return makeBDD(bdd_appex(x, y, z, a));
    }

    @Override
    public BDD applyUni(BDD that, BDDOp opr, BDD var) {
      int x = _index;
      int y = ((BDDImpl) that)._index;
      int z = opr.id;
      int a = ((BDDImpl) var)._index;
      return makeBDD(bdd_appuni(x, y, z, a));
    }

    @Override
    public BDD satOne() {
      int x = _index;
      return makeBDD(bdd_satone(x));
    }

    @Override
    public BDD fullSatOne() {
      int x = _index;
      return makeBDD(bdd_fullsatone(x));
    }

    @Override
    public BDD satOne(BDD var, boolean pol) {
      int x = _index;
      int y = ((BDDImpl) var)._index;
      int z = pol ? 1 : 0;
      return makeBDD(bdd_satoneset(x, y, z));
    }

    @Override
    public BDD replace(BDDPairing pair) {
      int x = _index;
      return makeBDD(bdd_replace(x, (bddPair) pair));
    }

    @Override
    public BDD replaceWith(BDDPairing pair) {
      int x = _index;
      int y = bdd_replace(x, (bddPair) pair);
      bdd_delref(x);
      bdd_addref(y);
      _index = y;
      return this;
    }

    @Override
    public int nodeCount() {
      return bdd_nodecount(_index);
    }

    @Override
    public double pathCount() {
      return bdd_pathcount(_index);
    }

    @Override
    public double satCount() {
      return bdd_satcount(_index).doubleValue();
    }

    @Override
    public int[] varProfile() {
      int x = _index;
      return bdd_varprofile(x);
    }

    @Override
    public boolean equals(BDD that) {
      boolean b = this._index == ((BDDImpl) that)._index;
      return b;
    }

    @Override
    public int hashCode() {
      return _index;
    }

    @Override
    public void free() {
      bdd_delref(_index);
      _index = INVALID_BDD;
    }
  }

  private class BDDImplWithFinalizer extends BDDImpl {

    BDDImplWithFinalizer(int id) {
      super(id);
    }

    @Override
    protected void finalize() throws Throwable {
      super.finalize();
      if (USE_FINALIZER) {
        if (DEBUG_FINALIZER && _index >= 0) {
          System.out.println("BDD not freed! " + System.identityHashCode(this));
        }
        this.free();
      }
    }
  }

  private static final int REF_MASK = 0xFFC00000;
  private static final int MARK_MASK = 0x00200000;
  private static final int LEV_MASK = 0x001FFFFF;
  private static final int MAXVAR = LEV_MASK;
  private static final int INVALID_BDD = -1;

  private static final int REF_INC = 0x00400000;

  private static final int offset__refcou_and_level = 0;
  private static final int offset__low = 1;
  private static final int offset__high = 2;
  private static final int offset__hash = 3;
  private static final int offset__next = 4;
  private static final int __node_size = 5;

  private boolean HASREF(int node) {
    boolean r = (bddnodes[node * __node_size + offset__refcou_and_level] & REF_MASK) != 0;
    return r;
  }

  private void SETMAXREF(int node) {
    bddnodes[node * __node_size + offset__refcou_and_level] |= REF_MASK;
  }

  private void CLEARREF(int node) {
    bddnodes[node * __node_size + offset__refcou_and_level] &= ~REF_MASK;
  }

  private void INCREF(int node) {
    if ((bddnodes[node * __node_size + offset__refcou_and_level] & REF_MASK) != REF_MASK) {
      bddnodes[node * __node_size + offset__refcou_and_level] += REF_INC;
    }
  }

  private void DECREF(int node) {
    int rc = bddnodes[node * __node_size + offset__refcou_and_level] & REF_MASK;
    if (rc != REF_MASK && rc != 0) {
      bddnodes[node * __node_size + offset__refcou_and_level] -= REF_INC;
    }
  }

  private int GETREF(int node) {
    return bddnodes[node * __node_size + offset__refcou_and_level] >>> 22;
  }

  private int LEVEL(int node) {
    return bddnodes[node * __node_size + offset__refcou_and_level] & LEV_MASK;
  }

  private int LEVELANDMARK(int node) {
    return bddnodes[node * __node_size + offset__refcou_and_level] & (LEV_MASK | MARK_MASK);
  }

  private void SETLEVEL(int node, int val) {
    if (VERIFY_ASSERTIONS) {
      _assert(val == (val & LEV_MASK));
    }
    bddnodes[node * __node_size + offset__refcou_and_level] &= ~LEV_MASK;
    bddnodes[node * __node_size + offset__refcou_and_level] |= val;
  }

  private void SETLEVELANDMARK(int node, int val) {
    if (VERIFY_ASSERTIONS) {
      _assert(val == (val & (LEV_MASK | MARK_MASK)));
    }
    bddnodes[node * __node_size + offset__refcou_and_level] &= ~(LEV_MASK | MARK_MASK);
    bddnodes[node * __node_size + offset__refcou_and_level] |= val;
  }

  private void SETMARK(int n) {
    bddnodes[n * __node_size + offset__refcou_and_level] |= MARK_MASK;
  }

  private void UNMARK(int n) {
    bddnodes[n * __node_size + offset__refcou_and_level] &= ~MARK_MASK;
  }

  private boolean MARKED(int n) {
    return (bddnodes[n * __node_size + offset__refcou_and_level] & MARK_MASK) != 0;
  }

  private int LOW(int r) {
    return bddnodes[r * __node_size + offset__low];
  }

  private void SETLOW(int r, int v) {
    bddnodes[r * __node_size + offset__low] = v;
  }

  private int HIGH(int r) {
    return bddnodes[r * __node_size + offset__high];
  }

  private void SETHIGH(int r, int v) {
    bddnodes[r * __node_size + offset__high] = v;
  }

  private int HASH(int r) {
    return bddnodes[r * __node_size + offset__hash];
  }

  private void SETHASH(int r, int v) {
    bddnodes[r * __node_size + offset__hash] = v;
  }

  private int NEXT(int r) {
    return bddnodes[r * __node_size + offset__next];
  }

  private void SETNEXT(int r, int v) {
    bddnodes[r * __node_size + offset__next] = v;
  }

  private int VARr(int n) {
    return LEVELANDMARK(n);
  }

  private void SETVARr(int n, int val) {
    SETLEVELANDMARK(n, val);
  }

  private static void _assert(boolean b) {
    if (!b) {
      throw new InternalError();
    }
  }

  private abstract static class BddCacheData {
    int a, b, c;

    abstract BddCacheData copy();
  }

  private static class BddCacheDataI extends BddCacheData {
    int res;

    @Override
    BddCacheData copy() {
      BddCacheDataI that = new BddCacheDataI();
      that.a = this.a;
      that.b = this.b;
      that.c = this.c;
      that.res = this.res;
      return that;
    }
  }

  // a = index, c = operator, value = value.
  private static class BigIntegerBddCacheData extends BddCacheData {
    BigInteger value;

    @Override
    BddCacheData copy() {
      BigIntegerBddCacheData that = new BigIntegerBddCacheData();
      that.a = a;
      that.b = b;
      that.c = c;
      that.value = value;
      return that;
    }
  }

  // a = operator, b = result, c = unused
  private static class MultiOpBddCacheData extends BddCacheData {
    int[] operands;

    @Override
    BddCacheData copy() {
      MultiOpBddCacheData that = new MultiOpBddCacheData();
      that.a = a;
      that.b = b;
      that.c = c;
      that.operands = Arrays.copyOf(operands, operands.length);
      return that;
    }
  }

  private static class BddCache {
    BddCacheData[] table;
    int tablesize;

    BddCache copy() {
      BddCache that = new BddCache();
      if (this.table instanceof BddCacheDataI[]) {
        that.table = new BddCacheDataI[this.table.length];
      } else if (this.table instanceof BigIntegerBddCacheData[]) {
        that.table = new BigIntegerBddCacheData[this.table.length];
      } else if (this.table instanceof MultiOpBddCacheData[]) {
        that.table = new MultiOpBddCacheData[this.table.length];
      } else {
        throw new IllegalStateException("Unexpected BddCache type");
      }
      that.tablesize = this.tablesize;
      for (int i = 0; i < table.length; ++i) {
        that.table[i] = this.table[i].copy();
      }
      return that;
    }

    /**
     * Returns the number of used entries in this cache.
     *
     * <p>Slow. Should only be used in debugging contexts.
     */
    private int used() {
      // Array lengths in Java must be representable by a signed int.
      return (int) Arrays.stream(table).filter(e -> e.a != -1).count();
    }
  }

  private static class JavaBDDException extends BDDException {
    /** Version ID for serialization. */
    private static final long serialVersionUID = 3257289144995952950L;

    JavaBDDException(int x) {
      super(errorstrings[-x]);
    }
  }

  private static final int BDDONE = 1;
  private static final int BDDZERO = 0;

  private boolean bddrunning; /* Flag - package initialized */
  private int bdderrorcond; /* Some error condition */
  private int bddnodesize; /* Number of allocated nodes */
  private int bddmaxnodesize; /* Maximum allowed number of nodes */
  private int bddmaxnodeincrease; /* Max. # of nodes used to inc. table */
  private int[] bddnodes; /* All of the bdd nodes */
  private int bddfreepos; /* First free node */
  private int bddfreenum; /* Number of free nodes */
  private int bddproduced; /* Number of new nodes ever produced */
  private int bddvarnum; /* Number of defined BDD variables */
  private int[] bddrefstack; /* Internal node reference stack */
  private int bddrefstacktop; /* Internal node reference stack top */
  private int[] bddvar2level; /* Variable -> level table */
  private int[] bddlevel2var; /* Level -> variable table */
  private boolean bddresized; /* Flag indicating a resize of the nodetable */

  private int minfreenodes = 20;

  /*=== PRIVATE KERNEL VARIABLES =========================================*/

  private int[] bddvarset; /* Set of defined BDD variables */
  private int gbcollectnum; /* Number of garbage collections */
  private int cachesize; /* Size of the operator caches */
  private long gbcclock; /* Clock ticks used in GBC */

  private static final int BDD_MEMORY = -1; /* Out of memory */
  private static final int BDD_VAR = -2; /* Unknown variable */
  private static final int BDD_RANGE = -3;
  /* Variable value out of range (not in domain) */
  private static final int BDD_DEREF = -4;
  /* Removing external reference to unknown node */
  private static final int BDD_RUNNING = -5;
  /* Called bdd_init() twice whithout bdd_done() */
  private static final int BDD_FILE = -6; /* Some file operation failed */
  private static final int BDD_FORMAT = -7; /* Incorrect file format */
  private static final int BDD_ORDER = -8;
  /* Vars. not in order for vector based functions */
  private static final int BDD_BREAK = -9; /* User called break */
  private static final int BDD_VARNUM = -10;
  /* Different number of vars. for vector pair */
  private static final int BDD_NODES = -11;
  /* Tried to set max. number of nodes to be fewer */
  /* than there already has been allocated */
  private static final int BDD_OP = -12; /* Unknown operator */
  private static final int BDD_VARSET = -13; /* Illegal variable set */
  private static final int BDD_VARBLK = -14; /* Bad variable block operation */
  private static final int BDD_DECVNUM = -15;
  /* Trying to decrease the number of variables */
  private static final int BDD_REPLACE = -16;
  /* Replacing to already existing variables */
  private static final int BDD_NODENUM = -17;
  /* Number of nodes reached user defined maximum */
  private static final int BDD_ILLBDD = -18; /* Illegal bdd argument */
  private static final int BDD_SIZE = -19; /* Illegal size argument */

  private static final int BVEC_SIZE = -20; /* Mismatch in bitvector size */
  private static final int BVEC_SHIFT = -21;
  /* Illegal shift-left/right parameter */
  private static final int BVEC_DIVZERO = -22; /* Division by zero */

  private static final int BDD_ERRNUM = 24;

  /* Strings for all error mesages */
  private static String[] errorstrings = {
    "",
    "Out of memory",
    "Unknown variable",
    "Value out of range",
    "Unknown BDD root dereferenced",
    "bdd_init() called twice",
    "File operation failed",
    "Incorrect file format",
    "Variables not in ascending order",
    "User called break",
    "Mismatch in size of variable sets",
    "Cannot allocate fewer nodes than already in use",
    "Unknown operator",
    "Illegal variable set",
    "Bad variable block operation",
    "Trying to decrease the number of variables",
    "Trying to replace with variables already in the bdd",
    "Number of nodes reached user defined maximum",
    "Unknown BDD - was not in node table",
    "Bad size argument",
    "Mismatch in bitvector size",
    "Illegal shift-left/right parameter",
    "Division by zero"
  };

  private static final int DEFAULTMAXNODEINC = 10000000;

  /*=== OTHER INTERNAL DEFINITIONS =======================================*/

  private static int PAIR(int a, int b) {
    // return Math.abs((a + b) * (a + b + 1) / 2 + a);
    return (a + b) * (a + b + 1) / 2 + a;
  }

  private static int TRIPLE(int a, int b, int c) {
    // return Math.abs(PAIR(c, PAIR(a, b)));
    return PAIR(c, PAIR(a, b));
  }

  private int NODEHASH(int lvl, int l, int h) {
    return Math.abs(TRIPLE(lvl, l, h) % bddnodesize);
  }

  @Override
  public BDD zero() {
    return makeBDD(BDDZERO);
  }

  @Override
  public BDD one() {
    return makeBDD(BDDONE);
  }

  private int bdd_ithvar(int var) {
    if (var < 0 || var >= bddvarnum) {
      bdd_error(BDD_VAR);
      return BDDZERO;
    }

    return bddvarset[var * 2];
  }

  private int bdd_nithvar(int var) {
    if (var < 0 || var >= bddvarnum) {
      bdd_error(BDD_VAR);
      return BDDZERO;
    }

    return bddvarset[var * 2 + 1];
  }

  private int bdd_varnum() {
    return bddvarnum;
  }

  private static int bdd_error(int v) {
    throw new JavaBDDException(v);
  }

  private static boolean ISZERO(int r) {
    return r == BDDZERO;
  }

  private static boolean ISONE(int r) {
    return r == BDDONE;
  }

  private static boolean ISCONST(int r) {
    // return r == BDDZERO || r == BDDONE;
    return r < 2;
  }

  private void CHECK(int r) {
    if (!bddrunning) {
      bdd_error(BDD_RUNNING);
    } else if (r < 0 || r >= bddnodesize) {
      bdd_error(BDD_ILLBDD);
    } else if (r >= 2 && LOW(r) == INVALID_BDD) {
      bdd_error(BDD_ILLBDD);
    }
  }

  private int bdd_var(int root) {
    CHECK(root);
    if (root < 2) {
      bdd_error(BDD_ILLBDD);
    }

    return bddlevel2var[LEVEL(root)];
  }

  private void checkresize() {
    if (bddresized) {
      bdd_operator_noderesize();
    }
    bddresized = false;
  }

  private static int NOTHASH(int r) {
    return r;
  }

  private static int APPLYHASH(int l, int r, int op) {
    return TRIPLE(l, r, op);
  }

  private static int MULTIOPHASH(int[] operands, int op) {
    int result = op;
    for (int operand : operands) {
      result = PAIR(result, operand);
    }
    return result;
  }

  private static int RESTRHASH(int r, int var) {
    return PAIR(r, var);
  }

  private static int CONSTRAINHASH(int f, int c) {
    return PAIR(f, c);
  }

  private static int QUANTHASH(int r) {
    return r;
  }

  private static int REPLACEHASH(int cacheid, int r) {
    return PAIR(cacheid, r);
  }

  private static int CORRECTIFYHASH(int level, int l, int r) {
    return TRIPLE(level, l, r);
  }

  private static int VECCOMPOSEHASH(int cacheid, int f) {
    return PAIR(cacheid, f);
  }

  private static int COMPOSEHASH(int cacheid, int f, int g) {
    return TRIPLE(cacheid, f, g);
  }

  private static int SATCOUHASH(int r, int miscid) {
    return PAIR(r, miscid);
  }

  private static int PATHCOUHASH(int r, int miscid) {
    return PAIR(r, miscid);
  }

  private static int APPEXHASH(int l, int r, int op) {
    return PAIR(l, r);
  }

  private boolean INVARSET(int a) {
    return quantvarset[a] == quantvarsetID; /* unsigned check */
  }

  private boolean INSVARSET(int a) {
    return Math.abs(quantvarset[a]) == quantvarsetID; /* signed check */
  }

  private static final int bddop_and = 0; // NOTE: ite_rec caching exploits bddop_and==0.
  private static final int bddop_xor = 1;
  private static final int bddop_or = 2;
  private static final int bddop_nand = 3;
  private static final int bddop_nor = 4;
  private static final int bddop_imp = 5;
  private static final int bddop_biimp = 6;
  private static final int bddop_diff = 7;
  private static final int bddop_less = 8;
  private static final int bddop_invimp = 9;

  /* Should *not* be used in bdd_apply calls !!! */
  private static final int bddop_not = 10;
  private static final int bddop_simplify = 11;

  @Override
  public BDD orAll(BDD... bddOperands) {
    return orAll(Arrays.asList(bddOperands));
  }

  @Override
  public BDD orAll(Collection<BDD> bddOperands) {
    int[] operands =
        bddOperands.stream()
            .mapToInt(bdd -> ((BDDImpl) bdd)._index)
            .filter(i -> i != 0)
            .sorted()
            .distinct()
            .toArray();
    if (operands.length == 0) {
      return zero();
    } else if (ISONE(operands[0])) {
      return one();
    } else {
      return makeBDD(bdd_orAll(operands));
    }
  }

  private int bdd_orAll(int[] operands) {
    if (multiopcache == null) {
      multiopcache = BddCacheMultiOp_init(cachesize);
    }

    INITREF();
    int res = orAll_rec(operands);
    checkresize();

    return res;
  }

  private int bdd_not(int r) {
    CHECK(r);

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }

    INITREF();
    int res = not_rec(r);
    checkresize();

    return res;
  }

  private int not_rec(int r) {
    BddCacheDataI entry;
    int res;

    if (ISZERO(r)) {
      return BDDONE;
    } else if (ISONE(r)) {
      return BDDZERO;
    }

    entry = BddCache_lookupI(applycache, NOTHASH(r));

    if (entry.a == r && entry.c == bddop_not) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    PUSHREF(not_rec(LOW(r)));
    PUSHREF(not_rec(HIGH(r)));
    res = bdd_makenode(LEVEL(r), READREF(2), READREF(1));
    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = r;
    entry.c = bddop_not;
    entry.res = res;

    return res;
  }

  private int bdd_ite(int f, int g, int h) {
    CHECK(f);
    CHECK(g);
    CHECK(h);

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }

    INITREF();
    int res = ite_rec(f, g, h);
    checkresize();

    return res;
  }

  private int ite_rec(int f, int g, int h) {
    BddCacheDataI entry;
    int res;

    if (ISONE(f)) {
      return g;
    } else if (ISZERO(f)) {
      return h;
    } else if (g == h) {
      return g;
    } else if (ISZERO(g)) {
      applyop = bddop_less;
      return apply_rec(f, h);
    } else if (ISONE(g)) {
      return or_rec(f, h);
    } else if (ISZERO(h)) {
      return and_rec(f, g);
    } else if (ISONE(h)) {
      applyop = bddop_imp;
      return apply_rec(f, g);
    }

    // ITE and APPLY share the same cache:
    //    APPLY is (l, r, op) where op in 0..10 (0=and, ..., 10=not) where l, r are BDD ids.
    //    ITE is (f, g, -h) where f, g, h are all BDD ids.
    //
    // The only possible collision is apply(l, r, bddop_and) and ite(l, r, 0==BDDZERO).
    // Fortuitously, these are logically equivalent -- if f then g else false === f and g.
    entry = BddCache_lookupI(applycache, APPLYHASH(f, g, -h));
    if (entry.a == f && entry.b == g && entry.c == -h) { // To explain -h, see caching note above.
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (LEVEL(f) == LEVEL(g)) {
      if (LEVEL(f) == LEVEL(h)) {
        PUSHREF(ite_rec(LOW(f), LOW(g), LOW(h)));
        PUSHREF(ite_rec(HIGH(f), HIGH(g), HIGH(h)));
        res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
      } else if (LEVEL(f) < LEVEL(h)) {
        PUSHREF(ite_rec(LOW(f), LOW(g), h));
        PUSHREF(ite_rec(HIGH(f), HIGH(g), h));
        res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
      } else /* f > h */ {
        PUSHREF(ite_rec(f, g, LOW(h)));
        PUSHREF(ite_rec(f, g, HIGH(h)));
        res = bdd_makenode(LEVEL(h), READREF(2), READREF(1));
      }
    } else if (LEVEL(f) < LEVEL(g)) {
      if (LEVEL(f) == LEVEL(h)) {
        PUSHREF(ite_rec(LOW(f), g, LOW(h)));
        PUSHREF(ite_rec(HIGH(f), g, HIGH(h)));
        res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
      } else if (LEVEL(f) < LEVEL(h)) {
        PUSHREF(ite_rec(LOW(f), g, h));
        PUSHREF(ite_rec(HIGH(f), g, h));
        res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
      } else /* f > h */ {
        PUSHREF(ite_rec(f, g, LOW(h)));
        PUSHREF(ite_rec(f, g, HIGH(h)));
        res = bdd_makenode(LEVEL(h), READREF(2), READREF(1));
      }
    } else /* f > g */ {
      if (LEVEL(g) == LEVEL(h)) {
        PUSHREF(ite_rec(f, LOW(g), LOW(h)));
        PUSHREF(ite_rec(f, HIGH(g), HIGH(h)));
        res = bdd_makenode(LEVEL(g), READREF(2), READREF(1));
      } else if (LEVEL(g) < LEVEL(h)) {
        PUSHREF(ite_rec(f, LOW(g), h));
        PUSHREF(ite_rec(f, HIGH(g), h));
        res = bdd_makenode(LEVEL(g), READREF(2), READREF(1));
      } else /* g > h */ {
        PUSHREF(ite_rec(f, g, LOW(h)));
        PUSHREF(ite_rec(f, g, HIGH(h)));
        res = bdd_makenode(LEVEL(h), READREF(2), READREF(1));
      }
    }

    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = f;
    entry.b = g;
    entry.c = -h; // To explain -h, see caching note above.
    entry.res = res;

    return res;
  }

  private int bdd_replace(int r, bddPair pair) {
    CHECK(r);

    if (replacecache == null) {
      replacecache = BddCacheI_init(cachesize);
    }
    replacepair = pair.result;
    replacelast = pair.last;
    replaceid = (pair.id << 2) | CACHEID_REPLACE;

    INITREF();
    int res = replace_rec(r);
    checkresize();

    return res;
  }

  private int replace_rec(int r) {
    BddCacheDataI entry;
    int res;

    if (ISCONST(r) || LEVEL(r) > replacelast) {
      return r;
    }

    entry = BddCache_lookupI(replacecache, REPLACEHASH(replaceid, r));
    if (entry.a == r && entry.c == replaceid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    PUSHREF(replace_rec(LOW(r)));
    PUSHREF(replace_rec(HIGH(r)));

    /* Replace the root variable with the new one. Replacements at the root or in the subbdds can
     * cause the new root to be out of order. bdd_correctify builds the bdd correctly by branching
     * on the new root at the correct level of the bdd.
     */
    {
      int level = LEVEL(replacepair[LEVEL(r)]);

      /* bdd_correctify calls are cached separately from replace_rec calls. Set the cacheid for
       * the bdd_correctify calls and restore when it returns.
       */
      int tmp = replaceid;
      replaceid = (level << 2) | CACHEID_CORRECTIFY;
      res = bdd_correctify(level, READREF(2), READREF(1));
      replaceid = tmp;
    }
    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = r;
    entry.c = replaceid;
    entry.res = res;

    return res;
  }

  /**
   * This is similar to {@link JFactory#bdd_makenode} -- it returns a BDD that branches at the input
   * level with the input low and high nodes. The difference between this and bdd_makenode is that
   * bdd_makenode requires level to be strictly less than LEVEL(l) and LEVEL(r), where this does
   * not. The base case of bdd_correctify is when that is true -- then it simply delegates to
   * bdd_makenode.
   *
   * @param level The level to branch on.
   * @param l The low branch.
   * @param r The high branch.
   */
  private int bdd_correctify(int level, int l, int r) {
    int res;

    if (level < LEVEL(l) && level < LEVEL(r)) {
      return bdd_makenode(level, l, r);
    }

    if (level == LEVEL(l) || level == LEVEL(r)) {
      bdd_error(BDD_REPLACE);
      return 0;
    }

    BddCacheDataI entry = BddCache_lookupI(replacecache, CORRECTIFYHASH(replaceid, l, r));
    if (entry.a == l && entry.b == r && entry.c == replaceid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (LEVEL(l) == LEVEL(r)) {
      PUSHREF(bdd_correctify(level, LOW(l), LOW(r)));
      PUSHREF(bdd_correctify(level, HIGH(l), HIGH(r)));
      res = bdd_makenode(LEVEL(l), READREF(2), READREF(1));
    } else if (LEVEL(l) < LEVEL(r)) {
      PUSHREF(bdd_correctify(level, LOW(l), r));
      PUSHREF(bdd_correctify(level, HIGH(l), r));
      res = bdd_makenode(LEVEL(l), READREF(2), READREF(1));
    } else {
      PUSHREF(bdd_correctify(level, l, LOW(r)));
      PUSHREF(bdd_correctify(level, l, HIGH(r)));
      res = bdd_makenode(LEVEL(r), READREF(2), READREF(1));
    }
    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = l;
    entry.b = r;
    entry.c = replaceid;
    entry.res = res;

    return res;
  }

  private int bdd_apply(int l, int r, int op) {
    CHECK(l);
    CHECK(r);

    if (op < 0 || op > bddop_invimp) {
      bdd_error(BDD_OP);
      return BDDZERO;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    applyop = op;

    INITREF();
    int res;
    switch (op) {
      case bddop_and:
        res = and_rec(l, r);
        break;
      case bddop_or:
        res = or_rec(l, r);
        break;
      default:
        res = apply_rec(l, r);
        break;
    }
    checkresize();

    return res;
  }

  private int apply_rec(int l, int r) {
    BddCacheDataI entry;
    int res;

    if (VERIFY_ASSERTIONS) {
      _assert(applyop != bddop_and && applyop != bddop_or);
    }

    if (ISCONST(l) && ISCONST(r)) {
      return oprres[applyop][l << 1 | r];
    }

    switch (applyop) {
        // case bddop_and: is handled elsehwere
      case bddop_xor:
        if (l == r) {
          return BDDZERO;
        } else if (ISZERO(l)) {
          return r;
        } else if (ISZERO(r)) {
          return l;
        } else if (ISONE(l)) {
          return not_rec(r);
        } else if (ISONE(r)) {
          return not_rec(l);
        } else if (l > r) {
          // Since XOR is symmetric, maximize caching by ensuring l < r (== handled above).
          int t = l;
          l = r;
          r = t;
        }
        break;
        // case bddop_or: is handled elsehwere
      case bddop_nand:
        if (l == r) {
          return not_rec(l);
        } else if (ISZERO(l) || ISZERO(r)) {
          return BDDONE;
        } else if (ISONE(l)) {
          return not_rec(r);
        } else if (ISONE(r)) {
          return not_rec(l);
        } else if (l > r) {
          // Since NAND is symmetric, maximize caching by ensuring l < r (== handled above).
          int t = l;
          l = r;
          r = t;
        }
        break;
      case bddop_nor:
        if (l == r) {
          return not_rec(l);
        } else if (ISONE(l) || ISONE(r)) {
          return BDDZERO;
        } else if (ISZERO(l)) {
          return not_rec(r);
        } else if (ISZERO(r)) {
          return not_rec(l);
        } else if (l > r) {
          // Since NOR is symmetric, maximize caching by ensuring l < r (== handled above).
          int t = l;
          l = r;
          r = t;
        }
        break;
      case bddop_imp:
        if (l == r) {
          return BDDONE;
        } else if (ISZERO(l)) {
          return BDDONE;
        } else if (ISONE(l)) {
          return r;
        } else if (ISZERO(r)) {
          return not_rec(l);
        } else if (ISONE(r)) {
          return BDDONE;
        }
        break;
      case bddop_biimp:
        if (l == r) {
          return BDDONE;
        } else if (ISZERO(l)) {
          return not_rec(r);
        } else if (ISZERO(r)) {
          return not_rec(l);
        } else if (ISONE(l)) {
          return r;
        } else if (ISONE(r)) {
          return l;
        } else if (l > r) {
          // Since BIIMP is symmetric, maximize caching by ensuring l < r (== handled above).
          int t = l;
          l = r;
          r = t;
        }
        break;
      case bddop_diff:
        if (l == r) {
          return BDDZERO;
        } else if (ISZERO(l)) {
          return BDDZERO;
        } else if (ISONE(r)) {
          return BDDZERO;
        } else if (ISONE(l)) {
          return not_rec(r);
        } else if (ISZERO(r)) {
          return l;
        }
        break;
      case bddop_less:
        if (l == r) {
          return BDDZERO;
        } else if (ISONE(l)) {
          return BDDZERO;
        } else if (ISZERO(r)) {
          return BDDZERO;
        } else if (ISZERO(l)) {
          return r;
        } else if (ISONE(r)) {
          return not_rec(l);
        } else {
          // Rewrite as equivalent diff to improve caching.
          applyop = bddop_diff;
          int t = l;
          l = r;
          r = t;
        }
        break;
      case bddop_invimp:
        if (l == r) {
          return BDDONE;
        } else if (ISONE(l)) {
          return BDDONE;
        } else if (ISZERO(r)) {
          return BDDONE;
        } else if (ISONE(r)) {
          return l;
        } else if (ISZERO(l)) {
          return not_rec(r);
        } else {
          // Rewrite as equivalent imp to improve caching.
          applyop = bddop_imp;
          int t = l;
          l = r;
          r = t;
        }
        break;
    }

    entry = BddCache_lookupI(applycache, APPLYHASH(l, r, applyop));

    if (entry.a == l && entry.b == r && entry.c == applyop) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (LEVEL(l) == LEVEL(r)) {
      PUSHREF(apply_rec(LOW(l), LOW(r)));
      PUSHREF(apply_rec(HIGH(l), HIGH(r)));
      res = bdd_makenode(LEVEL(l), READREF(2), READREF(1));
    } else if (LEVEL(l) < LEVEL(r)) {
      PUSHREF(apply_rec(LOW(l), r));
      PUSHREF(apply_rec(HIGH(l), r));
      res = bdd_makenode(LEVEL(l), READREF(2), READREF(1));
    } else {
      PUSHREF(apply_rec(l, LOW(r)));
      PUSHREF(apply_rec(l, HIGH(r)));
      res = bdd_makenode(LEVEL(r), READREF(2), READREF(1));
    }

    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = l;
    entry.b = r;
    entry.c = applyop;
    entry.res = res;

    return res;
  }

  private int and_rec(int l, int r) {
    BddCacheDataI entry;
    int res;

    if (l == r) {
      return l;
    } else if (ISZERO(l) || ISZERO(r)) {
      return BDDZERO;
    } else if (ISONE(l)) {
      return r;
    } else if (ISONE(r)) {
      return l;
    } else if (l > r) {
      // Since AND is symmetric, maximize caching by ensuring l < r (== handled above).
      int t = l;
      l = r;
      r = t;
    }
    entry = BddCache_lookupI(applycache, APPLYHASH(l, r, bddop_and));

    if (entry.a == l && entry.b == r && entry.c == bddop_and) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (LEVEL(l) == LEVEL(r)) {
      PUSHREF(and_rec(LOW(l), LOW(r)));
      PUSHREF(and_rec(HIGH(l), HIGH(r)));
      res = bdd_makenode(LEVEL(l), READREF(2), READREF(1));
    } else if (LEVEL(l) < LEVEL(r)) {
      PUSHREF(and_rec(LOW(l), r));
      PUSHREF(and_rec(HIGH(l), r));
      res = bdd_makenode(LEVEL(l), READREF(2), READREF(1));
    } else {
      PUSHREF(and_rec(l, LOW(r)));
      PUSHREF(and_rec(l, HIGH(r)));
      res = bdd_makenode(LEVEL(r), READREF(2), READREF(1));
    }

    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = l;
    entry.b = r;
    entry.c = bddop_and;
    entry.res = res;

    return res;
  }

  /**
   * Dedup a sorted array. Returns the input array if it contains no duplicates. Mutates the array
   * if there are duplicates.
   */
  static int[] dedupSorted(int[] values) {
    if (values.length < 2) {
      return values;
    }
    int i = 0; // index last written to
    int j = 1; // index to read from next
    while (j < values.length) {
      if (values[i] != values[j]) {
        values[++i] = values[j++];
      } else {
        j++;
      }
    }

    int dedupLen = i + 1;
    if (dedupLen < values.length) {
      return Arrays.copyOf(values, dedupLen);
    } else {
      return values;
    }
  }

  private int orAll_rec(int[] operands) {
    if (operands.length == 0) {
      return BDDZERO;
    } else if (operands.length == 1) {
      return operands[0];
    } else if (operands.length == 2) {
      return or_rec(operands[0], operands[1]);
    }

    // sort and dedup the operands to optimize caching
    Arrays.sort(operands);
    operands = dedupSorted(operands);

    MultiOpBddCacheData entry =
        BddCache_lookupMultiOp(multiopcache, MULTIOPHASH(operands, bddop_or));
    if (entry.a == bddop_or && Arrays.equals(operands, entry.operands)) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.b;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    /* Compute the result in a way that generalizes or_rec. Identify the variable to branch on, and
     * make two recursive calls (for when that variable is high or low).
     *
     * In a single pass over operands:
     * 1. Find the level of the variable the result BDD should branch on. This is the minimum level
     *    branched on at the roots of the current operand BDDs.
     * 2. Compute the size needed for the operand arrays of the two recursive calls. This is equal
     *    to the number of operands whose root level are greater than the minimum, plus the number
     *    of operands whose root level is equal to the minimum and whose child (low or high,
     *    corresponding to if the recursive call is computing the low or high child of the result)
     *    is not the zero BDD.
     * 3. Whether either recursive call can be short-circuited because one of the operands is the
     *    one BDD. This can only happen when the one is a child a BDD whose root level is the
     *    minimum.
     */

    int minLevel = LEVEL(operands[0]);
    int nodesWithMinLevel = 0;
    int nodesWithMinLevelLowNonZero = 0;
    int nodesWithMinLevelHighNonZero = 0;
    boolean nodeWithMinLevelHasLowOne = false;
    boolean nodeWithMinLevelHasHighOne = false;
    for (int n : operands) {
      int level = LEVEL(n);
      if (level < minLevel) {
        minLevel = level;
        nodesWithMinLevel = 0;
        nodesWithMinLevelHighNonZero = 0;
        nodesWithMinLevelLowNonZero = 0;
        nodeWithMinLevelHasHighOne = false;
        nodeWithMinLevelHasLowOne = false;
      } else if (level > minLevel) {
        continue;
      }

      // level == minLevel
      nodesWithMinLevel++;

      int high = HIGH(n);
      nodeWithMinLevelHasHighOne |= ISONE(high);
      nodesWithMinLevelHighNonZero += ISZERO(high) ? 0 : 1;

      int low = LOW(n);
      nodeWithMinLevelHasLowOne |= ISONE(low);
      nodesWithMinLevelLowNonZero += ISZERO(low) ? 0 : 1;
    }

    int nodesWithoutMinLevel = operands.length - nodesWithMinLevel;

    int low;
    if (!nodeWithMinLevelHasLowOne) {
      /* Make the resursive call for the low branch. None of the operands are 1, so we can't
       * short-circuit to 1. Allocate and build the array of operands, then make the call and push
       * the result onto the stack.
       */
      int[] lowOperands = new int[nodesWithMinLevelLowNonZero + nodesWithoutMinLevel];
      int i = 0;
      for (int operand : operands) {
        if (LEVEL(operand) == minLevel) {
          int l = LOW(operand);
          if (!ISZERO(l)) {
            assert !ISCONST(l);
            lowOperands[i++] = l;
          }
        } else {
          assert !ISCONST(operand);
          lowOperands[i++] = operand;
        }
      }
      assert i == lowOperands.length;
      low = orAll_rec(lowOperands);
      PUSHREF(low); // make sure low isn't garbage collected.
    } else {
      low = BDDONE;
    }

    int high;
    if (!nodeWithMinLevelHasHighOne) {
      /* Make the resursive call for the high branch. None of the operands are 1, so we can't
       * short-circuit to 1. Allocate and build the array of operands, then make the call and push
       * the result onto the stack.
       */
      int[] highOperands = new int[nodesWithMinLevelHighNonZero + nodesWithoutMinLevel];
      int i = 0;
      for (int operand : operands) {
        if (LEVEL(operand) == minLevel) {
          int h = HIGH(operand);
          if (!ISZERO(h)) {
            assert !ISCONST(h);
            highOperands[i++] = h;
          }
        } else {
          assert !ISCONST(operand);
          highOperands[i++] = operand;
        }
      }
      assert i == highOperands.length;
      high = orAll_rec(highOperands);
      PUSHREF(high); // make sure high isn't garbage collected.
    } else {
      high = BDDONE;
    }

    int res = bdd_makenode(minLevel, low, high);

    if (!nodeWithMinLevelHasHighOne) {
      POPREF(1);
    }
    if (!nodeWithMinLevelHasLowOne) {
      POPREF(1);
    }

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = bddop_or;
    entry.b = res;
    entry.operands = operands;
    return res;
  }

  private int or_rec(int l, int r) {
    BddCacheDataI entry;
    int res;

    if (l == r) {
      return l;
    } else if (ISONE(l) || ISONE(r)) {
      return BDDONE;
    } else if (ISZERO(l)) {
      return r;
    } else if (ISZERO(r)) {
      return l;
    } else if (l > r) {
      // Since OR is symmetric, maximize caching by ensuring l < r (== handled above).
      int t = l;
      l = r;
      r = t;
    }
    entry = BddCache_lookupI(applycache, APPLYHASH(l, r, bddop_or));

    if (entry.a == l && entry.b == r && entry.c == bddop_or) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (LEVEL(l) == LEVEL(r)) {
      PUSHREF(or_rec(LOW(l), LOW(r)));
      PUSHREF(or_rec(HIGH(l), HIGH(r)));
      res = bdd_makenode(LEVEL(l), READREF(2), READREF(1));
    } else if (LEVEL(l) < LEVEL(r)) {
      PUSHREF(or_rec(LOW(l), r));
      PUSHREF(or_rec(HIGH(l), r));
      res = bdd_makenode(LEVEL(l), READREF(2), READREF(1));
    } else {
      PUSHREF(or_rec(l, LOW(r)));
      PUSHREF(or_rec(l, HIGH(r)));
      res = bdd_makenode(LEVEL(r), READREF(2), READREF(1));
    }

    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = l;
    entry.b = r;
    entry.c = bddop_or;
    entry.res = res;

    return res;
  }

  private int relprod_rec(int l, int r) {
    BddCacheDataI entry;
    int res;

    if (l == BDDZERO || r == BDDZERO) {
      return BDDZERO;
    } else if (l == r) {
      return quant_rec(l);
    } else if (l == BDDONE) {
      return quant_rec(r);
    } else if (r == BDDONE) {
      return quant_rec(l);
    }

    int LEVEL_l = LEVEL(l);
    int LEVEL_r = LEVEL(r);
    if (LEVEL_l > quantlast && LEVEL_r > quantlast) {
      applyop = bddop_and;
      res = and_rec(l, r);
      applyop = bddop_or;
    } else {
      entry = BddCache_lookupI(appexcache, APPEXHASH(l, r, bddop_and));
      if (entry.a == l && entry.b == r && entry.c == appexid) {
        if (CACHESTATS) {
          cachestats.opHit++;
        }
        return entry.res;
      }
      if (CACHESTATS) {
        cachestats.opMiss++;
      }

      if (LEVEL_l == LEVEL_r) {
        PUSHREF(relprod_rec(LOW(l), LOW(r)));
        PUSHREF(relprod_rec(HIGH(l), HIGH(r)));
        if (INVARSET(LEVEL_l)) {
          res = or_rec(READREF(2), READREF(1));
        } else {
          res = bdd_makenode(LEVEL_l, READREF(2), READREF(1));
        }
      } else if (LEVEL_l < LEVEL_r) {
        PUSHREF(relprod_rec(LOW(l), r));
        PUSHREF(relprod_rec(HIGH(l), r));
        if (INVARSET(LEVEL_l)) {
          res = or_rec(READREF(2), READREF(1));
        } else {
          res = bdd_makenode(LEVEL_l, READREF(2), READREF(1));
        }
      } else {
        PUSHREF(relprod_rec(l, LOW(r)));
        PUSHREF(relprod_rec(l, HIGH(r)));
        if (INVARSET(LEVEL_r)) {
          res = or_rec(READREF(2), READREF(1));
        } else {
          res = bdd_makenode(LEVEL_r, READREF(2), READREF(1));
        }
      }

      POPREF(2);

      if (CACHESTATS && entry.a != -1) {
        cachestats.opOverwrite++;
      }
      entry.a = l;
      entry.b = r;
      entry.c = appexid;
      entry.res = res;
    }

    return res;
  }

  private int bdd_relprod(int a, int b, int var) {
    return bdd_appex(a, b, bddop_and, var);
  }

  private int bdd_appex(int l, int r, int opr, int var) {
    CHECK(l);
    CHECK(r);
    CHECK(var);

    if (opr < 0 || opr > bddop_invimp) {
      bdd_error(BDD_OP);
      return BDDZERO;
    }

    if (var < 2) /* Empty set */ {
      return bdd_apply(l, r, opr);
    }
    if (varset2vartable(var) < 0) {
      return BDDZERO;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (appexcache == null) {
      appexcache = BddCacheI_init(cachesize);
    }
    if (quantcache == null) {
      quantcache = BddCacheI_init(cachesize);
    }
    applyop = bddop_or;
    appexop = opr;
    appexid = (var << 5) | (appexop << 1); /* FIXME: range! */
    quantid = (appexid << 3) | CACHEID_APPEX;

    INITREF();
    int res = opr == bddop_and ? relprod_rec(l, r) : appquant_rec(l, r);
    checkresize();

    return res;
  }

  private int varset2vartable(int r) {
    if (r < 2) {
      return bdd_error(BDD_VARSET);
    }

    quantvarsetID++;

    if (quantvarsetID == INT_MAX) {
      for (int i = 0; i < bddvarnum; ++i) {
        quantvarset[i] = 0;
      }
      quantvarsetID = 1;
    }

    quantlast = -1;
    for (int n = r; n > 1; n = HIGH(n)) {
      quantvarset[LEVEL(n)] = quantvarsetID;
      if (VERIFY_ASSERTIONS) {
        _assert(quantlast < LEVEL(n));
      }
      quantlast = LEVEL(n);
    }

    return 0;
  }

  private static final int INT_MAX = Integer.MAX_VALUE;

  private int varset2svartable(int r) {
    if (r < 2) {
      return bdd_error(BDD_VARSET);
    }

    quantvarsetID++;

    if (quantvarsetID == INT_MAX / 2) {
      for (int i = 0; i < bddvarnum; ++i) {
        quantvarset[i] = 0;
      }
      quantvarsetID = 1;
    }

    quantlast = 0;
    for (int n = r; !ISCONST(n); ) {
      if (ISZERO(LOW(n))) {
        quantvarset[LEVEL(n)] = quantvarsetID;
        n = HIGH(n);
      } else {
        quantvarset[LEVEL(n)] = -quantvarsetID;
        n = LOW(n);
      }
      if (VERIFY_ASSERTIONS) {
        _assert(quantlast < LEVEL(n));
      }
      quantlast = LEVEL(n);
    }

    return 0;
  }

  private int appquant_rec(int l, int r) {
    BddCacheDataI entry;
    int res;

    if (VERIFY_ASSERTIONS) {
      _assert(appexop != bddop_and);
    }

    switch (appexop) {
      case bddop_or:
        if (l == BDDONE || r == BDDONE) {
          return BDDONE;
        } else if (l == r) {
          return quant_rec(l);
        } else if (l == BDDZERO) {
          return quant_rec(r);
        } else if (r == BDDZERO) {
          return quant_rec(l);
        }
        break;
      case bddop_xor:
        if (l == r) {
          return BDDZERO;
        } else if (l == BDDZERO) {
          return quant_rec(r);
        } else if (r == BDDZERO) {
          return quant_rec(l);
        }
        break;
      case bddop_nand:
        if (l == BDDZERO || r == BDDZERO) {
          return BDDONE;
        }
        break;
      case bddop_nor:
        if (l == BDDONE || r == BDDONE) {
          return BDDZERO;
        }
        break;
    }

    if (ISCONST(l) && ISCONST(r)) {
      res = oprres[appexop][(l << 1) | r];
    } else if (LEVEL(l) > quantlast && LEVEL(r) > quantlast) {
      int oldop = applyop;
      applyop = appexop;
      switch (applyop) {
        case bddop_and:
          res = and_rec(l, r);
          break;
        case bddop_or:
          res = or_rec(l, r);
          break;
        default:
          res = apply_rec(l, r);
          break;
      }
      applyop = oldop;
    } else {
      entry = BddCache_lookupI(appexcache, APPEXHASH(l, r, appexop));
      if (entry.a == l && entry.b == r && entry.c == appexid) {
        if (CACHESTATS) {
          cachestats.opHit++;
        }
        return entry.res;
      }
      if (CACHESTATS) {
        cachestats.opMiss++;
      }

      int lev;
      if (LEVEL(l) == LEVEL(r)) {
        PUSHREF(appquant_rec(LOW(l), LOW(r)));
        PUSHREF(appquant_rec(HIGH(l), HIGH(r)));
        lev = LEVEL(l);
      } else if (LEVEL(l) < LEVEL(r)) {
        PUSHREF(appquant_rec(LOW(l), r));
        PUSHREF(appquant_rec(HIGH(l), r));
        lev = LEVEL(l);
      } else {
        PUSHREF(appquant_rec(l, LOW(r)));
        PUSHREF(appquant_rec(l, HIGH(r)));
        lev = LEVEL(r);
      }
      if (INVARSET(lev)) {
        int r2 = READREF(2), r1 = READREF(1);
        switch (applyop) {
          case bddop_and:
            res = and_rec(r2, r1);
            break;
          case bddop_or:
            res = or_rec(r2, r1);
            break;
          default:
            res = apply_rec(r2, r1);
            break;
        }
      } else {
        res = bdd_makenode(lev, READREF(2), READREF(1));
      }

      POPREF(2);

      if (CACHESTATS && entry.a != -1) {
        cachestats.opOverwrite++;
      }
      entry.a = l;
      entry.b = r;
      entry.c = appexid;
      entry.res = res;
    }

    return res;
  }

  private int appuni_rec(int l, int r, int var) {
    BddCacheDataI entry;
    int res;

    int LEVEL_l, LEVEL_r, LEVEL_var;
    LEVEL_l = LEVEL(l);
    LEVEL_r = LEVEL(r);
    LEVEL_var = LEVEL(var);

    if (LEVEL_l > LEVEL_var && LEVEL_r > LEVEL_var) {
      // Skipped a quantified node, answer is zero.
      return BDDZERO;
    }

    if (ISCONST(l) && ISCONST(r)) {
      res = oprres[appexop][(l << 1) | r];
    } else if (ISCONST(var)) {
      int oldop = applyop;
      applyop = appexop;
      switch (applyop) {
        case bddop_and:
          res = and_rec(l, r);
          break;
        case bddop_or:
          res = or_rec(l, r);
          break;
        default:
          res = apply_rec(l, r);
          break;
      }
      applyop = oldop;
    } else {
      entry = BddCache_lookupI(appexcache, APPEXHASH(l, r, appexop));
      if (entry.a == l && entry.b == r && entry.c == appexid) {
        if (CACHESTATS) {
          cachestats.opHit++;
        }
        return entry.res;
      }
      if (CACHESTATS) {
        cachestats.opMiss++;
      }

      int lev;
      if (LEVEL_l == LEVEL_r) {
        if (LEVEL_l == LEVEL_var) {
          lev = -1;
          var = HIGH(var);
        } else {
          lev = LEVEL_l;
        }
        PUSHREF(appuni_rec(LOW(l), LOW(r), var));
        PUSHREF(appuni_rec(HIGH(l), HIGH(r), var));
        lev = LEVEL_l;
      } else if (LEVEL_l < LEVEL_r) {
        if (LEVEL_l == LEVEL_var) {
          lev = -1;
          var = HIGH(var);
        } else {
          lev = LEVEL_l;
        }
        PUSHREF(appuni_rec(LOW(l), r, var));
        PUSHREF(appuni_rec(HIGH(l), r, var));
      } else {
        if (LEVEL_r == LEVEL_var) {
          lev = -1;
          var = HIGH(var);
        } else {
          lev = LEVEL_r;
        }
        PUSHREF(appuni_rec(l, LOW(r), var));
        PUSHREF(appuni_rec(l, HIGH(r), var));
      }
      if (lev == -1) {
        int r2 = READREF(2), r1 = READREF(1);
        switch (applyop) {
          case bddop_and:
            res = and_rec(r2, r1);
            break;
          case bddop_or:
            res = or_rec(r2, r1);
            break;
          default:
            res = apply_rec(r2, r1);
            break;
        }
      } else {
        res = bdd_makenode(lev, READREF(2), READREF(1));
      }

      POPREF(2);

      if (CACHESTATS && entry.a != -1) {
        cachestats.opOverwrite++;
      }
      entry.a = l;
      entry.b = r;
      entry.c = appexid;
      entry.res = res;
    }

    return res;
  }

  private int unique_rec(int r, int q) {
    BddCacheDataI entry;
    int res;
    int LEVEL_r, LEVEL_q;

    LEVEL_r = LEVEL(r);
    LEVEL_q = LEVEL(q);
    if (LEVEL_r > LEVEL_q) {
      // Skipped a quantified node, answer is zero.
      return BDDZERO;
    }

    if (r < 2 || q < 2) {
      return r;
    }

    entry = BddCache_lookupI(quantcache, QUANTHASH(r));
    if (entry.a == r && entry.c == quantid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (LEVEL_r == LEVEL_q) {
      PUSHREF(unique_rec(LOW(r), HIGH(q)));
      PUSHREF(unique_rec(HIGH(r), HIGH(q)));
      res = apply_rec(READREF(2), READREF(1));
    } else {
      PUSHREF(unique_rec(LOW(r), q));
      PUSHREF(unique_rec(HIGH(r), q));
      res = bdd_makenode(LEVEL(r), READREF(2), READREF(1));
    }

    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = r;
    entry.c = quantid;
    entry.res = res;

    return res;
  }

  private int quant_rec(int r) {
    BddCacheDataI entry;
    int res;

    if (r < 2 || LEVEL(r) > quantlast) {
      return r;
    }

    entry = BddCache_lookupI(quantcache, QUANTHASH(r));
    if (entry.a == r && entry.c == quantid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (INVARSET(LEVEL(r))) {
      // The current node tests a variable that is to be quantified away. Commonly, the range of
      // variables to be quantified away is continuous, so it is relatively likely that
      // LOW(r) or HIGH(r) is also a variable to be quantified away. When we have multi-BDD
      // operations and it is correct to use them, collect all not-to-be-quantified-away
      // ("remaining") children of this node.
      if (applyop == bddop_or) {
        HashSet<Integer> remainingChildren = new HashSet<>();
        LinkedList<Integer> toInvestigate = new LinkedList<>();
        toInvestigate.add(LOW(r));
        toInvestigate.add(HIGH(r));

        while (!toInvestigate.isEmpty()) {
          int node = toInvestigate.removeFirst();
          int level = LEVEL(node);
          if (level <= quantlast && INVARSET(level)) {
            // `node` is also to be quantified away. See if that's also true for its children.
            toInvestigate.add(LOW(node));
            toInvestigate.add(HIGH(node));
          } else if (ISONE(node)) {
            // Short-circuit orAll(...ONE...) = ONE.
            remainingChildren.clear();
            remainingChildren.add(BDDONE);
            break;
          } else if (!ISZERO(node)) {
            // Do not include ZERO nodes in the remainingChildren set.
            remainingChildren.add(node);
          }
        }
        res = orAll_rec(Ints.toArray(remainingChildren));
      } else {
        int left = PUSHREF(quant_rec(LOW(r)));
        int right = PUSHREF(quant_rec(HIGH(r)));
        switch (applyop) {
          case bddop_and:
            res = and_rec(left, right);
            break;
          default:
            res = apply_rec(left, right);
            break;
        }
        POPREF(2);
      }
    } else {
      int left = PUSHREF(quant_rec(LOW(r)));
      int right = PUSHREF(quant_rec(HIGH(r)));
      res = bdd_makenode(LEVEL(r), left, right);
      POPREF(2);
    }

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = r;
    entry.c = quantid;
    entry.res = res;

    return res;
  }

  private int bdd_constrain(int f, int c) {
    CHECK(f);
    CHECK(c);

    if (misccache == null) {
      misccache = BddCacheI_init(cachesize);
    }
    miscid = CACHEID_CONSTRAIN;

    INITREF();
    int res = constrain_rec(f, c);
    checkresize();

    return res;
  }

  private int constrain_rec(int f, int c) {
    BddCacheDataI entry;
    int res;

    if (ISONE(c)) {
      return f;
    } else if (ISCONST(f)) {
      return f;
    } else if (c == f) {
      return BDDONE;
    } else if (ISZERO(c)) {
      return BDDZERO;
    }

    entry = BddCache_lookupI(misccache, CONSTRAINHASH(f, c));
    if (entry.a == f && entry.b == c && entry.c == miscid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (LEVEL(f) == LEVEL(c)) {
      if (ISZERO(LOW(c))) {
        res = constrain_rec(HIGH(f), HIGH(c));
      } else if (ISZERO(HIGH(c))) {
        res = constrain_rec(LOW(f), LOW(c));
      } else {
        PUSHREF(constrain_rec(LOW(f), LOW(c)));
        PUSHREF(constrain_rec(HIGH(f), HIGH(c)));
        res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
        POPREF(2);
      }
    } else if (LEVEL(f) < LEVEL(c)) {
      PUSHREF(constrain_rec(LOW(f), c));
      PUSHREF(constrain_rec(HIGH(f), c));
      res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
      POPREF(2);
    } else {
      if (ISZERO(LOW(c))) {
        res = constrain_rec(f, HIGH(c));
      } else if (ISZERO(HIGH(c))) {
        res = constrain_rec(f, LOW(c));
      } else {
        PUSHREF(constrain_rec(f, LOW(c)));
        PUSHREF(constrain_rec(f, HIGH(c)));
        res = bdd_makenode(LEVEL(c), READREF(2), READREF(1));
        POPREF(2);
      }
    }

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = f;
    entry.b = c;
    entry.c = miscid;
    entry.res = res;

    return res;
  }

  private int bdd_compose(int f, int g, int var) {
    CHECK(f);
    CHECK(g);
    if (var < 0 || var >= bddvarnum) {
      bdd_error(BDD_VAR);
      return BDDZERO;
    }

    if (replacecache == null) {
      // compose_rec uses replacecache
      replacecache = BddCacheI_init(cachesize);
    }
    if (applycache == null) {
      // compose_rec can call ite_rec, which uses applycache
      applycache = BddCacheI_init(cachesize);
    }
    composelevel = bddvar2level[var];
    replaceid = (composelevel << 2) | CACHEID_COMPOSE;

    INITREF();
    int res = compose_rec(f, g);
    checkresize();
    return res;
  }

  private int compose_rec(int f, int g) {
    BddCacheDataI entry;
    int res;

    if (LEVEL(f) > composelevel) {
      return f;
    }

    entry = BddCache_lookupI(replacecache, COMPOSEHASH(replaceid, f, g));
    if (entry.a == f && entry.b == g && entry.c == replaceid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (LEVEL(f) < composelevel) {
      if (LEVEL(f) == LEVEL(g)) {
        PUSHREF(compose_rec(LOW(f), LOW(g)));
        PUSHREF(compose_rec(HIGH(f), HIGH(g)));
        res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
      } else if (LEVEL(f) < LEVEL(g)) {
        PUSHREF(compose_rec(LOW(f), g));
        PUSHREF(compose_rec(HIGH(f), g));
        res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
      } else {
        PUSHREF(compose_rec(f, LOW(g)));
        PUSHREF(compose_rec(f, HIGH(g)));
        res = bdd_makenode(LEVEL(g), READREF(2), READREF(1));
      }
      POPREF(2);
    } else
    /*if (LEVEL(f) == composelevel) changed 2-nov-98 */ {
      res = ite_rec(g, HIGH(f), LOW(f));
    }

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = f;
    entry.b = g;
    entry.c = replaceid;
    entry.res = res;

    return res;
  }

  private int bdd_veccompose(int f, bddPair pair) {
    CHECK(f);

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (replacecache == null) {
      replacecache = BddCacheI_init(cachesize);
    }
    replacepair = pair.result;
    replaceid = (pair.id << 2) | CACHEID_VECCOMPOSE;
    replacelast = pair.last;

    INITREF();
    int res = veccompose_rec(f);
    checkresize();

    return res;
  }

  private int veccompose_rec(int f) {
    BddCacheDataI entry;
    int res;

    if (LEVEL(f) > replacelast) {
      return f;
    }

    entry = BddCache_lookupI(replacecache, VECCOMPOSEHASH(replaceid, f));
    if (entry.a == f && entry.c == replaceid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    PUSHREF(veccompose_rec(LOW(f)));
    PUSHREF(veccompose_rec(HIGH(f)));
    res = ite_rec(replacepair[LEVEL(f)], READREF(1), READREF(2));
    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = f;
    entry.c = replaceid;
    entry.res = res;

    return res;
  }

  private int bdd_exist(int r, int var) {
    CHECK(r);
    CHECK(var);

    if (var < 2) /* Empty set */ {
      return r;
    }
    if (varset2vartable(var) < 0) {
      return BDDZERO;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (quantcache == null) {
      quantcache = BddCacheI_init(cachesize);
    }
    if (multiopcache == null) {
      multiopcache = BddCacheMultiOp_init(cachesize);
    }
    applyop = bddop_or;
    quantid = (var << 3) | CACHEID_EXIST; /* FIXME: range */

    INITREF();
    int res = quant_rec(r);
    checkresize();

    return res;
  }

  private int bdd_forall(int r, int var) {
    CHECK(r);
    CHECK(var);

    if (var < 2) /* Empty set */ {
      return r;
    }
    if (varset2vartable(var) < 0) {
      return BDDZERO;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (quantcache == null) {
      quantcache = BddCacheI_init(cachesize);
    }
    quantid = (var << 3) | CACHEID_FORALL;
    applyop = bddop_and;

    INITREF();
    int res = quant_rec(r);
    checkresize();

    return res;
  }

  private int bdd_unique(int r, int var) {
    CHECK(r);
    CHECK(var);

    if (var < 2) /* Empty set */ {
      return r;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (quantcache == null) {
      quantcache = BddCacheI_init(cachesize);
    }
    quantid = (var << 3) | CACHEID_UNIQUE;
    applyop = bddop_xor;

    INITREF();
    int res = unique_rec(r, var);
    checkresize();

    return res;
  }

  private int bdd_restrict(int r, int var) {
    CHECK(r);
    CHECK(var);

    if (var < 2) /* Empty set */ {
      return r;
    }
    if (varset2svartable(var) < 0) {
      return BDDZERO;
    }

    if (misccache == null) {
      misccache = BddCacheI_init(cachesize);
    }
    miscid = (var << 3) | CACHEID_RESTRICT;

    INITREF();
    int res = restrict_rec(r);
    checkresize();

    return res;
  }

  private int restrict_rec(int r) {
    BddCacheDataI entry;
    int res;

    if (ISCONST(r) || LEVEL(r) > quantlast) {
      return r;
    }

    entry = BddCache_lookupI(misccache, RESTRHASH(r, miscid));
    if (entry.a == r && entry.c == miscid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (INSVARSET(LEVEL(r))) {
      if (quantvarset[LEVEL(r)] > 0) {
        res = restrict_rec(HIGH(r));
      } else {
        res = restrict_rec(LOW(r));
      }
    } else {
      PUSHREF(restrict_rec(LOW(r)));
      PUSHREF(restrict_rec(HIGH(r)));
      res = bdd_makenode(LEVEL(r), READREF(2), READREF(1));
      POPREF(2);
    }

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = r;
    entry.c = miscid;
    entry.res = res;

    return res;
  }

  private int bdd_simplify(int f, int d) {
    CHECK(f);
    CHECK(d);

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    applyop = bddop_or;

    INITREF();
    int res = simplify_rec(f, d);
    checkresize();

    return res;
  }

  private int simplify_rec(int f, int d) {
    BddCacheDataI entry;
    int res;

    if (ISONE(d) || ISCONST(f)) {
      return f;
    } else if (d == f) {
      return BDDONE;
    } else if (ISZERO(d)) {
      return BDDZERO;
    }

    entry = BddCache_lookupI(applycache, APPLYHASH(f, d, bddop_simplify));

    if (entry.a == f && entry.b == d && entry.c == bddop_simplify) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.res;
    }
    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    if (LEVEL(f) == LEVEL(d)) {
      if (ISZERO(LOW(d))) {
        res = simplify_rec(HIGH(f), HIGH(d));
      } else if (ISZERO(HIGH(d))) {
        res = simplify_rec(LOW(f), LOW(d));
      } else {
        PUSHREF(simplify_rec(LOW(f), LOW(d)));
        PUSHREF(simplify_rec(HIGH(f), HIGH(d)));
        res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
        POPREF(2);
      }
    } else if (LEVEL(f) < LEVEL(d)) {
      PUSHREF(simplify_rec(LOW(f), d));
      PUSHREF(simplify_rec(HIGH(f), d));
      res = bdd_makenode(LEVEL(f), READREF(2), READREF(1));
      POPREF(2);
    } else /* LEVEL(d) < LEVEL(f) */ {
      PUSHREF(or_rec(LOW(d), HIGH(d))); /* Exist quant */
      res = simplify_rec(f, READREF(1));
      POPREF(1);
    }

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = f;
    entry.b = d;
    entry.c = bddop_simplify;
    entry.res = res;

    return res;
  }

  private int bdd_support(int r) {
    int res = 1;

    CHECK(r);

    if (r < 2) {
      return BDDONE;
    }

    /* On-demand allocation of support set */
    if (supportSet.length < bddvarnum) {
      supportSet = new int[bddvarnum];
      supportID = 0;
    }

    /* Update global variables used to speed up bdd_support()
     * - instead of always memsetting support to zero, we use
     *   a change counter.
     * - and instead of reading the whole array afterwards, we just
     *   look from 'min' to 'max' used BDD variables.
     */
    if (supportID == 0x0FFFFFFF) {
      /* We probably don't get here -- but let's just be sure */
      for (int i = 0; i < bddvarnum; ++i) {
        supportSet[i] = 0;
      }
      supportID = 0;
    }
    ++supportID;
    supportMin = LEVEL(r);
    supportMax = supportMin;

    support_rec(r, supportSet);
    bdd_unmark(r);

    for (int n = supportMax; n >= supportMin; --n) {
      if (supportSet[n] == supportID) {
        int tmp;
        bdd_addref(res);
        tmp = bdd_makenode(n, BDDZERO, res);
        bdd_delref(res);
        res = tmp;
      }
    }

    return res;
  }

  private void support_rec(int r, int[] support) {

    if (r < 2) {
      return;
    }

    if (MARKED(r) || LOW(r) == INVALID_BDD) {
      return;
    }

    support[LEVEL(r)] = supportID;

    if (LEVEL(r) > supportMax) {
      supportMax = LEVEL(r);
    }

    SETMARK(r);

    support_rec(LOW(r), support);
    support_rec(HIGH(r), support);
  }

  private int bdd_appall(int l, int r, int opr, int var) {
    CHECK(l);
    CHECK(r);
    CHECK(var);

    if (opr < 0 || opr > bddop_invimp) {
      bdd_error(BDD_OP);
      return BDDZERO;
    }

    if (var < 2) /* Empty set */ {
      return bdd_apply(l, r, opr);
    }
    if (varset2vartable(var) < 0) {
      return BDDZERO;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (appexcache == null) {
      appexcache = BddCacheI_init(cachesize);
    }
    if (quantcache == null) {
      quantcache = BddCacheI_init(cachesize);
    }
    applyop = bddop_and;
    appexop = opr;
    appexid = (var << 5) | (appexop << 1) | 1; /* FIXME: range! */
    quantid = (appexid << 3) | CACHEID_APPAL;

    INITREF();
    int res = appquant_rec(l, r);
    checkresize();

    return res;
  }

  private int bdd_appuni(int l, int r, int opr, int var) {
    CHECK(l);
    CHECK(r);
    CHECK(var);

    if (opr < 0 || opr > bddop_invimp) {
      bdd_error(BDD_OP);
      return BDDZERO;
    }

    if (var < 2) /* Empty set */ {
      return bdd_apply(l, r, opr);
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (appexcache == null) {
      appexcache = BddCacheI_init(cachesize);
    }
    if (quantcache == null) {
      quantcache = BddCacheI_init(cachesize);
    }
    applyop = bddop_xor;
    appexop = opr;
    appexid = (var << 5) | (appexop << 1) | 1; /* FIXME: range! */
    quantid = (appexid << 3) | CACHEID_APPUN;

    INITREF();
    int res = appuni_rec(l, r, var);
    checkresize();

    return res;
  }

  private int bdd_satone(int r) {
    int res;

    CHECK(r);
    if (r < 2) {
      return r;
    }

    INITREF();
    res = satone_rec(r);

    checkresize();
    return res;
  }

  private int satone_rec(int r) {
    if (ISCONST(r)) {
      return r;
    }

    if (ISZERO(LOW(r))) {
      int res = satone_rec(HIGH(r));
      int m = bdd_makenode(LEVEL(r), BDDZERO, res);
      PUSHREF(m);
      return m;
    } else {
      int res = satone_rec(LOW(r));
      int m = bdd_makenode(LEVEL(r), res, BDDZERO);
      PUSHREF(m);
      return m;
    }
  }

  private int bdd_satoneset(int r, int var, int pol) {
    int res;

    CHECK(r);
    if (ISZERO(r)) {
      return r;
    }
    if (!ISCONST(pol)) {
      bdd_error(BDD_ILLBDD);
      return BDDZERO;
    }

    INITREF();
    satPolarity = pol;
    res = satoneset_rec(r, var);

    checkresize();
    return res;
  }

  private int satoneset_rec(int r, int var) {
    if (ISCONST(r) && ISCONST(var)) {
      return r;
    }

    if (LEVEL(r) < LEVEL(var)) {
      if (ISZERO(LOW(r))) {
        int res = satoneset_rec(HIGH(r), var);
        int m = bdd_makenode(LEVEL(r), BDDZERO, res);
        PUSHREF(m);
        return m;
      } else {
        int res = satoneset_rec(LOW(r), var);
        int m = bdd_makenode(LEVEL(r), res, BDDZERO);
        PUSHREF(m);
        return m;
      }
    } else if (LEVEL(var) < LEVEL(r)) {
      int res = satoneset_rec(r, HIGH(var));
      if (satPolarity == BDDONE) {
        int m = bdd_makenode(LEVEL(var), BDDZERO, res);
        PUSHREF(m);
        return m;
      } else {
        int m = bdd_makenode(LEVEL(var), res, BDDZERO);
        PUSHREF(m);
        return m;
      }
    } else /* LEVEL(r) == LEVEL(var) */ {
      if (ISZERO(LOW(r))) {
        int res = satoneset_rec(HIGH(r), HIGH(var));
        int m = bdd_makenode(LEVEL(r), BDDZERO, res);
        PUSHREF(m);
        return m;
      } else {
        int res = satoneset_rec(LOW(r), HIGH(var));
        int m = bdd_makenode(LEVEL(r), res, BDDZERO);
        PUSHREF(m);
        return m;
      }
    }
  }

  private int bdd_fullsatone(int r) {
    int res;

    CHECK(r);
    if (r == BDDZERO) {
      return 0;
    }

    INITREF();
    res = fullsatone_rec(r);

    for (int v = LEVEL(r) - 1; v >= 0; v--) {
      res = PUSHREF(bdd_makenode(v, res, BDDZERO));
    }

    checkresize();
    return res;
  }

  private int fullsatone_rec(int r) {
    if (r < 2) {
      return r;
    }

    if (LOW(r) != BDDZERO) {
      int res = fullsatone_rec(LOW(r));

      for (int v = LEVEL(LOW(r)) - 1; v > LEVEL(r); v--) {
        res = PUSHREF(bdd_makenode(v, res, BDDZERO));
      }

      return PUSHREF(bdd_makenode(LEVEL(r), res, BDDZERO));
    } else {
      int res = fullsatone_rec(HIGH(r));

      for (int v = LEVEL(HIGH(r)) - 1; v > LEVEL(r); v--) {
        res = PUSHREF(bdd_makenode(v, res, BDDZERO));
      }

      return PUSHREF(bdd_makenode(LEVEL(r), BDDZERO, res));
    }
  }

  private void bdd_gbc_rehash() {
    bddfreepos = 0;
    bddfreenum = 0;

    for (int n = bddnodesize - 1; n >= 2; n--) {
      if (LOW(n) != INVALID_BDD) {
        int hash2;

        hash2 = NODEHASH(LEVEL(n), LOW(n), HIGH(n));
        SETNEXT(n, HASH(hash2));
        SETHASH(hash2, n);
      } else {
        SETNEXT(n, bddfreepos);
        bddfreepos = n;
        bddfreenum++;
      }
    }
  }

  private void INITREF() {
    bddrefstacktop = 0;
  }

  private int PUSHREF(int a) {
    bddrefstack[bddrefstacktop++] = a;
    return a;
  }

  private int READREF(int a) {
    return bddrefstack[bddrefstacktop - a];
  }

  private void POPREF(int a) {
    bddrefstacktop -= a;
  }

  private int bdd_nodecount(int r) {
    int[] num = new int[1];

    CHECK(r);

    bdd_markcount(r, num);
    bdd_unmark(r);

    return num[0];
  }

  private int bdd_anodecount(int[] r) {
    int[] cou = new int[1];

    for (int i : r) {
      bdd_markcount(i, cou);
    }

    for (int i : r) {
      bdd_unmark(i);
    }

    return cou[0];
  }

  private int[] bdd_varprofile(int r) {
    CHECK(r);

    int[] varprofile = new int[bddvarnum];

    varprofile_rec(r, varprofile);
    bdd_unmark(r);
    return varprofile;
  }

  private void varprofile_rec(int r, int[] varprofile) {

    if (r < 2) {
      return;
    }

    if (MARKED(r)) {
      return;
    }

    varprofile[bddlevel2var[LEVEL(r)]]++;
    SETMARK(r);

    varprofile_rec(LOW(r), varprofile);
    varprofile_rec(HIGH(r), varprofile);
  }

  private double bdd_pathcount(int r) {
    CHECK(r);

    miscid = CACHEID_PATHCOU;

    if (countcache == null) {
      countcache = BddCacheBigInteger_init(cachesize);
    }

    return bdd_pathcount_rec(r).doubleValue();
  }

  private BigInteger bdd_pathcount_rec(int r) {
    if (ISZERO(r)) {
      return BigInteger.ZERO;
    } else if (ISONE(r)) {
      return BigInteger.ONE;
    }

    BigIntegerBddCacheData entry = BddCache_lookupBigInteger(countcache, PATHCOUHASH(r, miscid));
    if (entry.a == r && entry.c == miscid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.value;
    }

    if (CACHESTATS) {
      cachestats.opMiss++;
    }
    BigInteger size = bdd_pathcount_rec(LOW(r)).add(bdd_pathcount_rec(HIGH(r)));

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = r;
    entry.c = miscid;
    entry.value = size;

    return size;
  }

  private BigInteger bdd_satcount(int r) {
    CHECK(r);

    if (countcache == null) {
      countcache = BddCacheBigInteger_init(cachesize);
    }

    miscid = CACHEID_SATCOU;
    return satcount_rec(r).shiftLeft(LEVEL(r));
  }

  private BigInteger satcount_rec(int root) {
    if (ISZERO(root)) {
      return BigInteger.ZERO;
    } else if (ISONE(root)) {
      return BigInteger.ONE;
    }

    BigIntegerBddCacheData entry = BddCache_lookupBigInteger(countcache, SATCOUHASH(root, miscid));
    if (entry.a == root && entry.c == miscid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.value;
    }

    if (CACHESTATS) {
      cachestats.opMiss++;
    }

    int low = LOW(root);
    int high = HIGH(root);
    BigInteger size =
        satcount_rec(low)
            .shiftLeft(LEVEL(low) - LEVEL(root) - 1)
            .add(satcount_rec(high).shiftLeft(LEVEL(high) - LEVEL(root) - 1));

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = root;
    entry.c = miscid;
    entry.value = size;

    return size;
  }

  private void bdd_gbc() {
    long c2, c1 = System.currentTimeMillis();

    // if (gbc_handler != NULL)
    {
      gcstats.nodes = bddnodesize;
      gcstats.freenodes = bddfreenum;
      gcstats.time = 0;
      gcstats.sumtime = gbcclock;
      gcstats.num = gbcollectnum;
      gbc_handler(true, gcstats);
    }

    for (int r = 0; r < bddrefstacktop; r++) {
      bdd_mark(bddrefstack[r]);
    }

    for (int n = 0; n < bddnodesize; n++) {
      if (HASREF(n)) {
        bdd_mark(n);
      }
      SETHASH(n, 0);
    }

    bddfreepos = 0;
    bddfreenum = 0;

    for (int n = bddnodesize - 1; n >= 2; n--) {

      if (MARKED(n) && LOW(n) != INVALID_BDD) {
        int hash2;

        UNMARK(n);
        hash2 = NODEHASH(LEVEL(n), LOW(n), HIGH(n));
        SETNEXT(n, HASH(hash2));
        SETHASH(hash2, n);
      } else {
        SETLOW(n, INVALID_BDD);
        SETNEXT(n, bddfreepos);
        bddfreepos = n;
        bddfreenum++;
      }
    }

    if (bddfreenum > 0) {
      // Don't reset or clean caches if we didn't free any nodes.

      if (FLUSH_CACHE_ON_GC) {
        bdd_operator_reset();
      } else {
        bdd_operator_clean();
      }
    }

    c2 = System.currentTimeMillis();
    gbcclock += c2 - c1;
    gbcollectnum++;

    // if (gbc_handler != NULL)
    {
      gcstats.nodes = bddnodesize;
      gcstats.freenodes = bddfreenum;
      gcstats.time = c2 - c1;
      gcstats.sumtime = gbcclock;
      gcstats.num = gbcollectnum;
      gbc_handler(false, gcstats);
    }

    // validate_all();
  }

  private int bdd_addref(int root) {
    if (root == INVALID_BDD) {
      bdd_error(BDD_BREAK); /* distinctive */
    }
    if (root < 2 || !bddrunning) {
      return root;
    }
    if (root >= bddnodesize) {
      return bdd_error(BDD_ILLBDD);
    }
    if (LOW(root) == INVALID_BDD) {
      return bdd_error(BDD_ILLBDD);
    }

    INCREF(root);
    return root;
  }

  private int bdd_delref(int root) {
    if (root == INVALID_BDD) {
      bdd_error(BDD_BREAK); /* distinctive */
    }
    if (root < 2 || !bddrunning) {
      return root;
    }
    if (root >= bddnodesize) {
      return bdd_error(BDD_ILLBDD);
    }
    if (LOW(root) == INVALID_BDD) {
      return bdd_error(BDD_ILLBDD);
    }

    /* if the following line is present, fails there much earlier */
    if (!HASREF(root)) {
      bdd_error(BDD_BREAK); /* distinctive */
    }

    DECREF(root);
    return root;
  }

  private void bdd_mark(int i) {

    if (i < 2) {
      return;
    }

    if (MARKED(i) || LOW(i) == INVALID_BDD) {
      return;
    }

    SETMARK(i);

    bdd_mark(LOW(i));
    bdd_mark(HIGH(i));
  }

  private void bdd_markcount(int i, int[] cou) {

    if (i < 2) {
      return;
    }

    if (MARKED(i) || LOW(i) == INVALID_BDD) {
      return;
    }

    SETMARK(i);
    cou[0] += 1;

    bdd_markcount(LOW(i), cou);
    bdd_markcount(HIGH(i), cou);
  }

  private void bdd_unmark(int i) {

    if (i < 2) {
      return;
    }

    if (!MARKED(i) || LOW(i) == INVALID_BDD) {
      return;
    }
    UNMARK(i);

    bdd_unmark(LOW(i));
    bdd_unmark(HIGH(i));
  }

  private int bdd_makenode(int level, int low, int high) {
    assert (ISCONST(low) || level < LEVEL(low)) && (ISCONST(high) || level < LEVEL(high));

    /* check whether childs are equal */
    if (low == high) {
      if (CACHESTATS) {
        cachestats.uniqueTrivial++;
      }
      return low;
    }

    if (CACHESTATS) {
      cachestats.uniqueAccess++;
    }

    /* Try to find an existing node of this kind */
    int hash2 = NODEHASH(level, low, high);
    int res = HASH(hash2);

    while (res != 0) {
      if (LEVEL(res) == level && LOW(res) == low && HIGH(res) == high) {
        if (CACHESTATS) {
          cachestats.uniqueHit++;
        }
        return res;
      }

      res = NEXT(res);
      if (CACHESTATS) {
        cachestats.uniqueChain++;
      }
    }

    /* No existing node => build one */
    if (CACHESTATS) {
      cachestats.uniqueMiss++;
    }

    /* Any free nodes to use ? */
    if (bddfreepos == 0) {
      if (bdderrorcond != 0) {
        return 0;
      }

      /* Try to allocate more nodes */
      bdd_gbc();

      if ((bddfreenum * 100) / bddnodesize <= minfreenodes) {
        bdd_noderesize(true);
        hash2 = NODEHASH(level, low, high);
      }

      /* Panic if that is not possible */
      if (bddfreepos == 0) {
        bdd_error(BDD_NODENUM);
        bdderrorcond = Math.abs(BDD_NODENUM);
        return 0;
      }
    }

    /* Build new node */
    res = bddfreepos;
    bddfreepos = NEXT(bddfreepos);
    bddfreenum--;
    bddproduced++;

    SETLEVELANDMARK(res, level);
    SETLOW(res, low);
    SETHIGH(res, high);

    /* Insert node */
    SETNEXT(res, HASH(hash2));
    SETHASH(hash2, res);

    return res;
  }

  private int bdd_noderesize(boolean doRehash) {
    int oldsize = bddnodesize;
    int newsize = bddnodesize;

    if (bddmaxnodesize > 0) {
      if (newsize >= bddmaxnodesize) {
        return -1;
      }
    }

    if (increasefactor > 0) {
      newsize += (int) (newsize * increasefactor);
    } else {
      newsize = newsize << 1;
    }

    if (bddmaxnodeincrease > 0) {
      if (newsize > oldsize + bddmaxnodeincrease) {
        newsize = oldsize + bddmaxnodeincrease;
      }
    }

    if (bddmaxnodesize > 0) {
      if (newsize > bddmaxnodesize) {
        newsize = bddmaxnodesize;
      }
    }

    return doResize(doRehash, oldsize, newsize);
  }

  @Override
  public int setNodeTableSize(int size) {
    int old = bddnodesize;
    doResize(true, old, size);
    return old;
  }

  private int doResize(boolean doRehash, int oldsize, int newsize) {

    newsize = bdd_prime_lte(newsize);

    if (oldsize > newsize) {
      return 0;
    }

    resize_handler(oldsize, newsize);

    int[] newnodes;
    newnodes = new int[newsize * __node_size];
    System.arraycopy(bddnodes, 0, newnodes, 0, bddnodes.length);
    bddnodes = newnodes;
    bddnodesize = newsize;

    if (doRehash) {
      for (int n = 0; n < oldsize; n++) {
        SETHASH(n, 0);
      }
    }

    for (int n = oldsize; n < bddnodesize; n++) {
      SETLOW(n, INVALID_BDD);
      // SETREFCOU(n, 0);
      // SETHASH(n, 0);
      // SETLEVEL(n, 0);
      SETNEXT(n, n + 1);
    }
    SETNEXT(bddnodesize - 1, bddfreepos);
    bddfreepos = oldsize;
    bddfreenum += bddnodesize - oldsize;

    if (doRehash) {
      bdd_gbc_rehash();
    }

    bddresized = true;

    return 0;
  }

  @Override
  protected void initialize(int initnodesize, int cs) {
    if (bddrunning) {
      bdd_error(BDD_RUNNING);
    }

    bddnodesize = bdd_prime_gte(initnodesize);

    bddnodes = new int[bddnodesize * __node_size];

    bddresized = false;

    for (int n = 0; n < bddnodesize; n++) {
      SETLOW(n, INVALID_BDD);
      // SETREFCOU(n, 0);
      // SETHASH(n, 0);
      // SETLEVEL(n, 0);
      SETNEXT(n, n + 1);
    }
    SETNEXT(bddnodesize - 1, 0);

    SETMAXREF(0);
    SETMAXREF(1);
    SETLOW(0, 0);
    SETHIGH(0, 0);
    SETLOW(1, 1);
    SETHIGH(1, 1);

    bdd_operator_init();

    bddfreepos = 2;
    bddfreenum = bddnodesize - 2;
    bddrunning = true;
    bddvarnum = 0;
    gbcollectnum = 0;
    gbcclock = 0;
    cachesize = cs;
    bddmaxnodeincrease = DEFAULTMAXNODEINC;

    bdderrorcond = 0;

    // bdd_gbc_hook(bdd_default_gbchandler);
    // bdd_error_hook(bdd_default_errhandler);
    // bdd_resize_hook(NULL);
    bdd_pairs_init();
  }

  /* Hash value modifiers to distinguish between entries in misccache */
  private static final int CACHEID_CONSTRAIN = 0x0;
  private static final int CACHEID_RESTRICT = 0x1;
  private static final int CACHEID_SATCOU = 0x2;
  private static final int CACHEID_SATCOULN = 0x3;
  private static final int CACHEID_PATHCOU = 0x4;

  /* Hash value modifiers for replace/compose */
  private static final int CACHEID_REPLACE = 0x0;
  private static final int CACHEID_COMPOSE = 0x1;
  private static final int CACHEID_VECCOMPOSE = 0x2;
  private static final int CACHEID_CORRECTIFY = 0x3;

  /* Hash value modifiers for quantification */
  private static final int CACHEID_EXIST = 0x0;
  private static final int CACHEID_FORALL = 0x1;
  private static final int CACHEID_UNIQUE = 0x2;
  private static final int CACHEID_APPEX = 0x3;
  private static final int CACHEID_APPAL = 0x4;
  private static final int CACHEID_APPUN = 0x5;

  /* Number of boolean operators */
  static final int OPERATOR_NUM = 11;

  /* Operator results - entry = left<<1 | right  (left,right in {0,1}) */
  private static int[][] oprres = {
    {0, 0, 0, 1}, /* and                       ( & )         */
    {0, 1, 1, 0}, /* xor                       ( ^ )         */
    {0, 1, 1, 1}, /* or                        ( | )         */
    {1, 1, 1, 0}, /* nand                                    */
    {1, 0, 0, 0}, /* nor                                     */
    {1, 1, 0, 1}, /* implication               ( >> )        */
    {1, 0, 0, 1}, /* bi-implication                          */
    {0, 0, 1, 0}, /* difference /greater than  ( - ) ( > )   */
    {0, 1, 0, 0}, /* less than                 ( < )         */
    {1, 0, 1, 1}, /* inverse implication       ( << )        */
    {1, 1, 0, 0} /* not                       ( ! )         */
  };

  private int applyop; /* Current operator for apply */
  private int appexop; /* Current operator for appex */
  private int appexid; /* Current cache id for appex */
  private int quantid; /* Current cache id for quantifications */
  private int[] quantvarset; /* Current variable set for quant. */
  private int quantvarsetID; /* Current id used in quantvarset */
  private int quantlast; /* Current last variable to be quant. */
  private int replaceid; /* Current cache id for replace */
  private int[] replacepair; /* Current replace pair */
  private int replacelast; /* Current last var. level to replace */
  private int composelevel; /* Current variable used for compose */
  private int miscid; /* Current cache id for other results */
  private int supportID; /* Current ID (true value) for support */
  private int supportMin; /* Min. used level in support calc. */
  private int supportMax; /* Max. used level in support calc. */
  @Nonnull private int[] supportSet; /* The found support set */
  private BddCache applycache; /* Cache for apply and ite results. See note in ite_rec. */
  private BddCache quantcache; /* Cache for exist/forall results */
  private BddCache appexcache; /* Cache for appex/appall results */
  private BddCache replacecache; /* Cache for replace results */
  private BddCache misccache; /* Cache for other results */
  private BddCache multiopcache; /* Cache for varargs operators */
  private BddCache countcache; /* Cache for count results */
  private int cacheratio;
  private int satPolarity;
  /* Used instead of local variable in order
  to avoid compiler warning about 'first'
  being clobbered by setjmp */

  private void bdd_operator_init() {
    quantvarsetID = 0;
    quantvarset = null;
    cacheratio = 0;
    supportSet = new int[0];
  }

  private void bdd_operator_done() {
    if (quantvarset != null) {
      quantvarset = null;
    }

    BddCache_done(applycache);
    applycache = null;
    BddCache_done(quantcache);
    quantcache = null;
    BddCache_done(appexcache);
    appexcache = null;
    BddCache_done(replacecache);
    replacecache = null;
    BddCache_done(misccache);
    misccache = null;
    BddCache_done(multiopcache);
    multiopcache = null;
    BddCache_done(countcache);
    countcache = null;

    if (supportSet.length > 0) {
      supportSet = new int[0];
    }
  }

  private void bdd_operator_reset() {
    BddCache_reset(applycache);
    BddCache_reset(quantcache);
    BddCache_reset(appexcache);
    BddCache_reset(replacecache);
    BddCache_reset(misccache);
    BddCache_reset(multiopcache);
    BddCache_reset(countcache);
  }

  private void bdd_operator_clean() {
    BddCache_clean_ab(applycache);
    BddCache_clean_a(quantcache);
    BddCache_clean_ab(appexcache);
    BddCache_clean_ab(replacecache);
    BddCache_clean_ab(misccache);
    BddCache_clean_multiop(multiopcache);
    BddCache_clean_d(countcache);
  }

  private void bdd_operator_varresize() {
    quantvarset = new int[bddvarnum];

    // memset(quantvarset, 0, sizeof(int)*bddvarnum);
    quantvarsetID = 0;

    BddCache_reset(countcache);
  }

  @Override
  public int setCacheSize(int newcachesize) {
    int old = cachesize;
    BddCache_resize(applycache, newcachesize);
    BddCache_resize(quantcache, newcachesize);
    BddCache_resize(appexcache, newcachesize);
    BddCache_resize(replacecache, newcachesize);
    BddCache_resize(misccache, newcachesize);
    BddCache_resize(multiopcache, newcachesize);
    BddCache_resize(countcache, newcachesize);
    return old;
  }

  private void bdd_operator_noderesize() {
    if (cacheratio > 0) {
      int newcachesize = bddnodesize / cacheratio;

      BddCache_resize(applycache, newcachesize);
      BddCache_resize(quantcache, newcachesize);
      BddCache_resize(appexcache, newcachesize);
      BddCache_resize(replacecache, newcachesize);
      BddCache_resize(misccache, newcachesize);
      BddCache_resize(multiopcache, newcachesize);
      BddCache_resize(countcache, newcachesize);

      cachesize = newcachesize;
    }
  }

  private BddCache BddCacheI_init(int size) {
    size = bdd_prime_gte(size);

    BddCache cache = new BddCache();
    cache.table = new BddCacheDataI[size];

    for (int n = 0; n < size; n++) {
      cache.table[n] = new BddCacheDataI();
      cache.table[n].a = -1;
    }
    cache.tablesize = size;

    return cache;
  }

  private BddCache BddCacheMultiOp_init(int size) {
    size = bdd_prime_gte(size);

    BddCache cache = new BddCache();
    cache.table = new MultiOpBddCacheData[size];

    for (int n = 0; n < size; n++) {
      cache.table[n] = new MultiOpBddCacheData();
      cache.table[n].a = -1;
    }
    cache.tablesize = size;

    return cache;
  }

  private BddCache BddCacheBigInteger_init(int size) {
    size = bdd_prime_gte(size);

    BddCache cache = new BddCache();
    cache.table = new BigIntegerBddCacheData[size];

    for (int n = 0; n < size; n++) {
      cache.table[n] = new BigIntegerBddCacheData();
      cache.table[n].a = -1;
    }
    cache.tablesize = size;

    return cache;
  }

  private static void BddCache_done(BddCache cache) {
    if (cache == null) {
      return;
    }

    cache.table = null;
    cache.tablesize = 0;
  }

  /**
   * Returns the name of the type of {@link BddCache} that {@code cache} represents.
   *
   * <p>Slow. Should only be used in debugging contexts.
   */
  private String getCacheName(BddCache cache) {
    if (cache == applycache) {
      return "apply";
    } else if (cache == appexcache) {
      return "appex";
    } else if (cache == countcache) {
      return "count";
    } else if (cache == misccache) {
      return "misc";
    } else if (cache == multiopcache) {
      return "multiop";
    } else if (cache == quantcache) {
      return "quant";
    } else if (cache == replacecache) {
      return "replace";
    } else {
      return "unknown";
    }
  }

  private int BddCache_resize(BddCache cache, int newsize) {
    if (cache == null) {
      return 0;
    }

    if (CACHESTATS) {
      System.err.printf(
          "Cache %s resize: %d/%d slots used%n",
          getCacheName(cache), cache.used(), cache.table.length);
    }

    newsize = bdd_prime_gte(newsize);

    if (cache.table instanceof BddCacheDataI[]) {
      cache.table = new BddCacheDataI[newsize];
    } else if (cache.table instanceof BigIntegerBddCacheData[]) {
      cache.table = new BigIntegerBddCacheData[newsize];
    } else if (cache.table instanceof MultiOpBddCacheData[]) {
      cache.table = new MultiOpBddCacheData[newsize];
    } else {
      throw new IllegalStateException("unknown cache table type");
    }

    for (int n = 0; n < newsize; n++) {
      if (cache.table instanceof BddCacheDataI[]) {
        cache.table[n] = new BddCacheDataI();
      } else if (cache.table instanceof BigIntegerBddCacheData[]) {
        cache.table[n] = new BigIntegerBddCacheData();
      } else if (cache.table instanceof MultiOpBddCacheData[]) {
        cache.table[n] = new MultiOpBddCacheData();
      } else {
        throw new IllegalStateException("unknown cache table type");
      }
      cache.table[n].a = -1;
    }
    cache.tablesize = newsize;

    return 0;
  }

  private static BddCacheDataI BddCache_lookupI(BddCache cache, int hash) {
    return (BddCacheDataI) cache.table[Math.abs(hash % cache.tablesize)];
  }

  private static BigIntegerBddCacheData BddCache_lookupBigInteger(BddCache cache, int hash) {
    return (BigIntegerBddCacheData) cache.table[Math.abs(hash % cache.tablesize)];
  }

  private static MultiOpBddCacheData BddCache_lookupMultiOp(BddCache cache, int hash) {
    return (MultiOpBddCacheData) cache.table[Math.abs(hash % cache.tablesize)];
  }

  private void BddCache_reset(BddCache cache) {
    if (cache == null) {
      return;
    }
    if (CACHESTATS) {
      System.err.printf(
          "Cache %s reset: %d/%d slots used%n",
          getCacheName(cache), cache.used(), cache.table.length);
    }

    for (int n = 0; n < cache.tablesize; n++) {
      cache.table[n].a = -1;
    }
  }

  private void BddCache_clean_d(BddCache cache) {
    if (cache == null) {
      return;
    }
    for (int n = 0; n < cache.tablesize; n++) {
      int a = cache.table[n].a;
      if (a >= 0 && LOW(a) == INVALID_BDD) {
        cache.table[n].a = -1;
      }
    }
  }

  private void BddCache_clean_a(BddCache cache) {
    if (cache == null) {
      return;
    }
    for (int n = 0; n < cache.tablesize; n++) {
      int a = cache.table[n].a;
      if (a < 0) {
        continue;
      }
      if (LOW(a) == INVALID_BDD || LOW(((BddCacheDataI) cache.table[n]).res) == INVALID_BDD) {
        cache.table[n].a = -1;
      }
    }
  }

  private void BddCache_clean_ab(BddCache cache) {
    if (cache == null) {
      return;
    }
    for (int n = 0; n < cache.tablesize; n++) {
      int a = cache.table[n].a;
      if (a < 0) {
        continue;
      }
      if (LOW(a) == INVALID_BDD
          || (cache.table[n].b != 0 && LOW(cache.table[n].b) == INVALID_BDD)
          || LOW(((BddCacheDataI) cache.table[n]).res) == INVALID_BDD) {
        cache.table[n].a = -1;
      }
    }
  }

  private void BddCache_clean_abc(BddCache cache) {
    if (cache == null) {
      return;
    }
    for (int n = 0; n < cache.tablesize; n++) {
      int a = cache.table[n].a;
      if (a < 0) {
        continue;
      }
      if (LOW(a) == -1
          || LOW(cache.table[n].b) == INVALID_BDD
          || LOW(cache.table[n].c) == INVALID_BDD
          || LOW(((BddCacheDataI) cache.table[n]).res) == INVALID_BDD) {
        cache.table[n].a = -1;
      }
    }
  }

  private void BddCache_clean_multiop(BddCache cache) {
    throw new UnsupportedOperationException("Clean is unimplemented for multiop cache.");
  }

  private void bdd_setpair(bddPair pair, int oldvar, int newvar) {
    if (pair == null) {
      return;
    }

    if (oldvar < 0 || oldvar > bddvarnum - 1) {
      bdd_error(BDD_VAR);
    }
    if (newvar < 0 || newvar > bddvarnum - 1) {
      bdd_error(BDD_VAR);
    }

    bdd_delref(pair.result[bddvar2level[oldvar]]);
    pair.result[bddvar2level[oldvar]] = bdd_ithvar(newvar);
    pair.id = update_pairsid();

    if (bddvar2level[oldvar] > pair.last) {
      pair.last = bddvar2level[oldvar];
    }
  }

  private void bdd_setbddpair(bddPair pair, int oldvar, int newvar) {
    int oldlevel;

    if (pair == null) {
      return;
    }

    CHECK(newvar);
    if (oldvar < 0 || oldvar >= bddvarnum) {
      bdd_error(BDD_VAR);
    }
    oldlevel = bddvar2level[oldvar];

    bdd_delref(pair.result[oldlevel]);
    pair.result[oldlevel] = bdd_addref(newvar);
    pair.id = update_pairsid();

    if (oldlevel > pair.last) {
      pair.last = oldlevel;
    }
  }

  private void bdd_resetpair(bddPair p) {
    for (int n = 0; n < bddvarnum; n++) {
      p.result[n] = bdd_ithvar(bddlevel2var[n]);
    }
    p.last = 0;
  }

  class bddPair extends BDDPairing {
    int[] result;
    int last;
    int id;
    bddPair next;

    @Override
    public void set(int oldvar, int newvar) {
      bdd_setpair(this, oldvar, newvar);
    }

    @Override
    public void set(int oldvar, BDD newvar) {
      bdd_setbddpair(this, oldvar, ((BDDImpl) newvar)._index);
    }

    @Override
    public void reset() {
      bdd_resetpair(this);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      boolean any = false;
      for (int i = 0; i < result.length; ++i) {
        if (result[i] != bdd_ithvar(bddlevel2var[i])) {
          if (any) {
            sb.append(", ");
          }
          any = true;
          sb.append(bddlevel2var[i]);
          sb.append('=');
          BDDImpl b = new BDDImpl(result[i]);
          sb.append(b);
          b.free();
        }
      }
      sb.append('}');
      return sb.toString();
    }
  }

  private bddPair pairs; /* List of all replacement pairs in use */
  private int pairsid; /* Pair identifier */

  /**
   * ***********************************************************************
   * ***********************************************************************
   */
  private void bdd_pairs_init() {
    pairsid = 0;
    pairs = null;
  }

  private void bdd_pairs_done() {
    bddPair p = pairs;

    while (p != null) {
      bddPair next = p.next;
      for (int n = 0; n < bddvarnum; n++) {
        bdd_delref(p.result[n]);
      }
      p.result = null;
      p = next;
    }
  }

  private int update_pairsid() {
    pairsid++;

    if (pairsid == (INT_MAX >> 2)) {
      pairsid = 0;
      for (bddPair p = pairs; p != null; p = p.next) {
        p.id = pairsid++;
      }
      // bdd_operator_reset();
      BddCache_reset(replacecache);
    }

    return pairsid;
  }

  private void bdd_register_pair(bddPair p) {
    p.next = pairs;
    pairs = p;
  }

  private void bdd_pairs_vardown(int level) {
    for (bddPair p = pairs; p != null; p = p.next) {
      int tmp;

      tmp = p.result[level];
      p.result[level] = p.result[level + 1];
      p.result[level + 1] = tmp;

      if (p.last == level) {
        p.last++;
      }
    }
  }

  private int bdd_pairs_resize(int oldsize, int newsize) {
    for (bddPair p = pairs; p != null; p = p.next) {
      int[] new_result = new int[newsize];
      System.arraycopy(p.result, 0, new_result, 0, oldsize);
      p.result = new_result;

      for (int n = oldsize; n < newsize; n++) {
        p.result[n] = bdd_ithvar(bddlevel2var[n]);
      }
    }

    return 0;
  }

  @Override
  public boolean isInitialized() {
    return this.bddrunning;
  }

  @Override
  public void done() {
    bdd_done();
  }

  private void bdd_done() {
    /*sanitycheck(); FIXME */
    // bdd_fdd_done();
    // bdd_reorder_done();
    bdd_pairs_done();

    bddnodes = null;
    bddrefstack = null;
    bddvarset = null;
    bddvar2level = null;
    bddlevel2var = null;

    bdd_operator_done();

    bddrunning = false;
    bddnodesize = 0;
    bddmaxnodesize = 0;
    bddvarnum = 0;
    bddproduced = 0;

    // err_handler = null;
    // gbc_handler = null;
    // resize_handler = null;
  }

  @Override
  public void setError(int code) {
    bdderrorcond = code;
  }

  @Override
  public void clearError() {
    bdderrorcond = 0;
  }

  @Override
  public int setMaxNodeNum(int size) {
    return bdd_setmaxnodenum(size);
  }

  private int bdd_setmaxnodenum(int size) {
    if (size > bddnodesize || size == 0) {
      int old = bddmaxnodesize;
      bddmaxnodesize = size;
      return old;
    }

    return bdd_error(BDD_NODES);
  }

  @Override
  public double setMinFreeNodes(double x) {
    return bdd_setminfreenodes((int) (x * 100.)) / 100.;
  }

  private int bdd_setminfreenodes(int mf) {
    int old = minfreenodes;

    if (mf < 0 || mf > 100) {
      return bdd_error(BDD_RANGE);
    }

    minfreenodes = mf;
    return old;
  }

  @Override
  public int setMaxIncrease(int x) {
    return bdd_setmaxincrease(x);
  }

  private int bdd_setmaxincrease(int size) {
    int old = bddmaxnodeincrease;

    if (size < 0) {
      return bdd_error(BDD_SIZE);
    }

    bddmaxnodeincrease = size;
    return old;
  }

  private double increasefactor;

  @Override
  public double setIncreaseFactor(double x) {
    if (x < 0) {
      return bdd_error(BDD_RANGE);
    }
    double old = increasefactor;
    increasefactor = x;
    return old;
  }

  @Override
  public int setCacheRatio(int r) {
    return bdd_setcacheratio(r);
  }

  private int bdd_setcacheratio(int r) {
    int old = cacheratio;

    if (r <= 0) {
      return bdd_error(BDD_RANGE);
    }
    if (bddnodesize == 0) {
      return old;
    }

    cacheratio = r;
    bdd_operator_noderesize();
    return old;
  }

  @Override
  public int varNum() {
    return bdd_varnum();
  }

  @Override
  public int setVarNum(int num) {
    return bdd_setvarnum(num);
  }

  @Override
  public int duplicateVar(int var) {
    if (var < 0 || var >= bddvarnum) {
      bdd_error(BDD_VAR);
      return BDDZERO;
    }

    int newVar = bddvarnum;
    int lev = bddvar2level[var];
    // System.out.println("Adding new variable "+newVar+" at level "+(lev+1));
    // Increase the size of the various data structures.
    bdd_setvarnum(bddvarnum + 1);
    // Actually duplicate the var in all BDDs.
    insert_level(lev);
    dup_level(lev, 0);
    // Fix up bddvar2level
    for (int i = 0; i < bddvarnum; ++i) {
      if (bddvar2level[i] > lev && bddvar2level[i] < bddvarnum) {
        ++bddvar2level[i];
      }
    }
    bddvar2level[newVar] = lev + 1;
    // Fix up bddlevel2var
    for (int i = bddvarnum - 2; i > lev; --i) {
      bddlevel2var[i + 1] = bddlevel2var[i];
    }
    bddlevel2var[lev + 1] = newVar;
    // Fix up bddvarset
    for (int bdv = 0; bdv < bddvarnum; bdv++) {
      bddvarset[bdv * 2] = PUSHREF(bdd_makenode(bddvar2level[bdv], BDDZERO, BDDONE));
      bddvarset[bdv * 2 + 1] = bdd_makenode(bddvar2level[bdv], BDDONE, BDDZERO);
      POPREF(1);

      SETMAXREF(bddvarset[bdv * 2]);
      SETMAXREF(bddvarset[bdv * 2 + 1]);
    }
    // Fix up pairs
    for (bddPair pair = pairs; pair != null; pair = pair.next) {
      bdd_delref(pair.result[bddvarnum - 1]);
      for (int i = bddvarnum - 1; i > lev + 1; --i) {
        pair.result[i] = pair.result[i - 1];
        if (i != LEVEL(pair.result[i]) && i > pair.last) {
          pair.last = i;
        }
      }
      pair.result[lev + 1] = bdd_ithvar(newVar);
      // System.out.println("Pair "+pair);
    }

    return newVar;
  }

  private int bdd_setvarnum(int num) {
    int bdv;
    int oldbddvarnum = bddvarnum;

    if (num < 1 || num > MAXVAR) {
      bdd_error(BDD_RANGE);
      return BDDZERO;
    }

    if (num < bddvarnum) {
      return bdd_error(BDD_DECVNUM);
    }
    if (num == bddvarnum) {
      return 0;
    }

    if (bddvarset == null) {
      bddvarset = new int[num * 2];
      bddlevel2var = new int[num + 1];
      bddvar2level = new int[num + 1];
    } else {
      int[] bddvarset2 = new int[num * 2];
      System.arraycopy(bddvarset, 0, bddvarset2, 0, bddvarset.length);
      bddvarset = bddvarset2;
      int[] bddlevel2var2 = new int[num + 1];
      System.arraycopy(bddlevel2var, 0, bddlevel2var2, 0, bddlevel2var.length);
      bddlevel2var = bddlevel2var2;
      int[] bddvar2level2 = new int[num + 1];
      System.arraycopy(bddvar2level, 0, bddvar2level2, 0, bddvar2level.length);
      bddvar2level = bddvar2level2;
    }

    bddrefstack = new int[num * 2 + 1];
    bddrefstacktop = 0;

    for (bdv = bddvarnum; bddvarnum < num; bddvarnum++) {
      bddvarset[bddvarnum * 2] = PUSHREF(bdd_makenode(bddvarnum, BDDZERO, BDDONE));
      bddvarset[bddvarnum * 2 + 1] = bdd_makenode(bddvarnum, BDDONE, BDDZERO);
      POPREF(1);

      if (bdderrorcond != 0) {
        bddvarnum = bdv;
        return -bdderrorcond;
      }

      SETMAXREF(bddvarset[bddvarnum * 2]);
      SETMAXREF(bddvarset[bddvarnum * 2 + 1]);
      bddlevel2var[bddvarnum] = bddvarnum;
      bddvar2level[bddvarnum] = bddvarnum;
    }

    SETLEVELANDMARK(0, num);
    SETLEVELANDMARK(1, num);
    bddvar2level[num] = num;
    bddlevel2var[num] = num;

    bdd_pairs_resize(oldbddvarnum, bddvarnum);
    bdd_operator_varresize();

    return 0;
  }

  @Override
  public BDD ithVar(int var) {
    return makeBDD(bdd_ithvar(var));
  }

  @Override
  public BDD nithVar(int var) {
    return makeBDD(bdd_nithvar(var));
  }

  @Override
  public void printAll() {
    bdd_fprintall(System.out);
  }

  @Override
  public void printTable(BDD b) {
    int x = ((BDDImpl) b)._index;
    bdd_fprinttable(System.out, x);
  }

  @Override
  public BDD load(BufferedReader in, int[] translate) throws IOException {
    int result = bdd_load(in, translate);
    return makeBDD(result);
  }

  @Override
  public void save(BufferedWriter out, BDD b) throws IOException {
    int x = ((BDDImpl) b)._index;
    bdd_save(out, x);
  }

  @Override
  public int level2Var(int level) {
    return bddlevel2var[level];
  }

  @Override
  public int var2Level(int var) {
    return bddvar2level[var];
  }

  @Override
  public void setVarOrder(int[] neworder) {
    bdd_setvarorder(neworder);
  }

  private int[] extroots;
  private int extrootsize;

  private levelData[] levels; /* Indexed by variable! */

  static class levelData {
    int start; /* Start of this sub-table (entry in "bddnodes") */
    int size; /* Size of this sub-table */
    int maxsize; /* Max. allowed size of sub-table */
    int nodenum; /* Number of nodes in this level */
  }

  static class imatrix {
    byte[][] rows;
    int size;
  }

  /* Interaction matrix */
  private imatrix iactmtx;

  private int bdd_getnodenum() {
    return bddnodesize - bddfreenum;
  }

  private void bdd_setvarorder(int[] neworder) {
    reorder_init();

    for (int level = 0; level < bddvarnum; level++) {
      int lowvar = neworder[level];

      while (bddvar2level[lowvar] > level) {
        reorder_varup(lowvar);
      }
    }

    reorder_done();
  }

  private int reorder_varup(int var) {
    if (var < 0 || var >= bddvarnum) {
      return bdd_error(BDD_VAR);
    }
    if (bddvar2level[var] == 0) {
      return 0;
    }
    return reorder_vardown(bddlevel2var[bddvar2level[var] - 1]);
  }

  private int reorder_vardown(int var) {
    int n, level;

    if (var < 0 || var >= bddvarnum) {
      return bdd_error(BDD_VAR);
    }
    if ((level = bddvar2level[var]) >= bddvarnum - 1) {
      return 0;
    }

    resizedInMakenode = false;

    if (imatrixDepends(iactmtx, var, bddlevel2var[level + 1])) {
      int toBeProcessed = reorder_downSimple(var);
      levelData l = levels[var];

      if (l.nodenum < l.size / 3 || l.nodenum >= (l.size * 3) / 2 && l.size < l.maxsize) {
        reorder_swapResize(toBeProcessed, var);
        reorder_localGbcResize(toBeProcessed, var);
      } else {
        reorder_swap(toBeProcessed, var);
        reorder_localGbc(var);
      }
    }

    /* Swap the var<->level tables */
    n = bddlevel2var[level];
    bddlevel2var[level] = bddlevel2var[level + 1];
    bddlevel2var[level + 1] = n;

    n = bddvar2level[var];
    bddvar2level[var] = bddvar2level[bddlevel2var[level]];
    bddvar2level[bddlevel2var[level]] = n;

    /* Update all rename pairs */
    bdd_pairs_vardown(level);

    if (resizedInMakenode) {
      reorder_rehashAll();
    }

    return 0;
  }

  private static boolean imatrixDepends(imatrix mtx, int a, int b) {
    return (mtx.rows[a][b / 8] & (1 << (b % 8))) != 0;
  }

  private void reorder_setLevellookup() {
    for (int n = 0; n < bddvarnum; n++) {
      levels[n].maxsize = bddnodesize / bddvarnum;
      levels[n].start = n * levels[n].maxsize;
      levels[n].size = Math.min(levels[n].maxsize, (levels[n].nodenum * 5) / 4);

      if (levels[n].size >= 4) {
        levels[n].size = bdd_prime_lte(levels[n].size);
      }
    }
  }

  private void reorder_rehashAll() {
    reorder_setLevellookup();
    bddfreepos = 0;

    for (int n = bddnodesize - 1; n >= 0; n--) {
      SETHASH(n, 0);
    }

    for (int n = bddnodesize - 1; n >= 2; n--) {
      if (HASREF(n)) {
        int hash2 = NODEHASH2(VARr(n), LOW(n), HIGH(n));
        SETNEXT(n, HASH(hash2));
        SETHASH(hash2, n);
      } else {
        SETNEXT(n, bddfreepos);
        bddfreepos = n;
      }
    }
  }

  private void reorder_localGbc(int var0) {
    int var1 = bddlevel2var[bddvar2level[var0] + 1];
    int vl1 = levels[var1].start;
    int size1 = levels[var1].size;

    for (int n = 0; n < size1; n++) {
      int hash = n + vl1;
      int r = HASH(hash);
      SETHASH(hash, 0);

      while (r != 0) {
        int next = NEXT(r);

        if (HASREF(r)) {
          SETNEXT(r, HASH(hash));
          SETHASH(hash, r);
        } else {
          DECREF(LOW(r));
          DECREF(HIGH(r));

          SETLOW(r, INVALID_BDD);
          SETNEXT(r, bddfreepos);
          bddfreepos = r;
          levels[var1].nodenum--;
          bddfreenum++;
        }

        r = next;
      }
    }
  }

  private int reorder_downSimple(int var0) {
    int toBeProcessed = 0;
    int var1 = bddlevel2var[bddvar2level[var0] + 1];
    int vl0 = levels[var0].start;
    int size0 = levels[var0].size;

    levels[var0].nodenum = 0;

    for (int n = 0; n < size0; n++) {
      int r;

      r = HASH(n + vl0);
      SETHASH(n + vl0, 0);

      while (r != 0) {
        int next = NEXT(r);

        /**
         * * if (LOW(r) == -1) { System.out.println(r+": LOW="+LOW(r)); } if (HIGH(r) == -1) {
         * System.out.println(r+": HIGH="+HIGH(r)); } *
         */
        if (VARr(LOW(r)) != var1 && VARr(HIGH(r)) != var1) {
          /* Node does not depend on next var, let it stay in the chain */
          SETNEXT(r, HASH(n + vl0));
          SETHASH(n + vl0, r);
          levels[var0].nodenum++;
        } else {
          /* Node depends on next var - save it for later procesing */
          SETNEXT(r, toBeProcessed);
          toBeProcessed = r;
          if (CACHESTATS) {
            cachestats.swapCount++;
          }
        }

        r = next;
      }
    }

    return toBeProcessed;
  }

  private void reorder_swapResize(int toBeProcessed, int var0) {
    int var1 = bddlevel2var[bddvar2level[var0] + 1];

    while (toBeProcessed != 0) {
      int next = NEXT(toBeProcessed);
      int f0 = LOW(toBeProcessed);
      int f1 = HIGH(toBeProcessed);
      int f00, f01, f10, f11;

      /* Find the cofactors for the new nodes */
      if (VARr(f0) == var1) {
        f00 = LOW(f0);
        f01 = HIGH(f0);
      } else {
        f00 = f01 = f0;
      }

      if (VARr(f1) == var1) {
        f10 = LOW(f1);
        f11 = HIGH(f1);
      } else {
        f10 = f11 = f1;
      }

      /* Note: makenode does refcou. */
      f0 = reorder_makenode(var0, f00, f10);
      f1 = reorder_makenode(var0, f01, f11);
      // node = bddnodes[toBeProcessed]; /* Might change in makenode */

      /* We know that the refcou of the grandchilds of this node
       * is greater than one (these are f00...f11), so there is
       * no need to do a recursive refcou decrease. It is also
       * possible for the node.low/high nodes to come alive again,
       * so deref. of the childs is delayed until the local GBC. */

      DECREF(LOW(toBeProcessed));
      DECREF(HIGH(toBeProcessed));

      /* Update in-place */
      SETVARr(toBeProcessed, var1);
      SETLOW(toBeProcessed, f0);
      SETHIGH(toBeProcessed, f1);

      levels[var1].nodenum++;

      /* Do not rehash yet since we are going to resize the hash table */

      toBeProcessed = next;
    }
  }

  private static int MIN(int a, int b) {
    return Math.min(a, b);
  }

  private void reorder_localGbcResize(int toBeProcessed, int var0) {
    int var1 = bddlevel2var[bddvar2level[var0] + 1];
    int vl1 = levels[var1].start;
    int size1 = levels[var1].size;

    for (int n = 0; n < size1; n++) {
      int hash = n + vl1;
      int r = HASH(hash);
      SETHASH(hash, 0);

      while (r != 0) {
        int next = NEXT(r);

        if (HASREF(r)) {
          SETNEXT(r, toBeProcessed);
          toBeProcessed = r;
        } else {
          DECREF(LOW(r));
          DECREF(HIGH(r));

          SETLOW(r, INVALID_BDD);
          SETNEXT(r, bddfreepos);
          bddfreepos = r;
          levels[var1].nodenum--;
          bddfreenum++;
        }

        r = next;
      }
    }

    /* Resize */
    if (levels[var1].nodenum < levels[var1].size) {
      levels[var1].size = MIN(levels[var1].maxsize, levels[var1].size / 2);
    } else {
      levels[var1].size = MIN(levels[var1].maxsize, levels[var1].size * 2);
    }

    if (levels[var1].size >= 4) {
      levels[var1].size = bdd_prime_lte(levels[var1].size);
    }

    /* Rehash the remaining live nodes */
    while (toBeProcessed != 0) {
      int next = NEXT(toBeProcessed);
      int hash = NODEHASH2(VARr(toBeProcessed), LOW(toBeProcessed), HIGH(toBeProcessed));

      SETNEXT(toBeProcessed, HASH(hash));
      SETHASH(hash, toBeProcessed);

      toBeProcessed = next;
    }
  }

  private void reorder_swap(int toBeProcessed, int var0) {
    int var1 = bddlevel2var[bddvar2level[var0] + 1];

    while (toBeProcessed != 0) {
      int next = NEXT(toBeProcessed);
      int f0 = LOW(toBeProcessed);
      int f1 = HIGH(toBeProcessed);
      int f00, f01, f10, f11, hash;

      /* Find the cofactors for the new nodes */
      if (VARr(f0) == var1) {
        f00 = LOW(f0);
        f01 = HIGH(f0);
      } else {
        f00 = f01 = f0;
      }

      if (VARr(f1) == var1) {
        f10 = LOW(f1);
        f11 = HIGH(f1);
      } else {
        f10 = f11 = f1;
      }

      /* Note: makenode does refcou. */
      f0 = reorder_makenode(var0, f00, f10);
      f1 = reorder_makenode(var0, f01, f11);
      // node = bddnodes[toBeProcessed]; /* Might change in makenode */

      /* We know that the refcou of the grandchilds of this node
       * is greater than one (these are f00...f11), so there is
       * no need to do a recursive refcou decrease. It is also
       * possible for the node.low/high nodes to come alive again,
       * so deref. of the childs is delayed until the local GBC. */

      DECREF(LOW(toBeProcessed));
      DECREF(HIGH(toBeProcessed));

      /* Update in-place */
      SETVARr(toBeProcessed, var1);
      SETLOW(toBeProcessed, f0);
      SETHIGH(toBeProcessed, f1);

      levels[var1].nodenum++;

      /* Rehash the node since it got new childs */
      hash = NODEHASH2(VARr(toBeProcessed), LOW(toBeProcessed), HIGH(toBeProcessed));
      SETNEXT(toBeProcessed, HASH(hash));
      SETHASH(hash, toBeProcessed);

      toBeProcessed = next;
    }
  }

  private int NODEHASH2(int var, int l, int h) {
    return Math.abs(PAIR(l, h) % levels[var].size) + levels[var].start;
  }

  private boolean resizedInMakenode;

  private int reorder_makenode(int var, int low, int high) {
    /* check whether childs are equal */
    if (low == high) {
      /* Note: We know that low,high has a refcou greater than zero, so
      there is no need to add reference *recursively* */
      INCREF(low);
      if (CACHESTATS) {
        cachestats.uniqueTrivial++;
      }
      return low;
    }

    if (CACHESTATS) {
      cachestats.uniqueAccess++;
    }

    /* Try to find an existing node of this kind */
    int hash = NODEHASH2(var, low, high);
    int res = HASH(hash);

    while (res != 0) {
      if (LOW(res) == low && HIGH(res) == high) {
        if (CACHESTATS) {
          cachestats.uniqueHit++;
        }
        INCREF(res);
        return res;
      }
      res = NEXT(res);

      if (CACHESTATS) {
        cachestats.uniqueChain++;
      }
    }

    /* No existing node -> build one */
    if (CACHESTATS) {
      cachestats.uniqueMiss++;
    }

    /* Any free nodes to use ? */
    if (bddfreepos == 0) {
      if (bdderrorcond != 0) {
        return 0;
      }

      /* Try to allocate more nodes - call noderesize without
       * enabling rehashing.
       * Note: if ever rehashing is allowed here, then remember to
       * update local variable "hash" */
      bdd_noderesize(false);
      resizedInMakenode = true;

      /* Panic if that is not possible */
      if (bddfreepos == 0) {
        bdd_error(BDD_NODENUM);
        bdderrorcond = Math.abs(BDD_NODENUM);
        return 0;
      }
    }

    /* Build new node */
    res = bddfreepos;
    bddfreepos = NEXT(bddfreepos);
    levels[var].nodenum++;
    bddproduced++;
    bddfreenum--;

    SETVARr(res, var);
    SETLOW(res, low);
    SETHIGH(res, high);

    /* Insert node in hash chain */
    SETNEXT(res, HASH(hash));
    SETHASH(hash, res);

    /* Make sure it is reference counted */
    CLEARREF(res);
    INCREF(res);
    INCREF(LOW(res));
    INCREF(HIGH(res));

    return res;
  }

  private int reorder_init() {
    reorder_handler(true, reorderstats);

    levels = new levelData[bddvarnum];

    for (int n = 0; n < bddvarnum; n++) {
      levels[n] = new levelData();
      levels[n].start = -1;
      levels[n].size = 0;
      levels[n].nodenum = 0;
    }

    /* First mark and recursive refcou. all roots and childs. Also do some
     * setup here for both setLevellookup and reorder_gbc */
    if (mark_roots() < 0) {
      return -1;
    }

    /* Initialize the hash tables */
    reorder_setLevellookup();

    /* Garbage collect and rehash to new scheme */
    reorder_gbc();

    return 0;
  }

  private void insert_level(int levToInsert) {
    for (int n = 2; n < bddnodesize; n++) {
      if (LOW(n) == INVALID_BDD) {
        continue;
      }
      int lev = LEVEL(n);
      if (lev <= levToInsert || lev == bddvarnum - 1) {
        // Stays the same.
        continue;
      }
      int lo, hi, newLev;
      lo = LOW(n);
      hi = HIGH(n);
      // Need to increase level by one.
      newLev = lev + 1;

      // Find this node in its hash chain.
      int hash = NODEHASH(lev, lo, hi);
      int r = HASH(hash), r2 = 0;
      while (r != n && r != 0) {
        r2 = r;
        r = NEXT(r);
      }
      if (r == 0) {
        // Cannot find node in the hash chain ?!
        throw new InternalError();
      }
      // Remove from this hash chain.
      int NEXT_r = NEXT(r);
      if (r2 == 0) {
        SETHASH(hash, NEXT_r);
      } else {
        SETNEXT(r2, NEXT_r);
      }
      // Set level of this node.
      SETLEVEL(n, newLev);
      lo = LOW(n);
      hi = HIGH(n);
      // Add to new hash chain.
      hash = NODEHASH(newLev, lo, hi);
      r = HASH(hash);
      SETHASH(hash, n);
      SETNEXT(n, r);
    }
  }

  private void dup_level(int levToInsert, int val) {
    for (int n = 2; n < bddnodesize; n++) {
      if (LOW(n) == INVALID_BDD) {
        continue;
      }
      int lev = LEVEL(n);
      if (lev != levToInsert || lev == bddvarnum - 1) {
        // Stays the same.
        continue;
      }
      int lo, hi, newLev;
      lo = LOW(n);
      hi = HIGH(n);
      // Duplicate this node.
      _assert(LEVEL(lo) > levToInsert + 1);
      _assert(LEVEL(hi) > levToInsert + 1);
      int n_low, n_high;
      bdd_addref(n);
      // 0 = var is zero, 1 = var is one, -1 = var equals other
      n_low = bdd_makenode(levToInsert + 1, val <= 0 ? lo : 0, val <= 0 ? 0 : lo);
      n_high = bdd_makenode(levToInsert + 1, val == 0 ? hi : 0, val == 0 ? 0 : hi);
      bdd_delref(n);
      // System.out.println("Lev = "+lev+" old low = "+lo+" old high = "+hi+" new low = "+n_low+"
      // ("+new bdd(n_low)+") new high = "+n_high+" ("+new bdd(n_high)+")");
      newLev = lev;
      SETLOW(n, n_low);
      SETHIGH(n, n_high);

      // Find this node in its hash chain.
      int hash = NODEHASH(lev, lo, hi);
      int r = HASH(hash), r2 = 0;
      while (r != n && r != 0) {
        r2 = r;
        r = NEXT(r);
      }
      if (r == 0) {
        // Cannot find node in the hash chain ?!
        throw new InternalError();
      }
      // Remove from this hash chain.
      int NEXT_r = NEXT(r);
      if (r2 == 0) {
        SETHASH(hash, NEXT_r);
      } else {
        SETNEXT(r2, NEXT_r);
      }
      // Set level of this node.
      SETLEVEL(n, newLev);
      lo = LOW(n);
      hi = HIGH(n);
      // Add to new hash chain.
      hash = NODEHASH(newLev, lo, hi);
      r = HASH(hash);
      SETHASH(hash, n);
      SETNEXT(n, r);
    }
  }

  private int mark_roots() {
    boolean[] dep = new boolean[bddvarnum];

    extrootsize = 0;
    for (int n = 2; n < bddnodesize; n++) {
      /* This is where we go from .level to .var!
       * - Do NOT use the LEVEL macro here. */
      SETLEVELANDMARK(n, bddlevel2var[LEVELANDMARK(n)]);

      if (HASREF(n)) {
        SETMARK(n);
        extrootsize++;
      }
    }

    extroots = new int[extrootsize];

    iactmtx = imatrixNew(bddvarnum);

    extrootsize = 0;
    for (int n = 2; n < bddnodesize; n++) {

      if (MARKED(n)) {
        UNMARK(n);
        extroots[extrootsize++] = n;

        for (int i = 0; i < bddvarnum; ++i) {
          dep[i] = false;
        }
        dep[VARr(n)] = true;
        levels[VARr(n)].nodenum++;

        addref_rec(LOW(n), dep);
        addref_rec(HIGH(n), dep);

        addDependencies(dep);
      }

      /* Make sure the hash field is empty. This saves a loop in the
      initial GBC */
      SETHASH(n, 0);
    }

    SETHASH(0, 0);
    SETHASH(1, 0);

    return 0;
  }

  private static imatrix imatrixNew(int size) {
    imatrix mtx = new imatrix();

    mtx.rows = new byte[size][];

    for (int n = 0; n < size; n++) {
      mtx.rows[n] = new byte[size / 8 + 1];
    }

    mtx.size = size;

    return mtx;
  }

  private void addref_rec(int r, boolean[] dep) {
    if (r < 2) {
      return;
    }

    if (!HASREF(r) || MARKED(r)) {
      bddfreenum--;

      /* Detect variable dependencies for the interaction matrix */
      dep[VARr(r) & ~MARK_MASK] = true;

      /* Make sure the nodenum field is updated. Used in the initial GBC */
      levels[VARr(r) & ~MARK_MASK].nodenum++;

      addref_rec(LOW(r), dep);
      addref_rec(HIGH(r), dep);
    } else {

      /* Update (from previously found) variable dependencies
       * for the interaction matrix */
      for (int n = 0; n < bddvarnum; n++) {
        dep[n] |= imatrixDepends(iactmtx, VARr(r) & ~MARK_MASK, n);
      }
    }

    INCREF(r);
  }

  private void addDependencies(boolean[] dep) {
    for (int n = 0; n < bddvarnum; n++) {
      for (int m = n; m < bddvarnum; m++) {
        if (dep[n] && dep[m]) {
          imatrixSet(iactmtx, n, m);
          imatrixSet(iactmtx, m, n);
        }
      }
    }
  }

  private static void imatrixSet(imatrix mtx, int a, int b) {
    mtx.rows[a][b / 8] |= 1 << (b % 8);
  }

  private void reorder_gbc() {
    bddfreepos = 0;
    bddfreenum = 0;

    /* No need to zero all hash fields - this is done in mark_roots */

    for (int n = bddnodesize - 1; n >= 2; n--) {

      if (HASREF(n)) {
        int hash;

        hash = NODEHASH2(VARr(n), LOW(n), HIGH(n));
        SETNEXT(n, HASH(hash));
        SETHASH(hash, n);

      } else {
        SETLOW(n, INVALID_BDD);
        SETNEXT(n, bddfreepos);
        bddfreepos = n;
        bddfreenum++;
      }
    }
  }

  private void reorder_done() {
    for (int n = 0; n < extrootsize; n++) {
      SETMARK(extroots[n]);
    }
    for (int n = 2; n < bddnodesize; n++) {
      if (MARKED(n)) {
        UNMARK(n);
      } else {
        CLEARREF(n);
      }

      /* This is where we go from .var to .level again!
       * - Do NOT use the LEVEL macro here. */
      SETLEVELANDMARK(n, bddvar2level[LEVELANDMARK(n)]);
    }

    imatrixDelete(iactmtx);
    bdd_gbc();

    reorder_handler(false, reorderstats);
  }

  private static void imatrixDelete(imatrix mtx) {
    for (int n = 0; n < mtx.size; n++) {
      mtx.rows[n] = null;
    }
    mtx.rows = null;
  }

  @Override
  public int nodeCount(Collection<BDD> r) {
    int[] a = new int[r.size()];
    int j = 0;
    for (Object o : r) {
      BDDImpl b = (BDDImpl) o;
      a[j++] = b._index;
    }
    return bdd_anodecount(a);
  }

  @Override
  public int getNodeTableSize() {
    return bdd_getallocnum();
  }

  private int bdd_getallocnum() {
    return bddnodesize;
  }

  @Override
  public int getNodeNum() {
    return bdd_getnodenum();
  }

  @Override
  public int getCacheSize() {
    return cachesize;
  }

  @Override
  public void printStat() {
    bdd_fprintstat(System.out);
  }

  @Override
  public BDDPairing makePair() {
    bddPair p = new bddPair();
    p.result = new int[bddvarnum];
    for (int n = 0; n < bddvarnum; n++) {
      p.result[n] = bdd_ithvar(bddlevel2var[n]);
    }

    p.id = update_pairsid();
    p.last = -1;

    bdd_register_pair(p);
    return p;
  }

  private void bdd_fprintall(PrintStream out) {
    for (int n = 0; n < bddnodesize; n++) {
      if (LOW(n) != INVALID_BDD) {
        out.print("[" + right(n, 5) + " - " + right(GETREF(n), 2) + "] ");
        // TODO: labelling of vars
        out.print(right(bddlevel2var[LEVEL(n)], 3));

        out.print(": " + right(LOW(n), 3));
        out.println(" " + right(HIGH(n), 3));
      }
    }
  }

  private void bdd_fprinttable(PrintStream out, int r) {
    out.println("ROOT: " + r);
    if (r < 2) {
      return;
    }

    bdd_mark(r);

    for (int n = 0; n < bddnodesize; n++) {
      if (MARKED(n)) {
        UNMARK(n);

        out.print("[" + right(n, 5) + "] ");
        // TODO: labelling of vars
        out.print(right(bddlevel2var[LEVEL(n)], 3));

        out.print(": " + right(LOW(n), 3));
        out.println(" " + right(HIGH(n), 3));
      }
    }
  }

  private int lh_nodenum;
  private int lh_freepos;
  private int[] loadvar2level;
  private LoadHash[] lh_table;

  private int bdd_load(BufferedReader ifile, int[] translate) throws IOException {
    int vnum, tmproot;
    int root;

    lh_nodenum = Integer.parseInt(readNext(ifile));
    vnum = Integer.parseInt(readNext(ifile));

    // Check for constant true / false
    if (lh_nodenum == 0 && vnum == 0) {
      root = Integer.parseInt(readNext(ifile));
      return root;
    }

    // Not actually used.
    loadvar2level = new int[vnum];
    for (int n = 0; n < vnum; n++) {
      loadvar2level[n] = Integer.parseInt(readNext(ifile));
    }

    if (vnum > bddvarnum) {
      bdd_setvarnum(vnum);
    }

    lh_table = new LoadHash[lh_nodenum];

    for (int n = 0; n < lh_nodenum; n++) {
      lh_table[n] = new LoadHash();
      lh_table[n].first = -1;
      lh_table[n].next = n + 1;
    }
    lh_table[lh_nodenum - 1].next = -1;
    lh_freepos = 0;

    tmproot = bdd_loaddata(ifile, translate);

    for (int n = 0; n < lh_nodenum; n++) {
      bdd_delref(lh_table[n].data);
    }

    lh_table = null;
    loadvar2level = null;

    root = tmproot;
    return root;
  }

  static class LoadHash {
    int key;
    int data;
    int first;
    int next;
  }

  private int bdd_loaddata(BufferedReader ifile, int[] translate) throws IOException {
    int key, var, low, high, root = 0;

    for (int n = 0; n < lh_nodenum; n++) {
      key = Integer.parseInt(readNext(ifile));
      var = Integer.parseInt(readNext(ifile));
      if (translate != null) {
        var = translate[var];
      }
      low = Integer.parseInt(readNext(ifile));
      high = Integer.parseInt(readNext(ifile));

      if (low >= 2) {
        low = loadhash_get(low);
      }
      if (high >= 2) {
        high = loadhash_get(high);
      }

      if (low < 0 || high < 0 || var < 0) {
        return bdd_error(BDD_FORMAT);
      }

      root = bdd_addref(bdd_ite(bdd_ithvar(var), high, low));

      loadhash_add(key, root);
    }

    return root;
  }

  private void loadhash_add(int key, int data) {
    int hash = key % lh_nodenum;
    int pos = lh_freepos;

    lh_freepos = lh_table[pos].next;
    lh_table[pos].next = lh_table[hash].first;
    lh_table[hash].first = pos;

    lh_table[pos].key = key;
    lh_table[pos].data = data;
  }

  private int loadhash_get(int key) {
    int hash = lh_table[key % lh_nodenum].first;

    while (hash != -1 && lh_table[hash].key != key) {
      hash = lh_table[hash].next;
    }

    if (hash == -1) {
      return -1;
    }
    return lh_table[hash].data;
  }

  private void bdd_save(BufferedWriter out, int r) throws IOException {
    int[] n = new int[1];

    if (r < 2) {
      out.write("0 0 " + r + "\n");
      return;
    }

    bdd_markcount(r, n);
    bdd_unmark(r);
    out.write(n[0] + " " + bddvarnum + "\n");

    for (int x = 0; x < bddvarnum; x++) {
      out.write(bddvar2level[x] + " ");
    }
    out.write("\n");

    bdd_save_rec(out, r);
    bdd_unmark(r);

    out.flush();
  }

  private void bdd_save_rec(BufferedWriter out, int root) throws IOException {

    if (root < 2) {
      return;
    }

    if (MARKED(root)) {
      return;
    }
    SETMARK(root);

    bdd_save_rec(out, LOW(root));
    bdd_save_rec(out, HIGH(root));

    out.write(root + " ");
    out.write(bddlevel2var[LEVEL(root)] + " ");
    out.write(LOW(root) + " ");
    out.write(HIGH(root) + "\n");
  }

  private static String right(int x, int w) {
    return right(Integer.toString(x), w);
  }

  private static String right(String s, int w) {
    int n = s.length();
    // if (w < n) return s.substring(n - w);
    StringBuilder b = new StringBuilder(w);
    for (int i = n; i < w; ++i) {
      b.append(' ');
    }
    b.append(s);
    return b.toString();
  }

  private void bdd_fprintstat(PrintStream out) {
    CacheStats s = cachestats;
    out.print(s.toString());
  }

  @Override
  protected BDDDomain createDomain(int a, BigInteger b) {
    return new bddDomain(a, b);
  }

  private class bddDomain extends BDDDomain {

    bddDomain(int a, BigInteger b) {
      super(a, b);
    }

    @Override
    public BDDFactory getFactory() {
      return JFactory.this;
    }
  }

  @Override
  protected BDDBitVector createBitVector(int a) {
    return new bvec(a);
  }

  private class bvec extends BDDBitVector {

    /** @param bitnum */
    bvec(int bitnum) {
      super(bitnum);
    }

    @Override
    public BDDFactory getFactory() {
      return JFactory.this;
    }
  }

  //// Prime stuff below.

  private Random rng = new Random();

  private int Random(int i) {
    return rng.nextInt(i) + 1;
  }

  private static boolean isEven(int src) {
    return (src & 0x1) == 0;
  }

  private static boolean hasFactor(int src, int n) {
    return (src != n) && (src % n == 0);
  }

  private static boolean BitIsSet(int src, int b) {
    return (src & (1 << b)) != 0;
  }

  private static final int CHECKTIMES = 20;

  private static int u64_mulmod(int a, int b, int c) {
    return (int) (((long) a * (long) b) % (long) c);
  }

  /**
   * *********************************************************************** Miller Rabin check
   * ***********************************************************************
   */
  private static int numberOfBits(int src) {
    if (src == 0) {
      return 0;
    }

    for (int b = 31; b > 0; --b) {
      if (BitIsSet(src, b)) {
        return b + 1;
      }
    }

    return 1;
  }

  private static boolean isWitness(int witness, int src) {
    int bitNum = numberOfBits(src - 1) - 1;
    int d = 1;

    for (int i = bitNum; i >= 0; --i) {
      int x = d;

      d = u64_mulmod(d, d, src);

      if (d == 1 && x != 1 && x != src - 1) {
        return true;
      }

      if (BitIsSet(src - 1, i)) {
        d = u64_mulmod(d, witness, src);
      }
    }

    return d != 1;
  }

  private boolean isMillerRabinPrime(int src) {
    for (int n = 0; n < CHECKTIMES; ++n) {
      int witness = Random(src - 1);

      if (isWitness(witness, src)) {
        return false;
      }
    }

    return true;
  }

  /**
   * *********************************************************************** Basic prime searching
   * stuff ***********************************************************************
   */
  private static boolean hasEasyFactors(int src) {
    return hasFactor(src, 3)
        || hasFactor(src, 5)
        || hasFactor(src, 7)
        || hasFactor(src, 11)
        || hasFactor(src, 13);
  }

  private boolean isPrime(int src) {
    if (hasEasyFactors(src)) {
      return false;
    }

    return isMillerRabinPrime(src);
  }

  /**
   * *********************************************************************** External interface
   * ***********************************************************************
   */
  private int bdd_prime_gte(int src) {
    if (isEven(src)) {
      ++src;
    }

    while (!isPrime(src)) {
      src += 2;
    }

    return src;
  }

  private int bdd_prime_lte(int src) {
    if (isEven(src)) {
      --src;
    }

    while (!isPrime(src)) {
      src -= 2;
    }

    return src;
  }
}

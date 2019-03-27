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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
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

  private static final boolean VERIFY_ASSERTIONS = false;
  public static final String REVISION = "$Revision: 1.28 $";

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

  private static final boolean USE_FINALIZER = true;
  private static final boolean FLUSH_CACHE_ON_GC = true;

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
      return makeBDD(bdd_veccompose(x, ((bddPair) pair)));
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
      return makeBDD(bdd_replace(x, ((bddPair) pair)));
    }

    @Override
    public BDD replaceWith(BDDPairing pair) {
      int x = _index;
      int y = bdd_replace(x, ((bddPair) pair));
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
      return bdd_satcount(_index);
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

  private static final boolean DEBUG_FINALIZER = false;

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

  private static class BddCacheDataD extends BddCacheData {
    double dres;

    @Override
    BddCacheData copy() {
      BddCacheDataD that = new BddCacheDataD();
      that.a = this.a;
      that.b = this.b;
      that.c = this.c;
      that.dres = this.dres;
      return that;
    }
  }

  private static class BddCache {
    BddCacheData[] table;
    int tablesize;

    BddCache copy() {
      BddCache that = new BddCache();
      boolean is_d = this.table instanceof BddCacheDataD[];
      if (is_d) {
        that.table = new BddCacheDataD[this.table.length];
      } else {
        that.table = new BddCacheDataI[this.table.length];
      }
      that.tablesize = this.tablesize;
      for (int i = 0; i < table.length; ++i) {
        that.table[i] = this.table[i].copy();
      }
      return that;
    }
  }

  private static class JavaBDDException extends BDDException {
    /** Version ID for serialization. */
    private static final long serialVersionUID = 3257289144995952950L;

    JavaBDDException(int x) {
      super(errorstrings[-x]);
    }
  }

  private static class ReorderException extends RuntimeException {
    /** Version ID for serialization. */
    private static final long serialVersionUID = 3256727264505772345L;
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
  private int usednodes_nextreorder; /* When to do reorder next time */

  private static final int BDD_MEMORY = (-1); /* Out of memory */
  private static final int BDD_VAR = (-2); /* Unknown variable */
  private static final int BDD_RANGE = (-3);
  /* Variable value out of range (not in domain) */
  private static final int BDD_DEREF = (-4);
  /* Removing external reference to unknown node */
  private static final int BDD_RUNNING = (-5);
  /* Called bdd_init() twice whithout bdd_done() */
  private static final int BDD_FILE = (-6); /* Some file operation failed */
  private static final int BDD_FORMAT = (-7); /* Incorrect file format */
  private static final int BDD_ORDER = (-8);
  /* Vars. not in order for vector based functions */
  private static final int BDD_BREAK = (-9); /* User called break */
  private static final int BDD_VARNUM = (-10);
  /* Different number of vars. for vector pair */
  private static final int BDD_NODES = (-11);
  /* Tried to set max. number of nodes to be fewer */
  /* than there already has been allocated */
  private static final int BDD_OP = (-12); /* Unknown operator */
  private static final int BDD_VARSET = (-13); /* Illegal variable set */
  private static final int BDD_VARBLK = (-14); /* Bad variable block operation */
  private static final int BDD_DECVNUM = (-15);
  /* Trying to decrease the number of variables */
  private static final int BDD_REPLACE = (-16);
  /* Replacing to already existing variables */
  private static final int BDD_NODENUM = (-17);
  /* Number of nodes reached user defined maximum */
  private static final int BDD_ILLBDD = (-18); /* Illegal bdd argument */
  private static final int BDD_SIZE = (-19); /* Illegal size argument */

  private static final int BVEC_SIZE = (-20); /* Mismatch in bitvector size */
  private static final int BVEC_SHIFT = (-21);
  /* Illegal shift-left/right parameter */
  private static final int BVEC_DIVZERO = (-22); /* Division by zero */

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
    return ((a + b) * (a + b + 1) / 2 + a);
  }

  private static int TRIPLE(int a, int b, int c) {
    // return Math.abs(PAIR(c, PAIR(a, b)));
    return (PAIR(c, PAIR(a, b)));
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

  private void CHECKa(int r, int x) {
    CHECK(r);
  }

  private int bdd_var(int root) {
    CHECK(root);
    if (root < 2) {
      bdd_error(BDD_ILLBDD);
    }

    return (bddlevel2var[LEVEL(root)]);
  }

  int bdd_low(int root) {
    CHECK(root);
    if (root < 2) {
      return bdd_error(BDD_ILLBDD);
    }

    return (LOW(root));
  }

  int bdd_high(int root) {
    CHECK(root);
    if (root < 2) {
      return bdd_error(BDD_ILLBDD);
    }

    return (HIGH(root));
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

  private static int ITEHASH(int f, int g, int h) {
    return TRIPLE(f, g, h);
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

  private static int REPLACEHASH(int r) {
    return r;
  }

  private static int VECCOMPOSEHASH(int f) {
    return f;
  }

  private static int COMPOSEHASH(int f, int g) {
    return PAIR(f, g);
  }

  private static int SATCOUHASH(int r) {
    return r;
  }

  private static int PATHCOUHASH(int r) {
    return r;
  }

  private static int APPEXHASH(int l, int r, int op) {
    return PAIR(l, r);
  }

  private static final double M_LN2 = 0.69314718055994530942;

  private static double log1p(double a) {
    return Math.log(1.0 + a);
  }

  private boolean INVARSET(int a) {
    return (quantvarset[a] == quantvarsetID); /* unsigned check */
  }

  private boolean INSVARSET(int a) {
    return Math.abs(quantvarset[a]) == quantvarsetID; /* signed check */
  }

  private static final int bddop_and = 0;
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

  private int bdd_not(int r) {
    int res;
    firstReorder = 1;
    CHECKa(r, BDDZERO);

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    while (true) {
      try {
        INITREF();

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = not_rec(r);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();
        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

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
    int res;
    firstReorder = 1;

    CHECKa(f, BDDZERO);
    CHECKa(g, BDDZERO);
    CHECKa(h, BDDZERO);

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (itecache == null) {
      itecache = BddCacheI_init(cachesize);
    }

    while (true) {
      try {
        INITREF();

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = ite_rec(f, g, h);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

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
    } else if (ISONE(g) && ISZERO(h)) {
      return f;
    } else if (ISZERO(g) && ISONE(h)) {
      return not_rec(f);
    }

    entry = BddCache_lookupI(itecache, ITEHASH(f, g, h));
    if (entry.a == f && entry.b == g && entry.c == h) {
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
    entry.c = h;
    entry.res = res;

    return res;
  }

  private int bdd_replace(int r, bddPair pair) {
    int res;
    firstReorder = 1;

    CHECKa(r, BDDZERO);

    if (replacecache == null) {
      replacecache = BddCacheI_init(cachesize);
    }

    while (true) {
      try {
        INITREF();
        replacepair = pair.result;
        replacelast = pair.last;
        replaceid = (pair.id << 2) | CACHEID_REPLACE;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = replace_rec(r);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    checkresize();
    return res;
  }

  private int replace_rec(int r) {
    BddCacheDataI entry;
    int res;

    if (ISCONST(r) || LEVEL(r) > replacelast) {
      return r;
    }

    entry = BddCache_lookupI(replacecache, REPLACEHASH(r));
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

    res = bdd_correctify(LEVEL(replacepair[LEVEL(r)]), READREF(2), READREF(1));
    POPREF(2);

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = r;
    entry.c = replaceid;
    entry.res = res;

    return res;
  }

  private int bdd_correctify(int level, int l, int r) {
    int res;

    if (level < LEVEL(l) && level < LEVEL(r)) {
      return bdd_makenode(level, l, r);
    }

    if (level == LEVEL(l) || level == LEVEL(r)) {
      bdd_error(BDD_REPLACE);
      return 0;
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

    return res; /* FIXME: cache ? */
  }

  private int bdd_apply(int l, int r, int op) {
    int res;
    firstReorder = 1;

    CHECKa(l, BDDZERO);
    CHECKa(r, BDDZERO);

    if (op < 0 || op > bddop_invimp) {
      bdd_error(BDD_OP);
      return BDDZERO;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }

    while (true) {
      try {
        INITREF();
        applyop = op;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
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
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    // validate(res);

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
      case bddop_xor:
        if (l == r) {
          return BDDZERO;
        } else if (ISZERO(l)) {
          return r;
        } else if (ISZERO(r)) {
          return l;
        }
        break;
      case bddop_nand:
        if (ISZERO(l) || ISZERO(r)) {
          return BDDONE;
        }
        break;
      case bddop_nor:
        if (ISONE(l) || ISONE(r)) {
          return BDDZERO;
        }
        break;
      case bddop_imp:
        if (ISZERO(l)) {
          return BDDONE;
        } else if (ISONE(l)) {
          return r;
        } else if (ISONE(r)) {
          return l;
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
    int res;
    firstReorder = 1;

    CHECKa(l, BDDZERO);
    CHECKa(r, BDDZERO);
    CHECKa(var, BDDZERO);

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

    while (true) {
      if (varset2vartable(var) < 0) {
        return BDDZERO;
      }
      try {
        INITREF();

        applyop = bddop_or;
        appexop = opr;
        appexid = (var << 5) | (appexop << 1); /* FIXME: range! */
        quantid = (appexid << 3) | CACHEID_APPEX;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = opr == bddop_and ? relprod_rec(l, r) : appquant_rec(l, r);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    checkresize();
    return res;
  }

  private int varset2vartable(int r) {
    int n;

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
    for (n = r; n > 1; n = HIGH(n)) {
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
    int n;

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
    for (n = r; !ISCONST(n); ) {
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

    PUSHREF(quant_rec(LOW(r)));
    PUSHREF(quant_rec(HIGH(r)));

    if (INVARSET(LEVEL(r))) {
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

  private int bdd_constrain(int f, int c) {
    int res;
    firstReorder = 1;

    CHECKa(f, BDDZERO);
    CHECKa(c, BDDZERO);

    if (misccache == null) {
      misccache = BddCacheI_init(cachesize);
    }

    while (true) {
      try {
        INITREF();
        miscid = CACHEID_CONSTRAIN;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = constrain_rec(f, c);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

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
    int res;
    firstReorder = 1;

    CHECKa(f, BDDZERO);
    CHECKa(g, BDDZERO);
    if (var < 0 || var >= bddvarnum) {
      bdd_error(BDD_VAR);
      return BDDZERO;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (itecache == null) {
      itecache = BddCacheI_init(cachesize);
    }

    while (true) {
      try {
        INITREF();
        composelevel = bddvar2level[var];
        replaceid = (composelevel << 2) | CACHEID_COMPOSE;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = compose_rec(f, g);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    checkresize();
    return res;
  }

  private int compose_rec(int f, int g) {
    BddCacheDataI entry;
    int res;

    if (LEVEL(f) > composelevel) {
      return f;
    }

    entry = BddCache_lookupI(replacecache, COMPOSEHASH(f, g));
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
    int res;
    firstReorder = 1;

    CHECKa(f, BDDZERO);

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (itecache == null) {
      itecache = BddCacheI_init(cachesize);
    }
    if (replacecache == null) {
      replacecache = BddCacheI_init(cachesize);
    }

    while (true) {
      try {
        INITREF();
        replacepair = pair.result;
        replaceid = (pair.id << 2) | CACHEID_VECCOMPOSE;
        replacelast = pair.last;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = veccompose_rec(f);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    checkresize();
    return res;
  }

  private int veccompose_rec(int f) {
    BddCacheDataI entry;
    int res;

    if (LEVEL(f) > replacelast) {
      return f;
    }

    entry = BddCache_lookupI(replacecache, VECCOMPOSEHASH(f));
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
    int res;
    firstReorder = 1;

    CHECKa(r, BDDZERO);
    CHECKa(var, BDDZERO);

    if (var < 2) /* Empty set */ {
      return r;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (quantcache == null) {
      quantcache = BddCacheI_init(cachesize);
    }

    while (true) {
      if (varset2vartable(var) < 0) {
        return BDDZERO;
      }
      try {
        INITREF();

        quantid = (var << 3) | CACHEID_EXIST; /* FIXME: range */
        applyop = bddop_or;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = quant_rec(r);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    checkresize();
    return res;
  }

  private int bdd_forall(int r, int var) {
    int res;
    firstReorder = 1;

    CHECKa(r, BDDZERO);
    CHECKa(var, BDDZERO);

    if (var < 2) /* Empty set */ {
      return r;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (quantcache == null) {
      quantcache = BddCacheI_init(cachesize);
    }

    while (true) {
      if (varset2vartable(var) < 0) {
        return BDDZERO;
      }
      try {
        INITREF();
        quantid = (var << 3) | CACHEID_FORALL;
        applyop = bddop_and;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = quant_rec(r);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    checkresize();
    return res;
  }

  private int bdd_unique(int r, int var) {
    int res;
    firstReorder = 1;

    CHECKa(r, BDDZERO);
    CHECKa(var, BDDZERO);

    if (var < 2) /* Empty set */ {
      return r;
    }

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }
    if (quantcache == null) {
      quantcache = BddCacheI_init(cachesize);
    }

    while (true) {
      try {
        INITREF();
        quantid = (var << 3) | CACHEID_UNIQUE;
        applyop = bddop_xor;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = unique_rec(r, var);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    checkresize();
    return res;
  }

  private int bdd_restrict(int r, int var) {
    int res;
    firstReorder = 1;

    CHECKa(r, BDDZERO);
    CHECKa(var, BDDZERO);

    if (var < 2) /* Empty set */ {
      return r;
    }

    if (misccache == null) {
      misccache = BddCacheI_init(cachesize);
    }

    while (true) {
      if (varset2svartable(var) < 0) {
        return BDDZERO;
      }
      try {
        INITREF();
        miscid = (var << 3) | CACHEID_RESTRICT;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = restrict_rec(r);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

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
    int res;
    firstReorder = 1;

    CHECKa(f, BDDZERO);
    CHECKa(d, BDDZERO);

    if (applycache == null) {
      applycache = BddCacheI_init(cachesize);
    }

    while (true) {
      try {
        INITREF();
        applyop = bddop_or;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = simplify_rec(f, d);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

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
    int n;
    int res = 1;

    CHECKa(r, BDDZERO);

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

    bdd_disable_reorder();

    for (n = supportMax; n >= supportMin; --n) {
      if (supportSet[n] == supportID) {
        int tmp;
        bdd_addref(res);
        tmp = bdd_makenode(n, BDDZERO, res);
        bdd_delref(res);
        res = tmp;
      }
    }

    bdd_enable_reorder();

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
    int res;
    firstReorder = 1;

    CHECKa(l, BDDZERO);
    CHECKa(r, BDDZERO);
    CHECKa(var, BDDZERO);

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

    while (true) {
      if (varset2vartable(var) < 0) {
        return BDDZERO;
      }
      try {
        INITREF();
        applyop = bddop_and;
        appexop = opr;
        appexid = (var << 5) | (appexop << 1) | 1; /* FIXME: range! */
        quantid = (appexid << 3) | CACHEID_APPAL;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = appquant_rec(l, r);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();

        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    checkresize();
    return res;
  }

  private int bdd_appuni(int l, int r, int opr, int var) {
    int res;
    firstReorder = 1;

    CHECKa(l, BDDZERO);
    CHECKa(r, BDDZERO);
    CHECKa(var, BDDZERO);

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

    while (true) {
      try {
        INITREF();
        applyop = bddop_xor;
        appexop = opr;
        appexid = (var << 5) | (appexop << 1) | 1; /* FIXME: range! */
        quantid = (appexid << 3) | CACHEID_APPUN;

        if (firstReorder == 0) {
          bdd_disable_reorder();
        }
        res = appuni_rec(l, r, var);
        if (firstReorder == 0) {
          bdd_enable_reorder();
        }
      } catch (ReorderException x) {
        bdd_checkreorder();
        if (firstReorder-- == 1) {
          continue;
        }
        res = BDDZERO;
        /* avoid warning about res being uninitialized */
      }
      break;
    }

    checkresize();
    return res;
  }

  private int bdd_satone(int r) {
    int res;

    CHECKa(r, BDDZERO);
    if (r < 2) {
      return r;
    }

    bdd_disable_reorder();

    INITREF();
    res = satone_rec(r);

    bdd_enable_reorder();

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

    CHECKa(r, BDDZERO);
    if (ISZERO(r)) {
      return r;
    }
    if (!ISCONST(pol)) {
      bdd_error(BDD_ILLBDD);
      return BDDZERO;
    }

    bdd_disable_reorder();

    INITREF();
    satPolarity = pol;
    res = satoneset_rec(r, var);

    bdd_enable_reorder();

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
    int v;

    CHECKa(r, BDDZERO);
    if (r == BDDZERO) {
      return 0;
    }

    bdd_disable_reorder();

    INITREF();
    res = fullsatone_rec(r);

    for (v = LEVEL(r) - 1; v >= 0; v--) {
      res = PUSHREF(bdd_makenode(v, res, BDDZERO));
    }

    bdd_enable_reorder();

    checkresize();
    return res;
  }

  private int fullsatone_rec(int r) {
    if (r < 2) {
      return r;
    }

    if (LOW(r) != BDDZERO) {
      int res = fullsatone_rec(LOW(r));
      int v;

      for (v = LEVEL(LOW(r)) - 1; v > LEVEL(r); v--) {
        res = PUSHREF(bdd_makenode(v, res, BDDZERO));
      }

      return PUSHREF(bdd_makenode(LEVEL(r), res, BDDZERO));
    } else {
      int res = fullsatone_rec(HIGH(r));
      int v;

      for (v = LEVEL(HIGH(r)) - 1; v > LEVEL(r); v--) {
        res = PUSHREF(bdd_makenode(v, res, BDDZERO));
      }

      return PUSHREF(bdd_makenode(LEVEL(r), BDDZERO, res));
    }
  }

  private void bdd_gbc_rehash() {
    int n;

    bddfreepos = 0;
    bddfreenum = 0;

    for (n = bddnodesize - 1; n >= 2; n--) {
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
    int n;
    int[] cou = new int[1];

    for (n = 0; n < r.length; n++) {
      bdd_markcount(r[n], cou);
    }

    for (n = 0; n < r.length; n++) {
      bdd_unmark(r[n]);
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
      countcache = BddCacheD_init(cachesize);
    }

    return bdd_pathcount_rec(r);
  }

  private double bdd_pathcount_rec(int r) {
    BddCacheDataD entry;
    double size;

    if (ISZERO(r)) {
      return 0.0;
    }
    if (ISONE(r)) {
      return 1.0;
    }

    entry = BddCache_lookupD(countcache, PATHCOUHASH(r));
    if (entry.a == r && entry.c == miscid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.dres;
    }

    if (CACHESTATS) {
      cachestats.opMiss++;
    }
    size = bdd_pathcount_rec(LOW(r)) + bdd_pathcount_rec(HIGH(r));

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = r;
    entry.c = miscid;
    entry.dres = size;

    return size;
  }

  private double bdd_satcount(int r) {
    double size = 1;

    CHECK(r);

    if (countcache == null) {
      countcache = BddCacheD_init(cachesize);
    }

    miscid = CACHEID_SATCOU;
    size = Math.pow(2.0, (double) LEVEL(r));

    return size * satcount_rec(r);
  }

  private double bdd_satcountset(int r, int varset) {
    double unused = bddvarnum;
    int n;

    if (ISCONST(varset) || ISZERO(r)) /* empty set */ {
      return 0.0;
    }

    for (n = varset; !ISCONST(n); n = HIGH(n)) {
      unused--;
    }

    unused = bdd_satcount(r) / Math.pow(2.0, unused);

    return unused >= 1.0 ? unused : 1.0;
  }

  private double satcount_rec(int root) {
    BddCacheDataD entry;
    double size, s;

    if (root < 2) {
      return root;
    }

    entry = BddCache_lookupD(countcache, SATCOUHASH(root));
    if (entry.a == root && entry.c == miscid) {
      if (CACHESTATS) {
        cachestats.opHit++;
      }
      return entry.dres;
    }

    if (CACHESTATS) {
      cachestats.opMiss++;
    }
    size = 0;
    s = 1;

    s *= Math.pow(2.0, (float) (LEVEL(LOW(root)) - LEVEL(root) - 1));
    size += s * satcount_rec(LOW(root));

    s = 1;
    s *= Math.pow(2.0, (float) (LEVEL(HIGH(root)) - LEVEL(root) - 1));
    size += s * satcount_rec(HIGH(root));

    if (CACHESTATS && entry.a != -1) {
      cachestats.opOverwrite++;
    }
    entry.a = root;
    entry.c = miscid;
    entry.dres = size;

    return size;
  }

  private void bdd_gbc() {
    int r;
    int n;
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

    for (r = 0; r < bddrefstacktop; r++) {
      bdd_mark(bddrefstack[r]);
    }

    for (n = 0; n < bddnodesize; n++) {
      if (HASREF(n)) {
        bdd_mark(n);
      }
      SETHASH(n, 0);
    }

    bddfreepos = 0;
    bddfreenum = 0;

    for (n = bddnodesize - 1; n >= 2; n--) {

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

    if (FLUSH_CACHE_ON_GC) {
      bdd_operator_reset();
    } else {
      bdd_operator_clean();
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

  private static final boolean CACHESTATS = false;

  private int bdd_makenode(int level, int low, int high) {
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

      if ((bddnodesize - bddfreenum) >= usednodes_nextreorder && bdd_reorder_ready()) {
        throw new ReorderException();
      }

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
    int n;
    newnodes = new int[newsize * __node_size];
    System.arraycopy(bddnodes, 0, newnodes, 0, bddnodes.length);
    bddnodes = newnodes;
    bddnodesize = newsize;

    if (doRehash) {
      for (n = 0; n < oldsize; n++) {
        SETHASH(n, 0);
      }
    }

    for (n = oldsize; n < bddnodesize; n++) {
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
    int n;

    if (bddrunning) {
      bdd_error(BDD_RUNNING);
    }

    bddnodesize = bdd_prime_gte(initnodesize);

    bddnodes = new int[bddnodesize * __node_size];

    bddresized = false;

    for (n = 0; n < bddnodesize; n++) {
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

    bdd_operator_init(cs);

    bddfreepos = 2;
    bddfreenum = bddnodesize - 2;
    bddrunning = true;
    bddvarnum = 0;
    gbcollectnum = 0;
    gbcclock = 0;
    cachesize = cs;
    usednodes_nextreorder = bddnodesize;
    bddmaxnodeincrease = DEFAULTMAXNODEINC;

    bdderrorcond = 0;

    if (CACHESTATS) {
      // cachestats = new CacheStats();
    }

    // bdd_gbc_hook(bdd_default_gbchandler);
    // bdd_error_hook(bdd_default_errhandler);
    // bdd_resize_hook(NULL);
    bdd_pairs_init();
    bdd_reorder_init();
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
  private BddCache applycache; /* Cache for apply results */
  private BddCache itecache; /* Cache for ITE results */
  private BddCache quantcache; /* Cache for exist/forall results */
  private BddCache appexcache; /* Cache for appex/appall results */
  private BddCache replacecache; /* Cache for replace results */
  private BddCache misccache; /* Cache for other results */
  private BddCache countcache; /* Cache for count results */
  private int cacheratio;
  private int satPolarity;
  private int firstReorder;
  /* Used instead of local variable in order
  to avoid compiler warning about 'first'
  being clobbered by setjmp */

  private void bdd_operator_init(int cachesize) {
    if (false) {
      applycache = BddCacheI_init(cachesize);
      itecache = BddCacheI_init(cachesize);
      quantcache = BddCacheI_init(cachesize);
      appexcache = BddCacheI_init(cachesize);
      replacecache = BddCacheI_init(cachesize);
      misccache = BddCacheI_init(cachesize);
      countcache = BddCacheD_init(cachesize);
    }

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
    BddCache_done(itecache);
    itecache = null;
    BddCache_done(quantcache);
    quantcache = null;
    BddCache_done(appexcache);
    appexcache = null;
    BddCache_done(replacecache);
    replacecache = null;
    BddCache_done(misccache);
    misccache = null;
    BddCache_done(countcache);
    countcache = null;

    if (supportSet.length > 0) {
      supportSet = new int[0];
    }
  }

  private void bdd_operator_reset() {
    BddCache_reset(applycache);
    BddCache_reset(itecache);
    BddCache_reset(quantcache);
    BddCache_reset(appexcache);
    BddCache_reset(replacecache);
    BddCache_reset(misccache);
    BddCache_reset(countcache);
  }

  private void bdd_operator_clean() {
    BddCache_clean_ab(applycache);
    BddCache_clean_abc(itecache);
    BddCache_clean_a(quantcache);
    BddCache_clean_ab(appexcache);
    BddCache_clean_ab(replacecache);
    BddCache_clean_ab(misccache);
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
    BddCache_resize(itecache, newcachesize);
    BddCache_resize(quantcache, newcachesize);
    BddCache_resize(appexcache, newcachesize);
    BddCache_resize(replacecache, newcachesize);
    BddCache_resize(misccache, newcachesize);
    BddCache_resize(countcache, newcachesize);
    return old;
  }

  private void bdd_operator_noderesize() {
    if (cacheratio > 0) {
      int newcachesize = bddnodesize / cacheratio;

      BddCache_resize(applycache, newcachesize);
      BddCache_resize(itecache, newcachesize);
      BddCache_resize(quantcache, newcachesize);
      BddCache_resize(appexcache, newcachesize);
      BddCache_resize(replacecache, newcachesize);
      BddCache_resize(misccache, newcachesize);
      BddCache_resize(countcache, newcachesize);
    }
  }

  private BddCache BddCacheI_init(int size) {
    int n;

    size = bdd_prime_gte(size);

    BddCache cache = new BddCache();
    cache.table = new BddCacheDataI[size];

    for (n = 0; n < size; n++) {
      cache.table[n] = new BddCacheDataI();
      cache.table[n].a = -1;
    }
    cache.tablesize = size;

    return cache;
  }

  private BddCache BddCacheD_init(int size) {
    int n;

    size = bdd_prime_gte(size);

    BddCache cache = new BddCache();
    cache.table = new BddCacheDataD[size];

    for (n = 0; n < size; n++) {
      cache.table[n] = new BddCacheDataD();
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

  private int BddCache_resize(BddCache cache, int newsize) {
    if (cache == null) {
      return 0;
    }
    int n;

    boolean is_d = cache.table instanceof BddCacheDataD[];

    cache.table = null;

    newsize = bdd_prime_gte(newsize);

    if (is_d) {
      cache.table = new BddCacheDataD[newsize];
    } else {
      cache.table = new BddCacheDataI[newsize];
    }

    for (n = 0; n < newsize; n++) {
      if (is_d) {
        cache.table[n] = new BddCacheDataD();
      } else {
        cache.table[n] = new BddCacheDataI();
      }
      cache.table[n].a = -1;
    }
    cache.tablesize = newsize;

    return 0;
  }

  private static BddCacheDataI BddCache_lookupI(BddCache cache, int hash) {
    return (BddCacheDataI) cache.table[Math.abs(hash % cache.tablesize)];
  }

  private static BddCacheDataD BddCache_lookupD(BddCache cache, int hash) {
    return (BddCacheDataD) cache.table[Math.abs(hash % cache.tablesize)];
  }

  private static void BddCache_reset(BddCache cache) {
    if (cache == null) {
      return;
    }
    int n;
    for (n = 0; n < cache.tablesize; n++) {
      cache.table[n].a = -1;
    }
  }

  private void BddCache_clean_d(BddCache cache) {
    if (cache == null) {
      return;
    }
    int n;
    for (n = 0; n < cache.tablesize; n++) {
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
    int n;
    for (n = 0; n < cache.tablesize; n++) {
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
    int n;
    for (n = 0; n < cache.tablesize; n++) {
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
    int n;
    for (n = 0; n < cache.tablesize; n++) {
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
    int n;

    for (n = 0; n < bddvarnum; n++) {
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
    int n;

    while (p != null) {
      bddPair next = p.next;
      for (n = 0; n < bddvarnum; n++) {
        bdd_delref(p.result[n]);
      }
      p.result = null;
      p = next;
    }
  }

  private int update_pairsid() {
    pairsid++;

    if (pairsid == (INT_MAX >> 2)) {
      bddPair p;
      pairsid = 0;
      for (p = pairs; p != null; p = p.next) {
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
    bddPair p;

    for (p = pairs; p != null; p = p.next) {
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
    bddPair p;
    int n;

    for (p = pairs; p != null; p = p.next) {
      int[] new_result = new int[newsize];
      System.arraycopy(p.result, 0, new_result, 0, oldsize);
      p.result = new_result;

      for (n = oldsize; n < newsize; n++) {
        p.result[n] = bdd_ithvar(bddlevel2var[n]);
      }
    }

    return 0;
  }

  private void bdd_disable_reorder() {
    reorderdisabled = 1;
  }

  private void bdd_enable_reorder() {
    reorderdisabled = 0;
  }

  private void bdd_checkreorder() {
    bdd_reorder_auto();

    /* Do not reorder before twice as many nodes have been used */
    usednodes_nextreorder = 2 * (bddnodesize - bddfreenum);

    /* And if very little was gained this time (< 20%) then wait until
     * even more nodes (upto twice as many again) have been used */
    if (bdd_reorder_gain() < 20) {
      usednodes_nextreorder += (usednodes_nextreorder * (20 - bdd_reorder_gain())) / 20;
    }
  }

  private boolean bdd_reorder_ready() {
    return (bddreordermethod != BDD_REORDER_NONE)
        && (vartree != null)
        && (bddreordertimes != 0)
        && (reorderdisabled == 0);
  }

  private void bdd_reorder(int method) {
    BddTree top;
    int savemethod = bddreordermethod;
    int savetimes = bddreordertimes;

    bddreordermethod = method;
    bddreordertimes = 1;

    if ((top = bddtree_new(-1)) != null) {
      if (reorder_init() >= 0) {

        usednum_before = bddnodesize - bddfreenum;

        top.firstVar = top.firstLevel = 0;
        top.lastVar = top.lastLevel = bdd_varnum() - 1;
        top.fixed = false;
        top.interleaved = false;
        top.next = null;
        top.nextlevel = vartree;

        reorder_block(top, method);
        vartree = top.nextlevel;

        usednum_after = bddnodesize - bddfreenum;

        reorder_done();
        bddreordermethod = savemethod;
        bddreordertimes = savetimes;
      }
    }
  }

  private static BddTree bddtree_new(int id) {
    BddTree t = new BddTree();

    t.firstVar = t.lastVar = t.firstLevel = t.lastLevel = -1;
    t.fixed = true;
    // t.interleaved = false;
    // t.next = t.prev = t.nextlevel = null;
    // t.seq = null;
    t.id = id;
    return t;
  }

  private BddTree reorder_block(BddTree t, int method) {
    BddTree dis;

    if (t == null) {
      return null;
    }

    if (!t.fixed /*BDD_REORDER_FREE*/ && t.nextlevel != null) {
      switch (method) {
        case BDD_REORDER_WIN2:
          t.nextlevel = reorder_win2(t.nextlevel);
          break;
        case BDD_REORDER_WIN2ITE:
          t.nextlevel = reorder_win2ite(t.nextlevel);
          break;
        case BDD_REORDER_SIFT:
          t.nextlevel = reorder_sift(t.nextlevel);
          break;
        case BDD_REORDER_SIFTITE:
          t.nextlevel = reorder_siftite(t.nextlevel);
          break;
        case BDD_REORDER_WIN3:
          t.nextlevel = reorder_win3(t.nextlevel);
          break;
        case BDD_REORDER_WIN3ITE:
          t.nextlevel = reorder_win3ite(t.nextlevel);
          break;
        case BDD_REORDER_RANDOM:
          t.nextlevel = reorder_random(t.nextlevel);
          break;
      }
    }

    for (dis = t.nextlevel; dis != null; dis = dis.next) {
      reorder_block(dis, method);
    }

    if (t.seq != null) {
      // Arrays.sort(t.seq, 0, t.lastVar-t.firstVar + 1);
      varseq_qsort(t.seq, 0, t.lastVar - t.firstVar + 1);
      t.firstLevel = bddvar2level[t.seq[0]];
      t.lastLevel = bddvar2level[t.seq[t.lastVar - t.firstVar]];
    }

    return t;
  }

  // due to Akihiko Tozawa
  private void varseq_qsort(int[] target, int from, int to) {

    int x, i, j;

    switch (to - from) {
      case 0:
        return;

      case 1:
        return;

      case 2:
        if (bddvar2level[target[from]] <= bddvar2level[target[from + 1]]) {
          return;
        } else {
          x = target[from];
          target[from] = target[from + 1];
          target[from + 1] = x;
        }
        return;
    }

    int r = target[from];
    int s = target[(from + to) / 2];
    int t = target[to - 1];

    if (bddvar2level[r] <= bddvar2level[s]) {
      if (bddvar2level[s] <= bddvar2level[t]) {
      } else if (bddvar2level[r] <= bddvar2level[t]) {
        target[to - 1] = s;
        target[(from + to) / 2] = t;
      } else {
        target[to - 1] = s;
        target[from] = t;
        target[(from + to) / 2] = r;
      }
    } else {
      if (bddvar2level[r] <= bddvar2level[t]) {
        target[(from + to) / 2] = r;
        target[from] = s;
      } else if (bddvar2level[s] <= bddvar2level[t]) {
        target[to - 1] = r;
        target[(from + to) / 2] = t;
        target[from] = s;
      } else {
        target[to - 1] = r;
        target[from] = t;
      }
    }

    int mid = target[(from + to) / 2];

    for (i = from + 1, j = to - 1; i + 1 != j; ) {
      if (target[i] == mid) {
        target[i] = target[i + 1];
        target[i + 1] = mid;
      }

      x = target[i];

      if (x <= mid) {
        i++;
      } else {
        x = target[--j];
        target[j] = target[i];
        target[i] = x;
      }
    }

    varseq_qsort(target, from, i);
    varseq_qsort(target, i + 1, to);
  }

  private BddTree reorder_win2(BddTree t) {
    BddTree dis = t, first = t;

    if (t == null) {
      return null;
    }

    if (verbose > 1) {
      System.out.println("Win2 start: " + reorder_nodenum() + " nodes");
      System.out.flush();
    }

    while (dis.next != null) {
      int best = reorder_nodenum();
      blockdown(dis);

      if (best < reorder_nodenum()) {
        blockdown(dis.prev);
        dis = dis.next;
      } else if (first == dis) {
        first = dis.prev;
      }

      if (verbose > 1) {
        System.out.print(".");
        System.out.flush();
      }
    }

    if (verbose > 1) {
      System.out.println();
      System.out.println("Win2 end: " + reorder_nodenum() + " nodes");
      System.out.flush();
    }

    return first;
  }

  private BddTree reorder_win3(BddTree t) {
    BddTree dis = t, first = t;

    if (t == null) {
      return null;
    }

    if (verbose > 1) {
      System.out.println("Win3 start: " + reorder_nodenum() + " nodes");
      System.out.flush();
    }

    while (dis.next != null) {
      BddTree[] f = new BddTree[1];
      f[0] = first;
      dis = reorder_swapwin3(dis, f);
      first = f[0];

      if (verbose > 1) {
        System.out.print(".");
        System.out.flush();
      }
    }

    if (verbose > 1) {
      System.out.println();
      System.out.println("Win3 end: " + reorder_nodenum() + " nodes");
      System.out.flush();
    }

    return first;
  }

  private BddTree reorder_win3ite(BddTree t) {
    BddTree dis = t, first = t;
    int lastsize;

    if (t == null) {
      return null;
    }

    if (verbose > 1) {
      System.out.println("Win3ite start: " + reorder_nodenum() + " nodes");
    }

    do {
      lastsize = reorder_nodenum();
      dis = first;

      while (dis.next != null && dis.next.next != null) {
        BddTree[] f = new BddTree[1];
        f[0] = first;
        dis = reorder_swapwin3(dis, f);
        first = f[0];

        if (verbose > 1) {
          System.out.print(".");
          System.out.flush();
        }
      }

      if (verbose > 1) {
        System.out.println(" " + reorder_nodenum() + " nodes");
      }
    } while (reorder_nodenum() != lastsize);

    if (verbose > 1) {
      System.out.println("Win3ite end: " + reorder_nodenum() + " nodes");
    }

    return first;
  }

  private BddTree reorder_swapwin3(BddTree dis, BddTree[] first) {
    boolean setfirst = dis.prev == null;
    BddTree next = dis;
    int best = reorder_nodenum();

    if (dis.next.next == null) /* Only two blocks left => win2 swap */ {
      blockdown(dis.prev);

      if (best < reorder_nodenum()) {
        blockdown(dis.prev);
        next = dis.next;
      } else {
        next = dis;
        if (setfirst) {
          first[0] = dis.prev;
        }
      }
    } else /* Real win3 swap */ {
      int pos = 0;
      blockdown(dis); /* B A* C (4) */
      pos++;
      if (best > reorder_nodenum()) {
        pos = 0;
        best = reorder_nodenum();
      }

      blockdown(dis); /* B C A* (3) */
      pos++;
      if (best > reorder_nodenum()) {
        pos = 0;
        best = reorder_nodenum();
      }

      dis = dis.prev.prev;
      blockdown(dis); /* C B* A (2) */
      pos++;
      if (best > reorder_nodenum()) {
        pos = 0;
        best = reorder_nodenum();
      }

      blockdown(dis); /* C A B* (1) */
      pos++;
      if (best > reorder_nodenum()) {
        pos = 0;
        best = reorder_nodenum();
      }

      dis = dis.prev.prev;
      blockdown(dis); /* A C* B (0)*/
      pos++;
      if (best > reorder_nodenum()) {
        pos = 0;
        best = reorder_nodenum();
      }

      if (pos >= 1) /* A C B -> C A* B */ {
        dis = dis.prev;
        blockdown(dis);
        next = dis;
        if (setfirst) {
          first[0] = dis.prev;
        }
      }

      if (pos >= 2) /* C A B -> C B A* */ {
        blockdown(dis);
        next = dis.prev;
        if (setfirst) {
          first[0] = dis.prev.prev;
        }
      }

      if (pos >= 3) /* C B A -> B C* A */ {
        dis = dis.prev.prev;
        blockdown(dis);
        next = dis;
        if (setfirst) {
          first[0] = dis.prev;
        }
      }

      if (pos >= 4) /* B C A -> B A C* */ {
        blockdown(dis);
        next = dis.prev;
        if (setfirst) {
          first[0] = dis.prev.prev;
        }
      }

      if (pos >= 5) /* B A C -> A B* C */ {
        dis = dis.prev.prev;
        blockdown(dis);
        next = dis;
        if (setfirst) {
          first[0] = dis.prev;
        }
      }
    }

    return next;
  }

  private BddTree reorder_sift_seq(BddTree t, BddTree[] seq, int num) {
    BddTree dis;
    int n;

    if (t == null) {
      return null;
    }

    for (n = 0; n < num; n++) {
      long c2, c1 = System.currentTimeMillis();

      if (verbose > 1) {
        System.out.print("Sift ");
        // if (reorder_filehandler)
        //   reorder_filehandler(stdout, seq[n].id);
        // else
        System.out.print(seq[n].id);
        System.out.print(": ");
      }

      reorder_sift_bestpos(seq[n], num / 2);

      if (verbose > 1) {
        System.out.println();
        System.out.print("> " + reorder_nodenum() + " nodes");
      }

      c2 = System.currentTimeMillis();
      if (verbose > 1) {
        System.out.println(" (" + (float) (c2 - c1) / (float) 1000 + " sec)\n");
      }
    }

    /* Find first block */
    for (dis = t; dis.prev != null; dis = dis.prev) /* nil */ {}

    return dis;
  }

  private void reorder_sift_bestpos(BddTree blk, int middlePos) {
    int best = reorder_nodenum();
    int maxAllowed;
    int bestpos = 0;
    boolean dirIsUp = true;
    int n;

    if (bddmaxnodesize > 0) {
      maxAllowed = MIN(best / 5 + best, bddmaxnodesize - bddmaxnodeincrease - 2);
    } else {
      maxAllowed = best / 5 + best;
    }

    /* Determine initial direction */
    if (blk.pos > middlePos) {
      dirIsUp = false;
    }

    /* Move block back and forth */
    for (n = 0; n < 2; n++) {
      int first = 1;

      if (dirIsUp) {
        while (blk.prev != null && (reorder_nodenum() <= maxAllowed || first != 0)) {
          first = 0;
          blockdown(blk.prev);
          bestpos--;

          if (verbose > 1) {
            System.out.print("-");
            System.out.flush();
          }

          if (reorder_nodenum() < best) {
            best = reorder_nodenum();
            bestpos = 0;

            if (bddmaxnodesize > 0) {
              maxAllowed = MIN(best / 5 + best, bddmaxnodesize - bddmaxnodeincrease - 2);
            } else {
              maxAllowed = best / 5 + best;
            }
          }
        }
      } else {
        while (blk.next != null && (reorder_nodenum() <= maxAllowed || first != 0)) {
          first = 0;
          blockdown(blk);
          bestpos++;

          if (verbose > 1) {
            System.out.print("+");
            System.out.flush();
          }

          if (reorder_nodenum() < best) {
            best = reorder_nodenum();
            bestpos = 0;

            if (bddmaxnodesize > 0) {
              maxAllowed = MIN(best / 5 + best, bddmaxnodesize - bddmaxnodeincrease - 2);
            } else {
              maxAllowed = best / 5 + best;
            }
          }
        }
      }

      if (reorder_nodenum() > maxAllowed && verbose > 1) {
        System.out.print("!");
        System.out.flush();
      }

      dirIsUp = !dirIsUp;
    }

    /* Move to best pos */
    while (bestpos < 0) {
      blockdown(blk);
      bestpos++;
    }
    while (bestpos > 0) {
      blockdown(blk.prev);
      bestpos--;
    }
  }

  private BddTree reorder_random(BddTree t) {
    BddTree dis;
    BddTree[] seq;
    int n, num = 0;

    if (t == null) {
      return null;
    }

    for (dis = t; dis != null; dis = dis.next) {
      num++;
    }
    seq = new BddTree[num];
    for (dis = t, num = 0; dis != null; dis = dis.next) {
      seq[num++] = dis;
    }

    for (n = 0; n < 4 * num; n++) {
      int blk = rng.nextInt(num);
      if (seq[blk].next != null) {
        blockdown(seq[blk]);
      }
    }

    /* Find first block */
    for (dis = t; dis.prev != null; dis = dis.prev) /* nil */ {}

    if (verbose != 0) {
      System.out.println("Random order: " + reorder_nodenum() + " nodes");
    }
    return dis;
  }

  static class sizePair {
    int val;
    BddTree block;
  }

  private BddTree reorder_sift(BddTree t) {
    BddTree dis;
    BddTree[] seq;
    sizePair[] p;
    int n, num;

    for (dis = t, num = 0; dis != null; dis = dis.next) {
      dis.pos = num++;
    }

    p = new sizePair[num];
    seq = new BddTree[num];

    for (dis = t, n = 0; dis != null; dis = dis.next, n++) {
      int v;

      /* Accumulate number of nodes for each block */
      p[n] = new sizePair();
      p[n].val = 0;
      for (v = dis.firstVar; v <= dis.lastVar; v++) {
        p[n].val -= levels[v].nodenum;
      }

      p[n].block = dis;
    }

    /* Sort according to the number of nodes at each level */
    Arrays.sort(p, 0, num, Comparator.comparingInt(o -> o.val));

    /* Create sequence */
    for (n = 0; n < num; n++) {
      seq[n] = p[n].block;
    }

    /* Do the sifting on this sequence */
    t = reorder_sift_seq(t, seq, num);

    return t;
  }

  private BddTree reorder_siftite(BddTree t) {
    BddTree first = t;
    int lastsize;
    int c = 1;

    if (t == null) {
      return null;
    }

    do {
      if (verbose > 1) {
        System.out.println("Reorder " + (c++) + "\n");
      }

      lastsize = reorder_nodenum();
      first = reorder_sift(first);
    } while (reorder_nodenum() != lastsize);

    return first;
  }

  public void reverseAllDomains() {
    reorder_init();
    for (int i = 0; i < fdvarnum; ++i) {
      reverseDomain0(domain[i]);
    }
    reorder_done();
  }

  public void reverseDomain(BDDDomain d) {
    reorder_init();
    reverseDomain0(d);
    reorder_done();
  }

  private void reverseDomain0(BDDDomain d) {
    int n = d.varNum();
    BddTree[] trees = new BddTree[n];
    int v = d.ivar[0];
    addVarBlock(v, v, true);
    trees[0] = getBlock(vartree, v, v);
    BddTree parent = getParent(trees[0]);
    for (int i = 1; i < n; ++i) {
      v = d.ivar[i];
      addVarBlock(v, v, true);
      trees[i] = getBlock(vartree, v, v);
      BddTree parent2 = getParent(trees[i]);
      if (parent != parent2) {
        throw new BDDException();
      }
    }
    for (int i = 0; i < n; ++i) {
      for (int j = i + 1; j < n; ++j) {
        blockdown(trees[i]);
      }
    }
    BddTree newchild = trees[n - 1];
    while (newchild.prev != null) {
      newchild = newchild.prev;
    }
    if (parent == null) {
      vartree = newchild;
    } else {
      parent.nextlevel = newchild;
    }
  }

  public void setVarOrder(String ordering) {
    List<List<BDDDomain>> result = new LinkedList<>();
    int nDomains = numberOfDomains();
    StringTokenizer st = new StringTokenizer(ordering, "x_", true);
    boolean[] done = new boolean[nDomains];
    List<BDDDomain> last = null;
    while (true) {
      String s = st.nextToken();
      BDDDomain d;
      for (int j = 0; ; ++j) {
        if (j == nDomains) {
          throw new BDDException("bad domain: " + s);
        }
        d = getDomain(j);
        if (s.equals(d.getName())) {
          break;
        }
      }
      if (done[d.getIndex()]) {
        throw new BDDException("duplicate domain: " + s);
      }
      done[d.getIndex()] = true;
      if (last != null) {
        last.add(d);
      }
      if (st.hasMoreTokens()) {
        s = st.nextToken();
        if (s.equals("x")) {
          if (last == null) {
            last = new LinkedList<>();
            last.add(d);
            result.add(last);
          }
        } else if (s.equals("_")) {
          if (last == null) {
            result.add(Collections.singletonList(d));
          }
          last = null;
        } else {
          throw new BDDException("bad token: " + s);
        }
      } else {
        if (last == null) {
          result.add(Collections.singletonList(d));
        }
        break;
      }
    }

    for (int i = 0; i < done.length; ++i) {
      if (!done[i]) {
        throw new BDDException("missing domain #" + i + ": " + getDomain(i));
      }
    }

    setVarOrder(result);
  }

  /**
   * Set the variable order to be the given list of domains.
   *
   * @param domains domain order
   */
  public void setVarOrder(List<List<BDDDomain>> domains) {
    BddTree[] my_vartree = new BddTree[fdvarnum];
    boolean[] interleaved = new boolean[fdvarnum];
    int k = 0;
    for (List<BDDDomain> c : domains) {
      for (Iterator<BDDDomain> j = c.iterator(); j.hasNext(); ) {
        BDDDomain d = j.next();
        int low = d.ivar[0];
        int high = d.ivar[d.ivar.length - 1];
        bdd_intaddvarblock(low, high, false);
        BddTree b = getBlock(vartree, low, high);
        my_vartree[k] = b;
        interleaved[k] = j.hasNext();
        k++;
      }
    }
    if (k <= 1) {
      return;
    }
    BddTree parent = getParent(my_vartree[0]);
    for (int i = 0; i < k; ++i) {
      if (parent != getParent(my_vartree[i])) {
        throw new BDDException(
            "var block "
                + my_vartree[i].firstVar
                + ".."
                + my_vartree[i].lastVar
                + " is in wrong place in tree");
      }
    }
    reorder_init();
    BddTree prev = null;
    boolean prev_interleaved = false;
    for (int i = 0; i < k; ++i) {
      BddTree t = my_vartree[i];
      while (t.prev != prev) {
        blockdown(t.prev);
      }
      boolean inter = interleaved[i];
      if (prev_interleaved) {
        blockinterleave(t.prev);
        // ++i;
        prev = t.prev;
      } else {
        prev = t;
      }
      prev_interleaved = inter;
    }
    BddTree newchild = my_vartree[0];
    _assert(newchild.prev == null);
    // while (newchild.prev != null) newchild = newchild.prev;
    if (parent == null) {
      vartree = newchild;
    } else {
      parent.nextlevel = newchild;
    }
    reorder_done();
  }

  protected BddTree getParent(BddTree child) {
    for (BddTree p = vartree; p != null; p = p.next) {
      if (p == child) {
        return null;
      }
      BddTree q = getParent(p, child);
      if (q != null) {
        return q;
      }
    }
    throw new BDDException("Cannot find tree node " + child);
  }

  protected static BddTree getParent(BddTree parent, BddTree child) {
    if (parent.nextlevel == null) {
      return null;
    }
    for (BddTree p = parent.nextlevel; p != null; p = p.next) {
      if (p == child) {
        return parent;
      }
      BddTree q = getParent(p, child);
      if (q != null) {
        return q;
      }
    }
    return null;
  }

  private static BddTree getBlock(BddTree t, int low, int high) {
    if (t == null) {
      return null;
    }
    for (BddTree p = t; p != null; p = p.next) {
      if (p.firstVar == low && p.lastVar == high) {
        return p;
      }
      BddTree q = getBlock(p.nextlevel, low, high);
      if (q != null) {
        return q;
      }
    }
    return null;
  }

  private void blockinterleave(BddTree left) {
    BddTree right = left.next;
    // System.out.println("Interleaving "+left.first+".."+left.last+" and
    // "+right.first+".."+right.last);
    int n;
    int leftsize = left.lastVar - left.firstVar;
    int rightsize = right.lastVar - right.firstVar;
    int[] lseq = left.seq;
    int[] rseq = right.seq;
    int minsize = Math.min(leftsize, rightsize);
    for (n = 0; n <= minsize; ++n) {
      while (bddvar2level[lseq[n]] + 1 < bddvar2level[rseq[n]]) {
        reorder_varup(rseq[n]);
      }
    }
    outer:
    for (; n <= rightsize; ++n) {
      while (true) {
        BddTree t = left.prev;
        if (t == null || !t.interleaved) {
          break outer;
        }
        int tsize = t.lastVar - t.firstVar;
        if (n <= tsize) {
          int[] tseq = t.seq;
          while (bddvar2level[tseq[n]] + 1 < bddvar2level[rseq[n]]) {
            reorder_varup(rseq[n]);
          }
          break;
        }
      }
    }
    right.next.prev = left;
    left.next = right.next;
    left.firstVar = Math.min(left.firstVar, right.firstVar);
    left.lastVar = Math.max(left.lastVar, right.lastVar);
    left.seq = new int[left.lastVar - left.firstVar + 1];
    update_seq(left);
  }

  private void blockdown(BddTree left) {
    BddTree right = left.next;
    // System.out.println("Swapping "+left.first+".."+left.last+" and
    // "+right.first+".."+right.last);
    int n;
    int leftsize = left.lastVar - left.firstVar;
    int rightsize = right.lastVar - right.firstVar;
    int leftstart = bddvar2level[left.seq[0]];
    int[] lseq = left.seq;
    int[] rseq = right.seq;

    /* Move left past right */
    while (bddvar2level[lseq[0]] < bddvar2level[rseq[rightsize]]) {
      for (n = 0; n < leftsize; n++) {
        if (bddvar2level[lseq[n]] + 1 != bddvar2level[lseq[n + 1]]
            && bddvar2level[lseq[n]] < bddvar2level[rseq[rightsize]]) {
          reorder_vardown(lseq[n]);
        }
      }

      if (bddvar2level[lseq[leftsize]] < bddvar2level[rseq[rightsize]]) {
        reorder_vardown(lseq[leftsize]);
      }
    }

    /* Move right to where left started */
    while (bddvar2level[rseq[0]] > leftstart) {
      for (n = rightsize; n > 0; n--) {
        if (bddvar2level[rseq[n]] - 1 != bddvar2level[rseq[n - 1]]
            && bddvar2level[rseq[n]] > leftstart) {
          reorder_varup(rseq[n]);
        }
      }

      if (bddvar2level[rseq[0]] > leftstart) {
        reorder_varup(rseq[0]);
      }
    }

    /* Swap left and right data in the order */
    left.next = right.next;
    right.prev = left.prev;
    left.prev = right;
    right.next = left;

    if (right.prev != null) {
      right.prev.next = right;
    }
    if (left.next != null) {
      left.next.prev = left;
    }

    n = left.pos;
    left.pos = right.pos;
    right.pos = n;

    left.interleaved = false;
    right.interleaved = false;

    left.firstLevel = bddvar2level[lseq[0]];
    left.lastLevel = bddvar2level[lseq[leftsize]];
    right.firstLevel = bddvar2level[rseq[0]];
    right.lastLevel = bddvar2level[rseq[rightsize]];
  }

  private BddTree reorder_win2ite(BddTree t) {
    BddTree dis, first = t;
    int lastsize;
    int c = 1;

    if (t == null) {
      return null;
    }

    if (verbose > 1) {
      System.out.println("Win2ite start: " + reorder_nodenum() + " nodes");
    }

    do {
      lastsize = reorder_nodenum();

      dis = t;
      while (dis.next != null) {
        int best = reorder_nodenum();

        blockdown(dis);

        if (best < reorder_nodenum()) {
          blockdown(dis.prev);
          dis = dis.next;
        } else if (first == dis) {
          first = dis.prev;
        }
        if (verbose > 1) {
          System.out.print(".");
          System.out.flush();
        }
      }

      if (verbose > 1) {
        System.out.println(" " + reorder_nodenum() + " nodes");
      }
      c++;
    } while (reorder_nodenum() != lastsize);

    return first;
  }

  private void bdd_reorder_auto() {
    if (!bdd_reorder_ready()) {
      return;
    }

    bdd_reorder(bddreordermethod);
    bddreordertimes--;
  }

  private int bdd_reorder_gain() {
    if (usednum_before == 0) {
      return 0;
    }

    return (100 * (usednum_before - usednum_after)) / usednum_before;
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

    bdd_disable_reorder();

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

    bdd_enable_reorder();

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

    bdd_disable_reorder();

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

    bdd_enable_reorder();

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
  public void reorder(ReorderMethod m) {
    int x = m.id;
    bdd_reorder(x);
  }

  @Override
  public void autoReorder(ReorderMethod method) {
    int x = method.id;
    bdd_autoreorder(x);
  }

  @Override
  public void autoReorder(ReorderMethod method, int max) {
    int x = method.id;
    bdd_autoreorder_times(x, max);
  }

  @Override
  public ReorderMethod getReorderMethod() {
    switch (bddreordermethod) {
      case BDD_REORDER_NONE:
        return REORDER_NONE;
      case BDD_REORDER_WIN2:
        return REORDER_WIN2;
      case BDD_REORDER_WIN2ITE:
        return REORDER_WIN2ITE;
      case BDD_REORDER_WIN3:
        return REORDER_WIN3;
      case BDD_REORDER_WIN3ITE:
        return REORDER_WIN3ITE;
      case BDD_REORDER_SIFT:
        return REORDER_SIFT;
      case BDD_REORDER_SIFTITE:
        return REORDER_SIFTITE;
      case BDD_REORDER_RANDOM:
        return REORDER_RANDOM;
      default:
        throw new BDDException();
    }
  }

  @Override
  public int getReorderTimes() {
    return bddreordertimes;
  }

  @Override
  public void disableReorder() {
    bdd_disable_reorder();
  }

  @Override
  public void enableReorder() {
    bdd_enable_reorder();
  }

  @Override
  public int reorderVerbose(int v) {
    return bdd_reorder_verbose(v);
  }

  @Override
  public void setVarOrder(int[] neworder) {
    bdd_setvarorder(neworder);
  }

  static class BddTree {
    int firstVar, lastVar; /* First and last variable in this block */
    int firstLevel, lastLevel; /* First and last level in this block */
    int pos; /* Sifting position */
    int[] seq; /* Sequence of first...last in the current order */
    boolean fixed; /* Are the sub-blocks fixed or may they be reordered */
    boolean interleaved; /* Is this block interleaved with the next one */
    int id; /* A sequential id number given by addblock */
    BddTree next, prev;
    BddTree nextlevel;
  }

  /* Current auto reord. method and number of automatic reorderings left */
  private int bddreordermethod;
  private int bddreordertimes;

  /* Flag for disabling reordering temporarily */
  private int reorderdisabled;

  private BddTree vartree;
  private int blockid;

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

  private int verbose;
  // bddinthandler reorder_handler;
  // bddfilehandler reorder_filehandler;
  // bddsizehandler reorder_nodenum;

  /* Number of live nodes before and after a reordering session */
  private int usednum_before;
  private int usednum_after;

  private void bdd_reorder_init() {
    reorderdisabled = 0;
    vartree = null;

    bdd_clrvarblocks();
    // bdd_reorder_hook(bdd_default_reohandler);
    bdd_reorder_verbose(0);
    bdd_autoreorder_times(BDD_REORDER_NONE, 0);
    // reorder_nodenum = bdd_getnodenum;
    usednum_before = usednum_after = 0;
    blockid = 0;
  }

  private int reorder_nodenum() {
    return bdd_getnodenum();
  }

  private int bdd_getnodenum() {
    return bddnodesize - bddfreenum;
  }

  private int bdd_reorder_verbose(int v) {
    int tmp = verbose;
    verbose = v;
    return tmp;
  }

  private int bdd_autoreorder(int method) {
    int tmp = bddreordermethod;
    bddreordermethod = method;
    bddreordertimes = -1;
    return tmp;
  }

  private int bdd_autoreorder_times(int method, int num) {
    int tmp = bddreordermethod;
    bddreordermethod = method;
    bddreordertimes = num;
    return tmp;
  }

  private static final int BDD_REORDER_NONE = 0;
  private static final int BDD_REORDER_WIN2 = 1;
  private static final int BDD_REORDER_WIN2ITE = 2;
  private static final int BDD_REORDER_SIFT = 3;
  private static final int BDD_REORDER_SIFTITE = 4;
  private static final int BDD_REORDER_WIN3 = 5;
  private static final int BDD_REORDER_WIN3ITE = 6;
  private static final int BDD_REORDER_RANDOM = 7;

  static final int BDD_REORDER_FREE = 0;
  static final int BDD_REORDER_FIXED = 1;

  void bdd_reorder_done() {
    bddtree_del(vartree);
    bdd_operator_reset();
    vartree = null;
  }

  private static void bddtree_del(BddTree t) {
    if (t == null) {
      return;
    }

    bddtree_del(t.nextlevel);
    bddtree_del(t.next);
    t.seq = null;
  }

  private void bdd_clrvarblocks() {
    bddtree_del(vartree);
    vartree = null;
    blockid = 0;
  }

  int NODEHASHr(int var, int l, int h) {
    return (Math.abs(PAIR(l, (h)) % levels[var].size) + levels[var].start);
  }

  private void bdd_setvarorder(int[] neworder) {
    int level;

    /* Do not set order when variable-blocks are used */
    if (vartree != null) {
      bdd_error(BDD_VARBLK);
      return;
    }

    reorder_init();

    for (level = 0; level < bddvarnum; level++) {
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

      if (l.nodenum < (l.size) / 3 || l.nodenum >= (l.size * 3) / 2 && l.size < l.maxsize) {
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
    int n;

    for (n = 0; n < bddvarnum; n++) {
      levels[n].maxsize = bddnodesize / bddvarnum;
      levels[n].start = n * levels[n].maxsize;
      levels[n].size = Math.min(levels[n].maxsize, (levels[n].nodenum * 5) / 4);

      if (levels[n].size >= 4) {
        levels[n].size = bdd_prime_lte(levels[n].size);
      }
    }
  }

  private void reorder_rehashAll() {
    int n;

    reorder_setLevellookup();
    bddfreepos = 0;

    for (n = bddnodesize - 1; n >= 0; n--) {
      SETHASH(n, 0);
    }

    for (n = bddnodesize - 1; n >= 2; n--) {
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
    int n;

    for (n = 0; n < size1; n++) {
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

  private static final boolean SWAPCOUNT = false;

  private int reorder_downSimple(int var0) {
    int toBeProcessed = 0;
    int var1 = bddlevel2var[bddvar2level[var0] + 1];
    int vl0 = levels[var0].start;
    int size0 = levels[var0].size;
    int n;

    levels[var0].nodenum = 0;

    for (n = 0; n < size0; n++) {
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
          if (SWAPCOUNT) {
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
    int n;

    for (n = 0; n < size1; n++) {
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
    return (Math.abs(PAIR(l, h) % levels[var].size) + levels[var].start);
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
    int n;

    reorder_handler(true, reorderstats);

    levels = new levelData[bddvarnum];

    for (n = 0; n < bddvarnum; n++) {
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
    int n;

    for (n = 2, extrootsize = 0; n < bddnodesize; n++) {
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

    for (n = 2, extrootsize = 0; n < bddnodesize; n++) {

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
    int n;

    mtx.rows = new byte[size][];

    for (n = 0; n < size; n++) {
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
      int n;

      /* Update (from previously found) variable dependencies
       * for the interaction matrix */
      for (n = 0; n < bddvarnum; n++) {
        dep[n] |= imatrixDepends(iactmtx, VARr(r) & ~MARK_MASK, n);
      }
    }

    INCREF(r);
  }

  private void addDependencies(boolean[] dep) {
    int n, m;

    for (n = 0; n < bddvarnum; n++) {
      for (m = n; m < bddvarnum; m++) {
        if ((dep[n]) && (dep[m])) {
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
    int n;

    bddfreepos = 0;
    bddfreenum = 0;

    /* No need to zero all hash fields - this is done in mark_roots */

    for (n = bddnodesize - 1; n >= 2; n--) {

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
    int n;

    for (n = 0; n < extrootsize; n++) {
      SETMARK(extroots[n]);
    }
    for (n = 2; n < bddnodesize; n++) {
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
    int n;

    for (n = 0; n < mtx.size; n++) {
      mtx.rows[n] = null;
    }
    mtx.rows = null;
  }

  @Override
  public void addVarBlock(BDD var, boolean fixed) {
    // int x = ((bdd) var)._index;
    int[] set = var.scanSet();
    bdd_addvarblock(set, fixed);
  }

  @Override
  public void addVarBlock(int first, int last, boolean fixed) {
    bdd_intaddvarblock(first, last, fixed);
  }

  @Override
  public void varBlockAll() {
    bdd_varblockall();
  }

  @Override
  public void clearVarBlocks() {
    bdd_clrvarblocks();
  }

  @Override
  public void printOrder() {
    bdd_fprintorder(System.out);
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
  public int reorderGain() {
    return bdd_reorder_gain();
  }

  @Override
  public void printStat() {
    bdd_fprintstat(System.out);
  }

  @Override
  public BDDPairing makePair() {
    bddPair p = new bddPair();
    p.result = new int[bddvarnum];
    int n;
    for (n = 0; n < bddvarnum; n++) {
      p.result[n] = bdd_ithvar(bddlevel2var[n]);
    }

    p.id = update_pairsid();
    p.last = -1;

    bdd_register_pair(p);
    return p;
  }

  @Override
  public void swapVar(int v1, int v2) {
    bdd_swapvar(v1, v2);
  }

  private int bdd_swapvar(int v1, int v2) {
    int l1, l2;

    /* Do not swap when variable-blocks are used */
    if (vartree != null) {
      return bdd_error(BDD_VARBLK);
    }

    /* Don't bother swapping x with x */
    if (v1 == v2) {
      return 0;
    }

    /* Make sure the variable exists */
    if (v1 < 0 || v1 >= bddvarnum || v2 < 0 || v2 >= bddvarnum) {
      return bdd_error(BDD_VAR);
    }

    l1 = bddvar2level[v1];
    l2 = bddvar2level[v2];

    /* Make sure v1 is before v2 */
    if (l1 > l2) {
      int tmp = v1;
      v1 = v2;
      v2 = tmp;
      l1 = bddvar2level[v1];
      l2 = bddvar2level[v2];
    }

    reorder_init();

    /* Move v1 to v2's position */
    while (bddvar2level[v1] < l2) {
      reorder_vardown(v1);
    }

    /* Move v2 to v1's position */
    while (bddvar2level[v2] > l1) {
      reorder_varup(v2);
    }

    reorder_done();

    return 0;
  }

  private void bdd_fprintall(PrintStream out) {
    int n;

    for (n = 0; n < bddnodesize; n++) {
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
    int n;

    out.println("ROOT: " + r);
    if (r < 2) {
      return;
    }

    bdd_mark(r);

    for (n = 0; n < bddnodesize; n++) {
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
    int n, vnum, tmproot;
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
    for (n = 0; n < vnum; n++) {
      loadvar2level[n] = Integer.parseInt(readNext(ifile));
    }

    if (vnum > bddvarnum) {
      bdd_setvarnum(vnum);
    }

    lh_table = new LoadHash[lh_nodenum];

    for (n = 0; n < lh_nodenum; n++) {
      lh_table[n] = new LoadHash();
      lh_table[n].first = -1;
      lh_table[n].next = n + 1;
    }
    lh_table[lh_nodenum - 1].next = -1;
    lh_freepos = 0;

    tmproot = bdd_loaddata(ifile, translate);

    for (n = 0; n < lh_nodenum; n++) {
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
    int key, var, low, high, root = 0, n;

    for (n = 0; n < lh_nodenum; n++) {
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

  private int bdd_addvarblock(int[] v, boolean fixed) {
    BddTree t;
    int first, last;

    if (v.length < 1) {
      return bdd_error(BDD_VARBLK);
    }

    first = last = v[0];

    for (int i : v) {
      if (i < first) {
        first = i;
      }
      if (i > last) {
        last = i;
      }
    }

    if ((t = bddtree_addrange(vartree, first, last, fixed, blockid)) == null) {
      return bdd_error(BDD_VARBLK);
    }

    vartree = t;
    return blockid++;
  }

  private int bdd_intaddvarblock(int first, int last, boolean fixed) {
    BddTree t;

    if (first < 0 || first >= bddvarnum || last < 0 || last >= bddvarnum) {
      return bdd_error(BDD_VAR);
    }

    if ((t = bddtree_addrange(vartree, first, last, fixed, blockid)) == null) {
      return bdd_error(BDD_VARBLK);
    }

    vartree = t;
    return blockid++;
  }

  private BddTree bddtree_addrange_rec(
      BddTree t, BddTree prev, int first, int last, boolean fixed, int id) {
    if (first < 0 || last < 0 || last < first) {
      return null;
    }

    /* Empty tree -> build one */
    if (t == null) {
      t = bddtree_new(id);
      t.firstVar = first;
      t.firstLevel = bddvar2level[first];
      t.fixed = fixed;
      t.seq = new int[last - first + 1];
      t.lastVar = last;
      t.lastLevel = bddvar2level[last];
      update_seq(t);
      t.prev = prev;
      return t;
    }

    /* Check for identity */
    if (first == t.firstVar && last == t.lastVar) {
      return t;
    }

    int firstLev = Math.min(bddvar2level[first], bddvar2level[last]);
    int lastLev = Math.max(bddvar2level[first], bddvar2level[last]);

    /* Inside this section -> insert in next level */
    if (firstLev >= t.firstLevel && lastLev <= t.lastLevel) {
      t.nextlevel = bddtree_addrange_rec(t.nextlevel, null, first, last, fixed, id);
      return t;
    }

    /* Before this section -> insert */
    if (lastLev < t.firstLevel) {
      BddTree tnew = bddtree_new(id);
      tnew.firstVar = first;
      tnew.firstLevel = firstLev;
      tnew.lastVar = last;
      tnew.lastLevel = lastLev;
      tnew.fixed = fixed;
      tnew.seq = new int[last - first + 1];
      update_seq(tnew);
      tnew.next = t;
      tnew.prev = t.prev;
      t.prev = tnew;
      return tnew;
    }

    /* After this this section -> go to next */
    if (firstLev > t.lastLevel) {
      t.next = bddtree_addrange_rec(t.next, t, first, last, fixed, id);
      return t;
    }

    /* Covering this section -> insert above this level */
    if (firstLev <= t.firstLevel) {
      BddTree tnew;
      BddTree dis = t;

      while (true) {
        /* Partial cover ->error */
        if (lastLev >= dis.firstLevel && lastLev < dis.lastLevel) {
          return null;
        }

        if (dis.next == null || last < dis.next.firstLevel) {
          tnew = bddtree_new(id);
          tnew.firstVar = first;
          tnew.firstLevel = firstLev;
          tnew.lastVar = last;
          tnew.lastLevel = lastLev;
          tnew.fixed = fixed;
          tnew.seq = new int[last - first + 1];
          update_seq(tnew);
          tnew.nextlevel = t;
          tnew.next = dis.next;
          tnew.prev = t.prev;
          if (dis.next != null) {
            dis.next.prev = tnew;
          }
          dis.next = null;
          t.prev = null;
          return tnew;
        }

        dis = dis.next;
      }
    }

    return null;
  }

  private void update_seq(BddTree t) {
    int n;
    int low = t.firstVar;
    int high = t.lastVar;

    for (n = t.firstVar; n <= t.lastVar; n++) {
      if (bddvar2level[n] < bddvar2level[low]) {
        low = n;
      }
      if (bddvar2level[n] > bddvar2level[high]) {
        high = n;
      }
    }

    for (n = t.firstVar; n <= t.lastVar; n++) {
      t.seq[bddvar2level[n] - bddvar2level[low]] = n;
    }

    t.firstLevel = bddvar2level[low];
    t.lastLevel = bddvar2level[high];
  }

  private BddTree bddtree_addrange(BddTree t, int first, int last, boolean fixed, int id) {
    return bddtree_addrange_rec(t, null, first, last, fixed, id);
  }

  private void bdd_varblockall() {
    int n;

    for (n = 0; n < bddvarnum; n++) {
      bdd_intaddvarblock(n, n, true);
    }
  }

  private static void print_order_rec(PrintStream o, BddTree t, int level) {
    if (t == null) {
      return;
    }

    if (t.nextlevel != null) {
      for (int i = 0; i < level; ++i) {
        o.print("   ");
      }
      // todo: better reorder id printout
      o.print(right(t.id, 3));
      if (t.interleaved) {
        o.print('x');
      }
      o.println("{\n");

      print_order_rec(o, t.nextlevel, level + 1);

      for (int i = 0; i < level; ++i) {
        o.print("   ");
      }
      // todo: better reorder id printout
      o.print(right(t.id, 3));
      o.println("}\n");

      print_order_rec(o, t.next, level);
    } else {
      for (int i = 0; i < level; ++i) {
        o.print("   ");
      }
      // todo: better reorder id printout
      o.print(right(t.id, 3));
      if (t.interleaved) {
        o.print('x');
      }
      o.println();

      print_order_rec(o, t.next, level);
    }
  }

  private void bdd_fprintorder(PrintStream ofile) {
    print_order_rec(ofile, vartree, 0);
  }

  private void bdd_fprintstat(PrintStream out) {
    CacheStats s = cachestats;
    out.print(s.toString());
  }

  public void validateAll() {
    validate_all();
  }

  public void validateBDD(BDD b) {
    validate(((BDDImpl) b)._index);
  }

  private void validate_all() {
    int n;
    for (n = bddnodesize - 1; n >= 2; n--) {
      if (HASREF(n)) {
        validate(n);
      }
    }
  }

  private void validate(int k) {
    validate(k, -1);
  }

  private void validate(int k, int lastLevel) {
    if (k < 2) {
      return;
    }
    int lev = LEVEL(k);
    // System.out.println("Level("+k+") = "+lev);
    if (lev <= lastLevel) {
      throw new BDDException(lev + " <= " + lastLevel);
    }
    // System.out.println("Low:");
    validate(LOW(k), lev);
    // System.out.println("High:");
    validate(HIGH(k), lev);
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
    int b;

    if (src == 0) {
      return 0;
    }

    for (b = 31; b > 0; --b) {
      if (BitIsSet(src, b)) {
        return b + 1;
      }
    }

    return 1;
  }

  private static boolean isWitness(int witness, int src) {
    int bitNum = numberOfBits(src - 1) - 1;
    int d = 1;
    int i;

    for (i = bitNum; i >= 0; --i) {
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
    int n;

    for (n = 0; n < CHECKTIMES; ++n) {
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

  public JFactory cloneFactory() {
    JFactory INSTANCE = new JFactory();
    if (applycache != null) {
      INSTANCE.applycache = this.applycache.copy();
    }
    if (itecache != null) {
      INSTANCE.itecache = this.itecache.copy();
    }
    if (quantcache != null) {
      INSTANCE.quantcache = this.quantcache.copy();
    }
    INSTANCE.appexcache = this.appexcache.copy();
    if (replacecache != null) {
      INSTANCE.replacecache = this.replacecache.copy();
    }
    if (misccache != null) {
      INSTANCE.misccache = this.misccache.copy();
    }
    if (countcache != null) {
      INSTANCE.countcache = this.countcache.copy();
    }
    // TODO: potential difference here (!)
    INSTANCE.rng = new Random();
    INSTANCE.verbose = this.verbose;
    INSTANCE.cachestats.copyFrom(this.cachestats);

    INSTANCE.bddrunning = this.bddrunning;
    INSTANCE.bdderrorcond = this.bdderrorcond;
    INSTANCE.bddnodesize = this.bddnodesize;
    INSTANCE.bddmaxnodesize = this.bddmaxnodesize;
    INSTANCE.bddmaxnodeincrease = this.bddmaxnodeincrease;
    INSTANCE.bddfreepos = this.bddfreepos;
    INSTANCE.bddfreenum = this.bddfreenum;
    INSTANCE.bddproduced = this.bddproduced;
    INSTANCE.bddvarnum = this.bddvarnum;

    INSTANCE.gbcollectnum = this.gbcollectnum;
    INSTANCE.cachesize = this.cachesize;
    INSTANCE.gbcclock = this.gbcclock;
    INSTANCE.usednodes_nextreorder = this.usednodes_nextreorder;

    INSTANCE.bddrefstacktop = this.bddrefstacktop;
    INSTANCE.bddresized = this.bddresized;
    INSTANCE.minfreenodes = this.minfreenodes;
    INSTANCE.bddnodes = new int[this.bddnodes.length];
    System.arraycopy(this.bddnodes, 0, INSTANCE.bddnodes, 0, this.bddnodes.length);
    INSTANCE.bddrefstack = new int[this.bddrefstack.length];
    System.arraycopy(this.bddrefstack, 0, INSTANCE.bddrefstack, 0, this.bddrefstack.length);
    INSTANCE.bddvar2level = new int[this.bddvar2level.length];
    System.arraycopy(this.bddvar2level, 0, INSTANCE.bddvar2level, 0, this.bddvar2level.length);
    INSTANCE.bddlevel2var = new int[this.bddlevel2var.length];
    System.arraycopy(this.bddlevel2var, 0, INSTANCE.bddlevel2var, 0, this.bddlevel2var.length);
    INSTANCE.bddvarset = new int[this.bddvarset.length];
    System.arraycopy(this.bddvarset, 0, INSTANCE.bddvarset, 0, this.bddvarset.length);

    INSTANCE.domain = new BDDDomain[this.domain.length];
    for (int i = 0; i < INSTANCE.domain.length; ++i) {
      INSTANCE.domain[i] = INSTANCE.createDomain(i, this.domain[i].realsize);
    }
    return INSTANCE;
  }

  /**
   * Use this function to translate BDD's from a JavaFactory into its clone. This will only work
   * immediately after cloneFactory() is called, and before any other BDD operations are performed.
   *
   * @param that BDD in old factory
   * @return a BDD in the new factory
   */
  public BDD copyNode(BDD that) {
    BDDImpl b = (BDDImpl) that;
    return makeBDD(b._index);
  }
}

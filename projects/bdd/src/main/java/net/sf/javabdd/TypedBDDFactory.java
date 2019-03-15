// TypedBDDFactory.java, created Jan 29, 2003 9:50:57 PM by jwhaley
// Copyright (C) 2003 John Whaley
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This BDD factory keeps track of what domains each BDD uses, and complains if you try to do an
 * operation where the domains do not match.
 *
 * @see net.sf.javabdd.BDDFactory
 * @author John Whaley
 * @version $Id: TypedBDDFactory.java,v 1.8 2005/05/21 08:47:10 joewhaley Exp $
 */
public class TypedBDDFactory extends BDDFactory {

  static PrintStream out = System.out;
  static boolean STACK_TRACES = true;

  BDDFactory factory;

  public TypedBDDFactory(BDDFactory f) {
    this.factory = f;
  }

  public static BDDFactory init(int nodenum, int cachesize) {
    BDDFactory a = BDDFactory.init(nodenum, cachesize);
    return new TypedBDDFactory(a);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#zero()
   */
  @Override
  public BDD zero() {
    return new TypedBDD(factory.zero(), makeSet());
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#one()
   */
  @Override
  public BDD one() {
    Set s = makeSet();
    // Set s = allDomains();
    return new TypedBDD(factory.one(), s);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#initialize(int, int)
   */
  @Override
  protected void initialize(int nodenum, int cachesize) {
    factory.initialize(nodenum, cachesize);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#isInitialized()
   */
  @Override
  public boolean isInitialized() {
    return factory.isInitialized();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#done()
   */
  @Override
  public void done() {
    factory.done();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setError(int)
   */
  @Override
  public void setError(int code) {
    factory.setError(code);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#clearError()
   */
  @Override
  public void clearError() {
    factory.clearError();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setMaxNodeNum(int)
   */
  @Override
  public int setMaxNodeNum(int size) {
    return factory.setMaxNodeNum(size);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setNodeTableSize(int)
   */
  @Override
  public int setNodeTableSize(int size) {
    return factory.setNodeTableSize(size);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setCacheSize(int)
   */
  @Override
  public int setCacheSize(int size) {
    return factory.setCacheSize(size);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setMinFreeNodes(double)
   */
  @Override
  public double setMinFreeNodes(double x) {
    return factory.setMinFreeNodes(x);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setIncreaseFactor(double)
   */
  @Override
  public double setIncreaseFactor(double x) {
    return factory.setIncreaseFactor(x);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setMaxIncrease(int)
   */
  @Override
  public int setMaxIncrease(int x) {
    return factory.setMaxIncrease(x);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setCacheRatio(double)
   */
  @Override
  public double setCacheRatio(double x) {
    return factory.setCacheRatio(x);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#varNum()
   */
  @Override
  public int varNum() {
    return factory.varNum();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setVarNum(int)
   */
  @Override
  public int setVarNum(int num) {
    return factory.setVarNum(num);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#duplicateVar(int)
   */
  @Override
  public int duplicateVar(int var) {
    return factory.duplicateVar(var);
  }

  public BDDDomain whichDomain(int var) {
    for (int i = 0; i < numberOfDomains(); ++i) {
      int[] vars = getDomain(i).vars();
      for (int j = 0; j < vars.length; ++j) {
        if (var == vars[j]) {
          return getDomain(i);
        }
      }
    }
    return null;
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#ithVar(int)
   */
  @Override
  public BDD ithVar(int var) {
    Set s = makeSet();
    // BDDDomain d = whichDomain(var);
    // if (d != null) s.add(d);
    return new TypedBDD(factory.ithVar(var), s);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#nithVar(int)
   */
  @Override
  public BDD nithVar(int var) {
    Set s = makeSet();
    // BDDDomain d = whichDomain(var);
    // if (d != null) s.add(d);
    return new TypedBDD(factory.nithVar(var), s);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#printAll()
   */
  @Override
  public void printAll() {
    factory.printAll();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#printTable(net.sf.javabdd.BDD)
   */
  @Override
  public void printTable(BDD b) {
    TypedBDD bdd1 = (TypedBDD) b;
    factory.printTable(bdd1.bdd);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#load(java.lang.String)
   */
  @Override
  public BDD load(String filename) throws IOException {
    // TODO domains?
    Set d = makeSet();
    return new TypedBDD(factory.load(filename), d);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#save(java.lang.String, net.sf.javabdd.BDD)
   */
  @Override
  public void save(String filename, BDD var) throws IOException {
    TypedBDD bdd1 = (TypedBDD) var;
    factory.save(filename, bdd1.bdd);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#level2Var(int)
   */
  @Override
  public int level2Var(int level) {
    return factory.level2Var(level);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#var2Level(int)
   */
  @Override
  public int var2Level(int var) {
    return factory.var2Level(var);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#reorder(net.sf.javabdd.BDDFactory.ReorderMethod)
   */
  @Override
  public void reorder(ReorderMethod m) {
    factory.reorder(m);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#autoReorder(net.sf.javabdd.BDDFactory.ReorderMethod)
   */
  @Override
  public void autoReorder(ReorderMethod method) {
    factory.autoReorder(method);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#autoReorder(net.sf.javabdd.BDDFactory.ReorderMethod, int)
   */
  @Override
  public void autoReorder(ReorderMethod method, int max) {
    factory.autoReorder(method, max);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#getReorderMethod()
   */
  @Override
  public ReorderMethod getReorderMethod() {
    return factory.getReorderMethod();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#getReorderTimes()
   */
  @Override
  public int getReorderTimes() {
    return factory.getReorderTimes();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#disableReorder()
   */
  @Override
  public void disableReorder() {
    factory.disableReorder();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#enableReorder()
   */
  @Override
  public void enableReorder() {
    factory.enableReorder();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#reorderVerbose(int)
   */
  @Override
  public int reorderVerbose(int v) {
    return factory.reorderVerbose(v);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#setVarOrder(int[])
   */
  @Override
  public void setVarOrder(int[] neworder) {
    factory.setVarOrder(neworder);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#addVarBlock(net.sf.javabdd.BDD, boolean)
   */
  @Override
  public void addVarBlock(BDD var, boolean fixed) {
    TypedBDD bdd1 = (TypedBDD) var;
    factory.addVarBlock(bdd1.bdd, fixed);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#addVarBlock(int, int, boolean)
   */
  @Override
  public void addVarBlock(int first, int last, boolean fixed) {
    factory.addVarBlock(first, last, fixed);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#varBlockAll()
   */
  @Override
  public void varBlockAll() {
    factory.varBlockAll();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#clearVarBlocks()
   */
  @Override
  public void clearVarBlocks() {
    factory.clearVarBlocks();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#printOrder()
   */
  @Override
  public void printOrder() {
    factory.printOrder();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#nodeCount(java.util.Collection)
   */
  @Override
  public int nodeCount(Collection r) {
    Collection s = new LinkedList();
    for (Iterator i = r.iterator(); i.hasNext(); ) {
      TypedBDD bdd1 = (TypedBDD) i.next();
      s.add(bdd1.bdd);
    }
    return factory.nodeCount(s);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#getAllocNum()
   */
  @Override
  public int getNodeTableSize() {
    return factory.getNodeTableSize();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#getNodeNum()
   */
  @Override
  public int getNodeNum() {
    return factory.getNodeNum();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#getCacheSize()
   */
  @Override
  public int getCacheSize() {
    return factory.getCacheSize();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#reorderGain()
   */
  @Override
  public int reorderGain() {
    return factory.reorderGain();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#printStat()
   */
  @Override
  public void printStat() {
    factory.printStat();
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#makePair()
   */
  @Override
  public BDDPairing makePair() {
    return new TypedBDDPairing(factory.makePair());
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#swapVar(int, int)
   */
  @Override
  public void swapVar(int v1, int v2) {
    factory.swapVar(v1, v2);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#createDomain(int, BigInteger)
   */
  @Override
  protected BDDDomain createDomain(int a, BigInteger b) {
    return new TypedBDDDomain(factory.getDomain(a), a, b);
  }

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#createBitVector(int)
   */
  @Override
  protected BDDBitVector createBitVector(int a) {
    return factory.createBitVector(a);
  }

  @Override
  public BDDDomain[] extDomain(long[] domainSizes) {
    factory.extDomain(domainSizes);
    return super.extDomain(domainSizes);
  }

  public static Set makeSet() {
    // return SortedArraySet.FACTORY.makeSet(domain_comparator);
    return new TreeSet(domain_comparator);
  }

  public static Set makeSet(Set s) {
    // Set r = SortedArraySet.FACTORY.makeSet(domain_comparator);
    Set r = new TreeSet(domain_comparator);
    r.addAll(s);
    return r;
  }

  public Set allDomains() {
    Set r = makeSet();
    for (int i = 0; i < factory.numberOfDomains(); ++i) {
      r.add(factory.getDomain(i));
    }
    return r;
  }

  public static Map makeMap() {
    return new TreeMap(domain_comparator);
  }

  public static String domainNames(Set dom) {
    StringBuffer sb = new StringBuffer();
    for (Iterator i = dom.iterator(); i.hasNext(); ) {
      BDDDomain d = (BDDDomain) i.next();
      sb.append(d.getName());
      if (i.hasNext()) {
        sb.append(',');
      }
    }
    return sb.toString();
  }

  public static final Comparator domain_comparator =
      new Comparator() {

        @Override
        public int compare(Object arg0, Object arg1) {
          BDDDomain d1 = (BDDDomain) arg0;
          BDDDomain d2 = (BDDDomain) arg1;
          if (d1.getIndex() < d2.getIndex()) {
            return -1;
          } else if (d1.getIndex() > d2.getIndex()) {
            return 1;
          } else {
            return 0;
          }
        }
      };

  /**
   * A BDD with types (domains) attached to it.
   *
   * @author jwhaley
   * @version $Id: TypedBDDFactory.java,v 1.8 2005/05/21 08:47:10 joewhaley Exp $
   */
  public class TypedBDD extends BDD {

    final BDD bdd;
    final Set dom;

    public TypedBDD(BDD bdd, Set dom) {
      this.bdd = bdd;
      this.dom = dom;
    }

    /** Returns the set of domains that this BDD uses. */
    public Set getDomainSet() {
      return dom;
    }

    /** Changes this BDD's domains to be the given set. */
    public void setDomains(Set d) {
      dom.clear();
      dom.addAll(d);
    }

    /** Changes this BDD's domain to be the given domain. */
    public void setDomains(BDDDomain d) {
      dom.clear();
      dom.add(d);
    }

    /** Changes this BDD's domains to be the given domains. */
    public void setDomains(BDDDomain d1, BDDDomain d2) {
      dom.clear();
      dom.add(d1);
      dom.add(d2);
    }

    /** Changes this BDD's domains to be the given domains. */
    public void setDomains(BDDDomain d1, BDDDomain d2, BDDDomain d3) {
      dom.clear();
      dom.add(d1);
      dom.add(d2);
      dom.add(d3);
    }

    /** Changes this BDD's domains to be the given domains. */
    public void setDomains(BDDDomain d1, BDDDomain d2, BDDDomain d3, BDDDomain d4) {
      dom.clear();
      dom.add(d1);
      dom.add(d2);
      dom.add(d3);
      dom.add(d4);
    }

    /** Changes this BDD's domains to be the given domains. */
    public void setDomains(BDDDomain d1, BDDDomain d2, BDDDomain d3, BDDDomain d4, BDDDomain d5) {
      dom.clear();
      dom.add(d1);
      dom.add(d2);
      dom.add(d3);
      dom.add(d4);
      dom.add(d5);
    }

    /** Returns the set of domains in BDD format. */
    BDD getDomains() {
      BDD b = factory.one();
      for (Iterator i = dom.iterator(); i.hasNext(); ) {
        TypedBDDDomain d = (TypedBDDDomain) i.next();
        b.andWith(d.domain.set());
      }
      return b;
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#getFactory()
     */
    @Override
    public BDDFactory getFactory() {
      return TypedBDDFactory.this;
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#isZero()
     */
    @Override
    public boolean isZero() {
      return bdd.isZero();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#isOne()
     */
    @Override
    public boolean isOne() {
      return bdd.isOne();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#var()
     */
    @Override
    public int var() {
      return bdd.var();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#high()
     */
    @Override
    public BDD high() {
      return new TypedBDD(bdd.high(), makeSet(dom));
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#low()
     */
    @Override
    public BDD low() {
      return new TypedBDD(bdd.low(), makeSet(dom));
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#id()
     */
    @Override
    public BDD id() {
      return new TypedBDD(bdd.id(), makeSet(dom));
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#not()
     */
    @Override
    public BDD not() {
      return new TypedBDD(bdd.not(), makeSet(dom));
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#ite(net.sf.javabdd.BDD, net.sf.javabdd.BDD)
     */
    @Override
    public BDD ite(BDD thenBDD, BDD elseBDD) {
      TypedBDD bdd1 = (TypedBDD) thenBDD;
      TypedBDD bdd2 = (TypedBDD) elseBDD;
      Set newDom = makeSet();
      newDom.addAll(dom);
      newDom.addAll(bdd1.dom);
      newDom.addAll(bdd2.dom);
      return new TypedBDD(bdd.ite(bdd1.bdd, bdd2.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#relprod(net.sf.javabdd.BDD, net.sf.javabdd.BDD)
     */
    @Override
    public BDD relprod(BDD that, BDD var) {
      TypedBDD bdd1 = (TypedBDD) that;
      TypedBDD bdd2 = (TypedBDD) var;
      Set newDom = makeSet();
      newDom.addAll(dom);
      newDom.addAll(bdd1.dom);
      if (!newDom.containsAll(bdd2.dom)) {
        out.println("Warning! Quantifying domain that doesn't exist: " + domainNames(bdd2.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      newDom.removeAll(bdd2.dom);
      return new TypedBDD(bdd.relprod(bdd1.bdd, bdd2.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#compose(net.sf.javabdd.BDD, int)
     */
    @Override
    public BDD compose(BDD g, int var) {
      TypedBDD bdd1 = (TypedBDD) g;
      // TODO How does this change the domains?
      Set newDom = makeSet();
      newDom.addAll(dom);
      return new TypedBDD(bdd.compose(bdd1.bdd, var), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#veccompose(net.sf.javabdd.BDDPairing)
     */
    @Override
    public BDD veccompose(BDDPairing pair) {
      TypedBDDPairing p = (TypedBDDPairing) pair;
      // TODO How does this change the domains?
      Set newDom = makeSet();
      newDom.addAll(dom);
      return new TypedBDD(bdd.veccompose(p.pairing), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#constrain(net.sf.javabdd.BDD)
     */
    @Override
    public BDD constrain(BDD that) {
      TypedBDD bdd1 = (TypedBDD) that;
      // TODO How does this change the domains?
      Set newDom = makeSet();
      newDom.addAll(dom);
      return new TypedBDD(bdd.constrain(bdd1.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#exist(net.sf.javabdd.BDD)
     */
    @Override
    public BDD exist(BDD var) {
      TypedBDD bdd1 = (TypedBDD) var;
      Set newDom = makeSet();
      newDom.addAll(dom);
      if (!newDom.containsAll(bdd1.dom)) {
        out.println("Warning! Quantifying domain that doesn't exist: " + domainNames(bdd1.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      newDom.removeAll(bdd1.dom);
      return new TypedBDD(bdd.exist(bdd1.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#forAll(net.sf.javabdd.BDD)
     */
    @Override
    public BDD forAll(BDD var) {
      TypedBDD bdd1 = (TypedBDD) var;
      Set newDom = makeSet();
      newDom.addAll(dom);
      if (!newDom.containsAll(bdd1.dom)) {
        out.println("Warning! Quantifying domain that doesn't exist: " + domainNames(bdd1.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      newDom.removeAll(bdd1.dom);
      return new TypedBDD(bdd.forAll(bdd1.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#unique(net.sf.javabdd.BDD)
     */
    @Override
    public BDD unique(BDD var) {
      TypedBDD bdd1 = (TypedBDD) var;
      Set newDom = makeSet();
      newDom.addAll(dom);
      if (!newDom.containsAll(bdd1.dom)) {
        out.println("Warning! Quantifying domain that doesn't exist: " + domainNames(bdd1.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      newDom.removeAll(bdd1.dom);
      return new TypedBDD(bdd.unique(bdd1.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#restrict(net.sf.javabdd.BDD)
     */
    @Override
    public BDD restrict(BDD var) {
      TypedBDD bdd1 = (TypedBDD) var;
      Set newDom = makeSet();
      newDom.addAll(dom);
      if (!newDom.containsAll(bdd1.dom)) {
        out.println("Warning! Restricting domain that doesn't exist: " + domainNames(bdd1.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      if (bdd1.bdd.satCount(bdd1.getDomains()) > 1.0) {
        out.println("Warning! Using restrict with more than one value");
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      newDom.removeAll(bdd1.dom);
      return new TypedBDD(bdd.restrict(bdd1.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#restrictWith(net.sf.javabdd.BDD)
     */
    @Override
    public BDD restrictWith(BDD var) {
      TypedBDD bdd1 = (TypedBDD) var;
      if (!dom.containsAll(bdd1.dom)) {
        out.println("Warning! Restricting domain that doesn't exist: " + domainNames(bdd1.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      if (bdd1.bdd.satCount(bdd1.getDomains()) > 1.0) {
        out.println("Warning! Using restrict with more than one value");
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      dom.removeAll(bdd1.dom);
      bdd.restrictWith(bdd1.bdd);
      return this;
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#simplify(net.sf.javabdd.BDD)
     */
    @Override
    public BDD simplify(BDD d) {
      TypedBDD bdd1 = (TypedBDD) d;
      // TODO How does this change the domains?
      Set newDom = makeSet();
      newDom.addAll(dom);
      return new TypedBDD(bdd.simplify(bdd1.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#support()
     */
    @Override
    public BDD support() {
      // TODO How does this change the domains?
      Set newDom = makeSet();
      newDom.addAll(dom);
      return new TypedBDD(bdd.support(), newDom);
    }

    void applyHelper(Set newDom, TypedBDD bdd0, TypedBDD bdd1, BDDOp opr) {
      switch (opr.id) {
        case 1: // xor
        case 2: // or
        case 4: // nor
        case 5: // imp
        case 6: // biimp
        case 7: // diff
        case 8: // less
        case 9: // invimp
          if (!bdd0.isZero() && !bdd1.isZero() && !newDom.equals(bdd1.dom)) {
            out.println(
                "Warning! Or'ing BDD with different domains: "
                    + domainNames(newDom)
                    + " != "
                    + domainNames(bdd1.dom));
            if (STACK_TRACES) {
              new Exception().printStackTrace(out);
            }
          }
          // fallthrough
        case 0: // and
        case 3: // nand
          newDom.addAll(bdd1.dom);
          break;
        default:
          throw new BDDException();
      }
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#apply(net.sf.javabdd.BDD, net.sf.javabdd.BDDFactory.BDDOp)
     */
    @Override
    public BDD apply(BDD that, BDDOp opr) {
      TypedBDD bdd1 = (TypedBDD) that;
      Set newDom = makeSet();
      newDom.addAll(dom);
      applyHelper(newDom, this, bdd1, opr);
      return new TypedBDD(bdd.apply(bdd1.bdd, opr), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#applyWith(net.sf.javabdd.BDD, net.sf.javabdd.BDDFactory.BDDOp)
     */
    @Override
    public BDD applyWith(BDD that, BDDOp opr) {
      TypedBDD bdd1 = (TypedBDD) that;
      applyHelper(dom, this, bdd1, opr);
      bdd.applyWith(bdd1.bdd, opr);
      return this;
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#applyAll(net.sf.javabdd.BDD, net.sf.javabdd.BDDFactory.BDDOp, net.sf.javabdd.BDD)
     */
    @Override
    public BDD applyAll(BDD that, BDDOp opr, BDD var) {
      TypedBDD bdd1 = (TypedBDD) that;
      Set newDom = makeSet();
      newDom.addAll(dom);
      applyHelper(newDom, this, bdd1, opr);
      TypedBDD bdd2 = (TypedBDD) var;
      if (!newDom.containsAll(bdd2.dom)) {
        out.println("Warning! Quantifying domain that doesn't exist: " + domainNames(bdd2.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      newDom.removeAll(bdd2.dom);
      out.println(
          domainNames(dom)
              + " "
              + opr
              + " "
              + domainNames(bdd1.dom)
              + " / "
              + domainNames(bdd2.dom)
              + " = "
              + domainNames(newDom));
      return new TypedBDD(bdd.applyAll(bdd1.bdd, opr, bdd2.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#applyEx(net.sf.javabdd.BDD, net.sf.javabdd.BDDFactory.BDDOp, net.sf.javabdd.BDD)
     */
    @Override
    public BDD applyEx(BDD that, BDDOp opr, BDD var) {
      TypedBDD bdd1 = (TypedBDD) that;
      Set newDom = makeSet();
      newDom.addAll(dom);
      applyHelper(newDom, this, bdd1, opr);
      TypedBDD bdd2 = (TypedBDD) var;
      if (!newDom.containsAll(bdd2.dom)) {
        out.println("Warning! Quantifying domain that doesn't exist: " + domainNames(bdd2.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      newDom.removeAll(bdd2.dom);
      out.println(
          domainNames(dom)
              + " "
              + opr
              + " "
              + domainNames(bdd1.dom)
              + " / "
              + domainNames(bdd2.dom)
              + " = "
              + domainNames(newDom));
      return new TypedBDD(bdd.applyEx(bdd1.bdd, opr, bdd2.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#applyUni(net.sf.javabdd.BDD, net.sf.javabdd.BDDFactory.BDDOp, net.sf.javabdd.BDD)
     */
    @Override
    public BDD applyUni(BDD that, BDDOp opr, BDD var) {
      TypedBDD bdd1 = (TypedBDD) that;
      Set newDom = makeSet();
      newDom.addAll(dom);
      applyHelper(newDom, this, bdd1, opr);
      TypedBDD bdd2 = (TypedBDD) var;
      if (!newDom.containsAll(bdd2.dom)) {
        out.println("Warning! Quantifying domain that doesn't exist: " + domainNames(bdd2.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      newDom.removeAll(bdd2.dom);
      out.println(
          domainNames(dom)
              + " "
              + opr
              + " "
              + domainNames(bdd1.dom)
              + " / "
              + domainNames(bdd2.dom)
              + " = "
              + domainNames(newDom));
      return new TypedBDD(bdd.applyUni(bdd1.bdd, opr, bdd2.bdd), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#satOne()
     */
    @Override
    public BDD satOne() {
      return new TypedBDD(bdd.satOne(), makeSet(dom));
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#fullSatOne()
     */
    @Override
    public BDD fullSatOne() {
      return new TypedBDD(bdd.fullSatOne(), allDomains());
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#satOne(net.sf.javabdd.BDD, boolean)
     */
    @Override
    public BDD satOne(BDD var, boolean pol) {
      TypedBDD bdd1 = (TypedBDD) var;
      Set newDom = makeSet();
      newDom.addAll(dom);
      if (!newDom.containsAll(bdd1.dom)) {
        out.println("Warning! Selecting domain that doesn't exist: " + domainNames(bdd1.dom));
        if (STACK_TRACES) {
          new Exception().printStackTrace(out);
        }
      }
      newDom.addAll(bdd1.dom);
      return new TypedBDD(bdd.satOne(bdd1.bdd, pol), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#allsat()
     */
    @Override
    public AllSatIterator allsat() {
      return bdd.allsat();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#replace(net.sf.javabdd.BDDPairing)
     */
    @Override
    public BDD replace(BDDPairing pair) {
      TypedBDDPairing tpair = (TypedBDDPairing) pair;
      Set newDom = makeSet();
      newDom.addAll(dom);
      for (Iterator i = tpair.domMap.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry e = (Map.Entry) i.next();
        BDDDomain d_from = (BDDDomain) e.getKey();
        BDDDomain d_to = (BDDDomain) e.getValue();
        // System.out.println("Replace "+domainNames(dom)+" ("+d_from+"->"+d_to+")");
        if (!dom.contains(d_from)) {
          out.println("Warning! Replacing domain that doesn't exist: " + d_from.getName());
          new Exception().printStackTrace();
        }
        if (dom.contains(d_to) && !tpair.domMap.containsKey(d_to)) {
          out.println("Warning! Overwriting domain that exists: " + d_to.getName());
          new Exception().printStackTrace();
        }
      }
      newDom.removeAll(tpair.domMap.keySet());
      newDom.addAll(tpair.domMap.values());
      // System.out.println("Result = "+domainNames(newDom));
      return new TypedBDD(bdd.replace(tpair.pairing), newDom);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#replaceWith(net.sf.javabdd.BDDPairing)
     */
    @Override
    public BDD replaceWith(BDDPairing pair) {
      TypedBDDPairing tpair = (TypedBDDPairing) pair;
      for (Iterator i = tpair.domMap.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry e = (Map.Entry) i.next();
        BDDDomain d_from = (BDDDomain) e.getKey();
        BDDDomain d_to = (BDDDomain) e.getValue();
        if (!dom.contains(d_from)) {
          out.println("Warning! Replacing domain that doesn't exist: " + d_from.getName());
          new Exception().printStackTrace();
        }
        if (dom.contains(d_to) && !tpair.domMap.containsKey(d_to)) {
          out.println("Warning! Overwriting domain that exists: " + d_to.getName());
          new Exception().printStackTrace();
        }
      }
      dom.removeAll(tpair.domMap.keySet());
      dom.addAll(tpair.domMap.values());
      bdd.replaceWith(tpair.pairing);
      return this;
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#nodeCount()
     */
    @Override
    public int nodeCount() {
      return bdd.nodeCount();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#pathCount()
     */
    @Override
    public double pathCount() {
      return bdd.pathCount();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#satCount()
     */
    @Override
    public double satCount() {
      return bdd.satCount();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#satCount(net.sf.javabdd.BDD)
     */
    @Override
    public double satCount(BDD set) {
      TypedBDD bdd1 = (TypedBDD) set;
      if (!bdd.isZero() && !bdd1.dom.equals(dom)) {
        out.println(
            "Warning! satCount on the wrong domains: "
                + domainNames(dom)
                + " != "
                + domainNames(bdd1.dom));
        new Exception().printStackTrace();
      }
      return bdd.satCount(bdd1.bdd);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#varProfile()
     */
    @Override
    public int[] varProfile() {
      return bdd.varProfile();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#equals(net.sf.javabdd.BDD)
     */
    @Override
    public boolean equals(BDD that) {
      TypedBDD bdd1 = (TypedBDD) that;
      if (!dom.containsAll(bdd1.dom)) {
        out.println("Warning! Comparing domain that doesn't exist: " + domainNames(bdd1.dom));
      }
      return bdd.equals(bdd1.bdd);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#hashCode()
     */
    @Override
    public int hashCode() {
      return bdd.hashCode();
    }

    @Override
    public BDDIterator iterator(BDD var) {
      TypedBDD bdd1 = (TypedBDD) var;
      if (!dom.equals(bdd1.dom)) {
        out.println(
            "Warning! iterator on the wrong domain(s): "
                + domainNames(dom)
                + " != "
                + domainNames(bdd1.dom));
      }
      return super.iterator(var);
    }

    public BDDIterator iterator() {
      Set newDom = makeSet();
      newDom.addAll(dom);
      return super.iterator(new TypedBDD(getDomains(), newDom));
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDD#free()
     */
    @Override
    public void free() {
      bdd.free();
      dom.clear();
    }
  }

  private class TypedBDDDomain extends BDDDomain {

    BDDDomain domain;

    /**
     * @param index
     * @param range
     */
    protected TypedBDDDomain(BDDDomain domain, int index, BigInteger range) {
      super(index, range);
      this.domain = domain;
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDDDomain#getFactory()
     */
    @Override
    public BDDFactory getFactory() {
      return TypedBDDFactory.this;
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDDDomain#ithVar(long)
     */
    @Override
    public BDD ithVar(long val) {
      BDD v = domain.ithVar(val);
      Set s = makeSet();
      s.add(this);
      return new TypedBDD(v, s);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDDDomain#domain()
     */
    @Override
    public BDD domain() {
      BDD v = domain.domain();
      Set s = makeSet();
      s.add(this);
      return new TypedBDD(v, s);
    }

    @Override
    public BDD buildAdd(BDDDomain that, int bits, long value) {
      TypedBDDDomain d = (TypedBDDDomain) that;
      BDD v = domain.buildAdd(d.domain, bits, value);
      Set s = makeSet();
      s.add(this);
      s.add(that);
      return new TypedBDD(v, s);
    }

    @Override
    public BDD buildEquals(BDDDomain that) {
      TypedBDDDomain d = (TypedBDDDomain) that;
      BDD v = domain.buildEquals(d.domain);
      Set s = makeSet();
      s.add(this);
      s.add(that);
      return new TypedBDD(v, s);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDDDomain#set()
     */
    @Override
    public BDD set() {
      BDD v = domain.set();
      Set s = makeSet();
      s.add(this);
      return new TypedBDD(v, s);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDDDomain#varRange(long, long)
     */
    @Override
    public BDD varRange(BigInteger lo, BigInteger hi) {
      BDD v = domain.varRange(lo, hi);
      Set s = makeSet();
      s.add(this);
      return new TypedBDD(v, s);
    }
  }

  private static class TypedBDDPairing extends BDDPairing {

    final Map domMap;
    final BDDPairing pairing;

    TypedBDDPairing(BDDPairing pairing) {
      this.domMap = makeMap();
      this.pairing = pairing;
    }

    @Override
    public void set(BDDDomain p1, BDDDomain p2) {
      if (domMap.containsValue(p2)) {
        out.println("Warning! Set domain that already exists: " + p2.getName());
      }
      domMap.put(p1, p2);
      pairing.set(p1, p2);
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDDPairing#set(int, int)
     */
    @Override
    public void set(int oldvar, int newvar) {
      pairing.set(oldvar, newvar);
      // throw new BDDException();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDDPairing#set(int, net.sf.javabdd.BDD)
     */
    @Override
    public void set(int oldvar, BDD newvar) {
      throw new BDDException();
    }

    /* (non-Javadoc)
     * @see net.sf.javabdd.BDDPairing#reset()
     */
    @Override
    public void reset() {
      domMap.clear();
      pairing.reset();
    }
  }

  public static final String REVISION = "$Revision: 1.8 $";

  /* (non-Javadoc)
   * @see net.sf.javabdd.BDDFactory#getVersion()
   */
  @Override
  public String getVersion() {
    return "TypedBDD "
        + REVISION.substring(11, REVISION.length() - 2)
        + " with "
        + factory.getVersion();
  }
}

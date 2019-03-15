// BasicTests.java, created Oct 18, 2004 10:42:34 PM by jwhaley
// Copyright (C) 2004 jwhaley
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.javabdd.bdd;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import junit.framework.Assert;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDException;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;

/**
 * BasicTests
 *
 * @author jwhaley
 * @version $Id: BasicTests.java,v 1.6 2005/06/29 08:01:29 joewhaley Exp $
 */
public class BasicTests extends BDDTestCase {

  public static void main(String[] args) {
    junit.textui.TestRunner.run(BasicTests.class);
  }

  public void testIsZeroOne() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      BDD x = bdd.zero();
      BDD y = bdd.one();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD z = bdd.ithVar(1);
      Assert.assertEquals(true, x.isZero());
      Assert.assertEquals(false, x.isOne());
      Assert.assertEquals(false, y.isZero());
      Assert.assertEquals(true, y.isOne());
      Assert.assertEquals(false, z.isZero());
      Assert.assertEquals(false, z.isOne());
      x.free();
      y.free();
      z.free();
    }
  }

  public void testVar() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      bdd.setVarOrder(new int[] {0, 1, 2, 3, 4});
      BDD a = bdd.ithVar(1);
      BDD b = bdd.ithVar(2);
      BDD c = bdd.ithVar(3);
      BDD d = bdd.one();
      BDD e = bdd.zero();
      Assert.assertEquals(1, a.var());
      Assert.assertEquals(2, b.var());
      Assert.assertEquals(3, c.var());
      try {
        d.var();
        Assert.fail();
      } catch (BDDException x) {
      }
      try {
        e.var();
        Assert.fail();
      } catch (BDDException x) {
      }
      BDD f = a.and(b);
      Assert.assertEquals(1, f.var());
      a.free();
      b.free();
      c.free();
      d.free();
      e.free();
      f.free();
    }
  }

  public void testVarOrder() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      bdd.setVarOrder(new int[] {0, 1, 2, 3, 4});
      BDD a = bdd.ithVar(0);
      BDD b = bdd.ithVar(1);
      BDD c = bdd.ithVar(2);
      BDD d = bdd.ithVar(3);
      BDD e = bdd.ithVar(4);
      Assert.assertEquals(0, a.var());
      Assert.assertEquals(1, b.var());
      Assert.assertEquals(2, c.var());
      Assert.assertEquals(3, d.var());
      Assert.assertEquals(4, e.var());
      bdd.setVarOrder(new int[] {2, 3, 4, 0, 1});
      Assert.assertEquals(0, a.var());
      Assert.assertEquals(1, b.var());
      Assert.assertEquals(2, c.var());
      Assert.assertEquals(3, d.var());
      Assert.assertEquals(4, e.var());
      Assert.assertEquals(3, a.level());
      Assert.assertEquals(4, b.level());
      Assert.assertEquals(0, c.level());
      Assert.assertEquals(1, d.level());
      Assert.assertEquals(2, e.level());
      a.free();
      b.free();
      c.free();
      d.free();
      e.free();
    }
  }

  public void testLowHigh() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      bdd.setVarOrder(new int[] {0, 1, 2, 3, 4});
      BDD a, b, c;
      a = bdd.ithVar(0);
      a.andWith(bdd.ithVar(1));
      a.andWith(bdd.nithVar(2));
      Assert.assertEquals(0, a.var());
      b = a.low();
      Assert.assertEquals(true, b.isZero());
      b.free();
      b = a.high();
      Assert.assertEquals(1, b.var());
      c = b.high();
      b.free();
      Assert.assertEquals(2, c.var());
      b = c.low();
      Assert.assertEquals(true, b.isOne());
      a.free();
      b.free();
      c.free();
    }
  }

  public void testNot() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b, c;
      a = bdd.ithVar(0);
      b = a.not();
      c = bdd.nithVar(0);
      Assert.assertEquals(b, c);
      c.free();
      c = b.high();
      Assert.assertEquals(true, c.isZero());
      c.free();
      c = b.low();
      Assert.assertEquals(true, c.isOne());
      a.free();
      b.free();
      c.free();
    }
  }

  public void testId() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b;
      a = bdd.ithVar(1);
      b = a.id();
      a.andWith(bdd.ithVar(0));
      Assert.assertTrue(!a.equals(b));
      Assert.assertTrue(a.var() == 0);
      Assert.assertTrue(b.var() == 1);
      b.andWith(bdd.zero());
      Assert.assertTrue(b.isZero());
      Assert.assertTrue(!a.isZero());
      a.free();
      b.free();
    }
  }

  void testApply(
      BDDFactory bdd, BDDFactory.BDDOp op, boolean b1, boolean b2, boolean b3, boolean b4) {
    BDD a;
    Assert.assertEquals(b1, (a = bdd.zero().applyWith(bdd.zero(), op)).isOne());
    a.free();
    Assert.assertEquals(b2, (a = bdd.zero().applyWith(bdd.one(), op)).isOne());
    a.free();
    Assert.assertEquals(b3, (a = bdd.one().applyWith(bdd.zero(), op)).isOne());
    a.free();
    Assert.assertEquals(b4, (a = bdd.one().applyWith(bdd.one(), op)).isOne());
    a.free();
  }

  static boolean isFreed(BDD b) {
    return b.hashCode() == -1 || b.hashCode() == 0x07ffffff;
  }

  void testApplyWith(
      BDDFactory bdd, BDDFactory.BDDOp op, boolean b1, boolean b2, boolean b3, boolean b4) {
    BDD a, b, c, d;
    a = bdd.zero();
    b = bdd.zero();
    c = a;
    d = b;
    Assert.assertTrue(!isFreed(d));
    a.applyWith(b, op);
    Assert.assertEquals(b1, a.isOne());
    Assert.assertEquals(b1, c.isOne());
    Assert.assertTrue(isFreed(b));
    Assert.assertTrue(isFreed(d));
    a.free();

    a = bdd.zero();
    b = bdd.one();
    c = a;
    d = b;
    Assert.assertTrue(!isFreed(d));
    a.applyWith(b, op);
    Assert.assertEquals(b2, a.isOne());
    Assert.assertEquals(b2, c.isOne());
    Assert.assertTrue(isFreed(b));
    Assert.assertTrue(isFreed(d));
    a.free();

    a = bdd.one();
    b = bdd.zero();
    c = a;
    d = b;
    Assert.assertTrue(!isFreed(d));
    a.applyWith(b, op);
    Assert.assertEquals(b3, a.isOne());
    Assert.assertEquals(b3, c.isOne());
    Assert.assertTrue(isFreed(b));
    Assert.assertTrue(isFreed(d));
    a.free();

    a = bdd.one();
    b = bdd.one();
    c = a;
    d = b;
    Assert.assertTrue(!isFreed(d));
    a.applyWith(b, op);
    Assert.assertEquals(b4, a.isOne());
    Assert.assertEquals(b4, c.isOne());
    Assert.assertTrue(isFreed(b));
    Assert.assertTrue(isFreed(d));
    a.free();
  }

  public void testOr() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b, c;
      a = bdd.ithVar(1);
      b = bdd.ithVar(2);
      c = bdd.nithVar(1);
      c.orWith(a);
      Assert.assertTrue(c.isOne());
      a = bdd.zero();
      a.orWith(bdd.zero());
      Assert.assertTrue(a.isZero());
      b.orWith(b);
      Assert.assertEquals(2, b.var());
      a.free();
      b.free();
      c.free();
      testApply(bdd, BDDFactory.or, false, true, true, true);
    }
  }

  public void testXor() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b, c;
      a = bdd.ithVar(1);
      b = bdd.ithVar(2);
      c = bdd.nithVar(1);
      c.xorWith(a);
      Assert.assertTrue(c.isOne());
      a = bdd.zero();
      a.orWith(bdd.zero());
      Assert.assertTrue(a.isZero());
      b.xorWith(b);
      Assert.assertTrue(b.isZero());
      a.free();
      b.free();
      c.free();
      testApply(bdd, BDDFactory.xor, false, true, true, false);
    }
  }

  public void testImp() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      // TODO: more tests
      testApply(bdd, BDDFactory.imp, true, true, false, true);
    }
  }

  public void testBiimp() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      // TODO: more tests
      testApply(bdd, BDDFactory.biimp, true, false, false, true);
    }
  }

  public void testDiff() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      // TODO: more tests
      testApply(bdd, BDDFactory.diff, false, false, true, false);
    }
  }

  public void testLess() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      // TODO: more tests
      testApply(bdd, BDDFactory.less, false, true, false, false);
    }
  }

  public void testInvImp() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      // TODO: more tests
      testApply(bdd, BDDFactory.invimp, true, false, true, true);
    }
  }

  public void testNand() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      // TODO: more tests
      testApply(bdd, BDDFactory.nand, true, true, true, false);
    }
  }

  public void testNor() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      // TODO: more tests
      testApply(bdd, BDDFactory.nor, true, false, false, false);
    }
  }

  public void testApplyWith() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      // TODO: more tests
      testApplyWith(bdd, BDDFactory.and, false, false, false, true);
      testApplyWith(bdd, BDDFactory.or, false, true, true, true);
      testApplyWith(bdd, BDDFactory.xor, false, true, true, false);
      testApplyWith(bdd, BDDFactory.imp, true, true, false, true);
      testApplyWith(bdd, BDDFactory.biimp, true, false, false, true);
      testApplyWith(bdd, BDDFactory.diff, false, false, true, false);
      testApplyWith(bdd, BDDFactory.less, false, true, false, false);
      testApplyWith(bdd, BDDFactory.invimp, true, false, true, true);
      testApplyWith(bdd, BDDFactory.nand, true, true, true, false);
      testApplyWith(bdd, BDDFactory.nor, true, false, false, false);
    }
  }

  public void testIte() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b, c, d, e;
      a = bdd.ithVar(1);
      b = bdd.one();
      c = bdd.zero();
      d = a.ite(b, c);
      Assert.assertEquals(a, d);
      d.free();
      d = a.ite(c, b);
      e = d.not();
      Assert.assertEquals(a, e);
      d.free();
      e.free();
      e = bdd.ithVar(2);
      d = e.ite(a, a);
      Assert.assertEquals(a, d);
      // TODO: more tests.
      a.free();
      b.free();
      c.free();
      d.free();
      e.free();
    }
  }

  public void testReplace() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDDPairing p1 = bdd.makePair(0, 1);
      BDDPairing p2 = bdd.makePair();
      p2.set(1, 2);
      BDDPairing p3 = bdd.makePair();
      p3.set(new int[] {0, 1}, new int[] {1, 0});
      BDD a, b, c, d, e, f;
      a = bdd.ithVar(0);
      b = bdd.ithVar(1);
      c = bdd.ithVar(2);
      d = bdd.zero();
      e = bdd.one();
      a.replaceWith(p1);
      Assert.assertEquals(a, b);
      a.replaceWith(p2);
      Assert.assertEquals(a, c);
      if (bdd.varNum() < 25) {
        bdd.setVarNum(25);
      }
      b.andWith(bdd.nithVar(0));
      f = b.replace(p3);
      f.andWith(bdd.ithVar(0));
      Assert.assertTrue(!f.isZero());
      f.andWith(bdd.ithVar(1));
      Assert.assertTrue(f.isZero());
      d.replaceWith(p3);
      Assert.assertTrue(d.isZero());
      e.replaceWith(p3);
      Assert.assertTrue(e.isOne());
      a.free();
      b.free();
      c.free();
      d.free();
      e.free();
      f.free();
      p1.reset();
      p2.reset();
      p3.reset();
    }
  }

  void tEnsureCapacity() {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      long[] domains = new long[] {127, 17, 31, 4};
      BDDDomain[] d = bdd.extDomain(domains);
      BDD q = d[0].ithVar(7);
      BDD r = d[1].ithVar(9);
      BDD s = d[2].ithVar(4);
      BDD t = d[3].ithVar(2);
      BDD u = r.and(s);
      BDD v = q.and(t);
      BDD w = u.and(t);
      // BDD x = d[1].set();
      for (int i = 0; i < d.length; ++i) {
        d[i].ensureCapacity(BigInteger.valueOf(150));
        Assert.assertEquals(BigInteger.valueOf(7), q.scanVar(d[0]));
        Assert.assertEquals(BigInteger.valueOf(9), r.scanVar(d[1]));
        Assert.assertEquals(BigInteger.valueOf(4), s.scanVar(d[2]));
        Assert.assertEquals(BigInteger.valueOf(2), t.scanVar(d[3]));
        Assert.assertEquals(BigInteger.valueOf(9), u.scanVar(d[1]));
        Assert.assertEquals(BigInteger.valueOf(4), u.scanVar(d[2]));
        Assert.assertEquals(BigInteger.valueOf(7), v.scanVar(d[0]));
        Assert.assertEquals(BigInteger.valueOf(2), v.scanVar(d[3]));
        Assert.assertEquals(BigInteger.valueOf(9), w.scanVar(d[1]));
        Assert.assertEquals(BigInteger.valueOf(4), w.scanVar(d[2]));
        Assert.assertEquals(BigInteger.valueOf(2), w.scanVar(d[3]));
        // BDD y = d[1].set();
        // Assert.assertEquals(x, y);
        // y.free();
      }
      // x.free();
      w.free();
      v.free();
      u.free();
      t.free();
      s.free();
      r.free();
      q.free();
    }
  }

  void tEnsureCapacity2() throws IOException {
    reset();
    Assert.assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = nextFactory();
      System.out.println("Factory " + bdd);
      long[] domainSizes = new long[] {127, 17, 31, 4, 256, 87, 42, 666, 3405, 18};
      while (bdd.numberOfDomains() < domainSizes.length) {
        bdd.extDomain(domainSizes[bdd.numberOfDomains()]);
      }
      BDDDomain[] d = new BDDDomain[domainSizes.length];
      for (int i = 0; i < domainSizes.length; ++i) {
        d[i] = bdd.getDomain(i);
        domainSizes[i] = d[i].size().longValue();
      }
      for (int i = 0; i < d.length; ++i) {
        d[i].setName(Integer.toString(i));
      }
      final int count = 100;
      final int num = 10;
      for (int i = 0; i < count; ++i) {
        String order = randomOrder(d);
        // System.out.println("Random order: "+order);
        bdd.setVarOrder(bdd.makeVarOrdering(false, order));
        List bdds = new LinkedList();
        for (int j = 0; j < num; ++j) {
          BDD b = randomBDD(bdd);
          bdds.add(b);
        }
        StringBuffer sb = new StringBuffer();
        for (Iterator j = bdds.iterator(); j.hasNext(); ) {
          BDD b = (BDD) j.next();
          sb.append(b.toStringWithDomains());
          // bdd.save(new BufferedWriter(new PrintWriter(System.out)), b);
        }
        String before = sb.toString();
        int which = random.nextInt(d.length);
        int amount = random.nextInt(d[which].size().intValue() * 3);
        // System.out.println(" Ensure capacity "+d[which]+" = "+amount);
        d[which].ensureCapacity(amount);
        sb = new StringBuffer();
        for (Iterator j = bdds.iterator(); j.hasNext(); ) {
          BDD b = (BDD) j.next();
          sb.append(b.toStringWithDomains());
          // bdd.save(new BufferedWriter(new PrintWriter(System.out)), b);
        }
        String after = sb.toString();
        Assert.assertEquals(before, after);
        for (Iterator j = bdds.iterator(); j.hasNext(); ) {
          BDD b = (BDD) j.next();
          b.free();
        }
      }
    }
  }

  private static BDD randomBDD(BDDFactory f) {
    Assert.assertTrue(f.numberOfDomains() > 0);
    List list = new ArrayList(f.numberOfDomains());
    int k = random.nextInt(f.numberOfDomains());
    for (int i = 0; i < f.numberOfDomains(); ++i) {
      list.add(f.getDomain(i));
    }
    BDD result = f.one();
    for (int i = 0; i < k; ++i) {
      int x = random.nextInt(f.numberOfDomains() - i);
      BDDDomain d = (BDDDomain) list.remove(x);
      int y = random.nextInt(d.size().intValue());
      result.andWith(d.ithVar(y));
    }
    if (k == 0 && random.nextBoolean()) {
      result.andWith(f.zero());
    }
    return result;
  }

  private static String randomOrder(BDDDomain[] domains) {
    domains = (BDDDomain[]) randomShuffle(domains);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < domains.length; ++i) {
      if (i > 0) {
        boolean x = random.nextBoolean();
        if (x) {
          sb.append('x');
        } else {
          sb.append('_');
        }
      }
      sb.append(domains[i].toString());
    }
    return sb.toString();
  }

  private static Random random = new Random(System.currentTimeMillis());

  private static Object[] randomShuffle(Object[] a) {
    int n = a.length;
    List list = new ArrayList(Arrays.asList(a));
    Object[] result = (Object[]) a.clone();
    for (int i = 0; i < n; ++i) {
      int k = random.nextInt(n - i);
      result[i] = list.remove(k);
    }
    Assert.assertTrue(list.isEmpty());
    return result;
  }
}

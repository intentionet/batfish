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
package net.sf.javabdd.bdd;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDException;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;

/**
 * BasicTests
 *
 * @author jwhaley
 * @version $Id: BasicTests.java,v 1.6 2005/06/29 08:01:29 joewhaley Exp $
 */
@RunWith(JUnit38ClassRunner.class)
public class BasicTests extends BDDTestCase {

  public static void main(String[] args) {
    junit.textui.TestRunner.run(BasicTests.class);
  }

  public void testIsZeroOne() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      BDD x = bdd.zero();
      BDD y = bdd.one();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD z = bdd.ithVar(1);
      assertTrue(x.isZero());
      assertFalse(x.isOne());
      assertFalse(y.isZero());
      assertTrue(y.isOne());
      assertFalse(z.isZero());
      assertFalse(z.isOne());
      x.free();
      y.free();
      z.free();
    }
  }

  public void testVar() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      bdd.setVarOrder(new int[] {0, 1, 2, 3, 4});
      BDD a = bdd.ithVar(1);
      BDD b = bdd.ithVar(2);
      BDD c = bdd.ithVar(3);
      BDD d = bdd.one();
      BDD e = bdd.zero();
      assertEquals(1, a.var());
      assertEquals(2, b.var());
      assertEquals(3, c.var());
      try {
        d.var();
        fail();
      } catch (BDDException x) {
      }
      try {
        e.var();
        fail();
      } catch (BDDException x) {
      }
      BDD f = a.and(b);
      assertEquals(1, f.var());
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
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      bdd.setVarOrder(new int[] {0, 1, 2, 3, 4});
      BDD a = bdd.ithVar(0);
      BDD b = bdd.ithVar(1);
      BDD c = bdd.ithVar(2);
      BDD d = bdd.ithVar(3);
      BDD e = bdd.ithVar(4);
      assertEquals(0, a.var());
      assertEquals(1, b.var());
      assertEquals(2, c.var());
      assertEquals(3, d.var());
      assertEquals(4, e.var());
      bdd.setVarOrder(new int[] {2, 3, 4, 0, 1});
      assertEquals(0, a.var());
      assertEquals(1, b.var());
      assertEquals(2, c.var());
      assertEquals(3, d.var());
      assertEquals(4, e.var());
      assertEquals(3, a.level());
      assertEquals(4, b.level());
      assertEquals(0, c.level());
      assertEquals(1, d.level());
      assertEquals(2, e.level());
      a.free();
      b.free();
      c.free();
      d.free();
      e.free();
    }
  }

  public void testLowHigh() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      bdd.setVarOrder(new int[] {0, 1, 2, 3, 4});
      BDD a, b, c;
      a = bdd.ithVar(0);
      a.andWith(bdd.ithVar(1));
      a.andWith(bdd.nithVar(2));
      assertEquals(0, a.var());
      b = a.low();
      assertTrue(b.isZero());
      b.free();
      b = a.high();
      assertEquals(1, b.var());
      c = b.high();
      b.free();
      assertEquals(2, c.var());
      b = c.low();
      assertTrue(b.isOne());
      a.free();
      b.free();
      c.free();
    }
  }

  public void testNot() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b, c;
      a = bdd.ithVar(0);
      b = a.not();
      c = bdd.nithVar(0);
      assertEquals(b, c);
      c.free();
      c = b.high();
      assertTrue(c.isZero());
      c.free();
      c = b.low();
      assertTrue(c.isOne());
      a.free();
      b.free();
      c.free();
    }
  }

  public void testId() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b;
      a = bdd.ithVar(1);
      b = a.id();
      a.andWith(bdd.ithVar(0));
      assertThat(a, not(equalTo(b)));
      assertEquals(0, a.var());
      assertEquals(1, b.var());
      b.andWith(bdd.zero());
      assertTrue(b.isZero());
      assertTrue(!a.isZero());
      a.free();
      b.free();
    }
  }

  void testApply(
      BDDFactory bdd, BDDFactory.BDDOp op, boolean b1, boolean b2, boolean b3, boolean b4) {
    BDD a;
    assertEquals(b1, (a = bdd.zero().applyWith(bdd.zero(), op)).isOne());
    a.free();
    assertEquals(b2, (a = bdd.zero().applyWith(bdd.one(), op)).isOne());
    a.free();
    assertEquals(b3, (a = bdd.one().applyWith(bdd.zero(), op)).isOne());
    a.free();
    assertEquals(b4, (a = bdd.one().applyWith(bdd.one(), op)).isOne());
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
    assertTrue(!isFreed(d));
    a.applyWith(b, op);
    assertEquals(b1, a.isOne());
    assertEquals(b1, c.isOne());
    assertTrue(isFreed(b));
    assertTrue(isFreed(d));
    a.free();

    a = bdd.zero();
    b = bdd.one();
    c = a;
    d = b;
    assertTrue(!isFreed(d));
    a.applyWith(b, op);
    assertEquals(b2, a.isOne());
    assertEquals(b2, c.isOne());
    assertTrue(isFreed(b));
    assertTrue(isFreed(d));
    a.free();

    a = bdd.one();
    b = bdd.zero();
    c = a;
    d = b;
    assertTrue(!isFreed(d));
    a.applyWith(b, op);
    assertEquals(b3, a.isOne());
    assertEquals(b3, c.isOne());
    assertTrue(isFreed(b));
    assertTrue(isFreed(d));
    a.free();

    a = bdd.one();
    b = bdd.one();
    c = a;
    d = b;
    assertTrue(!isFreed(d));
    a.applyWith(b, op);
    assertEquals(b4, a.isOne());
    assertEquals(b4, c.isOne());
    assertTrue(isFreed(b));
    assertTrue(isFreed(d));
    a.free();
  }

  public void testOr() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b, c;
      a = bdd.ithVar(1);
      b = bdd.ithVar(2);
      c = bdd.nithVar(1);
      c.orWith(a);
      assertTrue(c.isOne());
      a = bdd.zero();
      a.orWith(bdd.zero());
      assertTrue(a.isZero());
      b.orWith(b);
      assertEquals(2, b.var());
      a.free();
      b.free();
      c.free();
      testApply(bdd, BDDFactory.or, false, true, true, true);
    }
  }

  public void testXor() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b, c;
      a = bdd.ithVar(1);
      b = bdd.ithVar(2);
      c = bdd.nithVar(1);
      c.xorWith(a);
      assertTrue(c.isOne());
      a = bdd.zero();
      a.orWith(bdd.zero());
      assertTrue(a.isZero());
      b.xorWith(b);
      assertTrue(b.isZero());
      a.free();
      b.free();
      c.free();
      testApply(bdd, BDDFactory.xor, false, true, true, false);
    }
  }

  public void testImp() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      // TODO: more tests
      testApply(bdd, BDDFactory.imp, true, true, false, true);
    }
  }

  public void testBiimp() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      // TODO: more tests
      testApply(bdd, BDDFactory.biimp, true, false, false, true);
    }
  }

  public void testDiff() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a = bdd.ithVar(1);
      BDD b = bdd.ithVar(2);
      BDD notb = bdd.nithVar(2);
      BDD diff = a.diff(b);
      BDD diffLongForm = a.and(notb);
      assertThat(diff, equalTo(diffLongForm));
      a.free();
      b.free();
      notb.free();
      diff.free();
      diffLongForm.free();
      testApply(bdd, BDDFactory.diff, false, false, true, false);
    }
  }

  public void testLess() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a = bdd.ithVar(1);
      BDD b = bdd.ithVar(2);
      BDD nota = bdd.nithVar(1);
      BDD less = a.less(b);
      BDD lessLongForm = nota.and(b);
      assertThat(less, equalTo(lessLongForm));
      a.free();
      b.free();
      nota.free();
      less.free();
      lessLongForm.free();
      testApply(bdd, BDDFactory.less, false, true, false, false);
    }
  }

  public void testInvImp() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      // TODO: more tests
      testApply(bdd, BDDFactory.invimp, true, false, true, true);
    }
  }

  public void testNand() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      // TODO: more tests
      testApply(bdd, BDDFactory.nand, true, true, true, false);
    }
  }

  public void testNor() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      // TODO: more tests
      testApply(bdd, BDDFactory.nor, true, false, false, false);
    }
  }

  public void testApplyWith() {
    reset();
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
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
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
      if (bdd.varNum() < 5) {
        bdd.setVarNum(5);
      }
      BDD a, b, c, d, e;
      a = bdd.ithVar(1);
      b = bdd.one();
      c = bdd.zero();
      d = a.ite(b, c);
      assertEquals(a, d);
      d.free();
      d = a.ite(c, b);
      e = d.not();
      assertEquals(a, e);
      d.free();
      e.free();
      e = bdd.ithVar(2);
      d = e.ite(a, a);
      assertEquals(a, d);
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
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
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
      assertEquals(a, b);
      a.replaceWith(p2);
      assertEquals(a, c);
      if (bdd.varNum() < 25) {
        bdd.setVarNum(25);
      }
      b.andWith(bdd.nithVar(0));
      f = b.replace(p3);
      f.andWith(bdd.ithVar(0));
      assertTrue(!f.isZero());
      f.andWith(bdd.ithVar(1));
      assertTrue(f.isZero());
      d.replaceWith(p3);
      assertTrue(d.isZero());
      e.replaceWith(p3);
      assertTrue(e.isOne());
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
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
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
      for (BDDDomain bddDomain : d) {
        bddDomain.ensureCapacity(BigInteger.valueOf(150));
        assertEquals(BigInteger.valueOf(7), q.scanVar(d[0]));
        assertEquals(BigInteger.valueOf(9), r.scanVar(d[1]));
        assertEquals(BigInteger.valueOf(4), s.scanVar(d[2]));
        assertEquals(BigInteger.valueOf(2), t.scanVar(d[3]));
        assertEquals(BigInteger.valueOf(9), u.scanVar(d[1]));
        assertEquals(BigInteger.valueOf(4), u.scanVar(d[2]));
        assertEquals(BigInteger.valueOf(7), v.scanVar(d[0]));
        assertEquals(BigInteger.valueOf(2), v.scanVar(d[3]));
        assertEquals(BigInteger.valueOf(9), w.scanVar(d[1]));
        assertEquals(BigInteger.valueOf(4), w.scanVar(d[2]));
        assertEquals(BigInteger.valueOf(2), w.scanVar(d[3]));
        // BDD y = d[1].set();
        // assertEquals(x, y);
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
    assertTrue(hasNext());
    while (hasNext()) {
      BDDFactory bdd = next();
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
        List<BDD> bdds = new LinkedList<>();
        for (int j = 0; j < num; ++j) {
          BDD b = randomBDD(bdd);
          bdds.add(b);
        }
        StringBuffer sb = new StringBuffer();
        for (Object bdd3 : bdds) {
          BDD b = (BDD) bdd3;
          sb.append(b.toStringWithDomains());
          // bdd.save(new BufferedWriter(new PrintWriter(System.out)), b);
        }
        String before = sb.toString();
        int which = random.nextInt(d.length);
        int amount = random.nextInt(d[which].size().intValue() * 3);
        // System.out.println(" Ensure capacity "+d[which]+" = "+amount);
        d[which].ensureCapacity(amount);
        sb = new StringBuffer();
        for (Object bdd2 : bdds) {
          BDD b = (BDD) bdd2;
          sb.append(b.toStringWithDomains());
          // bdd.save(new BufferedWriter(new PrintWriter(System.out)), b);
        }
        String after = sb.toString();
        assertEquals(before, after);
        for (Object bdd1 : bdds) {
          BDD b = (BDD) bdd1;
          b.free();
        }
      }
    }
  }

  private static BDD randomBDD(BDDFactory f) {
    assertTrue(f.numberOfDomains() > 0);
    List<BDDDomain> list = new ArrayList<>(f.numberOfDomains());
    int k = random.nextInt(f.numberOfDomains());
    for (int i = 0; i < f.numberOfDomains(); ++i) {
      list.add(f.getDomain(i));
    }
    BDD result = f.one();
    for (int i = 0; i < k; ++i) {
      int x = random.nextInt(f.numberOfDomains() - i);
      BDDDomain d = list.remove(x);
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
    StringBuilder sb = new StringBuilder();
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
    List<Object> list = new ArrayList<>(Arrays.asList(a));
    Object[] result = a.clone();
    for (int i = 0; i < n; ++i) {
      int k = random.nextInt(n - i);
      result[i] = list.remove(k);
    }
    assertTrue(list.isEmpty());
    return result;
  }
}

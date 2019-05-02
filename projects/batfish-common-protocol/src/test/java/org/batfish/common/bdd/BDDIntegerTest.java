package org.batfish.common.bdd;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BDDIntegerTest {
  @Rule public ExpectedException _exception = ExpectedException.none();

  @Test
  public void testSatAssignmentToLong() {
    BDDInteger dstIp = new BDDPacket().getDstIp();
    long value = 12345;
    BDD bdd = dstIp.value(value);
    assertThat(dstIp.getValueSatisfying(bdd), equalTo(Optional.of(value)));

    value = 0xFFFFFFFFL;
    bdd = dstIp.value(value);
    assertThat(dstIp.getValueSatisfying(bdd), equalTo(Optional.of(value)));
  }

  @Test
  public void testGetValueSatisfying() {
    BDDInteger dstIp = new BDDPacket().getDstIp();
    BDD bdd = dstIp.geq(1).and(dstIp.leq(0));
    assertThat(dstIp.getValueSatisfying(bdd), equalTo(Optional.empty()));

    bdd = dstIp.geq(1).and(dstIp.leq(1));
    assertThat(dstIp.getValueSatisfying(bdd), equalTo(Optional.of((long) 1)));
  }

  @Test
  public void testGetValuesSatisfying() {
    BDDInteger dstIp = new BDDPacket().getDstIp();
    BDD bdd = dstIp.geq(1).and(dstIp.leq(0));
    assertThat(dstIp.getValuesSatisfying(bdd, 10), hasSize(0));

    long max = 0xFFFFFFFFL;
    long min = 0xFFFFFFFAL;
    bdd = dstIp.geq(min).and(dstIp.leq(max));
    assertThat(
        dstIp.getValuesSatisfying(bdd, 10),
        containsInAnyOrder(
            0xFFFFFFFAL, 0xFFFFFFFBL, 0xFFFFFFFCL, 0xFFFFFFFDL, 0xFFFFFFFEL, 0xFFFFFFFFL));
  }

  @Test
  public void testGeqOutOfRange() {
    BDDFactory factory = BDDUtils.bddFactory(5);
    BDDInteger var = BDDInteger.makeFromIndex(factory, 5, 0, false);
    _exception.expect(IllegalArgumentException.class);
    _exception.expectMessage("value 32 is out of range");
    var.geq(32);
  }

  @Test
  public void testLeqOutOfRange() {
    BDDFactory factory = BDDUtils.bddFactory(5);
    BDDInteger var = BDDInteger.makeFromIndex(factory, 5, 0, false);
    _exception.expect(IllegalArgumentException.class);
    _exception.expectMessage("value 32 is out of range");
    var.leq(32);
  }

  @Test
  public void testValueOutOfRange() {
    BDDFactory factory = BDDUtils.bddFactory(5);
    BDDInteger var = BDDInteger.makeFromIndex(factory, 5, 0, false);
    _exception.expect(IllegalArgumentException.class);
    _exception.expectMessage("value 32 is out of range");
    var.value(32);
  }

  @Test
  public void testAdd() {
    BDDFactory factory = BDDUtils.bddFactory(10);
    BDDInteger x = BDDInteger.makeFromIndex(factory, 5, 0, false);
    BDDInteger constant1 = BDDInteger.makeFromValue(factory, 5, 1);
    BDDInteger xPlus1 = x.add(constant1);

    assertTrue(x.value(0).equals(xPlus1.value(1))); // x == 0 <==> x+1 == 1
    assertTrue(x.value(1).equals(xPlus1.value(2))); // x == 1 <==> x+1 == 2
    assertTrue(x.value(31).equals(xPlus1.value(0))); // x == 31 <==> x+1 == 0

    // convert to a relation representation (i.e. a constraint over two integer variables)
    BDDInteger y = BDDInteger.makeFromIndex(factory, 5, 5, false);
    BDD[] bv1 = xPlus1.getBitvec();
    BDD[] bv2 = y.getBitvec();
    BDD yEqXPlus1 = factory.one();
    for (int i = 0; i < bv2.length; i++) {
      yEqXPlus1 = yEqXPlus1.and(bv2[i].biimp(bv1[i]));
    }

    // solve y = x+1 for different values of x

    // x == 1 ==> y == 2
    assertThat(y.getValuesSatisfying(yEqXPlus1.and(x.value(1)), 5), contains(2L));
    // x == 10 ==> y == 11
    assertThat(y.getValuesSatisfying(yEqXPlus1.and(x.value(10)), 5), contains(11L));
    // x == 31 ==> y = 0
    assertThat(y.getValuesSatisfying(yEqXPlus1.and(x.value(31)), 5), contains(0L));

    // solve y = x+1 for different values of y

    // y == 0 ==> x == 31
    assertThat(x.getValuesSatisfying(yEqXPlus1.and(y.value(0)), 5), contains(31L));
    // y == 10 ==> x == 9
    assertThat(x.getValuesSatisfying(yEqXPlus1.and(y.value(10)), 5), contains(9L));
    // y == 31 ==> x == 30
    assertThat(x.getValuesSatisfying(yEqXPlus1.and(y.value(31)), 5), contains(30L));
  }

  @Test
  public void testRange() {
    BDDFactory factory = BDDUtils.bddFactory(10);
    BDDInteger x = BDDInteger.makeFromIndex(factory, 5, 0, false);
    for (int a = 0; a < 32; ++a) {
      for (int b = a; b < 32; ++b) {
        BDD range = x.range(a, b);
        BDD rangeEquiv = x.geq(a).and(x.leq(b));
        assertThat(range, equalTo(rangeEquiv));
      }
    }
  }
}

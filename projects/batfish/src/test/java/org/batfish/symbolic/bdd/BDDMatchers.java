package org.batfish.symbolic.bdd;

import net.sf.javabdd.BDD;
import org.batfish.symbolic.bdd.BDDMatchersImpl.IsOne;
import org.batfish.symbolic.bdd.BDDMatchersImpl.IsZero;
import org.hamcrest.Matcher;

public final class BDDMatchers {
  private BDDMatchers() {}

  public static Matcher<BDD> isOne() {
    return new IsOne();
  }

  public static Matcher<BDD> isZero() {
    return new IsZero();
  }
}

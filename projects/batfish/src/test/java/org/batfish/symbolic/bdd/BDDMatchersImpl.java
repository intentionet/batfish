package org.batfish.symbolic.bdd;

import net.sf.javabdd.BDD;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

final class BDDMatchersImpl {
  static final class IsOne extends BaseMatcher<BDD> {
    @Override
    public boolean matches(Object o) {
      return ((BDD) o).isOne();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("BDD::isOne() returns true");
    }
  }

  static final class IsZero extends BaseMatcher<BDD> {
    @Override
    public boolean matches(Object o) {
      return ((BDD) o).isZero();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("BDD::isZero() returns true");
    }
  }
}

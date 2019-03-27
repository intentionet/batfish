package org.batfish.bddreachability.transition;

import net.sf.javabdd.BDD;

/** A transition that allows nothing. */
public final class Zero implements Transition {
  public static final Zero INSTANCE = new Zero();

  private Zero() {}

  @Override
  public BDD transitForward(BDD bdd) {
    return bdd.getFactory().zero();
  }

  @Override
  public BDD transitBackward(BDD bdd) {
    return bdd.getFactory().zero();
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Zero;
  }
}

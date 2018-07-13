package org.batfish.symbolic.bdd;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.Arrays;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public final class BDDOps {
  private final BDDFactory _factory;

  public BDDOps(BDDFactory factory) {
    _factory = factory;
  }

  public BDD and(BDD... conjuncts) {
    return and(Arrays.asList(conjuncts));
  }

  public BDD and(Iterable<BDD> conjuncts) {
    return firstNonNull(andNull(conjuncts), _factory.one());
  }

  /** A variant of {@link #and(BDD...)} that returns {@code null} when all conjuncts are null. */
  public static BDD andNull(BDD... conjuncts) {
    return andNull(Arrays.asList(conjuncts));
  }

  public static BDD andNull(Iterable<BDD> conjuncts) {
    BDD result = null;
    for (BDD conjunct : conjuncts) {
      if (conjunct != null) {
        result = result == null ? conjunct : result.and(conjunct);
      }
    }
    return result;
  }

  /** Returns bdd.not() or {@code null} if given {@link BDD} is null. */
  public static BDD notDefaultNull(BDD bdd) {
    if (bdd == null) {
      return bdd;
    }
    return bdd.not();
  }

  public BDD or(BDD... disjuncts) {
    return or(Arrays.asList(disjuncts));
  }

  public BDD or(Iterable<BDD> disjuncts) {
    return firstNonNull(orNull(disjuncts), _factory.zero());
  }

  /** A variant of {@link #or(BDD...)} that returns {@code null} when all disjuncts are null. */
  public static BDD orNull(BDD... disjuncts) {
    return orNull(Arrays.asList(disjuncts));
  }

  public static BDD orNull(Iterable<BDD> disjuncts) {
    BDD result = null;
    for (BDD disjunct : disjuncts) {
      if (disjunct != null) {
        result = result == null ? disjunct : result.or(disjunct);
      }
    }
    return result;
  }
}

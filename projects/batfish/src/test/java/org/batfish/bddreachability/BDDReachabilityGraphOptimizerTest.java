package org.batfish.bddreachability;

import static org.batfish.bddreachability.BDDReachabilityGraphOptimizer.optimize;
import static org.batfish.bddreachability.transition.Transitions.IDENTITY;
import static org.batfish.bddreachability.transition.Transitions.constraint;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import com.google.common.collect.ImmutableSet;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.JFactory;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.symbolic.state.StateExpr;
import org.batfish.symbolic.state.StateExprVisitor;
import org.junit.Test;

public class BDDReachabilityGraphOptimizerTest {
  private static final class DummyState implements StateExpr {
    private final String _name;

    private DummyState(String name) {
      _name = name;
    }

    @Override
    public String toString() {
      return _name;
    }

    @Override
    public <R> R accept(StateExprVisitor<R> visitor) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class DummyTransition implements Transition {
    @Override
    public BDD transitForward(BDD bdd) {
      throw new UnsupportedOperationException();
    }

    @Override
    public BDD transitBackward(BDD bdd) {
      throw new UnsupportedOperationException();
    }
  }

  // some transition that we know can't be merged
  private static final DummyTransition DUMMY = new DummyTransition();

  private static final BDDFactory BDD_FACTORY = JFactory.init(100, 100);

  static {
    BDD_FACTORY.setVarNum(10);
  }

  private static final BDD BDD0 = BDD_FACTORY.ithVar(0);
  private static final BDD BDD1 = BDD_FACTORY.ithVar(1);
  private static final BDD BDD2 = BDD_FACTORY.ithVar(2);
  private static final BDD BDD3 = BDD_FACTORY.ithVar(3);

  private static final Transition CONSTRAINT0 = constraint(BDD0);
  private static final Transition CONSTRAINT1 = constraint(BDD1);
  private static final Transition CONSTRAINT2 = constraint(BDD2);
  private static final Transition CONSTRAINT3 = constraint(BDD3);

  private static final StateExpr STATE1 = new DummyState("STATE1");
  private static final StateExpr STATE2 = new DummyState("STATE2");
  private static final StateExpr STATE3 = new DummyState("STATE3");
  private static final StateExpr STATE4 = new DummyState("STATE4");

  @Test
  public void testStatesToKeep() {
    Edge edge = new Edge(STATE1, STATE2, IDENTITY);
    assertThat(optimize(ImmutableSet.of(edge), ImmutableSet.of(STATE1, STATE2)), contains(edge));
  }

  @Test
  public void testPruneRoot() {
    assertThat(
        optimize(ImmutableSet.of(new Edge(STATE1, STATE2, IDENTITY)), ImmutableSet.of(STATE2)),
        empty());
  }

  @Test
  public void testPruneLeaf() {
    assertThat(
        optimize(ImmutableSet.of(new Edge(STATE1, STATE2, IDENTITY)), ImmutableSet.of(STATE1)),
        empty());
  }

  @Test
  public void testPruneLeaf2() {
    // going to prune both STATE2 and STATE3
    assertThat(
        optimize(
            ImmutableSet.of(new Edge(STATE1, STATE2, IDENTITY), new Edge(STATE2, STATE3, IDENTITY)),
            ImmutableSet.of(STATE1)),
        empty());
  }

  @Test
  public void testSplice() {
    Edge edge1 = new Edge(STATE1, STATE2, CONSTRAINT0);
    Edge edge2 = new Edge(STATE2, STATE3, CONSTRAINT1);
    Edge merged = new Edge(STATE1, STATE3, constraint(BDD0.and(BDD1)));
    assertThat(
        optimize(ImmutableSet.of(edge1, edge2), ImmutableSet.of(STATE1, STATE3)), contains(merged));
  }

  @Test
  public void testUnmergeable() {
    // would splice, but the transitions don't merge
    Edge edge1 = new Edge(STATE1, STATE2, CONSTRAINT0);
    Edge edge2 = new Edge(STATE2, STATE3, DUMMY);
    assertThat(
        optimize(ImmutableSet.of(edge1, edge2), ImmutableSet.of(STATE1, STATE3)),
        containsInAnyOrder(edge1, edge2));
  }

  @Test
  public void testSpliceAndDrop() {
    Edge edge1 = new Edge(STATE1, STATE2, constraint(BDD0));
    Edge edge2 = new Edge(STATE2, STATE3, constraint(BDD0.not()));
    assertThat(optimize(ImmutableSet.of(edge1, edge2), ImmutableSet.of(STATE1, STATE3)), empty());
  }

  @Test
  public void testDropSelfLoopIdentity() {
    assertThat(
        optimize(ImmutableSet.of(new Edge(STATE1, STATE1, IDENTITY)), ImmutableSet.of(STATE1)),
        empty());
  }

  @Test
  public void testDropSelfLoopConstraint() {
    assertThat(
        optimize(ImmutableSet.of(new Edge(STATE1, STATE1, CONSTRAINT1)), ImmutableSet.of(STATE1)),
        empty());
  }

  @Test
  public void testKeepSelfLoop() {
    Edge edge = new Edge(STATE1, STATE1, DUMMY);
    assertThat(optimize(ImmutableSet.of(edge), ImmutableSet.of(STATE1)), contains(edge));
  }

  @Test
  public void testCycle() {
    Edge edge1 = new Edge(STATE1, STATE2, IDENTITY);
    Edge edge2 = new Edge(STATE2, STATE3, IDENTITY);
    Edge edge3 = new Edge(STATE3, STATE1, IDENTITY);
    assertThat(optimize(ImmutableSet.of(edge1, edge2, edge3), ImmutableSet.of()), empty());
  }

  @Test
  public void testEnterCycle() {
    Edge edge1 = new Edge(STATE1, STATE2, CONSTRAINT0);
    Edge edge2 = new Edge(STATE2, STATE3, CONSTRAINT1);
    Edge edge3 = new Edge(STATE3, STATE1, CONSTRAINT2);
    Edge edge4 = new Edge(STATE4, STATE1, CONSTRAINT3);
    assertThat(
        optimize(ImmutableSet.of(edge1, edge2, edge3, edge4), ImmutableSet.of(STATE4, STATE2)),
        containsInAnyOrder(
            new Edge(STATE4, STATE1, CONSTRAINT3),
            new Edge(STATE1, STATE2, CONSTRAINT0),
            new Edge(STATE2, STATE1, constraint(BDD1.and(BDD2)))));
  }
}

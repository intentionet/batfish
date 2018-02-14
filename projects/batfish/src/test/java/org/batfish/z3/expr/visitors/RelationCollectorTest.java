package org.batfish.z3.expr.visitors;

import static com.google.common.collect.ImmutableList.of;
import static org.batfish.z3.expr.visitors.BoolExprTransformer.getNodName;
import static org.batfish.z3.expr.visitors.RelationCollector.collectRelations;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import org.batfish.z3.SynthesizerInput;
import org.batfish.z3.expr.AndExpr;
import org.batfish.z3.expr.FalseExpr;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.expr.TrueExpr;
import org.batfish.z3.state.NumberedQuery;
import org.junit.Before;
import org.junit.Test;

public class RelationCollectorTest {

  private int _atomCounter;

  private SynthesizerInput _input;

  private StateExpr newRelation() {
    return new NumberedQuery(_atomCounter++);
  }

  @Before
  public void setup() {
    _input = SynthesizerInput.builder().setConfigurations(ImmutableMap.of()).build();
  }

  /** Test that collectRelations traverses all children of an AndExpr. */
  @Test
  public void testVisitAndExpr() {
    StateExpr p1 = newRelation();
    StateExpr p2 = newRelation();
    List<StateExpr> atoms = of(p1, p2);
    AndExpr expr = new AndExpr(ImmutableList.copyOf(atoms));
    Set<String> expectedRelations =
        atoms.stream().map(atom -> getNodName(_input, atom)).collect(ImmutableSet.toImmutableSet());

    assertThat(collectRelations(_input, expr), equalTo(expectedRelations));
  }

  /** Test that collectionRelations returns the empty set for boolean literals. */
  @Test
  public void testVisitLiteral() {
    AndExpr and = new AndExpr(of(TrueExpr.INSTANCE, FalseExpr.INSTANCE));
    assertThat(collectRelations(_input, and), is(empty()));
  }

  /**
   * Test that collectRelations on a StateExpr returns a singleton set containing that state's
   * nodName.
   */
  @Test
  public void testVisitStateExpr() {
    StateExpr expr = newRelation();

    assertThat(collectRelations(_input, expr), equalTo(ImmutableSet.of(getNodName(_input, expr))));
  }
}

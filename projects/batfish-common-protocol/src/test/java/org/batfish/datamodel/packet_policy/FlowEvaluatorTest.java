package org.batfish.datamodel.packet_policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.packet_policy.FlowEvaluator.FlowResult;
import org.batfish.datamodel.packet_policy.FlowEvaluator.StatementResult;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.common.ImmutableList;

/** Tests of {@link FlowEvaluator} */
public class FlowEvaluatorTest {

  private FlowEvaluator _evaluator;
  private Flow _flow;

  @Before
  public void setup() {
    _flow =
        Flow.builder()
            .setIngressNode("someNode")
            .setIngressInterface("Eth0")
            .setTag("noTag")
            .setSrcIp(Ip.parse("1.1.1.1"))
            .setDstIp(Ip.parse("2.2.2.2"))
            .build();
    _evaluator =
        new FlowEvaluator(
            _flow, "Eth0", ImmutableSet.of(Configuration.DEFAULT_VRF_NAME, "otherVRF"));
  }

  @Test
  public void visitDrop() {
    StatementResult result = _evaluator.visit(Drop.instance());
    // Stop evaluation
    assertTrue(result.stop());
    assertTrue(result.getAction() instanceof Drop);
  }

  @Test
  public void visitNoop() {
    StatementResult result = _evaluator.visit(Noop.instance());
    // Do not stop evaluation, but we matched
    assertFalse(result.stop());
    assertTrue(result.matched());
    assertTrue(result.getAction() instanceof Noop);
  }

  @Test
  public void visitIfWithMatch() {
    StatementResult result =
        _evaluator.visit(
            new If(
                new PacketMatchExpr(TrueExpr.INSTANCE),
                ImmutableList.of(Noop.instance(), Drop.instance())));
    // We matched, stop evaluation
    assertTrue(result.stop());
    assertTrue(result.matched());
    assertTrue(result.getAction() instanceof Drop);
  }

  @Test
  public void visitIfNoMatch() {
    StatementResult result =
        _evaluator.visit(
            new If(
                new PacketMatchExpr(FalseExpr.INSTANCE),
                ImmutableList.of(Noop.instance(), Drop.instance())));
    assertFalse(result.stop());
    assertFalse(result.matched());
    // Didn't match so Noop
    assertTrue(result.getAction() instanceof Noop);
  }

  @Test
  public void visitFibLookupWithVrfMatch() {
    FibLookup fl = new FibLookup("otherVRF");
    StatementResult result = _evaluator.visit(fl);

    assertTrue(result.stop());
    assertTrue(result.matched());
    // Ensure action returned is equivalent to the one visited
    assertThat(result.getAction(), equalTo(fl));
  }

  @Test
  public void visitFibLookupWithNoVrfMatch() {
    FibLookup fl = new FibLookup("NonExistentVRF");
    StatementResult result = _evaluator.visit(fl);

    assertFalse(result.stop());
    assertFalse(result.matched());
    // Didn't match so Noop
    assertThat(result.getAction(), equalTo(Noop.instance()));
  }

  @Test
  public void visitPacketMatchExprWithMatch() {
    StatementResult result = _evaluator.visit(new PacketMatchExpr(TrueExpr.INSTANCE));
    assertFalse(result.stop());
    assertTrue(result.matched());
    assertTrue(result.getAction() instanceof Noop);
  }

  @Test
  public void visitPacketMatchExprNoMatch() {
    StatementResult result = _evaluator.visit(new PacketMatchExpr(FalseExpr.INSTANCE));
    assertFalse(result.stop());
    assertFalse(result.matched());
    assertTrue(result.getAction() instanceof Noop);
  }

  @Test
  public void testStatementResultEquality() {
    new EqualsTester()
        .addEqualityGroup(
            new StatementResult(Drop.instance(), true), new StatementResult(Drop.instance(), true))
        .addEqualityGroup(new StatementResult(Noop.instance(), true))
        .addEqualityGroup(new StatementResult(Drop.instance(), false))
        .addEqualityGroup(new Object())
        .testEquals();
  }

  @Test
  public void testFlowResultEquality() {
    new EqualsTester()
        .addEqualityGroup(
            new FlowResult(_flow, Drop.instance()), new FlowResult(_flow, Drop.instance()))
        .addEqualityGroup(
            new FlowResult(
                _flow.toBuilder().setIngressNode("differentNode").build(), Drop.instance()))
        .addEqualityGroup(new FlowResult(_flow, new FibLookup("aVRF")))
        .addEqualityGroup(new Object())
        .testEquals();
  }
}

package org.batfish.common.util;

import static org.batfish.common.util.TracePruner.prune;
import static org.batfish.datamodel.FlowDisposition.ACCEPTED;
import static org.batfish.datamodel.FlowDisposition.DENIED_IN;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.EnterInputIfaceStep;
import org.batfish.datamodel.flow.EnterInputIfaceStep.EnterInputIfaceStepDetail;
import org.batfish.datamodel.flow.ExitOutputIfaceStep;
import org.batfish.datamodel.flow.ExitOutputIfaceStep.ExitOutputIfaceStepDetail;
import org.batfish.datamodel.flow.Hop;
import org.batfish.datamodel.flow.StepAction;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.pojo.Node;
import org.junit.Test;

/** Tests of {@link TracePruner}. */
public class TracePrunerTest {
  private static final Hop HOP_A =
      new Hop(
          new Node("A"),
          ImmutableList.of(
              ExitOutputIfaceStep.builder()
                  .setAction(StepAction.FORWARDED)
                  .setDetail(
                      ExitOutputIfaceStepDetail.builder()
                          .setOutputInterface(NodeInterfacePair.of("A", "out"))
                          .build())
                  .build()));
  private static final Hop HOP_B =
      new Hop(
          new Node("B"),
          ImmutableList.of(
              EnterInputIfaceStep.builder()
                  .setAction(StepAction.RECEIVED)
                  .setDetail(
                      EnterInputIfaceStepDetail.builder()
                          .setInputInterface(NodeInterfacePair.of("B", "in"))
                          .build())
                  .build()));
  private static final Hop HOP_C = new Hop(new Node("C"), ImmutableList.of());

  private static final Trace TRACE_A_ACCEPTED = new Trace(ACCEPTED, ImmutableList.of(HOP_A));
  private static final Trace TRACE_A_B_ACCEPTED =
      new Trace(ACCEPTED, ImmutableList.of(HOP_A, HOP_B));
  private static final Trace TRACE_A_DENIED_IN = new Trace(DENIED_IN, ImmutableList.of(HOP_A));
  private static final Trace TRACE_B_ACCEPTED = new Trace(ACCEPTED, ImmutableList.of(HOP_B));
  private static final Trace TRACE_C_ACCEPTED = new Trace(ACCEPTED, ImmutableList.of(HOP_C));

  @Test
  public void testEmpty() {
    assertThat(prune(ImmutableList.of(), 5), empty());
  }

  @Test
  public void testPickNone() {
    assertThat(prune(ImmutableList.of(TRACE_A_ACCEPTED), 0), empty());
  }

  @Test
  public void testPreferDispositionOverNode() {
    assertThat(
        prune(ImmutableList.of(TRACE_A_ACCEPTED, TRACE_B_ACCEPTED, TRACE_A_DENIED_IN), 2),
        equalTo(ImmutableList.of(TRACE_A_ACCEPTED, TRACE_A_DENIED_IN)));
  }

  @Test
  public void testPickByNode() {
    assertThat(
        prune(
            ImmutableList.of(
                TRACE_A_B_ACCEPTED, TRACE_A_ACCEPTED, TRACE_B_ACCEPTED, TRACE_C_ACCEPTED),
            2),
        equalTo(ImmutableList.of(TRACE_A_B_ACCEPTED, TRACE_C_ACCEPTED)));
  }

  @Test
  public void testPickFill() {
    // once we've covered all dispositions and flows, fill with unused traces in input order
    assertThat(
        prune(
            ImmutableList.of(
                TRACE_A_B_ACCEPTED, TRACE_A_ACCEPTED, TRACE_B_ACCEPTED, TRACE_C_ACCEPTED),
            3),
        equalTo(ImmutableList.of(TRACE_A_B_ACCEPTED, TRACE_C_ACCEPTED, TRACE_A_ACCEPTED)));
  }
}

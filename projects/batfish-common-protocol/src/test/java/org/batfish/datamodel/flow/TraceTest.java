package org.batfish.datamodel.flow;

import com.google.common.collect.ImmutableList;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.EnterInputIfaceStep.EnterInputIfaceStepDetail;
import org.batfish.datamodel.flow.ExitOutputIfaceStep.ExitOutputIfaceStepDetail;
import org.batfish.datamodel.pojo.Node;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Tests for {@link Trace}. */
public class TraceTest {

  public @Rule ExpectedException _thrown = ExpectedException.none();

  private Hop _emptyHop = new Hop(new Node("Empty"), ImmutableList.of());
  private Hop _hopWithOnlyEnter =
      new Hop(
          new Node("Enter"),
          ImmutableList.of(
              EnterInputIfaceStep.builder()
                  .setAction(StepAction.RECEIVED)
                  .setDetail(
                      EnterInputIfaceStepDetail.builder()
                          .setInputInterface(NodeInterfacePair.of("Enter", "in"))
                          .build())
                  .build()));
  private Hop _hopWithOnlyExit =
      new Hop(
          new Node("Exit"),
          ImmutableList.of(
              ExitOutputIfaceStep.builder()
                  .setAction(StepAction.RECEIVED)
                  .setDetail(
                      ExitOutputIfaceStepDetail.builder()
                          .setOutputInterface(NodeInterfacePair.of("Exit", "out"))
                          .build())
                  .build()));

  @Test
  public void validateHopBeginsWithEnterInputInterface() {
    _thrown.expectMessage("Hop 2/2 of trace does not begin with an EnterInputIfaceStep:");
    Trace.validateHops(ImmutableList.of(_hopWithOnlyExit, _emptyHop));
  }

  @Test
  public void validateHopEndsWithExitOutputInterface() {
    _thrown.expectMessage("Hop 1/2 of trace does not end with an ExitOutputIfaceStep:");
    Trace.validateHops(ImmutableList.of(_emptyHop, _hopWithOnlyEnter));
  }

  @Test
  public void validateValidHops() {
    Trace.validateHops(ImmutableList.of());
    Trace.validateHops(ImmutableList.of(_emptyHop));
    Trace.validateHops(ImmutableList.of(_hopWithOnlyEnter));
    Trace.validateHops(ImmutableList.of(_hopWithOnlyExit));
    Trace.validateHops(ImmutableList.of(_hopWithOnlyExit, _hopWithOnlyEnter));
  }
}

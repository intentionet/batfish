package org.batfish.bddreachability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.flow.IncomingSessionScope;
import org.batfish.datamodel.flow.OriginatingSessionScope;
import org.batfish.symbolic.state.OriginateInterface;
import org.batfish.symbolic.state.OriginateVrf;
import org.batfish.symbolic.state.PreInInterface;
import org.batfish.symbolic.state.StateExpr;
import org.junit.Test;

public class PrecedingStatesVisitorTest {
  private static final String HOSTNAME = "node";
  private static final String VRF_NAME = "vrf";
  private static final String IFACE_NAME = "iface";
  private static final Interface IFACE =
      Interface.builder().setName(IFACE_NAME).setVrf(new Vrf(VRF_NAME)).build();

  /** Visitor set up with hostname and interface defined above */
  private static final PrecedingStatesVisitor VISITOR =
      new PrecedingStatesVisitor(HOSTNAME, ImmutableSet.of(IFACE));

  @Test
  public void testIncomingSessionScope() {
    // Applying visitor to IncomingSessionScope for the known interface should give PreInInterface
    IncomingSessionScope scope = new IncomingSessionScope(ImmutableSet.of(IFACE_NAME));
    StateExpr expectedState = new PreInInterface(HOSTNAME, IFACE_NAME);
    List<StateExpr> actualStates = scope.accept(VISITOR).collect(ImmutableList.toImmutableList());
    assertThat(actualStates, contains(expectedState));
  }

  @Test
  public void testOriginatingSessionScope() {
    // Applying visitor to OriginatingSessionScope for the known VRF should give OriginateInterface
    // and OriginateVrf states
    OriginatingSessionScope scope = new OriginatingSessionScope(VRF_NAME);
    StateExpr expectedOriginateInterfaceState = new OriginateInterface(HOSTNAME, IFACE_NAME);
    StateExpr expectedOriginateVrfState = new OriginateVrf(HOSTNAME, VRF_NAME);
    List<StateExpr> actualStates = scope.accept(VISITOR).collect(ImmutableList.toImmutableList());
    assertThat(
        actualStates,
        containsInAnyOrder(expectedOriginateInterfaceState, expectedOriginateVrfState));
  }
}

package org.batfish.bddreachability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.flow.IncomingSessionScope;
import org.batfish.datamodel.flow.OriginatingSessionScope;
import org.batfish.symbolic.state.OriginateInterface;
import org.batfish.symbolic.state.OriginateVrf;
import org.batfish.symbolic.state.PostInVrfSession;
import org.batfish.symbolic.state.PreInInterface;
import org.junit.Test;

public class EdgesMatchingSessionVisitorTest {
  private static final BDD ZERO = new BDDPacket().getFactory().zero();
  private static final String HOSTNAME = "node";
  private static final String VRF_NAME = "vrf";
  private static final String IFACE_NAME = "iface";
  private static final Interface IFACE =
      Interface.builder().setName(IFACE_NAME).setVrf(new Vrf(VRF_NAME)).build();

  /** Visitor set up with hostname and interface defined above that sets flow constraint to ZERO */
  private static final EdgesMatchingSessionVisitor VISITOR =
      new EdgesMatchingSessionVisitor(HOSTNAME, ImmutableMap.of(IFACE_NAME, IFACE), ZERO);

  @Test
  public void testIncomingSessionScope() {
    // Applying visitor to IncomingSessionScope for the known interface should give edge from
    // PreInInterface to PostInVrf
    IncomingSessionScope scope = new IncomingSessionScope(ImmutableSet.of(IFACE_NAME));
    Edge expectedEdge =
        new Edge(
            new PreInInterface(HOSTNAME, IFACE_NAME),
            new PostInVrfSession(HOSTNAME, VRF_NAME),
            ZERO);
    List<Edge> actualEdges = scope.accept(VISITOR).collect(ImmutableList.toImmutableList());
    assertThat(actualEdges, contains(expectedEdge));
  }

  @Test
  public void testOriginatingSessionScope() {
    // Applying visitor to OriginatingSessionScope for the known VRF should give edges from
    // OriginateInterface and OriginateVrf to PostInVrf
    OriginatingSessionScope scope = new OriginatingSessionScope(VRF_NAME);
    Edge expectedOriginateInterfaceEdge =
        new Edge(
            new OriginateInterface(HOSTNAME, IFACE_NAME),
            new PostInVrfSession(HOSTNAME, VRF_NAME),
            ZERO);
    Edge expectedOriginateVrfEdge =
        new Edge(
            new OriginateVrf(HOSTNAME, VRF_NAME), new PostInVrfSession(HOSTNAME, VRF_NAME), ZERO);
    List<Edge> actualEdges = scope.accept(VISITOR).collect(ImmutableList.toImmutableList());
    assertThat(
        actualEdges, containsInAnyOrder(expectedOriginateInterfaceEdge, expectedOriginateVrfEdge));
  }
}

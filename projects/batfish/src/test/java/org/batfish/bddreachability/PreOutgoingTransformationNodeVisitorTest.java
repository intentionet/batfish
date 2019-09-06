package org.batfish.bddreachability;

import static org.junit.Assert.assertEquals;

import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.symbolic.state.PreOutEdge;
import org.batfish.symbolic.state.PreOutInterfaceDeliveredToSubnet;
import org.batfish.symbolic.state.PreOutInterfaceExitsNetwork;
import org.batfish.symbolic.state.PreOutInterfaceInsufficientInfo;
import org.batfish.symbolic.state.PreOutInterfaceNeighborUnreachable;
import org.junit.Test;

/** Tests for {@link PreOutgoingTransformationNodeVisitor}. */
public final class PreOutgoingTransformationNodeVisitorTest {

  @Test
  public void testPreOutgoingTransformationStates() {
    String node = "node";
    String iface = "iface";

    assertEquals(
        new PreOutEdge(node, iface, "", "").accept(PreOutgoingTransformationNodeVisitor.INSTANCE),
        NodeInterfacePair.of(node, iface));
    assertEquals(
        new PreOutInterfaceDeliveredToSubnet(node, iface)
            .accept(PreOutgoingTransformationNodeVisitor.INSTANCE),
        NodeInterfacePair.of(node, iface));
    assertEquals(
        new PreOutInterfaceExitsNetwork(node, iface)
            .accept(PreOutgoingTransformationNodeVisitor.INSTANCE),
        NodeInterfacePair.of(node, iface));
    assertEquals(
        new PreOutInterfaceInsufficientInfo(node, iface)
            .accept(PreOutgoingTransformationNodeVisitor.INSTANCE),
        NodeInterfacePair.of(node, iface));
    assertEquals(
        new PreOutInterfaceNeighborUnreachable(node, iface)
            .accept(PreOutgoingTransformationNodeVisitor.INSTANCE),
        NodeInterfacePair.of(node, iface));
  }
}

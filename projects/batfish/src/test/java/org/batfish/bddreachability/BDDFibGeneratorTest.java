package org.batfish.bddreachability;

import static org.batfish.common.util.CollectionUtil.toImmutableMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.datamodel.Ip;
import org.batfish.symbolic.state.EdgeStateExpr;
import org.batfish.symbolic.state.InterfaceAccept;
import org.batfish.symbolic.state.InterfaceStateExpr;
import org.batfish.symbolic.state.NodeDropNoRoute;
import org.batfish.symbolic.state.NodeDropNullRoute;
import org.batfish.symbolic.state.StateExprVisitor;
import org.batfish.symbolic.state.VrfAccept;
import org.batfish.symbolic.state.VrfStateExpr;
import org.junit.Test;

/** Test of {@link BDDFibGenerator}. */
@ParametersAreNonnullByDefault
public final class BDDFibGeneratorTest {

  private static final class TestPostInVrf extends VrfStateExpr {
    public TestPostInVrf(String hostname, String vrf) {
      super(hostname, vrf);
    }

    @Override
    public <R> R accept(StateExprVisitor<R> visitor) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class TestPreOutEdge extends EdgeStateExpr {
    public TestPreOutEdge(String srcNode, String srcIface, String dstNode, String dstIface) {
      super(srcNode, srcIface, dstNode, dstIface);
    }

    @Override
    public <R> R accept(StateExprVisitor<R> visitor) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class TestPreOutInterfaceDeliveredToSubnet extends InterfaceStateExpr {
    public TestPreOutInterfaceDeliveredToSubnet(String hostname, String iface) {
      super(hostname, iface);
    }

    @Override
    public <R> R accept(StateExprVisitor<R> visitor) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class TestPreOutInterfaceExitsNetwork extends InterfaceStateExpr {
    public TestPreOutInterfaceExitsNetwork(String hostname, String iface) {
      super(hostname, iface);
    }

    @Override
    public <R> R accept(StateExprVisitor<R> visitor) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class TestPreOutInterfaceInsufficientInfo extends InterfaceStateExpr {
    public TestPreOutInterfaceInsufficientInfo(String hostname, String iface) {
      super(hostname, iface);
    }

    @Override
    public <R> R accept(StateExprVisitor<R> visitor) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class TestPreOutInterfaceNeighborUnreachable extends InterfaceStateExpr {
    public TestPreOutInterfaceNeighborUnreachable(String hostname, String iface) {
      super(hostname, iface);
    }

    @Override
    public <R> R accept(StateExprVisitor<R> visitor) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class TestPreOutVrf extends VrfStateExpr {
    public TestPreOutVrf(String hostname, String vrf) {
      super(hostname, vrf);
    }

    @Override
    public <R> R accept(StateExprVisitor<R> visitor) {
      throw new UnsupportedOperationException();
    }
  }

  private static final String NODE1 = "n1";
  private static final String NODE2 = "n2";

  private static final String VRF1 = "v1";
  private static final String VRF2 = "v2";

  private static final String IFACE1 = "i1";
  private static final String IFACE2 = "i2";

  private static final BDDPacket PKT = new BDDPacket();
  private static final BDD DSTIP1;
  private static final BDD DSTIP2;
  private static final BDD ONE;
  private static final BDD ZERO;

  static {
    DSTIP1 = PKT.getDstIp().value(Ip.parse("10.0.0.1").asLong());
    DSTIP2 = PKT.getDstIp().value(Ip.parse("10.0.0.2").asLong());
    ONE = PKT.getFactory().one();
    ZERO = PKT.getFactory().zero();
  }

  @Test
  public void testGenerateRules_InterfaceAccept_VrfAccept() {
    Map<String, Map<String, Map<String, BDD>>> ifaceAcceptBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP1)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE2, DSTIP2)));
    // need corresponding entry in routableBDDs to prevent NPE in generateForwardingEdges
    Map<String, Map<String, BDD>> routableBDDs =
        ImmutableMap.of(NODE1, ImmutableMap.of(VRF1, DSTIP2), NODE2, ImmutableMap.of(VRF1, DSTIP2));
    BDDFibGenerator generator =
        new BDDFibGenerator(
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ifaceAcceptBDDs,
            toVrfAcceptBdds(ifaceAcceptBDDs),
            routableBDDs,
            ImmutableMap.of(),
            ImmutableMap.of(),
            (node, iface) -> ONE);
    Edge expectedEdge =
        new Edge(
            new InterfaceAccept(NODE1, IFACE1), new VrfAccept(NODE1, VRF1), PKT.getFactory().one());

    assertThat(
        generator
            .generateRules_InterfaceAccept_VrfAccept(NODE1::equals)
            .collect(ImmutableList.toImmutableList()),
        contains(expectedEdge));
    // ensure edge is produced by top-level generation function
    assertThat(
        generator
            .generateForwardingEdges(
                NODE1::equals,
                TestPostInVrf::new,
                TestPreOutEdge::new,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        hasItem(expectedEdge));
  }

  @Test
  public void testGenerateRules_PostInVrf_InterfaceAccept() {
    Map<String, Map<String, Map<String, BDD>>> ifaceAcceptBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP1)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE2, DSTIP2)));
    // need corresponding entry in routableBDDs to prevent NPE in generateForwardingEdges
    Map<String, Map<String, BDD>> routableBDDs =
        ImmutableMap.of(NODE1, ImmutableMap.of(VRF1, DSTIP2), NODE2, ImmutableMap.of(VRF1, DSTIP2));
    BDDFibGenerator generator =
        new BDDFibGenerator(
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ifaceAcceptBDDs,
            toVrfAcceptBdds(ifaceAcceptBDDs),
            routableBDDs,
            ImmutableMap.of(),
            ImmutableMap.of(),
            (node, iface) -> ONE);
    Edge expectedEdge =
        new Edge(new TestPostInVrf(NODE1, VRF1), new InterfaceAccept(NODE1, IFACE1), DSTIP1);

    assertThat(
        generator
            .generateRules_PostInVrf_InterfaceAccept(NODE1::equals, TestPostInVrf::new)
            .collect(ImmutableList.toImmutableList()),
        contains(expectedEdge));
    // ensure edge is produced by top-level generation function
    assertThat(
        generator
            .generateForwardingEdges(
                NODE1::equals,
                TestPostInVrf::new,
                TestPreOutEdge::new,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        hasItem(expectedEdge));
  }

  @Test
  public void testGenerateRules_PostInVrf_NodeDropNoRoute() {
    Map<String, Map<String, Map<String, BDD>>> ifaceAcceptBdds =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP1)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE2, DSTIP1)));
    Map<String, Map<String, BDD>> routableBDDs =
        ImmutableMap.of(NODE1, ImmutableMap.of(VRF1, DSTIP2), NODE2, ImmutableMap.of(VRF1, DSTIP2));
    BDDFibGenerator generator =
        new BDDFibGenerator(
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ifaceAcceptBdds,
            toVrfAcceptBdds(ifaceAcceptBdds),
            routableBDDs,
            ImmutableMap.of(),
            ImmutableMap.of(),
            (node, iface) -> ONE);
    Edge expectedEdge =
        new Edge(new TestPostInVrf(NODE1, VRF1), new NodeDropNoRoute(NODE1), DSTIP1.nor(DSTIP2));

    assertThat(
        generator
            .generateRules_PostInVrf_NodeDropNoRoute(NODE1::equals, TestPostInVrf::new)
            .collect(ImmutableList.toImmutableList()),
        contains(expectedEdge));
    // ensure edge is produced by top-level generation function
    assertThat(
        generator
            .generateForwardingEdges(
                NODE1::equals,
                TestPostInVrf::new,
                TestPreOutEdge::new,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        hasItem(expectedEdge));
  }

  @Test
  public void testGenerateRules_PostInVrf_PostInVrf() {
    Map<String, Map<String, Map<String, BDD>>> nextVrfBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(VRF2, DSTIP1)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(VRF2, DSTIP1)));
    BDDFibGenerator generator =
        new BDDFibGenerator(
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            nextVrfBDDs,
            ImmutableMap.of(),
            (node, iface) -> ONE);
    Edge expectedEdge =
        new Edge(new TestPostInVrf(NODE1, VRF1), new TestPostInVrf(NODE1, VRF2), DSTIP1);

    assertThat(
        generator
            .generateRules_PostInVrf_PostInVrf(NODE1::equals, TestPostInVrf::new)
            .collect(ImmutableList.toImmutableList()),
        contains(expectedEdge));
    // ensure edge is produced by top-level generation function
    assertThat(
        generator
            .generateForwardingEdges(
                NODE1::equals,
                TestPostInVrf::new,
                TestPreOutEdge::new,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        hasItem(expectedEdge));
  }

  @Test
  public void testGenerateRules_PostInVrf_PreOutVrf() {
    // Node1 has 1 vrf, routes dstip1 and 2, and only accepts dstip1. So dstip2 to PreOutVrf
    // Node2 has 2 vrfs
    //    vrf1 routes dstip1 and 2, sends dstip2 to vrf2 -> dstip1 to PreOutVrf
    //    vrf2 routes dstip1, accepts nothing -> dstip1 to PreOutVrf
    Map<String, Map<String, Map<String, BDD>>> ifaceAcceptBdds =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP1)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE2, ZERO), VRF2, ImmutableMap.of()));
    Map<String, Map<String, BDD>> routableBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, DSTIP1.or(DSTIP2)),
            NODE2,
            ImmutableMap.of(VRF1, DSTIP1.or(DSTIP2), VRF2, DSTIP1));
    Map<String, Map<String, Map<String, BDD>>> nextVrfBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of()),
            NODE2,
            ImmutableMap.of(
                VRF1, ImmutableMap.of(VRF2, DSTIP2), VRF2, ImmutableMap.of(VRF1, ZERO)));
    BDDFibGenerator generator =
        new BDDFibGenerator(
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ifaceAcceptBdds,
            toVrfAcceptBdds(ifaceAcceptBdds),
            routableBDDs,
            nextVrfBDDs,
            ImmutableMap.of(),
            (node, iface) -> ONE);
    Edge expectedEdgeSimple =
        new Edge(new TestPostInVrf(NODE1, VRF1), new TestPreOutVrf(NODE1, VRF1), DSTIP2);
    Edge expectedEdgeVrfLeakV1 =
        new Edge(new TestPostInVrf(NODE2, VRF1), new TestPreOutVrf(NODE2, VRF1), DSTIP1);
    Edge expectedEdgeVrfLeakV2 =
        new Edge(new TestPostInVrf(NODE2, VRF2), new TestPreOutVrf(NODE2, VRF2), DSTIP1);

    assertThat(
        generator
            .generateRules_PostInVrf_PreOutVrf(
                NODE1::equals, TestPostInVrf::new, TestPreOutVrf::new)
            .collect(ImmutableList.toImmutableList()),
        contains(expectedEdgeSimple));
    assertThat(
        generator
            .generateRules_PostInVrf_PreOutVrf(
                NODE2::equals, TestPostInVrf::new, TestPreOutVrf::new)
            .collect(ImmutableList.toImmutableList()),
        contains(expectedEdgeVrfLeakV1, expectedEdgeVrfLeakV2));
    // ensure edge is produced by top-level generation function
    assertThat(
        generator
            .generateForwardingEdges(
                NODE1::equals,
                TestPostInVrf::new,
                TestPreOutEdge::new,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        hasItem(expectedEdgeSimple));
    assertThat(
        generator
            .generateForwardingEdges(
                NODE2::equals,
                TestPostInVrf::new,
                TestPreOutEdge::new,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        hasItems(expectedEdgeVrfLeakV1, expectedEdgeVrfLeakV2));
  }

  @Test
  public void testGenerateRules_PreOutVrf_NodeDropNullRoute() {
    Map<String, Map<String, BDD>> nullRoutedBDDs =
        ImmutableMap.of(NODE1, ImmutableMap.of(VRF1, DSTIP1), NODE2, ImmutableMap.of(VRF1, DSTIP1));
    BDDFibGenerator generator =
        new BDDFibGenerator(
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            nullRoutedBDDs,
            (node, iface) -> ONE);
    Edge expectedEdge =
        new Edge(new TestPreOutVrf(NODE1, VRF1), new NodeDropNullRoute(NODE1), DSTIP1);

    assertThat(
        generator
            .generateRules_PreOutVrf_NodeDropNullRoute(NODE1::equals, TestPreOutVrf::new)
            .collect(ImmutableList.toImmutableList()),
        contains(expectedEdge));
    // ensure edge is produced by top-level generation function
    assertThat(
        generator
            .generateForwardingEdges(
                NODE1::equals,
                TestPostInVrf::new,
                TestPreOutEdge::new,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        hasItem(expectedEdge));
  }

  @Test
  public void testGenerateRules_PreOutVrf_PreOutEdge() {
    Map<String, Map<String, Map<org.batfish.datamodel.Edge, BDD>>> arpTrueEdgeBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(
                VRF1,
                ImmutableMap.of(
                    org.batfish.datamodel.Edge.of(NODE1, IFACE1, NODE2, IFACE2), DSTIP1)),
            NODE2,
            ImmutableMap.of(
                VRF1,
                ImmutableMap.of(
                    org.batfish.datamodel.Edge.of(NODE2, IFACE2, NODE1, IFACE1), DSTIP2)));
    BDDFibGenerator generator =
        new BDDFibGenerator(
            arpTrueEdgeBDDs,
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            (node, iface) -> ONE);
    Edge expectedEdge =
        new Edge(
            new TestPreOutVrf(NODE1, VRF1),
            new TestPreOutEdge(NODE1, IFACE1, NODE2, IFACE2),
            DSTIP1);

    assertThat(
        generator
            .generateRules_PreOutVrf_PreOutEdge(
                NODE1::equals, TestPreOutVrf::new, TestPreOutEdge::new)
            .collect(ImmutableList.toImmutableList()),
        contains(expectedEdge));
    // ensure edge is produced by top-level generation function
    assertThat(
        generator
            .generateForwardingEdges(
                NODE1::equals,
                TestPostInVrf::new,
                TestPreOutEdge::new,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        hasItem(expectedEdge));
  }

  @Test
  public void testGenerateRules_PreOutVrf_PreOutInterfaceDisposition_all() {
    Map<String, Map<String, Map<String, BDD>>> neighborUnreachableBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP1)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP1)));
    Map<String, Map<String, Map<String, BDD>>> deliveredToSubnetBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP2)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP2)));
    Map<String, Map<String, Map<String, BDD>>> exitsNetworkBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE2, DSTIP1)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE2, DSTIP1)));
    Map<String, Map<String, Map<String, BDD>>> insufficientInfoBDDs =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE2, DSTIP2)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE2, DSTIP2)));
    BDDFibGenerator generator =
        new BDDFibGenerator(
            ImmutableMap.of(),
            neighborUnreachableBDDs,
            deliveredToSubnetBDDs,
            exitsNetworkBDDs,
            insufficientInfoBDDs,
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            (node, iface) -> ONE);
    Edge expectedEdgeNeighborUnreachable =
        new Edge(
            new TestPreOutVrf(NODE1, VRF1),
            new TestPreOutInterfaceNeighborUnreachable(NODE1, IFACE1),
            DSTIP1);
    Edge expectedEdgeDeliveredToSubnet =
        new Edge(
            new TestPreOutVrf(NODE1, VRF1),
            new TestPreOutInterfaceDeliveredToSubnet(NODE1, IFACE1),
            DSTIP2);
    Edge expectedEdgeExitsNetwork =
        new Edge(
            new TestPreOutVrf(NODE1, VRF1),
            new TestPreOutInterfaceExitsNetwork(NODE1, IFACE2),
            DSTIP1);
    Edge expectedInsufficientInfo =
        new Edge(
            new TestPreOutVrf(NODE1, VRF1),
            new TestPreOutInterfaceInsufficientInfo(NODE1, IFACE2),
            DSTIP2);

    assertThat(
        generator
            .generateRules_PreOutVrf_PreOutInterfaceDisposition(
                NODE1::equals,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        containsInAnyOrder(
            expectedEdgeNeighborUnreachable,
            expectedEdgeDeliveredToSubnet,
            expectedEdgeExitsNetwork,
            expectedInsufficientInfo));
    // ensure at edges are produced by top-level generation function
    assertThat(
        generator
            .generateForwardingEdges(
                NODE1::equals,
                TestPostInVrf::new,
                TestPreOutEdge::new,
                TestPreOutVrf::new,
                TestPreOutInterfaceDeliveredToSubnet::new,
                TestPreOutInterfaceExitsNetwork::new,
                TestPreOutInterfaceInsufficientInfo::new,
                TestPreOutInterfaceNeighborUnreachable::new)
            .collect(ImmutableList.toImmutableList()),
        hasItems(
            expectedEdgeNeighborUnreachable,
            expectedEdgeDeliveredToSubnet,
            expectedEdgeExitsNetwork,
            expectedInsufficientInfo));
  }

  @Test
  public void testGenerateRules_PreOutVrf_PreOutInterfaceDisposition_individual() {
    Map<String, Map<String, Map<String, BDD>>> dispositionBddMap =
        ImmutableMap.of(
            NODE1,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP1)),
            NODE2,
            ImmutableMap.of(VRF1, ImmutableMap.of(IFACE1, DSTIP1)));

    assertThat(
        BDDFibGenerator.generateRules_PreOutVrf_PreOutInterfaceDisposition(
                NODE1::equals,
                dispositionBddMap,
                TestPreOutVrf::new,
                TestPreOutInterfaceExitsNetwork::new,
                (node, iface) -> ONE)
            .collect(ImmutableList.toImmutableList()),
        contains(
            new Edge(
                new TestPreOutVrf(NODE1, VRF1),
                new TestPreOutInterfaceExitsNetwork(NODE1, IFACE1),
                DSTIP1)));
  }

  private static Map<String, Map<String, BDD>> toVrfAcceptBdds(
      Map<String, Map<String, Map<String, BDD>>> ifaceAcceptBdds) {
    return toImmutableMap(
        ifaceAcceptBdds,
        Entry::getKey, // node name
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey, // vrf name
                vrfEntry ->
                    PKT.getFactory().orAll(vrfEntry.getValue().values()))); // vrf's accept BDD}
  }
}

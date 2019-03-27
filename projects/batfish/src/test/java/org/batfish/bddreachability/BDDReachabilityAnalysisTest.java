package org.batfish.bddreachability;

import static org.batfish.bddreachability.BDDReachabilityAnalysis.fixpoint;
import static org.batfish.bddreachability.BDDReachabilityAnalysis.toIngressLocation;
import static org.batfish.bddreachability.BDDReachabilityUtils.computeForwardEdgeTable;
import static org.batfish.bddreachability.TestNetwork.DST_PREFIX_1;
import static org.batfish.bddreachability.TestNetwork.DST_PREFIX_2;
import static org.batfish.bddreachability.TestNetwork.LINK_1_NETWORK;
import static org.batfish.bddreachability.TestNetwork.LINK_2_NETWORK;
import static org.batfish.bddreachability.TestNetwork.POST_SOURCE_NAT_ACL_DEST_PORT;
import static org.batfish.common.bdd.BDDMatchers.intersects;
import static org.batfish.common.bdd.BDDMatchers.isOne;
import static org.batfish.common.bdd.BDDMatchers.isZero;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDInteger;
import org.batfish.common.bdd.BDDOps;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.Accept;
import org.batfish.z3.state.Drop;
import org.batfish.z3.state.NodeAccept;
import org.batfish.z3.state.NodeDropAclIn;
import org.batfish.z3.state.NodeDropAclOut;
import org.batfish.z3.state.NodeDropNoRoute;
import org.batfish.z3.state.NodeDropNullRoute;
import org.batfish.z3.state.NodeInterfaceDeliveredToSubnet;
import org.batfish.z3.state.NodeInterfaceExitsNetwork;
import org.batfish.z3.state.NodeInterfaceInsufficientInfo;
import org.batfish.z3.state.OriginateVrf;
import org.batfish.z3.state.PostInInterface;
import org.batfish.z3.state.PostInVrf;
import org.batfish.z3.state.PreInInterface;
import org.batfish.z3.state.PreOutEdge;
import org.batfish.z3.state.PreOutEdgePostNat;
import org.batfish.z3.state.PreOutInterfaceNeighborUnreachable;
import org.batfish.z3.state.PreOutVrf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests of {@link BDDReachabilityAnalysis}. */
public final class BDDReachabilityAnalysisTest {
  @Rule public TemporaryFolder temp = new TemporaryFolder();

  private static final BDDPacket PKT = new BDDPacket();

  private BDDReachabilityAnalysis _graph;
  private BDDReachabilityAnalysisFactory _graphFactory;
  private TestNetwork _net;

  private BDDOps _bddOps;

  private Ip _dstIface1Ip;
  private BDD _dstIface1IpBDD;
  private Ip _dstIface2Ip;
  private BDD _dstIface2IpBDD;
  private String _dstIface1Name;
  private String _dstIface2Name;
  private String _dstName;
  private NodeAccept _dstNodeAccept;
  private PostInInterface _dstPostInInterface1;
  private PostInInterface _dstPostInInterface2;
  private PostInVrf _dstPostInVrf;
  private PreInInterface _dstPreInInterface1;
  private PreInInterface _dstPreInInterface2;
  private PreOutEdge _dstPreOutEdge1;
  private PreOutEdge _dstPreOutEdge2;
  private PreOutEdgePostNat _dstPreOutEdgePostNat1;
  private PreOutEdgePostNat _dstPreOutEdgePostNat2;
  private PreOutVrf _dstPreOutVrf;

  private BDD _link1DstIpBDD;
  private String _link1DstName;

  private BDD _link1SrcIpBDD;

  private BDD _link2DstIpBDD;
  private String _link2DstName;

  private BDD _link2SrcIpBDD;

  private String _srcName;
  private NodeAccept _srcNodeAccept;
  private PostInVrf _srcPostInVrf;
  private PreInInterface _srcPreInInterface1;
  private PreInInterface _srcPreInInterface2;
  private PreOutEdge _srcPreOutEdge1;
  private PreOutEdge _srcPreOutEdge2;
  private PreOutEdgePostNat _srcPreOutEdgePostNat1;
  private PreOutEdgePostNat _srcPreOutEdgePostNat2;
  private PreOutVrf _srcPreOutVrf;

  @Before
  public void setup() throws IOException {
    _net = new TestNetwork();
    Batfish batfish = BatfishTestUtils.getBatfish(_net._configs, temp);

    batfish.computeDataPlane();
    DataPlane dataPlane = batfish.loadDataPlane();
    _graphFactory =
        new BDDReachabilityAnalysisFactory(
            PKT, _net._configs, dataPlane.getForwardingAnalysis(), false, false);

    IpSpaceAssignment assignment =
        IpSpaceAssignment.builder()
            .assign(
                new InterfaceLocation(_net._srcNode.getHostname(), _net._link1Src.getName()),
                UniverseIpSpace.INSTANCE)
            .build();
    _graph = _graphFactory.bddReachabilityAnalysis(assignment);
    _bddOps = new BDDOps(PKT.getFactory());
    _dstIface1Ip = DST_PREFIX_1.getStartIp();
    _dstIface1IpBDD = dstIpBDD(_dstIface1Ip);
    _dstIface2Ip = DST_PREFIX_2.getStartIp();
    _dstIface2IpBDD = dstIpBDD(_dstIface2Ip);
    _dstIface1Name = _net._dstIface1.getName();
    _dstIface2Name = _net._dstIface2.getName();
    _dstName = _net._dstNode.getHostname();
    _dstNodeAccept = new NodeAccept(_dstName);
    _dstPostInVrf = new PostInVrf(_dstName, DEFAULT_VRF_NAME);
    _dstPreOutVrf = new PreOutVrf(_dstName, DEFAULT_VRF_NAME);

    _link1DstIpBDD = dstIpBDD(LINK_1_NETWORK.getEndIp());
    _link1DstName = _net._link1Dst.getName();

    _link1SrcIpBDD = dstIpBDD(LINK_1_NETWORK.getStartIp());

    _link2DstIpBDD = dstIpBDD(LINK_2_NETWORK.getEndIp());
    _link2DstName = _net._link2Dst.getName();

    _link2SrcIpBDD = dstIpBDD(LINK_2_NETWORK.getStartIp());
    String link2SrcName = _net._link2Src.getName();

    _srcName = _net._srcNode.getHostname();
    _srcNodeAccept = new NodeAccept(_srcName);
    _srcPostInVrf = new PostInVrf(_srcName, DEFAULT_VRF_NAME);

    _dstPostInInterface1 = new PostInInterface(_dstName, _link1DstName);
    _dstPostInInterface2 = new PostInInterface(_dstName, _link2DstName);

    _dstPreInInterface1 = new PreInInterface(_dstName, _link1DstName);
    _dstPreInInterface2 = new PreInInterface(_dstName, _link2DstName);

    _srcPreInInterface1 = new PreInInterface(_srcName, _net._link1Src.getName());
    _srcPreInInterface2 = new PreInInterface(_srcName, link2SrcName);

    _dstPreOutEdge1 = new PreOutEdge(_dstName, _link1DstName, _srcName, _net._link1Src.getName());
    _dstPreOutEdge2 = new PreOutEdge(_dstName, _link2DstName, _srcName, link2SrcName);
    _dstPreOutEdgePostNat1 =
        new PreOutEdgePostNat(_dstName, _link1DstName, _srcName, _net._link1Src.getName());
    _dstPreOutEdgePostNat2 = new PreOutEdgePostNat(_dstName, _link2DstName, _srcName, link2SrcName);
    _srcPreOutEdge1 = new PreOutEdge(_srcName, _net._link1Src.getName(), _dstName, _link1DstName);
    _srcPreOutEdge2 = new PreOutEdge(_srcName, link2SrcName, _dstName, _link2DstName);
    _srcPreOutEdgePostNat1 =
        new PreOutEdgePostNat(_srcName, _net._link1Src.getName(), _dstName, _link1DstName);
    _srcPreOutEdgePostNat2 = new PreOutEdgePostNat(_srcName, link2SrcName, _dstName, _link2DstName);
    _srcPreOutVrf = new PreOutVrf(_srcName, DEFAULT_VRF_NAME);
  }

  private List<Ip> bddIps(BDD bdd) {
    BDDInteger bddInteger = _graphFactory.getIpSpaceToBDD().getBDDInteger();

    return bddInteger.getValuesSatisfying(bdd, 10).stream()
        .map(Ip::create)
        .collect(Collectors.toList());
  }

  private BDD bddTransition(StateExpr preState, StateExpr postState) {
    return _graph
        .getForwardEdgeMap()
        .get(preState)
        .get(postState)
        .traverseForward(PKT.getFactory().one());
  }

  private Edge edge(StateExpr preState, StateExpr postState) {
    return _graph.getForwardEdgeMap().get(preState).get(postState);
  }

  private static BDD dstIpBDD(Ip ip) {
    return new IpSpaceToBDD(PKT.getDstIp()).toBDD(ip);
  }

  private static BDD dstPortBDD(int destPort) {
    return PKT.getDstPort().value(destPort);
  }

  private BDD or(BDD... bdds) {
    return _bddOps.or(bdds);
  }

  private BDD vrfAcceptBDD(String node) {
    return _graphFactory.getVrfAcceptBDDs().get(node).get(DEFAULT_VRF_NAME);
  }

  @Test
  public void testVrfAcceptBDDs() {
    assertThat(
        vrfAcceptBDD(_dstName),
        equalTo(or(_link1DstIpBDD, _link2DstIpBDD, _dstIface1IpBDD, _dstIface2IpBDD)));
    assertThat(vrfAcceptBDD(_srcName), equalTo(or(_link1SrcIpBDD, _link2SrcIpBDD)));
  }

  @Test
  public void testBDDTransitions_NodeAccept_Accept() {
    assertThat(bddTransition(_srcNodeAccept, Accept.INSTANCE), isOne());
    assertThat(bddTransition(_dstNodeAccept, Accept.INSTANCE), isOne());
  }

  @Test
  public void testBDDTransitions_PostInVrf_outEdges() {
    BDD nodeAccept = bddTransition(_srcPostInVrf, _srcNodeAccept);
    BDD nodeDropNoRoute = bddTransition(_srcPostInVrf, new NodeDropNoRoute(_srcName));
    BDD preOutVrf = bddTransition(_srcPostInVrf, _srcPreOutVrf);

    // test that out edges are mutually exclusive
    assertThat(nodeAccept, not(intersects(nodeDropNoRoute)));
    assertThat(nodeAccept, not(intersects(preOutVrf)));
    assertThat(nodeDropNoRoute, not(intersects(preOutVrf)));
  }

  @Test
  public void testBDDTransitions_PostInVrf_NodeAccept() {
    assertThat(
        bddTransition(_srcPostInVrf, new NodeAccept(_srcName)),
        equalTo(or(_link1SrcIpBDD, _link2SrcIpBDD)));
    assertThat(
        bddTransition(_dstPostInVrf, new NodeAccept(_dstName)),
        equalTo(or(_link1DstIpBDD, _link2DstIpBDD, _dstIface1IpBDD, _dstIface2IpBDD)));
  }

  @Test
  public void testBDDTransitions_PostInVrf_PreOutVrf() {
    assertThat(
        bddTransition(_dstPostInVrf, _dstPreOutVrf), equalTo(or(_link1SrcIpBDD, _link2SrcIpBDD)));

    assertThat(
        bddTransition(_srcPostInVrf, _srcPreOutVrf),
        equalTo(or(_link1DstIpBDD, _link2DstIpBDD, _dstIface1IpBDD, _dstIface2IpBDD)));
  }

  @Test
  public void testBDDTransitions_PreInInterface_NodeDropAclIn() {
    NodeDropAclIn dstDropAclIn = new NodeDropAclIn(_dstName);
    assertThat(bddTransition(_dstPreInInterface1, dstDropAclIn), equalTo(dstIpBDD(_dstIface2Ip)));
    assertThat(edge(_dstPreInInterface2, dstDropAclIn), nullValue());
  }

  @Test
  public void testBDDTransitions_PreInInterface_PostInInterface() {
    // link1: not(_dstIface2Ip)
    assertThat(
        bddTransition(_dstPreInInterface1, _dstPostInInterface1),
        equalTo(dstIpBDD(_dstIface2Ip).not()));
    // link2: universe
    assertThat(bddTransition(_dstPreInInterface2, _dstPostInInterface2), isOne());
  }

  @Test
  public void testBDDTransitions_PreOutVrf_outEdges() {
    String link1SrcName = _net._link1Src.getName();
    String link2SrcName = _net._link2Src.getName();
    BDD nodeDropNullRoute = bddTransition(_srcPreOutVrf, new NodeDropNullRoute(_srcName));
    BDD nodeInterfaceNeighborUnreachable1 =
        bddTransition(
            _srcPreOutVrf, new PreOutInterfaceNeighborUnreachable(_srcName, link1SrcName));
    BDD nodeInterfaceNeighborUnreachable2 =
        bddTransition(
            _srcPreOutVrf, new PreOutInterfaceNeighborUnreachable(_srcName, link2SrcName));
    BDD preOutEdge1 = bddTransition(_srcPreOutVrf, _srcPreOutEdge1);
    BDD preOutEdge2 = bddTransition(_srcPreOutVrf, _srcPreOutEdge2);

    assertThat(nodeDropNullRoute, isZero());

    assertEquals(nodeInterfaceNeighborUnreachable1, _link1SrcIpBDD);
    assertEquals(nodeInterfaceNeighborUnreachable2, _link2SrcIpBDD);

    assertThat(
        bddIps(preOutEdge1),
        containsInAnyOrder(_dstIface1Ip, _dstIface2Ip, _net._link1Dst.getAddress().getIp()));
    assertThat(
        bddIps(preOutEdge2), containsInAnyOrder(_dstIface2Ip, _net._link2Dst.getAddress().getIp()));

    // ECMP: _dstIface1Ip is routed out both edges
    assertThat(preOutEdge1.and(preOutEdge2), equalTo(dstIpBDD(_dstIface2Ip)));
  }

  @Test
  public void testBDDTransitions_PreOutVrf_NodeInterfaceDisposition() {
    // delievered to subnet
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceDeliveredToSubnet(_dstName, _dstIface1Name)),
        nullValue());
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceDeliveredToSubnet(_dstName, _dstIface2Name)),
        nullValue());
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceDeliveredToSubnet(_dstName, _link1DstName)),
        nullValue());
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceDeliveredToSubnet(_dstName, _link2DstName)),
        nullValue());

    // exits network
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceExitsNetwork(_dstName, _dstIface1Name)),
        nullValue());
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceExitsNetwork(_dstName, _dstIface2Name)),
        nullValue());
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceExitsNetwork(_dstName, _link1DstName)),
        nullValue());
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceExitsNetwork(_dstName, _link2DstName)),
        nullValue());

    // neighbor unreachable
    assertThat(
        bddTransition(
            _dstPreOutVrf, new PreOutInterfaceNeighborUnreachable(_dstName, _dstIface1Name)),
        equalTo(_dstIface1IpBDD));
    assertThat(
        bddTransition(
            _dstPreOutVrf, new PreOutInterfaceNeighborUnreachable(_dstName, _dstIface2Name)),
        equalTo(_dstIface2IpBDD));
    assertThat(
        bddTransition(
            _dstPreOutVrf, new PreOutInterfaceNeighborUnreachable(_dstName, _link1DstName)),
        equalTo(_link1DstIpBDD));
    assertThat(
        bddTransition(
            _dstPreOutVrf, new PreOutInterfaceNeighborUnreachable(_dstName, _link2DstName)),
        equalTo(_link2DstIpBDD));

    // insufficient info
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceInsufficientInfo(_dstName, _dstIface1Name)),
        nullValue());
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceInsufficientInfo(_dstName, _dstIface2Name)),
        nullValue());
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceInsufficientInfo(_dstName, _link1DstName)),
        nullValue());
    assertThat(
        _graph
            .getForwardEdgeMap()
            .get(_dstPreOutVrf)
            .get(new NodeInterfaceInsufficientInfo(_dstName, _link2DstName)),
        nullValue());
  }

  @Test
  public void testBDDTransitions_PreOutVrf_PreOutEdge() {
    assertThat(
        bddTransition(_srcPreOutVrf, _srcPreOutEdge1),
        equalTo(or(_link1DstIpBDD, _dstIface1IpBDD, _dstIface2IpBDD)));
    assertThat(
        bddTransition(_srcPreOutVrf, _srcPreOutEdge2),
        equalTo(or(_link2DstIpBDD, _dstIface2IpBDD)));

    assertThat(bddTransition(_dstPreOutVrf, _dstPreOutEdge1), equalTo(_link1SrcIpBDD));
    assertThat(bddTransition(_dstPreOutVrf, _dstPreOutEdge2), equalTo(_link2SrcIpBDD));
  }

  @Test
  public void testBDDTransitions_PreOutEdgePostNat_NodeDropAclOut() {
    assertThat(edge(_dstPreOutEdgePostNat1, new NodeDropAclOut(_dstName)), nullValue());
    assertThat(edge(_dstPreOutEdgePostNat2, new NodeDropAclOut(_dstName)), nullValue());
    assertThat(edge(_srcPreOutEdgePostNat1, new NodeDropAclOut(_srcName)), nullValue());
    assertThat(
        bddTransition(_srcPreOutEdgePostNat2, new NodeDropAclOut(_srcName)),
        equalTo(dstPortBDD(POST_SOURCE_NAT_ACL_DEST_PORT).not()));
  }

  @Test
  public void testBDDTransitions_PreOutEdgePostNat_PreInInterface() {
    assertThat(bddTransition(_dstPreOutEdgePostNat1, _srcPreInInterface1), isOne());
    assertThat(bddTransition(_dstPreOutEdgePostNat2, _srcPreInInterface2), isOne());
    assertThat(bddTransition(_srcPreOutEdgePostNat1, _dstPreInInterface1), isOne());
    assertThat(
        bddTransition(_srcPreOutEdgePostNat2, _dstPreInInterface2),
        equalTo(dstPortBDD(POST_SOURCE_NAT_ACL_DEST_PORT)));
  }

  @Test
  public void testDefaultAcceptBDD() {
    BDDPacket pkt = new BDDPacket();
    String hostname = "host";
    OriginateVrf originateVrf = new OriginateVrf(hostname, "vrf");
    BDD one = pkt.getFactory().one();
    BDDReachabilityAnalysis graph =
        new BDDReachabilityAnalysis(
            pkt,
            ImmutableSet.of(originateVrf),
            ImmutableList.of(new Edge(originateVrf, Drop.INSTANCE, one)),
            one);
    assertThat(
        graph.getIngressLocationReachableBDDs(),
        equalTo(ImmutableMap.of(toIngressLocation(originateVrf), pkt.getFactory().zero())));
  }

  @Test
  public void testFixpoint() {
    StateExpr a = new NodeAccept("A");
    StateExpr b = new NodeAccept("B");
    StateExpr c = new NodeAccept("C");

    BDD start = PKT.getSrcPort().value(1);
    BDD bddAB = PKT.getDstIp().value(1);
    BDD bddBC = PKT.getSrcIp().value(1);

    Edge edgeAB = new Edge(a, b, bddAB);
    Edge edgeBC = new Edge(b, c, bddBC);

    Table<StateExpr, StateExpr, Edge> forwardEdges =
        computeForwardEdgeTable(ImmutableList.of(edgeAB, edgeBC));
    Table<StateExpr, StateExpr, Edge> reverseEdges = Tables.transpose(forwardEdges);

    // forward from a
    {
      Map<StateExpr, BDD> forwardReachability = new HashMap<>();
      forwardReachability.put(a, start);
      fixpoint(forwardReachability, forwardEdges, Edge::traverseForward);
      assertThat(
          forwardReachability,
          equalTo(
              ImmutableMap.of(
                  a, start, //
                  b, start.and(bddAB), //
                  c, start.and(bddAB).and(bddBC))));
    }

    // forward from c
    {
      Map<StateExpr, BDD> forwardReachability = new HashMap<>();
      forwardReachability.put(c, start);
      fixpoint(forwardReachability, forwardEdges, Edge::traverseForward);
      assertThat(forwardReachability, equalTo(ImmutableMap.of(c, start)));
    }

    // reverse from a
    {
      Map<StateExpr, BDD> reverseReachability = new HashMap<>();
      reverseReachability.put(a, start);
      fixpoint(reverseReachability, reverseEdges, Edge::traverseBackward);
      assertThat(reverseReachability, equalTo(ImmutableMap.of(a, start)));
    }

    // reverse from c
    {
      Map<StateExpr, BDD> reverseReachability = new HashMap<>();
      reverseReachability.put(c, start);
      fixpoint(reverseReachability, reverseEdges, Edge::traverseBackward);
      assertThat(
          reverseReachability,
          equalTo(
              ImmutableMap.of(
                  a, start.and(bddBC).and(bddAB), //
                  b, start.and(bddBC),
                  c, start)));
    }
  }
}

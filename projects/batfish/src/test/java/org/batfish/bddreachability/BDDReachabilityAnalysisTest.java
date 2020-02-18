package org.batfish.bddreachability;

import static org.batfish.bddreachability.BDDReachabilityUtils.computeForwardEdgeTable;
import static org.batfish.bddreachability.BDDReachabilityUtils.fixpoint;
import static org.batfish.bddreachability.BDDReachabilityUtils.toIngressLocation;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.common.bdd.BDDInteger;
import org.batfish.common.bdd.BDDOps;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.symbolic.state.Accept;
import org.batfish.symbolic.state.DropNoRoute;
import org.batfish.symbolic.state.InterfaceAccept;
import org.batfish.symbolic.state.NodeAccept;
import org.batfish.symbolic.state.NodeDropAclIn;
import org.batfish.symbolic.state.NodeDropAclOut;
import org.batfish.symbolic.state.NodeDropNoRoute;
import org.batfish.symbolic.state.NodeDropNullRoute;
import org.batfish.symbolic.state.NodeInterfaceDeliveredToSubnet;
import org.batfish.symbolic.state.NodeInterfaceExitsNetwork;
import org.batfish.symbolic.state.NodeInterfaceInsufficientInfo;
import org.batfish.symbolic.state.OriginateInterface;
import org.batfish.symbolic.state.OriginateInterfaceLink;
import org.batfish.symbolic.state.OriginateVrf;
import org.batfish.symbolic.state.PostInInterface;
import org.batfish.symbolic.state.PostInVrf;
import org.batfish.symbolic.state.PreInInterface;
import org.batfish.symbolic.state.PreOutEdge;
import org.batfish.symbolic.state.PreOutEdgePostNat;
import org.batfish.symbolic.state.PreOutInterfaceNeighborUnreachable;
import org.batfish.symbolic.state.PreOutVrf;
import org.batfish.symbolic.state.Query;
import org.batfish.symbolic.state.StateExpr;
import org.batfish.symbolic.state.VrfAccept;
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
  private VrfAccept _dstVrfAccept;
  private InterfaceAccept _link1DstAccept;
  private InterfaceAccept _link2DstAccept;
  private InterfaceAccept _dstIface1Accept;
  private InterfaceAccept _dstIface2Accept;
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
  private String _link1SrcName;

  private BDD _link2DstIpBDD;
  private String _link2DstName;

  private BDD _link2SrcIpBDD;
  private String _link2SrcName;

  private String _srcName;
  private NodeAccept _srcNodeAccept;
  private VrfAccept _srcVrfAccept;
  private InterfaceAccept _link1SrcAccept;
  private InterfaceAccept _link2SrcAccept;
  private PostInVrf _srcPostInVrf;
  private PreInInterface _srcPreInInterface1;
  private PreInInterface _srcPreInInterface2;
  private PreOutEdge _srcPreOutEdge1;
  private PreOutEdge _srcPreOutEdge2;
  private PreOutEdgePostNat _srcPreOutEdgePostNat1;
  private PreOutEdgePostNat _srcPreOutEdgePostNat2;
  private PreOutVrf _srcPreOutVrf;

  private static final BDD TCP_BDD = PKT.getIpProtocol().value(IpProtocol.TCP);

  @Before
  public void setup() throws IOException {
    _net = new TestNetwork();
    Batfish batfish = BatfishTestUtils.getBatfish(_net._configs, temp);

    _link1DstIpBDD = dstIpBDD(LINK_1_NETWORK.getEndIp());
    _link1DstName = _net._link1Dst.getName();

    _link1SrcIpBDD = dstIpBDD(LINK_1_NETWORK.getStartIp());
    _link1SrcName = _net._link1Src.getName();

    _link2DstIpBDD = dstIpBDD(LINK_2_NETWORK.getEndIp());
    _link2DstName = _net._link2Dst.getName();

    _link2SrcIpBDD = dstIpBDD(LINK_2_NETWORK.getStartIp());
    _link2SrcName = _net._link2Src.getName();

    batfish.computeDataPlane(batfish.getSnapshot());
    DataPlane dataPlane = batfish.loadDataPlane(batfish.getSnapshot());
    _graphFactory =
        new BDDReachabilityAnalysisFactory(
            PKT,
            _net._configs,
            dataPlane.getForwardingAnalysis(),
            new IpsRoutedOutInterfacesFactory(dataPlane.getFibs()),
            false,
            false);

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
    _dstVrfAccept = new VrfAccept(_dstName, DEFAULT_VRF_NAME);
    _link1DstAccept = new InterfaceAccept(_dstName, _link1DstName);
    _link2DstAccept = new InterfaceAccept(_dstName, _link2DstName);
    _dstIface1Accept = new InterfaceAccept(_dstName, _dstIface1Name);
    _dstIface2Accept = new InterfaceAccept(_dstName, _dstIface2Name);
    _dstPostInVrf = new PostInVrf(_dstName, DEFAULT_VRF_NAME);
    _dstPreOutVrf = new PreOutVrf(_dstName, DEFAULT_VRF_NAME);

    _srcName = _net._srcNode.getHostname();
    _srcNodeAccept = new NodeAccept(_srcName);
    _srcVrfAccept = new VrfAccept(_srcName, DEFAULT_VRF_NAME);
    _link1SrcAccept = new InterfaceAccept(_srcName, _link1SrcName);
    _link2SrcAccept = new InterfaceAccept(_srcName, _link2SrcName);
    _srcPostInVrf = new PostInVrf(_srcName, DEFAULT_VRF_NAME);

    _dstPostInInterface1 = new PostInInterface(_dstName, _link1DstName);
    _dstPostInInterface2 = new PostInInterface(_dstName, _link2DstName);

    _dstPreInInterface1 = new PreInInterface(_dstName, _link1DstName);
    _dstPreInInterface2 = new PreInInterface(_dstName, _link2DstName);

    _srcPreInInterface1 = new PreInInterface(_srcName, _net._link1Src.getName());
    _srcPreInInterface2 = new PreInInterface(_srcName, _link2SrcName);

    _dstPreOutEdge1 = new PreOutEdge(_dstName, _link1DstName, _srcName, _net._link1Src.getName());
    _dstPreOutEdge2 = new PreOutEdge(_dstName, _link2DstName, _srcName, _link2SrcName);
    _dstPreOutEdgePostNat1 =
        new PreOutEdgePostNat(_dstName, _link1DstName, _srcName, _net._link1Src.getName());
    _dstPreOutEdgePostNat2 =
        new PreOutEdgePostNat(_dstName, _link2DstName, _srcName, _link2SrcName);
    _srcPreOutEdge1 = new PreOutEdge(_srcName, _net._link1Src.getName(), _dstName, _link1DstName);
    _srcPreOutEdge2 = new PreOutEdge(_srcName, _link2SrcName, _dstName, _link2DstName);
    _srcPreOutEdgePostNat1 =
        new PreOutEdgePostNat(_srcName, _net._link1Src.getName(), _dstName, _link1DstName);
    _srcPreOutEdgePostNat2 =
        new PreOutEdgePostNat(_srcName, _link2SrcName, _dstName, _link2DstName);
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
        .transitForward(PKT.getFactory().one());
  }

  private Edge edge(StateExpr preState, StateExpr postState) {
    Transition transition = _graph.getForwardEdgeMap().get(preState).get(postState);
    return transition == null ? null : new Edge(preState, postState, transition);
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

  private Map<String, BDD> ifaceAcceptBDDs(String node) {
    return _graphFactory.getIfaceAcceptBDDs().get(node).get(DEFAULT_VRF_NAME);
  }

  @Test
  public void testIfaceAcceptBDDs() {
    assertThat(
        ifaceAcceptBDDs(_dstName),
        equalTo(
            ImmutableMap.of(
                _link1DstName,
                _link1DstIpBDD,
                _link2DstName,
                _link2DstIpBDD,
                _dstIface1Name,
                _dstIface1IpBDD,
                _dstIface2Name,
                _dstIface2IpBDD)));
    assertThat(
        ifaceAcceptBDDs(_srcName),
        equalTo(ImmutableMap.of(_link1SrcName, _link1SrcIpBDD, _link2SrcName, _link2SrcIpBDD)));
  }

  @Test
  public void testBDDTransitions_InterfaceAccept_VrfAccept() {
    assertThat(bddTransition(_link1SrcAccept, _srcVrfAccept), isOne());
    assertThat(bddTransition(_link2SrcAccept, _srcVrfAccept), isOne());
    assertThat(bddTransition(_link1DstAccept, _dstVrfAccept), isOne());
    assertThat(bddTransition(_link2DstAccept, _dstVrfAccept), isOne());
    assertThat(bddTransition(_dstIface1Accept, _dstVrfAccept), isOne());
    assertThat(bddTransition(_dstIface2Accept, _dstVrfAccept), isOne());
  }

  @Test
  public void testBDDTransitions_VrfAccept_NodeAccept() {
    assertThat(bddTransition(_srcVrfAccept, _srcNodeAccept), isOne());
    assertThat(bddTransition(_dstVrfAccept, _dstNodeAccept), isOne());
  }

  @Test
  public void testBDDTransitions_NodeAccept_Accept() {
    assertThat(bddTransition(_srcNodeAccept, Accept.INSTANCE), isOne());
    assertThat(bddTransition(_dstNodeAccept, Accept.INSTANCE), isOne());
  }

  @Test
  public void testBDDTransitions_PostInVrf_outEdges() {
    Set<StateExpr> srcPostInVrfOutStates = _graph.getForwardEdgeMap().get(_srcPostInVrf).keySet();
    Set<StateExpr> dstPostInVrfOutStates = _graph.getForwardEdgeMap().get(_dstPostInVrf).keySet();

    // Confirm out edges are as expected
    assertThat(
        srcPostInVrfOutStates,
        containsInAnyOrder(
            _link1SrcAccept, _link2SrcAccept, new NodeDropNoRoute(_srcName), _srcPreOutVrf));
    assertThat(
        dstPostInVrfOutStates,
        containsInAnyOrder(
            _link1DstAccept,
            _link2DstAccept,
            _dstIface1Accept,
            _dstIface2Accept,
            new NodeDropNoRoute(_dstName),
            _dstPreOutVrf));

    // Test that out edges are mutually exclusive
    List<BDD> srcOutBdds =
        srcPostInVrfOutStates.stream()
            .map(outState -> bddTransition(_srcPostInVrf, outState))
            .collect(ImmutableList.toImmutableList());
    List<BDD> dstOutBdds =
        dstPostInVrfOutStates.stream()
            .map(outState -> bddTransition(_dstPostInVrf, outState))
            .collect(ImmutableList.toImmutableList());
    testMutuallyExclusive(srcOutBdds);
    testMutuallyExclusive(dstOutBdds);
  }

  private void testMutuallyExclusive(List<BDD> bdds) {
    for (int i = 0; i < bdds.size(); i++) {
      for (int j = i + 1; j < bdds.size(); j++) {
        assertThat(bdds.get(i), not(intersects(bdds.get(j))));
      }
    }
  }

  @Test
  public void testBDDTransitions_PostInVrf_InterfaceAccept() {
    BDD link1SrcAcceptBdd = bddTransition(_srcPostInVrf, _link1SrcAccept);
    BDD link2SrcAcceptBdd = bddTransition(_srcPostInVrf, _link2SrcAccept);
    assertThat(link1SrcAcceptBdd, equalTo(_link1SrcIpBDD));
    assertThat(link2SrcAcceptBdd, equalTo(_link2SrcIpBDD));

    BDD link1DstAcceptBdd = bddTransition(_dstPostInVrf, _link1DstAccept);
    BDD link2DstAcceptBdd = bddTransition(_dstPostInVrf, _link2DstAccept);
    BDD dstIface1AcceptBdd = bddTransition(_dstPostInVrf, _dstIface1Accept);
    BDD dstIface2AcceptBdd = bddTransition(_dstPostInVrf, _dstIface2Accept);
    assertThat(link1DstAcceptBdd, equalTo(_link1DstIpBDD));
    assertThat(link2DstAcceptBdd, equalTo(_link2DstIpBDD));
    assertThat(dstIface1AcceptBdd, equalTo(_dstIface1IpBDD));
    assertThat(dstIface2AcceptBdd, equalTo(_dstIface2IpBDD));
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
        containsInAnyOrder(
            _dstIface1Ip, _dstIface2Ip, _net._link1Dst.getConcreteAddress().getIp()));
    assertThat(
        bddIps(preOutEdge2),
        containsInAnyOrder(_dstIface2Ip, _net._link2Dst.getConcreteAddress().getIp()));

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
        equalTo(TCP_BDD.and(dstPortBDD(POST_SOURCE_NAT_ACL_DEST_PORT)).not()));
  }

  @Test
  public void testBDDTransitions_PreOutEdgePostNat_PreInInterface() {
    assertThat(bddTransition(_dstPreOutEdgePostNat1, _srcPreInInterface1), isOne());
    assertThat(bddTransition(_dstPreOutEdgePostNat2, _srcPreInInterface2), isOne());
    assertThat(bddTransition(_srcPreOutEdgePostNat1, _dstPreInInterface1), isOne());
    assertThat(
        bddTransition(_srcPreOutEdgePostNat2, _dstPreInInterface2),
        equalTo(TCP_BDD.and(dstPortBDD(POST_SOURCE_NAT_ACL_DEST_PORT))));
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
            Stream.of(new Edge(originateVrf, DropNoRoute.INSTANCE, one)),
            one);
    assertThat(
        graph.getIngressLocationReachableBDDs(),
        equalTo(ImmutableMap.of(toIngressLocation(originateVrf), pkt.getFactory().zero())));
  }

  @Test
  public void testGetSrcLocationBdds() {
    // Simple reachability graph starting with interface states. Include an intermediate state to
    // ensure reverseReachableStates are being correctly used (intermediate state is PostInVrf).
    String hostname = "c";
    String ifaceName = "iface";
    OriginateInterface originateInterface = new OriginateInterface(hostname, ifaceName);
    OriginateInterfaceLink originateInterfaceLink = new OriginateInterfaceLink(hostname, ifaceName);
    PostInVrf postInVrf = new PostInVrf(hostname, "vrf");
    BDD ifaceOriginateBdd = PKT.getDstIp().value(1);
    BDD ifaceLinkBdd = PKT.getDstIp().value(2);
    BDD postInVrfBdd = PKT.getSrcIp().value(3); // srcIp, so not mutually exclusive w/ other BDDS
    BDDReachabilityAnalysis analysis =
        new BDDReachabilityAnalysis(
            PKT,
            ImmutableSet.of(originateInterface, originateInterfaceLink),
            Stream.of(
                new Edge(originateInterface, Query.INSTANCE, ifaceOriginateBdd),
                new Edge(originateInterfaceLink, postInVrf, ifaceLinkBdd),
                new Edge(postInVrf, Query.INSTANCE, postInVrfBdd)),
            PKT.getFactory().one());

    // The origination states should translate to Locations with the expected success BDDs.
    InterfaceLocation ifaceLocation = new InterfaceLocation(hostname, ifaceName);
    InterfaceLinkLocation ifaceLinkLocation = new InterfaceLinkLocation(hostname, ifaceName);
    assertThat(
        analysis.getSrcLocationBdds(),
        equalTo(
            ImmutableMap.of(
                ifaceLocation,
                ifaceOriginateBdd,
                ifaceLinkLocation,
                ifaceLinkBdd.and(postInVrfBdd))));
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

    Table<StateExpr, StateExpr, Transition> forwardEdges =
        computeForwardEdgeTable(ImmutableList.of(edgeAB, edgeBC));
    Table<StateExpr, StateExpr, Transition> reverseEdges = Tables.transpose(forwardEdges);

    // forward from a
    {
      Map<StateExpr, BDD> forwardReachability = new HashMap<>();
      forwardReachability.put(a, start);
      fixpoint(forwardReachability, forwardEdges, Transition::transitForward);
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
      fixpoint(forwardReachability, forwardEdges, Transition::transitForward);
      assertThat(forwardReachability, equalTo(ImmutableMap.of(c, start)));
    }

    // reverse from a
    {
      Map<StateExpr, BDD> reverseReachability = new HashMap<>();
      reverseReachability.put(a, start);
      fixpoint(reverseReachability, reverseEdges, Transition::transitBackward);
      assertThat(reverseReachability, equalTo(ImmutableMap.of(a, start)));
    }

    // reverse from c
    {
      Map<StateExpr, BDD> reverseReachability = new HashMap<>();
      reverseReachability.put(c, start);
      fixpoint(reverseReachability, reverseEdges, Transition::transitBackward);
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

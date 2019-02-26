package org.batfish.bddreachability;

import static org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchers.hasHostname;
import static org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchers.hasIncomingInterfaces;
import static org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchers.hasNextHop;
import static org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchers.hasOutgoingInterface;
import static org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchers.hasSessionFlows;
import static org.batfish.bddreachability.BDDFirewallSessionTraceInfoMatchers.hasTransformation;
import static org.batfish.bddreachability.BDDReachabilityAnalysisSessionFactory.computeInitializedSesssions;
import static org.batfish.bddreachability.transition.Transitions.compose;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.common.BatfishException;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.HeaderSpaceToBDD;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.PreInInterface;
import org.batfish.z3.state.PreOutEdgePostNat;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link BDDReachabilityAnalysisSessionFactory}. */
@SuppressWarnings("unchecked")
public class BDDReachabilityAnalysisSessionFactoryTest {
  private static final String FW = "FW";
  private static final String R1 = "R1";
  private static final String R2 = "R2";
  private static final String R3 = "R3";

  // Just used for PreOutEdgePostNat on FW
  private static final String BORDER = "BORDER";
  private static final String BORDER_IFACE = "BORDER_IFACE";

  // interface names
  private static final String R1I1 = "R1I1";
  private static final String R2I1 = "R2I1";
  private static final String R3I1 = "R3I1";

  private static final String FWI1 = "FWI1";
  private static final String FWI2 = "FWI2";
  private static final String FWI3 = "FWI3";

  private static final BDDPacket PKT = new BDDPacket();
  private static final HeaderSpaceToBDD TO_BDD = new HeaderSpaceToBDD(PKT, ImmutableMap.of());

  private LastHopOutgoingInterfaceManager _lastHopMgr;
  private BDDSourceManager _fwSrcMgr;

  private Map<String, Configuration> _configs;

  // transformations
  private MockBDDReverseFlowTransformationFactory _reverseFlowTransformationFactory;
  private Transition _fwI3ToI1Transition;
  private Transition _fwI3ToI2Transition;

  private static BDD dstBdd(Prefix prefix) {
    return TO_BDD.getDstIpSpaceToBdd().toBDD(prefix);
  }

  private static BDD srcBdd(Prefix prefix) {
    return TO_BDD.getSrcIpSpaceToBdd().toBDD(prefix);
  }

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration fw = cb.setHostname(FW).build();
    Configuration r1 = cb.setHostname(R1).build();
    Configuration r2 = cb.setHostname(R2).build();
    Configuration r3 = cb.setHostname(R3).build();

    Interface.Builder ib = nf.interfaceBuilder().setActive(true);

    // initialize R1
    // interfaces
    Interface r1i1;
    {
      Vrf vrf = nf.vrfBuilder().setOwner(r1).build();
      ib.setOwner(r1).setVrf(vrf);
      r1i1 = ib.setName(R1I1).build();
    }

    // initialize R2
    Interface r2i1;
    {
      Vrf vrf = nf.vrfBuilder().setOwner(r2).build();
      ib.setOwner(r2).setVrf(vrf);
      r2i1 = ib.setName(R2I1).build();
    }

    // initialize R3
    Interface r3i1;
    {
      Vrf vrf = nf.vrfBuilder().setOwner(r3).build();
      ib.setOwner(r3).setVrf(vrf);
      r3i1 = ib.setName(R3I1).build();
    }

    // initialize FW
    Interface fwi1;
    Interface fwi2;
    {
      Vrf vrf = nf.vrfBuilder().setOwner(fw).build();
      ib.setOwner(fw).setVrf(vrf);
      fwi1 = ib.setName(FWI1).build();
      fwi2 = ib.setName(FWI2).build();
      Interface fwi3 = ib.setName(FWI3).build();

      // Create sessions for flows exiting FW:I3
      fwi3.setFirewallSessionInterfaceInfo(
          new FirewallSessionInterfaceInfo(ImmutableSet.of(FWI3), null, null));
    }

    _configs = ImmutableMap.of(FW, fw, R1, r1, R2, r2, R3, r3);
    _fwSrcMgr = BDDSourceManager.forInterfaces(PKT, ImmutableSet.of(FWI1, FWI2, FWI3));

    // temporarily add a FirewallSessionInterfaceInfo to FW to force its last hops to be tracked
    fwi1.setFirewallSessionInterfaceInfo(
        new FirewallSessionInterfaceInfo(ImmutableList.of(FWI2), null, null));
    Set<org.batfish.datamodel.Edge> edges =
        ImmutableSet.of(
            // R1:I1 -- FW:I1
            new org.batfish.datamodel.Edge(r1i1, fwi1),
            new org.batfish.datamodel.Edge(fwi1, r1i1),
            // R2:I1 -- FW:I1
            new org.batfish.datamodel.Edge(r2i1, fwi1),
            new org.batfish.datamodel.Edge(fwi1, r2i1),
            // R3:I1 -- FW:I2
            new org.batfish.datamodel.Edge(r3i1, fwi2),
            new org.batfish.datamodel.Edge(fwi2, r3i1));
    _lastHopMgr = new LastHopOutgoingInterfaceManager(PKT, _configs, edges);

    // transformations
    Transition fwI1Transition = new MockTransition(FWI1);
    Transition fwI2Transition = new MockTransition(FWI2);
    Transition fwI3Transition = new MockTransition(FWI3);
    _fwI3ToI1Transition = compose(fwI3Transition, fwI1Transition);
    _fwI3ToI2Transition = compose(fwI3Transition, fwI2Transition);
    _reverseFlowTransformationFactory =
        new MockBDDReverseFlowTransformationFactory(
            // incoming transformations
            ImmutableMap.of(
                new NodeInterfacePair(FW, FWI1), fwI1Transition,
                new NodeInterfacePair(FW, FWI2), fwI2Transition),
            // outgoing transformations
            ImmutableMap.of(new NodeInterfacePair(FW, FWI3), fwI3Transition));
  }

  public Map<String, List<BDDFirewallSessionTraceInfo>> computeInitializedSessions(
      Map<StateExpr, BDD> forwardReachableSets) {

    return computeInitializedSesssions(
        PKT,
        _configs,
        ImmutableMap.of(FW, _fwSrcMgr, R1, _fwSrcMgr, R2, _fwSrcMgr),
        _lastHopMgr,
        forwardReachableSets,
        _reverseFlowTransformationFactory);
  }

  /** Transition class that is only used to test how session transformations are built. */
  private final class MockTransition implements Transition {
    private final String _id;

    private MockTransition(String id) {
      _id = id;
    }

    @Override
    public BDD transitForward(BDD bdd) {
      throw new IllegalStateException("Should never be called");
    }

    @Override
    public BDD transitBackward(BDD bdd) {
      throw new IllegalStateException("Should never be called");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MockTransition)) {
        return false;
      }
      MockTransition that = (MockTransition) o;
      return Objects.equals(_id, that._id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_id);
    }
  }

  @Test
  public void testSinglePath() {
    // One path: R1:I1 -> FW:I1 -- FW:I3 -> BORDER_IFACE:BORDER

    // R1:I1 -> FW:I1
    BDD inBdd =
        _lastHopMgr
            .getLastHopOutgoingInterfaceBdd(R1, R1I1, FW, FWI1)
            .and(_fwSrcMgr.getSourceInterfaceBDD(FWI1));

    Prefix routePrefix = Prefix.parse("1.0.0.0/8");
    BDD fwdRouteBdd = dstBdd(routePrefix);
    BDD outBdd = inBdd.and(fwdRouteBdd);

    BDD sessionFlows = srcBdd(routePrefix);

    Map<StateExpr, BDD> forwardReachableSets =
        ImmutableMap.of(
            new PreInInterface(FW, FWI1), inBdd,
            new PreOutEdgePostNat(FW, FWI3, BORDER, BORDER_IFACE), outBdd);

    Map<String, List<BDDFirewallSessionTraceInfo>> sessions =
        computeInitializedSessions(forwardReachableSets);

    assertThat(sessions.keySet(), contains(FW));
    List<BDDFirewallSessionTraceInfo> fwSessions = sessions.get(FW);
    assertThat(fwSessions, hasSize(1));
    BDDFirewallSessionTraceInfo fwSession = fwSessions.get(0);

    assertThat(fwSession, hasHostname(FW));
    assertThat(fwSession, hasIncomingInterfaces(contains(FWI3)));
    assertThat(fwSession, hasNextHop(new NodeInterfacePair(R1, R1I1)));
    assertThat(fwSession, hasOutgoingInterface(FWI1));
    assertThat(fwSession, hasSessionFlows(sessionFlows));
    assertThat(fwSession, hasTransformation(_fwI3ToI1Transition));
  }

  @Test
  public void testDifferentLastHops() {
    /* Two paths, different last-hops, same source interface:
     * R1:I1 -> FW:I1 -- FW:I3 -> BORDER:BORDER_IFACE
     * R2:I1 -> FW:I1 -- FW:I3 -> BORDER:BORDER_IFACE
     */
    Prefix routePrefix1 = Prefix.parse("1.0.0.0/8");
    Prefix routePrefix2 = Prefix.parse("2.0.0.0/8");
    Prefix routePrefix3 = Prefix.parse("3.0.0.0/8");
    BDD fwdRouteBdd1 = dstBdd(routePrefix1);
    BDD fwdRouteBdd2 = dstBdd(routePrefix2);
    BDD fwdRouteBdd3 = dstBdd(routePrefix3);

    BDD r1I1Flows =
        _lastHopMgr
            .getLastHopOutgoingInterfaceBdd(R1, R1I1, FW, FWI1)
            .and(fwdRouteBdd1.or(fwdRouteBdd2));
    BDD r2I1Flows =
        _lastHopMgr
            .getLastHopOutgoingInterfaceBdd(R2, R2I1, FW, FWI1)
            .and(fwdRouteBdd2.or(fwdRouteBdd3));

    // R1:I1 -> FW:I1  or  R1:I2 -> FW:I1
    BDD inBdd = _fwSrcMgr.getSourceInterfaceBDD(FWI1).and(r1I1Flows.or(r2I1Flows));

    // Somewhere between entering and leaving the device, the traffic is constrained to TCP
    BDD tcp =
        TO_BDD.toBDD(
            HeaderSpace.builder().setIpProtocols(ImmutableList.of(IpProtocol.TCP)).build());

    BDD outBdd = inBdd.and(tcp);

    BDD r1I1SessionFlows = tcp.and(srcBdd(routePrefix1).or(srcBdd(routePrefix2)));
    BDD r1I2SessionFlows = tcp.and(srcBdd(routePrefix2).or(srcBdd(routePrefix3)));

    Map<StateExpr, BDD> forwardReachableSets =
        ImmutableMap.of(
            new PreInInterface(FW, FWI1), inBdd,
            new PreOutEdgePostNat(FW, FWI3, BORDER, BORDER_IFACE), outBdd);

    Map<String, List<BDDFirewallSessionTraceInfo>> sessions =
        computeInitializedSessions(forwardReachableSets);

    assertThat(sessions.keySet(), contains(FW));
    List<BDDFirewallSessionTraceInfo> fwSessions = sessions.get(FW);

    assertThat(
        fwSessions,
        containsInAnyOrder(
            allOf(
                // R1:I1 -> FW:I1
                hasHostname(FW),
                hasIncomingInterfaces(contains(FWI3)),
                hasNextHop(new NodeInterfacePair(R1, R1I1)),
                hasOutgoingInterface(FWI1),
                hasSessionFlows(r1I1SessionFlows),
                hasTransformation(_fwI3ToI1Transition)),
            allOf(
                // R2:I1 -> FW:I1
                hasHostname(FW),
                hasIncomingInterfaces(contains(FWI3)),
                hasNextHop(new NodeInterfacePair(R2, R2I1)),
                hasOutgoingInterface(FWI1),
                hasSessionFlows(r1I2SessionFlows),
                hasTransformation(_fwI3ToI1Transition))));
  }

  @Test
  public void testDifferentSourceInterfaces() {
    /* two paths, difference source interfaces
     * R1:I1 -> FW:I1 -- FW:I3 -> BORDER:BORDER_IFACE
     * R3:I1 -> FW:I2 -- FW:I3 -> BORDER:BORDER_IFACE
     */
    Prefix routePrefix1 = Prefix.parse("1.0.0.0/8");
    Prefix routePrefix2 = Prefix.parse("2.0.0.0/8");
    Prefix routePrefix3 = Prefix.parse("3.0.0.0/8");
    BDD fwdRouteBdd1 = dstBdd(routePrefix1);
    BDD fwdRouteBdd2 = dstBdd(routePrefix2);
    BDD fwdRouteBdd3 = dstBdd(routePrefix3);

    BDD r1I1Flows =
        _lastHopMgr
            .getLastHopOutgoingInterfaceBdd(R1, R1I1, FW, FWI1)
            .and(fwdRouteBdd1.or(fwdRouteBdd2));
    BDD r3I1Flows =
        _lastHopMgr
            .getLastHopOutgoingInterfaceBdd(R3, R3I1, FW, FWI2)
            .and(fwdRouteBdd2.or(fwdRouteBdd3));

    // R1:I1 -> FW:I1
    BDD inBdd1 = _fwSrcMgr.getSourceInterfaceBDD(FWI1).and(r1I1Flows);

    // R3:I1 -> FW:I2
    BDD inBdd2 = _fwSrcMgr.getSourceInterfaceBDD(FWI2).and(r3I1Flows);

    // Somewhere between entering and leaving the device, the traffic is constrained to TCP
    BDD tcp =
        TO_BDD.toBDD(
            HeaderSpace.builder().setIpProtocols(ImmutableList.of(IpProtocol.TCP)).build());
    BDD outBdd = tcp.and(inBdd1.or(inBdd2));

    BDD r1I1SessionFlows = tcp.and(srcBdd(routePrefix1).or(srcBdd(routePrefix2)));
    BDD r3I1SessionFlows = tcp.and(srcBdd(routePrefix2).or(srcBdd(routePrefix3)));

    Map<StateExpr, BDD> forwardReachableSets =
        ImmutableMap.of(
            new PreInInterface(FW, FWI1), inBdd1,
            new PreInInterface(FW, FWI2), inBdd2,
            new PreOutEdgePostNat(FW, FWI3, BORDER, BORDER_IFACE), outBdd);

    Map<String, List<BDDFirewallSessionTraceInfo>> sessions =
        computeInitializedSessions(forwardReachableSets);

    assertThat(sessions.keySet(), contains(FW));
    List<BDDFirewallSessionTraceInfo> fwSessions = sessions.get(FW);

    assertThat(
        fwSessions,
        containsInAnyOrder(
            allOf(
                // R1:I1 -> FW:I1
                hasHostname(FW),
                hasIncomingInterfaces(contains(FWI3)),
                hasNextHop(new NodeInterfacePair(R1, R1I1)),
                hasOutgoingInterface(FWI1),
                hasSessionFlows(r1I1SessionFlows),
                hasTransformation(_fwI3ToI1Transition)),
            allOf(
                // R3:I1 -> FW:I2
                hasHostname(FW),
                hasIncomingInterfaces(contains(FWI3)),
                hasNextHop(new NodeInterfacePair(R3, R3I1)),
                hasOutgoingInterface(FWI2),
                hasSessionFlows(r3I1SessionFlows),
                hasTransformation(_fwI3ToI2Transition))));
  }

  @Test
  public void testOriginatingFromDevice() {
    throw new BatfishException("TODO");
  }

  @Test
  public void testNoLastHopOutgoingInterface() {
    throw new BatfishException("TODO");
  }
}

package org.batfish.z3;

import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.datamodel.transformation.Transformation.when;
import static org.batfish.question.SrcNattedConstraint.REQUIRE_NOT_SRC_NATTED;
import static org.batfish.question.SrcNattedConstraint.REQUIRE_SRC_NATTED;
import static org.batfish.question.SrcNattedConstraint.UNCONSTRAINED;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.plugin.TracerouteEngine;
import org.batfish.config.Settings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.FlowTraceHop;
import org.batfish.datamodel.ForwardingAnalysisImpl;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.transformation.TransformationStep;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.question.SrcNattedConstraint;
import org.batfish.z3.expr.BooleanExpr;
import org.batfish.z3.expr.TrueExpr;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NodJobTest {

  private TracerouteEngine _tracerouteEngine;
  private SortedMap<String, Configuration> _configs;
  private DataPlane _dataPlane;
  private Configuration _dstNode;
  private IngressLocation _ingressLocation;
  private Configuration _srcNode;
  private Vrf _srcVrf;
  private Synthesizer _synthesizer;

  public static Status checkSat(NodJob nodJob) {
    Context z3Context = new Context();
    SmtInput smtInput = nodJob.computeSmtInput(System.currentTimeMillis(), z3Context);
    Solver solver = z3Context.mkSolver();
    solver.add(smtInput._expr);
    return solver.check();
  }

  private NodJob getNodJob(HeaderSpace headerSpace) {
    return getNodJob(headerSpace, UNCONSTRAINED);
  }

  private NodJob getNodJob(HeaderSpace headerSpace, SrcNattedConstraint srcNatted) {
    IngressLocation ingressLocation =
        IngressLocation.vrf(_srcNode.getHostname(), _srcVrf.getName());
    Map<IngressLocation, BooleanExpr> srcIpConstraints =
        ImmutableMap.of(ingressLocation, TrueExpr.INSTANCE);
    StandardReachabilityQuerySynthesizer querySynthesizer =
        StandardReachabilityQuerySynthesizer.builder()
            .setActions(ImmutableSet.of(FlowDisposition.ACCEPTED))
            .setFinalNodes(ImmutableSet.of(_dstNode.getHostname()))
            .setHeaderSpace(AclLineMatchExprs.match(headerSpace))
            .setSrcIpConstraints(srcIpConstraints)
            .setSrcNatted(srcNatted)
            .setRequiredTransitNodes(ImmutableSet.of())
            .setForbiddenTransitNodes(ImmutableSet.of())
            .build();
    return new NodJob(
        new Settings(), _synthesizer, querySynthesizer, srcIpConstraints, "tag", false);
  }

  @Before
  public void setup() throws IOException {
    setupConfigs();
    setupDataPlane();
    setupSynthesizer();
  }

  private void setupConfigs() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Interface.Builder ib = nf.interfaceBuilder().setBandwidth(1E9d);
    IpAccessList.Builder aclb = nf.aclBuilder();
    Vrf.Builder vb = nf.vrfBuilder();

    _srcNode = cb.build();
    _dstNode = cb.build();
    _srcVrf = vb.setOwner(_srcNode).build();
    _ingressLocation = IngressLocation.vrf(_srcNode.getHostname(), _srcVrf.getName());
    Vrf dstVrf = vb.setOwner(_dstNode).build();
    Prefix p1 = Prefix.parse("1.0.0.0/31");
    Ip poolIp1 = Ip.parse("1.0.0.10");

    // apply NAT to all packets
    IpAccessList sourceNat1Acl =
        aclb.setLines(
                ImmutableList.of(
                    IpAccessListLine.acceptingHeaderSpace(
                        HeaderSpace.builder()
                            .setSrcIps(ImmutableList.of(new IpWildcard("3.0.0.0/32")))
                            .build())))
            .setOwner(_srcNode)
            .build();

    ib.setOwner(_srcNode)
        .setVrf(_srcVrf)
        .setAddress(new InterfaceAddress(p1.getStartIp(), p1.getPrefixLength()))
        .setOutgoingTransformation(
            when(permittedByAcl(sourceNat1Acl.getName()))
                .apply(TransformationStep.assignSourceIp(poolIp1, poolIp1))
                .build())
        .build();
    ib.setOwner(_dstNode)
        .setVrf(dstVrf)
        .setAddress(new InterfaceAddress(p1.getEndIp(), p1.getPrefixLength()))
        .setOutgoingTransformation(null)
        .build();

    // For the destination
    Prefix pDest = Prefix.parse("2.0.0.0/32");
    ib.setOwner(_dstNode)
        .setVrf(dstVrf)
        .setAddress(new InterfaceAddress(pDest.getEndIp(), pDest.getPrefixLength()))
        .build();

    StaticRoute.Builder bld = StaticRoute.builder().setNetwork(pDest).setAdministrativeCost(1);
    _srcVrf.getStaticRoutes().add(bld.setNextHopIp(p1.getEndIp()).build());

    _configs =
        ImmutableSortedMap.of(
            _srcNode.getHostname(), _srcNode,
            _dstNode.getHostname(), _dstNode);
  }

  private void setupDataPlane() throws IOException {
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    Batfish batfish = BatfishTestUtils.getBatfish(_configs, tmp);
    batfish.computeDataPlane(false);
    _tracerouteEngine = batfish.getTracerouteEngine();
    _dataPlane = batfish.loadDataPlane();
  }

  private void setupSynthesizer() {
    Topology topology = new Topology(_dataPlane.getTopologyEdges());
    SynthesizerInput input =
        SynthesizerInputImpl.builder()
            .setConfigurations(_configs)
            .setForwardingAnalysis(
                new ForwardingAnalysisImpl(
                    _configs, _dataPlane.getRibs(), _dataPlane.getFibs(), topology))
            .setSimplify(false)
            .setTopology(topology)
            .build();

    _synthesizer = new Synthesizer(input);
  }

  /** Test that traffic originating from 3.0.0.0 is NATed */
  @Test
  public void testNatted() {
    HeaderSpace headerSpace = new HeaderSpace();
    headerSpace.setSrcIps(ImmutableList.of(new IpWildcard("3.0.0.0")));
    NodJob nodJob = getNodJob(headerSpace);

    Context z3Context = new Context();
    SmtInput smtInput = nodJob.computeSmtInput(System.currentTimeMillis(), z3Context);

    Map<IngressLocation, Map<String, Long>> ingressLocationConstraints =
        nodJob.getSolutionPerIngressLocation(z3Context, smtInput);
    assertThat(ingressLocationConstraints.entrySet(), hasSize(1));
    assertThat(ingressLocationConstraints, hasKey(_ingressLocation));
    Map<String, Long> fieldConstraints = ingressLocationConstraints.get(_ingressLocation);

    // Only one OriginateVrf choice, so this must be 0
    assertThat(
        fieldConstraints, hasEntry(IngressLocationInstrumentation.INGRESS_LOCATION_FIELD_NAME, 0L));
    assertThat(
        fieldConstraints, hasEntry(Field.ORIG_SRC_IP.getName(), Ip.parse("3.0.0.0").asLong()));
    assertThat(
        fieldConstraints,
        hasEntry(equalTo(Field.SRC_IP.getName()), not(equalTo(Ip.parse("3.0.0.0").asLong()))));
    assertThat(fieldConstraints, hasEntry(Field.SRC_IP.getName(), Ip.parse("1.0.0.10").asLong()));

    Set<Flow> flows = nodJob.getFlows(ingressLocationConstraints);

    List<FlowTrace> flowTraces = getFlowTraces(flows);

    flowTraces.forEach(
        trace -> {
          assertThat(trace.getNotes(), is("ACCEPTED"));
          List<FlowTraceHop> hops = trace.getHops();
          assertThat(hops, hasSize(1));
          FlowTraceHop hop = hops.get(0);
          assertThat(hop.getTransformedFlow(), notNullValue());
          assertThat(hop.getTransformedFlow().getSrcIp(), equalTo(Ip.parse("1.0.0.10")));
        });
  }

  private List<FlowTrace> getFlowTraces(Set<Flow> flows) {
    return _tracerouteEngine.processFlows(flows, false).values().stream()
        .flatMap(Set::stream)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Test that traffic originating from 3.0.0.0 that is expected to be NATed returns SAT when we
   * constrain to only allow NATed results.
   */
  @Test
  public void testNattedSat() {
    HeaderSpace headerSpace = new HeaderSpace();
    headerSpace.setSrcIps(ImmutableList.of(new IpWildcard("3.0.0.0")));
    NodJob nodJob = getNodJob(headerSpace, REQUIRE_SRC_NATTED);
    assertThat(checkSat(nodJob), equalTo(Status.SATISFIABLE));
  }

  /**
   * Test that traffic originating from 3.0.0.0 that is expected to be NATed returns UNSAT when we
   * constrain to only allow NOT-NATed results.
   */
  @Test
  public void testNattedUnsat() {
    HeaderSpace headerSpace = new HeaderSpace();
    headerSpace.setSrcIps(ImmutableList.of(new IpWildcard("3.0.0.0")));
    NodJob nodJob = getNodJob(headerSpace, REQUIRE_NOT_SRC_NATTED);
    assertThat(checkSat(nodJob), equalTo(Status.UNSATISFIABLE));
  }

  /** Test that traffic originating from 3.0.0.1 is not NATed */
  @Test
  public void testNotNatted() {
    HeaderSpace headerSpace = new HeaderSpace();
    headerSpace.setSrcIps(ImmutableList.of(new IpWildcard("3.0.0.1")));
    NodJob nodJob = getNodJob(headerSpace);

    Context z3Context = new Context();
    SmtInput smtInput = nodJob.computeSmtInput(System.currentTimeMillis(), z3Context);

    Map<IngressLocation, Map<String, Long>> ingressLocationConstraints =
        nodJob.getSolutionPerIngressLocation(z3Context, smtInput);
    assertThat(ingressLocationConstraints.entrySet(), hasSize(1));
    assertThat(ingressLocationConstraints, hasKey(_ingressLocation));
    Map<String, Long> fieldConstraints = ingressLocationConstraints.get(_ingressLocation);

    assertThat(
        fieldConstraints, hasEntry(IngressLocationInstrumentation.INGRESS_LOCATION_FIELD_NAME, 0L));
    assertThat(smtInput._variablesAsConsts, hasKey("SRC_IP"));
    assertThat(fieldConstraints, hasKey(Field.SRC_IP.getName()));

    assertThat(
        fieldConstraints, hasEntry(Field.ORIG_SRC_IP.getName(), Ip.parse("3.0.0.1").asLong()));
    assertThat(fieldConstraints, hasEntry(Field.SRC_IP.getName(), Ip.parse("3.0.0.1").asLong()));

    Set<Flow> flows = nodJob.getFlows(ingressLocationConstraints);
    List<FlowTrace> flowTraces = getFlowTraces(flows);

    flowTraces.forEach(
        trace -> {
          assertThat(trace.getNotes(), is("ACCEPTED"));
          List<FlowTraceHop> hops = trace.getHops();
          assertThat(hops, hasSize(1));
          FlowTraceHop hop = hops.get(0);
          assertThat(hop.getTransformedFlow(), nullValue());
        });
  }

  /**
   * Test that traffic originating from 3.0.0.1 that is expected NOT to be NATed returns SAT when we
   * constrain to only allow NOT-NATed results.
   */
  @Test
  public void testNotNattedSat() {
    HeaderSpace headerSpace = new HeaderSpace();
    headerSpace.setSrcIps(Ip.parse("3.0.0.1").toIpSpace());
    NodJob nodJob = getNodJob(headerSpace, REQUIRE_NOT_SRC_NATTED);
    assertThat(checkSat(nodJob), equalTo(Status.SATISFIABLE));
  }

  /**
   * Test that traffic originating from 3.0.0.1 that is expected NOT to be NATed returns UNSAT when
   * we constrain to only allow NATed results.
   */
  @Test
  public void testNotNattedUnsat() {
    HeaderSpace headerSpace = new HeaderSpace();
    headerSpace.setSrcIps(Ip.parse("3.0.0.1").toIpSpace());
    NodJob nodJob = getNodJob(headerSpace, REQUIRE_SRC_NATTED);
    assertThat(checkSat(nodJob), equalTo(Status.UNSATISFIABLE));
  }
}

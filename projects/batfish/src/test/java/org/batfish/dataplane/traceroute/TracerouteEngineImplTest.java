package org.batfish.dataplane.traceroute;

import static java.util.Collections.singletonList;
import static org.batfish.datamodel.FlowDisposition.ACCEPTED;
import static org.batfish.datamodel.FlowDisposition.NO_ROUTE;
import static org.batfish.datamodel.matchers.TraceMatchers.hasDisposition;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.matchers.TraceMatchers;
import org.batfish.dataplane.TracerouteEngineImpl;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link TracerouteEngineImpl} */
public class TracerouteEngineImplTest {
  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Rule public TemporaryFolder _tempFolder = new TemporaryFolder();

  private static Flow makeFlow() {
    Flow.Builder builder = new Flow.Builder();
    builder.setSrcIp(new Ip("1.2.3.4"));
    builder.setIngressNode("foo");
    builder.setTag("TEST");
    return builder.build();
  }

  private static IpAccessList makeAcl(String name, LineAction action) {
    IpAccessListLine aclLine =
        IpAccessListLine.builder().setAction(action).setMatchCondition(TrueExpr.INSTANCE).build();
    return IpAccessList.builder().setName(name).setLines(singletonList(aclLine)).build();
  }

  @Test
  public void testApplySourceNatSingleAclMatch() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("accept", LineAction.PERMIT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    Flow transformed =
        TracerouteEngineImplContext.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplySourceNatSingleAclNoMatch() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("reject", LineAction.DENY));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    Flow transformed =
        TracerouteEngineImplContext.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed, is(flow));
  }

  @Test
  public void testApplySourceNatFirstMatchWins() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("firstAccept", LineAction.PERMIT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    SourceNat secondNat = new SourceNat();
    secondNat.setAcl(makeAcl("secondAccept", LineAction.PERMIT));
    secondNat.setPoolIpFirst(new Ip("4.5.6.8"));

    Flow transformed =
        TracerouteEngineImplContext.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplySourceNatLateMatchWins() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("rejectAll", LineAction.DENY));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    SourceNat secondNat = new SourceNat();
    secondNat.setAcl(makeAcl("acceptAnyway", LineAction.PERMIT));
    secondNat.setPoolIpFirst(new Ip("4.5.6.8"));

    Flow transformed =
        TracerouteEngineImplContext.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.8")));
  }

  @Test
  public void testApplySourceNatInvalidAclThrows() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("matchAll", LineAction.PERMIT));

    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("missing NAT address or pool");
    TracerouteEngineImplContext.applySourceNat(
        flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
  }

  /*
   * iface1 and iface2 are interfaces on the same node. Send traffic with dstIp=iface2's ip to
   * iface1. Should accept if and only if iface1 and iface2 are in the same VRF.
   */
  @Test
  public void testAcceptVrf() throws IOException {
    // Construct network
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration config = cb.build();
    Vrf.Builder vb = nf.vrfBuilder().setOwner(config);
    Interface.Builder ib = nf.interfaceBuilder().setOwner(config);

    Vrf vrf1 = vb.build();
    Vrf vrf2 = vb.build();

    Interface i1 = ib.setVrf(vrf1).setAddress(new InterfaceAddress("1.1.1.1/24")).build();
    Interface i2 = ib.setVrf(vrf2).setAddress(new InterfaceAddress("2.2.2.2/24")).build();
    ib.setVrf(vrf2).setAddress(new InterfaceAddress("3.3.3.3/24")).build();

    // Compute data plane
    SortedMap<String, Configuration> configs = ImmutableSortedMap.of(config.getHostname(), config);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _tempFolder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();

    // Construct flows
    Flow.Builder fb =
        Flow.builder()
            .setDstIp(new Ip("3.3.3.3"))
            .setIngressNode(config.getHostname())
            .setTag("TAG");

    Flow flow1 = fb.setIngressInterface(i1.getName()).setIngressVrf(vrf1.getName()).build();
    Flow flow2 = fb.setIngressInterface(i2.getName()).setIngressVrf(vrf2.getName()).build();

    // Compute flow traces
    SortedMap<Flow, List<Trace>> traces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow1, flow2), dp.getFibs(), false);

    assertThat(traces, hasEntry(equalTo(flow1), contains(hasDisposition(NO_ROUTE))));
    assertThat(traces, hasEntry(equalTo(flow2), contains(hasDisposition(ACCEPTED))));
  }

  @Test
  public void testArpMultipleAccess() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Interface.Builder ib = nf.interfaceBuilder().setProxyArp(true);

    Configuration source = cb.build();
    Vrf vSource = vb.setOwner(source).build();
    ib.setOwner(source).setVrf(vSource).setAddress(new InterfaceAddress("10.0.0.1/24")).build();

    Configuration dst = cb.build();
    Vrf vDst = vb.setOwner(dst).build();
    ib.setOwner(dst).setVrf(vDst).setAddress(new InterfaceAddress("10.0.0.2/24")).build();

    Configuration other = cb.build();
    Vrf vOther = vb.setOwner(other).build();
    ib.setOwner(other).setVrf(vOther).setAddress(new InterfaceAddress("10.0.0.3/24")).build();

    SortedMap<String, Configuration> configurations =
        ImmutableSortedMap.<String, Configuration>naturalOrder()
            .put(source.getHostname(), source)
            .put(dst.getHostname(), dst)
            .put(other.getHostname(), other)
            .build();
    Batfish batfish = BatfishTestUtils.getBatfish(configurations, _tempFolder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();
    Flow flow =
        Flow.builder()
            .setIngressNode(source.getHostname())
            .setSrcIp(new Ip("10.0.0.1"))
            .setDstIp(new Ip("10.0.0.2"))
            .setTag("tag")
            .build();
    List<Trace> traces =
        TracerouteEngineImpl.getInstance()
            .buildFlows(dp, ImmutableSet.of(flow), dp.getFibs(), false)
            .get(flow);

    /*
     *  Since the 'other' neighbor should not respond to ARP:
     *  - There should only be one trace, ending at 'dst'.
     *  - It should be accepting.
     */
    assertThat(traces, Matchers.contains(TraceMatchers.hasDisposition(ACCEPTED)));
  }

  @Test
  public void testAclBeforeArpNoEdge() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c = cb.build();
    Vrf v = nf.vrfBuilder().setOwner(c).setName(Configuration.DEFAULT_VRF_NAME).build();
    // denies everything
    IpAccessList outgoingFilter =
        nf.aclBuilder().setOwner(c).setName("outgoingAcl").setLines(ImmutableList.of()).build();
    nf.interfaceBuilder()
        .setOwner(c)
        .setVrf(v)
        .setAddress(new InterfaceAddress("1.0.0.0/24"))
        .setOutgoingFilter(outgoingFilter)
        .build();
    SortedMap<String, Configuration> configurations = ImmutableSortedMap.of(c.getHostname(), c);
    Batfish b = BatfishTestUtils.getBatfish(configurations, _tempFolder);
    // make batfish call the new traceroute engine
    b.getSettings().setDebugFlags(ImmutableList.of("traceroute"));

    b.computeDataPlane(false);
    Flow flow =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setDstIp(new Ip("1.0.0.1"))
            .setTag("tag")
            .build();
    SortedMap<Flow, List<Trace>> flowTraces = b.buildFlows(ImmutableSet.of(flow), false);
    Trace trace = flowTraces.get(flow).iterator().next();

    /* Flow should be blocked by ACL before ARP, which would otherwise result in unreachable neighbor */
    assertThat(trace.getDisposition(), equalTo(FlowDisposition.DENIED_OUT));
  }

  @Test
  public void testAclBeforeArpWithEdge() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Vrf.Builder vb = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    Interface.Builder ib = nf.interfaceBuilder();

    // c1
    Configuration c1 = cb.build();
    Vrf v1 = vb.setOwner(c1).build();
    // denies everything
    IpAccessList outgoingFilter =
        nf.aclBuilder().setOwner(c1).setName("outgoingAcl").setLines(ImmutableList.of()).build();
    ib.setOwner(c1)
        .setVrf(v1)
        .setOutgoingFilter(outgoingFilter)
        .setAddress(new InterfaceAddress("1.0.0.0/24"))
        .build();

    // c2
    Configuration c2 = cb.build();
    Vrf v2 = vb.setOwner(c2).build();
    ib.setOwner(c2)
        .setVrf(v2)
        .setOutgoingFilter(null)
        .setAddress(new InterfaceAddress("1.0.0.3/24"))
        .build();

    SortedMap<String, Configuration> configurations =
        ImmutableSortedMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    Batfish b = BatfishTestUtils.getBatfish(configurations, _tempFolder);

    // make batfish call the new traceroute engine
    b.getSettings().setDebugFlags(ImmutableList.of("traceroute"));
    b.computeDataPlane(false);
    Flow flow =
        Flow.builder()
            .setIngressNode(c1.getHostname())
            .setTag("tag")
            .setDstIp(new Ip("1.0.0.1"))
            .build();
    SortedMap<Flow, List<Trace>> flowTraces = b.buildFlows(ImmutableSet.of(flow), false);
    Trace trace = flowTraces.get(flow).iterator().next();

    /* Flow should be blocked by ACL before ARP, which would otherwise result in unreachable neighbor */
    assertThat(trace.getDisposition(), equalTo(FlowDisposition.DENIED_OUT));
  }

  /** When ingress node is non-existent, don't crash with null-pointer. */
  @Test(expected = IllegalArgumentException.class)
  public void testTracerouteOutsideNetwork() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration c1 = cb.build();
    Batfish batfish =
        BatfishTestUtils.getBatfish(ImmutableSortedMap.of(c1.getHostname(), c1), _tempFolder);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();
    TracerouteEngineImpl.getInstance()
        .buildFlows(
            dp,
            ImmutableSet.of(Flow.builder().setTag("tag").setIngressNode("missingNode").build()),
            dp.getFibs(),
            false);
  }
}

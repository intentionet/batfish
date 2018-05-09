package org.batfish.dataplane;

import static java.util.Collections.singletonList;
import static org.batfish.datamodel.FlowDisposition.ACCEPTED;
import static org.batfish.datamodel.FlowDisposition.NO_ROUTE;
import static org.batfish.datamodel.matchers.FlowTraceMatchers.hasDisposition;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowTrace;
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
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link TracerouteEngineImpl} */
public class TracerouteEngineTest {
  @Rule public ExpectedException _thrown = ExpectedException.none();

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
    return new IpAccessList(name, singletonList(aclLine));
  }

  @Test
  public void testApplySourceNatSingleAclMatch() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("accept", LineAction.ACCEPT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    Flow transformed =
        TracerouteEngineImpl.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplySourceNatSingleAclNoMatch() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("reject", LineAction.REJECT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    Flow transformed =
        TracerouteEngineImpl.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), singletonList(nat));
    assertThat(transformed, is(flow));
  }

  @Test
  public void testApplySourceNatFirstMatchWins() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("firstAccept", LineAction.ACCEPT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    SourceNat secondNat = new SourceNat();
    secondNat.setAcl(makeAcl("secondAccept", LineAction.ACCEPT));
    secondNat.setPoolIpFirst(new Ip("4.5.6.8"));

    Flow transformed =
        TracerouteEngineImpl.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.7")));
  }

  @Test
  public void testApplySourceNatLateMatchWins() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("rejectAll", LineAction.REJECT));
    nat.setPoolIpFirst(new Ip("4.5.6.7"));

    SourceNat secondNat = new SourceNat();
    secondNat.setAcl(makeAcl("acceptAnyway", LineAction.ACCEPT));
    secondNat.setPoolIpFirst(new Ip("4.5.6.8"));

    Flow transformed =
        TracerouteEngineImpl.applySourceNat(
            flow, null, ImmutableMap.of(), ImmutableMap.of(), Lists.newArrayList(nat, secondNat));
    assertThat(transformed.getSrcIp(), equalTo(new Ip("4.5.6.8")));
  }

  @Test
  public void testApplySourceNatInvalidAclThrows() {
    Flow flow = makeFlow();

    SourceNat nat = new SourceNat();
    nat.setAcl(makeAcl("matchAll", LineAction.ACCEPT));

    _thrown.expect(BatfishException.class);
    _thrown.expectMessage("missing NAT address or pool");
    TracerouteEngineImpl.applySourceNat(
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
        new Configuration.Builder(nf).setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration config = cb.build();
    Vrf.Builder vb = new Vrf.Builder(nf).setOwner(config);
    Interface.Builder ib = new Interface.Builder(nf).setActive(true).setOwner(config);

    Vrf vrf1 = vb.build();
    Vrf vrf2 = vb.build();

    Interface i1 = ib.setVrf(vrf1).setAddress(new InterfaceAddress("1.1.1.1/24")).build();
    Interface i2 = ib.setVrf(vrf2).setAddress(new InterfaceAddress("2.2.2.2/24")).build();
    ib.setVrf(vrf2).setAddress(new InterfaceAddress("3.3.3.3/24")).build();

    // Compute data plane
    SortedMap<String, Configuration> configs = ImmutableSortedMap.of(config.getHostname(), config);
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, tmp);
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
    SortedMap<Flow, Set<FlowTrace>> flowTraces =
        TracerouteEngineImpl.getInstance()
            .processFlows(dp, ImmutableSet.of(flow1, flow2), dp.getFibs(), false);

    assertThat(flowTraces, hasEntry(equalTo(flow1), contains(hasDisposition(NO_ROUTE))));
    assertThat(flowTraces, hasEntry(equalTo(flow2), contains(hasDisposition(ACCEPTED))));
  }

  /*
   * iface1 and iface2 are on the same node. Their IPs are on different networks, and iface1
   * has proxy-arp on. We send an ARP request to iface1 for iface2's IP. iface1 should reply
   * if and only if iface1 and iface2 are in the same VRF.
   */
  @Test
  public void testArpVrf() throws IOException {
    // Construct network
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        new Configuration.Builder(nf).setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration config = cb.build();
    Vrf.Builder vb = new Vrf.Builder(nf).setOwner(config);
    Interface.Builder ib = new Interface.Builder(nf).setActive(true).setOwner(config);

    Vrf vrf1 = vb.build();
    Vrf vrf2 = vb.build();

    Interface i1 =
        ib.setVrf(vrf1).setAddress(new InterfaceAddress("1.1.1.1/24")).setProxyArp(true).build();
    Interface i2 =
        ib.setVrf(vrf2).setAddress(new InterfaceAddress("2.2.2.2/24")).setProxyArp(true).build();
    Interface i3 =
        ib.setVrf(vrf2).setAddress(new InterfaceAddress("3.3.3.3/24")).setProxyArp(false).build();
    Interface i4 =
        ib.setVrf(vrf2).setAddress(new InterfaceAddress("4.4.4.4/24")).setProxyArp(false).build();

    // Compute data plane
    SortedMap<String, Configuration> configs = ImmutableSortedMap.of(config.getHostname(), config);
    TemporaryFolder tmp = new TemporaryFolder();
    tmp.create();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, tmp);
    batfish.computeDataPlane(false);
    DataPlane dp = batfish.loadDataPlane();

    Ip arpIp = new Ip("4.4.4.4");
    assertFalse(
        "ARP request to interface on a different VRF should fail",
        TracerouteEngineImpl.interfaceRepliesToArpRequestForIp(dp, i1, arpIp));
    assertTrue(
        "ARP request to interface with proxy-arp enabled should succeed",
        TracerouteEngineImpl.interfaceRepliesToArpRequestForIp(dp, i2, arpIp));
    assertFalse(
        "ARP request to interface with proxy-arp disabled should fail",
        TracerouteEngineImpl.interfaceRepliesToArpRequestForIp(dp, i3, arpIp));
    assertTrue(
        "ARP request to interface that owns arpIp should succeed",
        TracerouteEngineImpl.interfaceRepliesToArpRequestForIp(dp, i4, arpIp));

    // now arpIp isn't owned by the VRF, but is routable
    arpIp = new Ip("4.4.4.0");
    assertFalse(
        "ARP request for interface subnet to interface on a different VRF should fail",
        TracerouteEngineImpl.interfaceRepliesToArpRequestForIp(dp, i1, arpIp));
    assertTrue(
        "ARP request for interface subnet to interface with proxy-arp enabled should succeed",
        TracerouteEngineImpl.interfaceRepliesToArpRequestForIp(dp, i2, arpIp));
    assertFalse(
        "ARP request for interface subnet to interface with proxy-arp disabled should fail",
        TracerouteEngineImpl.interfaceRepliesToArpRequestForIp(dp, i3, arpIp));
    assertFalse(
        "ARP request for interface subnet to the same interface should fail",
        TracerouteEngineImpl.interfaceRepliesToArpRequestForIp(dp, i4, arpIp));
  }
}

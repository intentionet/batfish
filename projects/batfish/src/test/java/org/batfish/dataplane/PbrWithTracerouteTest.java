package org.batfish.dataplane;

import static org.batfish.datamodel.matchers.HopMatchers.hasOutputInterface;
import static org.batfish.datamodel.matchers.TraceMatchers.hasDisposition;
import static org.batfish.datamodel.matchers.TraceMatchers.hasLastHop;
import static org.batfish.main.BatfishTestUtils.getBatfish;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.packet_policy.Drop;
import org.batfish.datamodel.packet_policy.FibLookup;
import org.batfish.datamodel.packet_policy.FibLookupOverrideLookupIp;
import org.batfish.datamodel.packet_policy.IngressInterfaceVrf;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.packet_policy.Return;
import org.batfish.main.Batfish;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** An e2e test of interpretation of {@link PacketPolicy} using traceroute */
public class PbrWithTracerouteTest {
  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  private String _ingressIfaceName = "ingress";
  private String _pbrOutIface = "exitPBR";
  private final int _subnetLength = 24;
  private final Ip _overrideNextHop = Ip.parse("2.2.2.2");

  @Test
  public void testOverrideIpExitsNetwork() throws IOException {

    NetworkFactory nf = new NetworkFactory();
    Configuration c = buildPBRConfig(nf);

    Batfish batfish = getBatfish(ImmutableSortedMap.of(c.getHostname(), c), _folder);
    batfish.computeDataPlane();

    Ip dstIp = Ip.parse("1.1.1.250");
    Flow flow =
        Flow.builder()
            .setTag("test")
            .setIngressNode(c.getHostname())
            .setIngressInterface(_ingressIfaceName)
            .setDstIp(dstIp)
            .setSrcIp(Ip.ZERO)
            .build();
    SortedMap<Flow, List<Trace>> traces =
        batfish.getTracerouteEngine().computeTraces(ImmutableSet.of(flow), false);
    assertThat(
        traces.get(flow),
        contains(
            allOf(
                hasDisposition(FlowDisposition.DELIVERED_TO_SUBNET),
                hasLastHop(
                    hasOutputInterface(NodeInterfacePair.of(c.getHostname(), _pbrOutIface))))));
  }

  @Test
  public void testOverrideIpDelivered() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    Configuration c = buildPBRConfig(nf);

    Configuration acceptor =
        nf.configurationBuilder()
            .setHostname("acceptor")
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();

    Ip dstIp = Ip.parse("1.1.1.250");
    Vrf vrf = nf.vrfBuilder().setOwner(acceptor).build();
    nf.interfaceBuilder()
        .setName("Loopback")
        .setVrf(vrf)
        .setOwner(acceptor)
        .setAddress(ConcreteInterfaceAddress.create(dstIp, Prefix.MAX_PREFIX_LENGTH))
        .build();
    nf.interfaceBuilder()
        .setName("ToPBRbox")
        .setVrf(vrf)
        .setOwner(acceptor)
        .setAddress(ConcreteInterfaceAddress.create(_overrideNextHop, _subnetLength))
        .build();

    Batfish batfish =
        getBatfish(
            ImmutableSortedMap.of(c.getHostname(), c, acceptor.getHostname(), acceptor), _folder);
    batfish.computeDataPlane();

    Flow flow =
        Flow.builder()
            .setTag("test")
            .setIngressNode(c.getHostname())
            .setIngressInterface(_ingressIfaceName)
            .setDstIp(dstIp)
            .setSrcIp(Ip.ZERO)
            .build();
    SortedMap<Flow, List<Trace>> traces =
        batfish.getTracerouteEngine().computeTraces(ImmutableSet.of(flow), false);
    assertThat(traces.get(flow), contains(hasDisposition(FlowDisposition.ACCEPTED)));
  }

  private Configuration buildPBRConfig(NetworkFactory nf) {
    Configuration c =
        nf.configurationBuilder()
            .setHostname("c1")
            .setConfigurationFormat(ConfigurationFormat.CISCO_NX)
            .build();

    Ip ingressIfaceIp = Ip.parse("3.3.3.1");
    Ip regularOutInterfaceIp = Ip.parse("1.1.1.1");
    Ip overrideInterfaceIp = Ip.parse("2.2.2.1");

    String policyName = "PBR";
    c.setPacketPolicies(
        ImmutableSortedMap.of(
            policyName,
            new PacketPolicy(
                policyName,
                ImmutableList.of(
                    new Return(
                        FibLookupOverrideLookupIp.builder()
                            .setIps(ImmutableList.of(_overrideNextHop))
                            .setVrfExpr(IngressInterfaceVrf.instance())
                            .setDefaultAction(new FibLookup(IngressInterfaceVrf.instance()))
                            .setRequireConnected(true)
                            .build())),
                new Return(Drop.instance()))));
    Vrf vrf = nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME).setOwner(c).build();

    nf.interfaceBuilder()
        .setName(_ingressIfaceName)
        .setAddress(ConcreteInterfaceAddress.create(ingressIfaceIp, _subnetLength))
        .setOwner(c)
        .setVrf(vrf)
        .setRoutingPolicy(policyName)
        .build();
    nf.interfaceBuilder()
        .setName(_pbrOutIface)
        .setAddress(ConcreteInterfaceAddress.create(overrideInterfaceIp, _subnetLength))
        .setOwner(c)
        .setVrf(vrf)
        .build();
    nf.interfaceBuilder()
        .setName("exitRegular")
        .setAddress(ConcreteInterfaceAddress.create(regularOutInterfaceIp, _subnetLength))
        .setOwner(c)
        .setVrf(vrf)
        .build();

    return c;
  }
}

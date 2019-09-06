package org.batfish.question.traceroute;

import static org.batfish.common.util.TracePruner.DEFAULT_MAX_TRACES;
import static org.batfish.datamodel.matchers.HopMatchers.hasOutputInterface;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.matchers.TraceMatchers.hasLastHop;
import static org.batfish.question.traceroute.TracerouteAnswerer.COL_TRACES;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.Comparator;
import java.util.SortedMap;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.packet_policy.FibLookup;
import org.batfish.datamodel.packet_policy.If;
import org.batfish.datamodel.packet_policy.LiteralVrfName;
import org.batfish.datamodel.packet_policy.PacketMatchExpr;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.packet_policy.Return;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** End-to-end-ish tests of traceroute with policy-based routing */
public class TraceroutePolicyBasedRoutingTest {

  private static final String ROUTING_POLICY_NAME = "packetPolicy";
  private static final String SOURCE_LOCATION_STR = "enter(c1[ingressInterface])";
  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  /*
   * Build a simple 1-node network with 2 VRFs
   */
  private static SortedMap<String, Configuration> pbrNetwork(boolean withPolicy) {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    ImmutableSortedMap.Builder<String, Configuration> configs =
        new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());

    Configuration c1 = cb.setHostname("c1").build();
    configs.put(c1.getHostname(), c1);

    Vrf v1 = nf.vrfBuilder().setOwner(c1).build();
    Vrf v2 = nf.vrfBuilder().setOwner(c1).setName("otherVRF").build();

    Interface ingressIface =
        nf.interfaceBuilder()
            .setAddress(ConcreteInterfaceAddress.parse("1.1.1.1/31"))
            .setOwner(c1)
            .setVrf(v1)
            .setName("ingressInterface")
            .build();

    if (withPolicy) {
      ingressIface.setRoutingPolicy(ROUTING_POLICY_NAME);
      // IF dst IP is 9.9.9.9 use PBR to do a lookup in V2, which will cause the packet to
      // take a different exit interface
      c1.setPacketPolicies(
          ImmutableSortedMap.of(
              ROUTING_POLICY_NAME,
              new PacketPolicy(
                  ROUTING_POLICY_NAME,
                  ImmutableList.of(
                      new If(
                          new PacketMatchExpr(
                              new MatchHeaderSpace(
                                  HeaderSpace.builder()
                                      .setDstIps(Ip.parse("9.9.9.9").toIpSpace())
                                      .build())),
                          ImmutableList.of(
                              new Return(new FibLookup(new LiteralVrfName(v2.getName())))))),
                  new Return(new FibLookup(new LiteralVrfName(v1.getName()))))));
      ingressIface.setRoutingPolicy(ROUTING_POLICY_NAME);
    }

    Interface i1 =
        nf.interfaceBuilder()
            .setAddress(ConcreteInterfaceAddress.parse("2.2.2.1/24"))
            .setOwner(c1)
            .setVrf(v1)
            .setName("i1")
            .build();
    Interface i2 =
        nf.interfaceBuilder()
            .setAddress(ConcreteInterfaceAddress.parse("3.3.3.1/24"))
            .setOwner(c1)
            .setVrf(v2)
            .setName("i2")
            .build();

    // Static routes. Different next hop interfaces in different VRFs
    final Prefix prefixToMatch = Prefix.parse("9.9.9.0/24");
    v1.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(prefixToMatch)
                .setNextHopInterface(i1.getName())
                .setAdmin(1)
                .build()));
    v2.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(prefixToMatch)
                .setNextHopInterface(i2.getName())
                .setAdmin(1)
                .build()));

    return configs.build();
  }

  @Test
  public void testWithNoPolicy() throws IOException {
    SortedMap<String, Configuration> configs = pbrNetwork(false);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.computeDataPlane();
    PacketHeaderConstraints header =
        PacketHeaderConstraints.builder().setSrcIp("1.1.1.222").setDstIp("9.9.9.9").build();

    TracerouteQuestion question =
        new TracerouteQuestion(SOURCE_LOCATION_STR, header, false, DEFAULT_MAX_TRACES);

    TracerouteAnswerer answerer = new TracerouteAnswerer(question, batfish);
    TableAnswerElement answer = (TableAnswerElement) answerer.answer();
    assertThat(answer.getRows().getData(), hasSize(1));
    assertThat(
        answer.getRows().getData(),
        everyItem(
            hasColumn(
                COL_TRACES,
                everyItem(hasLastHop(hasOutputInterface(NodeInterfacePair.of("c1", "i1")))),
                Schema.set(Schema.TRACE))));
  }

  @Test
  public void testMatchesPolicyIf() throws IOException {
    SortedMap<String, Configuration> configs = pbrNetwork(true);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.computeDataPlane();
    PacketHeaderConstraints header =
        PacketHeaderConstraints.builder().setSrcIp("1.1.1.222").setDstIp("9.9.9.9").build();

    TracerouteQuestion question =
        new TracerouteQuestion(SOURCE_LOCATION_STR, header, false, DEFAULT_MAX_TRACES);

    TracerouteAnswerer answerer = new TracerouteAnswerer(question, batfish);
    TableAnswerElement answer = (TableAnswerElement) answerer.answer();
    assertThat(answer.getRows().getData(), hasSize(1));
    assertThat(
        answer.getRows().getData(),
        everyItem(
            hasColumn(
                COL_TRACES,
                everyItem(hasLastHop(hasOutputInterface(NodeInterfacePair.of("c1", "i2")))),
                Schema.set(Schema.TRACE))));
  }

  @Test
  public void testDoesNotMatchPolicyIf() throws IOException {
    SortedMap<String, Configuration> configs = pbrNetwork(true);
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.computeDataPlane();
    PacketHeaderConstraints header =
        PacketHeaderConstraints.builder().setSrcIp("1.1.1.222").setDstIp("9.9.9.233").build();

    TracerouteQuestion question =
        new TracerouteQuestion(SOURCE_LOCATION_STR, header, false, DEFAULT_MAX_TRACES);

    TracerouteAnswerer answerer = new TracerouteAnswerer(question, batfish);
    TableAnswerElement answer = (TableAnswerElement) answerer.answer();
    assertThat(answer.getRows().getData(), hasSize(1));
    assertThat(
        answer.getRows().getData(),
        everyItem(
            hasColumn(
                COL_TRACES,
                everyItem(hasLastHop(hasOutputInterface(NodeInterfacePair.of("c1", "i1")))),
                Schema.set(Schema.TRACE))));
  }
}

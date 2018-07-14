package org.batfish.question.traceroute;

import static org.batfish.datamodel.matchers.FlowMatchers.hasDstIp;
import static org.batfish.datamodel.matchers.FlowMatchers.hasIngressInterface;
import static org.batfish.datamodel.matchers.FlowMatchers.hasIngressNode;
import static org.batfish.datamodel.matchers.FlowMatchers.hasIngressVrf;
import static org.batfish.datamodel.matchers.FlowMatchers.hasSrcIp;
import static org.batfish.datamodel.matchers.FlowMatchers.hasTag;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.specifier.NameRegexInterfaceLinkLocationSpecifierFactory;
import org.batfish.specifier.NodeNameRegexInterfaceLinkLocationSpecifierFactory;
import org.batfish.specifier.NodeNameRegexInterfaceLocationSpecifierFactory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TracerouteAnswererTest {
  private static final String TAG = "tag";

  private static final String FAST_ETHERNET = "FastEthernet0/0";
  private static final String LOOPBACK = "Loopback0";
  private static final String NODE1 = "node1";
  private static final String NODE2 = "node2";
  private static final Ip NODE1_FAST_ETHERNET_IP = new Ip("1.1.1.2");
  private static final Ip NODE1_LOOPBACK_IP = new Ip("1.1.1.1");
  private static final Ip NODE2_FAST_ETHERNET_IP = new Ip("1.1.1.3");
  private static final Ip NODE2_LOOPBACK_IP = new Ip("2.2.2.2");
  private static final String TESTRIGS_PREFIX = "org/batfish/allinone/testrigs/";
  private static final String TESTRIG_NAME = "specifiers-reachability";
  private static final List<String> TESTRIG_NODE_NAMES = ImmutableList.of(NODE1, NODE2);
  private static final String VRF = "default";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private Batfish _batfish;

  @Before
  public void setup() throws IOException {
    _batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + TESTRIG_NAME, TESTRIG_NODE_NAMES)
                .build(),
            _folder);

    _batfish.computeDataPlane(false);
  }

  @Test
  public void testGetFlows_interfaceSource() {
    TracerouteQuestion question = new TracerouteQuestion();
    question.setSourceLocationSpecifierFactory(NodeNameRegexInterfaceLocationSpecifierFactory.NAME);
    question.setSourceLocationSpecifierInput("node1");

    TracerouteAnswerer answerer = new TracerouteAnswerer(question, _batfish);
    Set<Flow> flows = answerer.getFlows(TAG);

    String ingressInterface = null;
    assertThat(flows, hasSize(2));
    assertThat(
        flows,
        hasItem(
            allOf(
                hasIngressInterface(ingressInterface),
                hasIngressNode("node1"),
                hasIngressVrf(VRF),
                hasSrcIp(NODE1_LOOPBACK_IP),
                hasTag(TAG))));
    assertThat(
        flows,
        hasItem(
            allOf(
                hasIngressInterface(ingressInterface),
                hasIngressNode("node1"),
                hasIngressVrf(VRF),
                hasSrcIp(NODE1_FAST_ETHERNET_IP),
                hasTag(TAG))));
  }

  @Test
  public void testGetFlows_interfaceLinkSource() {
    TracerouteQuestion question = new TracerouteQuestion();
    question.setSourceLocationSpecifierFactory(
        NodeNameRegexInterfaceLinkLocationSpecifierFactory.NAME);
    question.setSourceLocationSpecifierInput("node1");

    TracerouteAnswerer answerer = new TracerouteAnswerer(question, _batfish);
    Set<Flow> flows = answerer.getFlows(TAG);

    // neither interfaces have networks for which link (host) IPs can be inferred
    String ingressVrf = null;
    assertThat(flows, hasSize(2));
    assertThat(
        flows,
        hasItem(
            allOf(
                hasIngressInterface(LOOPBACK),
                hasIngressNode("node1"),
                hasIngressVrf(ingressVrf),
                hasSrcIp(Ip.ZERO),
                hasTag(TAG))));
    assertThat(
        flows,
        hasItem(
            allOf(
                hasIngressInterface(FAST_ETHERNET),
                hasIngressNode("node1"),
                hasIngressVrf(ingressVrf),
                hasSrcIp(Ip.ZERO),
                hasTag(TAG))));
  }

  @Test
  public void testGetFlows_dst() {
    TracerouteQuestion question = new TracerouteQuestion();
    question.setDst("node2");
    question.setSourceLocationSpecifierFactory(NameRegexInterfaceLinkLocationSpecifierFactory.NAME);
    question.setSourceLocationSpecifierInput(LOOPBACK);

    TracerouteAnswerer answerer = new TracerouteAnswerer(question, _batfish);
    Set<Flow> flows = answerer.getFlows(TAG);
    assertThat(
        flows, everyItem(anyOf(hasDstIp(NODE2_FAST_ETHERNET_IP), hasDstIp(NODE2_LOOPBACK_IP))));
  }

  /*
   * Build a simple 1-node network with an ACL.
   */
  private static SortedMap<String, Configuration> aclNetwork() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    ImmutableSortedMap.Builder<String, Configuration> configs =
        new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());

    Configuration c1 = cb.build();
    configs.put(c1.getHostname(), c1);

    Vrf v1 = nf.vrfBuilder().setOwner(c1).build();

    // destination interface
    nf.interfaceBuilder()
        .setActive(true)
        .setAddress(new InterfaceAddress("1.1.1.0/31"))
        .setOwner(c1)
        .setOutgoingFilter(
            nf.aclBuilder()
                .setOwner(c1)
                .setLines(ImmutableList.of(IpAccessListLine.REJECT_ALL))
                .build())
        .setVrf(v1)
        .build();

    return configs.build();
  }

  @Test
  public void testIgnoreAcls() throws IOException {
    SortedMap<String, Configuration> configs = aclNetwork();
    Batfish batfish = BatfishTestUtils.getBatfish(configs, _folder);
    batfish.computeDataPlane(false);

    TracerouteQuestion question = new TracerouteQuestion();
    question.setDst("1.1.1.1");
    question.setSourceLocationSpecifierInput(".*");

    // without ignoreAcls we get DENIED_OUT
    TracerouteAnswerer answerer = new TracerouteAnswerer(question, batfish);
    TableAnswerElement answer = (TableAnswerElement) answerer.answer();
    assertThat(answer.getRows().getData(), hasSize(1));
    assertThat(
        answer.getRows().getData(),
        everyItem(
            hasColumn(
                "results",
                Matchers.equalTo(ImmutableList.of("DENIED_OUT")),
                Schema.list(Schema.STRING))));

    // with ignoreAcls we get NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK
    question.setIgnoreAcls(true);
    answer = (TableAnswerElement) answerer.answer();
    assertThat(answer.getRows().getData(), hasSize(1));
    assertThat(
        answer.getRows().getData(),
        everyItem(
            hasColumn(
                "results",
                Matchers.equalTo(ImmutableList.of("NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK")),
                Schema.list(Schema.STRING))));
  }
}

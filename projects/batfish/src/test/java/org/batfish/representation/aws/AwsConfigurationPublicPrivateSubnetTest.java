package org.batfish.representation.aws;

import static com.google.common.collect.Iterators.getOnlyElement;
import static org.batfish.common.util.IspModelingUtils.INTERNET_HOST_NAME;
import static org.batfish.common.util.IspModelingUtils.getIspNodeName;
import static org.batfish.representation.aws.InternetGateway.AWS_BACKBONE_AS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.IspModelingUtils;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.flow.Trace;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * E2e tests that connectivity to and from the internet works as expected. The configs have a single
 * public subnet. They were pulled after creating this scenario:
 * https://docs.aws.amazon.com/vpc/latest/userguide/VPC_Scenario3.html with static route settings.
 * The wizard did not create the default route to vgw in the main route table. That route was added
 * manually.
 */
public class AwsConfigurationPublicPrivateSubnetTest {

  private static final String TESTCONFIGS_DIR =
      "org/batfish/representation/aws/test-public-private-subnet";

  private static final List<String> fileNames =
      ImmutableList.of(
          "Addresses.json",
          "AvailabilityZones.json",
          "CustomerGateways.json",
          "InternetGateways.json",
          "NatGateways.json",
          "NetworkAcls.json",
          "NetworkInterfaces.json",
          "Reservations.json",
          "RouteTables.json",
          "SecurityGroups.json",
          "Subnets.json",
          "VpcEndpoints.json",
          "Vpcs.json",
          "VpnConnections.json",
          "VpnGateways.json");

  @ClassRule public static TemporaryFolder _folder = new TemporaryFolder();

  private static IBatfish _batfish;

  // various entities in the configs
  private static String _vpc = "vpc-0b966fdeb36d5e43f";
  private static String _igw = "igw-0eac198308206c358";
  private static String _vgw = "vgw-070087240d6fa2989";

  private static String _publicSubnet = "subnet-0cf7a93fbd779f3a3";
  private static String _publicInstance = "i-094a9e5b545a4fde4";
  private static Ip _publicInstancePublicIp = Ip.parse("13.58.55.60");
  private static Ip _publicInstancePrivateIp = Ip.parse("10.0.0.47");

  private static String _privateSubnet = "subnet-0f263105946ad1a1d";
  private static String _privateInstance = "i-099cf38911942421c";
  private static Ip _privateInstancePrivateIp = Ip.parse("10.0.1.204");

  private static Prefix _sitePrefix = Prefix.parse("192.168.0.0/16");

  @BeforeClass
  public static void setup() throws IOException {
    _batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder().setAwsText(TESTCONFIGS_DIR, fileNames).build(), _folder);
    _batfish.computeDataPlane();
  }

  private static void testTrace(
      String ingressNode,
      Ip dstIp,
      FlowDisposition expectedDisposition,
      List<String> expectedNodes) {
    SortedMap<Flow, List<Trace>> traces = getTraces(ingressNode, dstIp);
    Flow flow = getOnlyElement(traces.keySet().iterator());
    Trace flowTrace = getOnlyElement(traces.get(flow).iterator());
    testTrace(flowTrace, expectedDisposition, expectedNodes);
  }

  private static SortedMap<Flow, List<Trace>> getTraces(String ingressNode, Ip dstIp) {
    Flow flow =
        Flow.builder()
            .setTag("test")
            .setIngressNode(ingressNode)
            .setDstIp(dstIp) // this public IP does not exists in the network
            .build();
    return _batfish.getTracerouteEngine().computeTraces(ImmutableSet.of(flow), false);
  }

  private static void testTrace(
      Trace trace, FlowDisposition expectedDisposition, List<String> expectedNodes) {
    assertThat(
        trace.getHops().stream()
            .map(h -> h.getNode().getName())
            .collect(ImmutableList.toImmutableList()),
        equalTo(expectedNodes));
    assertThat(trace.getDisposition(), equalTo(expectedDisposition));
  }

  @Test
  public void testFromPrivateToPublicSubnet() {
    // to the public IP: should end at VGW and not go to the public instance. disposition should be
    // no route since the VGW does not know what to do with the packet
    testTrace(
        _privateInstance,
        _publicInstancePublicIp,
        FlowDisposition.NO_ROUTE,
        ImmutableList.of(_privateInstance, _privateSubnet, _vgw));

    // to the private IP
    testTrace(
        _privateInstance,
        _publicInstancePrivateIp,
        FlowDisposition.DENIED_IN, // by the default security settings
        ImmutableList.of(_privateInstance, _privateSubnet, _vpc, _publicSubnet, _publicInstance));
  }

  @Test
  public void testFromPublicToPrivateSubnet() {
    testTrace(
        _publicInstance,
        _privateInstancePrivateIp,
        FlowDisposition.DENIED_IN, // by the default security settings
        ImmutableList.of(_publicInstance, _publicSubnet, _vpc, _privateSubnet, _privateInstance));
  }

  @Test
  public void testFromInternet() {
    testTrace(
        IspModelingUtils.INTERNET_HOST_NAME,
        _publicInstancePublicIp,
        FlowDisposition.DENIED_IN, // be the default security setting
        ImmutableList.of(
            IspModelingUtils.INTERNET_HOST_NAME,
            getIspNodeName(AWS_BACKBONE_AS),
            _igw,
            _publicSubnet,
            _publicInstance));

    testTrace(
        IspModelingUtils.INTERNET_HOST_NAME,
        _publicInstancePrivateIp,
        FlowDisposition.INSUFFICIENT_INFO,
        ImmutableList.of(IspModelingUtils.INTERNET_HOST_NAME));
  }

  @Test
  public void testToInternet() {
    testTrace(
        _publicInstance,
        Ip.parse("8.8.8.8"),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(
            _publicInstance,
            _publicSubnet,
            _igw,
            getIspNodeName(AWS_BACKBONE_AS),
            IspModelingUtils.INTERNET_HOST_NAME));

    testTrace(
        _privateInstance,
        Ip.parse("8.8.8.8"),
        FlowDisposition.NO_ROUTE,
        ImmutableList.of(_privateInstance, _privateSubnet, _vgw));
  }

  @Test
  public void testToSite() {
    // public instance should send site traffic to Internet
    testTrace(
        _publicInstance,
        _sitePrefix.getStartIp(),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(
            _publicInstance,
            _publicSubnet,
            _igw,
            getIspNodeName(AWS_BACKBONE_AS),
            INTERNET_HOST_NAME));

    // private instance should send site traffic to VGW
    SortedMap<Flow, List<Trace>> traces = getTraces(_privateInstance, _sitePrefix.getStartIp());
    Flow flow = getOnlyElement(traces.keySet().iterator());
    List<Trace> flowTraces = traces.get(flow);

    assertThat(flowTraces.size(), equalTo(2)); // one per tunnel
    testTrace(
        flowTraces.get(0),
        FlowDisposition.EXITS_NETWORK, //  the other end is not in the snapshot
        ImmutableList.of(_privateInstance, _privateSubnet, _vgw));
    testTrace(
        flowTraces.get(1),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(_privateInstance, _privateSubnet, _vgw));
  }

  @Test
  public void testFromVgw() {
    // public IP space should not be reachable
    testTrace(_vgw, _publicInstancePublicIp, FlowDisposition.NO_ROUTE, ImmutableList.of(_vgw));

    testTrace(
        _vgw,
        _publicInstancePrivateIp,
        FlowDisposition.DENIED_IN,
        ImmutableList.of(_vgw, _vpc, _publicSubnet, _publicInstance));

    testTrace(
        _vgw,
        _privateInstancePrivateIp,
        FlowDisposition.DENIED_IN,
        ImmutableList.of(_vgw, _vpc, _privateSubnet, _privateInstance));
  }
}

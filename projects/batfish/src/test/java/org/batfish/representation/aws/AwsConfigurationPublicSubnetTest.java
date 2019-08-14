package org.batfish.representation.aws;

import static com.google.common.collect.Iterators.getOnlyElement;
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
 * https://docs.aws.amazon.com/vpc/latest/userguide/VPC_Scenario1.html
 */
public class AwsConfigurationPublicSubnetTest {

  private static final String TESTCONFIGS_DIR = "org/batfish/representation/aws/test-public-subnet";

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
  private static String _instance = "i-06669a407b2ef3ae5";
  private static String _subnet = "subnet-0bb82ae8b3ea5dc78";
  private static String _igw = "igw-0a409e562d251f766";
  private static Ip _publicIp = Ip.parse("18.216.80.133");
  private static Ip _privateIp = Ip.parse("10.0.0.91");

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
    Flow flow =
        Flow.builder()
            .setTag("test")
            .setIngressNode(ingressNode)
            .setDstIp(dstIp) // this public IP does not exists in the network
            .build();
    SortedMap<Flow, List<Trace>> traces =
        _batfish.getTracerouteEngine().computeTraces(ImmutableSet.of(flow), false);

    Trace trace = getOnlyElement(traces.get(flow).iterator());

    assertThat(
        trace.getHops().stream()
            .map(h -> h.getNode().getName())
            .collect(ImmutableList.toImmutableList()),
        equalTo(expectedNodes));
    assertThat(trace.getDisposition(), equalTo(expectedDisposition));
  }

  @Test
  public void testFromInternetToPublicIp() {
    // to a valid public IP
    testTrace(
        IspModelingUtils.INTERNET_HOST_NAME,
        _publicIp,
        FlowDisposition.DENIED_IN, // by the default security settings
        ImmutableList.of(
            IspModelingUtils.INTERNET_HOST_NAME,
            IspModelingUtils.getIspNodeName(AWS_BACKBONE_AS),
            _igw,
            _subnet,
            _instance));

    // to a public IP outside of our space
    testTrace(
        IspModelingUtils.INTERNET_HOST_NAME,
        Ip.parse("54.191.107.23"),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(IspModelingUtils.INTERNET_HOST_NAME));
  }

  @Test
  public void testFromInternetToPrivateIp() {
    // we get insufficient info for private IPs that exist somewhere in the network
    testTrace(
        IspModelingUtils.INTERNET_HOST_NAME,
        _privateIp,
        FlowDisposition.INSUFFICIENT_INFO,
        ImmutableList.of(IspModelingUtils.INTERNET_HOST_NAME));

    // we get exits network for arbitrary private IPs
    testTrace(
        IspModelingUtils.INTERNET_HOST_NAME,
        Ip.parse("192.18.0.8"),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(IspModelingUtils.INTERNET_HOST_NAME));
  }

  @Test
  public void testToInternet() {
    testTrace(
        _instance,
        Ip.parse("8.8.8.8"),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(
            _instance,
            _subnet,
            _igw,
            IspModelingUtils.getIspNodeName(AWS_BACKBONE_AS),
            IspModelingUtils.INTERNET_HOST_NAME));
  }
}

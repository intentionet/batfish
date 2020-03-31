package org.batfish.representation.aws;

import static org.batfish.common.util.isp.IspModelingUtils.INTERNET_HOST_NAME;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.getTcpFlow;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.testSetup;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.testTrace;
import static org.batfish.representation.aws.InternetGateway.AWS_BACKBONE_NODE_NAME;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * E2e tests of NAT behavior. The snapshot was created using the terraform definition file that is
 * checked into {@link #TESTCONFIGS_DIR} and then AWS data was pulled.
 *
 * <p>The test setup has two subnets in the same VPC, one public and one private. It also has a NAT
 * gateway in the public subnet and an Internet gateway. The private subnet's default route points
 * to the NAT gateway, and it has an instance to act as the client of the NAT.
 *
 * <p>There are other entities in the setup (e.g., another private subnet) that are not relevant to
 * the tests below but were part of the definition file (and pulled data) for manual testing.
 */
public class AwsConfigurationNatGatewayTest {

  private static final String TESTCONFIGS_DIR = "org/batfish/representation/aws/test-nat-gateway";

  private static final List<String> fileNames =
      ImmutableList.of(
          "InternetGateways.json",
          "NatGateways.json",
          "NetworkAcls.json",
          "NetworkInterfaces.json",
          "Reservations.json",
          "RouteTables.json",
          "SecurityGroups.json",
          "Subnets.json",
          "Vpcs.json");

  // various entities in the configs
  private static String _vpc = "vpc-0008a7b45e3ddf1dd";
  private static String _natGateway = "nat-07ab4846da51f4612";
  private static String _subnetNat = "subnet-0428892a357fa1f94";
  private static Ip _publicIpNat = Ip.parse("3.135.127.225");
  private static Ip _privateIpNat = Ip.parse("10.1.250.210");
  private static String _internetGateway = "igw-071753b9c23d8a9b2";

  private static String _instanceS1 = "i-0a128d26e59be60f3"; // private subnet
  private static String _subnetS1 = "subnet-06f469bcee42e408e";

  private String _instanceNatSubnet = "i-0b31b509174d7f5de";

  private static IBatfish _batfish;

  @ClassRule public static TemporaryFolder _folder = new TemporaryFolder();

  @BeforeClass
  public static void setup() throws IOException {
    _batfish = testSetup(TESTCONFIGS_DIR, fileNames, _folder);
  }

  @Test
  public void testInstanceToInternet_bidirectional() {
    Flow flow = getTcpFlow(_instanceS1, Ip.parse("8.8.8.8"), 80, _batfish);
    AwsConfigurationTestUtils.testBidirectionalTrace(
        flow,
        ImmutableList.of(
            _instanceS1,
            _subnetS1,
            _vpc,
            _natGateway,
            _subnetNat,
            _vpc,
            _internetGateway,
            AWS_BACKBONE_NODE_NAME,
            INTERNET_HOST_NAME),
        ImmutableList.of(
            INTERNET_HOST_NAME,
            AWS_BACKBONE_NODE_NAME,
            _internetGateway,
            _vpc,
            _subnetNat,
            _natGateway,
            _vpc,
            _subnetS1,
            _instanceS1),
        _batfish);
  }

  /** Test that packets for unsupported IP protocols are dropped at the NAT */
  @Test
  public void testUnsupportedIpProtocol() {
    Flow flow =
        Flow.builder()
            .setIngressNode(_instanceS1)
            .setSrcIp(AwsConfigurationTestUtils.getOnlyNodeIp(_instanceS1, _batfish))
            .setDstIp(Ip.parse("8.8.8.8"))
            .setIpProtocol(IpProtocol.AN)
            .build();
    testTrace(
        flow,
        FlowDisposition.DENIED_IN,
        ImmutableList.of(_instanceS1, _subnetS1, _vpc, _natGateway),
        _batfish);
  }

  /** Test that packets that come into the NAT without an installed session are dropped */
  @Test
  public void testNonSessionPacket() {
    Flow flow = getTcpFlow(INTERNET_HOST_NAME, Ip.parse("8.8.8.8"), _publicIpNat, 80);
    testTrace(
        flow,
        FlowDisposition.DENIED_IN,
        ImmutableList.of(
            INTERNET_HOST_NAME,
            AWS_BACKBONE_NODE_NAME,
            _internetGateway,
            _vpc,
            _subnetNat,
            _natGateway),
        _batfish);
  }

  /** Test that packets that come into the NAT from within the subnet are dropped */
  @Test
  public void testIntraSubnetPacket() {
    Flow flow = getTcpFlow(_instanceNatSubnet, _privateIpNat, 80, _batfish);
    testTrace(
        flow,
        FlowDisposition.DENIED_IN,
        ImmutableList.of(_instanceNatSubnet, _natGateway),
        _batfish);
  }
}

package org.batfish.representation.aws;

import static org.batfish.datamodel.FlowDiff.flowDiff;
import static org.batfish.datamodel.transformation.IpField.DESTINATION;
import static org.batfish.datamodel.transformation.IpField.SOURCE;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.getAnyFlow;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.getTcpFlow;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.testSetup;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.testTrace;
import static org.batfish.representation.aws.InternetGateway.AWS_BACKBONE_NODE_NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.List;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.isp.IspModelingUtils;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.flow.StepAction;
import org.batfish.datamodel.flow.Trace;
import org.batfish.datamodel.flow.TransformationStep;
import org.batfish.datamodel.flow.TransformationStep.TransformationStepDetail;
import org.batfish.datamodel.flow.TransformationStep.TransformationType;
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

  // various entities in the configs
  private static String _instance = "i-06669a407b2ef3ae5";
  private static String _subnet = "subnet-0bb82ae8b3ea5dc78";
  private static String _vpc = "vpc-0672bce73895d7252";
  private static String _igw = "igw-0a409e562d251f766";
  private static Ip _publicIp = Ip.parse("18.216.80.133");
  private static Ip _privateIp = Ip.parse("10.0.0.91");

  @ClassRule public static TemporaryFolder _folder = new TemporaryFolder();

  private static IBatfish _batfish;

  @BeforeClass
  public static void setup() throws IOException {
    _batfish = testSetup(TESTCONFIGS_DIR, fileNames, _folder);
  }

  @Test
  public void testFromInternetToValidPublicIp() {
    Trace trace =
        testTrace(
            getAnyFlow(IspModelingUtils.INTERNET_HOST_NAME, _publicIp, _batfish),
            FlowDisposition.DENIED_IN, // by the default security settings
            ImmutableList.of(
                IspModelingUtils.INTERNET_HOST_NAME,
                AWS_BACKBONE_NODE_NAME,
                _igw,
                _vpc,
                _subnet,
                _instance),
            _batfish);

    // Test NAT behavior
    assertThat(
        trace.getHops().get(2).getSteps().get(1),
        equalTo(
            new TransformationStep(
                new TransformationStepDetail(
                    TransformationType.DEST_NAT,
                    ImmutableSortedSet.of(flowDiff(DESTINATION, _publicIp, _privateIp))),
                StepAction.TRANSFORMED)));
  }

  @Test
  public void testFromInternetToInvalidPublicIp() {
    testTrace(
        getAnyFlow(IspModelingUtils.INTERNET_HOST_NAME, Ip.parse("54.191.107.23"), _batfish),
        FlowDisposition.NULL_ROUTED,
        ImmutableList.of(IspModelingUtils.INTERNET_HOST_NAME, AWS_BACKBONE_NODE_NAME),
        _batfish);
  }

  @Test
  public void testFromInternetToPrivateIp() {
    // we get insufficient info for private IPs that exist somewhere in the network
    testTrace(
        getAnyFlow(IspModelingUtils.INTERNET_HOST_NAME, _privateIp, _batfish),
        FlowDisposition.NULL_ROUTED,
        ImmutableList.of(IspModelingUtils.INTERNET_HOST_NAME),
        _batfish);

    // we get exits network for arbitrary private IPs
    testTrace(
        getAnyFlow(IspModelingUtils.INTERNET_HOST_NAME, Ip.parse("192.18.0.8"), _batfish),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(IspModelingUtils.INTERNET_HOST_NAME),
        _batfish);
  }

  @Test
  public void testToInternet() {
    Trace trace =
        testTrace(
            getAnyFlow(_instance, Ip.parse("8.8.8.8"), _batfish),
            FlowDisposition.EXITS_NETWORK,
            ImmutableList.of(
                _instance,
                _subnet,
                _vpc,
                _igw,
                AWS_BACKBONE_NODE_NAME,
                IspModelingUtils.INTERNET_HOST_NAME),
            _batfish);

    // Test NAT behavior
    assertThat(
        trace.getHops().get(3).getSteps(),
        hasItem(
            new TransformationStep(
                new TransformationStepDetail(
                    TransformationType.SOURCE_NAT,
                    ImmutableSortedSet.of(flowDiff(SOURCE, _privateIp, _publicIp))),
                StepAction.TRANSFORMED)));
  }

  /** Packets comes with a private IP for which we don't have a public IP association */
  @Test
  public void testToInternetInvalidPrivateIp() {
    testTrace(
        // start the flow at subnet -- otherwise the anti-spoofing filter at the instance will block
        // and the packet won't make it to the igw
        getTcpFlow(_subnet, Ip.parse("10.0.0.92"), Ip.parse("8.8.8.8"), 80),
        FlowDisposition.DENIED_IN,
        ImmutableList.of(_subnet, _vpc, _igw),
        _batfish);
  }
}

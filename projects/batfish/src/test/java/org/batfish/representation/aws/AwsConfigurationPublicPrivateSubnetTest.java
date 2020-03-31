package org.batfish.representation.aws;

import static org.batfish.common.util.isp.IspModelingUtils.INTERNET_HOST_NAME;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.getAnyFlow;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.getTraces;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.testSetup;
import static org.batfish.representation.aws.AwsConfigurationTestUtils.testTrace;
import static org.batfish.representation.aws.InternetGateway.AWS_BACKBONE_NODE_NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.isp.IspModelingUtils;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.flow.Trace;
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

  private static Prefix _sitePrefix = Prefix.parse("19.168.0.0/16");

  @ClassRule public static TemporaryFolder _folder = new TemporaryFolder();

  private static IBatfish _batfish;

  @BeforeClass
  public static void setup() throws IOException {
    _batfish = testSetup(TESTCONFIGS_DIR, fileNames, _folder);
  }

  @Test
  public void testFromPrivateToPublicSubnet() {
    // to the public IP: should end at VGW and not go to the public instance. disposition should be
    // no route since the VGW does not know what to do with the packet
    testTrace(
        getAnyFlow(_privateInstance, _publicInstancePublicIp, _batfish),
        FlowDisposition.NO_ROUTE,
        ImmutableList.of(_privateInstance, _privateSubnet, _vpc, _vgw),
        _batfish);

    // to the private IP
    testTrace(
        getAnyFlow(_privateInstance, _publicInstancePrivateIp, _batfish),
        FlowDisposition.DENIED_IN, // by the default security settings
        ImmutableList.of(_privateInstance, _privateSubnet, _vpc, _publicSubnet, _publicInstance),
        _batfish);
  }

  @Test
  public void testFromPublicToPrivateSubnet() {
    testTrace(
        getAnyFlow(_publicInstance, _privateInstancePrivateIp, _batfish),
        FlowDisposition.DENIED_IN, // by the default security settings
        ImmutableList.of(_publicInstance, _publicSubnet, _vpc, _privateSubnet, _privateInstance),
        _batfish);
  }

  @Test
  public void testFromInternet() {
    testTrace(
        getAnyFlow(IspModelingUtils.INTERNET_HOST_NAME, _publicInstancePublicIp, _batfish),
        FlowDisposition.DENIED_IN, // be the default security setting
        ImmutableList.of(
            IspModelingUtils.INTERNET_HOST_NAME,
            AWS_BACKBONE_NODE_NAME,
            _igw,
            _vpc,
            _publicSubnet,
            _publicInstance),
        _batfish);

    testTrace(
        getAnyFlow(IspModelingUtils.INTERNET_HOST_NAME, _publicInstancePrivateIp, _batfish),
        FlowDisposition.NULL_ROUTED,
        ImmutableList.of(IspModelingUtils.INTERNET_HOST_NAME),
        _batfish);
  }

  @Test
  public void testToInternet() {
    testTrace(
        getAnyFlow(_publicInstance, Ip.parse("8.8.8.8"), _batfish),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(
            _publicInstance,
            _publicSubnet,
            _vpc,
            _igw,
            AWS_BACKBONE_NODE_NAME,
            IspModelingUtils.INTERNET_HOST_NAME),
        _batfish);

    testTrace(
        getAnyFlow(_privateInstance, Ip.parse("8.8.8.8"), _batfish),
        FlowDisposition.NO_ROUTE,
        ImmutableList.of(_privateInstance, _privateSubnet, _vpc, _vgw),
        _batfish);
  }

  @Test
  public void testToSite() {
    // public instance should send site traffic to Internet
    testTrace(
        getAnyFlow(_publicInstance, _sitePrefix.getStartIp(), _batfish),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(
            _publicInstance, _publicSubnet, _vpc, _igw, AWS_BACKBONE_NODE_NAME, INTERNET_HOST_NAME),
        _batfish);

    // private instance should send site traffic to VGW
    //    SortedMap<Flow, List<Trace>> traces = getTraces(_privateInstance,
    // _sitePrefix.getStartIp());
    //    Flow flow = getOnlyElement(traces.keySet().iterator());
    List<Trace> flowTraces =
        getTraces(getAnyFlow(_privateInstance, _sitePrefix.getStartIp(), _batfish), _batfish);

    assertThat(flowTraces.size(), equalTo(2)); // one per tunnel
    testTrace(
        flowTraces.get(0),
        FlowDisposition.EXITS_NETWORK, //  the other end is not in the snapshot
        ImmutableList.of(_privateInstance, _privateSubnet, _vpc, _vgw));
    testTrace(
        flowTraces.get(1),
        FlowDisposition.EXITS_NETWORK,
        ImmutableList.of(_privateInstance, _privateSubnet, _vpc, _vgw));
  }

  @Test
  public void testFromVgw() {
    // public IP space should not be reachable
    testTrace(
        getAnyFlow(_vgw, _publicInstancePublicIp, _batfish),
        FlowDisposition.NO_ROUTE,
        ImmutableList.of(_vgw),
        _batfish);

    // The private subnet does not point to the VGW, so its (private) address space is not available
    // via the VGW
    testTrace(
        getAnyFlow(_vgw, _publicInstancePrivateIp, _batfish),
        FlowDisposition.NULL_ROUTED,
        ImmutableList.of(_vgw, _vpc),
        _batfish);

    testTrace(
        getAnyFlow(_vgw, _privateInstancePrivateIp, _batfish),
        FlowDisposition.DENIED_IN,
        ImmutableList.of(_vgw, _vpc, _privateSubnet, _privateInstance),
        _batfish);
  }
}

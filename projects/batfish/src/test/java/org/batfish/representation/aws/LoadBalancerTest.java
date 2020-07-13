package org.batfish.representation.aws;

import static com.google.common.collect.Iterators.getOnlyElement;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.datamodel.NamedPort.EPHEMERAL_HIGHEST;
import static org.batfish.datamodel.NamedPort.EPHEMERAL_LOWEST;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.TraceTreeMatchers.hasTraceElement;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.batfish.representation.aws.AwsLocationInfoUtils.INFRASTRUCTURE_LOCATION_INFO;
import static org.batfish.representation.aws.LoadBalancer.FINAL_TRANSFORMATION;
import static org.batfish.representation.aws.LoadBalancer.LISTENER_FILTER_NAME;
import static org.batfish.representation.aws.LoadBalancer.LOAD_BALANCER_INTERFACE_DESCRIPTION_PREFIX;
import static org.batfish.representation.aws.LoadBalancer.chainListenerTransformations;
import static org.batfish.representation.aws.LoadBalancer.computeListenerFilter;
import static org.batfish.representation.aws.LoadBalancer.computeNotForwardedFilter;
import static org.batfish.representation.aws.LoadBalancer.computeTargetGroupTransformationStep;
import static org.batfish.representation.aws.LoadBalancer.computeTargetTransformationStep;
import static org.batfish.representation.aws.LoadBalancer.getActiveTargets;
import static org.batfish.representation.aws.LoadBalancer.getNodeId;
import static org.batfish.representation.aws.LoadBalancer.getTraceElementForForwardedPackets;
import static org.batfish.representation.aws.LoadBalancer.getTraceElementForMatchedListener;
import static org.batfish.representation.aws.LoadBalancer.getTraceElementForNoMatchedListener;
import static org.batfish.representation.aws.LoadBalancer.getTraceElementForNotForwardedPackets;
import static org.batfish.representation.aws.LoadBalancer.isTargetInAnyEnabledAvailabilityZone;
import static org.batfish.representation.aws.Subnet.NLB_INSTANCE_TARGETS_IFACE_SUFFIX;
import static org.batfish.representation.aws.Utils.interfaceNameToRemote;
import static org.batfish.representation.aws.Utils.publicIpAddressGroupName;
import static org.batfish.representation.aws.Utils.toStaticRoute;
import static org.batfish.specifier.Location.interfaceLinkLocation;
import static org.batfish.specifier.Location.interfaceLocation;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.batfish.common.Warnings;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclTracer;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.transformation.ApplyAll;
import org.batfish.datamodel.transformation.ApplyAny;
import org.batfish.datamodel.transformation.Noop;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationStep;
import org.batfish.referencelibrary.AddressGroup;
import org.batfish.referencelibrary.GeneratedRefBookUtils;
import org.batfish.referencelibrary.GeneratedRefBookUtils.BookType;
import org.batfish.representation.aws.Instance.Status;
import org.batfish.representation.aws.LoadBalancer.AvailabilityZone;
import org.batfish.representation.aws.LoadBalancer.Protocol;
import org.batfish.representation.aws.LoadBalancer.Scheme;
import org.batfish.representation.aws.LoadBalancer.Type;
import org.batfish.representation.aws.LoadBalancerListener.ActionType;
import org.batfish.representation.aws.LoadBalancerListener.DefaultAction;
import org.batfish.representation.aws.LoadBalancerListener.Listener;
import org.batfish.representation.aws.LoadBalancerTargetHealth.HealthState;
import org.batfish.representation.aws.LoadBalancerTargetHealth.TargetHealth;
import org.batfish.representation.aws.LoadBalancerTargetHealth.TargetHealthDescription;
import org.junit.Test;

public class LoadBalancerTest {

  /** Default Ip to use in tests for the load balancer */
  private static final Ip _loadBalancerIp = Ip.parse("10.10.10.10");

  private static final String _loadBalancerName = "lbName";
  private static final String _loadBalancerArnSuffix = "net/lbName/987654321";
  private static final String _loadBalancerArn =
      String.format(
          "arn:aws:elasticloadbalancing:region:123456789:loadbalancer/%s", _loadBalancerArnSuffix);
  private static final String _subnet1Name = "subnet1";
  private static final String _subnet2Name = "subnet2";

  /** Default load balancer object to use in tests */
  private static final LoadBalancer _loadBalancer =
      new LoadBalancer(
          _loadBalancerArn,
          ImmutableList.of(
              new AvailabilityZone(_subnet1Name, "zone1"),
              new AvailabilityZone(_subnet2Name, "zone2")),
          "lbDnsName",
          _loadBalancerName,
          Scheme.INTERNET_FACING,
          Type.NETWORK,
          "vpc");

  private static Flow getTcpFlow(Ip dstIp) {
    return Flow.builder()
        .setIpProtocol(IpProtocol.TCP)
        .setSrcPort(49876)
        .setDstIp(dstIp)
        .setDstPort(80)
        .setIngressNode("foo")
        .build();
  }

  @Test
  public void testDeserialization() throws IOException {
    String text = readResource("org/batfish/representation/aws/LoadBalancerTest.json", UTF_8);

    JsonNode json = BatfishObjectMapper.mapper().readTree(text);
    Region region = new Region("r1");
    region.addConfigElement(json, null, null);

    /** inactive load balancer should not show */
    assertThat(
        region.getLoadBalancers(),
        equalTo(
            ImmutableSet.of(
                new LoadBalancer(
                    "arn:aws:elasticloadbalancing:us-east-2:554773406868:loadbalancer/net/lb-lb/6f57a43b75d8f2c1",
                    ImmutableList.of(
                        new AvailabilityZone("subnet-01822d50b2db5a4a0", "us-east-2a"),
                        new AvailabilityZone("subnet-09f78a95e9df6b959", "us-east-2b")),
                    "lb-lb-6f57a43b75d8f2c1.elb.us-east-2.amazonaws.com",
                    "lb-lb",
                    Scheme.INTERNET_FACING,
                    Type.NETWORK,
                    "vpc-08afc01f5013ddc43"),
                new LoadBalancer(
                    "arn-application",
                    ImmutableList.of(),
                    "lb-lb-6f57a43b75d8f2c1.elb.us-east-2.amazonaws.com",
                    "lb3",
                    Scheme.INTERNAL,
                    Type.APPLICATION,
                    "vpc-08afc01f5013ddc43"))));
  }

  /**
   * Test that we create one configuration per availability zone. The content of the configuration
   * is tested in other tests
   */
  @Test
  public void testToConfigurationNodes() {
    List<Configuration> cfgNodes =
        _loadBalancer.toConfigurationNodes(
            new ConvertedConfiguration(), Region.builder("r1").build(), new Warnings());
    assertThat(cfgNodes.size(), equalTo(2));
  }

  @Test
  public void testToConfigurationNodes_skipApplicationLb() {
    List<Configuration> cfgNodes =
        new LoadBalancer(
                "arn",
                ImmutableList.of(
                    new AvailabilityZone("subnet1", "zone1"),
                    new AvailabilityZone("subnet2", "zone2")),
                "lbDnsName",
                "lbName",
                Scheme.INTERNET_FACING,
                Type.APPLICATION,
                "vpc")
            .toConfigurationNodes(
                new ConvertedConfiguration(), Region.builder("r1").build(), new Warnings());
    assertThat(cfgNodes.size(), equalTo(0));
  }

  /** Tests the basics of the configuration object */
  @Test
  public void testToConfigurationNode() {
    AvailabilityZone availabilityZone = _loadBalancer.getAvailabilityZones().get(0);
    Prefix subnetPrefix = Prefix.create(_loadBalancerIp, 24);
    Ip publicIp = Ip.parse("1.1.1.1");
    NetworkInterface networkInterface =
        new NetworkInterface(
            "interface",
            availabilityZone.getSubnetId(),
            _loadBalancer.getVpcId(),
            ImmutableList.of(),
            ImmutableList.of(new PrivateIpAddress(true, _loadBalancerIp, publicIp)),
            LOAD_BALANCER_INTERFACE_DESCRIPTION_PREFIX + _loadBalancerArnSuffix,
            null,
            ImmutableMap.of());
    Region region =
        Region.builder("r1")
            .setNetworkInterfaces(ImmutableMap.of("interface", networkInterface))
            .setSubnets(
                ImmutableMap.of(
                    availabilityZone.getSubnetId(),
                    new Subnet(
                        subnetPrefix,
                        availabilityZone.getSubnetId(),
                        _loadBalancer.getVpcId(),
                        availabilityZone.getZoneName(),
                        ImmutableMap.of())))
            .build();
    Configuration cfgNode =
        _loadBalancer.toConfigurationNode(
            availabilityZone, new ConvertedConfiguration(), region, new Warnings());

    assertThat(
        cfgNode.getHostname(),
        equalTo(
            getNodeId(_loadBalancer.getDnsName(), availabilityZone.getZoneName()).toLowerCase()));
    assertThat(cfgNode.getDeviceModel(), equalTo(DeviceModel.AWS_ELB_NETWORK));
    assertThat(cfgNode.getHumanName(), equalTo(_loadBalancer.getName()));
    assertThat(cfgNode.getVendorFamily().getAws().getVpcId(), equalTo(_loadBalancer.getVpcId()));
    assertThat(
        cfgNode.getVendorFamily().getAws().getSubnetId(), equalTo(availabilityZone.getSubnetId()));
    assertThat(cfgNode.getVendorFamily().getAws().getRegion(), equalTo(region.getName()));

    Interface viIface = getOnlyElement(cfgNode.getAllInterfaces().values().iterator());
    assertThat(
        viIface.getConcreteAddress(),
        equalTo(ConcreteInterfaceAddress.create(_loadBalancerIp, 24)));

    assertThat(viIface.getIncomingFilter().getName(), equalTo(LISTENER_FILTER_NAME));
    assertThat(viIface.getIncomingTransformation(), equalTo(FINAL_TRANSFORMATION));
    assertThat(
        viIface.getPostTransformationIncomingFilter(),
        equalTo(
            computeNotForwardedFilter(
                ImmutableList.of(new PrivateIpAddress(true, _loadBalancerIp, null)))));

    assertThat(
        cfgNode
            .getGeneratedReferenceBooks()
            .get(GeneratedRefBookUtils.getName(cfgNode.getHostname(), BookType.PublicIps))
            .getAddressGroups(),
        hasItem(
            new AddressGroup(
                ImmutableSortedSet.of(publicIp.toString()),
                publicIpAddressGroupName(networkInterface))));

    assertThat(
        cfgNode.getLocationInfo(),
        equalTo(
            ImmutableMap.of(
                interfaceLocation(viIface),
                INFRASTRUCTURE_LOCATION_INFO,
                interfaceLinkLocation(viIface),
                INFRASTRUCTURE_LOCATION_INFO)));
  }

  @Test
  public void testInstallTransformation() {
    Interface viIface =
        Interface.builder()
            .setName("interface")
            .setAddress(ConcreteInterfaceAddress.create(_loadBalancerIp, 24))
            .build();

    // We create two listeners that are identically configured and test that transformations for
    // both are created.

    TargetGroup targetGroup =
        new TargetGroup(
            "tgArn", ImmutableList.of(), Protocol.TCP, 80, "tgNameGood", TargetGroup.Type.IP);
    DefaultAction action = new DefaultAction(1, "tgArn", ActionType.FORWARD);
    TargetHealthDescription target =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "1.1.1.1", 80),
            new TargetHealth(HealthState.HEALTHY));

    List<Listener> listeners =
        ImmutableList.of(
            new Listener("listener1Arn", ImmutableList.of(action), Protocol.TCP, 80),
            new Listener("listener2Arn", ImmutableList.of(action), Protocol.TCP, 82));

    Region region =
        Region.builder("r1")
            .setTargetGroups(ImmutableMap.of(targetGroup.getId(), targetGroup))
            .setLoadBalancerTargetHealths(
                ImmutableMap.of(
                    "tgArn", new LoadBalancerTargetHealth("tgArn", ImmutableList.of(target))))
            .build();

    _loadBalancer.installTransformations(viIface, "zone1", true, listeners, region, new Warnings());

    assertThat(
        viIface.getIncomingTransformation(),
        equalTo(
            chainListenerTransformations(
                ImmutableList.of(
                    Objects.requireNonNull(
                        _loadBalancer.computeListenerTransformation(
                            listeners.get(0),
                            "zone1",
                            _loadBalancerIp,
                            true,
                            region,
                            new Warnings())),
                    Objects.requireNonNull(
                        _loadBalancer.computeListenerTransformation(
                            listeners.get(1),
                            "zone1",
                            _loadBalancerIp,
                            true,
                            region,
                            new Warnings()))))));
    assertThat(
        viIface.getFirewallSessionInterfaceInfo(),
        equalTo(
            new FirewallSessionInterfaceInfo(
                false, ImmutableList.of(viIface.getName()), null, null)));
  }

  @Test
  public void testInstallTransformation_noListener() {
    Interface viIface = Interface.builder().setName("interface").build();
    Region region = Region.builder("r1").build();
    _loadBalancer.installTransformations(
        viIface, "zone1", true, ImmutableList.of(), region, new Warnings());
    assertThat(viIface.getIncomingTransformation(), equalTo(FINAL_TRANSFORMATION));
    assertThat(
        viIface.getFirewallSessionInterfaceInfo(),
        equalTo(
            new FirewallSessionInterfaceInfo(
                false, ImmutableList.of(viIface.getName()), null, null)));
  }

  /** Test that we skip over bad actions and create the right transformation for the good one */
  @Test
  public void testComputeListenerTransformation() {
    DefaultAction actionBad = new DefaultAction(1, "tgArnBad", ActionType.AUTHENTICATE_OIDC);
    TargetGroup targetGroupBad =
        new TargetGroup(
            "tgArnBad", ImmutableList.of(), Protocol.TCP, 80, "tgNameBad", TargetGroup.Type.IP);

    DefaultAction actionGood = new DefaultAction(2, "tgArnGood", ActionType.FORWARD);
    TargetGroup targetGroupGood =
        new TargetGroup(
            "tgArnGood", ImmutableList.of(), Protocol.TCP, 80, "tgNameGood", TargetGroup.Type.IP);

    TargetHealthDescription badTarget =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "6.6.6.6", 80),
            new TargetHealth(HealthState.UNHEALTHY));
    TargetHealthDescription goodTarget =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "1.1.1.1", 80),
            new TargetHealth(HealthState.HEALTHY));

    Listener listener =
        new Listener("listenerArn", ImmutableList.of(actionBad, actionGood), Protocol.TCP, 80);

    Region region =
        Region.builder("r1")
            .setTargetGroups(
                ImmutableMap.of(
                    targetGroupBad.getId(),
                    targetGroupBad,
                    targetGroupGood.getId(),
                    targetGroupGood))
            .setLoadBalancerTargetHealths(
                ImmutableMap.of(
                    "tgBadArn",
                    new LoadBalancerTargetHealth("tgArnBad", ImmutableList.of(badTarget)),
                    "tgArnGood",
                    new LoadBalancerTargetHealth("tgArnGood", ImmutableList.of(goodTarget))))
            .build();

    LoadBalancerTransformation loadBalancerTransformation =
        _loadBalancer.computeListenerTransformation(
            listener, "zone", _loadBalancerIp, true, region, new Warnings());

    assertThat(
        loadBalancerTransformation.getGuard(),
        equalTo(new MatchHeaderSpace(listener.getMatchingHeaderSpace())));

    assertThat(
        loadBalancerTransformation.getStep(),
        equalTo(
            computeTargetGroupTransformationStep(
                "tgArnGood", "zone", _loadBalancerIp, true, region, new Warnings())));
  }

  /** Tests that we return null when no good action is possible */
  @Test
  public void testComputeListenerTransformation_nullReturn() {
    DefaultAction actionBad = new DefaultAction(1, "tgArnBad", ActionType.FORWARD);
    TargetGroup targetGroupBad =
        new TargetGroup(
            "tgArnBad", ImmutableList.of(), Protocol.TCP, 80, "tgNameBad", TargetGroup.Type.IP);
    TargetHealthDescription badTarget =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "6.6.6.6", 80),
            new TargetHealth(HealthState.UNHEALTHY));

    Listener listener = new Listener("listenerArn", ImmutableList.of(actionBad), Protocol.TCP, 80);

    Region region =
        Region.builder("r1")
            .setTargetGroups(ImmutableMap.of(targetGroupBad.getId(), targetGroupBad))
            .setLoadBalancerTargetHealths(
                ImmutableMap.of(
                    "tgBadArn",
                    new LoadBalancerTargetHealth("tgArnBad", ImmutableList.of(badTarget))))
            .build();

    assertThat(
        _loadBalancer.computeListenerTransformation(
            listener, "zone", _loadBalancerIp, true, region, new Warnings()),
        nullValue());
  }

  @Test
  public void testComputeTargetGroupTransformationStep() {
    TargetHealthDescription unhealthyTarget =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "6.6.6.6", 80),
            new TargetHealth(HealthState.UNHEALTHY));
    TargetHealthDescription healthyTarget1 =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "1.1.1.1", 80),
            new TargetHealth(HealthState.HEALTHY));
    TargetHealthDescription healthyTarget2 =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "2.2.2.2", 80),
            new TargetHealth(HealthState.HEALTHY));

    String targetGroupArn = "tgArn";
    TargetGroup targetGroup =
        new TargetGroup(
            targetGroupArn, ImmutableList.of(), Protocol.TCP, 80, "tgName", TargetGroup.Type.IP);
    Region region =
        Region.builder("r1")
            .setTargetGroups(ImmutableMap.of(targetGroupArn, targetGroup))
            .setLoadBalancerTargetHealths(
                ImmutableMap.of(
                    targetGroupArn,
                    new LoadBalancerTargetHealth(
                        targetGroupArn,
                        ImmutableList.of(unhealthyTarget, healthyTarget1, healthyTarget2))))
            .build();

    Ip loadBalancerIp = Ip.parse("10.10.10.10");

    // unhealthy target should be ignored
    assertThat(
        computeTargetGroupTransformationStep(
            targetGroupArn, "zone", loadBalancerIp, true, region, new Warnings()),
        equalTo(
            new ApplyAny(
                computeTargetTransformationStep(
                    healthyTarget1.getTarget(),
                    TargetGroup.Type.IP,
                    loadBalancerIp,
                    region,
                    new Warnings()),
                computeTargetTransformationStep(
                    healthyTarget2.getTarget(),
                    TargetGroup.Type.IP,
                    loadBalancerIp,
                    region,
                    new Warnings()))));
  }

  /** Test that we return null if no target in enabled availability zones found */
  @Test
  public void testComputeTargetGroupTransformationStep_noEnabledTarget() {
    TargetHealthDescription target1 =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "1.1.1.1", 80),
            new TargetHealth(HealthState.UNHEALTHY));

    String targetGroupArn = "tgArn";
    TargetGroup targetGroup =
        new TargetGroup(
            targetGroupArn, ImmutableList.of(), Protocol.TCP, 80, "tgName", TargetGroup.Type.IP);
    Region region =
        Region.builder("r1")
            .setTargetGroups(ImmutableMap.of(targetGroupArn, targetGroup))
            .setLoadBalancerTargetHealths(
                ImmutableMap.of(
                    targetGroupArn,
                    new LoadBalancerTargetHealth(targetGroupArn, ImmutableList.of(target1))))
            .build();

    Ip loadBalancerIp = Ip.parse("10.10.10.10");

    // unhealthy target is ignored should be ignored
    assertThat(
        computeTargetGroupTransformationStep(
            targetGroupArn, "zone", loadBalancerIp, false, region, new Warnings()),
        nullValue());
  }

  /** If all enabled targets are unhealthy, send traffic to all */
  @Test
  public void testComputeTargetGroupTransformationStep_allUnhealthyTargets() {
    TargetHealthDescription unhealthyTarget1 =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "1.1.1.1", 80),
            new TargetHealth(HealthState.UNHEALTHY));
    TargetHealthDescription unhealthyTarget2 =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "2.2.2.2", 80),
            new TargetHealth(HealthState.UNHEALTHY));

    String targetGroupArn = "tgArn";
    TargetGroup targetGroup =
        new TargetGroup(
            targetGroupArn, ImmutableList.of(), Protocol.TCP, 80, "tgName", TargetGroup.Type.IP);
    Region region =
        Region.builder("r1")
            .setTargetGroups(ImmutableMap.of(targetGroupArn, targetGroup))
            .setLoadBalancerTargetHealths(
                ImmutableMap.of(
                    targetGroupArn,
                    new LoadBalancerTargetHealth(
                        targetGroupArn, ImmutableList.of(unhealthyTarget1, unhealthyTarget2))))
            .build();

    Ip loadBalancerIp = Ip.parse("10.10.10.10");

    assertThat(
        computeTargetGroupTransformationStep(
            targetGroupArn, "zone", loadBalancerIp, true, region, new Warnings()),
        equalTo(
            new ApplyAny(
                computeTargetTransformationStep(
                    unhealthyTarget1.getTarget(),
                    TargetGroup.Type.IP,
                    loadBalancerIp,
                    region,
                    new Warnings()),
                computeTargetTransformationStep(
                    unhealthyTarget2.getTarget(),
                    TargetGroup.Type.IP,
                    loadBalancerIp,
                    region,
                    new Warnings()))));
  }

  @Test
  public void testGetActiveTargets() {
    TargetHealthDescription unhealthyTarget =
        new TargetHealthDescription(
            new LoadBalancerTarget("zone1", "6.6.6.6", 80),
            new TargetHealth(HealthState.UNHEALTHY));
    TargetHealthDescription healthyTarget1 =
        new TargetHealthDescription(
            new LoadBalancerTarget("zone1", "1.1.1.1", 80), new TargetHealth(HealthState.HEALTHY));
    TargetHealthDescription healthyTarget2 =
        new TargetHealthDescription(
            new LoadBalancerTarget("zone2", "2.2.2.2", 80), new TargetHealth(HealthState.HEALTHY));
    TargetHealthDescription healthyTargetAll =
        new TargetHealthDescription(
            new LoadBalancerTarget("all", "3.3.3.3", 80), new TargetHealth(HealthState.HEALTHY));

    String targetGroupArn = "tgArn";
    TargetGroup targetGroup =
        new TargetGroup(
            targetGroupArn, ImmutableList.of(), Protocol.TCP, 80, "tgName", TargetGroup.Type.IP);
    Region.RegionBuilder rb =
        Region.builder("r1").setTargetGroups(ImmutableMap.of(targetGroupArn, targetGroup));

    // Cross-zone load balancing on. All healthy targets should be included.
    {
      LoadBalancerTargetHealth targetHealth =
          new LoadBalancerTargetHealth(
              targetGroupArn,
              ImmutableList.of(unhealthyTarget, healthyTarget1, healthyTarget2, healthyTargetAll));
      rb.setLoadBalancerTargetHealths(ImmutableMap.of(targetGroupArn, targetHealth));
      assertThat(
          getActiveTargets(
              targetHealth, targetGroup, ImmutableSet.of("x"), true, rb.build(), false, null),
          containsInAnyOrder(healthyTarget1, healthyTarget2, healthyTargetAll));
    }

    // Cross-zone load balancing off, load balancer includes zone 1. Healthy targets with zone zone1
    // or zone "all" should be included.
    {
      LoadBalancerTargetHealth targetHealth =
          new LoadBalancerTargetHealth(
              targetGroupArn,
              ImmutableList.of(unhealthyTarget, healthyTarget1, healthyTarget2, healthyTargetAll));
      rb.setLoadBalancerTargetHealths(ImmutableMap.of(targetGroupArn, targetHealth));
      assertThat(
          getActiveTargets(
              targetHealth, targetGroup, ImmutableSet.of("zone1"), false, rb.build(), false, null),
          containsInAnyOrder(healthyTarget1, healthyTargetAll));
    }

    // Cross-zone load balancing off, load balancer includes zone 1, which only has an unhealthy
    // target. However, a healthy zone "all" target exists, so should get that.
    {
      LoadBalancerTargetHealth targetHealth =
          new LoadBalancerTargetHealth(
              targetGroupArn, ImmutableList.of(unhealthyTarget, healthyTarget2, healthyTargetAll));
      rb.setLoadBalancerTargetHealths(ImmutableMap.of(targetGroupArn, targetHealth));
      assertThat(
          getActiveTargets(
              targetHealth, targetGroup, ImmutableSet.of("zone1"), false, rb.build(), false, null),
          containsInAnyOrder(healthyTargetAll));
    }

    // Cross-zone load balancing off, load balancer includes zone 1, which only has an unhealthy
    // target. No zone "all" targets exist. Should use the unhealthy target.
    {
      LoadBalancerTargetHealth targetHealth =
          new LoadBalancerTargetHealth(
              targetGroupArn, ImmutableList.of(unhealthyTarget, healthyTarget2));
      rb.setLoadBalancerTargetHealths(ImmutableMap.of(targetGroupArn, targetHealth));
      assertThat(
          getActiveTargets(
              targetHealth, targetGroup, ImmutableSet.of("zone1"), false, rb.build(), false, null),
          containsInAnyOrder(unhealthyTarget));
    }
  }

  @Test
  public void testComputeTargetTransformationStep_instanceTarget() {
    Ip targetIp = Ip.parse("1.1.1.1");
    Ip loadBalancerIp = Ip.parse("10.10.10.10");
    String instanceId = "instance";
    LoadBalancerTarget target = new LoadBalancerTarget("zone", instanceId, 80);

    Region region =
        Region.builder("r1")
            .setInstances(
                ImmutableMap.of(
                    instanceId,
                    Instance.builder()
                        .setInstanceId(instanceId)
                        .setPrimaryPrivateIpAddress(targetIp)
                        .build()))
            .build();

    // Should only do dst NAT for instance targets
    assertThat(
        computeTargetTransformationStep(
            target, TargetGroup.Type.INSTANCE, loadBalancerIp, region, new Warnings()),
        equalTo(
            new ApplyAll(
                TransformationStep.assignDestinationIp(targetIp, targetIp),
                TransformationStep.assignDestinationPort(target.getPort(), target.getPort()))));
  }

  @Test
  public void testComputeTargetTransformationStep_ipTarget() {
    Ip targetIp = Ip.parse("1.1.1.1");
    Ip loadBalancerIp = Ip.parse("10.10.10.10");
    LoadBalancerTarget target = new LoadBalancerTarget("zone", targetIp.toString(), 80);

    assertThat(
        computeTargetTransformationStep(
            target,
            TargetGroup.Type.IP,
            loadBalancerIp,
            Region.builder("r1").build(),
            new Warnings()),
        equalTo(
            new ApplyAll(
                TransformationStep.assignSourceIp(loadBalancerIp, loadBalancerIp),
                TransformationStep.assignSourcePort(
                    EPHEMERAL_LOWEST.number(), EPHEMERAL_HIGHEST.number()),
                TransformationStep.assignDestinationIp(targetIp, targetIp),
                TransformationStep.assignDestinationPort(target.getPort(), target.getPort()))));
  }

  @Test
  public void testIsTargetInEnabledAvailabilityZone_instanceTarget() {
    Subnet subnet =
        new Subnet(Prefix.parse("1.1.1.1/32"), "subnet", "vpc", "targetZone", ImmutableMap.of());
    Instance instance =
        Instance.builder().setInstanceId("instance").setSubnetId(subnet.getId()).build();
    TargetHealthDescription targetHealthDescription =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", instance.getId(), 80),
            new TargetHealth(HealthState.HEALTHY));
    Region region =
        Region.builder("r1")
            .setInstances(ImmutableMap.of(instance.getId(), instance))
            .setSubnets(ImmutableMap.of(subnet.getId(), subnet))
            .build();

    Set<String> targetZone = ImmutableSet.of("targetZone");
    Set<String> otherZone = ImmutableSet.of("otherZone");

    // LB in same zone; cross zone load balancing is off
    assertTrue(
        isTargetInAnyEnabledAvailabilityZone(
            targetHealthDescription, TargetGroup.Type.INSTANCE, targetZone, false, region));

    // LB in same zone; cross zone load balancing is on
    assertTrue(
        isTargetInAnyEnabledAvailabilityZone(
            targetHealthDescription, TargetGroup.Type.INSTANCE, targetZone, true, region));

    // LB in different zone; cross zone load balancing is off
    assertFalse(
        isTargetInAnyEnabledAvailabilityZone(
            targetHealthDescription, TargetGroup.Type.INSTANCE, otherZone, false, region));

    // LB in different zone; cross zone load balancing is on
    assertTrue(
        isTargetInAnyEnabledAvailabilityZone(
            targetHealthDescription, TargetGroup.Type.INSTANCE, otherZone, true, region));
  }

  @Test
  public void testIsTargetInEnabledAvailabilityZone_ipTarget() {
    TargetGroup targetGroup =
        new TargetGroup(
            "tgArg", ImmutableList.of(), Protocol.TCP, 80, "tgName", TargetGroup.Type.IP);
    TargetHealthDescription targetHealthDescription =
        new TargetHealthDescription(
            new LoadBalancerTarget("targetZone", "1.1.1.1", 80),
            new TargetHealth(HealthState.HEALTHY));
    Set<String> targetZone = ImmutableSet.of("targetZone");
    Set<String> otherZone = ImmutableSet.of("otherZone");

    // LB in same zone; cross zone load balancing is off
    assertTrue(
        isTargetInAnyEnabledAvailabilityZone(
            targetHealthDescription,
            targetGroup.getTargetType(),
            targetZone,
            false,
            Region.builder("r1").build()));

    // LB in same zone; cross zone load balancing is on
    assertTrue(
        isTargetInAnyEnabledAvailabilityZone(
            targetHealthDescription,
            targetGroup.getTargetType(),
            targetZone,
            true,
            Region.builder("r1").build()));

    // LB in different zone; cross zone load balancing is off
    assertFalse(
        isTargetInAnyEnabledAvailabilityZone(
            targetHealthDescription,
            targetGroup.getTargetType(),
            otherZone,
            false,
            Region.builder("r1").build()));

    // LB in different zone; cross zone load balancing is on
    assertTrue(
        isTargetInAnyEnabledAvailabilityZone(
            targetHealthDescription,
            targetGroup.getTargetType(),
            otherZone,
            true,
            Region.builder("r1").build()));

    // Target in zone "all"; cross zone is off
    assertTrue(
        isTargetInAnyEnabledAvailabilityZone(
            new TargetHealthDescription(
                new LoadBalancerTarget("all", "1.1.1.1", 80),
                new TargetHealth(HealthState.HEALTHY)),
            targetGroup.getTargetType(),
            otherZone,
            false,
            Region.builder("r1").build()));
  }

  @Test
  public void testChainListenerTransformations() {
    assertThat(chainListenerTransformations(ImmutableList.of()), equalTo(FINAL_TRANSFORMATION));

    AclLineMatchExpr matchExpr =
        new MatchHeaderSpace(
            HeaderSpace.builder().setDstIps(Prefix.parse("1.1.1.1/32").toIpSpace()).build());

    assertThat(
        chainListenerTransformations(
            ImmutableList.of(new LoadBalancerTransformation(matchExpr, Noop.NOOP_SOURCE_NAT))),
        equalTo(
            new Transformation(
                matchExpr, ImmutableList.of(Noop.NOOP_SOURCE_NAT), null, FINAL_TRANSFORMATION)));
  }

  @Test
  public void testFindMyInterface() {
    // right description and subnet
    NetworkInterface networkInterface1 =
        new NetworkInterface(
            "id1",
            "subnet1",
            "vpc",
            ImmutableList.of(),
            ImmutableList.of(new PrivateIpAddress(true, Ip.parse("1.1.1.1"), null)),
            LOAD_BALANCER_INTERFACE_DESCRIPTION_PREFIX + _loadBalancerArnSuffix,
            null,
            ImmutableMap.of());
    // right description but wrong subnet
    NetworkInterface networkInterface2 =
        new NetworkInterface(
            "id2",
            "subnet2",
            "vpc",
            ImmutableList.of(),
            ImmutableList.of(new PrivateIpAddress(true, Ip.parse("1.1.1.1"), null)),
            LOAD_BALANCER_INTERFACE_DESCRIPTION_PREFIX + _loadBalancerArnSuffix,
            null,
            ImmutableMap.of());
    Region region =
        Region.builder("r1")
            .setNetworkInterfaces(
                ImmutableMap.of(
                    networkInterface1.getId(),
                    networkInterface1,
                    networkInterface2.getId(),
                    networkInterface2))
            .build();

    assertThat(
        LoadBalancer.findMyInterface("subnet1", _loadBalancerArn, region),
        equalTo(Optional.of(networkInterface1)));
    assertThat(
        LoadBalancer.findMyInterface("subnet2", _loadBalancerArn, region),
        equalTo(Optional.of(networkInterface2)));
    assertThat(
        LoadBalancer.findMyInterface("nosubnet", _loadBalancerArn, region),
        equalTo(Optional.empty()));
    assertThat(LoadBalancer.findMyInterface("subnet1", "noarn", region), equalTo(Optional.empty()));
  }

  @Test
  public void testComputeListenerFilter() {
    DefaultAction action = new DefaultAction(1, "tgArn", ActionType.FORWARD);
    List<Listener> listeners =
        ImmutableList.of(new Listener("listener1Arn", ImmutableList.of(action), Protocol.TCP, 80));

    IpAccessList ipAccessList = computeListenerFilter(listeners);

    // TCP tp port 80 -- allowed
    Flow allowedFlow = getTcpFlow(Ip.parse("1.1.1.1"));
    assertThat(
        ipAccessList.filter(allowedFlow, null, ImmutableMap.of(), ImmutableMap.of()).getAction(),
        equalTo(LineAction.PERMIT));
    assertThat(
        AclTracer.trace(
            ipAccessList,
            allowedFlow,
            null,
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of()),
        contains(hasTraceElement(getTraceElementForMatchedListener("listener1Arn"))));

    // TCP to port 81 -- dropped
    Flow deniedTcpFlow =
        Flow.builder()
            .setIngressNode("a")
            .setSrcPort(89)
            .setDstPort(81)
            .setIpProtocol(IpProtocol.TCP)
            .build();
    assertThat(
        ipAccessList.filter(deniedTcpFlow, null, ImmutableMap.of(), ImmutableMap.of()).getAction(),
        equalTo(LineAction.DENY));
    assertThat(
        AclTracer.trace(
            ipAccessList,
            deniedTcpFlow,
            null,
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of()),
        contains(hasTraceElement(getTraceElementForNoMatchedListener())));

    // UDP to port 80 -- dropped
    assertThat(
        ipAccessList
            .filter(
                Flow.builder()
                    .setIngressNode("a")
                    .setSrcPort(89)
                    .setDstPort(80)
                    .setIpProtocol(IpProtocol.UDP)
                    .build(),
                null,
                ImmutableMap.of(),
                ImmutableMap.of())
            .getAction(),
        equalTo(LineAction.DENY));
  }

  @Test
  public void testComputeNotForwardedFilter() {
    Ip loadBalancerIp = Ip.parse("10.10.10.10");
    IpAccessList ipAccessList =
        computeNotForwardedFilter(
            ImmutableList.of(new PrivateIpAddress(true, loadBalancerIp, null)));

    // not transformed
    Flow deniedFlow = getTcpFlow(loadBalancerIp);
    assertThat(
        ipAccessList.filter(deniedFlow, null, ImmutableMap.of(), ImmutableMap.of()).getAction(),
        equalTo(LineAction.DENY));
    assertThat(
        AclTracer.trace(
            ipAccessList,
            deniedFlow,
            null,
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of()),
        contains(hasTraceElement(getTraceElementForNotForwardedPackets())));

    // transformed
    Flow allowedFlow = getTcpFlow(Ip.parse("1.1.1.1"));
    assertThat(
        ipAccessList.filter(allowedFlow, null, ImmutableMap.of(), ImmutableMap.of()).getAction(),
        equalTo(LineAction.PERMIT));
    assertThat(
        AclTracer.trace(
            ipAccessList,
            allowedFlow,
            null,
            ImmutableMap.of(),
            ImmutableMap.of(),
            ImmutableMap.of()),
        contains(hasTraceElement(getTraceElementForForwardedPackets())));
  }

  @Test
  public void testInstanceTargetStaticRoutes_noInstanceTargets() {
    // Load balancers with no instance targets should not have any static routes
    List<Configuration> cfgNodes =
        _loadBalancer.toConfigurationNodes(
            new ConvertedConfiguration(), Region.builder("r1").build(), new Warnings());
    cfgNodes.forEach(cfgNode -> assertThat(cfgNode.getDefaultVrf().getStaticRoutes(), empty()));
  }

  @Test
  public void testInstanceTargetStaticRoutes() {
    // Load balancers with instance targets should have static routes to each instance target via
    // their subnet
    Ip instanceIp = Ip.parse("20.20.20.20");
    Instance instance =
        new Instance(
            "instanceId",
            _loadBalancer.getVpcId(),
            _subnet1Name,
            ImmutableList.of(),
            ImmutableList.of(),
            instanceIp,
            ImmutableMap.of(),
            Status.RUNNING);
    List<Configuration> cfgNodes =
        _loadBalancer.toConfigurationNodes(
            new ConvertedConfiguration(
                new HashMap<>(),
                new HashSet<>(),
                ImmutableMultimap.of(),
                ImmutableMultimap.of(),
                ImmutableMultimap.of(_loadBalancerArn, instance)),
            Region.builder("r1").build(),
            new Warnings());

    StaticRoute expectedRouteViaSubnet1 =
        toStaticRoute(
            instanceIp.toPrefix(),
            interfaceNameToRemote(Subnet.nodeName(_subnet1Name), NLB_INSTANCE_TARGETS_IFACE_SUFFIX),
            AwsConfiguration.LINK_LOCAL_IP);
    StaticRoute expectedRouteViaSubnet2 =
        toStaticRoute(
            instanceIp.toPrefix(),
            interfaceNameToRemote(Subnet.nodeName(_subnet2Name), NLB_INSTANCE_TARGETS_IFACE_SUFFIX),
            AwsConfiguration.LINK_LOCAL_IP);
    assertThat(
        cfgNodes,
        containsInAnyOrder(
            hasDefaultVrf(hasStaticRoutes(contains(expectedRouteViaSubnet1))),
            hasDefaultVrf(hasStaticRoutes(contains(expectedRouteViaSubnet2)))));
  }
}

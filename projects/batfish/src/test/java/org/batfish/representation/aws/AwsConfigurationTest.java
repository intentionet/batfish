package org.batfish.representation.aws;

import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.representation.aws.AwsConfiguration.AWS_SERVICES_FACING_INTERFACE_NAME;
import static org.batfish.representation.aws.AwsConfiguration.AWS_SERVICES_GATEWAY_EXPORT_POLICY_NAME;
import static org.batfish.representation.aws.AwsConfiguration.AWS_SERVICES_GATEWAY_NODE_NAME;
import static org.batfish.representation.aws.AwsConfiguration.BACKBONE_FACING_INTERFACE_NAME;
import static org.batfish.representation.aws.AwsConfiguration.LINK_LOCAL_IP;
import static org.batfish.representation.aws.AwsConfiguration.generateAwsServicesGateway;
import static org.batfish.representation.aws.Utils.toStaticRoute;
import static org.batfish.specifier.Location.interfaceLinkLocation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.representation.aws.Instance.Status;
import org.batfish.representation.aws.LoadBalancer.AvailabilityZone;
import org.batfish.representation.aws.LoadBalancer.Protocol;
import org.batfish.representation.aws.LoadBalancer.Scheme;
import org.batfish.representation.aws.LoadBalancer.Type;
import org.batfish.representation.aws.LoadBalancerTargetHealth.HealthState;
import org.batfish.representation.aws.LoadBalancerTargetHealth.TargetHealth;
import org.batfish.representation.aws.LoadBalancerTargetHealth.TargetHealthDescription;
import org.batfish.specifier.LocationInfo;
import org.junit.Test;

public class AwsConfigurationTest {

  /** Test that nothing is created if we have no data */
  @Test
  public void testToVendorConfigurations_noData() {
    AwsConfiguration awsConfiguration = new AwsConfiguration();
    List<Configuration> c = awsConfiguration.toVendorIndependentConfigurations();
    assertThat(c, equalTo(ImmutableList.of()));
  }

  /** Test that the AWS services gateway node is created if we have an account */
  @Test
  public void testToVendorConfigurations_awsServicesGatewayNode() {
    AwsConfiguration awsConfiguration = new AwsConfiguration();
    awsConfiguration.addOrGetAccount("123");
    List<Configuration> c = awsConfiguration.toVendorIndependentConfigurations();
    assertThat(c, contains(hasHostname(AWS_SERVICES_GATEWAY_NODE_NAME)));
  }

  @Test
  public void testPopulatePrecomputedMaps() {
    AwsConfiguration c = new AwsConfiguration();
    Account account = c.addOrGetAccount("123");
    String vpcId = "vpc";
    String noInstanceZoneName = "zone1";
    String instanceZoneName = "zone2";
    String noInstanceSubnetId = "subnet1";
    String instanceSubnetId = "subnet2";
    String instanceId = "instance";
    String lbArn = "lbArn";
    String tgArn = "tgArn";
    Vpc vpc = new Vpc(vpcId, ImmutableSet.of(), ImmutableMap.of());
    AvailabilityZone noInstanceZone = new AvailabilityZone(noInstanceSubnetId, noInstanceZoneName);
    AvailabilityZone instanceZone = new AvailabilityZone(instanceSubnetId, instanceZoneName);
    Prefix prefix1 = Prefix.parse("1.1.1.0/24");
    Prefix prefix2 = Prefix.parse("2.2.2.0/24");
    Subnet noInstanceSubnet =
        new Subnet(prefix1, noInstanceSubnetId, vpcId, noInstanceZoneName, ImmutableMap.of());
    Subnet instanceSubnet =
        new Subnet(prefix2, instanceSubnetId, vpcId, instanceZoneName, ImmutableMap.of());
    Instance instance =
        new Instance(
            instanceId,
            vpcId,
            instanceSubnetId,
            ImmutableList.of(),
            ImmutableList.of(),
            Ip.parse("3.3.3.3"),
            ImmutableMap.of(),
            Status.RUNNING);

    TargetGroup targetGroup =
        new TargetGroup(
            tgArn, ImmutableList.of(lbArn), Protocol.TCP, 80, "tgName", TargetGroup.Type.INSTANCE);
    TargetHealthDescription targetHealthDescription =
        new TargetHealthDescription(
            new LoadBalancerTarget(lbArn, instanceId, 80), new TargetHealth(HealthState.HEALTHY));
    LoadBalancerTargetHealth targetHealth =
        new LoadBalancerTargetHealth("tgArn", ImmutableList.of(targetHealthDescription));

    // Set up region with everything except load balancers
    Region.RegionBuilder regionBuilder =
        Region.builder("region")
            .setInstances(ImmutableMap.of(instanceId, instance))
            .setSubnets(
                ImmutableMap.of(
                    noInstanceSubnetId, noInstanceSubnet, instanceSubnetId, instanceSubnet))
            .setVpcs(ImmutableMap.of(vpcId, vpc))
            .setLoadBalancerTargetHealths(ImmutableMap.of(tgArn, targetHealth))
            .setTargetGroups(ImmutableMap.of(tgArn, targetGroup));

    {
      // Network load balancer: Maps should get populated
      LoadBalancer nlb =
          new LoadBalancer(
              lbArn,
              ImmutableList.of(instanceZone, noInstanceZone),
              "lbDnsName",
              "lbName",
              Scheme.INTERNET_FACING,
              Type.NETWORK,
              "vpc");
      account.addRegion(regionBuilder.setLoadBalancers(ImmutableMap.of(lbArn, nlb)).build());

      c.populatePrecomputedMaps();
      assertThat(
          c.getSubnetsToInstanceTargets(),
          equalTo(ImmutableMultimap.of(instanceSubnetId, instance)));
      assertThat(
          c.getSubnetsToNlbs(),
          equalTo(ImmutableMultimap.of(noInstanceSubnetId, nlb, instanceSubnetId, nlb)));
      assertThat(c.getNlbsToInstanceTargets(), equalTo(ImmutableMultimap.of(lbArn, instance)));
    }
    {
      // Application load balancer (not supported): Maps should not get populated
      LoadBalancer alb =
          new LoadBalancer(
              lbArn,
              ImmutableList.of(instanceZone, noInstanceZone),
              "lbDnsName",
              "lbName",
              Scheme.INTERNET_FACING,
              Type.APPLICATION,
              "vpc");
      account.addRegion(regionBuilder.setLoadBalancers(ImmutableMap.of(lbArn, alb)).build());

      c.populatePrecomputedMaps();
      assertTrue(c.getSubnetsToInstanceTargets().isEmpty());
      assertTrue(c.getSubnetsToNlbs().isEmpty());
      assertTrue(c.getNlbsToInstanceTargets().isEmpty());
    }
  }

  @Test
  public void testGenerateAwsServicesGateway() {
    Configuration cfg = generateAwsServicesGateway();

    assertThat(cfg.getDeviceModel(), equalTo(DeviceModel.AWS_SERVICES_GATEWAY));

    // interfaces are present with firewall sessions
    assertThat(
        cfg.getAllInterfaces().keySet(),
        containsInAnyOrder(BACKBONE_FACING_INTERFACE_NAME, AWS_SERVICES_FACING_INTERFACE_NAME));
    assertThat(
        cfg.getAllInterfaces()
            .get(AWS_SERVICES_FACING_INTERFACE_NAME)
            .getFirewallSessionInterfaceInfo(),
        notNullValue());
    assertThat(
        cfg.getAllInterfaces()
            .get(BACKBONE_FACING_INTERFACE_NAME)
            .getFirewallSessionInterfaceInfo(),
        notNullValue());

    // static routes are defined
    Set<Prefix> awsServicesPrefixes = AwsPrefixes.getAwsServicesPrefixes();
    assertThat(
        cfg.getDefaultVrf().getStaticRoutes(),
        equalTo(
            awsServicesPrefixes.stream()
                .map(prefix -> toStaticRoute(prefix, AWS_SERVICES_FACING_INTERFACE_NAME))
                .collect(Collectors.toSet())));

    // location info is defined
    assertThat(
        cfg.getLocationInfo()
            .get(
                interfaceLinkLocation(
                    cfg.getAllInterfaces().get(AWS_SERVICES_FACING_INTERFACE_NAME))),
        equalTo(
            new LocationInfo(
                true,
                IpWildcardSetIpSpace.builder()
                    .including(
                        awsServicesPrefixes.stream()
                            .map(IpWildcard::create)
                            .collect(Collectors.toList()))
                    .build(),
                // using LINK_LOCAL_IP gets us EXITS_NETWORK as disposition for service prefixes
                LINK_LOCAL_IP.toIpSpace())));

    // bgp policy has been created
    assertTrue(cfg.getRoutingPolicies().containsKey(AWS_SERVICES_GATEWAY_EXPORT_POLICY_NAME));
  }
}

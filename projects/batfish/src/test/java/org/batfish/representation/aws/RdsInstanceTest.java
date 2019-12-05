package org.batfish.representation.aws;

import static org.batfish.datamodel.matchers.IpAccessListLineMatchers.hasMatchCondition;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.hasLines;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_DB_INSTANCES;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.aws.RdsInstance.Status;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RdsInstanceTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  private StaticRoute.Builder _staticRouteBuilder;

  @Before
  public void setup() {
    _staticRouteBuilder =
        StaticRoute.builder()
            .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
            .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
            .setNetwork(Prefix.ZERO);
  }

  public Map<String, Configuration> loadAwsConfigurations() throws IOException {
    List<String> awsFilenames =
        ImmutableList.of(
            "RdsInstances.json",
            "SecurityGroups.json",
            "Subnets.json",
            "Vpcs.json",
            "NetworkAcls.json",
            "NetworkInterfaces.json",
            "Reservations.json");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setAwsText("org/batfish/representation/aws/test", awsFilenames)
                .build(),
            _folder);
    return batfish.loadConfigurations();
  }

  @Test
  public void testRdsSubnetEdge() throws IOException {
    Map<String, Configuration> configurations = loadAwsConfigurations();
    Topology topology = TopologyUtil.synthesizeL3Topology(configurations);

    // check that RDS instance is a neighbor of both  subnets in which its interfaces are
    assertThat(
        topology.getEdges(),
        hasItem(
            new Edge(
                NodeInterfacePair.of("subnet-073b8061", "subnet-073b8061"),
                NodeInterfacePair.of("test-rds", "test-rds-subnet-073b8061"))));
    assertThat(
        topology.getEdges(),
        hasItem(
            new Edge(
                NodeInterfacePair.of("subnet-1f315846", "subnet-1f315846"),
                NodeInterfacePair.of("test-rds", "test-rds-subnet-1f315846"))));
  }

  @Test
  public void testUniqueIps() throws IOException {
    Map<String, Configuration> configurations = loadAwsConfigurations();

    // check that  IPs are unique for all the interfaces
    List<Ip> ipsAsList =
        configurations.values().stream()
            .map(Configuration::getAllInterfaces)
            .map(Map::values)
            .flatMap(Collection::stream)
            .map(Interface::getAllConcreteAddresses)
            .flatMap(Collection::stream)
            .map(ConcreteInterfaceAddress::getIp)
            .collect(ImmutableList.toImmutableList());
    Set<Ip> ipsAsSet = ImmutableSet.copyOf(ipsAsList);
    assertThat(ipsAsList, hasSize(ipsAsSet.size()));
  }

  @Test
  public void testDefaultRoute() throws IOException {
    Map<String, Configuration> configurations = loadAwsConfigurations();
    StaticRoute defaultRoute1 = _staticRouteBuilder.setNextHopIp(Ip.parse("172.31.0.1")).build();
    StaticRoute defaultRoute2 = _staticRouteBuilder.setNextHopIp(Ip.parse("192.168.2.17")).build();

    // checking that both default routes exist(to both the subnets) in RDS instance
    assertThat(configurations, hasKey("test-rds"));
    assertThat(
        configurations.get("test-rds").getDefaultVrf().getStaticRoutes(),
        containsInAnyOrder(defaultRoute1, defaultRoute2));
  }

  @Test
  public void testSecurityGroupsAcl() throws IOException {
    Map<String, Configuration> configurations = loadAwsConfigurations();

    assertThat(configurations, hasKey("test-rds"));
    assertThat(configurations.get("test-rds").getAllInterfaces().entrySet(), hasSize(2));

    for (Interface iface : configurations.get("test-rds").getAllInterfaces().values()) {
      assertThat(
          iface.getOutgoingFilter(),
          hasLines(
              hasMatchCondition(
                  new MatchHeaderSpace(
                      HeaderSpace.builder()
                          .setDstIps(Sets.newHashSet(IpWildcard.parse("0.0.0.0/0")))
                          .build()))));
      assertThat(
          iface.getIncomingFilter(),
          hasLines(
              hasMatchCondition(
                  new MatchHeaderSpace(
                      HeaderSpace.builder()
                          .setIpProtocols(Sets.newHashSet(IpProtocol.TCP))
                          .setSrcIps(
                              Sets.newHashSet(
                                  IpWildcard.parse("1.2.3.4/32"),
                                  IpWildcard.parse("10.193.16.105/32"),
                                  IpWildcard.parse("54.191.107.22")))
                          .setDstPorts(Sets.newHashSet(new SubRange(45, 50)))
                          .build()))));
      assertThat(
          iface.getFirewallSessionInterfaceInfo(),
          equalTo(
              new FirewallSessionInterfaceInfo(
                  false, ImmutableList.of(iface.getName()), null, null)));
    }
  }

  @Test
  public void testDeserialization() throws IOException {
    String text = CommonUtil.readResource("org/batfish/representation/aws/RdsInstanceTest.json");

    JsonNode json = BatfishObjectMapper.mapper().readTree(text);
    ArrayNode array = (ArrayNode) json.get(JSON_KEY_DB_INSTANCES);
    List<RdsInstance> rdsInstances = new LinkedList<>();

    for (int index = 0; index < array.size(); index++) {
      rdsInstances.add(
          BatfishObjectMapper.mapper().convertValue(array.get(index), RdsInstance.class));
    }

    assertThat(
        rdsInstances,
        equalTo(
            ImmutableList.of(
                new RdsInstance(
                    "test-db",
                    "us-west-2b",
                    "vpc-1",
                    false,
                    Status.AVAILABLE,
                    ImmutableListMultimap.of("us-west-2b", "subnet-1"),
                    ImmutableList.of("sg-12345")))));
  }
}

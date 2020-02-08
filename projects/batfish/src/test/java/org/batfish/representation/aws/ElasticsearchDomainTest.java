package org.batfish.representation.aws;

import static org.batfish.datamodel.IpProtocol.TCP;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.or;
import static org.batfish.datamodel.matchers.AclLineMatchers.isExprAclLineThat;
import static org.batfish.datamodel.matchers.ExprAclLineMatchers.hasMatchCondition;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.hasLines;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_DOMAIN_STATUS_LIST;
import static org.batfish.representation.aws.Utils.traceElementForAddress;
import static org.batfish.representation.aws.Utils.traceElementForDstPorts;
import static org.batfish.representation.aws.Utils.traceElementForInstance;
import static org.batfish.representation.aws.Utils.traceElementForProtocol;
import static org.batfish.representation.aws.Utils.traceTextForAddress;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AclAclLine;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.aws.IpPermissions.AddressType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link ElasticsearchDomain} */
public class ElasticsearchDomainTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  private StaticRoute.Builder _staticRouteBuilder;

  public static final MatchHeaderSpace matchTcp =
      new MatchHeaderSpace(
          HeaderSpace.builder().setIpProtocols(TCP).build(), traceElementForProtocol(TCP));

  public static MatchHeaderSpace matchPorts(int fromPort, int toPort) {
    return new MatchHeaderSpace(
        HeaderSpace.builder().setDstPorts(new SubRange(fromPort, toPort)).build(),
        traceElementForDstPorts(fromPort, toPort));
  }

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
            "ElasticsearchDomains.json",
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
    return batfish.loadConfigurations(batfish.getSnapshot());
  }

  @Test
  public void testEsSubnetEdge() throws IOException {
    Map<String, Configuration> configurations = loadAwsConfigurations();
    Topology topology = TopologyUtil.synthesizeL3Topology(configurations);

    // check that ES instance is a neighbor of both  subnets in which its interfaces are
    assertThat(
        topology.getEdges(),
        hasItem(
            new Edge(
                NodeInterfacePair.of("subnet-073b8061", "subnet-073b8061"),
                NodeInterfacePair.of("es-domain", "es-domain-subnet-073b8061"))));
    assertThat(
        topology.getEdges(),
        hasItem(
            new Edge(
                NodeInterfacePair.of("subnet-1f315846", "subnet-1f315846"),
                NodeInterfacePair.of("es-domain", "es-domain-subnet-1f315846"))));
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
    assertThat(configurations, hasKey("es-domain"));
    assertThat(
        configurations.get("es-domain").getDefaultVrf().getStaticRoutes(),
        containsInAnyOrder(defaultRoute1, defaultRoute2));
  }

  @Test
  public void testDeserialization() throws IOException {
    String text =
        CommonUtil.readResource("org/batfish/representation/aws/ElasticsearchDomainTest.json");

    JsonNode json = BatfishObjectMapper.mapper().readTree(text);
    ArrayNode array = (ArrayNode) json.get(JSON_KEY_DOMAIN_STATUS_LIST);
    List<ElasticsearchDomain> domains = new LinkedList<>();

    for (int index = 0; index < array.size(); index++) {
      domains.add(
          BatfishObjectMapper.mapper().convertValue(array.get(index), ElasticsearchDomain.class));
    }

    assertThat(
        domains,
        equalTo(
            ImmutableList.of(
                new ElasticsearchDomain(
                    "es-domain",
                    "vpc-b390fad5",
                    ImmutableList.of("sg-55510831"),
                    ImmutableList.of("subnet-7044ff16"),
                    true))));
  }

  @Test
  public void testSecurityGroupsAcl() throws IOException {
    Map<String, Configuration> configurations = loadAwsConfigurations();

    assertThat(configurations, hasKey("es-domain"));
    Configuration esDomain = configurations.get("es-domain");
    assertThat(esDomain.getAllInterfaces().entrySet(), hasSize(2));
    assertThat(
        esDomain
            .getIpAccessLists()
            .get("~EGRESS~SECURITY-GROUP~Test Security Group~sg-0de0ddfa8a5a45810~"),
        hasLines(
            isExprAclLineThat(
                hasMatchCondition(
                    new MatchHeaderSpace(
                        HeaderSpace.builder().setDstIps(UniverseIpSpace.INSTANCE).build(),
                        traceElementForAddress(
                            "destination", "0.0.0.0/0", AddressType.CIDR_IP))))));
    assertThat(
        esDomain
            .getIpAccessLists()
            .get("~INGRESS~SECURITY-GROUP~Test Security Group~sg-0de0ddfa8a5a45810~"),
        hasLines(
            containsInAnyOrder(
                isExprAclLineThat(
                    hasMatchCondition(
                        and(
                            matchTcp,
                            matchPorts(45, 50),
                            new MatchHeaderSpace(
                                HeaderSpace.builder()
                                    .setSrcIps(Ip.parse("1.2.3.4").toIpSpace())
                                    .build(),
                                traceElementForAddress(
                                    "source", "1.2.3.4/32", AddressType.CIDR_IP))))),
                isExprAclLineThat(
                    hasMatchCondition(
                        and(
                            matchTcp,
                            matchPorts(45, 50),
                            or(
                                traceTextForAddress(
                                    "source", "Test-Instance-SG", AddressType.SECURITY_GROUP),
                                new MatchHeaderSpace(
                                    HeaderSpace.builder()
                                        .setSrcIps(Ip.parse("10.193.16.105").toIpSpace())
                                        .build(),
                                    traceElementForInstance(
                                        "Test host (i-066b1b9957b9200e7)")))))))));
    assertThat(
        esDomain.getIpAccessLists().get("~SECURITY_GROUP_INGRESS_ACL~").getLines(),
        equalTo(
            ImmutableList.of(
                new AclAclLine(
                    "Security Group Test Security Group",
                    "~INGRESS~SECURITY-GROUP~Test Security Group~sg-0de0ddfa8a5a45810~",
                    Utils.getTraceElementForSecurityGroup("Test Security Group")))));
    assertThat(
        esDomain.getIpAccessLists().get("~SECURITY_GROUP_EGRESS_ACL~").getLines(),
        equalTo(
            ImmutableList.of(
                new AclAclLine(
                    "Security Group Test Security Group",
                    "~EGRESS~SECURITY-GROUP~Test Security Group~" + "sg-0de0ddfa8a5a45810~",
                    Utils.getTraceElementForSecurityGroup("Test Security Group")))));
    for (Interface iface : esDomain.getAllInterfaces().values()) {
      assertThat(iface.getIncomingFilter().getName(), equalTo("~SECURITY_GROUP_INGRESS_ACL~"));
      assertThat(iface.getOutgoingFilter().getName(), equalTo("~SECURITY_GROUP_EGRESS_ACL~"));
      assertThat(
          iface.getFirewallSessionInterfaceInfo(),
          equalTo(
              new FirewallSessionInterfaceInfo(
                  false, ImmutableList.of(iface.getName()), null, null)));
    }
  }
}

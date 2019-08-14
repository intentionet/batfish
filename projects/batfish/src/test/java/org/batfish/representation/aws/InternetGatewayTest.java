package org.batfish.representation.aws;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_INTERNET_GATEWAYS;
import static org.batfish.representation.aws.InternetGateway.AWS_BACKBONE_AS;
import static org.batfish.representation.aws.InternetGateway.AWS_INTERNET_GATEWAY_AS;
import static org.batfish.representation.aws.InternetGateway.BACKBONE_EXPORT_POLICY_NAME;
import static org.batfish.representation.aws.InternetGateway.BACKBONE_INTERFACE_NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.common.util.IspModelingUtils;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.junit.Test;

/** Tests for {@link InternetGateway} */
public class InternetGatewayTest {

  @Test
  public void testDeserialization() throws IOException {
    String text =
        CommonUtil.readResource("org/batfish/representation/aws/InternetGatewayTest.json");

    JsonNode json = BatfishObjectMapper.mapper().readTree(text);
    ArrayNode gatewaysArray = (ArrayNode) json.get(JSON_KEY_INTERNET_GATEWAYS);
    List<InternetGateway> gateways = new LinkedList<>();

    for (int index = 0; index < gatewaysArray.size(); index++) {
      gateways.add(
          BatfishObjectMapper.mapper()
              .convertValue(gatewaysArray.get(index), InternetGateway.class));
    }

    assertThat(
        gateways,
        equalTo(
            ImmutableList.of(
                new InternetGateway("igw-fac5839d", ImmutableList.of("vpc-925131f4")))));
  }

  @Test
  public void testToConfiguration() {

    Vpc vpc = new Vpc("vpc", ImmutableSet.of());
    Configuration vpcConfig = Utils.newAwsConfiguration(vpc.getId(), "awstest");

    Ip publicIp = Ip.parse("1.1.1.1");

    NetworkInterface ni =
        new NetworkInterface(
            "ni",
            "subnet",
            vpc.getId(),
            ImmutableList.of(),
            ImmutableList.of(new PrivateIpAddress(true, Ip.parse("10.10.10.10"), publicIp)),
            "desc",
            null);

    InternetGateway internetGateway = new InternetGateway("igw", ImmutableList.of(vpc.getId()));

    Region region =
        Region.builder("region")
            .setInternetGateways(ImmutableMap.of(internetGateway.getId(), internetGateway))
            .setVpcs(ImmutableMap.of(vpc.getId(), vpc))
            .setNetworkInterfaces(ImmutableMap.of(ni.getId(), ni))
            .build();

    AwsConfiguration awsConfiguration =
        new AwsConfiguration(
            ImmutableMap.of(region.getName(), region),
            ImmutableMap.of(vpcConfig.getHostname(), vpcConfig));

    Configuration igwConfig = internetGateway.toConfigurationNode(awsConfiguration, region);

    // gateway should have interfaces to the backbone and vpc
    assertThat(
        igwConfig.getAllInterfaces().values().stream()
            .map(i -> i.getName())
            .collect(ImmutableList.toImmutableList()),
        equalTo(ImmutableList.of(BACKBONE_INTERFACE_NAME)));

    Prefix bbInterfacePrefix =
        igwConfig.getAllInterfaces().get(BACKBONE_INTERFACE_NAME).getConcreteAddress().getPrefix();

    assertTrue(igwConfig.getAllInterfaces().containsKey(BACKBONE_INTERFACE_NAME));
    assertThat(
        igwConfig.getDefaultVrf().getBgpProcess().getRouterId(),
        equalTo(bbInterfacePrefix.getStartIp()));

    assertThat(
        igwConfig.getRoutingPolicies().get(BACKBONE_EXPORT_POLICY_NAME).getStatements(),
        equalTo(
            Collections.singletonList(
                IspModelingUtils.getAdvertiseStaticStatement(
                    new PrefixSpace(
                        PrefixRange.fromPrefix(
                            Prefix.create(publicIp, Prefix.MAX_PREFIX_LENGTH)))))));

    BgpActivePeerConfig nbr =
        getOnlyElement(igwConfig.getDefaultVrf().getBgpProcess().getActiveNeighbors().values());
    assertThat(
        nbr,
        equalTo(
            BgpActivePeerConfig.builder()
                .setLocalIp(bbInterfacePrefix.getStartIp())
                .setLocalAs(AWS_INTERNET_GATEWAY_AS)
                .setRemoteAs(AWS_BACKBONE_AS)
                .setPeerAddress(bbInterfacePrefix.getEndIp())
                .setIpv4UnicastAddressFamily(
                    Ipv4UnicastAddressFamily.builder()
                        .setExportPolicy(BACKBONE_EXPORT_POLICY_NAME)
                        .build())
                .build()));
  }
}

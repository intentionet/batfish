package org.batfish.representation.aws;

import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVrfName;
import static org.batfish.representation.aws.TransitGateway.createBgpProcess;
import static org.batfish.representation.aws.TransitGatewayAttachment.STATE_ASSOCIATED;
import static org.batfish.representation.aws.Utils.suffixedInterfaceName;
import static org.batfish.representation.aws.Utils.toStaticRoute;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import org.batfish.common.Warnings;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.representation.aws.TransitGateway.TransitGatewayOptions;
import org.batfish.representation.aws.TransitGatewayAttachment.Association;
import org.batfish.representation.aws.TransitGatewayAttachment.ResourceType;
import org.batfish.representation.aws.VpnConnection.GatewayType;
import org.junit.Test;

/** Tests for {@link TransitGateway} */
public class TransitGatewayTest {

  @Test
  public void testDeserialization() throws IOException {
    String text = CommonUtil.readResource("org/batfish/representation/aws/TransitGatewayTest.json");

    JsonNode json = BatfishObjectMapper.mapper().readTree(text);
    Region region = new Region("r1");
    region.addConfigElement(json, null, null);

    /*
     * Only the available gateway should show up.
     */
    assertThat(
        region.getTransitGateways(),
        equalTo(
            ImmutableMap.of(
                "tgw-044be4464fcc69aff",
                new TransitGateway(
                    "tgw-044be4464fcc69aff",
                    new TransitGatewayOptions(
                        64512L,
                        true,
                        "tgw-rtb-0fa40c8df355dce6e",
                        true,
                        "tgw-rtb-0fa40c8df355dce6e",
                        true)))));
  }

  @Test
  public void testToConfigurationNodesVpcAttachment() {

    String routeTableId = "tgw-rtb";
    TransitGateway tgw =
        new TransitGateway(
            "tgw", new TransitGatewayOptions(0L, true, routeTableId, true, "tgw-rtb", true));

    Prefix vpcPrefix = Prefix.parse("3.3.3.0/24");
    Vpc vpc = new Vpc("vpc", ImmutableSet.of(vpcPrefix));
    Configuration vpcCfg = Utils.newAwsConfiguration(Vpc.nodeName(vpc.getId()), "aws");

    TransitGatewayAttachment tgwAttachment =
        new TransitGatewayAttachment(
            "tgw-attach",
            tgw.getId(),
            ResourceType.VPC,
            vpc.getId(),
            new Association(routeTableId, STATE_ASSOCIATED));

    TransitGatewayVpcAttachment vpcAttachment =
        new TransitGatewayVpcAttachment(
            tgwAttachment.getId(), tgw.getId(), vpc.getId(), ImmutableList.of());

    Region region =
        Region.builder("region")
            .setTransitGateways(ImmutableMap.of(tgw.getId(), tgw))
            .setTransitGatewayAttachments(ImmutableMap.of(tgwAttachment.getId(), tgwAttachment))
            .setTransitGatewayVpcAttachments(ImmutableMap.of(vpcAttachment.getId(), vpcAttachment))
            .setVpcs(ImmutableMap.of(vpc.getId(), vpc))
            .build();

    ConvertedConfiguration awsConfiguration =
        new ConvertedConfiguration(ImmutableMap.of(vpcCfg.getHostname(), vpcCfg));

    Warnings warnings = new Warnings(true, true, true);
    Configuration tgwCfg = tgw.toConfigurationNode(awsConfiguration, region, warnings);

    // check that vrfs exist
    assertTrue(tgwCfg.getVrfs().containsKey(TransitGateway.vrfNameForRouteTable(routeTableId)));
    assertTrue(vpcCfg.getVrfs().containsKey(Vpc.vrfNameForLink(tgwAttachment.getId())));

    // check that interfaces are created in the right VRFs
    Interface tgwInterface =
        tgwCfg.getAllInterfaces().get(suffixedInterfaceName(vpcCfg, tgwAttachment.getId()));
    Interface vpcInterface =
        vpcCfg.getAllInterfaces().get(suffixedInterfaceName(tgwCfg, tgwAttachment.getId()));
    assertThat(tgwInterface, hasVrfName(TransitGateway.vrfNameForRouteTable(routeTableId)));
    assertThat(vpcInterface, hasVrfName(Vpc.vrfNameForLink(tgwAttachment.getId())));

    // check that VRFs have the right static routes
    assertThat(
        tgwCfg.getVrfs().get(TransitGateway.vrfNameForRouteTable(routeTableId)).getStaticRoutes(),
        equalTo(
            ImmutableSet.of(toStaticRoute(vpcPrefix, vpcInterface.getConcreteAddress().getIp()))));
    assertThat(
        vpcCfg.getVrfs().get(Vpc.vrfNameForLink(tgwAttachment.getId())).getStaticRoutes(),
        equalTo(
            ImmutableSet.of(
                toStaticRoute(vpcPrefix, NULL_INTERFACE_NAME),
                toStaticRoute(Prefix.ZERO, tgwInterface.getConcreteAddress().getIp()))));
  }

  @Test
  public void testToConfigurationNodesVpnAttachment() {

    String routeTableId = "tgw-rtb";
    TransitGateway tgw =
        new TransitGateway(
            "tgw", new TransitGatewayOptions(0L, true, routeTableId, true, "tgw-rtb", true));

    IpsecTunnel ipsecTunnel =
        new IpsecTunnel(
            65301L,
            Ip.parse("169.254.15.194"),
            30,
            Ip.parse("147.75.69.27"),
            "sha1",
            "aes-128-cbc",
            28800,
            "main",
            "group2",
            "7db2fd6e9dcffcf826743b57bc0518cfcbca8f4db0b80a7a2c3f0c3b09deb49a",
            "hmac-sha1-96",
            "aes-128-cbc",
            3600,
            "tunnel",
            "group2",
            "esp",
            65401L,
            Ip.parse("169.254.15.193"),
            30,
            Ip.parse("52.27.166.152"));

    VpnConnection vpnConnection =
        new VpnConnection(
            true,
            "vpn",
            "cgw-fb76ace5",
            GatewayType.VPN,
            tgw.getId(),
            ImmutableList.of(ipsecTunnel),
            ImmutableList.of(),
            ImmutableList.of(),
            false);

    TransitGatewayAttachment tgwAttachment =
        new TransitGatewayAttachment(
            "tgw-attach",
            tgw.getId(),
            ResourceType.VPN,
            vpnConnection.getId(),
            new Association(routeTableId, STATE_ASSOCIATED));

    Region region =
        Region.builder("region")
            .setTransitGateways(ImmutableMap.of(tgw.getId(), tgw))
            .setTransitGatewayAttachments(ImmutableMap.of(tgwAttachment.getId(), tgwAttachment))
            .setVpnConnections(ImmutableMap.of(vpnConnection.getId(), vpnConnection))
            .build();

    ConvertedConfiguration awsConfiguration = new ConvertedConfiguration();

    Warnings warnings = new Warnings(true, true, true);
    Configuration tgwCfg = tgw.toConfigurationNode(awsConfiguration, region, warnings);

    // check that the vrf exists
    assertTrue(tgwCfg.getVrfs().containsKey(TransitGateway.vrfNameForRouteTable(routeTableId)));

    // check that BGP process was created
    assertThat(
        tgwCfg.getVrfs().get(TransitGateway.vrfNameForRouteTable(routeTableId)).getBgpProcess(),
        notNullValue());

    // if applyGateway was called with the right params, such an interface must exist
    Interface tgwInterface =
        tgwCfg
            .getAllInterfaces()
            .get(
                VpnConnection.getExternalInterfaceName(
                    VpnConnection.getTunnelId(vpnConnection.getId(), 1)));
    assertThat(tgwInterface, hasVrfName(TransitGateway.vrfNameForRouteTable(routeTableId)));
  }

  @Test
  public void testCreateBgpProcess() {
    Configuration tgwCfg = Utils.newAwsConfiguration("tgw", "local");
    Vrf vrf = Vrf.builder().setOwner(tgwCfg).setName("vrf").build();
    ConvertedConfiguration awsConfiguration = new ConvertedConfiguration();

    createBgpProcess(tgwCfg, vrf, awsConfiguration);

    // interface exists
    Interface bgpInterface = tgwCfg.getAllInterfaces().get("bgp-loopback- + vrf.getName()");
    assertThat(bgpInterface, hasVrfName(vrf.getName()));

    assertThat(vrf.getBgpProcess(), notNullValue());

    // TODO: check on routing policies
  }
}

package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;

/** Represents an AWS VPN gateway */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
final class VpnGateway implements AwsVpcEntity, Serializable {

  @JsonIgnoreProperties(ignoreUnknown = true)
  @ParametersAreNonnullByDefault
  private static class VpcAttachment {

    @Nonnull private final String _vpcId;

    @JsonCreator
    private static VpcAttachment create(@Nullable @JsonProperty(JSON_KEY_VPC_ID) String vpcId) {
      checkArgument(vpcId != null, "Vpc id cannot be null for VPN attachment");
      return new VpcAttachment(vpcId);
    }

    private VpcAttachment(String vpcId) {
      _vpcId = vpcId;
    }

    @Nonnull
    String getVpcId() {
      return _vpcId;
    }
  }

  @Nonnull private final List<String> _attachmentVpcIds;

  @Nonnull private final String _vpnGatewayId;

  @JsonCreator
  private static VpnGateway create(
      @Nullable @JsonProperty(JSON_KEY_VPN_GATEWAY_ID) String vpnGatewayId,
      @Nullable @JsonProperty(JSON_KEY_VPC_ATTACHMENTS) List<VpcAttachment> vpcAttachments) {
    checkArgument(vpnGatewayId != null, "Id cannot be null for VPC gateway");
    checkArgument(vpcAttachments != null, "Vpc attachments cannot be nul for VPN gateway");

    return new VpnGateway(
        vpnGatewayId,
        vpcAttachments.stream()
            .map(VpcAttachment::getVpcId)
            .collect(ImmutableList.toImmutableList()));
  }

  VpnGateway(String vpnGatewayId, List<String> attachmentVpcIds) {
    _vpnGatewayId = vpnGatewayId;
    _attachmentVpcIds = attachmentVpcIds;
  }

  @Nonnull
  List<String> getAttachmentVpcIds() {
    return _attachmentVpcIds;
  }

  @Override
  public String getId() {
    return _vpnGatewayId;
  }

  Configuration toConfigurationNode(
      AwsConfiguration awsConfiguration, Region region, Warnings warnings) {
    Configuration cfgNode = Utils.newAwsConfiguration(_vpnGatewayId, "aws");
    cfgNode.getVendorFamily().getAws().setRegion(region.getName());

    for (String vpcId : _attachmentVpcIds) {

      String vgwIfaceName = vpcId;
      Prefix vpcLink = awsConfiguration.getNextGeneratedLinkSubnet();
      ConcreteInterfaceAddress vgwIfaceAddress =
          ConcreteInterfaceAddress.create(vpcLink.getStartIp(), vpcLink.getPrefixLength());
      Utils.newInterface(vgwIfaceName, cfgNode, vgwIfaceAddress, "To VPC " + vpcId);

      // add the interface to the vpc router
      Configuration vpcConfigNode = awsConfiguration.getConfigurationNodes().get(vpcId);
      String vpcIfaceName = _vpnGatewayId;
      Interface vpcIface =
          Interface.builder().setName(vpcIfaceName).setOwner(vpcConfigNode).build();
      ConcreteInterfaceAddress vpcIfaceAddress =
          ConcreteInterfaceAddress.create(vpcLink.getEndIp(), vpcLink.getPrefixLength());
      vpcIface.setAddress(vpcIfaceAddress);
      Utils.newInterface(
          vpcIfaceName, vpcConfigNode, vpcIfaceAddress, "To VPN gateway " + _vpnGatewayId);

      // add a route on the gateway to the vpc
      Vpc vpc = region.getVpcs().get(vpcId);
      vpc.getCidrBlockAssociations()
          .forEach(
              prefix -> {
                StaticRoute vgwVpcRoute =
                    StaticRoute.builder()
                        .setNetwork(prefix)
                        .setNextHopIp(vpcIfaceAddress.getIp())
                        .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
                        .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
                        .build();
                cfgNode.getDefaultVrf().getStaticRoutes().add(vgwVpcRoute);
              });
    }

    return cfgNode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VpnGateway)) {
      return false;
    }
    VpnGateway that = (VpnGateway) o;
    return Objects.equals(_attachmentVpcIds, that._attachmentVpcIds)
        && Objects.equals(_vpnGatewayId, that._vpnGatewayId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_attachmentVpcIds, _vpnGatewayId);
  }
}

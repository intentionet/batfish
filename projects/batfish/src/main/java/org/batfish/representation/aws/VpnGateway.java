package org.batfish.representation.aws;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.common.util.isp.IspModelingUtils.installRoutingPolicyAdvertiseStatic;
import static org.batfish.representation.aws.AwsConfiguration.LINK_LOCAL_IP;
import static org.batfish.representation.aws.Utils.ACCEPT_ALL_BGP;
import static org.batfish.representation.aws.Utils.connectGatewayToVpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.LinkLocalAddress;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.routing_policy.RoutingPolicy;

/** Represents an AWS VPN gateway */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
final class VpnGateway implements AwsVpcEntity, Serializable {

  static final String VGW_EXPORT_POLICY_NAME = "~vgw~export-policy~";
  static final String VGW_IMPORT_POLICY_NAME = "~vgw~import-policy~";

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

  @Nonnull private final Map<String, String> _tags;

  @JsonCreator
  private static VpnGateway create(
      @Nullable @JsonProperty(JSON_KEY_VPN_GATEWAY_ID) String vpnGatewayId,
      @Nullable @JsonProperty(JSON_KEY_VPC_ATTACHMENTS) List<VpcAttachment> vpcAttachments,
      @Nullable @JsonProperty(JSON_KEY_TAGS) List<Tag> tags) {
    checkArgument(vpnGatewayId != null, "Id cannot be null for VPC gateway");
    checkArgument(vpcAttachments != null, "Vpc attachments cannot be null for VPN gateway");

    return new VpnGateway(
        vpnGatewayId,
        vpcAttachments.stream()
            .map(VpcAttachment::getVpcId)
            .collect(ImmutableList.toImmutableList()),
        firstNonNull(tags, ImmutableList.<Tag>of()).stream()
            .collect(ImmutableMap.toImmutableMap(Tag::getKey, Tag::getValue)));
  }

  VpnGateway(String vpnGatewayId, List<String> attachmentVpcIds, Map<String, String> tags) {
    _vpnGatewayId = vpnGatewayId;
    _attachmentVpcIds = attachmentVpcIds;
    _tags = tags;
  }

  @Nonnull
  List<String> getAttachmentVpcIds() {
    return _attachmentVpcIds;
  }

  @Override
  public String getId() {
    return _vpnGatewayId;
  }

  /**
   * Creates a node for the VPN gateway. Other essential elements of this node are created
   * elsewhere. During subnet processing, we create links to the subnet and also add static routes
   * to the subnet. During VPN connection processing, we create BGP processes on the node with the
   * right policy.
   */
  Configuration toConfigurationNode(
      ConvertedConfiguration awsConfiguration, Region region, Warnings warnings) {
    Configuration cfgNode =
        Utils.newAwsConfiguration(_vpnGatewayId, "aws", _tags, DeviceModel.AWS_VPN_GATEWAY);
    cfgNode.getVendorFamily().getAws().setRegion(region.getName());

    _attachmentVpcIds.forEach(
        vpcId ->
            connectGatewayToVpc(_vpnGatewayId, cfgNode, vpcId, awsConfiguration, region, warnings));

    // if this VGW has any BGP-based VPN connections, configure BGP on it
    boolean doBgp =
        region.getVpnConnections().values().stream()
            .filter(conn -> _vpnGatewayId.equals(conn.getAwsGatewayId()))
            .anyMatch(VpnConnection::isBgpConnection);

    if (doBgp) {
      String loopbackBgp = "loopbackBgp";
      LinkLocalAddress loopbackBgpAddress = LinkLocalAddress.of(LINK_LOCAL_IP);
      Utils.newInterface(loopbackBgp, cfgNode, loopbackBgpAddress, "BGP loopback");

      BgpProcess proc =
          BgpProcess.builder()
              .setRouterId(loopbackBgpAddress.getIp())
              .setVrf(cfgNode.getDefaultVrf())
              .setAdminCostsToVendorDefaults(ConfigurationFormat.AWS)
              .build();
      proc.setMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.EXACT_PATH);

      PrefixSpace originationSpace = new PrefixSpace();
      _attachmentVpcIds.stream()
          .flatMap(vpcId -> region.getVpcs().get(vpcId).getCidrBlockAssociations().stream())
          .forEach(originationSpace::addPrefix);

      installRoutingPolicyAdvertiseStatic(VGW_EXPORT_POLICY_NAME, cfgNode, originationSpace);

      RoutingPolicy.builder()
          .setName(VGW_IMPORT_POLICY_NAME)
          .setOwner(cfgNode)
          .setStatements(Collections.singletonList(ACCEPT_ALL_BGP))
          .build();
    }

    // process all VPN connections
    region.getVpnConnections().values().stream()
        .filter(c -> _vpnGatewayId.equals(c.getAwsGatewayId()))
        .forEach(
            c ->
                c.applyToGateway(
                    cfgNode,
                    cfgNode.getDefaultVrf(),
                    VGW_EXPORT_POLICY_NAME,
                    VGW_IMPORT_POLICY_NAME,
                    warnings));

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
        && Objects.equals(_tags, that._tags)
        && Objects.equals(_vpnGatewayId, that._vpnGatewayId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_attachmentVpcIds, _vpnGatewayId, _tags);
  }
}

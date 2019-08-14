package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.common.util.IspModelingUtils.installRoutingPolicyAdvertiseStatic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;

/**
 * Represents an AWS Internet Gateway
 * https://docs.aws.amazon.com/cli/latest/reference/ec2/describe-internet-gateways.html
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
final class InternetGateway implements AwsVpcEntity, Serializable {

  /** ASN to use for AWS backbone */
  static final long AWS_BACKBONE_AS = 16509L;

  /** ASN to use for AWS internet gateways */
  static final long AWS_INTERNET_GATEWAY_AS = 65534L;

  /** Name of the interface on the Internet Gateway that faces the backbone */
  static final String BACKBONE_INTERFACE_NAME = "backbone";

  /** Name of the routing policy on the Internet Gateway that faces the backbone */
  static final String BACKBONE_EXPORT_POLICY_NAME = "AwsInternetGatewayExportPolicy";

  @Nonnull private final List<String> _attachmentVpcIds;

  @Nonnull private String _internetGatewayId;

  @JsonIgnoreProperties(ignoreUnknown = true)
  @ParametersAreNonnullByDefault
  private static class Attachment {

    @Nonnull private final String _vpcId;

    @JsonCreator
    private static Attachment create(@Nullable @JsonProperty(JSON_KEY_VPC_ID) String vpcId) {
      checkArgument(vpcId != null, "Vpc id cannot be null for Internet gateway attachment");
      return new Attachment(vpcId);
    }

    private Attachment(String vpcId) {
      _vpcId = vpcId;
    }

    @Nonnull
    public String getVpcId() {
      return _vpcId;
    }
  }

  @JsonCreator
  private static InternetGateway create(
      @Nullable @JsonProperty(JSON_KEY_INTERNET_GATEWAY_ID) String internetGatewayId,
      @Nullable @JsonProperty(JSON_KEY_ATTACHMENTS) List<Attachment> attachments) {
    checkArgument(internetGatewayId != null, "Id cannot be null for Internet gateway");
    checkArgument(attachments != null, "Attachments cannot be nul for Internet gateway");

    return new InternetGateway(
        internetGatewayId,
        attachments.stream().map(Attachment::getVpcId).collect(ImmutableList.toImmutableList()));
  }

  public InternetGateway(String internetGatewayId, List<String> attachmentVpcIds) {
    _internetGatewayId = internetGatewayId;
    _attachmentVpcIds = attachmentVpcIds;
  }

  @Override
  public String getId() {
    return _internetGatewayId;
  }

  @Nonnull
  public List<String> getAttachmentVpcIds() {
    return _attachmentVpcIds;
  }

  Configuration toConfigurationNode(AwsConfiguration awsConfiguration, Region region) {
    Configuration cfgNode = Utils.newAwsConfiguration(_internetGatewayId, "aws");
    cfgNode.getVendorFamily().getAws().setRegion(region.getName());

    // Create an interface facing the backbone and run a BGP process that advertises static routes
    Prefix bbInterfaceSubnet = awsConfiguration.getNextGeneratedLinkSubnet();
    ConcreteInterfaceAddress bbInterfaceAddress =
        ConcreteInterfaceAddress.create(
            bbInterfaceSubnet.getStartIp(), bbInterfaceSubnet.getPrefixLength());
    Utils.newInterface(BACKBONE_INTERFACE_NAME, cfgNode, bbInterfaceAddress, "To AWS backbone");

    BgpProcess bgpProcess =
        BgpProcess.builder()
            .setRouterId(bbInterfaceAddress.getIp())
            .setVrf(cfgNode.getDefaultVrf())
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();

    /*
     collect all public IPs in attached VPCs to we can create the right backbone facing policy.
     internet gateway also needs static routes to these public IPs that point to the right subnet.
     these routes, along with interfaces to subnet, are created when we process subnets
    */
    PrefixSpace publicPrefixSpace = new PrefixSpace();
    for (String vpcId : _attachmentVpcIds) {
      region.getNetworkInterfaces().values().stream()
          .filter(ni -> ni.getVpcId().equals(vpcId))
          .flatMap(ni -> ni.getPrivateIpAddresses().stream())
          .map(PrivateIpAddress::getPublicIp)
          .filter(Objects::nonNull)
          .forEach(ip -> publicPrefixSpace.addPrefix(Prefix.create(ip, Prefix.MAX_PREFIX_LENGTH)));
    }
    cfgNode.setRoutingPolicies(
        ImmutableSortedMap.of(
            BACKBONE_EXPORT_POLICY_NAME,
            installRoutingPolicyAdvertiseStatic(
                BACKBONE_EXPORT_POLICY_NAME, cfgNode, publicPrefixSpace, new NetworkFactory())));

    BgpActivePeerConfig.builder()
        .setPeerAddress(bbInterfaceSubnet.getEndIp())
        .setRemoteAs(AWS_BACKBONE_AS)
        .setLocalIp(bbInterfaceAddress.getIp())
        .setLocalAs(AWS_INTERNET_GATEWAY_AS)
        .setBgpProcess(bgpProcess)
        .setIpv4UnicastAddressFamily(
            Ipv4UnicastAddressFamily.builder().setExportPolicy(BACKBONE_EXPORT_POLICY_NAME).build())
        .build();

    return cfgNode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InternetGateway)) {
      return false;
    }
    InternetGateway that = (InternetGateway) o;
    return Objects.equals(_attachmentVpcIds, that._attachmentVpcIds)
        && Objects.equals(_internetGatewayId, that._internetGatewayId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_attachmentVpcIds, _internetGatewayId);
  }
}

package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.representation.aws.Utils.checkNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;

/** Represents an AWS VPC https://docs.aws.amazon.com/cli/latest/reference/ec2/describe-vpcs.html */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
final class Vpc implements AwsVpcEntity, Serializable {

  @JsonIgnoreProperties(ignoreUnknown = true)
  @ParametersAreNonnullByDefault
  private static final class CidrBlockAssociation implements Serializable {

    @Nonnull private final Prefix _block;

    @JsonCreator
    private static CidrBlockAssociation create(
        @Nullable @JsonProperty(JSON_KEY_CIDR_BLOCK) Prefix block) {
      checkArgument(block != null, "CIDR block cannot be null in CIDR block association");
      return new CidrBlockAssociation(block);
    }

    CidrBlockAssociation(Prefix block) {
      _block = block;
    }

    @Nonnull
    Prefix getBlock() {
      return _block;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof CidrBlockAssociation)) {
        return false;
      }
      CidrBlockAssociation that = (CidrBlockAssociation) o;
      return Objects.equals(_block, that._block);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_block);
    }
  }

  @Nonnull private final Set<Prefix> _cidrBlockAssociations;

  @Nonnull private final String _vpcId;

  @JsonCreator
  private static Vpc create(
      @Nullable @JsonProperty(JSON_KEY_VPC_ID) String vpcId,
      @Nullable @JsonProperty(JSON_KEY_CIDR_BLOCK_ASSOCIATION_SET)
          Set<CidrBlockAssociation> cidrBlockAssociations) {
    /*
     We do not parse CidrBlock. The information there also shows up CidrBlockAssociationSet
    */
    checkNonNull(vpcId, JSON_KEY_VPC_ID, "VPC");
    checkNonNull(cidrBlockAssociations, JSON_KEY_CIDR_BLOCK_ASSOCIATION_SET, "VPC");
    return new Vpc(
        vpcId,
        cidrBlockAssociations.stream()
            .map(CidrBlockAssociation::getBlock)
            .collect(ImmutableSet.toImmutableSet()));
  }

  Vpc(String vpcId, Set<Prefix> cidrBlockAssociations) {
    _vpcId = vpcId;
    _cidrBlockAssociations = cidrBlockAssociations;
  }

  @Nonnull
  Set<Prefix> getCidrBlockAssociations() {
    return _cidrBlockAssociations;
  }

  @Override
  public String getId() {
    return _vpcId;
  }

  /**
   * Returns {@link Configuration} corresponding to this VPC node.
   *
   * <p>We only create the node here. Interfaces are added when we traverse its neighbors such as
   * subnets and internet gateways
   */
  Configuration toConfigurationNode(
      AwsConfiguration awsConfiguration, Region region, Warnings warnings) {
    Configuration cfgNode = Utils.newAwsConfiguration(_vpcId, "aws");
    cfgNode.getVendorFamily().getAws().setRegion(region.getName());
    cfgNode.getVendorFamily().getAws().setVpcId(_vpcId);
    _cidrBlockAssociations.forEach(
        cb ->
            cfgNode
                .getDefaultVrf()
                .getStaticRoutes()
                .add(
                    StaticRoute.builder()
                        .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
                        .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
                        .setNetwork(cb)
                        .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
                        .build()));
    return cfgNode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Vpc)) {
      return false;
    }
    Vpc vpc = (Vpc) o;
    return Objects.equals(_cidrBlockAssociations, vpc._cidrBlockAssociations)
        && Objects.equals(_vpcId, vpc._vpcId);
  }

  @Override
  public int hashCode() {
    return com.google.common.base.Objects.hashCode(_cidrBlockAssociations, _vpcId);
  }
}

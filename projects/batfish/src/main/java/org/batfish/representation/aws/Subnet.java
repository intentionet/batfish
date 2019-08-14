package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.representation.aws.Utils.addStaticRoute;
import static org.batfish.representation.aws.Utils.toStaticRoute;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.representation.aws.Route.State;

/**
 * Representation of an AWS subnet
 * https://docs.aws.amazon.com/cli/latest/reference/ec2/describe-subnets.html
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public class Subnet implements AwsVpcEntity, Serializable {

  @Nonnull private final Prefix _cidrBlock;

  @Nonnull private final String _subnetId;

  @Nonnull private final String _vpcId;

  @Nonnull private final Set<Long> _allocatedIps;

  private long _lastGeneratedIp;

  @JsonCreator
  private static Subnet create(
      @Nullable @JsonProperty(JSON_KEY_CIDR_BLOCK) Prefix cidrBlock,
      @Nullable @JsonProperty(JSON_KEY_SUBNET_ID) String subnetId,
      @Nullable @JsonProperty(JSON_KEY_VPC_ID) String vpcId) {
    checkArgument(cidrBlock != null, "CIDR block cannot be null for subnet");
    checkArgument(subnetId != null, "Subnet id cannot be null for subnet");
    checkArgument(vpcId != null, "VPC id cannot be null for subnet");
    return new Subnet(cidrBlock, subnetId, vpcId);
  }

  Subnet(Prefix cidrBlock, String subnetId, String vpcId) {
    _cidrBlock = cidrBlock;
    _subnetId = subnetId;
    _vpcId = vpcId;

    _allocatedIps = new HashSet<>();
    // skipping (startIp+1) as it is used as the default gateway for instances in this subnet
    _lastGeneratedIp = _cidrBlock.getStartIp().asLong() + 1;
  }

  Set<Long> getAllocatedIps() {
    return _allocatedIps;
  }

  Ip getNextIp() {
    for (Long ipAsLong = _lastGeneratedIp + 1;
        ipAsLong < _cidrBlock.getEndIp().asLong();
        ipAsLong++) {
      if (!_allocatedIps.contains(ipAsLong)) {
        _allocatedIps.add(ipAsLong);
        _lastGeneratedIp = ipAsLong;
        return Ip.create(ipAsLong);
      }
    }
    // subnet's CIDR block out of IPs
    throw new BatfishException(String.format("%s subnet ran out of IPs", _subnetId));
  }

  Ip computeInstancesIfaceIp() {
    Long generatedIp = _cidrBlock.getStartIp().asLong() + 1L;
    _allocatedIps.add(generatedIp);
    _lastGeneratedIp = generatedIp;
    return Ip.create(generatedIp);
  }

  private NetworkAcl findMyNetworkAcl(Map<String, NetworkAcl> networkAcls) {
    List<NetworkAcl> matchingAcls =
        networkAcls.values().stream()
            .filter((NetworkAcl acl) -> acl.getVpcId().equals(_vpcId))
            .collect(Collectors.toList());

    if (matchingAcls.isEmpty()) {
      throw new BatfishException("Could not find a network ACL for subnet " + _subnetId);
    }

    if (matchingAcls.size() > 1) {
      List<String> aclIds =
          matchingAcls.stream().map(NetworkAcl::getId).collect(Collectors.toList());
      throw new BatfishException(
          String.format("Found multiple network ACLs %s for subnet %s", aclIds, _subnetId));
    }

    return matchingAcls.get(0);
  }

  @Nonnull
  public Prefix getCidrBlock() {
    return _cidrBlock;
  }

  @Override
  public String getId() {
    return _subnetId;
  }

  public String getVpcId() {
    return _vpcId;
  }

  /**
   * Returns the {@link Configuration} node for this subnet.
   *
   * <p>We also do the work needed to connect to the VPC router here: Add an interface on the VPC
   * router and create the necessary static routes.
   */
  Configuration toConfigurationNode(
      AwsConfiguration awsConfiguration, Region region, Warnings warnings) {
    Configuration cfgNode = Utils.newAwsConfiguration(_subnetId, "aws");

    // add one interface that faces all instances (assumes a LAN)
    String instancesIfaceName = _subnetId;
    Ip instancesIfaceIp = computeInstancesIfaceIp();
    ConcreteInterfaceAddress instancesIfaceAddress =
        ConcreteInterfaceAddress.create(instancesIfaceIp, _cidrBlock.getPrefixLength());
    Interface subnetToInstances =
        Utils.newInterface(
            instancesIfaceName, cfgNode, instancesIfaceAddress, "To instances " + _subnetId);

    // connect to the VPC
    Configuration vpcConfigNode = awsConfiguration.getConfigurationNodes().get(_vpcId);
    Utils.connect(awsConfiguration, cfgNode, vpcConfigNode);

    // add a static route on the vpc router for this subnet;
    addStaticRoute(vpcConfigNode, toStaticRoute(_cidrBlock, Utils.getInterfaceIp(cfgNode, _vpcId)));

    // add network acls on the subnet node
    NetworkAcl myNetworkAcl = findMyNetworkAcl(region.getNetworkAcls());

    IpAccessList inAcl = myNetworkAcl.getIngressAcl();
    IpAccessList outAcl = myNetworkAcl.getEgressAcl();
    cfgNode.getIpAccessLists().put(inAcl.getName(), inAcl);
    cfgNode.getIpAccessLists().put(outAcl.getName(), outAcl);

    // add ACLs to interface facing the vpc
    Interface vpcIfaceOnSubnet = cfgNode.getAllInterfaces().get(vpcConfigNode.getHostname());
    vpcIfaceOnSubnet.setIncomingFilter(inAcl);
    vpcIfaceOnSubnet.setOutgoingFilter(outAcl);

    // connect the vpn gateway to the subnet if one exists
    Optional<VpnGateway> optVpnGateway = region.findVpnGateway(_vpcId);
    if (optVpnGateway.isPresent()) {
      Configuration vgwConfig =
          awsConfiguration.getConfigurationNodes().get(optVpnGateway.get().getId());
      Utils.connect(awsConfiguration, cfgNode, vgwConfig);
    }

    // connect the internet gateway if one exists
    Optional<InternetGateway> optInternetGateway = region.findInternetGateway(_vpcId);
    if (optInternetGateway.isPresent()) {
      Configuration igwConfig =
          awsConfiguration.getConfigurationNodes().get(optInternetGateway.get().getId());
      Utils.connect(awsConfiguration, cfgNode, igwConfig);
    }

    // for public IPs in the subnet, add static routes to enable inbound traffic
    //  - on internet gateway toward the subnet
    //  - on the subnet toward instances
    List<Ip> publicIps = findMyPublicIps(region);
    if (!publicIps.isEmpty()) {
      if (!optInternetGateway.isPresent()) {
        warnings.redFlag(
            String.format(
                "Internet gateway not found for subnet %s in vpc %s with public IPs %s",
                _subnetId, _vpcId, publicIps));
      } else {
        Configuration igwConfig =
            awsConfiguration.getConfigurationNodes().get(optInternetGateway.get().getId());
        Ip nhipOnIgw = Utils.getInterfaceIp(cfgNode, igwConfig.getHostname());
        publicIps.forEach(
            pip -> {
              addStaticRoute(igwConfig, toStaticRoute(pip, nhipOnIgw));
              addStaticRoute(cfgNode, toStaticRoute(pip, subnetToInstances));
            });
      }
    }

    // process route tables to get outbound traffic going
    Optional<RouteTable> routeTable = region.findRouteTable(_vpcId, _subnetId);
    if (!routeTable.isPresent()) {
      warnings.redFlag(
          String.format("Route table not found for subnet %s in vpc %s", _subnetId, _vpcId));
    } else {
      routeTable
          .get()
          .getRoutes()
          .forEach(
              route -> {
                StaticRoute sr =
                    getStaticRoute(
                        route,
                        optInternetGateway.orElse(null),
                        optVpnGateway.orElse(null),
                        awsConfiguration,
                        warnings);
                if (sr != null) {
                  addStaticRoute(cfgNode, sr);
                }
              });
    }

    cfgNode.getVendorFamily().getAws().setVpcId(_vpcId);
    cfgNode.getVendorFamily().getAws().setSubnetId(_subnetId);
    cfgNode.getVendorFamily().getAws().setRegion(region.getName());

    return cfgNode;
  }

  private List<Ip> findMyPublicIps(Region region) {
    return region.getNetworkInterfaces().values().stream()
        .filter(ni -> ni.getSubnetId().equals(_subnetId))
        .flatMap(ni -> ni.getPrivateIpAddresses().stream())
        .map(PrivateIpAddress::getPublicIp)
        .filter(Objects::nonNull)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns a static route corresponding to {@link Route}. Assumes that the node to which the route
   * is pointed is already connected to subnet node.
   */
  @Nullable
  StaticRoute getStaticRoute(
      Route route,
      @Nullable InternetGateway igw,
      @Nullable VpnGateway vgw,
      AwsConfiguration awsConfiguration,
      Warnings warnings) {

    StaticRoute.Builder sr =
        StaticRoute.builder()
            .setNetwork(route.getDestinationCidrBlock())
            .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
            .setMetric(Route.DEFAULT_STATIC_ROUTE_COST);

    if (route.getState() == State.BLACKHOLE) {
      return sr.setNextHopInterface(Interface.NULL_INTERFACE_NAME).build();
    } else {
      switch (route.getTargetType()) {
        case Gateway:
          if (route.getTarget() == null) {
            warnings.redFlag("Route target is null for target type Gateway");
            return null;
          }
          if (route.getTarget().equals("local")) {
            // Tp VPC
            return sr.setNextHopIp(
                    Utils.getInterfaceIp(
                        awsConfiguration.getConfigurationNodes().get(_vpcId), _subnetId))
                .build();
          } else {
            if (igw != null && route.getTarget().equals(igw.getId())) {
              // To IGW
              return sr.setNextHopIp(
                      Utils.getInterfaceIp(
                          awsConfiguration.getConfigurationNodes().get(igw.getId()), _subnetId))
                  .build();
            } else if (vgw != null && route.getTarget().equals(vgw.getId())) {
              // To VGW
              return sr.setNextHopIp(
                      Utils.getInterfaceIp(
                          awsConfiguration.getConfigurationNodes().get(vgw.getId()), _subnetId))
                  .build();
            } else {
              warnings.redFlag(
                  String.format(
                      "Unknown target %s specified in this route not accessible from this subnet",
                      route.getTarget()));
              return null;
            }
          }
        default:
          warnings.redFlag("Unsupported target type: " + route.getTargetType());
          return null;
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Subnet)) {
      return false;
    }
    Subnet subnet = (Subnet) o;
    return _lastGeneratedIp == subnet._lastGeneratedIp
        && Objects.equals(_cidrBlock, subnet._cidrBlock)
        && Objects.equals(_subnetId, subnet._subnetId)
        && Objects.equals(_vpcId, subnet._vpcId)
        && Objects.equals(_allocatedIps, subnet._allocatedIps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_cidrBlock, _subnetId, _vpcId, _allocatedIps, _lastGeneratedIp);
  }
}

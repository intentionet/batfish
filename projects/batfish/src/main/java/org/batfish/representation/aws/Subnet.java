package org.batfish.representation.aws;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.immutableEntry;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.representation.aws.AwsLocationInfoUtils.subnetInterfaceLinkLocationInfo;
import static org.batfish.representation.aws.AwsLocationInfoUtils.subnetInterfaceLocationInfo;
import static org.batfish.representation.aws.Utils.addStaticRoute;
import static org.batfish.representation.aws.Utils.connect;
import static org.batfish.representation.aws.Utils.getInterfaceLinkLocalIp;
import static org.batfish.representation.aws.Utils.interfaceNameToRemote;
import static org.batfish.representation.aws.Utils.toStaticRoute;
import static org.batfish.representation.aws.Vpc.vrfNameForLink;
import static org.batfish.specifier.Location.interfaceLinkLocation;
import static org.batfish.specifier.Location.interfaceLocation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.representation.aws.NetworkAcl.NetworkAclAssociation;
import org.batfish.representation.aws.Route.State;
import org.batfish.representation.aws.Route.TargetType;

/**
 * Representation of an AWS subnet
 * https://docs.aws.amazon.com/cli/latest/reference/ec2/describe-subnets.html
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public class Subnet implements AwsVpcEntity, Serializable {
  @VisibleForTesting static final String NLB_INSTANCE_TARGETS_VRF_NAME = "NLB_instance_targets";
  @VisibleForTesting static final String NLB_INSTANCE_TARGETS_IFACE_SUFFIX = "nlb-instance-targets";

  @Nonnull private final String _availabilityZone;

  @Nonnull private final Prefix _cidrBlock;

  @Nonnull private final String _subnetId;

  @Nonnull private final Map<String, String> _tags;

  @Nonnull private final String _vpcId;

  @Nonnull private final Set<Long> _allocatedIps;

  private long _lastGeneratedIp;

  @JsonCreator
  private static Subnet create(
      @Nullable @JsonProperty(JSON_KEY_CIDR_BLOCK) Prefix cidrBlock,
      @Nullable @JsonProperty(JSON_KEY_SUBNET_ID) String subnetId,
      @Nullable @JsonProperty(JSON_KEY_VPC_ID) String vpcId,
      @Nullable @JsonProperty(JSON_KEY_TAGS) List<Tag> tags,
      @Nullable @JsonProperty(JSON_KEY_AVAILABILITY_ZONE) String availabilityZone) {
    checkArgument(cidrBlock != null, "CIDR block cannot be null for subnet");
    checkArgument(subnetId != null, "Subnet id cannot be null for subnet");
    checkArgument(vpcId != null, "VPC id cannot be null for subnet");
    checkArgument(availabilityZone != null, "Availability zone cannot be null for subnet");
    return new Subnet(
        cidrBlock,
        subnetId,
        vpcId,
        availabilityZone,
        firstNonNull(tags, ImmutableList.<Tag>of()).stream()
            .collect(ImmutableMap.toImmutableMap(Tag::getKey, Tag::getValue)));
  }

  Subnet(
      Prefix cidrBlock,
      String subnetId,
      String vpcId,
      String availabilityZone,
      Map<String, String> tags) {
    _cidrBlock = cidrBlock;
    _subnetId = subnetId;
    _vpcId = vpcId;
    _availabilityZone = availabilityZone;

    _allocatedIps = new HashSet<>();
    // skipping (startIp+1) as it is used as the default gateway for instances in this subnet
    _lastGeneratedIp = _cidrBlock.getStartIp().asLong() + 1;
    _tags = tags;
  }

  Set<Long> getAllocatedIps() {
    return _allocatedIps;
  }

  /** Returns {@code true} if this subnet has an available IP address. */
  boolean hasNextIp() {
    for (long ipAsLong = _lastGeneratedIp + 1;
        ipAsLong < _cidrBlock.getEndIp().asLong();
        ipAsLong++) {
      if (!_allocatedIps.contains(ipAsLong)) {
        return true;
      }
    }
    return false;
  }

  Ip getNextIp() {
    for (long ipAsLong = _lastGeneratedIp + 1;
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
    long generatedIp = _cidrBlock.getStartIp().asLong() + 1L;
    _allocatedIps.add(generatedIp);
    _lastGeneratedIp = generatedIp;
    return Ip.create(generatedIp);
  }

  static List<NetworkAcl> findSubnetNetworkAcl(
      Map<String, NetworkAcl> networkAcls, String vpcId, String subnetId) {
    List<NetworkAcl> subnetAcls =
        networkAcls.values().stream()
            .filter(acl -> acl.getVpcId().equals(vpcId))
            .filter(
                acl ->
                    acl.getAssociations().stream()
                        .map(NetworkAclAssociation::getSubnetId)
                        .anyMatch(subnetId::equals))
            .collect(ImmutableList.toImmutableList());

    if (!subnetAcls.isEmpty()) {
      return subnetAcls;
    }

    /*
     use the default for the VPC if we don't find an explicit association. this is mostly a
     defensive move, as AWS appears to provide explicit associations at the moment
    */
    return networkAcls.values().stream()
        .filter(acl -> acl.getVpcId().equals(vpcId) && acl.isDefault())
        .collect(ImmutableList.toImmutableList());
  }

  @Nonnull
  public Prefix getCidrBlock() {
    return _cidrBlock;
  }

  @Override
  public String getId() {
    return _subnetId;
  }

  @Nonnull
  public String getVpcId() {
    return _vpcId;
  }

  @Nonnull
  public String getAvailabilityZone() {
    return _availabilityZone;
  }

  /**
   * Returns the {@link Configuration} node for this subnet.
   *
   * <p>We also do the work needed to connect to the VPC router here: Add an interface on the VPC
   * router and create the necessary static routes.
   */
  Configuration toConfigurationNode(
      ConvertedConfiguration awsConfiguration, Region region, Warnings warnings) {
    // Private subnet by default, may get overridden below.
    Configuration cfgNode =
        Utils.newAwsConfiguration(
            nodeName(_subnetId), "aws", _tags, DeviceModel.AWS_SUBNET_PRIVATE);
    cfgNode.getVendorFamily().getAws().setVpcId(_vpcId);
    cfgNode.getVendorFamily().getAws().setSubnetId(_subnetId);
    cfgNode.getVendorFamily().getAws().setRegion(region.getName());

    // add one interface that faces all instances (assumes a LAN)
    String instancesIfaceName = instancesInterfaceName(_subnetId);
    Ip instancesIfaceIp = computeInstancesIfaceIp();
    ConcreteInterfaceAddress instancesIfaceAddress =
        ConcreteInterfaceAddress.create(instancesIfaceIp, _cidrBlock.getPrefixLength());
    Utils.newInterface(
        instancesIfaceName, cfgNode, instancesIfaceAddress, "To instances " + _subnetId);

    IpAccessList ingressAcl = null;
    IpAccessList egressAcl = null;
    {
      // This network ACL should be applied to every subnet interface not facing an instance.
      // NOTE: Every call to NetworkAcl#getIngressAcl or NetworkAcl#getEgressAcl returns a *new*
      // IpAccessList object, which is why it's in a limited scope: they should only be called once.
      Optional<NetworkAcl> networkAcl = getNetworkAcl(region, warnings);
      if (networkAcl.isPresent()) {
        ingressAcl = networkAcl.get().getIngressAcl();
        egressAcl = networkAcl.get().getEgressAcl();
        cfgNode.getIpAccessLists().put(ingressAcl.getName(), ingressAcl);
        cfgNode.getIpAccessLists().put(egressAcl.getName(), egressAcl);
      }
    }
    // defined separately so they can be final and thus used in lambda expressions
    IpAccessList ingressNetworkAcl = ingressAcl;
    IpAccessList egressNetworkAcl = egressAcl;

    // connect to the VPC on each of its VRFs. add static routes on the VPC to this subnet.
    Configuration vpcConfigNode = awsConfiguration.getNode(Vpc.nodeName(_vpcId));
    vpcConfigNode
        .getVrfs()
        .values()
        .forEach(
            vrf -> {
              String interfaceSuffix = vrf.getName().equals(DEFAULT_VRF_NAME) ? "" : vrf.getName();
              connect(
                  awsConfiguration,
                  cfgNode,
                  DEFAULT_VRF_NAME,
                  vpcConfigNode,
                  vrf.getName(),
                  interfaceSuffix);

              String subnetToVpcIfaceName = interfaceNameToRemote(vpcConfigNode, interfaceSuffix);
              Interface subnetIfaceToVpc = cfgNode.getAllInterfaces().get(subnetToVpcIfaceName);
              subnetIfaceToVpc.setIncomingFilter(ingressNetworkAcl);
              subnetIfaceToVpc.setOutgoingFilter(egressNetworkAcl);

              // add a static route on the vpc router for this subnet
              addStaticRoute(
                  vrf,
                  toStaticRoute(
                      _cidrBlock,
                      interfaceNameToRemote(cfgNode, interfaceSuffix),
                      getInterfaceLinkLocalIp(cfgNode, subnetToVpcIfaceName)));
            });

    @Nullable
    String vpnGatewayId = region.findVpnGateway(_vpcId).map(VpnGateway::getId).orElse(null);
    Optional<RouteTable> routeTable = region.findRouteTable(_vpcId, _subnetId);
    Optional<InternetGateway> optInternetGateway = region.findInternetGateway(_vpcId);

    if (optInternetGateway.isPresent()
        && routeTable.isPresent()
        && isPublicSubnet(optInternetGateway.get(), routeTable.get())) {
      cfgNode.setDeviceModel(DeviceModel.AWS_SUBNET_PUBLIC);
    }

    // collect all VPC-level gateways: IGW, VGW, VPC Endpoint Gateway
    List<String> vpcGatewayIds =
        Streams.concat(
                Stream.of(optInternetGateway.map(InternetGateway::getId).orElse(null)),
                Stream.of(vpnGatewayId),
                region.getVpcEndpoints().values().stream()
                    .filter(
                        vpce ->
                            vpce instanceof VpcEndpointGateway && vpce.getVpcId().equals(_vpcId))
                    .map(VpcEndpoint::getId))
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());

    // process route tables to get outbound traffic going
    if (!routeTable.isPresent()) {
      warnings.redFlag(
          String.format("Route table not found for subnet %s in vpc %s", _subnetId, _vpcId));
    } else {
      routeTable
          .get()
          .getRoutes()
          .forEach(
              route ->
                  processRoute(
                      cfgNode,
                      region,
                      route,
                      vpcConfigNode,
                      vpcGatewayIds,
                      awsConfiguration,
                      warnings));
    }

    addNlbInstanceTargetInterfaces(
        awsConfiguration, region, cfgNode, vpcConfigNode, ingressNetworkAcl, egressNetworkAcl);

    // create LocationInfo for each link location on the node.
    cfgNode.setLocationInfo(
        cfgNode.getAllInterfaces().values().stream()
            .flatMap(
                iface ->
                    Stream.of(
                        immutableEntry(
                            interfaceLocation(iface), subnetInterfaceLocationInfo(iface)),
                        immutableEntry(
                            interfaceLinkLocation(iface), subnetInterfaceLinkLocationInfo(iface))))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue)));

    return cfgNode;
  }

  /**
   * Adds VRFs and connected interfaces required for NLBs with instance targets, if this subnet is
   * connected to any instance targets or NLBs with instance targets.
   */
  @VisibleForTesting
  void addNlbInstanceTargetInterfaces(
      ConvertedConfiguration awsConfiguration,
      Region region,
      Configuration subnetCfg,
      Configuration vpcCfg,
      // These ACLs should be applied to all subnet interfaces that don't face instances
      @Nullable IpAccessList ingressNetworkAcl,
      @Nullable IpAccessList egressNetworkAcl) {
    Collection<LoadBalancer> nlbs = awsConfiguration.getSubnetsToNlbs().get(_subnetId);
    if (!awsConfiguration.getSubnetsToInstanceTargets().containsKey(_subnetId) && nlbs.isEmpty()) {
      return;
    }
    //  New VRF in subnet and VPC nodes
    Vrf newSubnetVrf =
        Vrf.builder().setName(NLB_INSTANCE_TARGETS_VRF_NAME).setOwner(subnetCfg).build();
    Vrf newVpcVrf = vpcCfg.getVrfs().get(NLB_INSTANCE_TARGETS_VRF_NAME);
    if (newVpcVrf == null) {
      newVpcVrf = Vrf.builder().setName(NLB_INSTANCE_TARGETS_VRF_NAME).setOwner(vpcCfg).build();
    }

    Utils.connect(
        awsConfiguration,
        subnetCfg,
        NLB_INSTANCE_TARGETS_VRF_NAME,
        vpcCfg,
        NLB_INSTANCE_TARGETS_VRF_NAME,
        NLB_INSTANCE_TARGETS_IFACE_SUFFIX);

    // Add firewall session info on new subnet interface to VPC
    String subnetToVpcIfaceName = interfaceNameToRemote(vpcCfg, NLB_INSTANCE_TARGETS_IFACE_SUFFIX);
    String vpcToSubnetIfaceName =
        interfaceNameToRemote(subnetCfg, NLB_INSTANCE_TARGETS_IFACE_SUFFIX);
    Interface vpcToSubnetIface = vpcCfg.getAllInterfaces().get(vpcToSubnetIfaceName);
    Interface subnetToVpcIface = subnetCfg.getAllInterfaces().get(subnetToVpcIfaceName);
    subnetToVpcIface.setIncomingFilter(ingressNetworkAcl);
    subnetToVpcIface.setOutgoingFilter(egressNetworkAcl);
    String incomingAclName = ingressNetworkAcl == null ? null : ingressNetworkAcl.getName();
    String outgoingAclName = egressNetworkAcl == null ? null : egressNetworkAcl.getName();
    // If subnet has NLBs, use a session to direct return traffic from an instance target in another
    // subnet back to the NLB
    if (!nlbs.isEmpty()) {
      subnetToVpcIface.setFirewallSessionInterfaceInfo(
          new FirewallSessionInterfaceInfo(
              false, ImmutableList.of(subnetToVpcIfaceName), incomingAclName, outgoingAclName));
    }

    // For each NLB in the subnet: new interface connecting to NLB, no filters
    for (LoadBalancer nlb : nlbs) {
      Configuration nlbConfig =
          awsConfiguration.getNode(LoadBalancer.getNodeId(nlb.getDnsName(), _availabilityZone));
      Utils.connect(
          awsConfiguration,
          subnetCfg,
          NLB_INSTANCE_TARGETS_VRF_NAME,
          nlbConfig,
          nlbConfig.getDefaultVrf().getName(),
          NLB_INSTANCE_TARGETS_IFACE_SUFFIX);

      // Add firewall session info on NLB interface to this subnet
      String nlbIfaceName = interfaceNameToRemote(subnetCfg, NLB_INSTANCE_TARGETS_IFACE_SUFFIX);
      Interface nlbIface = nlbConfig.getAllInterfaces().get(nlbIfaceName);
      nlbIface.setFirewallSessionInterfaceInfo(
          new FirewallSessionInterfaceInfo(false, ImmutableList.of(nlbIfaceName), null, null));
    }

    // Add static routes in subnet's new VRF and connect subnet to its instance targets
    for (Entry<String, Instance> e : awsConfiguration.getSubnetsToInstanceTargets().entries()) {
      String subnetId = e.getKey();
      if (!region.getSubnets().get(subnetId).getVpcId().equals(_vpcId)) {
        continue;
      }
      Instance instanceTarget = e.getValue();
      // guaranteed during computation of subnets -> instance targets map
      assert instanceTarget.getPrimaryPrivateIpAddress() != null;
      Prefix instancePrefix = instanceTarget.getPrimaryPrivateIpAddress().toPrefix();

      String staticRouteNextHopIface = subnetToVpcIfaceName;
      if (subnetId.equals(_subnetId)) {
        // For each instance target in the subnet: New interface connecting to instance, no filters
        Configuration instanceConfig =
            awsConfiguration.getNode(Instance.instanceHostname(instanceTarget.getId()));
        Utils.connect(
            awsConfiguration,
            subnetCfg,
            NLB_INSTANCE_TARGETS_VRF_NAME,
            instanceConfig,
            instanceConfig.getDefaultVrf().getName(),
            NLB_INSTANCE_TARGETS_IFACE_SUFFIX);

        String subnetToInstanceIfaceName =
            interfaceNameToRemote(instanceConfig, NLB_INSTANCE_TARGETS_IFACE_SUFFIX);
        Interface subnetToInstanceIface =
            subnetCfg.getAllInterfaces().get(subnetToInstanceIfaceName);
        String instanceToSubnetIfaceName =
            interfaceNameToRemote(subnetCfg, NLB_INSTANCE_TARGETS_IFACE_SUFFIX);
        Interface newInstanceIface =
            instanceConfig.getAllInterfaces().get(instanceToSubnetIfaceName);

        // New interface on instance target needs security groups
        Set<String> instanceSecurityGroups =
            region.getNetworkInterfaces().values().stream()
                .filter(ni -> instanceTarget.getId().equals(ni.getAttachmentInstanceId()))
                .flatMap(ni -> ni.getGroups().stream())
                .collect(ImmutableSet.toImmutableSet());
        region.applyAclsToInterfaceBasedOnSecurityGroups(
            instanceSecurityGroups, instanceConfig, newInstanceIface);

        // Subnet needs to set up a session for return traffic from the instance
        subnetToInstanceIface.setFirewallSessionInterfaceInfo(
            new FirewallSessionInterfaceInfo(
                false, ImmutableList.of(subnetToInstanceIfaceName), null, null));

        staticRouteNextHopIface = subnetToInstanceIfaceName;

        // Give the VPC static routes to this subnet's instances, and set up session info on its
        // interface to the subnet so that return traffic will be routed back
        Utils.addStaticRoute(
            newVpcVrf,
            toStaticRoute(instancePrefix, vpcToSubnetIfaceName, AwsConfiguration.LINK_LOCAL_IP));
        vpcToSubnetIface.setFirewallSessionInterfaceInfo(
            new FirewallSessionInterfaceInfo(
                false, ImmutableList.of(vpcToSubnetIfaceName), null, null));
      }

      /*
      Static routes: One per instance target in this subnet's VPC
         - For instance targets within this subnet, forward out the interface to that instance
         - For other instance targets, forward out the interface to the VPC
         - All have next hop IP AwsConfiguration.LINK_LOCAL_IP
       */
      Utils.addStaticRoute(
          newSubnetVrf,
          toStaticRoute(instancePrefix, staticRouteNextHopIface, AwsConfiguration.LINK_LOCAL_IP));
    }
  }

  /** Get {@link NetworkAcl} for this subnet, if one is present, and file warnings if needed. */
  private Optional<NetworkAcl> getNetworkAcl(Region region, Warnings warnings) {
    List<NetworkAcl> myNetworkAcls =
        findSubnetNetworkAcl(region.getNetworkAcls(), _vpcId, _subnetId);
    if (!myNetworkAcls.isEmpty()) {
      if (myNetworkAcls.size() > 1) {
        List<String> aclIds =
            myNetworkAcls.stream().map(NetworkAcl::getId).collect(ImmutableList.toImmutableList());
        warnings.redFlag(
            String.format(
                "Found multiple network ACLs %s for subnet %s. Using %s.",
                aclIds, _subnetId, myNetworkAcls.get(0).getId()));
      }
      return Optional.of(myNetworkAcls.get(0));
    }
    warnings.redFlag("Could not find a network ACL for subnet " + _subnetId);
    return Optional.empty();
  }

  /**
   * A public subnet is one whose route table has a route to an Internet gateway.
   * https://docs.amazonaws.cn/en_us/vpc/latest/userguide/VPC_Scenario2.html
   */
  private static boolean isPublicSubnet(InternetGateway internetGateway, RouteTable routeTable) {
    return routeTable.getRoutes().stream()
        .anyMatch(
            route ->
                route.getTargetType() == TargetType.Gateway
                    && internetGateway.getId().equals(route.getTarget()));
  }

  /**
   * Processes a route entry corresponding to {@code route}. Assumes that the gateways and primary
   * VPC interface (i.e., not corresponding to a peering connection) are already connected to subnet
   * node. For VPC connections, creates the relevant interface on the VPC and then connects it.
   */
  @VisibleForTesting
  void processRoute(
      Configuration cfgNode,
      Region region,
      Route route,
      Configuration vpcNode,
      List<String> vpcGatewayIds, // IGW, VGW, VPC Endpoint Gateway
      ConvertedConfiguration awsConfiguration,
      Warnings warnings) {
    List<Prefix> networks;
    if (route instanceof RouteV4) {
      networks = ImmutableList.of(((RouteV4) route).getDestinationCidrBlock());
    } else if (route instanceof RoutePrefixListId) {
      PrefixList prefixList =
          region.getPrefixLists().get(((RoutePrefixListId) route).getPrefixListId());
      if (prefixList == null) {
        warnings.redFlag(
            String.format(
                "Prefix list %s mentioned in route %s not found in region %s",
                ((RoutePrefixListId) route).getPrefixListId(), route, region.getName()));
        return;
      }
      networks = ImmutableList.copyOf(prefixList.getCidrs());
    } else {
      // we don't do V6
      return;
    }

    final String nexthopInterfaceName;
    final Ip nextHopIp;

    if (route.getState() == State.BLACKHOLE) {
      nexthopInterfaceName = NULL_INTERFACE_NAME;
      nextHopIp = null;
    } else {

      switch (route.getTargetType()) {
        case Gateway:
          if (route.getTarget() == null) {
            warnings.redFlag("Route target is null for target type Gateway");
            return;
          }
          if (route.getTarget().equals("local")) {
            // To VPC
            nextHopIp = getInterfaceLinkLocalIp(vpcNode, _subnetId);
            nexthopInterfaceName = interfaceNameToRemote(vpcNode);
          } else if (vpcGatewayIds.contains(route.getTarget())) {
            nexthopInterfaceName =
                Utils.interfaceNameToRemote(vpcNode, vrfNameForLink(route.getTarget()));
            nextHopIp =
                getInterfaceLinkLocalIp(
                    vpcNode,
                    Utils.interfaceNameToRemote(cfgNode, vrfNameForLink(route.getTarget())));
          } else {
            warnings.redFlag(
                String.format(
                    "Unknown target %s specified in this route not accessible from this subnet",
                    route.getTarget()));
            return;
          }
          break;
        case VpcPeeringConnection:
          String connectionId = route.getTarget();
          if (connectionId == null) {
            warnings.redFlag(
                String.format("Route target is null for a VPC peering connection type: %s", route));
            return;
          }
          nexthopInterfaceName = Utils.interfaceNameToRemote(vpcNode, vrfNameForLink(connectionId));
          nextHopIp =
              getInterfaceLinkLocalIp(
                  vpcNode, Utils.interfaceNameToRemote(cfgNode, vrfNameForLink(connectionId)));
          break;
        case TransitGateway:
          assert route.getTarget() != null; // suppress warning
          TransitGatewayVpcAttachment attachment =
              region.findTransitGatewayVpcAttachment(_vpcId, route.getTarget()).orElse(null);
          if (attachment == null) {
            warnings.redFlag(
                String.format(
                    "Transit gateway VPC attachment between %s and %s not found. Needed for route: %s",
                    _vpcId, route.getTarget(), route));
            return;
          }
          // this attachment is not reachable if it is not present in our availability zone
          if (!attachment.getAvailabilityZones(region).contains(_availabilityZone)) {
            nexthopInterfaceName = NULL_INTERFACE_NAME;
            nextHopIp = null;
          } else {
            nexthopInterfaceName =
                Utils.interfaceNameToRemote(vpcNode, vrfNameForLink(attachment.getId()));
            nextHopIp =
                getInterfaceLinkLocalIp(
                    vpcNode,
                    Utils.interfaceNameToRemote(cfgNode, vrfNameForLink(attachment.getId())));
          }
          break;
        case NatGateway:
          NatGateway natGateway = region.getNatGateways().get(route.getTarget());
          if (natGateway == null) {
            warnings.redFlag(
                String.format(
                    "Nat gateway %s not found. Needed for route: %s", route.getTarget(), route));
            return;
          }
          // If the NAT is in our subnet, send it directly. Otherwise, send it via the VPC
          if (natGateway.getSubnetId().equals(_subnetId)) {
            // This configuration won't actually work (which manual testing confirms). The packet
            // will go the NAT, which will NAT the *source ip* and send it back to the subnet
            // router,  which will then send it to NAT, and so on. Nevertheless, we add this route
            // instead of ignoring it because it is the correct model and users expect routes in AWS
            // and Batfish to line up.
            nexthopInterfaceName =
                interfaceNameToRemote(awsConfiguration.getNode(natGateway.getId()));
            nextHopIp = natGateway.getPrivateIp();
          } else {
            nexthopInterfaceName =
                Utils.interfaceNameToRemote(vpcNode, vrfNameForLink(natGateway.getId()));
            nextHopIp =
                getInterfaceLinkLocalIp(
                    vpcNode,
                    Utils.interfaceNameToRemote(cfgNode, vrfNameForLink(natGateway.getId())));
          }
          break;
        default:
          warnings.redFlag("Unsupported target type: " + route.getTargetType());
          return;
      }
    }
    networks.forEach(
        network ->
            addStaticRoute(
                cfgNode,
                StaticRoute.builder()
                    .setNetwork(network)
                    .setNextHopInterface(nexthopInterfaceName)
                    .setNextHopIp(nextHopIp)
                    .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
                    .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
                    .build()));
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
        && Objects.equals(_availabilityZone, subnet._availabilityZone)
        && Objects.equals(_tags, subnet._tags)
        && Objects.equals(_allocatedIps, subnet._allocatedIps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _cidrBlock, _subnetId, _vpcId, _availabilityZone, _allocatedIps, _lastGeneratedIp, _tags);
  }

  public static String nodeName(String subnetId) {
    return subnetId;
  }

  public static String instancesInterfaceName(String subnetId) {
    // since there is only one such interface per subnet node, we can keep it simple
    return "to-instances";
  }
}

package org.batfish.representation.aws;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Pair;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Subnet implements AwsVpcEntity, Serializable {

  private static final long serialVersionUID = 1L;

  private Set<Long> _allocatedIps = new HashSet<>();

  private Prefix _cidrBlock;

  private transient String _internetGatewayId;

  private String _subnetId;

  private String _vpcId;

  private transient String _vpnGatewayId;

  public Subnet(JSONObject jObj, BatfishLogger logger) throws JSONException {
    _cidrBlock = Prefix.parse(jObj.getString(JSON_KEY_CIDR_BLOCK));
    _subnetId = jObj.getString(JSON_KEY_SUBNET_ID);
    _vpcId = jObj.getString(JSON_KEY_VPC_ID);
  }

  public Set<Long> getAllocatedIps() {
    return _allocatedIps;
  }

  Ip getNextIp() {
    // skipping (startIp+1) as it is used as the default gateway for instances in this subnet
    for (Long ipAsLong = _cidrBlock.getStartIp().asLong() + 2;
        ipAsLong < _cidrBlock.getEndIp().asLong();
        ipAsLong++) {
      if (!_allocatedIps.contains(ipAsLong)) {
        _allocatedIps.add(ipAsLong);
        return new Ip(ipAsLong);
      }
    }
    // subnet's CIDR block out of IPs
    throw new BatfishException(String.format("%s subnet ran out of IPs", _subnetId));
  }

  Ip computeInstancesIfaceIp() {
    Long generatedIp = _cidrBlock.getStartIp().asLong() + 1L;
    _allocatedIps.add(generatedIp);
    return new Ip(generatedIp);
  }

  private NetworkAcl findMyNetworkAcl(Map<String, NetworkAcl> networkAcls) {
    List<NetworkAcl> matchingAcls =
        networkAcls
            .values()
            .stream()
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

  // TODO: implement per-subnet routing
  @SuppressWarnings("unused")
  private RouteTable findMyRouteTable(Map<String, RouteTable> routeTables) {
    // All route tables for this VPC.
    List<RouteTable> sameVpcTables =
        routeTables
            .values()
            .stream()
            .filter((RouteTable rt) -> rt.getVpcId().equals(_vpcId))
            .collect(Collectors.toList());

    // First we look for the unique route table with an association for this subnet.
    List<RouteTable> matchingRouteTables =
        sameVpcTables
            .stream()
            .filter(
                (RouteTable rt) ->
                    rt.getAssociations()
                        .stream()
                        .anyMatch(
                            (RouteTableAssociation rtAssoc) ->
                                _subnetId.equals(rtAssoc.getSubnetId())))
            .collect(Collectors.toList());
    if (matchingRouteTables.size() > 1) {
      List<String> tableIds =
          matchingRouteTables.stream().map(RouteTable::getId).collect(Collectors.toList());
      throw new BatfishException(
          String.format(
              "Found multiple associated route tables %s for subnet %s", tableIds, _subnetId));
    }

    if (matchingRouteTables.size() == 1) {
      return matchingRouteTables.get(0);
    }

    // If no route table has an association with this subnet, find the unique main routing table.
    List<RouteTable> mainRouteTables =
        sameVpcTables
            .stream()
            .filter(
                (RouteTable rt) ->
                    rt.getAssociations().stream().anyMatch(RouteTableAssociation::isMain))
            .collect(Collectors.toList());

    if (mainRouteTables.isEmpty()) {
      throw new BatfishException("Could not find a route table for subnet " + _subnetId);
    }

    if (mainRouteTables.size() > 1) {
      List<String> tableIds =
          mainRouteTables.stream().map(RouteTable::getId).collect(Collectors.toList());
      throw new BatfishException(
          String.format("Found multiple main route tables %s for subnet %s", tableIds, _subnetId));
    }

    return mainRouteTables.get(0);
  }

  public Prefix getCidrBlock() {
    return _cidrBlock;
  }

  @Override
  public String getId() {
    return _subnetId;
  }

  public String getInternetGatewayId() {
    return _internetGatewayId;
  }

  public String getVpcId() {
    return _vpcId;
  }

  public String getVpnGatewayId() {
    return _vpnGatewayId;
  }

  public Configuration toConfigurationNode(AwsConfiguration awsConfiguration, Region region) {
    Configuration cfgNode = Utils.newAwsConfiguration(_subnetId);

    // add one interface that faces the instances
    String instancesIfaceName = _subnetId;
    Ip instancesIfaceIp = computeInstancesIfaceIp();
    InterfaceAddress instancesIfaceAddress =
        new InterfaceAddress(instancesIfaceIp, _cidrBlock.getPrefixLength());
    Utils.newInterface(instancesIfaceName, cfgNode, instancesIfaceAddress);

    // generate a prefix for the link between the VPC router and the subnet
    Pair<InterfaceAddress, InterfaceAddress> vpcSubnetLinkPrefix =
        awsConfiguration.getNextGeneratedLinkSubnet();
    InterfaceAddress subnetIfaceAddress = vpcSubnetLinkPrefix.getFirst();
    InterfaceAddress vpcIfaceAddress = vpcSubnetLinkPrefix.getSecond();

    // add an interface that faces the VPC router
    String subnetIfaceName = _vpcId;
    Interface subnetToVpc = Utils.newInterface(subnetIfaceName, cfgNode, subnetIfaceAddress);

    // add a corresponding interface on the VPC router facing the subnet
    Configuration vpcConfigNode = awsConfiguration.getConfigurationNodes().get(_vpcId);
    String vpcIfaceName = _subnetId;
    Utils.newInterface(vpcIfaceName, vpcConfigNode, vpcIfaceAddress);

    // add a static route on the vpc router for this subnet
    StaticRoute.Builder sb =
        StaticRoute.builder()
            .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
            .setMetric(Route.DEFAULT_STATIC_ROUTE_COST);
    StaticRoute vpcToSubnetRoute =
        sb.setNetwork(_cidrBlock).setNextHopIp(subnetIfaceAddress.getIp()).build();
    vpcConfigNode.getDefaultVrf().getStaticRoutes().add(vpcToSubnetRoute);

    // Install a default static route towards the VPC router.
    StaticRoute defaultRoute =
        sb.setNetwork(Prefix.ZERO).setNextHopIp(vpcIfaceAddress.getIp()).build();
    cfgNode.getDefaultVrf().getStaticRoutes().add(defaultRoute);

    NetworkAcl myNetworkAcl = findMyNetworkAcl(region.getNetworkAcls());

    IpAccessList inAcl = myNetworkAcl.getIngressAcl();
    IpAccessList outAcl = myNetworkAcl.getEgressAcl();
    cfgNode.getIpAccessLists().put(inAcl.getName(), inAcl);
    cfgNode.getIpAccessLists().put(outAcl.getName(), outAcl);

    subnetToVpc.setIncomingFilter(inAcl);
    subnetToVpc.setOutgoingFilter(outAcl);

    cfgNode.getVendorFamily().getAws().setVpcId(_vpcId);
    cfgNode.getVendorFamily().getAws().setSubnetId(_subnetId);
    cfgNode.getVendorFamily().getAws().setRegion(region.getName());

    return cfgNode;
  }
}

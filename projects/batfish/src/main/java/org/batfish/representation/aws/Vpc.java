package org.batfish.representation.aws;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.batfish.common.BatfishLogger;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Vpc implements AwsVpcEntity, Serializable {

  private static final long serialVersionUID = 1L;

  private Prefix _cidrBlock;

  private Set<Prefix> _cidrBlockAssociations;

  private transient String _internetGatewayId;

  private String _vpcId;

  private transient String _vpnGatewayId;

  public Vpc(JSONObject jObj, BatfishLogger logger) throws JSONException {
    _vpcId = jObj.getString(JSON_KEY_VPC_ID);
    _cidrBlock = Prefix.fromString(jObj.getString(JSON_KEY_CIDR_BLOCK));
    _cidrBlockAssociations = new HashSet<>();
    JSONArray cidrArray = jObj.getJSONArray(JSON_KEY_CIDR_BLOCK_ASSOCIATION_SET);
    for (int index = 0; index < cidrArray.length(); index++) {
      String cidrBlock = cidrArray.getJSONObject(index).getString(JSON_KEY_CIDR_BLOCK);
      _cidrBlockAssociations.add(Prefix.fromString(cidrBlock));
    }
  }

  public Prefix getCidrBlock() {
    return _cidrBlock;
  }

  public Set<Prefix> getCidrBlockAssociations() {
    return _cidrBlockAssociations;
  }

  @Override
  public String getId() {
    return _vpcId;
  }

  public String getInternetGatewayId() {
    return _internetGatewayId;
  }

  public String getVpnGatewayId() {
    return _vpnGatewayId;
  }

  public void setInternetGatewayId(String internetGatewayId) {
    _internetGatewayId = internetGatewayId;
  }

  public void setVpnGatewayId(String vpnGatewayId) {
    _vpnGatewayId = vpnGatewayId;
  }

  public Configuration toConfigurationNode(AwsConfiguration awsConfiguration, Region region) {
    Configuration cfgNode = Utils.newAwsConfiguration(_vpcId);
    cfgNode.getVendorFamily().getAws().setRegion(region.getName());
    cfgNode.getVendorFamily().getAws().setVpcId(_vpcId);
    cfgNode
        .getDefaultVrf()
        .getStaticRoutes()
        .add(
            StaticRoute.builder()
                .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
                .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
                .setNetwork(_cidrBlock)
                .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
                .build());

    // we only create a node here
    // interfaces are added to this node as we traverse subnets and
    // internetgateways

    return cfgNode;
  }
}

package org.batfish.representation.aws_vpcs;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.batfish.common.BatfishLogger;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class InternetGateway implements AwsVpcEntity, Serializable {

  private static final long serialVersionUID = 1L;

  private List<String> _attachmentVpcIds = new LinkedList<>();

  private String _internetGatewayId;

  public InternetGateway(JSONObject jObj, BatfishLogger logger) throws JSONException {
    _internetGatewayId = jObj.getString(JSON_KEY_INTERNET_GATEWAY_ID);

    JSONArray attachments = jObj.getJSONArray(JSON_KEY_ATTACHMENTS);
    for (int index = 0; index < attachments.length(); index++) {
      JSONObject childObject = attachments.getJSONObject(index);
      _attachmentVpcIds.add(childObject.getString(JSON_KEY_VPC_ID));
    }
  }

  @Override
  public String getId() {
    return _internetGatewayId;
  }

  public Configuration toConfigurationNode(AwsVpcConfiguration awsVpcConfiguration) {
    Configuration cfgNode = new Configuration(_internetGatewayId);
    cfgNode.setConfigurationFormat(ConfigurationFormat.AWS_VPC);

    for (String vpcId : _attachmentVpcIds) {

      String igwIfaceName = vpcId;
      Interface igwIface = new Interface(igwIfaceName, cfgNode);
      Prefix igwIfacePrefix = awsVpcConfiguration.getNextGeneratedLinkSubnet();
      igwIface.setPrefix(igwIfacePrefix);
      cfgNode.getInterfaces().put(igwIfaceName, igwIface);
      cfgNode.initDefaultVrf().getInterfaces().put(igwIfaceName, igwIface);

      // add the interface to the vpc router
      Configuration vpcConfigNode = awsVpcConfiguration.getConfigurationNodes().get(vpcId);
      vpcConfigNode.setConfigurationFormat(ConfigurationFormat.AWS_VPC);
      String vpcIfaceName = _internetGatewayId;
      Interface vpcIface = new Interface(vpcIfaceName, vpcConfigNode);
      Ip vpcIfaceIp = igwIfacePrefix.getEndAddress();
      Prefix vpcIfacePrefix = new Prefix(vpcIfaceIp, igwIfacePrefix.getPrefixLength());
      vpcIface.setPrefix(vpcIfacePrefix);
      vpcConfigNode.getInterfaces().put(vpcIfaceName, vpcIface);
      vpcConfigNode.initDefaultVrf().getInterfaces().put(vpcIfaceName, vpcIface);

      // associate this gateway with the vpc
      awsVpcConfiguration.getVpcs().get(vpcId).setInternetGatewayId(_internetGatewayId);

      // add a route on the gateway to the vpc
      Vpc vpc = awsVpcConfiguration.getVpcs().get(vpcId);
      StaticRoute igwVpcRoute =
          StaticRoute.builder()
              .setNetwork(vpc.getCidrBlock())
              .setNextHopIp(vpcIfaceIp)
              .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
              .setTag(Route.DEFAULT_STATIC_ROUTE_COST)
              .build();
      cfgNode.getDefaultVrf().getStaticRoutes().add(igwVpcRoute);
    }

    return cfgNode;
  }
}

package org.batfish.representation.aws;

import java.io.Serializable;
import org.batfish.common.BatfishLogger;
import org.batfish.datamodel.Prefix;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class VpcPeeringConnection implements AwsVpcEntity, Serializable {

  private static final long serialVersionUID = 1L;

  private final Prefix _accepterVpcCidrBlock;

  private final String _accepterVpcId;

  private final Prefix _requesterVpcCidrBlock;

  private final String _requesterVpcId;

  private final String _vpcPeeringConnectionId;

  public VpcPeeringConnection(JSONObject jObj, BatfishLogger logger) throws JSONException {
    _vpcPeeringConnectionId = jObj.getString(JSON_KEY_VPC_PEERING_CONNECTION_ID);

    JSONObject accepterJson = jObj.getJSONObject(JSON_KEY_ACCEPTER_VPC_INFO);
    _accepterVpcId = accepterJson.getString(JSON_KEY_VPC_ID);
    _accepterVpcCidrBlock = Prefix.fromString(accepterJson.getString(JSON_KEY_CIDR_BLOCK));

    JSONObject requesterJson = jObj.getJSONObject(JSON_KEY_REQUESTER_VPC_INFO);
    _requesterVpcId = requesterJson.getString(JSON_KEY_VPC_ID);
    _requesterVpcCidrBlock = Prefix.fromString(requesterJson.getString(JSON_KEY_CIDR_BLOCK));
  }

  public Prefix getAccepterVpcCidrBlock() {
    return _accepterVpcCidrBlock;
  }

  public String getAccepterVpcId() {
    return _accepterVpcId;
  }

  @Override
  public String getId() {
    return _vpcPeeringConnectionId;
  }

  public Prefix getRequesterVpcCidrBlock() {
    return _requesterVpcCidrBlock;
  }

  public String getRequesterVpcId() {
    return _requesterVpcId;
  }

  public String getVpcPeeringConnectionId() {
    return _vpcPeeringConnectionId;
  }
}

package org.batfish.representation.aws;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Prefix;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class VpcTest {

  @Test
  public void cidrBlocks() throws JSONException {
    String vpcs =
        CommonUtil.readResource("org/batfish/representation/aws/VpcTest-multipleCidrBlocks.json");

    JSONObject jObj = new JSONObject(vpcs);
    JSONArray vpcArray = jObj.getJSONArray("Vpcs");

    Vpc vpc = new Vpc(vpcArray.getJSONObject(0), null);

    Prefix p1 = Prefix.fromString("10.100.0.0/16");
    Prefix p2 = Prefix.fromString("10.200.0.0/16");
    assertThat(vpc.getCidrBlock(), equalTo(p1));
    assertThat(vpc.getCidrBlockAssociations(), hasSize(2));
    assertThat(vpc.getCidrBlockAssociations(), hasItem(p1));
    assertThat(vpc.getCidrBlockAssociations(), hasItem(p2));
  }
}

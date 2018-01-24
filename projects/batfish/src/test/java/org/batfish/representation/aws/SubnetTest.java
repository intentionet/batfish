package org.batfish.representation.aws;

import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_SUBNETS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.LinkedList;
import java.util.List;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class SubnetTest {

  private List<Subnet> _subnetList;

  @Before
  public void setup() throws JSONException {
    String text = CommonUtil.readResource("org/batfish/representation/aws/SubnetTest.json");
    JSONObject jObj = new JSONObject(text);
    JSONArray subnetArray = jObj.getJSONArray(JSON_KEY_SUBNETS);
    _subnetList = new LinkedList<>();
    for (int i = 0; i < subnetArray.length(); i++) {
      _subnetList.add(new Subnet(subnetArray.getJSONObject(i), null));
    }
  }

  @Test
  public void testSubnet() throws JSONException {
    // checking the count of subnets initialized
    assertThat(_subnetList.size(), equalTo(1));

    // checking the different attributes of subnet
    Subnet subnet = _subnetList.get(0);
    assertThat(subnet.getCidrBlock(), equalTo(Prefix.parse("172.31.0.0/20")));
    assertThat(subnet.getId(), equalTo("subnet-1"));
    assertThat(subnet.getVpcId(), equalTo("vpc-1"));
  }

  @Test
  public void testGetNextIp() {
    // checking the count of subnets initialized
    assertThat(_subnetList.size(), equalTo(1));

    // test getNextIp()
    Subnet subnet = _subnetList.get(0);
    assertThat(subnet.getNextIp(), equalTo(new Ip("172.31.0.2")));
    assertThat(subnet.getNextIp(), equalTo(new Ip("172.31.0.3")));
    assertThat(subnet.getNextIp(), equalTo(new Ip("172.31.0.4")));
  }
}

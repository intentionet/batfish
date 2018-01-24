package org.batfish.representation.aws;

import static org.batfish.representation.aws.AwsVpcEntity.JSON_KEY_DB_INSTANCES;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.LinkedList;
import java.util.List;
import org.batfish.common.util.CommonUtil;
import org.batfish.representation.aws.RdsInstance.Status;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class RdsInstanceTest {

  @Test
  public void testRdsInstance() throws JSONException {
    String text = CommonUtil.readResource("org/batfish/representation/aws/RdsInstanceTest.json");

    JSONObject jObj = new JSONObject(text);
    JSONArray rdsArray = jObj.getJSONArray(JSON_KEY_DB_INSTANCES);
    List<RdsInstance> rdsList = new LinkedList<>();
    for (int i = 0; i < rdsArray.length(); i++) {
      rdsList.add(new RdsInstance(rdsArray.getJSONObject(i), null));
    }

    // checking the count of RDS initialized
    assertThat(rdsList.size(), equalTo(1));

    RdsInstance rdsInstance = rdsList.get(0);

    // checking the attributes of this RDS instance
    assertThat(rdsInstance.getId(), equalTo("test-db"));
    assertThat(rdsInstance.getDbInstanceStatus(), equalTo(Status.AVAILABLE));
    assertThat(rdsInstance.getAvailabilityZone(), equalTo("us-west-2b"));
    Multimap<String, String> testMap = ArrayListMultimap.create();
    testMap.put("us-west-2b", "subnet-1");
    assertThat(rdsInstance.getAzSubnetIds(), equalTo(testMap));
    assertThat(rdsInstance.getSecurityGroups(), equalTo(ImmutableList.of("sg-12345")));
    assertThat(rdsInstance.getMultiAz(), equalTo(false));
    assertThat(rdsInstance.getVpcId(), equalTo("vpc-1"));
  }
}

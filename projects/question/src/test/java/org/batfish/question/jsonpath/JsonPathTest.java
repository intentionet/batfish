package org.batfish.question.jsonpath;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import org.batfish.common.util.CommonUtil;
import org.junit.Test;

/** Test JsonPath functionality */
public class JsonPathTest {

  private Configuration _baseConfiguration;

  private Object _oneNtpServerNodesAnswerJsonObject;

  private String _oneNtpServerNodesAnswerStr;

  private Configuration _prefixConfiguration;

  private Object _twoNtpServersNodesAnswerJsonObject;

  private String _twoNtpServersNodesAnswerStr;

  public JsonPathTest() {
    Configuration.setDefaults(BatfishJsonPathDefaults.INSTANCE);
    ConfigurationBuilder b = new ConfigurationBuilder();
    _baseConfiguration = b.build();
    _oneNtpServerNodesAnswerStr =
        CommonUtil.readResource("org/batfish/question/jsonpath/oneNtpServer.json");
    _oneNtpServerNodesAnswerJsonObject =
        JsonPath.parse(_oneNtpServerNodesAnswerStr, _baseConfiguration).json();
    _twoNtpServersNodesAnswerStr =
        CommonUtil.readResource("org/batfish/question/jsonpath/twoNtpServers.json");
    _twoNtpServersNodesAnswerJsonObject =
        JsonPath.parse(_twoNtpServersNodesAnswerStr, _baseConfiguration).json();
    ConfigurationBuilder prefixCb = new ConfigurationBuilder();
    prefixCb.mappingProvider(_baseConfiguration.mappingProvider());
    prefixCb.jsonProvider(_baseConfiguration.jsonProvider());
    prefixCb.evaluationListener(_baseConfiguration.getEvaluationListeners());
    prefixCb.options(_baseConfiguration.getOptions());
    prefixCb.options(Option.ALWAYS_RETURN_LIST);
    prefixCb.options(Option.AS_PATH_LIST);
    _prefixConfiguration = prefixCb.build();
  }

  @Test
  public void testOneNtpServerPresent() {
    String path = "$.nodes[*][?(!([\"1.2.3.4\"] subsetof @.ntpServers))].ntpServers";
    JsonPath jsonPath = JsonPath.compile(path);
    ArrayNode prefixes = null;
    try {
      prefixes = jsonPath.read(_oneNtpServerNodesAnswerJsonObject, _prefixConfiguration);
    } catch (PathNotFoundException e) {
      prefixes = JsonNodeFactory.instance.arrayNode();
    }
    assertThat(prefixes, not(equalTo(nullValue())));
    assertThat(prefixes.size(), equalTo(0));
  }

  @Test
  public void testOneNtpServerPresentViolation() {
    String path = "$.nodes[*][?(!([\"9.9.9.9\"] subsetof @.ntpServers))].ntpServers";
    JsonPath jsonPath = JsonPath.compile(path);
    ArrayNode prefixes = null;
    try {
      prefixes = jsonPath.read(_oneNtpServerNodesAnswerJsonObject, _prefixConfiguration);
    } catch (PathNotFoundException e) {
      prefixes = JsonNodeFactory.instance.arrayNode();
    }
    assertThat(prefixes, not(equalTo(nullValue())));
    assertThat(prefixes.size(), equalTo(1));
  }

  @Test
  public void testOneNtpServerSanctioned() {
    String path = "$.nodes[*].ntpServers[?(@ nin [\"1.2.3.4\"])]";
    JsonPath jsonPath = JsonPath.compile(path);
    ArrayNode prefixes = null;
    try {
      prefixes = jsonPath.read(_oneNtpServerNodesAnswerJsonObject, _prefixConfiguration);
    } catch (PathNotFoundException e) {
      prefixes = JsonNodeFactory.instance.arrayNode();
    }
    assertThat(prefixes, not(equalTo(nullValue())));
    assertThat(prefixes.size(), equalTo(0));
  }

  @Test
  public void testOneNtpServerSanctionedViolation() {
    String path = "$.nodes[*].ntpServers[?(@ nin [\"9.9.9.9\"])]";
    JsonPath jsonPath = JsonPath.compile(path);
    ArrayNode prefixes = null;
    try {
      prefixes = jsonPath.read(_oneNtpServerNodesAnswerJsonObject, _prefixConfiguration);
    } catch (PathNotFoundException e) {
      prefixes = JsonNodeFactory.instance.arrayNode();
    }
    assertThat(prefixes, not(equalTo(nullValue())));
    assertThat(prefixes.size(), equalTo(1));
  }

  @Test
  public void testPath1() {
    JsonPath jsonPath =
        JsonPath.compile(
            "$.nodes[*][?(@.interfaces['Management1'].prefix)].vendorFamily.cisco.logging"
                + "[?(@.sourceInterface!='Management1')]");
    assertThat(jsonPath, not(equalTo(nullValue())));
  }

  @Test
  public void testPath2() {
    JsonPath jsonPath =
        JsonPath.compile(
            "$.nodes[*][?(@.interfaces['Management1'].prefix)].vendorFamily.cisco.ntp.servers[*]"
                + "[?(@.vrf!='mgmt')]");
    assertThat(jsonPath, not(equalTo(nullValue())));
  }

  @Test
  public void testPath3() {
    JsonPath jsonPath = JsonPath.compile("$.nodes[*][?(!@.ntpServers)]");
    assertThat(jsonPath, not(equalTo(nullValue())));
  }

  @Test
  public void testPath4() {
    JsonPath jsonPath =
        JsonPath.compile("$.nodes[*][?([\"1.2.3.4\"] subsetof @.loggingServers)].loggingServers");
    assertThat(jsonPath, not(equalTo(nullValue())));
  }

  @Test
  public void testTwoNtpServersPresent() {
    String path = "$.nodes[*][?(!([\"1.2.3.4\", \"5.6.7.8\"] subsetof @.ntpServers))].ntpServers";
    JsonPath jsonPath = JsonPath.compile(path);
    ArrayNode prefixes = null;
    try {
      prefixes = jsonPath.read(_twoNtpServersNodesAnswerJsonObject, _prefixConfiguration);
    } catch (PathNotFoundException e) {
      prefixes = JsonNodeFactory.instance.arrayNode();
    }
    assertThat(prefixes, not(equalTo(nullValue())));
    assertThat(prefixes.size(), equalTo(0));
  }

  @Test
  public void testTwoNtpServersPresentViolation() {
    String path = "$.nodes[*][?(!([\"1.2.3.4\", \"9.9.9.9\"] subsetof @.ntpServers))].ntpServers";
    JsonPath jsonPath = JsonPath.compile(path);
    ArrayNode prefixes = null;
    try {
      prefixes = jsonPath.read(_twoNtpServersNodesAnswerJsonObject, _prefixConfiguration);
    } catch (PathNotFoundException e) {
      prefixes = JsonNodeFactory.instance.arrayNode();
    }
    assertThat(prefixes, not(equalTo(nullValue())));
    assertThat(prefixes.size(), equalTo(1));
  }

  @Test
  public void testTwoNtpServersSanctioned() {
    String path = "$.nodes[*].ntpServers[?(@ nin [\"1.2.3.4\", \"5.6.7.8\"])]";
    JsonPath jsonPath = JsonPath.compile(path);
    ArrayNode prefixes = null;
    try {
      prefixes = jsonPath.read(_twoNtpServersNodesAnswerJsonObject, _prefixConfiguration);
    } catch (PathNotFoundException e) {
      prefixes = JsonNodeFactory.instance.arrayNode();
    }
    assertThat(prefixes, not(equalTo(nullValue())));
    assertThat(prefixes.size(), equalTo(0));
  }

  @Test
  public void testTwoNtpServersSanctionedViolation() {
    String path = "$.nodes[*].ntpServers[?(@ nin [\"9.9.9.9\", \"5.6.7.8\"])]";
    JsonPath jsonPath = JsonPath.compile(path);
    ArrayNode prefixes = null;
    try {
      prefixes = jsonPath.read(_twoNtpServersNodesAnswerJsonObject, _prefixConfiguration);
    } catch (PathNotFoundException e) {
      prefixes = JsonNodeFactory.instance.arrayNode();
    }
    assertThat(prefixes, not(equalTo(nullValue())));
    assertThat(prefixes.size(), equalTo(1));
  }
}

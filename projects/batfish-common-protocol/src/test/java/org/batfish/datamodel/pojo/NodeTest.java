package org.batfish.datamodel.pojo;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.DeviceType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NodeTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void constructorFail() throws IOException {
    String nodeStr = "{\"nonamefield\" : \"nodeName\"}";
    _thrown.expect(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    BatfishObjectMapper.mapper().readValue(nodeStr, Node.class);
  }

  @Test
  public void constructorBasic() throws IOException {
    String nodeStr = "{\"name\" : \"nodeName\"}";
    Node node = BatfishObjectMapper.mapper().readValue(nodeStr, Node.class);

    assertThat(node.getId(), equalTo(Node.getId("nodeName")));
    assertThat(node.getName(), equalTo("nodeName"));
  }

  @Test
  public void constructorProperties() throws IOException {
    String nodeStr = "{\"name\" : \"nodeName\", \"properties\" : { \"key\": \"value\"}}";
    Node node = BatfishObjectMapper.mapper().readValue(nodeStr, Node.class);

    assertThat(node.getId(), equalTo(Node.getId("nodeName")));
    assertThat(node.getName(), equalTo("nodeName"));
    assertThat(node.getProperties().size(), equalTo(1));
    assertThat(node.getProperties().get("key"), equalTo("value"));
  }

  @Test
  public void serialization() {
    Node node = new Node("testnode", DeviceType.HOST);
    Map<String, String> properties = new HashMap<>();
    properties.put("key", "value");
    node.setProperties(properties);
    JsonNode jsonNode = BatfishObjectMapper.mapper().valueToTree(node);

    assertThat(jsonNode.get("id").asText(), equalTo(Node.getId("testnode")));
    assertThat(jsonNode.get("name").asText(), equalTo("testnode"));
    assertThat(jsonNode.get("type").asText(), equalTo("HOST"));
    assertThat(jsonNode.get("properties").get("key").asText(), equalTo("value"));
  }
}

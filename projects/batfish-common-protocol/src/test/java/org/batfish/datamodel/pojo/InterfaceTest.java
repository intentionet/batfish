package org.batfish.datamodel.pojo;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.pojo.Interface.InterfaceType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class InterfaceTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void constructorFail() throws IOException {
    String ifaceStr = "{\"nofield\" : \"test\"}";
    _thrown.expect(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    BatfishObjectMapper.mapper().readValue(ifaceStr, Interface.class);
  }

  @Test
  public void constructorBasic() throws IOException {
    String ifaceStr = "{\"nodeId\" : \"node\", \"name\" : \"iname\"}";
    Interface iface = BatfishObjectMapper.mapper().readValue(ifaceStr, Interface.class);

    assertThat(iface.getId(), equalTo(Interface.getId("node", "iname")));
    assertThat(iface.getNodeId(), equalTo("node"));
    assertThat(iface.getName(), equalTo("iname"));
  }

  @Test
  public void constructorProperties() throws IOException {
    String ifaceStr =
        "{\"nodeId\" : \"node\", \"name\" : \"name\", \"properties\" : { \"key\": \"value\"}}";
    Interface iface = BatfishObjectMapper.mapper().readValue(ifaceStr, Interface.class);

    assertThat(iface.getId(), equalTo(Interface.getId("node", "name")));
    assertThat(iface.getNodeId(), equalTo("node"));
    assertThat(iface.getName(), equalTo("name"));
    assertThat(iface.getProperties().size(), equalTo(1));
    assertThat(iface.getProperties().get("key"), equalTo("value"));
  }

  @Test
  public void serialization() {
    Interface iface = new Interface("node", "name", InterfaceType.PHYSICAL);
    Map<String, String> properties = new HashMap<>();
    properties.put("key", "value");
    iface.setProperties(properties);
    JsonNode jsonNode = BatfishObjectMapper.mapper().valueToTree(iface);

    assertThat(jsonNode.get("id").asText(), equalTo(Interface.getId("node", "name")));
    assertThat(jsonNode.get("nodeId").asText(), equalTo("node"));
    assertThat(jsonNode.get("name").asText(), equalTo("name"));
    assertThat(jsonNode.get("type").asText(), equalTo("PHYSICAL"));
    assertThat(jsonNode.get("properties").get("key").asText(), equalTo("value"));
  }
}

package org.batfish.datamodel.pojo;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.pojo.Aggregate.AggregateType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AggregateTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void constructorFail() throws IOException {
    String aggStr = "{\"nonamefield\" : \"nodeName\"}";
    _thrown.expect(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    BatfishObjectMapper.mapper().readValue(aggStr, Aggregate.class);
  }

  @Test
  public void constructorBasic() throws IOException {
    String aggStr = "{\"name\" : \"test\"}";
    Aggregate aggregate = BatfishObjectMapper.mapper().readValue(aggStr, Aggregate.class);

    assertThat(aggregate.getId(), equalTo(Aggregate.getId("test")));
    assertThat(aggregate.getName(), equalTo("test"));
  }

  @Test
  public void constructorProperties() throws IOException {
    String aggStr = "{\"name\" : \"aggName\", \"properties\" : { \"key\": \"value\"}}";
    Aggregate node = BatfishObjectMapper.mapper().readValue(aggStr, Aggregate.class);

    assertThat(node.getId(), equalTo(Aggregate.getId("aggName")));
    assertThat(node.getName(), equalTo("aggName"));
    assertThat(node.getProperties().size(), equalTo(1));
    assertThat(node.getProperties().get("key"), equalTo("value"));
  }

  @Test
  public void serialization() {
    Aggregate aggregate = new Aggregate("test", AggregateType.REGION);
    Map<String, String> properties = new HashMap<>();
    properties.put("key", "value");
    aggregate.setProperties(properties);
    JsonNode jsonNode = BatfishObjectMapper.mapper().valueToTree(aggregate);

    assertThat(jsonNode.get("id").asText(), equalTo(Aggregate.getId("test")));
    assertThat(jsonNode.get("name").asText(), equalTo("test"));
    assertThat(jsonNode.get("type").asText(), equalTo("REGION"));
    assertThat(jsonNode.get("properties").get("key").asText(), equalTo("value"));
  }
}

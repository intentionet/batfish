package org.batfish.datamodel.pojo;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.pojo.Link.LinkType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LinkTest {

  @Rule public ExpectedException _thrown = ExpectedException.none();

  @Test
  public void constructorFail() throws IOException {
    String linkStr = "{\"nofield\" : \"test\"}";
    _thrown.expect(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    BatfishObjectMapper.mapper().readValue(linkStr, Link.class);
  }

  @Test
  public void constructorBasic() throws IOException {
    String linkStr = "{\"srcId\" : \"src\", \"dstId\" : \"dst\"}";
    Link link = BatfishObjectMapper.mapper().readValue(linkStr, Link.class);

    assertThat(link.getId(), equalTo(Link.getId("src", "dst")));
    assertThat(link.getDstId(), equalTo("dst"));
    assertThat(link.getSrcId(), equalTo("src"));
  }

  @Test
  public void constructorProperties() throws IOException {
    String linkStr =
        "{\"srcId\" : \"src\", \"dstId\" : \"dst\", \"properties\" : { \"key\": \"value\"}}";
    Link link = BatfishObjectMapper.mapper().readValue(linkStr, Link.class);

    assertThat(link.getId(), equalTo(Link.getId("src", "dst")));
    assertThat(link.getDstId(), equalTo("dst"));
    assertThat(link.getSrcId(), equalTo("src"));
    assertThat(link.getProperties().size(), equalTo(1));
    assertThat(link.getProperties().get("key"), equalTo("value"));
  }

  @Test
  public void serialization() {
    Link link = new Link("src", "dst", LinkType.PHYSICAL);
    Map<String, String> properties = new HashMap<>();
    properties.put("key", "value");
    link.setProperties(properties);
    JsonNode jsonNode = BatfishObjectMapper.mapper().valueToTree(link);

    assertThat(jsonNode.get("id").asText(), equalTo(Link.getId("src", "dst")));
    assertThat(jsonNode.get("srcId").asText(), equalTo("src"));
    assertThat(jsonNode.get("dstId").asText(), equalTo("dst"));
    assertThat(jsonNode.get("type").asText(), equalTo("PHYSICAL"));
    assertThat(jsonNode.get("properties").get("key").asText(), equalTo("value"));
  }
}

package org.batfish.datamodel.pojo;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.pojo.Aggregate.AggregateType;
import org.junit.Test;

public class TopologyTest {

  @Test
  public void constructor() throws IOException {
    String str =
        "{\"testrigName\" : \"testrig\","
            + " \"nodes\" : [{\"name\": \"node\"}], "
            + " \"links\" : [{\"srcId\" : \"src\", \"dstId\" : \"dst\"}],"
            + " \"interfaces\" : [{\"nodeId\": \"node\", \"name\" : \"node\"}],"
            + " \"aggregates\" : [{\"name\": \"cloud\", \"contents\" : [\"id1\"]}]}";
    Topology topo = BatfishObjectMapper.mapper().readValue(str, Topology.class);

    assertThat(topo.getId(), equalTo(Topology.getId("testrig")));
    assertThat(topo.getTestrigName(), equalTo("testrig"));
    assertThat(topo.getAggregates().size(), equalTo(1));
    assertThat(
        topo.getOrCreateAggregate("cloud", AggregateType.CLOUD).getContents().size(), equalTo(1));
    assertThat(topo.getInterfaces().size(), equalTo(1));
    assertThat(topo.getLinks().size(), equalTo(1));
    assertThat(topo.getNodes().size(), equalTo(1));
  }

  @Test
  public void serialization() {
    Topology topo = new Topology("testrig");
    topo.getAggregates().add(new Aggregate("cloud"));
    topo.getInterfaces().add(new Interface("node", "name"));
    topo.getLinks().add(new Link("src", "dst"));
    topo.getNodes().add(new Node("node"));
    Map<String, String> properties = new HashMap<>();
    properties.put("key", "value");
    topo.setProperties(properties);
    JsonNode jsonNode = BatfishObjectMapper.mapper().valueToTree(topo);

    assertThat(jsonNode.get("id").asText(), equalTo(Topology.getId("testrig")));
    assertThat(jsonNode.get("aggregates").get(0).get("name").asText(), equalTo("cloud"));
    assertThat(jsonNode.get("interfaces").get(0).get("nodeId").asText(), equalTo("node"));
    assertThat(jsonNode.get("interfaces").get(0).get("name").asText(), equalTo("name"));
    assertThat(jsonNode.get("links").get(0).get("srcId").asText(), equalTo("src"));
    assertThat(jsonNode.get("links").get(0).get("dstId").asText(), equalTo("dst"));
    assertThat(jsonNode.get("nodes").get(0).get("name").asText(), equalTo("node"));
    assertThat(jsonNode.get("properties").get("key").asText(), equalTo("value"));
  }
}

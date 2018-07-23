package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.common.Pair;
import org.batfish.datamodel.collections.NodeInterfacePair;

public class Edge extends Pair<NodeInterfacePair, NodeInterfacePair> {

  private static final String PROP_INT1 = "node1interface";

  private static final String PROP_INT2 = "node2interface";

  private static final String PROP_NODE1 = "node1";

  private static final String PROP_NODE2 = "node2";

  private static final long serialVersionUID = 1L;

  public Edge(NodeInterfacePair p1, NodeInterfacePair p2) {
    super(p1, p2);
  }

  @JsonCreator
  public Edge(
      @JsonProperty(PROP_NODE1) String node1,
      @JsonProperty(PROP_INT1) String int1,
      @JsonProperty(PROP_NODE2) String node2,
      @JsonProperty(PROP_INT2) String int2) {
    super(new NodeInterfacePair(node1, int1), new NodeInterfacePair(node2, int2));
  }

  public Edge(Interface i1, Interface i2) {
    this(i1.getOwner().getHostname(), i1.getName(), i2.getOwner().getHostname(), i2.getName());
  }

  @JsonProperty(PROP_INT1)
  public String getInt1() {
    return _first.getInterface();
  }

  @JsonProperty(PROP_INT2)
  public String getInt2() {
    return _second.getInterface();
  }

  @JsonIgnore
  public NodeInterfacePair getInterface1() {
    return _first;
  }

  @JsonIgnore
  public NodeInterfacePair getInterface2() {
    return _second;
  }

  @JsonProperty(PROP_NODE1)
  public String getNode1() {
    return _first.getHostname();
  }

  @JsonProperty(PROP_NODE2)
  public String getNode2() {
    return _second.getHostname();
  }

  @Override
  public String toString() {
    return "<" + getNode1() + ":" + getInt1() + ", " + getNode2() + ":" + getInt2() + ">";
  }
}

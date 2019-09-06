package org.batfish.datamodel;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.collections.NodeInterfacePair;

/** Represents a directed edge between two {@link NodeInterfacePair}s */
@ParametersAreNonnullByDefault
public class Edge implements Serializable, Comparable<Edge> {
  private static final String PROP_INT1 = "node1interface";
  private static final String PROP_INT2 = "node2interface";
  private static final String PROP_NODE1 = "node1";
  private static final String PROP_NODE2 = "node2";

  @Nonnull private final NodeInterfacePair _tail;
  @Nonnull private final NodeInterfacePair _head;

  /**
   * Create a new directed edge connecting two node/interface pairs from {@code tail} to {@code
   * head}.
   *
   * @param tail the edge tail
   * @param head the edge head
   */
  public Edge(NodeInterfacePair tail, NodeInterfacePair head) {
    _tail = tail;
    _head = head;
  }

  @JsonCreator
  private static Edge create(
      @Nullable @JsonProperty(PROP_NODE1) String node1,
      @Nullable @JsonProperty(PROP_INT1) String int1,
      @Nullable @JsonProperty(PROP_NODE2) String node2,
      @Nullable @JsonProperty(PROP_INT2) String int2) {
    checkArgument(!Strings.isNullOrEmpty(node1), "Missing %s", PROP_NODE1);
    checkArgument(!Strings.isNullOrEmpty(int1), "Missing %s", PROP_INT1);
    checkArgument(!Strings.isNullOrEmpty(node2), "Missing %s", PROP_NODE2);
    checkArgument(!Strings.isNullOrEmpty(int2), "Missing %s", PROP_INT2);
    return new Edge(NodeInterfacePair.of(node1, int1), NodeInterfacePair.of(node2, int2));
  }

  /** Create an Edge from {@link Interface}s */
  public Edge(Interface tail, Interface head) {
    this(NodeInterfacePair.of(tail), NodeInterfacePair.of(head));
  }

  /** Create an edge from names of nodes and interfaces */
  public static Edge of(
      String tailNode, String tailInterface, String headNode, String headInterface) {
    return create(tailNode, tailInterface, headNode, headInterface);
  }

  @JsonProperty(PROP_INT1)
  public String getInt1() {
    return _tail.getInterface();
  }

  @JsonProperty(PROP_INT2)
  public String getInt2() {
    return _head.getInterface();
  }

  /** @deprecated Use {@link #getTail()} */
  @Deprecated
  @JsonIgnore
  @Nonnull
  public NodeInterfacePair getInterface1() {
    return getTail();
  }

  @JsonIgnore
  @Nonnull
  public NodeInterfacePair getTail() {
    return _tail;
  }

  /** @deprecated Use {@link #getHead()} */
  @JsonIgnore
  @Deprecated
  @Nonnull
  public NodeInterfacePair getInterface2() {
    return getHead();
  }

  @JsonIgnore
  @Nonnull
  public NodeInterfacePair getHead() {
    return _head;
  }

  /** Return a new edge, pointing in the reverse direction */
  public Edge reverse() {
    return new Edge(_head, _tail);
  }

  @JsonProperty(PROP_NODE1)
  public String getNode1() {
    return _tail.getHostname();
  }

  @JsonProperty(PROP_NODE2)
  public String getNode2() {
    return _head.getHostname();
  }

  @Override
  public int compareTo(Edge other) {
    return Comparator.comparing(Edge::getTail).thenComparing(Edge::getHead).compare(this, other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Edge)) {
      return false;
    }
    Edge edge = (Edge) o;
    return Objects.equals(_tail, edge._tail) && Objects.equals(_head, edge._head);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_tail, _head);
  }

  @Override
  public String toString() {
    return "<"
        + getTail().getHostname()
        + ":"
        + getTail().getInterface()
        + ", "
        + getHead().getHostname()
        + ":"
        + getHead().getInterface()
        + ">";
  }
}

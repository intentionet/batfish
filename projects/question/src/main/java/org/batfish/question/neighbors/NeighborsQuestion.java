package org.batfish.question.neighbors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import org.batfish.datamodel.NeighborType;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;

/**
 * Lists neighbor relationships in the network.
 *
 * @param neighborType The type(s) of neighbor relationships to focus. Default is IP.
 * @param nodes Regular expression to match the nodes names for one end of pair. Default is '.*'
 *     (all nodes).
 * @param remoteNodes Regular expression to match the nodes names for the other end of the pair.
 *     Default is '.*' (all nodes).
 * @example bf_answer("Neighbors", neighborType="ibgp" nodes="as1.*", remodeNodes="as2.*") Shows all
 *     iBGP neighbor relationships between nodes that start with as1 and those that start with as2.
 */
public class NeighborsQuestion extends Question {

  private static final String PROP_NEIGHBOR_TYPE = "neighborType";

  private static final String PROP_NODES = "nodes";

  private static final String PROP_REMOTE_NODES = "remoteNodes";

  @Nonnull private final NeighborType _neighborType;

  @Nonnull private final NodesSpecifier _nodes;

  @Nonnull private final NodesSpecifier _remoteNodes;

  @JsonCreator
  public NeighborsQuestion(
      @JsonProperty(PROP_NODES) NodesSpecifier nodes,
      @JsonProperty(PROP_REMOTE_NODES) NodesSpecifier remoteNodes,
      @JsonProperty(PROP_NEIGHBOR_TYPE) NeighborType neighborType) {
    _nodes = nodes == null ? NodesSpecifier.ALL : nodes;
    _remoteNodes = remoteNodes == null ? NodesSpecifier.ALL : remoteNodes;
    _neighborType = neighborType == null ? NeighborType.LAYER3 : neighborType;
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "neighbors2";
  }

  @JsonProperty(PROP_NEIGHBOR_TYPE)
  public NeighborType getNeighborType() {
    return _neighborType;
  }

  @JsonProperty(PROP_NODES)
  public NodesSpecifier getNodes() {
    return _nodes;
  }

  @JsonProperty(PROP_REMOTE_NODES)
  public NodesSpecifier getRemoteNodes() {
    return _remoteNodes;
  }
}

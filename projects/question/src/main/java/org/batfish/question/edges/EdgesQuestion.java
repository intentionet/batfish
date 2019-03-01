package org.batfish.question.edges;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import org.batfish.datamodel.EdgeType;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;

/** Lists neighbor relationships in the network as edges. */
public class EdgesQuestion extends Question {

  private static final String PROP_EDGE_TYPE = "edgeType";

  private static final String PROP_NODES = "nodes";

  private static final String PROP_REMOTE_NODES = "remoteNodes";

  @Nonnull private final EdgeType _edgeType;

  @Nonnull private final NodesSpecifier _nodes;

  @Nonnull private final NodesSpecifier _remoteNodes;

  @JsonCreator
  public EdgesQuestion(
      @JsonProperty(PROP_NODES) NodesSpecifier nodes,
      @JsonProperty(PROP_REMOTE_NODES) NodesSpecifier remoteNodes,
      @JsonProperty(PROP_EDGE_TYPE) EdgeType edgeType) {
    _nodes = nodes == null ? NodesSpecifier.ALL : nodes;
    _remoteNodes = remoteNodes == null ? NodesSpecifier.ALL : remoteNodes;
    _edgeType = edgeType == null ? EdgeType.LAYER3 : edgeType;
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "edges";
  }

  @JsonProperty(PROP_EDGE_TYPE)
  public EdgeType getEdgeType() {
    return _edgeType;
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

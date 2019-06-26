package org.batfish.question.edges;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/** Lists neighbor relationships in the network as edges. */
@ParametersAreNonnullByDefault
public class EdgesQuestion extends Question {
  private static final String PROP_EDGE_TYPE = "edgeType";
  private static final String PROP_INITIAL = "initial";
  private static final String PROP_NODES = "nodes";
  private static final String PROP_REMOTE_NODES = "remoteNodes";

  @Nonnull private final EdgeType _edgeType;
  private final boolean _initial;
  @Nullable private final String _nodes;
  @Nullable private final String _remoteNodes;

  @JsonCreator
  private static EdgesQuestion create(
      @Nullable @JsonProperty(PROP_NODES) String nodes,
      @Nullable @JsonProperty(PROP_INITIAL) Boolean initial,
      @Nullable @JsonProperty(PROP_REMOTE_NODES) String remoteNodes,
      @Nullable @JsonProperty(PROP_EDGE_TYPE) EdgeType edgeType) {
    return new EdgesQuestion(
        nodes,
        remoteNodes,
        firstNonNull(edgeType, EdgeType.LAYER3),
        firstNonNull(initial, Boolean.FALSE));
  }

  public EdgesQuestion(
      @Nullable String nodes, @Nullable String remoteNodes, EdgeType edgeType, boolean initial) {
    _nodes = nodes;
    _initial = initial;
    _remoteNodes = remoteNodes;
    _edgeType = edgeType;
  }

  @Override
  public boolean getDataPlane() {
    return !_initial;
  }

  @Override
  public String getName() {
    return "edges";
  }

  @Nonnull
  @JsonProperty(PROP_EDGE_TYPE)
  public EdgeType getEdgeType() {
    return _edgeType;
  }

  /** Returns true iff this question uses the initial (pre-dataplane) topology. */
  @JsonProperty(PROP_INITIAL)
  public boolean getInitial() {
    return _initial;
  }

  @Nullable
  @JsonProperty(PROP_NODES)
  public String getNodes() {
    return _nodes;
  }

  @Nonnull
  @JsonIgnore
  public NodeSpecifier getNodeSpecifier() {
    return SpecifierFactories.getNodeSpecifierOrDefault(_nodes, AllNodesNodeSpecifier.INSTANCE);
  }

  @Nullable
  @JsonProperty(PROP_REMOTE_NODES)
  public String getRemoteNodes() {
    return _remoteNodes;
  }

  @Nonnull
  @JsonIgnore
  public NodeSpecifier getRemoteNodeSpecifier() {
    return SpecifierFactories.getNodeSpecifierOrDefault(
        _remoteNodes, AllNodesNodeSpecifier.INSTANCE);
  }

  public enum EdgeType {
    BGP,
    EIGRP,
    IPSEC,
    ISIS,
    LAYER1,
    LAYER3,
    OSPF,
    VXLAN
  }
}

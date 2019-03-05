package org.batfish.question.ospfsession;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/** A {@link Question} that returns compatible OSPF sessions */
public class OspfSessionCompatibilityQuestion extends Question {
  private static final String PROP_NODES = "nodes";

  private static final String PROP_REMOTE_NODES = "remoteNodes";

  @Nullable private String _nodes;

  @Nullable private String _remoteNodes;

  public OspfSessionCompatibilityQuestion(
      @JsonProperty(PROP_NODES) @Nullable String nodes,
      @JsonProperty(PROP_REMOTE_NODES) @Nullable String remoteNodes) {
    _nodes = nodes;
    _remoteNodes = remoteNodes;
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "ospfSessionCompatibility";
  }

  @JsonProperty(PROP_NODES)
  @Nullable
  public String getNodes() {
    return _nodes;
  }

  @JsonProperty(PROP_REMOTE_NODES)
  @Nullable
  public String getremoteNodes() {
    return _remoteNodes;
  }

  @JsonIgnore
  @Nonnull
  NodeSpecifier getNodesSpecifier() {
    return SpecifierFactories.getNodeSpecifierOrDefault(_nodes, AllNodesNodeSpecifier.INSTANCE);
  }

  @JsonIgnore
  @Nonnull
  NodeSpecifier getRemoteNodesSpecifier() {
    return SpecifierFactories.getNodeSpecifierOrDefault(
        _remoteNodes, AllNodesNodeSpecifier.INSTANCE);
  }
}

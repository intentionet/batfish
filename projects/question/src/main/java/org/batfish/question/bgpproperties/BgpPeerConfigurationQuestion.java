package org.batfish.question.bgpproperties;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.questions.BgpPeerPropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/**
 * A question that returns properties of BGP routing processes. {@link #_nodes} determines which
 * nodes are included. The default is to include everything.
 */
public class BgpPeerConfigurationQuestion extends Question {

  private static final String PROP_NODES = "nodes";
  private static final String PROP_PROPERTIES = "properties";

  @Nullable private String _nodes;
  @Nonnull private BgpPeerPropertySpecifier _properties;

  @JsonCreator
  public BgpPeerConfigurationQuestion(
      @JsonProperty(PROP_NODES) @Nullable String nodes,
      @JsonProperty(PROP_PROPERTIES) @Nullable BgpPeerPropertySpecifier properties) {
    _nodes = nodes;
    _properties = firstNonNull(properties, BgpPeerPropertySpecifier.ALL);
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "bgpPeerConfiguration";
  }

  @JsonProperty(PROP_NODES)
  @Nullable
  public String getNodes() {
    return _nodes;
  }

  @JsonProperty(PROP_PROPERTIES)
  @Nullable
  public BgpPeerPropertySpecifier getProperties() {
    return _properties;
  }

  @JsonIgnore
  @Nonnull
  NodeSpecifier getNodeSpecifier() {
    return SpecifierFactories.getNodeSpecifierOrDefault(_nodes, AllNodesNodeSpecifier.INSTANCE);
  }
}

package org.batfish.question.bgpproperties;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.questions.BgpProcessPropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/**
 * A question that returns properties of BGP routing processes. {@link #_nodes} and {@link
 * #_properties} determine which nodes and properties are included. The default is to include
 * everything.
 */
@ParametersAreNonnullByDefault
public class BgpProcessConfigurationQuestion extends Question {

  private static final String PROP_NODES = "nodes";
  private static final String PROP_PROPERTIES = "properties";

  @Nullable private String _nodes;
  @Nonnull private NodeSpecifier _nodeSpecifier;
  @Nonnull private BgpProcessPropertySpecifier _properties;

  @JsonCreator
  private static BgpProcessConfigurationQuestion create(
      @Nullable @JsonProperty(PROP_NODES) String nodes,
      @Nullable @JsonProperty(PROP_PROPERTIES) BgpProcessPropertySpecifier propertySpec) {
    return new BgpProcessConfigurationQuestion(
        nodes,
        SpecifierFactories.getNodeSpecifierOrDefault(nodes, AllNodesNodeSpecifier.INSTANCE),
        firstNonNull(propertySpec, BgpProcessPropertySpecifier.ALL));
  }

  public BgpProcessConfigurationQuestion(
      NodeSpecifier nodeSpecifier, BgpProcessPropertySpecifier properties) {
    this(null, nodeSpecifier, properties);
  }

  private BgpProcessConfigurationQuestion(
      @Nullable String nodes, NodeSpecifier nodeSpecifier, BgpProcessPropertySpecifier properties) {
    _nodes = nodes;
    _nodeSpecifier = nodeSpecifier;
    _properties = properties;
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "bgpProcessConfiguration";
  }

  @Nullable
  @JsonProperty(PROP_NODES)
  public String getNodes() {
    return _nodes;
  }

  @Nonnull
  @JsonIgnore
  public NodeSpecifier getNodeSpecifier() {
    return _nodeSpecifier;
  }

  @Nonnull
  @JsonProperty(PROP_PROPERTIES)
  public BgpProcessPropertySpecifier getProperties() {
    return _properties;
  }
}

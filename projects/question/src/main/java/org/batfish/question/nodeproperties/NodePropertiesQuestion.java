package org.batfish.question.nodeproperties;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.questions.NodePropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/**
 * A question that returns properties of nodes in a tabular format. {@link
 * NodePropertiesQuestion#_nodes} determines which nodes are included, and {@link
 * NodePropertiesQuestion#_properties} determines which properties are included.
 */
@ParametersAreNonnullByDefault
public class NodePropertiesQuestion extends Question {

  private static final String PROP_NODES = "nodes";
  private static final String PROP_PROPERTIES = "properties";

  @Nullable private String _nodes;

  @Nonnull private NodeSpecifier _nodeSpecifier;

  @Nonnull private NodePropertySpecifier _properties;

  @JsonCreator
  private static NodePropertiesQuestion create(
      @Nullable @JsonProperty(PROP_NODES) String nodes,
      @Nullable @JsonProperty(PROP_PROPERTIES) NodePropertySpecifier propertySpec) {
    return new NodePropertiesQuestion(
        nodes,
        SpecifierFactories.getNodeSpecifierOrDefault(nodes, AllNodesNodeSpecifier.INSTANCE),
        firstNonNull(propertySpec, NodePropertySpecifier.ALL));
  }

  public NodePropertiesQuestion(NodeSpecifier nodeSpecifier, NodePropertySpecifier propertySpec) {
    this(null, nodeSpecifier, propertySpec);
  }

  private NodePropertiesQuestion(
      @Nullable String nodes, NodeSpecifier nodeSpecifier, NodePropertySpecifier propertySpec) {
    _nodes = nodes;
    _nodeSpecifier = nodeSpecifier;
    _properties = propertySpec;
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "nodeproperties";
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
  public NodePropertySpecifier getProperties() {
    return _properties;
  }
}

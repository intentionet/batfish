package org.batfish.question.nodeproperties;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.batfish.datamodel.questions.NodePropertySpecifier;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;

/**
 * A question that returns properties of nodes in a tabular format. {@link
 * NodePropertiesQuestion#_nodeRegex} determines which nodes are included, and {@link
 * NodePropertiesQuestion#_propertySpec} determines which properties are included.
 */
public class NodePropertiesQuestion extends Question {

  private static final String PROP_NODE_REGEX = "nodeRegex";
  private static final String PROP_PROPERTY_SPEC = "propertySpec";

  @Nonnull private NodesSpecifier _nodeRegex;

  @Nonnull private NodePropertySpecifier _propertySpec;

  public NodePropertiesQuestion(
      @JsonProperty(PROP_NODE_REGEX) NodesSpecifier nodeRegex,
      @JsonProperty(PROP_PROPERTY_SPEC) NodePropertySpecifier propertySpec) {
    _nodeRegex = firstNonNull(nodeRegex, NodesSpecifier.ALL);
    _propertySpec = firstNonNull(propertySpec, NodePropertySpecifier.ALL);
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "nodeproperties";
  }

  @JsonProperty(PROP_NODE_REGEX)
  public NodesSpecifier getNodeRegex() {
    return _nodeRegex;
  }

  @JsonProperty(PROP_PROPERTY_SPEC)
  public NodePropertySpecifier getPropertySpec() {
    return _propertySpec;
  }

  @JsonProperty("properties")
  private void setProperties(List<String> properties) {
    _propertySpec = new NodePropertySpecifier(StringUtils.join(properties, ","));
  }
}

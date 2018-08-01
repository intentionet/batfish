package org.batfish.question.ospfproperties;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.questions.Question;

/**
 * A question that returns properties of OSPF routing processes. {@link #_nodeRegex} and {@link
 * #_propertySpec} determine which nodes and properties are included. The default is to include
 * everything.
 */
public class OspfPropertiesQuestion extends Question {

  private static final String PROP_NODE_REGEX = "nodeRegex";
  private static final String PROP_PROPERTY_SPEC = "propertySpec";

  @Nonnull private NodesSpecifier _nodeRegex;
  @Nonnull private OspfPropertySpecifier _propertySpec;

  public OspfPropertiesQuestion(
      @JsonProperty(PROP_NODE_REGEX) NodesSpecifier nodeRegex,
      @JsonProperty(PROP_PROPERTY_SPEC) OspfPropertySpecifier propertySpec) {
    _nodeRegex = firstNonNull(nodeRegex, NodesSpecifier.ALL);
    _propertySpec = firstNonNull(propertySpec, OspfPropertySpecifier.ALL);
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "ospfproperties";
  }

  @JsonProperty(PROP_NODE_REGEX)
  public NodesSpecifier getNodeRegex() {
    return _nodeRegex;
  }

  @JsonProperty(PROP_PROPERTY_SPEC)
  public OspfPropertySpecifier getPropertySpec() {
    return _propertySpec;
  }
}

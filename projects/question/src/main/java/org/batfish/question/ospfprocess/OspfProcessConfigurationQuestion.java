package org.batfish.question.ospfprocess;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.questions.Question;

/** A question that returns a table with the all OSPF processes configurations */
public class OspfProcessConfigurationQuestion extends Question {
  private static final String PROP_NODES = "nodes";
  private static final String PROP_PROPERTIES = "properties";

  @Nonnull private NodesSpecifier _nodes;
  @Nonnull private OspfPropertySpecifier _properties;

  public OspfProcessConfigurationQuestion(
      @JsonProperty(PROP_NODES) NodesSpecifier nodeRegex,
      @JsonProperty(PROP_PROPERTIES) OspfPropertySpecifier propertySpec) {
    _nodes = firstNonNull(nodeRegex, NodesSpecifier.ALL);
    _properties = firstNonNull(propertySpec, OspfPropertySpecifier.ALL);
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "ospfProcessConfiguration";
  }

  @JsonProperty(PROP_NODES)
  public NodesSpecifier getNodes() {
    return _nodes;
  }

  @JsonProperty(PROP_PROPERTIES)
  public OspfPropertySpecifier getProperties() {
    return _properties;
  }
}

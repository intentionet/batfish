package org.batfish.question.ospfarea;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;

/** A question that returns a table with the all OSPF process areas configurations */
public class OspfAreaConfigurationQuestion extends Question {
  private static final String PROP_NODES = "nodes";

  @Nonnull private NodesSpecifier _nodes;

  public OspfAreaConfigurationQuestion(@JsonProperty(PROP_NODES) NodesSpecifier nodeRegex) {
    _nodes = firstNonNull(nodeRegex, NodesSpecifier.ALL);
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "ospfAreaConfiguration";
  }

  @JsonProperty(PROP_NODES)
  public NodesSpecifier getNodes() {
    return _nodes;
  }
}

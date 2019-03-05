package org.batfish.question.ospfproperties;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/**
 * A question that returns properties of OSPF routing processes. {@link #_nodes} and {@link
 * #_properties} determine which nodes and properties are included. The default is to include
 * everything.
 */
@ParametersAreNonnullByDefault
public class OspfPropertiesQuestion extends Question {

  private static final String PROP_NODES = "nodes";
  private static final String PROP_PROPERTIES = "properties";

  @Nullable private final String _nodes;
  @Nonnull private final NodeSpecifier _nodeSpecifier;
  @Nonnull private final OspfPropertySpecifier _properties;

  @JsonCreator
  private static OspfPropertiesQuestion create(
      @Nullable @JsonProperty(PROP_NODES) String nodes,
      @Nullable @JsonProperty(PROP_PROPERTIES) OspfPropertySpecifier propertySpec) {
    return new OspfPropertiesQuestion(
        nodes,
        SpecifierFactories.getNodeSpecifierOrDefault(nodes, AllNodesNodeSpecifier.INSTANCE),
        firstNonNull(propertySpec, OspfPropertySpecifier.ALL));
  }

  public OspfPropertiesQuestion(NodeSpecifier nodeSpecifier, OspfPropertySpecifier propertySpec) {
    this(null, nodeSpecifier, propertySpec);
  }

  private OspfPropertiesQuestion(
      @Nullable String nodes, NodeSpecifier nodeSpecifier, OspfPropertySpecifier propertySpec) {
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
    return "ospfProperties";
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
  public OspfPropertySpecifier getProperties() {
    return _properties;
  }
}

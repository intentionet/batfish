package org.batfish.question.ospfinterface;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNullableByDefault;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/** A question that returns a table with all OSPF interfaces configurations */
@ParametersAreNullableByDefault
public final class OspfInterfaceConfigurationQuestion extends Question {
  private static final String PROP_NODES = "nodes";
  private static final String PROP_PROPERTIES = "properties";

  @Nullable private String _nodes;
  @Nonnull private InterfacePropertySpecifier _properties;

  public OspfInterfaceConfigurationQuestion(
      @JsonProperty(PROP_NODES) String nodes,
      @JsonProperty(PROP_PROPERTIES) InterfacePropertySpecifier propertySpec) {
    _nodes = nodes;
    _properties = firstNonNull(propertySpec, InterfacePropertySpecifier.ALL);
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "ospfInterfaceConfiguration";
  }

  @JsonProperty(PROP_NODES)
  @Nullable
  public String getNodes() {
    return _nodes;
  }

  @JsonProperty(PROP_PROPERTIES)
  public InterfacePropertySpecifier getProperties() {
    return _properties;
  }

  @JsonIgnore
  @Nonnull
  NodeSpecifier getNodesSpecifier() {
    return SpecifierFactories.getNodeSpecifierOrDefault(_nodes, AllNodesNodeSpecifier.INSTANCE);
  }
}

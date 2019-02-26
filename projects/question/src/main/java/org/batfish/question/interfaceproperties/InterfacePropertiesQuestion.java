package org.batfish.question.interfaceproperties;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllInterfacesInterfaceSpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.InterfaceSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/**
 * A question that returns properties of interfaces in a tabular format. {@link #_nodes}, {@link
 * #_interfaces}, and {@link #_properties} determine which nodes, interfaces, and properties are
 * included. The default is to include everything.
 */
@ParametersAreNonnullByDefault
public class InterfacePropertiesQuestion extends Question {

  static final boolean DEFAULT_EXCLUDE_SHUT_INTERFACES = false;

  private static final String PROP_EXCLUDE_SHUT_INTERFACES = "excludeShutInterfaces";
  private static final String PROP_INTERFACES = "interfaces";
  private static final String PROP_NODES = "nodes";
  private static final String PROP_PROPERTIES = "properties";

  @Nonnull private InterfaceSpecifier _interfaces;
  @Nonnull private NodeSpecifier _nodes;
  private boolean _onlyActive;
  @Nonnull private InterfacePropertySpecifier _properties;

  @JsonCreator
  static InterfacePropertiesQuestion create(
      @Nullable @JsonProperty(PROP_EXCLUDE_SHUT_INTERFACES) Boolean excludeShutInterfaces,
      @Nullable @JsonProperty(PROP_INTERFACES) String interfaces,
      @Nullable @JsonProperty(PROP_NODES) String nodes,
      @Nullable @JsonProperty(PROP_PROPERTIES) InterfacePropertySpecifier propertySpec) {
    return new InterfacePropertiesQuestion(
        firstNonNull(excludeShutInterfaces, DEFAULT_EXCLUDE_SHUT_INTERFACES),
        SpecifierFactories.getInterfaceSpecifierOrDefault(
            interfaces, AllInterfacesInterfaceSpecifier.INSTANCE),
        SpecifierFactories.getNodeSpecifierOrDefault(nodes, AllNodesNodeSpecifier.INSTANCE),
        firstNonNull(propertySpec, InterfacePropertySpecifier.ALL));
  }

  public InterfacePropertiesQuestion(
      Boolean excludeShutInterfaces,
      InterfaceSpecifier interfaces,
      NodeSpecifier nodes,
      InterfacePropertySpecifier propertySpec) {
    _onlyActive = excludeShutInterfaces;
    _interfaces = interfaces;
    _nodes = nodes;
    _properties = propertySpec;
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @Override
  public String getName() {
    return "interfaceProperties";
  }

  @JsonProperty(PROP_EXCLUDE_SHUT_INTERFACES)
  public boolean getOnlyActive() {
    return _onlyActive;
  }

  @JsonProperty(PROP_INTERFACES)
  public InterfaceSpecifier getInterfaces() {
    return _interfaces;
  }

  @JsonProperty(PROP_NODES)
  public NodeSpecifier getNodes() {
    return _nodes;
  }

  @JsonProperty(PROP_PROPERTIES)
  public InterfacePropertySpecifier getProperties() {
    return _properties;
  }
}

package org.batfish.question.interfaceproperties;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

  public static final boolean DEFAULT_EXCLUDE_SHUT_INTERFACES = false;

  private static final String PROP_EXCLUDE_SHUT_INTERFACES = "excludeShutInterfaces";
  private static final String PROP_INTERFACES = "interfaces";
  private static final String PROP_NODES = "nodes";
  private static final String PROP_PROPERTIES = "properties";

  @Nullable private final String _interfaces;
  @Nonnull private final InterfaceSpecifier _interfaceSpecifier;
  @Nullable private final String _nodes;
  @Nonnull private final NodeSpecifier _nodeSpecifier;
  private boolean _onlyActive;
  @Nonnull private final InterfacePropertySpecifier _properties;

  @JsonCreator
  private static InterfacePropertiesQuestion create(
      @Nullable @JsonProperty(PROP_EXCLUDE_SHUT_INTERFACES) Boolean excludeShutInterfaces,
      @Nullable @JsonProperty(PROP_INTERFACES) String interfaces,
      @Nullable @JsonProperty(PROP_NODES) String nodes,
      @Nullable @JsonProperty(PROP_PROPERTIES) InterfacePropertySpecifier propertySpec) {
    return new InterfacePropertiesQuestion(
        nodes,
        SpecifierFactories.getNodeSpecifierOrDefault(nodes, AllNodesNodeSpecifier.INSTANCE),
        interfaces,
        SpecifierFactories.getInterfaceSpecifierOrDefault(
            interfaces, AllInterfacesInterfaceSpecifier.INSTANCE),
        firstNonNull(propertySpec, InterfacePropertySpecifier.ALL),
        firstNonNull(excludeShutInterfaces, DEFAULT_EXCLUDE_SHUT_INTERFACES));
  }

  public InterfacePropertiesQuestion(
      @Nonnull NodeSpecifier nodeSpecifier,
      @Nonnull InterfaceSpecifier interfaceSpecifier,
      InterfacePropertySpecifier propertySpec,
      boolean excludeShutInterfaces) {
    this(null, nodeSpecifier, null, interfaceSpecifier, propertySpec, excludeShutInterfaces);
  }

  private InterfacePropertiesQuestion(
      @Nullable String nodes,
      NodeSpecifier nodeSpecifier,
      @Nullable String interfaces,
      InterfaceSpecifier interfaceSpecifier,
      InterfacePropertySpecifier propertySpec,
      boolean excludeShutInterfaces) {
    _nodes = nodes;
    _nodeSpecifier = nodeSpecifier;
    _interfaces = interfaces;
    _interfaceSpecifier = interfaceSpecifier;
    _properties = propertySpec;
    _onlyActive = excludeShutInterfaces;
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

  @Nullable
  @JsonProperty(PROP_INTERFACES)
  public String getInterfaces() {
    return _interfaces;
  }

  @JsonIgnore
  public InterfaceSpecifier getInterfaceSpecifier() {
    return _interfaceSpecifier;
  }

  @Nullable
  @JsonProperty(PROP_NODES)
  public String getNodes() {
    return _nodes;
  }

  @JsonIgnore
  public NodeSpecifier getNodeSpecifier() {
    return _nodeSpecifier;
  }

  @JsonProperty(PROP_PROPERTIES)
  public InterfacePropertySpecifier getProperties() {
    return _properties;
  }
}

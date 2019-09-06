package org.batfish.specifier;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.collections.NodeInterfacePair;

/**
 * An {@link InterfaceSpecifier} that specifies interfaces by {@link InterfaceType type}. Uses a
 * regex to specify which types to include.
 */
@ParametersAreNonnullByDefault
public final class TypesInterfaceSpecifier implements InterfaceSpecifier {
  private final Set<InterfaceType> _interfaceTypes;

  public TypesInterfaceSpecifier(Pattern typeNameRegex) {
    this(
        Stream.of(InterfaceType.values())
            .filter(type -> typeNameRegex.matcher(type.name()).matches())
            .collect(ImmutableSet.toImmutableSet()));
  }

  public TypesInterfaceSpecifier(Set<InterfaceType> types) {
    checkArgument(!types.isEmpty(), "Set of interface types is empty");
    _interfaceTypes = types;
  }

  @Override
  public Set<NodeInterfacePair> resolve(Set<String> nodes, SpecifierContext ctxt) {
    Map<String, Configuration> configs = ctxt.getConfigs();

    return nodes.stream()
        .map(configs::get)
        .flatMap(config -> config.getAllInterfaces().values().stream())
        .filter(iface -> _interfaceTypes.contains(iface.getInterfaceType()))
        .map(NodeInterfacePair::of)
        .collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TypesInterfaceSpecifier)) {
      return false;
    }
    TypesInterfaceSpecifier that = (TypesInterfaceSpecifier) o;
    return Objects.equals(_interfaceTypes, that._interfaceTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(_interfaceTypes);
  }
}

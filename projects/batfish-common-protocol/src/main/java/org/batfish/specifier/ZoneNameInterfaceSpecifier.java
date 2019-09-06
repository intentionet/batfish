package org.batfish.specifier;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.collections.NodeInterfacePair;

/**
 * An {@link InterfaceSpecifier} for interfaces that belong to a Zone name. Name matching is case
 * insensitive.
 */
public final class ZoneNameInterfaceSpecifier implements InterfaceSpecifier {
  String _name;

  public ZoneNameInterfaceSpecifier(String name) {
    _name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ZoneNameInterfaceSpecifier)) {
      return false;
    }
    ZoneNameInterfaceSpecifier that = (ZoneNameInterfaceSpecifier) o;
    return Objects.equals(_name, that._name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name);
  }

  @Override
  public Set<NodeInterfacePair> resolve(Set<String> nodes, SpecifierContext ctxt) {
    return nodes.stream()
        .map(n -> resolve(n, ctxt))
        .flatMap(Collection::stream)
        .collect(ImmutableSet.toImmutableSet());
  }

  // This helper could be avoided if Zones stored Interfaces and not (just) interface names
  private Set<NodeInterfacePair> resolve(String node, SpecifierContext ctxt) {
    Configuration config = ctxt.getConfigs().get(node);
    Set<String> interfaceNamesInMatchingZones =
        config.getZones().values().stream()
            .filter(z -> z.getName().equalsIgnoreCase(_name))
            .map(Zone::getInterfaces)
            .flatMap(Collection::stream)
            .collect(ImmutableSet.toImmutableSet());
    return config.getAllInterfaces().values().stream()
        .filter(i -> interfaceNamesInMatchingZones.contains(i.getName()))
        .map(NodeInterfacePair::of)
        .collect(ImmutableSet.toImmutableSet());
  }
}

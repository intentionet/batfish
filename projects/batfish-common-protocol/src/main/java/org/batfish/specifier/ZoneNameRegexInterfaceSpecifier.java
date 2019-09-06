package org.batfish.specifier;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Zone;
import org.batfish.datamodel.collections.NodeInterfacePair;

/**
 * An {@link InterfaceSpecifier} specifying interfaces that belong to Zones with names matching the
 * input regex.
 */
public final class ZoneNameRegexInterfaceSpecifier implements InterfaceSpecifier {
  Pattern _pattern;

  public ZoneNameRegexInterfaceSpecifier(Pattern pattern) {
    _pattern = pattern;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ZoneNameRegexInterfaceSpecifier)) {
      return false;
    }
    ZoneNameRegexInterfaceSpecifier that = (ZoneNameRegexInterfaceSpecifier) o;
    return Objects.equals(_pattern.pattern(), that._pattern.pattern());
  }

  @Override
  public int hashCode() {
    return Objects.hash(_pattern.pattern());
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
            .filter(z -> _pattern.matcher(z.getName()).matches())
            .map(Zone::getInterfaces)
            .flatMap(Collection::stream)
            .collect(ImmutableSet.toImmutableSet());
    return config.getAllInterfaces().values().stream()
        .filter(i -> interfaceNamesInMatchingZones.contains(i.getName()))
        .map(NodeInterfacePair::of)
        .collect(ImmutableSet.toImmutableSet());
  }
}

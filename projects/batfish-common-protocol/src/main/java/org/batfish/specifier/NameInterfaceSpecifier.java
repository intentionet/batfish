package org.batfish.specifier;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.collections.NodeInterfacePair;

/** A {@link InterfaceSpecifier} that matches interface names (case insensitive). */
@ParametersAreNonnullByDefault
public final class NameInterfaceSpecifier implements InterfaceSpecifier {
  @Nonnull private final String _name;

  public NameInterfaceSpecifier(String name) {
    _name = name;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NameInterfaceSpecifier)) {
      return false;
    }
    NameInterfaceSpecifier that = (NameInterfaceSpecifier) o;
    return Objects.equals(_name, that._name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name);
  }

  @Override
  public Set<NodeInterfacePair> resolve(Set<String> nodes, SpecifierContext ctxt) {
    return ctxt.getConfigs().values().stream()
        .filter(c -> nodes.contains(c.getHostname()))
        .map(c -> c.getAllInterfaces().values())
        .flatMap(Collection::stream)
        .filter(iface -> iface.getName().equalsIgnoreCase(_name))
        .map(NodeInterfacePair::of)
        .collect(ImmutableSet.toImmutableSet());
  }
}

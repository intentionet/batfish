package org.batfish.specifier;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.collections.NodeInterfacePair;

/** A {@link InterfaceSpecifier} that returns all interfaces. */
@ParametersAreNonnullByDefault
public final class AllInterfacesInterfaceSpecifier implements InterfaceSpecifier {
  public static final AllInterfacesInterfaceSpecifier INSTANCE =
      new AllInterfacesInterfaceSpecifier();

  private AllInterfacesInterfaceSpecifier() {}

  @Override
  public boolean equals(@Nullable Object o) {
    return o instanceof AllInterfacesInterfaceSpecifier;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public Set<NodeInterfacePair> resolve(Set<String> nodes, SpecifierContext ctxt) {
    return ctxt.getConfigs().values().stream()
        .filter(c -> nodes.contains(c.getHostname()))
        .flatMap(c -> c.getAllInterfaces().values().stream())
        .map(NodeInterfacePair::of)
        .collect(ImmutableSet.toImmutableSet());
  }
}

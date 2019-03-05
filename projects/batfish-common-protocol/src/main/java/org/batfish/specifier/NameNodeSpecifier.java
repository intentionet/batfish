package org.batfish.specifier;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/** A {@link NodeSpecifier} that matches on node names (case insensitive). */
@ParametersAreNonnullByDefault
public final class NameNodeSpecifier implements NodeSpecifier {
  private @Nonnull final String _name;

  public NameNodeSpecifier(String name) {
    _name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NameNodeSpecifier)) {
      return false;
    }
    NameNodeSpecifier that = (NameNodeSpecifier) o;
    return Objects.equals(_name, that._name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name);
  }

  @Override
  public Set<String> resolve(SpecifierContext ctxt) {
    return ctxt.getConfigs().keySet().stream()
        .filter(n -> n.equalsIgnoreCase(_name))
        .collect(ImmutableSet.toImmutableSet());
  }
}

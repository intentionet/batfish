package org.batfish.specifier;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.collections.NodeInterfacePair;

/** A {@link InterfaceSpecifier} that matches regex pattern over interface names. */
@ParametersAreNonnullByDefault
public final class NameRegexInterfaceSpecifier implements InterfaceSpecifier {
  @Nonnull private final Pattern _pattern;

  public NameRegexInterfaceSpecifier(Pattern pattern) {
    _pattern = pattern;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NameRegexInterfaceSpecifier)) {
      return false;
    }
    NameRegexInterfaceSpecifier that = (NameRegexInterfaceSpecifier) o;
    return Objects.equals(_pattern.pattern(), that._pattern.pattern());
  }

  @Override
  public int hashCode() {
    return Objects.hash(_pattern.pattern());
  }

  @Override
  public Set<NodeInterfacePair> resolve(Set<String> nodes, SpecifierContext ctxt) {
    return ctxt.getConfigs().values().stream()
        .filter(c -> nodes.contains(c.getHostname()))
        .map(c -> c.getAllInterfaces().values())
        .flatMap(Collection::stream)
        .filter(iface -> _pattern.matcher(iface.getName()).find())
        .map(NodeInterfacePair::of)
        .collect(ImmutableSet.toImmutableSet());
  }
}

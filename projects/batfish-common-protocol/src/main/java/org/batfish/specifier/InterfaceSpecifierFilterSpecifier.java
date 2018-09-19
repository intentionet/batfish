package org.batfish.specifier;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.IpAccessList;

/** A {@link FilterSpecifier} based on an {@link InterfaceSpecifier}. */
@ParametersAreNonnullByDefault
public final class InterfaceSpecifierFilterSpecifier implements FilterSpecifier {

  public enum Type {
    IN_FILTER,
    OUT_FILTER
  }

  @Nonnull private final InterfaceSpecifier _interfaceSpecifier;
  @Nonnull private final Type _type;

  public InterfaceSpecifierFilterSpecifier(Type type, InterfaceSpecifier interfaceSpecifier) {
    _type = type;
    _interfaceSpecifier = interfaceSpecifier;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InterfaceSpecifierFilterSpecifier)) {
      return false;
    }
    InterfaceSpecifierFilterSpecifier that = (InterfaceSpecifierFilterSpecifier) o;
    return Objects.equals(_interfaceSpecifier, that._interfaceSpecifier)
        && Objects.equals(_type, that._type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_interfaceSpecifier, _type.ordinal());
  }

  @Override
  public Set<IpAccessList> resolve(String node, SpecifierContext ctxt) {
    return _interfaceSpecifier
        .resolve(ImmutableSet.of(node), ctxt)
        .stream()
        .map(
            iface ->
                _type == Type.IN_FILTER ? iface.getIncomingFilter() : iface.getOutgoingFilter())
        .collect(Collectors.toSet());
  }
}

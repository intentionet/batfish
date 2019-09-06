package org.batfish.specifier;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.referencelibrary.InterfaceGroup;

/**
 * An {@link InterfaceSpecifier} that looks up an {@link InterfaceGroup} in a {@link
 * org.batfish.referencelibrary.ReferenceBook}.
 */
public final class ReferenceInterfaceGroupInterfaceSpecifier implements InterfaceSpecifier {
  private final String _interfaceGroupName;
  private final String _bookName;

  public ReferenceInterfaceGroupInterfaceSpecifier(String interfaceGroupName, String bookName) {
    _interfaceGroupName = interfaceGroupName.trim();
    _bookName = bookName.trim();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReferenceInterfaceGroupInterfaceSpecifier)) {
      return false;
    }
    ReferenceInterfaceGroupInterfaceSpecifier other = (ReferenceInterfaceGroupInterfaceSpecifier) o;
    return Objects.equals(_interfaceGroupName, other._interfaceGroupName)
        && Objects.equals(_bookName, other._bookName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_interfaceGroupName, _bookName);
  }

  @Override
  public Set<NodeInterfacePair> resolve(Set<String> nodes, SpecifierContext ctxt) {
    InterfaceGroup interfaceGroup =
        ctxt.getReferenceBook(_bookName)
            .orElseThrow(
                () -> new NoSuchElementException("ReferenceBook '" + _bookName + "' not found"))
            .getInterfaceGroup(_interfaceGroupName)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "InterfaceGroup '"
                            + _interfaceGroupName
                            + "' not found in ReferenceBook '"
                            + _bookName
                            + "'"));
    return nodes.stream()
        .map(n -> ctxt.getConfigs().get(n).getAllInterfaces().values())
        .flatMap(Collection::stream)
        .map(NodeInterfacePair::of)
        .filter(interfaceGroup.getInterfaces()::contains)
        .collect(ImmutableSet.toImmutableSet());
  }
}

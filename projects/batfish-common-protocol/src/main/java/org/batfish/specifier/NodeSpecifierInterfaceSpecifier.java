package org.batfish.specifier;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.collections.NodeInterfacePair;

/**
 * An {@link InterfaceSpecifier} that yields {@link Interface}s based on a {@code NodeSpecifier}.
 * The {@link NodeSpecifier} helps select the nodes and interfaces include all interfaces on those
 * nodes.
 */
@ParametersAreNonnullByDefault
public class NodeSpecifierInterfaceSpecifier implements InterfaceSpecifier {
  private NodeSpecifier _nodeSpecifier;

  public NodeSpecifierInterfaceSpecifier(NodeSpecifier nodeSpecifier) {
    _nodeSpecifier = nodeSpecifier;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NodeSpecifierInterfaceSpecifier)) {
      return false;
    }
    return Objects.equals(_nodeSpecifier, ((NodeSpecifierInterfaceSpecifier) o)._nodeSpecifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_nodeSpecifier);
  }

  @Override
  public Set<NodeInterfacePair> resolve(Set<String> nodes, SpecifierContext ctxt) {
    return Sets.intersection(_nodeSpecifier.resolve(ctxt), nodes).stream()
        .flatMap(n -> ctxt.getConfigs().get(n).getAllInterfaces().values().stream())
        .map(NodeInterfacePair::of)
        .collect(ImmutableSet.toImmutableSet());
  }
}

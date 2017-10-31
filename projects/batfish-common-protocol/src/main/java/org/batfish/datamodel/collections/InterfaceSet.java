package org.batfish.datamodel.collections;

import java.util.HashSet;
import java.util.SortedSet;

public class InterfaceSet extends HashSet<NodeInterfacePair> {

  private static final long serialVersionUID = 1L;

  public void removeNodes(SortedSet<String> nodes) {
    InterfaceSet toRemove = new InterfaceSet();
    for (NodeInterfacePair iface : this) {
      String node = iface.getHostname();
      if (nodes.contains(node)) {
        toRemove.add(iface);
      }
    }
    removeAll(toRemove);
  }
}

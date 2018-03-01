package org.batfish.datamodel;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.datamodel.collections.FibRow;
import org.batfish.datamodel.collections.NodeInterfacePair;

public interface DataPlane extends Serializable {

  /** Mapping: hostname -> vrfName -> fibRows */
  Map<String, Map<String, SortedSet<FibRow>>> getFibRows();

  Set<NodeInterfacePair> getFlowSinks();

  SortedMap<String, Map<Ip, SortedSet<Edge>>> getPolicyRouteFibNodeMap();

  SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> getRibs();

  Map<String, Map<String, Fib>> getFibs();

  SortedSet<Edge> getTopologyEdges();
}

package org.batfish.datamodel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.datamodel.collections.EdgeSet;
import org.batfish.datamodel.collections.FibRow;
import org.batfish.datamodel.collections.InterfaceSet;
import org.batfish.datamodel.collections.PolicyRouteFibNodeMap;

public interface DataPlane extends Serializable {

  HashMap<String, Map<String, SortedSet<FibRow>>> getFibs();

  InterfaceSet getFlowSinks();

  PolicyRouteFibNodeMap getPolicyRouteFibNodeMap();

  SortedMap<String, SortedMap<String, IRib<AbstractRoute>>> getRibs();

  EdgeSet getTopologyEdges();
}

package org.batfish.symbolic.bdd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.batfish.common.Pair;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.Protocol;
import org.batfish.symbolic.abstraction.InterfacePolicy;

public class BDDNetwork {

  private Graph _graph;

  private NodesSpecifier _nodeSpecifier;

  private PolicyQuotient _policyQuotient;

  private Map<GraphEdge, BDDRoute> _exportBgpPolicies;

  private Map<GraphEdge, BDDRoute> _importBgpPolicies;

  private Map<GraphEdge, InterfacePolicy> _exportPolicyMap;

  private Map<GraphEdge, InterfacePolicy> _importPolicyMap;

  private Map<GraphEdge, BDDAcl> _inAcls;

  private Map<GraphEdge, BDDAcl> _outAcls;

  public static BDDNetwork create(Graph g) {
    return create(g, NodesSpecifier.ALL);
  }

  public static BDDNetwork create(Graph g, NodesSpecifier nodesSpecifier) {
    PolicyQuotient pq = new PolicyQuotient(g);
    BDDNetwork network = new BDDNetwork(g, nodesSpecifier, pq);
    network.computeInterfacePolicies();
    return network;
  }

  private BDDNetwork(Graph graph, NodesSpecifier nodesSpecifier, PolicyQuotient pq) {
    _graph = graph;
    _nodeSpecifier = nodesSpecifier;
    _policyQuotient = pq;
    _importPolicyMap = new HashMap<>();
    _exportPolicyMap = new HashMap<>();
    _importBgpPolicies = new HashMap<>();
    _exportBgpPolicies = new HashMap<>();
    _inAcls = new HashMap<>();
    _outAcls = new HashMap<>();
  }

  /*
   * Compute a BDD representation of a routing policy.
   */
  private BDDRoute computeBDD(
      Graph g, Configuration conf, RoutingPolicy pol, boolean ignoreNetworks) {
    Set<Prefix> networks = null;
    if (ignoreNetworks) {
      networks = Graph.getOriginatedNetworks(conf);
    }
    TransferBDD t = new TransferBDD(g, conf, pol.getStatements(), _policyQuotient);
    return t.compute(networks);
  }

  /*
   * For each interface in the network, creates a canonical
   * representation of the import and export policies on this interface.
   */
  private void computeInterfacePolicies() {
    Set<String> includeNodes = _nodeSpecifier.getMatchingNodes(_graph.getBatfish());
    for (Entry<String, Configuration> entry : _graph.getConfigurations().entrySet()) {
      String router = entry.getKey();
      if (!includeNodes.contains(router)) { // skip if we don't care about this node
        continue;
      }
      Configuration conf = entry.getValue();
      List<GraphEdge> edges = _graph.getEdgeMap().get(router);
      for (GraphEdge ge : edges) {
        // Import BGP policy
        RoutingPolicy importBgp = _graph.findImportRoutingPolicy(router, Protocol.BGP, ge);
        if (importBgp != null) {
          BDDRoute rec = computeBDD(_graph, conf, importBgp, true);
          _importBgpPolicies.put(ge, rec);
        }
        // Export BGP policy
        RoutingPolicy exportBgp = _graph.findExportRoutingPolicy(router, Protocol.BGP, ge);
        if (exportBgp != null) {
          BDDRoute rec = computeBDD(_graph, conf, exportBgp, true);
          _exportBgpPolicies.put(ge, rec);
        }

        IpAccessList in = ge.getStart().getIncomingFilter();
        IpAccessList out = ge.getStart().getOutgoingFilter();

        // Incoming ACL
        if (in != null) {
          BDDAcl x = BDDAcl.create(in, conf.getIpAccessLists(), conf.getIpSpaces());
          _inAcls.put(ge, x);
        }
        // Outgoing ACL
        if (out != null) {
          BDDAcl x = BDDAcl.create(out, conf.getIpAccessLists(), conf.getIpSpaces());
          _outAcls.put(ge, x);
        }
      }
    }

    for (Entry<String, List<GraphEdge>> entry : _graph.getEdgeMap().entrySet()) {
      String router = entry.getKey();
      if (!includeNodes.contains(router)) { // skip if we don't care about this node
        continue;
      }
      List<GraphEdge> edges = entry.getValue();
      Configuration conf = _graph.getConfigurations().get(router);
      for (GraphEdge ge : edges) {
        BDDRoute bgpIn = _importBgpPolicies.get(ge);
        BDDRoute bgpOut = _exportBgpPolicies.get(ge);
        BDDAcl aclIn = _inAcls.get(ge);
        BDDAcl aclOut = _outAcls.get(ge);
        Integer ospfCost = ge.getStart().getOspfCost();
        SortedSet<Pair<Prefix, Integer>> staticPrefixes = new TreeSet<>();
        SortedSet<StaticRoute> staticRoutes = conf.getDefaultVrf().getStaticRoutes();
        for (StaticRoute sr : staticRoutes) {
          Prefix pfx = sr.getNetwork();
          Integer adminCost = sr.getAdministrativeCost();
          Pair<Prefix, Integer> tup = new Pair<>(pfx, adminCost);
          staticPrefixes.add(tup);
        }
        InterfacePolicy ipol = new InterfacePolicy(aclIn, bgpIn, null, staticPrefixes);
        InterfacePolicy epol = new InterfacePolicy(aclOut, bgpOut, ospfCost, null);
        _importPolicyMap.put(ge, ipol);
        _exportPolicyMap.put(ge, epol);
      }
    }
  }

  public Map<GraphEdge, BDDRoute> getExportBgpPolicies() {
    return _exportBgpPolicies;
  }

  public Map<GraphEdge, BDDRoute> getImportBgpPolicies() {
    return _importBgpPolicies;
  }

  public Map<GraphEdge, InterfacePolicy> getExportPolicyMap() {
    return _exportPolicyMap;
  }

  public Map<GraphEdge, InterfacePolicy> getImportPolicyMap() {
    return _importPolicyMap;
  }

  public Map<GraphEdge, BDDAcl> getInAcls() {
    return _inAcls;
  }

  public Map<GraphEdge, BDDAcl> getOutAcls() {
    return _outAcls;
  }
}

package org.batfish.symbolic.abstraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.OspfNeighbor;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.symbolic.Graph;
import org.batfish.symbolic.GraphEdge;
import org.batfish.symbolic.Protocol;
import org.batfish.symbolic.bdd.BDDNetwork;
import org.batfish.symbolic.collections.Table2;
import org.batfish.symbolic.utils.PrefixUtils;
import org.batfish.symbolic.utils.Tuple;

/**
 * Creates an abstraction(s) of the network by splitting the network into a collection of
 * equivalence classes and compressing the representation of each equivalence class. Each
 * equivalence class has the property that all of its stable solutions are bisimilar to the original
 * network. That is, there is a bug in the abstracted network iff there is a bug in the concrete
 * network.
 *
 * <p>How the compression occurs does depend on the property we want to check, since we can only
 * check properties for all concrete nodes that map to the same abstract node. For example, if we
 * want to check reachability between two concrete nodes, then these 2 nodes must remain distinct in
 * the compressed form.
 */

// - iBGP check source ACL?
// - add parent / client RRs?
// - Always assume multipath?

public class Abstraction {

  private IBatfish _batfish;

  private Graph _graph;

  private BDDNetwork _network;

  private HeaderSpace _headerspace;

  private int _possibleFailures;

  private boolean _useDefaultCase;

  private Map<Set<String>, Tuple<HeaderSpace, List<Prefix>>> _headerspaceMap;

  private Abstraction(IBatfish batfish, @Nullable HeaderSpace h, int fails, boolean defaultCase) {
    _batfish = batfish;
    _graph = new Graph(batfish);
    _network = BDDNetwork.create(_graph);
    _headerspace = h;
    _headerspaceMap = new HashMap<>();
    _possibleFailures = fails;
    _useDefaultCase = defaultCase;
  }

  public static Abstraction create(
      IBatfish batfish, @Nullable HeaderSpace h, int fails, boolean defaultCase) {
    Abstraction abs = new Abstraction(batfish, h, fails, defaultCase);
    abs.initDestinationMap();
    return abs;
  }

  public static Abstraction create(IBatfish batfish, int fails, boolean defaultCase) {
    Abstraction abs = new Abstraction(batfish, null, fails, defaultCase);
    abs.initDestinationMap();
    return abs;
  }

  /*
   * Initialize a map from sets of nodes that represent possible destinations,
   * to a set of prefixes that represent the collection of destination
   * IP addresses for which those nodes might be physical destinations
   */
  private void initDestinationMap() {
    Map<String, List<Protocol>> protoMap = buildProtocolMap();
    List<Prefix> dstIps = new ArrayList<>();
    List<Prefix> notDstIps = new ArrayList<>();
    extractPrefixesFromHeaderSpace(dstIps, notDstIps);
    PrefixTrieMap pt = new PrefixTrieMap();
    buildPrefixTrie(protoMap, dstIps, notDstIps, pt);
    Map<Set<String>, List<Prefix>> destinationMap = pt.createDestinationMap();
    buildHeaderSpaceEcs(destinationMap);
    if (_useDefaultCase) {
      addCatchAllCase(dstIps, notDstIps, destinationMap);
    }
  }

  private void addCatchAllCase(
      List<Prefix> dstIps, List<Prefix> notDstIps, Map<Set<String>, List<Prefix>> destinationMap) {
    HeaderSpace catchAll = createHeaderSpace(dstIps);
    // System.out.println("DstIps: " + dstIps);
    for (Prefix pfx : notDstIps) {
      catchAll.getNotDstIps().add(new IpWildcard(pfx));
    }
    for (List<Prefix> prefixes : destinationMap.values()) {
      for (Prefix pfx : prefixes) {
        catchAll.getNotDstIps().add(new IpWildcard(pfx));
      }
    }
    if (_headerspace != null) {
      copyAllButDestinationIp(catchAll, _headerspace);
    }
    if (!catchAll.getNotDstIps().equals(catchAll.getDstIps())) {
      // System.out.println("Catch all: " + catchAll.getDstIps());
      // System.out.println("Catch all: " + catchAll.getNotDstIps());
      Tuple<HeaderSpace, List<Prefix>> tup = new Tuple<>(catchAll, null);
      _headerspaceMap.put(new HashSet<>(), tup);
    }
  }

  private void buildHeaderSpaceEcs(Map<Set<String>, List<Prefix>> destinationMap) {
    destinationMap.forEach(
        (devices, prefixes) -> {
          HeaderSpace h = createHeaderSpace(prefixes);
          if (_headerspace != null) {
            copyAllButDestinationIp(h, _headerspace);
          }
          Tuple<HeaderSpace, List<Prefix>> tup = new Tuple<>(h, prefixes);
          _headerspaceMap.put(devices, tup);
        });
  }

  private void buildPrefixTrie(
      Map<String, List<Protocol>> protoMap,
      List<Prefix> dstIps,
      List<Prefix> notDstIps,
      PrefixTrieMap pt) {
    // Populate prefix trie
    for (Entry<String, Configuration> entry : _graph.getConfigurations().entrySet()) {
      String router = entry.getKey();
      Configuration conf = entry.getValue();
      for (Protocol proto : protoMap.get(router)) {
        Set<Prefix> destinations = new HashSet<>();
        if (!proto.isStatic()) {
          destinations = Graph.getOriginatedNetworks(conf, proto);
        }
        // Add all destinations to the prefix trie relevant to this slice
        for (Prefix p : destinations) {
          if (PrefixUtils.overlap(p, dstIps) && !PrefixUtils.overlap(p, notDstIps)) {
            Set<Prefix> toAdd = new HashSet<>();
            for (Prefix pfx : dstIps) {
              if (p.equals(pfx)) {
                toAdd.add(p);
              } else if (pfx.containsPrefix(p)) {
                toAdd.add(p);
              } else if (p.containsPrefix(pfx)) {
                toAdd.add(pfx);
              }
            }
            for (Prefix prefix : toAdd) {
              pt.add(prefix, router);
            }
          }
        }
      }
    }
  }

  private void extractPrefixesFromHeaderSpace(List<Prefix> dstIps, List<Prefix> notDstIps) {
    if (_headerspace == null || _headerspace.getDstIps().isEmpty()) {
      dstIps.add(Prefix.ZERO);
    } else {
      for (IpWildcard ip : _headerspace.getDstIps()) {
        if (!ip.isPrefix()) {
          throw new BatfishException("Unimplemented: IpWildcard that is not prefix: " + ip);
        }
        dstIps.add(ip.toPrefix());
      }
      for (IpWildcard ip : _headerspace.getNotDstIps()) {
        if (!ip.isPrefix()) {
          throw new BatfishException("Unimplemented: IpWildcard that is not prefix: " + ip);
        }
        notDstIps.add(ip.toPrefix());
      }
    }
  }

  private void copyAllButDestinationIp(HeaderSpace h1, HeaderSpace h2) {
    h1.setDscps(h2.getDscps());
    h1.setDstPorts(h2.getDstPorts());
    h1.setNotDstPorts(h2.getNotDstPorts());
    h1.setNotDstProtocols(h2.getNotDstProtocols());
    h1.setDstProtocols(h2.getDstProtocols());
    h1.setSrcPorts(h2.getSrcPorts());
    h1.setSrcIps(h2.getSrcIps());
    h1.setSrcOrDstIps(h2.getSrcOrDstIps());
    h1.setSrcOrDstPorts(h2.getSrcOrDstPorts());
    h1.setNotSrcIps(h2.getNotSrcIps());
    h1.setNotSrcPorts(h2.getNotSrcPorts());
    h1.setEcns(h2.getEcns());
    h1.setNotEcns(h2.getNotEcns());
    h1.setFragmentOffsets(h2.getFragmentOffsets());
    h1.setNotFragmentOffsets(h2.getNotFragmentOffsets());
    h1.setPacketLengths(h2.getPacketLengths());
    h1.setNotPacketLengths(h2.getNotPacketLengths());
    h1.setIcmpCodes(h2.getIcmpCodes());
    h1.setNotIcmpCodes(h2.getNotIcmpCodes());
    h1.setIcmpTypes(h2.getIcmpTypes());
    h1.setNotIcmpTypes(h2.getNotIcmpTypes());
    h1.setStates(h2.getStates());
    h1.setTcpFlags(h2.getTcpFlags());
  }

  /*
   * Initialize a mapping from router to collection of protocol
   */
  private Map<String, List<Protocol>> buildProtocolMap() {
    // Figure out which protocols are running on which devices
    Map<String, List<Protocol>> protocols = new HashMap<>();
    for (Entry<String, Configuration> entry : _graph.getConfigurations().entrySet()) {
      String router = entry.getKey();
      Configuration conf = entry.getValue();
      List<Protocol> protos = new ArrayList<>();
      protocols.put(router, protos);

      if (conf.getDefaultVrf().getOspfProcess() != null) {
        protos.add(Protocol.OSPF);
      }

      if (conf.getDefaultVrf().getBgpProcess() != null) {
        protos.add(Protocol.BGP);
      }

      if (!conf.getDefaultVrf().getStaticRoutes().isEmpty()) {
        protos.add(Protocol.STATIC);
      }

      if (!conf.getInterfaces().isEmpty()) {
        protos.add(Protocol.CONNECTED);
      }
    }
    return protocols;
  }

  public ArrayList<Supplier<EquivalenceClass>> equivalenceClasses() {
    ArrayList<Supplier<EquivalenceClass>> classes = new ArrayList<>();
    for (Entry<Set<String>, Tuple<HeaderSpace, List<Prefix>>> entry : _headerspaceMap.entrySet()) {
      Set<String> devices = entry.getKey();
      HeaderSpace headerspace = entry.getValue().getFirst();
      List<Prefix> prefixes = entry.getValue().getSecond();
      Supplier<EquivalenceClass> sup = () -> computeAbstraction(devices, headerspace, prefixes);
      classes.add(sup);
    }
    return classes;
  }

  /*
   * Convert a collection of prefixes over destination IP in to a headerspace
   */
  private HeaderSpace createHeaderSpace(List<Prefix> prefixes) {
    HeaderSpace h = new HeaderSpace();
    SortedSet<IpWildcard> ips = new TreeSet<>();
    for (Prefix pfx : prefixes) {
      IpWildcard ip = new IpWildcard(pfx);
      ips.add(ip);
    }
    h.setDstIps(ips);
    return h;
  }

  /*
   * Create an abstract network that is forwarding-equivalent to the original
   * network for a given destination-based slice of the original network.
   */
  private EquivalenceClass computeAbstraction(
      Set<String> devices, HeaderSpace headerspace, List<Prefix> prefixes) {

    Map<GraphEdge, InterfacePolicy> exportPol = new HashMap<>();
    Map<GraphEdge, InterfacePolicy> importPol = new HashMap<>();

    if (prefixes == null) {
      exportPol = _network.getExportPolicyMap();
      importPol = _network.getImportPolicyMap();
    } else {
      specializeBdds(prefixes, exportPol, importPol);
    }

    UnionSplit<String> workset = new UnionSplit<>(_graph.getRouters());

    // Each origination point will remain concrete
    for (String device : devices) {
      workset.split(device);
    }

    // Repeatedly split the abstraction to a fixed point
    Set<Set<String>> todo;
    do {
      todo = new HashSet<>();
      List<Set<String>> ps = workset.partitions();

      for (Set<String> partition : ps) {
        // Nothing to refine if already a concrete node
        if (partition.size() <= 1) {
          continue;
        }
        if (needUniversalAbstraction()) {
          abstractUniversal(exportPol, importPol, workset, todo, ps, partition);
        } else if (_possibleFailures > 0) {
          abstractExistential(exportPol, importPol, workset, todo, ps, partition, true);
        } else {
          abstractExistential(exportPol, importPol, workset, todo, ps, partition, false);
        }
        // If something changed, then start over early
        // Helps the next iteration to use the newly reflected information.
        if (todo.size() > 0) {
          break;
        }
      }

      // Now refine the abstraction further
      for (Set<String> newPartition : todo) {
        workset.split(newPartition);
      }

    } while (!todo.isEmpty());

    Tuple<Graph, AbstractionMap> abstractNetwork = createAbstractNetwork(workset, devices);
    Graph abstractGraph = abstractNetwork.getFirst();
    AbstractionMap abstraction = abstractNetwork.getSecond();

    System.out.println("EC Devices: " + devices);
    System.out.println("EC Prefixes: " + prefixes);
    // System.out.println("Groups: \n" + workset.partitions());
    // System.out.println("New graph: \n" + abstractGraph);
    System.out.println("Num Groups: " + workset.partitions().size());
    System.out.println("Num configs: " + abstractGraph.getConfigurations().size());

    return new EquivalenceClass(headerspace, abstractGraph, abstraction);
  }

  /*
   * Specialize the collection of BDDs representing ACL and route map policies on
   * each edge. Must be synchronized since BDDs are not thread-safe.
   */
  private synchronized void specializeBdds(
      List<Prefix> prefixes,
      Map<GraphEdge, InterfacePolicy> exportPol,
      Map<GraphEdge, InterfacePolicy> importPol) {
    for (Entry<GraphEdge, InterfacePolicy> entry : _network.getExportPolicyMap().entrySet()) {
      GraphEdge ge = entry.getKey();
      InterfacePolicy pol = entry.getValue();
      exportPol.put(ge, pol.restrict(prefixes));
    }
    for (Entry<GraphEdge, InterfacePolicy> entry : _network.getImportPolicyMap().entrySet()) {
      GraphEdge ge = entry.getKey();
      InterfacePolicy pol = entry.getValue();
      importPol.put(ge, pol.restrict(prefixes));
    }
  }

  /*
   * Refine the abstraction for the given partition of nodes so that
   * we have an existential abstraction. For each node there exists an
   * edge to the same abstract neighbor.
   */
  private void abstractExistential(
      Map<GraphEdge, InterfacePolicy> exportPol,
      Map<GraphEdge, InterfacePolicy> importPol,
      UnionSplit<String> workset,
      Set<Set<String>> todo,
      List<Set<String>> ps,
      Set<String> partition,
      boolean countMatters) {

    // Split by existential abstraction
    Table2<String, EquivalenceEdge, Integer> existentialMap = new Table2<>();
    Table2<String, Integer, Set<EquivalenceEdge>> byId = new Table2<>();

    for (String router : partition) {
      List<GraphEdge> edges = _graph.getEdgeMap().get(router);
      for (GraphEdge edge : edges) {
        if (!edge.isAbstract()) {
          String peer = edge.getPeer();
          InterfacePolicy ipol = importPol.get(edge);
          GraphEdge otherEnd = _graph.getOtherEnd().get(edge);
          InterfacePolicy epol = null;
          if (otherEnd != null) {
            epol = exportPol.get(otherEnd);
          }
          // Update the existential map
          Integer peerGroup = (peer == null ? -1 : workset.getHandle(peer));
          EquivalenceEdge ee = new EquivalenceEdge(peerGroup, ipol, epol);
          Integer i = existentialMap.get(router, ee);
          i = (i == null ? 1 : i + 1);
          existentialMap.put(router, ee, i);
          // Update the id map
          if (peerGroup != -1) {
            Set<EquivalenceEdge> existing = byId.get(router, peerGroup);
            existing = (existing == null ? new HashSet<>() : existing);
            existing.add(ee);
            byId.put(router, peerGroup, existing);
          }
        }
      }
    }

    // If there is more than one policy to the same abstract neighbor, we make concrete
    // Since by definition this can not be a valid abstraction
    Set<String> makeConcrete = new HashSet<>();
    byId.forEach(
        (router, peerGroup, edges) -> {
          if (edges.size() > 1) {
            makeConcrete.add(router);
          }
        });

    Collection<Set<String>> newPartitions = new HashSet<>();

    // Add concrete devices
    for (String router : makeConcrete) {
      Set<String> singleDevice = new HashSet<>();
      singleDevice.add(router);
      newPartitions.add(singleDevice);
    }

    // Collect router by policy type
    if (countMatters) {
      Map<Map<EquivalenceEdge, Integer>, Set<String>> inversePolicyMap = new HashMap<>();
      existentialMap.forEach(
          (router, map) -> {
            if (!makeConcrete.contains(router)) {
              Set<String> routers = inversePolicyMap.computeIfAbsent(map, k -> new HashSet<>());
              routers.add(router);
            }
          });
      newPartitions.addAll(inversePolicyMap.values());
    } else {
      Map<Set<EquivalenceEdge>, Set<String>> inversePolicyMap = new HashMap<>();
      existentialMap.forEach(
          (router, map) -> {
            if (!makeConcrete.contains(router)) {
              Set<EquivalenceEdge> edges = map.keySet();
              Set<String> routers = inversePolicyMap.computeIfAbsent(edges, k -> new HashSet<>());
              routers.add(router);
            }
          });
      newPartitions.addAll(inversePolicyMap.values());
    }

    // Only add changed to the list
    for (Set<String> collection : newPartitions) {
      if (!ps.contains(collection)) {
        todo.add(collection);
      }
    }
  }

  /*
   * Refine an abstraction for a given partition of nodes to have
   * a unviersal abstraction. That is each concrete node has the
   * same collection of concrete neighbors for each abstract neighbor.
   */
  private void abstractUniversal(
      Map<GraphEdge, InterfacePolicy> exportPol,
      Map<GraphEdge, InterfacePolicy> importPol,
      UnionSplit<String> workset,
      Set<Set<String>> todo,
      List<Set<String>> ps,
      Set<String> partition) {

    // Split by universal abstraction
    Map<String, Set<Tuple<String, EquivalenceEdge>>> universalMap = new HashMap<>();
    Table2<String, Integer, Set<EquivalenceEdge>> byId = new Table2<>();

    for (String router : partition) {
      List<GraphEdge> edges = _graph.getEdgeMap().get(router);
      for (GraphEdge edge : edges) {
        if (!edge.isAbstract()) {
          String peer = edge.getPeer();
          InterfacePolicy ipol = importPol.get(edge);
          GraphEdge otherEnd = _graph.getOtherEnd().get(edge);
          InterfacePolicy epol = null;
          if (otherEnd != null) {
            epol = exportPol.get(otherEnd);
          }
          Integer peerGroup = (peer == null ? -1 : workset.getHandle(peer));
          EquivalenceEdge ee = new EquivalenceEdge(peerGroup, ipol, epol);
          Tuple<String, EquivalenceEdge> tup = new Tuple<>(peer, ee);
          Set<Tuple<String, EquivalenceEdge>> group =
              universalMap.computeIfAbsent(router, k -> new HashSet<>());
          group.add(tup);
          // Update the id map
          if (peerGroup != -1) {
            Set<EquivalenceEdge> existing = byId.get(router, peerGroup);
            existing = (existing == null ? new HashSet<>() : existing);
            existing.add(ee);
            byId.put(router, peerGroup, existing);
          }
        }
      }
    }

    // If there is more than one policy to the same abstract neighbor, we make concrete
    // Since by definition this can not be a valid abstraction
    Set<String> makeConcrete = new HashSet<>();
    byId.forEach(
        (router, peerGroup, edges) -> {
          if (edges.size() > 1) {
            makeConcrete.add(router);
          }
        });

    Collection<Set<String>> newPartitions = new HashSet<>();

    // Add concrete devices
    for (String router : makeConcrete) {
      Set<String> singleDevice = new HashSet<>();
      singleDevice.add(router);
      newPartitions.add(singleDevice);
    }

    // Collect router by policy
    Map<Set<Tuple<String, EquivalenceEdge>>, Set<String>> inversePolicyMap = new HashMap<>();
    universalMap.forEach(
        (router, set) -> {
          if (!makeConcrete.contains(router)) {
            Set<String> routers = inversePolicyMap.computeIfAbsent(set, gs -> new HashSet<>());
            routers.add(router);
          }
        });

    newPartitions.addAll(inversePolicyMap.values());

    // Only add changed to the list
    for (Set<String> collection : newPartitions) {
      if (!ps.contains(collection)) {
        todo.add(collection);
      }
    }
  }

  // TODO: lookup based on local preference
  private boolean needUniversalAbstraction() {
    return false;
  }

  /*
   * Collect concrete neighbors by their abstract ids
   */
  private Table2<String, Integer, Set<String>> collectNeighborByAbstractId(UnionSplit<String> us) {
    // organize neighbors by abstract id
    Table2<String, Integer, Set<String>> neighborByAbstractId = new Table2<>();
    for (Entry<String, List<GraphEdge>> entry : _graph.getEdgeMap().entrySet()) {
      String router = entry.getKey();
      List<GraphEdge> edges = entry.getValue();
      for (GraphEdge ge : edges) {
        String peer = ge.getPeer();
        if (peer != null) {
          Integer j = us.getHandle(peer);
          Set<String> existing = neighborByAbstractId.get(router, j);
          if (existing != null) {
            existing.add(peer);
          } else {
            Set<String> neighbors = new HashSet<>();
            neighbors.add(peer);
            neighborByAbstractId.put(router, j, neighbors);
          }
        }
      }
    }
    return neighborByAbstractId;
  }

  /*
   * Given a collection of abstract roles, computes a set of canonical
   * representatives from each role that serve as the abstraction.
   */
  private Map<Integer, Set<String>> pickCanonicalRouters(UnionSplit<String> us, Set<String> dsts) {
    Table2<String, Integer, Set<String>> neighborByAbstractId = collectNeighborByAbstractId(us);
    Map<Integer, Set<String>> chosen = new HashMap<>();
    Stack<String> stack = new Stack<>();
    List<String> options = new ArrayList<>();

    if (dsts.isEmpty()) {
      // When destination only can be from external
      // Pick a representative from each externally-facing abstract group
      Set<Integer> picked = new HashSet<>();
      for (GraphEdge ge : _graph.getAllRealEdges()) {
        if (_graph.isExternal(ge)) {
          String d = ge.getRouter();
          Integer i = us.getHandle(d);
          if (!picked.contains(i)) {
            Set<String> dest = new HashSet<>();
            dest.add(d);
            stack.push(d);
            chosen.put(i, dest);
            picked.add(i);
          }
        }
      }
    } else {
      // Start with the concrete nodes
      for (String d : dsts) {
        stack.push(d);
        Set<String> dest = new HashSet<>();
        dest.add(d);
        chosen.put(us.getHandle(d), dest);
      }
    }

    // Need to choose representatives that are connected
    while (!stack.isEmpty()) {
      String router = stack.pop();
      Map<Integer, Set<String>> byId = neighborByAbstractId.get(router);
      for (Entry<Integer, Set<String>> entry : byId.entrySet()) {
        Integer j = entry.getKey();
        Set<String> peers = entry.getValue();
        Set<String> chosenPeers = chosen.computeIfAbsent(j, k -> new HashSet<>());
        // Find how many choices we need, and collect the options
        int numNeeded = _possibleFailures + 1;
        options.clear();
        for (String x : peers) {
          if (chosenPeers.contains(x)) {
            numNeeded--;
          } else {
            options.add(x);
          }
        }
        // Add new neighbors until satisfied
        for (int k = 0; k < Math.min(numNeeded, options.size()); k++) {
          String y = options.get(k);
          chosenPeers.add(y);
          stack.push(y);
        }
      }
    }

    return chosen;
  }

  /*
   * Creates a new Configuration from an old one for an abstract router
   * by copying the old configuration, but removing any concrete interfaces,
   * neighbors etc that do not correpond to any abstract neighbors.
   */
  private Configuration createAbstractConfig(Set<String> abstractRouters, Configuration conf) {
    Configuration abstractConf =
        new Configuration(conf.getHostname(), conf.getConfigurationFormat());
    abstractConf.setDomainName(conf.getDomainName());
    abstractConf.setDnsServers(conf.getDnsServers());
    abstractConf.setDnsSourceInterface(conf.getDnsSourceInterface());
    abstractConf.setAuthenticationKeyChains(conf.getAuthenticationKeyChains());
    abstractConf.setIkeGateways(conf.getIkeGateways());
    abstractConf.setDefaultCrossZoneAction(conf.getDefaultCrossZoneAction());
    abstractConf.setIkePolicies(conf.getIkePolicies());
    abstractConf.setIkeProposals(conf.getIkeProposals());
    abstractConf.setDefaultInboundAction(conf.getDefaultInboundAction());
    abstractConf.setIpAccessLists(conf.getIpAccessLists());
    abstractConf.setIp6AccessLists(conf.getIp6AccessLists());
    abstractConf.setRouteFilterLists(conf.getRouteFilterLists());
    abstractConf.setRoute6FilterLists(conf.getRoute6FilterLists());
    abstractConf.setIpsecPolicies(conf.getIpsecPolicies());
    abstractConf.setIpsecProposals(conf.getIpsecProposals());
    abstractConf.setIpsecVpns(conf.getIpsecVpns());
    abstractConf.setLoggingServers(conf.getLoggingServers());
    abstractConf.setLoggingSourceInterface(conf.getLoggingSourceInterface());
    abstractConf.setNormalVlanRange(conf.getNormalVlanRange());
    abstractConf.setNtpServers(conf.getNtpServers());
    abstractConf.setNtpSourceInterface(conf.getNtpSourceInterface());
    abstractConf.setRoles(conf.getRoles());
    abstractConf.setSnmpSourceInterface(conf.getSnmpSourceInterface());
    abstractConf.setSnmpTrapServers(conf.getSnmpTrapServers());
    abstractConf.setTacacsServers(conf.getTacacsServers());
    abstractConf.setTacacsSourceInterface(conf.getTacacsSourceInterface());
    abstractConf.setVendorFamily(conf.getVendorFamily());
    abstractConf.setZones(conf.getZones());
    abstractConf.setCommunityLists(conf.getCommunityLists());
    abstractConf.setRoutingPolicies(conf.getRoutingPolicies());
    abstractConf.setRoute6FilterLists(conf.getRoute6FilterLists());

    SortedSet<Interface> toRetain = new TreeSet<>();
    SortedSet<Pair<Ip, Ip>> ipNeighbors = new TreeSet<>();
    SortedSet<BgpNeighbor> bgpNeighbors = new TreeSet<>();

    List<GraphEdge> edges = _graph.getEdgeMap().get(conf.getName());
    for (GraphEdge ge : edges) {
      boolean leavesNetwork = (ge.getPeer() == null);
      if (leavesNetwork
          || (abstractRouters.contains(ge.getRouter()) && abstractRouters.contains(ge.getPeer()))) {
        toRetain.add(ge.getStart());
        Ip start = ge.getStart().getAddress().getAddress();
        if (!leavesNetwork) {
          Ip end = ge.getEnd().getAddress().getAddress();
          ipNeighbors.add(new Pair<>(start, end));
        }
        BgpNeighbor n = _graph.getEbgpNeighbors().get(ge);
        if (n != null) {
          bgpNeighbors.add(n);
        }
      }
    }

    // Update interfaces
    NavigableMap<String, Interface> abstractInterfaces = new TreeMap<>();
    for (Entry<String, Interface> entry : conf.getInterfaces().entrySet()) {
      String name = entry.getKey();
      Interface iface = entry.getValue();
      if (toRetain.contains(iface)) {
        abstractInterfaces.put(name, iface);
      }
    }
    abstractConf.setInterfaces(abstractInterfaces);

    // Update VRFs
    Map<String, Vrf> abstractVrfs = new HashMap<>();
    for (Entry<String, Vrf> entry : conf.getVrfs().entrySet()) {
      String name = entry.getKey();
      Vrf vrf = entry.getValue();
      Vrf abstractVrf = new Vrf(name);
      abstractVrf.setStaticRoutes(vrf.getStaticRoutes());
      abstractVrf.setIsisProcess(vrf.getIsisProcess());
      abstractVrf.setRipProcess(vrf.getRipProcess());
      abstractVrf.setSnmpServer(vrf.getSnmpServer());

      NavigableMap<String, Interface> abstractVrfInterfaces = new TreeMap<>();
      for (Entry<String, Interface> entry2 : vrf.getInterfaces().entrySet()) {
        String iname = entry2.getKey();
        Interface iface = entry2.getValue();
        if (toRetain.contains(iface)) {
          abstractVrfInterfaces.put(iname, iface);
        }
      }
      abstractVrf.setInterfaces(abstractVrfInterfaces);
      abstractVrf.setInterfaceNames(new TreeSet<>(abstractVrfInterfaces.keySet()));

      OspfProcess ospf = vrf.getOspfProcess();
      if (ospf != null) {
        OspfProcess abstractOspf = new OspfProcess();
        abstractOspf.setAreas(ospf.getAreas());
        abstractOspf.setExportPolicy(ospf.getExportPolicy());
        abstractOspf.setReferenceBandwidth(ospf.getReferenceBandwidth());
        abstractOspf.setRouterId(ospf.getRouterId());
        // Copy over neighbors
        Map<Pair<Ip, Ip>, OspfNeighbor> abstractNeighbors = new HashMap<>();
        if (ospf.getOspfNeighbors() != null) {
          for (Entry<Pair<Ip, Ip>, OspfNeighbor> entry2 : ospf.getOspfNeighbors().entrySet()) {
            Pair<Ip, Ip> pair = entry2.getKey();
            OspfNeighbor neighbor = entry2.getValue();
            if (ipNeighbors.contains(pair)) {
              abstractNeighbors.put(pair, neighbor);
            }
          }
        }
        abstractOspf.setOspfNeighbors(abstractNeighbors);
        abstractVrf.setOspfProcess(abstractOspf);
      }

      BgpProcess bgp = vrf.getBgpProcess();
      if (bgp != null) {
        BgpProcess abstractBgp = new BgpProcess();
        abstractBgp.setMultipathEbgp(bgp.getMultipathEbgp());
        abstractBgp.setMultipathIbgp(bgp.getMultipathIbgp());
        abstractBgp.setRouterId(bgp.getRouterId());
        abstractBgp.setOriginationSpace(bgp.getOriginationSpace());
        // TODO: set bgp neighbors accordingly
        // Copy over neighbors
        SortedMap<Prefix, BgpNeighbor> abstractBgpNeighbors = new TreeMap<>();
        if (bgp.getNeighbors() != null) {
          for (Entry<Prefix, BgpNeighbor> entry2 : bgp.getNeighbors().entrySet()) {
            Prefix prefix = entry2.getKey();
            BgpNeighbor neighbor = entry2.getValue();
            if (bgpNeighbors.contains(neighbor)) {
              abstractBgpNeighbors.put(prefix, neighbor);
            }
          }
        }
        abstractBgp.setNeighbors(abstractBgpNeighbors);
        abstractVrf.setBgpProcess(abstractBgp);
      }

      abstractVrfs.put(name, abstractVrf);
    }

    abstractConf.setVrfs(abstractVrfs);
    return abstractConf;
  }

  /*
   * Create a collection of abstract configurations given the roles computed
   * and the collection of concrete devices. Chooses a collection of canonical
   * representatives from each role, and then removes all their interfaces etc
   * that connect to non-canonical routers.
   */
  private Tuple<Graph, AbstractionMap> createAbstractNetwork(
      UnionSplit<String> us, Set<String> dests) {

    Map<Integer, Set<String>> canonicalChoices = pickCanonicalRouters(us, dests);
    Set<String> abstractRouters = new HashSet<>();
    for (Set<String> canonical : canonicalChoices.values()) {
      abstractRouters.addAll(canonical);
    }
    // Create the abstract configurations
    Map<String, Configuration> newConfigs = new HashMap<>();
    for (Entry<String, Configuration> entry : _graph.getConfigurations().entrySet()) {
      String router = entry.getKey();
      Configuration conf = entry.getValue();
      if (abstractRouters.contains(router)) {
        Configuration abstractConf = createAbstractConfig(abstractRouters, conf);
        newConfigs.put(router, abstractConf);
      }
    }

    Graph abstractGraph = new Graph(_batfish, newConfigs);
    AbstractionMap map = new AbstractionMap(canonicalChoices, us.getParitionMap());
    return new Tuple<>(abstractGraph, map);
  }
}

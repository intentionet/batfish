package org.batfish.symbolic;

import static java.util.stream.Collectors.toMap;
import static org.batfish.symbolic.CommunityVarCollector.collectCommunityVars;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchCommunitySet;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.PrefixSetExpr;
import org.batfish.datamodel.routing_policy.statement.AddCommunity;
import org.batfish.datamodel.routing_policy.statement.DeleteCommunity;
import org.batfish.datamodel.routing_policy.statement.RetainCommunity;
import org.batfish.datamodel.routing_policy.statement.SetCommunity;
import org.batfish.symbolic.collections.Table2;

/**
 * A graph object representing the structure of the network. The graph is built potentially by
 * inferring the link connectivity of the network based on interface ip addresses. Interfaces are
 * abstracted into graph edges, which include a direction. Each graph edge can be mapped to its
 * opposite edge, which may be null, if there is no such edge (e.g., for loopbacks).
 *
 * <p>iBGP is modeled as additional "abstract" edges that do not correspond to any concrete edge in
 * the topology.
 *
 * @author Ryan Beckett
 */
public class Graph {

  public enum BgpSendType {
    TO_EBGP,
    TO_NONCLIENT,
    TO_CLIENT,
    TO_RR
  }

  public static final String BGP_COMMON_FILTER_LIST_NAME = "BGP_COMMON_EXPORT_POLICY";
  private static final String NULL_INTERFACE_NAME = "null_interface";
  private IBatfish _batfish;
  private Set<String> _routers;
  private Map<String, Configuration> _configurations;
  private Map<String, Set<Long>> _areaIds;
  private Table2<String, String, List<StaticRoute>> _staticRoutes;
  private Map<String, List<StaticRoute>> _nullStaticRoutes;
  private Map<String, Set<String>> _neighbors;
  private Map<String, List<GraphEdge>> _edgeMap;
  private Set<GraphEdge> _allRealEdges;
  private Set<GraphEdge> _allEdges;
  private Map<GraphEdge, GraphEdge> _otherEnd;
  private Map<GraphEdge, BgpActivePeerConfig> _ebgpNeighbors;
  private Map<GraphEdge, BgpActivePeerConfig> _ibgpNeighbors;
  private Map<String, String> _routeReflectorParent;
  private Map<String, Set<String>> _routeReflectorClients;
  private Map<String, Integer> _originatorId;
  private Map<String, Integer> _domainMap;
  private Map<Integer, Set<String>> _domainMapInverse;

  /**
   * A graph with a static route with a dynamic next hop cannot be encoded to SMT, so some of the
   * Minesweeper analyses will fail. Compression is still possible though.
   */
  private boolean _hasStaticRouteWithDynamicNextHop;

  private Set<CommunityVar> _allCommunities;

  /** Keys are all REGEX vars, and values are lists of EXACT or OTHER vars. */
  private SortedMap<CommunityVar, List<CommunityVar>> _communityDependencies;

  private Map<String, String> _namedCommunities;

  /**
   * Create a graph, loading configurations from the given {@link IBatfish}.
   *
   * <p>Note that, because configurations are not supplied, this {@link Graph} will clone the active
   * configurations before use. This avoids side-effects that occur when {@link Graph} and other
   * code in this package mutates the configs in the graph.
   *
   * <p>For increased, efficiency, use {@link #Graph(IBatfish, Map)} which will skip the cloning,
   * assuming that the caller has made a defensive copy first.
   */
  public Graph(IBatfish batfish) {
    this(batfish, null, null);
  }

  /**
   * Create a graph, using the specified configurations.
   *
   * <p>Note that the given {@code configs} may be mutated during computation; callers are advised
   * to defensively copy them or use {@link #Graph(IBatfish)}, which will do the defensive copy
   * automatically, to avoid this side effect.
   */
  public Graph(IBatfish batfish, @Nullable Map<String, Configuration> configs) {
    this(batfish, configs, null);
  }

  /*
   * Create a graph, while selecting the subset of routers to use.
   */
  public Graph(
      IBatfish batfish,
      @Nullable Map<String, Configuration> configs,
      @Nullable Set<String> routers) {
    _batfish = batfish;
    _edgeMap = new HashMap<>();
    _allEdges = new HashSet<>();
    _allRealEdges = new HashSet<>();
    _otherEnd = new HashMap<>();
    _areaIds = new HashMap<>();
    _staticRoutes = new Table2<>();
    _nullStaticRoutes = new HashMap<>();
    _neighbors = new HashMap<>();
    _ebgpNeighbors = new HashMap<>();
    _ibgpNeighbors = new HashMap<>();
    _routeReflectorParent = new HashMap<>();
    _routeReflectorClients = new HashMap<>();
    _originatorId = new HashMap<>();
    _domainMap = new HashMap<>();
    _domainMapInverse = new HashMap<>();
    _configurations = configs;
    _communityDependencies = new TreeMap<>();

    if (_configurations == null) {
      // Since many functions that use the graph mutate the configurations, we must clone them
      // before that happens.
      // A simple way to do this is to create a deep clone of each entry using Java serialization.
      Map<String, Configuration> clonedConfigs =
          _batfish
              .loadConfigurations()
              .entrySet()
              .parallelStream()
              .collect(toMap(Entry::getKey, entry -> SerializationUtils.clone(entry.getValue())));

      _configurations = clonedConfigs;
    }
    _routers = _configurations.keySet();

    Topology topology = _batfish.getEnvironmentTopology();

    // Remove the routers we don't want to model
    if (routers != null) {
      Set<String> toRemove = new HashSet<>();
      for (String router : _configurations.keySet()) {
        if (!routers.contains(router)) {
          toRemove.add(router);
        }
      }
      for (String router : toRemove) {
        _configurations.remove(router);
      }
      topology = topology.prune(ImmutableSet.of(), toRemove, ImmutableSet.of());
    }

    initGraph(topology);
    initStaticRoutes();
    addNullRouteEdges();
    initEbgpNeighbors();
    initIbgpNeighbors();
    initAreaIds();
    initDomains();
    initAllCommunities();
    initCommDependencies();
    initNamedCommunities();
  }

  /*
   * Check if a static route is configured to drop packets
   */
  private static boolean isNullRouted(StaticRoute sr) {
    return sr.getNextHopInterface().equals(NULL_INTERFACE_NAME);
  }

  /*
   * Is a graph edge external facing (can receive BGP advertisements)
   */
  public boolean isExternal(GraphEdge ge) {
    return ge.getPeer() == null && _ebgpNeighbors.containsKey(ge);
  }

  /** Does the graph have a static route with a dynamic next hop? */
  public boolean hasStaticRouteWithDynamicNextHop() {
    return _hasStaticRouteWithDynamicNextHop;
  }

  /*
   * Find the common (default) routing policy for the protocol.
   */
  @Nullable
  public static RoutingPolicy findCommonRoutingPolicy(Configuration conf, Protocol proto) {
    if (proto.isOspf()) {
      String exp = conf.getDefaultVrf().getOspfProcess().getExportPolicy();
      return conf.getRoutingPolicies().get(exp);
    }
    if (proto.isBgp()) {
      for (Map.Entry<String, RoutingPolicy> entry : conf.getRoutingPolicies().entrySet()) {
        String name = entry.getKey();
        if (name.contains(BGP_COMMON_FILTER_LIST_NAME)) {
          return entry.getValue();
        }
      }
      return null;
    }
    if (proto.isStatic()) {
      return null;
    }
    if (proto.isConnected()) {
      return null;
    }
    throw new BatfishException("TODO: findCommonRoutingPolicy for " + proto.name());
  }

  public static Set<Prefix> getOriginatedNetworks(Configuration conf) {
    Set<Prefix> allNetworks = new HashSet<>();
    Vrf vrf = conf.getDefaultVrf();
    if (vrf.getOspfProcess() != null) {
      allNetworks.addAll(getOriginatedNetworks(conf, Protocol.OSPF));
    }
    if (vrf.getBgpProcess() != null) {
      allNetworks.addAll(getOriginatedNetworks(conf, Protocol.BGP));
    }
    if (vrf.getStaticRoutes() != null) {
      allNetworks.addAll(getOriginatedNetworks(conf, Protocol.STATIC));
    }
    allNetworks.addAll(getOriginatedNetworks(conf, Protocol.CONNECTED));
    return allNetworks;
  }

  /*
   * Collects and returns all originated prefixes for the given
   * router as well as the protocol. Static routes and connected
   * routes are treated as originating the prefix.
   */
  public static Set<Prefix> getOriginatedNetworks(Configuration conf, Protocol proto) {
    Set<Prefix> acc = new HashSet<>();

    if (proto.isOspf()) {
      OspfProcess ospf = conf.getDefaultVrf().getOspfProcess();
      for (OspfArea area : ospf.getAreas().values()) {
        for (String ifaceName : area.getInterfaces()) {
          Interface iface = conf.getAllInterfaces().get(ifaceName);
          if (iface.getActive() && iface.getOspfEnabled()) {
            acc.add(iface.getAddress().getPrefix());
          }
        }
      }
      return acc;
    }

    if (proto.isBgp()) {
      RoutingPolicy defaultPol = findCommonRoutingPolicy(conf, Protocol.BGP);
      if (defaultPol != null) {
        AstVisitor v = new AstVisitor();
        v.visit(
            conf,
            defaultPol.getStatements(),
            stmt -> {},
            expr -> {
              if (expr instanceof Conjunction) {
                Conjunction c = (Conjunction) expr;
                if (c.getConjuncts().size() >= 2) {
                  BooleanExpr be1 = c.getConjuncts().get(0);
                  BooleanExpr be2 = c.getConjuncts().get(1);
                  if (be1 instanceof MatchPrefixSet && be2 instanceof Not) {
                    MatchPrefixSet mps = (MatchPrefixSet) be1;
                    Not n = (Not) be2;
                    if (n.getExpr() instanceof MatchProtocol) {
                      MatchProtocol mp = (MatchProtocol) n.getExpr();
                      if (mp.getProtocol() == RoutingProtocol.BGP) {
                        PrefixSetExpr e = mps.getPrefixSet();
                        if (e instanceof ExplicitPrefixSet) {
                          ExplicitPrefixSet eps = (ExplicitPrefixSet) e;
                          Set<PrefixRange> ranges = eps.getPrefixSpace().getPrefixRanges();
                          for (PrefixRange r : ranges) {
                            acc.add(r.getPrefix());
                          }
                        }
                      }
                    }
                  }
                }
              }
            });
      }
      return acc;
    }

    if (proto.isConnected()) {
      for (Interface iface : conf.getAllInterfaces().values()) {
        InterfaceAddress address = iface.getAddress();
        if (address != null) {
          acc.add(address.getPrefix());
        }
      }
      return acc;
    }

    if (proto.isStatic()) {
      for (StaticRoute sr : conf.getDefaultVrf().getStaticRoutes()) {
        acc.add(sr.getNetwork());
      }
      return acc;
    }

    throw new BatfishException("ERROR: getOriginatedNetworks: " + proto.name());
  }

  /*
   * Initialize the topology by inferring interface pairs and
   * create the opposite edge mapping.
   */
  private void initGraph(Topology topology) {
    Map<NodeInterfacePair, Interface> ifaceMap = new HashMap<>();
    Map<String, Set<NodeInterfacePair>> routerIfaceMap = new HashMap<>();

    for (Entry<String, Configuration> entry : _configurations.entrySet()) {
      String router = entry.getKey();
      Configuration conf = entry.getValue();
      Set<NodeInterfacePair> ifacePairs = new HashSet<>();
      for (Entry<String, Interface> entry2 : conf.getAllInterfaces().entrySet()) {
        String name = entry2.getKey();
        Interface iface = entry2.getValue();
        NodeInterfacePair nip = new NodeInterfacePair(router, name);
        ifacePairs.add(nip);
        ifaceMap.put(nip, iface);
      }
      routerIfaceMap.put(router, ifacePairs);
    }

    _neighbors = new HashMap<>();

    for (Entry<String, Set<NodeInterfacePair>> entry : routerIfaceMap.entrySet()) {
      String router = entry.getKey();
      Set<NodeInterfacePair> nips = entry.getValue();
      Set<GraphEdge> graphEdges = new HashSet<>();
      Set<String> neighs = new HashSet<>();

      for (NodeInterfacePair nip : nips) {
        SortedSet<NodeInterfacePair> neighborIfaces = topology.getNeighbors(nip);
        Interface i1 = ifaceMap.get(nip);
        boolean hasNoOtherEnd = (neighborIfaces.isEmpty() && i1.getAddress() != null);
        if (hasNoOtherEnd) {
          GraphEdge ge = new GraphEdge(i1, null, router, null, false, false);
          graphEdges.add(ge);
        }
        if (!neighborIfaces.isEmpty()) {
          boolean hasMultipleEnds = (neighborIfaces.size() > 2);
          if (hasMultipleEnds) {
            GraphEdge ge = new GraphEdge(i1, null, router, null, false, false);
            graphEdges.add(ge);
          } else {
            for (NodeInterfacePair neighborIface : neighborIfaces) {
              // Weird inference behavior from Batfish here with a self-loop
              if (router.equals(neighborIface.getHostname())) {
                GraphEdge ge = new GraphEdge(i1, null, router, null, false, false);
                graphEdges.add(ge);
              }
              Interface i2 = ifaceMap.get(neighborIface);
              String neighbor = neighborIface.getHostname();
              GraphEdge ge1 = new GraphEdge(i1, i2, router, neighbor, false, false);
              GraphEdge ge2 = new GraphEdge(i2, i1, neighbor, router, false, false);
              _otherEnd.put(ge1, ge2);
              graphEdges.add(ge1);
              neighs.add(neighbor);
            }
          }
        }
      }

      _allRealEdges.addAll(graphEdges);
      _allEdges.addAll(graphEdges);
      _edgeMap.put(router, new ArrayList<>(graphEdges));
      _neighbors.put(router, neighs);
    }
  }

  /*
   * Collect all static routes after inferring which interface they indicate
   * should be used for the next-hop.
   */
  private void initStaticRoutes() {

    for (Entry<String, Configuration> entry : _configurations.entrySet()) {
      String router = entry.getKey();
      Configuration conf = entry.getValue();
      Map<String, List<StaticRoute>> map = new HashMap<>();

      _staticRoutes.put(router, map);

      for (StaticRoute sr : conf.getDefaultVrf().getStaticRoutes()) {

        boolean someIface = false;

        for (GraphEdge ge : _edgeMap.get(router)) {
          Interface here = ge.getStart();
          Interface there = ge.getEnd();

          // Check if next-hop interface is specified
          String hereName = here.getName();
          someIface = true;
          if (hereName.equals(sr.getNextHopInterface())) {
            List<StaticRoute> srs = map.computeIfAbsent(hereName, k -> new ArrayList<>());
            srs.add(sr);
            map.put(hereName, srs);
          }

          // Check if next-hop ip corresponds to direct interface
          Ip nhIp = sr.getNextHopIp();

          boolean isNextHop =
              there != null
                  && there.getAddress() != null
                  && there.getAddress().getIp().equals(nhIp);

          if (isNextHop) {
            someIface = true;
            List<StaticRoute> srs = map.computeIfAbsent(hereName, k -> new ArrayList<>());
            srs.add(sr);
            map.put(here.getName(), srs);
          }
        }

        if (Graph.isNullRouted(sr)) {
          List<StaticRoute> nulls =
              _nullStaticRoutes.computeIfAbsent(router, k -> new ArrayList<>());
          nulls.add(sr);
        }

        if (!someIface && !Graph.isNullRouted(sr)) {
          _hasStaticRouteWithDynamicNextHop = true;
        }
      }
    }
  }

  /*
   * Add graph edges to represent the null interface when used by a static route
   */
  private void addNullRouteEdges() {
    for (Entry<String, List<StaticRoute>> entry : _nullStaticRoutes.entrySet()) {
      String router = entry.getKey();
      List<StaticRoute> srs = entry.getValue();
      for (StaticRoute sr : srs) {
        String name = sr.getNextHopInterface();
        // Create null route interface
        Interface iface = new Interface(name);
        iface.setActive(true);
        iface.setAddress(
            new InterfaceAddress(sr.getNetwork().getStartIp(), sr.getNextHopIp().numSubnetBits()));
        iface.setBandwidth(0.);
        // Add static route to all static routes list
        Map<String, List<StaticRoute>> map = _staticRoutes.get(router);
        List<StaticRoute> routes = map.computeIfAbsent(name, k -> new ArrayList<>());
        routes.add(sr);
        // Create and add graph edge for null route
        GraphEdge ge = new GraphEdge(iface, null, router, null, false, true);
        _allRealEdges.add(ge);
        _allEdges.add(ge);
        List<GraphEdge> edges = _edgeMap.computeIfAbsent(router, k -> new ArrayList<>());
        edges.add(ge);
      }
    }
  }

  /*
   * Initialize external eBGP neighbors by looking for BGP neighbors
   * where their is no neighbor in the configurations, and the IPs align.
   */
  private void initEbgpNeighbors() {
    Map<String, List<Ip>> ips = new HashMap<>();
    Map<String, List<BgpActivePeerConfig>> neighbors = new HashMap<>();

    for (Entry<String, Configuration> entry : _configurations.entrySet()) {
      String router = entry.getKey();
      Configuration conf = entry.getValue();
      List<Ip> ipList = new ArrayList<>();
      List<BgpActivePeerConfig> ns = new ArrayList<>();
      ips.put(router, ipList);
      neighbors.put(router, ns);
      if (conf.getDefaultVrf().getBgpProcess() != null) {
        BgpProcess bgp = conf.getDefaultVrf().getBgpProcess();
        for (BgpActivePeerConfig neighbor : bgp.getActiveNeighbors().values()) {
          ipList.add(neighbor.getPeerAddress());
          ns.add(neighbor);
        }
      }
    }

    for (Entry<String, Configuration> entry : _configurations.entrySet()) {
      String router = entry.getKey();
      Configuration conf = entry.getValue();
      List<Ip> ipList = ips.get(router);
      List<BgpActivePeerConfig> ns = neighbors.get(router);
      if (conf.getDefaultVrf().getBgpProcess() != null) {
        List<GraphEdge> edges = _edgeMap.get(router);
        for (GraphEdge ge : edges) {
          for (int i = 0; i < ipList.size(); i++) {
            Ip ip = ipList.get(i);
            BgpActivePeerConfig n = ns.get(i);
            Interface iface = ge.getStart();
            if (ip != null && iface.getAddress().getPrefix().containsIp(ip)) {
              _ebgpNeighbors.put(ge, n);
            }
          }
        }
      }
    }
  }

  /*
   * Create a new "fake" interface to correspond to an abstract
   * iBGP control plane edge in the network.
   */
  private Interface createIbgpInterface(BgpActivePeerConfig n, String peer) {
    Interface iface = new Interface("iBGP-" + peer);
    iface.setActive(true);
    // TODO is this valid.
    iface.setAddress(new InterfaceAddress(n.getPeerAddress(), Prefix.MAX_PREFIX_LENGTH));
    iface.setBandwidth(0.);
    return iface;
  }

  // TODO: very inefficient
  /*
   * Initialize iBGP neighbors by looking for nieghbors
   * with the same AS number.
   */
  private void initIbgpNeighbors() {
    Table2<String, String, BgpActivePeerConfig> neighbors = generateIbgpNeighbors(_configurations);

    // Add abstract graph edges for iBGP sessions
    Table2<String, String, GraphEdge> reverse = new Table2<>();

    neighbors.forEach(
        (r1, r2, n1) -> {
          Interface iface1 = createIbgpInterface(n1, r2);

          BgpActivePeerConfig n2 = neighbors.get(r2, r1);

          GraphEdge ge;
          if (n2 != null) {
            Interface iface2 = createIbgpInterface(n2, r1);
            ge = new GraphEdge(iface1, iface2, r1, r2, true, false);
          } else {
            ge = new GraphEdge(iface1, null, r1, null, true, false);
          }

          _allEdges.add(ge);
          _ibgpNeighbors.put(ge, n1);

          reverse.put(r1, r2, ge);

          List<GraphEdge> edges = _edgeMap.get(r1);
          if (edges != null) {
            edges.add(ge);
          } else {
            edges = new ArrayList<>();
            edges.add(ge);
            _edgeMap.put(r1, edges);
          }
        });

    // Add other end to ibgp edges
    reverse.forEach(
        (r1, r2, ge1) -> {
          GraphEdge ge2 = reverse.get(r2, r1);
          _otherEnd.put(ge1, ge2);
        });

    // Configure Route Reflector information
    Integer[] id = new Integer[1];
    id[0] = 1;
    neighbors.forEach(
        (r1, ns) -> {
          if (!_originatorId.containsKey(r1)) {
            _originatorId.put(r1, id[0]);
            id[0]++;
          }
          Set<String> clients = new HashSet<>();
          ns.forEach(
              (r2, n) -> {
                if (n.getRouteReflectorClient()) {
                  clients.add(r2);
                  _routeReflectorParent.put(r2, r1);
                }
              });
          _routeReflectorClients.put(r1, clients);
        });
  }

  /**
   * Compute a map of iBGP neighbors given the configurations. Return a {@link Table2} containing
   * directional edges, representing iBGP sessions that are <i>likely</i> to come up.
   *
   * <p>Table mapping: source hostname -&gt; target hostname -&gt; source's BGP config
   */
  @VisibleForTesting
  @Nonnull
  static Table2<String, String, BgpActivePeerConfig> generateIbgpNeighbors(
      Map<String, Configuration> configurations) {
    // Map of hostname to a set of local IPs of *all* iBGP neighbors
    Map<String, Set<Ip>> ips = new HashMap<>();

    // Match iBGP sessions with pairs of routers and BgpPeerConfig
    for (Entry<String, Configuration> entry : configurations.entrySet()) {
      String hostname = entry.getKey();
      Configuration conf = entry.getValue();
      BgpProcess p = conf.getDefaultVrf().getBgpProcess();
      if (p == null) {
        // No bgp process, nothing to do
        continue;
      }
      for (BgpActivePeerConfig n : p.getActiveNeighbors().values()) {
        if (n.getLocalAs() == null || n.getRemoteAsns().isEmpty()) {
          // Invalid config
          continue;
        }
        if (n.getRemoteAsns().equals(LongSpace.of(n.getLocalAs()))) {
          ips.computeIfAbsent(hostname, key -> new HashSet<>()).add(n.getLocalIp());
        }
      }
    }

    // Init the resulting map
    Table2<String, String, BgpActivePeerConfig> neighbors = new Table2<>();

    // Loop over all iBGP configs and match up
    for (Entry<String, Configuration> entry : configurations.entrySet()) {
      String localHostname = entry.getKey();
      BgpProcess proc = entry.getValue().getDefaultVrf().getBgpProcess();
      if (proc == null) {
        // No bgp process, nothing to do
        continue;
      }

      for (Entry<Prefix, BgpActivePeerConfig> entry2 : proc.getActiveNeighbors().entrySet()) {
        Prefix remotePrefix = entry2.getKey();
        BgpActivePeerConfig localBgpConfig = entry2.getValue();
        if (localBgpConfig.getLocalAs() == null || localBgpConfig.getRemoteAsns().isEmpty()) {
          // Invalid config
          continue;
        }
        if (!localBgpConfig.getRemoteAsns().equals(LongSpace.of(localBgpConfig.getLocalAs()))) {
          // Not iBGP
          continue;
        }

        // Loop over all local IPs computed earlier to find session matches
        for (Entry<String, Set<Ip>> ipEntry : ips.entrySet()) {
          String candidateHostname = ipEntry.getKey();
          Set<Ip> candidateIps = ipEntry.getValue();
          for (Ip candidateLocalIp : candidateIps) {
            // Check that it's not a self-edge and candidate's IP matches remote prefix
            if (!localHostname.equals(candidateHostname)
                && remotePrefix.containsIp(candidateLocalIp)) {
              // We have a neighbor match
              neighbors.put(localHostname, candidateHostname, localBgpConfig);
            }
          }
        }
      }
    }
    return neighbors;
  }

  public BgpSendType peerType(GraphEdge ge) {
    if (_ebgpNeighbors.get(ge) != null) {
      return BgpSendType.TO_EBGP;
    }
    if (_ibgpNeighbors.get(ge) != null) {
      Set<String> clients = _routeReflectorClients.get(ge.getPeer());
      Set<String> clients2 = _routeReflectorClients.get(ge.getRouter());
      if (clients != null && clients.contains(ge.getRouter())) {
        return BgpSendType.TO_RR;
      } else if (clients2 != null && clients2.contains(ge.getPeer())) {
        return BgpSendType.TO_CLIENT;
      } else {
        return BgpSendType.TO_NONCLIENT;
      }
    }
    throw new BatfishException("Invalid BGP edge: " + ge);
  }

  /*
   * Initialize each routers set of area IDs for OSPF
   */
  private void initAreaIds() {
    for (Entry<String, Configuration> entry : _configurations.entrySet()) {
      String router = entry.getKey();
      Configuration conf = entry.getValue();
      Set<Long> areaIds = new HashSet<>();
      OspfProcess p = conf.getDefaultVrf().getOspfProcess();
      if (p != null) {
        p.getAreas().forEach((id, area) -> areaIds.add(id));
      }
      _areaIds.put(router, areaIds);
    }
  }

  /*
   * Determines the collection of routers within the same AS
   * as the router provided as a parameter
   */
  private Set<String> findDomain(String router) {
    String currentRouter = router;
    Set<String> sameDomain = new HashSet<>();
    Queue<String> todo = new ArrayDeque<>();
    todo.add(currentRouter);
    sameDomain.add(currentRouter);
    while (!todo.isEmpty()) {
      currentRouter = todo.remove();
      for (GraphEdge ge : getEdgeMap().get(currentRouter)) {
        String peer = ge.getPeer();
        if (peer != null && !sameDomain.contains(peer) && _ebgpNeighbors.get(ge) == null) {
          todo.add(peer);
          sameDomain.add(peer);
        }
      }
    }
    return sameDomain;
  }

  /*
   * Breaks the network up into a collection of devices that
   * are in the same autonomous system. This is useful when
   * modeling iBGP since we can restrict
   */
  private void initDomains() {
    int i = 0;
    Set<String> routers = new HashSet<>(_configurations.keySet());
    while (!routers.isEmpty()) {
      String router = routers.iterator().next();
      Set<String> domain = findDomain(router);
      _domainMapInverse.put(i, domain);
      for (String r : domain) {
        _domainMap.put(r, i);
        routers.remove(r);
      }
      i++;
    }
  }

  /*
   * Get all the routers in the same AS as the
   * router provided to the function.
   */
  public Set<String> getDomain(String router) {
    int idx = _domainMap.get(router);
    return _domainMapInverse.get(idx);
  }

  private void initAllCommunities() {
    _allCommunities = findAllCommunities();
  }

  private void initCommDependencies() {
    // Map community regex matches to Java regex
    Map<CommunityVar, java.util.regex.Pattern> regexes = new HashMap<>();
    for (CommunityVar c : _allCommunities) {
      if (c.getType() == CommunityVar.Type.REGEX) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(c.getRegex());
        regexes.put(c, p);
      }
    }

    for (CommunityVar c1 : _allCommunities) {
      // map exact match to corresponding regexes
      if (c1.getType() == CommunityVar.Type.REGEX) {

        List<CommunityVar> list = new ArrayList<>();
        _communityDependencies.put(c1, list);
        java.util.regex.Pattern p = regexes.get(c1);

        for (CommunityVar c2 : _allCommunities) {
          if (c2.getType() == CommunityVar.Type.EXACT) {
            Matcher m = p.matcher(c2.getRegex());
            if (m.find()) {
              list.add(c2);
            }
          }
          if (c2.getType() == CommunityVar.Type.OTHER && c1.getRegex().equals(c2.getRegex())) {
            list.add(c2);
          }
        }
      }
    }
  }

  /*
   * Map named community sets that contain a single match
   * back to the community/regex value. This makes it
   * easier to provide intuitive counter examples.
   */
  private void initNamedCommunities() {
    _namedCommunities = new HashMap<>();
    for (Configuration conf : getConfigurations().values()) {
      for (Entry<String, CommunityList> entry : conf.getCommunityLists().entrySet()) {
        String name = entry.getKey();
        CommunityList cl = entry.getValue();
        if (cl != null && cl.getLines().size() == 1) {
          CommunitySetExpr matchCondition = cl.getLines().get(0).getMatchCondition();
          if (matchCondition instanceof RegexCommunitySet) {
            _namedCommunities.put(((RegexCommunitySet) matchCondition).getRegex(), name);
          }
        }
      }
    }
  }

  public Set<CommunityVar> getAllCommunities() {
    return _allCommunities;
  }

  public SortedMap<CommunityVar, List<CommunityVar>> getCommunityDependencies() {
    return _communityDependencies;
  }

  public Map<String, String> getNamedCommunities() {
    return _namedCommunities;
  }

  /*
   * Finds all uniquely mentioned community matches
   * in the network by walking over every configuration.
   */
  public Set<CommunityVar> findAllCommunities() {
    Set<CommunityVar> comms = new HashSet<>();
    for (String router : getRouters()) {
      comms.addAll(findAllCommunities(router));
    }
    // Add an other option that matches a regex but isn't from this network
    List<CommunityVar> others = new ArrayList<>();
    for (CommunityVar c : comms) {
      if (c.getType() == CommunityVar.Type.REGEX) {
        CommunityVar x = CommunityVar.other(c.getRegex());
        others.add(x);
      }
    }
    comms.addAll(others);
    return comms;
  }

  public Set<CommunityVar> findAllCommunities(String router) {
    Set<CommunityVar> comms = new HashSet<>();
    Configuration conf = getConfigurations().get(router);

    for (RoutingPolicy pol : conf.getRoutingPolicies().values()) {
      AstVisitor v = new AstVisitor();
      v.visit(
          conf,
          pol.getStatements(),
          stmt -> {
            if (stmt instanceof SetCommunity) {
              SetCommunity sc = (SetCommunity) stmt;
              comms.addAll(collectCommunityVars(conf, sc.getExpr()));
            }
            if (stmt instanceof AddCommunity) {
              AddCommunity ac = (AddCommunity) stmt;
              comms.addAll(collectCommunityVars(conf, ac.getExpr()));
            }
            if (stmt instanceof DeleteCommunity) {
              DeleteCommunity dc = (DeleteCommunity) stmt;
              comms.addAll(collectCommunityVars(conf, dc.getExpr()));
            }
            if (stmt instanceof RetainCommunity) {
              RetainCommunity rc = (RetainCommunity) stmt;
              comms.addAll(collectCommunityVars(conf, rc.getExpr()));
            }
          },
          expr -> {
            if (expr instanceof MatchCommunitySet) {
              MatchCommunitySet m = (MatchCommunitySet) expr;
              CommunitySetExpr ce = m.getExpr();
              comms.addAll(collectCommunityVars(conf, ce));
            }
          });
    }

    return comms;
  }

  /*
   * Map named community sets that contain a single match
   * back to the community/regex value. This makes it
   * easier to provide intuitive counter examples.
   */
  public Map<String, String> findNamedCommunities() {
    Map<String, String> comms = new HashMap<>();
    for (Configuration conf : getConfigurations().values()) {
      for (Entry<String, CommunityList> entry : conf.getCommunityLists().entrySet()) {
        String name = entry.getKey();
        CommunityList cl = entry.getValue();
        if (cl != null && cl.getLines().size() == 1) {
          CommunitySetExpr matchCondition = cl.getLines().get(0).getMatchCondition();
          if (matchCondition instanceof RegexCommunitySet) {
            comms.put(((RegexCommunitySet) matchCondition).getRegex(), name);
          }
        }
      }
    }
    return comms;
  }

  /*
   * Find the set of all protocols that might be redistributed into
   * protocol p given the current configuration and routing policy.
   * This is based on structure of the AST.
   */
  public Set<Protocol> findRedistributedProtocols(
      Configuration conf, RoutingPolicy pol, Protocol p) {
    Set<Protocol> protos = new HashSet<>();
    AstVisitor v = new AstVisitor();
    v.visit(
        conf,
        pol.getStatements(),
        stmt -> {},
        expr -> {
          if (expr instanceof MatchProtocol) {
            MatchProtocol mp = (MatchProtocol) expr;
            RoutingProtocol other = mp.getProtocol();
            Protocol otherP = Protocol.fromRoutingProtocol(other);
            if (otherP != null && otherP != p) {
              switch (other) {
                case BGP:
                  protos.add(otherP);
                  break;
                case OSPF:
                  protos.add(otherP);
                  break;
                case STATIC:
                  protos.add(otherP);
                  break;
                case CONNECTED:
                  protos.add(otherP);
                  break;
                default:
                  throw new BatfishException("Unrecognized protocol: " + other.protocolName());
              }
            }
          }
        });
    return protos;
  }

  /*
   * Find the router Id for the neighbor corresponding to a logical edge.
   */
  public long findRouterId(GraphEdge ge, Protocol proto) {
    GraphEdge eOther = _otherEnd.get(ge);

    if (proto.isOspf() || proto.isConnected() || proto.isStatic()) {
      return 0L;
    }

    if (eOther != null) {
      String peer = eOther.getRouter();
      Configuration peerConf = getConfigurations().get(peer);
      return routerId(peerConf, proto);
    }

    BgpActivePeerConfig n = findBgpNeighbor(ge);

    if (n != null && n.getPeerAddress() != null) {
      return n.getPeerAddress().asLong();
    }

    throw new BatfishException("Unable to find router id for " + ge + "," + proto.name());
  }

  /*
   * Find the router Id for a router and a protocol.
   */
  private long routerId(Configuration conf, Protocol proto) {
    if (proto.isBgp()) {
      return conf.getDefaultVrf().getBgpProcess().getRouterId().asLong();
    }
    if (proto.isOspf()) {
      return conf.getDefaultVrf().getOspfProcess().getRouterId().asLong();
    } else {
      return 0;
    }
  }

  /*
   * Check if an interface is active for a particular protocol.
   */
  public boolean isInterfaceActive(Protocol proto, Interface iface) {
    if (proto.isOspf()) {
      return iface.getActive() && iface.getOspfEnabled();
    }
    return iface.getActive();
  }

  /*
   * Check if a topology edge is used in a particular protocol.
   */
  public boolean isEdgeUsed(Configuration conf, Protocol proto, GraphEdge ge) {
    Interface iface = ge.getStart();

    // Use a null routed edge, but only for the static protocol
    if (ge.isNullEdge()) {
      return proto.isStatic();
    }

    // Don't use if interface is not active
    if (!isInterfaceActive(proto, iface)) {
      return false;
    }

    // Exclude abstract iBGP edges from all protocols except BGP
    if (iface.getName().startsWith("iBGP-")) {
      return proto.isBgp();
    }
    // Never use Loopbacks for any protocol except connected
    if (ge.getStart().isLoopback(conf.getConfigurationFormat())) {
      return proto.isConnected();
    }

    // Don't use ospf over edges to hosts / external
    if ((ge.getPeer() == null || isHost(ge.getPeer())) && proto.isOspf()) {
      return false;
    }

    // Only use specified edges from static routes
    if (proto.isStatic()) {
      List<StaticRoute> srs = getStaticRoutes().get(conf.getHostname(), iface.getName());
      return iface.getActive() && srs != null && !srs.isEmpty();
    }

    // Only use an edge in BGP if there is an explicit peering
    if (proto.isBgp()) {
      BgpPeerConfig n1 = _ebgpNeighbors.get(ge);
      BgpPeerConfig n2 = _ibgpNeighbors.get(ge);
      return n1 != null || n2 != null;
    }

    return true;
  }

  /*
   * Determine if an edge is potentially attached to a host
   */
  public boolean isHost(String router) {
    Configuration peerConf = _configurations.get(router);
    String vendor = peerConf.getConfigurationFormat().getVendorString();
    return "host".equals(vendor);
  }

  /*
   * Check if a graph edge is a loopback address
   */
  public boolean isLoopback(GraphEdge ge) {
    Configuration conf = _configurations.get(ge.getRouter());
    ConfigurationFormat format = conf.getConfigurationFormat();
    return ge.getStart().isLoopback(format);
  }

  /*
   * Find the BGP neighbor of a particular edge
   */
  public BgpActivePeerConfig findBgpNeighbor(GraphEdge e) {
    if (e.isAbstract()) {
      return _ibgpNeighbors.get(e);
    } else {
      return _ebgpNeighbors.get(e);
    }
  }

  /* TODO: move this to Logical Graph
   * Find the import routing policy for a given edge
   */
  @Nullable
  public RoutingPolicy findImportRoutingPolicy(String router, Protocol proto, GraphEdge ge) {
    Configuration conf = _configurations.get(router);
    if (proto.isConnected()) {
      return null;
    }
    if (proto.isStatic()) {
      return null;
    }
    if (proto.isOspf()) {
      return null;
    }
    if (proto.isBgp()) {
      BgpPeerConfig n = findBgpNeighbor(ge);
      if (n == null || n.getImportPolicy() == null) {
        return null;
      }
      return conf.getRoutingPolicies().get(n.getImportPolicy());
    }
    throw new BatfishException("TODO: findImportRoutingPolicy: " + proto.name());
  }

  /*
   * Find the export routing policy for a given edge
   */
  @Nullable
  public RoutingPolicy findExportRoutingPolicy(String router, Protocol proto, GraphEdge ge) {
    Configuration conf = _configurations.get(router);
    if (proto.isConnected()) {
      return null;
    }
    if (proto.isStatic()) {
      return null;
    }
    if (proto.isOspf()) {
      OspfProcess p = conf.getDefaultVrf().getOspfProcess();
      if (p == null) {
        return null;
      }
      String exp = p.getExportPolicy();
      return conf.getRoutingPolicies().get(exp);
    }
    if (proto.isBgp()) {
      BgpPeerConfig n = findBgpNeighbor(ge);
      // if no neighbor (e.g., loopback), or no export policy
      if (n == null || n.getExportPolicy() == null) {
        return null;
      }
      return conf.getRoutingPolicies().get(n.getExportPolicy());
    }
    throw new BatfishException("TODO: findExportRoutingPolicy for " + proto.name());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("=======================================================\n");
    sb.append("---------- Router to edges map ----------\n");
    _edgeMap.forEach(
        (router, graphEdges) -> {
          sb.append("Router: ").append(router).append("\n");
          graphEdges.forEach(
              edge -> {
                sb.append("  edge from: ").append(edge.getStart().getName());
                if (edge.getEnd() == null) {
                  sb.append(" to: null ");
                } else {
                  sb.append(" to: ")
                      .append(edge.getPeer())
                      .append(",")
                      .append(edge.getEnd().getName());
                }
                sb.append(edge.getStart().getAddress());
                sb.append("\n");
              });
        });

    sb.append("---------------- eBGP Neighbors ----------------\n");
    _ebgpNeighbors.forEach(
        (ge, n) -> {
          sb.append(n);
          sb.append("Edge: ").append(ge).append(" (").append(n.getPeerAddress()).append(")\n");
        });

    sb.append("---------------- iBGP Neighbors ----------------\n");
    _ibgpNeighbors.forEach(
        (ge, n) ->
            sb.append("Edge: ").append(ge).append(" (").append(n.getPeerAddress()).append(")\n"));

    sb.append("---------- Static Routes by Interface ----------\n");
    _staticRoutes.forEach(
        (router, map) ->
            map.forEach(
                (iface, srs) -> {
                  for (StaticRoute sr : srs) {
                    sb.append("Router: ")
                        .append(router)
                        .append(", Interface: ")
                        .append(iface)
                        .append(" --> ")
                        .append(sr.getNetwork())
                        .append("\n");
                  }
                }));

    sb.append("---------- Area ids ----------\n");
    _areaIds.forEach(
        (router, ids) -> {
          if (!ids.isEmpty()) {
            sb.append("Router: ").append(router).append("=").append(ids).append("\n");
          }
        });

    sb.append("=======================================================\n");
    return sb.toString();
  }

  public Map<String, String> getRouteReflectorParent() {
    return _routeReflectorParent;
  }

  /*
   * Getters and setters
   */

  public Map<String, Set<String>> getRouteReflectorClients() {
    return _routeReflectorClients;
  }

  public Map<String, Integer> getOriginatorId() {
    return _originatorId;
  }

  public Map<GraphEdge, BgpActivePeerConfig> getEbgpNeighbors() {
    return _ebgpNeighbors;
  }

  public Map<GraphEdge, BgpActivePeerConfig> getIbgpNeighbors() {
    return _ibgpNeighbors;
  }

  public Table2<String, String, List<StaticRoute>> getStaticRoutes() {
    return _staticRoutes;
  }

  public Map<String, Set<Long>> getAreaIds() {
    return _areaIds;
  }

  public Map<String, Configuration> getConfigurations() {
    return _configurations;
  }

  public Map<String, Set<String>> getNeighbors() {
    return _neighbors;
  }

  public Map<String, List<GraphEdge>> getEdgeMap() {
    return _edgeMap;
  }

  public Map<GraphEdge, GraphEdge> getOtherEnd() {
    return _otherEnd;
  }

  public IBatfish getBatfish() {
    return _batfish;
  }

  public Set<GraphEdge> getAllRealEdges() {
    return _allRealEdges;
  }

  public Set<GraphEdge> getAllEdges() {
    return _allEdges;
  }

  public Set<String> getRouters() {
    return _routers;
  }
}

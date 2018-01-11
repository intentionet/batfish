package org.batfish.bdp;

import static java.util.Comparator.naturalOrder;
import static org.batfish.datamodel.Configuration.NODE_NONE_NAME;
import static org.batfish.datamodel.Interface.FLOW_SINK_TERMINATION_NAME;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.datamodel.Prefix.MAX_PREFIX_LENGTH;
import static org.batfish.datamodel.Route.UNSET_NEXT_HOP_INTERFACE;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.IRib;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.collections.FibRow;
import org.batfish.datamodel.collections.NodeInterfacePair;

public class BdpDataPlane implements Serializable, DataPlane {

  /** */
  private static final long serialVersionUID = 1L;

  Set<NodeInterfacePair> _flowSinks;

  Map<Ip, Set<String>> _ipOwners;

  private Map<Ip, String> _ipOwnersSimple;

  Map<String, Node> _nodes;

  Topology _topology;

  @Override
  public Map<String, Map<String, SortedSet<FibRow>>> getFibs() {
    ImmutableMap.Builder<String, Map<String, SortedSet<FibRow>>> fibsByNode =
        ImmutableMap.builder();
    _nodes
        .values()
        .parallelStream()
        .forEach(
            (node) -> {
              String hostname = node._c.getHostname();
              final ImmutableMap.Builder<String, SortedSet<FibRow>> fibsByVrf =
                  ImmutableMap.builder();
              node._virtualRouters.forEach(
                  (vrName, vr) -> {
                    ImmutableSortedSet.Builder<FibRow> fibs =
                        new ImmutableSortedSet.Builder<>(naturalOrder());
                    // handle routes
                    ImmutableMap.Builder<AbstractRoute, Set<FibRow>> interfaceRouteRowsBuilder =
                        ImmutableMap.builder();
                    ImmutableList.Builder<AbstractRoute> remainingRoutes = ImmutableList.builder();
                    for (AbstractRoute route : vr._mainRib.getRoutes()) {
                      Prefix network = route.getNetwork().getNetworkPrefix();
                      switch (route.getProtocol()) {
                        case CONNECTED:
                          importConnectedRoute(
                              hostname, fibs, interfaceRouteRowsBuilder, route, network);
                          break;

                        case STATIC:
                          importStaticRoute(
                              hostname,
                              fibs,
                              interfaceRouteRowsBuilder,
                              remainingRoutes,
                              route,
                              network);
                          break;

                        case AGGREGATE:
                          importAggregateRoute(
                              fibs, interfaceRouteRowsBuilder, remainingRoutes, route, network);
                          break;

                          // $CASES-OMITTED$
                        default:
                          // handle all other routes
                          remainingRoutes.add(route);
                          break;
                      }
                    }
                    Map<AbstractRoute, Set<FibRow>> interfaceRouteRows =
                        interfaceRouteRowsBuilder.build();
                    for (AbstractRoute route : remainingRoutes.build()) {
                      Ip currentNextHopIp = route.getNextHopIp();
                      Map<String, Map<Ip, Set<AbstractRoute>>> nextHopInterfaces =
                          vr._fib.getNextHopInterfaces(currentNextHopIp);
                      nextHopInterfaces.forEach(
                          (nextHopInterface, nextHopInterfaceRoutesByFinalNextHopIp) -> {
                            if (nextHopInterfaceRoutesByFinalNextHopIp.size() > 1) {
                              throw new BatfishException("Did not expect this");
                            }
                            for (Entry<Ip, Set<AbstractRoute>> e2 :
                                nextHopInterfaceRoutesByFinalNextHopIp.entrySet()) {
                              Set<AbstractRoute> nextHopInterfaceRoutes = e2.getValue();
                              for (AbstractRoute nextHopInterfaceRoute : nextHopInterfaceRoutes) {
                                Set<FibRow> currentInterfaceRouteRows =
                                    interfaceRouteRows.get(nextHopInterfaceRoute);
                                for (FibRow interfaceRouteRow : currentInterfaceRouteRows) {
                                  FibRow row =
                                      new FibRow(
                                          route.getNetwork().getNetworkPrefix(),
                                          interfaceRouteRow.getInterface(),
                                          interfaceRouteRow.getNextHop(),
                                          interfaceRouteRow.getNextHopInterface());
                                  fibs.add(row);
                                }
                              }
                            }
                          });
                    }
                    fibsByVrf.put(vrName, fibs.build());
                  });
              synchronized (fibsByNode) {
                fibsByNode.put(hostname, fibsByVrf.build());
              }
            });

    return fibsByNode.build();
  }

  @Override
  public Set<NodeInterfacePair> getFlowSinks() {
    return _flowSinks;
  }

  public Map<Ip, Set<String>> getIpOwners() {
    return _ipOwners;
  }

  public Map<Ip, String> getIpOwnersSimple() {
    return _ipOwnersSimple;
  }

  public Map<String, Node> getNodes() {
    return _nodes;
  }

  @Override
  public SortedMap<String, Map<Ip, SortedSet<Edge>>> getPolicyRouteFibNodeMap() {
    // TODO: implement
    return Collections.emptySortedMap();
  }

  @Override
  public SortedMap<String, SortedMap<String, IRib<AbstractRoute>>> getRibs() {
    ImmutableSortedMap.Builder<String, SortedMap<String, IRib<AbstractRoute>>> ribs =
        new ImmutableSortedMap.Builder<>(naturalOrder());
    _nodes.forEach(
        (hostname, node) -> {
          ImmutableSortedMap.Builder<String, IRib<AbstractRoute>> byVrf =
              new ImmutableSortedMap.Builder<>(naturalOrder());
          node._virtualRouters.forEach(
              (vrf, virtualRouter) -> {
                IRib<AbstractRoute> rib = virtualRouter._mainRib;
                byVrf.put(vrf, rib);
              });
          ribs.put(hostname, byVrf.build());
        });
    return ribs.build();
  }

  @Override
  public SortedSet<Edge> getTopologyEdges() {
    return _topology.getEdges();
  }

  void importAggregateRoute(
      ImmutableSortedSet.Builder<FibRow> fibs,
      ImmutableMap.Builder<AbstractRoute, Set<FibRow>> interfaceRouteRows,
      ImmutableList.Builder<AbstractRoute> remainingRoutes,
      AbstractRoute route,
      Prefix network) {
    GeneratedRoute gr = (GeneratedRoute) route;
    if (gr.getDiscard()) {
      FibRow row = new FibRow(network, NULL_INTERFACE_NAME, NODE_NONE_NAME, NULL_INTERFACE_NAME);
      fibs.add(row);
      interfaceRouteRows.put(route, Collections.singleton(row));
    } else {
      remainingRoutes.add(route);
    }
  }

  void importConnectedRoute(
      String hostname,
      ImmutableSortedSet.Builder<FibRow> fibs,
      ImmutableMap.Builder<AbstractRoute, Set<FibRow>> interfaceRouteRows,
      AbstractRoute route,
      Prefix network) {
    // do nothing for /32 network
    if (network.getPrefixLength() != MAX_PREFIX_LENGTH) {
      // if flow sink, accept, else drop
      ConnectedRoute cr = (ConnectedRoute) route;
      String outInt = cr.getNextHopInterface();
      FibRow row;
      if (_flowSinks.contains(new NodeInterfacePair(hostname, outInt))) {
        row = new FibRow(network, outInt, NODE_NONE_NAME, FLOW_SINK_TERMINATION_NAME);
      } else {
        row = new FibRow(network, NULL_INTERFACE_NAME, NODE_NONE_NAME, NULL_INTERFACE_NAME);
      }
      fibs.add(row);

      ImmutableSet.Builder<FibRow> currentRows = ImmutableSet.builder();
      SortedSet<Edge> edges =
          _topology.getInterfaceEdges().get(new NodeInterfacePair(hostname, outInt));
      if (edges != null) {
        for (Edge edge : edges) {
          if (edge.getNode1().equals(hostname)) {
            // add interface route rows that are non-dropping for
            // recursive
            // matches to this route (for ips NOT in the
            // connected
            // subnet)
            String nextHopName = edge.getNode2();
            String nextHopInIntName = edge.getInt2();
            FibRow currentRow = new FibRow(network, outInt, nextHopName, nextHopInIntName);
            currentRows.add(currentRow);

            // handle connected neighbors
            Configuration nextHop = _nodes.get(nextHopName)._c;
            Interface nextHopInInt = nextHop.getInterfaces().get(nextHopInIntName);
            for (Prefix prefix : nextHopInInt.getAllAddresses()) {
              Ip address = prefix.getAddress();
              if (network.contains(address)) {
                Prefix neighborPrefix = new Prefix(address, MAX_PREFIX_LENGTH);
                FibRow neighborRow =
                    new FibRow(neighborPrefix, outInt, nextHopName, nextHopInIntName);
                fibs.add(neighborRow);
              }
            }
          }
        }
      }
      interfaceRouteRows.put(route, currentRows.build());
    }
  }

  void importStaticInterfaceRoute(
      String hostname,
      ImmutableSortedSet.Builder<FibRow> fibs,
      ImmutableMap.Builder<AbstractRoute, Set<FibRow>> interfaceRouteRows,
      AbstractRoute route,
      Prefix network,
      String srNextHopInterfaceName) {
    NodeInterfacePair srNextHopInterface = new NodeInterfacePair(hostname, srNextHopInterfaceName);
    boolean routeToNullInterface = srNextHopInterfaceName.equals(NULL_INTERFACE_NAME);
    FibRow discardRow =
        new FibRow(network, NULL_INTERFACE_NAME, NODE_NONE_NAME, NULL_INTERFACE_NAME);
    if (routeToNullInterface) {
      fibs.add(discardRow);
      interfaceRouteRows.put(route, ImmutableSet.of(discardRow));
    } else {
      SortedSet<Edge> edges = _topology.getInterfaceEdges().get(srNextHopInterface);
      if (edges == null) {
        if (_flowSinks.contains(srNextHopInterface)) {
          FibRow row =
              new FibRow(
                  network, srNextHopInterfaceName, NODE_NONE_NAME, FLOW_SINK_TERMINATION_NAME);
          fibs.add(row);
          interfaceRouteRows.put(route, ImmutableSet.of(row));
        } else {
          fibs.add(discardRow);
          interfaceRouteRows.put(route, ImmutableSet.of(discardRow));
        }
      } else {
        Set<FibRow> rows =
            edges
                .stream()
                .filter(e -> e.getNode1().equals(hostname))
                .map(e -> new FibRow(network, srNextHopInterfaceName, e.getNode2(), e.getInt2()))
                .collect(ImmutableSet.toImmutableSet());
        interfaceRouteRows.put(route, rows);
        fibs.addAll(rows);
      }
    }
  }

  void importStaticRoute(
      String hostname,
      ImmutableSortedSet.Builder<FibRow> fibs,
      ImmutableMap.Builder<AbstractRoute, Set<FibRow>> interfaceRouteRows,
      ImmutableList.Builder<AbstractRoute> remainingRoutes,
      AbstractRoute route,
      Prefix network) {
    StaticRoute sr = (StaticRoute) route;
    Ip srNextHopIp = sr.getNextHopIp();
    String srNextHopInterfaceName = sr.getNextHopInterface();
    boolean hasNextHopIp = !srNextHopIp.equals(UNSET_ROUTE_NEXT_HOP_IP);
    boolean hasNextHopInterface = !srNextHopInterfaceName.equals(UNSET_NEXT_HOP_INTERFACE);
    if (hasNextHopIp && hasNextHopInterface) {
      /*
       * both nextHopIp and nextHopInterface; neighbor must not send
       * nextHopIp back out receiving interface
       */
      // TODO: implement above condition
      importStaticInterfaceRoute(
          hostname, fibs, interfaceRouteRows, route, network, srNextHopInterfaceName);
    } else if (!hasNextHopIp && hasNextHopInterface) {
      /*
       * just nextHopInterface; neighbor must not send dstIp back out
       * receiving interface
       */
      // TODO: implement above condition
      importStaticInterfaceRoute(
          hostname, fibs, interfaceRouteRows, route, network, srNextHopInterfaceName);
    } else if (!hasNextHopIp && !hasNextHopInterface) {
      throw new BatfishException("Invalid static route; must have nextHopIp or nextHopInterface");
    } else {
      /*
       *  If we get this far, it is a next-hop-ip-only static-route. Since
       *  it is not an interface-route, we add and break.
       */
      remainingRoutes.add(route);
    }
  }

  protected void initIpOwners(
      Map<String, Configuration> configurations,
      Map<Ip, Set<String>> ipOwners,
      Map<Ip, String> ipOwnersSimple) {
    setIpOwners(ipOwners);
    setIpOwnersSimple(ipOwnersSimple);
  }

  public void setFlowSinks(Set<NodeInterfacePair> flowSinks) {
    _flowSinks = flowSinks;
  }

  public void setIpOwners(Map<Ip, Set<String>> ipOwners) {
    _ipOwners = ipOwners;
  }

  public void setIpOwnersSimple(Map<Ip, String> ipOwnersSimple) {
    _ipOwnersSimple = ipOwnersSimple;
  }

  public void setNodes(Map<String, Node> nodes) {
    _nodes = nodes;
  }

  public void setTopology(Topology topology) {
    _topology = topology;
  }
}

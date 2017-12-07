package org.batfish.bdp;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
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
import org.batfish.datamodel.Route;
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
  public HashMap<String, Map<String, SortedSet<FibRow>>> getFibs() {
    HashMap<String, Map<String, SortedSet<FibRow>>> fibs = new HashMap<>();
    Object fibsMonitor = new Object();
    _nodes
        .values()
        .parallelStream()
        .forEach(
            (node) -> {
              String hostname = node._c.getHostname();
              final Map<String, SortedSet<FibRow>> vrfToFibSet = new HashMap<>();
              synchronized (fibsMonitor) {
                fibs.put(hostname, vrfToFibSet);
              }
              node._virtualRouters.forEach(
                  (vrName, vr) -> {
                    SortedSet<FibRow> fibSet = new TreeSet<>();
                    vrfToFibSet.put(vrName, fibSet);
                    // handle routes
                    Map<AbstractRoute, Set<FibRow>> interfaceRouteRows = new LinkedHashMap<>();
                    List<AbstractRoute> remainingRoutes = new LinkedList<>();
                    for (AbstractRoute route : vr._mainRib.getRoutes()) {
                      boolean add = false;
                      Prefix network = route.getNetwork().getNetworkPrefix();
                      switch (route.getProtocol()) {
                        case CONNECTED:
                          {
                            // do nothing for /32 network
                            if (network.getPrefixLength() == Prefix.MAX_PREFIX_LENGTH) {
                              continue;
                            }
                            // if flow sink, accept, else drop
                            ConnectedRoute cr = (ConnectedRoute) route;
                            String outInt = cr.getNextHopInterface();
                            FibRow row;
                            if (_flowSinks.contains(outInt)) {
                              row =
                                  new FibRow(
                                      network,
                                      outInt,
                                      Configuration.NODE_NONE_NAME,
                                      Interface.FLOW_SINK_TERMINATION_NAME);
                            } else {
                              row =
                                  new FibRow(
                                      network,
                                      Interface.NULL_INTERFACE_NAME,
                                      Configuration.NODE_NONE_NAME,
                                      Interface.NULL_INTERFACE_NAME);
                            }
                            fibSet.add(row);

                            Set<FibRow> currentRows = new HashSet<>();
                            interfaceRouteRows.put(route, currentRows);
                            SortedSet<Edge> edges =
                                _topology
                                    .getInterfaceEdges()
                                    .get(new NodeInterfacePair(hostname, outInt));
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
                                  FibRow currentRow =
                                      new FibRow(network, outInt, nextHopName, nextHopInIntName);
                                  currentRows.add(currentRow);

                                  // handle connected neighbors
                                  Configuration nextHop = _nodes.get(nextHopName)._c;
                                  Interface nextHopInInt =
                                      nextHop.getInterfaces().get(nextHopInIntName);
                                  for (Prefix prefix : nextHopInInt.getAllPrefixes()) {
                                    Ip address = prefix.getAddress();
                                    if (network.contains(address)) {
                                      Prefix neighborPrefix =
                                          new Prefix(address, Prefix.MAX_PREFIX_LENGTH);
                                      FibRow neighborRow =
                                          new FibRow(
                                              neighborPrefix,
                                              outInt,
                                              nextHopName,
                                              nextHopInIntName);
                                      fibSet.add(neighborRow);
                                    }
                                  }
                                }
                              }
                            }
                            break;
                          }

                        case STATIC:
                          {
                            StaticRoute sr = (StaticRoute) route;
                            Ip srNextHopIp = sr.getNextHopIp();
                            String srNextHopInterface = sr.getNextHopInterface();
                            if (!srNextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)
                                && !srNextHopInterface.equals(Route.UNSET_NEXT_HOP_INTERFACE)) {
                              // both nextHopIp and nextHopInterface; neighbor must not
                              // send
                              // nextHopIp back out receiving interface
                              // TODO: implement above condition
                              if (srNextHopInterface.equals(Interface.NULL_INTERFACE_NAME)) {
                                FibRow row =
                                    new FibRow(
                                        network,
                                        Interface.NULL_INTERFACE_NAME,
                                        Configuration.NODE_NONE_NAME,
                                        Interface.NULL_INTERFACE_NAME);
                                fibSet.add(row);
                                interfaceRouteRows.put(route, Collections.singleton(row));
                              } else {
                                Set<FibRow> currentRows = new HashSet<>();
                                SortedSet<Edge> edges =
                                    _topology
                                        .getInterfaceEdges()
                                        .get(new NodeInterfacePair(hostname, srNextHopInterface));
                                if (edges == null) {
                                  if (_flowSinks.contains(srNextHopInterface)) {
                                    fibSet.add(
                                        new FibRow(
                                            network,
                                            srNextHopInterface,
                                            Configuration.NODE_NONE_NAME,
                                            Interface.FLOW_SINK_TERMINATION_NAME));
                                  } else {
                                    fibSet.add(
                                        new FibRow(
                                            network,
                                            Interface.NULL_INTERFACE_NAME,
                                            Configuration.NODE_NONE_NAME,
                                            Interface.NULL_INTERFACE_NAME));
                                  }
                                } else {
                                  interfaceRouteRows.put(route, currentRows);
                                  for (Edge edge : edges) {
                                    if (edge.getNode1().equals(hostname)) {
                                      String nextHop = edge.getNode2();
                                      String nextHopInInt = edge.getInt2();
                                      FibRow row =
                                          new FibRow(
                                              network, srNextHopInterface, nextHop, nextHopInInt);
                                      fibSet.add(row);
                                      currentRows.add(row);
                                    }
                                  }
                                }
                              }
                              break;
                            } else if (srNextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)
                                && !srNextHopInterface.equals(Route.UNSET_NEXT_HOP_INTERFACE)) {
                              // just nextHopInterface; neighbor must not send dstIp back
                              // out receiving interface
                              // TODO: implement above condition
                              if (srNextHopInterface.equals(Interface.NULL_INTERFACE_NAME)) {
                                FibRow row =
                                    new FibRow(
                                        network,
                                        Interface.NULL_INTERFACE_NAME,
                                        Configuration.NODE_NONE_NAME,
                                        Interface.NULL_INTERFACE_NAME);
                                fibSet.add(row);
                                interfaceRouteRows.put(route, Collections.singleton(row));
                              } else {
                                Set<FibRow> currentRows = new HashSet<>();
                                SortedSet<Edge> edges =
                                    _topology
                                        .getInterfaceEdges()
                                        .get(new NodeInterfacePair(hostname, srNextHopInterface));
                                if (edges == null) {
                                  if (_flowSinks.contains(srNextHopInterface)) {
                                    fibSet.add(
                                        new FibRow(
                                            network,
                                            srNextHopInterface,
                                            Configuration.NODE_NONE_NAME,
                                            Interface.FLOW_SINK_TERMINATION_NAME));
                                  } else {
                                    fibSet.add(
                                        new FibRow(
                                            network,
                                            Interface.NULL_INTERFACE_NAME,
                                            Configuration.NODE_NONE_NAME,
                                            Interface.NULL_INTERFACE_NAME));
                                  }
                                } else {
                                  interfaceRouteRows.put(route, currentRows);
                                  for (Edge edge : edges) {
                                    if (edge.getNode1().equals(hostname)) {
                                      String nextHop = edge.getNode2();
                                      String nextHopInInt = edge.getInt2();
                                      FibRow row =
                                          new FibRow(
                                              network, srNextHopInterface, nextHop, nextHopInInt);
                                      fibSet.add(row);
                                      currentRows.add(row);
                                    }
                                  }
                                }
                              }
                              break;
                            } else if (srNextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)
                                && srNextHopInterface.equals(Route.UNSET_NEXT_HOP_INTERFACE)) {
                              throw new BatfishException(
                                  "Invalid static route; must have nextHopIp or nextHopInterface");
                            }
                            // If we get this far, it is a next-hop-ip-only static-route.
                            // Since it is not an interface-route, we add and break.
                            add = true;
                            break;
                          }

                        case AGGREGATE:
                          {
                            GeneratedRoute gr = (GeneratedRoute) route;
                            if (gr.getDiscard()) {
                              FibRow row =
                                  new FibRow(
                                      network,
                                      Interface.NULL_INTERFACE_NAME,
                                      Configuration.NODE_NONE_NAME,
                                      Interface.NULL_INTERFACE_NAME);
                              fibSet.add(row);
                              interfaceRouteRows.put(route, Collections.singleton(row));
                              break;
                            } else {
                              add = true;
                              break;
                            }
                          }

                          // $CASES-OMITTED$
                        default:
                          {
                            // handle all other routes
                            add = true;
                            break;
                          }
                      }
                      if (add) {
                        remainingRoutes.add(route);
                      }
                    }

                    for (AbstractRoute route : remainingRoutes) {
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
                                  fibSet.add(row);
                                }
                              }
                            }
                          });
                    }
                  });
            });

    return fibs;
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
  public SortedMap<String, HashMap<Ip, SortedSet<Edge>>> getPolicyRouteFibNodeMap() {
    // TODO: implement
    return new TreeMap<>();
  }

  @Override
  public SortedMap<String, SortedMap<String, IRib<AbstractRoute>>> getRibs() {
    SortedMap<String, SortedMap<String, IRib<AbstractRoute>>> ribs = new TreeMap<>();
    _nodes.forEach(
        (hostname, node) -> {
          SortedMap<String, IRib<AbstractRoute>> byVrf = new TreeMap<>();
          ribs.put(hostname, byVrf);
          node._virtualRouters.forEach(
              (vrf, virtualRouter) -> {
                IRib<AbstractRoute> rib = virtualRouter._mainRib;
                byVrf.put(vrf, rib);
              });
        });
    return ribs;
  }

  @Override
  public SortedSet<Edge> getTopologyEdges() {
    return _topology.getEdges();
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

package org.batfish.datamodel;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.collections.NodeInterfacePair;

public final class ForwardingAnalysisImpl implements ForwardingAnalysis {

  private final Map<String, Map<String, IpSpace>> _arpReplies;

  private final Map<Edge, IpSpace> _arpTrueEdge;

  private final Map<Edge, IpSpace> _arpTrueEdgeDestIp;

  private final Map<Edge, IpSpace> _arpTrueEdgeNextHopIp;

  private final Map<String, Map<String, Set<Ip>>> _interfaceOwnedIps;

  private final Map<String, Map<String, IpSpace>> _ipsRoutedOutInterfaces;

  private final Map<String, Map<String, Map<String, IpSpace>>> _neighborUnreachableOrExitsNetwork;

  private final Map<String, Map<String, IpSpace>> _neighborUnreachable;

  private final Map<String, Map<String, Map<String, IpSpace>>> _neighborUnreachableArpDestIp;

  private final Map<String, Map<String, Map<String, IpSpace>>> _neighborUnreachableArpNextHopIp;

  private final Map<String, Map<String, IpSpace>> _nullRoutedIps;

  private final Map<String, Map<String, IpSpace>> _routableIps;

  private final Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
      _routesWhereDstIpCanBeArpIp;

  private final Map<Edge, Set<AbstractRoute>> _routesWithDestIpEdge;

  private final Map<String, Map<String, Map<String, Set<AbstractRoute>>>> _routesWithNextHop;

  private final Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
      _routesWithNextHopIpArpFalse;

  private final Map<Edge, Set<AbstractRoute>> _routesWithNextHopIpArpTrue;

  private final Map<String, Map<String, IpSpace>> _someoneReplies;

  // mapping: hostname -&gt; interfacename -&gt; ip space
  private Map<String, Map<String, IpSpace>> _deliveredToSubnet;

  private Map<String, Map<String, IpSpace>> _exitsNetwork;

  // mapping: hostname -&gt; set of interfacenames
  private Map<String, Set<String>> _interfacesWithMissingDevices;

  private IpSpaceToBDD _ipSpaceToBDD;

  private final Map<String, Map<String, BDD>> _hostSubnetBDDs;

  private final BDD _vrfOwnedIpBDD;

  private final BDD _snapshotOwnedIpBDD;

  public ForwardingAnalysisImpl(
      Map<String, Configuration> configurations,
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs,
      Map<String, Map<String, Fib>> fibs,
      Topology topology) {
    BDDPacket bddPacket = new BDDPacket();
    _ipSpaceToBDD = new IpSpaceToBDD(bddPacket.getFactory(), bddPacket.getDstIp());
    _vrfOwnedIpBDD = computeVrfOwnedIpBDD(configurations);
    _hostSubnetBDDs = computeHostSubnetBDDs(configurations);
    _snapshotOwnedIpBDD = computeSnapshotOwnedIps();
    _interfacesWithMissingDevices = computeInterfacesWithMissingDevices(configurations);
    _interfaceOwnedIps = CommonUtil.computeInterfaceOwnedIps(configurations, false);
    _nullRoutedIps = computeNullRoutedIps(ribs, fibs);
    _routableIps = computeRoutableIps(ribs);
    _routesWithNextHop = computeRoutesWithNextHop(fibs);
    _ipsRoutedOutInterfaces = computeIpsRoutedOutInterfaces(ribs);
    _arpReplies = computeArpReplies(configurations, ribs);
    _someoneReplies = computeSomeoneReplies(topology);
    _routesWithNextHopIpArpFalse = computeRoutesWithNextHopIpArpFalse(fibs);
    _neighborUnreachableArpNextHopIp = computeNeighborUnreachableArpNextHopIp(ribs);
    _routesWithNextHopIpArpTrue = computeRoutesWithNextHopIpArpTrue(fibs, topology);
    _arpTrueEdgeNextHopIp = computeArpTrueEdgeNextHopIp(configurations, ribs);
    _routesWhereDstIpCanBeArpIp = computeRoutesWhereDstIpCanBeArpIp(fibs);
    _neighborUnreachableArpDestIp = computeNeighborUnreachableArpDestIp(ribs);
    _neighborUnreachableOrExitsNetwork = computeNeighborUnreachableOrExitsNetwork();
    _routesWithDestIpEdge = computeRoutesWithDestIpEdge(fibs, topology);
    _arpTrueEdgeDestIp = computeArpTrueEdgeDestIp(configurations, ribs);
    _arpTrueEdge = computeArpTrueEdge();
    _deliveredToSubnet = computeDeliveredToSubnet(configurations);
    _exitsNetwork = computeExitsNetwork(configurations);
    _neighborUnreachable = computeNeighborUnreachable();
  }

  /* The constructor should only be used for tests */
  @VisibleForTesting
  ForwardingAnalysisImpl(
      Map<String, Map<String, IpSpace>> arpReplies,
      Map<Edge, IpSpace> arpTrueEdge,
      Map<Edge, IpSpace> arpTrueEdgeDestIp,
      Map<Edge, IpSpace> arpTrueEdgeNextHopIp,
      Map<String, Map<String, Set<Ip>>> interfaceOwnedIps,
      Map<String, Map<String, IpSpace>> ipsRoutedOutInterfaces,
      Map<String, Map<String, Map<String, IpSpace>>> neighborUnreachable,
      Map<String, Map<String, Map<String, IpSpace>>> neighborUnreachableArpDestIp,
      Map<String, Map<String, Map<String, IpSpace>>> neighborUnreachableArpNextHopIp,
      Map<String, Map<String, IpSpace>> nullRoutedIps,
      Map<String, Map<String, IpSpace>> routableIps,
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWhereDstIpCanBeArpIp,
      Map<Edge, Set<AbstractRoute>> routesWithDestIpEdge,
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop,
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHopIpArpFalse,
      Map<Edge, Set<AbstractRoute>> routesWithNextHopIpArpTrue,
      Map<String, Map<String, IpSpace>> someoneReplies) {
    _nullRoutedIps = nullRoutedIps;
    _routableIps = routableIps;
    _routesWithNextHop = routesWithNextHop;
    _interfaceOwnedIps = interfaceOwnedIps;
    _ipsRoutedOutInterfaces = ipsRoutedOutInterfaces;
    _arpReplies = arpReplies;
    _someoneReplies = someoneReplies;
    _routesWithNextHopIpArpFalse = routesWithNextHopIpArpFalse;
    _neighborUnreachableArpNextHopIp = neighborUnreachableArpNextHopIp;
    _routesWithNextHopIpArpTrue = routesWithNextHopIpArpTrue;
    _arpTrueEdgeNextHopIp = arpTrueEdgeNextHopIp;
    _routesWhereDstIpCanBeArpIp = routesWhereDstIpCanBeArpIp;
    _neighborUnreachableArpDestIp = neighborUnreachableArpDestIp;
    _neighborUnreachableOrExitsNetwork = neighborUnreachable;
    _routesWithDestIpEdge = routesWithDestIpEdge;
    _arpTrueEdgeDestIp = arpTrueEdgeDestIp;
    _arpTrueEdge = arpTrueEdge;
    _neighborUnreachable = null;
    _hostSubnetBDDs = null;
    _vrfOwnedIpBDD = null;
    _snapshotOwnedIpBDD = null;
  }

  /**
   * Compute an IP address ACL for each interface of each node permitting only those IPs for which
   * the node would send out an ARP reply on that interface: <br>
   * <br>
   * 1) PERMIT IPs belonging to the interface.<br>
   * 2) (Proxy-ARP) DENY any IP for which there is a longest-prefix match entry in the FIB that goes
   * through the interface.<br>
   * 3) (Proxy-ARP) PERMIT any other IP routable via the VRF of the interface.
   */
  @VisibleForTesting
  Map<String, Map<String, IpSpace>> computeArpReplies(
      Map<String, Configuration> configurations,
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
    Map<String, Map<String, IpSpace>> routableIpsByNodeVrf = computeRoutableIpsByNodeVrf(ribs);
    return ribs.entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey, // hostname
                ribsByNodeEntry -> {
                  String hostname = ribsByNodeEntry.getKey();
                  Map<String, Interface> interfaces =
                      configurations.get(hostname).getAllInterfaces();
                  Map<String, IpSpace> routableIpsByVrf = routableIpsByNodeVrf.get(hostname);
                  Map<String, IpSpace> ipsRoutedOutInterfaces =
                      _ipsRoutedOutInterfaces.get(hostname);
                  return computeArpRepliesByInterface(
                      interfaces, routableIpsByVrf, ipsRoutedOutInterfaces);
                }));
  }

  @VisibleForTesting
  Map<String, IpSpace> computeArpRepliesByInterface(
      Map<String, Interface> interfaces,
      Map<String, IpSpace> routableIpsByVrf,
      Map<String, IpSpace> ipsRoutedOutInterfaces) {
    ImmutableMap.Builder<String, IpSpace> arpRepliesByInterfaceBuilder = ImmutableMap.builder();
    ipsRoutedOutInterfaces.forEach(
        (iface, ipsRoutedOutIface) -> {
          IpSpace routableIpsForThisVrf = routableIpsByVrf.get(interfaces.get(iface).getVrfName());
          arpRepliesByInterfaceBuilder.put(
              iface,
              computeInterfaceArpReplies(
                  interfaces.get(iface), routableIpsForThisVrf, ipsRoutedOutIface));
        });
    return arpRepliesByInterfaceBuilder.build();
  }

  @VisibleForTesting
  Map<Edge, IpSpace> computeArpTrueEdge() {
    return Sets.union(_arpTrueEdgeDestIp.keySet(), _arpTrueEdgeNextHopIp.keySet())
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Function.identity(),
                edge -> {
                  AclIpSpace.Builder ipSpace = AclIpSpace.builder();
                  IpSpace dstIp = _arpTrueEdgeDestIp.get(edge);
                  if (dstIp != null) {
                    ipSpace.thenPermitting(dstIp);
                  }
                  IpSpace nextHopIp = _arpTrueEdgeNextHopIp.get(edge);
                  if (nextHopIp != null) {
                    ipSpace.thenPermitting(nextHopIp);
                  }
                  return ipSpace.build();
                }));
  }

  @VisibleForTesting
  Map<Edge, IpSpace> computeArpTrueEdgeDestIp(
      Map<String, Configuration> configurations,
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
    return _routesWithDestIpEdge
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* edge */,
                routesWithDestIpEdgeEntry -> {
                  Edge edge = routesWithDestIpEdgeEntry.getKey();
                  Set<AbstractRoute> routes = routesWithDestIpEdgeEntry.getValue();
                  String hostname = edge.getNode1();
                  String iface = edge.getInt1();
                  String vrf =
                      configurations.get(hostname).getAllInterfaces().get(iface).getVrfName();
                  GenericRib<AbstractRoute> rib = ribs.get(hostname).get(vrf);
                  IpSpace dstIpMatchesSomeRoutePrefix = computeRouteMatchConditions(routes, rib);
                  String recvNode = edge.getNode2();
                  String recvInterface = edge.getInt2();
                  IpSpace recvReplies = _arpReplies.get(recvNode).get(recvInterface);
                  return AclIpSpace.rejecting(dstIpMatchesSomeRoutePrefix.complement())
                      .thenPermitting(recvReplies)
                      .build();
                }));
  }

  @VisibleForTesting
  Map<Edge, IpSpace> computeArpTrueEdgeNextHopIp(
      Map<String, Configuration> configurations,
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
    return _routesWithNextHopIpArpTrue
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* edge */,
                routesWithNextHopIpArpTrueEntry -> {
                  Edge edge = routesWithNextHopIpArpTrueEntry.getKey();
                  String hostname = edge.getNode1();
                  String iface = edge.getInt1();
                  String vrf =
                      configurations.get(hostname).getAllInterfaces().get(iface).getVrfName();
                  GenericRib<AbstractRoute> rib = ribs.get(hostname).get(vrf);
                  Set<AbstractRoute> routes = routesWithNextHopIpArpTrueEntry.getValue();
                  return computeRouteMatchConditions(routes, rib);
                }));
  }

  @VisibleForTesting
  IpSpace computeInterfaceArpReplies(
      Interface iface, IpSpace routableIpsForThisVrf, IpSpace ipsRoutedThroughInterface) {
    IpSpace ipsAssignedToThisInterface = computeIpsAssignedToThisInterface(iface);
    if (ipsAssignedToThisInterface == EmptyIpSpace.INSTANCE) {
      // if no IPs are assigned to this interface, it replies to no ARP requests.
      return EmptyIpSpace.INSTANCE;
    }
    /* Accept IPs assigned to this interface */
    AclIpSpace.Builder interfaceArpReplies = AclIpSpace.permitting(ipsAssignedToThisInterface);
    if (iface.getProxyArp()) {
      /* Reject IPs routed through this interface */
      interfaceArpReplies.thenRejecting(ipsRoutedThroughInterface);

      /* Accept all other routable IPs */
      interfaceArpReplies.thenPermitting(routableIpsForThisVrf);
    }
    return interfaceArpReplies.build();
  }

  @VisibleForTesting
  IpSpace computeIpsAssignedToThisInterface(Interface iface) {
    Set<Ip> ips = _interfaceOwnedIps.get(iface.getOwner().getHostname()).get(iface.getName());
    if (ips == null || ips.isEmpty()) {
      return EmptyIpSpace.INSTANCE;
    }
    IpWildcardSetIpSpace.Builder ipsAssignedToThisInterfaceBuilder = IpWildcardSetIpSpace.builder();
    ips.forEach(ip -> ipsAssignedToThisInterfaceBuilder.including(new IpWildcard(ip)));
    IpWildcardSetIpSpace ipsAssignedToThisInterface = ipsAssignedToThisInterfaceBuilder.build();
    return ipsAssignedToThisInterface;
  }

  @VisibleForTesting
  Map<String, Map<String, IpSpace>> computeIpsRoutedOutInterfaces(
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
    return _routesWithNextHop
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                routesWithNextHopByHostnameEntry -> {
                  String hostname = routesWithNextHopByHostnameEntry.getKey();
                  ImmutableMap.Builder<String, IpSpace> ipsRoutedOutInterfacesByInterface =
                      ImmutableMap.builder();
                  routesWithNextHopByHostnameEntry
                      .getValue()
                      .forEach(
                          (vrf, routesWithNextHopByInterface) -> {
                            GenericRib<AbstractRoute> rib = ribs.get(hostname).get(vrf);
                            routesWithNextHopByInterface.forEach(
                                (iface, routes) -> {
                                  /*
                                   *  Cannot determine IPs for null interface here because it is
                                   *  not tied to a single VRF.
                                   */
                                  if (iface.equals(Interface.NULL_INTERFACE_NAME)) {
                                    return;
                                  }
                                  ipsRoutedOutInterfacesByInterface.put(
                                      iface, computeRouteMatchConditions(routes, rib));
                                });
                          });
                  return ipsRoutedOutInterfacesByInterface.build();
                }));
  }

  @VisibleForTesting
  Map<String, Map<String, Map<String, IpSpace>>> computeNeighborUnreachableOrExitsNetwork() {
    Map<String, Map<String, Map<String, ImmutableList.Builder<IpSpace>>>> neighborUnreachable =
        new HashMap<>();
    computeNeighborUnreachableHelper(neighborUnreachable, _neighborUnreachableArpDestIp);
    computeNeighborUnreachableHelper(neighborUnreachable, _neighborUnreachableArpNextHopIp);
    return neighborUnreachable
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                neighborUnreachableByHostnameEntry ->
                    neighborUnreachableByHostnameEntry
                        .getValue()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey /* vrf */,
                                neighborUnreachableByVrfEntry ->
                                    neighborUnreachableByVrfEntry
                                        .getValue()
                                        .entrySet()
                                        .stream()
                                        .collect(
                                            ImmutableMap.toImmutableMap(
                                                Entry::getKey /* outInterface */,
                                                neighborUnreachableByOutInterfaceEntry ->
                                                    AclIpSpace.permitting(
                                                            neighborUnreachableByOutInterfaceEntry
                                                                .getValue()
                                                                .build())
                                                        .build()))))));
  }

  @VisibleForTesting
  Map<String, Map<String, Map<String, IpSpace>>> computeNeighborUnreachableArpDestIp(
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
    return _routesWhereDstIpCanBeArpIp
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                routesWhereDstIpCanBeArpIpByHostnameEntry -> {
                  String hostname = routesWhereDstIpCanBeArpIpByHostnameEntry.getKey();
                  return routesWhereDstIpCanBeArpIpByHostnameEntry
                      .getValue()
                      .entrySet()
                      .stream()
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Entry::getKey /* vrf */,
                              routesWhereDstIpCanBeArpIpByVrfEntry -> {
                                String vrf = routesWhereDstIpCanBeArpIpByVrfEntry.getKey();
                                return routesWhereDstIpCanBeArpIpByVrfEntry
                                    .getValue()
                                    .entrySet()
                                    .stream()
                                    /* null_interface is handled in computeNullRoutedIps */
                                    .filter(
                                        entry ->
                                            !entry.getKey().equals(Interface.NULL_INTERFACE_NAME))
                                    .collect(
                                        ImmutableMap.toImmutableMap(
                                            Entry::getKey /* outInterface */,
                                            routesWhereDstIpCanBeArpIpByInterfaceEntry -> {
                                              String outInterface =
                                                  routesWhereDstIpCanBeArpIpByInterfaceEntry
                                                      .getKey();
                                              Set<AbstractRoute> routes =
                                                  routesWhereDstIpCanBeArpIpByInterfaceEntry
                                                      .getValue();
                                              IpSpace someoneReplies =
                                                  _someoneReplies
                                                      .getOrDefault(hostname, ImmutableMap.of())
                                                      .getOrDefault(
                                                          outInterface, EmptyIpSpace.INSTANCE);
                                              GenericRib<AbstractRoute> rib =
                                                  ribs.get(hostname).get(vrf);
                                              IpSpace ipsRoutedOutInterface =
                                                  computeRouteMatchConditions(routes, rib);
                                              return AclIpSpace.rejecting(someoneReplies)
                                                  .thenPermitting(ipsRoutedOutInterface)
                                                  .build();
                                            }));
                              }));
                }));
  }

  @VisibleForTesting
  Map<String, Map<String, Map<String, IpSpace>>> computeNeighborUnreachableArpNextHopIp(
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
    return _routesWithNextHopIpArpFalse
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                routesWithNextHopIpArpFalseByHostnameEntry -> {
                  String hostname = routesWithNextHopIpArpFalseByHostnameEntry.getKey();
                  return routesWithNextHopIpArpFalseByHostnameEntry
                      .getValue()
                      .entrySet()
                      .stream()
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Entry::getKey /* vrf */,
                              routesWithNextHopIpArpFalseByVrfEntry -> {
                                String vrf = routesWithNextHopIpArpFalseByVrfEntry.getKey();
                                return routesWithNextHopIpArpFalseByVrfEntry
                                    .getValue()
                                    .entrySet()
                                    .stream()
                                    /* null_interface is handled in computeNullRoutedIps */
                                    .filter(
                                        entry ->
                                            !entry.getKey().equals(Interface.NULL_INTERFACE_NAME))
                                    .collect(
                                        ImmutableMap.toImmutableMap(
                                            Entry::getKey /* outInterface */,
                                            routesWithNextHopIpArpFalseByOutInterfaceEntry ->
                                                computeRouteMatchConditions(
                                                    routesWithNextHopIpArpFalseByOutInterfaceEntry
                                                        .getValue(),
                                                    ribs.get(hostname).get(vrf))));
                              }));
                }));
  }

  @VisibleForTesting
  void computeNeighborUnreachableHelper(
      Map<String, Map<String, Map<String, ImmutableList.Builder<IpSpace>>>> neighborUnreachable,
      Map<String, Map<String, Map<String, IpSpace>>> part) {
    part.forEach(
        (hostname, partByVrf) -> {
          Map<String, Map<String, ImmutableList.Builder<IpSpace>>> neighborUnreachableByVrf =
              neighborUnreachable.computeIfAbsent(hostname, n -> new HashMap<>());
          partByVrf.forEach(
              (vrf, partByOutInterface) -> {
                Map<String, ImmutableList.Builder<IpSpace>> neighborUnreachableByOutInterface =
                    neighborUnreachableByVrf.computeIfAbsent(vrf, n -> new HashMap<>());
                partByOutInterface.forEach(
                    (outInterface, ipSpace) ->
                        neighborUnreachableByOutInterface
                            .computeIfAbsent(outInterface, n -> ImmutableList.builder())
                            .add(ipSpace));
              });
        });
  }

  @VisibleForTesting
  Map<String, Map<String, IpSpace>> computeNullRoutedIps(
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs,
      Map<String, Map<String, Fib>> fibs) {
    return fibs.entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                fibsByHostnameEntry -> {
                  String hostname = fibsByHostnameEntry.getKey();
                  return fibsByHostnameEntry
                      .getValue()
                      .entrySet()
                      .stream()
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Entry::getKey /* vrf */,
                              fibsByVrfEntry -> {
                                String vrf = fibsByVrfEntry.getKey();
                                Fib fib = fibsByVrfEntry.getValue();
                                GenericRib<AbstractRoute> rib = ribs.get(hostname).get(vrf);
                                Set<AbstractRoute> nullRoutes =
                                    fib.getNextHopInterfaces()
                                        .entrySet()
                                        .stream()
                                        .filter(
                                            nextHopInterfacesByRouteEntry ->
                                                nextHopInterfacesByRouteEntry
                                                    .getValue()
                                                    .keySet()
                                                    .contains(Interface.NULL_INTERFACE_NAME))
                                        .map(Entry::getKey)
                                        .collect(ImmutableSet.toImmutableSet());
                                return computeRouteMatchConditions(nullRoutes, rib);
                              }));
                }));
  }

  @VisibleForTesting
  Map<String, Map<String, IpSpace>> computeRoutableIps(
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
    return ribs.entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                ribsByHostnameEntry ->
                    ribsByHostnameEntry
                        .getValue()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey /* vrf */,
                                ribsByVrfEntry -> ribsByVrfEntry.getValue().getRoutableIps()))));
  }

  /** Compute for each VRF of each node the IPs that are routable. */
  @VisibleForTesting
  Map<String, Map<String, IpSpace>> computeRoutableIpsByNodeVrf(
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
    return ribs.entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey, // hostname
                ribsByNodeEntry ->
                    ribsByNodeEntry
                        .getValue()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey, // vrfName
                                ribsByVrfEntry -> ribsByVrfEntry.getValue().getRoutableIps()))));
  }

  @VisibleForTesting
  IpSpace computeRouteMatchConditions(Set<AbstractRoute> routes, GenericRib<AbstractRoute> rib) {
    Map<Prefix, IpSpace> matchingIps = rib.getMatchingIps();
    return AclIpSpace.permitting(
            routes
                .stream()
                .map(AbstractRoute::getNetwork)
                .collect(ImmutableSet.toImmutableSet())
                .stream()
                .map(matchingIps::get))
        .build();
  }

  /*
   * Mapping: hostname -&gt; vrfname -&gt; interfacename -&gt; a set of routes where each route
   * has at least one unset final next hop ip
   */
  @VisibleForTesting
  Map<String, Map<String, Map<String, Set<AbstractRoute>>>> computeRoutesWhereDstIpCanBeArpIp(
      Map<String, Map<String, Fib>> fibs) {
    return _routesWithNextHop
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                routesWithNextHopByHostnameEntry -> {
                  String hostname = routesWithNextHopByHostnameEntry.getKey();
                  return routesWithNextHopByHostnameEntry
                      .getValue()
                      .entrySet()
                      .stream()
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Entry::getKey /* vrf */,
                              routesWithNextHopByVrfEntry -> {
                                String vrf = routesWithNextHopByVrfEntry.getKey();
                                Fib fib = fibs.get(hostname).get(vrf);
                                return routesWithNextHopByVrfEntry
                                    .getValue()
                                    .entrySet()
                                    .stream()
                                    .collect(
                                        ImmutableMap.toImmutableMap(
                                            Entry::getKey /* interface */,
                                            routesWithNextHopByInterfaceEntry -> {
                                              String iface =
                                                  routesWithNextHopByInterfaceEntry.getKey();
                                              // return a set of routes where each route has
                                              // some final next hop ip unset
                                              return routesWithNextHopByInterfaceEntry
                                                  .getValue() // routes with this interface as
                                                  // outgoing interfaces
                                                  .stream()
                                                  .filter(
                                                      route ->
                                                          fib.getNextHopInterfaces()
                                                              .get(route)
                                                              .get(iface)
                                                              .keySet() // final next hop ips
                                                              .contains(
                                                                  Route.UNSET_ROUTE_NEXT_HOP_IP))
                                                  .collect(ImmutableSet.toImmutableSet());
                                            }));
                              }));
                }));
  }

  @VisibleForTesting
  Map<Edge, Set<AbstractRoute>> computeRoutesWithDestIpEdge(
      Map<String, Map<String, Fib>> fibs, Topology topology) {
    ImmutableMap.Builder<Edge, Set<AbstractRoute>> routesByEdgeBuilder = ImmutableMap.builder();
    _routesWhereDstIpCanBeArpIp.forEach(
        (hostname, routesWhereDstIpCanBeArpIpByVrf) ->
            routesWhereDstIpCanBeArpIpByVrf.forEach(
                (vrf, routesWhereDstIpCanBeArpIpByOutInterface) ->
                    routesWhereDstIpCanBeArpIpByOutInterface.forEach(
                        (outInterface, routes) -> {
                          NodeInterfacePair out = new NodeInterfacePair(hostname, outInterface);
                          Set<NodeInterfacePair> receivers = topology.getNeighbors(out);
                          receivers.forEach(
                              receiver -> routesByEdgeBuilder.put(new Edge(out, receiver), routes));
                        })));
    return routesByEdgeBuilder.build();
  }

  /* Mapping: hostname -&gt; vrfname -&gt; interfacename -&gt; set of associated routes (i.e.,
   * routes that use the interface as outgoing interface */
  @VisibleForTesting
  Map<String, Map<String, Map<String, Set<AbstractRoute>>>> computeRoutesWithNextHop(
      Map<String, Map<String, Fib>> fibs) {
    return fibs.entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                fibsByHostnameEntry -> {
                  return fibsByHostnameEntry
                      .getValue()
                      .entrySet()
                      .stream()
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Entry::getKey /* vrf */,
                              fibsByVrfEntry -> {
                                Fib fib = fibsByVrfEntry.getValue();
                                return fib.getRoutesByNextHopInterface();
                              }));
                }));
  }

  @VisibleForTesting
  Map<String, Map<String, Map<String, Set<AbstractRoute>>>> computeRoutesWithNextHopIpArpFalse(
      Map<String, Map<String, Fib>> fibs) {
    return _routesWithNextHop
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                routesWithNextHopByHostnameEntry -> {
                  String hostname = routesWithNextHopByHostnameEntry.getKey();
                  return routesWithNextHopByHostnameEntry
                      .getValue()
                      .entrySet()
                      .stream()
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Entry::getKey /* vrf */,
                              routesWithNextHopByVrfEntry -> {
                                String vrf = routesWithNextHopByVrfEntry.getKey();
                                return routesWithNextHopByVrfEntry
                                    .getValue()
                                    .entrySet()
                                    .stream()
                                    .collect(
                                        ImmutableMap.toImmutableMap(
                                            Entry::getKey /* outInterface */,
                                            routesWithNextHopByOutInterfaceEntry -> {
                                              Fib fib = fibs.get(hostname).get(vrf);
                                              return computeRoutesWithNextHopIpArpFalseForInterface(
                                                  fib,
                                                  hostname,
                                                  routesWithNextHopByOutInterfaceEntry);
                                            }));
                              }));
                }));
  }

  @VisibleForTesting
  Set<AbstractRoute> computeRoutesWithNextHopIpArpFalseForInterface(
      Fib fib,
      String hostname,
      Entry<String, Set<AbstractRoute>> routesWithNextHopByOutInterfaceEntry) {
    String outInterface = routesWithNextHopByOutInterfaceEntry.getKey();
    IpSpace someoneReplies =
        _someoneReplies
            .getOrDefault(hostname, ImmutableMap.of())
            .getOrDefault(outInterface, EmptyIpSpace.INSTANCE);
    Set<AbstractRoute> candidateRoutes = routesWithNextHopByOutInterfaceEntry.getValue();
    return candidateRoutes
        .stream()
        .filter(
            candidateRoute ->
                fib.getNextHopInterfaces()
                    .get(candidateRoute)
                    .get(outInterface)
                    .keySet()
                    .stream()
                    .filter(ip -> !ip.equals(Route.UNSET_ROUTE_NEXT_HOP_IP))
                    .anyMatch(
                        Predicates.not(
                            nextHopIp -> someoneReplies.containsIp(nextHopIp, ImmutableMap.of()))))
        .collect(ImmutableSet.toImmutableSet());
  }

  @VisibleForTesting
  Map<Edge, Set<AbstractRoute>> computeRoutesWithNextHopIpArpTrue(
      Map<String, Map<String, Fib>> fibs, Topology topology) {
    ImmutableMap.Builder<Edge, Set<AbstractRoute>> routesByEdgeBuilder = ImmutableMap.builder();
    _routesWithNextHop.forEach(
        (hostname, routesWithNextHopByVrf) ->
            routesWithNextHopByVrf.forEach(
                (vrf, routesWithNextHopByInterface) ->
                    routesWithNextHopByInterface.forEach(
                        (outInterface, candidateRoutes) -> {
                          Fib fib = fibs.get(hostname).get(vrf);
                          NodeInterfacePair out = new NodeInterfacePair(hostname, outInterface);
                          Set<NodeInterfacePair> receivers = topology.getNeighbors(out);
                          receivers.forEach(
                              receiver -> {
                                String recvNode = receiver.getHostname();
                                String recvInterface = receiver.getInterface();
                                IpSpace recvReplies = _arpReplies.get(recvNode).get(recvInterface);
                                Edge edge = new Edge(out, receiver);
                                Set<AbstractRoute> routes =
                                    candidateRoutes
                                        .stream()
                                        .filter(
                                            route ->
                                                fib.getNextHopInterfaces()
                                                    .get(route)
                                                    .get(outInterface)
                                                    .keySet() /* nextHopIps */
                                                    .stream()
                                                    .filter(
                                                        ip ->
                                                            !ip.equals(
                                                                Route.UNSET_ROUTE_NEXT_HOP_IP))
                                                    .anyMatch(
                                                        nextHopIp ->
                                                            recvReplies.containsIp(
                                                                nextHopIp, ImmutableMap.of())))
                                        .collect(ImmutableSet.toImmutableSet());
                                routesByEdgeBuilder.put(edge, routes);
                              });
                        })));
    return routesByEdgeBuilder.build();
  }

  @VisibleForTesting
  Map<String, Map<String, IpSpace>> computeSomeoneReplies(Topology topology) {
    Map<String, Map<String, AclIpSpace.Builder>> someoneRepliesByNode = new HashMap<>();
    topology
        .getEdges()
        .forEach(
            edge ->
                someoneRepliesByNode
                    .computeIfAbsent(edge.getNode1(), n -> new HashMap<>())
                    .computeIfAbsent(edge.getInt1(), i -> AclIpSpace.builder())
                    .thenPermitting((_arpReplies.get(edge.getNode2()).get(edge.getInt2()))));
    return someoneRepliesByNode
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                someoneRepliesByNodeEntry ->
                    someoneRepliesByNodeEntry
                        .getValue()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey /* interface */,
                                someoneRepliesByInterfaceEntry ->
                                    someoneRepliesByInterfaceEntry.getValue().build()))));
  }

  @Override
  public Map<String, Map<String, IpSpace>> getArpReplies() {
    return _arpReplies;
  }

  @Override
  public Map<Edge, IpSpace> getArpTrueEdge() {
    return _arpTrueEdge;
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getNeighborUnreachableOrExitsNetwork() {
    return _neighborUnreachableOrExitsNetwork;
  }

  @Override
  public Map<String, Map<String, IpSpace>> getNullRoutedIps() {
    return _nullRoutedIps;
  }

  @Override
  public Map<String, Map<String, IpSpace>> getRoutableIps() {
    return _routableIps;
  }

  @Override
  public Map<String, Map<String, IpSpace>> getDeliveredToSubnet() {
    return _deliveredToSubnet;
  }

  @Override
  public boolean isIpInSnapshot(Ip ip) {
    return ip.toIpSpace().accept(_ipSpaceToBDD).and(_snapshotOwnedIpBDD).isZero();
  }

  BDD computeVrfOwnedIpBDD(Map<String, Configuration> configurations) {
    Map<Ip, Map<String, Set<String>>> ipInterfaceOwners =
        CommonUtil.computeIpInterfaceOwners(CommonUtil.computeNodeInterfaces(configurations), true);
    Map<String, Map<String, IpSpace>> vrfOwnedIps =
        CommonUtil.computeVrfOwnedIpSpaces(
            CommonUtil.computeIpVrfOwners(ipInterfaceOwners, configurations));
    return vrfOwnedIps
        .entrySet()
        .stream()
        .flatMap(
            nodeEntry ->
                nodeEntry
                    .getValue()
                    .entrySet()
                    .stream()
                    .map(vrfEntry -> vrfEntry.getValue().accept(_ipSpaceToBDD)))
        .reduce(_ipSpaceToBDD.getBDDInteger().getFactory().zero(), BDD::or);
  }

  BDD computeHostSubnetBDDPerPrefix(Prefix subnetPrefix, IpSpaceToBDD ipSpaceToBDD) {
    BDD subnetBDD = subnetPrefix.toIpSpace().accept(ipSpaceToBDD);
    if (subnetPrefix.getPrefixLength() >= 31) {
      return subnetBDD;
    }
    BDD firstIpNegBdd = subnetPrefix.getStartIp().toIpSpace().accept(ipSpaceToBDD).not();
    BDD endIpNegBdd = subnetPrefix.getEndIp().toIpSpace().accept(ipSpaceToBDD).not();
    return subnetBDD.and(firstIpNegBdd).and(endIpNegBdd);
  }

  BDD computeHostSubnetBDDPerInterface(Interface iface) {
    return iface
        .getAllAddresses()
        .stream()
        .map(ifaceAddress -> computeHostSubnetBDDPerPrefix(ifaceAddress.getPrefix(), _ipSpaceToBDD))
        .reduce(_ipSpaceToBDD.getBDDInteger().getFactory().zero(), BDD::or);
  }

  Map<String, Map<String, BDD>> computeHostSubnetBDDs(Map<String, Configuration> configurations) {
    return configurations
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* node name */,
                nodeEntry ->
                    nodeEntry
                        .getValue()
                        .getAllInterfaces()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey /* interface name */,
                                ifaceEntry ->
                                    computeHostSubnetBDDPerInterface(ifaceEntry.getValue())))));
  }

  BDD computeSnapshotOwnedIps() {
    return _hostSubnetBDDs
        .entrySet()
        .stream()
        .flatMap(nodeEntry -> nodeEntry.getValue().entrySet().stream().map(Entry::getValue))
        .reduce(_ipSpaceToBDD.getBDDInteger().getFactory().zero(), BDD::or);
  }

  IpSpace computeDeliveredToSubnetPerInterface(
      String hostname, String interfaceName, Configuration configuration) {
    return null;
    // TODO: complete this function
    /*
    Interface outgoingInterface = configuration.getAllInterfaces().get(interfaceName);
    // compute all dst IPs that delivered to a host subnet
    AclIpSpace.Builder ipSpaceBuilder = AclIpSpace.builder();
    ipSpaceBuilder.thenPermitting(
        outgoingInterface
            .getAllAddresses()
            .stream()
            .map(InterfaceAddress::getPrefix)
            .filter(prefix -> prefix.getPrefixLength() <= NodeNameRegexConnectedHostsIpSpaceSpecifier
                .HOST_SUBNET_MAX_PREFIX_LENGTH)
            .map(Prefix::toIpSpace));

    // compute all dst IPs routed to this interface
    // IpSpace ipsRoutedOutThisInterface = _ipsRoutedOutInterfaces.get(hostname).get(interfaceName);

    // dst IPs that are delivered to subnet through this interface should be
    // the intersection of the two IpSpaces above
    return AclIpSpace.intersection(
            _neighborUnreachableOrExitsNetwork
                .get(hostname)
                .get(outgoingInterface.getVrfName())
                .get(interfaceName),
            _snapshotOwnedIps.complement(),
            ipSpaceBuilder.build());
    */
  }

  Map<String, IpSpace> computeDeliveredToSubnetPerHost(
      String hostname, Configuration configuration) {
    return configuration
        .getAllInterfaces()
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* interface name */,
                entry -> {
                  String interfaceName = entry.getKey();
                  return computeDeliveredToSubnetPerInterface(
                      hostname, interfaceName, configuration);
                }));
  }

  Map<String, Map<String, IpSpace>> computeDeliveredToSubnet(
      Map<String, Configuration> configurations) {
    return configurations
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                entry -> {
                  String hostname = entry.getKey();
                  Configuration configuration = entry.getValue();
                  return computeDeliveredToSubnetPerHost(hostname, configuration);
                }));
  }

  @Override
  public Map<String, Map<String, IpSpace>> getExitsNetwork() {
    return _exitsNetwork;
  }

  IpSpace computeExitsNetworkPerInterface(
      String hostname, String interfaceName, Configuration configuration) {

    return null;
    // TODO: complete this function
    /*
    Interface outgoingInterface = configuration.getAllInterfaces().get(interfaceName);

    // IP space for interfaces with prefix length > 29
    IpSpace ipSpaceExitsViaInfraIface =
        AclIpSpace.permitting(
                outgoingInterface
                    .getAllAddresses()
                    .stream()
                    .map(InterfaceAddress::getPrefix)
                    .filter(
                        prefix ->
                            prefix.getPrefixLength()
                                > NodeNameRegexConnectedHostsIpSpaceSpecifier
                                    .HOST_SUBNET_MAX_PREFIX_LENGTH)
                    .map(Prefix::toIpSpace))
            .build();

    IpSpace ipSpace1 =
        AclIpSpace.intersection(
            _neighborUnreachableOrExitsNetwork
                .get(hostname)
                .get(outgoingInterface.getVrfName())
                .get(interfaceName),
            _snapshotOwnedIps.complement(),
            ipSpaceExitsViaInfraIface);

    if (hasMissingDevicesOnInterface(outgoingInterface)) {
      IpSpace ipSpaceNotInIfaces =
          AclIpSpace.permitting(
                  outgoingInterface
                      .getAllAddresses()
                      .stream()
                      .map(InterfaceAddress::getPrefix)
                      .map(Prefix::toIpSpace)
          ).build().complement();

      IpSpace ipSpace2 =
          AclIpSpace.intersection(
              _neighborUnreachableOrExitsNetwork
                  .get(hostname)
                  .get(outgoingInterface.getVrfName())
                  .get(interfaceName),
              _snapshotOwnedIps.complement(),
              ipSpaceNotInIfaces);

       IpSpace ipSpace3Post =
          AclIpSpace.intersection(
              _neighborUnreachableOrExitsNetwork
                .get(hostname)
                .get(outgoingInterface.getVrfName())
                .get(interfaceName),
              _snapshotOwnedIps.complement());

       //IpSpace ipSpace3 = getDstIpSpaceForNHIpSpace(ipSpace3Post);

      //return AclIpSpace.union(ipSpace1, ipSpace2, ipSpace3);
      return AclIpSpace.union(ipSpace1, ipSpace2);
    }
    return ipSpace1;
    */
  }

  Map<String, IpSpace> computeExitsNetworkPerHost(String hostname, Configuration configuration) {
    return configuration
        .getAllInterfaces()
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* interface name */,
                entry -> {
                  String interfaceName = entry.getKey();
                  return computeExitsNetworkPerInterface(hostname, interfaceName, configuration);
                }));
  }

  Map<String, Map<String, IpSpace>> computeExitsNetwork(Map<String, Configuration> configurations) {
    return configurations
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* hostname */,
                entry -> {
                  String hostname = entry.getKey();
                  Configuration configuration = entry.getValue();
                  return computeExitsNetworkPerHost(hostname, configuration);
                }));
  }

  Map<String, Map<String, IpSpace>> computeNeighborUnreachable() {
    return _neighborUnreachableOrExitsNetwork
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* host name */,
                nodeEntry -> {
                  String hostname = nodeEntry.getKey();
                  ImmutableMap.Builder<String, IpSpace> mapBuilder = new ImmutableMap.Builder<>();
                  nodeEntry
                      .getValue()
                      .values()
                      .stream()
                      .forEach(
                          ifaceIpspaceMap ->
                              ifaceIpspaceMap
                                  .entrySet()
                                  .stream()
                                  .forEach(
                                      entry -> {
                                        String ifaceName = entry.getKey();
                                        IpSpace ipSpace = entry.getValue();
                                        AclIpSpace.Builder aclIpSpaceBuilder = AclIpSpace.builder();
                                        aclIpSpaceBuilder.thenRejecting(
                                            _deliveredToSubnet.get(hostname).get(ifaceName));
                                        aclIpSpaceBuilder.thenRejecting(
                                            _exitsNetwork.get(hostname).get(ifaceName));
                                        aclIpSpaceBuilder.thenPermitting(ipSpace);
                                        mapBuilder.put(ifaceName, aclIpSpaceBuilder.build());
                                      }));
                  return mapBuilder.build();
                }));
  }

  @Override
  public Map<String, Map<String, IpSpace>> getNeighborUnreachable() {
    return _neighborUnreachable;
  }

  public BDD getSnapshotOwnedIpBDD() {
    return _snapshotOwnedIpBDD;
  }

  private boolean hasMissingDevicesOnInterface(String hostname, String ifaceName) {
    return !_hostSubnetBDDs.get(hostname).get(ifaceName).and(_vrfOwnedIpBDD.not()).isZero();
  }

  private Map<String, Set<String>> computeInterfacesWithMissingDevices(
      Map<String, Configuration> configurations) {
    return configurations
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* node name */,
                nodeEntry ->
                    nodeEntry
                        .getValue()
                        .getAllInterfaces()
                        .entrySet()
                        .stream()
                        .filter(
                            ifaceEntry ->
                                hasMissingDevicesOnInterface(
                                    nodeEntry.getKey(), ifaceEntry.getKey()))
                        .map(Entry::getKey)
                        .collect(Collectors.toSet())));
  }

  @Override
  public Map<String, Set<String>> getInterfacesWithMissingDevices() {
    return _interfacesWithMissingDevices;
  }
}

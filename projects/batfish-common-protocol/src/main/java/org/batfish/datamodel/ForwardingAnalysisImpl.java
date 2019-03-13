package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Ordering.natural;
import static org.batfish.common.util.CommonUtil.toImmutableMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.datamodel.collections.NodeInterfacePair;

/** Implementation of {@link ForwardingAnalysis}. */
public final class ForwardingAnalysisImpl implements ForwardingAnalysis {

  // node -> interface -> ips that the interface would reply arp request
  private final Map<String, Map<String, IpSpace>> _arpReplies;

  /* node -> vrf -> edge -> dest IPs for which the vrf will forward out the source of the edge,
   * ARPing for some ARP IP and receiving a reply from thhe target of the edge.
   */
  private final Map<String, Map<String, Map<Edge, IpSpace>>> _arpTrueEdge;

  private final Map<String, Map<String, Map<String, IpSpace>>> _arpFalse;

  private final Map<String, Map<String, IpSpace>> _nullRoutedIps;

  private final Map<String, Map<String, IpSpace>> _routableIps;

  // node -> vrf -> interface -> dst ips that end up with neighbor unreachable
  private final Map<String, Map<String, Map<String, IpSpace>>> _neighborUnreachable;

  // node -> vrf -> interface -> dst ips that end up delivered to subnet
  private final Map<String, Map<String, Map<String, IpSpace>>> _deliveredToSubnet;

  // node -> vrf -> interface -> dst ips that end up exiting the network
  private final Map<String, Map<String, Map<String, IpSpace>>> _exitsNetwork;

  // node -> vrf -> interface -> dst ips that end up with insufficient info
  private final Map<String, Map<String, Map<String, IpSpace>>> _insufficientInfo;

  public <T extends AbstractRouteDecorator> ForwardingAnalysisImpl(
      Map<String, Configuration> configurations,
      Map<String, Map<String, Fib>> fibs,
      Topology topology) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("Construct ForwardingAnalysisImpl").startActive()) {
      assert span != null; // avoid unused warning

      // TODO accept IpSpaceToBDD as parameter
      IpSpaceToBDD ipSpaceToBDD = new IpSpaceToBDD(new BDDPacket().getDstIp());
      Map<String, Map<String, Set<Ip>>> interfaceOwnedIps =
          TopologyUtil.computeInterfaceOwnedIps(configurations, false);
      // ips belonging to any interface in the network
      IpSpace ownedIps = computeOwnedIps(interfaceOwnedIps);
      BDD unownedIpsBDD = ipSpaceToBDD.visit(ownedIps).not();

      // IpSpaces matched by each prefix.
      Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps = computeMatchingIps(fibs);
      // Set of routes that forward out each interface
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop =
          computeRoutesWithNextHop(configurations, fibs);
      _nullRoutedIps = computeNullRoutedIps(matchingIps, fibs);
      _routableIps = computeRoutableIps(fibs);

      /* Compute _arpReplies: for each interface, the set of arp IPs for which that interface will
       * respond.
       */
      {
        // mapping: node name -> interface name -> dst ips which are routed to the interface
        Map<String, Map<String, IpSpace>> ipsRoutedOutInterfaces =
            computeIpsRoutedOutInterfaces(matchingIps, routesWithNextHop);
        _arpReplies =
            computeArpReplies(
                configurations, ipsRoutedOutInterfaces, interfaceOwnedIps, _routableIps);
      }

      /* Compute ARP stuff bottom-up from _arpReplies. */

      /* node -> vrf -> interface -> dst IPs for which that VRF forwards out that interface, ARPing
       * for the dst ip itself with no reply
       */
      Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp;
      /* node -> vrf -> interface -> dst IPs for which that VRF forwards out that interface, ARPing
       *for some unowned next-hop IP with no reply
       */
      Map<String, Map<String, Map<String, IpSpace>>> dstIpsWithUnownedNextHopIpArpFalse;
      /* node -> vrf -> interface -> dst IPs for which that VRF forwards out that interface, ARPing
       * for some owned next-hop IP with no reply
       */
      Map<String, Map<String, Map<String, IpSpace>>> dstIpsWithOwnedNextHopIpArpFalse;
      {
        Map<String, Map<String, IpSpace>> someoneReplies =
            computeSomeoneReplies(topology, _arpReplies);

        /* node -> vrf -> interface -> set of routes on that vrf that forward out that interface
         * with a next hop ip that gets no arp replies
         */
        Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHopIpArpFalse =
            computeRoutesWithNextHopIpArpFalse(fibs, routesWithNextHop, someoneReplies);

        /* node -> vrf -> interface -> set of routes on that vrf that forward out that interface
         * with next hop ip not owned by the snapshot devices and that gets no arp reply
         */
        Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
            routesWithUnownedNextHopIpArpFalse =
                computeRoutesWithNextHopIpArpFalseFilter(
                    routesWithNextHopIpArpFalse,
                    route -> !ipSpaceToBDD.toBDD(route.getNextHopIp()).and(unownedIpsBDD).isZero());

        /* node -> vrf -> interface -> set of routes on that vrf that forward out that interface
         * with next hop ip owned by the snapshot devices and that gets no arp reply
         */
        Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithOwnedNextHopIpArpFalse =
            computeRoutesWithNextHopIpArpFalseFilter(
                routesWithNextHopIpArpFalse,
                route -> ipSpaceToBDD.toBDD(route.getNextHopIp()).and(unownedIpsBDD).isZero());

        /* node -> vrf -> interface -> dst ips for which that vrf forwards out that interface,
         * ARPing for a next-hop IP and receiving no reply
         */
        Map<String, Map<String, Map<String, IpSpace>>> arpFalseNextHopIp =
            computeArpFalseNextHopIp(matchingIps, routesWithNextHopIpArpFalse);

        /* node -> vrf -> interface -> set of routes on that vrf that forward out that interface,
         * ARPing for the destination IP
         */
        Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWhereDstIpCanBeArpIp =
            computeRoutesWhereDstIpCanBeArpIp(fibs, routesWithNextHop);

        arpFalseDestIp =
            computeArpFalseDestIp(matchingIps, routesWhereDstIpCanBeArpIp, someoneReplies);
        _arpFalse = union(arpFalseDestIp, arpFalseNextHopIp);

        /* node -> vrf -> edge -> routes in that vrf that forward out the source of that edge,
         * ARPing for the dest IP and receiving a response from the target of the edge.
         *
         * Note: the source interface of the edge must be in the node, but may not be in the vrf,
         * due to route leaking, etc
         */
        Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> routesWithDestIpEdge =
            computeRoutesWithDestIpEdge(topology, routesWhereDstIpCanBeArpIp);

        /* node -> vrf -> edge -> dst ips for which that vrf forwards out the source of the edge,
         * ARPing for the dest IP and receiving a reply from the target of the edge.
         *
         * Note: the source interface of the edge must be in the node, but may not be in the vrf,
         * due to route leaking, etc
         */
        Map<String, Map<String, Map<Edge, IpSpace>>> arpTrueEdgeDestIp =
            computeArpTrueEdgeDestIp(matchingIps, routesWithDestIpEdge, _arpReplies);

        /* node -> vrf -> edge -> dst ips for which that vrf forwards out the source of the edge,
         * ARPing for some next-hop IP and receiving a reply from the target of the edge.
         *
         * Note: the source interface of the edge must be in the node, but may not be in the vrf,
         * due to route leaking, etc
         */
        Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> routesWithNextHopIpArpTrue =
            computeRoutesWithNextHopIpArpTrue(fibs, topology, _arpReplies, routesWithNextHop);

        /* node -> vrf -> edge -> dst ips for which that vrf forwards out the source of the edge,
         * ARPing for some next-hop IP and receiving a reply from the target of the edge.
         *
         * Note: the source interface of the edge must be in the node, but may not be in the vrf,
         * due to route leaking, etc
         */
        Map<String, Map<String, Map<Edge, IpSpace>>> arpTrueEdgeNextHopIp =
            computeArpTrueEdgeNextHopIp(matchingIps, routesWithNextHopIpArpTrue);

        _arpTrueEdge = computeArpTrueEdge(arpTrueEdgeDestIp, arpTrueEdgeNextHopIp);

        dstIpsWithUnownedNextHopIpArpFalse =
            computeDstIpsWithUnownedNextHopIpArpFalse(
                matchingIps, routesWithUnownedNextHopIpArpFalse);
        dstIpsWithOwnedNextHopIpArpFalse =
            computeDstIpsWithOwnedNextHopIpArpFalse(matchingIps, routesWithOwnedNextHopIpArpFalse);
      }

      /* Compute dispositions top-down, based on what routes are available in each VRF's FIB */
      Map<String, Map<String, Set<String>>> fibInterfaces = computeFibInterfaces(fibs);

      // mapping: hostname -> vrf name -> interface -> ips belonging to a subnet of interface
      Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps =
          computeInterfaceHostSubnetIps(configurations);

      _deliveredToSubnet =
          computeDeliveredToSubnet(fibInterfaces, arpFalseDestIp, interfaceHostSubnetIps, ownedIps);

      // ips belonging to any subnet in the network
      IpSpace internalIps = computeInternalIps(interfaceHostSubnetIps);
      // ips not belonging to any subnet in the network
      IpSpace externalIps = internalIps.complement();

      Map<String, Map<String, BDD>> interfaceHostSubnetIpBDDs =
          computeInterfaceHostSubnetIpBDDs(interfaceHostSubnetIps, ipSpaceToBDD);
      // hostname -> interfaces that are not full. I.e. could have neighbors not present in snapshot
      Map<String, Set<String>> interfacesWithMissingDevices =
          computeInterfacesWithMissingDevices(interfaceHostSubnetIpBDDs, unownedIpsBDD);

      _exitsNetwork =
          computeExitsNetwork(
              fibInterfaces,
              interfacesWithMissingDevices,
              dstIpsWithUnownedNextHopIpArpFalse,
              arpFalseDestIp,
              externalIps);
      _insufficientInfo =
          computeInsufficientInfo(
              fibInterfaces,
              interfaceHostSubnetIps,
              interfacesWithMissingDevices,
              arpFalseDestIp,
              dstIpsWithUnownedNextHopIpArpFalse,
              dstIpsWithOwnedNextHopIpArpFalse,
              internalIps);
      _neighborUnreachable =
          computeNeighborUnreachable(
              fibInterfaces,
              _arpFalse,
              interfacesWithMissingDevices,
              arpFalseDestIp,
              interfaceHostSubnetIps,
              ownedIps);
    }
  }

  /**
   * Compute the set of interfaces each FIB can forward out of.<br>
   * This set by design excludes the special NULL interface. Mapping: node -&gt; vrf -&gt; set of
   * interface names.
   */
  @VisibleForTesting
  static Map<String, Map<String, Set<String>>> computeFibInterfaces(
      Map<String, Map<String, Fib>> fibs) {
    return toImmutableMap(
        fibs,
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey,
                vrfEntry ->
                    vrfEntry.getValue().allEntries().stream()
                        .map(FibEntry::getInterfaceName)
                        // Null-routing is a special case handled in compute nullRoutedIps
                        .filter(name -> !Interface.NULL_INTERFACE_NAME.equals(name))
                        .collect(ImmutableSet.toImmutableSet())));
  }

  /**
   * Compute an IP address ACL for each interface of each node permitting only those IPs for which
   * the node would send out an ARP reply on that interface: <br>
   * <br>
   * 1) PERMIT IPs belonging to the interface.<br>
   * 2) (Proxy-ARP) DENY any IP for which there is a longest-prefix match entry in the FIB that goes
   * through the interface.<br>
   * 3) (Proxy-ARP) PERMIT any other IP routable via the VRF of the interface.<br>
   * 4) (Proxy-ARP) PERMIT any statically configured arp IPs.
   */
  @VisibleForTesting
  static Map<String, Map<String, IpSpace>> computeArpReplies(
      Map<String, Configuration> configurations,
      Map<String, Map<String, IpSpace>> ipsRoutedOutInterfaces,
      Map<String, Map<String, Set<Ip>>> interfaceOwnedIps,
      Map<String, Map<String, IpSpace>> routableIps) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeArpReplies").startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          configurations,
          Entry::getKey,
          nodeEntry -> {
            String hostname = nodeEntry.getKey();
            return computeArpRepliesByInterface(
                nodeEntry.getValue().getAllInterfaces(),
                routableIps.get(hostname),
                ipsRoutedOutInterfaces.get(hostname),
                interfaceOwnedIps);
          });
    }
  }

  @VisibleForTesting
  static Map<String, IpSpace> computeArpRepliesByInterface(
      Map<String, Interface> interfaces,
      Map<String, IpSpace> routableIpsByVrf,
      Map<String, IpSpace> ipsRoutedOutInterfaces,
      Map<String, Map<String, Set<Ip>>> interfaceOwnedIps) {
    return toImmutableMap(
        interfaces,
        Entry::getKey,
        ifaceEntry ->
            computeInterfaceArpReplies(
                ifaceEntry.getValue(),
                routableIpsByVrf.get(ifaceEntry.getValue().getVrfName()),
                ipsRoutedOutInterfaces.get(ifaceEntry.getKey()),
                interfaceOwnedIps));
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<Edge, IpSpace>>> computeArpTrueEdge(
      Map<String, Map<String, Map<Edge, IpSpace>>> arpTrueEdgeDestIp,
      Map<String, Map<String, Map<Edge, IpSpace>>> arpTrueEdgeNextHopIp) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeArpTrueEdge").startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          arpTrueEdgeDestIp,
          Entry::getKey, // node
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey, // vrf
                  vrfEntry -> {
                    Map<Edge, IpSpace> dstIp = vrfEntry.getValue();
                    Map<Edge, IpSpace> nextHopIp =
                        arpTrueEdgeNextHopIp.get(nodeEntry.getKey()).get(vrfEntry.getKey());
                    return Sets.union(dstIp.keySet(), nextHopIp.keySet()).stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Function.identity(),
                                edge -> AclIpSpace.union(dstIp.get(edge), nextHopIp.get(edge))));
                  }));
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<Edge, IpSpace>>> computeArpTrueEdgeDestIp(
      Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps,
      Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> routesWithDestIpEdge,
      Map<String, Map<String, IpSpace>> arpReplies) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeArpTrueEdgeDestIp")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWithDestIpEdge,
          Entry::getKey, // node
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey, // vrf
                  vrfEntry ->
                      toImmutableMap(
                          vrfEntry.getValue(),
                          Entry::getKey, // edge
                          edgeEntry -> {
                            Edge edge = edgeEntry.getKey();
                            Set<AbstractRoute> routes = edgeEntry.getValue();
                            String hostname = edge.getNode1();
                            String vrf = vrfEntry.getKey();
                            IpSpace dstIpMatchesSomeRoutePrefix =
                                computeRouteMatchConditions(
                                    routes, matchingIps.get(hostname).get(vrf));
                            String recvNode = edge.getNode2();
                            String recvInterface = edge.getInt2();
                            IpSpace recvReplies = arpReplies.get(recvNode).get(recvInterface);
                            return AclIpSpace.rejecting(dstIpMatchesSomeRoutePrefix.complement())
                                .thenPermitting(recvReplies)
                                .build();
                          })));
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<Edge, IpSpace>>> computeArpTrueEdgeNextHopIp(
      Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps,
      Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> routesWithNextHopIpArpTrue) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeArpTrueEdgeNextHopIp")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWithNextHopIpArpTrue,
          Entry::getKey, // node
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey, // vrf
                  vrfEntry ->
                      vrfEntry.getValue().entrySet().stream()
                          .collect(
                              ImmutableMap.toImmutableMap(
                                  Entry::getKey, // edge
                                  edgeEntry -> {
                                    String hostname = nodeEntry.getKey();
                                    String vrf = vrfEntry.getKey();
                                    Set<AbstractRoute> routes = edgeEntry.getValue();
                                    return computeRouteMatchConditions(
                                        routes, matchingIps.get(hostname).get(vrf));
                                  }))));
    }
  }

  @VisibleForTesting
  static IpSpace computeInterfaceArpReplies(
      Interface iface,
      IpSpace routableIpsForThisVrf,
      IpSpace ipsRoutedThroughInterface,
      Map<String, Map<String, Set<Ip>>> interfaceOwnedIps) {
    IpSpace ipsAssignedToThisInterface =
        computeIpsAssignedToThisInterface(iface, interfaceOwnedIps);
    if (ipsAssignedToThisInterface == EmptyIpSpace.INSTANCE) {
      // if no IPs are assigned to this interface, it replies to no ARP requests.
      return EmptyIpSpace.INSTANCE;
    }
    /* Accept IPs assigned to this interface */
    AclIpSpace.Builder interfaceArpReplies = AclIpSpace.permitting(ipsAssignedToThisInterface);

    /* Accept IPs configured statically */
    interfaceArpReplies.thenPermitting(iface.getAdditionalArpIps());

    if (iface.getProxyArp()) {
      /* Reject IPs routed through this interface */
      interfaceArpReplies.thenRejecting(ipsRoutedThroughInterface);

      /* Accept all other routable IPs */
      interfaceArpReplies.thenPermitting(routableIpsForThisVrf);
    }

    return interfaceArpReplies.build();
  }

  @VisibleForTesting
  static IpSpace computeIpsAssignedToThisInterface(
      Interface iface, Map<String, Map<String, Set<Ip>>> interfaceOwnedIps) {
    if (iface.getAllAddresses().isEmpty()) {
      return EmptyIpSpace.INSTANCE;
    }

    Set<Ip> ips = interfaceOwnedIps.get(iface.getOwner().getHostname()).get(iface.getName());
    if (ips == null || ips.isEmpty()) {
      return EmptyIpSpace.INSTANCE;
    }
    IpWildcardSetIpSpace.Builder ipsAssignedToThisInterfaceBuilder = IpWildcardSetIpSpace.builder();
    ips.forEach(ip -> ipsAssignedToThisInterfaceBuilder.including(new IpWildcard(ip)));
    return ipsAssignedToThisInterfaceBuilder.build();
  }

  @VisibleForTesting
  static Map<String, Map<String, IpSpace>> computeIpsRoutedOutInterfaces(
      Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps,
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeIpsRoutedOutInterfaces")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWithNextHop,
          Entry::getKey /* hostname */,
          nodeEntry -> {
            String hostname = nodeEntry.getKey();
            return nodeEntry.getValue().entrySet().stream()
                .flatMap(
                    vrfEntry -> {
                      String vrf = vrfEntry.getKey();
                      Map<Prefix, IpSpace> vrfMatchingIps = matchingIps.get(hostname).get(vrf);
                      return vrfEntry.getValue().entrySet().stream()
                          /*
                           *  Cannot determine IPs for null interface here because it is
                           *  not tied to a single VRF.
                           */
                          .filter(
                              ifaceEntry ->
                                  !ifaceEntry.getKey().equals(Interface.NULL_INTERFACE_NAME))
                          .map(
                              ifaceEntry -> {
                                String iface = ifaceEntry.getKey();
                                Set<AbstractRoute> routes = ifaceEntry.getValue();
                                return Maps.immutableEntry(
                                    iface, computeRouteMatchConditions(routes, vrfMatchingIps));
                              });
                    })
                .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
          });
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<String, IpSpace>>> computeArpFalseDestIp(
      Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps,
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWhereDstIpCanBeArpIp,
      Map<String, Map<String, IpSpace>> someoneReplies) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeArpFalseDestIp")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWhereDstIpCanBeArpIp,
          Entry::getKey /* hostname */,
          nodeEntry -> {
            String hostname = nodeEntry.getKey();
            Map<String, IpSpace> someoneRepliesNode =
                someoneReplies.getOrDefault(hostname, ImmutableMap.of());
            return toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey /* vrf */,
                vrfEntry -> {
                  String vrf = vrfEntry.getKey();
                  Map<Prefix, IpSpace> vrfMatchingIps = matchingIps.get(hostname).get(vrf);
                  return vrfEntry.getValue().entrySet().stream()
                      /* null_interface is handled in computeNullRoutedIps */
                      .filter(entry -> !entry.getKey().equals(Interface.NULL_INTERFACE_NAME))
                      .collect(
                          ImmutableMap.toImmutableMap(
                              Entry::getKey /* outInterface */,
                              ifaceEntry -> {
                                String outInterface = ifaceEntry.getKey();
                                Set<AbstractRoute> routes = ifaceEntry.getValue();
                                IpSpace someoneRepliesIface =
                                    someoneRepliesNode.getOrDefault(
                                        outInterface, EmptyIpSpace.INSTANCE);
                                IpSpace ipsRoutedOutInterface =
                                    computeRouteMatchConditions(routes, vrfMatchingIps);
                                return AclIpSpace.rejecting(someoneRepliesIface)
                                    .thenPermitting(ipsRoutedOutInterface)
                                    .build();
                              }));
                });
          });
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<String, IpSpace>>> computeArpFalseNextHopIp(
      Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps,
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHopIpArpFalse) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeArpFalseNextHopIp")
            .startActive()) {
      assert span != null; // avoid unused warning
      return routesWithNextHopIpArpFalse.entrySet().stream()
          .collect(
              ImmutableMap.toImmutableMap(
                  Entry::getKey /* hostname */,
                  routesWithNextHopIpArpFalseByHostnameEntry -> {
                    String hostname = routesWithNextHopIpArpFalseByHostnameEntry.getKey();
                    return routesWithNextHopIpArpFalseByHostnameEntry.getValue().entrySet().stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey /* vrf */,
                                routesWithNextHopIpArpFalseByVrfEntry -> {
                                  String vrf = routesWithNextHopIpArpFalseByVrfEntry.getKey();
                                  return routesWithNextHopIpArpFalseByVrfEntry.getValue().entrySet()
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
                                                      matchingIps.get(hostname).get(vrf))));
                                }));
                  }));
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, IpSpace>> computeNullRoutedIps(
      Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps,
      Map<String, Map<String, Fib>> fibs) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeNullRoutedIps").startActive()) {
      assert span != null; // avoid unused warning
      return fibs.entrySet().stream()
          .collect(
              ImmutableMap.toImmutableMap(
                  Entry::getKey /* hostname */,
                  fibsByHostnameEntry -> {
                    String hostname = fibsByHostnameEntry.getKey();
                    return fibsByHostnameEntry.getValue().entrySet().stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey /* vrf */,
                                fibsByVrfEntry -> {
                                  String vrf = fibsByVrfEntry.getKey();
                                  Fib fib = fibsByVrfEntry.getValue();
                                  Map<Prefix, IpSpace> vrfMatchingIps =
                                      matchingIps.get(hostname).get(vrf);
                                  Set<AbstractRoute> nullRoutes =
                                      fib.getNextHopInterfaces().entrySet().stream()
                                          .filter(
                                              nextHopInterfacesByRouteEntry ->
                                                  nextHopInterfacesByRouteEntry
                                                      .getValue()
                                                      .keySet()
                                                      .contains(Interface.NULL_INTERFACE_NAME))
                                          .map(Entry::getKey)
                                          .collect(ImmutableSet.toImmutableSet());
                                  return computeRouteMatchConditions(nullRoutes, vrfMatchingIps);
                                }));
                  }));
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, IpSpace>> computeRoutableIps(Map<String, Map<String, Fib>> fibs) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeRoutableIps").startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          fibs,
          Entry::getKey, // node
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey, // vrf
                  vrfEntry ->
                      new IpWildcardSetIpSpace(
                          ImmutableSortedSet.of(),
                          vrfEntry.getValue().allEntries().stream()
                              .map(
                                  fibEntry ->
                                      new IpWildcard(fibEntry.getTopLevelRoute().getNetwork()))
                              .collect(ImmutableSortedSet.toImmutableSortedSet(natural())))));
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<Prefix, IpSpace>>> computeMatchingIps(
      Map<String, Map<String, Fib>> fibs) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeMatchingIps").startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          fibs,
          Entry::getKey, // node
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey, // vrf
                  vrfEntry -> vrfEntry.getValue().getMatchingIps()));
    }
  }

  @VisibleForTesting
  static IpSpace computeRouteMatchConditions(
      Set<AbstractRoute> routes, Map<Prefix, IpSpace> matchingIps) {
    // get the union of IpSpace that match one of the routes
    return firstNonNull(
        AclIpSpace.union(
            routes.stream()
                .map(AbstractRoute::getNetwork)
                .distinct()
                .map(matchingIps::get)
                .toArray(IpSpace[]::new)),
        EmptyIpSpace.INSTANCE);
  }

  /*
   * Mapping: hostname -&gt; vrfname -&gt; interfacename -&gt; a set of routes where each route
   * has at least one unset final next hop ip
   */
  @VisibleForTesting
  static Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
      computeRoutesWhereDstIpCanBeArpIp(
          Map<String, Map<String, Fib>> fibs,
          Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeRoutesWhereDstIpCanBeArpIp")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWithNextHop,
          Entry::getKey /* hostname */,
          nodeEntry -> {
            String hostname = nodeEntry.getKey();
            return toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey /* vrf */,
                vrfEntry -> {
                  String vrf = vrfEntry.getKey();
                  Fib fib = fibs.get(hostname).get(vrf);
                  return toImmutableMap(
                      vrfEntry.getValue(),
                      Entry::getKey /* interface */,
                      ifaceEntry -> {
                        String iface = ifaceEntry.getKey();
                        // return a set of routes where each route has
                        // some final next hop ip unset
                        return ifaceEntry.getValue() // routes with this interface as
                            // outgoing interfaces
                            .stream()
                            .filter(
                                route ->
                                    fib.getNextHopInterfaces()
                                        .get(route)
                                        .get(iface)
                                        .keySet() // final next hop ips
                                        .contains(Route.UNSET_ROUTE_NEXT_HOP_IP))
                            .collect(ImmutableSet.toImmutableSet());
                      });
                });
          });
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> computeRoutesWithDestIpEdge(
      Topology topology,
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWhereDstIpCanBeArpIp) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeRoutesWithDestIpEdge")
            .startActive()) {
      assert span != null; // avoid unused warning

      return toImmutableMap(
          routesWhereDstIpCanBeArpIp,
          Entry::getKey, // node
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey, // vrf
                  vrfEntry ->
                      vrfEntry.getValue().entrySet().stream()
                          .flatMap(
                              ifaceEntry -> {
                                NodeInterfacePair out =
                                    new NodeInterfacePair(nodeEntry.getKey(), ifaceEntry.getKey());
                                Set<AbstractRoute> routes = ifaceEntry.getValue();
                                return topology.getNeighbors(out).stream()
                                    .map(
                                        receiver ->
                                            Maps.immutableEntry(new Edge(out, receiver), routes));
                              })
                          .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue))));
    }
  }

  /* Mapping: hostname -&gt; vrfname -&gt; interfacename -&gt; set of associated routes (i.e.,
   * routes that use the interface as outgoing interface */
  @VisibleForTesting
  static Map<String, Map<String, Map<String, Set<AbstractRoute>>>> computeRoutesWithNextHop(
      Map<String, Configuration> configurations, Map<String, Map<String, Fib>> fibs) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeRoutesWithNextHop")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          configurations,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue().getVrfs(),
                  Entry::getKey,
                  vrfEntry -> {
                    Map<String, Set<AbstractRoute>> routesByNextHopInterface =
                        fibs.get(nodeEntry.getKey())
                            .get(vrfEntry.getKey())
                            .getRoutesByNextHopInterface();
                    return toImmutableMap(
                        vrfEntry.getValue().getInterfaces(),
                        Entry::getKey,
                        ifaceEntry ->
                            routesByNextHopInterface.getOrDefault(
                                ifaceEntry.getKey(), ImmutableSet.of()));
                  }));
    }
  }

  private static Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
      computeRoutesWithNextHopIpArpFalseFilter(
          Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHopIpArpFalse,
          Function<AbstractRoute, Boolean> routeFilter) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeRoutesWithNextHopIpArpFalseFilter")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWithNextHopIpArpFalse,
          Entry::getKey /* hostname */,
          routesWithNextHopByHostnameEntry ->
              toImmutableMap(
                  routesWithNextHopByHostnameEntry.getValue(),
                  Entry::getKey /* vrf */,
                  routesWithNextHopByVrfEntry ->
                      toImmutableMap(
                          routesWithNextHopByVrfEntry.getValue(),
                          Entry::getKey /* outInterface */,
                          routesWithNextHopByOutInterfaceEntry ->
                              routesWithNextHopByOutInterfaceEntry.getValue().stream()
                                  .filter(routeFilter::apply)
                                  .collect(Collectors.toSet()))));
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
      computeRoutesWithNextHopIpArpFalse(
          Map<String, Map<String, Fib>> fibs,
          Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop,
          Map<String, Map<String, IpSpace>> someoneReplies) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeRoutesWithNextHopIpArpFalse")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWithNextHop,
          Entry::getKey /* hostname */,
          nodeEntry -> {
            String hostname = nodeEntry.getKey();
            Map<String, IpSpace> nodeSomeoneReplies =
                someoneReplies.getOrDefault(hostname, ImmutableMap.of());
            return toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey, /* vrf */
                vrfEntry -> {
                  String vrf = vrfEntry.getKey();
                  Fib fib = fibs.get(hostname).get(vrf);
                  return toImmutableMap(
                      vrfEntry.getValue(),
                      Entry::getKey, /* outInterface */
                      ifaceEntry -> {
                        String outInterface = ifaceEntry.getKey();
                        Set<AbstractRoute> candidateRoutes = ifaceEntry.getValue();
                        return computeRoutesWithNextHopIpArpFalseForInterface(
                            fib, outInterface, candidateRoutes, nodeSomeoneReplies);
                      });
                });
          });
    }
  }

  @VisibleForTesting
  static Set<AbstractRoute> computeRoutesWithNextHopIpArpFalseForInterface(
      Fib fib,
      String outInterface,
      Set<AbstractRoute> candidateRoutes,
      Map<String, IpSpace> nodeSomeoneReplies) {
    IpSpace someoneReplies = nodeSomeoneReplies.getOrDefault(outInterface, EmptyIpSpace.INSTANCE);
    return candidateRoutes.stream()
        .filter(
            candidateRoute ->
                fib.getNextHopInterfaces().get(candidateRoute).get(outInterface).keySet().stream()
                    .filter(ip -> !ip.equals(Route.UNSET_ROUTE_NEXT_HOP_IP))
                    .anyMatch(
                        nextHopIp -> !someoneReplies.containsIp(nextHopIp, ImmutableMap.of())))
        .collect(ImmutableSet.toImmutableSet());
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> computeRoutesWithNextHopIpArpTrue(
      Map<String, Map<String, Fib>> fibs,
      Topology topology,
      Map<String, Map<String, IpSpace>> arpReplies,
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeRoutesWithNextHopIpArpTrue")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWithNextHop,
          Entry::getKey, // node
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey, // vrf
                  vrfEntry -> {
                    String hostname = nodeEntry.getKey();
                    String vrf = vrfEntry.getKey();
                    Fib fib = fibs.get(hostname).get(vrf);
                    return vrfEntry.getValue().entrySet().stream()
                        .flatMap(
                            ifaceEntry -> {
                              String outInterface = ifaceEntry.getKey();
                              Set<AbstractRoute> candidateRoutes = ifaceEntry.getValue();
                              NodeInterfacePair out = new NodeInterfacePair(hostname, outInterface);
                              Set<NodeInterfacePair> receivers = topology.getNeighbors(out);
                              return receivers.stream()
                                  .map(
                                      receiver -> {
                                        String recvNode = receiver.getHostname();
                                        String recvInterface = receiver.getInterface();
                                        IpSpace recvReplies =
                                            arpReplies.get(recvNode).get(recvInterface);
                                        Edge edge = new Edge(out, receiver);
                                        Set<AbstractRoute> routes =
                                            candidateRoutes.stream()
                                                .filter(
                                                    route ->
                                                        fib.getNextHopInterfaces().get(route)
                                                            .get(outInterface)
                                                            .keySet() // nextHopIps
                                                            .stream()
                                                            .filter(
                                                                ip ->
                                                                    !Route.UNSET_ROUTE_NEXT_HOP_IP
                                                                        .equals(ip))
                                                            .anyMatch(
                                                                nextHopIp ->
                                                                    recvReplies.containsIp(
                                                                        nextHopIp,
                                                                        ImmutableMap.of())))
                                                .collect(ImmutableSet.toImmutableSet());
                                        return routes.isEmpty()
                                            ? null
                                            : Maps.immutableEntry(edge, routes);
                                      })
                                  .filter(Objects::nonNull);
                            })
                        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
                  }));
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, IpSpace>> computeSomeoneReplies(
      Topology topology, Map<String, Map<String, IpSpace>> arpReplies) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeSomeoneReplies")
            .startActive()) {
      assert span != null; // avoid unused warning
      Map<String, Map<String, AclIpSpace.Builder>> someoneRepliesByNode = new HashMap<>();
      topology
          .getEdges()
          .forEach(
              edge ->
                  someoneRepliesByNode
                      .computeIfAbsent(edge.getNode1(), n -> new HashMap<>())
                      .computeIfAbsent(edge.getInt1(), i -> AclIpSpace.builder())
                      .thenPermitting((arpReplies.get(edge.getNode2()).get(edge.getInt2()))));
      return someoneRepliesByNode.entrySet().stream()
          .collect(
              ImmutableMap.toImmutableMap(
                  Entry::getKey /* hostname */,
                  someoneRepliesByNodeEntry ->
                      someoneRepliesByNodeEntry.getValue().entrySet().stream()
                          .collect(
                              ImmutableMap.toImmutableMap(
                                  Entry::getKey /* interface */,
                                  someoneRepliesByInterfaceEntry ->
                                      someoneRepliesByInterfaceEntry.getValue().build()))));
    }
  }

  @Override
  public Map<String, Map<String, IpSpace>> getArpReplies() {
    return _arpReplies;
  }

  @Override
  public Map<String, Map<String, Map<Edge, IpSpace>>> getArpTrueEdge() {
    return _arpTrueEdge;
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getNeighborUnreachableOrExitsNetwork() {
    /* The old NEIGHBOR_UNREACHABLE_OR_EXITS_NETWORK disposition is all dst IPs for which ARP
     * fails.
     */
    return _arpFalse;
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
  public Map<String, Map<String, Map<String, IpSpace>>> getDeliveredToSubnet() {
    return _deliveredToSubnet;
  }

  private static Stream<Entry<String, IpSpace>> getInterfaceIpSpaceEntries(
      Map<String, Map<String, IpSpace>> vrfInterfaceIpSpaceMap) {
    return vrfInterfaceIpSpaceMap.values().stream().flatMap(entry -> entry.entrySet().stream());
  }

  private static Map<String, Map<String, BDD>> computeInterfaceHostSubnetIpBDDs(
      Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps,
      IpSpaceToBDD ipSpaceToBDD) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeInterfaceHostSubnetIpBDDs")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          interfaceHostSubnetIps,
          Entry::getKey /* host name */,
          nodeEntry ->
              getInterfaceIpSpaceEntries(nodeEntry.getValue())
                  .collect(
                      ImmutableMap.toImmutableMap(
                          Entry::getKey, ifaceEntry -> ipSpaceToBDD.visit(ifaceEntry.getValue()))));
    }
  }

  static Map<String, Map<String, Map<String, IpSpace>>> union(
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaces1,
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaces2) {
    return merge(ipSpaces1, ipSpaces2, AclIpSpace::union);
  }

  private static Map<String, Map<String, Map<String, IpSpace>>> intersection(
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaces1,
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaces2) {
    return merge(ipSpaces1, ipSpaces2, AclIpSpace::intersection);
  }

  private static Map<String, Map<String, Map<String, IpSpace>>> merge(
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaces1,
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaces2,
      BiFunction<IpSpace, IpSpace, IpSpace> op) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.merge").startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          ipSpaces1,
          Entry::getKey, /* hostname */
          nodeEntry -> {
            Map<String, Map<String, IpSpace>> nodeIpSpace2 = ipSpaces2.get(nodeEntry.getKey());
            return toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey, /* vrf */
                vrfEntry -> {
                  Map<String, IpSpace> vrfIpSpaces2 = nodeIpSpace2.get(vrfEntry.getKey());
                  return toImmutableMap(
                      vrfEntry.getValue(),
                      Entry::getKey, /* interface */
                      ifaceEntry ->
                          op.apply(ifaceEntry.getValue(), vrfIpSpaces2.get(ifaceEntry.getKey())));
                });
          });
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<String, IpSpace>>> computeInterfaceHostSubnetIps(
      Map<String, Configuration> configs) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeInterfaceHostSubnetIps")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          configs,
          Entry::getKey, /* hostname */
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue().getVrfs(),
                  Entry::getKey, /* vrf */
                  vrfEntry ->
                      toImmutableMap(
                          vrfEntry.getValue().getInterfaces(),
                          Entry::getKey, /* interface */
                          ifaceEntry ->
                              firstNonNull(
                                  AclIpSpace.union(
                                      ifaceEntry.getValue().getAllAddresses().stream()
                                          .map(InterfaceAddress::getPrefix)
                                          .map(Prefix::toHostIpSpace)
                                          .collect(ImmutableList.toImmutableList())),
                                  EmptyIpSpace.INSTANCE))));
    }
  }

  private static IpSpace computeInternalIps(
      Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeInternalIps").startActive()) {
      assert span != null; // avoid unused warning
      return firstNonNull(
          AclIpSpace.union(
              interfaceHostSubnetIps.values().stream()
                  .flatMap(vrfSubnetIps -> vrfSubnetIps.values().stream())
                  .flatMap(ifaceSubnetIps -> ifaceSubnetIps.values().stream())
                  .collect(Collectors.toList())),
          EmptyIpSpace.INSTANCE);
    }
  }

  /*
   * Necessary and sufficient: Arping dst ip and the dst IP is not owned but is in an interface
   * subnet.
   */
  @VisibleForTesting
  static Map<String, Map<String, Map<String, IpSpace>>> computeDeliveredToSubnet(
      Map<String, Map<String, Set<String>>> fibInterfaces,
      Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp,
      Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps,
      IpSpace ownedIps) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeDeliveredToSubnet")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          fibInterfaces,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry ->
                      vrfEntry.getValue().stream()
                          .collect(
                              ImmutableMap.toImmutableMap(
                                  Function.identity(),
                                  iface ->
                                      AclIpSpace.difference(
                                          AclIpSpace.intersection(
                                              arpFalseDestIp
                                                  .get(nodeEntry.getKey())
                                                  .get(vrfEntry.getKey())
                                                  .get(iface),
                                              interfaceHostSubnetIps
                                                  .get(nodeEntry.getKey())
                                                  .get(vrfEntry.getKey())
                                                  .get(iface)),
                                          ownedIps)))));
    }
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getExitsNetwork() {
    return _exitsNetwork;
  }

  /**
   * Necessary and sufficient: The connected subnet is not full, the dest IP is external, and path
   * is not expected to come back into network (i.e. the ARP IP is also external).
   */
  static Map<String, Map<String, Map<String, IpSpace>>> computeExitsNetwork(
      Map<String, Map<String, Set<String>>> fibInterfaces,
      Map<String, Set<String>> interfacesWithMissingDevices,
      Map<String, Map<String, Map<String, IpSpace>>> dstIpsWithUnownedNextHopIpArpFalse,
      Map<String, Map<String, Map<String, IpSpace>>> arpFalseDstIp,
      IpSpace externalIps) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeExitsNetwork").startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          fibInterfaces,
          Entry::getKey,
          nodeEntry -> {
            String hostname = nodeEntry.getKey();
            Set<String> interfacesWithMissingDevicesNode =
                interfacesWithMissingDevices.get(hostname);
            return toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey,
                vrfEntry -> {
                  String vrfName = vrfEntry.getKey();
                  Map<String, IpSpace> arpFalseDstIpVrf = arpFalseDstIp.get(hostname).get(vrfName);
                  return toImmutableMap(
                      vrfEntry.getValue(),
                      Function.identity(),
                      ifaceName -> {
                        // the connected subnet is full
                        if (!interfacesWithMissingDevicesNode.contains(ifaceName)) {
                          return EmptyIpSpace.INSTANCE;
                        }

                        // Returns the union of the following 2 cases:
                        // 1. Arp for dst ip and dst ip is external
                        // 2. Arp for next hop ip, next hop ip is not owned by any
                        // interfaces,
                        // and dst ip is external
                        return AclIpSpace.intersection(
                            // dest ip is external
                            externalIps,
                            // arp for dst Ip OR arp for external next-hop IP
                            AclIpSpace.union(
                                arpFalseDstIpVrf.get(ifaceName),
                                dstIpsWithUnownedNextHopIpArpFalse
                                    .get(hostname)
                                    .get(vrfName)
                                    .get(ifaceName)));
                      });
                });
          });
    }
  }

  /**
   * is internal but not in the interface subnet, when arping for next hop ip, either next hop ip is
   * Necessary and sufficient: The connected subnet is not full, and when arping for dst ip, dst ip
   * owned by interfaces or dst ip is internal.
   *
   * @param fibInterfaces the set of interfaces each FIB can forward out to
   * @param interfaceHostSubnetIps Set of IPs belonging to the subnet of each interface.
   * @param interfacesWithMissingDevices Interfaces whose attached subnets are not full -- there may
   *     be other devices connected to the subnet for which we don't have a config.
   * @param arpFalseDestIp For each interface, dst IPs that can be ARP IPs and that we will not
   *     receive an ARP response for.
   * @param dstIpsWithUnownedNextHopIpArpFalse node -> vrf -> iface -> dst IPs the vrf forwards out
   *     the interface, ARPing for some unowned next-hop IP and not receiving a reply.
   * @param dstIpsWithOwnedNextHopIpArpFalse node -> vrf -> iface -> dst IPs the vrf forwards out
   *     the interface, ARPing for some owned next-hop IP and not receiving a reply.
   * @param internalIps IPs owned by devices in the snapshot or in connected subnets.
   */
  @VisibleForTesting
  static Map<String, Map<String, Map<String, IpSpace>>> computeInsufficientInfo(
      Map<String, Map<String, Set<String>>> fibInterfaces,
      Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps,
      Map<String, Set<String>> interfacesWithMissingDevices,
      Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp,
      Map<String, Map<String, Map<String, IpSpace>>> dstIpsWithUnownedNextHopIpArpFalse,
      Map<String, Map<String, Map<String, IpSpace>>> dstIpsWithOwnedNextHopIpArpFalse,
      IpSpace internalIps) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeInsufficientInfo")
            .startActive()) {
      assert span != null; // avoid unused warning

      return toImmutableMap(
          fibInterfaces,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry -> {
                    String hostname = nodeEntry.getKey();
                    String vrfName = vrfEntry.getKey();
                    return toImmutableMap(
                        vrfEntry.getValue(),
                        Function.identity(),
                        ifaceName -> {
                          // If interface is full (no missing devices), it cannot be
                          // insufficient info
                          if (!interfacesWithMissingDevices.get(hostname).contains(ifaceName)) {
                            return EmptyIpSpace.INSTANCE;
                          }

                          IpSpace ipSpaceElsewhere =
                              AclIpSpace.difference(
                                  internalIps,
                                  interfaceHostSubnetIps.get(hostname).get(vrfName).get(ifaceName));

                          // case 1: arp for dst ip, dst ip is internal but not in any
                          // subnet of the interface
                          IpSpace ipSpaceInternalDstIp =
                              AclIpSpace.intersection(
                                  arpFalseDestIp.get(hostname).get(vrfName).get(ifaceName),
                                  ipSpaceElsewhere);

                          // case 2: arp for nhip, nhip is not owned by interfaces, dst ip
                          // is internal
                          IpSpace dstIpsWithUnownedNextHopIpArpFalsePerInterafce =
                              dstIpsWithUnownedNextHopIpArpFalse
                                  .get(hostname)
                                  .get(vrfName)
                                  .get(ifaceName);

                          IpSpace ipSpaceInternalDstIpUnownedNexthopIp =
                              AclIpSpace.intersection(
                                  dstIpsWithUnownedNextHopIpArpFalsePerInterafce, internalIps);

                          // case 3: arp for nhip, nhip is owned by some interfaces
                          IpSpace ipSpaceOwnedNextHopIp =
                              dstIpsWithOwnedNextHopIpArpFalse
                                  .get(hostname)
                                  .get(vrfName)
                                  .get(ifaceName);

                          return AclIpSpace.union(
                              ipSpaceInternalDstIp,
                              ipSpaceInternalDstIpUnownedNexthopIp,
                              ipSpaceOwnedNextHopIp);
                        });
                  }));
    }
  }

  /**
   * Necessary and sufficient: No ARP response, and either: 1. the interface is full, or 2. we ARPed
   * for a dest IP that is in a conncected subnet and is owned in the snapshot.
   *
   * <p>An interface is full if all subnets connected to it are full.
   */
  static Map<String, Map<String, Map<String, IpSpace>>> computeNeighborUnreachable(
      Map<String, Map<String, Set<String>>> fibInterfaces,
      Map<String, Map<String, Map<String, IpSpace>>> arpFalse,
      Map<String, Set<String>> interfacesWithMissingDevices,
      Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp,
      Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps,
      IpSpace ownedIps) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeNeighborUnreachable")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          fibInterfaces,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry ->
                      toImmutableMap(
                          vrfEntry.getValue(),
                          Function.identity(),
                          iface -> {
                            String node = nodeEntry.getKey();
                            String vrf = vrfEntry.getKey();

                            IpSpace ifaceArpFalse = arpFalse.get(node).get(vrf).get(iface);

                            return interfacesWithMissingDevices.get(node).contains(iface)
                                ? AclIpSpace.intersection(
                                    arpFalseDestIp.get(node).get(vrf).get(iface),
                                    interfaceHostSubnetIps.get(node).get(vrf).get(iface),
                                    ownedIps)
                                : ifaceArpFalse;
                          })));
    }
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getNeighborUnreachable() {
    return _neighborUnreachable;
  }

  /** hostname -> interfaces that are not full. I.e. could have neighbors not present in snapshot */
  private static Map<String, Set<String>> computeInterfacesWithMissingDevices(
      Map<String, Map<String, BDD>> interfaceHostSubnetIpBDDs, BDD unownedIpsBDD) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeInterfacesWithMissingDevices")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          interfaceHostSubnetIpBDDs,
          Entry::getKey,
          nodeEntry ->
              nodeEntry.getValue().entrySet().stream()
                  .filter(ifaceEntry -> !ifaceEntry.getValue().and(unownedIpsBDD).isZero())
                  .map(Entry::getKey)
                  .collect(ImmutableSet.toImmutableSet()));
    }
  }

  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getInsufficientInfo() {
    return _insufficientInfo;
  }

  private static Map<String, Map<String, Map<String, IpSpace>>>
      computeDstIpsWithOwnedNextHopIpArpFalse(
          Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps,
          Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
              routesWithOwnedNextHopIpArpFalse) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeDstIpsWithOwnedNextHopIpArpFalse")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWithOwnedNextHopIpArpFalse,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry -> {
                    Map<Prefix, IpSpace> vrfMatchingIps =
                        matchingIps.get(nodeEntry.getKey()).get(vrfEntry.getKey());
                    return toImmutableMap(
                        vrfEntry.getValue(),
                        Entry::getKey,
                        ifaceEntry ->
                            computeRouteMatchConditions(ifaceEntry.getValue(), vrfMatchingIps));
                  }));
    }
  }

  private static Map<String, Map<String, Map<String, IpSpace>>>
      computeDstIpsWithUnownedNextHopIpArpFalse(
          Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps,
          Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
              routesWithUnownedNextHopIpArpFalse) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeDstIpsWithUnownedNextHopIpArpFalse")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          routesWithUnownedNextHopIpArpFalse,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry -> {
                    Map<Prefix, IpSpace> vrfMatchingIps =
                        matchingIps.get(nodeEntry.getKey()).get(vrfEntry.getKey());
                    return toImmutableMap(
                        vrfEntry.getValue(),
                        Entry::getKey,
                        ifaceEntry ->
                            computeRouteMatchConditions(ifaceEntry.getValue(), vrfMatchingIps));
                  }));
    }
  }

  private static IpSpace computeOwnedIps(Map<String, Map<String, Set<Ip>>> interfaceOwnedIps) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeOwnedIps").startActive()) {
      assert span != null; // avoid unused warning
      return IpWildcardSetIpSpace.builder()
          .including(
              interfaceOwnedIps.values().stream()
                  .flatMap(ifaceMap -> ifaceMap.values().stream())
                  .flatMap(Collection::stream)
                  .map(IpWildcard::new)
                  .collect(Collectors.toList()))
          .build();
    }
  }
}

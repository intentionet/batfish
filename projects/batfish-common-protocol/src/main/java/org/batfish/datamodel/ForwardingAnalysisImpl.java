package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Ordering.natural;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.batfish.common.util.CollectionUtil.toImmutableMap;

import com.google.common.annotations.VisibleForTesting;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.bdd.MemoizedIpSpaceToBDD;
import org.batfish.common.topology.IpOwners;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationInfo;

/** Implementation of {@link ForwardingAnalysis}. */
public final class ForwardingAnalysisImpl implements ForwardingAnalysis {

  /** node -&gt; vrf -&gt; interface -&gt; ips accepted by that interface */
  private Map<String, Map<String, Map<String, IpSpace>>> _acceptedIps;

  // node -> interface -> ips that the interface would reply arp request
  private final Map<String, Map<String, IpSpace>> _arpReplies;

  // node -> vrf -> edge -> dest IPs for which the vrf will forward out the source of the edge,
  // ARPing for some ARP IP and receiving a reply from the target of the edge.
  private final Map<String, Map<String, Map<Edge, IpSpace>>> _arpTrueEdge;

  // node -> vrf -> interface -> destination IPs for which arp will fail
  private final Map<String, Map<String, Map<String, IpSpace>>> _arpFalse;

  // node -> vrf -> nextVrf -> IPs that vrf delegates to nextVrf
  private final Map<String, Map<String, Map<String, IpSpace>>> _nextVrfIpsByNodeVrf;

  // node -> vrf -> destination IPs that will be null routes
  private final Map<String, Map<String, IpSpace>> _nullRoutedIps;

  // node -> vrf -> destination IPs that can be routed
  private final Map<String, Map<String, IpSpace>> _routableIps;

  // node -> vrf -> interface -> dst ips that end up with neighbor unreachable
  private final Map<String, Map<String, Map<String, IpSpace>>> _neighborUnreachable;

  // node -> vrf -> interface -> dst ips that end up delivered to subnet
  private final Map<String, Map<String, Map<String, IpSpace>>> _deliveredToSubnet;

  // node -> vrf -> interface -> dst ips that end up exiting the network
  private final Map<String, Map<String, Map<String, IpSpace>>> _exitsNetwork;

  // node -> vrf -> interface -> dst ips that end up with insufficient info
  private final Map<String, Map<String, Map<String, IpSpace>>> _insufficientInfo;

  public ForwardingAnalysisImpl(
      Map<String, Configuration> configurations,
      Map<String, Map<String, Fib>> fibs,
      Topology topology,
      Map<Location, LocationInfo> locationInfo) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("Construct ForwardingAnalysisImpl").startActive()) {
      assert span != null; // avoid unused warning

      // TODO accept IpSpaceToBDD as parameter
      IpSpaceToBDD ipSpaceToBDD =
          new MemoizedIpSpaceToBDD(new BDDPacket().getDstIp(), ImmutableMap.of());

      IpOwners ipOwners = new IpOwners(configurations);

      // IPs belonging to any interface in the network, even inactive interfaces
      // node -> interface -> IPs owned by that interface
      Map<String, Map<String, Set<Ip>>> interfaceOwnedIps =
          IpOwners.computeInterfaceOwnedIps(configurations, /*excludeInactive=*/ false);

      // Owned (i.e., internal to the network) IPs
      IpSpace ownedIps = computeOwnedIps(interfaceOwnedIps);
      // Unowned (i.e., external to the network) IPs
      BDD unownedIpsBDD = ipSpaceToBDD.visit(ownedIps).not();

      _acceptedIps = computeAcceptedIps(ipOwners);

      // IpSpaces matched by each prefix
      // -- only will have entries for active interfaces if FIB is correct
      Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps = computeMatchingIps(fibs);
      // Set of routes that forward out each interface
      Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop =
          computeRoutesWithNextHop(fibs);
      _nullRoutedIps = computeNullRoutedIps(matchingIps, fibs);
      _nextVrfIpsByNodeVrf = computeNextVrfIpsByNodeVrf(matchingIps, fibs);
      _routableIps = computeRoutableIps(fibs);

      /* Compute _arpReplies: for each interface, the set of arp IPs for which that interface will
       * respond.
       */
      {
        // mapping: node name -> vrf name -> interface name -> dst ips which are routed to the
        // interface. Should only include active interfaces.
        Map<String, Map<String, Map<String, IpSpace>>> ipsRoutedOutInterfaces =
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

        /*
         * Mapping: node -> vrf -> route -> nexthopinterface -> resolved nextHopIp ->
         * interfaceRoutes
         */
        Map<String, Map<String, Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>>>
            nextHopInterfacesByNodeVrf = computeNextHopInterfacesByNodeVrf(fibs);

        /* node -> vrf -> interface -> set of routes on that vrf that forward out that interface
         * with a next hop ip that gets no arp replies
         */
        Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHopIpArpFalse =
            computeRoutesWithNextHopIpArpFalse(
                nextHopInterfacesByNodeVrf, routesWithNextHop, someoneReplies);

        /* node -> vrf -> interface -> set of routes on that vrf that forward out that interface
         * with next hop ip not owned by the snapshot devices and that gets no arp reply
         */
        Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
            routesWithUnownedNextHopIpArpFalse =
                computeRoutesWithNextHopIpArpFalseFilter(
                    routesWithNextHopIpArpFalse,
                    route -> ipSpaceToBDD.toBDD(route.getNextHopIp()).andSat(unownedIpsBDD));

        /* node -> vrf -> interface -> set of routes on that vrf that forward out that interface
         * with next hop ip owned by the snapshot devices and that gets no arp reply
         */
        Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithOwnedNextHopIpArpFalse =
            computeRoutesWithNextHopIpArpFalseFilter(
                routesWithNextHopIpArpFalse,
                route -> !ipSpaceToBDD.toBDD(route.getNextHopIp()).andSat(unownedIpsBDD));

        /* node -> vrf -> interface -> dst ips for which that vrf forwards out that interface,
         * ARPing for a next-hop IP and receiving no reply
         */
        Map<String, Map<String, Map<String, IpSpace>>> arpFalseNextHopIp =
            computeArpFalseNextHopIp(matchingIps, routesWithNextHopIpArpFalse);

        /* node -> vrf -> interface -> set of routes on that vrf that forward out that interface,
         * ARPing for the destination IP
         */
        Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWhereDstIpCanBeArpIp =
            computeRoutesWhereDstIpCanBeArpIp(nextHopInterfacesByNodeVrf, routesWithNextHop);

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
            computeRoutesWithNextHopIpArpTrue(
                nextHopInterfacesByNodeVrf, topology, _arpReplies, routesWithNextHop);

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

      // mapping: hostname -> interface -> ips on which we should assume some external device (not
      // modeled in batfish) is listening, and would reply to ARP in the real world.
      Map<String, Map<String, IpSpace>> interfaceExternalArpIps =
          locationInfo.entrySet().stream()
              .filter(entry -> entry.getKey() instanceof InterfaceLinkLocation)
              .collect(
                  Collectors.groupingBy(
                      entry -> entry.getKey().getNodeName(),
                      Collectors.toMap(
                          entry -> ((InterfaceLinkLocation) entry.getKey()).getInterfaceName(),
                          entry -> entry.getValue().getArpIps())));

      _deliveredToSubnet =
          computeDeliveredToSubnet(arpFalseDestIp, interfaceExternalArpIps, ownedIps);

      Map<String, Map<String, BDD>> interfaceExternalArpIpBDDs =
          computeInterfaceExternalArpIpBDDs(interfaceExternalArpIps, ipSpaceToBDD);
      // hostname -> interfaces that are not full. I.e. could have neighbors not present in snapshot
      Map<String, Set<String>> interfacesWithMissingDevices =
          computeInterfacesWithMissingDevices(interfaceExternalArpIpBDDs, unownedIpsBDD);

      _neighborUnreachable =
          computeNeighborUnreachable(
              _arpFalse,
              interfacesWithMissingDevices,
              arpFalseDestIp,
              interfaceExternalArpIps,
              ownedIps);

      // ips belonging to any subnet in the network, including inactive interfaces.
      IpSpace internalIps = computeInternalIps(ipOwners.getAllInterfaceHostIps());

      _insufficientInfo =
          computeInsufficientInfo(
              interfaceExternalArpIps,
              interfacesWithMissingDevices,
              arpFalseDestIp,
              dstIpsWithUnownedNextHopIpArpFalse,
              dstIpsWithOwnedNextHopIpArpFalse,
              internalIps);

      // ips not belonging to any subnet in the network, including inactive interfaces.
      IpSpace externalIps = internalIps.complement();

      _exitsNetwork =
          computeExitsNetwork(
              interfacesWithMissingDevices,
              dstIpsWithUnownedNextHopIpArpFalse,
              arpFalseDestIp,
              externalIps);

      assert sanityCheck(ipSpaceToBDD, configurations);
    }
  }

  /**
   * Compute the space of IPs accepted by an interface<br>
   * Mapping: hostname -&gt; vrf name -&gt; interface name -&gt; space of IPs
   */
  // TODO: Account for special case VRF-accepted IPs that are not interface IPs.
  private static Map<String, Map<String, Map<String, IpSpace>>> computeAcceptedIps(
      IpOwners ipOwners) {
    return ipOwners.getVrfIfaceOwnedIpSpaces();
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
      Map<String, Map<String, Map<String, IpSpace>>> ipsRoutedOutInterfaces,
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
                nodeEntry.getValue().getActiveInterfaces(),
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
      Map<String, Map<String, IpSpace>> ipsRoutedOutInterfaces,
      Map<String, Map<String, Set<Ip>>> interfaceOwnedIps) {
    return toImmutableMap(
        interfaces,
        Entry::getKey,
        ifaceEntry ->
            computeInterfaceArpReplies(
                ifaceEntry.getValue(),
                /* We believe at this time that an interface would send an ARP reply only based
                 * on the routes in it's own VRF.
                 * This type of routing separation is the point of VRFs, and cross-VRF introspection
                 * for the purposes of ARP replies is unlikely to happen by default.
                 */
                routableIpsByVrf.get(ifaceEntry.getValue().getVrfName()),
                ipsRoutedOutInterfaces
                    .get(ifaceEntry.getValue().getVrfName())
                    .getOrDefault(ifaceEntry.getKey(), EmptyIpSpace.INSTANCE),
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
      @Nonnull Interface iface,
      @Nonnull IpSpace routableIpsForThisVrf,
      @Nonnull IpSpace ipsRoutedThroughInterface,
      @Nonnull Map<String, Map<String, Set<Ip>>> interfaceOwnedIps) {
    IpSpace ipsAssignedToThisInterface =
        computeIpsAssignedToThisInterfaceForArpReplies(iface, interfaceOwnedIps);
    if (ipsAssignedToThisInterface == EmptyIpSpace.INSTANCE) {
      // if no IPs are assigned to this interface at all (not even link-local), it replies to no ARP
      // requests.
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

  /**
   * Compute IP addresses "assigned" to this interface for the purposes for ARP replies. This is a
   * space of IPs that an interface will send an ARP reply for. Includes IPs that an interface owns
   * (explicitly assigned or virtual) as well as any defined link-local addresses.
   */
  @VisibleForTesting
  static IpSpace computeIpsAssignedToThisInterfaceForArpReplies(
      Interface iface, Map<String, Map<String, Set<Ip>>> interfaceOwnedIps) {
    /*
     * If a device has no interfaces with concrete IPs, it will not appear in interfaceOwnedIps.
     * When we get the owned IP space for such interfaces, there could be an NPE, work around that
     */
    Set<Ip> concreteIps =
        interfaceOwnedIps
            .getOrDefault(iface.getOwner().getHostname(), ImmutableMap.of())
            .getOrDefault(iface.getName(), ImmutableSet.of());
    Set<LinkLocalAddress> linkLocalIps = iface.getAllLinkLocalAddresses();
    if (concreteIps.isEmpty() && linkLocalIps.isEmpty()) {
      return EmptyIpSpace.INSTANCE;
    }
    IpWildcardSetIpSpace.Builder ipsAssignedToThisInterfaceBuilder = IpWildcardSetIpSpace.builder();
    concreteIps.forEach(ip -> ipsAssignedToThisInterfaceBuilder.including(IpWildcard.create(ip)));
    linkLocalIps.forEach(
        addr -> ipsAssignedToThisInterfaceBuilder.including(IpWildcard.create(addr.getIp())));
    return ipsAssignedToThisInterfaceBuilder.build();
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<String, IpSpace>>> computeIpsRoutedOutInterfaces(
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
            return toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey,
                vrfEntry -> {
                  String vrf = vrfEntry.getKey();
                  Map<Prefix, IpSpace> vrfMatchingIps = matchingIps.get(hostname).get(vrf);
                  return vrfEntry.getValue().entrySet().stream()
                      /*
                       *  Cannot determine IPs for null interface here because it is
                       *  not tied to a single VRF.
                       */
                      .filter(
                          ifaceEntry -> !ifaceEntry.getKey().equals(Interface.NULL_INTERFACE_NAME))
                      .map(
                          ifaceEntry -> {
                            String iface = ifaceEntry.getKey();
                            Set<AbstractRoute> routes = ifaceEntry.getValue();
                            return Maps.immutableEntry(
                                iface, computeRouteMatchConditions(routes, vrfMatchingIps));
                          })
                      .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
                });
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
                                      fib.allEntries().stream()
                                          .filter(
                                              fibEntry ->
                                                  fibEntry.getAction() instanceof FibNullRoute)
                                          .map(FibEntry::getTopLevelRoute)
                                          .collect(ImmutableSet.toImmutableSet());
                                  return computeRouteMatchConditions(nullRoutes, vrfMatchingIps);
                                }));
                  }));
    }
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<String, IpSpace>>> computeNextVrfIpsByNodeVrf(
      Map<String, Map<String, Map<Prefix, IpSpace>>> matchingIps,
      Map<String, Map<String, Fib>> fibs) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeNextVrfIps").startActive()) {
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
                                fibsByVrfEntry ->
                                    computeNextVrfIps(
                                        fibsByVrfEntry.getValue(), /* fib */
                                        matchingIps
                                            .get(hostname)
                                            .get(fibsByVrfEntry.getKey()) /* matchingIps */)));
                  }));
    }
  }

  private static Map<String, IpSpace> computeNextVrfIps(Fib fib, Map<Prefix, IpSpace> matchingIps) {
    return fib.allEntries().stream()
        .filter(fibEntry -> fibEntry.getAction() instanceof FibNextVrf)
        .collect(
            groupingBy(
                fibEntry -> ((FibNextVrf) fibEntry.getAction()).getNextVrf(),
                mapping(FibEntry::getTopLevelRoute, ImmutableSet.toImmutableSet())))
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey /* nextVrf */,
                routesByNextVrfEntry ->
                    computeRouteMatchConditions(
                        routesByNextVrfEntry.getValue() /* routes */, matchingIps)));
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
                                      IpWildcard.create(fibEntry.getTopLevelRoute().getNetwork()))
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

  /**
   * Mapping: hostname -&gt; vrfname -&gt; interfacename -&gt; a set of routes where each route has
   * at least one unset final next hop ip
   */
  @VisibleForTesting
  static Map<String, Map<String, Map<String, Set<AbstractRoute>>>>
      computeRoutesWhereDstIpCanBeArpIp(
          Map<String, Map<String, Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>>>
              nextHopInterfacesByNodeVrf,
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
                  Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> nextHopInterfaces =
                      nextHopInterfacesByNodeVrf.get(hostname).get(vrf);
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
                                    nextHopInterfaces
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
                                    NodeInterfacePair.of(nodeEntry.getKey(), ifaceEntry.getKey());
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
      Map<String, Map<String, Fib>> fibs) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeRoutesWithNextHop")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          fibs,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry ->
                      vrfEntry.getValue().allEntries().stream()
                          .filter(fibEntry -> fibEntry.getAction() instanceof FibForward)
                          .collect(
                              Collectors.groupingBy(
                                  fibEntry ->
                                      ((FibForward) fibEntry.getAction()).getInterfaceName(),
                                  Collectors.mapping(
                                      FibEntry::getTopLevelRoute, Collectors.toSet())))));
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
          Map<String, Map<String, Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>>>
              nextHopInterfacesByNodeVrf,
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
                  Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> nextHopInterfaces =
                      nextHopInterfacesByNodeVrf.get(hostname).get(vrf);
                  return toImmutableMap(
                      vrfEntry.getValue(),
                      Entry::getKey, /* outInterface */
                      ifaceEntry -> {
                        String outInterface = ifaceEntry.getKey();
                        Set<AbstractRoute> candidateRoutes = ifaceEntry.getValue();
                        return computeRoutesWithNextHopIpArpFalseForInterface(
                            nextHopInterfaces, outInterface, candidateRoutes, nodeSomeoneReplies);
                      });
                });
          });
    }
  }

  @VisibleForTesting
  static Set<AbstractRoute> computeRoutesWithNextHopIpArpFalseForInterface(
      Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> nextHopInterfaces,
      String outInterface,
      Set<AbstractRoute> candidateRoutes,
      Map<String, IpSpace> nodeSomeoneReplies) {
    IpSpace someoneReplies = nodeSomeoneReplies.getOrDefault(outInterface, EmptyIpSpace.INSTANCE);
    return candidateRoutes.stream()
        .filter(
            candidateRoute ->
                nextHopInterfaces.get(candidateRoute).get(outInterface).keySet().stream()
                    .filter(ip -> !ip.equals(Route.UNSET_ROUTE_NEXT_HOP_IP))
                    .anyMatch(
                        nextHopIp -> !someoneReplies.containsIp(nextHopIp, ImmutableMap.of())))
        .collect(ImmutableSet.toImmutableSet());
  }

  @VisibleForTesting
  static Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> computeRoutesWithNextHopIpArpTrue(
      Map<String, Map<String, Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>>>
          nextHopInterfacesByNodeVrf,
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
                    Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>> nextHopInterfaces =
                        nextHopInterfacesByNodeVrf.get(hostname).get(vrf);
                    return vrfEntry.getValue().entrySet().stream()
                        .flatMap(
                            ifaceEntry -> {
                              String outInterface = ifaceEntry.getKey();
                              Set<AbstractRoute> candidateRoutes = ifaceEntry.getValue();
                              NodeInterfacePair out = NodeInterfacePair.of(hostname, outInterface);
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
                                                        nextHopInterfaces.get(route)
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

  @Nonnull
  @Override
  public Map<String, Map<String, Map<String, IpSpace>>> getAcceptsIps() {
    return _acceptedIps;
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
  public Map<String, Map<String, Map<String, IpSpace>>> getNextVrfIps() {
    return _nextVrfIpsByNodeVrf;
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

  private static Map<String, Map<String, BDD>> computeInterfaceExternalArpIpBDDs(
      Map<String, Map<String, IpSpace>> interfaceExternalArpIps, IpSpaceToBDD ipSpaceToBDD) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeInterfaceHostSubnetIpBDDs")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          interfaceExternalArpIps,
          Entry::getKey /* host name */,
          nodeEntry ->
              nodeEntry.getValue().entrySet().stream()
                  .collect(
                      ImmutableMap.toImmutableMap(
                          Entry::getKey, ifaceEntry -> ipSpaceToBDD.visit(ifaceEntry.getValue()))));
    }
  }

  static Map<String, Map<String, Map<String, IpSpace>>> union(
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaces1,
      Map<String, Map<String, Map<String, IpSpace>>> ipSpaces2) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.union").startActive()) {
      assert span != null; // avoid unused warning

      checkArgument(
          ipSpaces1.keySet().equals(ipSpaces2.keySet()),
          "Can't union with different nodes: %s and %s",
          ipSpaces1.keySet(),
          ipSpaces2.keySet());

      return toImmutableMap(
          ipSpaces1,
          Entry::getKey, /* hostname */
          nodeEntry -> {
            Map<String, Map<String, IpSpace>> nodeIpSpace2 = ipSpaces2.get(nodeEntry.getKey());
            checkArgument(
                nodeIpSpace2.keySet().equals(nodeEntry.getValue().keySet()),
                "Can't union with different VRFs in node %s: %s and %s",
                nodeEntry.getKey(),
                nodeEntry.getValue().keySet(),
                nodeIpSpace2.keySet());
            return toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey, /* vrf */
                vrfEntry -> {
                  Map<String, IpSpace> vrfIpSpaces2 = nodeIpSpace2.get(vrfEntry.getKey());
                  checkArgument(
                      vrfIpSpaces2.keySet().equals(vrfEntry.getValue().keySet()),
                      "Can't union with different interfaces in node %s VRF %s: %s and %s",
                      nodeEntry.getKey(),
                      vrfEntry.getKey(),
                      vrfEntry.getValue().keySet(),
                      vrfIpSpaces2.keySet());
                  return toImmutableMap(
                      vrfEntry.getValue(),
                      Entry::getKey, /* interface */
                      ifaceEntry ->
                          AclIpSpace.union(
                              ifaceEntry.getValue(), vrfIpSpaces2.get(ifaceEntry.getKey())));
                });
          });
    }
  }

  private static IpSpace computeInternalIps(
      Map<String, Map<String, IpSpace>> interfaceHostSubnetIps) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeInternalIps").startActive()) {
      assert span != null; // avoid unused warning
      return firstNonNull(
          AclIpSpace.union(
              interfaceHostSubnetIps.values().stream()
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
      Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp,
      Map<String, Map<String, IpSpace>> interfaceExternalArpIps,
      IpSpace ownedIps) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeDeliveredToSubnet")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          arpFalseDestIp,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry ->
                      toImmutableMap(
                          vrfEntry.getValue(),
                          Entry::getKey,
                          ifaceEntry ->
                              AclIpSpace.difference(
                                  AclIpSpace.intersection(
                                      ifaceEntry.getValue(),
                                      interfaceExternalArpIps
                                          .get(nodeEntry.getKey())
                                          .get(ifaceEntry.getKey())),
                                  ownedIps))));
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
      Map<String, Set<String>> interfacesWithMissingDevices,
      Map<String, Map<String, Map<String, IpSpace>>> dstIpsWithUnownedNextHopIpArpFalse,
      Map<String, Map<String, Map<String, IpSpace>>> arpFalseDstIp,
      IpSpace externalIps) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("ForwardingAnalysisImpl.computeExitsNetwork").startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          dstIpsWithUnownedNextHopIpArpFalse,
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
                      Entry::getKey,
                      ifaceEntry -> {
                        String ifaceName = ifaceEntry.getKey();
                        // the connected subnet is full
                        if (!interfacesWithMissingDevicesNode.contains(ifaceName)) {
                          return EmptyIpSpace.INSTANCE;
                        }

                        // Returns the union of the following 2 cases:
                        // 1. Arp for dst ip and dst ip is external
                        // 2. Arp for next hop ip, next hop ip is not owned by any interfaces,
                        // and dst ip is external
                        return AclIpSpace.intersection(
                            // dest ip is external
                            externalIps,
                            // arp for dst Ip OR arp for external next-hop IP
                            AclIpSpace.union(
                                arpFalseDstIpVrf.get(ifaceName), ifaceEntry.getValue()));
                      });
                });
          });
    }
  }

  /**
   * Necessary and sufficient: The connected subnet is not full, and when arping for dst ip, dst ip
   * is internal but not in the interface subnet, when arping for next hop ip, either next hop ip is
   * owned by interfaces or dst ip is internal.
   *
   * @param interfaceExternalArpIps Set of IPs for which some external device (not modeled by
   *     Batfish) would reply to ARP in the real world.
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
      Map<String, Map<String, IpSpace>> interfaceExternalArpIps,
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
          arpFalseDestIp,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry ->
                      toImmutableMap(
                          vrfEntry.getValue(),
                          Entry::getKey,
                          ifaceEntry -> {
                            String hostname = nodeEntry.getKey();
                            String vrfName = vrfEntry.getKey();
                            String ifaceName = ifaceEntry.getKey();
                            // If interface is full (no missing devices), it cannot be insufficient
                            // info
                            if (!interfacesWithMissingDevices.get(hostname).contains(ifaceName)) {
                              return EmptyIpSpace.INSTANCE;
                            }

                            IpSpace ipSpaceElsewhere =
                                AclIpSpace.difference(
                                    internalIps,
                                    interfaceExternalArpIps.get(hostname).get(ifaceName));

                            // case 1: arp for dst ip, dst ip is internal but not in any subnet of
                            // the interface
                            IpSpace ipSpaceInternalDstIp =
                                AclIpSpace.intersection(
                                    arpFalseDestIp.get(hostname).get(vrfName).get(ifaceName),
                                    ipSpaceElsewhere);

                            // case 2: arp for nhip, nhip is not owned by interfaces, dst ip is
                            // internal
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
                          })));
    }
  }

  /**
   * Necessary and sufficient: No ARP response, and either: 1. the interface is full, or 2. we ARPed
   * for a dest IP that is in a connected subnet and is owned in the snapshot.
   *
   * <p>An interface is full if all subnets connected to it are full.
   */
  static Map<String, Map<String, Map<String, IpSpace>>> computeNeighborUnreachable(
      Map<String, Map<String, Map<String, IpSpace>>> arpFalse,
      Map<String, Set<String>> interfacesWithMissingDevices,
      Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp,
      Map<String, Map<String, IpSpace>> interfaceExternalArpIps,
      IpSpace ownedIps) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeNeighborUnreachable")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          arpFalse,
          Entry::getKey,
          nodeEntry ->
              toImmutableMap(
                  nodeEntry.getValue(),
                  Entry::getKey,
                  vrfEntry ->
                      toImmutableMap(
                          vrfEntry.getValue(),
                          Entry::getKey,
                          ifaceEntry -> {
                            String node = nodeEntry.getKey();
                            String vrf = vrfEntry.getKey();
                            String iface = ifaceEntry.getKey();

                            IpSpace ifaceArpFalse = ifaceEntry.getValue();

                            return interfacesWithMissingDevices.get(node).contains(iface)
                                ? AclIpSpace.intersection(
                                    arpFalseDestIp.get(node).get(vrf).get(iface),
                                    interfaceExternalArpIps.get(node).get(iface),
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
      Map<String, Map<String, BDD>> interfaceExternalArpIpBDDs, BDD unownedIpsBDD) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeInterfacesWithMissingDevices")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          interfaceExternalArpIpBDDs,
          Entry::getKey,
          nodeEntry ->
              nodeEntry.getValue().entrySet().stream()
                  .filter(ifaceEntry -> ifaceEntry.getValue().andSat(unownedIpsBDD))
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
                  .map(IpWildcard::create)
                  .collect(Collectors.toList()))
          .build();
    }
  }

  /**
   * Run sanity checks over the computed variables. Can be slow so only run in debug/assertion mode.
   */
  private boolean sanityCheck(
      IpSpaceToBDD ipSpaceToBDD, Map<String, Configuration> configurations) {
    // Sanity check internal properties.
    assertAllInterfacesActiveNodeInterface(_arpReplies, configurations);
    assertAllInterfacesActiveNodeVrfInterface(_arpFalse, configurations);
    assertAllInterfacesActiveNodeVrfInterface(_deliveredToSubnet, configurations);
    assertAllInterfacesActiveNodeVrfInterface(_exitsNetwork, configurations);
    assertAllInterfacesActiveNodeVrfInterface(_insufficientInfo, configurations);
    assertAllInterfacesActiveNodeVrfInterface(_neighborUnreachable, configurations);

    // Sanity check public APIs.
    assertAllInterfacesActiveNodeInterface(getArpReplies(), configurations);
    assertAllInterfacesActiveNodeVrfInterface(getDeliveredToSubnet(), configurations);
    assertAllInterfacesActiveNodeVrfInterface(getExitsNetwork(), configurations);
    assertAllInterfacesActiveNodeVrfInterface(getInsufficientInfo(), configurations);
    assertAllInterfacesActiveNodeVrfInterface(getNeighborUnreachable(), configurations);
    assertAllInterfacesActiveNodeVrfInterface(_arpFalse, configurations);

    // Sanity check traceroute-reachability different variables.
    Map<String, Map<String, Map<String, IpSpace>>> unionOthers =
        union(
            getNeighborUnreachable(),
            union(
                getInsufficientInfo(), //
                union(getDeliveredToSubnet(), getExitsNetwork())));
    assertDeepIpSpaceEquality(
        union(getNeighborUnreachable(), getInsufficientInfo()),
        union(getInsufficientInfo(), getNeighborUnreachable()),
        ipSpaceToBDD);

    Map<String, Map<String, Map<String, IpSpace>>> union1 =
        union(getNeighborUnreachable(), getInsufficientInfo());
    Map<String, Map<String, Map<String, IpSpace>>> union2 = union(union1, getDeliveredToSubnet());
    Map<String, Map<String, Map<String, IpSpace>>> union3 = union(union2, getExitsNetwork());
    assertDeepIpSpaceEquality(unionOthers, union3, ipSpaceToBDD);
    assertDeepIpSpaceEquality(_arpFalse, unionOthers, ipSpaceToBDD);

    return true;
  }

  /**
   * Asserts that all interfaces in the given nested map are inactive in the given configurations.
   */
  private static void assertAllInterfacesActiveNodeVrfInterface(
      Map<String, Map<String, Map<String, IpSpace>>> nodeVrfInterfaceMap,
      Map<String, Configuration> configurations) {
    nodeVrfInterfaceMap.forEach(
        (node, vrfInterfaceMap) ->
            vrfInterfaceMap.forEach(
                (vrf, ifaceMap) ->
                    ifaceMap
                        .keySet()
                        .forEach(
                            i -> {
                              if (i.equals(Interface.NULL_INTERFACE_NAME)) {
                                return;
                              }
                              Configuration c = configurations.get(node);
                              assert c != null : node + " is null";
                              Interface iface = c.getAllInterfaces().get(i);
                              assert iface != null : node + "[" + i + "] is null";
                              assert iface.getActive() : node + "[" + i + "] is not active";
                            })));
  }

  /**
   * Asserts that all interfaces in the given nested map are inactive in the given configurations.
   */
  private static void assertAllInterfacesActiveNodeInterface(
      Map<String, Map<String, IpSpace>> nodeInterfaceMap,
      Map<String, Configuration> configurations) {
    nodeInterfaceMap.forEach(
        (node, ifaceMap) ->
            ifaceMap
                .keySet()
                .forEach(
                    i -> {
                      if (i.equals(Interface.NULL_INTERFACE_NAME)) {
                        return;
                      }
                      Configuration c = configurations.get(node);
                      assert c != null : node + " is null";
                      Interface iface = c.getAllInterfaces().get(i);
                      assert iface != null : node + "[" + i + "] is null";
                      assert iface.getActive() : node + "[" + i + "] is not active";
                    }));
  }

  /**
   * Asserts that all interfaces in the given nested map are inactive in the given configurations.
   */
  private static void assertDeepIpSpaceEquality(
      Map<String, Map<String, Map<String, IpSpace>>> left,
      Map<String, Map<String, Map<String, IpSpace>>> right,
      IpSpaceToBDD toBDD) {
    assert left.keySet().equals(right.keySet())
        : "Different node sets " + left.keySet() + " " + right.keySet();
    left.forEach(
        (node, vrfIfaceMap) -> {
          Map<String, Map<String, IpSpace>> rightVrfIfaceMap = right.get(node);
          assert vrfIfaceMap.keySet().equals(rightVrfIfaceMap.keySet())
              : "Different VRFs for node " + node;
          vrfIfaceMap.forEach(
              (vrf, ifaceMap) -> {
                Map<String, IpSpace> rightIfaceMap = rightVrfIfaceMap.get(vrf);
                assert vrfIfaceMap.keySet().equals(rightVrfIfaceMap.keySet())
                    : "Different interfaces node " + node + " VRF " + vrf;
                ifaceMap.forEach(
                    (iface, ipSpace) -> {
                      IpSpace rightIpSpace = rightIfaceMap.get(iface);
                      BDD bdd = toBDD.visit(ipSpace);
                      BDD rightBDD = toBDD.visit(rightIpSpace);
                      assert !bdd.diffSat(rightBDD)
                          : "Left BDDs larger for node "
                              + node
                              + " VRF "
                              + vrf
                              + " interface "
                              + iface;
                      assert !rightBDD.diffSat(bdd)
                          : "Right BDDs larger for node "
                              + node
                              + " VRF "
                              + vrf
                              + " interface "
                              + iface;
                    });
              });
        });
  }

  /**
   * Mapping: node -&gt; vrf -&gt; route -&gt; nexthopinterface -&gt; resolved nextHopIp -&gt;
   * interfaceRoutes
   */
  private static Map<
          String, Map<String, Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>>>
      computeNextHopInterfacesByNodeVrf(Map<String, Map<String, Fib>> fibsByNode) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("ForwardingAnalysisImpl.computeNextHopInterfacesByNodeVrf")
            .startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          fibsByNode,
          Entry::getKey,
          fibsByNodeEntry ->
              toImmutableMap(
                  fibsByNodeEntry.getValue(),
                  Entry::getKey,
                  fibsByVrfEntry -> computeNextHopInterfaces(fibsByVrfEntry.getValue())));
    }
  }

  /** Mapping: route -&gt; nexthopinterface -&gt; resolved nextHopIp -&gt; interfaceRoutes */
  private static Map<AbstractRoute, Map<String, Map<Ip, Set<AbstractRoute>>>>
      computeNextHopInterfaces(Fib fib) {
    return fib.allEntries().stream()
        .filter(fibEntry -> fibEntry.getAction() instanceof FibForward)
        .collect(
            Collectors.groupingBy(
                FibEntry::getTopLevelRoute,
                Collectors.groupingBy(
                    fibEntry -> ((FibForward) fibEntry.getAction()).getInterfaceName(),
                    Collectors.groupingBy(
                        fibEntry -> ((FibForward) fibEntry.getAction()).getArpIp(),
                        Collectors.mapping(FibEntry::getResolvedToRoute, Collectors.toSet())))));
  }
}

package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.batfish.bddreachability.LastHopOutgoingInterfaceManager.NO_LAST_HOP;
import static org.batfish.bddreachability.transition.Transitions.compose;
import static org.batfish.bddreachability.transition.Transitions.constraint;
import static org.batfish.common.util.CollectionUtil.toImmutableMap;
import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.SOURCE_ORIGINATING_FROM_DEVICE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.common.bdd.BDDFiniteDomain;
import org.batfish.common.bdd.BDDOps;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.flow.Accept;
import org.batfish.datamodel.flow.FibLookup;
import org.batfish.datamodel.flow.ForwardOutInterface;
import org.batfish.datamodel.flow.OriginatingSessionScope;
import org.batfish.datamodel.flow.SessionAction;
import org.batfish.symbolic.state.StateExpr;
import org.batfish.symbolic.state.VrfAccept;

/**
 * Factory for creating {@link BDDFirewallSessionTraceInfo} objects for bidirectional reachability
 * analysis. Bidirectional reachability analysis consists of 3 steps:
 *
 * <p>First, we do a forward reachability query (from sources to destinations) of the first
 * direction. The result of that query is the set of packets that can reach the destination(s), as
 * well as the sets of packets that reached thee various points in the network that trigger session
 * creation.
 *
 * <p>Second, we construct the possibly-created sessions, and instrument the graph to account for
 * those sessions.
 *
 * <p>Third, we do a reachability query for the return direction (destinations back to the original
 * sources) with the sessions installed.
 */
@ParametersAreNonnullByDefault
final class BDDReachabilityAnalysisSessionFactory {
  private final BDDPacket _bddPacket;
  private final Map<String, Configuration> _configs;
  private final Map<String, BDDSourceManager> _srcManagers;
  private final LastHopOutgoingInterfaceManager _lastHopManager;
  private final Map<NodeInterfacePair, BDD> _incomingSessionBdds;
  private final Map<String, Map<String, BDD>> _originatingSessionBdds;
  private final BDDReverseFlowTransformationFactory _reverseFlowTransformationFactory;
  private final BDDReverseTransformationRanges _reverseTransformationRanges;

  private BDDReachabilityAnalysisSessionFactory(
      BDDPacket bddPacket,
      Map<String, Configuration> configs,
      Map<String, BDDSourceManager> srcManagers,
      LastHopOutgoingInterfaceManager lastHopManager,
      Map<StateExpr, BDD> forwardReachableBdds,
      BDDReverseFlowTransformationFactory transformationFactory,
      BDDReverseTransformationRanges reverseTransformationRanges) {
    _bddPacket = bddPacket;
    _configs = configs;
    _srcManagers = srcManagers;
    _lastHopManager = lastHopManager;
    BDD ipProtocolsWithSessionsBdd =
        IpProtocol.IP_PROTOCOLS_WITH_SESSIONS.stream()
            .map(bddPacket.getIpProtocol()::value)
            .reduce(bddPacket.getFactory().one(), BDD::or);
    _incomingSessionBdds =
        reachableIncomingSessionCreationBdds(
            configs, forwardReachableBdds, ipProtocolsWithSessionsBdd);
    _originatingSessionBdds =
        reachableOriginatingSessionCreationBdds(
            configs, forwardReachableBdds, ipProtocolsWithSessionsBdd);
    _reverseFlowTransformationFactory = transformationFactory;
    _reverseTransformationRanges = reverseTransformationRanges;
  }

  /**
   * Computes initialized firewall sessions for the return pass of bidirectional reachability query.
   *
   * @param forwardReachableStates The reachable packets to each state in the reachability analysis
   *     graph. Must be computed using forward propagation (from origination to termination points).
   * @return Mapping from node name to the established sessions on that node.
   */
  static Map<String, List<BDDFirewallSessionTraceInfo>> computeInitializedSesssions(
      BDDPacket bddPacket,
      Map<String, Configuration> configs,
      Map<String, BDDSourceManager> srcManagers,
      LastHopOutgoingInterfaceManager lastHopManager,
      Map<StateExpr, BDD> forwardReachableStates,
      BDDReverseFlowTransformationFactory transformationFactory,
      BDDReverseTransformationRanges reverseTransformationRanges) {
    return new BDDReachabilityAnalysisSessionFactory(
            bddPacket,
            configs,
            srcManagers,
            lastHopManager,
            forwardReachableStates,
            transformationFactory,
            reverseTransformationRanges)
        .computeInitializedSessions();
  }

  /** Compute initialized sessions for the entire network */
  private Map<String, List<BDDFirewallSessionTraceInfo>> computeInitializedSessions() {
    return _configs.values().stream()
        .flatMap(
            config -> {
              List<BDDFirewallSessionTraceInfo> sessions = computeInitializedSessions(config);
              return sessions.isEmpty()
                  ? Stream.of()
                  : Stream.of(Maps.immutableEntry(config.getHostname(), sessions));
            })
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  /** Compute initialized sessions for a config */
  private List<BDDFirewallSessionTraceInfo> computeInitializedSessions(Configuration config) {
    String hostname = config.getHostname();

    List<String> sessionOriginateVrfs =
        config.getVrfs().values().stream()
            .filter(Vrf::hasOriginatingSessions)
            .map(Vrf::getName)
            .collect(ImmutableList.toImmutableList());

    List<Interface> sessionExitInterfaces =
        config.getAllInterfaces().values().stream()
            .filter(iface -> iface.getActive() && iface.getFirewallSessionInterfaceInfo() != null)
            .collect(ImmutableList.toImmutableList());

    if (sessionOriginateVrfs.isEmpty() && sessionExitInterfaces.isEmpty()) {
      // No sessions to create (and won't have any entries in _lastHopManager), so return now
      return ImmutableList.of();
    }

    Map<String, Map<NodeInterfacePair, BDD>> lastHopOutgoingInterfaceBdds =
        toImmutableMap(
            config.activeInterfaceNames(),
            Function.identity(),
            iface ->
                Optional.ofNullable(
                        _lastHopManager
                            .getFiniteDomains()
                            .get(NodeInterfacePair.of(hostname, iface)))
                    .map(BDDFiniteDomain::getValueBdds)
                    .orElse(ImmutableMap.of()));

    Stream<BDDFirewallSessionTraceInfo> originatingSessions =
        sessionOriginateVrfs.stream()
            .flatMap(
                vrf -> {
                  BDD originateBdd =
                      _originatingSessionBdds.getOrDefault(hostname, ImmutableMap.of()).get(vrf);
                  return originateBdd == null || originateBdd.isZero()
                      ? Stream.of()
                      : computeInitializedOriginatingSessions(
                          hostname, vrf, lastHopOutgoingInterfaceBdds, originateBdd)
                          .stream();
                });
    Stream<BDDFirewallSessionTraceInfo> incomingSessions =
        sessionExitInterfaces.stream()
            .flatMap(
                iface -> {
                  BDD exitIfaceBdd =
                      _incomingSessionBdds.get(NodeInterfacePair.of(hostname, iface.getName()));
                  return exitIfaceBdd == null || exitIfaceBdd.isZero()
                      ? Stream.of()
                      : computeInitializedIncomingSessions(
                          iface, lastHopOutgoingInterfaceBdds, exitIfaceBdd)
                          .stream();
                });

    return Stream.concat(originatingSessions, incomingSessions)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Compute initialized sessions for flows exiting an outgoing interface that has a {@link
   * org.batfish.datamodel.FirewallSessionInterfaceInfo} object.
   *
   * @param outIface this node's egress iface; a session is set up when a flow leaves this iface
   * @param lastHopOutIfaceBdds this node's ingress iface -> (last hop, out iface) -> constraint
   * @param outIfaceBdd BDD representing the set of packets for which sessions can be initiated
   *     exiting outIface.
   */
  private List<BDDFirewallSessionTraceInfo> computeInitializedIncomingSessions(
      Interface outIface,
      Map<String, Map<NodeInterfacePair, BDD>> lastHopOutIfaceBdds,
      BDD outIfaceBdd) {
    String hostname = outIface.getOwner().getHostname();
    checkNotNull(outIface.getFirewallSessionInterfaceInfo());
    Set<String> sessionInterfaces =
        outIface.getFirewallSessionInterfaceInfo().getSessionInterfaces();

    BDDSourceManager srcMgr = _srcManagers.get(hostname);

    /* In order to setup sessions correctly, we have to track all possible sources in the firewall.
     * This means the source manager must distinguish between all sources, and the reachability
     * graph must impose the source constraints.
     */
    checkArgument(srcMgr.allSourcesTracked(), "Each possible source must be tracked.");
    checkArgument(
        srcMgr.hasSourceConstraint(outIfaceBdd), "Session devices must have source constraints");

    /* outIface's outgoing transformation, applied in the reverse direction to the
     * return flow.
     */
    Transition outgoingTransformation =
        _reverseFlowTransformationFactory.reverseFlowOutgoingTransformation(
            hostname, outIface.getName());

    boolean fibLookup = outIface.getFirewallSessionInterfaceInfo().getFibLookup();
    ImmutableList.Builder<BDDFirewallSessionTraceInfo> builder = ImmutableList.builder();
    srcMgr
        .getSourceBDDs()
        .forEach(
            (src, srcBdd) -> {
              // Flows entered src (either an interface or this device) and exited outIface
              BDD srcToOutIfaceBdd = outIfaceBdd.and(srcBdd);

              if (srcToOutIfaceBdd.isZero()) {
                return;
              }

              if (src.equals(SOURCE_ORIGINATING_FROM_DEVICE)) {
                assert !_lastHopManager.hasLastHopConstraint(srcToOutIfaceBdd);

                BDD outgoingTransformationRange =
                    _reverseTransformationRanges.reverseOutgoingTransformationRange(
                        hostname, outIface.getName(), null, null);

                // reverse direction, so out comes before in.
                Transition transformation =
                    compose(outgoingTransformation, constraint(outgoingTransformationRange));

                // Return flows for this session.
                BDD sessionBdd =
                    _bddPacket.swapSourceAndDestinationFields(
                        srcMgr.existsSource(srcToOutIfaceBdd));
                builder.add(
                    new BDDFirewallSessionTraceInfo(
                        hostname, sessionInterfaces, Accept.INSTANCE, sessionBdd, transformation));
              } else {
                Transition incomingTransformation =
                    _reverseFlowTransformationFactory.reverseFlowIncomingTransformation(
                        hostname, src);
                Map<NodeInterfacePair, BDD> nextHops = lastHopOutIfaceBdds.get(src);
                if (nextHops.isEmpty()) {
                  // The src interface has no neighbors
                  assert !_lastHopManager.hasLastHopConstraint(srcToOutIfaceBdd);

                  SessionAction action =
                      fibLookup ? FibLookup.INSTANCE : new ForwardOutInterface(src, null);

                  BDD incomingTransformationRange =
                      _reverseTransformationRanges.reverseIncomingTransformationRange(
                          hostname, src, src, null);

                  BDD outgoingTransformationRange =
                      _reverseTransformationRanges.reverseOutgoingTransformationRange(
                          hostname, outIface.getName(), src, null);

                  BDD sessionBdd =
                      _bddPacket.swapSourceAndDestinationFields(
                          srcMgr.existsSource(srcToOutIfaceBdd));
                  builder.add(
                      new BDDFirewallSessionTraceInfo(
                          hostname,
                          sessionInterfaces,
                          action,
                          sessionBdd,
                          compose(
                              outgoingTransformation,
                              constraint(outgoingTransformationRange),
                              incomingTransformation,
                              constraint(incomingTransformationRange))));
                  return;
                }

                nextHops.forEach(
                    (lastHopOutIface, lastHopOutIfaceBdd) -> {
                      // Flows that came from lastHopOutIface, entered src and exited outIface
                      BDD lastHopToSrcIfaceToOutIfaceBdd = srcToOutIfaceBdd.and(lastHopOutIfaceBdd);
                      if (lastHopToSrcIfaceToOutIfaceBdd.isZero()) {
                        return;
                      }

                      // Return flows for this session.
                      BDD sessionBdd =
                          _bddPacket.swapSourceAndDestinationFields(
                              srcMgr.existsSource(
                                  _lastHopManager.existsLastHop(lastHopToSrcIfaceToOutIfaceBdd)));

                      // Next hop for session flows
                      NodeInterfacePair lastHop =
                          lastHopOutIface == NO_LAST_HOP ? null : lastHopOutIface;
                      SessionAction action =
                          fibLookup ? FibLookup.INSTANCE : new ForwardOutInterface(src, lastHop);

                      BDD incomingTransformationRange =
                          _reverseTransformationRanges.reverseIncomingTransformationRange(
                              hostname, src, src, lastHop);

                      BDD outgoingTransformationRange =
                          _reverseTransformationRanges.reverseOutgoingTransformationRange(
                              hostname, outIface.getName(), src, lastHop);

                      builder.add(
                          new BDDFirewallSessionTraceInfo(
                              hostname,
                              sessionInterfaces,
                              action,
                              sessionBdd,
                              compose(
                                  outgoingTransformation,
                                  constraint(outgoingTransformationRange),
                                  incomingTransformation,
                                  constraint(incomingTransformationRange))));
                    });
              }
            });
    return builder.build();
  }

  /**
   * Compute initialized sessions for flows accepted into the VRF specified by {@code hostname} and
   * {@code vrf}.
   *
   * @param lastHopOutIfaceBdds this node's ingress iface -> (last hop, out iface) -> constraint
   * @param vrfAcceptBdd BDD representing the set of packets for which sessions can be initiated
   *     upon being accepted into the VRF.
   */
  private List<BDDFirewallSessionTraceInfo> computeInitializedOriginatingSessions(
      String hostname,
      String vrfName,
      Map<String, Map<NodeInterfacePair, BDD>> lastHopOutIfaceBdds,
      BDD vrfAcceptBdd) {
    BDDSourceManager srcMgr = _srcManagers.get(hostname);

    /* In order to setup sessions correctly, we have to track all possible sources in the firewall.
     * This means the source manager must distinguish between all sources, and the reachability
     * graph must impose the source constraints.
     */
    checkArgument(srcMgr.allSourcesTracked(), "Each possible source must be tracked.");
    checkArgument(
        srcMgr.hasSourceConstraint(vrfAcceptBdd), "Session devices must have source constraints");

    ImmutableList.Builder<BDDFirewallSessionTraceInfo> sessions = ImmutableList.builder();
    srcMgr
        .getSourceBDDs()
        .forEach(
            (src, srcBdd) -> {
              if (src.equals(SOURCE_ORIGINATING_FROM_DEVICE)) {
                // This assumes traffic to self can't create and match a session. See related
                // comment in FlowTracer#buildAcceptTrace.
                return;
              }

              // Flows that entered src (either an interface or this device) and were accepted
              BDD srcToAcceptBdd = vrfAcceptBdd.and(srcBdd);
              if (srcToAcceptBdd.isZero()) {
                return;
              }
              Transition incomingTransformation =
                  _reverseFlowTransformationFactory.reverseFlowIncomingTransformation(
                      hostname, src);
              Map<NodeInterfacePair, BDD> nextHops = lastHopOutIfaceBdds.get(src);
              if (nextHops.isEmpty()) {
                // The src interface has no neighbors
                assert !_lastHopManager.hasLastHopConstraint(srcToAcceptBdd);

                BDD incomingTransformationRange =
                    _reverseTransformationRanges.reverseIncomingTransformationRange(
                        hostname, src, src, null);

                BDD sessionBdd =
                    _bddPacket.swapSourceAndDestinationFields(srcMgr.existsSource(srcToAcceptBdd));
                sessions.add(
                    new BDDFirewallSessionTraceInfo(
                        hostname,
                        new OriginatingSessionScope(vrfName),
                        // Batfish does not support VRF-originating sessions with other actions
                        FibLookup.INSTANCE,
                        sessionBdd,
                        compose(incomingTransformation, constraint(incomingTransformationRange))));
                return;
              }

              nextHops.forEach(
                  (lastHopOutIface, lastHopOutIfaceBdd) -> {
                    // Flows that came from lastHopOutIface, entered src and were accepted
                    BDD lastHopToSrcIfaceToAcceptBdd = srcToAcceptBdd.and(lastHopOutIfaceBdd);
                    if (lastHopToSrcIfaceToAcceptBdd.isZero()) {
                      return;
                    }

                    // Return flows for this session.
                    BDD sessionBdd =
                        _bddPacket.swapSourceAndDestinationFields(
                            srcMgr.existsSource(
                                _lastHopManager.existsLastHop(lastHopToSrcIfaceToAcceptBdd)));

                    // Next hop for session flows
                    NodeInterfacePair lastHop =
                        lastHopOutIface == NO_LAST_HOP ? null : lastHopOutIface;

                    BDD incomingTransformationRange =
                        _reverseTransformationRanges.reverseIncomingTransformationRange(
                            hostname, src, src, lastHop);

                    sessions.add(
                        new BDDFirewallSessionTraceInfo(
                            hostname,
                            new OriginatingSessionScope(vrfName),
                            // Batfish does not support VRF-originating sessions with other actions
                            FibLookup.INSTANCE,
                            sessionBdd,
                            compose(
                                incomingTransformation, constraint(incomingTransformationRange))));
                  });
            });
    return sessions.build();
  }

  /**
   * Compute a mapping from (node,iface) to BDD representing the headers that can establish a
   * session exiting that interface.
   */
  private static Map<NodeInterfacePair, BDD> reachableIncomingSessionCreationBdds(
      Map<String, Configuration> configs,
      Map<StateExpr, BDD> reachable,
      BDD ipProtocolsWithSessionsBdd) {
    BDDOps ops = new BDDOps(ipProtocolsWithSessionsBdd.getFactory());
    return reachable.entrySet().stream()
        .map(
            entry ->
                immutableEntry(
                    entry.getKey().accept(SessionCreationNodeVisitor.INSTANCE), entry.getValue()))
        .filter(entry -> entry.getKey() != null)
        .filter(
            entry -> {
              NodeInterfacePair key = entry.getKey();
              return configs
                      .get(key.getHostname())
                      .getAllInterfaces()
                      .get(key.getInterface())
                      .getFirewallSessionInterfaceInfo()
                  != null;
            })
        .collect(
            groupingBy(
                Entry::getKey,
                mapping(
                    Entry::getValue,
                    collectingAndThen(
                        toList(), bdds -> ipProtocolsWithSessionsBdd.and(ops.orAll(bdds))))));
  }

  /**
   * Compute a mapping from hostname to VRF to BDD representing the flows that can establish a
   * session when they are accepted into that VRF.
   */
  private static Map<String, Map<String, BDD>> reachableOriginatingSessionCreationBdds(
      Map<String, Configuration> configs,
      Map<StateExpr, BDD> reachable,
      BDD ipProtocolsWithSessionsBdd) {
    Map<String, Map<String, List<BDD>>> reachableBdds = new HashMap<>();
    for (Entry<StateExpr, BDD> e : reachable.entrySet()) {
      StateExpr state = e.getKey();
      // Not necessary to check InterfaceAccept states because all InterfaceAccept states have an
      // unconstrained edge to a VrfAccept state.
      // TODO: Replace with a visitor.
      if (state instanceof VrfAccept) {
        String hostname = ((VrfAccept) state).getHostname();
        String vrf = ((VrfAccept) state).getVrf();
        Configuration c = configs.get(hostname);
        if (c.getVrfs().get(vrf).hasOriginatingSessions()) {
          reachableBdds
              .computeIfAbsent(hostname, k -> new HashMap<>())
              .computeIfAbsent(vrf, k -> new ArrayList<>())
              .add(e.getValue());
        }
      }
    }

    BDDOps ops = new BDDOps(ipProtocolsWithSessionsBdd.getFactory());
    return reachableBdds.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey, // hostname
                nodeEntry ->
                    nodeEntry.getValue().entrySet().stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey, // VRF name
                                vrfEntry ->
                                    // BDD of flows that can match originating session on VRF
                                    ipProtocolsWithSessionsBdd.and(
                                        ops.orAll(vrfEntry.getValue()))))));
  }
}

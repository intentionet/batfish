package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.stream.Collectors.toMap;
import static org.batfish.bddreachability.transition.Transitions.IDENTITY;
import static org.batfish.bddreachability.transition.Transitions.compose;
import static org.batfish.common.util.CommonUtil.toImmutableMap;
import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.SOURCE_ORIGINATING_FROM_DEVICE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.expr.StateExpr;

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
  private final Map<NodeInterfacePair, BDD> _sessionBdds;
  private final BDDReverseFlowTransformationFactory _reverseFlowTransformationFactory;

  private BDDReachabilityAnalysisSessionFactory(
      BDDPacket bddPacket,
      Map<String, Configuration> configs,
      Map<String, BDDSourceManager> srcManagers,
      LastHopOutgoingInterfaceManager lastHopManager,
      Map<StateExpr, BDD> forwardReachableBdds,
      BDDReverseFlowTransformationFactory transformationFactory) {
    _bddPacket = bddPacket;
    _configs = configs;
    _srcManagers = srcManagers;
    _lastHopManager = lastHopManager;
    _sessionBdds = reachableSessionCreationBdds(configs, forwardReachableBdds);
    _reverseFlowTransformationFactory = transformationFactory;
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
      BDDReverseFlowTransformationFactory transformationFactory) {
    return new BDDReachabilityAnalysisSessionFactory(
            bddPacket,
            configs,
            srcManagers,
            lastHopManager,
            forwardReachableStates,
            transformationFactory)
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

    List<Interface> sessionExitInterfaces =
        config.getAllInterfaces().values().stream()
            .filter(iface -> iface.getActive() && iface.getFirewallSessionInterfaceInfo() != null)
            .collect(ImmutableList.toImmutableList());

    if (sessionExitInterfaces.isEmpty()) {
      // No sessions to create (and won't have any entries in _lastHopManager), so return now
      return ImmutableList.of();
    }

    BDDSourceManager srcManager = _srcManagers.get(hostname);
    Map<String, BDD> sourceBdds = srcManager.getSourceBDDs();
    Map<String, Map<NodeInterfacePair, BDD>> lastHopOutgoingInterfaceBdds =
        toImmutableMap(
            config.activeInterfaces(),
            Function.identity(),
            iface ->
                Optional.ofNullable(
                        _lastHopManager
                            .getFiniteDomains()
                            .get(new NodeInterfacePair(hostname, iface)))
                    .map(BDDFiniteDomain::getValueBdds)
                    .orElse(ImmutableMap.of()));

    return sessionExitInterfaces.stream()
        .flatMap(
            iface -> {
              BDD exitIfaceBdd = _sessionBdds.get(new NodeInterfacePair(hostname, iface.getName()));
              return exitIfaceBdd == null || exitIfaceBdd.isZero()
                  ? Stream.of()
                  : computeInitializedSessions(iface, lastHopOutgoingInterfaceBdds, exitIfaceBdd)
                      .stream();
            })
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Compute initialized sessions for an outgoing interface (that has a {@link
   * org.batfish.datamodel.FirewallSessionInterfaceInfo} object).
   *
   * @param outIface this nodes' egress iface
   * @param lastHopOutIfaceBdds this node's ingress iface -> (last hop, out iface) -> constraint
   * @param outIfaceBdd BDD representing the set of packets for which sessions can be initiated
   *     exiting outIface.
   */
  private List<BDDFirewallSessionTraceInfo> computeInitializedSessions(
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

              /* src's incoming transformation, applied in the reverse direction to the return
               * flow.
               */
              Transition incomingTransformation =
                  src.equals(SOURCE_ORIGINATING_FROM_DEVICE)
                      ? IDENTITY
                      : _reverseFlowTransformationFactory.reverseFlowIncomingTransformation(
                          hostname, src);

              // reverse direction, so out comes before in.
              Transition transformation = compose(outgoingTransformation, incomingTransformation);

              if (src.equals(SOURCE_ORIGINATING_FROM_DEVICE)) {
                assert !_lastHopManager.hasLastHopConstraint(srcToOutIfaceBdd);

                // Return flows for this session.
                BDD sessionBdd =
                    _bddPacket.swapSourceAndDestinationFields(
                        srcMgr.existsSource(srcToOutIfaceBdd));
                builder.add(
                    new BDDFirewallSessionTraceInfo(
                        hostname, sessionInterfaces, null, null, sessionBdd, transformation));
              } else {
                Map<NodeInterfacePair, BDD> nextHops = lastHopOutIfaceBdds.get(src);
                if (nextHops.isEmpty()) {
                  // The src interface has no neighbors
                  assert !_lastHopManager.hasLastHopConstraint(srcToOutIfaceBdd);
                  BDD sessionBdd =
                      _bddPacket.swapSourceAndDestinationFields(
                          srcMgr.existsSource(srcToOutIfaceBdd));
                  builder.add(
                      new BDDFirewallSessionTraceInfo(
                          hostname, sessionInterfaces, null, src, sessionBdd, transformation));
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
                      NodeInterfacePair sessionNextHop =
                          lastHopOutIface == LastHopOutgoingInterfaceManager.NO_LAST_HOP
                              ? null
                              : lastHopOutIface;

                      builder.add(
                          new BDDFirewallSessionTraceInfo(
                              hostname,
                              sessionInterfaces,
                              sessionNextHop,
                              src,
                              sessionBdd,
                              transformation));
                    });
              }
            });
    return builder.build();
  }

  /**
   * Compute a mapping from (node,iface) to BDD representing the headers that can establish a
   * session exiting that interface.
   */
  private static Map<NodeInterfacePair, BDD> reachableSessionCreationBdds(
      Map<String, Configuration> configs, Map<StateExpr, BDD> reachable) {
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
        .collect(toMap(Entry::getKey, Entry::getValue, BDD::or));
  }
}

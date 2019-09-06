package org.batfish.bddreachability;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.batfish.common.bdd.BDDFiniteDomain.domainsWithSharedVariable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDFiniteDomain;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.collections.NodeInterfacePair;

/**
 * Tracks {@link BDD} constraints to identify the previous hop and its outgoing interface. This is
 * required for implementing sessions, since they don't forward using the FIB. Instead, return
 * traffic is forwarded to the node/interface the forward traffic was received from. In real
 * devices, the MAC address of the previous hop node/interface is stored in the session. We model
 * that behavior using the node/interface names.
 *
 * <p>We use a separate {@link BDDFiniteDomain} for each (receiving node, ingress interface), since
 * that makes the domains smaller (thus fewer BDD variables required; more efficient) and we already
 * track ingress interface.
 */
public final class LastHopOutgoingInterfaceManager {
  private static final String VAR_NAME = "LAST_HOP_OUTGOING_IFACE";
  private static final String NO_LAST_HOP_INTERFACE =
      "LastHopOutgoingInterfaceManager.NO_LAST_HOP_INTERFACE";
  private static final String NO_LAST_HOP_NODE = "LastHopOutgoingInterfaceManager.NO_LAST_HOP_NODE";

  // used for originating at an interface
  public static final NodeInterfacePair NO_LAST_HOP =
      NodeInterfacePair.of(NO_LAST_HOP_NODE, NO_LAST_HOP_INTERFACE);

  // (Receiving node, ingress interface) --> [(Sending node, egress interface)]
  private final Map<NodeInterfacePair, BDDFiniteDomain<NodeInterfacePair>> _finiteDomains;

  private final Set<String> _receivingNodes;

  private final BDD _trueBdd;

  public LastHopOutgoingInterfaceManager(
      BDDPacket pkt, Map<String, Configuration> configs, Set<Edge> topologyEdges) {
    this(pkt, computeFiniteDomains(pkt, configs, topologyEdges));
  }

  @VisibleForTesting
  LastHopOutgoingInterfaceManager(
      BDDPacket pkt, Map<NodeInterfacePair, BDDFiniteDomain<NodeInterfacePair>> finiteDomains) {
    _finiteDomains = ImmutableMap.copyOf(finiteDomains);
    _receivingNodes =
        _finiteDomains.keySet().stream()
            .map(NodeInterfacePair::getHostname)
            .collect(ImmutableSet.toImmutableSet());
    _trueBdd = pkt.getFactory().one();
  }

  private static Map<NodeInterfacePair, BDDFiniteDomain<NodeInterfacePair>> computeFiniteDomains(
      BDDPacket pkt, Map<String, Configuration> configs, Set<Edge> topologyEdges) {
    // Nodes that can have sessions (and thus need to track last hop outgoing interface).
    Set<String> sessionNodes =
        configs.values().stream()
            .filter(
                config ->
                    config.getAllInterfaces().values().stream()
                        .filter(Interface::getActive)
                        .anyMatch(iface -> iface.getFirewallSessionInterfaceInfo() != null))
            .map(Configuration::getHostname)
            .collect(ImmutableSet.toImmutableSet());

    Map<NodeInterfacePair, Set<NodeInterfacePair>> lastHops =
        topologyEdges.stream()
            .filter(edge -> sessionNodes.contains(edge.getNode2()))
            .collect(groupingBy(Edge::getHead, mapping(Edge::getTail, Collectors.toSet())));

    // add a dummy NodeInterfacePair for originating from that interface
    lastHops.values().forEach(set -> set.add(NO_LAST_HOP));

    return lastHops.isEmpty()
        ? ImmutableMap.of()
        : domainsWithSharedVariable(pkt, VAR_NAME, lastHops);
  }

  public BDD existsLastHop(BDD bdd) {
    // all domains have the same variable, so just pick one.
    Iterator<BDDFiniteDomain<NodeInterfacePair>> iterator = _finiteDomains.values().iterator();
    if (iterator.hasNext()) {
      return iterator.next().existsValue(bdd);
    }

    // No domains! Nothing to do.
    return bdd;
  }

  /** Return whether the input {@link BDD} has a constraint on the last hop variable. */
  public boolean hasLastHopConstraint(BDD bdd) {
    return !existsLastHop(bdd).equals(bdd);
  }

  public BDD getNoLastHopOutgoingInterfaceBdd(String node, String iface) {
    return getLastHopOutgoingInterfaceBdd(NO_LAST_HOP_NODE, NO_LAST_HOP_INTERFACE, node, iface);
  }

  public BDD getLastHopOutgoingInterfaceBdd(
      String sendingNode, String sendingIface, String receivingNode, String receivingIface) {
    NodeInterfacePair key = NodeInterfacePair.of(receivingNode, receivingIface);
    BDDFiniteDomain<NodeInterfacePair> fd = _finiteDomains.get(key);
    if (fd == null) {
      // receivingNode doesn't track last-hop outgoing interfaces.
      return _trueBdd;
    }
    NodeInterfacePair val = NodeInterfacePair.of(sendingNode, sendingIface);
    return fd.getConstraintForValue(val);
  }

  public Optional<NodeInterfacePair> getLastHopOutgoingInterfaceFromAssignment(
      String receivingNode, String receivingIface, BDD bdd) {
    NodeInterfacePair key = NodeInterfacePair.of(receivingNode, receivingIface);
    BDDFiniteDomain<NodeInterfacePair> fd = _finiteDomains.get(key);
    return fd == null ? Optional.empty() : Optional.of(fd.getValueFromAssignment(bdd));
  }

  @VisibleForTesting
  Map<NodeInterfacePair, BDDFiniteDomain<NodeInterfacePair>> getFiniteDomains() {
    return _finiteDomains;
  }

  public boolean isTrackedReceivingNode(String node) {
    return _receivingNodes.contains(node);
  }
}

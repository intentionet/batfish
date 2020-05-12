package org.batfish.bddreachability;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.flow.IncomingSessionScope;
import org.batfish.datamodel.flow.OriginatingSessionScope;
import org.batfish.datamodel.flow.SessionScope;
import org.batfish.datamodel.flow.SessionScopeVisitor;
import org.batfish.symbolic.state.OriginateInterface;
import org.batfish.symbolic.state.OriginateVrf;
import org.batfish.symbolic.state.PostInVrfSession;
import org.batfish.symbolic.state.PreInInterface;
import org.batfish.symbolic.state.StateExpr;

/**
 * Visitor for a {@link SessionScope} that returns the {@link Edge edges} that flows will traverse
 * when they match the session. These edges all terminate in {@link PostInVrfSession}, and may start
 * with {@link PreInInterface} (for {@link IncomingSessionScope}) or {@link OriginateInterface} or
 * {@link OriginateVrf} (for {@link OriginatingSessionScope}).
 */
@ParametersAreNonnullByDefault
public class EdgesMatchingSessionVisitor implements SessionScopeVisitor<Stream<Edge>> {
  private final String _hostname;
  private final Map<String, Interface> _ifaces;
  private final BDD _sessionFlows;

  EdgesMatchingSessionVisitor(String hostname, Map<String, Interface> ifaces, BDD sessionFlows) {
    _hostname = hostname;
    _ifaces = ImmutableMap.copyOf(ifaces);
    _sessionFlows = sessionFlows;
  }

  @Override
  public Stream<Edge> visitIncomingSessionScope(IncomingSessionScope incomingSessionScope) {
    return incomingSessionScope.getIncomingInterfaces().stream()
        .map(
            incomingInterface ->
                new Edge(
                    new PreInInterface(_hostname, incomingInterface),
                    new PostInVrfSession(
                        _hostname, _ifaces.get(incomingInterface).getVrf().getName()),
                    _sessionFlows));
  }

  @Override
  public Stream<Edge> visitOriginatingSessionScope(
      OriginatingSessionScope originatingSessionScope) {
    // Create edges for originating flows to match this session
    String vrf = originatingSessionScope.getOriginatingVrf();
    StateExpr postState = new PostInVrfSession(_hostname, vrf);

    // Prestates (OriginateVrf and an OriginateInterface for each interface in the VRF)
    StateExpr originateVrfState = new OriginateVrf(_hostname, vrf);
    Stream<StateExpr> originateIfaceStates =
        _ifaces.values().stream()
            .filter(iface -> iface.getVrfName().equals(vrf))
            .map(iface -> new OriginateInterface(_hostname, iface.getName()));

    // Convert prestates to edges
    return Stream.concat(originateIfaceStates, Stream.of(originateVrfState))
        .map(preState -> new Edge(preState, postState, _sessionFlows));
  }
}

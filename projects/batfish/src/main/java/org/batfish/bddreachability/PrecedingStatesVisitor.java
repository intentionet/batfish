package org.batfish.bddreachability;

import java.util.Collection;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.flow.IncomingSessionScope;
import org.batfish.datamodel.flow.OriginatingSessionScope;
import org.batfish.datamodel.flow.SessionScope;
import org.batfish.datamodel.flow.SessionScopeVisitor;
import org.batfish.symbolic.state.OriginateInterface;
import org.batfish.symbolic.state.OriginateVrf;
import org.batfish.symbolic.state.PreInInterface;
import org.batfish.symbolic.state.StateExpr;

/**
 * Visitor for a {@link SessionScope} that returns the {@link StateExpr states} from which the
 * corresponding session can be matched. These states include {@link PreInInterface} (for {@link
 * IncomingSessionScope}) and {@link OriginateInterface} and {@link OriginateVrf} (for {@link
 * OriginatingSessionScope}).
 */
@ParametersAreNonnullByDefault
public class PrecedingStatesVisitor implements SessionScopeVisitor<Stream<StateExpr>> {
  private final String _hostname;
  private final Collection<Interface> _ifaces;

  PrecedingStatesVisitor(String hostname, Collection<Interface> ifaces) {
    _hostname = hostname;
    _ifaces = ifaces;
  }

  @Override
  public Stream<StateExpr> visitIncomingSessionScope(IncomingSessionScope incomingSessionScope) {
    return incomingSessionScope.getIncomingInterfaces().stream()
        .map(incomingIface -> new PreInInterface(_hostname, incomingIface));
  }

  @Override
  public Stream<StateExpr> visitOriginatingSessionScope(
      OriginatingSessionScope originatingSessionScope) {
    String vrf = originatingSessionScope.getOriginatingVrf();

    // Prestates (OriginateVrf and an OriginateInterface for each interface in the VRF)
    StateExpr originateVrfState = new OriginateVrf(_hostname, vrf);
    Stream<StateExpr> originateIfaceStates =
        _ifaces.stream()
            .filter(iface -> iface.getVrfName().equals(vrf))
            .map(iface -> new OriginateInterface(_hostname, iface.getName()));
    return Stream.concat(originateIfaceStates, Stream.of(originateVrfState));
  }
}

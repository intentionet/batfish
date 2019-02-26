package org.batfish.bddreachability;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.transition.Transition;
import org.batfish.datamodel.collections.NodeInterfacePair;

/** BDD version of {@link org.batfish.datamodel.flow.FirewallSessionTraceInfo}. */
@ParametersAreNonnullByDefault
final class BDDFirewallSessionTraceInfo {
  private final @Nonnull String _hostname;
  private final @Nonnull Set<String> _incomingInterfaces;
  private final @Nullable NodeInterfacePair _nextHop;
  private final @Nullable String _outgoingInterface;
  private final @Nonnull BDD _sessionFlows;
  private final @Nonnull Transition _transformation;

  BDDFirewallSessionTraceInfo(
      String hostname,
      Set<String> incomingInterfaces,
      @Nullable NodeInterfacePair nextHop,
      @Nullable String outgoingInterface,
      BDD sessionFlows,
      Transition transformation) {
    checkArgument(
        (outgoingInterface != null) || (nextHop == null),
        "If outgoingInterface is null, nextHop must be null");
    _hostname = hostname;
    _incomingInterfaces = incomingInterfaces;
    _nextHop = nextHop;
    _outgoingInterface = outgoingInterface;
    _sessionFlows = sessionFlows;
    _transformation = transformation;
  }

  @Nonnull
  public String getHostname() {
    return _hostname;
  }

  @Nonnull
  public Set<String> getIncomingInterfaces() {
    return _incomingInterfaces;
  }

  @Nullable
  public NodeInterfacePair getNextHop() {
    return _nextHop;
  }

  @Nullable
  public String getOutgoingInterface() {
    return _outgoingInterface;
  }

  @Nonnull
  public BDD getSessionFlows() {
    return _sessionFlows;
  }

  @Nonnull
  public Transition getTransformation() {
    return _transformation;
  }
}

package org.batfish.datamodel.flow;

import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.transformation.Transformation;

/** Data about a firewall session created by a specific {@link Trace trace}. */
@ParametersAreNonnullByDefault
public final class FirewallSessionTraceInfo {
  private final @Nonnull String _hostname;
  private final @Nonnull Set<String> _incomingInterfaces;
  private final @Nonnull SessionAction _action;
  private final @Nonnull SessionMatchExpr _sessionFlows;
  private final @Nullable Transformation _transformation;

  public FirewallSessionTraceInfo(
      String hostname,
      SessionAction action,
      Set<String> incomingInterfaces,
      SessionMatchExpr sessionFlows,
      @Nullable Transformation transformation) {
    _hostname = hostname;
    _action = action;
    _incomingInterfaces = incomingInterfaces;
    _sessionFlows = sessionFlows;
    _transformation = transformation;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FirewallSessionTraceInfo)) {
      return false;
    }
    FirewallSessionTraceInfo that = (FirewallSessionTraceInfo) o;
    return _hostname.equals(that._hostname)
        && _incomingInterfaces.equals(that._incomingInterfaces)
        && _action.equals(that._action)
        && _sessionFlows.equals(that._sessionFlows)
        && Objects.equals(_transformation, that._transformation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostname, _incomingInterfaces, _action, _sessionFlows, _transformation);
  }

  /** The action to take on return traffic. */
  public @Nonnull SessionAction getAction() {
    return _action;
  }

  /** The hostname where the session exists. */
  public @Nonnull String getHostname() {
    return _hostname;
  }

  /** The interfaces through which the flow is allowed to enter. */
  public @Nonnull Set<String> getIncomingInterfaces() {
    return _incomingInterfaces;
  }

  /**
   * The allowed ingress flows for the session. Not allowed to contain {@link
   * org.batfish.datamodel.acl.PermittedByAcl} or {@link org.batfish.datamodel.IpSpaceReference}.
   */
  public @Nonnull AclLineMatchExpr getSessionFlows() {
    return _sessionFlows.toAclLineMatchExpr();
  }

  /** The match criteria of the ingress flow for the session. */
  public @Nonnull SessionMatchExpr getMatchCriteria() {
    return _sessionFlows;
  }

  /** The (optional) transformation for session traffic. */
  @Nullable
  public Transformation getTransformation() {
    return _transformation;
  }
}

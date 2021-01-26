package org.batfish.representation.cisco;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.Transformation;

/**
 * Abstract class which represents any Cisco IOS NAT. NATs are {@link Comparable} to represent the
 * order in which they should be evaluated when converted to {@link Transformation}s.
 */
@ParametersAreNonnullByDefault
public abstract class CiscoIosNat implements Comparable<CiscoIosNat>, Serializable {

  private RuleAction _action;
  private boolean _addRoute;
  @Nullable private String _routeMap;
  @Nullable private String _vrf;

  /**
   * All IOS NATs have a particular action which defines where and when to modify source and
   * destination
   */
  public final RuleAction getAction() {
    return _action;
  }

  public final void setAction(RuleAction action) {
    _action = action;
  }

  /**
   * The add-route option installs a static route to the local IP via the global IP (for {@link
   * RuleAction#SOURCE_OUTSIDE} only). <b>Only works on default VRF, otherwise no effect.</b>
   */
  public final boolean getAddRoute() {
    return _addRoute;
  }

  public final void setAddRoute(boolean addRoute) {
    _addRoute = addRoute;
  }

  /** Route-map specifying matching traffic (mutually exclusive with ACL) */
  @Nullable
  public String getRouteMap() {
    return _routeMap;
  }

  public void setRouteMap(@Nullable String routeMap) {
    _routeMap = routeMap;
  }

  /** Which VRF this NAT is in */
  @Nullable
  public final String getVrf() {
    return _vrf;
  }

  public final void setVrf(@Nullable String vrf) {
    _vrf = vrf;
  }

  /**
   * Converts a single NAT from the configuration into a {@link Transformation}.
   *
   * @param ifaceName Name of the (outside) interface for which we're creating the transformation
   * @param routeMaps Route-maps which may be referenced by static or dynamic NATs
   * @param natPools NAT pools from the configuration
   * @param insideInterfaces Names of interfaces which are defined as 'inside'
   * @param c Configuration
   * @return A single {@link Transformation} for inside-to-outside, or nothing if the {@link
   *     Transformation} could not be built
   */
  public abstract Optional<Transformation.Builder> toOutgoingTransformation(
      String ifaceName,
      Map<String, RouteMap> routeMaps,
      Map<String, NatPool> natPools,
      Set<String> insideInterfaces,
      Map<String, Interface> interfaces,
      Configuration c,
      Warnings w);

  /**
   * Converts a single NAT from the configuration into a {@link Transformation}.
   *
   * @param ifaceName Name of the (outside) interface for which we're creating the transformation
   * @param ipAccessLists Named access lists which may be referenced by dynamic NATs
   * @param routeMaps Route-maps which may be referenced by static or dynamic NATs
   * @param natPools NAT pools from the configuration
   * @return A single {@link Transformation} for inside-to-outside, or nothing if the {@link
   *     Transformation} could not be built
   */
  public abstract Optional<Transformation.Builder> toIncomingTransformation(
      String ifaceName,
      Map<String, IpAccessList> ipAccessLists,
      Map<String, RouteMap> routeMaps,
      Map<String, NatPool> natPools,
      Map<String, Interface> interfaces,
      Warnings w);

  /**
   * Creates the {@link StaticRoute} that will be added due to this NAT, if any (only possible if
   * {@link #getAddRoute() add-route} is set).
   */
  public abstract Optional<StaticRoute> toRoute();

  @Override
  public abstract boolean equals(@Nullable Object o);

  @Override
  public abstract int hashCode();

  /**
   * Compare NATs of equal type for sorting. Orders NATs in the order they would appear in the
   * config (so highest precedence first).
   *
   * @param other NAT to compare
   * @return a negative integer if this NAT should be matched before the other, positive if it
   *     should be matched after, or 0 if the NATs are of different types or their match order
   *     doesn't matter (e.g. both malformed or match mutually exclusive traffic).
   * @throws IllegalArgumentException if NATs are of different types.
   */
  protected abstract int natCompare(CiscoIosNat other);

  @Override
  public final int compareTo(CiscoIosNat other) {
    return Comparator.comparingInt(CiscoIosNatUtil::getTypePrecedence)
        .thenComparing(CiscoIosNat::natCompare)
        .compare(this, other);
  }

  public enum RuleAction {
    SOURCE_INSIDE,
    SOURCE_OUTSIDE,
    DESTINATION_INSIDE;

    IpField whatChanges(boolean outgoing) {
      switch (this) {
        case SOURCE_INSIDE:
          // Match and transform source for outgoing (inside-to-outside)
          // Match and transform destination for incoming (outside-to-inside)
          return outgoing ? IpField.SOURCE : IpField.DESTINATION;
        case SOURCE_OUTSIDE:
          // Match and transform destination for outgoing (inside-to-outside)
          // Match and transform source for incoming (outside-to-inside)
          return outgoing ? IpField.DESTINATION : IpField.SOURCE;
        case DESTINATION_INSIDE:
          // Match and transform destination for outgoing (inside-to-outside)
          // Match and transform source for incoming (outside-to-inside)
          return outgoing ? IpField.DESTINATION : IpField.SOURCE;
        default:
          throw new BatfishException("Unsupported RuleAction");
      }
    }
  }

  // Direction of NAT
  public enum Direction {
    INSIDE,
    OUTSIDE
  }
}

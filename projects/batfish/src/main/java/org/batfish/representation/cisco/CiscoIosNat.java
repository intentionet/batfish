package org.batfish.representation.cisco;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
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
  public boolean getAddRoute() {
    return _addRoute;
  }

  public void setAddRoute(boolean addRoute) {
    _addRoute = addRoute;
  }

  /** Which VRF this NAT is in */
  @Nullable
  public String getVrf() {
    return _vrf;
  }

  public void setVrf(@Nullable String vrf) {
    _vrf = vrf;
  }

  /**
   * Converts a single NAT from the configuration into a {@link Transformation}.
   *
   * @param ipAccessLists Named access lists which may be referenced by dynamic NATs
   * @param natPools NAT pools from the configuration
   * @param insideInterfaces Names of interfaces which are defined as 'inside'
   * @param c Configuration
   * @return A single {@link Transformation} for inside-to-outside, or nothing if the {@link
   *     Transformation} could not be built
   */
  public abstract Optional<Transformation.Builder> toOutgoingTransformation(
      Map<String, IpAccessList> ipAccessLists,
      Map<String, NatPool> natPools,
      Set<String> insideInterfaces,
      Map<String, Interface> interfaces,
      Configuration c);

  /**
   * Converts a single NAT from the configuration into a {@link Transformation}.
   *
   * @param ipAccessLists Named access lists which may be referenced by dynamic NATs
   * @param natPools NAT pools from the configuration
   * @return A single {@link Transformation} for inside-to-outside, or nothing if the {@link
   *     Transformation} could not be built
   */
  public abstract Optional<Transformation.Builder> toIncomingTransformation(
      Map<String, IpAccessList> ipAccessLists,
      Map<String, NatPool> natPools,
      Map<String, Interface> interfaces);

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
   * Compare NATs of equal type for sorting.
   *
   * @param other NAT to compare
   * @return a negative integer, zero, or a positive integer as this NAT precedence is less than,
   *     equal to, or greater than the specified NAT precedence.
   */
  protected abstract int natCompare(CiscoIosNat other);

  @Override
  public final int compareTo(CiscoIosNat other) {
    return Comparator.comparingInt(CiscoIosNatUtil::getTypePrecedence)
        .thenComparing(this::natCompare)
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

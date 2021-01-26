package org.batfish.representation.cisco;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.representation.cisco.CiscoConfiguration.DEFAULT_STATIC_ROUTE_DISTANCE;
import static org.batfish.representation.cisco.CiscoIosNatUtil.toMatchExpr;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationStep;

@ParametersAreNonnullByDefault
public class CiscoIosStaticNat extends CiscoIosNat {

  private Prefix _localNetwork;
  private Prefix _globalNetwork;

  @Override
  protected int natCompare(CiscoIosNat o) {
    checkArgument(
        o instanceof CiscoIosStaticNat,
        "CiscoIosNat.natCompare should only be used for NATs of the same type.");
    CiscoIosStaticNat other = (CiscoIosStaticNat) o;
    // Rules with longer prefixes should come first
    return Comparator.comparing(Prefix::getPrefixLength)
        .reversed()
        .compare(_localNetwork, other._localNetwork);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof CiscoIosStaticNat)) {
      return false;
    }
    CiscoIosStaticNat other = (CiscoIosStaticNat) o;
    return (getAction() == other.getAction())
        && (getAddRoute() == other.getAddRoute())
        && Objects.equals(getRouteMap(), other.getRouteMap())
        && Objects.equals(getVrf(), other.getVrf())
        && Objects.equals(_localNetwork, other._localNetwork)
        && Objects.equals(_globalNetwork, other._globalNetwork);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getAction(), getAddRoute(), _localNetwork, _globalNetwork, getRouteMap(), getVrf());
  }

  public Prefix getLocalNetwork() {
    return _localNetwork;
  }

  public void setLocalNetwork(Prefix localNetwork) {
    _localNetwork = localNetwork;
  }

  public Prefix getGlobalNetwork() {
    return _globalNetwork;
  }

  public void setGlobalNetwork(Prefix globalNetwork) {
    _globalNetwork = globalNetwork;
  }

  @Override
  public Optional<Transformation.Builder> toOutgoingTransformation(
      String ifaceName,
      Map<String, RouteMap> routeMaps,
      Map<String, NatPool> natPools,
      Set<String> insideInterfaces,
      Map<String, Interface> interfaces,
      Configuration c,
      Warnings w) {
    ImmutableList.Builder<AclLineMatchExpr> conjunctsToMatch = ImmutableList.builder();

    // Match src/dest to global/local according to direction and rule type
    TransformationStep step;
    switch (getAction()) {
      case SOURCE_INSIDE:
        conjunctsToMatch.add(AclLineMatchExprs.matchSrc(_localNetwork));
        step = TransformationStep.shiftSourceIp(_globalNetwork);
        break;
      case SOURCE_OUTSIDE:
        conjunctsToMatch.add(AclLineMatchExprs.matchDst(_localNetwork));
        step = TransformationStep.shiftDestinationIp(_globalNetwork);
        break;
      default:
        return Optional.empty();
    }

    // Create match expr for route-map if one is configured
    if (getRouteMap() != null) {
      Optional<AclLineMatchExpr> matchRouteMap =
          getRouteMapMatchExpr(ifaceName, routeMaps, c.getIpAccessLists(), w);
      if (!matchRouteMap.isPresent()) {
        return Optional.empty();
      } else {
        conjunctsToMatch.add(matchRouteMap.get());
      }
    }

    conjunctsToMatch.add(new MatchSrcInterface(insideInterfaces));
    return Optional.of(Transformation.when(and(conjunctsToMatch.build())).apply(step));
  }

  @Override
  public Optional<Transformation.Builder> toIncomingTransformation(
      String ifaceName,
      Map<String, IpAccessList> ipAccessLists,
      Map<String, RouteMap> routeMaps,
      Map<String, NatPool> natPools,
      Map<String, Interface> interfaces,
      Warnings w) {
    ImmutableList.Builder<AclLineMatchExpr> conjunctsToMatch = ImmutableList.builder();

    // Match src/dest to global/local according to direction and rule type
    TransformationStep step;
    switch (getAction()) {
      case SOURCE_INSIDE:
        conjunctsToMatch.add(AclLineMatchExprs.matchDst(_globalNetwork));
        step = TransformationStep.shiftDestinationIp(_localNetwork);
        break;
      case SOURCE_OUTSIDE:
        conjunctsToMatch.add(AclLineMatchExprs.matchSrc(_globalNetwork));
        step = TransformationStep.shiftSourceIp(_localNetwork);
        break;
      default:
        return Optional.empty();
    }

    // Create match expr for route-map if one is configured
    if (getRouteMap() != null) {
      Optional<AclLineMatchExpr> matchRouteMap =
          getRouteMapMatchExpr(ifaceName, routeMaps, ipAccessLists, w);
      if (!matchRouteMap.isPresent()) {
        return Optional.empty();
      } else {
        conjunctsToMatch.add(matchRouteMap.get());
      }
    }

    return Optional.of(Transformation.when(and(conjunctsToMatch.build())).apply(step));
  }

  /**
   * Handles conversion of {@link #getRouteMap() route-map} to an {@link AclLineMatchExpr}. Assumes
   * there is a route-map configured, so returning an empty optional indicates something has gone
   * wrong -- the route-map doesn't exist or can't be converted.
   */
  private Optional<AclLineMatchExpr> getRouteMapMatchExpr(
      String ifaceName,
      Map<String, RouteMap> routeMaps,
      Map<String, IpAccessList> ipAccessLists,
      Warnings w) {
    // route-map can't be configured for outside NAT; static rules can't be used for destination nat
    assert getAction() == RuleAction.SOURCE_INSIDE;
    String routeMap = getRouteMap();
    assert routeMap != null; // this method should only be called if a route-map is configured.
    RouteMap rm = routeMaps.get(routeMap);
    if (rm == null) {
      w.redFlag(String.format("Ignoring NAT rule with undefined route-map %s", routeMap));
      return Optional.empty();
    }
    return toMatchExpr(rm, ipAccessLists.keySet(), ifaceName, w);
  }

  @Override
  public Optional<StaticRoute> toRoute() {
    if (!getAddRoute() || getVrf() != null) {
      return Optional.empty();
    }
    assert getAction() == RuleAction.SOURCE_OUTSIDE; // only valid option for add-route
    return Optional.of(
        new StaticRoute(
            _localNetwork,
            _globalNetwork.getStartIp(),
            null,
            DEFAULT_STATIC_ROUTE_DISTANCE,
            null,
            null,
            false));
  }
}

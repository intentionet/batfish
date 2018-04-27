package org.batfish.dataplane.rib;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;

/**
 * Used to store the routes, supports longest prefix match operation.
 *
 * <p>This data structure is a more restrictive version of a ddNF (disjoint difference Normal Form),
 * where the wildcard symbols can appear only after (to-the-right-of) non wildcard symbols in the
 * bit vector. E.g., 101010**, but not 1*001***
 */
class RibTree<R extends AbstractRoute> implements Serializable {

  private static final long serialVersionUID = 1L;

  private RibTreeNode<R> _root;

  RibTree(AbstractRib<R> owner) {
    _root = new RibTreeNode<>(Prefix.ZERO, owner);
  }

  /**
   * Remove a single route from the RIB, if it exists
   *
   * @param route route to remove
   * @return {@link RibDelta} if the route was removed, otherwise {@code null};
   */
  @Nullable
  RibDelta<R> removeRouteGetDelta(R route, Reason reason) {
    Prefix prefix = route.getNetwork();
    int prefixLength = prefix.getPrefixLength();
    long bits = prefix.getStartIp().asLong();
    return _root.removeRoute(route, bits, prefixLength, 0, reason);
  }

  /**
   * Check if the route is present in the RIB
   *
   * @param route route to find
   * @return true if the route exists in the RIB
   */
  boolean containsRoute(R route) {
    Prefix prefix = route.getNetwork();
    int prefixLength = prefix.getPrefixLength();
    long bits = prefix.getStartIp().asLong();
    return _root.containsRoute(route, bits, prefixLength);
  }

  Set<R> getLongestPrefixMatch(Ip address) {
    long addressBits = address.asLong();
    return _root.getLongestPrefixMatch(address, addressBits, 0);
  }

  /**
   * Return a set of all routes contained in this RIB
   *
   * @return a {@link Set} of routes
   */
  public Set<R> getRoutes() {
    ImmutableSet.Builder<R> routes = ImmutableSet.builder();
    _root.collectRoutes(routes);
    return routes.build();
  }

  /**
   * Add a new route into the RIB, potentially replacing other routes
   *
   * @param route route to add
   * @return a {@link RibDelta} objects indicating which routes where added and evicted from this
   *     RIB
   */
  @Nullable
  RibDelta<R> mergeRoute(R route) {
    Prefix prefix = route.getNetwork();
    int prefixLength = prefix.getPrefixLength();
    long bits = prefix.getStartIp().asLong();
    return _root.mergeRoute(route, bits, prefixLength, 0);
  }

  @Override
  public int hashCode() {
    return _root.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj == this) || (obj instanceof RibTree && this._root.equals(((RibTree<?>) obj)._root));
  }

  public RibDelta<R> clearRoutes(Prefix prefix) {
    return _root.clearRoutes(prefix);
  }

  public Map<Prefix, IpWildcardSetIpSpace> getMatchingIps() {
    ImmutableMap.Builder<Prefix, IpWildcardSetIpSpace> builder = ImmutableMap.builder();
    _root.addMatchingIps(builder);
    return builder.build();
  }

  IpSpace getRoutableIps() {
    IpWildcardSetIpSpace.Builder builder = IpWildcardSetIpSpace.builder();
    _root.addRoutableIps(builder);
    return builder.build();
  }
}

package org.batfish.dataplane.rib;

import static org.batfish.dataplane.rib.RouteAdvertisement.Reason.REPLACE;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixTrieNode;
import org.batfish.dataplane.rib.RibDelta.Builder;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;

/**
 * RibTree is constructed from nodes of this type. A node has a prefix, a set of routes that match
 * the prefix (and it's length) and two children. The children's prefixes must always be more
 * specific (i.e., their prefix length is larger).
 */
@ParametersAreNonnullByDefault
final class RibTreeNode<R extends AbstractRoute> extends PrefixTrieNode<RibTreeNode<R>, R> {

  private static final long serialVersionUID = 1L;

  private AbstractRib<R> _owner;

  RibTreeNode(Prefix prefix, @Nonnull AbstractRib<R> owner) {
    super(prefix);
    _owner = owner;
  }

  @Nonnull
  @Override
  public RibTreeNode<R> makeNode(Prefix prefix) {
    return new RibTreeNode<>(prefix, _owner);
  }

  @Nonnull
  @Override
  public RibTreeNode<R> getThis() {
    return this;
  }

  void collectRoutes(ImmutableCollection.Builder<R> routes) {
    super.collect(routes);
  }

  /**
   * Check if the route exists in our subtree
   *
   * @param route route in question
   * @return true if the route is in the subtree
   */
  boolean containsRoute(R route) {
    RibTreeNode<R> node = findNode(route.getNetwork());
    if (node == this) {
      return _objects.contains(route);
    }
    return node != null && node.containsRoute(route);
  }

  /** Returns the forwarding routes stored in this node. */
  private Set<R> getForwardingRoutes() {
    return _objects.stream()
        .filter(r -> !r.getNonForwarding())
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Returns a set of routes with the longest prefix match for a given IP address
   *
   * @param address IP address
   * @param bits IP address represented as a set of bits
   * @param maxPrefixLength only return routes with prefix length less than or equal to given value
   * @return a set of routes
   */
  @Nonnull
  Set<R> getLongestPrefixMatch(Ip address, long bits, int maxPrefixLength) {
    int pl = maxPrefixLength;
    RibTreeNode<R> node = findLongestPrefixMatchNode(address, bits, pl);
    while ((node == null || !node.hasForwardingRoute()) && pl >= 0) {
      pl--;
      node = findLongestPrefixMatchNode(address, bits, pl);
    }
    return node == null || !node.hasForwardingRoute()
        ? ImmutableSet.of()
        : node.getForwardingRoutes();
  }

  /** Retrieve an immutable copy of the routes currently available for the given prefix. */
  @Nonnull
  Set<R> getRoutes(Prefix p) {
    RibTreeNode<R> node = findNode(p);
    if (node == null) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(node._objects);
  }

  @Nonnull
  RibDelta<R> mergeRoute(R route) {

    RibTreeNode<R> node = findOrCreateNode(route.getNetwork());
    // No routes with this prefix, so just add it. No comparison necessary
    if (node._objects.isEmpty()) {
      node._objects.add(route);
      return RibDelta.<R>builder().add(route).build();
    }

    /*
     * Check if the route we are adding is preferred to the routes we already have.
     * We only need to compare to one route, because all routes already in this node have the
     * same preference level. Hence, the route we are checking will be better than all,
     * worse than all, or at the same preference level.
     */

    R oldRoute = node._objects.iterator().next();
    int preferenceComparison = _owner.comparePreference(route, oldRoute);
    if (preferenceComparison < 0) { // less preferable, so route doesn't get added
      return RibDelta.empty();
    }
    if (preferenceComparison == 0) { // equal preference, so add for multipath routing
      // Otherwise add the route
      if (node._objects.add(route)) {
        return RibDelta.<R>builder().add(route).build();
      } else {
        return RibDelta.empty();
      }
    }
    // Last case, preferenceComparison > 0
    /*
     * Better than all existing routes for this prefix, so
     * replace them with this one.
     */
    RibDelta<R> delta = RibDelta.<R>builder().remove(node._objects, REPLACE).add(route).build();
    node._objects.clear();
    node._objects.add(route);
    return delta;
  }

  @Override
  public String toString() {
    return _prefix.toString();
  }

  @Nonnull
  RibDelta<R> removeRoute(R route, Reason reason) {
    RibTreeNode<R> node = findNode(route.getNetwork());
    if (node == null) {
      // No effect, return empty
      return RibDelta.empty();
    }
    Builder<R> b = RibDelta.builder();
    if (node._objects.remove(route)) {
      b.remove(route, reason);
      if (node._objects.isEmpty() && _owner._backupRoutes != null) {
        SortedSet<? extends R> backups =
            _owner._backupRoutes.getOrDefault(route.getNetwork(), ImmutableSortedSet.of());
        if (!backups.isEmpty()) {
          node._objects.add(backups.first());
          b.add(backups.first());
        }
      }
    }
    // Return new delta
    return b.build();
  }

  RibDelta<R> clearRoutes(Prefix prefix) {
    RibTreeNode<R> node = findNode(prefix);
    if (node == null) {
      return RibDelta.empty();
    }
    RibDelta<R> delta = RibDelta.<R>builder().remove(node._objects, REPLACE).build();
    node._objects.clear();
    return delta;
  }

  void addMatchingIps(ImmutableMap.Builder<Prefix, IpSpace> builder) {
    if (_left != null) {
      _left.addMatchingIps(builder);
    }
    if (_right != null) {
      _right.addMatchingIps(builder);
    }
    if (hasForwardingRoute()) {
      IpWildcardSetIpSpace.Builder matchingIps = IpWildcardSetIpSpace.builder();
      if (_left != null) {
        _left.excludeRoutableIps(matchingIps);
      }
      if (_right != null) {
        _right.excludeRoutableIps(matchingIps);
      }
      matchingIps.including(new IpWildcard(_prefix));
      builder.put(_prefix, matchingIps.build());
    }
  }

  void addRoutableIps(IpWildcardSetIpSpace.Builder builder) {
    if (hasForwardingRoute()) {
      builder.including(new IpWildcard(_prefix));
    } else {
      if (_left != null) {
        _left.addRoutableIps(builder);
      }
      if (_right != null) {
        _right.addRoutableIps(builder);
      }
    }
  }

  private void excludeRoutableIps(IpWildcardSetIpSpace.Builder builder) {
    if (hasForwardingRoute()) {
      builder.excluding(new IpWildcard(_prefix));
    } else {
      if (_left != null) {
        _left.excludeRoutableIps(builder);
      }
      if (_right != null) {
        _right.excludeRoutableIps(builder);
      }
    }
  }

  private boolean hasForwardingRoute() {
    return !_objects.stream().allMatch(AbstractRoute::getNonForwarding);
  }
}

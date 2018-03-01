package org.batfish.bdp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.collections.MultiSet;
import org.batfish.datamodel.collections.TreeMultiSet;

/**
 * Implements general RIB (Routing Information Base) semantics. RIB stores routes for different
 * network prefixes and supports retrieving them based on the longest prefix match between a given
 * IP address and the route's IP prefix.
 *
 * @param <R> Type of route that this RIB will be storing. Required for properly comparing route
 *     preferences.
 */
public abstract class AbstractRib<R extends AbstractRoute> implements GenericRib<R> {

  public final Map<Prefix, IpSpace> getMatchingIps() {
    return _tree.getMatchingIps();
  }

  public final IpSpace getRoutableIps() {
    return _tree.getRoutableIps();
  }

  /**
   * Used to store the routes, supports longest prefix match operation.
   *
   * <p>This data structure is a more restrictive version of a ddNF (disjoint difference Normal
   * Form), where the wildcard symbols can appear only after (to-the-right-of) non wildcard symbols
   * in the bit vector. E.g., 101010**, but not 1*001***
   */
  class RibTree implements Serializable {

    private static final long serialVersionUID = 1L;

    RibTreeNode _root;

    RibTree() {
      _root = new RibTreeNode(Prefix.ZERO);
    }

    public Map<Prefix, IpSpace> getMatchingIps() {
      ImmutableMap.Builder<Prefix, IpSpace> builder = ImmutableMap.builder();
      _root.addMatchingIps(builder);
      return builder.build();
    }

    IpSpace getRoutableIps() {
      IpSpace.Builder builder = IpSpace.builder();
      _root.addRoutableIps(builder);
      return builder.build();
    }

    boolean containsRoute(R route) {
      Prefix prefix = route.getNetwork();
      int prefixLength = prefix.getPrefixLength();
      long bits = prefix.getStartIp().asLong();
      return _root.containsRoute(route, bits, prefixLength, 0);
    }

    Set<R> getLongestPrefixMatch(Ip address) {
      long addressBits = address.asLong();
      return _root.getLongestPrefixMatch(address, addressBits, 0);
    }

    public Set<R> getRoutes() {
      Set<R> routes = new LinkedHashSet<>();
      _root.collectRoutes(routes);
      return routes;
    }

    boolean mergeRoute(R route) {
      Prefix prefix = route.getNetwork();
      int prefixLength = prefix.getPrefixLength();
      long bits = prefix.getStartIp().asLong();
      return _root.mergeRoute(route, bits, prefixLength, 0);
    }

    private boolean hasSameRoutes(AbstractRib<?>.RibTree other) {
      return _root.hasSameRoutes(other._root);
    }
  }

  /**
   * RibTree is constructed from nodes of this type. A node has a prefix, a set of routes that match
   * the prefix (and it's length) and two children. The children's prefixes must always be more
   * specific (i.e., their prefix length is larger).
   */
  class RibTreeNode implements Serializable {

    private static final long serialVersionUID = 1L;

    RibTreeNode _left;

    Prefix _prefix;

    RibTreeNode _right;

    Set<R> _routes;

    RibTreeNode(Prefix prefix) {
      _routes = Collections.emptySet();
      _prefix = prefix;
    }

    public void addMatchingIps(ImmutableMap.Builder<Prefix, IpSpace> builder) {
      if (_left != null) {
        _left.addMatchingIps(builder);
      }
      if (_right != null) {
        _right.addMatchingIps(builder);
      }
      if (!_routes.isEmpty()) {
        IpSpace.Builder matchingIps = IpSpace.builder();
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

    public void addRoutableIps(IpSpace.Builder builder) {
      if (!_routes.isEmpty()) {
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

    public void excludeRoutableIps(IpSpace.Builder builder) {
      if (!_routes.isEmpty()) {
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

    void collectRoutes(Set<R> routes) {
      if (_left != null) {
        _left.collectRoutes(routes);
      }
      if (_right != null) {
        _right.collectRoutes(routes);
      }
      routes.addAll(_routes);
    }

    /**
     * Check if the route exists in our subtree
     *
     * @param route route in question
     * @param bits route's IP address represented as a long
     * @param prefixLength route's prefix length
     * @param firstUnmatchedBitIndex how far into the address have we matched
     * @return true if the route is in the subtree
     */
    boolean containsRoute(R route, long bits, int prefixLength, int firstUnmatchedBitIndex) {
      // If prefix lengths match, this is the node where such route would be stored.
      if (prefixLength == _prefix.getPrefixLength()) {
        return _routes.contains(route);
      }
      boolean currentBit = Ip.getBitAtPosition(bits, firstUnmatchedBitIndex);

      /*
       * If prefixes don't match exactly, look at the current bit. That determines whether we look
       * left or right. As long as the child is not null, recurse.
       *
       * Note that:
       * 1) routes are stored in the nodes where lengths of the node prefix and the route prefix
       *    match exactly; and
       * 2) prefix matches only get more specific (longer) the deeper we go in the tree
       *
       * Therefore, we can fast-forward the firstUnmatchedBitIndex to the prefix length of the
       * child node
       */
      if (currentBit) {
        return _right != null
            && _right.containsRoute(route, bits, prefixLength, _right._prefix.getPrefixLength());
      } else {
        return _left != null
            && _left.containsRoute(route, bits, prefixLength, _left._prefix.getPrefixLength());
      }
    }

    private Set<R> getLongestPrefixMatch(Ip address) {
      Set<R> longestPrefixMatches = new HashSet<>();
      for (R route : _routes) {
        Prefix prefix = route.getNetwork();
        if (prefix.contains(address)) {
          longestPrefixMatches.add(route);
        }
      }
      return longestPrefixMatches;
    }

    /**
     * Returns a set of routes with the longest prefix match for a given IP address
     *
     * @param address IP address
     * @param bits IP address represented as a set of bits
     * @param index the position of the bit up to which the match has already been found
     *     (tail-recursion way of keeping track how deep we are).
     * @return a set of routes
     */
    Set<R> getLongestPrefixMatch(Ip address, long bits, int index) {
      // Get the list of routes stored in our node that contain the IP address
      Set<R> longestPrefixMatches = getLongestPrefixMatch(address);
      // If we reached the max prefix length (e.g., 32 for for IPv4) then return routes
      // from the current node
      if (index == Prefix.MAX_PREFIX_LENGTH) {
        return longestPrefixMatches;
      }

      // Examine the bit at the given index
      boolean currentBit = Ip.getBitAtPosition(bits, index);
      RibTreeNode child;

      // the current bit is 1, go right recursively
      if (currentBit) {
        child = _right;
      } else {
        child = _left;
      }
      if (child == null) {
        return longestPrefixMatches;
      }

      // Represents any potentially longer route matches (than ones stored at this node)
      Set<R> longerMatches =
          child.getLongestPrefixMatch(address, bits, child._prefix.getPrefixLength());

      // If we found no better matches, return the ones from this node
      if (longerMatches == null || longerMatches.isEmpty()) {
        return longestPrefixMatches;
      } else { // otherwise return longer matches
        return longerMatches;
      }
    }

    void assignChild(RibTreeNode parent, RibTreeNode child, boolean branchRight) {
      if (branchRight) {
        parent._right = child;
      } else {
        parent._left = child;
      }
    }

    /**
     * Takes care of adding new nodes to the tree and maintaining correct pointers.
     *
     * @param parent node that we are trying to merge a route into
     * @param route the route to merge
     * @param routeBits the long representation of the route's IP address
     * @param prefixLength the route's prefix length
     * @param firstUnmatchedBitIndex the index of the first bit in the route's prefix that we
     *     haven't checked yet
     * @param rightBranch whether we should recurse down the right side of the tree
     * @return True if a route has been inserted into the tree
     */
    boolean mergeHelper(
        RibTreeNode parent,
        R route,
        long routeBits,
        int prefixLength,
        int firstUnmatchedBitIndex,
        boolean rightBranch) {
      RibTreeNode node;

      // Get our node from one of the tree sides
      if (rightBranch) {
        node = parent._right;
      } else {
        node = parent._left;
      }

      // Node doesn't exist, so create one. By construction, it will be the best match
      // for the given route
      if (node == null) {
        node = new RibTreeNode(route.getNetwork());
        node._routes = Collections.singleton(route);
        // don't forget to assign new node object to parent node
        assignChild(parent, node, rightBranch);
        return true;
      }

      // Node exists, get some helper data out of the current node we are examining
      Prefix nodePrefix = node._prefix;
      int nodePrefixLength = nodePrefix.getPrefixLength();
      Ip nodeAddress = nodePrefix.getStartIp();
      long nodeAddressBits = nodeAddress.asLong();
      int nextUnmatchedBit;
      // Set up two "pointers" as we scan through the route's and the node's prefixes
      boolean currentAddressBit = false;
      boolean currentNodeAddressBit;

      /*
       * We know we matched up to firstUnmatchedBitIndex. Continue going forward in the bits
       * to find a longer match.
       * At the end of this loop nextUnmatchedBit will be the first place where the route prefix
       * and this node's prefix diverge.
       * Note that nextUnmatchedBit can be outside of the node's or the route's prefix.
       */
      for (nextUnmatchedBit = firstUnmatchedBitIndex + 1;
          nextUnmatchedBit < nodePrefixLength && nextUnmatchedBit < prefixLength;
          nextUnmatchedBit++) {
        currentAddressBit = Ip.getBitAtPosition(routeBits, nextUnmatchedBit);
        currentNodeAddressBit = Ip.getBitAtPosition(nodeAddressBits, nextUnmatchedBit);
        if (currentNodeAddressBit != currentAddressBit) {
          break;
        }
      }

      /*
       * If the next unmatched bit is the same as node prefix length, we "ran off" the node prefix.
       * Recursively merge the route into this node.
       */
      if (nextUnmatchedBit == nodePrefixLength) {
        return node.mergeRoute(route, routeBits, prefixLength, nextUnmatchedBit);
      }

      /*
       * If we reached the route's prefix length (but not the nodes's) we need to create a new node
       * above the current node that matches the route's prefix and re-attach the current node to
       * the newly created node.
       */
      if (nextUnmatchedBit == prefixLength) {
        currentNodeAddressBit = Ip.getBitAtPosition(nodeAddressBits, nextUnmatchedBit);
        RibTreeNode oldNode = node;
        node = new RibTreeNode(route.getNetwork());
        node._routes = Collections.singleton(route);
        assignChild(parent, node, rightBranch);
        assignChild(node, oldNode, currentNodeAddressBit);
        return true;
      }

      /*
       * If we are here, there is a bit difference between the node and route prefixes before we
       * reach the end of either prefix. This requires the following:
       * - Compute the max prefix match (up to nextUnmatchedBit)
       * - Create a new node with this new prefix above the current node
       * - Create a new node with the route's full prefix and assign it the parent.
       * - Existing node becomes a sibling of the node with full route prefix
       */
      RibTreeNode oldNode = node;

      // newNetwork has the max prefix match up to nextUnmatchedBit
      Prefix newNetwork = new Prefix(route.getNetwork().getStartIp(), nextUnmatchedBit);
      node = new RibTreeNode(newNetwork); // node is the node we are inserting in the middle
      RibTreeNode child = new RibTreeNode(route.getNetwork());
      child._routes = Collections.singleton(route);
      assignChild(parent, node, rightBranch);
      // child and old node become siblings, children of the newly inserted node
      assignChild(node, child, currentAddressBit);
      assignChild(node, oldNode, !currentAddressBit);
      return true;
    }

    boolean mergeRoute(R route, long routeBits, int routePrefixLength, int firstUnmatchedBitIndex) {
      /*
       * We have reached the node where a route should be inserted, because:
       * 1) the prefix length of this node matches the prefix length of the route exactly, and
       * 2) going deeper can only gets us longer matches
       */
      if (routePrefixLength == _prefix.getPrefixLength()) {

        // No routes with this prefix, so just add it. No comparison necessary
        if (_routes.isEmpty()) {
          _routes = Collections.singleton(route);
          return true;
        }

        /*
         * Check if the route we are adding is preferred to the routes we already have.
         * We only need to compare to one route, because all routes already in this node have the
         * same preference level. Hence, the route we are checking will be better than all,
         * worse than all, or at the same preference level.
         */
        R rhs = _routes.iterator().next();
        int preferenceComparison = comparePreference(route, rhs);
        if (preferenceComparison < 0) { // less preferable, so route doesn't get added
          return false;
        }
        if (preferenceComparison == 0) { // equal preference, so add for multipath routing
          if (_routes.contains(route)) {
            // route is already here, so nothing to do
            return false;
          }
          // Otherwise add the route
          _routes = ImmutableSet.<R>builder().addAll(_routes).add(route).build();
          return true;
        }
        // Last case, preferenceComparison > 0
        /*
         * Better than all pre-existing routes for this prefix, so
         * replace them with this one.
         */
        _routes = Collections.singleton(route);
        return true;
      }
      /*
       * The prefix match is not exact, do some extra insertion logic.
       * Current bit determines which side of the tree to go down (1 = right, 0 = left)
       */
      boolean currentBit = Ip.getBitAtPosition(routeBits, firstUnmatchedBitIndex);
      return mergeHelper(
          this, route, routeBits, routePrefixLength, firstUnmatchedBitIndex, currentBit);
    }

    @Override
    public String toString() {
      return _prefix.toString();
    }

    private boolean hasSameRoutes(@Nullable AbstractRib<?>.RibTreeNode other) {
      /*
       * If other is not null, compare routes stored at this node with routes at other node,
       * followed by comparison of left and right branches recursively.
       */
      return (other != null && _routes.equals(other._routes))
          && (_left == null ? other._left == null : _left.hasSameRoutes(other._left))
          && (_right == null ? other._right == null : _right.hasSameRoutes(other._right));
    }
  }

  private static final long serialVersionUID = 1L;

  protected VirtualRouter _owner;

  RibTree _tree;

  private Set<R> _allRoutes;

  public AbstractRib(VirtualRouter owner) {
    _tree = new RibTree();
    _owner = owner;
    _allRoutes = ImmutableSet.of();
  }

  final boolean containsRoute(R route) {
    return _tree.containsRoute(route);
  }

  @Override
  public final MultiSet<Prefix> getPrefixCount() {
    MultiSet<Prefix> prefixCount = new TreeMultiSet<>();
    for (R route : getRoutes()) {
      Prefix prefix = route.getNetwork();
      prefixCount.add(prefix);
    }
    return prefixCount;
  }

  @Override
  public final SortedSet<Prefix> getPrefixes() {
    SortedSet<Prefix> prefixes = new TreeSet<>();
    Set<R> routes = getRoutes();
    for (R route : routes) {
      prefixes.add(route.getNetwork());
    }
    return prefixes;
  }

  @Override
  public final Set<R> getRoutes() {
    if (_allRoutes == null) {
      _allRoutes = ImmutableSet.copyOf(_tree.getRoutes());
    }
    return _allRoutes;
  }

  @Override
  public final Map<Integer, Map<Ip, List<AbstractRoute>>> getRoutesByPrefixPopularity() {
    Map<Integer, Map<Ip, List<AbstractRoute>>> map = new TreeMap<>();
    MultiSet<Prefix> prefixCountSet = getPrefixCount();
    for (AbstractRoute route : getRoutes()) {
      Prefix prefix = route.getNetwork();
      int prefixCount = prefixCountSet.count(prefix);
      Map<Ip, List<AbstractRoute>> byIp = map.computeIfAbsent(prefixCount, k -> new TreeMap<>());
      Ip nextHopIp = route.getNextHopIp();
      List<AbstractRoute> routesByPopularity =
          byIp.computeIfAbsent(nextHopIp, k -> new ArrayList<>());
      routesByPopularity.add(route);
    }
    return map;
  }

  @Override
  public Set<R> longestPrefixMatch(Ip address) {
    return _tree.getLongestPrefixMatch(address);
  }

  /**
   * Add a new route to the RIB.
   *
   * @param route the route to add
   * @return true if the route was added. False if the route already existed or was discarded due to
   *     preference comparisons.
   */
  @Override
  public boolean mergeRoute(R route) {
    _allRoutes = null;
    return _tree.mergeRoute(route);
  }

  @Override
  public final Map<Prefix, Set<Ip>> nextHopIpsByPrefix() {
    Map<Prefix, Set<Ip>> map = new TreeMap<>();
    for (AbstractRoute route : getRoutes()) {
      Prefix prefix = route.getNetwork();
      Ip nextHopIp = route.getNextHopIp();
      Set<Ip> nextHopIps = map.computeIfAbsent(prefix, k -> new TreeSet<>());
      nextHopIps.add(nextHopIp);
    }
    return map;
  }

  /**
   * Check if two RIBs have exactly same sets of routes.
   *
   * <p>Designed to be faster (in an average case) than doing two calls to {@link #getRoutes} and
   * then testing the sets for equality.
   *
   * @param other the other RIB
   * @return True if both ribs contain identical routes
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return other != null
        && (other instanceof AbstractRib<?>)
        && _tree.hasSameRoutes(((AbstractRib<?>) other)._tree);
  }
}

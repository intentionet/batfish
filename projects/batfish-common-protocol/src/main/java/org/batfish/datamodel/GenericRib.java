package org.batfish.datamodel;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public interface GenericRib<R extends AbstractRoute> extends Serializable {

  /**
   * Compare the preferability of one route with anther
   *
   * @param lhs 1st route with which to compare preference
   * @param rhs 2nd route with which to compare preference
   * @return -1 if lhs route is less preferable than rhs; 0 if lhs route and rhs are equally
   *     preferable (i.e. for multipath routing); 1 if lhs route is strictly more preferred than rhs
   */
  int comparePreference(R lhs, R rhs);

  /**
   * For each prefix appearing in a route in the RIB, get the IPs for which the longest-prefix match
   * is a route of that prefix.
   */
  Map<Prefix, IpSpace> getMatchingIps();

  SortedSet<Prefix> getPrefixes();

  /** Get all the IPs for which there is a matching route */
  IpSpace getRoutableIps();

  /** Return a set of routes this RIB contains. */
  Set<R> getRoutes();

  /**
   * Execute the longest prefix match for a given IP address.
   *
   * <p><strong>Note</strong>: this function returns only forwarding routes, aka, routes where
   * {@link AbstractRoute#getNonForwarding()} returns false.
   *
   * @param address the IP address to match
   * @return a set of routes with the maximum allowable prefix length that match the {@code address}
   */
  Set<R> longestPrefixMatch(Ip address);

  /**
   * Execute a constrained longest prefix match for a given IP address.
   *
   * <p><strong>Note</strong>: this function returns only forwarding routes, aka, routes where
   * {@link AbstractRoute#getNonForwarding()} returns false.
   *
   * <p>Most callers should use {@link #longestPrefixMatch(Ip)}; this function may be used when the
   * longest prefix matches are unsatisfactory and less specific routes are required.
   *
   * @param address the IP address to match
   * @param maxPrefixLength the maximum prefix length allowed (i.e., do not match more specific
   *     routes). This is a less than or equal constraint.
   * @return a set of routes that match the {@code address} given the constraint.
   */
  Set<R> longestPrefixMatch(Ip address, int maxPrefixLength);

  boolean mergeRoute(R route);
}

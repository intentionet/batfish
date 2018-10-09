package org.batfish.z3.expr;

import static org.hamcrest.Matchers.equalTo;

import org.batfish.z3.expr.NodeInterfaceNeighborUnreachableMatchersImpl.HasHostname;
import org.batfish.z3.state.NodeInterfaceNeighborUnreachable;
import org.hamcrest.Matcher;

public final class NodeInterfaceNeighborUnreachableMatchers {
  private NodeInterfaceNeighborUnreachableMatchers() {}

  public static Matcher<NodeInterfaceNeighborUnreachable> hasHostname(
      Matcher<String> hostnameMatcher) {
    return new HasHostname(hostnameMatcher);
  }

  public static Matcher<NodeInterfaceNeighborUnreachable> hasHostname(
      String hostname) {
    return new HasHostname(equalTo(hostname));
  }
}

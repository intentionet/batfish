package org.batfish.datamodel.matchers;

import org.batfish.datamodel.Topology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.matchers.TopologyMatchersImpl.IsNeighborOfNode;
import org.batfish.datamodel.matchers.TopologyMatchersImpl.WithNode;
import org.hamcrest.Matcher;

public class TopologyMatchers {

  /**
   * Provides a matcher that matches if the provided {@code node} is a neighbor of the {@link
   * NodeInterfacePair} to be matched in the provided {@link Topology}.
   */
  public static IsNeighborOfNode isNeighborOfNode(Topology topology, String node) {
    return new IsNeighborOfNode(topology, node);
  }

  /**
   * Provides a matcher that matches if the provided {@code subMatcher} matches the {@link
   * NodeInterfacePair} formed by qualifying the interface name to be matched with the provided
   * {@code node}.
   */
  public static WithNode withNode(String node, Matcher<? super NodeInterfacePair> subMatcher) {
    return new WithNode(node, subMatcher);
  }

  private TopologyMatchers() {}
}

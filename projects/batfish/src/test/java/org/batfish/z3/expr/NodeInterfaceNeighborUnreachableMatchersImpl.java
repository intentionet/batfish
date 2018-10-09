package org.batfish.z3.expr;

import org.batfish.z3.state.NodeInterfaceNeighborUnreachable;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

final class NodeInterfaceNeighborUnreachableMatchersImpl {
  private NodeInterfaceNeighborUnreachableMatchersImpl() {}

  public static final class HasHostname
      extends FeatureMatcher<NodeInterfaceNeighborUnreachable, String> {
    public HasHostname(Matcher<? super String> subMatcher) {
      super(subMatcher, "a NodeInterfaceNeighborUnreachable with hostname", "hostname");
    }

    @Override
    protected String featureValueOf(
        NodeInterfaceNeighborUnreachable nodeInterfaceNeighborUnreachable) {
      return nodeInterfaceNeighborUnreachable.getHostname();
    }
  }
}

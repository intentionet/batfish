package org.batfish.datamodel.matchers;

import java.util.Set;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

class TopologyMatchersImpl {

  static class IsNeighborOfNode extends TypeSafeDiagnosingMatcher<NodeInterfacePair> {

    private final String _node;

    private final Topology _topology;

    IsNeighborOfNode(@Nonnull Topology topology, @Nonnull String node) {
      _topology = topology;
      _node = node;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(
          String.format("is neighbor of %s in provided topology: %s", _node, _topology.getEdges()));
    }

    @Override
    protected boolean matchesSafely(NodeInterfacePair item, Description mismatchDescription) {
      Set<NodeInterfacePair> neighbors = _topology.getNeighbors(item);
      if (neighbors == null || neighbors.isEmpty()) {
        mismatchDescription.appendText(
            String.format(
                "%s has no neighbors in provided topology: %s", item, _topology.getEdges()));
        return false;
      }
      if (neighbors.stream().noneMatch(neighbor -> neighbor.getHostname().equals(_node))) {
        mismatchDescription.appendText(
            String.format("%s was not among the neighbors of %s: %s", _node, item, neighbors));
        return false;
      }
      return true;
    }
  }

  static class WithNode extends TypeSafeDiagnosingMatcher<String> {

    private final String _node;

    private Matcher<? super NodeInterfacePair> _subMatcher;

    WithNode(String node, Matcher<? super NodeInterfacePair> subMatcher) {
      _node = node;
      _subMatcher = subMatcher;
    }

    @Override
    public void describeTo(Description description) {
      description
          .appendText(String.format("as interface of %s: ", _node))
          .appendDescriptionOf(_subMatcher);
    }

    @Override
    protected boolean matchesSafely(String item, Description mismatchDescription) {
      NodeInterfacePair pair = NodeInterfacePair.of(_node, item);
      if (!_subMatcher.matches(pair)) {
        mismatchDescription
            .appendText(String.format("%s as interface of %s did not satisfy: ", item, _node))
            .appendDescriptionOf(_subMatcher);
        return false;
      }
      return true;
    }
  }

  private TopologyMatchersImpl() {}
}

package org.batfish.datamodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

public class MockDataPlane implements DataPlane {

  public static class Builder {

    private Map<String, Map<String, Fib>> _fibs;

    private SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> _ribs;

    private SortedSet<Edge> _topologyEdges;

    private Network<BgpNeighbor, BgpSession> _bgpTopology;

    private Builder() {
      _fibs = ImmutableMap.of();
      _ribs = ImmutableSortedMap.of();
      _topologyEdges = ImmutableSortedSet.of();
      _bgpTopology = NetworkBuilder.directed().build();
    }

    public MockDataPlane build() {
      return new MockDataPlane(_fibs, _ribs, _topologyEdges, _bgpTopology);
    }

    public Builder setRibs(SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
      _ribs = ribs;
      return this;
    }

    public Builder setTopologyEdges(SortedSet<Edge> topologyEdges) {
      _topologyEdges = topologyEdges;
      return this;
    }

    public Builder setBgpTopology(Network<BgpNeighbor, BgpSession> bgpTopology) {
      _bgpTopology = bgpTopology;
      return this;
    }
  }

  /** */
  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  private final Network<BgpNeighbor, BgpSession> _bgpTopology;

  private final Map<String, Map<String, Fib>> _fibs;

  private final SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> _ribs;

  private final SortedSet<Edge> _topologyEdges;

  private MockDataPlane(
      Map<String, Map<String, Fib>> fibs,
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs,
      SortedSet<Edge> topologyEdges,
      Network<BgpNeighbor, BgpSession> bgpTopology) {
    _fibs = fibs;
    _ribs = ImmutableSortedMap.copyOf(ribs);
    _topologyEdges = ImmutableSortedSet.copyOf(topologyEdges);
    _bgpTopology = bgpTopology;
  }

  @Override
  public Map<String, Map<String, Fib>> getFibs() {
    return _fibs;
  }

  /** Return the map of Ip owners (as computed during dataplane computation). Map structure */
  @Override
  public Map<Ip, Set<String>> getIpOwners() {
    return null;
  }

  @Override
  public SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> getRibs() {
    return _ribs;
  }

  @Override
  public Topology getTopology() {
    return null;
  }

  @Override
  public SortedSet<Edge> getTopologyEdges() {
    return _topologyEdges;
  }

  @Override
  public Network<BgpNeighbor, BgpSession> getBgpTopology() {
    return _bgpTopology;
  }

  @Override
  public Map<String, Configuration> getConfigurations() {
    return null;
  }
}

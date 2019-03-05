package org.batfish.datamodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Table;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.Nullable;

public class MockDataPlane implements DataPlane {

  public static class Builder {
    private ValueGraph<BgpPeerConfigId, BgpSessionProperties> _bgpTopology;

    private Map<String, Configuration> _configurations;

    private Map<String, Map<String, Fib>> _fibs;

    private ForwardingAnalysis _forwardingAnalysis;

    private SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> _ribs;

    @Nullable private Topology _topology;

    private SortedSet<Edge> _topologyEdges;

    Map<Ip, Set<String>> _ipOwners;

    Map<Ip, Map<String, Set<String>>> _ipVrfOwners;

    private Builder() {
      _bgpTopology = ValueGraphBuilder.directed().build();
      _configurations = ImmutableMap.of();
      _fibs = ImmutableMap.of();
      _ribs = ImmutableSortedMap.of();
      _topologyEdges = ImmutableSortedSet.of();
      _ipOwners = ImmutableMap.of();
    }

    public MockDataPlane build() {
      return new MockDataPlane(this);
    }

    public Builder setBgpTopology(ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology) {
      _bgpTopology = bgpTopology;
      return this;
    }

    public Builder setIpOwners(Map<Ip, Set<String>> owners) {
      _ipOwners = owners;
      return this;
    }

    public Builder setForwardingAnalysis(ForwardingAnalysis forwardingAnalysis) {
      _forwardingAnalysis = forwardingAnalysis;
      return this;
    }

    public Builder setRibs(
        SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> ribs) {
      _ribs = ribs;
      return this;
    }

    public Builder setTopology(Topology topology) {
      _topology = topology;
      return this;
    }

    public Builder setTopologyEdges(SortedSet<Edge> topologyEdges) {
      _topologyEdges = topologyEdges;
      return this;
    }
  }

  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  private Table<String, String, Set<BgpRoute>> _bgpRoutes;

  private final ValueGraph<BgpPeerConfigId, BgpSessionProperties> _bgpTopology;

  private final Map<String, Configuration> _configurations;

  private final Map<String, Map<String, Fib>> _fibs;

  private final ForwardingAnalysis _forwardingAnalysis;

  private final Map<Ip, Map<String, Set<String>>> _ipVrfOwners;

  private final SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>>
      _ribs;

  @Nullable private final Topology _topology;

  private final SortedSet<Edge> _topologyEdges;

  private MockDataPlane(Builder builder) {
    _bgpTopology = builder._bgpTopology;
    _configurations = builder._configurations;
    _fibs = builder._fibs;
    _forwardingAnalysis = builder._forwardingAnalysis;
    _ipVrfOwners = builder._ipVrfOwners;
    _ribs = ImmutableSortedMap.copyOf(builder._ribs);
    _topology = builder._topology;
    _topologyEdges = ImmutableSortedSet.copyOf(builder._topologyEdges);
  }

  @Override
  public Table<String, String, Set<BgpRoute>> getBgpRoutes(boolean multipath) {
    return _bgpRoutes;
  }

  @Override
  public ValueGraph<BgpPeerConfigId, BgpSessionProperties> getBgpTopology() {
    return _bgpTopology;
  }

  @Override
  public Map<String, Map<String, Fib>> getFibs() {
    return _fibs;
  }

  @Override
  public ForwardingAnalysis getForwardingAnalysis() {
    return _forwardingAnalysis;
  }

  @Override
  public Map<Ip, Map<String, Set<String>>> getIpVrfOwners() {
    return _ipVrfOwners;
  }

  @Override
  public SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> getRibs() {
    return _ribs;
  }

  @Override
  @Nullable
  public Topology getTopology() {
    return _topology;
  }

  @Override
  public SortedSet<Edge> getTopologyEdges() {
    return _topologyEdges;
  }

  @Override
  public Map<String, Configuration> getConfigurations() {
    return _configurations;
  }

  @Override
  public SortedMap<String, SortedMap<String, Map<Prefix, Map<String, Set<String>>>>>
      getPrefixTracingInfoSummary() {
    return ImmutableSortedMap.of();
  }
}

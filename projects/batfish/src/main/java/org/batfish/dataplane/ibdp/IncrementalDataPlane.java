package org.batfish.dataplane.ibdp;

import static org.batfish.common.util.CommonUtil.toImmutableMap;
import static org.batfish.common.util.CommonUtil.toImmutableSortedMap;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Fib;
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.ForwardingAnalysisImpl;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Topology;

public final class IncrementalDataPlane implements Serializable, DataPlane {

  private static final long serialVersionUID = 1L;

  public static class Builder {

    private Map<Ip, Map<String, Set<String>>> _ipVrfOwners;
    private Map<String, Node> _nodes;
    private Topology _layer3Topology;

    public Builder setIpVrfOwners(Map<Ip, Map<String, Set<String>>> ipVrfOwners) {
      _ipVrfOwners = ImmutableMap.copyOf(ipVrfOwners);
      return this;
    }

    public Builder setNodes(Map<String, Node> nodes) {
      _nodes = ImmutableMap.copyOf(nodes);
      return this;
    }

    public Builder setLayer3Topology(Topology layer3Topology) {
      _layer3Topology = layer3Topology;
      return this;
    }

    public IncrementalDataPlane build() {
      return new IncrementalDataPlane(this);
    }
  }

  private final class ConfigurationsSupplier
      implements Serializable, Supplier<Map<String, Configuration>> {

    private static final long serialVersionUID = 1L;

    @Override
    public Map<String, Configuration> get() {
      return computeConfigurations();
    }
  }

  private final class FibsSupplier
      implements Serializable, Supplier<Map<String, Map<String, Fib>>> {

    private static final long serialVersionUID = 1L;

    @Override
    public Map<String, Map<String, Fib>> get() {
      return computeFibs();
    }
  }

  private final class ForwardingAnalysisSupplier
      implements Serializable, Supplier<ForwardingAnalysis> {

    private static final long serialVersionUID = 1L;

    @Override
    public ForwardingAnalysis get() {
      return computeForwardingAnalysis();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private final Supplier<Map<String, Configuration>> _configurations =
      Suppliers.memoize(new ConfigurationsSupplier());

  private final Supplier<Map<String, Map<String, Fib>>> _fibs =
      Suppliers.memoize(new FibsSupplier());

  private final Supplier<ForwardingAnalysis> _forwardingAnalysis =
      Suppliers.memoize(new ForwardingAnalysisSupplier());

  private final Map<Ip, Map<String, Set<String>>> _ipVrfOwners;

  private final Map<String, Node> _nodes;

  private final Topology _layer3Topology;

  private transient SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>>
      _ribs;

  @Nonnull private final Table<String, String, Set<Bgpv4Route>> _bgpRoutes;

  private IncrementalDataPlane(Builder builder) {
    _ipVrfOwners = builder._ipVrfOwners;
    _nodes = builder._nodes;
    _layer3Topology = builder._layer3Topology;
    _bgpRoutes = computeBgpRoutes();
  }

  private Map<String, Configuration> computeConfigurations() {
    return _nodes.entrySet().stream()
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getConfiguration()));
  }

  private Map<String, Map<String, Fib>> computeFibs() {
    return toImmutableMap(
        _nodes,
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue().getVirtualRouters(),
                Entry::getKey,
                vrfEntry -> vrfEntry.getValue().getFib()));
  }

  private ForwardingAnalysis computeForwardingAnalysis() {
    return new ForwardingAnalysisImpl(getConfigurations(), getFibs(), _layer3Topology);
  }

  private SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>>
      computeRibs() {
    return toImmutableSortedMap(
        _nodes,
        Entry::getKey,
        nodeEntry ->
            toImmutableSortedMap(
                nodeEntry.getValue().getVirtualRouters(),
                Entry::getKey,
                vrfEntry -> vrfEntry.getValue().getMainRib()));
  }

  @Nonnull
  @Override
  public Table<String, String, Set<Bgpv4Route>> getBgpRoutes(boolean multipath) {
    return _bgpRoutes;
  }

  @Nonnull
  private Table<String, String, Set<Bgpv4Route>> computeBgpRoutes() {
    Table<String, String, Set<Bgpv4Route>> table = TreeBasedTable.create();

    _nodes.forEach(
        (hostname, node) ->
            node.getVirtualRouters()
                .forEach(
                    (vrfName, vr) -> {
                      table.put(hostname, vrfName, vr.getBgpRoutes());
                    }));
    return table;
  }

  @Override
  public Map<String, Configuration> getConfigurations() {
    return _configurations.get();
  }

  @Override
  public Map<String, Map<String, Fib>> getFibs() {
    return _fibs.get();
  }

  @Override
  public ForwardingAnalysis getForwardingAnalysis() {
    return _forwardingAnalysis.get();
  }

  @Override
  public Map<Ip, Map<String, Set<String>>> getIpVrfOwners() {
    return _ipVrfOwners;
  }

  public Map<String, Node> getNodes() {
    return _nodes;
  }

  /**
   * Retrieve the {@link PrefixTracer} for each {@link VirtualRouter} after dataplane computation.
   * Map structure: Hostname -&gt; VRF name -&gt; prefix tracer.
   */
  public SortedMap<String, SortedMap<String, PrefixTracer>> getPrefixTracingInfo() {
    /*
     * Iterate over nodes, then virtual routers, and extract prefix tracer from each.
     * Sort hostnames and VRF names
     */
    return toImmutableSortedMap(
        _nodes,
        Entry::getKey,
        nodeEntry ->
            toImmutableSortedMap(
                nodeEntry.getValue().getVirtualRouters(),
                Entry::getKey,
                vrfEntry -> vrfEntry.getValue().getPrefixTracer()));
  }

  @Override
  public SortedMap<String, SortedMap<String, Map<Prefix, Map<String, Set<String>>>>>
      getPrefixTracingInfoSummary() {
    /*
     * Iterate over nodes, then virtual routers, and extract prefix tracer from each.
     * Sort hostnames and VRF names
     */
    return toImmutableSortedMap(
        _nodes,
        Entry::getKey,
        nodeEntry ->
            toImmutableSortedMap(
                nodeEntry.getValue().getVirtualRouters(),
                Entry::getKey,
                vrfEntry -> vrfEntry.getValue().getPrefixTracer().summarize()));
  }

  @Override
  public SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>> getRibs() {
    if (_ribs == null) {
      _ribs = computeRibs();
    }
    return _ribs;
  }
}

package org.batfish.dataplane.ibdp;

import static java.util.Comparator.naturalOrder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.Network;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Fib;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Topology;

public class IncrementalDataPlane implements Serializable, DataPlane {

  private static final long serialVersionUID = 1L;

  private transient Network<BgpNeighbor, BgpSession> _bgpTopology;

  private Map<Ip, Set<String>> _ipOwners;

  private Map<Ip, String> _ipOwnersSimple;

  Map<String, Node> _nodes;

  private Topology _topology;

  @Override
  public Map<String, Map<String, Fib>> getFibs() {
    return _nodes
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                eNode ->
                    eNode
                        .getValue()
                        .getVirtualRouters()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableMap.toImmutableMap(
                                Entry::getKey, eVr -> eVr.getValue().getFib()))));
  }

  @Override
  public Map<Ip, Set<String>> getIpOwners() {
    return _ipOwners;
  }

  public Map<Ip, String> getIpOwnersSimple() {
    return _ipOwnersSimple;
  }

  public Map<String, Node> getNodes() {
    return _nodes;
  }

  @Override
  public SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> getRibs() {
    ImmutableSortedMap.Builder<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        new ImmutableSortedMap.Builder<>(naturalOrder());
    _nodes.forEach(
        (hostname, node) -> {
          ImmutableSortedMap.Builder<String, GenericRib<AbstractRoute>> byVrf =
              new ImmutableSortedMap.Builder<>(naturalOrder());
          node.getVirtualRouters()
              .forEach(
                  (vrf, virtualRouter) -> {
                    GenericRib<AbstractRoute> rib = virtualRouter.getMainRib();
                    byVrf.put(vrf, rib);
                  });
          ribs.put(hostname, byVrf.build());
        });
    return ribs.build();
  }

  @Override
  public Topology getTopology() {
    return _topology;
  }

  @Override
  public SortedSet<Edge> getTopologyEdges() {
    return _topology.getEdges();
  }

  protected void initIpOwners(
      Map<String, Configuration> configurations,
      Map<Ip, Set<String>> ipOwners,
      Map<Ip, String> ipOwnersSimple) {
    setIpOwners(ipOwners);
    setIpOwnersSimple(ipOwnersSimple);
  }

  public void setIpOwners(Map<Ip, Set<String>> ipOwners) {
    _ipOwners = ipOwners;
  }

  public void setIpOwnersSimple(Map<Ip, String> ipOwnersSimple) {
    _ipOwnersSimple = ipOwnersSimple;
  }

  public void setNodes(Map<String, Node> nodes) {
    _nodes = nodes;
  }

  public void setTopology(Topology topology) {
    _topology = topology;
  }

  @Override
  public Network<BgpNeighbor, BgpSession> getBgpTopology() {
    return _bgpTopology;
  }

  @Override
  public Map<String, Configuration> getConfigurations() {
    return _nodes
        .entrySet()
        .stream()
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getConfiguration()));
  }

  void setBgpTopology(Network<BgpNeighbor, BgpSession> bgpTopology) {
    this._bgpTopology = bgpTopology;
  }
}

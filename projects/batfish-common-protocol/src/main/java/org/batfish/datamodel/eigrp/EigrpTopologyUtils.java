package org.batfish.datamodel.eigrp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;

/** Utility functions for computing EIGRP topology and neighbors */
public class EigrpTopologyUtils {

  /** Initialize the EIGRP topology as a directed graph. */
  public static @Nonnull EigrpTopology initEigrpTopology(
      Map<String, Configuration> configurations, Topology topology) {
    Set<EigrpEdge> edges =
        topology.getEdges().stream()
            .map(edge -> EigrpEdge.edgeIfAdjacent(edge, configurations))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(ImmutableSet.toImmutableSet());
    MutableNetwork<EigrpNeighborConfigId, EigrpEdge> graph =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();
    ImmutableSet.Builder<EigrpNeighborConfigId> nodes = ImmutableSet.builder();
    edges.forEach(
        edge -> {
          nodes.add(edge.getNode1());
          nodes.add(edge.getNode2());
        });
    nodes.build().forEach(graph::addNode);
    edges.forEach(edge -> graph.addEdge(edge.getNode1(), edge.getNode2(), edge));
    return new EigrpTopology(graph);
  }

  /**
   * Examine {@link Configuration} objects to compute and generate IDs for all available EIGRP
   * neighbors on all EIGRP processes; initialize {@link EigrpProcess} map containing all local
   * neighbor configs, obtainable with {@link EigrpProcess#getNeighbors()} ()}.
   */
  public static void initNeighborConfigs(NetworkConfigurations configurations) {
    for (Configuration config : configurations.all()) {
      for (Vrf vrf : config.getVrfs().values()) {
        for (EigrpProcess proc : vrf.getEigrpProcesses().values()) {
          Builder<String, EigrpNeighborConfig> neighborMap = ImmutableMap.builder();
          for (Interface iface : vrf.getInterfaces().values()) {
            if (iface.getConcreteAddress() == null
                || iface.getEigrp() == null
                || !iface.getEigrp().getEnabled()
                || iface.getEigrp().getAsn() != proc.getAsn()) {
              continue;
            }
            // TODO: check if secondary addresses also participate in EIGRP neighbor relationships
            neighborMap.put(
                iface.getName(),
                EigrpNeighborConfig.builder()
                    .setHostname(config.getHostname())
                    .setInterfaceName(iface.getName())
                    .setIp(iface.getConcreteAddress().getIp())
                    .setVrfName(vrf.getName())
                    .setPassive(iface.getEigrp().getPassive())
                    .build());
          }
          proc.setNeighbors(neighborMap.build());
        }
      }
    }
  }
}

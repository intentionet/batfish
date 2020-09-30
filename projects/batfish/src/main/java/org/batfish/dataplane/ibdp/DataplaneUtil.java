package org.batfish.dataplane.ibdp;

import static org.batfish.common.util.CollectionUtil.toImmutableMap;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.specifier.LocationInfoUtils.computeLocationInfo;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AnnotatedRoute;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EvpnRoute;
import org.batfish.datamodel.Fib;
import org.batfish.datamodel.ForwardingAnalysis;
import org.batfish.datamodel.ForwardingAnalysisImpl;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.vxlan.Layer2Vni;

/** Utility functions to convert dataplane {@link Node} into other stuctures */
public class DataplaneUtil {

  static Map<String, Configuration> computeConfigurations(Map<String, Node> nodes) {
    return nodes.entrySet().stream()
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> e.getValue().getConfiguration()));
  }

  static Map<String, Map<String, Fib>> computeFibs(Map<String, Node> nodes) {
    return toImmutableMap(
        nodes,
        Entry::getKey,
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue().getVirtualRouters(),
                Entry::getKey,
                vrfEntry -> vrfEntry.getValue().getFib()));
  }

  static ForwardingAnalysis computeForwardingAnalysis(
      Map<String, Map<String, Fib>> fibs,
      Map<String, Configuration> configs,
      Topology layer3Topology) {
    return new ForwardingAnalysisImpl(configs, fibs, layer3Topology, computeLocationInfo(configs));
  }

  static SortedMap<String, SortedMap<String, GenericRib<AnnotatedRoute<AbstractRoute>>>>
      computeRibs(Map<String, Node> nodes) {
    return toImmutableSortedMap(
        nodes,
        Entry::getKey,
        nodeEntry ->
            toImmutableSortedMap(
                nodeEntry.getValue().getVirtualRouters(),
                Entry::getKey,
                vrfEntry -> vrfEntry.getValue().getMainRib()));
  }

  @Nonnull
  static Table<String, String, Set<Bgpv4Route>> computeBgpRoutes(Map<String, Node> nodes) {
    Table<String, String, Set<Bgpv4Route>> table = HashBasedTable.create();

    nodes.forEach(
        (hostname, node) ->
            node.getVirtualRouters()
                .forEach(
                    (vrfName, vr) -> {
                      table.put(hostname, vrfName, vr.getBgpRoutes());
                    }));
    return table;
  }

  @Nonnull
  static Table<String, String, Set<EvpnRoute<?, ?>>> computeEvpnRoutes(Map<String, Node> nodes) {
    Table<String, String, Set<EvpnRoute<?, ?>>> table = HashBasedTable.create();
    nodes.forEach(
        (hostname, node) ->
            node.getVirtualRouters()
                .forEach(
                    (vrfName, vr) -> {
                      table.put(hostname, vrfName, vr.getEvpnRoutes());
                    }));
    return table;
  }

  @Nonnull
  static Table<String, String, Set<Layer2Vni>> computeVniSettings(Map<String, Node> nodes) {
    Table<String, String, Set<Layer2Vni>> result = HashBasedTable.create();
    for (Node node : nodes.values()) {
      for (Entry<String, VirtualRouter> vr : node.getVirtualRouters().entrySet()) {
        result.put(
            node.getConfiguration().getHostname(), vr.getKey(), vr.getValue().getLayer2Vnis());
      }
    }
    return result;
  }
}

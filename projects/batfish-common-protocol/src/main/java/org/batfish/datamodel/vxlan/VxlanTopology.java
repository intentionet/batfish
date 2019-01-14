package org.batfish.datamodel.vxlan;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableNetwork;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.VniSettings;
import org.batfish.datamodel.Vrf;

@ParametersAreNonnullByDefault
public final class VxlanTopology {

  public VxlanTopology(Map<String, Configuration> configurations) {
    MutableNetwork<VxlanNode, VxlanEdge> graph =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();
    Map<Integer, List<Vrf>> vrfsByVni = new HashMap<>();
    Map<Vrf, String> vrfHostnames = new IdentityHashMap<>();
    for (Configuration c : configurations.values()) {
      for (Vrf v : c.getVrfs().values()) {
        vrfHostnames.put(v, c.getHostname());
        for (VniSettings vniSettings : v.getVniSettings().values()) {
          vrfsByVni.computeIfAbsent(vniSettings.getVni(), vni -> new ArrayList<>()).add(v);
        }
      }
    }
    vrfsByVni.forEach(
        (vni, vrfs) -> {
          for (Vrf v1 : vrfs) {
            VniSettings vs1 = v1.getVniSettings().get(vni);
            vrfs.stream()
                .filter(v2 -> v1 != v2)
                .forEach(
                    v2 -> {
                      VniSettings vs2 = v2.getVniSettings().get(vni);
                      if (vs1.getBumTransportMethod() == vs2.getBumTransportMethod()
                          && vs1.getUdpPort() != null
                          && vs1.getUdpPort().equals(vs2.getUdpPort())
                          && vs1.getSourceAddress() != null
                          && vs2.getSourceAddress() != null
                          && vs1.getVlan() != null
                          && vs2.getVlan() != null
                          && !vs1.getSourceAddress().equals(vs2.getSourceAddress())
                          && ((vs1.getBumTransportMethod() == BumTransportMethod.MULTICAST_GROUP
                                  && vs1.getBumTransportIps().equals(vs2.getBumTransportIps()))
                              || (vs1.getBumTransportMethod()
                                      == BumTransportMethod.UNICAST_FLOOD_GROUP
                                  && vs1.getBumTransportIps().contains(vs2.getSourceAddress())
                                  && vs2.getBumTransportIps().contains(vs1.getSourceAddress())))) {
                        VxlanNode node1 =
                            VxlanNode.builder()
                                .setHostname(vrfHostnames.get(v1))
                                .setSourceAddress(vs1.getSourceAddress())
                                .setVlan(vs1.getVlan())
                                .setVrf(v1.getName())
                                .build();
                        VxlanNode node2 =
                            VxlanNode.builder()
                                .setHostname(vrfHostnames.get(v2))
                                .setSourceAddress(vs2.getSourceAddress())
                                .setVlan(vs2.getVlan())
                                .setVrf(v2.getName())
                                .build();
                        VxlanEdge edge =
                            VxlanEdge.builder()
                                .setMulticastGroup(
                                    vs1.getBumTransportMethod()
                                            == BumTransportMethod.MULTICAST_GROUP
                                        ? vs1.getBumTransportIps().first()
                                        : null)
                                .setNode1(node1)
                                .setNode2(node2)
                                .setUdpPort(vs1.getUdpPort())
                                .setVni(vni)
                                .build();
                        graph.addEdge(node1, node2, edge);
                      }
                    });
          }
        });
    _graph = ImmutableNetwork.copyOf(graph);
  }

  private final ImmutableNetwork<VxlanNode, VxlanEdge> _graph;

  public VxlanTopology(@Nonnull Iterable<VxlanEdge> edges) {
    MutableNetwork<VxlanNode, VxlanEdge> graph =
        NetworkBuilder.directed().allowsParallelEdges(false).allowsSelfLoops(false).build();
    edges.forEach(
        edge -> {
          graph.addNode(edge.getTail());
          graph.addNode(edge.getHead());
          graph.addEdge(edge.getTail(), edge.getHead(), edge);
        });
    _graph = ImmutableNetwork.copyOf(graph);
  }

  public Set<VxlanEdge> getEdges() {
    return ImmutableSet.copyOf(_graph.edges());
  }
}

package org.batfish.dataplane.ibdp;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.NetworkBuilder;
import com.google.common.graph.ValueGraphBuilder;
import com.google.common.testing.EqualsTester;
import java.util.Optional;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Node;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.TunnelTopology;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.IpsecPeerConfigId;
import org.batfish.datamodel.IpsecSession;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.eigrp.EigrpEdge;
import org.batfish.datamodel.eigrp.EigrpNeighborConfigId;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.ipsec.IpsecTopology;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisNode;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfNeighborConfigId;
import org.batfish.datamodel.ospf.OspfSessionProperties;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.vxlan.VxlanNode;
import org.batfish.datamodel.vxlan.VxlanTopology;
import org.junit.Test;

/** Test of {@link TopologyContext}. */
public final class TopologyContextTest {

  @Test
  public void testEquals() {
    TopologyContext.Builder builder = TopologyContext.builder();
    TopologyContext base = builder.build();
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ValueGraphBuilder.directed().build();
    bgpTopology.addNode(new BgpPeerConfigId("a", "b", "c"));
    MutableValueGraph<IpsecPeerConfigId, IpsecSession> ipsecTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    ipsecTopology.putEdgeValue(
        new IpsecPeerConfigId("a", "b"),
        new IpsecPeerConfigId("c", "d"),
        IpsecSession.builder().build());
    MutableNetwork<EigrpNeighborConfigId, EigrpEdge> eigrpTopology =
        NetworkBuilder.directed().build();
    eigrpTopology.addNode(new EigrpNeighborConfigId("a", "b", "c"));
    MutableNetwork<IsisNode, IsisEdge> isisTopology = NetworkBuilder.directed().build();
    isisTopology.addNode(new IsisNode("a", "b"));
    MutableValueGraph<OspfNeighborConfigId, OspfSessionProperties> ospfTopology =
        ValueGraphBuilder.directed().build();
    ospfTopology.addNode(new OspfNeighborConfigId("a", "b", "c", "d"));
    MutableGraph<VxlanNode> vxlanTopology =
        GraphBuilder.undirected().allowsSelfLoops(false).build();
    vxlanTopology.addNode(new VxlanNode("a", 5));

    new EqualsTester()
        .addEqualityGroup(new Object())
        .addEqualityGroup(base, base, builder.build())
        .addEqualityGroup(builder.setBgpTopology(new BgpTopology(bgpTopology)).build())
        .addEqualityGroup(builder.setEigrpTopology(new EigrpTopology(eigrpTopology)).build())
        .addEqualityGroup(builder.setIpsecTopology(new IpsecTopology(ipsecTopology)).build())
        .addEqualityGroup(builder.setIsisTopology(new IsisTopology(isisTopology)).build())
        .addEqualityGroup(
            builder
                .setLayer1LogicalTopology(
                    Optional.of(
                        new Layer1Topology(ImmutableList.of(new Layer1Edge("a", "b", "c", "d")))))
                .build())
        .addEqualityGroup(
            builder
                .setLayer2Topology(
                    Optional.of(
                        Layer2Topology.fromDomains(
                            ImmutableList.of(ImmutableSet.of(new Layer2Node("a", "b", 5))))))
                .build())
        .addEqualityGroup(
            builder
                .setLayer3Topology(new Topology(ImmutableSortedSet.of(Edge.of("a", "b", "c", "d"))))
                .build())
        .addEqualityGroup(builder.setOspfTopology(new OspfTopology(ospfTopology)).build())
        .addEqualityGroup(
            builder
                .setRawLayer1PhysicalTopology(
                    Optional.of(
                        new Layer1Topology(ImmutableList.of(new Layer1Edge("a", "b", "c", "d")))))
                .build())
        .addEqualityGroup(
            builder
                .setTunnelTopology(
                    TunnelTopology.builder()
                        .add(new NodeInterfacePair("n1", "i1"), new NodeInterfacePair("n2", "i2"))
                        .build())
                .build())
        .addEqualityGroup(builder.setVxlanTopology(new VxlanTopology(vxlanTopology)).build())
        .testEquals();
  }

  @Test
  public void testToBuilder() {
    TopologyContext.Builder builder = TopologyContext.builder();
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ValueGraphBuilder.directed().build();
    bgpTopology.addNode(new BgpPeerConfigId("a", "b", "c"));
    MutableNetwork<EigrpNeighborConfigId, EigrpEdge> eigrpTopology =
        NetworkBuilder.directed().build();
    eigrpTopology.addNode(new EigrpNeighborConfigId("a", "b", "c"));
    MutableValueGraph<IpsecPeerConfigId, IpsecSession> ipsecTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    ipsecTopology.putEdgeValue(
        new IpsecPeerConfigId("a", "b"),
        new IpsecPeerConfigId("c", "d"),
        IpsecSession.builder().build());
    MutableNetwork<IsisNode, IsisEdge> isisTopology = NetworkBuilder.directed().build();
    isisTopology.addNode(new IsisNode("a", "b"));
    MutableValueGraph<OspfNeighborConfigId, OspfSessionProperties> ospfTopology =
        ValueGraphBuilder.directed().build();
    ospfTopology.addNode(new OspfNeighborConfigId("a", "b", "c", "d"));
    MutableGraph<VxlanNode> vxlanTopology =
        GraphBuilder.undirected().allowsSelfLoops(false).build();
    vxlanTopology.addNode(new VxlanNode("a", 5));
    builder
        .setBgpTopology(new BgpTopology(bgpTopology))
        .setEigrpTopology(new EigrpTopology(eigrpTopology))
        .setIsisTopology(new IsisTopology(isisTopology))
        .setIpsecTopology(new IpsecTopology(ipsecTopology))
        .setLayer1LogicalTopology(
            Optional.of(new Layer1Topology(ImmutableList.of(new Layer1Edge("a", "b", "c", "d")))))
        .setLayer2Topology(
            Optional.of(
                Layer2Topology.fromDomains(
                    ImmutableList.of(ImmutableSet.of(new Layer2Node("a", "b", 5))))))
        .setLayer3Topology(new Topology(ImmutableSortedSet.of(Edge.of("a", "b", "c", "d"))))
        .setOspfTopology(new OspfTopology(ospfTopology))
        .setRawLayer1PhysicalTopology(
            Optional.of(new Layer1Topology(ImmutableList.of(new Layer1Edge("a", "b", "c", "d")))))
        .setTunnelTopology(
            TunnelTopology.builder()
                .add(new NodeInterfacePair("n1", "i1"), new NodeInterfacePair("n2", "i2"))
                .build())
        .setVxlanTopology(new VxlanTopology(vxlanTopology));

    assertThat(builder.build(), equalTo(builder.build().toBuilder().build()));
  }
}

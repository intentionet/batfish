package org.batfish.datamodel;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.testing.EqualsTester;
import java.io.IOException;
import java.util.Set;
import java.util.SortedSet;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.junit.Test;

public class TopologyTest {

  // Set up two edges with 3 nodes with 1 interface each:
  // n1:i1 -> n2:i2
  // n2:i2 -> n3:i3
  NodeInterfacePair _nip1 = NodeInterfacePair.of("n1", "i1");
  NodeInterfacePair _nip2 = NodeInterfacePair.of("n2", "i2");
  NodeInterfacePair _nip3 = NodeInterfacePair.of("n3", "i3");
  Set<Edge> _edge1to2Set = ImmutableSet.of(new Edge(_nip1, _nip2));
  Set<Edge> _edge2to3Set = ImmutableSet.of(new Edge(_nip2, _nip3));
  SortedSet<Edge> _bothEdges =
      ImmutableSortedSet.of(new Edge(_nip1, _nip2), new Edge(_nip2, _nip3));

  @Test
  public void testEmptyTopology() {
    SortedSet<Edge> edges = ImmutableSortedSet.of();
    Topology topo = new Topology(edges);

    assertThat(topo.getEdges(), equalTo(ImmutableSet.of()));
    assertThat(topo.getNodeEdges(), equalTo(ImmutableMap.of()));
    assertThat(
        topo.getNeighbors(NodeInterfacePair.of("node", "iface")), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testOneEdge() {
    NodeInterfacePair nip1 = NodeInterfacePair.of("n1", "i1");
    NodeInterfacePair nip2 = NodeInterfacePair.of("n2", "i2");
    Edge edge = new Edge(nip1, nip2);
    SortedSet<Edge> edges = ImmutableSortedSet.of(edge);
    Topology topo = new Topology(edges);

    assertThat(topo.getEdges(), equalTo(edges));
    assertThat(topo.getNodeEdges(), equalTo(ImmutableMap.of("n1", edges, "n2", edges)));
    assertThat(topo.getNeighbors(nip1), equalTo(ImmutableSet.of(nip2)));
    assertThat(topo.getNeighbors(nip2), equalTo(ImmutableSet.of()));
    assertThat(topo.getNeighbors(NodeInterfacePair.of("x", "y")), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testTwoEdges() {
    Topology topo = new Topology(_bothEdges);
    assertThat(topo.getEdges(), equalTo(_bothEdges));
    assertThat(
        topo.getNodeEdges(),
        equalTo(ImmutableMap.of("n1", _edge1to2Set, "n2", _bothEdges, "n3", _edge2to3Set)));
    assertThat(topo.getNeighbors(_nip1), equalTo(ImmutableSet.of(_nip2)));
    assertThat(topo.getNeighbors(_nip2), equalTo(ImmutableSet.of(_nip3)));
    assertThat(topo.getNeighbors(_nip3), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testPruneEdge() {
    Topology topo =
        new Topology(_bothEdges).prune(_edge1to2Set, ImmutableSet.of(), ImmutableSet.of());
    assertThat(topo.getEdges(), equalTo(_edge2to3Set));
    assertThat(
        topo.getNodeEdges(), equalTo(ImmutableMap.of("n2", _edge2to3Set, "n3", _edge2to3Set)));
    assertThat(topo.getNeighbors(_nip1), equalTo(ImmutableSet.of()));
    assertThat(topo.getNeighbors(_nip2), equalTo(ImmutableSet.of(_nip3)));
    assertThat(topo.getNeighbors(_nip3), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testPruneNode1() {
    Topology topo =
        new Topology(_bothEdges).prune(ImmutableSet.of(), ImmutableSet.of("n1"), ImmutableSet.of());
    assertThat(topo.getEdges(), equalTo(_edge2to3Set));
    assertThat(
        topo.getNodeEdges(), equalTo(ImmutableMap.of("n2", _edge2to3Set, "n3", _edge2to3Set)));
    assertThat(topo.getNeighbors(_nip2), equalTo(ImmutableSet.of(_nip3)));
    assertThat(topo.getNeighbors(_nip3), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testPruneNode2() {
    Topology topo =
        new Topology(_bothEdges).prune(ImmutableSet.of(), ImmutableSet.of("n2"), ImmutableSet.of());
    assertThat(topo.getEdges(), equalTo(ImmutableSet.of()));
    assertThat(topo.getNodeEdges(), equalTo(ImmutableMap.of()));
    assertThat(topo.getNeighbors(_nip1), equalTo(ImmutableSet.of()));
    assertThat(topo.getNeighbors(_nip3), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testPruneNode3() {
    Topology topo =
        new Topology(_bothEdges).prune(ImmutableSet.of(), ImmutableSet.of("n3"), ImmutableSet.of());
    assertThat(topo.getEdges(), equalTo(_edge1to2Set));
    assertThat(
        topo.getNodeEdges(), equalTo(ImmutableMap.of("n1", _edge1to2Set, "n2", _edge1to2Set)));
    assertThat(topo.getNeighbors(_nip1), equalTo(ImmutableSet.of(_nip2)));
    assertThat(topo.getNeighbors(_nip2), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testPruneInterface1() {
    Topology topo =
        new Topology(_bothEdges)
            .prune(ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(_nip1));
    assertThat(topo.getEdges(), equalTo(_edge2to3Set));
    assertThat(
        topo.getNodeEdges(), equalTo(ImmutableMap.of("n2", _edge2to3Set, "n3", _edge2to3Set)));
    assertThat(topo.getNeighbors(_nip2), equalTo(ImmutableSet.of(_nip3)));
    assertThat(topo.getNeighbors(_nip3), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testPruneInterface2() {
    Topology topo =
        new Topology(_bothEdges)
            .prune(ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(_nip2));
    assertThat(topo.getEdges(), equalTo(ImmutableSet.of()));
    assertThat(topo.getNodeEdges(), equalTo(ImmutableMap.of()));
    assertThat(topo.getNeighbors(_nip1), equalTo(ImmutableSet.of()));
    assertThat(topo.getNeighbors(_nip3), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testPruneInterface3() {
    Topology topo =
        new Topology(_bothEdges)
            .prune(ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(_nip3));
    assertThat(topo.getEdges(), equalTo(_edge1to2Set));
    assertThat(
        topo.getNodeEdges(), equalTo(ImmutableMap.of("n1", _edge1to2Set, "n2", _edge1to2Set)));
    assertThat(topo.getNeighbors(_nip1), equalTo(ImmutableSet.of(_nip2)));
    assertThat(topo.getNeighbors(_nip2), equalTo(ImmutableSet.of()));
  }

  @Test
  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(new Object())
        .addEqualityGroup(Topology.EMPTY, Topology.EMPTY, new Topology(ImmutableSortedSet.of()))
        .addEqualityGroup(new Topology(_bothEdges))
        .testEquals();
  }

  @Test
  public void testJacksonSerialization() throws IOException {
    Topology topo = new Topology(_bothEdges);
    assertEquals(BatfishObjectMapper.clone(topo, Topology.class), topo);
  }
}

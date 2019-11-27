package org.batfish.dataplane.ibdp.schedule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.graph.EndpointPair;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.ospf.OspfTopology.EdgeId;
import org.batfish.dataplane.ibdp.Node;
import org.batfish.dataplane.ibdp.TopologyContext;
import org.jgrapht.Graph;
import org.jgrapht.alg.color.GreedyColoring;
import org.jgrapht.alg.color.RandomGreedyColoring;
import org.jgrapht.alg.color.SaturationDegreeColoring;
import org.jgrapht.alg.interfaces.VertexColoringAlgorithm;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class NodeColoredSchedule extends IbdpSchedule {

  public enum Coloring {
    GREEDY,
    RANDOM,
    SATURATION
  }

  private final Iterator<Set<String>> _iterator;
  private Graph<String, DefaultEdge> _graph;

  /**
   * Create a new schedule based on existing nodes and topology
   *
   * @param nodes all nodes in the network
   * @param topologyContext the various network topologies
   */
  public NodeColoredSchedule(
      Map<String, Node> nodes, Coloring algorithm, TopologyContext topologyContext) {
    super(nodes);
    makeGraph(nodes, topologyContext);

    // Color the graph
    VertexColoringAlgorithm<String> coloringAlg = getColoringAlgorithmInstance(algorithm, _graph);
    VertexColoringAlgorithm.Coloring<String> coloring = coloringAlg.getColoring();
    List<Set<String>> colorClasses = ImmutableList.copyOf(coloring.getColorClasses());
    _iterator = colorClasses.iterator();
  }

  /**
   * Get a new instance of the coloring algorithm based
   *
   * @param type a {@link Coloring} type
   * @param graph the graph to color. Will be converted to a undirected graph
   * @return a new instance of {@link VertexColoringAlgorithm}
   */
  private static VertexColoringAlgorithm<String> getColoringAlgorithmInstance(
      Coloring type, Graph<String, DefaultEdge> graph) {
    AsUndirectedGraph<String, DefaultEdge> g = new AsUndirectedGraph<>(graph);
    switch (type) {
      case GREEDY:
        return new GreedyColoring<>(g);
      case RANDOM:
        return new RandomGreedyColoring<>(g);
      case SATURATION:
      default:
        return new SaturationDegreeColoring<>(g);
    }
  }

  /**
   * Create a graph for coloring purposes, and color it.
   *
   * @param nodes all nodes in the network
   * @param topologyContext the various network topologies
   */
  @SuppressWarnings("deprecation")
  private void makeGraph(Map<String, Node> nodes, TopologyContext topologyContext) {
    /*
     * For the purposes of coloring, two nodes are adjacent if:
     * - They have established a BGP session
     * - They have an OSPF adjacency
     */

    // Add all nodes first
    _graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    nodes.keySet().forEach(n -> _graph.addVertex(n));

    // Process BGP connections
    for (EndpointPair<BgpPeerConfigId> edge : topologyContext.getBgpTopology().getGraph().edges()) {
      _graph.addEdge(edge.source().getHostname(), edge.target().getHostname());
    }
    // Process OSPF edges
    for (EdgeId edge : topologyContext.getOspfTopology().edges()) {
      _graph.addEdge(edge.getTail().getHostname(), edge.getHead().getHostname());
    }
  }

  /**
   * Checks if unprocessed nodes are available in this schedule
   *
   * @return true if more unprocessed nodes are available
   */
  @Override
  public boolean hasNext() {
    return _iterator.hasNext();
  }

  /**
   * Get the next set of nodes that are allowed to be run in parallel during a dataplane iteration
   *
   * @return a map of nodes keyed by name, containing a subset of all network nodes
   */
  @Override
  public Map<String, Node> next() {
    Set<String> nodeNames = _iterator.next();
    return Maps.filterKeys(_nodes, nodeNames::contains);
  }
}

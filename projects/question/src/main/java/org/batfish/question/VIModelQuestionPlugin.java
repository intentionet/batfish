package org.batfish.question;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Network;
import com.google.common.graph.ValueGraph;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.topology.IpOwners;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.Layer3Edge;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.BgpTopologyUtils;
import org.batfish.datamodel.collections.IpEdge;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.collections.VerboseBgpEdge;
import org.batfish.datamodel.collections.VerboseEigrpEdge;
import org.batfish.datamodel.eigrp.EigrpEdge;
import org.batfish.datamodel.eigrp.EigrpNeighborConfigId;
import org.batfish.datamodel.eigrp.EigrpTopologyUtils;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class VIModelQuestionPlugin extends QuestionPlugin {

  private static final Comparator<VerboseBgpEdge> VERBOSE_BGP_EDGE_COMPARATOR =
      Comparator.nullsFirst(
          Comparator.comparing(VerboseBgpEdge::getEdgeSummary)
              .thenComparing(VerboseBgpEdge::getSession1Id)
              .thenComparing(VerboseBgpEdge::getSession2Id));

  public static class VIModelAnswerElement extends AnswerElement {

    private static final class Edges {
      private static final String PROP_BGP = "bgp";
      private static final String PROP_ISIS = "isis";
      private static final String PROP_LAYER1 = "layer1";
      private static final String PROP_LAYER3 = "layer3";
      private static final String PROP_EIGRP = "eigrp";
      private static final String PROP_OSPF = "ospf";

      private SortedSet<VerboseBgpEdge> _bgpEdges;

      private SortedSet<VerboseEigrpEdge> _eigrpEdges;

      private SortedSet<IsisEdge> _isisEdges;

      private Layer1Topology _layer1Edges;

      private SortedSet<Layer3Edge> _layer3Edges;

      private SortedSet<OspfTopology.EdgeId> _ospfEdges;

      private static Edges createEmpty() {
        return new Edges(null, null, null, null, null, null);
      }

      @JsonCreator
      private Edges(
          @JsonProperty(PROP_BGP) Set<VerboseBgpEdge> bgpEdges,
          @JsonProperty(PROP_EIGRP) SortedSet<VerboseEigrpEdge> eigrpEdges,
          @JsonProperty(PROP_ISIS) SortedSet<IsisEdge> isisEdges,
          @JsonProperty(PROP_LAYER1) Layer1Topology layer1Edges,
          @JsonProperty(PROP_LAYER3) SortedSet<Layer3Edge> layer3Edges,
          @JsonProperty(PROP_OSPF) SortedSet<OspfTopology.EdgeId> ospfEdges) {
        _bgpEdges = new TreeSet<>(VERBOSE_BGP_EDGE_COMPARATOR);
        _bgpEdges.addAll(firstNonNull(bgpEdges, ImmutableSet.of()));
        _eigrpEdges = firstNonNull(eigrpEdges, ImmutableSortedSet.of());
        _isisEdges = firstNonNull(isisEdges, ImmutableSortedSet.of());
        _layer1Edges = layer1Edges;
        _layer3Edges = firstNonNull(layer3Edges, ImmutableSortedSet.of());
        _ospfEdges = firstNonNull(ospfEdges, ImmutableSortedSet.of());
      }

      @JsonProperty(PROP_BGP)
      public @Nonnull SortedSet<VerboseBgpEdge> getBgpEdges() {
        return _bgpEdges;
      }

      @JsonProperty(PROP_EIGRP)
      public @Nonnull SortedSet<VerboseEigrpEdge> getEigrpEdges() {
        return _eigrpEdges;
      }

      @JsonProperty(PROP_ISIS)
      public @Nonnull SortedSet<IsisEdge> getIsisEdges() {
        return _isisEdges;
      }

      @JsonProperty(PROP_LAYER1)
      public @Nullable Layer1Topology getLayer1Edges() {
        return _layer1Edges;
      }

      @JsonProperty(PROP_LAYER3)
      public @Nonnull SortedSet<Layer3Edge> getLayer3Edges() {
        return _layer3Edges;
      }

      @JsonProperty(PROP_OSPF)
      public @Nonnull SortedSet<OspfTopology.EdgeId> getOspfEdges() {
        return _ospfEdges;
      }
    }

    private static final String PROP_NODES = "nodes";
    private static final String PROP_EDGES = "edges";

    private final SortedMap<String, Configuration> _nodes;

    private final Edges _edges;

    @JsonCreator
    private VIModelAnswerElement(
        @JsonProperty(PROP_NODES) SortedMap<String, Configuration> nodes,
        @JsonProperty(PROP_EDGES) Edges edges) {
      _nodes = firstNonNull(nodes, ImmutableSortedMap.of());
      _edges = firstNonNull(edges, Edges.createEmpty());
    }

    public VIModelAnswerElement(
        @Nonnull SortedMap<String, Configuration> nodes,
        Set<VerboseBgpEdge> bgp,
        SortedSet<VerboseEigrpEdge> eigrp,
        SortedSet<IsisEdge> isis,
        Layer1Topology layer1,
        SortedSet<Layer3Edge> layer3,
        SortedSet<OspfTopology.EdgeId> ospf) {
      this._nodes = nodes;
      this._edges = new Edges(bgp, eigrp, isis, layer1, layer3, ospf);
    }

    @JsonProperty(PROP_NODES)
    public @Nonnull SortedMap<String, Configuration> getNodes() {
      return _nodes;
    }

    @JsonProperty(PROP_EDGES)
    public @Nonnull Edges getEdges() {
      return _edges;
    }
  }

  public static class VIModelAnswerer extends Answerer {

    public VIModelAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public VIModelAnswerElement answer() {
      SortedMap<String, Configuration> configs = _batfish.loadConfigurations();
      Topology topology =
          _batfish.getTopologyProvider().getInitialLayer3Topology(_batfish.getNetworkSnapshot());
      Map<Ip, Set<String>> ipOwners = IpOwners.computeIpNodeOwners(configs, true);
      Layer2Topology layer2Topology =
          _batfish
              .getTopologyProvider()
              .getInitialLayer2Topology(_batfish.getNetworkSnapshot())
              .orElse(null);

      return new VIModelAnswerElement(
          configs,
          getBgpEdges(configs, ipOwners, layer2Topology),
          getEigrpEdges(configs, topology),
          getIsisEdges(configs, topology),
          _batfish
              .getTopologyProvider()
              .getLayer1PhysicalTopology(_batfish.getNetworkSnapshot())
              .orElse(null),
          getLayer3Edges(configs, topology),
          getOspfEdges(
              _batfish
                  .getTopologyProvider()
                  .getInitialOspfTopology(_batfish.getNetworkSnapshot())));
    }

    private static SortedSet<VerboseBgpEdge> getBgpEdges(
        Map<String, Configuration> configs,
        Map<Ip, Set<String>> ipOwners,
        Layer2Topology layer2Topology) {
      BgpTopology bgpTopology =
          BgpTopologyUtils.initBgpTopology(configs, ipOwners, false, false, null, layer2Topology);
      return getBgpEdges(bgpTopology.getGraph(), NetworkConfigurations.of(configs));
    }

    @VisibleForTesting
    static SortedSet<VerboseBgpEdge> getBgpEdges(
        ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology, NetworkConfigurations nc) {
      SortedSet<VerboseBgpEdge> bgpEdges = new TreeSet<>(VERBOSE_BGP_EDGE_COMPARATOR);
      for (EndpointPair<BgpPeerConfigId> session : bgpTopology.edges()) {
        BgpPeerConfigId bgpPeerConfigId = session.source();
        BgpPeerConfigId remoteBgpPeerConfigId = session.target();
        BgpSessionProperties sessionProperties =
            bgpTopology.edgeValue(bgpPeerConfigId, remoteBgpPeerConfigId).orElse(null);
        assert sessionProperties != null; // condition of the edge existing
        String hostname = bgpPeerConfigId.getHostname();
        String remoteHostname = remoteBgpPeerConfigId.getHostname();
        BgpPeerConfig bgpPeerConfig = nc.getBgpPeerConfig(bgpPeerConfigId);
        BgpPeerConfig remoteBgpPeerConfig = nc.getBgpPeerConfig(remoteBgpPeerConfigId);

        if (bgpPeerConfig != null && remoteBgpPeerConfig != null) {
          Ip localIp = sessionProperties.getTailIp();
          Ip remoteIp = sessionProperties.getHeadIp();
          IpEdge ipEdge = new IpEdge(hostname, localIp, remoteHostname, remoteIp);
          bgpEdges.add(
              new VerboseBgpEdge(
                  bgpPeerConfig,
                  remoteBgpPeerConfig,
                  bgpPeerConfigId,
                  remoteBgpPeerConfigId,
                  ipEdge));
        }
      }
      return bgpEdges;
    }

    private static SortedSet<VerboseEigrpEdge> getEigrpEdges(
        Map<String, Configuration> configs, Topology topology) {
      Network<EigrpNeighborConfigId, EigrpEdge> eigrpTopology =
          EigrpTopologyUtils.initEigrpTopology(configs, topology).getNetwork();
      NetworkConfigurations nc = NetworkConfigurations.of(configs);
      SortedSet<VerboseEigrpEdge> eigrpEdges = new TreeSet<>();
      for (Configuration c : configs.values()) {
        String hostname = c.getHostname();
        for (Vrf vrf : c.getVrfs().values()) {
          eigrpEdges.addAll(
              vrf.getInterfaceNames().stream()
                  .map(ifaceName -> new EigrpNeighborConfigId(hostname, ifaceName, vrf.getName()))
                  .filter(eigrpTopology.nodes()::contains)
                  .flatMap(n -> eigrpTopology.inEdges(n).stream())
                  .map(edge -> new VerboseEigrpEdge(edge, edge.toIpEdge(nc)))
                  .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
        }
      }
      return eigrpEdges;
    }

    private static SortedSet<IsisEdge> getIsisEdges(
        Map<String, Configuration> configs, Topology topology) {
      return IsisTopology.initIsisTopology(configs, topology).getNetwork().edges().stream()
          .filter(Objects::nonNull)
          .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
    }

    private static SortedSet<Layer3Edge> getLayer3Edges(
        Map<String, Configuration> configs, Topology topology) {
      SortedSet<Layer3Edge> layer3Edges = new TreeSet<>();
      for (Edge edge : topology.getEdges()) {
        Interface i1 = configs.get(edge.getNode1()).getAllInterfaces().get(edge.getInt1());
        Interface i2 = configs.get(edge.getNode2()).getAllInterfaces().get(edge.getInt2());
        layer3Edges.add(
            new Layer3Edge(
                new NodeInterfacePair(edge.getNode1(), edge.getInt1()),
                new NodeInterfacePair(edge.getNode2(), edge.getInt2()),
                ImmutableSortedSet.copyOf(i1.getAllConcreteAddresses()),
                ImmutableSortedSet.copyOf(i2.getAllConcreteAddresses())));
      }
      return layer3Edges;
    }

    private static SortedSet<OspfTopology.EdgeId> getOspfEdges(OspfTopology topology) {
      return ImmutableSortedSet.copyOf(Ordering.natural(), topology.edges());
    }
  }

  public static class VIModelQuestion extends Question {
    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "viModel";
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new VIModelAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new VIModelQuestion();
  }
}

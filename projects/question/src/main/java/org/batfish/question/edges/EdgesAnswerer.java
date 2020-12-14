package org.batfish.question.edges;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Range;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ValueGraph;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Edge;
import org.batfish.common.topology.TopologyProvider;
import org.batfish.common.util.IpsecUtil;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpPeerConfigId.BgpPeerConfigType;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPeerConfigId;
import org.batfish.datamodel.IpsecSession;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.eigrp.EigrpEdge;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.eigrp.EigrpTopologyUtils;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfNeighborConfigId;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.VxlanNode;
import org.batfish.datamodel.vxlan.VxlanTopology;
import org.batfish.question.edges.EdgesQuestion.EdgeType;

public class EdgesAnswerer extends Answerer {

  static final String COL_INTERFACE = "Interface";
  static final String COL_REMOTE_INTERFACE = "Remote_Interface";
  static final String COL_IPS = "IPs";
  static final String COL_REMOTE_IPS = "Remote_IPs";

  // BGP only
  static final String COL_NODE = "Node";
  static final String COL_REMOTE_NODE = "Remote_Node";
  static final String COL_AS_NUMBER = "AS_Number";
  static final String COL_REMOTE_AS_NUMBER = "Remote_AS_Number";
  static final String COL_IP = "IP";
  static final String COL_REMOTE_IP = "Remote_IP";

  // VXLAN only
  static final String COL_MULTICAST_GROUP = "Multicast_Group";
  static final String COL_REMOTE_VTEP_ADDRESS = "Remote_VTEP_Address";
  static final String COL_UDP_PORT = "UDP_Port";
  static final String COL_VNI = "VNI";
  static final String COL_VTEP_ADDRESS = "VTEP_Address";

  // Layer 2
  static final String COL_VLAN = "VLAN";
  static final String COL_REMOTE_VLAN = "Remote_VLAN";

  // IPsec only
  static final String COL_SOURCE_INTERFACE = "Source_Interface";
  static final String COL_TUNNEL_INTERFACE = "Tunnel_Interface";
  static final String COL_REMOTE_SOURCE_INTERFACE = "Remote_Source_Interface";
  static final String COL_REMOTE_TUNNEL_INTERFACE = "Remote_Tunnel_Interface";

  EdgesAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer(NetworkSnapshot snapshot) {
    EdgesQuestion question = (EdgesQuestion) _question;

    Map<String, Configuration> configurations = _batfish.loadConfigurations(snapshot);
    Set<String> includeNodes =
        question.getNodeSpecifier().resolve(_batfish.specifierContext(snapshot));
    Set<String> includeRemoteNodes =
        question.getRemoteNodeSpecifier().resolve(_batfish.specifierContext(snapshot));

    TableAnswerElement answer = new TableAnswerElement(getTableMetadata(question.getEdgeType()));
    TopologyProvider topologyProvider = _batfish.getTopologyProvider();
    Topology topology =
        question.getInitial()
            ? topologyProvider.getInitialLayer3Topology(snapshot)
            : topologyProvider.getLayer3Topology(snapshot);
    answer.postProcessAnswer(
        _question,
        generateRows(
            configurations,
            snapshot,
            topology,
            includeNodes,
            includeRemoteNodes,
            question.getEdgeType(),
            question.getInitial()));
    return answer;
  }

  private Collection<Row> generateRows(
      Map<String, Configuration> configurations,
      NetworkSnapshot snapshot,
      Topology topology,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      EdgeType edgeType,
      boolean initial) {
    TopologyProvider topologyProvider = _batfish.getTopologyProvider();
    switch (edgeType) {
      case BGP:
        BgpTopology bgpTopology = topologyProvider.getBgpTopology(snapshot);
        return getBgpEdges(configurations, includeNodes, includeRemoteNodes, bgpTopology);
      case EIGRP:
        EigrpTopology eigrpTopology =
            EigrpTopologyUtils.initEigrpTopology(configurations, topology);
        return getEigrpEdges(includeNodes, includeRemoteNodes, eigrpTopology);
      case IPSEC:
        ValueGraph<IpsecPeerConfigId, IpsecSession> ipsecTopology =
            IpsecUtil.initIpsecTopology(configurations).getGraph();
        return getIpsecEdges(ipsecTopology, configurations);
      case ISIS:
        IsisTopology isisTopology = IsisTopology.initIsisTopology(configurations, topology);
        return getIsisEdges(includeNodes, includeRemoteNodes, isisTopology);
      case OSPF:
        return getOspfEdges(
            includeNodes,
            includeRemoteNodes,
            initial
                ? topologyProvider.getInitialOspfTopology(snapshot)
                : topologyProvider.getOspfTopology(snapshot));
      case VXLAN:
        VxlanTopology vxlanTopology =
            initial
                ? topologyProvider.getInitialVxlanTopology(snapshot)
                : topologyProvider.getVxlanTopology(snapshot);
        return getVxlanEdges(
            NetworkConfigurations.of(configurations),
            includeNodes,
            includeRemoteNodes,
            vxlanTopology);
      case LAYER1:
        return topologyProvider
            .getLayer1LogicalTopology(snapshot)
            .map(
                layer1LogicalTopology ->
                    getLayer1Edges(includeNodes, includeRemoteNodes, layer1LogicalTopology))
            .orElse(ImmutableMultiset.of());
      case LAYER3:
      default:
        return getLayer3Edges(configurations, includeNodes, includeRemoteNodes, topology);
    }
  }

  @VisibleForTesting
  static Multiset<Row> getEigrpEdges(
      Set<String> includeNodes, Set<String> includeRemoteNodes, EigrpTopology eigrpTopology) {
    return eigrpTopology.getNetwork().edges().stream()
        .filter(
            eigrpEdge ->
                includeNodes.contains(eigrpEdge.getNode1().getHostname())
                    && includeRemoteNodes.contains(eigrpEdge.getNode2().getHostname()))
        .map(EdgesAnswerer::eigrpEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  @VisibleForTesting
  static Multiset<Row> getVxlanEdges(
      NetworkConfigurations nc,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      VxlanTopology vxlanTopology) {
    return vxlanTopology.getGraph().edges().stream()
        .flatMap(edge -> vxlanEdgeToRows(nc, includeNodes, includeRemoteNodes, edge))
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  @VisibleForTesting
  static Multiset<Row> getBgpEdges(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      BgpTopology bgpTopology) {
    Multiset<Row> rows = HashMultiset.create();
    Map<String, ColumnMetadata> columnMap = getTableMetadata(EdgeType.BGP).toColumnMap();
    for (EndpointPair<BgpPeerConfigId> session : bgpTopology.getGraph().edges()) {
      BgpPeerConfigId bgpPeerConfigId = session.source();
      BgpPeerConfigId remoteBgpPeerConfigId = session.target();
      NetworkConfigurations nc = NetworkConfigurations.of(configurations);
      BgpPeerConfig bgpPeerConfig = nc.getBgpPeerConfig(bgpPeerConfigId);
      BgpPeerConfig remoteBgpPeerConfig = nc.getBgpPeerConfig(remoteBgpPeerConfigId);
      if (bgpPeerConfig == null || remoteBgpPeerConfig == null) {
        continue;
      }
      String hostname = bgpPeerConfigId.getHostname();
      String remoteHostname = remoteBgpPeerConfigId.getHostname();
      if (includeNodes.contains(hostname) && includeRemoteNodes.contains(remoteHostname)) {
        BgpSessionProperties sessionProperties =
            bgpTopology.getGraph().edgeValue(bgpPeerConfigId, remoteBgpPeerConfigId).orElse(null);
        assert sessionProperties != null;
        // Leave IPs null for BGP unnumbered session
        boolean unnumbered = bgpPeerConfigId.getType() == BgpPeerConfigType.UNNUMBERED;
        rows.add(
            Row.builder(columnMap)
                .put(COL_NODE, new Node(hostname))
                .put(COL_IP, unnumbered ? null : sessionProperties.getTailIp())
                .put(COL_INTERFACE, bgpPeerConfigId.getPeerInterface())
                .put(COL_AS_NUMBER, sessionProperties.getTailAs())
                .put(COL_REMOTE_NODE, new Node(remoteHostname))
                .put(COL_REMOTE_IP, unnumbered ? null : sessionProperties.getHeadIp())
                .put(COL_REMOTE_INTERFACE, remoteBgpPeerConfigId.getPeerInterface())
                .put(COL_REMOTE_AS_NUMBER, sessionProperties.getHeadAs())
                .build());
      }
    }

    return rows;
  }

  @VisibleForTesting
  static Multiset<Row> getIsisEdges(
      Set<String> includeNodes, Set<String> includeRemoteNodes, IsisTopology isisTopology) {
    return isisTopology.getNetwork().edges().stream()
        .filter(Objects::nonNull)
        .filter(
            isisEdge ->
                includeNodes.contains(isisEdge.getNode1().getNode())
                    && includeRemoteNodes.contains(isisEdge.getNode2().getNode()))
        .map(EdgesAnswerer::isisEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  @VisibleForTesting
  static Multiset<Row> getLayer1Edges(
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      @Nullable Layer1Topology layer1Topology) {
    if (layer1Topology == null) {
      return HashMultiset.create();
    }
    return layer1Topology.getGraph().edges().stream()
        .filter(
            layer1Edge ->
                includeNodes.contains(layer1Edge.getNode1().getHostname())
                    && includeRemoteNodes.contains(layer1Edge.getNode2().getHostname()))
        .map(EdgesAnswerer::layer1EdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  @VisibleForTesting
  static Multiset<Row> getLayer3Edges(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      Topology topology) {
    return topology.getEdges().stream()
        .filter(
            layer3Edge ->
                includeNodes.contains(layer3Edge.getNode1())
                    && includeRemoteNodes.contains(layer3Edge.getNode2()))
        .map(layer3edge -> layer3EdgeToRow(configurations, layer3edge))
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  @VisibleForTesting
  static List<Row> getOspfEdges(
      Set<String> includeNodes, Set<String> includeRemoteNodes, OspfTopology topology) {
    ImmutableSet.Builder<Row> rows = ImmutableSet.builder();
    topology
        .getGraph()
        .edges()
        .forEach(
            p -> {
              OspfNeighborConfigId sourceId = p.source();
              OspfNeighborConfigId targetId = p.target();
              String hostname = sourceId.getHostname();
              String remoteHostname = targetId.getHostname();
              if (!includeNodes.contains(hostname)
                  || !includeRemoteNodes.contains(remoteHostname)) {
                return;
              }
              rows.add(
                  getOspfEdgeRow(
                      hostname,
                      sourceId.getInterfaceName(),
                      remoteHostname,
                      targetId.getInterfaceName()));
            });
    return ImmutableList.copyOf(rows.build());
  }

  @VisibleForTesting
  static Multiset<Row> getIpsecEdges(
      ValueGraph<IpsecPeerConfigId, IpsecSession> ipsecTopology,
      Map<String, Configuration> configurations) {
    NetworkConfigurations nf = NetworkConfigurations.of(configurations);
    Multiset<Row> rows = HashMultiset.create();
    ipsecTopology.edges().stream()
        .filter(
            // only considering endpoints with established IPsec session
            endpoint -> {
              Optional<IpsecSession> ipsecSession =
                  ipsecTopology.edgeValue(endpoint.nodeU(), endpoint.nodeV());
              return ipsecSession.isPresent()
                  && ipsecSession.get().getNegotiatedIpsecP2Proposal() != null;
            })
        .forEach(
            endpoint -> {
              IpsecPeerConfig ipsecPeerConfigU = nf.getIpsecPeerConfig(endpoint.nodeU());
              IpsecPeerConfig ipsecPeerConfigV = nf.getIpsecPeerConfig(endpoint.nodeV());
              if (ipsecPeerConfigU == null || ipsecPeerConfigV == null) {
                return;
              }
              rows.add(
                  getIpsecEdge(
                      endpoint.nodeU().getHostName(),
                      ipsecPeerConfigU,
                      endpoint.nodeV().getHostName(),
                      ipsecPeerConfigV));
            });
    return rows;
  }

  private static Row getIpsecEdge(
      String nodeU,
      IpsecPeerConfig ipsecPeerConfigU,
      String nodeV,
      IpsecPeerConfig ipsecPeerConfigV) {
    RowBuilder row = Row.builder();
    row.put(
            COL_SOURCE_INTERFACE,
            NodeInterfacePair.of(nodeU, ipsecPeerConfigU.getSourceInterface()))
        .put(
            COL_TUNNEL_INTERFACE,
            ipsecPeerConfigU.getTunnelInterface() == null
                ? null
                : NodeInterfacePair.of(nodeU, ipsecPeerConfigU.getTunnelInterface()))
        .put(
            COL_REMOTE_SOURCE_INTERFACE,
            NodeInterfacePair.of(nodeV, ipsecPeerConfigV.getSourceInterface()))
        .put(
            COL_REMOTE_TUNNEL_INTERFACE,
            ipsecPeerConfigV.getTunnelInterface() == null
                ? null
                : NodeInterfacePair.of(nodeV, ipsecPeerConfigV.getTunnelInterface()));
    return row.build();
  }

  @VisibleForTesting
  static Row eigrpEdgeToRow(EigrpEdge eigrpEdge) {
    RowBuilder row = Row.builder();
    row.put(
            COL_INTERFACE,
            NodeInterfacePair.of(
                eigrpEdge.getNode1().getHostname(), eigrpEdge.getNode1().getInterfaceName()))
        .put(
            COL_REMOTE_INTERFACE,
            NodeInterfacePair.of(
                eigrpEdge.getNode2().getHostname(), eigrpEdge.getNode2().getInterfaceName()));
    return row.build();
  }

  @VisibleForTesting
  static Stream<Row> vxlanEdgeToRows(
      NetworkConfigurations nc,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      EndpointPair<VxlanNode> edge) {
    VxlanNode node1 = edge.nodeU();
    VxlanNode node2 = edge.nodeV();
    String h1 = node1.getHostname();
    String h2 = node2.getHostname();
    Stream.Builder<Row> builder = Stream.builder();
    if (includeNodes.contains(h1) && includeRemoteNodes.contains(h2)) {
      builder.add(vxlanEdgeToRow(nc, node1, node2));
    }
    if (includeNodes.contains(h2) && includeRemoteNodes.contains(h1)) {
      builder.add(vxlanEdgeToRow(nc, node2, node1));
    }
    return builder.build();
  }

  @VisibleForTesting
  static Row vxlanEdgeToRow(NetworkConfigurations nc, VxlanNode node, VxlanNode remoteNode) {
    Layer2Vni node1Settings = nc.getVniSettings(node.getHostname(), node.getVni()).get();
    Layer2Vni node2Settings =
        nc.getVniSettings(remoteNode.getHostname(), remoteNode.getVni()).get();
    RowBuilder row = Row.builder();
    row.put(COL_VNI, node.getVni())
        .put(COL_NODE, new Node(node.getHostname()))
        .put(COL_REMOTE_NODE, new Node(remoteNode.getHostname()))
        .put(COL_VTEP_ADDRESS, node1Settings.getSourceAddress())
        .put(COL_REMOTE_VTEP_ADDRESS, node2Settings.getSourceAddress())
        .put(COL_VLAN, node1Settings.getVlan())
        .put(COL_REMOTE_VLAN, node2Settings.getVlan())
        .put(COL_UDP_PORT, node1Settings.getUdpPort())
        .put(COL_MULTICAST_GROUP, node1Settings.getMulticastGroup());
    return row.build();
  }

  static Row isisEdgeToRow(IsisEdge isisEdge) {
    RowBuilder row = Row.builder();
    row.put(
            COL_INTERFACE,
            NodeInterfacePair.of(
                isisEdge.getNode1().getNode(), isisEdge.getNode1().getInterfaceName()))
        .put(
            COL_REMOTE_INTERFACE,
            NodeInterfacePair.of(
                isisEdge.getNode2().getNode(), isisEdge.getNode2().getInterfaceName()));
    return row.build();
  }

  @VisibleForTesting
  static Row layer1EdgeToRow(Layer1Edge layer1Edge) {
    RowBuilder row = Row.builder();
    row.put(
            COL_INTERFACE,
            NodeInterfacePair.of(
                layer1Edge.getNode1().getHostname(), layer1Edge.getNode1().getInterfaceName()))
        .put(
            COL_REMOTE_INTERFACE,
            NodeInterfacePair.of(
                layer1Edge.getNode2().getHostname(), layer1Edge.getNode2().getInterfaceName()));

    return row.build();
  }

  @VisibleForTesting
  static Row layer2EdgeToRow(Layer2Edge layer2Edge) {
    RowBuilder row = Row.builder();
    Range<Integer> vlan1 = layer2Edge.getNode1().getSwitchportVlanRange();
    Range<Integer> vlan2 = layer2Edge.getNode2().getSwitchportVlanRange();
    row.put(
            COL_INTERFACE,
            NodeInterfacePair.of(
                layer2Edge.getNode1().getHostname(), layer2Edge.getNode1().getInterfaceName()))
        // Use integer space to pretty-stringify the range
        .put(COL_VLAN, vlan1 == null ? null : IntegerSpace.of(vlan1).toString())
        .put(
            COL_REMOTE_INTERFACE,
            NodeInterfacePair.of(
                layer2Edge.getNode2().getHostname(), layer2Edge.getNode2().getInterfaceName()))
        .put(COL_REMOTE_VLAN, vlan2 == null ? null : IntegerSpace.of(vlan2).toString());

    return row.build();
  }

  @VisibleForTesting
  static Row layer3EdgeToRow(Map<String, Configuration> configurations, Edge edge) {
    Interface interface1 =
        configurations.get(edge.getNode1()).getAllInterfaces().get(edge.getInt1());
    Interface interface2 =
        configurations.get(edge.getNode2()).getAllInterfaces().get(edge.getInt2());
    Set<Ip> ips1 =
        interface1.getAllConcreteAddresses().stream()
            .filter(Objects::nonNull)
            .map(ConcreteInterfaceAddress::getIp)
            .collect(Collectors.toSet());
    Set<Ip> ips2 =
        interface2.getAllConcreteAddresses().stream()
            .filter(Objects::nonNull)
            .map(ConcreteInterfaceAddress::getIp)
            .collect(Collectors.toSet());

    RowBuilder row = Row.builder();
    row.put(COL_INTERFACE, NodeInterfacePair.of(edge.getNode1(), edge.getInt1()))
        .put(COL_IPS, ips1)
        .put(COL_REMOTE_INTERFACE, NodeInterfacePair.of(edge.getNode2(), edge.getInt2()))
        .put(COL_REMOTE_IPS, ips2);

    return row.build();
  }

  @VisibleForTesting
  static Row getOspfEdgeRow(String node, String iface, String remoteNode, String remoteIface) {
    RowBuilder row = Row.builder();
    row.put(COL_INTERFACE, NodeInterfacePair.of(node, iface))
        .put(COL_REMOTE_INTERFACE, NodeInterfacePair.of(remoteNode, remoteIface));
    return row.build();
  }

  static Row getRipEdgeRow(String node, String iface, String remoteNode, String remoteIface) {
    RowBuilder row = Row.builder();
    row.put(COL_INTERFACE, NodeInterfacePair.of(node, iface))
        .put(COL_REMOTE_INTERFACE, NodeInterfacePair.of(remoteNode, remoteIface));
    return row.build();
  }

  /** Generate the table metadata based on the type of edge requested */
  @VisibleForTesting
  static TableMetadata getTableMetadata(EdgeType edgeType) {
    ImmutableList.Builder<ColumnMetadata> columnBuilder = ImmutableList.builder();
    switch (edgeType) {
      case LAYER3:
        columnBuilder.add(
            new ColumnMetadata(
                COL_INTERFACE,
                Schema.INTERFACE,
                "Interface from which the edge originates",
                Boolean.TRUE,
                Boolean.FALSE));
        columnBuilder.add(
            new ColumnMetadata(COL_IPS, Schema.set(Schema.IP), "IPs", Boolean.FALSE, Boolean.TRUE));

        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_INTERFACE,
                Schema.INTERFACE,
                "Interface at which the edge terminates",
                Boolean.TRUE,
                Boolean.FALSE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_IPS, Schema.set(Schema.IP), "Remote IPs", Boolean.FALSE, Boolean.TRUE));
        break;

      case BGP:
        columnBuilder.add(
            new ColumnMetadata(
                COL_NODE,
                Schema.NODE,
                "Node from which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_IP, Schema.IP, "IP at the side of originator", Boolean.FALSE, Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_INTERFACE,
                Schema.STRING,
                "Interface at which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_AS_NUMBER,
                Schema.STRING,
                "AS Number at the side of originator",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_NODE,
                Schema.NODE,
                "Node at which the edge terminates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_IP,
                Schema.IP,
                "IP at the side of the responder",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_INTERFACE,
                Schema.STRING,
                "Interface at which the edge terminates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_AS_NUMBER,
                Schema.STRING,
                "AS Number at the side of responder",
                Boolean.FALSE,
                Boolean.TRUE));
        break;
      case VXLAN:
        columnBuilder.add(
            new ColumnMetadata(
                COL_VNI,
                Schema.INTEGER,
                "VNI of the VXLAN tunnel edge",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_NODE,
                Schema.NODE,
                "Node from which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_NODE,
                Schema.NODE,
                "Node at which the edge terminates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_VTEP_ADDRESS,
                Schema.IP,
                "VTEP IP of node from which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_VTEP_ADDRESS,
                Schema.IP,
                "VTEP IP of node at which the edge terminates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_VLAN,
                Schema.INTEGER,
                "VLAN associated with VNI on node from which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_VLAN,
                Schema.INTEGER,
                "VLAN associated with VNI on node at which the edge terminates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_UDP_PORT,
                Schema.INTEGER,
                "UDP port of the VXLAN tunnel transport",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_MULTICAST_GROUP,
                Schema.IP,
                "Multicast group of the VXLAN tunnel transport",
                Boolean.FALSE,
                Boolean.TRUE));
        break;
      case IPSEC:
        columnBuilder.add(
            new ColumnMetadata(
                COL_SOURCE_INTERFACE,
                Schema.INTERFACE,
                "Source interface used in the IPsec session",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_TUNNEL_INTERFACE,
                Schema.INTERFACE,
                "Tunnel interface (if any) used in the IPsec session",
                Boolean.FALSE,
                Boolean.TRUE));

        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_SOURCE_INTERFACE,
                Schema.INTERFACE,
                "Remote source interface used in the IPsec session",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_TUNNEL_INTERFACE,
                Schema.INTERFACE,
                "Remote tunnel interface (if any) used in the IPsec session",
                Boolean.FALSE,
                Boolean.TRUE));
        break;
      case OSPF:
      case ISIS:
      case EIGRP:
      case LAYER1:
      default:
        columnBuilder.add(
            new ColumnMetadata(
                COL_INTERFACE,
                Schema.INTERFACE,
                "Interface from which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));

        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_INTERFACE,
                Schema.INTERFACE,
                "Interface at which the edge terminates",
                Boolean.FALSE,
                Boolean.TRUE));
    }
    return new TableMetadata(columnBuilder.build(), "Display Edges");
  }
}

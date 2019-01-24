package org.batfish.question.edges;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Network;
import com.google.common.graph.ValueGraph;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Edge;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.common.util.IpsecUtil;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.EdgeType;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPeerConfigId;
import org.batfish.datamodel.IpsecSession;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.RipNeighbor;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.bgp.BgpTopologyUtils;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.eigrp.EigrpEdge;
import org.batfish.datamodel.eigrp.EigrpInterface;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisNode;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfNeighbor;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.OspfTopologyUtils;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.datamodel.vxlan.VxlanEdge;
import org.batfish.datamodel.vxlan.VxlanTopology;

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
  public AnswerElement answer() {
    EdgesQuestion question = (EdgesQuestion) _question;

    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Set<String> includeNodes = question.getNodes().getMatchingNodes(_batfish);
    Set<String> includeRemoteNodes = question.getRemoteNodes().getMatchingNodes(_batfish);

    TableAnswerElement answer = new TableAnswerElement(getTableMetadata(question.getEdgeType()));
    Topology topology = _batfish.getEnvironmentTopology();
    answer.postProcessAnswer(
        _question,
        generateRows(
            configurations, topology, includeNodes, includeRemoteNodes, question.getEdgeType()));
    return answer;
  }

  private Multiset<Row> generateRows(
      Map<String, Configuration> configurations,
      Topology topology,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      EdgeType edgeType) {
    Map<Ip, Set<String>> ipOwners = TopologyUtil.computeIpNodeOwners(configurations, true);
    switch (edgeType) {
      case BGP:
        ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
            BgpTopologyUtils.initBgpTopology(configurations, ipOwners, false, false, null);
        return getBgpEdges(configurations, includeNodes, includeRemoteNodes, bgpTopology);
      case EIGRP:
        Network<EigrpInterface, EigrpEdge> eigrpTopology =
            EigrpTopology.initEigrpTopology(configurations, topology);
        return getEigrpEdges(includeNodes, includeRemoteNodes, eigrpTopology);
      case IPSEC:
        ValueGraph<IpsecPeerConfigId, IpsecSession> ipsecTopology =
            IpsecUtil.initIpsecTopology(configurations);
        return getIpsecEdges(ipsecTopology, configurations);
      case ISIS:
        Network<IsisNode, IsisEdge> isisTopology =
            IsisTopology.initIsisTopology(configurations, topology);
        return getIsisEdges(includeNodes, includeRemoteNodes, isisTopology);
      case OSPF:
        return getOspfEdges(configurations, includeNodes, includeRemoteNodes, topology);
      case RIP:
        _batfish.initRemoteRipNeighbors(configurations, ipOwners, topology);
        return getRipEdges(configurations, includeNodes, includeRemoteNodes);
      case VXLAN:
        VxlanTopology vxlanTopology =
            _batfish.getTopologyProvider().getVxlanTopology(_batfish.getNetworkSnapshot());
        return getVxlanEdges(includeNodes, includeRemoteNodes, vxlanTopology);
      case LAYER1:
        Layer1Topology layer1LogicalTopology =
            _batfish.getTopologyProvider().getLayer1LogicalTopology(_batfish.getNetworkSnapshot());
        return getLayer1Edges(includeNodes, includeRemoteNodes, layer1LogicalTopology);
      case LAYER2:
        // Unsupported until we decide how to present layer2 topology.
        return ImmutableMultiset.of();
      case LAYER3:
      default:
        return getLayer3Edges(configurations, includeNodes, includeRemoteNodes, topology);
    }
  }

  @VisibleForTesting
  static Multiset<Row> getEigrpEdges(
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      Network<EigrpInterface, EigrpEdge> eigrpTopology) {

    return eigrpTopology.edges().stream()
        .filter(
            eigrpEdge ->
                includeNodes.contains(eigrpEdge.getNode1().getHostname())
                    && includeRemoteNodes.contains(eigrpEdge.getNode2().getHostname()))
        .map(EdgesAnswerer::eigrpEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  @VisibleForTesting
  static Multiset<Row> getVxlanEdges(
      Set<String> includeNodes, Set<String> includeRemoteNodes, VxlanTopology vxlanTopology) {
    return vxlanTopology.getEdges().stream()
        .filter(
            edge ->
                includeNodes.contains(edge.getTail().getHostname())
                    && includeRemoteNodes.contains(edge.getHead().getHostname()))
        .map(EdgesAnswerer::vxlanEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  @VisibleForTesting
  static Multiset<Row> getBgpEdges(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology) {
    Multiset<Row> rows = HashMultiset.create();
    for (EndpointPair<BgpPeerConfigId> session : bgpTopology.edges()) {
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
        rows.add(
            getBgpEdgeRow(
                hostname,
                bgpPeerConfig.getLocalIp(),
                bgpPeerConfig.getLocalAs(),
                remoteHostname,
                remoteBgpPeerConfig.getLocalIp(),
                remoteBgpPeerConfig.getLocalAs()));
      }
    }

    return rows;
  }

  @VisibleForTesting
  static Multiset<Row> getIsisEdges(
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      Network<IsisNode, IsisEdge> isisTopology) {
    return isisTopology.edges().stream()
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
  static Multiset<Row> getOspfEdges(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      Topology topology) {
    Multiset<Row> rows = HashMultiset.create();
    OspfTopologyUtils.initRemoteOspfNeighbors(configurations, topology);
    for (Configuration c : configurations.values()) {
      String hostname = c.getHostname();
      for (Vrf vrf : c.getVrfs().values()) {
        OspfProcess proc = vrf.getOspfProcess();
        if (proc != null) {
          for (OspfNeighbor ospfNeighbor : proc.getOspfNeighbors().values()) {
            OspfNeighbor remoteOspfNeighbor = ospfNeighbor.getRemoteOspfNeighbor();
            if (remoteOspfNeighbor != null) {
              Configuration remoteHost = remoteOspfNeighbor.getOwner();
              String remoteHostname = remoteHost.getHostname();
              if (includeNodes.contains(hostname) && includeRemoteNodes.contains(remoteHostname)) {
                rows.add(
                    getOspfEdgeRow(
                        hostname,
                        ospfNeighbor.getIface().getName(),
                        remoteHostname,
                        remoteOspfNeighbor.getIface().getName()));
              }
            }
          }
        }
      }
    }
    return rows;
  }

  @VisibleForTesting
  static Multiset<Row> getRipEdges(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes) {
    Multiset<Row> rows = HashMultiset.create();
    for (Configuration c : configurations.values()) {
      String hostname = c.getHostname();
      for (Vrf vrf : c.getVrfs().values()) {
        RipProcess proc = vrf.getRipProcess();
        if (proc != null) {
          for (RipNeighbor ripNeighbor : proc.getRipNeighbors().values()) {
            RipNeighbor remoteRipNeighbor = ripNeighbor.getRemoteRipNeighbor();
            if (remoteRipNeighbor != null) {
              Configuration remoteHost = remoteRipNeighbor.getOwner();
              String remoteHostname = remoteHost.getHostname();
              if (includeNodes.contains(hostname) && includeRemoteNodes.contains(remoteHostname)) {
                rows.add(
                    getRipEdgeRow(
                        ripNeighbor.getOwner().getHostname(),
                        ripNeighbor.getIface().getName(),
                        remoteRipNeighbor.getOwner().getHostname(),
                        remoteRipNeighbor.getIface().getName()));
              }
            }
          }
        }
      }
    }

    return rows;
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
            new NodeInterfacePair(nodeU, ipsecPeerConfigU.getSourceInterface()))
        .put(
            COL_TUNNEL_INTERFACE,
            ipsecPeerConfigU.getTunnelInterface() == null
                ? null
                : new NodeInterfacePair(nodeU, ipsecPeerConfigU.getTunnelInterface()))
        .put(
            COL_REMOTE_SOURCE_INTERFACE,
            new NodeInterfacePair(nodeV, ipsecPeerConfigV.getSourceInterface()))
        .put(
            COL_REMOTE_TUNNEL_INTERFACE,
            ipsecPeerConfigV.getTunnelInterface() == null
                ? null
                : new NodeInterfacePair(nodeV, ipsecPeerConfigV.getTunnelInterface()));
    return row.build();
  }

  @VisibleForTesting
  static Row eigrpEdgeToRow(EigrpEdge eigrpEdge) {
    RowBuilder row = Row.builder();
    row.put(
            COL_INTERFACE,
            new NodeInterfacePair(
                eigrpEdge.getNode1().getHostname(), eigrpEdge.getNode1().getInterfaceName()))
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                eigrpEdge.getNode2().getHostname(), eigrpEdge.getNode2().getInterfaceName()));
    return row.build();
  }

  @VisibleForTesting
  static Row vxlanEdgeToRow(VxlanEdge edge) {
    RowBuilder row = Row.builder();
    row.put(COL_VNI, edge.getVni())
        .put(COL_NODE, new Node(edge.getTail().getHostname()))
        .put(COL_REMOTE_NODE, new Node(edge.getHead().getHostname()))
        .put(COL_VTEP_ADDRESS, edge.getTail().getSourceAddress())
        .put(COL_REMOTE_VTEP_ADDRESS, edge.getHead().getSourceAddress())
        .put(COL_VLAN, edge.getTail().getVlan())
        .put(COL_REMOTE_VLAN, edge.getHead().getVlan())
        .put(COL_UDP_PORT, edge.getUdpPort())
        .put(COL_MULTICAST_GROUP, edge.getMulticastGroup());
    return row.build();
  }

  @VisibleForTesting
  static Row getBgpEdgeRow(
      String node,
      @Nullable Ip ip,
      @Nullable Long asNumber,
      String remoteNode,
      @Nullable Ip remoteIp,
      @Nullable Long remoteAsNumber) {
    RowBuilder row = Row.builder();
    row.put(COL_NODE, new Node(node))
        .put(COL_IP, ip)
        .put(COL_AS_NUMBER, asNumber)
        .put(COL_REMOTE_NODE, new Node(remoteNode))
        .put(COL_REMOTE_IP, remoteIp)
        .put(COL_REMOTE_AS_NUMBER, remoteAsNumber);
    return row.build();
  }

  static Row isisEdgeToRow(IsisEdge isisEdge) {
    RowBuilder row = Row.builder();
    row.put(
            COL_INTERFACE,
            new NodeInterfacePair(
                isisEdge.getNode1().getNode(), isisEdge.getNode1().getInterfaceName()))
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                isisEdge.getNode2().getNode(), isisEdge.getNode2().getInterfaceName()));
    return row.build();
  }

  @VisibleForTesting
  static Row layer1EdgeToRow(Layer1Edge layer1Edge) {
    RowBuilder row = Row.builder();
    row.put(
            COL_INTERFACE,
            new NodeInterfacePair(
                layer1Edge.getNode1().getHostname(), layer1Edge.getNode1().getInterfaceName()))
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                layer1Edge.getNode2().getHostname(), layer1Edge.getNode2().getInterfaceName()));

    return row.build();
  }

  @VisibleForTesting
  static Row layer2EdgeToRow(Layer2Edge layer2Edge) {
    RowBuilder row = Row.builder();
    row.put(
            COL_INTERFACE,
            new NodeInterfacePair(
                layer2Edge.getNode1().getHostname(), layer2Edge.getNode1().getInterfaceName()))
        .put(COL_VLAN, layer2Edge.getNode1().getSwitchportVlanId())
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                layer2Edge.getNode2().getHostname(), layer2Edge.getNode2().getInterfaceName()))
        .put(COL_REMOTE_VLAN, layer2Edge.getNode2().getSwitchportVlanId());

    return row.build();
  }

  @VisibleForTesting
  static Row layer3EdgeToRow(Map<String, Configuration> configurations, Edge edge) {
    Interface interface1 =
        configurations.get(edge.getNode1()).getAllInterfaces().get(edge.getInt1());
    Interface interface2 =
        configurations.get(edge.getNode2()).getAllInterfaces().get(edge.getInt2());
    Set<Ip> ips1 =
        interface1.getAllAddresses().stream()
            .filter(Objects::nonNull)
            .map(InterfaceAddress::getIp)
            .collect(Collectors.toSet());
    Set<Ip> ips2 =
        interface2.getAllAddresses().stream()
            .filter(Objects::nonNull)
            .map(InterfaceAddress::getIp)
            .collect(Collectors.toSet());

    RowBuilder row = Row.builder();
    row.put(COL_INTERFACE, new NodeInterfacePair(edge.getNode1(), edge.getInt1()))
        .put(COL_IPS, ips1)
        .put(COL_REMOTE_INTERFACE, new NodeInterfacePair(edge.getNode2(), edge.getInt2()))
        .put(COL_REMOTE_IPS, ips2);

    return row.build();
  }

  @VisibleForTesting
  static Row getOspfEdgeRow(String node, String iface, String remoteNode, String remoteIface) {
    RowBuilder row = Row.builder();
    row.put(COL_INTERFACE, new NodeInterfacePair(node, iface))
        .put(COL_REMOTE_INTERFACE, new NodeInterfacePair(remoteNode, remoteIface));
    return row.build();
  }

  static Row getRipEdgeRow(String node, String iface, String remoteNode, String remoteIface) {
    RowBuilder row = Row.builder();
    row.put(COL_INTERFACE, new NodeInterfacePair(node, iface))
        .put(COL_REMOTE_INTERFACE, new NodeInterfacePair(remoteNode, remoteIface));
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
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(COL_IPS, Schema.set(Schema.IP), "IPs", Boolean.FALSE, Boolean.TRUE));

        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_INTERFACE,
                Schema.INTERFACE,
                "Interface at which the edge terminates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_IPS, Schema.set(Schema.IP), "Remote IPs", Boolean.FALSE, Boolean.TRUE));
        break;

      case LAYER2:
        columnBuilder.add(
            new ColumnMetadata(
                COL_INTERFACE,
                Schema.INTERFACE,
                "Interface from which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_VLAN,
                Schema.STRING,
                "VLAN containing the originator",
                Boolean.FALSE,
                Boolean.TRUE));

        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_INTERFACE,
                Schema.INTERFACE,
                "Interface at which the edge terminates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_REMOTE_VLAN,
                Schema.STRING,
                "VLAN  containing the remote node",
                Boolean.FALSE,
                Boolean.TRUE));
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
      case RIP:
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

package org.batfish.question.neighbors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Network;
import com.google.common.graph.ValueGraph;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Topology;
import org.batfish.common.topology.Layer2Edge;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NeighborType;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.RipNeighbor;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.IpEdge;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.collections.VerboseBgpEdge;
import org.batfish.datamodel.collections.VerboseEigrpEdge;
import org.batfish.datamodel.collections.VerboseOspfEdge;
import org.batfish.datamodel.collections.VerboseRipEdge;
import org.batfish.datamodel.eigrp.EigrpEdge;
import org.batfish.datamodel.eigrp.EigrpInterface;
import org.batfish.datamodel.eigrp.EigrpTopology;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisTopology;
import org.batfish.datamodel.ospf.OspfNeighbor;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

public class NeighborsAnswerer extends Answerer {

  // Global (always present) columns
  static final String COL_NODE = "Node";
  static final String COL_REMOTE_NODE = "Remote_Node";
  static final String COL_INTERFACE = "Interface";
  static final String COL_REMOTE_INTERFACE = "Remote_Interface";

  // Present sometimes
  static final String COL_IPS = "IPs";
  static final String COL_REMOTE_IPS = "Remote_IPs";

  // BGP only
  static final String COL_AS_NUMBER = "AS_Number";
  static final String COL_REMOTE_AS_NUMBER = "Remote_AS_Number";
  static final String COL_IP = "IP";
  static final String COL_REMOTE_IP = "Remote_IP";

  // Layer 2
  static final String COL_VLAN = "VLAN";
  static final String COL_REMOTE_VLAN = "Remote_VLAN";

  private static final Comparator<VerboseBgpEdge> VERBOSE_BGP_EDGE_COMPARATOR =
      Comparator.nullsFirst(
          Comparator.comparing(VerboseBgpEdge::getEdgeSummary)
              .thenComparing(VerboseBgpEdge::getSession1Id)
              .thenComparing(VerboseBgpEdge::getSession2Id));

  NeighborsAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    NeighborsQuestion question = (NeighborsQuestion) _question;

    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Set<String> includeNodes = question.getNodes().getMatchingNodes(_batfish);
    Set<String> includeRemoteNodes = question.getRemoteNodes().getMatchingNodes(_batfish);

    TableAnswerElement answer =
        new TableAnswerElement(getTableMetadata(question.getNeighborType()));
    Topology topology = _batfish.getEnvironmentTopology();
    answer.postProcessAnswer(
        _question,
        generateRows(
            configurations,
            topology,
            includeNodes,
            includeRemoteNodes,
            question.getNeighborType()));
    return answer;
  }

  private Multiset<Row> generateRows(
      Map<String, Configuration> configurations,
      Topology topology,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      NeighborType neighborType) {
    switch (neighborType) {
      case EBGP:
        return getEbgpNeighbors(configurations, includeNodes, includeRemoteNodes);
      case EIGRP:
        return getEigrpNeighbors(configurations, includeNodes, includeRemoteNodes, topology);
      case IBGP:
        return getIBgpNeighbors(configurations, includeNodes, includeRemoteNodes);
      case ISIS:
        return getIsisNeighbors(configurations, includeNodes, includeRemoteNodes, topology);
      case OSPF:
        return getOspfNeighbors(configurations, includeNodes, includeRemoteNodes, topology);
      case RIP:
        Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
        _batfish.initRemoteRipNeighbors(configurations, ipOwners, topology);
        return getRipNeighbors(configurations, includeNodes, includeRemoteNodes);
      case LAYER1:
        Layer1Topology layer1Topology = _batfish.getLayer1Topology();
        return getLayer1Neighbors(includeNodes, includeRemoteNodes, layer1Topology);
      case LAYER2:
        Layer2Topology layer2Topology = _batfish.getLayer2Topology();
        return getLayer2Neighbors(includeNodes, includeRemoteNodes, layer2Topology);
      default:
        return getLayer3Neighbors(configurations, includeNodes, includeRemoteNodes, topology);
    }
  }

  private static Multiset<Row> getEigrpNeighbors(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      Topology topology) {
    NetworkConfigurations nc = NetworkConfigurations.of(configurations);
    SortedSet<EigrpEdge> vedges = new TreeSet<>();
    Network<EigrpInterface, EigrpEdge> eigrpTopology =
        EigrpTopology.initEigrpTopology(configurations, topology);
    for (Configuration c : configurations.values()) {
      String hostname = c.getHostname();
      for (Vrf vrf : c.getVrfs().values()) {
        vedges.addAll(
            vrf.getInterfaceNames()
                .stream()
                .map(ifaceName -> new EigrpInterface(hostname, ifaceName, vrf.getName()))
                .filter(eigrpTopology.nodes()::contains)
                .flatMap(n -> eigrpTopology.inEdges(n).stream())
                .map(edge -> new VerboseEigrpEdge(edge, edge.toIpEdge(nc)))
                .map(VerboseEigrpEdge::getEdge)
                .filter(
                    eigrpEdge ->
                        includeNodes.contains(eigrpEdge.getNode1().getHostname())
                            && includeRemoteNodes.contains(eigrpEdge.getNode2().getHostname()))
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
      }
    }

    return vedges
        .stream()
        .filter(Objects::nonNull)
        .map(NeighborsAnswerer::eigrpEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  private static Multiset<Row> getEbgpNeighbors(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes) {
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        CommonUtil.initBgpTopology(configurations, ipOwners, false, false, null, null);
    SortedSet<VerboseBgpEdge> vedges = new TreeSet<>(VERBOSE_BGP_EDGE_COMPARATOR);
    for (EndpointPair<BgpPeerConfigId> session : bgpTopology.edges()) {
      BgpPeerConfigId bgpPeerConfigId = session.source();
      BgpPeerConfigId remoteBgpPeerConfigId = session.target();
      if (bgpTopology.edgeValue(bgpPeerConfigId, remoteBgpPeerConfigId).isEbgp()) {
        VerboseBgpEdge edge =
            constructVerboseBgpEdge(
                includeNodes,
                includeRemoteNodes,
                bgpPeerConfigId,
                remoteBgpPeerConfigId,
                NetworkConfigurations.of(configurations));
        if (edge != null) {
          vedges.add(edge);
        }
      }
    }

    return vedges
        .stream()
        .map(NeighborsAnswerer::bgpEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  private static Multiset<Row> getIBgpNeighbors(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes) {
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        CommonUtil.initBgpTopology(configurations, ipOwners, false, false, null, null);
    SortedSet<VerboseBgpEdge> vedges = new TreeSet<>(VERBOSE_BGP_EDGE_COMPARATOR);
    for (EndpointPair<BgpPeerConfigId> session : bgpTopology.edges()) {
      BgpPeerConfigId bgpPeerConfigId = session.source();
      BgpPeerConfigId remoteBgpPeerConfigId = session.target();
      BgpSessionProperties sessionProp =
          bgpTopology.edgeValue(bgpPeerConfigId, remoteBgpPeerConfigId);
      if (sessionProp.getSessionType() == SessionType.IBGP) {
        VerboseBgpEdge edge =
            constructVerboseBgpEdge(
                includeNodes,
                includeRemoteNodes,
                bgpPeerConfigId,
                remoteBgpPeerConfigId,
                NetworkConfigurations.of(configurations));
        if (edge != null) {
          vedges.add(edge);
        }
      }
    }
    return vedges
        .stream()
        .map(NeighborsAnswerer::bgpEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  private static Multiset<Row> getIsisNeighbors(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      Topology topology) {
    return IsisTopology.initIsisTopology(configurations, topology)
        .edges()
        .stream()
        .filter(Objects::nonNull)
        .filter(
            isisEdge ->
                includeNodes.contains(isisEdge.getNode1().getHostname())
                    && includeRemoteNodes.contains(isisEdge.getNode2().getHostname()))
        .map(NeighborsAnswerer::isisEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  private static Multiset<Row> getLayer1Neighbors(
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      @Nullable Layer1Topology layer1Topology) {
    if (layer1Topology == null) {
      return HashMultiset.create();
    }
    return layer1Topology
        .getGraph()
        .edges()
        .stream()
        .filter(
            layer1Edge ->
                includeNodes.contains(layer1Edge.getNode1().getHostname())
                    && includeRemoteNodes.contains(layer1Edge.getNode2().getHostname()))
        .map(NeighborsAnswerer::layer1EdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  private static Multiset<Row> getLayer2Neighbors(
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      @Nullable Layer2Topology layer2Topology) {
    if (layer2Topology == null) {
      return HashMultiset.create();
    }
    return layer2Topology
        .getGraph()
        .edges()
        .stream()
        .filter(
            layer2Edge ->
                includeNodes.contains(layer2Edge.getNode1().getHostname())
                    && includeRemoteNodes.contains(layer2Edge.getNode2().getHostname()))
        .map(NeighborsAnswerer::layer2EdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  private static Multiset<Row> getLayer3Neighbors(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      Topology topology) {
    return topology
        .getEdges()
        .stream()
        .filter(
            layer3Edge ->
                includeNodes.contains(layer3Edge.getNode1())
                    && includeRemoteNodes.contains(layer3Edge.getNode2()))
        .map(layer3edge -> layer3EdgeToRow(configurations, layer3edge))
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  private static Multiset<Row> getOspfNeighbors(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes,
      Topology topology) {
    SortedSet<VerboseOspfEdge> vedges = new TreeSet<>();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    CommonUtil.initRemoteOspfNeighbors(configurations, ipOwners, topology);
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
                Ip localIp = ospfNeighbor.getLocalIp();
                Ip remoteIp = remoteOspfNeighbor.getLocalIp();
                IpEdge edge = new IpEdge(hostname, localIp, remoteHostname, remoteIp);
                vedges.add(new VerboseOspfEdge(ospfNeighbor, remoteOspfNeighbor, edge));
              }
            }
          }
        }
      }
    }

    return vedges
        .stream()
        .map(NeighborsAnswerer::ospfEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  private static Multiset<Row> getRipNeighbors(
      Map<String, Configuration> configurations,
      Set<String> includeNodes,
      Set<String> includeRemoteNodes) {
    SortedSet<VerboseRipEdge> vedges = new TreeSet<>();
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
                Ip localIp = ripNeighbor.getLocalIp();
                Ip remoteIp = remoteRipNeighbor.getLocalIp();
                IpEdge edge = new IpEdge(hostname, localIp, remoteHostname, remoteIp);
                vedges.add(new VerboseRipEdge(ripNeighbor, remoteRipNeighbor, edge));
              }
            }
          }
        }
      }
    }

    return vedges
        .stream()
        .map(NeighborsAnswerer::ripEdgeToRow)
        .collect(Collectors.toCollection(HashMultiset::create));
  }

  static Row eigrpEdgeToRow(EigrpEdge eigrpEdge) {
    RowBuilder row = Row.builder();
    row.put(COL_NODE, new Node(eigrpEdge.getNode1().getHostname()))
        .put(
            COL_INTERFACE,
            new NodeInterfacePair(
                eigrpEdge.getNode1().getHostname(), eigrpEdge.getNode1().getInterfaceName()))
        .put(COL_REMOTE_NODE, new Node(eigrpEdge.getNode2().getHostname()))
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                eigrpEdge.getNode2().getHostname(), eigrpEdge.getNode2().getInterfaceName()));
    return row.build();
  }

  static Row bgpEdgeToRow(VerboseBgpEdge verboseBgpEdge) {
    RowBuilder row = Row.builder();
    IpEdge ipEdge = verboseBgpEdge.getEdgeSummary();
    row.put(COL_NODE, new Node(ipEdge.getNode1()))
        .put(COL_IP, ipEdge.getIp1())
        .put(COL_AS_NUMBER, verboseBgpEdge.getNode1Session().getLocalAs())
        .put(COL_REMOTE_NODE, new Node(ipEdge.getNode2()))
        .put(COL_REMOTE_IP, ipEdge.getIp2())
        .put(COL_REMOTE_AS_NUMBER, verboseBgpEdge.getNode2Session().getLocalAs());
    return row.build();
  }

  static Row isisEdgeToRow(IsisEdge isisEdge) {
    RowBuilder row = Row.builder();
    row.put(COL_NODE, new Node(isisEdge.getNode1().getHostname()))
        .put(
            COL_INTERFACE,
            new NodeInterfacePair(
                isisEdge.getNode1().getHostname(), isisEdge.getNode1().getInterfaceName()))
        .put(COL_REMOTE_NODE, new Node(isisEdge.getNode2().getHostname()))
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                isisEdge.getNode2().getHostname(), isisEdge.getNode2().getInterfaceName()));
    return row.build();
  }

  static Row layer1EdgeToRow(Layer1Edge layer1Edge) {
    RowBuilder row = Row.builder();
    row.put(COL_NODE, new Node(layer1Edge.getNode1().getHostname()))
        .put(
            COL_INTERFACE,
            new NodeInterfacePair(
                layer1Edge.getNode1().getHostname(), layer1Edge.getNode1().getInterfaceName()))
        .put(COL_REMOTE_NODE, new Node(layer1Edge.getNode2().getHostname()))
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                layer1Edge.getNode2().getHostname(), layer1Edge.getNode2().getInterfaceName()));

    return row.build();
  }

  static Row layer2EdgeToRow(Layer2Edge layer2Edge) {
    RowBuilder row = Row.builder();
    row.put(COL_NODE, new Node(layer2Edge.getNode1().getHostname()))
        .put(
            COL_INTERFACE,
            new NodeInterfacePair(
                layer2Edge.getNode1().getHostname(), layer2Edge.getNode1().getInterfaceName()))
        .put(COL_VLAN, layer2Edge.getNode1().getVlanId())
        .put(COL_REMOTE_NODE, new Node(layer2Edge.getNode2().getHostname()))
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                layer2Edge.getNode2().getHostname(), layer2Edge.getNode2().getInterfaceName()))
        .put(COL_REMOTE_VLAN, layer2Edge.getNode2().getVlanId());

    return row.build();
  }

  static Row layer3EdgeToRow(Map<String, Configuration> configurations, Edge edge) {
    Interface interface1 = configurations.get(edge.getNode1()).getInterfaces().get(edge.getInt1());
    Interface interface2 = configurations.get(edge.getNode2()).getInterfaces().get(edge.getInt2());
    Set<Ip> ips1 =
        interface1
            .getAllAddresses()
            .stream()
            .filter(Objects::nonNull)
            .map(InterfaceAddress::getIp)
            .collect(Collectors.toSet());
    Set<Ip> ips2 =
        interface2
            .getAllAddresses()
            .stream()
            .filter(Objects::nonNull)
            .map(InterfaceAddress::getIp)
            .collect(Collectors.toSet());

    RowBuilder row = Row.builder();
    row.put(COL_NODE, new Node(edge.getNode1()))
        .put(COL_INTERFACE, new NodeInterfacePair(edge.getNode1(), edge.getInt1()))
        .put(COL_IPS, ips1)
        .put(COL_REMOTE_NODE, new Node(edge.getNode2()))
        .put(COL_REMOTE_INTERFACE, new NodeInterfacePair(edge.getNode2(), edge.getInt2()))
        .put(COL_REMOTE_IPS, ips2);

    return row.build();
  }

  static Row ospfEdgeToRow(VerboseOspfEdge verboseOspfEdge) {
    RowBuilder row = Row.builder();
    row.put(COL_NODE, new Node(verboseOspfEdge.getSession1().getOwner().getHostname()))
        .put(
            COL_INTERFACE,
            new NodeInterfacePair(
                verboseOspfEdge.getSession1().getOwner().getHostname(),
                verboseOspfEdge.getSession1().getIface().getName()))
        .put(COL_REMOTE_NODE, new Node(verboseOspfEdge.getSession2().getOwner().getHostname()))
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                verboseOspfEdge.getSession2().getOwner().getHostname(),
                verboseOspfEdge.getSession2().getIface().getName()));
    return row.build();
  }

  static Row ripEdgeToRow(VerboseRipEdge verboseRipEdge) {
    RowBuilder row = Row.builder();
    row.put(COL_NODE, new Node(verboseRipEdge.getSession1().getOwner().getHostname()))
        .put(
            COL_INTERFACE,
            new NodeInterfacePair(
                verboseRipEdge.getSession1().getOwner().getHostname(),
                verboseRipEdge.getSession1().getIface().getName()))
        .put(COL_REMOTE_NODE, new Node(verboseRipEdge.getSession2().getOwner().getHostname()))
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(
                verboseRipEdge.getSession2().getOwner().getHostname(),
                verboseRipEdge.getSession2().getIface().getName()));
    return row.build();
  }

  /**
   * Create a verbose bgp edge, if hostnames match specified pattern
   *
   * @param includeNodes1 Allowed src hostnames
   * @param includeNodes2 Allowed dst hostnames
   * @param bgpPeerConfigId The id of node1 bgp neighbor
   * @param remoteBgpPeerConfigId The id of node2 bgp neighbor
   * @param nc {@link NetworkConfigurations} to get {@link BgpPeerConfig}s
   * @return a new {@link VerboseBgpEdge} describing the BGP peering or {@code null} if hostname
   *     filters are not satisfied.
   */
  @Nullable
  private static VerboseBgpEdge constructVerboseBgpEdge(
      Set<String> includeNodes1,
      Set<String> includeNodes2,
      BgpPeerConfigId bgpPeerConfigId,
      BgpPeerConfigId remoteBgpPeerConfigId,
      NetworkConfigurations nc) {
    String hostname = bgpPeerConfigId.getHostname();
    String remoteHostname = remoteBgpPeerConfigId.getHostname();
    BgpPeerConfig bgpPeerConfig = nc.getBgpPeerConfig(bgpPeerConfigId);
    BgpPeerConfig remoteBgpPeerConfig = nc.getBgpPeerConfig(remoteBgpPeerConfigId);
    if (bgpPeerConfig == null || remoteBgpPeerConfig == null) {
      return null;
    }
    if (includeNodes1.contains(hostname) && includeNodes2.contains(remoteHostname)) {
      Ip localIp = bgpPeerConfig.getLocalIp();
      Ip remoteIp = remoteBgpPeerConfig.getLocalIp();
      IpEdge edge = new IpEdge(hostname, localIp, remoteHostname, remoteIp);
      return new VerboseBgpEdge(
          bgpPeerConfig, remoteBgpPeerConfig, bgpPeerConfigId, remoteBgpPeerConfigId, edge);
    }
    return null;
  }

  /** Generate the table metadata based on the type of neighbor requested */
  @VisibleForTesting
  static TableMetadata getTableMetadata(NeighborType neighborType) {
    ImmutableList.Builder<ColumnMetadata> columnBuilder = ImmutableList.builder();
    switch (neighborType) {
      case LAYER3:
        columnBuilder.add(
            new ColumnMetadata(
                COL_NODE,
                Schema.NODE,
                "Node from which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));
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
                COL_REMOTE_NODE,
                Schema.NODE,
                "Node at which the edge terminates",
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
                COL_REMOTE_IPS, Schema.set(Schema.IP), "Remote IPs", Boolean.FALSE, Boolean.TRUE));
        break;

      case LAYER2:
        columnBuilder.add(
            new ColumnMetadata(
                COL_NODE,
                Schema.NODE,
                "Node from which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));
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
                COL_REMOTE_NODE,
                Schema.NODE,
                "Node at which the edge terminates",
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

      case IBGP:
      case EBGP:
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

      default:
        // For OSPF, ISIS, EIGRP, RIP
        columnBuilder.add(
            new ColumnMetadata(
                COL_NODE,
                Schema.NODE,
                "Node from which the edge originates",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_INTERFACE,
                Schema.INTERFACE,
                "Interface from which the edge originates",
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
                COL_REMOTE_INTERFACE,
                Schema.INTERFACE,
                "Interface at which the edge terminates",
                Boolean.FALSE,
                Boolean.TRUE));
    }
    return new TableMetadata(columnBuilder.build(), "Display Neighbors");
  }
}

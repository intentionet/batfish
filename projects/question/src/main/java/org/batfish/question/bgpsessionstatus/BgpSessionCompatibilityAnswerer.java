package org.batfish.question.bgpsessionstatus;

import static org.batfish.datamodel.BgpSessionProperties.getSessionType;
import static org.batfish.datamodel.questions.ConfiguredSessionStatus.DYNAMIC_MATCH;
import static org.batfish.datamodel.questions.ConfiguredSessionStatus.NO_MATCH_FOUND;
import static org.batfish.datamodel.questions.ConfiguredSessionStatus.UNIQUE_MATCH;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_LOCAL_AS;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_LOCAL_INTERFACE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_LOCAL_IP;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_NODE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_REMOTE_AS;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_REMOTE_INTERFACE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_REMOTE_IP;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_REMOTE_NODE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_SESSION_TYPE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_VRF;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.getBgpPeerConfig;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.getConfiguredStatus;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.getLocallyBrokenStatus;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.matchesNodesAndType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.graph.ValueGraph;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.TopologyUtil;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.answers.SelfDescribingObject;
import org.batfish.datamodel.bgp.BgpTopologyUtils;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.ConfiguredSessionStatus;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

public class BgpSessionCompatibilityAnswerer extends Answerer {

  public static final String COL_CONFIGURED_STATUS = "Configured_Status";

  /** Answerer for the BGP session compatibility question. */
  public BgpSessionCompatibilityAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    BgpSessionCompatibilityQuestion question = (BgpSessionCompatibilityQuestion) _question;
    TableAnswerElement answer =
        new TableAnswerElement(BgpSessionCompatibilityAnswerer.createMetadata(question));
    answer.postProcessAnswer(question, getRows(question));
    return answer;
  }

  /**
   * Return the answer for {@link BgpSessionCompatibilityQuestion} -- a set of BGP sessions and
   * their compatibility.
   */
  public List<Row> getRows(BgpSessionQuestion question) {
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Map<String, ColumnMetadata> metadataMap = createMetadata(question).toColumnMap();
    Set<String> nodes = question.getNodeSpecifier().resolve(_batfish.specifierContext());
    Set<String> remoteNodes =
        question.getRemoteNodeSpecifier().resolve(_batfish.specifierContext());
    Layer2Topology layer2Topology =
        _batfish
            .getTopologyProvider()
            .getInitialLayer2Topology(_batfish.getNetworkSnapshot())
            .orElse(null);
    return getRows(question, configurations, nodes, remoteNodes, metadataMap, layer2Topology);
  }

  @VisibleForTesting
  static List<Row> getRows(
      BgpSessionQuestion question,
      Map<String, Configuration> configurations,
      Set<String> nodes,
      Set<String> remoteNodes,
      Map<String, ColumnMetadata> metadataMap,
      @Nullable Layer2Topology layer2Topology) {
    Map<Ip, Set<String>> ipOwners = TopologyUtil.computeIpNodeOwners(configurations, true);
    Set<Ip> allInterfaceIps = ipOwners.keySet();

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredBgpTopology =
        BgpTopologyUtils.initBgpTopology(configurations, ipOwners, true, layer2Topology).getGraph();

    // Keep track of compatible active peers to assess whether passive peers have compatible remotes
    Set<BgpPeerConfigId> compatibleActivePeers = new TreeSet<>();

    Stream<Row> activePeerRows =
        configuredBgpTopology.nodes().stream()
            .map(
                neighbor -> {
                  BgpPeerConfig bpc = getBgpPeerConfig(configurations, neighbor);
                  if (bpc instanceof BgpPassivePeerConfig
                      || bpc instanceof BgpUnnumberedPeerConfig) {
                    return null;
                  } else if (!(bpc instanceof BgpActivePeerConfig)) {
                    throw new BatfishException(
                        "Unsupported type of BGP peer config (not active or passive): "
                            + bpc.getClass().getName());
                  }
                  BgpActivePeerConfig activePeer = (BgpActivePeerConfig) bpc;

                  SessionType type = getSessionType(activePeer);
                  ConfiguredSessionStatus configuredStatus =
                      getConfiguredStatus(
                          neighbor, activePeer, type, allInterfaceIps, configuredBgpTopology);

                  // If neighbor is compatible, add it to compatibleActivePeers
                  if (configuredStatus == UNIQUE_MATCH) {
                    compatibleActivePeers.add(neighbor);
                  }

                  return buildActivePeerRow(
                      neighbor, activePeer, configuredStatus, metadataMap, configuredBgpTopology);
                })
            .filter(
                row -> row != null && matchesQuestionFilters(row, nodes, remoteNodes, question));

    Stream<Row> unnumPeerRows =
        configuredBgpTopology.nodes().stream()
            .map(
                neighbor -> {
                  BgpPeerConfig bpc = getBgpPeerConfig(configurations, neighbor);
                  if (!(bpc instanceof BgpUnnumberedPeerConfig)) {
                    return null;
                  }

                  BgpUnnumberedPeerConfig unnumPeer = (BgpUnnumberedPeerConfig) bpc;

                  ConfiguredSessionStatus configuredStatus =
                      getConfiguredStatus(neighbor, unnumPeer, configuredBgpTopology);

                  return buildUnnumPeerRow(
                      neighbor, unnumPeer, configuredStatus, metadataMap, configuredBgpTopology);
                })
            .filter(
                row -> row != null && matchesQuestionFilters(row, nodes, remoteNodes, question));

    Stream<Row> passivePeerRows =
        configuredBgpTopology.nodes().stream()
            .filter(neighbor -> nodes.contains(neighbor.getHostname()))
            .flatMap(
                neighbor -> {
                  BgpPeerConfig bpc = getBgpPeerConfig(configurations, neighbor);
                  if (!(bpc instanceof BgpPassivePeerConfig)) {
                    return Stream.of();
                  }
                  BgpPassivePeerConfig passivePeer = (BgpPassivePeerConfig) bpc;

                  // If peer has null remote prefix or empty remote AS list, generate one row
                  ConfiguredSessionStatus brokenStatus = getLocallyBrokenStatus(passivePeer);
                  if (brokenStatus != null) {
                    return Stream.of(
                        buildPassivePeerWithoutRemoteRow(
                            metadataMap, neighbor, passivePeer, brokenStatus));
                  }

                  // Find all correctly configured remote peers compatible with this peer
                  Set<BgpPeerConfigId> remoteIds =
                      Sets.intersection(
                          configuredBgpTopology.adjacentNodes(neighbor), compatibleActivePeers);

                  // If no compatible neighbors exist, generate one NO_MATCH_FOUND row
                  if (remoteIds.isEmpty()) {
                    return Stream.of(
                        buildPassivePeerWithoutRemoteRow(
                            metadataMap, neighbor, passivePeer, NO_MATCH_FOUND));
                  }

                  // Compatible neighbors exist. Generate a row for each.
                  return remoteIds.stream()
                      .map(
                          remoteId ->
                              buildDynamicMatchRow(
                                  metadataMap,
                                  neighbor,
                                  passivePeer,
                                  remoteId,
                                  (BgpActivePeerConfig)
                                      getBgpPeerConfig(configurations, remoteId)));
                })
            .filter(row -> matchesQuestionFilters(row, nodes, remoteNodes, question));

    return Streams.concat(activePeerRows, unnumPeerRows, passivePeerRows)
        .collect(ImmutableList.toImmutableList());
  }

  public static TableMetadata createMetadata(Question question) {
    List<ColumnMetadata> columnMetadata =
        ImmutableList.of(
            new ColumnMetadata(
                COL_NODE, Schema.NODE, "The node where this session is configured", true, false),
            new ColumnMetadata(
                COL_VRF, Schema.STRING, "The VRF in which this session is configured", true, false),
            new ColumnMetadata(
                COL_LOCAL_AS, Schema.LONG, "The local AS of the session", false, false),
            new ColumnMetadata(
                COL_LOCAL_INTERFACE,
                Schema.INTERFACE,
                "Local interface of the session",
                false,
                true),
            new ColumnMetadata(
                COL_LOCAL_IP, Schema.IP, "The local IP of the session", false, false),
            new ColumnMetadata(
                COL_REMOTE_AS,
                Schema.STRING,
                "The remote AS or list of ASes of the session",
                false,
                false),
            new ColumnMetadata(
                COL_REMOTE_NODE, Schema.NODE, "Remote node for this session", false, false),
            new ColumnMetadata(
                COL_REMOTE_INTERFACE,
                Schema.INTERFACE,
                "Remote interface for this session",
                false,
                false),
            new ColumnMetadata(
                COL_REMOTE_IP,
                Schema.SELF_DESCRIBING,
                "Remote IP or prefix for this session",
                true,
                false),
            new ColumnMetadata(
                COL_SESSION_TYPE, Schema.STRING, "The type of this session", false, false),
            new ColumnMetadata(
                COL_CONFIGURED_STATUS, Schema.STRING, "Configured status", false, true));

    String textDesc =
        String.format(
            "On ${%s} session ${%s}:${%s} has configured status ${%s}.",
            COL_NODE, COL_VRF, COL_REMOTE_IP, COL_CONFIGURED_STATUS);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(columnMetadata, textDesc);
  }

  private static @Nonnull Row buildActivePeerRow(
      BgpPeerConfigId activeId,
      BgpActivePeerConfig activePeer,
      ConfiguredSessionStatus status,
      Map<String, ColumnMetadata> metadataMap,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology) {
    Node remoteNode = null;
    if (status == UNIQUE_MATCH) {
      String remoteNodeName = bgpTopology.adjacentNodes(activeId).iterator().next().getHostname();
      remoteNode = new Node(remoteNodeName);
    }

    return Row.builder(metadataMap)
        .put(COL_CONFIGURED_STATUS, status)
        .put(COL_LOCAL_INTERFACE, null)
        .put(COL_LOCAL_AS, activePeer.getLocalAs())
        .put(COL_LOCAL_IP, activePeer.getLocalIp())
        .put(COL_NODE, new Node(activeId.getHostname()))
        .put(COL_REMOTE_AS, activePeer.getRemoteAsns().toString())
        .put(COL_REMOTE_NODE, remoteNode)
        .put(COL_REMOTE_INTERFACE, null)
        .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, activePeer.getPeerAddress()))
        .put(COL_SESSION_TYPE, getSessionType(activePeer))
        .put(COL_VRF, activeId.getVrfName())
        .build();
  }

  private static @Nonnull Row buildUnnumPeerRow(
      BgpPeerConfigId unnumId,
      BgpUnnumberedPeerConfig unnumPeer,
      ConfiguredSessionStatus status,
      Map<String, ColumnMetadata> metadataMap,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology) {
    Node remoteNode = null;
    NodeInterfacePair remoteInterface = null;
    if (status == UNIQUE_MATCH) {
      BgpPeerConfigId remoteId = bgpTopology.adjacentNodes(unnumId).iterator().next();
      remoteNode = new Node(remoteId.getHostname());
      remoteInterface = new NodeInterfacePair(remoteId.getHostname(), remoteId.getPeerInterface());
    }

    return Row.builder(metadataMap)
        .put(COL_CONFIGURED_STATUS, status)
        .put(
            COL_LOCAL_INTERFACE,
            new NodeInterfacePair(unnumId.getHostname(), unnumPeer.getPeerInterface()))
        .put(COL_LOCAL_AS, unnumPeer.getLocalAs())
        .put(COL_LOCAL_IP, null)
        .put(COL_NODE, new Node(unnumId.getHostname()))
        .put(COL_REMOTE_AS, unnumPeer.getRemoteAsns().toString())
        .put(COL_REMOTE_NODE, remoteNode)
        .put(COL_REMOTE_INTERFACE, remoteInterface)
        .put(COL_REMOTE_IP, null)
        .put(COL_SESSION_TYPE, getSessionType(unnumPeer))
        .put(COL_VRF, unnumId.getVrfName())
        .build();
  }

  private static @Nonnull Row buildPassivePeerWithoutRemoteRow(
      Map<String, ColumnMetadata> metadataMap,
      BgpPeerConfigId passiveId,
      BgpPassivePeerConfig passivePeer,
      ConfiguredSessionStatus status) {
    return Row.builder(metadataMap)
        .put(COL_CONFIGURED_STATUS, status)
        .put(COL_LOCAL_INTERFACE, null)
        .put(COL_LOCAL_AS, passivePeer.getLocalAs())
        .put(COL_LOCAL_IP, passivePeer.getLocalIp())
        .put(COL_NODE, new Node(passiveId.getHostname()))
        .put(COL_REMOTE_AS, passivePeer.getRemoteAsns().toString())
        .put(COL_REMOTE_NODE, null)
        .put(COL_REMOTE_INTERFACE, null)
        .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.PREFIX, passivePeer.getPeerPrefix()))
        .put(COL_SESSION_TYPE, SessionType.UNSET)
        .put(COL_VRF, passiveId.getVrfName())
        .build();
  }

  private static @Nonnull Row buildDynamicMatchRow(
      Map<String, ColumnMetadata> metadataMap,
      BgpPeerConfigId passiveId,
      BgpPassivePeerConfig passivePeer,
      BgpPeerConfigId activeId,
      BgpActivePeerConfig activePeer) {
    return Row.builder(metadataMap)
        .put(COL_CONFIGURED_STATUS, DYNAMIC_MATCH)
        .put(COL_LOCAL_INTERFACE, null)
        .put(COL_LOCAL_AS, passivePeer.getLocalAs())
        .put(COL_LOCAL_IP, activePeer.getPeerAddress())
        .put(COL_NODE, new Node(passiveId.getHostname()))
        .put(COL_REMOTE_AS, LongSpace.of(activePeer.getLocalAs()).toString())
        .put(COL_REMOTE_NODE, new Node(activeId.getHostname()))
        .put(COL_REMOTE_INTERFACE, null)
        .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, activePeer.getLocalIp()))
        .put(COL_SESSION_TYPE, getSessionType(activePeer))
        .put(COL_VRF, passiveId.getVrfName())
        .build();
  }

  static boolean matchesQuestionFilters(
      Row row, Set<String> nodes, Set<String> remoteNodes, BgpSessionQuestion question) {
    if (!matchesNodesAndType(row, nodes, remoteNodes, question)) {
      return false;
    }

    // Check configured session status
    String statusName = (String) row.get(COL_CONFIGURED_STATUS, Schema.STRING);
    ConfiguredSessionStatus status =
        statusName == null ? null : ConfiguredSessionStatus.valueOf(statusName);
    if (!question.matchesStatus(status)) {
      return false;
    }

    return true;
  }
}

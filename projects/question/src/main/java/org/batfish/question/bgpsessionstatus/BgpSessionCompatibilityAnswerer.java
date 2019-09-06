package org.batfish.question.bgpsessionstatus;

import static org.batfish.datamodel.BgpSessionProperties.getSessionType;
import static org.batfish.datamodel.questions.ConfiguredSessionStatus.DYNAMIC_MATCH;
import static org.batfish.datamodel.questions.ConfiguredSessionStatus.NO_MATCH_FOUND;
import static org.batfish.datamodel.questions.ConfiguredSessionStatus.UNIQUE_MATCH;
import static org.batfish.datamodel.table.TableMetadata.toColumnMap;
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
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.getConfiguredStatus;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.getLocallyBrokenStatus;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.matchesNodesAndType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.graph.ValueGraph;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.topology.IpOwners;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.NetworkConfigurations;
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
import org.batfish.specifier.SpecifierContext;

/** Answerer for {@link BgpSessionCompatibilityQuestion} */
public class BgpSessionCompatibilityAnswerer extends Answerer {

  public static final String COL_CONFIGURED_STATUS = "Configured_Status";

  @VisibleForTesting
  static final List<ColumnMetadata> COLUMN_METADATA =
      ImmutableList.of(
          new ColumnMetadata(
              COL_NODE, Schema.NODE, "The node where this session is configured", true, false),
          new ColumnMetadata(
              COL_VRF, Schema.STRING, "The VRF in which this session is configured", true, false),
          new ColumnMetadata(
              COL_LOCAL_AS, Schema.LONG, "The local AS of the session", false, false),
          new ColumnMetadata(
              COL_LOCAL_INTERFACE, Schema.INTERFACE, "Local interface of the session", false, true),
          new ColumnMetadata(COL_LOCAL_IP, Schema.IP, "The local IP of the session", false, false),
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

  private static final Map<String, ColumnMetadata> METADATA_MAP = toColumnMap(COLUMN_METADATA);

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
  private List<Row> getRows(BgpSessionCompatibilityQuestion question) {
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    NetworkConfigurations nc = NetworkConfigurations.of(configurations);
    SpecifierContext specifierContext = _batfish.specifierContext();
    Set<String> nodes = question.getNodeSpecifier().resolve(specifierContext);
    Set<String> remoteNodes = question.getRemoteNodeSpecifier().resolve(specifierContext);
    Layer2Topology layer2Topology =
        _batfish
            .getTopologyProvider()
            .getInitialLayer2Topology(_batfish.getNetworkSnapshot())
            .orElse(null);
    Map<Ip, Map<String, Set<String>>> ipVrfOwners = new IpOwners(configurations).getIpVrfOwners();
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology =
        BgpTopologyUtils.initBgpTopology(configurations, ipVrfOwners, true, layer2Topology)
            .getGraph();

    // Generate answer row for each BGP peer (or rows, for dynamic peers with multiple remotes)
    return configuredTopology.nodes().stream()
        .flatMap(
            peerId -> {
              switch (peerId.getType()) {
                case ACTIVE:
                  BgpActivePeerConfig activePeer = nc.getBgpPointToPointPeerConfig(peerId);
                  assert activePeer != null;
                  return Stream.of(
                      getActivePeerRow(peerId, activePeer, ipVrfOwners, configuredTopology));
                case DYNAMIC:
                  BgpPassivePeerConfig passivePeer = nc.getBgpDynamicPeerConfig(peerId);
                  assert passivePeer != null;
                  return getPassivePeerRows(peerId, passivePeer, nc, configuredTopology).stream();
                case UNNUMBERED:
                  BgpUnnumberedPeerConfig unnumPeer = nc.getBgpUnnumberedPeerConfig(peerId);
                  assert unnumPeer != null;
                  return Stream.of(getUnnumberedPeerRow(peerId, unnumPeer, configuredTopology));
                default:
                  throw new BatfishException(
                      String.format("Unsupported type of BGP peer config: %s", peerId.getType()));
              }
            })
        .filter(row -> matchesQuestionFilters(row, nodes, remoteNodes, question))
        .collect(ImmutableList.toImmutableList());
  }

  @Nonnull
  @VisibleForTesting
  static Row getActivePeerRow(
      BgpPeerConfigId activeId,
      BgpActivePeerConfig activePeer,
      Map<Ip, Map<String, Set<String>>> ipVrfOwners,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology) {
    // Determine peer's session type and status. If compatible, find its unique remote match
    SessionType type = getSessionType(activePeer);
    ConfiguredSessionStatus status =
        getConfiguredStatus(activeId, activePeer, type, ipVrfOwners, configuredTopology);
    Node remoteNode = null;
    if (status == UNIQUE_MATCH) {
      String remoteNodeName =
          configuredTopology.adjacentNodes(activeId).iterator().next().getHostname();
      remoteNode = new Node(remoteNodeName);
    }
    return Row.builder(METADATA_MAP)
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

  @Nonnull
  @VisibleForTesting
  static List<Row> getPassivePeerRows(
      BgpPeerConfigId passiveId,
      BgpPassivePeerConfig passivePeer,
      NetworkConfigurations nc,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology) {
    // Start row with base columns. Need to add status.
    // If there are compatible peers, will also add remote node and replace:
    // - local IP, with the remote node's remote IP
    // - remote AS
    // - remote IP
    // - session type
    // Local and remote interface will not be filled in (reserved for unnumbered peers).
    Row.TypedRowBuilder rb =
        Row.builder(METADATA_MAP)
            .put(COL_LOCAL_AS, passivePeer.getLocalAs())
            .put(COL_LOCAL_IP, passivePeer.getLocalIp())
            .put(COL_NODE, new Node(passiveId.getHostname()))
            .put(COL_REMOTE_AS, passivePeer.getRemoteAsns().toString())
            .put(
                COL_REMOTE_IP, new SelfDescribingObject(Schema.PREFIX, passivePeer.getPeerPrefix()))
            .put(COL_SESSION_TYPE, SessionType.UNSET)
            .put(COL_VRF, passiveId.getVrfName());

    // If peer has null remote prefix or empty remote AS list, generate one row
    ConfiguredSessionStatus brokenStatus = getLocallyBrokenStatus(passivePeer);
    if (brokenStatus != null) {
      return ImmutableList.of(rb.put(COL_CONFIGURED_STATUS, brokenStatus).build());
    }

    // Create a row for each valid remote peer compatible with this peer
    List<Row> rows =
        configuredTopology.adjacentNodes(passiveId).stream()
            // inDegree vs outDegree doesn't matter here, just need to make sure the remote isn't
            // compatible with any other peers
            .filter(remoteId -> configuredTopology.inDegree(remoteId) == 1)
            .map(
                remoteId -> {
                  BgpSessionProperties sessionProps =
                      configuredTopology.edgeValue(passiveId, remoteId).orElse(null);
                  assert sessionProps != null;
                  BgpActivePeerConfig activeRemote = nc.getBgpPointToPointPeerConfig(remoteId);
                  assert activeRemote != null;
                  return rb.put(COL_CONFIGURED_STATUS, DYNAMIC_MATCH)
                      .put(COL_LOCAL_IP, sessionProps.getTailIp())
                      .put(COL_REMOTE_AS, LongSpace.of(activeRemote.getLocalAs()).toString())
                      .put(COL_REMOTE_NODE, new Node(remoteId.getHostname()))
                      .put(
                          COL_REMOTE_IP,
                          new SelfDescribingObject(Schema.IP, sessionProps.getHeadIp()))
                      .put(COL_SESSION_TYPE, getSessionType(activeRemote))
                      .build();
                })
            .collect(ImmutableList.toImmutableList());

    // If no compatible neighbors were found, generate one NO_MATCH_FOUND row
    return rows.isEmpty()
        ? ImmutableList.of(rb.put(COL_CONFIGURED_STATUS, NO_MATCH_FOUND).build())
        : rows;
  }

  @Nonnull
  @VisibleForTesting
  static Row getUnnumberedPeerRow(
      BgpPeerConfigId unnumId,
      BgpUnnumberedPeerConfig unnumPeer,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology) {
    ConfiguredSessionStatus status = getConfiguredStatus(unnumId, unnumPeer, configuredTopology);
    Node remoteNode = null;
    NodeInterfacePair remoteInterface = null;
    if (status == UNIQUE_MATCH) {
      BgpPeerConfigId remoteId = configuredTopology.adjacentNodes(unnumId).iterator().next();
      remoteNode = new Node(remoteId.getHostname());
      remoteInterface = NodeInterfacePair.of(remoteId.getHostname(), remoteId.getPeerInterface());
    }
    return Row.builder(METADATA_MAP)
        .put(COL_CONFIGURED_STATUS, status)
        .put(
            COL_LOCAL_INTERFACE,
            NodeInterfacePair.of(unnumId.getHostname(), unnumPeer.getPeerInterface()))
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

  public static TableMetadata createMetadata(Question question) {
    String textDesc =
        String.format(
            "On ${%s} session ${%s}:${%s} has configured status ${%s}.",
            COL_NODE, COL_VRF, COL_REMOTE_IP, COL_CONFIGURED_STATUS);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(COLUMN_METADATA, textDesc);
  }

  private static boolean matchesQuestionFilters(
      Row row,
      Set<String> nodes,
      Set<String> remoteNodes,
      BgpSessionCompatibilityQuestion question) {
    return matchesNodesAndType(row, nodes, remoteNodes, question)
        && question.matchesStatus(
            ConfiguredSessionStatus.parse((String) row.get(COL_CONFIGURED_STATUS, Schema.STRING)));
  }
}

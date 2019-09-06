package org.batfish.question.bgpsessionstatus;

import static org.batfish.datamodel.BgpSessionProperties.getSessionType;
import static org.batfish.datamodel.questions.BgpSessionStatus.ESTABLISHED;
import static org.batfish.datamodel.questions.BgpSessionStatus.NOT_COMPATIBLE;
import static org.batfish.datamodel.questions.BgpSessionStatus.NOT_ESTABLISHED;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ValueGraph;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
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
import org.batfish.datamodel.questions.BgpSessionStatus;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.SpecifierContext;

/** Answerer for {@link BgpSessionStatusQuestion} */
public class BgpSessionStatusAnswerer extends Answerer {

  static final String COL_ESTABLISHED_STATUS = "Established_Status";

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
              COL_ESTABLISHED_STATUS, Schema.STRING, "Established status", false, true));

  private static final Map<String, ColumnMetadata> METADATA_MAP = toColumnMap(COLUMN_METADATA);

  /** Answerer for the BGP Session status question (new version). */
  public BgpSessionStatusAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    BgpSessionStatusQuestion question = (BgpSessionStatusQuestion) _question;
    TableAnswerElement answer = new TableAnswerElement(createMetadata(question));
    answer.postProcessAnswer(question, getRows(question));
    return answer;
  }

  /**
   * Return the answer for {@link BgpSessionStatusQuestion} -- a set of BGP sessions and their
   * status.
   */
  private List<Row> getRows(BgpSessionStatusQuestion question) {
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    NetworkConfigurations nc = NetworkConfigurations.of(configurations);
    SpecifierContext specifierContext = _batfish.specifierContext();
    Set<String> nodes = question.getNodeSpecifier().resolve(specifierContext);
    Set<String> remoteNodes = question.getRemoteNodeSpecifier().resolve(specifierContext);
    Map<Ip, Map<String, Set<String>>> ipVrfOwners =
        _batfish.getTopologyProvider().getIpOwners(_batfish.getNetworkSnapshot()).getIpVrfOwners();
    Layer2Topology layer2Topology =
        _batfish
            .getTopologyProvider()
            .getLayer2Topology(_batfish.getNetworkSnapshot())
            .orElse(null);

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology =
        BgpTopologyUtils.initBgpTopology(configurations, ipVrfOwners, true, layer2Topology)
            .getGraph();

    ValueGraph<BgpPeerConfigId, BgpSessionProperties> establishedTopology =
        _batfish.getTopologyProvider().getBgpTopology(_batfish.getNetworkSnapshot()).getGraph();

    // Generate answer row for each BGP peer (or rows, for dynamic peers with multiple remotes)
    return configuredTopology.nodes().stream()
        .flatMap(
            peerId -> {
              switch (peerId.getType()) {
                case ACTIVE:
                  BgpActivePeerConfig activePeer = nc.getBgpPointToPointPeerConfig(peerId);
                  assert activePeer != null;
                  return Stream.of(
                      getActivePeerRow(
                          peerId,
                          activePeer,
                          ipVrfOwners,
                          configuredTopology,
                          establishedTopology));
                case DYNAMIC:
                  BgpPassivePeerConfig passivePeer = nc.getBgpDynamicPeerConfig(peerId);
                  assert passivePeer != null;
                  return getPassivePeerRows(
                      peerId, passivePeer, nc, configuredTopology, establishedTopology)
                      .stream();
                case UNNUMBERED:
                  BgpUnnumberedPeerConfig unnumPeer = nc.getBgpUnnumberedPeerConfig(peerId);
                  assert unnumPeer != null;
                  return Stream.of(
                      getUnnumberedPeerRow(
                          peerId, unnumPeer, configuredTopology, establishedTopology));
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
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> establishedTopology) {
    SessionType type = getSessionType(activePeer);

    // Check topologies to determine the peer's status and, if applicable, remote node
    BgpSessionStatus status = NOT_COMPATIBLE;
    Node remoteNode = null;
    if (establishedTopology.nodes().contains(activeId)
        && establishedTopology.outDegree(activeId) == 1) {
      status = ESTABLISHED;
      /*
      Find the remote node with which the session was established. Note that this is NOT necessarily
      the same as the remote node we would find from the configured topology, because the peer could
      have multiple compatible remotes of which only one turned out to be reachable.
       */
      String remoteNodeName =
          establishedTopology.adjacentNodes(activeId).iterator().next().getHostname();
      remoteNode = new Node(remoteNodeName);
    } else if (getConfiguredStatus(activeId, activePeer, type, ipVrfOwners, configuredTopology)
        == UNIQUE_MATCH) {
      status = NOT_ESTABLISHED;
      // This peer has a unique match, but it's unreachable. Show that remote peer's node.
      String remoteNodeName =
          configuredTopology.adjacentNodes(activeId).iterator().next().getHostname();
      remoteNode = new Node(remoteNodeName);
    }
    return Row.builder(METADATA_MAP)
        .put(COL_ESTABLISHED_STATUS, status)
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
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> establishedTopology) {
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

    // If peer is locally misconfigured (missing remote prefix or local/remote AS) generate one row
    if (getLocallyBrokenStatus(passivePeer) != null) {
      return ImmutableList.of(rb.put(COL_ESTABLISHED_STATUS, NOT_COMPATIBLE).build());
    }

    // Find all correctly configured remote peers compatible with this peer
    Set<BgpPeerConfigId> compatibleRemotes = configuredTopology.adjacentNodes(passiveId);

    // If no compatible neighbors exist, generate one NOT_ESTABLISHED row
    if (compatibleRemotes.isEmpty()) {
      return ImmutableList.of(rb.put(COL_ESTABLISHED_STATUS, NOT_ESTABLISHED).build());
    }

    // Find all remote peers that established a session with this peer. Passive peer will not be in
    // establishedBgpTopology at all it was invalid according to bgpConfigPassesSanityChecks()
    Set<BgpPeerConfigId> establishedRemotes =
        establishedTopology.nodes().contains(passiveId)
            ? establishedTopology.adjacentNodes(passiveId)
            : ImmutableSet.of();

    // Compatible remotes exist. Generate a row for each.
    return compatibleRemotes.stream()
        .map(
            remoteId -> {
              BgpSessionProperties sessionProps =
                  configuredTopology.edgeValue(passiveId, remoteId).orElse(null);
              assert sessionProps != null;
              BgpActivePeerConfig activeRemote = nc.getBgpPointToPointPeerConfig(remoteId);
              assert activeRemote != null;
              BgpSessionStatus status =
                  establishedRemotes.contains(remoteId) ? ESTABLISHED : NOT_ESTABLISHED;
              return rb.put(COL_ESTABLISHED_STATUS, status)
                  .put(COL_LOCAL_IP, sessionProps.getTailIp())
                  .put(COL_REMOTE_AS, LongSpace.of(activeRemote.getLocalAs()).toString())
                  .put(COL_REMOTE_NODE, new Node(remoteId.getHostname()))
                  .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, sessionProps.getHeadIp()))
                  .put(COL_SESSION_TYPE, sessionProps.getSessionType())
                  .build();
            })
        .collect(ImmutableList.toImmutableList());
  }

  @Nonnull
  @VisibleForTesting
  static Row getUnnumberedPeerRow(
      BgpPeerConfigId unnumId,
      BgpUnnumberedPeerConfig unnumPeer,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology,
      ValueGraph<BgpPeerConfigId, BgpSessionProperties> establishedTopology) {
    BgpSessionStatus status = NOT_COMPATIBLE;
    BgpPeerConfigId remoteId = null;
    if (establishedTopology.nodes().contains(unnumId)
        && establishedTopology.outDegree(unnumId) == 1) {
      status = ESTABLISHED;
      remoteId = establishedTopology.adjacentNodes(unnumId).iterator().next();
    } else if (getConfiguredStatus(unnumId, unnumPeer, configuredTopology) == UNIQUE_MATCH) {
      status = NOT_ESTABLISHED;
      remoteId = configuredTopology.adjacentNodes(unnumId).iterator().next();
    }

    // If there's enough info to identify a remote peer, get remote node and interface
    Node remoteNode = null;
    NodeInterfacePair remoteInterface = null;
    if (remoteId != null) {
      remoteNode = new Node(remoteId.getHostname());
      remoteInterface = NodeInterfacePair.of(remoteId.getHostname(), remoteId.getPeerInterface());
    }

    return Row.builder(METADATA_MAP)
        .put(COL_ESTABLISHED_STATUS, status)
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

  @VisibleForTesting
  static TableMetadata createMetadata(Question question) {
    String textDesc =
        String.format(
            "On ${%s} session ${%s}:${%s} has status ${%s}.",
            COL_NODE, COL_VRF, COL_REMOTE_IP, COL_ESTABLISHED_STATUS);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(COLUMN_METADATA, textDesc);
  }

  private static boolean matchesQuestionFilters(
      Row row, Set<String> nodes, Set<String> remoteNodes, BgpSessionStatusQuestion question) {
    return matchesNodesAndType(row, nodes, remoteNodes, question)
        && question.matchesStatus(
            BgpSessionStatus.parse((String) row.get(COL_ESTABLISHED_STATUS, Schema.STRING)));
  }
}

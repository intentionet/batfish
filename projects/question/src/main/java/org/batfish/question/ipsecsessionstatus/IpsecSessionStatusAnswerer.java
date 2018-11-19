package org.batfish.question.ipsecsessionstatus;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.IKE_PHASE1_FAILED;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.IKE_PHASE1_KEY_MISMATCH;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.IPSEC_PHASE2_FAILED;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.IPSEC_SESSION_ESTABLISHED;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.MISSING_END_POINT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.graph.ValueGraph;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.IpsecUtil;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IpsecDynamicPeerConfig;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPeerConfigId;
import org.batfish.datamodel.IpsecSession;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

class IpsecSessionStatusAnswerer extends Answerer {
  static final String COL_INITIATOR = "Node";
  static final String COL_INIT_INTERFACE = "Node_Interface";
  static final String COL_INIT_IP = "Node_IP";
  static final String COL_RESPONDER = "Remote_Node";
  static final String COL_RESPONDER_INTERFACE = "Remote_Node_Interface";
  static final String COL_RESPONDER_IP = "Remote_Node_IP";
  static final String COL_STATUS = "Status";
  static final String COL_TUNNEL_INTERFACES = "Tunnel_Interfaces";
  private static final String NOT_APPLICABLE = "Not Applicable";

  IpsecSessionStatusAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    IpsecSessionStatusQuestion question = (IpsecSessionStatusQuestion) _question;
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    NetworkConfigurations networkConfigurations = NetworkConfigurations.of(configurations);
    ValueGraph<IpsecPeerConfigId, IpsecSession> ipsecTopology =
        IpsecUtil.initIpsecTopology(configurations);

    Set<String> initiatorNodes = question.getInitiatorRegex().getMatchingNodes(_batfish);
    Set<String> responderNodes = question.getResponderRegex().getMatchingNodes(_batfish);

    TableAnswerElement answerElement = new TableAnswerElement(createTableMetaData(question));

    Multiset<IpsecSessionInfo> ipsecSessionInfos =
        rawAnswer(networkConfigurations, ipsecTopology, initiatorNodes, responderNodes);
    answerElement.postProcessAnswer(
        question,
        ipsecSessionInfos
            .stream()
            .filter(
                ipsecSessionInfo ->
                    question.matchesStatus(ipsecSessionInfo.getIpsecSessionStatus()))
            .map(IpsecSessionStatusAnswerer::toRow)
            .collect(Collectors.toCollection(HashMultiset::create)));
    return answerElement;
  }

  @VisibleForTesting
  static Multiset<IpsecSessionInfo> rawAnswer(
      NetworkConfigurations networkConfigurations,
      ValueGraph<IpsecPeerConfigId, IpsecSession> ipsecTopology,
      Set<String> initiatorNodes,
      Set<String> responderNodes) {
    Multiset<IpsecSessionInfo> ipsecSessionInfos = HashMultiset.create();

    for (IpsecPeerConfigId node : ipsecTopology.nodes()) {
      IpsecPeerConfig ipsecPeerConfig = networkConfigurations.getIpsecPeerConfig(node);
      if (ipsecPeerConfig == null
          || ipsecPeerConfig instanceof IpsecDynamicPeerConfig
          || !initiatorNodes.contains(node.getHostName())) {
        continue;
      }
      IpsecSessionInfo.Builder ipsecSessionInfoBuilder = IpsecSessionInfo.builder();

      ipsecSessionInfoBuilder.setInitiatorHostname(node.getHostName());
      ipsecSessionInfoBuilder.setInitiatorInterface(ipsecPeerConfig.getPhysicalInterface());
      ipsecSessionInfoBuilder.setInitiatorIp(ipsecPeerConfig.getLocalAddress());
      ipsecSessionInfoBuilder.setInitiatorTunnelInterface(ipsecPeerConfig.getTunnelInterface());

      Set<IpsecPeerConfigId> neighbors = ipsecTopology.adjacentNodes(node);

      if (neighbors.isEmpty()) {
        ipsecSessionInfoBuilder.setIpsecSessionStatus(MISSING_END_POINT);
        ipsecSessionInfos.add(ipsecSessionInfoBuilder.build());
        continue;
      }

      for (IpsecPeerConfigId neighbor : neighbors) {
        if (!responderNodes.contains(neighbor.getHostName())) {
          continue;
        }
        IpsecSession ipsecSession = ipsecTopology.edgeValueOrDefault(node, neighbor, null);
        if (ipsecSession == null) {
          continue;
        }
        IpsecPeerConfig ipsecPeerConfigNeighbor =
            networkConfigurations.getIpsecPeerConfig(neighbor);
        if (ipsecPeerConfigNeighbor == null) {
          continue;
        }
        processNeighbor(neighbor, ipsecSessionInfoBuilder, ipsecPeerConfigNeighbor, ipsecSession);
        ipsecSessionInfos.add(ipsecSessionInfoBuilder.build());
      }
    }
    return ipsecSessionInfos;
  }

  private static void processNeighbor(
      IpsecPeerConfigId ipsecPeerConfigIdNeighbor,
      IpsecSessionInfo.Builder ipsecSessioningInfoBuilder,
      IpsecPeerConfig ipsecPeerConfigNeighbor,
      IpsecSession ipsecSession) {

    ipsecSessioningInfoBuilder.setResponderHostname(ipsecPeerConfigIdNeighbor.getHostName());
    ipsecSessioningInfoBuilder.setResponderInterface(
        ipsecPeerConfigNeighbor.getPhysicalInterface());
    ipsecSessioningInfoBuilder.setResponderIp(ipsecPeerConfigNeighbor.getLocalAddress());
    ipsecSessioningInfoBuilder.setResponderTunnelInterface(
        ipsecPeerConfigNeighbor.getTunnelInterface());

    if (ipsecSession.getNegotiatedIkeP1Proposal() == null) {
      ipsecSessioningInfoBuilder.setIpsecSessionStatus(IKE_PHASE1_FAILED);
    } else if (ipsecSession.getNegotiatedIkeP1Key() == null) {
      ipsecSessioningInfoBuilder.setIpsecSessionStatus(IKE_PHASE1_KEY_MISMATCH);
    } else if (ipsecSession.getNegotiatedIpsecP2Proposal() == null) {
      ipsecSessioningInfoBuilder.setIpsecSessionStatus(IPSEC_PHASE2_FAILED);
    } else {
      ipsecSessioningInfoBuilder.setIpsecSessionStatus(IPSEC_SESSION_ESTABLISHED);
    }
  }

  /**
   * Creates a {@link Row} object from the corresponding {@link IpsecSessionInfo} object.
   *
   * @param info input {@link IpsecSessionInfo}
   * @return The output {@link Row}
   */
  @VisibleForTesting
  static Row toRow(@Nonnull IpsecSessionInfo info) {
    RowBuilder row = Row.builder();
    row.put(COL_INITIATOR, new Node(info.getInitiatorHostname()))
        .put(
            COL_INIT_INTERFACE,
            new NodeInterfacePair(
                info.getInitiatorHostname(), String.format("%s", info.getInitiatorInterface())))
        .put(COL_INIT_IP, info.getInitiatorIp())
        .put(
            COL_RESPONDER,
            info.getResponderHostname() == null ? null : new Node(info.getResponderHostname()))
        .put(
            COL_RESPONDER_INTERFACE,
            info.getResponderHostname() != null && info.getResponderInterface() != null
                ? new NodeInterfacePair(
                    info.getResponderHostname(), String.format("%s", info.getResponderInterface()))
                : null)
        .put(COL_RESPONDER_IP, info.getResponderIp())
        .put(
            COL_TUNNEL_INTERFACES,
            info.getInitiatorTunnelInterface() == null && info.getResponderTunnelInterface() == null
                ? NOT_APPLICABLE
                : String.format(
                    "%s->%s",
                    firstNonNull(info.getInitiatorTunnelInterface(), ""),
                    firstNonNull(info.getResponderTunnelInterface(), "")))
        .put(COL_STATUS, info.getIpsecSessionStatus());
    return row.build();
  }

  /** Create table metadata for this answer. */
  private static TableMetadata createTableMetaData(Question question) {
    List<ColumnMetadata> columnMetadata = getColumnMetadata();
    String textDesc =
        String.format(
            " IPSec peering session between initiator ${%s} with interface {%s} and IP ${%s} and responder ${%s} with interface {%s} and IP ${%s}s has status ${%s}.",
            COL_INITIATOR,
            COL_INIT_INTERFACE,
            COL_INIT_IP,
            COL_RESPONDER,
            COL_RESPONDER_INTERFACE,
            COL_RESPONDER_IP,
            COL_STATUS);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(columnMetadata, textDesc);
  }

  /** Create column metadata. */
  private static List<ColumnMetadata> getColumnMetadata() {
    return ImmutableList.of(
        new ColumnMetadata(COL_INITIATOR, Schema.NODE, "IPSec initiator"),
        new ColumnMetadata(COL_INIT_INTERFACE, Schema.INTERFACE, "Initiator Interface"),
        new ColumnMetadata(COL_INIT_IP, Schema.IP, "Initiator IP"),
        new ColumnMetadata(COL_RESPONDER, Schema.NODE, "IPSec responder"),
        new ColumnMetadata(COL_RESPONDER_INTERFACE, Schema.INTERFACE, "Responder Interface"),
        new ColumnMetadata(COL_RESPONDER_IP, Schema.IP, "Responder IP"),
        new ColumnMetadata(
            COL_TUNNEL_INTERFACES, Schema.STRING, "Tunnel interfaces pair used in peering session"),
        new ColumnMetadata(COL_STATUS, Schema.STRING, "IPSec session status"));
  }
}

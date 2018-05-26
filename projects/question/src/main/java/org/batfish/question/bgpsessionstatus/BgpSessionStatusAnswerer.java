package org.batfish.question.bgpsessionstatus;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.graph.Network;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.question.bgpsessionstatus.BgpSessionInfo.BgpSessionInfoBuilder;
import org.batfish.question.bgpsessionstatus.BgpSessionInfo.SessionStatus;
import org.batfish.question.bgpsessionstatus.BgpSessionInfo.SessionType;

public class BgpSessionStatusAnswerer extends Answerer {

  public static final String COL_CONFIGURED_STATUS = "configuredStatus";
  public static final String COL_ESTABLISHED_NEIGHBORS = "establishedNeighbors";
  public static final String COL_LOCAL_IP = "localIp";
  public static final String COL_NODE = "node";
  public static final String COL_ON_LOOPBACK = "onLoopback";
  public static final String COL_REMOTE_NODE = "remoteNode";
  public static final String COL_REMOTE_PREFIX = "remotePrefix";
  public static final String COL_SESSION_TYPE = "sessionType";
  public static final String COL_VRF_NAME = "vrfName";

  /** Answerer for the BGP Session status question (new version). */
  public BgpSessionStatusAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    BgpSessionStatusQuestion question = (BgpSessionStatusQuestion) _question;
    Multiset<BgpSessionInfo> sessions = rawAnswer(question);
    TableAnswerElement answer =
        new TableAnswerElement(BgpSessionStatusAnswerer.createMetadata(question));
    answer.postProcessAnswer(
        question,
        sessions
            .stream()
            .map(BgpSessionStatusAnswerer::toRow)
            .collect(Collectors.toCollection(HashMultiset::create)));
    return answer;
  }

  private static boolean node2RegexMatchesIp(
      Ip ip, Map<Ip, Set<String>> ipOwners, Set<String> includeNodes2) {
    Set<String> owners = ipOwners.get(ip);
    if (owners == null) {
      throw new BatfishException("Expected at least one owner of ip: " + ip);
    }
    return !Sets.intersection(includeNodes2, owners).isEmpty();
  }

  public Multiset<BgpSessionInfo> rawAnswer(BgpSessionStatusQuestion question) {
    Multiset<BgpSessionInfo> sessions = HashMultiset.create();
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Set<String> includeNodes1 = question.getNode1Regex().getMatchingNodes(_batfish);
    Set<String> includeNodes2 = question.getNode2Regex().getMatchingNodes(_batfish);

    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    Set<Ip> allInterfaceIps = ipOwners.keySet();

    Network<BgpNeighbor, BgpSession> configuredBgpTopology =
        CommonUtil.initBgpTopology(configurations, ipOwners, true);

    Network<BgpNeighbor, BgpSession> establishedBgpTopology =
        question.getIncludeEstablishedCount()
            ? CommonUtil.initBgpTopology(
                configurations,
                ipOwners,
                false,
                true,
                _batfish.getDataPlanePlugin().getTracerouteEngine(),
                _batfish.loadDataPlane())
            : null;

    for (BgpNeighbor bgpNeighbor : configuredBgpTopology.nodes()) {
      String hostname = bgpNeighbor.getOwner().getHostname();
      String vrfName = bgpNeighbor.getVrf();
      // Only match nodes we care about
      if (!includeNodes1.contains(hostname)) {
        continue;
      }

      // Match foreign group
      boolean foreign =
          bgpNeighbor.getGroup() != null && question.matchesForeignGroup(bgpNeighbor.getGroup());
      if (foreign) {
        continue;
      }

      // Setup session info
      boolean ebgp = !Objects.equals(bgpNeighbor.getRemoteAs(), bgpNeighbor.getLocalAs());
      boolean ebgpMultihop = bgpNeighbor.getEbgpMultihop();
      Prefix remotePrefix = bgpNeighbor.getPrefix();
      SessionType sessionType =
          ebgp
              ? ebgpMultihop ? SessionType.EBGP_MULTIHOP : SessionType.EBGP_SINGLEHOP
              : SessionType.IBGP;
      // Skip session types we don't care about
      if (!question.matchesType(sessionType)) {
        continue;
      }
      BgpSessionInfoBuilder bsiBuilder =
          new BgpSessionInfoBuilder(hostname, vrfName, remotePrefix, sessionType);

      SessionStatus configuredStatus;

      Ip localIp = bgpNeighbor.getLocalIp();
      if (bgpNeighbor.getDynamic()) {
        configuredStatus = SessionStatus.DYNAMIC_LISTEN;
      } else if (localIp == null) {
        configuredStatus = SessionStatus.NO_LOCAL_IP;
      } else {
        bsiBuilder.withLocalIp(localIp);
        bsiBuilder.withOnLoopback(
            CommonUtil.isActiveLoopbackIp(localIp, configurations.get(hostname)));

        Ip remoteIp = bgpNeighbor.getAddress();

        if (!allInterfaceIps.contains(localIp)) {
          configuredStatus = SessionStatus.INVALID_LOCAL_IP;
        } else if (remoteIp == null || !allInterfaceIps.contains(remoteIp)) {
          configuredStatus = SessionStatus.UNKNOWN_REMOTE;
        } else {
          if (!node2RegexMatchesIp(remoteIp, ipOwners, includeNodes2)) {
            continue;
          }
          if (configuredBgpTopology.adjacentNodes(bgpNeighbor).isEmpty()) {
            configuredStatus = SessionStatus.HALF_OPEN;
            // degree > 2 because of directed edges. 1 edge in, 1 edge out == single connection
          } else if (configuredBgpTopology.degree(bgpNeighbor) > 2) {
            configuredStatus = SessionStatus.MULTIPLE_REMOTES;
          } else {
            BgpNeighbor remoteNeighbor =
                configuredBgpTopology.adjacentNodes(bgpNeighbor).iterator().next();
            bsiBuilder.withRemoteNode(remoteNeighbor.getOwner().getHostname());
            configuredStatus = SessionStatus.UNIQUE_MATCH;
          }
        }
      }
      if (!question.matchesStatus(configuredStatus)) {
        continue;
      }

      bsiBuilder.withConfiguredStatus(configuredStatus);

      bsiBuilder.withEstablishedNeighbors(
          establishedBgpTopology != null && establishedBgpTopology.nodes().contains(bgpNeighbor)
              ? establishedBgpTopology.inDegree(bgpNeighbor)
              : -1);

      sessions.add(bsiBuilder.build());
    }

    return sessions;
  }

  public static TableMetadata createMetadata(Question question) {
    List<ColumnMetadata> columnMetadata =
        ImmutableList.of(
            new ColumnMetadata(
                COL_NODE, Schema.NODE, "The node where this session is configured", true, false),
            new ColumnMetadata(
                COL_LOCAL_IP, Schema.IP, "The local IP of the session", false, false),
            new ColumnMetadata(
                COL_VRF_NAME,
                Schema.STRING,
                "The VRF in which this session is configured",
                true,
                false),
            new ColumnMetadata(
                COL_REMOTE_NODE, Schema.NODE, "Remote node for this session", false, false),
            new ColumnMetadata(
                COL_REMOTE_PREFIX, Schema.PREFIX, "Remote prefix for this session", true, false),
            new ColumnMetadata(
                COL_SESSION_TYPE, Schema.STRING, "The type of this session", false, false),
            new ColumnMetadata(
                COL_CONFIGURED_STATUS, Schema.STRING, "Configured status", false, true),
            new ColumnMetadata(
                COL_ESTABLISHED_NEIGHBORS,
                Schema.INTEGER,
                "Number of neighbors with whom BGP session was established",
                false,
                true),
            new ColumnMetadata(
                COL_ON_LOOPBACK,
                Schema.BOOLEAN,
                "Whether the session was established on loopback interface",
                false,
                true));

    DisplayHints dhints = question.getDisplayHints();
    if (dhints == null) {
      dhints = new DisplayHints();
      dhints.setTextDesc(
          String.format(
              "On ${%s} session ${%s}:${%s} has configured status ${%s}.",
              COL_NODE, COL_VRF_NAME, COL_REMOTE_PREFIX, COL_CONFIGURED_STATUS));
    }
    return new TableMetadata(columnMetadata, dhints);
  }

  /**
   * Creates a {@link BgpSessionInfo} object from the corresponding {@link Row} object.
   *
   * @param row The input row
   * @return The output object
   */
  public static BgpSessionInfo fromRow(Row row) {
    Ip localIp = row.get(COL_LOCAL_IP, Ip.class);
    SessionStatus configuredStatus = row.get(COL_CONFIGURED_STATUS, SessionStatus.class);
    Integer establishedNeighbors = row.get(COL_ESTABLISHED_NEIGHBORS, Integer.class);
    Boolean onLoopback = row.get(COL_ON_LOOPBACK, Boolean.class);
    Node node = row.get(COL_NODE, Node.class);
    Node remoteNode = row.get(COL_REMOTE_NODE, Node.class);
    Prefix remotePrefix = row.get(COL_REMOTE_PREFIX, Prefix.class);
    SessionType sessionType = row.get(COL_SESSION_TYPE, SessionType.class);
    String vrfName = row.get(COL_VRF_NAME, String.class);

    return new BgpSessionInfo(
        configuredStatus,
        establishedNeighbors,
        node.getName(),
        localIp,
        onLoopback,
        remoteNode.getName(),
        remotePrefix,
        sessionType,
        vrfName);
  }

  /**
   * Creates a {@link Row} object from the corresponding {@link BgpSessionInfo} object.
   *
   * @param info The input object
   * @return The output row
   */
  public static Row toRow(BgpSessionInfo info) {
    RowBuilder row = Row.builder();
    row.put(COL_CONFIGURED_STATUS, info.getConfiguredStatus())
        .put(COL_ESTABLISHED_NEIGHBORS, info.getEstablishedNeighbors())
        .put(COL_LOCAL_IP, info.getLocalIp())
        .put(COL_NODE, new Node(info.getNodeName()))
        .put(COL_ON_LOOPBACK, info.getOnLoopback())
        .put(COL_REMOTE_NODE, info.getRemoteNode() == null ? null : new Node(info.getRemoteNode()))
        .put(COL_REMOTE_PREFIX, info.getRemotePrefix())
        .put(COL_SESSION_TYPE, info.getSessionType())
        .put(COL_VRF_NAME, info.getVrfName());
    return row.build();
  }
}

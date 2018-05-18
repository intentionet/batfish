package org.batfish.question.bgpsessionstatus;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.graph.Network;
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
import org.batfish.datamodel.questions.Question;
import org.batfish.question.bgpsessionstatus.BgpSessionInfo.BgpSessionInfoBuilder;
import org.batfish.question.bgpsessionstatus.BgpSessionInfo.SessionStatus;
import org.batfish.question.bgpsessionstatus.BgpSessionInfo.SessionType;

public class BgpSessionStatusAnswerer extends Answerer {

  /** Answerer for the BGP Session status question (new version). */
  public BgpSessionStatusAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    BgpSessionStatusQuestion question = (BgpSessionStatusQuestion) _question;
    Multiset<BgpSessionInfo> sessions = rawAnswer(question);
    BgpSessionStatusAnswerElement answer =
        new BgpSessionStatusAnswerElement(BgpSessionStatusAnswerElement.createMetadata(question));
    answer.postProcessAnswer(
        question,
        sessions
            .stream()
            .map(BgpSessionStatusAnswerElement::toRow)
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
}

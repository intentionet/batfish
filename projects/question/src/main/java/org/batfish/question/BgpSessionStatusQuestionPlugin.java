package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableSortedSet.Builder;
import com.google.common.collect.Sets;
import com.google.common.graph.ValueGraph;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.bgp.BgpTopologyUtils;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.bgpsessionstatus.BgpSessionStatusPlugin;

/** @deprecated in favor of {@link BgpSessionStatusPlugin} */
@AutoService(Plugin.class)
@Deprecated
public class BgpSessionStatusQuestionPlugin extends QuestionPlugin {

  public enum SessionType {
    IBGP,
    EBGP_SINGLEHOP,
    EBGP_MULTIHOP,
    UNKNOWN
  }

  public enum SessionStatus {
    // ordered by how we evaluate status
    PASSIVE,
    MISSING_LOCAL_IP,
    UNKNOWN_LOCAL_IP,
    UNKNOWN_REMOTE_IP,
    HALF_OPEN,
    MULTIPLE_REMOTES,
    UNIQUE_MATCH,
  }

  public static class BgpSessionInfo implements Comparable<BgpSessionInfo> {

    private static final String PROP_HOSTNAME = "hostname";
    private static final String PROP_LOCAL_IP = "localIp";
    private static final String PROP_ON_LOOPBACK = "onLoopback";
    private static final String PROP_REMOTE_PREFIX = "remotePrefix";
    private static final String PROP_REMOTE_NODE = "remoteNode";
    private static final String PROP_STATUS = "status";
    private static final String PROP_SESSION_TYPE = "sessionType";
    private static final String PROP_VRF_NAME = "vrfName";

    @JsonProperty(PROP_HOSTNAME)
    private String _hostname;

    @JsonProperty(PROP_VRF_NAME)
    private String _vrfName;

    @JsonProperty(PROP_REMOTE_PREFIX)
    private Prefix _remotePrefix;

    @JsonProperty(PROP_LOCAL_IP)
    Ip _localIp;

    @JsonProperty(PROP_ON_LOOPBACK)
    Boolean _onLoopback;

    @JsonProperty(PROP_REMOTE_NODE)
    String _remoteNode;

    @JsonProperty(PROP_STATUS)
    SessionStatus _status;

    @JsonProperty(PROP_SESSION_TYPE)
    SessionType _sessionType;

    @JsonCreator
    public BgpSessionInfo(
        @JsonProperty(PROP_HOSTNAME) String hostname,
        @JsonProperty(PROP_VRF_NAME) String vrfName,
        @JsonProperty(PROP_REMOTE_PREFIX) Prefix remotePrefix,
        @JsonProperty(PROP_LOCAL_IP) Ip localIp,
        @JsonProperty(PROP_ON_LOOPBACK) Boolean onLoopback,
        @JsonProperty(PROP_REMOTE_NODE) String remoteNode,
        @JsonProperty(PROP_STATUS) SessionStatus status,
        @JsonProperty(PROP_SESSION_TYPE) SessionType sessionType) {
      _hostname = hostname;
      _vrfName = vrfName;
      _remotePrefix = remotePrefix;
      _localIp = localIp;
      _onLoopback = onLoopback;
      _remoteNode = remoteNode;
      _status = status;
      _sessionType = sessionType;
    }

    BgpSessionInfo(String hostname, String vrfName, Prefix remotePrefix) {
      this._hostname = hostname;
      this._vrfName = vrfName;
      this._remotePrefix = remotePrefix;
    }

    @JsonProperty(PROP_HOSTNAME)
    public String getHostname() {
      return _hostname;
    }

    @JsonProperty(PROP_VRF_NAME)
    public String getVrfName() {
      return _vrfName;
    }

    @JsonProperty(PROP_REMOTE_PREFIX)
    public Prefix getRemotePrefix() {
      return _remotePrefix;
    }

    @Override
    public int compareTo(@Nonnull BgpSessionInfo o) {
      return Comparator.comparing(BgpSessionInfo::getHostname)
          .thenComparing(BgpSessionInfo::getVrfName)
          .thenComparing(BgpSessionInfo::getRemotePrefix)
          .compare(this, o);
    }

    @Override
    public String toString() {
      return String.format(
          "%s vrf=%s remote=%s type=%s loopback=%s status=%s localIp=%s remoteNode=%s",
          _hostname,
          _vrfName,
          _remotePrefix,
          _sessionType,
          _onLoopback,
          _status,
          _localIp,
          _remoteNode);
    }
  }

  public static class BgpSessionStatusAnswerElement extends AnswerElement {

    private static final String PROP_BGP_SESSIONS = "bgpSessions";

    private SortedSet<BgpSessionInfo> _bgpSessions;

    public BgpSessionStatusAnswerElement() {
      _bgpSessions = ImmutableSortedSet.of();
    }

    @JsonGetter(PROP_BGP_SESSIONS)
    public SortedSet<BgpSessionInfo> getBgpSessions() {
      return _bgpSessions;
    }

    @JsonSetter(PROP_BGP_SESSIONS)
    void setBgpSessions(SortedSet<BgpSessionInfo> bgpSessions) {
      _bgpSessions = ImmutableSortedSet.copyOf(bgpSessions);
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder();
      _bgpSessions.forEach(session -> sb.append(" ").append(session).append("\n"));
      return sb.toString();
    }
  }

  public static class BgpSessionStatusAnswerer extends Answerer {

    BgpSessionStatusAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {

      BgpSessionStatusQuestion question = (BgpSessionStatusQuestion) _question;

      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      Set<String> includeNodes1 = question.getNode1Regex().getMatchingNodes(_batfish);
      Set<String> includeNodes2 = question.getNode2Regex().getMatchingNodes(_batfish);

      BgpSessionStatusAnswerElement answerElement = new BgpSessionStatusAnswerElement();
      Set<Ip> allInterfaceIps = new HashSet<>();
      Set<Ip> loopbackIps = new HashSet<>();
      Map<Ip, Set<String>> ipOwners = new HashMap<>();
      // TODO: refactor this out into CommonUtil
      for (Configuration c : configurations.values()) {
        for (Interface i : c.getAllInterfaces().values()) {
          if (i.getActive() && i.getAddress() != null) {
            for (InterfaceAddress address : i.getAllAddresses()) {
              Ip ip = address.getIp();
              if (i.isLoopback(c.getConfigurationFormat())) {
                loopbackIps.add(ip);
              }
              allInterfaceIps.add(ip);
              Set<String> currentIpOwners = ipOwners.computeIfAbsent(ip, k -> new HashSet<>());
              currentIpOwners.add(c.getHostname());
            }
          }
        }
      }

      ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
          BgpTopologyUtils.initBgpTopology(configurations, ipOwners, true);

      Builder<BgpSessionInfo> bgpSessionInfoBuilder = new Builder<>(Comparator.naturalOrder());
      NetworkConfigurations nc = NetworkConfigurations.of(configurations);
      for (BgpPeerConfigId bgpPeerConfigId : bgpTopology.nodes()) {
        BgpPeerConfig bgpPeerConfig = nc.getBgpPeerConfig(bgpPeerConfigId);
        String hostname = bgpPeerConfigId.getHostname();
        String vrfName = bgpPeerConfigId.getVrfName();
        // Only match nodes we care about
        if (!includeNodes1.contains(hostname)) {
          continue;
        }

        // Match foreign group
        boolean foreign =
            bgpPeerConfig.getGroup() != null
                && question.matchesForeignGroup(bgpPeerConfig.getGroup());
        if (foreign) {
          continue;
        }

        boolean ebgpMultihop = bgpPeerConfig.getEbgpMultihop();
        Prefix remotePrefix = bgpPeerConfigId.getRemotePeerPrefix();
        BgpSessionInfo bgpSessionInfo = new BgpSessionInfo(hostname, vrfName, remotePrefix);
        // Setup session info
        // TODO(https://github.com/batfish/batfish/issues/1331): Handle list of remote ASes. Until
        // then, we can't assume remote AS will always be a single integer that is present and
        // non-null.
        bgpSessionInfo._sessionType = SessionType.UNKNOWN;
        if (!bgpPeerConfigId.isDynamic()) {
          Long remoteAs = ((BgpActivePeerConfig) bgpPeerConfig).getRemoteAs();
          if (remoteAs != null && !remoteAs.equals(bgpPeerConfig.getLocalAs())) {
            bgpSessionInfo._sessionType =
                ebgpMultihop ? SessionType.EBGP_MULTIHOP : SessionType.EBGP_SINGLEHOP;
          } else if (remoteAs != null) {
            bgpSessionInfo._sessionType = SessionType.IBGP;
          }
        }

        // Skip session types we don't care about
        if (!question.matchesType(bgpSessionInfo._sessionType)) {
          continue;
        }

        Ip localIp = bgpPeerConfig.getLocalIp();
        if (bgpPeerConfigId.isDynamic()) {
          bgpSessionInfo._status = SessionStatus.PASSIVE;
        } else if (localIp == null) {
          bgpSessionInfo._status = SessionStatus.MISSING_LOCAL_IP;
        } else {
          bgpSessionInfo._localIp = localIp;
          bgpSessionInfo._onLoopback = loopbackIps.contains(localIp);
          Ip remoteIp = ((BgpActivePeerConfig) bgpPeerConfig).getPeerAddress();

          if (!allInterfaceIps.contains(localIp)) {
            bgpSessionInfo._status = SessionStatus.UNKNOWN_LOCAL_IP;
          } else if (remoteIp == null || !allInterfaceIps.contains(remoteIp)) {
            bgpSessionInfo._status = SessionStatus.UNKNOWN_REMOTE_IP;
          } else {
            if (!node2RegexMatchesIp(remoteIp, ipOwners, includeNodes2)) {
              continue;
            }
            if (bgpTopology.adjacentNodes(bgpPeerConfigId).isEmpty()) {
              bgpSessionInfo._status = SessionStatus.HALF_OPEN;
              // degree > 2 because of directed edges. 1 edge in, 1 edge out == single connection
            } else if (bgpTopology.degree(bgpPeerConfigId) > 2) {
              bgpSessionInfo._status = SessionStatus.MULTIPLE_REMOTES;
            } else {
              BgpPeerConfigId remoteNeighborId =
                  bgpTopology.adjacentNodes(bgpPeerConfigId).iterator().next();
              bgpSessionInfo._remoteNode = remoteNeighborId.getHostname();
              bgpSessionInfo._status = SessionStatus.UNIQUE_MATCH;
            }
          }
        }
        if (question.matchesStatus(bgpSessionInfo._status)) {
          bgpSessionInfoBuilder.add(bgpSessionInfo);
        }
      }
      answerElement.setBgpSessions(bgpSessionInfoBuilder.build());
      return answerElement;
    }

    private static boolean node2RegexMatchesIp(
        Ip ip, Map<Ip, Set<String>> ipOwners, Set<String> includeNodes2) {
      Set<String> owners = ipOwners.get(ip);
      if (owners == null) {
        throw new BatfishException("Expected at least one owner of ip: " + ip);
      }
      return !Sets.intersection(includeNodes2, owners).isEmpty();
    }
  }

  /**
   * Returns the status of BGP sessions.
   *
   * <p>Based on config data, determines the status of IBGP and EBGP sessions
   */
  public static class BgpSessionStatusQuestion extends Question {

    private static final String PROP_FOREIGN_BGP_GROUPS = "foreignBgpGroups";

    private static final String PROP_NODE1_REGEX = "node1Regex";

    private static final String PROP_NODE2_REGEX = "node2Regex";

    private static final String PROP_STATUS = "status";

    private static final String PROP_TYPE_REGEX = "type";

    @Nonnull private SortedSet<String> _foreignBgpGroups;

    @Nonnull private NodesSpecifier _node1Regex;

    @Nonnull private NodesSpecifier _node2Regex;

    @Nonnull private Pattern _statusRegex;

    @Nonnull private Pattern _typeRegex;

    @JsonCreator
    public BgpSessionStatusQuestion(
        @JsonProperty(PROP_FOREIGN_BGP_GROUPS) SortedSet<String> foreignBgpGroups,
        @JsonProperty(PROP_NODE1_REGEX) NodesSpecifier regex1,
        @JsonProperty(PROP_NODE2_REGEX) NodesSpecifier regex2,
        @JsonProperty(PROP_STATUS) String statusRegex,
        @JsonProperty(PROP_TYPE_REGEX) String type) {
      _foreignBgpGroups = foreignBgpGroups == null ? new TreeSet<>() : foreignBgpGroups;
      _node1Regex = regex1 == null ? NodesSpecifier.ALL : regex1;
      _node2Regex = regex2 == null ? NodesSpecifier.ALL : regex2;
      _statusRegex =
          Strings.isNullOrEmpty(statusRegex)
              ? Pattern.compile(".*")
              : Pattern.compile(statusRegex.toUpperCase());
      _typeRegex =
          Strings.isNullOrEmpty(type) ? Pattern.compile(".*") : Pattern.compile(type.toUpperCase());
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @JsonProperty(PROP_FOREIGN_BGP_GROUPS)
    private SortedSet<String> getForeignBgpGroups() {
      return _foreignBgpGroups;
    }

    @Override
    public String getName() {
      return "bgpsessionstatus";
    }

    @JsonProperty(PROP_NODE1_REGEX)
    public NodesSpecifier getNode1Regex() {
      return _node1Regex;
    }

    @JsonProperty(PROP_NODE2_REGEX)
    public NodesSpecifier getNode2Regex() {
      return _node2Regex;
    }

    @JsonProperty(PROP_STATUS)
    private String getStatusRegex() {
      return _statusRegex.toString();
    }

    @JsonProperty(PROP_TYPE_REGEX)
    private String getTypeRegex() {
      return _typeRegex.toString();
    }

    boolean matchesForeignGroup(String group) {
      return _foreignBgpGroups.contains(group);
    }

    boolean matchesStatus(SessionStatus status) {
      return _statusRegex.matcher(status.toString()).matches();
    }

    boolean matchesType(SessionType type) {
      return _typeRegex.matcher(type.toString()).matches();
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new BgpSessionStatusAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new BgpSessionStatusQuestion(null, null, null, null, null);
  }
}

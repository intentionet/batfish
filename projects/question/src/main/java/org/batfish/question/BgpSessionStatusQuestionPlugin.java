package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class BgpSessionStatusQuestionPlugin extends QuestionPlugin {

  public enum SessionType {
    IBGP,
    EBGP_SINGLEHOP,
    EBGP_MULTIHOP
  }

  public enum SessionStatus {
    // ordered by how we evaluate status
    MISSING_LOCAL_IP,
    UNKNOWN_LOCAL_IP,
    UNKNOWN_REMOTE_IP,
    HALF_OPEN,
    MULTIPLE_REMOTES,
    UNIQUE_MATCH,
  }

  public static class BgpSession {
    @JsonProperty("hostname")
    public String hostname;

    @JsonProperty("vrfName")
    public String vrfName;

    @JsonProperty("localIp")
    public Ip localIp;

    @JsonProperty("onLoopback")
    public Boolean onLoopback;

    @JsonProperty("remoteNode")
    public String remoteNode;

    @JsonProperty("remotePrefix")
    public Prefix remotePrefix;

    @JsonProperty("status")
    public SessionStatus status;

    @JsonProperty("sessionType")
    public SessionType sessionType;

    @JsonCreator
    private BgpSession() {}

    public BgpSession(String hostname, String vrfName, Prefix remotePrefix) {
      this.hostname = hostname;
      this.vrfName = vrfName;
      this.remotePrefix = remotePrefix;
    }

    @Override
    public String toString() {
      return String.format(
          "%s vrf=%s remote=%s type=%s loopback=%s status=%s localIp=%s remoteNode=%s",
          hostname, vrfName, remotePrefix, sessionType, onLoopback, status, localIp, remoteNode);
    }
  }

  public static class BgpSessionStatusAnswerElement implements AnswerElement {

    private static final String PROP_BGP_SESSIONS = "bgpSessions";

    private Set<BgpSession> _bgpSessions;

    public BgpSessionStatusAnswerElement() {
      _bgpSessions = new HashSet<>();
    }

    @JsonProperty(PROP_BGP_SESSIONS)
    public Set<BgpSession> getBgpSessions() {
      return _bgpSessions;
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder();
      _bgpSessions.forEach(session -> sb.append(" " + session + "\n"));
      return sb.toString();
    }
  }

  public static class BgpSessionStatusAnswerer extends Answerer {

    public BgpSessionStatusAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {

      BgpSessionStatusQuestion question = (BgpSessionStatusQuestion) _question;

      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      Set<String> includeNodes1 = question.getNode1Regex().getMatchingNodes(configurations);
      Set<String> includeNodes2 = question.getNode2Regex().getMatchingNodes(configurations);

      BgpSessionStatusAnswerElement answerElement = new BgpSessionStatusAnswerElement();
      Set<Ip> allInterfaceIps = new HashSet<>();
      Set<Ip> loopbackIps = new HashSet<>();
      Map<Ip, Set<String>> ipOwners = new HashMap<>();
      for (Configuration c : configurations.values()) {
        for (Interface i : c.getInterfaces().values()) {
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
      CommonUtil.initRemoteBgpNeighbors(configurations, ipOwners);
      for (Configuration co : configurations.values()) {
        String hostname = co.getHostname();
        if (!includeNodes1.contains(hostname)) {
          continue;
        }
        for (Vrf vrf : co.getVrfs().values()) {
          String vrfName = vrf.getName();
          BgpProcess proc = vrf.getBgpProcess();

          if (proc != null) {
            for (BgpNeighbor bgpNeighbor : proc.getNeighbors().values()) {
              boolean ebgp = !bgpNeighbor.getRemoteAs().equals(bgpNeighbor.getLocalAs());
              boolean foreign =
                  bgpNeighbor.getGroup() != null
                      && question.matchesForeignGroup(bgpNeighbor.getGroup());
              boolean ebgpMultihop = bgpNeighbor.getEbgpMultihop();
              Ip localIp = bgpNeighbor.getLocalIp();
              Ip remoteIp = bgpNeighbor.getAddress();
              if (bgpNeighbor.getPrefix().getPrefixLength() != 32) { // why is this here?
                continue;
              }
              if (foreign) {
                continue;
              }
              BgpSession bgpSession = new BgpSession(hostname, vrfName, bgpNeighbor.getPrefix());

              bgpSession.sessionType =
                  ebgp
                      ? ebgpMultihop ? SessionType.EBGP_MULTIHOP : SessionType.EBGP_SINGLEHOP
                      : SessionType.IBGP;
              if (!question.matchesType(bgpSession.sessionType)) {
                continue;
              }

              if (localIp == null) {
                bgpSession.status = SessionStatus.MISSING_LOCAL_IP;
              } else {
                bgpSession.localIp = localIp;
                bgpSession.onLoopback = loopbackIps.contains(localIp);

                if (!allInterfaceIps.contains(localIp)) {
                  bgpSession.status = SessionStatus.UNKNOWN_LOCAL_IP;
                } else if (!allInterfaceIps.contains(remoteIp)) {
                  bgpSession.status = SessionStatus.UNKNOWN_REMOTE_IP;
                } else {
                  if (node2RegexMatchesIp(remoteIp, ipOwners, includeNodes2)) {
                    if (bgpNeighbor.getRemoteBgpNeighbor() == null) {
                      bgpSession.status = SessionStatus.HALF_OPEN;
                    } else if (bgpNeighbor.getCandidateRemoteBgpNeighbors().size() != 1) {
                      bgpSession.status = SessionStatus.MULTIPLE_REMOTES;
                    } else {
                      bgpSession.remoteNode =
                          bgpNeighbor.getRemoteBgpNeighbor().getOwner().getName();
                      bgpSession.status = SessionStatus.UNIQUE_MATCH;
                    }
                  }
                }
              }
              if (question.matchesStatus(bgpSession.status)) {
                answerElement.getBgpSessions().add(bgpSession);
              }
            }
          }
        }
      }
      return answerElement;
    }

    private boolean node2RegexMatchesIp(
        Ip ip, Map<Ip, Set<String>> ipOwners, Set<String> includeNodes2) {
      Set<String> owners = ipOwners.get(ip);
      if (owners == null) {
        throw new BatfishException("Expected at least one owner of ip: " + ip);
      }
      for (String owner : owners) {
        if (includeNodes2.contains(owner)) {
          return true;
        }
      }
      return false;
    }
  }

  // <question_page_comment>

  /**
   * Returns the status of BGP sessions.
   *
   * <p>Based on config data, determines the status of IBGP and EBGP sessions
   *
   * @type BgpSessionCheck multifile
   * @param foreignBgpGroups Details coming.
   * @param node1Regex Regular expression to match the nodes names for one end of the sessions.
   *     Default is '.*' (all nodes).
   * @param node2Regex Regular expression to match the nodes names for the other end of the
   *     sessions. Default is '.*' (all nodes).
   * @example bf_answer("BgpSessionCheck", node1Regex="as1.*", node2Regex="as2.*") Checks all BGP
   *     sessions between nodes that start with as1 and those that start with as2.
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

    @Nullable private Pattern _typeRegex;

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

    public boolean matchesForeignGroup(String group) {
      return _foreignBgpGroups.contains(group);
    }

    public boolean matchesStatus(SessionStatus status) {
      return _statusRegex.matcher(status.toString()).matches();
    }

    public boolean matchesType(SessionType type) {
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

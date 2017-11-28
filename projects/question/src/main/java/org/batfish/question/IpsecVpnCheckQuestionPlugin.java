package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IpsecVpn;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class IpsecVpnCheckQuestionPlugin extends QuestionPlugin {

  public static class IpsecVpnCheckAnswerElement implements AnswerElement {

    public static class IpsecVpnPair extends Pair<Pair<String, String>, Pair<String, String>> {

      private static final String PROP_HOSTNAME1 = "hostname1";

      private static final String PROP_HOSTNAME2 = "hostname2";

      private static final String PROP_IPSEC_VPN1 = "ipsecVpn1";

      private static final String PROP_IPSEC_VPN2 = "ipsecVpn2";

      /** */
      private static final long serialVersionUID = 1L;

      @JsonCreator
      public IpsecVpnPair(
          @JsonProperty(PROP_HOSTNAME1) String hostname1,
          @JsonProperty(PROP_IPSEC_VPN1) String ipsecVpn1,
          @JsonProperty(PROP_HOSTNAME2) String hostname2,
          @JsonProperty(PROP_IPSEC_VPN2) String ipsecVpn2) {
        super(new Pair<>(hostname1, ipsecVpn1), new Pair<>(hostname2, ipsecVpn2));
      }

      @JsonProperty(PROP_HOSTNAME1)
      public String getHostname1() {
        return _first.getFirst();
      }

      @JsonProperty(PROP_HOSTNAME2)
      public String getHostname2() {
        return _second.getFirst();
      }

      @JsonProperty(PROP_IPSEC_VPN1)
      public String getIpsecVpn1() {
        return _first.getSecond();
      }

      @JsonProperty(PROP_IPSEC_VPN2)
      public String getIpsecVpn2() {
        return _second.getSecond();
      }
    }

    private SortedMap<String, SortedSet<IpsecVpnPair>> _incompatibleIkeProposals;

    private SortedMap<String, SortedSet<IpsecVpnPair>> _incompatibleIpsecProposals;

    private SortedMap<String, SortedSet<String>> _missingEndpoint;

    private SortedMap<String, SortedSet<IpsecVpnPair>> _nonUniqueEndpoint;

    private SortedMap<String, SortedSet<IpsecVpnPair>> _preSharedKeyMismatch;

    public IpsecVpnCheckAnswerElement() {
      _incompatibleIkeProposals = new TreeMap<>();
      _incompatibleIpsecProposals = new TreeMap<>();
      _missingEndpoint = new TreeMap<>();
      _nonUniqueEndpoint = new TreeMap<>();
      _preSharedKeyMismatch = new TreeMap<>();
    }

    public void addIpsecVpn(
        SortedMap<String, SortedSet<String>> ipsecVpnMap, Configuration c, IpsecVpn ipsecVpn) {
      String hostname = c.getHostname();
      SortedSet<String> ipsecVpnsByHostname =
          ipsecVpnMap.computeIfAbsent(hostname, k -> new TreeSet<>());
      String ipsecVpnName = ipsecVpn.getName();
      ipsecVpnsByHostname.add(ipsecVpnName);
    }

    public void addIpsecVpnPair(
        SortedMap<String, SortedSet<IpsecVpnPair>> ipsecVpnPairMap,
        Configuration c,
        IpsecVpn ipsecVpn,
        IpsecVpn remoteIpsecVpn) {
      String hostname = c.getHostname();
      SortedSet<IpsecVpnPair> ipsecVpnPairsByHostname =
          ipsecVpnPairMap.computeIfAbsent(hostname, k -> new TreeSet<>());
      String ipsecVpnName = ipsecVpn.getName();
      String remoteHostname = ipsecVpn.getRemoteIpsecVpn().getOwner().getHostname();
      String remoteIpsecVpnName = remoteIpsecVpn.getName();
      ipsecVpnPairsByHostname.add(
          new IpsecVpnPair(hostname, ipsecVpnName, remoteHostname, remoteIpsecVpnName));
    }

    public SortedMap<String, SortedSet<IpsecVpnPair>> getIncompatibleIkeProposals() {
      return _incompatibleIkeProposals;
    }

    public SortedMap<String, SortedSet<IpsecVpnPair>> getIncompatibleIpsecProposals() {
      return _incompatibleIpsecProposals;
    }

    public SortedMap<String, SortedSet<String>> getMissingEndpoint() {
      return _missingEndpoint;
    }

    public SortedMap<String, SortedSet<IpsecVpnPair>> getNonUniqueEndpoint() {
      return _nonUniqueEndpoint;
    }

    public SortedMap<String, SortedSet<IpsecVpnPair>> getPreSharedKeyMismatch() {
      return _preSharedKeyMismatch;
    }

    public void setIncompatibleIkeProposals(
        SortedMap<String, SortedSet<IpsecVpnPair>> incompatibleIkeProposals) {
      _incompatibleIkeProposals = incompatibleIkeProposals;
    }

    public void setIncompatibleIpsecProposals(
        SortedMap<String, SortedSet<IpsecVpnPair>> incompatibleIpsecProposals) {
      _incompatibleIpsecProposals = incompatibleIpsecProposals;
    }

    public void setMissingEndpoint(SortedMap<String, SortedSet<String>> missingEndpoint) {
      _missingEndpoint = missingEndpoint;
    }

    public void setNonUniqueEndpoint(SortedMap<String, SortedSet<IpsecVpnPair>> nonUniqueEndpoint) {
      _nonUniqueEndpoint = nonUniqueEndpoint;
    }

    public void setPreSharedKeyMismatch(
        SortedMap<String, SortedSet<IpsecVpnPair>> preSharedKeyMismatch) {
      _preSharedKeyMismatch = preSharedKeyMismatch;
    }
  }

  public static class IpsecVpnCheckAnswerer extends Answerer {

    public IpsecVpnCheckAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {
      IpsecVpnCheckQuestion question = (IpsecVpnCheckQuestion) _question;

      Pattern node1Regex;
      Pattern node2Regex;
      try {
        node1Regex = Pattern.compile(question.getNode1Regex());
        node2Regex = Pattern.compile(question.getNode2Regex());
      } catch (PatternSyntaxException e) {
        throw new BatfishException(
            String.format(
                "One of the supplied regexes (%s  OR  %s) is not a valid java regex.",
                question.getNode1Regex(), question.getNode2Regex()),
            e);
      }

      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      _batfish.initRemoteIpsecVpns(configurations);

      IpsecVpnCheckAnswerElement answerElement = new IpsecVpnCheckAnswerElement();
      for (Configuration c : configurations.values()) {
        if (!node1Regex.matcher(c.getHostname()).matches()) {
          continue;
        }
        for (IpsecVpn ipsecVpn : c.getIpsecVpns().values()) {
          if (ipsecVpn.getRemoteIpsecVpn() == null) {
            answerElement.addIpsecVpn(answerElement.getMissingEndpoint(), c, ipsecVpn);
          } else {
            if (ipsecVpn.getCandidateRemoteIpsecVpns().size() != 1) {
              for (IpsecVpn remoteIpsecVpn : ipsecVpn.getCandidateRemoteIpsecVpns()) {
                answerElement.addIpsecVpnPair(
                    answerElement.getNonUniqueEndpoint(), c, ipsecVpn, remoteIpsecVpn);
              }
            }
            IpsecVpn remoteIpsecVpn = ipsecVpn.getRemoteIpsecVpn();
            String remoteHost = remoteIpsecVpn.getOwner().getHostname();
            if (!node2Regex.matcher(remoteHost).matches()) {
              continue;
            }
            if (!ipsecVpn.compatibleIkeProposals(remoteIpsecVpn)) {
              answerElement.addIpsecVpnPair(
                  answerElement.getIncompatibleIkeProposals(), c, ipsecVpn, remoteIpsecVpn);
            }
            if (!ipsecVpn.compatibleIpsecProposals(remoteIpsecVpn)) {
              answerElement.addIpsecVpnPair(
                  answerElement.getIncompatibleIpsecProposals(), c, ipsecVpn, remoteIpsecVpn);
            }
            if (!ipsecVpn.compatiblePreSharedKey(remoteIpsecVpn)) {
              answerElement.addIpsecVpnPair(
                  answerElement.getPreSharedKeyMismatch(), c, ipsecVpn, remoteIpsecVpn);
            }
          }
        }
      }
      return answerElement;
    }
  }

  // <question_page_comment>

  /**
   * Checks if IPSec VPNs are correctly configured.
   *
   * <p>Details coming on what it means to be correctly configured.
   *
   * @type IpsecVpnCheck multifile
   * @param node1Regex Regular expression to match the nodes names for one end of the sessions.
   *     Default is '.*' (all nodes).
   * @param node2Regex Regular expression to match the nodes names for the other end of the
   *     sessions. Default is '.*' (all nodes).
   * @example bf_answer("IpsecVpnCheck", node1Regex="as1.*", node2Regex="as2.*") Checks all IPSec
   *     VPN sessions between nodes that start with as1 and those that start with as2.
   */
  public static class IpsecVpnCheckQuestion extends Question {

    private static final String PROP_NODE1_REGEX = "node1Regex";

    private static final String PROP_NODE2_REGEX = "node2Regex";

    private String _node1Regex;

    private String _node2Regex;

    public IpsecVpnCheckQuestion() {
      _node1Regex = ".*";
      _node2Regex = ".*";
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "ipsecvpncheck";
    }

    @JsonProperty(PROP_NODE1_REGEX)
    public String getNode1Regex() {
      return _node1Regex;
    }

    @JsonProperty(PROP_NODE2_REGEX)
    public String getNode2Regex() {
      return _node2Regex;
    }

    @Override
    public boolean getTraffic() {
      return false;
    }

    @JsonProperty(PROP_NODE1_REGEX)
    public void setNode1Regex(String regex) {
      _node1Regex = regex;
    }

    @JsonProperty(PROP_NODE2_REGEX)
    public void setNode2Regex(String regex) {
      _node2Regex = regex;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new IpsecVpnCheckAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new IpsecVpnCheckQuestion();
  }
}

package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.common.Answerer;
import org.batfish.common.Pair;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkAddress;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.collections.MultiSet;
import org.batfish.datamodel.collections.TreeMultiSet;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class SelfAdjacenciesQuestionPlugin extends QuestionPlugin {

  public static class SelfAdjacenciesAnswerElement implements AnswerElement {

    public static class InterfaceIpPair extends Pair<String, Ip> {

      private static final String PROP_INTERFACE_NAME = "interfaceName";

      private static final String PROP_IP = "ip";
      /** */
      private static final long serialVersionUID = 1L;

      @JsonCreator
      public InterfaceIpPair(
          @JsonProperty(PROP_INTERFACE_NAME) String t1, @JsonProperty(PROP_IP) Ip t2) {
        super(t1, t2);
      }

      @JsonProperty(PROP_INTERFACE_NAME)
      public String getInterfaceName() {
        return _first;
      }

      @JsonProperty(PROP_IP)
      public Ip getIp() {
        return _second;
      }
    }

    private SortedMap<String, SortedMap<Prefix, SortedSet<InterfaceIpPair>>> _selfAdjacencies;

    public SelfAdjacenciesAnswerElement() {
      _selfAdjacencies = new TreeMap<>();
    }

    public void add(String hostname, Prefix prefix, String interfaceName, Ip address) {
      SortedMap<Prefix, SortedSet<InterfaceIpPair>> prefixMap =
          _selfAdjacencies.computeIfAbsent(hostname, k -> new TreeMap<>());
      SortedSet<InterfaceIpPair> interfaces =
          prefixMap.computeIfAbsent(prefix, k -> new TreeSet<>());
      interfaces.add(new InterfaceIpPair(interfaceName, address));
    }

    public SortedMap<String, SortedMap<Prefix, SortedSet<InterfaceIpPair>>> getSelfAdjacencies() {
      return _selfAdjacencies;
    }

    public void setSelfAdjacencies(
        SortedMap<String, SortedMap<Prefix, SortedSet<InterfaceIpPair>>> selfAdjacencies) {
      _selfAdjacencies = selfAdjacencies;
    }
  }

  public static class SelfAdjacenciesAnswerer extends Answerer {

    public SelfAdjacenciesAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {

      SelfAdjacenciesQuestion question = (SelfAdjacenciesQuestion) _question;

      SelfAdjacenciesAnswerElement answerElement = new SelfAdjacenciesAnswerElement();
      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      Set<String> includeNodes = question.getNodeRegex().getMatchingNodes(configurations);
      configurations.forEach(
          (hostname, c) -> {
            if (includeNodes.contains(hostname)) {
              for (Vrf vrf : c.getVrfs().values()) {
                MultiSet<Prefix> nodePrefixes = new TreeMultiSet<>();
                for (Interface iface : vrf.getInterfaces().values()) {
                  Set<Prefix> ifaceBasePrefixes = new HashSet<>();
                  if (iface.getActive()) {
                    for (NetworkAddress prefix : iface.getAllAddresses()) {
                      Prefix basePrefix = Prefix.forNetworkAddress(prefix);
                      if (!ifaceBasePrefixes.contains(basePrefix)) {
                        ifaceBasePrefixes.add(basePrefix);
                        nodePrefixes.add(basePrefix);
                      }
                    }
                  }
                }
                for (Interface iface : vrf.getInterfaces().values()) {
                  for (NetworkAddress prefix : iface.getAllAddresses()) {
                    Prefix basePrefix = Prefix.forNetworkAddress(prefix);
                    if (nodePrefixes.count(basePrefix) > 1) {
                      Ip address = prefix.getAddress();
                      String interfaceName = iface.getName();
                      answerElement.add(hostname, basePrefix, interfaceName, address);
                    }
                  }
                }
              }
            }
          });
      return answerElement;
    }
  }

  // <question_page_comment>

  /**
   * Outputs cases where two interfaces on the same node are in the same subnet.
   *
   * <p>This occurrence likely indicates an error in IP address assignment.
   *
   * @type SelfAdjacencies onefile
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   * @example bf_answer("SelfAdjacencies", nodeRegex="as1.*") Analyze nodes whose names begin with
   *     "as1".
   */
  public static class SelfAdjacenciesQuestion extends Question {

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private NodesSpecifier _nodeRegex;

    public SelfAdjacenciesQuestion() {
      _nodeRegex = NodesSpecifier.ALL;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "selfadjacencies";
    }

    @JsonProperty(PROP_NODE_REGEX)
    public NodesSpecifier getNodeRegex() {
      return _nodeRegex;
    }

    @JsonProperty(PROP_NODE_REGEX)
    public void setNodeRegex(NodesSpecifier nodeRegex) {
      _nodeRegex = nodeRegex;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new SelfAdjacenciesAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new SelfAdjacenciesQuestion();
  }
}

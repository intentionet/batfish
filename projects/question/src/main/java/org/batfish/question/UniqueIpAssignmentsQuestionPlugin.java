package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.questions.InterfacesSpecifier;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class UniqueIpAssignmentsQuestionPlugin extends QuestionPlugin {

  public static class UniqueIpAssignmentsAnswerElement extends AnswerElement {

    private static final String PROP_DUPLICATE_IPS = "duplicateIps";

    private SortedMap<Ip, SortedSet<NodeInterfacePair>> _duplicateIps;

    public UniqueIpAssignmentsAnswerElement() {
      _summary = new AnswerSummary();
      _duplicateIps = new TreeMap<>();
    }

    @JsonProperty(PROP_DUPLICATE_IPS)
    public SortedMap<Ip, SortedSet<NodeInterfacePair>> getDuplicateIps() {
      return _duplicateIps;
    }

    private Object ipsToString() {
      StringBuilder sb = new StringBuilder("  Duplicate IPs\n");
      for (Ip ip : _duplicateIps.keySet()) {
        sb.append(String.format("    %s\n", ip));
        for (NodeInterfacePair nip : _duplicateIps.get(ip)) {
          sb.append(String.format("      %s\n", nip));
        }
      }
      return sb.toString();
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder("Results for unique IP assignment check\n");
      if (_duplicateIps != null) {
        sb.append(ipsToString());
      }
      return sb.toString();
    }

    @JsonProperty(PROP_DUPLICATE_IPS)
    public void setDuplicateIps(SortedMap<Ip, SortedSet<NodeInterfacePair>> duplicateIps) {
      _summary.setNumResults(duplicateIps.size());
      _duplicateIps = duplicateIps;
    }
  }

  public static class UniqueIpAssignmentsAnswerer extends Answerer {
    public UniqueIpAssignmentsAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {
      UniqueIpAssignmentsAnswerElement answerElement = new UniqueIpAssignmentsAnswerElement();
      answerElement.setDuplicateIps(getDuplicateIps());
      return answerElement;
    }

    private SortedMap<Ip, SortedSet<NodeInterfacePair>> getDuplicateIps() {
      UniqueIpAssignmentsQuestion question = (UniqueIpAssignmentsQuestion) _question;
      Set<String> nodes = question.getNodeRegex().getMatchingNodes(_batfish);
      Map<String, Configuration> configs = _batfish.loadConfigurations();
      return nodes
          .stream()
          // convert to stream of interfaces
          .flatMap(node -> configs.get(node).getAllInterfaces().values().stream())
          // narrow to interfaces of interest
          .filter(
              iface ->
                  question.getInterfacesSpecifier().matches(iface)
                      && (!question.getEnabledIpsOnly() || iface.getActive()))
          // convert to stream of Entry<Ip, NodeInterfacePair>
          .flatMap(
              iface ->
                  iface
                      .getAllAddresses()
                      .stream()
                      .map(
                          ifaceAdrr ->
                              Maps.immutableEntry(ifaceAdrr.getIp(), new NodeInterfacePair(iface))))
          // group by Ip
          .collect(Multimaps.toMultimap(Entry::getKey, Entry::getValue, TreeMultimap::create))
          // convert to stream of Entry<Ip, Set<NodeInterfacePair>>
          .asMap()
          .entrySet()
          .stream()
          // narrow to entries with multiple NodeInterfacePairs
          .filter(entry -> entry.getValue().size() > 1)
          .collect(
              ImmutableSortedMap.toImmutableSortedMap(
                  Comparator.naturalOrder(),
                  Entry::getKey,
                  entry -> ImmutableSortedSet.copyOf(entry.getValue())));
    }
  }

  // <question_page_comment>
  /*
   * Lists IP addresses that are assigned to multiple interfaces.
   *
   * <p>Except in cases of anycast, an IP address should be assigned to only one interface. This
   * question produces the list of IP addresses for which this condition does not hold.
   *
   * @type UniqueIpAssignments multifile
   * @param interfacesSpecifier Specification for interfaces to consider. Default value is '.*' (all
   *     interfaces).
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   * @example bf_answer("UniqueIpAssignments", nodeRegex='as2.*') Answers the question only for
   *     nodes whose names start with 'as2'.
   */
  public static class UniqueIpAssignmentsQuestion extends Question {

    private static final String PROP_ENABLED_IPS_ONLY = "enabledIpsOnly";

    private static final String PROP_INTERFACES_SPECIFIER = "interfacesSpecifier";

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private boolean _enabledIpsOnly;

    private InterfacesSpecifier _interfacesSpecifier;

    private NodesSpecifier _nodeRegex;

    @JsonCreator
    public UniqueIpAssignmentsQuestion(
        @JsonProperty(PROP_ENABLED_IPS_ONLY) Boolean enabledIpsOnly,
        @JsonProperty(PROP_INTERFACES_SPECIFIER) InterfacesSpecifier interfacesSpecifier,
        @JsonProperty(PROP_NODE_REGEX) NodesSpecifier nodesSpecifier) {
      _enabledIpsOnly = enabledIpsOnly != null && enabledIpsOnly;
      _interfacesSpecifier =
          interfacesSpecifier == null ? InterfacesSpecifier.ALL : interfacesSpecifier;
      _nodeRegex = nodesSpecifier == null ? NodesSpecifier.ALL : nodesSpecifier;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "uniqueipassignments";
    }

    @JsonProperty(PROP_ENABLED_IPS_ONLY)
    public boolean getEnabledIpsOnly() {
      return _enabledIpsOnly;
    }

    @JsonProperty(PROP_INTERFACES_SPECIFIER)
    public InterfacesSpecifier getInterfacesSpecifier() {
      return _interfacesSpecifier;
    }

    @JsonProperty(PROP_NODE_REGEX)
    public NodesSpecifier getNodeRegex() {
      return _nodeRegex;
    }

    @Override
    public String prettyPrint() {
      return String.format(
          "uniqueipassignments %senabledIpsOnly=%s, interfacesSpecifier=%s, nodeRegex=\"%s\"",
          prettyPrintBase(), _enabledIpsOnly, _interfacesSpecifier, _nodeRegex);
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new UniqueIpAssignmentsAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new UniqueIpAssignmentsQuestion(null, null, null);
  }
}

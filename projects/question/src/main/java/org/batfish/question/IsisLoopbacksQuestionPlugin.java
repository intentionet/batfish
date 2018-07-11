package org.batfish.question;

import static org.batfish.datamodel.isis.IsisInterfaceMode.ACTIVE;
import static org.batfish.datamodel.isis.IsisInterfaceMode.PASSIVE;
import static org.batfish.datamodel.isis.IsisInterfaceMode.UNSET;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;

@AutoService(Plugin.class)
public class IsisLoopbacksQuestionPlugin extends QuestionPlugin {

  public static class IsisLoopbacksAnswerElement extends AnswerElement {

    private SortedMap<String, SortedSet<String>> _inactive;

    private SortedMap<String, SortedSet<String>> _l1;

    private SortedMap<String, SortedSet<String>> _l1Active;

    private SortedMap<String, SortedSet<String>> _l1Passive;

    private SortedMap<String, SortedSet<String>> _l2;

    private SortedMap<String, SortedSet<String>> _l2Active;

    private SortedMap<String, SortedSet<String>> _l2Passive;

    private SortedMap<String, SortedSet<String>> _running;

    public IsisLoopbacksAnswerElement() {
      _inactive = new TreeMap<>();
      _l1 = new TreeMap<>();
      _l1Active = new TreeMap<>();
      _l1Passive = new TreeMap<>();
      _l2 = new TreeMap<>();
      _l2Active = new TreeMap<>();
      _l2Passive = new TreeMap<>();
      _running = new TreeMap<>();
    }

    public void add(
        SortedMap<String, SortedSet<String>> map, String hostname, String interfaceName) {
      SortedSet<String> interfacesByHostname = map.computeIfAbsent(hostname, k -> new TreeSet<>());
      interfacesByHostname.add(interfaceName);
    }

    public SortedMap<String, SortedSet<String>> getInactive() {
      return _inactive;
    }

    public SortedMap<String, SortedSet<String>> getL1() {
      return _l1;
    }

    public SortedMap<String, SortedSet<String>> getL1Active() {
      return _l1Active;
    }

    public SortedMap<String, SortedSet<String>> getL1Passive() {
      return _l1Passive;
    }

    public SortedMap<String, SortedSet<String>> getL2() {
      return _l2;
    }

    public SortedMap<String, SortedSet<String>> getL2Active() {
      return _l2Active;
    }

    public SortedMap<String, SortedSet<String>> getL2Passive() {
      return _l2Passive;
    }

    public SortedMap<String, SortedSet<String>> getRunning() {
      return _running;
    }

    public void setInactive(SortedMap<String, SortedSet<String>> inactive) {
      _inactive = inactive;
    }

    public void setL1(SortedMap<String, SortedSet<String>> l1) {
      _l1 = l1;
    }

    public void setL1Active(SortedMap<String, SortedSet<String>> l1Active) {
      _l1Active = l1Active;
    }

    public void setL1Passive(SortedMap<String, SortedSet<String>> l1Passive) {
      _l1Passive = l1Passive;
    }

    public void setL2(SortedMap<String, SortedSet<String>> l2) {
      _l2 = l2;
    }

    public void setL2Active(SortedMap<String, SortedSet<String>> l2Active) {
      _l2Active = l2Active;
    }

    public void setL2Passive(SortedMap<String, SortedSet<String>> l2Passive) {
      _l2Passive = l2Passive;
    }

    public void setRunning(SortedMap<String, SortedSet<String>> running) {
      _running = running;
    }
  }

  public static class IsisLoopbacksAnswerer extends Answerer {

    public IsisLoopbacksAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {

      IsisLoopbacksQuestion question = (IsisLoopbacksQuestion) _question;

      IsisLoopbacksAnswerElement answerElement = new IsisLoopbacksAnswerElement();
      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      Set<String> includeNodes = question.getNodeRegex().getMatchingNodes(_batfish);
      for (Entry<String, Configuration> e : configurations.entrySet()) {
        String hostname = e.getKey();
        if (!includeNodes.contains(hostname)) {
          continue;
        }
        Configuration c = e.getValue();
        c.getInterfaces()
            .forEach(
                (interfaceName, iface) -> {
                  if (iface.isLoopback(c.getConfigurationFormat())) {
                    IsisInterfaceSettings ifaceSettings = iface.getIsis();
                    IsisInterfaceMode l1Mode =
                        ifaceSettings != null
                            ? ifaceSettings.getLevel1() != null
                                ? ifaceSettings.getLevel1().getMode()
                                : UNSET
                            : UNSET;
                    IsisInterfaceMode l2Mode =
                        ifaceSettings != null
                            ? ifaceSettings.getLevel2() != null
                                ? ifaceSettings.getLevel2().getMode()
                                : UNSET
                            : UNSET;
                    boolean l1 = false;
                    boolean l2 = false;
                    boolean isis = false;
                    if (l1Mode == ACTIVE) {
                      l1 = true;
                      isis = true;
                      answerElement.add(answerElement.getL1Active(), hostname, interfaceName);
                    } else if (l1Mode == PASSIVE) {
                      l1 = true;
                      isis = true;
                      answerElement.add(answerElement.getL1Passive(), hostname, interfaceName);
                    }
                    if (l2Mode == ACTIVE) {
                      l2 = true;
                      isis = true;
                      answerElement.add(answerElement.getL2Active(), hostname, interfaceName);
                    } else if (l2Mode == PASSIVE) {
                      l2 = true;
                      isis = true;
                      answerElement.add(answerElement.getL2Passive(), hostname, interfaceName);
                    }
                    if (l1) {
                      answerElement.add(answerElement.getL1(), hostname, interfaceName);
                    }
                    if (l2) {
                      answerElement.add(answerElement.getL2(), hostname, interfaceName);
                    }
                    if (isis) {
                      answerElement.add(answerElement.getRunning(), hostname, interfaceName);
                    } else {
                      answerElement.add(answerElement.getInactive(), hostname, interfaceName);
                    }
                  }
                });
      }

      return answerElement;
    }
  }

  // <question_page_comment>

  /**
   * Lists which loopbacks interfaces are being announced into ISIS.
   *
   * <p>When running ISIS, it is a good practice to announce loopbacks interface IPs into ISIS. This
   * question produces the list of nodes for which such announcements are happening.
   *
   * @type IsisLoopbacks onefile
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   * @example bf_answer("IsisLoopbacks", nodeRegex='as2.*') Answers the question only for nodes
   *     whose names start with 'as2'.
   */
  public static class IsisLoopbacksQuestion extends Question {

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private NodesSpecifier _nodeRegex;

    public IsisLoopbacksQuestion() {
      _nodeRegex = NodesSpecifier.ALL;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "isisloopbacks";
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
    return new IsisLoopbacksAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new IsisLoopbacksQuestion();
  }
}

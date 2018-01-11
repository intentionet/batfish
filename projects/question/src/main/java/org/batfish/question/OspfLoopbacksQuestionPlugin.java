package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;

@AutoService(Plugin.class)
public class OspfLoopbacksQuestionPlugin extends QuestionPlugin {

  public static class OspfLoopbacksAnswerElement implements AnswerElement {

    private SortedMap<String, SortedSet<String>> _active;

    private SortedMap<String, SortedSet<String>> _exported;

    private SortedMap<String, SortedSet<String>> _missing;

    private SortedMap<String, SortedSet<String>> _passive;

    private SortedMap<String, SortedSet<String>> _running;

    public OspfLoopbacksAnswerElement() {
      _active = new TreeMap<>();
      _exported = new TreeMap<>();
      _missing = new TreeMap<>();
      _passive = new TreeMap<>();
      _running = new TreeMap<>();
    }

    public void add(
        SortedMap<String, SortedSet<String>> map, String hostname, String interfaceName) {
      SortedSet<String> interfacesByHostname = map.computeIfAbsent(hostname, k -> new TreeSet<>());
      interfacesByHostname.add(interfaceName);
    }

    @JsonIgnore
    public SortedMap<String, SortedSet<String>> getActive() {
      return _active;
    }

    @JsonIgnore
    public SortedMap<String, SortedSet<String>> getExported() {
      return _exported;
    }

    public SortedMap<String, SortedSet<String>> getMissing() {
      return _missing;
    }

    @JsonIgnore
    public SortedMap<String, SortedSet<String>> getPassive() {
      return _passive;
    }

    @JsonIgnore
    public SortedMap<String, SortedSet<String>> getRunning() {
      return _running;
    }

    private Object interfacesToString(
        String indent, String header, SortedMap<String, SortedSet<String>> interfaces) {
      StringBuilder sb = new StringBuilder(indent + header + "\n");
      for (String node : interfaces.keySet()) {
        for (String iface : interfaces.get(node)) {
          sb.append(indent + indent + node + " : " + iface + "\n");
        }
      }
      return sb.toString();
    }

    @Override
    public String prettyPrint() {
      StringBuilder sb = new StringBuilder("Results for OSPF loopbacks check\n");
      // if (_active.size() > 0) {
      // sb.append(interfacesToString(" ", "Active loopbacks", _active));
      // }
      // if (_exported.size() > 0) {
      // sb.append(
      // interfacesToString(" ", "Exported loopbacks", _exported));
      // }
      if (_missing.size() > 0) {
        sb.append(interfacesToString("  ", "Missing loopbacks", _missing));
      }
      // if (_passive.size() > 0) {
      // sb.append(interfacesToString(" ", "Passive loopbacks", _passive));
      // }
      // if (_running.size() > 0) {
      // sb.append(interfacesToString(" ", "Running loopbacks", _running));
      // }
      return sb.toString();
    }

    @JsonIgnore
    public void setActive(SortedMap<String, SortedSet<String>> active) {
      _active = active;
    }

    @JsonIgnore
    public void setExported(SortedMap<String, SortedSet<String>> exported) {
      _exported = exported;
    }

    public void setMissing(SortedMap<String, SortedSet<String>> missing) {
      _missing = missing;
    }

    @JsonIgnore
    public void setPassive(SortedMap<String, SortedSet<String>> passive) {
      _passive = passive;
    }

    @JsonIgnore
    public void setRunning(SortedMap<String, SortedSet<String>> running) {
      _running = running;
    }
  }

  public static class OspfLoopbacksAnswerer extends Answerer {

    public OspfLoopbacksAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {

      OspfLoopbacksQuestion question = (OspfLoopbacksQuestion) _question;

      OspfLoopbacksAnswerElement answerElement = new OspfLoopbacksAnswerElement();

      Map<String, Configuration> configurations = _batfish.loadConfigurations();
      Set<String> includeNodes = question.getNodeRegex().getMatchingNodes(configurations);

      for (Entry<String, Configuration> e : configurations.entrySet()) {
        String hostname = e.getKey();
        if (!includeNodes.contains(hostname)) {
          continue;
        }
        Configuration c = e.getValue();
        for (Vrf vrf : c.getVrfs().values()) {
          for (Entry<String, Interface> e2 : vrf.getInterfaces().entrySet()) {
            String interfaceName = e2.getKey();
            Interface iface = e2.getValue();
            if (iface.isLoopback(c.getConfigurationFormat())) {
              if (iface.getOspfEnabled()) {
                // ospf is running either passively or actively
                answerElement.add(answerElement.getRunning(), hostname, interfaceName);
                if (iface.getOspfPassive()) {
                  answerElement.add(answerElement.getPassive(), hostname, interfaceName);
                } else {
                  answerElement.add(answerElement.getActive(), hostname, interfaceName);
                }
              } else {
                // check if exported as external ospf route
                boolean exported = false;
                OspfProcess proc = vrf.getOspfProcess();
                if (proc != null) {
                  String exportPolicyName = proc.getExportPolicy();
                  if (exportPolicyName != null) {
                    RoutingPolicy exportPolicy = c.getRoutingPolicies().get(exportPolicyName);
                    if (exportPolicy != null) {
                      for (Prefix prefix : iface.getAllAddresses()) {
                        ConnectedRoute route = new ConnectedRoute(prefix, interfaceName);
                        if (exportPolicy.process(
                            route,
                            new OspfExternalRoute.Builder(),
                            null,
                            vrf.getName(),
                            Direction.OUT)) {
                          exported = true;
                        }
                      }
                    }
                  }
                  if (exported) {
                    answerElement.add(answerElement.getExported(), hostname, interfaceName);
                  } else {
                    // not exported, so should be inactive
                    answerElement.add(answerElement.getMissing(), hostname, interfaceName);
                  }
                }
              }
            }
          }
        }
      }

      return answerElement;
    }
  }

  // <question_page_comment>

  /**
   * Lists which loopbacks interfaces are being announced into OSPF.
   *
   * <p>When running OSPF, it is a good practice to announce loopbacks interface IPs into OSPF. This
   * question produces the list of nodes for which such announcements are happening.
   *
   * @type OspfLoopbacks onefile
   * @param nodeRegex Regular expression for names of nodes to include. Default value is '.*' (all
   *     nodes).
   * @example bf_answer("OspfLoopbacks", nodeRegex='as2.*') Answers the question only for nodes
   *     whose names start with 'as2'.
   */
  public static class OspfLoopbacksQuestion extends Question {

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private NodesSpecifier _nodeRegex;

    public OspfLoopbacksQuestion() {
      _nodeRegex = NodesSpecifier.ALL;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "ospfloopbacks";
    }

    @JsonProperty(PROP_NODE_REGEX)
    public NodesSpecifier getNodeRegex() {
      return _nodeRegex;
    }

    @Override
    public String prettyPrint() {
      String retString =
          String.format("ospfLoopbacks %snodeRegex=\"%s\"", prettyPrintBase(), _nodeRegex);
      return retString;
    }

    @JsonProperty(PROP_NODE_REGEX)
    public void setNodeRegex(NodesSpecifier nodeRegex) {
      _nodeRegex = nodeRegex;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new OspfLoopbacksAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new OspfLoopbacksQuestion();
  }
}

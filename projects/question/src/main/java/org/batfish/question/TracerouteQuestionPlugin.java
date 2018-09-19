package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Flow.Builder;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.pojo.Environment;
import org.batfish.datamodel.questions.IPacketTraceQuestion;
import org.batfish.datamodel.questions.Question;

@Deprecated
@AutoService(Plugin.class)
public class TracerouteQuestionPlugin extends QuestionPlugin {

  @Deprecated
  public static class TracerouteAnswerer extends Answerer {

    public TracerouteAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {
      String tag = _batfish.getFlowTag();
      Set<Flow> flows = getFlows(tag);
      _batfish.processFlows(flows, ((TracerouteQuestion) _question).getIgnoreAcls());
      AnswerElement answerElement = _batfish.getHistory();
      return answerElement;
    }

    @Override
    public AnswerElement answerDiff() {
      String tag = _batfish.getDifferentialFlowTag();
      Set<Flow> flows = getFlows(tag);
      TracerouteQuestion tracerouteQuestion = (TracerouteQuestion) _question;
      _batfish.pushBaseEnvironment();
      _batfish.processFlows(flows, tracerouteQuestion.getIgnoreAcls());
      _batfish.popEnvironment();
      _batfish.pushDeltaEnvironment();
      _batfish.processFlows(flows, tracerouteQuestion.getIgnoreAcls());
      _batfish.popEnvironment();
      FlowHistory history = _batfish.getHistory();
      FlowHistory filteredHistory = new FlowHistory();
      for (String flowText : history.getTraces().keySet()) {
        // String baseEnvTag = _batfish.getBaseTestrigSettings().getEnvName() +
        // ":"
        // + _batfish.getBaseTestrigSettings().getEnvironmentSettings()
        // .getEnvName();
        _batfish.pushBaseEnvironment();
        String baseEnvTag = _batfish.getFlowTag();
        Environment baseEnv = _batfish.getEnvironment();
        _batfish.popEnvironment();
        // String deltaEnvTag = _batfish.getDeltaTestrigSettings().getEnvName()
        // +
        // ":"
        // + _batfish.getDeltaTestrigSettings().getEnvironmentSettings()
        // .getEnvName();
        _batfish.pushDeltaEnvironment();
        String deltaEnvTag = _batfish.getFlowTag();
        Environment deltaEnv = _batfish.getEnvironment();
        _batfish.popEnvironment();
        Set<FlowTrace> baseFlowTraces =
            history.getTraces().get(flowText).getPaths().get(baseEnvTag);
        Set<FlowTrace> deltaFlowTraces =
            history.getTraces().get(flowText).getPaths().get(deltaEnvTag);
        if (!baseFlowTraces.toString().equals(deltaFlowTraces.toString())) {
          Flow flow = history.getTraces().get(flowText).getFlow();
          for (FlowTrace flowTrace : baseFlowTraces) {
            filteredHistory.addFlowTrace(flow, baseEnvTag, baseEnv, flowTrace);
          }
          for (FlowTrace flowTrace : deltaFlowTraces) {
            filteredHistory.addFlowTrace(flow, deltaEnvTag, deltaEnv, flowTrace);
          }
        }
      }
      return filteredHistory;
    }

    private Set<Flow> getFlows(String tag) {
      Set<Flow> flows = new TreeSet<>();
      TracerouteQuestion question = (TracerouteQuestion) _question;
      Set<Flow.Builder> flowBuilders = question.getFlowBuilders();
      Map<String, Configuration> configurations = null;
      for (Flow.Builder flowBuilder : flowBuilders) {
        // TODO: better automatic source ip, considering VRFs and routing
        if (flowBuilder.getSrcIp().equals(Ip.AUTO)) {
          if (configurations == null) {
            _batfish.pushBaseEnvironment();
            configurations = _batfish.loadConfigurations();
            _batfish.popEnvironment();
          }
          String hostname = flowBuilder.getIngressNode();
          Configuration node =
              Strings.isNullOrEmpty(hostname) ? null : configurations.get(hostname);
          if (node != null) {
            Ip canonicalIp = node.getCanonicalIp();
            if (canonicalIp != null) {
              flowBuilder.setSrcIp(canonicalIp);
            } else {
              throw new BatfishException(
                  "Cannot automatically assign source ip to flow since no there are no ip "
                      + "addresses assigned to any interface on ingress node: '"
                      + hostname
                      + "'");
            }
          } else {
            throw new BatfishException(
                "Cannot create flow with non-existent ingress node: '" + hostname + "'");
          }
        }
        if (flowBuilder.getDstIp().equals(Ip.AUTO)) {
          if (configurations == null) {
            _batfish.pushBaseEnvironment();
            configurations = _batfish.loadConfigurations();
            _batfish.popEnvironment();
          }
          flowBuilder.setDstIp(question.createDstIpFromDst(configurations));
        }
        flowBuilder.setTag(tag);
        Flow flow = flowBuilder.build();
        flows.add(flow);
      }
      return flows;
    }
  }

  // <question_page_comment>

  /**
   * Perform a traceroute.
   *
   * <p>This question performs a virtual traceroute in the network from a starting node. The
   * destination IP is randomly picked if not explicitly specified. Other IP headers are also
   * randomly picked if unspecified, with a bias toward generating packets similar to a real
   * traceroute (see below).
   *
   * <p>Unlike a real traceroute, this traceroute is directional. That is, for it to succeed, the
   * reverse connectivity is not needed. This feature can help debug connectivity issues by
   * decoupling the two directions.
   *
   * @type Traceroute dataplane
   */
  @Deprecated
  public static class TracerouteQuestion extends IPacketTraceQuestion {

    private static final String PROP_IGNORE_ACLS = "ignoreAcls";

    private static final String PROP_INGRESS_NODE = "ingressNode";

    private static final String PROP_INGRESS_VRF = "ingressVrf";

    private boolean _ignoreAcls;

    private String _ingressNode;

    private String _ingressVrf;

    public TracerouteQuestion() {
      _ignoreAcls = false;
    }

    public Flow.Builder createFlowBuilder() {
      Flow.Builder flowBuilder = createBaseFlowBuilder();
      if (_ingressNode != null) {
        flowBuilder.setIngressNode(_ingressNode);
      }
      if (_ingressVrf != null) {
        flowBuilder.setIngressVrf(_ingressVrf);
      }
      return flowBuilder;
    }

    @Override
    public boolean getDataPlane() {
      return true;
    }

    @JsonProperty(PROP_IGNORE_ACLS)
    public boolean getIgnoreAcls() {
      return _ignoreAcls;
    }

    @JsonIgnore
    public Set<Builder> getFlowBuilders() {
      return Collections.singleton(createFlowBuilder());
    }

    @JsonProperty(PROP_INGRESS_NODE)
    public String getIngressNode() {
      return _ingressNode;
    }

    @JsonProperty(PROP_INGRESS_VRF)
    public String getIngressVrf() {
      return _ingressVrf;
    }

    @Override
    public String getName() {
      return "oldtraceroute";
    }

    @Override
    public String prettyPrint() {
      try {
        String retString =
            String.format("traceroute %singressNode=%s", prettyPrintBase(), _ingressNode);
        if (_ingressVrf != null) {
          retString += String.format(", %s=%s", PROP_INGRESS_VRF, _ingressVrf);
        }
        retString += toString(); // calls parent toString()
        return retString;
      } catch (Exception e) {
        try {
          return "Pretty printing failed. Printing Json\n" + toJsonString();
        } catch (BatfishException e1) {
          throw new BatfishException("Both pretty and json printing failed\n");
        }
      }
    }

    @JsonProperty(PROP_IGNORE_ACLS)
    public void setIgnoreAcls(boolean ignoreAcls) {
      _ignoreAcls = ignoreAcls;
    }

    @JsonProperty(PROP_INGRESS_NODE)
    public void setIngressNode(String ingressNode) {
      _ingressNode = ingressNode;
    }

    @JsonProperty(PROP_INGRESS_VRF)
    public void setIngressVrf(String ingressVrf) {
      _ingressVrf = ingressVrf;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new TracerouteAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new TracerouteQuestion();
  }
}

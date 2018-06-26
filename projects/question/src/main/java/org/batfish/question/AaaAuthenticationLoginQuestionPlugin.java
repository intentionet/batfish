package org.batfish.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.vendor_family.cisco.Line;

@AutoService(Plugin.class)
public class AaaAuthenticationLoginQuestionPlugin extends QuestionPlugin {

  public static class AaaAuthenticationAnswerElement extends AnswerElement {

    private SortedMap<String, List<String>> _exposedLines;

    public AaaAuthenticationAnswerElement() {
      _exposedLines = new TreeMap<>();
    }

    public SortedMap<String, List<String>> getExposedLines() {
      return _exposedLines;
    }
  }

  public static class AaaAuthenticationAnswerer extends Answerer {
    public AaaAuthenticationAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AaaAuthenticationAnswerElement answer() {
      AaaAuthenticationQuestion question = (AaaAuthenticationQuestion) _question;
      AaaAuthenticationAnswerElement answerElement = new AaaAuthenticationAnswerElement();

      Set<String> specifiedNodes = question.getNodeRegex().getMatchingNodes(_batfish);

      SortedMap<String, Configuration> configs = _batfish.loadConfigurations();
      configs.forEach(
          (configName, config) -> {
            if (specifiedNodes.contains(configName)
                && config.getVendorFamily().getCisco() != null) {
              List<String> lines = new ArrayList<>();
              for (Line line : config.getVendorFamily().getCisco().getLines().values()) {
                if (!line.requiresAuthentication()) {
                  lines.add(line.getName());
                }
              }
              if (!lines.isEmpty()) {
                answerElement.getExposedLines().put(configName, lines);
              }
            }
          });

      return answerElement;
    }
  }

  public static class AaaAuthenticationQuestion extends Question {

    private static final String PROP_NODE_REGEX = "nodeRegex";

    private NodesSpecifier _nodeRegex;

    public AaaAuthenticationQuestion() {
      _nodeRegex = NodesSpecifier.ALL;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "AaaAuthenticationLogin";
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
    return new AaaAuthenticationAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new AaaAuthenticationQuestion();
  }
}

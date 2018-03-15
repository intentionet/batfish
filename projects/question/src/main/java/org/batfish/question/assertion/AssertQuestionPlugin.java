package org.batfish.question.assertion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.auto.service.AutoService;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.Plugin;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.assertion.AssertionAst;
import org.batfish.datamodel.questions.Question;
import org.batfish.question.NodesQuestionPlugin.NodesAnswerer;
import org.batfish.question.NodesQuestionPlugin.NodesQuestion;
import org.batfish.question.QuestionPlugin;

@AutoService(Plugin.class)
public class AssertQuestionPlugin extends QuestionPlugin {

  public static class AssertAnswerElement implements AnswerElement {

    private Boolean _fail;

    private SortedMap<Integer, Assertion> _failing;

    private SortedMap<Integer, Assertion> _passing;

    public AssertAnswerElement() {
      _failing = new TreeMap<>();
      _passing = new TreeMap<>();
    }

    public Boolean getFail() {
      return _fail;
    }

    public SortedMap<Integer, Assertion> getFailing() {
      return _failing;
    }

    public SortedMap<Integer, Assertion> getPassing() {
      return _passing;
    }

    public void setFail(Boolean fail) {
      _fail = fail;
    }

    public void setFailing(SortedMap<Integer, Assertion> failing) {
      _failing = failing;
    }

    public void setPassing(SortedMap<Integer, Assertion> passing) {
      _passing = passing;
    }
  }

  public static class AssertAnswerer extends Answerer {

    public AssertAnswerer(Question question, IBatfish batfish) {
      super(question, batfish);
    }

    @Override
    public AnswerElement answer() {
      ConfigurationBuilder b = new ConfigurationBuilder();
      b.jsonProvider(new JacksonJsonNodeJsonProvider());
      b.options(Option.ALWAYS_RETURN_LIST);
      Configuration c = b.build();

      AssertQuestion question = (AssertQuestion) _question;
      List<Assertion> assertions = question.getAssertions();

      NodesQuestion nodesQuestion = new NodesQuestion();
      nodesQuestion.setSummary(false);
      NodesAnswerer nodesAnswerer = new NodesAnswerer(nodesQuestion, _batfish);
      AnswerElement nodesAnswer = nodesAnswerer.answer();
      String nodesAnswerStr = null;
      try {
        nodesAnswerStr = BatfishObjectMapper.writePrettyString(nodesAnswer);
      } catch (IOException e) {
        throw new BatfishException("Could not get JSON string from nodes answer", e);
      }
      Object jsonObject = JsonPath.parse(nodesAnswerStr, c).json();
      Map<Integer, Assertion> failing = new ConcurrentHashMap<>();
      Map<Integer, Assertion> passing = new ConcurrentHashMap<>();
      List<Integer> indices = new ArrayList<>();
      for (int i = 0; i < assertions.size(); i++) {
        indices.add(i);
      }
      final boolean[] fail = new boolean[1];
      ConcurrentMap<String, ArrayNode> pathCache = new ConcurrentHashMap<>();
      indices
          .parallelStream()
          .forEach(
              i -> {
                Assertion assertion = assertions.get(i);
                String assertionText = assertion.getAssertion();
                AssertionAst ast = _batfish.parseAssertion(assertionText);
                if (ast.execute(_batfish, jsonObject, pathCache, c)) {
                  passing.put(i, assertion);
                } else {
                  failing.put(i, assertion);
                  synchronized (fail) {
                    fail[0] = true;
                  }
                }
              });
      AssertAnswerElement answerElement = new AssertAnswerElement();
      answerElement.setFail(fail[0]);
      answerElement.getFailing().putAll(failing);
      answerElement.getPassing().putAll(passing);
      return answerElement;
    }
  }

  // <question_page_comment>

  /**
   * Checks assertions.
   *
   * @type Assert misc
   * @param assertions List of assertions
   */
  public static class AssertQuestion extends Question {

    private static final String PROP_ASSERTIONS = "assertions";

    private List<Assertion> _assertions;

    public AssertQuestion() {
      _assertions = new ArrayList<>();
    }

    @JsonProperty(PROP_ASSERTIONS)
    public List<Assertion> getAssertions() {
      return _assertions;
    }

    @Override
    public boolean getDataPlane() {
      return false;
    }

    @Override
    public String getName() {
      return "assert";
    }

    @Override
    public String prettyPrint() {
      String retString =
          String.format("assert %s%s=\"%s\"", prettyPrintBase(), PROP_ASSERTIONS, _assertions);
      return retString;
    }

    @JsonProperty(PROP_ASSERTIONS)
    public void setAssertions(List<Assertion> assertions) {
      _assertions = assertions;
    }
  }

  @Override
  protected Answerer createAnswerer(Question question, IBatfish batfish) {
    return new AssertAnswerer(question, batfish);
  }

  @Override
  protected Question createQuestion() {
    return new AssertQuestion();
  }
}

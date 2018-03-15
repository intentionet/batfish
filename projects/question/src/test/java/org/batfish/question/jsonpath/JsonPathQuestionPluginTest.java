package org.batfish.question.jsonpath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.question.jsonpath.JsonPathQuestionPlugin.JsonPathAnswerElement;
import org.batfish.question.jsonpath.JsonPathQuestionPlugin.JsonPathQuestion;
import org.junit.Test;

public class JsonPathQuestionPluginTest {

  @Test
  public void configureTemplateTest() throws IOException {
    ObjectMapper mapper = BatfishObjectMapper.mapper();

    Set<JsonPathException> oldExceptions =
        Collections.singleton(new JsonPathException(Collections.singletonList("old"), null));
    JsonPathAssertion oldAssertion =
        new JsonPathAssertion(
            JsonPathAssertionType.equals, mapper.readValue("[true]", JsonNode.class));
    JsonPathQuery query = new JsonPathQuery("ll", false, null, null, oldExceptions, oldAssertion);
    JsonPathQuestion oldQuestion = new JsonPathQuestion();
    oldQuestion.setPaths(Collections.singletonList(query));

    Set<JsonPathException> newExceptions =
        Collections.singleton(new JsonPathException(Collections.singletonList("new"), null));
    JsonPathAssertion newAssertion =
        new JsonPathAssertion(
            JsonPathAssertionType.equals, mapper.readValue("[false]", JsonNode.class));

    JsonPathQuestion newQuestion =
        (JsonPathQuestion)
            oldQuestion.configureTemplate(
                mapper.writeValueAsString(newExceptions), mapper.writeValueAsString(newAssertion));

    // the exceptions and assertion of the newQuestion should be the new values
    assertThat(newQuestion.getPaths().get(0).getExceptions(), equalTo(newExceptions));
    assertThat(newQuestion.getPaths().get(0).getAssertion(), equalTo(newAssertion));
  }

  @Test
  public void configureTemplateTestNullValues() throws IOException {
    ObjectMapper mapper = BatfishObjectMapper.mapper();

    Set<JsonPathException> oldExceptions =
        Collections.singleton(new JsonPathException(Collections.singletonList("old"), null));
    JsonPathAssertion oldAssertion =
        new JsonPathAssertion(
            JsonPathAssertionType.equals, mapper.readValue("[true]", JsonNode.class));
    JsonPathQuery query = new JsonPathQuery("ll", false, null, null, oldExceptions, oldAssertion);
    JsonPathQuestion oldQuestion = new JsonPathQuestion();
    oldQuestion.setPaths(Collections.singletonList(query));

    Set<JsonPathException> newExceptions =
        Collections.singleton(new JsonPathException(Collections.singletonList("new"), null));
    JsonPathQuestion nullAssertionQuestion =
        (JsonPathQuestion)
            oldQuestion.configureTemplate(mapper.writeValueAsString(newExceptions), null);

    JsonPathAssertion newAssertion =
        new JsonPathAssertion(
            JsonPathAssertionType.equals, mapper.readValue("[false]", JsonNode.class));
    JsonPathQuestion nullExceptionQuestion =
        (JsonPathQuestion)
            oldQuestion.configureTemplate(null, mapper.writeValueAsString(newAssertion));

    // check if null values retained the original values
    assertThat(nullAssertionQuestion.getPaths().get(0).getAssertion(), equalTo(oldAssertion));
    assertThat(nullExceptionQuestion.getPaths().get(0).getExceptions(), equalTo(oldExceptions));
  }

  @Test
  public void updateSummaryTest() {
    JsonPathAnswerElement answerElement = new JsonPathAnswerElement();

    JsonPathResult result1 = new JsonPathResult();
    result1.setAssertionResult(true);
    result1.setNumResults(2);

    JsonPathResult result2 = new JsonPathResult();
    result2.setAssertionResult(false);
    result2.setNumResults(5);

    JsonPathResult result3 = new JsonPathResult();
    result3.setNumResults(3);

    answerElement.getResults().put(0, result1);
    answerElement.getResults().put(1, result2);
    answerElement.getResults().put(2, result3);
    answerElement.updateSummary();

    AnswerSummary summary = answerElement.getSummary();

    assertThat(summary.getNumFailed(), equalTo(1));
    assertThat(summary.getNumPassed(), equalTo(1));
    assertThat(summary.getNumResults(), equalTo(10));
  }
}

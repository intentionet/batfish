package org.batfish.question.jsonpath;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.batfish.datamodel.answers.AnswerSummary;
import org.batfish.question.jsonpath.JsonPathQuestionPlugin.JsonPathAnswerElement;
import org.junit.Test;

public class JsonPathQuestionPluginTest {

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

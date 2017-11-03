package org.batfish.datamodel.answers;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import org.batfish.common.util.BatfishObjectMapper;
import org.junit.Test;

/** Tests for {@link AnswerSummary} */
public class AnswerSummaryTest {

  @Test
  public void combineTest() {
    AnswerSummary summary = new AnswerSummary("notes1", 1, 2, 3);
    AnswerSummary summaryOther = new AnswerSummary("notes2", 4, 5, 6);
    summary.combine(summaryOther);
    assertThat(summary.getNotes(), equalTo("notes1; notes2"));
    assertThat(summary.getNumFailed(), equalTo(5));
    assertThat(summary.getNumPassed(), equalTo(7));
    assertThat(summary.getNumResults(), equalTo(9));
  }

  @Test
  public void deserializationTest() throws IOException {
    String summaryStr =
        "{\"notes\" : \"notes1\", \"numFailed\" : 21, \"numPassed\" : 23, " + "\"numResults\": 42}";
    BatfishObjectMapper mapper = new BatfishObjectMapper();
    AnswerSummary summary = mapper.readValue(summaryStr, AnswerSummary.class);
    assertThat(summary.getNotes(), equalTo("notes1"));
    assertThat(summary.getNumFailed(), equalTo(21));
    assertThat(summary.getNumPassed(), equalTo(23));
    assertThat(summary.getNumResults(), equalTo(42));
  }

  @Test
  public void serializationTest() throws IOException {
    AnswerSummary summary = new AnswerSummary("notes1", 21, 23, 42);
    BatfishObjectMapper mapper = new BatfishObjectMapper();
    String summaryStr = mapper.writeValueAsString(summary);

    // we test if serialization happened correctly, by deserializing it
    // in fact, deserializing might be the problem, but then the test above should fail
    AnswerSummary summaryAfter = mapper.readValue(summaryStr, AnswerSummary.class);
    assertThat(summaryAfter.getNotes(), equalTo("notes1"));
    assertThat(summaryAfter.getNumFailed(), equalTo(21));
    assertThat(summaryAfter.getNumPassed(), equalTo(23));
    assertThat(summaryAfter.getNumResults(), equalTo(42));
  }
}

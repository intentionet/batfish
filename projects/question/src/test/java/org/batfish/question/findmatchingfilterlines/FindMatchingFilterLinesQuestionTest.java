package org.batfish.question.findmatchingfilterlines;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import org.batfish.common.util.BatfishObjectMapper;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.junit.Test;

public class FindMatchingFilterLinesQuestionTest {

  @Test
  public void testJsonSerialization() throws IOException {
    // Default parameters
    FindMatchingFilterLinesQuestion q = new FindMatchingFilterLinesQuestion();
    FindMatchingFilterLinesQuestion clone =
        BatfishObjectMapper.clone(q, FindMatchingFilterLinesQuestion.class);
    assertQuestionsEqual(q, clone);

    // Non-default parameters
    q =
        new FindMatchingFilterLinesQuestion(
            "nodes",
            "filters",
            LineAction.PERMIT,
            PacketHeaderConstraints.builder()
                .setIpProtocols(ImmutableSet.of(IpProtocol.TCP))
                .build(),
            false);
    clone = BatfishObjectMapper.clone(q, FindMatchingFilterLinesQuestion.class);
    assertQuestionsEqual(q, clone);
  }

  private static void assertQuestionsEqual(
      FindMatchingFilterLinesQuestion q1, FindMatchingFilterLinesQuestion q2) {
    assertThat(q1.getNodeSpecifier(), equalTo(q2.getNodeSpecifier()));
    assertThat(q1.getFilterSpecifier(), equalTo(q2.getFilterSpecifier()));
    assertThat(q1.getAction(), equalTo(q2.getAction()));
    assertThat(q1.getHeaderConstraints(), equalTo(q2.getHeaderConstraints()));
    assertThat(q1.getIgnoreComposites(), equalTo(q2.getIgnoreComposites()));
  }
}

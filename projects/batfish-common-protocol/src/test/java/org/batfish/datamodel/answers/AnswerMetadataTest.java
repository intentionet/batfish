package org.batfish.datamodel.answers;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public final class AnswerMetadataTest {

  @Test
  public void testEquals() {
    AnswerMetadata.Builder builder = AnswerMetadata.builder().setStatus(AnswerStatus.SUCCESS);
    AnswerMetadata group1Elem1 = builder.build();
    AnswerMetadata group1Elem2 = builder.build();
    AnswerMetadata group1Elem3 = AnswerMetadata.forStatus(AnswerStatus.SUCCESS);
    AnswerMetadata group2Elem1 =
        builder.setMetrics(Metrics.builder().setNumRows(5).build()).build();
    AnswerMetadata group3Elem1 = builder.setStatus(AnswerStatus.FAILURE).build();

    new EqualsTester()
        .addEqualityGroup(group1Elem1, group1Elem2, group1Elem3)
        .addEqualityGroup(group2Elem1)
        .addEqualityGroup(group3Elem1)
        .testEquals();
  }
}

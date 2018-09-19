package org.batfish.datamodel.answers;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

public final class GetAnalysisAnswerMetricsAnswerTest {

  @Test
  public void testEquals() {
    GetAnalysisAnswerMetricsAnswer group1Elem1 =
        new GetAnalysisAnswerMetricsAnswer(
            ImmutableMap.of(
                "a",
                AnswerMetadata.builder()
                    .setMetrics(
                        Metrics.builder()
                            .setAggregations(
                                ImmutableMap.of("A", ImmutableMap.of(Aggregation.MAX, 1)))
                            .setNumRows(2)
                            .build())
                    .setStatus(AnswerStatus.SUCCESS)
                    .build()));
    GetAnalysisAnswerMetricsAnswer group1Elem2 =
        new GetAnalysisAnswerMetricsAnswer(
            ImmutableMap.of(
                "a",
                AnswerMetadata.builder()
                    .setMetrics(
                        Metrics.builder()
                            .setAggregations(
                                ImmutableMap.of("A", ImmutableMap.of(Aggregation.MAX, 1)))
                            .setNumRows(2)
                            .build())
                    .setStatus(AnswerStatus.SUCCESS)
                    .build()));
    GetAnalysisAnswerMetricsAnswer group2Elem1 =
        new GetAnalysisAnswerMetricsAnswer(
            ImmutableMap.of(
                "b",
                AnswerMetadata.builder()
                    .setMetrics(
                        Metrics.builder()
                            .setAggregations(
                                ImmutableMap.of("A", ImmutableMap.of(Aggregation.MAX, 1)))
                            .setNumRows(2)
                            .build())
                    .setStatus(AnswerStatus.SUCCESS)
                    .build()));

    new EqualsTester()
        .addEqualityGroup(group1Elem1, group1Elem2)
        .addEqualityGroup(group2Elem1)
        .testEquals();
  }
}

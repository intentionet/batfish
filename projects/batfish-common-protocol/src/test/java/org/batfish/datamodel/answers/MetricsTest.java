package org.batfish.datamodel.answers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

public final class MetricsTest {

  @Test
  public void testEquals() {
    Metrics.Builder builder = Metrics.builder();
    Metrics group1Elem1 = builder.build();
    Metrics group1Elem2 = builder.build();
    Metrics group2Elem1 =
        builder
            .setAggregations(ImmutableMap.of("a", ImmutableMap.of(Aggregation.MAX, "A")))
            .build();
    Metrics group3Elem1 = builder.setEmptyColumns(ImmutableSet.of("b")).build();
    Metrics group4Elem1 = builder.setMajorIssueTypes(ImmutableSet.of("c")).build();
    Metrics group5Elem1 = builder.setNumRows(5).build();

    new EqualsTester()
        .addEqualityGroup(group1Elem1, group1Elem2)
        .addEqualityGroup(group2Elem1)
        .addEqualityGroup(group3Elem1)
        .addEqualityGroup(group4Elem1)
        .addEqualityGroup(group5Elem1)
        .testEquals();
  }
}

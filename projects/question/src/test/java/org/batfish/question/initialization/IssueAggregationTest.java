package org.batfish.question.initialization;

import static org.batfish.question.initialization.IssueAggregation.aggregateDuplicateErrors;
import static org.batfish.question.initialization.IssueAggregation.aggregateDuplicateParseWarnings;
import static org.batfish.question.initialization.IssueAggregation.aggregateDuplicateRedflagWarnings;
import static org.batfish.question.initialization.IssueAggregation.aggregateDuplicateUnimplementedWarnings;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Map;
import org.batfish.common.BatfishException.BatfishStackTrace;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.common.Warnings.ParseWarning;
import org.batfish.question.initialization.IssueAggregation.WarningTriplet;
import org.junit.Test;

/** Tests of {@link IssueAggregation}. */
public class IssueAggregationTest {

  @Test
  public void testAggregateDuplicateErrors() {
    BatfishStackTrace stackTraceDup = new BatfishStackTrace(ImmutableList.of("lines1", "line2"));
    BatfishStackTrace stackTraceDup2 = new BatfishStackTrace(ImmutableList.of("lines1", "line2"));
    BatfishStackTrace stackTraceUnique =
        new BatfishStackTrace(ImmutableList.of("lines1", "line2", "line3"));
    Map<String, BatfishStackTrace> errors =
        ImmutableMap.of("dup1", stackTraceDup, "dup2", stackTraceDup2, "unique", stackTraceUnique);

    // Confirm that only the errors
    assertThat(
        aggregateDuplicateErrors(errors),
        equalTo(
            ImmutableMap.of(
                stackTraceDup,
                ImmutableSortedSet.of("dup1", "dup2"),
                stackTraceUnique,
                ImmutableSortedSet.of("unique"))));
  }

  @Test
  public void testAggregateDuplicateParseWarnings() {
    Warnings f1Warnings = new Warnings();
    f1Warnings
        .getParseWarnings()
        .addAll(
            ImmutableList.of(
                new ParseWarning(3, "dup", "[configuration]", null),
                new ParseWarning(4, "dup", "[configuration]", null),
                new ParseWarning(5, "unique", "[configuration]", null)));
    Warnings f2Warnings = new Warnings();
    f2Warnings
        .getParseWarnings()
        .addAll(ImmutableList.of(new ParseWarning(23, "dup", "[configuration]", null)));

    Map<String, Warnings> fileWarnings = ImmutableMap.of("f1", f1Warnings, "f2", f2Warnings);

    assertThat(
        aggregateDuplicateParseWarnings(fileWarnings),
        equalTo(
            ImmutableMap.of(
                new WarningTriplet("dup", "[configuration]", null),
                ImmutableMap.of("f1", ImmutableSortedSet.of(3, 4), "f2", ImmutableSortedSet.of(23)),
                new WarningTriplet("unique", "[configuration]", null),
                ImmutableMap.of("f1", ImmutableSortedSet.of(5)))));
  }

  @Test
  public void testAggregateDuplicateRedflagWarnings() {
    Warnings f1Warnings = new Warnings();
    f1Warnings
        .getRedFlagWarnings()
        .addAll(
            ImmutableList.of(
                new Warning("dup warning", null), new Warning("unique warning", null)));
    Warnings f2Warnings = new Warnings();
    f2Warnings.getRedFlagWarnings().addAll(ImmutableList.of(new Warning("dup warning", null)));

    Map<String, Warnings> fileWarnings = ImmutableMap.of("f1", f1Warnings, "f2", f2Warnings);

    assertThat(
        aggregateDuplicateRedflagWarnings(fileWarnings),
        equalTo(
            ImmutableMap.of(
                new Warning("dup warning", null),
                ImmutableSortedSet.of("f1", "f2"),
                new Warning("unique warning", null),
                ImmutableSortedSet.of("f1"))));
  }

  @Test
  public void testAggregateDuplicateUnimplementedarnings() {
    Warnings f1Warnings = new Warnings();
    f1Warnings
        .getUnimplementedWarnings()
        .addAll(
            ImmutableList.of(
                new Warning("dup warning", null), new Warning("unique warning", null)));
    Warnings f2Warnings = new Warnings();
    f2Warnings
        .getUnimplementedWarnings()
        .addAll(ImmutableList.of(new Warning("dup warning", null)));

    Map<String, Warnings> fileWarnings = ImmutableMap.of("f1", f1Warnings, "f2", f2Warnings);

    assertThat(
        aggregateDuplicateUnimplementedWarnings(fileWarnings),
        equalTo(
            ImmutableMap.of(
                new Warning("dup warning", null),
                ImmutableSortedSet.of("f1", "f2"),
                new Warning("unique warning", null),
                ImmutableSortedSet.of("f1"))));
  }
}

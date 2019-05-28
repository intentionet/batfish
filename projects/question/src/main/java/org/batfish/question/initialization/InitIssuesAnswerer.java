package org.batfish.question.initialization;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.question.initialization.IssueAggregation.aggregateDuplicateErrors;
import static org.batfish.question.initialization.IssueAggregation.aggregateDuplicateParseWarnings;
import static org.batfish.question.initialization.IssueAggregation.aggregateDuplicateWarnings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.ErrorDetails;
import org.batfish.common.ErrorDetails.ParseExceptionContext;
import org.batfish.common.Warnings;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.FileLines;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Rows;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

/** Implements {@link InitIssuesQuestion}. */
public class InitIssuesAnswerer extends Answerer {
  private static List<FileLines> filenamesToFileLines(Set<String> filenames) {
    return filenames.stream()
        .sorted()
        .map(n -> new FileLines(n, ImmutableSortedSet.of()))
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public TableAnswerElement answer() {
    ConvertConfigurationAnswerElement ccae =
        _batfish.loadConvertConfigurationAnswerElementOrReparse();
    ParseVendorConfigurationAnswerElement pvcae =
        _batfish.loadParseVendorConfigurationAnswerElement();

    Rows rows = new Rows();

    Map<String, Warnings> convertWarnings = ccae.getWarnings();
    aggregateDuplicateWarnings(convertWarnings, Warnings::getRedFlagWarnings)
        .forEach(
            (warning, nodes) ->
                rows.add(getRow(nodes, null, IssueType.ConvertWarningRedFlag, warning.getText())));
    aggregateDuplicateWarnings(convertWarnings, Warnings::getUnimplementedWarnings)
        .forEach(
            (warning, nodes) ->
                rows.add(
                    getRow(nodes, null, IssueType.ConvertWarningUnimplemented, warning.getText())));

    Map<String, Warnings> fileWarnings = pvcae.getWarnings();
    aggregateDuplicateParseWarnings(fileWarnings)
        .forEach(
            (triplet, fileLines) ->
                rows.add(
                    getRow(
                        null,
                        fileLines.asMap().entrySet().stream()
                            .map(
                                e ->
                                    new FileLines(
                                        e.getKey(), ImmutableSortedSet.copyOf(e.getValue())))
                            .collect(ImmutableList.toImmutableList()),
                        IssueType.ParseWarning,
                        firstNonNull(triplet._comment, "(details not provided)"),
                        triplet._text,
                        triplet._parserContext)));
    aggregateDuplicateWarnings(fileWarnings, Warnings::getRedFlagWarnings)
        .forEach(
            (warning, filenames) ->
                rows.add(
                    getRow(
                        null,
                        filenamesToFileLines(filenames),
                        IssueType.ParseWarningRedFlag,
                        warning.getText())));
    aggregateDuplicateWarnings(fileWarnings, Warnings::getUnimplementedWarnings)
        .forEach(
            (warning, filenames) ->
                rows.add(
                    getRow(
                        null,
                        filenamesToFileLines(filenames),
                        IssueType.ParseWarningUnimplemented,
                        warning.getText())));

    for (Entry<ParseStatus, Set<String>> entry :
        aggregateParseStatuses(pvcae.getParseStatus()).entrySet()) {
      // We're already adding issues for FAILED and PARTIALLY_UNRECOGNIZED, so skip those
      ParseStatus status = entry.getKey();
      switch (status) {
        case PASSED:
        case IGNORED:
          // No issue needed for files that passed or were explicitly ignored by user
          continue;
          // fall through
        case FAILED:
        case PARTIALLY_UNRECOGNIZED:
          // Other issues are already in the table for these files (e.g. stack trace, parse warn)
          continue;
        default:
          rows.add(
              getRow(
                  null,
                  filenamesToFileLines(entry.getValue()),
                  IssueType.ParseStatus,
                  ParseStatus.explanation(status)));
      }
    }

    aggregateDuplicateErrors(ccae.getErrorDetails())
        .forEach(
            (errorDetails, nodeNames) ->
                rows.add(getRow(nodeNames, null, IssueType.ConvertError, errorDetails._message)));
    aggregateDuplicateErrors(pvcae.getErrorDetails())
        .forEach(
            (errorDetails, fileNames) ->
                rows.add(
                    getRow(
                        null,
                        fileNames.stream()
                            .map(
                                name -> {
                                  ImmutableSortedSet<Integer> lines = ImmutableSortedSet.of();
                                  ErrorDetails details = pvcae.getErrorDetails().get(name);
                                  if (details != null) {
                                    ParseExceptionContext context =
                                        details.getParseExceptionContext();
                                    if (context != null && context.getLineNumber() != null) {
                                      lines = ImmutableSortedSet.of(context.getLineNumber());
                                    }
                                  }
                                  return new FileLines(name, lines);
                                })
                            .collect(ImmutableList.toImmutableList()),
                        IssueType.ParseError,
                        errorDetails._message,
                        errorDetails._lineContent,
                        errorDetails._parserContext)));

    TableAnswerElement answerElement = new TableAnswerElement(TABLE_METADATA);
    answerElement.postProcessAnswer(_question, rows.getData());
    return answerElement;
  }

  InitIssuesAnswerer(InitIssuesQuestion question, IBatfish batfish) {
    super(question, batfish);
  }

  @VisibleForTesting
  static SortedMap<ParseStatus, Set<String>> aggregateParseStatuses(
      SortedMap<String, ParseStatus> fileStatuses) {
    SortedMap<ParseStatus, Set<String>> aggregatedStatuses = new TreeMap<>();
    for (Entry<String, ParseStatus> entry : fileStatuses.entrySet()) {
      Set<String> files =
          aggregatedStatuses.computeIfAbsent(entry.getValue(), s -> new HashSet<>());
      files.add(entry.getKey());
    }
    return aggregatedStatuses;
  }

  private static Row getRow(
      @Nullable Iterable<String> nodes,
      @Nullable Iterable<FileLines> fileLines,
      IssueType issueType,
      String issue) {
    return getRow(nodes, fileLines, issueType, issue, null, null);
  }

  private static Row getRow(
      Iterable<String> nodes,
      Iterable<FileLines> fileLines,
      IssueType issueType,
      String issue,
      @Nullable String lineText,
      @Nullable String parserContext) {
    return Row.builder(TABLE_METADATA.toColumnMap())
        .put(COL_NODES, nodes)
        .put(COL_FILELINES, fileLines)
        .put(COL_TYPE, issueType.toString())
        .put(COL_DETAILS, issue)
        .put(COL_LINE_TEXT, lineText)
        .put(COL_PARSER_CONTEXT, parserContext)
        .build();
  }

  static final String COL_NODES = "Nodes";
  static final String COL_FILELINES = "Source_Lines";
  static final String COL_TYPE = "Type";
  static final String COL_DETAILS = "Details";
  static final String COL_LINE_TEXT = "Line_Text";
  static final String COL_PARSER_CONTEXT = "Parser_Context";

  private static final List<ColumnMetadata> METADATA =
      ImmutableList.of(
          new ColumnMetadata(
              COL_NODES,
              Schema.list(Schema.STRING),
              "The nodes that were converted (if applicable)",
              true,
              false),
          new ColumnMetadata(
              COL_FILELINES,
              Schema.list(Schema.FILE_LINES),
              "The files and lines that caused the issues (if applicable)",
              true,
              false),
          new ColumnMetadata(COL_TYPE, Schema.STRING, "The type of issues identified", true, false),
          new ColumnMetadata(
              COL_DETAILS, Schema.STRING, "Details about the issues identified", false, true),
          new ColumnMetadata(
              COL_LINE_TEXT,
              Schema.STRING,
              "The text of the input files that caused the issues (if applicable)",
              false,
              true),
          new ColumnMetadata(
              COL_PARSER_CONTEXT,
              Schema.STRING,
              "Batfish parser state when issues were encountered (if applicable)",
              false,
              true));

  private static final TableMetadata TABLE_METADATA = new TableMetadata(METADATA);
}

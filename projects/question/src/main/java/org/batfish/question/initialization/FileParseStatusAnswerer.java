package org.batfish.question.initialization;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.answers.ParseStatus;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Rows;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

/** Implements {@link FileParseStatusQuestion}. */
class FileParseStatusAnswerer extends Answerer {
  @Override
  public TableAnswerElement answer() {
    ParseVendorConfigurationAnswerElement pvcae =
        _batfish.loadParseVendorConfigurationAnswerElement();

    Map<String, String> fileMap = pvcae.getFileMap();
    Map<String, ParseStatus> statusMap = pvcae.getParseStatus();
    Rows rows = new Rows();

    statusMap.forEach(
        (filename, status) -> {
          Row row = getRow(filename, status, fileMap.get(filename));
          rows.add(row);
        });

    TableAnswerElement answerElement = new TableAnswerElement(TABLE_METADATA);
    answerElement.postProcessAnswer(_question, rows.getData());
    return answerElement;
  }

  FileParseStatusAnswerer(FileParseStatusQuestion question, IBatfish batfish) {
    super(question, batfish);
  }

  @Nonnull
  @VisibleForTesting
  static Row getRow(String filename, ParseStatus status, @Nullable String hostProduced) {
    Row.TypedRowBuilder builder = Row.builder(TABLE_METADATA.toColumnMap());
    builder.put(COL_FILENAME, filename);
    builder.put(COL_PARSE_STATUS, status.toString());

    if (hostProduced == null) {
      builder.put(COL_HOSTS, ImmutableList.of());
    } else {
      builder.put(COL_HOSTS, ImmutableList.of(new Node(hostProduced)));
    }

    return builder.build();
  }

  static final String COL_FILENAME = "Filename";
  static final String COL_PARSE_STATUS = "Status";
  static final String COL_HOSTS = "Hosts";

  private static final List<ColumnMetadata> METADATA =
      ImmutableList.of(
          new ColumnMetadata(COL_FILENAME, Schema.STRING, "The file that was parsed"),
          new ColumnMetadata(
              COL_PARSE_STATUS, Schema.STRING, "The status of the parsing operation"),
          new ColumnMetadata(
              COL_HOSTS, Schema.list(Schema.NODE), "Names of hosts produced from this file"));

  private static final DisplayHints DISPLAY_HINTS =
      new DisplayHints()
          .setTextDesc(
              String.format(
                  "File ${%s} parsed with status ${%s} and produced hosts ${%s}",
                  COL_FILENAME, COL_PARSE_STATUS, COL_HOSTS));

  private static final TableMetadata TABLE_METADATA = new TableMetadata(METADATA, DISPLAY_HINTS);
}

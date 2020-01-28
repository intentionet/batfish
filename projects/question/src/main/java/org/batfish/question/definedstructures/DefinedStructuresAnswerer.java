package org.batfish.question.definedstructures;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.batfish.common.Answerer;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.FileLines;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

public class DefinedStructuresAnswerer extends Answerer {
  public static final String COL_SOURCE_LINES = "Source_Lines";
  public static final String COL_STRUCTURE_NAME = "Structure_Name";
  public static final String COL_STRUCT_TYPE = "Structure_Type";

  public DefinedStructuresAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public TableAnswerElement answer(NetworkSnapshot snapshot) {
    DefinedStructuresQuestion question = (DefinedStructuresQuestion) _question;
    Multiset<Row> structures = rawAnswer(snapshot, question);
    TableAnswerElement answer = new TableAnswerElement(createMetadata(question));
    answer.postProcessAnswer(question, structures);
    return answer;
  }

  private Multiset<Row> rawAnswer(NetworkSnapshot snapshot, DefinedStructuresQuestion question) {
    Multiset<Row> structures = HashMultiset.create();
    Set<String> includeNodes =
        question.getNodeSpecifier().resolve(_batfish.specifierContext(snapshot));
    Multimap<String, String> hostnameFilenameMap =
        _batfish.loadParseVendorConfigurationAnswerElement(snapshot).getFileMap();
    Pattern includeFileNames = Pattern.compile(question.getFiles(), Pattern.CASE_INSENSITIVE);
    Set<String> includeFiles =
        hostnameFilenameMap.entries().stream()
            .filter(e -> includeNodes.contains(e.getKey()))
            .map(Entry::getValue)
            .filter(f -> includeFileNames.matcher(f).find())
            .collect(Collectors.toSet());

    Pattern includeStructureNames = Pattern.compile(question.getNames(), Pattern.CASE_INSENSITIVE);
    Pattern includeStructureTypes = Pattern.compile(question.getTypes(), Pattern.CASE_INSENSITIVE);

    ConvertConfigurationAnswerElement ccae =
        _batfish.loadConvertConfigurationAnswerElementOrReparse(snapshot);

    ccae.getDefinedStructures()
        .forEach(
            (filename, byStructType) -> {
              if (!includeFiles.contains(filename)) {
                return;
              }
              byStructType.forEach(
                  (structType, byStructName) -> {
                    if (!includeStructureTypes.matcher(structType).matches()) {
                      return;
                    }
                    byStructName.forEach(
                        (structName, info) -> {
                          if (!includeStructureNames.matcher(structName).matches()) {
                            return;
                          }
                          DefinedStructureRow row =
                              new DefinedStructureRow(
                                  filename, structType, structName, info.getDefinitionLines());
                          structures.add(toRow(row));
                        });
                  });
            });

    return structures;
  }

  private static final List<ColumnMetadata> COLUMN_METADATA =
      ImmutableList.of(
          new ColumnMetadata(
              COL_STRUCT_TYPE, Schema.STRING, "Vendor-specific type of the structure", true, false),
          new ColumnMetadata(
              COL_STRUCTURE_NAME, Schema.STRING, "Name of the structure", true, false),
          new ColumnMetadata(
              COL_SOURCE_LINES,
              Schema.FILE_LINES,
              "File and line numbers where the structure is defined",
              true,
              false));

  private static final Map<String, ColumnMetadata> COLUMN_METADATA_MAP =
      COLUMN_METADATA.stream()
          .collect(ImmutableMap.toImmutableMap(ColumnMetadata::getName, cm -> cm));

  private static final String DEFAULT_TEXT_DESC =
      String.format(
          "A structure of type ${%s} named ${%s} is defined at ${%s}.",
          COL_STRUCT_TYPE, COL_STRUCTURE_NAME, COL_SOURCE_LINES);

  public static TableMetadata createMetadata(Question question) {
    String textDesc = DEFAULT_TEXT_DESC;
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(COLUMN_METADATA, textDesc);
  }

  private static Row toRow(DefinedStructureRow info) {
    return Row.builder(COLUMN_METADATA_MAP)
        .put(COL_STRUCTURE_NAME, info.getStructName())
        .put(COL_STRUCT_TYPE, info.getStructType())
        .put(COL_SOURCE_LINES, new FileLines(info.getFilename(), info.getDefinitionLines()))
        .build();
  }
}

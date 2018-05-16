package org.batfish.question.nodeproperties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.NodePropertySpecifier;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

@ParametersAreNonnullByDefault
public class NodePropertiesAnswerElement extends TableAnswerElement {

  public static final String COL_NODE = "node";

  @JsonCreator
  public NodePropertiesAnswerElement(@JsonProperty(PROP_METADATA) TableMetadata tableMetadata) {
    super(tableMetadata);
  }

  /**
   * Creates a {@link TableMetadata} object from the question and a map of schemas.
   *
   * @param question The question
   * @param schemas The schemas map
   * @return The resulting {@link TableMetadata} object
   */
  static TableMetadata createMetadata(
      NodePropertiesQuestion question, Map<String, Schema> schemas) {
    List<ColumnMetadata> columnMetadata =
        new ImmutableList.Builder<ColumnMetadata>()
            .add(new ColumnMetadata(COL_NODE, Schema.NODE, "Node", true, false))
            .addAll(
                question
                    .getProperties()
                    .stream()
                    .map(
                        nps -> {
                          String columnName = getColumnNameFromPropertySpec(nps);
                          return new ColumnMetadata(
                              columnName,
                              schemas.containsKey(columnName)
                                  ? schemas.get(columnName)
                                  : Schema.OBJECT,
                              "Property " + nps.toString(),
                              false,
                              true);
                        })
                    .collect(Collectors.toList()))
            .build();

    DisplayHints dhints = question.getDisplayHints();
    if (dhints == null) {
      dhints = new DisplayHints();
      dhints.setTextDesc(String.format("Properties of node ${%s}.", COL_NODE));
    }
    return new TableMetadata(columnMetadata, dhints);
  }

  static String getColumnNameFromPropertySpec(NodePropertySpecifier nodePropertySpec) {
    return nodePropertySpec.toString();
  }
}

package org.batfish.question.namedstructures;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.NamedStructureSpecifier;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

public class NamedStructuresAnswerer extends Answerer {

  public static final String COL_NODE = "Node";
  public static final String COL_PRESENT_ON_NODE = "Present_On_Node";
  public static final String COL_STRUCTURE_TYPE = "Structure_Type";
  public static final String COL_STRUCTURE_NAME = "Structure_Name";
  public static final String COL_STRUCTURE_DEFINITION = "Structure_Definition";

  public NamedStructuresAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  /**
   * Creates {@link ColumnMetadata}s that the answer should have based on the {@code
   * namedStructureSpecifier}.
   *
   * <p>The {@link NamedStructureSpecifier} that describes the set of named structures
   *
   * @return The {@link List} of {@link ColumnMetadata}s
   */
  public static TableMetadata createMetadata(NamedStructuresQuestion question) {
    List<ColumnMetadata> columns =
        ImmutableList.of(
            new ColumnMetadata(COL_NODE, Schema.NODE, "Node", true, false),
            new ColumnMetadata(COL_STRUCTURE_TYPE, Schema.STRING, "Structure type", true, false),
            new ColumnMetadata(COL_STRUCTURE_NAME, Schema.STRING, "Structure name", true, false),
            question.getIndicatePresence()
                ? new ColumnMetadata(
                    COL_PRESENT_ON_NODE,
                    Schema.BOOLEAN,
                    "Whether the structure is present on the node",
                    false,
                    true)
                : new ColumnMetadata(
                    COL_STRUCTURE_DEFINITION, Schema.OBJECT, "Structure definition", false, true));

    String textDesc = String.format("Structure ${%s} on node ${%s}.", COL_STRUCTURE_NAME, COL_NODE);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(columns, textDesc);
  }

  @VisibleForTesting
  static Multiset<Row> rawAnswer(
      NamedStructuresQuestion question,
      Set<String> nodes,
      Map<String, Configuration> configurations,
      Map<String, ColumnMetadata> columns) {

    Multiset<Row> rows = HashMultiset.create();

    for (String structureType : question.getStructureTypes().getMatchingProperties()) {
      RowBuilder row = Row.builder(columns).put(COL_STRUCTURE_TYPE, structureType);

      Function<Configuration, Object> structMapGetter =
          NamedStructureSpecifier.JAVA_MAP.get(structureType).getGetter();

      Set<String> structNames = getAllStructureNamesOfType(structureType, nodes, configurations);

      for (String structName : structNames) {
        if (!question.getStructureNamePattern().matcher(structName).matches()) {
          continue;
        }
        for (String nodeName : nodes) {
          row.put(COL_NODE, new Node(nodeName));

          Object namedStructuresMap = structMapGetter.apply(configurations.get(nodeName));

          if (namedStructuresMap instanceof Map<?, ?>) {
            if (question.getIndicatePresence()) {
              row.put(COL_STRUCTURE_NAME, structName);
              row.put(COL_PRESENT_ON_NODE, ((Map) namedStructuresMap).containsKey(structName));
              rows.add(row.build());
            } else {
              if (((Map) namedStructuresMap).containsKey(structName)) {
                row.put(COL_STRUCTURE_NAME, structName);
                row.put(COL_STRUCTURE_DEFINITION, ((Map) namedStructuresMap).get(structName));
                rows.add(row.build());
              }
            }
          }
        }
      }
    }
    return rows;
  }

  @Override
  public AnswerElement answer() {
    NamedStructuresQuestion question = (NamedStructuresQuestion) _question;
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Set<String> nodes = question.getNodes().getMatchingNodes(_batfish);

    TableMetadata tableMetadata = createMetadata(question);

    Multiset<Row> propertyRows =
        rawAnswer(question, nodes, configurations, tableMetadata.toColumnMap());

    TableAnswerElement answer = new TableAnswerElement(tableMetadata);
    answer.postProcessAnswer(question, propertyRows);
    return answer;
  }

  /** Returns all structures of type {@code structureType} across {@code nodes} */
  public static Set<String> getAllStructureNamesOfType(
      String structureType, Set<String> nodes, Map<String, Configuration> configurations) {

    Set<String> structNames = new HashSet<>();

    Function<Configuration, Object> structMapGetter =
        NamedStructureSpecifier.JAVA_MAP.get(structureType).getGetter();

    for (String node : nodes) {
      Object namedStructuresMap = structMapGetter.apply(configurations.get(node));

      if (namedStructuresMap instanceof Map<?, ?>) {
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) namedStructuresMap).entrySet()) {
          structNames.add((String) entry.getKey());
        }
      }
    }
    return structNames;
  }
}

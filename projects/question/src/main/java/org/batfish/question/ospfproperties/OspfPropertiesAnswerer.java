package org.batfish.question.ospfproperties;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.questions.PropertySpecifier;
import org.batfish.datamodel.questions.PropertySpecifier.PropertyDescriptor;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierContext;

public class OspfPropertiesAnswerer extends Answerer {

  public static final String COL_NODE = "Node";
  public static final String COL_VRF = "VRF";
  public static final String COL_PROCESS_ID = "Process_ID";

  public OspfPropertiesAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  /**
   * Creates {@link ColumnMetadata}s that the answer should have based on the {@code
   * propertySpecifier}.
   *
   * @param propertySpecifier The {@link OspfPropertySpecifier} that describes the set of properties
   * @return The {@link List} of {@link ColumnMetadata}s
   */
  public static List<ColumnMetadata> createColumnMetadata(OspfPropertySpecifier propertySpecifier) {
    return ImmutableList.<ColumnMetadata>builder()
        .add(new ColumnMetadata(COL_NODE, Schema.NODE, "Node", true, false))
        .add(new ColumnMetadata(COL_VRF, Schema.STRING, "VRF", true, false))
        .add(new ColumnMetadata(COL_PROCESS_ID, Schema.STRING, "Process ID", true, false))
        .addAll(
            propertySpecifier.getMatchingProperties().stream()
                .map(
                    prop ->
                        new ColumnMetadata(
                            getColumnName(prop),
                            OspfPropertySpecifier.JAVA_MAP.get(prop).getSchema(),
                            "Property " + prop,
                            false,
                            true))
                .collect(Collectors.toList()))
        .build();
  }

  /** Creates a {@link TableMetadata} object from the question. */
  static TableMetadata createTableMetadata(OspfPropertiesQuestion question) {
    String textDesc =
        String.format(
            "Properties of OSPF process ${%s}:${%s}:${%s}.", COL_NODE, COL_VRF, COL_PROCESS_ID);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(createColumnMetadata(question.getProperties()), textDesc);
  }

  @Override
  public AnswerElement answer() {
    OspfPropertiesQuestion question = (OspfPropertiesQuestion) _question;
    TableMetadata tableMetadata = createTableMetadata(question);
    TableAnswerElement answer = new TableAnswerElement(tableMetadata);

    Multiset<Row> propertyRows =
        getProperties(
            question.getProperties(),
            _batfish.specifierContext(),
            question.getNodeSpecifier(),
            tableMetadata.toColumnMap());

    answer.postProcessAnswer(question, propertyRows);
    return answer;
  }

  /** Returns the name of the column that contains the value of property {@code property} */
  public static String getColumnName(String property) {
    return property;
  }

  /**
   * Returns a set of rows that contain the values of all the properties denoted by {@code
   * propertySpecifier}.
   */
  public static Multiset<Row> getProperties(
      OspfPropertySpecifier propertySpecifier,
      SpecifierContext ctxt,
      NodeSpecifier nodeSpecifier,
      Map<String, ColumnMetadata> columnMetadata) {

    Multiset<Row> rows = HashMultiset.create();

    nodeSpecifier
        .resolve(ctxt)
        .forEach(
            nodeName -> {
              ctxt.getConfigs()
                  .get(nodeName)
                  .getVrfs()
                  .values()
                  .forEach(
                      vrf -> {
                        OspfProcess ospfProcess = vrf.getOspfProcess();
                        if (ospfProcess == null) {
                          return;
                        }
                        RowBuilder rowBuilder =
                            Row.builder(columnMetadata)
                                .put(COL_NODE, new Node(nodeName))
                                .put(COL_VRF, vrf.getName())
                                .put(COL_PROCESS_ID, ospfProcess.getProcessId());

                        for (String property : propertySpecifier.getMatchingProperties()) {
                          PropertyDescriptor<OspfProcess> propertyDescriptor =
                              OspfPropertySpecifier.JAVA_MAP.get(property);
                          try {
                            PropertySpecifier.fillProperty(
                                propertyDescriptor, ospfProcess, property, rowBuilder);
                          } catch (ClassCastException e) {
                            throw new BatfishException(
                                String.format(
                                    "Type mismatch between property value ('%s') and Schema ('%s') for property '%s' for OSPF process '%s->%s-%s': %s",
                                    propertyDescriptor.getGetter().apply(ospfProcess),
                                    propertyDescriptor.getSchema(),
                                    property,
                                    nodeName,
                                    vrf.getName(),
                                    ospfProcess,
                                    e.getMessage()),
                                e);
                          }
                        }

                        rows.add(rowBuilder.build());
                      });
            });

    return rows;
  }
}

package org.batfish.question.ospfprocess;

import static org.batfish.datamodel.questions.OspfPropertySpecifier.AREAS;
import static org.batfish.datamodel.questions.OspfPropertySpecifier.AREA_BORDER_ROUTER;
import static org.batfish.datamodel.questions.OspfPropertySpecifier.EXPORT_POLICY_SOURCES;
import static org.batfish.datamodel.questions.OspfPropertySpecifier.REFERENCE_BANDWIDTH;
import static org.batfish.datamodel.questions.OspfPropertySpecifier.ROUTER_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.OspfPropertySpecifier;
import org.batfish.datamodel.questions.PropertySpecifier;
import org.batfish.datamodel.questions.PropertySpecifier.PropertyDescriptor;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

/** Implements {@link OspfProcessConfigurationQuestion}. */
@ParametersAreNonnullByDefault
public final class OspfProcessConfigurationAnswerer extends Answerer {

  static final String COL_NODE = "Node";
  static final String COL_VRF = "VRF";
  static final String COL_PROCESS_ID = "Process_ID";

  // this list also ensures order of columns excluding keys
  static final List<String> COLUMNS_FROM_PROP_SPEC =
      ImmutableList.of(
          AREAS, REFERENCE_BANDWIDTH, ROUTER_ID, EXPORT_POLICY_SOURCES, AREA_BORDER_ROUTER);

  public OspfProcessConfigurationAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    OspfProcessConfigurationQuestion question = (OspfProcessConfigurationQuestion) _question;
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Set<String> nodes = question.getNodesSpecifier().resolve(_batfish.specifierContext());

    Set<String> matchingProperties =
        ImmutableSet.copyOf(question.getProperties().getMatchingProperties());
    List<String> orderedProperties =
        COLUMNS_FROM_PROP_SPEC.stream()
            .filter(matchingProperties::contains)
            .collect(ImmutableList.toImmutableList());
    TableMetadata tableMetadata =
        createTableMetadata(
            question.getDisplayHints() != null ? question.getDisplayHints().getTextDesc() : null,
            orderedProperties);

    TableAnswerElement answer = new TableAnswerElement(tableMetadata);

    Multiset<Row> propertyRows =
        getRows(orderedProperties, configurations, nodes, tableMetadata.toColumnMap());

    answer.postProcessAnswer(question, propertyRows);
    return answer;
  }

  @VisibleForTesting
  static List<ColumnMetadata> createColumnMetadata(List<String> properties) {
    List<ColumnMetadata> columnMetadatas = new ArrayList<>();
    columnMetadatas.add(new ColumnMetadata(COL_NODE, Schema.NODE, "Node", true, false));
    columnMetadatas.add(new ColumnMetadata(COL_VRF, Schema.STRING, "VRF", true, false));
    columnMetadatas.add(
        new ColumnMetadata(COL_PROCESS_ID, Schema.STRING, "Process ID", true, false));
    for (String property : properties) {
      columnMetadatas.add(
          new ColumnMetadata(
              property,
              OspfPropertySpecifier.JAVA_MAP.get(property).getSchema(),
              "Property " + property,
              false,
              true));
    }
    return columnMetadatas;
  }

  /** Creates a {@link TableMetadata} object from the question. */
  @VisibleForTesting
  static TableMetadata createTableMetadata(
      @Nullable String textDescription, List<String> propertiesList) {
    return new TableMetadata(
        createColumnMetadata(propertiesList),
        textDescription == null
            ? String.format(
                "Configuration of OSPF process ${%s}:${%s}: ${%s}",
                COL_NODE, COL_VRF, COL_PROCESS_ID)
            : textDescription);
  }

  @VisibleForTesting
  static Multiset<Row> getRows(
      List<String> properties,
      Map<String, Configuration> configurations,
      Set<String> nodes,
      Map<String, ColumnMetadata> columnMetadata) {

    Multiset<Row> rows = HashMultiset.create();
    nodes.forEach(
        nodeName -> {
          configurations
              .get(nodeName)
              .getVrfs()
              .values()
              .forEach(
                  vrf -> {
                    OspfProcess ospfProcess = vrf.getOspfProcess();
                    if (ospfProcess == null) {
                      return;
                    }
                    rows.add(
                        getRow(nodeName, vrf.getName(), ospfProcess, properties, columnMetadata));
                  });
        });
    return rows;
  }

  @VisibleForTesting
  static Row getRow(
      String nodeName,
      String vrfName,
      OspfProcess ospfProcess,
      List<String> properties,
      Map<String, ColumnMetadata> columnMetadataMap) {
    RowBuilder rowBuilder =
        Row.builder(columnMetadataMap)
            .put(COL_NODE, new Node(nodeName))
            .put(COL_VRF, vrfName)
            .put(COL_PROCESS_ID, ospfProcess.getProcessId());

    for (String property : properties) {
      PropertyDescriptor<OspfProcess> propertyDescriptor =
          OspfPropertySpecifier.JAVA_MAP.get(property);
      try {
        PropertySpecifier.fillProperty(propertyDescriptor, ospfProcess, property, rowBuilder);
      } catch (ClassCastException e) {
        throw new BatfishException(
            String.format(
                "Type mismatch between property value ('%s') and Schema ('%s') for property '%s' for OSPF process '%s->%s-%s': %s",
                propertyDescriptor.getGetter().apply(ospfProcess),
                propertyDescriptor.getSchema(),
                property,
                nodeName,
                vrfName,
                ospfProcess,
                e.getMessage()),
            e);
      }
    }
    return rowBuilder.build();
  }
}

package org.batfish.question.ospfarea;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

/** Implements {@link OspfAreaConfigurationQuestion}. */
public class OspfAreaConfigurationAnswerer extends Answerer {

  static final String COL_NODE = "Node";
  static final String COL_VRF = "VRF";
  static final String COL_PROCESS_ID = "Process_ID";
  static final String COL_AREA = "Area";
  static final String COL_AREA_TYPE = "Area_Type";
  static final String COL_ACTIVE_INTERFACES = "Active_Interfaces";
  static final String COL_PASSIVE_INTERFACES = "Passive_Interfaces";

  public OspfAreaConfigurationAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    OspfAreaConfigurationQuestion question = (OspfAreaConfigurationQuestion) _question;
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    Set<String> nodes = question.getNodesSpecifier().resolve(_batfish.specifierContext());
    TableMetadata tableMetadata = createTableMetadata();

    TableAnswerElement answer = new TableAnswerElement(tableMetadata);

    Multiset<Row> propertyRows = getRows(configurations, nodes, tableMetadata.toColumnMap());

    answer.postProcessAnswer(question, propertyRows);
    return answer;
  }

  /** Creates a {@link TableMetadata} object from the question. */
  @VisibleForTesting
  static TableMetadata createTableMetadata() {
    ImmutableList.Builder<ColumnMetadata> columnBuilder = ImmutableList.builder();
    columnBuilder.add(new ColumnMetadata(COL_NODE, Schema.NODE, "Node", true, false));
    columnBuilder.add(new ColumnMetadata(COL_VRF, Schema.STRING, "VRF", true, false));
    columnBuilder.add(new ColumnMetadata(COL_PROCESS_ID, Schema.STRING, "Process ID", true, false));
    columnBuilder.add(new ColumnMetadata(COL_AREA, Schema.STRING, "Area Number", true, false));
    columnBuilder.add(new ColumnMetadata(COL_AREA_TYPE, Schema.STRING, "Area Type", false, true));
    columnBuilder.add(
        new ColumnMetadata(
            COL_ACTIVE_INTERFACES, Schema.set(Schema.STRING), "Active Interfaces", false, true));
    columnBuilder.add(
        new ColumnMetadata(
            COL_PASSIVE_INTERFACES, Schema.set(Schema.STRING), "Passive Interfaces", false, true));

    return new TableMetadata(columnBuilder.build(), "Display OSPF information areas");
  }

  @VisibleForTesting
  static Multiset<Row> getRows(
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
                    for (OspfArea area : ospfProcess.getAreas().values()) {
                      rows.add(
                          getRow(
                              configurations.get(nodeName),
                              nodeName,
                              vrf.getName(),
                              ospfProcess.getProcessId(),
                              area,
                              columnMetadata));
                    }
                  });
        });
    return rows;
  }

  private static Row getRow(
      @Nonnull Configuration configuration,
      String nodeName,
      String vrfName,
      @Nullable String processId,
      OspfArea ospfArea,
      Map<String, ColumnMetadata> columnMetadata) {

    RowBuilder rowBuilder =
        Row.builder(columnMetadata)
            .put(COL_NODE, new Node(nodeName))
            .put(COL_VRF, vrfName)
            .put(COL_PROCESS_ID, processId)
            .put(COL_AREA, ospfArea.getAreaNumber())
            .put(COL_AREA_TYPE, ospfArea.getStubType());

    ImmutableList.Builder<String> activeInterfaces = ImmutableList.builder();
    ImmutableList.Builder<String> passiveInterfaces = ImmutableList.builder();
    for (String ifaceName : ospfArea.getInterfaces()) {
      Interface iface = configuration.getAllInterfaces().get(ifaceName);
      if (iface.getOspfPassive()) {
        passiveInterfaces.add(ifaceName);
      } else {
        activeInterfaces.add(ifaceName);
      }
    }
    rowBuilder.put(COL_ACTIVE_INTERFACES, activeInterfaces.build());
    rowBuilder.put(COL_PASSIVE_INTERFACES, passiveInterfaces.build());
    return rowBuilder.build();
  }
}

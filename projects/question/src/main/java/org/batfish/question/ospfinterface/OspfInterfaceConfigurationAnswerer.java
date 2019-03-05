package org.batfish.question.ospfinterface;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_AREA_NAME;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_COST;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_HELLO_MULTIPLIER;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_PASSIVE;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_POINT_TO_POINT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.questions.InterfacePropertySpecifier;
import org.batfish.datamodel.questions.PropertySpecifier;
import org.batfish.datamodel.questions.PropertySpecifier.PropertyDescriptor;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

/** Implements {@link OspfInterfaceConfigurationQuestion}. */
@ParametersAreNonnullByDefault
public final class OspfInterfaceConfigurationAnswerer extends Answerer {

  static final String COL_INTERFACE = "Interface";
  static final String COL_VRF = "VRF";
  static final String COL_PROCESS_ID = "Process_ID";

  // this list also ensures order of columns excluding keys
  static final List<String> COLUMNS_FROM_PROP_SPEC =
      ImmutableList.of(
          OSPF_AREA_NAME, OSPF_PASSIVE, OSPF_COST, OSPF_POINT_TO_POINT, OSPF_HELLO_MULTIPLIER);

  public OspfInterfaceConfigurationAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    OspfInterfaceConfigurationQuestion question = (OspfInterfaceConfigurationQuestion) _question;
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
    ImmutableList.Builder<ColumnMetadata> columnMetadatas = ImmutableList.builder();
    columnMetadatas.add(
        new ColumnMetadata(COL_INTERFACE, Schema.INTERFACE, "Interface", true, false));
    columnMetadatas.add(new ColumnMetadata(COL_VRF, Schema.STRING, "VRF", true, false));
    columnMetadatas.add(
        new ColumnMetadata(COL_PROCESS_ID, Schema.STRING, "Process ID", true, false));
    for (String property : properties) {
      columnMetadatas.add(
          new ColumnMetadata(
              property,
              InterfacePropertySpecifier.JAVA_MAP.get(property).getSchema(),
              "Property " + property,
              false,
              true));
    }
    return columnMetadatas.build();
  }

  /** Creates a {@link TableMetadata} object from the question. */
  @VisibleForTesting
  static TableMetadata createTableMetadata(
      @Nullable String textDescription, List<String> propertiesList) {
    return new TableMetadata(
        createColumnMetadata(propertiesList),
        firstNonNull(
            textDescription, String.format("Configuration of OSPF Interface {%s}", COL_INTERFACE)));
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
                    List<String> ifaces =
                        ospfProcess.getAreas().values().stream()
                            .flatMap(area -> area.getInterfaces().stream())
                            .collect(ImmutableList.toImmutableList());
                    for (String iface : ifaces) {
                      Interface ifaceObject =
                          configurations.get(nodeName).getAllInterfaces().get(iface);
                      if (ifaceObject == null) {
                        continue;
                      }
                      rows.add(
                          getRow(
                              nodeName,
                              ospfProcess.getProcessId(),
                              ifaceObject,
                              properties,
                              columnMetadata));
                    }
                  });
        });
    return rows;
  }

  private static Row getRow(
      String nodeName,
      @Nullable String ospfProcessId,
      Interface iface,
      List<String> properties,
      Map<String, ColumnMetadata> columnMetadataMap) {
    RowBuilder rowBuilder =
        Row.builder(columnMetadataMap)
            .put(COL_INTERFACE, new NodeInterfacePair(nodeName, iface.getName()))
            .put(COL_VRF, iface.getVrfName())
            .put(COL_PROCESS_ID, ospfProcessId);

    for (String property : properties) {
      PropertyDescriptor<Interface> propertyDescriptor =
          InterfacePropertySpecifier.JAVA_MAP.get(property);
      try {
        PropertySpecifier.fillProperty(propertyDescriptor, iface, property, rowBuilder);
      } catch (ClassCastException e) {
        throw new BatfishException(
            String.format(
                "Type mismatch between property value ('%s') and Schema ('%s') for property '%s' for Interface '%s->%s-%s': %s",
                propertyDescriptor.getGetter().apply(iface),
                propertyDescriptor.getSchema(),
                property,
                nodeName,
                iface.getVrfName(),
                iface.getName(),
                e.getMessage()),
            e);
      }
    }
    return rowBuilder.build();
  }
}

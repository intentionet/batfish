package org.batfish.question.ospfsession;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.ospf.OspfNeighborConfig;
import org.batfish.datamodel.ospf.OspfNeighborConfigId;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.OspfSessionProperties;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;

/** Implements {@link OspfSessionCompatibilityQuestion}. */
public class OspfSessionCompatibilityAnswerer extends Answerer {

  static final String COL_INTERFACE = "Interface";
  static final String COL_VRF = "VRF";
  static final String COL_IP = "IP";
  static final String COL_AREA = "Area";

  static final String COL_REMOTE_INTERFACE = "Remote_Interface";
  static final String COL_REMOTE_VRF = "Remote_VRF";
  static final String COL_REMOTE_IP = "Remote_IP";
  static final String COL_REMOTE_AREA = "Remote_Area";

  // no column for session compatibility since only compatible sessions are shown

  public OspfSessionCompatibilityAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer() {
    OspfSessionCompatibilityQuestion question = (OspfSessionCompatibilityQuestion) _question;
    Map<String, Configuration> configurations = _batfish.loadConfigurations();

    Set<String> nodes = question.getNodesSpecifier().resolve(_batfish.specifierContext());
    Set<String> remoteNodes =
        question.getRemoteNodesSpecifier().resolve(_batfish.specifierContext());

    TableMetadata tableMetadata = createTableMetadata();

    TableAnswerElement answer = new TableAnswerElement(tableMetadata);

    Multiset<Row> propertyRows =
        getRows(
            configurations,
            nodes,
            remoteNodes,
            _batfish.getTopologyProvider().getOspfTopology(_batfish.getNetworkSnapshot()),
            tableMetadata.toColumnMap());

    answer.postProcessAnswer(question, propertyRows);
    return answer;
  }

  @VisibleForTesting
  static Multiset<Row> getRows(
      Map<String, Configuration> configurations,
      Set<String> nodes,
      Set<String> remoteNodes,
      OspfTopology ospfTopology,
      Map<String, ColumnMetadata> columnMetadataMap) {
    Multiset<Row> rows = HashMultiset.create();
    for (String node : nodes) {
      Configuration configuration = configurations.get(node);
      configuration
          .getVrfs()
          .values()
          .forEach(
              vrf -> {
                OspfProcess ospfProcess = vrf.getOspfProcess();
                if (ospfProcess == null) {
                  return;
                }
                ospfProcess
                    .getOspfNeighborConfigs()
                    .keySet()
                    .forEach(
                        iface -> {
                          OspfNeighborConfigId ospfNeighborConfigId =
                              new OspfNeighborConfigId(node, vrf.getName(), iface);
                          Set<OspfNeighborConfigId> filteredNeighbors =
                              ospfTopology.neighbors(ospfNeighborConfigId).stream()
                                  .filter(
                                      neighborConfigId ->
                                          remoteNodes.contains(neighborConfigId.getHostname()))
                                  .collect(ImmutableSet.toImmutableSet());
                          rows.addAll(
                              getRowsForAllNeighbors(
                                  configurations,
                                  ospfNeighborConfigId,
                                  filteredNeighbors,
                                  ospfTopology,
                                  columnMetadataMap));
                        });
              });
    }
    return rows;
  }

  private static Multiset<Row> getRowsForAllNeighbors(
      Map<String, Configuration> configurations,
      OspfNeighborConfigId nodeU,
      Set<OspfNeighborConfigId> nodeVs,
      OspfTopology ospfTopology,
      Map<String, ColumnMetadata> columnMetadataMap) {
    Multiset<Row> rows = HashMultiset.create();
    NetworkConfigurations nf = NetworkConfigurations.of(configurations);
    Optional<OspfNeighborConfig> nodeUConfig = nf.getOspfNeighborConfig(nodeU);
    if (!nodeUConfig.isPresent()) {
      return rows;
    }
    for (OspfNeighborConfigId nodeV : nodeVs) {
      Optional<OspfNeighborConfig> nodeVConfig = nf.getOspfNeighborConfig(nodeV);
      if (!nodeVConfig.isPresent()) {
        continue;
      }
      Optional<OspfSessionProperties> session =
          ospfTopology.getSession(OspfTopology.makeEdge(nodeU, nodeV));
      session.ifPresent(
          ospfSessionProperties ->
              rows.add(
                  createRow(
                      nodeUConfig.get(),
                      nodeVConfig.get(),
                      ospfSessionProperties,
                      columnMetadataMap)));
    }
    return rows;
  }

  private static Row createRow(
      OspfNeighborConfig nodeU,
      OspfNeighborConfig nodeV,
      OspfSessionProperties session,
      Map<String, ColumnMetadata> columnMetadataMap) {
    return Row.builder(columnMetadataMap)
        .put(COL_INTERFACE, new NodeInterfacePair(nodeU.getHostname(), nodeU.getInterfaceName()))
        .put(COL_VRF, nodeU.getVrfName())
        .put(COL_IP, session.getIpLink().getIp1())
        .put(COL_AREA, nodeU.getArea())
        .put(
            COL_REMOTE_INTERFACE,
            new NodeInterfacePair(nodeV.getHostname(), nodeV.getInterfaceName()))
        .put(COL_REMOTE_VRF, nodeV.getVrfName())
        .put(COL_REMOTE_IP, session.getIpLink().getIp2())
        .put(COL_REMOTE_AREA, nodeV.getArea())
        .build();
  }

  @VisibleForTesting
  static TableMetadata createTableMetadata() {
    ImmutableList.Builder<ColumnMetadata> columnBuilder = ImmutableList.builder();
    columnBuilder.add(
        new ColumnMetadata(COL_INTERFACE, Schema.INTERFACE, "Interface", true, false));
    columnBuilder.add(new ColumnMetadata(COL_VRF, Schema.STRING, "VRF", true, false));
    columnBuilder.add(new ColumnMetadata(COL_IP, Schema.IP, "Ip", true, false));
    columnBuilder.add(new ColumnMetadata(COL_AREA, Schema.LONG, "Area", true, false));
    columnBuilder.add(
        new ColumnMetadata(
            COL_REMOTE_INTERFACE, Schema.INTERFACE, "Remote Interface", false, true));
    columnBuilder.add(new ColumnMetadata(COL_REMOTE_VRF, Schema.STRING, "Remote VRF", false, true));
    columnBuilder.add(new ColumnMetadata(COL_REMOTE_IP, Schema.IP, "Remote IP", false, true));
    columnBuilder.add(new ColumnMetadata(COL_REMOTE_AREA, Schema.LONG, "Remote Area", false, true));

    return new TableMetadata(columnBuilder.build(), "Display OSPF sessions");
  }
}

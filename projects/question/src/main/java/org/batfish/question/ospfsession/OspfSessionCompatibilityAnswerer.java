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
import org.batfish.datamodel.ospf.CandidateOspfTopology;
import org.batfish.datamodel.ospf.OspfNeighborConfig;
import org.batfish.datamodel.ospf.OspfNeighborConfigId;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.OspfSessionStatus;
import org.batfish.datamodel.ospf.OspfTopologyUtils;
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
  static final String COL_SESSION_STATUS = "Session_Status";

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

    CandidateOspfTopology candidateTopo =
        OspfTopologyUtils.computeCandidateOspfTopology(
            NetworkConfigurations.of(configurations),
            _batfish.getTopologyProvider().getInitialLayer3Topology(_batfish.getNetworkSnapshot()));
    Multiset<Row> propertyRows =
        getRows(
            configurations,
            nodes,
            remoteNodes,
            candidateTopo,
            tableMetadata.toColumnMap(),
            question.getStatusSet());

    answer.postProcessAnswer(question, propertyRows);
    return answer;
  }

  @VisibleForTesting
  static Multiset<Row> getRows(
      Map<String, Configuration> configurations,
      Set<String> nodes,
      Set<String> remoteNodes,
      CandidateOspfTopology ospfTopology,
      Map<String, ColumnMetadata> columnMetadataMap,
      Set<OspfSessionStatus> statuses) {
    Multiset<Row> rows = HashMultiset.create();
    for (String node : nodes) {
      Configuration configuration = configurations.get(node);
      configuration
          .getVrfs()
          .values()
          .forEach(
              vrf -> {
                for (OspfProcess ospfProcess : vrf.getOspfProcesses().values()) {
                  ospfProcess
                      .getOspfNeighborConfigs()
                      .keySet()
                      .forEach(
                          iface -> {
                            OspfNeighborConfigId ospfNeighborConfigId =
                                new OspfNeighborConfigId(
                                    node, vrf.getName(), ospfProcess.getProcessId(), iface);
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
                                    columnMetadataMap,
                                    statuses));
                          });
                }
              });
    }
    return rows;
  }

  private static Multiset<Row> getRowsForAllNeighbors(
      Map<String, Configuration> configurations,
      OspfNeighborConfigId nodeU,
      Set<OspfNeighborConfigId> nodeVs,
      CandidateOspfTopology ospfTopology,
      Map<String, ColumnMetadata> columnMetadataMap,
      Set<OspfSessionStatus> statuses) {
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
      Optional<OspfSessionStatus> session = ospfTopology.getSessionStatus(nodeU, nodeV);
      session.ifPresent(
          ospfSessionStatus -> {
            if (statuses.contains(ospfSessionStatus)) {
              rows.add(
                  createRow(
                      nodeUConfig.get(), nodeVConfig.get(), ospfSessionStatus, columnMetadataMap));
            }
          });
    }
    return rows;
  }

  private static Row createRow(
      OspfNeighborConfig nodeU,
      OspfNeighborConfig nodeV,
      OspfSessionStatus sessionStatus,
      Map<String, ColumnMetadata> columnMetadataMap) {
    return Row.builder(columnMetadataMap)
        .put(COL_INTERFACE, NodeInterfacePair.of(nodeU.getHostname(), nodeU.getInterfaceName()))
        .put(COL_VRF, nodeU.getVrfName())
        .put(COL_IP, nodeU.getIp())
        .put(COL_AREA, nodeU.getArea())
        .put(
            COL_REMOTE_INTERFACE,
            NodeInterfacePair.of(nodeV.getHostname(), nodeV.getInterfaceName()))
        .put(COL_REMOTE_VRF, nodeV.getVrfName())
        .put(COL_REMOTE_IP, nodeV.getIp())
        .put(COL_REMOTE_AREA, nodeV.getArea())
        .put(COL_SESSION_STATUS, sessionStatus.toString())
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
    columnBuilder.add(
        new ColumnMetadata(
            COL_SESSION_STATUS, Schema.STRING, "Status of the OSPF session", false, true));

    return new TableMetadata(columnBuilder.build(), "Display OSPF sessions");
  }
}

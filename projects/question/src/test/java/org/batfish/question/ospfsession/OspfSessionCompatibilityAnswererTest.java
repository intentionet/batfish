package org.batfish.question.ospfsession;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_AREA;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_INTERFACE;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_IP;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_REMOTE_AREA;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_REMOTE_INTERFACE;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_REMOTE_IP;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_REMOTE_VRF;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_SESSION_STATUS;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_VRF;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.createTableMetadata;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.getRows;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.ospf.CandidateOspfTopology;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfNeighborConfig;
import org.batfish.datamodel.ospf.OspfNeighborConfigId;
import org.batfish.datamodel.ospf.OspfSessionStatus;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link OspfSessionCompatibilityAnswerer} */
public class OspfSessionCompatibilityAnswererTest {

  private Map<String, Configuration> _configurations;
  private CandidateOspfTopology _ospfTopology;

  private Configuration buildConfig(
      NetworkFactory nf, String hostname, String vrfName, String iface, Ip addr) {
    Configuration configuration =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(hostname)
            .build();
    Vrf vrf = nf.vrfBuilder().setName(vrfName).setOwner(configuration).build();

    nf.interfaceBuilder()
        .setAddress(ConcreteInterfaceAddress.create(addr, 31))
        .setName(iface)
        .setVrf(vrf)
        .setOwner(configuration)
        .setOspfSettings(OspfInterfaceSettings.defaultSettingsBuilder().setProcess("proc").build())
        .build();

    nf.ospfProcessBuilder()
        .setVrf(vrf)
        .setProcessId("proc")
        .setNeighbors(
            ImmutableMap.of(
                iface,
                OspfNeighborConfig.builder()
                    .setArea(1L)
                    .setHostname(hostname)
                    .setInterfaceName(iface)
                    .setVrfName(vrfName)
                    .setIp(addr)
                    .build()))
        .setRouterId(Ip.ZERO)
        .build();
    return configuration;
  }

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    _configurations =
        ImmutableMap.of(
            "configuration_u",
            buildConfig(nf, "configuration_u", "vrf_u", "int_u", Ip.parse("1.1.1.2")),
            "configuration_v",
            buildConfig(nf, "configuration_v", "vrf_v", "int_v", Ip.parse("1.1.1.3")),
            "configuration_w",
            buildConfig(nf, "configuration_w", "vrf_w", "int_w", Ip.parse("1.1.1.4")),
            "configuration_x",
            buildConfig(nf, "configuration_x", "vrf_x", "int_x", Ip.parse("1.1.1.5")));

    MutableValueGraph<OspfNeighborConfigId, OspfSessionStatus> ospfGraph =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();

    ospfGraph.putEdgeValue(
        new OspfNeighborConfigId("configuration_u", "vrf_u", "proc", "int_u"),
        new OspfNeighborConfigId("configuration_v", "vrf_v", "proc", "int_v"),
        OspfSessionStatus.ESTABLISHED);
    ospfGraph.putEdgeValue(
        new OspfNeighborConfigId("configuration_w", "vrf_w", "proc", "int_w"),
        new OspfNeighborConfigId("configuration_x", "vrf_x", "proc", "int_x"),
        OspfSessionStatus.NETWORK_TYPE_MISMATCH);
    _ospfTopology = new CandidateOspfTopology(ImmutableValueGraph.copyOf(ospfGraph));
  }

  @Test
  public void testGetRows() {
    Multiset<Row> rows =
        getRows(
            _configurations,
            ImmutableSet.of("configuration_u", "configuration_w"),
            ImmutableSet.of("configuration_v", "configuration_x"),
            _ospfTopology,
            createTableMetadata().toColumnMap(),
            ImmutableSet.copyOf(OspfSessionStatus.values()));

    Iterator<Row> i = rows.iterator();
    assertThat(
        i.next(),
        allOf(
            hasColumn(
                COL_INTERFACE,
                equalTo(NodeInterfacePair.of("configuration_u", "int_u")),
                Schema.INTERFACE),
            hasColumn(COL_VRF, equalTo("vrf_u"), Schema.STRING),
            hasColumn(COL_IP, equalTo(Ip.parse("1.1.1.2")), Schema.IP),
            hasColumn(COL_AREA, equalTo(1L), Schema.LONG),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(NodeInterfacePair.of("configuration_v", "int_v")),
                Schema.INTERFACE),
            hasColumn(COL_REMOTE_VRF, equalTo("vrf_v"), Schema.STRING),
            hasColumn(COL_REMOTE_IP, equalTo(Ip.parse("1.1.1.3")), Schema.IP),
            hasColumn(COL_REMOTE_AREA, equalTo(1L), Schema.LONG),
            hasColumn(
                COL_SESSION_STATUS,
                equalTo(OspfSessionStatus.ESTABLISHED.toString()),
                Schema.STRING)));
    Row r2 = i.next();
    assertThat(
        r2,
        allOf(
            hasColumn(
                COL_INTERFACE,
                equalTo(NodeInterfacePair.of("configuration_w", "int_w")),
                Schema.INTERFACE),
            hasColumn(COL_VRF, equalTo("vrf_w"), Schema.STRING),
            hasColumn(COL_IP, equalTo(Ip.parse("1.1.1.4")), Schema.IP),
            hasColumn(COL_AREA, equalTo(1L), Schema.LONG),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(NodeInterfacePair.of("configuration_x", "int_x")),
                Schema.INTERFACE),
            hasColumn(COL_REMOTE_VRF, equalTo("vrf_x"), Schema.STRING),
            hasColumn(COL_REMOTE_IP, equalTo(Ip.parse("1.1.1.5")), Schema.IP),
            hasColumn(COL_REMOTE_AREA, equalTo(1L), Schema.LONG),
            hasColumn(
                COL_SESSION_STATUS,
                equalTo(OspfSessionStatus.NETWORK_TYPE_MISMATCH.toString()),
                Schema.STRING)));
  }

  @Test
  public void testGetRowsWithFilter() {
    Multiset<Row> rowsWithNodesFilter =
        getRows(
            _configurations,
            ImmutableSet.of(),
            ImmutableSet.of("configuration_v"),
            _ospfTopology,
            createTableMetadata().toColumnMap(),
            ImmutableSet.copyOf(OspfSessionStatus.values()));

    Multiset<Row> rowsWithRemoteNodesFilter =
        getRows(
            _configurations,
            ImmutableSet.of("configuration_u"),
            ImmutableSet.of(),
            _ospfTopology,
            createTableMetadata().toColumnMap(),
            ImmutableSet.copyOf(OspfSessionStatus.values()));

    assertThat(rowsWithNodesFilter, hasSize(0));
    assertThat(rowsWithRemoteNodesFilter, hasSize(0));
  }

  @Test
  public void testGetRowsWithFilterStatus() {
    Multiset<Row> rowsWithStatusFilter =
        getRows(
            _configurations,
            ImmutableSet.of("configuration_u", "configuration_w"),
            ImmutableSet.of("configuration_v", "configuration_x"),
            _ospfTopology,
            createTableMetadata().toColumnMap(),
            ImmutableSet.of(OspfSessionStatus.ESTABLISHED));

    // Should only get the one established session result
    assertThat(rowsWithStatusFilter, hasSize(1));
  }

  @Test
  public void testMetaData() {
    List<ColumnMetadata> metas =
        OspfSessionCompatibilityAnswerer.createTableMetadata().getColumnMetadata();

    assertThat(
        metas.stream().map(ColumnMetadata::getName).collect(ImmutableList.toImmutableList()),
        equalTo(
            ImmutableList.builder()
                .add(COL_INTERFACE)
                .add(COL_VRF)
                .add(COL_IP)
                .add(COL_AREA)
                .add(COL_REMOTE_INTERFACE)
                .add(COL_REMOTE_VRF)
                .add(COL_REMOTE_IP)
                .add(COL_REMOTE_AREA)
                .add(COL_SESSION_STATUS)
                .build()));
  }
}

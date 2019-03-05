package org.batfish.question.ospfsession;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_AREA;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_INTERFACE;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_IP;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_REMOTE_AREA;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_REMOTE_INTERFACE;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_REMOTE_IP;
import static org.batfish.question.ospfsession.OspfSessionCompatibilityAnswerer.COL_REMOTE_VRF;
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
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpLink;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.ospf.OspfNeighborConfig;
import org.batfish.datamodel.ospf.OspfNeighborConfigId;
import org.batfish.datamodel.ospf.OspfSessionProperties;
import org.batfish.datamodel.ospf.OspfTopology;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link OspfSessionCompatibilityAnswerer} */
public class OspfSessionCompatibilityAnswererTest {

  private Map<String, Configuration> _configurations;
  private OspfTopology _ospfTopology;

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration configurationU =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("configuration_u")
            .build();

    Configuration configurationV =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("configuration_v")
            .build();

    Vrf vrfU = nf.vrfBuilder().setName("vrf_u").setOwner(configurationU).build();
    Vrf vrfV = nf.vrfBuilder().setName("vrf_v").setOwner(configurationV).build();

    nf.interfaceBuilder()
        .setAddress(new InterfaceAddress(Ip.parse("1.1.1.2"), 31))
        .setName("int_u")
        .setVrf(vrfU)
        .setOwner(configurationU)
        .build();

    nf.interfaceBuilder()
        .setAddress(new InterfaceAddress(Ip.parse("1.1.1.3"), 31))
        .setName("int_v")
        .setVrf(vrfV)
        .setOwner(configurationV)
        .build();

    nf.ospfProcessBuilder()
        .setVrf(vrfU)
        .setNeighbors(
            ImmutableMap.of(
                "int_u",
                OspfNeighborConfig.builder()
                    .setArea(1L)
                    .setHostname("configuration_u")
                    .setInterfaceName("int_u")
                    .setVrfName("vrf_u")
                    .build()))
        .build();
    nf.ospfProcessBuilder()
        .setVrf(vrfV)
        .setNeighbors(
            ImmutableMap.of(
                "int_v",
                OspfNeighborConfig.builder()
                    .setArea(1L)
                    .setHostname("configuration_v")
                    .setInterfaceName("int_v")
                    .setVrfName("vrf_v")
                    .build()))
        .build();
    _configurations =
        ImmutableMap.of("configuration_u", configurationU, "configuration_v", configurationV);

    MutableValueGraph<OspfNeighborConfigId, OspfSessionProperties> ospfGraph =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();

    ospfGraph.putEdgeValue(
        new OspfNeighborConfigId("configuration_u", "vrf_u", "int_u"),
        new OspfNeighborConfigId("configuration_v", "vrf_v", "int_v"),
        new OspfSessionProperties(new IpLink(Ip.parse("1.1.1.2"), Ip.parse("1.1.1.3"))));
    _ospfTopology = new OspfTopology(ImmutableValueGraph.copyOf(ospfGraph));
  }

  @Test
  public void testGetRows() {
    Multiset<Row> rows =
        getRows(
            _configurations,
            ImmutableSet.of("configuration_u"),
            ImmutableSet.of("configuration_v"),
            _ospfTopology,
            createTableMetadata().toColumnMap());

    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(
                COL_INTERFACE,
                equalTo(new NodeInterfacePair("configuration_u", "int_u")),
                Schema.INTERFACE),
            hasColumn(COL_VRF, equalTo("vrf_u"), Schema.STRING),
            hasColumn(COL_IP, equalTo(Ip.parse("1.1.1.2")), Schema.IP),
            hasColumn(COL_AREA, equalTo(1L), Schema.LONG)));
    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("configuration_v", "int_v")),
                Schema.INTERFACE),
            hasColumn(COL_REMOTE_VRF, equalTo("vrf_v"), Schema.STRING),
            hasColumn(COL_REMOTE_IP, equalTo(Ip.parse("1.1.1.3")), Schema.IP),
            hasColumn(COL_REMOTE_AREA, equalTo(1L), Schema.LONG)));
  }

  @Test
  public void testGetRowsWithFilter() {
    Multiset<Row> rowsWithNodesFilter =
        getRows(
            _configurations,
            ImmutableSet.of(),
            ImmutableSet.of("configuration_v"),
            _ospfTopology,
            createTableMetadata().toColumnMap());

    Multiset<Row> rowsWithRemoteNodesFilter =
        getRows(
            _configurations,
            ImmutableSet.of("configuration_u"),
            ImmutableSet.of(),
            _ospfTopology,
            createTableMetadata().toColumnMap());

    assertThat(rowsWithNodesFilter, hasSize(0));
    assertThat(rowsWithRemoteNodesFilter, hasSize(0));
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
                .build()));
  }
}

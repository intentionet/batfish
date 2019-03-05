package org.batfish.question.ospfinterface;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_AREA_NAME;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_COST;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_HELLO_MULTIPLIER;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_PASSIVE;
import static org.batfish.datamodel.questions.InterfacePropertySpecifier.OSPF_POINT_TO_POINT;
import static org.batfish.question.ospfinterface.OspfInterfaceConfigurationAnswerer.COLUMNS_FROM_PROP_SPEC;
import static org.batfish.question.ospfinterface.OspfInterfaceConfigurationAnswerer.COL_INTERFACE;
import static org.batfish.question.ospfinterface.OspfInterfaceConfigurationAnswerer.COL_PROCESS_ID;
import static org.batfish.question.ospfinterface.OspfInterfaceConfigurationAnswerer.COL_VRF;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import java.util.List;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.junit.Test;

/** Test for {@link OspfInterfaceConfigurationAnswerer} */
public class OspfInterfaceConfigAnswererTest {

  @Test
  public void testGetRows() {
    NetworkFactory nf = new NetworkFactory();
    Configuration configuration =
        nf.configurationBuilder()
            .setHostname("test_conf")
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    Vrf vrf = nf.vrfBuilder().setName("test_vrf").setOwner(configuration).build();
    OspfArea ospfArea =
        nf.ospfAreaBuilder().setInterfaces(ImmutableSet.of("int1")).setNumber(1L).build();
    Interface iface =
        nf.interfaceBuilder()
            .setName("int1")
            .setOspfArea(ospfArea)
            .setOspfPassive(true)
            .setOspfCost(2)
            .setOspfPointToPoint(true)
            .setOwner(configuration)
            .setVrf(vrf)
            .build();
    iface.setOspfHelloMultiplier(2);
    nf.ospfProcessBuilder()
        .setProcessId("ospf_1")
        .setAreas(ImmutableSortedMap.of(1L, ospfArea))
        .setRouterId(Ip.parse("1.1.1.1"))
        .setVrf(vrf)
        .build();

    Multiset<Row> rows =
        OspfInterfaceConfigurationAnswerer.getRows(
            COLUMNS_FROM_PROP_SPEC,
            ImmutableMap.of(configuration.getHostname(), configuration),
            ImmutableSet.of(configuration.getHostname()),
            OspfInterfaceConfigurationAnswerer.createTableMetadata(null, COLUMNS_FROM_PROP_SPEC)
                .toColumnMap());
    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(COL_VRF, equalTo("test_vrf"), Schema.STRING),
            hasColumn(COL_PROCESS_ID, equalTo("ospf_1"), Schema.STRING),
            hasColumn(
                COL_INTERFACE,
                equalTo(new NodeInterfacePair("test_conf", "int1")),
                Schema.INTERFACE),
            hasColumn(OSPF_AREA_NAME, equalTo(1), Schema.INTEGER)));
    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(OSPF_PASSIVE, equalTo(true), Schema.BOOLEAN),
            hasColumn(OSPF_COST, equalTo(2), Schema.INTEGER),
            hasColumn(OSPF_POINT_TO_POINT, equalTo(true), Schema.BOOLEAN),
            hasColumn(OSPF_HELLO_MULTIPLIER, equalTo(2), Schema.INTEGER)));
  }

  @Test
  public void testMetaData() {
    List<ColumnMetadata> metas =
        OspfInterfaceConfigurationAnswerer.createColumnMetadata(COLUMNS_FROM_PROP_SPEC);

    assertThat(
        metas.stream().map(ColumnMetadata::getName).collect(ImmutableList.toImmutableList()),
        equalTo(
            ImmutableList.builder()
                .add(COL_INTERFACE)
                .add(COL_VRF)
                .add(COL_PROCESS_ID)
                .add(OSPF_AREA_NAME)
                .add(OSPF_PASSIVE)
                .add(OSPF_COST)
                .add(OSPF_POINT_TO_POINT)
                .add(OSPF_HELLO_MULTIPLIER)
                .build()));
  }
}

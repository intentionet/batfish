package org.batfish.question.ospfinterface;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.questions.OspfInterfacePropertySpecifier.OSPF_AREA_NAME;
import static org.batfish.datamodel.questions.OspfInterfacePropertySpecifier.OSPF_COST;
import static org.batfish.datamodel.questions.OspfInterfacePropertySpecifier.OSPF_DEAD_INTERVAL;
import static org.batfish.datamodel.questions.OspfInterfacePropertySpecifier.OSPF_ENABLED;
import static org.batfish.datamodel.questions.OspfInterfacePropertySpecifier.OSPF_HELLO_INTERVAL;
import static org.batfish.datamodel.questions.OspfInterfacePropertySpecifier.OSPF_NETWORK_TYPE;
import static org.batfish.datamodel.questions.OspfInterfacePropertySpecifier.OSPF_PASSIVE;
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
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfNetworkType;
import org.batfish.datamodel.questions.OspfInterfacePropertySpecifier;
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
    nf.interfaceBuilder()
        .setName("int1")
        .setOspfSettings(
            OspfInterfaceSettings.defaultSettingsBuilder()
                .setAreaName(ospfArea.getAreaNumber())
                .setPassive(true)
                .setCost(2)
                .setNetworkType(OspfNetworkType.POINT_TO_POINT)
                .setHelloMultiplier(2)
                .setHelloInterval(1)
                .setDeadInterval(2)
                .setInboundDistributeListPolicy("policy_name")
                .build())
        .setOwner(configuration)
        .setVrf(vrf)
        .build();
    ospfArea.addInterface("int1");
    nf.ospfProcessBuilder()
        .setProcessId("ospf_1")
        .setAreas(ImmutableSortedMap.of(1L, ospfArea))
        .setRouterId(Ip.parse("1.1.1.1"))
        .setVrf(vrf)
        .build();

    Multiset<Row> rows =
        OspfInterfaceConfigurationAnswerer.getRows(
            OspfInterfacePropertySpecifier.ALL.getMatchingProperties(),
            ImmutableMap.of(configuration.getHostname(), configuration),
            ImmutableSet.of(configuration.getHostname()),
            OspfInterfaceConfigurationAnswerer.createTableMetadata(
                    null, OspfInterfacePropertySpecifier.ALL.getMatchingProperties())
                .toColumnMap());
    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(COL_VRF, equalTo("test_vrf"), Schema.STRING),
            hasColumn(COL_PROCESS_ID, equalTo("ospf_1"), Schema.STRING),
            hasColumn(
                COL_INTERFACE,
                equalTo(NodeInterfacePair.of("test_conf", "int1")),
                Schema.INTERFACE),
            hasColumn(OSPF_AREA_NAME, equalTo(1), Schema.INTEGER)));
    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(OSPF_PASSIVE, equalTo(true), Schema.BOOLEAN),
            hasColumn(OSPF_ENABLED, equalTo(true), Schema.BOOLEAN),
            hasColumn(OSPF_COST, equalTo(2), Schema.INTEGER),
            hasColumn(
                OSPF_NETWORK_TYPE,
                equalTo(OspfNetworkType.POINT_TO_POINT.toString()),
                Schema.STRING),
            hasColumn(OSPF_DEAD_INTERVAL, equalTo(2), Schema.INTEGER),
            hasColumn(OSPF_HELLO_INTERVAL, equalTo(1), Schema.INTEGER)));
  }

  @Test
  public void testMetaData() {
    List<ColumnMetadata> metas =
        OspfInterfaceConfigurationAnswerer.createColumnMetadata(
            OspfInterfacePropertySpecifier.ALL.getMatchingProperties());

    assertThat(
        metas.stream().map(ColumnMetadata::getName).collect(ImmutableList.toImmutableList()),
        equalTo(
            ImmutableList.builder()
                .add(COL_INTERFACE)
                .add(COL_VRF)
                .add(COL_PROCESS_ID)
                .add(OSPF_AREA_NAME)
                .add(OSPF_ENABLED)
                .add(OSPF_PASSIVE)
                .add(OSPF_COST)
                .add(OSPF_NETWORK_TYPE)
                .add(OSPF_HELLO_INTERVAL)
                .add(OSPF_DEAD_INTERVAL)
                .build()));
  }
}

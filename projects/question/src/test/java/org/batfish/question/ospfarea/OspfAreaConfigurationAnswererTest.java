package org.batfish.question.ospfarea;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import java.util.List;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableMetadata;
import org.junit.Test;

/** Tests for {@link OspfAreaConfigurationAnswerer} */
public class OspfAreaConfigurationAnswererTest {

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
        nf.ospfAreaBuilder()
            .setStubType(StubType.STUB)
            .setInterfaces(ImmutableSortedSet.of("int1", "int2"))
            .setNumber(1L)
            .build();
    nf.ospfProcessBuilder()
        .setProcessId("ospf_1")
        .setAreas(ImmutableSortedMap.of(1L, ospfArea))
        .setRouterId(Ip.parse("1.1.1.1"))
        .setVrf(vrf)
        .build();
    nf.interfaceBuilder()
        .setVrf(vrf)
        .setName("int1")
        .setOwner(configuration)
        .setBandwidth(2d)
        .setOspfArea(ospfArea)
        .setOspfEnabled(true)
        .setOspfPassive(false)
        .build();
    nf.interfaceBuilder()
        .setVrf(vrf)
        .setName("int2")
        .setOwner(configuration)
        .setBandwidth(2d)
        .setOspfArea(ospfArea)
        .setOspfEnabled(true)
        .setOspfPassive(true)
        .build();

    TableMetadata tableMetadata = OspfAreaConfigurationAnswerer.createTableMetadata();
    Multiset<Row> rows =
        OspfAreaConfigurationAnswerer.getRows(
            ImmutableMap.of(configuration.getHostname(), configuration),
            ImmutableSet.of(configuration.getHostname()),
            tableMetadata.toColumnMap());
    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(
                OspfAreaConfigurationAnswerer.COL_NODE,
                equalTo(new Node("test_conf")),
                Schema.NODE),
            hasColumn(OspfAreaConfigurationAnswerer.COL_VRF, equalTo("test_vrf"), Schema.STRING),
            hasColumn(
                OspfAreaConfigurationAnswerer.COL_PROCESS_ID, equalTo("ospf_1"), Schema.STRING),
            hasColumn(OspfAreaConfigurationAnswerer.COL_AREA, equalTo("1"), Schema.STRING),
            hasColumn(
                OspfAreaConfigurationAnswerer.COL_AREA_TYPE, equalTo("STUB"), Schema.STRING)));
    assertThat(
        rows.iterator().next(),
        allOf(
            hasColumn(
                OspfAreaConfigurationAnswerer.COL_ACTIVE_INTERFACES,
                equalTo(ImmutableSet.of("int1")),
                Schema.set(Schema.STRING)),
            hasColumn(
                OspfAreaConfigurationAnswerer.COL_PASSIVE_INTERFACES,
                equalTo(ImmutableSet.of("int2")),
                Schema.set(Schema.STRING))));
  }

  @Test
  public void testMetaData() {
    List<ColumnMetadata> metas =
        OspfAreaConfigurationAnswerer.createTableMetadata().getColumnMetadata();

    assertThat(
        metas.stream().map(ColumnMetadata::getName).collect(ImmutableList.toImmutableList()),
        equalTo(
            ImmutableList.builder()
                .add(OspfAreaConfigurationAnswerer.COL_NODE)
                .add(OspfAreaConfigurationAnswerer.COL_VRF)
                .add(OspfAreaConfigurationAnswerer.COL_PROCESS_ID)
                .add(OspfAreaConfigurationAnswerer.COL_AREA)
                .add(OspfAreaConfigurationAnswerer.COL_AREA_TYPE)
                .add(OspfAreaConfigurationAnswerer.COL_ACTIVE_INTERFACES)
                .add(OspfAreaConfigurationAnswerer.COL_PASSIVE_INTERFACES)
                .build()));
  }
}

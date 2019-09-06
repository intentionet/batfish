package org.batfish.question.mlag;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.mlag.MlagPropertiesAnswerer.COL_MLAG_ID;
import static org.batfish.question.mlag.MlagPropertiesAnswerer.COL_MLAG_LOCAL_INTERFACE;
import static org.batfish.question.mlag.MlagPropertiesAnswerer.COL_MLAG_PEER_ADDRESS;
import static org.batfish.question.mlag.MlagPropertiesAnswerer.COL_MLAG_PEER_INTERFACE;
import static org.batfish.question.mlag.MlagPropertiesAnswerer.COL_NODE;
import static org.batfish.question.mlag.MlagPropertiesAnswerer.computeAnswer;
import static org.batfish.question.mlag.MlagPropertiesAnswerer.configToRow;
import static org.batfish.question.mlag.MlagPropertiesAnswerer.getAllMlagIds;
import static org.batfish.question.mlag.MlagPropertiesAnswerer.getMetadata;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Configuration.Builder;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Mlag;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.junit.Test;

/** Tests of {@link MlagPropertiesAnswerer} */
public final class MlagPropertiesAnswererTest {

  @Test
  public void testColumnOrder() {
    TableMetadata tableMetadata = MlagPropertiesAnswerer.getMetadata();

    assertThat(
        tableMetadata.getColumnMetadata().stream()
            .map(ColumnMetadata::getName)
            .collect(ImmutableList.toImmutableList()),
        equalTo(
            ImmutableList.of(
                COL_NODE,
                COL_MLAG_ID,
                COL_MLAG_PEER_ADDRESS,
                COL_MLAG_PEER_INTERFACE,
                COL_MLAG_LOCAL_INTERFACE)));
  }

  @Test
  public void testColumnSchemas() {
    TableMetadata tableMetadata = MlagPropertiesAnswerer.getMetadata();

    assertThat(
        tableMetadata.getColumnMetadata().stream()
            .map(ColumnMetadata::getSchema)
            .collect(ImmutableList.toImmutableList()),
        equalTo(
            ImmutableList.of(
                Schema.NODE, Schema.STRING, Schema.IP, Schema.INTERFACE, Schema.INTERFACE)));
  }

  @Test
  public void testConfigToRow() {
    Mlag m =
        Mlag.builder()
            .setId("ID")
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setPeerInterface("Port-Channel1")
            .build();
    final String hostname = "node";
    Row r = configToRow(hostname, m);
    assertThat(
        r,
        equalTo(
            Row.builder(getMetadata().toColumnMap())
                .put(COL_NODE, new Node(hostname))
                .put(COL_MLAG_ID, m.getId())
                .put(COL_MLAG_PEER_ADDRESS, m.getPeerAddress())
                .put(COL_MLAG_PEER_INTERFACE, NodeInterfacePair.of(hostname, "Port-Channel1"))
                .put(COL_MLAG_LOCAL_INTERFACE, null)
                .build()));
  }

  @Test
  public void testFilterNodes() {
    ImmutableSortedMap<String, Configuration> configs = getConfigs();
    TableAnswerElement answer =
        computeAnswer(ImmutableSet.of("n1"), getAllMlagIds(configs), configs);

    assertThat(
        answer.getRowsList(),
        contains(hasColumn(equalTo(COL_NODE), equalTo(new Node("n1")), Schema.NODE)));
  }

  @Test
  public void testFilterMlags() {
    ImmutableSortedMap<String, Configuration> configs = getConfigs();
    TableAnswerElement answer =
        computeAnswer(ImmutableSet.of("n1", "n2"), ImmutableSet.of("ID2"), configs);

    assertThat(
        answer.getRowsList(),
        contains(
            allOf(
                hasColumn(equalTo(COL_NODE), equalTo(new Node("n2")), Schema.NODE),
                hasColumn(equalTo(COL_MLAG_ID), equalTo("ID2"), Schema.STRING))));
  }

  private static ImmutableSortedMap<String, Configuration> getConfigs() {
    NetworkFactory nf = new NetworkFactory();
    Builder cb = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    Configuration c1 = cb.setHostname("n1").build();
    c1.setMlags(ImmutableMap.of("ID1", Mlag.builder().setId("ID1").build()));
    Configuration c2 = cb.setHostname("n2").build();
    c2.setMlags(ImmutableMap.of("ID2", Mlag.builder().setId("ID2").build()));

    return ImmutableSortedMap.of("n1", c1, "n2", c2);
  }
}

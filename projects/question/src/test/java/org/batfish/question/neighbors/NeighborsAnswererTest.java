package org.batfish.question.neighbors;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_AS_NUMBER;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_INTERFACE;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_IP;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_IPS;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_NODE;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_REMOTE_AS_NUMBER;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_REMOTE_INTERFACE;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_REMOTE_IP;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_REMOTE_IPS;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_REMOTE_NODE;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_REMOTE_VLAN;
import static org.batfish.question.neighbors.NeighborsAnswerer.COL_VLAN;
import static org.batfish.question.neighbors.NeighborsAnswerer.bgpEdgeToRow;
import static org.batfish.question.neighbors.NeighborsAnswerer.eigrpEdgeToRow;
import static org.batfish.question.neighbors.NeighborsAnswerer.isisEdgeToRow;
import static org.batfish.question.neighbors.NeighborsAnswerer.layer1EdgeToRow;
import static org.batfish.question.neighbors.NeighborsAnswerer.layer2EdgeToRow;
import static org.batfish.question.neighbors.NeighborsAnswerer.layer3EdgeToRow;
import static org.batfish.question.neighbors.NeighborsAnswerer.ospfEdgeToRow;
import static org.batfish.question.neighbors.NeighborsAnswerer.ripEdgeToRow;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;
import org.batfish.common.Pair;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer2Edge;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpLink;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipNeighbor;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.IpEdge;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.collections.VerboseBgpEdge;
import org.batfish.datamodel.collections.VerboseOspfEdge;
import org.batfish.datamodel.collections.VerboseRipEdge;
import org.batfish.datamodel.eigrp.EigrpEdge;
import org.batfish.datamodel.eigrp.EigrpInterface;
import org.batfish.datamodel.isis.IsisEdge;
import org.batfish.datamodel.isis.IsisLevel;
import org.batfish.datamodel.isis.IsisNode;
import org.batfish.datamodel.ospf.OspfNeighbor;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.Row;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link NeighborsAnswerer} */
public class NeighborsAnswererTest {
  private Configuration _host1;
  private Configuration _host2;

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    _host1 = cb.setHostname("host1").build();
    _host1.setInterfaces(
        ImmutableSortedMap.of(
            "int1",
            Interface.builder()
                .setName("int1")
                .setAddress(new InterfaceAddress(new Ip("1.1.1.1"), 24))
                .build()));
    _host2 = cb.setHostname("host2").build();
    _host2.setInterfaces(
        ImmutableSortedMap.of(
            "int2",
            Interface.builder()
                .setName("int2")
                .setAddress(new InterfaceAddress(new Ip("2.2.2.2"), 24))
                .build()));
  }

  @Test
  public void testEigrpToRow() {
    EigrpEdge testEdge =
        new EigrpEdge(
            new EigrpInterface("host1", "int1", "vrf1"),
            new EigrpInterface("host2", "int2", "vrf2"));
    Row row = eigrpEdgeToRow(testEdge);

    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("host1")), Schema.NODE),
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(COL_REMOTE_NODE, equalTo(new Node("host2")), Schema.NODE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testOspfToRow() {
    OspfNeighbor ospfNeighbor1 = new OspfNeighbor(new IpLink(new Ip("1.1.1.1"), new Ip("2.2.2.2")));
    OspfNeighbor ospfNeighbor2 = new OspfNeighbor(new IpLink(new Ip("2.2.2.2"), new Ip("3.3.3.3")));
    ospfNeighbor1.setOwner(_host1);
    ospfNeighbor1.setInterface(_host1.getInterfaces().get("int1"));
    ospfNeighbor2.setOwner(_host2);
    ospfNeighbor2.setInterface(_host2.getInterfaces().get("int2"));

    VerboseOspfEdge testEdge =
        new VerboseOspfEdge(
            ospfNeighbor1,
            ospfNeighbor2,
            new IpEdge("host1", new Ip("100.101.102.103"), "host2", new Ip("100.101.102.103")));
    Row row = ospfEdgeToRow(testEdge);

    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("host1")), Schema.NODE),
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(COL_REMOTE_NODE, equalTo(new Node("host2")), Schema.NODE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testIsisToRow() {
    IsisEdge testEdge =
        new IsisEdge(
            IsisLevel.LEVEL_1, new IsisNode("host1", "int1"), new IsisNode("host2", "int2"));
    Row row = isisEdgeToRow(testEdge);

    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("host1")), Schema.NODE),
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(COL_REMOTE_NODE, equalTo(new Node("host2")), Schema.NODE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testRipToRow() {
    RipNeighbor ripNeighbor1 = new RipNeighbor(new Pair<>(new Ip("1.1.1.1"), new Ip("2.2.2.2")));
    RipNeighbor ripNeighbor2 = new RipNeighbor(new Pair<>(new Ip("2.2.2.2"), new Ip("3.3.3.3")));
    ripNeighbor1.setOwner(_host1);
    ripNeighbor1.setInterface(_host1.getInterfaces().get("int1"));
    ripNeighbor2.setOwner(_host2);
    ripNeighbor2.setInterface(_host2.getInterfaces().get("int2"));

    VerboseRipEdge testEdge =
        new VerboseRipEdge(
            ripNeighbor1,
            ripNeighbor2,
            new IpEdge("host1", new Ip("100.101.102.103"), "host2", new Ip("100.101.102.103")));
    Row row = ripEdgeToRow(testEdge);

    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("host1")), Schema.NODE),
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(COL_REMOTE_NODE, equalTo(new Node("host2")), Schema.NODE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testBgpToRow() {
    BgpPeerConfig bgpPeerConfig1 = BgpActivePeerConfig.builder().setLocalAs(1L).build();
    BgpPeerConfig bgpPeerConfig2 = BgpActivePeerConfig.builder().setLocalAs(2L).build();

    VerboseBgpEdge verboseBgpEdge =
        new VerboseBgpEdge(
            bgpPeerConfig1,
            bgpPeerConfig2,
            new BgpPeerConfigId("na", "na", new Prefix(new Ip("1.1.1.1"), 24), false),
            new BgpPeerConfigId("na", "na", new Prefix(new Ip("2.2.2.2"), 24), false),
            new IpEdge("host1", new Ip("1.1.1.1"), "host2", new Ip("2.2.2.2")));
    Row row = bgpEdgeToRow(verboseBgpEdge);
    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("host1")), Schema.NODE),
            hasColumn(COL_IP, equalTo(new Ip("1.1.1.1")), Schema.IP),
            hasColumn(COL_AS_NUMBER, equalTo("1"), Schema.STRING),
            hasColumn(COL_REMOTE_NODE, equalTo(new Node("host2")), Schema.NODE),
            hasColumn(COL_REMOTE_IP, equalTo(new Ip("2.2.2.2")), Schema.IP),
            hasColumn(COL_REMOTE_AS_NUMBER, equalTo("2"), Schema.STRING)));
  }

  @Test
  public void testLayer1ToRow() {
    Row row = layer1EdgeToRow(new Layer1Edge("host1", "int1", "host2", "int2"));
    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("host1")), Schema.NODE),
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(COL_REMOTE_NODE, equalTo(new Node("host2")), Schema.NODE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE)));
  }

  @Test
  public void testLayer2ToRow() {
    Row row = layer2EdgeToRow(new Layer2Edge("host1", "int1", 1, "host2", "int2", 2, 12));
    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("host1")), Schema.NODE),
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(COL_VLAN, equalTo("1"), Schema.STRING),
            hasColumn(COL_REMOTE_NODE, equalTo(new Node("host2")), Schema.NODE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE),
            hasColumn(COL_REMOTE_VLAN, equalTo("2"), Schema.STRING)));
  }

  @Test
  public void testLayer3ToRow() {
    Map<String, Configuration> configurationMap =
        ImmutableSortedMap.of("host1", _host1, "host2", _host2);
    Row row = layer3EdgeToRow(configurationMap, new Edge("host1", "int1", "host2", "int2"));
    assertThat(
        row,
        allOf(
            hasColumn(COL_NODE, equalTo(new Node("host1")), Schema.NODE),
            hasColumn(
                COL_INTERFACE, equalTo(new NodeInterfacePair("host1", "int1")), Schema.INTERFACE),
            hasColumn(COL_IPS, equalTo(ImmutableSet.of(new Ip("1.1.1.1"))), Schema.set(Schema.IP)),
            hasColumn(COL_REMOTE_NODE, equalTo(new Node("host2")), Schema.NODE),
            hasColumn(
                COL_REMOTE_INTERFACE,
                equalTo(new NodeInterfacePair("host2", "int2")),
                Schema.INTERFACE),
            hasColumn(
                COL_REMOTE_IPS,
                equalTo(ImmutableSet.of(new Ip("2.2.2.2"))),
                Schema.set(Schema.IP))));
  }
}

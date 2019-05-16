package org.batfish.question.bgpproperties;

import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.CLUSTER_ID;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.EXPORT_POLICY;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.IMPORT_POLICY;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.IS_PASSIVE;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.LOCAL_AS;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.LOCAL_IP;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.PEER_GROUP;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.REMOTE_AS;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.ROUTE_REFLECTOR_CLIENT;
import static org.batfish.datamodel.questions.BgpPeerPropertySpecifier.SEND_COMMUNITY;
import static org.batfish.question.bgpproperties.BgpPeerConfigurationAnswerer.COL_LOCAL_INTERFACE;
import static org.batfish.question.bgpproperties.BgpPeerConfigurationAnswerer.COL_NODE;
import static org.batfish.question.bgpproperties.BgpPeerConfigurationAnswerer.COL_REMOTE_IP;
import static org.batfish.question.bgpproperties.BgpPeerConfigurationAnswerer.COL_VRF;
import static org.batfish.question.bgpproperties.BgpPeerConfigurationAnswerer.getColumnName;
import static org.batfish.question.bgpproperties.BgpPeerConfigurationAnswerer.getRemoteIp;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.answers.SelfDescribingObject;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.BgpPeerPropertySpecifier;
import org.batfish.datamodel.table.Row;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.NameNodeSpecifier;
import org.junit.Test;

/** Test of {@link BgpPeerConfigurationAnswerer}. */
public final class BgpPeerConfigurationAnswererTest {

  private final Configuration _c;

  public BgpPeerConfigurationAnswererTest() {
    NetworkFactory nf = new NetworkFactory();
    _c = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();

    BgpActivePeerConfig activePeer =
        BgpActivePeerConfig.builder()
            .setLocalAs(100L)
            .setRemoteAsns(LongSpace.of(200L))
            .setLocalIp(Ip.parse("1.1.1.1"))
            .setPeerAddress(Ip.parse("2.2.2.2"))
            .setRouteReflectorClient(false)
            .setGroup("g1")
            .setImportPolicySources(ImmutableSortedSet.of("p1"))
            .setExportPolicySources(ImmutableSortedSet.of("p2"))
            .setSendCommunity(false)
            .build();
    BgpPassivePeerConfig passivePeer =
        BgpPassivePeerConfig.builder()
            .setLocalAs(100L)
            .setRemoteAsns(LongSpace.of(300L))
            .setLocalIp(Ip.parse("1.1.1.2"))
            .setPeerPrefix(Prefix.create(Ip.parse("3.3.3.0"), 24))
            .setRouteReflectorClient(true)
            .setClusterId(Ip.parse("5.5.5.5").asLong())
            .setGroup("g2")
            .setImportPolicySources(ImmutableSortedSet.of("p3"))
            .setExportPolicySources(ImmutableSortedSet.of("p4"))
            .setSendCommunity(false)
            .build();
    BgpUnnumberedPeerConfig unnumberedPeer =
        BgpUnnumberedPeerConfig.builder()
            .setLocalAs(100L)
            .setRemoteAsns(LongSpace.of(400L))
            .setLocalIp(Ip.parse("169.254.0.1"))
            .setPeerInterface("iface")
            .setRouteReflectorClient(true)
            .setClusterId(Ip.parse("6.6.6.6").asLong())
            .setGroup("g3")
            .setImportPolicySources(ImmutableSortedSet.of("p5"))
            .setExportPolicySources(ImmutableSortedSet.of("p6"))
            .setSendCommunity(false)
            .build();

    BgpProcess process = new BgpProcess(Ip.ZERO);
    process.setNeighbors(ImmutableSortedMap.of(Prefix.create(Ip.parse("1.1.1.0"), 24), activePeer));
    process.setPassiveNeighbors(
        ImmutableSortedMap.of(Prefix.create(Ip.parse("1.1.1.0"), 24), passivePeer));
    process.setInterfaceNeighbors(ImmutableSortedMap.of("iface", unnumberedPeer));

    Vrf vrf = new Vrf("v");
    vrf.setBgpProcess(process);

    _c.setVrfs(ImmutableMap.of("v", vrf, "emptyVrf", new Vrf("emptyVrf")));
  }

  @Test
  public void testAnswer() {
    MockSpecifierContext ctxt =
        MockSpecifierContext.builder().setConfigs(ImmutableMap.of("c", _c)).build();
    Multiset<Row> rows =
        BgpPeerConfigurationAnswerer.getAnswerRows(
            ctxt,
            new NameNodeSpecifier("c"),
            BgpPeerConfigurationAnswerer.createTableMetadata(
                    new BgpPeerConfigurationQuestion(null, null))
                .toColumnMap(),
            BgpPeerPropertySpecifier.ALL);

    Node node = new Node("c");
    Multiset<Row> expected = HashMultiset.create();
    expected.add(
        Row.builder()
            .put(COL_NODE, node)
            .put(COL_VRF, "v")
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, Ip.parse("2.2.2.2")))
            .put(getColumnName(LOCAL_AS), 100L)
            .put(COL_LOCAL_INTERFACE, null)
            .put(getColumnName(REMOTE_AS), LongSpace.of(200L).toString())
            .put(getColumnName(LOCAL_IP), Ip.parse("1.1.1.1"))
            .put(getColumnName(IS_PASSIVE), false)
            .put(getColumnName(ROUTE_REFLECTOR_CLIENT), false)
            .put(getColumnName(CLUSTER_ID), null)
            .put(getColumnName(PEER_GROUP), "g1")
            .put(getColumnName(IMPORT_POLICY), ImmutableSet.of("p1"))
            .put(getColumnName(EXPORT_POLICY), ImmutableSet.of("p2"))
            .put(getColumnName(SEND_COMMUNITY), false)
            .build());
    expected.add(
        Row.builder()
            .put(COL_NODE, node)
            .put(COL_VRF, "v")
            .put(getColumnName(LOCAL_AS), 100L)
            .put(COL_LOCAL_INTERFACE, null)
            .put(
                COL_REMOTE_IP,
                new SelfDescribingObject(Schema.PREFIX, Prefix.create(Ip.parse("3.3.3.0"), 24)))
            .put(getColumnName(REMOTE_AS), LongSpace.of(300L).toString())
            .put(getColumnName(LOCAL_IP), Ip.parse("1.1.1.2"))
            .put(getColumnName(IS_PASSIVE), true)
            .put(getColumnName(ROUTE_REFLECTOR_CLIENT), true)
            .put(getColumnName(CLUSTER_ID), Ip.parse("5.5.5.5"))
            .put(getColumnName(PEER_GROUP), "g2")
            .put(getColumnName(IMPORT_POLICY), ImmutableSet.of("p3"))
            .put(getColumnName(EXPORT_POLICY), ImmutableSet.of("p4"))
            .put(getColumnName(SEND_COMMUNITY), false)
            .build());
    expected.add(
        Row.builder()
            .put(COL_NODE, node)
            .put(COL_VRF, "v")
            .put(getColumnName(LOCAL_AS), 100L)
            .put(COL_LOCAL_INTERFACE, "iface")
            .put(COL_REMOTE_IP, null)
            .put(getColumnName(REMOTE_AS), LongSpace.of(400L).toString())
            .put(getColumnName(LOCAL_IP), null)
            .put(getColumnName(IS_PASSIVE), false)
            .put(getColumnName(ROUTE_REFLECTOR_CLIENT), true)
            .put(getColumnName(CLUSTER_ID), Ip.parse("6.6.6.6"))
            .put(getColumnName(PEER_GROUP), "g3")
            .put(getColumnName(IMPORT_POLICY), ImmutableSet.of("p5"))
            .put(getColumnName(EXPORT_POLICY), ImmutableSet.of("p6"))
            .put(getColumnName(SEND_COMMUNITY), false)
            .build());

    assertThat(rows, equalTo(expected));
  }

  @Test
  public void testFilteredAnswer() {
    MockSpecifierContext ctxt =
        MockSpecifierContext.builder().setConfigs(ImmutableMap.of("c", _c)).build();
    Multiset<Row> rows =
        BgpPeerConfigurationAnswerer.getAnswerRows(
            ctxt,
            new NameNodeSpecifier("c"),
            BgpPeerConfigurationAnswerer.createTableMetadata(
                    new BgpPeerConfigurationQuestion(
                        null, new BgpPeerPropertySpecifier("Local_IP")))
                .toColumnMap(),
            new BgpPeerPropertySpecifier("Local_IP"));

    Node node = new Node("c");
    Multiset<Row> expected = HashMultiset.create();
    expected.add(
        Row.builder()
            .put(COL_NODE, node)
            .put(COL_VRF, "v")
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, Ip.parse("2.2.2.2")))
            .put(getColumnName(LOCAL_IP), Ip.parse("1.1.1.1"))
            .build());
    expected.add(
        Row.builder()
            .put(COL_NODE, node)
            .put(COL_VRF, "v")
            .put(COL_LOCAL_INTERFACE, null)
            .put(
                COL_REMOTE_IP,
                new SelfDescribingObject(Schema.PREFIX, Prefix.create(Ip.parse("3.3.3.0"), 24)))
            .put(getColumnName(LOCAL_IP), Ip.parse("1.1.1.2"))
            .build());
    expected.add(
        Row.builder()
            .put(COL_NODE, node)
            .put(COL_VRF, "v")
            .put(COL_LOCAL_INTERFACE, "iface")
            .put(COL_REMOTE_IP, null)
            .put(getColumnName(LOCAL_IP), null)
            .build());

    assertThat(rows, equalTo(expected));
  }

  @Test
  public void getRemoteIpActivePeer() {
    Ip ip = Ip.parse("1.1.1.1");
    BgpActivePeerConfig activePeerConfig = BgpActivePeerConfig.builder().setPeerAddress(ip).build();
    assertThat(getRemoteIp(activePeerConfig), equalTo(new SelfDescribingObject(Schema.IP, ip)));
  }

  @Test
  public void getRemoteIpPassivePeer() {
    Prefix prefix = Prefix.create(Ip.parse("1.1.1.1"), 23);
    BgpPassivePeerConfig passivePeerConfig =
        BgpPassivePeerConfig.builder().setPeerPrefix(prefix).build();
    assertThat(
        getRemoteIp(passivePeerConfig), equalTo(new SelfDescribingObject(Schema.PREFIX, prefix)));
  }
}

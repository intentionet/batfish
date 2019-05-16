package org.batfish.question.bgpproperties;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.BgpProcessPropertySpecifier;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.NameNodeSpecifier;
import org.junit.Test;

public class BgpProcessConfigurationAnswererTest {

  @Test
  public void testWithNeighbors() {
    // Create process with active, passive, and unnumbered peers
    BgpProcess proc = new BgpProcess(Ip.ZERO);
    BgpActivePeerConfig.builder()
        .setBgpProcess(proc)
        .setLocalAs(100L)
        .setRemoteAsns(LongSpace.of(200L))
        .setLocalIp(Ip.parse("1.1.1.1"))
        .setPeerAddress(Ip.parse("2.2.2.2"))
        .build();
    BgpPassivePeerConfig.builder()
        .setBgpProcess(proc)
        .setLocalAs(100L)
        .setRemoteAsns(LongSpace.of(300L))
        .setLocalIp(Ip.parse("1.1.1.2"))
        .setPeerPrefix(Prefix.create(Ip.parse("3.3.3.0"), 24))
        .build();
    BgpUnnumberedPeerConfig.builder()
        .setBgpProcess(proc)
        .setLocalAs(100L)
        .setRemoteAsns(LongSpace.of(400L))
        .setLocalIp(Ip.parse("169.254.0.1"))
        .setPeerInterface("iface")
        .build();

    Vrf vrf = new Vrf("vrf");
    vrf.setBgpProcess(proc);
    Configuration conf = new Configuration("c", ConfigurationFormat.CISCO_IOS);
    conf.setVrfs(ImmutableMap.of("vrf", vrf));

    // Generate process configuration answer
    BgpProcessConfigurationQuestion question =
        new BgpProcessConfigurationQuestion(
            AllNodesNodeSpecifier.INSTANCE, BgpProcessPropertySpecifier.ALL);
    MockSpecifierContext ctxt =
        MockSpecifierContext.builder().setConfigs(ImmutableMap.of("c", conf)).build();
    Multiset<Row> rows =
        BgpProcessConfigurationAnswerer.getProperties(
            BgpProcessPropertySpecifier.ALL,
            ctxt,
            new NameNodeSpecifier("c"),
            BgpProcessConfigurationAnswerer.createTableMetadata(question).toColumnMap());

    // Create expected answer and compare
    Row expectedRow =
        Row.builder()
            .put(BgpProcessConfigurationAnswerer.COL_NODE, new Node("c"))
            .put(BgpProcessConfigurationAnswerer.COL_VRF, "vrf")
            .put(BgpProcessConfigurationAnswerer.COL_ROUTER_ID, Ip.ZERO)
            .put(BgpProcessPropertySpecifier.MULTIPATH_EQUIVALENT_AS_PATH_MATCH_MODE, null)
            .put(BgpProcessPropertySpecifier.MULTIPATH_EBGP, false)
            .put(BgpProcessPropertySpecifier.MULTIPATH_IBGP, false)
            .put(
                BgpProcessPropertySpecifier.NEIGHBORS,
                ImmutableSet.of("2.2.2.2/32", "3.3.3.0/24", "iface"))
            .put(BgpProcessPropertySpecifier.ROUTE_REFLECTOR, false)
            .put(BgpProcessPropertySpecifier.TIE_BREAKER, BgpTieBreaker.ARRIVAL_ORDER.toString())
            .build();
    assertThat(rows, equalTo(ImmutableMultiset.of(expectedRow)));
  }

  @Test
  public void getProperties() {

    BgpProcess bgp1 = new BgpProcess(Ip.parse("1.1.1.1"));
    bgp1.setMultipathEbgp(true);
    bgp1.setTieBreaker(BgpTieBreaker.ARRIVAL_ORDER);

    Vrf vrf1 = new Vrf("vrf1");
    vrf1.setBgpProcess(bgp1);

    Configuration conf1 = new Configuration("node1", ConfigurationFormat.CISCO_IOS);
    conf1.setVrfs(ImmutableMap.of("vrf1", vrf1));

    String property1 = BgpProcessPropertySpecifier.MULTIPATH_EBGP;
    String property2 = BgpProcessPropertySpecifier.TIE_BREAKER;

    BgpProcessConfigurationQuestion question =
        new BgpProcessConfigurationQuestion(
            AllNodesNodeSpecifier.INSTANCE,
            new BgpProcessPropertySpecifier(property1 + "|" + property2));

    TableMetadata metadata = BgpProcessConfigurationAnswerer.createTableMetadata(question);

    Multiset<Row> propertyRows =
        BgpProcessConfigurationAnswerer.getProperties(
            question.getProperties(),
            MockSpecifierContext.builder().setConfigs(ImmutableMap.of("node1", conf1)).build(),
            new NameNodeSpecifier("node1"),
            metadata.toColumnMap());

    // we should have exactly one row1 with two properties
    Row expectedRow =
        Row.builder()
            .put(BgpProcessConfigurationAnswerer.COL_NODE, new Node("node1"))
            .put(BgpProcessConfigurationAnswerer.COL_VRF, "vrf1")
            .put(BgpProcessConfigurationAnswerer.COL_ROUTER_ID, Ip.parse("1.1.1.1"))
            .put(property2, BgpTieBreaker.ARRIVAL_ORDER.toString())
            .put(property1, true)
            .build();

    assertThat(propertyRows, equalTo(ImmutableMultiset.of(expectedRow)));
  }
}

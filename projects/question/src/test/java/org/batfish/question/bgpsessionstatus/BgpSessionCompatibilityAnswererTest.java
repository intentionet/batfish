package org.batfish.question.bgpsessionstatus;

import static org.batfish.datamodel.matchers.TableAnswerElementMatchers.hasRows;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_LOCAL_AS;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_LOCAL_INTERFACE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_LOCAL_IP;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_NODE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_REMOTE_AS;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_REMOTE_INTERFACE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_REMOTE_IP;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_REMOTE_NODE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_SESSION_TYPE;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_VRF;
import static org.batfish.question.bgpsessionstatus.BgpSessionCompatibilityAnswerer.COLUMN_METADATA;
import static org.batfish.question.bgpsessionstatus.BgpSessionCompatibilityAnswerer.COL_CONFIGURED_STATUS;
import static org.batfish.question.bgpsessionstatus.BgpSessionCompatibilityAnswerer.createMetadata;
import static org.batfish.question.bgpsessionstatus.BgpSessionCompatibilityAnswerer.getActivePeerRow;
import static org.batfish.question.bgpsessionstatus.BgpSessionCompatibilityAnswerer.getPassivePeerRows;
import static org.batfish.question.bgpsessionstatus.BgpSessionCompatibilityAnswerer.getUnnumberedPeerRow;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Range;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.batfish.common.BatfishException;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.TopologyProvider;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpPeerConfigId.BgpPeerConfigType;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.answers.SelfDescribingObject;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.ConfiguredSessionStatus;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.SpecifierContext;
import org.junit.Test;

/** Tests of {@link BgpSessionCompatibilityAnswerer} */
public class BgpSessionCompatibilityAnswererTest {

  @Test
  public void testCreateMetadata() {
    BgpSessionCompatibilityQuestion q = new BgpSessionCompatibilityQuestion();
    TableMetadata metadata = createMetadata(q);
    assertThat(
        metadata.getTextDesc(),
        equalTo(
            "On ${Node} session ${VRF}:${Remote_IP} has configured status ${Configured_Status}."));
    assertThat(metadata.getColumnMetadata(), equalTo(COLUMN_METADATA));

    q.setDisplayHints(new DisplayHints(null, null, "display hints"));
    metadata = createMetadata(q);
    assertThat(metadata.getTextDesc(), equalTo("display hints"));
    assertThat(metadata.getColumnMetadata(), equalTo(COLUMN_METADATA));
  }

  @Test
  public void testGetActivePeerRowNoLocalIp() {
    Ip remoteIp = Ip.parse("2.2.2.2");

    // Active peer missing local IP for which we're generating a row
    BgpPeerConfigId peerId =
        new BgpPeerConfigId("c1", "vrf1", Prefix.create(remoteIp, Prefix.MAX_PREFIX_LENGTH), false);
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(remoteIp)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    Row row = getActivePeerRow(peerId, peer, ImmutableMap.of(), null);
    Row expected =
        Row.builder()
            .put(COL_CONFIGURED_STATUS, ConfiguredSessionStatus.NO_LOCAL_IP)
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_LOCAL_IP, null)
            .put(COL_LOCAL_AS, 1L)
            .put(COL_NODE, new Node("c1"))
            .put(COL_REMOTE_AS, LongSpace.of(2L).toString())
            .put(COL_REMOTE_NODE, null)
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, remoteIp))
            .put(COL_SESSION_TYPE, SessionType.EBGP_SINGLEHOP)
            .put(COL_VRF, "vrf1")
            .build();
    assertThat(row, equalTo(expected));
  }

  @Test
  public void testGetActivePeerRowUniqueMatch() {
    Ip localIp = Ip.parse("1.1.1.1");
    Ip remoteIp = Ip.parse("2.2.2.2");

    // Active peer for which we're generating a row
    BgpPeerConfigId peerId =
        new BgpPeerConfigId("c1", "vrf1", Prefix.create(remoteIp, Prefix.MAX_PREFIX_LENGTH), false);
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setLocalIp(localIp)
            .setPeerAddress(remoteIp)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    // Remote active peer
    BgpPeerConfigId remotePeerId =
        new BgpPeerConfigId("c2", "vrf2", Prefix.create(localIp, Prefix.MAX_PREFIX_LENGTH), false);
    BgpActivePeerConfig remotePeer =
        BgpActivePeerConfig.builder()
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    Map<Ip, Map<String, Set<String>>> ipVrfOwners =
        ImmutableMap.of(
            localIp,
            ImmutableMap.of("c1", ImmutableSet.of("vrf1")),
            remoteIp,
            ImmutableMap.of("c2", ImmutableSet.of("vrf2")));

    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    bgpTopology.putEdgeValue(
        peerId, remotePeerId, BgpSessionProperties.from(peer, remotePeer, false));
    bgpTopology.putEdgeValue(
        remotePeerId, peerId, BgpSessionProperties.from(peer, remotePeer, true));

    Row row = getActivePeerRow(peerId, peer, ipVrfOwners, bgpTopology);
    Row expected =
        Row.builder()
            .put(COL_CONFIGURED_STATUS, ConfiguredSessionStatus.UNIQUE_MATCH)
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_LOCAL_IP, localIp)
            .put(COL_LOCAL_AS, 1L)
            .put(COL_NODE, new Node("c1"))
            .put(COL_REMOTE_AS, LongSpace.of(2L).toString())
            .put(COL_REMOTE_NODE, new Node("c2"))
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, remoteIp))
            .put(COL_SESSION_TYPE, SessionType.EBGP_SINGLEHOP)
            .put(COL_VRF, "vrf1")
            .build();
    assertThat(row, equalTo(expected));
  }

  @Test
  public void testGetDynamicPeerRowNoRemoteAs() {
    Prefix remotePrefix = Prefix.parse("2.2.2.0/24");

    // Dynamic peer missing remote AS for which we're generating a row
    BgpPeerConfigId peerId = new BgpPeerConfigId("c1", "vrf1", remotePrefix, true);
    BgpPassivePeerConfig peer =
        BgpPassivePeerConfig.builder()
            .setLocalIp(Ip.AUTO)
            .setPeerPrefix(remotePrefix)
            .setLocalAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    List<Row> rows = getPassivePeerRows(peerId, peer, null, null);
    Row expected =
        Row.builder()
            .put(COL_CONFIGURED_STATUS, ConfiguredSessionStatus.NO_REMOTE_AS)
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_LOCAL_IP, Ip.AUTO)
            .put(COL_LOCAL_AS, 1L)
            .put(COL_NODE, new Node("c1"))
            .put(COL_REMOTE_AS, "")
            .put(COL_REMOTE_NODE, null)
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.PREFIX, remotePrefix))
            .put(COL_SESSION_TYPE, SessionType.UNSET)
            .put(COL_VRF, "vrf1")
            .build();
    assertThat(rows, contains(expected));
  }

  @Test
  public void testGetDynamicPeerRowNoCompatiblePeers() {
    Prefix remotePrefix = Prefix.parse("2.2.2.0/24");
    LongSpace remoteAsns = LongSpace.of(Range.closed(2L, 3L));

    // Dynamic peer correctly configured, but no adjacent nodes in BGP topology
    BgpPeerConfigId peerId = new BgpPeerConfigId("c1", "vrf1", remotePrefix, true);
    BgpPassivePeerConfig peer =
        BgpPassivePeerConfig.builder()
            .setLocalIp(Ip.AUTO)
            .setPeerPrefix(remotePrefix)
            .setLocalAs(1L)
            .setRemoteAsns(remoteAsns)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    bgpTopology.addNode(peerId);

    List<Row> rows = getPassivePeerRows(peerId, peer, null, bgpTopology);
    Row expected =
        Row.builder()
            .put(COL_CONFIGURED_STATUS, ConfiguredSessionStatus.NO_MATCH_FOUND)
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_LOCAL_IP, Ip.AUTO)
            .put(COL_LOCAL_AS, 1L)
            .put(COL_NODE, new Node("c1"))
            .put(COL_REMOTE_AS, remoteAsns.toString())
            .put(COL_REMOTE_NODE, null)
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.PREFIX, remotePrefix))
            .put(COL_SESSION_TYPE, SessionType.UNSET)
            .put(COL_VRF, "vrf1")
            .build();
    assertThat(rows, contains(expected));
  }

  @Test
  public void testGetDynamicPeerRowTwoCompatiblePeers() {
    Ip localIp = Ip.parse("1.1.1.1");
    Prefix localAddress = Prefix.create(localIp, Prefix.MAX_PREFIX_LENGTH);
    Prefix remotePrefix = Prefix.parse("2.2.2.0/24");
    LongSpace remoteAsns = LongSpace.of(Range.closed(2L, 3L));

    // Dynamic peer correctly configured, with two adjacent nodes in BGP topology
    BgpPeerConfigId peerId = new BgpPeerConfigId("c1", "vrf1", remotePrefix, true);
    BgpPassivePeerConfig peer =
        BgpPassivePeerConfig.builder()
            .setLocalIp(Ip.AUTO)
            .setPeerPrefix(remotePrefix)
            .setLocalAs(1L)
            .setRemoteAsns(remoteAsns)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    // Remote peers
    Ip remote1Ip = Ip.parse("2.2.2.1");
    BgpPeerConfigId remote1Id = new BgpPeerConfigId("c2", "vrf2", localAddress, false);
    BgpActivePeerConfig remote1 =
        BgpActivePeerConfig.builder()
            .setLocalIp(remote1Ip)
            .setPeerAddress(localIp)
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    Ip remote2Ip = Ip.parse("2.2.2.2");
    BgpPeerConfigId remote2Id = new BgpPeerConfigId("c3", "vrf3", localAddress, false);
    BgpActivePeerConfig remote2 =
        BgpActivePeerConfig.builder()
            .setLocalIp(remote2Ip)
            .setPeerAddress(localIp)
            .setLocalAs(3L)
            .setRemoteAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    bgpTopology.putEdgeValue(peerId, remote1Id, BgpSessionProperties.from(remote1, peer, true));
    bgpTopology.putEdgeValue(remote1Id, peerId, BgpSessionProperties.from(remote1, peer, false));
    bgpTopology.putEdgeValue(peerId, remote2Id, BgpSessionProperties.from(remote2, peer, true));
    bgpTopology.putEdgeValue(remote2Id, peerId, BgpSessionProperties.from(remote2, peer, false));

    // Build configs for remote peers because answerer needs to look up peers from peer IDs
    NetworkConfigurations nc =
        NetworkConfigurations.of(
            createConfigurations(
                ImmutableList.of(remote1Id, remote2Id), ImmutableList.of(remote1, remote2)));

    List<Row> rows = getPassivePeerRows(peerId, peer, nc, bgpTopology);

    Row.RowBuilder expectedRowBuilder =
        Row.builder()
            // Columns that should be the same in both rows
            .put(COL_LOCAL_AS, 1L)
            .put(COL_LOCAL_IP, localIp)
            .put(COL_NODE, new Node("c1"))
            .put(COL_SESSION_TYPE, SessionType.EBGP_SINGLEHOP)
            .put(COL_VRF, "vrf1")
            .put(COL_CONFIGURED_STATUS, ConfiguredSessionStatus.DYNAMIC_MATCH)
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_REMOTE_INTERFACE, null);
    Row expected1 =
        expectedRowBuilder
            .put(COL_REMOTE_AS, "2")
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, remote1Ip))
            .put(COL_REMOTE_NODE, new Node("c2"))
            .build();
    Row expected2 =
        expectedRowBuilder
            .put(COL_REMOTE_AS, "3")
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, remote2Ip))
            .put(COL_REMOTE_NODE, new Node("c3"))
            .build();
    assertThat(rows, containsInAnyOrder(expected1, expected2));
  }

  @Test
  public void testGetUnnumberedPeerRowNoRemoteAs() {
    // Unnumbered peer missing remote AS for which we're generating a row
    BgpPeerConfigId peerId = new BgpPeerConfigId("c1", "vrf1", "iface");
    BgpUnnumberedPeerConfig peer =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface("iface")
            .setLocalAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    Row row = getUnnumberedPeerRow(peerId, peer, null);
    Row expected =
        Row.builder()
            .put(COL_CONFIGURED_STATUS, ConfiguredSessionStatus.NO_REMOTE_AS)
            .put(COL_LOCAL_INTERFACE, NodeInterfacePair.of("c1", "iface"))
            .put(COL_LOCAL_IP, null)
            .put(COL_LOCAL_AS, 1L)
            .put(COL_NODE, new Node("c1"))
            .put(COL_REMOTE_AS, "")
            .put(COL_REMOTE_NODE, null)
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_REMOTE_IP, null)
            .put(COL_SESSION_TYPE, SessionType.UNSET)
            .put(COL_VRF, "vrf1")
            .build();
    assertThat(row, equalTo(expected));
  }

  @Test
  public void testGetUnnumberedPeerRowUniqueMatch() {
    Ip unnumIp = Ip.parse("169.254.0.1");

    // Unnumbered peer for which we're generating a row
    BgpPeerConfigId peerId = new BgpPeerConfigId("c1", "vrf1", "iface");
    BgpUnnumberedPeerConfig peer =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface("iface")
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setLocalIp(unnumIp)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    // Remote unnumbered peer
    BgpPeerConfigId remoteId = new BgpPeerConfigId("c2", "vrf2", "iface2");
    BgpUnnumberedPeerConfig remote =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface("iface2")
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setLocalIp(unnumIp)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    bgpTopology.putEdgeValue(peerId, remoteId, BgpSessionProperties.from(remote, peer, false));
    bgpTopology.putEdgeValue(remoteId, peerId, BgpSessionProperties.from(remote, peer, true));

    Row row = getUnnumberedPeerRow(peerId, peer, bgpTopology);
    Row expected =
        Row.builder()
            .put(COL_CONFIGURED_STATUS, ConfiguredSessionStatus.UNIQUE_MATCH)
            .put(COL_LOCAL_INTERFACE, NodeInterfacePair.of("c1", "iface"))
            .put(COL_LOCAL_IP, null)
            .put(COL_LOCAL_AS, 1L)
            .put(COL_NODE, new Node("c1"))
            .put(COL_REMOTE_AS, "2")
            .put(COL_REMOTE_NODE, new Node("c2"))
            .put(COL_REMOTE_INTERFACE, NodeInterfacePair.of("c2", "iface2"))
            .put(COL_REMOTE_IP, null)
            .put(COL_SESSION_TYPE, SessionType.EBGP_UNNUMBERED)
            .put(COL_VRF, "vrf1")
            .build();
    assertThat(row, equalTo(expected));
  }

  @Test
  public void testEndToEndAnswerAndFilters() {
    // Setup: Two correctly configured active peers
    Ip ip1 = Ip.parse("1.1.1.1");
    Ip ip2 = Ip.parse("2.2.2.2");

    BgpPeerConfigId id1 =
        new BgpPeerConfigId("c1", "vrf1", Prefix.create(ip2, Prefix.MAX_PREFIX_LENGTH), false);
    BgpActivePeerConfig peer1 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip1)
            .setPeerAddress(ip2)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpPeerConfigId id2 =
        new BgpPeerConfigId("c2", "vrf2", Prefix.create(ip1, Prefix.MAX_PREFIX_LENGTH), false);
    BgpActivePeerConfig peer2 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip2)
            .setPeerAddress(ip1)
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    IBatfish batfish =
        new MockBatfish(
            createConfigurations(ImmutableList.of(id1, id2), ImmutableList.of(peer1, peer2)));

    // Rows that will appear if no filters are applied
    Row.RowBuilder expectedRowBuilder =
        Row.builder()
            // Columns that will be the same in both rows
            .put(COL_CONFIGURED_STATUS, ConfiguredSessionStatus.UNIQUE_MATCH)
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_SESSION_TYPE, SessionType.EBGP_SINGLEHOP);
    Row row1To2 =
        expectedRowBuilder
            .put(COL_LOCAL_IP, ip1)
            .put(COL_LOCAL_AS, 1L)
            .put(COL_NODE, new Node("c1"))
            .put(COL_REMOTE_AS, "2")
            .put(COL_REMOTE_NODE, new Node("c2"))
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, ip2))
            .put(COL_VRF, "vrf1")
            .build();
    Row row2To1 =
        expectedRowBuilder
            .put(COL_LOCAL_IP, ip2)
            .put(COL_LOCAL_AS, 2L)
            .put(COL_NODE, new Node("c2"))
            .put(COL_REMOTE_AS, "1")
            .put(COL_REMOTE_NODE, new Node("c1"))
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, ip1))
            .put(COL_VRF, "vrf2")
            .build();

    // Full answer
    BgpSessionCompatibilityAnswerer answerer =
        new BgpSessionCompatibilityAnswerer(new BgpSessionCompatibilityQuestion(), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(), hasRows(containsInAnyOrder(row1To2, row2To1)));

    // Limit by node
    answerer =
        new BgpSessionCompatibilityAnswerer(
            new BgpSessionCompatibilityQuestion("c1", null, null, null), batfish);
    assertThat((TableAnswerElement) answerer.answer(), hasRows(contains(row1To2)));

    // Limit by remote node
    answerer =
        new BgpSessionCompatibilityAnswerer(
            new BgpSessionCompatibilityQuestion(null, "c1", null, null), batfish);
    assertThat((TableAnswerElement) answerer.answer(), hasRows(contains(row2To1)));

    // Limit by status. Since both rows have the same status, test twice with different statuses
    answerer =
        new BgpSessionCompatibilityAnswerer(
            new BgpSessionCompatibilityQuestion(null, null, "UNIQUE_MATCH", null), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(), hasRows(containsInAnyOrder(row1To2, row2To1)));
    answerer =
        new BgpSessionCompatibilityAnswerer(
            new BgpSessionCompatibilityQuestion(null, null, "NO_LOCAL_IP", null), batfish);
    assertThat((TableAnswerElement) answerer.answer(), hasRows(emptyIterable()));

    // Limit by type. Since both rows have the same type, test twice with different types
    answerer =
        new BgpSessionCompatibilityAnswerer(
            new BgpSessionCompatibilityQuestion(null, null, null, "EBGP_SINGLEHOP"), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(), hasRows(containsInAnyOrder(row1To2, row2To1)));
    answerer =
        new BgpSessionCompatibilityAnswerer(
            new BgpSessionCompatibilityQuestion(null, null, null, "IBGP"), batfish);
    assertThat((TableAnswerElement) answerer.answer(), hasRows(emptyIterable()));
  }

  /**
   * Given equally sized lists of {@link BgpPeerConfigId}s and {@link BgpPeerConfig}s, creates one
   * {@link Configuration} per ID/peer pair and returns the resulting configurations.
   */
  static SortedMap<String, Configuration> createConfigurations(
      List<BgpPeerConfigId> ids, List<BgpPeerConfig> peers) {
    assert ids.size() == peers.size();
    SortedMap<String, Configuration> configs = new TreeMap<>();
    NetworkFactory nf = new NetworkFactory();
    for (int i = 0; i < ids.size(); i++) {
      BgpPeerConfigId id = ids.get(i);
      BgpPeerConfig peer = peers.get(i);

      // Create a configuration with a BgpProcess
      Configuration c =
          nf.configurationBuilder()
              .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
              .setHostname(id.getHostname())
              .build();
      Vrf vrf = nf.vrfBuilder().setOwner(c).setName(id.getVrfName()).build();
      BgpProcess bgpProc =
          nf.bgpProcessBuilder()
              .setVrf(vrf)
              .setRouterId(Ip.ZERO)
              .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
              .build();
      configs.put(c.getHostname(), c);

      // Add interface to make IpOwners accurate
      if (peer.getLocalIp() != null && peer.getLocalIp() != Ip.AUTO) {
        nf.interfaceBuilder()
            .setOwner(c)
            .setVrf(vrf)
            .setAddress(ConcreteInterfaceAddress.create(peer.getLocalIp(), 30))
            .build();
      }

      // Add peer in the appropriate map in the BgpProcess
      if (id.getType() == BgpPeerConfigType.ACTIVE) {
        bgpProc.setNeighbors(
            ImmutableSortedMap.of(id.getRemotePeerPrefix(), (BgpActivePeerConfig) peer));
      } else if (id.getType() == BgpPeerConfigType.DYNAMIC) {
        bgpProc.setPassiveNeighbors(
            ImmutableSortedMap.of(id.getRemotePeerPrefix(), (BgpPassivePeerConfig) peer));
      } else if (id.getType() == BgpPeerConfigType.UNNUMBERED) {
        bgpProc.setInterfaceNeighbors(
            ImmutableSortedMap.of(id.getPeerInterface(), (BgpUnnumberedPeerConfig) peer));
      } else {
        throw new BatfishException(String.format("Unhandled peer type %s", id.getType()));
      }
    }
    return configs;
  }

  static class MockBatfish extends IBatfishTestAdapter {

    private final SortedMap<String, Configuration> _configs;

    public MockBatfish(SortedMap<String, Configuration> configs) {
      _configs = configs;
    }

    @Override
    public SortedMap<String, Configuration> loadConfigurations() {
      return _configs;
    }

    @Override
    public SpecifierContext specifierContext() {
      return MockSpecifierContext.builder().setConfigs(_configs).build();
    }

    @Override
    public TopologyProvider getTopologyProvider() {
      return new MockLayer2TopologyProvider(this);
    }

    /** Mock topology provider that provides empty initial layer 2 topology */
    static class MockLayer2TopologyProvider extends TopologyProviderTestAdapter {
      MockLayer2TopologyProvider(IBatfish bf) {
        super(bf);
      }

      @Override
      public Optional<Layer2Topology> getInitialLayer2Topology(NetworkSnapshot networkSnapshot) {
        return Optional.empty();
      }
    }
  }
}

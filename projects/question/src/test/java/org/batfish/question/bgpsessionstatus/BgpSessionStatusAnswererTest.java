package org.batfish.question.bgpsessionstatus;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.matchers.TableAnswerElementMatchers.hasRows;
import static org.batfish.question.bgpsessionstatus.BgpSessionAnswererUtils.COL_ADDRESS_FAMILIES;
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
import static org.batfish.question.bgpsessionstatus.BgpSessionCompatibilityAnswererTest.createConfigurations;
import static org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.COLUMN_METADATA;
import static org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.COL_ESTABLISHED_STATUS;
import static org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.createMetadata;
import static org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.getActivePeerRow;
import static org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.getPassivePeerRows;
import static org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.getUnnumberedPeerRow;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.common.topology.GlobalBroadcastNoPointToPoint;
import org.batfish.common.topology.L3Adjacencies;
import org.batfish.common.topology.TopologyProvider;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.answers.SelfDescribingObject;
import org.batfish.datamodel.bgp.AddressFamily.Type;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.BgpSessionStatus;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.SpecifierContext;
import org.junit.Test;

/** Tests of {@link BgpSessionStatusAnswerer} */
public class BgpSessionStatusAnswererTest {

  @Test
  public void testCreateMetadata() {
    BgpSessionStatusQuestion q = new BgpSessionStatusQuestion();
    TableMetadata metadata = createMetadata(q);
    assertThat(
        metadata.getTextDesc(),
        equalTo("On ${Node} session ${VRF}:${Remote_IP} has status ${Established_Status}."));
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
    BgpPeerConfigId peerId = new BgpPeerConfigId("c1", "vrf1", remoteIp.toPrefix(), false);
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(remoteIp)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    // Topology with no other peers
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> topology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    topology.addNode(peerId);

    Row row = getActivePeerRow(peerId, peer, ImmutableMap.of(), topology, topology);
    Row expected =
        Row.builder()
            .put(COL_ESTABLISHED_STATUS, BgpSessionStatus.NOT_COMPATIBLE)
            .put(COL_ADDRESS_FAMILIES, ImmutableSet.of())
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
  public void testGetActivePeerRowCompatible() {
    Ip localIp = Ip.parse("1.1.1.1");
    Ip remoteIp = Ip.parse("2.2.2.2");

    // Active peer for which we're generating a row
    BgpPeerConfigId peerId = new BgpPeerConfigId("c1", "vrf1", remoteIp.toPrefix(), false);
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setLocalIp(localIp)
            .setPeerAddress(remoteIp)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    // Remote active peer
    BgpPeerConfigId remotePeerId = new BgpPeerConfigId("c2", "vrf2", localIp.toPrefix(), false);
    BgpActivePeerConfig remotePeer =
        BgpActivePeerConfig.builder()
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    Map<Ip, Map<String, Set<String>>> ipVrfOwners =
        ImmutableMap.of(
            localIp,
            ImmutableMap.of("c1", ImmutableSet.of("vrf1")),
            remoteIp,
            ImmutableMap.of("c2", ImmutableSet.of("vrf2")));

    // Topology containing both peers, but no link between them
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> unlinkedTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    unlinkedTopology.addNode(peerId);
    unlinkedTopology.addNode(remotePeerId);

    // Topology with link between the two peers
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> linkedTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    linkedTopology.putEdgeValue(
        peerId, remotePeerId, BgpSessionProperties.from(peer, remotePeer, false));
    linkedTopology.putEdgeValue(
        remotePeerId, peerId, BgpSessionProperties.from(peer, remotePeer, true));

    // Case 1: Peers are compatible, but can't reach each other (established topology is empty)
    Row row = getActivePeerRow(peerId, peer, ipVrfOwners, linkedTopology, unlinkedTopology);
    Row.RowBuilder expected =
        Row.builder()
            .put(COL_ESTABLISHED_STATUS, BgpSessionStatus.NOT_ESTABLISHED)
            .put(COL_ADDRESS_FAMILIES, ImmutableSet.of(Type.IPV4_UNICAST))
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_LOCAL_IP, localIp)
            .put(COL_LOCAL_AS, 1L)
            .put(COL_NODE, new Node("c1"))
            .put(COL_REMOTE_AS, LongSpace.of(2L).toString())
            .put(COL_REMOTE_NODE, new Node("c2"))
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, remoteIp))
            .put(COL_SESSION_TYPE, SessionType.EBGP_SINGLEHOP)
            .put(COL_VRF, "vrf1");
    assertThat(row, equalTo(expected.build()));

    // Case 2: Peers are NOT both compatible, but session comes up (could happen if one peer is
    // missing local IP or has multiple compatible remotes)
    row = getActivePeerRow(peerId, peer, ipVrfOwners, unlinkedTopology, linkedTopology);
    expected.put(COL_ESTABLISHED_STATUS, BgpSessionStatus.ESTABLISHED);
    assertThat(row, equalTo(expected.build()));

    // Case 3: Peers are compatible and able to reach each other
    row = getActivePeerRow(peerId, peer, ipVrfOwners, linkedTopology, linkedTopology);
    assertThat(row, equalTo(expected.build()));
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

    List<Row> rows = getPassivePeerRows(peerId, peer, null, null, null);
    Row expected =
        Row.builder()
            .put(COL_ESTABLISHED_STATUS, BgpSessionStatus.NOT_COMPATIBLE)
            .put(COL_ADDRESS_FAMILIES, ImmutableSet.of())
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

    List<Row> rows = getPassivePeerRows(peerId, peer, null, bgpTopology, bgpTopology);
    Row expected =
        Row.builder()
            .put(COL_ESTABLISHED_STATUS, BgpSessionStatus.NOT_ESTABLISHED)
            .put(COL_ADDRESS_FAMILIES, ImmutableSet.of())
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
  public void testGetUnnumberedPeerRowNoRemoteAs() {
    // Unnumbered peer missing remote AS for which we're generating a row
    BgpPeerConfigId peerId = new BgpPeerConfigId("c1", "vrf1", "iface");
    BgpUnnumberedPeerConfig peer =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface("iface")
            .setLocalAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    // Topology with no other peers
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> topology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    topology.addNode(peerId);

    Row row = getUnnumberedPeerRow(peerId, peer, topology, topology);
    Row expected =
        Row.builder()
            .put(COL_ESTABLISHED_STATUS, BgpSessionStatus.NOT_COMPATIBLE)
            .put(COL_ADDRESS_FAMILIES, ImmutableSet.of())
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
  public void testGetUnnumberedPeerRowEstablished() {
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
    bgpTopology.putEdgeValue(peerId, remoteId, BgpSessionProperties.from(peer, remote, false));
    bgpTopology.putEdgeValue(remoteId, peerId, BgpSessionProperties.from(peer, remote, true));
    Row row = getUnnumberedPeerRow(peerId, peer, bgpTopology, bgpTopology);
    Row expected =
        Row.builder()
            .put(COL_ESTABLISHED_STATUS, BgpSessionStatus.ESTABLISHED)
            .put(COL_ADDRESS_FAMILIES, ImmutableSet.of(Type.IPV4_UNICAST))
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
  public void testGetDynamicPeerRowTwoCompatiblePeers() {
    Ip localIp = Ip.parse("1.1.1.1");
    Prefix localAddress = localIp.toPrefix();
    Prefix remotePrefix = Prefix.parse("2.2.2.0/24");
    LongSpace remoteAsns = LongSpace.of(Range.closed(2L, 3L));

    // Dynamic peer with two compatible remotes: one can reach the dynamic peer, one can't.
    // getPassivePeerRows() should include rows for both, one ESTABLISHED and one NOT_ESTABLISHED.
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

    // Configured topology: both remote peers have edges with dynamic peer
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    configuredTopology.putEdgeValue(
        peerId, remote1Id, BgpSessionProperties.from(remote1, peer, true));
    configuredTopology.putEdgeValue(
        remote1Id, peerId, BgpSessionProperties.from(remote1, peer, false));
    configuredTopology.putEdgeValue(
        peerId, remote2Id, BgpSessionProperties.from(remote2, peer, true));
    configuredTopology.putEdgeValue(
        remote2Id, peerId, BgpSessionProperties.from(remote2, peer, false));

    // Established topology: Only remote1 has edge with dynamic peer
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> establishedTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    establishedTopology.putEdgeValue(
        peerId, remote1Id, BgpSessionProperties.from(remote1, peer, true));
    establishedTopology.putEdgeValue(
        remote1Id, peerId, BgpSessionProperties.from(remote1, peer, false));

    // Build configs for remote peers because answerer needs to look up remote peers from peer IDs
    NetworkConfigurations nc =
        NetworkConfigurations.of(
            createConfigurations(
                ImmutableList.of(remote1Id, remote2Id), ImmutableList.of(remote1, remote2)));

    List<Row> rows = getPassivePeerRows(peerId, peer, nc, configuredTopology, establishedTopology);

    Row.RowBuilder expectedRowBuilder =
        Row.builder()
            // Columns that should be the same in both rows
            .put(COL_LOCAL_AS, 1L)
            .put(COL_LOCAL_IP, localIp)
            .put(COL_NODE, new Node("c1"))
            .put(COL_SESSION_TYPE, SessionType.EBGP_SINGLEHOP)
            .put(COL_VRF, "vrf1")
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_ADDRESS_FAMILIES, ImmutableSet.of(Type.IPV4_UNICAST));
    Row expected1 =
        expectedRowBuilder
            .put(COL_ESTABLISHED_STATUS, BgpSessionStatus.ESTABLISHED)
            .put(COL_REMOTE_AS, "2")
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, remote1Ip))
            .put(COL_REMOTE_NODE, new Node("c2"))
            .build();
    Row expected2 =
        expectedRowBuilder
            .put(COL_ESTABLISHED_STATUS, BgpSessionStatus.NOT_ESTABLISHED)
            .put(COL_REMOTE_AS, "3")
            .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, remote2Ip))
            .put(COL_REMOTE_NODE, new Node("c3"))
            .build();
    assertThat(rows, containsInAnyOrder(expected1, expected2));
  }

  @Test
  public void testEstablishedWithMultipleRemotes() {
    /*
    Setup: Peer X will have multiple compatible remotes, but only one reachable remote. Should see
    the one reachable remote listed as the remote node.

    This test leaves the reachable remote peer out of the configured topology to be sure that the
    logic is finding the remote peer in the established topology rather than using the first
    adjacent node in the configured topology.
     */
    Ip localIp = Ip.parse("1.1.1.1");
    Ip remoteIp = Ip.parse("2.2.2.2");
    BgpPeerConfigId peerXId =
        new BgpPeerConfigId("c", DEFAULT_VRF_NAME, remoteIp.toPrefix(), false);
    BgpActivePeerConfig peerX =
        BgpActivePeerConfig.builder()
            .setLocalIp(localIp)
            .setPeerAddress(remoteIp)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    // Two compatible (but unreachable) remote peers
    BgpPeerConfigId compat1Id =
        new BgpPeerConfigId("c2", DEFAULT_VRF_NAME, remoteIp.toPrefix(), false);
    BgpPeerConfigId compat2Id =
        new BgpPeerConfigId("c3", DEFAULT_VRF_NAME, remoteIp.toPrefix(), false);

    // One compatible AND reachable remote peer
    BgpPeerConfigId establishedId =
        new BgpPeerConfigId("c4", DEFAULT_VRF_NAME, remoteIp.toPrefix(), false);

    // Recycle the same compatible remote config for all the remotes
    BgpActivePeerConfig remotePeer =
        BgpActivePeerConfig.builder()
            .setLocalIp(remoteIp)
            .setPeerAddress(localIp)
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    // Configured topology: Peer X has edges with both compatible remotes
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> configuredTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    configuredTopology.putEdgeValue(
        peerXId, compat1Id, BgpSessionProperties.from(peerX, remotePeer, false));
    configuredTopology.putEdgeValue(
        compat1Id, peerXId, BgpSessionProperties.from(peerX, remotePeer, true));
    configuredTopology.putEdgeValue(
        peerXId, compat2Id, BgpSessionProperties.from(peerX, remotePeer, false));
    configuredTopology.putEdgeValue(
        compat2Id, peerXId, BgpSessionProperties.from(peerX, remotePeer, true));

    // Established topology: Peer X has edge with established remote
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> establishedTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    establishedTopology.putEdgeValue(
        peerXId, establishedId, BgpSessionProperties.from(peerX, remotePeer, false));
    establishedTopology.putEdgeValue(
        establishedId, peerXId, BgpSessionProperties.from(peerX, remotePeer, true));

    Map<Ip, Map<String, Set<String>>> ipVrfOwners =
        ImmutableMap.of(
            localIp,
            ImmutableMap.of("c", ImmutableSet.of(DEFAULT_VRF_NAME)),
            remoteIp,
            ImmutableMap.of(
                "c2",
                ImmutableSet.of(DEFAULT_VRF_NAME),
                "c3",
                ImmutableSet.of(DEFAULT_VRF_NAME),
                "c4",
                ImmutableSet.of(DEFAULT_VRF_NAME)));

    Row row =
        getActivePeerRow(peerXId, peerX, ipVrfOwners, configuredTopology, establishedTopology);
    assertThat(row, hasColumn(COL_REMOTE_NODE, new Node("c4"), Schema.NODE));
  }

  @Test
  public void testEndToEndAnswerAndFilters() {
    // Setup: Two correctly configured active peers
    Ip ip1 = Ip.parse("1.1.1.1");
    Ip ip2 = Ip.parse("2.2.2.2");

    BgpPeerConfigId id1 = new BgpPeerConfigId("c1", "vrf1", ip2.toPrefix(), false);
    BgpActivePeerConfig peer1 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip1)
            .setPeerAddress(ip2)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpPeerConfigId id2 = new BgpPeerConfigId("c2", "vrf2", ip1.toPrefix(), false);
    BgpActivePeerConfig peer2 =
        BgpActivePeerConfig.builder()
            .setLocalIp(ip2)
            .setPeerAddress(ip1)
            .setLocalAs(2L)
            .setRemoteAs(1L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    bgpTopology.putEdgeValue(id1, id2, BgpSessionProperties.from(peer1, peer2, false));
    bgpTopology.putEdgeValue(id2, id1, BgpSessionProperties.from(peer1, peer2, true));

    IBatfish batfish =
        new MockBatfish(
            createConfigurations(ImmutableList.of(id1, id2), ImmutableList.of(peer1, peer2)),
            new BgpTopology(bgpTopology));

    // Rows that will appear if no filters are applied
    Row.RowBuilder expectedRowBuilder =
        Row.builder()
            // Columns that will be the same in both rows
            .put(COL_ESTABLISHED_STATUS, BgpSessionStatus.ESTABLISHED)
            .put(COL_LOCAL_INTERFACE, null)
            .put(COL_REMOTE_INTERFACE, null)
            .put(COL_ADDRESS_FAMILIES, ImmutableSet.of(Type.IPV4_UNICAST))
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
    BgpSessionStatusAnswerer answerer =
        new BgpSessionStatusAnswerer(new BgpSessionStatusQuestion(), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(batfish.getSnapshot()),
        hasRows(containsInAnyOrder(row1To2, row2To1)));

    // Limit by node
    answerer =
        new BgpSessionStatusAnswerer(new BgpSessionStatusQuestion("c1", null, null, null), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(batfish.getSnapshot()), hasRows(contains(row1To2)));

    // Limit by remote node
    answerer =
        new BgpSessionStatusAnswerer(new BgpSessionStatusQuestion(null, "c1", null, null), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(batfish.getSnapshot()), hasRows(contains(row2To1)));

    // Limit by status. Since both rows have the same status, test twice with different statuses
    answerer =
        new BgpSessionStatusAnswerer(
            new BgpSessionStatusQuestion(null, null, "ESTABLISHED", null), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(batfish.getSnapshot()),
        hasRows(containsInAnyOrder(row1To2, row2To1)));
    answerer =
        new BgpSessionStatusAnswerer(
            new BgpSessionStatusQuestion(null, null, "NOT_ESTABLISHED", null), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(batfish.getSnapshot()), hasRows(emptyIterable()));

    // Limit by type. Since both rows have the same type, test twice with different types
    answerer =
        new BgpSessionStatusAnswerer(
            new BgpSessionStatusQuestion(null, null, null, "EBGP_SINGLEHOP"), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(batfish.getSnapshot()),
        hasRows(containsInAnyOrder(row1To2, row2To1)));
    answerer =
        new BgpSessionStatusAnswerer(
            new BgpSessionStatusQuestion(null, null, null, "IBGP"), batfish);
    assertThat(
        (TableAnswerElement) answerer.answer(batfish.getSnapshot()), hasRows(emptyIterable()));
  }

  static class MockBatfish extends IBatfishTestAdapter {

    private final SortedMap<String, Configuration> _configs;
    private final BgpTopology _bgpTopology;

    MockBatfish(SortedMap<String, Configuration> configs, BgpTopology bgpTopology) {
      _configs = configs;
      _bgpTopology = bgpTopology;
    }

    @Override
    public SortedMap<String, Configuration> loadConfigurations(NetworkSnapshot snapshot) {
      return _configs;
    }

    @Override
    public SpecifierContext specifierContext(NetworkSnapshot snapshot) {
      assertThat(snapshot, equalTo(getSnapshot()));
      return MockSpecifierContext.builder().setConfigs(_configs).build();
    }

    @Override
    public TopologyProvider getTopologyProvider() {
      return new MockTopologyProvider(this, _bgpTopology);
    }

    /** Provides empty layer 2 topology and a given {@link BgpTopology} */
    static class MockTopologyProvider extends TopologyProviderTestAdapter {
      private final BgpTopology _bgpTopology;

      MockTopologyProvider(IBatfish bf, BgpTopology bgpTopology) {
        super(bf);
        _bgpTopology = bgpTopology;
      }

      @Nonnull
      @Override
      public L3Adjacencies getL3Adjacencies(NetworkSnapshot snapshot) {
        return GlobalBroadcastNoPointToPoint.instance();
      }

      @Override
      public BgpTopology getBgpTopology(NetworkSnapshot snapshot) {
        return _bgpTopology;
      }
    }
  }
}

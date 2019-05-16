package org.batfish.question.bgpsessionstatus;

import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.bgp.BgpTopologyUtils.initBgpTopology;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
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
import static org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.COL_ESTABLISHED_STATUS;
import static org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.createMetadata;
import static org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.getRows;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.BatfishException;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.plugin.IBatfishTestAdapter;
import org.batfish.common.topology.Layer2Edge;
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
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.answers.SelfDescribingObject;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.question.bgpsessionstatus.BgpSessionStatusAnswerer.SessionStatus;
import org.batfish.specifier.MockSpecifierContext;
import org.batfish.specifier.SpecifierContext;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;

/** Tests of {@link BgpSessionStatusAnswerer} */
public class BgpSessionStatusAnswererTest {

  /*
  Setup for all tests:
  node1 peers with node2.
  node1 has an active BGP peer from 1.1.1.1 to 2.2.2.2 (AS 1 to AS 2).
  node2 has an active BGP peer from 2.2.2.2 to 1.1.1.1 (AS 2 to AS 1).
  node3 has an active BGP peer from 3.3.3.3 to 3.3.3.4 (AS 3 to AS 3).
  node4 has a dynamic BGP peer from 3.3.3.4 to 3.3.3.0/30 (AS 3 to AS 3).
  This results in four rows, represented by ROW_1, ROW_2, ROW_3, and ROW_4.
   */

  private static SortedMap<String, Configuration> CONFIGURATIONS;
  private static ValueGraph<BgpPeerConfigId, BgpSessionProperties> TOPOLOGY;

  private static final Ip IP1 = Ip.parse("1.1.1.1");
  private static final Ip IP2 = Ip.parse("2.2.2.2");
  private static final Ip IP3 = Ip.parse("3.3.3.3");
  private static final Ip IP4 = Ip.parse("3.3.3.4");
  private static final Ip UNNUM_IP = Ip.parse("169.254.0.1");

  private static final String NODE1 = "node1";
  private static final String NODE2 = "node2";
  private static final String NODE3 = "node3";
  private static final String NODE4 = "node4";
  private static final Set<String> ALL_NODES = ImmutableSet.of(NODE1, NODE2, NODE3, NODE4);

  private static final Map<String, ColumnMetadata> METADATA_MAP =
      createMetadata(new BgpSessionStatusQuestion()).toColumnMap();

  private static final Row ROW_1 =
      Row.builder()
          .put(COL_ESTABLISHED_STATUS, SessionStatus.ESTABLISHED)
          .put(COL_LOCAL_INTERFACE, null)
          .put(COL_LOCAL_IP, IP1)
          .put(COL_LOCAL_AS, 1L)
          .put(COL_NODE, new Node(NODE1))
          .put(COL_REMOTE_AS, LongSpace.of(2L).toString())
          .put(COL_REMOTE_NODE, new Node(NODE2))
          .put(COL_REMOTE_INTERFACE, null)
          .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, IP2))
          .put(COL_SESSION_TYPE, SessionType.EBGP_SINGLEHOP)
          .put(COL_VRF, "vrf")
          .build();

  private static final Row ROW_2 =
      Row.builder()
          .put(COL_ESTABLISHED_STATUS, SessionStatus.ESTABLISHED)
          .put(COL_LOCAL_INTERFACE, null)
          .put(COL_LOCAL_IP, IP2)
          .put(COL_LOCAL_AS, 2L)
          .put(COL_NODE, new Node(NODE2))
          .put(COL_REMOTE_AS, LongSpace.of(1L).toString())
          .put(COL_REMOTE_NODE, new Node(NODE1))
          .put(COL_REMOTE_INTERFACE, null)
          .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, IP1))
          .put(COL_SESSION_TYPE, SessionType.EBGP_SINGLEHOP)
          .put(COL_VRF, "vrf")
          .build();

  private static final Row ROW_3 =
      Row.builder()
          .put(COL_ESTABLISHED_STATUS, SessionStatus.ESTABLISHED)
          .put(COL_LOCAL_INTERFACE, null)
          .put(COL_LOCAL_IP, IP3)
          .put(COL_LOCAL_AS, 3L)
          .put(COL_NODE, new Node(NODE3))
          .put(COL_REMOTE_AS, LongSpace.of(3L).toString())
          .put(COL_REMOTE_NODE, new Node(NODE4))
          .put(COL_REMOTE_INTERFACE, null)
          .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, IP4))
          .put(COL_SESSION_TYPE, SessionType.IBGP)
          .put(COL_VRF, "vrf")
          .build();

  private static final Row ROW_4 =
      Row.builder()
          .put(COL_ESTABLISHED_STATUS, SessionStatus.ESTABLISHED)
          .put(COL_LOCAL_INTERFACE, null)
          .put(COL_LOCAL_IP, IP4)
          .put(COL_LOCAL_AS, 3L)
          .put(COL_NODE, new Node(NODE4))
          .put(COL_REMOTE_AS, LongSpace.of(3L).toString())
          .put(COL_REMOTE_NODE, new Node(NODE3))
          .put(COL_REMOTE_INTERFACE, null)
          .put(COL_REMOTE_IP, new SelfDescribingObject(Schema.IP, IP3))
          .put(COL_SESSION_TYPE, SessionType.IBGP)
          .put(COL_VRF, "vrf")
          .build();

  @BeforeClass
  public static void initConfigsAndTopology() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    Configuration node1 = createConfiguration(cb, NODE1, IP1, IP2, 1L, 2L);
    Configuration node2 = createConfiguration(cb, NODE2, IP2, IP1, 2L, 1L);
    Configuration node3 = createConfiguration(cb, NODE3, IP3, IP4, 3L, 3L);
    Configuration node4 =
        createConfigurationWithDynamicSession(
            cb, IP4, Prefix.create(IP3, 24), ImmutableList.of(3L));

    CONFIGURATIONS = ImmutableSortedMap.of(NODE1, node1, NODE2, node2, NODE3, node3, NODE4, node4);

    Map<Ip, Set<String>> ipOwners =
        ImmutableMap.of(
            IP1,
            ImmutableSet.of(NODE1),
            IP2,
            ImmutableSet.of(NODE2),
            IP3,
            ImmutableSet.of(NODE3),
            IP4,
            ImmutableSet.of(NODE4));
    TOPOLOGY = initBgpTopology(CONFIGURATIONS, ipOwners, false, null).getGraph();
  }

  private static Configuration createConfiguration(
      Configuration.Builder cb,
      String nodeName,
      Ip localIp,
      Ip remoteIp,
      Long localAs,
      Long remoteAs) {

    Configuration node = cb.setHostname(nodeName).build();
    Interface iface = new Interface("iface", node, InterfaceType.PHYSICAL);
    iface.setAllAddresses(
        ImmutableList.of(new InterfaceAddress(localIp, Ip.numSubnetBitsToSubnetMask(32))));

    BgpActivePeerConfig peerConfig =
        BgpActivePeerConfig.builder()
            .setLocalAs(localAs)
            .setRemoteAs(remoteAs)
            .setLocalIp(localIp)
            .setPeerAddress(remoteIp)
            .build();

    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO);
    bgpProcess.setNeighbors(ImmutableSortedMap.of(Prefix.create(remoteIp, 32), peerConfig));

    Vrf vrf1 = new Vrf("vrf");
    vrf1.setBgpProcess(bgpProcess);

    node.setVrfs(ImmutableMap.of("vrf", vrf1));
    node.setInterfaces(ImmutableSortedMap.of("iface", iface));
    return node;
  }

  private static Configuration createConfigurationWithDynamicSession(
      Configuration.Builder cb, Ip localIp, Prefix remotePrefix, List<Long> remoteAsList) {

    Configuration node = cb.setHostname(NODE4).build();
    Interface iface = new Interface("iface", node, InterfaceType.PHYSICAL);
    iface.setAllAddresses(
        ImmutableList.of(new InterfaceAddress(localIp, Ip.numSubnetBitsToSubnetMask(32))));

    BgpPassivePeerConfig peerConfig =
        BgpPassivePeerConfig.builder()
            .setLocalAs(3L)
            .setRemoteAsns(LongSpace.builder().includingAll(remoteAsList).build())
            .setLocalIp(localIp)
            .setPeerPrefix(remotePrefix)
            .build();

    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO);
    bgpProcess.setPassiveNeighbors(ImmutableSortedMap.of(remotePrefix, peerConfig));

    Vrf vrf1 = new Vrf("vrf");
    vrf1.setBgpProcess(bgpProcess);

    node.setVrfs(ImmutableMap.of("vrf", vrf1));
    node.setInterfaces(ImmutableSortedMap.of("iface", iface));
    return node;
  }

  @Test
  public void testAnswer() {
    BgpSessionStatusQuestion q = new BgpSessionStatusQuestion();
    List<Row> rows =
        getRows(
            q,
            CONFIGURATIONS,
            ALL_NODES,
            ALL_NODES,
            METADATA_MAP,
            ImmutableMap.of(),
            TOPOLOGY,
            TOPOLOGY);
    assertThat(rows, contains(ROW_1, ROW_2, ROW_3, ROW_4));
  }

  @Test
  public void testLimitNodes() {
    BgpSessionStatusQuestion q = new BgpSessionStatusQuestion(NODE1, null, null, null);
    List<Row> rows =
        getRows(
            q,
            CONFIGURATIONS,
            ImmutableSet.of(NODE1),
            ALL_NODES,
            METADATA_MAP,
            ImmutableMap.of(),
            TOPOLOGY,
            TOPOLOGY);
    assertThat(rows, contains(ROW_1));
  }

  @Test
  public void testLimitRemoteNodes() {
    BgpSessionStatusQuestion q = new BgpSessionStatusQuestion(null, NODE1, null, null);
    List<Row> rows =
        getRows(
            q,
            CONFIGURATIONS,
            ALL_NODES,
            ImmutableSet.of(NODE1),
            METADATA_MAP,
            ImmutableMap.of(),
            TOPOLOGY,
            TOPOLOGY);
    assertThat(rows, contains(ROW_2));
  }

  @Test
  public void testLimitStatus() {
    // Both sessions have status UNIQUE_MATCH
    BgpSessionStatusQuestion q =
        new BgpSessionStatusQuestion(null, null, SessionStatus.ESTABLISHED.name(), null);
    List<Row> rows =
        getRows(
            q,
            CONFIGURATIONS,
            ALL_NODES,
            ALL_NODES,
            METADATA_MAP,
            ImmutableMap.of(),
            TOPOLOGY,
            TOPOLOGY);
    assertThat(rows, contains(ROW_1, ROW_2, ROW_3, ROW_4));

    q = new BgpSessionStatusQuestion(null, null, SessionStatus.NOT_COMPATIBLE.name(), null);
    rows =
        getRows(
            q,
            CONFIGURATIONS,
            ALL_NODES,
            ALL_NODES,
            METADATA_MAP,
            ImmutableMap.of(),
            TOPOLOGY,
            TOPOLOGY);
    assertThat(rows, empty());
  }

  @Test
  public void testLimitType() {
    // Session between nodes 1 and 2 has type EBGP_SINGLEHOP
    BgpSessionStatusQuestion q =
        new BgpSessionStatusQuestion(null, null, null, SessionType.EBGP_SINGLEHOP.name());
    List<Row> rows =
        getRows(
            q,
            CONFIGURATIONS,
            ALL_NODES,
            ALL_NODES,
            METADATA_MAP,
            ImmutableMap.of(),
            TOPOLOGY,
            TOPOLOGY);
    assertThat(rows, contains(ROW_1, ROW_2));

    // Session between nodes 3 and 4 has type IBGP
    q = new BgpSessionStatusQuestion(null, null, null, SessionType.IBGP.name());
    rows =
        getRows(
            q,
            CONFIGURATIONS,
            ALL_NODES,
            ALL_NODES,
            METADATA_MAP,
            ImmutableMap.of(),
            TOPOLOGY,
            TOPOLOGY);
    assertThat(rows, containsInAnyOrder(ROW_3, ROW_4));
  }

  @Test
  public void testUnnumbered() {
    // Create a pair of configs with an unnumbered session
    NetworkFactory nf = new NetworkFactory();
    Configuration c1 =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(NODE1)
            .build();
    Configuration c2 =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(NODE2)
            .build();
    Vrf vrf1 = nf.vrfBuilder().setOwner(c1).setName(DEFAULT_VRF_NAME).build();
    Vrf vrf2 = nf.vrfBuilder().setOwner(c2).setName(DEFAULT_VRF_NAME).build();
    BgpProcess proc1 =
        nf.bgpProcessBuilder().setVrf(vrf1).setRouterId(Ip.ZERO).build(); // .setRouterId()
    BgpProcess proc2 =
        nf.bgpProcessBuilder().setVrf(vrf2).setRouterId(Ip.ZERO).build(); // .setRouterId()

    // Build interfaces and unnumbered peers
    String i1Name = "iface1";
    String i2Name = "iface2";
    nf.interfaceBuilder().setOwner(c1).setName(i1Name).setVrf(vrf1).build();
    nf.interfaceBuilder().setOwner(c2).setName(i2Name).setVrf(vrf1).build();
    BgpUnnumberedPeerConfig.Builder peerBuilder =
        BgpUnnumberedPeerConfig.builder().setLocalAs(1L).setRemoteAs(1L).setLocalIp(UNNUM_IP);
    proc1.setInterfaceNeighbors(
        ImmutableSortedMap.of(i1Name, peerBuilder.setPeerInterface(i1Name).build()));
    proc2.setInterfaceNeighbors(
        ImmutableSortedMap.of(i2Name, peerBuilder.setPeerInterface(i2Name).build()));

    // Build layer 2 topology with an edge between the two nodes' interfaces
    Layer2Topology layer2Topology =
        Layer2Topology.fromEdges(
            ImmutableSet.of(new Layer2Edge(NODE1, i1Name, null, NODE2, i2Name, null, null)));

    // Build BGP topology with layer 2 topology taken into account
    ValueGraph<BgpPeerConfigId, BgpSessionProperties> bgpTopology =
        initBgpTopology(
                ImmutableMap.of(NODE1, c1, NODE2, c2), ImmutableMap.of(), false, layer2Topology)
            .getGraph();

    // Check answer. Should see two rows, one for each peer
    List<Row> rows =
        getRows(
            new BgpSessionStatusQuestion(),
            ImmutableMap.of(NODE1, c1, NODE2, c2),
            ALL_NODES,
            ALL_NODES,
            METADATA_MAP,
            ImmutableMap.of(),
            bgpTopology,
            bgpTopology);

    // Attributes both peers have in common
    Matcher<Row> commonMatchers =
        allOf(
            hasColumn(COL_VRF, DEFAULT_VRF_NAME, Schema.STRING),
            hasColumn(COL_LOCAL_IP, nullValue(), Schema.IP),
            hasColumn(COL_REMOTE_IP, nullValue(), Schema.IP),
            hasColumn(COL_LOCAL_AS, 1L, Schema.LONG),
            hasColumn(COL_REMOTE_AS, 1L, Schema.LONG),
            hasColumn(COL_SESSION_TYPE, SessionType.IBGP_UNNUMBERED.name(), Schema.STRING),
            hasColumn(COL_ESTABLISHED_STATUS, SessionStatus.ESTABLISHED.name(), Schema.STRING));
    NodeInterfacePair nip1 = new NodeInterfacePair(NODE1, i1Name);
    NodeInterfacePair nip2 = new NodeInterfacePair(NODE2, i2Name);
    assertThat(
        rows,
        containsInAnyOrder(
            allOf(
                // NODE1:iface1 -> NODE2:iface2
                commonMatchers,
                hasColumn(COL_NODE, new Node(NODE1), Schema.NODE),
                hasColumn(COL_LOCAL_INTERFACE, nip1, Schema.INTERFACE),
                hasColumn(COL_REMOTE_NODE, new Node(NODE2), Schema.NODE),
                hasColumn(COL_REMOTE_INTERFACE, nip2, Schema.INTERFACE)),
            allOf(
                // NODE2:iface2 -> NODE1:iface1
                commonMatchers,
                hasColumn(COL_NODE, new Node(NODE2), Schema.NODE),
                hasColumn(COL_LOCAL_INTERFACE, nip2, Schema.INTERFACE),
                hasColumn(COL_REMOTE_NODE, new Node(NODE1), Schema.NODE),
                hasColumn(COL_REMOTE_INTERFACE, nip1, Schema.INTERFACE))));
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
        new BgpPeerConfigId(
            "c", DEFAULT_VRF_NAME, Prefix.create(remoteIp, Prefix.MAX_PREFIX_LENGTH), false);
    BgpActivePeerConfig peerX =
        BgpActivePeerConfig.builder()
            .setLocalIp(localIp)
            .setPeerAddress(remoteIp)
            .setLocalAs(1L)
            .setRemoteAs(2L)
            .build();

    // Two compatible (but unreachable) remote peers
    BgpPeerConfigId compat1Id =
        new BgpPeerConfigId(
            "c2", DEFAULT_VRF_NAME, Prefix.create(remoteIp, Prefix.MAX_PREFIX_LENGTH), false);
    BgpPeerConfigId compat2Id =
        new BgpPeerConfigId(
            "c3", DEFAULT_VRF_NAME, Prefix.create(remoteIp, Prefix.MAX_PREFIX_LENGTH), false);

    // One compatible AND reachable remote peer
    BgpPeerConfigId establishedId =
        new BgpPeerConfigId(
            "c4", DEFAULT_VRF_NAME, Prefix.create(remoteIp, Prefix.MAX_PREFIX_LENGTH), false);

    // Recycle the same compatible remote config for all the remotes
    BgpActivePeerConfig remotePeer =
        BgpActivePeerConfig.builder()
            .setLocalIp(remoteIp)
            .setPeerAddress(localIp)
            .setLocalAs(2L)
            .setRemoteAs(1L)
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

    SortedMap<String, Configuration> configs =
        ImmutableSortedMap.of(
            "c",
            createConfig(peerXId, peerX),
            "c2",
            createConfig(compat1Id, remotePeer),
            "c3",
            createConfig(compat2Id, remotePeer),
            "c4",
            createConfig(establishedId, remotePeer));
    Set<String> remoteNodeNames = ImmutableSet.of("c2", "c3", "c4");
    Map<Ip, Set<String>> ipOwners =
        ImmutableMap.of(localIp, ImmutableSet.of("c"), remoteIp, remoteNodeNames);

    List<Row> rows =
        getRows(
            new BgpSessionStatusQuestion("c", null, null, null),
            configs,
            ImmutableSet.of("c"),
            remoteNodeNames,
            METADATA_MAP,
            ipOwners,
            configuredTopology,
            establishedTopology);
    assertThat(rows, contains(hasColumn(COL_REMOTE_NODE, new Node("c4"), Schema.NODE)));
  }

  private Configuration createConfig(BgpPeerConfigId id, BgpPeerConfig peer) {
    NetworkFactory nf = new NetworkFactory();
    // Create a configuration with a BgpProcess
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(id.getHostname())
            .build();
    Vrf vrf = nf.vrfBuilder().setOwner(c).setName(id.getVrfName()).build();
    BgpProcess bgpProc = nf.bgpProcessBuilder().setVrf(vrf).setRouterId(Ip.ZERO).build();

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

    return c;
  }

  @Test
  public void testFullEndToEndAnswer() {
    BgpSessionStatusAnswerer answerer =
        new BgpSessionStatusAnswerer(new BgpSessionStatusQuestion(), new MockBatfish());
    assertThat(
        (TableAnswerElement) answerer.answer(),
        hasRows(containsInAnyOrder(ROW_1, ROW_2, ROW_3, ROW_4)));
  }

  static class MockBatfish extends IBatfishTestAdapter {
    @Override
    public SortedMap<String, Configuration> loadConfigurations() {
      return CONFIGURATIONS;
    }

    @Override
    public SpecifierContext specifierContext() {
      return MockSpecifierContext.builder().setConfigs(CONFIGURATIONS).build();
    }

    @Override
    public TopologyProvider getTopologyProvider() {
      return new MockTopologyProvider(this);
    }

    /**
     * Provides empty layer 2 topology and BGP topology based on {@link
     * BgpSessionStatusAnswererTest#TOPOLOGY}.
     */
    static class MockTopologyProvider extends TopologyProviderTestAdapter {
      MockTopologyProvider(IBatfish bf) {
        super(bf);
      }

      @Override
      public Optional<Layer2Topology> getLayer2Topology(NetworkSnapshot networkSnapshot) {
        return Optional.empty();
      }

      @Override
      public BgpTopology getBgpTopology(NetworkSnapshot snapshot) {
        return new BgpTopology(TOPOLOGY);
      }
    }
  }
}

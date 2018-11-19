package org.batfish.question.ipsecsessionstatus;

import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.IKE_PHASE1_FAILED;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.IKE_PHASE1_KEY_MISMATCH;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.IPSEC_PHASE2_FAILED;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.IPSEC_SESSION_ESTABLISHED;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfo.IpsecSessionStatus.MISSING_END_POINT;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionInfoMatchers.hasIpsecSessionStatus;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.COL_INITIATOR;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.COL_INIT_INTERFACE;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.COL_INIT_IP;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.COL_RESPONDER;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.COL_RESPONDER_INTERFACE;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.COL_RESPONDER_IP;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.COL_STATUS;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.COL_TUNNEL_INTERFACES;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.rawAnswer;
import static org.batfish.question.ipsecsessionstatus.IpsecSessionStatusAnswerer.toRow;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multiset;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.Comparator;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPeerConfigId;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.IpsecSession;
import org.batfish.datamodel.IpsecStaticPeerConfig;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.Row;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link IpsecSessionStatusAnswerer} */
public class IpsecSessionStatusAnswererTest {
  private static final String INITIATOR_IPSEC_PEER_CONFIG = "initiatorIpsecPeerConfig";
  private static final String RESPONDER_IPSEC_PEER_CONFIG = "responderIpsecPeerConfig";

  private static final String INITIATOR_HOST_NAME = "initiatorHostName";
  private static final String RESPONDER_HOST_NAME = "responderHostName";

  private IpsecStaticPeerConfig.Builder _ipsecStaticPeerConfigBuilder =
      IpsecStaticPeerConfig.builder();
  private MutableValueGraph<IpsecPeerConfigId, IpsecSession> _graph;
  private IpsecSession.Builder _ipsecSessionBuilder;
  private NetworkConfigurations _networkConfigurations;

  @Before
  public void setup() {
    Configuration initiatorNode;
    Configuration responderNode;
    _ipsecStaticPeerConfigBuilder
        .setPhysicalInterface("Test_interface")
        .setLocalAddress(new Ip("1.2.3.4"))
        .setTunnelInterface("Tunnel_interface");
    _graph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    _ipsecSessionBuilder = IpsecSession.builder();

    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    ImmutableSortedMap.Builder<String, Configuration> configs =
        new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());

    initiatorNode = cb.setHostname(INITIATOR_HOST_NAME).build();
    responderNode = cb.setHostname(RESPONDER_HOST_NAME).build();

    ImmutableSortedMap.Builder<String, IpsecPeerConfig> initiatorIpsecPeerConfigMapBuilder =
        ImmutableSortedMap.naturalOrder();
    initiatorNode.setIpsecPeerConfigs(
        initiatorIpsecPeerConfigMapBuilder
            .put(INITIATOR_IPSEC_PEER_CONFIG, _ipsecStaticPeerConfigBuilder.build())
            .build());

    ImmutableSortedMap.Builder<String, IpsecPeerConfig> responderIpsecPeerConfigMapBuilder =
        ImmutableSortedMap.naturalOrder();
    responderNode.setIpsecPeerConfigs(
        responderIpsecPeerConfigMapBuilder
            .put(RESPONDER_IPSEC_PEER_CONFIG, _ipsecStaticPeerConfigBuilder.build())
            .build());

    configs.put(initiatorNode.getHostname(), initiatorNode);
    configs.put(responderNode.getHostname(), responderNode);

    _networkConfigurations = NetworkConfigurations.of(configs.build());
  }

  @Test
  public void testGenerateRowsIke1Fail() {
    // IPSecSession does not have IKE phase 1 proposal set
    _graph.putEdgeValue(
        new IpsecPeerConfigId(INITIATOR_IPSEC_PEER_CONFIG, INITIATOR_HOST_NAME),
        new IpsecPeerConfigId(RESPONDER_IPSEC_PEER_CONFIG, RESPONDER_HOST_NAME),
        _ipsecSessionBuilder.build());
    Multiset<IpsecSessionInfo> sessions =
        rawAnswer(
            _networkConfigurations,
            _graph,
            ImmutableSet.of(INITIATOR_HOST_NAME),
            ImmutableSet.of(RESPONDER_HOST_NAME));

    // answer should have exactly one row
    assertThat(sessions, hasSize(1));

    assertThat(sessions.iterator().next(), hasIpsecSessionStatus(equalTo(IKE_PHASE1_FAILED)));
  }

  @Test
  public void testGenerateRowsIke1KeyFail() {
    // IPSecSession does not have IKE phase 1 key set
    _ipsecSessionBuilder.setNegotiatedIkeP1Proposal(new IkePhase1Proposal("test_ike_proposal"));
    _graph.putEdgeValue(
        new IpsecPeerConfigId(INITIATOR_IPSEC_PEER_CONFIG, INITIATOR_HOST_NAME),
        new IpsecPeerConfigId(RESPONDER_IPSEC_PEER_CONFIG, RESPONDER_HOST_NAME),
        _ipsecSessionBuilder.build());
    Multiset<IpsecSessionInfo> ipsecSessionInfos =
        rawAnswer(
            _networkConfigurations,
            _graph,
            ImmutableSet.of(INITIATOR_HOST_NAME),
            ImmutableSet.of(RESPONDER_HOST_NAME));

    // answer should have exactly one row
    assertThat(ipsecSessionInfos, hasSize(1));

    assertThat(
        ipsecSessionInfos.iterator().next(),
        hasIpsecSessionStatus(equalTo(IKE_PHASE1_KEY_MISMATCH)));
  }

  @Test
  public void testGenerateRowsIpsec2Fail() {
    // IPSecSession does not have IPSec phase 2 proposal set
    _ipsecSessionBuilder.setNegotiatedIkeP1Proposal(new IkePhase1Proposal("test_ike_proposal"));
    _ipsecSessionBuilder.setNegotiatedIkeP1Key(new IkePhase1Key());
    _graph.putEdgeValue(
        new IpsecPeerConfigId(INITIATOR_IPSEC_PEER_CONFIG, INITIATOR_HOST_NAME),
        new IpsecPeerConfigId(RESPONDER_IPSEC_PEER_CONFIG, RESPONDER_HOST_NAME),
        _ipsecSessionBuilder.build());
    Multiset<IpsecSessionInfo> ipsecSessionInfos =
        rawAnswer(
            _networkConfigurations,
            _graph,
            ImmutableSet.of(INITIATOR_HOST_NAME),
            ImmutableSet.of(RESPONDER_HOST_NAME));

    // answer should have exactly one row
    assertThat(ipsecSessionInfos, hasSize(1));

    assertThat(
        ipsecSessionInfos.iterator().next(), hasIpsecSessionStatus(equalTo(IPSEC_PHASE2_FAILED)));
  }

  @Test
  public void testGenerateRowsMissingEndpoint() {
    // Responder not set in the graph
    _graph.addNode(new IpsecPeerConfigId(INITIATOR_IPSEC_PEER_CONFIG, INITIATOR_HOST_NAME));
    Multiset<IpsecSessionInfo> ipsecSessionInfos =
        rawAnswer(
            _networkConfigurations,
            _graph,
            ImmutableSet.of(INITIATOR_HOST_NAME),
            ImmutableSet.of(RESPONDER_HOST_NAME));

    // answer should have exactly one row
    assertThat(ipsecSessionInfos, hasSize(1));

    assertThat(
        ipsecSessionInfos.iterator().next(), hasIpsecSessionStatus(equalTo(MISSING_END_POINT)));
  }

  @Test
  public void testGenerateRowsIpsecEstablished() {
    // IPSecSession has all phases negotiated and IKE phase 1 key consistent
    _ipsecSessionBuilder.setNegotiatedIkeP1Proposal(new IkePhase1Proposal("test_ike_proposal"));
    _ipsecSessionBuilder.setNegotiatedIkeP1Key(new IkePhase1Key());
    _ipsecSessionBuilder.setNegotiatedIpsecP2Proposal(new IpsecPhase2Proposal());
    _graph.putEdgeValue(
        new IpsecPeerConfigId(INITIATOR_IPSEC_PEER_CONFIG, INITIATOR_HOST_NAME),
        new IpsecPeerConfigId(RESPONDER_IPSEC_PEER_CONFIG, RESPONDER_HOST_NAME),
        _ipsecSessionBuilder.build());
    Multiset<IpsecSessionInfo> ipsecSessionInfos =
        rawAnswer(
            _networkConfigurations,
            _graph,
            ImmutableSet.of(INITIATOR_HOST_NAME),
            ImmutableSet.of(RESPONDER_HOST_NAME));

    // answer should have exactly one row
    assertThat(ipsecSessionInfos, hasSize(1));

    assertThat(
        ipsecSessionInfos.iterator().next(),
        hasIpsecSessionStatus(equalTo(IPSEC_SESSION_ESTABLISHED)));
  }

  @Test
  public void testToRow() {
    IpsecSessionInfo.Builder ipsecPeeringInfoBuilder = IpsecSessionInfo.builder();

    IpsecSessionInfo ipsecSessionInfo =
        ipsecPeeringInfoBuilder
            .setInitiatorHostname(INITIATOR_HOST_NAME)
            .setInitiatorInterface("Test_interface")
            .setInitiatorIp(new Ip("1.2.3.4"))
            .setInitiatorTunnelInterface("Tunnel_interface")
            .setResponderHostname(RESPONDER_HOST_NAME)
            .setResponderInterface("Test_interface")
            .setResponderIp(new Ip("2.3.4.5"))
            .setResponderTunnelInterface("Tunnel1_interface")
            .setIpsecSessionStatus(IPSEC_SESSION_ESTABLISHED)
            .build();

    Row row = toRow(ipsecSessionInfo);

    assertThat(
        row,
        allOf(
            hasColumn(COL_INITIATOR, equalTo(new Node(INITIATOR_HOST_NAME)), Schema.NODE),
            hasColumn(COL_RESPONDER, equalTo(new Node(RESPONDER_HOST_NAME)), Schema.NODE),
            hasColumn(
                COL_INIT_INTERFACE,
                equalTo(new NodeInterfacePair(INITIATOR_HOST_NAME, "Test_interface")),
                Schema.INTERFACE),
            hasColumn(COL_INIT_IP, equalTo(new Ip("1.2.3.4")), Schema.IP)));

    // Splitting the assertions to avoid "Unchecked generics arrays creation warnings for allOf()
    // varargs"
    assertThat(
        row,
        allOf(
            hasColumn(
                COL_RESPONDER_INTERFACE,
                equalTo(new NodeInterfacePair(RESPONDER_HOST_NAME, "Test_interface")),
                Schema.INTERFACE),
            hasColumn(COL_RESPONDER_IP, equalTo(new Ip("2.3.4.5")), Schema.IP)));

    assertThat(
        row,
        allOf(
            hasColumn(
                COL_TUNNEL_INTERFACES,
                equalTo("Tunnel_interface->Tunnel1_interface"),
                Schema.STRING),
            hasColumn(COL_STATUS, equalTo("IPSEC_SESSION_ESTABLISHED"), Schema.STRING)));
  }

  @Test
  public void testToRowTunnelMissingEndpoint() {
    IpsecSessionInfo.Builder ipsecPeeringInfoBuilder = IpsecSessionInfo.builder();

    IpsecSessionInfo ipsecSessionInfo =
        ipsecPeeringInfoBuilder
            .setInitiatorHostname(INITIATOR_HOST_NAME)
            .setInitiatorInterface("Test_interface")
            .setInitiatorIp(new Ip("1.2.3.4"))
            .setInitiatorTunnelInterface("Tunnel1_interface")
            .setResponderHostname(RESPONDER_HOST_NAME)
            .setResponderInterface("Test_interface")
            .setResponderIp(new Ip("2.3.4.5"))
            .setIpsecSessionStatus(MISSING_END_POINT)
            .build();

    Row row = toRow(ipsecSessionInfo);

    assertThat(
        row,
        allOf(
            hasColumn(COL_TUNNEL_INTERFACES, equalTo("Tunnel1_interface->"), Schema.STRING),
            hasColumn(COL_STATUS, equalTo("MISSING_END_POINT"), Schema.STRING)));
  }
}

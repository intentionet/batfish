package org.batfish.common.util;

import static org.batfish.common.util.IpsecUtil.getIpsecSession;
import static org.batfish.common.util.IpsecUtil.negotiateIkePhase1Key;
import static org.batfish.common.util.IpsecUtil.retainCompatibleTunnelEdges;
import static org.batfish.common.util.IpsecUtil.toEdgeSet;
import static org.batfish.datamodel.ConfigurationFormat.AWS;
import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.IkeKeyType;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.IpsecPeerConfigId;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.IpsecSession;
import org.batfish.datamodel.IpsecStaticPeerConfig;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.ipsec.IpsecTopology;
import org.junit.Before;
import org.junit.Test;

public class IpsecUtilTest {
  private IpsecTopology _ipsecTopology;
  private Map<String, Configuration> _configurations = new HashMap<>();

  @Before
  public void setup() {
    // creates a topology with the following edges with edge between Tunnel9 and Tunnel10 having no
    // corresponding IPsec edges/nodes
    /*
    * +----------------+                      +----------------+
      |                |                      | Tunnel2        |
      |        Tunnel1 +----------------------+                |
      |                |                      |                |
      |                |                      |                |
      |        Tunnel3 +----------------------+ Tunnel4        |
      |                |                      |                |
      |        interface5                     |                |
      |                +----------------------+interface6      |
      |                |                      |                |
      |                |                      |                |
      |       Tunnel7  +----------------------+Tunnel8         |
      |                |                      |                |
      |                |                      |                |
      |       Tunnel9  +----------------------+Tunnel10        |
      +----------------+                      +----------------+
    *
    *
    * */
    IpsecStaticPeerConfig ipsecPeerConfig1 =
        IpsecStaticPeerConfig.builder()
            .setSourceInterface("interface1")
            .setTunnelInterface("Tunnel1")
            .build();
    IpsecStaticPeerConfig ipsecPeerConfig2 =
        IpsecStaticPeerConfig.builder()
            .setSourceInterface("interface2")
            .setTunnelInterface("Tunnel2")
            .build();
    IpsecStaticPeerConfig ipsecPeerConfig3 =
        IpsecStaticPeerConfig.builder()
            .setSourceInterface("interface3")
            .setTunnelInterface("Tunnel3")
            .build();
    IpsecStaticPeerConfig ipsecPeerConfig4 =
        IpsecStaticPeerConfig.builder()
            .setSourceInterface("interface4")
            .setTunnelInterface("Tunnel4")
            .build();
    IpsecStaticPeerConfig ipsecPeerConfig5 =
        IpsecStaticPeerConfig.builder().setSourceInterface("interface5").build();
    IpsecStaticPeerConfig ipsecPeerConfig6 =
        IpsecStaticPeerConfig.builder().setSourceInterface("interface6").build();
    IpsecStaticPeerConfig ipsecPeerConfig7 =
        IpsecStaticPeerConfig.builder()
            .setSourceInterface("interface7")
            .setTunnelInterface("Tunnel7")
            .build();

    IpsecSession establishedSession =
        IpsecSession.builder().setNegotiatedIpsecP2Proposal(new IpsecPhase2Proposal()).build();
    IpsecSession unEstablishedSession = IpsecSession.builder().build();

    MutableValueGraph<IpsecPeerConfigId, IpsecSession> graph =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();

    // populate IPsec topology
    graph.putEdgeValue(
        new IpsecPeerConfigId("peer1", "host1"),
        new IpsecPeerConfigId("peer2", "host2"),
        establishedSession);
    graph.putEdgeValue(
        new IpsecPeerConfigId("peer2", "host2"),
        new IpsecPeerConfigId("peer1", "host1"),
        establishedSession);
    // between peer3 and peer4 session is broken in one direction
    graph.putEdgeValue(
        new IpsecPeerConfigId("peer3", "host1"),
        new IpsecPeerConfigId("peer4", "host2"),
        unEstablishedSession);
    graph.putEdgeValue(
        new IpsecPeerConfigId("peer4", "host2"),
        new IpsecPeerConfigId("peer3", "host1"),
        establishedSession);
    // peer5 and peer6 are not running IPsec on tunnel interfaces
    graph.putEdgeValue(
        new IpsecPeerConfigId("peer5", "host1"),
        new IpsecPeerConfigId("peer6", "host2"),
        unEstablishedSession);
    // a dangling IPsec peer with no corresponding endpoint
    graph.addNode(new IpsecPeerConfigId("peer7", "host1"));
    _ipsecTopology = new IpsecTopology(graph);

    // populate Configurations to get IPsecPeerConfig objects from IPsecPeerConfigIds
    Configuration c1 = new Configuration("host1", ConfigurationFormat.CISCO_IOS);
    Configuration c2 = new Configuration("host2", ConfigurationFormat.CISCO_IOS);
    c1.setIpsecPeerConfigs(
        ImmutableSortedMap.of(
            "peer1",
            ipsecPeerConfig1,
            "peer3",
            ipsecPeerConfig3,
            "peer5",
            ipsecPeerConfig5,
            "peer7",
            ipsecPeerConfig7));
    c2.setIpsecPeerConfigs(
        ImmutableSortedMap.of(
            "peer2", ipsecPeerConfig2, "peer4", ipsecPeerConfig4, "peer6", ipsecPeerConfig6));

    ImmutableSortedMap.Builder<String, Interface> interfaceBuilder =
        ImmutableSortedMap.naturalOrder();
    c1.setInterfaces(
        interfaceBuilder
            .put("Tunnel1", Interface.builder().setName("Tunnel1").setOwner(c1).build())
            .put("Tunnel3", Interface.builder().setName("Tunnel3").setOwner(c1).build())
            .put("interface5", Interface.builder().setName("interface5").setOwner(c1).build())
            .put("Tunnel9", Interface.builder().setName("Tunnel9").setOwner(c1).build())
            .put("Tunnel7", Interface.builder().setName("Tunnel7").setOwner(c1).build())
            .build());
    interfaceBuilder = ImmutableSortedMap.naturalOrder();
    c2.setInterfaces(
        interfaceBuilder
            .put("Tunnel2", Interface.builder().setName("Tunnel2").setOwner(c2).build())
            .put("Tunnel4", Interface.builder().setName("Tunnel4").setOwner(c2).build())
            .put("interface6", Interface.builder().setName("interface6").setOwner(c2).build())
            .put("Tunnel8", Interface.builder().setName("Tunnel8").setOwner(c2).build())
            .put("Tunnel10", Interface.builder().setName("Tunnel10").setOwner(c2).build())
            .build());

    _configurations.put("host1", c1);
    _configurations.put("host2", c2);
  }

  @Test
  public void testRetainCompatibleTunnelEdges() {
    Set<Edge> compatibleIpsecEdges =
        toEdgeSet(retainCompatibleTunnelEdges(_ipsecTopology, _configurations), _configurations);

    assertThat(
        compatibleIpsecEdges,
        containsInAnyOrder(
            new Edge(
                NodeInterfacePair.of("host1", "Tunnel1"), NodeInterfacePair.of("host2", "Tunnel2")),
            new Edge(
                NodeInterfacePair.of("host2", "Tunnel2"),
                NodeInterfacePair.of("host1", "Tunnel1"))));
  }

  @Test
  public void testGetIpsecSesssionCloud() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb = nf.configurationBuilder();
    IpsecStaticPeerConfig.Builder ipsecPeerConfigBuilder = IpsecStaticPeerConfig.builder();

    assertTrue(
        getIpsecSession(
                cb.setConfigurationFormat(AWS).build(),
                cb.setConfigurationFormat(AWS).build(),
                ipsecPeerConfigBuilder.build(),
                ipsecPeerConfigBuilder.build())
            .isCloud());

    assertTrue(
        getIpsecSession(
                cb.setConfigurationFormat(CISCO_IOS).build(),
                cb.setConfigurationFormat(AWS).build(),
                ipsecPeerConfigBuilder.build(),
                ipsecPeerConfigBuilder.build())
            .isCloud());

    assertFalse(
        getIpsecSession(
                cb.setConfigurationFormat(CISCO_IOS).build(),
                cb.setConfigurationFormat(CISCO_IOS).build(),
                ipsecPeerConfigBuilder.build(),
                ipsecPeerConfigBuilder.build())
            .isCloud());
  }

  @Test
  public void testNegotiateIkePhase1KeyRsa() {
    IpsecSession.Builder ipsecSessionBuilder = IpsecSession.builder();
    IkePhase1Key initiatorKey = new IkePhase1Key();
    initiatorKey.setKeyType(IkeKeyType.RSA_PUB_KEY);
    initiatorKey.setKeyHash("key1");
    IkePhase1Key responderKey = new IkePhase1Key();
    responderKey.setKeyType(IkeKeyType.RSA_PUB_KEY);
    responderKey.setKeyHash("key2");

    negotiateIkePhase1Key(initiatorKey, responderKey, ipsecSessionBuilder);

    assertThat(ipsecSessionBuilder.getNegotiatedIkeP1Key(), notNullValue());
    assertThat(
        ipsecSessionBuilder.getNegotiatedIkeP1Key().getKeyType(), equalTo(IkeKeyType.RSA_PUB_KEY));
  }

  @Test
  public void testNegotiatedIkePhase1EncryptedPsk() {
    IpsecSession.Builder ipsecSessionBuilder = IpsecSession.builder();
    IkePhase1Key initiatorKey = new IkePhase1Key();
    initiatorKey.setKeyType(IkeKeyType.PRE_SHARED_KEY_ENCRYPTED);
    initiatorKey.setKeyHash("key1");
    IkePhase1Key responderKey = new IkePhase1Key();
    responderKey.setKeyType(IkeKeyType.PRE_SHARED_KEY_ENCRYPTED);
    responderKey.setKeyHash("key2");

    negotiateIkePhase1Key(initiatorKey, responderKey, ipsecSessionBuilder);

    assertThat(ipsecSessionBuilder.getNegotiatedIkeP1Key(), notNullValue());
    assertThat(
        ipsecSessionBuilder.getNegotiatedIkeP1Key().getKeyType(),
        equalTo(IkeKeyType.PRE_SHARED_KEY_ENCRYPTED));
  }

  @Test
  public void testNegotiateIkePhase1KeyPsk() {
    IpsecSession.Builder ipsecSessionBuilder = IpsecSession.builder();
    IkePhase1Key initiatorKey = new IkePhase1Key();
    initiatorKey.setKeyType(IkeKeyType.PRE_SHARED_KEY_UNENCRYPTED);
    initiatorKey.setKeyHash("key1");
    IkePhase1Key responderKey = new IkePhase1Key();
    responderKey.setKeyType(IkeKeyType.PRE_SHARED_KEY_UNENCRYPTED);
    responderKey.setKeyHash("key1");

    negotiateIkePhase1Key(initiatorKey, responderKey, ipsecSessionBuilder);

    assertThat(ipsecSessionBuilder.getNegotiatedIkeP1Key(), notNullValue());
    assertThat(
        ipsecSessionBuilder.getNegotiatedIkeP1Key().getKeyType(),
        equalTo(IkeKeyType.PRE_SHARED_KEY_UNENCRYPTED));
    assertThat(ipsecSessionBuilder.getNegotiatedIkeP1Key().getKeyHash(), equalTo("key1"));
  }

  @Test
  public void testNegotiateIkePhase1KeyPskFail() {
    IpsecSession.Builder ipsecSessionBuilder = IpsecSession.builder();
    IkePhase1Key initiatorKey = new IkePhase1Key();
    initiatorKey.setKeyType(IkeKeyType.PRE_SHARED_KEY_UNENCRYPTED);
    initiatorKey.setKeyHash("key1");
    IkePhase1Key responderKey = new IkePhase1Key();
    responderKey.setKeyType(IkeKeyType.PRE_SHARED_KEY_UNENCRYPTED);
    responderKey.setKeyHash("key2");

    negotiateIkePhase1Key(initiatorKey, responderKey, ipsecSessionBuilder);

    assertThat(ipsecSessionBuilder.getNegotiatedIkeP1Key(), nullValue());
  }
}

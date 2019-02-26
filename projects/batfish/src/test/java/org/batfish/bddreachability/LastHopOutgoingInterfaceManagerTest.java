package org.batfish.bddreachability;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.sf.javabdd.BDD;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link LastHopOutgoingInterfaceManager}. */
public final class LastHopOutgoingInterfaceManagerTest {
  private static final Topology EMPTY_TOPOLOGY = new Topology(ImmutableSortedSet.of());

  private static final String NODE1 = "NODE1";
  private static final String NODE2 = "NODE2";
  private static final String IFACE1 = "IFACE1";
  private static final String IFACE2 = "IFACE2";

  private BDDPacket _pkt;
  private BDD _trueBdd;

  private Configuration.Builder _cb;
  private Interface.Builder _ib;
  private Vrf.Builder _vb;

  private static Map<String, Configuration> configs(Configuration... configurations) {
    return Arrays.stream(configurations)
        .collect(ImmutableMap.toImmutableMap(Configuration::getHostname, Function.identity()));
  }

  @Before
  public void setup() {
    _pkt = new BDDPacket();
    _trueBdd = _pkt.getFactory().one();

    NetworkFactory nf = new NetworkFactory();
    _cb = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _ib = nf.interfaceBuilder();
    _vb = nf.vrfBuilder();
  }

  private Configuration configurationWithSession() {
    Configuration c = _cb.build();
    Vrf vrf = _vb.setOwner(c).build();
    FirewallSessionInterfaceInfo firewallSessionInterfaceInfo =
        new FirewallSessionInterfaceInfo(ImmutableSet.of(), null, null);
    _ib.setActive(true)
        .setOwner(c)
        .setVrf(vrf)
        .setFirewallSessionInterfaceInfo(firewallSessionInterfaceInfo)
        .build();
    return c;
  }

  @Test
  public void testTrivial() {
    LastHopOutgoingInterfaceManager mgr =
        new LastHopOutgoingInterfaceManager(_pkt, ImmutableMap.of(), ImmutableSet.of());
    assertThat(mgr.getFiniteDomains().values(), empty());
    assertThat(mgr.getLastHopOutgoingInterfaceBdd("", "", "", ""), equalTo(_trueBdd));
  }

  @Test
  public void testNoEdges() {
    Configuration c = configurationWithSession();
    LastHopOutgoingInterfaceManager mgr =
        new LastHopOutgoingInterfaceManager(_pkt, configs(c), ImmutableSet.of());
    assertThat(mgr.getFiniteDomains().values(), empty());
    assertThat(
        mgr.getLastHopOutgoingInterfaceBdd(
            NODE1, IFACE1, c.getHostname(), c.getAllInterfaces().firstKey()),
        equalTo(_trueBdd));
  }

  @Test
  public void testOneEdge() {
    Configuration c = configurationWithSession();
    String node = c.getHostname();
    String iface = c.getAllInterfaces().firstKey();
    Set<Edge> edges = ImmutableSortedSet.of(Edge.of(NODE1, IFACE1, node, iface));
    LastHopOutgoingInterfaceManager mgr =
        new LastHopOutgoingInterfaceManager(_pkt, configs(c), edges);
    assertThat(mgr.getFiniteDomains().entrySet(), hasSize(1));
    BDD lastHopBdd = mgr.getLastHopOutgoingInterfaceBdd(NODE1, IFACE1, node, iface);
    assertFalse(lastHopBdd.isZero());
    assertFalse(lastHopBdd.isOne());

    BDD noLastHopBdd = mgr.getNoLastHopOutgoingInterfaceBdd(node, iface);
    assertFalse(noLastHopBdd.isZero());
    assertFalse(noLastHopBdd.isOne());

    assertThat(lastHopBdd, not(equalTo(noLastHopBdd)));
  }

  @Test
  public void testTwoEdges() {
    Configuration c = configurationWithSession();
    String node = c.getHostname();
    String iface = c.getAllInterfaces().firstKey();
    Set<Edge> edges =
        ImmutableSet.of(Edge.of(NODE1, IFACE1, node, iface), Edge.of(NODE2, IFACE2, node, iface));
    LastHopOutgoingInterfaceManager mgr =
        new LastHopOutgoingInterfaceManager(_pkt, configs(c), edges);
    assertThat(mgr.getFiniteDomains().values(), not(empty()));
    BDD bdd1 = mgr.getLastHopOutgoingInterfaceBdd(NODE1, IFACE1, node, iface);
    BDD bdd2 = mgr.getLastHopOutgoingInterfaceBdd(NODE2, IFACE2, node, iface);
    assertFalse(bdd1.isZero());
    assertFalse(bdd1.isOne());
    assertFalse(bdd2.isZero());
    assertFalse(bdd2.isOne());
    assertTrue(!bdd1.equals(bdd2));
  }
}

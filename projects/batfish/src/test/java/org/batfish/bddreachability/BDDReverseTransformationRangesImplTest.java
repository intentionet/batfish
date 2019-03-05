package org.batfish.bddreachability;

import static org.batfish.bddreachability.BDDReverseTransformationRangesImpl.TransformationType.INCOMING;
import static org.batfish.bddreachability.BDDReverseTransformationRangesImpl.TransformationType.OUTGOING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.util.Map;
import net.sf.javabdd.BDD;
import org.batfish.bddreachability.BDDReverseTransformationRangesImpl.Key;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.HeaderSpaceToBDD;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.expr.StateExpr;
import org.batfish.z3.state.PreInInterface;
import org.batfish.z3.state.PreOutEdge;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link BDDReverseTransformationRangesImpl}. */
public class BDDReverseTransformationRangesImplTest {
  private static final String HOSTNAME = "HOSTNAME";

  private BDDPacket _bddPacket;
  private Map<String, Configuration> _configs;
  private Interface.Builder _ib;
  private HeaderSpaceToBDD _headerSpaceToBDD;
  private LastHopOutgoingInterfaceManager _lastHopManager;

  @Before
  public void setup() {
    _bddPacket = new BDDPacket();
    NetworkFactory nf = new NetworkFactory();
    Configuration config =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(HOSTNAME)
            .build();
    Vrf vrf = nf.vrfBuilder().setOwner(config).build();
    _ib = nf.interfaceBuilder().setOwner(config).setVrf(vrf).setActive(true);
    _configs = ImmutableMap.of(HOSTNAME, config);
    _headerSpaceToBDD = new HeaderSpaceToBDD(_bddPacket, ImmutableMap.of());
    _lastHopManager = new LastHopOutgoingInterfaceManager(_bddPacket, _configs, ImmutableSet.of());
  }

  @Test
  public void testUnreachable() {
    /* Should get 0 BDD when the transformation was not reached by the forward analysis (i.e. there
     * is no entry for the corresponding state(s) in the reachability map.
     */

    Interface iface = _ib.build();

    Map<String, BDDSourceManager> srcManagers =
        BDDSourceManager.forNetwork(_bddPacket, _configs, true);

    BDDReverseTransformationRangesImpl ranges =
        new BDDReverseTransformationRangesImpl(
            _configs, ImmutableMap.of(), _bddPacket, null, srcManagers, _lastHopManager);

    assertTrue(
        ranges.reverseIncomingTransformationRange(HOSTNAME, iface.getName(), null, null).isZero());
    assertTrue(
        ranges.reverseOutgoingTransformationRange(HOSTNAME, iface.getName(), null, null).isZero());
  }

  @Test
  public void testIncomingTransformationRange() {
    Interface iface = _ib.build();
    String iName = iface.getName();

    Map<String, BDDSourceManager> srcManagers =
        BDDSourceManager.forNetwork(_bddPacket, _configs, true);

    BDD fwdPreInBdd = _headerSpaceToBDD.getDstIpSpaceToBdd().toBDD(Ip.parse("1.1.1.1"));
    BDD bwdPreInBdd = _headerSpaceToBDD.getSrcIpSpaceToBdd().toBDD(Ip.parse("1.1.1.1"));

    Map<StateExpr, BDD> forwardReach =
        ImmutableMap.of(new PreInInterface(HOSTNAME, iface.getName()), fwdPreInBdd);

    // without incoming filter
    {
      BDDReverseTransformationRangesImpl ranges =
          new BDDReverseTransformationRangesImpl(
              _configs, forwardReach, _bddPacket, null, srcManagers, _lastHopManager);
      assertThat(
          ranges.reverseIncomingTransformationRange(HOSTNAME, iface.getName(), iName, null),
          equalTo(bwdPreInBdd));
    }

    // with incoming filter
    {
      String aclName = "ACL";
      iface.setIncomingFilter(IpAccessList.builder().setName(aclName).build());
      BDD fwdAclBdd =
          _headerSpaceToBDD.toBDD(
              HeaderSpace.builder().setDstPorts(ImmutableList.of(new SubRange(100, 200))).build());
      BDD bwdAclBdd =
          _headerSpaceToBDD.toBDD(
              HeaderSpace.builder().setSrcPorts(ImmutableList.of(new SubRange(100, 200))).build());

      BDDReverseTransformationRangesImpl ranges =
          new BDDReverseTransformationRangesImpl(
              _configs,
              forwardReach,
              _bddPacket,
              ImmutableMap.of(HOSTNAME, ImmutableMap.of(aclName, () -> fwdAclBdd)),
              srcManagers,
              _lastHopManager);
      assertThat(
          ranges.reverseIncomingTransformationRange(HOSTNAME, iName, iName, null),
          equalTo(bwdPreInBdd.and(bwdAclBdd)));
    }
  }

  @Test
  public void testOutgoingTransformationRange() {
    Interface iface = _ib.build();
    String iName = iface.getName();

    Map<String, BDDSourceManager> srcManagers =
        BDDSourceManager.forNetwork(_bddPacket, _configs, true);

    BDD fwdPreOutEdge1Bdd = _headerSpaceToBDD.getDstIpSpaceToBdd().toBDD(Ip.parse("1.1.1.1"));
    BDD fwdPreOutEdge2Bdd = _headerSpaceToBDD.getDstIpSpaceToBdd().toBDD(Ip.parse("1.1.1.2"));

    BDD reach =
        _headerSpaceToBDD
            .getSrcIpSpaceToBdd()
            .toBDD(Ip.parse("1.1.1.1"))
            .or(_headerSpaceToBDD.getSrcIpSpaceToBdd().toBDD(Ip.parse("1.1.1.2")));

    // two edges from iface
    Map<StateExpr, BDD> forwardReach =
        ImmutableMap.of(
            new PreOutEdge(HOSTNAME, iface.getName(), "1", "1"),
            fwdPreOutEdge1Bdd,
            new PreOutEdge(HOSTNAME, iface.getName(), "2", "2"),
            fwdPreOutEdge2Bdd);

    // without pre-transformation outgoing filter
    {
      BDDReverseTransformationRangesImpl ranges =
          new BDDReverseTransformationRangesImpl(
              _configs, forwardReach, _bddPacket, null, srcManagers, _lastHopManager);
      assertThat(
          ranges.reverseOutgoingTransformationRange(HOSTNAME, iName, null, null), equalTo(reach));
    }

    // with pre-transformation outgoing filter
    {
      String aclName = "ACL";
      iface.setPreTransformationOutgoingFilter(IpAccessList.builder().setName(aclName).build());
      BDD fwdAclBdd =
          _headerSpaceToBDD.toBDD(
              HeaderSpace.builder().setDstPorts(ImmutableList.of(new SubRange(100, 200))).build());
      BDD bwdAclBdd =
          _headerSpaceToBDD.toBDD(
              HeaderSpace.builder().setSrcPorts(ImmutableList.of(new SubRange(100, 200))).build());

      BDDReverseTransformationRangesImpl ranges =
          new BDDReverseTransformationRangesImpl(
              _configs,
              forwardReach,
              _bddPacket,
              ImmutableMap.of(HOSTNAME, ImmutableMap.of(aclName, () -> fwdAclBdd)),
              srcManagers,
              _lastHopManager);
      assertThat(
          ranges.reverseOutgoingTransformationRange(HOSTNAME, iName, null, null),
          equalTo(reach.and(bwdAclBdd)));
    }
  }

  /** Test that source and last hop constraints are erased. */
  @Test
  public void testEraseNonPacketVars() {
    Interface iface = _ib.build();
    String iName = iface.getName();

    Map<String, BDDSourceManager> srcManagers =
        BDDSourceManager.forNetwork(_bddPacket, _configs, true);

    // make the node a session node.
    iface.setFirewallSessionInterfaceInfo(
        new FirewallSessionInterfaceInfo(ImmutableList.of(iName), null, null));

    _lastHopManager =
        new LastHopOutgoingInterfaceManager(
            _bddPacket,
            _configs,
            ImmutableSet.of(
                new Edge(new NodeInterfacePair("A", "A"), new NodeInterfacePair(HOSTNAME, iName))));

    BDD srcBdd = srcManagers.get(HOSTNAME).getSourceInterfaceBDD(iName);
    BDD lastHopBdd = _lastHopManager.getNoLastHopOutgoingInterfaceBdd(HOSTNAME, iName);
    BDD reach = srcBdd.and(lastHopBdd);

    Map<StateExpr, BDD> forwardReach =
        ImmutableMap.of(
            new PreInInterface(HOSTNAME, iName),
            reach,
            new PreOutEdge(HOSTNAME, iName, "1", "1"),
            reach);

    BDDReverseTransformationRangesImpl ranges =
        new BDDReverseTransformationRangesImpl(
            _configs, forwardReach, _bddPacket, null, srcManagers, _lastHopManager);
    assertTrue(ranges.reverseOutgoingTransformationRange(HOSTNAME, iName, iName, null).isOne());
    assertTrue(ranges.reverseIncomingTransformationRange(HOSTNAME, iName, iName, null).isOne());
  }

  @Test
  public void testSourceAndLastHopConstraint() {
    Interface iface = _ib.build();
    String iName = iface.getName();

    // reinitialize source managers with new interface
    Map<String, BDDSourceManager> srcManagers =
        BDDSourceManager.forNetwork(_bddPacket, _configs, true);
    BDDSourceManager srcManager = srcManagers.get(HOSTNAME);

    String neighbor1 = "neighbor1";
    String neighbor2 = "neighbor2";
    String neighborIface = "neighborIface";
    NodeInterfacePair lastHop1 = new NodeInterfacePair(neighbor1, neighborIface);
    NodeInterfacePair lastHop2 = new NodeInterfacePair(neighbor2, neighborIface);
    _lastHopManager =
        new LastHopOutgoingInterfaceManager(
            _bddPacket,
            _configs,
            ImmutableSet.of(
                new Edge(lastHop1, new NodeInterfacePair(HOSTNAME, iName)),
                new Edge(lastHop2, new NodeInterfacePair(HOSTNAME, iName))));

    BDDReverseTransformationRangesImpl ranges =
        new BDDReverseTransformationRangesImpl(
            _configs, ImmutableMap.of(), _bddPacket, null, srcManagers, _lastHopManager);

    assertThat(
        ranges.sourceAndLastHopConstraint(HOSTNAME, null, null),
        equalTo(srcManager.getOriginatingFromDeviceBDD()));

    assertThat(
        ranges.sourceAndLastHopConstraint(HOSTNAME, iName, null),
        equalTo(
            srcManager
                .getSourceInterfaceBDD(iName)
                .and(_lastHopManager.getNoLastHopOutgoingInterfaceBdd(HOSTNAME, iName))));

    assertThat(
        ranges.sourceAndLastHopConstraint(HOSTNAME, iName, lastHop1),
        equalTo(
            srcManager
                .getSourceInterfaceBDD(iName)
                .and(
                    _lastHopManager.getLastHopOutgoingInterfaceBdd(
                        HOSTNAME, iName, neighbor1, neighborIface))));

    assertThat(
        ranges.sourceAndLastHopConstraint(HOSTNAME, iName, lastHop2),
        equalTo(
            srcManager
                .getSourceInterfaceBDD(iName)
                .and(
                    _lastHopManager.getLastHopOutgoingInterfaceBdd(
                        HOSTNAME, iName, neighbor2, neighborIface))));
  }

  @Test
  public void testKeyEquals() {
    String node = "node";
    String iface = "iface";
    String inIface = "inIface";
    NodeInterfacePair lastHop = new NodeInterfacePair("lastHopNode", "lastHopIface");
    new EqualsTester()
        .addEqualityGroup(
            new Key(node, iface, OUTGOING, inIface, null),
            new Key(node, iface, OUTGOING, inIface, null))
        .addEqualityGroup(new Key("", iface, OUTGOING, inIface, null))
        .addEqualityGroup(new Key(node, "", OUTGOING, inIface, null))
        .addEqualityGroup(new Key(node, iface, INCOMING, inIface, null))
        .addEqualityGroup(new Key(node, iface, OUTGOING, null, null))
        .addEqualityGroup(new Key(node, iface, OUTGOING, inIface, lastHop))
        .testEquals();
  }
}

package org.batfish.z3;

import static com.google.common.collect.Maps.immutableEntry;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasAclActions;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasAclConditions;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasEnabledEdges;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasEnabledFlowSinks;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasEnabledInterfaces;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasEnabledNodes;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasEnabledVrfs;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasFibConditions;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasIpsByHostname;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasSourceNats;
import static org.batfish.z3.matchers.SynthesizerInputMatchers.hasTopologyInterfaces;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Range;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SourceNat;
import org.batfish.datamodel.TestDataPlane;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.collections.FibRow;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.z3.expr.FibRowMatchExpr;
import org.batfish.z3.expr.HeaderSpaceMatchExpr;
import org.batfish.z3.expr.OrExpr;
import org.batfish.z3.expr.RangeMatchExpr;
import org.batfish.z3.state.AclPermit;
import org.junit.Before;
import org.junit.Test;

public class SynthesizerInputImplTest {

  private IpAccessList.Builder _aclb;

  private IpAccessListLine.Builder _acllb;

  private Configuration.Builder _cb;

  private Interface.Builder _ib;

  private SynthesizerInputImpl.Builder _inputBuilder;

  private NetworkFactory _nf;

  private SourceNat.Builder _snb;

  private Vrf.Builder _vb;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _vb = _nf.vrfBuilder();
    _ib = _nf.interfaceBuilder().setActive(true).setBandwidth(1E9d);
    _aclb = _nf.aclBuilder();
    _acllb = IpAccessListLine.builder();
    _inputBuilder = SynthesizerInputImpl.builder();
    _snb = SourceNat.builder();
  }

  @Test
  public void testComputeAclActions() {
    Configuration srcNode = _cb.build();
    Configuration nextHop = _cb.build();
    IpAccessList edgeInterfaceInAcl =
        _aclb
            .setOwner(srcNode)
            .setLines(
                ImmutableList.of(
                    IpAccessListLine.builder().setAction(LineAction.ACCEPT).build(),
                    IpAccessListLine.builder().setAction(LineAction.REJECT).build()))
            .build();
    IpAccessList srcInterfaceOutAcl = _aclb.build();
    IpAccessList iFlowSinkOutAcl = _aclb.build();
    IpAccessList iNoEdgeInAcl = _aclb.build();
    IpAccessList iNoEdgeOutAcl = _aclb.build();
    IpAccessList nextHopInterfaceInAcl = _aclb.setOwner(nextHop).build();
    IpAccessList nextHopInterfaceOutAcl = _aclb.build();
    Vrf srcVrf = _vb.setOwner(srcNode).build();
    Vrf nextHopVrf = _vb.setOwner(nextHop).build();
    Interface srcInterface =
        _ib.setOwner(srcNode)
            .setVrf(srcVrf)
            .setIncomingFilter(edgeInterfaceInAcl)
            .setOutgoingFilter(srcInterfaceOutAcl)
            .build();
    Interface iFlowSink =
        _ib.setIncomingFilter(edgeInterfaceInAcl).setOutgoingFilter(iFlowSinkOutAcl).build();
    /*
     * Interface without an edge: Its ACLs should be absent with data plane, but present without
     * data plane.
     */
    _ib.setIncomingFilter(iNoEdgeInAcl).setOutgoingFilter(iNoEdgeOutAcl).build();
    Interface nextHopInterface =
        _ib.setIncomingFilter(nextHopInterfaceInAcl)
            .setOutgoingFilter(nextHopInterfaceOutAcl)
            .setOwner(nextHop)
            .setVrf(nextHopVrf)
            .build();
    Edge forwardEdge = new Edge(srcInterface, nextHopInterface);
    Edge backEdge = new Edge(nextHopInterface, srcInterface);
    SynthesizerInput inputWithoutDataPlane =
        _inputBuilder
            .setConfigurations(
                ImmutableMap.of(srcNode.getName(), srcNode, nextHop.getName(), nextHop))
            .build();
    SynthesizerInput inputWithDataPlane =
        _inputBuilder
            .setDataPlane(
                TestDataPlane.builder()
                    .setFlowSinks(
                        ImmutableSet.of(
                            new NodeInterfacePair(srcNode.getName(), iFlowSink.getName())))
                    .setTopologyEdges(ImmutableSortedSet.of(forwardEdge, backEdge))
                    .build())
            .build();
    Map<Integer, LineAction> expectedActions =
        ImmutableMap.of(0, LineAction.ACCEPT, 1, LineAction.REJECT);
    Map<String, Map<Integer, LineAction>> expectedSrcNodeWithDataPlane =
        ImmutableMap.of(
            edgeInterfaceInAcl.getName(),
            expectedActions,
            srcInterfaceOutAcl.getName(),
            expectedActions,
            iFlowSinkOutAcl.getName(),
            expectedActions);
    Map<String, Map<Integer, LineAction>> expectedSrcNodeWithoutDataPlane =
        ImmutableMap.<String, Map<Integer, LineAction>>builder()
            .putAll(expectedSrcNodeWithDataPlane)
            .put(iNoEdgeInAcl.getName(), expectedActions)
            .put(iNoEdgeOutAcl.getName(), expectedActions)
            .build();
    Map<String, Map<Integer, LineAction>> expectedNextHop =
        ImmutableMap.of(
            nextHopInterfaceInAcl.getName(),
            expectedActions,
            nextHopInterfaceOutAcl.getName(),
            expectedActions);

    assertThat(
        inputWithDataPlane,
        hasAclActions(
            equalTo(
                ImmutableMap.of(
                    srcNode.getName(),
                    expectedSrcNodeWithDataPlane,
                    nextHop.getName(),
                    expectedNextHop))));
    assertThat(
        inputWithoutDataPlane,
        hasAclActions(
            equalTo(
                ImmutableMap.of(
                    srcNode.getName(),
                    expectedSrcNodeWithoutDataPlane,
                    nextHop.getName(),
                    expectedNextHop))));
  }

  @Test
  public void testComputeAclConditions() {
    Configuration c = _cb.build();
    IpAccessList aclWithoutLines = _aclb.setOwner(c).build();
    _acllb.setAction(LineAction.ACCEPT);
    IpAccessList aclWithLines =
        _aclb
            .setLines(
                ImmutableList.<IpAccessListLine>of(
                    _acllb.setDstIps(ImmutableSet.of(new IpWildcard(new Ip("1.2.3.4")))).build(),
                    _acllb.setDstIps(ImmutableSet.of(new IpWildcard(new Ip("5.6.7.8")))).build()))
            .build();
    SynthesizerInput input =
        _inputBuilder.setConfigurations(ImmutableMap.of(c.getName(), c)).build();

    assertThat(
        input,
        hasAclConditions(
            equalTo(
                ImmutableMap.of(
                    c.getName(),
                    ImmutableMap.of(
                        aclWithoutLines.getName(),
                        ImmutableMap.of(),
                        aclWithLines.getName(),
                        ImmutableMap.of(
                            0,
                            new HeaderSpaceMatchExpr(aclWithLines.getLines().get(0)),
                            1,
                            new HeaderSpaceMatchExpr(aclWithLines.getLines().get(1))))))));

    Configuration srcNode = _cb.build();
    Configuration nextHop = _cb.build();
    Vrf srcVrf = _vb.setOwner(srcNode).build();
    Vrf nextHopVrf = _vb.setOwner(nextHop).build();
    Ip ip11 = new Ip("1.0.0.0");
    Ip ip12 = new Ip("1.0.0.10");
    Ip ip21 = new Ip("2.0.0.0");
    Ip ip22 = new Ip("2.0.0.10");
    IpAccessList sourceNat1Acl = _aclb.setLines(ImmutableList.of()).setOwner(srcNode).build();
    IpAccessList sourceNat2Acl = _aclb.build();
    SourceNat sourceNat1 =
        _snb.setPoolIpFirst(ip11).setPoolIpLast(ip12).setAcl(sourceNat1Acl).build();
    SourceNat sourceNat2 =
        _snb.setPoolIpFirst(ip21).setPoolIpLast(ip22).setAcl(sourceNat2Acl).build();
    Interface srcInterfaceZeroSourceNats =
        _ib.setOwner(srcNode).setVrf(srcVrf).setSourceNats(ImmutableList.of()).build();
    Interface srcInterfaceOneSourceNat = _ib.setSourceNats(ImmutableList.of(sourceNat1)).build();
    Interface srcInterfaceTwoSourceNats =
        _ib.setSourceNats(ImmutableList.of(sourceNat1, sourceNat2)).build();
    Interface nextHopInterface =
        _ib.setOwner(nextHop).setVrf(nextHopVrf).setSourceNats(ImmutableList.of()).build();
    Edge forwardEdge1 = new Edge(srcInterfaceZeroSourceNats, nextHopInterface);
    Edge forwardEdge2 = new Edge(srcInterfaceOneSourceNat, nextHopInterface);
    Edge forwardEdge3 = new Edge(srcInterfaceTwoSourceNats, nextHopInterface);
    Edge backEdge1 = new Edge(nextHopInterface, srcInterfaceZeroSourceNats);
    Edge backEdge2 = new Edge(nextHopInterface, srcInterfaceOneSourceNat);
    Edge backEdge3 = new Edge(nextHopInterface, srcInterfaceTwoSourceNats);
    SynthesizerInput inputWithDataPlane =
        _inputBuilder
            .setConfigurations(
                ImmutableMap.of(srcNode.getName(), srcNode, nextHop.getName(), nextHop))
            .setDataPlane(
                TestDataPlane.builder()
                    .setTopologyEdges(
                        ImmutableSortedSet.of(
                            forwardEdge1,
                            forwardEdge2,
                            forwardEdge3,
                            backEdge1,
                            backEdge2,
                            backEdge3))
                    .build())
            .build();
    assertThat(
        inputWithDataPlane,
        hasAclConditions(
            equalTo(
                ImmutableMap.of(
                    srcNode.getName(),
                    ImmutableMap.of(
                        sourceNat1Acl.getName(), ImmutableMap.of(),
                        sourceNat2Acl.getName(), ImmutableMap.of()),
                    nextHop.getName(),
                    ImmutableMap.of()))));
  }

  @Test
  public void testComputeEnabledEdges() {
    Configuration srcNode = _cb.build();
    Configuration nextHop = _cb.build();
    Vrf srcVrf = _vb.setOwner(srcNode).build();
    Vrf nextHopVrf = _vb.setOwner(nextHop).build();
    Interface srcInterface = _ib.setOwner(srcNode).setVrf(srcVrf).build();
    Interface nextHopInterface = _ib.setOwner(nextHop).setVrf(nextHopVrf).build();
    Interface disabledNextHopInterface = _ib.setActive(false).build();
    Edge expectedEnabledEdge = new Edge(srcInterface, nextHopInterface);
    Edge expectedDisabledEdge = new Edge(srcInterface, disabledNextHopInterface);
    SynthesizerInput inputWithoutDataPlane =
        _inputBuilder
            .setConfigurations(
                ImmutableMap.of(srcNode.getName(), srcNode, nextHop.getName(), nextHop))
            .build();
    SynthesizerInput inputWithDataPlane =
        _inputBuilder
            .setDataPlane(
                TestDataPlane.builder()
                    .setTopologyEdges(
                        ImmutableSortedSet.of(expectedEnabledEdge, expectedDisabledEdge))
                    .build())
            .build();

    assertThat(inputWithDataPlane, hasEnabledEdges(hasItem(expectedEnabledEdge)));
    assertThat(inputWithDataPlane, hasEnabledEdges(not(hasItem(expectedDisabledEdge))));
    assertThat(inputWithDataPlane, hasEnabledEdges(not(hasItem(expectedDisabledEdge))));
    assertThat(inputWithoutDataPlane, hasEnabledEdges(nullValue()));
  }

  @Test
  public void testComputeEnabledFlowSinks() {
    Configuration cEnabled = _cb.build();
    Configuration cDisabled = _cb.build();
    Vrf vEnabled = _vb.setOwner(cEnabled).build();
    // Enabled but not flow sink. Should not appear in enabledFlowSinks.
    _ib.setOwner(cEnabled).setVrf(vEnabled).build();
    Interface iEnabledAndFlowSink = _ib.build();
    Interface iDisabledViaInactiveAndFlowSink = _ib.setActive(false).build();
    SynthesizerInput inputWithoutDataPlane =
        _inputBuilder
            .setConfigurations(
                ImmutableMap.of(cEnabled.getName(), cEnabled, cDisabled.getName(), cDisabled))
            .build();
    SynthesizerInput inputWithDataPlane =
        _inputBuilder
            .setDataPlane(
                TestDataPlane.builder()
                    .setFlowSinks(
                        ImmutableSet.of(
                            new NodeInterfacePair(
                                cEnabled.getName(), iEnabledAndFlowSink.getName()),
                            new NodeInterfacePair(
                                cEnabled.getName(), iDisabledViaInactiveAndFlowSink.getName())))
                    .build())
            .build();

    assertThat(
        inputWithDataPlane,
        hasEnabledFlowSinks(
            equalTo(
                ImmutableSet.of(
                    new NodeInterfacePair(cEnabled.getName(), iEnabledAndFlowSink.getName())))));
    assertThat(inputWithoutDataPlane, hasEnabledFlowSinks(nullValue()));
  }

  @Test
  public void testComputeEnabledInterfaces() {
    Configuration cEnabled = _cb.build();
    Configuration cDisabled = _cb.build();
    Vrf vEnabled = _vb.setOwner(cEnabled).build();
    Vrf vDisabledViaVrf = _vb.build();
    Vrf vDisabledViaNode = _vb.setOwner(cDisabled).build();
    Interface iEnabled = _ib.setOwner(cEnabled).setVrf(vEnabled).build();
    Interface iDisabledViaInactive = _ib.setActive(false).build();
    Interface iDisabledViaBlacklisted = _ib.setActive(true).setBlacklisted(true).build();
    Interface iDisabledViaInterface = _ib.setBlacklisted(false).build();
    Interface iDisabledViaVrf = _ib.setVrf(vDisabledViaVrf).build();
    // interface disabled via disabledNodes
    _ib.setOwner(cDisabled).setVrf(vDisabledViaNode).build();
    SynthesizerInput input =
        _inputBuilder
            .setConfigurations(
                ImmutableMap.of(cEnabled.getName(), cEnabled, cDisabled.getName(), cDisabled))
            .setDisabledNodes(ImmutableSet.of(cDisabled.getName()))
            .setDisabledVrfs(
                ImmutableMap.of(cEnabled.getName(), ImmutableSet.of(vDisabledViaVrf.getName())))
            .setDisabledInterfaces(
                ImmutableMap.of(
                    cEnabled.getName(), ImmutableSet.of(iDisabledViaInterface.getName())))
            .build();

    assertThat(
        input,
        hasEnabledInterfaces(hasEntry(equalTo(cEnabled.getName()), hasItem(iEnabled.getName()))));
    assertThat(
        input,
        hasEnabledInterfaces(
            hasEntry(equalTo(cEnabled.getName()), not(hasItem(iDisabledViaInactive.getName())))));
    assertThat(
        input,
        hasEnabledInterfaces(
            hasEntry(
                equalTo(cEnabled.getName()), not(hasItem(iDisabledViaBlacklisted.getName())))));
    assertThat(
        input,
        hasEnabledInterfaces(
            hasEntry(equalTo(cEnabled.getName()), not(hasItem(iDisabledViaInterface.getName())))));
    assertThat(
        input,
        hasEnabledInterfaces(
            hasEntry(equalTo(cEnabled.getName()), not(hasItem(iDisabledViaVrf.getName())))));
    assertThat(input, hasEnabledInterfaces(not(hasKey(cDisabled.getName()))));
  }

  @Test
  public void testComputeEnabledNodes() {
    Configuration cEnabled = _cb.build();
    Configuration cDisabled = _cb.build();
    SynthesizerInput input =
        _inputBuilder
            .setConfigurations(
                ImmutableMap.of(cEnabled.getName(), cEnabled, cDisabled.getName(), cDisabled))
            .setDisabledNodes(ImmutableSet.of(cDisabled.getName()))
            .build();

    assertThat(input, hasEnabledNodes(hasItem(cEnabled.getName())));
    assertThat(input, hasEnabledNodes(not(hasItem(cDisabled.getName()))));
  }

  @Test
  public void testComputeEnabledVrfs() {
    Configuration cEnabled = _cb.build();
    Configuration cDisabled = _cb.build();
    Vrf vEnabled = _vb.setOwner(cEnabled).build();
    Vrf vDisabledViaVrf = _vb.build();
    // disabled via disabledNodes
    _vb.setOwner(cDisabled).build();
    SynthesizerInput input =
        _inputBuilder
            .setConfigurations(
                ImmutableMap.of(cEnabled.getName(), cEnabled, cDisabled.getName(), cDisabled))
            .setDisabledNodes(ImmutableSet.of(cDisabled.getName()))
            .setDisabledVrfs(
                ImmutableMap.of(cEnabled.getName(), ImmutableSet.of(vDisabledViaVrf.getName())))
            .build();

    assertThat(
        input, hasEnabledVrfs(hasEntry(equalTo(cEnabled.getName()), hasItem(vEnabled.getName()))));
    assertThat(
        input,
        hasEnabledVrfs(
            hasEntry(equalTo(cEnabled.getName()), not(hasItem(vDisabledViaVrf.getName())))));
    assertThat(input, hasEnabledVrfs(not(hasKey(cDisabled.getName()))));
  }

  @Test
  public void testComputeFibConditions() {
    Configuration srcNode = _cb.build();
    Vrf srcVrf = _vb.setOwner(srcNode).build();
    Interface srcInterface = _ib.setOwner(srcNode).setVrf(srcVrf).build();
    String nextHop1 = "nextHop1";
    String nextHopInterface1 = "nextHopInterface1";
    String nextHop2 = "nextHop2";
    String nextHopInterface2 = "nextHopInterface2";
    Prefix p1 = Prefix.parse("1.2.3.0/24");
    Prefix p2 = Prefix.parse("3.4.5.0/24");
    FibRow iEnabledFibRow11 = new FibRow(p1, srcInterface.getName(), nextHop1, nextHopInterface1);
    FibRow iEnabledFibRow12 = new FibRow(p2, srcInterface.getName(), nextHop1, nextHopInterface1);
    FibRow iEnabledFibRow21 = new FibRow(p1, srcInterface.getName(), nextHop2, nextHopInterface2);
    FibRow iEnabledFibRow22 = new FibRow(p2, srcInterface.getName(), nextHop2, nextHopInterface2);
    FibRow defaultDropFibRow = new FibRow(Prefix.ZERO, FibRow.DROP_NO_ROUTE, "", "");
    SortedSet<FibRow> fibs =
        ImmutableSortedSet.of(
            iEnabledFibRow11,
            iEnabledFibRow12,
            iEnabledFibRow21,
            iEnabledFibRow22,
            defaultDropFibRow);
    SynthesizerInput inputWithoutDataPlane =
        _inputBuilder.setConfigurations(ImmutableMap.of(srcNode.getName(), srcNode)).build();
    SynthesizerInput inputWithDataPlane =
        _inputBuilder
            .setDataPlane(
                TestDataPlane.builder()
                    .setFibRows(
                        ImmutableMap.of(srcNode.getName(), ImmutableMap.of(srcVrf.getName(), fibs)))
                    .build())
            .build();

    assertThat(
        inputWithDataPlane,
        hasFibConditions(
            equalTo(
                ImmutableMap.of(
                    srcNode.getName(),
                    ImmutableMap.of(
                        srcVrf.getName(),
                        ImmutableMap.of(
                            srcInterface.getName(),
                            ImmutableMap.of(
                                new NodeInterfacePair(nextHop1, nextHopInterface1),
                                new OrExpr(
                                    ImmutableList.of(
                                        FibRowMatchExpr.getFibRowConditions(
                                            srcNode.getName(),
                                            srcVrf.getName(),
                                            ImmutableList.copyOf(fibs),
                                            1,
                                            iEnabledFibRow11),
                                        FibRowMatchExpr.getFibRowConditions(
                                            srcNode.getName(),
                                            srcVrf.getName(),
                                            ImmutableList.copyOf(fibs),
                                            3,
                                            iEnabledFibRow12))),
                                new NodeInterfacePair(nextHop2, nextHopInterface2),
                                new OrExpr(
                                    ImmutableList.of(
                                        FibRowMatchExpr.getFibRowConditions(
                                            srcNode.getName(),
                                            srcVrf.getName(),
                                            ImmutableList.copyOf(fibs),
                                            2,
                                            iEnabledFibRow21),
                                        FibRowMatchExpr.getFibRowConditions(
                                            srcNode.getName(),
                                            srcVrf.getName(),
                                            ImmutableList.copyOf(fibs),
                                            4,
                                            iEnabledFibRow22)))),
                            FibRow.DROP_NO_ROUTE,
                            ImmutableMap.of(
                                new NodeInterfacePair("", ""),
                                new OrExpr(
                                    ImmutableList.of(
                                        FibRowMatchExpr.getFibRowConditions(
                                            srcNode.getName(),
                                            srcVrf.getName(),
                                            ImmutableList.copyOf(fibs),
                                            0,
                                            defaultDropFibRow))))))))));
    assertThat(inputWithoutDataPlane, hasFibConditions(nullValue()));
  }

  @Test
  public void testComputeIpsByHostname() {
    Configuration c = _cb.build();
    Vrf v = _vb.setOwner(c).build();
    // Enabled but not flow sink. Should not appear in enabledFlowSinks.
    Ip ipEnabled1 = new Ip("1.1.1.1");
    Ip ipEnabled2 = new Ip("2.2.2.2");
    Ip ipDisabled = new Ip("3.3.3.3");
    // enabledInterface1
    _ib.setOwner(c)
        .setVrf(v)
        .setAddress(new InterfaceAddress(ipEnabled1, Prefix.MAX_PREFIX_LENGTH))
        .build();
    // enabledInterface1
    _ib.setAddress(new InterfaceAddress(ipEnabled2, Prefix.MAX_PREFIX_LENGTH)).build();
    // disabledInterface
    _ib.setAddress(new InterfaceAddress(ipDisabled, Prefix.MAX_PREFIX_LENGTH))
        .setActive(false)
        .build();
    SynthesizerInput inputWithoutDataPlane =
        _inputBuilder.setConfigurations(ImmutableMap.of(c.getName(), c)).build();
    SynthesizerInput inputWithDataPlane =
        _inputBuilder.setDataPlane(TestDataPlane.builder().build()).build();

    assertThat(inputWithoutDataPlane, hasIpsByHostname(nullValue()));
    assertThat(
        inputWithDataPlane,
        hasIpsByHostname(
            equalTo(ImmutableMap.of(c.getName(), ImmutableSet.of(ipEnabled1, ipEnabled2)))));
  }

  @Test
  public void testComputeSourceNats() {
    Configuration srcNode = _cb.build();
    Configuration nextHop = _cb.build();
    Vrf srcVrf = _vb.setOwner(srcNode).build();
    Vrf nextHopVrf = _vb.setOwner(nextHop).build();
    Ip ip11 = new Ip("1.0.0.0");
    Ip ip12 = new Ip("1.0.0.10");
    Ip ip21 = new Ip("2.0.0.0");
    Ip ip22 = new Ip("2.0.0.10");
    IpAccessList sourceNat1Acl = _aclb.setLines(ImmutableList.of()).setOwner(srcNode).build();
    IpAccessList sourceNat2Acl = _aclb.build();
    SourceNat sourceNat1 =
        _snb.setPoolIpFirst(ip11).setPoolIpLast(ip12).setAcl(sourceNat1Acl).build();
    SourceNat sourceNat2 =
        _snb.setPoolIpFirst(ip21).setPoolIpLast(ip22).setAcl(sourceNat2Acl).build();
    Interface srcInterfaceZeroSourceNats =
        _ib.setOwner(srcNode).setVrf(srcVrf).setSourceNats(ImmutableList.of()).build();
    Interface srcInterfaceOneSourceNat = _ib.setSourceNats(ImmutableList.of(sourceNat1)).build();
    Interface srcInterfaceTwoSourceNats =
        _ib.setSourceNats(ImmutableList.of(sourceNat1, sourceNat2)).build();
    Interface nextHopInterface =
        _ib.setOwner(nextHop).setVrf(nextHopVrf).setSourceNats(ImmutableList.of()).build();
    Edge forwardEdge1 = new Edge(srcInterfaceZeroSourceNats, nextHopInterface);
    Edge forwardEdge2 = new Edge(srcInterfaceOneSourceNat, nextHopInterface);
    Edge forwardEdge3 = new Edge(srcInterfaceTwoSourceNats, nextHopInterface);
    Edge backEdge1 = new Edge(nextHopInterface, srcInterfaceZeroSourceNats);
    Edge backEdge2 = new Edge(nextHopInterface, srcInterfaceOneSourceNat);
    Edge backEdge3 = new Edge(nextHopInterface, srcInterfaceTwoSourceNats);
    SynthesizerInput inputWithoutDataPlane =
        _inputBuilder
            .setConfigurations(
                ImmutableMap.of(srcNode.getName(), srcNode, nextHop.getName(), nextHop))
            .build();
    SynthesizerInput inputWithDataPlane =
        _inputBuilder
            .setDataPlane(
                TestDataPlane.builder()
                    .setTopologyEdges(
                        ImmutableSortedSet.of(
                            forwardEdge1,
                            forwardEdge2,
                            forwardEdge3,
                            backEdge1,
                            backEdge2,
                            backEdge3))
                    .build())
            .build();

    assertThat(
        inputWithDataPlane,
        hasSourceNats(
            hasEntry(
                equalTo(srcNode.getName()),
                hasEntry(
                    equalTo(srcInterfaceZeroSourceNats.getName()), equalTo(ImmutableList.of())))));
    assertThat(
        inputWithDataPlane,
        hasSourceNats(
            hasEntry(
                equalTo(srcNode.getName()),
                hasEntry(
                    equalTo(srcInterfaceOneSourceNat.getName()),
                    equalTo(
                        ImmutableList.of(
                            immutableEntry(
                                Optional.of(
                                    new AclPermit(srcNode.getName(), sourceNat1Acl.getName())),
                                new RangeMatchExpr(
                                    TransformationHeaderField.NEW_SRC_IP,
                                    TransformationHeaderField.NEW_SRC_IP.getSize(),
                                    ImmutableSet.of(
                                        Range.closed(ip11.asLong(), ip12.asLong()))))))))));
    assertThat(
        inputWithDataPlane,
        hasSourceNats(
            hasEntry(
                equalTo(srcNode.getName()),
                hasEntry(
                    equalTo(srcInterfaceTwoSourceNats.getName()),
                    equalTo(
                        ImmutableList.of(
                            immutableEntry(
                                Optional.of(
                                    new AclPermit(srcNode.getName(), sourceNat1Acl.getName())),
                                new RangeMatchExpr(
                                    TransformationHeaderField.NEW_SRC_IP,
                                    TransformationHeaderField.NEW_SRC_IP.getSize(),
                                    ImmutableSet.of(Range.closed(ip11.asLong(), ip12.asLong())))),
                            immutableEntry(
                                Optional.of(
                                    new AclPermit(srcNode.getName(), sourceNat2Acl.getName())),
                                new RangeMatchExpr(
                                    TransformationHeaderField.NEW_SRC_IP,
                                    TransformationHeaderField.NEW_SRC_IP.getSize(),
                                    ImmutableSet.of(
                                        Range.closed(ip21.asLong(), ip22.asLong()))))))))));
    assertThat(inputWithoutDataPlane, hasSourceNats(nullValue()));
  }

  @Test
  public void testComputeTopologyInterfaces() {
    Configuration srcNode = _cb.build();
    Configuration nextHop = _cb.build();
    Vrf srcVrf = _vb.setOwner(srcNode).build();
    Vrf nextHopVrf = _vb.setOwner(nextHop).build();
    Interface srcInterface = _ib.setOwner(srcNode).setVrf(srcVrf).build();
    Interface iFlowSink = _ib.build();
    Interface iNoEdge = _ib.build();
    Interface nextHopInterface = _ib.setOwner(nextHop).setVrf(nextHopVrf).build();
    Edge forwardEdge = new Edge(srcInterface, nextHopInterface);
    Edge backEdge = new Edge(nextHopInterface, srcInterface);
    SynthesizerInput inputWithoutDataPlane =
        _inputBuilder
            .setConfigurations(
                ImmutableMap.of(srcNode.getName(), srcNode, nextHop.getName(), nextHop))
            .build();
    SynthesizerInput inputWithDataPlane =
        _inputBuilder
            .setDataPlane(
                TestDataPlane.builder()
                    .setFlowSinks(
                        ImmutableSet.of(
                            new NodeInterfacePair(srcNode.getName(), iFlowSink.getName())))
                    .setTopologyEdges(ImmutableSortedSet.of(forwardEdge, backEdge))
                    .build())
            .build();

    assertThat(
        inputWithDataPlane,
        hasTopologyInterfaces(
            hasEntry(equalTo(srcNode.getName()), hasItem(srcInterface.getName()))));
    assertThat(
        inputWithDataPlane,
        hasTopologyInterfaces(
            hasEntry(equalTo(srcNode.getName()), not(hasItem(iNoEdge.getName())))));
    assertThat(
        inputWithDataPlane,
        hasTopologyInterfaces(
            hasEntry(equalTo(nextHop.getName()), hasItem(nextHopInterface.getName()))));
    assertThat(inputWithoutDataPlane, hasTopologyInterfaces(nullValue()));
  }
}

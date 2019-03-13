package org.batfish.datamodel;

import static org.batfish.datamodel.ForwardingAnalysisImpl.computeArpFalseDestIp;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeArpFalseNextHopIp;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeArpReplies;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeArpTrueEdge;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeArpTrueEdgeDestIp;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeArpTrueEdgeNextHopIp;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeDeliveredToSubnet;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeExitsNetwork;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeFibInterfaces;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeInsufficientInfo;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeInterfaceArpReplies;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeInterfaceHostSubnetIps;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeIpsAssignedToThisInterface;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeIpsRoutedOutInterfaces;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeMatchingIps;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeNeighborUnreachable;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeRoutesWhereDstIpCanBeArpIp;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeRoutesWithDestIpEdge;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeRoutesWithNextHopIpArpFalse;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeRoutesWithNextHopIpArpFalseForInterface;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeRoutesWithNextHopIpArpTrue;
import static org.batfish.datamodel.ForwardingAnalysisImpl.computeSomeoneReplies;
import static org.batfish.datamodel.ForwardingAnalysisImpl.union;
import static org.batfish.datamodel.matchers.AclIpSpaceMatchers.hasLines;
import static org.batfish.datamodel.matchers.AclIpSpaceMatchers.isAclIpSpaceThat;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.topology.TopologyUtil;
import org.junit.Before;
import org.junit.Test;

public class ForwardingAnalysisImplTest {

  private static final String CONFIG1 = "config1";

  private static final String VRF1 = "vrf1";

  private static final String INTERFACE1 = "interface1";

  private static final String INTERFACE2 = "interface2";

  private static final IpSpace IPSPACE1 = new MockIpSpace(1);

  private static final IpSpace IPSPACE2 = new MockIpSpace(2);

  private static final Prefix P1 = Prefix.parse("1.0.0.0/8");

  private static final Prefix P2 = Prefix.parse("2.0.0.0/16");

  private static final Prefix P3 = Prefix.parse("3.0.0.0/24");

  private Configuration.Builder _cb;

  private Interface.Builder _ib;

  private Vrf.Builder _vb;

  @Before
  public void setup() {
    NetworkFactory nf = new NetworkFactory();
    _cb = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _vb = nf.vrfBuilder();
    _ib = nf.interfaceBuilder();
  }

  @Test
  public void testComputeArpReplies() {
    Configuration c1 = _cb.build();
    Configuration c2 = _cb.build();
    Map<String, Configuration> configs =
        ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    Vrf vrf1 = _vb.setOwner(c1).build();
    Vrf vrf2 = _vb.setOwner(c2).build();
    Interface i1 =
        _ib.setOwner(c1)
            .setVrf(vrf1)
            .setAddress(new InterfaceAddress(P1.getStartIp(), P1.getPrefixLength()))
            .setProxyArp(true)
            .build();
    Interface i2 =
        _ib.setOwner(c2)
            .setVrf(vrf2)
            .setAddress(new InterfaceAddress(P2.getStartIp(), P2.getPrefixLength()))
            .setProxyArp(false)
            .build();
    Ip additionalIp = Ip.parse("10.10.10.1");
    i1.setAdditionalArpIps(additionalIp.toIpSpace());
    i2.setAdditionalArpIps(additionalIp.toIpSpace());
    IpSpace ipsRoutedOutI1 =
        IpWildcardSetIpSpace.builder().including(new IpWildcard(P1), new IpWildcard(P3)).build();
    IpSpace ipsRoutedOutI2 = IpWildcardSetIpSpace.builder().including(new IpWildcard(P2)).build();
    Map<String, Configuration> configurations =
        ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    Map<String, Map<String, IpSpace>> routableIps =
        ImmutableMap.of(
            c1.getHostname(),
            ImmutableMap.of(vrf1.getName(), UniverseIpSpace.INSTANCE),
            c2.getHostname(),
            ImmutableMap.of(vrf2.getName(), UniverseIpSpace.INSTANCE));
    Map<String, Map<String, IpSpace>> ipsRoutedOutInterfaces =
        ImmutableMap.of(
            c1.getHostname(),
            ImmutableMap.of(i1.getName(), ipsRoutedOutI1),
            c2.getHostname(),
            ImmutableMap.of(i2.getName(), ipsRoutedOutI2));
    Map<String, Map<String, Set<Ip>>> interfaceOwnedIps =
        TopologyUtil.computeInterfaceOwnedIps(configs, false);
    Map<String, Map<String, IpSpace>> result =
        computeArpReplies(configurations, ipsRoutedOutInterfaces, interfaceOwnedIps, routableIps);

    /* Proxy-arp: Match interface IP, reject what's routed through i1, accept everything else*/
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(equalTo(i1.getName()), containsIp(P1.getStartIp()))));
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(equalTo(i1.getName()), not(containsIp(P1.getEndIp())))));
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(equalTo(i1.getName()), not(containsIp(P3.getStartIp())))));
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(equalTo(i1.getName()), containsIp(P2.getStartIp()))));
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()), hasEntry(equalTo(i1.getName()), containsIp(additionalIp))));
    /* No proxy-arp: just match interface ip and additional arp ip */
    assertThat(
        result,
        hasEntry(
            equalTo(c2.getHostname()),
            hasEntry(equalTo(i2.getName()), containsIp(P2.getStartIp()))));
    assertThat(
        result,
        hasEntry(
            equalTo(c2.getHostname()),
            hasEntry(equalTo(i2.getName()), not(containsIp(P2.getEndIp())))));
    assertThat(
        result,
        hasEntry(
            equalTo(c2.getHostname()),
            hasEntry(equalTo(i2.getName()), not(containsIp(P3.getStartIp())))));
    assertThat(
        result,
        hasEntry(
            equalTo(c2.getHostname()),
            hasEntry(equalTo(i2.getName()), not(containsIp(P1.getStartIp())))));
    assertThat(
        result,
        hasEntry(
            equalTo(c2.getHostname()), hasEntry(equalTo(i2.getName()), containsIp(additionalIp))));
  }

  @Test
  public void testComputeArpRepliesByInterface() {
    Configuration config = _cb.build();
    _ib.setOwner(config);
    _vb.setOwner(config);
    Vrf vrf1 = _vb.build();
    Vrf vrf2 = _vb.build();
    Interface i1 =
        _ib.setVrf(vrf1)
            .setAddress(new InterfaceAddress(P1.getStartIp(), P1.getPrefixLength()))
            .setProxyArp(true)
            .build();
    Interface i2 =
        _ib.setVrf(vrf2)
            .setAddress(new InterfaceAddress(P2.getStartIp(), P2.getPrefixLength()))
            .setProxyArp(false)
            .build();
    Interface i3 = _ib.setAddress(null).setProxyArp(true).build();
    IpSpace ipsRoutedOutI1 =
        IpWildcardSetIpSpace.builder().including(new IpWildcard(P1), new IpWildcard(P3)).build();
    IpSpace ipsRoutedOutI2 = IpWildcardSetIpSpace.builder().including(new IpWildcard(P2)).build();
    IpSpace ipsRoutedOutI3 = EmptyIpSpace.INSTANCE;
    Map<String, Interface> interfaces =
        ImmutableMap.of(i1.getName(), i1, i2.getName(), i2, i3.getName(), i3);
    Map<String, IpSpace> routableIpsByVrf =
        ImmutableMap.of(
            vrf1.getName(), UniverseIpSpace.INSTANCE, vrf2.getName(), UniverseIpSpace.INSTANCE);
    Map<String, IpSpace> ipsRoutedOutInterfaces =
        ImmutableMap.of(
            i1.getName(),
            ipsRoutedOutI1,
            i2.getName(),
            ipsRoutedOutI2,
            i3.getName(),
            ipsRoutedOutI3);

    i1.setAdditionalArpIps(new IpIpSpace(Ip.parse("10.10.10.1")));
    i2.setAdditionalArpIps(new IpIpSpace(Ip.parse("10.10.10.2")));
    i3.setAdditionalArpIps(new IpIpSpace(Ip.parse("10.10.10.3")));

    Map<String, Configuration> configs = ImmutableMap.of(config.getHostname(), config);
    Map<String, Map<String, Set<Ip>>> interfaceOwnedIps =
        TopologyUtil.computeInterfaceOwnedIps(configs, false);
    Map<String, IpSpace> result =
        ForwardingAnalysisImpl.computeArpRepliesByInterface(
            interfaces, routableIpsByVrf, ipsRoutedOutInterfaces, interfaceOwnedIps);

    /* Proxy-arp: Match interface IP, reject what's routed through i1, accept everything else*/
    assertThat(result, hasEntry(equalTo(i1.getName()), containsIp(P1.getStartIp())));
    assertThat(result, hasEntry(equalTo(i1.getName()), not(containsIp(P1.getEndIp()))));
    assertThat(result, hasEntry(equalTo(i1.getName()), not(containsIp(P3.getStartIp()))));
    assertThat(result, hasEntry(equalTo(i1.getName()), containsIp(P2.getStartIp())));
    assertThat(result, hasEntry(equalTo(i1.getName()), containsIp(Ip.parse("10.10.10.1"))));
    /* No proxy-arp: just match interface ip and additional arp ip */
    assertThat(result, hasEntry(equalTo(i2.getName()), containsIp(P2.getStartIp())));
    assertThat(result, hasEntry(equalTo(i2.getName()), not(containsIp(P2.getEndIp()))));
    assertThat(result, hasEntry(equalTo(i2.getName()), not(containsIp(P3.getStartIp()))));
    assertThat(result, hasEntry(equalTo(i2.getName()), not(containsIp(P1.getStartIp()))));
    assertThat(result, hasEntry(equalTo(i2.getName()), containsIp(Ip.parse("10.10.10.2"))));
    /* No interface IPs: reject everything */
    assertThat(result, hasEntry(equalTo(i3.getName()), equalTo(EmptyIpSpace.INSTANCE)));
  }

  @Test
  public void testComputeArpReplies_VRRP() {
    Configuration c = _cb.build();
    Map<String, Configuration> configs = ImmutableMap.of(c.getHostname(), c);
    _ib.setOwner(c);
    Vrf vrf1 = _vb.build();
    Vrf vrf2 = _vb.build();
    Interface i1 =
        _ib.setVrf(vrf1)
            .setAddress(new InterfaceAddress(P1.getStartIp(), P1.getPrefixLength()))
            .setVrrpGroups(
                ImmutableSortedMap.of(
                    1,
                    VrrpGroup.builder()
                        .setName(1)
                        .setPriority(100)
                        .setVirtualAddress(new InterfaceAddress("1.1.1.1/32"))
                        .build()))
            .build();

    Interface i2 =
        _ib.setVrf(vrf2)
            .setAddress(new InterfaceAddress(P1.getEndIp(), P1.getPrefixLength()))
            .setVrrpGroups(
                ImmutableSortedMap.of(
                    1,
                    VrrpGroup.builder()
                        .setName(1)
                        .setPriority(110)
                        .setVirtualAddress(new InterfaceAddress("1.1.1.1/32"))
                        .build()))
            .build();

    Map<String, Map<String, Set<Ip>>> interfaceOwnedIps =
        TopologyUtil.computeInterfaceOwnedIps(configs, false);

    IpSpace p1IpSpace = new IpWildcard(P1).toIpSpace();
    IpSpace i1ArpReplies =
        computeInterfaceArpReplies(i1, UniverseIpSpace.INSTANCE, p1IpSpace, interfaceOwnedIps);
    IpSpace i2ArpReplies =
        computeInterfaceArpReplies(i2, UniverseIpSpace.INSTANCE, p1IpSpace, interfaceOwnedIps);

    assertThat(i1ArpReplies, not(containsIp(Ip.parse("1.1.1.1"))));
    assertThat(i2ArpReplies, containsIp(Ip.parse("1.1.1.1")));
  }

  @Test
  public void testComputeArpTrueEdge() {
    IpSpace nextHopIpSpace = new MockIpSpace(1);
    IpSpace dstIpSpace = new MockIpSpace(2);
    Edge e1 = Edge.of("c1", "i1", "c2", "i2");
    Map<String, Map<String, Map<Edge, IpSpace>>> arpTrueEdgeDestIp =
        ImmutableMap.of("c1", ImmutableMap.of("v1", ImmutableMap.of(e1, dstIpSpace)));
    Map<String, Map<String, Map<Edge, IpSpace>>> arpTrueEdgeNextHopIp =
        ImmutableMap.of("c1", ImmutableMap.of("v1", ImmutableMap.of(e1, nextHopIpSpace)));
    Map<String, Map<String, Map<Edge, IpSpace>>> result =
        computeArpTrueEdge(arpTrueEdgeDestIp, arpTrueEdgeNextHopIp);

    assertThat(
        result,
        hasEntry(
            equalTo("c1"),
            hasEntry(
                equalTo("v1"),
                hasEntry(
                    equalTo(e1),
                    isAclIpSpaceThat(
                        hasLines(
                            containsInAnyOrder(
                                AclIpSpaceLine.permit(nextHopIpSpace),
                                AclIpSpaceLine.permit(dstIpSpace))))))));
  }

  @Test
  public void testComputeArpTrueEdgeDestIp() {
    Configuration c1 = _cb.build();
    Configuration c2 = _cb.build();
    Vrf vrf1 = _vb.setOwner(c1).build();
    Vrf vrf2 = _vb.setOwner(c2).build();
    Interface i1 = _ib.setOwner(c1).setVrf(vrf1).build();
    Ip i2Ip = Ip.create(P1.getStartIp().asLong() + 1);
    Interface i2 = _ib.setOwner(c2).setVrf(vrf2).build();
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1.getHostname(),
            ImmutableMap.of(
                vrf1.getName(),
                MockFib.builder()
                    .setMatchingIps(
                        ImmutableMap.of(
                            P1,
                            AclIpSpace.rejecting(
                                    Prefix.create(P1.getEndIp(), Prefix.MAX_PREFIX_LENGTH)
                                        .toIpSpace())
                                .thenPermitting(P1.toIpSpace())
                                .build()))
                    .build()));
    Edge edge = Edge.of(c1.getHostname(), i1.getName(), c2.getHostname(), i2.getName());
    Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> routesWithDestIpEdge =
        ImmutableMap.of(
            c1.getHostname(),
            ImmutableMap.of(
                vrf1.getName(),
                ImmutableMap.of(edge, ImmutableSet.of(new ConnectedRoute(P1, i1.getName())))));
    Map<String, Map<String, IpSpace>> arpReplies =
        ImmutableMap.of(
            c2.getHostname(),
            ImmutableMap.of(
                i2.getName(),
                AclIpSpace.permitting(i2Ip.toIpSpace())
                    .thenPermitting(P1.getEndIp().toIpSpace())
                    .build()));
    Map<String, Map<String, Map<Edge, IpSpace>>> result =
        computeArpTrueEdgeDestIp(computeMatchingIps(fibs), routesWithDestIpEdge, arpReplies);

    /* Respond to request for IP on i2. */
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(equalTo(vrf1.getName()), hasEntry(equalTo(edge), containsIp(i2Ip)))));
    /* Do not make ARP request for IP matched by more specific route not going out i1.  */
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()), hasEntry(equalTo(edge), not(containsIp(P1.getEndIp()))))));
    /* Do not receive response for IP i2 does not own. */
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()),
                hasEntry(equalTo(edge), not(containsIp(P1.getStartIp()))))));
  }

  @Test
  public void testComputeArpTrueEdgeNextHopIp() {
    Configuration c1 = _cb.build();
    Configuration c2 = _cb.build();
    Vrf vrf1 = _vb.setOwner(c1).build();
    Vrf vrf2 = _vb.setOwner(c2).build();
    Interface i1 =
        _ib.setOwner(c1)
            .setVrf(vrf1)
            .setAddress(new InterfaceAddress(P1.getStartIp(), P1.getPrefixLength()))
            .build();
    Ip i2Ip = Ip.create(P1.getStartIp().asLong() + 1);
    Interface i2 =
        _ib.setOwner(c2)
            .setVrf(vrf2)
            .setAddress(new InterfaceAddress(i2Ip, P1.getPrefixLength()))
            .build();
    Edge edge = Edge.of(c1.getHostname(), i1.getName(), c2.getHostname(), i2.getName());
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1.getHostname(),
            ImmutableMap.of(
                vrf1.getName(),
                MockFib.builder()
                    .setMatchingIps(
                        ImmutableMap.of(
                            P1,
                            AclIpSpace.rejecting(
                                    Prefix.create(P1.getEndIp(), Prefix.MAX_PREFIX_LENGTH)
                                        .toIpSpace())
                                .thenPermitting(P1.toIpSpace())
                                .build()))
                    .build()));
    Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> routesWithNextHopIpArpTrue =
        ImmutableMap.of(
            c1.getHostname(),
            ImmutableMap.of(
                vrf1.getName(),
                ImmutableMap.of(
                    edge,
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNetwork(P1)
                            .setNextHopIp(P2.getStartIp())
                            .setAdministrativeCost(1)
                            .build()))));
    Map<String, Map<String, Map<Edge, IpSpace>>> result =
        computeArpTrueEdgeNextHopIp(computeMatchingIps(fibs), routesWithNextHopIpArpTrue);

    /*
     * Respond for any destination IP in network not matching more specific route not going out i1.
     */
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()), hasEntry(equalTo(edge), containsIp(P1.getStartIp())))));

    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(equalTo(vrf1.getName()), hasEntry(equalTo(edge), containsIp(i2Ip)))));

    /* Do not respond for destination IP matching more specific route not going out i1 */
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()), hasEntry(equalTo(edge), not(containsIp(P1.getEndIp()))))));

    /* Do not respond for destination IPs not matching route */
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()),
                hasEntry(equalTo(edge), not(containsIp(P2.getStartIp()))))));
    assertThat(
        result,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()), hasEntry(equalTo(edge), not(containsIp(P2.getEndIp()))))));
  }

  @Test
  public void testComputeInterfaceArpReplies() {
    Configuration config = _cb.build();
    _ib.setOwner(config);
    InterfaceAddress primary = new InterfaceAddress(P1.getStartIp(), P1.getPrefixLength());
    InterfaceAddress secondary = new InterfaceAddress(P2.getStartIp(), P2.getPrefixLength());
    Interface iNoProxyArp = _ib.setAddresses(primary, secondary).build();
    Interface iProxyArp = _ib.setProxyArp(true).build();
    IpSpace routableIpsForThisVrf = UniverseIpSpace.INSTANCE;
    IpSpace ipsRoutedThroughInterface =
        IpWildcardSetIpSpace.builder().including(new IpWildcard(P1), new IpWildcard(P2)).build();
    Map<String, Map<String, Set<Ip>>> interfaceOwnedIps =
        TopologyUtil.computeInterfaceOwnedIps(ImmutableMap.of(config.getHostname(), config), false);
    IpSpace noProxyArpResult =
        computeInterfaceArpReplies(
            iNoProxyArp, routableIpsForThisVrf, ipsRoutedThroughInterface, interfaceOwnedIps);
    IpSpace proxyArpResult =
        computeInterfaceArpReplies(
            iProxyArp, routableIpsForThisVrf, ipsRoutedThroughInterface, interfaceOwnedIps);

    /* No proxy-ARP */
    /* Accept IPs belonging to interface */
    assertThat(noProxyArpResult, containsIp(P1.getStartIp()));
    assertThat(noProxyArpResult, containsIp(P2.getStartIp()));
    /* Reject all other IPs */
    assertThat(noProxyArpResult, not(containsIp(P1.getEndIp())));
    assertThat(noProxyArpResult, not(containsIp(P2.getEndIp())));
    assertThat(noProxyArpResult, not(containsIp(P3.getStartIp())));

    /* Proxy-ARP */
    /* Accept IPs belonging to interface */
    assertThat(proxyArpResult, containsIp(P1.getStartIp()));
    assertThat(proxyArpResult, containsIp(P2.getStartIp()));
    /* Reject IPs routed through interface */
    assertThat(proxyArpResult, not(containsIp(P1.getEndIp())));
    assertThat(proxyArpResult, not(containsIp(P2.getEndIp())));
    /* Accept all other routable IPs */
    assertThat(proxyArpResult, containsIp(P3.getStartIp()));
  }

  @Test
  public void testComputeIpsAssignedToThisInterface() {
    Configuration config = _cb.build();
    Map<String, Configuration> configs = ImmutableMap.of(config.getHostname(), config);
    _ib.setOwner(config);
    InterfaceAddress primary = new InterfaceAddress(P1.getStartIp(), P1.getPrefixLength());
    InterfaceAddress secondary = new InterfaceAddress(P2.getStartIp(), P2.getPrefixLength());
    Interface i = _ib.setAddresses(primary, secondary).build();
    Map<String, Map<String, Set<Ip>>> interfaceOwnedIps =
        TopologyUtil.computeInterfaceOwnedIps(configs, false);
    IpSpace result = computeIpsAssignedToThisInterface(i, interfaceOwnedIps);

    assertThat(result, containsIp(P1.getStartIp()));
    assertThat(result, containsIp(P2.getStartIp()));
    assertThat(result, not(containsIp(P2.getEndIp())));
  }

  @Test
  public void testComputeIpsRoutedOutInterfaces() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    ConnectedRoute r1 = new ConnectedRoute(P1, i1);
    StaticRoute nullRoute =
        StaticRoute.builder()
            .setNetwork(P2)
            .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
            .setAdministrativeCost(1)
            .build();
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1,
                MockFib.builder()
                    .setMatchingIps(ImmutableMap.of(P1, P1.toIpSpace(), P2, P2.toIpSpace()))
                    .build()));
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1,
                ImmutableMap.of(
                    i1,
                    ImmutableSet.of(r1),
                    Interface.NULL_INTERFACE_NAME,
                    ImmutableSet.of(nullRoute))));
    Map<String, Map<String, IpSpace>> result =
        computeIpsRoutedOutInterfaces(computeMatchingIps(fibs), routesWithNextHop);

    /* Should contain IPs matching the route */
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(i1), containsIp(P1.getStartIp()))));
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(i1), containsIp(P1.getEndIp()))));
    /* Should not contain IP not matching the route */
    assertThat(
        result, hasEntry(equalTo(c1), hasEntry(equalTo(i1), not(containsIp(P2.getStartIp())))));
    /* Null interface should be excluded because we would not be able to tie back to single VRF. */
    assertThat(result, hasEntry(equalTo(c1), not(hasKey(equalTo(Interface.NULL_INTERFACE_NAME)))));
  }

  @Test
  public void testComputeNeighborUnreachableOrExitsNetwork() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, P1.getStartIp().toIpSpace())));
    Map<String, Map<String, Map<String, IpSpace>>> arpFalseNextHopIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, P1.getEndIp().toIpSpace())));
    Map<String, Map<String, Map<String, IpSpace>>> result =
        union(arpFalseDestIp, arpFalseNextHopIp);

    /* Should contain both IPs. */
    assertThat(
        result,
        hasEntry(
            equalTo(c1),
            hasEntry(equalTo(v1), hasEntry(equalTo(i1), containsIp(P1.getStartIp())))));
    assertThat(
        result,
        hasEntry(
            equalTo(c1), hasEntry(equalTo(v1), hasEntry(equalTo(i1), containsIp(P1.getEndIp())))));
    /* Should not contain unrelated IPs. */
    assertThat(
        result,
        hasEntry(
            equalTo(c1),
            hasEntry(equalTo(v1), hasEntry(equalTo(i1), not(containsIp(P2.getEndIp()))))));
  }

  @Test
  public void testComputeArpFalseDestIp() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    AbstractRoute ifaceRoute = new ConnectedRoute(P1, i1);
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1, MockFib.builder().setMatchingIps(ImmutableMap.of(P1, P1.toIpSpace())).build()));
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWhereDstIpCanBeArpIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(ifaceRoute))));
    Map<String, Map<String, IpSpace>> someoneReplies =
        ImmutableMap.of(c1, ImmutableMap.of(i1, P1.getEndIp().toIpSpace()));
    Map<String, Map<String, Map<String, IpSpace>>> result =
        computeArpFalseDestIp(computeMatchingIps(fibs), routesWhereDstIpCanBeArpIp, someoneReplies);

    /* Should contain IP in the route's prefix that sees no reply */
    assertThat(
        result,
        hasEntry(
            equalTo(c1),
            hasEntry(equalTo(v1), hasEntry(equalTo(i1), containsIp(P1.getStartIp())))));
    /* Should not contain IP in the route's prefix that sees reply */
    assertThat(
        result,
        hasEntry(
            equalTo(c1),
            hasEntry(equalTo(v1), hasEntry(equalTo(i1), not(containsIp(P1.getEndIp()))))));

    /* Should not contain other IPs */
    assertThat(
        result,
        hasEntry(
            equalTo(c1),
            hasEntry(equalTo(v1), hasEntry(equalTo(i1), not(containsIp(P2.getEndIp()))))));
  }

  @Test
  public void testComputeArpFalseDestIpNoNeighbors() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    AbstractRoute ifaceRoute = new ConnectedRoute(P1, i1);
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1, MockFib.builder().setMatchingIps(ImmutableMap.of(P1, P1.toIpSpace())).build()));
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWhereDstIpCanBeArpIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(ifaceRoute))));
    Map<String, Map<String, IpSpace>> someoneReplies = ImmutableMap.of();
    Map<String, Map<String, Map<String, IpSpace>>> result =
        computeArpFalseDestIp(computeMatchingIps(fibs), routesWhereDstIpCanBeArpIp, someoneReplies);

    /*
     * Since _someoneReplies is empty, all IPs for which longest-prefix-match route has no
     * next-hop-ip should be in the result space.
     */
    assertThat(
        result,
        hasEntry(
            equalTo(c1),
            hasEntry(equalTo(v1), hasEntry(equalTo(i1), containsIp(P1.getStartIp())))));
    assertThat(
        result,
        hasEntry(
            equalTo(c1), hasEntry(equalTo(v1), hasEntry(equalTo(i1), containsIp(P1.getEndIp())))));

    /* Should not contain other IPs */
    assertThat(
        result,
        hasEntry(
            equalTo(c1),
            hasEntry(equalTo(v1), hasEntry(equalTo(i1), not(containsIp(P2.getEndIp()))))));
  }

  @Test
  public void testComputeNeighborUnreachableArpNextHopIp() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    AbstractRoute r1 =
        StaticRoute.builder()
            .setNetwork(P1)
            .setNextHopIp(P2.getStartIp())
            .setAdministrativeCost(1)
            .build();
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHopIpArpFalse =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1))));
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1, MockFib.builder().setMatchingIps(ImmutableMap.of(P1, P1.toIpSpace())).build()));

    Map<String, Map<String, Map<String, IpSpace>>> result =
        computeArpFalseNextHopIp(computeMatchingIps(fibs), routesWithNextHopIpArpFalse);

    /* IPs matching some route on interface with no response should appear */
    assertThat(
        result,
        hasEntry(
            equalTo(c1),
            hasEntry(equalTo(v1), hasEntry(equalTo(i1), containsIp(P1.getStartIp())))));
    assertThat(
        result,
        hasEntry(
            equalTo(c1), hasEntry(equalTo(v1), hasEntry(equalTo(i1), containsIp(P1.getEndIp())))));
    /* Other IPs should not appear */
    assertThat(
        result,
        hasEntry(
            equalTo(c1),
            hasEntry(equalTo(v1), hasEntry(equalTo(i1), not(containsIp(P2.getStartIp()))))));
  }

  @Test
  public void testComputeNullRoutedIps() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    AbstractRoute nullRoute =
        StaticRoute.builder()
            .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
            .setNetwork(P1)
            .setAdministrativeCost(1)
            .build();
    AbstractRoute otherRoute = new ConnectedRoute(P2, i1);
    Map<String, Map<String, Fib>> fibs =
        ImmutableSortedMap.of(
            c1,
            ImmutableSortedMap.of(
                v1,
                MockFib.builder()
                    .setMatchingIps(
                        ImmutableMap.of(
                            P1,
                            AclIpSpace.permitting(P1.toIpSpace()).build(),
                            P2,
                            AclIpSpace.permitting(P2.toIpSpace()).build()))
                    .setNextHopInterfaces(
                        ImmutableMap.of(
                            nullRoute,
                            ImmutableMap.of(
                                Interface.NULL_INTERFACE_NAME,
                                ImmutableMap.of(
                                    Route.UNSET_ROUTE_NEXT_HOP_IP, ImmutableSet.of(nullRoute))),
                            otherRoute,
                            ImmutableMap.of(
                                i1,
                                ImmutableMap.of(
                                    Route.UNSET_ROUTE_NEXT_HOP_IP, ImmutableSet.of(otherRoute)))))
                    .build()));
    Map<String, Map<String, IpSpace>> result =
        ForwardingAnalysisImpl.computeNullRoutedIps(computeMatchingIps(fibs), fibs);

    /* IPs for the null route should appear */
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(v1), containsIp(P1.getStartIp()))));
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(v1), containsIp(P1.getEndIp()))));
    /* IPs for the non-null route should not appear */
    assertThat(
        result, hasEntry(equalTo(c1), hasEntry(equalTo(v1), not(containsIp(P2.getStartIp())))));
    assertThat(
        result, hasEntry(equalTo(c1), hasEntry(equalTo(v1), not(containsIp(P2.getEndIp())))));
  }

  /**
   * The neighbor unreachable or exits network predicate map should not include an entry for null
   * interface.
   */
  @Test
  public void testComputeNeighborUnreachbleOrExitsNetwork_nullInterface() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf v = nf.vrfBuilder().setOwner(c).build();
    StaticRoute nullRoute =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("1.0.0.0/8"))
            .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
            .setAdministrativeCost(1)
            .build();
    v.setStaticRoutes(ImmutableSortedSet.of(nullRoute));
    SortedMap<String, Configuration> configs = ImmutableSortedMap.of(c.getHostname(), c);
    MockFib mockFib =
        MockFib.builder()
            .setNextHopInterfaces(
                ImmutableMap.of(
                    nullRoute,
                    ImmutableMap.of(
                        Interface.NULL_INTERFACE_NAME,
                        ImmutableMap.of(Ip.AUTO, ImmutableSet.of(nullRoute)))))
            .setRoutesByNextHopInterface(
                ImmutableMap.of(Interface.NULL_INTERFACE_NAME, ImmutableSet.of(nullRoute)))
            .build();

    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(c.getHostname(), ImmutableMap.of(v.getName(), mockFib));

    ForwardingAnalysisImpl forwardingAnalysisImpl =
        new ForwardingAnalysisImpl(configs, fibs, new Topology(ImmutableSortedSet.of()));

    Map<String, Map<String, Map<String, IpSpace>>> neighborUnreachableOrExitsNetwork =
        forwardingAnalysisImpl.getNeighborUnreachableOrExitsNetwork();

    assertThat(
        neighborUnreachableOrExitsNetwork,
        hasEntry(
            equalTo(c.getHostname()),
            hasEntry(equalTo(v.getName()), not(hasKey(Interface.NULL_INTERFACE_NAME)))));
  }

  @Test
  public void testComputeRoutableIps() {
    String c1 = "c1";
    String v1 = "v1";
    Prefix prefix = Prefix.parse("1.0.0.0/8");
    // The only important part of the Fib for this test is the route networks.
    MockFib fib =
        MockFib.builder()
            .setFibEntries(
                ImmutableMap.of(
                    Ip.ZERO,
                    ImmutableSet.of(
                        new FibEntry(
                            Ip.ZERO,
                            "iface",
                            ImmutableList.of(
                                StaticRoute.builder()
                                    .setAdministrativeCost(1)
                                    .setNetwork(prefix)
                                    .build())))))
            .build();
    Map<String, Map<String, Fib>> fibs = ImmutableMap.of(c1, ImmutableMap.of(v1, fib));
    Map<String, Map<String, IpSpace>> result = ForwardingAnalysisImpl.computeRoutableIps(fibs);

    assertThat(
        result,
        equalTo(
            ImmutableMap.of(
                c1,
                ImmutableMap.of(
                    v1,
                    IpWildcardSetIpSpace.builder().including(new IpWildcard(prefix)).build()))));
  }

  @Test
  public void testComputeRouteMatchConditions() {
    Set<AbstractRoute> routes =
        ImmutableSet.of(new ConnectedRoute(P1, INTERFACE1), new ConnectedRoute(P2, INTERFACE2));
    Map<Prefix, IpSpace> matchingIps = ImmutableMap.of(P1, IPSPACE1, P2, IPSPACE2);

    /* Resulting IP space should permit matching IPs */
    assertThat(
        ForwardingAnalysisImpl.computeRouteMatchConditions(routes, matchingIps),
        isAclIpSpaceThat(
            hasLines(
                containsInAnyOrder(
                    AclIpSpaceLine.permit(IPSPACE1), AclIpSpaceLine.permit(IPSPACE2)))));
  }

  @Test
  public void testComputeRoutesWhereDstIpCanBeArpIp() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    AbstractRoute r1 =
        StaticRoute.builder()
            .setNetwork(P1)
            .setNextHopIp(P2.getStartIp())
            .setAdministrativeCost(1)
            .build();
    AbstractRoute ifaceRoute = new ConnectedRoute(P2, i1);
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop =
        ImmutableMap.of(
            c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1, ifaceRoute))));
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1,
                MockFib.builder()
                    .setNextHopInterfaces(
                        ImmutableMap.of(
                            r1,
                            ImmutableMap.of(
                                i1,
                                ImmutableMap.of(r1.getNextHopIp(), ImmutableSet.of(ifaceRoute))),
                            ifaceRoute,
                            ImmutableMap.of(
                                i1,
                                ImmutableMap.of(
                                    Route.UNSET_ROUTE_NEXT_HOP_IP, ImmutableSet.of(ifaceRoute)))))
                    .build()));
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> result =
        computeRoutesWhereDstIpCanBeArpIp(fibs, routesWithNextHop);

    /* Only the interface route should show up */
    assertThat(
        result,
        equalTo(
            ImmutableMap.of(
                c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(ifaceRoute))))));
  }

  @Test
  public void testComputeRoutesWithDestIpEdge() {
    String c1 = "c1";
    String c2 = "c2";
    String v1 = "v1";
    String i1 = "i1";
    String i2 = "i2";
    AbstractRoute r1 = new ConnectedRoute(P1, i1);
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWhereDstIpCanBeArpIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1))));
    Edge e1 = Edge.of(c1, i1, c2, i2);
    Topology topology = new Topology(ImmutableSortedSet.of(e1));
    Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> result =
        computeRoutesWithDestIpEdge(topology, routesWhereDstIpCanBeArpIp);

    assertThat(
        result,
        hasEntry(
            equalTo(c1), hasEntry(equalTo(v1), equalTo(ImmutableMap.of(e1, ImmutableSet.of(r1))))));
  }

  @Test
  public void testComputeRoutesWithNextHop() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    ConnectedRoute r1 = new ConnectedRoute(P1, i1);
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1,
                MockFib.builder()
                    .setRoutesByNextHopInterface(ImmutableMap.of(i1, ImmutableSet.of(r1)))
                    .build()));

    Configuration config = _cb.setHostname(c1).build();
    Vrf vrf = _vb.setName(v1).setOwner(config).build();
    _ib.setName(i1).setVrf(vrf).setOwner(config).build();
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> result =
        ForwardingAnalysisImpl.computeRoutesWithNextHop(ImmutableMap.of(c1, config), fibs);

    assertThat(
        result,
        equalTo(
            ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1))))));
  }

  @Test
  public void testComputeRoutesWithNextHopIpArpFalse() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    AbstractRoute r1 =
        StaticRoute.builder()
            .setNetwork(P1)
            .setNextHopIp(P2.getStartIp())
            .setAdministrativeCost(1)
            .build();
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1))));
    AbstractRoute ifaceRoute = new ConnectedRoute(P2, i1);
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1,
                MockFib.builder()
                    .setNextHopInterfaces(
                        ImmutableMap.of(
                            r1,
                            ImmutableMap.of(
                                i1,
                                ImmutableMap.of(r1.getNextHopIp(), ImmutableSet.of(ifaceRoute)))))
                    .build()));
    Map<String, Map<String, IpSpace>> someoneReplies =
        ImmutableMap.of(c1, ImmutableMap.of(i1, P2.getEndIp().toIpSpace()));
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> result =
        computeRoutesWithNextHopIpArpFalse(fibs, routesWithNextHop, someoneReplies);

    assertThat(
        result,
        equalTo(
            ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1))))));
  }

  @Test
  public void testComputeRoutesWithNextHopIpArpFalseForInterface() {
    String outInterface = "i1";
    AbstractRoute nextHopIpRoute1 =
        StaticRoute.builder()
            .setNetwork(P1)
            .setNextHopIp(P2.getStartIp())
            .setAdministrativeCost(1)
            .build();
    AbstractRoute nextHopIpRoute2 =
        StaticRoute.builder()
            .setNetwork(P1)
            .setNextHopIp(P2.getEndIp())
            .setAdministrativeCost(1)
            .build();
    AbstractRoute ifaceRoute = new ConnectedRoute(P2, outInterface);
    Set<AbstractRoute> candidateRoutes =
        ImmutableSet.of(nextHopIpRoute1, nextHopIpRoute2, ifaceRoute);
    Fib fib =
        MockFib.builder()
            .setNextHopInterfaces(
                ImmutableMap.of(
                    nextHopIpRoute1,
                    ImmutableMap.of(
                        outInterface,
                        ImmutableMap.of(
                            nextHopIpRoute1.getNextHopIp(), ImmutableSet.of(ifaceRoute))),
                    nextHopIpRoute2,
                    ImmutableMap.of(
                        outInterface,
                        ImmutableMap.of(
                            nextHopIpRoute2.getNextHopIp(), ImmutableSet.of(ifaceRoute))),
                    ifaceRoute,
                    ImmutableMap.of(
                        outInterface,
                        ImmutableMap.of(
                            Route.UNSET_ROUTE_NEXT_HOP_IP, ImmutableSet.of(ifaceRoute)))))
            .build();
    Set<AbstractRoute> result =
        computeRoutesWithNextHopIpArpFalseForInterface(
            fib,
            outInterface,
            candidateRoutes,
            ImmutableMap.of(outInterface, P2.getStartIp().toIpSpace()));

    /*
     * Should only contain nextHopIpRoute1 since it is the only route with a next-hop-ip for which
     * there is no ARP reply.
     */
    assertThat(result, contains(nextHopIpRoute2));
  }

  @Test
  public void testComputeRoutesWithNextHopIpArpFalseForInterfaceNoNeighbors() {
    String outInterface = "i1";
    AbstractRoute nextHopIpRoute1 =
        StaticRoute.builder()
            .setNetwork(P1)
            .setNextHopIp(P2.getStartIp())
            .setAdministrativeCost(1)
            .build();
    AbstractRoute nextHopIpRoute2 =
        StaticRoute.builder()
            .setNetwork(P1)
            .setNextHopIp(P2.getEndIp())
            .setAdministrativeCost(1)
            .build();
    AbstractRoute ifaceRoute = new ConnectedRoute(P2, outInterface);
    Set<AbstractRoute> candidateRoutes =
        ImmutableSet.of(nextHopIpRoute1, nextHopIpRoute2, ifaceRoute);
    Fib fib =
        MockFib.builder()
            .setNextHopInterfaces(
                ImmutableMap.of(
                    nextHopIpRoute1,
                    ImmutableMap.of(
                        outInterface,
                        ImmutableMap.of(
                            nextHopIpRoute1.getNextHopIp(), ImmutableSet.of(ifaceRoute))),
                    nextHopIpRoute2,
                    ImmutableMap.of(
                        outInterface,
                        ImmutableMap.of(
                            nextHopIpRoute2.getNextHopIp(), ImmutableSet.of(ifaceRoute))),
                    ifaceRoute,
                    ImmutableMap.of(
                        outInterface,
                        ImmutableMap.of(
                            Route.UNSET_ROUTE_NEXT_HOP_IP, ImmutableSet.of(ifaceRoute)))))
            .build();
    Set<AbstractRoute> result =
        computeRoutesWithNextHopIpArpFalseForInterface(
            fib, outInterface, candidateRoutes, ImmutableMap.of());

    /*
     * Should contain both nextHopIpRoute1 and nextHopIpRoute2, since:
     * 1) They are the only routes with a next hop IP.
     * 2) Their next hop IPs do not receive ARP reply since _someoneReplies is empty.
     */
    assertThat(result, equalTo(ImmutableSet.of(nextHopIpRoute1, nextHopIpRoute2)));
  }

  @Test
  public void testComputeRoutesWithNextHopIpArpTrue() {
    String c1 = "c1";
    String i1 = "i1";
    String c2 = "c2";
    String i2 = "i2";
    Edge e1 = Edge.of(c1, i1, c2, i2);
    Map<String, Map<String, IpSpace>> arpReplies =
        ImmutableMap.of(c2, ImmutableMap.of(i2, P2.getStartIp().toIpSpace()));
    Topology topology = new Topology(ImmutableSortedSet.of(e1));
    String v1 = "v1";
    AbstractRoute r1 =
        StaticRoute.builder()
            .setNetwork(P1)
            .setNextHopIp(P2.getStartIp())
            .setAdministrativeCost(1)
            .build();
    AbstractRoute r2 =
        StaticRoute.builder()
            .setNetwork(P1)
            .setNextHopIp(P2.getEndIp())
            .setAdministrativeCost(1)
            .build();
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> routesWithNextHop =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1, r2))));
    AbstractRoute ifaceRoute = new ConnectedRoute(P2, i1);
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1,
                MockFib.builder()
                    .setNextHopInterfaces(
                        ImmutableMap.of(
                            r1,
                            ImmutableMap.of(
                                i1,
                                ImmutableMap.of(r1.getNextHopIp(), ImmutableSet.of(ifaceRoute))),
                            r2,
                            ImmutableMap.of(
                                i1,
                                ImmutableMap.of(r2.getNextHopIp(), ImmutableSet.of(ifaceRoute)))))
                    .build()));
    Map<String, Map<String, Map<Edge, Set<AbstractRoute>>>> result =
        computeRoutesWithNextHopIpArpTrue(fibs, topology, arpReplies, routesWithNextHop);

    /* Only the route with the next hop ip that gets a reply should be present. */
    assertThat(
        result,
        equalTo(
            ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(e1, ImmutableSet.of(r1))))));
  }

  @Test
  public void testComputeSomeoneReplies() {
    String c1 = "c1";
    String i1 = "i1";
    String c2 = "c2";
    String i2 = "i2";
    Edge e1 = Edge.of(c1, i1, c2, i2);
    Map<String, Map<String, IpSpace>> arpReplies =
        ImmutableMap.of(c2, ImmutableMap.of(i2, P1.toIpSpace()));
    Topology topology = new Topology(ImmutableSortedSet.of(e1));
    Map<String, Map<String, IpSpace>> result = computeSomeoneReplies(topology, arpReplies);

    /* IPs allowed by neighbor should appear */
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(i1), containsIp(P1.getStartIp()))));
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(i1), containsIp(P1.getEndIp()))));
    /* IPs not allowed by neighbor should not appear */
    assertThat(
        result, hasEntry(equalTo(c1), hasEntry(equalTo(i1), not(containsIp(P2.getStartIp())))));
  }

  @Test
  public void testComputeInterfaceHostSubnetIps() {
    Configuration c1 = _cb.build();
    Map<String, Configuration> configs = ImmutableMap.of(c1.getHostname(), c1);
    Vrf vrf1 = _vb.setOwner(c1).build();
    Interface i1 =
        _ib.setOwner(c1)
            .setVrf(vrf1)
            .setAddress(new InterfaceAddress(P1.getStartIp(), P1.getPrefixLength()))
            .build();
    Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps =
        computeInterfaceHostSubnetIps(configs);

    assertThat(
        interfaceHostSubnetIps,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()),
                hasEntry(equalTo(i1.getName()), (containsIp(Ip.parse("1.0.0.2")))))));
    assertThat(
        interfaceHostSubnetIps,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()),
                hasEntry(equalTo(i1.getName()), not(containsIp(P1.getStartIp()))))));
    assertThat(
        interfaceHostSubnetIps,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()),
                hasEntry(equalTo(i1.getName()), not(containsIp(P1.getEndIp()))))));
  }

  @Test
  public void testComputeInterfaceHostSubnetIpsWithPrefixLength31() {
    Configuration c1 = _cb.build();
    Map<String, Configuration> configs = ImmutableMap.of(c1.getHostname(), c1);
    Vrf vrf1 = _vb.setOwner(c1).build();
    Prefix prefix = Prefix.parse("1.0.0.1/31");
    Interface i1 =
        _ib.setOwner(c1)
            .setVrf(vrf1)
            .setAddress(new InterfaceAddress(prefix.getStartIp(), prefix.getPrefixLength()))
            .build();
    Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps =
        computeInterfaceHostSubnetIps(configs);

    assertThat(
        interfaceHostSubnetIps,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()),
                hasEntry(equalTo(i1.getName()), containsIp(prefix.getStartIp())))));
    assertThat(
        interfaceHostSubnetIps,
        hasEntry(
            equalTo(c1.getHostname()),
            hasEntry(
                equalTo(vrf1.getName()),
                hasEntry(equalTo(i1.getName()), containsIp(prefix.getEndIp())))));
  }

  @Test
  public void testComputeDeliveredToSubnetNoArpFalse() {
    String c1 = "c1";
    String vrf1 = "vrf1";
    String i1 = "i1";
    Ip ip = Ip.parse("10.0.0.1");

    Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp =
        ImmutableMap.of(c1, ImmutableMap.of(vrf1, ImmutableMap.of(i1, EmptyIpSpace.INSTANCE)));
    Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps =
        ImmutableMap.of(c1, ImmutableMap.of(vrf1, ImmutableMap.of(i1, ip.toIpSpace())));
    Map<String, Map<String, Set<String>>> fibInterfaces =
        ImmutableMap.of(c1, ImmutableMap.of(vrf1, ImmutableSet.of(i1)));
    IpSpace ownedIps = EmptyIpSpace.INSTANCE;

    Map<String, Map<String, Map<String, IpSpace>>> result =
        computeDeliveredToSubnet(fibInterfaces, arpFalseDestIp, interfaceHostSubnetIps, ownedIps);

    assertThat(
        result,
        hasEntry(equalTo(c1), hasEntry(equalTo(vrf1), hasEntry(equalTo(i1), not(containsIp(ip))))));
  }

  @Test
  public void testComputeDeliveredToSubnetNoInterfaceHostIps() {
    String c1 = "c1";
    String vrf1 = "vrf1";
    String i1 = "i1";
    Ip ip = Ip.parse("10.0.0.1");

    Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp =
        ImmutableMap.of(c1, ImmutableMap.of(vrf1, ImmutableMap.of(i1, ip.toIpSpace())));
    Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps =
        ImmutableMap.of(c1, ImmutableMap.of(vrf1, ImmutableMap.of(i1, EmptyIpSpace.INSTANCE)));
    Map<String, Map<String, Set<String>>> fibInterfaces =
        ImmutableMap.of(c1, ImmutableMap.of(vrf1, ImmutableSet.of(i1)));
    IpSpace ownedIps = EmptyIpSpace.INSTANCE;

    Map<String, Map<String, Map<String, IpSpace>>> result =
        computeDeliveredToSubnet(fibInterfaces, arpFalseDestIp, interfaceHostSubnetIps, ownedIps);

    assertThat(
        result,
        hasEntry(equalTo(c1), hasEntry(equalTo(vrf1), hasEntry(equalTo(i1), not(containsIp(ip))))));
  }

  @Test
  public void testComputeDeliveredToSubnetEqual() {
    String c1 = "c1";
    String vrf1 = "vrf1";
    String i1 = "i1";
    Ip ip = Ip.parse("10.0.0.1");

    Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp =
        ImmutableMap.of(c1, ImmutableMap.of(vrf1, ImmutableMap.of(i1, ip.toIpSpace())));
    Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps =
        ImmutableMap.of(c1, ImmutableMap.of(vrf1, ImmutableMap.of(i1, ip.toIpSpace())));
    Map<String, Map<String, Set<String>>> fibInterfaces =
        ImmutableMap.of(c1, ImmutableMap.of(vrf1, ImmutableSet.of(i1)));
    IpSpace ownedIps = EmptyIpSpace.INSTANCE;

    Map<String, Map<String, Map<String, IpSpace>>> result =
        computeDeliveredToSubnet(fibInterfaces, arpFalseDestIp, interfaceHostSubnetIps, ownedIps);

    assertThat(
        result,
        hasEntry(equalTo(c1), hasEntry(equalTo(vrf1), hasEntry(equalTo(i1), containsIp(ip)))));
  }

  enum NextHopIpStatus {
    NONE,
    INTERNAL,
    EXTERNAL
  }

  private static void testDispositionComputationTemplate(
      NextHopIpStatus nextHopIpStatus,
      boolean isSubnetFull,
      boolean isDstIpInternal,
      boolean isDstIpInSubnet,
      FlowDisposition expectedDisposition) {
    String nextHopIpString = "1.0.0.1";
    Prefix dstPrefix = P3;
    Ip nextHopIp = Ip.parse(nextHopIpString);

    AclIpSpace.Builder internalIpsBuilder = AclIpSpace.builder();

    Map<String, Set<String>> interfacesWithMissingDevices;
    if (!isSubnetFull) {
      interfacesWithMissingDevices = ImmutableMap.of(CONFIG1, ImmutableSet.of(INTERFACE1));
    } else {
      interfacesWithMissingDevices = ImmutableMap.of(CONFIG1, ImmutableSet.of());
    }

    Map<String, Map<String, Map<String, IpSpace>>> arpFalseDestIp;
    Map<String, Map<String, Map<String, IpSpace>>> dstIpsWithOwnedNextHopIpArpFalse;
    Map<String, Map<String, Map<String, IpSpace>>> dstIpsWithUnownedNextHopIpArpFalse;
    if (nextHopIpStatus == NextHopIpStatus.EXTERNAL) {
      arpFalseDestIp =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, EmptyIpSpace.INSTANCE)));
      dstIpsWithOwnedNextHopIpArpFalse =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, EmptyIpSpace.INSTANCE)));
      dstIpsWithUnownedNextHopIpArpFalse =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, dstPrefix.toIpSpace())));
    } else if (nextHopIpStatus == NextHopIpStatus.INTERNAL) {
      arpFalseDestIp =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, EmptyIpSpace.INSTANCE)));
      internalIpsBuilder.thenPermitting(nextHopIp.toIpSpace());
      dstIpsWithOwnedNextHopIpArpFalse =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, dstPrefix.toIpSpace())));
      dstIpsWithUnownedNextHopIpArpFalse =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, EmptyIpSpace.INSTANCE)));
    } else {
      arpFalseDestIp =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, dstPrefix.toIpSpace())));
      dstIpsWithOwnedNextHopIpArpFalse =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, EmptyIpSpace.INSTANCE)));
      dstIpsWithUnownedNextHopIpArpFalse =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, EmptyIpSpace.INSTANCE)));
    }

    if (isDstIpInternal) {
      internalIpsBuilder.thenPermitting(dstPrefix.toIpSpace());
    }

    IpSpace internalIps = internalIpsBuilder.build();
    IpSpace externalIps = internalIps.complement();

    Map<String, Map<String, Map<String, IpSpace>>> interfaceHostSubnetIps;
    if (isDstIpInSubnet) {
      interfaceHostSubnetIps =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, dstPrefix.toIpSpace())));
    } else {
      interfaceHostSubnetIps =
          ImmutableMap.of(
              CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, EmptyIpSpace.INSTANCE)));
    }

    Map<String, Map<String, Map<String, IpSpace>>> arpFalse =
        ImmutableMap.of(
            CONFIG1, ImmutableMap.of(VRF1, ImmutableMap.of(INTERFACE1, dstPrefix.toIpSpace())));

    IpSpace ownedIps = EmptyIpSpace.INSTANCE;

    Map<String, Map<String, Set<String>>> fibInterfaces =
        ImmutableMap.of(CONFIG1, ImmutableMap.of(VRF1, ImmutableSet.of(INTERFACE1)));
    IpSpace deliveredToSubnetIpSpace =
        computeDeliveredToSubnet(fibInterfaces, arpFalseDestIp, interfaceHostSubnetIps, ownedIps)
            .get(CONFIG1)
            .get(VRF1)
            .get(INTERFACE1);
    IpSpace exitsNetworkIpSpace =
        computeExitsNetwork(
                fibInterfaces,
                interfacesWithMissingDevices,
                dstIpsWithUnownedNextHopIpArpFalse,
                arpFalseDestIp,
                externalIps)
            .get(CONFIG1)
            .get(VRF1)
            .get(INTERFACE1);

    IpSpace insufficientInfoIpSpace =
        computeInsufficientInfo(
                fibInterfaces,
                interfaceHostSubnetIps,
                interfacesWithMissingDevices,
                arpFalseDestIp,
                dstIpsWithUnownedNextHopIpArpFalse,
                dstIpsWithOwnedNextHopIpArpFalse,
                internalIps)
            .get(CONFIG1)
            .get(VRF1)
            .get(INTERFACE1);
    IpSpace neighborUnreachableIpSpace =
        computeNeighborUnreachable(
                fibInterfaces,
                arpFalse,
                interfacesWithMissingDevices,
                arpFalseDestIp,
                interfaceHostSubnetIps,
                ownedIps)
            .get(CONFIG1)
            .get(VRF1)
            .get(INTERFACE1);

    if (expectedDisposition == FlowDisposition.EXITS_NETWORK) {
      assertThat(exitsNetworkIpSpace, containsIp(dstPrefix.getStartIp()));
      assertThat(exitsNetworkIpSpace, containsIp(dstPrefix.getEndIp()));
    } else {
      assertThat(exitsNetworkIpSpace, not(containsIp(dstPrefix.getStartIp())));
      assertThat(exitsNetworkIpSpace, not(containsIp(dstPrefix.getEndIp())));
    }

    if (expectedDisposition == FlowDisposition.INSUFFICIENT_INFO) {
      assertThat(insufficientInfoIpSpace, containsIp(dstPrefix.getStartIp()));
      assertThat(insufficientInfoIpSpace, containsIp(dstPrefix.getEndIp()));
    } else {
      assertThat(insufficientInfoIpSpace, not(containsIp(dstPrefix.getStartIp())));
      assertThat(insufficientInfoIpSpace, not(containsIp(dstPrefix.getEndIp())));
    }

    if (expectedDisposition == FlowDisposition.DELIVERED_TO_SUBNET) {
      assertThat(deliveredToSubnetIpSpace, containsIp(dstPrefix.getStartIp()));
      assertThat(deliveredToSubnetIpSpace, containsIp(dstPrefix.getEndIp()));
    } else {
      assertThat(deliveredToSubnetIpSpace, not(containsIp(dstPrefix.getStartIp())));
      assertThat(deliveredToSubnetIpSpace, not(containsIp(dstPrefix.getEndIp())));
    }

    if (expectedDisposition == FlowDisposition.NEIGHBOR_UNREACHABLE) {
      assertThat(neighborUnreachableIpSpace, (containsIp(dstPrefix.getStartIp())));
      assertThat(neighborUnreachableIpSpace, (containsIp(dstPrefix.getEndIp())));
    } else {
      assertThat(neighborUnreachableIpSpace, not(containsIp(dstPrefix.getStartIp())));
      assertThat(neighborUnreachableIpSpace, not(containsIp(dstPrefix.getEndIp())));
    }
  }

  @Test
  public void testDispositionComputation() {
    /*
     * Avoid the case where arp dst ip, interface is full, and dst ip is in subnet (would be accepted).
     * Avoid cases where dst ip is internal but not in subet.
     */

    // Arp dst ip, interface is full, dst ip is internal -> neighbor unreachable
    testDispositionComputationTemplate(
        NextHopIpStatus.NONE, true, true, false, FlowDisposition.NEIGHBOR_UNREACHABLE);

    // Arp dst ip, interface is full, dst ip is external -> neighbor unreachable
    testDispositionComputationTemplate(
        NextHopIpStatus.NONE, true, false, false, FlowDisposition.NEIGHBOR_UNREACHABLE);

    // Arp dst ip, interface is not full, dst ip is subnet -> delivered to subnet
    testDispositionComputationTemplate(
        NextHopIpStatus.NONE, false, true, true, FlowDisposition.DELIVERED_TO_SUBNET);

    // Arp dst ip, interface is not full, dst ip is internal -> insufficient info
    testDispositionComputationTemplate(
        NextHopIpStatus.NONE, false, true, false, FlowDisposition.INSUFFICIENT_INFO);

    // Arp dst ip, interface is not full, dst ip is external -> exits network
    testDispositionComputationTemplate(
        NextHopIpStatus.NONE, false, false, false, FlowDisposition.EXITS_NETWORK);

    // nhip external, interface is full, dst ip is internal -> neighbor unreachable
    testDispositionComputationTemplate(
        NextHopIpStatus.EXTERNAL, true, true, true, FlowDisposition.NEIGHBOR_UNREACHABLE);

    testDispositionComputationTemplate(
        NextHopIpStatus.EXTERNAL, true, true, false, FlowDisposition.NEIGHBOR_UNREACHABLE);

    // nhip external, interface is full, dst ip is external -> neighbor unreachable
    testDispositionComputationTemplate(
        NextHopIpStatus.EXTERNAL, true, false, false, FlowDisposition.NEIGHBOR_UNREACHABLE);

    // nhip external, interface is not full, dst ip is internal -> insufficient info
    testDispositionComputationTemplate(
        NextHopIpStatus.EXTERNAL, false, true, true, FlowDisposition.INSUFFICIENT_INFO);

    testDispositionComputationTemplate(
        NextHopIpStatus.EXTERNAL, false, true, false, FlowDisposition.INSUFFICIENT_INFO);

    // nhip external, interface is not full, dst ip is external -> exits network
    testDispositionComputationTemplate(
        NextHopIpStatus.EXTERNAL, false, false, false, FlowDisposition.EXITS_NETWORK);

    // nhip internal, interface is full, dst ip is internal -> neighbor unreachable
    testDispositionComputationTemplate(
        NextHopIpStatus.INTERNAL, true, true, true, FlowDisposition.NEIGHBOR_UNREACHABLE);

    testDispositionComputationTemplate(
        NextHopIpStatus.INTERNAL, true, true, false, FlowDisposition.NEIGHBOR_UNREACHABLE);

    // nhip internal, interface is full, dst ip is external -> neighbor unreachable
    testDispositionComputationTemplate(
        NextHopIpStatus.INTERNAL, true, false, false, FlowDisposition.NEIGHBOR_UNREACHABLE);

    // nhip internal, interface is not full, dst ip is internal -> insufficient info
    testDispositionComputationTemplate(
        NextHopIpStatus.INTERNAL, false, true, true, FlowDisposition.INSUFFICIENT_INFO);

    testDispositionComputationTemplate(
        NextHopIpStatus.INTERNAL, false, true, false, FlowDisposition.INSUFFICIENT_INFO);

    // nhip internal, interface is not full, dst ip is external -> insufficient info
    testDispositionComputationTemplate(
        NextHopIpStatus.INTERNAL, false, false, false, FlowDisposition.INSUFFICIENT_INFO);
  }

  // If two nodes are in the same subnet but not connected per the given topology,
  // sending packets from one to the other should result in Neighbor Unreachable.
  @Test
  public void testDispositionWithTopology() {
    Prefix prefix = Prefix.parse("1.0.0.0/24");
    IpSpace ipSpace = prefix.toIpSpace();
    Ip ip2 = Ip.parse("1.0.0.2");

    Configuration c1 = _cb.setHostname("c1").build();
    Configuration c2 = _cb.setHostname("c2").build();
    Vrf v1 = _vb.setName("v1").setOwner(c1).build();
    Vrf v2 = _vb.setName("v2").setOwner(c2).build();
    _ib.setActive(true);
    Interface i1 =
        _ib.setAddresses(new InterfaceAddress("1.0.0.1/24"))
            .setName("i1")
            .setOwner(c1)
            .setVrf(v1)
            .build();
    Interface i2 =
        _ib.setAddresses(new InterfaceAddress("1.0.0.2/24"))
            .setName("i2")
            .setOwner(c2)
            .setVrf(v2)
            .build();

    StaticRoute route1 =
        StaticRoute.builder()
            .setNetwork(prefix)
            .setNextHopInterface(i1.getName())
            .setAdministrativeCost(1)
            .build();

    StaticRoute route2 =
        StaticRoute.builder()
            .setNextHopInterface(i2.getName())
            .setNetwork(prefix)
            .setAdministrativeCost(1)
            .build();

    v1.setStaticRoutes(ImmutableSortedSet.of(route1));
    v2.setStaticRoutes(ImmutableSortedSet.of(route2));

    MockFib fib1 =
        MockFib.builder()
            .setMatchingIps(ImmutableMap.of(prefix, ipSpace))
            .setNextHopInterfaces(
                ImmutableMap.of(
                    route1,
                    ImmutableMap.of(
                        i1.getName(),
                        ImmutableMap.of(Route.UNSET_ROUTE_NEXT_HOP_IP, ImmutableSet.of(route1)))))
            .setRoutesByNextHopInterface(ImmutableMap.of(i1.getName(), ImmutableSet.of(route1)))
            .setFibEntries(
                ImmutableMap.of(
                    Route.UNSET_ROUTE_NEXT_HOP_IP,
                    ImmutableSet.of(
                        new FibEntry(
                            Route.UNSET_ROUTE_NEXT_HOP_IP,
                            route1.getNextHopInterface(),
                            ImmutableList.of(route1)))))
            .build();

    MockFib fib2 =
        MockFib.builder()
            .setMatchingIps(ImmutableMap.of(prefix, ipSpace))
            .setNextHopInterfaces(
                ImmutableMap.of(
                    route2,
                    ImmutableMap.of(
                        i2.getName(),
                        ImmutableMap.of(Route.UNSET_ROUTE_NEXT_HOP_IP, ImmutableSet.of(route2)))))
            .setRoutesByNextHopInterface(ImmutableMap.of(i2.getName(), ImmutableSet.of(route2)))
            .setFibEntries(
                ImmutableMap.of(
                    Route.UNSET_ROUTE_NEXT_HOP_IP,
                    ImmutableSet.of(
                        new FibEntry(
                            Route.UNSET_ROUTE_NEXT_HOP_IP,
                            route2.getNextHopInterface(),
                            ImmutableList.of(route2)))))
            .build();

    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(
            c1.getHostname(), ImmutableMap.of(v1.getName(), fib1),
            c2.getHostname(), ImmutableMap.of(v2.getName(), fib2));

    ForwardingAnalysis analysis =
        new ForwardingAnalysisImpl(
            ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2),
            fibs,
            new Topology(ImmutableSortedSet.of()));

    assertFalse(
        analysis
            .getDeliveredToSubnet()
            .get(c1.getHostname())
            .get(v1.getName())
            .get(i1.getName())
            .containsIp(ip2, c1.getIpSpaces()));

    assertTrue(
        analysis
            .getNeighborUnreachable()
            .get(c1.getHostname())
            .get(v1.getName())
            .get(i1.getName())
            .containsIp(ip2, c1.getIpSpaces()));

    assertFalse(
        analysis
            .getInsufficientInfo()
            .get(c1.getHostname())
            .get(v1.getName())
            .get(i1.getName())
            .containsIp(ip2, c1.getIpSpaces()));
  }

  /**
   * Test that interfaces reachable from FIB in VRF X are not limited to interfaces that belong to
   * VRF X. This is due to route leaking across VRFs and ability to do lookup in a different VRF.
   */
  @Test
  public void testComputeFibInterfaces() {
    // Setup
    final String hostname = "c1";
    Configuration c1 =
        _cb.setHostname(hostname).setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf v1 = _vb.setOwner(c1).setName("v1").build();
    Vrf v2 = _vb.setOwner(c1).setName("v2").build();
    Interface i1 =
        _ib.setAddress(new InterfaceAddress("1.1.1.0/24")).setVrf(v1).setOwner(c1).build();

    Ip ip = Ip.parse("1.1.1.222");
    Map<Ip, Set<FibEntry>> fibEntryMap =
        ImmutableMap.of(
            ip,
            ImmutableSet.of(
                new FibEntry(
                    ip,
                    i1.getName(),
                    ImmutableList.of(new ConnectedRoute(i1.getPrimaryNetwork(), i1.getName())))));

    // Test:
    Map<String, Map<String, Set<String>>> fibInterfaces =
        computeFibInterfaces(
            ImmutableMap.of(
                hostname,
                ImmutableMap.of(
                    "v1",
                    MockFib.builder().setFibEntries(fibEntryMap).build(),
                    "v2",
                    MockFib.builder().setFibEntries(fibEntryMap).build())));

    // Both vrf1 and vrf2 can go out to i1
    assertThat(
        fibInterfaces.get(hostname).get(v1.getName()), equalTo(ImmutableSet.of(i1.getName())));
    assertThat(
        fibInterfaces.get(hostname).get(v2.getName()), equalTo(ImmutableSet.of(i1.getName())));
  }
}

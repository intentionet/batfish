package org.batfish.datamodel;

import static org.batfish.datamodel.matchers.AclIpSpaceMatchers.hasLines;
import static org.batfish.datamodel.matchers.AclIpSpaceMatchers.isAclIpSpaceThat;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.util.CommonUtil;
import org.junit.Before;
import org.junit.Test;

public class ForwardingAnalysisImplTest {

  private static final String INTERFACE1 = "interface1";

  private static final String INTERFACE2 = "interface2";

  private static final IpSpace IPSPACE1 = new MockIpSpace(1);

  private static final IpSpace IPSPACE2 = new MockIpSpace(2);

  private static final Prefix P1 = Prefix.parse("1.0.0.0/8");

  private static final Prefix P2 = Prefix.parse("2.0.0.0/16");

  private static final Prefix P3 = Prefix.parse("3.0.0.0/24");

  private Map<String, Map<String, IpSpace>> _arpReplies;

  private Map<Edge, IpSpace> _arpTrueEdge;

  private Map<Edge, IpSpace> _arpTrueEdgeDestIp;

  private Map<Edge, IpSpace> _arpTrueEdgeNextHopIp;

  private Configuration.Builder _cb;

  private Interface.Builder _ib;

  private Map<String, Map<String, Set<Ip>>> _interfaceOwnedIps;

  private Map<String, Map<String, IpSpace>> _ipsRoutedOutInterfaces;

  private Map<String, Map<String, Map<String, IpSpace>>> _neighborUnreachable;

  private Map<String, Map<String, Map<String, IpSpace>>> _neighborUnreachableArpDestIp;

  private Map<String, Map<String, Map<String, IpSpace>>> _neighborUnreachableArpNextHopIp;

  private NetworkFactory _nf;

  private Map<String, Map<String, IpSpace>> _nullRoutedIps;

  private Map<String, Map<String, IpSpace>> _routableIps;

  private Map<String, Map<String, Map<String, Set<AbstractRoute>>>> _routesWhereDstIpCanBeArpIp;

  private Map<Edge, Set<AbstractRoute>> _routesWithDestIpEdge;

  private Map<String, Map<String, Map<String, Set<AbstractRoute>>>> _routesWithNextHop;

  private Map<String, Map<String, Map<String, Set<AbstractRoute>>>> _routesWithNextHopIpArpFalse;

  private Map<Edge, Set<AbstractRoute>> _routesWithNextHopIpArpTrue;

  private Map<String, Map<String, IpSpace>> _someoneReplies;

  private Vrf.Builder _vb;

  private ForwardingAnalysisImpl initForwardingAnalysisImpl() {
    return new ForwardingAnalysisImpl(
        _arpReplies,
        _arpTrueEdge,
        _arpTrueEdgeDestIp,
        _arpTrueEdgeNextHopIp,
        _interfaceOwnedIps,
        _ipsRoutedOutInterfaces,
        _neighborUnreachable,
        _neighborUnreachableArpDestIp,
        _neighborUnreachableArpNextHopIp,
        _nullRoutedIps,
        _routableIps,
        _routesWhereDstIpCanBeArpIp,
        _routesWithDestIpEdge,
        _routesWithNextHop,
        _routesWithNextHopIpArpFalse,
        _routesWithNextHopIpArpTrue,
        _someoneReplies);
  }

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _vb = _nf.vrfBuilder();
    _ib = _nf.interfaceBuilder().setActive(true);
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
    IpSpace ipsRoutedOutI1 =
        IpWildcardSetIpSpace.builder().including(new IpWildcard(P1), new IpWildcard(P3)).build();
    IpSpace ipsRoutedOutI2 = IpWildcardSetIpSpace.builder().including(new IpWildcard(P2)).build();
    Map<String, Configuration> configurations =
        ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            c1.getHostname(),
            ImmutableSortedMap.of(
                vrf1.getName(), MockRib.builder().setRoutableIps(UniverseIpSpace.INSTANCE).build()),
            c2.getHostname(),
            ImmutableSortedMap.of(
                vrf2.getName(),
                MockRib.builder().setRoutableIps(UniverseIpSpace.INSTANCE).build()));
    _ipsRoutedOutInterfaces =
        ImmutableMap.of(
            c1.getHostname(),
            ImmutableMap.of(i1.getName(), ipsRoutedOutI1),
            c2.getHostname(),
            ImmutableMap.of(i2.getName(), ipsRoutedOutI2));
    _interfaceOwnedIps = CommonUtil.computeInterfaceOwnedIps(configs, false);
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, IpSpace>> result =
        forwardingAnalysisImpl.computeArpReplies(configurations, ribs);

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
    /* No proxy-arp: just match interface ip*/
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

    Map<String, Configuration> configs = ImmutableMap.of(config.getHostname(), config);
    _interfaceOwnedIps = CommonUtil.computeInterfaceOwnedIps(configs, false);
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, IpSpace> result =
        forwardingAnalysisImpl.computeArpRepliesByInterface(
            interfaces, routableIpsByVrf, ipsRoutedOutInterfaces);

    /* Proxy-arp: Match interface IP, reject what's routed through i1, accept everything else*/
    assertThat(result, hasEntry(equalTo(i1.getName()), containsIp(P1.getStartIp())));
    assertThat(result, hasEntry(equalTo(i1.getName()), not(containsIp(P1.getEndIp()))));
    assertThat(result, hasEntry(equalTo(i1.getName()), not(containsIp(P3.getStartIp()))));
    assertThat(result, hasEntry(equalTo(i1.getName()), containsIp(P2.getStartIp())));
    /* No proxy-arp: just match interface ip*/
    assertThat(result, hasEntry(equalTo(i2.getName()), containsIp(P2.getStartIp())));
    assertThat(result, hasEntry(equalTo(i2.getName()), not(containsIp(P2.getEndIp()))));
    assertThat(result, hasEntry(equalTo(i2.getName()), not(containsIp(P3.getStartIp()))));
    assertThat(result, hasEntry(equalTo(i2.getName()), not(containsIp(P1.getStartIp()))));
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

    _interfaceOwnedIps = CommonUtil.computeInterfaceOwnedIps(configs, false);
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();

    IpSpace p1IpSpace = new IpWildcard(P1).toIpSpace();
    IpSpace i1ArpReplies =
        forwardingAnalysisImpl.computeInterfaceArpReplies(i1, UniverseIpSpace.INSTANCE, p1IpSpace);
    IpSpace i2ArpReplies =
        forwardingAnalysisImpl.computeInterfaceArpReplies(i2, UniverseIpSpace.INSTANCE, p1IpSpace);

    assertThat(i1ArpReplies, not(containsIp(new Ip("1.1.1.1"))));
    assertThat(i2ArpReplies, containsIp(new Ip("1.1.1.1")));
  }

  @Test
  public void testComputeArpTrueEdge() {
    IpSpace nextHopIpSpace = new MockIpSpace(1);
    IpSpace dstIpSpace = new MockIpSpace(2);
    Edge e1 = new Edge("c1", "i1", "c2", "i2");
    _arpTrueEdgeDestIp = ImmutableMap.of(e1, dstIpSpace);
    _arpTrueEdgeNextHopIp = ImmutableMap.of(e1, nextHopIpSpace);
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<Edge, IpSpace> result = forwardingAnalysisImpl.computeArpTrueEdge();

    assertThat(
        result,
        hasEntry(
            equalTo(e1),
            isAclIpSpaceThat(
                hasLines(
                    containsInAnyOrder(
                        AclIpSpaceLine.permit(nextHopIpSpace),
                        AclIpSpaceLine.permit(dstIpSpace))))));
  }

  @Test
  public void testComputeArpTrueEdgeDestIp() {
    Configuration c1 = _cb.build();
    Configuration c2 = _cb.build();
    Vrf vrf1 = _vb.setOwner(c1).build();
    Vrf vrf2 = _vb.setOwner(c2).build();
    Interface i1 = _ib.setOwner(c1).setVrf(vrf1).build();
    Ip i2Ip = new Ip(P1.getStartIp().asLong() + 1);
    Interface i2 = _ib.setOwner(c2).setVrf(vrf2).build();
    Map<String, Configuration> configurations =
        ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            c1.getHostname(),
            ImmutableSortedMap.of(
                vrf1.getName(),
                MockRib.builder()
                    .setMatchingIps(
                        ImmutableMap.of(
                            P1,
                            AclIpSpace.rejecting(
                                    new Prefix(P1.getEndIp(), Prefix.MAX_PREFIX_LENGTH).toIpSpace())
                                .thenPermitting(P1.toIpSpace())
                                .build()))
                    .build()));
    Edge edge = new Edge(c1.getHostname(), i1.getName(), c2.getHostname(), i2.getName());
    _routesWithDestIpEdge =
        ImmutableMap.of(edge, ImmutableSet.of(new ConnectedRoute(P1, i1.getName())));
    _arpReplies =
        ImmutableMap.of(
            c2.getHostname(),
            ImmutableMap.of(
                i2.getName(),
                AclIpSpace.permitting(i2Ip.toIpSpace())
                    .thenPermitting(P1.getEndIp().toIpSpace())
                    .build()));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<Edge, IpSpace> result =
        forwardingAnalysisImpl.computeArpTrueEdgeDestIp(configurations, ribs);

    /* Respond to request for IP on i2. */
    assertThat(result, hasEntry(equalTo(edge), containsIp(i2Ip)));
    /* Do not make ARP request for IP matched by more specific route not going out i1.  */
    assertThat(result, hasEntry(equalTo(edge), not(containsIp(P1.getEndIp()))));
    /* Do not receive response for IP i2 does not own. */
    assertThat(result, hasEntry(equalTo(edge), not(containsIp(P1.getStartIp()))));
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
    Ip i2Ip = new Ip(P1.getStartIp().asLong() + 1);
    Interface i2 =
        _ib.setOwner(c2)
            .setVrf(vrf2)
            .setAddress(new InterfaceAddress(i2Ip, P1.getPrefixLength()))
            .build();
    Edge edge = new Edge(c1.getHostname(), i1.getName(), c2.getHostname(), i2.getName());
    Map<String, Configuration> configurations =
        ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            c1.getHostname(),
            ImmutableSortedMap.of(
                vrf1.getName(),
                MockRib.builder()
                    .setMatchingIps(
                        ImmutableMap.of(
                            P1,
                            AclIpSpace.rejecting(
                                    new Prefix(P1.getEndIp(), Prefix.MAX_PREFIX_LENGTH).toIpSpace())
                                .thenPermitting(P1.toIpSpace())
                                .build()))
                    .build()));
    _routesWithNextHopIpArpTrue =
        ImmutableMap.of(
            edge,
            ImmutableSet.of(
                StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getStartIp()).build()));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<Edge, IpSpace> result =
        forwardingAnalysisImpl.computeArpTrueEdgeNextHopIp(configurations, ribs);

    /*
     * Respond for any destination IP in network not matching more specific route not going out i1.
     */
    assertThat(result, hasEntry(equalTo(edge), containsIp(P1.getStartIp())));
    assertThat(result, hasEntry(equalTo(edge), containsIp(i2Ip)));
    /* Do not respond for destination IP matching more specific route not going out i1 */
    assertThat(result, hasEntry(equalTo(edge), not(containsIp(P1.getEndIp()))));
    /* Do not respond for destination IPs not matching route */
    assertThat(result, hasEntry(equalTo(edge), not(containsIp(P2.getStartIp()))));
    assertThat(result, hasEntry(equalTo(edge), not(containsIp(P2.getEndIp()))));
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
    _interfaceOwnedIps =
        CommonUtil.computeInterfaceOwnedIps(ImmutableMap.of(config.getHostname(), config), false);
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    IpSpace noProxyArpResult =
        forwardingAnalysisImpl.computeInterfaceArpReplies(
            iNoProxyArp, routableIpsForThisVrf, ipsRoutedThroughInterface);
    IpSpace proxyArpResult =
        forwardingAnalysisImpl.computeInterfaceArpReplies(
            iProxyArp, routableIpsForThisVrf, ipsRoutedThroughInterface);

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
    _interfaceOwnedIps = CommonUtil.computeInterfaceOwnedIps(configs, false);
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    IpSpace result = forwardingAnalysisImpl.computeIpsAssignedToThisInterface(i);

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
            .build();
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            c1,
            ImmutableSortedMap.of(
                v1,
                MockRib.builder()
                    .setMatchingIps(ImmutableMap.of(P1, P1.toIpSpace(), P2, P2.toIpSpace()))
                    .build()));
    _routesWithNextHop =
        ImmutableMap.of(
            c1,
            ImmutableMap.of(
                v1,
                ImmutableMap.of(
                    i1,
                    ImmutableSet.of(r1),
                    Interface.NULL_INTERFACE_NAME,
                    ImmutableSet.of(nullRoute))));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, IpSpace>> result =
        forwardingAnalysisImpl.computeIpsRoutedOutInterfaces(ribs);

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
  public void testComputeNeighborUnreachable() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    _neighborUnreachableArpDestIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, P1.getStartIp().toIpSpace())));
    _neighborUnreachableArpNextHopIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, P1.getEndIp().toIpSpace())));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, Map<String, IpSpace>>> result =
        forwardingAnalysisImpl.computeNeighborUnreachable();

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
  public void testComputeNeighborUnreachableArpDestIp() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    AbstractRoute ifaceRoute = new ConnectedRoute(P1, i1);
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            c1,
            ImmutableSortedMap.of(
                v1, MockRib.builder().setMatchingIps(ImmutableMap.of(P1, P1.toIpSpace())).build()));
    _routesWhereDstIpCanBeArpIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(ifaceRoute))));
    _someoneReplies = ImmutableMap.of(c1, ImmutableMap.of(i1, P1.getEndIp().toIpSpace()));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, Map<String, IpSpace>>> result =
        forwardingAnalysisImpl.computeNeighborUnreachableArpDestIp(ribs);

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
  public void testComputeNeighborUnreachableArpDestIpNoNeighbors() {
    String c1 = "c1";
    String v1 = "v1";
    String i1 = "i1";
    AbstractRoute ifaceRoute = new ConnectedRoute(P1, i1);
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            c1,
            ImmutableSortedMap.of(
                v1, MockRib.builder().setMatchingIps(ImmutableMap.of(P1, P1.toIpSpace())).build()));
    _routesWhereDstIpCanBeArpIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(ifaceRoute))));
    _someoneReplies = ImmutableMap.of();
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, Map<String, IpSpace>>> result =
        forwardingAnalysisImpl.computeNeighborUnreachableArpDestIp(ribs);

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
    AbstractRoute r1 = StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getStartIp()).build();
    _routesWithNextHop =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1))));
    _routesWithNextHopIpArpFalse =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1))));
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            c1,
            ImmutableSortedMap.of(
                v1, MockRib.builder().setMatchingIps(ImmutableMap.of(P1, P1.toIpSpace())).build()));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, Map<String, IpSpace>>> result =
        forwardingAnalysisImpl.computeNeighborUnreachableArpNextHopIp(ribs);

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
    GenericRib<AbstractRoute> rib1 =
        MockRib.builder()
            .setMatchingIps(
                ImmutableMap.of(
                    P1,
                    AclIpSpace.permitting(P1.toIpSpace()).build(),
                    P2,
                    AclIpSpace.permitting(P2.toIpSpace()).build()))
            .build();
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(c1, ImmutableSortedMap.of(v1, rib1));
    AbstractRoute nullRoute =
        StaticRoute.builder()
            .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
            .setNetwork(P1)
            .build();
    AbstractRoute otherRoute = new ConnectedRoute(P2, i1);
    Map<String, Map<String, Fib>> fibs =
        ImmutableSortedMap.of(
            c1,
            ImmutableSortedMap.of(
                v1,
                MockFib.builder()
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
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, IpSpace>> result =
        forwardingAnalysisImpl.computeNullRoutedIps(ribs, fibs);

    /* IPs for the null route should appear */
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(v1), containsIp(P1.getStartIp()))));
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(v1), containsIp(P1.getEndIp()))));
    /* IPs for the non-null route should not appear */
    assertThat(
        result, hasEntry(equalTo(c1), hasEntry(equalTo(v1), not(containsIp(P2.getStartIp())))));
    assertThat(
        result, hasEntry(equalTo(c1), hasEntry(equalTo(v1), not(containsIp(P2.getEndIp())))));
  }

  /** The neighbor unreachable predicate map should not include an entry for null interface. */
  @Test
  public void testComputeNeighborUnreachble_nullInterface() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    Vrf v = nf.vrfBuilder().setOwner(c).build();
    StaticRoute nullRoute =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("1.0.0.0/8"))
            .setNextHopInterface(Interface.NULL_INTERFACE_NAME)
            .build();
    IpSpace ipSpace = IpWildcardSetIpSpace.builder().including(new IpWildcard("1.0.0.0/8")).build();
    v.setStaticRoutes(ImmutableSortedSet.of(nullRoute));
    SortedMap<String, Configuration> configs = ImmutableSortedMap.of(c.getHostname(), c);
    MockRib mockRib =
        MockRib.builder()
            .setRoutes(ImmutableSet.of(nullRoute))
            .setRoutableIps(ipSpace)
            .setMatchingIps(ImmutableMap.of(Prefix.parse("1.0.0.0/8"), ipSpace))
            .build();
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

    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(c.getHostname(), ImmutableSortedMap.of(v.getName(), mockRib));
    Map<String, Map<String, Fib>> fibs =
        ImmutableMap.of(c.getHostname(), ImmutableMap.of(v.getName(), mockFib));

    ForwardingAnalysisImpl forwardingAnalysisImpl =
        new ForwardingAnalysisImpl(configs, ribs, fibs, new Topology(ImmutableSortedSet.of()));

    Map<String, Map<String, Map<String, IpSpace>>> neighborUnreachable =
        forwardingAnalysisImpl.getNeighborUnreachable();

    assertThat(
        neighborUnreachable,
        hasEntry(
            equalTo(c.getHostname()),
            hasEntry(equalTo(v.getName()), not(hasKey(Interface.NULL_INTERFACE_NAME)))));
  }

  @Test
  public void testComputeRoutableIps() {
    String c1 = "c1";
    String v1 = "v1";
    SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs =
        ImmutableSortedMap.of(
            c1, ImmutableSortedMap.of(v1, MockRib.builder().setRoutableIps(IPSPACE1).build()));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, IpSpace>> result = forwardingAnalysisImpl.computeRoutableIps(ribs);

    assertThat(result, equalTo(ImmutableMap.of(c1, ImmutableMap.of(v1, IPSPACE1))));
  }

  @Test
  public void testComputeRouteMatchConditions() {
    Set<AbstractRoute> routes =
        ImmutableSet.of(new ConnectedRoute(P1, INTERFACE1), new ConnectedRoute(P2, INTERFACE2));
    MockRib rib =
        MockRib.builder().setMatchingIps(ImmutableMap.of(P1, IPSPACE1, P2, IPSPACE2)).build();
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();

    /* Resulting IP space should permit matching IPs */
    assertThat(
        forwardingAnalysisImpl.computeRouteMatchConditions(routes, rib),
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
    AbstractRoute r1 = StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getStartIp()).build();
    AbstractRoute ifaceRoute = new ConnectedRoute(P2, i1);
    _routesWithNextHop =
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
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> result =
        forwardingAnalysisImpl.computeRoutesWhereDstIpCanBeArpIp(fibs);

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
    _routesWhereDstIpCanBeArpIp =
        ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1))));
    Edge e1 = new Edge(c1, i1, c2, i2);
    _arpReplies = ImmutableMap.of(c2, ImmutableMap.of(i2, P2.getStartIp().toIpSpace()));
    Topology topology = new Topology(ImmutableSortedSet.of(e1));
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
                                i1, ImmutableMap.of(r1.getNextHopIp(), ImmutableSet.of(r1)))))
                    .build()));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<Edge, Set<AbstractRoute>> result =
        forwardingAnalysisImpl.computeRoutesWithDestIpEdge(fibs, topology);

    assertThat(result, equalTo(ImmutableMap.of(e1, ImmutableSet.of(r1))));
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
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> result =
        forwardingAnalysisImpl.computeRoutesWithNextHop(fibs);

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
    AbstractRoute r1 = StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getStartIp()).build();
    _routesWithNextHop =
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
    _someoneReplies = ImmutableMap.of(c1, ImmutableMap.of(i1, P2.getEndIp().toIpSpace()));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, Map<String, Set<AbstractRoute>>>> result =
        forwardingAnalysisImpl.computeRoutesWithNextHopIpArpFalse(fibs);

    assertThat(
        result,
        equalTo(
            ImmutableMap.of(c1, ImmutableMap.of(v1, ImmutableMap.of(i1, ImmutableSet.of(r1))))));
  }

  @Test
  public void testComputeRoutesWithNextHopIpArpFalseForInterface() {
    String hostname = "c1";
    String outInterface = "i1";
    AbstractRoute nextHopIpRoute1 =
        StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getStartIp()).build();
    AbstractRoute nextHopIpRoute2 =
        StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getEndIp()).build();
    AbstractRoute ifaceRoute = new ConnectedRoute(P2, outInterface);
    Entry<String, Set<AbstractRoute>> routesWithNextHopByOutInterfaceEntry =
        Maps.immutableEntry(
            outInterface, ImmutableSet.of(nextHopIpRoute1, nextHopIpRoute2, ifaceRoute));
    _someoneReplies =
        ImmutableMap.of(hostname, ImmutableMap.of(outInterface, P2.getStartIp().toIpSpace()));
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
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Set<AbstractRoute> result =
        forwardingAnalysisImpl.computeRoutesWithNextHopIpArpFalseForInterface(
            fib, hostname, routesWithNextHopByOutInterfaceEntry);

    /*
     * Should only contain nextHopIpRoute1 since it is the only route with a next-hop-ip for which
     * there is no ARP reply.
     */
    assertThat(result, equalTo(ImmutableSet.of(nextHopIpRoute2)));
  }

  @Test
  public void testComputeRoutesWithNextHopIpArpFalseForInterfaceNoNeighbors() {
    String hostname = "c1";
    String outInterface = "i1";
    AbstractRoute nextHopIpRoute1 =
        StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getStartIp()).build();
    AbstractRoute nextHopIpRoute2 =
        StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getEndIp()).build();
    AbstractRoute ifaceRoute = new ConnectedRoute(P2, outInterface);
    Entry<String, Set<AbstractRoute>> routesWithNextHopByOutInterfaceEntry =
        Maps.immutableEntry(
            outInterface, ImmutableSet.of(nextHopIpRoute1, nextHopIpRoute2, ifaceRoute));
    _someoneReplies = ImmutableMap.of();
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
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Set<AbstractRoute> result =
        forwardingAnalysisImpl.computeRoutesWithNextHopIpArpFalseForInterface(
            fib, hostname, routesWithNextHopByOutInterfaceEntry);

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
    Edge e1 = new Edge(c1, i1, c2, i2);
    _arpReplies = ImmutableMap.of(c2, ImmutableMap.of(i2, P2.getStartIp().toIpSpace()));
    Topology topology = new Topology(ImmutableSortedSet.of(e1));
    String v1 = "v1";
    AbstractRoute r1 = StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getStartIp()).build();
    AbstractRoute r2 = StaticRoute.builder().setNetwork(P1).setNextHopIp(P2.getEndIp()).build();
    _routesWithNextHop =
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
    _someoneReplies = ImmutableMap.of(c1, ImmutableMap.of(i1, P2.getEndIp().toIpSpace()));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<Edge, Set<AbstractRoute>> result =
        forwardingAnalysisImpl.computeRoutesWithNextHopIpArpTrue(fibs, topology);

    /* Only the route with the next hop ip that gets a reply should be present. */
    assertThat(result, equalTo(ImmutableMap.of(e1, ImmutableSet.of(r1))));
  }

  @Test
  public void testComputeSomeoneReplies() {
    String c1 = "c1";
    String i1 = "i1";
    String c2 = "c2";
    String i2 = "i2";
    Edge e1 = new Edge(c1, i1, c2, i2);
    _arpReplies = ImmutableMap.of(c2, ImmutableMap.of(i2, P1.toIpSpace()));
    Topology topology = new Topology(ImmutableSortedSet.of(e1));
    ForwardingAnalysisImpl forwardingAnalysisImpl = initForwardingAnalysisImpl();
    Map<String, Map<String, IpSpace>> result =
        forwardingAnalysisImpl.computeSomeoneReplies(topology);

    /* IPs allowed by neighbor should appear */
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(i1), containsIp(P1.getStartIp()))));
    assertThat(result, hasEntry(equalTo(c1), hasEntry(equalTo(i1), containsIp(P1.getEndIp()))));
    /* IPs not allowed by neighbor should not appear */
    assertThat(
        result, hasEntry(equalTo(c1), hasEntry(equalTo(i1), not(containsIp(P2.getStartIp())))));
  }
}

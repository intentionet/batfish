package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasProtocol;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.batfish.common.BatfishLogger;
import org.batfish.common.plugin.DataPlanePlugin.ComputeDataPlaneResult;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.junit.Before;
import org.junit.Test;

public class RouteReflectionTest {

  private static final Prefix AS1_PREFIX = Prefix.parse("1.0.0.0/8");

  private static final Prefix AS3_PREFIX = Prefix.parse("3.0.0.0/8");

  private static final int EDGE_PREFIX_LENGTH = 24;

  private static final String EDGE1_NAME = "edge1";

  private static final String EDGE2_NAME = "edge2";

  private static final String RR_NAME = "rr";

  private static final String RR1_NAME = "rr1";

  private static final String RR2_NAME = "rr2";

  private static void assertIbgpRoute(
      SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode,
      String hostname,
      Prefix prefix) {
    assertThat(routesByNode, hasKey(hostname));
    SortedMap<String, SortedSet<AbstractRoute>> routesByVrf = routesByNode.get(hostname);
    assertThat(routesByVrf, hasKey(Configuration.DEFAULT_VRF_NAME));
    SortedSet<AbstractRoute> routes = routesByVrf.get(Configuration.DEFAULT_VRF_NAME);
    assertThat(routes, hasItem(hasPrefix(prefix)));
    AbstractRoute route =
        routes.stream().filter(r -> r.getNetwork().equals(prefix)).findAny().get();
    assertThat(route, hasProtocol(RoutingProtocol.IBGP));
  }

  private static void assertNoRoute(
      SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routesByNode,
      String hostname,
      Prefix prefix) {
    assertThat(routesByNode, hasKey(hostname));
    SortedMap<String, SortedSet<AbstractRoute>> routesByVrf = routesByNode.get(hostname);
    assertThat(routesByVrf, hasKey(Configuration.DEFAULT_VRF_NAME));
    SortedSet<AbstractRoute> routes = routesByVrf.get(Configuration.DEFAULT_VRF_NAME);
    assertThat(routes, not(hasItem(hasPrefix(prefix))));
  }

  private BgpAdvertisement.Builder _ab;

  private Configuration.Builder _cb;

  private RoutingPolicy.Builder _defaultExportPolicyBuilder;

  private Interface.Builder _ib;

  private BgpActivePeerConfig.Builder _nb;

  private NetworkFactory _nf;

  private RoutingPolicy.Builder _nullExportPolicyBuilder;

  private BgpProcess.Builder _pb;

  private Vrf.Builder _vb;

  /*
   * See documentation of calling functions for information description of produced network
   */
  private SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> generateRoutesOneReflector(
      boolean edge1RouteReflectorClient, boolean edge2RouteReflectorClient) {
    Ip as1PeeringIp = new Ip("10.12.11.1");
    Ip edge1EbgpIfaceIp = new Ip("10.12.11.2");
    Ip edge1IbgpIfaceIp = new Ip("10.1.12.1");
    Ip edge1LoopbackIp = new Ip("2.0.0.1");
    Ip rrEdge1IfaceIp = new Ip("10.1.12.2");
    Ip rrEdge2IfaceIp = new Ip("10.1.23.2");
    Ip rrLoopbackIp = new Ip("2.0.0.2");
    Ip as3PeeringIp = new Ip("10.23.31.3");
    Ip edge2EbgpIfaceIp = new Ip("10.23.31.2");
    Ip edge2IbgpIfaceIp = new Ip("10.1.23.3");
    Ip edge2LoopbackIp = new Ip("2.0.0.3");

    Configuration edge1 = _cb.setHostname(EDGE1_NAME).build();
    Vrf vEdge1 = _vb.setOwner(edge1).build();
    _ib.setOwner(edge1).setVrf(vEdge1);
    _ib.setAddress(new InterfaceAddress(edge1EbgpIfaceIp, EDGE_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(edge1LoopbackIp, Prefix.MAX_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(edge1IbgpIfaceIp, EDGE_PREFIX_LENGTH)).build();
    StaticRoute.Builder sb = StaticRoute.builder();
    vEdge1.setStaticRoutes(
        ImmutableSortedSet.of(
            sb.setNextHopIp(rrEdge1IfaceIp)
                .setNetwork(new Prefix(rrLoopbackIp, Prefix.MAX_PREFIX_LENGTH))
                .build()));
    BgpProcess edge1Proc = _pb.setRouterId(edge1LoopbackIp).setVrf(vEdge1).build();
    RoutingPolicy edge1EbgpExportPolicy = _nullExportPolicyBuilder.setOwner(edge1).build();
    _nb.setBgpProcess(edge1Proc)
        .setClusterId(edge1LoopbackIp.asLong())
        .setRemoteAs(1L)
        .setLocalIp(edge1EbgpIfaceIp)
        .setPeerAddress(as1PeeringIp)
        .setExportPolicy(edge1EbgpExportPolicy.getName())
        .build();
    RoutingPolicy edge1IbgpExportPolicy = _defaultExportPolicyBuilder.setOwner(edge1).build();
    _nb.setRemoteAs(2L)
        .setLocalIp(edge1LoopbackIp)
        .setPeerAddress(rrLoopbackIp)
        .setExportPolicy(edge1IbgpExportPolicy.getName())
        .build();

    Configuration rr = _cb.setHostname(RR_NAME).build();
    Vrf vRr = _vb.setOwner(rr).build();
    _ib.setOwner(rr).setVrf(vRr);
    _ib.setAddress(new InterfaceAddress(rrEdge1IfaceIp, EDGE_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(rrLoopbackIp, Prefix.MAX_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(rrEdge2IfaceIp, EDGE_PREFIX_LENGTH)).build();
    vRr.setStaticRoutes(
        ImmutableSortedSet.of(
            sb.setNextHopIp(edge1IbgpIfaceIp)
                .setNetwork(new Prefix(edge1LoopbackIp, Prefix.MAX_PREFIX_LENGTH))
                .build(),
            sb.setNextHopIp(edge2IbgpIfaceIp)
                .setNetwork(new Prefix(edge2LoopbackIp, Prefix.MAX_PREFIX_LENGTH))
                .build()));
    BgpProcess rrProc = _pb.setRouterId(rrLoopbackIp).setVrf(vRr).build();
    RoutingPolicy rrExportPolicy = _defaultExportPolicyBuilder.setOwner(rr).build();
    _nb.setBgpProcess(rrProc)
        .setClusterId(rrLoopbackIp.asLong())
        .setRemoteAs(2L)
        .setLocalIp(rrLoopbackIp)
        .setExportPolicy(rrExportPolicy.getName());
    if (edge1RouteReflectorClient) {
      _nb.setRouteReflectorClient(true);
    }
    _nb.setPeerAddress(edge1LoopbackIp).build();
    _nb.setRouteReflectorClient(false);
    if (edge2RouteReflectorClient) {
      _nb.setRouteReflectorClient(true);
    }
    _nb.setPeerAddress(edge2LoopbackIp).build();
    _nb.setRouteReflectorClient(false);

    Configuration edge2 = _cb.setHostname(EDGE2_NAME).build();
    Vrf vEdge2 = _vb.setOwner(edge2).build();
    _ib.setOwner(edge2).setVrf(vEdge2);
    _ib.setAddress(new InterfaceAddress(edge2EbgpIfaceIp, EDGE_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(edge2LoopbackIp, Prefix.MAX_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(edge2IbgpIfaceIp, EDGE_PREFIX_LENGTH)).build();
    vEdge2.setStaticRoutes(
        ImmutableSortedSet.of(
            sb.setNextHopIp(rrEdge2IfaceIp)
                .setNetwork(new Prefix(rrLoopbackIp, Prefix.MAX_PREFIX_LENGTH))
                .build()));
    BgpProcess edge2Proc = _pb.setRouterId(edge2LoopbackIp).setVrf(vEdge2).build();
    RoutingPolicy edge2EbgpExportPolicy = _nullExportPolicyBuilder.setOwner(edge2).build();
    _nb.setBgpProcess(edge2Proc)
        .setClusterId(edge2LoopbackIp.asLong())
        .setRemoteAs(3L)
        .setLocalIp(edge2EbgpIfaceIp)
        .setPeerAddress(as3PeeringIp)
        .setExportPolicy(edge2EbgpExportPolicy.getName())
        .build();
    RoutingPolicy edge2IbgpExportPolicy = _defaultExportPolicyBuilder.setOwner(edge2).build();
    _nb.setRemoteAs(2L)
        .setLocalIp(edge2LoopbackIp)
        .setPeerAddress(rrLoopbackIp)
        .setExportPolicy(edge2IbgpExportPolicy.getName())
        .build();

    SortedMap<String, Configuration> configurations =
        new ImmutableSortedMap.Builder<String, Configuration>(String::compareTo)
            .put(edge1.getHostname(), edge1)
            .put(rr.getHostname(), rr)
            .put(edge2.getHostname(), edge2)
            .build();
    IncrementalBdpEngine engine =
        new IncrementalBdpEngine(
            new IncrementalDataPlaneSettings(),
            new BatfishLogger(BatfishLogger.LEVELSTR_OUTPUT, false),
            (s, i) -> new AtomicInteger());
    Topology topology = CommonUtil.synthesizeTopology(configurations);
    ComputeDataPlaneResult dpResult =
        engine.computeDataPlane(
            false,
            configurations,
            topology,
            ImmutableSet.of(
                _ab.setAsPath(AsPath.ofSingletonAsSets(1L))
                    .setDstIp(edge1EbgpIfaceIp)
                    .setDstNode(edge1.getHostname())
                    .setNetwork(AS1_PREFIX)
                    .setNextHopIp(as1PeeringIp)
                    .setOriginatorIp(as1PeeringIp)
                    .setSrcIp(as1PeeringIp)
                    .setSrcNode("as1Edge")
                    .build(),
                _ab.setAsPath(AsPath.ofSingletonAsSets(3L))
                    .setDstIp(edge2EbgpIfaceIp)
                    .setDstNode(edge2.getHostname())
                    .setNetwork(AS3_PREFIX)
                    .setNextHopIp(as3PeeringIp)
                    .setOriginatorIp(as3PeeringIp)
                    .setSrcIp(as3PeeringIp)
                    .setSrcNode("as3Edge")
                    .build()));
    return IncrementalBdpEngine.getRoutes((IncrementalDataPlane) dpResult._dataPlane);
  }

  private SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>>
      generateRoutesTwoReflectors(boolean useSameClusterIds) {
    Ip as1PeeringIp = new Ip("10.12.11.1");
    Ip edge1EbgpIfaceIp = new Ip("10.12.11.2");
    Ip edge1IbgpIfaceIp = new Ip("10.1.12.1");
    Ip edge1LoopbackIp = new Ip("2.0.0.1");
    Ip rr1Edge1IfaceIp = new Ip("10.1.12.2");
    Ip rr1Rr2IfaceIp = new Ip("10.1.23.2");
    Ip rr1LoopbackIp = new Ip("2.0.0.2");
    Ip rr2IbgpIfaceIp = new Ip("10.1.23.3");
    Ip rr2LoopbackIp = new Ip("2.0.0.3");

    StaticRoute.Builder sb = StaticRoute.builder();
    Configuration edge1 = _cb.setHostname(EDGE1_NAME).build();
    Vrf vEdge1 = _vb.setOwner(edge1).build();
    _ib.setOwner(edge1).setVrf(vEdge1);
    _ib.setAddress(new InterfaceAddress(edge1EbgpIfaceIp, EDGE_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(edge1LoopbackIp, Prefix.MAX_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(edge1IbgpIfaceIp, EDGE_PREFIX_LENGTH)).build();
    vEdge1.setStaticRoutes(
        ImmutableSortedSet.of(
            sb.setNextHopIp(rr1Edge1IfaceIp)
                .setNetwork(new Prefix(rr1LoopbackIp, Prefix.MAX_PREFIX_LENGTH))
                .build()));
    BgpProcess edge1Proc = _pb.setRouterId(edge1LoopbackIp).setVrf(vEdge1).build();
    RoutingPolicy edge1EbgpExportPolicy = _nullExportPolicyBuilder.setOwner(edge1).build();
    _nb.setBgpProcess(edge1Proc)
        .setClusterId(edge1LoopbackIp.asLong())
        .setRemoteAs(1L)
        .setLocalIp(edge1EbgpIfaceIp)
        .setPeerAddress(as1PeeringIp)
        .setExportPolicy(edge1EbgpExportPolicy.getName())
        .build();
    RoutingPolicy edge1IbgpExportPolicy = _defaultExportPolicyBuilder.setOwner(edge1).build();
    _nb.setRemoteAs(2L)
        .setLocalIp(edge1LoopbackIp)
        .setPeerAddress(rr1LoopbackIp)
        .setExportPolicy(edge1IbgpExportPolicy.getName())
        .build();

    Configuration rr1 = _cb.setHostname(RR1_NAME).build();
    Vrf vRr1 = _vb.setOwner(rr1).build();
    _ib.setOwner(rr1).setVrf(vRr1);
    _ib.setAddress(new InterfaceAddress(rr1Edge1IfaceIp, EDGE_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(rr1LoopbackIp, Prefix.MAX_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(rr1Rr2IfaceIp, EDGE_PREFIX_LENGTH)).build();
    vRr1.setStaticRoutes(
        ImmutableSortedSet.of(
            sb.setNextHopIp(edge1IbgpIfaceIp)
                .setNetwork(new Prefix(edge1LoopbackIp, Prefix.MAX_PREFIX_LENGTH))
                .build(),
            sb.setNextHopIp(rr2IbgpIfaceIp)
                .setNetwork(new Prefix(rr2LoopbackIp, Prefix.MAX_PREFIX_LENGTH))
                .build()));
    BgpProcess rr1Proc = _pb.setRouterId(rr1LoopbackIp).setVrf(vRr1).build();
    RoutingPolicy rr1ExportPolicy = _defaultExportPolicyBuilder.setOwner(rr1).build();
    _nb.setBgpProcess(rr1Proc)
        .setClusterId(rr1LoopbackIp.asLong())
        .setRemoteAs(2L)
        .setLocalIp(rr1LoopbackIp)
        .setExportPolicy(rr1ExportPolicy.getName())
        .setRouteReflectorClient(true)
        .setPeerAddress(edge1LoopbackIp)
        .build();
    _nb.setRouteReflectorClient(false).setPeerAddress(rr2LoopbackIp).build();

    Configuration rr2 = _cb.setHostname(RR2_NAME).build();
    Vrf vRr2 = _vb.setOwner(rr2).build();
    _ib.setOwner(rr2).setVrf(vRr2);
    _ib.setAddress(new InterfaceAddress(rr2LoopbackIp, Prefix.MAX_PREFIX_LENGTH)).build();
    _ib.setAddress(new InterfaceAddress(rr2IbgpIfaceIp, EDGE_PREFIX_LENGTH)).build();
    vRr2.setStaticRoutes(
        ImmutableSortedSet.of(
            sb.setNextHopIp(rr1Rr2IfaceIp)
                .setNetwork(new Prefix(rr1LoopbackIp, Prefix.MAX_PREFIX_LENGTH))
                .build()));
    BgpProcess rr2Proc = _pb.setRouterId(rr2LoopbackIp).setVrf(vRr2).build();
    RoutingPolicy edge2IbgpExportPolicy = _defaultExportPolicyBuilder.setOwner(rr2).build();

    Ip rr2ClusterIdForRr1 = useSameClusterIds ? rr1LoopbackIp : rr2LoopbackIp;
    _nb.setBgpProcess(rr2Proc)
        .setClusterId(rr2ClusterIdForRr1.asLong())
        .setLocalIp(rr2LoopbackIp)
        .setPeerAddress(rr1LoopbackIp)
        .setRouteReflectorClient(true)
        .setExportPolicy(edge2IbgpExportPolicy.getName())
        .build();

    SortedMap<String, Configuration> configurations =
        new ImmutableSortedMap.Builder<String, Configuration>(String::compareTo)
            .put(edge1.getHostname(), edge1)
            .put(rr1.getHostname(), rr1)
            .put(rr2.getHostname(), rr2)
            .build();
    IncrementalBdpEngine engine =
        new IncrementalBdpEngine(
            new IncrementalDataPlaneSettings(),
            new BatfishLogger(BatfishLogger.LEVELSTR_OUTPUT, false),
            (s, i) -> new AtomicInteger());
    Topology topology = CommonUtil.synthesizeTopology(configurations);
    IncrementalDataPlane dp =
        (IncrementalDataPlane)
            engine.computeDataPlane(
                    false,
                    configurations,
                    topology,
                    ImmutableSet.of(
                        _ab.setAsPath(AsPath.ofSingletonAsSets(1L))
                            .setDstIp(edge1EbgpIfaceIp)
                            .setDstNode(edge1.getHostname())
                            .setNetwork(AS1_PREFIX)
                            .setNextHopIp(as1PeeringIp)
                            .setOriginatorIp(as1PeeringIp)
                            .setSrcIp(as1PeeringIp)
                            .setSrcNode("as1Edge")
                            .build()))
                ._dataPlane;
    return IncrementalBdpEngine.getRoutes(dp);
  }

  /** Initialize builders with values common to all tests */
  @Before
  public void setup() {
    _ab =
        new BgpAdvertisement.Builder()
            .setClusterList(ImmutableSortedSet.of())
            .setCommunities(ImmutableSortedSet.of())
            .setDstVrf(Configuration.DEFAULT_VRF_NAME)
            .setOriginType(OriginType.INCOMPLETE)
            .setSrcProtocol(RoutingProtocol.AGGREGATE)
            .setSrcVrf(Configuration.DEFAULT_VRF_NAME)
            .setType(BgpAdvertisementType.EBGP_SENT);
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    _ib = _nf.interfaceBuilder().setActive(true);
    _nb = _nf.bgpNeighborBuilder().setLocalAs(2L);
    _pb = _nf.bgpProcessBuilder();
    _vb = _nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    If acceptIffBgp = new If();
    Disjunction guard = new Disjunction();
    guard.setDisjuncts(
        ImmutableList.of(
            new MatchProtocol(RoutingProtocol.BGP), new MatchProtocol(RoutingProtocol.IBGP)));
    acceptIffBgp.setGuard(guard);
    acceptIffBgp.setTrueStatements(ImmutableList.of(Statements.ExitAccept.toStaticStatement()));
    acceptIffBgp.setFalseStatements(ImmutableList.of(Statements.ExitReject.toStaticStatement()));

    /* Builder that creates default BGP export policy */
    _defaultExportPolicyBuilder =
        _nf.routingPolicyBuilder().setStatements(ImmutableList.of(acceptIffBgp));

    /* Builder that creates BGP export policy that rejects everything */
    _nullExportPolicyBuilder =
        _nf.routingPolicyBuilder()
            .setStatements(ImmutableList.of(Statements.ExitReject.toStaticStatement()));
  }

  /*
   * AS1 |                                            AS2
   *       edge1(client of rr, CID: rr1-loopback) <=> rr1(client of rr2, CID: rr2-loopback) <=> rr2
   */
  @Test
  public void testAcceptDifferentCluster() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        generateRoutesTwoReflectors(false);

    assertIbgpRoute(routes, RR1_NAME, AS1_PREFIX);
    assertIbgpRoute(routes, RR2_NAME, AS1_PREFIX);
  }

  /*
   * AS1 |           AS2                      | AS3
   *       edge1 <=> rr(no clients) <=> edge2
   */
  @Test
  public void testNoRouteReflection() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        generateRoutesOneReflector(false, false);

    assertNoRoute(routes, EDGE1_NAME, AS3_PREFIX);
    assertIbgpRoute(routes, RR_NAME, AS1_PREFIX);
    assertIbgpRoute(routes, RR_NAME, AS3_PREFIX);
    assertNoRoute(routes, EDGE2_NAME, AS1_PREFIX);
  }

  /*
   * AS1 |                                            AS2
   *       edge1(client of rr, CID: rr1-loopback) <=> rr1(client of rr2, CID: rr1-loopback) <=> rr2
   */
  @Test
  public void testRejectSameCluster() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        generateRoutesTwoReflectors(true);

    assertIbgpRoute(routes, RR1_NAME, AS1_PREFIX);
    assertNoRoute(routes, RR2_NAME, AS1_PREFIX);
  }

  /*
   * AS1 |                   AS2          | AS3
   *       edge1(client) <=> rr <=> edge2
   */
  @Test
  public void testSingleReflectorOneClient() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        generateRoutesOneReflector(true, false);

    assertIbgpRoute(routes, EDGE1_NAME, AS3_PREFIX);
    assertIbgpRoute(routes, RR_NAME, AS1_PREFIX);
    assertIbgpRoute(routes, RR_NAME, AS3_PREFIX);
    assertIbgpRoute(routes, EDGE2_NAME, AS1_PREFIX);
  }

  /*
   * AS1 |                  AS2                   | AS3
   *       edge1(client) <=> rr <=> (client)edge2
   */
  @Test
  public void testSingleReflectorTwoClients() {
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        generateRoutesOneReflector(true, true);

    assertIbgpRoute(routes, EDGE1_NAME, AS3_PREFIX);
    assertIbgpRoute(routes, RR_NAME, AS1_PREFIX);
    assertIbgpRoute(routes, RR_NAME, AS3_PREFIX);
    assertIbgpRoute(routes, EDGE2_NAME, AS1_PREFIX);
  }
}

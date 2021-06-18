package org.batfish.representation.cumulus;

import static com.google.common.base.MoreObjects.firstNonNull;
import static junit.framework.TestCase.assertNotNull;
import static org.batfish.common.matchers.WarningMatchers.hasText;
import static org.batfish.common.matchers.WarningsMatchers.hasRedFlags;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.InterfaceType.LOOPBACK;
import static org.batfish.datamodel.InterfaceType.PHYSICAL;
import static org.batfish.datamodel.RoutingProtocol.BGP;
import static org.batfish.datamodel.RoutingProtocol.IBGP;
import static org.batfish.datamodel.bgp.AllowRemoteAsOutMode.ALWAYS;
import static org.batfish.datamodel.matchers.BgpRouteMatchers.hasCommunities;
import static org.batfish.datamodel.routing_policy.statement.Statements.ExitAccept;
import static org.batfish.representation.cumulus.CumulusConcatenatedConfiguration.LOOPBACK_INTERFACE_NAME;
import static org.batfish.representation.cumulus.CumulusConversions.DEFAULT_MAX_MED;
import static org.batfish.representation.cumulus.CumulusConversions.GENERATED_DEFAULT_ROUTE;
import static org.batfish.representation.cumulus.CumulusConversions.REJECT_DEFAULT_ROUTE;
import static org.batfish.representation.cumulus.CumulusConversions.addBgpNeighbor;
import static org.batfish.representation.cumulus.CumulusConversions.addOspfInterfaces;
import static org.batfish.representation.cumulus.CumulusConversions.computeBgpCommonExportPolicyName;
import static org.batfish.representation.cumulus.CumulusConversions.computeBgpGenerationPolicyName;
import static org.batfish.representation.cumulus.CumulusConversions.computeBgpNeighborImportRoutingPolicy;
import static org.batfish.representation.cumulus.CumulusConversions.computeBgpPeerExportPolicyName;
import static org.batfish.representation.cumulus.CumulusConversions.computeLocalIpForBgpNeighbor;
import static org.batfish.representation.cumulus.CumulusConversions.computeMatchSuppressedSummaryOnlyPolicyName;
import static org.batfish.representation.cumulus.CumulusConversions.computeOspfAreas;
import static org.batfish.representation.cumulus.CumulusConversions.computeOspfExportPolicyName;
import static org.batfish.representation.cumulus.CumulusConversions.convertIpv4UnicastAddressFamily;
import static org.batfish.representation.cumulus.CumulusConversions.convertOspfRedistributionPolicy;
import static org.batfish.representation.cumulus.CumulusConversions.convertVxlans;
import static org.batfish.representation.cumulus.CumulusConversions.generateBgpAggregates;
import static org.batfish.representation.cumulus.CumulusConversions.generateBgpCommonPeerConfig;
import static org.batfish.representation.cumulus.CumulusConversions.generateExportAggregateConditions;
import static org.batfish.representation.cumulus.CumulusConversions.generateGenerationPolicy;
import static org.batfish.representation.cumulus.CumulusConversions.getSetMaxMedMetric;
import static org.batfish.representation.cumulus.CumulusConversions.getSetNextHop;
import static org.batfish.representation.cumulus.CumulusConversions.inferClusterId;
import static org.batfish.representation.cumulus.CumulusConversions.inferPeerIp;
import static org.batfish.representation.cumulus.CumulusConversions.inferRouterId;
import static org.batfish.representation.cumulus.CumulusConversions.resolveLocalIpFromUpdateSource;
import static org.batfish.representation.cumulus.CumulusConversions.suppressSummarizedPrefixes;
import static org.batfish.representation.cumulus.CumulusConversions.toAsPathAccessList;
import static org.batfish.representation.cumulus.CumulusConversions.toBgpProcess;
import static org.batfish.representation.cumulus.CumulusConversions.toOspfProcess;
import static org.batfish.representation.cumulus.CumulusConversions.toRouteFilterLine;
import static org.batfish.representation.cumulus.CumulusConversions.toRouteFilterList;
import static org.batfish.representation.cumulus.CumulusConversions.toRouteTarget;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsPathAccessList;
import org.batfish.datamodel.AsPathAccessListLine;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Bgpv4Route.Builder;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.Layer2VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.route.nh.NextHopDiscard;
import org.batfish.datamodel.routing_policy.Common;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.CommunityIs;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.HasCommunity;
import org.batfish.datamodel.routing_policy.communities.InputCommunities;
import org.batfish.datamodel.routing_policy.communities.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.communities.MatchCommunities;
import org.batfish.datamodel.routing_policy.communities.SetCommunities;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralLong;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetMetric;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.representation.cumulus.BgpNeighbor.RemoteAs;
import org.batfish.vendor.VendorStructureId;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link CumulusConversions}. */
public final class CumulusConversionsTest {
  private NetworkFactory _nf;
  private Configuration _c;
  private Vrf _v;

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _c = _nf.configurationBuilder().build();
    _v = _nf.vrfBuilder().setOwner(_c).build();
  }

  private Environment finalEnvironment(Statement statement, String network) {
    RoutingPolicy policy =
        _nf.routingPolicyBuilder().setOwner(_c).setStatements(ImmutableList.of(statement)).build();
    Environment env =
        Environment.builder(_c)
            .setOriginalRoute(
                Bgpv4Route.testBuilder()
                    .setNetwork(Prefix.parse(network))
                    // Only network matters for these tests, but Bgp4Route requires these have
                    // values.
                    .setOriginatorIp(Ip.parse("1.1.1.1"))
                    .setOriginType(OriginType.IGP)
                    .setProtocol(RoutingProtocol.BGP)
                    .build())
            .build();
    policy.call(env);
    return env;
  }

  private boolean value(BooleanExpr expr, Environment env) {
    RoutingPolicy policy =
        _nf.routingPolicyBuilder()
            .setOwner(_c)
            .setStatements(
                ImmutableList.of(
                    new If(
                        expr,
                        ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
                        ImmutableList.of(Statements.ReturnFalse.toStaticStatement()))))
            .build();
    return policy.call(env).getBooleanValue();
  }

  private boolean value(RoutingPolicy policy, String network) {
    return value(policy, Prefix.parse(network));
  }

  /**
   * Evaluate the policy in an environment with the original route being a static route with the
   * input network.
   */
  private boolean value(RoutingPolicy policy, Prefix network) {
    return policy
        .call(
            Environment.builder(_c)
                .setOriginalRoute(
                    StaticRoute.testBuilder().setAdministrativeCost(0).setNetwork(network).build())
                .build())
        .getBooleanValue();
  }

  @Test
  public void testGenerateExportAggregateConditions() {
    BooleanExpr booleanExpr =
        generateExportAggregateConditions(
            ImmutableMap.of(
                Prefix.parse("1.2.3.0/24"),
                new BgpVrfAddressFamilyAggregateNetworkConfiguration()));

    // longer not exported
    {
      Environment env =
          Environment.builder(_c)
              .setOriginalRoute(
                  GeneratedRoute.builder().setNetwork(Prefix.parse("1.2.3.4/32")).build())
              .build();
      assertFalse(value(booleanExpr, env));
    }

    // shorter not exported
    {
      Environment env =
          Environment.builder(_c)
              .setOriginalRoute(
                  GeneratedRoute.builder().setNetwork(Prefix.parse("1.2.0.0/16")).build())
              .build();
      assertFalse(value(booleanExpr, env));
    }

    // exact match exported
    {
      Environment env =
          Environment.builder(_c)
              .setOriginalRoute(
                  GeneratedRoute.builder().setNetwork(Prefix.parse("1.2.3.0/24")).build())
              .build();
      assertTrue(value(booleanExpr, env));
    }
  }

  @Test
  public void testGenerateGeneratedRoutes() {
    Prefix prefix = Prefix.parse("1.2.3.0/24");
    generateBgpAggregates(
        _c, _v, ImmutableMap.of(prefix, new BgpVrfAddressFamilyAggregateNetworkConfiguration()));
    String policyName = computeBgpGenerationPolicyName(true, _v.getName(), prefix.toString());

    // configuration has the generation policy
    assertThat(_c.getRoutingPolicies(), Matchers.hasKey(policyName));

    // vrf has generated route
    ImmutableList<GeneratedRoute> grs =
        _v.getGeneratedRoutes().stream()
            .filter(gr -> gr.getNetwork().equals(prefix))
            .collect(ImmutableList.toImmutableList());
    assertThat(grs, hasSize(1));

    GeneratedRoute gr = grs.get(0);
    assertTrue(gr.getDiscard());
    assertThat(gr.getGenerationPolicy(), equalTo(policyName));
  }

  @Test
  public void testGenerateGenerationPolicy() {
    Prefix prefix = Prefix.parse("1.2.3.0/24");
    generateGenerationPolicy(_c, _v.getName(), prefix);

    RoutingPolicy policy =
        _c.getRoutingPolicies()
            .get(computeBgpGenerationPolicyName(true, _v.getName(), prefix.toString()));

    assertTrue(value(policy, "1.2.3.4/32"));
    assertFalse(value(policy, "1.2.3.0/24"));
    assertFalse(value(policy, "1.2.0.0/16"));
  }

  @Test
  public void testSuppressSummarizedPrefixes() {
    Prefix suppressedPrefix = Prefix.parse("1.2.3.0/24");
    If stmt = suppressSummarizedPrefixes(_c, _v.getName(), Stream.of(suppressedPrefix));
    assertNotNull(stmt);
    assertTrue(firstNonNull(finalEnvironment(stmt, "1.2.3.4/32").getSuppressed(), false));
    assertFalse(firstNonNull(finalEnvironment(stmt, "1.2.3.0/24").getSuppressed(), false));
    assertFalse(firstNonNull(finalEnvironment(stmt, "1.2.0.0/16").getSuppressed(), false));
  }

  @Test
  public void testToRouteFilterLine() {
    IpPrefixListLine prefixListLine =
        new IpPrefixListLine(
            LineAction.PERMIT, 10, Prefix.parse("10.0.0.1/24"), new SubRange(27, 30));

    RouteFilterLine rfl = toRouteFilterLine(prefixListLine);
    assertThat(
        rfl,
        equalTo(
            new RouteFilterLine(
                LineAction.PERMIT, Prefix.parse("10.0.0.1/24"), new SubRange(27, 30))));
  }

  @Test
  public void testToRouteFilter() {
    IpPrefixList prefixList = new IpPrefixList("name");
    prefixList
        .getLines()
        .put(
            10L,
            new IpPrefixListLine(
                LineAction.DENY, 10, Prefix.parse("10.0.0.1/24"), new SubRange(27, 30)));
    prefixList
        .getLines()
        .put(
            20L,
            new IpPrefixListLine(
                LineAction.PERMIT, 20, Prefix.parse("10.0.2.1/24"), new SubRange(28, 31)));

    RouteFilterList rfl = toRouteFilterList(prefixList, "file");

    assertThat(
        rfl,
        equalTo(
            new RouteFilterList(
                "name",
                ImmutableList.of(
                    new RouteFilterLine(
                        LineAction.DENY, Prefix.parse("10.0.0.1/24"), new SubRange(27, 30)),
                    new RouteFilterLine(
                        LineAction.PERMIT, Prefix.parse("10.0.2.1/24"), new SubRange(28, 31))))));

    assertThat(
        rfl.getVendorStructureId(),
        equalTo(
            new VendorStructureId(
                "file", CumulusStructureType.IP_PREFIX_LIST.getDescription(), "name")));
  }

  @Test
  public void testToAsPathAccessList() {
    long permitted = 11111;
    long denied = 22222;
    IpAsPathAccessList asPathAccessList = new IpAsPathAccessList("name");
    asPathAccessList.addLine(new IpAsPathAccessListLine(LineAction.DENY, denied));
    asPathAccessList.addLine(new IpAsPathAccessListLine(LineAction.PERMIT, permitted));
    AsPathAccessList viList = toAsPathAccessList(asPathAccessList);

    // Cache initialization only happens in AsPathAccessList on deserialization o.O
    viList = SerializationUtils.clone(viList);

    List<AsPathAccessListLine> expectedViLines =
        ImmutableList.of(
            new AsPathAccessListLine(LineAction.DENY, String.format("(^| )%s($| )", denied)),
            new AsPathAccessListLine(LineAction.PERMIT, String.format("(^| )%s($| )", permitted)));
    assertThat(viList, equalTo(new AsPathAccessList("name", expectedViLines)));

    // Matches paths containing permitted ASN
    long other = 33333;
    assertTrue(viList.permits(AsPath.ofSingletonAsSets(permitted)));
    assertTrue(viList.permits(AsPath.ofSingletonAsSets(permitted, other)));
    assertTrue(viList.permits(AsPath.ofSingletonAsSets(other, permitted)));
    assertTrue(viList.permits(AsPath.ofSingletonAsSets(other, permitted, other)));

    // Does not match if denied ASN is in path, even if permitted is also there
    assertFalse(viList.permits(AsPath.ofSingletonAsSets(denied)));
    assertFalse(viList.permits(AsPath.ofSingletonAsSets(denied, permitted)));
    assertFalse(viList.permits(AsPath.ofSingletonAsSets(permitted, denied)));
    assertFalse(viList.permits(AsPath.ofSingletonAsSets(permitted, denied, permitted)));

    // Does not match by default
    assertFalse(viList.permits(AsPath.ofSingletonAsSets(other)));
  }

  @Test
  public void testGetAcceptStatements() {
    BgpNeighbor bgpNeighbor = new BgpIpNeighbor("10.0.0.1", Ip.parse("10.0.0.1"));

    {
      // if no local-as set, do not set next-hop-self
      SetNextHop setNextHop = getSetNextHop(bgpNeighbor, null);
      assertNull(setNextHop);
    }

    {
      // if is not ibgp, do not set next-hop-self
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      SetNextHop setNextHop = getSetNextHop(bgpNeighbor, 20000L);
      assertNull(setNextHop);
    }

    {
      // if is ibgp but no address family set, do not set next-hop-self
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      SetNextHop setNextHop = getSetNextHop(bgpNeighbor, 10000L);
      assertNull(setNextHop);
    }

    {
      // if is ibgp but neighbor configuration does not have next-hop-self set, do not set
      // next-hop-self
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      bgpNeighbor.setIpv4UnicastAddressFamily(new BgpNeighborIpv4UnicastAddressFamily());

      assertNull(getSetNextHop(bgpNeighbor, 10000L));
    }

    {
      // if is ibgp and neighbor configuration has next-hop-self set, then getSetNextHop should
      // return null.
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      BgpNeighborIpv4UnicastAddressFamily ipv4af = new BgpNeighborIpv4UnicastAddressFamily();
      bgpNeighbor.setIpv4UnicastAddressFamily(ipv4af);
      ipv4af.setNextHopSelf(true);
      assertNull(getSetNextHop(bgpNeighbor, 10000L));
    }
  }

  @Test
  public void testComputeLocalIpForBgpNeighbor() {
    Ip remoteIp = Ip.parse("1.1.1.1");
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    String vrfName = "vrf";
    Vrf vrf = nf.vrfBuilder().setName(vrfName).setOwner(c).build();
    org.batfish.datamodel.Interface.Builder ib = nf.interfaceBuilder().setVrf(vrf);

    // Should not accept interface whose subnet doesn't include the remote IP
    InterfaceAddress addr1 = ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 24);
    ib.setType(PHYSICAL).setOwner(c).setName("i1").setAddress(addr1).build();
    assertNull(computeLocalIpForBgpNeighbor(remoteIp, c, vrfName));

    // Should not accept interface that owns the remote IP
    InterfaceAddress addr2 = ConcreteInterfaceAddress.create(remoteIp, 24);
    ib.setType(PHYSICAL).setOwner(c).setName("i2").setAddress(addr2).build();
    assertNull(computeLocalIpForBgpNeighbor(remoteIp, c, vrfName));

    // Should accept interface that doesn't own the remote IP but whose subnet does include
    // it
    Ip ifaceIp = Ip.parse("1.1.1.2");
    InterfaceAddress addr3 = ConcreteInterfaceAddress.create(ifaceIp, 24);
    ib.setType(PHYSICAL).setOwner(c).setName("i3").setAddress(addr3).build();
    assertThat(computeLocalIpForBgpNeighbor(remoteIp, c, vrfName), equalTo(ifaceIp));
  }

  @Test
  public void testComputeLocalIpForBgpNeighbor_vrf() {
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    Vrf vrf1 = nf.vrfBuilder().setOwner(c).build();
    Vrf vrf2 = nf.vrfBuilder().setOwner(c).build();

    org.batfish.datamodel.Interface.Builder ib =
        nf.interfaceBuilder().setOwner(c).setType(PHYSICAL);
    ib.setVrf(vrf1).setAddress(ConcreteInterfaceAddress.parse("1.1.1.1/24")).build();
    ib.setVrf(vrf2).setAddress(ConcreteInterfaceAddress.parse("2.2.2.2/24")).build();

    Ip remoteIp = Ip.parse("1.1.1.3");

    // vrf1 owns the compatible localIp = 1.1.1.1
    assertThat(
        computeLocalIpForBgpNeighbor(remoteIp, c, vrf1.getName()), equalTo(Ip.parse("1.1.1.1")));

    // vrf2 does not own a compatible localIp
    assertNull(computeLocalIpForBgpNeighbor(remoteIp, c, vrf2.getName()));
  }

  @Test
  public void testToBgpProcess_aggregateRoutes() {
    testToBgpProcess_aggregateRoutes(true);
    testToBgpProcess_aggregateRoutes(false);
  }

  private static void testToBgpProcess_aggregateRoutes(boolean summaryOnly) {
    // setup VI model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    Vrf viVrf = nf.vrfBuilder().setOwner(viConfig).setName(DEFAULT_VRF_NAME).build();

    // setup VS model
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    BgpProcess bgpProcess = new BgpProcess();
    vsConfig.getFrrConfiguration().setBgpProcess(bgpProcess);

    // setup BgpVrf
    BgpVrf vrf = bgpProcess.getDefaultVrf();
    vrf.setRouterId(Ip.parse("1.1.1.1"));
    BgpIpv4UnicastAddressFamily ipv4Unicast = new BgpIpv4UnicastAddressFamily();
    Prefix prefix = Prefix.parse("1.2.3.0/24");

    BgpVrfAddressFamilyAggregateNetworkConfiguration agg =
        new BgpVrfAddressFamilyAggregateNetworkConfiguration();
    agg.setSummaryOnly(summaryOnly);

    ipv4Unicast.getAggregateNetworks().put(prefix, agg);
    vrf.setIpv4Unicast(ipv4Unicast);

    // the method under test
    toBgpProcess(viConfig, vsConfig, DEFAULT_VRF_NAME, vrf);

    // generation policy exists
    assertThat(
        viConfig.getRoutingPolicies(),
        hasKey(computeBgpGenerationPolicyName(true, DEFAULT_VRF_NAME, prefix.toString())));

    // generated route exists
    assertTrue(viVrf.getGeneratedRoutes().stream().anyMatch(gr -> gr.getNetwork().equals(prefix)));

    if (summaryOnly) {
      // suppress summary only filter list exists
      assertThat(
          viConfig.getRouteFilterLists(),
          hasKey(computeMatchSuppressedSummaryOnlyPolicyName(viVrf.getName())));
    } else {
      // suppress summary only filter list does not exist
      assertThat(
          viConfig.getRouteFilterLists(),
          not(hasKey(computeMatchSuppressedSummaryOnlyPolicyName(viVrf.getName()))));
    }
  }

  /**
   * Test that networks statements at BGP VRF level (outside of ipv4 address family stanza) are
   * accounted for when the address family is active
   */
  @Test
  public void testToBgpProcess_vrfLevelNetworks_activeV4Family() {
    // setup VI model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    nf.vrfBuilder().setOwner(viConfig).setName(DEFAULT_VRF_NAME).build();

    // setup VS model
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    BgpProcess bgpProcess = new BgpProcess();
    vsConfig.getFrrConfiguration().setBgpProcess(bgpProcess);

    // setup BgpVrf
    Prefix prefix = Prefix.parse("1.2.3.0/24");
    BgpVrf vrf = bgpProcess.getDefaultVrf();
    vrf.setRouterId(Ip.parse("1.1.1.1"));
    vrf.addNetwork(new BgpNetwork(prefix));

    // the method under test
    org.batfish.datamodel.BgpProcess viBgp =
        toBgpProcess(viConfig, vsConfig, DEFAULT_VRF_NAME, vrf);

    // generation policy exists
    assertTrue(viBgp.getOriginationSpace().containsPrefix(prefix));

    // the prefix is allowed to leave
    AbstractRoute route = new ConnectedRoute(prefix, "dummy");
    assertTrue(
        viConfig
            .getRoutingPolicies()
            .get(computeBgpCommonExportPolicyName(DEFAULT_VRF_NAME))
            .process(
                route, Bgpv4Route.testBuilder().setNetwork(route.getNetwork()), Direction.OUT));
  }

  /** Test that networks statement routemaps are processed in VI. */
  @Test
  public void testToBgpProcess_vrfLevelNetworks_withRouteMap() {
    // setup VI model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    nf.vrfBuilder().setOwner(viConfig).setName(DEFAULT_VRF_NAME).build();
    String networkRouteMapName = "NETWORK_RM";
    StandardCommunity community = StandardCommunity.of(1);
    RoutingPolicy.builder()
        .setName(networkRouteMapName)
        .setOwner(viConfig)
        .setStatements(
            ImmutableList.of(
                new SetCommunities(new LiteralCommunitySet(CommunitySet.of(community))),
                ExitAccept.toStaticStatement()))
        .build();
    // setup VS model
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    BgpProcess bgpProcess = new BgpProcess();
    vsConfig.getFrrConfiguration().setBgpProcess(bgpProcess);
    // Note that content of route map does not matter in VS land, only in VI land
    vsConfig.getRouteMaps().put(networkRouteMapName, new RouteMap(networkRouteMapName));

    // setup BgpVrf
    Prefix prefix = Prefix.parse("1.2.3.0/24");
    BgpVrf vrf = bgpProcess.getDefaultVrf();
    vrf.setRouterId(Ip.parse("1.1.1.1"));
    vrf.addNetwork(new BgpNetwork(prefix, networkRouteMapName));

    // the method under test. In charge of creating peer export policies.
    toBgpProcess(viConfig, vsConfig, DEFAULT_VRF_NAME, vrf);

    // the prefix is allowed to leave
    AbstractRoute route = new ConnectedRoute(prefix, "dummy");
    Builder builder = Bgpv4Route.testBuilder();
    assertTrue(
        viConfig
            .getRoutingPolicies()
            .get(computeBgpCommonExportPolicyName(DEFAULT_VRF_NAME))
            .process(route, builder.setNetwork(route.getNetwork()), Direction.OUT));
    assertThat(builder, hasCommunities(community));
  }

  /**
   * Test that networks statements at BGP VRF level (outside of ipv4 address family stanza) are
   * ignored when the address family is not active
   */
  @Test
  public void testToBgpProcess_vrfLevelNetworks_inactiveV4family() {
    // setup VI model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    nf.vrfBuilder().setOwner(viConfig).setName(DEFAULT_VRF_NAME).build();

    // setup VS model
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    BgpProcess bgpProcess = new BgpProcess();
    vsConfig.getFrrConfiguration().setBgpProcess(bgpProcess);

    // setup BgpVrf
    Prefix prefix = Prefix.parse("1.2.3.0/24");
    BgpVrf vrf = bgpProcess.getDefaultVrf();
    vrf.setRouterId(Ip.parse("1.1.1.1"));
    vrf.addNetwork(new BgpNetwork(prefix));
    vrf.setDefaultIpv4Unicast(false);

    // the method under test
    org.batfish.datamodel.BgpProcess viBgp =
        toBgpProcess(viConfig, vsConfig, DEFAULT_VRF_NAME, vrf);

    // generation policy exists
    assertFalse(viBgp.getOriginationSpace().containsPrefix(prefix));

    // the prefix is blocked
    AbstractRoute route = new ConnectedRoute(prefix, "dummy");
    assertFalse(
        viConfig
            .getRoutingPolicies()
            .get(computeBgpCommonExportPolicyName(DEFAULT_VRF_NAME))
            .process(
                route, Bgpv4Route.testBuilder().setNetwork(route.getNetwork()), Direction.OUT));
  }

  @Test
  public void testGenerateBgpCommonPeerConfig_rejectDefault() {
    Ip peerIp = Ip.parse("10.0.0.2");
    BgpIpNeighbor neighbor = new BgpIpNeighbor("BgpNeighbor", peerIp);
    neighbor.setRemoteAs(RemoteAs.internal());

    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("10.0.0.1"), ConfigurationFormat.CUMULUS_CONCATENATED);

    Configuration viConfig =
        _nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();

    BgpActivePeerConfig.Builder peerConfigBuilder =
        BgpActivePeerConfig.builder().setPeerAddress(peerIp);
    generateBgpCommonPeerConfig(
        viConfig,
        vsConfig,
        neighbor,
        10000L,
        new BgpVrf("vrf"),
        newProc,
        peerConfigBuilder,
        new Warnings());
  }

  @Test
  public void testRejectDefaultRoute() {
    Configuration viConfig =
        _nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    RoutingPolicy rejectDefaultPolicy =
        RoutingPolicy.builder()
            .setOwner(viConfig)
            .setName("policy")
            .addStatement(REJECT_DEFAULT_ROUTE)
            .addStatement(
                // accept non-default
                new If(
                    new Not(Common.matchDefaultRoute()),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement())))
            .build();

    AbstractRoute defaultRoute = new ConnectedRoute(Prefix.ZERO, "dummy");
    AbstractRoute nonDefaultRoute = new ConnectedRoute(Prefix.parse("1.1.1.1/32"), "dummy");

    assertFalse(
        rejectDefaultPolicy.process(
            defaultRoute,
            Bgpv4Route.testBuilder().setNetwork(defaultRoute.getNetwork()),
            Direction.OUT));

    assertTrue(
        rejectDefaultPolicy.process(
            nonDefaultRoute,
            Bgpv4Route.testBuilder().setNetwork(nonDefaultRoute.getNetwork()),
            Direction.OUT));
  }

  @Test
  public void testGenerateBgpCommonPeerConfig_defaultOriginate_unset() {
    // set bgp neighbor without default originate
    Ip peerIp = Ip.parse("10.0.0.2");
    BgpIpNeighbor neighbor = new BgpIpNeighbor("BgpNeighbor", peerIp);
    neighbor.setRemoteAs(RemoteAs.internal());

    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("10.0.0.1"), ConfigurationFormat.CUMULUS_CONCATENATED);

    Configuration viConfig =
        _nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();

    BgpActivePeerConfig.Builder peerConfigBuilder =
        BgpActivePeerConfig.builder().setPeerAddress(peerIp);
    generateBgpCommonPeerConfig(
        viConfig,
        vsConfig,
        neighbor,
        10000L,
        new BgpVrf("vrf"),
        newProc,
        peerConfigBuilder,
        new Warnings());

    // there should be no generated default route
    assertThat(
        newProc.getActiveNeighbors().get(peerIp.toPrefix()).getGeneratedRoutes(),
        equalTo(ImmutableSet.of()));
  }

  @Test
  public void testGenerateBgpCommonPeerConfig_defaultOriginate_set() {
    // set bgp neighbor with default originate
    Ip peerIp = Ip.parse("10.0.0.2");
    BgpIpNeighbor neighbor = new BgpIpNeighbor("BgpNeighbor", peerIp);
    neighbor.setRemoteAs(RemoteAs.internal());
    BgpNeighborIpv4UnicastAddressFamily ipv4UnicastAddressFamily =
        new BgpNeighborIpv4UnicastAddressFamily();
    ipv4UnicastAddressFamily.setDefaultOriginate(true);
    neighbor.setIpv4UnicastAddressFamily(ipv4UnicastAddressFamily);

    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("10.0.0.1"), ConfigurationFormat.CUMULUS_CONCATENATED);

    Configuration viConfig =
        _nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();

    BgpActivePeerConfig.Builder peerConfigBuilder =
        BgpActivePeerConfig.builder().setPeerAddress(peerIp);
    generateBgpCommonPeerConfig(
        viConfig,
        vsConfig,
        neighbor,
        10000L,
        new BgpVrf("vrf"),
        newProc,
        peerConfigBuilder,
        new Warnings());

    // there should be a generated default route
    assertThat(
        newProc.getActiveNeighbors().get(peerIp.toPrefix()).getGeneratedRoutes(),
        equalTo(ImmutableSet.of(GENERATED_DEFAULT_ROUTE)));

    // We test exact match with the constant REJECT_DEFAULT_ROUTE here. The constant is
    // tested in testRejectDefaultRoute()
    assertThat(
        viConfig
            .getRoutingPolicies()
            .get(computeBgpPeerExportPolicyName("vrf", neighbor.getName()))
            .getStatements()
            .get(1),
        equalTo(REJECT_DEFAULT_ROUTE));
  }

  @Test
  public void testGenerateBgpCommonPeerConfig_SetEbgpMultiHop() {
    // set bgp neighbor
    Ip peerIp = Ip.parse("10.0.0.2");
    BgpIpNeighbor neighbor = new BgpIpNeighbor("BgpNeighbor", peerIp);
    neighbor.setRemoteAs(RemoteAs.internal());
    neighbor.setEbgpMultihop(3L);

    // set bgp process
    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("10.0.0.1"), ConfigurationFormat.CUMULUS_CONCATENATED);

    Configuration viConfig =
        new NetworkFactory()
            .configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();

    BgpActivePeerConfig.Builder peerConfigBuilder =
        BgpActivePeerConfig.builder().setPeerAddress(peerIp);
    generateBgpCommonPeerConfig(
        viConfig,
        concatenatedConfiguration,
        neighbor,
        10000L,
        new BgpVrf("Vrf"),
        newProc,
        peerConfigBuilder,
        new Warnings());

    BgpActivePeerConfig peerConfig = newProc.getActiveNeighbors().get(peerIp.toPrefix());
    assertTrue(peerConfig.getEbgpMultihop());
  }

  /** Test that L2 VNIs from all VRFs are injected into BGP neighbor */
  @Test
  public void testGenerateBgpCommonPeerConfig_allL2Vni() {
    Configuration c = new Configuration("c", ConfigurationFormat.CUMULUS_CONCATENATED);

    Vrf vrf1 = Vrf.builder().setName("vrf1").build();
    vrf1.setLayer2Vnis(
        ImmutableList.of(
            Layer2Vni.builder()
                .setVni(1)
                .setVlan(11)
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                .setSrcVrf(vrf1.getName())
                .build()));

    Vrf vrf2 = Vrf.builder().setName("vrf2").build();
    vrf2.setLayer2Vnis(
        ImmutableList.of(
            Layer2Vni.builder()
                .setVni(2)
                .setVlan(22)
                .setBumTransportMethod(BumTransportMethod.UNICAST_FLOOD_GROUP)
                .setSrcVrf(vrf2.getName())
                .build()));

    c.setVrfs(ImmutableMap.of(vrf1.getName(), vrf1, vrf2.getName(), vrf2));

    InterfacesInterface vxlan1 = new InterfacesInterface("vxlan1");
    vxlan1.setVxlanId(1);
    vxlan1.createOrGetBridgeSettings().setAccess(1);

    InterfacesInterface vxlan2 = new InterfacesInterface("vxlan2");
    vxlan2.setVxlanId(2);
    vxlan2.createOrGetBridgeSettings().setAccess(2);

    CumulusFrrConfiguration frrConfiguration = new CumulusFrrConfiguration();
    frrConfiguration.setBgpProcess(new BgpProcess());

    CumulusConcatenatedConfiguration vsConfig =
        CumulusConcatenatedConfiguration.builder()
            .setHostname("c")
            .addInterfaces(ImmutableMap.of(vxlan1.getName(), vxlan1, vxlan2.getName(), vxlan2))
            .setFrrConfiguration(frrConfiguration)
            .build();

    BgpVrf bgpVrf = new BgpVrf(DEFAULT_VRF_NAME);
    bgpVrf.setL2VpnEvpn(new BgpL2vpnEvpnAddressFamily());
    bgpVrf.getL2VpnEvpn().setAdvertiseAllVni(true);

    BgpNeighbor neighbor = new BgpInterfaceNeighbor("swp1");
    BgpNeighborL2vpnEvpnAddressFamily bgpNeighborL2vpnEvpnAddressFamily =
        new BgpNeighborL2vpnEvpnAddressFamily();
    bgpNeighborL2vpnEvpnAddressFamily.setActivated(true);
    neighbor.setL2vpnEvpnAddressFamily(bgpNeighborL2vpnEvpnAddressFamily);
    neighbor.setRemoteAs(RemoteAs.external());

    long localAs = 101L;
    Ip routerId = Ip.parse("1.1.1.1");

    BgpUnnumberedPeerConfig.Builder peerConfigBuilder =
        BgpUnnumberedPeerConfig.builder().setPeerInterface("swp1");

    generateBgpCommonPeerConfig(
        c,
        vsConfig,
        neighbor,
        localAs,
        bgpVrf,
        new org.batfish.datamodel.BgpProcess(routerId, ConfigurationFormat.CUMULUS_CONCATENATED),
        peerConfigBuilder,
        new Warnings());

    assertThat(
        peerConfigBuilder.build().getEvpnAddressFamily().getL2VNIs(),
        equalTo(
            ImmutableSortedSet.of(
                Layer2VniConfig.builder()
                    .setVni(1)
                    .setVrf(vrf1.getName())
                    .setRouteDistinguisher(RouteDistinguisher.from(routerId, 0))
                    .setRouteTarget(toRouteTarget(localAs, 1))
                    .build(),
                Layer2VniConfig.builder()
                    .setVni(2)
                    .setVrf(vrf2.getName())
                    .setRouteDistinguisher(RouteDistinguisher.from(routerId, 1))
                    .setRouteTarget(toRouteTarget(localAs, 2))
                    .build())));
  }

  /** Test that L2 VPN EVPN address family route reflect is set correctly */
  @Test
  public void testGenerateBgpCommonPeerConfig_L2vpnRouteReflector() {
    CumulusConcatenatedConfiguration vsConfig =
        CumulusConcatenatedConfiguration.builder()
            .setHostname("c")
            .setBgpProcess(new BgpProcess())
            .build();

    BgpVrf bgpVrf = new BgpVrf(DEFAULT_VRF_NAME);
    bgpVrf.setL2VpnEvpn(new BgpL2vpnEvpnAddressFamily());

    BgpNeighbor neighbor = new BgpInterfaceNeighbor("swp1");
    BgpNeighborL2vpnEvpnAddressFamily bgpNeighborL2vpnEvpnAddressFamily =
        new BgpNeighborL2vpnEvpnAddressFamily();
    bgpNeighborL2vpnEvpnAddressFamily.setActivated(true);
    neighbor.setL2vpnEvpnAddressFamily(bgpNeighborL2vpnEvpnAddressFamily);
    neighbor.setRemoteAs(RemoteAs.external());

    BgpUnnumberedPeerConfig.Builder peerConfigBuilder =
        BgpUnnumberedPeerConfig.builder().setPeerInterface("swp1");

    // we didn't set route reflector yet
    generateBgpCommonPeerConfig(
        new Configuration("c", ConfigurationFormat.CUMULUS_CONCATENATED),
        vsConfig,
        neighbor,
        101L,
        bgpVrf,
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("1.1.1.1"), ConfigurationFormat.CUMULUS_CONCATENATED),
        peerConfigBuilder,
        new Warnings());
    assertFalse(peerConfigBuilder.build().getEvpnAddressFamily().getRouteReflectorClient());

    // now set and test again
    bgpNeighborL2vpnEvpnAddressFamily.setRouteReflectorClient(true);
    generateBgpCommonPeerConfig(
        new Configuration("c", ConfigurationFormat.CUMULUS_CONCATENATED),
        vsConfig,
        neighbor,
        101L,
        bgpVrf,
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("1.1.1.1"), ConfigurationFormat.CUMULUS_CONCATENATED),
        peerConfigBuilder,
        new Warnings());
    assertTrue(peerConfigBuilder.build().getEvpnAddressFamily().getRouteReflectorClient());
  }

  @Test
  public void testConvertIpv4UnicastAddressFamily_deactivated() {
    // setup vi model
    NetworkFactory nf = new NetworkFactory();
    RoutingPolicy policy = nf.routingPolicyBuilder().build();

    // setup vs model
    BgpNeighborIpv4UnicastAddressFamily af = new BgpNeighborIpv4UnicastAddressFamily();
    af.setRouteReflectorClient(true);

    // explicitly deactivated
    af.setActivated(false);
    assertNull(convertIpv4UnicastAddressFamily(af, true, policy, null));
    assertNull(convertIpv4UnicastAddressFamily(af, false, policy, null));

    // explicitly activated
    af.setActivated(true);
    assertNotNull(convertIpv4UnicastAddressFamily(af, true, policy, null));
    assertNotNull(convertIpv4UnicastAddressFamily(af, false, policy, null));

    // no explicit configuration
    af.setActivated(null);
    assertNotNull(convertIpv4UnicastAddressFamily(af, true, policy, null));
    assertNull(convertIpv4UnicastAddressFamily(af, false, policy, null));
  }

  @Test
  public void testConvertIpv4UnicastAddressFamily_allowAsIn() {
    // setup vi model
    NetworkFactory nf = new NetworkFactory();
    RoutingPolicy policy = nf.routingPolicyBuilder().build();

    {
      // address family is null
      AddressFamilyCapabilities capabilities =
          convertIpv4UnicastAddressFamily(null, true, policy, null).getAddressFamilyCapabilities();
      assertFalse(capabilities.getAllowLocalAsIn());
      // Counter-part to allow-as-in. Always allowed.
      assertThat(capabilities.getAllowRemoteAsOut(), equalTo(ALWAYS));
    }

    {
      // address family is non-null but allowasin is not set
      BgpNeighborIpv4UnicastAddressFamily af = new BgpNeighborIpv4UnicastAddressFamily();
      AddressFamilyCapabilities capabilities =
          convertIpv4UnicastAddressFamily(af, true, policy, null).getAddressFamilyCapabilities();
      assertFalse(capabilities.getAllowLocalAsIn());
      // Counter-part to allow-as-in. Always allowed.
      assertThat(capabilities.getAllowRemoteAsOut(), equalTo(ALWAYS));
    }

    {
      // address family is non-null and allowasin is  set
      BgpNeighborIpv4UnicastAddressFamily af = new BgpNeighborIpv4UnicastAddressFamily();
      af.setAllowAsIn(5);
      AddressFamilyCapabilities capabilities =
          convertIpv4UnicastAddressFamily(af, true, policy, null).getAddressFamilyCapabilities();
      assertTrue(capabilities.getAllowLocalAsIn());
      // Counter-part to allow-as-in. Always allowed.
      assertThat(capabilities.getAllowRemoteAsOut(), equalTo(ALWAYS));
    }
  }

  @Test
  public void testConvertIpv4UnicastAddressFamily_routeReflectorClient() {

    // setup vi model
    NetworkFactory nf = new NetworkFactory();
    RoutingPolicy policy = nf.routingPolicyBuilder().build();

    // route-reflector-client is false if ipv4af is null
    assertFalse(
        convertIpv4UnicastAddressFamily(null, true, policy, null).getRouteReflectorClient());

    // VI route-reflector-client is true if VS activate and route-reflector-client are both true
    {
      BgpNeighborIpv4UnicastAddressFamily af = new BgpNeighborIpv4UnicastAddressFamily();
      af.setActivated(true);
      af.setRouteReflectorClient(true);
      assertTrue(convertIpv4UnicastAddressFamily(af, true, policy, null).getRouteReflectorClient());
    }

    // Despite cumulus docs, GNS3 testing confirms VI route-reflector-client should be true even if
    // activate is null (i.e. not explicitly activated).
    {
      BgpNeighborIpv4UnicastAddressFamily af = new BgpNeighborIpv4UnicastAddressFamily();
      af.setRouteReflectorClient(true);
      assertTrue(convertIpv4UnicastAddressFamily(af, true, policy, null).getRouteReflectorClient());
    }
  }

  @Test
  public void testComputeBgpNeighborImportRoutingPolicy() {
    BgpNeighbor neighbor = new BgpIpNeighbor("neighbor", Ip.parse("1.2.3.4"));

    BgpNeighborIpv4UnicastAddressFamily neighborIpv4UnicastFamily =
        new BgpNeighborIpv4UnicastAddressFamily();

    neighborIpv4UnicastFamily.setRouteMapIn("peerMapIn");

    neighbor.setIpv4UnicastAddressFamily(neighborIpv4UnicastFamily);

    BgpVrf bgpVrf = new BgpVrf("bgpVrf");

    Configuration config = new Configuration("host", ConfigurationFormat.CUMULUS_CONCATENATED);

    RoutingPolicy importPolicy = computeBgpNeighborImportRoutingPolicy(config, neighbor, bgpVrf);

    // checking the import policy is correct (i.e. it successfully calls the route map to
    // permite/block route).
    RoutingPolicy routemapPolicy =
        RoutingPolicy.builder()
            .setOwner(config)
            .setName("peerMapIn")
            .addStatement(
                new If(
                    "match community 10000:1 routes",
                    new MatchCommunities(
                        InputCommunities.instance(),
                        new HasCommunity(new CommunityIs(StandardCommunity.parse("10000:1")))),
                    ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                    ImmutableList.of(Statements.ExitReject.toStaticStatement())))
            .build();

    Builder builder =
        Bgpv4Route.testBuilder()
            .setOriginatorIp(Ip.parse("10.0.0.1"))
            .setOriginType(OriginType.EGP)
            .setProtocol(RoutingProtocol.BGP)
            .setNetwork(Prefix.parse("10.0.0.0/24"));
    {
      // should permit route
      Bgpv4Route ipv4Route =
          builder.setCommunities(ImmutableSet.of(StandardCommunity.parse("10000:1"))).build();

      Environment envIpv4 =
          Environment.builder(config)
              .setRoutingPolicies(ImmutableMap.of("peerMapIn", routemapPolicy))
              .setOriginalRoute(ipv4Route)
              .setDirection(Direction.IN)
              .build();
      Result result = importPolicy.call(envIpv4);
      assertTrue(result.getBooleanValue());
    }

    {
      // should block route
      Bgpv4Route ipv4Route =
          builder.setCommunities(ImmutableSet.of(StandardCommunity.parse("20000:1"))).build();

      Environment envIpv4 =
          Environment.builder(config)
              .setRoutingPolicies(ImmutableMap.of("peerMapIn", routemapPolicy))
              .setOriginalRoute(ipv4Route)
              .setDirection(Direction.IN)
              .build();
      Result result = importPolicy.call(envIpv4);
      assertFalse(result.getBooleanValue());
    }
  }

  private static Configuration getConfigurationWithLoopback(
      @Nullable ConcreteInterfaceAddress address) {
    Configuration c = new Configuration("test", ConfigurationFormat.CUMULUS_CONCATENATED);
    org.batfish.datamodel.Interface loopback =
        org.batfish.datamodel.Interface.builder()
            .setName(LOOPBACK_INTERFACE_NAME)
            .setOwner(c)
            .build();
    if (address != null) {
      loopback.setAddress(address);
      loopback.setAllAddresses(ImmutableSet.of(address));
    }
    return c;
  }

  @Test
  public void testToOspfProcess_NoRouterId() {
    OspfVrf ospfVrf = new OspfVrf(DEFAULT_VRF_NAME);
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    OspfProcess vsOspf = new OspfProcess();
    vsConfig.getFrrConfiguration().setOspfProcess(vsOspf);

    org.batfish.datamodel.ospf.OspfProcess ospfProcess =
        toOspfProcess(
            new Configuration("dummy", ConfigurationFormat.CUMULUS_CONCATENATED),
            vsConfig,
            ospfVrf,
            ImmutableMap.of(),
            new Warnings());
    assertThat(ospfProcess.getRouterId(), equalTo(Ip.parse("0.0.0.0")));
    assertThat(ospfProcess.getProcessId(), equalTo("default"));
    assertThat(
        ospfProcess.getReferenceBandwidth(),
        equalTo(org.batfish.representation.cumulus.OspfProcess.DEFAULT_REFERENCE_BANDWIDTH));
  }

  @Test
  public void testToOspfProcess_InferRouterId() {
    OspfVrf ospfVrf = new OspfVrf(DEFAULT_VRF_NAME);
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    OspfProcess vsOspf = new OspfProcess();
    vsConfig.getFrrConfiguration().setOspfProcess(vsOspf);

    org.batfish.datamodel.ospf.OspfProcess ospfProcess =
        toOspfProcess(
            getConfigurationWithLoopback(ConcreteInterfaceAddress.parse("1.1.1.1/24")),
            vsConfig,
            ospfVrf,
            ImmutableMap.of(),
            new Warnings());
    assertThat(ospfProcess.getRouterId(), equalTo(Ip.parse("1.1.1.1")));
    assertThat(ospfProcess.getProcessId(), equalTo("default"));
    assertThat(
        ospfProcess.getReferenceBandwidth(),
        equalTo(org.batfish.representation.cumulus.OspfProcess.DEFAULT_REFERENCE_BANDWIDTH));
  }

  @Test
  public void testToOspfProcess_ConfigedRouterId() {
    OspfVrf ospfVrf = new OspfVrf(DEFAULT_VRF_NAME);
    ospfVrf.setRouterId(Ip.parse("1.2.3.4"));
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    OspfProcess vsOspf = new OspfProcess();
    vsConfig.getFrrConfiguration().setOspfProcess(vsOspf);

    org.batfish.datamodel.ospf.OspfProcess ospfProcess =
        toOspfProcess(
            new Configuration("dummy", ConfigurationFormat.CUMULUS_CONCATENATED),
            vsConfig,
            ospfVrf,
            ImmutableMap.of(),
            new Warnings());
    assertThat(ospfProcess.getRouterId(), equalTo(Ip.parse("1.2.3.4")));
    assertThat(ospfProcess.getProcessId(), equalTo("default"));
    assertThat(
        ospfProcess.getReferenceBandwidth(),
        equalTo(org.batfish.representation.cumulus.OspfProcess.DEFAULT_REFERENCE_BANDWIDTH));
  }

  @Test
  public void testToOspfProcess_RedistributionPolicy() {
    // setup VI model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    nf.vrfBuilder().setOwner(viConfig).setName(DEFAULT_VRF_NAME).build();

    // Setup VS
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    // Setup OSPF
    OspfVrf ospfVrf = new OspfVrf(DEFAULT_VRF_NAME);
    ospfVrf.setRouterId(Ip.parse("1.2.3.4"));
    OspfProcess vsOspf = new OspfProcess();
    vsConfig.getFrrConfiguration().setOspfProcess(vsOspf);

    vsConfig.getRouteMaps().put("some-map", new RouteMap("some-map"));
    String policyName = computeOspfExportPolicyName(ospfVrf.getVrfName());

    toOspfProcess(viConfig, vsConfig, ospfVrf, ImmutableMap.of(), new Warnings());

    assertEquals(
        viConfig.getRoutingPolicies().get(policyName).getStatements(),
        ImmutableList.of(
            new SetOspfMetricType(OspfMetricType.E2), new SetMetric(new LiteralLong(20L))));
  }

  @Test
  public void testToOspfProcess_MaxMetricRouterLsa() {
    OspfVrf ospfVrf = new OspfVrf(DEFAULT_VRF_NAME);
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    OspfProcess vsOspf = new OspfProcess();
    vsOspf.setMaxMetricRouterLsa(true);
    vsConfig.getFrrConfiguration().setOspfProcess(vsOspf);

    org.batfish.datamodel.ospf.OspfProcess ospfProcess =
        toOspfProcess(
            new Configuration("dummy", ConfigurationFormat.CUMULUS_CONCATENATED),
            vsConfig,
            ospfVrf,
            ImmutableMap.of(),
            new Warnings());
    assertThat(
        ospfProcess.getMaxMetricTransitLinks(),
        equalTo(CumulusConversions.DEFAULT_OSPF_MAX_METRIC));
  }

  @Test
  public void testConvertOspfRedistributionPolicy() {
    // setup VI model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    nf.vrfBuilder().setOwner(viConfig).setName(DEFAULT_VRF_NAME).build();

    // Setup VS
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();

    // Setup OSPF
    OspfVrf ospfVrf = new OspfVrf(DEFAULT_VRF_NAME);
    ospfVrf.setRouterId(Ip.parse("1.2.3.4"));
    OspfProcess vsOspf = new OspfProcess();
    vsConfig.getFrrConfiguration().setOspfProcess(vsOspf);

    RedistributionPolicy rp = new RedistributionPolicy(CumulusRoutingProtocol.BGP, "some-map");
    vsConfig.getRouteMaps().put("some-map", new RouteMap("some-map"));

    // Method under test
    If policy = convertOspfRedistributionPolicy(rp, vsConfig.getRouteMaps());
    List<BooleanExpr> guard = ((Conjunction) policy.getGuard()).getConjuncts();

    assertThat(guard, contains(new MatchProtocol(BGP, IBGP), new CallExpr("some-map")));
    assertThat(policy.getTrueStatements(), contains(ExitAccept.toStaticStatement()));
  }

  @Test
  public void testInferRouterID_DefaultCase() {
    assertThat(
        inferRouterId(new Configuration("dummy", ConfigurationFormat.CUMULUS_CONCATENATED)),
        equalTo(Ip.parse("0.0.0.0")));
  }

  @Test
  public void testInferRouterID_Loopback() {
    assertThat(
        inferRouterId(getConfigurationWithLoopback(ConcreteInterfaceAddress.parse("1.1.1.1/31"))),
        equalTo(Ip.parse("1.1.1.1")));
  }

  @Test
  public void testInferRouterID_LoopbackNotUsable() {
    assertThat(
        inferRouterId(getConfigurationWithLoopback(ConcreteInterfaceAddress.parse("127.0.0.2/31"))),
        equalTo(Ip.parse("0.0.0.0")));
  }

  @Test
  public void testInferRouterID_MaxInterfaceIp() {
    Configuration c = new Configuration("test", ConfigurationFormat.CUMULUS_CONCATENATED);
    org.batfish.datamodel.Interface.builder()
        .setName("eth1")
        .setOwner(c)
        .setAddress(ConcreteInterfaceAddress.parse("1.1.1.1/30"))
        .build();
    org.batfish.datamodel.Interface.builder()
        .setName("eth2")
        .setOwner(c)
        .setAddress(ConcreteInterfaceAddress.parse("2.2.2.2/30"))
        .build();

    assertThat(inferRouterId(c), equalTo(Ip.parse("2.2.2.2")));
  }

  @Test
  public void testInferClusterId_with_ClusterId() {
    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("1.1.1.1"), ConfigurationFormat.CUMULUS_CONCATENATED);

    BgpVrf bgpVrf = new BgpVrf("bgpVrf");
    bgpVrf.setClusterId(Ip.parse("2.2.2.2"));
    bgpVrf.setAutonomousSystem(123L);

    BgpNeighbor neighbor = new BgpInterfaceNeighbor("iface");
    neighbor.setRemoteAs(RemoteAs.explicit(123));

    assertThat(
        inferClusterId(bgpVrf, newProc.getRouterId(), neighbor, 123L),
        equalTo(Ip.parse("2.2.2.2").asLong()));
  }

  @Test
  public void testInferClusterId_without_ClusterId() {
    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("1.1.1.1"), ConfigurationFormat.CUMULUS_CONCATENATED);

    BgpVrf bgpVrf = new BgpVrf("bgpVrf");
    bgpVrf.setAutonomousSystem(123L);
    bgpVrf.setClusterId(null);

    BgpNeighbor neighbor = new BgpInterfaceNeighbor("iface");
    neighbor.setRemoteAs(RemoteAs.explicit(123));

    assertThat(
        inferClusterId(bgpVrf, newProc.getRouterId(), neighbor, 123L),
        equalTo(Ip.parse("1.1.1.1").asLong()));
  }

  @Test
  public void testInferClusterId_eBGP() {
    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("1.1.1.1"), ConfigurationFormat.CUMULUS_CONCATENATED);

    BgpVrf bgpVrf = new BgpVrf("bgpVrf");
    bgpVrf.setClusterId(Ip.parse("2.2.2.2"));
    bgpVrf.setAutonomousSystem(2000L);

    BgpNeighbor neighbor = new BgpInterfaceNeighbor("iface");
    neighbor.setRemoteAs(RemoteAs.explicit(123));

    assertThat(inferClusterId(bgpVrf, newProc.getRouterId(), neighbor, 2000L), equalTo(null));
  }

  @Test
  public void testAddOspfInterfaces_HasArea() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().setOspfProcess(new OspfProcess());
    concatenatedConfiguration
        .getFrrConfiguration()
        .getOrCreateInterface("iface")
        .getOrCreateOspf()
        .setOspfArea(1L);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);
    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());
    assertThat(viIface.getOspfAreaName(), equalTo(1L));
  }

  @Test
  public void testAddOspfInterfaces_NoArea() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().getOrCreateInterface("iface").getOrCreateOspf();

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    assertNull(viIface.getOspfAreaName());
  }

  @Test
  public void testAddOspfInterfaces_HasCost() {
    CumulusConcatenatedConfiguration config = new CumulusConcatenatedConfiguration();
    config.getFrrConfiguration().setOspfProcess(new OspfProcess());
    OspfInterface ospf =
        config.getFrrConfiguration().getOrCreateInterface("iface").getOrCreateOspf();
    ospf.setOspfArea(0L);
    ospf.setCost(100);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(config, ifaceMap, "1", new Warnings());
    assertThat(viIface.getOspfCost(), equalTo(100));
  }

  /** Test that loopback cost is ignored, even if configured, and forced to 0. */
  @Test
  public void testAddOspfInterfaces_LoopbackCost() {
    CumulusConcatenatedConfiguration config = new CumulusConcatenatedConfiguration();
    config.getFrrConfiguration().setOspfProcess(new OspfProcess());
    OspfInterface ospf = config.getFrrConfiguration().getOrCreateInterface("lo").getOrCreateOspf();
    ospf.setOspfArea(0L);
    ospf.setCost(100);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder()
            .setName("lo")
            .setVrf(vrf)
            .setType(LOOPBACK)
            .build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(config, ifaceMap, "1", new Warnings());
    assertThat(viIface.getOspfCost(), equalTo(0));
  }

  @Test
  public void testAddOspfInterfaces_NoCost() {
    CumulusConcatenatedConfiguration config = new CumulusConcatenatedConfiguration();
    config.getFrrConfiguration().setOspfProcess(new OspfProcess());
    config.getFrrConfiguration().getOrCreateInterface("iface").getOrCreateOspf().setOspfArea(0L);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(config, ifaceMap, "1", new Warnings());
    assertNull(viIface.getOspfCost());
  }

  @Test
  public void testAddOspfInterfaces_NoNetworkType() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().setOspfProcess(new OspfProcess());
    concatenatedConfiguration
        .getFrrConfiguration()
        .getOrCreateInterface("iface")
        .getOrCreateOspf()
        .setOspfArea(0L);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());
    assertNull(viIface.getOspfNetworkType());
  }

  /**
   * Test that networks statements at BGP VRF level (outside of ipv4 address family stanza) are
   * accounted for when the address family is active
   */
  @Test
  public void testToBgpProcess_OspfToBgpRedistribution() {
    // setup VI model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    nf.vrfBuilder().setOwner(viConfig).setName(DEFAULT_VRF_NAME).build();

    // setup VS model
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    BgpProcess bgpProcess = new BgpProcess();
    OspfProcess ospfProcess = new OspfProcess();
    vsConfig.getFrrConfiguration().setOspfProcess(ospfProcess);
    vsConfig.getFrrConfiguration().setBgpProcess(bgpProcess);
    vsConfig.getRouteMaps().put("redist_policy", new RouteMap("redist_policy"));

    // setup routing policy - block default and allow all else.
    RoutingPolicy.builder()
        .setOwner(viConfig)
        .setName("redist_policy")
        .addStatement(REJECT_DEFAULT_ROUTE)
        .addStatement(
            // accept non-default
            new If(
                new Not(Common.matchDefaultRoute()),
                ImmutableList.of(Statements.ReturnTrue.toStaticStatement())))
        .build();

    // setup BgpVrf
    BgpVrf vrf = bgpProcess.getDefaultVrf();
    vrf.setRouterId(Ip.parse("1.1.1.1"));
    vrf.getRedistributionPolicies()
        .put(
            CumulusRoutingProtocol.OSPF,
            new BgpRedistributionPolicy(CumulusRoutingProtocol.OSPF, "redist_policy"));

    // setup OspfVrf
    OspfVrf ospf = ospfProcess.getDefaultVrf();
    ospf.setRouterId(Ip.parse("1.1.1.1"));

    // the method under test
    toBgpProcess(viConfig, vsConfig, DEFAULT_VRF_NAME, vrf);

    // Spawn test prefixes
    Prefix prefix = Prefix.parse("1.1.1.1/32");

    OspfExternalRoute.Builder ospfExternalRouteBuilder =
        OspfExternalRoute.builder()
            .setNetwork(Prefix.ZERO)
            .setNextHop(NextHopDiscard.instance())
            .setOspfMetricType(OspfMetricType.E1)
            .setLsaMetric(1L)
            .setArea(0L)
            .setCostToAdvertiser(1L)
            .setAdvertiser("advertiser");

    OspfExternalRoute.Builder ospfExternalRouteBuilder2 =
        OspfExternalRoute.builder()
            .setNetwork(prefix)
            .setNextHop(NextHopDiscard.instance())
            .setOspfMetricType(OspfMetricType.E2)
            .setLsaMetric(1L)
            .setArea(0L)
            .setCostToAdvertiser(1L)
            .setAdvertiser("advertiser");

    // Based on OSPF type and Routing Policy, 0.0.0.0/0 should be denied and 1.1.1.1/32 should be
    // allowed
    assertFalse(
        viConfig
            .getRoutingPolicies()
            .get(computeBgpCommonExportPolicyName(DEFAULT_VRF_NAME))
            .process(
                ospfExternalRouteBuilder.build(),
                Bgpv4Route.testBuilder().setNetwork(Prefix.ZERO),
                Direction.OUT));

    assertTrue(
        viConfig
            .getRoutingPolicies()
            .get(computeBgpCommonExportPolicyName(DEFAULT_VRF_NAME))
            .process(
                ospfExternalRouteBuilder2.build(),
                Bgpv4Route.testBuilder().setNetwork(prefix),
                Direction.OUT));
  }

  @Test
  public void testAddOspfInterfaces_NoPassiveInterface() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().setOspfProcess(new OspfProcess());
    concatenatedConfiguration
        .getFrrConfiguration()
        .getOrCreateInterface("iface")
        .getOrCreateOspf()
        .setOspfArea(0L);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());
    assertFalse(viIface.getOspfPassive());
  }

  @Test
  public void testAddOspfInterfaces_NoPassiveInterface_DefaultPassive() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().setOspfProcess(new OspfProcess());
    concatenatedConfiguration.getOspfProcess().setDefaultPassiveInterface(true);

    concatenatedConfiguration
        .getFrrConfiguration()
        .getOrCreateInterface("iface")
        .getOrCreateOspf()
        .setOspfArea(0L);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());
    assertTrue(viIface.getOspfPassive());
  }

  @Test
  public void testAddOspfInterfaces_PassiveInterface() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().setOspfProcess(new OspfProcess());
    OspfInterface ospf =
        concatenatedConfiguration
            .getFrrConfiguration()
            .getOrCreateInterface("iface")
            .getOrCreateOspf();
    ospf.setOspfArea(0L);
    ospf.setPassive(true);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());
    assertTrue(viIface.getOspfPassive());
  }

  @Test
  public void testAddOspfInterfaces_NetworkTypeP2P() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().setOspfProcess(new OspfProcess());
    OspfInterface ospf =
        concatenatedConfiguration
            .getFrrConfiguration()
            .getOrCreateInterface("iface")
            .getOrCreateOspf();
    ospf.setOspfArea(0L);
    ospf.setNetwork(OspfNetworkType.POINT_TO_POINT);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());
    assertThat(
        viIface.getOspfNetworkType(),
        equalTo(org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT));
  }

  @Test
  public void testAddOspfInterfaces_HelloInterval() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().setOspfProcess(new OspfProcess());
    OspfInterface ospf =
        concatenatedConfiguration
            .getFrrConfiguration()
            .getOrCreateInterface("iface")
            .getOrCreateOspf();
    ospf.setOspfArea(0L);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());

    // default hello interval
    assertThat(
        viIface.getOspfSettings().getHelloInterval(),
        equalTo(OspfInterface.DEFAULT_OSPF_HELLO_INTERVAL));

    // set hello interval
    ospf.setHelloInterval(1);
    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());
    assertThat(viIface.getOspfSettings().getHelloInterval(), equalTo(1));
  }

  @Test
  public void testAddOspfInterfaces_DeadInterval() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().setOspfProcess(new OspfProcess());
    OspfInterface ospf =
        concatenatedConfiguration
            .getFrrConfiguration()
            .getOrCreateInterface("iface")
            .getOrCreateOspf();
    ospf.setOspfArea(0L);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();
    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);

    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());

    // default dead interval
    assertThat(
        viIface.getOspfSettings().getDeadInterval(),
        equalTo(OspfInterface.DEFAULT_OSPF_DEAD_INTERVAL));

    // set dead interval
    ospf.setDeadInterval(1);
    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());
    assertThat(viIface.getOspfSettings().getDeadInterval(), equalTo(1));
  }

  @Test
  public void testAddOspfInterfaces_ProcessId() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().setOspfProcess(new OspfProcess());
    OspfInterface ospf =
        concatenatedConfiguration
            .getFrrConfiguration()
            .getOrCreateInterface("iface")
            .getOrCreateOspf();
    ospf.setOspfArea(0L);

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);
    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());

    // default dead interval
    assertThat(viIface.getOspfSettings().getProcess(), equalTo("1"));
  }

  @Test
  public void testAddOspfInterfaces_NoInterface() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();

    Vrf vrf = new Vrf(DEFAULT_VRF_NAME);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder().setName("iface").setVrf(vrf).build();

    Map<String, org.batfish.datamodel.Interface> ifaceMap =
        ImmutableMap.of(viIface.getName(), viIface);
    addOspfInterfaces(concatenatedConfiguration, ifaceMap, "1", new Warnings());

    assertNull(viIface.getOspfSettings());
  }

  @Test
  public void testComputeOspfProcess_HasArea() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration
        .getFrrConfiguration()
        .getOrCreateInterface("iface")
        .getOrCreateOspf()
        .setOspfArea(1L);
    OspfVrf vsVrf =
        concatenatedConfiguration.getFrrConfiguration().getOrCreateOspfProcess().getDefaultVrf();
    Configuration c = new Configuration("c", ConfigurationFormat.CUMULUS_CONCATENATED);

    SortedMap<Long, OspfArea> areas =
        computeOspfAreas(c, concatenatedConfiguration, vsVrf, ImmutableList.of("iface"));
    assertThat(
        areas,
        equalTo(
            ImmutableSortedMap.of(
                1L, OspfArea.builder().addInterface("iface").setNumber(1L).build())));
  }

  @Test
  public void testComputeOspfProcess_NoArea() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    concatenatedConfiguration.getFrrConfiguration().getOrCreateInterface("iface").getOrCreateOspf();
    OspfVrf vsVrf =
        concatenatedConfiguration.getFrrConfiguration().getOrCreateOspfProcess().getDefaultVrf();
    Configuration c = new Configuration("c", ConfigurationFormat.CUMULUS_CONCATENATED);

    SortedMap<Long, OspfArea> areas =
        computeOspfAreas(c, concatenatedConfiguration, vsVrf, ImmutableList.of("iface"));
    assertThat(areas, equalTo(ImmutableSortedMap.of()));
  }

  @Test
  public void testComputeOspfProcess_NoInterface() {
    CumulusConcatenatedConfiguration concatenatedConfiguration =
        new CumulusConcatenatedConfiguration();
    OspfVrf vsVrf =
        concatenatedConfiguration.getFrrConfiguration().getOrCreateOspfProcess().getDefaultVrf();
    Configuration c = new Configuration("c", ConfigurationFormat.CUMULUS_CONCATENATED);

    SortedMap<Long, OspfArea> areas =
        computeOspfAreas(c, concatenatedConfiguration, vsVrf, ImmutableList.of("iface"));
    assertThat(areas, equalTo(ImmutableSortedMap.of()));
  }

  @Test
  public void testResolveLocalIpFromUpdateSource_Null() {
    Warnings warnings = new Warnings(false, true, false);

    // create vi config
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    assertNull(resolveLocalIpFromUpdateSource(null, c, warnings));
  }

  @Test
  public void testResolveLocalIpFromUpdateSource_Ip() {
    Warnings warnings = new Warnings(false, true, false);
    BgpNeighborSourceAddress source = new BgpNeighborSourceAddress(Ip.parse("1.1.1.1"));

    // create vi config
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    assertEquals(resolveLocalIpFromUpdateSource(source, c, warnings), Ip.parse("1.1.1.1"));
  }

  @Test
  public void testResolveLocalIpFromUpdateSource_Interface_NoInterface() {
    Warnings warnings = new Warnings(false, true, false);
    BgpNeighborSourceInterface source = new BgpNeighborSourceInterface("lo");

    // create vi config
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    assertNull(resolveLocalIpFromUpdateSource(source, c, warnings));

    assertThat(
        warnings,
        hasRedFlags(contains(hasText("cannot find interface named lo for update-source"))));
  }

  @Test
  public void testResolveLocalIpFromUpdateSource_Interface_NoInterfaceAddress() {
    Warnings warnings = new Warnings(false, true, false);
    BgpNeighborSourceInterface source = new BgpNeighborSourceInterface("lo");

    // create vi config
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    nf.interfaceBuilder().setType(PHYSICAL).setOwner(c).setName("lo").build();

    assertNull(resolveLocalIpFromUpdateSource(source, c, warnings));
    assertThat(
        warnings,
        hasRedFlags(
            contains(hasText("cannot find an address for interface named lo for update-source"))));
  }

  @Test
  public void testResolveLocalIpFromUpdateSource_Interface() {
    Warnings warnings = new Warnings(false, true, false);
    BgpNeighborSourceInterface source = new BgpNeighborSourceInterface("lo");

    // create vi config
    NetworkFactory nf = new NetworkFactory();
    Configuration c =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();

    nf.interfaceBuilder()
        .setType(PHYSICAL)
        .setOwner(c)
        .setName("lo")
        .setAddress(ConcreteInterfaceAddress.parse("1.1.1.1/24"))
        .build();

    assertEquals(resolveLocalIpFromUpdateSource(source, c, warnings), Ip.parse("1.1.1.1"));
  }

  /**
   * An interface neighbor with (only) a /31 or /30 address should be treated as a numbered neighbor
   */
  @Test
  public void testAddBgpNeighbor_numberedInterface() {
    // set up the VI bgp process
    org.batfish.datamodel.BgpProcess bgpProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("0.0.0.0"), ConfigurationFormat.CUMULUS_CONCATENATED);
    Vrf viVrf = Vrf.builder().setName("vrf").build();
    viVrf.setBgpProcess(bgpProc);

    // set up the vi interface
    ConcreteInterfaceAddress ifaceAddress = ConcreteInterfaceAddress.parse("1.1.1.1/31");
    Configuration c = new Configuration("c", ConfigurationFormat.CUMULUS_CONCATENATED);
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder()
            .setName("iface")
            .setOwner(c)
            .setAddress(ifaceAddress)
            .build();
    c.getAllInterfaces().put(viIface.getName(), viIface);
    c.getVrfs().put(viVrf.getName(), viVrf);

    BgpNeighbor neighbor = new BgpInterfaceNeighbor("iface");
    neighbor.setRemoteAs(RemoteAs.explicit(123));

    addBgpNeighbor(
        c,
        CumulusConcatenatedConfiguration.builder().setHostname("c").build(),
        new BgpVrf(viVrf.getName()),
        neighbor,
        new Warnings());

    Prefix peerPrefix = Prefix.parse("1.1.1.0/32");
    assertTrue(bgpProc.getActiveNeighbors().containsKey(peerPrefix));
    assertEquals(bgpProc.getActiveNeighbors().get(peerPrefix).getLocalIp(), ifaceAddress.getIp());
  }

  @Test
  public void testInferPeerIp_slash31() {
    assertEquals(
        inferPeerIp(
            org.batfish.datamodel.Interface.builder()
                .setAddress(ConcreteInterfaceAddress.parse("1.1.1.0/31")) // first address
                .setName("iface")
                .build()),
        Optional.of(Ip.parse("1.1.1.1")));
    assertEquals(
        inferPeerIp(
            org.batfish.datamodel.Interface.builder()
                .setAddress(ConcreteInterfaceAddress.parse("1.1.1.1/31")) // second address
                .setName("iface")
                .build()),
        Optional.of(Ip.parse("1.1.1.0")));
  }

  @Test
  public void testInferPeerIp_slash30() {
    assertEquals(
        inferPeerIp(
            org.batfish.datamodel.Interface.builder()
                .setAddress(ConcreteInterfaceAddress.parse("1.1.1.0/30")) // first address (network)
                .setName("iface")
                .build()),
        Optional.empty());
    assertEquals(
        inferPeerIp(
            org.batfish.datamodel.Interface.builder()
                .setAddress(ConcreteInterfaceAddress.parse("1.1.1.1/30")) // second address
                .setName("iface")
                .build()),
        Optional.of(Ip.parse("1.1.1.2")));
    assertEquals(
        inferPeerIp(
            org.batfish.datamodel.Interface.builder()
                .setAddress(ConcreteInterfaceAddress.parse("1.1.1.2/30")) // third address
                .setName("iface")
                .build()),
        Optional.of(Ip.parse("1.1.1.1")));
    assertEquals(
        inferPeerIp(
            org.batfish.datamodel.Interface.builder()
                .setAddress(ConcreteInterfaceAddress.parse("1.1.1.3/30")) // fourth address (bcast)
                .setName("iface")
                .build()),
        Optional.empty());
  }

  @Test
  public void testInferPeerIp_otherLength() {
    assertEquals(
        inferPeerIp(
            org.batfish.datamodel.Interface.builder()
                .setAddress(ConcreteInterfaceAddress.parse("1.1.1.0/32"))
                .setName("iface")
                .build()),
        Optional.empty());
    assertEquals(
        inferPeerIp(
            org.batfish.datamodel.Interface.builder()
                .setAddress(ConcreteInterfaceAddress.parse("1.1.1.0/29"))
                .setName("iface")
                .build()),
        Optional.empty());
  }

  @Test
  public void testInferPeerIp_multipleAddresses() {
    org.batfish.datamodel.Interface viIface =
        org.batfish.datamodel.Interface.builder()
            .setAddresses(
                ConcreteInterfaceAddress.parse("1.1.1.1/31"),
                ConcreteInterfaceAddress.parse("2.2.2.2/31"))
            .setName("iface")
            .build();
    assertEquals(inferPeerIp(viIface), Optional.empty());
  }

  @Test
  public void testConvertVxlan_localIpPrecedence() {
    Configuration c = new Configuration("c", ConfigurationFormat.CUMULUS_CONCATENATED);
    Vrf vrf = new Vrf("vrf");
    c.setVrfs(ImmutableMap.of(vrf.getName(), vrf));

    Ip vxlanLocalTunnelIp = Ip.parse("1.1.1.1");
    Ip loopbackTunnelIp = Ip.parse("2.2.2.2");
    Ip loopbackAnycastIp = Ip.parse("3.3.3.3");

    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    InterfacesInterface iface =
        vsConfig.getInterfacesConfiguration().createOrGetInterface("vxlan1001");
    iface.setVxlanId(1001);
    iface.setVxlanLocalTunnelIp(vxlanLocalTunnelIp);
    iface.createOrGetBridgeSettings().setAccess(101);

    // vxlan's local tunnel ip should win when anycast is null
    convertVxlans(
        c, vsConfig, ImmutableMap.of(1001, vrf.getName()), null, loopbackTunnelIp, new Warnings());
    assertThat(vrf.getLayer3Vnis().get(1001).getSourceAddress(), equalTo(vxlanLocalTunnelIp));

    // anycast should win if non-null
    vrf.setLayer3Vnis(ImmutableList.of()); // wipe out prior state
    convertVxlans(
        c,
        vsConfig,
        ImmutableMap.of(1001, vrf.getName()),
        loopbackAnycastIp,
        loopbackTunnelIp,
        new Warnings());
    assertThat(vrf.getLayer3Vnis().get(1001).getSourceAddress(), equalTo(loopbackAnycastIp));

    // loopback tunnel ip should win when nothing else is present
    vrf.setLayer3Vnis(ImmutableList.of()); // wipe out prior state
    iface.setVxlanLocalTunnelIp(null);
    convertVxlans(
        c, vsConfig, ImmutableMap.of(1001, vrf.getName()), null, loopbackTunnelIp, new Warnings());
    assertThat(vrf.getLayer3Vnis().get(1001).getSourceAddress(), equalTo(loopbackTunnelIp));
  }

  @Test
  public void testNextHopSelfAll() {
    BgpNeighbor bgpNeighbor = new BgpIpNeighbor("10.0.0.1", Ip.parse("10.0.0.1"));
    {
      // If eBGP and next-hop-self is NOT set, then next-hop should not be self.
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      BgpNeighborIpv4UnicastAddressFamily ipv4af = new BgpNeighborIpv4UnicastAddressFamily();
      bgpNeighbor.setIpv4UnicastAddressFamily(ipv4af);
      ipv4af.setNextHopSelf(false);
      ipv4af.setNextHopSelfAll(false);
      assertThat(getSetNextHop(bgpNeighbor, 20000L), equalTo(null));
    }

    {
      // If eBGP and next-hop-self is set WITHOUT force, then next-hop should be self.
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      BgpNeighborIpv4UnicastAddressFamily ipv4af = new BgpNeighborIpv4UnicastAddressFamily();
      bgpNeighbor.setIpv4UnicastAddressFamily(ipv4af);
      ipv4af.setNextHopSelf(true);
      ipv4af.setNextHopSelfAll(false);
      assertThat(
          getSetNextHop(bgpNeighbor, 20000L), equalTo(new SetNextHop(SelfNextHop.getInstance())));
    }

    {
      // If eBGP and next-hop-self is set WITH force, then next-hop should be self.
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      BgpNeighborIpv4UnicastAddressFamily ipv4af = new BgpNeighborIpv4UnicastAddressFamily();
      bgpNeighbor.setIpv4UnicastAddressFamily(ipv4af);
      ipv4af.setNextHopSelf(true);
      ipv4af.setNextHopSelfAll(true);
      assertThat(
          getSetNextHop(bgpNeighbor, 20000L), equalTo(new SetNextHop(SelfNextHop.getInstance())));
    }

    {
      // If iBGP and next-hop-self NOT set, then next-hop should be null.
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      BgpNeighborIpv4UnicastAddressFamily ipv4af = new BgpNeighborIpv4UnicastAddressFamily();
      bgpNeighbor.setIpv4UnicastAddressFamily(ipv4af);
      ipv4af.setNextHopSelf(false);
      ipv4af.setNextHopSelfAll(false);
      assertThat(getSetNextHop(bgpNeighbor, 10000L), equalTo(null));
    }

    {
      // If iBGP and next-hop-self is set WITHOUT force, then next-hop should be null.
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      BgpNeighborIpv4UnicastAddressFamily ipv4af = new BgpNeighborIpv4UnicastAddressFamily();
      bgpNeighbor.setIpv4UnicastAddressFamily(ipv4af);
      ipv4af.setNextHopSelf(true);
      ipv4af.setNextHopSelfAll(false);
      assertThat(getSetNextHop(bgpNeighbor, 10000L), equalTo(null));
    }

    {
      // If iBGP and next-hop-self is set WITH force, then next-hop should be self.
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      BgpNeighborIpv4UnicastAddressFamily ipv4af = new BgpNeighborIpv4UnicastAddressFamily();
      bgpNeighbor.setIpv4UnicastAddressFamily(ipv4af);
      ipv4af.setNextHopSelf(true);
      ipv4af.setNextHopSelfAll(true);
      assertThat(
          getSetNextHop(bgpNeighbor, 10000L), equalTo(new SetNextHop(SelfNextHop.getInstance())));
    }

    {
      // If iBGP and force is null, then next-hop should be null.
      bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
      BgpNeighborIpv4UnicastAddressFamily ipv4af = new BgpNeighborIpv4UnicastAddressFamily();
      bgpNeighbor.setIpv4UnicastAddressFamily(ipv4af);
      ipv4af.setNextHopSelfAll(true);
      assertThat(getSetNextHop(bgpNeighbor, 10000L), equalTo(null));
    }
  }

  @Test
  public void testGetSetMaxMedMetric() {
    BgpVrf bgpVrf = new BgpVrf("bgpVrf");

    // Test Default Value
    assertThat(getSetMaxMedMetric(bgpVrf), equalTo(null));

    // Set the value and test again
    bgpVrf.setMaxMedAdministrative(1234L);
    assertThat(getSetMaxMedMetric(bgpVrf), equalTo(new SetMetric(new LiteralLong(1234L))));
  }

  @Test
  public void testGenerateBgpCommonPeerConfigMaxMedAdministrative() {
    // setup VI model
    NetworkFactory nf = new NetworkFactory();
    Configuration viConfig =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CUMULUS_CONCATENATED)
            .build();
    nf.vrfBuilder().setOwner(viConfig).setName(DEFAULT_VRF_NAME).build();

    // setup VS model
    CumulusConcatenatedConfiguration vsConfig = new CumulusConcatenatedConfiguration();
    BgpProcess bgpProcess = new BgpProcess();
    vsConfig.getFrrConfiguration().setBgpProcess(bgpProcess);

    // setup BgpVrf and BgpNeighbor
    BgpVrf vrf = bgpProcess.getDefaultVrf();
    vrf.setRouterId(Ip.parse("1.1.1.1"));
    vrf.setAutonomousSystem(20000L);
    Ip peerIp = Ip.parse("10.0.0.1");
    BgpNeighbor bgpNeighbor = new BgpIpNeighbor("10.0.0.1", peerIp);
    bgpNeighbor.setRemoteAs(RemoteAs.explicit(10000));
    vrf.getNeighbors().put("10.0.0.1", bgpNeighbor);

    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(
            Ip.parse("10.0.0.1"), ConfigurationFormat.CUMULUS_CONCATENATED);

    BgpActivePeerConfig.Builder peerConfigBuilder =
        BgpActivePeerConfig.builder().setPeerAddress(peerIp);

    // Method under test
    generateBgpCommonPeerConfig(
        viConfig, vsConfig, bgpNeighbor, 10000L, vrf, newProc, peerConfigBuilder, new Warnings());

    // Test that by default, we don't set a metric

    assertEquals(
        viConfig
            .getRoutingPolicies()
            .get(computeBgpPeerExportPolicyName(DEFAULT_VRF_NAME, bgpNeighbor.getName()))
            .getStatements(),
        ImmutableList.of(
            new If(
                new CallExpr(computeBgpCommonExportPolicyName(DEFAULT_VRF_NAME)),
                ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
                ImmutableList.of(Statements.ExitReject.toStaticStatement()))));

    // Set max-med admin on the vrf, regenerate and test again
    vrf.setMaxMedAdministrative(DEFAULT_MAX_MED);
    generateBgpCommonPeerConfig(
        viConfig, vsConfig, bgpNeighbor, 10000L, vrf, newProc, peerConfigBuilder, new Warnings());

    assertEquals(
        viConfig
            .getRoutingPolicies()
            .get(computeBgpPeerExportPolicyName(DEFAULT_VRF_NAME, bgpNeighbor.getName()))
            .getStatements(),
        ImmutableList.of(
            new If(
                new CallExpr(computeBgpCommonExportPolicyName(DEFAULT_VRF_NAME)),
                ImmutableList.of(
                    new SetMetric(new LiteralLong(DEFAULT_MAX_MED)),
                    Statements.ExitAccept.toStaticStatement()),
                ImmutableList.of(Statements.ExitReject.toStaticStatement()))));
  }
}

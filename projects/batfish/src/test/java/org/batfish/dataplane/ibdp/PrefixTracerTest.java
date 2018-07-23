package org.batfish.dataplane.ibdp;

import static org.batfish.datamodel.Prefix.MAX_PREFIX_LENGTH;
import static org.batfish.dataplane.ibdp.PrefixTracer.SENT;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.fromHostname;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.fromIp;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.toHostname;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.toIp;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.wasFilteredIn;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.wasFilteredOut;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.wasInstalled;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.wasOriginated;
import static org.batfish.dataplane.matchers.PrefixTracerMatchers.wasSent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Configuration.Builder;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests of {@link PrefixTracer} */
public class PrefixTracerTest {

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  private static Prefix _staticRoutePrefix = Prefix.parse("3.3.3.3/32");

  /**
   * Create a network with two nodes and a direct eBGP peering on a /31 interface.
   *
   * @param blockExport specify which export policy to create: one that blocks all routes (if set to
   *     true) or permits them (if set to false)
   */
  private static SortedMap<String, Configuration> twoNodeNetwork(boolean blockExport) {
    /*

       N1+-----------------------+N2
    1.1.1.2                     1.1.1.3
    AS 1                        AS 2
    exporter
    has static routes


    Much of the boiler plate code is to setup proper BGP neighbors & policies

    */

    Ip neighbor1Ip = new Ip("1.1.1.2");
    Ip neighbor2Ip = new Ip("1.1.1.3");
    NetworkFactory nf = new NetworkFactory();
    Builder cb = nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);

    Configuration c1 = cb.setHostname("n1").build();
    RoutingPolicy.Builder pb =
        nf.routingPolicyBuilder()
            .setStatements(
                ImmutableList.of(
                    new SetOrigin(new LiteralOrigin(OriginType.IGP, null)),
                    blockExport
                        ? Statements.ExitReject.toStaticStatement()
                        : Statements.ExitAccept.toStaticStatement()))
            .setName("policy");
    RoutingPolicy policy = pb.setOwner(c1).build();

    c1.getRoutingPolicies().put(policy.getName(), policy);
    Vrf vrf1 = nf.vrfBuilder().setOwner(c1).setName(Configuration.DEFAULT_VRF_NAME).build();
    Interface i1 =
        nf.interfaceBuilder()
            .setAddress(new InterfaceAddress(neighbor1Ip, MAX_PREFIX_LENGTH - 1))
            .setOwner(c1)
            .setVrf(vrf1)
            .setActive(true)
            .build();
    vrf1.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNextHopInterface(i1.getName())
                .setNetwork(_staticRoutePrefix)
                .build()));
    BgpProcess bp = nf.bgpProcessBuilder().setVrf(vrf1).setRouterId(neighbor1Ip).build();
    nf.bgpNeighborBuilder()
        .setBgpProcess(bp)
        .setLocalIp(neighbor1Ip)
        .setLocalAs(1L)
        .setPeerAddress(neighbor2Ip)
        .setRemoteAs(2L)
        .setExportPolicy(policy.getName())
        .build();

    Configuration c2 = cb.setHostname("n2").build();
    Vrf vrf2 = nf.vrfBuilder().setOwner(c2).setName(Configuration.DEFAULT_VRF_NAME).build();
    policy = pb.setOwner(c2).build();
    nf.interfaceBuilder()
        .setAddress(new InterfaceAddress(neighbor2Ip, MAX_PREFIX_LENGTH - 1))
        .setOwner(c2)
        .setActive(true)
        .setVrf(vrf2)
        .build();
    bp = nf.bgpProcessBuilder().setVrf(vrf2).setRouterId(neighbor2Ip).build();
    nf.bgpNeighborBuilder()
        .setBgpProcess(bp)
        .setLocalIp(neighbor2Ip)
        .setLocalAs(2L)
        .setPeerAddress(neighbor1Ip)
        .setRemoteAs(1L)
        .setExportPolicy(policy.getName())
        .build();
    return ImmutableSortedMap.of(c1.getHostname(), c1, c2.getHostname(), c2);
  }

  /** Test initialization */
  @Test
  public void testConstructor() {
    PrefixTracer pt = new PrefixTracer();
    assertThat(pt.getOriginated(), emptyIterable());
    assertThat(pt.getSent().isEmpty(), equalTo(true));
    assertThat(pt.getFiltered(Direction.IN).isEmpty(), equalTo(true));
    assertThat(pt.getFiltered(Direction.OUT).isEmpty(), equalTo(true));
  }

  /** Test basic setters */
  @Test
  public void testSimpleSetters() {
    PrefixTracer pt = new PrefixTracer();
    Prefix prefix = Prefix.parse("1.1.1.1/32");
    Ip testIp1 = new Ip("1.1.1.2");
    Ip testIp2 = new Ip("1.1.1.3");

    // Test considered for origination
    pt.originated(prefix);
    assertThat(pt, wasOriginated(prefix));

    // Test sent
    pt.sentTo(prefix, "n1", testIp1, Configuration.DEFAULT_VRF_NAME, null);
    assertThat(pt, wasSent(prefix, toHostname("n1")));
    assertThat(pt, wasSent(prefix, toIp(testIp1)));

    // Test filtered out
    pt.filtered(prefix, "n1", testIp1, Configuration.DEFAULT_VRF_NAME, null, Direction.OUT);
    assertThat(pt, wasFilteredOut(prefix, toHostname("n1")));
    assertThat(pt, wasFilteredOut(prefix, toIp(testIp1)));

    // Test filtered in
    pt.filtered(prefix, "n2", testIp2, Configuration.DEFAULT_VRF_NAME, null, Direction.IN);
    assertThat(pt, wasFilteredIn(prefix, toHostname("n2")));
    assertThat(pt, wasFilteredIn(prefix, fromIp(testIp2)));

    // Test installed
    pt.installed(prefix, "n2", testIp2, Configuration.DEFAULT_VRF_NAME, null);
    assertThat(pt, wasInstalled(prefix, fromHostname("n2")));
    assertThat(pt, wasInstalled(prefix, fromIp(testIp2)));
  }

  /**
   * Test that all prefixes are marked as allowed (according to policy) by the prefix tracer during
   * dataplane computation
   */
  @Test
  public void testPrefixExportAllowed() throws IOException {

    Batfish batfish = BatfishTestUtils.getBatfish(twoNodeNetwork(false), _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);

    // Test: compute dataplane
    IncrementalDataPlane dp =
        (IncrementalDataPlane) batfish.getDataPlanePlugin().computeDataPlane(false)._dataPlane;
    PrefixTracer pt = dp.getPrefixTracingInfo().get("n1").get(Configuration.DEFAULT_VRF_NAME);

    // Assert that static was considered
    assertThat(pt, wasOriginated(_staticRoutePrefix));
    // route passed export policy and was sent
    assertThat(pt, wasSent(_staticRoutePrefix, toHostname("n2")));

    // Assert prefix was installed on the remote side
    pt = dp.getPrefixTracingInfo().get("n2").get(Configuration.DEFAULT_VRF_NAME);
    assertThat(pt, wasInstalled(_staticRoutePrefix, fromHostname("n1")));
  }

  /**
   * Test that all prefixes are marked as filtered (according to policy) by the prefix tracer during
   * dataplane computation
   */
  @Test
  public void testPrefixExportDenied() throws IOException {

    Batfish batfish = BatfishTestUtils.getBatfish(twoNodeNetwork(true), _folder);
    batfish.getSettings().setDataplaneEngineName(IncrementalDataPlanePlugin.PLUGIN_NAME);

    // Test: compute dataplane
    IncrementalDataPlane dp =
        (IncrementalDataPlane) batfish.getDataPlanePlugin().computeDataPlane(false)._dataPlane;
    PrefixTracer pt = dp.getPrefixTracingInfo().get("n1").get(Configuration.DEFAULT_VRF_NAME);

    // Assert that static route was filtered
    assertThat(pt, wasOriginated(_staticRoutePrefix));
    assertThat(pt, wasFilteredOut(_staticRoutePrefix, toHostname("n2")));
  }

  @Test
  public void testSummarizeEmpty() {
    assertThat(new PrefixTracer().summarize(), anEmptyMap());
  }

  @Test
  public void testSummarize() throws IOException {
    Batfish batfish = BatfishTestUtils.getBatfish(twoNodeNetwork(false), _folder);
    IncrementalDataPlane dp =
        (IncrementalDataPlane) batfish.getDataPlanePlugin().computeDataPlane(false)._dataPlane;
    PrefixTracer pt = dp.getPrefixTracingInfo().get("n1").get(Configuration.DEFAULT_VRF_NAME);

    // Test: summarize pt
    Map<Prefix, Map<String, Set<String>>> summary = pt.summarize();

    assertThat(
        summary, hasEntry(equalTo(_staticRoutePrefix), hasEntry(equalTo(SENT), contains("n2"))));
  }

  @Test
  public void testSummarizeDataplaneAccess() throws IOException {
    Batfish batfish = BatfishTestUtils.getBatfish(twoNodeNetwork(false), _folder);
    IncrementalDataPlane dp =
        (IncrementalDataPlane) batfish.getDataPlanePlugin().computeDataPlane(false)._dataPlane;

    // Test: get summary directly from data plane
    Map<Prefix, Map<String, Set<String>>> summary =
        dp.getPrefixTracingInfoSummary().get("n1").get(Configuration.DEFAULT_VRF_NAME);

    assertThat(
        summary, hasEntry(equalTo(_staticRoutePrefix), hasEntry(equalTo(SENT), contains("n2"))));
  }
}

package org.batfish.z3;

import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.batfish.datamodel.acl.AclLineMatchExprs.TRUE;
import static org.batfish.datamodel.matchers.FlowHistoryInfoMatchers.hasFlow;
import static org.batfish.datamodel.matchers.FlowMatchers.hasDstIp;
import static org.batfish.datamodel.matchers.FlowMatchers.hasSrcIp;
import static org.batfish.main.BatfishTestUtils.getBatfish;
import static org.batfish.specifier.LocationSpecifiers.ALL_LOCATIONS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Configuration.Builder;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.main.Batfish;
import org.batfish.question.ReachabilityParameters;
import org.batfish.question.differentialreachability.DifferentialReachabilityParameters;
import org.batfish.question.differentialreachability.DifferentialReachabilityResult;
import org.batfish.specifier.InferFromLocationIpSpaceSpecifier;
import org.batfish.specifier.IntersectionLocationSpecifier;
import org.batfish.specifier.NameRegexInterfaceLocationSpecifier;
import org.batfish.specifier.NameRegexNodeSpecifier;
import org.batfish.specifier.NodeNameRegexInterfaceLocationSpecifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReducedReachabilityTest {
  private static final String LOOPBACK = "Loopback0";
  private static final String NODE1 = "node1";
  private static final Ip NODE1_LOOPBACK_IP = new Ip("1.1.1.1");
  private static final String NODE2 = "node2";
  private static final Ip NODE2_ALTERNATE_IP = new Ip("2.2.2.2");
  private static final String PHYSICAL = "FastEthernet0/0";

  private Builder _cb;

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private org.batfish.datamodel.Interface.Builder _ib;

  private NetworkFactory _nf;

  private org.batfish.datamodel.Vrf.Builder _vb;

  private SortedMap<String, Configuration> generateConfigs(boolean delta) {
    Configuration node1 = _cb.setHostname(NODE1).build();
    Vrf v1 = _vb.setOwner(node1).build();
    _ib.setOwner(node1).setVrf(v1);
    _ib.setName(LOOPBACK).setAddresses(new InterfaceAddress("1.1.1.1/32")).build();
    _ib.setName(PHYSICAL).setAddresses(new InterfaceAddress("1.1.1.2/31")).build();
    if (!delta) {
      v1.setStaticRoutes(
          ImmutableSortedSet.of(
              StaticRoute.builder()
                  .setNetwork(Prefix.parse("2.2.2.2/32"))
                  .setNextHopInterface(PHYSICAL)
                  .setAdministrativeCost(1)
                  .build()));
    }

    Configuration node2 = _cb.setHostname(NODE2).build();
    Vrf v2 = _vb.setOwner(node2).build();
    _ib.setOwner(node2).setVrf(v2);
    _ib.setName(PHYSICAL)
        .setAddresses(new InterfaceAddress("1.1.1.3/31"), new InterfaceAddress("2.2.2.2/32"))
        .build();
    v2.setStaticRoutes(
        ImmutableSortedSet.of(
            StaticRoute.builder()
                .setNetwork(Prefix.parse("1.1.1.1/32"))
                .setNextHopInterface(PHYSICAL)
                .setAdministrativeCost(1)
                .build()));

    return ImmutableSortedMap.of(NODE1, node1, NODE2, node2);
  }

  private Batfish initBatfish() throws IOException {
    SortedMap<String, Configuration> baseConfigs = generateConfigs(false);
    SortedMap<String, Configuration> deltaConfigs = generateConfigs(true);
    Batfish batfish = getBatfish(baseConfigs, deltaConfigs, _folder);

    batfish.pushBaseSnapshot();
    batfish.computeDataPlane(true);
    batfish.popSnapshot();

    batfish.pushDeltaSnapshot();
    batfish.computeDataPlane(true);
    batfish.popSnapshot();

    return batfish;
  }

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(CISCO_IOS);
    _vb = _nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    _ib = _nf.interfaceBuilder();
  }

  /** Test that we detect reduced reachability caused by a removed static route. */
  @Test
  public void testStaticRouteRemoved() throws IOException {
    Batfish batfish = initBatfish();
    AnswerElement answer =
        batfish.reducedReachability(
            ReachabilityParameters.builder()
                .setActions(ImmutableSortedSet.of(FlowDisposition.ACCEPTED))
                .setFinalNodesSpecifier(new NameRegexNodeSpecifier(Pattern.compile(NODE2)))
                .setHeaderSpace(
                    HeaderSpace.builder().setDstIps(NODE2_ALTERNATE_IP.toIpSpace()).build())
                .setSourceLocationSpecifier(
                    /* Source = loopback0 on node1 */
                    new IntersectionLocationSpecifier(
                        new NodeNameRegexInterfaceLocationSpecifier(Pattern.compile(NODE1)),
                        new NameRegexInterfaceLocationSpecifier(Pattern.compile(LOOPBACK))))
                .build());
    assertThat(answer, instanceOf(FlowHistory.class));
    FlowHistory flowHistory = (FlowHistory) answer;
    assertThat(flowHistory.getTraces().entrySet(), hasSize(1));
    assertThat(
        flowHistory.getTraces().values(),
        contains(hasFlow(allOf(hasSrcIp(NODE1_LOOPBACK_IP), hasDstIp(NODE2_ALTERNATE_IP)))));
  }

  private static DifferentialReachabilityParameters parameters(Batfish batfish) {
    return new DifferentialReachabilityParameters(
        ImmutableSet.of(FlowDisposition.ACCEPTED),
        ImmutableSet.of(),
        batfish.loadConfigurations().keySet(),
        TRUE,
        false,
        InferFromLocationIpSpaceSpecifier.INSTANCE.resolve(
            ALL_LOCATIONS.resolve(batfish.specifierContext()), batfish.specifierContext()),
        ImmutableSet.of());
  }

  @Test
  public void testBDDReducedReachability() throws IOException {
    Batfish batfish = initBatfish();
    DifferentialReachabilityResult differentialReachabilityResult =
        batfish.bddReducedReachability(parameters(batfish));
    assertThat(differentialReachabilityResult.getIncreasedReachabilityFlows(), empty());
    Set<Flow> flows = differentialReachabilityResult.getDecreasedReachabilityFlows();
    assertThat(flows, hasSize(1));
    assertThat(flows, contains(allOf(hasSrcIp(NODE1_LOOPBACK_IP), hasDstIp(NODE2_ALTERNATE_IP))));
  }
}

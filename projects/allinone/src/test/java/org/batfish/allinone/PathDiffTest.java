package org.batfish.allinone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.SortedMap;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.ForwardingAction;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.question.ReachabilityParameters;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PathDiffTest {
  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  /** 3.3.3.3/32 <--- A <---> B ---> 4.4.4.4/32 */
  private static SortedMap<String, Configuration> twoNodeNetwork(boolean connected) {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb =
        nf.configurationBuilder().setConfigurationFormat(ConfigurationFormat.CISCO_IOS);
    Configuration cA = cb.build();
    Configuration cB = cb.build();

    Vrf.Builder vb = nf.vrfBuilder();
    Vrf vA = vb.setOwner(cA).build();
    Vrf vB = vb.setOwner(cB).build();

    Interface.Builder ib = nf.interfaceBuilder().setActive(true).setBandwidth(1E9d);

    // A's interface
    Prefix pA = Prefix.parse("3.3.3.3/32");
    ib.setOwner(cA)
        .setVrf(vA)
        .setAddress(new InterfaceAddress(pA.getStartIp(), pA.getPrefixLength()))
        .build();

    // B's interface
    Prefix pB = Prefix.parse("4.4.4.4/32");
    ib.setOwner(cB)
        .setVrf(vB)
        .setAddress(new InterfaceAddress(pB.getStartIp(), pB.getPrefixLength()))
        .build();

    // A's interface on link to B
    Prefix pAB = Prefix.parse("10.0.0.0/31");
    ib.setOwner(cA)
        .setVrf(vA)
        .setAddress(new InterfaceAddress(pAB.getStartIp(), pAB.getPrefixLength()))
        .build();

    // B's interface on link to A
    ib.setOwner(cB)
        .setVrf(vB)
        .setAddress(new InterfaceAddress(pAB.getEndIp(), pAB.getPrefixLength()))
        .build();

    if (connected) {
      // add a static route from A to pB
      StaticRoute.Builder rb = StaticRoute.builder();
      vA.getStaticRoutes().add(rb.setNetwork(pB).setNextHopIp(pAB.getEndIp()).build());
      // add a static route from B to pA
      vB.getStaticRoutes().add(rb.setNetwork(pA).setNextHopIp(pAB.getStartIp()).build());
    }

    return ImmutableSortedMap.of(cA.getHostname(), cA, cB.getHostname(), cB);
  }

  @Test
  public void testPathDiff() throws IOException {
    SortedMap<String, Configuration> baseConfigs = twoNodeNetwork(true);
    SortedMap<String, Configuration> deltaConfigs = twoNodeNetwork(false);
    Batfish batfish = BatfishTestUtils.getBatfish(baseConfigs, deltaConfigs, tmp);

    batfish.pushBaseEnvironment();
    batfish.computeDataPlane(true);
    batfish.popEnvironment();

    batfish.pushDeltaEnvironment();
    batfish.computeDataPlane(true);
    batfish.popEnvironment();

    batfish.checkDifferentialDataPlaneQuestionDependencies();
    AnswerElement answer =
        batfish.pathDiff(
            ReachabilityParameters.builder()
                .setActions(ImmutableSortedSet.of(ForwardingAction.ACCEPT))
                .setFinalNodesSpecifier(AllNodesNodeSpecifier.INSTANCE)
                .build());
    assertThat(answer, Matchers.instanceOf(FlowHistory.class));

    FlowHistory flowHistory = (FlowHistory) answer;

    assertThat(flowHistory.getTraces().entrySet(), hasSize(2));
    assertThat(
        flowHistory.getTraces().keySet(),
        containsInAnyOrder(
            ImmutableList.of(
                Matchers.containsString("dstIp:3.3.3.3"),
                Matchers.containsString("dstIp:4.4.4.4"))));
  }
}

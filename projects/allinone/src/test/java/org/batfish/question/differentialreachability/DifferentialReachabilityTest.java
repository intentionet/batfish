package org.batfish.question.differentialreachability;

import static org.batfish.datamodel.ConfigurationFormat.CISCO_IOS;
import static org.batfish.datamodel.matchers.FlowTraceMatchers.hasDisposition;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.matchers.TableAnswerElementMatchers.hasRows;
import static org.batfish.main.BatfishTestUtils.getBatfish;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.SortedMap;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.FlowDisposition;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.matchers.FlowMatchers;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.main.Batfish;
import org.batfish.question.specifiers.DispositionSpecifier;
import org.batfish.question.specifiers.PathConstraintsInput;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** End-to-end tests of {@link DifferentialReachabilityQuestion}. */
public class DifferentialReachabilityTest {
  private static final String LOOPBACK = "Loopback0";
  private static final String NODE1 = "node1";
  private static final String NODE2 = "node2";
  private static final String PHYSICAL = "FastEthernet0/0";

  private Configuration.Builder _cb;

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private org.batfish.datamodel.Interface.Builder _ib;

  private NetworkFactory _nf;

  private org.batfish.datamodel.Vrf.Builder _vb;

  public DifferentialReachabilityTest() {}

  @Before
  public void setup() {
    _nf = new NetworkFactory();
    _cb = _nf.configurationBuilder().setConfigurationFormat(CISCO_IOS);
    _vb = _nf.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME);
    _ib = _nf.interfaceBuilder();
  }

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
                  .setAdministrativeCost(1)
                  .setNetwork(Prefix.parse("2.2.2.2/32"))
                  .setNextHopInterface(PHYSICAL)
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

  @Test
  public void testAccepted() throws IOException {
    Question question =
        new DifferentialReachabilityQuestion(
            new DispositionSpecifier(ImmutableSet.of(FlowDisposition.ACCEPTED)),
            PacketHeaderConstraints.unconstrained(),
            false,
            PathConstraintsInput.unconstrained());
    Batfish batfish = initBatfish();
    TableAnswerElement answer = new DifferentialReachabilityAnswerer(question, batfish).answer();
    Ip dstIp = new Ip("2.2.2.2");
    assertThat(
        answer,
        hasRows(
            contains(
                ImmutableList.of(
                    allOf(
                        hasColumn(
                            DifferentialReachabilityAnswerer.COL_FLOW,
                            FlowMatchers.hasDstIp(dstIp),
                            Schema.FLOW),
                        // at least one trace in snapshot is accepted
                        hasColumn(
                            DifferentialReachabilityAnswerer.COL_BASE_TRACES,
                            hasItem(hasDisposition(FlowDisposition.ACCEPTED)),
                            Schema.list(Schema.FLOW_TRACE)),
                        // no trace in reference snapshot is accepted
                        hasColumn(
                            DifferentialReachabilityAnswerer.COL_DELTA_TRACES,
                            not(hasItem(hasDisposition(FlowDisposition.ACCEPTED))),
                            Schema.list(Schema.FLOW_TRACE)))))));
  }

  @Test
  public void testHeaderSpaceConstraint1() throws IOException {
    Question question =
        new DifferentialReachabilityQuestion(
            new DispositionSpecifier(ImmutableSet.of(FlowDisposition.ACCEPTED)),
            PacketHeaderConstraints.builder().setDstIp("2.2.2.2").build(),
            false,
            PathConstraintsInput.unconstrained());

    Batfish batfish = initBatfish();
    TableAnswerElement answer = new DifferentialReachabilityAnswerer(question, batfish).answer();
    Ip dstIp = new Ip("2.2.2.2");
    assertThat(
        answer,
        hasRows(
            contains(
                ImmutableList.of(
                    allOf(
                        hasColumn(
                            DifferentialReachabilityAnswerer.COL_FLOW,
                            FlowMatchers.hasDstIp(dstIp),
                            Schema.FLOW),
                        // at least one trace in snapshot is accepted
                        hasColumn(
                            DifferentialReachabilityAnswerer.COL_BASE_TRACES,
                            hasItem(hasDisposition(FlowDisposition.ACCEPTED)),
                            Schema.list(Schema.FLOW_TRACE)),
                        // no trace in reference snapshot is accepted
                        hasColumn(
                            DifferentialReachabilityAnswerer.COL_DELTA_TRACES,
                            not(hasItem(hasDisposition(FlowDisposition.ACCEPTED))),
                            Schema.list(Schema.FLOW_TRACE)))))));
  }

  @Test
  public void testHeaderSpaceConstraint2() throws IOException {
    Question question =
        new DifferentialReachabilityQuestion(
            new DispositionSpecifier(ImmutableSet.of(FlowDisposition.ACCEPTED)),
            PacketHeaderConstraints.builder().setDstIp("5.5.5.5").build(),
            false,
            PathConstraintsInput.unconstrained());

    Batfish batfish = initBatfish();
    TableAnswerElement answer = new DifferentialReachabilityAnswerer(question, batfish).answer();
    assertThat(answer.getRows().size(), equalTo(0));
  }
}

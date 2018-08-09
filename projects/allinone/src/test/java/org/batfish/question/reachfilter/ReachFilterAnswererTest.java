package org.batfish.question.reachfilter;

import static org.batfish.datamodel.IpAccessListLine.ACCEPT_ALL;
import static org.batfish.datamodel.IpAccessListLine.accepting;
import static org.batfish.datamodel.IpAccessListLine.rejecting;
import static org.batfish.datamodel.acl.AclLineMatchExprs.ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.matchers.FlowMatchers.hasDstIp;
import static org.batfish.datamodel.matchers.FlowMatchers.hasIngressInterface;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.matchers.TableAnswerElementMatchers.hasRows;
import static org.batfish.question.reachfilter.ReachFilterAnswerer.toDenyAcl;
import static org.batfish.question.reachfilter.ReachFilterAnswerer.toMatchLineAcl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import org.batfish.common.Pair;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Interface.Builder;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class ReachFilterAnswererTest {
  private static final String IFACE1 = "iface1";

  private static final String IFACE2 = "iface2";

  private static final Ip IP0 = new Ip("1.1.1.0");

  private static final Ip IP1 = new Ip("1.1.1.1");

  private static final Ip IP2 = new Ip("1.1.1.2");

  private static final Ip IP3 = new Ip("1.1.1.3");

  private static final IpAccessList ACL =
      IpAccessList.builder()
          .setName("acl")
          .setLines(
              ImmutableList.of(
                  accepting().setMatchCondition(matchDst(IP0)).build(),
                  IpAccessListLine.rejecting().setMatchCondition(matchDst(IP1)).build(),
                  IpAccessListLine.rejecting().setMatchCondition(matchDst(IP2)).build(),
                  accepting().setMatchCondition(matchDst(IP3)).build()))
          .build();

  private static final IpAccessList ACCEPT_ALL_ACL =
      IpAccessList.builder().setName("ACCEPT_ALL").setLines(ImmutableList.of(ACCEPT_ALL)).build();

  private static final IpAccessList BLOCKED_LINE_ACL =
      IpAccessList.builder()
          .setName("blockedAcl")
          .setLines(
              ImmutableList.of(
                  accepting().setMatchCondition(matchDst(IP0)).build(),
                  IpAccessListLine.rejecting().setMatchCondition(matchDst(IP1)).build(),
                  accepting().setMatchCondition(matchDst(Prefix.parse("1.1.1.0/31"))).build()))
          .build();

  private static final IpAccessList DENY_ACL =
      IpAccessList.builder()
          .setName("denyAcl")
          .setLines(
              ImmutableList.of(
                  IpAccessListLine.rejecting().setMatchCondition(matchDst(IP0)).build(),
                  accepting().setMatchCondition(matchDst(IP1)).build(),
                  accepting().setMatchCondition(matchDst(IP2)).build(),
                  IpAccessListLine.rejecting().setMatchCondition(matchDst(IP3)).build(),
                  ACCEPT_ALL))
          .build();

  private static final IpAccessList MATCH_LINE2_ACL =
      IpAccessList.builder()
          .setName("matchLine2Acl")
          .setLines(
              ImmutableList.of(
                  IpAccessListLine.rejecting().setMatchCondition(matchDst(IP0)).build(),
                  IpAccessListLine.rejecting().setMatchCondition(matchDst(IP1)).build(),
                  accepting().setMatchCondition(matchDst(IP2)).build()))
          .build();

  private static final IpAccessList SRC_ACL =
      IpAccessList.builder()
          .setName("srcAcl")
          .setLines(
              ImmutableList.of(
                  accepting()
                      .setMatchCondition(and(ORIGINATING_FROM_DEVICE, matchDst(IP0)))
                      .build(),
                  accepting()
                      .setMatchCondition(and(matchSrcInterface(IFACE1), matchDst(IP1)))
                      .build(),
                  accepting()
                      .setMatchCondition(and(matchSrcInterface(IFACE2), matchDst(IP2)))
                      .build(),
                  accepting()
                      .setMatchCondition(and(matchSrcInterface(IFACE1), matchSrcInterface(IFACE2)))
                      .build(),
                  accepting()
                      .setMatchCondition(and(ORIGINATING_FROM_DEVICE, matchSrcInterface(IFACE1)))
                      .build()))
          .build();

  private static final IpAccessList REJECT_ALL_ACL =
      IpAccessList.builder()
          .setName("REJECT_ALL")
          .setLines(ImmutableList.of(IpAccessListLine.REJECT_ALL))
          .build();

  @Rule public TemporaryFolder _tmp = new TemporaryFolder();

  private Batfish _batfish;

  private Configuration _config;

  @Before
  public void setup() throws IOException {
    NetworkFactory nf = new NetworkFactory();
    _config =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("A")
            .build();
    _config
        .getIpAccessLists()
        .putAll(
            ImmutableMap.of(
                ACL.getName(),
                ACL,
                BLOCKED_LINE_ACL.getName(),
                BLOCKED_LINE_ACL,
                SRC_ACL.getName(),
                SRC_ACL));

    Builder ib = nf.interfaceBuilder().setOwner(_config);
    ib.setName(IFACE1).build();
    ib.setName(IFACE2).build();

    SortedMap<String, Configuration> configurationMap =
        ImmutableSortedMap.of(_config.getHostname(), _config);

    _batfish = BatfishTestUtils.getBatfish(configurationMap, _tmp);
  }

  @Test
  public void testGetQueryAcls_permit() {
    ReachFilterQuestion question = new ReachFilterQuestion();
    question.setQuery("permit");
    question.setNodesSpecifier(new NodesSpecifier(".*"));
    question.setFilterSpecifierInput(ACL.getName());
    ReachFilterAnswerer answerer = new ReachFilterAnswerer(question, _batfish);
    List<Pair<String, IpAccessList>> queryAcls = answerer.getQueryAcls(question);
    assertThat(queryAcls, hasSize(1));
    String queryConfig = queryAcls.get(0).getFirst();
    IpAccessList queryAcl = queryAcls.get(0).getSecond();
    assertThat(queryConfig, equalTo(_config.getHostname()));
    assertThat(queryAcl, is(ACL));
  }

  @Test
  public void testGetQueryAcls_deny() {
    ReachFilterQuestion question = new ReachFilterQuestion();
    question.setQuery("deny");
    question.setNodesSpecifier(new NodesSpecifier(".*"));
    question.setFilterSpecifierInput(ACL.getName());
    ReachFilterAnswerer answerer = new ReachFilterAnswerer(question, _batfish);
    List<Pair<String, IpAccessList>> queryAcls = answerer.getQueryAcls(question);
    assertThat(queryAcls, hasSize(1));
    String queryConfig = queryAcls.get(0).getFirst();
    IpAccessList queryAcl = queryAcls.get(0).getSecond();
    assertThat(queryConfig, equalTo(_config.getHostname()));
    assertThat(queryAcl, is(DENY_ACL));
  }

  @Test
  public void testGetQueryAcls_matchLine2() {
    ReachFilterQuestion question = new ReachFilterQuestion();
    question.setQuery("matchLine 2");
    question.setNodesSpecifier(new NodesSpecifier(".*"));
    question.setFilterSpecifierInput(ACL.getName());
    ReachFilterAnswerer answerer = new ReachFilterAnswerer(question, _batfish);
    List<Pair<String, IpAccessList>> queryAcls = answerer.getQueryAcls(question);
    assertThat(queryAcls, hasSize(1));
    String queryConfig = queryAcls.get(0).getFirst();
    IpAccessList queryAcl = queryAcls.get(0).getSecond();
    assertThat(queryConfig, equalTo(_config.getHostname()));
    assertThat(queryAcl, is(MATCH_LINE2_ACL));
  }

  @Test
  public void testReachFilter_deny_ACCEPT_ALL() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, toDenyAcl(ACCEPT_ALL_ACL));
    assertThat("Should not find permitted flow", !permitFlow.isPresent());
  }

  @Test
  public void testReachFilter_deny_REJECT_ALL() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, toDenyAcl(REJECT_ALL_ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
  }

  @Test
  public void testReachFilter_permit_ACCEPT_ALL() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, ACCEPT_ALL_ACL);
    assertThat("Should find permitted flow", permitFlow.isPresent());
  }

  @Test
  public void testReachFilter_permit_REJECT_ALL() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, REJECT_ALL_ACL);
    assertThat(permitFlow, equalTo(Optional.empty()));
  }

  @Test
  public void testReachFilter_permit() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, ACL);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(oneOf(IP0, IP3)));
  }

  @Test
  public void testReachFilter_deny() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, toDenyAcl(ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(not(oneOf(IP0, IP3))));
  }

  @Test
  public void testReachFilter_matchLine() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, toMatchLineAcl(0, ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP0));

    permitFlow = _batfish.reachFilter(_config, toMatchLineAcl(1, ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP1));

    permitFlow = _batfish.reachFilter(_config, toMatchLineAcl(2, ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP2));

    permitFlow = _batfish.reachFilter(_config, toMatchLineAcl(3, ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP3));
  }

  @Test
  public void testReachFilter_matchLine_blocked() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, toMatchLineAcl(2, BLOCKED_LINE_ACL));
    assertThat("Should not find permitted flow", !permitFlow.isPresent());
  }

  @Test
  public void testTraceFilter() {
    ReachFilterQuestion question = new ReachFilterQuestion();
    Flow flow =
        Flow.builder().setIngressNode(_config.getHostname()).setDstIp(IP2).setTag("tag").build();
    ReachFilterAnswerer answerer = new ReachFilterAnswerer(question, _batfish);
    TableAnswerElement ae = answerer.traceFilter(_config, ACL, flow);
    assertThat(
        ae,
        hasRows(
            contains(
                allOf(
                    hasColumn("action", equalTo("REJECT"), Schema.STRING),
                    hasColumn("filterName", equalTo(ACL.getName()), Schema.STRING)))));
  }

  @Test
  public void testAnswer() {
    ReachFilterQuestion question = new ReachFilterQuestion();
    ReachFilterAnswerer answerer = new ReachFilterAnswerer(question, _batfish);
    question.setNodesSpecifier(new NodesSpecifier(".*"));
    question.setFilterSpecifierInput(null);
    TableAnswerElement ae = (TableAnswerElement) answerer.answer();
    assertThat(
        ae,
        hasRows(
            containsInAnyOrder(
                ImmutableList.of(
                    allOf(
                        hasColumn("action", equalTo("ACCEPT"), Schema.STRING),
                        hasColumn("filterName", equalTo(ACL.getName()), Schema.STRING),
                        hasColumn("lineNumber", oneOf(0, 3), Schema.INTEGER)),
                    allOf(
                        hasColumn("action", equalTo("ACCEPT"), Schema.STRING),
                        hasColumn("filterName", equalTo(BLOCKED_LINE_ACL.getName()), Schema.STRING),
                        hasColumn("lineNumber", equalTo(0), Schema.INTEGER)),
                    allOf(
                        hasColumn("action", equalTo("ACCEPT"), Schema.STRING),
                        hasColumn("filterName", equalTo(SRC_ACL.getName()), Schema.STRING),
                        hasColumn("lineNumber", oneOf(0, 1, 2), Schema.INTEGER))))));
  }

  @Test
  public void testMatchSrcInterface() {
    Optional<Flow> flow = _batfish.reachFilter(_config, toMatchLineAcl(0, SRC_ACL));
    assertThat(flow.get(), allOf(hasIngressInterface(nullValue()), hasDstIp(IP0)));

    flow = _batfish.reachFilter(_config, toMatchLineAcl(1, SRC_ACL));
    assertThat(flow.get(), allOf(hasIngressInterface(IFACE1), hasDstIp(IP1)));

    flow = _batfish.reachFilter(_config, toMatchLineAcl(2, SRC_ACL));
    assertThat(flow.get(), allOf(hasIngressInterface(IFACE2), hasDstIp(IP2)));

    // cannot have two different source interfaces
    flow = _batfish.reachFilter(_config, toMatchLineAcl(3, SRC_ACL));
    assertThat(flow, equalTo(Optional.empty()));

    // cannot have originate from device and have a source interface
    flow = _batfish.reachFilter(_config, toMatchLineAcl(4, SRC_ACL));
    assertThat(flow, equalTo(Optional.empty()));
  }

  @Test
  public void testSane() {
    // an ACL that can only match with an insane interface
    IpAccessList insaneAcl =
        IpAccessList.builder()
            .setName("srcAcl")
            .setLines(
                ImmutableList.of(
                    rejecting().setMatchCondition(ORIGINATING_FROM_DEVICE).build(),
                    rejecting().setMatchCondition(matchSrcInterface(IFACE1)).build(),
                    rejecting().setMatchCondition(matchSrcInterface(IFACE2)).build(),
                    ACCEPT_ALL))
            .build();
    Optional<Flow> flow = _batfish.reachFilter(_config, toMatchLineAcl(0, SRC_ACL));
    assertThat(flow, equalTo(Optional.empty()));
  }
}

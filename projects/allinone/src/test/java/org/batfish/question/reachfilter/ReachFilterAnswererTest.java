package org.batfish.question.reachfilter;

import static org.batfish.datamodel.matchers.FlowMatchers.hasDstIp;
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
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.acl.AclLineMatchExprs;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.questions.FiltersSpecifier;
import org.batfish.datamodel.questions.NodesSpecifier;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class ReachFilterAnswererTest {
  private static final Ip IP0 = new Ip("1.1.1.0");

  private static final Ip IP1 = new Ip("1.1.1.1");

  private static final Ip IP2 = new Ip("1.1.1.2");

  private static final Ip IP3 = new Ip("1.1.1.3");

  private static final IpAccessList ACL =
      IpAccessList.builder()
          .setName("acl")
          .setLines(
              ImmutableList.of(
                  IpAccessListLine.accepting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP0))
                      .build(),
                  IpAccessListLine.rejecting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP1))
                      .build(),
                  IpAccessListLine.rejecting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP2))
                      .build(),
                  IpAccessListLine.accepting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP3))
                      .build()))
          .build();

  private static final IpAccessList ACCEPT_ALL_ACL =
      IpAccessList.builder()
          .setName("ACCEPT_ALL")
          .setLines(ImmutableList.of(IpAccessListLine.ACCEPT_ALL))
          .build();

  private static final IpAccessList BLOCKED_LINE_ACL =
      IpAccessList.builder()
          .setName("blockedAcl")
          .setLines(
              ImmutableList.of(
                  IpAccessListLine.accepting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP0))
                      .build(),
                  IpAccessListLine.rejecting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP1))
                      .build(),
                  IpAccessListLine.accepting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(Prefix.parse("1.1.1.0/31")))
                      .build()))
          .build();

  private static final IpAccessList DENY_ACL =
      IpAccessList.builder()
          .setName("foo")
          .setLines(
              ImmutableList.of(
                  IpAccessListLine.rejecting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP0))
                      .build(),
                  IpAccessListLine.accepting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP1))
                      .build(),
                  IpAccessListLine.accepting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP2))
                      .build(),
                  IpAccessListLine.rejecting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP3))
                      .build(),
                  IpAccessListLine.ACCEPT_ALL))
          .build();

  private static final IpAccessList MATCH_LINE2_ACL =
      IpAccessList.builder()
          .setName("foo")
          .setLines(
              ImmutableList.of(
                  IpAccessListLine.rejecting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP0))
                      .build(),
                  IpAccessListLine.rejecting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP1))
                      .build(),
                  IpAccessListLine.accepting()
                      .setMatchCondition(AclLineMatchExprs.matchDst(IP2))
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
        .putAll(ImmutableMap.of(ACL.getName(), ACL, BLOCKED_LINE_ACL.getName(), BLOCKED_LINE_ACL));

    SortedMap<String, Configuration> configurationMap =
        ImmutableSortedMap.of(_config.getHostname(), _config);

    _batfish = BatfishTestUtils.getBatfish(configurationMap, _tmp);
  }

  @Test
  public void testGetQueryAcls_permit() {
    ReachFilterQuestion question = new ReachFilterQuestion();
    question.setQuery("permit");
    question.setNodesSpecifier(new NodesSpecifier(".*"));
    question.setFiltersSpecifier(new FiltersSpecifier(ACL.getName()));
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
    question.setFiltersSpecifier(new FiltersSpecifier(ACL.getName()));
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
    question.setFiltersSpecifier(new FiltersSpecifier(ACL.getName()));
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
    Optional<Flow> permitFlow =
        _batfish.reachFilter(_config.getHostname(), toDenyAcl(ACCEPT_ALL_ACL));
    assertThat("Should not find permitted flow", !permitFlow.isPresent());
  }

  @Test
  public void testReachFilter_deny_REJECT_ALL() {
    Optional<Flow> permitFlow =
        _batfish.reachFilter(_config.getHostname(), toDenyAcl(REJECT_ALL_ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
  }

  @Test
  public void testReachFilter_permit_ACCEPT_ALL() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config.getHostname(), ACCEPT_ALL_ACL);
    assertThat("Should find permitted flow", permitFlow.isPresent());
  }

  @Test
  public void testReachFilter_permit_REJECT_ALL() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config.getHostname(), REJECT_ALL_ACL);
    assertThat(permitFlow, equalTo(Optional.empty()));
  }

  @Test
  public void testReachFilter_permit() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config.getHostname(), ACL);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(oneOf(IP0, IP3)));
  }

  @Test
  public void testReachFilter_deny() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config.getHostname(), toDenyAcl(ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(not(oneOf(IP0, IP3))));
  }

  @Test
  public void testReachFilter_matchLine() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config.getHostname(), toMatchLineAcl(0, ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP0));

    permitFlow = _batfish.reachFilter(_config.getHostname(), toMatchLineAcl(1, ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP1));

    permitFlow = _batfish.reachFilter(_config.getHostname(), toMatchLineAcl(2, ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP2));

    permitFlow = _batfish.reachFilter(_config.getHostname(), toMatchLineAcl(3, ACL));
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP3));
  }

  @Test
  public void testReachFilter_matchLine_blocked() {
    Optional<Flow> permitFlow =
        _batfish.reachFilter(_config.getHostname(), toMatchLineAcl(2, BLOCKED_LINE_ACL));
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
    question.setFiltersSpecifier(new FiltersSpecifier(".*"));
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
                        hasColumn("lineNumber", equalTo(0), Schema.INTEGER))))));
  }
}

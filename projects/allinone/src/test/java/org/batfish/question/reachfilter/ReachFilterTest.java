package org.batfish.question.reachfilter;

import static org.batfish.datamodel.IpAccessListLine.ACCEPT_ALL;
import static org.batfish.datamodel.IpAccessListLine.accepting;
import static org.batfish.datamodel.IpAccessListLine.rejecting;
import static org.batfish.datamodel.acl.AclLineMatchExprs.ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchDst;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.matchers.FlowMatchers.hasDstIp;
import static org.batfish.datamodel.matchers.FlowMatchers.hasDstPort;
import static org.batfish.datamodel.matchers.FlowMatchers.hasIngressInterface;
import static org.batfish.datamodel.matchers.FlowMatchers.hasSrcIp;
import static org.batfish.datamodel.matchers.FlowMatchers.hasSrcPort;
import static org.batfish.datamodel.matchers.RowMatchers.hasColumn;
import static org.batfish.datamodel.matchers.TableAnswerElementMatchers.hasRows;
import static org.batfish.question.reachfilter.ReachFilterAnswerer.toDenyAcl;
import static org.batfish.question.reachfilter.ReachFilterAnswerer.toMatchLineAcl;
import static org.batfish.question.testfilters.TestFiltersAnswerer.COL_ACTION;
import static org.batfish.question.testfilters.TestFiltersAnswerer.COL_FILTER_NAME;
import static org.batfish.question.testfilters.TestFiltersAnswerer.COL_LINE_NUMBER;
import static org.batfish.specifier.LocationSpecifiers.ALL_LOCATIONS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import org.batfish.common.Pair;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface.Builder;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.question.ReachFilterParameters;
import org.batfish.question.testfilters.TestFiltersAnswerer;
import org.batfish.specifier.ConstantIpSpaceSpecifier;
import org.batfish.specifier.NameRegexInterfaceLinkLocationSpecifier;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** End-to-end tests of {@link org.batfish.question.reachfilter}. */
public final class ReachFilterTest {
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

  @ClassRule public static TemporaryFolder _tmp = new TemporaryFolder();

  private static Batfish _batfish;

  private static Configuration _config;

  private static ReachFilterParameters _allLocationsParams;

  @BeforeClass
  public static void setup() throws IOException {
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

    Builder ib = nf.interfaceBuilder().setActive(true).setOwner(_config);
    ib.setName(IFACE1).build();
    ib.setName(IFACE2).build();

    SortedMap<String, Configuration> configurationMap =
        ImmutableSortedMap.of(_config.getHostname(), _config);

    _batfish = BatfishTestUtils.getBatfish(configurationMap, _tmp);
    _allLocationsParams =
        new ReachFilterQuestion()
            .toReachFilterParameters()
            .toBuilder()
            .setStartLocationSpecifier(ALL_LOCATIONS)
            .build();
  }

  @Test
  public void testGetQueryAcls_permit() {
    ReachFilterQuestion question =
        ReachFilterQuestion.builder()
            .setFilterSpecifierInput(ACL.getName())
            .setQuery("permit")
            .build();
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
    ReachFilterQuestion question =
        ReachFilterQuestion.builder()
            .setFilterSpecifierInput(ACL.getName())
            .setQuery("deny")
            .build();
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
    ReachFilterQuestion question =
        ReachFilterQuestion.builder()
            .setFilterSpecifierInput(ACL.getName())
            .setQuery("matchLine 2")
            .build();
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
        _batfish.reachFilter(_config, toDenyAcl(ACCEPT_ALL_ACL), _allLocationsParams);
    assertThat("Should not find permitted flow", !permitFlow.isPresent());
  }

  @Test
  public void testReachFilter_deny_REJECT_ALL() {
    Optional<Flow> permitFlow =
        _batfish.reachFilter(_config, toDenyAcl(REJECT_ALL_ACL), _allLocationsParams);
    assertThat("Should find permitted flow", permitFlow.isPresent());
  }

  @Test
  public void testReachFilter_permit_ACCEPT_ALL() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, ACCEPT_ALL_ACL, _allLocationsParams);
    assertThat("Should find permitted flow", permitFlow.isPresent());
  }

  @Test
  public void testReachFilter_permit_REJECT_ALL() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, REJECT_ALL_ACL, _allLocationsParams);
    assertThat(permitFlow, equalTo(Optional.empty()));
  }

  @Test
  public void testReachFilter_permit() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, ACL, _allLocationsParams);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(oneOf(IP0, IP3)));
  }

  @Test
  public void testReachFilter_permit_headerSpace() {
    ReachFilterParameters.Builder paramsBuilder =
        _allLocationsParams
            .toBuilder()
            .setDestinationIpSpaceSpecifier(new ConstantIpSpaceSpecifier(IP0.toIpSpace()))
            .setSourceIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setHeaderSpace(HeaderSpace.builder().build());

    ReachFilterParameters params = paramsBuilder.build();
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, ACL, params);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP0));

    params = paramsBuilder.setHeaderSpace(HeaderSpace.builder().setNegate(true).build()).build();
    permitFlow = _batfish.reachFilter(_config, ACL, params);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP3));
  }

  @Test
  public void testReachFilter_deny() {
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, toDenyAcl(ACL), _allLocationsParams);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(not(oneOf(IP0, IP3))));
  }

  @Test
  public void testReachFilter_matchLine() {
    Optional<Flow> permitFlow =
        _batfish.reachFilter(_config, toMatchLineAcl(0, ACL), _allLocationsParams);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP0));

    permitFlow = _batfish.reachFilter(_config, toMatchLineAcl(1, ACL), _allLocationsParams);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP1));

    permitFlow = _batfish.reachFilter(_config, toMatchLineAcl(2, ACL), _allLocationsParams);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP2));

    permitFlow = _batfish.reachFilter(_config, toMatchLineAcl(3, ACL), _allLocationsParams);
    assertThat("Should find permitted flow", permitFlow.isPresent());
    assertThat(permitFlow.get(), hasDstIp(IP3));
  }

  @Test
  public void testReachFilter_matchLine_blocked() {
    Optional<Flow> permitFlow =
        _batfish.reachFilter(_config, toMatchLineAcl(2, BLOCKED_LINE_ACL), _allLocationsParams);
    assertThat("Should not find permitted flow", !permitFlow.isPresent());
  }

  @Test
  public void testTestFilter() {
    String hostname = _config.getHostname();
    Flow flow = Flow.builder().setIngressNode(hostname).setDstIp(IP2).setTag("tag").build();
    assertThat(
        TestFiltersAnswerer.getRow(ACL, flow, _config),
        allOf(
            hasColumn(COL_ACTION, equalTo("DENY"), Schema.STRING),
            hasColumn(COL_FILTER_NAME, equalTo(ACL.getName()), Schema.STRING)));
  }

  @Test
  public void testAnswer() {
    ReachFilterQuestion question = new ReachFilterQuestion();
    ReachFilterAnswerer answerer = new ReachFilterAnswerer(question, _batfish);
    TableAnswerElement ae = (TableAnswerElement) answerer.answer();
    assertThat(
        ae,
        hasRows(
            containsInAnyOrder(
                ImmutableList.of(
                    allOf(
                        hasColumn(COL_ACTION, equalTo("PERMIT"), Schema.STRING),
                        hasColumn(COL_FILTER_NAME, equalTo(ACL.getName()), Schema.STRING),
                        hasColumn(COL_LINE_NUMBER, oneOf(0, 3), Schema.INTEGER)),
                    allOf(
                        hasColumn(COL_ACTION, equalTo("PERMIT"), Schema.STRING),
                        hasColumn(
                            COL_FILTER_NAME, equalTo(BLOCKED_LINE_ACL.getName()), Schema.STRING),
                        hasColumn(COL_LINE_NUMBER, equalTo(0), Schema.INTEGER)),
                    allOf(
                        hasColumn(COL_ACTION, equalTo("PERMIT"), Schema.STRING),
                        hasColumn(COL_FILTER_NAME, equalTo(SRC_ACL.getName()), Schema.STRING),
                        hasColumn(COL_LINE_NUMBER, oneOf(0, 1, 2), Schema.INTEGER))))));
  }

  @Test
  public void testMatchSrcInterface() {
    Optional<Flow> flow =
        _batfish.reachFilter(_config, toMatchLineAcl(0, SRC_ACL), _allLocationsParams);
    assertThat(flow.get(), allOf(hasIngressInterface(nullValue()), hasDstIp(IP0)));

    flow = _batfish.reachFilter(_config, toMatchLineAcl(1, SRC_ACL), _allLocationsParams);
    assertThat(flow.get(), allOf(hasIngressInterface(IFACE1), hasDstIp(IP1)));

    flow = _batfish.reachFilter(_config, toMatchLineAcl(2, SRC_ACL), _allLocationsParams);
    assertThat(flow.get(), allOf(hasIngressInterface(IFACE2), hasDstIp(IP2)));

    // cannot have two different source interfaces
    flow = _batfish.reachFilter(_config, toMatchLineAcl(3, SRC_ACL), _allLocationsParams);
    assertThat(flow, equalTo(Optional.empty()));

    // cannot have originate from device and have a source interface
    flow = _batfish.reachFilter(_config, toMatchLineAcl(4, SRC_ACL), _allLocationsParams);
    assertThat(flow, equalTo(Optional.empty()));
  }

  @Test
  public void testSane() {
    // an ACL that can only match with an insane interface
    IpAccessList denyAllSourcesAcl =
        IpAccessList.builder()
            .setName("srcAcl")
            .setLines(
                ImmutableList.of(
                    rejecting().setMatchCondition(ORIGINATING_FROM_DEVICE).build(),
                    rejecting().setMatchCondition(matchSrcInterface(IFACE1)).build(),
                    rejecting().setMatchCondition(matchSrcInterface(IFACE2)).build(),
                    ACCEPT_ALL))
            .build();
    Optional<Flow> flow = _batfish.reachFilter(_config, denyAllSourcesAcl, _allLocationsParams);
    assertThat(flow, equalTo(Optional.empty()));
  }

  @Test
  public void testSane2() {
    // An ACL that can only match with ingress interface IFACE2.
    IpAccessList denyAllSourcesAcl =
        IpAccessList.builder()
            .setName("srcAcl")
            .setLines(
                ImmutableList.of(
                    rejecting().setMatchCondition(ORIGINATING_FROM_DEVICE).build(),
                    rejecting().setMatchCondition(matchSrcInterface(IFACE1)).build(),
                    ACCEPT_ALL))
            .build();
    Optional<Flow> flow = _batfish.reachFilter(_config, denyAllSourcesAcl, _allLocationsParams);
    assertThat("Should find a flow", flow.isPresent());
    assertThat(flow.get(), hasIngressInterface(IFACE2));
  }

  @Test
  public void testSourceInterfaceParameter() {
    ReachFilterParameters params =
        _allLocationsParams
            .toBuilder()
            .setStartLocationSpecifier(new NameRegexInterfaceLinkLocationSpecifier(IFACE1))
            .build();

    // can match line 1 because IFACE1 is specified
    Optional<Flow> flow = _batfish.reachFilter(_config, toMatchLineAcl(1, SRC_ACL), params);
    assertThat(flow.get(), allOf(hasIngressInterface(IFACE1), hasDstIp(IP1)));

    // cannot match line 2 because IFACE2 is not specified
    flow = _batfish.reachFilter(_config, toMatchLineAcl(2, SRC_ACL), params);
    assertThat("Should not find a matching flow", !flow.isPresent());
  }

  @Test
  public void testReachFilter_ACCEPT_ALL_dstIpConstraint() {
    Ip constraintIp = new Ip("21.21.21.21");
    ReachFilterParameters params =
        _allLocationsParams
            .toBuilder()
            .setDestinationIpSpaceSpecifier(new ConstantIpSpaceSpecifier(constraintIp.toIpSpace()))
            .setSourceIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setHeaderSpace(new HeaderSpace())
            .build();
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, ACCEPT_ALL_ACL, params);
    assertThat(permitFlow.get(), hasDstIp(constraintIp));
  }

  @Test
  public void testReachFilter_ACCEPT_ALL_srcIpConstraint() {
    Ip constraintIp = new Ip("21.21.21.21");
    ReachFilterParameters params =
        _allLocationsParams
            .toBuilder()
            .setDestinationIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setSourceIpSpaceSpecifier(new ConstantIpSpaceSpecifier(constraintIp.toIpSpace()))
            .setHeaderSpace(new HeaderSpace())
            .build();
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, ACCEPT_ALL_ACL, params);
    assertThat(permitFlow.get(), hasSrcIp(constraintIp));
  }

  @Test
  public void testReachFilter_DENY_ALL_portConstraints() {
    HeaderSpace hs = new HeaderSpace();
    hs.setSrcPorts(Collections.singletonList(new SubRange(1111, 1111)));
    hs.setDstPorts(Collections.singletonList(new SubRange(2222, 2222)));
    ReachFilterParameters params =
        _allLocationsParams
            .toBuilder()
            .setDestinationIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setSourceIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setHeaderSpace(hs)
            .build();
    Optional<Flow> permitFlow = _batfish.reachFilter(_config, ACCEPT_ALL_ACL, params);
    assertThat(permitFlow.get(), allOf(hasSrcPort(1111), hasDstPort(2222)));
  }

  @Test
  public void testReachFilterNodeSpecifierDefault() {
    ReachFilterQuestion q = new ReachFilterQuestion();
    Set<String> nodes = q.getNodesSpecifier().resolve(_batfish.specifierContext());
    assertThat(nodes, contains(_config.getHostname()));

    q =
        ReachFilterQuestion.builder()
            .setFilterSpecifierInput(ACL.getName())
            .setQuery("permit")
            .setNodeSpecifierInput("UNMATCHABLE")
            .build();
    nodes = q.getNodesSpecifier().resolve(_batfish.specifierContext());
    assertThat(nodes, emptyIterable());
  }
}

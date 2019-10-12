package org.batfish.question.searchfilters;

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
import static org.batfish.question.searchfilters.SearchFiltersAnswerer.MATCH_LINE_RENAMER;
import static org.batfish.question.searchfilters.SearchFiltersAnswerer.NEGATED_RENAMER;
import static org.batfish.question.searchfilters.SearchFiltersAnswerer.reachFilter;
import static org.batfish.question.searchfilters.SearchFiltersAnswerer.toDenyAcl;
import static org.batfish.question.searchfilters.SearchFiltersAnswerer.toMatchLineAcl;
import static org.batfish.question.testfilters.TestFiltersAnswerer.COL_ACTION;
import static org.batfish.question.testfilters.TestFiltersAnswerer.COL_FILTER_NAME;
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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Triple;
import org.batfish.common.plugin.IBatfish;
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
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.question.SearchFiltersParameters;
import org.batfish.question.testfilters.TestFiltersAnswerer;
import org.batfish.specifier.ConstantIpSpaceSpecifier;
import org.batfish.specifier.LocationSpecifier;
import org.batfish.specifier.NameRegexInterfaceLinkLocationSpecifier;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** End-to-end tests of {@link org.batfish.question.searchfilters}. */
public final class SearchFiltersTest {
  private static final String IFACE1 = "iface1";

  private static final String IFACE2 = "iface2";

  private static final Ip IP0 = Ip.parse("1.1.1.0");

  private static final Ip IP1 = Ip.parse("1.1.1.1");

  private static final Ip IP2 = Ip.parse("1.1.1.2");

  private static final Ip IP3 = Ip.parse("1.1.1.3");

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

  private static IBatfish _batfish;

  private static Configuration _config;

  private static SearchFiltersParameters _allLocationsParams;

  @BeforeClass
  public static void setup() {
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
    ib.setName("inactiveIface").setActive(false).build();

    _batfish = new MockBatfish(_config);
    _allLocationsParams =
        new SearchFiltersQuestion()
            .toSearchFiltersParameters()
            .toBuilder()
            .setStartLocationSpecifier(LocationSpecifier.ALL_LOCATIONS)
            .setGenerateExplanations(false)
            .build();
  }

  @Test
  public void testGetQueryAcls_permit() {
    SearchFiltersQuestion question =
        SearchFiltersQuestion.builder()
            .setFilterSpecifier(ACL.getName())
            .setAction("permit")
            .build();
    SearchFiltersAnswerer answerer = new SearchFiltersAnswerer(question, _batfish);
    List<Triple<String, String, IpAccessList>> queryAcls = answerer.getQueryAcls(question);
    assertThat(queryAcls, hasSize(1));
    String queryConfig = queryAcls.get(0).getLeft();
    String queryAclName = queryAcls.get(0).getMiddle();
    IpAccessList queryAcl = queryAcls.get(0).getRight();
    assertThat(queryConfig, equalTo(_config.getHostname()));
    assertThat(queryAclName, equalTo(ACL.getName()));
    assertThat(queryAcl, is(ACL));
  }

  @Test
  public void testGetQueryAcls_deny() {
    SearchFiltersQuestion question =
        SearchFiltersQuestion.builder().setFilterSpecifier(ACL.getName()).setAction("deny").build();
    SearchFiltersAnswerer answerer = new SearchFiltersAnswerer(question, _batfish);
    List<Triple<String, String, IpAccessList>> queryAcls = answerer.getQueryAcls(question);
    assertThat(queryAcls, hasSize(1));
    String queryConfig = queryAcls.get(0).getLeft();
    String queryAclName = queryAcls.get(0).getMiddle();
    IpAccessList queryAcl = queryAcls.get(0).getRight();
    assertThat(queryConfig, equalTo(_config.getHostname()));
    assertThat(queryAclName, equalTo(ACL.getName()));
    assertThat(queryAcl.getName(), equalTo(NEGATED_RENAMER.apply(ACL.getName())));
    assertThat(queryAcl.getLines(), equalTo(DENY_ACL.getLines()));
  }

  @Test
  public void testGetQueryAcls_matchLine2() {
    SearchFiltersQuestion question =
        SearchFiltersQuestion.builder()
            .setFilterSpecifier(ACL.getName())
            .setAction("matchLine 2")
            .build();
    SearchFiltersAnswerer answerer = new SearchFiltersAnswerer(question, _batfish);
    List<Triple<String, String, IpAccessList>> queryAcls = answerer.getQueryAcls(question);
    assertThat(queryAcls, hasSize(1));
    String queryConfig = queryAcls.get(0).getLeft();
    String queryAclName = queryAcls.get(0).getMiddle();
    IpAccessList queryAcl = queryAcls.get(0).getRight();
    assertThat(queryConfig, equalTo(_config.getHostname()));
    assertThat(queryAclName, equalTo(ACL.getName()));
    assertThat(queryAcl.getName(), equalTo(MATCH_LINE_RENAMER.apply(2, ACL.getName())));
    assertThat(queryAcl.getLines(), equalTo(MATCH_LINE2_ACL.getLines()));
  }

  @Test
  public void testReachFilter_deny_ACCEPT_ALL() {
    Optional<SearchFiltersResult> result =
        reachFilter(_batfish, _config, toDenyAcl(ACCEPT_ALL_ACL), _allLocationsParams);
    assertTrue("Should not find permitted flow", !result.isPresent());
  }

  @Test
  public void testReachFilter_deny_REJECT_ALL() {
    Optional<SearchFiltersResult> result =
        reachFilter(_batfish, _config, toDenyAcl(REJECT_ALL_ACL), _allLocationsParams);
    assertTrue("Should find permitted flow", result.isPresent());
  }

  @Test
  public void testReachFilter_permit_ACCEPT_ALL() {
    Optional<SearchFiltersResult> result =
        reachFilter(_batfish, _config, ACCEPT_ALL_ACL, _allLocationsParams);
    assertTrue("Should find permitted flow", result.isPresent());
  }

  @Test
  public void testReachFilter_permit_REJECT_ALL() {
    Optional<SearchFiltersResult> result =
        reachFilter(_batfish, _config, REJECT_ALL_ACL, _allLocationsParams);
    assertThat(result, equalTo(Optional.empty()));
  }

  @Test
  public void testReachFilter_permit() {
    Optional<SearchFiltersResult> result = reachFilter(_batfish, _config, ACL, _allLocationsParams);
    assertTrue("Should find permitted flow", result.isPresent());
    assertThat(result.get().getExampleFlow(), hasDstIp(oneOf(IP0, IP3)));
  }

  @Test
  public void testReachFilter_permit_headerSpace() {
    SearchFiltersParameters.Builder paramsBuilder =
        _allLocationsParams
            .toBuilder()
            .setDestinationIpSpaceSpecifier(new ConstantIpSpaceSpecifier(IP0.toIpSpace()))
            .setSourceIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setHeaderSpace(HeaderSpace.builder().build());

    SearchFiltersParameters params = paramsBuilder.build();
    Optional<SearchFiltersResult> result = reachFilter(_batfish, _config, ACL, params);
    assertTrue("Should find result", result.isPresent());
    assertThat(result.get().getExampleFlow(), hasDstIp(IP0));

    params = paramsBuilder.setHeaderSpace(HeaderSpace.builder().setNegate(true).build()).build();
    result = reachFilter(_batfish, _config, ACL, params);
    assertTrue("Should find result", result.isPresent());
    assertThat(result.get().getExampleFlow(), hasDstIp(IP3));
  }

  @Test
  public void testReachFilter_deny() {
    Optional<SearchFiltersResult> permitResult =
        reachFilter(_batfish, _config, toDenyAcl(ACL), _allLocationsParams);
    assertTrue("Should find permitted flow", permitResult.isPresent());
    assertThat(permitResult.get().getExampleFlow(), hasDstIp(not(oneOf(IP0, IP3))));
  }

  @Test
  public void testReachFilter_matchLine() {
    Optional<SearchFiltersResult> permitResult =
        reachFilter(_batfish, _config, toMatchLineAcl(0, ACL), _allLocationsParams);
    assertTrue("Should find permitted flow", permitResult.isPresent());
    assertThat(permitResult.get().getExampleFlow(), hasDstIp(IP0));

    permitResult = reachFilter(_batfish, _config, toMatchLineAcl(1, ACL), _allLocationsParams);
    assertTrue("Should find permitted flow", permitResult.isPresent());
    assertThat(permitResult.get().getExampleFlow(), hasDstIp(IP1));

    permitResult = reachFilter(_batfish, _config, toMatchLineAcl(2, ACL), _allLocationsParams);
    assertTrue("Should find permitted flow", permitResult.isPresent());
    assertThat(permitResult.get().getExampleFlow(), hasDstIp(IP2));

    permitResult = reachFilter(_batfish, _config, toMatchLineAcl(3, ACL), _allLocationsParams);
    assertTrue("Should find permitted flow", permitResult.isPresent());
    assertThat(permitResult.get().getExampleFlow(), hasDstIp(IP3));
  }

  @Test
  public void testReachFilter_matchLine_blocked() {
    Optional<SearchFiltersResult> permitResult =
        reachFilter(_batfish, _config, toMatchLineAcl(2, BLOCKED_LINE_ACL), _allLocationsParams);
    assertTrue("Should not find permitted flow", !permitResult.isPresent());
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
    SearchFiltersQuestion question = new SearchFiltersQuestion();
    SearchFiltersAnswerer answerer = new SearchFiltersAnswerer(question, _batfish);
    TableAnswerElement ae = (TableAnswerElement) answerer.answer();
    assertThat(
        ae,
        hasRows(
            containsInAnyOrder(
                ImmutableList.of(
                    allOf(
                        hasColumn(COL_ACTION, equalTo("PERMIT"), Schema.STRING),
                        hasColumn(COL_FILTER_NAME, equalTo(ACL.getName()), Schema.STRING)),
                    allOf(
                        hasColumn(COL_ACTION, equalTo("PERMIT"), Schema.STRING),
                        hasColumn(
                            COL_FILTER_NAME, equalTo(BLOCKED_LINE_ACL.getName()), Schema.STRING)),
                    allOf(
                        hasColumn(COL_ACTION, equalTo("PERMIT"), Schema.STRING),
                        hasColumn(COL_FILTER_NAME, equalTo(SRC_ACL.getName()), Schema.STRING))))));
  }

  @Test
  public void testAnswerWithRenamingAndExplanations() {
    SearchFiltersQuestion question =
        SearchFiltersQuestion.builder().setGenerateExplanations(true).setAction("deny").build();
    SearchFiltersAnswerer answerer = new SearchFiltersAnswerer(question, _batfish);
    TableAnswerElement ae = (TableAnswerElement) answerer.answer();
    assertThat(
        ae,
        hasRows(
            containsInAnyOrder(
                ImmutableList.of(
                    allOf(
                        hasColumn(COL_ACTION, equalTo("DENY"), Schema.STRING),
                        hasColumn(COL_FILTER_NAME, equalTo(ACL.getName()), Schema.STRING)),
                    allOf(
                        hasColumn(COL_ACTION, equalTo("DENY"), Schema.STRING),
                        hasColumn(
                            COL_FILTER_NAME, equalTo(BLOCKED_LINE_ACL.getName()), Schema.STRING)),
                    allOf(
                        hasColumn(COL_ACTION, equalTo("DENY"), Schema.STRING),
                        hasColumn(COL_FILTER_NAME, equalTo(SRC_ACL.getName()), Schema.STRING))))));
  }

  @Test
  public void testMatchSrcInterface() {
    Optional<SearchFiltersResult> result =
        reachFilter(_batfish, _config, toMatchLineAcl(0, SRC_ACL), _allLocationsParams);
    assertThat(
        result.get().getExampleFlow(), allOf(hasIngressInterface(nullValue()), hasDstIp(IP0)));

    result = reachFilter(_batfish, _config, toMatchLineAcl(1, SRC_ACL), _allLocationsParams);
    assertThat(result.get().getExampleFlow(), allOf(hasIngressInterface(IFACE1), hasDstIp(IP1)));

    result = reachFilter(_batfish, _config, toMatchLineAcl(2, SRC_ACL), _allLocationsParams);
    assertThat(result.get().getExampleFlow(), allOf(hasIngressInterface(IFACE2), hasDstIp(IP2)));

    // cannot have two different source interfaces
    result = reachFilter(_batfish, _config, toMatchLineAcl(3, SRC_ACL), _allLocationsParams);
    assertThat(result, equalTo(Optional.empty()));

    // cannot have originate from device and have a source interface
    result = reachFilter(_batfish, _config, toMatchLineAcl(4, SRC_ACL), _allLocationsParams);
    assertThat(result, equalTo(Optional.empty()));
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
    Optional<SearchFiltersResult> flow =
        reachFilter(_batfish, _config, denyAllSourcesAcl, _allLocationsParams);
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
    Optional<SearchFiltersResult> flow =
        reachFilter(_batfish, _config, denyAllSourcesAcl, _allLocationsParams);
    assertTrue("Should find a result", flow.isPresent());
    assertThat(flow.get().getExampleFlow(), hasIngressInterface(IFACE2));
  }

  @Test
  public void testSourceInterfaceParameter() {
    SearchFiltersParameters params =
        _allLocationsParams
            .toBuilder()
            .setStartLocationSpecifier(new NameRegexInterfaceLinkLocationSpecifier(IFACE1))
            .build();

    // can match line 1 because IFACE1 is specified
    Optional<SearchFiltersResult> result =
        reachFilter(_batfish, _config, toMatchLineAcl(1, SRC_ACL), params);
    assertThat(result.get().getExampleFlow(), allOf(hasIngressInterface(IFACE1), hasDstIp(IP1)));

    // cannot match line 2 because IFACE2 is not specified
    result = reachFilter(_batfish, _config, toMatchLineAcl(2, SRC_ACL), params);
    assertTrue("Should not find a result", !result.isPresent());
  }

  @Test
  public void testReachFilter_ACCEPT_ALL_dstIpConstraint() {
    Ip constraintIp = Ip.parse("21.21.21.21");
    SearchFiltersParameters params =
        _allLocationsParams
            .toBuilder()
            .setDestinationIpSpaceSpecifier(new ConstantIpSpaceSpecifier(constraintIp.toIpSpace()))
            .setSourceIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setHeaderSpace(new HeaderSpace())
            .build();
    Optional<SearchFiltersResult> result = reachFilter(_batfish, _config, ACCEPT_ALL_ACL, params);
    assertThat(result.get().getExampleFlow(), hasDstIp(constraintIp));
  }

  @Test
  public void testReachFilter_ACCEPT_ALL_srcIpConstraint() {
    Ip constraintIp = Ip.parse("21.21.21.21");
    SearchFiltersParameters params =
        _allLocationsParams
            .toBuilder()
            .setDestinationIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setSourceIpSpaceSpecifier(new ConstantIpSpaceSpecifier(constraintIp.toIpSpace()))
            .setHeaderSpace(new HeaderSpace())
            .build();
    Optional<SearchFiltersResult> result = reachFilter(_batfish, _config, ACCEPT_ALL_ACL, params);
    assertThat(result.get().getExampleFlow(), hasSrcIp(constraintIp));
  }

  @Test
  public void testReachFilter_DENY_ALL_portConstraints() {
    HeaderSpace hs = new HeaderSpace();
    hs.setSrcPorts(Collections.singletonList(SubRange.singleton(1111)));
    hs.setDstPorts(Collections.singletonList(SubRange.singleton(2222)));
    SearchFiltersParameters params =
        _allLocationsParams
            .toBuilder()
            .setDestinationIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setSourceIpSpaceSpecifier(new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE))
            .setHeaderSpace(hs)
            .build();
    Optional<SearchFiltersResult> result = reachFilter(_batfish, _config, ACCEPT_ALL_ACL, params);
    assertThat(result.get().getExampleFlow(), allOf(hasSrcPort(1111), hasDstPort(2222)));
  }

  @Test
  public void testReachFilterNodeSpecifierDefault() {
    SearchFiltersQuestion q = new SearchFiltersQuestion();
    Set<String> nodes = q.getNodesSpecifier().resolve(_batfish.specifierContext());
    assertThat(nodes, contains(_config.getHostname()));

    q =
        SearchFiltersQuestion.builder()
            .setFilterSpecifier(ACL.getName())
            .setAction("permit")
            .setNodeSpecifier("UNMATCHABLE")
            .build();
    nodes = q.getNodesSpecifier().resolve(_batfish.specifierContext());
    assertThat(nodes, emptyIterable());
  }

  @Test
  public void testGetExplanation() {
    SearchFiltersParameters params =
        _allLocationsParams.toBuilder().setGenerateExplanations(false).build();
    Optional<SearchFiltersResult> result = reachFilter(_batfish, _config, ACCEPT_ALL_ACL, params);
    assertTrue("Should get a result", result.isPresent());
    assertTrue(
        "Should not get an explanation", !result.get().getHeaderSpaceDescription().isPresent());

    params = _allLocationsParams.toBuilder().setGenerateExplanations(true).build();
    result = reachFilter(_batfish, _config, ACCEPT_ALL_ACL, params);
    assertTrue("Should get a result", result.isPresent());
    assertTrue("Should get an explanation", result.get().getHeaderSpaceDescription().isPresent());
    assertThat(result.get().getHeaderSpaceDescription().get(), equalTo(TrueExpr.INSTANCE));
  }
}

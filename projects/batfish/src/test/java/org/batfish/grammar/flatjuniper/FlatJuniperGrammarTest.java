package org.batfish.grammar.flatjuniper;

import static java.util.Collections.emptyMap;
import static org.batfish.datamodel.matchers.AbstractRouteMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.hasConjuncts;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.isAndMatchExprThat;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasEnforceFirstAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbors;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessList;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrfs;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasReferenceBandwidth;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.DataModelMatchers.isDynamic;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAccessVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAdditionalArpIps;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllowedVlans;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfCost;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasZoneName;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPassive;
import static org.batfish.datamodel.matchers.IpAccessListLineMatchers.hasAction;
import static org.batfish.datamodel.matchers.IpAccessListLineMatchers.hasMatchCondition;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.accepts;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.rejects;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.LiteralIntMatcher.hasVal;
import static org.batfish.datamodel.matchers.LiteralIntMatcher.isLiteralIntThat;
import static org.batfish.datamodel.matchers.OrMatchExprMatchers.hasDisjuncts;
import static org.batfish.datamodel.matchers.OrMatchExprMatchers.isOrMatchExprThat;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.hasMetric;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.isAdvertised;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasArea;
import static org.batfish.datamodel.matchers.SetAdministrativeCostMatchers.hasAdmin;
import static org.batfish.datamodel.matchers.SetAdministrativeCostMatchers.isSetAdministrativeCostThat;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasOspfProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.batfish.representation.juniper.JuniperConfiguration.ACL_NAME_EXISTING_CONNECTION;
import static org.batfish.representation.juniper.JuniperConfiguration.ACL_NAME_GLOBAL_POLICY;
import static org.batfish.representation.juniper.JuniperConfiguration.ACL_NAME_SECURITY_POLICY;
import static org.batfish.representation.juniper.JuniperConfiguration.computeOspfExportPolicyName;
import static org.batfish.representation.juniper.JuniperConfiguration.computePeerExportPolicyName;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LocalRoute;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OspfAreaSummary;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.State;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.InitInfoAnswerElement;
import org.batfish.datamodel.matchers.ConfigurationMatchers;
import org.batfish.datamodel.matchers.IpAccessListMatchers;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.datamodel.matchers.RouteFilterListMatchers;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetAdministrativeCost;
import org.batfish.grammar.flatjuniper.FlatJuniperParser.Flat_juniper_configurationContext;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.juniper.JuniperStructureType;
import org.batfish.representation.juniper.JuniperStructureUsage;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link FlatJuniperParser}flat Junpier parser and {@link
 * FlatJuniperControlPlaneExtractor}.
 */
public class FlatJuniperGrammarTest {

  private static class HasClusterId extends FeatureMatcher<BgpNeighbor, Long> {
    public HasClusterId(Matcher<? super Long> subMatcher) {
      super(subMatcher, "clusterId", "clusterId");
    }

    @Override
    protected Long featureValueOf(BgpNeighbor actual) {
      return actual.getClusterId();
    }
  }

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/juniper/testconfigs/";

  private static String TESTRIGS_PREFIX = "org/batfish/grammar/juniper/testrigs/";

  private static HasClusterId hasClusterId(long expectedClusterId) {
    return new HasClusterId(equalTo(expectedClusterId));
  }

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static Flow createFlow(String sourceAddress, String destinationAddress) {
    return createFlow(sourceAddress, destinationAddress, State.NEW);
  }

  private static Flow createFlow(String sourceAddress, String destinationAddress, State state) {
    Flow.Builder fb = new Flow.Builder();
    fb.setIngressNode("node");
    fb.setSrcIp(new Ip(sourceAddress));
    fb.setDstIp(new Ip(destinationAddress));
    fb.setState(state);
    fb.setTag("test");
    return fb.build();
  }

  private static Flow createFlow(IpProtocol protocol, int port) {
    Flow.Builder fb = new Flow.Builder();
    fb.setIngressNode("node");
    fb.setIpProtocol(protocol);
    fb.setDstPort(port);
    fb.setSrcPort(port);
    fb.setTag("test");
    return fb.build();
  }

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private Configuration parseConfig(String hostname) throws IOException {
    return parseTextConfigs(hostname).get(hostname);
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    return getBatfishForConfigurationNames(configurationNames).loadConfigurations();
  }

  @Test
  public void testApplications() throws IOException {
    String hostname = "applications";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm application usage is tracked properly */
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.APPLICATION, "a2", 0));
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.APPLICATION, "a1", 1));
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.APPLICATION, "a3", 1));

    /* Confirm undefined reference is identified */
    assertThat(
        ccae,
        hasUndefinedReference(
            hostname, JuniperStructureType.APPLICATION_OR_APPLICATION_SET, "a_undef"));
  }

  @Test
  public void testApplicationSet() throws IOException {
    String hostname = "application-set";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    Configuration c = parseConfig(hostname);

    /* Check that appset2 contains definition of appset1 concatenated with definition of a3 */
    assertThat(
        c,
        hasIpAccessList(
            ACL_NAME_GLOBAL_POLICY,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
                                .setSrcPorts(ImmutableList.of(new SubRange(1, 1)))
                                .build()),
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setDstPorts(ImmutableList.of(new SubRange(2, 2)))
                                .setIpProtocols(ImmutableList.of(IpProtocol.UDP))
                                .build()),
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setDstPorts(ImmutableList.of(new SubRange(3, 3)))
                                .setIpProtocols(ImmutableList.of(IpProtocol.UDP))
                                .build()))))));

    /* Check that appset1 and appset2 are referenced, but appset3 is not */
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.APPLICATION_SET, "appset1", 1));
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.APPLICATION_SET, "appset2", 1));
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.APPLICATION_SET, "appset3", 0));

    /*
     * Check that there is an undefined reference to appset4, but not to appset1-3
     * (via reference in security policy).
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            hostname,
            JuniperStructureType.APPLICATION_OR_APPLICATION_SET,
            "appset4",
            JuniperStructureUsage.SECURITY_POLICY_MATCH_APPLICATION));
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                hostname, JuniperStructureType.APPLICATION_OR_APPLICATION_SET, "appset1")));
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                hostname, JuniperStructureType.APPLICATION_OR_APPLICATION_SET, "appset2")));
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                hostname, JuniperStructureType.APPLICATION_OR_APPLICATION_SET, "appset3")));

    /*
     * Check that there is an undefined reference to application-set appset4, but not to appset1-3
     * (via reference in application-set definition).
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            hostname,
            JuniperStructureType.APPLICATION_SET,
            "appset4",
            JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION_SET));
    assertThat(
        ccae,
        not(hasUndefinedReference(hostname, JuniperStructureType.APPLICATION_SET, "appset1")));
    assertThat(
        ccae,
        not(hasUndefinedReference(hostname, JuniperStructureType.APPLICATION_SET, "appset2")));
    assertThat(
        ccae,
        not(hasUndefinedReference(hostname, JuniperStructureType.APPLICATION_SET, "appset3")));

    /*
     * Check that there is an undefined reference to application a4 but not a1-3
     * (via reference in application-set definition).
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            hostname,
            JuniperStructureType.APPLICATION_OR_APPLICATION_SET,
            "a4",
            JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION));
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                hostname, JuniperStructureType.APPLICATION_OR_APPLICATION_SET, "a1")));
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                hostname, JuniperStructureType.APPLICATION_OR_APPLICATION_SET, "a2")));
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                hostname, JuniperStructureType.APPLICATION_OR_APPLICATION_SET, "a3")));
  }

  @Test
  public void testApplicationSetNested() throws IOException {
    String hostname = "application-set-nested";
    Configuration c = parseConfig(hostname);

    String aclNameNonNested = "~FROM_ZONE~z1~TO_ZONE~z2";
    String aclNameNested = "~FROM_ZONE~z1~TO_ZONE~z3";
    String aclNameMultiNested = "~FROM_ZONE~z1~TO_ZONE~z4";
    String z1Interface = "ge-0/0/0.0";
    IpAccessList aclNonNested = c.getIpAccessLists().get(aclNameNonNested);
    IpAccessList aclNested = c.getIpAccessLists().get(aclNameNested);
    IpAccessList aclMultiNested = c.getIpAccessLists().get(aclNameMultiNested);
    /* Allowed application permits TCP from port 1 only */
    Flow permittedFlow = createFlow(IpProtocol.TCP, 1);
    Flow rejectedFlow = createFlow(IpProtocol.TCP, 2);

    /*
     * Confirm non-nested application-set acl accepts the allowed protocol-port combo and reject
     * others
     */
    assertThat(
        aclNonNested, accepts(permittedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclNonNested, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm nested application-set acl accepts the allowed protocol-port combo and reject others
     */
    assertThat(
        aclNested, accepts(permittedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclNested, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm multi-nested application-set acl accepts the allowed protocol-port combo and reject
     * others
     */
    assertThat(
        aclMultiNested, accepts(permittedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclMultiNested, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testApplicationWithTerms() throws IOException {
    String hostname = "application-with-terms";
    Configuration c = parseConfig(hostname);

    /*
     * An IpAccessList should be generated for the cross-zone policy from z1 to z2. Its definition
     * should inline the matched application, with the action applied to each generated line
     * from the application. One line should be generated per application term.
     */
    assertThat(
        c,
        hasIpAccessList(
            ACL_NAME_GLOBAL_POLICY,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setDstPorts(ImmutableList.of(new SubRange(1, 1)))
                                .setIpProtocols(ImmutableList.of(IpProtocol.TCP))
                                .setSrcPorts(ImmutableList.of(new SubRange(2, 2)))
                                .build()),
                        IpAccessListLine.acceptingHeaderSpace(
                            HeaderSpace.builder()
                                .setDstPorts(ImmutableList.of(new SubRange(3, 3)))
                                .setIpProtocols(ImmutableList.of(IpProtocol.UDP))
                                .setSrcPorts(ImmutableList.of(new SubRange(4, 4)))
                                .build()))))));
  }

  @Test
  public void testAuthenticationKeyChain() throws IOException {
    String hostname = "authentication-key-chain";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm filter usage is tracked properly */
    assertThat(
        ccae, hasNumReferrers(hostname, JuniperStructureType.AUTHENTICATION_KEY_CHAIN, "KC", 1));
    assertThat(
        ccae,
        hasNumReferrers(hostname, JuniperStructureType.AUTHENTICATION_KEY_CHAIN, "KC_UNUSED", 0));

    /* Confirm undefined reference is identified */
    assertThat(
        ccae,
        hasUndefinedReference(hostname, JuniperStructureType.AUTHENTICATION_KEY_CHAIN, "KC_UNDEF"));
  }

  @Test
  public void testPredefinedJunosApplications() throws IOException {
    Batfish batfish = getBatfishForConfigurationNames("pre-defined-junos-applications");
    InitInfoAnswerElement answer = batfish.initInfo(false, true);
    assertThat(
        answer.prettyPrint(),
        not(Matchers.containsString("unimplemented pre-defined junos application")));
  }

  @Test
  public void testPredefinedJunosApplicationSets() throws IOException {
    Batfish batfish = getBatfishForConfigurationNames("pre-defined-junos-application-sets");
    InitInfoAnswerElement answer = batfish.initInfo(false, true);
    assertThat(
        answer.prettyPrint(),
        not(Matchers.containsString("unimplemented pre-defined junos application-set")));
  }

  /** Tests support for dynamic bgp parsing using "bgp allow" command */
  @Test
  public void testBgpAllow() throws IOException {
    Configuration c = parseConfig("bgp-allow");
    assertThat(
        c, hasDefaultVrf(hasBgpProcess(hasNeighbor(Prefix.parse("10.1.1.0/24"), isDynamic()))));
  }

  @Test
  public void testAutonomousSystem() throws IOException {
    String testrigName = "autonomous-system";
    String c1Name = "as1";
    String c2Name = "as2";
    String c3Name = "as3";
    Prefix neighborPrefix = Prefix.parse("1.0.0.1/32");

    List<String> configurationNames = ImmutableList.of(c1Name, c2Name, c3Name);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Configuration c1 = configurations.get(c1Name);
    Configuration c2 = configurations.get(c2Name);
    Configuration c3 = configurations.get(c3Name);

    assertThat(c1, hasDefaultVrf(hasBgpProcess(hasNeighbor(neighborPrefix, hasLocalAs(1)))));
    assertThat(c2, hasDefaultVrf(hasBgpProcess(hasNeighbor(neighborPrefix, hasLocalAs(1)))));
    assertThat(c3, hasDefaultVrf(hasBgpProcess(hasNeighbor(neighborPrefix, hasLocalAs(1)))));
  }

  @Test
  public void testBgpClusterId() throws IOException {
    String testrigName = "rr";
    String configName = "rr";
    Ip neighbor1Ip = new Ip("2.2.2.2");
    Ip neighbor2Ip = new Ip("4.4.4.4");

    List<String> configurationNames = ImmutableList.of(configName);
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration rr = configurations.get(configName);
    BgpProcess proc = rr.getDefaultVrf().getBgpProcess();
    BgpNeighbor neighbor1 =
        proc.getNeighbors().get(new Prefix(neighbor1Ip, Prefix.MAX_PREFIX_LENGTH));
    BgpNeighbor neighbor2 =
        proc.getNeighbors().get(new Prefix(neighbor2Ip, Prefix.MAX_PREFIX_LENGTH));

    assertThat(neighbor1, hasClusterId(new Ip("3.3.3.3").asLong()));
    assertThat(neighbor2, hasClusterId(new Ip("1.1.1.1").asLong()));
  }

  @Test
  public void testBgpMultipathMultipleAs() throws IOException {
    String testrigName = "multipath-multiple-as";
    List<String> configurationNames =
        ImmutableList.of("multiple_as_disabled", "multiple_as_enabled", "multiple_as_mixed");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    MultipathEquivalentAsPathMatchMode multipleAsDisabled =
        configurations
            .get("multiple_as_disabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode multipleAsEnabled =
        configurations
            .get("multiple_as_enabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode multipleAsMixed =
        configurations
            .get("multiple_as_mixed")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();

    assertThat(multipleAsDisabled, equalTo(MultipathEquivalentAsPathMatchMode.FIRST_AS));
    assertThat(multipleAsEnabled, equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
    assertThat(multipleAsMixed, equalTo(MultipathEquivalentAsPathMatchMode.FIRST_AS));
  }

  @Test
  public void testDefaultApplications() throws IOException {
    String hostname = "default-applications";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    SortedMap<String, SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>>>
        undefinedReferences = ccae.getUndefinedReferences();
    Configuration c = parseConfig(hostname);

    String aclApplicationsName = "~FROM_ZONE~z1~TO_ZONE~z2";
    String aclApplicationSetName = "~FROM_ZONE~z1~TO_ZONE~z3";
    String aclApplicationSetAnyName = "~FROM_ZONE~z1~TO_ZONE~z4";
    String aclApplicationAnyName = "~FROM_ZONE~z1~TO_ZONE~z5";
    String z1Interface = "ge-0/0/0.0";
    IpAccessList aclApplication = c.getIpAccessLists().get(aclApplicationsName);
    IpAccessList aclApplicationSet = c.getIpAccessLists().get(aclApplicationSetName);
    IpAccessList aclApplicationSetAny = c.getIpAccessLists().get(aclApplicationSetAnyName);
    IpAccessList aclApplicationAny = c.getIpAccessLists().get(aclApplicationAnyName);
    /* Allowed applications permits TCP to port 80 and 443 */
    Flow permittedHttpFlow = createFlow(IpProtocol.TCP, 80);
    Flow permittedHttpsFlow = createFlow(IpProtocol.TCP, 443);
    Flow rejectedFlow = createFlow(IpProtocol.TCP, 100);

    /*
     * Confirm there are no undefined references
     */
    assertThat(undefinedReferences.keySet(), emptyIterable());

    /*
     * Confirm acl with explicit application constraints accepts http and https flows and rejects
     * others
     */
    assertThat(
        aclApplication,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplication,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplication, rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm acl with indirect constraints (application-set) accepts http and https flows and
     * rejects others
     */
    assertThat(
        aclApplicationSet,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSet,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSet,
        rejects(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm policy referencing permissive application-set accepts all three flows
     */
    assertThat(
        aclApplicationSetAny,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSetAny,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationSetAny,
        accepts(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));

    /*
     * Confirm policy directly permitting any application accepts all three flows
     */
    assertThat(
        aclApplicationAny,
        accepts(permittedHttpFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationAny,
        accepts(permittedHttpsFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclApplicationAny,
        accepts(rejectedFlow, z1Interface, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testEnforceFirstAs() throws IOException {
    String hostname = "bgp-enforce-first-as";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasNeighbors(hasValue(hasEnforceFirstAs())))));
  }

  @Test
  public void testEthernetSwitchingFilterReference() throws IOException {
    String hostname = "ethernet-switching-filter";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* esfilter should be referred, while esfilter2 should be unreferred */
    assertThat(
        ccae, hasNumReferrers(hostname, JuniperStructureType.FIREWALL_FILTER, "esfilter", 1));
    assertThat(
        ccae, hasNumReferrers(hostname, JuniperStructureType.FIREWALL_FILTER, "esfilter2", 0));
  }

  @Test
  public void testFirewallFilters() throws IOException {
    String hostname = "firewall-filters";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /* Confirm filter usage is tracked properly */
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.FIREWALL_FILTER, "FILTER1", 1));
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.FIREWALL_FILTER, "FILTER2", 2));
    assertThat(
        ccae, hasNumReferrers(hostname, JuniperStructureType.FIREWALL_FILTER, "FILTER_UNUSED", 0));

    /* Confirm undefined reference is identified */
    assertThat(
        ccae,
        hasUndefinedReference(hostname, JuniperStructureType.FIREWALL_FILTER, "FILTER_UNDEF"));
  }

  @Test
  public void testFirewallGlobalAddressBook() throws IOException {
    Configuration c = parseConfig("firewall-global-address-book");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String specificSpaceName = "global~ADDR1";
    String wildcardSpaceName = "global~ADDR2";
    String indirectSpaceName = "global~ADDRSET";

    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-set
    String specificAddr = "2.2.2.2";
    // Address allowed by the wildcard-address in the address-set
    String wildcardAddr = "1.3.3.4";
    // Address not allowed by either entry in the address-set
    String notWildcardAddr = "1.2.3.5";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromWildcardAddr = createFlow(wildcardAddr, untrustIpAddr);
    Flow flowFromNotWildcardAddr = createFlow(notWildcardAddr, untrustIpAddr);
    IpAccessList untrustCombinedAcl =
        c.getInterfaces().get(interfaceNameUntrust).getOutgoingFilter();

    // Should have three global IpSpaces in the config
    assertThat(
        c.getIpSpaces().keySet(),
        containsInAnyOrder(specificSpaceName, wildcardSpaceName, indirectSpaceName));

    IpSpace specificSpace = c.getIpSpaces().get(specificSpaceName);
    IpSpace wildcardSpace = c.getIpSpaces().get(wildcardSpaceName);
    IpSpace indirectSpace = c.getIpSpaces().get(indirectSpaceName);

    // Specific space should contain the specific addr and not others
    assertThat(specificSpace, containsIp(new Ip(specificAddr)));
    assertThat(specificSpace, not(containsIp(new Ip(wildcardAddr))));

    // Wildcard space should contain the wildcard addr and not others
    assertThat(wildcardSpace, containsIp(new Ip(wildcardAddr)));
    assertThat(wildcardSpace, not(containsIp(new Ip(notWildcardAddr))));

    // Indirect space should contain both specific and wildcard addr, but not others
    assertThat(indirectSpace, containsIp(new Ip(specificAddr), c.getIpSpaces()));
    assertThat(indirectSpace, containsIp(new Ip(wildcardAddr), c.getIpSpaces()));
    assertThat(indirectSpace, not(containsIp(new Ip(notWildcardAddr), c.getIpSpaces())));

    // Specifically allowed source addr should be accepted
    assertThat(
        untrustCombinedAcl,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source addr covered by the wildcard entry should be accepted
    assertThat(
        untrustCombinedAcl,
        accepts(flowFromWildcardAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source addr covered by neither addr-set entry should be rejected
    assertThat(
        untrustCombinedAcl,
        rejects(
            flowFromNotWildcardAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallGlobalPolicy() throws IOException {
    Configuration c = parseConfig("firewall-global-policy");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut = c.getInterfaces().get(interfaceNameTrust).getOutgoingFilter();
    IpAccessList aclUntrustOut = c.getInterfaces().get(interfaceNameUntrust).getOutgoingFilter();

    /*
     * Should have six ACLs:
     *  Explicitly defined in the config file:
     *    One from the global security policy
     *  Generated by logic in toVendorIndependent
     *    Two combined outgoing filters for the two interfaces (combines security policies with
     *        egress ACLs)
     *    One permitting existing connections (default firewall behavior)
     *    Two defining security policies for each interface (combines explicit security policy with
     *        implicit security policies like allow existing connection)
     */
    assertThat(
        c.getIpAccessLists().keySet(),
        containsInAnyOrder(
            ACL_NAME_GLOBAL_POLICY,
            aclTrustOut.getName(),
            aclUntrustOut.getName(),
            ACL_NAME_EXISTING_CONNECTION,
            ACL_NAME_SECURITY_POLICY + interfaceNameTrust,
            ACL_NAME_SECURITY_POLICY + interfaceNameUntrust));

    IpAccessListLine aclGlobalPolicyLine =
        Iterables.getOnlyElement(c.getIpAccessLists().get(ACL_NAME_GLOBAL_POLICY).getLines());

    /* Global policy should permit the specific src address defined in the config */
    assertThat(
        aclGlobalPolicyLine,
        hasMatchCondition(equalTo(new MatchHeaderSpace(HeaderSpace.builder().build()))));
    assertThat(aclGlobalPolicyLine, hasAction(equalTo(LineAction.ACCEPT)));

    List<IpAccessListLine> aclTrustSPLines =
        c.getIpAccessLists().get(ACL_NAME_SECURITY_POLICY + interfaceNameTrust).getLines();
    List<IpAccessListLine> aclUntrustSPLines =
        c.getIpAccessLists().get(ACL_NAME_SECURITY_POLICY + interfaceNameUntrust).getLines();

    /* Security policy ACLs should have two lines, one for specific policies and one for default */
    assertThat(aclTrustSPLines, iterableWithSize(3));
    assertThat(aclUntrustSPLines, iterableWithSize(3));

    /* First line should be specific security policy (existing connection in this case) */
    assertThat(
        aclTrustSPLines.get(0),
        hasMatchCondition(
            isOrMatchExprThat(
                hasDisjuncts(
                    containsInAnyOrder(new PermittedByAcl(ACL_NAME_EXISTING_CONNECTION))))));
    assertThat(
        aclUntrustSPLines.get(0),
        hasMatchCondition(
            isOrMatchExprThat(
                hasDisjuncts(
                    containsInAnyOrder(new PermittedByAcl(ACL_NAME_EXISTING_CONNECTION))))));

    /* Second line should be global policy */
    assertThat(
        aclTrustSPLines.get(1),
        hasMatchCondition(equalTo(new PermittedByAcl(ACL_NAME_GLOBAL_POLICY))));
    assertThat(
        aclUntrustSPLines.get(1),
        hasMatchCondition(equalTo(new PermittedByAcl(ACL_NAME_GLOBAL_POLICY))));

    /* Third line should be default policy (reject all traffic) */
    assertThat(aclTrustSPLines.get(2), hasMatchCondition(equalTo(TrueExpr.INSTANCE)));
    assertThat(aclTrustSPLines.get(2), hasAction(equalTo(LineAction.REJECT)));
    assertThat(aclUntrustSPLines.get(2), hasMatchCondition(equalTo(TrueExpr.INSTANCE)));
    assertThat(aclUntrustSPLines.get(2), hasAction(equalTo(LineAction.REJECT)));

    /* Flows in either direction should be permitted by the global policy */
    assertThat(
        aclUntrustOut,
        accepts(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        accepts(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallGlobalPolicyGlobalAddressBook() throws IOException {
    /*
     * Test address book behavior when used in a global policy
     * i.e. a policy that does not have fromZone or toZone
     */
    Configuration c = parseConfig("firewall-global-policy-global-address-book");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";
    String trustedSpaceName = "global~ADDR1";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut = c.getInterfaces().get(interfaceNameTrust).getOutgoingFilter();
    IpAccessList aclUntrustOut = c.getInterfaces().get(interfaceNameUntrust).getOutgoingFilter();

    /* Make sure the global-address-book address is the only config ipSpace */
    assertThat(c.getIpSpaces().keySet(), containsInAnyOrder(trustedSpaceName));

    IpSpace ipSpace = Iterables.getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(new Ip(trustedIpAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(new Ip(untrustedIpAddr))));

    /* Flow from ADDR1 to untrust should be permitted */
    assertThat(
        aclUntrustOut,
        accepts(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    /* Flow from not ADDR1 to trust should be rejected */
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallNoPolicies() throws IOException {
    Configuration c = parseConfig("firewall-no-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut = c.getInterfaces().get(interfaceNameTrust).getOutgoingFilter();
    IpAccessList aclUntrustOut = c.getInterfaces().get(interfaceNameUntrust).getOutgoingFilter();

    /*
     * Should have five ACLs generated by logic in toVendorIndependent:
     *    Two combined outgoing filters for the two interfaces (combines security policies with
     *        egress ACLs)
     *    One permitting existing connections (default firewall behavior)
     *    Two defining security policies for each interface (combines explicit security policy with
     *        implicit security policies like allow existing connection)
     */
    assertThat(
        c.getIpAccessLists().keySet(),
        containsInAnyOrder(
            aclTrustOut.getName(),
            aclUntrustOut.getName(),
            ACL_NAME_EXISTING_CONNECTION,
            ACL_NAME_SECURITY_POLICY + interfaceNameTrust,
            ACL_NAME_SECURITY_POLICY + interfaceNameUntrust));

    List<IpAccessListLine> aclTrustSPLines =
        c.getIpAccessLists().get(ACL_NAME_SECURITY_POLICY + interfaceNameTrust).getLines();
    List<IpAccessListLine> aclUntrustSPLines =
        c.getIpAccessLists().get(ACL_NAME_SECURITY_POLICY + interfaceNameUntrust).getLines();

    /*
     * The interface security policies should have two lines each: one for the actual
     * security policy (allow established connections here), and one for the default action
     */
    assertThat(aclTrustSPLines, iterableWithSize(2));
    assertThat(aclUntrustSPLines, iterableWithSize(2));

    IpAccessListLine aclTrustOutLine = Iterables.getOnlyElement(aclTrustOut.getLines());
    IpAccessListLine aclUntrustOutLine = Iterables.getOnlyElement(aclUntrustOut.getLines());

    /* Each interface's outgoing ACL line should reference its security policy */
    assertThat(
        aclTrustOutLine,
        hasMatchCondition(
            isAndMatchExprThat(
                hasConjuncts(
                    containsInAnyOrder(
                        new PermittedByAcl(ACL_NAME_SECURITY_POLICY + interfaceNameTrust))))));
    assertThat(
        aclUntrustOutLine,
        hasMatchCondition(
            isAndMatchExprThat(
                hasConjuncts(
                    containsInAnyOrder(
                        new PermittedByAcl(ACL_NAME_SECURITY_POLICY + interfaceNameUntrust))))));

    /*
     * Since no policies are defined in either direction, should default to allowing only
     * established connections
     */
    assertThat(
        aclTrustSPLines.get(0),
        hasMatchCondition(
            isOrMatchExprThat(
                hasDisjuncts(
                    containsInAnyOrder(new PermittedByAcl(ACL_NAME_EXISTING_CONNECTION))))));
    assertThat(
        aclUntrustSPLines.get(0),
        hasMatchCondition(
            isOrMatchExprThat(
                hasDisjuncts(
                    containsInAnyOrder(new PermittedByAcl(ACL_NAME_EXISTING_CONNECTION))))));

    /* Default line should be reject all traffic */
    assertThat(aclTrustSPLines.get(1), hasMatchCondition(equalTo(TrueExpr.INSTANCE)));
    assertThat(aclTrustSPLines.get(1), hasAction(equalTo(LineAction.REJECT)));
    assertThat(aclUntrustSPLines.get(1), hasMatchCondition(equalTo(TrueExpr.INSTANCE)));
    assertThat(aclUntrustSPLines.get(1), hasAction(equalTo(LineAction.REJECT)));

    /* Simple flow in either direction should be blocked */
    assertThat(
        aclUntrustOut,
        rejects(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallPolicies() throws IOException {
    Configuration c = parseConfig("firewall-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String securityPolicyName = "~FROM_ZONE~trust~TO_ZONE~untrust";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);
    Flow trustToUntrustReturnFlow = createFlow(trustedIpAddr, untrustedIpAddr, State.ESTABLISHED);
    Flow untrustToTrustReturnFlow = createFlow(untrustedIpAddr, trustedIpAddr, State.ESTABLISHED);

    IpAccessList aclTrustOut = c.getInterfaces().get(interfaceNameTrust).getOutgoingFilter();
    IpAccessList aclUntrustOut = c.getInterfaces().get(interfaceNameUntrust).getOutgoingFilter();

    /*
     * Should have six ACLs:
     *  Explicitly defined in the config file:
     *    One from the security policy from trust to untrust
     *  Generated by logic in toVendorIndependent
     *    Two combined outgoing filters for the two interfaces (combines security policies with
     *        egress ACLs)
     *    One permitting existing connections (default firewall behavior)
     *    Two defining security policies for each interface (combines explicit security policy with
     *        implicit security policies like allow existing connection)
     */
    assertThat(
        c.getIpAccessLists().keySet(),
        containsInAnyOrder(
            securityPolicyName,
            aclTrustOut.getName(),
            aclUntrustOut.getName(),
            ACL_NAME_EXISTING_CONNECTION,
            ACL_NAME_SECURITY_POLICY + interfaceNameTrust,
            ACL_NAME_SECURITY_POLICY + interfaceNameUntrust));

    List<IpAccessListLine> aclTrustSPLines =
        c.getIpAccessLists().get(ACL_NAME_SECURITY_POLICY + interfaceNameTrust).getLines();
    List<IpAccessListLine> aclUntrustSPLines =
        c.getIpAccessLists().get(ACL_NAME_SECURITY_POLICY + interfaceNameUntrust).getLines();

    /* Security policy ACLs should have two lines, one for specific policies and one for default */
    assertThat(aclTrustSPLines, iterableWithSize(2));
    assertThat(aclUntrustSPLines, iterableWithSize(2));

    /* Extract the lines for content testing */
    IpAccessListLine aclTrustSecurityPolicyLine = aclTrustSPLines.get(0);
    IpAccessListLine aclUntrustSecurityPolicyLine = aclUntrustSPLines.get(0);
    IpAccessListLine aclSecurityPolicyLine =
        Iterables.getOnlyElement(c.getIpAccessLists().get(securityPolicyName).getLines());

    /* Security policy for traffic to trust zone should just allow existing connections */
    assertThat(
        aclTrustSecurityPolicyLine,
        hasMatchCondition(
            isOrMatchExprThat(
                hasDisjuncts(
                    containsInAnyOrder(new PermittedByAcl(ACL_NAME_EXISTING_CONNECTION))))));

    /*
     * Security policy for traffic to untrust zone should allow existing connections OR matching
     * the security policy
     */
    assertThat(
        aclUntrustSecurityPolicyLine,
        hasMatchCondition(
            isOrMatchExprThat(
                hasDisjuncts(
                    containsInAnyOrder(
                        new PermittedByAcl(ACL_NAME_EXISTING_CONNECTION),
                        new PermittedByAcl(securityPolicyName))))));

    /* Confirm the security policy acl contains logical AND of srcInterface and headerSpace match */
    assertThat(
        aclSecurityPolicyLine,
        hasMatchCondition(
            isAndMatchExprThat(
                hasConjuncts(
                    containsInAnyOrder(
                        new MatchSrcInterface(ImmutableList.of(interfaceNameTrust)),
                        new MatchHeaderSpace(HeaderSpace.builder().build()))))));

    /* Simple flow from trust to untrust should be permitted */
    assertThat(
        aclUntrustOut,
        accepts(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));

    /* Simple flow from untrust to trust should be blocked */
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));

    /* Return flow in either direction should be permitted */
    assertThat(
        aclUntrustOut,
        accepts(
            trustToUntrustReturnFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        accepts(
            untrustToTrustReturnFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZoneAddressBook() throws IOException {
    Configuration c = parseConfig("firewall-zone-address-book");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    // Address on untrust interface's subnet
    String untrustIpAddr = "1.2.4.5";
    // Specific address allowed by the address-book
    String specificAddr = "2.2.2.2";
    // Address not allowed by the address-book
    String notAllowedAddr = "3.3.3.3";

    Flow flowFromSpecificAddr = createFlow(specificAddr, untrustIpAddr);
    Flow flowFromNotAllowedAddr = createFlow(notAllowedAddr, untrustIpAddr);

    IpAccessList aclUntrustOut = c.getInterfaces().get(interfaceNameUntrust).getOutgoingFilter();

    // Should have a an IpSpace in the config corresponding to the trust zone's ADDR1 address
    assertThat(c.getIpSpaces(), hasKey(equalTo("trust~ADDR1")));

    // It should be the only IpSpace
    assertThat(c.getIpSpaces().keySet(), iterableWithSize(1));
    IpSpace ipSpace = Iterables.getOnlyElement(c.getIpSpaces().values());

    // It should contain the specific address
    assertThat(ipSpace, containsIp(new Ip(specificAddr)));

    // It should not contain the address that is not allowed
    assertThat(ipSpace, not(containsIp(new Ip(notAllowedAddr))));

    // Specifically allowed source address should be accepted
    assertThat(
        aclUntrustOut,
        accepts(flowFromSpecificAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    // Source address not covered by the address-book should be rejected
    assertThat(
        aclUntrustOut,
        rejects(flowFromNotAllowedAddr, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testFirewallZones() throws IOException {
    Configuration c = parseConfig("firewall-no-policies");
    String interfaceNameTrust = "ge-0/0/0.0";
    String interfaceNameUntrust = "ge-0/0/1.0";
    String zoneTrust = "trust";
    String zoneUntrust = "untrust";
    String trustedIpAddr = "1.2.3.5";
    String untrustedIpAddr = "1.2.4.5";

    Flow trustToUntrustFlow = createFlow(trustedIpAddr, untrustedIpAddr);
    Flow untrustToTrustFlow = createFlow(untrustedIpAddr, trustedIpAddr);

    IpAccessList aclTrustOut = c.getInterfaces().get(interfaceNameTrust).getOutgoingFilter();
    IpAccessList aclUntrustOut = c.getInterfaces().get(interfaceNameUntrust).getOutgoingFilter();

    // Should have two zones
    assertThat(c.getZones().keySet(), containsInAnyOrder(zoneTrust, zoneUntrust));

    // Should have two interfaces
    assertThat(
        c.getInterfaces().keySet(), containsInAnyOrder(interfaceNameTrust, interfaceNameUntrust));

    // Confirm the interfaces are associated with their zones
    assertThat(c.getInterfaces().get(interfaceNameTrust), hasZoneName(equalTo(zoneTrust)));
    assertThat(c.getInterfaces().get(interfaceNameUntrust), hasZoneName(equalTo(zoneUntrust)));

    /* Simple flows should be blocked */
    assertThat(
        aclUntrustOut,
        rejects(trustToUntrustFlow, interfaceNameTrust, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        aclTrustOut,
        rejects(untrustToTrustFlow, interfaceNameUntrust, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testOspfMetric() throws IOException {
    Configuration config =
        BatfishTestUtils.parseTextConfigs(
                _folder, "org/batfish/grammar/juniper/testconfigs/ospfmetric")
            .get("ospfmetric");
    OspfAreaSummary summary =
        config
            .getDefaultVrf()
            .getOspfProcess()
            .getAreas()
            .get(1L)
            .getSummaries()
            .get(Prefix.parse("10.0.0.0/16"));
    assertThat(summary, not(isAdvertised()));
    assertThat(summary, hasMetric(123L));

    // Defaults
    summary =
        config
            .getDefaultVrf()
            .getOspfProcess()
            .getAreas()
            .get(2L)
            .getSummaries()
            .get(Prefix.parse("10.0.0.0/16"));
    assertThat(summary, isAdvertised());
    assertThat(summary, hasMetric(nullValue()));

    // Interface override
    assertThat(config, hasInterface("fe-1/0/1.0", hasOspfCost(equalTo(17))));
  }

  @Test
  public void testOspfPsk() throws IOException {
    /* allow both encrypted and unencrypted key */
    parseConfig("ospf-psk");
  }

  @Test
  public void testOspfReferenceBandwidth() throws IOException {
    String hostname = "ospf-reference-bandwidth";
    Configuration c = parseConfig(hostname);
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasReferenceBandwidth(equalTo(1E9D)))));
    assertThat(c, hasVrf("vrf1", hasOspfProcess(hasReferenceBandwidth(equalTo(2E9D)))));
    assertThat(c, hasVrf("vrf2", hasOspfProcess(hasReferenceBandwidth(equalTo(3E9D)))));
    assertThat(c, hasVrf("vrf3", hasOspfProcess(hasReferenceBandwidth(equalTo(4E9D)))));
    assertThat(c, hasVrf("vrf4", hasOspfProcess(hasReferenceBandwidth(equalTo(5E9D)))));
  }

  @Test
  public void testPsPreferenceBehavior() throws IOException {
    Configuration c = parseConfig("policy-statement-preference");

    RoutingPolicy policyPreference = c.getRoutingPolicies().get("preference");

    StaticRoute staticRoute =
        StaticRoute.builder()
            .setNetwork(Prefix.parse("10.0.1.0/24"))
            .setNextHopInterface("nextint")
            .setNextHopIp(new Ip("10.0.0.1"))
            .build();

    Environment.Builder eb = Environment.builder(c).setDirection(Direction.IN);
    eb.setVrf("vrf1");
    policyPreference.call(
        eb.setOriginalRoute(staticRoute)
            .setOutputRoute(new OspfExternalType2Route.Builder())
            .build());

    // Checking admin cost set on the output route
    assertThat(eb.build().getOutputRoute().getAdmin(), equalTo(123));
  }

  @Test
  public void testPsPreferenceStructure() throws IOException {
    Configuration c = parseConfig("policy-statement-preference");

    Environment.Builder eb = Environment.builder(c).setDirection(Direction.IN);
    eb.setVrf("vrf1");

    RoutingPolicy policyPreference = c.getRoutingPolicies().get("preference");

    assertThat(policyPreference.getStatements(), hasSize(2));

    // Extracting the If statement
    MatcherAssert.assertThat(policyPreference.getStatements().get(0), instanceOf(If.class));

    If i = (If) policyPreference.getStatements().get(0);

    MatcherAssert.assertThat(i.getTrueStatements(), hasSize(1));
    MatcherAssert.assertThat(
        Iterables.getOnlyElement(i.getTrueStatements()), instanceOf(SetAdministrativeCost.class));

    assertThat(
        Iterables.getOnlyElement(i.getTrueStatements()),
        isSetAdministrativeCostThat(hasAdmin(isLiteralIntThat(hasVal(123)))));
  }

  @Test
  public void testTacplusPsk() throws IOException {
    /* allow both encrypted and unencrypted key */
    parseConfig("tacplus-psk");
  }

  @Test
  public void testInterfaceArp() throws IOException {
    Configuration c = parseConfig("interface-arp");

    /* The additional ARP IP set for irb.0 should appear in the data model */
    assertThat(c, hasInterface("irb.0", hasAdditionalArpIps(hasItem(new Ip("1.0.0.2")))));
  }

  @Test
  public void testInterfaceMtu() throws IOException {
    Configuration c = parseConfig("interfaceMtu");

    /* Properly configured interfaces should be present in respective areas. */
    assertThat(c.getInterfaces().keySet(), equalTo(Collections.singleton("xe-0/0/0:0.0")));
    assertThat(c, hasInterface("xe-0/0/0:0.0", hasMtu(9000)));
  }

  @Test
  public void testInterfaceVlan() throws IOException {
    Configuration c = parseConfig("interface-vlan");

    // Expecting an Interface in ACCESS mode with VLAN 101
    assertThat(c, hasInterface("ge-0/0/0.0", hasSwitchPortMode(SwitchportMode.ACCESS)));
    assertThat(c, hasInterface("ge-0/0/0.0", hasAccessVlan(101)));

    // Expecting an Interface in TRUNK mode with VLANs 1-5
    assertThat(c, hasInterface("ge-0/3/0.0", hasSwitchPortMode(SwitchportMode.TRUNK)));
    assertThat(
        c, hasInterface("ge-0/3/0.0", hasAllowedVlans(ImmutableList.of(new SubRange("1-5")))));
  }

  @Test
  public void testInterfaceVlanReferences() throws IOException {
    String hostname = "interface-vlan";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * We expect an undefined reference for VLAN_TEST_UNDEFINED
     */
    assertThat(
        ccae,
        hasUndefinedReference(
            hostname,
            JuniperStructureType.VLAN,
            "VLAN_TEST_UNDEFINED",
            JuniperStructureUsage.INTERFACE_VLAN));

    /*
     * VLAN should not contribute to defined structures
     */
    assertThat(ccae.getDefinedStructures().get(hostname), equalTo(emptyMap()));
  }

  @Test
  public void testIpProtocol() throws IOException {
    String hostname = "firewall-filter-ip-protocol";
    Configuration c = parseConfig(hostname);

    Flow tcpFlow = createFlow(IpProtocol.TCP, 0);
    Flow icmpFlow = createFlow(IpProtocol.ICMP, 0);

    // Tcp flow should be accepted by the filter and others should be rejected
    assertThat(c, hasIpAccessList("FILTER", accepts(tcpFlow, null, c)));
    assertThat(c, hasIpAccessList("FILTER", rejects(icmpFlow, null, c)));
  }

  @Test
  public void testSourceAddress() throws IOException {
    Configuration c = parseConfig("firewall-source-address");
    String filterNameV4 = "FILTER";
    String filterNameV6 = "FILTERv6";

    assertThat(c.getIpAccessLists().keySet(), containsInAnyOrder(filterNameV4, filterNameV6));

    IpAccessList fwSourceAddressAcl = c.getIpAccessLists().get(filterNameV4);
    assertThat(fwSourceAddressAcl.getLines(), hasSize(1));

    // should have the same acl as defined in the config
    assertThat(
        c,
        hasIpAccessList(
            filterNameV4,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        IpAccessListLine.builder()
                            .setAction(LineAction.ACCEPT)
                            .setMatchCondition(
                                new MatchHeaderSpace(
                                    HeaderSpace.builder()
                                        .setSrcIps(
                                            AclIpSpace.union(
                                                new IpWildcard(
                                                        new Ip("1.0.3.0"), new Ip("0.255.0.255"))
                                                    .toIpSpace(),
                                                new IpWildcard("2.3.4.5/24").toIpSpace()))
                                        .build()))
                            .setName("TERM")
                            .build())))));
  }

  @Test
  public void testDestinationAddress() throws IOException {
    Configuration c = parseConfig("firewall-destination-address");
    String filterNameV4 = "FILTER";
    String filterNameV6 = "FILTERv6";

    assertThat(c.getIpAccessLists().keySet(), containsInAnyOrder(filterNameV4, filterNameV6));

    IpAccessList fwDestinationAddressAcl = c.getIpAccessLists().get(filterNameV4);
    assertThat(fwDestinationAddressAcl.getLines(), hasSize(1));

    // should have the same acl as defined in the config
    assertThat(
        c,
        hasIpAccessList(
            filterNameV4,
            IpAccessListMatchers.hasLines(
                equalTo(
                    ImmutableList.of(
                        IpAccessListLine.builder()
                            .setAction(LineAction.ACCEPT)
                            .setMatchCondition(
                                new MatchHeaderSpace(
                                    HeaderSpace.builder()
                                        .setDstIps(
                                            AclIpSpace.union(
                                                new IpWildcard(
                                                        new Ip("1.0.3.0"), new Ip("0.255.0.255"))
                                                    .toIpSpace(),
                                                new IpWildcard("2.3.4.5/24").toIpSpace()))
                                        .build()))
                            .setName("TERM")
                            .build())))));
  }

  @Test
  public void testSourceAddressBehavior() throws IOException {
    Configuration c = parseConfig("firewall-source-address");

    assertThat(c.getIpAccessLists().keySet(), hasSize(2));

    Flow whiteListedSrc = createFlow("1.8.3.9", "2.5.6.7");
    Flow blackListedSrc = createFlow("5.8.4.9", "2.5.6.7");

    IpAccessList incomingFilter = c.getInterfaces().get("fw-s-add.0").getIncomingFilter();

    // Whitelisted source address should be allowed
    assertThat(incomingFilter, accepts(whiteListedSrc, "fw-s-add.0", c));

    // Blacklisted source address should be denied
    assertThat(incomingFilter, rejects(blackListedSrc, "fw-s-add.0", c));
  }

  @Test
  public void testDestinationAddressBehavior() throws IOException {
    Configuration c = parseConfig("firewall-destination-address");

    assertThat(c.getIpAccessLists().keySet(), hasSize(2));

    Flow whiteListedDst = createFlow("2.5.6.7", "1.8.3.9");
    Flow blackListedDst = createFlow("2.5.6.7", "5.8.4.9");

    IpAccessList incomingFilter = c.getInterfaces().get("fw-s-add.0").getIncomingFilter();

    // Whitelisted source address should be allowed
    assertThat(incomingFilter, accepts(whiteListedDst, "fw-s-add.0", c));

    // Blacklisted source address should be denied
    assertThat(incomingFilter, rejects(blackListedDst, "fw-s-add.0", c));
  }

  @Test
  public void testLocalRouteExportBgp() throws IOException {
    Configuration c = parseConfig("local-route-export-bgp");
    Environment.Builder eb = Environment.builder(c).setDirection(Direction.OUT);

    String peer1Vrf = "peer1Vrf";
    RoutingPolicy peer1RejectAllLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("1.0.0.1/32")));

    String peer2Vrf = "peer2Vrf";
    RoutingPolicy peer2RejectPtpLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("2.0.0.1/32")));

    String peer3Vrf = "peer3Vrf";
    RoutingPolicy peer3RejectLanLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("3.0.0.1/32")));

    String peer4Vrf = "peer3Vrf";
    RoutingPolicy peer4AllowAllLocal =
        c.getRoutingPolicies().get(computePeerExportPolicyName(Prefix.parse("4.0.0.1/32")));

    LocalRoute localRoutePtp = new LocalRoute(new InterfaceAddress("10.0.0.0/31"), "ge-0/0/0.0");
    LocalRoute localRouteLan = new LocalRoute(new InterfaceAddress("10.0.1.0/30"), "ge-0/0/1.0");

    // Peer policies should reject local routes not exported by their VRFs
    eb.setVrf(peer1Vrf);
    assertThat(
        peer1RejectAllLocal
            .call(eb.setOriginalRoute(localRoutePtp).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        peer1RejectAllLocal
            .call(eb.setOriginalRoute(localRouteLan).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(false));

    eb.setVrf(peer2Vrf);
    assertThat(
        peer2RejectPtpLocal
            .call(eb.setOriginalRoute(localRoutePtp).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        peer2RejectPtpLocal
            .call(eb.setOriginalRoute(localRouteLan).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(true));

    eb.setVrf(peer3Vrf);
    assertThat(
        peer3RejectLanLocal
            .call(eb.setOriginalRoute(localRoutePtp).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        peer3RejectLanLocal
            .call(eb.setOriginalRoute(localRouteLan).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(false));

    eb.setVrf(peer4Vrf);
    assertThat(
        peer4AllowAllLocal
            .call(eb.setOriginalRoute(localRoutePtp).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        peer4AllowAllLocal
            .call(eb.setOriginalRoute(localRouteLan).setOutputRoute(new BgpRoute.Builder()).build())
            .getBooleanValue(),
        equalTo(true));
  }

  @Test
  public void testLocalRouteExportOspf() throws IOException {
    Configuration c = parseConfig("local-route-export-ospf");
    Environment.Builder eb = Environment.builder(c).setDirection(Direction.OUT);

    String vrf1 = "vrf1";
    RoutingPolicy vrf1RejectAllLocal =
        c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf1));

    String vrf2 = "vrf2";
    RoutingPolicy vrf2RejectPtpLocal =
        c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf2));

    String vrf3 = "vrf3";
    RoutingPolicy vrf3RejectLanLocal =
        c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf3));

    String vrf4 = "vrf4";
    RoutingPolicy vrf4AllowAllLocal = c.getRoutingPolicies().get(computeOspfExportPolicyName(vrf4));

    LocalRoute localRoutePtp = new LocalRoute(new InterfaceAddress("10.0.0.0/31"), "ge-0/0/0.0");
    LocalRoute localRouteLan = new LocalRoute(new InterfaceAddress("10.0.1.0/30"), "ge-0/0/1.0");

    // Peer policies should reject local routes not exported by their VRFs
    eb.setVrf(vrf1);
    assertThat(
        vrf1RejectAllLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(new OspfExternalType2Route.Builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        vrf1RejectAllLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(new OspfExternalType2Route.Builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));

    eb.setVrf(vrf2);
    assertThat(
        vrf2RejectPtpLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(new OspfExternalType2Route.Builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        vrf2RejectPtpLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(new OspfExternalType2Route.Builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));

    eb.setVrf(vrf3);
    assertThat(
        vrf3RejectLanLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(new OspfExternalType2Route.Builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        vrf3RejectLanLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(new OspfExternalType2Route.Builder())
                    .build())
            .getBooleanValue(),
        equalTo(false));

    eb.setVrf(vrf4);
    assertThat(
        vrf4AllowAllLocal
            .call(
                eb.setOriginalRoute(localRoutePtp)
                    .setOutputRoute(new OspfExternalType2Route.Builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        vrf4AllowAllLocal
            .call(
                eb.setOriginalRoute(localRouteLan)
                    .setOutputRoute(new OspfExternalType2Route.Builder())
                    .build())
            .getBooleanValue(),
        equalTo(true));
  }

  @Test
  public void testOspfInterfaceAreaAssignment() throws IOException {
    Configuration c = parseConfig("ospfInterfaceAreaAssignment");

    /* Properly configured interfaces should be present in respective areas. */
    assertThat(c, hasInterface("xe-0/0/0.0", isOspfPassive(equalTo(false))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(0L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.0"))))));

    assertThat(c, hasInterface("xe-0/0/0.1", isOspfPassive()));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(0L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.1"))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(hasArea(1L, OspfAreaMatchers.hasInterfaces(hasItem("xe-0/0/0.1"))))));

    /* The following interfaces should be absent since they have no IP addresses assigned. */
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(0L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.2")))))));

    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(0L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.3")))))));
    assertThat(
        c,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(1L, OspfAreaMatchers.hasInterfaces(not(hasItem("xe-0/0/0.3")))))));
  }

  @Test
  public void testStaticRoutePreference() throws IOException {
    Configuration c = parseConfig("static-route-preference");

    assertThat(
        c,
        ConfigurationMatchers.hasVrf(
            "default",
            hasStaticRoutes(
                equalTo(
                    ImmutableSet.of(
                        StaticRoute.builder()
                            .setNetwork(Prefix.ZERO)
                            .setNextHopIp(new Ip("10.0.0.1"))
                            .setAdministrativeCost(250)
                            .build())))));
  }

  @Test
  public void testParsingRecovery() {
    String recoveryText =
        CommonUtil.readResource("org/batfish/grammar/juniper/testconfigs/recovery");
    Settings settings = new Settings();
    FlatJuniperCombinedParser cp = new FlatJuniperCombinedParser(recoveryText, settings);
    Flat_juniper_configurationContext ctx = cp.parse();
    FlatJuniperRecoveryExtractor extractor = new FlatJuniperRecoveryExtractor();
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(extractor, ctx);

    assertThat(extractor.getNumSets(), equalTo(8));
    assertThat(extractor.getNumErrorNodes(), equalTo(8));
  }

  @Test
  public void testPrefixList() throws IOException {
    String hostname = "prefix-lists";
    Configuration c = parseConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    Flow flowAccepted1 = createFlow("1.2.3.4", "0.0.0.0");
    Flow flowAccepted2 = createFlow("1.2.3.5", "0.0.0.0");
    Flow flowDenied = createFlow("9.8.7.6", "0.0.0.0");

    IpAccessList filterPrefixList = c.getIpAccessLists().get("FILTER_PL");

    // Confirm referrers are tracked properly
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.PREFIX_LIST, "PL", 5));
    assertThat(ccae, hasNumReferrers(hostname, JuniperStructureType.PREFIX_LIST, "PL_UNUSED", 0));

    // Confirm undefined reference is detected
    assertThat(ccae, hasUndefinedReference(hostname, JuniperStructureType.PREFIX_LIST, "PL_UNDEF"));

    // Only flow from accepted source-prefixes should be accepted
    assertThat(filterPrefixList, rejects(flowDenied, null, c));
    assertThat(filterPrefixList, accepts(flowAccepted1, null, c));
    assertThat(filterPrefixList, accepts(flowAccepted2, null, c));
  }

  @Test
  public void testPrefixListEmpty() throws IOException {
    Configuration c = parseConfig("prefix-list-empty");
    Flow testFlow1 = createFlow("9.8.7.6", "0.0.0.0");
    Flow testFlow2 = createFlow("1.2.3.4", "1.2.3.4");
    Flow testFlow3 = createFlow("0.0.0.0", "9.8.7.6");

    IpAccessList incomingFilterSource = c.getIpAccessLists().get("TEST_FILTER_SOURCE");
    IpAccessList incomingFilterSourceExcept = c.getIpAccessLists().get("TEST_FILTER_SOURCE_EXCEPT");

    IpAccessList incomingFilterDestination = c.getIpAccessLists().get("TEST_FILTER_DESTINATION");
    IpAccessList incomingFilterDestinationExcept =
        c.getIpAccessLists().get("TEST_FILTER_DESTINATION_EXCEPT");

    IpAccessList incomingFilter = c.getIpAccessLists().get("TEST_FILTER");

    // No source IP should match the empty prefix list
    assertThat(incomingFilterSource, rejects(testFlow1, null, c));
    assertThat(incomingFilterSource, rejects(testFlow2, null, c));
    assertThat(incomingFilterSource, rejects(testFlow3, null, c));

    // Every source IP should match the empty prefix list
    assertThat(incomingFilterSourceExcept, accepts(testFlow1, null, c));
    assertThat(incomingFilterSourceExcept, accepts(testFlow2, null, c));
    assertThat(incomingFilterSourceExcept, accepts(testFlow3, null, c));

    // No destination IP should match the empty prefix list
    assertThat(incomingFilterDestination, rejects(testFlow1, null, c));
    assertThat(incomingFilterDestination, rejects(testFlow2, null, c));
    assertThat(incomingFilterDestination, rejects(testFlow3, null, c));

    // Every destination IP should match the empty prefix list
    assertThat(incomingFilterDestinationExcept, accepts(testFlow1, null, c));
    assertThat(incomingFilterDestinationExcept, accepts(testFlow2, null, c));
    assertThat(incomingFilterDestinationExcept, accepts(testFlow3, null, c));

    // No dest or source IP should match the empty prefix list
    assertThat(incomingFilter, rejects(testFlow1, null, c));
    assertThat(incomingFilter, rejects(testFlow2, null, c));
    assertThat(incomingFilter, rejects(testFlow3, null, c));
  }

  @Test
  public void testRouteFilters() throws IOException {
    Configuration c = parseConfig("route-filter");
    RouteFilterList rfl = c.getRouteFilterLists().get("route-filter-test:t1");
    assertThat(
        rfl,
        RouteFilterListMatchers.hasLines(
            containsInAnyOrder(
                new RouteFilterLine(
                    LineAction.ACCEPT, Prefix.parse("1.2.0.0/16"), new SubRange(16, 16)),
                new RouteFilterLine(
                    LineAction.ACCEPT, Prefix.parse("1.2.0.0/16"), new SubRange(17, 32)),
                new RouteFilterLine(
                    LineAction.ACCEPT, Prefix.parse("1.7.0.0/16"), new SubRange(16, 16)),
                new RouteFilterLine(
                    LineAction.ACCEPT, Prefix.parse("1.7.0.0/17"), new SubRange(17, 17)),
                new RouteFilterLine(
                    LineAction.ACCEPT,
                    new IpWildcard("1.0.0.0:0.255.0.255"),
                    new SubRange(16, 16)))));
  }

  @Test
  public void testRoutingInstanceType() throws IOException {
    Configuration c = parseConfig("routing-instance-type");

    /* All types for now should result in a VRF */
    /* TODO: perhaps some types e.g. forwarding should not result in a VRF */
    assertThat(c, hasVrfs(hasKey("ri-forwarding")));
    assertThat(c, hasVrfs(hasKey("ri-l2vpn")));
    assertThat(c, hasVrfs(hasKey("ri-virtual-router")));
    assertThat(c, hasVrfs(hasKey("ri-virtual-switch")));
    assertThat(c, hasVrfs(hasKey("ri-vrf")));
  }

  @Test
  public void testRoutingPolicy() throws IOException {
    Configuration c = parseConfig("routing-policy");
    Environment.Builder eb = Environment.builder(c).setDirection(Direction.IN);

    RoutingPolicy policyExact = c.getRoutingPolicies().get("route-filter-exact");
    RoutingPolicy policyLonger = c.getRoutingPolicies().get("route-filter-longer");
    RoutingPolicy policyPrange = c.getRoutingPolicies().get("route-filter-prange");
    RoutingPolicy policyThrough = c.getRoutingPolicies().get("route-filter-through");
    RoutingPolicy policyAddressmask = c.getRoutingPolicies().get("route-filter-addressmask");

    ConnectedRoute connectedRouteExact =
        new ConnectedRoute(Prefix.parse("1.2.3.4/16"), "nhinttest");
    ConnectedRoute connectedRouteLonger =
        new ConnectedRoute(Prefix.parse("1.2.3.4/19"), "nhinttest");
    ConnectedRoute connectedRouteInRange =
        new ConnectedRoute(Prefix.parse("1.2.3.4/17"), "nhinttest");
    ConnectedRoute connectedRouteInvalidPrefix =
        new ConnectedRoute(Prefix.parse("2.3.3.4/17"), "nhinttest");
    ConnectedRoute connectedRouteInvalidLength =
        new ConnectedRoute(Prefix.parse("2.3.3.4/29"), "nhinttest");

    ConnectedRoute connectedRouteMaskInvalidPrefix =
        new ConnectedRoute(Prefix.parse("9.2.9.4/16"), "nhinttest");
    ConnectedRoute connectedRouteMaskValid =
        new ConnectedRoute(Prefix.parse("1.9.3.9/16"), "nhinttest");
    ConnectedRoute connectedRouteMaskInvalidLength =
        new ConnectedRoute(Prefix.parse("1.9.3.9/17"), "nhinttest");

    eb.setVrf("vrf1");

    assertThat(
        policyExact.call(eb.setOriginalRoute(connectedRouteExact).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyExact.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(false));
    assertThat(
        policyExact
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyLonger.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyLonger
            .call(eb.setOriginalRoute(connectedRouteInvalidLength).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        policyLonger
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyPrange.call(eb.setOriginalRoute(connectedRouteInRange).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyPrange.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(false));
    assertThat(
        policyPrange
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyThrough.call(eb.setOriginalRoute(connectedRouteInRange).build()).getBooleanValue(),
        equalTo(true));
    assertThat(
        policyThrough.call(eb.setOriginalRoute(connectedRouteLonger).build()).getBooleanValue(),
        equalTo(false));
    assertThat(
        policyThrough
            .call(eb.setOriginalRoute(connectedRouteInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));

    assertThat(
        policyAddressmask
            .call(eb.setOriginalRoute(connectedRouteMaskValid).build())
            .getBooleanValue(),
        equalTo(true));
    assertThat(
        policyAddressmask
            .call(eb.setOriginalRoute(connectedRouteMaskInvalidPrefix).build())
            .getBooleanValue(),
        equalTo(false));
    assertThat(
        policyAddressmask
            .call(eb.setOriginalRoute(connectedRouteMaskInvalidLength).build())
            .getBooleanValue(),
        equalTo(false));
  }

  @Test
  public void testStaticRoutes() throws IOException {
    Configuration c = parseConfig("static-routes");

    assertThat(c, hasDefaultVrf(hasStaticRoutes(hasItem(hasPrefix(Prefix.parse("1.0.0.0/8"))))));
    assertThat(c, hasVrf("ri2", hasStaticRoutes(hasItem(hasPrefix(Prefix.parse("2.0.0.0/8"))))));
  }

  @Test
  public void testStormControl() throws IOException {
    /* allow storm-control configuration in an interface */
    parseConfig("storm-control");
  }
}

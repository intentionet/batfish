package org.batfish.grammar.cisco;

import static org.batfish.datamodel.matchers.AndMatchExprMatchers.hasConjuncts;
import static org.batfish.datamodel.matchers.AndMatchExprMatchers.isAndMatchExprThat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessList;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessLists;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpSpace;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVendorFamily;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrfs;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasAclName;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasIpProtocols;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasMemberInterfaces;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasName;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasOutgoingFilter;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasOutgoingFilterName;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasSrcOrDstPorts;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasZone;
import static org.batfish.datamodel.matchers.DataModelMatchers.isIpSpaceReferenceThat;
import static org.batfish.datamodel.matchers.DataModelMatchers.isPermittedByAclThat;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasDstIps;
import static org.batfish.datamodel.matchers.HeaderSpaceMatchers.hasSrcIps;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDeclaredNames;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasOspfArea;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVrf;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPassive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isOspfPointToPoint;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isProxyArp;
import static org.batfish.datamodel.matchers.IpAccessListLineMatchers.hasMatchCondition;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.accepts;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.hasLines;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.rejects;
import static org.batfish.datamodel.matchers.IpSpaceMatchers.containsIp;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.hasHeaderSpace;
import static org.batfish.datamodel.matchers.MatchHeaderSpaceMatchers.isMatchHeaderSpaceThat;
import static org.batfish.datamodel.matchers.OrMatchExprMatchers.hasDisjuncts;
import static org.batfish.datamodel.matchers.OrMatchExprMatchers.isOrMatchExprThat;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasSummary;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.hasMetric;
import static org.batfish.datamodel.matchers.OspfAreaSummaryMatchers.isAdvertised;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasArea;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasAreas;
import static org.batfish.datamodel.matchers.VrfMatchers.hasOspfProcess;
import static org.batfish.datamodel.vendor_family.VendorFamilyMatchers.hasCisco;
import static org.batfish.datamodel.vendor_family.cisco.CiscoFamilyMatchers.hasLogging;
import static org.batfish.datamodel.vendor_family.cisco.LoggingMatchers.isOn;
import static org.batfish.representation.cisco.CiscoConfiguration.computeCombinedOutgoingAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeInspectClassMapAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeInspectPolicyMapAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeServiceObjectGroupAclName;
import static org.batfish.representation.cisco.CiscoConfiguration.computeZonePairAclName;
import static org.batfish.representation.cisco.OspfProcess.getReferenceOspfBandwidth;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.Network;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.plugin.DataPlanePlugin;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.batfish.representation.cisco.CiscoStructureType;
import org.batfish.representation.cisco.CiscoStructureUsage;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link CiscoParser} and {@link CiscoControlPlaneExtractor}. */
public class CiscoGrammarTest {

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco/testconfigs/";
  private static final String TESTRIGS_PREFIX = "org/batfish/grammar/cisco/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  @Test
  public void testAaaNewmodel() throws IOException {
    Configuration newModelConfiguration = parseConfig("aaaNewmodel");
    boolean aaaNewmodel = newModelConfiguration.getVendorFamily().getCisco().getAaa().getNewModel();
    assertTrue(aaaNewmodel);

    Configuration noNewModelConfiguration = parseConfig("aaaNoNewmodel");
    aaaNewmodel = noNewModelConfiguration.getVendorFamily().getCisco().getAaa().getNewModel();
    assertFalse(aaaNewmodel);
  }

  @Test
  public void testAGAclReferrers() throws IOException {
    String hostName = "iosAccessGroupAcl";
    String testrigName = "access-group-acl";
    List<String> configurationNames = ImmutableList.of("iosAccessGroupAcl");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // check expected references for {mac,ip}_acl{_unused,}
    assertThat(
        ccae, hasNumReferrers(hostName, CiscoStructureType.MAC_ACCESS_LIST, "mac_acl_unused", 0));
    assertThat(ccae, hasNumReferrers(hostName, CiscoStructureType.MAC_ACCESS_LIST, "mac_acl", 1));
    assertThat(
        ccae,
        hasNumReferrers(hostName, CiscoStructureType.IP_ACCESS_LIST_EXTENDED, "ip_acl_unused", 0));
    assertThat(
        ccae, hasNumReferrers(hostName, CiscoStructureType.IP_ACCESS_LIST_EXTENDED, "ip_acl", 1));
  }

  @Test
  public void testAGAclUndefined() throws IOException {
    String hostName = "iosAccessGroupAcl";
    String testrigName = "access-group-acl";
    List<String> configurationNames = ImmutableList.of("iosAccessGroupAcl");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    SortedMap<String, SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>>>
        undefinedReferences =
            batfish.loadConvertConfigurationAnswerElementOrReparse().getUndefinedReferences();

    // only mac_acl_udef and ip_acl_udef should be undefined references
    assertThat(undefinedReferences, hasKey(hostName));
    SortedMap<String, SortedMap<String, SortedMap<String, SortedSet<Integer>>>> byHost =
        undefinedReferences.get(hostName);
    assertThat(byHost, hasKey(CiscoStructureType.ACCESS_LIST.getDescription()));
    SortedMap<String, SortedMap<String, SortedSet<Integer>>> byType =
        byHost.get(CiscoStructureType.ACCESS_LIST.getDescription());

    assertThat(byType.keySet(), hasSize(2));
    assertThat(byType, hasKey("ip_acl_udef"));
    assertThat(byType, hasKey("mac_acl_udef"));
  }

  @Test
  public void testAristaOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("aristaOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(3e6d));

    Configuration defaults = parseConfig("aristaOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.ARISTA)));
  }

  @Test
  public void testAsaOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("asaOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(3e6d));

    Configuration defaults = parseConfig("asaOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_ASA)));
  }

  @Test
  public void testIosLoggingOnDefault() throws IOException {
    Configuration loggingOnOmitted = parseConfig("iosLoggingOnOmitted");
    assertThat(loggingOnOmitted, hasVendorFamily(hasCisco(hasLogging(isOn()))));
  }

  @Test
  public void testIosAclObjectGroup() throws IOException {
    String hostname = "ios-acl-object-group";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * The produced ACL should permit if source matchers object-group ogn1, destination matches
     * ogn2, and service matches ogs1.
     */
    assertThat(
        c,
        hasIpAccessList(
            "acl1",
            hasLines(
                hasItem(
                    hasMatchCondition(
                        isAndMatchExprThat(
                            hasConjuncts(
                                containsInAnyOrder(
                                    ImmutableList.of(
                                        isMatchHeaderSpaceThat(
                                            hasHeaderSpace(
                                                allOf(
                                                    hasDstIps(
                                                        isIpSpaceReferenceThat(hasName("ogn2"))),
                                                    hasSrcIps(
                                                        isIpSpaceReferenceThat(hasName("ogn1")))))),
                                        isPermittedByAclThat(
                                            hasAclName(
                                                computeServiceObjectGroupAclName("ogs1"))))))))))));

    /*
     * We expect only object-groups ogsunused1, ognunused1 to have zero referrers
     */
    assertThat(ccae, hasNumReferrers(hostname, CiscoStructureType.SERVICE_OBJECT_GROUP, "ogs1", 1));
    assertThat(ccae, hasNumReferrers(hostname, CiscoStructureType.NETWORK_OBJECT_GROUP, "ogn1", 1));
    assertThat(ccae, hasNumReferrers(hostname, CiscoStructureType.NETWORK_OBJECT_GROUP, "ogn2", 1));
    assertThat(
        ccae, hasNumReferrers(hostname, CiscoStructureType.SERVICE_OBJECT_GROUP, "ogsunused1", 0));
    assertThat(
        ccae, hasNumReferrers(hostname, CiscoStructureType.NETWORK_OBJECT_GROUP, "ognunused1", 0));

    /*
     * We expect undefined references only to object-groups ogsfake, ognfake1, ognfake2
     */
    assertThat(
        ccae,
        not(hasUndefinedReference(hostname, CiscoStructureType.SERVICE_OBJECT_GROUP, "ogs1")));
    assertThat(
        ccae,
        not(hasUndefinedReference(hostname, CiscoStructureType.NETWORK_OBJECT_GROUP, "ogn1")));
    assertThat(
        ccae,
        not(hasUndefinedReference(hostname, CiscoStructureType.NETWORK_OBJECT_GROUP, "ogn2")));
    assertThat(
        ccae,
        hasUndefinedReference(
            hostname,
            CiscoStructureType.SERVICE_OBJECT_GROUP,
            "ogsfake",
            CiscoStructureUsage.EXTENDED_ACCESS_LIST_SERVICE_OBJECT_GROUP));
    assertThat(
        ccae,
        hasUndefinedReference(
            hostname,
            CiscoStructureType.NETWORK_OBJECT_GROUP,
            "ognfake2",
            CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP));
    assertThat(
        ccae,
        hasUndefinedReference(
            hostname,
            CiscoStructureType.NETWORK_OBJECT_GROUP,
            "ognfake1",
            CiscoStructureUsage.EXTENDED_ACCESS_LIST_NETWORK_OBJECT_GROUP));
  }

  @Test
  public void testIosInspection() throws IOException {
    Configuration c = parseConfig("ios-inspection");

    String zone1Name = "z1";
    String zone2Name = "z2";
    String inspectAclName = "inspectacl";
    String inspectClassMapName = "ci";
    String inspectPolicyMapName = "pmi";
    String eth0Name = "Ethernet0";
    String eth1Name = "Ethernet1";
    String eth2Name = "Ethernet2";
    String eth3Name = "Ethernet3";
    String eth3OriginalAclName = "acl3out";
    String eth3CombinedAclName = computeCombinedOutgoingAclName(eth3Name);
    String zonePairAclName = computeZonePairAclName(zone1Name, zone2Name);
    String zone2OutgoingAclName = CiscoConfiguration.computeZoneOutgoingAclName(zone2Name);

    /* Check for expected generated ACLs */
    assertThat(c, hasIpAccessLists(hasKey(inspectAclName)));
    assertThat(c, hasIpAccessLists(hasKey(computeInspectClassMapAclName(inspectClassMapName))));
    assertThat(c, hasIpAccessLists(hasKey(computeInspectPolicyMapAclName(inspectPolicyMapName))));
    assertThat(c, hasIpAccessLists(hasKey(zonePairAclName)));
    assertThat(c, hasIpAccessLists(hasKey(eth3OriginalAclName)));
    assertThat(c, hasIpAccessLists(hasKey(eth3CombinedAclName)));
    assertThat(c, hasIpAccessLists(hasKey(zone2OutgoingAclName)));

    /* Check that interfaces have correct ACLs assigned */
    assertThat(c, hasInterface(eth2Name, hasOutgoingFilterName(zone2OutgoingAclName)));
    assertThat(c, hasInterface(eth3Name, hasOutgoingFilterName(eth3CombinedAclName)));

    IpAccessList eth2Acl = c.getInterfaces().get(eth2Name).getOutgoingFilter();
    IpAccessList eth3Acl = c.getInterfaces().get(eth3Name).getOutgoingFilter();

    /* Check that expected traffic is permitted/denied */
    Flow permittedByBoth =
        Flow.builder()
            .setSrcIp(new Ip("1.1.1.1"))
            .setDstIp(new Ip("2.2.2.2"))
            .setIpProtocol(IpProtocol.TCP)
            .setDstPort(80)
            .setIngressNode("internet")
            .setTag("none")
            .build();
    assertThat(eth2Acl, accepts(permittedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth2Acl, accepts(permittedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, accepts(permittedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, accepts(permittedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));

    Flow permittedThroughEth2Only =
        Flow.builder()
            .setSrcIp(new Ip("1.1.1.2"))
            .setDstIp(new Ip("2.2.2.2"))
            .setIpProtocol(IpProtocol.TCP)
            .setDstPort(80)
            .setIngressNode("internet")
            .setTag("none")
            .build();
    assertThat(
        eth2Acl,
        accepts(permittedThroughEth2Only, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        eth2Acl,
        accepts(permittedThroughEth2Only, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        eth3Acl,
        rejects(permittedThroughEth2Only, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(
        eth3Acl,
        rejects(permittedThroughEth2Only, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));

    Flow deniedByBoth =
        Flow.builder()
            .setSrcIp(new Ip("1.1.1.1"))
            .setDstIp(new Ip("2.2.2.2"))
            .setIpProtocol(IpProtocol.TCP)
            .setDstPort(81)
            .setIngressNode("internet")
            .setTag("none")
            .build();
    assertThat(eth2Acl, rejects(deniedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth2Acl, rejects(deniedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, rejects(deniedByBoth, eth0Name, c.getIpAccessLists(), c.getIpSpaces()));
    assertThat(eth3Acl, rejects(deniedByBoth, eth1Name, c.getIpAccessLists(), c.getIpSpaces()));
  }

  @Test
  public void testIosObjectGroupNetwork() throws IOException {
    Configuration c = parseConfig("ios-object-group-network");
    Ip ogn1TestIp = new Ip("1.128.0.0");
    Ip ogn2TestIp1 = new Ip("2.0.0.0");
    Ip ogn2TestIp2 = new Ip("2.0.0.1");

    /* Each object group should permit an IP iff it is in its space. */
    assertThat(c, hasIpSpace("ogn1", containsIp(ogn1TestIp)));
    assertThat(c, hasIpSpace("ogn1", not(containsIp(ogn2TestIp1))));
    assertThat(c, hasIpSpace("ogn2", not(containsIp(ogn1TestIp))));
    assertThat(c, hasIpSpace("ogn2", containsIp(ogn2TestIp1)));
    assertThat(c, hasIpSpace("ogn2", containsIp(ogn2TestIp2)));
    assertThat(c, hasIpSpace("ogn3", not(containsIp(ogn2TestIp2))));
  }

  @Test
  public void testIosObjectGroupService() throws IOException {
    Configuration c = parseConfig("ios-object-group-service");

    /* og-icmp */
    assertThat(
        c,
        hasIpAccessList(
            computeServiceObjectGroupAclName("og-icmp"),
            hasLines(
                containsInAnyOrder(
                    ImmutableList.of(
                        hasMatchCondition(
                            isOrMatchExprThat(
                                hasDisjuncts(
                                    contains(
                                        isMatchHeaderSpaceThat(
                                            hasHeaderSpace(
                                                hasIpProtocols(
                                                    contains(IpProtocol.ICMP)))))))))))));
    /* og-tcp */
    assertThat(
        c,
        hasIpAccessList(
            computeServiceObjectGroupAclName("og-tcp"),
            hasLines(
                containsInAnyOrder(
                    ImmutableList.of(
                        hasMatchCondition(
                            isOrMatchExprThat(
                                hasDisjuncts(
                                    containsInAnyOrder(
                                        ImmutableList.of(
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(new SubRange(65500)))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.DOMAIN.number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.CMDtcp_OR_SYSLOGudp
                                                                        .number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.TCP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.HTTP
                                                                        .number()))))))))))))))));
    /* og-udp */
    assertThat(
        c,
        hasIpAccessList(
            computeServiceObjectGroupAclName("og-udp"),
            hasLines(
                containsInAnyOrder(
                    ImmutableList.of(
                        hasMatchCondition(
                            isOrMatchExprThat(
                                hasDisjuncts(
                                    containsInAnyOrder(
                                        ImmutableList.of(
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(new SubRange(65501)))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.NTP.number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.SNMPTRAP
                                                                        .number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.CMDtcp_OR_SYSLOGudp
                                                                        .number())))))),
                                            isMatchHeaderSpaceThat(
                                                hasHeaderSpace(
                                                    allOf(
                                                        hasIpProtocols(contains(IpProtocol.UDP)),
                                                        hasSrcOrDstPorts(
                                                            hasItem(
                                                                new SubRange(
                                                                    NamedPort.TFTP
                                                                        .number()))))))))))))))));
  }

  @Test
  public void testIosOspfNetwork() throws IOException {
    Configuration c = parseConfig("ios-interface-ospf-network");

    /*
     * Confirm interfaces with ospf network broadcast, non-broadcast, etc do not show up as
     * point-to-point
     */
    assertThat(c, hasInterface("Ethernet0/0", not(isOspfPointToPoint())));
    assertThat(c, hasInterface("Ethernet0/2", not(isOspfPointToPoint())));
    assertThat(c, hasInterface("Ethernet0/3", not(isOspfPointToPoint())));
    assertThat(c, hasInterface("Ethernet0/4", not(isOspfPointToPoint())));

    /* Confirm the point-to-point interface shows up as such */
    assertThat(c, hasInterface("Ethernet0/1", isOspfPointToPoint()));
  }

  @Test
  public void testIosOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("iosOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(10e6d));

    Configuration defaults = parseConfig("iosOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_IOS)));
  }

  @Test
  public void testIosClassMapInspect() throws IOException {
    String hostname = "ios-class-map-inspect";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * We expected the only unused acl to be aclunused
     */
    assertThat(
        ccae,
        hasNumReferrers(hostname, CiscoStructureType.IP_ACCESS_LIST_EXTENDED, "acldefined", 1));
    assertThat(
        ccae,
        hasNumReferrers(hostname, CiscoStructureType.IP_ACCESS_LIST_EXTENDED, "aclunused", 0));

    /*
     * We expect an undefined reference only to aclundefined
     */
    assertThat(
        ccae,
        not(hasUndefinedReference(hostname, CiscoStructureType.IP_ACCESS_LIST, "acldefined")));
    assertThat(
        ccae, hasUndefinedReference(hostname, CiscoStructureType.IP_ACCESS_LIST, "aclundefined"));
  }

  @Test
  public void testIosPolicyMapInspect() throws IOException {
    String hostname = "ios-policy-map-inspect";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * We expected the only unused class-map to be cmunused
     */
    assertThat(
        ccae, hasNumReferrers(hostname, CiscoStructureType.INSPECT_CLASS_MAP, "cmdefined", 1));
    assertThat(
        ccae, hasNumReferrers(hostname, CiscoStructureType.INSPECT_CLASS_MAP, "cmunused", 0));

    /*
     * We expect the only unused policy-map to be pmiunused
     */
    assertThat(
        ccae, hasNumReferrers(hostname, CiscoStructureType.INSPECT_POLICY_MAP, "pmidefined", 1));
    assertThat(
        ccae, hasNumReferrers(hostname, CiscoStructureType.INSPECT_POLICY_MAP, "pmiunused", 0));

    /*
     * We expect undefined references only to cmundefined and pmmiundefined
     */
    assertThat(
        ccae,
        not(hasUndefinedReference(hostname, CiscoStructureType.INSPECT_CLASS_MAP, "cmdefined")));
    assertThat(
        ccae, hasUndefinedReference(hostname, CiscoStructureType.INSPECT_CLASS_MAP, "cmundefined"));
    assertThat(
        ccae,
        not(hasUndefinedReference(hostname, CiscoStructureType.INSPECT_POLICY_MAP, "pmidefined")));
    assertThat(
        ccae,
        hasUndefinedReference(hostname, CiscoStructureType.INSPECT_POLICY_MAP, "pmiundefined"));
  }

  @Test
  public void testIosProxyArp() throws IOException {
    Configuration proxyArpOmitted = parseConfig("iosProxyArp");
    assertThat(proxyArpOmitted, hasInterfaces(hasEntry(equalTo("Ethernet0/0"), isProxyArp())));
    assertThat(proxyArpOmitted, hasInterfaces(hasEntry(equalTo("Ethernet0/1"), isProxyArp())));
    assertThat(
        proxyArpOmitted,
        hasInterfaces(hasEntry(equalTo("Ethernet0/2"), isProxyArp(equalTo(false)))));
  }

  @Test
  public void testIosZoneSecurity() throws IOException {
    String hostname = "ios-zone-security";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    /*
     * We expected the only unused zone to be zunreferenced
     */
    assertThat(ccae, hasNumReferrers(hostname, CiscoStructureType.SECURITY_ZONE, "z1", 1));
    assertThat(ccae, hasNumReferrers(hostname, CiscoStructureType.SECURITY_ZONE, "z2", 1));
    assertThat(ccae, hasNumReferrers(hostname, CiscoStructureType.SECURITY_ZONE, "zempty", 1));
    assertThat(
        ccae, hasNumReferrers(hostname, CiscoStructureType.SECURITY_ZONE, "zunreferenced", 0));

    /*
     * We expect an undefined reference only to zundefined
     */
    assertThat(ccae, not(hasUndefinedReference(hostname, CiscoStructureType.SECURITY_ZONE, "z1")));
    assertThat(ccae, not(hasUndefinedReference(hostname, CiscoStructureType.SECURITY_ZONE, "z2")));
    assertThat(
        ccae, not(hasUndefinedReference(hostname, CiscoStructureType.SECURITY_ZONE, "zempty")));
    assertThat(
        ccae, hasUndefinedReference(hostname, CiscoStructureType.SECURITY_ZONE, "zundefined"));

    /*
     * We only expect zempty to be empty (have no interfaces)
     */
    assertThat(c, hasZone("z1", hasMemberInterfaces(not(empty()))));
    assertThat(c, hasZone("z2", hasMemberInterfaces(not(empty()))));
    assertThat(c, hasZone("zempty", hasMemberInterfaces(empty())));
  }

  @Test
  public void testOspfSummaryRouteMetric() throws IOException {
    Configuration manual = parseConfig("iosOspfCost");

    assertThat(
        manual,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(
                    1L, hasSummary(Prefix.parse("10.0.0.0/16"), isAdvertised(equalTo(false)))))));
    assertThat(
        manual,
        hasDefaultVrf(
            hasOspfProcess(hasArea(1L, hasSummary(Prefix.parse("10.0.0.0/16"), hasMetric(100L))))));

    Configuration defaults = parseConfig("iosOspfCostDefaults");

    assertThat(
        defaults,
        hasDefaultVrf(
            hasOspfProcess(hasArea(1L, hasSummary(Prefix.parse("10.0.0.0/16"), isAdvertised())))));
    assertThat(
        defaults,
        hasDefaultVrf(
            hasOspfProcess(
                hasArea(1L, hasSummary(Prefix.parse("10.0.0.0/16"), hasMetric(nullValue()))))));
  }

  @Test
  public void testIosXePolicyMapClassDefault() throws IOException {
    Configuration c = parseConfig("ios-xe-policy-map-class-default");

    String dropPolicyMapAclName = computeInspectPolicyMapAclName("pdrop");
    String passPolicyMapAclName = computeInspectPolicyMapAclName("ppass");
    String unspecifiedPolicyMapAclName = computeInspectPolicyMapAclName("punspecified");

    Flow flow = Flow.builder().setTag("").setIngressNode("").build();

    assertThat(c, hasIpAccessList(dropPolicyMapAclName, rejects(flow, null, c)));
    assertThat(c, hasIpAccessList(passPolicyMapAclName, accepts(flow, null, c)));
    assertThat(c, hasIpAccessList(unspecifiedPolicyMapAclName, rejects(flow, null, c)));
  }

  @Test
  public void testIosXePolicyMapInspectClassInspectActions() throws IOException {
    Configuration c = parseConfig("ios-xe-policy-map-inspect-class-inspect-actions");

    String policyMapName = "pm";
    String policyMapAclName = computeInspectPolicyMapAclName(policyMapName);

    String classMapPassName = "cpass";
    String classMapPassAclName = computeInspectClassMapAclName(classMapPassName);
    String classMapInspectName = "cinspect";
    String classMapInspectAclName = computeInspectClassMapAclName(classMapInspectName);
    String classMapDropName = "cdrop";
    String classMapDropAclName = computeInspectClassMapAclName(classMapDropName);

    Flow flowPass =
        Flow.builder().setIngressNode(c.getName()).setTag("").setIpProtocol(IpProtocol.TCP).build();
    Flow flowInspect =
        Flow.builder().setIngressNode(c.getName()).setTag("").setIpProtocol(IpProtocol.UDP).build();
    Flow flowDrop =
        Flow.builder()
            .setIngressNode(c.getName())
            .setTag("")
            .setIpProtocol(IpProtocol.ICMP)
            .build();

    assertThat(c, hasIpAccessList(policyMapAclName, accepts(flowPass, null, c)));
    assertThat(c, hasIpAccessList(policyMapAclName, accepts(flowInspect, null, c)));
    assertThat(c, hasIpAccessList(policyMapAclName, rejects(flowDrop, null, c)));

    assertThat(c, hasIpAccessList(classMapPassAclName, accepts(flowPass, null, c)));
    assertThat(c, hasIpAccessList(classMapPassAclName, rejects(flowInspect, null, c)));
    assertThat(c, hasIpAccessList(classMapPassAclName, rejects(flowDrop, null, c)));

    assertThat(c, hasIpAccessList(classMapInspectAclName, rejects(flowPass, null, c)));
    assertThat(c, hasIpAccessList(classMapInspectAclName, accepts(flowInspect, null, c)));
    assertThat(c, hasIpAccessList(classMapInspectAclName, rejects(flowDrop, null, c)));

    assertThat(c, hasIpAccessList(classMapDropAclName, rejects(flowPass, null, c)));
    assertThat(c, hasIpAccessList(classMapDropAclName, rejects(flowInspect, null, c)));
    assertThat(c, hasIpAccessList(classMapDropAclName, accepts(flowDrop, null, c)));
  }

  @Test
  public void testIosXeZoneDefaultBehavior() throws IOException {
    Configuration c = parseConfig("ios-xe-zone-default-behavior");

    /* Ethernet1 and Ethernet2 are in zone z12 */
    String e1Name = "Ethernet1";
    String e2Name = "Ethernet2";

    /* Ethernet3 is in zone z3 */
    String e3Name = "Ethernet3";

    Flow flow = Flow.builder().setIngressNode(c.getName()).setTag("").build();

    /* Traffic originating from device should not be subject to zone filtering */
    assertThat(c, hasInterface(e1Name, hasOutgoingFilter(accepts(flow, null, c))));
    assertThat(c, hasInterface(e2Name, hasOutgoingFilter(accepts(flow, null, c))));
    assertThat(c, hasInterface(e3Name, hasOutgoingFilter(accepts(flow, null, c))));

    /* Traffic with src and dst interface in same zone should be permitted by default */
    assertThat(c, hasInterface(e1Name, hasOutgoingFilter(accepts(flow, e1Name, c))));
    assertThat(c, hasInterface(e1Name, hasOutgoingFilter(accepts(flow, e2Name, c))));
    assertThat(c, hasInterface(e2Name, hasOutgoingFilter(accepts(flow, e1Name, c))));
    assertThat(c, hasInterface(e2Name, hasOutgoingFilter(accepts(flow, e2Name, c))));
    assertThat(c, hasInterface(e3Name, hasOutgoingFilter(accepts(flow, e3Name, c))));

    /* Traffic crossing zones should be blocked by default */
    assertThat(c, hasInterface(e1Name, hasOutgoingFilter(rejects(flow, e3Name, c))));
    assertThat(c, hasInterface(e2Name, hasOutgoingFilter(rejects(flow, e3Name, c))));
    assertThat(c, hasInterface(e3Name, hasOutgoingFilter(rejects(flow, e1Name, c))));
    assertThat(c, hasInterface(e3Name, hasOutgoingFilter(rejects(flow, e2Name, c))));
  }

  @Test
  public void testIosXrOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("iosxrOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(10e6d));

    Configuration defaults = parseConfig("iosxrOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_IOS_XR)));
  }

  @Test
  public void testNxosOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("nxosOspfCost");
    assertThat(manual.getDefaultVrf().getOspfProcess().getReferenceBandwidth(), equalTo(10e9d));

    Configuration defaults = parseConfig("nxosOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcess().getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth(ConfigurationFormat.CISCO_NX)));
  }

  @Test
  public void testBgpLocalAs() throws IOException {
    String testrigName = "bgp-local-as";
    List<String> configurationNames = ImmutableList.of("r1", "r2");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpOwners(configurations, true);
    Network<BgpNeighbor, BgpSession> bgpTopology =
        CommonUtil.initBgpTopology(configurations, ipOwners, false);
    Configuration r1 = configurations.get("r1");
    Configuration r2 = configurations.get("r2");
    assertThat(
        bgpTopology
            .outEdges(
                r1.getDefaultVrf().getBgpProcess().getNeighbors().get(Prefix.parse("1.2.0.2/32")))
            .stream()
            .map(BgpSession::getDst),
        is(notNullValue()));
    assertThat(
        bgpTopology
            .outEdges(
                r2.getDefaultVrf().getBgpProcess().getNeighbors().get(Prefix.parse("1.2.0.1/32")))
            .stream()
            .map(BgpSession::getDst),
        is(notNullValue()));
  }

  @Test
  public void testBgpMultipathRelax() throws IOException {
    String testrigName = "bgp-multipath-relax";
    List<String> configurationNames =
        ImmutableList.of("arista_disabled", "arista_enabled", "nxos_disabled", "nxos_enabled");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpOwners(configurations, true);
    CommonUtil.initBgpTopology(configurations, ipOwners, false);
    MultipathEquivalentAsPathMatchMode aristaDisabled =
        configurations
            .get("arista_disabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode aristaEnabled =
        configurations
            .get("arista_enabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode nxosDisabled =
        configurations
            .get("nxos_disabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();
    MultipathEquivalentAsPathMatchMode nxosEnabled =
        configurations
            .get("nxos_enabled")
            .getDefaultVrf()
            .getBgpProcess()
            .getMultipathEquivalentAsPathMatchMode();

    assertThat(aristaDisabled, equalTo(MultipathEquivalentAsPathMatchMode.EXACT_PATH));
    assertThat(aristaEnabled, equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
    assertThat(nxosDisabled, equalTo(MultipathEquivalentAsPathMatchMode.EXACT_PATH));
    assertThat(nxosEnabled, equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
  }

  @Test
  public void testBgpRemovePrivateAs() throws IOException {
    String testrigName = "bgp-remove-private-as";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();
    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpOwners(configurations, true);
    CommonUtil.initBgpTopology(configurations, ipOwners, false);
    DataPlanePlugin dataPlanePlugin = batfish.getDataPlanePlugin();
    batfish.computeDataPlane(false); // compute and cache the dataPlane

    // Check that 1.1.1.1/32 appears on r3
    SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> routes =
        dataPlanePlugin.getRoutes(batfish.loadDataPlane());
    SortedSet<AbstractRoute> r3Routes = routes.get("r3").get(Configuration.DEFAULT_VRF_NAME);
    Set<Prefix> r3Prefixes =
        r3Routes.stream().map(AbstractRoute::getNetwork).collect(Collectors.toSet());
    Prefix r1Loopback = Prefix.parse("1.1.1.1/32");
    assertTrue(r3Prefixes.contains(r1Loopback));

    // check that private AS is present in path in received 1.1.1.1/32 advert on r2
    batfish.initBgpAdvertisements(configurations);
    Configuration r2 = configurations.get("r2");
    boolean r2HasPrivate =
        r2.getReceivedEbgpAdvertisements()
            .stream()
            .filter(a -> a.getNetwork().equals(r1Loopback))
            .toArray(BgpAdvertisement[]::new)[0]
            .getAsPath()
            .getAsSets()
            .stream()
            .flatMap(Collection::stream)
            .anyMatch(AsPath::isPrivateAs);
    assertTrue(r2HasPrivate);

    // check that private AS is absent from path in received 1.1.1.1/32 advert on r3
    Configuration r3 = configurations.get("r3");
    boolean r3HasPrivate =
        r3.getReceivedEbgpAdvertisements()
            .stream()
            .filter(a -> a.getNetwork().equals(r1Loopback))
            .toArray(BgpAdvertisement[]::new)[0]
            .getAsPath()
            .getAsSets()
            .stream()
            .flatMap(Collection::stream)
            .anyMatch(AsPath::isPrivateAs);
    assertFalse(r3HasPrivate);
  }

  @Test
  public void testCommunityListConversion() throws IOException {
    String testrigName = "community-list-conversion";
    String iosName = "ios";
    String nxosName = "nxos";
    String eosName = "eos";
    List<String> configurationNames = ImmutableList.of(iosName, nxosName, eosName);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosCommunityListConfig = configurations.get(iosName);
    SortedMap<String, CommunityList> iosCommunityLists = iosCommunityListConfig.getCommunityLists();

    Configuration eosCommunityListConfig = configurations.get(eosName);
    SortedMap<String, CommunityList> eosCommunityLists = eosCommunityListConfig.getCommunityLists();

    Configuration nxosCommunityListConfig = configurations.get(nxosName);
    SortedMap<String, CommunityList> nxosCommunityLists =
        nxosCommunityListConfig.getCommunityLists();

    String iosRegexImpliedStd = getCLRegex(iosCommunityLists, "40");
    String iosRegexImpliedExp = getCLRegex(iosCommunityLists, "400");
    String iosRegexStd = getCLRegex(iosCommunityLists, "std_community");
    String iosRegexExp = getCLRegex(iosCommunityLists, "exp_community");
    String iosRegexStdAsnn = getCLRegex(iosCommunityLists, "std_as_nn");
    String iosRegexExpAsnn = getCLRegex(iosCommunityLists, "exp_as_nn");
    String iosRegexStdGshut = getCLRegex(iosCommunityLists, "std_gshut");
    String iosRegexExpGshut = getCLRegex(iosCommunityLists, "exp_gshut");
    String iosRegexStdInternet = getCLRegex(iosCommunityLists, "std_internet");
    String iosRegexExpInternet = getCLRegex(iosCommunityLists, "exp_internet");
    String iosRegexStdLocalAs = getCLRegex(iosCommunityLists, "std_local_AS");
    String iosRegexExpLocalAs = getCLRegex(iosCommunityLists, "exp_local_AS");
    String iosRegexStdNoAdv = getCLRegex(iosCommunityLists, "std_no_advertise");
    String iosRegexExpNoAdv = getCLRegex(iosCommunityLists, "exp_no_advertise");
    String iosRegexStdNoExport = getCLRegex(iosCommunityLists, "std_no_export");
    String iosRegexExpNoExport = getCLRegex(iosCommunityLists, "exp_no_export");

    String eosRegexStd = getCLRegex(eosCommunityLists, "eos_std");
    String eosRegexExp = getCLRegex(eosCommunityLists, "eos_exp");
    String eosRegexStdGshut = getCLRegex(eosCommunityLists, "eos_std_gshut");
    String eosRegexStdInternet = getCLRegex(eosCommunityLists, "eos_std_internet");
    String eosRegexStdLocalAs = getCLRegex(eosCommunityLists, "eos_std_local_AS");
    String eosRegexStdNoAdv = getCLRegex(eosCommunityLists, "eos_std_no_adv");
    String eosRegexStdNoExport = getCLRegex(eosCommunityLists, "eos_std_no_export");
    String eosRegexStdMulti = getCLRegex(eosCommunityLists, "eos_std_multi");
    String eosRegexExpMulti = getCLRegex(eosCommunityLists, "eos_exp_multi");

    String nxosRegexStd = getCLRegex(nxosCommunityLists, "nxos_std");
    String nxosRegexExp = getCLRegex(nxosCommunityLists, "nxos_exp");
    String nxosRegexStdInternet = getCLRegex(nxosCommunityLists, "nxos_std_internet");
    String nxosRegexStdLocalAs = getCLRegex(nxosCommunityLists, "nxos_std_local_AS");
    String nxosRegexStdNoAdv = getCLRegex(nxosCommunityLists, "nxos_std_no_adv");
    String nxosRegexStdNoExport = getCLRegex(nxosCommunityLists, "nxos_std_no_export");
    String nxosRegexStdMulti = getCLRegex(nxosCommunityLists, "nxos_std_multi");
    String nxosRegexExpMulti = getCLRegex(nxosCommunityLists, "nxos_exp_multi");

    // Check well known community regexes are generated properly
    String regexInternet =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.INTERNET.getValue()) + "$";
    String regexNoAdv =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.NO_ADVERTISE.getValue()) + "$";
    String regexNoExport =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.NO_EXPORT.getValue()) + "$";
    String regexGshut = "^" + CommonUtil.longToCommunity(WellKnownCommunity.GSHUT.getValue()) + "$";
    String regexLocalAs =
        "^" + CommonUtil.longToCommunity(WellKnownCommunity.LOCAL_AS.getValue()) + "$";
    assertThat(iosRegexStdInternet, equalTo(regexInternet));
    assertThat(iosRegexStdNoAdv, equalTo(regexNoAdv));
    assertThat(iosRegexStdNoExport, equalTo(regexNoExport));
    assertThat(iosRegexStdGshut, equalTo(regexGshut));
    assertThat(iosRegexStdLocalAs, equalTo(regexLocalAs));
    assertThat(eosRegexStdInternet, equalTo(regexInternet));
    assertThat(eosRegexStdNoAdv, equalTo(regexNoAdv));
    assertThat(eosRegexStdNoExport, equalTo(regexNoExport));
    assertThat(eosRegexStdGshut, equalTo(regexGshut));
    assertThat(eosRegexStdLocalAs, equalTo(regexLocalAs));
    // NX-OS does not support gshut
    assertThat(nxosRegexStdInternet, equalTo(regexInternet));
    assertThat(nxosRegexStdNoAdv, equalTo(regexNoAdv));
    assertThat(nxosRegexStdNoExport, equalTo(regexNoExport));
    assertThat(nxosRegexStdLocalAs, equalTo(regexLocalAs));

    // Confirm for the same literal communities, standard and expanded regexs are different
    assertThat(iosRegexImpliedStd, not(equalTo(iosRegexImpliedExp)));
    assertThat(iosRegexStd, not(equalTo(iosRegexExp)));
    assertThat(iosRegexStdAsnn, not(equalTo(iosRegexExpAsnn)));
    assertThat(iosRegexStdInternet, not(equalTo(iosRegexExpInternet)));
    assertThat(iosRegexStdNoAdv, not(equalTo(iosRegexExpNoAdv)));
    assertThat(iosRegexStdNoExport, not(equalTo(iosRegexExpNoExport)));
    assertThat(iosRegexStdGshut, not(equalTo(iosRegexExpGshut)));
    assertThat(iosRegexStdLocalAs, not(equalTo(iosRegexExpLocalAs)));
    assertThat(eosRegexStd, not(equalTo(eosRegexExp)));
    assertThat(eosRegexStdMulti, not(equalTo(eosRegexExpMulti)));
    assertThat(nxosRegexStd, not(equalTo(nxosRegexExp)));
    assertThat(nxosRegexStdMulti, not(equalTo(nxosRegexExpMulti)));
  }

  private static String getCLRegex(
      SortedMap<String, CommunityList> communityLists, String communityName) {
    return communityLists.get(communityName).getLines().get(0).getRegex();
  }

  @Test
  public void testInterfaceNames() throws IOException {
    String testrigName = "interface-names";
    String iosHostname = "ios";
    String i1Name = "Ethernet0/0";

    List<String> configurationNames = ImmutableList.of(iosHostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Interface i1 = configurations.get(iosHostname).getInterfaces().get(i1Name);
    assertThat(i1, hasDeclaredNames("Ethernet0/0", "e0/0", "Eth0/0", "ether0/0-1"));
  }

  @Test
  public void testIpsecVpnIos() throws IOException {
    String testrigName = "ipsec-vpn-ios";
    List<String> configurationNames = ImmutableList.of("r1", "r2", "r3");

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    assertThat(
        configurations.values().stream().mapToLong(c -> c.getIpsecVpns().values().size()).sum(),
        equalTo(6L));
    configurations
        .values()
        .stream()
        .flatMap(c -> c.getIpsecVpns().values().stream())
        .forEach(iv -> assertThat(iv.getRemoteIpsecVpn(), not(nullValue())));
    /* Two tunnels should not be established because of a password mismatch between r1 and r3 */
    assertThat(
        configurations
            .values()
            .stream()
            .flatMap(c -> c.getInterfaces().values().stream())
            .filter(i -> i.getInterfaceType().equals(InterfaceType.TUNNEL) && i.getActive())
            .count(),
        equalTo(4L));
  }

  @Test
  public void testNxosOspfAreaParameters() throws IOException {
    String testrigName = "nxos-ospf";
    String hostname = "nxos-ospf-area";
    String ifaceName = "Ethernet1";
    long areaNum = 1L;
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    /* Ensure bidirectional references between OSPF area and interface */
    assertThat(configurations, hasKey(hostname));
    Configuration c = configurations.get(hostname);
    assertThat(c, hasDefaultVrf(hasOspfProcess(hasAreas(hasKey(areaNum)))));
    OspfArea area = c.getDefaultVrf().getOspfProcess().getAreas().get(areaNum);
    assertThat(area, OspfAreaMatchers.hasInterfaces(hasItem(ifaceName)));
    assertThat(c, hasInterface(ifaceName, hasOspfArea(sameInstance(area))));
    assertThat(c, hasInterface(ifaceName, isOspfPassive(equalTo(false))));
    assertThat(c, hasInterface(ifaceName, isOspfPointToPoint()));
  }

  @Test
  public void testNxosOspfNonDefaultVrf() throws IOException {
    String testrigName = "nxos-ospf";
    String hostname = "nxos-ospf-iface-in-vrf";
    String ifaceName = "Ethernet1";
    String vrfName = "OTHER-VRF";
    long areaNum = 1L;
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    /* Ensure bidirectional references between OSPF area and interface */
    assertThat(configurations, hasKey(hostname));
    Configuration c = configurations.get(hostname);
    assertThat(c, hasVrfs(hasKey(vrfName)));
    Vrf vrf = c.getVrfs().get(vrfName);
    assertThat(vrf, hasOspfProcess(hasAreas(hasKey(areaNum))));
    OspfArea area = vrf.getOspfProcess().getAreas().get(areaNum);
    assertThat(area, OspfAreaMatchers.hasInterfaces(hasItem(ifaceName)));
    assertThat(c, hasInterface(ifaceName, hasVrf(sameInstance(vrf))));
    assertThat(c, hasInterface(ifaceName, hasOspfArea(sameInstance(area))));
    assertThat(c, hasInterface(ifaceName, isOspfPassive(equalTo(false))));
    assertThat(c, hasInterface(ifaceName, isOspfPointToPoint()));
  }

  @Test
  public void testOspfMaxMetric() throws IOException {
    String testrigName = "ospf-max-metric";
    String iosMaxMetricName = "ios-max-metric";
    String iosMaxMetricCustomName = "ios-max-metric-custom";
    String iosMaxMetricOnStartupName = "ios-max-metric-on-startup";
    List<String> configurationNames =
        ImmutableList.of(iosMaxMetricName, iosMaxMetricCustomName, iosMaxMetricOnStartupName);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosMaxMetric = configurations.get(iosMaxMetricName);
    Configuration iosMaxMetricCustom = configurations.get(iosMaxMetricCustomName);
    Configuration iosMaxMetricOnStartup = configurations.get(iosMaxMetricOnStartupName);
    OspfProcess proc = iosMaxMetric.getDefaultVrf().getOspfProcess();
    OspfProcess procCustom = iosMaxMetricCustom.getDefaultVrf().getOspfProcess();
    OspfProcess procOnStartup = iosMaxMetricOnStartup.getDefaultVrf().getOspfProcess();
    long expectedMaxMetricRouterLsa =
        org.batfish.representation.cisco.OspfProcess.MAX_METRIC_ROUTER_LSA;
    long expectedMaxMetricStub = org.batfish.representation.cisco.OspfProcess.MAX_METRIC_ROUTER_LSA;
    long expectedMaxMetricExternal =
        org.batfish.representation.cisco.OspfProcess.DEFAULT_MAX_METRIC_EXTERNAL_LSA;
    long expectedMaxMetricSummary =
        org.batfish.representation.cisco.OspfProcess.DEFAULT_MAX_METRIC_SUMMARY_LSA;
    long expectedCustomMaxMetricExternal = 12345L;
    long expectedCustomMaxMetricSummary = 23456L;

    assertThat(proc.getMaxMetricTransitLinks(), equalTo(expectedMaxMetricRouterLsa));
    assertThat(proc.getMaxMetricStubNetworks(), equalTo(expectedMaxMetricStub));
    assertThat(proc.getMaxMetricExternalNetworks(), equalTo(expectedMaxMetricExternal));
    assertThat(proc.getMaxMetricSummaryNetworks(), equalTo(expectedMaxMetricSummary));
    assertThat(procCustom.getMaxMetricTransitLinks(), equalTo(expectedMaxMetricRouterLsa));
    assertThat(procCustom.getMaxMetricStubNetworks(), equalTo(expectedMaxMetricStub));
    assertThat(procCustom.getMaxMetricExternalNetworks(), equalTo(expectedCustomMaxMetricExternal));
    assertThat(procCustom.getMaxMetricSummaryNetworks(), equalTo(expectedCustomMaxMetricSummary));
    assertThat(procOnStartup.getMaxMetricTransitLinks(), is(nullValue()));
    assertThat(procOnStartup.getMaxMetricStubNetworks(), is(nullValue()));
    assertThat(procOnStartup.getMaxMetricExternalNetworks(), is(nullValue()));
    assertThat(procOnStartup.getMaxMetricSummaryNetworks(), is(nullValue()));
  }

  @Test
  public void testOspfPointToPoint() throws IOException {
    String testrigName = "ospf-point-to-point";
    String iosOspfPointToPoint = "ios-ospf-point-to-point";
    List<String> configurationNames = ImmutableList.of(iosOspfPointToPoint);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosMaxMetric = configurations.get(iosOspfPointToPoint);
    Interface e0Sub0 = iosMaxMetric.getInterfaces().get("Ethernet0/0");
    Interface e0Sub1 = iosMaxMetric.getInterfaces().get("Ethernet0/1");

    assertTrue(e0Sub0.getOspfPointToPoint());
    assertFalse(e0Sub1.getOspfPointToPoint());
  }

  @Test
  public void testParsingRecovery() throws IOException {
    String testrigName = "parsing-recovery";
    String hostname = "ios-recovery";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosRecovery = configurations.get(hostname);
    Map<String, Interface> iosRecoveryInterfaces = iosRecovery.getInterfaces();
    Set<String> iosRecoveryInterfaceNames = iosRecoveryInterfaces.keySet();
    Set<InterfaceAddress> l3Prefixes = iosRecoveryInterfaces.get("Loopback3").getAllAddresses();
    Set<InterfaceAddress> l4Prefixes = iosRecoveryInterfaces.get("Loopback4").getAllAddresses();

    assertThat("Loopback0", isIn(iosRecoveryInterfaceNames));
    assertThat("Loopback1", isIn(iosRecoveryInterfaceNames));
    assertThat("Loopback2", not(isIn(iosRecoveryInterfaceNames)));
    assertThat("Loopback3", isIn(iosRecoveryInterfaceNames));
    assertThat(new InterfaceAddress("10.0.0.1/32"), not(isIn(l3Prefixes)));
    assertThat(new InterfaceAddress("10.0.0.2/32"), isIn(l3Prefixes));
    assertThat("Loopback4", isIn(iosRecoveryInterfaceNames));
    assertThat(new InterfaceAddress("10.0.0.3/32"), not(isIn(l4Prefixes)));
    assertThat(new InterfaceAddress("10.0.0.4/32"), isIn(l4Prefixes));
  }

  @Test
  public void testParsingRecovery1141() throws IOException {
    // Test for https://github.com/batfish/batfish/issues/1141
    String testrigName = "parsing-recovery";
    String hostname = "ios-recovery-1141";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    Configuration iosRecovery = configurations.get(hostname);
    assertThat(iosRecovery, allOf(Matchers.notNullValue(), hasInterface("Loopback0", anything())));
  }

  @Test
  public void testParsingRecoveryNoInfiniteLoopDuringAdaptivePredictionAtEof() throws IOException {
    String testrigName = "parsing-recovery";
    String hostname = "ios-blankish-file";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    /* Hostname is unknown, but a file should be generated nonetheless */
    assertThat(configurations.entrySet(), hasSize(1));
  }

  @Test
  public void testParsingUnrecognizedInterfaceName() throws IOException {
    String testrigName = "parsing-recovery";
    String hostname = "ios-bad-interface-name";
    List<String> configurationNames = ImmutableList.of(hostname);

    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationText(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);
    Map<String, Configuration> configurations = batfish.loadConfigurations();

    /* Parser should not crash, and configuration with hostname from file should be generated */
    assertThat(configurations, hasKey(hostname));
  }

  private Configuration parseConfig(String hostname) throws IOException {
    return parseTextConfigs(hostname).get(hostname);
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.parseTextConfigs(_folder, names);
  }

  @Test
  public void testRfc1583Compatible() throws IOException {
    String[] configurationNames =
        new String[] {"rfc1583Compatible", "rfc1583NoCompatible", "rfc1583Unconfigured"};
    Map<String, Configuration> configurations = parseTextConfigs(configurationNames);

    Boolean[] expectedResults = new Boolean[] {Boolean.TRUE, Boolean.FALSE, null};
    for (int i = 0; i < configurationNames.length; i++) {
      Configuration configuration = configurations.get(configurationNames[i]);
      assertThat(configuration.getVrfs().size(), equalTo(1));
      for (Vrf vrf : configuration.getVrfs().values()) {
        assertThat(vrf.getOspfProcess().getRfc1583Compatible(), is(expectedResults[i]));
      }
    }
  }

  @Test
  public void testAristaSubinterfaceMtu() throws IOException {
    Configuration c = parseConfig("aristaInterface");

    assertThat(c, hasInterface("Ethernet3/2/1.4", hasMtu(9000)));
  }

  @Test
  public void testNxosBgpVrf() throws IOException {
    Configuration c = parseConfig("nxosBgpVrf");
    assertThat(c.getVrfs().get("bar").getBgpProcess().getNeighbors().values(), hasSize(1));
  }
}

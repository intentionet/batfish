package org.batfish.grammar.arista;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.matchers.ParseWarningMatchers.hasComment;
import static org.batfish.common.matchers.ParseWarningMatchers.hasText;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.datamodel.Names.generatedBgpPeerEvpnExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerExportPolicyName;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasAllowRemoteAsOut;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasSendCommunity;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasSendExtendedCommunity;
import static org.batfish.datamodel.matchers.AddressFamilyMatchers.hasAddressFamilyCapabilites;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasIpv4UnicastAddressFamily;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbors;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasConfigurationFormat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDefaultVrf;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessList;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasMlagConfig;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasBandwidth;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRedFlagWarning;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllowedVlans;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDescription;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasEncapsulationVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMlagId;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSpeed;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVrf;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isSwitchport;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.hasLines;
import static org.batfish.datamodel.matchers.MlagMatchers.hasId;
import static org.batfish.datamodel.matchers.MlagMatchers.hasPeerAddress;
import static org.batfish.datamodel.matchers.MlagMatchers.hasPeerInterface;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasBumTransportIps;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasBumTransportMethod;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasSourceAddress;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasUdpPort;
import static org.batfish.datamodel.matchers.VniSettingsMatchers.hasVlan;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasL2VniSettings;
import static org.batfish.datamodel.matchers.VrfMatchers.hasName;
import static org.batfish.datamodel.transformation.Transformation.when;
import static org.batfish.datamodel.transformation.TransformationStep.assignSourceIp;
import static org.batfish.main.BatfishTestUtils.TEST_SNAPSHOT;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.arista.AristaStructureType.INTERFACE;
import static org.batfish.representation.arista.AristaStructureType.VXLAN;
import static org.batfish.representation.arista.OspfProcess.getReferenceOspfBandwidth;
import static org.batfish.representation.arista.eos.AristaBgpProcess.DEFAULT_VRF;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_EXTERNAL;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_INTERNAL;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_NSSA_EXTERNAL;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_NSSA_EXTERNAL_TYPE_1;
import static org.batfish.representation.arista.eos.AristaRedistributeType.OSPF_NSSA_EXTERNAL_TYPE_2;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.Warnings;
import org.batfish.common.plugin.IBatfish;
import org.batfish.config.Settings;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Bgpv4Route.Builder;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.VrrpGroup;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.bgp.BgpConfederation;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Layer2VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.matchers.ConfigurationMatchers;
import org.batfish.datamodel.matchers.MlagMatchers;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Layer3Vni;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.batfish.representation.arista.AristaConfiguration;
import org.batfish.representation.arista.MlagConfiguration;
import org.batfish.representation.arista.VrrpInterface;
import org.batfish.representation.arista.eos.AristaBgpAggregateNetwork;
import org.batfish.representation.arista.eos.AristaBgpBestpathTieBreaker;
import org.batfish.representation.arista.eos.AristaBgpDefaultOriginate;
import org.batfish.representation.arista.eos.AristaBgpNeighbor.RemovePrivateAsMode;
import org.batfish.representation.arista.eos.AristaBgpNeighborAddressFamily;
import org.batfish.representation.arista.eos.AristaBgpNetworkConfiguration;
import org.batfish.representation.arista.eos.AristaBgpPeerGroupNeighbor;
import org.batfish.representation.arista.eos.AristaBgpRedistributionPolicy;
import org.batfish.representation.arista.eos.AristaBgpV4DynamicNeighbor;
import org.batfish.representation.arista.eos.AristaBgpV4Neighbor;
import org.batfish.representation.arista.eos.AristaBgpVlan;
import org.batfish.representation.arista.eos.AristaBgpVlanAwareBundle;
import org.batfish.representation.arista.eos.AristaBgpVrf;
import org.batfish.representation.arista.eos.AristaBgpVrfEvpnAddressFamily;
import org.batfish.representation.arista.eos.AristaBgpVrfIpv4UnicastAddressFamily;
import org.batfish.representation.arista.eos.AristaBgpVrfIpv6UnicastAddressFamily;
import org.batfish.representation.arista.eos.AristaEosVxlan;
import org.batfish.representation.arista.eos.AristaRedistributeType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@ParametersAreNonnullByDefault
public class AristaGrammarTest {
  private static final String DEFAULT_VRF_NAME = "default";
  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/arista/testconfigs/";
  private static final String TESTRIGS_PREFIX = "org/batfish/grammar/arista/testrigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private static @Nonnull AristaConfiguration parseVendorConfig(String hostname) {
    String src = readResource(TESTCONFIGS_PREFIX + hostname, UTF_8);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    AristaCombinedParser parser = new AristaCombinedParser(src, settings);
    AristaControlPlaneExtractor extractor =
        new AristaControlPlaneExtractor(src, parser, ConfigurationFormat.ARISTA, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(TEST_SNAPSHOT, tree);
    AristaConfiguration vendorConfiguration =
        (AristaConfiguration) extractor.getVendorConfiguration();
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + hostname);
    // crash if not serializable
    return SerializationUtils.clone(vendorConfiguration);
  }

  private @Nonnull Batfish getBatfishForConfigurationNames(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private @Nonnull Configuration parseConfig(String hostname) {
    try {
      Map<String, Configuration> configs = parseTextConfigs(hostname);
      String canonicalHostname = hostname.toLowerCase();
      assertThat(configs, hasKey(canonicalHostname));
      Configuration c = configs.get(canonicalHostname);
      assertThat(c, hasConfigurationFormat(ConfigurationFormat.ARISTA));
      return c;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private @Nonnull Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    IBatfish iBatfish = getBatfishForConfigurationNames(configurationNames);
    return iBatfish.loadConfigurations(iBatfish.getSnapshot());
  }

  @Test
  public void testTopLevelBgpExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp");
    // Basic VRF config
    {
      AristaBgpVrf defaultVrf = config.getAristaBgp().getDefaultVrf();
      assertThat(defaultVrf.getAdvertiseInactive(), equalTo(Boolean.TRUE));
      assertTrue(defaultVrf.getShutdown());
      assertThat(defaultVrf.getRouterId(), equalTo(Ip.parse("1.2.3.4")));
      assertThat(defaultVrf.getKeepAliveTimer(), equalTo(3));
      assertThat(defaultVrf.getHoldTimer(), equalTo(9));
      assertThat(defaultVrf.getEbgpAdminDistance(), equalTo(300));
      assertThat(defaultVrf.getIbgpAdminDistance(), nullValue());
      assertThat(defaultVrf.getLocalAdminDistance(), nullValue());
      assertThat(defaultVrf.getDefaultMetric(), equalTo(100L));
      assertFalse(defaultVrf.getDefaultIpv4Unicast());
      assertThat(defaultVrf.getMaxPaths(), equalTo(2));
      assertThat(defaultVrf.getMaxPathsEcmp(), nullValue());
    }
    {
      String vrfName = "tenant_vrf";
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get(vrfName);
      assertThat(vrf.getAdvertiseInactive(), nullValue());
      assertThat(vrf.getShutdown(), nullValue());
      assertThat(vrf.getRouterId(), equalTo(Ip.parse("5.6.7.8")));
      assertThat(vrf.getKeepAliveTimer(), equalTo(6));
      assertThat(vrf.getHoldTimer(), equalTo(18));
      assertThat(vrf.getEbgpAdminDistance(), equalTo(333));
      assertThat(vrf.getIbgpAdminDistance(), equalTo(400));
      assertThat(vrf.getLocalAdminDistance(), equalTo(500));
      assertTrue(vrf.getDefaultIpv4Unicast());
      assertThat(vrf.getMaxPaths(), equalTo(3));
      assertThat(vrf.getMaxPathsEcmp(), equalTo(2));
    }
    {
      String vrfName = "tenant2_vrf";
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get(vrfName);
      assertThat(vrf.getAdvertiseInactive(), nullValue());
      assertTrue(vrf.getShutdown());
      assertThat(vrf.getRouterId(), nullValue());
    }
  }

  @Test
  public void testAristaOspfReferenceBandwidth() throws IOException {
    Configuration manual = parseConfig("aristaOspfCost");
    assertThat(
        manual.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(), equalTo(3e6d));

    Configuration defaults = parseConfig("aristaOspfCostDefaults");
    assertThat(
        defaults.getDefaultVrf().getOspfProcesses().get("1").getReferenceBandwidth(),
        equalTo(getReferenceOspfBandwidth()));
  }

  @Test
  public void testAristaVsIosOspfRedistributeBgpSyntax() throws IOException {
    // Both an Arista and an IOS config contain these "redistribute bgp" lines in their OSPF setups:
    String l1 = "redistribute bgp";
    String l2 = "redistribute bgp route-map MAP";
    String l3 = "redistribute bgp 65100";
    String l4 = "redistribute bgp 65100 route-map MAP";
    String l5 = "redistribute bgp 65100 metric 10";
    /*
    Arista does not allow specification of an AS, while IOS requires it, and IOS allows setting
    metric and a few other settings where Arista does not (both allow route-maps). So:
    - First two lines should generate warnings on IOS but not Arista
    - Remaining lines should generate warnings on Arista but not IOS
    */
    String testrigName = "arista-vs-ios-warnings";
    String iosName = "ios-ospf-redistribute-bgp";
    String aristaName = "arista-ospf-redistribute-bgp";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(
                    TESTRIGS_PREFIX + testrigName, ImmutableList.of(aristaName, iosName))
                .build(),
            _folder);
    batfish.getSettings().setDisableUnrecognized(false);
    ParseVendorConfigurationAnswerElement pvcae =
        batfish.loadParseVendorConfigurationAnswerElement(batfish.getSnapshot());
    assertThat(
        pvcae.getWarnings().get("configs/" + iosName).getParseWarnings(),
        contains(
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l1))),
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l2)))));
    assertThat(
        pvcae.getWarnings().get("configs/" + aristaName).getParseWarnings(),
        contains(
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l3))),
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l4))),
            allOf(hasComment("This syntax is unrecognized"), hasText(containsString(l5)))));
  }

  @Test
  public void testAristaBgpDefaultOriginatePolicy() throws IOException {
    /*
     Arista originator has: neighbor 10.1.1.2 default-originate route-map ROUTE_MAP
     Because this is an Arista device, the route-map should be applied to the default route before
     it is exported rather than used as a default route generation policy.
    */
    String testrigName = "arista-bgp-default-originate";
    List<String> configurationNames = ImmutableList.of("arista-originator", "ios-listener");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    Set<AbstractRoute> listenerRoutes =
        dp.getRibs().get("ios-listener").get(DEFAULT_VRF_NAME).getRoutes();

    // ROUTE_MAP adds two communities to default route. Make sure listener's default route has them.
    Bgpv4Route expectedDefaultRoute =
        Bgpv4Route.builder()
            .setCommunities(
                ImmutableSet.of(StandardCommunity.of(7274718L), StandardCommunity.of(21823932L)))
            .setNetwork(Prefix.ZERO)
            .setNextHopIp(Ip.parse("10.1.1.1"))
            .setReceivedFromIp(Ip.parse("10.1.1.1"))
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setSrcProtocol(RoutingProtocol.BGP)
            .setAsPath(AsPath.ofSingletonAsSets(1L))
            .setAdmin(20)
            .setLocalPreference(100)
            .build();
    assertThat(listenerRoutes, hasItem(equalTo(expectedDefaultRoute)));
    // Ensure 10.10.10.0/24 doesn't get blocked
    assertThat(listenerRoutes, hasItem(hasPrefix(Prefix.parse("10.10.10.0/24"))));
  }

  @Test
  public void testAristaBgpDefaultOriginateUndefinedRouteMap() throws IOException {
    /*
     Arista originator has: neighbor 10.1.1.2 default-originate route-map ROUTE_MAP
     However, ROUTE_MAP is undefined. Since this is Arista, default route should still be exported
     (on IOS, where ROUTE_MAP is used as a generation policy rather than an export policy in this
     context, the default route would not be exported).
    */
    String testrigName = "arista-bgp-default-originate";
    List<String> configurationNames =
        ImmutableList.of("arista-originator-undefined-rm", "ios-listener");
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(TESTRIGS_PREFIX + testrigName, configurationNames)
                .build(),
            _folder);

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    DataPlane dp = batfish.loadDataPlane(snapshot);
    Set<AbstractRoute> listenerRoutes =
        dp.getRibs().get("ios-listener").get(DEFAULT_VRF_NAME).getRoutes();

    Bgpv4Route expectedDefaultRoute =
        Bgpv4Route.builder()
            .setNetwork(Prefix.ZERO)
            .setNextHopIp(Ip.parse("10.1.1.1"))
            .setReceivedFromIp(Ip.parse("10.1.1.1"))
            .setOriginatorIp(Ip.parse("1.1.1.1"))
            .setOriginType(OriginType.INCOMPLETE)
            .setProtocol(RoutingProtocol.BGP)
            .setSrcProtocol(RoutingProtocol.BGP)
            .setAsPath(AsPath.ofSingletonAsSets(1L))
            .setCommunities(ImmutableSet.of())
            .setAdmin(20)
            .setLocalPreference(100)
            .build();
    assertThat(listenerRoutes, hasItem(equalTo(expectedDefaultRoute)));
  }

  @Test
  public void testAristaAdvertiseInactive() throws IOException {
    Configuration config = parseConfig("arista-advertise-inactive");
    assertTrue(
        config
            .getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Prefix.parse("1.1.1.1/32"))
            .getIpv4UnicastAddressFamily()
            .getAddressFamilyCapabilities()
            .getAdvertiseInactive());
  }

  @Test
  public void testAristaNoAdvertiseInactive() throws IOException {
    Configuration config = parseConfig("arista-no-advertise-inactive");
    assertFalse(
        config
            .getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Prefix.parse("1.1.1.1/32"))
            .getIpv4UnicastAddressFamily()
            .getAddressFamilyCapabilities()
            .getAdvertiseInactive());
  }

  /**
   * Ensure that Arista redistributes a static default route even though default-originate is not
   * explicitly specified in the config
   */
  @Test
  public void testAristaDefaultRouteRedistribution() throws IOException {
    final String receiver = "ios_receiver";
    final String advertiser = "arista_advertiser";
    Batfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(
                    TESTRIGS_PREFIX + "arista-redistribute-default-route",
                    ImmutableList.of(advertiser, receiver))
                .build(),
            _folder);

    NetworkSnapshot snapshot = batfish.getSnapshot();
    batfish.computeDataPlane(snapshot);
    Set<AbstractRoute> routes =
        batfish.loadDataPlane(snapshot).getRibs().get(receiver).get(DEFAULT_VRF_NAME).getRoutes();

    assertThat(
        routes,
        hasItem(
            OspfExternalType2Route.builder()
                .setNetwork(Prefix.ZERO)
                .setNextHopIp(Ip.parse("1.2.3.5"))
                .setArea(1)
                .setCostToAdvertiser(1)
                .setAdvertiser(advertiser)
                .setAdmin(110)
                .setMetric(1)
                .setLsaMetric(1)
                .build()));
  }

  @Test
  public void testAristaDefaultSwitchPort() throws IOException {
    Configuration cUndeclared = parseConfig("eos_switchport_default_mode_undeclared");
    Configuration cDefaultAccess = parseConfig("eos_switchport_default_mode_access");
    Configuration cDefaultRouted = parseConfig("eos_switchport_default_mode_routed");

    String l0Name = "Loopback0";
    String e0Name = "Ethernet0/0";
    String e1Name = "Ethernet0/1";
    String e2Name = "Ethernet0/2";
    String e3Name = "Ethernet0/3";
    String e4Name = "Ethernet0/4";
    String p0Name = "Port-Channel0";

    Interface l0Undeclared = cUndeclared.getAllInterfaces().get(l0Name);
    Interface e0Undeclared = cUndeclared.getAllInterfaces().get(e0Name);
    Interface e1Undeclared = cUndeclared.getAllInterfaces().get(e1Name);
    Interface e2Undeclared = cUndeclared.getAllInterfaces().get(e2Name);
    Interface e3Undeclared = cUndeclared.getAllInterfaces().get(e3Name);
    Interface e4Undeclared = cUndeclared.getAllInterfaces().get(e4Name);
    Interface p0Undeclared = cUndeclared.getAllInterfaces().get(p0Name);

    Interface l0DefaultAccess = cDefaultAccess.getAllInterfaces().get(l0Name);
    Interface e0DefaultAccess = cDefaultAccess.getAllInterfaces().get(e0Name);
    Interface e1DefaultAccess = cDefaultAccess.getAllInterfaces().get(e1Name);
    Interface e2DefaultAccess = cDefaultAccess.getAllInterfaces().get(e2Name);
    Interface e3DefaultAccess = cDefaultAccess.getAllInterfaces().get(e3Name);
    Interface e4DefaultAccess = cDefaultAccess.getAllInterfaces().get(e4Name);
    Interface p0DefaultAccess = cDefaultAccess.getAllInterfaces().get(p0Name);

    Interface l0DefaultRouted = cDefaultRouted.getAllInterfaces().get(l0Name);
    Interface e0DefaultRouted = cDefaultRouted.getAllInterfaces().get(e0Name);
    Interface e1DefaultRouted = cDefaultRouted.getAllInterfaces().get(e1Name);
    Interface e2DefaultRouted = cDefaultRouted.getAllInterfaces().get(e2Name);
    Interface e3DefaultRouted = cDefaultRouted.getAllInterfaces().get(e3Name);
    Interface e4DefaultRouted = cDefaultRouted.getAllInterfaces().get(e4Name);
    Interface p0DefaultRouted = cDefaultRouted.getAllInterfaces().get(p0Name);

    assertThat(l0Undeclared, isSwitchport(false));
    assertThat(l0Undeclared, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e0Undeclared, isSwitchport(true));
    assertThat(e0Undeclared, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e1Undeclared, isSwitchport(true));
    assertThat(e1Undeclared, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e2Undeclared, isSwitchport(true));
    assertThat(e2Undeclared, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e3Undeclared, isSwitchport(false));
    assertThat(e3Undeclared, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e4Undeclared, isSwitchport(true));
    assertThat(e4Undeclared, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(p0Undeclared, isSwitchport(true));
    assertThat(p0Undeclared, hasSwitchPortMode(SwitchportMode.ACCESS));

    assertThat(l0DefaultAccess, isSwitchport(false));
    assertThat(l0DefaultAccess, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e0DefaultAccess, isSwitchport(true));
    assertThat(e0DefaultAccess, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e1DefaultAccess, isSwitchport(true));
    assertThat(e1DefaultAccess, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e2DefaultAccess, isSwitchport(true));
    assertThat(e2DefaultAccess, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e3DefaultAccess, isSwitchport(false));
    assertThat(e3DefaultAccess, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e4DefaultAccess, isSwitchport(true));
    assertThat(e4DefaultAccess, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(p0DefaultAccess, isSwitchport(true));
    assertThat(p0DefaultAccess, hasSwitchPortMode(SwitchportMode.ACCESS));

    assertThat(l0DefaultRouted, isSwitchport(false));
    assertThat(l0DefaultRouted, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e0DefaultRouted, isSwitchport(false));
    assertThat(e0DefaultRouted, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e1DefaultRouted, isSwitchport(true));
    assertThat(e1DefaultRouted, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e2DefaultRouted, isSwitchport(true));
    assertThat(e2DefaultRouted, hasSwitchPortMode(SwitchportMode.ACCESS));
    assertThat(e3DefaultRouted, isSwitchport(false));
    assertThat(e3DefaultRouted, hasSwitchPortMode(SwitchportMode.NONE));
    assertThat(e4DefaultRouted, isSwitchport(true));
    assertThat(e4DefaultRouted, hasSwitchPortMode(SwitchportMode.TRUNK));
    assertThat(p0DefaultRouted, isSwitchport(false));
    assertThat(p0DefaultRouted, hasSwitchPortMode(SwitchportMode.NONE));
  }

  @Test
  public void testAristaDynamicSourceNat() throws IOException {
    Configuration c = parseConfig("arista-dynamic-source-nat");
    Interface iface = c.getAllInterfaces().get("Ethernet1");
    assertThat(iface.getIncomingTransformation(), nullValue());
    assertThat(
        iface.getOutgoingTransformation(),
        equalTo(
            when(permittedByAcl("acl1"))
                .apply(assignSourceIp(Ip.parse("1.1.1.1"), Ip.parse("1.1.1.2")))
                .build()));

    iface = c.getAllInterfaces().get("Ethernet2");
    assertThat(iface.getIncomingTransformation(), nullValue());
    assertThat(
        iface.getOutgoingTransformation(),
        equalTo(
            when(permittedByAcl("acl1"))
                .apply(assignSourceIp(Ip.parse("1.1.1.1"), Ip.parse("1.1.1.2")))
                .setOrElse(
                    when(permittedByAcl("acl2"))
                        .apply(assignSourceIp(Ip.parse("2.2.2.2"), Ip.parse("2.2.2.3")))
                        .build())
                .build()));

    iface = c.getAllInterfaces().get("Ethernet3");
    assertThat(iface.getIncomingTransformation(), nullValue());
    assertThat(
        iface.getOutgoingTransformation(),
        equalTo(
            when(permittedByAcl("acl1"))
                // due to the subnet mask, shift the pool IPs to avoid network/bcast IPs.
                .apply(assignSourceIp(Ip.parse("2.0.0.1"), Ip.parse("2.0.0.6")))
                .build()));

    iface = c.getAllInterfaces().get("Ethernet4");
    assertThat(iface.getIncomingTransformation(), nullValue());
    assertThat(
        iface.getOutgoingTransformation(),
        equalTo(
            when(permittedByAcl("acl1"))
                // overload rule, so use the interface IP
                .apply(assignSourceIp(Ip.parse("8.8.8.8"), Ip.parse("8.8.8.8")))
                .build()));
  }

  @Test
  public void testAristaBanner() throws IOException {
    Configuration c = parseConfig("eos_banner");
    assertThat(
        c.getVendorFamily().getCisco().getBanners().get("login"),
        equalTo("Some text\nEOF not alone\n"));
  }

  @Test
  public void testArista100gfullInterface() throws IOException {
    Configuration c = parseConfig("arista100gfull");
    assertThat(
        c,
        hasInterface(
            "Ethernet1/1",
            hasAllAddresses(containsInAnyOrder(ConcreteInterfaceAddress.parse("10.20.0.3/31")))));
  }

  @Test
  public void testAristaSubinterfaceMtu() throws IOException {
    Configuration c = parseConfig("aristaInterface");

    assertThat(c, hasInterface("Ethernet3/2/1.4", hasMtu(9000)));
  }

  @Test
  public void testAggregateAddressExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_aggregate_address");
    {
      AristaBgpAggregateNetwork agg =
          config.getAristaBgp().getDefaultVrf().getV4aggregates().get(Prefix.parse("1.2.33.0/24"));
      assertTrue(agg.getAdvertiseOnly());
      assertTrue(agg.getAsSet());
      assertTrue(agg.getSummaryOnly());
      assertThat(agg.getAttributeMap(), equalTo("ATTR_MAP"));
      assertThat(agg.getMatchMap(), equalTo("MATCH_MAP"));
    }
    {
      AristaBgpAggregateNetwork agg =
          config.getAristaBgp().getDefaultVrf().getV4aggregates().get(Prefix.parse("1.2.44.0/24"));
      assertThat(agg.getAdvertiseOnly(), nullValue());
      assertThat(agg.getAsSet(), nullValue());
      assertTrue(agg.getSummaryOnly());
      assertThat(agg.getAttributeMap(), nullValue());
      assertThat(agg.getMatchMap(), nullValue());
    }
    {
      AristaBgpAggregateNetwork agg =
          config.getAristaBgp().getDefaultVrf().getV4aggregates().get(Prefix.parse("1.2.55.0/24"));
      assertThat(agg, notNullValue());
    }
    {
      AristaBgpAggregateNetwork agg =
          config
              .getAristaBgp()
              .getDefaultVrf()
              .getV6aggregates()
              .get(Prefix6.parse("2001:0db8:85a3:0000:0000:8a2e:0370::/112"));
      assertThat(agg.getAdvertiseOnly(), nullValue());
      assertThat(agg.getAsSet(), nullValue());
      assertThat(agg.getSummaryOnly(), equalTo(true));
      assertThat(agg.getAttributeMap(), nullValue());
      assertThat(agg.getMatchMap(), nullValue());
    }
    {
      AristaBgpAggregateNetwork agg =
          config
              .getAristaBgp()
              .getVrfs()
              .get("FOO")
              .getV4aggregates()
              .get(Prefix.parse("5.6.7.0/24"));
      assertTrue(agg.getAsSet());
    }
    {
      AristaBgpAggregateNetwork agg =
          config
              .getAristaBgp()
              .getVrfs()
              .get("FOO")
              .getV6aggregates()
              .get(Prefix6.parse("2001:0db8:85a3:0000:0000:8a2e:0370::/112"));
      assertThat(agg.getAdvertiseOnly(), nullValue());
      assertThat(agg.getAsSet(), nullValue());
      assertThat(agg.getSummaryOnly(), nullValue());
      assertThat(agg.getAttributeMap(), equalTo("ATTR_MAP6"));
      assertThat(agg.getMatchMap(), nullValue());
    }
  }

  @Test
  public void testAggregateAddressConversion() {
    // Don't crash.
    parseConfig("arista_bgp_aggregate_address");
  }

  @Test
  public void testAllowedVlans() throws IOException {
    Configuration c = parseConfig("eos-allowed-vlans");

    assertThat(
        c.getAllInterfaces().get("Port-Channel1").getAllowedVlans(),
        equalTo(IntegerSpace.of(Range.closed(1, 2))));
    assertThat(
        c.getAllInterfaces().get("Port-Channel2").getAllowedVlans(),
        equalTo(IntegerSpace.of(Range.closed(2, 3))));
    assertThat(
        c.getAllInterfaces().get("Port-Channel3").getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    assertThat(
        c.getAllInterfaces().get("Port-Channel4").getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    assertThat(
        c.getAllInterfaces().get("Port-Channel5").getAllowedVlans(),
        equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    assertThat(
        c.getAllInterfaces().get("Port-Channel6").getAllowedVlans(),
        equalTo(IntegerSpace.of(Range.closed(1, 4))));
  }

  @Test
  public void testEosMlagExtraction() {
    AristaConfiguration c = parseVendorConfig("eos-mlag");
    MlagConfiguration mlag = c.getEosMlagConfiguration();
    assertThat(mlag, notNullValue());
    assertThat(mlag.getDomainId(), equalTo("MLAG_DOMAIN_ID"));
    assertThat(mlag.getLocalInterface(), equalTo("Vlan4094"));
    assertThat(mlag.getPeerAddress(), equalTo(Ip.parse("1.1.1.3")));
    assertThat(mlag.getPeerAddressHeartbeat(), equalTo(Ip.parse("1.1.1.4")));
    assertThat(mlag.getPeerLink(), equalTo("Port-Channel1"));
  }

  @Test
  public void testEosMlagConversion() throws IOException {
    Configuration c = parseConfig("eos-mlag");

    final String mlagName = "MLAG_DOMAIN_ID";
    assertThat(c, hasMlagConfig(mlagName, hasId(mlagName)));
    assertThat(c, hasMlagConfig(mlagName, hasPeerAddress(Ip.parse("1.1.1.3"))));
    assertThat(c, hasMlagConfig(mlagName, hasPeerInterface("Port-Channel1")));
    assertThat(c, hasMlagConfig(mlagName, MlagMatchers.hasLocalInterface("Vlan4094")));

    // Test interface config
    assertThat(c, hasInterface("Port-Channel1", hasMlagId(5)));
  }

  @Test
  public void testEosBgpPeers() throws IOException {
    String hostname = "eos-bgp-peers";
    Prefix neighborWithRemoteAs = Prefix.parse("1.1.1.1/32");
    Prefix neighborWithoutRemoteAs = Prefix.parse("2.2.2.2/32");

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    /*
     * The peer with a remote-as should appear in the datamodel. The peer without a remote-as
     * should not appear, and there should be a warning about the missing remote-as.
     */
    assertThat(
        c, hasDefaultVrf(hasBgpProcess(hasActiveNeighbor(neighborWithRemoteAs, hasRemoteAs(1L)))));
    assertThat(c, hasDefaultVrf(hasBgpProcess(hasNeighbors(not(hasKey(neighborWithoutRemoteAs))))));
    assertThat(
        ccae,
        hasRedFlagWarning(
            hostname,
            containsString(
                String.format(
                    "No remote-as configured for BGP neighbor %s in vrf default",
                    neighborWithoutRemoteAs.getStartIp()))));

    /*
     * Also ensure that default value of allowRemoteAsOut is true.
     */
    assertThat(
        c,
        hasDefaultVrf(
            hasBgpProcess(
                hasActiveNeighbor(
                    neighborWithRemoteAs,
                    hasIpv4UnicastAddressFamily(
                        hasAddressFamilyCapabilites(hasAllowRemoteAsOut(true)))))));
  }

  @Test
  public void testEosPortChannel() throws IOException {
    String hostname = "eos-port-channel";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);

    assertThat(c, hasInterface("Ethernet0", hasBandwidth(40E9D)));
    assertThat(c, hasInterface("Ethernet0", hasSpeed(40E9D)));
    assertThat(c, hasInterface("Ethernet1", hasBandwidth(40E9D)));
    assertThat(c, hasInterface("Ethernet1", hasSpeed(40E9D)));
    assertThat(c, hasInterface("Ethernet2", hasBandwidth(40E9D)));
    assertThat(c, hasInterface("Ethernet2", hasSpeed(40E9D)));
    assertThat(c, hasInterface("Port-Channel1", hasBandwidth(80E9D)));
    assertThat(c, hasInterface("Port-Channel1", hasSpeed(nullValue())));
    assertThat(c, hasInterface("Port-Channel2", isActive(false)));
    // 0 since no members
    assertThat(c, hasInterface("Port-Channel2", hasBandwidth(0D)));
    assertThat(c, hasInterface("Port-Channel2", hasSpeed(nullValue())));
    assertThat(
        c.getAllInterfaces().get("Port-Channel1").getDependencies(),
        equalTo(
            ImmutableSet.of(
                new Dependency("Ethernet0", DependencyType.AGGREGATE),
                new Dependency("Ethernet1", DependencyType.AGGREGATE))));
  }

  @Test
  public void testEosVxlan() throws IOException {
    String hostnameBase = "eos-vxlan";
    String hostnameNoLoopbackAddr = "eos-vxlan-no-loopback-address";
    String hostnameNoSourceIface = "eos-vxlan-no-source-interface";

    Batfish batfish =
        getBatfishForConfigurationNames(
            hostnameBase, hostnameNoSourceIface, hostnameNoLoopbackAddr);
    // Config with proper loopback iface, VLAN-specific unicast, explicit UDP port
    Configuration configBase = batfish.loadConfigurations(batfish.getSnapshot()).get(hostnameBase);
    assertThat(configBase, hasDefaultVrf(hasL2VniSettings(hasKey(10002))));
    Layer2Vni vnisBase = configBase.getDefaultVrf().getLayer2Vnis().get(10002);

    // Config with no loopback address, using multicast, and default UDP port
    Configuration configNoLoopbackAddr =
        batfish.loadConfigurations(batfish.getSnapshot()).get(hostnameNoLoopbackAddr);
    assertThat(configNoLoopbackAddr, hasDefaultVrf(hasL2VniSettings(hasKey(10002))));
    Layer2Vni vnisNoAddr = configNoLoopbackAddr.getDefaultVrf().getLayer2Vnis().get(10002);

    // Config with no source interface and general VXLAN unicast address
    Configuration configNoSourceIface =
        batfish.loadConfigurations(batfish.getSnapshot()).get(hostnameNoSourceIface);
    assertThat(configNoSourceIface, hasDefaultVrf(hasL2VniSettings(hasKey(10002))));
    Layer2Vni vnisNoIface = configNoSourceIface.getDefaultVrf().getLayer2Vnis().get(10002);

    // Confirm VLAN-specific unicast address takes priority over the other addresses
    assertThat(vnisBase, hasBumTransportMethod(equalTo(BumTransportMethod.UNICAST_FLOOD_GROUP)));
    assertThat(vnisBase, hasBumTransportIps(contains(Ip.parse("1.1.1.10"))));
    // Confirm source address is inherited from source interface
    assertThat(vnisBase, hasSourceAddress(equalTo(Ip.parse("1.1.1.4"))));
    // Confirm explicit UDP port is used
    assertThat(vnisBase, hasUdpPort(equalTo(5555)));
    // Confirm VLAN<->VNI mapping is applied
    assertThat(vnisBase, hasVlan(equalTo(2)));

    // Confirm multicast address is present
    assertThat(vnisNoAddr, hasBumTransportMethod(equalTo(BumTransportMethod.MULTICAST_GROUP)));
    assertThat(vnisNoAddr, hasBumTransportIps(contains(Ip.parse("227.10.1.1"))));
    // Confirm no source address is present (no address specified for loopback interface)
    assertThat(vnisNoAddr, hasSourceAddress(nullValue()));
    // Confirm default UDP port is used even though none is supplied
    assertThat(vnisNoAddr, hasUdpPort(equalTo(4789)));

    // Confirm general VXLAN flood addresses are used
    assertThat(vnisNoIface, hasBumTransportMethod(equalTo(BumTransportMethod.UNICAST_FLOOD_GROUP)));
    assertThat(
        vnisNoIface,
        hasBumTransportIps(containsInAnyOrder(Ip.parse("1.1.1.5"), Ip.parse("1.1.1.6"))));
    // Confirm no source address is present (no interface is linked to the VXLAN)
    assertThat(vnisNoIface, hasSourceAddress(nullValue()));
  }

  @Test
  public void testEosVxlanMisconfig() throws IOException {
    String hostname = "eos-vxlan-misconfig";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration config = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);

    // Make sure that misconfigured VXLAN is still converted into VI model properly
    assertThat(config, hasDefaultVrf(hasL2VniSettings(hasKey(10002))));
    Layer2Vni vnisMisconfig = config.getDefaultVrf().getLayer2Vnis().get(10002);

    // No BUM IPs specified
    assertThat(vnisMisconfig, hasBumTransportIps(emptyIterable()));
    // No source interface so no source address
    assertThat(vnisMisconfig, hasSourceAddress(nullValue()));
    // Confirm default UDP port is used
    assertThat(vnisMisconfig, hasUdpPort(equalTo(4789)));
    // Confirm VLAN<->VNI mapping is applied
    assertThat(vnisMisconfig, hasVlan(equalTo(2)));
  }

  @Test
  public void testEosTrunkGroup() throws IOException {
    String hostname = "eos_trunk_group";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);

    assertThat(
        c, hasInterface("Port-Channel1", hasAllowedVlans(IntegerSpace.of(Range.closed(1, 2)))));
    assertThat(c, hasInterface("Port-Channel2", hasAllowedVlans(IntegerSpace.of(99))));
  }

  @Test
  public void testEosVxlanCiscoConfig() {
    String hostname = "eos-vxlan";

    AristaConfiguration config = parseVendorConfig(hostname);

    assertThat(config, notNullValue());
    AristaEosVxlan eosVxlan = config.getEosVxlan();
    assertThat(eosVxlan, notNullValue());

    assertThat(eosVxlan.getDescription(), equalTo("vxlan vti"));
    // Confirm flood address set doesn't contain the removed address
    assertThat(
        eosVxlan.getFloodAddresses(), containsInAnyOrder(Ip.parse("1.1.1.5"), Ip.parse("1.1.1.7")));
    assertThat(eosVxlan.getMulticastGroup(), equalTo(Ip.parse("227.10.1.1")));
    assertThat(eosVxlan.getSourceInterface(), equalTo("Loopback1"));
    assertThat(eosVxlan.getUdpPort(), equalTo(5555));

    assertThat(eosVxlan.getVlanVnis(), hasEntry(equalTo(2), equalTo(10002)));

    // Confirm flood address set was overwritten as expected
    assertThat(
        eosVxlan.getVlanFloodAddresses(), hasEntry(equalTo(2), contains(Ip.parse("1.1.1.10"))));
  }

  @Test
  public void testEosVxlanReference() throws IOException {
    String hostname = "eos-vxlan";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(ccae, hasNumReferrers(filename, VXLAN, "Vxlan1", 1));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "Loopback1", 2));
  }

  @Test
  public void testBgpVlansExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_vlans");
    {
      AristaBgpVlanAwareBundle bundle = config.getAristaBgp().getVlanAwareBundles().get("Tenant_A");
      assertThat(bundle, notNullValue());
      assertThat(bundle.getRd(), equalTo(RouteDistinguisher.parse("192.168.255.8:10101")));
      assertThat(bundle.getRtImport(), equalTo(ExtendedCommunity.target(10101, 10101)));
      assertThat(bundle.getRtExport(), equalTo(ExtendedCommunity.target(10101, 10101)));
      assertThat(bundle.getVlans(), equalTo(IntegerSpace.builder().including(1, 110, 111).build()));
    }

    {
      AristaBgpVlan vlan = config.getAristaBgp().getVlans().get(300);
      assertThat(vlan, notNullValue());
      assertThat(vlan.getRd(), equalTo(RouteDistinguisher.parse("192.168.255.100:10103")));
      assertThat(vlan.getRtImport(), equalTo(ExtendedCommunity.target(10101, 10103)));
      assertThat(vlan.getRtExport(), equalTo(ExtendedCommunity.target(10101, 10103)));
    }
  }

  @Test
  public void testCommunityListExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_community_list");
    assertThat(config.getStandardCommunityLists(), hasKey("SOME_CL"));
  }

  @Test
  public void testNeighborExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_neighbors");
    {
      String peergName = "PEER_G";
      assertThat(config.getAristaBgp().getPeerGroups(), hasKey(peergName));
      AristaBgpPeerGroupNeighbor neighbor = config.getAristaBgp().getPeerGroups().get(peergName);
      assertThat(config.getAristaBgp().getPeerGroups().get(peergName).getAllowAsIn(), equalTo(3));
      assertThat(neighbor.getRemovePrivateAsMode(), nullValue());
      assertThat(neighbor.getRouteReflectorClient(), nullValue());
    }
    {
      AristaBgpPeerGroupNeighbor pg = config.getAristaBgp().getPeerGroups().get("PEER_G2");
      assertThat(pg, notNullValue());
      assertThat(pg.getRouteReflectorClient(), equalTo(Boolean.TRUE));
    }
    {
      Ip neighborAddr = Ip.parse("1.1.1.1");
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getDefaultVrf().getV4neighbors().get(neighborAddr);
      assertThat(neighbor.getAllowAsIn(), nullValue());
      AristaBgpDefaultOriginate defaultOriginate = neighbor.getDefaultOriginate();
      assertThat(defaultOriginate, notNullValue());
      assertThat(defaultOriginate.getAlways(), equalTo(true));
      assertThat(defaultOriginate.getRouteMap(), nullValue());
      assertThat(neighbor.getDescription(), equalTo("SOME NEIGHBOR"));
      assertTrue(neighbor.getDontCapabilityNegotiate());
      assertThat(neighbor.getEbgpMultihop(), equalTo(Integer.MAX_VALUE));
      assertThat(neighbor.getLocalAs(), equalTo(65111L));
      assertTrue(neighbor.getNextHopSelf());
      assertTrue(neighbor.getNextHopUnchanged());
      assertThat(neighbor.getPeerGroup(), equalTo("PEER_G"));
      assertThat(neighbor.getRemoteAs(), equalTo(35L));
      assertThat(neighbor.getGenericAddressFamily().getRouteMapIn(), equalTo("RM_IN"));
      assertThat(neighbor.getGenericAddressFamily().getRouteMapOut(), equalTo("RM_OUT"));
      assertThat(neighbor.getRemovePrivateAsMode(), is(RemovePrivateAsMode.BASIC));
      assertThat(neighbor.getRouteReflectorClient(), nullValue());
      assertTrue(neighbor.getSendCommunity());
      assertThat(neighbor.getShutdown(), nullValue());
      assertThat(neighbor.getUpdateSource(), equalTo("Loopback0"));
    }
    {
      Ip neighborAddr = Ip.parse("2.2.2.2");
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getDefaultVrf().getV4neighbors().get(neighborAddr);
      AristaBgpDefaultOriginate defaultOriginate = neighbor.getDefaultOriginate();
      assertThat(defaultOriginate, notNullValue());
      assertThat(defaultOriginate.getAlways(), equalTo(true));
      assertThat(defaultOriginate.getRouteMap(), equalTo("DEF_ORIG_MAP"));
      assertThat(neighbor.getEbgpMultihop(), equalTo(10));
      assertThat(neighbor.getRemoteAs(), equalTo(36L));
      assertThat(neighbor.getRemovePrivateAsMode(), is(RemovePrivateAsMode.ALL));
      assertThat(neighbor.getRouteReflectorClient(), nullValue());
      assertThat(neighbor.getShutdown(), equalTo(Boolean.TRUE));
      // TODO: default-originate
    }
    {
      Ip neighborAddr = Ip.parse("3.3.3.3");
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getDefaultVrf().getV4neighbors().get(neighborAddr);
      assertThat(neighbor.getEnforceFirstAs(), equalTo(Boolean.FALSE));
      assertThat(neighbor.getPeerGroup(), equalTo("PEER_G2"));
      assertThat(neighbor.getRemovePrivateAsMode(), is(RemovePrivateAsMode.REPLACE_AS));
      assertThat(neighbor.getRouteReflectorClient(), nullValue());
      assertThat(neighbor.getShutdown(), equalTo(Boolean.FALSE));
    }
    {
      Ip neighborAddr = Ip.parse("2.2.2.2");
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getVrfs().get("tenant").getV4neighbors().get(neighborAddr);
      assertThat(neighbor.getRemoteAs(), equalTo(88L));
      assertThat(neighbor.getRouteReflectorClient(), nullValue());
    }
    {
      assertThat(config.getAristaBgp().getDefaultVrf().getListenLimit(), equalTo(10));
      Prefix neighborAddr = Prefix.parse("4.4.4.0/24");
      AristaBgpV4DynamicNeighbor neighbor =
          config.getAristaBgp().getDefaultVrf().getV4DynamicNeighbors().get(neighborAddr);
      assertThat(config.getAristaBgp().getPeerGroups(), hasKey("DYNAMIC"));
      assertThat(neighbor.getPeerGroup(), equalTo("DYNAMIC"));
      assertThat(neighbor.getRange(), equalTo(neighborAddr));
      assertThat(neighbor.getRouteReflectorClient(), nullValue());
    }
    {
      Ip neighborAddr = Ip.parse("5.5.5.5");
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getDefaultVrf().getV4neighbors().get(neighborAddr);
      assertThat(neighbor.getEnforceFirstAs(), equalTo(true));
      assertThat(neighbor.getRemoteAs(), nullValue());
    }
  }

  @Test
  public void testNeighborConversion() {
    Configuration c = parseConfig("arista_bgp_neighbors");
    assertThat(c.getDefaultVrf(), notNullValue());
    BgpProcess proc = c.getDefaultVrf().getBgpProcess();
    assertThat(proc, notNullValue());
    {
      Prefix neighborPrefix = Prefix.parse("1.1.1.1/32");
      assertThat(proc.getActiveNeighbors(), hasKey(neighborPrefix));
      BgpActivePeerConfig neighbor = proc.getActiveNeighbors().get(neighborPrefix);
      assertThat(neighbor.getClusterId(), equalTo(Ip.parse("99.99.99.99").asLong()));
      assertThat(neighbor.getEnforceFirstAs(), equalTo(true));
      assertThat(neighbor.getIpv4UnicastAddressFamily(), notNullValue());
      assertThat(neighbor.getGeneratedRoutes(), hasSize(1));
      GeneratedRoute defaultOriginate = Iterables.getOnlyElement(neighbor.getGeneratedRoutes());
      assertThat(defaultOriginate.getGenerationPolicy(), nullValue());
      assertThat(defaultOriginate.getAttributePolicy(), nullValue());
      Ipv4UnicastAddressFamily af = neighbor.getIpv4UnicastAddressFamily();
      assertThat(af.getAddressFamilyCapabilities().getAllowLocalAsIn(), equalTo(true));
      assertThat(af.getAddressFamilyCapabilities().getAllowRemoteAsOut(), equalTo(true));
      assertThat(af.getRouteReflectorClient(), equalTo(false));
    }
    {
      Prefix neighborPrefix = Prefix.parse("2.2.2.2/32");
      // shutdown neighbor is not converted
      assertThat(proc.getActiveNeighbors(), not(hasKey(neighborPrefix)));
    }
    {
      Prefix neighborPrefix = Prefix.parse("3.3.3.3/32");
      assertThat(proc.getActiveNeighbors(), hasKey(neighborPrefix));
      BgpActivePeerConfig neighbor = proc.getActiveNeighbors().get(neighborPrefix);
      assertThat(neighbor.getEnforceFirstAs(), equalTo(false));
      assertThat(neighbor.getGeneratedRoutes(), empty());
      Ipv4UnicastAddressFamily af = neighbor.getIpv4UnicastAddressFamily();
      assertThat(af, notNullValue());
      assertThat(af.getRouteReflectorClient(), equalTo(true));
    }
    {
      Prefix neighborPrefix = Prefix.parse("4.4.4.0/24");
      assertThat(proc.getPassiveNeighbors(), hasKey(neighborPrefix));
      BgpPassivePeerConfig neighbor = proc.getPassiveNeighbors().get(neighborPrefix);
      assertThat(neighbor.getRemoteAsns(), equalTo(LongSpace.of(4000L)));
      Ipv4UnicastAddressFamily af = neighbor.getIpv4UnicastAddressFamily();
      assertThat(af, notNullValue());
      assertTrue(af.getAddressFamilyCapabilities().getSendCommunity());
      assertThat(af.getRouteReflectorClient(), equalTo(false));
    }
    {
      Prefix neighborPrefix = Prefix.parse("5.5.5.5/32");
      // neighbor with missing remote-as is not converted
      assertThat(proc.getActiveNeighbors(), not(hasKey(neighborPrefix)));
    }
  }

  @Test
  public void testVrfExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_vrf");
    assertThat(config.getAristaBgp(), notNullValue());
    assertThat(
        config.getAristaBgp().getVrfs().keySet(), containsInAnyOrder(DEFAULT_VRF, "FOO", "BAR"));
    {
      AristaBgpVrf vrf = config.getAristaBgp().getDefaultVrf();
      assertThat(vrf.getBestpathAsPathMultipathRelax(), nullValue());
      assertThat(vrf.getBestpathTieBreaker(), nullValue());
      assertThat(vrf.getClusterId(), equalTo(Ip.parse("1.2.3.4")));
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("FOO");
      assertThat(vrf.getBestpathAsPathMultipathRelax(), equalTo(Boolean.TRUE));
      assertThat(vrf.getRouteDistinguisher(), equalTo(RouteDistinguisher.parse("123:123")));
      assertThat(vrf.getExportRouteTarget(), equalTo(ExtendedCommunity.target(1L, 1L)));
      assertThat(vrf.getImportRouteTarget(), equalTo(ExtendedCommunity.target(2L, 2L)));
      assertThat(vrf.getLocalAs(), equalTo(65000L));
      assertThat(vrf.getBestpathTieBreaker(), equalTo(AristaBgpBestpathTieBreaker.ROUTER_ID));
      assertThat(vrf.getClusterId(), nullValue());
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("BAR");
      assertThat(vrf.getBestpathAsPathMultipathRelax(), equalTo(Boolean.FALSE));
      assertThat(
          vrf.getBestpathTieBreaker(), equalTo(AristaBgpBestpathTieBreaker.CLUSTER_LIST_LENGTH));
      assertThat(vrf.getClusterId(), nullValue());
    }
  }

  @Test
  public void testVrfConversion() {
    Configuration c = parseConfig("arista_bgp_vrf");
    assertThat(c.getVrfs().keySet(), containsInAnyOrder(DEFAULT_VRF, "FOO", "BAR"));
    {
      BgpProcess proc = c.getDefaultVrf().getBgpProcess();
      assertThat(proc, notNullValue());
      assertThat(
          proc.getMultipathEquivalentAsPathMatchMode(),
          equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
      assertThat(proc.getTieBreaker(), equalTo(BgpTieBreaker.ROUTER_ID));
    }
    {
      BgpProcess proc = c.getVrfs().get("FOO").getBgpProcess();
      assertThat(proc, notNullValue());
      assertThat(
          proc.getMultipathEquivalentAsPathMatchMode(),
          equalTo(MultipathEquivalentAsPathMatchMode.PATH_LENGTH));
      assertThat(proc.getTieBreaker(), equalTo(BgpTieBreaker.ROUTER_ID));
    }
    {
      BgpProcess proc = c.getVrfs().get("BAR").getBgpProcess();
      assertThat(proc, notNullValue());
      assertThat(
          proc.getMultipathEquivalentAsPathMatchMode(),
          equalTo(MultipathEquivalentAsPathMatchMode.EXACT_PATH));
      assertThat(proc.getTieBreaker(), equalTo(BgpTieBreaker.CLUSTER_LIST_LENGTH));
    }
  }

  @Test
  public void testAddressFamilyExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_af");
    AristaBgpVrf vrf = config.getAristaBgp().getDefaultVrf();
    AristaBgpVrfIpv4UnicastAddressFamily ipv4af = vrf.getV4UnicastAf();
    AristaBgpVrfIpv6UnicastAddressFamily ipv6af = vrf.getV6UnicastAf();
    assertThat(ipv4af, notNullValue());
    AristaBgpVrfEvpnAddressFamily evpnaf = config.getAristaBgp().getDefaultVrf().getEvpnAf();
    assertThat(evpnaf, notNullValue());

    {
      assertThat(vrf.getV4neighbors(), hasKey(Ip.parse("1.1.1.1")));
      AristaBgpNeighborAddressFamily v4 = ipv4af.getNeighbor(Ip.parse("1.1.1.1"));
      assertThat(v4, notNullValue());
      assertThat(v4.getActivate(), equalTo(Boolean.TRUE));
      AristaBgpNeighborAddressFamily evpn = evpnaf.getNeighbor(Ip.parse("1.1.1.1"));
      assertThat(evpn, notNullValue());
      assertThat(evpn.getActivate(), equalTo(Boolean.TRUE));
    }
    {
      assertThat(vrf.getV4neighbors(), hasKey(Ip.parse("2.2.2.2")));
      AristaBgpNeighborAddressFamily v4 = ipv4af.getNeighbor(Ip.parse("2.2.2.2"));
      assertThat(v4, notNullValue());
      assertThat(v4.getActivate(), equalTo(Boolean.FALSE));
      AristaBgpNeighborAddressFamily evpn = evpnaf.getNeighbor(Ip.parse("2.2.2.2"));
      assertThat(evpn, nullValue());
    }
    {
      assertThat(config.getAristaBgp().getPeerGroups(), hasKey("PG"));
      AristaBgpNeighborAddressFamily v4 = ipv4af.getPeerGroup("PG");
      assertThat(v4, nullValue());
      AristaBgpNeighborAddressFamily evpn = evpnaf.getPeerGroup("PG");
      assertThat(evpn, notNullValue());
      assertThat(evpn.getActivate(), equalTo(Boolean.TRUE));
      AristaBgpNeighborAddressFamily v6 = ipv6af.getPeerGroup("PG");
      assertThat(v6, notNullValue());
      assertThat(v6.getActivate(), equalTo(Boolean.TRUE));
    }
    {
      Ip neighbor = Ip.parse("3.3.3.3");
      assertThat(vrf.getV4neighbors(), not(hasKey(neighbor)));
      AristaBgpVrf vrf1 = config.getAristaBgp().getVrfs().get("vrf1");
      assertThat(vrf1.getV4neighbors(), hasKey(neighbor));
      assertTrue(vrf1.getV4UnicastAf().getNeighbor(neighbor).getActivate());
      assertThat(vrf1.getV4UnicastAf().getNeighbor(neighbor).getRouteMapOut(), equalTo("RM1"));
    }
  }

  @Test
  public void testNetworkExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_network");
    {
      Prefix prefix = Prefix.parse("1.1.1.0/24");
      AristaBgpNetworkConfiguration network =
          config.getAristaBgp().getDefaultVrf().getV4UnicastAf().getNetworks().get(prefix);
      assertThat(network, notNullValue());
      assertThat(network.getRouteMap(), nullValue());
    }
    {
      Prefix prefix = Prefix.parse("1.1.2.0/24");
      AristaBgpNetworkConfiguration network =
          config.getAristaBgp().getDefaultVrf().getV4UnicastAf().getNetworks().get(prefix);
      assertThat(network, notNullValue());
      assertThat(network.getRouteMap(), nullValue());
    }
    {
      Prefix prefix = Prefix.parse("1.1.3.0/24");
      AristaBgpVrf vrf = config.getAristaBgp().getDefaultVrf();
      AristaBgpVrfIpv4UnicastAddressFamily v4UnicastAf = vrf.getV4UnicastAf();
      AristaBgpNetworkConfiguration network = v4UnicastAf.getNetworks().get(prefix);
      assertThat(network, notNullValue());
      assertThat(network.getRouteMap(), equalTo("RM"));
      // Ensure parser didn't go into "router bgp" context and stayed in "address family ipv4"
      assertNull(vrf.getNextHopUnchanged());
      assertTrue(v4UnicastAf.getNextHopUnchanged());
    }
  }

  @Test
  public void testVxlanExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_vxlan");
    assertThat(config.getEosVxlan().getArpReplyRelay(), equalTo(true));
    assertThat(config.getEosVxlan().getVrfToVni(), hasEntry("TENANT", 10000));
    assertThat(config.getEosVxlan().getVlanVnis(), hasEntry(1, 10001));
  }

  @Test
  public void testVxlanConversion() {
    Configuration config = parseConfig("arista_vxlan");
    {
      Layer3Vni vniSettings = config.getVrfs().get("TENANT").getLayer3Vnis().get(10000);
      assertThat(
          vniSettings.getBumTransportMethod(), equalTo(BumTransportMethod.UNICAST_FLOOD_GROUP));
      assertThat(vniSettings.getBumTransportIps(), empty());
    }
    {
      Layer2Vni vniSettings = config.getVrfs().get("VRF_1").getLayer2Vnis().get(10001);
      assertThat(
          vniSettings.getBumTransportMethod(), equalTo(BumTransportMethod.UNICAST_FLOOD_GROUP));
      assertThat(vniSettings.getBumTransportIps(), empty());
    }
    {
      Layer2Vni vniSettings = config.getVrfs().get("VRF_2").getLayer2Vnis().get(10002);
      assertThat(
          vniSettings.getBumTransportMethod(), equalTo(BumTransportMethod.UNICAST_FLOOD_GROUP));
      assertThat(vniSettings.getBumTransportIps(), empty());
    }
  }

  @Test
  public void testRedistributeExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_redistribute");
    {
      Map<AristaRedistributeType, AristaBgpRedistributionPolicy> redistributionPolicies =
          config.getAristaBgp().getDefaultVrf().getRedistributionPolicies();
      assertThat(
          redistributionPolicies,
          hasEntry(
              AristaRedistributeType.CONNECTED,
              new AristaBgpRedistributionPolicy(AristaRedistributeType.CONNECTED, "RM")));
    }
    {
      Map<AristaRedistributeType, AristaBgpRedistributionPolicy> redistributionPolicies =
          config.getAristaBgp().getVrfs().get("tenant").getRedistributionPolicies();
      assertThat(
          redistributionPolicies,
          hasEntry(
              AristaRedistributeType.STATIC,
              new AristaBgpRedistributionPolicy(AristaRedistributeType.STATIC, "RM2")));
    }
  }

  @Test
  public void testRedistributeOspfExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_redistribute_ospf");
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf1");
      assertThat(
          vrf.getRedistributionPolicies().get(OSPF),
          equalTo(new AristaBgpRedistributionPolicy(OSPF, "ALLOW_10")));
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf2");
      assertThat(
          vrf.getRedistributionPolicies().get(OSPF_INTERNAL),
          equalTo(new AristaBgpRedistributionPolicy(OSPF_INTERNAL, "ALLOW_10")));
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf3");
      assertThat(
          vrf.getRedistributionPolicies().get(OSPF_EXTERNAL),
          equalTo(new AristaBgpRedistributionPolicy(OSPF_EXTERNAL, "ALLOW_10")));
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf4");
      assertThat(
          vrf.getRedistributionPolicies().get(OSPF_NSSA_EXTERNAL),
          equalTo(new AristaBgpRedistributionPolicy(OSPF_NSSA_EXTERNAL, "ALLOW_10")));
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf5");
      assertThat(
          vrf.getRedistributionPolicies().get(OSPF_NSSA_EXTERNAL_TYPE_1),
          equalTo(new AristaBgpRedistributionPolicy(OSPF_NSSA_EXTERNAL_TYPE_1, "ALLOW_10")));
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf6");
      assertThat(
          vrf.getRedistributionPolicies().get(OSPF_NSSA_EXTERNAL_TYPE_2),
          equalTo(new AristaBgpRedistributionPolicy(OSPF_NSSA_EXTERNAL_TYPE_2, "ALLOW_10")));
    }
  }

  @Test
  public void testRedistributeOspfConversion() {
    Configuration config = parseConfig("arista_bgp_redistribute_ospf");
    Prefix prefix = Prefix.parse("10.1.1.0/24");
    OspfIntraAreaRoute intra = OspfIntraAreaRoute.builder().setNetwork(prefix).build();
    OspfInterAreaRoute inter = OspfInterAreaRoute.builder().setNetwork(prefix).setArea(0).build();
    OspfExternalType1Route ext1 =
        (OspfExternalType1Route)
            OspfExternalType1Route.builder()
                .setNetwork(prefix)
                .setLsaMetric(0)
                .setArea(0)
                .setCostToAdvertiser(1)
                .setAdvertiser("node")
                .build();
    OspfExternalType2Route ext2 =
        (OspfExternalType2Route)
            OspfExternalType2Route.builder()
                .setNetwork(prefix)
                .setLsaMetric(0)
                .setArea(0)
                .setAdvertiser("node")
                .setCostToAdvertiser(1)
                .build();
    Builder builder = Bgpv4Route.builder();
    {
      String policyName =
          config
              .getVrfs()
              .get("vrf1")
              .getBgpProcess()
              .getActiveNeighbors()
              .get(Prefix.parse("1.1.1.1/32"))
              .getIpv4UnicastAddressFamily()
              .getExportPolicy();
      RoutingPolicy policy = config.getRoutingPolicies().get(policyName);
      assertTrue(policy.process(intra, builder, Direction.OUT));
      assertTrue(policy.process(inter, builder, Direction.OUT));
      assertTrue(policy.process(ext1, builder, Direction.OUT));
      assertTrue(policy.process(ext2, builder, Direction.OUT));
    }
    {
      String policyName =
          config
              .getVrfs()
              .get("vrf2")
              .getBgpProcess()
              .getActiveNeighbors()
              .get(Prefix.parse("2.2.2.2/32"))
              .getIpv4UnicastAddressFamily()
              .getExportPolicy();
      RoutingPolicy policy = config.getRoutingPolicies().get(policyName);
      assertTrue(policy.process(intra, builder, Direction.OUT));
      assertTrue(policy.process(inter, builder, Direction.OUT));
      assertFalse(policy.process(ext1, builder, Direction.OUT));
      assertFalse(policy.process(ext2, builder, Direction.OUT));
    }
    {
      String policyName =
          config
              .getVrfs()
              .get("vrf3")
              .getBgpProcess()
              .getActiveNeighbors()
              .get(Prefix.parse("3.3.3.3/32"))
              .getIpv4UnicastAddressFamily()
              .getExportPolicy();
      RoutingPolicy policy = config.getRoutingPolicies().get(policyName);
      assertFalse(policy.process(intra, builder, Direction.OUT));
      assertFalse(policy.process(inter, builder, Direction.OUT));
      assertTrue(policy.process(ext1, builder, Direction.OUT));
      assertTrue(policy.process(ext2, builder, Direction.OUT));
    }
    // TODO: support for nssa-external variants
  }

  @Test
  public void testInterfaceConversion() {
    Configuration c = parseConfig("arista_interface");
    assertThat(
        c,
        hasInterface(
            "Ethernet1", allOf(hasVrf(hasName(equalTo("VRF_1"))), hasEncapsulationVlan(7))));
    assertThat(c, hasInterface("Ethernet2", hasVrf(hasName(equalTo("VRF_2")))));
    assertThat(c, hasInterface("UnconnectedEthernet5"));
  }

  @Test
  public void testMulticastParsing() {
    // doesn't crash.
    parseVendorConfig("arista_multicast");
  }

  @Test
  public void testVrrpConversion() {
    Configuration c = parseConfig("arista_vrrp");
    assertThat(c.getAllInterfaces(), hasKey("Vlan20"));
    Interface i = c.getAllInterfaces().get("Vlan20");
    assertThat(i.getVrrpGroups(), hasKey(1));
    VrrpGroup group = i.getVrrpGroups().get(1);
    assertThat(group.getVirtualAddress(), equalTo(ConcreteInterfaceAddress.parse("1.2.3.4/24")));
    assertThat(group.getPriority(), equalTo(200));
  }

  @Test
  public void testVrrpExtraction() {
    AristaConfiguration c = parseVendorConfig("arista_vrrp");
    assertThat(c.getInterfaces(), hasKey("Vlan20"));
    assertThat(c.getVrrpGroups(), hasKey("Vlan20"));
    VrrpInterface vrrpI = c.getVrrpGroups().get("Vlan20");
    assertThat(vrrpI.getVrrpGroups(), hasKey(1));
    org.batfish.representation.arista.VrrpGroup g = vrrpI.getVrrpGroups().get(1);
    assertThat(g.getVirtualAddress(), equalTo(Ip.parse("1.2.3.4")));
    assertThat(g.getPriority(), equalTo(200));
  }

  @Test
  public void testEvpnConversion() {
    Configuration c = parseConfig("arista_evpn");
    Ip[] neighborIPs = {Ip.parse("192.168.255.1"), Ip.parse("192.168.255.2")};
    for (Ip ip : neighborIPs) {

      BgpActivePeerConfig neighbor =
          c.getDefaultVrf().getBgpProcess().getActiveNeighbors().get(ip.toPrefix());
      assertThat(neighbor.getEvpnAddressFamily(), notNullValue());
      assertTrue(
          neighbor.getEvpnAddressFamily().getAddressFamilyCapabilities().getAllowRemoteAsOut());
      assertThat(
          neighbor.getEvpnAddressFamily().getL2VNIs(),
          equalTo(
              ImmutableSet.of(
                  Layer2VniConfig.builder()
                      .setVrf("Tenant_A_OPZone")
                      .setVni(10110)
                      .setRouteDistinguisher(RouteDistinguisher.parse("192.168.255.3:10110"))
                      .setImportRouteTarget(ExtendedCommunity.target(10110, 10110).matchString())
                      .setRouteTarget(ExtendedCommunity.target(10110, 10110))
                      .build(),
                  Layer2VniConfig.builder()
                      .setVrf("Tenant_B_OPZone")
                      .setVni(10210)
                      .setRouteDistinguisher(RouteDistinguisher.parse("192.168.255.3:10210"))
                      .setImportRouteTarget(ExtendedCommunity.target(10210, 10210).matchString())
                      .setRouteTarget(ExtendedCommunity.target(10210, 10210))
                      .build())));

      assertThat(
          neighbor.getEvpnAddressFamily().getL3VNIs(),
          equalTo(
              ImmutableSet.of(
                  Layer3VniConfig.builder()
                      .setVrf("Tenant_A_OPZone")
                      .setVni(50101)
                      .setRouteDistinguisher(RouteDistinguisher.parse("192.168.255.3:50101"))
                      .setImportRouteTarget(ExtendedCommunity.target(50101, 50101).matchString())
                      .setRouteTarget(ExtendedCommunity.target(50101, 50101))
                      .setAdvertiseV4Unicast(true)
                      .build(),
                  Layer3VniConfig.builder()
                      .setVrf("Tenant_B_OPZone")
                      .setVni(50201)
                      .setRouteDistinguisher(RouteDistinguisher.parse("192.168.255.3:50201"))
                      .setImportRouteTarget(ExtendedCommunity.target(50201, 50201).matchString())
                      .setRouteTarget(ExtendedCommunity.target(50201, 50201))
                      .setAdvertiseV4Unicast(true)
                      .build())));
    }
  }

  /**
   * Ensure that when L2 VNIs are present and no bgp VRFs are defined, we still make Bgp procesess
   * for non-default VRF to prevent crashing the dataplane computation.
   */
  @Test
  public void testEvpnConversionL2VnisOnly() {
    Configuration c = parseConfig("arista_evpn_l2_vni_only");
    assertThat(c, ConfigurationMatchers.hasVrf("vrf1", hasBgpProcess(notNullValue())));
    assertThat(c.getDefaultVrf().getLayer2Vnis(), hasKey(10030));
  }

  @Test
  public void testBgpSendCommunityExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_send_community");
    AristaBgpVrf vrf = config.getAristaBgp().getDefaultVrf();
    {
      Ip ip = Ip.parse("1.1.1.1");
      assertThat(vrf.getV4neighbors(), hasKey(ip));
      assertTrue(vrf.getV4neighbors().get(ip).getSendCommunity());
      assertTrue(vrf.getV4neighbors().get(ip).getSendExtendedCommunity());
    }
    {
      Ip ip = Ip.parse("1.1.1.2");
      assertThat(vrf.getV4neighbors(), hasKey(ip));
      assertTrue(vrf.getV4neighbors().get(ip).getSendCommunity());
      assertNull(vrf.getV4neighbors().get(ip).getSendExtendedCommunity());
    }
    {
      Ip ip = Ip.parse("1.1.1.3");
      assertThat(vrf.getV4neighbors(), hasKey(ip));
      assertNull(vrf.getV4neighbors().get(ip).getSendCommunity());
      assertTrue(vrf.getV4neighbors().get(ip).getSendExtendedCommunity());
    }
    {
      Ip ip = Ip.parse("1.1.1.4");
      assertThat(vrf.getV4neighbors(), hasKey(ip));
      assertTrue(vrf.getV4neighbors().get(ip).getSendCommunity());
      assertTrue(vrf.getV4neighbors().get(ip).getSendExtendedCommunity());
    }
    {
      Ip ip = Ip.parse("1.1.1.5");
      assertThat(vrf.getV4neighbors(), hasKey(ip));
      assertTrue(vrf.getV4neighbors().get(ip).getSendCommunity());
      assertNull(vrf.getV4neighbors().get(ip).getSendExtendedCommunity());
    }
    {
      Ip ip = Ip.parse("1.1.1.6");
      assertThat(vrf.getV4neighbors(), hasKey(ip));
      assertNull(vrf.getV4neighbors().get(ip).getSendCommunity());
      assertTrue(vrf.getV4neighbors().get(ip).getSendExtendedCommunity());
    }
    {
      Ip ip = Ip.parse("1.1.1.7");
      assertThat(vrf.getV4neighbors(), hasKey(ip));
      assertTrue(vrf.getV4neighbors().get(ip).getSendCommunity());
      assertTrue(vrf.getV4neighbors().get(ip).getSendExtendedCommunity());
    }
    {
      String peerGroupName = "PG";
      AristaBgpPeerGroupNeighbor group = config.getAristaBgp().getPeerGroup(peerGroupName);
      assertThat(group, notNullValue());
      assertTrue(group.getSendCommunity());
      assertTrue(group.getSendExtendedCommunity());
    }
    {
      Ip ip = Ip.parse("1.1.1.8");
      assertThat(vrf.getV4neighbors(), hasKey(ip));
      assertNull(vrf.getV4neighbors().get(ip).getSendCommunity());
      assertNull(vrf.getV4neighbors().get(ip).getSendExtendedCommunity());
    }
  }

  @Test
  public void testBgpSendCommunityConversion() {
    Configuration config = parseConfig("arista_bgp_send_community");
    BgpProcess proc = config.getDefaultVrf().getBgpProcess();
    {
      Prefix prefix = Prefix.parse("1.1.1.1/32");
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(hasAddressFamilyCapabilites(hasSendCommunity(true)))));
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(
                  hasAddressFamilyCapabilites(hasSendExtendedCommunity(true)))));
    }
    {
      Prefix prefix = Prefix.parse("1.1.1.2/32");
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(hasAddressFamilyCapabilites(hasSendCommunity(true)))));
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(
                  hasAddressFamilyCapabilites(hasSendExtendedCommunity(false)))));
    }
    {
      Prefix prefix = Prefix.parse("1.1.1.3/32");
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(hasAddressFamilyCapabilites(hasSendCommunity(false)))));
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(
                  hasAddressFamilyCapabilites(hasSendExtendedCommunity(true)))));
    }
    {
      Prefix prefix = Prefix.parse("1.1.1.4/32");
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(hasAddressFamilyCapabilites(hasSendCommunity(true)))));
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(
                  hasAddressFamilyCapabilites(hasSendExtendedCommunity(true)))));
    }
    {
      Prefix prefix = Prefix.parse("1.1.1.5/32");
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(hasAddressFamilyCapabilites(hasSendCommunity(true)))));
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(
                  hasAddressFamilyCapabilites(hasSendExtendedCommunity(false)))));
    }
    {
      Prefix prefix = Prefix.parse("1.1.1.6/32");
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(hasAddressFamilyCapabilites(hasSendCommunity(false)))));
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(
                  hasAddressFamilyCapabilites(hasSendExtendedCommunity(true)))));
    }
    {
      Prefix prefix = Prefix.parse("1.1.1.7/32");
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(hasAddressFamilyCapabilites(hasSendCommunity(true)))));
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(
                  hasAddressFamilyCapabilites(hasSendExtendedCommunity(true)))));
    }
    {
      // honor inheritance
      Prefix prefix = Prefix.parse("1.1.1.8/32");
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(hasAddressFamilyCapabilites(hasSendCommunity(true)))));
      assertThat(
          proc,
          hasActiveNeighbor(
              prefix,
              hasIpv4UnicastAddressFamily(
                  hasAddressFamilyCapabilites(hasSendExtendedCommunity(true)))));
    }
  }

  @Test
  public void testAllowasInExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_allowas_in");
    assertThat(config.getAristaBgp().getDefaultVrf().getAllowAsIn(), equalTo(2));
  }

  @Test
  public void testAllowasInConversion() {
    Configuration c = parseConfig("arista_bgp_allowas_in");
    {
      BgpActivePeerConfig peerConfig =
          c.getDefaultVrf().getBgpProcess().getActiveNeighbors().get(Prefix.parse("1.1.1.1/32"));
      assertTrue(
          peerConfig
              .getIpv4UnicastAddressFamily()
              .getAddressFamilyCapabilities()
              .getAllowLocalAsIn());
    }
    {
      BgpActivePeerConfig peerConfig =
          c.getDefaultVrf().getBgpProcess().getActiveNeighbors().get(Prefix.parse("2.2.2.2/32"));
      assertTrue(
          peerConfig
              .getIpv4UnicastAddressFamily()
              .getAddressFamilyCapabilities()
              .getAllowLocalAsIn());
      assertTrue(
          peerConfig.getEvpnAddressFamily().getAddressFamilyCapabilities().getAllowLocalAsIn());
    }
  }

  @Test
  public void testParseCvx() {
    parseVendorConfig("arista_cvx");
    // don't crash.
  }

  @Test
  public void testNatParse() {
    parseVendorConfig("arista_nat");
    // don't crash.
  }

  @Test
  public void testNatConvert() {
    parseConfig("arista_nat");
    // don't crash.
  }

  @Test
  public void testParseAclShowRunAll() {
    Configuration c = parseConfig("arista_acl_show_run_all");
    // Tests that the ACL parses.
    assertThat(c, hasIpAccessList("SOME_ACL", hasLines(hasSize(1))));
  }

  @Test
  public void testParseBgpShowRunAll() {
    AristaConfiguration c = parseVendorConfig("arista_bgp_show_run_all");
    // Test relies on route-maps configured as the last line of specific address families.
    assertThat(c.getAristaBgp().getPeerGroups().keySet(), containsInAnyOrder("SOME_GROUP"));
    AristaBgpVrf defaultVrf = c.getAristaBgp().getDefaultVrf();
    Ip neighborIp = Ip.parse("192.0.2.7");
    assertThat(defaultVrf.getV4neighbors().keySet(), contains(neighborIp));
    {
      AristaBgpNeighborAddressFamily evpn = defaultVrf.getEvpnAf().getNeighbor(neighborIp);
      assertThat(evpn, notNullValue());
      assertThat(evpn.getRouteMapIn(), equalTo("EVPN_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv4u = defaultVrf.getV4UnicastAf().getNeighbor(neighborIp);
      assertThat(ipv4u, notNullValue());
      assertThat(ipv4u.getRouteMapIn(), equalTo("IPV4_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6u = defaultVrf.getV6UnicastAf().getNeighbor(neighborIp);
      assertThat(ipv6u, notNullValue());
      assertThat(ipv6u.getRouteMapIn(), equalTo("IPV6_IN"));
    }
  }

  @Test
  public void testParseBgpShowRunAll2() {
    AristaConfiguration c = parseVendorConfig("arista_bgp_show_run_all_2");
    // Test relies on route-maps configured as the last line of specific address families.
    assertThat(
        c.getAristaBgp().getPeerGroups().keySet(), containsInAnyOrder("SOME_GROUP", "OTHER_GROUP"));
    AristaBgpVrf defaultVrf = c.getAristaBgp().getDefaultVrf();
    Ip neighborIp = Ip.parse("192.0.2.7");
    assertThat(defaultVrf.getV4neighbors().keySet(), contains(neighborIp));
    {
      AristaBgpNeighborAddressFamily evpn = defaultVrf.getEvpnAf().getNeighbor(neighborIp);
      assertThat(evpn, notNullValue());
      assertThat(evpn.getRouteMapIn(), equalTo("EVPN_IN"));
    }
    {
      AristaBgpNeighborAddressFamily fs4 = defaultVrf.getFlowSpecV4Af().getNeighbor(neighborIp);
      assertThat(fs4, notNullValue());
      assertThat(fs4.getActivate(), equalTo(Boolean.TRUE));
    }
    {
      AristaBgpNeighborAddressFamily fs6 = defaultVrf.getFlowSpecV6Af().getNeighbor(neighborIp);
      assertThat(fs6, notNullValue());
      assertThat(fs6.getActivate(), equalTo(Boolean.TRUE));
    }
    {
      AristaBgpNeighborAddressFamily ipv4u = defaultVrf.getV4UnicastAf().getNeighbor(neighborIp);
      assertThat(ipv4u, notNullValue());
      assertThat(ipv4u.getRouteMapIn(), equalTo("IPV4_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv4m = defaultVrf.getV4MulticastAf().getNeighbor(neighborIp);
      assertThat(ipv4m, notNullValue());
      assertThat(ipv4m.getRouteMapIn(), equalTo("IPV4MC_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv4lu =
          defaultVrf.getV4LabeledUnicastAf().getNeighbor(neighborIp);
      assertThat(ipv4lu, notNullValue());
      assertThat(ipv4lu.getRouteMapIn(), equalTo("IPV4LU_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6lu =
          defaultVrf.getV6LabeledUnicastAf().getNeighbor(neighborIp);
      assertThat(ipv6lu, notNullValue());
      assertThat(ipv6lu.getRouteMapIn(), equalTo("IPV6LU_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6u = defaultVrf.getV6UnicastAf().getNeighbor(neighborIp);
      assertThat(ipv6u, notNullValue());
      assertThat(ipv6u.getRouteMapIn(), equalTo("IPV6_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6m = defaultVrf.getV6MulticastAf().getNeighbor(neighborIp);
      assertThat(ipv6m, notNullValue());
      assertThat(ipv6m.getRouteMapIn(), equalTo("IPV6MC_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv4sr = defaultVrf.getV4SrTeAf().getNeighbor(neighborIp);
      assertThat(ipv4sr, notNullValue());
      assertThat(ipv4sr.getRouteMapIn(), equalTo("IPV4SRTE_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6sr = defaultVrf.getV6SrTeAf().getNeighbor(neighborIp);
      assertThat(ipv6sr, notNullValue());
      assertThat(ipv6sr.getRouteMapIn(), equalTo("IPV6SRTE_IN"));
    }
    {
      AristaBgpNeighborAddressFamily vpn4 = defaultVrf.getVpnV4Af().getNeighbor(neighborIp);
      assertThat(vpn4, notNullValue());
      assertThat(vpn4.getRouteMapIn(), equalTo("VPN4_IN"));
    }
    {
      AristaBgpNeighborAddressFamily vpn6 = defaultVrf.getVpnV6Af().getNeighbor(neighborIp);
      assertThat(vpn6, notNullValue());
      assertThat(vpn6.getRouteMapIn(), equalTo("VPN6_IN"));
    }
  }

  @Test
  public void testParseBgpShowRunAll3() {
    AristaConfiguration c = parseVendorConfig("arista_bgp_show_run_all_3");
    // Test relies on route-maps configured as the last line of specific address families.
    assertThat(c.getAristaBgp().getVrfs().keySet(), containsInAnyOrder("default", "a"));
    assertThat(c.getAristaBgp().getPeerGroups().keySet(), containsInAnyOrder("SOME_GROUP"));
    Ip neighborIp = Ip.parse("192.0.2.7");
    AristaBgpVrf defaultVrf = c.getAristaBgp().getDefaultVrf();
    assertThat(defaultVrf.getV4neighbors().keySet(), contains(neighborIp));
    {
      AristaBgpNeighborAddressFamily evpn = defaultVrf.getEvpnAf().getNeighbor(neighborIp);
      assertThat(evpn, notNullValue());
      assertThat(evpn.getRouteMapIn(), equalTo("EVPN_IN"));
    }
    {
      AristaBgpNeighborAddressFamily fs4 = defaultVrf.getFlowSpecV4Af().getNeighbor(neighborIp);
      assertThat(fs4, notNullValue());
      assertThat(fs4.getActivate(), equalTo(Boolean.TRUE));
    }
    {
      AristaBgpNeighborAddressFamily fs6 = defaultVrf.getFlowSpecV6Af().getNeighbor(neighborIp);
      assertThat(fs6, notNullValue());
      assertThat(fs6.getActivate(), equalTo(Boolean.TRUE));
    }
    {
      AristaBgpNeighborAddressFamily ipv4u = defaultVrf.getV4UnicastAf().getNeighbor(neighborIp);
      assertThat(ipv4u, notNullValue());
      assertThat(ipv4u.getRouteMapIn(), equalTo("IPV4_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv4m = defaultVrf.getV4MulticastAf().getNeighbor(neighborIp);
      assertThat(ipv4m, notNullValue());
      assertThat(ipv4m.getRouteMapIn(), equalTo("IPV4MC_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv4lu =
          defaultVrf.getV4LabeledUnicastAf().getNeighbor(neighborIp);
      assertThat(ipv4lu, notNullValue());
      assertThat(ipv4lu.getRouteMapIn(), equalTo("IPV4LU_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6lu =
          defaultVrf.getV6LabeledUnicastAf().getNeighbor(neighborIp);
      assertThat(ipv6lu, notNullValue());
      assertThat(ipv6lu.getRouteMapIn(), equalTo("IPV6LU_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6u = defaultVrf.getV6UnicastAf().getNeighbor(neighborIp);
      assertThat(ipv6u, notNullValue());
      assertThat(ipv6u.getRouteMapIn(), equalTo("IPV6_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6m = defaultVrf.getV6MulticastAf().getNeighbor(neighborIp);
      assertThat(ipv6m, notNullValue());
      assertThat(ipv6m.getRouteMapIn(), equalTo("IPV6MC_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv4sr = defaultVrf.getV4SrTeAf().getNeighbor(neighborIp);
      assertThat(ipv4sr, notNullValue());
      assertThat(ipv4sr.getRouteMapIn(), equalTo("IPV4SRTE_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6sr = defaultVrf.getV6SrTeAf().getNeighbor(neighborIp);
      assertThat(ipv6sr, notNullValue());
      assertThat(ipv6sr.getRouteMapIn(), equalTo("IPV6SRTE_IN"));
    }
    {
      AristaBgpNeighborAddressFamily vpn4 = defaultVrf.getVpnV4Af().getNeighbor(neighborIp);
      assertThat(vpn4, notNullValue());
      assertThat(vpn4.getRouteMapIn(), equalTo("VPN4_IN"));
    }
    {
      AristaBgpNeighborAddressFamily vpn6 = defaultVrf.getVpnV6Af().getNeighbor(neighborIp);
      assertThat(vpn6, notNullValue());
      assertThat(vpn6.getRouteMapIn(), equalTo("VPN6_IN"));
    }
    /// vrf
    AristaBgpVrf vrfA = c.getAristaBgp().getVrfs().get("a");
    Ip neighborIpA = Ip.parse("192.0.2.8");
    assertThat(vrfA.getV4neighbors().keySet(), contains(neighborIpA));
    {
      AristaBgpNeighborAddressFamily ipv4u = vrfA.getV4UnicastAf().getNeighbor(neighborIpA);
      assertThat(ipv4u, notNullValue());
      assertThat(ipv4u.getRouteMapIn(), equalTo("A-IPV4_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv4m = vrfA.getV4MulticastAf().getNeighbor(neighborIpA);
      assertThat(ipv4m, notNullValue());
      assertThat(ipv4m.getRouteMapIn(), equalTo("A-IPV4MC_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6u = vrfA.getV6UnicastAf().getNeighbor(neighborIpA);
      assertThat(ipv6u, notNullValue());
      assertThat(ipv6u.getRouteMapIn(), equalTo("A-IPV6_IN"));
    }
    {
      AristaBgpNeighborAddressFamily ipv6m = vrfA.getV6MulticastAf().getNeighbor(neighborIpA);
      assertThat(ipv6m, notNullValue());
      assertThat(ipv6m.getRouteMapIn(), equalTo("A-IPV6MC_IN"));
    }
  }

  @Test
  public void testParseInterfaceShowRunAll() {
    Configuration c = parseConfig("arista_interface_show_run_all");
    // Test relies on the last line in each interface being this description.
    assertThat(c, hasInterface("Ethernet1/1", hasDescription("Made it to the end of Ethernet1/1")));
    assertThat(c, hasInterface("Ethernet1/2", hasDescription("Made it to the end of Ethernet1/2")));
    assertThat(c, hasInterface("Loopback0", hasDescription("Made it to the end of Loopback0")));
    assertThat(c, hasInterface("Management1", hasDescription("Made it to the end of Management1")));
  }

  @Test
  public void testParseInterfaceShowRunAll2() {
    Configuration c = parseConfig("arista_interface_show_run_all_2");
    // Test relies on the last line in each interface being this description.
    assertThat(c, hasInterface("Ethernet1/1", hasDescription("Made it to the end of Ethernet1/1")));
    assertThat(c, hasInterface("Ethernet1/3", hasDescription("Made it to the end of Ethernet1/3")));
    assertThat(c, hasInterface("Loopback0", hasDescription("Made it to the end of Loopback0")));
    assertThat(c, hasInterface("Management1", hasDescription("Made it to the end of Management1")));
  }

  @Test
  public void testParseInterfaceShowRunAll3() {
    Configuration c = parseConfig("arista_interface_show_run_all_3");
    // Test relies on the last line in each interface being this description.
    assertThat(c, hasInterface("Ethernet1/1", hasDescription("Made it to the end of Ethernet1/1")));
    assertThat(c, hasInterface("Ethernet1/3", hasDescription("Made it to the end of Ethernet1/3")));
    assertThat(c, hasInterface("Loopback0", hasDescription("Made it to the end of Loopback0")));
    assertThat(c, hasInterface("Management1", hasDescription("Made it to the end of Management1")));
  }

  @Test
  public void testParseLoggingShowRunAll() {
    Configuration c = parseConfig("arista_logging_show_run_all");
    assertThat(
        c.getLoggingServers(),
        containsInAnyOrder("1.2.3.4", "1.2.3.5", "1.2.3.6", "some_host.company.com"));
    assertThat(c.getLoggingSourceInterface(), equalTo("Loopback0"));
  }

  @Test
  public void testEnforceFirstAsExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_enforce_first_as");
    assertThat(config.getAristaBgp().getDefaultVrf().getEnforceFirstAs(), equalTo(Boolean.TRUE));
  }

  @Test
  public void testEnforceFirstAsConversion() {
    Configuration c = parseConfig("arista_bgp_enforce_first_as");
    BgpActivePeerConfig peerConfig =
        c.getDefaultVrf().getBgpProcess().getActiveNeighbors().get(Prefix.parse("1.1.1.1/32"));
    assertTrue(peerConfig.getEnforceFirstAs());
  }

  @Test
  public void testNeighborPrefixListExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_neighbor_prefix_list");
    assertThat(
        config
            .getAristaBgp()
            .getDefaultVrf()
            .getV4neighbors()
            .get(Ip.parse("1.1.1.1"))
            .getGenericAddressFamily()
            .getPrefixListIn(),
        equalTo("PREFIX_LIST_IN"));
    assertThat(
        config
            .getAristaBgp()
            .getDefaultVrf()
            .getV4neighbors()
            .get(Ip.parse("1.1.1.1"))
            .getGenericAddressFamily()
            .getPrefixListOut(),
        equalTo("PREFIX_LIST_OUT"));
  }

  @Test
  public void testNeighborPrefixListConversion() {
    Configuration c = parseConfig("arista_bgp_neighbor_prefix_list");

    RoutingPolicy exportPolicy =
        c.getRoutingPolicies()
            .get(
                c.getDefaultVrf()
                    .getBgpProcess()
                    .getActiveNeighbors()
                    .get(Prefix.parse("1.1.1.1/32"))
                    .getIpv4UnicastAddressFamily()
                    .getExportPolicy());
    RoutingPolicy importPolicy =
        c.getRoutingPolicies()
            .get(
                c.getDefaultVrf()
                    .getBgpProcess()
                    .getActiveNeighbors()
                    .get(Prefix.parse("1.1.1.1/32"))
                    .getIpv4UnicastAddressFamily()
                    .getImportPolicy());

    // assert on the behavior of routing policies
    Builder originalRoute =
        Bgpv4Route.builder()
            .setNextHopIp(Ip.ZERO)
            .setAdmin(1)
            .setOriginatorIp(Ip.parse("9.8.7.6"))
            .setOriginType(OriginType.EGP)
            .setProtocol(RoutingProtocol.BGP);
    Bgpv4Route.Builder outputRouteBuilder =
        Bgpv4Route.builder().setNextHopIp(UNSET_ROUTE_NEXT_HOP_IP);

    Ip sessionPropsHeadIp = Ip.parse("1.1.1.1");
    BgpSessionProperties.Builder sessionProps =
        BgpSessionProperties.builder()
            .setHeadAs(1L)
            .setTailAs(1L)
            .setHeadIp(sessionPropsHeadIp)
            .setTailIp(Ip.parse("2.2.2.2"));
    BgpSessionProperties session = sessionProps.setSessionType(SessionType.IBGP).build();

    Prefix allowedIn = Prefix.parse("10.1.2.0/24");
    Prefix allowedOut = Prefix.parse("10.7.8.0/24");
    Prefix deniedBoth = Prefix.parse("10.3.4.0/24");

    assertTrue(
        importPolicy.processBgpRoute(
            originalRoute.setNetwork(allowedIn).build(),
            outputRouteBuilder,
            session,
            Direction.IN));
    assertFalse(
        importPolicy.processBgpRoute(
            originalRoute.setNetwork(deniedBoth).build(),
            outputRouteBuilder,
            session,
            Direction.IN));
    assertTrue(
        exportPolicy.processBgpRoute(
            originalRoute.setNetwork(allowedOut).build(),
            outputRouteBuilder,
            session,
            Direction.OUT));
    assertFalse(
        exportPolicy.processBgpRoute(
            originalRoute.setNetwork(deniedBoth).build(),
            outputRouteBuilder,
            session,
            Direction.OUT));
  }

  @Test
  public void testNextHopUnchangedExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_nexthop_unchanged");
    {
      AristaBgpVrf vrf = config.getAristaBgp().getDefaultVrf();
      assertTrue(vrf.getV4neighbors().get(Ip.parse("9.9.9.9")).getNextHopUnchanged());
      assertNull(vrf.getV4neighbors().get(Ip.parse("8.8.8.8")).getNextHopUnchanged());
      assertTrue(vrf.getEvpnAf().getNextHopUnchanged());
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf2");
      assertTrue(vrf.getNextHopUnchanged());
      assertNull(vrf.getV4neighbors().get(Ip.parse("2.2.2.2")).getNextHopUnchanged());
      assertNull(vrf.getV4neighbors().get(Ip.parse("2.2.2.22")).getNextHopUnchanged());
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf3");
      assertNull(vrf.getNextHopUnchanged());
      assertNull(vrf.getV4neighbors().get(Ip.parse("3.3.3.3")).getNextHopUnchanged());
      assertNull(vrf.getV4neighbors().get(Ip.parse("3.3.3.33")).getNextHopUnchanged());
      assertTrue(vrf.getV4UnicastAf().getNeighbor(Ip.parse("3.3.3.3")).getNextHopUnchanged());
      assertNull(vrf.getV4UnicastAf().getNeighbor(Ip.parse("3.3.3.33")).getNextHopUnchanged());
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf4");
      assertTrue(vrf.getV4UnicastAf().getNextHopUnchanged());
    }
  }

  @Test
  public void testNextHopUnchangedConversion() {
    Configuration c = parseConfig("arista_bgp_nexthop_unchanged");
    Ip nextHopIp = Ip.parse("42.42.42.42");
    Bgpv4Route originalRoute =
        Bgpv4Route.builder()
            .setNetwork(Prefix.parse("1.2.3.0/24"))
            .setNextHopIp(nextHopIp)
            .setAdmin(1)
            .setOriginatorIp(nextHopIp)
            .setOriginType(OriginType.EGP)
            .setProtocol(RoutingProtocol.BGP)
            .build();
    Ip headIp = Ip.parse("1.1.1.1");
    Ip tailIp = Ip.parse("1.1.1.2");
    BgpSessionProperties session =
        BgpSessionProperties.builder()
            .setHeadAs(1)
            .setTailAs(2)
            .setHeadIp(headIp)
            .setTailIp(tailIp)
            .setSessionType(SessionType.EBGP_SINGLEHOP)
            .build();
    {
      // 9.9.9.9 for IPv4
      RoutingPolicy policy =
          c.getRoutingPolicies().get(generatedBgpPeerExportPolicyName(DEFAULT_VRF, "9.9.9.9"));
      Builder builder = Bgpv4Route.builder();
      policy.processBgpRoute(originalRoute, builder, session, Direction.OUT);
      assertThat(builder.getNextHopIp(), equalTo(nextHopIp));
    }
    {
      // 8.8.8.8 for IPv4
      RoutingPolicy policy =
          c.getRoutingPolicies().get(generatedBgpPeerExportPolicyName(DEFAULT_VRF, "8.8.8.8"));
      Builder builder = Bgpv4Route.builder();
      policy.processBgpRoute(originalRoute, builder, session, Direction.OUT);
      assertThat(builder.getNextHopIp(), equalTo(UNSET_ROUTE_NEXT_HOP_IP));
    }
    {
      // 8.8.8.8 for EVPN
      RoutingPolicy policy =
          c.getRoutingPolicies().get(generatedBgpPeerEvpnExportPolicyName(DEFAULT_VRF, "8.8.8.8"));
      Builder builder = Bgpv4Route.builder();
      policy.processBgpRoute(originalRoute, builder, session, Direction.OUT);
      assertThat(builder.getNextHopIp(), equalTo(nextHopIp));
    }
    {
      // 7.7.7.7 for IPv4
      RoutingPolicy policy =
          c.getRoutingPolicies().get(generatedBgpPeerExportPolicyName(DEFAULT_VRF, "7.7.7.7"));
      Builder builder = Bgpv4Route.builder();
      policy.processBgpRoute(originalRoute, builder, session, Direction.OUT);
      assertThat(builder.getNextHopIp(), equalTo(UNSET_ROUTE_NEXT_HOP_IP));
    }
    {
      // 2.2.2.2 for IPv4
      RoutingPolicy policy =
          c.getRoutingPolicies().get(generatedBgpPeerExportPolicyName("vrf2", "2.2.2.2"));
      Builder builder = Bgpv4Route.builder();
      policy.processBgpRoute(originalRoute, builder, session, Direction.OUT);
      assertThat(builder.getNextHopIp(), equalTo(nextHopIp));
    }
    {
      // 2.2.2.22 for IPv4
      RoutingPolicy policy =
          c.getRoutingPolicies().get(generatedBgpPeerExportPolicyName("vrf2", "2.2.2.22"));
      Builder builder = Bgpv4Route.builder();
      policy.processBgpRoute(originalRoute, builder, session, Direction.OUT);
      assertThat(builder.getNextHopIp(), equalTo(nextHopIp));
    }
    {
      // 3.3.3.3 for IPv4
      RoutingPolicy policy =
          c.getRoutingPolicies().get(generatedBgpPeerExportPolicyName("vrf3", "3.3.3.3"));
      Builder builder = Bgpv4Route.builder();
      policy.processBgpRoute(originalRoute, builder, session, Direction.OUT);
      assertThat(builder.getNextHopIp(), equalTo(nextHopIp));
    }
    {
      // 3.3.3.33 for IPv4
      RoutingPolicy policy =
          c.getRoutingPolicies().get(generatedBgpPeerExportPolicyName("vrf3", "3.3.3.33"));
      Builder builder = Bgpv4Route.builder();
      policy.processBgpRoute(originalRoute, builder, session, Direction.OUT);
      assertThat(builder.getNextHopIp(), equalTo(UNSET_ROUTE_NEXT_HOP_IP));
    }
  }

  @Test
  public void testConfederationExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_confederations");
    {
      AristaBgpVrf vrf = config.getAristaBgp().getDefaultVrf();
      assertThat(vrf.getConfederationIdentifier(), equalTo(1111L));
      assertThat(vrf.getConfederationPeers(), equalTo(LongSpace.of(Range.closed(3L, 6L))));
    }
    {
      AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("vrf2");
      assertThat(vrf.getConfederationIdentifier(), equalTo((22L << 16) + 22));
      assertThat(
          vrf.getConfederationPeers(),
          equalTo(
              LongSpace.unionOf(
                  Range.closed((1L << 16) + 1, (1L << 16) + 2),
                  Range.singleton((3L << 16) + 3),
                  Range.singleton(44L))));
    }
  }

  @Test
  public void testConfederationConversion() {
    Configuration c = parseConfig("arista_bgp_confederations");
    {
      BgpConfederation confederation = c.getDefaultVrf().getBgpProcess().getConfederation();
      assertThat(confederation.getId(), equalTo(1111L));
      assertThat(confederation.getMembers(), equalTo(ImmutableSet.of(3L, 4L, 5L, 6L)));
    }
    {
      BgpConfederation confederation = c.getVrfs().get("vrf2").getBgpProcess().getConfederation();

      assertThat(confederation.getId(), equalTo((22L << 16) + 22));
      assertThat(
          confederation.getMembers(),
          containsInAnyOrder((1L << 16) + 1, (1L << 16) + 2, (3L << 16) + 3, 44L));
    }
  }

  @Test
  public void testEvpnImportPolicyExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_evpn_import_policy");
    AristaBgpNeighborAddressFamily neighbor =
        config.getAristaBgp().getDefaultVrf().getEvpnAf().getNeighbor(Ip.parse("2.2.2.2"));
    assertThat(neighbor.getRouteMapIn(), equalTo("ALLOW_10"));
  }

  @Test
  public void testEvpnImportPolicyConversion() {
    Configuration config = parseConfig("arista_bgp_evpn_import_policy");
    String policyName =
        config
            .getDefaultVrf()
            .getBgpProcess()
            .getActiveNeighbors()
            .get(Prefix.parse("2.2.2.2/32"))
            .getEvpnAddressFamily()
            .getImportPolicy();
    RoutingPolicy policy = config.getRoutingPolicies().get(policyName);

    Builder builder =
        Bgpv4Route.builder()
            .setNextHopIp(Ip.ZERO)
            .setAdmin(1)
            .setOriginatorIp(Ip.ZERO)
            .setOriginType(OriginType.EGP)
            .setProtocol(RoutingProtocol.BGP);

    Bgpv4Route acceptRoute = builder.setNetwork(Prefix.parse("10.1.1.0/24")).build();
    Bgpv4Route denyRoute = builder.setNetwork(Prefix.parse("240.1.1.0/24")).build();
    assertTrue(policy.processBgpRoute(acceptRoute, Bgpv4Route.builder(), null, Direction.IN));
    assertFalse(policy.processBgpRoute(denyRoute, Bgpv4Route.builder(), null, Direction.IN));
  }

  @Test
  public void testIpv6RouteParsing() {
    // Don't crash
    parseConfig("ipv6_route");
  }

  @Test
  public void testRouteMapParsing() {
    // Don't crash
    parseConfig("route_map");
  }

  @Test
  public void testSnmpExtraction() {
    Configuration config = parseConfig("arista_snmp");
    assertThat(config.getSnmpTrapServers(), containsInAnyOrder("10.1.2.3"));
  }

  @Test
  public void testPeerFilterExtraction() {
    AristaConfiguration config = parseVendorConfig("arista_bgp_peer_filter");
    assertThat(config.getPeerFilters().keySet(), containsInAnyOrder("PF", "EMPTY"));
    assertThat(
        config
            .getAristaBgp()
            .getDefaultVrf()
            .getV4DynamicNeighbors()
            .get(Prefix.parse("1.1.1.0/24"))
            .getPeerFilter(),
        equalTo("PF"));
    assertThat(
        config
            .getAristaBgp()
            .getDefaultVrf()
            .getV4DynamicNeighbors()
            .get(Prefix.parse("2.2.2.0/24"))
            .getPeerFilter(),
        equalTo("EMPTY"));
    assertThat(
        config
            .getAristaBgp()
            .getDefaultVrf()
            .getV4DynamicNeighbors()
            .get(Prefix.parse("3.3.3.0/24"))
            .getPeerFilter(),
        equalTo("DOES_NOT_EXIST"));
  }

  @Test
  public void testPeerFilterConversion() {
    Configuration config = parseConfig("arista_bgp_peer_filter");
    assertThat(
        config
            .getDefaultVrf()
            .getBgpProcess()
            .getPassiveNeighbors()
            .get(Prefix.parse("1.1.1.0/24"))
            .getRemoteAsns(),
        equalTo(
            LongSpace.builder()
                .including(Range.closed(1L, 10L))
                .excluding(Range.closed(3L, 4L))
                .build()));
    assertThat(
        config
            .getDefaultVrf()
            .getBgpProcess()
            .getPassiveNeighbors()
            .get(Prefix.parse("2.2.2.0/24"))
            .getRemoteAsns(),
        equalTo(BgpPeerConfig.ALL_AS_NUMBERS));
    assertThat(
        config
            .getDefaultVrf()
            .getBgpProcess()
            .getPassiveNeighbors()
            .get(Prefix.parse("3.3.3.0/24"))
            .getRemoteAsns(),
        equalTo(BgpPeerConfig.ALL_AS_NUMBERS));
  }
}

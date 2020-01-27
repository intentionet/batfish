package org.batfish.grammar.arista;

import static org.batfish.datamodel.Names.generatedBgpPeerEvpnExportPolicyName;
import static org.batfish.datamodel.Names.generatedBgpPeerExportPolicyName;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasSendCommunity;
import static org.batfish.datamodel.matchers.AddressFamilyCapabilitiesMatchers.hasSendExtendedCommunity;
import static org.batfish.datamodel.matchers.AddressFamilyMatchers.hasAddressFamilyCapabilites;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasIpv4UnicastAddressFamily;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasConfigurationFormat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVrf;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasName;
import static org.batfish.main.BatfishTestUtils.TEST_SNAPSHOT;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.cisco.eos.AristaBgpProcess.DEFAULT_VRF;
import static org.batfish.representation.cisco.eos.AristaRedistributeType.OSPF;
import static org.batfish.representation.cisco.eos.AristaRedistributeType.OSPF_EXTERNAL;
import static org.batfish.representation.cisco.eos.AristaRedistributeType.OSPF_INTERNAL;
import static org.batfish.representation.cisco.eos.AristaRedistributeType.OSPF_NSSA_EXTERNAL;
import static org.batfish.representation.cisco.eos.AristaRedistributeType.OSPF_NSSA_EXTERNAL_TYPE_1;
import static org.batfish.representation.cisco.eos.AristaRedistributeType.OSPF_NSSA_EXTERNAL_TYPE_2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.plugin.IBatfish;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
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
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface;
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
import org.batfish.datamodel.VrrpGroup;
import org.batfish.datamodel.bgp.BgpConfederation;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Layer2VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.matchers.ConfigurationMatchers;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Layer3Vni;
import org.batfish.grammar.cisco.CiscoCombinedParser;
import org.batfish.grammar.cisco.CiscoControlPlaneExtractor;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.batfish.representation.cisco.VrrpInterface;
import org.batfish.representation.cisco.eos.AristaBgpAggregateNetwork;
import org.batfish.representation.cisco.eos.AristaBgpBestpathTieBreaker;
import org.batfish.representation.cisco.eos.AristaBgpDefaultOriginate;
import org.batfish.representation.cisco.eos.AristaBgpNeighbor.RemovePrivateAsMode;
import org.batfish.representation.cisco.eos.AristaBgpNeighborAddressFamily;
import org.batfish.representation.cisco.eos.AristaBgpNetworkConfiguration;
import org.batfish.representation.cisco.eos.AristaBgpPeerGroupNeighbor;
import org.batfish.representation.cisco.eos.AristaBgpRedistributionPolicy;
import org.batfish.representation.cisco.eos.AristaBgpV4DynamicNeighbor;
import org.batfish.representation.cisco.eos.AristaBgpV4Neighbor;
import org.batfish.representation.cisco.eos.AristaBgpVlan;
import org.batfish.representation.cisco.eos.AristaBgpVlanAwareBundle;
import org.batfish.representation.cisco.eos.AristaBgpVrf;
import org.batfish.representation.cisco.eos.AristaBgpVrfEvpnAddressFamily;
import org.batfish.representation.cisco.eos.AristaBgpVrfIpv4UnicastAddressFamily;
import org.batfish.representation.cisco.eos.AristaBgpVrfIpv6UnicastAddressFamily;
import org.batfish.representation.cisco.eos.AristaRedistributeType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@ParametersAreNonnullByDefault
public class AristaGrammarTest {
  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/arista/testconfigs/";
  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private static @Nonnull CiscoConfiguration parseVendorConfig(String hostname) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    CiscoCombinedParser ciscoParser =
        new CiscoCombinedParser(src, settings, ConfigurationFormat.ARISTA);
    CiscoControlPlaneExtractor extractor =
        new CiscoControlPlaneExtractor(
            src, ciscoParser, ConfigurationFormat.ARISTA, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(
            ciscoParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(TEST_SNAPSHOT, tree);
    CiscoConfiguration vendorConfiguration =
        (CiscoConfiguration) extractor.getVendorConfiguration();
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp");
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
  public void testAggregateAddressExtraction() {
    CiscoConfiguration config = parseVendorConfig("arista_bgp_aggregate_address");
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
  public void testBgpVlansExtraction() {
    CiscoConfiguration config = parseVendorConfig("arista_bgp_vlans");
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
  public void testNeighborExtraction() {
    CiscoConfiguration config = parseVendorConfig("arista_bgp_neighbors");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_vrf");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_af");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_network");
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
    CiscoConfiguration config = parseVendorConfig("arista_vxlan");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_redistribute");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_redistribute_ospf");
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
    assertThat(c, hasInterface("Ethernet1", hasVrf(hasName(equalTo("VRF_1")))));
    assertThat(c, hasInterface("Ethernet2", hasVrf(hasName(equalTo("VRF_2")))));
  }

  @Test
  public void testVrrpConversion() {
    Configuration c = parseConfig("arista_vrrp");
    assertThat(c.getAllInterfaces(), hasKey("Vlan20"));
    Interface i = c.getAllInterfaces().get("Vlan20");
    assertThat(i.getVrrpGroups(), hasKey(1));
    VrrpGroup group = i.getVrrpGroups().get(1);
    assertThat(group.getVirtualAddress(), equalTo(ConcreteInterfaceAddress.parse("1.2.3.4/24")));
  }

  @Test
  public void testVrrpExtraction() {
    CiscoConfiguration c = parseVendorConfig("arista_vrrp");
    assertThat(c.getInterfaces(), hasKey("Vlan20"));
    org.batfish.representation.cisco.Interface i = c.getInterfaces().get("Vlan20");
    assertThat(c.getVrrpGroups(), hasKey("Vlan20"));
    VrrpInterface vrrpI = c.getVrrpGroups().get("Vlan20");
    assertThat(vrrpI.getVrrpGroups(), hasKey(1));
    org.batfish.representation.cisco.VrrpGroup g = vrrpI.getVrrpGroups().get(1);
    assertThat(g.getVirtualAddress(), equalTo(Ip.parse("1.2.3.4")));
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_send_community");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_allowas_in");
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
  public void testEnforceFirstAsExtraction() {
    CiscoConfiguration config = parseVendorConfig("arista_bgp_enforce_first_as");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_neighbor_prefix_list");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_nexthop_unchanged");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_confederations");
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
    CiscoConfiguration config = parseVendorConfig("arista_bgp_evpn_import_policy");
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
}

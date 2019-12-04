package org.batfish.grammar.arista;

import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasConfigurationFormat;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVrf;
import static org.batfish.datamodel.matchers.VrfMatchers.hasName;
import static org.batfish.grammar.cisco.CiscoCombinedParser.DEBUG_FLAG_USE_ARISTA_BGP;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
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
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.BumTransportMethod;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Layer2VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.vxlan.Layer2Vni;
import org.batfish.datamodel.vxlan.Layer3Vni;
import org.batfish.grammar.cisco.CiscoCombinedParser;
import org.batfish.grammar.cisco.CiscoControlPlaneExtractor;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.batfish.representation.cisco.eos.AristaBgpAggregateNetwork;
import org.batfish.representation.cisco.eos.AristaBgpBestpathTieBreaker;
import org.batfish.representation.cisco.eos.AristaBgpDefaultOriginate;
import org.batfish.representation.cisco.eos.AristaBgpNeighbor.RemovePrivateAsMode;
import org.batfish.representation.cisco.eos.AristaBgpNeighborAddressFamily;
import org.batfish.representation.cisco.eos.AristaBgpNetworkConfiguration;
import org.batfish.representation.cisco.eos.AristaBgpPeerGroupNeighbor;
import org.batfish.representation.cisco.eos.AristaBgpProcess;
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
    settings.setDebugFlags(ImmutableList.of(DEBUG_FLAG_USE_ARISTA_BGP));
    CiscoCombinedParser ciscoParser =
        new CiscoCombinedParser(src, settings, ConfigurationFormat.ARISTA);
    CiscoControlPlaneExtractor extractor =
        new CiscoControlPlaneExtractor(
            src, ciscoParser, ConfigurationFormat.ARISTA, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(
            ciscoParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(tree);
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
    Batfish batfish = BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
    batfish.getSettings().setDebugFlags(ImmutableList.of(DEBUG_FLAG_USE_ARISTA_BGP));
    return batfish;
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
    return getBatfishForConfigurationNames(configurationNames).loadConfigurations();
  }

  @Test
  public void testTopLevelBgpExtraction() {
    CiscoConfiguration config = parseVendorConfig("arista_bgp");
    // Basic VRF config
    {
      AristaBgpVrf defaultVrf = config.getAristaBgp().getDefaultVrf();
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
      assertTrue(config.getAristaBgp().getVrfs().get(vrfName).getShutdown());
      assertThat(config.getAristaBgp().getVrfs().get(vrfName).getRouterId(), nullValue());
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
      assertThat(neighbor.getPeerGroup(), equalTo("PEER_G2"));
      assertThat(neighbor.getRemovePrivateAsMode(), is(RemovePrivateAsMode.REPLACE_AS));
      assertThat(neighbor.getRouteReflectorClient(), nullValue());
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
        config.getAristaBgp().getVrfs().keySet(),
        containsInAnyOrder(AristaBgpProcess.DEFAULT_VRF, "FOO", "BAR"));
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
    assertThat(
        c.getVrfs().keySet(), containsInAnyOrder(AristaBgpProcess.DEFAULT_VRF, "FOO", "BAR"));
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
      AristaBgpNetworkConfiguration network =
          config.getAristaBgp().getDefaultVrf().getV4UnicastAf().getNetworks().get(prefix);
      assertThat(network, notNullValue());
      assertThat(network.getRouteMap(), equalTo("RM"));
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
  public void testInterfaceConversion() {
    Configuration c = parseConfig("arista_interface");
    assertThat(c, hasInterface("Ethernet1", hasVrf(hasName(equalTo("VRF_1")))));
    assertThat(c, hasInterface("Ethernet2", hasVrf(hasName(equalTo("VRF_2")))));
  }

  @Test
  public void testEvpnConversion() {
    Configuration c = parseConfig("arista_evpn");
    Ip[] neighborIPs = {Ip.parse("192.168.255.1"), Ip.parse("192.168.255.2")};
    for (Ip ip : neighborIPs) {

      BgpActivePeerConfig neighbor =
          c.getDefaultVrf().getBgpProcess().getActiveNeighbors().get(ip.toPrefix());
      assertThat(neighbor.getEvpnAddressFamily(), notNullValue());
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
                      .build(),
                  Layer3VniConfig.builder()
                      .setVrf("Tenant_B_OPZone")
                      .setVni(50201)
                      .setRouteDistinguisher(RouteDistinguisher.parse("192.168.255.3:50201"))
                      .setImportRouteTarget(ExtendedCommunity.target(50201, 50201).matchString())
                      .setRouteTarget(ExtendedCommunity.target(50201, 50201))
                      .build())));
    }
  }
}

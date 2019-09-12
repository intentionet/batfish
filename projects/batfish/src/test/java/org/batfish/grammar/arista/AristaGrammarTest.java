package org.batfish.grammar.arista;

import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.grammar.cisco.CiscoCombinedParser;
import org.batfish.grammar.cisco.CiscoControlPlaneExtractor;
import org.batfish.main.Batfish;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.batfish.representation.cisco.eos.AristaBgpAggregateNetwork;
import org.batfish.representation.cisco.eos.AristaBgpNetworkConfiguration;
import org.batfish.representation.cisco.eos.AristaBgpPeerGroupNeighbor;
import org.batfish.representation.cisco.eos.AristaBgpRedistributionPolicy;
import org.batfish.representation.cisco.eos.AristaBgpV4Neighbor;
import org.batfish.representation.cisco.eos.AristaBgpVlan;
import org.batfish.representation.cisco.eos.AristaBgpVlanAwareBundle;
import org.batfish.representation.cisco.eos.AristaBgpVrf;
import org.batfish.representation.cisco.eos.AristaBgpVrfEvpnAddressFamily;
import org.batfish.representation.cisco.eos.AristaBgpVrfIpv4UnicastAddressFamily;
import org.batfish.representation.cisco.eos.AristaRedistributeType;
import org.junit.Test;

@ParametersAreNonnullByDefault
public class AristaGrammarTest {
  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/arista/testconfigs/";

  private @Nonnull CiscoConfiguration parseVendorConfig(String hostname) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    settings.setDebugFlags(ImmutableList.of(CiscoCombinedParser.DEBUG_FLAG_USE_ARISTA_BGP));
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
    SerializationUtils.clone(vendorConfiguration);
    return vendorConfiguration;
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
              .getVrfs()
              .get("FOO")
              .getV4aggregates()
              .get(Prefix.parse("5.6.7.0/24"));
      assertTrue(agg.getAsSet());
    }
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
      assertThat(config.getAristaBgp().getPeerGroups().get(peergName).getAllowAsIn(), equalTo(3));
    }
    {
      Ip neighborAddr = Ip.parse("1.1.1.1");
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getDefaultVrf().getV4neighbors().get(neighborAddr);
      assertThat(neighbor.getAllowAsIn(), nullValue());
      // TODO: default-originate
      assertThat(neighbor.getDescription(), equalTo("SOME NEIGHBOR"));
      assertTrue(neighbor.getDontCapabilityNegotiate());
      assertThat(neighbor.getEbgpMultihop(), equalTo(Integer.MAX_VALUE));
      assertTrue(neighbor.getNextHopSelf());
      assertThat(neighbor.getPeerGroup(), equalTo("PEER_G"));
      assertThat(neighbor.getRemoteAs(), equalTo(35L));
      assertTrue(neighbor.getSendCommunity());
    }
    {
      Ip neighborAddr = Ip.parse("2.2.2.2");
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getDefaultVrf().getV4neighbors().get(neighborAddr);
      assertThat(neighbor.getRemoteAs(), equalTo(36L));
      assertThat(neighbor.getEbgpMultihop(), equalTo(10));
      // TODO: default-originate
    }
    {
      Ip neighborAddr = Ip.parse("2.2.2.2");
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getVrfs().get("tenant").getV4neighbors().get(neighborAddr);
      assertThat(neighbor.getRemoteAs(), equalTo(88L));
    }
  }

  @Test
  public void testVrfExtraction() {
    CiscoConfiguration config = parseVendorConfig("arista_bgp_vrf");
    AristaBgpVrf vrf = config.getAristaBgp().getVrfs().get("FOO");
    assertThat(vrf.getRouteDistinguisher(), equalTo(RouteDistinguisher.parse("123:123")));
    assertThat(vrf.getExportRouteTarget(), equalTo(ExtendedCommunity.target(1L, 1L)));
    assertThat(vrf.getImportRouteTarget(), equalTo(ExtendedCommunity.target(2L, 2L)));
    assertThat(vrf.getLocalAs(), equalTo(65000L));
  }

  @Test
  public void testAddressFamilyExtraction() {
    CiscoConfiguration config = parseVendorConfig("arista_bgp_af");
    AristaBgpVrfIpv4UnicastAddressFamily ipv4af =
        config.getAristaBgp().getDefaultVrf().getV4UnicastAf();
    AristaBgpVrfEvpnAddressFamily evpnaf = config.getAristaBgp().getDefaultVrf().getEvpnAf();
    assertThat(ipv4af, notNullValue());
    assertThat(evpnaf, notNullValue());

    {
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getDefaultVrf().getV4neighbors().get(Ip.parse("1.1.1.1"));
      assertTrue(neighbor.getV4UnicastAf().getActivate());
      assertTrue(neighbor.getEvpnAf().getActivate());
    }
    {
      AristaBgpV4Neighbor neighbor =
          config.getAristaBgp().getDefaultVrf().getV4neighbors().get(Ip.parse("2.2.2.2"));
      assertThat(neighbor.getV4UnicastAf(), notNullValue());
      assertFalse(neighbor.getV4UnicastAf().getActivate());
      assertThat(neighbor.getEvpnAf(), nullValue());
    }
    {
      AristaBgpPeerGroupNeighbor pg = config.getAristaBgp().getPeerGroups().get("PG");
      assertThat(pg.getV4UnicastAf(), notNullValue());
      assertThat(pg.getV4UnicastAf().getActivate(), nullValue());
      assertThat(pg.getEvpnAf(), notNullValue());
      assertTrue(pg.getEvpnAf().getActivate());
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
}

package org.batfish.grammar.cisco_nxos;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Maps.immutableEntry;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.datamodel.IpWildcard.ipWithWildcardMask;
import static org.batfish.datamodel.Route.UNSET_NEXT_HOP_INTERFACE;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasAdministrativeCost;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopInterface;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopIp;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasRouteFilterLists;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAddress;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasBandwidth;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasChannelGroup;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasChannelGroupMembers;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDependencies;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDescription;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasInterfaceType;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasSwitchPortMode;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isAutoState;
import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.batfish.datamodel.matchers.StaticRouteMatchers.hasTag;
import static org.batfish.grammar.cisco_nxos.CiscoNxosCombinedParser.DEBUG_FLAG_USE_NEW_CISCO_NXOS_PARSER;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.cisco_nxos.CiscoNxosConfiguration.NULL_VRF_NAME;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.WellKnownCommunity;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DscpType;
import org.batfish.datamodel.IcmpCode;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.matchers.RouteFilterListMatchers;
import org.batfish.datamodel.matchers.VrfMatchers;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.representation.cisco_nxos.ActionIpAccessListLine;
import org.batfish.representation.cisco_nxos.AddrGroupIpAddressSpec;
import org.batfish.representation.cisco_nxos.AddressFamily;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType;
import org.batfish.representation.cisco_nxos.CiscoNxosStructureType;
import org.batfish.representation.cisco_nxos.FragmentsBehavior;
import org.batfish.representation.cisco_nxos.IcmpOptions;
import org.batfish.representation.cisco_nxos.Interface;
import org.batfish.representation.cisco_nxos.InterfaceAddressWithAttributes;
import org.batfish.representation.cisco_nxos.IpAccessList;
import org.batfish.representation.cisco_nxos.IpAccessListLine;
import org.batfish.representation.cisco_nxos.IpAddressSpec;
import org.batfish.representation.cisco_nxos.IpAsPathAccessList;
import org.batfish.representation.cisco_nxos.IpAsPathAccessListLine;
import org.batfish.representation.cisco_nxos.IpCommunityListStandard;
import org.batfish.representation.cisco_nxos.IpCommunityListStandardLine;
import org.batfish.representation.cisco_nxos.IpPrefixList;
import org.batfish.representation.cisco_nxos.IpPrefixListLine;
import org.batfish.representation.cisco_nxos.Layer3Options;
import org.batfish.representation.cisco_nxos.LiteralIpAddressSpec;
import org.batfish.representation.cisco_nxos.LiteralPortSpec;
import org.batfish.representation.cisco_nxos.Nve;
import org.batfish.representation.cisco_nxos.Nve.IngressReplicationProtocol;
import org.batfish.representation.cisco_nxos.NveVni;
import org.batfish.representation.cisco_nxos.PortGroupPortSpec;
import org.batfish.representation.cisco_nxos.PortSpec;
import org.batfish.representation.cisco_nxos.RouteDistinguisherOrAuto;
import org.batfish.representation.cisco_nxos.RouteMap;
import org.batfish.representation.cisco_nxos.RouteMapEntry;
import org.batfish.representation.cisco_nxos.RouteMapMatchAsPath;
import org.batfish.representation.cisco_nxos.RouteMapMatchCommunity;
import org.batfish.representation.cisco_nxos.RouteMapMatchInterface;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpAddress;
import org.batfish.representation.cisco_nxos.RouteMapMatchIpAddressPrefixList;
import org.batfish.representation.cisco_nxos.RouteMapMatchMetric;
import org.batfish.representation.cisco_nxos.RouteMapMatchTag;
import org.batfish.representation.cisco_nxos.RouteMapMetricType;
import org.batfish.representation.cisco_nxos.RouteMapSetAsPathPrependLastAs;
import org.batfish.representation.cisco_nxos.RouteMapSetAsPathPrependLiteralAs;
import org.batfish.representation.cisco_nxos.RouteMapSetCommunity;
import org.batfish.representation.cisco_nxos.RouteMapSetIpNextHopLiteral;
import org.batfish.representation.cisco_nxos.RouteMapSetIpNextHopUnchanged;
import org.batfish.representation.cisco_nxos.RouteMapSetLocalPreference;
import org.batfish.representation.cisco_nxos.RouteMapSetMetric;
import org.batfish.representation.cisco_nxos.RouteMapSetMetricType;
import org.batfish.representation.cisco_nxos.RouteMapSetTag;
import org.batfish.representation.cisco_nxos.StaticRoute;
import org.batfish.representation.cisco_nxos.TcpOptions;
import org.batfish.representation.cisco_nxos.UdpOptions;
import org.batfish.representation.cisco_nxos.Vlan;
import org.batfish.representation.cisco_nxos.Vrf;
import org.batfish.representation.cisco_nxos.VrfAddressFamily;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@ParametersAreNonnullByDefault
public final class CiscoNxosGrammarTest {

  private static final IpSpaceToBDD DST_IP_BDD;
  private static final BDDPacket PKT;
  private static final IpSpaceToBDD SRC_IP_BDD;

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/cisco_nxos/testconfigs/";

  static {
    PKT = new BDDPacket();
    DST_IP_BDD = PKT.getDstIpSpaceToBDD();
    SRC_IP_BDD = PKT.getSrcIpSpaceToBDD();
  }

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private @Nonnull Batfish getBatfishForConfigurationNames(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    Batfish batfish = BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
    batfish.getSettings().setDebugFlags(ImmutableList.of(DEBUG_FLAG_USE_NEW_CISCO_NXOS_PARSER));
    return batfish;
  }

  private @Nonnull Configuration parseConfig(String hostname) throws IOException {
    Map<String, Configuration> configs = parseTextConfigs(hostname);
    String canonicalHostname = hostname.toLowerCase();
    assertThat(configs, hasEntry(equalTo(canonicalHostname), hasHostname(canonicalHostname)));
    return configs.get(canonicalHostname);
  }

  private @Nonnull Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    return getBatfishForConfigurationNames(configurationNames).loadConfigurations();
  }

  private @Nonnull CiscoNxosConfiguration parseVendorConfig(String hostname) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    settings.setDebugFlags(ImmutableList.of(DEBUG_FLAG_USE_NEW_CISCO_NXOS_PARSER));
    CiscoNxosCombinedParser ciscoNxosParser = new CiscoNxosCombinedParser(src, settings);
    CiscoNxosControlPlaneExtractor extractor =
        new CiscoNxosControlPlaneExtractor(src, ciscoNxosParser, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(
            ciscoNxosParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(tree);
    CiscoNxosConfiguration vendorConfiguration =
        (CiscoNxosConfiguration) extractor.getVendorConfiguration();
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + hostname);
    // crash if not serializable
    SerializationUtils.clone(vendorConfiguration);
    return vendorConfiguration;
  }

  /**
   * Temporary parsing test of port of unified Cisco NX-OS BGP grammar. The test file should be
   * moved to parsing ref tests once bit is flipped on new parser.
   */
  @Test
  public void testBgpParsingTemporary() {
    String hostname = "nxos_bgp_parsing_temporary";

    // Just test that parser does not choke.
    assertThat(parseVendorConfig(hostname), not(nullValue()));
  }

  @Test
  public void testHostnameConversion() throws IOException {
    String hostname = "nxos_hostname";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasHostname(hostname));
  }

  @Test
  public void testHostnameExtraction() {
    String hostname = "nxos_hostname";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getHostname(), equalTo(hostname));
  }

  @Test
  public void testInterfaceBindDependency() throws IOException {
    String hostname = "nxos_interface_bind_dependency";
    String ifaceName = "Ethernet1/1";
    String subName = "Ethernet1/1.1";
    Configuration c = parseConfig(hostname);

    // parent should have no dependencies
    assertThat(c, hasInterface(ifaceName, hasDependencies(empty())));
    // subinterface should be bound to parent
    assertThat(
        c,
        hasInterface(
            subName, hasDependencies(contains(new Dependency(ifaceName, DependencyType.BIND)))));
  }

  @Test
  public void testInterfaceIpAddressConversion() throws IOException {
    String hostname = "nxos_interface_ip_address";
    String ifaceName = "Ethernet1/1";
    Configuration c = parseConfig(hostname);

    assertThat(
        c,
        hasInterface(
            ifaceName,
            allOf(
                hasAddress(ConcreteInterfaceAddress.parse("10.0.0.1/24")),
                hasAllAddresses(
                    containsInAnyOrder(
                        ConcreteInterfaceAddress.parse("10.0.0.1/24"),
                        ConcreteInterfaceAddress.parse("10.0.0.2/24"),
                        ConcreteInterfaceAddress.parse("10.0.0.3/24"))))));
  }

  @Test
  public void testInterfaceIpAddressExtraction() {
    String hostname = "nxos_interface_ip_address";
    String ifaceName = "Ethernet1/1";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getInterfaces(), hasKey(ifaceName));

    Interface iface = vc.getInterfaces().get(ifaceName);
    InterfaceAddressWithAttributes primary =
        new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.0.0.1/24"));
    InterfaceAddressWithAttributes secondary2 =
        new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.0.0.2/24"));
    InterfaceAddressWithAttributes secondary3 =
        new InterfaceAddressWithAttributes(ConcreteInterfaceAddress.parse("10.0.0.3/24"));
    secondary3.setTag(3L);

    assertThat(iface.getAddress(), equalTo(primary));
    assertThat(iface.getSecondaryAddresses(), containsInAnyOrder(secondary2, secondary3));
  }

  @Test
  public void testInterfaceRangeConversion() throws IOException {
    String hostname = "nxos_interface_range";
    Configuration c = parseConfig(hostname);

    assertThat(
        c, hasInterfaces(hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/1.1", "Ethernet1/1.2")));
  }

  @Test
  public void testInterfaceRangeExtraction() {
    String hostname = "nxos_interface_range";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/1.1", "Ethernet1/1.2"));
  }

  @Test
  public void testInterfaceSwitchportConversion() throws IOException {
    String hostname = "nxos_interface_switchport";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/2.1",
            "Ethernet1/2.2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8",
            "Ethernet1/9",
            "Ethernet1/10",
            "loopback0",
            "mgmt0",
            "port-channel1",
            "port-channel2",
            "port-channel2.1"));

    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("loopback0");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("mgmt0");
      // Management interfaces are blacklisted in post-processing
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel1");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2.1");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2.2");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), equalTo(2));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel2");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel2.1");
      assertThat(iface, isActive(false));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(2));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(2));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/6");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/7");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/8");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3966))));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/9");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/10");
      assertThat(iface, isActive());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 2))));
    }
  }

  @Test
  public void testInterfaceSwitchportExtraction() {
    String hostname = "nxos_interface_switchport";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/2.1",
            "Ethernet1/2.2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8",
            "Ethernet1/9",
            "Ethernet1/10",
            "loopback0",
            "mgmt0",
            "port-channel1",
            "port-channel2",
            "port-channel2.1"));

    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("loopback0");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("mgmt0");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2.1");
      assertTrue(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2.2");
      assertTrue(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), equalTo(2));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel2.1");
      assertTrue(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(2));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(2));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4094))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/7");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 4))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/8");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 3966))));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/9");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.EMPTY));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/10");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.TRUNK));
      assertThat(iface.getNativeVlan(), equalTo(1));
      assertThat(iface.getAllowedVlans(), equalTo(IntegerSpace.of(Range.closed(1, 2))));
    }
  }

  @Test
  public void testInterfaceSwitchportExtractionInvalid() {
    String hostname = "nxos_interface_switchport_invalid";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/2.1", "Ethernet1/3", "Ethernet1/4"));

    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2.1");
      assertTrue(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getEncapsulationVlan(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getAccessVlan(), equalTo(1));
    }
  }

  /**
   * A generic test that exercised basic interface property extraction and conversion.
   *
   * <p>Note that this should only be for <strong>simple</strong> properties; anything with many
   * cases deserves its own unit test. (See, e.g., {@link #testInterfaceSwitchportExtraction()}.
   */
  @Test
  public void testInterfaceProperties() throws Exception {
    Configuration c = parseConfig("nxos_interface_properties");
    assertThat(c, hasInterface("Ethernet1/1", any(org.batfish.datamodel.Interface.class)));

    org.batfish.datamodel.Interface eth11 = c.getAllInterfaces().get("Ethernet1/1");
    assertThat(
        eth11,
        allOf(
            hasDescription(
                "here is a description with punctuation! and IP address 1.2.3.4/24 etc."),
            hasMtu(9216)));
  }

  @Test
  public void testIpAccessListExtraction() {
    String hostname = "nxos_ip_access_list";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getIpAccessLists(),
        hasKeys(
            "acl_global_options",
            "acl_indices",
            "acl_simple_protocols",
            "acl_common_ip_options_destination_ip",
            "acl_common_ip_options_source_ip",
            "acl_common_ip_options_dscp",
            "acl_common_ip_options_packet_length",
            "acl_common_ip_options_precedence",
            "acl_common_ip_options_ttl",
            "acl_common_ip_options_log",
            "acl_icmp",
            "acl_igmp",
            "acl_tcp_flags",
            "acl_tcp_flags_mask",
            "acl_tcp_destination_ports",
            "acl_tcp_destination_ports_named",
            "acl_tcp_source_ports",
            "acl_tcp_http_method",
            "acl_tcp_option_length",
            "acl_tcp_established",
            "acl_udp_destination_ports",
            "acl_udp_destination_ports_named",
            "acl_udp_source_ports",
            "acl_udp_vxlan"));
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_global_options");
      assertThat(acl.getFragmentsBehavior(), equalTo(FragmentsBehavior.PERMIT_ALL));
      assertThat(acl.getLines(), anEmptyMap());
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_indices");
      assertThat(acl.getFragmentsBehavior(), equalTo(FragmentsBehavior.DEFAULT));
      // check keySet directly to test iteration order
      assertThat(acl.getLines().keySet(), contains(1L, 10L, 13L, 15L, 25L, 35L));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_simple_protocols");
      assertThat(acl.getFragmentsBehavior(), equalTo(FragmentsBehavior.DEFAULT));
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getProtocol)
              .collect(Collectors.toList()), // cannot use immutable list because of null value
          contains(
              equalTo(IpProtocol.AHP),
              equalTo(IpProtocol.EIGRP),
              equalTo(IpProtocol.ESP),
              equalTo(IpProtocol.GRE),
              nullValue(),
              equalTo(IpProtocol.IPIP),
              equalTo(IpProtocol.OSPF),
              equalTo(IpProtocol.IPCOMP),
              equalTo(IpProtocol.PIM),
              equalTo(IpProtocol.fromNumber(1))));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_destination_ip");
      Iterator<IpAddressSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getDstAddressSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      IpAddressSpec spec;

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(DST_IP_BDD),
          equalTo(
              ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.0.0.255"))
                  .toIpSpace()
                  .accept(DST_IP_BDD)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(DST_IP_BDD),
          equalTo(Prefix.parse("10.0.1.0/24").toIpSpace().accept(DST_IP_BDD)));

      spec = specs.next();
      assertThat(((AddrGroupIpAddressSpec) spec).getName(), equalTo("mysrcaddrgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(DST_IP_BDD),
          equalTo(Ip.parse("10.0.2.2").toIpSpace().accept(DST_IP_BDD)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(DST_IP_BDD),
          equalTo(UniverseIpSpace.INSTANCE.accept(DST_IP_BDD)));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_source_ip");
      Iterator<IpAddressSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getSrcAddressSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      IpAddressSpec spec;

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(SRC_IP_BDD),
          equalTo(
              ipWithWildcardMask(Ip.parse("10.0.0.0"), Ip.parse("0.0.0.255"))
                  .toIpSpace()
                  .accept(SRC_IP_BDD)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(SRC_IP_BDD),
          equalTo(Prefix.parse("10.0.1.0/24").toIpSpace().accept(SRC_IP_BDD)));

      spec = specs.next();
      assertThat(((AddrGroupIpAddressSpec) spec).getName(), equalTo("mysrcaddrgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(SRC_IP_BDD),
          equalTo(Ip.parse("10.0.2.2").toIpSpace().accept(SRC_IP_BDD)));

      spec = specs.next();
      assertThat(
          ((LiteralIpAddressSpec) spec).getIpSpace().accept(SRC_IP_BDD),
          equalTo(UniverseIpSpace.INSTANCE.accept(SRC_IP_BDD)));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_dscp");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL3Options)
              .map(Layer3Options::getDscp)
              .collect(ImmutableList.toImmutableList()),
          contains(
              1,
              DscpType.AF11.number(),
              DscpType.AF12.number(),
              DscpType.AF13.number(),
              DscpType.AF21.number(),
              DscpType.AF22.number(),
              DscpType.AF23.number(),
              DscpType.AF31.number(),
              DscpType.AF32.number(),
              DscpType.AF33.number(),
              DscpType.AF41.number(),
              DscpType.AF42.number(),
              DscpType.AF43.number(),
              DscpType.CS1.number(),
              DscpType.CS2.number(),
              DscpType.CS3.number(),
              DscpType.CS4.number(),
              DscpType.CS5.number(),
              DscpType.CS6.number(),
              DscpType.CS7.number(),
              DscpType.DEFAULT.number(),
              DscpType.EF.number()));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_log");
      Iterator<IpAccessListLine> lines = acl.getLines().values().iterator();
      IpAccessListLine line;
      line = lines.next();
      assertTrue(((ActionIpAccessListLine) line).getLog());
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_packet_length");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL3Options)
              .map(Layer3Options::getPacketLength)
              .collect(ImmutableList.toImmutableList()),
          contains(
              IntegerSpace.of(100),
              IntegerSpace.of(Range.closed(20, 199)),
              IntegerSpace.of(Range.closed(301, 9210)),
              IntegerSpace.of(Range.closed(20, 9210)).difference(IntegerSpace.of(400)),
              IntegerSpace.of(Range.closed(500, 600))));
    }
    // TODO: extract and test precedence once name->value mappings are known
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_common_ip_options_ttl");
      assertThat(
          ((ActionIpAccessListLine) acl.getLines().values().iterator().next())
              .getL3Options()
              .getTtl(),
          equalTo(5));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_icmp");
      assertThat(
          acl.getLines().values().stream()
              .filter(ActionIpAccessListLine.class::isInstance) // filter ICMPv6
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(IcmpOptions.class::cast)
              .map(icmpOptions -> immutableEntry(icmpOptions.getType(), icmpOptions.getCode()))
              .collect(ImmutableList.toImmutableList()),
          contains(
              immutableEntry(0, null),
              immutableEntry(1, 2),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE,
                  IcmpCode.COMMUNICATION_ADMINISTRATIVELY_PROHIBITED),
              immutableEntry(IcmpType.ALTERNATE_ADDRESS, null),
              immutableEntry(IcmpType.CONVERSION_ERROR, null),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_HOST_PROHIBITED),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_NETWORK_PROHIBITED),
              immutableEntry(IcmpType.ECHO_REQUEST, null),
              immutableEntry(IcmpType.ECHO_REPLY, null),
              immutableEntry(IcmpType.PARAMETER_PROBLEM, null),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.SOURCE_HOST_ISOLATED),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.HOST_PRECEDENCE_VIOLATION),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, IcmpCode.HOST_ERROR),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, IcmpCode.TOS_AND_HOST_ERROR),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.HOST_UNREACHABLE_FOR_TOS),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_HOST_UNKNOWN),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.HOST_UNREACHABLE),
              immutableEntry(IcmpType.INFO_REPLY, null),
              immutableEntry(IcmpType.INFO_REQUEST, null),
              immutableEntry(IcmpType.MASK_REPLY, null),
              immutableEntry(IcmpType.MASK_REQUEST, null),
              immutableEntry(IcmpType.MOBILE_REDIRECT, null),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, IcmpCode.NETWORK_ERROR),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, IcmpCode.TOS_AND_NETWORK_ERROR),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.NETWORK_UNREACHABLE_FOR_TOS),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.NETWORK_UNREACHABLE),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.DESTINATION_NETWORK_UNKNOWN),
              immutableEntry(IcmpType.PARAMETER_PROBLEM, IcmpCode.BAD_LENGTH),
              immutableEntry(IcmpType.PARAMETER_PROBLEM, IcmpCode.REQUIRED_OPTION_MISSING),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.FRAGMENTATION_NEEDED),
              immutableEntry(IcmpType.PARAMETER_PROBLEM, IcmpCode.INVALID_IP_HEADER),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.PORT_UNREACHABLE),
              immutableEntry(
                  IcmpType.DESTINATION_UNREACHABLE, IcmpCode.PRECEDENCE_CUTOFF_IN_EFFECT),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.PROTOCOL_UNREACHABLE),
              immutableEntry(
                  IcmpType.TIME_EXCEEDED, IcmpCode.TIME_EXCEEDED_DURING_FRAGMENT_REASSEMBLY),
              immutableEntry(IcmpType.REDIRECT_MESSAGE, null),
              immutableEntry(IcmpType.ROUTER_ADVERTISEMENT, null),
              immutableEntry(IcmpType.ROUTER_SOLICITATION, null),
              immutableEntry(IcmpType.SOURCE_QUENCH, null),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, IcmpCode.SOURCE_ROUTE_FAILED),
              immutableEntry(IcmpType.TIME_EXCEEDED, null),
              immutableEntry(IcmpType.TIMESTAMP_REPLY, null),
              immutableEntry(IcmpType.TIMESTAMP_REQUEST, null),
              immutableEntry(IcmpType.TRACEROUTE, null),
              immutableEntry(IcmpType.TIME_EXCEEDED, IcmpCode.TTL_EQ_ZERO_DURING_TRANSIT),
              immutableEntry(IcmpType.DESTINATION_UNREACHABLE, null)));
    }
    // TODO: extract and test IGMP types (and codes?) once name->value mappings are known
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_destination_ports");
      Iterator<PortSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(TcpOptions.class::cast)
              .map(TcpOptions::getDstPortSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      PortSpec spec;

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(1)));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(6, 65535))));

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(0, 9))));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(),
          equalTo(IntegerSpace.of(Range.closed(0, 65535)).difference(IntegerSpace.of(15))));

      spec = specs.next();
      assertThat(((PortGroupPortSpec) spec).getName(), equalTo("mydstportgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(20, 25))));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_destination_ports_named");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(TcpOptions.class::cast)
              .map(TcpOptions::getDstPortSpec)
              .map(LiteralPortSpec.class::cast)
              .map(LiteralPortSpec::getPorts)
              .map(IntegerSpace::singletonValue)
              .collect(ImmutableList.toImmutableList()),
          contains(
              NamedPort.BGP.number(),
              NamedPort.CHARGEN.number(),
              NamedPort.CMDtcp_OR_SYSLOGudp.number(),
              NamedPort.DAYTIME.number(),
              NamedPort.DISCARD.number(),
              NamedPort.DOMAIN.number(),
              NamedPort.DRIP.number(),
              NamedPort.ECHO.number(),
              NamedPort.BIFFudp_OR_EXECtcp.number(),
              NamedPort.FINGER.number(),
              NamedPort.FTP.number(),
              NamedPort.FTP_DATA.number(),
              NamedPort.GOPHER.number(),
              NamedPort.HOSTNAME.number(),
              NamedPort.IDENT.number(),
              NamedPort.IRC.number(),
              NamedPort.KLOGIN.number(),
              NamedPort.KSHELL.number(),
              NamedPort.LOGINtcp_OR_WHOudp.number(),
              NamedPort.LPD.number(),
              NamedPort.NNTP.number(),
              NamedPort.PIM_AUTO_RP.number(),
              NamedPort.POP2.number(),
              NamedPort.POP3.number(),
              NamedPort.SMTP.number(),
              NamedPort.SUNRPC.number(),
              NamedPort.TACACS.number(),
              NamedPort.TALK.number(),
              NamedPort.TELNET.number(),
              NamedPort.TIME.number(),
              NamedPort.UUCP.number(),
              NamedPort.WHOIS.number(),
              NamedPort.HTTP.number()));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_source_ports");
      Iterator<PortSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(TcpOptions.class::cast)
              .map(TcpOptions::getSrcPortSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      PortSpec spec;

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(1)));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(6, 65535))));

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(0, 9))));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(),
          equalTo(IntegerSpace.of(Range.closed(0, 65535)).difference(IntegerSpace.of(15))));

      spec = specs.next();
      assertThat(((PortGroupPortSpec) spec).getName(), equalTo("mysrcportgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(20, 25))));
    }
    // TODO: extract and test http-method match
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_flags");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(TcpOptions.class::cast)
              .map(TcpOptions::getTcpFlags)
              .collect(ImmutableList.toImmutableList()),
          contains(
              TcpFlags.builder().setAck(true).build(),
              TcpFlags.builder().setFin(true).build(),
              TcpFlags.builder().setPsh(true).build(),
              TcpFlags.builder().setRst(true).build(),
              TcpFlags.builder().setSyn(true).build(),
              TcpFlags.builder().setUrg(true).build()));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_flags_mask");
      assertThat(
          ((TcpOptions)
                  ((ActionIpAccessListLine) acl.getLines().values().iterator().next())
                      .getL4Options())
              .getTcpFlagsMask(),
          equalTo(50));
    }
    // TODO: extract and test tcp-option-length match
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_tcp_established");
      assertTrue(
          ((TcpOptions)
                  ((ActionIpAccessListLine) acl.getLines().values().iterator().next())
                      .getL4Options())
              .getEstablished());
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_udp_destination_ports");
      Iterator<PortSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(UdpOptions.class::cast)
              .map(UdpOptions::getDstPortSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      PortSpec spec;

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(1)));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(6, 65535))));

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(0, 9))));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(),
          equalTo(IntegerSpace.of(Range.closed(0, 65535)).difference(IntegerSpace.of(15))));

      spec = specs.next();
      assertThat(((PortGroupPortSpec) spec).getName(), equalTo("mydstportgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(20, 25))));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_udp_destination_ports_named");
      assertThat(
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(UdpOptions.class::cast)
              .map(UdpOptions::getDstPortSpec)
              .map(LiteralPortSpec.class::cast)
              .map(LiteralPortSpec::getPorts)
              .map(IntegerSpace::singletonValue)
              .collect(ImmutableList.toImmutableList()),
          contains(
              NamedPort.BIFFudp_OR_EXECtcp.number(),
              NamedPort.BOOTPC.number(),
              NamedPort.BOOTPS_OR_DHCP.number(),
              NamedPort.DISCARD.number(),
              NamedPort.DNSIX.number(),
              NamedPort.DOMAIN.number(),
              NamedPort.ECHO.number(),
              NamedPort.ISAKMP.number(),
              NamedPort.MOBILE_IP_AGENT.number(),
              NamedPort.NAMESERVER.number(),
              NamedPort.NETBIOS_DGM.number(),
              NamedPort.NETBIOS_NS.number(),
              NamedPort.NETBIOS_SSN.number(),
              NamedPort.NON500_ISAKMP.number(),
              NamedPort.NTP.number(),
              NamedPort.PIM_AUTO_RP.number(),
              NamedPort.EFStcp_OR_RIPudp.number(),
              NamedPort.SNMP.number(),
              NamedPort.SNMPTRAP.number(),
              NamedPort.SUNRPC.number(),
              NamedPort.CMDtcp_OR_SYSLOGudp.number(),
              NamedPort.TACACS.number(),
              NamedPort.TALK.number(),
              NamedPort.TFTP.number(),
              NamedPort.TIME.number(),
              NamedPort.LOGINtcp_OR_WHOudp.number(),
              NamedPort.XDMCP.number()));
    }
    {
      IpAccessList acl = vc.getIpAccessLists().get("acl_udp_source_ports");
      Iterator<PortSpec> specs =
          acl.getLines().values().stream()
              .map(ActionIpAccessListLine.class::cast)
              .map(ActionIpAccessListLine::getL4Options)
              .map(UdpOptions.class::cast)
              .map(UdpOptions::getSrcPortSpec)
              .collect(ImmutableList.toImmutableList())
              .iterator();
      PortSpec spec;

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(1)));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(6, 65535))));

      spec = specs.next();
      assertThat(((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(0, 9))));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(),
          equalTo(IntegerSpace.of(Range.closed(0, 65535)).difference(IntegerSpace.of(15))));

      spec = specs.next();
      assertThat(((PortGroupPortSpec) spec).getName(), equalTo("mysrcportgroup"));

      spec = specs.next();
      assertThat(
          ((LiteralPortSpec) spec).getPorts(), equalTo(IntegerSpace.of(Range.closed(20, 25))));
    }
    // TODO: extract and test UDP nve vni match
  }

  @Test
  public void testIpAccessListReferences() throws IOException {
    String hostname = "nxos_ip_access_list_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(
        ans, hasNumReferrers(filename, CiscoNxosStructureType.IP_ACCESS_LIST, "acl_unused", 0));
  }

  @Test
  public void testIpAsPathAccessListExtraction() {
    String hostname = "nxos_ip_as_path_access_list";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpAsPathAccessLists(), hasKeys("aspacl_seq", "aspacl_test"));
    {
      IpAsPathAccessList acl = vc.getIpAsPathAccessLists().get("aspacl_seq");
      // check keySet directly to test iteration order
      assertThat(acl.getLines().keySet(), contains(1L, 5L, 10L, 11L));
    }
    {
      IpAsPathAccessList acl = vc.getIpAsPathAccessLists().get("aspacl_test");
      // check keySet directly to test iteration order
      Iterator<IpAsPathAccessListLine> lines = acl.getLines().values().iterator();
      IpAsPathAccessListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getRegex(), equalTo("(_1_2_|_2_1_)"));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo("_1_"));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getRegex(), equalTo("_2_"));
    }
  }

  @Test
  public void testIpCommunityListStandardExtraction() {
    String hostname = "nxos_ip_community_list_standard";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpCommunityLists(), hasKeys("cl_seq", "cl_values", "cl_test"));
    {
      IpCommunityListStandard cl = (IpCommunityListStandard) vc.getIpCommunityLists().get("cl_seq");
      Iterator<IpCommunityListStandardLine> lines = cl.getLines().values().iterator();
      IpCommunityListStandardLine line;

      line = lines.next();
      assertThat(line.getLine(), equalTo(1L));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(1, 1)));

      line = lines.next();
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(5, 5)));

      line = lines.next();
      assertThat(line.getLine(), equalTo(10L));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(10, 10)));

      line = lines.next();
      assertThat(line.getLine(), equalTo(11L));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(11, 11)));

      assertFalse(lines.hasNext());
    }
    {
      IpCommunityListStandard cl =
          (IpCommunityListStandard) vc.getIpCommunityLists().get("cl_values");
      assertThat(
          cl.getLines().values().stream()
              .map(IpCommunityListStandardLine::getCommunities)
              .map(Iterables::getOnlyElement)
              .collect(ImmutableList.toImmutableList()),
          contains(
              StandardCommunity.of(1, 1),
              StandardCommunity.of(WellKnownCommunity.INTERNET),
              StandardCommunity.of(WellKnownCommunity.NO_EXPORT_SUBCONFED),
              StandardCommunity.of(WellKnownCommunity.NO_ADVERTISE),
              StandardCommunity.of(WellKnownCommunity.NO_EXPORT)));
    }
    {
      IpCommunityListStandard cl =
          (IpCommunityListStandard) vc.getIpCommunityLists().get("cl_test");
      Iterator<IpCommunityListStandardLine> lines = cl.getLines().values().iterator();
      IpCommunityListStandardLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(
          line.getCommunities(), contains(StandardCommunity.of(1, 1), StandardCommunity.of(2, 2)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(1, 1)));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getCommunities(), contains(StandardCommunity.of(2, 2)));

      assertFalse(lines.hasNext());
    }
  }

  @Test
  public void testIpPrefixListConversion() throws IOException {
    String hostname = "nxos_ip_prefix_list";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasRouteFilterLists(hasKeys("pl_empty", "pl_test", "pl_range")));
    {
      RouteFilterList r = c.getRouteFilterLists().get("pl_empty");
      assertThat(r, RouteFilterListMatchers.hasLines(empty()));
    }
    {
      RouteFilterList r = c.getRouteFilterLists().get("pl_test");
      Iterator<RouteFilterLine> lines = r.getLines().iterator();
      RouteFilterLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.0.3.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.0.1.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.0.2.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.0.4.0/24")));
    }
    {
      RouteFilterList r = c.getRouteFilterLists().get("pl_range");
      Iterator<RouteFilterLine> lines = r.getLines().iterator();
      RouteFilterLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(16, Prefix.MAX_PREFIX_LENGTH)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(24, 24)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(8, Prefix.MAX_PREFIX_LENGTH)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(20, 24)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(16, 24)));
      assertThat(line.getIpWildcard().toPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));
    }
  }

  @Test
  public void testIpPrefixListExtraction() {
    String hostname = "nxos_ip_prefix_list";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getIpPrefixLists(), hasKeys("pl_empty", "pl_test", "pl_range"));

    {
      IpPrefixList pl = vc.getIpPrefixLists().get("pl_empty");
      assertThat(pl.getDescription(), equalTo("An empty prefix-list"));
      assertThat(pl.getLines(), anEmptyMap());
    }
    {
      IpPrefixList pl = vc.getIpPrefixLists().get("pl_test");
      assertThat(pl.getDescription(), nullValue());
      assertThat(pl.getLines().keySet(), contains(3L, 5L, 10L, 15L));
      Iterator<IpPrefixListLine> lines = pl.getLines().values().iterator();
      IpPrefixListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLine(), equalTo(3L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.0.3.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.0.1.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.DENY));
      assertThat(line.getLine(), equalTo(10L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.0.2.0/24")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLine(), equalTo(15L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.0.4.0/24")));
    }
    {
      IpPrefixList pl = vc.getIpPrefixLists().get("pl_range");
      assertThat(pl.getDescription(), nullValue());
      assertThat(pl.getLines().keySet(), contains(5L, 10L, 15L, 20L, 25L));
      Iterator<IpPrefixListLine> lines = pl.getLines().values().iterator();
      IpPrefixListLine line;

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(16, Prefix.MAX_PREFIX_LENGTH)));
      assertThat(line.getLine(), equalTo(5L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(24, 24)));
      assertThat(line.getLine(), equalTo(10L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(8, Prefix.MAX_PREFIX_LENGTH)));
      assertThat(line.getLine(), equalTo(15L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(20, 24)));
      assertThat(line.getLine(), equalTo(20L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));

      line = lines.next();
      assertThat(line.getAction(), equalTo(LineAction.PERMIT));
      assertThat(line.getLengthRange(), equalTo(new SubRange(16, 24)));
      assertThat(line.getLine(), equalTo(25L));
      assertThat(line.getPrefix(), equalTo(Prefix.parse("10.10.0.0/16")));
    }
  }

  @Test
  public void testNveExtraction() {
    CiscoNxosConfiguration vc = parseVendorConfig("nxos_nve");
    Map<Integer, Nve> nves = vc.getNves();
    assertThat(nves, hasKeys(1, 2, 3, 4));
    {
      Nve nve = nves.get(1);
      assertFalse(nve.isShutdown());
      assertThat(nve.getSourceInterface(), equalTo("loopback0"));
      assertThat(nve.getGlobalIngressReplicationProtocol(), nullValue());
      assertTrue(nve.isGlobalSuppressArp());
      int vni = 10001;
      assertThat(nve.getMemberVnis(), hasKeys(vni));
      NveVni vniConfig = nve.getMemberVni(vni);
      assertThat(vniConfig.getVni(), equalTo(vni));
      assertThat(vniConfig.getSuppressArp(), equalTo(Boolean.FALSE));
      assertThat(vniConfig.getIngressReplicationProtocol(), nullValue());
      assertThat(vniConfig.getMcastGroup(), nullValue());
    }
    {
      Nve nve = nves.get(2);
      assertFalse(nve.isShutdown());
      assertThat(nve.getSourceInterface(), equalTo("loopback0"));
      assertThat(nve.getGlobalIngressReplicationProtocol(), nullValue());
      assertTrue(nve.isGlobalSuppressArp());
    }
    {
      Nve nve = nves.get(3);
      assertFalse(nve.isShutdown());
      assertThat(nve.getSourceInterface(), equalTo("loopback0"));
      assertThat(nve.getGlobalIngressReplicationProtocol(), nullValue());
      assertTrue(nve.isGlobalSuppressArp());
      assertThat(nve.getMulticastGroupL2(), nullValue());
      assertThat(nve.getMulticastGroupL3(), nullValue());
    }
    {
      Nve nve = nves.get(4);
      assertTrue(nve.isShutdown());
      assertThat(nve.getSourceInterface(), equalTo("loopback4"));
      assertThat(
          nve.getGlobalIngressReplicationProtocol(), equalTo(IngressReplicationProtocol.BGP));
      assertFalse(nve.isGlobalSuppressArp());
      assertEquals(nve.getMulticastGroupL2(), Ip.parse("233.0.0.0"));
      assertEquals(nve.getMulticastGroupL3(), Ip.parse("234.0.0.0"));
      int vni = 40001;
      assertThat(nve.getMemberVnis(), hasKeys(vni));
      NveVni vniConfig = nve.getMemberVni(vni);
      assertThat(vniConfig.getSuppressArp(), nullValue());
      assertThat(
          vniConfig.getIngressReplicationProtocol(), equalTo(IngressReplicationProtocol.STATIC));
      assertThat(vniConfig.getMcastGroup(), equalTo(Ip.parse("235.0.0.0")));
    }
  }

  @Test
  public void testPortChannelConversion() throws IOException {
    String hostname = "nxos_port_channel";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces(),
        hasKeys(
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8",
            "Ethernet1/9",
            "Ethernet1/10",
            "Ethernet1/11",
            "port-channel1",
            "port-channel2",
            "port-channel3",
            "port-channel4",
            "port-channel5",
            "port-channel6"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertThat(iface, isActive(false));
      assertThat(iface, hasChannelGroup("port-channel2"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel3"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel3"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive(false));
      assertThat(iface, hasChannelGroup("port-channel3"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/6");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel4"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/7");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel4"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/8");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel5"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/9");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel5"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/10");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel6"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/11");
      assertThat(iface, isActive());
      assertThat(iface, hasChannelGroup("port-channel6"));
      assertThat(iface, hasInterfaceType(InterfaceType.PHYSICAL));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel1");
      assertThat(iface, isActive(false));
      assertThat(iface, hasChannelGroupMembers(empty()));
      assertThat(iface, hasDependencies(empty()));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel2");
      assertThat(iface, isActive(false));
      assertThat(iface, hasChannelGroupMembers(contains("Ethernet1/2")));
      assertThat(
          iface,
          hasDependencies(contains(new Dependency("Ethernet1/2", DependencyType.AGGREGATE))));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel3");
      assertThat(iface, isActive(true));
      assertThat(
          iface, hasChannelGroupMembers(contains("Ethernet1/3", "Ethernet1/4", "Ethernet1/5")));
      assertThat(
          iface,
          hasDependencies(
              containsInAnyOrder(
                  new Dependency("Ethernet1/3", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/4", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/5", DependencyType.AGGREGATE))));
      assertThat(iface, hasBandwidth(200E6));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel4");
      assertThat(iface, isActive(true));
      assertThat(iface, hasChannelGroupMembers(contains("Ethernet1/6", "Ethernet1/7")));
      assertThat(
          iface,
          hasDependencies(
              containsInAnyOrder(
                  new Dependency("Ethernet1/6", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/7", DependencyType.AGGREGATE))));
      assertThat(iface, hasBandwidth(200E6));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel5");
      assertThat(iface, isActive(true));
      assertThat(iface, hasChannelGroupMembers(contains("Ethernet1/8", "Ethernet1/9")));
      assertThat(
          iface,
          hasDependencies(
              containsInAnyOrder(
                  new Dependency("Ethernet1/8", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/9", DependencyType.AGGREGATE))));
      assertThat(iface, hasBandwidth(200E6));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("port-channel6");
      assertThat(iface, isActive(true));
      assertThat(iface, hasChannelGroupMembers(contains("Ethernet1/10", "Ethernet1/11")));
      assertThat(
          iface,
          hasDependencies(
              containsInAnyOrder(
                  new Dependency("Ethernet1/10", DependencyType.AGGREGATE),
                  new Dependency("Ethernet1/11", DependencyType.AGGREGATE))));
      assertThat(iface, hasBandwidth(200E6));
      assertThat(iface, hasInterfaceType(InterfaceType.AGGREGATED));
    }
  }

  @Test
  public void testPortChannelExtraction() {
    String hostname = "nxos_port_channel";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys(
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "Ethernet1/7",
            "Ethernet1/8",
            "Ethernet1/9",
            "Ethernet1/10",
            "Ethernet1/11",
            "port-channel1",
            "port-channel2",
            "port-channel3",
            "port-channel4",
            "port-channel5",
            "port-channel6"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertTrue(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel2"));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel3"));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel3"));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertTrue(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel3"));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel4"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/7");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel4"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/8");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel5"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/9");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel5"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/10");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel6"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/11");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), equalTo("port-channel6"));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel5");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
    {
      Interface iface = vc.getInterfaces().get("port-channel6");
      assertFalse(iface.getShutdown());
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.ACCESS));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.PORT_CHANNEL));
    }
  }

  @Test
  public void testPortChannelExtractionInvalid() {
    String hostname = "nxos_port_channel_invalid";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys(
            "Ethernet1/1",
            "Ethernet1/2",
            "Ethernet1/3",
            "Ethernet1/4",
            "Ethernet1/5",
            "Ethernet1/6",
            "port-channel1"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertTrue(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/6");
      assertFalse(iface.getShutdown());
      assertThat(iface.getChannelGroup(), nullValue());
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.ETHERNET));
    }
  }

  @Test
  public void testPortChannelReferences() throws IOException {
    String hostname = "nxos_port_channel_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(
        ans, hasNumReferrers(filename, CiscoNxosStructureType.PORT_CHANNEL, "port-channel1", 1));
    assertThat(
        ans, hasNumReferrers(filename, CiscoNxosStructureType.PORT_CHANNEL, "port-channel2", 0));
  }

  @Test
  public void testRouteMapExtraction() {
    String hostname = "nxos_route_map";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getRouteMaps(),
        hasKeys(
            "empty_deny",
            "empty_permit",
            "match_as_path",
            "match_community",
            "match_interface",
            "match_ip_address",
            "match_ip_address_prefix_list",
            "match_metric",
            "match_tag",
            "set_as_path_prepend_last_as",
            "set_as_path_prepend_literal_as",
            "set_community",
            "set_community_additive",
            "set_ip_next_hop_literal",
            "set_ip_next_hop_unchanged",
            "set_local_preference",
            "set_metric",
            "set_metric_type_external",
            "set_metric_type_internal",
            "set_metric_type_type_1",
            "set_metric_type_type_2",
            "set_tag",
            "match_undefined_access_list",
            "match_undefined_community_list",
            "match_undefined_prefix_list",
            "continue_skip_deny",
            "continue_from_deny_to_permit",
            "continue_from_permit_to_fall_off",
            "continue_from_permit_and_set_to_fall_off",
            "continue_with_set_and_fall_off",
            "continue_from_set_to_match_on_set_field"));
    {
      RouteMap rm = vc.getRouteMaps().get("empty_deny");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.DENY));
      assertThat(entry.getSequence(), equalTo(10));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("empty_permit");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_as_path");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchAsPath match = entry.getMatchAsPath();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("as_path_access_list1"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_community");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchCommunity match = entry.getMatchCommunity();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("community_list1"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_interface");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchInterface match = entry.getMatchInterface();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("loopback0"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_ip_address");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchIpAddress match = entry.getMatchIpAddress();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getName(), equalTo("access_list1"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_ip_address_prefix_list");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchIpAddressPrefixList match = entry.getMatchIpAddressPrefixList();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getNames(), contains("prefix_list1"));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_metric");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchMetric match = entry.getMatchMetric();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getMetric(), equalTo(1L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("match_tag");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapMatchTag match = entry.getMatchTag();
      assertThat(entry.getMatches().collect(onlyElement()), equalTo(match));
      assertThat(match.getTag(), equalTo(1L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_as_path_prepend_last_as");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetAsPathPrependLastAs set =
          (RouteMapSetAsPathPrependLastAs) entry.getSetAsPathPrepend();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getNumPrepends(), equalTo(3));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_as_path_prepend_literal_as");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetAsPathPrependLiteralAs set =
          (RouteMapSetAsPathPrependLiteralAs) entry.getSetAsPathPrepend();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getAsNumbers(), contains(65000L, 65100L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_community");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetCommunity set = entry.getSetCommunity();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(
          set.getCommunities(), contains(StandardCommunity.of(1, 1), StandardCommunity.of(1, 2)));
      assertFalse(set.getAdditive());
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_community_additive");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetCommunity set = entry.getSetCommunity();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(
          set.getCommunities(), contains(StandardCommunity.of(1, 1), StandardCommunity.of(1, 2)));
      assertTrue(set.getAdditive());
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_ip_next_hop_literal");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetIpNextHopLiteral set = (RouteMapSetIpNextHopLiteral) entry.getSetIpNextHop();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getNextHops(), contains(Ip.parse("192.0.2.1"), Ip.parse("192.0.2.2")));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_ip_next_hop_unchanged");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetIpNextHopUnchanged set = (RouteMapSetIpNextHopUnchanged) entry.getSetIpNextHop();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_local_preference");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetLocalPreference set = entry.getSetLocalPreference();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getLocalPreference(), equalTo(1L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetric set = entry.getSetMetric();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetric(), equalTo(1L));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric_type_external");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetricType set = entry.getSetMetricType();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetricType(), equalTo(RouteMapMetricType.EXTERNAL));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric_type_internal");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetricType set = entry.getSetMetricType();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetricType(), equalTo(RouteMapMetricType.INTERNAL));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric_type_type_1");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetricType set = entry.getSetMetricType();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetricType(), equalTo(RouteMapMetricType.TYPE_1));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_metric_type_type_2");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetMetricType set = entry.getSetMetricType();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getMetricType(), equalTo(RouteMapMetricType.TYPE_2));
    }
    {
      RouteMap rm = vc.getRouteMaps().get("set_tag");
      assertThat(rm.getEntries().keySet(), contains(10));
      RouteMapEntry entry = getOnlyElement(rm.getEntries().values());
      assertThat(entry.getAction(), equalTo(LineAction.PERMIT));
      assertThat(entry.getSequence(), equalTo(10));
      RouteMapSetTag set = entry.getSetTag();
      assertThat(entry.getSets().collect(onlyElement()), equalTo(set));
      assertThat(set.getTag(), equalTo(1L));
    }
    // TODO: route-map 'continue' extraction
  }

  @Test
  public void testStaticRouteConversion() throws IOException {
    String hostname = "nxos_static_route";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVrfs(), hasKeys(DEFAULT_VRF_NAME, NULL_VRF_NAME, "vrf1"));
    assertThat(
        c.getDefaultVrf().getStaticRoutes(),
        containsInAnyOrder(
            hasPrefix(Prefix.strict("10.0.0.0/24")),
            hasPrefix(Prefix.strict("10.0.1.0/24")),
            hasPrefix(Prefix.strict("10.0.2.0/24")),
            hasPrefix(Prefix.strict("10.0.3.0/24")),
            hasPrefix(Prefix.strict("10.0.4.0/24")),
            hasPrefix(Prefix.strict("10.0.5.0/24")),
            hasPrefix(Prefix.strict("10.0.6.0/24")),
            hasPrefix(Prefix.strict("10.0.7.0/24")),
            hasPrefix(Prefix.strict("10.0.8.0/24"))));
    assertThat(
        c.getVrfs().get("vrf1").getStaticRoutes(),
        contains(hasPrefix(Prefix.strict("10.0.11.0/24"))));
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.0.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasNextHopInterface(NULL_INTERFACE_NAME));
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.1.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasNextHopInterface(UNSET_NEXT_HOP_INTERFACE));
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.2.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.3.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.4.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.5.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(1000L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.6.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(5));
      assertThat(route, hasTag(1000L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.7.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(5));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    {
      org.batfish.datamodel.StaticRoute route =
          c.getDefaultVrf().getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.8.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(5));
      assertThat(route, hasTag(1000L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.1.254")));
      assertThat(route, hasNextHopInterface("Ethernet1/1"));
    }
    // TODO: support next-hop-vrf used by 10.0.9.0/24 and 10.0.10.0/24
    {
      org.batfish.datamodel.StaticRoute route =
          c.getVrfs().get("vrf1").getStaticRoutes().stream()
              .filter(r -> r.getNetwork().equals(Prefix.strict("10.0.11.0/24")))
              .findFirst()
              .get();
      assertThat(route, hasAdministrativeCost(1));
      assertThat(route, hasTag(0L));
      assertThat(route, hasNextHopIp(Ip.parse("10.255.2.254")));
    }
  }

  @Test
  public void testStaticRouteExtraction() {
    String hostname = "nxos_static_route";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getVrfs(), hasKeys("vrf1"));
    assertThat(
        vc.getDefaultVrf().getStaticRoutes().asMap(),
        hasKeys(
            Prefix.strict("10.0.0.0/24"),
            Prefix.strict("10.0.1.0/24"),
            Prefix.strict("10.0.2.0/24"),
            Prefix.strict("10.0.3.0/24"),
            Prefix.strict("10.0.4.0/24"),
            Prefix.strict("10.0.5.0/24"),
            Prefix.strict("10.0.6.0/24"),
            Prefix.strict("10.0.7.0/24"),
            Prefix.strict("10.0.8.0/24"),
            Prefix.strict("10.0.9.0/24"),
            Prefix.strict("10.0.10.0/24")));
    assertThat(
        vc.getVrfs().get("vrf1").getStaticRoutes().asMap(),
        hasKeys(Prefix.strict("10.0.11.0/24"), Prefix.strict("10.0.12.0/24")));
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.0.0/24")).iterator().next();
      assertTrue(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.1.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.2.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.3.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.4.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.5.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.6.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.7.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.8.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.1.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/1"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.9.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.2.254")));
      assertThat(route.getNextHopInterface(), nullValue());
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
      assertThat(route.getNextHopVrf(), equalTo("vrf2"));
    }
    {
      StaticRoute route =
          vc.getDefaultVrf().getStaticRoutes().get(Prefix.strict("10.0.10.0/24")).iterator().next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 5));
      assertThat(route.getTag(), equalTo(1000L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.2.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/2"));
      assertThat(route.getTrack(), equalTo((short) 500));
      assertThat(route.getName(), equalTo("foo"));
      assertThat(route.getNextHopVrf(), equalTo("vrf2"));
    }
    {
      StaticRoute route =
          vc.getVrfs()
              .get("vrf1")
              .getStaticRoutes()
              .get(Prefix.strict("10.0.11.0/24"))
              .iterator()
              .next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.2.254")));
      assertThat(route.getNextHopVrf(), nullValue());
    }
    {
      StaticRoute route =
          vc.getVrfs()
              .get("vrf1")
              .getStaticRoutes()
              .get(Prefix.strict("10.0.12.0/24"))
              .iterator()
              .next();
      assertFalse(route.getDiscard());
      assertThat(route.getPreference(), equalTo((short) 1));
      assertThat(route.getTag(), equalTo(0L));
      assertThat(route.getNextHopIp(), equalTo(Ip.parse("10.255.2.254")));
      assertThat(route.getNextHopInterface(), equalTo("Ethernet1/100"));
      assertThat(route.getNextHopVrf(), nullValue());
    }
  }

  @Test
  public void testStaticRouteReferences() throws IOException {
    String hostname = "nxos_static_route_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.INTERFACE, "Ethernet1/1", 2));
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VRF, "vrf1", 2));
  }

  @Test
  public void testVlanConversion() throws IOException {
    String hostname = "nxos_vlan";
    Configuration c = parseConfig(hostname);

    assertThat(
        c.getAllInterfaces(),
        hasKeys("Ethernet1/1", "Vlan1", "Vlan2", "Vlan3", "Vlan4", "Vlan6", "Vlan7"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan1");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(1));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan2");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(2));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan3");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState(false));
      assertThat(iface, hasVlan(3));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan4");
      assertThat(iface, isActive(false));
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(4));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan6");
      assertThat(iface, isActive());
      assertThat(iface, isAutoState(false));
      assertThat(iface, hasVlan(6));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Vlan7");
      assertThat(iface, isActive());
      assertThat(iface, isAutoState());
      assertThat(iface, hasVlan(7));
      assertThat(iface, hasSwitchPortMode(SwitchportMode.NONE));
      assertThat(iface, hasInterfaceType(InterfaceType.VLAN));
    }
  }

  @Test
  public void testVlanExtraction() {
    String hostname = "nxos_vlan";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Vlan1", "Vlan2", "Vlan3", "Vlan4", "Vlan6", "Vlan7"));
    {
      Interface iface = vc.getInterfaces().get("Vlan1");
      assertTrue(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(1));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan2");
      assertTrue(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(2));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan3");
      assertFalse(iface.getShutdown());
      assertFalse(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(3));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan4");
      assertFalse(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(4));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan6");
      assertFalse(iface.getShutdown());
      assertFalse(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(6));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }
    {
      Interface iface = vc.getInterfaces().get("Vlan7");
      assertFalse(iface.getShutdown());
      assertTrue(iface.getAutostate());
      assertThat(iface.getVlan(), equalTo(7));
      assertThat(iface.getSwitchportMode(), equalTo(SwitchportMode.NONE));
      assertThat(iface.getType(), equalTo(CiscoNxosInterfaceType.VLAN));
    }

    assertThat(vc.getVlans(), hasKeys(2, 4, 6, 7, 8));
    {
      Vlan vlan = vc.getVlans().get(2);
      assertThat(vlan.getVni(), equalTo(12345));
    }
    {
      Vlan vlan = vc.getVlans().get(4);
      assertThat(vlan.getVni(), nullValue());
    }
  }

  @Test
  public void testVlanExtractionInvalid() {
    String hostname = "nxos_vlan_invalid";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getInterfaces(), anEmptyMap());
    assertThat(vc.getVlans(), anEmptyMap());
  }

  @Test
  public void testVlanReferences() throws IOException {
    String hostname = "nxos_vlan_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VLAN, "1", 1));
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VLAN, "2", 0));
    assertThat(ans, hasUndefinedReference(filename, CiscoNxosStructureType.VLAN, "3"));
  }

  @Test
  public void testVrfConversion() throws IOException {
    String hostname = "nxos_vrf";
    Configuration c = parseConfig(hostname);

    assertThat(c.getVrfs(), hasKeys(DEFAULT_VRF_NAME, NULL_VRF_NAME, "vrf1", "vrf3"));
    {
      org.batfish.datamodel.Vrf vrf = c.getVrfs().get(DEFAULT_VRF_NAME);
      assertThat(vrf, VrfMatchers.hasInterfaces(empty()));
    }
    {
      org.batfish.datamodel.Vrf vrf = c.getVrfs().get(NULL_VRF_NAME);
      assertThat(vrf, VrfMatchers.hasInterfaces(contains("Ethernet1/2")));
    }
    {
      org.batfish.datamodel.Vrf vrf = c.getVrfs().get("vrf1");
      assertThat(
          vrf, VrfMatchers.hasInterfaces(contains("Ethernet1/1", "Ethernet1/3", "Ethernet1/4")));
    }
    {
      org.batfish.datamodel.Vrf vrf = c.getVrfs().get("vrf3");
      assertThat(vrf, VrfMatchers.hasInterfaces(contains("Ethernet1/5")));
    }

    assertThat(
        c.getAllInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3", "Ethernet1/4", "Ethernet1/5"));
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/1");
      assertThat(iface, isActive());
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/2");
      assertThat(iface, isActive(false));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/3");
      assertThat(iface, isActive());
      assertThat(iface, hasAddress(nullValue()));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/4");
      assertThat(iface, isActive());
      assertThat(iface, hasAddress("10.0.4.1/24"));
    }
    {
      org.batfish.datamodel.Interface iface = c.getAllInterfaces().get("Ethernet1/5");
      assertThat(iface, isActive(false));
      assertThat(iface, hasAddress("10.0.5.1/24"));
    }
  }

  @Test
  public void testVrfExtraction() {
    String hostname = "nxos_vrf";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getVrfs(), hasKeys("vrf1", "vrf3"));
    {
      Vrf vrf = vc.getDefaultVrf();
      assertFalse(vrf.getShutdown());
    }
    {
      Vrf vrf = vc.getVrfs().get("vrf1");
      assertFalse(vrf.getShutdown());
      assertThat(
          vrf.getRd(), equalTo(RouteDistinguisherOrAuto.of(RouteDistinguisher.from(65001, 10L))));
      VrfAddressFamily af4 = vrf.getAddressFamily(AddressFamily.IPV4_UNICAST);
      assertThat(af4.getImportRtEvpn(), equalTo(RouteDistinguisherOrAuto.auto()));
      assertThat(af4.getExportRtEvpn(), equalTo(RouteDistinguisherOrAuto.auto()));
      assertThat(
          af4.getImportRt(),
          equalTo(RouteDistinguisherOrAuto.of(RouteDistinguisher.from(11, 65536L))));
      assertThat(af4.getExportRt(), nullValue());

      VrfAddressFamily af6 = vrf.getAddressFamily(AddressFamily.IPV6_UNICAST);
      assertThat(
          af6.getImportRtEvpn(),
          equalTo(RouteDistinguisherOrAuto.of(RouteDistinguisher.from(65001, 11L))));
      assertThat(
          af6.getExportRtEvpn(),
          equalTo(RouteDistinguisherOrAuto.of(RouteDistinguisher.from(65001, 11L))));
      assertThat(af6.getImportRt(), equalTo(RouteDistinguisherOrAuto.auto()));
      assertThat(af6.getExportRt(), equalTo(RouteDistinguisherOrAuto.auto()));
    }
    {
      Vrf vrf = vc.getVrfs().get("vrf3");
      assertTrue(vrf.getShutdown());
      assertThat(vrf.getRd(), equalTo(RouteDistinguisherOrAuto.auto()));
    }

    assertThat(
        vc.getInterfaces(),
        hasKeys("Ethernet1/1", "Ethernet1/2", "Ethernet1/3", "Ethernet1/4", "Ethernet1/5"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("vrf1"));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/2");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("vrf2"));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/3");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("vrf1"));
      assertThat(iface.getAddress(), nullValue());
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/4");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("vrf1"));
      assertThat(
          iface.getAddress().getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.4.1/24")));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("vrf3"));
      assertThat(
          iface.getAddress().getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.5.1/24")));
    }
  }

  @Test
  public void testVrfExtractionInvalid() {
    String hostname = "nxos_vrf_invalid";
    CiscoNxosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getVrfs(), hasKeys("vrf1"));
    assertThat(vc.getInterfaces(), hasKeys("Ethernet1/1"));
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/1");
      assertThat(iface.getVrfMember(), nullValue());
    }
  }

  @Test
  public void testVrfReferences() throws IOException {
    String hostname = "nxos_vrf_references";
    String filename = String.format("configs/%s", hostname);
    ConvertConfigurationAnswerElement ans =
        getBatfishForConfigurationNames(hostname).loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VRF, "vrf_used", 1));
    assertThat(ans, hasNumReferrers(filename, CiscoNxosStructureType.VRF, "vrf_unused", 0));
    assertThat(ans, hasUndefinedReference(filename, CiscoNxosStructureType.VRF, "vrf_undefined"));
  }
}

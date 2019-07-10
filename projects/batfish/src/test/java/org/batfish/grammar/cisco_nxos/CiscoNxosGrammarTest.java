package org.batfish.grammar.cisco_nxos;

import static com.google.common.collect.Maps.immutableEntry;
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
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAddress;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasBandwidth;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasChannelGroup;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasChannelGroupMembers;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDependencies;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDescription;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasInterfaceType;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
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
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TcpFlags;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.matchers.VrfMatchers;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.representation.cisco_nxos.ActionIpAccessListLine;
import org.batfish.representation.cisco_nxos.AddrGroupIpAddressSpec;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType;
import org.batfish.representation.cisco_nxos.CiscoNxosStructureType;
import org.batfish.representation.cisco_nxos.FragmentsBehavior;
import org.batfish.representation.cisco_nxos.IcmpOptions;
import org.batfish.representation.cisco_nxos.Interface;
import org.batfish.representation.cisco_nxos.IpAccessList;
import org.batfish.representation.cisco_nxos.IpAccessListLine;
import org.batfish.representation.cisco_nxos.IpAddressSpec;
import org.batfish.representation.cisco_nxos.Layer3Options;
import org.batfish.representation.cisco_nxos.LiteralIpAddressSpec;
import org.batfish.representation.cisco_nxos.LiteralPortSpec;
import org.batfish.representation.cisco_nxos.PortGroupPortSpec;
import org.batfish.representation.cisco_nxos.PortSpec;
import org.batfish.representation.cisco_nxos.StaticRoute;
import org.batfish.representation.cisco_nxos.TcpOptions;
import org.batfish.representation.cisco_nxos.UdpOptions;
import org.batfish.representation.cisco_nxos.Vrf;
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

    assertThat(iface.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.0.1/24")));
    assertThat(
        iface.getSecondaryAddresses(),
        containsInAnyOrder(
            ConcreteInterfaceAddress.parse("10.0.0.2/24"),
            ConcreteInterfaceAddress.parse("10.0.0.3/24")));
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
    // TODO: remove the leading space
    assertThat(
        eth11,
        hasDescription(" here is a description with punctuation! and IP address 1.2.3.4/24 etc."));
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
    assertThat(vc.getVlans(), hasKeys(2, 4, 6, 7, 8));
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
    }
    {
      Vrf vrf = vc.getVrfs().get("vrf3");
      assertTrue(vrf.getShutdown());
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
      assertThat(iface.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.4.1/24")));
    }
    {
      Interface iface = vc.getInterfaces().get("Ethernet1/5");
      assertFalse(iface.getShutdown());
      assertThat(iface.getVrfMember(), equalTo("vrf3"));
      assertThat(iface.getAddress(), equalTo(ConcreteInterfaceAddress.parse("10.0.5.1/24")));
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

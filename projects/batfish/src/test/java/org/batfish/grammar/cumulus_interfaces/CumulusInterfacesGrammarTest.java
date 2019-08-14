package org.batfish.grammar.cumulus_interfaces;

import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Set;
import org.batfish.common.Warnings;
import org.batfish.config.Settings;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.MacAddress;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.grammar.BatfishParseTreeWalker;
import org.batfish.grammar.cumulus_interfaces.CumulusInterfacesParser.Cumulus_interfaces_configurationContext;
import org.batfish.representation.cumulus.CumulusNcluConfiguration;
import org.batfish.representation.cumulus.CumulusStructureType;
import org.batfish.representation.cumulus.CumulusStructureUsage;
import org.batfish.representation.cumulus.InterfaceBridgeSettings;
import org.batfish.representation.cumulus.InterfaceClagSettings;
import org.batfish.representation.cumulus_interfaces.Interface;
import org.batfish.representation.cumulus_interfaces.Interfaces;
import org.junit.Before;
import org.junit.Test;

/** Test of {@link CumulusInterfacesParser}. */
public class CumulusInterfacesGrammarTest {
  private static final String FILENAME = "";
  private static CumulusNcluConfiguration CONFIG;

  @Before
  public void setup() {
    CONFIG = new CumulusNcluConfiguration();
    CONFIG.setFilename(FILENAME);
    CONFIG.setAnswerElement(new ConvertConfigurationAnswerElement());
    CONFIG.setWarnings(new Warnings());
  }

  private static DefinedStructureInfo getDefinedStructureInfo(
      CumulusStructureType type, String name) {
    return CONFIG
        .getAnswerElement()
        .getDefinedStructures()
        .get(FILENAME)
        .getOrDefault(type.getDescription(), ImmutableSortedMap.of())
        .get(name);
  }

  private static Set<Integer> getStructureReferences(
      CumulusStructureType type, String name, CumulusStructureUsage usage) {
    // The config keeps reference data in a private variable, and only copies into the answer
    // element when you set it.
    CONFIG.setAnswerElement(new ConvertConfigurationAnswerElement());
    return CONFIG
        .getAnswerElement()
        .getReferencedStructures()
        .get(FILENAME)
        .get(type.getDescription())
        .get(name)
        .get(usage.getDescription());
  }

  private static Interfaces parse(String input) {
    Settings settings = new Settings();
    settings.setDisableUnrecognized(true);
    settings.setThrowOnLexerError(true);
    settings.setThrowOnParserError(true);
    CumulusInterfacesCombinedParser parser =
        new CumulusInterfacesCombinedParser(input, settings, 1, 0);
    Cumulus_interfaces_configurationContext ctxt = parser.parse();
    CumulusInterfacesConfigurationBuilder configurationBuilder =
        new CumulusInterfacesConfigurationBuilder(CONFIG, parser, input, CONFIG.getWarnings());
    new BatfishParseTreeWalker(parser).walk(configurationBuilder, ctxt);
    return configurationBuilder.getInterfaces();
  }

  @Test
  public void testAuto() {
    String input = "auto swp1\n";
    Interfaces interfaces = parse(input);
    assertThat(interfaces.getAutoIfaces(), contains("swp1"));
  }

  @Test
  public void testBlankLines() {
    String input = "\n\n\n";
    parse(input);
  }

  @Test
  public void testIface() {
    String input = "iface swp1\n";
    Interfaces interfaces = parse(input);
    assertThat(interfaces.getInterfaces(), hasKeys("swp1"));
    assertThat(
        getDefinedStructureInfo(CumulusStructureType.INTERFACE, "swp1").getDefinitionLines(),
        contains(1));
  }

  @Test
  public void testIfaceDescription() {
    String description = "foo hey 123!#?<>";
    String input = "iface swp1\n alias " + description + "\n";
    Interface iface = parse(input).getInterfaces().get("swp1");
    assertEquals(iface.getDescription(), description);
  }

  @Test
  public void testBondLacpBypassAllow() {
    parse("iface swp1\n bond-lacp-bypass-allow yes\n");
  }

  @Test
  public void testIfaceBondSlaves() {
    String input = "iface swp1\n bond-slaves i2 i3 i4\n";
    Interfaces interfaces = parse(input);
    assertThat(
        getStructureReferences(
            CumulusStructureType.INTERFACE, "i2", CumulusStructureUsage.BOND_SLAVE),
        contains(2));
    assertThat(
        getStructureReferences(
            CumulusStructureType.INTERFACE, "i3", CumulusStructureUsage.BOND_SLAVE),
        contains(2));
    assertThat(
        getStructureReferences(
            CumulusStructureType.INTERFACE, "i4", CumulusStructureUsage.BOND_SLAVE),
        contains(2));
    assertThat(
        interfaces.getBondSlaveParents(),
        equalTo(
            ImmutableMap.of(
                "i2", "swp1", //
                "i3", "swp1", //
                "i4", "swp1")));
  }

  @Test
  public void testIfaceBondSlaves2() {
    String input = "iface swp1\n bond-slaves s1\n iface swp2\n bond-slaves s2\n";
    Interfaces interfaces = parse(input);
    assertThat(
        getStructureReferences(
            CumulusStructureType.INTERFACE, "s1", CumulusStructureUsage.BOND_SLAVE),
        contains(2));
    assertThat(
        getStructureReferences(
            CumulusStructureType.INTERFACE, "s2", CumulusStructureUsage.BOND_SLAVE),
        contains(4));
    assertThat(
        interfaces.getBondSlaveParents(),
        equalTo(
            ImmutableMap.of(
                "s1", "swp1", //
                "s2", "swp2")));
  }

  @Test
  public void testIfaceAddress() {
    String input = "iface swp1\n address 10.12.13.14/24\n";
    Interface iface = parse(input).getInterfaces().get("swp1");
    assertThat(iface.getAddresses(), contains(ConcreteInterfaceAddress.parse("10.12.13.14/24")));
  }

  @Test
  public void testIfaceAddressVirtual() {
    String input = "iface vlan1\n address-virtual 00:00:00:00:00:00 1.2.3.4/24\n";
    Interface iface = parse(input).getInterfaces().get("vlan1");
    assertThat(
        iface.getAddressVirtuals(),
        equalTo(
            ImmutableMap.of(
                MacAddress.parse("00:00:00:00:00:00"),
                ImmutableSet.of(ConcreteInterfaceAddress.parse("1.2.3.4/24")))));
  }

  @Test
  public void testIfaceBridgeAccess() {
    String input = "iface swp1\n bridge-access 1234\n";
    Interfaces interfaces = parse(input);
    InterfaceBridgeSettings bridgeSettings =
        interfaces.getInterfaces().get("swp1").getBridgeSettings();
    assertThat(bridgeSettings.getAccess(), equalTo(1234));
  }

  @Test
  public void testIfaceBridgeArpNdSuppress() {
    parse("iface vni1\n bridge-arp-nd-suppress on\n");
  }

  @Test
  public void testIfaceBridgeLearning() {
    parse("iface vni1\n bridge-learning on\n");
  }

  @Test
  public void testIfaceBridgePorts_multiline() {
    String input = "iface bridge\n bridge-ports i2 \\\n i3 i4\n";
    Interface iface = parse(input).getInterfaces().get("bridge");
    assertThat(iface.getBridgePorts(), contains("i2", "i3", "i4"));
  }

  @Test
  public void testIfaceBridgePorts() {
    String input = "iface bridge\n bridge-ports i2 i3 i4\n";
    Interface iface = parse(input).getInterfaces().get("bridge");
    assertThat(iface.getBridgePorts(), contains("i2", "i3", "i4"));
  }

  @Test
  public void testIfaceBridgePvid() {
    String input = "iface bridge\n bridge-pvid 1\n";
    InterfaceBridgeSettings bridgeSettings =
        parse(input).getInterfaces().get("bridge").getBridgeSettings();
    assertThat(bridgeSettings.getPvid(), equalTo(1));
  }

  @Test
  public void testIfaceBridgeVids() {
    String input = "iface swp1\n bridge-vids 1 2 3 4\n";
    InterfaceBridgeSettings bridgeSettings =
        parse(input).getInterfaces().get("swp1").getBridgeSettings();
    assertThat(bridgeSettings.getVids().enumerate(), contains(1, 2, 3, 4));
  }

  @Test
  public void testIfaceBridgeVlanAware_no() {
    parse("iface bridge\n bridge-vlan-aware no\n");
    assertThat(CONFIG.getWarnings().getParseWarnings().size(), equalTo(1));
  }

  @Test
  public void testIfaceBridgeVlanAware_yes() {
    parse("iface bridge\n bridge-vlan-aware yes\n");
    assertThat(CONFIG.getWarnings().getParseWarnings(), empty());
  }

  @Test
  public void testIfaceClagId() {
    String input = "iface swp1\n clag-id 123\n";
    Interface iface = parse(input).getInterfaces().get("swp1");
    assertThat(iface.getClagId(), equalTo(123));
  }

  @Test
  public void testIfaceClagBackupIpAndVrf() {
    String input = "iface swp1\n clagd-backup-ip 1.2.3.4 vrf v1\n";
    InterfaceClagSettings clag = parse(input).getInterfaces().get("swp1").getClagSettings();
    assertThat(clag.getBackupIp(), equalTo(Ip.parse("1.2.3.4")));
    assertThat(clag.getBackupIpVrf(), equalTo("v1"));
    assertThat(
        getStructureReferences(
            CumulusStructureType.VRF, "v1", CumulusStructureUsage.INTERFACE_CLAG_BACKUP_IP_VRF),
        contains(2));
  }

  @Test
  public void testIfaceClagdPeerIp() {
    String input = "iface swp1\n clagd-peer-ip 1.2.3.4\n";
    InterfaceClagSettings clag = parse(input).getInterfaces().get("swp1").getClagSettings();
    assertThat(clag.getPeerIp(), equalTo(Ip.parse("1.2.3.4")));
  }

  @Test
  public void testIfaceClagdPeerIpLinkLocal() {
    String input = "iface swp1\n clagd-peer-ip linklocal\n";
    InterfaceClagSettings clag = parse(input).getInterfaces().get("swp1").getClagSettings();
    assertTrue(clag.isPeerIpLinkLocal());
  }

  @Test
  public void testClagdSysMac() {
    String input = "iface swp1\n clagd-sys-mac 00:00:00:00:00:00\n";
    InterfaceClagSettings clag = parse(input).getInterfaces().get("swp1").getClagSettings();
    assertThat(clag.getSysMac(), equalTo(MacAddress.parse("00:00:00:00:00:00")));
  }

  @Test
  public void testIfaceHwaddress() {
    parse("iface vlan1\n hwaddress 00:00:00:00:00:00\n");
  }

  @Test
  public void testIfaceLinkSpeed() {
    String input = "iface swp1\n link-speed 10000\n";
    Interfaces interfaces = parse(input);
    Interface iface = interfaces.getInterfaces().get("swp1");
    assertThat(iface.getLinkSpeed(), equalTo(10000));
  }

  @Test
  public void testIfaceLinkSpeed_null() {
    String input = "iface swp1\n";
    Interfaces interfaces = parse(input);
    Interface iface = interfaces.getInterfaces().get("swp1");
    assertNull(iface.getLinkSpeed());
  }

  @Test
  public void testMstpctlBpduguard() {
    parse("iface vni1\n mstpctl-bpduguard yes\n");
  }

  @Test
  public void testMstpctlPortadminedge() {
    parse("iface vni1\n mstpctl-portadminedge yes\n");
  }

  @Test
  public void testMstpctlPortpdufilter() {
    parse("iface vni1\n mstpctl-portbpdufilter yes\n");
  }

  @Test
  public void testIfaceVlanId() {
    String input = "iface vlan1\n vlan-id 1\n";
    Interfaces interfaces = parse(input);
    Interface iface = interfaces.getInterfaces().get("vlan1");
    assertThat(iface.getVlanId(), equalTo(1));
    // not marked as an interface definition
    assertNull(getDefinedStructureInfo(CumulusStructureType.INTERFACE, "vlan1"));
    assertNotNull(getDefinedStructureInfo(CumulusStructureType.VLAN, "vlan1"));
  }

  @Test
  public void testIfaceVlanRawDevice() {
    String input = "iface vlan1\n vlan-raw-device bridge\n";
    Interfaces interfaces = parse(input);
    Interface iface = interfaces.getInterfaces().get("vlan1");
    assertThat(iface.getVlanRawDevice(), equalTo("bridge"));
  }

  @Test
  public void testIfaceVrf() {
    String input = "iface swp1\n vrf v1\n";
    Interfaces interfaces = parse(input);
    Interface iface = interfaces.getInterfaces().get("swp1");
    assertThat(iface.getVrf(), equalTo("v1"));
    assertThat(
        getStructureReferences(CumulusStructureType.VRF, "v1", CumulusStructureUsage.INTERFACE_VRF),
        contains(2));
  }

  @Test
  public void testIfaceVrfTable() {
    String input = "iface vrf1\n vrf-table auto\n";
    Interfaces interfaces = parse(input);
    Interface iface = interfaces.getInterfaces().get("vrf1");
    assertThat(iface.getVrfTable(), equalTo("auto"));
    // not marked as an interface definition
    assertNull(getDefinedStructureInfo(CumulusStructureType.INTERFACE, "vrf1"));
    assertNotNull(getDefinedStructureInfo(CumulusStructureType.VRF, "vrf1"));
  }

  @Test
  public void testVxlanId() {
    String input = "iface swp1\n vxlan-id 123\n";
    Interfaces interfaces = parse(input);
    Interface iface = interfaces.getInterfaces().get("swp1");
    assertThat(iface.getVxlanId(), equalTo(123));
    // not marked as an interface definition
    assertNull(getDefinedStructureInfo(CumulusStructureType.INTERFACE, "swp1"));
    assertNotNull(getDefinedStructureInfo(CumulusStructureType.VXLAN, "swp1"));
  }

  @Test
  public void testVxlanLocalTunnelIp() {
    String input = "iface swp1\n vxlan-local-tunnelip 1.2.3.4\n";
    Interfaces interfaces = parse(input);
    Interface iface = interfaces.getInterfaces().get("swp1");
    assertEquals(iface.getVxlanLocalTunnelIp(), Ip.parse("1.2.3.4"));
  }
}

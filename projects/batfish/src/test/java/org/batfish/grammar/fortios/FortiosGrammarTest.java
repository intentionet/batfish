package org.batfish.grammar.fortios;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.matchers.ParseWarningMatchers.hasComment;
import static org.batfish.common.matchers.ParseWarningMatchers.hasText;
import static org.batfish.common.matchers.WarningsMatchers.hasParseWarning;
import static org.batfish.common.matchers.WarningsMatchers.hasParseWarnings;
import static org.batfish.common.matchers.WarningsMatchers.hasRedFlags;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructure;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructureWithDefinitionLines;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasOutgoingFilter;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.accepts;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.rejects;
import static org.batfish.datamodel.matchers.MapMatchers.hasKeys;
import static org.batfish.main.BatfishTestUtils.TEST_SNAPSHOT;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.fortios.FortiosConfiguration.computeVrfName;
import static org.batfish.representation.fortios.FortiosPolicyConversions.computeOutgoingFilterName;
import static org.batfish.representation.fortios.FortiosPolicyConversions.getPolicyName;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import net.sf.javabdd.BDD;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.common.bdd.IpSpaceToBDD;
import org.batfish.common.bdd.PermitAndDenyBdds;
import org.batfish.common.matchers.WarningMatchers;
import org.batfish.common.plugin.IBatfish;
import org.batfish.config.Settings;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.BddTestbed;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpRange;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.Names;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.route.nh.NextHopInterface;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.representation.fortios.Address;
import org.batfish.representation.fortios.BgpNeighbor;
import org.batfish.representation.fortios.BgpProcess;
import org.batfish.representation.fortios.FortiosConfiguration;
import org.batfish.representation.fortios.FortiosStructureType;
import org.batfish.representation.fortios.FortiosStructureUsage;
import org.batfish.representation.fortios.Interface;
import org.batfish.representation.fortios.Interface.Status;
import org.batfish.representation.fortios.Interface.Type;
import org.batfish.representation.fortios.Policy;
import org.batfish.representation.fortios.Policy.Action;
import org.batfish.representation.fortios.Service;
import org.batfish.representation.fortios.Service.Protocol;
import org.batfish.representation.fortios.StaticRoute;
import org.batfish.representation.fortios.Zone;
import org.batfish.representation.fortios.Zone.IntrazoneAction;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public final class FortiosGrammarTest {

  @Test
  public void testHostnameExtraction() {
    String filename = "fortios_hostname";
    String hostname = "my_fortios-hostname1";
    assertThat(parseVendorConfig(filename).getHostname(), equalTo(hostname));
  }

  @Test
  public void testHostnameConversion() throws IOException {
    String filename = "fortios_hostname";
    String hostname = "my_fortios-hostname1";
    assertThat(parseTextConfigs(filename), hasEntry(equalTo(hostname), hasHostname(hostname)));
  }

  @Test
  public void testInvalidHostnameWithDotExtraction() {
    String filename = "fortios_bad_hostname";
    // invalid hostname from config file is thrown away
    assertThat(parseVendorConfig(filename).getHostname(), nullValue());
  }

  @Test
  public void testInvalidHostnameWithDotConversion() throws IOException {
    String filename = "fortios_bad_hostname";
    Batfish batfish = getBatfishForConfigurationNames(filename);
    Warnings warnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(warnings, hasParseWarning(hasComment("Illegal value for device hostname")));
  }

  @Test
  public void testReplacemsgExtraction() {
    String hostname = "fortios_replacemsg";
    String majorType = "admin";
    String minorTypePre = "pre_admin-disclaimer-text";
    String minorTypePost = "post_admin-disclaimer-text";
    FortiosConfiguration vc = parseVendorConfig(hostname);
    assertThat(
        vc.getReplacemsgs(), hasEntry(equalTo(majorType), hasKeys(minorTypePre, minorTypePost)));
    assertThat(
        vc.getReplacemsgs().get(majorType).get(minorTypePre).getBuffer(),
        equalTo("\"npre\"''\\\\nabc\\\\\\\" \"\nlastline"));
    assertThat(vc.getReplacemsgs().get(majorType).get(minorTypePost).getBuffer(), nullValue());
  }

  @Test
  public void testReplacemsgConversion() throws IOException {
    String filename = "fortios_replacemsg";
    Batfish batfish = getBatfishForConfigurationNames(filename);
    // Should see a single conversion warning for Ethernet1/1's conflicting speeds
    Warnings warnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(warnings, hasParseWarning(hasComment("Illegal value for replacemsg minor type")));
  }

  @Test
  public void testAddressExtraction() {
    String hostname = "address";
    FortiosConfiguration vc = parseVendorConfig(hostname);

    Map<String, Address> addresses = vc.getAddresses();
    assertThat(
        addresses,
        hasKeys(
            "ipmask",
            "ipmask-default",
            "iprange",
            "iprange-default",
            "fqdn",
            "dynamic",
            "geography",
            "interface-subnet",
            "mac",
            "wildcard",
            "wildcard-default",
            "undefined-refs",
            "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxy"));

    Address ipmask = addresses.get("ipmask");
    Address ipmaskDefault = addresses.get("ipmask-default");
    Address iprange = addresses.get("iprange");
    Address iprangeDefault = addresses.get("iprange-default");
    Address fqdn = addresses.get("fqdn");
    Address dynamic = addresses.get("dynamic");
    Address geography = addresses.get("geography");
    Address interfaceSubnet = addresses.get("interface-subnet");
    Address undefinedRefs = addresses.get("undefined-refs");
    Address mac = addresses.get("mac");
    Address wildcard = addresses.get("wildcard");
    Address wildcardDefault = addresses.get("wildcard-default");
    Address longName =
        addresses.get(
            "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxy");

    // Test types
    assertThat(ipmask.getType(), equalTo(Address.Type.IPMASK));
    assertThat(iprange.getType(), equalTo(Address.Type.IPRANGE));
    assertThat(interfaceSubnet.getType(), equalTo(Address.Type.INTERFACE_SUBNET));
    assertThat(wildcard.getType(), equalTo(Address.Type.WILDCARD));
    assertNull(longName.getType());
    assertThat(undefinedRefs.getType(), equalTo(Address.Type.INTERFACE_SUBNET));
    assertThat(dynamic.getType(), equalTo(Address.Type.DYNAMIC));
    assertThat(fqdn.getType(), equalTo(Address.Type.FQDN));
    assertThat(geography.getType(), equalTo(Address.Type.GEOGRAPHY));
    assertThat(mac.getType(), equalTo(Address.Type.MAC));

    // Test that type-specific fields are populated correctly
    assertThat(ipmask.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("1.1.1.0")));
    assertThat(ipmask.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("255.255.255.0")));
    assertThat(iprange.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("1.1.1.0")));
    assertThat(iprange.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("1.1.1.255")));
    assertThat(interfaceSubnet.getTypeSpecificFields().getInterface(), equalTo("port1"));
    // Configured IP is 2.2.2.2, but mask should canonicalize it to 2.0.0.2
    assertThat(wildcard.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("2.0.0.2")));
    assertThat(wildcard.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("255.0.0.255")));
    assertThat(longName.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("1.1.1.0")));
    assertThat(longName.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("255.255.255.0")));

    // Test that type-specific field defaults are correct
    assertThat(ipmaskDefault.getTypeSpecificFields().getIp1Effective(), equalTo(Ip.ZERO));
    assertThat(ipmaskDefault.getTypeSpecificFields().getIp2Effective(), equalTo(Ip.ZERO));
    assertThat(iprangeDefault.getTypeSpecificFields().getIp1Effective(), equalTo(Ip.ZERO));
    // Skip ip2 for iprange; end-ip must be set.
    assertThat(wildcardDefault.getTypeSpecificFields().getIp1Effective(), equalTo(Ip.ZERO));
    assertThat(wildcardDefault.getTypeSpecificFields().getIp2Effective(), equalTo(Ip.ZERO));

    // Test explicitly set values
    assertThat(ipmask.getAllowRouting(), equalTo(true));
    assertThat(ipmask.getAssociatedInterface(), equalTo("port1"));
    assertThat(ipmask.getComment(), equalTo("Hello world"));
    assertThat(ipmask.getFabricObject(), equalTo(true));

    // Test default values
    assertThat(longName.getTypeEffective(), equalTo(Address.Type.IPMASK));
    assertNull(longName.getAllowRouting());
    assertFalse(longName.getAllowRoutingEffective());
    assertNull(longName.getAssociatedInterface());
    assertNull(longName.getComment());
    assertNull(longName.getFabricObject());
    assertFalse(longName.getFabricObjectEffective());

    // Test that undefined structures are not added to Address object
    // TODO Also check that undefined references are filed (once they are filed)
    assertNull(undefinedRefs.getAssociatedInterface());
    assertNull(undefinedRefs.getTypeSpecificFields().getInterface());
  }

  @Test
  public void testAddressTypeSwitchingExtraction() throws IOException {
    String hostname = "address_type_switching";
    FortiosConfiguration vc = parseVendorConfig(hostname);

    Map<String, Address> addresses = vc.getAddresses();
    assertThat(
        addresses,
        hasKeys(
            "mask-to-range",
            "mask-to-wildcard",
            "range-to-mask",
            "range-to-wildcard",
            "wildcard-to-mask",
            "wildcard-to-range"));

    Address maskToRange = addresses.get("mask-to-range");
    Address maskToWildcard = addresses.get("mask-to-wildcard");
    Address rangeToMask = addresses.get("range-to-mask");
    Address rangeToWildcard = addresses.get("range-to-wildcard");
    Address wildcardToMask = addresses.get("wildcard-to-mask");
    Address wildcardToRange = addresses.get("wildcard-to-range");

    // Set subnet 1.2.2.0 1.1.1.255, then set type iprange.
    assertThat(maskToRange.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("1.0.0.0")));
    assertThat(maskToRange.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("1.1.1.255")));

    // Set subnet 1.2.2.0 1.1.1.255, then set type wildcard.
    assertThat(maskToWildcard.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("1.0.0.0")));
    assertThat(maskToWildcard.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("1.1.1.255")));

    // Set start-ip 255.1.1.1 and end-ip 255.0.0.0, then set type ipmask.
    // Switching type does not canonicalize subnet IP.
    assertThat(rangeToMask.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("255.1.1.1")));
    assertThat(rangeToMask.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("255.0.0.0")));

    // Set start-ip 255.1.1.1 and end-ip 128.0.255.0, then set type wildcard.
    // Switching type does not canonicalize wildcard IP.
    assertThat(rangeToWildcard.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("255.1.1.1")));
    assertThat(rangeToWildcard.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("128.0.255.0")));

    // Set wildcard 1.1.1.1 255.255.0.0, then set type ipmask.
    assertThat(wildcardToMask.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("1.1.0.0")));
    assertThat(wildcardToMask.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("255.255.0.0")));

    // Set wildcard 1.1.1.1 255.255.0.0, then set type iprange.
    assertThat(wildcardToRange.getTypeSpecificFields().getIp1(), equalTo(Ip.parse("1.1.0.0")));
    assertThat(wildcardToRange.getTypeSpecificFields().getIp2(), equalTo(Ip.parse("255.255.0.0")));
  }

  @Test
  public void testAddressWarnings() throws IOException {
    String hostname = "address_warnings";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings w =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());

    List<Matcher<Warnings.ParseWarning>> warningMatchers = new ArrayList<>();

    // Expect warnings for each illegal name used in the config
    Map<String, String> illegalNames =
        ImmutableMap.of(
            "address name",
            "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz",
            "zone or interface name",
            "abcdefghijklmnopqrstuvwxyz abcdefghi",
            "interface name",
            "abcdefghijklmnop");
    illegalNames.forEach(
        (nameType, name) ->
            warningMatchers.add(
                allOf(hasComment("Illegal value for " + nameType), hasText(containsString(name)))));
    warningMatchers.add(hasComment("Address edit block ignored: name is invalid"));

    // Expect warnings for each undefined reference in the config (in an otherwise legal context)
    warningMatchers.add(hasComment("No interface or zone named undefined_iface"));
    warningMatchers.add(hasComment("No interface named undefined_iface"));

    // Warn on all type-specific fields set for inappropriate types
    for (String f : ImmutableList.of("start-ip", "end-ip", "interface", "wildcard")) {
      warningMatchers.add(
          hasComment(String.format("Cannot set %s for address type %s", f, Address.Type.IPMASK)));
    }
    for (String f : ImmutableList.of("interface", "subnet", "wildcard")) {
      warningMatchers.add(
          hasComment(String.format("Cannot set %s for address type %s", f, Address.Type.IPRANGE)));
    }
    for (String f : ImmutableList.of("start-ip", "end-ip", "wildcard")) {
      warningMatchers.add(
          hasComment(
              String.format(
                  "Cannot set %s for address type %s", f, Address.Type.INTERFACE_SUBNET)));
    }
    for (String f : ImmutableList.of("start-ip", "end-ip", "interface", "subnet")) {
      warningMatchers.add(
          hasComment(String.format("Cannot set %s for address type %s", f, Address.Type.WILDCARD)));
    }
    for (String f : ImmutableList.of("start-ip", "end-ip", "interface", "subnet", "wildcard")) {
      // None of the type-specific fields are settable for any of the unsupported types
      for (Address.Type type :
          ImmutableList.of(
              Address.Type.DYNAMIC, Address.Type.FQDN, Address.Type.GEOGRAPHY, Address.Type.MAC)) {
        warningMatchers.add(
            hasComment(String.format("Cannot set %s for address type %s", f, type)));
      }
    }
    assertThat(w.getParseWarnings(), hasSize(warningMatchers.size()));
    warningMatchers.forEach(matcher -> assertThat(w, hasParseWarning(matcher)));

    // TODO None of the defined addresses are valid, so none should make it into VS.
    // Once this is correctly implemented, test that no addresses are in the VS config.
  }

  @Test
  public void testAddressConversion() throws IOException {
    String hostname = "address";
    Configuration c = parseConfig(hostname);

    Map<String, IpSpace> ipSpaces = c.getIpSpaces();
    String longName =
        "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxy";
    assertThat(
        ipSpaces,
        hasKeys(
            "ipmask",
            "ipmask-default",
            "iprange",
            "iprange-default",
            "fqdn",
            "dynamic",
            "geography",
            "interface-subnet",
            "mac",
            "wildcard",
            "wildcard-default",
            // TODO undefined-refs shouldn't be converted
            "undefined-refs",
            longName));

    BDD prefix1110 = Prefix.parse("1.1.1.0/24").toIpSpace().accept(_srcIpBdd);
    assertThat(ipSpaces.get("ipmask").accept(_srcIpBdd), equalTo(prefix1110));
    assertThat(ipSpaces.get("iprange").accept(_srcIpBdd), equalTo(prefix1110));
    assertThat(ipSpaces.get(longName).accept(_srcIpBdd), equalTo(prefix1110));
    assertThat(
        ipSpaces.get("wildcard").accept(_srcIpBdd),
        equalTo(
            // Configured mask is 255.0.0.255, but in IpWildcard, the set bits mean "don't care"
            IpWildcard.ipWithWildcardMask(Ip.parse("2.2.2.2"), Ip.parse("0.255.255.0"))
                .toIpSpace()
                .accept(_srcIpBdd)));

    BDD allSrcIps = UniverseIpSpace.INSTANCE.accept(_srcIpBdd);
    assertThat(ipSpaces.get("ipmask-default").accept(_srcIpBdd), equalTo(allSrcIps));
    assertThat(
        ipSpaces.get("iprange-default").accept(_srcIpBdd),
        equalTo(IpRange.range(Ip.ZERO, Ip.parse("1.1.1.1")).accept(_srcIpBdd)));
    assertThat(ipSpaces.get("wildcard-default").accept(_srcIpBdd), equalTo(allSrcIps));

    // Unsupported types
    Stream.of("fqdn", "dynamic", "geography", "interface-subnet", "mac", "undefined-refs")
        .forEach(t -> assertThat(ipSpaces.get(t).accept(_srcIpBdd), equalTo(_zero)));
  }

  @Test
  public void testAddressTypeSwitchingConversion() throws IOException {
    String hostname = "address_type_switching";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings w =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());

    assertThat(
        w.getParseWarnings(),
        containsInAnyOrder(
            // Set subnet 0.0.0.0/0, then set type iprange
            hasComment("Address edit block ignored: end-ip cannot be 0"),
            // Set type iprange, set start-ip to 1.1.1.1 and end-ip to 2.2.2.2, then set type ipmask
            hasComment("Address edit block ignored: 2.2.2.2 is not a valid subnet mask"),
            // Set type wildcard, set wildcard 1.1.1.1 255.0.255.0, then set type ipmask
            hasComment("Address edit block ignored: 255.0.255.0 is not a valid subnet mask"),
            // Set type iprange, set start-ip to 1.1.1.1 and end-ip to 0.0.255.255, set type
            // wildcard (non-canonical IP 1.1.1.1 is preserved), set type iprange
            hasComment("Address edit block ignored: end-ip must be greater than start-ip")));

    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    Map<String, IpSpace> ipSpaces = c.getIpSpaces();
    assertThat(
        ipSpaces,
        hasKeys(
            "mask-to-range",
            "mask-to-wildcard",
            "range-to-mask",
            "range-to-wildcard",
            "wildcard-to-mask",
            "wildcard-to-range"));

    IpSpace maskToRange = ipSpaces.get("mask-to-range");
    IpSpace maskToWildcard = ipSpaces.get("mask-to-wildcard");
    IpSpace rangeToMask = ipSpaces.get("range-to-mask");
    IpSpace rangeToWildcard = ipSpaces.get("range-to-wildcard");
    IpSpace wildcardToMask = ipSpaces.get("wildcard-to-mask");
    IpSpace wildcardToRange = ipSpaces.get("wildcard-to-range");

    // Set subnet 1.2.2.0 1.1.1.255, then set type iprange.
    assertThat(
        maskToRange.accept(_srcIpBdd),
        equalTo(IpRange.range(Ip.parse("1.0.0.0"), Ip.parse("1.1.1.255")).accept(_srcIpBdd)));

    // Set subnet 1.2.2.0 1.1.1.255, then set type wildcard.
    assertThat(
        maskToWildcard.accept(_srcIpBdd),
        equalTo(
            IpWildcard.ipWithWildcardMask(Ip.parse("1.0.0.0"), Ip.parse("1.1.1.255").inverted())
                .toIpSpace()
                .accept(_srcIpBdd)));

    // Set start-ip 255.1.1.1 and end-ip 255.0.0.0, then set type ipmask.
    assertThat(
        rangeToMask.accept(_srcIpBdd),
        equalTo(Prefix.parse("255.0.0.0/8").toIpSpace().accept(_srcIpBdd)));

    // Set start-ip 255.1.1.1 and end-ip 128.0.255.0, then set type wildcard.
    assertThat(
        rangeToWildcard.accept(_srcIpBdd),
        equalTo(
            IpWildcard.ipWithWildcardMask(Ip.parse("255.1.1.1"), Ip.parse("128.0.255.0").inverted())
                .toIpSpace()
                .accept(_srcIpBdd)));

    // Set wildcard 1.1.1.1 255.255.0.0, then set type ipmask.
    assertThat(
        wildcardToMask.accept(_srcIpBdd),
        equalTo(Prefix.parse("1.1.0.0/16").toIpSpace().accept(_srcIpBdd)));

    // Set wildcard 1.1.1.1 255.255.0.0, then set type iprange.
    assertThat(
        wildcardToRange.accept(_srcIpBdd),
        equalTo(IpRange.range(Ip.parse("1.1.0.0"), Ip.parse("255.255.0.0")).accept(_srcIpBdd)));
  }

  @Test
  public void testBgpExtraction() {
    String hostname = "bgp";
    FortiosConfiguration vc = parseVendorConfig(hostname);
    BgpProcess bgpProcess = vc.getBgpProcess();

    assertThat(bgpProcess.getAs(), equalTo(0L));
    assertThat(bgpProcess.getRouterId(), equalTo(Ip.parse("1.1.1.1")));

    Map<Ip, BgpNeighbor> neighbors = bgpProcess.getNeighbors();
    Ip ip2222 = Ip.parse("2.2.2.2");
    Ip ip3333 = Ip.parse("3.3.3.3");
    assertThat(neighbors.keySet(), containsInAnyOrder(ip2222, ip3333));
    BgpNeighbor neighbor2222 = neighbors.get(ip2222);
    BgpNeighbor neighbor3333 = neighbors.get(ip3333);
    assertThat(neighbor2222.getIp(), equalTo(ip2222));
    assertThat(neighbor3333.getIp(), equalTo(ip3333));
    assertThat(neighbor2222.getRemoteAs(), equalTo(1L));
    assertThat(neighbor3333.getRemoteAs(), equalTo(4294967295L));
  }

  @Test
  public void testBgpWarnings() throws IOException {
    String hostname = "bgp_warnings";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings parseWarnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        parseWarnings.getParseWarnings(),
        containsInAnyOrder(
            hasComment("Expected BGP AS in range 0-4294967295, but got '4294967296'"),
            hasComment("Cannot use 0.0.0.0 as BGP router-id"),
            hasComment("BGP neighbor edit block ignored: neighbor ID is invalid"),
            hasComment("Expected BGP remote AS in range 1-4294967295, but got '0'"),
            hasComment("Expected BGP remote AS in range 1-4294967295, but got '4294967296'"),
            hasComment("BGP neighbor edit block ignored: remote-as must be set"),
            hasComment("Redistribution into BGP is not yet supported")));
  }

  @Test
  public void testInterfaceExtraction() {
    String hostname = "iface";
    FortiosConfiguration vc = parseVendorConfig(hostname);

    Map<String, Interface> ifaces = vc.getInterfaces();
    assertThat(
        ifaces.keySet(),
        containsInAnyOrder(
            "port1",
            "port2",
            "longest if name",
            "tunnel",
            "loopback123",
            "agg",
            "emac",
            "redundant",
            "vlan",
            "wl"));

    Interface port1 = ifaces.get("port1");
    Interface port2 = ifaces.get("port2");
    Interface longName = ifaces.get("longest if name");
    Interface tunnel = ifaces.get("tunnel");
    Interface loopback = ifaces.get("loopback123");
    Interface agg = ifaces.get("agg");
    Interface emac = ifaces.get("emac");
    Interface redundant = ifaces.get("redundant");
    Interface vlan = ifaces.get("vlan");
    Interface wl = ifaces.get("wl");

    assertThat(port1.getVdom(), equalTo("root"));
    assertThat(port1.getIp(), equalTo(ConcreteInterfaceAddress.parse("192.168.122.2/24")));
    assertThat(port1.getType(), equalTo(Type.PHYSICAL));
    assertThat(port1.getAlias(), equalTo("longest possibl alias str"));
    assertThat(port1.getDescription(), equalTo("quoted description w/ spaces and more"));
    // Check defaults
    assertThat(port1.getStatus(), equalTo(Status.UNKNOWN));
    assertTrue(port1.getStatusEffective());
    assertThat(port1.getMtu(), nullValue());
    assertThat(port1.getMtuEffective(), equalTo(Interface.DEFAULT_INTERFACE_MTU));
    assertThat(port1.getMtuOverride(), nullValue());
    assertThat(port1.getVrf(), nullValue());
    assertThat(port1.getVrfEffective(), equalTo(0));

    assertThat(port2.getVdom(), equalTo("root"));
    assertThat(port2.getAlias(), equalTo("no_spaces"));
    assertThat(port2.getDescription(), equalTo("no_spaces_descr"));
    // Check overriding defaults
    assertThat(port2.getMtuOverride(), equalTo(true));
    assertThat(port2.getMtu(), equalTo(1234));
    assertThat(port2.getMtuEffective(), equalTo(1234));
    // Check default type
    assertThat(port2.getType(), nullValue());
    assertThat(port2.getTypeEffective(), equalTo(Type.VLAN));

    assertThat(longName.getIp(), equalTo(ConcreteInterfaceAddress.parse("169.254.1.1/24")));
    assertThat(longName.getAlias(), equalTo(""));
    // Check overriding defaults
    assertTrue(longName.getStatusEffective());
    assertThat(longName.getStatus(), equalTo(Status.UP));
    assertThat(longName.getVrf(), equalTo(31));
    assertThat(longName.getVrfEffective(), equalTo(31));

    assertThat(tunnel.getStatus(), equalTo(Status.DOWN));
    assertFalse(tunnel.getStatusEffective());
    assertThat(tunnel.getType(), equalTo(Type.TUNNEL));
    // MTU is set, but not used since override isn't set
    assertThat(tunnel.getMtuOverride(), nullValue());
    assertThat(tunnel.getMtu(), equalTo(65535));
    assertThat(tunnel.getMtuEffective(), equalTo(Interface.DEFAULT_INTERFACE_MTU));

    assertThat(loopback.getType(), equalTo(Type.LOOPBACK));
    assertThat(agg.getType(), equalTo(Type.AGGREGATE));
    assertThat(emac.getType(), equalTo(Type.EMAC_VLAN));
    assertThat(redundant.getType(), equalTo(Type.REDUNDANT));
    assertThat(vlan.getType(), equalTo(Type.VLAN));
    assertThat(wl.getType(), equalTo(Type.WL_MESH));
  }

  @Test
  public void testInterfaceConversion() throws IOException {
    String hostname = "iface";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings warnings =
        batfish
            .loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot())
            .getWarnings()
            .get(hostname);

    // AGGREGATE, REDUNDANT, and WL_MESH aren't yet supported; confirm in warnings
    assertThat(
        warnings.getRedFlagWarnings(),
        hasItems(
            WarningMatchers.hasText(
                String.format(
                    "Interface %s has unsupported type %s and will not be converted",
                    "agg", Type.AGGREGATE)),
            WarningMatchers.hasText(
                String.format(
                    "Interface %s has unsupported type %s and will not be converted",
                    "redundant", Type.REDUNDANT)),
            WarningMatchers.hasText(
                String.format(
                    "Interface %s has unsupported type %s and will not be converted",
                    "wl", Type.WL_MESH))));

    Configuration c = batfish.loadConfigurations(batfish.getSnapshot()).get(hostname);
    Map<String, org.batfish.datamodel.Interface> ifaces = c.getAllInterfaces();
    assertThat(
        ifaces.keySet(),
        containsInAnyOrder(
            "port1", "port2", "longest if name", "tunnel", "loopback123", "emac", "vlan"));
    org.batfish.datamodel.Interface port1 = ifaces.get("port1");
    org.batfish.datamodel.Interface port2 = ifaces.get("port2");
    org.batfish.datamodel.Interface longName = ifaces.get("longest if name");
    org.batfish.datamodel.Interface tunnel = ifaces.get("tunnel");
    org.batfish.datamodel.Interface loopback = ifaces.get("loopback123");
    org.batfish.datamodel.Interface emac = ifaces.get("emac");
    org.batfish.datamodel.Interface vlan = ifaces.get("vlan");

    // Check active
    assertFalse(tunnel.getActive()); // explicitly set to down
    Stream.of(port1, port2, longName, loopback, emac, vlan)
        .forEach(iface -> assertTrue(iface.getName() + " is up", iface.getActive()));

    // Check VRFs
    assertThat(longName.getVrf().getName(), equalTo(computeVrfName("root", 31)));
    String defaultVrf = computeVrfName("root", 0);
    Stream.of(port1, port2, tunnel, loopback, emac, vlan)
        .forEach(iface -> assertThat(iface.getVrf().getName(), equalTo(defaultVrf)));

    // Check addresses
    assertThat(port1.getAddress(), equalTo(ConcreteInterfaceAddress.parse("192.168.122.2/24")));
    assertThat(longName.getAddress(), equalTo(ConcreteInterfaceAddress.parse("169.254.1.1/24")));
    Stream.of(port2, tunnel, loopback, emac, vlan).forEach(iface -> assertNull(iface.getAddress()));

    // Check interface types
    assertThat(port1.getInterfaceType(), equalTo(InterfaceType.PHYSICAL));
    assertThat(port2.getInterfaceType(), equalTo(InterfaceType.VLAN));
    assertThat(longName.getInterfaceType(), equalTo(InterfaceType.VLAN));
    assertThat(tunnel.getInterfaceType(), equalTo(InterfaceType.TUNNEL));
    assertThat(loopback.getInterfaceType(), equalTo(InterfaceType.LOOPBACK));
    assertThat(emac.getInterfaceType(), equalTo(InterfaceType.VLAN));
    assertThat(vlan.getInterfaceType(), equalTo(InterfaceType.VLAN));

    // Check MTUs
    assertThat(port2.getMtu(), equalTo(1234));
    Stream.of(port1, longName, tunnel, loopback, emac, vlan)
        .forEach(iface -> assertThat(iface.getMtu(), equalTo(Interface.DEFAULT_INTERFACE_MTU)));

    // Check aliases
    assertThat(port1.getDeclaredNames(), contains("longest possibl alias str"));
    assertThat(port2.getDeclaredNames(), contains("no_spaces"));
    assertThat(longName.getDeclaredNames(), contains(""));
    Stream.of(tunnel, loopback, emac, vlan)
        .forEach(iface -> assertThat(iface.getDeclaredNames(), empty()));

    // Check descriptions
    assertThat(port1.getDescription(), equalTo("quoted description w/ spaces and more"));
    assertThat(port2.getDescription(), equalTo("no_spaces_descr"));
    Stream.of(longName, tunnel, loopback, emac, vlan)
        .forEach(iface -> assertNull(iface.getDescription()));
  }

  @Test
  public void testInterfaceWarnings() throws IOException {
    String hostname = "iface_warn";
    FortiosConfiguration vc = parseVendorConfig(hostname);
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings parseWarnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        parseWarnings,
        hasParseWarnings(
            containsInAnyOrder(
                allOf(
                    hasComment("Illegal value for interface name"),
                    hasText(containsString("name is too long for iface"))),
                allOf(
                    hasComment("Illegal value for interface alias"),
                    hasText(containsString("alias string is too long to associate with iface"))),
                hasComment("Interface edit block ignored: name conflicts with a zone name"))));

    // Also check extraction to make sure the conflicting-name lines are discarded, i.e. no VS
    // object is created when the name conflicts
    assertThat(vc.getInterfaces(), hasKeys("port1"));
  }

  @Test
  public void testZoneExtraction() {
    String hostname = "zone";
    FortiosConfiguration vc = parseVendorConfig(hostname);

    Map<String, Zone> zones = vc.getZones();
    assertThat(
        zones.keySet(),
        containsInAnyOrder("zone1", "zone2", "longest possible valid name for zon"));

    Zone zone1 = zones.get("zone1");
    Zone zone2 = zones.get("zone2");
    Zone zoneLongName = zones.get("longest possible valid name for zon");

    assertThat(zone1.getInterface(), containsInAnyOrder("port1", "port2"));
    // Defaults
    assertThat(zone1.getIntrazone(), nullValue());
    assertThat(zone1.getIntrazoneEffective(), equalTo(Zone.DEFAULT_INTRAZONE_ACTION));
    assertThat(zone1.getDescription(), nullValue());

    assertThat(zone2.getDescription(), equalTo("zone2 description"));
    assertThat(zone2.getIntrazone(), equalTo(IntrazoneAction.DENY));
    assertThat(zone2.getIntrazoneEffective(), equalTo(IntrazoneAction.DENY));
    assertThat(zone2.getInterface(), contains("port3"));

    assertThat(zoneLongName.getIntrazone(), equalTo(IntrazoneAction.ALLOW));
    assertThat(zoneLongName.getIntrazoneEffective(), equalTo(IntrazoneAction.ALLOW));
    assertThat(zoneLongName.getInterface(), containsInAnyOrder("port4", "port5"));
  }

  @Test
  public void testZoneConversion() throws IOException {
    String hostname = "zone";
    Configuration c = parseConfig(hostname);

    Map<String, org.batfish.datamodel.Zone> zones = c.getZones();
    assertThat(
        zones.keySet(),
        containsInAnyOrder("zone1", "zone2", "longest possible valid name for zon"));

    org.batfish.datamodel.Zone zone1 = zones.get("zone1");
    org.batfish.datamodel.Zone zone2 = zones.get("zone2");
    org.batfish.datamodel.Zone zoneLongName = zones.get("longest possible valid name for zon");

    assertThat(zone1.getInterfaces(), containsInAnyOrder("port1", "port2"));
    assertThat(zone2.getInterfaces(), contains("port3"));
    assertThat(zoneLongName.getInterfaces(), containsInAnyOrder("port4", "port5"));

    // Ensure that all zones' interfaces know what they're in
    Stream.of(zone1, zone2, zoneLongName)
        .forEach(
            zone ->
                zone.getInterfaces()
                    .forEach(
                        zoneIface ->
                            assertThat(
                                c.getAllInterfaces().get(zoneIface).getZoneName(),
                                equalTo(zone.getName()))));
  }

  @Test
  public void testZoneWarnings() throws IOException {
    String hostname = "zone_warn";

    FortiosConfiguration vc = parseVendorConfig(hostname);

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings parseWarnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        parseWarnings,
        hasParseWarnings(
            containsInAnyOrder(
                allOf(
                    hasComment("Zone edit block ignored: interface must be set"),
                    hasText(containsString("zone1"))),
                hasComment("Illegal value for zone name"),
                hasComment("Zone edit block ignored: name is invalid"),
                hasComment("Interface UNDEFINED is undefined and cannot be added to zone zone3"),
                hasComment(
                    "Interface port1 is already in another zone and cannot be added to zone"
                        + " zone4"),
                hasComment(
                    "Zone edit block ignored: name conflicts with a system interface name"))));

    // Also check extraction to make sure the right lines/blocks are discarded
    Map<String, Zone> zones = vc.getZones();
    assertThat(zones, hasKeys("zone3", "zone4"));

    Zone zone3 = zones.get("zone3");
    Zone zone4 = zones.get("zone4");

    assertThat(zone3.getInterface(), contains("port1"));
    // Only port2 should show up, since port1 is already taken
    assertThat(zone4.getInterface(), contains("port2"));
  }

  @Test
  public void testServiceCustomExtraction() {
    String hostname = "service_custom";
    FortiosConfiguration vc = parseVendorConfig(hostname);

    Map<String, Service> services = vc.getServices();
    assertThat(
        services.keySet(),
        containsInAnyOrder(
            "longest possible firewall service custom service name that is accepted by devic",
            "custom_tcp",
            "explicit_tcp",
            "src_port_defaults",
            "custom_icmp",
            "custom_icmp6",
            "custom_ip",
            "change_protocol"));

    Service serviceLongName =
        services.get(
            "longest possible firewall service custom service name that is accepted by devic");
    Service serviceTcpDefault = services.get("custom_tcp");
    Service serviceTcpExplicit = services.get("explicit_tcp");
    Service serviceSrcPortDefaults = services.get("src_port_defaults");
    Service serviceIcmp = services.get("custom_icmp");
    Service serviceIcmp6 = services.get("custom_icmp6");
    Service serviceIp = services.get("custom_ip");
    Service serviceChangeProtocol = services.get("change_protocol");

    assertThat(serviceLongName.getComment(), equalTo("service custom comment"));

    // Check default protocol
    assertThat(serviceTcpDefault.getProtocol(), nullValue());
    assertThat(serviceTcpDefault.getProtocolEffective(), equalTo(Service.DEFAULT_PROTOCOL));
    // Check defaults
    assertThat(serviceTcpDefault.getSctpPortRangeDst(), nullValue());
    assertThat(serviceTcpDefault.getSctpPortRangeSrc(), nullValue());
    assertThat(serviceTcpDefault.getTcpPortRangeSrc(), nullValue());
    assertThat(serviceTcpDefault.getUdpPortRangeDst(), nullValue());
    assertThat(serviceTcpDefault.getUdpPortRangeSrc(), nullValue());

    // Even with dest ports configured, source ports should still show up as default
    assertThat(serviceSrcPortDefaults.getSctpPortRangeSrc(), nullValue());
    assertThat(
        serviceSrcPortDefaults.getSctpPortRangeSrcEffective(),
        equalTo(Service.DEFAULT_SOURCE_PORT_RANGE));
    assertThat(serviceSrcPortDefaults.getTcpPortRangeSrc(), nullValue());
    assertThat(
        serviceSrcPortDefaults.getTcpPortRangeSrcEffective(),
        equalTo(Service.DEFAULT_SOURCE_PORT_RANGE));
    assertThat(serviceSrcPortDefaults.getUdpPortRangeSrc(), nullValue());
    assertThat(
        serviceSrcPortDefaults.getUdpPortRangeSrcEffective(),
        equalTo(Service.DEFAULT_SOURCE_PORT_RANGE));

    assertThat(serviceTcpExplicit.getProtocol(), equalTo(Protocol.TCP_UDP_SCTP));
    assertThat(serviceTcpExplicit.getProtocolEffective(), equalTo(Protocol.TCP_UDP_SCTP));
    // Check variety of port range syntax
    // TCP
    assertThat(serviceTcpDefault.getTcpPortRangeDst(), equalTo(IntegerSpace.of(1)));
    assertThat(
        serviceTcpExplicit.getTcpPortRangeDst(),
        equalTo(IntegerSpace.builder().including(1, 2, 10, 11, 13).build()));
    assertThat(
        serviceTcpExplicit.getTcpPortRangeSrc(),
        equalTo(IntegerSpace.builder().including(3, 4, 6, 7).build()));
    // UDP
    assertThat(
        serviceTcpExplicit.getUdpPortRangeDst(),
        equalTo(IntegerSpace.builder().including(100).build()));
    assertThat(serviceTcpExplicit.getUdpPortRangeSrc(), nullValue());
    // SCTP
    assertThat(
        serviceTcpExplicit.getSctpPortRangeDst(),
        equalTo(IntegerSpace.builder().including(200, 201).build()));
    assertThat(
        serviceTcpExplicit.getSctpPortRangeSrc(),
        equalTo(IntegerSpace.builder().including(300).build()));

    assertThat(serviceIcmp.getProtocol(), equalTo(Protocol.ICMP));
    assertThat(serviceIcmp.getProtocolEffective(), equalTo(Protocol.ICMP));
    assertThat(serviceIcmp.getIcmpCode(), equalTo(255));
    assertThat(serviceIcmp.getIcmpType(), equalTo(255));

    assertThat(serviceIcmp6.getProtocol(), equalTo(Protocol.ICMP6));
    assertThat(serviceIcmp6.getProtocolEffective(), equalTo(Protocol.ICMP6));
    // Check defaults
    assertThat(serviceIcmp6.getIcmpCode(), nullValue());
    assertThat(serviceIcmp6.getIcmpType(), nullValue());

    assertThat(serviceIp.getProtocol(), equalTo(Protocol.IP));
    assertThat(serviceIp.getProtocolEffective(), equalTo(Protocol.IP));
    assertThat(serviceIp.getProtocolNumber(), equalTo(254));
    assertThat(serviceIp.getProtocolNumberEffective(), equalTo(254));

    assertThat(serviceChangeProtocol.getProtocol(), equalTo(Protocol.IP));
    assertThat(serviceChangeProtocol.getProtocolEffective(), equalTo(Protocol.IP));
    // Should revert to default after changing protocol
    assertThat(serviceChangeProtocol.getProtocolNumber(), nullValue());
    assertThat(
        serviceChangeProtocol.getProtocolNumberEffective(),
        equalTo(Service.DEFAULT_PROTOCOL_NUMBER));
    // Check that other protocol's values were cleared
    assertThat(serviceChangeProtocol.getIcmpCode(), nullValue());
    assertThat(serviceChangeProtocol.getIcmpType(), nullValue());
    assertThat(serviceChangeProtocol.getSctpPortRangeDst(), nullValue());
    assertThat(serviceChangeProtocol.getSctpPortRangeSrc(), nullValue());
    assertThat(serviceChangeProtocol.getTcpPortRangeDst(), nullValue());
    assertThat(serviceChangeProtocol.getTcpPortRangeSrc(), nullValue());
    assertThat(serviceChangeProtocol.getUdpPortRangeDst(), nullValue());
    assertThat(serviceChangeProtocol.getUdpPortRangeSrc(), nullValue());
  }

  @Test
  public void testServiceWarnings() throws IOException {
    String hostname = "service_warnings";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings warnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        warnings,
        hasParseWarnings(
            containsInAnyOrder(
                hasComment("Illegal value for service name"),
                allOf(
                    hasComment("Service edit block ignored: name is invalid"),
                    hasText(
                        containsString(
                            "edit \"longer than longest possible firewall service custom service"
                                + " name that is accepted by device\""))),
                hasComment(
                    "Cannot set IP protocol number for service setting props for wrong protocol"
                        + " when protocol is not set to IP."),
                hasComment(
                    "Cannot set ICMP code for service setting props for wrong protocol when"
                        + " protocol is not set to ICMP or ICMP6."),
                hasComment(
                    "Cannot set ICMP type for service setting props for wrong protocol when"
                        + " protocol is not set to ICMP or ICMP6."),
                hasComment(
                    "Cannot set SCTP port range for service setting props for wrong protocol when"
                        + " protocol is not set to TCP/UDP/SCTP."),
                hasComment(
                    "Cannot set TCP port range for service setting props for wrong protocol when"
                        + " protocol is not set to TCP/UDP/SCTP."),
                hasComment(
                    "Cannot set UDP port range for service setting props for wrong protocol when"
                        + " protocol is not set to TCP/UDP/SCTP."),
                hasComment(
                    "Cannot set ICMP code for service icmp code before type when ICMP type is not"
                        + " set."))));
  }

  @Test
  public void testFirewallPolicyExtraction() {
    String hostname = "firewall_policy";
    FortiosConfiguration vc = parseVendorConfig(hostname);

    Map<String, Policy> policies = vc.getPolicies();
    assertThat(policies, hasKeys(contains("0", "4294967294", "1", "2", "3")));
    Policy policyDisable = policies.get("0");
    Policy policyDeny = policies.get("4294967294");
    Policy policyAllow = policies.get("1");
    Policy policyAny = policies.get("2");
    Policy policyZone = policies.get("3");

    Map<String, Service> services = vc.getServices();
    String service11 = "custom_tcp_11";
    String service12From11 = "custom_tcp_12_from_11";
    String serviceAll = "ALL";
    assertThat(services, hasKeys(containsInAnyOrder(service11, service12From11, serviceAll)));

    Map<String, Address> addresses = vc.getAddresses();
    String addr1 = "addr1";
    String addr2 = "addr2";
    String addrAll = "all";
    assertThat(addresses, hasKeys(containsInAnyOrder(addr1, addr2, addrAll)));

    Map<String, Interface> interfaces = vc.getInterfaces();
    String port1 = "port1";
    String port2 = "port2";
    String port3 = "port3";
    String port4 = "port4";
    String port5 = "port5";
    assertThat(interfaces, hasKeys(containsInAnyOrder(port1, port2, port3, port4, port5)));

    Map<String, Zone> zones = vc.getZones();
    String zone1 = "zone1";
    String zone2 = "zone2";
    String zone3 = "zone3";
    assertThat(zones, hasKeys(containsInAnyOrder(zone1, zone2, zone3)));

    assertThat(policyDisable.getAction(), equalTo(Action.DENY));
    assertThat(policyDisable.getStatus(), equalTo(Policy.Status.DISABLE));
    assertThat(policyDisable.getStatusEffective(), equalTo(Policy.Status.DISABLE));
    assertThat(policyDisable.getService(), contains(service11));
    assertThat(policyDisable.getSrcIntf(), contains(port1));
    assertThat(policyDisable.getDstIntf(), contains(port2));
    assertThat(policyDisable.getSrcAddr(), contains(addr1));
    assertThat(policyDisable.getDstAddr(), contains(addr2));

    assertThat(policyDeny.getAction(), nullValue());
    assertThat(policyDeny.getActionEffective(), equalTo(Action.DENY));
    assertThat(policyDeny.getComments(), equalTo("firewall policy comments"));
    assertThat(policyDeny.getName(), equalTo("longest allowed firewall policy nam"));
    assertThat(policyDeny.getStatus(), nullValue());
    assertThat(policyDeny.getStatusEffective(), equalTo(Policy.Status.ENABLE));
    assertThat(policyDeny.getService(), contains(service12From11));
    assertThat(policyDeny.getSrcIntf(), contains(port1));
    assertThat(policyDeny.getDstIntf(), containsInAnyOrder(port1, port2));
    assertThat(policyDeny.getSrcAddr(), contains(addr1));
    assertThat(policyDeny.getDstAddr(), contains(addr2));

    assertThat(policyAllow.getAction(), equalTo(Action.ACCEPT));
    assertThat(policyAllow.getStatus(), equalTo(Policy.Status.ENABLE));
    assertThat(policyAllow.getStatusEffective(), equalTo(Policy.Status.ENABLE));
    assertThat(policyAllow.getService(), containsInAnyOrder(service11, service12From11));
    assertThat(policyAllow.getSrcIntf(), containsInAnyOrder(port1, port2));
    assertThat(policyAllow.getDstIntf(), containsInAnyOrder(port1, port2));
    assertThat(policyAllow.getSrcAddr(), containsInAnyOrder(addr1, addr2));
    assertThat(policyAllow.getDstAddr(), containsInAnyOrder(addr1, addr2));

    assertThat(policyAny.getService(), contains(serviceAll));
    assertThat(policyAny.getSrcAddr(), contains(addrAll));
    assertThat(policyAny.getDstAddr(), contains(addrAll));
    assertThat(policyAny.getSrcIntf(), contains(Policy.ANY_INTERFACE));
    assertThat(policyAny.getDstIntf(), contains(Policy.ANY_INTERFACE));

    assertThat(policyZone.getSrcIntfZones(), containsInAnyOrder(zone1, zone2, zone3));
    assertThat(policyZone.getSrcIntf(), contains(port2));
    assertThat(policyZone.getDstIntfZones(), containsInAnyOrder(zone1, zone3));
  }

  @Test
  public void testFirewallPolicyConversion() {
    String hostname = "firewall_policy";
    FortiosConfiguration vc = parseVendorConfig(hostname);

    // Policy 0 should not be converted because it's disabled
    String denyName = "4294967294";
    String allowName = "1";
    String anyName = "2";
    String zonePolicyName = "3";

    Map<String, AclLine> convertedPolicies = vc.getConvertedPolicies(vc.getAddresses().keySet());
    assertThat(convertedPolicies, hasKeys(denyName, allowName, anyName, zonePolicyName));

    AclLine deny = convertedPolicies.get(denyName);
    AclLine allow = convertedPolicies.get(allowName);
    AclLine any = convertedPolicies.get(anyName);
    AclLine zonePolicy = convertedPolicies.get(zonePolicyName);

    assertThat(
        deny.getName(), equalTo(getPolicyName(denyName, "longest allowed firewall policy nam")));
    assertThat(allow.getName(), equalTo(getPolicyName(allowName, "Permit Custom TCP Traffic")));
    assertThat(any.getName(), equalTo(getPolicyName(anyName, null)));
    assertThat(zonePolicy.getName(), equalTo(getPolicyName(zonePolicyName, null)));

    // Create IpAccessListToBdd to convert ACLs.
    Map<String, IpSpace> namedIpSpaces =
        ImmutableMap.of(
            "addr1",
            Prefix.parse("10.0.1.0/24").toIpSpace(),
            "addr2",
            Prefix.parse("10.0.2.0/24").toIpSpace(),
            "all",
            UniverseIpSpace.INSTANCE);
    IpAccessListToBdd aclToBdd =
        new IpAccessListToBddImpl(
            _pkt, BDDSourceManager.empty(_pkt), ImmutableMap.of(), namedIpSpaces);

    // Make BDDs representing components of defined policies
    BDD addr1AsSrc = _srcIpBdd.toBDD(Prefix.parse("10.0.1.0/24"));
    BDD addr2AsSrc = _srcIpBdd.toBDD(Prefix.parse("10.0.2.0/24"));
    BDD addr1AsDst = _dstIpBdd.toBDD(Prefix.parse("10.0.1.0/24"));
    BDD addr2AsDst = _dstIpBdd.toBDD(Prefix.parse("10.0.2.0/24"));
    BDD service11 =
        _bddTestbed.toBDD(
            HeaderSpace.builder()
                .setIpProtocols(IpProtocol.TCP)
                .setSrcPorts(Service.DEFAULT_SOURCE_PORT_RANGE.getSubRanges())
                .setDstPorts(SubRange.singleton(11))
                .build());
    BDD service12From11 =
        _bddTestbed.toBDD(
            HeaderSpace.builder()
                .setIpProtocols(IpProtocol.TCP)
                .setSrcPorts(SubRange.singleton(11))
                .setDstPorts(SubRange.singleton(12))
                .build());
    {
      // Deny service custom_tcp_12_from_11 from addr1 to addr2
      PermitAndDenyBdds expected =
          new PermitAndDenyBdds(_zero, addr1AsSrc.and(addr2AsDst).and(service12From11));
      assertThat(aclToBdd.toPermitAndDenyBdds(deny), equalTo(expected));
    }
    {
      // Allow services custom_tcp_11, custom_tcp_11_from_12 from addr1, addr2 to addr1, addr2
      BDD services = service11.or(service12From11);
      BDD srcAddrs = addr1AsSrc.or(addr2AsSrc);
      BDD dstAddrs = addr1AsDst.or(addr2AsDst);
      PermitAndDenyBdds expected =
          new PermitAndDenyBdds(services.and(srcAddrs).and(dstAddrs), _zero);
      assertThat(aclToBdd.toPermitAndDenyBdds(allow), equalTo(expected));
    }
    {
      // Allow all
      PermitAndDenyBdds expected = new PermitAndDenyBdds(_one, _zero);
      assertThat(aclToBdd.toPermitAndDenyBdds(any), equalTo(expected));
      assertThat(aclToBdd.toPermitAndDenyBdds(zonePolicy), equalTo(expected));
    }
  }

  @Test
  public void testViAcls() throws IOException {
    String hostname = "firewall_vi_policy";
    Configuration c = parseConfig(hostname);

    // Configuration contains unzoned interface port1 and zone zone1 containing port2 and port3
    String port1IntrazoneName = Names.zoneToZoneFilter("port1", "port1");
    String zone1IntrazoneName = Names.zoneToZoneFilter("zone1", "zone1");
    String port1ToZone1Name = Names.zoneToZoneFilter("port1", "zone1");
    String zone1ToPort1Name = Names.zoneToZoneFilter("zone1", "port1");
    String port1OutgoingName = computeOutgoingFilterName("interface", "port1");
    String zone1OutgoingName = computeOutgoingFilterName("zone", "zone1");

    Map<String, IpAccessList> acls = c.getIpAccessLists();
    assertThat(
        acls.keySet(),
        containsInAnyOrder(
            port1IntrazoneName,
            zone1IntrazoneName,
            port1ToZone1Name,
            zone1ToPort1Name,
            port1OutgoingName,
            zone1OutgoingName));

    IpAccessList port1Intrazone = acls.get(port1IntrazoneName);
    IpAccessList zone1Intrazone = acls.get(zone1IntrazoneName);
    IpAccessList port1ToZone1 = acls.get(port1ToZone1Name);
    IpAccessList zone1ToPort1 = acls.get(zone1ToPort1Name);
    IpAccessList port1Outgoing = acls.get(port1OutgoingName);
    IpAccessList zone1Outgoing = acls.get(zone1OutgoingName);

    // Create IpAccessListToBdd to convert ACLs.
    Prefix addr1 = Prefix.parse("10.0.1.0/24");
    Prefix addr2 = Prefix.parse("10.0.0.0/16");
    Map<String, IpSpace> namedIpSpaces =
        ImmutableMap.of(
            "addr1",
            addr1.toIpSpace(),
            "addr2",
            addr2.toIpSpace(),
            "all",
            UniverseIpSpace.INSTANCE);
    BDDSourceManager srcMgr =
        BDDSourceManager.forInterfaces(_pkt, c.getActiveInterfaces().keySet());
    IpAccessListToBdd aclToBdd = new IpAccessListToBddImpl(_pkt, srcMgr, acls, namedIpSpaces);

    // Make BDDs representing components of defined policies
    BDD addr1AsDst = _dstIpBdd.toBDD(addr1);
    BDD addr2AsDst = _dstIpBdd.toBDD(addr2);

    // No policies apply to traffic from port1 to port1
    assertThat(aclToBdd.toBdd(port1Intrazone), equalTo(_zero));

    // Only policy 2 applies to zone1 intrazone traffic
    assertThat(aclToBdd.toBdd(zone1Intrazone), equalTo(addr2AsDst));

    // Policy 1 denies 10.0.1.0/24, then policy 2 permits 10.0.0.0/16
    assertThat(aclToBdd.toBdd(port1ToZone1), equalTo(addr2AsDst.diff(addr1AsDst)));

    // No policies apply to traffic from zone1 to port1
    assertThat(aclToBdd.toBdd(zone1ToPort1), equalTo(_zero));

    // No policies apply to traffic leaving port1
    assertThat(aclToBdd.toBdd(port1Outgoing), equalTo(_zero));

    // Should reflect that policy 1 blocks traffic from port1 to addr1, and that traffic from zone1
    // to addr2 is permitted
    BDD fromPort1 = srcMgr.getSourceInterfaceBDD("port1");
    BDD fromZone1 = srcMgr.getSourceInterfaceBDD("port2").or(srcMgr.getSourceInterfaceBDD("port3"));
    BDD permittedFromPort1 = fromPort1.and(addr2AsDst.diff(addr1AsDst));
    BDD permittedFromZone1 = fromZone1.and(addr2AsDst);
    assertThat(aclToBdd.toBdd(zone1Outgoing), equalTo(permittedFromPort1.or(permittedFromZone1)));
  }

  /**
   * Test extraction of firewall policy when warnings are generated for invalid / pruned properties.
   */
  @Test
  public void testFirewallPolicyExtractionWithWarnings() {
    String hostname = "firewall_policy_warn";
    FortiosConfiguration vc = parseVendorConfig(hostname);

    Map<String, Policy> policies = vc.getPolicies();
    assertThat(policies, hasKeys(containsInAnyOrder("1", "3")));
    Policy policy = policies.get("1");

    Map<String, Service> services = vc.getServices();
    String service10 = "service10";
    String service20 = "service20";
    assertThat(services, hasKeys(containsInAnyOrder(service10, service20)));

    Map<String, Address> addresses = vc.getAddresses();
    String addr10 = "addr10";
    String addr20 = "addr20";
    assertThat(addresses, hasKeys(containsInAnyOrder(addr10, addr20)));

    Map<String, Interface> interfaces = vc.getInterfaces();
    String port10 = "port10";
    String port20 = "port20";
    assertThat(interfaces, hasKeys(containsInAnyOrder(port10, port20)));

    // Confirm invalid any/all specifiers are dropped when appropriate
    assertThat(policy.getSrcIntf(), contains(port10));
    assertThat(policy.getSrcAddr(), contains(addr10));
    assertThat(policy.getDstAddr(), contains(addr20));
    // Confirm a line with invalid ALL service is ignored; i.e. previous value isn't overwritten
    assertThat(policy.getService(), contains(service20));
    // Confirm a line with dstintf combining any and another interface is accepted
    assertThat(policy.getDstIntf(), containsInAnyOrder(Policy.ANY_INTERFACE, port20));
  }

  @Test
  public void testFirewallPolicyWarnings() throws IOException {
    String hostname = "firewall_policy_warn";

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings parseWarnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        parseWarnings,
        hasParseWarnings(
            containsInAnyOrder(
                hasComment("Expected policy number in range 0-4294967294, but got '4294967295'"),
                hasComment("Expected policy number in range 0-4294967294, but got 'not_a_number'"),
                hasComment("Illegal value for policy name"),
                allOf(
                    hasComment("Policy edit block ignored: name is invalid"),
                    hasText(containsString("4294967295"))),
                allOf(
                    hasComment("Policy edit block ignored: name is invalid"),
                    hasText(containsString("not_a_number"))),
                allOf(
                    hasComment("Policy edit block ignored: service must be set"),
                    hasText(containsString("edit 2"))),
                hasComment(
                    "Interface/zone port1 is undefined and cannot be added to policy 4294967295"),
                hasComment(
                    "Interface/zone port2 is undefined and cannot be added to policy 4294967295"),
                hasComment(
                    "Interface/zone port3 is undefined and cannot be added to policy 4294967295"),
                hasComment(
                    "Interface/zone port4 is undefined and cannot be added to policy 4294967295"),
                hasComment(
                    "Service service1 is undefined and cannot be added to policy 4294967295"),
                hasComment(
                    "Service service2 is undefined and cannot be added to policy 4294967295"),
                hasComment("Address addr1 is undefined and cannot be added to policy 4294967295"),
                hasComment("Address addr2 is undefined and cannot be added to policy 4294967295"),
                hasComment("Address addr3 is undefined and cannot be added to policy 4294967295"),
                hasComment("Address addr4 is undefined and cannot be added to policy 4294967295"),
                hasComment("Cannot combine 'ALL' with other services"),
                allOf(
                    hasComment("When 'all' is set together with other address(es), it is removed"),
                    hasText("addr10 all")),
                allOf(
                    hasComment("When 'all' is set together with other address(es), it is removed"),
                    hasText("addr20 all")),
                allOf(
                    hasComment("When 'any' is set together with other interfaces, it is removed"),
                    hasText("any port10")))));

    Warnings conversionWarnings =
        batfish
            .loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot())
            .getWarnings()
            .get(hostname);
    assertThat(
        conversionWarnings,
        hasRedFlags(
            contains(WarningMatchers.hasText("Ignoring policy 3: Action IPSEC is not supported"))));
  }

  @Test
  public void testStaticRouteExtraction() {
    String hostname = "static_routes";
    FortiosConfiguration vc = parseVendorConfig(hostname);

    assertThat(vc.getStaticRoutes(), hasKeys("0", "1", "2", "4294967295"));

    // All values explicitly configured
    StaticRoute r0 = vc.getStaticRoutes().get("0");
    assertThat(r0.getDevice(), equalTo("port1"));
    assertThat(r0.getDistance(), equalTo(20));
    assertThat(r0.getDst(), equalTo(Prefix.parse("1.1.1.0/24")));
    assertThat(r0.getGateway(), equalTo(Ip.parse("2.2.2.2")));
    assertThat(r0.getSdwanEnabled(), equalTo(false));
    assertThat(r0.getStatus(), equalTo(StaticRoute.Status.ENABLE));

    // All values default except device (which is required to be set)
    StaticRoute r1 = vc.getStaticRoutes().get("1");
    assertThat(r1.getDevice(), equalTo("port1"));
    assertNull(r1.getDistance());
    assertThat(r1.getDistanceEffective(), equalTo(StaticRoute.DEFAULT_DISTANCE));
    assertNull(r1.getDst());
    assertThat(r1.getDstEffective(), equalTo(StaticRoute.DEFAULT_DST));
    assertNull(r1.getGateway());
    assertNull(r1.getSdwanEnabled());
    assertFalse(r1.getSdwanEnabledEffective());
    assertNull(r1.getStatus());
    assertTrue(r1.getStatusEffective());

    // SD-WAN enabled; default distance should reflect that
    StaticRoute r2 = vc.getStaticRoutes().get("2");
    assertThat(r2.getSdwanEnabled(), equalTo(true));
    assertThat(r2.getDistanceEffective(), equalTo(StaticRoute.DEFAULT_DISTANCE_SDWAN));

    // Disabled (also max seq num)
    StaticRoute r4294967295 = vc.getStaticRoutes().get("4294967295");
    assertThat(r4294967295.getStatus(), equalTo(StaticRoute.Status.DISABLE));
  }

  @Test
  public void testStaticRouteConversion() throws IOException {
    String hostname = "static_routes";
    Configuration c = parseConfig(hostname);

    Set<org.batfish.datamodel.StaticRoute> staticRoutes =
        c.getVrfs().get(computeVrfName("root", Interface.DEFAULT_VRF)).getStaticRoutes();

    org.batfish.datamodel.StaticRoute expected0 =
        org.batfish.datamodel.StaticRoute.builder()
            .setAdmin(20)
            .setNetwork(Prefix.parse("1.1.1.0/24"))
            .setNextHop(NextHopInterface.of("port1", Ip.parse("2.2.2.2")))
            .build();
    org.batfish.datamodel.StaticRoute expected1 =
        org.batfish.datamodel.StaticRoute.builder()
            .setAdmin(StaticRoute.DEFAULT_DISTANCE)
            .setNetwork(Prefix.ZERO)
            .setNextHop(NextHopInterface.of("port1"))
            .build();
    org.batfish.datamodel.StaticRoute expected2 =
        org.batfish.datamodel.StaticRoute.builder()
            .setAdmin(StaticRoute.DEFAULT_DISTANCE_SDWAN)
            .setNetwork(Prefix.parse("2.2.2.0/24"))
            .setNextHop(NextHopInterface.of("port1"))
            .build();

    // Route 4294967295 doesn't get converted because it's disabled
    assertThat(staticRoutes, containsInAnyOrder(expected0, expected1, expected2));
  }

  @Test
  public void testStaticRouteWarnings() throws IOException {
    String hostname = "static_route_warnings";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings parseWarnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        parseWarnings,
        hasParseWarnings(
            containsInAnyOrder(
                hasComment(
                    "Expected static route sequence number in range 0-4294967295, but got"
                        + " '4294967296'"),
                hasComment(
                    "Expected static route sequence number in range 0-4294967295, but got"
                        + " 'not_a_number'"),
                allOf(
                    hasComment("Static route edit block ignored: sequence number is invalid"),
                    hasText(containsString("edit 4294967296"))),
                allOf(
                    hasComment("Static route edit block ignored: sequence number is invalid"),
                    hasText(containsString("edit not_a_number"))),
                // Route 1 tries to set device to port1 (undefined) and an invalid interface name
                hasComment("Interface port1 is undefined"),
                allOf(
                    hasComment("Static route edit block ignored: device must be set"),
                    hasText(containsString("edit 1"))))));

    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());
    assertThat(
        ccae,
        hasUndefinedReference(
            "configs/" + hostname,
            FortiosStructureType.INTERFACE,
            "port1",
            FortiosStructureUsage.STATIC_ROUTE_DEVICE));
  }

  @Test
  public void testSystemRecovery() throws IOException {
    String hostname = "fortios_system_recovery";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.getSettings().setDisableUnrecognized(false);
    FortiosConfiguration vc =
        (FortiosConfiguration)
            batfish.loadVendorConfigurations(batfish.getSnapshot()).get(hostname);
    assertThat(vc.getInterfaces(), hasKeys("port1"));

    // make sure the line was actually unrecognized
    Warnings warnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        warnings,
        hasParseWarning(
            allOf(
                hasComment("This syntax is unrecognized"),
                hasText("set APropertyThatHopefullyDoesNotExist to a bunch of garbage"))));
  }

  @Test
  public void testRename() throws IOException {
    String hostname = "rename";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    // batfish.getSettings().setDisableUnrecognized(false);
    FortiosConfiguration vc =
        (FortiosConfiguration)
            batfish.loadVendorConfigurations(batfish.getSnapshot()).get(hostname);

    // Policy 1 should not convert because it doesn't have any valid src or dst addresses
    assertThat(vc.getPolicies(), hasKeys("0"));
    assertThat(vc.getAddresses(), hasKeys("new_addr1", "new_addr2"));
    assertThat(vc.getServices(), hasKeys("new_service1", "new_service2"));
    assertThat(vc.getZones(), hasKeys("new_zone1", "new_zone2"));

    Policy policy = vc.getPolicies().get("0");
    // Policy should be using renamed structures
    // Whether or not they were renamed after initial reference
    assertThat(policy.getSrcIntfZones(), contains("new_zone1"));
    assertThat(policy.getDstIntfZones(), contains("new_zone2"));
    assertThat(policy.getSrcAddr(), contains("new_addr1"));
    assertThat(policy.getDstAddr(), contains("new_addr2"));
    assertThat(policy.getService(), containsInAnyOrder("new_service1", "new_service2"));
  }

  @Test
  public void testRenameWarnings() throws IOException {
    String hostname = "rename";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Warnings warnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());

    // Should get warnings when trying to use a renamed structure
    // Or trying to use an undefined structure that will be defined (renamed) later
    assertThat(
        warnings,
        hasParseWarnings(
            containsInAnyOrder(
                hasComment("Address old_addr1 is undefined and cannot be added to policy 1"),
                hasComment("Address new_addr2 is undefined and cannot be added to policy 1"),
                hasComment("Interface/zone old_zone1 is undefined and cannot be added to policy 1"),
                hasComment("Interface/zone new_zone2 is undefined and cannot be added to policy 1"),
                hasComment("Service old_service1 is undefined and cannot be added to policy 1"),
                hasComment("Service new_service2 is undefined and cannot be added to policy 1"),
                hasComment("Cannot rename non-existent address undefined"),
                hasComment("Cannot rename non-existent service custom undefined"),
                hasComment("Cannot rename non-existent zone undefined"),
                hasComment(
                    "Renaming zone new_zone1 conflicts with an existing object port1, ignoring"
                        + " this rename operation"),
                allOf(
                    hasComment("Illegal value for zone name"),
                    hasText(containsString("a name that is too long to use for this object type"))),
                hasComment("Policy edit block ignored: srcintf must be set"))));
  }

  @Test
  public void testRenameReferences() throws IOException {
    String hostname = "rename";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    // Should have definitions for the renamed structures
    assertThat(ccae, hasDefinedStructure(filename, FortiosStructureType.ADDRESS, "new_addr1"));
    assertThat(
        ccae, hasDefinedStructure(filename, FortiosStructureType.SERVICE_CUSTOM, "new_service1"));

    // Rename should be part of the definitions
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, FortiosStructureType.ADDRESS, "new_addr2", contains(18, 19, 20, 74)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, FortiosStructureType.SERVICE_CUSTOM, "new_service2", contains(8, 9, 10, 71)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, FortiosStructureType.ZONE, "new_zone2", contains(39, 40, 41, 77)));

    // Should have references for the renamed structures, even if the renaming happened after the
    // reference
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.ADDRESS, "new_addr1", 1));
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.ADDRESS, "new_addr2", 1));
    assertThat(
        ccae, hasNumReferrers(filename, FortiosStructureType.SERVICE_CUSTOM, "new_service1", 1));
    assertThat(
        ccae, hasNumReferrers(filename, FortiosStructureType.SERVICE_CUSTOM, "new_service2", 1));

    // Should have undefined references where either:
    //   1. New names are used before the structure is renamed
    //   2. Old names are used after the structure is renamed
    assertThat(
        ccae,
        hasUndefinedReference(filename, FortiosStructureType.ADDRESS_OR_ADDRGRP, "old_addr1"));
    assertThat(
        ccae,
        hasUndefinedReference(filename, FortiosStructureType.ADDRESS_OR_ADDRGRP, "new_addr2"));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, FortiosStructureType.SERVICE_CUSTOM_OR_SERVICE_GROUP, "old_service1"));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, FortiosStructureType.SERVICE_CUSTOM_OR_SERVICE_GROUP, "new_service2"));
  }

  @Test
  public void testPolicyDefinitionsAndReferences() throws IOException {
    String hostname = "policy_defs_refs";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse(batfish.getSnapshot());

    assertThat(ccae, hasDefinedStructure(filename, FortiosStructureType.INTERFACE, "port1"));
    assertThat(ccae, hasDefinedStructure(filename, FortiosStructureType.INTERFACE, "port2"));
    assertThat(ccae, hasDefinedStructure(filename, FortiosStructureType.INTERFACE, "port3"));
    assertThat(
        ccae, hasDefinedStructure(filename, FortiosStructureType.SERVICE_CUSTOM, "service1"));
    assertThat(
        ccae, hasDefinedStructure(filename, FortiosStructureType.SERVICE_CUSTOM, "service2"));
    assertThat(
        ccae, hasDefinedStructure(filename, FortiosStructureType.SERVICE_CUSTOM, "service3"));
    assertThat(ccae, hasDefinedStructure(filename, FortiosStructureType.ADDRESS, "addr1"));
    assertThat(ccae, hasDefinedStructure(filename, FortiosStructureType.ADDRESS, "addr2"));
    assertThat(ccae, hasDefinedStructure(filename, FortiosStructureType.ADDRESS, "addr3"));
    assertThat(ccae, hasDefinedStructure(filename, FortiosStructureType.POLICY, "1"));

    // Confirm reference count is correct for used structure
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.ADDRESS, "addr1", 2));
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.ADDRESS, "addr2", 2));
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.ADDRESS, "addr3", 1));
    // Interface refs include self-refs
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.INTERFACE, "port1", 3));
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.INTERFACE, "port2", 3));
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.INTERFACE, "port3", 2));
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.SERVICE_CUSTOM, "service1", 1));
    assertThat(ccae, hasNumReferrers(filename, FortiosStructureType.SERVICE_CUSTOM, "service2", 1));

    // Confirm undefined references are detected
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            FortiosStructureType.INTERFACE_OR_ZONE,
            "UNDEFINED",
            FortiosStructureUsage.POLICY_DSTINTF));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            FortiosStructureType.INTERFACE_OR_ZONE,
            "UNDEFINED",
            FortiosStructureUsage.POLICY_SRCINTF));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            FortiosStructureType.ADDRESS_OR_ADDRGRP,
            "UNDEFINED",
            FortiosStructureUsage.POLICY_DSTADDR));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            FortiosStructureType.ADDRESS_OR_ADDRGRP,
            "UNDEFINED",
            FortiosStructureUsage.POLICY_SRCADDR));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            FortiosStructureType.SERVICE_CUSTOM_OR_SERVICE_GROUP,
            "UNDEFINED",
            FortiosStructureUsage.POLICY_SERVICE));
  }

  @Test
  public void testEditRecovery() throws IOException {
    String hostname = "edit_recovery";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    batfish.getSettings().setDisableUnrecognized(false);
    FortiosConfiguration vc =
        (FortiosConfiguration)
            batfish.loadVendorConfigurations(batfish.getSnapshot()).get(hostname);
    assertThat(vc.getInterfaces(), hasKeys("port1"));
    assertThat(vc.getAddresses(), hasKeys("addr1"));
    assertThat(vc.getServices(), hasKeys("service1"));
    assertThat(vc.getPolicies(), hasKeys("1"));

    // Make sure the lines were actually unrecognized
    Warnings warnings =
        getOnlyElement(
            batfish
                .loadParseVendorConfigurationAnswerElement(batfish.getSnapshot())
                .getWarnings()
                .values());
    assertThat(
        warnings,
        hasParseWarnings(
            containsInAnyOrder(
                allOf(
                    hasComment("This syntax is unrecognized"),
                    hasText("set UNDEFINED_ADDR_PROP to a bunch of garbage")),
                allOf(
                    hasComment("This syntax is unrecognized"),
                    hasText("set UNDEFINED_SERVICE_PROP to a bunch of garbage")),
                allOf(
                    hasComment("This syntax is unrecognized"),
                    hasText("set UNDEFINED_IFACE_PROP to a bunch of garbage")),
                allOf(
                    hasComment("This syntax is unrecognized"),
                    hasText("set UNDEFINED_POLICY_PROP to a bunch of garbage")))));

    // Make sure other props were still set, after the unrecognized lines
    assertThat(vc.getInterfaces().get("port1").getVdom(), equalTo("root"));
    assertThat(vc.getAddresses().get("addr1").getComment(), equalTo("addr comment"));
    assertThat(vc.getServices().get("service1").getComment(), equalTo("service comment"));
    assertThat(vc.getPolicies().get("1").getComments(), equalTo("policy comments, plural"));
  }

  private Flow createFlow(String ingressIface, Ip src, Ip dst, int port) {
    return Flow.builder()
        .setIngressNode("node")
        .setIngressInterface(ingressIface)
        .setIpProtocol(IpProtocol.TCP)
        .setSrcIp(src)
        .setDstIp(dst)
        .setSrcPort(9999) // Arbitrary src port
        .setDstPort(port)
        .build();
  }

  /**
   * Test that policies are converted correctly for use as interface outgoing filters. This test is
   * currently ignored because of a bug in how we convert policies - deny rules that don't match
   * still end up denying traffic.
   */
  @Test
  public void testInterfaceOutgoingFilterPolicyConversion() throws IOException {
    String hostname = "policy";
    Configuration c = parseConfig(hostname);

    int dstPortAllowed = 2345;
    int dstPortDenied = 1234;

    String port1 = "port1";
    Ip port1Addr = Ip.parse("10.0.1.2");
    String port2 = "port2";
    Ip port2Addr = Ip.parse("10.0.2.2");
    String port3 = "port3";
    Ip port3Addr = Ip.parse("10.0.3.2");
    String port4 = "port4";
    Ip port4Addr = Ip.parse("10.0.4.2");

    // Explicitly permitted
    Flow p1ToP3 = createFlow(port1, port1Addr, port3Addr, dstPortAllowed);
    Flow p1ToP4 = createFlow(port1, port1Addr, port3Addr, dstPortAllowed);
    Flow p2ToP3 = createFlow(port2, port2Addr, port4Addr, dstPortAllowed);
    Flow p2ToP4 = createFlow(port2, port2Addr, port4Addr, dstPortAllowed);

    // Explicitly denied
    Flow p1ToP3Denied = createFlow(port1, port1Addr, port3Addr, dstPortDenied);
    Flow p2ToP3Denied = createFlow(port2, port2Addr, port4Addr, dstPortDenied);

    // No-match, denied
    Flow p3ToP1 = createFlow(port3, port3Addr, port1Addr, dstPortAllowed);

    // Explicitly permitted
    assertThat(c, hasInterface(port3, hasOutgoingFilter(accepts(p1ToP3, port1, c))));
    assertThat(c, hasInterface(port4, hasOutgoingFilter(accepts(p1ToP4, port1, c))));
    assertThat(c, hasInterface(port3, hasOutgoingFilter(accepts(p2ToP3, port2, c))));
    assertThat(c, hasInterface(port4, hasOutgoingFilter(accepts(p2ToP4, port2, c))));

    // No-match, denied
    assertThat(c, hasInterface(port1, hasOutgoingFilter(rejects(p3ToP1, port3, c))));

    // Explicitly denied
    assertThat(c, hasInterface(port3, hasOutgoingFilter(rejects(p1ToP3Denied, port1, c))));
    assertThat(c, hasInterface(port3, hasOutgoingFilter(rejects(p2ToP3Denied, port2, c))));
  }

  ////////////////////////
  // Setup / test infra //
  ////////////////////////

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/fortios/testconfigs/";

  private final BddTestbed _bddTestbed = new BddTestbed(ImmutableMap.of(), ImmutableMap.of());
  private final BDDPacket _pkt = _bddTestbed.getPkt();
  private final BDD _zero = _bddTestbed.getPkt().getFactory().zero();
  private final BDD _one = _bddTestbed.getPkt().getFactory().one();
  private final IpSpaceToBDD _dstIpBdd = _bddTestbed.getDstIpBdd();
  private final IpSpaceToBDD _srcIpBdd = _bddTestbed.getSrcIpBdd();

  private @Nonnull Batfish getBatfishForConfigurationNames(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    Batfish batfish = BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
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
    IBatfish iBatfish = getBatfishForConfigurationNames(configurationNames);
    return iBatfish.loadConfigurations(iBatfish.getSnapshot());
  }

  private @Nonnull FortiosConfiguration parseVendorConfig(String hostname) {
    String src = readResource(TESTCONFIGS_PREFIX + hostname, UTF_8);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    FortiosCombinedParser parser = new FortiosCombinedParser(src, settings);
    FortiosControlPlaneExtractor extractor =
        new FortiosControlPlaneExtractor(src, parser, new Warnings());
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(TEST_SNAPSHOT, tree);
    FortiosConfiguration vendorConfiguration =
        (FortiosConfiguration) extractor.getVendorConfiguration();
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + hostname);
    // crash if not serializable
    return SerializationUtils.clone(vendorConfiguration);
  }
}

package org.batfish.grammar.palo_alto;

import static org.batfish.datamodel.ConfigurationFormat.PALO_ALTO_NESTED;
import static org.batfish.datamodel.Interface.DependencyType.BIND;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasAdministrativeCost;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasMetric;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopInterface;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasNextHopIp;
import static org.batfish.datamodel.matchers.AbstractRouteDecoratorMatchers.hasPrefix;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasRouterId;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpAccessList;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasIpSpace;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrf;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructure;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasDefinedStructureWithDefinitionLines;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasMemberInterfaces;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasNumReferrers;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasOutgoingFilter;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasUndefinedReference;
import static org.batfish.datamodel.matchers.DataModelMatchers.hasZone;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAddress;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDependencies;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasDescription;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasEncapsulationVlan;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasInterfaceType;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasMtu;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasZoneName;
import static org.batfish.datamodel.matchers.InterfaceMatchers.isActive;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.accepts;
import static org.batfish.datamodel.matchers.IpAccessListMatchers.rejects;
import static org.batfish.datamodel.matchers.NssaSettingsMatchers.hasDefaultOriginateType;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasNssa;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasStub;
import static org.batfish.datamodel.matchers.OspfAreaMatchers.hasStubType;
import static org.batfish.datamodel.matchers.OspfProcessMatchers.hasArea;
import static org.batfish.datamodel.matchers.StaticRouteMatchers.hasNextVrf;
import static org.batfish.datamodel.matchers.StubSettingsMatchers.hasSuppressType3;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasInterfaces;
import static org.batfish.datamodel.matchers.VrfMatchers.hasName;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.batfish.grammar.VendorConfigurationFormatDetector.BATFISH_FLATTENED_PALO_ALTO_HEADER;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.DEFAULT_VSYS_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.NULL_VRF_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.SHARED_VSYS_NAME;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.computeObjectName;
import static org.batfish.representation.palo_alto.PaloAltoConfiguration.computeServiceGroupMemberAclName;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.APPLICATION_GROUP_OR_APPLICATION;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SERVICE;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SERVICE_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.SHARED_GATEWAY;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ZONE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.IMPORT_INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.RULE_APPLICATION;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.STATIC_ROUTE_INTERFACE;
import static org.batfish.representation.palo_alto.PaloAltoStructureUsage.VIRTUAL_ROUTER_INTERFACE;
import static org.batfish.representation.palo_alto.Zone.Type.EXTERNAL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishException;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.util.CommonUtil;
import org.batfish.config.Settings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DiffieHellmanGroup;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.EncryptionAlgorithm;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IkeHashingAlgorithm;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpRange;
import org.batfish.datamodel.IpsecAuthenticationAlgorithm;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.ConvertConfigurationAnswerElement;
import org.batfish.datamodel.matchers.InterfaceMatchers;
import org.batfish.datamodel.matchers.NssaSettingsMatchers;
import org.batfish.datamodel.matchers.OspfAreaMatchers;
import org.batfish.datamodel.matchers.OspfProcessMatchers;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfNetworkType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.StubType;
import org.batfish.grammar.flattener.Flattener;
import org.batfish.grammar.flattener.FlattenerLineMap;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.representation.palo_alto.AddressGroup;
import org.batfish.representation.palo_alto.AddressObject;
import org.batfish.representation.palo_alto.AdminDistances;
import org.batfish.representation.palo_alto.Application;
import org.batfish.representation.palo_alto.BgpConnectionOptions;
import org.batfish.representation.palo_alto.BgpPeer;
import org.batfish.representation.palo_alto.BgpPeer.ReflectorClient;
import org.batfish.representation.palo_alto.BgpPeerGroup;
import org.batfish.representation.palo_alto.BgpVr;
import org.batfish.representation.palo_alto.BgpVrRoutingOptions.AsFormat;
import org.batfish.representation.palo_alto.CryptoProfile;
import org.batfish.representation.palo_alto.CryptoProfile.Type;
import org.batfish.representation.palo_alto.IbgpPeerGroupType;
import org.batfish.representation.palo_alto.Interface;
import org.batfish.representation.palo_alto.OspfArea;
import org.batfish.representation.palo_alto.OspfAreaNormal;
import org.batfish.representation.palo_alto.OspfAreaNssa;
import org.batfish.representation.palo_alto.OspfAreaNssa.DefaultRouteType;
import org.batfish.representation.palo_alto.OspfAreaStub;
import org.batfish.representation.palo_alto.OspfInterface;
import org.batfish.representation.palo_alto.OspfInterface.LinkType;
import org.batfish.representation.palo_alto.OspfVr;
import org.batfish.representation.palo_alto.PaloAltoConfiguration;
import org.batfish.representation.palo_alto.PaloAltoStructureType;
import org.batfish.representation.palo_alto.PaloAltoStructureUsage;
import org.batfish.representation.palo_alto.ServiceBuiltIn;
import org.batfish.representation.palo_alto.StaticRoute;
import org.batfish.representation.palo_alto.Tag;
import org.batfish.representation.palo_alto.VirtualRouter;
import org.batfish.representation.palo_alto.Vsys;
import org.batfish.representation.palo_alto.Zone;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public final class PaloAltoGrammarTest {
  private static final String TESTCONFIGS_PREFIX = "org/batfish/grammar/palo_alto/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  @Rule public ExpectedException _thrown = ExpectedException.none();

  private Batfish getBatfishForConfigurationNames(String... configurationNames) throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    return BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
  }

  private @Nonnull Configuration parseConfig(String hostname) {
    try {
      Map<String, Configuration> configs = parseTextConfigs(hostname);
      assertThat(configs, hasKey(hostname));
      return configs.get(hostname);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private PaloAltoConfiguration parsePaloAltoConfig(String hostname) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    PaloAltoCombinedParser parser = new PaloAltoCombinedParser(src, settings, null);
    ParserRuleContext tree =
        Batfish.parse(parser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    PaloAltoControlPlaneExtractor extractor =
        new PaloAltoControlPlaneExtractor(src, parser, new Warnings());
    extractor.processParseTree(tree);
    PaloAltoConfiguration pac = (PaloAltoConfiguration) extractor.getVendorConfiguration();
    pac.setVendor(ConfigurationFormat.PALO_ALTO);
    ConvertConfigurationAnswerElement answerElement = new ConvertConfigurationAnswerElement();
    pac.setFilename(TESTCONFIGS_PREFIX + hostname);
    // crash if not serializable
    pac = SerializationUtils.clone(pac);
    pac.setAnswerElement(answerElement);
    return pac;
  }

  private @Nonnull PaloAltoConfiguration parseNestedConfig(String hostname) {
    String src = CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    Warnings w = new Warnings();
    BatfishLogger logger = new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false);
    Flattener flattener =
        Batfish.flatten(
            src, logger, settings, w, PALO_ALTO_NESTED, BATFISH_FLATTENED_PALO_ALTO_HEADER);
    String fileText = flattener.getFlattenedConfigurationText();
    FlattenerLineMap lineMap = flattener.getOriginalLineMap();
    PaloAltoCombinedParser paParser = new PaloAltoCombinedParser(fileText, settings, lineMap);
    PaloAltoControlPlaneExtractor extractor =
        new PaloAltoControlPlaneExtractor(fileText, paParser, w);
    ParserRuleContext tree = Batfish.parse(paParser, logger, settings);
    extractor.processParseTree(tree);
    PaloAltoConfiguration pac = (PaloAltoConfiguration) extractor.getVendorConfiguration();
    pac.setVendor(ConfigurationFormat.PALO_ALTO);
    ConvertConfigurationAnswerElement answerElement = new ConvertConfigurationAnswerElement();
    pac.setFilename(TESTCONFIGS_PREFIX + hostname);
    pac.setAnswerElement(answerElement);
    return pac;
  }

  private Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    return getBatfishForConfigurationNames(configurationNames).loadConfigurations();
  }

  private static Flow createFlow(IpProtocol protocol, int sourcePort, int destinationPort) {
    Flow.Builder fb = Flow.builder();
    fb.setIngressNode("node");
    fb.setIpProtocol(protocol);
    fb.setDstPort(destinationPort);
    fb.setSrcPort(sourcePort);
    fb.setTag("test");
    return fb.build();
  }

  private static Flow createFlow(String sourceAddress, String destinationAddress) {
    return createFlow(sourceAddress, destinationAddress, IpProtocol.TCP, 1, 1);
  }

  private static Flow createFlow(
      String sourceAddress,
      String destinationAddress,
      IpProtocol protocol,
      int sourcePort,
      int destinationPort) {
    Flow.Builder fb = Flow.builder();
    fb.setIngressNode("node");
    fb.setSrcIp(Ip.parse(sourceAddress));
    fb.setDstIp(Ip.parse(destinationAddress));
    fb.setIpProtocol(protocol);
    fb.setDstPort(destinationPort);
    fb.setSrcPort(sourcePort);
    fb.setTag("test");
    return fb.build();
  }

  @Test
  public void testTags() {
    PaloAltoConfiguration c = parsePaloAltoConfig("tags");

    String tag1 = "vsys_tag";
    String tag2 = "vsys_tag with spaces";
    String sharedTag = "shared_tag";

    Vsys vsys = c.getVirtualSystems().get(DEFAULT_VSYS_NAME);
    Map<String, Tag> tags = vsys.getTags();

    Vsys sharedVsys = c.getShared();
    Map<String, Tag> sharedTags = sharedVsys.getTags();

    // Confirm vsys-specific tags are extracted correctly
    assertThat(tags.keySet(), contains(tag1, tag2));
    assertThat(tags.get(tag1).getComments(), equalTo("something"));
    assertThat(tags.get(tag2).getComments(), equalTo("comments with spaces"));

    // Confirm shared tags are extracted correctly
    assertThat(sharedTags.keySet(), contains(sharedTag));
    assertThat(sharedTags.get(sharedTag).getComments(), nullValue());
  }

  @Test
  public void testAddressGroups() {
    PaloAltoConfiguration c = parsePaloAltoConfig("address-groups");

    Vsys vsys = c.getVirtualSystems().get(DEFAULT_VSYS_NAME);
    Map<String, AddressGroup> addressGroups = vsys.getAddressGroups();
    Map<String, AddressObject> addressObjects = vsys.getAddressObjects();

    // there are three address groups defined in the file, including the empty one
    assertThat(addressGroups.keySet(), equalTo(ImmutableSet.of("group0", "group1", "group2")));

    // the ip space of the empty group is empty
    assertThat(addressGroups.get("group0").getMembers(), equalTo(ImmutableSet.of()));
    assertThat(
        addressGroups.get("group0").getIpSpace(addressObjects, addressGroups),
        equalTo(EmptyIpSpace.INSTANCE));

    // we parsed the description and tag, addr3 is undefined but should not have been discarded
    assertThat(addressGroups.get("group1").getDescription(), equalTo("group1-desc"));
    assertThat(addressGroups.get("group1").getTags(), contains("tag1"));
    assertThat(
        addressGroups.get("group1").getMembers(),
        equalTo(ImmutableSet.of("addr1", "addr2", "addr3")));
    assertThat(
        addressGroups.get("group1").getIpSpace(addressObjects, addressGroups),
        equalTo(
            AclIpSpace.union(
                addressObjects.get("addr2").getIpSpace(),
                addressObjects.get("addr1").getIpSpace())));

    // check that we parse multiple address objects and tags on the same line correctly
    assertThat(
        addressGroups.get("group2").getMembers(), equalTo(ImmutableSet.of("addr1", "addr2")));
    assertThat(
        addressGroups.get("group2").getTags(), containsInAnyOrder("tag2", "tag with spaces"));

    // check that ip spaces were inserted properly
    Configuration viConfig = c.toVendorIndependentConfigurations().get(0);
    assertThat(
        viConfig,
        hasIpSpace(
            "group0",
            equalTo(addressGroups.get("group0").getIpSpace(addressObjects, addressGroups))));
    assertThat(
        viConfig,
        hasIpSpace(
            "group1",
            equalTo(addressGroups.get("group1").getIpSpace(addressObjects, addressGroups))));
  }

  // TODO: https://github.com/batfish/batfish/issues/4921
  @Ignore
  @Test
  public void testAddressGroupHybrid() {
    PaloAltoConfiguration c = parsePaloAltoConfig("address-group-hybrid");

    Vsys vsys = c.getVirtualSystems().get(DEFAULT_VSYS_NAME);
    Map<String, AddressGroup> addressGroups = vsys.getAddressGroups();
    Map<String, AddressObject> addressObjects = vsys.getAddressObjects();

    Vsys sharedVsys = c.getShared();
    Map<String, AddressObject> sharedAddressObjects = sharedVsys.getAddressObjects();

    assertThat(addressGroups.keySet(), equalTo(ImmutableSet.of("group")));

    // Confirm hybrid address-group contains both shared and vsys-specific addresses
    assertThat(
        addressGroups.get("group").getIpSpace(addressObjects, addressGroups),
        equalTo(
            AclIpSpace.union(
                addressObjects.get("addr").getIpSpace(),
                sharedAddressObjects.get("shared_addr").getIpSpace())));
  }

  @Test
  public void testAddressGroupDynamic() {
    PaloAltoConfiguration c = parsePaloAltoConfig("address-group-dynamic");

    Vsys vsys = c.getVirtualSystems().get(DEFAULT_VSYS_NAME);
    Map<String, AddressObject> addressObjects = vsys.getAddressObjects();
    Map<String, AddressGroup> addressGroups = vsys.getAddressGroups();

    // Confirm filter was attached to the dynamic address-group
    assertThat(addressGroups.keySet(), contains("group"));
    AddressGroup ag = addressGroups.get("group");
    assertThat(ag.getType(), equalTo(AddressGroup.Type.DYNAMIC));
    assertThat(ag.getFilter(), equalTo("'tagA' and tag1"));

    // Confirm the filter resolves to the correct IpSpace, containing only the one matching address
    assertThat(
        ag.getIpSpace(addressObjects, addressGroups),
        equalTo(addressObjects.get("addr1").getIpSpace()));
  }

  @Test
  public void testAddressGroupsNested() {
    PaloAltoConfiguration c = parsePaloAltoConfig("address-groups-nested");

    Vsys vsys = c.getVirtualSystems().get(DEFAULT_VSYS_NAME);
    Map<String, AddressGroup> addressGroups = vsys.getAddressGroups();

    assertThat(addressGroups.get("group1").getMembers(), equalTo(ImmutableSet.of("group0")));
    assertThat(addressGroups.get("group0").getMembers(), equalTo(ImmutableSet.of("group2")));
    assertThat(addressGroups.get("group2").getMembers(), equalTo(ImmutableSet.of("group0")));
  }

  @Test
  public void testAddressGroupsReference() throws IOException {
    String hostname = "address-groups-nested";
    String filename = "configs/" + hostname;

    String group0 = computeObjectName(DEFAULT_VSYS_NAME, "group0");
    String group1 = computeObjectName(DEFAULT_VSYS_NAME, "group1");

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ccae, hasNumReferrers(filename, PaloAltoStructureType.ADDRESS_GROUP, group0, 2));
    assertThat(ccae, hasNumReferrers(filename, PaloAltoStructureType.ADDRESS_GROUP, group1, 0));
  }

  @Test
  public void testAddressObjectGroupInheritance() {
    String hostname = "address-object-group-inheritance";
    Configuration c = parseConfig(hostname);

    String if1name = "ethernet1/1";
    String if2name = "ethernet1/2";

    // rule1: literal 11.11.11.11 should not be allowed but 10.10.10.10 should be
    Flow rule1Permitted = createFlow("10.10.10.10", "33.33.33.33");
    Flow rule1Denied = createFlow("11.11.11.11", "33.33.33.33");

    assertThat(c, hasInterface(if1name, hasOutgoingFilter(accepts(rule1Permitted, if2name, c))));
    assertThat(c, hasInterface(if1name, hasOutgoingFilter(rejects(rule1Denied, if2name, c))));

    // rule2: literal 22.22.22.22 should not be allowed but 10.10.10.10 should be
    Flow rule2Permitted = createFlow("44.44.44.44", "10.10.10.10");
    Flow rule2Denied = createFlow("44.44.44.44", "22.22.22.22");

    assertThat(c, hasInterface(if1name, hasOutgoingFilter(accepts(rule2Permitted, if2name, c))));
    assertThat(c, hasInterface(if1name, hasOutgoingFilter(rejects(rule2Denied, if2name, c))));
  }

  @Test
  public void testAddressObjectGroupNameConflict() {
    PaloAltoConfiguration c = parsePaloAltoConfig("address-object-group-name-conflict");

    Vsys vsys = c.getVirtualSystems().get(DEFAULT_VSYS_NAME);

    // the address object definition should win for addr1
    assertThat(vsys.getAddressObjects().keySet(), equalTo(ImmutableSet.of("addr1")));

    // the address group definition should win for group1
    assertThat(vsys.getAddressGroups().keySet(), equalTo(ImmutableSet.of("group1")));
  }

  @Test
  public void testAddressObjectGroupReferenceInRule() throws IOException {
    String hostname = "address-object-group-inheritance";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String objectName = computeObjectName(DEFAULT_VSYS_NAME, "11.11.11.11");
    String groupName = computeObjectName(DEFAULT_VSYS_NAME, "22.22.22.22");

    assertThat(
        ccae, hasNumReferrers(filename, PaloAltoStructureType.ADDRESS_OBJECT, objectName, 2));
    assertThat(ccae, hasNumReferrers(filename, PaloAltoStructureType.ADDRESS_GROUP, groupName, 1));
  }

  @Test
  public void testAddressObjects() {
    PaloAltoConfiguration c = parsePaloAltoConfig("address-objects");

    Vsys vsys = c.getVirtualSystems().get(DEFAULT_VSYS_NAME);
    Map<String, AddressObject> addressObjects = vsys.getAddressObjects();

    // there are four address objects defined in the file, including the empty one
    assertThat(
        vsys.getAddressObjects().keySet(),
        equalTo(ImmutableSet.of("addr0", "addr1", "addr2", "addr3")));

    // check that we parse the name-only object right
    assertThat(addressObjects.get("addr0").getIpSpace(), equalTo(EmptyIpSpace.INSTANCE));

    // check that we parsed description, tag, and prefix right
    // note that PA allows non-canonical prefixes
    assertThat(
        addressObjects.get("addr1").getIpSpace(),
        equalTo(Prefix.strict("10.1.1.0/24").toIpSpace()));
    assertThat(addressObjects.get("addr1").getDescription(), equalTo("addr1-desc"));
    assertThat(addressObjects.get("addr1").getTags(), contains("tag1"));

    // check that we parse the IP address and tags right
    assertThat(addressObjects.get("addr2").getIpSpace(), equalTo(Ip.parse("10.1.1.2").toIpSpace()));
    assertThat(
        addressObjects.get("addr2").getTags(), containsInAnyOrder("tag2", "tag with spaces"));

    // check that we parse the IP range right
    assertThat(
        addressObjects.get("addr3").getIpSpace(),
        equalTo(IpRange.range(Ip.parse("1.1.1.1"), Ip.parse("1.1.1.2"))));

    // check that ip spaces were inserted properly
    Configuration viConfig = c.toVendorIndependentConfigurations().get(0);
    assertThat(viConfig, hasIpSpace("addr0", equalTo(addressObjects.get("addr0").getIpSpace())));
    assertThat(viConfig, hasIpSpace("addr1", equalTo(addressObjects.get("addr1").getIpSpace())));
    assertThat(viConfig, hasIpSpace("addr2", equalTo(addressObjects.get("addr2").getIpSpace())));
    assertThat(viConfig, hasIpSpace("addr3", equalTo(addressObjects.get("addr3").getIpSpace())));
  }

  @Test
  public void testAddressObjectReference() throws IOException {
    // addr1 is not referenced
    String hostname1 = "address-objects";
    String filename1 = "configs/" + hostname1;

    // addr1 is referenced by two groups
    String hostname2 = "address-groups";
    String filename2 = "configs/" + hostname2;

    String name = computeObjectName(DEFAULT_VSYS_NAME, "addr1");

    // Confirm reference count is correct for used structure
    Batfish batfish1 = getBatfishForConfigurationNames(hostname1);
    ConvertConfigurationAnswerElement ccae1 =
        batfish1.loadConvertConfigurationAnswerElementOrReparse();

    assertThat(ccae1, hasNumReferrers(filename1, PaloAltoStructureType.ADDRESS_OBJECT, name, 0));

    Batfish batfish2 = getBatfishForConfigurationNames(hostname2);
    ConvertConfigurationAnswerElement ccae2 =
        batfish2.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm reference count is correct for used structure
    assertThat(ccae2, hasNumReferrers(filename2, PaloAltoStructureType.ADDRESS_OBJECT, name, 2));

    // Confirm undefined reference is detected
    assertThat(
        ccae2,
        hasUndefinedReference(
            filename2, PaloAltoStructureType.ADDRESS_GROUP_OR_ADDRESS_OBJECT, "addr3"));
  }

  @Test
  public void testApplications() {
    PaloAltoConfiguration c = parsePaloAltoConfig("applications");

    Vsys vsys = c.getVirtualSystems().get(DEFAULT_VSYS_NAME);
    Map<String, Application> applications = vsys.getApplications();

    // Should have two applications, including an empty one
    assertThat(applications.keySet(), equalTo(ImmutableSet.of("app1", "app2")));

    // Check that descriptions are extracted
    assertThat(applications.get("app1").getDescription(), nullValue());
    assertThat(applications.get("app2").getDescription(), equalTo("this is a description"));
  }

  @Test
  public void testApplicationsReference() throws IOException {
    String hostname = "applications";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String app1Name = computeObjectName(DEFAULT_VSYS_NAME, "app1");
    String app2Name = computeObjectName(DEFAULT_VSYS_NAME, "app2");

    // Confirm reference count is correct for defined structures
    assertThat(ccae, hasNumReferrers(filename, PaloAltoStructureType.APPLICATION, app1Name, 0));
    assertThat(ccae, hasNumReferrers(filename, PaloAltoStructureType.APPLICATION, app2Name, 1));

    // Confirm undefined application is detected
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, APPLICATION_GROUP_OR_APPLICATION, "undefined", RULE_APPLICATION));
  }

  @Test
  public void testBgpExtraction() {
    PaloAltoConfiguration c = parseNestedConfig("bgp");
    VirtualRouter vr = c.getVirtualRouters().get("BGP");
    assertThat(vr, notNullValue());
    BgpVr bgp = vr.getBgp();
    assertThat(bgp, notNullValue());
    assertThat(bgp.getEnable(), equalTo(Boolean.TRUE));
    assertThat(bgp.getInstallRoute(), equalTo(Boolean.TRUE));
    assertThat(bgp.getLocalAs(), equalTo(65001L));
    assertThat(bgp.getRejectDefaultRoute(), equalTo(Boolean.FALSE));
    assertThat(bgp.getRouterId(), equalTo(Ip.parse("1.2.3.4")));
    assertThat(bgp.getRoutingOptions().getAggregateMed(), equalTo(Boolean.FALSE));
    assertThat(bgp.getRoutingOptions().getAlwaysCompareMed(), equalTo(Boolean.TRUE));
    assertThat(bgp.getRoutingOptions().getAsFormat(), equalTo(AsFormat.FOUR_BYTE_AS));
    assertThat(bgp.getRoutingOptions().getDefaultLocalPreference(), equalTo(103L));
    assertThat(bgp.getRoutingOptions().getDeterministicMedComparison(), equalTo(Boolean.FALSE));
    assertThat(bgp.getRoutingOptions().getGracefulRestartEnable(), equalTo(Boolean.FALSE));
    assertThat(bgp.getRoutingOptions().getReflectorClusterId(), equalTo(Ip.parse("1.2.3.5")));

    assertThat(bgp.getPeerGroups().keySet(), contains("PG"));
    BgpPeerGroup pg = bgp.getPeerGroups().get("PG");
    assertThat(pg.getEnable(), equalTo(true));
    assertThat(pg.getTypeAndOptions(), instanceOf(IbgpPeerGroupType.class));

    assertThat(pg.getPeers().keySet(), contains("PEER"));
    BgpPeer peer = pg.getOrCreatePeerGroup("PEER");
    assertThat(peer.getEnable(), equalTo(true));
    assertThat(peer.getLocalInterface(), equalTo("ethernet1/1"));
    assertThat(peer.getLocalAddress(), equalTo(Ip.parse("1.2.3.6")));
    assertThat(peer.getPeerAddress(), equalTo(Ip.parse("5.4.3.2")));
    assertThat(peer.getPeerAs(), equalTo(54321L));
    assertThat(peer.getReflectorClient(), equalTo(ReflectorClient.NON_CLIENT));

    BgpConnectionOptions co = peer.getConnectionOptions();
    assertTrue(co.getIncomingAllow());
    assertThat(co.getRemotePort(), equalTo(0));
    assertFalse(co.getOutgoingAllow());
    assertThat(co.getLocalPort(), equalTo(4321));
  }

  @Test
  public void testBgpConversion() {
    Configuration c = parseConfig("bgp");
    assertThat(c, hasVrf("BGP", hasBgpProcess(any(BgpProcess.class))));
    BgpProcess proc = c.getVrfs().get("BGP").getBgpProcess();
    assertThat(proc, hasRouterId(Ip.parse("1.2.3.4")));
    assertThat(proc.getActiveNeighbors().keySet(), contains(Prefix.parse("5.4.3.2/32")));
    BgpActivePeerConfig peer = proc.getActiveNeighbors().get(Prefix.parse("5.4.3.2/32"));
    assertThat(peer.getDescription(), equalTo("PEER"));
    assertThat(peer.getGroup(), equalTo("PG"));
    assertThat(peer.getLocalIp(), equalTo(Ip.parse("1.2.3.6")));
    assertThat(peer.getLocalAs(), equalTo(65001L));
    assertThat(peer.getRemoteAsns(), equalTo(LongSpace.of(65001)));
    // BgpRoutingProcess requires an export policy be present
    String exportPolicyName = peer.getIpv4UnicastAddressFamily().getExportPolicy();
    assertThat(exportPolicyName, not(nullValue()));
    assertThat(c.getRoutingPolicies().get(exportPolicyName), not(nullValue()));
  }

  @Test
  public void testOspfExtraction() {
    PaloAltoConfiguration c = parsePaloAltoConfig("ospf");
    VirtualRouter vr = c.getVirtualRouters().get("vr1");
    assertThat(vr, notNullValue());
    OspfVr ospf = vr.getOspf();

    assertThat(ospf, notNullValue());
    assertThat(ospf.getRouterId(), equalTo(Ip.parse("0.0.0.0")));
    assertTrue(ospf.isEnable());
    assertFalse(ospf.isRejectDefaultRoute());

    Ip areaId = Ip.parse("0.0.0.1");
    assertThat(ospf.getAreas(), hasKey(areaId));
    OspfArea ospfArea = ospf.getAreas().get(areaId);
    assertThat(ospfArea.getAreaId(), equalTo(areaId));
    assertThat(ospfArea.getTypeSettings(), not(nullValue()));
    assertThat(ospfArea.getTypeSettings(), instanceOf(OspfAreaStub.class));
    OspfAreaStub stubArea = (OspfAreaStub) ospfArea.getTypeSettings();
    assertThat(stubArea.getDefaultRouteMetric(), equalTo(12));
    assertThat(stubArea.getAcceptSummary(), equalTo(Boolean.TRUE));
    assertTrue(stubArea.isDefaultRouteDisable());

    areaId = Ip.parse("0.0.0.2");
    assertThat(ospf.getAreas(), hasKey(areaId));
    ospfArea = ospf.getAreas().get(areaId);
    assertThat(ospfArea.getAreaId(), equalTo(areaId));
    assertThat(ospfArea.getTypeSettings(), not(nullValue()));
    assertThat(ospfArea.getTypeSettings(), instanceOf(OspfAreaNormal.class));

    areaId = Ip.parse("0.0.0.3");
    assertThat(ospf.getAreas(), hasKey(areaId));
    ospfArea = ospf.getAreas().get(areaId);
    assertThat(ospfArea.getAreaId(), equalTo(areaId));
    assertThat(ospfArea.getTypeSettings(), not(nullValue()));
    assertThat(ospfArea.getTypeSettings(), instanceOf(OspfAreaNssa.class));
    OspfAreaNssa nssaArea = (OspfAreaNssa) ospfArea.getTypeSettings();
    assertThat(nssaArea.getDefaultRouteMetric(), equalTo(13));
    assertThat(nssaArea.getAcceptSummary(), equalTo(Boolean.FALSE));
    assertThat(nssaArea.getDefaultRouteType(), equalTo(DefaultRouteType.EXT_2));
    assertFalse(nssaArea.isDefaultRouteDisable());

    String ifaceName = "ethernet1/3.5";
    assertThat(ospfArea.getInterfaces(), hasKey(ifaceName));
    OspfInterface ospfIface = ospfArea.getInterfaces().get(ifaceName);
    assertThat(ospfIface.getEnable(), equalTo(Boolean.FALSE));
    assertThat(ospfIface.getPassive(), equalTo(Boolean.FALSE));
    assertThat(ospfIface.getMetric(), equalTo(10));
    assertThat(ospfIface.getPriority(), equalTo(1));
    assertThat(ospfIface.getHelloInterval(), equalTo(10));
    assertThat(ospfIface.getDeadCounts(), equalTo(4));
    assertThat(ospfIface.getRetransmitInterval(), equalTo(5));
    assertThat(ospfIface.getTransitDelay(), equalTo(1));
    assertThat(ospfIface.getLinkType(), equalTo(LinkType.BROADCAST));
  }

  @Test
  public void testOspfConversion() {
    String processId = "~OSPF_PROCESS_0.0.0.0";
    String ifaceName34 = "ethernet1/3.4";
    String ifaceName35 = "ethernet1/3.5";

    Configuration c = parseConfig("ospf");

    // process
    assertThat(c.getVrfs(), hasKey("vr1"));
    assertThat(c.getVrfs().get("vr1").getOspfProcesses(), hasKey(processId));

    OspfProcess ospfProcess = c.getVrfs().get("vr1").getOspfProcesses().get(processId);

    assertThat(ospfProcess, OspfProcessMatchers.hasRouterId(equalTo(Ip.parse("0.0.0.0."))));

    // areas
    assertThat(
        ospfProcess,
        hasArea(
            1L,
            allOf(
                hasStub(hasSuppressType3(false)),
                OspfAreaMatchers.hasInterfaces(equalTo(ImmutableSortedSet.of(ifaceName34))))));
    assertThat(ospfProcess, hasArea(2L, allOf(hasStubType(equalTo(StubType.NONE)))));
    assertThat(
        ospfProcess,
        hasArea(
            3L,
            allOf(
                hasNssa(
                    allOf(
                        hasDefaultOriginateType(OspfDefaultOriginateType.EXTERNAL_TYPE2),
                        NssaSettingsMatchers.hasSuppressType3(true))),
                OspfAreaMatchers.hasInterfaces(equalTo(ImmutableSortedSet.of(ifaceName35))))));

    // interface
    assertThat(
        c.getAllInterfaces().get(ifaceName34).getOspfSettings(),
        equalTo(
            OspfInterfaceSettings.builder()
                .setAreaName(1L)
                .setCost(14)
                .setDeadInterval(8 * 15)
                .setEnabled(true)
                .setHelloInterval(15)
                .setNetworkType(OspfNetworkType.POINT_TO_POINT)
                .setPassive(true)
                .setProcess(processId)
                .build()));
    assertThat(
        c.getAllInterfaces().get(ifaceName35).getOspfSettings(),
        equalTo(
            OspfInterfaceSettings.builder()
                .setAreaName(3L)
                .setCost(10)
                .setDeadInterval(4 * 10)
                .setEnabled(false)
                .setHelloInterval(10)
                .setNetworkType(OspfNetworkType.BROADCAST)
                .setPassive(false)
                .setProcess(processId)
                .build()));
  }

  @Test
  public void testApplicationsBuiltinReference() throws IOException {
    String hostname = "applications-builtin";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String app1Name = computeObjectName(DEFAULT_VSYS_NAME, "ssh");

    // Confirm reference count is correct for defined structure that overwrites builtin
    assertThat(ccae, hasNumReferrers(filename, PaloAltoStructureType.APPLICATION, app1Name, 1));

    // Confirm using builtin does not produce undefined reference
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                filename, APPLICATION_GROUP_OR_APPLICATION, "ssl", RULE_APPLICATION)));
  }

  @Test
  public void testDnsServerInvalid() {
    _thrown.expect(BatfishException.class);
    // This should throw a BatfishException due to a malformed IP address
    parseConfig("dns-server-invalid");
  }

  @Test
  public void testDnsServers() {
    Configuration c = parseConfig("dns-server");

    // Confirm both dns servers show up
    assertThat(c.getDnsServers(), containsInAnyOrder("1.9.10.99", "100.199.200.255"));
  }

  @Test
  public void testFilesystemConfigFormat() {
    String hostname = "config-filesystem-format";
    Configuration c = parseConfig(hostname);

    // Confirm alternate config format is parsed and extracted properly
    // Confirm config devices set-line extraction works
    assertThat(c, hasHostname(equalTo(hostname)));
    // Confirm general config set-line extraction works
    assertThat(c.getLoggingServers(), contains("2.2.2.2"));
  }

  @Test
  public void testGlobalProtectAppCryptoProfiles() {
    PaloAltoConfiguration c = parsePaloAltoConfig("global-protect-app-crypto-profiles");

    assertThat(
        c.getCryptoProfiles(),
        containsInAnyOrder(
            new CryptoProfile(
                "default",
                Type.GLOBAL_PROTECT_APP,
                IpsecAuthenticationAlgorithm.HMAC_SHA1_96,
                null,
                ImmutableList.of(EncryptionAlgorithm.AES_128_CBC),
                null,
                null)));
  }

  @Test
  public void testHostname() throws IOException {
    String filename = "basic-parsing";
    String hostname = "my-hostname";

    // Confirm hostname extraction works
    assertThat(parseTextConfigs(filename).keySet(), contains(hostname));
  }

  @Test
  public void testIgnoredLines() {
    // will fail if some lines don't parse
    parseConfig("ignored-lines");
  }

  @Test
  public void testIkeCryptoProfiles() {
    PaloAltoConfiguration c = parsePaloAltoConfig("ike-crypto-profiles");

    assertThat(
        c.getCryptoProfiles(),
        containsInAnyOrder(
            new CryptoProfile(
                "default",
                Type.IKE,
                null,
                DiffieHellmanGroup.GROUP2,
                ImmutableList.of(EncryptionAlgorithm.AES_128_CBC, EncryptionAlgorithm.THREEDES_CBC),
                IkeHashingAlgorithm.SHA1,
                123 * 3600),
            new CryptoProfile(
                "profile1",
                Type.IKE,
                null,
                DiffieHellmanGroup.GROUP19,
                ImmutableList.of(EncryptionAlgorithm.AES_256_CBC),
                IkeHashingAlgorithm.SHA_384,
                8)));
  }

  @Test
  public void testIpsecCryptoProfiles() {
    PaloAltoConfiguration c = parsePaloAltoConfig("ipsec-crypto-profiles");

    assertThat(
        c.getCryptoProfiles(),
        containsInAnyOrder(
            new CryptoProfile(
                "default",
                Type.IPSEC,
                IpsecAuthenticationAlgorithm.HMAC_SHA1_96,
                DiffieHellmanGroup.GROUP2,
                ImmutableList.of(EncryptionAlgorithm.AES_128_CBC, EncryptionAlgorithm.DES_CBC),
                null,
                60),
            new CryptoProfile(
                "profile1",
                Type.IPSEC,
                null,
                DiffieHellmanGroup.GROUP14,
                ImmutableList.of(EncryptionAlgorithm.AES_128_GCM),
                null,
                24 * 3600)));
  }

  @Test
  public void testInterface() {
    String hostname = "interface";
    String interfaceName1 = "ethernet1/1";
    String interfaceName2 = "ethernet1/2";
    String interfaceName3 = "ethernet1/3";
    String interfaceName311 = "ethernet1/3.11";
    String loopback = "loopback";
    Configuration c = parseConfig(hostname);

    // Confirm interface MTU is extracted
    assertThat(c, hasInterface(interfaceName1, hasMtu(9001)));

    // Confirm address is extracted
    assertThat(
        c,
        hasInterface(
            interfaceName1,
            hasAllAddresses(contains(ConcreteInterfaceAddress.parse("1.1.1.1/24")))));
    assertThat(
        c,
        hasInterface(
            loopback,
            allOf(
                hasAddress("7.7.7.7/32"),
                hasAllAddresses(
                    contains(
                        ConcreteInterfaceAddress.parse("7.7.7.7/32"),
                        ConcreteInterfaceAddress.parse("7.7.7.8/32"))))));

    // Confirm comments are extracted
    assertThat(c, hasInterface(interfaceName1, hasDescription("description")));
    assertThat(c, hasInterface(interfaceName2, hasDescription("interface's long description")));
    assertThat(c, hasInterface(interfaceName3, hasDescription("single quoted description")));
    assertThat(c, hasInterface(interfaceName311, hasDescription("unit description")));

    // Confirm link status is extracted
    assertThat(c, hasInterface(interfaceName1, isActive()));
    assertThat(c, hasInterface(interfaceName2, not(isActive())));
    assertThat(c, hasInterface(interfaceName3, isActive()));
    assertThat(c, hasInterface(interfaceName311, not(isActive())));

    // Confirm tag extraction for units
    assertThat(c, hasInterface(interfaceName311, hasEncapsulationVlan(11)));

    // Confirm types
    assertThat(c, hasInterface(interfaceName1, hasInterfaceType(InterfaceType.PHYSICAL)));
    assertThat(c, hasInterface(interfaceName2, hasInterfaceType(InterfaceType.PHYSICAL)));
    assertThat(c, hasInterface(interfaceName3, hasInterfaceType(InterfaceType.PHYSICAL)));
    assertThat(c, hasInterface(interfaceName311, hasInterfaceType(InterfaceType.LOGICAL)));
  }

  @Test
  public void testInterfaceUnits() {
    String hostname = "interface-units";
    String interfaceNameUnit1 = "ethernet1/1.1";
    String interfaceNameUnit2 = "ethernet1/1.2";
    Configuration c = parseConfig(hostname);

    // Confirm interface MTU is extracted
    assertThat(c, hasInterface(interfaceNameUnit1, hasMtu(Interface.DEFAULT_INTERFACE_MTU)));
    assertThat(c, hasInterface(interfaceNameUnit2, hasMtu(1234)));

    // Confirm address is extracted
    assertThat(
        c,
        hasInterface(
            interfaceNameUnit1,
            allOf(
                hasAllAddresses(contains(ConcreteInterfaceAddress.parse("1.1.1.1/24"))),
                hasDependencies(contains(new Dependency("ethernet1/1", BIND))))));
    assertThat(
        c,
        hasInterface(
            interfaceNameUnit2,
            allOf(
                hasAllAddresses(contains(ConcreteInterfaceAddress.parse("1.1.2.1/24"))),
                hasDependencies(contains(new Dependency("ethernet1/1", BIND))))));

    // Confirm comment is extracted
    assertThat(c, hasInterface(interfaceNameUnit1, hasDescription("unit 1")));
  }

  @Test
  public void testInterfaceReference() throws IOException {
    String hostname = "interface-reference";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm reference counts are correct for both used and unused structures
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "ethernet1/1", 1));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "ethernet1/unused", 0));

    // Confirm undefined reference is detected
    assertThat(
        ccae,
        hasUndefinedReference(
            filename, INTERFACE, "ethernet1/undefined", VIRTUAL_ROUTER_INTERFACE));
  }

  @Test
  public void testInterfaceVirtualRouterAssignment() {
    String hostname = "interface-virtual-router";
    Configuration c = parseConfig(hostname);

    // Make sure each interface is associated with the correct vrf
    assertThat(
        c, hasInterface("ethernet1/1", InterfaceMatchers.hasVrf(hasName(equalTo("default")))));
    assertThat(
        c, hasInterface("ethernet1/2", InterfaceMatchers.hasVrf(hasName(equalTo("somename")))));
    assertThat(
        c, hasInterface("ethernet1/3", InterfaceMatchers.hasVrf(hasName(equalTo("somename")))));

    // Make sure the orphaned vrf is associated with the "null", constructed vrf
    assertThat(
        c, hasInterface("ethernet1/4", InterfaceMatchers.hasVrf(hasName(equalTo(NULL_VRF_NAME)))));
  }

  @Test
  public void testLogSettingsSyslog() {
    Configuration c = parseConfig("log-settings-syslog");

    // Confirm all the defined syslog servers show up in VI model
    assertThat(
        c.getLoggingServers(), containsInAnyOrder("1.1.1.1", "2.2.2.2", "3.3.3.3", "4.4.4.4"));
  }

  @Test
  public void testMgmtIface() {
    PaloAltoConfiguration c = parsePaloAltoConfig("mgmt-iface");

    assertThat(c.getMgmtIfaceAddress(), equalTo(Ip.parse("12.25.51.103")));
    assertThat(c.getMgmtIfaceGateway(), equalTo(Ip.parse("12.25.51.1")));
    assertThat(c.getMgmtIfaceNetmask(), equalTo(Ip.parse("255.255.255.128")));
  }

  @Test
  public void testNestedConfig() throws IOException {
    String hostname = "nested-config";

    // Confirm a simple extraction (hostname) works for nested config format
    assertThat(parseTextConfigs(hostname).keySet(), contains(hostname));
  }

  @Test
  public void testNestedConfigLineComments() throws IOException {
    String hostname = "nested-config-line-comments";

    // Confirm extraction works for nested configs even in the presence of line comments
    assertThat(parseTextConfigs(hostname).keySet(), contains(hostname));
  }

  @Test
  public void testNestedConfigLineMap() {
    String hostname = "nested-config";
    Flattener flattener =
        Batfish.flatten(
            CommonUtil.readResource(TESTCONFIGS_PREFIX + hostname),
            new BatfishLogger(BatfishLogger.LEVELSTR_OUTPUT, false),
            new Settings(),
            new Warnings(),
            PALO_ALTO_NESTED,
            BATFISH_FLATTENED_PALO_ALTO_HEADER);
    FlattenerLineMap lineMap = flattener.getOriginalLineMap();
    /*
     * Flattened config should be two lines: header line and set-hostname line
     * This test is only checking content of the set-hostname line
     */
    String setLineText = flattener.getFlattenedConfigurationText().split("\n", -1)[1];

    /* Confirm original line numbers are preserved */
    assertThat(lineMap.getOriginalLine(2, setLineText.indexOf("deviceconfig")), equalTo(1));
    assertThat(lineMap.getOriginalLine(2, setLineText.indexOf("system")), equalTo(2));
    assertThat(lineMap.getOriginalLine(2, setLineText.indexOf("hostname")), equalTo(3));
    assertThat(lineMap.getOriginalLine(2, setLineText.indexOf("nested-config")), equalTo(3));
  }

  @Test
  public void testNestedConfigStructureDef() throws IOException {
    String hostname = "nested-config-structure-def";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm defined structures in nested config show up with original definition line numbers
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, INTERFACE, "ethernet1/1", contains(9, 10, 11, 12)));
    assertThat(
        ccae,
        hasDefinedStructureWithDefinitionLines(
            filename, INTERFACE, "ethernet1/2", contains(16, 17, 18, 19)));
  }

  @Test
  public void testNtpServers() {
    Configuration c = parseConfig("ntp-server");

    // Confirm both ntp servers show up
    assertThat(c.getNtpServers(), containsInAnyOrder("1.1.1.1", "ntpservername"));
  }

  @Test
  public void testRulebaseWithPanorama() {
    String hostname = "panorama-rulebase";
    Configuration c = parseConfig(hostname);

    String if1name = "ethernet1/1";
    String if2name = "ethernet1/2";
    Flow.Builder baseBuilder =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setSrcIp(Ip.ZERO)
            .setDstIp(Ip.ZERO)
            .setIpProtocol(IpProtocol.TCP)
            .setTag("test")
            .setSrcPort(1);
    // See comments in the config for how this test works.
    Flow service1 = baseBuilder.setDstPort(1001).build();
    Flow service2 = baseBuilder.setDstPort(1002).build();
    Flow service3 = baseBuilder.setDstPort(1003).build();
    Flow service4 = baseBuilder.setDstPort(1004).build();

    // Cross-zone flows (starting e2)
    assertThat(
        c,
        hasInterface(
            if1name,
            hasOutgoingFilter(
                allOf(
                    accepts(service1, if2name, c),
                    rejects(service2, if2name, c),
                    accepts(service3, if2name, c),
                    rejects(service4, if2name, c)))));

    // Intra-zone flows (starting e1)
    assertThat(
        c,
        hasInterface(
            if1name,
            hasOutgoingFilter(
                allOf(
                    accepts(service1, if1name, c),
                    rejects(service2, if1name, c),
                    accepts(service3, if1name, c),
                    accepts(service4, if1name, c)))));
  }

  @Test
  public void testRulebaseInApplicationWithPanorama() {
    String hostname = "panorama-rulebase-applications";
    Configuration c = parseConfig(hostname);

    String if1name = "ethernet1/1";
    String if2name = "ethernet1/2";
    Flow.Builder tcpBaseBuilder =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setSrcIp(Ip.ZERO)
            .setDstIp(Ip.ZERO)
            .setIpProtocol(IpProtocol.TCP)
            .setTag("test")
            .setSrcPort(1);
    Flow.Builder udpBaseBuilder =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setSrcIp(Ip.ZERO)
            .setDstIp(Ip.ZERO)
            .setIpProtocol(IpProtocol.UDP)
            .setTag("test")
            .setSrcPort(1);
    // See comments in the config for how this test works.
    Flow webapplication = tcpBaseBuilder.setDstPort(80).build();
    Flow sslapplication = tcpBaseBuilder.setDstPort(443).build();
    Flow snmpapplication = udpBaseBuilder.setDstPort(161).build();
    Flow pingapplication =
        Flow.builder()
            .setIngressNode(c.getHostname())
            .setSrcIp(Ip.ZERO)
            .setDstIp(Ip.ZERO)
            .setIpProtocol(IpProtocol.ICMP)
            .setTag("test")
            .setIcmpType(IcmpType.ECHO_REPLY)
            .setIcmpCode(0)
            .build();
    assertThat(
        c,
        hasInterface(
            if1name,
            hasOutgoingFilter(
                allOf(
                    rejects(webapplication, if2name, c),
                    accepts(sslapplication, if2name, c),
                    accepts(snmpapplication, if2name, c),
                    accepts(pingapplication, if2name, c)))));
  }

  @Test
  public void testRulebase() {
    String hostname = "rulebase";
    Configuration c = parseConfig(hostname);

    String if1name = "ethernet1/1";
    String if2name = "ethernet1/2";
    String if3name = "ethernet1/3";
    String if4name = "ethernet1/4";
    Flow z1ToZ1permitted = createFlow("1.1.2.255", "1.1.1.2");
    Flow z1ToZ1rejectedSource = createFlow("2.2.2.2", "1.1.1.2");
    Flow z1ToZ1rejectedDestination = createFlow("1.1.2.2", "1.1.1.3");
    Flow z1ToZ1rejectedService = createFlow("1.1.2.2", "1.1.1.2", IpProtocol.TCP, 1, 999);
    Flow z2ToZ1permitted = createFlow("1.1.4.255", "1.1.1.2");
    Flow noZoneToZ1rejected = createFlow("1.1.3.2", "1.1.1.2");

    // Confirm flow matching deny rule is rejected
    assertThat(
        c, hasInterface(if1name, hasOutgoingFilter(rejects(z1ToZ1rejectedService, if2name, c))));

    // Confirm intrazone flow matching allow rule is accepted
    assertThat(c, hasInterface(if1name, hasOutgoingFilter(accepts(z1ToZ1permitted, if2name, c))));
    // Confirm intrazone flow not matching allow rule (bad destination address) is rejected
    assertThat(
        c,
        hasInterface(if1name, hasOutgoingFilter(rejects(z1ToZ1rejectedDestination, if2name, c))));
    // Confirm intrazone flow not matching allow rule (source address negated) is rejected
    assertThat(
        c, hasInterface(if1name, hasOutgoingFilter(rejects(z1ToZ1rejectedSource, if2name, c))));

    // Confirm interzone flow matching allow rule is accepted
    assertThat(c, hasInterface(if1name, hasOutgoingFilter(accepts(z2ToZ1permitted, if4name, c))));

    // Confirm flow from an unzoned interface is rejected
    assertThat(
        c, hasInterface(if1name, hasOutgoingFilter(rejects(noZoneToZ1rejected, if3name, c))));
  }

  @Test
  public void testRulebaseService() {
    String hostname = "rulebase-service";
    Configuration c = parseConfig(hostname);

    String if1name = "ethernet1/1";
    String if4name = "ethernet1/4";
    Flow z1ToZ2OverridePermitted = createFlow("1.1.1.1", "1.1.1.2", IpProtocol.TCP, 999, 1);
    Flow z1ToZ2HttpRejected = createFlow("1.1.1.1", "1.1.1.2", IpProtocol.TCP, 999, 80);
    Flow z1ToZ2HttpsPermitted = createFlow("1.1.1.1", "1.1.1.2", IpProtocol.TCP, 999, 443);

    // Confirm flow matching overridden service is permitted
    assertThat(
        c, hasInterface(if1name, hasOutgoingFilter(accepts(z1ToZ2OverridePermitted, if4name, c))));
    // Confirm flow matching built-in service that was overridden is rejected
    assertThat(
        c, hasInterface(if1name, hasOutgoingFilter(rejects(z1ToZ2HttpRejected, if4name, c))));
    // Confirm flow matching built-in service, indirectly referenced is permitted
    assertThat(
        c, hasInterface(if1name, hasOutgoingFilter(accepts(z1ToZ2HttpsPermitted, if4name, c))));
  }

  @Test
  public void testRulebaseDefault() {
    String hostname = "rulebase-default";
    Configuration c = parseConfig(hostname);

    String if1name = "ethernet1/1";
    String if2name = "ethernet1/2";
    String if3name = "ethernet1/3";
    String if4name = "ethernet1/4";
    Flow z1ToZ1 = createFlow("1.1.1.2", "1.1.2.2");
    Flow z1ToZ2 = createFlow("1.1.1.2", "1.1.4.2");
    Flow z2ToZ1 = createFlow("1.1.4.2", "1.1.1.2");
    Flow noZoneToZ1 = createFlow("1.1.3.2", "1.1.1.2");
    Flow z1ToNoZone = createFlow("1.1.1.2", "1.1.3.2");

    // Confirm intrazone flow is accepted by default
    assertThat(c, hasInterface(if2name, hasOutgoingFilter(accepts(z1ToZ1, if1name, c))));

    // Confirm interzone flows are rejected by default
    assertThat(c, hasInterface(if4name, hasOutgoingFilter(rejects(z1ToZ2, if1name, c))));
    assertThat(c, hasInterface(if1name, hasOutgoingFilter(rejects(z2ToZ1, if4name, c))));

    // Confirm unzoned flows are rejected by default
    assertThat(c, hasInterface(if1name, hasOutgoingFilter(rejects(noZoneToZ1, if3name, c))));
    assertThat(c, hasInterface(if3name, hasOutgoingFilter(rejects(z1ToNoZone, if1name, c))));
  }

  @Test
  public void testRulebaseIprange() {
    String hostname = "rulebase-iprange";
    Configuration c = parseConfig(hostname);

    String if1name = "ethernet1/1";
    String if2name = "ethernet1/2";

    // rule1: 11.11.11.11 should be allowed but 11.11.11.13 shouldn't be
    Flow rule1Permitted = createFlow("11.11.11.11", "33.33.33.33");
    Flow rule1Denied = createFlow("11.11.11.13", "33.33.33.33");

    assertThat(c, hasInterface(if1name, hasOutgoingFilter(accepts(rule1Permitted, if2name, c))));
    assertThat(c, hasInterface(if1name, hasOutgoingFilter(rejects(rule1Denied, if2name, c))));
  }

  @Test
  public void testRulebaseReference() throws IOException {
    String hostname = "rulebase";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String serviceName = computeObjectName(DEFAULT_VSYS_NAME, "SERVICE1");
    String z1Name = computeObjectName(DEFAULT_VSYS_NAME, "z1");
    String z2Name = computeObjectName(DEFAULT_VSYS_NAME, "z2");
    String zoneUndefName = computeObjectName(DEFAULT_VSYS_NAME, "ZONE_UNDEF");

    // Confirm reference count is correct for used structure
    assertThat(ccae, hasNumReferrers(filename, PaloAltoStructureType.SERVICE, serviceName, 1));
    assertThat(ccae, hasNumReferrers(filename, PaloAltoStructureType.ZONE, z1Name, 2));
    assertThat(ccae, hasNumReferrers(filename, PaloAltoStructureType.ZONE, z2Name, 2));

    // Confirm undefined references are detected
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP,
            "SERVICE_UNDEF",
            PaloAltoStructureUsage.RULEBASE_SERVICE));
    assertThat(
        ccae,
        hasUndefinedReference(
            filename,
            PaloAltoStructureType.ZONE,
            zoneUndefName,
            PaloAltoStructureUsage.RULE_FROM_ZONE));

    // Confirm builtins do not show up as undefined references
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                filename, SERVICE_OR_SERVICE_GROUP, ServiceBuiltIn.ANY.getName())));
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                filename, SERVICE_OR_SERVICE_GROUP, ServiceBuiltIn.APPLICATION_DEFAULT.getName())));
  }

  @Test
  public void testVsysRulebase() {
    String hostname = "vsys-rulebase";
    Configuration c = parseConfig(hostname);

    String if1name = "ethernet1/1";
    String if2name = "ethernet1/2";
    String if3name = "ethernet1/3";
    String if4name = "ethernet1/4";
    Flow flow = createFlow("1.1.1.1", "2.2.2.2");

    // Confirm flow is rejected in vsys1
    assertThat(c, hasInterface(if1name, hasOutgoingFilter(rejects(flow, if2name, c))));

    // Confirm intravsys flow is accepted in vsys2
    assertThat(c, hasInterface(if3name, hasOutgoingFilter(accepts(flow, if4name, c))));
    // Confirm intervsys flow (even from same zone name) is rejected in vsys2
    assertThat(c, hasInterface(if3name, hasOutgoingFilter(rejects(flow, if2name, c))));
  }

  @Test
  public void testStaticRoute() throws IOException {
    String vrName = "somename";
    String vr2Name = "vr2";
    String hostname = "static-route";
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);

    // Confirm static route shows up with correct extractions
    assertThat(c, hasVrf(vrName, hasStaticRoutes(hasItem(hasAdministrativeCost(equalTo(123))))));
    assertThat(c, hasVrf(vrName, hasStaticRoutes(hasItem(hasMetric(equalTo(12L))))));
    assertThat(
        c, hasVrf(vrName, hasStaticRoutes(hasItem(hasNextHopIp(equalTo(Ip.parse("1.1.1.1")))))));
    assertThat(
        c, hasVrf(vrName, hasStaticRoutes(hasItem(hasNextHopInterface(equalTo("ethernet1/1"))))));
    assertThat(c, hasVrf(vrName, hasStaticRoutes(hasItem(hasPrefix(Prefix.ZERO)))));
    assertThat(
        c,
        hasVrf(
            vr2Name,
            hasStaticRoutes(
                hasItem(allOf(hasPrefix(Prefix.strict("10.0.0.0/8")), hasNextVrf(vrName))))));
    // static-route with undefined next-vr should not be converted
    assertThat(c, hasVrf(vr2Name, hasStaticRoutes(not(hasItem(hasPrefix(Prefix.ZERO))))));
    // static-route with next-vr same as its own virtual-router should not be converted
    assertThat(
        c, hasVrf(vr2Name, hasStaticRoutes(not(hasItem(hasPrefix(Prefix.strict("1.0.0.0/8")))))));

    // static route with next hop specified as prefix
    assertThat(
        c,
        hasVrf(
            vr2Name,
            hasStaticRoutes(
                hasItem(
                    allOf(
                        hasPrefix(Prefix.strict("2.0.0.0/8")),
                        hasNextHopIp(Ip.parse("2.2.2.2")))))));

    // assert static interface route reference
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();
    String filename = "configs/" + hostname;
    assertThat(
        ccae, hasUndefinedReference(filename, INTERFACE, "ethernet1/1", STATIC_ROUTE_INTERFACE));
    assertThat(
        ccae, hasUndefinedReference(filename, PaloAltoStructureType.VIRTUAL_ROUTER, "fakevr"));
  }

  @Test
  public void testStaticRouteExtraction() {
    String hostname = "static-route";
    PaloAltoConfiguration vc = parsePaloAltoConfig(hostname);
    String vrName = "vr2";

    assertThat(vc.getVirtualRouters(), hasKey(vrName));

    assertThat(vc.getVirtualRouters().get(vrName).getStaticRoutes(), hasKey("ROUTE2"));

    StaticRoute sr = vc.getVirtualRouters().get(vrName).getStaticRoutes().get("ROUTE2");

    assertEquals(sr.getDestination(), Prefix.ZERO);
    assertEquals(sr.getNextVr(), "fakevr");
  }

  @Test
  public void testStaticRouteDefaults() {
    String hostname = "static-route-defaults";
    String vrName = "default";
    Configuration c = parseConfig(hostname);

    // Confirm static route shows up with correct defaults
    assertThat(c, hasVrf(vrName, hasStaticRoutes(hasItem(hasAdministrativeCost(equalTo(10))))));
    assertThat(c, hasVrf(vrName, hasStaticRoutes(hasItem(hasMetric(equalTo(10L))))));
    assertThat(c, hasVrf(vrName, hasStaticRoutes(hasItem(hasPrefix(Prefix.strict("0.0.0.0/0"))))));
  }

  @Test
  public void testService() {
    String hostname = "service";
    Configuration c = parseConfig(hostname);

    String service1AclName = computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SERVICE1");
    String service2AclName = computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SERVICE2");
    String service3AclName = computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SERVICE3");
    String service4AclName = computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SERVICE 4");
    String serviceGroup1AclName = computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SG1");
    String serviceGroup2AclName = computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SG 2");

    Flow service1Flow = createFlow(IpProtocol.TCP, 999, 1);
    Flow service2Flow = createFlow(IpProtocol.UDP, 4, 999);
    Flow service3Flow1 = createFlow(IpProtocol.UDP, 10, 5);
    Flow service3Flow2 = createFlow(IpProtocol.UDP, 9, 6);
    Flow service4Flow = createFlow(IpProtocol.SCTP, 1, 1);

    // Confirm services accept flows matching their definitions and reject others
    assertThat(c, hasIpAccessList(service1AclName, accepts(service1Flow, null, c)));
    assertThat(c, hasIpAccessList(service1AclName, rejects(service2Flow, null, c)));

    assertThat(c, hasIpAccessList(service2AclName, accepts(service2Flow, null, c)));
    assertThat(c, hasIpAccessList(service2AclName, accepts(service3Flow2, null, c)));
    assertThat(c, hasIpAccessList(service2AclName, rejects(service3Flow1, null, c)));

    assertThat(c, hasIpAccessList(service3AclName, accepts(service3Flow1, null, c)));
    assertThat(c, hasIpAccessList(service3AclName, accepts(service3Flow2, null, c)));
    assertThat(c, hasIpAccessList(service3AclName, rejects(service2Flow, null, c)));

    assertThat(c, hasIpAccessList(service4AclName, accepts(service4Flow, null, c)));
    assertThat(c, hasIpAccessList(service4AclName, rejects(service1Flow, null, c)));

    // Confirm service-group SG1 accepts flows matching its member definitions and rejects others
    assertThat(c, hasIpAccessList(serviceGroup1AclName, accepts(service1Flow, null, c)));
    assertThat(c, hasIpAccessList(serviceGroup1AclName, accepts(service2Flow, null, c)));
    assertThat(c, hasIpAccessList(serviceGroup1AclName, rejects(service3Flow1, null, c)));

    // Confirm SG2 accepts flows matching its member's member definitions and rejects others
    assertThat(c, hasIpAccessList(serviceGroup2AclName, accepts(service1Flow, null, c)));
    assertThat(c, hasIpAccessList(serviceGroup2AclName, accepts(service2Flow, null, c)));
    assertThat(c, hasIpAccessList(serviceGroup2AclName, rejects(service3Flow1, null, c)));

    // Verify transitive name.
    IpAccessList service1 = c.getIpAccessLists().get(service1AclName);
    IpAccessList sg1 = c.getIpAccessLists().get(serviceGroup1AclName);
    IpAccessListLine line1 = sg1.getLines().get(0);
    assertThat(line1.getName(), equalTo(service1.getSourceName()));
  }

  @Test
  public void testServiceReference() throws IOException {
    String hostname = "service";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String service1Name = computeObjectName(DEFAULT_VSYS_NAME, "SERVICE1");
    String service2Name = computeObjectName(DEFAULT_VSYS_NAME, "SERVICE2");
    String service3Name = computeObjectName(DEFAULT_VSYS_NAME, "SERVICE3");
    String service4Name = computeObjectName(DEFAULT_VSYS_NAME, "SERVICE 4");
    String serviceGroup1Name = computeObjectName(DEFAULT_VSYS_NAME, "SG1");
    String serviceGroup2Name = computeObjectName(DEFAULT_VSYS_NAME, "SG 2");

    // Confirm structure definitions are tracked
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, service1Name));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, service2Name));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, service3Name));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, service4Name));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE_GROUP, serviceGroup1Name));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE_GROUP, serviceGroup2Name));

    // Confirm structure references are tracked
    assertThat(ccae, hasNumReferrers(filename, SERVICE, service1Name, 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE, service2Name, 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE, service3Name, 0));
    assertThat(ccae, hasNumReferrers(filename, SERVICE, service4Name, 0));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_GROUP, serviceGroup1Name, 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE_GROUP, serviceGroup2Name, 0));

    // Confirm undefined reference shows up as such
    assertThat(
        ccae, hasUndefinedReference(filename, SERVICE_OR_SERVICE_GROUP, "SERVICE_UNDEFINED"));
  }

  @Test
  public void testServiceBuiltin() {
    String hostname = "service-built-in";
    Configuration c = parseConfig(hostname);

    String serviceGroupHttpsAclName =
        computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SG-HTTPS");
    String serviceGroupHttpAclName = computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SG-HTTP");
    String serviceGroupAnyAclName = computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SG-ANY");

    Flow flowHttps = createFlow(IpProtocol.TCP, 999, 443);
    Flow flowHttp = createFlow(IpProtocol.TCP, 999, 80);
    Flow flowHttpAlt = createFlow(IpProtocol.TCP, 999, 8080);
    Flow flowOther = createFlow(IpProtocol.TCP, 999, 1);

    // Confirm HTTPS built-in accepts flows matching built-in HTTPS definition only
    assertThat(c, hasIpAccessList(serviceGroupHttpsAclName, accepts(flowHttps, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupHttpsAclName, rejects(flowHttp, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupHttpsAclName, rejects(flowHttpAlt, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupHttpsAclName, rejects(flowOther, null, c)));

    // Confirm HTTP built-in accepts flows matching built-in HTTP definitions only
    assertThat(c, hasIpAccessList(serviceGroupHttpAclName, rejects(flowHttps, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupHttpAclName, accepts(flowHttp, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupHttpAclName, accepts(flowHttpAlt, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupHttpAclName, rejects(flowOther, null, c)));

    // Confirm any built-in accepts all flows
    assertThat(c, hasIpAccessList(serviceGroupAnyAclName, accepts(flowHttps, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupAnyAclName, accepts(flowHttp, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupAnyAclName, accepts(flowHttpAlt, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupAnyAclName, accepts(flowOther, null, c)));
  }

  @Test
  public void testServiceBuiltinReference() throws IOException {
    String hostname = "service-built-in";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String serviceHttp =
        computeObjectName(DEFAULT_VSYS_NAME, ServiceBuiltIn.SERVICE_HTTP.getName());
    String serviceHttps =
        computeObjectName(DEFAULT_VSYS_NAME, ServiceBuiltIn.SERVICE_HTTPS.getName());
    String serviceAny = computeObjectName(DEFAULT_VSYS_NAME, ServiceBuiltIn.ANY.getName());

    String serviceGroupHttpName = computeObjectName(DEFAULT_VSYS_NAME, "SG-HTTP");
    String serviceGroupHttpsName = computeObjectName(DEFAULT_VSYS_NAME, "SG-HTTPS");
    String serviceGroupAnyName = computeObjectName(DEFAULT_VSYS_NAME, "SG-ANY");

    // Confirm structure definitions are tracked
    assertThat(ccae, hasDefinedStructure(filename, SERVICE_GROUP, serviceGroupHttpName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE_GROUP, serviceGroupHttpsName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE_GROUP, serviceGroupAnyName));

    // Confirm built-ins that are not overridden are not considered defined structures
    assertThat(ccae, not(hasDefinedStructure(filename, SERVICE, serviceHttp)));
    assertThat(ccae, not(hasDefinedStructure(filename, SERVICE, serviceHttps)));
    assertThat(ccae, not(hasDefinedStructure(filename, SERVICE, serviceAny)));

    // Confirm there are no undefined references for the built-ins
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                filename, SERVICE_OR_SERVICE_GROUP, ServiceBuiltIn.ANY.getName())));
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                filename, SERVICE_OR_SERVICE_GROUP, ServiceBuiltIn.SERVICE_HTTP.getName())));
    assertThat(
        ccae,
        not(
            hasUndefinedReference(
                filename, SERVICE_OR_SERVICE_GROUP, ServiceBuiltIn.SERVICE_HTTPS.getName())));
  }

  @Test
  public void testServiceBuiltinOverride() {
    String hostname = "service-built-in-override";
    Configuration c = parseConfig(hostname);

    String serviceGroupOverrideHttpsAclName =
        computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SG-OVERRIDE-HTTPS");
    String serviceGroupOverrideHttpAclName =
        computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SG-OVERRIDE-HTTP");
    String serviceGroupComboAclName =
        computeServiceGroupMemberAclName(DEFAULT_VSYS_NAME, "SG-COMBO");

    Flow flowHttp = createFlow(IpProtocol.TCP, 999, 80);
    Flow flowHttpOverride = createFlow(IpProtocol.TCP, 999, 1);
    Flow flowHttps = createFlow(IpProtocol.TCP, 999, 443);
    Flow flowHttpsOverride = createFlow(IpProtocol.TCP, 999, 2);
    Flow flowAny = createFlow(IpProtocol.UDP, 999, 999);
    Flow flowAnyOverride = createFlow(IpProtocol.TCP, 999, 3);

    // Confirm HTTPS service group accepts flows matching overridden HTTPS definition only
    assertThat(
        c, hasIpAccessList(serviceGroupOverrideHttpsAclName, accepts(flowHttpsOverride, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupOverrideHttpsAclName, rejects(flowHttps, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupOverrideHttpsAclName, rejects(flowHttp, null, c)));

    // Confirm HTTP service group accepts flows matching overridden HTTP definition only
    assertThat(
        c, hasIpAccessList(serviceGroupOverrideHttpAclName, accepts(flowHttpOverride, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupOverrideHttpAclName, rejects(flowHttp, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupOverrideHttpAclName, rejects(flowHttps, null, c)));

    // Confirm service group accepts flows matching overridden any definition only
    assertThat(c, hasIpAccessList(serviceGroupComboAclName, accepts(flowAnyOverride, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupComboAclName, rejects(flowAny, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupComboAclName, rejects(flowHttp, null, c)));
    assertThat(c, hasIpAccessList(serviceGroupComboAclName, rejects(flowHttps, null, c)));
  }

  @Test
  public void testServiceBuiltinOverrideReference() throws IOException {
    String hostname = "service-built-in-override";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String serviceAnyName = computeObjectName(DEFAULT_VSYS_NAME, ServiceBuiltIn.ANY.getName());
    String serviceHttpName =
        computeObjectName(DEFAULT_VSYS_NAME, ServiceBuiltIn.SERVICE_HTTP.getName());
    String serviceHttpsName =
        computeObjectName(DEFAULT_VSYS_NAME, ServiceBuiltIn.SERVICE_HTTPS.getName());
    String serviceGroupOverrideHttpName = computeObjectName(DEFAULT_VSYS_NAME, "SG-OVERRIDE-HTTP");
    String serviceGroupOverrideHttpsName =
        computeObjectName(DEFAULT_VSYS_NAME, "SG-OVERRIDE-HTTPS");
    String serviceGroupComboName = computeObjectName(DEFAULT_VSYS_NAME, "SG-COMBO");

    // Confirm structure definitions are tracked
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, serviceHttpName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, serviceHttpsName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, serviceAnyName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE_GROUP, serviceGroupOverrideHttpName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE_GROUP, serviceGroupOverrideHttpsName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE_GROUP, serviceGroupComboName));

    // Confirm structure references are tracked
    assertThat(ccae, hasNumReferrers(filename, SERVICE, serviceHttpName, 2));
    assertThat(ccae, hasNumReferrers(filename, SERVICE, serviceHttpsName, 2));
    assertThat(ccae, hasNumReferrers(filename, SERVICE, serviceAnyName, 1));
  }

  @Test
  public void testSharedGateway() {
    PaloAltoConfiguration c = parsePaloAltoConfig("shared-gateway");

    // Confirm shared-gateways show up in the vendor model
    Map<String, Vsys> sharedGateways = c.getSharedGateways();
    assertThat(sharedGateways, hasKey("sg1"));
    assertThat(sharedGateways, hasKey("sg2"));
    assertThat(sharedGateways, hasKey("sg3"));

    // Confirm display names show up as well
    assertThat(sharedGateways.get("sg1").getDisplayName(), equalTo("shared-gateway1"));
    assertThat(sharedGateways.get("sg2").getDisplayName(), equalTo("shared gateway2"));
    assertThat(sharedGateways.get("sg3").getDisplayName(), equalTo("invalid shared gateway"));
  }

  @Test
  public void testSharedGatewayReference() throws IOException {
    String hostname = "shared-gateway";
    String filename = "configs/" + hostname;

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm structure definitions are recorded correctly, even for badly named shared-gateway
    assertThat(ccae, hasDefinedStructure(filename, SHARED_GATEWAY, "sg1"));
    assertThat(ccae, hasDefinedStructure(filename, SHARED_GATEWAY, "sg2"));
    assertThat(ccae, hasDefinedStructure(filename, SHARED_GATEWAY, "sg3"));

    // Confirm structure references are counted correctly
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "ethernet1/1", 2));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "ethernet1/2", 1));

    // Confirm the undefined interface is properly detected as an undefined reference
    assertThat(ccae, hasUndefinedReference(filename, INTERFACE, "ethernet1/3", IMPORT_INTERFACE));
  }

  @Test
  public void testVirtualRouterExtraction() {
    PaloAltoConfiguration c = parseNestedConfig("virtual-router");
    VirtualRouter vr = c.getVirtualRouters().get("VR");
    assertThat(vr, notNullValue());
    AdminDistances ads = vr.getAdminDists();
    assertThat(ads.getEbgp(), equalTo(21));
    assertThat(ads.getIbgp(), equalTo(201));
    assertThat(ads.getOspfExt(), equalTo(111));
    assertThat(ads.getOspfInt(), equalTo(31));
    assertThat(ads.getOspfV3Ext(), equalTo(112));
    assertThat(ads.getOspfV3Int(), equalTo(32));
    assertThat(ads.getRip(), equalTo(121));
    assertThat(ads.getStatic(), equalTo(11));
    assertThat(ads.getStaticv6(), equalTo(12));
  }

  @Test
  public void testVirtualRouterConversion() {
    Configuration c = parseConfig("virtual-router");
    assertThat(c, hasVrf("VR", any(Vrf.class)));
    // TODO: proper conversion test.
  }

  @Test
  public void testVirtualRouterInterfaces() {
    String hostname = "virtual-router-interfaces";
    Configuration c = parseConfig(hostname);

    assertThat(c, hasVrf("default", hasInterfaces(hasItem("ethernet1/1"))));
    assertThat(c, hasVrf("somename", hasInterfaces(hasItems("ethernet1/2", "ethernet1/3"))));
    assertThat(c, hasVrf("some other name", hasInterfaces(emptyIterable())));
  }

  @Test
  public void testVsysImport() {
    PaloAltoConfiguration c = parsePaloAltoConfig("vsys-import");

    // Confirm vsys imports are extracted correctly
    assertThat(c.getVirtualSystems().get("vsys1").getImportedVsyses(), emptyIterable());
    assertThat(c.getVirtualSystems().get("vsys2").getImportedVsyses(), contains("vsys1"));
  }

  @Test
  public void testVsysService() {
    String hostname = "vsys-service";
    Configuration c = parseConfig(hostname);

    String vsys1ServiceName = computeServiceGroupMemberAclName("vsys1", "SERVICE1");
    String vsys2ServiceName = computeServiceGroupMemberAclName("vsys2", "SERVICE1");
    String sharedServiceName = computeServiceGroupMemberAclName(SHARED_VSYS_NAME, "SERVICE1");
    String sharedOnlyServiceName = computeServiceGroupMemberAclName(SHARED_VSYS_NAME, "SERVICE2");

    String vsys1ServiceGroupName = computeServiceGroupMemberAclName("vsys1", "SG1");
    String vsys2ServiceGroupName = computeServiceGroupMemberAclName("vsys2", "SG1");
    String sharedServiceGroupName = computeServiceGroupMemberAclName(SHARED_VSYS_NAME, "SG1");

    Flow vsys1Flow = createFlow(IpProtocol.UDP, 999, 1);
    Flow vsys2Flow = createFlow(IpProtocol.UDP, 999, 2);
    Flow sharedFlow = createFlow(IpProtocol.UDP, 999, 3);
    Flow sharedOnlyFlow = createFlow(IpProtocol.UDP, 999, 4);

    // Confirm services accept flows matching their definitions and reject others
    assertThat(c, hasIpAccessList(vsys1ServiceName, accepts(vsys1Flow, null, c)));
    assertThat(c, hasIpAccessList(vsys1ServiceName, rejects(vsys2Flow, null, c)));
    assertThat(c, hasIpAccessList(vsys2ServiceName, accepts(vsys2Flow, null, c)));
    assertThat(c, hasIpAccessList(vsys2ServiceName, rejects(vsys1Flow, null, c)));
    assertThat(c, hasIpAccessList(sharedServiceName, accepts(sharedFlow, null, c)));
    assertThat(c, hasIpAccessList(sharedServiceName, rejects(vsys1Flow, null, c)));
    assertThat(c, hasIpAccessList(sharedOnlyServiceName, accepts(sharedOnlyFlow, null, c)));
    assertThat(c, hasIpAccessList(sharedOnlyServiceName, rejects(vsys1Flow, null, c)));

    // Confirm group in vsys1 namespace references its own service, not shared or neighbor services
    assertThat(c, hasIpAccessList(vsys1ServiceGroupName, accepts(vsys1Flow, null, c)));
    assertThat(c, hasIpAccessList(vsys1ServiceGroupName, rejects(vsys2Flow, null, c)));
    assertThat(c, hasIpAccessList(vsys1ServiceGroupName, rejects(sharedFlow, null, c)));
    // Confirm group in the vsys1 namespace correctly references the shared service
    assertThat(c, hasIpAccessList(vsys1ServiceGroupName, accepts(sharedOnlyFlow, null, c)));

    // Confirm group in vsys2 namespace references its own service, not shared or neighbor services
    assertThat(c, hasIpAccessList(vsys2ServiceGroupName, accepts(vsys2Flow, null, c)));
    assertThat(c, hasIpAccessList(vsys2ServiceGroupName, rejects(vsys1Flow, null, c)));
    assertThat(c, hasIpAccessList(vsys2ServiceGroupName, rejects(sharedFlow, null, c)));
    // Confirm group in the vsys2 namespace correctly references the shared service
    assertThat(c, hasIpAccessList(vsys2ServiceGroupName, accepts(sharedOnlyFlow, null, c)));

    // Confirm group in shared namespace references its own service, not vsys specific services
    assertThat(c, hasIpAccessList(sharedServiceGroupName, accepts(sharedFlow, null, c)));
    assertThat(c, hasIpAccessList(sharedServiceGroupName, rejects(vsys1Flow, null, c)));
    assertThat(c, hasIpAccessList(sharedServiceGroupName, rejects(vsys2Flow, null, c)));
    // Confirm group in the shared namespace correctly references the shared service
    assertThat(c, hasIpAccessList(sharedServiceGroupName, accepts(sharedOnlyFlow, null, c)));
  }

  @Test
  public void testVsysServiceReference() throws IOException {
    String hostname = "vsys-service";
    String filename = "configs/" + hostname;
    String vsys1ServiceName = computeObjectName("vsys1", "SERVICE1");
    String vsys2ServiceName = computeObjectName("vsys2", "SERVICE1");
    String sharedServiceName = computeObjectName(SHARED_VSYS_NAME, "SERVICE1");
    String sharedOnlyServiceName = computeObjectName(SHARED_VSYS_NAME, "SERVICE2");

    Batfish batfish = getBatfishForConfigurationNames(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    // Confirm structure definitions are tracked separately for each vsys
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, vsys1ServiceName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, vsys2ServiceName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, sharedServiceName));
    assertThat(ccae, hasDefinedStructure(filename, SERVICE, sharedOnlyServiceName));

    // Confirm structure references are tracked separately for each vsys
    assertThat(ccae, hasNumReferrers(filename, SERVICE, vsys1ServiceName, 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE, vsys2ServiceName, 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE, sharedServiceName, 1));
    assertThat(ccae, hasNumReferrers(filename, SERVICE, sharedOnlyServiceName, 3));
  }

  @Test
  public void testVsysZones() throws IOException {
    String hostname = "vsys-zones";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String zoneName = computeObjectName("vsys1", "z1");
    String zoneEmptyName = computeObjectName("vsys11", "z1");

    // Confirm zone definitions are recorded properly
    assertThat(ccae, hasDefinedStructure(filename, ZONE, zoneName));
    assertThat(ccae, hasDefinedStructure(filename, ZONE, zoneEmptyName));

    // Confirm interface references in zones are recorded properly
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "ethernet1/1", 1));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "ethernet1/2", 1));

    // Confirm zones contain the correct interfaces
    assertThat(
        c,
        hasZone(
            zoneName,
            hasMemberInterfaces(
                containsInAnyOrder("ethernet1/1", "ethernet1/2", "ethernet1/3.1"))));
    assertThat(c, hasZone(zoneEmptyName, hasMemberInterfaces(empty())));

    // Confirm interfaces have the correct zones
    assertThat(
        c,
        allOf(
            hasInterface("ethernet1/1", hasZoneName("z1")),
            hasInterface("ethernet1/2", hasZoneName("z1")),
            hasInterface("ethernet1/3.1", hasZoneName("z1")),
            hasInterface("ethernet1/3", hasZoneName(nullValue()))));
  }

  @Test
  public void testZoneExternal() {
    PaloAltoConfiguration c = parsePaloAltoConfig("zone-external");

    // Make sure the vsys and zones we're going to check exist
    assertThat(c.getVirtualSystems(), hasKey("vsys3"));
    SortedMap<String, Zone> zones = c.getVirtualSystems().get("vsys3").getZones();
    assertThat(zones.keySet(), containsInAnyOrder("ZONE1", "ZONE2"));

    // Make sure the external zones are correctly populated with the specified external names
    Zone z1 = zones.get("ZONE1");
    assertThat(z1.getType(), equalTo(EXTERNAL));
    assertThat(z1.getExternalNames(), containsInAnyOrder("vsys1", "sg1"));

    Zone z2 = zones.get("ZONE2");
    assertThat(z2.getType(), equalTo(EXTERNAL));
    assertThat(z2.getExternalNames(), contains("vsys2"));
  }

  @Test
  public void testZones() throws IOException {
    String hostname = "zones";
    String filename = "configs/" + hostname;
    Batfish batfish = getBatfishForConfigurationNames(hostname);
    Configuration c = batfish.loadConfigurations().get(hostname);
    ConvertConfigurationAnswerElement ccae =
        batfish.loadConvertConfigurationAnswerElementOrReparse();

    String z1Name = computeObjectName(DEFAULT_VSYS_NAME, "zone 1");
    String zEmptyName = computeObjectName(DEFAULT_VSYS_NAME, "zempty");

    // Confirm zone definitions are recorded properly
    assertThat(ccae, hasDefinedStructure(filename, ZONE, z1Name));
    assertThat(ccae, hasDefinedStructure(filename, ZONE, zEmptyName));

    // Confirm interface references in zones are recorded properly
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "ethernet1/1", 1));
    assertThat(ccae, hasNumReferrers(filename, INTERFACE, "ethernet1/2", 1));

    // Confirm zones contain the correct interfaces
    assertThat(
        c, hasZone(z1Name, hasMemberInterfaces(containsInAnyOrder("ethernet1/1", "ethernet1/2"))));
    assertThat(c, hasZone(zEmptyName, hasMemberInterfaces(empty())));

    // Confirm interfaces are associated with the correct zones
    assertThat(c, hasInterface("ethernet1/1", hasZoneName(equalTo("zone 1"))));
    assertThat(c, hasInterface("ethernet1/2", hasZoneName(equalTo("zone 1"))));
    assertThat(c, hasInterface("ethernet1/3", hasZoneName(is(nullValue()))));
  }
}

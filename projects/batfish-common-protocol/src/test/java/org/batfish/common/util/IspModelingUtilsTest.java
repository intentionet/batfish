package org.batfish.common.util;

import static org.batfish.common.Warnings.TAG_RED_FLAG;
import static org.batfish.common.util.IspModelingUtils.EXPORT_POLICY_ON_ISP;
import static org.batfish.datamodel.BgpPeerConfig.ALL_AS_NUMBERS;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasLocalAs;
import static org.batfish.datamodel.matchers.BgpNeighborMatchers.hasRemoteAs;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasActiveNeighbor;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasMultipathEbgp;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasNeighbors;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasRouterId;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasDeviceType;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasHostname;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasInterface;
import static org.batfish.datamodel.matchers.ConfigurationMatchers.hasVrf;
import static org.batfish.datamodel.matchers.InterfaceMatchers.hasAllAddresses;
import static org.batfish.datamodel.matchers.VrfMatchers.hasBgpProcess;
import static org.batfish.datamodel.matchers.VrfMatchers.hasStaticRoutes;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.common.util.IspModelingUtils.IspInfo;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DeviceType;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.isp_configuration.BorderInterfaceInfo;
import org.batfish.datamodel.isp_configuration.IspConfiguration;
import org.batfish.datamodel.isp_configuration.IspFilter;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.junit.Test;

/** Tests for {@link IspModelingUtils} */
public class IspModelingUtilsTest {

  @Test
  public void testNonExistentNode() {
    NetworkFactory nf = new NetworkFactory();

    Warnings warnings = new Warnings(true, true, true);
    IspModelingUtils.combineIspConfigurations(
        ImmutableMap.of("conf", nf.configurationBuilder().setHostname("conf").build()),
        ImmutableList.of(
            new IspConfiguration(
                ImmutableList.of(new BorderInterfaceInfo(NodeInterfacePair.of("conf1", "init1"))),
                IspFilter.ALLOW_ALL)),
        warnings);

    assertThat(
        warnings.getRedFlagWarnings(),
        equalTo(
            ImmutableList.of(
                new Warning(
                    "ISP Modeling: Non-existent border node conf1 specified in ISP configuration",
                    TAG_RED_FLAG))));
  }

  @Test
  public void testNonExistentInterface() {
    NetworkFactory nf = new NetworkFactory();
    Warnings warnings = new Warnings(true, true, true);

    IspModelingUtils.populateIspInfos(
        nf.configurationBuilder().setHostname("conf").build(),
        ImmutableSet.of("init"),
        ImmutableList.of(),
        ImmutableList.of(),
        Maps.newHashMap(),
        warnings);

    assertThat(
        warnings.getRedFlagWarnings(),
        equalTo(
            ImmutableList.of(
                new Warning("ISP Modeling: Cannot find interface init on node conf", TAG_RED_FLAG),
                new Warning(
                    "ISP Modeling: Cannot find any valid eBGP configurations for provided interfaces on node conf",
                    TAG_RED_FLAG))));
  }

  @Test
  public void testReverseLocalAndRemote() {
    BgpActivePeerConfig bgpActivePeerConfig =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    BgpActivePeerConfig reversedPeer = IspModelingUtils.getBgpPeerOnIsp(bgpActivePeerConfig);
    assertThat(reversedPeer.getPeerAddress(), equalTo(Ip.parse("2.2.2.2")));
    assertThat(reversedPeer.getLocalIp(), equalTo(Ip.parse("1.1.1.1")));
    assertThat(reversedPeer, allOf(hasLocalAs(1L), hasRemoteAs(2L)));
  }

  @Test
  public void testIsValidBgpPeer() {
    Set<Ip> validLocalIps = ImmutableSet.of(Ip.parse("3.3.3.3"));
    BgpActivePeerConfig invalidPeer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpActivePeerConfig validPeer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("3.3.3.3"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    assertFalse(
        IspModelingUtils.isValidBgpPeerConfig(
            invalidPeer, validLocalIps, ImmutableSet.of(), ALL_AS_NUMBERS));
    assertTrue(
        IspModelingUtils.isValidBgpPeerConfig(
            validPeer, validLocalIps, ImmutableSet.of(), ALL_AS_NUMBERS));
  }

  @Test
  public void testGetIspConfigurationNode() {
    ConcreteInterfaceAddress interfaceAddress =
        ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 30);
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    IspInfo ispInfo = new IspInfo(ImmutableList.of(interfaceAddress), ImmutableList.of(peer));

    Configuration ispConfiguration =
        IspModelingUtils.getIspConfigurationNode(
            2L,
            ispInfo,
            ImmutableMap.of(),
            new NetworkFactory(),
            new BatfishLogger("output", false));

    assertThat(
        ispConfiguration,
        allOf(
            hasHostname("isp_2"),
            hasDeviceType(equalTo(DeviceType.ISP)),
            hasInterface(
                "~Interface_0~", hasAllAddresses(equalTo(ImmutableSet.of(interfaceAddress)))),
            hasVrf(
                DEFAULT_VRF_NAME,
                hasBgpProcess(
                    allOf(
                        hasMultipathEbgp(true),
                        hasActiveNeighbor(
                            Prefix.parse("1.1.1.1/32"),
                            allOf(hasRemoteAs(1L), hasLocalAs(2L))))))));

    assertThat(
        ispConfiguration
            .getVrfs()
            .get(DEFAULT_VRF_NAME)
            .getBgpProcess()
            .getActiveNeighbors()
            .values()
            .iterator()
            .next(),
        equalTo(peer));
    assertThat(ispConfiguration.getRoutingPolicies(), hasKey(EXPORT_POLICY_ON_ISP));
  }

  @Test
  public void testGetIspConfigurationNodeInvalid() {
    ConcreteInterfaceAddress interfaceAddress =
        ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 30);
    ConcreteInterfaceAddress interfaceAddress2 =
        ConcreteInterfaceAddress.create(Ip.parse("3.3.3.3"), 30);
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    IspInfo ispInfo =
        new IspInfo(ImmutableList.of(interfaceAddress, interfaceAddress2), ImmutableList.of(peer));
    BatfishLogger logger = new BatfishLogger("debug", false);
    Configuration ispConfiguration =
        IspModelingUtils.getIspConfigurationNode(
            2L, ispInfo, ImmutableMap.of(), new NetworkFactory(), logger);

    assertThat(ispConfiguration, nullValue());

    assertThat(logger.getHistory(), hasSize(1));
    assertThat(
        logger.getHistory().toString(300), equalTo("ISP information for ASN '2' is not correct"));
  }

  @Test
  public void testPopulateIspInfos() {
    NetworkFactory nf = new NetworkFactory();

    Configuration.Builder cb = nf.configurationBuilder();
    Configuration configuration = cb.setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(configuration).build();
    nf.vrfBuilder().setName("emptyVRF").setOwner(configuration).build();
    nf.interfaceBuilder()
        .setName("interface")
        .setOwner(configuration)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 24))
        .build();
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    bgpProcess.getActiveNeighbors().put(Prefix.parse("1.1.1.1/32"), peer);
    configuration.getDefaultVrf().setBgpProcess(bgpProcess);

    Map<Long, IspInfo> inputMap = Maps.newHashMap();
    IspModelingUtils.populateIspInfos(
        configuration,
        ImmutableSet.of("interface"),
        ImmutableList.of(),
        ImmutableList.of(),
        inputMap,
        new Warnings());

    assertThat(inputMap, hasKey(1L));

    IspInfo ispInfo = inputMap.get(1L);

    BgpActivePeerConfig reversedPeer =
        BgpActivePeerConfig.builder()
            .setLocalIp(Ip.parse("1.1.1.1"))
            .setLocalAs(1L)
            .setPeerAddress(Ip.parse("2.2.2.2"))
            .setRemoteAs(2L)
            .setIpv4UnicastAddressFamily(
                Ipv4UnicastAddressFamily.builder().setExportPolicy(EXPORT_POLICY_ON_ISP).build())
            .build();
    assertThat(ispInfo.getBgpActivePeerConfigs(), equalTo(ImmutableList.of(reversedPeer)));
    assertThat(
        ispInfo.getInterfaceAddresses(),
        equalTo(ImmutableList.of(ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 24))));
  }

  @Test
  public void testGetAsnOfIspNode() {
    NetworkFactory nf = new NetworkFactory();
    Configuration.Builder cb = nf.configurationBuilder();
    Configuration ispConfiguration =
        cb.setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(ispConfiguration).build();
    nf.interfaceBuilder()
        .setName("interface")
        .setOwner(ispConfiguration)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 24))
        .build();
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    bgpProcess.getActiveNeighbors().put(Prefix.parse("1.1.1.1/32"), peer);
    ispConfiguration.getDefaultVrf().setBgpProcess(bgpProcess);

    assertThat(IspModelingUtils.getAsnOfIspNode(ispConfiguration), equalTo(2L));
  }

  @Test
  public void testCreateInternetNode() {
    Configuration internet = IspModelingUtils.createInternetNode(new NetworkFactory());
    InterfaceAddress interfaceAddress =
        ConcreteInterfaceAddress.create(
            IspModelingUtils.INTERNET_OUT_ADDRESS, IspModelingUtils.INTERNET_OUT_SUBNET);
    assertThat(
        internet,
        allOf(
            hasHostname(IspModelingUtils.INTERNET_HOST_NAME),
            hasDeviceType(equalTo(DeviceType.INTERNET)),
            hasInterface(
                IspModelingUtils.INTERNET_OUT_INTERFACE,
                hasAllAddresses(equalTo(ImmutableSet.of(interfaceAddress)))),
            hasVrf(
                DEFAULT_VRF_NAME,
                allOf(
                    hasStaticRoutes(
                        equalTo(
                            ImmutableSortedSet.of(
                                StaticRoute.builder()
                                    .setNetwork(Prefix.ZERO)
                                    .setNextHopInterface(IspModelingUtils.INTERNET_OUT_INTERFACE)
                                    .setAdministrativeCost(1)
                                    .build()))),
                    hasBgpProcess(
                        allOf(
                            hasRouterId(IspModelingUtils.INTERNET_OUT_ADDRESS),
                            hasMultipathEbgp(true)))))));

    assertThat(internet.getRoutingPolicies(), hasKey(IspModelingUtils.EXPORT_POLICY_ON_INTERNET));
  }

  @Test
  public void testGetInternetAndIspsCaseInsensitive() {
    NetworkFactory nf = new NetworkFactory();

    Configuration.Builder cb = nf.configurationBuilder();
    Configuration configuration =
        cb.setHostname("conf").setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(configuration).build();
    nf.interfaceBuilder()
        .setName("Interface")
        .setOwner(configuration)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 24))
        .build();
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    bgpProcess.getActiveNeighbors().put(Prefix.parse("1.1.1.1/32"), peer);
    configuration.getDefaultVrf().setBgpProcess(bgpProcess);

    Map<String, Configuration> internetAndIsps =
        IspModelingUtils.getInternetAndIspNodes(
            ImmutableMap.of(configuration.getHostname(), configuration),
            ImmutableList.of(
                new IspConfiguration(
                    ImmutableList.of(
                        new BorderInterfaceInfo(NodeInterfacePair.of("CoNf", "InTeRfAcE"))),
                    IspFilter.ALLOW_ALL)),
            new BatfishLogger("output", false),
            new Warnings());

    // Isp and Internet nodes should be created irrespective of case used in Isp configuration
    assertThat(internetAndIsps, hasKey("isp_1"));
    assertThat(internetAndIsps, hasKey("internet"));
  }

  @Test
  public void testGetInternetAndIspNodes() {
    NetworkFactory nf = new NetworkFactory();

    Configuration.Builder cb = nf.configurationBuilder();
    Configuration configuration =
        cb.setHostname("conf").setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(configuration).build();
    nf.interfaceBuilder()
        .setName("interface")
        .setOwner(configuration)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 24))
        .build();
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    bgpProcess.getActiveNeighbors().put(Prefix.parse("1.1.1.1/32"), peer);
    configuration.getDefaultVrf().setBgpProcess(bgpProcess);

    Map<String, Configuration> internetAndIsps =
        IspModelingUtils.getInternetAndIspNodes(
            ImmutableMap.of(configuration.getHostname(), configuration),
            ImmutableList.of(
                new IspConfiguration(
                    ImmutableList.of(
                        new BorderInterfaceInfo(NodeInterfacePair.of("conf", "interface"))),
                    IspFilter.ALLOW_ALL)),
            new BatfishLogger("output", false),
            new Warnings());

    assertThat(internetAndIsps, hasKey(IspModelingUtils.INTERNET_HOST_NAME));
    Configuration internetNode = internetAndIsps.get(IspModelingUtils.INTERNET_HOST_NAME);

    assertThat(
        internetNode,
        allOf(
            hasHostname(IspModelingUtils.INTERNET_HOST_NAME),
            hasInterface(
                "~Interface_1~",
                hasAllAddresses(
                    equalTo(
                        ImmutableSet.of(
                            ConcreteInterfaceAddress.create(Ip.parse("240.1.1.2"), 31))))),
            hasVrf(
                DEFAULT_VRF_NAME,
                hasBgpProcess(
                    hasNeighbors(
                        equalTo(
                            ImmutableMap.of(
                                Prefix.parse("240.1.1.3/32"),
                                BgpActivePeerConfig.builder()
                                    .setPeerAddress(Ip.parse("240.1.1.3"))
                                    .setRemoteAs(1L)
                                    .setLocalIp(Ip.parse("240.1.1.2"))
                                    .setLocalAs(IspModelingUtils.INTERNET_AS)
                                    .setIpv4UnicastAddressFamily(
                                        Ipv4UnicastAddressFamily.builder()
                                            .setExportPolicy(
                                                IspModelingUtils.EXPORT_POLICY_ON_INTERNET)
                                            .build())
                                    .build())))))));

    assertThat(internetAndIsps, hasKey("isp_1"));
    Configuration ispNode = internetAndIsps.get("isp_1");

    ImmutableSet<InterfaceAddress> interfaceAddresses =
        ispNode.getAllInterfaces().values().stream()
            .flatMap(iface -> iface.getAllConcreteAddresses().stream())
            .collect(ImmutableSet.toImmutableSet());
    assertThat(
        interfaceAddresses,
        equalTo(
            ImmutableSet.of(
                ConcreteInterfaceAddress.create(Ip.parse("240.1.1.3"), 31),
                ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 24))));

    assertThat(
        ispNode,
        hasVrf(
            DEFAULT_VRF_NAME,
            hasBgpProcess(
                hasNeighbors(
                    equalTo(
                        ImmutableMap.of(
                            Prefix.parse("2.2.2.2/32"),
                            BgpActivePeerConfig.builder()
                                .setPeerAddress(Ip.parse("2.2.2.2"))
                                .setRemoteAs(2L)
                                .setLocalIp(Ip.parse("1.1.1.1"))
                                .setLocalAs(1L)
                                .setIpv4UnicastAddressFamily(
                                    Ipv4UnicastAddressFamily.builder()
                                        .setExportPolicy(EXPORT_POLICY_ON_ISP)
                                        .build())
                                .build(),
                            Prefix.parse("240.1.1.2/32"),
                            BgpActivePeerConfig.builder()
                                .setPeerAddress(Ip.parse("240.1.1.2"))
                                .setRemoteAs(IspModelingUtils.INTERNET_AS)
                                .setLocalIp(Ip.parse("240.1.1.3"))
                                .setLocalAs(1L)
                                .setIpv4UnicastAddressFamily(
                                    Ipv4UnicastAddressFamily.builder()
                                        .setExportPolicy(EXPORT_POLICY_ON_ISP)
                                        .build())
                                .build()))))));
  }

  @Test
  public void testGetRoutingPolicyAdvertizeStatic() {
    NetworkFactory nf = new NetworkFactory();
    Configuration internet =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("fakeInternet")
            .build();
    RoutingPolicy internetRoutingPolicy =
        IspModelingUtils.installRoutingPolicyAdvertiseStatic(
            IspModelingUtils.EXPORT_POLICY_ON_INTERNET,
            internet,
            new PrefixSpace(PrefixRange.fromPrefix(Prefix.ZERO)),
            nf);

    PrefixSpace prefixSpace = new PrefixSpace();
    prefixSpace.addPrefix(Prefix.ZERO);
    RoutingPolicy expectedRoutingPolicy =
        nf.routingPolicyBuilder()
            .setName(IspModelingUtils.EXPORT_POLICY_ON_INTERNET)
            .setOwner(internet)
            .setStatements(
                Collections.singletonList(
                    new If(
                        new Conjunction(
                            ImmutableList.of(
                                new MatchProtocol(RoutingProtocol.STATIC),
                                new MatchPrefixSet(
                                    DestinationNetwork.instance(),
                                    new ExplicitPrefixSet(prefixSpace)))),
                        ImmutableList.of(
                            new SetOrigin(new LiteralOrigin(OriginType.INCOMPLETE, null)),
                            Statements.ExitAccept.toStaticStatement()))))
            .build();
    assertThat(internetRoutingPolicy, equalTo(expectedRoutingPolicy));
  }

  @Test
  public void testGetRoutingPolicyForIsp() {
    NetworkFactory nf = new NetworkFactory();
    Configuration isp =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("fakeIsp")
            .build();
    RoutingPolicy ispRoutingPolicy = IspModelingUtils.getRoutingPolicyForIsp(isp, nf);

    RoutingPolicy expectedRoutingPolicy =
        nf.routingPolicyBuilder()
            .setName(EXPORT_POLICY_ON_ISP)
            .setOwner(isp)
            .setStatements(
                Collections.singletonList(
                    new If(
                        new MatchProtocol(RoutingProtocol.BGP),
                        ImmutableList.of(Statements.ReturnTrue.toStaticStatement()))))
            .build();
    assertThat(ispRoutingPolicy, equalTo(expectedRoutingPolicy));
  }

  @Test
  public void testInterfaceNamesIsp() {
    NetworkFactory nf = new NetworkFactory();
    BgpProcess.Builder pb =
        nf.bgpProcessBuilder().setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS);

    Configuration.Builder cb = nf.configurationBuilder();
    Configuration configuration1 =
        cb.setHostname("conf1").setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(configuration1).build();
    nf.interfaceBuilder()
        .setName("interface1")
        .setOwner(configuration1)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 24))
        .build();
    Vrf vrfConf1 = nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(configuration1).build();
    BgpProcess bgpProcess1 = pb.setRouterId(Ip.parse("1.1.1.1")).setVrf(vrfConf1).build();
    BgpActivePeerConfig.builder()
        .setBgpProcess(bgpProcess1)
        .setPeerAddress(Ip.parse("1.1.1.2"))
        .setRemoteAs(1234L)
        .setLocalIp(Ip.parse("1.1.1.1"))
        .setLocalAs(1L)
        .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
        .build();

    Configuration configuration2 =
        cb.setHostname("conf2").setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(configuration2).build();
    nf.interfaceBuilder()
        .setName("interface2")
        .setOwner(configuration2)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 24))
        .build();
    Vrf vrfConf2 = nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(configuration2).build();
    BgpProcess bgpProcess2 = pb.setVrf(vrfConf2).setRouterId(Ip.parse("2.2.2.2")).build();
    BgpActivePeerConfig.builder()
        .setBgpProcess(bgpProcess2)
        .setPeerAddress(Ip.parse("2.2.2.3"))
        .setRemoteAs(1234L)
        .setLocalIp(Ip.parse("2.2.2.2"))
        .setLocalAs(1L)
        .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
        .build();

    Map<String, Configuration> internetAndIsps =
        IspModelingUtils.getInternetAndIspNodes(
            ImmutableMap.of(
                configuration1.getHostname(),
                configuration1,
                configuration2.getHostname(),
                configuration2),
            ImmutableList.of(
                new IspConfiguration(
                    ImmutableList.of(
                        new BorderInterfaceInfo(NodeInterfacePair.of("conf1", "interface1")),
                        new BorderInterfaceInfo(NodeInterfacePair.of("conf2", "interface2"))),
                    IspFilter.ALLOW_ALL)),
            new BatfishLogger("output", false),
            new Warnings());

    assertThat(internetAndIsps, hasKey("isp_1234"));

    Configuration isp = internetAndIsps.get("isp_1234");
    // two interfaces for peering with the two configurations and one interface for peering with
    // internet
    assertThat(isp.getAllInterfaces().entrySet(), hasSize(3));
    assertThat(
        isp.getAllInterfaces().keySet(),
        equalTo(ImmutableSet.of("~Interface_0~", "~Interface_1~", "~Interface_3~")));
  }

  @Test
  public void testNoIsps() {
    NetworkFactory nf = new NetworkFactory();

    Configuration.Builder cb = nf.configurationBuilder();
    Configuration configuration1 =
        cb.setHostname("conf1").setConfigurationFormat(ConfigurationFormat.CISCO_IOS).build();
    nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(configuration1).build();
    nf.interfaceBuilder()
        .setName("interface1")
        .setOwner(configuration1)
        .setAddress(ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 24))
        .build();
    Vrf vrfConf1 = nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(configuration1).build();
    BgpProcess bgpProcess1 =
        nf.bgpProcessBuilder()
            .setRouterId(Ip.parse("1.1.1.1"))
            .setVrf(vrfConf1)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    BgpActivePeerConfig.builder()
        .setBgpProcess(bgpProcess1)
        .setPeerAddress(Ip.parse("1.1.1.2"))
        .setRemoteAs(1234L)
        .setLocalIp(Ip.parse("1.1.1.1"))
        .setLocalAs(1L)
        .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
        .build();

    // passing non-existent border interfaces
    Map<String, Configuration> internetAndIsps =
        IspModelingUtils.getInternetAndIspNodes(
            ImmutableMap.of(configuration1.getHostname(), configuration1),
            ImmutableList.of(
                new IspConfiguration(
                    ImmutableList.of(
                        new BorderInterfaceInfo(NodeInterfacePair.of("conf2", "interface2")),
                        new BorderInterfaceInfo(NodeInterfacePair.of("conf2", "interface2"))),
                    IspFilter.ALLOW_ALL)),
            new BatfishLogger("output", false),
            new Warnings());

    // no ISPs and no Internet
    assertThat(internetAndIsps, anEmptyMap());
  }

  private static Configuration createBgpNode(
      NetworkFactory nf, String hostname, String bgpIfaceName, Ip bgpInterfaceIp) {
    Configuration c =
        nf.configurationBuilder()
            .setHostname(hostname)
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .build();
    Vrf vrf = nf.vrfBuilder().setName(DEFAULT_VRF_NAME).setOwner(c).build();
    nf.interfaceBuilder()
        .setName(bgpIfaceName)
        .setOwner(c)
        .setAddress(ConcreteInterfaceAddress.create(bgpInterfaceIp, 24))
        .build();
    nf.bgpProcessBuilder()
        .setRouterId(bgpInterfaceIp)
        .setVrf(vrf)
        .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
        .build();
    return c;
  }

  private static BgpActivePeerConfig addBgpPeer(
      Configuration c, Ip remoteIp, long remoteAsn, Ip localIp) {
    return BgpActivePeerConfig.builder()
        .setBgpProcess(c.getDefaultVrf().getBgpProcess())
        .setPeerAddress(remoteIp)
        .setRemoteAs(remoteAsn)
        .setLocalIp(localIp)
        .setLocalAs(1L)
        .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
        .build();
  }

  /** Test that combining ISP configs works when two configs have an ASN in common */
  @Test
  public void testCombineIspConfigurationsCommonAsn() {
    NetworkFactory nf = new NetworkFactory();

    long remoteAsn = 1234L;
    String bgpIfaceName = "iface";

    Ip localBgpIp1 = Ip.parse("1.1.1.1");
    Ip remoteBgpIp1 = Ip.parse("1.1.1.2");
    Configuration c1 = createBgpNode(nf, "c1", bgpIfaceName, localBgpIp1);
    addBgpPeer(c1, remoteBgpIp1, remoteAsn, localBgpIp1);

    Ip localBgpIp2 = Ip.parse("2.1.1.1");
    Ip remoteBgpIp2 = Ip.parse("2.1.1.2");
    Configuration c2 = createBgpNode(nf, "c2", bgpIfaceName, localBgpIp2);
    addBgpPeer(c2, remoteBgpIp2, remoteAsn, localBgpIp2);

    Map<Long, IspInfo> combinedMap =
        IspModelingUtils.combineIspConfigurations(
            ImmutableMap.of(c1.getHostname(), c1, c2.getHostname(), c2),
            ImmutableList.of(
                new IspConfiguration(
                    ImmutableList.of(
                        new BorderInterfaceInfo(
                            NodeInterfacePair.of(c1.getHostname(), bgpIfaceName))),
                    IspFilter.ALLOW_ALL),
                new IspConfiguration(
                    ImmutableList.of(
                        new BorderInterfaceInfo(
                            NodeInterfacePair.of(c2.getHostname(), bgpIfaceName))),
                    IspFilter.ALLOW_ALL)),
            new Warnings());

    assertThat(
        combinedMap,
        equalTo(
            ImmutableMap.of(
                remoteAsn,
                new IspInfo(
                    ImmutableList.of(
                        ConcreteInterfaceAddress.create(remoteBgpIp1, 24),
                        ConcreteInterfaceAddress.create(remoteBgpIp2, 24)),
                    ImmutableList.of(
                        BgpActivePeerConfig.builder()
                            .setLocalAs(remoteAsn)
                            .setLocalIp(remoteBgpIp1)
                            .setPeerAddress(localBgpIp1)
                            .setRemoteAs(1L)
                            .setIpv4UnicastAddressFamily(
                                Ipv4UnicastAddressFamily.builder()
                                    .setExportPolicy(EXPORT_POLICY_ON_ISP)
                                    .build())
                            .build(),
                        BgpActivePeerConfig.builder()
                            .setLocalAs(remoteAsn)
                            .setLocalIp(remoteBgpIp2)
                            .setPeerAddress(localBgpIp2)
                            .setRemoteAs(1L)
                            .setIpv4UnicastAddressFamily(
                                Ipv4UnicastAddressFamily.builder()
                                    .setExportPolicy(EXPORT_POLICY_ON_ISP)
                                    .build())
                            .build())))));
  }
}

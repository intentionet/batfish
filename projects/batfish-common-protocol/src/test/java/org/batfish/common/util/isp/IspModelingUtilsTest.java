package org.batfish.common.util.isp;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.batfish.common.Warnings.TAG_RED_FLAG;
import static org.batfish.common.util.isp.IspModelingUtils.EXPORT_POLICY_ON_ISP_TO_CUSTOMERS;
import static org.batfish.common.util.isp.IspModelingUtils.EXPORT_POLICY_ON_ISP_TO_INTERNET;
import static org.batfish.common.util.isp.IspModelingUtils.HIGH_ADMINISTRATIVE_COST;
import static org.batfish.common.util.isp.IspModelingUtils.INTERNET_HOST_NAME;
import static org.batfish.common.util.isp.IspModelingUtils.INTERNET_NULL_ROUTED_PREFIXES;
import static org.batfish.common.util.isp.IspModelingUtils.INTERNET_OUT_INTERFACE;
import static org.batfish.common.util.isp.IspModelingUtils.INTERNET_OUT_INTERFACE_LINK_LOCATION_INFO;
import static org.batfish.common.util.isp.IspModelingUtils.ISP_TO_INTERNET_INTERFACE_NAME;
import static org.batfish.common.util.isp.IspModelingUtils.LINK_LOCAL_ADDRESS;
import static org.batfish.common.util.isp.IspModelingUtils.LINK_LOCAL_IP;
import static org.batfish.common.util.isp.IspModelingUtils.addBgpPeerToIsp;
import static org.batfish.common.util.isp.IspModelingUtils.createInternetNode;
import static org.batfish.common.util.isp.IspModelingUtils.createIspNode;
import static org.batfish.common.util.isp.IspModelingUtils.getAdvertiseBgpStatement;
import static org.batfish.common.util.isp.IspModelingUtils.getAdvertiseStaticStatement;
import static org.batfish.common.util.isp.IspModelingUtils.getDefaultIspNodeName;
import static org.batfish.common.util.isp.IspModelingUtils.installRoutingPolicyForIspToCustomers;
import static org.batfish.common.util.isp.IspModelingUtils.installRoutingPolicyForIspToInternet;
import static org.batfish.common.util.isp.IspModelingUtils.ispNameConflicts;
import static org.batfish.datamodel.BgpPeerConfig.ALL_AS_NUMBERS;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.datamodel.matchers.BgpProcessMatchers.hasInterfaceNeighbors;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
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
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warning;
import org.batfish.common.Warnings;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.topology.Layer1Node;
import org.batfish.common.util.isp.IspModel.Remote;
import org.batfish.common.util.isp.IspModelingUtils.ModeledNodes;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
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
import org.batfish.datamodel.isp_configuration.IspAnnouncement;
import org.batfish.datamodel.isp_configuration.IspConfiguration;
import org.batfish.datamodel.isp_configuration.IspFilter;
import org.batfish.datamodel.isp_configuration.IspNodeInfo;
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
import org.batfish.specifier.InterfaceLinkLocation;
import org.junit.Test;

/** Tests for {@link IspModelingUtils} */
public class IspModelingUtilsTest {

  private static long _localAsn = 2L;

  private static long _remoteAsn = 1L;

  /** Makes a Configuration object with one BGP peer */
  private static Configuration configurationWithOnePeer() {
    return configurationWithOnePeer(false);
  }

  private static Configuration configurationWithOnePeer(boolean bgpUnnumbered) {
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
    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    configuration.getDefaultVrf().setBgpProcess(bgpProcess);
    BgpPeerConfig.Builder<?, ?> peer =
        bgpUnnumbered
            ? BgpUnnumberedPeerConfig.builder().setPeerInterface("interface")
            : BgpActivePeerConfig.builder().setPeerAddress(Ip.parse("1.1.1.1"));
    peer.setRemoteAs(_remoteAsn)
        .setLocalIp(Ip.parse("2.2.2.2"))
        .setLocalAs(_localAsn)
        .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
        .setBgpProcess(bgpProcess)
        .build();
    return configuration;
  }

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

    IspModelingUtils.populateIspModels(
        nf.configurationBuilder().setHostname("conf").build(),
        ImmutableSet.of("init"),
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableList.of(),
        Maps.newHashMap(),
        warnings);

    assertThat(
        warnings.getRedFlagWarnings(),
        equalTo(
            ImmutableList.of(
                new Warning(
                    "ISP Modeling: Cannot find interface init on node conf", TAG_RED_FLAG))));
  }

  @Test
  public void testAddBgpPeerToIsp() {
    Ip ispIp = Ip.parse("1.1.1.1");
    Ip remoteIp = Ip.parse("2.2.2.2");
    BgpActivePeerConfig remotePeerConfig =
        BgpActivePeerConfig.builder()
            .setPeerAddress(ispIp)
            .setRemoteAs(1L)
            .setLocalIp(remoteIp)
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    addBgpPeerToIsp(remotePeerConfig, "iface", bgpProcess);
    BgpActivePeerConfig reversedPeer = getOnlyElement(bgpProcess.getActiveNeighbors().values());

    assertThat(
        reversedPeer,
        equalTo(
            BgpActivePeerConfig.builder()
                .setLocalIp(ispIp)
                .setPeerAddress(remoteIp)
                .setLocalAs(1L)
                .setRemoteAs(2L)
                .setIpv4UnicastAddressFamily(
                    Ipv4UnicastAddressFamily.builder()
                        .setExportPolicy(EXPORT_POLICY_ON_ISP_TO_CUSTOMERS)
                        .build())
                .build()));
  }

  @Test
  public void testAddBgpPeerToIsp_preferConfederationAs() {
    Ip ispIp = Ip.parse("1.1.1.1");
    Ip remoteIp = Ip.parse("2.2.2.2");
    BgpActivePeerConfig remotePeerConfig =
        BgpActivePeerConfig.builder()
            .setPeerAddress(ispIp)
            .setRemoteAs(1L)
            .setLocalIp(remoteIp)
            .setLocalAs(2L)
            .setConfederation(1000L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    addBgpPeerToIsp(remotePeerConfig, "iface", bgpProcess);
    BgpActivePeerConfig reversedPeer = getOnlyElement(bgpProcess.getActiveNeighbors().values());

    assertThat(
        reversedPeer,
        equalTo(
            BgpActivePeerConfig.builder()
                .setLocalIp(ispIp)
                .setPeerAddress(remoteIp)
                .setLocalAs(1L)
                .setRemoteAs(1000L)
                .setIpv4UnicastAddressFamily(
                    Ipv4UnicastAddressFamily.builder()
                        .setExportPolicy(EXPORT_POLICY_ON_ISP_TO_CUSTOMERS)
                        .build())
                .build()));
  }

  @Test
  public void testAddBgpPeerToIsp_Unnumbered() {
    BgpUnnumberedPeerConfig remotePeerConfig =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface("interface")
            .setRemoteAs(1L)
            .setLocalIp(LINK_LOCAL_IP)
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();

    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    addBgpPeerToIsp(remotePeerConfig, "iface", bgpProcess);
    BgpUnnumberedPeerConfig reversedPeer =
        getOnlyElement(bgpProcess.getInterfaceNeighbors().values());

    assertThat(
        reversedPeer,
        equalTo(
            BgpUnnumberedPeerConfig.builder()
                .setLocalIp(LINK_LOCAL_IP)
                .setPeerInterface("iface")
                .setLocalAs(1L)
                .setRemoteAs(2L)
                .setIpv4UnicastAddressFamily(
                    Ipv4UnicastAddressFamily.builder()
                        .setExportPolicy(EXPORT_POLICY_ON_ISP_TO_CUSTOMERS)
                        .build())
                .build()));
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
  public void testIsValidBgpPeerInterfaceNeighbor() {
    Set<Ip> validLocalIps = ImmutableSet.of(LINK_LOCAL_IP);
    BgpUnnumberedPeerConfig invalidPeer =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface("iface")
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    BgpUnnumberedPeerConfig validPeer =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface("iface")
            .setRemoteAs(1L)
            .setLocalIp(LINK_LOCAL_IP)
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
  public void testCreateIspConfigurationNode() {
    Ip ispIp = Ip.parse("2.2.2.2");
    Ip remoteIp = Ip.parse("1.1.1.1");
    ConcreteInterfaceAddress ispIfaceAddress = ConcreteInterfaceAddress.create(ispIp, 30);
    BgpActivePeerConfig remotePeerConfig =
        BgpActivePeerConfig.builder()
            .setPeerAddress(ispIp)
            .setRemoteAs(1L)
            .setLocalIp(remoteIp)
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    long asn = 2L;
    String ispName = getDefaultIspNodeName(asn);
    IspModel ispModel =
        IspModel.builder()
            .setAsn(asn)
            .setName(ispName)
            .setRemotes(new Remote("testNode", "testIface", ispIfaceAddress, remotePeerConfig))
            .build();

    ModeledNodes modeledNodes = new ModeledNodes();
    createIspNode(modeledNodes, ispModel, new NetworkFactory(), new BatfishLogger("output", false));
    Configuration ispConfiguration = modeledNodes.getConfigurations().get(ispName);

    assertThat(
        ispConfiguration,
        allOf(
            hasHostname(ispName),
            hasDeviceType(equalTo(DeviceType.ISP)),
            hasInterface(
                "~Interface_0~", hasAllAddresses(equalTo(ImmutableSet.of(ispIfaceAddress)))),
            hasVrf(DEFAULT_VRF_NAME, hasBgpProcess(allOf(hasMultipathEbgp(true))))));

    // compute the reverse config
    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    addBgpPeerToIsp(remotePeerConfig, "iface", bgpProcess);
    BgpActivePeerConfig expectedIspPeerConfig =
        getOnlyElement(bgpProcess.getActiveNeighbors().values());

    assertThat(
        getOnlyElement(
            ispConfiguration
                .getVrfs()
                .get(DEFAULT_VRF_NAME)
                .getBgpProcess()
                .getActiveNeighbors()
                .values()),
        equalTo(expectedIspPeerConfig));

    assertThat(ispConfiguration.getRoutingPolicies(), hasKey(EXPORT_POLICY_ON_ISP_TO_CUSTOMERS));

    assertThat(
        modeledNodes.getLayer1Edges(),
        equalTo(
            ImmutableSet.of(
                new Layer1Edge("testNode", "testIface", ispName, "~Interface_0~"),
                new Layer1Edge(ispName, "~Interface_0~", "testNode", "testIface"))));
  }

  @Test
  public void testCreateIspConfigurationNode_unnumbered() {
    BgpUnnumberedPeerConfig remotePeerConfig =
        BgpUnnumberedPeerConfig.builder()
            .setPeerInterface("remoteIface")
            .setRemoteAs(1L)
            .setLocalIp(LINK_LOCAL_IP)
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    long asn = 2L;
    String ispName = getDefaultIspNodeName(asn);
    IspModel ispModel =
        IspModel.builder()
            .setAsn(asn)
            .setName(ispName)
            .setRemotes(new Remote("testNode", "remoteIface", LINK_LOCAL_ADDRESS, remotePeerConfig))
            .build();

    ModeledNodes modeledNodes = new ModeledNodes();
    createIspNode(modeledNodes, ispModel, new NetworkFactory(), new BatfishLogger("output", false));
    Configuration ispConfiguration = modeledNodes.getConfigurations().get(ispName);

    assertThat(
        ispConfiguration,
        allOf(
            hasHostname(ispName),
            hasDeviceType(equalTo(DeviceType.ISP)),
            hasInterface(
                "~Interface_0~", hasAllAddresses(equalTo(ImmutableSet.of(LINK_LOCAL_ADDRESS)))),
            hasVrf(DEFAULT_VRF_NAME, hasBgpProcess(hasMultipathEbgp(true)))));

    // compute the reverse config
    BgpProcess bgpProcess = new BgpProcess(Ip.ZERO, ConfigurationFormat.CISCO_IOS);
    addBgpPeerToIsp(remotePeerConfig, "~Interface_0~", bgpProcess);
    BgpUnnumberedPeerConfig expectedIspPeerConfig =
        getOnlyElement(bgpProcess.getInterfaceNeighbors().values());

    assertThat(
        getOnlyElement(
            ispConfiguration
                .getVrfs()
                .get(DEFAULT_VRF_NAME)
                .getBgpProcess()
                .getInterfaceNeighbors()
                .values()),
        equalTo(expectedIspPeerConfig));

    assertThat(ispConfiguration.getRoutingPolicies(), hasKey(EXPORT_POLICY_ON_ISP_TO_CUSTOMERS));

    assertThat(
        modeledNodes.getLayer1Edges(),
        equalTo(
            ImmutableSet.of(
                new Layer1Edge("testNode", "remoteIface", ispName, "~Interface_0~"),
                new Layer1Edge(ispName, "~Interface_0~", "testNode", "remoteIface"))));
  }

  @Test
  public void testGetIspConfigurationNodeInvalid() {
    long asn = 2L;
    IspModel ispInfo = IspModel.builder().setAsn(asn).setName(getDefaultIspNodeName(asn)).build();
    BatfishLogger logger = new BatfishLogger("debug", false);
    ModeledNodes modeledNodes = new ModeledNodes();
    createIspNode(modeledNodes, ispInfo, new NetworkFactory(), logger);
    Configuration ispConfiguration = modeledNodes.getConfigurations().get(ispInfo.getName());

    assertThat(ispConfiguration, nullValue());

    assertThat(logger.getHistory(), hasSize(1));
    assertThat(
        logger.getHistory().toString(300), equalTo("ISP information for ASN '2' is not correct"));
  }

  /** Test that null static routes are created for additional announcements to the Internet */
  @Test
  public void testGetIspConfigurationNodeAdditionalAnnouncements() {
    BgpActivePeerConfig peer =
        BgpActivePeerConfig.builder()
            .setPeerAddress(Ip.parse("1.1.1.1"))
            .setRemoteAs(1L)
            .setLocalIp(Ip.parse("2.2.2.2"))
            .setLocalAs(2L)
            .setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.builder().build())
            .build();
    Set<Prefix> additionalPrefixes =
        ImmutableSet.of(Prefix.parse("1.1.1.1/32"), Prefix.parse("2.2.2.2/32"));
    IspModel ispInfo =
        IspModel.builder()
            .setAsn(2L)
            .setName(getDefaultIspNodeName(2L))
            .setRemotes(
                new Remote(
                    "test", "test", ConcreteInterfaceAddress.create(Ip.parse("2.2.2.2"), 30), peer))
            .setAdditionalPrefixesToInternet(additionalPrefixes)
            .build();
    ModeledNodes modeledNodes = new ModeledNodes();
    createIspNode(modeledNodes, ispInfo, new NetworkFactory(), new BatfishLogger("debug", false));
    Configuration ispConfiguration = modeledNodes.getConfigurations().get(ispInfo.getName());

    assertThat(
        ispConfiguration.getDefaultVrf().getStaticRoutes(),
        equalTo(
            ImmutableSortedSet.copyOf(
                ispInfo.getAdditionalPrefixesToInternet().stream()
                    .map(
                        prefix ->
                            StaticRoute.builder()
                                .setNetwork(prefix)
                                .setNextHopInterface(NULL_INTERFACE_NAME)
                                .setAdministrativeCost(HIGH_ADMINISTRATIVE_COST)
                                .build())
                    .collect(ImmutableSet.toImmutableSet()))));
  }

  @Test
  public void testPopulateIspModels() {
    Map<Long, IspModel> inputMap = Maps.newHashMap();
    Configuration remote = configurationWithOnePeer();
    IspModelingUtils.populateIspModels(
        remote,
        ImmutableSet.of("interface"),
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableList.of(),
        inputMap,
        new Warnings());

    assertThat(inputMap, hasKey(_remoteAsn));

    IspModel ispInfo = inputMap.get(_remoteAsn);

    assertThat(
        ispInfo,
        equalTo(
            IspModel.builder()
                .setAsn(_remoteAsn)
                .setName(getDefaultIspNodeName(_remoteAsn))
                .setRemotes(
                    new Remote(
                        remote.getHostname(),
                        getOnlyElement(remote.getAllInterfaces().keySet()),
                        ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 24),
                        getOnlyElement(
                            remote.getDefaultVrf().getBgpProcess().getActiveNeighbors().values())))
                .build()));
    assertThat(ispInfo.getName(), equalTo(getDefaultIspNodeName(_remoteAsn)));
  }

  @Test
  public void testPopulateIspModelsBgpUnnumbered() {
    Map<Long, IspModel> inputMap = Maps.newHashMap();
    Configuration remote = configurationWithOnePeer(true);
    IspModelingUtils.populateIspModels(
        remote,
        ImmutableSet.of("interface"),
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableList.of(),
        inputMap,
        new Warnings());

    assertThat(inputMap, hasKey(_remoteAsn));

    IspModel ispInfo = inputMap.get(_remoteAsn);

    assertThat(
        ispInfo,
        equalTo(
            IspModel.builder()
                .setAsn(_remoteAsn)
                .setName(getDefaultIspNodeName(_remoteAsn))
                .setRemotes(
                    new Remote(
                        remote.getHostname(),
                        getOnlyElement(remote.getAllInterfaces().keySet()),
                        LINK_LOCAL_ADDRESS,
                        getOnlyElement(
                            remote
                                .getDefaultVrf()
                                .getBgpProcess()
                                .getInterfaceNeighbors()
                                .values())))
                .build()));
    assertThat(ispInfo.getName(), equalTo(getDefaultIspNodeName(_remoteAsn)));
  }

  @Test
  public void testPopulateIspInfosCustomIspName() {
    Map<Long, IspModel> inputMap = Maps.newHashMap();
    IspModelingUtils.populateIspModels(
        configurationWithOnePeer(),
        ImmutableSet.of("interface"),
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableList.of(new IspNodeInfo(_remoteAsn, "myisp")),
        inputMap,
        new Warnings());

    assertThat(inputMap, hasKey(_remoteAsn));

    IspModel ispInfo = inputMap.get(_remoteAsn);

    assertThat(ispInfo.getName(), equalTo("myisp"));
  }

  @Test
  public void testPopulateIspInfosMergeAdditionalPrefixes() {
    Map<Long, IspModel> inputMap = Maps.newHashMap();
    IspModelingUtils.populateIspModels(
        configurationWithOnePeer(),
        ImmutableSet.of("interface"),
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableList.of(
            new IspNodeInfo(
                _remoteAsn,
                "myisp",
                ImmutableList.of(
                    new IspAnnouncement(Prefix.parse("1.1.1.1/32")),
                    new IspAnnouncement(Prefix.parse("2.2.2.2/32")))),
            new IspNodeInfo(
                _remoteAsn,
                "myisp",
                ImmutableList.of(
                    new IspAnnouncement(Prefix.parse("3.3.3.3/32")),
                    new IspAnnouncement(Prefix.parse("2.2.2.2/32"))))),
        inputMap,
        new Warnings());

    assertThat(
        inputMap.get(_remoteAsn).getAdditionalPrefixesToInternet(),
        equalTo(
            ImmutableSet.of(
                Prefix.parse("1.1.1.1/32"),
                Prefix.parse("2.2.2.2/32"),
                Prefix.parse("3.3.3.3/32"))));
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
    ModeledNodes modeledNodes = new ModeledNodes();
    createInternetNode(modeledNodes);
    Configuration internet = modeledNodes.getConfigurations().get(INTERNET_HOST_NAME);
    InterfaceAddress interfaceAddress =
        ConcreteInterfaceAddress.create(
            IspModelingUtils.INTERNET_OUT_ADDRESS,
            IspModelingUtils.INTERNET_OUT_SUBNET.getPrefixLength());
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
                            new ImmutableSortedSet.Builder<StaticRoute>(Comparator.naturalOrder())
                                .add(
                                    StaticRoute.builder()
                                        .setNetwork(Prefix.ZERO)
                                        .setNextHopInterface(
                                            IspModelingUtils.INTERNET_OUT_INTERFACE)
                                        .setAdministrativeCost(1)
                                        .build())
                                .addAll(
                                    INTERNET_NULL_ROUTED_PREFIXES.stream()
                                        .map(
                                            prefix ->
                                                StaticRoute.builder()
                                                    .setNetwork(prefix)
                                                    .setNextHopInterface(NULL_INTERFACE_NAME)
                                                    .setAdministrativeCost(1)
                                                    .build())
                                        .collect(ImmutableSet.toImmutableSet()))
                                .build())),
                    hasBgpProcess(
                        allOf(
                            hasRouterId(IspModelingUtils.INTERNET_OUT_ADDRESS),
                            hasMultipathEbgp(true)))))));

    assertThat(internet.getRoutingPolicies(), hasKey(IspModelingUtils.EXPORT_POLICY_ON_INTERNET));

    assertThat(
        internet.getLocationInfo(),
        hasEntry(
            new InterfaceLinkLocation(INTERNET_HOST_NAME, INTERNET_OUT_INTERFACE),
            INTERNET_OUT_INTERFACE_LINK_LOCATION_INFO));
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
                new Warnings())
            .getConfigurations();

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

    ModeledNodes modeledNodes =
        IspModelingUtils.getInternetAndIspNodes(
            ImmutableMap.of(configuration.getHostname(), configuration),
            ImmutableList.of(
                new IspConfiguration(
                    ImmutableList.of(
                        new BorderInterfaceInfo(NodeInterfacePair.of("conf", "interface"))),
                    IspFilter.ALLOW_ALL)),
            new BatfishLogger("output", false),
            new Warnings());
    Map<String, Configuration> configurations = modeledNodes.getConfigurations();

    assertThat(configurations, hasKey(IspModelingUtils.INTERNET_HOST_NAME));
    Configuration internetNode = configurations.get(IspModelingUtils.INTERNET_HOST_NAME);

    assertThat(
        internetNode,
        allOf(
            hasHostname(IspModelingUtils.INTERNET_HOST_NAME),
            hasInterface(
                "~Interface_1~", hasAllAddresses(equalTo(ImmutableSet.of(LINK_LOCAL_ADDRESS)))),
            hasVrf(
                DEFAULT_VRF_NAME,
                hasBgpProcess(
                    hasInterfaceNeighbors(
                        equalTo(
                            ImmutableMap.of(
                                "~Interface_1~",
                                BgpUnnumberedPeerConfig.builder()
                                    .setPeerInterface("~Interface_1~")
                                    .setRemoteAs(1L)
                                    .setLocalIp(LINK_LOCAL_IP)
                                    .setLocalAs(IspModelingUtils.INTERNET_AS)
                                    .setIpv4UnicastAddressFamily(
                                        Ipv4UnicastAddressFamily.builder()
                                            .setExportPolicy(
                                                IspModelingUtils.EXPORT_POLICY_ON_INTERNET)
                                            .build())
                                    .build())))))));

    assertThat(configurations, hasKey("isp_1"));
    Configuration ispNode = configurations.get("isp_1");

    ImmutableSet<InterfaceAddress> interfaceAddresses =
        ispNode.getAllInterfaces().values().stream()
            .flatMap(iface -> iface.getAllAddresses().stream())
            .collect(ImmutableSet.toImmutableSet());
    assertThat(
        interfaceAddresses,
        equalTo(
            ImmutableSet.of(
                LINK_LOCAL_ADDRESS, ConcreteInterfaceAddress.create(Ip.parse("1.1.1.1"), 24))));

    assertThat(
        ispNode,
        hasVrf(
            DEFAULT_VRF_NAME,
            hasBgpProcess(
                allOf(
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
                                            .setExportPolicy(EXPORT_POLICY_ON_ISP_TO_CUSTOMERS)
                                            .build())
                                    .build()))),
                    hasInterfaceNeighbors(
                        equalTo(
                            ImmutableMap.of(
                                ISP_TO_INTERNET_INTERFACE_NAME,
                                BgpUnnumberedPeerConfig.builder()
                                    .setPeerInterface(ISP_TO_INTERNET_INTERFACE_NAME)
                                    .setRemoteAs(IspModelingUtils.INTERNET_AS)
                                    .setLocalIp(LINK_LOCAL_IP)
                                    .setLocalAs(1L)
                                    .setIpv4UnicastAddressFamily(
                                        Ipv4UnicastAddressFamily.builder()
                                            .setExportPolicy(EXPORT_POLICY_ON_ISP_TO_INTERNET)
                                            .build())
                                    .build())))))));

    Layer1Node internetLayer1 = new Layer1Node(INTERNET_HOST_NAME, "~Interface_1~");
    Layer1Node ispLayer1Iface0 = new Layer1Node(ispNode.getHostname(), "~Interface_0~");
    Layer1Node ispLayer1Iface2 =
        new Layer1Node(ispNode.getHostname(), ISP_TO_INTERNET_INTERFACE_NAME);
    Layer1Node borderLayer1 = new Layer1Node("conf", "interface");

    assertThat(
        modeledNodes.getLayer1Edges(),
        equalTo(
            ImmutableSet.of(
                new Layer1Edge(borderLayer1, ispLayer1Iface0),
                new Layer1Edge(ispLayer1Iface0, borderLayer1),
                new Layer1Edge(internetLayer1, ispLayer1Iface2),
                new Layer1Edge(ispLayer1Iface2, internetLayer1))));
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
            new PrefixSpace(PrefixRange.fromPrefix(Prefix.ZERO)));

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
  public void testInstallRoutingPolicyForIspToCustomers() {
    NetworkFactory nf = new NetworkFactory();
    Configuration isp =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("fakeIsp")
            .build();
    RoutingPolicy ispRoutingPolicy = installRoutingPolicyForIspToCustomers(isp);

    RoutingPolicy expectedRoutingPolicy =
        nf.routingPolicyBuilder()
            .setName(EXPORT_POLICY_ON_ISP_TO_CUSTOMERS)
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
  public void testInstallRoutingPolicyForIspToInternet() {
    NetworkFactory nf = new NetworkFactory();
    Configuration isp =
        nf.configurationBuilder()
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname("fakeIsp")
            .build();
    PrefixSpace prefixSpace = new PrefixSpace(PrefixRange.fromPrefix(Prefix.parse("1.1.1.1/32")));
    RoutingPolicy expectedRoutingPolicy =
        nf.routingPolicyBuilder()
            .setName(EXPORT_POLICY_ON_ISP_TO_INTERNET)
            .setOwner(isp)
            .setStatements(
                ImmutableList.of(
                    getAdvertiseBgpStatement(), getAdvertiseStaticStatement(prefixSpace)))
            .build();

    assertThat(
        installRoutingPolicyForIspToInternet(isp, prefixSpace), equalTo(expectedRoutingPolicy));
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
                new Warnings())
            .getConfigurations();

    assertThat(internetAndIsps, hasKey("isp_1234"));

    Configuration isp = internetAndIsps.get("isp_1234");
    // two interfaces for peering with the two configurations and one interface for peering with
    // internet
    assertThat(isp.getAllInterfaces().entrySet(), hasSize(3));
    assertThat(
        isp.getAllInterfaces().keySet(),
        equalTo(ImmutableSet.of("~Interface_0~", "~Interface_1~", ISP_TO_INTERNET_INTERFACE_NAME)));
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
                new Warnings())
            .getConfigurations();

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

    Map<Long, IspModel> combinedMap =
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
                IspModel.builder()
                    .setAsn(remoteAsn)
                    .setName(getDefaultIspNodeName(remoteAsn))
                    .setRemotes(
                        new Remote(
                            c1.getHostname(),
                            getOnlyElement(c1.getAllInterfaces().keySet()),
                            ConcreteInterfaceAddress.create(remoteBgpIp1, 24),
                            getOnlyElement(
                                c1.getDefaultVrf().getBgpProcess().getActiveNeighbors().values())),
                        new Remote(
                            c2.getHostname(),
                            getOnlyElement(c2.getAllInterfaces().keySet()),
                            ConcreteInterfaceAddress.create(remoteBgpIp2, 24),
                            getOnlyElement(
                                c2.getDefaultVrf().getBgpProcess().getActiveNeighbors().values())))
                    .build())));
  }

  @Test
  public void testIspNameConflictsGoodCase() {
    Map<Long, IspModel> ispInfoMap =
        ImmutableMap.of(
            1L,
            IspModel.builder().setAsn(1).setName("isp1").build(),
            2L,
            IspModel.builder().setAsn(2).setName("isp2").build());
    Map<String, Configuration> configurations =
        ImmutableMap.of(
            "node",
            new NetworkFactory()
                .configurationBuilder()
                .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
                .build());
    assertTrue(ispNameConflicts(configurations, ispInfoMap).isEmpty());
  }

  @Test
  public void testIspNameConflictsIspConflict() {
    Map<Long, IspModel> ispInfoMap =
        ImmutableMap.of(
            1L,
            IspModel.builder().setAsn(1).setName("isp1").build(),
            2L,
            IspModel.builder().setAsn(2).setName("isp1").build());
    Map<String, Configuration> configurations = ImmutableMap.of();

    String message = getOnlyElement(ispNameConflicts(configurations, ispInfoMap));
    assertThat(message, containsString("ASN 1"));
  }

  @Test
  public void testIspNameConflictsNodeConflict() {
    Map<Long, IspModel> ispInfoMap =
        ImmutableMap.of(
            1L,
            IspModel.builder().setAsn(1).setName("isp1").build(),
            2L,
            IspModel.builder().setAsn(2).setName("isp2").build());
    Map<String, Configuration> configurations =
        ImmutableMap.of(
            "isp1",
            new NetworkFactory()
                .configurationBuilder()
                .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
                .build());

    String message = getOnlyElement(ispNameConflicts(configurations, ispInfoMap));
    assertThat(message, containsString("ASN 1"));
  }
}

package org.batfish.common.util.isp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.naturalOrder;
import static org.batfish.datamodel.BgpPeerConfig.ALL_AS_NUMBERS;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.specifier.Location.interfaceLinkLocation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Streams;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.util.isp.IspModel.Remote;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.DeviceType;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.LinkLocalAddress;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.isp_configuration.BorderInterfaceInfo;
import org.batfish.datamodel.isp_configuration.IspAnnouncement;
import org.batfish.datamodel.isp_configuration.IspConfiguration;
import org.batfish.datamodel.isp_configuration.IspNodeInfo;
import org.batfish.datamodel.isp_configuration.traffic_filtering.IspTrafficFiltering;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.specifier.LocationInfo;

/** Util classes and functions to model ISPs and Internet for a given network */
public final class IspModelingUtils {
  static final Prefix INTERNET_OUT_SUBNET = Prefix.parse("240.254.254.0/30");

  static final LocationInfo INTERNET_OUT_INTERFACE_LINK_LOCATION_INFO =
      new LocationInfo(
          // use as a source
          true,
          // pick any source IP (excluding snapshot owned IPs)
          UniverseIpSpace.INSTANCE,
          // pretend there's a neighbor that responds to ARP, so we get EXITS_NETWORK instead of
          // NEIGHBOR_UNREACHABLE for traffic routed to the internet
          INTERNET_OUT_SUBNET.getLastHostIp().toIpSpace());

  static final String EXPORT_POLICY_ON_INTERNET = "exportPolicyOnInternet";
  static final String EXPORT_POLICY_ON_ISP_TO_CUSTOMERS = "exportPolicyOnIspToCustomers";
  static final String EXPORT_POLICY_ON_ISP_TO_INTERNET = "exportPolicyOnIspToInternet";
  static final long INTERNET_AS = 65537L;
  public static final String INTERNET_HOST_NAME = "internet";
  static final Ip INTERNET_OUT_ADDRESS = INTERNET_OUT_SUBNET.getFirstHostIp();
  public static final String INTERNET_OUT_INTERFACE = "out";
  static final Ip LINK_LOCAL_IP = Ip.parse("169.254.0.1");
  static final LinkLocalAddress LINK_LOCAL_ADDRESS = LinkLocalAddress.of(LINK_LOCAL_IP);
  static final String ISP_TO_INTERNET_INTERFACE_NAME = "To Internet";

  // null routing private address space at the internet prevents "INSUFFICIENT_INFO" for networks
  // that use this space internally
  public static final Set<Prefix> INTERNET_NULL_ROUTED_PREFIXES =
      ImmutableSet.of(
          Prefix.parse("10.0.0.0/8"),
          Prefix.parse("172.16.0.0/12"),
          Prefix.parse("192.168.0.0/16"));

  /** Use this cost to install static routes on ISP nodes for prefixes originated to the Internet */
  static final int HIGH_ADMINISTRATIVE_COST = 32767; // maximum possible

  public static String getDefaultIspNodeName(Long asn) {
    return String.format("%s_%s", "isp", asn);
  }

  public static class ModeledNodes {

    @Nonnull private final Map<String, Configuration> _configurations;

    @Nonnull private final Set<Layer1Edge> _layer1Edgesdges;

    public ModeledNodes() {
      _configurations = new HashMap<>();
      _layer1Edgesdges = new HashSet<>();
    }

    public void addConfiguration(Configuration configuration) {
      _configurations.put(configuration.getHostname(), configuration);
    }

    /** Add both directions of the node/interface pairs as layer 1 edges */
    public void addLayer1Edge(String node1, String node1Iface, String node2, String node2Iface) {
      addLayer1Edge(new Layer1Edge(node1, node1Iface, node2, node2Iface));
      addLayer1Edge(new Layer1Edge(node2, node2Iface, node1, node1Iface));
    }

    public void addLayer1Edge(Layer1Edge edge) {
      _layer1Edgesdges.add(edge);
    }

    @Nonnull
    public Map<String, Configuration> getConfigurations() {
      return ImmutableMap.copyOf(_configurations);
    }

    @Nonnull
    public Set<Layer1Edge> getLayer1Edges() {
      return ImmutableSet.copyOf(_layer1Edgesdges);
    }
  }

  private IspModelingUtils() {}

  /**
   * Creates and returns internet and ISP nodes for a {@link Map} of {@link Configuration}s
   *
   * @param configurations {@link Configuration}s for the given network
   * @param ispConfigurations A list of {@link IspConfiguration} objects to initialize the ISPs
   * @param logger {@link BatfishLogger} to log warnings and errors
   * @param warnings {@link Warnings} containing all the warnings logged during the ISP modeling
   * @return {@link Map} of {@link Configuration}s for the ISPs and Internet
   */
  public static ModeledNodes getInternetAndIspNodes(
      @Nonnull Map<String, Configuration> configurations,
      @Nonnull List<IspConfiguration> ispConfigurations,
      @Nonnull BatfishLogger logger,
      @Nonnull Warnings warnings) {

    NetworkFactory nf = new NetworkFactory();

    Map<Long, IspModel> asnToIspInfos =
        combineIspConfigurations(configurations, ispConfigurations, warnings);

    List<String> conflicts = ispNameConflicts(configurations, asnToIspInfos);

    if (!conflicts.isEmpty()) {
      conflicts.forEach(warnings::redFlag);
      return new ModeledNodes();
    }

    return createInternetAndIspNodes(asnToIspInfos, nf, logger);
  }

  /**
   * Checks if the ISP names conflicts with a node name in the configurations or with another ISP
   * name. Returns messages that explain the conflicts
   */
  @VisibleForTesting
  static List<String> ispNameConflicts(
      Map<String, Configuration> configurations, Map<Long, IspModel> asnToIspInfo) {
    ImmutableList.Builder<String> conflicts = ImmutableList.builder();

    asnToIspInfo.forEach(
        (asn, ispInfo) -> {
          String ispName = ispInfo.getName();
          if (configurations.containsKey(ispName)) {
            conflicts.add(
                String.format(
                    "ISP name %s for ASN %d conflicts with a node name in the snapshot",
                    ispName, asn));
          }
          asnToIspInfo.values().stream()
              .filter(info -> info.getAsn() > asn && info.getName().equals(ispName))
              .forEach(
                  info ->
                      conflicts.add(
                          String.format(
                              "ISP name %s for ASN %d conflicts with that for ASN %d",
                              ispName, asn, info.getAsn())));
        });
    return conflicts.build();
  }

  @VisibleForTesting
  static Map<Long, IspModel> combineIspConfigurations(
      Map<String, Configuration> configurations,
      List<IspConfiguration> ispConfigurations,
      Warnings warnings) {
    Map<Long, IspModel> asnToIspInfos = new HashMap<>();

    for (IspConfiguration ispConfiguration : ispConfigurations) {
      Map<String, Set<String>> interfaceSetByNodes =
          ispConfiguration.getBorderInterfaces().stream()
              .map(BorderInterfaceInfo::getBorderInterface)
              .collect(
                  Collectors.groupingBy(
                      nodeInterfacePair -> nodeInterfacePair.getHostname().toLowerCase(),
                      Collectors.mapping(NodeInterfacePair::getInterface, Collectors.toSet())));

      for (Entry<String, Set<String>> remoteNodeAndInterfaces : interfaceSetByNodes.entrySet()) {
        Configuration remoteCfg = configurations.get(remoteNodeAndInterfaces.getKey());
        if (remoteCfg == null) {
          warnings.redFlag(
              String.format(
                  "ISP Modeling: Non-existent border node %s specified in ISP configuration",
                  remoteNodeAndInterfaces.getKey()));
          continue;
        }
        populateIspModels(
            remoteCfg,
            remoteNodeAndInterfaces.getValue(),
            ispConfiguration.getfilter().getOnlyRemoteIps(),
            ispConfiguration.getfilter().getOnlyRemoteAsns(),
            ispConfiguration.getIspNodeInfos(),
            asnToIspInfos,
            warnings);
      }
    }

    return asnToIspInfos;
  }

  private static ModeledNodes createInternetAndIspNodes(
      Map<Long, IspModel> asnToIspModel, NetworkFactory nf, BatfishLogger logger) {
    ModeledNodes modeledNodes = new ModeledNodes();

    asnToIspModel.values().forEach(ispModel -> createIspNode(modeledNodes, ispModel, nf, logger));

    // not proceeding if no ISPs were created
    if (modeledNodes.getConfigurations().isEmpty()) {
      return modeledNodes;
    }

    createInternetNode(modeledNodes);

    connectIspsToInternet(modeledNodes, nf);

    return modeledNodes;
  }

  /** Creates the modeled Internet node and inserts it into {@code modeledNodes} */
  @VisibleForTesting
  static void createInternetNode(ModeledNodes modeledNodes) {
    Configuration.Builder cb = Configuration.builder();
    Configuration internetConfiguration =
        cb.setHostname(INTERNET_HOST_NAME)
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setDeviceModel(DeviceModel.BATFISH_INTERNET)
            .build();
    internetConfiguration.setDeviceType(DeviceType.INTERNET);
    Vrf defaultVrf =
        Vrf.builder().setName(DEFAULT_VRF_NAME).setOwner(internetConfiguration).build();
    Interface internetOutInterface =
        Interface.builder()
            .setName(INTERNET_OUT_INTERFACE)
            .setOwner(internetConfiguration)
            .setVrf(defaultVrf)
            .setAddress(
                ConcreteInterfaceAddress.create(
                    INTERNET_OUT_ADDRESS, INTERNET_OUT_SUBNET.getPrefixLength()))
            .build();

    internetConfiguration
        .getDefaultVrf()
        .setStaticRoutes(
            new ImmutableSortedSet.Builder<StaticRoute>(naturalOrder())
                .add(
                    StaticRoute.builder()
                        .setNetwork(Prefix.ZERO)
                        .setNextHopInterface(INTERNET_OUT_INTERFACE)
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
                .build());

    BgpProcess bgpProcess =
        BgpProcess.builder()
            .setRouterId(INTERNET_OUT_ADDRESS)
            .setVrf(defaultVrf)
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    bgpProcess.setMultipathEbgp(true);
    bgpProcess.setMultipathEquivalentAsPathMatchMode(
        MultipathEquivalentAsPathMatchMode.PATH_LENGTH);

    internetConfiguration.setRoutingPolicies(
        ImmutableSortedMap.of(
            EXPORT_POLICY_ON_INTERNET,
            installRoutingPolicyAdvertiseStatic(
                EXPORT_POLICY_ON_INTERNET,
                internetConfiguration,
                new PrefixSpace(PrefixRange.fromPrefix(Prefix.ZERO)))));

    internetConfiguration.setLocationInfo(
        ImmutableMap.of(
            interfaceLinkLocation(internetOutInterface),
            INTERNET_OUT_INTERFACE_LINK_LOCATION_INFO));

    modeledNodes.addConfiguration(internetConfiguration);
  }

  /**
   * Adds connection between internet and each ISP by creating interface pairs (in /31 subnet) on
   * both with connected edges. Also adds eBGP peers on both Internet and all the ISPs to peer with
   * each other using the created Interface pairs.
   */
  private static void connectIspsToInternet(ModeledNodes modeledNodes, NetworkFactory nf) {
    Configuration internet = modeledNodes.getConfigurations().get(INTERNET_HOST_NAME);
    for (Configuration ispConfiguration : modeledNodes.getConfigurations().values()) {
      if (ispConfiguration.getHostname().equals(INTERNET_HOST_NAME)) {
        continue;
      }
      long ispAs = getAsnOfIspNode(ispConfiguration);
      Interface internetIface =
          nf.interfaceBuilder()
              .setOwner(internet)
              .setVrf(internet.getDefaultVrf())
              .setAddress(LINK_LOCAL_ADDRESS)
              .build();
      Interface ispIface = ispConfiguration.getAllInterfaces().get(ISP_TO_INTERNET_INTERFACE_NAME);

      BgpUnnumberedPeerConfig.builder()
          .setPeerInterface(ispIface.getName())
          .setRemoteAs(INTERNET_AS)
          .setLocalAs(ispAs)
          .setLocalIp(LINK_LOCAL_IP)
          .setBgpProcess(ispConfiguration.getDefaultVrf().getBgpProcess())
          .setIpv4UnicastAddressFamily(
              Ipv4UnicastAddressFamily.builder()
                  .setExportPolicy(EXPORT_POLICY_ON_ISP_TO_INTERNET)
                  .build())
          .build();

      BgpUnnumberedPeerConfig.builder()
          .setPeerInterface(internetIface.getName())
          .setRemoteAs(ispAs)
          .setLocalAs(INTERNET_AS)
          .setLocalIp(LINK_LOCAL_IP)
          .setBgpProcess(internet.getDefaultVrf().getBgpProcess())
          .setIpv4UnicastAddressFamily(
              Ipv4UnicastAddressFamily.builder().setExportPolicy(EXPORT_POLICY_ON_INTERNET).build())
          .build();

      modeledNodes.addLayer1Edge(
          internet.getHostname(),
          internetIface.getName(),
          ispConfiguration.getHostname(),
          ispIface.getName());
    }
  }

  /**
   * Extracts the ISP information from a given {@link Configuration} and merges it to a given map of
   * ASNs to {@link IspModel}s
   *
   * @param remoteCfg {@link Configuration} owning given interfaces
   * @param remoteInterfaces {@link List} of interfaces on this node having eBGP sessions with the
   *     ISP
   * @param remoteIps Expected {@link Ip}s of the ISPs (optional)
   * @param remoteAsnsList Expected ASNs of the ISP nodes (optional)
   * @param allIspModels {@link Map} containing existing ASNs and corresponding {@link IspModel}s to
   *     which ISPs extracted from this {@link Configuration} will be merged
   * @param warnings {@link Warnings} for ISP and Internet modeling
   */
  @VisibleForTesting
  static void populateIspModels(
      @Nonnull Configuration remoteCfg,
      @Nonnull Set<String> remoteInterfaces,
      @Nonnull List<Ip> remoteIps,
      @Nonnull List<Long> remoteAsnsList,
      @Nonnull List<IspNodeInfo> ispNodeInfos,
      Map<Long, IspModel> allIspModels,
      @Nonnull Warnings warnings) {

    Set<Ip> remoteIpsSet = ImmutableSet.copyOf(remoteIps);
    LongSpace remoteAsns =
        remoteAsnsList.isEmpty()
            ? ALL_AS_NUMBERS
            : LongSpace.builder().includingAll(remoteAsnsList).build();

    Map<String, Interface> lowerCasedInterfaces =
        remoteCfg.getAllInterfaces().entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Entry::getValue));

    for (String remoteIfaceName : remoteInterfaces) {
      Interface remoteIface = lowerCasedInterfaces.get(remoteIfaceName.toLowerCase());
      if (remoteIface == null) {
        warnings.redFlag(
            String.format(
                "ISP Modeling: Cannot find interface %s on node %s",
                remoteIfaceName, remoteCfg.getHostname()));
        continue;
      }
      // collecting InterfaceAddresses for interfaces
      Map<Ip, InterfaceAddress> ipToInterfaceAddresses =
          remoteIface.getAllAddresses().stream()
              .collect(
                  ImmutableMap.toImmutableMap(
                      addr ->
                          addr instanceof ConcreteInterfaceAddress
                              ? ((ConcreteInterfaceAddress) addr).getIp()
                              : ((LinkLocalAddress) addr).getIp(),
                      Function.identity()));

      List<BgpPeerConfig> validRemoteBgpPeerConfigs =
          remoteCfg.getVrfs().values().stream()
              .map(Vrf::getBgpProcess)
              .filter(Objects::nonNull)
              .flatMap(
                  bgpProcess ->
                      Streams.concat(
                          bgpProcess.getActiveNeighbors().values().stream(),
                          bgpProcess.getInterfaceNeighbors().values().stream()))
              .filter(
                  bgpPeerConfig ->
                      isValidBgpPeerConfig(
                          bgpPeerConfig, ipToInterfaceAddresses.keySet(), remoteIpsSet, remoteAsns))
              .collect(Collectors.toList());

      if (validRemoteBgpPeerConfigs.isEmpty()) {
        warnings.redFlag(
            String.format(
                "ISP Modeling: Cannot find any valid eBGP configurations for interface %s on node %s",
                remoteIfaceName, remoteCfg.getHostname()));
        continue;
      }
      for (BgpPeerConfig bgpPeerConfig : validRemoteBgpPeerConfigs) {
        long asn = bgpPeerConfig.getRemoteAsns().least();
        // Pick the first IspInfo we find.
        Optional<IspNodeInfo> isp =
            ispNodeInfos.stream().filter(i -> i.getAsn() == asn).findFirst();
        String ispName = isp.map(IspNodeInfo::getName).orElse(getDefaultIspNodeName(asn));
        // Merge the sets of additional announcements to internet is merging their prefixes
        Set<Prefix> additionalPrefixes =
            isp.map(IspNodeInfo::getAdditionalAnnouncements)
                .map(List::stream)
                .orElse(Stream.empty())
                .map(IspAnnouncement::getPrefix)
                .collect(ImmutableSet.toImmutableSet());
        IspTrafficFiltering trafficFiltering =
            isp.map(IspNodeInfo::getIspTrafficFiltering)
                .orElse(IspTrafficFiltering.blockReservedAddressesAtInternet());
        IspModel ispInfo =
            allIspModels.computeIfAbsent(
                asn,
                k ->
                    IspModel.builder()
                        .setAsn(asn)
                        .setName(ispName)
                        .setAdditionalPrefixesToInternet(additionalPrefixes)
                        .setTrafficFiltering(trafficFiltering)
                        .build());
        InterfaceAddress interfaceAddress =
            bgpPeerConfig instanceof BgpActivePeerConfig
                ? ConcreteInterfaceAddress.create(
                    ((BgpActivePeerConfig) bgpPeerConfig).getPeerAddress(),
                    ((ConcreteInterfaceAddress)
                            ipToInterfaceAddresses.get(bgpPeerConfig.getLocalIp()))
                        .getNetworkBits())
                : LINK_LOCAL_ADDRESS;
        ispInfo.addNeighbor(
            new Remote(remoteCfg.getHostname(), remoteIfaceName, interfaceAddress, bgpPeerConfig));
      }
    }
  }

  /**
   * Creates the {@link Configuration} for the ISP node given an ASN and {@link IspModel}. Inserts
   * that node and its layer1 edges to the Internet into {@code modeledNodes}.
   */
  @VisibleForTesting
  static void createIspNode(
      ModeledNodes modeledNodes, IspModel ispInfo, NetworkFactory nf, BatfishLogger logger) {
    if (ispInfo.getRemotes().isEmpty()) {
      logger.warnf("ISP information for ASN '%s' is not correct", ispInfo.getAsn());
      return;
    }

    Configuration ispConfiguration =
        Configuration.builder()
            .setHostname(ispInfo.getName())
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setDeviceModel(DeviceModel.BATFISH_ISP)
            .build();
    ispConfiguration.setDeviceType(DeviceType.ISP);
    Vrf defaultVrf = Vrf.builder().setName(DEFAULT_VRF_NAME).setOwner(ispConfiguration).build();

    // add a static route for each additional prefix announced to the internet
    ispConfiguration
        .getDefaultVrf()
        .setStaticRoutes(
            ImmutableSortedSet.copyOf(
                ispInfo.getAdditionalPrefixesToInternet().stream()
                    .map(
                        prefix ->
                            StaticRoute.builder()
                                .setNetwork(prefix)
                                .setNextHopInterface(NULL_INTERFACE_NAME)
                                .setAdministrativeCost(HIGH_ADMINISTRATIVE_COST)
                                .build())
                    .collect(ImmutableSet.toImmutableSet())));

    PrefixSpace prefixSpace = new PrefixSpace();
    ispInfo.getAdditionalPrefixesToInternet().forEach(prefixSpace::addPrefix);

    ispConfiguration.setRoutingPolicies(
        ImmutableSortedMap.of(
            EXPORT_POLICY_ON_ISP_TO_CUSTOMERS,
            installRoutingPolicyForIspToCustomers(ispConfiguration),
            EXPORT_POLICY_ON_ISP_TO_INTERNET,
            installRoutingPolicyForIspToInternet(ispConfiguration, prefixSpace)));

    // using the lowest IP among the InterfaceAddresses as the router ID
    BgpProcess bgpProcess =
        nf.bgpProcessBuilder()
            .setRouterId(
                ispInfo.getRemotes().stream()
                    .map(Remote::getIspIfaceIp)
                    .min(Ip::compareTo)
                    .orElse(null))
            .setVrf(ispConfiguration.getDefaultVrf())
            .setAdminCostsToVendorDefaults(ConfigurationFormat.CISCO_IOS)
            .build();
    bgpProcess.setMultipathEbgp(true);

    // Get the traffic filtering policy for this ISP.
    IspTrafficFilteringPolicy fp =
        IspTrafficFilteringPolicy.createFor(ispInfo.getTrafficFiltering());
    // Get the 4 filters out, add them all to the node.
    IpAccessList toInternet = fp.filterTrafficToInternet();
    if (toInternet != null) {
      ispConfiguration.getIpAccessLists().put(toInternet.getName(), toInternet);
    }
    IpAccessList fromInternet = fp.filterTrafficFromInternet();
    if (fromInternet != null) {
      ispConfiguration.getIpAccessLists().put(fromInternet.getName(), fromInternet);
    }
    IpAccessList toNetwork = fp.filterTrafficToNetwork();
    if (toNetwork != null) {
      ispConfiguration.getIpAccessLists().put(toNetwork.getName(), toNetwork);
    }
    IpAccessList fromNetwork = fp.filterTrafficFromNetwork();
    if (fromNetwork != null) {
      ispConfiguration.getIpAccessLists().put(fromNetwork.getName(), fromNetwork);
    }
    // Create Internet-facing interface and apply filters.
    nf.interfaceBuilder()
        .setOwner(ispConfiguration)
        .setVrf(ispConfiguration.getDefaultVrf())
        .setName(ISP_TO_INTERNET_INTERFACE_NAME)
        .setAddress(LINK_LOCAL_ADDRESS)
        .setIncomingFilter(fromInternet)
        .setOutgoingFilter(toInternet)
        .build();

    ispInfo
        .getRemotes()
        .forEach(
            remote -> {
              Interface ispInterface =
                  nf.interfaceBuilder()
                      .setOwner(ispConfiguration)
                      .setVrf(defaultVrf)
                      .setAddress(remote.getIspIfaceAddress())
                      .setIncomingFilter(fromNetwork)
                      .setOutgoingFilter(toNetwork)
                      .build();
              modeledNodes.addLayer1Edge(
                  ispConfiguration.getHostname(),
                  ispInterface.getName(),
                  remote.getRemoteHostname(),
                  remote.getRemoteIfaceName());
              addBgpPeerToIsp(remote.getRemoteBgpPeerConfig(), ispInterface.getName(), bgpProcess);
            });

    modeledNodes.addConfiguration(ispConfiguration);
  }

  /**
   * Gets the local AS of a given ISP node {@link Configuration}. Since Local AS of all eBGP peers
   * on this node will be same, returning the Local AS of the any eBGP peer will suffice.
   */
  @VisibleForTesting
  @Nonnull
  static Long getAsnOfIspNode(Configuration ispConfiguration) {
    checkState(
        Objects.nonNull(ispConfiguration.getDefaultVrf()), "default VRF should be present in ISP");
    checkState(
        Objects.nonNull(ispConfiguration.getDefaultVrf().getBgpProcess()),
        "default VRF should have a BGP process");
    BgpProcess bgpProcess = ispConfiguration.getDefaultVrf().getBgpProcess();
    checkState(
        !(bgpProcess.getActiveNeighbors().isEmpty()
            && bgpProcess.getInterfaceNeighbors().isEmpty()),
        "ISP should have greater than 0 BGP peers");
    Long localAs =
        ispConfiguration
            .getDefaultVrf()
            .getBgpProcess()
            .getAllPeerConfigs()
            .iterator()
            .next()
            .getLocalAs();
    checkState(Objects.nonNull(localAs), "Local AS of all eBGP peers should be set on ISP");
    return localAs;
  }

  /**
   * Installs a routing policy named {@code policyName} on {@code node} that advertises all static
   * routes to {@code prefixSpace}. Returns the created policy.
   */
  public static RoutingPolicy installRoutingPolicyAdvertiseStatic(
      String policyName, Configuration node, PrefixSpace prefixSpace) {
    return RoutingPolicy.builder()
        .setName(policyName)
        .setOwner(node)
        .setStatements(Collections.singletonList(getAdvertiseStaticStatement(prefixSpace)))
        .build();
  }

  /** Returns a routing policy statement that advertises static routes to {@code prefixSpace} */
  public static Statement getAdvertiseStaticStatement(PrefixSpace prefixSpace) {
    return new If(
        new Conjunction(
            ImmutableList.of(
                new MatchProtocol(RoutingProtocol.STATIC),
                new MatchPrefixSet(
                    DestinationNetwork.instance(), new ExplicitPrefixSet(prefixSpace)))),
        ImmutableList.of(
            new SetOrigin(new LiteralOrigin(OriginType.INCOMPLETE, null)),
            Statements.ExitAccept.toStaticStatement()));
  }

  /** Returns a routing policy statement that advertises all BGP routes */
  public static Statement getAdvertiseBgpStatement() {
    return new If(
        new MatchProtocol(RoutingProtocol.BGP),
        ImmutableList.of(Statements.ReturnTrue.toStaticStatement()));
  }

  /** Creates a routing policy to export all BGP routes */
  @VisibleForTesting
  static RoutingPolicy installRoutingPolicyForIspToCustomers(Configuration isp) {
    return RoutingPolicy.builder()
        .setName(EXPORT_POLICY_ON_ISP_TO_CUSTOMERS)
        .setOwner(isp)
        .setStatements(Collections.singletonList(getAdvertiseBgpStatement()))
        .build();
  }

  /** Creates a routing policy to export all BGP and static routes */
  @VisibleForTesting
  static RoutingPolicy installRoutingPolicyForIspToInternet(
      Configuration isp, PrefixSpace prefixSpace) {
    return RoutingPolicy.builder()
        .setName(EXPORT_POLICY_ON_ISP_TO_INTERNET)
        .setOwner(isp)
        .setStatements(
            ImmutableList.of(getAdvertiseBgpStatement(), getAdvertiseStaticStatement(prefixSpace)))
        .build();
  }

  @VisibleForTesting
  static boolean isValidBgpPeerConfig(
      @Nonnull BgpPeerConfig bgpPeerConfig,
      @Nonnull Set<Ip> localIps,
      @Nonnull Set<Ip> remoteIps,
      @Nonnull LongSpace remoteAsns) {
    boolean commonCriteria =
        Objects.nonNull(bgpPeerConfig.getLocalIp())
            && Objects.nonNull(bgpPeerConfig.getLocalAs())
            && !bgpPeerConfig.getRemoteAsns().equals(LongSpace.of(bgpPeerConfig.getLocalAs()))
            && localIps.contains(bgpPeerConfig.getLocalIp())
            && !remoteAsns.intersection(bgpPeerConfig.getRemoteAsns()).isEmpty();
    if (!commonCriteria) {
      return false;
    }
    if (bgpPeerConfig instanceof BgpActivePeerConfig) {
      BgpActivePeerConfig activePeerConfig = (BgpActivePeerConfig) bgpPeerConfig;
      return Objects.nonNull(activePeerConfig.getPeerAddress())
          && (remoteIps.isEmpty() || remoteIps.contains(activePeerConfig.getPeerAddress()));
    } else if (bgpPeerConfig instanceof BgpUnnumberedPeerConfig) {
      // peer interface is always non-null, so need to check
      return true;
    } else {
      // passive peers, in case passed into this function, are declared invalid
      return false;
    }
  }

  /**
   * Computes the mirror of the {@code remotePeerConfig} and adds it to {@code bgpProcess}. Also
   * sets the export policy meant for the ISP.
   */
  @VisibleForTesting
  static void addBgpPeerToIsp(
      BgpPeerConfig remotePeerConfig, String localInterfaceName, BgpProcess bgpProcess) {
    BgpPeerConfig.Builder<?, ?> ispPeerConfig =
        remotePeerConfig instanceof BgpActivePeerConfig
            ? BgpActivePeerConfig.builder()
                .setPeerAddress(remotePeerConfig.getLocalIp())
                .setLocalIp(((BgpActivePeerConfig) remotePeerConfig).getPeerAddress())
            : BgpUnnumberedPeerConfig.builder()
                .setPeerInterface(localInterfaceName)
                .setLocalIp(LINK_LOCAL_IP);

    ispPeerConfig
        .setRemoteAs(
            firstNonNull(remotePeerConfig.getConfederationAsn(), remotePeerConfig.getLocalAs()))
        .setLocalAs(remotePeerConfig.getRemoteAsns().least())
        .setIpv4UnicastAddressFamily(
            Ipv4UnicastAddressFamily.builder()
                .setExportPolicy(EXPORT_POLICY_ON_ISP_TO_CUSTOMERS)
                .build())
        .setBgpProcess(bgpProcess)
        .build();
  }
}

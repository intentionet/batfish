package org.batfish.representation.aws;

import static org.batfish.common.util.isp.IspModelingUtils.installRoutingPolicyAdvertiseStatic;
import static org.batfish.representation.aws.LoadBalancer.getActiveTargets;
import static org.batfish.representation.aws.LoadBalancer.getTargetIp;
import static org.batfish.representation.aws.Utils.addStaticRoute;
import static org.batfish.representation.aws.Utils.toStaticRoute;
import static org.batfish.specifier.Location.interfaceLinkLocation;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BfConsts;
import org.batfish.common.VendorConversionException;
import org.batfish.common.topology.Layer1Edge;
import org.batfish.common.util.isp.IspModelingUtils;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LinkLocalAddress;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.answers.ParseVendorConfigurationAnswerElement;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.datamodel.isp_configuration.BorderInterfaceInfo;
import org.batfish.datamodel.isp_configuration.IspAnnouncement;
import org.batfish.datamodel.isp_configuration.IspConfiguration;
import org.batfish.datamodel.isp_configuration.IspFilter;
import org.batfish.datamodel.isp_configuration.IspNodeInfo;
import org.batfish.representation.aws.LoadBalancer.AvailabilityZone;
import org.batfish.representation.aws.TargetGroup.Type;
import org.batfish.specifier.LocationInfo;
import org.batfish.vendor.VendorConfiguration;

/** The top-level class that represent AWS configuration across different accounts */
@ParametersAreNonnullByDefault
public class AwsConfiguration extends VendorConfiguration {

  public static final String DEFAULT_REGION_NAME = "us-west-2";
  static final Ip LINK_LOCAL_IP = Ip.parse("169.254.0.1");
  public static final String DEFAULT_ACCOUNT_NAME = "default";

  /**
   * The name to use for the logical node that represents the AWS services gateway.
   *
   * <p>TODO: ensure that this name does not conflict with any name that appears in the snapshot
   */
  static final String AWS_SERVICES_GATEWAY_NODE_NAME = "__aws-services-gateway__";

  static final String AWS_SERVICES_GATEWAY_HUMAN_NAME = "public aws services gateway";

  static final String AWS_SERVICES_FACING_INTERFACE_NAME = "aws-services";

  static final String AWS_SERVICES_GATEWAY_EXPORT_POLICY_NAME = "~asgw~to~backbone~export~policy~";

  /** Human name to use for AWS backbone */
  static final String AWS_BACKBONE_HUMAN_NAME = "aws-backbone";

  /** ASN to use for AWS backbone */
  static final long AWS_BACKBONE_ASN = 16509L;

  /** Hostname to use for AWS backbone */
  public static final String AWS_BACKBONE_HOSTNAME =
      IspModelingUtils.getDefaultIspNodeName(AWS_BACKBONE_ASN);

  /** Name of the interface on nodes that faces the backbone (e.g., IGW, services gateway) */
  static final String BACKBONE_FACING_INTERFACE_NAME = "backbone";

  /** ASN to use for nodes that faces the backbone (e.g., IGW, services gateway) */
  static final long BACKBONE_PEERING_ASN = 65534L;

  @Nullable private ConvertedConfiguration _convertedConfiguration;
  @Nonnull private final Map<String, Account> _accounts;

  /**
   * Multimap of subnet IDs to {@link Instance} in that subnet used as targets by some {@link
   * LoadBalancer}
   */
  private Multimap<String, Instance> _subnetsToInstanceTargets;

  /**
   * Multimap of subnet IDs to {@link LoadBalancer} connected to that subnet that have active
   * instance targets
   */
  private Multimap<String, LoadBalancer> _subnetsToNlbs;

  /** Multimap of load balancer ARNs to {@link Instance} used as targets for that load balancer */
  private Multimap<String, Instance> _nlbsToInstanceTargets;

  public AwsConfiguration() {
    this(new HashMap<>());
  }

  private AwsConfiguration(Map<String, Account> accounts) {
    _accounts = accounts;
  }

  public Collection<Account> getAccounts() {
    return _accounts.values();
  }

  /** Return a stream of all VPCs, across all accounts */
  @Nonnull
  public Stream<Vpc> getAllVpc() {
    return getAccounts().stream()
        .flatMap(a -> a.getRegions().stream())
        .flatMap(r -> r.getVpcs().values().stream());
  }

  /** Return a VPC with a given ID (in any account/region) if it exists, or {@code null} */
  @Nullable
  public Vpc getVpc(String vpcId) {
    return getAllVpc().filter(v -> vpcId.equals(v.getId())).findFirst().orElse(null);
  }

  @Nonnull
  public Account addOrGetAccount(String accountId) {
    return _accounts.computeIfAbsent(accountId, Account::new);
  }

  /** Adds a config subtree */
  public void addConfigElement(
      String region,
      JsonNode json,
      String sourceFileName,
      ParseVendorConfigurationAnswerElement pvcae,
      String account) {
    addOrGetAccount(account).addOrGetRegion(region).addConfigElement(json, sourceFileName, pvcae);
  }

  /**
   * Convert this AWS config to a set of VI configurations
   *
   * <p>TODO: Populate all the structure names that appear in these configs
   */
  @Nonnull
  @Override
  public List<Configuration> toVendorIndependentConfigurations() throws VendorConversionException {
    if (_convertedConfiguration == null) {
      convertConfigurations();
    }
    return ImmutableList.copyOf(_convertedConfiguration.getAllNodes());
  }

  @VisibleForTesting
  void populatePrecomputedMaps() {
    // A multimap from subnet ID -> Instance targets within subnet
    ImmutableMultimap.Builder<String, Instance> subnetsToInstanceTargets =
        ImmutableMultimap.builder();

    // A multimap from subnet ID -> NLBs within subnet that have instance targets
    ImmutableMultimap.Builder<String, LoadBalancer> subnetsToNlbs = ImmutableMultimap.builder();

    // A multimap of load balancer ARN -> instance targets
    ImmutableMultimap.Builder<String, Instance> nlbsToInstanceTargets = ImmutableMultimap.builder();

    for (Account account : getAccounts()) {
      Collection<Region> regions = account.getRegions();
      for (Region region : regions) {
        for (Entry<String, TargetGroup> targetEntry : region.getTargetGroups().entrySet()) {
          TargetGroup targetGroup = targetEntry.getValue();
          if (targetGroup.getTargetType() != Type.INSTANCE) {
            continue;
          }
          String targetGroupArn = targetEntry.getKey();
          LoadBalancerTargetHealth lbth = region.getLoadBalancerTargetHealth(targetGroupArn);
          if (lbth == null) {
            continue;
          }

          for (String lbArn : targetGroup.getLoadBalancerArns()) {
            LoadBalancerAttributes loadBalancerAttributes =
                region.getLoadBalancerAttributes().get(lbArn);
            boolean crossZoneLoadBalancing =
                loadBalancerAttributes != null
                    && loadBalancerAttributes.getCrossZoneLoadBalancing();
            LoadBalancer lb = region.getLoadBalancersMap().get(lbArn);
            if (lb.getType() == LoadBalancer.Type.APPLICATION) {
              // Application load balancers not supported
              continue;
            }
            Set<String> azNames =
                lb.getAvailabilityZones().stream()
                    .map(AvailabilityZone::getZoneName)
                    .collect(ImmutableSet.toImmutableSet());
            AtomicBoolean hasInstanceTarget = new AtomicBoolean(false);

            getActiveTargets(
                    lbth, targetGroup, azNames, crossZoneLoadBalancing, region, false, null)
                .stream()
                .filter(desc -> getTargetIp(desc.getTarget(), Type.INSTANCE, region) != null)
                .map(desc -> region.getInstances().get(desc.getTarget().getId()))
                .forEach(
                    instance -> {
                      assert instance.getSubnetId() != null; // guaranteed by getActiveTargets
                      subnetsToInstanceTargets.put(instance.getSubnetId(), instance);
                      nlbsToInstanceTargets.put(lbArn, instance);
                      hasInstanceTarget.set(true);
                    });
            if (hasInstanceTarget.get()) {
              lb.getAvailabilityZones().stream()
                  .map(AvailabilityZone::getSubnetId)
                  .filter(region.getSubnets()::containsKey)
                  .forEach(subnetId -> subnetsToNlbs.put(subnetId, lb));
            }
          }
        }
      }
    }
    _subnetsToInstanceTargets = subnetsToInstanceTargets.build();
    _subnetsToNlbs = subnetsToNlbs.build();
    _nlbsToInstanceTargets = nlbsToInstanceTargets.build();
  }

  private void convertConfigurations() {
    populatePrecomputedMaps();
    _convertedConfiguration = new ConvertedConfiguration(this);
    if (!_accounts.isEmpty()) { // generate only if we have any data
      _convertedConfiguration.addNode(generateAwsServicesGateway());
    }
    for (Account account : getAccounts()) {
      Collection<Region> regions = account.getRegions();
      for (Region region : regions) {
        try {
          region.toConfigurationNodes(_convertedConfiguration, getWarnings());
          region.addPrefixListReferenceBook(_convertedConfiguration, getWarnings());
        } catch (Exception e) {
          getWarnings()
              .redFlag(
                  String.format(
                      "Failed conversion for account %s, region %s\n%s",
                      account.getId(), region.getName(), Throwables.getStackTraceAsString(e)));
        }
      }
    }
    // Vpc peerings can be both cross-region and cross-account, so we handle them here
    processVpcPeerings();
    // Transit gateways can be cross-account so we handle them here
    try {
      TransitGatewayConverter.convertTransitGateways(this, _convertedConfiguration)
          .forEach(_convertedConfiguration::addNode);
    } catch (Exception e) {
      getWarnings().redFlag(String.format("Failed to convert transit gateways %s", e));
    }
    TransitGatewayPeeringConnector.connect(this, _convertedConfiguration, getWarnings());
  }

  private void processVpcPeerings() {
    // We do this de-duplication (collecting to a set) because cross-region (or cross-account)
    // connections will show up in both regions. No need to re-create them twice in conversion.
    Set<VpcPeeringConnection> vpcPeeringConnections =
        getAccounts().stream()
            .flatMap(a -> a.getRegions().stream())
            .flatMap(r -> r.getVpcPeeringConnections().values().stream())
            .collect(ImmutableSet.toImmutableSet());
    try {
      vpcPeeringConnections.forEach(
          c -> c.createConnection(_convertedConfiguration, getWarnings()));
    } catch (Exception e) {
      getWarnings().redFlag(String.format("Failed to process VPC peerings %s", e));
    }
  }

  @Override
  @Nonnull
  public IspConfiguration getIspConfiguration() {
    if (_convertedConfiguration == null) {
      throw new IllegalStateException(
          "getIspConfiguration called when converted configuration is null");
    }
    List<BorderInterfaceInfo> borderInterfaces =
        getAccounts().stream()
            .flatMap(a -> a.getRegions().stream())
            .flatMap(
                r ->
                    Streams.concat(
                            // all types of nodes that connect to the backbone
                            r.getInternetGateways().keySet().stream(),
                            r.getTransitGateways().keySet().stream(),
                            r.getVpnGateways().keySet().stream(),
                            Stream.of(AWS_SERVICES_GATEWAY_NODE_NAME))
                        .filter(
                            gw ->
                                _convertedConfiguration.getNode(gw) != null
                                    && Objects.requireNonNull(_convertedConfiguration.getNode(gw))
                                            .getAllInterfaces()
                                            .get(BACKBONE_FACING_INTERFACE_NAME)
                                        != null)
                        .map(gw -> NodeInterfacePair.of(gw, BACKBONE_FACING_INTERFACE_NAME)))
            .map(BorderInterfaceInfo::new)
            .collect(Collectors.toList());
    return new IspConfiguration(
        ImmutableList.copyOf(borderInterfaces),
        IspFilter.ALLOW_ALL,
        ImmutableList.of(
            new IspNodeInfo(
                AWS_BACKBONE_ASN,
                AWS_BACKBONE_HUMAN_NAME,
                AwsPrefixes.getPrefixes(AwsPrefixes.SERVICE_AMAZON).stream()
                    .map(IspAnnouncement::new)
                    .collect(ImmutableList.toImmutableList()))));
  }

  /**
   * Generates a logical node that is the gateway to AWS services like S3.
   *
   * <p>This node will BGP peer with the AWS backbone (via the getIspConfiguration route), and it
   * will announce all AWS service prefixes to the backbone.
   */
  static Configuration generateAwsServicesGateway() {
    Configuration cfgNode =
        Configuration.builder()
            // don't use AWS config format; we don't want this node to be part of the AWS aggregate
            // cisco_ios is a placeholder we use for isps as well
            .setConfigurationFormat(ConfigurationFormat.CISCO_IOS)
            .setHostname(AWS_SERVICES_GATEWAY_NODE_NAME)
            .setHumanName(AWS_SERVICES_GATEWAY_HUMAN_NAME)
            .setDefaultInboundAction(LineAction.PERMIT)
            .setDefaultCrossZoneAction(LineAction.PERMIT)
            .setDeviceModel(DeviceModel.AWS_SERVICES_GATEWAY)
            .build();
    Vrf.builder().setName(Configuration.DEFAULT_VRF_NAME).setOwner(cfgNode).build();

    Interface outInterface =
        Utils.newInterface(
            AWS_SERVICES_FACING_INTERFACE_NAME,
            cfgNode,
            LinkLocalAddress.of(LINK_LOCAL_IP),
            "To AWS services");

    Set<Prefix> awsServicesPrefixes = AwsPrefixes.getAwsServicesPrefixes();

    PrefixSpace servicesPrefixSpace = new PrefixSpace();
    awsServicesPrefixes.forEach(
        prefix -> {
          servicesPrefixSpace.addPrefix(prefix);
          addStaticRoute(cfgNode, toStaticRoute(prefix, outInterface.getName()));
        });

    installRoutingPolicyAdvertiseStatic(
        AWS_SERVICES_GATEWAY_EXPORT_POLICY_NAME, cfgNode, servicesPrefixSpace);
    Utils.createBackboneConnection(
        cfgNode, cfgNode.getDefaultVrf(), AWS_SERVICES_GATEWAY_EXPORT_POLICY_NAME);

    cfgNode
        .getAllInterfaces()
        .get(BACKBONE_FACING_INTERFACE_NAME)
        .setFirewallSessionInterfaceInfo(
            new FirewallSessionInterfaceInfo(
                false, ImmutableList.of(BACKBONE_FACING_INTERFACE_NAME), null, null));

    outInterface.setFirewallSessionInterfaceInfo(
        new FirewallSessionInterfaceInfo(
            false, ImmutableList.of(outInterface.getName()), null, null));

    // configure location info
    IpSpace servicesIpSpace =
        IpWildcardSetIpSpace.builder()
            .including(
                awsServicesPrefixes.stream().map(IpWildcard::create).collect(Collectors.toList()))
            .build();
    cfgNode.setLocationInfo(
        ImmutableMap.of(
            interfaceLinkLocation(outInterface),
            new LocationInfo(
                true,
                servicesIpSpace,
                // using LINK_LOCAL_IP gets us EXITS_NETWORK as disposition for service prefixes
                LINK_LOCAL_IP.toIpSpace())));

    return cfgNode;
  }

  @Override
  public String getFilename() {
    // not a real file name but a folder
    return BfConsts.RELPATH_AWS_CONFIGS_FILE;
  }

  @Override
  public String getHostname() {
    // This hostname does not appear in the vendor independent configs that are returned
    return BfConsts.RELPATH_AWS_CONFIGS_FILE;
  }

  /** Subnet IDs to {@link Instance} in that subnet used as targets by some {@link LoadBalancer} */
  @VisibleForTesting
  Multimap<String, Instance> getSubnetsToInstanceTargets() {
    return _subnetsToInstanceTargets;
  }

  /**
   * Subnet IDs to {@link LoadBalancer} connected to that subnet that have active instance targets
   */
  @VisibleForTesting
  Multimap<String, LoadBalancer> getSubnetsToNlbs() {
    return _subnetsToNlbs;
  }

  /** Load balancer ARNs to {@link Instance} used as targets for that load balancer */
  @VisibleForTesting
  Multimap<String, Instance> getNlbsToInstanceTargets() {
    return _nlbsToInstanceTargets;
  }

  @Override
  @Nonnull
  public Set<Layer1Edge> getLayer1Edges() {
    if (_convertedConfiguration == null) {
      convertConfigurations();
    }
    return _convertedConfiguration.getLayer1Edges();
  }

  @Override
  public void setHostname(String hostname) {
    throw new IllegalStateException("Setting the hostname is not allowed for AWS configs");
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    throw new IllegalStateException("Setting the format is not allowed for AWS configs");
  }

  public static String vpnExternalInterfaceName(String tunnelId) {
    return "external-" + tunnelId;
  }

  public static String vpnTunnelId(String vpnConnectionId, int idNum) {
    return String.format("%s-%s", vpnConnectionId, idNum);
  }

  public static String vpnInterfaceName(String tunnelId) {
    return "vpn-" + tunnelId;
  }
}

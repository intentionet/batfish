package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.datamodel.NamedPort.EPHEMERAL_HIGHEST;
import static org.batfish.datamodel.NamedPort.EPHEMERAL_LOWEST;
import static org.batfish.representation.aws.AwsLocationInfoUtils.INFRASTRUCTURE_LOCATION_INFO;
import static org.batfish.representation.aws.Subnet.NLB_INSTANCE_TARGETS_IFACE_SUFFIX;
import static org.batfish.representation.aws.TargetGroup.Type.IP;
import static org.batfish.representation.aws.Utils.addNodeToSubnet;
import static org.batfish.representation.aws.Utils.checkNonNull;
import static org.batfish.representation.aws.Utils.createPublicIpsRefBook;
import static org.batfish.representation.aws.Utils.interfaceNameToRemote;
import static org.batfish.representation.aws.Utils.toStaticRoute;
import static org.batfish.specifier.Location.interfaceLinkLocation;
import static org.batfish.specifier.Location.interfaceLocation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.transformation.ApplyAll;
import org.batfish.datamodel.transformation.ApplyAny;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationStep;
import org.batfish.representation.aws.LoadBalancerListener.ActionType;
import org.batfish.representation.aws.LoadBalancerListener.DefaultAction;
import org.batfish.representation.aws.LoadBalancerListener.Listener;
import org.batfish.representation.aws.LoadBalancerTargetHealth.HealthState;
import org.batfish.representation.aws.LoadBalancerTargetHealth.TargetHealthDescription;

/**
 * Represents an elastic load balancer v2 https://docs.aws.amazon.com/elasticloadbalancing/.
 *
 * <p>We currently only support network load balancer (not application)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ParametersAreNonnullByDefault
public final class LoadBalancer implements AwsVpcEntity, Serializable {

  enum Scheme {
    INTERNAL,
    INTERNET_FACING
  }

  enum Type {
    APPLICATION,
    NETWORK
  }

  /** Protocols supported by elastic load balancers */
  public enum Protocol {
    HTTP,
    HTTPS,
    TCP,
    TLS,
    UDP,
    TCP_UDP
  }

  /** Regex for load balancer ARN. Used to find the interface in {@link #findMyInterface}. */
  static final Pattern LOAD_BALANCER_ARN_PATTERN =
      Pattern.compile("^arn:aws:elasticloadbalancing:[^:]+:[0-9]+:loadbalancer\\/(.+)$");

  /** The prefix is that used for interfaces that belong to load balancer */
  static final String LOAD_BALANCER_INTERFACE_DESCRIPTION_PREFIX = "ELB ";

  /** Name for the filter that permits only packets that will match a listener */
  static final String LISTENER_FILTER_NAME = "~LISTENER~FILTER~";

  /** Name for the filter that drops all packets that the load balancer could not forward */
  static final String FORWARDED_PACKETS_FILTER_NAME = "~FORWARDED~PACKET~FILTER~";

  /** We end the transformation chain with this */
  static final Transformation FINAL_TRANSFORMATION = null;

  @JsonIgnoreProperties(ignoreUnknown = true)
  @ParametersAreNonnullByDefault
  static class AvailabilityZone implements Serializable {

    @Nonnull private final String _subnetId;

    @Nonnull private final String _zoneName;

    @JsonCreator
    private static AvailabilityZone create(
        @Nullable @JsonProperty(JSON_KEY_SUBNET_ID) String subnetId,
        @Nullable @JsonProperty(JSON_KEY_ZONE_NAME) String zoneName) {
      // not parsing "LoadBalancerAddresses" -- assuming that network interfaces cover the info
      checkNonNull(subnetId, JSON_KEY_SUBNET_ID, "Load balancer availability zone");
      checkNonNull(zoneName, JSON_KEY_ZONE_NAME, "Load balancer availability zone");
      return new AvailabilityZone(subnetId, zoneName);
    }

    AvailabilityZone(String subnetId, String zoneName) {
      _subnetId = subnetId;
      _zoneName = zoneName;
    }

    @Nonnull
    public String getSubnetId() {
      return _subnetId;
    }

    @Nonnull
    public String getZoneName() {
      return _zoneName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof AvailabilityZone)) {
        return false;
      }
      AvailabilityZone that = (AvailabilityZone) o;
      return _subnetId.equals(that._subnetId) && _zoneName.equals(that._zoneName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_subnetId, _zoneName);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("_subnetId", _subnetId)
          .add("_zoneName", _zoneName)
          .toString();
    }
  }

  @Nonnull private String _arn;

  @Nonnull private final List<AvailabilityZone> _availabilityZones;

  @Nonnull private final String _dnsName;

  @Nonnull private final String _name;

  @Nonnull private final Scheme _scheme;

  @Nonnull private final Type _type;

  @Nonnull private final String _vpcId;

  @JsonCreator
  private static LoadBalancer create(
      @Nullable @JsonProperty(JSON_KEY_LOAD_BALANCER_ARN) String arn,
      @Nullable @JsonProperty(JSON_KEY_AVAILABILITY_ZONES) List<AvailabilityZone> availabilityZones,
      @Nullable @JsonProperty(JSON_KEY_DNS_NAME) String dnsName,
      @Nullable @JsonProperty(JSON_KEY_LOAD_BALANCER_NAME) String name,
      @Nullable @JsonProperty(JSON_KEY_SCHEME) String scheme,
      @Nullable @JsonProperty(JSON_KEY_TYPE) String type,
      @Nullable @JsonProperty(JSON_KEY_VPC_ID) String vpcId) {
    checkNonNull(arn, JSON_KEY_LOAD_BALANCER_ARN, "LoadBalancer");
    checkNonNull(availabilityZones, JSON_KEY_AVAILABILITY_ZONES, "LoadBalancer");
    checkNonNull(dnsName, JSON_KEY_DNS_NAME, "LoadBalancer");
    checkNonNull(name, JSON_KEY_LOAD_BALANCER_NAME, "LoadBalancer");
    checkNonNull(scheme, JSON_KEY_SCHEME, "LoadBalancer");
    checkNonNull(type, JSON_KEY_TYPE, "LoadBalancer");
    checkNonNull(vpcId, JSON_KEY_VPC_ID, "LoadBalancer");

    return new LoadBalancer(
        arn,
        availabilityZones,
        dnsName,
        name,
        Scheme.valueOf(scheme.toUpperCase().replace('-', '_')),
        Type.valueOf(type.toUpperCase()),
        vpcId);
  }

  public LoadBalancer(
      String arn,
      List<AvailabilityZone> availabilityZones,
      String dnsName,
      String name,
      Scheme scheme,
      Type type,
      String vpcId) {
    _arn = arn;
    _availabilityZones = availabilityZones;
    _dnsName = dnsName;
    _name = name;
    _scheme = scheme;
    _type = type;
    _vpcId = vpcId;
  }

  List<Configuration> toConfigurationNodes(
      ConvertedConfiguration awsConfiguration, Region region, Warnings warnings) {
    if (_type == Type.APPLICATION) {
      warnings.redFlag("Application load balancer is not currently supported");
      return ImmutableList.of();
    }
    List<Configuration> configurations = new LinkedList<>();
    _availabilityZones.forEach(
        zone -> configurations.add(toConfigurationNode(zone, awsConfiguration, region, warnings)));
    return configurations;
  }

  /** Creates configuration node corresponding to one availability zone */
  @VisibleForTesting
  Configuration toConfigurationNode(
      AvailabilityZone availabilityZone,
      ConvertedConfiguration awsConfiguration,
      Region region,
      Warnings warnings) {
    Configuration cfgNode =
        Utils.newAwsConfiguration(
            getNodeId(_dnsName, availabilityZone.getZoneName()),
            "aws",
            DeviceModel.AWS_ELB_NETWORK);
    cfgNode.setHumanName(_name);
    cfgNode.getVendorFamily().getAws().setVpcId(_vpcId);
    cfgNode.getVendorFamily().getAws().setSubnetId(availabilityZone.getSubnetId());
    cfgNode.getVendorFamily().getAws().setRegion(region.getName());

    // Add static routes to any instance targets
    getInstanceTargetStaticRoutes(
            awsConfiguration.getNlbsToInstanceTargets().get(_arn), availabilityZone.getSubnetId())
        .forEach(staticRoute -> Utils.addStaticRoute(cfgNode, staticRoute));

    Optional<NetworkInterface> networkInterface =
        findMyInterface(availabilityZone.getSubnetId(), _arn, region);
    if (!networkInterface.isPresent()) {
      warnings.redFlag(
          String.format(
              "Network interface not found for load balancer %s (%s) in subnet %s.",
              _name, _arn, availabilityZone.getSubnetId()));
      return cfgNode;
    }
    Subnet subnet = region.getSubnets().get(availabilityZone.getSubnetId());
    Interface viIface =
        addNodeToSubnet(cfgNode, networkInterface.get(), subnet, awsConfiguration, warnings);

    LoadBalancerAttributes loadBalancerAttributes = region.getLoadBalancerAttributes().get(_arn);
    if (loadBalancerAttributes == null) {
      warnings.redFlag(
          String.format(
              "Attributes not found for load balancer %s (%s). Assuming that cross zone load balancing is disabled.",
              _name, _arn));
    }
    boolean crossZoneLoadBalancing =
        loadBalancerAttributes != null && loadBalancerAttributes.getCrossZoneLoadBalancing();

    List<Listener> listeners = ImmutableList.of();
    LoadBalancerListener lbListener = region.getLoadBalancerListeners().get(_arn);
    if (lbListener != null) {
      listeners = lbListener.getListeners();
    } else {
      warnings.redFlag(
          String.format("Listeners not found for load balancer %s (%s).", _name, _arn));
    }

    IpAccessList incomingFilter = computeListenerFilter(listeners);
    viIface.setIncomingFilter(incomingFilter);
    cfgNode.getIpAccessLists().put(incomingFilter.getName(), incomingFilter);

    installTransformations(
        viIface,
        availabilityZone.getZoneName(),
        crossZoneLoadBalancing,
        listeners,
        region,
        warnings);

    IpAccessList defaultFilter =
        computeNotForwardedFilter(networkInterface.get().getPrivateIpAddresses());
    viIface.setPostTransformationIncomingFilter(defaultFilter);
    cfgNode.getIpAccessLists().put(defaultFilter.getName(), defaultFilter);

    createPublicIpsRefBook(Collections.singleton(networkInterface.get()), cfgNode);

    // Create LocationInfo the interface
    cfgNode.setLocationInfo(
        ImmutableMap.of(
            interfaceLocation(viIface),
            INFRASTRUCTURE_LOCATION_INFO,
            interfaceLinkLocation(viIface),
            INFRASTRUCTURE_LOCATION_INFO));

    return cfgNode;
  }

  /**
   * Creates static routes to the IP of each of the given {@code instanceTargets} via subnet {@code
   * lbSubnetId} (that is, using an interface to that subnet's instance target VRF as their next
   * hop). The instance targets will be in the load balancer's VPC, but may not be in the given
   * subnet's availability zone.
   */
  @VisibleForTesting
  static List<StaticRoute> getInstanceTargetStaticRoutes(
      Collection<Instance> instanceTargets, String lbSubnetId) {
    return instanceTargets.stream()
        .map(
            instance -> {
              String subnetCfgHostname = Subnet.nodeName(lbSubnetId);
              String instanceTargetsIface =
                  interfaceNameToRemote(subnetCfgHostname, NLB_INSTANCE_TARGETS_IFACE_SUFFIX);
              // Guaranteed by isTargetInAnyEnabledAvailabilityZone
              assert instance.getPrimaryPrivateIpAddress() != null;
              return toStaticRoute(
                  instance.getPrimaryPrivateIpAddress().toPrefix(),
                  instanceTargetsIface,
                  AwsConfiguration.LINK_LOCAL_IP);
            })
        .collect(ImmutableList.toImmutableList());
  }

  @VisibleForTesting
  void installTransformations(
      Interface viIface,
      String lbAvailabilityZoneName,
      boolean crossZoneLoadBalancing,
      List<Listener> listeners,
      Region region,
      Warnings warnings) {
    if (viIface.getConcreteAddress() != null) { // May be null if we couldn't find a usable address
      List<LoadBalancerTransformation> listenerTransformations =
          listeners.stream()
              .map(
                  listener ->
                      computeListenerTransformation(
                          listener,
                          lbAvailabilityZoneName,
                          viIface.getConcreteAddress().getIp(),
                          crossZoneLoadBalancing,
                          region,
                          warnings))
              .filter(Objects::nonNull)
              .collect(ImmutableList.toImmutableList());

      viIface.setIncomingTransformation(chainListenerTransformations(listenerTransformations));
    }
    viIface.setFirewallSessionInterfaceInfo(
        new FirewallSessionInterfaceInfo(false, ImmutableList.of(viIface.getName()), null, null));
  }

  /**
   * Gets {@link LoadBalancerTransformation} corresponding to the given {@code listener}. The
   * listener has match conditions and a sequence of actions. The first valid action of type forward
   * is used for the returned transformation.
   *
   * @return null if we cannot build a valid transformation, e.g., the listener protocol for
   *     incoming packets is not supported or none of the targets are valid.
   */
  @Nullable
  @VisibleForTesting
  LoadBalancerTransformation computeListenerTransformation(
      LoadBalancerListener.Listener listener,
      String lbAvailabilityZoneName,
      Ip loadBalancerIp,
      boolean crossZoneLoadBalancing,
      Region region,
      Warnings warnings) {
    try {
      HeaderSpace matchHeaderSpace = listener.getMatchingHeaderSpace();

      Optional<DefaultAction> forwardingAction =
          listener.getDefaultActions().stream()
              .filter(defaultAction -> defaultAction.getType() == ActionType.FORWARD)
              .findFirst();
      if (!forwardingAction.isPresent()) {
        warnings.redFlag(
            String.format(
                "No forwarding action found for listener %s of load balancer %s (%s)",
                listener, _arn, _name));
        return null;
      }

      TransformationStep transformationStep =
          computeTargetGroupTransformationStep(
              forwardingAction.get().getTargetGroupArn(),
              lbAvailabilityZoneName,
              loadBalancerIp,
              crossZoneLoadBalancing,
              region,
              warnings);

      if (transformationStep == null) {
        // no need to warn here. computerTargetGroupTransformationStep does it
        return null;
      }

      return new LoadBalancerTransformation(
          new MatchHeaderSpace(matchHeaderSpace), transformationStep);
    } catch (Exception e) {
      warnings.redFlag(e.getMessage());
      return null;
    }
  }

  /**
   * Returns the set of active {@link TargetHealthDescription} for the given {@link
   * LoadBalancerTargetHealth}, and optionally files warnings. Note that requests are normally only
   * routed to healthy targets, but if there are no healthy targets, requests will be routed to all
   * targets in enabled availability zones:
   * https://docs.aws.amazon.com/elasticloadbalancing/latest/network/target-group-health-checks.html
   */
  @Nonnull
  static Set<TargetHealthDescription> getActiveTargets(
      LoadBalancerTargetHealth targetHealth,
      TargetGroup targetGroup,
      Set<String> lbAvailabilityZones,
      boolean crossZoneLoadBalancing,
      Region region,
      boolean fileWarnings,
      @Nullable Warnings warnings) {
    checkArgument(
        !fileWarnings || warnings != null, "Can't file warnings because warnings is null");

    Set<TargetHealthDescription> enabledTargets =
        targetHealth.getTargetHealthDescriptions().stream()
            .filter(
                desc ->
                    isTargetInAnyEnabledAvailabilityZone(
                        desc,
                        targetGroup.getTargetType(),
                        lbAvailabilityZones,
                        crossZoneLoadBalancing,
                        region))
            .collect(ImmutableSet.toImmutableSet());
    if (enabledTargets.isEmpty()) {
      if (fileWarnings) {
        warnings.redFlag(
            String.format(
                "No targets found in enabled availability zone(s) for target group ARN %s",
                targetGroup.getId()));
      }
      return ImmutableSet.of();
    }

    Set<TargetHealthDescription> healthyTargets =
        enabledTargets.stream()
            .filter(desc -> desc.getTargetHealth().getState() == HealthState.HEALTHY)
            .collect(ImmutableSet.toImmutableSet());

    // https://docs.aws.amazon.com/elasticloadbalancing/latest/network/target-group-health-checks.html
    // If there are no enabled Availability Zones with a healthy target in each target group,
    // requests are routed to targets in all enabled Availability Zones.
    return healthyTargets.isEmpty() ? enabledTargets : healthyTargets;
  }

  /**
   * Gets {@link TransformationStep} for a {@link TargetGroup}. The load balancer sprays packets
   * across valid targets in the group. Validity is based on target health and zone criteria.
   *
   * @return null if a valid transformation step could not be constructed
   */
  @Nullable
  @VisibleForTesting
  static TransformationStep computeTargetGroupTransformationStep(
      String targetGroupArn,
      String lbAvailabilityZoneName,
      Ip loadBalancerIp,
      boolean crossZoneLoadBalancing,
      Region region,
      Warnings warnings) {
    TargetGroup targetGroup = region.getTargetGroups().get(targetGroupArn);
    if (targetGroup == null) {
      warnings.redFlag(String.format("Target group ARN %s not found", targetGroupArn));
      return null;
    }

    LoadBalancerTargetHealth targetHealths =
        region.getLoadBalancerTargetHealths().get(targetGroupArn);
    if (targetHealths == null) {
      warnings.redFlag(
          String.format(
              "Target health information not found for target group ARN %s", targetGroupArn));
      return null;
    }

    Set<TargetHealthDescription> activeTargets =
        getActiveTargets(
            targetHealths,
            targetGroup,
            ImmutableSet.of(lbAvailabilityZoneName),
            crossZoneLoadBalancing,
            region,
            true,
            warnings);

    Set<TransformationStep> transformationSteps =
        activeTargets.stream()
            .map(
                desc ->
                    computeTargetTransformationStep(
                        desc.getTarget(),
                        targetGroup.getTargetType(),
                        loadBalancerIp,
                        region,
                        warnings))
            .filter(Objects::nonNull)
            .collect(ImmutableSet.toImmutableSet());

    if (transformationSteps.isEmpty()) {
      // warning logged by computeTargetTransformationStep
      return null;
    }

    return new ApplyAny(transformationSteps);
  }

  /**
   * Gets the target IP for the given {@link LoadBalancerTarget}. Assumes that target validity has
   * already been checked with {@link #isTargetInAnyEnabledAvailabilityZone(TargetHealthDescription,
   * TargetGroup.Type, Set, boolean, Region) isTargetInAnyEnabledAvailabilityZone}.
   */
  @Nullable
  static Ip getTargetIp(
      LoadBalancerTarget target, TargetGroup.Type targetGroupType, Region region) {
    return targetGroupType.equals(IP)
        ? Ip.parse(target.getId())
        // instance must exist since this target is valid (see isTargetInAnyEnabledAvailabilityZone)
        : region.getInstances().get(target.getId()).getPrimaryPrivateIpAddress();
  }

  /**
   * Returns the transformation step corresponding to {@code target}. This method assumes that the
   * target is valid.
   *
   * @return The transformation step or null if the Ip of the target cannot be determined.
   */
  @Nullable
  @VisibleForTesting
  static TransformationStep computeTargetTransformationStep(
      LoadBalancerTarget target,
      TargetGroup.Type targetGroupType,
      Ip loadBalancerIp,
      Region region,
      Warnings warnings) {
    Ip targetIp = getTargetIp(target, targetGroupType, region);
    if (targetIp == null) {
      warnings.redFlag(String.format("Could not determine IP for load balancer target %s", target));
      return null;
    }
    TransformationStep transformDstIp = TransformationStep.assignDestinationIp(targetIp, targetIp);
    TransformationStep transformDstPort =
        TransformationStep.assignDestinationPort(target.getPort(), target.getPort());
    switch (targetGroupType) {
      case INSTANCE:
        // No source NAT for instance targets
        return new ApplyAll(transformDstIp, transformDstPort);
      case IP:
        return new ApplyAll(
            TransformationStep.assignSourceIp(loadBalancerIp, loadBalancerIp),
            TransformationStep.assignSourcePort(
                EPHEMERAL_LOWEST.number(), EPHEMERAL_HIGHEST.number()),
            transformDstIp,
            transformDstPort);
      default:
        throw new IllegalArgumentException(
            String.format("Unrecognized target group type %s", targetGroupType));
    }
  }

  /**
   * A target is in some enabled zone for this load balancer if the load balancer does cross zone
   * load balancing; or if the target is either in zone "all" or in any of the {@code
   * lbAvailabilityZones}.
   */
  @VisibleForTesting
  static boolean isTargetInAnyEnabledAvailabilityZone(
      TargetHealthDescription targetHealthDescription,
      TargetGroup.Type targetType,
      Set<String> lbAvailabilityZones,
      boolean crossZoneLoadBalancing,
      Region region) {
    switch (targetType) {
      case IP:
        return crossZoneLoadBalancing
            || "all".equals(targetHealthDescription.getTarget().getAvailabilityZone())
            || lbAvailabilityZones.contains(
                targetHealthDescription.getTarget().getAvailabilityZone());
      case INSTANCE:
        Instance instance = region.getInstances().get(targetHealthDescription.getTarget().getId());
        if (instance == null) {
          return false;
        }
        Subnet subnet = region.getSubnets().get(instance.getSubnetId());
        return subnet != null
            && (crossZoneLoadBalancing
                || lbAvailabilityZones.contains(subnet.getAvailabilityZone()));
      default:
        throw new IllegalArgumentException("Unknown target group type " + targetType);
    }
  }

  /** Chains the provided list of transformations. */
  @Nonnull
  @VisibleForTesting
  static Transformation chainListenerTransformations(
      List<LoadBalancerTransformation> listenerTransformations) {
    Transformation tailTransformation = FINAL_TRANSFORMATION;
    for (int index = listenerTransformations.size() - 1; index >= 0; index--) {
      tailTransformation = listenerTransformations.get(index).toTransformation(tailTransformation);
    }
    return tailTransformation;
  }

  /**
   * Returns a filter that permits packets for listeners with a forwarding action and denies all
   * packets that do not match a listener or match a non-forwarding listener
   */
  @VisibleForTesting
  static IpAccessList computeListenerFilter(List<Listener> listeners) {
    List<AclLine> aclLines =
        listeners.stream()
            // consider only listeners with a forwarding action
            .filter(
                listener ->
                    listener.getDefaultActions().stream()
                        .anyMatch(action -> action.getType() == ActionType.FORWARD))
            .map(
                listener ->
                    ExprAclLine.builder()
                        .setTraceElement(getTraceElementForMatchedListener(listener.getId()))
                        .setMatchCondition(new MatchHeaderSpace(listener.getMatchingHeaderSpace()))
                        .setAction(LineAction.PERMIT)
                        .setName("Listener " + listener.getId())
                        .build())
            .collect(ImmutableList.toImmutableList());
    return IpAccessList.builder()
        .setName(LISTENER_FILTER_NAME)
        .setLines(
            new ImmutableList.Builder<AclLine>()
                .addAll(aclLines)
                .add(
                    ExprAclLine.builder()
                        .setTraceElement(getTraceElementForNoMatchedListener())
                        .setMatchCondition(TrueExpr.INSTANCE)
                        .setAction(LineAction.DENY)
                        .setName("Default deny")
                        .build())
                .build())
        .build();
  }

  @VisibleForTesting
  static TraceElement getTraceElementForMatchedListener(String listenerArn) {
    return TraceElement.of("Matched listener " + listenerArn);
  }

  @VisibleForTesting
  static TraceElement getTraceElementForNoMatchedListener() {
    return TraceElement.of("Did not match any forwarding listeners");
  }

  /**
   * The computed filter rejects all packets that match the network interface's addresses. The idea
   * is that all packets addressed to the load balancer (whose interface is provided as an argument)
   * are dropped if they were not transformed and thus not forwarded. We are relying on the fact
   * that all successfully transformed packets have an IP address different from that of the load
   * balancer.
   */
  @VisibleForTesting
  static IpAccessList computeNotForwardedFilter(
      List<PrivateIpAddress> loadBalancerInterfaceAddresses) {
    return IpAccessList.builder()
        .setName(FORWARDED_PACKETS_FILTER_NAME)
        .setLines(
            ExprAclLine.builder()
                // since we have a filter that permits only packets for valid listeners, all
                // untransformed packets are those for which we didn't find a valid target
                .setTraceElement(getTraceElementForNotForwardedPackets())
                .setMatchCondition(
                    new MatchHeaderSpace(
                        HeaderSpace.builder()
                            .setDstIps(
                                loadBalancerInterfaceAddresses.stream()
                                    .map(privateIp -> IpWildcard.create(privateIp.getPrivateIp()))
                                    .collect(ImmutableList.toImmutableList()))
                            .build()))
                .setAction(LineAction.DENY)
                .setName("Deny packets without a valid target")
                .build(),
            ExprAclLine.builder()
                .setTraceElement(getTraceElementForForwardedPackets())
                .setMatchCondition(TrueExpr.INSTANCE)
                .setAction(LineAction.PERMIT)
                .setName("Permit packets with valid targets")
                .build())
        .build();
  }

  @VisibleForTesting
  static TraceElement getTraceElementForForwardedPackets() {
    return TraceElement.of("Forwarded to a target");
  }

  @VisibleForTesting
  static TraceElement getTraceElementForNotForwardedPackets() {
    return TraceElement.of("No valid (healthy, within availability zone) target found");
  }

  /**
   * Finds the interface that the load balancer uses inside the given subnet.
   *
   * <p>Example arn is
   * 'arn:aws:elasticloadbalancing:us-east-2:554773406868:loadbalancer/net/lb-lb/6f57a43b75d8f2c1'.
   * The regex extracts 'net/lb-lb/6f57a43b75d8f2c1' and uses that to join with interface
   * descriptions which are like 'ELB net/lb-lb/6f57a43b75d8f2c1'.
   * https://docs.aws.amazon.com/elasticloadbalancing/latest/network/network-load-balancers.html#availability-zones
   */
  @VisibleForTesting
  static Optional<NetworkInterface> findMyInterface(String subnetId, String arn, Region region) {
    Matcher matcher = LOAD_BALANCER_ARN_PATTERN.matcher(arn);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    String matchString = matcher.group(1);
    return region.getNetworkInterfaces().values().stream()
        .filter(
            iface ->
                iface
                        .getDescription()
                        .equals(LOAD_BALANCER_INTERFACE_DESCRIPTION_PREFIX + matchString)
                    && iface.getSubnetId().equals(subnetId))
        .findAny();
  }

  static String getNodeId(String dnsName, String availabilityZoneName) {
    return String.format("%s-%s", availabilityZoneName, dnsName);
  }

  @Override
  public String getId() {
    return _arn;
  }

  @Nonnull
  public List<AvailabilityZone> getAvailabilityZones() {
    return _availabilityZones;
  }

  @Nonnull
  public String getDnsName() {
    return _dnsName;
  }

  @Nonnull
  public String getName() {
    return _name;
  }

  @Nonnull
  public Scheme getScheme() {
    return _scheme;
  }

  @Nonnull
  public Type getType() {
    return _type;
  }

  @Nonnull
  public String getVpcId() {
    return _vpcId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LoadBalancer)) {
      return false;
    }
    LoadBalancer that = (LoadBalancer) o;
    return _arn.equals(that._arn)
        && _availabilityZones.equals(that._availabilityZones)
        && Objects.equals(_dnsName, that._dnsName)
        && Objects.equals(_name, that._name)
        && _scheme == that._scheme
        && _type == that._type
        && _vpcId.equals(that._vpcId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_arn, _availabilityZones, _dnsName, _name, _scheme, _type, _vpcId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("_arn", _arn)
        .add("_availabilityZones", _availabilityZones)
        .add("_dnsName", _dnsName)
        .add("_name", _name)
        .add("_scheme", _scheme)
        .add("_type", _type)
        .add("_vpcId", _vpcId)
        .toString();
  }
}

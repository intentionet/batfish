package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.vendor_family.AwsFamily;

/** A collection for utilities for AWS vendor model */
@ParametersAreNonnullByDefault
final class Utils {

  private static final NetworkFactory FACTORY = new NetworkFactory();

  static void checkNonNull(@Nullable Object value, String fieldName, String objectType) {
    if (value == null) {
      throw new IllegalArgumentException(
          String.format("Field '%s' must exist for '%s", fieldName, objectType));
    }
  }

  static Configuration newAwsConfiguration(String name, String domainName) {
    Configuration c =
        FACTORY
            .configurationBuilder()
            .setHostname(name)
            .setDomainName(domainName)
            .setConfigurationFormat(ConfigurationFormat.AWS)
            .setDefaultInboundAction(LineAction.PERMIT)
            .setDefaultCrossZoneAction(LineAction.PERMIT)
            .build();
    FACTORY.vrfBuilder().setName(Configuration.DEFAULT_VRF_NAME).setOwner(c).build();
    c.getVendorFamily().setAws(new AwsFamily());
    return c;
  }

  static Interface newInterface(
      String name, Configuration c, ConcreteInterfaceAddress primaryAddress, String description) {
    return FACTORY
        .interfaceBuilder()
        .setName(name)
        .setOwner(c)
        .setVrf(c.getDefaultVrf())
        .setAddress(primaryAddress)
        .setDescription(description)
        .build();
  }

  /**
   * Updates {@link Region}'s mapping between {@link Configuration} names and {@link SecurityGroup}
   * for a given configuration. Also updates {@link org.batfish.datamodel.Ip} of instances in {@link
   * SecurityGroup}
   *
   * @param region {@link Region} in which the configuration is in
   * @param configuration {@link Configuration} for which security groups are to be processed
   * @param securityGroupsIds {@link List} of security group IDs
   * @param warnings {@link Warnings} for the configuration
   */
  static void processSecurityGroups(
      Region region,
      Configuration configuration,
      List<String> securityGroupsIds,
      Warnings warnings) {
    for (String sGroupId : securityGroupsIds) {
      SecurityGroup securityGroup = region.getSecurityGroups().get(sGroupId);
      if (securityGroup == null) {
        warnings.pedantic(
            String.format(
                "Security group \"%s\" for \"%s\" not found",
                sGroupId, configuration.getHostname()));
        continue;
      }
      region.updateConfigurationSecurityGroups(configuration.getHostname(), securityGroup);

      securityGroup.updateConfigIps(configuration);
    }
  }

  @Nullable
  static IpProtocol toIpProtocol(String ipProtocolAsString) {
    switch (ipProtocolAsString) {
      case "tcp":
        return IpProtocol.TCP;
      case "udp":
        return IpProtocol.UDP;
      case "icmp":
        return IpProtocol.ICMP;
      case "-1":
        return null;
      default:
        try {
          int ipProtocolAsInt = Integer.parseInt(ipProtocolAsString);
          return IpProtocol.fromNumber(ipProtocolAsInt);
        } catch (NumberFormatException e) {
          throw new BatfishException(
              "Could not convert AWS IP protocol: \""
                  + ipProtocolAsString
                  + "\" to batfish Ip Protocol",
              e);
        }
    }
  }

  /** Adds a static route on {@code cfgNode} */
  static void addStaticRoute(Configuration cfgNode, StaticRoute staticRoute) {
    cfgNode.getDefaultVrf().getStaticRoutes().add(staticRoute);
  }

  @Nonnull
  static StaticRoute toStaticRoute(Ip targetIp, Ip nextHopIp) {
    return toStaticRoute(Prefix.create(targetIp, Prefix.MAX_PREFIX_LENGTH), nextHopIp);
  }

  @Nonnull
  static StaticRoute toStaticRoute(Prefix targetPrefix, Ip nextHopIp) {
    return StaticRoute.builder()
        .setNetwork(targetPrefix)
        .setNextHopIp(nextHopIp)
        .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
        .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
        .build();
  }

  @Nonnull
  static StaticRoute toStaticRoute(Ip targetIp, Interface nextHopInterface) {
    return toStaticRoute(Prefix.create(targetIp, Prefix.MAX_PREFIX_LENGTH), nextHopInterface);
  }

  @Nonnull
  static StaticRoute toStaticRoute(Prefix targetPrefix, Interface nextHopInterface) {
    return StaticRoute.builder()
        .setNetwork(targetPrefix)
        .setNextHopInterface(nextHopInterface.getName())
        .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
        .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
        .build();
  }

  /**
   * Creates a subnet link between the two nodes represented by {@code cfgNode1} and {@code
   * cfgNode2}. Create a new interface on each node for this purpose and assigns it a name that
   * corresponds to the name of the other node
   */
  static void connect(
      AwsConfiguration awsConfiguration, Configuration cfgNode1, Configuration cfgNode2) {
    Prefix linkPrefix = awsConfiguration.getNextGeneratedLinkSubnet();
    ConcreteInterfaceAddress ifaceAddress1 =
        ConcreteInterfaceAddress.create(linkPrefix.getStartIp(), linkPrefix.getPrefixLength());
    ConcreteInterfaceAddress ifaceAddress2 =
        ConcreteInterfaceAddress.create(linkPrefix.getEndIp(), linkPrefix.getPrefixLength());

    String ifaceName1 = cfgNode2.getHostname();
    Utils.newInterface(ifaceName1, cfgNode1, ifaceAddress1, "To " + ifaceName1);

    String ifaceName2 = cfgNode1.getHostname();
    Utils.newInterface(ifaceName2, cfgNode2, ifaceAddress2, "To " + ifaceName2);
  }

  /**
   * Returns the IP address of the interface with name {@code ifaceName} in {@code configuration}.
   * Throws an exception if the interface is not present or does not have an assigned address
   */
  @Nonnull
  static Ip getInterfaceIp(Configuration configuration, String ifaceName) {
    Interface iface = configuration.getAllInterfaces().get(ifaceName);
    checkArgument(
        iface != null,
        "Interface name '%s' not found on node %s",
        ifaceName,
        configuration.getHostname());
    checkArgument(
        iface.getConcreteAddress() != null,
        "Concrete address for interface name '%s' on node %s is null",
        ifaceName,
        configuration);

    return iface.getConcreteAddress().getIp();
  }

  private Utils() {}
}

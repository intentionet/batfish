package org.batfish.representation.aws;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.representation.aws.AwsConfiguration.LINK_LOCAL_IP;
import static org.batfish.representation.aws.AwsVpcEntity.TAG_NAME;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LinkLocalAddress;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.vendor_family.AwsFamily;
import org.batfish.representation.aws.IpPermissions.AddressType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** A collection for utilities for AWS vendor model */
@ParametersAreNonnullByDefault
final class Utils {

  static final Statement ACCEPT_ALL_BGP =
      new If(
          new MatchProtocol(RoutingProtocol.BGP),
          ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
          ImmutableList.of(Statements.ExitReject.toStaticStatement()));

  static final Statement ACCEPT_ALL_BGP_AND_STATIC =
      new If(
          new Disjunction(
              new MatchProtocol(RoutingProtocol.BGP), new MatchProtocol(RoutingProtocol.STATIC)),
          ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
          ImmutableList.of(Statements.ExitReject.toStaticStatement()));

  static void checkNonNull(@Nullable Object value, String fieldName, String objectType) {
    if (value == null) {
      throw new IllegalArgumentException(
          String.format("Field '%s' must exist for '%s", fieldName, objectType));
    }
  }

  static Configuration newAwsConfiguration(String name, String domainName) {
    return newAwsConfiguration(name, domainName, Collections.emptyMap());
  }

  static Configuration newAwsConfiguration(
      String name, String domainName, Map<String, String> tags) {
    Configuration c =
        Configuration.builder()
            .setHostname(name)
            .setDomainName(domainName)
            .setConfigurationFormat(ConfigurationFormat.AWS)
            .setDefaultInboundAction(LineAction.PERMIT)
            .setDefaultCrossZoneAction(LineAction.PERMIT)
            .setHumanName(tags.get(TAG_NAME))
            .build();
    Vrf.builder().setName(Configuration.DEFAULT_VRF_NAME).setOwner(c).build();
    c.getVendorFamily().setAws(new AwsFamily());
    return c;
  }

  /** Creates a new interface on {@code c} with the provided name, address, and description. */
  static Interface newInterface(
      String name, Configuration c, InterfaceAddress primaryAddress, String description) {
    return newInterface(name, c, c.getDefaultVrf().getName(), primaryAddress, description);
  }

  /**
   * Creates a new interface on {@code c} in the provided VRF name with the provided name, address,
   * and description.
   */
  static Interface newInterface(
      String name,
      Configuration c,
      String vrfName,
      InterfaceAddress primaryAddress,
      String description) {
    checkArgument(
        c.getVrfs().containsKey(vrfName), "VRF %s does not exist on %s", vrfName, c.getHostname());
    return Interface.builder()
        .setName(name)
        .setOwner(c)
        .setVrf(c.getVrfs().get(vrfName))
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

  /** Adds a static route on {@code vrf} */
  static void addStaticRoute(Vrf vrf, StaticRoute staticRoute) {
    vrf.getStaticRoutes().add(staticRoute);
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
  static StaticRoute toStaticRoute(Prefix targetPrefix, String nextHopInterfaceName) {
    return StaticRoute.builder()
        .setNetwork(targetPrefix)
        .setNextHopInterface(nextHopInterfaceName)
        .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
        .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
        .build();
  }

  @Nonnull
  static StaticRoute toStaticRoute(Prefix targetPrefix, String nextHopInterfaceName, Ip nextHopIp) {
    return StaticRoute.builder()
        .setNetwork(targetPrefix)
        .setNextHopInterface(nextHopInterfaceName)
        .setNextHopIp(nextHopIp)
        .setAdministrativeCost(Route.DEFAULT_STATIC_ROUTE_ADMIN)
        .setMetric(Route.DEFAULT_STATIC_ROUTE_COST)
        .build();
  }

  /**
   * Creates a subnet link between the two nodes represented by {@code cfgNode1} and {@code
   * cfgNode2}. Create a new interface on each node in the supplied VRF names and assigns it a name
   * that corresponds to the name of the other node plus {@code ifaceNameSuffix}
   */
  static void connect(
      ConvertedConfiguration awsConfiguration,
      Configuration cfgNode1,
      String vrfName1,
      Configuration cfgNode2,
      String vrfName2,
      String ifaceNameSuffix) {
    String ifaceName1 = interfaceNameToRemote(cfgNode2, ifaceNameSuffix);
    Utils.newInterface(
        ifaceName1, cfgNode1, vrfName1, LinkLocalAddress.of(LINK_LOCAL_IP), "To " + ifaceName1);

    String ifaceName2 = interfaceNameToRemote(cfgNode1, ifaceNameSuffix);
    Utils.newInterface(
        ifaceName2, cfgNode2, vrfName2, LinkLocalAddress.of(LINK_LOCAL_IP), "To " + ifaceName2);

    addLayer1Edge(
        awsConfiguration, cfgNode1.getHostname(), ifaceName1, cfgNode2.getHostname(), ifaceName2);
  }

  /**
   * Creates a subnet link between the two nodes represented by {@code cfgNode1} and {@code
   * cfgNode2}. Create a new interface on each node for this purpose and assigns it a name that
   * corresponds to the name of the other node.
   */
  static void connect(
      ConvertedConfiguration awsConfiguration, Configuration cfgNode1, Configuration cfgNode2) {
    connect(
        awsConfiguration,
        cfgNode1,
        cfgNode1.getDefaultVrf().getName(),
        cfgNode2,
        cfgNode2.getDefaultVrf().getName(),
        "");
  }

  static String interfaceNameToRemote(Configuration remoteCfg) {
    return interfaceNameToRemote(remoteCfg, "");
  }

  static String interfaceNameToRemote(Configuration remoteCfg, String suffix) {
    return suffix.isEmpty() ? remoteCfg.getHostname() : remoteCfg.getHostname() + "-" + suffix;
  }

  /** Adds a bidirectional layer1 edge between the interfaces and nodes */
  static void addLayer1Edge(
      ConvertedConfiguration awsConfiguration,
      String node1,
      String iface1,
      String node2,
      String iface2) {
    awsConfiguration.addEdge(node1, iface1, node2, iface2);
    awsConfiguration.addEdge(node2, iface2, node1, iface1);
  }

  private static InterfaceAddress getInterfaceAddress(
      Configuration configuration, String ifaceName) {
    Interface iface = configuration.getAllInterfaces().get(ifaceName);
    checkArgument(
        iface != null,
        "Interface name '%s' not found on node %s",
        ifaceName,
        configuration.getHostname());
    return iface.getAddress();
  }

  /**
   * Returns the IP address of the interface with name {@code ifaceName} in {@code configuration}.
   * Throws an exception if the interface is not present or does not have an assigned address
   */
  @Nonnull
  static Ip getInterfaceLinkLocalIp(Configuration configuration, String ifaceName) {
    InterfaceAddress ifaceAddress = getInterfaceAddress(configuration, ifaceName);
    if (ifaceAddress instanceof LinkLocalAddress) {
      return ((LinkLocalAddress) ifaceAddress).getIp();
    }
    throw new IllegalArgumentException(
        String.format(
            "Interface %s on %s does not have a link local address",
            ifaceName, configuration.getHostname()));
  }

  /** Extracts the text content of the first element with {@code tag} within {@code element}. */
  static String textOfFirstXmlElementWithTag(Element element, String tag) {
    NodeList nodes = element.getElementsByTagName(tag);
    checkArgument(nodes.getLength() > 0, "Tag '%s' not found", tag);
    return nodes.item(0).getTextContent();
  }

  /**
   * Extracts the text content of the first element with {@code innerTag} within the first element
   * with {@code outerTag} within {@code element}.
   */
  static String textOfFirstXmlElementWithInnerTag(
      Element element, String outerTag, String innerTag) {
    NodeList outerNodes = element.getElementsByTagName(outerTag);
    checkArgument(outerNodes.getLength() > 0, "OuterTag '%s' not found", outerTag);
    return textOfFirstXmlElementWithTag((Element) outerNodes.item(0), innerTag);
  }

  public static TraceElement getTraceElementForRule(@Nullable String ruleDescription) {
    if (ruleDescription == null) {
      return TraceElement.of("Matched rule with no description");
    }
    return TraceElement.of(String.format("Matched rule with description %s", ruleDescription));
  }

  public static TraceElement getTraceElementForSecurityGroup(String securityGroupName) {
    return TraceElement.of(String.format("Matched security group %s", securityGroupName));
  }

  static TraceElement traceElementForAddress(
      String direction, String vsAddressStructure, AddressType addressType) {
    return TraceElement.of(traceTextForAddress(direction, vsAddressStructure, addressType));
  }

  static String traceTextForAddress(
      String direction, String vsAddressStructure, AddressType addressType) {
    return String.format("Matched %s address %s %s", direction, addressType, vsAddressStructure);
  }

  static TraceElement traceElementForProtocol(IpProtocol protocol) {
    return TraceElement.of(String.format("Matched protocol %s", protocol));
  }

  static TraceElement traceElementForDstPorts(int low, int high) {
    if (low == high) {
      return TraceElement.of(String.format("Matched destination port %s", low));
    }
    return TraceElement.of(String.format("Matched destination ports [%s-%s]", low, high));
  }

  static TraceElement traceElementForInstance(String instanceName) {
    return TraceElement.of(String.format("Matched instance %s", instanceName));
  }

  static TraceElement traceElementForIcmp(int type, int code) {
    assert type != -1;
    TraceElement.Builder treBuilder =
        TraceElement.builder().add(String.format("Matched ICMP type %s", type));
    if (code != -1) {
      treBuilder.add(String.format("Matched ICMP code %s", code));
    }
    return treBuilder.build();
  }

  private Utils() {}
}

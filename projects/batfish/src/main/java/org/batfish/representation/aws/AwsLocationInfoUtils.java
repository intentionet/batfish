package org.batfish.representation.aws;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.specifier.LocationInfoUtils.connectedHostSubnetHostIps;

import com.google.common.collect.ImmutableList;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.specifier.LocationInfo;

/** Helpers for defining AWS-specific {@link LocationInfo}. */
public final class AwsLocationInfoUtils {
  private AwsLocationInfoUtils() {}

  private static IpSpace configuredIps(Interface iface) {
    return firstNonNull(
        AclIpSpace.union(
            iface.getAllConcreteAddresses().stream()
                .map(ConcreteInterfaceAddress::getIp)
                .map(Ip::toIpSpace)
                .collect(ImmutableList.toImmutableList())),
        EmptyIpSpace.INSTANCE);
  }

  static LocationInfo instanceInterfaceLocationInfo(Interface iface) {
    return new LocationInfo(
        true, // use as a traffic source
        configuredIps(iface), // use configured IPs for source IP by default
        EmptyIpSpace.INSTANCE); // interface locations do not have external ARP IPs
  }

  static final LocationInfo INSTANCE_INTERFACE_LINK_LOCATION_INFO =
      new LocationInfo(
          false, // do not use as a traffic source
          EmptyIpSpace.INSTANCE,
          EmptyIpSpace.INSTANCE);

  static LocationInfo subnetInterfaceLocationInfo(Interface iface) {
    return new LocationInfo(
        // infrastructure interface; not a source
        false,
        // if user explicitly selects this location to be a source, use
        // its configured IPs for source IPs by default
        configuredIps(iface),
        // interface locations never have external ARP IPs
        EmptyIpSpace.INSTANCE);
  }

  static LocationInfo subnetInterfaceLinkLocationInfo(Interface iface) {
    return new LocationInfo(
        // not a source of traffic
        false,
        // if user explicitly selects this location to be a source, use
        // these source IPs by default
        connectedHostSubnetHostIps(iface),
        // no external ARP replies
        EmptyIpSpace.INSTANCE);
  }
}

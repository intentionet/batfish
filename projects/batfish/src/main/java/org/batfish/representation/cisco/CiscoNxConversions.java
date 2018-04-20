package org.batfish.representation.cisco;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.representation.cisco.nx.CiscoNxBgpVrfConfiguration;

/**
 * A utility class for converting between Cisco NX-OS configurations and the Batfish
 * vendor-independent {@link org.batfish.datamodel}.
 */
final class CiscoNxConversions {
  /** Computes the router ID on Cisco NX-OS. */
  // See CiscoNxosTest#testRouterId for a test that is verifiable using GNS3.
  @Nonnull
  static Ip getNxBgpRouterId(
      Configuration c, CiscoNxBgpVrfConfiguration vrfConfig, org.batfish.datamodel.Vrf vrf) {
    // If Router ID is configured in the VRF-Specific BGP config, it wins.
    Ip routerId = vrfConfig.getRouterId();
    if (routerId != null) {
      return routerId;
    }

    // Otherwise, Router ID is defined based on the interfaces in the VRF that have IP addresses.
    Map<String, Interface> interfaceMap =
        vrf.getInterfaces()
            .entrySet()
            .stream()
            .filter(e -> e.getValue().getAddress() != null)
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    if (interfaceMap.isEmpty()) {
      // With no interfaces in the VRF that have IP addresses, show ip bgp vrf all reports 0.0.0.0
      // as the router ID. Of course, this is not really relevant as no routes will be exchanged.
      return Ip.ZERO;
    }

    // Next, NX-OS prefers the IP of Loopback0 if one exists.
    Interface loopback0 = interfaceMap.get("Loopback0");
    if (loopback0 != null) {
      return loopback0.getAddress().getIp();
    }

    // Next, NX-OS prefers the smallest IP of any loopback interface
    Collection<Interface> interfaces = interfaceMap.values();
    routerId =
        interfaces
            .stream()
            .filter(i -> i.getName().startsWith("Loopback"))
            .map(Interface::getAddress)
            .map(InterfaceAddress::getIp)
            .min(Comparator.naturalOrder())
            // Finally, NX uses the first interface defined in the file, if no loopback addresses
            // with IP address are present.
            .orElseGet(() -> interfaces.iterator().next().getAddress().getIp());
    return routerId;
  }

  private CiscoNxConversions() {} // prevent instantiation of utility class.
}

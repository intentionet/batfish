package org.batfish.datamodel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DataPlaneArpAnalysis implements ArpAnalysis {

  private final Map<String, Map<String, IpAddressAcl>> _arpReplies;

  private final Map<
          String, Map<String, Map<AbstractRoute, Map<String, Map<ArpIpChoice, IpAddressAcl>>>>>
      _arpRequests;

  public DataPlaneArpAnalysis(
      Map<String, Configuration> configurations, DataPlane dp, Topology topology) {
    _arpReplies = computeArpReplies(configurations, dp);
    _arpRequests = computeArpRequests(dp, topology);
  }

  private Map<String, Map<String, IpAddressAcl>> computeArpReplies(
      Map<String, Configuration> configurations, DataPlane dp) {
    Map<String, Map<String, IpSpace>> routableIpsByNodeVrf =
        dp.getRibs()
            .entrySet()
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey,
                    ribsByNodeEntry ->
                        ribsByNodeEntry
                            .getValue()
                            .entrySet()
                            .stream()
                            .collect(
                                ImmutableMap.toImmutableMap(
                                    Entry::getKey,
                                    ribsByVrfEntry ->
                                        ribsByVrfEntry.getValue().getRoutableIps()))));
    return dp.getRibs()
        .entrySet()
        .stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                ribsByNodeEntry -> {
                  String hostname = ribsByNodeEntry.getKey();
                  Map<String, Interface> interfaces = configurations.get(hostname).getInterfaces();
                  Map<String, Fib> fibsByVrf = dp.getFibs().get(hostname);
                  Map<String, IpSpace> routableIpsByVrf = routableIpsByNodeVrf.get(hostname);
                  return ribsByNodeEntry
                      .getValue()
                      .entrySet()
                      .stream()
                      .flatMap(
                          ribsByVrfEntry -> {
                            String vrf = ribsByVrfEntry.getKey();
                            IpSpace routableIpsForThisVrf = routableIpsByVrf.get(vrf);
                            GenericRib<AbstractRoute> rib = ribsByVrfEntry.getValue();
                            Fib fib = fibsByVrf.get(vrf);
                            Map<Prefix, IpSpace> matchingIpsByPrefix = rib.getMatchingIps();
                            Map<String, ImmutableSet.Builder<IpSpace>> interfaceIps =
                                new HashMap<>();
                            rib.getRoutes()
                                .forEach(
                                    route -> {
                                      IpSpace matchingIps =
                                          matchingIpsByPrefix.get(route.getNetwork());
                                      fib.getNextHopInterfaces()
                                          .get(route)
                                          .keySet()
                                          .forEach(
                                              ifaceName -> {
                                                interfaceIps
                                                    .computeIfAbsent(
                                                        ifaceName,
                                                        n -> ImmutableSet.<IpSpace>builder())
                                                    .add(matchingIps);
                                              });
                                    });
                            return interfaceIps
                                .entrySet()
                                .stream()
                                .map(
                                    interfaceIpsEntry -> {
                                      String ifaceName = interfaceIpsEntry.getKey();
                                      Interface iface = interfaces.get(ifaceName);
                                      ImmutableList.Builder<IpAddressAclLine> lines =
                                          ImmutableList.builder();
                                      IpSpace.Builder ipsAssignedToThisInterface =
                                          IpSpace.builder();
                                      iface
                                          .getAllAddresses()
                                          .stream()
                                          .map(InterfaceAddress::getIp)
                                          .forEach(
                                              ip ->
                                                  ipsAssignedToThisInterface.including(
                                                      new IpWildcard(ip)));
                                      /* Accept IPs assigned to this interface */
                                      lines.add(
                                          new IpAddressAclLine(
                                              ipsAssignedToThisInterface.build(),
                                              LineAction.ACCEPT));

                                      if (iface.getProxyArp()) {
                                        /* Reject IPs routed through this interface */
                                        interfaceIpsEntry
                                            .getValue()
                                            .build()
                                            .stream()
                                            .map(
                                                ipSpace ->
                                                    new IpAddressAclLine(
                                                        ipSpace, LineAction.REJECT))
                                            .forEach(lines::add);

                                        /* Accept all other routable IPs */
                                        lines.add(
                                            new IpAddressAclLine(
                                                routableIpsForThisVrf, LineAction.ACCEPT));
                                      }
                                      return Maps.immutableEntry(
                                          ifaceName,
                                          IpAddressAcl.builder().setLines(lines.build()).build());
                                    });
                          })
                      .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
                }));
  }

  private Map<String, Map<String, Map<AbstractRoute, Map<String, Map<ArpIpChoice, IpAddressAcl>>>>>
      computeArpRequests(DataPlane dp, Topology topology) {
    throw new UnsupportedOperationException(
        "no implementation for generated method"); // TODO Auto-generated method stub
  }

  @Override
  public Map<String, Map<String, IpAddressAcl>> getArpReplies() {
    return _arpReplies;
  }

  @Override
  public Map<String, Map<String, Map<AbstractRoute, Map<String, Map<ArpIpChoice, IpAddressAcl>>>>>
      getArpRequests() {
    return _arpRequests;
  }
}

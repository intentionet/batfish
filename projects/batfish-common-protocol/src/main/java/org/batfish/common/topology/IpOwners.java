package org.batfish.common.topology;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.common.topology.TopologyUtil.computeNodeInterfaces;
import static org.batfish.common.util.CollectionUtil.toImmutableMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import io.opentracing.ActiveSpan;
import io.opentracing.util.GlobalTracer;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.Prefix;

/** A utility class for working with IPs owned by network devices. */
public final class IpOwners {

  /**
   * Mapping from a IP to hostname to set of interfaces that own that IP (including inactive
   * interfaces)
   */
  private final Map<Ip, Map<String, Set<String>>> _allDeviceOwnedIps;

  /**
   * Mapping from a IP to hostname to set of interfaces that own that IP (for active interfaces
   * only)
   */
  private final Map<Ip, Map<String, Set<String>>> _activeDeviceOwnedIps;

  /** Mapping from hostname to interface name to IpSpace owned by that interface */
  private final Map<String, Map<String, IpSpace>> _hostToInterfaceToIpSpace;

  /**
   * Mapping from hostname to interface name to host IP subnet.
   *
   * @see Prefix#toHostIpSpace()
   */
  private final Map<String, Map<String, IpSpace>> _allInterfaceHostIps;

  /**
   * Mapping from hostname to interface name to host IP subnet (for active interfaces only).
   *
   * @see Prefix#toHostIpSpace()
   */
  private final Map<String, Map<String, IpSpace>> _activeInterfaceHostIps;

  /** Mapping from an IP to hostname to set of VRFs that own that IP. */
  private final Map<Ip, Map<String, Set<String>>> _ipVrfOwners;

  private final Map<String, Map<String, IpSpace>> _vrfOwnedIpSpaces;

  public IpOwners(Map<String, Configuration> configurations) {
    /* Mapping from a hostname to a set of all (including inactive) interfaces that node owns */
    Map<String, Set<Interface>> allInterfaces =
        ImmutableMap.copyOf(computeNodeInterfaces(configurations));

    {
      _allDeviceOwnedIps = ImmutableMap.copyOf(computeIpInterfaceOwners(allInterfaces, false));
      _activeDeviceOwnedIps = ImmutableMap.copyOf(computeIpInterfaceOwners(allInterfaces, true));
    }

    {
      _hostToInterfaceToIpSpace =
          ImmutableMap.copyOf(computeInterfaceOwnedIpSpaces(_activeDeviceOwnedIps));
      _allInterfaceHostIps = computeInterfaceHostSubnetIps(configurations, false);
      _activeInterfaceHostIps = computeInterfaceHostSubnetIps(configurations, true);
    }

    {
      _ipVrfOwners = computeIpVrfOwners(allInterfaces, _activeDeviceOwnedIps);
      _vrfOwnedIpSpaces = computeVrfOwnedIpSpaces(_ipVrfOwners);
    }
  }

  /**
   * Invert a mapping from {@link Ip} to owner interfaces (Ip -&gt; hostname -&gt; interface name)
   * and convert the set of owned Ips into an IpSpace.
   */
  public static Map<String, Map<String, IpSpace>> computeInterfaceOwnedIpSpaces(
      Map<Ip, Map<String, Set<String>>> ipInterfaceOwners) {
    return toImmutableMap(
        computeInterfaceOwnedIps(ipInterfaceOwners),
        Entry::getKey, /* host */
        hostEntry ->
            toImmutableMap(
                hostEntry.getValue(),
                Entry::getKey, /* interface */
                ifaceEntry ->
                    AclIpSpace.union(
                        ifaceEntry.getValue().stream()
                            .map(Ip::toIpSpace)
                            .collect(Collectors.toList()))));
  }

  @VisibleForTesting
  static Map<String, Map<String, IpSpace>> computeInterfaceHostSubnetIps(
      Map<String, Configuration> configs, boolean excludeInactive) {
    try (ActiveSpan span =
        GlobalTracer.get().buildSpan("IpOwners.computeInterfaceHostSubnetIps").startActive()) {
      assert span != null; // avoid unused warning
      return toImmutableMap(
          configs,
          Entry::getKey, /* hostname */
          nodeEntry ->
              toImmutableMap(
                  excludeInactive
                      ? nodeEntry.getValue().getActiveInterfaces()
                      : nodeEntry.getValue().getAllInterfaces(),
                  Entry::getKey, /* interface */
                  ifaceEntry ->
                      firstNonNull(
                          AclIpSpace.union(
                              ifaceEntry.getValue().getAllConcreteAddresses().stream()
                                  .map(ConcreteInterfaceAddress::getPrefix)
                                  .map(Prefix::toHostIpSpace)
                                  .collect(ImmutableList.toImmutableList())),
                          EmptyIpSpace.INSTANCE)));
    }
  }

  /**
   * Compute the {@link Ip}s owned by each interface. hostname -&gt; interface name -&gt; {@link
   * Ip}s.
   */
  public static Map<String, Map<String, Set<Ip>>> computeInterfaceOwnedIps(
      Map<String, Configuration> configurations, boolean excludeInactive) {
    // TODO: cleanup callers, make this private
    return computeInterfaceOwnedIps(
        computeIpInterfaceOwners(computeNodeInterfaces(configurations), excludeInactive));
  }

  /**
   * Invert a mapping from {@link Ip} to owner interfaces (Ip -&gt; hostname -&gt; interface name)
   * to (hostname -&gt; interface name -&gt; Ip).
   */
  private static Map<String, Map<String, Set<Ip>>> computeInterfaceOwnedIps(
      Map<Ip, Map<String, Set<String>>> ipInterfaceOwners) {
    Map<String, Map<String, Set<Ip>>> ownedIps = new HashMap<>();

    ipInterfaceOwners.forEach(
        (ip, owners) ->
            owners.forEach(
                (host, ifaces) ->
                    ifaces.forEach(
                        iface ->
                            ownedIps
                                .computeIfAbsent(host, k -> new HashMap<>())
                                .computeIfAbsent(iface, k -> new HashSet<>())
                                .add(ip))));

    // freeze
    return toImmutableMap(
        ownedIps,
        Entry::getKey, /* host */
        hostEntry ->
            toImmutableMap(
                hostEntry.getValue(),
                Entry::getKey, /* interface */
                ifaceEntry -> ImmutableSet.copyOf(ifaceEntry.getValue())));
  }

  /**
   * Compute a mapping of IP addresses to a set of hostnames that "own" this IP (e.g., as a network
   * interface address)
   *
   * @param configurations map of configurations keyed by hostname
   * @param excludeInactive Whether to exclude inactive interfaces
   * @return A map of {@link Ip}s to a set of hostnames that own this IP
   */
  public static Map<Ip, Set<String>> computeIpNodeOwners(
      Map<String, Configuration> configurations, boolean excludeInactive) {
    try (ActiveSpan span =
        GlobalTracer.get()
            .buildSpan("TopologyUtil.computeIpNodeOwners excludeInactive=" + excludeInactive)
            .startActive()) {
      assert span != null; // avoid unused warning

      return toImmutableMap(
          computeIpInterfaceOwners(computeNodeInterfaces(configurations), excludeInactive),
          Entry::getKey, /* Ip */
          ipInterfaceOwnersEntry ->
              /* project away interfaces */
              ipInterfaceOwnersEntry.getValue().keySet());
    }
  }

  /**
   * Compute a mapping from IP address to the interfaces that "own" that IP (e.g., as a network
   * interface address).
   *
   * <p>Takes into account VRRP configuration.
   *
   * @param allInterfaces A mapping of interfaces: hostname -&gt; set of {@link Interface}
   * @param excludeInactive whether to ignore inactive interfaces
   * @return A map from {@link Ip}s to hostname to set of interface names that own that IP.
   */
  public static Map<Ip, Map<String, Set<String>>> computeIpInterfaceOwners(
      Map<String, Set<Interface>> allInterfaces, boolean excludeInactive) {
    Map<Ip, Map<String, Set<String>>> ipOwners = new HashMap<>();
    Table<ConcreteInterfaceAddress, Integer, Set<Interface>> vrrpGroups = HashBasedTable.create();
    allInterfaces.forEach(
        (hostname, interfaces) ->
            interfaces.forEach(
                i -> {
                  if ((!i.getActive() || i.getBlacklisted()) && excludeInactive) {
                    return;
                  }
                  // collect vrrp info
                  i.getVrrpGroups()
                      .forEach(
                          (groupNum, vrrpGroup) -> {
                            ConcreteInterfaceAddress address = vrrpGroup.getVirtualAddress();
                            if (address == null) {
                              /*
                               * Invalid VRRP configuration. The VRRP has no source IP address that
                               * would be used for VRRP election. This interface could never win the
                               * election, so is not a candidate.
                               */
                              return;
                            }
                            Set<Interface> candidates = vrrpGroups.get(address, groupNum);
                            if (candidates == null) {
                              candidates = Collections.newSetFromMap(new IdentityHashMap<>());
                              vrrpGroups.put(address, groupNum, candidates);
                            }
                            candidates.add(i);
                          });
                  // collect prefixes
                  i.getAllConcreteAddresses().stream()
                      .map(ConcreteInterfaceAddress::getIp)
                      .forEach(
                          ip ->
                              ipOwners
                                  .computeIfAbsent(ip, k -> new HashMap<>())
                                  .computeIfAbsent(hostname, k -> new HashSet<>())
                                  .add(i.getName()));
                }));
    vrrpGroups
        .cellSet()
        .forEach(
            cell -> {
              ConcreteInterfaceAddress address = cell.getRowKey();
              assert address != null;
              Integer groupNum = cell.getColumnKey();
              assert groupNum != null;
              Set<Interface> candidates = cell.getValue();
              assert candidates != null;
              /*
               * Compare priorities first. If tied, break tie based on highest interface IP.
               */
              Interface vrrpMaster =
                  Collections.max(
                      candidates,
                      Comparator.comparingInt(
                              (Interface o) -> o.getVrrpGroups().get(groupNum).getPriority())
                          .thenComparing(o -> o.getConcreteAddress().getIp()));
              ipOwners
                  .computeIfAbsent(address.getIp(), k -> new HashMap<>())
                  .computeIfAbsent(vrrpMaster.getOwner().getHostname(), k -> new HashSet<>())
                  .add(vrrpMaster.getName());
            });

    // freeze
    return toImmutableMap(
        ipOwners,
        Entry::getKey,
        ipOwnersEntry ->
            toImmutableMap(
                ipOwnersEntry.getValue(),
                Entry::getKey, // hostname
                hostIpOwnersEntry -> ImmutableSet.copyOf(hostIpOwnersEntry.getValue())));
  }

  /**
   * Compute a mapping of IP addresses to the VRFs that "own" this IP (e.g., as a network interface
   * address).
   *
   * @param allInterfaces A mapping of enabled interfaces hostname -&gt; interface name -&gt; {@link
   *     Interface}
   * @param activeDeviceOwnedIps Mapping from a IP to hostname to set of interfaces that own that IP
   *     (for active interfaces only)
   * @return A map of {@link Ip}s to a map of hostnames to vrfs that own the Ip.
   */
  @VisibleForTesting
  static Map<Ip, Map<String, Set<String>>> computeIpVrfOwners(
      Map<String, Set<Interface>> allInterfaces,
      Map<Ip, Map<String, Set<String>>> activeDeviceOwnedIps) {

    // Helper mapping: Hostname -> interface name -> vrf name
    Map<String, Map<String, String>> allInterfaceVrfs =
        toImmutableMap(
            allInterfaces,
            Entry::getKey, /* hostname */
            nodeInterfaces ->
                nodeInterfaces.getValue().stream()
                    .collect(
                        ImmutableMap.toImmutableMap(Interface::getName, Interface::getVrfName)));

    return toImmutableMap(
        activeDeviceOwnedIps,
        Entry::getKey, /* Ip */
        ipInterfaceOwnersEntry ->
            toImmutableMap(
                ipInterfaceOwnersEntry.getValue(),
                Entry::getKey, /* Hostname */
                ipNodeInterfaceOwnersEntry ->
                    ipNodeInterfaceOwnersEntry.getValue().stream()
                        .map(allInterfaceVrfs.get(ipNodeInterfaceOwnersEntry.getKey())::get)
                        .collect(ImmutableSet.toImmutableSet())));
  }

  /**
   * Invert a mapping from Ip to VRF owners (Ip -&gt; host name -&gt; VRF name) and combine all IPs
   * owned by each VRF into an IpSpace.
   */
  private static Map<String, Map<String, IpSpace>> computeVrfOwnedIpSpaces(
      Map<Ip, Map<String, Set<String>>> ipVrfOwners) {
    Map<String, Map<String, AclIpSpace.Builder>> builders = new HashMap<>();
    ipVrfOwners.forEach(
        (ip, ipNodeVrfs) ->
            ipNodeVrfs.forEach(
                (node, vrfs) ->
                    vrfs.forEach(
                        vrf ->
                            builders
                                .computeIfAbsent(node, k -> new HashMap<>())
                                .computeIfAbsent(vrf, k -> AclIpSpace.builder())
                                .thenPermitting(ip.toIpSpace()))));

    return toImmutableMap(
        builders,
        Entry::getKey, /* node */
        nodeEntry ->
            toImmutableMap(
                nodeEntry.getValue(),
                Entry::getKey, /* vrf */
                vrfEntry -> vrfEntry.getValue().build()));
  }

  /**
   * Mapping from a IP to hostname to set of interfaces that own that IP (for active interfaces
   * only)
   */
  public Map<Ip, Map<String, Set<String>>> getActiveDeviceOwnedIps() {
    return _activeDeviceOwnedIps;
  }

  /**
   * Mapping from a IP to hostname to set of interfaces that own that IP (including inactive
   * interfaces)
   */
  public Map<Ip, Map<String, Set<String>>> getAllDeviceOwnedIps() {
    return _allDeviceOwnedIps;
  }

  /**
   * Returns a mapping from hostname to interface name to the host {@link IpSpace} of that
   * interface, for active interfaces only.
   *
   * @see Prefix#toHostIpSpace()
   */
  public Map<String, Map<String, IpSpace>> getActiveInterfaceHostIps() {
    return _activeInterfaceHostIps;
  }

  /**
   * Returns a mapping from hostname to interface name to the host {@link IpSpace} of that
   * interface, including inactive interfaces.
   *
   * @see Prefix#toHostIpSpace()
   */
  public Map<String, Map<String, IpSpace>> getAllInterfaceHostIps() {
    return _allInterfaceHostIps;
  }

  /**
   * Returns a mapping from hostname to interface name to IpSpace owned by that interface, for
   * active interfaces only
   */
  public Map<String, Map<String, IpSpace>> getInterfaceOwnedIpSpaces() {
    return _hostToInterfaceToIpSpace;
  }

  /** Returns a mapping from IP to hostname to set of VRFs that own that IP. */
  public Map<Ip, Map<String, Set<String>>> getIpVrfOwners() {
    return _ipVrfOwners;
  }

  /**
   * Returns a mapping from hostname to vrf name to a space of IPs owned by that VRF. Only considers
   * interface IPs. Considers <em>only active</em> interfaces.
   */
  public Map<String, Map<String, IpSpace>> getVrfOwnedIpSpaces() {
    return _vrfOwnedIpSpaces;
  }
}

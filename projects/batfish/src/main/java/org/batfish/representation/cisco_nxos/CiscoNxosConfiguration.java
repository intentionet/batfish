package org.batfish.representation.cisco_nxos;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.datamodel.Interface.NULL_INTERFACE_NAME;
import static org.batfish.datamodel.Route.UNSET_NEXT_HOP_INTERFACE;
import static org.batfish.datamodel.Route.UNSET_ROUTE_NEXT_HOP_IP;
import static org.batfish.representation.cisco_nxos.CiscoNxosInterfaceType.PORT_CHANNEL;
import static org.batfish.representation.cisco_nxos.Interface.BANDWIDTH_CONVERSION_FACTOR;
import static org.batfish.representation.cisco_nxos.Interface.getDefaultBandwidth;
import static org.batfish.representation.cisco_nxos.Interface.getDefaultSpeed;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.VendorConversionException;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.vendor_family.cisco_nxos.CiscoNxosFamily;
import org.batfish.vendor.VendorConfiguration;

/** Vendor-specific representation of a Cisco NX-OS network configuration */
public final class CiscoNxosConfiguration extends VendorConfiguration {

  /*
   * This map is used to convert interface names to their canonical forms.
   * The entries are visited in insertion order until a key is found of which the name to convert is
   * case-insensitively a prefix. The value corresponding to that key is chosen as the canonical
   * form for that name.
   *
   * NOTE: Entries are sorted by priority. Do not reorder unless you have a good reason.
   */
  private static final Map<String, String> CISCO_NXOS_INTERFACE_PREFIXES;
  private static final Pattern CISCO_NXOS_INTERFACE_PREFIXES_REGEX;
  private static final IntegerSpace DEFAULT_RESERVED_VLAN_RANGE =
      IntegerSpace.of(Range.closed(3968, 4094));
  public static final String NULL_VRF_NAME = "~NULL_VRF~";

  static {
    CISCO_NXOS_INTERFACE_PREFIXES =
        ImmutableMap.<String, String>builder()
            .put("Ethernet", "Ethernet")
            .put("loopback", "loopback")
            .put("mgmt", "mgmt")
            .put("Null", "Null")
            .put("nve", "nve")
            .put("port-channel", "port-channel")
            .put("Vlan", "Vlan")
            .build();
    CISCO_NXOS_INTERFACE_PREFIXES_REGEX =
        Pattern.compile(
            CISCO_NXOS_INTERFACE_PREFIXES.values().stream()
                .map(String::toLowerCase)
                .collect(Collectors.joining("|")));
  }

  /** Returns canonical prefix of interface name if valid, else {@code null}. */
  public static @Nullable String getCanonicalInterfaceNamePrefix(String prefix) {
    for (Entry<String, String> e : CISCO_NXOS_INTERFACE_PREFIXES.entrySet()) {
      String matchPrefix = e.getKey();
      String canonicalPrefix = e.getValue();
      if (matchPrefix.toLowerCase().startsWith(prefix.toLowerCase())) {
        return canonicalPrefix;
      }
    }
    return null;
  }

  private static @Nonnull RouteFilterLine toRouteFilterLine(IpPrefixListLine ipPrefixListLine) {
    return new RouteFilterLine(
        ipPrefixListLine.getAction(),
        ipPrefixListLine.getPrefix(),
        ipPrefixListLine.getLengthRange());
  }

  private static @Nonnull RouteFilterList toRouteFilterList(IpPrefixList ipPrefixList) {
    String name = ipPrefixList.getName();
    RouteFilterList rfl = new RouteFilterList(name);
    rfl.setLines(
        ipPrefixList.getLines().values().stream()
            .map(CiscoNxosConfiguration::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList()));
    return rfl;
  }

  private transient Configuration _c;
  private final @Nonnull Vrf _defaultVrf;
  private @Nullable String _hostname;
  private final @Nonnull Map<String, Interface> _interfaces;
  private final @Nonnull Map<String, IpAccessList> _ipAccessLists;
  private final @Nonnull Map<String, IpCommunityList> _ipCommunityLists;
  private final @Nonnull Map<String, IpPrefixList> _ipPrefixLists;
  private transient Multimap<String, String> _portChannelMembers;
  private @Nonnull IntegerSpace _reservedVlanRange;

  private final @Nonnull Map<Integer, Vlan> _vlans;

  private final @Nonnull Map<String, Vrf> _vrfs;

  public CiscoNxosConfiguration() {
    _defaultVrf = new Vrf(DEFAULT_VRF_NAME);
    _interfaces = new HashMap<>();
    _ipAccessLists = new HashMap<>();
    _ipCommunityLists = new HashMap<>();
    _ipPrefixLists = new HashMap<>();
    _reservedVlanRange = DEFAULT_RESERVED_VLAN_RANGE;
    _vlans = new HashMap<>();
    _vrfs = new HashMap<>();
  }

  @Override
  public String canonicalizeInterfaceName(String ifaceName) {
    Matcher matcher = CISCO_NXOS_INTERFACE_PREFIXES_REGEX.matcher(ifaceName.toLowerCase());
    if (!matcher.find()) {
      throw new BatfishException("Invalid interface name: '" + ifaceName + "'");
    }
    String ifacePrefix = matcher.group();
    String canonicalPrefix = getCanonicalInterfaceNamePrefix(ifacePrefix);
    if (canonicalPrefix == null) {
      throw new BatfishException("Invalid interface name: '" + ifaceName + "'");
    }
    String suffix = ifaceName.substring(ifacePrefix.length());
    return canonicalPrefix + suffix;
  }

  private void convertInterface(Interface iface) {
    String ifaceName = iface.getName();
    org.batfish.datamodel.Interface newIface = toInterface(iface);
    _c.getAllInterfaces().put(ifaceName, newIface);
    org.batfish.datamodel.Vrf vrf = newIface.getVrf();
    vrf.getInterfaces().put(ifaceName, newIface);
  }

  private void convertInterfaces() {
    _portChannelMembers = HashMultimap.create();
    // non-port channels
    _interfaces.values().stream()
        .filter(iface -> iface.getType() != CiscoNxosInterfaceType.PORT_CHANNEL)
        .forEach(this::convertInterface);
    // port channels
    _interfaces.values().stream()
        .filter(iface -> iface.getType() == CiscoNxosInterfaceType.PORT_CHANNEL)
        .forEach(this::convertInterface);
  }

  private void convertIpPrefixLists() {
    _ipPrefixLists.forEach(
        (name, ipPrefixList) ->
            _c.getRouteFilterLists().put(name, toRouteFilterList(ipPrefixList)));
  }

  private void convertStaticRoutes() {
    Stream.concat(Stream.of(_defaultVrf), _vrfs.values().stream())
        .forEach(this::convertStaticRoutes);
  }

  private void convertStaticRoutes(Vrf vrf) {
    _c.getVrfs()
        .get(vrf.getName())
        .setStaticRoutes(
            vrf.getStaticRoutes().values().stream()
                .map(this::toStaticRoute)
                .filter(Objects::nonNull)
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  private void convertVrfs() {
    _c.getVrfs().put(DEFAULT_VRF_NAME, new org.batfish.datamodel.Vrf(DEFAULT_VRF_NAME));
    _c.getVrfs().put(NULL_VRF_NAME, new org.batfish.datamodel.Vrf(NULL_VRF_NAME));
    _vrfs.forEach((name, vrf) -> _c.getVrfs().put(name, toVrf(vrf)));
  }

  private @Nonnull CiscoNxosFamily createCiscoNxosFamily() {
    return CiscoNxosFamily.builder().build();
  }

  /** Disable Vlan interfaces without corresponding top-level vlan declaration. */
  private void disableUnregisteredVlanInterfaces() {
    _c.getAllInterfaces().values().stream()
        .filter(
            iface ->
                iface.getInterfaceType() == InterfaceType.VLAN
                    && !_vlans.keySet().contains(iface.getVlan()))
        .forEach(iface -> iface.setActive(false));
  }

  public @Nonnull Vrf getDefaultVrf() {
    return _defaultVrf;
  }

  @Override
  public @Nullable String getHostname() {
    return _hostname;
  }

  public @Nonnull Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public @Nonnull Map<String, IpAccessList> getIpAccessLists() {
    return _ipAccessLists;
  }

  public @Nonnull Map<String, IpCommunityList> getIpCommunityLists() {
    return _ipCommunityLists;
  }

  public @Nonnull Map<String, IpPrefixList> getIpPrefixLists() {
    return _ipPrefixLists;
  }

  /** Range of VLAN IDs reserved by the system and therefore unassignable. */
  public @Nonnull IntegerSpace getReservedVlanRange() {
    return _reservedVlanRange;
  }

  public @Nonnull Map<Integer, Vlan> getVlans() {
    return _vlans;
  }

  public @Nonnull Map<String, Vrf> getVrfs() {
    return _vrfs;
  }

  private void markStructures() {
    markConcreteStructure(
        CiscoNxosStructureType.INTERFACE,
        CiscoNxosStructureUsage.INTERFACE_SELF_REFERENCE,
        CiscoNxosStructureUsage.IP_ROUTE_NEXT_HOP_INTERFACE);
    markConcreteStructure(
        CiscoNxosStructureType.PORT_CHANNEL, CiscoNxosStructureUsage.INTERFACE_CHANNEL_GROUP);
    markConcreteStructure(CiscoNxosStructureType.VLAN, CiscoNxosStructureUsage.INTERFACE_VLAN);
    markConcreteStructure(
        CiscoNxosStructureType.VRF,
        CiscoNxosStructureUsage.INTERFACE_VRF_MEMBER,
        CiscoNxosStructureUsage.IP_ROUTE_NEXT_HOP_VRF);
  }

  @Override
  public void setHostname(String hostname) {
    checkNotNull(hostname, "hostname cannot be null");
    _hostname = hostname.toLowerCase();
  }

  @Override
  public void setVendor(ConfigurationFormat format) {}

  private @Nonnull org.batfish.datamodel.Interface toInterface(Interface iface) {
    String ifaceName = iface.getName();
    org.batfish.datamodel.Interface.Builder newIfaceBuilder =
        org.batfish.datamodel.Interface.builder().setName(ifaceName);

    String parent = iface.getParentInterface();
    if (parent != null) {
      newIfaceBuilder.setDependencies(ImmutableSet.of(new Dependency(parent, DependencyType.BIND)));
    }

    newIfaceBuilder.setActive(!iface.getShutdown());

    if (iface.getAddress() != null) {
      newIfaceBuilder.setAddress(iface.getAddress().getAddress());
    }
    newIfaceBuilder.setSecondaryAddresses(
        iface.getSecondaryAddresses().stream()
            .map(InterfaceAddressWithAttributes::getAddress)
            .collect(ImmutableSet.toImmutableSet()));

    newIfaceBuilder.setDescription(iface.getDescription());

    newIfaceBuilder.setMtu(iface.getMtu());

    // switchport+vlan settings
    SwitchportMode switchportMode = iface.getSwitchportMode();
    newIfaceBuilder.setSwitchportMode(switchportMode);
    switch (iface.getSwitchportMode()) {
      case ACCESS:
        newIfaceBuilder.setSwitchport(true);
        newIfaceBuilder.setAccessVlan(iface.getAccessVlan());
        break;

      case NONE:
        newIfaceBuilder.setEncapsulationVlan(iface.getEncapsulationVlan());
        break;

      case TRUNK:
        newIfaceBuilder.setSwitchport(true);
        newIfaceBuilder.setAllowedVlans(iface.getAllowedVlans());
        newIfaceBuilder.setNativeVlan(iface.getNativeVlan());
        break;

      case DOT1Q_TUNNEL:
      case DYNAMIC_AUTO:
      case DYNAMIC_DESIRABLE:
      case FEX_FABRIC:
      case TAP:
      case TOOL:
      default:
        // unsupported
        break;
    }
    newIfaceBuilder.setVlan(iface.getVlan());
    newIfaceBuilder.setAutoState(iface.getAutostate());

    CiscoNxosInterfaceType type = iface.getType();
    newIfaceBuilder.setType(toInterfaceType(type, parent != null));

    Double speed = getDefaultSpeed(type);
    newIfaceBuilder.setSpeed(speed);
    Integer nxosBandwidth = iface.getBandwidth();
    Double finalBandwidth;
    if (nxosBandwidth != null) {
      finalBandwidth = nxosBandwidth * BANDWIDTH_CONVERSION_FACTOR;
    } else if (speed != null) {
      finalBandwidth = speed;
    } else {
      finalBandwidth = getDefaultBandwidth(type);
    }
    newIfaceBuilder.setBandwidth(finalBandwidth);

    // port-channel members
    String portChannel = iface.getChannelGroup();
    if (portChannel != null) {
      newIfaceBuilder.setChannelGroup(portChannel);
      _portChannelMembers.put(portChannel, ifaceName);
    }

    // port-channels
    if (type == PORT_CHANNEL) {
      Collection<String> members = _portChannelMembers.get(ifaceName);
      newIfaceBuilder.setChannelGroupMembers(members);
      newIfaceBuilder.setDependencies(
          members.stream()
              .map(member -> new Dependency(member, DependencyType.AGGREGATE))
              .collect(ImmutableSet.toImmutableSet()));
    }

    org.batfish.datamodel.Interface newIface = newIfaceBuilder.build();

    String vrfName = iface.getVrfMember();

    if (vrfName != null) {
      org.batfish.datamodel.Vrf vrf = _c.getVrfs().get(vrfName);
      if (vrf == null) {
        // Non-existent VRF set; disable and put in null VRF
        newIface.setActive(false);
        vrf = _c.getVrfs().get(NULL_VRF_NAME);
      } else if (_vrfs.get(vrfName).getShutdown()) {
        // VRF is shutdown; disable
        newIface.setActive(false);
      }
      newIface.setVrf(vrf);
    } else {
      // No VRF set; put in default VRF
      newIface.setVrf(_c.getDefaultVrf());
    }

    newIface.setOwner(_c);
    return newIface;
  }

  private @Nonnull InterfaceType toInterfaceType(
      CiscoNxosInterfaceType type, boolean subinterface) {
    switch (type) {
      case ETHERNET:
        return subinterface ? InterfaceType.LOGICAL : InterfaceType.PHYSICAL;
      case LOOPBACK:
        return InterfaceType.LOOPBACK;
      case MGMT:
        return InterfaceType.PHYSICAL;
      case PORT_CHANNEL:
        return subinterface ? InterfaceType.AGGREGATE_CHILD : InterfaceType.AGGREGATED;
      case VLAN:
        return InterfaceType.VLAN;
      default:
        return InterfaceType.UNKNOWN;
    }
  }

  /**
   * Converts the supplied {@code staticRoute} to a a vendor-independent {@link
   * org.batfish.datamodel.StaticRoute} if all options are supported and static route contains no
   * undefined references. Otherwise, returns {@code null}.
   */
  private @Nullable org.batfish.datamodel.StaticRoute toStaticRoute(StaticRoute staticRoute) {
    // TODO: VI and VS support for lookup of next-hop-ip in a different VRF
    if (staticRoute.getNextHopVrf() != null) {
      return null;
    }
    // TODO: support track object number
    String nextHopInterface = staticRoute.getNextHopInterface();
    String newNextHopInterface;
    if (nextHopInterface != null) {
      if (!_interfaces.containsKey(nextHopInterface)) {
        // undefined reference
        return null;
      }
      newNextHopInterface = nextHopInterface;
    } else if (staticRoute.getDiscard()) {
      newNextHopInterface = NULL_INTERFACE_NAME;
    } else {
      newNextHopInterface = UNSET_NEXT_HOP_INTERFACE;
    }
    return org.batfish.datamodel.StaticRoute.builder()
        .setAdministrativeCost((int) staticRoute.getPreference())
        .setMetric(0L)
        .setNetwork(staticRoute.getPrefix())
        .setNextHopInterface(newNextHopInterface)
        .setNextHopIp(firstNonNull(staticRoute.getNextHopIp(), UNSET_ROUTE_NEXT_HOP_IP))
        .setTag(staticRoute.getTag())
        .build();
  }

  private @Nonnull Configuration toVendorIndependentConfiguration() {
    _c = new Configuration(_hostname, ConfigurationFormat.CISCO_NX);
    _c.getVendorFamily().setCiscoNxos(createCiscoNxosFamily());
    _c.setDefaultInboundAction(LineAction.PERMIT);
    _c.setDefaultCrossZoneAction(LineAction.PERMIT);

    convertVrfs();
    convertInterfaces();
    disableUnregisteredVlanInterfaces();
    convertStaticRoutes();
    convertIpPrefixLists();

    markStructures();
    return _c;
  }

  @Override
  public @Nonnull List<Configuration> toVendorIndependentConfigurations()
      throws VendorConversionException {
    return ImmutableList.of(toVendorIndependentConfiguration());
  }

  private @Nonnull org.batfish.datamodel.Vrf toVrf(Vrf vrf) {
    org.batfish.datamodel.Vrf.Builder newVrfBuilder =
        org.batfish.datamodel.Vrf.builder().setName(vrf.getName());
    return newVrfBuilder.build();
  }
}

package org.batfish.representation.cisco_nxos;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

  private transient Configuration _c;
  private @Nullable String _hostname;
  private final @Nonnull Map<String, Interface> _interfaces;
  private @Nonnull IntegerSpace _reservedVlanRange;
  private final @Nonnull Map<Integer, Vlan> _vlans;

  public CiscoNxosConfiguration() {
    _interfaces = new HashMap<>();
    _reservedVlanRange = DEFAULT_RESERVED_VLAN_RANGE;
    _vlans = new HashMap<>();
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

  private void convertInterfaces() {
    _interfaces.forEach(
        (ifaceName, iface) -> {
          org.batfish.datamodel.Interface newIface = toInterface(iface);
          _c.getAllInterfaces().put(ifaceName, newIface);
          newIface.getVrf().getInterfaces().put(ifaceName, newIface);
        });
  }

  private void convertVrfs() {
    _c.getVrfs().put(DEFAULT_VRF_NAME, new org.batfish.datamodel.Vrf(DEFAULT_VRF_NAME));
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

  @Override
  public @Nullable String getHostname() {
    return _hostname;
  }

  public @Nonnull Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  /** Range of VLAN IDs reserved by the system and therefore unassignable. */
  public @Nonnull IntegerSpace getReservedVlanRange() {
    return _reservedVlanRange;
  }

  public @Nonnull Map<Integer, Vlan> getVlans() {
    return _vlans;
  }

  private void markStructures() {
    markConcreteStructure(
        CiscoNxosStructureType.INTERFACE, CiscoNxosStructureUsage.INTERFACE_SELF_REFERENCE);
    markConcreteStructure(CiscoNxosStructureType.VLAN, CiscoNxosStructureUsage.INTERFACE_VLAN);
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

    newIfaceBuilder.setAddresses(iface.getAddress(), iface.getSecondaryAddresses());

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

    newIfaceBuilder.setType(toInterfaceType(iface.getType(), parent != null));

    org.batfish.datamodel.Interface newIface = newIfaceBuilder.build();
    String vrfName = firstNonNull(iface.getVrfMember(), DEFAULT_VRF_NAME);
    newIface.setVrf(_c.getVrfs().get(vrfName));
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

  private @Nonnull Configuration toVendorIndependentConfiguration() {
    _c = new Configuration(_hostname, ConfigurationFormat.CISCO_NX);
    _c.getVendorFamily().setCiscoNxos(createCiscoNxosFamily());
    _c.setDefaultInboundAction(LineAction.PERMIT);
    _c.setDefaultCrossZoneAction(LineAction.PERMIT);

    convertVrfs();
    convertInterfaces();
    disableUnregisteredVlanInterfaces();

    markStructures();
    return _c;
  }

  @Override
  public @Nonnull List<Configuration> toVendorIndependentConfigurations()
      throws VendorConversionException {
    return ImmutableList.of(toVendorIndependentConfiguration());
  }
}

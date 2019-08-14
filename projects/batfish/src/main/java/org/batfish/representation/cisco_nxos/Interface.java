package org.batfish.representation.cisco_nxos;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Range;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.SwitchportMode;

/** A layer-2- or layer-3-capable network interface */
public final class Interface implements Serializable {

  public static final double BANDWIDTH_CONVERSION_FACTOR = 1000D; // kbits

  private static final double DEFAULT_INTERFACE_BANDWIDTH = 1E12D;

  /** Loopback bandwidth */
  private static final double DEFAULT_LOOPBACK_BANDWIDTH = 8E9D;
  /** NX-OS Ethernet 802.3z - may not apply for non-NX-OS */
  private static final double DEFAULT_NXOS_ETHERNET_SPEED = 1E9D;

  private static final double DEFAULT_VLAN_BANDWIDTH = 1E9D;
  public static final IntegerSpace VLAN_RANGE = IntegerSpace.of(Range.closed(1, 4094));

  /**
   * Returns the shutdown status for an interface when shutdown is not explicitly configured. Once
   * explicitly configured, inference no longer applies for the life of the interface.
   *
   * <ul>
   *   <li>Ethernet parent interfaces are shutdown by default iff they are not in switchport mode.
   *   <li>port-channel parent interfaces are not shutdown by default.
   *   <li>Ethernet and port-channel subinterfaces are shut down by default.
   *   <li>loopback interfaces are not shutdown by default.
   *   <li>mgmt interfaces are not shutdown by default.
   *   <li>vlan interfaces are shutdown by default.
   * </ul>
   */
  private static boolean defaultShutdown(
      SwitchportMode switchportMode, CiscoNxosInterfaceType type, boolean subinterface) {
    switch (type) {
      case ETHERNET:
        return switchportMode == SwitchportMode.NONE;

      case PORT_CHANNEL:
        return subinterface;

      case VLAN:
        return true;

      case LOOPBACK:
      case MGMT:
      default:
        return false;
    }
  }

  public static @Nullable Double getDefaultBandwidth(CiscoNxosInterfaceType type) {
    Double defaultSpeed = getDefaultSpeed(type);
    if (defaultSpeed != null) {
      return defaultSpeed;
    }
    switch (type) {
      case LOOPBACK:
        return DEFAULT_LOOPBACK_BANDWIDTH;
      case PORT_CHANNEL:
        return null;
      case VLAN:
        return DEFAULT_VLAN_BANDWIDTH;
      default:
        // Use default bandwidth for other interface types that have no speed
        return DEFAULT_INTERFACE_BANDWIDTH;
    }
  }

  public static @Nullable Double getDefaultSpeed(CiscoNxosInterfaceType type) {
    if (type == CiscoNxosInterfaceType.ETHERNET) {
      return DEFAULT_NXOS_ETHERNET_SPEED;
    } else {
      // loopback
      // port-Channel
      // Vlan
      // ... others
      return null;
    }
  }

  /**
   * Construct a non-Vlan interface with given {@code name}, {@code parentInterface}, and {@code
   * type}.
   */
  public static @Nonnull Interface newNonVlanInterface(
      String name, @Nullable String parentInterface, CiscoNxosInterfaceType type) {
    checkArgument(type != CiscoNxosInterfaceType.VLAN, "Expected non-VLAN interface type");
    return new Interface(name, parentInterface, type, null);
  }

  /** Construct a Vlan interface with given {@code name} and {@code vlan} ID. */
  public static @Nonnull Interface newVlanInterface(String name, int vlan) {
    return new Interface(name, null, CiscoNxosInterfaceType.VLAN, vlan);
  }

  private @Nullable Integer _accessVlan;
  private @Nullable InterfaceAddressWithAttributes _address;
  private @Nullable IntegerSpace _allowedVlans;
  private boolean _autostate;
  private @Nullable Integer _bandwidth;
  private @Nullable String _channelGroup;
  private final @Nonnull Set<String> _declaredNames;
  private @Nullable Integer _delayTensOfMicroseconds;
  private @Nullable String _description;
  private @Nullable Integer _encapsulationVlan;
  private @Nullable InterfaceHsrp _hsrp;
  private @Nullable Lacp _lacp;
  private @Nullable Integer _mtu;
  private final @Nonnull String _name;
  private @Nullable Integer _nativeVlan;
  private @Nullable OspfInterface _ospf;
  private final @Nullable String _parentInterface;
  private @Nullable String _pbrPolicy;
  private final @Nonnull Set<InterfaceAddressWithAttributes> _secondaryAddresses;
  private @Nullable Boolean _shutdown;
  private @Nullable Integer _speedMbps;
  private @Nonnull SwitchportMode _switchportMode;
  private boolean _switchportMonitor;
  private final @Nonnull CiscoNxosInterfaceType _type;
  private final @Nullable Integer _vlan;
  private @Nullable String _vrfMember;

  private Interface(
      String name,
      @Nullable String parentInterface,
      CiscoNxosInterfaceType type,
      @Nullable Integer vlan) {
    _name = name;
    _parentInterface = parentInterface;
    _declaredNames = new HashSet<>();
    _secondaryAddresses = new HashSet<>();
    _type = type;
    _vlan = vlan;
    _autostate = true;
    _switchportMode = getDefaultSwitchportSettings(parentInterface != null, type);

    // Set defaults for individual switchport modes
    // - only effective when corresponding switchport mode is active
    _accessVlan = 1;
    _nativeVlan = 1;
    _allowedVlans = VLAN_RANGE;
  }

  public @Nullable Integer getAccessVlan() {
    return _accessVlan;
  }

  /** The primary IPv4 address of the interface. */
  public @Nullable InterfaceAddressWithAttributes getAddress() {
    return _address;
  }

  public @Nullable IntegerSpace getAllowedVlans() {
    return _allowedVlans;
  }

  public boolean getAutostate() {
    return _autostate;
  }

  /** Bandwidth in kbits */
  public @Nullable Integer getBandwidth() {
    return _bandwidth;
  }

  public @Nullable String getChannelGroup() {
    return _channelGroup;
  }

  public @Nonnull Set<String> getDeclaredNames() {
    return _declaredNames;
  }

  public @Nullable Integer getDelayTensOfMicroseconds() {
    return _delayTensOfMicroseconds;
  }

  public @Nullable String getDescription() {
    return _description;
  }

  public @Nullable Integer getEncapsulationVlan() {
    return _encapsulationVlan;
  }

  public @Nullable InterfaceHsrp getHsrp() {
    return _hsrp;
  }

  public @Nonnull InterfaceHsrp getOrCreateHsrp() {
    if (_hsrp == null) {
      _hsrp = new InterfaceHsrp();
    }
    return _hsrp;
  }

  public @Nullable Lacp getLacp() {
    return _lacp;
  }

  public @Nonnull Lacp getOrCreateLacp() {
    if (_lacp == null) {
      _lacp = new Lacp();
    }
    return _lacp;
  }

  public @Nullable Integer getMtu() {
    return _mtu;
  }

  public @Nonnull String getName() {
    return _name;
  }

  public @Nullable Integer getNativeVlan() {
    return _nativeVlan;
  }

  public @Nullable OspfInterface getOspf() {
    return _ospf;
  }

  public @Nonnull OspfInterface getOrCreateOspf() {
    if (_ospf == null) {
      _ospf = new OspfInterface();
    }
    return _ospf;
  }

  public @Nullable String getParentInterface() {
    return _parentInterface;
  }

  @Nullable
  public String getPbrPolicy() {
    return _pbrPolicy;
  }

  public void setPbrPolicy(@Nullable String pbrPolicy) {
    _pbrPolicy = pbrPolicy;
  }

  /** The set of secondary IPv4 addresses of the interface. */
  public @Nonnull Set<InterfaceAddressWithAttributes> getSecondaryAddresses() {
    return _secondaryAddresses;
  }

  public boolean getShutdown() {
    return _shutdown != null
        ? _shutdown
        : defaultShutdown(_switchportMode, _type, _parentInterface != null);
  }

  public @Nullable Integer getSpeedMbps() {
    return _speedMbps;
  }

  public void setSpeed(@Nullable Integer speedMbps) {
    _speedMbps = speedMbps;
  }

  @Nonnull
  public SwitchportMode getSwitchportMode() {
    return _switchportMode;
  }

  public boolean getSwitchportMonitor() {
    return _switchportMonitor;
  }

  public @Nonnull CiscoNxosInterfaceType getType() {
    return _type;
  }

  public @Nullable Integer getVlan() {
    return _vlan;
  }

  public @Nullable String getVrfMember() {
    return _vrfMember;
  }

  private static @Nonnull SwitchportMode getDefaultSwitchportSettings(
      boolean isSubinterface, CiscoNxosInterfaceType type) {
    switch (type) {
      case ETHERNET:
      case PORT_CHANNEL:
        if (isSubinterface) {
          // this is a subinterface
          return SwitchportMode.NONE;
        } else {
          // this is a parent interface
          return SwitchportMode.ACCESS;
        }

      case LOOPBACK:
      case MGMT:
      default:
        return SwitchportMode.NONE;
    }
  }

  public void setAccessVlan(@Nullable Integer accessVlan) {
    _accessVlan = accessVlan;
  }

  public void setAddress(@Nullable InterfaceAddressWithAttributes address) {
    _address = address;
  }

  public void setAllowedVlans(@Nullable IntegerSpace allowedVlans) {
    _allowedVlans = allowedVlans;
  }

  public void setAutostate(boolean autostate) {
    _autostate = autostate;
  }

  public void setBandwidth(@Nullable Integer bandwidth) {
    _bandwidth = bandwidth;
  }

  public void setChannelGroup(@Nullable String channelGroup) {
    _channelGroup = channelGroup;
  }

  public void setDelayTensOfMicroseconds(@Nullable Integer delayTensOfMicroseconds) {
    _delayTensOfMicroseconds = delayTensOfMicroseconds;
  }

  public void setDescription(@Nullable String description) {
    _description = description;
  }

  public void setEncapsulationVlan(@Nullable Integer encapsulationVlan) {
    _encapsulationVlan = encapsulationVlan;
  }

  public void setMtu(@Nullable Integer mtu) {
    _mtu = mtu;
  }

  public void setNativeVlan(@Nullable Integer nativeVlan) {
    _nativeVlan = nativeVlan;
  }

  public void setOspf(@Nullable OspfInterface ospf) {
    _ospf = ospf;
  }

  public void setShutdown(@Nullable Boolean shutdown) {
    _shutdown = shutdown;
  }

  public void setSwitchportMode(SwitchportMode switchportMode) {
    _switchportMode = switchportMode;
  }

  public void setSwitchportMonitor(boolean switchportMonitor) {
    _switchportMonitor = switchportMonitor;
  }

  public void setVrfMember(String vrfMember) {
    _vrfMember = vrfMember;
  }
}

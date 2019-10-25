package org.batfish.representation.juniper;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.VrrpGroup;

public class Interface implements Serializable {

  public static double getDefaultBandwidthByName(String name) {
    if (name.startsWith("xe")) {
      return 1E10;
    } else if (name.startsWith("ge")) {
      return 1E9;
    } else if (name.startsWith("fe")) {
      return 1E8;
    } else if (name.startsWith("irb")) {
      return 1E9;
    } else if (name.startsWith("et")) {
      return 1E11;
    } else {
      return 1E12;
    }
  }

  public @Nonnull Set<InterfaceOspfNeighbor> getOspfNeighbors() {
    return _ospfNeighbors;
  }

  /** Represents the type of interface for OSPF */
  public enum OspfInterfaceType {
    /** This is not an explicit type -- assumed by default */
    BROADCAST,
    /** non-broadcast multi-access */
    NBMA,
    /** Point to multipoint */
    P2MP,
    /** Point to multipoint over lan */
    P2MP_OVER_LAN,
    /** Point to point */
    P2P
  }

  private String _accessVlan;
  private boolean _active;
  private Set<Ip> _additionalArpIps;
  private final Set<ConcreteInterfaceAddress> _allAddresses;
  // Dumb name to appease checkstyle
  private String _agg8023adInterface;
  private final Set<Ip> _allAddressIps;
  private final List<SubRange> _allowedVlans;
  private final List<String> _allowedVlanNames;
  private double _bandwidth;
  private String _description;
  private @Nullable String _incomingFilter;
  private @Nullable List<String> _incomingFilterList;
  private transient boolean _inherited;
  @Nullable private IsisInterfaceSettings _isisSettings;
  private IsoAddress _isoAddress;
  private Integer _mtu;
  private final String _name;
  @Nullable private Integer _nativeVlan;
  private Ip _ospfArea;
  private Integer _ospfCost;
  @Nullable private Integer _ospfDeadInterval;
  @Nullable private Boolean _ospfDisable;
  @Nullable private Integer _ospfHelloInterval;
  private int _ospfHelloMultiplier;
  private boolean _ospfPassive;
  private OspfInterfaceType _ospfInterfaceType;
  private @Nonnull Set<InterfaceOspfNeighbor> _ospfNeighbors;
  private @Nullable String _outgoingFilter;
  private @Nullable List<String> _outgoingFilterList;
  private Interface _parent;
  private InterfaceAddress _preferredAddress;
  private ConcreteInterfaceAddress _primaryAddress;
  @Nullable private String _redundantParentInterface;
  private String _routingInstance;
  private SwitchportMode _switchportMode;
  private SwitchportEncapsulationType _switchportTrunkEncapsulation;
  private final SortedMap<String, Interface> _units;
  private final SortedMap<Integer, VrrpGroup> _vrrpGroups;
  private Integer _tcpMss;

  public Interface(String name) {
    _active = true;
    _additionalArpIps = ImmutableSet.of();
    _allAddresses = new LinkedHashSet<>();
    _allAddressIps = new LinkedHashSet<>();
    _bandwidth = getDefaultBandwidthByName(name);
    _name = name;
    _ospfInterfaceType = OspfInterfaceType.BROADCAST;
    _ospfNeighbors = new HashSet<>();
    _switchportMode = SwitchportMode.NONE;
    _switchportTrunkEncapsulation = SwitchportEncapsulationType.DOT1Q;
    _allowedVlans = new LinkedList<>();
    _allowedVlanNames = new LinkedList<>();
    _ospfCost = null;
    _units = new TreeMap<>();
    _vrrpGroups = new TreeMap<>();
  }

  public String get8023adInterface() {
    return _agg8023adInterface;
  }

  public String getAccessVlan() {
    return _accessVlan;
  }

  public boolean getActive() {
    return _active;
  }

  public Set<Ip> getAdditionalArpIps() {
    return _additionalArpIps;
  }

  public Set<ConcreteInterfaceAddress> getAllAddresses() {
    return _allAddresses;
  }

  public Set<Ip> getAllAddressIps() {
    return _allAddressIps;
  }

  public List<SubRange> getAllowedVlans() {
    return _allowedVlans;
  }

  public List<String> getAllowedVlanNames() {
    return _allowedVlanNames;
  }

  public double getBandwidth() {
    return _bandwidth;
  }

  public String getDescription() {
    return _description;
  }

  public @Nullable String getIncomingFilter() {
    return _incomingFilter;
  }

  public @Nullable List<String> getIncomingFilterList() {
    return _incomingFilterList;
  }

  @Nullable
  public IsisInterfaceSettings getIsisSettings() {
    return _isisSettings;
  }

  public IsoAddress getIsoAddress() {
    return _isoAddress;
  }

  @Nullable
  public Integer getMtu() {
    return _mtu;
  }

  public String getName() {
    return _name;
  }

  @Nullable
  public Integer getNativeVlan() {
    return _nativeVlan;
  }

  public Ip getOspfArea() {
    return _ospfArea;
  }

  public Integer getOspfCost() {
    return _ospfCost;
  }

  /** Get the time (in seconds) to wait before neighbors are declared dead */
  @Nullable
  public Integer getOspfDeadInterval() {
    return _ospfDeadInterval;
  }

  @Nullable
  public Boolean getOspfDisable() {
    return _ospfDisable;
  }

  /** Get the time (in seconds) between sending hello messages to neighbors */
  @Nullable
  public Integer getOspfHelloInterval() {
    return _ospfHelloInterval;
  }

  public int getOspfHelloMultiplier() {
    return _ospfHelloMultiplier;
  }

  public OspfInterfaceType getOspfInterfaceType() {
    return _ospfInterfaceType;
  }

  public boolean getOspfPassive() {
    return _ospfPassive;
  }

  public @Nullable String getOutgoingFilter() {
    return _outgoingFilter;
  }

  public @Nullable List<String> getOutgoingFilterList() {
    return _outgoingFilterList;
  }

  public Interface getParent() {
    return _parent;
  }

  public InterfaceAddress getPreferredAddress() {
    return _preferredAddress;
  }

  public ConcreteInterfaceAddress getPrimaryAddress() {
    return _primaryAddress;
  }

  @Nullable
  public String getRedundantParentInterface() {
    return _redundantParentInterface;
  }

  public String getRoutingInstance() {
    return _routingInstance;
  }

  public SwitchportMode getSwitchportMode() {
    return _switchportMode;
  }

  public SwitchportEncapsulationType getSwitchportTrunkEncapsulation() {
    return _switchportTrunkEncapsulation;
  }

  public Map<String, Interface> getUnits() {
    return _units;
  }

  public SortedMap<Integer, VrrpGroup> getVrrpGroups() {
    return _vrrpGroups;
  }

  public void inheritUnsetFields() {
    if (_parent == null || _inherited) {
      return;
    }
    _inherited = true;
    _parent.inheritUnsetFields();
    if (_description == null) {
      _description = _parent._description;
    }
    if (_mtu == null) {
      _mtu = _parent._mtu;
    }
    if (_ospfCost == null) {
      _ospfCost = _parent._ospfCost;
    }
    if (_ospfArea == null) {
      _ospfArea = _parent._ospfArea;
    }
    if (_ospfDisable == null) {
      _ospfDisable = _parent._ospfDisable;
    }
  }

  /**
   * Copies the values of fields associated with physical interfaces from {@code bestower} to this
   * interface.
   *
   * <p>TODO: This list is incomplete. We don't have a clean separation of which properties are
   * physical only
   */
  public void inheritUnsetPhysicalFields(Interface bestower) {
    if (_agg8023adInterface == null) {
      _agg8023adInterface = bestower._agg8023adInterface;
    }
    if (_description == null) {
      _description = bestower._description;
    }
    if (_mtu == null) {
      _mtu = bestower._mtu;
    }
    if (_redundantParentInterface == null) {
      _redundantParentInterface = bestower._redundantParentInterface;
    }
  }

  /** Initializes {@link IsisInterfaceSettings} for this interface if not already initialized */
  public void initIsisSettings() {
    if (_isisSettings == null) {
      _isisSettings = new IsisInterfaceSettings();
    }
  }

  public void set8023adInterface(String interfaceName) {
    _agg8023adInterface = interfaceName;
  }

  public void setAccessVlan(String vlan) {
    _accessVlan = vlan;
  }

  public void setActive(boolean active) {
    _active = active;
  }

  public void setAdditionalArpIps(Iterable<Ip> additionalArpIps) {
    _additionalArpIps = ImmutableSet.copyOf(additionalArpIps);
  }

  public void setBandwidth(double bandwidth) {
    _bandwidth = bandwidth;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setIncomingFilter(@Nullable String accessListName) {
    _incomingFilter = accessListName;
    _incomingFilterList = null;
  }

  public void addIncomingFilterList(@Nonnull String accessListName) {
    _incomingFilter = null;
    if (_incomingFilterList == null) {
      _incomingFilterList = new LinkedList<>();
    }
    _incomingFilterList.add(accessListName);
  }

  public void setIsoAddress(IsoAddress address) {
    _isoAddress = address;
  }

  public void setMtu(Integer mtu) {
    _mtu = mtu;
  }

  public void setNativeVlan(Integer vlan) {
    _nativeVlan = vlan;
  }

  public void setOspfArea(Ip ospfArea) {
    _ospfArea = ospfArea;
  }

  public void setOspfCost(int ospfCost) {
    _ospfCost = ospfCost;
  }

  public void setOspfDeadInterval(int seconds) {
    _ospfDeadInterval = seconds;
  }

  public void setOspfDisable(boolean disable) {
    _ospfDisable = disable;
  }

  public void setOspfHelloInterval(int seconds) {
    _ospfHelloInterval = seconds;
  }

  public void setOspfHelloMultiplier(int multiplier) {
    _ospfHelloMultiplier = multiplier;
  }

  public void setOspfPassive(boolean ospfPassive) {
    _ospfPassive = true;
  }

  public void setOspfInterfaceType(OspfInterfaceType ospfInterfaceType) {
    _ospfInterfaceType = ospfInterfaceType;
  }

  public void setOutgoingFilter(@Nullable String accessListName) {
    _outgoingFilter = accessListName;
    _outgoingFilterList = null;
  }

  public void addOutgoingFilterList(@Nonnull String accessListName) {
    _outgoingFilter = null;
    if (_outgoingFilterList == null) {
      _outgoingFilterList = new LinkedList<>();
    }
    _outgoingFilterList.add(accessListName);
  }

  public void setParent(Interface parent) {
    _parent = parent;
  }

  public void setPreferredAddress(InterfaceAddress address) {
    _preferredAddress = address;
  }

  public void setPrimaryAddress(ConcreteInterfaceAddress address) {
    _primaryAddress = address;
  }

  public void setRedundantParentInterface(@Nullable String redundantParentInterface) {
    _redundantParentInterface = redundantParentInterface;
  }

  public void setRoutingInstance(String routingInstance) {
    _routingInstance = routingInstance;
  }

  public void setSwitchportMode(SwitchportMode switchportMode) {
    _switchportMode = switchportMode;
  }

  public void setSwitchportTrunkEncapsulation(SwitchportEncapsulationType encapsulation) {
    _switchportTrunkEncapsulation = encapsulation;
  }

  public void setTcpMss(@Nullable Integer tcpMss) {
    _tcpMss = tcpMss;
  }

  public @Nullable Integer getTcpMss() {
    return _tcpMss;
  }

  @Override
  public String toString() {
    return _name + " parent=" + _parent;
  }
}

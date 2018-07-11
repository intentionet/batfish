package org.batfish.representation.cisco;

import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.isis.IsisInterfaceMode;

public class Interface extends ComparableStructure<String> {

  private static final double ARISTA_ETHERNET_BANDWIDTH = 1E9;

  private static final double DEFAULT_INTERFACE_BANDWIDTH = 1E12;

  private static final int DEFAULT_INTERFACE_MTU = 1500;

  private static final double FAST_ETHERNET_BANDWIDTH = 100E6;

  private static final double GIGABIT_ETHERNET_BANDWIDTH = 1E9;

  private static final double IOS_ETHERNET_BANDWIDTH = 1E7;

  private static final double LONG_REACH_ETHERNET_BANDWIDTH = 10E6;

  /** dirty hack: just chose a very large number */
  private static final double LOOPBACK_BANDWIDTH = 1E12;

  /** NX-OS Ethernet 802.3z - may not apply for non-NX-OS */
  private static final double NXOS_ETHERNET_BANDWIDTH = 1E9;

  private static final long serialVersionUID = 1L;

  private static final double TEN_GIGABIT_ETHERNET_BANDWIDTH = 10E9;

  public static double getDefaultBandwidth(String name, ConfigurationFormat format) {
    Double bandwidth = null;
    if (name.startsWith("Ethernet")) {
      switch (format) {
        case ARISTA:
          return ARISTA_ETHERNET_BANDWIDTH;

        case ALCATEL_AOS:
        case ARUBAOS: // TODO: verify https://github.com/batfish/batfish/issues/1548
        case CADANT:
        case CISCO_ASA:
        case CISCO_IOS:
        case CISCO_IOS_XR:
        case FORCE10:
        case FOUNDRY:
          return IOS_ETHERNET_BANDWIDTH;

        case CISCO_NX:
          return NXOS_ETHERNET_BANDWIDTH;

        case AWS:
        case BLADENETWORK:
        case EMPTY:
        case F5:
        case FLAT_JUNIPER:
        case FLAT_VYOS:
        case HOST:
        case IGNORED:
        case IPTABLES:
        case JUNIPER:
        case JUNIPER_SWITCH:
        case MRV:
        case MRV_COMMANDS:
        case MSS:
        case UNKNOWN:
        case VXWORKS:
        case VYOS:
        default:
          throw new BatfishException("Unuspported format: " + format);
      }
    } else if (name.startsWith("FastEthernet")) {
      bandwidth = FAST_ETHERNET_BANDWIDTH;
    } else if (name.startsWith("GigabitEthernet")) {
      bandwidth = GIGABIT_ETHERNET_BANDWIDTH;
    } else if (name.startsWith("LongReachEthernet")) {
      bandwidth = LONG_REACH_ETHERNET_BANDWIDTH;
    } else if (name.startsWith("TenGigabitEthernet")) {
      bandwidth = TEN_GIGABIT_ETHERNET_BANDWIDTH;
    } else if (name.startsWith("Vlan")) {
      bandwidth = null;
    } else if (name.startsWith("Loopback")) {
      bandwidth = LOOPBACK_BANDWIDTH;
    } else if (name.startsWith("Bundle-Ethernet") || name.startsWith("Port-Channel")) {
      bandwidth = 0D;
    }
    if (bandwidth == null) {
      bandwidth = DEFAULT_INTERFACE_BANDWIDTH;
    }
    return bandwidth;
  }

  public static int getDefaultMtu() {
    return DEFAULT_INTERFACE_MTU;
  }

  private int _accessVlan;

  private boolean _active;

  private List<SubRange> _allowedVlans;

  private boolean _autoState;

  private Double _bandwidth;

  private String _channelGroup;

  private String _cryptoMap;

  private String _description;

  private SortedSet<Ip> _dhcpRelayAddresses;

  private boolean _dhcpRelayClient;

  private String _incomingFilter;

  private int _incomingFilterLine;

  @Nullable private Long _isisCost;

  @Nullable private IsisInterfaceMode _isisInterfaceMode;

  private int _mtu;

  private int _nativeVlan;

  private boolean _ospfActive;

  private Long _ospfArea;

  private Integer _ospfCost;

  private int _ospfDeadInterval;

  private int _ospfHelloMultiplier;

  private boolean _ospfPassive;

  private boolean _ospfPointToPoint;

  private boolean _ospfShutdown;

  private String _outgoingFilter;

  private int _outgoingFilterLine;

  private InterfaceAddress _address;

  private boolean _proxyArp;

  private String _routingPolicy;

  int _routingPolicyLine;

  private Set<InterfaceAddress> _secondaryAddresses;

  private List<CiscoSourceNat> _sourceNats;

  private boolean _spanningTreePortfast;

  private InterfaceAddress _standbyAddress;

  private Boolean _switchport;

  private boolean _switchportAccessDynamic;

  private SwitchportMode _switchportMode;

  private SwitchportEncapsulationType _switchportTrunkEncapsulation;

  private Tunnel _tunnel;

  private String _vrf;

  private SortedSet<String> _declaredNames;

  private String _securityZone;

  public String getSecurityZone() {
    return _securityZone;
  }

  public Interface(String name, CiscoConfiguration c) {
    super(name);
    _active = true;
    _autoState = true;
    _allowedVlans = new ArrayList<>();
    _declaredNames = ImmutableSortedSet.of();
    _dhcpRelayAddresses = new TreeSet<>();
    _isisInterfaceMode = IsisInterfaceMode.UNSET;
    _nativeVlan = 1;
    _secondaryAddresses = new LinkedHashSet<>();
    SwitchportMode defaultSwitchportMode = c.getCf().getDefaultSwitchportMode();
    ConfigurationFormat vendor = c.getVendor();
    if (defaultSwitchportMode == null) {
      switch (vendor) {
        case ARISTA:
          _switchportMode = SwitchportMode.ACCESS;
          break;

        case ALCATEL_AOS:
        case ARUBAOS: // TODO: verify https://github.com/batfish/batfish/issues/1548
        case AWS:
        case CADANT:
        case CISCO_ASA:
        case CISCO_IOS:
        case CISCO_IOS_XR:
        case CISCO_NX:
        case FORCE10:
        case FOUNDRY:
          _switchportMode = SwitchportMode.NONE;
          break;

          // $CASES-OMITTED$
        default:
          throw new BatfishException(
              "Invalid vendor format for cisco parser: " + vendor.getVendorString());
      }
    } else {
      _switchportMode = defaultSwitchportMode;
    }
    _spanningTreePortfast = c.getSpanningTreePortfastDefault();
  }

  public void addAllowedRanges(List<SubRange> ranges) {
    _allowedVlans.addAll(ranges);
  }

  public int getAccessVlan() {
    return _accessVlan;
  }

  public boolean getActive() {
    return _active;
  }

  public List<SubRange> getAllowedVlans() {
    return _allowedVlans;
  }

  public Set<InterfaceAddress> getAllAddresses() {
    Set<InterfaceAddress> allAddresses = new TreeSet<>();
    if (_address != null) {
      allAddresses.add(_address);
    }
    allAddresses.addAll(_secondaryAddresses);
    return allAddresses;
  }

  public boolean getAutoState() {
    return _autoState;
  }

  public Double getBandwidth() {
    return _bandwidth;
  }

  public String getChannelGroup() {
    return _channelGroup;
  }

  public String getCryptoMap() {
    return _cryptoMap;
  }

  public String getDescription() {
    return _description;
  }

  public SortedSet<Ip> getDhcpRelayAddresses() {
    return _dhcpRelayAddresses;
  }

  public boolean getDhcpRelayClient() {
    return _dhcpRelayClient;
  }

  public String getIncomingFilter() {
    return _incomingFilter;
  }

  public int getIncomingFilterLine() {
    return _incomingFilterLine;
  }

  public Long getIsisCost() {
    return _isisCost;
  }

  public IsisInterfaceMode getIsisInterfaceMode() {
    return _isisInterfaceMode;
  }

  public int getMtu() {
    return _mtu;
  }

  public int getNativeVlan() {
    return _nativeVlan;
  }

  public boolean getOspfActive() {
    return _ospfActive;
  }

  public Long getOspfArea() {
    return _ospfArea;
  }

  public Integer getOspfCost() {
    return _ospfCost;
  }

  public int getOspfDeadInterval() {
    return _ospfDeadInterval;
  }

  public int getOspfHelloMultiplier() {
    return _ospfHelloMultiplier;
  }

  public boolean getOspfPassive() {
    return _ospfPassive;
  }

  public boolean getOspfPointToPoint() {
    return _ospfPointToPoint;
  }

  public boolean getOspfShutdown() {
    return _ospfShutdown;
  }

  public String getOutgoingFilter() {
    return _outgoingFilter;
  }

  public int getOutgoingFilterLine() {
    return _outgoingFilterLine;
  }

  public InterfaceAddress getAddress() {
    return _address;
  }

  public boolean getProxyArp() {
    return _proxyArp;
  }

  public String getRoutingPolicy() {
    return _routingPolicy;
  }

  public int getRoutingPolicyLine() {
    return _routingPolicyLine;
  }

  public Set<InterfaceAddress> getSecondaryAddresses() {
    return _secondaryAddresses;
  }

  public List<CiscoSourceNat> getSourceNats() {
    return _sourceNats;
  }

  public boolean getSpanningTreePortfast() {
    return _spanningTreePortfast;
  }

  public InterfaceAddress getStandbyAddress() {
    return _standbyAddress;
  }

  public Boolean getSwitchport() {
    return _switchport;
  }

  public boolean getSwitchportAccessDynamic() {
    return _switchportAccessDynamic;
  }

  public SwitchportMode getSwitchportMode() {
    return _switchportMode;
  }

  public SwitchportEncapsulationType getSwitchportTrunkEncapsulation() {
    return _switchportTrunkEncapsulation;
  }

  public Tunnel getTunnel() {
    return _tunnel;
  }

  public Tunnel getTunnelInitIfNull() {
    if (_tunnel == null) {
      _tunnel = new Tunnel();
    }
    return _tunnel;
  }

  public String getVrf() {
    return _vrf;
  }

  public void setAccessVlan(int vlan) {
    _accessVlan = vlan;
  }

  public void setActive(boolean active) {
    _active = active;
  }

  public void setAutoState(boolean autoState) {
    _autoState = autoState;
  }

  public void setBandwidth(Double bandwidth) {
    _bandwidth = bandwidth;
  }

  public void setChannelGroup(String channelGroup) {
    _channelGroup = channelGroup;
  }

  public void setCryptoMap(String cryptoMap) {
    _cryptoMap = cryptoMap;
  }

  public void setDescription(String description) {
    _description = description;
  }

  public void setDhcpRelayAddress(SortedSet<Ip> dhcpRelayAddress) {
    _dhcpRelayAddresses = dhcpRelayAddress;
  }

  public void setDhcpRelayClient(boolean dhcpRelayClient) {
    _dhcpRelayClient = dhcpRelayClient;
  }

  public void setIncomingFilter(String accessListName) {
    _incomingFilter = accessListName;
  }

  public void setIncomingFilterLine(int incomingFilterLine) {
    _incomingFilterLine = incomingFilterLine;
  }

  public void setIsisCost(Long isisCost) {
    _isisCost = isisCost;
  }

  public void setIsisInterfaceMode(IsisInterfaceMode mode) {
    _isisInterfaceMode = mode;
  }

  public void setMtu(int mtu) {
    _mtu = mtu;
  }

  public void setNativeVlan(int vlan) {
    _nativeVlan = vlan;
  }

  public void setOspfActive(boolean ospfActive) {
    _ospfActive = ospfActive;
  }

  public void setOspfArea(Long ospfArea) {
    _ospfArea = ospfArea;
  }

  public void setOspfCost(int ospfCost) {
    _ospfCost = ospfCost;
  }

  public void setOspfDeadInterval(int seconds) {
    _ospfDeadInterval = seconds;
  }

  public void setOspfHelloMultiplier(int multiplier) {
    _ospfHelloMultiplier = multiplier;
  }

  public void setOspfPassive(boolean ospfPassive) {
    _ospfPassive = ospfPassive;
  }

  public void setOspfPointToPoint(boolean ospfPointToPoint) {
    _ospfPointToPoint = ospfPointToPoint;
  }

  public void setOspfShutdown(boolean ospfShutdown) {
    _ospfShutdown = ospfShutdown;
  }

  public void setOutgoingFilter(String accessListName) {
    _outgoingFilter = accessListName;
  }

  public void setOutgoingFilterLine(int outgoingFilterLine) {
    _outgoingFilterLine = outgoingFilterLine;
  }

  public void setAddress(InterfaceAddress address) {
    _address = address;
  }

  public void setProxyArp(boolean proxyArp) {
    _proxyArp = proxyArp;
  }

  public void setRoutingPolicy(String routingPolicy) {
    _routingPolicy = routingPolicy;
  }

  public void setRoutingPolicyLine(int routingPolicyLine) {
    _routingPolicyLine = routingPolicyLine;
  }

  public void setSourceNats(List<CiscoSourceNat> sourceNats) {
    _sourceNats = sourceNats;
  }

  public void setSpanningTreePortfast(boolean spanningTreePortfast) {
    _spanningTreePortfast = spanningTreePortfast;
  }

  public void setStandbyAddress(InterfaceAddress standbyAddress) {
    _standbyAddress = standbyAddress;
  }

  public void setSwitchport(boolean switchport) {
    _switchport = switchport;
  }

  public void setSwitchport(Boolean switchport) {
    _switchport = switchport;
  }

  public void setSwitchportAccessDynamic(boolean switchportAccessDynamic) {
    _switchportAccessDynamic = switchportAccessDynamic;
  }

  public void setSwitchportMode(SwitchportMode switchportMode) {
    _switchportMode = switchportMode;
  }

  public void setSwitchportTrunkEncapsulation(SwitchportEncapsulationType encapsulation) {
    _switchportTrunkEncapsulation = encapsulation;
  }

  public void setTunnel(Tunnel tunnel) {
    _tunnel = tunnel;
  }

  public void setVrf(String vrf) {
    _vrf = vrf;
  }

  public SortedSet<String> getDeclaredNames() {
    return _declaredNames;
  }

  public void setDeclaredNames(SortedSet<String> declaredNames) {
    _declaredNames = ImmutableSortedSet.copyOf(declaredNames);
  }

  public void setSecurityZone(String securityZone) {
    _securityZone = securityZone;
  }
}

package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public final class Interface extends ComparableStructure<String> {

  public static class Builder extends NetworkFactoryBuilder<Interface> {

    private boolean _active;

    private InterfaceAddress _address;

    @Nullable private Double _bandwidth;

    private boolean _blacklisted;

    private SortedSet<String> _declaredNames;

    private IpAccessList _incomingFilter;

    private IsisInterfaceSettings _isis;

    private String _name;

    private OspfArea _ospfArea;

    private Integer _ospfCost;

    private boolean _ospfEnabled;

    private boolean _ospfPassive;

    private boolean _ospfPointToPoint;

    private IpAccessList _outgoingFilter;

    private Configuration _owner;

    private boolean _proxyArp;

    private Set<InterfaceAddress> _secondaryAddresses;

    private List<SourceNat> _sourceNats;

    private SortedSet<Ip> _additionalArpIps;

    private Vrf _vrf;

    private SortedMap<Integer, VrrpGroup> _vrrpGroups;

    Builder(NetworkFactory networkFactory) {
      super(networkFactory, Interface.class);
      _additionalArpIps = ImmutableSortedSet.of();
      _declaredNames = ImmutableSortedSet.of();
      _secondaryAddresses = ImmutableSet.of();
      _sourceNats = ImmutableList.of();
      _vrrpGroups = ImmutableSortedMap.of();
    }

    @Override
    public Interface build() {
      String name = _name != null ? _name : generateName();
      Interface iface = new Interface(name, _owner);
      ImmutableSet.Builder<InterfaceAddress> allAddresses = ImmutableSet.builder();
      iface.setActive(_active);
      if (_address != null) {
        iface.setAddress(_address);
        allAddresses.add(_address);
      }
      iface.setAdditionalArpIps(_additionalArpIps);
      iface.setAllAddresses(allAddresses.addAll(_secondaryAddresses).build());
      iface.setBandwidth(_bandwidth);
      iface.setBlacklisted(_blacklisted);
      iface.setDeclaredNames(_declaredNames);
      iface.setIncomingFilter(_incomingFilter);
      iface.setIsis(_isis);
      iface.setOspfArea(_ospfArea);
      if (_ospfArea != null) {
        _ospfArea.getInterfaces().add(name);
        iface.setOspfAreaName(_ospfArea.getName());
      }
      iface.setOspfCost(_ospfCost);
      iface.setOspfEnabled(_ospfEnabled);
      iface.setOspfPassive(_ospfPassive);
      iface.setOspfPointToPoint(_ospfPointToPoint);
      iface.setOutgoingFilter(_outgoingFilter);
      iface.setOwner(_owner);
      if (_owner != null) {
        _owner.getInterfaces().put(name, iface);
      }
      iface.setProxyArp(_proxyArp);
      iface.setSourceNats(_sourceNats);
      iface.setVrf(_vrf);
      if (_vrf != null) {
        _vrf.getInterfaces().put(name, iface);
        OspfProcess proc = _vrf.getOspfProcess();
        if (proc != null && _active) {
          iface.setOspfCost(proc.computeInterfaceCost(iface));
        }
      }
      iface.setVrrpGroups(_vrrpGroups);
      return iface;
    }

    public Builder setActive(boolean active) {
      _active = active;
      return this;
    }

    public Builder setAdditionalArpIps(Iterable<Ip> additionalArpIps) {
      _additionalArpIps = ImmutableSortedSet.copyOf(additionalArpIps);
      return this;
    }

    /**
     * Set the primary address of the interface. <br>
     * The {@link Interface#getAllAddresses()} method of the built {@link Interface} will return a
     * set containing the primary address and secondary addresses. <br>
     * The node will accept traffic whose destination IP belongs is among any of the addresses of
     * any of the interfaces. The primary address is the one used by default as the source IP for
     * traffic sent out the interface. A secondary address is another address potentially associated
     * with a different subnet living on the interface. The interface will reply to ARP for the
     * primary or any secondary IP.
     */
    public Builder setAddress(InterfaceAddress address) {
      _address = address;
      return this;
    }

    /**
     * Set the primary address and secondary addresses of the interface. <br>
     * The {@link Interface#getAllAddresses()} method of the built {@link Interface} will return a
     * set containing the primary address and secondary addresses.<br>
     * The node will accept traffic whose destination IP is among any of the addresses of any of the
     * interfaces. The primary address is the one used by default as the source IP for traffic sent
     * out the interface. A secondary address is another address potentially associated with a
     * different subnet living on the interface. The interface will reply to ARP for the primary or
     * any secondary IP.
     */
    public Builder setAddresses(
        InterfaceAddress primaryAddress, InterfaceAddress... secondaryAddresses) {
      return setAddresses(primaryAddress, Arrays.asList(secondaryAddresses));
    }

    /**
     * Set the primary address and secondary addresses of the interface. <br>
     * The {@link Interface#getAllAddresses()} method of the built {@link Interface} will return a
     * set containing the primary address and secondary addresses.<br>
     * The node will accept traffic whose destination IP belongs is among any of the addresses of
     * any of the interfaces. The primary address is the one used by default as the source IP for
     * traffic sent out the interface. A secondary address is another address potentially associated
     * with a different subnet living on the interface. The interface will reply to ARP for the
     * primary or any secondary IP.
     */
    public Builder setAddresses(
        InterfaceAddress primaryAddress, Iterable<InterfaceAddress> secondaryAddresses) {
      _address = primaryAddress;
      _secondaryAddresses = ImmutableSet.copyOf(secondaryAddresses);
      return this;
    }

    public Builder setBandwidth(@Nullable Double bandwidth) {
      _bandwidth = bandwidth;
      return this;
    }

    public Builder setBlacklisted(boolean blacklisted) {
      _blacklisted = blacklisted;
      return this;
    }

    public Builder setDeclaredNames(Iterable<String> declaredNames) {
      _declaredNames = ImmutableSortedSet.copyOf(declaredNames);
      return this;
    }

    public Builder setIncomingFilter(IpAccessList incomingFilter) {
      _incomingFilter = incomingFilter;
      return this;
    }

    public Builder setIsis(IsisInterfaceSettings isis) {
      _isis = isis;
      return this;
    }

    public Builder setName(String name) {
      _name = name;
      return this;
    }

    public Builder setOspfArea(OspfArea ospfArea) {
      _ospfArea = ospfArea;
      return this;
    }

    public Builder setOspfCost(Integer ospfCost) {
      _ospfCost = ospfCost;
      return this;
    }

    public Builder setOspfEnabled(boolean ospfEnabled) {
      _ospfEnabled = ospfEnabled;
      return this;
    }

    public Builder setOspfPassive(boolean ospfPassive) {
      _ospfPassive = ospfPassive;
      return this;
    }

    public Builder setOspfPointToPoint(boolean ospfPointToPoint) {
      _ospfPointToPoint = ospfPointToPoint;
      return this;
    }

    public Builder setOutgoingFilter(IpAccessList outgoingFilter) {
      _outgoingFilter = outgoingFilter;
      return this;
    }

    public Builder setOwner(Configuration owner) {
      _owner = owner;
      return this;
    }

    public Builder setProxyArp(boolean proxyArp) {
      _proxyArp = proxyArp;
      return this;
    }

    /**
     * Set the secondary addresses of the interface. <br>
     * The {@link Interface#getAllAddresses()} method of the built {@link Interface} will return a
     * set containing the primary address and secondary addresses.<br>
     * The node will accept traffic whose destination IP belongs is among any of the addresses of
     * any of the interfaces. The primary address is the one used by default as the source IP for
     * traffic sent out the interface. A secondary address is another address potentially associated
     * with a different subnet living on the interface. The interface will reply to ARP for the
     * primary or any secondary IP.
     */
    public Builder setSecondaryAddresses(Iterable<InterfaceAddress> secondaryAddresses) {
      _secondaryAddresses = ImmutableSet.copyOf(secondaryAddresses);
      return this;
    }

    public Builder setSourceNats(List<SourceNat> sourceNats) {
      _sourceNats = sourceNats;
      return this;
    }

    public Builder setVrf(Vrf vrf) {
      _vrf = vrf;
      return this;
    }

    public Builder setVrrpGroups(SortedMap<Integer, VrrpGroup> vrrpGroups) {
      _vrrpGroups = ImmutableSortedMap.copyOf(vrrpGroups);
      return this;
    }
  }

  private static final int DEFAULT_MTU = 1500;

  public static final String FLOW_SINK_TERMINATION_NAME = "flow_sink_termination";

  public static final String NULL_INTERFACE_NAME = "null_interface";

  public static final String UNSET_LOCAL_INTERFACE = "unset_local_interface";

  public static final String INVALID_LOCAL_INTERFACE = "invalid_local_interface";

  private static final String PROP_ACCESS_VLAN = "accessVlan";

  private static final String PROP_ACTIVE = "active";

  private static final String PROP_ADDITIONAL_ARP_IPS = "additionalArpIps";

  private static final String PROP_ALL_PREFIXES = "allPrefixes";

  private static final String PROP_ALLOWED_VLANS = "allowedVlans";

  private static final String PROP_AUTOSTATE = "autostate";

  private static final String PROP_BANDWIDTH = "bandwidth";

  private static final String PROP_CHANNEL_GROUP = "channelGroup";

  private static final String PROP_CHANNEL_GROUP_MEMBERS = "channelGroupMembers";

  private static final String PROP_CRYPTO_MAP = "cryptoMap";

  private static final String PROP_DECLARED_NAMES = "declaredNames";

  private static final String PROP_DESCRIPTION = "description";

  private static final String PROP_DHCP_RELAY_ADDRESSES = "dhcpRelayAddresses";

  private static final String PROP_INBOUND_FILTER = "inboundFilter";

  private static final String PROP_INCOMING_FILTER = "incomingFilter";

  private static final String PROP_INTERFACE_TYPE = "type";

  private static final String PROP_ISIS = "isis";

  private static final String PROP_ISIS_COST = "isisCost";

  private static final String PROP_ISIS_L1_INTERFACE_MODE = "isisL1InterfaceMode";

  private static final String PROP_ISIS_L2_INTERFACE_MODE = "isisL2InterfaceMode";

  private static final String PROP_MTU = "mtu";

  private static final String PROP_NATIVE_VLAN = "nativeVlan";

  private static final String PROP_OSPF_AREA = "ospfArea";

  private static final String PROP_OSPF_COST = "ospfCost";

  private static final String PROP_OSPF_DEAD_INTERVAL = "ospfDeadInterval";

  private static final String PROP_OSPF_ENABLED = "ospfEnabled";

  private static final String PROP_OSPF_HELLO_MULTIPLIER = "ospfHelloMultiplier";

  private static final String PROP_OSPF_PASSIVE = "ospfPassive";

  private static final String PROP_OSPF_POINT_TO_POINT = "ospfPointToPoint";

  private static final String PROP_OUTGOING_FILTER = "outgoingFilter";

  private static final String PROP_PREFIX = "prefix";

  private static final String PROP_RIP_ENABLED = "ripEnabled";

  private static final String PROP_RIP_PASSIVE = "ripPassive";

  private static final String PROP_ROUTING_POLICY = "routingPolicy";

  private static final String PROP_SOURCE_NATS = "sourceNats";

  private static final String PROP_SPANNING_TREE_PORTFAST = "spanningTreePortfast";

  private static final String PROP_SWITCHPORT = "switchport";

  private static final String PROP_SWITCHPORT_MODE = "switchportMode";

  private static final String PROP_SWITCHPORT_TRUNK_ENCAPSULATION = "switchportTrunkEncapsulation";

  private static final String PROP_VLAN = "vlan";

  private static final String PROP_VRF = "vrf";

  private static final String PROP_VRRP_GROUPS = "vrrpGroups";

  private static final String PROP_ZONE = "zone";

  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder(null);
  }

  private static InterfaceType computeAosInteraceType(String name) {
    if (name.startsWith("vlan")) {
      return InterfaceType.VLAN;
    } else if (name.startsWith("loopback")) {
      return InterfaceType.LOOPBACK;
    } else {
      return InterfaceType.PHYSICAL;
    }
  }

  private static InterfaceType computeAwsInterfaceType(String name) {
    if (name.startsWith("v")) {
      return InterfaceType.VPN;
    } else {
      return InterfaceType.PHYSICAL;
    }
  }

  private static InterfaceType computeCiscoInterfaceType(String name) {
    if (name.startsWith("Async")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("ATM")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Bundle-Ethernet")) {
      return InterfaceType.AGGREGATED;
    } else if (name.startsWith("cmp-mgmt")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Crypto-Engine")) {
      return InterfaceType.VPN; // IPSec VPN
    } else if (name.startsWith("Dialer")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Dot11Radio")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Embedded-Service-Engine")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Ethernet")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("FastEthernet")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("FortyGigabitEthernet")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("GigabitEthernet")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("GMPLS")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("HundredGigabitEthernet")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Group-Async")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Loopback")) {
      return InterfaceType.LOOPBACK;
    } else if (name.startsWith("Management")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("mgmt")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("MgmtEth")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Null")) {
      return InterfaceType.NULL;
    } else if (name.startsWith("nve")) {
      return InterfaceType.VLAN;
    } else if (name.startsWith("Port-Channel")) {
      return InterfaceType.AGGREGATED;
    } else if (name.startsWith("POS")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Serial")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("TenGigabitEthernet")) {
      return InterfaceType.PHYSICAL;
    } else if (name.startsWith("Tunnel")) {
      return InterfaceType.TUNNEL;
    } else if (name.startsWith("tunnel-ip")) {
      return InterfaceType.TUNNEL;
    } else if (name.startsWith("tunnel-te")) {
      return InterfaceType.TUNNEL;
    } else if (name.startsWith("Vlan")) {
      return InterfaceType.VLAN;
    } else if (name.startsWith("Vxlan")) {
      return InterfaceType.TUNNEL;
    } else {
      return InterfaceType.UNKNOWN;
    }
  }

  private static InterfaceType computeHostInterfaceType(String name) {
    if (name.startsWith("lo")) {
      return InterfaceType.LOOPBACK;
    } else {
      return InterfaceType.PHYSICAL;
    }
  }

  public static InterfaceType computeInterfaceType(String name, ConfigurationFormat format) {
    switch (format) {
      case ALCATEL_AOS:
        return computeAosInteraceType(name);

      case AWS:
        return computeAwsInterfaceType(name);

      case ARISTA:
      case ARUBAOS:
      case CADANT:
      case CISCO_ASA:
      case CISCO_IOS:
      case CISCO_IOS_XR:
      case CISCO_NX:
      case FOUNDRY:
        return computeCiscoInterfaceType(name);

      case FLAT_JUNIPER:
      case JUNIPER:
      case JUNIPER_SWITCH:
        return computeJuniperInterfaceType(name);

      case VYOS:
      case FLAT_VYOS:
        return computeVyosInterfaceType(name);

      case HOST:
        return computeHostInterfaceType(name);

      case MRV:
        // TODO: find out if other interface types are possible
        return InterfaceType.PHYSICAL;

      case EMPTY:
      case MSS:
      case IPTABLES:
      case UNKNOWN:
      case VXWORKS:
        // $CASES-OMITTED$
      default:
        throw new BatfishException(
            "Cannot compute interface type for unsupported configuration format: " + format);
    }
  }

  private static InterfaceType computeJuniperInterfaceType(String name) {
    if (name.startsWith("st")) {
      return InterfaceType.VPN;
    } else if (name.startsWith("reth")) {
      return InterfaceType.REDUNDANT;
    } else if (name.startsWith("ae")) {
      return InterfaceType.AGGREGATED;
    } else {
      return InterfaceType.PHYSICAL;
    }
  }

  private static InterfaceType computeVyosInterfaceType(String name) {
    if (name.startsWith("vti")) {
      return InterfaceType.VPN;
    } else if (name.startsWith("lo")) {
      return InterfaceType.LOOPBACK;
    } else {
      return InterfaceType.PHYSICAL;
    }
  }

  private int _accessVlan;

  private boolean _active;

  private SortedSet<Ip> _additionalArpIps;

  private List<SubRange> _allowedVlans;

  private SortedSet<InterfaceAddress> _allAddresses;

  private boolean _autoState;

  @Nullable private Double _bandwidth;

  private transient boolean _blacklisted;

  private String _channelGroup;

  private SortedSet<String> _channelGroupMembers;

  private String _cryptoMap;

  private SortedSet<String> _declaredNames;

  private String _description;

  private List<Ip> _dhcpRelayAddresses;

  private IpAccessList _inboundFilter;

  private transient String _inboundFilterName;

  private IpAccessList _incomingFilter;

  private transient String _incomingFilterName;

  private InterfaceType _interfaceType;

  private IsisInterfaceSettings _isis;

  private int _mtu;

  private int _nativeVlan;

  private OspfArea _ospfArea;

  private transient Long _ospfAreaName;

  private Integer _ospfCost;

  private int _ospfDeadInterval;

  private boolean _ospfEnabled;

  private int _ospfHelloMultiplier;

  private boolean _ospfPassive;

  private boolean _ospfPointToPoint;

  private IpAccessList _outgoingFilter;

  private transient String _outgoingFilterName;

  private Configuration _owner;

  private InterfaceAddress _address;

  private boolean _proxyArp;

  private boolean _ripEnabled;

  private boolean _ripPassive;

  private RoutingPolicy _routingPolicy;

  private transient String _routingPolicyName;

  private List<SourceNat> _sourceNats;

  private boolean _spanningTreePortfast;

  private Boolean _switchport;

  private SwitchportMode _switchportMode;

  private SwitchportEncapsulationType _switchportTrunkEncapsulation;

  private Integer _vlan;

  private Vrf _vrf;

  private transient String _vrfName;

  private SortedMap<Integer, VrrpGroup> _vrrpGroups;

  private Zone _zone;

  private transient String _zoneName;

  @SuppressWarnings("unused")
  private Interface() {
    this(null, null);
  }

  @JsonCreator
  public Interface(@JsonProperty(PROP_NAME) String name) {
    this(name, null);
  }

  public Interface(String name, Configuration owner) {
    this(name, owner, InterfaceType.UNKNOWN);

    // Determine interface type after setting owner
    _interfaceType =
        ((_key == null) || (_owner == null))
            ? InterfaceType.UNKNOWN
            : computeInterfaceType(_key, _owner.getConfigurationFormat());
  }

  public Interface(String name, Configuration owner, @Nonnull InterfaceType interfaceType) {
    super(name);
    _active = true;
    _autoState = true;
    _allowedVlans = new ArrayList<>();
    _allAddresses = ImmutableSortedSet.of();
    _channelGroupMembers = ImmutableSortedSet.of();
    _declaredNames = ImmutableSortedSet.of();
    _dhcpRelayAddresses = new ArrayList<>();
    _interfaceType = interfaceType;
    _mtu = DEFAULT_MTU;
    _nativeVlan = 1;
    _owner = owner;
    _switchportMode = SwitchportMode.NONE;
    _switchportTrunkEncapsulation = SwitchportEncapsulationType.DOT1Q;
    _sourceNats = Collections.emptyList();
    _vrfName = Configuration.DEFAULT_VRF_NAME;
    _vrrpGroups = new TreeMap<>();
  }

  public void addAllowedRanges(List<SubRange> ranges) {
    _allowedVlans.addAll(ranges);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Interface)) {
      return false;
    }
    Interface other = (Interface) o;
    if (_accessVlan != other._accessVlan) {
      return false;
    }
    if (_active != other._active) {
      return false;
    }
    if (!Objects.equals(_address, other._address)) {
      return false;
    }
    if (!Objects.equals(_allowedVlans, other._allowedVlans)) {
      return false;
    }
    if (!Objects.equals(_allAddresses, other._allAddresses)) {
      return false;
    }
    if (_autoState != other._autoState) {
      return false;
    }
    if (!Objects.equals(_bandwidth, other._bandwidth)) {
      return false;
    }
    if (!Objects.equals(_cryptoMap, other._cryptoMap)) {
      return false;
    }
    // we check ACLs for name match only -- full ACL diff can be done
    // elsewhere.
    if (!IpAccessList.bothNullOrSameName(this.getInboundFilter(), other.getInboundFilter())) {
      return false;
    }
    if (!IpAccessList.bothNullOrSameName(this.getIncomingFilter(), other.getIncomingFilter())) {
      return false;
    }
    if (this._interfaceType != other._interfaceType) {
      return false;
    }
    if (!Objects.equals(_key, other._key)) {
      return false;
    }
    // TODO: check ISIS settings for equality.
    if (_mtu != other._mtu) {
      return false;
    }
    if (_nativeVlan != other._nativeVlan) {
      return false;
    }
    // TODO: check OSPF settings for equality.
    if (!IpAccessList.bothNullOrSameName(this._outgoingFilter, other._outgoingFilter)) {
      return false;
    }
    if (!_proxyArp == other._proxyArp) {
      return false;
    }
    if (!Objects.equals(this._routingPolicy, other._routingPolicy)) {
      return false;
    }
    if (!Objects.equals(this._switchportMode, other._switchportMode)) {
      return false;
    }
    if (!Objects.equals(this._zone, other._zone)) {
      return false;
    }
    return true;
  }

  @JsonProperty(PROP_ACCESS_VLAN)
  @JsonPropertyDescription("Number of access VLAN when switchport mode is ACCESS")
  public int getAccessVlan() {
    return _accessVlan;
  }

  @JsonProperty(PROP_ACTIVE)
  @JsonPropertyDescription(
      "Whether this interface is administratively active (true) or disabled (false)")
  public boolean getActive() {
    return _active;
  }

  @JsonProperty(PROP_ADDITIONAL_ARP_IPS)
  public SortedSet<Ip> getAdditionalArpIps() {
    return _additionalArpIps;
  }

  @JsonProperty(PROP_ALLOWED_VLANS)
  @JsonPropertyDescription("Ranges of allowed VLANs when switchport mode is TRUNK")
  public List<SubRange> getAllowedVlans() {
    return _allowedVlans;
  }

  @JsonProperty(PROP_ALL_PREFIXES)
  @JsonPropertyDescription("All IPV4 address/network assignments on this interface")
  public Set<InterfaceAddress> getAllAddresses() {
    return _allAddresses;
  }

  @JsonProperty(PROP_AUTOSTATE)
  @JsonPropertyDescription(
      "Whether this VLAN interface's operational status is dependent on corresponding member "
          + "switchports")
  public boolean getAutoState() {
    return _autoState;
  }

  @JsonProperty(PROP_BANDWIDTH)
  @JsonPropertyDescription(
      "The nominal bandwidth of this interface in bits/sec for use in protocol cost calculations")
  @Nullable
  public Double getBandwidth() {
    return _bandwidth;
  }

  @JsonIgnore
  public boolean getBlacklisted() {
    return _blacklisted;
  }

  @JsonProperty(PROP_CHANNEL_GROUP)
  public String getChannelGroup() {
    return _channelGroup;
  }

  @JsonProperty(PROP_CHANNEL_GROUP_MEMBERS)
  public SortedSet<String> getChannelGroupMembers() {
    return _channelGroupMembers;
  }

  @JsonProperty(PROP_CRYPTO_MAP)
  public String getCryptoMap() {
    return _cryptoMap;
  }

  @JsonProperty(PROP_DECLARED_NAMES)
  public SortedSet<String> getDeclaredNames() {
    return _declaredNames;
  }

  @JsonProperty(PROP_DESCRIPTION)
  @JsonPropertyDescription("Description of this interface")
  public String getDescription() {
    return _description;
  }

  @JsonProperty(PROP_DHCP_RELAY_ADDRESSES)
  public List<Ip> getDhcpRelayAddresses() {
    return _dhcpRelayAddresses;
  }

  @JsonIgnore
  public IpAccessList getInboundFilter() {
    return _inboundFilter;
  }

  @JsonProperty(PROP_INBOUND_FILTER)
  @JsonPropertyDescription(
      "The IPV4 access-list used to filter traffic destined for this device on this interface.")
  public String getInboundFilterName() {
    if (_inboundFilter != null) {
      return _inboundFilter.getName();
    } else {
      return _inboundFilterName;
    }
  }

  @JsonIgnore
  public IpAccessList getIncomingFilter() {
    return _incomingFilter;
  }

  @JsonProperty(PROP_INCOMING_FILTER)
  @JsonPropertyDescription(
      "The IPV4 access-list used to filter traffic that arrives on this interface.")
  public String getIncomingFilterName() {
    if (_incomingFilter != null) {
      return _incomingFilter.getName();
    } else {
      return _incomingFilterName;
    }
  }

  @JsonProperty(PROP_INTERFACE_TYPE)
  @JsonPropertyDescription("The type of this interface")
  public InterfaceType getInterfaceType() {
    return _interfaceType;
  }

  @JsonProperty(PROP_ISIS)
  public @Nullable IsisInterfaceSettings getIsis() {
    return _isis;
  }

  @JsonProperty(PROP_ISIS_COST)
  @JsonPropertyDescription("The IS-IS cost of this interface")
  @Deprecated
  public Integer getIsisCost() {
    return null;
  }

  @JsonProperty(PROP_ISIS_L1_INTERFACE_MODE)
  @JsonPropertyDescription(
      "Specifies whether this interface is active, passive, or unconfigured with respect to IS-IS "
          + "level 1")
  @Deprecated
  public IsisInterfaceMode getIsisL1InterfaceMode() {
    return null;
  }

  @JsonProperty(PROP_ISIS_L2_INTERFACE_MODE)
  @JsonPropertyDescription(
      "Specifies whether this interface is active, passive, or unconfigured with respect to IS-IS "
          + "level 2")
  @Deprecated
  public IsisInterfaceMode getIsisL2InterfaceMode() {
    // TODO: deprecate properly
    return null;
  }

  @JsonProperty(PROP_MTU)
  @JsonPropertyDescription("The maximum transmission unit (MTU) of this interface in bytes")
  public int getMtu() {
    return _mtu;
  }

  @JsonProperty(PROP_NATIVE_VLAN)
  @JsonPropertyDescription("The native VLAN of this interface when switchport mode is TRUNK")
  public int getNativeVlan() {
    return _nativeVlan;
  }

  @JsonIgnore
  public OspfArea getOspfArea() {
    return _ospfArea;
  }

  @JsonProperty(PROP_OSPF_AREA)
  @JsonPropertyDescription("The OSPF area to which this interface belongs.")
  public Long getOspfAreaName() {
    if (_ospfArea != null) {
      return _ospfArea.getName();
    } else {
      return _ospfAreaName;
    }
  }

  @JsonProperty(PROP_OSPF_COST)
  @JsonPropertyDescription(
      "The explicit OSPF cost of this interface. If unset, the cost is automatically calculated.")
  public Integer getOspfCost() {
    return _ospfCost;
  }

  @JsonProperty(PROP_OSPF_DEAD_INTERVAL)
  @JsonPropertyDescription("Dead-interval in seconds for OSPF updates")
  public int getOspfDeadInterval() {
    return _ospfDeadInterval;
  }

  @JsonProperty(PROP_OSPF_ENABLED)
  @JsonPropertyDescription(
      "Whether or not OSPF is enabled at all on this interface (either actively or passively)")
  public boolean getOspfEnabled() {
    return _ospfEnabled;
  }

  @JsonProperty(PROP_OSPF_HELLO_MULTIPLIER)
  @JsonPropertyDescription(
      "Number of OSPF packets to send out during dead-interval period for fast OSPF updates")
  public int getOspfHelloMultiplier() {
    return _ospfHelloMultiplier;
  }

  @JsonProperty(PROP_OSPF_PASSIVE)
  @JsonPropertyDescription(
      "Whether or not OSPF is enabled passively on this interface. If passive, this interface is "
          + "included in the OSPF RIB, but no OSPF packets are sent from it.")
  public boolean getOspfPassive() {
    return _ospfPassive;
  }

  @JsonProperty(PROP_OSPF_POINT_TO_POINT)
  public boolean getOspfPointToPoint() {
    return _ospfPointToPoint;
  }

  @JsonIgnore
  public IpAccessList getOutgoingFilter() {
    return _outgoingFilter;
  }

  @JsonProperty(PROP_OUTGOING_FILTER)
  @JsonPropertyDescription(
      "The IPV4 access-list used to filter traffic that is sent out this interface. Stored as @id")
  public String getOutgoingFilterName() {
    if (_outgoingFilter != null) {
      return _outgoingFilter.getName();
    } else {
      return _outgoingFilterName;
    }
  }

  @JsonIgnore
  public Configuration getOwner() {
    return _owner;
  }

  @JsonProperty(PROP_PREFIX)
  @JsonPropertyDescription("The primary IPV4 address/network of this interface")
  public InterfaceAddress getAddress() {
    return _address;
  }

  @JsonPropertyDescription("Whether or not proxy-ARP is enabled on this interface.")
  public boolean getProxyArp() {
    return _proxyArp;
  }

  @JsonProperty(PROP_RIP_ENABLED)
  public boolean getRipEnabled() {
    return _ripEnabled;
  }

  @JsonProperty(PROP_RIP_PASSIVE)
  public boolean getRipPassive() {
    return _ripPassive;
  }

  @JsonIgnore
  public RoutingPolicy getRoutingPolicy() {
    return _routingPolicy;
  }

  @JsonProperty(PROP_ROUTING_POLICY)
  @JsonPropertyDescription(
      "The routing policy used on this interface for policy-routing (as opposed to destination-"
          + "routing). Stored as @id")
  public String getRoutingPolicyName() {
    if (_routingPolicy != null) {
      return _routingPolicy.getName();
    } else {
      return _routingPolicyName;
    }
  }

  @JsonProperty(PROP_SOURCE_NATS)
  public List<SourceNat> getSourceNats() {
    return _sourceNats;
  }

  @JsonProperty(PROP_SPANNING_TREE_PORTFAST)
  @JsonPropertyDescription("Whether or not spanning-tree portfast feature is enabled")
  public boolean getSpanningTreePortfast() {
    return _spanningTreePortfast;
  }

  @JsonProperty(PROP_SWITCHPORT)
  @JsonPropertyDescription(
      "Whether this interface is explicitly set as a switchport. Nothing may be inferred from "
          + "absence of this field.")
  public Boolean getSwitchport() {
    return _switchport;
  }

  @JsonProperty(PROP_SWITCHPORT_MODE)
  @JsonPropertyDescription("The switchport mode (if any) of this interface")
  public SwitchportMode getSwitchportMode() {
    return _switchportMode;
  }

  @JsonProperty(PROP_SWITCHPORT_TRUNK_ENCAPSULATION)
  @JsonPropertyDescription(
      "The switchport trunk encapsulation type of this interface. Only relevant when switchport "
          + "mode is TRUNK")
  public SwitchportEncapsulationType getSwitchportTrunkEncapsulation() {
    return _switchportTrunkEncapsulation;
  }

  @JsonProperty(PROP_VLAN)
  public @Nullable Integer getVlan() {
    return _vlan;
  }

  @JsonIgnore
  public Vrf getVrf() {
    return _vrf;
  }

  @JsonProperty(PROP_VRF)
  @JsonPropertyDescription("The name of the VRF to which this interface belongs")
  public String getVrfName() {
    if (_vrf != null) {
      return _vrf.getName();
    } else {
      return _vrfName;
    }
  }

  @JsonProperty(PROP_VRRP_GROUPS)
  public SortedMap<Integer, VrrpGroup> getVrrpGroups() {
    return _vrrpGroups;
  }

  @JsonIgnore
  public Zone getZone() {
    return _zone;
  }

  @JsonProperty(PROP_ZONE)
  @JsonPropertyDescription("The firewall zone to which this interface belongs.")
  public String getZoneName() {
    if (_zone != null) {
      return _zone.getName();
    } else {
      return _zoneName;
    }
  }

  public boolean isLoopback(ConfigurationFormat vendor) {
    String name = _key.toLowerCase();
    if (vendor == ConfigurationFormat.JUNIPER || vendor == ConfigurationFormat.FLAT_JUNIPER) {
      if (!name.contains(".")) {
        return false;
      }
    } else if (name.contains("longreach")) {
      return false;
    }
    return name.startsWith("lo");
  }

  public void resolveReferences(Configuration owner) {
    _owner = owner;
    _vrf = owner.getVrfs().get(_vrfName);
    if (_inboundFilterName != null) {
      _inboundFilter = owner.getIpAccessLists().get(_inboundFilterName);
    }
    if (_incomingFilterName != null) {
      _incomingFilter = owner.getIpAccessLists().get(_incomingFilterName);
    }
    if (_outgoingFilterName != null) {
      _outgoingFilter = owner.getIpAccessLists().get(_outgoingFilterName);
    }
    if (_ospfAreaName != null) {
      OspfProcess ospfProc = _vrf.getOspfProcess();
      if (ospfProc != null) {
        _ospfArea = ospfProc.getAreas().get(_ospfAreaName);
      }
    }
  }

  @JsonProperty(PROP_ACCESS_VLAN)
  public void setAccessVlan(int vlan) {
    _accessVlan = vlan;
  }

  @JsonProperty(PROP_ACTIVE)
  public void setActive(boolean active) {
    _active = active;
  }

  @JsonProperty(PROP_ADDITIONAL_ARP_IPS)
  public void setAdditionalArpIps(Iterable<Ip> additionalArpIps) {
    _additionalArpIps = ImmutableSortedSet.copyOf(additionalArpIps);
  }

  @JsonProperty(PROP_ALLOWED_VLANS)
  public void setAllowedVlans(List<SubRange> allowedVlans) {
    _allowedVlans = allowedVlans;
  }

  @JsonProperty(PROP_ALL_PREFIXES)
  public void setAllAddresses(Iterable<InterfaceAddress> allAddresses) {
    _allAddresses = ImmutableSortedSet.copyOf(allAddresses);
  }

  @JsonProperty(PROP_AUTOSTATE)
  public void setAutoState(boolean autoState) {
    _autoState = autoState;
  }

  @JsonProperty(PROP_BANDWIDTH)
  public void setBandwidth(@Nullable Double bandwidth) {
    _bandwidth = bandwidth;
  }

  @JsonIgnore
  public void setBlacklisted(boolean blacklisted) {
    _blacklisted = blacklisted;
  }

  @JsonProperty(PROP_CHANNEL_GROUP)
  public void setChannelGroup(String channelGroup) {
    _channelGroup = channelGroup;
  }

  @JsonProperty(PROP_CHANNEL_GROUP_MEMBERS)
  public void setChannelGroupMembers(Iterable<String> channelGroupMembers) {
    _channelGroupMembers = ImmutableSortedSet.copyOf(channelGroupMembers);
  }

  @JsonProperty(PROP_CRYPTO_MAP)
  public void setCryptoMap(String cryptoMap) {
    _cryptoMap = cryptoMap;
  }

  @JsonProperty(PROP_DECLARED_NAMES)
  public void setDeclaredNames(SortedSet<String> declaredNames) {
    _declaredNames = ImmutableSortedSet.copyOf(declaredNames);
  }

  @JsonProperty(PROP_DESCRIPTION)
  public void setDescription(String description) {
    _description = description;
  }

  @JsonProperty(PROP_DHCP_RELAY_ADDRESSES)
  public void setDhcpRelayAddresses(List<Ip> dhcpRelayAddresses) {
    _dhcpRelayAddresses = dhcpRelayAddresses;
  }

  @JsonIgnore
  public void setInboundFilter(IpAccessList inboundFilter) {
    _inboundFilter = inboundFilter;
  }

  @JsonProperty(PROP_INBOUND_FILTER)
  public void setInboundFilterName(String inboundFilterName) {
    _inboundFilterName = inboundFilterName;
  }

  @JsonIgnore
  public void setIncomingFilter(IpAccessList incomingFilter) {
    _incomingFilter = incomingFilter;
  }

  @JsonProperty(PROP_INCOMING_FILTER)
  public void setIncomingFilterName(String incomingFilterName) {
    _incomingFilterName = incomingFilterName;
  }

  @JsonProperty(PROP_INTERFACE_TYPE)
  public void setInterfaceType(InterfaceType it) {
    _interfaceType = it;
  }

  @JsonProperty(PROP_ISIS)
  public void setIsis(@Nullable IsisInterfaceSettings isis) {
    _isis = isis;
  }

  @JsonProperty(PROP_ISIS_COST)
  @Deprecated
  public void setIsisCost(Integer isisCost) {
    // TODO: deprecate properly
  }

  @JsonProperty(PROP_ISIS_L1_INTERFACE_MODE)
  @Deprecated
  public void setIsisL1InterfaceMode(IsisInterfaceMode mode) {
    // TODO: deprecate properly
  }

  @JsonProperty(PROP_ISIS_L2_INTERFACE_MODE)
  @Deprecated
  public void setIsisL2InterfaceMode(IsisInterfaceMode mode) {
    // TODO: deprecate properly
  }

  @JsonProperty(PROP_MTU)
  public void setMtu(int mtu) {
    _mtu = mtu;
  }

  @JsonProperty(PROP_NATIVE_VLAN)
  public void setNativeVlan(int vlan) {
    _nativeVlan = vlan;
  }

  @JsonIgnore
  public void setOspfArea(OspfArea ospfArea) {
    _ospfArea = ospfArea;
  }

  @JsonProperty(PROP_OSPF_AREA)
  public void setOspfAreaName(Long ospfAreaName) {
    _ospfAreaName = ospfAreaName;
  }

  @JsonProperty(PROP_OSPF_COST)
  public void setOspfCost(Integer ospfCost) {
    _ospfCost = ospfCost;
  }

  @JsonProperty(PROP_OSPF_DEAD_INTERVAL)
  public void setOspfDeadInterval(int seconds) {
    _ospfDeadInterval = seconds;
  }

  @JsonProperty(PROP_OSPF_ENABLED)
  public void setOspfEnabled(boolean b) {
    _ospfEnabled = b;
  }

  @JsonProperty(PROP_OSPF_HELLO_MULTIPLIER)
  public void setOspfHelloMultiplier(int multiplier) {
    _ospfHelloMultiplier = multiplier;
  }

  @JsonProperty(PROP_OSPF_PASSIVE)
  public void setOspfPassive(boolean passive) {
    _ospfPassive = passive;
  }

  @JsonProperty(PROP_OSPF_POINT_TO_POINT)
  public void setOspfPointToPoint(boolean ospfPointToPoint) {
    _ospfPointToPoint = ospfPointToPoint;
  }

  @JsonIgnore
  public void setOutgoingFilter(IpAccessList outgoingFilter) {
    _outgoingFilter = outgoingFilter;
  }

  @JsonProperty(PROP_OUTGOING_FILTER)
  public void setOutgoingFilter(String outgoingFilterName) {
    _outgoingFilterName = outgoingFilterName;
  }

  @JsonIgnore
  public void setOwner(Configuration owner) {
    _owner = owner;
  }

  @JsonProperty(PROP_PREFIX)
  public void setAddress(InterfaceAddress address) {
    _address = address;
  }

  public void setProxyArp(boolean proxyArp) {
    _proxyArp = proxyArp;
  }

  @JsonProperty(PROP_RIP_ENABLED)
  public void setRipEnabled(boolean ripEnabled) {
    _ripEnabled = ripEnabled;
  }

  @JsonProperty(PROP_RIP_PASSIVE)
  public void setRipPassive(boolean ripPassive) {
    _ripPassive = ripPassive;
  }

  @JsonIgnore
  public void setRoutingPolicy(RoutingPolicy routingPolicy) {
    _routingPolicy = routingPolicy;
  }

  @JsonProperty(PROP_ROUTING_POLICY)
  public void setRoutingPolicy(String routingPolicyName) {
    _routingPolicyName = routingPolicyName;
  }

  @JsonProperty(PROP_SOURCE_NATS)
  public void setSourceNats(List<SourceNat> sourceNats) {
    _sourceNats = sourceNats;
  }

  @JsonProperty(PROP_SPANNING_TREE_PORTFAST)
  public void setSpanningTreePortfast(boolean spanningTreePortfast) {
    _spanningTreePortfast = spanningTreePortfast;
  }

  @JsonProperty(PROP_SWITCHPORT)
  public void setSwitchport(Boolean switchport) {
    _switchport = switchport;
  }

  @JsonProperty(PROP_SWITCHPORT_MODE)
  public void setSwitchportMode(SwitchportMode switchportMode) {
    _switchportMode = switchportMode;
  }

  @JsonProperty(PROP_SWITCHPORT_TRUNK_ENCAPSULATION)
  public void setSwitchportTrunkEncapsulation(SwitchportEncapsulationType encapsulation) {
    _switchportTrunkEncapsulation = encapsulation;
  }

  @JsonProperty(PROP_VLAN)
  public void setVlan(@Nullable Integer vlan) {
    _vlan = vlan;
  }

  @JsonIgnore
  public void setVrf(Vrf vrf) {
    _vrf = vrf;
    if (vrf != null) {
      _vrfName = vrf.getName();
    }
  }

  @JsonProperty(PROP_VRF)
  public void setVrfName(String vrfName) {
    _vrfName = vrfName;
  }

  @JsonProperty(PROP_VRRP_GROUPS)
  public void setVrrpGroups(SortedMap<Integer, VrrpGroup> vrrpGroups) {
    _vrrpGroups = vrrpGroups;
  }

  @JsonIgnore
  public void setZone(Zone zone) {
    _zone = zone;
  }

  @JsonProperty(PROP_ZONE)
  public void setZoneName(String zoneName) {
    _zoneName = zoneName;
  }

  public JSONObject toJSONObject() throws JSONException {
    JSONObject iface = new JSONObject();
    iface.put("node", _owner.getName());
    iface.put("name", _key);
    iface.put(PROP_PREFIX, _address.toString());
    iface.put(PROP_INTERFACE_TYPE, _interfaceType.toString());
    return iface;
  }
}

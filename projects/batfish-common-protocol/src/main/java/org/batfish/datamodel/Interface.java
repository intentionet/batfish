package org.batfish.datamodel;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;
import org.batfish.datamodel.eigrp.EigrpInterfaceSettings;
import org.batfish.datamodel.hsrp.HsrpGroup;
import org.batfish.datamodel.isis.IsisInterfaceSettings;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.transformation.Transformation;

public final class Interface extends ComparableStructure<String> {

  public static class Builder extends NetworkFactoryBuilder<Interface> {

    private @Nullable Integer _accessVlan;
    private boolean _active;
    private InterfaceAddress _address;
    private @Nullable IntegerSpace _allowedVlans;
    private boolean _autoState;
    @Nullable private Double _bandwidth;
    private boolean _blacklisted;
    private @Nullable String _channelGroup;
    private @Nonnull SortedSet<String> _channelGroupMembers;
    private SortedSet<String> _declaredNames;
    @Nonnull private Set<Dependency> _dependencies = ImmutableSet.of();
    @Nullable private String _description;
    @Nullable private EigrpInterfaceSettings _eigrp;
    @Nullable private Integer _encapsulationVlan;
    private Map<Integer, HsrpGroup> _hsrpGroups;
    private String _hsrpVersion;
    private FirewallSessionInterfaceInfo _firewallSessionInterfaceInfo;
    private String _ospfInboundDistributeListPolicy;
    private IpAccessList _incomingFilter;
    private Transformation _incomingTransformation;
    private IsisInterfaceSettings _isis;
    private @Nullable Integer _mlagId;
    private String _name;
    private @Nullable Integer _nativeVlan;
    private OspfArea _ospfArea;
    private Integer _ospfCost;
    private boolean _ospfEnabled;
    private boolean _ospfPassive;
    private boolean _ospfPointToPoint;
    private String _ospfProcess;
    private IpAccessList _outgoingFilter;
    private Transformation _outgoingTransformation;
    private Configuration _owner;
    private IpAccessList _postTransformationIncomingFilter;
    private boolean _proxyArp;
    private IpAccessList _preTransformationOutgoingFilter;
    private Set<InterfaceAddress> _secondaryAddresses;
    private @Nullable Double _speed;
    private @Nullable Boolean _switchport;
    private @Nullable SwitchportMode _switchportMode;
    private @Nonnull IpSpace _additionalArpIps;
    private InterfaceType _type;
    private @Nullable Integer _vlan;
    private Vrf _vrf;
    private SortedMap<Integer, VrrpGroup> _vrrpGroups;

    Builder(NetworkFactory networkFactory) {
      super(networkFactory, Interface.class);
      _active = true;
      _additionalArpIps = EmptyIpSpace.INSTANCE;
      _autoState = true;
      _channelGroupMembers = ImmutableSortedSet.of();
      _declaredNames = ImmutableSortedSet.of();
      _hsrpGroups = ImmutableMap.of();
      _secondaryAddresses = ImmutableSet.of();
      _vrrpGroups = ImmutableSortedMap.of();
    }

    @Override
    public Interface build() {
      String name = _name != null ? _name : generateName();
      Interface iface =
          _type == null ? new Interface(name, _owner) : new Interface(name, _owner, _type);
      if (_accessVlan != null) {
        iface.setAccessVlan(_accessVlan);
      }
      iface.setActive(_active);

      // Set addresses. If the primary address is missing from allAddresses, add it.
      ImmutableSet.Builder<InterfaceAddress> allAddresses = ImmutableSet.builder();
      if (_address != null) {
        iface.setAddress(_address);
        allAddresses.add(_address);
      }
      iface.setAllAddresses(allAddresses.addAll(_secondaryAddresses).build());

      iface.setAdditionalArpIps(_additionalArpIps);
      if (_allowedVlans != null) {
        iface.setAllowedVlans(_allowedVlans);
      }
      iface.setAutoState(_autoState);
      iface.setBandwidth(_bandwidth);
      iface.setBlacklisted(_blacklisted);
      iface.setChannelGroup(_channelGroup);
      iface.setChannelGroupMembers(_channelGroupMembers);
      iface.setDeclaredNames(_declaredNames);
      iface.setDependencies(_dependencies);
      iface.setDescription(_description);
      iface.setEigrp(_eigrp);
      iface.setEncapsulationVlan(_encapsulationVlan);
      iface.setHsrpGroups(_hsrpGroups);
      iface.setHsrpVersion(_hsrpVersion);
      iface.setFirewallSessionInterfaceInfo(_firewallSessionInterfaceInfo);
      iface.setIncomingFilter(_incomingFilter);
      iface.setIncomingTransformation(_incomingTransformation);
      iface.setIsis(_isis);
      iface.setMlagId(_mlagId);
      if (_nativeVlan != null) {
        iface.setNativeVlan(_nativeVlan);
      }
      iface.setOspfArea(_ospfArea);
      if (_ospfArea != null) {
        _ospfArea.addInterface(name);
        iface.setOspfAreaName(_ospfArea.getAreaNumber());
      }
      iface.setOspfCost(_ospfCost);
      iface.setOspfEnabled(_ospfEnabled);
      iface.setOspfInboundDistributeListPolicy(_ospfInboundDistributeListPolicy);
      iface.setOspfPassive(_ospfPassive);
      iface.setOspfPointToPoint(_ospfPointToPoint);
      iface.setOspfProcess(_ospfProcess);
      iface.setOutgoingFilter(_outgoingFilter);
      iface.setOutgoingTransformation(_outgoingTransformation);
      iface.setOwner(_owner);
      if (_owner != null) {
        _owner.getAllInterfaces().put(name, iface);
      }
      iface.setPostTransformationIncomingFilter(_postTransformationIncomingFilter);
      iface.setPreTransformationOutgoingFilter(_preTransformationOutgoingFilter);
      iface.setProxyArp(_proxyArp);
      iface.setSpeed(_speed);
      if (_switchport != null) {
        iface.setSwitchport(_switchport);
      }
      if (_switchportMode != null) {
        iface.setSwitchportMode(_switchportMode);
      }
      if (_type != null) {
        iface.setInterfaceType(_type);
      }
      iface.setVlan(_vlan);
      iface.setVrf(_vrf);
      if (_vrf != null) {
        _vrf.getInterfaces().put(name, iface);
        if (_active && _ospfProcess != null && _vrf.getOspfProcesses().containsKey(_ospfProcess)) {
          iface.setOspfCost(_vrf.getOspfProcesses().get(_ospfProcess).computeInterfaceCost(iface));
        }
      }
      iface.setVrrpGroups(_vrrpGroups);
      return iface;
    }

    public Builder setActive(boolean active) {
      _active = active;
      return this;
    }

    public Builder setAdditionalArpIps(IpSpace additionalArpIps) {
      _additionalArpIps = additionalArpIps;
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

    public @Nonnull Builder setAccessVlan(@Nullable Integer accessVlan) {
      _accessVlan = accessVlan;
      return this;
    }

    public @Nonnull Builder setAllowedVlans(@Nullable IntegerSpace allowedVlans) {
      _allowedVlans = allowedVlans;
      return this;
    }

    public @Nonnull Builder setAutoState(boolean autoState) {
      _autoState = autoState;
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

    public @Nonnull Builder setChannelGroup(@Nullable String channelGroup) {
      _channelGroup = channelGroup;
      return this;
    }

    public @Nonnull Builder setChannelGroupMembers(Iterable<String> channelGroupMembers) {
      _channelGroupMembers = ImmutableSortedSet.copyOf(channelGroupMembers);
      return this;
    }

    public Builder setDeclaredNames(Iterable<String> declaredNames) {
      _declaredNames = ImmutableSortedSet.copyOf(declaredNames);
      return this;
    }

    public Builder setDependencies(@Nonnull Iterable<Dependency> dependencies) {
      _dependencies = ImmutableSet.copyOf(dependencies);
      return this;
    }

    public Builder setDescription(@Nullable String description) {
      _description = description;
      return this;
    }

    /**
     * Name of the routing policy which is generated from the Global and Interface level inbound
     * distribute lists for OSPF
     */
    public Builder setOspfInboundDistributeListPolicy(
        @Nonnull String ospfInboundDistributeListPolicy) {
      _ospfInboundDistributeListPolicy = ospfInboundDistributeListPolicy;
      return this;
    }

    public Builder setEigrp(@Nullable EigrpInterfaceSettings eigrp) {
      _eigrp = eigrp;
      return this;
    }

    public @Nonnull Builder setEncapsulationVlan(@Nullable Integer encapsulationVlan) {
      _encapsulationVlan = encapsulationVlan;
      return this;
    }

    public Builder setFirewallSessionInterfaceInfo(
        @Nullable FirewallSessionInterfaceInfo firewallSessionInterfaceInfo) {
      _firewallSessionInterfaceInfo = firewallSessionInterfaceInfo;
      return this;
    }

    public @Nonnull Builder setHsrpGroups(@Nonnull Map<Integer, HsrpGroup> hsrpGroups) {
      _hsrpGroups = ImmutableMap.copyOf(hsrpGroups);
      return this;
    }

    public @Nonnull Builder setHsrpVersion(@Nullable String hsrpVersion) {
      _hsrpVersion = hsrpVersion;
      return this;
    }

    public Builder setIncomingFilter(IpAccessList incomingFilter) {
      _incomingFilter = incomingFilter;
      return this;
    }

    public Builder setIncomingTransformation(Transformation incomingTransformation) {
      _incomingTransformation = incomingTransformation;
      return this;
    }

    public Builder setIsis(IsisInterfaceSettings isis) {
      _isis = isis;
      return this;
    }

    public @Nonnull Builder setMlagId(@Nullable Integer mlagId) {
      _mlagId = mlagId;
      return this;
    }

    public Builder setName(String name) {
      _name = name;
      return this;
    }

    public @Nonnull Builder setNativeVlan(@Nullable Integer nativeVlan) {
      _nativeVlan = nativeVlan;
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

    public Builder setOspfProcess(String process) {
      _ospfProcess = process;
      return this;
    }

    public Builder setOutgoingFilter(IpAccessList outgoingFilter) {
      _outgoingFilter = outgoingFilter;
      return this;
    }

    public Builder setOutgoingTransformation(Transformation outgoingTransformation) {
      _outgoingTransformation = outgoingTransformation;
      return this;
    }

    public Builder setOwner(Configuration owner) {
      _owner = owner;
      return this;
    }

    public Builder setPostTransformationIncomingFilter(
        IpAccessList postTransformationIncomingFilter) {
      _postTransformationIncomingFilter = postTransformationIncomingFilter;
      return this;
    }

    public Builder setPreTransformationOutgoingFilter(
        IpAccessList preTransformationOutgoingFilter) {
      _preTransformationOutgoingFilter = preTransformationOutgoingFilter;
      return this;
    }

    public Builder setProxyArp(boolean proxyArp) {
      _proxyArp = proxyArp;
      return this;
    }

    /**
     * Set the secondary addresses of the interface. <br>
     * The {@link Interface#getAllConcreteAddresses()} method of the built {@link Interface} will
     * return a set containing the primary address and secondary addresses.<br>
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

    public @Nonnull Builder setSwitchport(@Nullable Boolean switchport) {
      _switchport = switchport;
      return this;
    }

    public @Nonnull Builder setSwitchportMode(@Nullable SwitchportMode switchportMode) {
      _switchportMode = switchportMode;
      return this;
    }

    public @Nonnull Builder setSpeed(@Nullable Double speed) {
      _speed = speed;
      return this;
    }

    public Builder setType(InterfaceType type) {
      _type = type;
      return this;
    }

    public @Nonnull Builder setVlan(@Nullable Integer vlan) {
      _vlan = vlan;
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

  /** Type of interface dependency. Informs failure analysis and bandwidth computation */
  public enum DependencyType {
    /** Aggregate dependency, part of one-to-many dependencies */
    AGGREGATE,
    /** A bind dependency, one-to-one, required for fate sharing */
    BIND
  }

  /**
   * Represents a directional dependency between two interfaces. Owner of this object <b>depends
   * on</b> the interface name described by this object.
   */
  @ParametersAreNonnullByDefault
  public static final class Dependency implements Serializable {
    @Nonnull private final String _interfaceName;
    @Nonnull private final DependencyType _type;

    public Dependency(String interfaceName, DependencyType type) {
      _interfaceName = interfaceName;
      _type = type;
    }

    @Nonnull
    public String getInterfaceName() {
      return _interfaceName;
    }

    @Nonnull
    public DependencyType getType() {
      return _type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Dependency)) {
        return false;
      }
      Dependency that = (Dependency) o;
      return _interfaceName.equals(that._interfaceName) && _type == that._type;
    }

    @Override
    public int hashCode() {
      return Objects.hash(_interfaceName, _type.ordinal());
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(Dependency.class)
          .add("interfaceName", _interfaceName)
          .add("type", _type)
          .toString();
    }
  }

  private static final int DEFAULT_MTU = 1500;
  public static final String DYNAMIC_INTERFACE_NAME = "dynamic";
  public static final String NULL_INTERFACE_NAME = "null_interface";
  public static final Set<InterfaceType> TUNNEL_INTERFACE_TYPES =
      ImmutableSet.of(InterfaceType.TUNNEL, InterfaceType.VPN);
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
  private static final String PROP_EIGRP = "eigrp";
  private static final String PROP_ENCAPSULATION_VLAN = "encapsulationVlan";
  private static final String PROP_FIREWALL_SESSION_INTERFACE_INFO = "firewallSessionInterfaceInfo";
  private static final String PROP_HSRP_GROUPS = "hsrpGroups";
  private static final String PROP_HSRP_VERSION = "hsrpVersion";
  private static final String PROP_INBOUND_FILTER = "inboundFilter";
  private static final String PROP_INCOMING_FILTER = "incomingFilter";
  private static final String PROP_INCOMING_TRANSFORMATION = "incomingTransformation";
  private static final String PROP_INTERFACE_TYPE = "type";
  private static final String PROP_ISIS = "isis";
  private static final String PROP_MLAG_ID = "mlagId";
  private static final String PROP_MTU = "mtu";
  private static final String PROP_NATIVE_VLAN = "nativeVlan";
  private static final String PROP_OSPF_AREA = "ospfArea";
  private static final String PROP_OSPF_COST = "ospfCost";
  private static final String PROP_OSPF_DEAD_INTERVAL = "ospfDeadInterval";
  private static final String PROP_OSPF_ENABLED = "ospfEnabled";
  private static final String PROP_OSPF_HELLO_MULTIPLIER = "ospfHelloMultiplier";
  private static final String PROP_OSPF_INBOUND_DISTRIBUTE_LIST_POLICY =
      "ospfInboundDistributeListPolicy";
  private static final String PROP_OSPF_PASSIVE = "ospfPassive";
  private static final String PROP_OSPF_POINT_TO_POINT = "ospfPointToPoint";
  private static final String PROP_OSPF_PROCESS = "ospfProcess";
  private static final String PROP_OUTGOING_FILTER = "outgoingFilter";
  private static final String PROP_OUTGOING_TRANSFORMATION = "outgoingTransformation";
  private static final String PROP_POST_TRANSFORMATION_INCOMING_FILTER =
      "postTransformationIncomingFilter";
  private static final String PROP_PREFIX = "prefix";
  private static final String PROP_PRE_TRANSFORMATION_OUTGOING_FILTER =
      "preTransformationOutgoingFilter";
  private static final String PROP_PROXY_ARP = "proxyArp";
  private static final String PROP_RIP_ENABLED = "ripEnabled";
  private static final String PROP_RIP_PASSIVE = "ripPassive";
  private static final String PROP_ROUTING_POLICY = "routingPolicy";
  private static final String PROP_SPANNING_TREE_PORTFAST = "spanningTreePortfast";
  private static final String PROP_SPEED = "speed";
  private static final String PROP_SWITCHPORT = "switchport";
  private static final String PROP_SWITCHPORT_MODE = "switchportMode";
  private static final String PROP_SWITCHPORT_TRUNK_ENCAPSULATION = "switchportTrunkEncapsulation";
  private static final String PROP_VLAN = "vlan";
  private static final String PROP_VRF = "vrf";
  private static final String PROP_VRRP_GROUPS = "vrrpGroups";
  private static final String PROP_ZONE = "zone";

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
    if (name.startsWith("vpn")) {
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
      if (name.contains(".")) {
        // Subinterface of a port channel
        return InterfaceType.AGGREGATE_CHILD;
      } else {
        return InterfaceType.AGGREGATED;
      }
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

      case F5_BIGIP_STRUCTURED:
        return computeF5BigipStructuredInterfaceType(name);

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

  private static final Pattern F5_PHYSICAL_INTERFACE_NAME_PATTERN =
      Pattern.compile("^\\d+\\.\\d+$");

  private static @Nonnull InterfaceType computeF5BigipStructuredInterfaceType(String name) {
    if (F5_PHYSICAL_INTERFACE_NAME_PATTERN.matcher(name).find()) {
      return InterfaceType.PHYSICAL;
    } else {
      return InterfaceType.UNKNOWN;
    }
  }

  private static InterfaceType computeJuniperInterfaceType(String name) {
    if (name.startsWith("st")) {
      return InterfaceType.VPN;
    } else if (name.startsWith("reth")) {
      return InterfaceType.REDUNDANT;
    } else if (name.startsWith("ae") && name.contains(".")) {
      return InterfaceType.AGGREGATE_CHILD;
    } else if (name.startsWith("ae")) {
      return InterfaceType.AGGREGATED;
    } else if (name.startsWith("lo")) {
      return InterfaceType.LOOPBACK;
    } else if (name.startsWith("irb")) {
      return InterfaceType.VLAN;
    } else if (name.contains(".")) {
      return InterfaceType.LOGICAL;
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

  @Nullable private Integer _accessVlan;
  private boolean _active;
  private @Nonnull IpSpace _additionalArpIps;
  private IntegerSpace _allowedVlans;
  @Nonnull private SortedSet<InterfaceAddress> _allAddresses;
  /** Cache of all concrete addresses */
  @Nullable private transient Set<ConcreteInterfaceAddress> _allConcreteAddresses;
  /** Cache of all link-local addresses */
  @Nullable private transient Set<LinkLocalAddress> _allLinkLocalAddresses;

  private boolean _autoState;
  @Nullable private Double _bandwidth;
  private transient boolean _blacklisted;
  private String _channelGroup;
  private SortedSet<String> _channelGroupMembers;
  private String _cryptoMap;
  private SortedSet<String> _declaredNames;
  /** Set of interface dependencies required for this interface to active */
  @Nonnull private Set<Dependency> _dependencies;

  private String _description;
  private List<Ip> _dhcpRelayAddresses;
  @Nullable private EigrpInterfaceSettings _eigrp;
  @Nullable private Integer _encapsulationVlan;
  @Nullable private FirewallSessionInterfaceInfo _firewallSessionInterfaceInfo;
  private Map<Integer, HsrpGroup> _hsrpGroups;
  private IpAccessList _inboundFilter;
  private transient String _inboundFilterName;
  private IpAccessList _incomingFilter;
  private transient String _incomingFilterName;
  private Transformation _incomingTransformation;
  private InterfaceType _interfaceType;
  private IsisInterfaceSettings _isis;
  @Nullable private Integer _mlagId;
  private int _mtu;
  @Nullable private Integer _nativeVlan;
  @Nullable private Long _ospfAreaName;
  private Integer _ospfCost;
  private int _ospfDeadInterval;
  private boolean _ospfEnabled;
  private int _ospfHelloMultiplier;
  @Nullable private String _ospfInboundDistributeListPolicy;
  private boolean _ospfPassive;
  private boolean _ospfPointToPoint;
  @Nullable private String _ospfProcess;
  private IpAccessList _outgoingFilter;
  private transient String _outgoingFilterName;
  private Transformation _outgoingTransformation;
  private Configuration _owner;
  private InterfaceAddress _address;
  private IpAccessList _postTransformationIncomingFilter;
  private transient String _postTransformationIncomingFilterName;
  private boolean _proxyArp;
  private IpAccessList _preTransformationOutgoingFilter;
  private transient String _preTransformationOutgoingFilterName;
  private boolean _ripEnabled;
  private boolean _ripPassive;
  private String _routingPolicyName;
  private boolean _spanningTreePortfast;
  private @Nullable Double _speed;
  private boolean _switchport;
  private SwitchportMode _switchportMode;
  private SwitchportEncapsulationType _switchportTrunkEncapsulation;
  private Integer _vlan;
  private Vrf _vrf;
  private transient String _vrfName;
  private SortedMap<Integer, VrrpGroup> _vrrpGroups;
  private String _zoneName;
  private String _hsrpVersion;

  @JsonCreator
  private Interface(@Nullable @JsonProperty(PROP_NAME) String name) {
    this(name, null);
  }

  private Interface(String name, Configuration owner) {
    this(name, owner, InterfaceType.UNKNOWN);

    // Determine interface type after setting owner
    _interfaceType =
        ((_key == null) || (_owner == null))
            ? InterfaceType.UNKNOWN
            : computeInterfaceType(_key, _owner.getConfigurationFormat());
  }

  private Interface(String name, Configuration owner, @Nonnull InterfaceType interfaceType) {
    super(name);
    _active = true;
    _additionalArpIps = EmptyIpSpace.INSTANCE;
    _autoState = true;
    _allowedVlans = IntegerSpace.EMPTY;
    _allAddresses = ImmutableSortedSet.of();
    _channelGroupMembers = ImmutableSortedSet.of();
    _declaredNames = ImmutableSortedSet.of();
    _dependencies = ImmutableSet.of();
    _dhcpRelayAddresses = ImmutableList.of();
    _hsrpGroups = new TreeMap<>();
    _interfaceType = interfaceType;
    _mtu = DEFAULT_MTU;
    _owner = owner;
    _switchportMode = SwitchportMode.NONE;
    _switchportTrunkEncapsulation = SwitchportEncapsulationType.DOT1Q;
    _vrfName = Configuration.DEFAULT_VRF_NAME;
    _vrrpGroups = new TreeMap<>();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof Interface)) {
      return false;
    }
    Interface other = (Interface) o;
    if (!Objects.equals(_accessVlan, other._accessVlan)) {
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

    if (!Objects.equals(_eigrp, other._eigrp)) {
      return false;
    }

    if (!Objects.equals(_encapsulationVlan, other._encapsulationVlan)) {
      return false;
    }

    // TODO: check ISIS settings for equality.
    if (_mtu != other._mtu) {
      return false;
    }
    if (!Objects.equals(_nativeVlan, other._nativeVlan)) {
      return false;
    }
    // TODO: check OSPF settings for equality.
    if (!IpAccessList.bothNullOrSameName(this._outgoingFilter, other._outgoingFilter)) {
      return false;
    }
    if (!_proxyArp == other._proxyArp) {
      return false;
    }
    if (!Objects.equals(_routingPolicyName, other._routingPolicyName)) {
      return false;
    }
    if (!Objects.equals(_speed, other._speed)) {
      return false;
    }
    if (!Objects.equals(this._switchportMode, other._switchportMode)) {
      return false;
    }
    if (!Objects.equals(this._zoneName, other._zoneName)) {
      return false;
    }
    if (!IpAccessList.bothNullOrSameName(
        this._postTransformationIncomingFilter, other._postTransformationIncomingFilter)) {
      return false;
    }
    if (!IpAccessList.bothNullOrSameName(
        this._preTransformationOutgoingFilter, other._preTransformationOutgoingFilter)) {
      return false;
    }
    return true;
  }

  /** Number of access VLAN when switchport mode is ACCESS. */
  @JsonProperty(PROP_ACCESS_VLAN)
  @Nullable
  public Integer getAccessVlan() {
    return _accessVlan;
  }

  /** Whether this interface is administratively active (true) or disabled (false). */
  @JsonProperty(PROP_ACTIVE)
  public boolean getActive() {
    return _active;
  }

  @JsonProperty(PROP_ADDITIONAL_ARP_IPS)
  public IpSpace getAdditionalArpIps() {
    return _additionalArpIps;
  }

  /** Ranges of allowed VLANs when switchport mode is TRUNK. */
  @JsonProperty(PROP_ALLOWED_VLANS)
  public IntegerSpace getAllowedVlans() {
    return _allowedVlans;
  }

  /**
   * All IPV4 address/network assignments on this interface. These are addresses that are routable
   * to/through this interface.
   */
  @JsonProperty(PROP_ALL_PREFIXES)
  public Set<ConcreteInterfaceAddress> getAllConcreteAddresses() {
    if (_allConcreteAddresses == null) {
      _allConcreteAddresses =
          _allAddresses.stream()
              .filter(a -> a instanceof ConcreteInterfaceAddress)
              .map(a -> (ConcreteInterfaceAddress) a)
              .collect(ImmutableSet.toImmutableSet());
    }
    return _allConcreteAddresses;
  }

  /**
   * Return all link-local addresses of this interface. These addresses are valid only in a given
   * layer 2 broadcast domain, and cannot be routed to across a broadcast domain
   */
  @JsonIgnore
  public Set<LinkLocalAddress> getAllLinkLocalAddresses() {
    if (_allLinkLocalAddresses == null) {
      _allLinkLocalAddresses =
          _allAddresses.stream()
              .filter(a -> a instanceof LinkLocalAddress)
              .map(a -> (LinkLocalAddress) a)
              .collect(ImmutableSet.toImmutableSet());
    }
    return _allLinkLocalAddresses;
  }

  @Nonnull
  public Set<InterfaceAddress> getAllAddresses() {
    return _allAddresses;
  }

  /**
   * Whether this VLAN interface's operational status is dependent on corresponding member
   * switchports.
   */
  @JsonProperty(PROP_AUTOSTATE)
  public boolean getAutoState() {
    return _autoState;
  }

  /** The nominal bandwidth of this interface in bits/sec for use in protocol cost calculations. */
  @JsonProperty(PROP_BANDWIDTH)
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

  /** Return the set of interfaces this interface depends on (see {@link Dependency}). */
  @JsonIgnore
  @Nonnull
  public Set<Dependency> getDependencies() {
    return _dependencies;
  }

  /** Description of this interface. */
  @JsonProperty(PROP_DESCRIPTION)
  public String getDescription() {
    return _description;
  }

  @JsonProperty(PROP_DHCP_RELAY_ADDRESSES)
  public List<Ip> getDhcpRelayAddresses() {
    return _dhcpRelayAddresses;
  }

  @JsonProperty(PROP_EIGRP)
  public @Nullable EigrpInterfaceSettings getEigrp() {
    return _eigrp;
  }

  @JsonProperty(PROP_ENCAPSULATION_VLAN)
  public @Nullable Integer getEncapsulationVlan() {
    return _encapsulationVlan;
  }

  @JsonProperty(PROP_FIREWALL_SESSION_INTERFACE_INFO)
  public @Nullable FirewallSessionInterfaceInfo getFirewallSessionInterfaceInfo() {
    return _firewallSessionInterfaceInfo;
  }

  /** Mapping: hsrpGroupID -&gt; HsrpGroup */
  @JsonProperty(PROP_HSRP_GROUPS)
  public Map<Integer, HsrpGroup> getHsrpGroups() {
    return _hsrpGroups;
  }

  /** Version of the HSRP protocol to use */
  @JsonProperty(PROP_HSRP_VERSION)
  public @Nullable String getHsrpVersion() {
    return _hsrpVersion;
  }

  @JsonIgnore
  public IpAccessList getInboundFilter() {
    return _inboundFilter;
  }

  /** The IPV4 access-list used to filter traffic destined for this device on this interface. */
  @JsonProperty(PROP_INBOUND_FILTER)
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

  /** The IPV4 access-list used to filter traffic that arrives on this interface. */
  @JsonProperty(PROP_INCOMING_FILTER)
  public String getIncomingFilterName() {
    if (_incomingFilter != null) {
      return _incomingFilter.getName();
    } else {
      return _incomingFilterName;
    }
  }

  @JsonProperty(PROP_INCOMING_TRANSFORMATION)
  public Transformation getIncomingTransformation() {
    return _incomingTransformation;
  }

  /** The type of this interface. */
  @JsonProperty(PROP_INTERFACE_TYPE)
  public InterfaceType getInterfaceType() {
    return _interfaceType;
  }

  @JsonProperty(PROP_ISIS)
  public @Nullable IsisInterfaceSettings getIsis() {
    return _isis;
  }

  @JsonProperty(PROP_MLAG_ID)
  @Nullable
  public Integer getMlagId() {
    return _mlagId;
  }

  /** The maximum transmission unit (MTU) of this interface in bytes. */
  @JsonProperty(PROP_MTU)
  public int getMtu() {
    return _mtu;
  }

  /** The native VLAN of this interface when switchport mode is TRUNK. */
  @JsonProperty(PROP_NATIVE_VLAN)
  @Nullable
  public Integer getNativeVlan() {
    return _nativeVlan;
  }

  @JsonIgnore
  @Nullable
  public OspfArea getOspfArea() {
    if (_ospfProcess == null
        || _ospfAreaName == null
        || !_vrf.getOspfProcesses().containsKey(_ospfProcess)) {
      return null;
    }
    return _vrf.getOspfProcesses().get(_ospfProcess).getAreas().get(_ospfAreaName);
  }

  /** The OSPF area to which this interface belongs. */
  @JsonProperty(PROP_OSPF_AREA)
  public Long getOspfAreaName() {
    return _ospfAreaName;
  }

  /** The explicit OSPF cost of this interface. If unset, the cost is automatically calculated. */
  @JsonProperty(PROP_OSPF_COST)
  public Integer getOspfCost() {
    return _ospfCost;
  }

  /** Dead-interval in seconds for OSPF updates. */
  @JsonProperty(PROP_OSPF_DEAD_INTERVAL)
  public int getOspfDeadInterval() {
    return _ospfDeadInterval;
  }

  /** Whether or not OSPF is enabled at all on this interface (either actively or passively). */
  @JsonProperty(PROP_OSPF_ENABLED)
  public boolean getOspfEnabled() {
    return _ospfEnabled;
  }

  /** Number of OSPF packets to send out during dead-interval period for fast OSPF updates. */
  @JsonProperty(PROP_OSPF_HELLO_MULTIPLIER)
  public int getOspfHelloMultiplier() {
    return _ospfHelloMultiplier;
  }

  /**
   * "Returns name of the routing policy which is generated from the Global and Interface level
   * inbound distribute-lists for OSPF"
   */
  @JsonProperty(PROP_OSPF_INBOUND_DISTRIBUTE_LIST_POLICY)
  @Nullable
  public String getOspfInboundDistributeListPolicy() {
    return _ospfInboundDistributeListPolicy;
  }

  /**
   * Whether or not OSPF is enabled passively on this interface. If passive, this interface is
   * included in the OSPF RIB, but no OSPF packets are sent from it.
   */
  @JsonProperty(PROP_OSPF_PASSIVE)
  public boolean getOspfPassive() {
    return _ospfPassive;
  }

  @JsonProperty(PROP_OSPF_POINT_TO_POINT)
  public boolean getOspfPointToPoint() {
    return _ospfPointToPoint;
  }

  @JsonProperty(PROP_OSPF_PROCESS)
  @Nullable
  public String getOspfProcess() {
    return _ospfProcess;
  }

  @JsonIgnore
  public IpAccessList getOutgoingFilter() {
    return _outgoingFilter;
  }

  /** The IPV4 access-list used to filter traffic that is sent out this interface. Stored as @id. */
  @JsonProperty(PROP_OUTGOING_FILTER)
  public String getOutgoingFilterName() {
    if (_outgoingFilter != null) {
      return _outgoingFilter.getName();
    } else {
      return _outgoingFilterName;
    }
  }

  @JsonProperty(PROP_OUTGOING_TRANSFORMATION)
  public Transformation getOutgoingTransformation() {
    return _outgoingTransformation;
  }

  @JsonIgnore
  public Configuration getOwner() {
    return _owner;
  }

  /** The primary IPV4 address/network of this interface. */
  @JsonProperty(PROP_PREFIX)
  @Nullable
  public InterfaceAddress getAddress() {
    return _address;
  }

  @JsonIgnore
  @Nullable
  public ConcreteInterfaceAddress getConcreteAddress() {
    return _address instanceof ConcreteInterfaceAddress
        ? (ConcreteInterfaceAddress) _address
        : null;
  }

  @JsonIgnore
  public IpAccessList getPostTransformationIncomingFilter() {
    return _postTransformationIncomingFilter;
  }

  /** The IPV4 access-list used to filter incoming traffic after applying destination NAT. */
  @JsonProperty(PROP_POST_TRANSFORMATION_INCOMING_FILTER)
  public String getPostTransformationIncomingFilterName() {
    if (_postTransformationIncomingFilter != null) {
      return _postTransformationIncomingFilter.getName();
    } else {
      return _postTransformationIncomingFilterName;
    }
  }

  @JsonIgnore
  public IpAccessList getPreTransformationOutgoingFilter() {
    return _preTransformationOutgoingFilter;
  }

  /** The IPV4 access-list used to filter outgoing traffic before applying source NAT. */
  @JsonProperty(PROP_PRE_TRANSFORMATION_OUTGOING_FILTER)
  public String getPreTransformationOutgoingFilterName() {
    if (_preTransformationOutgoingFilter != null) {
      return _preTransformationOutgoingFilter.getName();
    } else {
      return _preTransformationOutgoingFilterName;
    }
  }

  @JsonIgnore
  public @Nullable Prefix getPrimaryNetwork() {
    if (_address instanceof ConcreteInterfaceAddress) {
      return ((ConcreteInterfaceAddress) _address).getPrefix();
    }
    return null;
  }

  /** Whether or not proxy-ARP is enabled on this interface. */
  @JsonProperty(PROP_PROXY_ARP)
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

  /**
   * The name of the policy used on this interface for policy routing (as opposed to
   * destination-based routing).
   */
  @JsonProperty(PROP_ROUTING_POLICY)
  public String getRoutingPolicyName() {
    return _routingPolicyName;
  }

  /** Whether or not spanning-tree portfast feature is enabled. */
  @JsonProperty(PROP_SPANNING_TREE_PORTFAST)
  public boolean getSpanningTreePortfast() {
    return _spanningTreePortfast;
  }

  /** The link speed of this interface, in bits per second (bps) */
  public @Nullable Double getSpeed() {
    return _speed;
  }

  /** Whether this interface is configured as a switchport. */
  @JsonProperty(PROP_SWITCHPORT)
  public boolean getSwitchport() {
    return _switchport;
  }

  /** The switchport mode (if any) of this interface. */
  @JsonProperty(PROP_SWITCHPORT_MODE)
  public SwitchportMode getSwitchportMode() {
    return _switchportMode;
  }

  /**
   * The switchport trunk encapsulation type of this interface. Only relevant when switchport mode
   * is TRUNK.
   */
  @JsonProperty(PROP_SWITCHPORT_TRUNK_ENCAPSULATION)
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

  /** The name of the VRF to which this interface belongs. */
  @JsonProperty(PROP_VRF)
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

  /** The firewall zone to which this interface belongs. */
  @JsonProperty(PROP_ZONE)
  public String getZoneName() {
    return _zoneName;
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

  @JsonProperty(PROP_ACCESS_VLAN)
  public void setAccessVlan(@Nullable Integer vlan) {
    _accessVlan = vlan;
  }

  @JsonProperty(PROP_ACTIVE)
  public void setActive(boolean active) {
    _active = active;
  }

  @JsonProperty(PROP_ADDITIONAL_ARP_IPS)
  public void setAdditionalArpIps(IpSpace additionalArpIps) {
    _additionalArpIps = firstNonNull(additionalArpIps, EmptyIpSpace.INSTANCE);
  }

  @JsonProperty(PROP_ALLOWED_VLANS)
  public void setAllowedVlans(IntegerSpace allowedVlans) {
    _allowedVlans = allowedVlans;
  }

  @JsonProperty(PROP_ALL_PREFIXES)
  public void setAllAddresses(Iterable<? extends InterfaceAddress> allAddresses) {
    _allAddresses = ImmutableSortedSet.copyOf(allAddresses);
    // Clear cached values
    _allLinkLocalAddresses = null;
    _allConcreteAddresses = null;
  }

  @JsonProperty(PROP_AUTOSTATE)
  public void setAutoState(boolean autoState) {
    _autoState = autoState;
  }

  @JsonProperty(PROP_BANDWIDTH)
  public void setBandwidth(@Nullable Double bandwidth) {
    _bandwidth = bandwidth;
  }

  /**
   * To be used by the builder only. Use {@link #blacklist()} for public blacklisting, it enforces
   * that the interface is inactive if blacklisted.
   */
  @JsonIgnore
  private void setBlacklisted(boolean blacklisted) {
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

  /** Set (overwrite) all dependencies for this interface */
  @JsonIgnore
  public void setDependencies(@Nonnull Collection<Dependency> dependencies) {
    _dependencies = ImmutableSet.copyOf(dependencies);
  }

  /** Add an interface dependency to this interface */
  public void addDependency(@Nonnull Dependency dependency) {
    _dependencies =
        ImmutableSet.<Dependency>builder().addAll(_dependencies).add(dependency).build();
  }

  @JsonProperty(PROP_DESCRIPTION)
  public void setDescription(String description) {
    _description = description;
  }

  @JsonProperty(PROP_DHCP_RELAY_ADDRESSES)
  public void setDhcpRelayAddresses(List<Ip> dhcpRelayAddresses) {
    _dhcpRelayAddresses = ImmutableList.copyOf(dhcpRelayAddresses);
  }

  @JsonProperty(PROP_EIGRP)
  public void setEigrp(@Nullable EigrpInterfaceSettings eigrp) {
    _eigrp = eigrp;
  }

  @JsonProperty(PROP_ENCAPSULATION_VLAN)
  public void setEncapsulationVlan(@Nullable Integer encapsulationVlan) {
    _encapsulationVlan = encapsulationVlan;
  }

  @JsonProperty(PROP_FIREWALL_SESSION_INTERFACE_INFO)
  public void setFirewallSessionInterfaceInfo(
      @Nullable FirewallSessionInterfaceInfo firewallSessionInterfaceInfo) {
    _firewallSessionInterfaceInfo = firewallSessionInterfaceInfo;
  }

  @JsonProperty(PROP_HSRP_GROUPS)
  public void setHsrpGroups(@Nonnull Map<Integer, HsrpGroup> hsrpGroups) {
    _hsrpGroups = hsrpGroups;
  }

  @JsonProperty(PROP_HSRP_VERSION)
  public void setHsrpVersion(String hsrpVersion) {
    _hsrpVersion = hsrpVersion;
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

  @JsonProperty(PROP_INCOMING_TRANSFORMATION)
  public void setIncomingTransformation(Transformation incomingTransformation) {
    _incomingTransformation = incomingTransformation;
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

  @JsonProperty(PROP_MLAG_ID)
  public void setMlagId(Integer mlagId) {
    _mlagId = mlagId;
  }

  @JsonProperty(PROP_MTU)
  public void setMtu(int mtu) {
    _mtu = mtu;
  }

  @JsonProperty(PROP_NATIVE_VLAN)
  public void setNativeVlan(@Nullable Integer vlan) {
    _nativeVlan = vlan;
  }

  @JsonIgnore
  public void setOspfArea(@Nullable OspfArea ospfArea) {
    if (ospfArea == null) {
      _ospfAreaName = null;
    } else {
      _ospfAreaName = ospfArea.getAreaNumber();
    }
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

  @JsonProperty(PROP_OSPF_INBOUND_DISTRIBUTE_LIST_POLICY)
  public void setOspfInboundDistributeListPolicy(@Nullable String ospfInboundDistributeListPolicy) {
    _ospfInboundDistributeListPolicy = ospfInboundDistributeListPolicy;
  }

  @JsonProperty(PROP_OSPF_PASSIVE)
  public void setOspfPassive(boolean passive) {
    _ospfPassive = passive;
  }

  @JsonProperty(PROP_OSPF_POINT_TO_POINT)
  public void setOspfPointToPoint(boolean ospfPointToPoint) {
    _ospfPointToPoint = ospfPointToPoint;
  }

  public void setOspfProcess(@Nullable String ospfProcess) {
    _ospfProcess = ospfProcess;
  }

  @JsonIgnore
  public void setOutgoingFilter(IpAccessList outgoingFilter) {
    _outgoingFilter = outgoingFilter;
  }

  @JsonProperty(PROP_OUTGOING_FILTER)
  public void setOutgoingFilter(String outgoingFilterName) {
    _outgoingFilterName = outgoingFilterName;
  }

  @JsonProperty(PROP_OUTGOING_TRANSFORMATION)
  public void setOutgoingTransformation(Transformation outgoingTransformation) {
    _outgoingTransformation = outgoingTransformation;
  }

  @JsonIgnore
  public void setOwner(Configuration owner) {
    _owner = owner;
  }

  @JsonProperty(PROP_PREFIX)
  public void setAddress(InterfaceAddress address) {
    _address = address;
    // Clear cached values
    _allLinkLocalAddresses = null;
    _allConcreteAddresses = null;
  }

  @JsonIgnore
  public void setPostTransformationIncomingFilter(IpAccessList postTransformationIncomingFilter) {
    _postTransformationIncomingFilter = postTransformationIncomingFilter;
  }

  @JsonProperty(PROP_POST_TRANSFORMATION_INCOMING_FILTER)
  public void setPostTransformationIncomingFilter(String postTransformationIncomingFilterName) {
    _postTransformationIncomingFilterName = postTransformationIncomingFilterName;
  }

  @JsonIgnore
  public void setPreTransformationOutgoingFilter(IpAccessList preTransformationOutgoingFilter) {
    _preTransformationOutgoingFilter = preTransformationOutgoingFilter;
  }

  @JsonProperty(PROP_PRE_TRANSFORMATION_OUTGOING_FILTER)
  public void setPreTransformationOutgoingFilter(String preTransformationOutgoingFilterName) {
    _preTransformationOutgoingFilterName = preTransformationOutgoingFilterName;
  }

  @JsonProperty(PROP_PROXY_ARP)
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

  @JsonProperty(PROP_ROUTING_POLICY)
  public void setRoutingPolicy(String routingPolicyName) {
    _routingPolicyName = routingPolicyName;
  }

  @JsonProperty(PROP_SPANNING_TREE_PORTFAST)
  public void setSpanningTreePortfast(boolean spanningTreePortfast) {
    _spanningTreePortfast = spanningTreePortfast;
  }

  @JsonProperty(PROP_SPEED)
  public void setSpeed(@Nullable Double speed) {
    _speed = speed;
  }

  @JsonProperty(PROP_SWITCHPORT)
  public void setSwitchport(boolean switchport) {
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

  @JsonProperty(PROP_ZONE)
  public void setZoneName(String zoneName) {
    _zoneName = zoneName;
  }

  public void addVrrpGroup(Integer num, @Nonnull VrrpGroup group) {
    _vrrpGroups =
        ImmutableSortedMap.<Integer, VrrpGroup>naturalOrder()
            .putAll(_vrrpGroups)
            .put(num, group)
            .build();
  }

  /** Blacklist this interface, making it inactive and blacklisted */
  public void blacklist() {
    setActive(false);
    _blacklisted = true;
  }

  /**
   * Check if the given interface name is <b>not</b> one of the special values defined by batfish or
   * virtual null interface.
   */
  public static boolean isRealInterfaceName(@Nonnull String name) {
    return !ImmutableList.of(
            UNSET_LOCAL_INTERFACE,
            DYNAMIC_INTERFACE_NAME,
            NULL_INTERFACE_NAME,
            INVALID_LOCAL_INTERFACE)
        .contains(name);
  }
}

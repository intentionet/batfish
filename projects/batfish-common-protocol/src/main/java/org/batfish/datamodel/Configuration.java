package org.batfish.datamodel;

import static com.google.common.base.Predicates.not;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.tracking.TrackMethod;
import org.batfish.datamodel.vendor_family.VendorFamily;

/**
 * A Configuration represents an autonomous network device, such as a router, host, switch, or
 * firewall.
 */
public final class Configuration implements Serializable {

  public static class Builder extends NetworkFactoryBuilder<Configuration> {

    private ConfigurationFormat _configurationFormat;

    private String _domainName;

    private String _hostname;

    private LineAction _defaultCrossZoneAction;

    private LineAction _defaultInboundAction;

    Builder(NetworkFactory networkFactory) {
      super(networkFactory, Configuration.class);
    }

    @Override
    public Configuration build() {
      String name = _hostname != null ? _hostname : generateName();
      Configuration configuration = new Configuration(name, _configurationFormat);
      if (_defaultCrossZoneAction != null) {
        configuration.setDefaultCrossZoneAction(_defaultCrossZoneAction);
      }
      if (_defaultInboundAction != null) {
        configuration.setDefaultInboundAction(_defaultInboundAction);
      }
      configuration.setDomainName(_domainName);
      return configuration;
    }

    public Builder setConfigurationFormat(ConfigurationFormat configurationFormat) {
      _configurationFormat = configurationFormat;
      return this;
    }

    public Builder setDomainName(String domainName) {
      _domainName = domainName;
      return this;
    }

    public Builder setHostname(String hostname) {
      _hostname = hostname;
      return this;
    }

    public Builder setDefaultCrossZoneAction(LineAction defaultCrossZoneAction) {
      _defaultCrossZoneAction = defaultCrossZoneAction;
      return this;
    }

    public Builder setDefaultInboundAction(LineAction defaultInboundAction) {
      _defaultInboundAction = defaultInboundAction;
      return this;
    }
  }

  public static final String DEFAULT_VRF_NAME = "default";

  public static final String NODE_NONE_NAME = "(none)";

  private static final String PROP_AS_PATH_ACCESS_LISTS = "asPathAccessLists";

  private static final String PROP_AUTHENTICATION_KEY_CHAINS = "authenticationKeyChains";

  private static final String PROP_COMMUNITY_LISTS = "communityLists";

  private static final String PROP_CONFIGURATION_FORMAT = "configurationFormat";

  private static final String PROP_DEFAULT_CROSS_ZONE_ACTION = "defaultCrossZoneAction";

  private static final String PROP_DEFAULT_INBOUND_ACTION = "defaultInboundAction";

  private static final String PROP_DEVICE_TYPE = "deviceType";

  private static final String PROP_DNS_SOURCE_INTERFACE = "dnsSourceInterface";

  private static final String PROP_DOMAIN_NAME = "domainName";

  private static final String PROP_IKE_PHASE1_KEYS = "ikePhase1Keys";

  private static final String PROP_IKE_PHASE1_POLICIES = "ikePhase1Policies";

  private static final String PROP_IKE_PHASE1_PROPOSALS = "ikePhase1Proposals";

  private static final String PROP_INTERFACES = "interfaces";

  private static final String PROP_IP6_ACCESS_LISTS = "ip6AccessLists";

  private static final String PROP_IP_ACCESS_LISTS = "ipAccessLists";

  private static final String PROP_IP_SPACES = "ipSpaces";

  private static final String PROP_IP_SPACE_METADATA = "ipSpaceMetadata";

  private static final String PROP_IPSEC_PEER_CONFIGS = "ipsecPeerConfigs";

  private static final String PROP_IPSEC_PHASE2_POLICIES = "ipsecPhase2Policies";

  private static final String PROP_IPSEC_PHASE2_PROPOSALS = "ipsecPhase2Proposals";

  private static final String PROP_LOGGING_SERVERS = "loggingServers";

  private static final String PROP_LOGGING_SOURCE_INTERFACE = "loggingSourceInterface";

  private static final String PROP_MLAGS = "mlags";

  private static final String PROP_NAME = "name";

  private static final String PROP_NTP_SERVERS = "ntpServers";

  private static final String PROP_NTP_SOURCE_INTERFACE = "ntpSourceInterface";

  private static final String PROP_PACKET_POLICIES = "packetPolicies";

  private static final String PROP_ROUTE6_FILTER_LISTS = "route6FilterLists";

  private static final String PROP_ROUTE_FILTER_LISTS = "routeFilterLists";

  private static final String PROP_ROUTING_POLICIES = "routingPolicies";

  private static final String PROP_SNMP_SOURCE_INTERFACE = "snmpSourceInterface";

  private static final String PROP_SNMP_TRAP_SERVERS = "snmpTrapServers";

  private static final String PROP_TACACS_SERVERS = "tacacsServers";

  private static final String PROP_TACACS_SOURCE_INTERFACE = "tacacsSourceInterface";

  private static final String PROP_TRACKING_GROUPS = "trackingGroups";

  private static final String PROP_VENDOR_FAMILY = "vendorFamily";

  private static final String PROP_VRFS = "vrfs";

  private static final String PROP_ZONES = "zones";

  private static final long serialVersionUID = 1L;

  private static final int VLAN_NORMAL_MAX_DEFAULT = 4094;

  private static final int VLAN_NORMAL_MIN_DEFAULT = 1;

  private NavigableMap<String, AsPathAccessList> _asPathAccessLists;

  private NavigableMap<String, AuthenticationKeyChain> _authenticationKeyChains;

  private transient NavigableSet<BgpAdvertisement> _bgpAdvertisements;

  private NavigableMap<String, CommunityList> _communityLists;

  private final ConfigurationFormat _configurationFormat;

  private LineAction _defaultCrossZoneAction;

  private LineAction _defaultInboundAction;

  private DeviceType _deviceType;

  private NavigableSet<String> _dnsServers;

  private String _dnsSourceInterface;

  private String _domainName;

  private @Nonnull NavigableMap<String, IkePhase1Key> _ikePhase1keys;

  private NavigableMap<String, IkePhase1Proposal> _ikePhase1Proposals;

  private NavigableMap<String, IkePhase1Policy> _ikePhase1Policies;

  private NavigableMap<String, Interface> _interfaces;

  private NavigableMap<String, Ip6AccessList> _ip6AccessLists;

  private NavigableMap<String, IpAccessList> _ipAccessLists;

  private NavigableMap<String, IpSpace> _ipSpaces;

  private NavigableMap<String, IpSpaceMetadata> _ipSpaceMetadata;

  private NavigableMap<String, IpsecPeerConfig> _ipsecPeerConfigs;

  private NavigableMap<String, IpsecPhase2Policy> _ipsecPhase2Policies;

  private NavigableMap<String, IpsecPhase2Proposal> _ipsecPhase2Proposals;

  private NavigableSet<String> _loggingServers;

  private String _loggingSourceInterface;

  private NavigableMap<String, Mlag> _mlags;

  private final String _name;

  /** Normal =&gt; Excluding extended and reserved vlans that should not be modified or deleted. */
  private SubRange _normalVlanRange;

  private NavigableSet<String> _ntpServers;

  private String _ntpSourceInterface;

  private NavigableMap<String, PacketPolicy> _packetPolicies;

  private NavigableMap<String, Route6FilterList> _route6FilterLists;

  private NavigableMap<String, RouteFilterList> _routeFilterLists;

  private NavigableMap<String, RoutingPolicy> _routingPolicies;

  private String _snmpSourceInterface;

  private NavigableSet<String> _snmpTrapServers;

  private NavigableSet<String> _tacacsServers;

  private String _tacacsSourceInterface;

  private NavigableMap<String, TrackMethod> _trackingGroups;

  private VendorFamily _vendorFamily;

  private Map<String, Vrf> _vrfs;

  private NavigableMap<String, Zone> _zones;

  @JsonCreator
  private static Configuration makeConfiguration(
      @JsonProperty(PROP_NAME) String hostname,
      @Nullable @JsonProperty(PROP_CONFIGURATION_FORMAT) ConfigurationFormat configurationFormat) {
    if (configurationFormat == null) {
      throw new BatfishException("Configuration format cannot be null");
    }
    return new Configuration(hostname, configurationFormat);
  }

  public Configuration(String hostname, @Nonnull ConfigurationFormat configurationFormat) {
    _name = hostname;
    _asPathAccessLists = new TreeMap<>();
    _authenticationKeyChains = new TreeMap<>();
    _communityLists = new TreeMap<>();
    _configurationFormat = configurationFormat;
    _dnsServers = new TreeSet<>();
    _domainName = null;
    _ikePhase1keys = ImmutableSortedMap.of();
    _ikePhase1Policies = new TreeMap<>();
    _ikePhase1Proposals = new TreeMap<>();
    _interfaces = new TreeMap<>();
    _ipAccessLists = new TreeMap<>();
    _ip6AccessLists = new TreeMap<>();
    _ipSpaces = new TreeMap<>();
    _ipSpaceMetadata = new TreeMap<>();
    _ipsecPeerConfigs = ImmutableSortedMap.of();
    _ipsecPhase2Policies = ImmutableSortedMap.of();
    _ipsecPhase2Proposals = ImmutableSortedMap.of();
    _loggingServers = new TreeSet<>();
    _mlags = ImmutableSortedMap.of();
    _normalVlanRange = new SubRange(VLAN_NORMAL_MIN_DEFAULT, VLAN_NORMAL_MAX_DEFAULT);
    _ntpServers = new TreeSet<>();
    _packetPolicies = new TreeMap<>();
    _routeFilterLists = new TreeMap<>();
    _route6FilterLists = new TreeMap<>();
    _routingPolicies = new TreeMap<>();
    _snmpTrapServers = new TreeSet<>();
    _tacacsServers = new TreeSet<>();
    _trackingGroups = new TreeMap<>();
    _vendorFamily = new VendorFamily();
    _vrfs = new TreeMap<>();
    _zones = new TreeMap<>();
  }

  private void computeRoutingPolicySources(String routingPolicyName, Warnings w) {
    if (routingPolicyName == null) {
      return;
    }
    RoutingPolicy routingPolicy = _routingPolicies.get(routingPolicyName);
    if (routingPolicy == null) {
      return;
    }
    routingPolicy.computeSources(Collections.emptySet(), _routingPolicies, w);
  }

  public void computeRoutingPolicySources(Warnings w) {
    for (String rpName : _routingPolicies.keySet()) {
      computeRoutingPolicySources(rpName, w);
    }
    for (Vrf vrf : _vrfs.values()) {
      BgpProcess bgpProcess = vrf.getBgpProcess();
      if (bgpProcess != null) {
        for (BgpPeerConfig neighbor :
            Iterables.concat(
                bgpProcess.getActiveNeighbors().values(),
                bgpProcess.getPassiveNeighbors().values())) {
          neighbor.setExportPolicySources(getRoutingPolicySources(neighbor.getExportPolicy()));
          neighbor.setImportPolicySources(getRoutingPolicySources(neighbor.getImportPolicy()));
        }
      }
      OspfProcess ospfProcess = vrf.getOspfProcess();
      if (ospfProcess != null) {
        ospfProcess.setExportPolicySources(getRoutingPolicySources(ospfProcess.getExportPolicy()));
      }
      for (GeneratedRoute gr : vrf.getGeneratedRoutes()) {
        gr.setAttributePolicySources(getRoutingPolicySources(gr.getAttributePolicy()));
        gr.setGenerationPolicySources(getRoutingPolicySources(gr.getGenerationPolicy()));
      }
    }
  }

  @JsonProperty(PROP_AS_PATH_ACCESS_LISTS)
  @JsonPropertyDescription("Dictionary of all AS-path access-lists for this node.")
  public NavigableMap<String, AsPathAccessList> getAsPathAccessLists() {
    return _asPathAccessLists;
  }

  @JsonProperty(PROP_AUTHENTICATION_KEY_CHAINS)
  @JsonPropertyDescription("Dictionary of all authentication key chains for this node.")
  public NavigableMap<String, AuthenticationKeyChain> getAuthenticationKeyChains() {
    return _authenticationKeyChains;
  }

  @JsonIgnore
  public NavigableSet<BgpAdvertisement> getBgpAdvertisements() {
    return _bgpAdvertisements;
  }

  /** Returns the lowest IP across all interfaces for now. We'll improve it later. */
  @JsonIgnore
  @Nullable
  public Ip getCanonicalIp() {
    return getVrfs().values().stream()
        .flatMap(v -> v.getInterfaces().values().stream())
        .flatMap(i -> i.getAllAddresses().stream())
        .map(InterfaceAddress::getIp)
        .min(Ip::compareTo)
        .orElse(null);
  }

  public Set<String> activeInterfaces() {
    return _interfaces.values().stream()
        .filter(Interface::getActive)
        .map(Interface::getName)
        .collect(ImmutableSet.toImmutableSet());
  }

  @JsonProperty(PROP_COMMUNITY_LISTS)
  @JsonPropertyDescription("Dictionary of all community-lists for this node.")
  public NavigableMap<String, CommunityList> getCommunityLists() {
    return _communityLists;
  }

  @JsonProperty(PROP_CONFIGURATION_FORMAT)
  @JsonPropertyDescription(
      "Best guess at vendor configuration format. Used for setting default values, protocol "
          + "costs, etc.")
  public ConfigurationFormat getConfigurationFormat() {
    return _configurationFormat;
  }

  @JsonProperty(PROP_DEFAULT_CROSS_ZONE_ACTION)
  @JsonPropertyDescription(
      "Default forwarding action to take for traffic that crosses firewall zones.")
  public LineAction getDefaultCrossZoneAction() {
    return _defaultCrossZoneAction;
  }

  @JsonProperty(PROP_DEFAULT_INBOUND_ACTION)
  @JsonPropertyDescription(
      "Default forwarding action to take for traffic destined for this device.")
  public LineAction getDefaultInboundAction() {
    return _defaultInboundAction;
  }

  @JsonIgnore
  public Vrf getDefaultVrf() {
    return _vrfs.get(DEFAULT_VRF_NAME);
  }

  @JsonProperty(PROP_DEVICE_TYPE)
  public DeviceType getDeviceType() {
    return _deviceType;
  }

  public NavigableSet<String> getDnsServers() {
    return _dnsServers;
  }

  @JsonProperty(PROP_DNS_SOURCE_INTERFACE)
  public String getDnsSourceInterface() {
    return _dnsSourceInterface;
  }

  @JsonProperty(PROP_DOMAIN_NAME)
  @JsonPropertyDescription("Domain name of this node.")
  public String getDomainName() {
    return _domainName;
  }

  @JsonProperty(PROP_NAME)
  @JsonPropertyDescription("Hostname of this node.")
  public String getHostname() {
    return _name;
  }

  @JsonProperty(PROP_IKE_PHASE1_KEYS)
  @JsonPropertyDescription("Dictionary of all IKE phase1 keys for this node.")
  public NavigableMap<String, IkePhase1Key> getIkePhase1Keys() {
    return _ikePhase1keys;
  }

  @JsonProperty(PROP_IKE_PHASE1_POLICIES)
  @JsonPropertyDescription("Dictionary of all IKE phase1 policies for this node.")
  public NavigableMap<String, IkePhase1Policy> getIkePhase1Policies() {
    return _ikePhase1Policies;
  }

  @JsonProperty(PROP_IKE_PHASE1_PROPOSALS)
  @JsonPropertyDescription("Dictionary of all IKE phase1 proposals for this node.")
  public NavigableMap<String, IkePhase1Proposal> getIkePhase1Proposals() {
    return _ikePhase1Proposals;
  }

  @JsonPropertyDescription("Dictionary of all interfaces across all VRFs for this node.")
  @JsonProperty(PROP_INTERFACES)
  public NavigableMap<String, Interface> getAllInterfaces() {
    return _interfaces;
  }

  @JsonIgnore
  @Deprecated // to enable users to migrate to new API.
  public NavigableMap<String, Interface> getInterfaces() {
    return getAllInterfaces();
  }

  @JsonPropertyDescription("Dictionary of all IPV6 access-lists for this node.")
  @JsonProperty(PROP_IP6_ACCESS_LISTS)
  public NavigableMap<String, Ip6AccessList> getIp6AccessLists() {
    return _ip6AccessLists;
  }

  @JsonProperty(PROP_IP_ACCESS_LISTS)
  @JsonPropertyDescription("Dictionary of all IPV4 access-lists for this node.")
  public NavigableMap<String, IpAccessList> getIpAccessLists() {
    return _ipAccessLists;
  }

  @JsonPropertyDescription("Dictionary of all IPSec peer configs for this node")
  @JsonProperty(PROP_IPSEC_PEER_CONFIGS)
  public NavigableMap<String, IpsecPeerConfig> getIpsecPeerConfigs() {
    return _ipsecPeerConfigs;
  }

  @JsonProperty(PROP_IP_SPACES)
  public NavigableMap<String, IpSpace> getIpSpaces() {
    return _ipSpaces;
  }

  @JsonProperty(PROP_IP_SPACE_METADATA)
  public NavigableMap<String, IpSpaceMetadata> getIpSpaceMetadata() {
    return _ipSpaceMetadata;
  }

  @JsonProperty(PROP_IPSEC_PHASE2_POLICIES)
  @JsonPropertyDescription("Dictionary of all IPSec phase 2 policies for this node.")
  public NavigableMap<String, IpsecPhase2Policy> getIpsecPhase2Policies() {
    return _ipsecPhase2Policies;
  }

  @JsonProperty(PROP_IPSEC_PHASE2_PROPOSALS)
  @JsonPropertyDescription("Dictionary of all IPSec phase 2 proposals for this node.")
  public NavigableMap<String, IpsecPhase2Proposal> getIpsecPhase2Proposals() {
    return _ipsecPhase2Proposals;
  }

  @JsonProperty(PROP_LOGGING_SERVERS)
  public NavigableSet<String> getLoggingServers() {
    return _loggingServers;
  }

  @JsonProperty(PROP_LOGGING_SOURCE_INTERFACE)
  public String getLoggingSourceInterface() {
    return _loggingSourceInterface;
  }

  @JsonProperty(PROP_MLAGS)
  @Nonnull
  public NavigableMap<String, Mlag> getMlags() {
    return _mlags;
  }

  @JsonIgnore
  public SubRange getNormalVlanRange() {
    return _normalVlanRange;
  }

  @JsonProperty(PROP_NTP_SERVERS)
  public NavigableSet<String> getNtpServers() {
    return _ntpServers;
  }

  @JsonProperty(PROP_NTP_SOURCE_INTERFACE)
  public String getNtpSourceInterface() {
    return _ntpSourceInterface;
  }

  /** Return the defined policies that can be used for policy-based routing */
  @JsonProperty(PROP_PACKET_POLICIES)
  public Map<String, PacketPolicy> getPacketPolicies() {
    return _packetPolicies;
  }

  @JsonPropertyDescription("Dictionary of all IPV6 route filter lists for this node.")
  @JsonProperty(PROP_ROUTE6_FILTER_LISTS)
  public NavigableMap<String, Route6FilterList> getRoute6FilterLists() {
    return _route6FilterLists;
  }

  @JsonProperty(PROP_ROUTE_FILTER_LISTS)
  @JsonPropertyDescription("Dictionary of all IPV4 route filter lists for this node.")
  public NavigableMap<String, RouteFilterList> getRouteFilterLists() {
    return _routeFilterLists;
  }

  @JsonProperty(PROP_ROUTING_POLICIES)
  @JsonPropertyDescription("Dictionary of all routing policies for this node.")
  public NavigableMap<String, RoutingPolicy> getRoutingPolicies() {
    return _routingPolicies;
  }

  private SortedSet<String> getRoutingPolicySources(@Nullable String routingPolicyName) {
    if (routingPolicyName == null) {
      return Collections.emptySortedSet();
    }
    RoutingPolicy rp = _routingPolicies.get(routingPolicyName);
    if (rp == null) {
      return Collections.emptySortedSet();
    }
    return rp.getSources().stream()
        .filter(not(RoutingPolicy::isGenerated))
        .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
  }

  @JsonProperty(PROP_SNMP_SOURCE_INTERFACE)
  public String getSnmpSourceInterface() {
    return _snmpSourceInterface;
  }

  @JsonProperty(PROP_SNMP_TRAP_SERVERS)
  public NavigableSet<String> getSnmpTrapServers() {
    return _snmpTrapServers;
  }

  @JsonProperty(PROP_TACACS_SERVERS)
  public NavigableSet<String> getTacacsServers() {
    return _tacacsServers;
  }

  @JsonProperty(PROP_TACACS_SOURCE_INTERFACE)
  public String getTacacsSourceInterface() {
    return _tacacsSourceInterface;
  }

  /** Mapping: trackingGroupID -&gt; trackMethod */
  @JsonProperty(PROP_TRACKING_GROUPS)
  public @Nonnull NavigableMap<String, TrackMethod> getTrackingGroups() {
    return _trackingGroups;
  }

  @JsonPropertyDescription("Object containing vendor-specific information for this node.")
  @JsonProperty(PROP_VENDOR_FAMILY)
  public VendorFamily getVendorFamily() {
    return _vendorFamily;
  }

  @JsonPropertyDescription("Dictionary of all VRFs for this node.")
  @JsonProperty(PROP_VRFS)
  public Map<String, Vrf> getVrfs() {
    return _vrfs;
  }

  @JsonProperty(PROP_ZONES)
  @JsonPropertyDescription("Dictionary of all firewall zones for this node.")
  public NavigableMap<String, Zone> getZones() {
    return _zones;
  }

  public void initBgpAdvertisements() {
    _bgpAdvertisements = new TreeSet<>();
    for (Vrf vrf : _vrfs.values()) {
      vrf.initBgpAdvertisements();
    }
  }

  @JsonProperty(PROP_AS_PATH_ACCESS_LISTS)
  public void setAsPathAccessLists(NavigableMap<String, AsPathAccessList> asPathAccessLists) {
    _asPathAccessLists = asPathAccessLists;
  }

  @JsonProperty(PROP_AUTHENTICATION_KEY_CHAINS)
  public void setAuthenticationKeyChains(
      NavigableMap<String, AuthenticationKeyChain> authenticationKeyChains) {
    _authenticationKeyChains = authenticationKeyChains;
  }

  @JsonProperty(PROP_COMMUNITY_LISTS)
  public void setCommunityLists(NavigableMap<String, CommunityList> communityLists) {
    _communityLists = communityLists;
  }

  public void setDefaultCrossZoneAction(LineAction defaultCrossZoneAction) {
    _defaultCrossZoneAction = defaultCrossZoneAction;
  }

  public void setDefaultInboundAction(LineAction defaultInboundAction) {
    _defaultInboundAction = defaultInboundAction;
  }

  @JsonProperty(PROP_DEVICE_TYPE)
  public void setDeviceType(DeviceType deviceType) {
    _deviceType = deviceType;
  }

  public void setDnsServers(NavigableSet<String> dnsServers) {
    _dnsServers = dnsServers;
  }

  @JsonProperty(PROP_DNS_SOURCE_INTERFACE)
  public void setDnsSourceInterface(String dnsSourceInterface) {
    _dnsSourceInterface = dnsSourceInterface;
  }

  @JsonProperty(PROP_DOMAIN_NAME)
  public void setDomainName(String domainName) {
    _domainName = domainName;
  }

  @JsonProperty(PROP_IKE_PHASE1_KEYS)
  public void setIkePhase1Keys(@Nullable NavigableMap<String, IkePhase1Key> ikePhase1Keys) {
    _ikePhase1keys =
        ikePhase1Keys == null ? ImmutableSortedMap.of() : ImmutableSortedMap.copyOf(ikePhase1Keys);
  }

  @JsonProperty(PROP_IKE_PHASE1_POLICIES)
  public void setIkePhase1Policies(NavigableMap<String, IkePhase1Policy> ikePhase1Policies) {
    _ikePhase1Policies = ikePhase1Policies;
  }

  @JsonProperty(PROP_IKE_PHASE1_PROPOSALS)
  public void setIkePhase1Proposals(NavigableMap<String, IkePhase1Proposal> ikePhase1Proposals) {
    _ikePhase1Proposals = ikePhase1Proposals;
  }

  @JsonProperty(PROP_INTERFACES)
  public void setInterfaces(NavigableMap<String, Interface> interfaces) {
    _interfaces = interfaces;
  }

  @JsonProperty(PROP_IP6_ACCESS_LISTS)
  public void setIp6AccessLists(NavigableMap<String, Ip6AccessList> ip6AccessLists) {
    _ip6AccessLists = ip6AccessLists;
  }

  @JsonProperty(PROP_IP_ACCESS_LISTS)
  public void setIpAccessLists(NavigableMap<String, IpAccessList> ipAccessLists) {
    _ipAccessLists = ipAccessLists;
  }

  @JsonProperty(PROP_IP_SPACES)
  public void setIpSpaces(NavigableMap<String, IpSpace> ipSpaces) {
    _ipSpaces = ipSpaces;
  }

  @JsonProperty(PROP_IPSEC_PEER_CONFIGS)
  public void setIpsecPeerConfigs(
      @Nullable NavigableMap<String, IpsecPeerConfig> ipsecPeerConfigs) {
    _ipsecPeerConfigs =
        ipsecPeerConfigs == null
            ? ImmutableSortedMap.of()
            : ImmutableSortedMap.copyOf(ipsecPeerConfigs);
  }

  @JsonProperty(PROP_IPSEC_PHASE2_POLICIES)
  public void setIpsecPhase2Policies(
      @Nullable NavigableMap<String, IpsecPhase2Policy> ipsecPhase2Policies) {
    _ipsecPhase2Policies =
        ipsecPhase2Policies == null
            ? ImmutableSortedMap.of()
            : ImmutableSortedMap.copyOf(ipsecPhase2Policies);
  }

  @JsonProperty(PROP_IPSEC_PHASE2_PROPOSALS)
  public void setIpsecPhase2Proposals(
      @Nullable NavigableMap<String, IpsecPhase2Proposal> ipsecPhase2Proposals) {
    _ipsecPhase2Proposals =
        ipsecPhase2Proposals == null
            ? ImmutableSortedMap.of()
            : ImmutableSortedMap.copyOf(ipsecPhase2Proposals);
  }

  @JsonProperty(PROP_LOGGING_SERVERS)
  public void setLoggingServers(NavigableSet<String> loggingServers) {
    _loggingServers = loggingServers;
  }

  @JsonProperty(PROP_LOGGING_SOURCE_INTERFACE)
  public void setLoggingSourceInterface(String loggingSourceInterface) {
    _loggingSourceInterface = loggingSourceInterface;
  }

  @JsonProperty(PROP_MLAGS)
  public void setMlags(Map<String, Mlag> mlags) {
    _mlags = ImmutableSortedMap.copyOf(mlags);
  }

  @JsonIgnore
  public void setNormalVlanRange(SubRange normalVlanRange) {
    _normalVlanRange = normalVlanRange;
  }

  @JsonProperty(PROP_NTP_SERVERS)
  public void setNtpServers(NavigableSet<String> ntpServers) {
    _ntpServers = ntpServers;
  }

  @JsonProperty(PROP_NTP_SOURCE_INTERFACE)
  public void setNtpSourceInterface(String ntpSourceInterface) {
    _ntpSourceInterface = ntpSourceInterface;
  }

  @JsonProperty(PROP_ROUTE6_FILTER_LISTS)
  public void setRoute6FilterLists(NavigableMap<String, Route6FilterList> route6FilterLists) {
    _route6FilterLists = route6FilterLists;
  }

  @JsonProperty(PROP_ROUTE_FILTER_LISTS)
  public void setRouteFilterLists(NavigableMap<String, RouteFilterList> routeFilterLists) {
    _routeFilterLists = routeFilterLists;
  }

  @JsonProperty(PROP_ROUTING_POLICIES)
  public void setRoutingPolicies(NavigableMap<String, RoutingPolicy> routingPolicies) {
    _routingPolicies = routingPolicies;
  }

  @JsonProperty(PROP_PACKET_POLICIES)
  public void setPacketPolicies(NavigableMap<String, PacketPolicy> packetPolicies) {
    _packetPolicies = packetPolicies;
  }

  @JsonProperty(PROP_SNMP_SOURCE_INTERFACE)
  public void setSnmpSourceInterface(String snmpSourceInterface) {
    _snmpSourceInterface = snmpSourceInterface;
  }

  @JsonProperty(PROP_SNMP_TRAP_SERVERS)
  public void setSnmpTrapServers(NavigableSet<String> snmpTrapServers) {
    _snmpTrapServers = snmpTrapServers;
  }

  @JsonProperty(PROP_TACACS_SERVERS)
  public void setTacacsServers(NavigableSet<String> tacacsServers) {
    _tacacsServers = tacacsServers;
  }

  @JsonProperty(PROP_TACACS_SOURCE_INTERFACE)
  public void setTacacsSourceInterface(String tacacsSourceInterface) {
    _tacacsSourceInterface = tacacsSourceInterface;
  }

  @JsonProperty(PROP_TRACKING_GROUPS)
  public void setTrackingGroups(@Nonnull NavigableMap<String, TrackMethod> trackingGroups) {
    _trackingGroups = trackingGroups;
  }

  @JsonProperty(PROP_VENDOR_FAMILY)
  public void setVendorFamily(VendorFamily vendorFamily) {
    _vendorFamily = vendorFamily;
  }

  @JsonProperty(PROP_VRFS)
  public void setVrfs(Map<String, Vrf> vrfs) {
    _vrfs = vrfs;
  }

  @JsonProperty(PROP_ZONES)
  public void setZones(NavigableMap<String, Zone> zones) {
    _zones = zones;
  }

  public void simplifyRoutingPolicies() {
    NavigableMap<String, RoutingPolicy> simpleRoutingPolicies =
        new TreeMap<>(
            _routingPolicies.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().simplify())));
    _routingPolicies = simpleRoutingPolicies;
  }

  @Override
  public String toString() {
    return _name;
  }
}

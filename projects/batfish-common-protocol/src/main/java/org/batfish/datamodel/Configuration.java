package org.batfish.datamodel;

import static com.google.common.base.Predicates.not;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;
import org.batfish.datamodel.bgp.AddressFamily;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.communities.CommunityMatchExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySet;
import org.batfish.datamodel.routing_policy.communities.CommunitySetExpr;
import org.batfish.datamodel.routing_policy.communities.CommunitySetMatchExpr;
import org.batfish.datamodel.tracking.TrackMethod;
import org.batfish.datamodel.vendor_family.VendorFamily;
import org.batfish.referencelibrary.ReferenceBook;

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
      String name = _hostname != null ? _hostname : generateName().toLowerCase();
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

    public Builder setHostname(@Nullable String hostname) {
      if (hostname != null) {
        _hostname = hostname.toLowerCase();
      } else {
        _hostname = null;
      }
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

  private static final String PROP_AS_PATH_ACCESS_LISTS = "asPathAccessLists";
  private static final String PROP_AUTHENTICATION_KEY_CHAINS = "authenticationKeyChains";
  private static final String PROP_COMMUNITY_LISTS = "communityLists";
  private static final String PROP_COMMUNITY_MATCH_EXPRS = "communityMatchExprs";
  private static final String PROP_COMMUNITY_SET_EXPRS = "communitySetExprs";
  private static final String PROP_COMMUNITY_SET_MATCH_EXPRS = "communitySetMatchExprs";
  private static final String PROP_COMMUNITY_SETS = "communitySets";
  private static final String PROP_CONFIGURATION_FORMAT = "configurationFormat";
  private static final String PROP_DEFAULT_CROSS_ZONE_ACTION = "defaultCrossZoneAction";
  private static final String PROP_DEFAULT_INBOUND_ACTION = "defaultInboundAction";
  private static final String PROP_DEVICE_TYPE = "deviceType";
  private static final String PROP_DNS_SOURCE_INTERFACE = "dnsSourceInterface";
  private static final String PROP_DOMAIN_NAME = "domainName";
  private static final String PROP_GENERATED_REFERENCE_BOOKS = "generatedReferenceBooks";
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

  private static final int VLAN_NORMAL_MAX_DEFAULT = 4094;

  private static final int VLAN_NORMAL_MIN_DEFAULT = 1;

  private Map<String, AsPathAccessList> _asPathAccessLists;

  private NavigableMap<String, AuthenticationKeyChain> _authenticationKeyChains;

  private Map<String, CommunityList> _communityLists;

  private Map<String, CommunityMatchExpr> _communityMatchExprs;
  private Map<String, CommunitySetExpr> _communitySetExprs;
  private Map<String, CommunitySetMatchExpr> _communitySetMatchExprs;
  private Map<String, CommunitySet> _communitySets;

  private final ConfigurationFormat _configurationFormat;

  private LineAction _defaultCrossZoneAction;

  private LineAction _defaultInboundAction;

  private DeviceType _deviceType;

  private NavigableSet<String> _dnsServers;

  private String _dnsSourceInterface;

  private String _domainName;

  private NavigableMap<String, ReferenceBook> _generatedReferenceBooks;

  private @Nonnull NavigableMap<String, IkePhase1Key> _ikePhase1keys;

  private NavigableMap<String, IkePhase1Proposal> _ikePhase1Proposals;

  private NavigableMap<String, IkePhase1Policy> _ikePhase1Policies;

  private Map<String, Interface> _interfaces;

  private Map<String, Ip6AccessList> _ip6AccessLists;

  private Map<String, IpAccessList> _ipAccessLists;

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

  private Map<String, Route6FilterList> _route6FilterLists;

  private Map<String, RouteFilterList> _routeFilterLists;

  private Map<String, RoutingPolicy> _routingPolicies;

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
    _communityMatchExprs = new HashMap<>();
    _communitySetExprs = new HashMap<>();
    _communitySetMatchExprs = new HashMap<>();
    _communitySets = new HashMap<>();
    _configurationFormat = configurationFormat;
    _dnsServers = new TreeSet<>();
    _domainName = null;
    _generatedReferenceBooks = new TreeMap<>();
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
        for (BgpPeerConfig neighbor : bgpProcess.getAllPeerConfigs()) {
          for (AddressFamily af : neighbor.getAllAddressFamilies()) {
            af.setExportPolicySources(getRoutingPolicySources(af.getExportPolicy()));
            af.setImportPolicySources(getRoutingPolicySources(af.getImportPolicy()));
          }
        }
      }
      for (OspfProcess ospfProcess : vrf.getOspfProcesses().values()) {
        ospfProcess.setExportPolicySources(getRoutingPolicySources(ospfProcess.getExportPolicy()));
      }
      for (GeneratedRoute gr : vrf.getGeneratedRoutes()) {
        gr.setAttributePolicySources(getRoutingPolicySources(gr.getAttributePolicy()));
        gr.setGenerationPolicySources(getRoutingPolicySources(gr.getGenerationPolicy()));
      }
    }
  }

  /** Dictionary of all AS-path access-lists for this node. */
  @JsonProperty(PROP_AS_PATH_ACCESS_LISTS)
  public Map<String, AsPathAccessList> getAsPathAccessLists() {
    return _asPathAccessLists;
  }

  /** Dictionary of all authentication key chains for this node. */
  @JsonProperty(PROP_AUTHENTICATION_KEY_CHAINS)
  public NavigableMap<String, AuthenticationKeyChain> getAuthenticationKeyChains() {
    return _authenticationKeyChains;
  }

  public Set<String> activeInterfaceNames() {
    return activeInterfaces().map(Interface::getName).collect(ImmutableSet.toImmutableSet());
  }

  public @Nonnull Stream<Interface> activeInterfaces() {
    return _interfaces.values().stream().filter(Interface::getActive);
  }

  /** Dictionary of all community-lists for this node. */
  @JsonProperty(PROP_COMMUNITY_LISTS)
  public Map<String, CommunityList> getCommunityLists() {
    return _communityLists;
  }

  @JsonIgnore
  public @Nonnull Map<String, CommunityMatchExpr> getCommunityMatchExprs() {
    return _communityMatchExprs;
  }

  @JsonProperty(PROP_COMMUNITY_MATCH_EXPRS)
  private @Nonnull NavigableMap<String, CommunityMatchExpr> getCommunityMatchExprsSorted() {
    return ImmutableSortedMap.copyOf(_communityMatchExprs);
  }

  @JsonIgnore
  public @Nonnull Map<String, CommunitySetExpr> getCommunitySetExprs() {
    return _communitySetExprs;
  }

  @JsonProperty(PROP_COMMUNITY_SET_EXPRS)
  private @Nonnull NavigableMap<String, CommunitySetExpr> getCommunitySetExprsSorted() {
    return ImmutableSortedMap.copyOf(_communitySetExprs);
  }

  @JsonIgnore
  public @Nonnull Map<String, CommunitySetMatchExpr> getCommunitySetMatchExprs() {
    return _communitySetMatchExprs;
  }

  @JsonProperty(PROP_COMMUNITY_SET_MATCH_EXPRS)
  private @Nonnull NavigableMap<String, CommunitySetMatchExpr> getCommunitySetMatchExprsSorted() {
    return ImmutableSortedMap.copyOf(_communitySetMatchExprs);
  }

  @JsonIgnore
  public @Nonnull Map<String, CommunitySet> getCommunitySets() {
    return _communitySets;
  }

  @JsonProperty(PROP_COMMUNITY_SETS)
  private @Nonnull NavigableMap<String, CommunitySet> getCommunitySetsSorted() {
    return ImmutableSortedMap.copyOf(_communitySets);
  }

  /**
   * Best guess at vendor configuration format. Used for setting default values, protocol costs,
   * etc.
   */
  @JsonProperty(PROP_CONFIGURATION_FORMAT)
  public ConfigurationFormat getConfigurationFormat() {
    return _configurationFormat;
  }

  /** Default forwarding action to take for traffic that crosses firewall zones. */
  @JsonProperty(PROP_DEFAULT_CROSS_ZONE_ACTION)
  public LineAction getDefaultCrossZoneAction() {
    return _defaultCrossZoneAction;
  }

  /** Default forwarding action to take for traffic destined for this device. */
  @JsonProperty(PROP_DEFAULT_INBOUND_ACTION)
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

  /** Domain name of this node. */
  @JsonProperty(PROP_DOMAIN_NAME)
  public String getDomainName() {
    return _domainName;
  }

  /** Dictionary of Reference Books generated from device configurations (e.g., F5 Pools). */
  @JsonProperty(PROP_GENERATED_REFERENCE_BOOKS)
  public NavigableMap<String, ReferenceBook> getGeneratedReferenceBooks() {
    return _generatedReferenceBooks;
  }

  /** Hostname of this node. */
  @JsonProperty(PROP_NAME)
  public String getHostname() {
    return _name;
  }

  /** Dictionary of all IKE phase1 keys for this node. */
  @JsonProperty(PROP_IKE_PHASE1_KEYS)
  public NavigableMap<String, IkePhase1Key> getIkePhase1Keys() {
    return _ikePhase1keys;
  }

  /** Dictionary of all IKE phase1 policies for this node. */
  @JsonProperty(PROP_IKE_PHASE1_POLICIES)
  public NavigableMap<String, IkePhase1Policy> getIkePhase1Policies() {
    return _ikePhase1Policies;
  }

  /** Dictionary of all IKE phase1 proposals for this node. */
  @JsonProperty(PROP_IKE_PHASE1_PROPOSALS)
  public NavigableMap<String, IkePhase1Proposal> getIkePhase1Proposals() {
    return _ikePhase1Proposals;
  }

  /** Dictionary of all interfaces across all VRFs for this node. */
  @JsonProperty(PROP_INTERFACES)
  public Map<String, Interface> getAllInterfaces() {
    return _interfaces;
  }

  @JsonIgnore
  public Map<String, Interface> getActiveInterfaces() {
    return _interfaces.entrySet().stream()
        .filter(e -> e.getValue().getActive())
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  /** Dictionary of all IPV6 access-lists for this node. */
  @JsonProperty(PROP_IP6_ACCESS_LISTS)
  public Map<String, Ip6AccessList> getIp6AccessLists() {
    return _ip6AccessLists;
  }

  /** Dictionary of all IPV4 access-lists for this node. */
  @JsonProperty(PROP_IP_ACCESS_LISTS)
  public Map<String, IpAccessList> getIpAccessLists() {
    return _ipAccessLists;
  }

  /** Dictionary of all IPSec peer configs for this node. */
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

  /** Dictionary of all IPSec phase 2 policies for this node. */
  @JsonProperty(PROP_IPSEC_PHASE2_POLICIES)
  public NavigableMap<String, IpsecPhase2Policy> getIpsecPhase2Policies() {
    return _ipsecPhase2Policies;
  }

  /** Dictionary of all IPSec phase 2 proposals for this node. */
  @JsonProperty(PROP_IPSEC_PHASE2_PROPOSALS)
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

  /** Dictionary of all IPV6 route filter lists for this node. */
  @JsonProperty(PROP_ROUTE6_FILTER_LISTS)
  public Map<String, Route6FilterList> getRoute6FilterLists() {
    return _route6FilterLists;
  }

  /** Dictionary of all IPV4 route filter lists for this node. */
  @JsonProperty(PROP_ROUTE_FILTER_LISTS)
  public Map<String, RouteFilterList> getRouteFilterLists() {
    return _routeFilterLists;
  }

  /** Dictionary of all routing policies for this node. */
  @JsonProperty(PROP_ROUTING_POLICIES)
  public Map<String, RoutingPolicy> getRoutingPolicies() {
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

  /** Object containing vendor-specific information for this node. */
  @JsonProperty(PROP_VENDOR_FAMILY)
  public VendorFamily getVendorFamily() {
    return _vendorFamily;
  }

  /** Dictionary of all VRFs for this node. */
  @JsonProperty(PROP_VRFS)
  public Map<String, Vrf> getVrfs() {
    return _vrfs;
  }

  /** Dictionary of all firewall zones for this node. */
  @JsonProperty(PROP_ZONES)
  public NavigableMap<String, Zone> getZones() {
    return _zones;
  }

  @JsonProperty(PROP_AS_PATH_ACCESS_LISTS)
  public void setAsPathAccessLists(Map<String, AsPathAccessList> asPathAccessLists) {
    _asPathAccessLists = asPathAccessLists;
  }

  @JsonProperty(PROP_AUTHENTICATION_KEY_CHAINS)
  public void setAuthenticationKeyChains(
      NavigableMap<String, AuthenticationKeyChain> authenticationKeyChains) {
    _authenticationKeyChains = authenticationKeyChains;
  }

  @JsonProperty(PROP_COMMUNITY_LISTS)
  public void setCommunityLists(Map<String, CommunityList> communityLists) {
    _communityLists = communityLists;
  }

  @JsonProperty(PROP_COMMUNITY_MATCH_EXPRS)
  public void setCommunityMatchExprs(@Nonnull Map<String, CommunityMatchExpr> communityMatchExprs) {
    _communityMatchExprs = communityMatchExprs;
  }

  @JsonProperty(PROP_COMMUNITY_SET_EXPRS)
  public void setCommunitySetExprs(@Nonnull Map<String, CommunitySetExpr> communitySetExprs) {
    _communitySetExprs = communitySetExprs;
  }

  @JsonProperty(PROP_COMMUNITY_SET_MATCH_EXPRS)
  public void setCommunitySetMatchExprs(
      @Nonnull Map<String, CommunitySetMatchExpr> communitySetMatchExprs) {
    _communitySetMatchExprs = communitySetMatchExprs;
  }

  @JsonProperty(PROP_COMMUNITY_SETS)
  public void setCommunitySets(@Nonnull Map<String, CommunitySet> communitySets) {
    _communitySets = communitySets;
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
  public void setInterfaces(Map<String, Interface> interfaces) {
    _interfaces = interfaces;
  }

  @JsonProperty(PROP_IP6_ACCESS_LISTS)
  public void setIp6AccessLists(Map<String, Ip6AccessList> ip6AccessLists) {
    _ip6AccessLists = ip6AccessLists;
  }

  @JsonProperty(PROP_IP_ACCESS_LISTS)
  public void setIpAccessLists(Map<String, IpAccessList> ipAccessLists) {
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
  public void setRoute6FilterLists(Map<String, Route6FilterList> route6FilterLists) {
    _route6FilterLists = route6FilterLists;
  }

  @JsonProperty(PROP_ROUTE_FILTER_LISTS)
  public void setRouteFilterLists(Map<String, RouteFilterList> routeFilterLists) {
    _routeFilterLists = routeFilterLists;
  }

  @JsonProperty(PROP_ROUTING_POLICIES)
  public void setRoutingPolicies(Map<String, RoutingPolicy> routingPolicies) {
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
        _routingPolicies.entrySet().stream()
            .collect(
                ImmutableSortedMap.toImmutableSortedMap(
                    Ordering.natural(), Entry::getKey, e -> e.getValue().simplify()));
    _routingPolicies = simpleRoutingPolicies;
  }

  @Override
  public String toString() {
    return _name;
  }
}

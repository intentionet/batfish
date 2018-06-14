package org.batfish.representation.cisco;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Prefix6;
import org.batfish.datamodel.RoutingProtocol;

public class BgpProcess extends ComparableStructure<Integer> {

  private static final int DEFAULT_BGP_DEFAULT_METRIC = 0;

  private static final long serialVersionUID = 1L;

  private Map<String, NamedBgpPeerGroup> _afGroups;

  private Map<Prefix6, BgpAggregateIpv6Network> _aggregateIpv6Networks;

  private Map<Prefix, BgpAggregateIpv4Network> _aggregateNetworks;

  private Set<BgpPeerGroup> _allPeerGroups;

  private boolean _alwaysCompareMed;

  private boolean _asPathMultipathRelax;

  private Ip _clusterId;

  private boolean _defaultIpv4Activate;

  private boolean _defaultIpv6Activate;

  private Map<Prefix, DynamicIpBgpPeerGroup> _dynamicIpPeerGroups;

  private Map<Prefix6, DynamicIpv6BgpPeerGroup> _dynamicIpv6PeerGroups;

  private Map<Prefix, BgpNetwork> _ipNetworks;

  private Map<Ip, IpBgpPeerGroup> _ipPeerGroups;

  private Map<Prefix6, BgpNetwork6> _ipv6Networks;

  private Map<Ip6, Ipv6BgpPeerGroup> _ipv6PeerGroups;

  private MasterBgpPeerGroup _masterBgpPeerGroup;

  private Integer _maximumPaths;

  private Integer _maximumPathsEbgp;

  private Integer _maximumPathsEibgp;

  private Integer _maximumPathsIbgp;

  private Map<String, NamedBgpPeerGroup> _namedPeerGroups;

  private Map<String, NamedBgpPeerGroup> _peerSessions;

  private final Map<RoutingProtocol, BgpRedistributionPolicy> _redistributionPolicies;

  private Ip _routerId;

  private BgpTieBreaker _tieBreaker;

  public BgpProcess(ConfigurationFormat format, int procnum) {
    super(procnum);
    _afGroups = new HashMap<>();
    _aggregateNetworks = new HashMap<>();
    _aggregateIpv6Networks = new HashMap<>();
    _allPeerGroups = new HashSet<>();
    _defaultIpv4Activate = true;
    _dynamicIpPeerGroups = new HashMap<>();
    _dynamicIpv6PeerGroups = new HashMap<>();
    _namedPeerGroups = new HashMap<>();
    _ipNetworks = new LinkedHashMap<>();
    _ipPeerGroups = new HashMap<>();
    _ipv6Networks = new LinkedHashMap<>();
    _ipv6PeerGroups = new HashMap<>();
    _peerSessions = new HashMap<>();
    _redistributionPolicies = new EnumMap<>(RoutingProtocol.class);
    _masterBgpPeerGroup = new MasterBgpPeerGroup();
    if (format == ConfigurationFormat.ARISTA) {
      _asPathMultipathRelax = true;
    }
    switch (format) {
      case CISCO_IOS:
      case CISCO_IOS_XR:
      case CISCO_NX:
        _masterBgpPeerGroup.setAdvertiseInactive(true);
        break;

        // $CASES-OMITTED$
      default:
        break;
    }
    _masterBgpPeerGroup.setDefaultMetric(DEFAULT_BGP_DEFAULT_METRIC);
  }

  public DynamicIpBgpPeerGroup addDynamicIpPeerGroup(Prefix prefix) {
    DynamicIpBgpPeerGroup pg = new DynamicIpBgpPeerGroup(prefix);
    if (_defaultIpv4Activate) {
      pg.setActive(true);
    }
    _dynamicIpPeerGroups.put(prefix, pg);
    _allPeerGroups.add(pg);
    return pg;
  }

  public DynamicIpv6BgpPeerGroup addDynamicIpv6PeerGroup(Prefix6 prefix6) {
    DynamicIpv6BgpPeerGroup pg = new DynamicIpv6BgpPeerGroup(prefix6);
    if (_defaultIpv6Activate) {
      pg.setActive(true);
    }
    _dynamicIpv6PeerGroups.put(prefix6, pg);
    _allPeerGroups.add(pg);
    return pg;
  }

  public void addIpPeerGroup(Ip ip) {
    IpBgpPeerGroup pg = new IpBgpPeerGroup(ip);
    if (_defaultIpv4Activate) {
      pg.setActive(true);
    }
    _ipPeerGroups.put(ip, pg);
    _allPeerGroups.add(pg);
  }

  public void addIpv6PeerGroup(Ip6 ip6) {
    Ipv6BgpPeerGroup pg = new Ipv6BgpPeerGroup(ip6);
    if (_defaultIpv6Activate) {
      pg.setActive(true);
    }
    _ipv6PeerGroups.put(ip6, pg);
    _allPeerGroups.add(pg);
  }

  public void addNamedPeerGroup(String name, int definitionLine) {
    NamedBgpPeerGroup pg = new NamedBgpPeerGroup(name, definitionLine);
    _namedPeerGroups.put(name, pg);
    _allPeerGroups.add(pg);
  }

  public void addPeerSession(String name, int definitionLine) {
    NamedBgpPeerGroup pg = new NamedBgpPeerGroup(name, definitionLine);
    _peerSessions.put(name, pg);
    _allPeerGroups.add(pg);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BgpProcess)) {
      return false;
    }
    BgpProcess other = (BgpProcess) o;
    // TODO: compare all fields
    return _name.equals(other._name);
  }

  public Map<String, NamedBgpPeerGroup> getAfGroups() {
    return _afGroups;
  }

  public Map<Prefix6, BgpAggregateIpv6Network> getAggregateIpv6Networks() {
    return _aggregateIpv6Networks;
  }

  public Map<Prefix, BgpAggregateIpv4Network> getAggregateNetworks() {
    return _aggregateNetworks;
  }

  public Set<BgpPeerGroup> getAllPeerGroups() {
    return _allPeerGroups;
  }

  public boolean getAlwaysCompareMed() {
    return _alwaysCompareMed;
  }

  public boolean getAsPathMultipathRelax() {
    return _asPathMultipathRelax;
  }

  public Ip getClusterId() {
    return _clusterId;
  }

  public int getDefaultMetric() {
    return _masterBgpPeerGroup.getDefaultMetric();
  }

  public Map<Prefix, DynamicIpBgpPeerGroup> getDynamicIpPeerGroups() {
    return _dynamicIpPeerGroups;
  }

  public Map<Prefix6, DynamicIpv6BgpPeerGroup> getDynamicIpv6PeerGroups() {
    return _dynamicIpv6PeerGroups;
  }

  public Map<Prefix, BgpNetwork> getIpNetworks() {
    return _ipNetworks;
  }

  public Map<Ip, IpBgpPeerGroup> getIpPeerGroups() {
    return _ipPeerGroups;
  }

  public Map<Prefix6, BgpNetwork6> getIpv6Networks() {
    return _ipv6Networks;
  }

  public Map<Ip6, Ipv6BgpPeerGroup> getIpv6PeerGroups() {
    return _ipv6PeerGroups;
  }

  public MasterBgpPeerGroup getMasterBgpPeerGroup() {
    return _masterBgpPeerGroup;
  }

  public Integer getMaximumPaths() {
    return _maximumPaths;
  }

  public Integer getMaximumPathsEbgp() {
    return _maximumPathsEbgp;
  }

  public Integer getMaximumPathsEibgp() {
    return _maximumPathsEibgp;
  }

  public Integer getMaximumPathsIbgp() {
    return _maximumPathsIbgp;
  }

  public Map<String, NamedBgpPeerGroup> getNamedPeerGroups() {
    return _namedPeerGroups;
  }

  public Map<String, NamedBgpPeerGroup> getPeerSessions() {
    return _peerSessions;
  }

  public Map<RoutingProtocol, BgpRedistributionPolicy> getRedistributionPolicies() {
    return _redistributionPolicies;
  }

  public Ip getRouterId() {
    return _routerId;
  }

  public BgpTieBreaker getTieBreaker() {
    return _tieBreaker;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_name);
  }

  public void setAlwaysCompareMed(boolean b) {
    _alwaysCompareMed = b;
  }

  public void setAsPathMultipathRelax(boolean asPathMultipathRelax) {
    _asPathMultipathRelax = asPathMultipathRelax;
  }

  public void setClusterId(Ip clusterId) {
    _clusterId = clusterId;
  }

  public void setMaximumPaths(Integer maximumPaths) {
    _maximumPaths = maximumPaths;
  }

  public void setMaximumPathsEbgp(Integer maximumPathsEbgp) {
    _maximumPathsEbgp = maximumPathsEbgp;
  }

  public void setMaximumPathsEibgp(Integer maximumPathsEibgp) {
    _maximumPathsEibgp = maximumPathsEibgp;
  }

  public void setMaximumPathsIbgp(Integer maximumPathsIbgp) {
    _maximumPathsIbgp = maximumPathsIbgp;
  }

  public void setRouterId(Ip routerId) {
    _routerId = routerId;
  }

  public void setTieBreaker(BgpTieBreaker tieBreaker) {
    _tieBreaker = tieBreaker;
  }
}

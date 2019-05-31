package org.batfish.representation.palo_alto;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.datamodel.Names.zoneToZoneFilter;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_OBJECT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.Streams;
import com.google.common.collect.TreeMultiset;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.collections4.list.TreeList;
import org.batfish.common.VendorConversionException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpRange;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceMetadata;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.NotMatchExpr;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.representation.palo_alto.Zone.Type;
import org.batfish.vendor.StructureUsage;
import org.batfish.vendor.VendorConfiguration;

public final class PaloAltoConfiguration extends VendorConfiguration {

  private static final long serialVersionUID = 1L;

  /** This is the name of an application that matches all traffic */
  public static final String CATCHALL_APPLICATION_NAME = "any";

  /** This is the name of an endpoint that matches all traffic */
  public static final String CATCHALL_ENDPOINT_NAME = "any";

  /** This is the name of a service that matches all traffic */
  public static final String CATCHALL_SERVICE_NAME = "any";

  /** This is the name of the zone that matches traffic in all zones (but not unzoned traffic) */
  public static final String CATCHALL_ZONE_NAME = "any";

  public static final String DEFAULT_VSYS_NAME = "vsys1";

  public static final String NULL_VRF_NAME = "~NULL_VRF~";

  public static final String PANORAMA_VSYS_NAME = "panorama";

  public static final String SHARED_VSYS_NAME = "~SHARED_VSYS~";

  private Configuration _c;

  private List<CryptoProfile> _cryptoProfiles;

  private String _dnsServerPrimary;

  private String _dnsServerSecondary;

  private String _hostname;

  private final SortedMap<String, Interface> _interfaces;

  private Ip _mgmtIfaceAddress;

  private Ip _mgmtIfaceGateway;

  private Ip _mgmtIfaceNetmask;

  private String _ntpServerPrimary;

  private String _ntpServerSecondary;

  private ConfigurationFormat _vendor;

  private final SortedMap<String, VirtualRouter> _virtualRouters;

  private final SortedMap<String, Vsys> _virtualSystems;

  public PaloAltoConfiguration() {
    _cryptoProfiles = new LinkedList<>();
    _interfaces = new TreeMap<>();
    _virtualRouters = new TreeMap<>();
    _virtualSystems = new TreeMap<>();
  }

  private NavigableSet<String> getDnsServers() {
    NavigableSet<String> servers = new TreeSet<>();
    if (_dnsServerPrimary != null) {
      servers.add(_dnsServerPrimary);
    }
    if (_dnsServerSecondary != null) {
      servers.add(_dnsServerSecondary);
    }
    return servers;
  }

  public List<CryptoProfile> getCryptoProfiles() {
    return _cryptoProfiles;
  }

  /** Gets the crypto profile by the provided name and type; creates anew if one does not exist */
  public CryptoProfile getCryptoProfileOrCreate(String name, CryptoProfile.Type cpType) {
    Optional<CryptoProfile> optCp =
        _cryptoProfiles.stream()
            .filter(p -> p.getName().equals(name) && p.getType() == cpType)
            .findAny();

    if (optCp.isPresent()) {
      return optCp.get();
    }

    CryptoProfile cp = new CryptoProfile(name, cpType);
    _cryptoProfiles.add(cp);
    return cp;
  }

  @Override
  public String getHostname() {
    return _hostname;
  }

  public SortedMap<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public Ip getMgmtIfaceAddress() {
    return _mgmtIfaceAddress;
  }

  public Ip getMgmtIfaceGateway() {
    return _mgmtIfaceGateway;
  }

  public Ip getMgmtIfaceNetmask() {
    return _mgmtIfaceNetmask;
  }

  private NavigableSet<String> getNtpServers() {
    NavigableSet<String> servers = new TreeSet<>();
    if (_ntpServerPrimary != null) {
      servers.add(_ntpServerPrimary);
    }
    if (_ntpServerSecondary != null) {
      servers.add(_ntpServerSecondary);
    }
    return servers;
  }

  public SortedMap<String, VirtualRouter> getVirtualRouters() {
    return _virtualRouters;
  }

  public SortedMap<String, Vsys> getVirtualSystems() {
    return _virtualSystems;
  }

  public void setDnsServerPrimary(String dnsServerPrimary) {
    _dnsServerPrimary = dnsServerPrimary;
  }

  public void setDnsServerSecondary(String dnsServerSecondary) {
    _dnsServerSecondary = dnsServerSecondary;
  }

  @Override
  public void setHostname(String hostname) {
    checkNotNull(hostname, "'hostname' cannot be null");
    _hostname = hostname.toLowerCase();
  }

  public void setMgmtIfaceAddress(Ip ip) {
    _mgmtIfaceAddress = ip;
  }

  public void setMgmtIfaceGateway(Ip ip) {
    _mgmtIfaceGateway = ip;
  }

  public void setMgmtIfaceNetmask(Ip ip) {
    _mgmtIfaceNetmask = ip;
  }

  public void setNtpServerPrimary(String ntpServerPrimary) {
    _ntpServerPrimary = ntpServerPrimary;
  }

  public void setNtpServerSecondary(String ntpServerSecondary) {
    _ntpServerSecondary = ntpServerSecondary;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    _vendor = format;
  }

  // Visible for testing

  /**
   * Generate unique object name (no collision across vsys namespaces) given a vsys name and
   * original object name.
   */
  public static String computeObjectName(String vsysName, String objectName) {
    return String.format("%s~%s", objectName, vsysName);
  }

  /** Generate egress IpAccessList name given an interface or zone name */
  public static String computeOutgoingFilterName(String interfaceOrZoneName) {
    return String.format("~%s~OUTGOING_FILTER~", interfaceOrZoneName);
  }

  /**
   * Extract object name from a name with an embedded namespace. For example: {@code
   * nameWithNamespace} might be `SERVICE1~vsys1`, where `SERVICE1` is the object name extracted and
   * returned.
   *
   * <p>Note that {@code nameWithNamespace} is expected to have the user object name
   * <strong>first</strong> to enable users to recognize their objects in Batfish output.
   */
  private static String extractObjectName(String nameWithNamespace) {
    String[] parts = nameWithNamespace.split("~", -1);
    return parts[0];
  }

  /**
   * Generate the {@link IpAccessList} name for the specified {@code serviceGroupMemberName} in the
   * specified {@code vsysName}.
   *
   * <p>Note that this is <strong>not</strong> a generated name, just a namespaced name.
   */
  @VisibleForTesting
  public static String computeServiceGroupMemberAclName(
      String vsysName, String serviceGroupMemberName) {
    return String.format("%s~%s~SERVICE_GROUP_MEMBER", serviceGroupMemberName, vsysName);
  }

  /** Convert vsys components to vendor independent model */
  private void convertVirtualSystems() {
    NavigableSet<String> loggingServers = new TreeSet<>();

    for (Vsys vsys : _virtualSystems.values()) {
      loggingServers.addAll(vsys.getSyslogServerAddresses());
      String vsysName = vsys.getName();

      // convert address objects and groups to ip spaces
      vsys.getAddressObjects()
          .forEach(
              (name, addressObject) -> {
                _c.getIpSpaces().put(name, addressObject.getIpSpace());
                _c.getIpSpaceMetadata()
                    .put(name, new IpSpaceMetadata(name, ADDRESS_OBJECT.getDescription()));
              });

      vsys.getAddressGroups()
          .forEach(
              (name, addressGroup) -> {
                _c.getIpSpaces()
                    .put(
                        name,
                        addressGroup.getIpSpace(vsys.getAddressObjects(), vsys.getAddressGroups()));
                _c.getIpSpaceMetadata()
                    .put(name, new IpSpaceMetadata(name, ADDRESS_GROUP.getDescription()));
              });

      List<Map.Entry<Rule, Vsys>> rules = getAllRules(vsys);
      // Convert PAN zones
      for (Entry<String, Zone> zoneEntry : vsys.getZones().entrySet()) {
        Zone zone = zoneEntry.getValue();
        org.batfish.datamodel.Zone newZone =
            toZone(computeObjectName(vsysName, zone.getName()), zone);
        _c.getZones().put(newZone.getName(), newZone);
      }

      // Create zone-specific outgoing ACLs.
      for (Zone zone : vsys.getZones().values()) {
        IpAccessList acl =
            generateOutgoingFilter(
                computeOutgoingFilterName(computeObjectName(vsysName, zone.getName())),
                zone,
                rules);
        _c.getIpAccessLists().put(acl.getName(), acl);
      }

      // Create cross-zone ACLs for each pair of zones, including self-zone.
      for (Zone fromZone : vsys.getZones().values()) {
        for (Zone toZone : vsys.getZones().values()) {
          if (fromZone.getType() == Type.EXTERNAL && toZone.getType() == Type.EXTERNAL) {
            // Don't add ACLs for zones when both are external.
            continue;
          }
          if (fromZone.getType() != Type.EXTERNAL
              && toZone.getType() != Type.EXTERNAL
              && fromZone.getType() != toZone.getType()) {
            // If one zone is not external, they have to match.
            continue;
          }
          IpAccessList acl = generateCrossZoneFilter(fromZone, toZone, rules);
          _c.getIpAccessLists().put(acl.getName(), acl);
        }
      }

      // Services
      for (Service service : vsys.getServices().values()) {
        IpAccessList acl = service.toIpAccessList(LineAction.PERMIT, this, vsys, _w);
        _c.getIpAccessLists().put(acl.getName(), acl);
      }

      // Service groups
      for (ServiceGroup serviceGroup : vsys.getServiceGroups().values()) {
        IpAccessList acl = serviceGroup.toIpAccessList(LineAction.PERMIT, this, vsys, _w);
        _c.getIpAccessLists().put(acl.getName(), acl);
      }
    }
    _c.setLoggingServers(loggingServers);
  }

  /** Generates a cross-zone ACL from the two given zones in the same Vsys using the given rules. */
  private IpAccessList generateCrossZoneFilter(
      Zone fromZone, Zone toZone, List<Map.Entry<Rule, Vsys>> rules) {
    assert fromZone.getVsys() == toZone.getVsys();

    String crossZoneFilterName =
        zoneToZoneFilter(
            computeObjectName(fromZone.getVsys().getName(), fromZone.getName()),
            computeObjectName(toZone.getVsys().getName(), toZone.getName()));

    if (fromZone.getInterfaceNames().isEmpty() || toZone.getInterfaceNames().isEmpty()) {
      return IpAccessList.builder()
          .setName(crossZoneFilterName)
          .setLines(
              ImmutableList.of(
                  IpAccessListLine.rejecting("No interfaces in zone", TrueExpr.INSTANCE)))
          .build();
    }

    // Build an ACL Line for each rule that is enabled and applies to this from/to zone pair.
    List<IpAccessListLine> lines =
        rules.stream()
            .filter(
                e -> {
                  Rule rule = e.getKey();
                  if (rule.getDisabled()) {
                    return false;
                  } else if (Sets.intersection(
                          rule.getFrom(), ImmutableSet.of(fromZone.getName(), CATCHALL_ZONE_NAME))
                      .isEmpty()) {
                    return false;
                  }
                  return !Sets.intersection(
                          rule.getTo(), ImmutableSet.of(toZone.getName(), CATCHALL_ZONE_NAME))
                      .isEmpty();
                })
            .map(entry -> toIpAccessListLine(entry.getKey(), entry.getValue()))
            .collect(ImmutableList.toImmutableList());
    // Intrazone traffic is allowed by default.
    if (fromZone == toZone) {
      lines =
          ImmutableList.<IpAccessListLine>builder()
              .addAll(lines)
              .add(IpAccessListLine.accepting("Accept intrazone by default", TrueExpr.INSTANCE))
              .build();
    }

    // Create a new ACL with a vsys-specific name.
    return IpAccessList.builder().setName(crossZoneFilterName).setLines(lines).build();
  }

  /** Collects the rules from this Vsys and merges the common pre-/post-rulebases from Panorama. */
  private List<Map.Entry<Rule, Vsys>> getAllRules(Vsys vsys) {
    @Nullable Vsys panorama = _virtualSystems.get(PANORAMA_VSYS_NAME);
    Stream<Map.Entry<Rule, Vsys>> pre =
        panorama == null
            ? Stream.of()
            : panorama.getPreRules().values().stream()
                .map(r -> new SimpleImmutableEntry<>(r, panorama));
    Stream<Map.Entry<Rule, Vsys>> post =
        panorama == null
            ? Stream.of()
            : panorama.getPostRules().values().stream()
                .map(r -> new SimpleImmutableEntry<>(r, panorama));
    Stream<Map.Entry<Rule, Vsys>> rules =
        vsys.getRules().values().stream().map(r -> new SimpleImmutableEntry<>(r, vsys));

    return Stream.concat(Stream.concat(pre, rules), post).collect(ImmutableList.toImmutableList());
  }

  /** Generate outgoing IpAccessList for the specified zone */
  private IpAccessList generateOutgoingFilter(
      String name, Zone toZone, List<Map.Entry<Rule, Vsys>> rules) {
    List<IpAccessListLine> lines =
        rules.stream()
            .filter(
                entry -> {
                  Rule rule = entry.getKey();
                  return (!rule.getDisabled()
                      && (rule.getTo().contains(toZone.getName())
                          || rule.getTo().contains(CATCHALL_ZONE_NAME)));
                })
            .map(entry -> toIpAccessListLine(entry.getKey(), entry.getValue()))
            .collect(ImmutableList.toImmutableList());

    return IpAccessList.builder().setName(name).setLines(lines).build();
  }

  @Nullable
  private static IpSpace ipSpaceFromRuleEndpoints(
      Collection<RuleEndpoint> endpoints, Vsys vsys, Warnings w) {
    return AclIpSpace.union(
        endpoints.stream()
            .map(source -> ruleEndpointToIpSpace(source, vsys, w))
            .collect(Collectors.toList()));
  }

  /** Convert specified firewall rule into an IpAccessListLine */
  private IpAccessListLine toIpAccessListLine(Rule rule, Vsys vsys) {
    assert !rule.getDisabled(); // handled by caller.

    IpAccessListLine.Builder ipAccessListLineBuilder =
        IpAccessListLine.builder().setName(rule.getName()).setAction(rule.getAction());

    List<AclLineMatchExpr> conjuncts = new TreeList<>();
    // Match SRC IPs if specified.
    IpSpace srcIps = ipSpaceFromRuleEndpoints(rule.getSource(), vsys, _w);
    if (srcIps != null) {
      AclLineMatchExpr match =
          new MatchHeaderSpace(HeaderSpace.builder().setSrcIps(srcIps).build());
      if (rule.getNegateSource()) {
        match = new NotMatchExpr(match);
      }
      conjuncts.add(match);
    }
    // Match DST IPs if specified.
    IpSpace dstIps = ipSpaceFromRuleEndpoints(rule.getDestination(), vsys, _w);
    if (dstIps != null) {
      AclLineMatchExpr match =
          new MatchHeaderSpace(HeaderSpace.builder().setDstIps(dstIps).build());
      if (rule.getNegateDestination()) {
        match = new NotMatchExpr(match);
      }
      conjuncts.add(match);
    }

    // Construct source zone (source interface) match expression
    SortedSet<String> ruleFroms = rule.getFrom();
    if (!ruleFroms.isEmpty()) {
      List<String> srcInterfaces = new TreeList<>();
      for (String zoneName : ruleFroms) {
        if (zoneName.equals(CATCHALL_ZONE_NAME)) {
          for (Zone zone : rule.getVsys().getZones().values()) {
            srcInterfaces.addAll(zone.getInterfaceNames());
          }
          break;
        }
        srcInterfaces.addAll(rule.getVsys().getZones().get(zoneName).getInterfaceNames());
      }
      // TODO: uncomment when we fix inheritance from panorama
      assert srcInterfaces != null;
      // conjuncts.add(new MatchSrcInterface(srcInterfaces));
    }

    // TODO(https://github.com/batfish/batfish/issues/2097): need to handle matching specified
    // applications

    // Construct service match expression
    SortedSet<ServiceOrServiceGroupReference> ruleServices = rule.getService();
    if (!ruleServices.isEmpty()) {
      List<AclLineMatchExpr> serviceDisjuncts = new TreeList<>();
      for (ServiceOrServiceGroupReference service : ruleServices) {
        String serviceName = service.getName();

        // Check for matching object before using built-ins
        String vsysName = service.getVsysName(this, vsys);
        if (vsysName != null) {
          serviceDisjuncts.add(
              new PermittedByAcl(computeServiceGroupMemberAclName(vsysName, serviceName)));
        } else if (serviceName.equals(CATCHALL_SERVICE_NAME)) {
          serviceDisjuncts.clear();
          serviceDisjuncts.add(TrueExpr.INSTANCE);
          break;
        } else if (serviceName.equals(ServiceBuiltIn.SERVICE_HTTP.getName())) {
          serviceDisjuncts.add(new MatchHeaderSpace(ServiceBuiltIn.SERVICE_HTTP.getHeaderSpace()));
        } else if (serviceName.equals(ServiceBuiltIn.SERVICE_HTTPS.getName())) {
          serviceDisjuncts.add(new MatchHeaderSpace(ServiceBuiltIn.SERVICE_HTTPS.getHeaderSpace()));
        } else {
          _w.redFlag(String.format("No matching service group/object found for: %s", serviceName));
        }
      }
      conjuncts.add(new OrMatchExpr(serviceDisjuncts));
    }

    return ipAccessListLineBuilder
        .setName(rule.getName())
        .setMatchCondition(new AndMatchExpr(conjuncts))
        .build();
  }

  /** Converts {@link RuleEndpoint} to {@code IpSpace} */
  @Nonnull
  private static IpSpace ruleEndpointToIpSpace(RuleEndpoint endpoint, Vsys vsys, Warnings w) {
    String endpointValue = endpoint.getValue();
    // Palo Alto allows object references that look like IP addresses, ranges, etc.
    // Devices use objects over constants when possible, so, check to see if there is a matching
    // group or object regardless of the type of endpoint we're expecting.
    if (vsys.getAddressObjects().containsKey(endpointValue)) {
      return vsys.getAddressObjects().get(endpointValue).getIpSpace();
    } else if (vsys.getAddressGroups().containsKey(endpoint.getValue())) {
      return vsys.getAddressGroups()
          .get(endpointValue)
          .getIpSpace(vsys.getAddressObjects(), vsys.getAddressGroups());
    } else {
      // No named object found matching this endpoint, so parse the endpoint value as is
      switch (endpoint.getType()) {
        case Any:
          return UniverseIpSpace.INSTANCE;
        case IP_ADDRESS:
          return Ip.parse(endpointValue).toIpSpace();
        case IP_PREFIX:
          return Prefix.parse(endpointValue).toIpSpace();
        case IP_RANGE:
          String[] ips = endpointValue.split("-");
          return IpRange.range(Ip.parse(ips[0]), Ip.parse(ips[1]));
        case REFERENCE:
          // Undefined reference
          w.redFlag("No matching address group/object found for RuleEndpoint: " + endpoint);
          return EmptyIpSpace.INSTANCE;
        default:
          w.redFlag("Could not convert RuleEndpoint to IpSpace: " + endpoint);
          return EmptyIpSpace.INSTANCE;
      }
    }
  }

  /** Convert Palo Alto specific interface into vendor independent model interface */
  private org.batfish.datamodel.Interface toInterface(Interface iface) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(name, _c, InterfaceType.PHYSICAL);
    Integer mtu = iface.getMtu();
    if (mtu != null) {
      newIface.setMtu(mtu);
    }
    newIface.setAddress(iface.getAddress());
    newIface.setAllAddresses(iface.getAllAddresses());
    newIface.setActive(iface.getActive());
    newIface.setDescription(iface.getComment());

    Zone zone = iface.getZone();
    List<IpAccessListLine> lines;
    if (zone != null) {
      newIface.setZoneName(zone.getName());
      lines =
          ImmutableList.of(
              IpAccessListLine.accepting(
                  new PermittedByAcl(
                      computeOutgoingFilterName(
                          computeObjectName(zone.getVsys().getName(), zone.getName())))));
    } else {
      // Do not allow any traffic exiting an unzoned interface
      lines = ImmutableList.of(IpAccessListLine.rejecting("Not in a zone", TrueExpr.INSTANCE));
    }
    /* TODO: correct and uncomment.
    IpAccessList outgoing =
        IpAccessList.builder()
            .setOwner(_c)
            .setName(computeOutgoingFilterName(iface.getName()))
            .setLines(lines)
            .build();
     */
    assert lines != null; // suppress unused warning
    return newIface;
  }

  /** Convert Palo Alto specific virtual router into vendor independent model Vrf */
  private Vrf toVrf(VirtualRouter vr) {
    Vrf vrf = new Vrf(vr.getName());

    // Static routes
    for (Entry<String, StaticRoute> e : vr.getStaticRoutes().entrySet()) {
      StaticRoute sr = e.getValue();
      // Can only construct a static route if it has a destination
      if (sr.getDestination() != null) {
        vrf.getStaticRoutes()
            .add(
                org.batfish.datamodel.StaticRoute.builder()
                    .setNextHopInterface(sr.getNextHopInterface())
                    .setNextHopIp(sr.getNextHopIp())
                    .setAdministrativeCost(sr.getAdminDistance())
                    .setMetric(sr.getMetric())
                    .setNetwork(sr.getDestination())
                    .build());
      } else {
        _w.redFlag(
            String.format(
                "Cannot convert static route %s, as it does not have a destination.", e.getKey()));
      }
    }

    // Interfaces
    NavigableMap<String, org.batfish.datamodel.Interface> map = new TreeMap<>();
    for (String interfaceName : vr.getInterfaceNames()) {
      org.batfish.datamodel.Interface iface = _c.getAllInterfaces().get(interfaceName);
      if (iface != null) {
        map.put(interfaceName, iface);
        iface.setVrf(vrf);
      }
    }
    vrf.setInterfaces(map);

    return vrf;
  }

  /** Convert Palo Alto zone to vendor independent model zone */
  private org.batfish.datamodel.Zone toZone(String name, Zone zone) {
    org.batfish.datamodel.Zone newZone = new org.batfish.datamodel.Zone(name);
    newZone.setInterfaces(zone.getInterfaceNames());
    return newZone;
  }

  /**
   * Attach interfaces to zones. This is not done during extraction in case the file is structured
   * so that zones are defined first.
   */
  private void attachInterfacesToZones() {
    Map<String, Interface> allInterfaces =
        Streams.concat(
                getInterfaces().entrySet().stream(),
                getInterfaces().values().stream().flatMap(i -> i.getUnits().entrySet().stream()))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    // Assign the appropriate zone to each interface
    for (Vsys vsys : getVirtualSystems().values()) {
      for (Zone zone : vsys.getZones().values()) {
        for (String ifname : zone.getInterfaceNames()) {
          Interface iface = allInterfaces.get(ifname);
          if (iface != null) {
            iface.setZone(zone);
          } else {
            // do nothing. Assume that an undefined reference was logged elsewhere.
            assert true;
          }
        }
      }
    }
  }

  @Override
  public List<Configuration> toVendorIndependentConfigurations() throws VendorConversionException {
    String hostname = getHostname();
    _c = new Configuration(hostname, _vendor);
    _c.setDefaultCrossZoneAction(LineAction.DENY);
    _c.setDefaultInboundAction(LineAction.PERMIT);
    _c.setDnsServers(getDnsServers());
    _c.setNtpServers(getNtpServers());

    // Before processing any Vsys, ensure that interfaces are attached to zones.
    attachInterfacesToZones();

    // Handle converting items within virtual systems
    convertVirtualSystems();

    for (Entry<String, Interface> i : _interfaces.entrySet()) {
      org.batfish.datamodel.Interface viIface = toInterface(i.getValue());
      _c.getAllInterfaces().put(viIface.getName(), viIface);

      for (Entry<String, Interface> unit : i.getValue().getUnits().entrySet()) {
        org.batfish.datamodel.Interface viUnit = toInterface(unit.getValue());
        viUnit.addDependency(new Dependency(viIface.getName(), DependencyType.BIND));
        _c.getAllInterfaces().put(viUnit.getName(), viUnit);
      }
    }

    // Vrf conversion uses interfaces, so must be done after interface exist in VI model
    for (Entry<String, VirtualRouter> vr : _virtualRouters.entrySet()) {
      _c.getVrfs().put(vr.getKey(), toVrf(vr.getValue()));
    }

    // Batfish cannot handle interfaces without a Vrf
    // So put orphaned interfaces in a constructed Vrf and shut them down
    Vrf nullVrf = new Vrf(NULL_VRF_NAME);
    NavigableMap<String, org.batfish.datamodel.Interface> orphanedInterfaces = new TreeMap<>();
    for (Entry<String, org.batfish.datamodel.Interface> i : _c.getAllInterfaces().entrySet()) {
      org.batfish.datamodel.Interface iface = i.getValue();
      if (iface.getVrf() == null) {
        orphanedInterfaces.put(iface.getName(), iface);
        iface.setVrf(nullVrf);
        iface.setActive(false);
        _w.redFlag(
            String.format(
                "Interface %s is not in a virtual-router, placing in %s and shutting it down.",
                iface.getName(), nullVrf.getName()));
      }
    }
    if (orphanedInterfaces.size() > 0) {
      nullVrf.setInterfaces(orphanedInterfaces);
      _c.getVrfs().put(nullVrf.getName(), nullVrf);
    }

    // Count and mark simple structure usages and identify undefined references
    markConcreteStructure(PaloAltoStructureType.GLOBAL_PROTECT_APP_CRYPTO_PROFILE);
    markConcreteStructure(PaloAltoStructureType.IKE_CRYPTO_PROFILE);
    markConcreteStructure(PaloAltoStructureType.IPSEC_CRYPTO_PROFILE);
    markConcreteStructure(
        PaloAltoStructureType.INTERFACE,
        PaloAltoStructureUsage.IMPORT_INTERFACE,
        PaloAltoStructureUsage.STATIC_ROUTE_INTERFACE,
        PaloAltoStructureUsage.VIRTUAL_ROUTER_INTERFACE,
        PaloAltoStructureUsage.VSYS_IMPORT_INTERFACE,
        PaloAltoStructureUsage.ZONE_INTERFACE);
    markConcreteStructure(PaloAltoStructureType.RULE, PaloAltoStructureUsage.RULE_SELF_REF);
    markConcreteStructure(
        PaloAltoStructureType.ZONE,
        PaloAltoStructureUsage.RULE_FROM_ZONE,
        PaloAltoStructureUsage.RULE_TO_ZONE);

    // Handle marking for structures that may exist in one of a couple namespaces
    // Handle service objects/groups that may overlap with built-in names
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP_OR_NONE,
        ImmutableList.of(PaloAltoStructureType.SERVICE, PaloAltoStructureType.SERVICE_GROUP),
        true,
        PaloAltoStructureUsage.SERVICE_GROUP_MEMBER,
        PaloAltoStructureUsage.RULEBASE_SERVICE);
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP,
        ImmutableList.of(PaloAltoStructureType.SERVICE, PaloAltoStructureType.SERVICE_GROUP),
        PaloAltoStructureUsage.SERVICE_GROUP_MEMBER,
        PaloAltoStructureUsage.RULEBASE_SERVICE);

    // Handle marking rule endpoints
    // First, handle those which may or may not be referencing objects (e.g. "1.2.3.4" may be IP
    // address or a named object)
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.ADDRESS_GROUP_OR_ADDRESS_OBJECT_OR_NONE,
        ImmutableList.of(PaloAltoStructureType.ADDRESS_GROUP, PaloAltoStructureType.ADDRESS_OBJECT),
        true,
        PaloAltoStructureUsage.RULE_DESTINATION,
        PaloAltoStructureUsage.RULE_SOURCE);
    // Next, handle address object references which are definitely referencing objects
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.ADDRESS_GROUP_OR_ADDRESS_OBJECT,
        ImmutableList.of(PaloAltoStructureType.ADDRESS_GROUP, PaloAltoStructureType.ADDRESS_OBJECT),
        PaloAltoStructureUsage.ADDRESS_GROUP_STATIC,
        PaloAltoStructureUsage.RULE_DESTINATION,
        PaloAltoStructureUsage.RULE_SOURCE);

    // Applications or Application-Groups
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.APPLICATION_GROUP_OR_APPLICATION,
        ImmutableList.of(
            PaloAltoStructureType.APPLICATION_GROUP, PaloAltoStructureType.APPLICATION),
        PaloAltoStructureUsage.RULE_APPLICATION);

    return ImmutableList.of(_c);
  }

  /**
   * Helper method to return DefinedStructureInfo for the structure with the specified name that
   * could be any of the specified structureTypesToCheck, return null if no match is found
   */
  private @Nullable DefinedStructureInfo findDefinedStructure(
      String name, Collection<PaloAltoStructureType> structureTypesToCheck) {
    for (PaloAltoStructureType typeToCheck : structureTypesToCheck) {
      Map<String, DefinedStructureInfo> matchingDefinitions =
          _structureDefinitions.get(typeToCheck.getDescription());
      if (matchingDefinitions != null && !matchingDefinitions.isEmpty()) {
        DefinedStructureInfo definition = matchingDefinitions.get(name);
        if (definition != null) {
          return definition;
        }
      }
    }
    return null;
  }

  /**
   * Update referrers and/or warn for undefined structures based on references to an abstract
   * structure type existing in either the reference's namespace or shared namespace
   */
  private void markAbstractStructureFromUnknownNamespace(
      PaloAltoStructureType type,
      Collection<PaloAltoStructureType> structureTypesToCheck,
      PaloAltoStructureUsage... usages) {
    markAbstractStructureFromUnknownNamespace(type, structureTypesToCheck, false, usages);
  }

  private void markAbstractStructureFromUnknownNamespace(
      PaloAltoStructureType type,
      Collection<PaloAltoStructureType> structureTypesToCheck,
      boolean ignoreUndefined,
      PaloAltoStructureUsage... usages) {
    Map<String, SortedMap<StructureUsage, SortedMultiset<Integer>>> references =
        firstNonNull(_structureReferences.get(type), Collections.emptyMap());
    for (PaloAltoStructureUsage usage : usages) {
      references.forEach(
          (nameWithNamespace, byUsage) -> {
            String name = extractObjectName(nameWithNamespace);
            Multiset<Integer> lines = firstNonNull(byUsage.get(usage), TreeMultiset.create());
            // Check this namespace first
            DefinedStructureInfo info =
                findDefinedStructure(nameWithNamespace, structureTypesToCheck);
            // Check shared namespace if there was no match
            if (info == null) {
              info =
                  findDefinedStructure(
                      computeObjectName(SHARED_VSYS_NAME, name), structureTypesToCheck);
            }

            // Now update reference count if applicable
            if (info != null) {
              info.setNumReferrers(
                  info.getNumReferrers() == DefinedStructureInfo.UNKNOWN_NUM_REFERRERS
                      ? DefinedStructureInfo.UNKNOWN_NUM_REFERRERS
                      : info.getNumReferrers() + lines.size());
            } else if (!ignoreUndefined) {
              for (int line : lines) {
                undefined(type, name, usage, line);
              }
            }
          });
    }
  }
}

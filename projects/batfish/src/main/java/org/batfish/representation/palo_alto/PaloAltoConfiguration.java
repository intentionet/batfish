package org.batfish.representation.palo_alto;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.datamodel.IpAccessListLine.accepting;
import static org.batfish.datamodel.IpAccessListLine.rejecting;
import static org.batfish.datamodel.Names.zoneToZoneFilter;
import static org.batfish.datamodel.acl.AclLineMatchExprs.ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.representation.palo_alto.Conversions.computeAndSetPerPeerExportPolicy;
import static org.batfish.representation.palo_alto.Conversions.computeAndSetPerPeerImportPolicy;
import static org.batfish.representation.palo_alto.Conversions.getBgpCommonExportPolicy;
import static org.batfish.representation.palo_alto.OspfVr.DEFAULT_LOOPBACK_OSPF_COST;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_OBJECT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.Streams;
import com.google.common.collect.TreeMultiset;
import com.google.common.collect.TreeRangeSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.VendorConversionException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IntegerSpace;
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
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.NotMatchExpr;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily.Builder;
import org.batfish.datamodel.flow.TransformationStep.TransformationType;
import org.batfish.datamodel.ospf.NssaSettings;
import org.batfish.datamodel.ospf.OspfArea;
import org.batfish.datamodel.ospf.OspfDefaultOriginateType;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.ospf.OspfNetworkType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.ospf.StubSettings;
import org.batfish.datamodel.packet_policy.ApplyTransformation;
import org.batfish.datamodel.packet_policy.BoolExpr;
import org.batfish.datamodel.packet_policy.Conjunction;
import org.batfish.datamodel.packet_policy.FibLookup;
import org.batfish.datamodel.packet_policy.FibLookupOutgoingInterfaceIsOneOf;
import org.batfish.datamodel.packet_policy.IngressInterfaceVrf;
import org.batfish.datamodel.packet_policy.PacketMatchExpr;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.packet_policy.Return;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationStep;
import org.batfish.representation.palo_alto.OspfAreaNssa.DefaultRouteType;
import org.batfish.representation.palo_alto.OspfInterface.LinkType;
import org.batfish.representation.palo_alto.Zone.Type;
import org.batfish.vendor.StructureUsage;
import org.batfish.vendor.VendorConfiguration;

public final class PaloAltoConfiguration extends VendorConfiguration {

  /** This is the name of an application that matches all traffic */
  public static final String CATCHALL_APPLICATION_NAME = "any";

  /** This is the name of an endpoint that matches all traffic */
  public static final String CATCHALL_ENDPOINT_NAME = "any";

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

  private @Nullable Vsys _panorama;

  private @Nullable Vsys _shared;

  private final SortedMap<String, Vsys> _sharedGateways;

  private ConfigurationFormat _vendor;

  private final SortedMap<String, VirtualRouter> _virtualRouters;

  private final SortedMap<String, Vsys> _virtualSystems;

  // vsys name -> zone name -> outgoing transformation
  private final Map<String, Map<String, Transformation>> _zoneOutgoingTransformations;

  public PaloAltoConfiguration() {
    _cryptoProfiles = new LinkedList<>();
    _interfaces = new TreeMap<>();
    _sharedGateways = new TreeMap<>();
    _virtualRouters = new TreeMap<>();
    _virtualSystems = new TreeMap<>();
    _zoneOutgoingTransformations = new TreeMap<>();
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

  public @Nonnull SortedMap<String, Vsys> getSharedGateways() {
    return _sharedGateways;
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

  static String computePeerExportPolicyName(Prefix remoteAddress) {
    return "~PEER_EXPORT_POLICY:" + remoteAddress + "~";
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

  /** Generate PacketPolicy name given an interface name */
  public static String computePacketPolicyName(String interfaceName) {
    return String.format("~%s~PACKET_POLICY~", interfaceName);
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

  /**
   * Checks whether the given NAT rule's {@link NatRule#getFrom()}, {@link NatRule#getTo()}, {@link
   * NatRule#getSource()}, and {@link NatRule#getDestination()} are valid (configured and not
   * empty). Files conversion warnings for any invalid rules if {@code fileWarnings} is set.
   *
   * <p>No NAT rule that fails this check should be converted to VI. However, a rule that does pass
   * should not necessarily be converted to VI -- there may be other reasons not to convert.
   */
  private boolean checkNatRuleValid(NatRule rule, boolean fileWarnings) {
    String missingItem = null;
    if (rule.getTo() == null) {
      missingItem = "to zone";
    } else if (rule.getFrom().isEmpty()) {
      missingItem = "from zones";
    } else if (rule.getSource().isEmpty()) {
      missingItem = "source addresses";
    } else if (rule.getDestination().isEmpty()) {
      missingItem = "destination addresses";
    }
    if (missingItem != null && fileWarnings) {
      _w.redFlag(
          String.format(
              "NAT rule %s ignored because it has no %s configured", rule.getName(), missingItem));
    }
    return missingItem == null;
  }

  /** Convert vsys components to vendor independent model */
  private void convertVirtualSystems() {
    for (Vsys vsys : _virtualSystems.values()) {

      populateZoneOutgoingTransformations(vsys);

      // Create zone-specific outgoing ACLs.
      for (Zone toZone : vsys.getZones().values()) {
        if (toZone.getType() != Type.LAYER3) {
          continue;
        }
        IpAccessList acl =
            generateOutgoingFilter(toZone, _sharedGateways.values(), _virtualSystems.values());
        _c.getIpAccessLists().put(acl.getName(), acl);
      }

      // Create cross-zone ACLs for each pair of zones, including self-zone.
      List<Map.Entry<SecurityRule, Vsys>> rules = getAllSecurityRules(vsys);
      for (Zone fromZone : vsys.getZones().values()) {
        Type fromType = fromZone.getType();
        for (Zone toZone : vsys.getZones().values()) {
          Type toType = toZone.getType();
          if (fromType == Type.EXTERNAL && toType == Type.EXTERNAL) {
            // Don't add ACLs for zones when both are external.
            continue;
          }
          if (fromType != Type.EXTERNAL && toType != Type.EXTERNAL && fromType != toType) {
            // If one zone is not external, they have to match.
            continue;
          }
          if (fromType == Type.LAYER3 || toType == Type.LAYER3) {
            // only generate IP ACL when at least one zone is layer-3
            IpAccessList acl = generateCrossZoneFilter(fromZone, toZone, rules);
            _c.getIpAccessLists().put(acl.getName(), acl);
          }
        }
      }
    }
  }

  @Nonnull
  private void populateZoneOutgoingTransformations(Vsys vsys) {
    Map<String, List<Transformation.Builder>> toZoneTransformations = new HashMap<>();

    TransformationStep transformPort =
        TransformationStep.assignSourcePort(
            NamedPort.EPHEMERAL_LOWEST.number(), NamedPort.EPHEMERAL_HIGHEST.number());

    getAllNatRules(vsys)
        // This method is run once and goes through all NAT rules. File invalid rule warnings here.
        .filter(r -> checkNatRuleValid(r, true) && r.doesSourceTranslation())
        .forEach(
            r -> {
              RangeSet<Ip> pool =
                  ipRangeSetFromRuleEndpoints(
                      // Already filtered out any rules for which these are null
                      r.getSourceTranslation().getDynamicIpAndPort().getTranslatedAddresses(),
                      vsys,
                      _w);
              if (pool.isEmpty()) {
                // Can't have source IP translation rules with empty IP pool
                _w.redFlag(
                    String.format(
                        "NAT rule %s ignored for source translation because its source translation pool is empty",
                        r.getName()));
                return;
              }
              Transformation.Builder t =
                  Transformation.when(getSourceNatRuleMatchExpr(r, vsys))
                      .apply(
                          new AssignIpAddressFromPool(
                              TransformationType.SOURCE_NAT, IpField.SOURCE, pool),
                          transformPort);
              // Note that "to any" is not permitted
              toZoneTransformations.computeIfAbsent(r.getTo(), x -> new ArrayList<>()).add(t);
            });

    Map<String, Transformation> finalZoneTransformations =
        _zoneOutgoingTransformations.computeIfAbsent(vsys.getName(), v -> new TreeMap<>());
    toZoneTransformations.forEach(
        (zoneName, builders) -> {
          Transformation t = null;
          for (int i = builders.size() - 1; i >= 0; i--) {
            Transformation.Builder prevT = builders.get(i);
            prevT.setOrElse(t);
            t = prevT.build();
          }
          finalZoneTransformations.put(zoneName, t);
        });
  }

  private AclLineMatchExpr getSourceNatRuleMatchExpr(NatRule rule, Vsys vsys) {
    // Match source and destination
    MatchHeaderSpace matchHeaderSpace = getRuleMatchHeaderSpace(rule, vsys);

    if (rule.getFrom().contains(CATCHALL_ZONE_NAME)) {
      // Rule says "from any" -- no need to match on packet source interface
      return matchHeaderSpace;
    }

    // Match from
    Set<String> fromIfaces =
        rule.getFrom().stream()
            .flatMap(
                fromZone -> {
                  Zone zone = vsys.getZones().get(fromZone);
                  return zone == null ? Stream.of() : zone.getInterfaceNames().stream();
                })
            .collect(ImmutableSet.toImmutableSet());
    MatchSrcInterface matchSrcInterface = new MatchSrcInterface(fromIfaces);

    return new AndMatchExpr(ImmutableList.of(matchHeaderSpace, matchSrcInterface));
  }

  /** Convert unique aspects of shared-gateways. */
  private void convertSharedGateways() {
    for (Vsys sharedGateway : _sharedGateways.values()) {
      // Create shared-gateway outgoing ACL.
      IpAccessList acl =
          generateSharedGatewayOutgoingFilter(
              sharedGateway, _sharedGateways.values(), _virtualSystems.values());
      _c.getIpAccessLists().put(acl.getName(), acl);
    }
  }

  /** Convert structures common to all vsys-like namespaces */
  private void convertNamespaces() {
    ImmutableSortedSet.Builder<String> loggingServers = ImmutableSortedSet.naturalOrder();
    Streams.concat(
            _sharedGateways.values().stream(),
            _virtualSystems.values().stream(),
            Stream.of(_panorama, _shared).filter(Objects::nonNull))
        .forEach(
            namespace -> {
              loggingServers.addAll(namespace.getSyslogServerAddresses());
              // convert address objects and groups to ip spaces
              namespace
                  .getAddressObjects()
                  .forEach(
                      (name, addressObject) -> {
                        _c.getIpSpaces().put(name, addressObject.getIpSpace());
                        _c.getIpSpaceMetadata()
                            .put(name, new IpSpaceMetadata(name, ADDRESS_OBJECT.getDescription()));
                      });

              namespace
                  .getAddressGroups()
                  .forEach(
                      (name, addressGroup) -> {
                        _c.getIpSpaces()
                            .put(
                                name,
                                addressGroup.getIpSpace(
                                    namespace.getAddressObjects(), namespace.getAddressGroups()));
                        _c.getIpSpaceMetadata()
                            .put(name, new IpSpaceMetadata(name, ADDRESS_GROUP.getDescription()));
                      });

              // Convert PAN zones
              for (Entry<String, Zone> zoneEntry : namespace.getZones().entrySet()) {
                Zone zone = zoneEntry.getValue();
                org.batfish.datamodel.Zone newZone =
                    toZone(computeObjectName(namespace.getName(), zone.getName()), zone);
                _c.getZones().put(newZone.getName(), newZone);
              }

              // Services
              for (Service service : namespace.getServices().values()) {
                IpAccessList acl = service.toIpAccessList(LineAction.PERMIT, this, namespace, _w);
                _c.getIpAccessLists().put(acl.getName(), acl);
              }

              // Service groups
              for (ServiceGroup serviceGroup : namespace.getServiceGroups().values()) {
                IpAccessList acl =
                    serviceGroup.toIpAccessList(LineAction.PERMIT, this, namespace, _w);
                _c.getIpAccessLists().put(acl.getName(), acl);
              }
            });
    _c.setLoggingServers(loggingServers.build());
  }

  /** Generates a cross-zone ACL from the two given zones in the same Vsys using the given rules. */
  private IpAccessList generateCrossZoneFilter(
      Zone fromZone, Zone toZone, List<Map.Entry<SecurityRule, Vsys>> rules) {
    assert fromZone.getVsys() == toZone.getVsys();

    String crossZoneFilterName =
        zoneToZoneFilter(
            computeObjectName(fromZone.getVsys().getName(), fromZone.getName()),
            computeObjectName(toZone.getVsys().getName(), toZone.getName()));

    if (fromZone.getType() != Type.EXTERNAL && fromZone.getInterfaceNames().isEmpty()
        || toZone.getType() != Type.EXTERNAL && toZone.getInterfaceNames().isEmpty()) {
      // Non-external zones must have interfaces.
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
                  SecurityRule rule = e.getKey();
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

  /**
   * Collects the security rules from this Vsys and merges the common pre-/post-rulebases from
   * Panorama.
   */
  private List<Map.Entry<SecurityRule, Vsys>> getAllSecurityRules(Vsys vsys) {
    Stream<Map.Entry<SecurityRule, Vsys>> pre =
        _panorama == null
            ? Stream.of()
            : _panorama.getPreRulebase().getSecurityRules().values().stream()
                .map(r -> new SimpleImmutableEntry<>(r, _panorama));
    Stream<Map.Entry<SecurityRule, Vsys>> post =
        _panorama == null
            ? Stream.of()
            : _panorama.getPostRulebase().getSecurityRules().values().stream()
                .map(r -> new SimpleImmutableEntry<>(r, _panorama));
    Stream<Map.Entry<SecurityRule, Vsys>> rules =
        vsys.getRulebase().getSecurityRules().values().stream()
            .map(r -> new SimpleImmutableEntry<>(r, vsys));

    return Stream.concat(Stream.concat(pre, rules), post).collect(ImmutableList.toImmutableList());
  }

  /**
   * Collects the NAT rules from this Vsys and merges the common pre-/post-rulebases from Panorama.
   */
  private Stream<NatRule> getAllNatRules(Vsys vsys) {
    Stream<NatRule> pre =
        _panorama == null
            ? Stream.of()
            : _panorama.getPreRulebase().getNatRules().values().stream();
    Stream<NatRule> post =
        _panorama == null
            ? Stream.of()
            : _panorama.getPostRulebase().getNatRules().values().stream();
    Stream<NatRule> rules = vsys.getRulebase().getNatRules().values().stream();
    return Streams.concat(pre, rules, post);
  }

  /**
   * Generate {@link IpAccessList} to be used as outgoing filter by interfaces in layer-3 zone
   * {@code toZone}, given supplied definitions for all {@code sharedGateways} and {@code
   * virtualSystems}.
   */
  @VisibleForTesting
  static @Nonnull IpAccessList generateOutgoingFilter(
      Zone toZone, Collection<Vsys> sharedGateways, Collection<Vsys> virtualSystems) {
    Vsys vsys = toZone.getVsys();
    List<IpAccessListLine> lines =
        vsys.getZones().values().stream()
            .flatMap(
                fromZone ->
                    generateCrossZoneCalls(fromZone, toZone, sharedGateways, virtualSystems))
            .collect(ImmutableList.toImmutableList());
    return IpAccessList.builder()
        .setName(computeOutgoingFilterName(computeObjectName(vsys.getName(), toZone.getName())))
        .setLines(lines)
        .build();
  }

  /**
   * Generate outgoing filter lines for traffic exiting {@code sharedGateway} and entering some
   * vsys, given supplied definitions for all {@code sharedGateways} and {@code virtualSystems}.
   */
  @VisibleForTesting
  static @Nonnull IpAccessList generateSharedGatewayOutgoingFilter(
      Vsys sharedGateway, Collection<Vsys> sharedGateways, Collection<Vsys> virtualSystems) {
    Stream<IpAccessListLine> vsysSgLines =
        virtualSystems.stream()
            .flatMap(vsys -> generateVsysSharedGatewayCalls(sharedGateway, vsys));
    Stream<IpAccessListLine> sgSgLines =
        Stream.concat(Stream.of(sharedGateway), sharedGateways.stream())
            .flatMap(
                ingressSharedGateway -> generateSgSgLines(sharedGateway, ingressSharedGateway));
    return IpAccessList.builder()
        .setName(
            computeOutgoingFilterName(
                computeObjectName(sharedGateway.getName(), sharedGateway.getName())))
        .setLines(Stream.concat(vsysSgLines, sgSgLines).collect(ImmutableList.toImmutableList()))
        .build();
  }

  /**
   * Generate outgoing filter lines for traffic entering {@code ingressSharedGateway} and exiting
   * {@code sharedGateway}. Such traffic is unfiltered.
   */
  @VisibleForTesting
  static @Nonnull Stream<IpAccessListLine> generateSgSgLines(
      Vsys sharedGateway, Vsys ingressSharedGateway) {
    Set<String> ingressInterfaces = ingressSharedGateway.getImportedInterfaces();
    if (ingressInterfaces.isEmpty()) {
      return Stream.of();
    }
    AclLineMatchExpr matchFromIngressSgInterface = matchSrcInterface(ingressInterfaces);
    // If src interface in ingressSharedGateway, then permit.
    // Else no action.
    return Stream.of(accepting(matchFromIngressSgInterface));
  }

  /**
   * Generate outgoing filter lines for traffic exiting {@code sharedGateway} and entering at some
   * zone of {@code vsys}. No lines are generated if there are no external zones in {@code vsys}
   * that see the {@code sharedGateway}.
   */
  @VisibleForTesting
  static @Nonnull Stream<IpAccessListLine> generateVsysSharedGatewayCalls(
      Vsys sharedGateway, Vsys vsys) {
    String sharedGatewayName = sharedGateway.getName();
    return vsys.getZones().values().stream()
        .filter(
            externalToZone ->
                externalToZone.getType() == Type.EXTERNAL
                    && externalToZone.getExternalNames().contains(sharedGatewayName))
        .flatMap(
            externalToZone ->
                vsys.getZones().values().stream()
                    .filter(externalFromZone -> externalFromZone.getType() == Type.LAYER3)
                    .flatMap(
                        externalFromZone ->
                            generateCrossZoneCallsFromLayer3(externalFromZone, externalToZone)));
  }

  /**
   * Generate outgoing filter lines to be applied to traffic entering {@code fromZone} (either
   * directly or via an external zone) and exiting layer-3 zone {@code toZone} in the same vsys,
   * given supplied definitions for all {@code virtualSystems}.
   */
  @VisibleForTesting
  static @Nonnull Stream<IpAccessListLine> generateCrossZoneCalls(
      Zone fromZone,
      Zone toZone,
      Collection<Vsys> sharedGateways,
      Collection<Vsys> virtualSystems) {
    Vsys vsys = fromZone.getVsys();
    assert vsys == toZone.getVsys(); // sanity check
    switch (fromZone.getType()) {
      case EXTERNAL:
        return generateCrossZoneCallsFromExternal(fromZone, toZone, sharedGateways, virtualSystems);
      case LAYER3:
        return generateCrossZoneCallsFromLayer3(fromZone, toZone);
      default:
        return Stream.of();
    }
  }

  /**
   * Generate outgoing filter lines to be applied to traffic entering layer-3 zone {@code fromZone}
   * and exiting layer-3 zone {@code toZone} of the same vsys. The generated lines apply the
   * appropriate cross-zone filter to traffic entering an interface of {@code fromZone}.
   */
  @VisibleForTesting
  static @Nonnull Stream<IpAccessListLine> generateCrossZoneCallsFromLayer3(
      Zone fromZone, Zone toZone) {
    Set<String> fromZoneInterfaces = fromZone.getInterfaceNames();
    if (fromZoneInterfaces.isEmpty()) {
      return Stream.of();
    }
    AclLineMatchExpr matchFromZoneInterface = matchSrcInterface(fromZoneInterfaces);
    Vsys vsys = fromZone.getVsys();
    assert vsys == toZone.getVsys(); // sanity check
    String vsysName = vsys.getName();
    String crossZoneFilterName =
        zoneToZoneFilter(
            computeObjectName(vsysName, fromZone.getName()),
            computeObjectName(vsysName, toZone.getName()));
    // If src interface in zone and filters permits, then permit.
    // Else if src interface in zone, filter must have denied.
    return Stream.of(
        accepting(and(matchFromZoneInterface, permittedByAcl(crossZoneFilterName))),
        rejecting(matchFromZoneInterface));
  }

  /**
   * Generate outgoing filter lines implementing the policy for all inter-vsys traffic exiting the
   * device through {@code toZone} on some vsys after entering the device at a layer-3 zone on
   * another external vsys. Any such traffic must pass each of a pair of cross-zone policies:
   * ({@code fromZone}, {@code toZone}) and some ({@code externalFromZone}, {@code externalToZone})
   * in an external vsys such that:
   *
   * <ul>
   *   <li>{@code fromZone} sees the external vsys.
   *   <li>{@code externalToZone} is an external zone on the external vsys that sees the egress
   *       vsys.
   *   <li>{@code externalFromZone} is a layer-3 zone on the external vsys containing the ingress
   *       interface of the traffic.
   * </ul>
   */
  @VisibleForTesting
  static @Nonnull Stream<IpAccessListLine> generateCrossZoneCallsFromExternal(
      Zone fromZone,
      Zone toZone,
      Collection<Vsys> sharedGateways,
      Collection<Vsys> virtualSystems) {
    Vsys vsys = fromZone.getVsys();
    assert fromZone.getVsys() == toZone.getVsys(); // sanity check
    Stream<IpAccessListLine> vsysLines =
        virtualSystems.stream()
            .filter(not(equalTo(vsys)))
            .filter(externalVsys -> fromZone.getExternalNames().contains(externalVsys.getName()))
            .flatMap(
                externalVsys -> generateInterVsysCrossZoneCalls(fromZone, toZone, externalVsys));
    Stream<IpAccessListLine> sgLines =
        sharedGateways.stream()
            .filter(sharedGateway -> fromZone.getExternalNames().contains(sharedGateway.getName()))
            .flatMap(
                sharedGateway ->
                    generatedSharedGatewayVsysCrossZoneCalls(fromZone, toZone, sharedGateway));
    return Stream.concat(vsysLines, sgLines);
  }

  /**
   * Generate outgoing filter lines to be applied to traffic entering some interface of {@code
   * sharedGateway} and exiting layer-3 zone {@code toZone} via external zone {@code fromZone} of
   * the latter's vsys.
   */
  @VisibleForTesting
  static @Nonnull Stream<IpAccessListLine> generatedSharedGatewayVsysCrossZoneCalls(
      Zone fromZone, Zone toZone, Vsys sharedGateway) {
    Vsys vsys = fromZone.getVsys();
    // sanity check
    assert vsys == toZone.getVsys();
    String vsysName = vsys.getName();
    Set<String> sharedGatewayInterfaces = sharedGateway.getImportedInterfaces();
    if (sharedGatewayInterfaces.isEmpty()) {
      return Stream.of();
    }
    AclLineMatchExpr matchFromZoneInterface = matchSrcInterface(sharedGatewayInterfaces);
    String crossZoneFilterName =
        zoneToZoneFilter(
            computeObjectName(vsysName, fromZone.getName()),
            computeObjectName(vsysName, toZone.getName()));
    // If src interface in shared-gateway and filters permits, then permit.
    // Else if src interface in shared-gateway, filter must have denied.
    return Stream.of(
        accepting(and(matchFromZoneInterface, permittedByAcl(crossZoneFilterName))),
        rejecting(matchFromZoneInterface));
  }

  /**
   * Generate outgoing filter lines to be applied to traffic entering some interface of {@code
   * externalVsys} and exiting layer-3 zone {@code toZone} via external zone {@code fromZone} of the
   * latter's vsys.
   */
  @VisibleForTesting
  static @Nonnull Stream<IpAccessListLine> generateInterVsysCrossZoneCalls(
      Zone fromZone, Zone toZone, Vsys externalVsys) {
    Vsys vsys = fromZone.getVsys();
    assert vsys == toZone.getVsys() && vsys != externalVsys; // sanity check
    String vsysName = vsys.getName();
    return externalVsys.getZones().values().stream()
        .filter(
            externalVsysToZone ->
                externalVsysToZone.getType() == Type.EXTERNAL
                    && externalVsysToZone.getExternalNames().contains(vsysName))
        .flatMap(
            externalVsysToZone ->
                externalVsys.getZones().values().stream()
                    .filter(externalVsysFromZone -> externalVsysFromZone.getType() == Type.LAYER3)
                    .flatMap(
                        externalVsysFromZone ->
                            generateDoubleCrossZoneCalls(
                                fromZone, toZone, externalVsysFromZone, externalVsysToZone)));
  }

  /**
   * Generate outgoing filter lines to be applied to traffic exiting {@code toZone} of some vsys
   * after entering {@code externalFromZone} of some other external vsys. The generated lines apply
   * the cross-zone filters for the two zone-pairs ({@code externalFromZone}, {@code
   * externalToZone}), and ({@code fromZone}, {@code toZone}), where {@code externalToZone} and
   * {@code fromZone} are external zones pointing at each other's vsys.
   */
  @VisibleForTesting
  static @Nonnull Stream<IpAccessListLine> generateDoubleCrossZoneCalls(
      Zone fromZone, Zone toZone, Zone externalFromZone, Zone externalToZone) {
    Vsys vsys = fromZone.getVsys();
    Vsys externalVsys = externalFromZone.getVsys();
    // sanity check
    assert vsys == toZone.getVsys()
        && externalVsys == externalToZone.getVsys()
        && vsys != externalVsys;
    Set<String> externalFromZoneInterfaces = externalFromZone.getInterfaceNames();
    if (externalFromZoneInterfaces.isEmpty()) {
      return Stream.of();
    }
    AclLineMatchExpr matchExternalFromZoneInterface = matchSrcInterface(externalFromZoneInterfaces);
    String externalVsysName = externalVsys.getName();
    String externalCrossZoneFilterName =
        zoneToZoneFilter(
            computeObjectName(externalVsysName, externalFromZone.getName()),
            computeObjectName(externalVsysName, externalToZone.getName()));
    String vsysName = vsys.getName();
    String crossZoneFilterName =
        zoneToZoneFilter(
            computeObjectName(vsysName, fromZone.getName()),
            computeObjectName(vsysName, toZone.getName()));
    // If the source interface is in externalFromZone and both vsys<=>external filters permit, then
    // permit.
    // Else if the source interface is in externalFromZone, one of the filters must have denied.
    return Stream.of(
        accepting(
            and(
                matchExternalFromZoneInterface,
                permittedByAcl(externalCrossZoneFilterName),
                permittedByAcl(crossZoneFilterName))),
        rejecting(matchExternalFromZoneInterface));
  }

  @Nullable
  private IpSpace ipSpaceFromRuleEndpoints(
      Collection<RuleEndpoint> endpoints, Vsys vsys, Warnings w) {
    return AclIpSpace.union(
        endpoints.stream()
            .map(source -> ruleEndpointToIpSpace(source, vsys, w))
            .collect(Collectors.toList()));
  }

  @Nonnull
  private RangeSet<Ip> ipRangeSetFromRuleEndpoints(
      Collection<RuleEndpoint> endpoints, Vsys vsys, Warnings w) {
    RangeSet<Ip> rangeSet = TreeRangeSet.create();
    endpoints.stream()
        .map(endpoint -> ruleEndpointToIpRangeSet(endpoint, vsys, w))
        .forEach(rangeSet::addAll);
    return ImmutableRangeSet.copyOf(rangeSet);
  }

  /** Convert specified firewall rule into an {@link IpAccessListLine}. */
  // Most of the conversion is fairly straight-forward: rules have actions, src and dest IP
  // constraints, and service (aka Protocol + Ports) constraints.
  //   However, services are a bit complicated when `service application-default` is used. In that
  //   case, we extract service definitions from the application that matches.
  private IpAccessListLine toIpAccessListLine(SecurityRule rule, Vsys vsys) {
    assert !rule.getDisabled(); // handled by caller.

    //////////////////////////////////////////////////////////////////////////////////////////
    // 1. Initialize the list of conditions.
    List<AclLineMatchExpr> conjuncts = new LinkedList<>();

    //////////////////////////////////////////////////////////////////////////////////////////
    // 2. Match SRC IPs if specified.
    IpSpace srcIps = ipSpaceFromRuleEndpoints(rule.getSource(), vsys, _w);
    if (srcIps != null) {
      AclLineMatchExpr match =
          new MatchHeaderSpace(HeaderSpace.builder().setSrcIps(srcIps).build());
      if (rule.getNegateSource()) {
        match = new NotMatchExpr(match);
      }
      conjuncts.add(match);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // 3. Match DST IPs if specified.
    IpSpace dstIps = ipSpaceFromRuleEndpoints(rule.getDestination(), vsys, _w);
    if (dstIps != null) {
      AclLineMatchExpr match =
          new MatchHeaderSpace(HeaderSpace.builder().setDstIps(dstIps).build());
      if (rule.getNegateDestination()) {
        match = new NotMatchExpr(match);
      }
      conjuncts.add(match);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // 4. Match services.
    getServiceExpr(rule, vsys).ifPresent(conjuncts::add);

    return IpAccessListLine.builder()
        .setName(rule.getName())
        .setAction(rule.getAction())
        .setMatchCondition(new AndMatchExpr(conjuncts))
        .build();
  }

  /**
   * Returns an expression describing the protocol/port combinations permitted by this rule, or
   * {@link Optional#empty()} if all are allowed.
   */
  private Optional<AclLineMatchExpr> getServiceExpr(SecurityRule rule, Vsys vsys) {
    SortedSet<ServiceOrServiceGroupReference> services = rule.getService();
    if (services.isEmpty()) {
      // No filtering.
      return Optional.empty();
    }

    List<AclLineMatchExpr> serviceDisjuncts = new LinkedList<>();
    for (ServiceOrServiceGroupReference service : services) {
      String serviceName = service.getName();

      // Check for matching object before using built-ins
      String vsysName = service.getVsysName(this, vsys);
      if (vsysName != null) {
        serviceDisjuncts.add(
            permittedByAcl(computeServiceGroupMemberAclName(vsysName, serviceName)));
      } else if (serviceName.equals(ServiceBuiltIn.ANY.getName())) {
        // Anything is allowed.
        return Optional.empty();
      } else if (serviceName.equals(ServiceBuiltIn.APPLICATION_DEFAULT.getName())) {
        if (rule.getAction() == LineAction.PERMIT) {
          // Since Batfish cannot currently match above L4, we follow Cisco-fragments-like logic:
          // When permitting an application, optimistically permit all traffic where the L4 rule
          // matches, assuming it is this application. But when blocking a specific application, do
          // not block all matching L4 traffic, since we can't know it is this specific application.
          serviceDisjuncts.addAll(matchApplications(rule, vsys));
        }
      } else if (serviceName.equals(ServiceBuiltIn.SERVICE_HTTP.getName())) {
        serviceDisjuncts.add(new MatchHeaderSpace(ServiceBuiltIn.SERVICE_HTTP.getHeaderSpace()));
      } else if (serviceName.equals(ServiceBuiltIn.SERVICE_HTTPS.getName())) {
        serviceDisjuncts.add(new MatchHeaderSpace(ServiceBuiltIn.SERVICE_HTTPS.getHeaderSpace()));
      } else {
        _w.redFlag(String.format("No matching service group/object found for: %s", serviceName));
      }
    }
    return Optional.of(new OrMatchExpr(serviceDisjuncts));
  }

  private List<AclLineMatchExpr> matchApplications(SecurityRule rule, Vsys vsys) {
    ImmutableList.Builder<AclLineMatchExpr> ret = ImmutableList.builder();
    Queue<String> applications = new LinkedBlockingQueue<>(rule.getApplications());
    while (!applications.isEmpty()) {
      String name = applications.remove();
      ApplicationGroup group = vsys.getApplicationGroups().get(name);
      if (group != null) {
        applications.addAll(
            group.getDescendantObjects(vsys.getApplications(), vsys.getApplicationGroups()));
        continue;
      }
      Application a = vsys.getApplications().get(name);
      if (a != null) {
        for (Service s : a.getServices()) {
          ret.add(s.toMatchHeaderSpace(_w));
        }
        continue;
      }
      Optional<Application> builtIn = ApplicationBuiltIn.getBuiltInApplication(name);
      if (builtIn.isPresent()) {
        builtIn.get().getServices().forEach(s -> ret.add(s.toMatchHeaderSpace(_w)));
        continue;
      }
      // Did not find in the right hierarchy, so stop and warn.
      _w.redFlag(
          String.format(
              "Unable to identify application %s in vsys %s rule %s",
              name, rule.getName(), vsys.getName()));
    }
    return ret.build();
  }

  /** Converts {@link RuleEndpoint} to {@code IpSpace} */
  @Nonnull
  @SuppressWarnings("fallthrough")
  private IpSpace ruleEndpointToIpSpace(RuleEndpoint endpoint, Vsys vsys, Warnings w) {
    String endpointValue = endpoint.getValue();
    // Palo Alto allows object references that look like IP addresses, ranges, etc.
    // Devices use objects over constants when possible, so, check to see if there is a matching
    // group or object regardless of the type of endpoint we're expecting.
    if (vsys.getAddressObjects().containsKey(endpointValue)) {
      return vsys.getAddressObjects().get(endpointValue).getIpSpace();
    }
    if (vsys.getAddressGroups().containsKey(endpoint.getValue())) {
      return vsys.getAddressGroups()
          .get(endpointValue)
          .getIpSpace(vsys.getAddressObjects(), vsys.getAddressGroups());
    }
    switch (vsys.getNamespaceType()) {
      case LEAF:
        if (_shared != null) {
          return ruleEndpointToIpSpace(endpoint, _shared, w);
        }
        // fall-through
      case SHARED:
        if (_panorama != null) {
          return ruleEndpointToIpSpace(endpoint, _panorama, w);
        }
        // fall-through
      default:
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
            // Rely on undefined references to surface this issue (endpoint reference not defined)
            return EmptyIpSpace.INSTANCE;
          default:
            w.redFlag("Could not convert RuleEndpoint to IpSpace: " + endpoint);
            return EmptyIpSpace.INSTANCE;
        }
    }
  }

  /** Converts {@link RuleEndpoint} to IP {@code RangeSet} */
  @Nonnull
  @SuppressWarnings("fallthrough")
  private RangeSet<Ip> ruleEndpointToIpRangeSet(RuleEndpoint endpoint, Vsys vsys, Warnings w) {
    String endpointValue = endpoint.getValue();
    // Palo Alto allows object references that look like IP addresses, ranges, etc.
    // Devices use objects over constants when possible, so, check to see if there is a matching
    // group or object regardless of the type of endpoint we're expecting.
    if (vsys.getAddressObjects().containsKey(endpointValue)) {
      return vsys.getAddressObjects().get(endpointValue).getAddressAsRangeSet();
    }
    if (vsys.getAddressGroups().containsKey(endpoint.getValue())) {
      return vsys.getAddressGroups()
          .get(endpointValue)
          .getIpRangeSet(vsys.getAddressObjects(), vsys.getAddressGroups());
    }
    switch (vsys.getNamespaceType()) {
      case LEAF:
        if (_shared != null) {
          return ruleEndpointToIpRangeSet(endpoint, _shared, w);
        }
        // fall-through
      case SHARED:
        if (_panorama != null) {
          return ruleEndpointToIpRangeSet(endpoint, _panorama, w);
        }
        // fall-through
      default:
        // No named object found matching this endpoint, so parse the endpoint value as is
        switch (endpoint.getType()) {
          case Any:
            return ImmutableRangeSet.of(Range.closed(Ip.ZERO, Ip.MAX));
          case IP_ADDRESS:
            return ImmutableRangeSet.of(Range.singleton(Ip.parse(endpointValue)));
          case IP_PREFIX:
            Prefix prefix = Prefix.parse(endpointValue);
            return ImmutableRangeSet.of(Range.closed(prefix.getStartIp(), prefix.getEndIp()));
          case IP_RANGE:
            String[] ips = endpointValue.split("-");
            return ImmutableRangeSet.of(Range.closed(Ip.parse(ips[0]), Ip.parse(ips[1])));
          case REFERENCE:
            // Rely on undefined references to surface this issue (endpoint reference not defined)
            return ImmutableRangeSet.of();
          default:
            w.redFlag("Could not convert RuleEndpoint to RangeSet: " + endpoint);
            return ImmutableRangeSet.of();
        }
    }
  }

  private static InterfaceType batfishInterfaceType(@Nonnull Interface.Type panType, Warnings w) {
    switch (panType) {
      case PHYSICAL:
        return InterfaceType.PHYSICAL;
      case LAYER2:
      case LAYER3:
        return InterfaceType.LOGICAL;
      case LOOPBACK:
        return InterfaceType.LOOPBACK;
      case TUNNEL:
        return InterfaceType.TUNNEL;
      case VLAN:
        return InterfaceType.VLAN;
      default:
        w.unimplemented("Unknown Palo Alto interface type " + panType);
        return InterfaceType.UNKNOWN;
    }
  }

  /** Convert Palo Alto specific interface into vendor independent model interface */
  private org.batfish.datamodel.Interface toInterface(Interface iface) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(name)
            .setOwner(_c)
            .setType(batfishInterfaceType(iface.getType(), _w))
            .build();
    Integer mtu = iface.getMtu();
    if (mtu != null) {
      newIface.setMtu(mtu);
    }
    newIface.setAddress(iface.getAddress());
    newIface.setAllAddresses(iface.getAllAddresses());
    newIface.setActive(iface.getActive());
    newIface.setDescription(iface.getComment());

    if (iface.getType() == Interface.Type.LAYER3) {
      newIface.setEncapsulationVlan(iface.getTag());
    } else if (iface.getType() == Interface.Type.LAYER2) {
      newIface.setAccessVlan(iface.getTag());
    }

    Zone zone = iface.getZone();
    // add outgoing transformation
    if (zone != null) {
      String vsysName = zone.getVsys().getName();
      newIface.setOutgoingTransformation(
          _zoneOutgoingTransformations
              .getOrDefault(vsysName, ImmutableMap.of())
              .get(zone.getName()));
    }
    // add outgoing filter
    IpAccessList.Builder aclBuilder =
        IpAccessList.builder().setOwner(_c).setName(computeOutgoingFilterName(iface.getName()));
    List<IpAccessListLine> aclLines = new ArrayList<>();
    Optional<Vsys> sharedGatewayOptional =
        _sharedGateways.values().stream()
            .filter(sg -> sg.getImportedInterfaces().contains(name))
            .findFirst();
    if (sharedGatewayOptional.isPresent()) {
      Vsys sharedGateway = sharedGatewayOptional.get();
      String sgName = sharedGateway.getName();
      aclLines.add(
          accepting(permittedByAcl(computeOutgoingFilterName(computeObjectName(sgName, sgName)))));
      newIface.setFirewallSessionInterfaceInfo(
          new FirewallSessionInterfaceInfo(
              true, sharedGateway.getImportedInterfaces(), null, null));
    } else if (zone != null) {
      newIface.setZoneName(zone.getName());
      if (zone.getType() == Type.LAYER3) {
        aclLines.add(
            accepting(
                permittedByAcl(
                    computeOutgoingFilterName(
                        computeObjectName(zone.getVsys().getName(), zone.getName())))));
        newIface.setFirewallSessionInterfaceInfo(
            new FirewallSessionInterfaceInfo(true, zone.getInterfaceNames(), null, null));
      }
    } else {
      // Do not allow any traffic to exit an unzoned interface
      aclLines.add(IpAccessListLine.rejecting("Not in a zone", TrueExpr.INSTANCE));
    }

    if (!aclLines.isEmpty()) {
      // For interfaces with security rules, assume traffic originating from the device is allowed
      // out
      // the interface
      // TODO this isn't tested and may not line up with actual device behavior, but is in place to
      // allow things like BGP sessions to come up
      aclLines.add(accepting().setMatchCondition(ORIGINATING_FROM_DEVICE).build());
      newIface.setOutgoingFilter(aclBuilder.setLines(ImmutableList.copyOf(aclLines)).build());
    }

    // If there is a NAT for this iface, apply it
    String packetPolicyName = generatePacketPolicyForIface(iface);
    if (packetPolicyName != null) {
      newIface.setRoutingPolicy(packetPolicyName);
    }

    return newIface;
  }

  /**
   * Generate packet policy for the specified interface, attach it to the VI config, and return the
   * name if applicable. If no packet policy is applicable, returns {@code null}
   */
  private @Nullable String generatePacketPolicyForIface(Interface iface) {
    Zone zone = iface.getZone();
    // Unzoned interfaces don't transmit traffic, so don't need packet policy
    if (zone == null) {
      return null;
    }
    Vsys vsys = zone.getVsys();
    List<NatRule> natRules = getNatRulesForIface(iface);
    if (!natRules.isEmpty()) {
      String packetPolicyName = computePacketPolicyName(iface.getName());
      _c.getPacketPolicies()
          .put(packetPolicyName, buildPacketPolicy(packetPolicyName, natRules, vsys));
      return packetPolicyName;
    }
    return null;
  }

  private MatchHeaderSpace getRuleMatchHeaderSpace(NatRule rule, Vsys vsys) {
    IpSpace srcIps = ipSpaceFromRuleEndpoints(rule.getSource(), vsys, _w);
    IpSpace dstIps = ipSpaceFromRuleEndpoints(rule.getDestination(), vsys, _w);
    return new MatchHeaderSpace(HeaderSpace.builder().setSrcIps(srcIps).setDstIps(dstIps).build());
  }

  /** Build a routing policy for the specified NAT rule + vsys entries in the specified vsys. */
  private PacketPolicy buildPacketPolicy(String name, List<NatRule> natRules, Vsys vsys) {
    ImmutableList.Builder<org.batfish.datamodel.packet_policy.Statement> lines =
        ImmutableList.builder();
    for (NatRule rule : natRules) {
      RuleEndpoint translatedAddress =
          Optional.ofNullable(rule.getDestinationTranslation())
              .map(DestinationTranslation::getTranslatedAddress)
              .orElse(null);
      Zone toZone = vsys.getZones().get(rule.getTo());
      if (translatedAddress == null || toZone == null) {
        continue;
      }

      // Conditions under which to apply dest NAT
      MatchHeaderSpace matchHeaderSpace = getRuleMatchHeaderSpace(rule, vsys);
      BoolExpr condition =
          Conjunction.of(
              new PacketMatchExpr(matchHeaderSpace),
              // Only apply dest NAT if flow is exiting an interface in the to-zone
              new FibLookupOutgoingInterfaceIsOneOf(
                  IngressInterfaceVrf.instance(), toZone.getInterfaceNames()));

      // Actual dest NAT transformation
      Transformation transform =
          new Transformation(
              // No need to guard since packet policy already encodes this rule's match conditions
              TrueExpr.INSTANCE,
              ImmutableList.of(
                  new AssignIpAddressFromPool(
                      TransformationType.DEST_NAT,
                      IpField.DESTINATION,
                      ruleEndpointToIpRangeSet(translatedAddress, vsys, _w))),
              null,
              null);

      lines.add(
          new org.batfish.datamodel.packet_policy.If(
              condition,
              ImmutableList.of(
                  new ApplyTransformation(transform),
                  new Return(new FibLookup(IngressInterfaceVrf.instance())))));
    }
    return new PacketPolicy(
        name, lines.build(), new Return(new FibLookup(IngressInterfaceVrf.instance())));
  }

  /** Return a list of all valid NAT rules associated with the specified from-interface. */
  private List<NatRule> getNatRulesForIface(Interface iface) {
    Zone zone = iface.getZone();
    if (zone == null) {
      return ImmutableList.of();
    }
    Vsys vsys = zone.getVsys();
    return getAllNatRules(vsys)
        .filter(
            rule ->
                checkNatRuleValid(rule, false)
                    && (rule.getFrom().contains(zone.getName())
                        || rule.getFrom().contains(CATCHALL_ZONE_NAME)))
        .collect(ImmutableList.toImmutableList());
  }

  private void convertPeerGroup(BgpPeerGroup pg, BgpVr bgp, BgpProcess proc, VirtualRouter vr) {
    if (!pg.getEnable()) {
      return;
    }

    pg.getPeers().forEach((peerName, peer) -> convertPeer(peer, pg, bgp, proc, vr));
  }

  private void convertPeer(
      BgpPeer peer, BgpPeerGroup pg, BgpVr bgp, BgpProcess proc, VirtualRouter vr) {
    if (!peer.getEnable()) {
      return;
    }

    if (peer.getPeerAddress() == null) {
      _w.redFlag("Missing peer-address for peer %s; disabling it", peer.getName());
      return;
    }

    assert bgp.getLocalAs() != null; // checked before this function is called.
    long localAs = bgp.getLocalAs();
    Long peerAs = peer.getPeerAs();

    if (pg.getTypeAndOptions() instanceof IbgpPeerGroupType) {
      peerAs = firstNonNull(peerAs, localAs);
      // Peer AS must be unset or equal to Local AS.
      if (localAs != peerAs) {
        _w.redFlag(
            String.format(
                "iBGP peer %s has a mismatched peer-as %s which is not the local-as %s; replacing it",
                peer.getName(), peerAs, localAs));
        peerAs = localAs;
      }
    } else if (pg.getTypeAndOptions() instanceof EbgpPeerGroupType) {
      // Peer AS must be set and not equal to Local AS.
      if (peerAs == null) {
        _w.redFlag(
            String.format("eBGP peer %s must have peer-as set; disabling it", peer.getName()));
        return;
      }
      if (peerAs == localAs) {
        _w.redFlag(
            String.format(
                "eBGP peer %s must have peer-as different from local-as; disabling it",
                peer.getName()));
        return;
      }
    } else {
      assert true; // TODO figure out the default and handle separately.
    }

    BgpActivePeerConfig.Builder peerB =
        BgpActivePeerConfig.builder()
            .setBgpProcess(proc)
            .setDescription(peer.getName())
            .setGroup(pg.getName())
            .setLocalAs(localAs)
            .setPeerAddress(peer.getPeerAddress())
            .setRemoteAs(peerAs);
    if (peer.getLocalAddress() != null) {
      peerB.setLocalIp(peer.getLocalAddress());
    } else {
      // Get the local address by choosing the IP on the specified interface.
      Optional.ofNullable(peer.getLocalInterface())
          .map(_interfaces::get)
          .map(Interface::getAddress)
          .map(ConcreteInterfaceAddress::getIp)
          .ifPresent(peerB::setLocalIp);
    }

    // TODO
    Builder ipv4af =
        Ipv4UnicastAddressFamily.builder()
            .setAddressFamilyCapabilities(AddressFamilyCapabilities.builder().build());

    ipv4af.setExportPolicy(
        computeAndSetPerPeerExportPolicy(peer, _c, vr, bgp, pg.getName()).getName());

    @Nullable
    RoutingPolicy importPolicyForThisPeer =
        computeAndSetPerPeerImportPolicy(peer, _c, vr, bgp, pg.getName());
    ipv4af.setImportPolicy(
        importPolicyForThisPeer == null ? null : importPolicyForThisPeer.getName());

    peerB.setIpv4UnicastAddressFamily(ipv4af.build());

    peerB.build(); // automatically adds itself to the process
  }

  private Optional<BgpProcess> toBgpProcess(VirtualRouter vr) {
    BgpVr bgp = vr.getBgp();
    if (bgp == null || !firstNonNull(bgp.getEnable(), Boolean.FALSE)) {
      return Optional.empty();
    }

    // Router ID must be configured manually or you cannot enable the router.
    if (bgp.getRouterId() == null) {
      _w.redFlag(
          String.format("virtual-router %s bgp has no router-id; disabling it", vr.getName()));
      return Optional.empty();
    }

    // Local AS must be configured manually or you cannot enable the router.
    if (bgp.getLocalAs() == null) {
      _w.redFlag(
          String.format("virtual-router %s bgp has no local-as; disabling it", vr.getName()));
      return Optional.empty();
    }

    BgpProcess proc =
        new BgpProcess(
            bgp.getRouterId(), vr.getAdminDists().getEbgp(), vr.getAdminDists().getIbgp());
    // common BGP export policy (combination of all redist rules at the BgpVr level)
    RoutingPolicy commonExportPolicy = getBgpCommonExportPolicy(bgp, vr, _w, _c);
    _c.getRoutingPolicies().put(commonExportPolicy.getName(), commonExportPolicy);

    bgp.getPeerGroups().forEach((name, pg) -> convertPeerGroup(pg, bgp, proc, vr));

    return Optional.of(proc);
  }

  private Optional<OspfProcess> toOspfProcess(VirtualRouter vr, Vrf vrf) {
    OspfVr ospf = vr.getOspf();
    if (ospf == null || !ospf.isEnable()) {
      return Optional.empty();
    }

    // Router ID is ensured to be present by the CLI/UI
    if (ospf.getRouterId() == null) {
      _w.redFlag(
          String.format("Virtual-router %s ospf has no router-id; disabling it.", vr.getName()));
      return Optional.empty();
    }
    OspfProcess.Builder ospfProcessBuilder = OspfProcess.builder();
    ospfProcessBuilder.setRouterId(ospf.getRouterId());
    String processId = String.format("~OSPF_PROCESS_%s", ospf.getRouterId());
    ospfProcessBuilder
        .setProcessId(processId)
        .setAreas(
            ospf.getAreas().values().stream()
                .map(area -> toOspfArea(area, vrf, processId))
                .collect(
                    ImmutableSortedMap.toImmutableSortedMap(
                        Comparator.naturalOrder(), OspfArea::getAreaNumber, Function.identity())));
    // Setting reference bandwidth to an arbitrary value to avoid builder crash
    ospfProcessBuilder.setReferenceBandwidth(1D);
    return Optional.of(ospfProcessBuilder.build());
  }

  private @Nonnull OspfArea toOspfArea(
      org.batfish.representation.palo_alto.OspfArea vsArea, Vrf vrf, String ospfProcessName) {
    OspfArea.Builder viAreaBuilder = OspfArea.builder().setNumber(vsArea.getAreaId().asLong());
    if (vsArea.getTypeSettings() != null) {
      vsArea
          .getTypeSettings()
          .accept(
              new OspfAreaTypeSettingsVisitor<Void>() {
                @Override
                public Void visitOspfAreaNssa(OspfAreaNssa ospfAreaNssa) {
                  assert ospfAreaNssa.getAcceptSummary()
                      != null; // Palo Alto always has explicit setting for this
                  viAreaBuilder.setNssa(
                      NssaSettings.builder()
                          .setDefaultOriginateType(
                              // PAN enforces either of these two values
                              ospfAreaNssa.getDefaultRouteType() == DefaultRouteType.EXT_1
                                  ? OspfDefaultOriginateType.EXTERNAL_TYPE1
                                  : OspfDefaultOriginateType.EXTERNAL_TYPE2)
                          .setSuppressType3(!ospfAreaNssa.getAcceptSummary())
                          .build());
                  return null;
                }

                @Override
                public Void visitOspfAreaStub(OspfAreaStub ospfAreaStub) {
                  assert ospfAreaStub.getAcceptSummary()
                      != null; // Palo Alto always has explicit setting for this
                  viAreaBuilder.setStub(
                      StubSettings.builder()
                          .setSuppressType3(!ospfAreaStub.getAcceptSummary())
                          .build());
                  return null;
                }

                @Override
                public Void visitOspfAreaNormal(OspfAreaNormal ospfAreaNormal) {
                  return null;
                }
              });
    }
    viAreaBuilder.setInterfaces(computeAreaInterfaces(vrf, vsArea, ospfProcessName));
    return viAreaBuilder.build();
  }

  private @Nonnull Set<String> computeAreaInterfaces(
      Vrf vrf, org.batfish.representation.palo_alto.OspfArea vsArea, String ospfProcessName) {
    ImmutableSet.Builder<String> ospfIfaceNames = ImmutableSet.builder();
    Ip vsAreaId = vsArea.getAreaId();
    Map<String, org.batfish.datamodel.Interface> viInterfaces = vrf.getInterfaces();
    vsArea
        .getInterfaces()
        .values()
        .forEach(
            ospfVsIface -> {
              org.batfish.datamodel.Interface viIface = viInterfaces.get(ospfVsIface.getName());
              if (viIface == null) {
                _w.redFlag(
                    String.format(
                        "OSPF area %s refers a non-existent interface %s",
                        vsAreaId, ospfVsIface.getName()));
                return;
              }
              ospfIfaceNames.add(viIface.getName());
              finalizeInterfaceOspfSettings(
                  viIface, vsAreaId.asLong(), ospfProcessName, ospfVsIface);
            });
    return ospfIfaceNames.build();
  }

  private void finalizeInterfaceOspfSettings(
      org.batfish.datamodel.Interface viIface,
      long areaId,
      String processName,
      OspfInterface vsOspfIface) {
    // (enable = yes or no)  and (passive = yes or no should be explicitly configured
    assert vsOspfIface.getEnable() != null;
    assert vsOspfIface.getPassive() != null;
    OspfInterfaceSettings.Builder ospfSettings = OspfInterfaceSettings.builder();
    ospfSettings.setCost(vsOspfIface.getMetric());
    ospfSettings.setPassive(vsOspfIface.getPassive());
    ospfSettings.setEnabled(vsOspfIface.getEnable());
    ospfSettings.setAreaName(areaId);
    ospfSettings.setProcess(processName);
    ospfSettings.setPassive(vsOspfIface.getPassive());
    OspfNetworkType networkType = toNetworkType(vsOspfIface.getLinkType());
    ospfSettings.setNetworkType(networkType);
    if (vsOspfIface.getMetric() == null
        && viIface.isLoopback()
        && networkType != org.batfish.datamodel.ospf.OspfNetworkType.POINT_TO_POINT) {
      ospfSettings.setCost(DEFAULT_LOOPBACK_OSPF_COST);
    }
    ospfSettings.setHelloInterval(vsOspfIface.getHelloInterval());
    ospfSettings.setDeadInterval(vsOspfIface.getHelloInterval() * vsOspfIface.getDeadCounts());
    viIface.setOspfSettings(ospfSettings.build());
  }

  @Nullable
  private OspfNetworkType toNetworkType(@Nullable LinkType linkType) {
    if (linkType == null) {
      return null;
    }
    if (linkType == LinkType.BROADCAST) {
      return OspfNetworkType.BROADCAST;
    } else if (linkType == LinkType.P2P) {
      return OspfNetworkType.POINT_TO_POINT;
    } else if (linkType == LinkType.P2MP) {
      return OspfNetworkType.POINT_TO_MULTIPOINT;
    } else {
      return null;
    }
  }

  /** Convert Palo Alto specific virtual router into vendor independent model Vrf */
  private Vrf toVrf(VirtualRouter vr) {
    String vrfName = vr.getName();
    Vrf vrf = new Vrf(vrfName);

    // Static routes
    for (Entry<String, StaticRoute> e : vr.getStaticRoutes().entrySet()) {
      StaticRoute sr = e.getValue();
      // Can only construct a static route if it has a destination
      Prefix destination = sr.getDestination();
      if (destination == null) {
        _w.redFlag(
            String.format(
                "Cannot convert static route %s, as it does not have a destination.", e.getKey()));
        continue;
      }
      String nextVrf = sr.getNextVr();
      if (nextVrf != null) {
        if (nextVrf.equals(vrfName)) {
          _w.redFlag(
              String.format(
                  "Cannot convert static route %s, as its next-vr '%s' is its own virtual-router.",
                  e.getKey(), nextVrf));
          continue;
        }
        if (!_virtualRouters.containsKey(nextVrf)) {
          _w.redFlag(
              String.format(
                  "Cannot convert static route %s, as its next-vr '%s' is not a virtual-router.",
                  e.getKey(), nextVrf));
          continue;
        }
      }
      vrf.getStaticRoutes()
          .add(
              org.batfish.datamodel.StaticRoute.builder()
                  .setNextHopInterface(sr.getNextHopInterface())
                  .setNextHopIp(sr.getNextHopIp())
                  .setAdministrativeCost(sr.getAdminDistance())
                  .setMetric(sr.getMetric())
                  .setNetwork(destination)
                  .setNextVrf(nextVrf)
                  .build());
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

    // BGP
    toBgpProcess(vr).ifPresent(vrf::setBgpProcess);
    // OSPF
    toOspfProcess(vr, vrf).ifPresent(vrf::addOspfProcess);

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
    Stream.concat(_virtualSystems.values().stream(), _sharedGateways.values().stream())
        .forEach(
            zoneContainer -> {
              for (Zone zone : zoneContainer.getZones().values()) {
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
            });
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

    convertNamespaces();

    // Handle converting items within virtual systems
    convertVirtualSystems();

    convertSharedGateways();

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
        if (iface.getDependencies().stream().anyMatch(d -> d.getType() == DependencyType.BIND)) {
          // This is a child interface. Just shut it down.
          iface.setActive(false);
          _w.redFlag(
              String.format(
                  "Interface %s is not in a virtual-router, placing in %s and shutting it down.",
                  iface.getName(), nullVrf.getName()));
        } else {
          // This is a parent interface. We can't shut it down, so instead we must just clear L2/L3
          // data.
          boolean warn = false;
          if (iface.getAccessVlan() != null) {
            warn = true;
            iface.setAccessVlan(null);
          }
          if (iface.getAddress() != null) {
            warn = true;
            iface.setAddress(null);
          }
          if (!iface.getAllAddresses().isEmpty()) {
            warn = true;
            iface.setAllAddresses(ImmutableSortedSet.of());
          }
          if (!iface.getAllowedVlans().isEmpty()) {
            warn = true;
            iface.setAllowedVlans(IntegerSpace.EMPTY);
          }
          if (iface.getSwitchportMode() != SwitchportMode.NONE) {
            warn = true;
            iface.setSwitchportMode(SwitchportMode.NONE);
          }
          // Only warn if some L2/L3 data actually set.
          if (warn) {
            _w.redFlag(
                String.format(
                    "Interface %s is not in a virtual-router, placing in %s and clearing L2/L3 data.",
                    iface.getName(), nullVrf.getName()));
          }
        }
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
    markConcreteStructure(PaloAltoStructureType.INTERFACE);
    markConcreteStructure(PaloAltoStructureType.REDIST_PROFILE);
    markConcreteStructure(PaloAltoStructureType.SECURITY_RULE);
    markConcreteStructure(PaloAltoStructureType.ZONE);
    markConcreteStructure(PaloAltoStructureType.VIRTUAL_ROUTER);

    // Handle marking for structures that may exist in one of a couple namespaces
    // Handle application objects/groups that may overlap with built-in names
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.APPLICATION_GROUP_OR_APPLICATION_OR_NONE,
        ImmutableList.of(
            PaloAltoStructureType.APPLICATION, PaloAltoStructureType.APPLICATION_GROUP),
        true,
        PaloAltoStructureUsage.APPLICATION_GROUP_MEMBERS,
        PaloAltoStructureUsage.SECURITY_RULE_APPLICATION);

    // Handle service objects/groups that may overlap with built-in names
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP_OR_NONE,
        ImmutableList.of(PaloAltoStructureType.SERVICE, PaloAltoStructureType.SERVICE_GROUP),
        true,
        PaloAltoStructureUsage.SERVICE_GROUP_MEMBER,
        PaloAltoStructureUsage.SECURITY_RULE_SERVICE);
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP,
        ImmutableList.of(PaloAltoStructureType.SERVICE, PaloAltoStructureType.SERVICE_GROUP),
        PaloAltoStructureUsage.SERVICE_GROUP_MEMBER,
        PaloAltoStructureUsage.SECURITY_RULE_SERVICE);

    // Handle marking rule endpoints
    // First, handle those which may or may not be referencing objects (e.g. "1.2.3.4" may be IP
    // address or a named object)
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.ADDRESS_LIKE_OR_NONE,
        ImmutableList.of(
            PaloAltoStructureType.ADDRESS_GROUP,
            PaloAltoStructureType.ADDRESS_OBJECT,
            PaloAltoStructureType.EXTERNAL_LIST),
        true,
        PaloAltoStructureUsage.SECURITY_RULE_DESTINATION,
        PaloAltoStructureUsage.SECURITY_RULE_SOURCE);
    // Next, handle address object references which are definitely referencing objects
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.ADDRESS_LIKE,
        ImmutableList.of(
            PaloAltoStructureType.ADDRESS_GROUP,
            PaloAltoStructureType.ADDRESS_OBJECT,
            PaloAltoStructureType.EXTERNAL_LIST),
        PaloAltoStructureUsage.ADDRESS_GROUP_STATIC,
        PaloAltoStructureUsage.SECURITY_RULE_DESTINATION,
        PaloAltoStructureUsage.SECURITY_RULE_SOURCE);

    // Applications or Application-Groups
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.APPLICATION_GROUP_OR_APPLICATION,
        ImmutableList.of(
            PaloAltoStructureType.APPLICATION_GROUP, PaloAltoStructureType.APPLICATION),
        PaloAltoStructureUsage.APPLICATION_GROUP_MEMBERS,
        PaloAltoStructureUsage.SECURITY_RULE_APPLICATION);

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
              info.setNumReferrers(info.getNumReferrers() + lines.size());
            } else if (!ignoreUndefined) {
              for (int line : lines) {
                undefined(type, name, usage, line);
              }
            }
          });
    }
  }

  public @Nullable Vsys getPanorama() {
    return _panorama;
  }

  public @Nullable Vsys getShared() {
    return _shared;
  }

  public void setPanorama(@Nullable Vsys panorama) {
    _panorama = panorama;
  }

  public void setShared(@Nullable Vsys shared) {
    _shared = shared;
  }
}

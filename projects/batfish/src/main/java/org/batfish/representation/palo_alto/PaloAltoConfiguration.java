package org.batfish.representation.palo_alto;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.datamodel.ExprAclLine.accepting;
import static org.batfish.datamodel.ExprAclLine.rejecting;
import static org.batfish.datamodel.Names.zoneToZoneFilter;
import static org.batfish.datamodel.acl.AclLineMatchExprs.ORIGINATING_FROM_DEVICE;
import static org.batfish.datamodel.acl.AclLineMatchExprs.and;
import static org.batfish.datamodel.acl.AclLineMatchExprs.deniedByAcl;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.datamodel.acl.AclLineMatchExprs.permittedByAcl;
import static org.batfish.representation.palo_alto.Conversions.computeAndSetPerPeerExportPolicy;
import static org.batfish.representation.palo_alto.Conversions.computeAndSetPerPeerImportPolicy;
import static org.batfish.representation.palo_alto.Conversions.getBgpCommonExportPolicy;
import static org.batfish.representation.palo_alto.OspfVr.DEFAULT_LOOPBACK_OSPF_COST;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_GROUP;
import static org.batfish.representation.palo_alto.PaloAltoStructureType.ADDRESS_OBJECT;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.emptyZoneRejectTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.ifaceOutgoingTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.intrazoneDefaultAcceptTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchAddressAnyTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchAddressGroupTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchAddressObjectTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchAddressValueTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchApplicationAnyTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchApplicationGroupTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchApplicationObjectTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchBuiltInApplicationTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchDestinationAddressTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchNegatedAddressTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchServiceApplicationDefaultTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchServiceTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.matchSourceAddressTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.originatedFromDeviceTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.unzonedIfaceRejectTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.zoneToZoneMatchTraceElement;
import static org.batfish.representation.palo_alto.PaloAltoTraceElementCreators.zoneToZoneRejectTraceElement;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.VendorConversionException;
import org.batfish.common.Warnings;
import org.batfish.common.runtime.InterfaceRuntimeData;
import org.batfish.datamodel.AclAclLine;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.datamodel.DeviceModel;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.ExprAclLine;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpRange;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceMetadata;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.TraceElement;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.NotMatchExpr;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.bgp.AddressFamilyCapabilities;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily.Builder;
import org.batfish.datamodel.collections.InsertOrderedMap;
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
import org.batfish.datamodel.packet_policy.Statement;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.AssignPortFromPool;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.PortField;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationStep;
import org.batfish.representation.palo_alto.OspfAreaNssa.DefaultRouteType;
import org.batfish.representation.palo_alto.OspfInterface.LinkType;
import org.batfish.representation.palo_alto.SecurityRule.RuleType;
import org.batfish.representation.palo_alto.Vsys.NamespaceType;
import org.batfish.representation.palo_alto.Zone.Type;
import org.batfish.vendor.StructureUsage;
import org.batfish.vendor.VendorConfiguration;

public class PaloAltoConfiguration extends VendorConfiguration {

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

  /** Device groups owned by this configuration. */
  private final Map<String, DeviceGroup> _deviceGroups;

  private String _dnsServerPrimary;

  private String _dnsServerSecondary;

  private String _hostname;

  /**
   * Map of device id to hostname. This represents hostname mapping extracted from Panorama `show
   * devices` commands.
   */
  private final Map<String, String> _hostnameMap;

  private final SortedMap<String, Interface> _interfaces;

  private Ip _mgmtIfaceAddress;

  private Ip _mgmtIfaceGateway;

  private Ip _mgmtIfaceNetmask;

  private String _ntpServerPrimary;

  private String _ntpServerSecondary;

  private @Nullable Vsys _panorama;

  private @Nullable Vsys _shared;

  private final SortedMap<String, Vsys> _sharedGateways;

  /** Templates owned by this configuration */
  private final Map<String, Template> _templates;

  /** Template Stacks owned by this configuration */
  private final Map<String, TemplateStack> _templateStacks;

  private ConfigurationFormat _vendor;

  private final SortedMap<String, VirtualRouter> _virtualRouters;

  private final SortedMap<String, Vsys> _virtualSystems;

  /**
   * Temporary map for translating SecurityRules in each Vsys into their corresponding ExprAclLine,
   * used in conversion. Maps Vsys name to map of rule name to ExprAclLine.
   */
  private transient Map<String, Map<String, ExprAclLine>> _securityRuleToExprAclLine;

  /**
   * Temporary map for translating NatRules in each Vsys into their corresponding Transformation,
   * used in conversion. Maps Vsys name to map of rule name to Transformation.
   */
  private transient Map<String, Map<String, Transformation>> _natRuleToTransformation;

  /**
   * Temporary map for translating NatRules in each Vsys into their corresponding HeaderSpace
   * BoolExprs, used in conversion. Maps Vsys name to map of rule name to List of BoolExpr.
   */
  private transient Map<String, Map<String, List<BoolExpr>>> _natRuleToHeaderSpaceBoolExprs;

  public PaloAltoConfiguration() {
    _cryptoProfiles = new LinkedList<>();
    _deviceGroups = new TreeMap<>();
    _interfaces = new TreeMap<>();
    _hostnameMap = new HashMap<>();
    _sharedGateways = new TreeMap<>();
    _templates = new TreeMap<>();
    _templateStacks = new HashMap<>();
    _virtualRouters = new TreeMap<>();
    _virtualSystems = new TreeMap<>();
  }

  /** Add mapping from specified device id to specified hostname. */
  public void addHostnameMapping(String deviceId, String hostname) {
    _hostnameMap.put(deviceId, hostname);
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

  @Nullable
  String getDnsServerPrimary() {
    return _dnsServerPrimary;
  }

  @Nullable
  String getDnsServerSecondary() {
    return _dnsServerSecondary;
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

  public @Nullable DeviceGroup getDeviceGroup(String name) {
    return _deviceGroups.get(name);
  }

  public DeviceGroup getOrCreateDeviceGroup(String name) {
    return _deviceGroups.computeIfAbsent(name, DeviceGroup::new);
  }

  public Template getOrCreateTemplate(String name) {
    return _templates.computeIfAbsent(name, Template::new);
  }

  public TemplateStack getOrCreateTemplateStack(String name) {
    return _templateStacks.computeIfAbsent(name, TemplateStack::new);
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

  @Nullable
  String getNtpServerPrimary() {
    return _ntpServerPrimary;
  }

  @Nullable
  String getNtpServerSecondary() {
    return _ntpServerSecondary;
  }

  public @Nonnull SortedMap<String, Vsys> getSharedGateways() {
    return _sharedGateways;
  }

  public @Nullable Template getTemplate(String name) {
    return _templates.get(name);
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

  public static String computeObjectName(@Nullable Vsys vsys, String objectName) {
    return (vsys != null) ? computeObjectName(vsys.getName(), objectName) : objectName;
  }

  /** Generate egress IpAccessList name given an interface or zone name */
  public static String computeOutgoingFilterName(String interfaceOrZoneName) {
    return String.format("~%s~OUTGOING_FILTER~", interfaceOrZoneName);
  }

  /** Generate PacketPolicy name using the given zone's name and vsys name */
  public static String computePacketPolicyName(Zone zone) {
    return String.format("~%s~%s~PACKET_POLICY~", zone.getVsys().getName(), zone.getName());
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

  /**
   * Build maps of converted nat rule for each Vsys. Populates transient maps of Vsys name->NatRule
   * name->Transformation and HeaderSpace BoolExprs.
   */
  private void convertNatRules() {
    ImmutableMap.Builder<String, Map<String, Transformation>> vsysToRuleToTransformation =
        new ImmutableMap.Builder<>();
    ImmutableMap.Builder<String, Map<String, List<BoolExpr>>> vsysToRuleToBoolExprs =
        new ImmutableMap.Builder<>();

    Vsys panorama = this.getPanorama();
    Map<String, Transformation> panoramaTransformations =
        panorama == null ? ImmutableMap.of() : convertVsysNatRulesToTransformation(panorama);
    Map<String, List<BoolExpr>> panoramaHeaderSpaceBoolExprs =
        panorama == null ? ImmutableMap.of() : convertVsysNatRulesToHeaderSpaceBoolExprs(panorama);

    for (Entry<String, Vsys> entry : this.getVirtualSystems().entrySet()) {
      String vsysName = entry.getKey();

      // Transformations
      Map<String, Transformation> ruleToTransformation = new HashMap<>();
      Map<String, Transformation> vsysTransformations =
          convertVsysNatRulesToTransformation(entry.getValue());
      // Combine all relevant rules for this Vsys
      ruleToTransformation.putAll(panoramaTransformations);
      ruleToTransformation.putAll(vsysTransformations);
      vsysToRuleToTransformation.put(vsysName, ImmutableMap.copyOf(ruleToTransformation));

      // HeaderSpace BoolExprs
      Map<String, List<BoolExpr>> ruleToHeaderSpaceBoolExpr = new HashMap<>();
      Map<String, List<BoolExpr>> vsysHeaderSpaceBoolExprs =
          convertVsysNatRulesToHeaderSpaceBoolExprs(entry.getValue());
      // Combine all relevant rules for this Vsys
      ruleToHeaderSpaceBoolExpr.putAll(panoramaHeaderSpaceBoolExprs);
      ruleToHeaderSpaceBoolExpr.putAll(vsysHeaderSpaceBoolExprs);
      vsysToRuleToBoolExprs.put(vsysName, ImmutableMap.copyOf(ruleToHeaderSpaceBoolExpr));
    }

    _natRuleToHeaderSpaceBoolExprs = vsysToRuleToBoolExprs.build();
    _natRuleToTransformation = vsysToRuleToTransformation.build();
  }

  /** Convert NatRules in specified Vsys into a map of rule name to HeaderSpace BoolExprs. */
  private Map<String, List<BoolExpr>> convertVsysNatRulesToHeaderSpaceBoolExprs(Vsys vsys) {
    Map<String, List<BoolExpr>> ruleToBoolExprs = new HashMap<>();
    addNatRulesToHeaderSpaceBoolExprsMap(vsys.getPreRulebase(), vsys, ruleToBoolExprs);
    addNatRulesToHeaderSpaceBoolExprsMap(vsys.getRulebase(), vsys, ruleToBoolExprs);
    addNatRulesToHeaderSpaceBoolExprsMap(vsys.getPostRulebase(), vsys, ruleToBoolExprs);
    return ruleToBoolExprs;
  }

  /**
   * Helper to add NatRules from specified Rulebase into specified map of rule name to List of
   * HeaderSpace BoolExprs.
   */
  private void addNatRulesToHeaderSpaceBoolExprsMap(
      Rulebase rulebase, Vsys vsys, Map<String, List<BoolExpr>> ruleToHeaderSpaceBoolExprs) {
    for (Entry<String, NatRule> entry : rulebase.getNatRules().entrySet()) {
      String name = entry.getKey();
      NatRule rule = entry.getValue();
      if (!rule.getDisabled() && !ruleToHeaderSpaceBoolExprs.containsKey(name)) {
        ruleToHeaderSpaceBoolExprs.put(name, buildNatRuleHeaderSpaceBoolExprs(rule, vsys));
      }
    }
  }

  /**
   * Get List of HeaderSpace BoolExprs for specified NatRule in specified Vsys. If no HeaderSpace
   * BoolExprs are found {@link Optional#empty()} is returned instead.
   */
  private Optional<List<BoolExpr>> getNatHeaderSpaceBoolExprs(String vsysName, String ruleName) {
    return Optional.ofNullable(
        _natRuleToHeaderSpaceBoolExprs.getOrDefault(vsysName, ImmutableMap.of()).get(ruleName));
  }

  /** Convert NatRules in specified Vsys into a map of rule name to Transformation. */
  private Map<String, Transformation> convertVsysNatRulesToTransformation(Vsys vsys) {
    Map<String, Transformation> ruleToTransformation = new HashMap<>();
    addNatRulesToTransformationMap(vsys.getPreRulebase(), vsys, ruleToTransformation);
    addNatRulesToTransformationMap(vsys.getRulebase(), vsys, ruleToTransformation);
    addNatRulesToTransformationMap(vsys.getPostRulebase(), vsys, ruleToTransformation);
    return ruleToTransformation;
  }

  /**
   * Helper to add NatRules from specified Rulebase into specified map of rule name to
   * Transformation.
   */
  private void addNatRulesToTransformationMap(
      Rulebase rulebase, Vsys vsys, Map<String, Transformation> ruleToTransformation) {
    for (Entry<String, NatRule> entry : rulebase.getNatRules().entrySet()) {
      String name = entry.getKey();
      NatRule rule = entry.getValue();
      if (!rule.getDisabled() && !ruleToTransformation.containsKey(name)) {
        convertRuleToTransformation(rule, vsys).ifPresent(t -> ruleToTransformation.put(name, t));
      }
    }
  }

  /**
   * Get Transformation for specified NatRule in specified Vsys. If no Transformation is found
   * {@link Optional#empty()} is returned instead.
   */
  private Optional<Transformation> getNatTransformation(String vsysName, String ruleName) {
    return Optional.ofNullable(
        _natRuleToTransformation.getOrDefault(vsysName, ImmutableMap.of()).get(ruleName));
  }

  /**
   * Build map of converted security rule for each Vsys. Populates transient map of Vsys name to map
   * of SecurityRule name to corresponding ExprAclLine.
   */
  private void convertSecurityRules() {
    ImmutableMap.Builder<String, Map<String, ExprAclLine>> vsysToRuleToExprAclLine =
        new ImmutableMap.Builder<>();

    Vsys panorama = this.getPanorama();
    Map<String, ExprAclLine> panoramaRules =
        panorama == null ? ImmutableMap.of() : convertVsysSecurityRules(panorama);

    for (Entry<String, Vsys> entry : this.getVirtualSystems().entrySet()) {
      Map<String, ExprAclLine> ruleToExprAclLine = new HashMap<>();
      Map<String, ExprAclLine> vsysRules = convertVsysSecurityRules(entry.getValue());
      String vsysName = entry.getKey();

      // Combine all relevant rules for this Vsys
      ruleToExprAclLine.putAll(panoramaRules);
      ruleToExprAclLine.putAll(vsysRules);
      vsysToRuleToExprAclLine.put(vsysName, ImmutableMap.copyOf(ruleToExprAclLine));
    }

    _securityRuleToExprAclLine = vsysToRuleToExprAclLine.build();
  }

  /** Convert SecurityRules in specified Vsys into a map of rule name to ExprAclLine. */
  private Map<String, ExprAclLine> convertVsysSecurityRules(Vsys vsys) {
    Map<String, ExprAclLine> ruleToExprAclLine = new HashMap<>();
    addSecurityRulesToMap(vsys.getPreRulebase(), vsys, ruleToExprAclLine);
    addSecurityRulesToMap(vsys.getRulebase(), vsys, ruleToExprAclLine);
    addSecurityRulesToMap(vsys.getPostRulebase(), vsys, ruleToExprAclLine);
    return ruleToExprAclLine;
  }

  /**
   * Helper to add SecurityRules from specified Rulebase into specified map of rule name to
   * ExprAclLine.
   */
  private void addSecurityRulesToMap(
      Rulebase rulebase, Vsys vsys, Map<String, ExprAclLine> ruleToExprAclLine) {
    for (Entry<String, SecurityRule> entry : rulebase.getSecurityRules().entrySet()) {
      String name = entry.getKey();
      SecurityRule rule = entry.getValue();
      if (!rule.getDisabled() && !ruleToExprAclLine.containsKey(name)) {
        ruleToExprAclLine.put(name, toIpAccessListLine(rule, vsys));
      }
    }
  }

  /** Convert vsys components to vendor independent model */
  private void convertVirtualSystems() {
    for (Vsys vsys : _virtualSystems.values()) {
      // Generate warnings for invalid NAT rules in this vsys
      getAllNatRules(vsys).forEach(r -> checkNatRuleValid(r, true));

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
      List<Map.Entry<SecurityRule, Vsys>> rules = getAllValidSecurityRules(vsys);
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
    String vsysName = fromZone.getVsys().getName();

    String crossZoneFilterName =
        zoneToZoneFilter(
            computeObjectName(fromZone.getVsys().getName(), fromZone.getName()),
            computeObjectName(toZone.getVsys().getName(), toZone.getName()));

    boolean fromZoneEmpty =
        fromZone.getType() != Type.EXTERNAL && fromZone.getInterfaceNames().isEmpty();
    boolean toZoneEmpty = toZone.getType() != Type.EXTERNAL && toZone.getInterfaceNames().isEmpty();
    if (fromZoneEmpty || toZoneEmpty) {
      return IpAccessList.builder()
          .setName(crossZoneFilterName)
          .setLines(
              ImmutableList.of(
                  ExprAclLine.REJECT_ALL.toBuilder()
                      .setName("No interfaces in zone")
                      .setTraceElement(
                          emptyZoneRejectTraceElement(
                              vsysName, (fromZoneEmpty ? fromZone : toZone).getName()))
                      .build()))
          .build();
    }

    // Build an ACL Line for each rule that is enabled and applies to this from/to zone pair.
    List<AclLine> lines =
        rules.stream()
            .filter(e -> securityRuleApplies(fromZone.getName(), toZone.getName(), e.getKey(), _w))
            .map(entry -> _securityRuleToExprAclLine.get(vsysName).get(entry.getKey().getName()))
            .collect(ImmutableList.toImmutableList());
    // Intrazone traffic is allowed by default.
    if (fromZone == toZone) {
      lines =
          ImmutableList.<AclLine>builder()
              .addAll(lines)
              .add(
                  new ExprAclLine(
                      LineAction.PERMIT,
                      TrueExpr.INSTANCE,
                      "Accept intrazone by default",
                      intrazoneDefaultAcceptTraceElement(vsysName, fromZone.getName())))
              .build();
    }

    // Create a new ACL with a vsys-specific name.
    return IpAccessList.builder().setName(crossZoneFilterName).setLines(lines).build();
  }

  @VisibleForTesting
  static boolean securityRuleApplies(
      String fromZoneName, String toZoneName, SecurityRule rule, Warnings warnings) {
    if (rule.getDisabled()) {
      return false;
    }
    boolean fromZoneInRuleFrom =
        !Sets.intersection(rule.getFrom(), ImmutableSet.of(fromZoneName, CATCHALL_ZONE_NAME))
            .isEmpty();
    boolean toZoneInRuleTo =
        !Sets.intersection(rule.getTo(), ImmutableSet.of(toZoneName, CATCHALL_ZONE_NAME)).isEmpty();

    if (!fromZoneInRuleFrom || !toZoneInRuleTo) {
      return false;
    }

    // rule-type doc:
    // https://knowledgebase.paloaltonetworks.com/KCSArticleDetail?id=kA10g000000ClomCAC
    switch (firstNonNull(rule.getRuleType(), RuleType.UNIVERSAL)) {
      case INTRAZONE:
        return fromZoneName.equals(toZoneName);
      case INTERZONE:
        return !fromZoneName.equals(toZoneName);
      case UNIVERSAL:
        return true;
      default:
        warnings.redFlag(
            String.format(
                "Skipped unhandled rule type '%s' from zone %s to %s",
                rule.getRuleType(), fromZoneName, toZoneName));
        return false;
    }
  }

  /**
   * Collects the security rules from this Vsys and merges the common pre-/post-rulebases from
   * Panorama. Filters out invalid intrazone rules.
   */
  @SuppressWarnings("PMD.CloseResource") // PMD has a bug for this pattern.
  private List<Map.Entry<SecurityRule, Vsys>> getAllValidSecurityRules(Vsys vsys) {
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

    return Stream.concat(Stream.concat(pre, rules), post)
        .filter(e -> checkIntrazoneValidityAndWarn(e.getKey(), _w))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Check if the intrazone security rule is valid, and log a warning if it is not. Returns true for
   * non-intrazone rules.
   */
  @VisibleForTesting
  static boolean checkIntrazoneValidityAndWarn(SecurityRule rule, Warnings w) {
    if (rule.getRuleType() == RuleType.INTRAZONE && !rule.getFrom().equals(rule.getTo())) {
      w.redFlag(
          String.format(
              "Skipping invalid intrazone security rule: %s. It has different From and To zones:"
                  + " %s vs %s",
              rule.getName(), rule.getFrom(), rule.getTo()));
      return false;
    }
    return true;
  }

  /**
   * Collects the NAT rules from this Vsys and merges the common pre-/post-rulebases from Panorama.
   */
  @SuppressWarnings("PMD.CloseResource") // PMD has a bug for this pattern.
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
   * Generate {@link IpAccessList} to be used as {@link
   * org.batfish.datamodel.Interface#getOutgoingOriginalFlowFilter() outgoingOriginalFlowFilter} by
   * interfaces in layer-3 zone {@code toZone}, given supplied definitions for all {@code
   * sharedGateways} and {@code virtualSystems}.
   */
  @VisibleForTesting
  static @Nonnull IpAccessList generateOutgoingFilter(
      Zone toZone, Collection<Vsys> sharedGateways, Collection<Vsys> virtualSystems) {
    Vsys vsys = toZone.getVsys();
    List<AclLine> lines =
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
    Stream<ExprAclLine> vsysSgLines =
        virtualSystems.stream()
            .flatMap(vsys -> generateVsysSharedGatewayCalls(sharedGateway, vsys));
    Stream<ExprAclLine> sgSgLines =
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
  static @Nonnull Stream<ExprAclLine> generateSgSgLines(
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
  static @Nonnull Stream<ExprAclLine> generateVsysSharedGatewayCalls(
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
  static @Nonnull Stream<ExprAclLine> generateCrossZoneCalls(
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
  static @Nonnull Stream<ExprAclLine> generateCrossZoneCallsFromLayer3(Zone fromZone, Zone toZone) {
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
        ExprAclLine.builder()
            .accepting()
            .setMatchCondition(and(matchFromZoneInterface, permittedByAcl(crossZoneFilterName)))
            .setTraceElement(
                zoneToZoneMatchTraceElement(fromZone.getName(), toZone.getName(), vsysName))
            .build(),
        ExprAclLine.builder()
            .rejecting()
            // DeniedByAcl is guaranteed to match if reached, but including it in the line allows
            // traces to include any trace generated by the cross zone filter (e.g. rejecting rule).
            .setMatchCondition(and(matchFromZoneInterface, deniedByAcl(crossZoneFilterName)))
            .setTraceElement(
                zoneToZoneRejectTraceElement(fromZone.getName(), toZone.getName(), vsysName))
            .build());
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
  static @Nonnull Stream<ExprAclLine> generateCrossZoneCallsFromExternal(
      Zone fromZone,
      Zone toZone,
      Collection<Vsys> sharedGateways,
      Collection<Vsys> virtualSystems) {
    Vsys vsys = fromZone.getVsys();
    assert fromZone.getVsys() == toZone.getVsys(); // sanity check
    Stream<ExprAclLine> vsysLines =
        virtualSystems.stream()
            .filter(not(equalTo(vsys)))
            .filter(externalVsys -> fromZone.getExternalNames().contains(externalVsys.getName()))
            .flatMap(
                externalVsys -> generateInterVsysCrossZoneCalls(fromZone, toZone, externalVsys));
    Stream<ExprAclLine> sgLines =
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
  static @Nonnull Stream<ExprAclLine> generatedSharedGatewayVsysCrossZoneCalls(
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
  static @Nonnull Stream<ExprAclLine> generateInterVsysCrossZoneCalls(
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
  static @Nonnull Stream<ExprAclLine> generateDoubleCrossZoneCalls(
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
  private List<MatchHeaderSpace> aclLineMatchExprsFromRuleEndpointSources(
      Collection<RuleEndpoint> endpoints, Vsys vsys, Warnings w, String filename) {
    return endpoints.stream()
        .map(
            source ->
                new MatchHeaderSpace(
                    HeaderSpace.builder().setSrcIps(ruleEndpointToIpSpace(source, vsys, w)).build(),
                    getRuleEndpointTraceElement(source, vsys, filename)))
        .collect(ImmutableList.toImmutableList());
  }

  @Nonnull
  private List<MatchHeaderSpace> aclLineMatchExprsFromRuleEndpointDestinations(
      Collection<RuleEndpoint> endpoints, Vsys vsys, Warnings w, String filename) {
    return endpoints.stream()
        .map(
            dest ->
                new MatchHeaderSpace(
                    HeaderSpace.builder().setDstIps(ruleEndpointToIpSpace(dest, vsys, w)).build(),
                    getRuleEndpointTraceElement(dest, vsys, filename)))
        .collect(ImmutableList.toImmutableList());
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

  private TraceElement matchSecurityRuleTraceElement(String ruleName, Vsys vsys) {
    return PaloAltoTraceElementCreators.matchSecurityRuleTraceElement(
        ruleName, vsys.getName(), _filename);
  }

  /**
   * Negate source and destination IPs for specified list of {@link MatchHeaderSpace}, and wrap with
   * negate trace element
   */
  private List<AclLineMatchExpr> negateMatchIps(List<MatchHeaderSpace> sources) {
    return sources.stream()
        .map(e -> new NotMatchExpr(e, matchNegatedAddressTraceElement()))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Convert specified firewall rule into an {@link ExprAclLine}. This should only be called once
   * per rule, during initial conversion (i.e. during {@code convertSecurityRules}).
   */
  // Most of the conversion is fairly straight-forward: rules have actions, src and dest IP
  // constraints, and service (aka Protocol + Ports) constraints.
  //   However, services are a bit complicated when `service application-default` is used. In that
  //   case, we extract service definitions from the application that matches.
  private ExprAclLine toIpAccessListLine(SecurityRule rule, Vsys vsys) {
    assert !rule.getDisabled(); // handled by caller.

    //////////////////////////////////////////////////////////////////////////////////////////
    // 1. Initialize the list of conditions.
    List<AclLineMatchExpr> conjuncts = new LinkedList<>();

    //////////////////////////////////////////////////////////////////////////////////////////
    // 2. Match SRC IPs if specified.
    List<MatchHeaderSpace> srcExprs =
        aclLineMatchExprsFromRuleEndpointSources(rule.getSource(), vsys, _w, _filename);
    if (!srcExprs.isEmpty()) {
      conjuncts.add(
          rule.getNegateSource()
              // Tracing past NotExpr does not work well, so convert from Not(Or(...)) to
              // And(Not(...)) to push Not further down in trace
              ? new AndMatchExpr(negateMatchIps(srcExprs), matchSourceAddressTraceElement())
              : new OrMatchExpr(srcExprs, matchSourceAddressTraceElement()));
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // 3. Match DST IPs if specified.
    List<MatchHeaderSpace> dstExprs =
        aclLineMatchExprsFromRuleEndpointDestinations(rule.getDestination(), vsys, _w, _filename);
    if (!dstExprs.isEmpty()) {
      conjuncts.add(
          rule.getNegateDestination()
              // Tracing past NotExpr does not work well, so convert from Not(Or(...)) to
              // And(Not(...)) to push Not further down in trace
              ? new AndMatchExpr(negateMatchIps(dstExprs), matchDestinationAddressTraceElement())
              : new OrMatchExpr(dstExprs, matchDestinationAddressTraceElement()));
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // 4. Match services.
    getServiceExpr(rule, vsys).ifPresent(conjuncts::add);

    return ExprAclLine.builder()
        .setName(rule.getName())
        .setAction(rule.getAction())
        .setMatchCondition(new AndMatchExpr(conjuncts))
        .setTraceElement(matchSecurityRuleTraceElement(rule.getName(), vsys))
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
            permittedByAcl(
                computeServiceGroupMemberAclName(vsysName, serviceName),
                matchServiceTraceElement()));
      } else if (serviceName.equals(ServiceBuiltIn.ANY.getName())) {
        // Anything is allowed.
        serviceDisjuncts.add(ServiceBuiltIn.ANY.toAclLineMatchExpr());
      } else if (serviceName.equals(ServiceBuiltIn.APPLICATION_DEFAULT.getName())) {
        if (rule.getAction() == LineAction.PERMIT) {
          // Since Batfish cannot currently match above L4, we follow Cisco-fragments-like logic:
          // When permitting an application, optimistically permit all traffic where the L4 rule
          // matches, assuming it is this application. But when blocking a specific application, do
          // not block all matching L4 traffic, since we can't know it is this specific application.
          serviceDisjuncts.add(
              new OrMatchExpr(
                  matchServicesForApplications(rule, vsys),
                  matchServiceApplicationDefaultTraceElement()));
        }
      } else if (serviceName.equals(ServiceBuiltIn.SERVICE_HTTP.getName())) {
        serviceDisjuncts.add(ServiceBuiltIn.SERVICE_HTTP.toAclLineMatchExpr());
      } else if (serviceName.equals(ServiceBuiltIn.SERVICE_HTTPS.getName())) {
        serviceDisjuncts.add(ServiceBuiltIn.SERVICE_HTTPS.toAclLineMatchExpr());
      } else {
        _w.redFlag(String.format("No matching service group/object found for: %s", serviceName));
      }
    }
    return Optional.of(new OrMatchExpr(serviceDisjuncts));
  }

  /**
   * Create an {@link Optional} {@link AclLineMatchExpr} for the specified application/group name.
   * If no corresponding application/group is found, then {@link Optional#empty()} is returned.
   */
  private Optional<AclLineMatchExpr> aclLineMatchExprForApplicationOrGroup(
      String name, SecurityRule rule, Vsys vsys) {
    String vsysName = vsys.getName();

    // Assume all traffic matches some application under the "any" definition
    if (name.equals(CATCHALL_APPLICATION_NAME)) {
      return Optional.of(new TrueExpr(matchApplicationAnyTraceElement()));
    }

    ApplicationGroup group = vsys.getApplicationGroups().get(name);
    if (group != null) {
      return Optional.of(
          new OrMatchExpr(
              group
                  .getDescendantObjects(vsys.getApplications(), vsys.getApplicationGroups())
                  .stream()
                  // Don't add trace for children; we've already flattened intermediate app groups
                  .map(a -> aclLineMatchExprForApplication(a, null))
                  .collect(ImmutableList.toImmutableList()),
              matchApplicationGroupTraceElement(name, vsysName, _filename)));
    }

    Application a = vsys.getApplications().get(name);
    if (a != null) {
      return Optional.of(
          aclLineMatchExprForApplication(
              a, matchApplicationObjectTraceElement(name, vsysName, _filename)));
    }

    Optional<Application> builtIn = ApplicationBuiltIn.getBuiltInApplication(name);
    if (builtIn.isPresent()) {
      return Optional.of(
          aclLineMatchExprForApplication(builtIn.get(), matchBuiltInApplicationTraceElement(name)));
    }
    // Did not find in the right hierarchy, so stop and warn.
    _w.redFlag(
        String.format(
            "Unable to identify application %s in vsys %s rule %s",
            name, vsys.getName(), rule.getName()));
    return Optional.empty();
  }

  /** Create an {@link AclLineMatchExpr} matching any services in the specified application. */
  private AclLineMatchExpr aclLineMatchExprForApplication(
      Application application, @Nullable TraceElement traceElement) {
    return new OrMatchExpr(
        application.getServices().stream()
            .map(s -> s.toMatchHeaderSpace(_w))
            .collect(ImmutableList.toImmutableList()),
        traceElement);
  }

  private List<AclLineMatchExpr> matchServicesForApplications(SecurityRule rule, Vsys vsys) {
    return rule.getApplications().stream()
        .map(a -> aclLineMatchExprForApplicationOrGroup(a, rule, vsys))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(ImmutableList.toImmutableList());
  }

  /** Converts interface address {@code String} to {@link IpSpace} */
  @Nullable
  @SuppressWarnings("fallthrough")
  private ConcreteInterfaceAddress interfaceAddressToConcreteInterfaceAddress(
      @Nullable InterfaceAddress address, Vsys vsys, Warnings w) {
    if (address == null) {
      return null;
    }
    String addressText = address.getValue();
    // Palo Alto allows object references that look like IP addresses etc.
    // Devices use objects over constants when possible, so, check to see if there is a matching
    // object regardless of the type of interface address we're expecting.
    if (vsys.getAddressObjects().containsKey(addressText)) {
      AddressObject addrObject = vsys.getAddressObjects().get(addressText);
      ConcreteInterfaceAddress concreteIfaceAddr = addrObject.toConcreteInterfaceAddress(_w);
      if (concreteIfaceAddr != null) {
        return concreteIfaceAddr;
      }
      // If we cannot build a concrete interface address from the address object, assume we're
      // either using the literal value (for names that look like addresses) or referencing some
      // object in a different namespace.

      // NOTE: not sure if real devices actually check other namespaces or just fail here.
    }
    switch (vsys.getNamespaceType()) {
      case LEAF:
        if (_shared != null) {
          return interfaceAddressToConcreteInterfaceAddress(address, _shared, w);
        }
        // fall-through
      case SHARED:
        if (_panorama != null) {
          return interfaceAddressToConcreteInterfaceAddress(address, _panorama, w);
        }
        // fall-through
      default:
        // No named object found matching this value, so parse the value as is
        switch (address.getType()) {
          case IP_ADDRESS:
            return ConcreteInterfaceAddress.create(Ip.parse(addressText), Prefix.MAX_PREFIX_LENGTH);
          case IP_PREFIX:
            return ConcreteInterfaceAddress.parse(addressText);
          case REFERENCE:
          default:
            // Assume warning is surfaced in undefined references or in conversion to concrete addr
            return null;
        }
    }
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
            Optional<IpSpace> ipSpace = rangeStringToIpSpace(endpointValue);
            if (ipSpace.isPresent()) {
              return ipSpace.get();
            }
            w.redFlag("Could not convert RuleEndpoint range to IpSpace: " + endpoint);
            return EmptyIpSpace.INSTANCE;
          case REFERENCE:
            // Rely on undefined references to surface this issue (endpoint reference not defined)
            return EmptyIpSpace.INSTANCE;
          default:
            w.redFlag("Could not convert RuleEndpoint to IpSpace: " + endpoint);
            return EmptyIpSpace.INSTANCE;
        }
    }
  }

  /**
   * Convert specified range string into an {@link Optional} {@link IpSpace}. If the range is not
   * valid, {@link Optional#empty()} is returned. Assumes the supplied range string is a
   * syntactically valid formatted range (should be guaranteed by the parser).
   */
  private Optional<IpSpace> rangeStringToIpSpace(String range) {
    String[] ips = range.split("-");
    Ip low = Ip.parse(ips[0]);
    Ip high = Ip.parse(ips[1]);
    if (low.compareTo(high) < 0) {
      return Optional.of(IpRange.range(low, high));
    }
    return Optional.empty();
  }

  /**
   * Convert specified range string into an {@link Optional} {@link Range}. If the range is not
   * valid, {@link Optional#empty()} is returned. Assumes the supplied range string is a
   * syntactically valid formatted range (should be guaranteed by the parser).
   */
  private Optional<Range<Ip>> rangeStringToRange(String range) {
    String[] ips = range.split("-");
    Ip low = Ip.parse(ips[0]);
    Ip high = Ip.parse(ips[1]);
    if (low.compareTo(high) < 0) {
      return Optional.of(Range.closed(low, high));
    }
    return Optional.empty();
  }

  /**
   * Gets the {@code TraceElement} corresponding to the specified {@link RuleEndpoint}. Returns
   * {@code null} if the endpoint cannot be resolved.
   */
  @Nullable
  @SuppressWarnings("fallthrough")
  private TraceElement getRuleEndpointTraceElement(
      RuleEndpoint endpoint, Vsys vsys, String filename) {
    String endpointValue = endpoint.getValue();
    String vsysName = vsys.getName();
    // Palo Alto allows object references that look like IP addresses, ranges, etc.
    // Devices use objects over constants when possible, so, check to see if there is a matching
    // group or object regardless of the type of endpoint we're expecting.
    if (vsys.getAddressObjects().containsKey(endpointValue)) {
      return matchAddressObjectTraceElement(endpointValue, vsysName, filename);
    }
    if (vsys.getAddressGroups().containsKey(endpoint.getValue())) {
      return matchAddressGroupTraceElement(endpointValue, vsysName, filename);
    }
    switch (vsys.getNamespaceType()) {
      case LEAF:
        if (_shared != null) {
          return getRuleEndpointTraceElement(endpoint, _shared, filename);
        }
        // fall-through
      case SHARED:
        if (_panorama != null) {
          return getRuleEndpointTraceElement(endpoint, _panorama, filename);
        }
        // fall-through
      default:
        // No named object found matching this endpoint, so parse the endpoint value as is
        switch (endpoint.getType()) {
          case Any:
            return matchAddressAnyTraceElement();
          case IP_ADDRESS:
          case IP_PREFIX:
          case IP_RANGE:
            return matchAddressValueTraceElement(endpointValue);
          case REFERENCE:
          default:
            // Unresolved reference or unhandled type
            return null;
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
            Optional<Range<Ip>> range = rangeStringToRange(endpointValue);
            if (range.isPresent()) {
              return ImmutableRangeSet.of(range.get());
            }
            w.redFlag("Could not convert RuleEndpoint range to RangeSet: " + endpoint);
            return ImmutableRangeSet.of();
          case REFERENCE:
            // Rely on undefined references to surface this issue (endpoint reference not defined)
            return ImmutableRangeSet.of();
          default:
            w.redFlag("Could not convert RuleEndpoint to RangeSet: " + endpoint);
            return ImmutableRangeSet.of();
        }
    }
  }

  private static InterfaceType batfishInterfaceType(
      @Nonnull Interface.Type panType, @Nullable Interface.Type parentType, Warnings w) {
    switch (panType) {
      case AGGREGATED_ETHERNET:
        return InterfaceType.AGGREGATED;
      case PHYSICAL:
        return InterfaceType.PHYSICAL;
      case LAYER2:
      case LAYER3:
        if (parentType == Interface.Type.AGGREGATED_ETHERNET) {
          return InterfaceType.AGGREGATE_CHILD;
        }
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
    Optional<InterfaceRuntimeData> ifaceRuntimeData =
        Optional.ofNullable(_hostname)
            .map(h -> _runtimeData.getRuntimeData(h))
            .map(d -> d.getInterface(name));
    Interface.Type parentType = iface.getParent() != null ? iface.getParent().getType() : null;
    org.batfish.datamodel.Interface.Builder newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(name)
            .setOwner(_c)
            .setType(batfishInterfaceType(iface.getType(), parentType, _w));

    Integer mtu = iface.getMtu();
    if (mtu != null) {
      newIface.setMtu(mtu);
    }

    // It is unclear which vsys is used to start the object lookup process on multi-vsys systems,
    // since interfaces are not associated with particular vsys.
    // Assuming default vsys is good enough for now (this is the behavior for single-vsys systems).
    ConcreteInterfaceAddress interfaceAddress =
        interfaceAddressToConcreteInterfaceAddress(
            iface.getAddress(), _virtualSystems.get(DEFAULT_VSYS_NAME), _w);
    // No explicit address detected, fallback to runtime data
    if (interfaceAddress == null) {
      interfaceAddress = ifaceRuntimeData.map(InterfaceRuntimeData::getAddress).orElse(null);
    }

    if (interfaceAddress != null) {
      if (iface.getType() == Interface.Type.LOOPBACK
          && interfaceAddress.getPrefix().getPrefixLength() != Prefix.MAX_PREFIX_LENGTH) {
        _w.redFlag("Loopback ip address must be /32 or without mask");
      } else {
        newIface.setAddress(interfaceAddress);
        newIface.setSecondaryAddresses(
            Sets.difference(
                iface.getAllAddresses().stream()
                    .map(
                        a ->
                            interfaceAddressToConcreteInterfaceAddress(
                                a, _virtualSystems.get(DEFAULT_VSYS_NAME), _w))
                    .collect(Collectors.toSet()),
                ImmutableSet.of(interfaceAddress)));
      }
    }
    newIface.setActive(iface.getActive());
    newIface.setDescription(iface.getComment());
    newIface.setChannelGroup(iface.getAggregateGroup());

    if (iface.getType() == Interface.Type.PHYSICAL) {
      double speed = 1e9;
      if (iface.getName().matches("ethernet(\\d+)/2[1234]")) {
        // https://knowledgebase.paloaltonetworks.com/KCSArticleDetail?id=kA10g000000ClssCAC
        speed = 1e10;
      }
      newIface.setSpeed(speed);
      newIface.setBandwidth(speed);
    } else if (iface.getParent() != null) {
      org.batfish.datamodel.Interface parentIface =
          _c.getAllInterfaces().get(iface.getParent().getName());
      assert parentIface != null; // because interfaces are processed in sorted order
      newIface.setBandwidth(parentIface.getBandwidth());
      // do not set speed, that's a physical property.
    }

    if (iface.getType() == Interface.Type.LAYER3) {
      newIface.setEncapsulationVlan(iface.getTag());
    } else if (iface.getType() == Interface.Type.LAYER2) {
      newIface.setAccessVlan(iface.getTag());
    }

    // add outgoing filter
    IpAccessList.Builder aclBuilder =
        IpAccessList.builder().setOwner(_c).setName(computeOutgoingFilterName(iface.getName()));
    List<AclLine> aclLines = new ArrayList<>();
    Optional<Vsys> sharedGatewayOptional =
        _sharedGateways.values().stream()
            .filter(sg -> sg.getImportedInterfaces().contains(name))
            .findFirst();
    Zone zone = iface.getZone();
    if (sharedGatewayOptional.isPresent()) {
      Vsys sharedGateway = sharedGatewayOptional.get();
      String sgName = sharedGateway.getName();
      String outgoingFilterName = computeOutgoingFilterName(computeObjectName(sgName, sgName));
      aclLines.add(
          new AclAclLine(
              String.format("Match restrictions for shared gateway %s", sgName),
              outgoingFilterName));
      newIface.setFirewallSessionInterfaceInfo(
          new FirewallSessionInterfaceInfo(
              true, sharedGateway.getImportedInterfaces(), null, null));
    } else if (zone != null) {
      newIface.setZoneName(zone.getName());
      if (zone.getType() == Type.LAYER3) {
        String zoneFilterName =
            computeOutgoingFilterName(computeObjectName(zone.getVsys().getName(), zone.getName()));
        aclLines.add(
            new AclAclLine(
                String.format(
                    "Match rules for exiting interface %s in vsys %s zone %s",
                    iface.getName(), zone.getVsys().getName(), zone.getName()),
                zoneFilterName,
                ifaceOutgoingTraceElement(
                    iface.getName(), zone.getName(), zone.getVsys().getName())));
        newIface.setFirewallSessionInterfaceInfo(
            new FirewallSessionInterfaceInfo(true, zone.getInterfaceNames(), null, null));
      }
    } else {
      // Do not allow any traffic to exit an unzoned interface
      aclLines.add(
          ExprAclLine.builder()
              .rejecting()
              .setName("Not in a zone")
              .setMatchCondition(TrueExpr.INSTANCE)
              .setTraceElement(unzonedIfaceRejectTraceElement(iface.getName()))
              .build());
    }

    if (!aclLines.isEmpty()) {
      // For interfaces with security rules, assume traffic originating from the device is allowed
      // out the interface
      // TODO this isn't tested and may not line up with actual device behavior, but is in place to
      // allow things like BGP sessions to come up
      aclLines.add(
          accepting()
              .setMatchCondition(ORIGINATING_FROM_DEVICE)
              .setTraceElement(originatedFromDeviceTraceElement())
              .build());
      newIface.setOutgoingOriginalFlowFilter(
          aclBuilder.setLines(ImmutableList.copyOf(aclLines)).build());
    }

    // If there are NAT rules for packets entering this interface's zone, apply them
    String packetPolicyName = getPacketPolicyForZone(zone);
    if (packetPolicyName != null) {
      newIface.setRoutingPolicy(packetPolicyName);
    }

    return newIface.build();
  }

  /**
   * Get or generate packet policy for entering the specified zone, attach it to the VI config, and
   * return the name if applicable. If no packet policy is applicable, returns {@code null}.
   */
  private @Nullable String getPacketPolicyForZone(@Nullable Zone zone) {
    // Unzoned interfaces don't transmit traffic, so don't need packet policy
    if (zone == null) {
      return null;
    }
    String packetPolicyName = computePacketPolicyName(zone);
    if (!_c.getPacketPolicies().containsKey(packetPolicyName)) {
      // Packet policy does not exist for this zone. Check NAT rules and generate policy if needed.
      List<NatRule> natRules = getNatRulesForEnteringZone(zone);
      if (natRules.isEmpty()) {
        // No NAT rules apply to packets entering this zone.
        return null;
      }
      _c.getPacketPolicies()
          .put(packetPolicyName, buildPacketPolicy(packetPolicyName, natRules, zone.getVsys()));
    }
    return packetPolicyName;
  }

  /**
   * Build {@link BoolExpr} for service in specified {@link NatRule}. Returns {@link
   * Optional#empty()} if no service is specified or if a reference cannot be resolved.
   */
  private Optional<BoolExpr> getNatServiceBoolExpr(
      @Nullable ServiceOrServiceGroupReference service, Vsys vsys) {
    if (service == null) {
      return Optional.empty();
    }
    String serviceName = service.getName();
    // Check for matching object before using built-ins
    String vsysName = service.getVsysName(this, vsys);

    if (vsysName != null) {
      return Optional.of(
          new PacketMatchExpr(
              permittedByAcl(computeServiceGroupMemberAclName(vsysName, serviceName))));
    } else if (serviceName.equals(ServiceBuiltIn.ANY.getName())) {
      // Anything is allowed
      return Optional.of(new PacketMatchExpr(TrueExpr.INSTANCE));
    } else if (serviceName.equals(ServiceBuiltIn.SERVICE_HTTP.getName())) {
      return Optional.of(
          new PacketMatchExpr(new MatchHeaderSpace(ServiceBuiltIn.SERVICE_HTTP.getHeaderSpace())));
    } else if (serviceName.equals(ServiceBuiltIn.SERVICE_HTTPS.getName())) {
      return Optional.of(
          new PacketMatchExpr(new MatchHeaderSpace(ServiceBuiltIn.SERVICE_HTTPS.getHeaderSpace())));
    } else {
      _w.redFlag(String.format("No matching service group/object found for: %s", serviceName));
    }
    return Optional.empty();
  }

  /**
   * Build and return a list of all {@link BoolExpr}s describing the header space matching the
   * specified {@link NatRule}. This should only be called once per rule, during initial conversion
   * (i.e. during {@code convertNatRules}).
   */
  private List<BoolExpr> buildNatRuleHeaderSpaceBoolExprs(NatRule rule, Vsys vsys) {
    ImmutableList.Builder<BoolExpr> boolExprs = ImmutableList.builder();
    HeaderSpace.Builder headerSpace = HeaderSpace.builder();

    IpSpace srcIps = ipSpaceFromRuleEndpoints(rule.getSource(), vsys, _w);
    IpSpace dstIps = ipSpaceFromRuleEndpoints(rule.getDestination(), vsys, _w);
    boolExprs.add(
        new PacketMatchExpr(
            new MatchHeaderSpace(headerSpace.setSrcIps(srcIps).setDstIps(dstIps).build())));
    getNatServiceBoolExpr(rule.getService(), vsys).ifPresent(boolExprs::add);
    return boolExprs.build();
  }

  /** Build a routing policy for the specified NAT rule + vsys entries in the specified vsys. */
  private PacketPolicy buildPacketPolicy(String name, List<NatRule> natRules, Vsys vsys) {
    ImmutableList.Builder<org.batfish.datamodel.packet_policy.Statement> lines =
        ImmutableList.builder();
    for (NatRule rule : natRules) {
      // Handled by caller
      assert !rule.getDisabled();

      Zone toZone = vsys.getZones().get(rule.getTo());
      if (toZone == null) {
        continue;
      }

      ImmutableList.Builder<BoolExpr> conditions = ImmutableList.builder();
      // Conditions under which to apply this NAT rule
      getNatHeaderSpaceBoolExprs(vsys.getName(), rule.getName()).ifPresent(conditions::addAll);
      // Only apply NAT if flow is exiting an interface in the to-zone
      conditions.add(
          new FibLookupOutgoingInterfaceIsOneOf(
              IngressInterfaceVrf.instance(), toZone.getInterfaceNames()));

      // Actions to take when NAT rule is matched: transformation, FIB lookup, return
      ImmutableList.Builder<Statement> actionsIfMatched = ImmutableList.builder();
      getNatTransformation(vsys.getName(), rule.getName())
          .ifPresent(transform -> actionsIfMatched.add(new ApplyTransformation(transform)));
      actionsIfMatched.add(new Return(new FibLookup(IngressInterfaceVrf.instance())));

      // Add packet policy line for matching this rule
      lines.add(
          new org.batfish.datamodel.packet_policy.If(
              Conjunction.of(conditions.build()), actionsIfMatched.build()));
    }
    return new PacketPolicy(
        name, lines.build(), new Return(new FibLookup(IngressInterfaceVrf.instance())));
  }

  /**
   * Converts the given {@link NatRule} to a {@link Transformation} with a TRUE guard. There is no
   * need for a nontrivial guard because all transformations are applied in PBR, and the rule's
   * match conditions are converted to a {@link BoolExpr} condition on the packet policy line that
   * applies the transformation. This should only be called once per rule, during initial conversion
   * (i.e. during {@code convertNatRules}).
   */
  private Optional<Transformation> convertRuleToTransformation(NatRule rule, Vsys vsys) {
    List<TransformationStep> transformationSteps =
        ImmutableList.<TransformationStep>builder()
            .addAll(getSourceTransformationSteps(rule, vsys))
            .addAll(getDestinationTransformationSteps(rule, vsys))
            .build();

    return transformationSteps.isEmpty()
        ? Optional.empty()
        : Optional.of(Transformation.always().apply(transformationSteps).build());
  }

  private List<TransformationStep> getSourceTransformationSteps(NatRule rule, Vsys vsys) {
    List<RuleEndpoint> translatedSrcAddrs =
        Optional.ofNullable(rule.getSourceTranslation())
            .map(SourceTranslation::getDynamicIpAndPort)
            .map(DynamicIpAndPort::getTranslatedAddresses)
            .orElse(null);
    if (translatedSrcAddrs == null) {
      // No source translation
      return ImmutableList.of();
    }

    RangeSet<Ip> pool = ipRangeSetFromRuleEndpoints(translatedSrcAddrs, vsys, _w);
    if (pool.isEmpty()) {
      // Can't apply a source IP translation with empty IP pool
      // TODO: Check real behavior in this scenario
      _w.redFlag(
          String.format(
              "NAT rule %s of VSYS %s will not apply source translation because its source"
                  + " translation pool is empty",
              rule.getName(), vsys.getName()));
      return ImmutableList.of();
    }

    // Create steps to transform src IP and port
    return ImmutableList.of(
        new AssignIpAddressFromPool(TransformationType.SOURCE_NAT, IpField.SOURCE, pool),
        TransformationStep.assignSourcePort(
            NamedPort.EPHEMERAL_LOWEST.number(), NamedPort.EPHEMERAL_HIGHEST.number()));
  }

  private Optional<TransformationStep> getDestinationAddressTransformationStep(
      NatRule rule, Vsys vsys, DestinationTranslation destinationTranslation) {
    RuleEndpoint translatedDstAddr = destinationTranslation.getTranslatedAddress();
    if (translatedDstAddr == null) {
      // No destination address translation
      return Optional.empty();
    }

    RangeSet<Ip> pool = ruleEndpointToIpRangeSet(translatedDstAddr, vsys, _w);
    if (pool.isEmpty()) {
      // Can't apply a dest IP translation with empty IP pool
      // TODO: Check real behavior in this scenario
      _w.redFlag(
          String.format(
              "NAT rule %s of VSYS %s will not apply destination translation because its"
                  + " destination translation pool is empty",
              rule.getName(), vsys.getName()));
      return Optional.empty();
    }
    return Optional.of(
        new AssignIpAddressFromPool(TransformationType.DEST_NAT, IpField.DESTINATION, pool));
  }

  private Optional<TransformationStep> getDestinationPortTransformationStep(
      DestinationTranslation destinationTranslation) {
    return Optional.ofNullable(destinationTranslation.getTranslatedPort())
        .map(p -> new AssignPortFromPool(TransformationType.DEST_NAT, PortField.DESTINATION, p, p));
  }

  private List<TransformationStep> getDestinationTransformationSteps(NatRule rule, Vsys vsys) {
    DestinationTranslation destinationTranslation = rule.getDestinationTranslation();
    if (destinationTranslation == null) {
      return ImmutableList.of();
    }

    ImmutableList.Builder<TransformationStep> steps = ImmutableList.builder();
    getDestinationAddressTransformationStep(rule, vsys, destinationTranslation)
        .ifPresent(steps::add);
    getDestinationPortTransformationStep(destinationTranslation).ifPresent(steps::add);
    return steps.build();
  }

  /**
   * Return a list of all valid, enabled NAT rules to apply to packets entering the specified zone.
   */
  private List<NatRule> getNatRulesForEnteringZone(Zone zone) {
    Vsys vsys = zone.getVsys();
    return getAllNatRules(vsys)
        .filter(
            rule ->
                // Note: This isn't a good place to file warnings for invalid NAT rules because it
                // leaves out rules that aren't in a zone. Instead file warnings per vsys.
                checkNatRuleValid(rule, false)
                    && !rule.getDisabled()
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
                "iBGP peer %s has a mismatched peer-as %s which is not the local-as %s; replacing"
                    + " it",
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
          .map(
              a ->
                  interfaceAddressToConcreteInterfaceAddress(
                      a, _virtualSystems.get(DEFAULT_VSYS_NAME), _w))
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
    viAreaBuilder.setInterfaces(
        computeAreaInterfaces(_c.getAllInterfaces(vrf.getName()), vsArea, ospfProcessName));
    return viAreaBuilder.build();
  }

  private @Nonnull Set<String> computeAreaInterfaces(
      Map<String, org.batfish.datamodel.Interface> viInterfaces,
      org.batfish.representation.palo_alto.OspfArea vsArea,
      String ospfProcessName) {
    ImmutableSet.Builder<String> ospfIfaceNames = ImmutableSet.builder();
    Ip vsAreaId = vsArea.getAreaId();
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
    for (String interfaceName : vr.getInterfaceNames()) {
      org.batfish.datamodel.Interface iface = _c.getAllInterfaces().get(interfaceName);
      if (iface != null) {
        iface.setVrf(vrf);
      }
    }

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

  /**
   * Copy configuration from specified source vsys to specified target vsys. Any previously made
   * changes will be overwritten in this process. Note: this only supports copying device-group vsys
   * configuration (objects and rules) and rules are merged by appending pre-rulebase and prepending
   * post-rulebase.
   */
  private void applyVsys(@Nullable Vsys source, Vsys target) {
    if (source == null) {
      return;
    }
    // Merge (and replace) objects
    target.getApplications().putAll(source.getApplications());
    target.getApplicationGroups().putAll(source.getApplicationGroups());
    target.getAddressObjects().putAll(source.getAddressObjects());
    target.getAddressGroups().putAll(source.getAddressGroups());
    target.getServices().putAll(source.getServices());
    target.getServiceGroups().putAll(source.getServiceGroups());
    target.getTags().putAll(source.getTags());

    /*
     * Merge rules. Pre-rulebase rules should be appended, post-rulebase rules should be prepended.
     * Note: "regular" rulebase does not apply to panorama
     */
    // NAT pre
    target.getPreRulebase().getNatRules().putAll(source.getPreRulebase().getNatRules());
    // Security pre
    target.getPreRulebase().getSecurityRules().putAll(source.getPreRulebase().getSecurityRules());

    // NAT post
    // Note: using LinkedHashMaps to preserve insertion order
    Map<String, NatRule> postRulebaseNat =
        new LinkedHashMap<>(source.getPostRulebase().getNatRules());
    Map<String, NatRule> targetPostNat = target.getPostRulebase().getNatRules();
    postRulebaseNat.putAll(targetPostNat);
    targetPostNat.clear();
    targetPostNat.putAll(postRulebaseNat);
    // Security post
    // Note: using InsertOrderedMap to preserve insertion order
    Map<String, SecurityRule> postRulebaseSecurity =
        new InsertOrderedMap<>(source.getPostRulebase().getSecurityRules());
    Map<String, SecurityRule> targetPostSecurity = target.getPostRulebase().getSecurityRules();
    postRulebaseSecurity.putAll(targetPostSecurity);
    targetPostSecurity.clear();
    targetPostSecurity.putAll(postRulebaseSecurity);
  }

  /**
   * Apply the specified device-group "pseudo-config" and shared config to this
   * PaloAltoConfiguration. Any previously made changes will be overwritten in this process.
   */
  private void applyDeviceGroup(
      DeviceGroup template, @Nullable Vsys shared, Map<String, DeviceGroup> panoramaDeviceGroups) {
    // TODO support applying device-group to specific vsys
    // https://github.com/batfish/batfish/issues/5910

    // Create the target vsys (it shouldn't already exist)
    Vsys target = new Vsys(PANORAMA_VSYS_NAME, NamespaceType.PANORAMA);
    assert _panorama == null;
    _panorama = target;

    // Apply shared config first, since it is overwritten by device-groups applied after it
    applyVsys(shared, target);

    List<DeviceGroup> inheritedDeviceGroups = new ArrayList<>();
    inheritedDeviceGroups.add(template);
    collectParents(template, panoramaDeviceGroups, inheritedDeviceGroups);

    // Apply higher level parents first
    // since their config should be overwritten by lower level parents
    for (DeviceGroup parent : ImmutableList.copyOf(inheritedDeviceGroups).reverse()) {
      applyVsys(parent.getPanorama(), target);
    }
  }

  /** Collect all parents of the specified device-group (recursively) into the specified list. */
  private void collectParents(
      DeviceGroup deviceGroup,
      Map<String, DeviceGroup> panoramaDeviceGroups,
      List<DeviceGroup> parents) {
    String parentName = deviceGroup.getParentDg();
    if (parentName == null) {
      return;
    }
    DeviceGroup parent = panoramaDeviceGroups.get(parentName);
    if (parents.contains(parent)) {
      _w.redFlag(String.format("Device-group %s cannot be inherited more than once.", parentName));
      return;
    }
    if (parent == null) {
      _w.redFlag(
          String.format(
              "Device-group %s cannot inherit from unknown device-group %s.",
              deviceGroup.getName(), parentName));
      return;
    }

    parents.add(parent);
    // If this parent has its own parent(s), collect those too
    collectParents(parent, panoramaDeviceGroups, parents);
  }

  /**
   * Merge specified template "vsys" configuration into specified target. Only merges configuration
   * supported by templates (i.e. does not merge device-group vsys configuration).
   */
  private void applyTemplateVsys(Vsys template, Vsys target) {
    // Merge template objects and imports
    for (Entry<String, Zone> entry : template.getZones().entrySet()) {
      entry.getValue().setVsys(target);
      target.getZones().put(entry.getKey(), entry.getValue());
    }
    target.getSyslogServerGroups().putAll(template.getSyslogServerGroups());
    target.getImportedInterfaces().addAll(template.getImportedInterfaces());

    // Overwrite settings
    if (template.getDisplayName() != null) {
      target.setDisplayName(template.getDisplayName());
    }
  }

  /**
   * Apply the specified template-stack "pseudo-config" to this PaloAltoConfiguration. Any
   * previously made changes will be overwritten in this process.
   */
  private void applyTemplateStack(TemplateStack stack, PaloAltoConfiguration mainConfig) {
    /* Iterate over templates in reverse order, since first template should overwrite other template configuration */
    for (String templateName : ImmutableList.copyOf(stack.getTemplates()).reverse()) {
      Template template = mainConfig.getTemplate(templateName);
      if (template == null) {
        // Warning will be surfaced through undefined references
        continue;
      }
      // Deviceconfig entities
      if (template.getHostname() != null) {
        setHostname(template.getHostname());
      }
      if (template.getMgmtIfaceGateway() != null) {
        setMgmtIfaceGateway(template.getMgmtIfaceGateway());
      }
      if (template.getMgmtIfaceAddress() != null) {
        setMgmtIfaceAddress(template.getMgmtIfaceAddress());
      }
      if (template.getMgmtIfaceNetmask() != null) {
        setMgmtIfaceNetmask(template.getMgmtIfaceNetmask());
      }
      if (template.getDnsServerPrimary() != null) {
        setDnsServerPrimary(template.getDnsServerPrimary());
      }
      if (template.getDnsServerSecondary() != null) {
        setDnsServerSecondary(template.getDnsServerSecondary());
      }
      if (template.getNtpServerPrimary() != null) {
        setNtpServerPrimary(template.getNtpServerPrimary());
      }
      if (template.getNtpServerSecondary() != null) {
        setNtpServerSecondary(template.getNtpServerSecondary());
      }

      // Network entities
      _interfaces.putAll(template.getInterfaces());
      _sharedGateways.putAll(template.getSharedGateways());
      _virtualRouters.putAll(template.getVirtualRouters());
      _cryptoProfiles.addAll(template.getCryptoProfiles());

      // Vsys entities
      for (Entry<String, Vsys> entry : template.getVirtualSystems().entrySet()) {
        Vsys target = _virtualSystems.computeIfAbsent(entry.getKey(), Vsys::new);
        applyTemplateVsys(entry.getValue(), target);
      }
      // Shared vsys
      if (template.getShared() != null) {
        if (_shared == null) {
          _shared = new Vsys(SHARED_VSYS_NAME, NamespaceType.SHARED);
        }
        applyTemplateVsys(template.getShared(), _shared);
      }
    }
  }

  /** Create a config for a new device, managed the by the current device. */
  private PaloAltoConfiguration createManagedDeviceConfig(String deviceId) {
    PaloAltoConfiguration c = new PaloAltoConfiguration();
    c.setFilename(_filename);
    c.setWarnings(_w);
    c.setVendor(_vendor);
    c.setRuntimeData(_runtimeData);
    // Assume hostname is device id for now
    c.setHostname(deviceId);
    return c;
  }

  @VisibleForTesting
  public List<PaloAltoConfiguration> getManagedConfigurations() {
    // Build configs for each managed device, if applicable
    // Map of managed device ID to managed device config
    Map<String, PaloAltoConfiguration> managedConfigurations = new HashMap<>();
    // Apply device-groups to firewalls
    _deviceGroups
        .entrySet()
        .forEach(
            deviceGroupEntry ->
                deviceGroupEntry
                    .getValue()
                    .getDevices()
                    .forEach(
                        deviceId -> {
                          // Create new managed config if one doesn't already exist for this device
                          if (managedConfigurations.containsKey(deviceId)) {
                            // If the device already has a config associated with it, it must
                            // already be associated with another device-group (should not happen)
                            _w.redFlag(
                                String.format(
                                    "Managed device '%s' cannot be associated with more than one"
                                        + " device-group. Ignoring association with device-group"
                                        + " '%s'.",
                                    deviceId, deviceGroupEntry.getKey()));
                          } else {
                            PaloAltoConfiguration c = createManagedDeviceConfig(deviceId);
                            c.applyDeviceGroup(deviceGroupEntry.getValue(), _shared, _deviceGroups);
                            managedConfigurations.put(deviceId, c);
                          }
                        }));
    // Apply device-groups to individual vsyses
    _deviceGroups
        .entrySet()
        .forEach(
            deviceGroupEntry ->
                deviceGroupEntry
                    .getValue()
                    .getVsys()
                    .forEach(
                        (deviceId, vsys) -> {
                          // Create new managed config if one doesn't already exist for this device
                          if (managedConfigurations.containsKey(deviceId)) {
                            _w.redFlag(
                                String.format(
                                    "Associating vsys on a managed device with different"
                                        + " device-groups is not yet supported. Ignoring"
                                        + " association with device-group '%s' for managed device"
                                        + " '%s'.",
                                    deviceGroupEntry.getKey(), deviceId));
                            return;
                          }
                          PaloAltoConfiguration c = createManagedDeviceConfig(deviceId);
                          c.applyDeviceGroup(deviceGroupEntry.getValue(), _shared, _deviceGroups);
                          managedConfigurations.put(deviceId, c);
                        }));
    // Apply template-stacks
    _templateStacks
        .entrySet()
        .forEach(
            stackEntry ->
                stackEntry
                    .getValue()
                    .getDevices()
                    .forEach(
                        deviceId -> {
                          PaloAltoConfiguration c = managedConfigurations.get(deviceId);
                          // Create new managed config if one doesn't already exist for this device
                          if (c == null) {
                            c = createManagedDeviceConfig(deviceId);
                            managedConfigurations.put(deviceId, c);
                          }
                          c.applyTemplateStack(stackEntry.getValue(), this);
                        }));

    // Update hostnames for managed devices
    _hostnameMap.forEach(
        (deviceId, hostname) -> {
          if (managedConfigurations.containsKey(deviceId)) {
            managedConfigurations.get(deviceId).setHostname(hostname);
          } else {
            _w.redFlag(String.format("Cannot set hostname for unknown device id %s.", deviceId));
          }
        });
    return ImmutableList.copyOf(managedConfigurations.values());
  }

  @Override
  public List<Configuration> toVendorIndependentConfigurations() throws VendorConversionException {
    ImmutableList.Builder<Configuration> outputConfigurations = ImmutableList.builder();
    // Build primary config
    Configuration primaryConfig = this.toVendorIndependentConfiguration();
    outputConfigurations.add(primaryConfig);

    List<PaloAltoConfiguration> managedConfigurations = getManagedConfigurations();
    // Once managed devices are built, convert them too
    outputConfigurations.addAll(
        managedConfigurations.stream()
            .map(PaloAltoConfiguration::toVendorIndependentConfiguration)
            .collect(ImmutableList.toImmutableList()));

    if (!managedConfigurations.isEmpty()) {
      primaryConfig.setDeviceModel(DeviceModel.PALO_ALTO_PANORAMA);
    }
    return outputConfigurations.build();
  }

  private Configuration toVendorIndependentConfiguration() throws VendorConversionException {
    String hostname = getHostname();
    _c = new Configuration(hostname, _vendor);
    _c.setDeviceModel(DeviceModel.PALO_ALTO_FIREWALL);
    _c.setDefaultCrossZoneAction(LineAction.DENY);
    _c.setDefaultInboundAction(LineAction.PERMIT);
    _c.setDnsServers(getDnsServers());
    _c.setNtpServers(getNtpServers());

    // Before processing any Vsys, ensure that interfaces are attached to zones.
    attachInterfacesToZones();

    convertNamespaces();

    // Convert rules before using them in Vsys / Interfaces conversion
    convertSecurityRules();
    convertNatRules();

    // Handle converting items within virtual systems
    convertVirtualSystems();

    convertSharedGateways();

    // A map from aggregate ethernet name (like ae1) to the set of interfaces it aggregates
    Multimap<String, String> aggregates = HashMultimap.create();

    for (Interface i : _interfaces.values()) {
      // NB: sorted order is used here.
      org.batfish.datamodel.Interface viIface = toInterface(i);
      _c.getAllInterfaces().put(viIface.getName(), viIface);
      if (i.getAggregateGroup() != null) {
        aggregates.put(i.getAggregateGroup(), i.getName());
      }

      for (Entry<String, Interface> unit : i.getUnits().entrySet()) {
        org.batfish.datamodel.Interface viUnit = toInterface(unit.getValue());
        viUnit.addDependency(new Dependency(viIface.getName(), DependencyType.BIND));
        _c.getAllInterfaces().put(viUnit.getName(), viUnit);
      }
    }
    // Populate aggregates where they exist.
    for (Entry<String, Collection<String>> entry : aggregates.asMap().entrySet()) {
      org.batfish.datamodel.Interface ae = _c.getAllInterfaces().get(entry.getKey());
      if (ae == null) {
        continue;
      }

      Collection<String> members = entry.getValue();
      ae.setChannelGroupMembers(members);
      ae.setDependencies(
          members.stream()
              .map(member -> new Dependency(member, DependencyType.AGGREGATE))
              .collect(ImmutableSet.toImmutableSet()));
    }

    // Vrf conversion uses interfaces, so must be done after interface exist in VI model
    for (VirtualRouter vr : _virtualRouters.values()) {
      _c.getVrfs().put(vr.getName(), toVrf(vr));
    }

    // Batfish cannot handle interfaces without a Vrf
    // So put orphaned interfaces in a constructed Vrf and shut them down
    Vrf nullVrf = new Vrf(NULL_VRF_NAME);
    int oraphnedInterfaces = 0;
    for (Entry<String, org.batfish.datamodel.Interface> i : _c.getAllInterfaces().entrySet()) {
      org.batfish.datamodel.Interface iface = i.getValue();
      if (iface.getVrf() == null) {
        iface.setVrf(nullVrf);
        oraphnedInterfaces++;
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
                    "Interface %s is not in a virtual-router, placing in %s and clearing L2/L3"
                        + " data.",
                    iface.getName(), nullVrf.getName()));
          }
        }
      }
    }
    // Don't pollute VI model will null VRF unless we have to.
    if (oraphnedInterfaces > 0) {
      _c.getVrfs().put(nullVrf.getName(), nullVrf);
    }

    // Count and mark simple structure usages and identify undefined references
    markConcreteStructure(PaloAltoStructureType.GLOBAL_PROTECT_APP_CRYPTO_PROFILE);
    markConcreteStructure(PaloAltoStructureType.IKE_CRYPTO_PROFILE);
    markConcreteStructure(PaloAltoStructureType.IPSEC_CRYPTO_PROFILE);
    markConcreteStructure(PaloAltoStructureType.INTERFACE);
    markConcreteStructure(PaloAltoStructureType.REDIST_PROFILE);
    markConcreteStructure(PaloAltoStructureType.SECURITY_RULE);
    markConcreteStructure(PaloAltoStructureType.TEMPLATE);
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
        PaloAltoStructureUsage.SECURITY_RULE_SERVICE,
        PaloAltoStructureUsage.NAT_RULE_SERVICE);
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.SERVICE_OR_SERVICE_GROUP,
        ImmutableList.of(PaloAltoStructureType.SERVICE, PaloAltoStructureType.SERVICE_GROUP),
        PaloAltoStructureUsage.SERVICE_GROUP_MEMBER,
        PaloAltoStructureUsage.SECURITY_RULE_SERVICE,
        PaloAltoStructureUsage.NAT_RULE_SERVICE);

    // First, handle things which may or may not be referencing objects (e.g. "1.2.3.4" may be IP
    // address or a named object)
    // Handle marking rule endpoints
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.ADDRESS_LIKE_OR_NONE,
        ImmutableList.of(
            PaloAltoStructureType.ADDRESS_GROUP,
            PaloAltoStructureType.ADDRESS_OBJECT,
            PaloAltoStructureType.EXTERNAL_LIST),
        true,
        PaloAltoStructureUsage.SECURITY_RULE_DESTINATION,
        PaloAltoStructureUsage.SECURITY_RULE_SOURCE);
    // Handle ambiguous interface addresses (e.g. address object names can look like IP addresses)
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.ADDRESS_OBJECT_OR_NONE,
        ImmutableList.of(PaloAltoStructureType.ADDRESS_OBJECT),
        true,
        PaloAltoStructureUsage.LAYER3_INTERFACE_ADDRESS,
        PaloAltoStructureUsage.LOOPBACK_INTERFACE_ADDRESS);

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

    // Handle interface addresses
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.ADDRESS_OBJECT,
        ImmutableList.of(PaloAltoStructureType.ADDRESS_OBJECT),
        false,
        PaloAltoStructureUsage.LAYER3_INTERFACE_ADDRESS,
        PaloAltoStructureUsage.LOOPBACK_INTERFACE_ADDRESS);

    // Applications or Application-Groups
    markAbstractStructureFromUnknownNamespace(
        PaloAltoStructureType.APPLICATION_GROUP_OR_APPLICATION,
        ImmutableList.of(
            PaloAltoStructureType.APPLICATION_GROUP, PaloAltoStructureType.APPLICATION),
        PaloAltoStructureUsage.APPLICATION_GROUP_MEMBERS,
        PaloAltoStructureUsage.SECURITY_RULE_APPLICATION);

    return _c;
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

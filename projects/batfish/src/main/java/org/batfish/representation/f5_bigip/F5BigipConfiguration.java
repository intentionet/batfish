package org.batfish.representation.f5_bigip;

import static com.google.common.base.Predicates.notNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.common.util.CommonUtil.toImmutableMap;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.VendorConversionException;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.KernelRoute;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.flow.TransformationStep.TransformationType;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.transformation.ApplyAll;
import org.batfish.datamodel.transformation.ApplyAny;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.AssignPortFromPool;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.PortField;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationStep;
import org.batfish.datamodel.vendor_family.f5_bigip.F5BigipFamily;
import org.batfish.datamodel.vendor_family.f5_bigip.Pool;
import org.batfish.datamodel.vendor_family.f5_bigip.PoolMember;
import org.batfish.datamodel.vendor_family.f5_bigip.RouteAdvertisementMode;
import org.batfish.datamodel.vendor_family.f5_bigip.Virtual;
import org.batfish.datamodel.vendor_family.f5_bigip.VirtualAddress;
import org.batfish.vendor.VendorConfiguration;

/** Vendor-specific configuration for F5 BIG-IP device */
@ParametersAreNonnullByDefault
public class F5BigipConfiguration extends VendorConfiguration {

  private static final long serialVersionUID = 1L;

  private static WithEnvironmentExpr bgpRedistributeWithEnvironmentExpr(
      BooleanExpr expr, OriginType originType) {
    WithEnvironmentExpr we = new WithEnvironmentExpr();
    we.setExpr(expr);
    we.setPreStatements(
        ImmutableList.of(Statements.SetWriteIntermediateBgpAttributes.toStaticStatement()));
    we.setPostStatements(
        ImmutableList.of(Statements.UnsetWriteIntermediateBgpAttributes.toStaticStatement()));
    we.setPostTrueStatements(
        ImmutableList.of(
            Statements.SetReadIntermediateBgpAttributes.toStaticStatement(),
            new SetOrigin(new LiteralOrigin(originType, null))));
    return we;
  }

  public static @Nonnull String computeBgpCommonExportPolicyName(String bgpProcessName) {
    return String.format("~BGP_COMMON_EXPORT_POLICY:%s~", bgpProcessName);
  }

  public static @Nonnull String computeBgpPeerExportPolicyName(
      String bgpProcessName, Ip peerAddress) {
    return String.format("~BGP_PEER_EXPORT_POLICY:%s:%s~", bgpProcessName, peerAddress);
  }

  private final @Nonnull Map<String, BgpProcess> _bgpProcesses;
  private transient Configuration _c;
  private ConfigurationFormat _format;
  private String _hostname;
  private final @Nonnull Map<String, Interface> _interfaces;
  private final @Nonnull Map<String, Node> _nodes;
  private final @Nonnull Map<String, Pool> _pools;
  private final @Nonnull Map<String, PrefixList> _prefixLists;
  private final @Nonnull Map<String, RouteMap> _routeMaps;
  private final @Nonnull Map<String, Self> _selves;
  private transient Map<String, Set<IpSpace>> _snatAdditionalArpIps;
  private final @Nonnull Map<String, SnatPool> _snatPools;
  private final @Nonnull Map<String, Snat> _snats;
  private final @Nonnull Map<String, SnatTranslation> _snatTranslations;
  private transient Map<String, Set<IpSpace>> _virtualAdditionalDnatArpIps;
  private transient Map<String, Set<IpSpace>> _virtualAdditionalSnatArpIps;
  private final @Nonnull Map<String, VirtualAddress> _virtualAddresses;
  private transient Map<String, Transformation> _virtualIncomingTransformations;

  // TODO: implement outgoing transformations https://github.com/batfish/batfish/issues/3243
  @SuppressWarnings("unused")
  private transient Map<String, Transformation> _virtualOutgoingTransformations;

  private final @Nonnull Map<String, Virtual> _virtuals;
  private final @Nonnull Map<String, Vlan> _vlans;

  public F5BigipConfiguration() {
    _bgpProcesses = new HashMap<>();
    _interfaces = new HashMap<>();
    _nodes = new HashMap<>();
    _pools = new HashMap<>();
    _prefixLists = new HashMap<>();
    _routeMaps = new HashMap<>();
    _selves = new HashMap<>();
    _snats = new HashMap<>();
    _snatPools = new HashMap<>();
    _snatTranslations = new HashMap<>();
    _virtualAddresses = new HashMap<>();
    _virtuals = new HashMap<>();
    _vlans = new HashMap<>();
  }

  private void addActivePeer(
      BgpNeighbor neighbor, BgpProcess proc, org.batfish.datamodel.BgpProcess newProc) {
    Ip updateSource = getUpdateSource(neighbor, neighbor.getUpdateSource());

    RoutingPolicy.Builder peerExportPolicy =
        RoutingPolicy.builder()
            .setOwner(_c)
            .setName(computeBgpPeerExportPolicyName(proc.getName(), neighbor.getAddress()));

    Conjunction peerExportConditions = new Conjunction();
    If peerExportConditional =
        new If(
            "peer-export policy main conditional: exitAccept if true / exitReject if false",
            peerExportConditions,
            ImmutableList.of(Statements.ExitAccept.toStaticStatement()),
            ImmutableList.of(Statements.ExitReject.toStaticStatement()));
    peerExportPolicy.addStatement(peerExportConditional);
    Disjunction localOrCommonOrigination = new Disjunction();
    peerExportConditions.getConjuncts().add(localOrCommonOrigination);
    localOrCommonOrigination
        .getDisjuncts()
        .add(new CallExpr(computeBgpCommonExportPolicyName(proc.getName())));
    String outboundRouteMapName = neighbor.getIpv4AddressFamily().getRouteMapOut();
    if (outboundRouteMapName != null) {
      RouteMap outboundRouteMap = _routeMaps.get(outboundRouteMapName);
      if (outboundRouteMap != null) {
        peerExportConditions.getConjuncts().add(new CallExpr(outboundRouteMapName));
      } else {
        _w.redFlag(
            String.format(
                "Ignoring reference to missing outbound route-map: %s", outboundRouteMapName));
      }
    }

    BgpActivePeerConfig.Builder builder =
        BgpActivePeerConfig.builder()
            .setBgpProcess(newProc)
            .setDescription(neighbor.getDescription())
            .setExportPolicy(peerExportPolicy.build().getName())
            .setLocalAs(proc.getLocalAs())
            .setLocalIp(updateSource)
            .setPeerAddress(neighbor.getAddress())
            .setRemoteAs(neighbor.getRemoteAs());
    builder.build();
  }

  private void addNatRules(org.batfish.datamodel.Interface iface) {
    String ifaceName = iface.getName();
    iface.setIncomingTransformation(computeInterfaceIncomingTransformation(ifaceName));
    iface.setAdditionalArpIps(computeAdditionalArpIps(ifaceName));
  }

  private IpSpace computeAdditionalArpIps(String ifaceName) {
    Stream<IpSpace> virtualDnatIps =
        _virtuals.values().stream()
            .filter(
                virtual ->
                    !virtual.getVlansEnabled()
                        || virtual.getVlans().isEmpty()
                        || virtual.getVlans().contains(ifaceName))
            .flatMap(virtual -> _virtualAdditionalDnatArpIps.get(virtual.getName()).stream());
    Stream<IpSpace> virtualSnatIps =
        _virtuals.values().stream()
            .flatMap(virtual -> _virtualAdditionalSnatArpIps.get(virtual.getName()).stream());
    Stream<IpSpace> snatIps =
        _snats.values().stream()
            .filter(
                snat ->
                    !snat.getVlansEnabled()
                        || snat.getVlans().isEmpty()
                        || snat.getVlans().contains(ifaceName))
            .flatMap(snat -> _snatAdditionalArpIps.get(snat.getName()).stream());
    return AclIpSpace.union(
        Streams.concat(virtualDnatIps, virtualSnatIps, snatIps)
            .collect(ImmutableList.toImmutableList()));
  }

  private Transformation computeInterfaceIncomingTransformation(String ifaceName) {
    return ImmutableSortedMap.copyOf(_virtuals, Comparator.reverseOrder()).values().stream()
        .filter(
            virtual ->
                !virtual.getVlansEnabled()
                    || virtual.getVlans().isEmpty()
                    || virtual.getVlans().contains(ifaceName))
        .map(Virtual::getName)
        .map(_virtualIncomingTransformations::get)
        .filter(Objects::nonNull)
        .reduce(
            Transformation.when(FalseExpr.INSTANCE).build(),
            (transformation, otherTransformation) ->
                Transformation.when(otherTransformation.getGuard())
                    .apply(otherTransformation.getTransformationSteps())
                    .setOrElse(transformation)
                    .build());
  }

  private @Nonnull Stream<IpSpace> computeSnatAdditionalArpIps(Snat snat) {
    return Optional.ofNullable(snat.getSnatpool())
        .map(this::computeSnatPoolIps)
        .orElse(Stream.of());
  }

  private @Nonnull Stream<IpSpace> computeSnatPoolIps(String snatPoolName) {
    SnatPool snatPool = _snatPools.get(snatPoolName);
    if (snatPool == null) {
      // no SNAT pool, so no SNAT IPs
      return Stream.of();
    }
    return snatPool.getMembers().stream()
        .map(_snatTranslations::get)
        .filter(Objects::nonNull)
        .map(SnatTranslation::getAddress)
        .filter(Objects::nonNull)
        .map(Ip::toIpSpace);
  }

  private @Nonnull Set<IpSpace> computeVirtualDnatIps(Virtual virtual) {
    if (!_virtualIncomingTransformations.containsKey(virtual.getName())) {
      // Early exit if no incoming transformation for this virtual
      return ImmutableSet.of();
    }
    VirtualAddress virtualAddress = _virtualAddresses.get(virtual.getDestination());
    Ip destinationIp = virtualAddress.getAddress();
    Ip mask = virtualAddress.getMask();
    if (mask == null) {
      mask = Ip.MAX;
    }
    return ImmutableSet.of(Prefix.create(destinationIp, mask).toIpSpace());
  }

  private TransformationStep computeVirtualIncomingPoolMemberTransformation(PoolMember member) {
    return new ApplyAll(
        new AssignIpAddressFromPool(
            TransformationType.DEST_NAT,
            IpField.DESTINATION,
            ImmutableRangeSet.of(Range.singleton(member.getAddress()))),
        new AssignPortFromPool(
            TransformationType.DEST_NAT,
            PortField.DESTINATION,
            member.getPort(),
            member.getPort()));
  }

  private TransformationStep computeVirtualIncomingPoolTransformation(Pool pool) {
    return new ApplyAny(
        pool.getMembers().values().stream()
            .filter(member -> member.getAddress() != null) // IPv4 members only
            .map(this::computeVirtualIncomingPoolMemberTransformation)
            .collect(ImmutableList.toImmutableList()));
  }

  private @Nonnull Optional<Transformation> computeVirtualIncomingTransformation(Virtual virtual) {
    //// Perform DNAT if source IP is in range and destination IP and port match

    // Retrieve pool of addresses to which destination IP may be translated
    String poolName = virtual.getPool();
    if (poolName == null) {
      // Cannot translate without pool
      _w.redFlag(String.format("Cannot DNAT for virtual '%s' without pool", virtual.getName()));
      return Optional.empty();
    }
    Pool pool = _pools.get(poolName);
    if (pool == null) {
      // Cannot translate without pool
      _w.redFlag(
          String.format(
              "Cannot DNAT for virtual '%s' using missing pool: '%s'",
              virtual.getName(), poolName));
      return Optional.empty();
    }

    // Compute matching headers
    String destination = virtual.getDestination();
    if (destination == null) {
      // Cannot match without destination node
      _w.redFlag(String.format("Virtual '%s' is missing destination", virtual.getName()));
      return Optional.empty();
    }
    VirtualAddress virtualAddress = _virtualAddresses.get(destination);
    if (virtualAddress == null) {
      // Cannot match without destination virtual address
      _w.redFlag(
          String.format(
              "Virtual '%s' refers to missing destination '%s'", virtual.getName(), destination));
      return Optional.empty();
    }
    Ip destinationIp = virtualAddress.getAddress();
    if (destinationIp == null) {
      // Cannot match without destination IP (might be IPv6, so don't warn here)
      return Optional.empty();
    }
    Ip destinationMask = virtualAddress.getMask();
    if (destinationMask == null) {
      destinationMask = Ip.MAX;
    }
    Integer destinationPort = virtual.getDestinationPort();
    if (destinationPort == null) {
      // Cannot match without destination port
      _w.redFlag(String.format("Virtual '%s' is missing destination port", virtual.getName()));
      return Optional.empty();
    }
    Prefix source = virtual.getSource();
    if (source == null) {
      source = Prefix.ZERO;
    }
    AclLineMatchExpr matchCondition =
        new MatchHeaderSpace(
            HeaderSpace.builder()
                .setDstIps(Prefix.create(destinationIp, destinationMask).toIpSpace())
                .setDstPorts(ImmutableList.of(new SubRange(destinationPort, destinationPort)))
                .setSrcIps(source.toIpSpace())
                .build(),
            virtual.getName());
    // TODO: track information needed for SNAT in outgoing transformation
    // https://github.com/batfish/batfish/issues/3243
    return Optional.of(
        Transformation.when(matchCondition)
            .apply(computeVirtualIncomingPoolTransformation(pool))
            .build());
  }

  private @Nonnull Optional<Transformation> computeVirtualOutgoingTransformation(Virtual virtual) {
    // TODO: https://github.com/batfish/batfish/issues/3243
    assert virtual != null; // silence PMD
    return Optional.empty();
  }

  private @Nonnull Set<IpSpace> computeVirtualSnatIps(Virtual virtual) {
    return Optional.ofNullable(virtual.getSourceAddressTranslationPool())
        .map(this::computeSnatPoolIps)
        .orElse(Stream.of())
        .collect(ImmutableSet.toImmutableSet());
  }

  public @Nonnull Map<String, BgpProcess> getBgpProcesses() {
    return _bgpProcesses;
  }

  private @Nonnull Ip getBgpRouterId(BgpProcess proc) {
    Ip processRouterId = proc.getRouterId();
    return processRouterId != null
        ? processRouterId
        : _c.getAllInterfaces().values().stream()
            .map(org.batfish.datamodel.Interface::getAllAddresses)
            .flatMap(Collection::stream)
            .map(InterfaceAddress::getIp)
            .max(Ip::compareTo)
            .orElse(Ip.ZERO);
  }

  @Override
  public String getHostname() {
    return _hostname;
  }

  public @Nonnull Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public @Nonnull Map<String, Node> getNodes() {
    return _nodes;
  }

  public @Nonnull Map<String, Pool> getPools() {
    return _pools;
  }

  public @Nonnull Map<String, PrefixList> getPrefixLists() {
    return _prefixLists;
  }

  public @Nonnull Map<String, RouteMap> getRouteMaps() {
    return _routeMaps;
  }

  public @Nonnull Map<String, Self> getSelves() {
    return _selves;
  }

  public @Nonnull Map<String, SnatPool> getSnatPools() {
    return _snatPools;
  }

  public @Nonnull Map<String, Snat> getSnats() {
    return _snats;
  }

  public @Nonnull Map<String, SnatTranslation> getSnatTranslations() {
    return _snatTranslations;
  }

  private Ip getUpdateSource(BgpNeighbor neighbor, String updateSourceInterface) {
    Ip updateSource = null;
    if (updateSourceInterface != null) {
      org.batfish.datamodel.Interface sourceInterface =
          _c.getDefaultVrf().getInterfaces().get(updateSourceInterface);
      if (sourceInterface != null) {
        InterfaceAddress address = sourceInterface.getAddress();
        if (address != null) {
          Ip sourceIp = address.getIp();
          updateSource = sourceIp;
        } else {
          _w.redFlag(
              "bgp update source interface: '"
                  + updateSourceInterface
                  + "' not assigned an ip address");
        }
      }
    }
    if (updateSource == null) {
      _w.redFlag(
          "Could not determine update source for BGP neighbor: '" + neighbor.getName() + "'");
    }
    return updateSource;
  }

  public @Nonnull Map<String, VirtualAddress> getVirtualAddresses() {
    return _virtualAddresses;
  }

  public @Nonnull Map<String, Virtual> getVirtuals() {
    return _virtuals;
  }

  public @Nonnull Map<String, Vlan> getVlans() {
    return _vlans;
  }

  private void initSnatTransformations() {
    // TODO: outgoing transformations

    // additional ARP IPs
    _snatAdditionalArpIps =
        toImmutableMap(
            _snats,
            Entry::getKey,
            snatAdditionalArpIpsEntry ->
                computeSnatAdditionalArpIps(snatAdditionalArpIpsEntry.getValue())
                    .collect(ImmutableSet.toImmutableSet()));
  }

  private void initVirtualTransformations() {
    // incoming transformations
    ImmutableSortedMap.Builder<String, Transformation> virtualIncomingTransformations =
        ImmutableSortedMap.naturalOrder();
    _virtuals.forEach(
        (virtualName, virtual) ->
            computeVirtualIncomingTransformation(virtual)
                .ifPresent(
                    transformation ->
                        virtualIncomingTransformations.put(virtualName, transformation)));
    _virtualIncomingTransformations = virtualIncomingTransformations.build();
    _virtualAdditionalDnatArpIps =
        toImmutableMap(_virtuals, Entry::getKey, e -> computeVirtualDnatIps(e.getValue()));

    // outgoing transformations
    ImmutableSortedMap.Builder<String, Transformation> virtualOutgoingTransformations =
        ImmutableSortedMap.naturalOrder();
    _virtuals.forEach(
        (virtualName, virtual) ->
            computeVirtualOutgoingTransformation(virtual)
                .ifPresent(
                    transformation ->
                        virtualOutgoingTransformations.put(virtualName, transformation)));
    _virtualOutgoingTransformations = virtualOutgoingTransformations.build();
    _virtualAdditionalSnatArpIps =
        toImmutableMap(_virtuals, Entry::getKey, e -> computeVirtualSnatIps(e.getValue()));
  }

  private void markStructures() {
    markConcreteStructure(
        F5BigipStructureType.BGP_NEIGHBOR, F5BigipStructureUsage.BGP_NEIGHBOR_SELF_REFERENCE);
    markConcreteStructure(
        F5BigipStructureType.BGP_PROCESS, F5BigipStructureUsage.BGP_PROCESS_SELF_REFERENCE);
    markConcreteStructure(
        F5BigipStructureType.INTERFACE,
        F5BigipStructureUsage.INTERFACE_SELF_REFERENCE,
        F5BigipStructureUsage.VLAN_INTERFACE);
    markAbstractStructure(
        F5BigipStructureType.MONITOR,
        F5BigipStructureUsage.POOL_MONITOR,
        ImmutableList.of(F5BigipStructureType.MONITOR_HTTP, F5BigipStructureType.MONITOR_HTTPS));
    markConcreteStructure(
        F5BigipStructureType.MONITOR_HTTP, F5BigipStructureUsage.MONITOR_HTTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.MONITOR_HTTPS, F5BigipStructureUsage.MONITOR_HTTPS_DEFAULTS_FROM);
    markConcreteStructure(F5BigipStructureType.NODE, F5BigipStructureUsage.POOL_MEMBER);
    markAbstractStructure(
        F5BigipStructureType.PERSISTENCE,
        F5BigipStructureUsage.VIRTUAL_PERSIST_PERSISTENCE,
        ImmutableList.of(
            F5BigipStructureType.PERSISTENCE_SOURCE_ADDR, F5BigipStructureType.PERSISTENCE_SSL));
    markConcreteStructure(
        F5BigipStructureType.PERSISTENCE_SOURCE_ADDR,
        F5BigipStructureUsage.PERSISTENCE_SOURCE_ADDR_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PERSISTENCE_SSL, F5BigipStructureUsage.PERSISTENCE_SSL_DEFAULTS_FROM);
    markConcreteStructure(F5BigipStructureType.POOL, F5BigipStructureUsage.VIRTUAL_POOL);
    markConcreteStructure(
        F5BigipStructureType.PREFIX_LIST,
        F5BigipStructureUsage.ROUTE_MAP_MATCH_IPV4_ADDRESS_PREFIX_LIST);
    markAbstractStructure(
        F5BigipStructureType.PROFILE,
        F5BigipStructureUsage.VIRTUAL_PROFILE,
        ImmutableList.of(
            F5BigipStructureType.PROFILE_CLIENT_SSL,
            F5BigipStructureType.PROFILE_HTTP,
            F5BigipStructureType.PROFILE_ONE_CONNECT,
            F5BigipStructureType.PROFILE_OCSP_STAPLING_PARAMS,
            F5BigipStructureType.PROFILE_SERVER_SSL,
            F5BigipStructureType.PROFILE_TCP));
    markConcreteStructure(
        F5BigipStructureType.PROFILE_CLIENT_SSL,
        F5BigipStructureUsage.PROFILE_CLIENT_SSL_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_HTTP, F5BigipStructureUsage.PROFILE_HTTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_ONE_CONNECT,
        F5BigipStructureUsage.PROFILE_ONE_CONNECT_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_OCSP_STAPLING_PARAMS,
        F5BigipStructureUsage.PROFILE_OCSP_STAPLING_PARAMS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_SERVER_SSL,
        F5BigipStructureUsage.MONITOR_HTTPS_SSL_PROFILE,
        F5BigipStructureUsage.PROFILE_SERVER_SSL_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_TCP, F5BigipStructureUsage.PROFILE_TCP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.ROUTE_MAP,
        F5BigipStructureUsage.BGP_ADDRESS_FAMILY_REDISTRIBUTE_KERNEL_ROUTE_MAP,
        F5BigipStructureUsage.BGP_NEIGHBOR_IPV4_ROUTE_MAP_OUT,
        F5BigipStructureUsage.BGP_NEIGHBOR_IPV6_ROUTE_MAP_OUT);
    markConcreteStructure(F5BigipStructureType.RULE, F5BigipStructureUsage.VIRTUAL_RULES_RULE);
    markConcreteStructure(F5BigipStructureType.SELF, F5BigipStructureUsage.SELF_SELF_REFERENCE);
    markConcreteStructure(F5BigipStructureType.SNAT, F5BigipStructureUsage.SNAT_SELF_REFERENCE);
    markConcreteStructure(
        F5BigipStructureType.SNAT_TRANSLATION, F5BigipStructureUsage.SNATPOOL_MEMBERS_MEMBER);
    markConcreteStructure(
        F5BigipStructureType.SNATPOOL,
        F5BigipStructureUsage.SNAT_SNATPOOL,
        F5BigipStructureUsage.VIRTUAL_SOURCE_ADDRESS_TRANSLATION_POOL);
    markConcreteStructure(
        F5BigipStructureType.VIRTUAL, F5BigipStructureUsage.VIRTUAL_SELF_REFERENCE);
    markConcreteStructure(
        F5BigipStructureType.VIRTUAL_ADDRESS, F5BigipStructureUsage.VIRTUAL_DESTINATION);
    markConcreteStructure(
        F5BigipStructureType.VLAN,
        F5BigipStructureUsage.BGP_NEIGHBOR_UPDATE_SOURCE,
        F5BigipStructureUsage.SELF_VLAN,
        F5BigipStructureUsage.SNAT_VLANS_VLAN,
        F5BigipStructureUsage.VIRTUAL_VLANS_VLAN);
  }

  private void processSelf(Self self) {
    // Add addresses to appropriate VLAN interfaces.
    String vlanName = self.getVlan();
    if (vlanName == null) {
      return;
    }
    org.batfish.datamodel.Interface vlanIface = _c.getAllInterfaces().get(vlanName);
    if (vlanIface == null) {
      return;
    }
    InterfaceAddress address = self.getAddress();
    if (address == null) {
      // IPv6
      return;
    }
    vlanIface.setAddress(address);
    vlanIface.setAllAddresses(ImmutableSortedSet.of(address));
  }

  private void processVlanSettings(Vlan vlan) {
    // Configure interface switchport parameters.
    Integer tag = vlan.getTag();
    if (tag == null) {
      return;
    }
    _interfaces.keySet().stream()
        .map(ifaceName -> _c.getAllInterfaces().get(ifaceName))
        .filter(notNull())
        .forEach(
            iface -> {
              iface.setSwitchport(true);
              iface.setSwitchportMode(SwitchportMode.TRUNK);
              // TODO: something else for configs with no concept of native VLAN
              iface.setNativeVlan(null);
              iface.setAllowedVlans(iface.getAllowedVlans().union(IntegerSpace.of(tag)));
            });
  }

  @Override
  public void setHostname(String hostname) {
    _hostname = hostname;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    _format = format;
  }

  private @Nonnull List<Statement> toActions(RouteMapEntry entry) {
    ImmutableList.Builder<Statement> builder = ImmutableList.builder();
    entry.getSets().flatMap(set -> set.toStatements(_c, this, _w)).forEach(builder::add);
    return builder.add(toStatement(entry.getAction())).build();
  }

  private @Nonnull org.batfish.datamodel.BgpProcess toBgpProcess(BgpProcess proc) {
    org.batfish.datamodel.BgpProcess newProc = new org.batfish.datamodel.BgpProcess();
    newProc.setRouterId(getBgpRouterId(proc));

    // TODO: verify correct method of determining whether two AS-paths are equivalent
    newProc.setMultipathEquivalentAsPathMatchMode(MultipathEquivalentAsPathMatchMode.EXACT_PATH);

    /*
     * Create common BGP export policy. This policy encompasses:
     * - redistribution from other protocols
     */
    RoutingPolicy.Builder bgpCommonExportPolicy =
        RoutingPolicy.builder()
            .setOwner(_c)
            .setName(computeBgpCommonExportPolicyName(proc.getName()));
    // The body of the export policy is a huge disjunction over many reasons routes may be exported.
    Disjunction routesShouldBeExported = new Disjunction();
    bgpCommonExportPolicy.addStatement(
        new If(
            routesShouldBeExported,
            ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
            ImmutableList.of()));
    // This list of reasons to export a route will be built up over the remainder of this function.
    List<BooleanExpr> exportConditions = routesShouldBeExported.getDisjuncts();

    // Finally, the export policy ends with returning false: do not export unmatched routes.
    bgpCommonExportPolicy.addStatement(Statements.ReturnFalse.toStaticStatement()).build();

    // Export kernel routes that should be redistributed.
    BgpRedistributionPolicy redistributeProtocolPolicy =
        proc.getIpv4AddressFamily().getRedistributionPolicies().get(F5BigipRoutingProtocol.KERNEL);
    if (redistributeProtocolPolicy != null) {
      BooleanExpr weInterior = BooleanExprs.TRUE;
      Conjunction exportProtocolConditions = new Conjunction();
      exportProtocolConditions.setComment("Redistribute kernel routes into BGP");
      exportProtocolConditions.getConjuncts().add(new MatchProtocol(RoutingProtocol.KERNEL));
      String mapName = redistributeProtocolPolicy.getRouteMap();
      if (mapName != null) {
        RouteMap redistributeProtocolRouteMap = _routeMaps.get(mapName);
        if (redistributeProtocolRouteMap != null) {
          weInterior = new CallExpr(mapName);
        }
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);
      exportProtocolConditions.getConjuncts().add(we);
      exportConditions.add(exportProtocolConditions);
    }

    // Export BGP and IBGP routes.
    exportConditions.add(new MatchProtocol(RoutingProtocol.BGP));
    exportConditions.add(new MatchProtocol(RoutingProtocol.IBGP));

    proc.getNeighbors().values().stream()
        .filter(neighbor -> neighbor.getAddress() != null) // must be IPv4 peer
        .forEach(neighbor -> addActivePeer(neighbor, proc, newProc));
    return newProc;
  }

  private @Nonnull BooleanExpr toGuard(RouteMapEntry entry) {
    return new Conjunction(
        entry
            .getMatches()
            .map(match -> match.toBooleanExpr(_c, this, _w))
            .collect(ImmutableList.toImmutableList()));
  }

  private @Nonnull org.batfish.datamodel.Interface toInterface(Interface iface) {
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(iface.getName(), _c);
    Double speed = iface.getSpeed();
    newIface.setSpeed(speed);
    newIface.setBandwidth(firstNonNull(iface.getBandwidth(), speed, Interface.DEFAULT_BANDWIDTH));
    // Assume all interfaces are in default VRF for now
    newIface.setVrf(_c.getDefaultVrf());
    return newIface;
  }

  private @Nonnull org.batfish.datamodel.Interface toInterface(Vlan vlan) {
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(vlan.getName(), _c, InterfaceType.VLAN);
    // TODO: Possibly add dependencies on ports allowing this VLAN
    newIface.setVlan(vlan.getTag());
    newIface.setBandwidth(Interface.DEFAULT_BANDWIDTH);
    newIface.setVrf(_c.getDefaultVrf());
    return newIface;
  }

  /**
   * Converts {@code prefixList} to {@link Route6FilterList}. If {@code prefixList} contains IPv4
   * information, returns {@code null}.
   */
  private @Nullable Route6FilterList toRoute6FilterList(PrefixList prefixList) {
    Collection<PrefixListEntry> entries = prefixList.getEntries().values();
    if (entries.stream().map(PrefixListEntry::getPrefix).anyMatch(Objects::nonNull)) {
      return null;
    }
    String name = prefixList.getName();
    Route6FilterList output = new Route6FilterList(name);
    entries.stream()
        .map(entry -> entry.toRoute6FilterLine(_w, name))
        .filter(Objects::nonNull)
        .forEach(output::addLine);
    return output;
  }

  /**
   * Converts {@code prefixList} to {@link RouteFilterList}. If {@code prefixList} contains IPv6
   * information, returns {@code null}.
   */
  private @Nullable RouteFilterList toRouteFilterList(PrefixList prefixList) {
    Collection<PrefixListEntry> entries = prefixList.getEntries().values();
    if (entries.stream().map(PrefixListEntry::getPrefix6).anyMatch(Objects::nonNull)) {
      return null;
    }
    String name = prefixList.getName();
    RouteFilterList output = new RouteFilterList(name);
    entries.stream()
        .map(entry -> entry.toRouteFilterLine(_w, name))
        .filter(Objects::nonNull)
        .forEach(output::addLine);
    return output;
  }

  private @Nonnull RoutingPolicy toRoutingPolicy(RouteMap routeMap) {
    RoutingPolicy.Builder builder =
        RoutingPolicy.builder().setName(routeMap.getName()).setOwner(_c);
    // Warn about entries missing an action.
    routeMap.getEntries().values().stream()
        .filter(entry -> entry.getAction() == null)
        .forEach(
            entry ->
                _w.redFlag(
                    String.format(
                        "route-map: '%s' entry '%d' has no action",
                        routeMap.getName(), entry.getNum())));
    routeMap.getEntries().values().stream()
        .filter(entry -> entry.getAction() != null)
        .map(entry -> toRoutingPolicyStatement(entry))
        .forEach(builder::addStatement);
    return builder.addStatement(Statements.ReturnFalse.toStaticStatement()).build();
  }

  private @Nonnull Statement toRoutingPolicyStatement(RouteMapEntry entry) {
    return new If(toGuard(entry), toActions(entry));
  }

  private @Nonnull Statement toStatement(LineAction action) {
    switch (action) {
      case PERMIT:
        return Statements.ReturnTrue.toStaticStatement();
      case DENY:
        return Statements.ReturnFalse.toStaticStatement();
      default:
        throw new IllegalArgumentException(String.format("Invalid action: %s", action));
    }
  }

  private @Nonnull Configuration toVendorIndependentConfiguration() {
    _c = new Configuration(_hostname, _format);

    // store vendor-specific information
    _c.getVendorFamily()
        .setF5Bigip(
            F5BigipFamily.builder()
                .setPools(_pools)
                .setVirtuals(_virtuals)
                .setVirtualAddresses(_virtualAddresses)
                .build());

    // TODO: alter as behavior fleshed out
    _c.setDefaultCrossZoneAction(LineAction.PERMIT);
    _c.setDefaultInboundAction(LineAction.PERMIT);

    // Add default VRF
    _c.getVrfs().computeIfAbsent(DEFAULT_VRF_NAME, Vrf::new);

    // Add interfaces
    _interfaces.forEach(
        (name, iface) -> {
          org.batfish.datamodel.Interface newIface = toInterface(iface);
          _c.getAllInterfaces().put(name, newIface);
          // Assume all interfaces are in default VRF for now
          _c.getDefaultVrf().getInterfaces().put(name, newIface);
        });

    // Add VLAN interfaces
    _vlans.forEach(
        (name, vlan) -> {
          org.batfish.datamodel.Interface newIface = toInterface(vlan);
          _c.getAllInterfaces().put(name, newIface);
          // Assume all interfaces are in default VRF for now
          _c.getDefaultVrf().getInterfaces().put(name, newIface);
        });
    // Process vlans:
    _vlans.values().forEach(this::processVlanSettings);

    // Process selves:
    _selves.values().forEach(this::processSelf);

    // Convert valid IPv4 prefix-lists to RouteFilterLists
    _prefixLists.forEach(
        (name, prefixList) -> {
          RouteFilterList converted = toRouteFilterList(prefixList);
          if (converted != null) {
            _c.getRouteFilterLists().put(name, converted);
          }
        });

    // Convert valid IPv6 prefix-lists to Route6FilterLists
    _prefixLists.forEach(
        (name, prefixList) -> {
          Route6FilterList converted = toRoute6FilterList(prefixList);
          if (converted != null) {
            _c.getRoute6FilterLists().put(name, converted);
          }
        });

    // Warn about invalid prefix-lists
    _prefixLists.values().forEach(this::warnInvalidPrefixList);

    // Convert route-maps to RoutingPolicies
    _routeMaps.forEach(
        (name, routeMap) -> _c.getRoutingPolicies().put(name, toRoutingPolicy(routeMap)));

    if (!_bgpProcesses.isEmpty()) {
      BgpProcess proc =
          ImmutableSortedSet.copyOf(
                  Comparator.comparing(BgpProcess::getName), _bgpProcesses.values())
              .first();
      if (_bgpProcesses.size() > 1) {
        _w.redFlag(
            String.format(
                "Multiple BGP processes not supported. Only using first process alphabetically: '%s'",
                proc.getName()));
      }
      _c.getDefaultVrf().setBgpProcess(toBgpProcess(proc));
    }

    // Add kernel routes for each virtual-address if applicable
    _c.getDefaultVrf()
        .setKernelRoutes(
            _virtualAddresses.values().stream()
                .map(this::tryAddKernelRoute)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));

    // Create NAT transformation rules
    initSnatTransformations();
    initVirtualTransformations();
    _vlans.keySet().stream().map(_c.getAllInterfaces()::get).forEach(this::addNatRules);

    markStructures();

    return _c;
  }

  @Override
  public @Nonnull List<Configuration> toVendorIndependentConfigurations()
      throws VendorConversionException {
    return ImmutableList.of(toVendorIndependentConfiguration());
  }

  private @Nonnull Optional<KernelRoute> tryAddKernelRoute(VirtualAddress virtualAddress) {
    if (virtualAddress.getRouteAdvertisementMode() == RouteAdvertisementMode.DISABLED
        || virtualAddress.getAddress() == null
        || virtualAddress.getMask() == null) {
      return Optional.empty();
    }
    return Optional.of(
        new KernelRoute(Prefix.create(virtualAddress.getAddress(), virtualAddress.getMask())));
  }

  private void warnInvalidPrefixList(PrefixList prefixList) {
    if (prefixList.getEntries().values().stream()
        .anyMatch(entry -> entry.getPrefix() != null && entry.getPrefix6() != null)) {
      _w.redFlag(
          String.format(
              "prefix-list '%s' is invalid since it contains both IPv4 and IPv6 information",
              prefixList.getName()));
    }
  }
}

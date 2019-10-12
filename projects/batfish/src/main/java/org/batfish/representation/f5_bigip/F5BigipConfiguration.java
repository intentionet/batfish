package org.batfish.representation.f5_bigip;

import static com.google.common.base.Predicates.notNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.batfish.common.util.CollectionUtil.toImmutableMap;
import static org.batfish.datamodel.Configuration.DEFAULT_VRF_NAME;
import static org.batfish.representation.f5_bigip.F5NatUtil.orElseChain;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Streams;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.VendorConversionException;
import org.batfish.common.Warnings;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.ConcreteInterfaceAddress;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.KernelRoute;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.Names;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.flow.TransformationStep.TransformationType;
import org.batfish.datamodel.ospf.OspfInterfaceSettings;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.SelfNextHop;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetNextHop;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.transformation.ApplyAll;
import org.batfish.datamodel.transformation.ApplyAny;
import org.batfish.datamodel.transformation.AssignIpAddressFromPool;
import org.batfish.datamodel.transformation.AssignPortFromPool;
import org.batfish.datamodel.transformation.IpField;
import org.batfish.datamodel.transformation.Noop;
import org.batfish.datamodel.transformation.PortField;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.datamodel.transformation.TransformationStep;
import org.batfish.datamodel.vendor_family.f5_bigip.F5BigipFamily;
import org.batfish.datamodel.vendor_family.f5_bigip.RouteAdvertisementMode;
import org.batfish.datamodel.vendor_family.f5_bigip.Virtual;
import org.batfish.datamodel.vendor_family.f5_bigip.VirtualAddress;
import org.batfish.referencelibrary.AddressGroup;
import org.batfish.referencelibrary.ReferenceBook;
import org.batfish.vendor.VendorConfiguration;

/** Vendor-specific configuration for F5 BIG-IP device */
@ParametersAreNonnullByDefault
public class F5BigipConfiguration extends VendorConfiguration {

  // Ephemeral port range defined in https://support.f5.com/csp/article/K8246
  private static final TransformationStep ASSIGN_EPHEMERAL_SOURCE_PORT =
      new AssignPortFromPool(TransformationType.SOURCE_NAT, PortField.SOURCE, 1024, 65535);

  static final String REFBOOK_SOURCE_POOLS = "pools";
  static final String REFBOOK_SOURCE_VIRTUAL_ADDRESSES = "virtualAddresses";

  // https://techdocs.f5.com/content/kb/en-us/products/big-ip_ltm/manuals/related/ospf-commandreference-7-10-4/_jcr_content/pdfAttach/download/file.res/arm-ospf-command-reference-7-10-4.pdf
  private static final int DEFAULT_DEAD_INTERVAL_S = 40;

  // https://techdocs.f5.com/content/kb/en-us/products/big-ip_ltm/manuals/related/ospf-commandreference-7-10-4/_jcr_content/pdfAttach/download/file.res/arm-ospf-command-reference-7-10-4.pdf
  private static final int DEFAULT_HELLO_INTERVAL_S = 10;

  private static final double OSPF_REFERENCE_BANDWIDTH_CONVERSION_FACTOR = 1E6D; // bps per Mbps

  private static boolean appliesToVlan(Snat snat, String vlanName) {
    return !snat.getVlansEnabled()
        || snat.getVlans().isEmpty()
        || snat.getVlans().contains(vlanName);
  }

  private static boolean appliesToVlan(Virtual virtual, String vlanName) {
    return !virtual.getVlansEnabled()
        || virtual.getVlans().isEmpty()
        || virtual.getVlans().contains(vlanName);
  }

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

  /**
   * Return name of {@link RouteFilterList} generated from access-list with name {@code aclName}.
   */
  public static @Nonnull String computeAccessListRouteFilterName(String aclName) {
    return String.format("~access-list~%s~", aclName);
  }

  @VisibleForTesting
  public static @Nonnull String computeBgpCommonExportPolicyName(String bgpProcessName) {
    return String.format("~BGP_COMMON_EXPORT_POLICY:%s~", bgpProcessName);
  }

  @VisibleForTesting
  public static @Nonnull String computeBgpPeerExportPolicyName(
      String bgpProcessName, Ip peerAddress) {
    return String.format("~BGP_PEER_EXPORT_POLICY:%s:%s~", bgpProcessName, peerAddress);
  }

  @VisibleForTesting
  public static @Nonnull String computeInterfaceIncomingFilterName(String vlanName) {
    return String.format("~incoming_filter:%s~", vlanName);
  }

  private static @Nonnull Optional<HeaderSpace> computeVirtualAddressRejectIcmpHeaders(
      VirtualAddress virtualAddress) {
    Ip address = virtualAddress.getAddress();
    if (address == null) {
      // not IPv4
      return Optional.empty();
    }
    if (!Boolean.TRUE.equals(virtualAddress.getIcmpEchoDisabled())) {
      // nothing to reject
      return Optional.empty();
    }
    return Optional.of(
        HeaderSpace.builder()
            .setDstIps(address.toIpSpace())
            .setIcmpTypes(ImmutableList.of(SubRange.singleton(IcmpType.ECHO_REQUEST)))
            .setIcmpCodes(ImmutableList.of(SubRange.singleton(0)))
            .build());
  }

  private static boolean isEbgpSingleHop(BgpProcess proc, BgpNeighbor neighbor) {
    return !proc.getLocalAs().equals(neighbor.getRemoteAs()) && neighbor.getEbgpMultihop() == null;
  }

  private final Map<String, AccessList> _accessLists;
  private final @Nonnull Map<String, BgpProcess> _bgpProcesses;
  private transient Configuration _c;
  private transient Map<String, Virtual> _enabledVirtuals;
  private ConfigurationFormat _format;
  private String _hostname;
  private boolean _imish;
  private final Map<String, ImishInterface> _imishInterfaces;
  private transient Map<String, ImmutableList.Builder<IpAccessListLine>>
      _interfaceIncomingFilterLines;
  private final @Nonnull Map<String, Interface> _interfaces;
  private final @Nonnull Map<String, Node> _nodes;
  private @Nonnull List<String> _ntpServers;
  private final @Nonnull Map<String, OspfProcess> _ospfProcesses;
  private final @Nonnull Map<String, Pool> _pools;
  private final @Nonnull Map<String, PrefixList> _prefixLists;
  private final @Nonnull Map<String, RouteMap> _routeMaps;
  private final @Nonnull Map<String, Route> _routes;
  private final @Nonnull Map<String, Self> _selves;
  private transient Map<String, Set<IpSpace>> _snatAdditionalArpIps;
  private final @Nonnull Map<String, SnatPool> _snatPools;
  private final @Nonnull Map<String, Snat> _snats;
  private transient SortedMap<String, SimpleTransformation> _snatTransformations;
  private final @Nonnull Map<String, SnatTranslation> _snatTranslations;
  private final @Nonnull Map<String, Trunk> _trunks;
  private transient Map<String, Set<IpSpace>> _virtualAdditionalDnatArpIps;
  private transient Map<String, Set<IpSpace>> _virtualAdditionalSnatArpIps;
  private final @Nonnull Map<String, VirtualAddress> _virtualAddresses;
  private transient SortedMap<String, HeaderSpace> _virtualAddressRejectIcmpHeaders;
  private transient SortedMap<String, SimpleTransformation> _virtualIncomingTransformations;
  private transient Map<String, HeaderSpace> _virtualMatchedHeaders;
  private transient SortedMap<String, SimpleTransformation> _virtualOutgoingTransformations;
  private final @Nonnull Map<String, Virtual> _virtuals;
  private final @Nonnull Map<String, Vlan> _vlans;

  public F5BigipConfiguration() {
    _accessLists = new HashMap<>();
    _bgpProcesses = new HashMap<>();
    _imishInterfaces = new HashMap<>();
    _interfaces = new HashMap<>();
    _nodes = new HashMap<>();
    _ntpServers = ImmutableList.of();
    _ospfProcesses = new HashMap<>();
    _pools = new HashMap<>();
    _prefixLists = new HashMap<>();
    _routeMaps = new HashMap<>();
    _routes = new HashMap<>();
    _selves = new HashMap<>();
    _snats = new HashMap<>();
    _snatPools = new HashMap<>();
    _snatTranslations = new HashMap<>();
    _trunks = new HashMap<>();
    _virtualAddresses = new HashMap<>();
    _virtuals = new HashMap<>();
    _vlans = new HashMap<>();
  }

  private void addActivePeer(
      BgpNeighbor neighbor, BgpProcess proc, org.batfish.datamodel.BgpProcess newProc) {
    String peerGroupName = neighbor.getPeerGroup();
    if (peerGroupName != null) {
      BgpPeerGroup peerGroup = proc.getPeerGroups().get(peerGroupName);
      if (peerGroup != null) {
        neighbor.applyPeerGroup(peerGroup);
      }
    }
    Ip updateSource = getUpdateSource(proc, neighbor);

    RoutingPolicy.Builder peerExportPolicy =
        RoutingPolicy.builder()
            .setOwner(_c)
            .setName(computeBgpPeerExportPolicyName(proc.getName(), neighbor.getAddress()));

    // next-hop-self
    if (Boolean.TRUE.equals(neighbor.getNextHopSelf())) {
      peerExportPolicy.addStatement(new SetNextHop(SelfNextHop.getInstance()));
    }

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
    LongSpace remoteAsns =
        Optional.ofNullable(neighbor.getRemoteAs()).map(LongSpace::of).orElse(LongSpace.EMPTY);

    BgpActivePeerConfig.Builder builder =
        BgpActivePeerConfig.builder()
            .setBgpProcess(newProc)
            .setDescription(neighbor.getDescription())
            .setEbgpMultihop(neighbor.getEbgpMultihop() != null)
            .setLocalAs(proc.getLocalAs())
            .setLocalIp(updateSource)
            .setPeerAddress(neighbor.getAddress())
            .setRemoteAsns(remoteAsns)
            .setIpv4UnicastAddressFamily(
                Ipv4UnicastAddressFamily.builder()
                    .setExportPolicy(peerExportPolicy.build().getName())
                    .build());
    builder.build();
  }

  private void addIncomingFilter(org.batfish.datamodel.Interface vlanInterface) {
    String vlanName = vlanInterface.getName();
    vlanInterface.setIncomingFilter(computeInterfaceIncomingFilter(vlanName));
  }

  private void addIpAccessList(AccessList acl) {
    IpAccessList.builder()
        .setOwner(_c)
        .setLines(
            acl.getLines().stream()
                .map(AccessListLine::toIpAccessListLine)
                .collect(ImmutableList.toImmutableList()))
        .setName(acl.getName())
        .build();
  }

  private void addNatRules(org.batfish.datamodel.Interface vlanInterface) {
    String vlanName = vlanInterface.getName();
    vlanInterface.setIncomingTransformation(computeInterfaceIncomingTransformation(vlanName));
    vlanInterface.setOutgoingTransformation(computeInterfaceOutgoingTransformation(vlanName));
    vlanInterface.setAdditionalArpIps(computeAdditionalArpIps(vlanName));
  }

  private IpSpace computeAdditionalArpIps(String vlanName) {
    Stream<IpSpace> virtualDnatIps =
        _enabledVirtuals.values().stream()
            .filter(
                virtual ->
                    !virtual.getVlansEnabled()
                        || virtual.getVlans().isEmpty()
                        || virtual.getVlans().contains(vlanName))
            .flatMap(virtual -> _virtualAdditionalDnatArpIps.get(virtual.getName()).stream());
    Stream<IpSpace> virtualSnatIps =
        _enabledVirtuals.values().stream()
            .flatMap(virtual -> _virtualAdditionalSnatArpIps.get(virtual.getName()).stream());
    Stream<IpSpace> snatIps =
        _snats.values().stream()
            .filter(
                snat ->
                    !snat.getVlansEnabled()
                        || snat.getVlans().isEmpty()
                        || snat.getVlans().contains(vlanName))
            .flatMap(snat -> _snatAdditionalArpIps.get(snat.getName()).stream());
    return AclIpSpace.union(
        Streams.concat(virtualDnatIps, virtualSnatIps, snatIps)
            .collect(ImmutableList.toImmutableList()));
  }

  private @Nullable IpAccessList computeInterfaceIncomingFilter(String vlanName) {
    List<IpAccessListLine> lines =
        firstNonNull(
                _interfaceIncomingFilterLines.get(vlanName),
                ImmutableList.<IpAccessListLine>builder())
            .add(IpAccessListLine.ACCEPT_ALL)
            .build();
    return IpAccessList.builder()
        .setOwner(_c)
        .setName(computeInterfaceIncomingFilterName(vlanName))
        .setLines(lines)
        .build();
  }

  private @Nullable Transformation computeInterfaceIncomingTransformation(String vlanName) {
    ImmutableList.Builder<SimpleTransformation> applicableTransformations = ImmutableList.builder();
    _virtualIncomingTransformations.forEach(
        (virtualName, transformation) -> {
          if (appliesToVlan(_enabledVirtuals.get(virtualName), vlanName)) {
            applicableTransformations.add(transformation);
          }
        });
    return orElseChain(applicableTransformations.build());
  }

  private @Nonnull Stream<SimpleTransformation> computeInterfaceOutgoingSnatTransformations(
      String vlanName) {
    return _snatTransformations.entrySet().stream()
        .filter(
            snatTransformationEntry ->
                appliesToVlan(_snats.get(snatTransformationEntry.getKey()), vlanName))
        .map(Entry::getValue);
  }

  private @Nullable Transformation computeInterfaceOutgoingTransformation(String vlanName) {
    Stream<SimpleTransformation> virtualTransformations =
        _virtualOutgoingTransformations.values().stream();
    Stream<SimpleTransformation> snatTransformations =
        computeInterfaceOutgoingSnatTransformations(vlanName);
    // Note that these streams come from maps whose keys are sorted in reverse order. The first
    // transformation guard checked will be the condition for the virtual that is first
    // lexicographically. The last transformation guard checked will be the condition for the snat
    // that is last lexicographically. The final transformation is constructed from the constituent
    // stream from back to front.

    // TODO: Implement correct order of precedence for virtual matching
    // https://support.f5.com/csp/article/K14800
    return orElseChain(
        Stream.concat(snatTransformations, virtualTransformations)
            .collect(ImmutableList.toImmutableList()));
  }

  private @Nonnull Optional<TransformationStep> computeOutgoingSnatPoolTransformation(
      SnatPool snatPool) {
    RangeSet<Ip> pool =
        ImmutableRangeSet.copyOf(
            snatPool.getMembers().stream()
                .map(_snatTranslations::get)
                .filter(Objects::nonNull)
                .map(SnatTranslation::getAddress)
                .filter(Objects::nonNull)
                .map(Range::singleton)
                .collect(ImmutableList.toImmutableList()));
    return pool.isEmpty()
        ? Optional.empty()
        : Optional.of(
            new ApplyAll(
                ASSIGN_EPHEMERAL_SOURCE_PORT,
                new AssignIpAddressFromPool(TransformationType.SOURCE_NAT, IpField.SOURCE, pool)));
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

  private @Nonnull Optional<SimpleTransformation> computeSnatTransformation(Snat snat) {
    //// Perform SNAT if source IP is in range

    // Retrieve pool of addresses to which sourceIP may be translated
    String snatPoolName = snat.getSnatpool();
    if (snatPoolName == null) {
      // Cannot translate without pool
      _w.redFlag(String.format("Cannot SNAT for snat '%s' without snatpool", snat.getName()));
      return Optional.empty();
    }
    SnatPool snatPool = _snatPools.get(snatPoolName);
    if (snatPool == null) {
      // Cannot translate without pool
      _w.redFlag(
          String.format(
              "Cannot SNAT for snat '%s' using missing snatpool: '%s'",
              snat.getName(), snatPoolName));
      return Optional.empty();
    }
    if (!snat.getIpv6Origins().isEmpty()) {
      // IPv6, so nothing to do
      return Optional.empty();
    }
    if (snat.getIpv4Origins().isEmpty()) {
      _w.redFlag("Cannot SNAT for snat '%s' without origins", snat.getName());
      return Optional.empty();
    }

    // Compute matching headers
    IpSpace sourceIpSpace =
        AclIpSpace.union(
            snat.getIpv4Origins().keySet().stream().map(Prefix::toIpSpace).toArray(IpSpace[]::new));
    AclLineMatchExpr matchCondition =
        new MatchHeaderSpace(
            HeaderSpace.builder().setSrcIps(sourceIpSpace).build(), snat.getName());
    return computeOutgoingSnatPoolTransformation(snatPool)
        .map(step -> new SimpleTransformation(matchCondition, step));
  }

  private @Nonnull Set<IpSpace> computeVirtualDnatIps(Virtual virtual) {
    if (!_virtualIncomingTransformations.containsKey(virtual.getName())) {
      // Early exit if no incoming transformation for this virtual
      return ImmutableSet.of();
    }
    // No need to verify presence of destination, virtual-address, etc. since all this must have
    // been present for incoming transformation to have been populated.
    VirtualAddress virtualAddress = _virtualAddresses.get(virtual.getDestination());
    if (Boolean.TRUE.equals(virtualAddress.getArpDisabled())) {
      return ImmutableSet.of();
    }
    Ip destinationIp = virtualAddress.getAddress();
    Ip mask = firstNonNull(virtualAddress.getMask(), Ip.MAX);
    return ImmutableSet.of(Prefix.create(destinationIp, mask).toIpSpace());
  }

  private @Nonnull TransformationStep computeVirtualIncomingPoolMemberTransformation(
      PoolMember member, boolean translateAddress, boolean translatePort) {
    TransformationStep addressTranslation =
        translateAddress
            ? new AssignIpAddressFromPool(
                TransformationType.DEST_NAT,
                IpField.DESTINATION,
                ImmutableRangeSet.of(Range.singleton(member.getAddress())))
            : null;
    TransformationStep portTranslation =
        translatePort
            ? new AssignPortFromPool(
                TransformationType.DEST_NAT,
                PortField.DESTINATION,
                member.getPort(),
                member.getPort())
            : null;
    if (translateAddress && translatePort) {
      // pool
      return new ApplyAll(addressTranslation, portTranslation);
    } else if (translateAddress) {
      // pool
      return addressTranslation;
    } else if (translatePort) {
      // pool
      return portTranslation;
    } else {
      // ip-forward or just pool with weird options
      return Noop.NOOP_DEST_NAT;
    }
  }

  private @Nonnull TransformationStep computeVirtualIncomingPoolTransformation(
      Pool pool, boolean translateAddress, boolean translatePort) {
    return new ApplyAny(
        pool.getMembers().values().stream()
            .filter(member -> member.getAddress() != null) // IPv4 members only
            .map(
                member ->
                    computeVirtualIncomingPoolMemberTransformation(
                        member, translateAddress, translatePort))
            .collect(ImmutableList.toImmutableList()));
  }

  private @Nonnull Optional<SimpleTransformation> computeVirtualIncomingTransformation(
      Virtual virtual) {
    if (virtual.getReject()) {
      // No transformation.
      return Optional.empty();
    }

    //// Forward with possible DNAT if source IP is in range and destination IP and port match

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
      // Cannot match without destination IP (IPv6, so don't warn)
      return Optional.empty();
    }
    Integer destinationPort = virtual.getDestinationPort();
    if (destinationPort == null) {
      // Cannot match without destination port
      _w.redFlag(String.format("Virtual '%s' is missing destination port", virtual.getName()));
      return Optional.empty();
    }
    Ip destinationMask = firstNonNull(virtualAddress.getMask(), Ip.MAX);
    Prefix source = virtual.getSource();
    if (source == null) {
      source = Prefix.ZERO;
    }
    HeaderSpace.Builder headerSpace =
        HeaderSpace.builder()
            .setDstIps(Prefix.create(destinationIp, destinationMask).toIpSpace())
            .setDstPorts(ImmutableList.of(SubRange.singleton(destinationPort)))
            .setSrcIps(source.toIpSpace());
    IpProtocol protocol = virtual.getIpProtocol();
    if (protocol != null) {
      headerSpace.setIpProtocols(ImmutableList.of(protocol));
    }
    AclLineMatchExpr matchCondition = new MatchHeaderSpace(headerSpace.build(), virtual.getName());

    if (virtual.getIpForward()) {
      // stop here if we are forwarding without DNAT
      return Optional.of(new SimpleTransformation(matchCondition, Noop.NOOP_DEST_NAT));
    }

    // Retrieve pool of addresses to which destination IP may be translated
    String poolName = virtual.getPool();
    if (poolName == null) {
      // Cannot continue without action
      _w.redFlag(
          String.format(
              "Cannot install virtual '%s' without action; need either ip-forward, pool, or reject",
              virtual.getName()));
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

    // TODO: track information needed for SNAT in outgoing transformation
    // https://github.com/batfish/batfish/issues/3243
    return Optional.of(
        new SimpleTransformation(
            matchCondition,
            computeVirtualIncomingPoolTransformation(
                pool, virtual.getTranslateAddress(), virtual.getTranslatePort())));
  }

  private @Nonnull Optional<HeaderSpace> computeVirtualMatchedHeaders(Virtual virtual) {
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
    HeaderSpace.Builder headerSpace =
        HeaderSpace.builder()
            .setDstIps(Prefix.create(destinationIp, destinationMask).toIpSpace())
            .setDstPorts(ImmutableList.of(SubRange.singleton(destinationPort)))
            .setSrcIps(source.toIpSpace());
    IpProtocol protocol = virtual.getIpProtocol();
    if (protocol != null) {
      headerSpace.setIpProtocols(ImmutableList.of(protocol));
    }
    return Optional.of(headerSpace.build());
  }

  private @Nonnull Optional<SimpleTransformation> computeVirtualOutgoingTransformation(
      Virtual virtual) {
    String snatPoolName = virtual.getSourceAddressTranslationPool();
    if (snatPoolName == null) {
      // No transformation since no source-address-translation occurs
      return Optional.empty();
    }
    SnatPool snatPool = _snatPools.get(snatPoolName);
    if (snatPool == null) {
      // Undefined reference
      return Optional.empty();
    }
    // TODO: Replace FalseExpr.INSTANCE with condition that matches token set by incoming
    // transformation https://github.com/batfish/batfish/issues/3243
    return computeOutgoingSnatPoolTransformation(snatPool)
        .map(step -> new SimpleTransformation(FalseExpr.INSTANCE, step));
  }

  private @Nonnull Set<IpSpace> computeVirtualSnatIps(Virtual virtual) {
    return Optional.ofNullable(virtual.getSourceAddressTranslationPool())
        .map(this::computeSnatPoolIps)
        .orElse(Stream.of())
        .collect(ImmutableSet.toImmutableSet());
  }

  private void convertOspfProcesses() {
    _c.getDefaultVrf()
        .setOspfProcesses(
            _ospfProcesses.entrySet().stream()
                .collect(
                    ImmutableSortedMap.toImmutableSortedMap(
                        Comparator.naturalOrder(),
                        Entry::getKey,
                        e -> toOspfProcess(e.getValue()))));
  }

  public Map<String, AccessList> getAccessLists() {
    return _accessLists;
  }

  public @Nonnull Map<String, BgpProcess> getBgpProcesses() {
    return _bgpProcesses;
  }

  private @Nonnull Ip getBgpRouterId(BgpProcess proc) {
    Ip processRouterId = proc.getRouterId();
    return processRouterId != null
        ? processRouterId
        : _c.getAllInterfaces().values().stream()
            .map(org.batfish.datamodel.Interface::getAllConcreteAddresses)
            .flatMap(Collection::stream)
            .map(ConcreteInterfaceAddress::getIp)
            .max(Ip::compareTo)
            .orElse(Ip.ZERO);
  }

  @Override
  public String getHostname() {
    return _hostname;
  }

  /** Returns {@code true} iff the source of this configuration included an imish component */
  public boolean getImish() {
    return _imish;
  }

  public @Nonnull Map<String, ImishInterface> getImishInterfaces() {
    return _imishInterfaces;
  }

  public @Nonnull Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public @Nonnull Map<String, Node> getNodes() {
    return _nodes;
  }

  public @Nonnull Map<String, OspfProcess> getOspfProcesses() {
    return _ospfProcesses;
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

  public @Nonnull Map<String, Route> getRoutes() {
    return _routes;
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

  public @Nonnull Map<String, Trunk> getTrunks() {
    return _trunks;
  }

  private @Nullable Ip getUpdateSource(BgpProcess proc, BgpNeighbor neighbor) {
    Ip neighborAddress = neighbor.getAddress();
    if (neighborAddress == null || proc.getLocalAs() == null || neighbor.getRemoteAs() == null) {
      // Only compute for IPv4 neighbors for now.
      // Also skip if we are missing AS information.
      return null;
    }
    String updateSourceInterface = neighbor.getUpdateSource();
    if (!isEbgpSingleHop(proc, neighbor) && updateSourceInterface != null) {
      org.batfish.datamodel.Interface sourceInterface =
          _c.getDefaultVrf().getInterfaces().get(updateSourceInterface);
      if (sourceInterface != null) {
        ConcreteInterfaceAddress address = sourceInterface.getConcreteAddress();
        if (address != null) {
          return address.getIp();
        } else {
          _w.redFlag(
              String.format(
                  "BGP neighbor: '%s' update-source interface: '%s' not assigned an ip address",
                  neighbor.getName(), updateSourceInterface));
        }
      }
    }
    // Either the neighbor is eBGP single-hop, or no update-source was specified, or we failed to
    // get IP from update-source.
    // So try to get IP of an interface in same network as neighbor address.
    for (org.batfish.datamodel.Interface iface : _c.getDefaultVrf().getInterfaces().values()) {
      for (ConcreteInterfaceAddress interfaceAddress : iface.getAllConcreteAddresses()) {
        if (interfaceAddress.getPrefix().containsIp(neighborAddress)) {
          return interfaceAddress.getIp();
        }
      }
    }

    _w.redFlag("Could not determine update source for BGP neighbor: '" + neighbor.getName() + "'");
    return null;
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

  private void initInterfaceIncomingFilterLines() {
    _interfaceIncomingFilterLines = new HashMap<>();
    _virtualAddressRejectIcmpHeaders.forEach(
        (virtualAddressName, matchedHeaders) -> {
          IpAccessListLine line =
              IpAccessListLine.rejecting()
                  .setMatchCondition(new MatchHeaderSpace(matchedHeaders))
                  .setName(
                      String.format(
                          "virtual-address %s { icmp-echo disabled }", virtualAddressName))
                  .build();
          _vlans.keySet().stream()
              .forEach(
                  vlanName ->
                      _interfaceIncomingFilterLines
                          .computeIfAbsent(vlanName, n -> ImmutableList.builder())
                          .add(line));
        });
    _virtualMatchedHeaders.forEach(
        (virtualName, matchedHeaders) -> {
          Virtual virtual = _enabledVirtuals.get(virtualName);
          IpAccessListLine line =
              toIpAccessListLine(virtualName, matchedHeaders, virtual.getReject());
          _vlans.keySet().stream()
              .filter(vlanName -> appliesToVlan(virtual, vlanName))
              .forEach(
                  vlanName ->
                      _interfaceIncomingFilterLines
                          .computeIfAbsent(vlanName, n -> ImmutableList.builder())
                          .add(line));
        });
  }

  private void initSnatTransformations() {
    // Note the use of reverseOrder() comparator. This iteration order allows for efficient
    // construction of chained transformations later on.

    // transformations
    ImmutableSortedMap.Builder<String, SimpleTransformation> snatTransformations =
        ImmutableSortedMap.reverseOrder();
    _snats.forEach(
        (snatName, snat) ->
            computeSnatTransformation(snat)
                .ifPresent(transformation -> snatTransformations.put(snatName, transformation)));
    _snatTransformations = snatTransformations.build();

    // additional ARP IPs
    _snatAdditionalArpIps =
        toImmutableMap(
            _snats,
            Entry::getKey,
            snatAdditionalArpIpsEntry ->
                computeSnatAdditionalArpIps(snatAdditionalArpIpsEntry.getValue())
                    .collect(ImmutableSet.toImmutableSet()));
  }

  private void initVirtualMatchedHeaders() {
    // incoming transformations
    ImmutableSortedMap.Builder<String, HeaderSpace> virtualMatchedHeaders =
        ImmutableSortedMap.naturalOrder();
    _enabledVirtuals.forEach(
        (virtualName, virtual) ->
            computeVirtualMatchedHeaders(virtual)
                .ifPresent(
                    matchedHeaders -> virtualMatchedHeaders.put(virtualName, matchedHeaders)));
    _virtualMatchedHeaders = virtualMatchedHeaders.build();
  }

  private void initVirtualAddressRejectIcmpHeaders() {
    // incoming transformations
    ImmutableSortedMap.Builder<String, HeaderSpace> virtualAddressRejectIcmpHeaders =
        ImmutableSortedMap.naturalOrder();
    _virtualAddresses.forEach(
        (virtualAddressName, virtualAddress) ->
            computeVirtualAddressRejectIcmpHeaders(virtualAddress)
                .ifPresent(
                    matchedHeaders ->
                        virtualAddressRejectIcmpHeaders.put(virtualAddressName, matchedHeaders)));
    _virtualAddressRejectIcmpHeaders = virtualAddressRejectIcmpHeaders.build();
  }

  private void initVirtualTransformations() {
    // Note the use of reverseOrder() comparator. This iteration order allows for efficient
    // construction of chained transformations later on.

    // incoming transformations
    ImmutableSortedMap.Builder<String, SimpleTransformation> virtualIncomingTransformations =
        ImmutableSortedMap.reverseOrder();
    _enabledVirtuals.forEach(
        (virtualName, virtual) ->
            computeVirtualIncomingTransformation(virtual)
                .ifPresent(
                    transformation ->
                        virtualIncomingTransformations.put(virtualName, transformation)));
    _virtualIncomingTransformations = virtualIncomingTransformations.build();
    _virtualAdditionalDnatArpIps =
        toImmutableMap(_enabledVirtuals, Entry::getKey, e -> computeVirtualDnatIps(e.getValue()));

    // outgoing transformations
    ImmutableSortedMap.Builder<String, SimpleTransformation> virtualOutgoingTransformations =
        ImmutableSortedMap.reverseOrder();
    _enabledVirtuals.forEach(
        (virtualName, virtual) ->
            computeVirtualOutgoingTransformation(virtual)
                .ifPresent(
                    transformation ->
                        virtualOutgoingTransformations.put(virtualName, transformation)));
    _virtualOutgoingTransformations = virtualOutgoingTransformations.build();
    _virtualAdditionalSnatArpIps =
        toImmutableMap(_enabledVirtuals, Entry::getKey, e -> computeVirtualSnatIps(e.getValue()));
  }

  private boolean isReferencedByRouteMap(String aclName) {
    // Return true iff the named acl is referenced via route-map match ip address
    return Optional.ofNullable(_structureReferences.get(F5BigipStructureType.ACCESS_LIST))
        .map(byStructureName -> byStructureName.get(aclName))
        .map(byUsage -> byUsage.get(F5BigipStructureUsage.ROUTE_MAP_MATCH_IP_ADDRESS))
        .map(lines -> !lines.isEmpty())
        .orElse(false);
  }

  private void markStructures() {
    markConcreteStructure(
        F5BigipStructureType.ACCESS_LIST, F5BigipStructureUsage.ROUTE_MAP_MATCH_IP_ADDRESS);
    markConcreteStructure(
        F5BigipStructureType.BGP_NEIGHBOR, F5BigipStructureUsage.BGP_NEIGHBOR_SELF_REFERENCE);
    markConcreteStructure(
        F5BigipStructureType.BGP_PROCESS, F5BigipStructureUsage.BGP_PROCESS_SELF_REFERENCE);
    markConcreteStructure(F5BigipStructureType.DATA_GROUP_EXTERNAL);
    markConcreteStructure(F5BigipStructureType.DATA_GROUP_INTERNAL);
    markConcreteStructure(
        F5BigipStructureType.IMISH_INTERFACE,
        F5BigipStructureUsage.IMISH_INTERFACE_SELF_REFERENCE,
        F5BigipStructureUsage.OSPF_PASSIVE_INTERFACE);
    markConcreteStructure(
        F5BigipStructureType.INTERFACE,
        F5BigipStructureUsage.INTERFACE_SELF_REFERENCE,
        F5BigipStructureUsage.TRUNK_INTERFACE);
    markAbstractStructure(
        F5BigipStructureType.MONITOR,
        F5BigipStructureUsage.POOL_MONITOR,
        ImmutableList.of(
            F5BigipStructureType.MONITOR_DNS,
            F5BigipStructureType.MONITOR_GATEWAY_ICMP,
            F5BigipStructureType.MONITOR_HTTP,
            F5BigipStructureType.MONITOR_HTTPS,
            F5BigipStructureType.MONITOR_LDAP,
            F5BigipStructureType.MONITOR_TCP));
    markConcreteStructure(
        F5BigipStructureType.MONITOR_DNS, F5BigipStructureUsage.MONITOR_DNS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.MONITOR_GATEWAY_ICMP,
        F5BigipStructureUsage.MONITOR_GATEWAY_ICMP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.MONITOR_HTTP, F5BigipStructureUsage.MONITOR_HTTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.MONITOR_HTTPS, F5BigipStructureUsage.MONITOR_HTTPS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.MONITOR_LDAP, F5BigipStructureUsage.MONITOR_LDAP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.MONITOR_TCP, F5BigipStructureUsage.MONITOR_TCP_DEFAULTS_FROM);
    markConcreteStructure(F5BigipStructureType.NODE, F5BigipStructureUsage.POOL_MEMBER);
    markConcreteStructure(
        F5BigipStructureType.OSPF_PROCESS, F5BigipStructureUsage.OSPF_PROCESS_SELF_REFERENCE);
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
            F5BigipStructureType.PROFILE_ANALYTICS,
            F5BigipStructureType.PROFILE_CERTIFICATE_AUTHORITY,
            F5BigipStructureType.PROFILE_CLASSIFICATION,
            F5BigipStructureType.PROFILE_CLIENT_LDAP,
            F5BigipStructureType.PROFILE_CLIENT_SSL,
            F5BigipStructureType.PROFILE_DHCPV4,
            F5BigipStructureType.PROFILE_DHCPV6,
            F5BigipStructureType.PROFILE_DIAMETER,
            F5BigipStructureType.PROFILE_DNS,
            F5BigipStructureType.PROFILE_FASTHTTP,
            F5BigipStructureType.PROFILE_FASTL4,
            F5BigipStructureType.PROFILE_FIX,
            F5BigipStructureType.PROFILE_FTP,
            F5BigipStructureType.PROFILE_GTP,
            F5BigipStructureType.PROFILE_HTML,
            F5BigipStructureType.PROFILE_HTTP2,
            F5BigipStructureType.PROFILE_HTTP_COMPRESSION,
            F5BigipStructureType.PROFILE_HTTP,
            F5BigipStructureType.PROFILE_HTTP_PROXY_CONNECT,
            F5BigipStructureType.PROFILE_ICAP,
            F5BigipStructureType.PROFILE_ILX,
            F5BigipStructureType.PROFILE_IPOTHER,
            F5BigipStructureType.PROFILE_IPSECALG,
            F5BigipStructureType.PROFILE_MAP_T,
            F5BigipStructureType.PROFILE_MQTT,
            F5BigipStructureType.PROFILE_NETFLOW,
            F5BigipStructureType.PROFILE_OCSP_STAPLING_PARAMS,
            F5BigipStructureType.PROFILE_ONE_CONNECT,
            F5BigipStructureType.PROFILE_PCP,
            F5BigipStructureType.PROFILE_PPTP,
            F5BigipStructureType.PROFILE_QOE,
            F5BigipStructureType.PROFILE_RADIUS,
            F5BigipStructureType.PROFILE_REQUEST_ADAPT,
            F5BigipStructureType.PROFILE_REQUEST_LOG,
            F5BigipStructureType.PROFILE_RESPONSE_ADAPT,
            F5BigipStructureType.PROFILE_REWRITE,
            F5BigipStructureType.PROFILE_RTSP,
            F5BigipStructureType.PROFILE_SCTP,
            F5BigipStructureType.PROFILE_SERVER_LDAP,
            F5BigipStructureType.PROFILE_SERVER_SSL,
            F5BigipStructureType.PROFILE_SIP,
            F5BigipStructureType.PROFILE_SMTPS,
            F5BigipStructureType.PROFILE_SOCKS,
            F5BigipStructureType.PROFILE_SPLITSESSIONCLIENT,
            F5BigipStructureType.PROFILE_SPLITSESSIONSERVER,
            F5BigipStructureType.PROFILE_STATISTICS,
            F5BigipStructureType.PROFILE_STREAM,
            F5BigipStructureType.PROFILE_TCP_ANALYTICS,
            F5BigipStructureType.PROFILE_TCP,
            F5BigipStructureType.PROFILE_TFTP,
            F5BigipStructureType.PROFILE_TRAFFIC_ACCELERATION,
            F5BigipStructureType.PROFILE_UDP,
            F5BigipStructureType.PROFILE_WEB_ACCELERATION,
            F5BigipStructureType.PROFILE_WEB_SECURITY,
            F5BigipStructureType.PROFILE_WEBSOCKET,
            F5BigipStructureType.PROFILE_XML));
    markConcreteStructure(
        F5BigipStructureType.PROFILE_ANALYTICS,
        F5BigipStructureUsage.PROFILE_ANALYTICS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_CERTIFICATE_AUTHORITY,
        F5BigipStructureUsage.PROFILE_CERTIFICATE_AUTHORITY_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_CLASSIFICATION,
        F5BigipStructureUsage.PROFILE_CLASSIFICATION_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_CLIENT_LDAP,
        F5BigipStructureUsage.PROFILE_CLIENT_LDAP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_CLIENT_SSL,
        F5BigipStructureUsage.PROFILE_CLIENT_SSL_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_DHCPV4, F5BigipStructureUsage.PROFILE_DHCPV4_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_DHCPV6, F5BigipStructureUsage.PROFILE_DHCPV6_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_DIAMETER,
        F5BigipStructureUsage.PROFILE_DIAMETER_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_DNS, F5BigipStructureUsage.PROFILE_DNS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_FASTHTTP,
        F5BigipStructureUsage.PROFILE_FASTHTTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_FASTL4, F5BigipStructureUsage.PROFILE_FASTL4_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_FIX, F5BigipStructureUsage.PROFILE_FIX_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_FTP, F5BigipStructureUsage.PROFILE_FTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_GTP, F5BigipStructureUsage.PROFILE_GTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_HTML, F5BigipStructureUsage.PROFILE_HTML_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_HTTP2, F5BigipStructureUsage.PROFILE_HTTP2_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_HTTP_COMPRESSION,
        F5BigipStructureUsage.PROFILE_HTTP_COMPRESSION_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_HTTP, F5BigipStructureUsage.PROFILE_HTTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_HTTP_PROXY_CONNECT,
        F5BigipStructureUsage.PROFILE_HTTP_PROXY_CONNECT_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_ICAP, F5BigipStructureUsage.PROFILE_ICAP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_ILX, F5BigipStructureUsage.PROFILE_ILX_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_IPOTHER, F5BigipStructureUsage.PROFILE_IPOTHER_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_IPSECALG,
        F5BigipStructureUsage.PROFILE_IPSECALG_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_MAP_T, F5BigipStructureUsage.PROFILE_MAP_T_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_MQTT, F5BigipStructureUsage.PROFILE_MQTT_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_NETFLOW, F5BigipStructureUsage.PROFILE_NETFLOW_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_OCSP_STAPLING_PARAMS,
        F5BigipStructureUsage.PROFILE_OCSP_STAPLING_PARAMS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_ONE_CONNECT,
        F5BigipStructureUsage.PROFILE_ONE_CONNECT_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_PCP, F5BigipStructureUsage.PROFILE_PCP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_PPTP, F5BigipStructureUsage.PROFILE_PPTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_QOE, F5BigipStructureUsage.PROFILE_QOE_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_RADIUS, F5BigipStructureUsage.PROFILE_RADIUS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_REQUEST_ADAPT,
        F5BigipStructureUsage.PROFILE_REQUEST_ADAPT_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_REQUEST_LOG,
        F5BigipStructureUsage.PROFILE_REQUEST_LOG_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_RESPONSE_ADAPT,
        F5BigipStructureUsage.PROFILE_RESPONSE_ADAPT_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_REWRITE, F5BigipStructureUsage.PROFILE_REWRITE_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_RTSP, F5BigipStructureUsage.PROFILE_RTSP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_SCTP, F5BigipStructureUsage.PROFILE_SCTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_SERVER_LDAP,
        F5BigipStructureUsage.PROFILE_SERVER_LDAP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_SERVER_SSL,
        F5BigipStructureUsage.MONITOR_HTTPS_SSL_PROFILE,
        F5BigipStructureUsage.PROFILE_SERVER_SSL_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_SIP, F5BigipStructureUsage.PROFILE_SIP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_SMTPS, F5BigipStructureUsage.PROFILE_SMTPS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_SOCKS, F5BigipStructureUsage.PROFILE_SOCKS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_SPLITSESSIONCLIENT,
        F5BigipStructureUsage.PROFILE_SPLITSESSIONCLIENT_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_SPLITSESSIONSERVER,
        F5BigipStructureUsage.PROFILE_SPLITSESSIONSERVER_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_STATISTICS,
        F5BigipStructureUsage.PROFILE_STATISTICS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_STREAM, F5BigipStructureUsage.PROFILE_STREAM_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_TCP_ANALYTICS,
        F5BigipStructureUsage.PROFILE_TCP_ANALYTICS_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_TCP, F5BigipStructureUsage.PROFILE_TCP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_TFTP, F5BigipStructureUsage.PROFILE_TFTP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_TRAFFIC_ACCELERATION,
        F5BigipStructureUsage.PROFILE_TRAFFIC_ACCELERATION_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_UDP, F5BigipStructureUsage.PROFILE_UDP_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_WEB_ACCELERATION,
        F5BigipStructureUsage.PROFILE_WEB_ACCELERATION_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_WEB_SECURITY,
        F5BigipStructureUsage.PROFILE_WEB_SECURITY_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_WEBSOCKET,
        F5BigipStructureUsage.PROFILE_WEBSOCKET_DEFAULTS_FROM);
    markConcreteStructure(
        F5BigipStructureType.PROFILE_XML, F5BigipStructureUsage.PROFILE_XML_DEFAULTS_FROM);
    markConcreteStructure(F5BigipStructureType.ROUTE, F5BigipStructureUsage.ROUTE_SELF_REFERENCE);
    markConcreteStructure(
        F5BigipStructureType.ROUTE_MAP,
        F5BigipStructureUsage.BGP_ADDRESS_FAMILY_REDISTRIBUTE_KERNEL_ROUTE_MAP,
        F5BigipStructureUsage.BGP_NEIGHBOR_IPV4_ROUTE_MAP_OUT,
        F5BigipStructureUsage.BGP_NEIGHBOR_IPV6_ROUTE_MAP_OUT,
        F5BigipStructureUsage.BGP_PEER_GROUP_ROUTE_MAP_OUT,
        F5BigipStructureUsage.BGP_REDISTRIBUTE_KERNEL_ROUTE_MAP);
    markConcreteStructure(F5BigipStructureType.RULE);
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
    markAbstractStructure(
        F5BigipStructureType.VLAN_MEMBER_INTERFACE,
        F5BigipStructureUsage.VLAN_INTERFACE,
        ImmutableList.of(F5BigipStructureType.INTERFACE, F5BigipStructureType.TRUNK));
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
    ConcreteInterfaceAddress address = self.getAddress();
    if (address == null) {
      // IPv6
      return;
    }
    // keep the first address we encountered as primary
    if (vlanIface.getAddress() == null) {
      vlanIface.setAddress(address);
    }
    vlanIface.setAllAddresses(
        ImmutableSortedSet.<InterfaceAddress>naturalOrder()
            .addAll(vlanIface.getAllAddresses())
            .add(address)
            .build());
  }

  private void processVlanSettings(Vlan vlan) {
    // Configure interface switchport parameters.
    Integer tag = vlan.getTag();
    if (tag == null) {
      return;
    }
    vlan.getInterfaces().keySet().stream()
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

  public void setImish(boolean imish) {
    _imish = imish;
  }

  public void setNtpServers(List<String> ntpServers) {
    _ntpServers = ImmutableList.copyOf(ntpServers);
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
    int ebgpAdmin = RoutingProtocol.BGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int ibgpAdmin = RoutingProtocol.IBGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    org.batfish.datamodel.BgpProcess newProc =
        new org.batfish.datamodel.BgpProcess(getBgpRouterId(proc), ebgpAdmin, ibgpAdmin);

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
    exportConditions.add(new MatchProtocol(RoutingProtocol.BGP, RoutingProtocol.IBGP));

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
        org.batfish.datamodel.Interface.builder().setName(iface.getName()).setOwner(_c).build();
    Double speed = iface.getSpeed();
    newIface.setActive(!Boolean.TRUE.equals(iface.getDisabled()));
    newIface.setSpeed(speed);
    newIface.setBandwidth(firstNonNull(iface.getBandwidth(), speed, Interface.DEFAULT_BANDWIDTH));
    // Assume all interfaces are in default VRF for now
    newIface.setVrf(_c.getDefaultVrf());
    return newIface;
  }

  private org.batfish.datamodel.Interface toInterface(Trunk trunk) {
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(trunk.getName())
            .setOwner(_c)
            .setType(InterfaceType.AGGREGATED)
            .build();
    newIface.setDependencies(
        trunk.getInterfaces().stream()
            .filter(_interfaces::containsKey)
            .map(member -> new Dependency(member, DependencyType.AGGREGATE))
            .collect(ImmutableSet.toImmutableSet()));
    // Assume all interfaces are in default VRF for now
    newIface.setVrf(_c.getDefaultVrf());
    return newIface;
  }

  private @Nonnull org.batfish.datamodel.Interface toInterface(Vlan vlan) {
    org.batfish.datamodel.Interface newIface =
        org.batfish.datamodel.Interface.builder()
            .setName(vlan.getName())
            .setOwner(_c)
            .setType(InterfaceType.VLAN)
            .build();
    // TODO: Possibly add dependencies on ports allowing this VLAN
    newIface.setVlan(vlan.getTag());
    newIface.setBandwidth(Interface.DEFAULT_BANDWIDTH);
    newIface.setVrf(_c.getDefaultVrf());
    // Assume each interface has its own session info (sessions are not shared by interfaces).
    // That is, return flows can only enter the interface the forward flow exited in order to match
    // the session setup by the forward flow.
    // By default, F5 do not apply packet filters to established connections; but one can enable
    // packet filter for established connections. However, packet filters are not fully supported at
    // this point
    newIface.setFirewallSessionInterfaceInfo(
        new FirewallSessionInterfaceInfo(false, ImmutableList.of(newIface.getName()), null, null));
    return newIface;
  }

  private @Nonnull IpAccessListLine toIpAccessListLine(
      String virtualName, HeaderSpace matchedHeaders, boolean reject) {
    return IpAccessListLine.builder()
        .setMatchCondition(new MatchHeaderSpace(matchedHeaders))
        .setAction(reject ? LineAction.DENY : LineAction.PERMIT)
        .setName(virtualName)
        .build();
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

  private @Nonnull RouteFilterLine toRouteFilterLine(AccessListLine line) {
    Prefix prefix = line.getPrefix();
    return new RouteFilterLine(
        line.getAction(), prefix, new SubRange(prefix.getPrefixLength(), Prefix.MAX_PREFIX_LENGTH));
  }

  private @Nonnull RouteFilterList toRouteFilterList(AccessList accessList) {
    String name = accessList.getName();
    return new RouteFilterList(
        computeAccessListRouteFilterName(name),
        accessList.getLines().stream()
            .map(this::toRouteFilterLine)
            .collect(ImmutableList.toImmutableList()));
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

  /** Returns a {@link StaticRoute} if {code route} is valid, or else {@code null}. */
  private @Nullable StaticRoute toStaticRoute(Route route) {
    if (route.getGw() == null) {
      return null;
    }
    if (route.getNetwork() == null) {
      return null;
    }
    return StaticRoute.builder()
        .setAdministrativeCost(
            RoutingProtocol.STATIC.getDefaultAdministrativeCost(
                ConfigurationFormat.F5_BIGIP_STRUCTURED))
        .setMetric(Route.METRIC)
        .setNetwork(route.getNetwork())
        .setNextHopIp(route.getGw())
        .build();
  }

  private @Nonnull Configuration toVendorIndependentConfiguration() {
    _c = new Configuration(_hostname, _format);

    // store vendor-specific information
    _c.getVendorFamily()
        .setF5Bigip(
            F5BigipFamily.builder()
                .setPools(
                    _pools.entrySet().stream()
                        .collect(
                            ImmutableMap.toImmutableMap(Entry::getKey, e -> toPool(e.getValue()))))
                .setVirtuals(_virtuals)
                .setVirtualAddresses(_virtualAddresses)
                .build());

    // add a reference book for virtual addresses
    String virtualAddressesBookname =
        Names.generatedReferenceBook(_hostname, REFBOOK_SOURCE_VIRTUAL_ADDRESSES);
    _c.getGeneratedReferenceBooks()
        .put(
            virtualAddressesBookname,
            ReferenceBook.builder(virtualAddressesBookname)
                .setAddressGroups(
                    _virtualAddresses.values().stream()
                        .map(F5BigipConfiguration::toAddressGroup)
                        .collect(ImmutableList.toImmutableList()))
                .build());

    // add a reference book for pools
    String poolAddressBookname = Names.generatedReferenceBook(_hostname, REFBOOK_SOURCE_POOLS);
    _c.getGeneratedReferenceBooks()
        .put(
            poolAddressBookname,
            ReferenceBook.builder(poolAddressBookname)
                .setAddressGroups(
                    _pools.values().stream()
                        .map(F5BigipConfiguration::toAddressGroup)
                        .collect(ImmutableList.toImmutableList()))
                .build());

    // TODO: alter as behavior fleshed out
    _c.setDefaultCrossZoneAction(LineAction.PERMIT);
    _c.setDefaultInboundAction(LineAction.PERMIT);

    // initialize maps of enabled structures
    initEnabledVirtuals();

    // Add default VRF
    _c.getVrfs().computeIfAbsent(DEFAULT_VRF_NAME, Vrf::new);

    // Add access-lists
    _accessLists.values().forEach(this::addIpAccessList);

    // Convert access-lists referenced by route-maps to RouteFilterLists
    _accessLists.forEach(
        (name, acl) -> {
          if (!isReferencedByRouteMap(name)) {
            return;
          }
          RouteFilterList rfl = toRouteFilterList(acl);
          _c.getRouteFilterLists().put(rfl.getName(), rfl);
        });

    // Add interfaces
    _interfaces.forEach(
        (name, iface) -> {
          org.batfish.datamodel.Interface newIface = toInterface(iface);
          _c.getAllInterfaces().put(name, newIface);
          // Assume all interfaces are in default VRF for now
          _c.getDefaultVrf().getInterfaces().put(name, newIface);
        });

    // Add trunks
    _trunks.forEach(
        (name, trunk) -> {
          org.batfish.datamodel.Interface newIface = toInterface(trunk);
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

    convertOspfProcesses();

    // Add kernel routes for each virtual-address if applicable
    _c.getDefaultVrf()
        .setKernelRoutes(
            _virtualAddresses.values().stream()
                .map(this::tryAddKernelRoute)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));

    // Create interface filters
    initVirtualMatchedHeaders();
    initVirtualAddressRejectIcmpHeaders();
    initInterfaceIncomingFilterLines();
    _vlans.keySet().stream().map(_c.getAllInterfaces()::get).forEach(this::addIncomingFilter);

    // Create NAT transformation rules
    initSnatTransformations();
    initVirtualTransformations();
    _vlans.keySet().stream().map(_c.getAllInterfaces()::get).forEach(this::addNatRules);

    // NTP servers
    _c.setNtpServers(ImmutableSortedSet.copyOf(_ntpServers));

    // Static Routes
    _c.getDefaultVrf()
        .setStaticRoutes(
            _routes.values().stream()
                .map(this::toStaticRoute)
                .filter(Objects::nonNull)
                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
    _routes.values().forEach(this::warnIfInvalidRoute);

    markStructures();

    return _c;
  }

  private static @Nonnull org.batfish.datamodel.vendor_family.f5_bigip.Pool toPool(Pool pool) {
    return new org.batfish.datamodel.vendor_family.f5_bigip.Pool(
        pool.getDescription(),
        pool.getMembers().entrySet().stream()
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, e -> toPoolMember(e.getValue()))),
        pool.getMonitors(),
        pool.getName());
  }

  private static @Nonnull org.batfish.datamodel.vendor_family.f5_bigip.PoolMember toPoolMember(
      PoolMember poolMember) {
    return new org.batfish.datamodel.vendor_family.f5_bigip.PoolMember(
        poolMember.getAddress(),
        poolMember.getAddress6(),
        poolMember.getDescription(),
        poolMember.getName(),
        poolMember.getNode(),
        poolMember.getPort());
  }

  private org.batfish.datamodel.ospf.OspfProcess toOspfProcess(OspfProcess proc) {
    Ip routerId =
        proc.getRouterId() != null
            ? proc.getRouterId()
            : inferRouterId(_c.getDefaultVrf(), _w, "OSPF process " + proc.getName());
    return toOspfProcessBuilder(proc, proc.getName(), Configuration.DEFAULT_VRF_NAME)
        .setProcessId(proc.getName())
        .setRouterId(routerId)
        .build();
  }

  private @Nonnull org.batfish.datamodel.ospf.OspfProcess.Builder toOspfProcessBuilder(
      OspfProcess proc, String processName, String vrfName) {
    org.batfish.datamodel.ospf.OspfProcess.Builder builder =
        org.batfish.datamodel.ospf.OspfProcess.builder();

    // convert areas
    Multimap<Long, Prefix> prefixesByAreaId =
        ImmutableMultimap.copyOf(
            proc.getNetworks().entrySet().stream()
                .map(e -> Maps.immutableEntry(e.getValue(), e.getKey()))
                .collect(ImmutableList.toImmutableList()));
    builder
        .setAreas(
            proc.getAreas().values().stream()
                .collect(
                    ImmutableSortedMap.toImmutableSortedMap(
                        Comparator.naturalOrder(),
                        OspfArea::getId,
                        area ->
                            toOspfArea(
                                processName,
                                vrfName,
                                proc,
                                area,
                                prefixesByAreaId.get(area.getId())))))
        .setReferenceBandwidth(
            OSPF_REFERENCE_BANDWIDTH_CONVERSION_FACTOR * proc.getAutoCostReferenceBandwidthMbps());

    return builder;
  }

  private @Nonnull org.batfish.datamodel.ospf.OspfArea toOspfArea(
      String processName,
      String vrfName,
      OspfProcess proc,
      OspfArea area,
      Collection<Prefix> prefixes) {
    org.batfish.datamodel.ospf.OspfArea.Builder builder =
        org.batfish.datamodel.ospf.OspfArea.builder().setNumber(area.getId());
    builder.setInterfaces(computeAreaInterfaces(processName, vrfName, proc, area, prefixes));
    return builder.build();
  }

  private @Nonnull Set<String> computeAreaInterfaces(
      String processName,
      String vrfName,
      OspfProcess proc,
      OspfArea area,
      Collection<Prefix> prefixes) {
    org.batfish.datamodel.Vrf vrf = _c.getVrfs().get(vrfName);
    if (vrf == null) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<String> interfaces = ImmutableSet.builder();
    long areaId = area.getId();
    _selves
        .values()
        .forEach(
            self -> {
              Optional<Prefix> prefixOpt =
                  Optional.ofNullable(self.getAddress())
                      .map(ConcreteInterfaceAddress::getPrefix)
                      .filter(prefixes::contains);
              if (!prefixOpt.isPresent()) {
                // no address or prefix not included in area
                return;
              }
              String vlanName = self.getVlan();
              if (!_vlans.containsKey(vlanName)) {
                // vlan doesn't exist
                return;
              }

              String imishName = computeImishName(vlanName);
              boolean passive = proc.getPassiveInterfaces().contains(imishName);

              // Add to this area.
              interfaces.add(vlanName);
              OspfInterface ospf =
                  Optional.ofNullable(_imishInterfaces.get(imishName))
                      .map(ImishInterface::getOspf)
                      // If interface being added has no explicit OSPF configuration, use defaults
                      .orElseGet(() -> new OspfInterface());
              finalizeInterfaceOspfSettings(vlanName, areaId, processName, ospf, passive);
            });
    return interfaces.build();
  }

  private @Nonnull String computeImishName(String name) {
    return Iterables.getLast(Arrays.asList(name.split("/", -1)));
  }

  private void finalizeInterfaceOspfSettings(
      String ifaceName, long areaId, String processName, OspfInterface ospf, boolean passive) {
    org.batfish.datamodel.Interface newIface = _c.getAllInterfaces().get(ifaceName);
    OspfInterfaceSettings.Builder ospfSettings = OspfInterfaceSettings.builder();
    ospfSettings.setCost(ospf.getCost());
    ospfSettings.setEnabled(true);
    ospfSettings.setAreaName(areaId);
    ospfSettings.setProcess(processName);
    ospfSettings.setPassive(passive);
    ospfSettings.setNetworkType(toOspfNetworkType(ospf.getNetwork()));
    ospfSettings.setDeadInterval(firstNonNull(ospf.getDeadIntervalS(), DEFAULT_DEAD_INTERVAL_S));
    ospfSettings.setHelloInterval(firstNonNull(ospf.getHelloIntervalS(), DEFAULT_HELLO_INTERVAL_S));

    newIface.setOspfSettings(ospfSettings.build());
  }

  private @Nullable org.batfish.datamodel.ospf.OspfNetworkType toOspfNetworkType(
      OspfNetworkType type) {
    if (type == null) {
      return null;
    }
    switch (type) {
      case NON_BROADCAST:
        return org.batfish.datamodel.ospf.OspfNetworkType.NON_BROADCAST_MULTI_ACCESS;

      default:
        _w.redFlag(
            String.format(
                "Conversion of F5 BIG-IP OSPF network type '%s' is not handled.", type.toString()));
        return null;
    }
  }

  /**
   * Infers router ID on F5 BIG-IP when not configured in a routing process.
   *
   * <p>From linked documentation, omitting inapplicable text involving loopbacks:
   *
   * <p><em>...biggest IP Address among all of the interfaces is used as a Router ID.
   *
   * <p>Each VRF has separate Router ID. VRF's Router ID selection does not affect to other
   * VRF.</em>
   *
   * @see <a href="https://github.com/coreswitch/zebra/blob/master/docs/router-id.md">zebra docs</a>
   */
  @Nonnull
  static Ip inferRouterId(Vrf vrf, Warnings w, String processDesc) {
    Optional<Ip> highestIp =
        vrf.getInterfaces().values().stream()
            .map(org.batfish.datamodel.Interface::getConcreteAddress)
            .filter(Objects::nonNull)
            .map(ConcreteInterfaceAddress::getIp)
            .max(Comparator.naturalOrder());
    if (!highestIp.isPresent()) {
      w.redFlag(
          String.format(
              "Router-id is not manually configured for %s in VRF %s. Unable to infer default router-id as no interfaces have IP addresses",
              processDesc, vrf.getName()));
      return Ip.ZERO;
    }
    return highestIp.get();
  }

  @VisibleForTesting
  static AddressGroup toAddressGroup(Pool pool) {
    return new AddressGroup(
        pool.getMembers().values().stream()
            .map(PoolMember::getAddress)
            .filter(Objects::nonNull)
            .map(Objects::toString)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())),
        pool.getName());
  }

  @VisibleForTesting
  static AddressGroup toAddressGroup(VirtualAddress virtualAddress) {
    return new AddressGroup(
        virtualAddress.getAddress() == null
            ? ImmutableSortedSet.of()
            : virtualAddress.getMask() == null
                ? ImmutableSortedSet.of(virtualAddress.getAddress().toString())
                : ImmutableSortedSet.of(
                    virtualAddress.getAddress().toString(),
                    Prefix.create(virtualAddress.getAddress(), virtualAddress.getMask())
                        .toString()),
        virtualAddress.getName());
  }

  private void initEnabledVirtuals() {
    _enabledVirtuals =
        _virtuals.entrySet().stream()
            .filter(virtualEntry -> !Boolean.TRUE.equals(virtualEntry.getValue().getDisabled()))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  @Override
  public @Nonnull List<Configuration> toVendorIndependentConfigurations()
      throws VendorConversionException {
    return ImmutableList.of(toVendorIndependentConfiguration());
  }

  private @Nonnull Optional<KernelRoute> tryAddKernelRoute(VirtualAddress virtualAddress) {
    if (virtualAddress.getRouteAdvertisementMode() == RouteAdvertisementMode.DISABLED
        || virtualAddress.getAddress() == null) {
      return Optional.empty();
    }
    return Optional.of(
        new KernelRoute(
            Prefix.create(
                virtualAddress.getAddress(),
                Optional.ofNullable(virtualAddress.getMask()).orElse(Ip.MAX))));
  }

  private void warnIfInvalidRoute(Route route) {
    boolean ipv4Gw = route.getGw() != null;
    boolean ipv6Gw = route.getGw6() != null;
    boolean ipv4Network = route.getNetwork() != null;
    boolean ipv6Network = route.getNetwork6() != null;
    boolean ipv4 = ipv4Gw || ipv4Network;
    boolean ipv6 = ipv6Gw || ipv6Network;
    if (!ipv4Gw && !ipv6Gw) {
      _w.redFlag(
          String.format(
              "Cannot convert %s to static route because it is missing default gateway",
              route.getName()));
    }
    if (!ipv4Network && !ipv6Network) {
      _w.redFlag(
          String.format(
              "Cannot convert %s to static route because it is missing network", route.getName()));
    }
    if (ipv4 && ipv6) {
      _w.redFlag(
          String.format(
              "Cannot convert %s to static route because it has mixed IPv4 and IPv6 information",
              route.getName()));
    }
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

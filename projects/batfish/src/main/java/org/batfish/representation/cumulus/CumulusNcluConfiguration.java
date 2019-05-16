package org.batfish.representation.cumulus;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Comparator.naturalOrder;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.representation.cumulus.CumulusRoutingProtocol.VI_PROTOCOLS_MAP;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.VendorConversionException;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpUnnumberedPeerConfig;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.InterfaceType;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.LongSpace;
import org.batfish.datamodel.Mlag;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.bgp.EvpnAddressFamily;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.Layer2VniConfig;
import org.batfish.datamodel.bgp.Layer3VniConfig;
import org.batfish.datamodel.bgp.RouteDistinguisher;
import org.batfish.datamodel.bgp.community.ExtendedCommunity;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.Not;
import org.batfish.datamodel.routing_policy.expr.WithEnvironmentExpr;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.vendor_family.cumulus.CumulusFamily;
import org.batfish.vendor.VendorConfiguration;

/** A {@link VendorConfiguration} for the Cumulus NCLU configuration language. */
public class CumulusNcluConfiguration extends VendorConfiguration {

  @VisibleForTesting public static final String CUMULUS_CLAG_DOMAIN_ID = "~CUMULUS_CLAG_DOMAIN~";

  /**
   * Bandwidth cannot be determined from name alone, so we choose the following made-up plausible
   * value in absence of explicit information.
   */
  private static final double DEFAULT_PORT_BANDWIDTH = 10E9D;

  public static final int DEFAULT_STATIC_ROUTE_ADMINISTRATIVE_DISTANCE = 1;
  public static final int DEFAULT_STATIC_ROUTE_METRIC = 0;
  public static final String LOOPBACK_INTERFACE_NAME = "lo";
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

  public static @Nonnull String computeBgpCommonExportPolicyName(String vrfName) {
    return String.format("~BGP_COMMON_EXPORT_POLICY:%s~", vrfName);
  }

  @VisibleForTesting
  public static @Nonnull String computeBgpPeerExportPolicyName(
      String vrfName, String peerInterface) {
    return String.format("~BGP_PEER_EXPORT_POLICY:%s:%s~", vrfName, peerInterface);
  }

  private @Nullable BgpProcess _bgpProcess;
  private final @Nonnull Map<String, Bond> _bonds;
  private final @Nonnull Bridge _bridge;
  private transient Configuration _c;
  private @Nullable String _hostname;
  private final @Nonnull Map<String, Interface> _interfaces;
  private final @Nonnull List<Ip> _ipv4Nameservers;
  private final @Nonnull List<Ip6> _ipv6Nameservers;
  private final @Nonnull Loopback _loopback;
  private final @Nonnull Map<String, RouteMap> _routeMaps;
  private final @Nonnull Set<StaticRoute> _staticRoutes;
  private final @Nonnull Map<String, Vlan> _vlans;
  private final @Nonnull Map<String, Vrf> _vrfs;
  private final @Nonnull Map<String, Vxlan> _vxlans;

  public CumulusNcluConfiguration() {
    _bonds = new HashMap<>();
    _bridge = new Bridge();
    _interfaces = new HashMap<>();
    _ipv4Nameservers = new LinkedList<>();
    _ipv6Nameservers = new LinkedList<>();
    _loopback = new Loopback();
    _routeMaps = new HashMap<>();
    _staticRoutes = new HashSet<>();
    _vlans = new HashMap<>();
    _vrfs = new HashMap<>();
    _vxlans = new HashMap<>();
  }

  private void addInterfaceNeighbor(
      String peerInterface,
      BgpInterfaceNeighbor neighbor,
      @Nullable Long localAs,
      BgpVrf bgpVrf,
      org.batfish.datamodel.BgpProcess newProc) {
    String vrfName = bgpVrf.getVrfName();

    RoutingPolicy.Builder peerExportPolicy =
        RoutingPolicy.builder()
            .setOwner(_c)
            .setName(computeBgpPeerExportPolicyName(vrfName, peerInterface));

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
        .add(new CallExpr(computeBgpCommonExportPolicyName(vrfName)));

    BgpUnnumberedPeerConfig.Builder builder =
        BgpUnnumberedPeerConfig.builder()
            .setBgpProcess(newProc)
            .setExportPolicy(peerExportPolicy.build().getName())
            .setLocalAs(localAs)
            .setLocalIp(BgpProcess.BGP_UNNUMBERED_IP)
            .setPeerInterface(peerInterface)
            .setRemoteAsns(computeRemoteAsns(neighbor, localAs));
    builder.setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.instance());

    BgpL2vpnEvpnAddressFamily evpnConfig = bgpVrf.getL2VpnEvpn();
    // sadly, we allow localAs == null in VI datamodel above
    if (evpnConfig != null && localAs != null) {
      ImmutableSet.Builder<Layer2VniConfig> l2Vnis = ImmutableSet.builder();
      ImmutableSet.Builder<Layer3VniConfig> l3Vnis = ImmutableSet.builder();
      ImmutableMap.Builder<Integer, Integer> vniToIndexBuilder = ImmutableMap.builder();
      if (evpnConfig.getAdvertiseAllVni()) {
        CommonUtil.forEachWithIndex(
            // Keep indices in deterministic order
            ImmutableList.sortedCopyOf(
                Comparator.nullsLast(Comparator.comparing(Vxlan::getId)), _vxlans.values()),
            (index, vxlan) -> {
              if (vxlan.getId() == null) {
                return;
              }
              vniToIndexBuilder.put(vxlan.getId(), index);
              RouteDistinguisher rd = RouteDistinguisher.from(newProc.getRouterId(), index);
              ExtendedCommunity rt = ExtendedCommunity.target(localAs, vxlan.getId());
              if (vxlan.getLocalTunnelip() != null) {
                // Advertise L2 VNIs
                l2Vnis.add(new Layer2VniConfig(vxlan.getId(), bgpVrf.getVrfName(), rd, rt));
                if (evpnConfig.getAdvertiseDefaultGw()) {
                  // Advertise VTEP gateway IP address for the L2 VNI as type 3 route
                  l3Vnis.add(
                      new Layer3VniConfig(vxlan.getId(), bgpVrf.getVrfName(), rd, rt, false));
                }
              }
            });
      }
      Map<Integer, Integer> vniToIndex = vniToIndexBuilder.build();
      // Advertise the L3 VNI per vrf if one is configured
      assert _bgpProcess != null; // Since we are in neighbor conversion, this must be true
      Iterables.concat(ImmutableSet.of(_bgpProcess.getDefaultVrf()), _bgpProcess.getVrfs().values())
          .forEach(
              aBgpVrf -> {
                Vrf vrf = _vrfs.get(aBgpVrf.getVrfName());
                Integer l3Vni = vrf == null ? null : vrf.getVni();
                if (l3Vni != null) {
                  RouteDistinguisher rd =
                      RouteDistinguisher.from(
                          _c.getVrfs().get(vrf.getName()).getBgpProcess().getRouterId(),
                          vniToIndex.get(l3Vni));
                  ExtendedCommunity rt = ExtendedCommunity.target(localAs, l3Vni);
                  l3Vnis.add(
                      new Layer3VniConfig(
                          l3Vni,
                          aBgpVrf.getVrfName(),
                          rd,
                          rt,
                          evpnConfig.getAdvertiseIpv4Unicast() != null));
                }
              });

      builder.setEvpnAddressFamily(new EvpnAddressFamily(l2Vnis.build(), l3Vnis.build()));
    }
    builder.build();
  }

  private void applyBridgeSettings(
      InterfaceBridgeSettings bridge, org.batfish.datamodel.Interface newIface) {
    String name = newIface.getName();
    Integer access = bridge.getAccess();
    Integer ifacePvid = bridge.getPvid();
    IntegerSpace ifaceVids = bridge.getVids();
    if (!_bridge.getPorts().contains(name)) {
      if (access != null || ifacePvid != null || !ifaceVids.isEmpty()) {
        _w.redFlag(
            String.format(
                "No support for VLAN switching options on non-'bridge bridge' port: '%s'", name));
      }
      return;
    }
    newIface.setSwitchport(true);
    if (access != null) {
      // access
      newIface.setSwitchportMode(SwitchportMode.ACCESS);
      newIface.setAccessVlan(access);
      return;
    }
    // trunk
    newIface.setSwitchportMode(SwitchportMode.TRUNK);
    int nativeVlan = firstNonNull(ifacePvid, _bridge.getPvid());
    newIface.setNativeVlan(nativeVlan);
    newIface.setAllowedVlans(
        (!ifaceVids.isEmpty() ? ifaceVids : _bridge.getVids()).union(IntegerSpace.of(nativeVlan)));
  }

  private void applyCommonInterfaceSettings(
      Interface iface, org.batfish.datamodel.Interface newIface) {
    newIface.setActive(true);
    if (!iface.getIpAddresses().isEmpty()) {
      newIface.setAddress(iface.getIpAddresses().get(0));
    }
    newIface.setAllAddresses(iface.getIpAddresses());
  }

  /**
   * Create bidirectional associate between {@code vrf} and interface-like entities with vrf
   * assigned to {@code vrfName}.
   */
  private void assignInterfacesToVrf(
      org.batfish.datamodel.Vrf vrf, @Nonnull String assignedVrfName) {
    Stream<String> matchingBondNames =
        _bonds.entrySet().stream()
            .filter(bondEntry -> Objects.equals(bondEntry.getValue().getVrf(), assignedVrfName))
            .map(Entry::getKey);
    Stream<String> matchingInterfaceNames =
        _interfaces.entrySet().stream()
            .filter(
                interfaceEntry ->
                    Objects.equals(interfaceEntry.getValue().getVrf(), assignedVrfName))
            .map(Entry::getKey);
    Stream<String> matchingVlanNames =
        _vlans.entrySet().stream()
            .filter(
                interfaceEntry ->
                    Objects.equals(interfaceEntry.getValue().getVrf(), assignedVrfName))
            .map(Entry::getKey);
    Stream<String> matchingVrfLoopbackNames = Stream.of(assignedVrfName);
    Streams.concat(
            matchingBondNames, matchingInterfaceNames, matchingVlanNames, matchingVrfLoopbackNames)
        .map(_c.getAllInterfaces()::get)
        .forEach(
            iface -> {
              iface.setVrf(vrf);
              vrf.getInterfaces().put(iface.getName(), iface);
            });
  }

  private @Nonnull LongSpace computeRemoteAsns(
      BgpInterfaceNeighbor neighbor, @Nullable Long localAs) {
    if (neighbor.getRemoteAsType() == RemoteAsType.EXPLICIT) {
      Long remoteAs = neighbor.getRemoteAs();
      return remoteAs == null ? LongSpace.EMPTY : LongSpace.of(remoteAs);
    } else if (localAs == null) {
      return LongSpace.EMPTY;
    } else if (neighbor.getRemoteAsType() == RemoteAsType.EXTERNAL) {
      return BgpPeerConfig.ALL_AS_NUMBERS.difference(LongSpace.of(localAs));
    } else if (neighbor.getRemoteAsType() == RemoteAsType.INTERNAL) {
      return LongSpace.of(localAs);
    }
    throw new IllegalArgumentException(
        String.format("Invalid remote-as type: %s", neighbor.getRemoteAsType()));
  }

  private void convertBgpProcess() {
    if (_bgpProcess == null) {
      return;
    }
    // First pass: only core processes
    _c.getDefaultVrf()
        .setBgpProcess(toBgpProcess(Configuration.DEFAULT_VRF_NAME, _bgpProcess.getDefaultVrf()));
    // We make one VI process per VRF because our current datamodel requires it
    _bgpProcess
        .getVrfs()
        .forEach(
            (vrfName, bgpVrf) ->
                _c.getVrfs().get(vrfName).setBgpProcess(toBgpProcess(vrfName, bgpVrf)));
    /*
     * Second pass: Add neighbors.
     * Requires all VRFs & bgp processes in a VRF to be set in VI so that we can initialize address families
     * that access other VRFs (e.g., EVPN)
     */
    Iterables.concat(ImmutableSet.of(_bgpProcess.getDefaultVrf()), _bgpProcess.getVrfs().values())
        .forEach(
            bgpVrf -> {
              Long localAs = bgpVrf.getAutonomousSystem();
              bgpVrf
                  .getInterfaceNeighbors()
                  .forEach(
                      (peerInterface, neighbor) ->
                          addInterfaceNeighbor(
                              peerInterface,
                              neighbor,
                              localAs,
                              bgpVrf,
                              _c.getVrfs().get(bgpVrf.getVrfName()).getBgpProcess()));
            });

    // All interfaces involved in BGP unnumbered should reply to ARP for BGP_UNNUMBERED_IP
    Streams.concat(
            _bgpProcess.getDefaultVrf().getInterfaceNeighbors().keySet().stream(),
            _bgpProcess.getVrfs().values().stream()
                .flatMap(vrf -> vrf.getInterfaceNeighbors().keySet().stream()))
        .collect(ImmutableSet.toImmutableSet()).stream()
        .map(_c.getAllInterfaces()::get)
        .forEach(
            iface ->
                iface.setAdditionalArpIps(
                    AclIpSpace.union(
                        iface.getAdditionalArpIps(), BgpProcess.BGP_UNNUMBERED_IP.toIpSpace())));
  }

  private void convertBondInterfaces() {
    _bonds.forEach((name, bond) -> _c.getAllInterfaces().put(name, toInterface(bond)));
  }

  private void convertClags() {
    List<Interface> clagSourceInterfaces =
        _interfaces.values().stream()
            .filter(i -> i.getClag() != null)
            .collect(ImmutableList.toImmutableList());
    if (clagSourceInterfaces.isEmpty()) {
      return;
    }
    if (clagSourceInterfaces.size() > 1) {
      _w.redFlag(
          String.format(
              "CLAG configuration on multiple peering interfaces is unsupported: %s",
              clagSourceInterfaces.stream()
                  .map(Interface::getName)
                  .collect(ImmutableList.toImmutableList())));
      return;
    }
    Interface clagSourceInterface = clagSourceInterfaces.get(0);
    Ip peerAddress = clagSourceInterface.getClag().getPeerIp();
    String sourceInterfaceName = clagSourceInterface.getName();
    String peerInterfaceName = clagSourceInterface.getSuperInterfaceName();
    _c.setMlags(
        ImmutableMap.of(
            CUMULUS_CLAG_DOMAIN_ID,
            Mlag.builder()
                .setId(CUMULUS_CLAG_DOMAIN_ID)
                .setLocalInterface(sourceInterfaceName)
                .setPeerAddress(peerAddress)
                .setPeerInterface(peerInterfaceName)
                .build()));
  }

  private void convertDefaultVrf() {
    org.batfish.datamodel.Vrf defaultVrf =
        new org.batfish.datamodel.Vrf(Configuration.DEFAULT_VRF_NAME);
    defaultVrf.setStaticRoutes(
        _staticRoutes.stream()
            .map(StaticRoute::convert)
            .collect(ImmutableSortedSet.toImmutableSortedSet(naturalOrder())));
    // Add all unassigned interfaces to default VRF
    _c.getAllInterfaces()
        .forEach(
            (ifaceName, iface) -> {
              if (iface.getVrf() == null) {
                iface.setVrf(defaultVrf);
                defaultVrf.getInterfaces().put(ifaceName, iface);
              }
            });
    _c.getVrfs().put(Configuration.DEFAULT_VRF_NAME, defaultVrf);
  }

  private void convertDnsServers() {
    _c.setDnsServers(
        _ipv4Nameservers.stream()
            .map(Object::toString)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder())));
  }

  private void convertLoopback() {
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(LOOPBACK_INTERFACE_NAME, _c, InterfaceType.LOOPBACK);
    newIface.setActive(true);
    if (!_loopback.getAddresses().isEmpty()) {
      newIface.setAddress(_loopback.getAddresses().get(0));
    }
    newIface.setAllAddresses(_loopback.getAddresses());
    _c.getAllInterfaces().put(LOOPBACK_INTERFACE_NAME, newIface);
  }

  private void convertPhysicalInterfaces() {
    // create physical interfaces
    _interfaces.values().stream()
        .filter(i -> i.getType() == CumulusInterfaceType.PHYSICAL)
        .forEach(
            physicalInterface ->
                _c.getAllInterfaces()
                    .put(physicalInterface.getName(), toInterface(physicalInterface)));
    // create VI interface for every superinterface of a physical subinterface not explicitly
    // configured
    _interfaces.values().stream()
        .filter(i -> i.getType() == CumulusInterfaceType.PHYSICAL_SUBINTERFACE)
        .map(Interface::getSuperInterfaceName)
        .filter(Predicates.not(_interfaces::containsKey))
        .forEach(
            superInterfaceName ->
                _c.getAllInterfaces()
                    .put(
                        superInterfaceName,
                        toInterface(
                            new Interface(
                                superInterfaceName, CumulusInterfaceType.PHYSICAL, null, null))));
  }

  private void convertRouteMaps() {
    _routeMaps.forEach((name, routeMap) -> _c.getRoutingPolicies().put(name, toRouteMap(routeMap)));
  }

  private void convertSubinterfaces() {
    _interfaces.values().stream()
        .filter(
            i ->
                i.getType() == CumulusInterfaceType.BOND_SUBINTERFACE
                    || i.getType() == CumulusInterfaceType.PHYSICAL_SUBINTERFACE)
        .map(i -> toInterface(i, i.getSuperInterfaceName()))
        .forEach(newIface -> _c.getAllInterfaces().put(newIface.getName(), newIface));
  }

  private void convertVlanInterfaces() {
    _vlans.forEach((name, vlan) -> _c.getAllInterfaces().put(name, toInterface(vlan)));
  }

  private org.batfish.datamodel.Interface convertVrfLoopbackInterface(Vrf vrf) {
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(vrf.getName(), _c, InterfaceType.LOOPBACK);
    newIface.setActive(true);
    if (!vrf.getAddresses().isEmpty()) {
      newIface.setAddress(vrf.getAddresses().get(0));
    }
    newIface.setAllAddresses(vrf.getAddresses());
    return newIface;
  }

  private void convertVrfLoopbackInterfaces() {
    _vrfs.forEach((name, vrf) -> _c.getAllInterfaces().put(name, convertVrfLoopbackInterface(vrf)));
  }

  private void convertVrfs() {
    _vrfs.forEach(this::initVrf);
  }

  public @Nullable BgpProcess getBgpProcess() {
    return _bgpProcess;
  }

  public @Nonnull Map<String, Bond> getBonds() {
    return _bonds;
  }

  public @Nonnull Bridge getBridge() {
    return _bridge;
  }

  @Override
  public @Nullable String getHostname() {
    return _hostname;
  }

  public @Nonnull Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public @Nonnull List<Ip> getIpv4Nameservers() {
    return _ipv4Nameservers;
  }

  public @Nonnull List<Ip6> getIpv6Nameservers() {
    return _ipv6Nameservers;
  }

  public @Nonnull Loopback getLoopback() {
    return _loopback;
  }

  public @Nonnull Map<String, RouteMap> getRouteMaps() {
    return _routeMaps;
  }

  public @Nonnull Set<StaticRoute> getStaticRoutes() {
    return _staticRoutes;
  }

  public @Nonnull Map<String, Vlan> getVlans() {
    return _vlans;
  }

  public @Nonnull Map<String, Vrf> getVrfs() {
    return _vrfs;
  }

  public @Nonnull Map<String, Vxlan> getVxlans() {
    return _vxlans;
  }

  private void initVendorFamily() {
    _c.getVendorFamily()
        .setCumulus(
            CumulusFamily.builder()
                .setBridge(_bridge.toDataModel())
                .setInterfaceClagSettings(
                    _interfaces.entrySet().stream()
                        .filter(ifaceEntry -> ifaceEntry.getValue().getClag() != null)
                        .collect(
                            ImmutableSortedMap.toImmutableSortedMap(
                                Comparator.naturalOrder(),
                                Entry::getKey,
                                ifaceEntry -> ifaceEntry.getValue().getClag().toDataModel())))
                .build());
  }

  private void initVrf(String name, Vrf vrf) {
    org.batfish.datamodel.Vrf newVrf = new org.batfish.datamodel.Vrf(name);
    newVrf.setStaticRoutes(
        vrf.getStaticRoutes().stream()
            .map(StaticRoute::convert)
            .collect(ImmutableSortedSet.toImmutableSortedSet(naturalOrder())));
    assignInterfacesToVrf(newVrf, name);
    _c.getVrfs().put(name, newVrf);
  }

  private void markStructures() {
    markAbstractStructure(
        CumulusStructureType.ABSTRACT_INTERFACE,
        CumulusStructureUsage.BGP_NEIGHBOR_INTERFACE,
        ImmutableSet.of(
            CumulusStructureType.BOND,
            CumulusStructureType.INTERFACE,
            CumulusStructureType.LOOPBACK,
            CumulusStructureType.VLAN,
            CumulusStructureType.VRF));
    markAbstractStructure(
        CumulusStructureType.ABSTRACT_INTERFACE,
        CumulusStructureUsage.BRIDGE_PORT,
        ImmutableSet.of(
            CumulusStructureType.BOND,
            CumulusStructureType.INTERFACE,
            CumulusStructureType.LOOPBACK,
            CumulusStructureType.VLAN,
            CumulusStructureType.VRF));
    markAbstractStructure(
        CumulusStructureType.ABSTRACT_INTERFACE,
        CumulusStructureUsage.ROUTE_MAP_MATCH_INTERFACE,
        ImmutableSet.of(
            CumulusStructureType.BOND,
            CumulusStructureType.INTERFACE,
            CumulusStructureType.LOOPBACK,
            CumulusStructureType.VLAN,
            CumulusStructureType.VRF));
    markConcreteStructure(CumulusStructureType.BOND, CumulusStructureUsage.BOND_SELF_REFERENCE);
    markConcreteStructure(
        CumulusStructureType.INTERFACE,
        CumulusStructureUsage.BOND_SLAVE,
        CumulusStructureUsage.INTERFACE_SELF_REFERENCE);
    markConcreteStructure(CumulusStructureType.VLAN, CumulusStructureUsage.VLAN_SELF_REFERENCE);
    markConcreteStructure(
        CumulusStructureType.LOOPBACK, CumulusStructureUsage.LOOPBACK_SELF_REFERENCE);
    markConcreteStructure(
        CumulusStructureType.ROUTE_MAP,
        CumulusStructureUsage.BGP_IPV4_UNICAST_REDISTRIBUTE_CONNECTED_ROUTE_MAP,
        CumulusStructureUsage.BGP_IPV4_UNICAST_REDISTRIBUTE_STATIC_ROUTE_MAP);
    markConcreteStructure(
        CumulusStructureType.VRF,
        CumulusStructureUsage.BGP_VRF,
        CumulusStructureUsage.BOND_VRF,
        CumulusStructureUsage.INTERFACE_CLAG_BACKUP_IP_VRF,
        CumulusStructureUsage.INTERFACE_VRF,
        CumulusStructureUsage.VLAN_VRF,
        CumulusStructureUsage.VRF_SELF_REFERENCE);
    markConcreteStructure(CumulusStructureType.VXLAN, CumulusStructureUsage.VXLAN_SELF_REFERENCE);
  }

  public void setBgpProcess(@Nullable BgpProcess bgpProcess) {
    _bgpProcess = bgpProcess;
  }

  @Override
  public void setHostname(@Nullable String hostname) {
    _hostname = hostname;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {}

  private @Nonnull List<Statement> toActions(RouteMapEntry entry) {
    ImmutableList.Builder<Statement> builder = ImmutableList.builder();
    entry.getSets().flatMap(set -> set.toStatements(_c, this, _w)).forEach(builder::add);
    return builder.add(toStatement(entry.getAction())).build();
  }

  /**
   * Returns {@link org.batfish.datamodel.BgpProcess} for named {@code bgpVrf} if valid, or else
   * {@code null}.
   */
  private @Nullable org.batfish.datamodel.BgpProcess toBgpProcess(String vrfName, BgpVrf bgpVrf) {
    Ip routerId = bgpVrf.getRouterId();
    if (routerId == null) {
      if (_loopback.getConfigured() && !_loopback.getAddresses().isEmpty()) {
        routerId = _loopback.getAddresses().get(0).getIp();
      } else {
        _w.redFlag(
            String.format(
                "Cannot configure BGP session for vrf '%s' because router-id is missing", vrfName));
        return null;
      }
    }
    org.batfish.datamodel.BgpProcess newProc = new org.batfish.datamodel.BgpProcess(routerId);
    newProc.setMultipathEquivalentAsPathMatchMode(EXACT_PATH);
    newProc.setMultipathEbgp(false);
    newProc.setMultipathIbgp(false);

    /*
     * Create common BGP export policy. This policy permits:
     * - BGP and iBGP routes
     * - routes whose network matches a configured network statement
     * - routes whose protocol matches a configured protocol redistribution policy
     * and denies all other routes.
     */
    RoutingPolicy.builder()
        .setOwner(_c)
        .setName(computeBgpCommonExportPolicyName(vrfName))
        .setStatements(
            ImmutableList.of(
                new If(
                    new Disjunction(getBgpExportConditions(bgpVrf)),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
                    ImmutableList.of(Statements.ReturnFalse.toStaticStatement()))))
        .build();

    // Add networks from network statements to new process's origination space
    if (bgpVrf.getIpv4Unicast() != null) {
      bgpVrf.getIpv4Unicast().getNetworks().keySet().forEach(newProc::addToOriginationSpace);
    }

    return newProc;
  }

  private List<BooleanExpr> getBgpExportConditions(BgpVrf bgpVrf) {
    List<BooleanExpr> exportConditions = new ArrayList<>();

    // Always export BGP and iBGP routes
    exportConditions.add(new MatchProtocol(RoutingProtocol.BGP));
    exportConditions.add(new MatchProtocol(RoutingProtocol.IBGP));

    // If no IPv4 address family is not defined, there is no capability to explicitly advertise v4
    // networks or redistribute protocols, so no non-BGP routes can be exported.
    if (bgpVrf.getIpv4Unicast() == null) {
      return exportConditions;
    }

    // Add conditions to redistribute other protocols
    for (BgpRedistributionPolicy redistributeProtocolPolicy :
        bgpVrf.getIpv4Unicast().getRedistributionPolicies().values()) {

      // Get a match expression for the protocol to be redistributed
      CumulusRoutingProtocol protocol = redistributeProtocolPolicy.getProtocol();
      Disjunction matchProtocol =
          new Disjunction(
              VI_PROTOCOLS_MAP.get(protocol).stream()
                  .map(MatchProtocol::new)
                  .collect(ImmutableList.toImmutableList()));

      // Create a WithEnvironmentExpr with the redistribution route-map, if one is defined
      BooleanExpr weInterior = BooleanExprs.TRUE;
      String mapName = redistributeProtocolPolicy.getRouteMap();
      if (mapName != null && _routeMaps.keySet().contains(mapName)) {
        weInterior = new CallExpr(mapName);
      }
      BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weInterior, OriginType.INCOMPLETE);

      // Export routes that match the protocol and WithEnvironmentExpr
      Conjunction exportProtocolConditions = new Conjunction(ImmutableList.of(matchProtocol, we));
      exportProtocolConditions.setComment(
          String.format("Redistribute %s routes into BGP", protocol));
      exportConditions.add(exportProtocolConditions);
    }

    // create origination prefilter from listed advertised networks
    bgpVrf
        .getIpv4Unicast()
        .getNetworks()
        .forEach(
            (prefix, bgpNetwork) -> {
              BooleanExpr weExpr = BooleanExprs.TRUE;
              BooleanExpr we = bgpRedistributeWithEnvironmentExpr(weExpr, OriginType.IGP);
              Conjunction exportNetworkConditions = new Conjunction();
              exportNetworkConditions
                  .getConjuncts()
                  .add(
                      new MatchPrefixSet(
                          DestinationNetwork.instance(),
                          new ExplicitPrefixSet(new PrefixSpace(PrefixRange.fromPrefix(prefix)))));
              /*
              Don't need to explicitly exclude BGP and iBGP routes here because those routes will
              already be matched earlier in exportConditions (which are disjuncts).
               */
              exportNetworkConditions
                  .getConjuncts()
                  .add(new Not(new MatchProtocol(RoutingProtocol.AGGREGATE)));
              exportNetworkConditions.getConjuncts().add(we);
              exportConditions.add(exportNetworkConditions);
            });
    return exportConditions;
  }

  private @Nonnull BooleanExpr toGuard(RouteMapEntry entry) {
    return new Conjunction(
        entry
            .getMatches()
            .map(match -> match.toBooleanExpr(_c, this, _w))
            .collect(ImmutableList.toImmutableList()));
  }

  private @Nonnull org.batfish.datamodel.Interface toInterface(Bond bond) {
    String name = bond.getName();
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(name, _c, InterfaceType.AGGREGATED);

    bond.getSlaves().forEach(slave -> _c.getAllInterfaces().get(slave).setChannelGroup(name));
    newIface.setChannelGroupMembers(bond.getSlaves());
    newIface.setDependencies(
        bond.getSlaves().stream()
            .map(slave -> new Dependency(slave, DependencyType.AGGREGATE))
            .collect(ImmutableSet.toImmutableSet()));

    applyBridgeSettings(bond.getBridge(), newIface);

    newIface.setActive(true);
    if (!bond.getIpAddresses().isEmpty()) {
      newIface.setAddress(bond.getIpAddresses().get(0));
    }
    newIface.setAllAddresses(bond.getIpAddresses());

    newIface.setMlagId(bond.getClagId());

    return newIface;
  }

  private @Nonnull org.batfish.datamodel.Interface toInterface(Interface iface) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(name, _c, InterfaceType.PHYSICAL);
    applyCommonInterfaceSettings(iface, newIface);

    applyBridgeSettings(iface.getBridge(), newIface);

    // TODO: support explicitly-configured bandwidth
    newIface.setBandwidth(DEFAULT_PORT_BANDWIDTH);

    return newIface;
  }

  private @Nonnull org.batfish.datamodel.Interface toInterface(
      Interface iface, String superInterfaceName) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(name, _c, InterfaceType.LOGICAL);
    newIface.setDependencies(
        ImmutableSet.of(new Dependency(superInterfaceName, DependencyType.BIND)));
    newIface.setEncapsulationVlan(iface.getEncapsulationVlan());
    applyCommonInterfaceSettings(iface, newIface);
    return newIface;
  }

  private org.batfish.datamodel.Interface toInterface(Vlan vlan) {
    org.batfish.datamodel.Interface newIface =
        new org.batfish.datamodel.Interface(vlan.getName(), _c, InterfaceType.VLAN);
    newIface.setActive(true);
    newIface.setVlan(vlan.getVlanId());

    // Interface addreses
    if (!vlan.getAddresses().isEmpty()) {
      newIface.setAddress(vlan.getAddresses().get(0));
    }
    ImmutableSet.Builder<InterfaceAddress> allAddresses = ImmutableSet.builder();
    allAddresses.addAll(vlan.getAddresses());
    vlan.getAddressVirtuals().values().forEach(allAddresses::addAll);
    newIface.setAllAddresses(allAddresses.build());

    return newIface;
  }

  private @Nonnull RoutingPolicy toRouteMap(RouteMap routeMap) {
    RoutingPolicy.Builder builder =
        RoutingPolicy.builder().setName(routeMap.getName()).setOwner(_c);
    routeMap.getEntries().values().stream()
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
    _c = new Configuration(getHostname(), ConfigurationFormat.CUMULUS_NCLU);
    _c.setDefaultCrossZoneAction(LineAction.PERMIT);
    _c.setDefaultInboundAction(LineAction.PERMIT);

    convertPhysicalInterfaces();
    convertBondInterfaces();
    convertSubinterfaces();
    convertVlanInterfaces();
    convertLoopback();
    convertVrfLoopbackInterfaces();
    convertVrfs();
    convertDefaultVrf();
    convertRouteMaps();
    convertDnsServers();
    convertClags();
    convertBgpProcess();

    initVendorFamily();

    markStructures();

    warnDuplicateClagIds();

    return _c;
  }

  @Override
  public @Nonnull List<Configuration> toVendorIndependentConfigurations()
      throws VendorConversionException {
    return ImmutableList.of(toVendorIndependentConfiguration());
  }

  private void warnDuplicateClagIds() {
    Map<Integer, List<Bond>> clagBondsById = new HashMap<>();
    _bonds.values().stream()
        .filter(bond -> bond.getClagId() != null)
        .forEach(
            clagBond ->
                clagBondsById
                    .computeIfAbsent(clagBond.getClagId(), id -> new LinkedList<>())
                    .add(clagBond));
    clagBondsById.forEach(
        (id, clagBonds) -> {
          if (clagBonds.size() > 1) {
            _w.redFlag(
                String.format(
                    "clag-id %d is erroneously configured on more than one bond: %s",
                    id,
                    clagBonds.stream()
                        .map(Bond::getName)
                        .collect(ImmutableList.toImmutableList())));
          }
        });
  }
}

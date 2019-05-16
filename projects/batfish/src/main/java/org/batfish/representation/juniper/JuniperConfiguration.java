package org.batfish.representation.juniper;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.acl.AclLineMatchExprs.matchSrcInterface;
import static org.batfish.representation.juniper.JuniperStructureType.ADDRESS_BOOK;
import static org.batfish.representation.juniper.NatPacketLocation.interfaceLocation;
import static org.batfish.representation.juniper.NatPacketLocation.routingInstanceLocation;
import static org.batfish.representation.juniper.NatPacketLocation.zoneLocation;
import static org.batfish.representation.juniper.RoutingInformationBase.RIB_IPV4_UNICAST;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.collections4.list.TreeList;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.VendorConversionException;
import org.batfish.common.Warnings;
import org.batfish.common.util.CollectionUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AuthenticationKey;
import org.batfish.datamodel.AuthenticationKeyChain;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpAuthenticationAlgorithm;
import org.batfish.datamodel.BgpAuthenticationSettings;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig.Builder;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.FirewallSessionInterfaceInfo;
import org.batfish.datamodel.FlowState;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkeKeyType;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.IkePhase1Policy;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.Interface.Dependency;
import org.batfish.datamodel.Interface.DependencyType;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceMetadata;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.IpsecPeerConfig;
import org.batfish.datamodel.IpsecPhase2Policy;
import org.batfish.datamodel.IpsecPhase2Proposal;
import org.batfish.datamodel.IpsecStaticPeerConfig;
import org.batfish.datamodel.IsoAddress;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RegexCommunitySet;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SnmpServer;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.NotMatchExpr;
import org.batfish.datamodel.acl.OrMatchExpr;
import org.batfish.datamodel.acl.OriginatingFromDevice;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.bgp.Ipv4UnicastAddressFamily;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.batfish.datamodel.dataplane.rib.RibId;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.ospf.OspfAreaSummary;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.packet_policy.Drop;
import org.batfish.datamodel.packet_policy.PacketMatchExpr;
import org.batfish.datamodel.packet_policy.PacketPolicy;
import org.batfish.datamodel.packet_policy.Return;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.ConjunctionChain;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.FirstMatchChain;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySet;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchLocalRouteSourcePrefixLength;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.expr.PrefixExpr;
import org.batfish.datamodel.routing_policy.expr.PrefixSetExpr;
import org.batfish.datamodel.routing_policy.statement.CallStatement;
import org.batfish.datamodel.routing_policy.statement.Comment;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetCommunity;
import org.batfish.datamodel.routing_policy.statement.SetDefaultPolicy;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.transformation.Transformation;
import org.batfish.representation.juniper.BgpGroup.BgpGroupType;
import org.batfish.representation.juniper.Interface.OspfInterfaceType;
import org.batfish.representation.juniper.Zone.AddressBookType;
import org.batfish.vendor.VendorConfiguration;

public final class JuniperConfiguration extends VendorConfiguration {

  public static final String ACL_NAME_COMBINED_INCOMING = "~COMBINED_INCOMING_FILTER~";

  public static final String ACL_NAME_COMBINED_OUTGOING = "~COMBINED_OUTGOING_FILTER~";

  public static final String ACL_NAME_EXISTING_CONNECTION = "~EXISTING_CONNECTION~";

  public static final String ACL_NAME_GLOBAL_POLICY = "~GLOBAL_SECURITY_POLICY~";

  public static final String ACL_NAME_SCREEN = "~SCREEN~";

  public static final String ACL_NAME_SCREEN_INTERFACE = "~SCREEN_INTERFACE~";

  public static final String ACL_NAME_SCREEN_ZONE = "~SCREEN_ZONE~";

  public static final String ACL_NAME_SECURITY_POLICY = "~SECURITY_POLICIES_TO~";

  private static final IpAccessList ACL_EXISTING_CONNECTION =
      IpAccessList.builder()
          .setName(ACL_NAME_EXISTING_CONNECTION)
          .setLines(
              ImmutableList.of(
                  new IpAccessListLine(
                      LineAction.PERMIT,
                      new MatchHeaderSpace(
                          HeaderSpace.builder()
                              .setStates(ImmutableList.of(FlowState.ESTABLISHED))
                              .build()),
                      ACL_NAME_EXISTING_CONNECTION)))
          .build();

  private static final BgpAuthenticationAlgorithm DEFAULT_BGP_AUTHENTICATION_ALGORITHM =
      BgpAuthenticationAlgorithm.HMAC_SHA_1_96;

  private static final String DEFAULT_BGP_EXPORT_POLICY_NAME = "~DEFAULT_BGP_EXPORT_POLICY~";

  private static final String DEFAULT_BGP_IMPORT_POLICY_NAME = "~DEFAULT_BGP_IMPORT_POLICY~";

  private static final String DEFAULT_PSEUDO_PROTOCOL_IMPORT_POLICY_NAME =
      "~DEFAULT_PSEUDO_PROTOCOL_IMPORT_POLICY~";

  private static final String DEFAULT_REJECT_POLICY_NAME = "~DEFAULT_REJECT_POLICY~";

  private static final Map<RoutingProtocol, String> DEFAULT_IMPORT_POLICIES =
      ImmutableMap.<RoutingProtocol, String>builder()
          .put(RoutingProtocol.CONNECTED, DEFAULT_PSEUDO_PROTOCOL_IMPORT_POLICY_NAME)
          .put(RoutingProtocol.LOCAL, DEFAULT_PSEUDO_PROTOCOL_IMPORT_POLICY_NAME)
          .put(RoutingProtocol.AGGREGATE, DEFAULT_PSEUDO_PROTOCOL_IMPORT_POLICY_NAME)
          .put(RoutingProtocol.STATIC, DEFAULT_PSEUDO_PROTOCOL_IMPORT_POLICY_NAME)
          .put(RoutingProtocol.BGP, DEFAULT_BGP_IMPORT_POLICY_NAME)
          .put(RoutingProtocol.IBGP, DEFAULT_BGP_IMPORT_POLICY_NAME)
          .build();

  @VisibleForTesting static final int DEFAULT_ISIS_COST = 10;

  /** Maximum IS-IS route cost if wide-metrics-only is not set */
  @VisibleForTesting static final int MAX_ISIS_COST_WITHOUT_WIDE_METRICS = 63;

  private static final String FIRST_LOOPBACK_INTERFACE_NAME = "lo0";

  private static final long serialVersionUID = 1L;

  private static String communityRegexToJavaRegex(String regex) {
    String out = regex;
    out = out.replace(":*", ":.*");
    out = out.replaceFirst("^\\*", ".*");
    return out;
  }

  private final Set<StandardCommunity> _allStandardCommunities;

  Configuration _c;

  private transient Interface _lo0;

  private transient boolean _lo0Initialized;

  /** Map of policy name to routing instances referenced in the policy, in the order they appear */
  private transient Map<String, List<String>> _vrfReferencesInPolicies;

  private final Map<String, NodeDevice> _nodeDevices;

  private ConfigurationFormat _vendor;

  private Map<String, LogicalSystem> _logicalSystems;

  private LogicalSystem _masterLogicalSystem;

  public JuniperConfiguration() {
    _allStandardCommunities = new HashSet<>();
    _logicalSystems = new TreeMap<>();
    _masterLogicalSystem = new LogicalSystem("");
    _nodeDevices = new TreeMap<>();
  }

  private NavigableMap<String, AuthenticationKeyChain> convertAuthenticationKeyChains(
      Map<String, JuniperAuthenticationKeyChain> juniperAuthenticationKeyChains) {
    NavigableMap<String, AuthenticationKeyChain> authenticationKeyChains = new TreeMap<>();
    for (Entry<String, JuniperAuthenticationKeyChain> keyChainEntry :
        juniperAuthenticationKeyChains.entrySet()) {
      JuniperAuthenticationKeyChain juniperAuthenticationKeyChain = keyChainEntry.getValue();
      AuthenticationKeyChain authenticationKeyChain =
          new AuthenticationKeyChain(juniperAuthenticationKeyChain.getName());
      authenticationKeyChain.setDescription(juniperAuthenticationKeyChain.getDescription());
      authenticationKeyChain.setTolerance(juniperAuthenticationKeyChain.getTolerance());
      for (Entry<String, JuniperAuthenticationKey> keyEntry :
          juniperAuthenticationKeyChain.getKeys().entrySet()) {
        JuniperAuthenticationKey juniperAuthenticationKey = keyEntry.getValue();
        AuthenticationKey authenticationKey =
            new AuthenticationKey(juniperAuthenticationKey.getName());
        authenticationKey.setIsisAuthenticationAlgorithm(
            juniperAuthenticationKey.getIsisAuthenticationAlgorithm());
        authenticationKey.setIsisOption(juniperAuthenticationKey.getIsisOption());
        authenticationKey.setSecret(juniperAuthenticationKey.getSecret());
        authenticationKey.setStartTime(juniperAuthenticationKey.getStartTime());
        authenticationKeyChain.getKeys().put(keyEntry.getKey(), authenticationKey);
      }
      authenticationKeyChains.put(keyChainEntry.getKey(), authenticationKeyChain);
    }
    return authenticationKeyChains;
  }

  @Nullable
  private BgpProcess createBgpProcess(RoutingInstance routingInstance) {
    BgpGroup mg = routingInstance.getMasterBgpGroup();
    if (firstNonNull(mg.getDisable(), Boolean.FALSE)) {
      return null;
    }
    initDefaultBgpExportPolicy();
    initDefaultBgpImportPolicy();
    String vrfName = routingInstance.getName();
    Vrf vrf = _c.getVrfs().get(vrfName);
    Ip routerId = routingInstance.getRouterId();
    if (routerId == null) {
      routerId = _masterLogicalSystem.getDefaultRoutingInstance().getRouterId();
      if (routerId == null) {
        routerId = Ip.ZERO;
      }
    }
    BgpProcess proc = new BgpProcess(routerId);
    boolean multipathEbgp = false;
    boolean multipathIbgp = false;
    boolean multipathMultipleAs = false;
    boolean multipathEbgpSet = false;
    boolean multipathIbgpSet = false;
    boolean multipathMultipleAsSet = false;

    if (mg.getLocalAs() == null) {
      Long routingInstanceAs = routingInstance.getAs();
      if (routingInstanceAs == null) {
        routingInstanceAs = _masterLogicalSystem.getDefaultRoutingInstance().getAs();
      }
      if (routingInstanceAs != null) {
        mg.setLocalAs(routingInstanceAs);
      }
    }
    // Set default authentication algorithm if missing
    if (mg.getAuthenticationAlgorithm() == null) {
      mg.setAuthenticationAlgorithm(DEFAULT_BGP_AUTHENTICATION_ALGORITHM);
    }
    for (IpBgpGroup ig : routingInstance.getIpBgpGroups().values()) {
      ig.cascadeInheritance();
    }

    /*
     * For new BGP advertisements, i.e. those that are created from non-BGP
     * routes, an origin code must be set. By default, Juniper sets the origin
     * code to IGP.
     */
    If setOriginForNonBgp =
        new If(
            new Disjunction(
                ImmutableList.of(
                    new MatchProtocol(RoutingProtocol.BGP),
                    new MatchProtocol(RoutingProtocol.IBGP))),
            ImmutableList.of(),
            ImmutableList.of(new SetOrigin(new LiteralOrigin(OriginType.IGP, null))));

    /*
     * Juniper allows setting BGP communities for static routes. Rather than add communities to VI
     * routes, add statements to peer export policies that set the appropriate communities.
     */
    List<If> staticRouteCommunitySetters = getStaticRouteCommunitySetters(routingInstance);

    for (Entry<Prefix, IpBgpGroup> e : routingInstance.getIpBgpGroups().entrySet()) {
      Prefix prefix = e.getKey();
      IpBgpGroup ig = e.getValue();
      Builder<?, ?> neighbor;
      Long remoteAs = ig.getType() == BgpGroupType.INTERNAL ? ig.getLocalAs() : ig.getPeerAs();
      if (ig.getDynamic()) {
        neighbor = BgpPassivePeerConfig.builder().setPeerPrefix(prefix).setRemoteAs(remoteAs);
      } else {
        neighbor =
            BgpActivePeerConfig.builder().setPeerAddress(prefix.getStartIp()).setRemoteAs(remoteAs);
      }

      // route reflection
      Ip declaredClusterId = ig.getClusterId();
      if (declaredClusterId != null) {
        neighbor.setRouteReflectorClient(true);
        neighbor.setClusterId(declaredClusterId.asLong());
      } else {
        neighbor.setClusterId(routerId.asLong());
      }

      // multipath multiple-as
      boolean currentGroupMultipathMultipleAs = ig.getMultipathMultipleAs();
      if (multipathMultipleAsSet && currentGroupMultipathMultipleAs != multipathMultipleAs) {
        _w.redFlag(
            "Currently do not support mixed multipath-multiple-as/non-multipath-multiple-as bgp"
                + "groups on Juniper - FORCING NON-MULTIPATH-MULTIPLE-AS");
        multipathMultipleAs = false;
      } else {
        multipathMultipleAs = currentGroupMultipathMultipleAs;
        multipathMultipleAsSet = true;
      }

      String authenticationKeyChainName = ig.getAuthenticationKeyChainName();
      if (ig.getAuthenticationKeyChainName() != null) {
        if (!_c.getAuthenticationKeyChains().containsKey(authenticationKeyChainName)) {
          authenticationKeyChainName = null;
        } else if (ig.getAuthenticationKey() != null) {
          _w.redFlag(
              "Both authentication-key and authentication-key-chain specified for neighbor "
                  + ig.getRemoteAddress());
        }
      }
      BgpAuthenticationSettings bgpAuthenticationSettings = new BgpAuthenticationSettings();
      bgpAuthenticationSettings.setAuthenticationAlgorithm(ig.getAuthenticationAlgorithm());
      bgpAuthenticationSettings.setAuthenticationKey(ig.getAuthenticationKey());
      bgpAuthenticationSettings.setAuthenticationKeyChainName(authenticationKeyChainName);
      neighbor.setAuthenticationSettings(bgpAuthenticationSettings);
      Boolean ebgpMultihop = ig.getEbgpMultihop();
      if (ebgpMultihop == null) {
        ebgpMultihop = false;
      }
      neighbor.setEbgpMultihop(ebgpMultihop);
      neighbor.setEnforceFirstAs(firstNonNull(ig.getEnforceFirstAs(), Boolean.FALSE));

      // Check for loops in the following order:
      Integer loops =
          Stream.of(
                  ig.getLoops(),
                  routingInstance.getLoops(),
                  _masterLogicalSystem.getDefaultRoutingInstance().getLoops(),
                  0)
              .filter(Objects::nonNull)
              .findFirst()
              .get();

      boolean allowLocalAsIn = loops > 0;
      neighbor.setAllowLocalAsIn(allowLocalAsIn);
      Boolean advertisePeerAs = ig.getAdvertisePeerAs();
      if (advertisePeerAs == null) {
        advertisePeerAs = false;
      }
      neighbor.setAllowRemoteAsOut(advertisePeerAs);
      Boolean advertiseExternal = ig.getAdvertiseExternal();
      if (advertiseExternal == null) {
        advertiseExternal = false;
      }
      neighbor.setAdvertiseExternal(advertiseExternal);
      Boolean advertiseInactive = ig.getAdvertiseInactive();
      if (advertiseInactive == null) {
        advertiseInactive = false;
      }
      neighbor.setAdvertiseInactive(advertiseInactive);
      neighbor.setGroup(ig.getGroupName());

      // import policies
      String peerImportPolicyName = "~PEER_IMPORT_POLICY:" + ig.getRemoteAddress() + "~";
      neighbor.setImportPolicy(peerImportPolicyName);
      RoutingPolicy peerImportPolicy = new RoutingPolicy(peerImportPolicyName, _c);
      _c.getRoutingPolicies().put(peerImportPolicyName, peerImportPolicy);
      // default import policy is to accept
      peerImportPolicy.getStatements().add(new SetDefaultPolicy(DEFAULT_BGP_IMPORT_POLICY_NAME));
      peerImportPolicy.getStatements().add(Statements.SetDefaultActionAccept.toStaticStatement());
      List<BooleanExpr> importPolicyCalls = new ArrayList<>();
      ig.getImportPolicies()
          .forEach(
              importPolicyName -> {
                PolicyStatement importPolicy =
                    _masterLogicalSystem.getPolicyStatements().get(importPolicyName);
                if (importPolicy != null) {
                  setPolicyStatementReferent(importPolicyName);
                  CallExpr callPolicy = new CallExpr(importPolicyName);
                  importPolicyCalls.add(callPolicy);
                }
              });
      If peerImportPolicyConditional = new If();
      FirstMatchChain importPolicyChain = new FirstMatchChain(importPolicyCalls);
      peerImportPolicyConditional.setGuard(importPolicyChain);
      peerImportPolicy.getStatements().add(peerImportPolicyConditional);
      peerImportPolicyConditional
          .getTrueStatements()
          .add(Statements.ExitAccept.toStaticStatement());
      peerImportPolicyConditional
          .getFalseStatements()
          .add(Statements.ExitReject.toStaticStatement());

      // Apply rib groups
      if (ig.getRibGroup() != null) {
        neighbor.setAppliedRibGroup(
            toRibGroup(
                _masterLogicalSystem.getRibGroups().get(ig.getRibGroup()),
                ig.getType() == BgpGroupType.INTERNAL ? RoutingProtocol.IBGP : RoutingProtocol.BGP,
                _c,
                routingInstance.getName(),
                _w));
      }

      // export policies
      String peerExportPolicyName = computePeerExportPolicyName(ig.getRemoteAddress());
      neighbor.setExportPolicy(peerExportPolicyName);
      RoutingPolicy peerExportPolicy = new RoutingPolicy(peerExportPolicyName, _c);
      _c.getRoutingPolicies().put(peerExportPolicyName, peerExportPolicy);
      peerExportPolicy.getStatements().add(new SetDefaultPolicy(DEFAULT_BGP_EXPORT_POLICY_NAME));
      applyLocalRoutePolicy(routingInstance, peerExportPolicy);

      // Add route modifier statements
      peerExportPolicy.getStatements().add(setOriginForNonBgp);
      peerExportPolicy.getStatements().addAll(staticRouteCommunitySetters);

      List<BooleanExpr> exportPolicyCalls = new ArrayList<>();
      ig.getExportPolicies()
          .forEach(
              exportPolicyName -> {
                PolicyStatement exportPolicy =
                    _masterLogicalSystem.getPolicyStatements().get(exportPolicyName);
                if (exportPolicy != null) {
                  setPolicyStatementReferent(exportPolicyName);
                  CallExpr callPolicy = new CallExpr(exportPolicyName);
                  exportPolicyCalls.add(callPolicy);
                }
              });
      If peerExportPolicyConditional = new If();
      FirstMatchChain exportPolicyChain = new FirstMatchChain(exportPolicyCalls);
      peerExportPolicyConditional.setGuard(exportPolicyChain);
      peerExportPolicyConditional
          .getTrueStatements()
          .add(Statements.ExitAccept.toStaticStatement());
      peerExportPolicyConditional
          .getFalseStatements()
          .add(Statements.ExitReject.toStaticStatement());
      peerExportPolicy.getStatements().add(peerExportPolicyConditional);

      // inherit local-as
      neighbor.setLocalAs(ig.getLocalAs());
      if (ig.getLocalAs() == null) {
        _w.redFlag("Missing local-as for neighbor: " + ig.getRemoteAddress());
        continue;
      }

      /* Inherit multipath */
      if (ig.getType() == BgpGroupType.INTERNAL || ig.getType() == null) {
        boolean currentGroupMultipathIbgp = ig.getMultipath();
        if (multipathIbgpSet && currentGroupMultipathIbgp != multipathIbgp) {
          _w.redFlag(
              "Currently do not support mixed iBGP multipath/non-multipath bgp groups on Juniper "
                  + "- FORCING NON-MULTIPATH IBGP");
          multipathIbgp = false;
        } else {
          multipathIbgp = currentGroupMultipathIbgp;
          multipathIbgpSet = true;
        }
      }
      if (ig.getType() == BgpGroupType.EXTERNAL || ig.getType() == null) {
        boolean currentGroupMultipathEbgp = ig.getMultipath();
        if (multipathEbgpSet && currentGroupMultipathEbgp != multipathEbgp) {
          _w.redFlag(
              "Currently do not support mixed eBGP multipath/non-multipath bgp groups on Juniper "
                  + "- FORCING NON-MULTIPATH EBGP");
          multipathEbgp = false;
        } else {
          multipathEbgp = currentGroupMultipathEbgp;
          multipathEbgpSet = true;
        }
      }

      // TODO: implement better behavior than setting default metric to 0
      neighbor.setDefaultMetric(0);

      // TODO: find out if there is a juniper equivalent of cisco
      // send-community
      neighbor.setSendCommunity(true);

      // inherit update-source
      Ip localIp = ig.getLocalAddress();
      if (localIp == null) {
        // assign the ip of the interface that is likely connected to this
        // peer
        outerloop:
        for (org.batfish.datamodel.Interface iface : vrf.getInterfaces().values()) {
          for (InterfaceAddress address : iface.getAllAddresses()) {
            if (address.getPrefix().containsPrefix(prefix)) {
              localIp = address.getIp();
              break outerloop;
            }
          }
        }
      }
      if (localIp == null && _masterLogicalSystem.getDefaultAddressSelection()) {
        initFirstLoopbackInterface();
        if (_lo0 != null) {
          InterfaceAddress lo0Unit0Address = _lo0.getPrimaryAddress();
          if (lo0Unit0Address != null) {
            localIp = lo0Unit0Address.getIp();
          }
        }
      }
      if (localIp == null) {
        if (ig.getDynamic()) {
          _w.redFlag(
              "Could not determine local ip for bgp peering with neighbor prefix: " + prefix);
        } else {
          _w.redFlag(
              "Could not determine local ip for bgp peering with neighbor ip: "
                  + prefix.getStartIp());
        }
      } else {
        neighbor.setLocalIp(localIp);
      }
      neighbor.setBgpProcess(proc);
      neighbor.setIpv4UnicastAddressFamily(Ipv4UnicastAddressFamily.instance());
      neighbor.build();
    }
    proc.setMultipathEbgp(multipathEbgpSet);
    proc.setMultipathIbgp(multipathIbgp);
    MultipathEquivalentAsPathMatchMode multipathEquivalentAsPathMatchMode =
        multipathMultipleAs
            ? MultipathEquivalentAsPathMatchMode.PATH_LENGTH
            : MultipathEquivalentAsPathMatchMode.FIRST_AS;
    proc.setMultipathEquivalentAsPathMatchMode(multipathEquivalentAsPathMatchMode);

    return proc;
  }

  /**
   * For each static route in the given {@link RoutingInstance} that has at least one community set,
   * creates an {@link If} that matches that route (specifically, matches static routes with that
   * route's destination network), and sets communities for matching exported routes.
   */
  @Nonnull
  private static List<If> getStaticRouteCommunitySetters(@Nonnull RoutingInstance ri) {
    MatchProtocol matchStatic = new MatchProtocol(RoutingProtocol.STATIC);
    return ri.getRibs().get(RoutingInformationBase.RIB_IPV4_UNICAST).getStaticRoutes().values()
        .stream()
        .filter(route -> !route.getCommunities().isEmpty())
        .map(
            route -> {
              // Create matcher that matches routes that share this route's destination network
              PrefixExpr destNetworkMatcher = DestinationNetwork.instance();
              PrefixSetExpr destNetwork =
                  new ExplicitPrefixSet(new PrefixSpace(PrefixRange.fromPrefix(route.getPrefix())));
              MatchPrefixSet networkMatcher = new MatchPrefixSet(destNetworkMatcher, destNetwork);

              // When a matching static route is exported, set its communities
              return new If(
                  new Conjunction(ImmutableList.of(matchStatic, networkMatcher)),
                  ImmutableList.of(
                      new SetCommunity(new LiteralCommunitySet(route.getCommunities()))));
            })
        .collect(ImmutableList.toImmutableList());
  }

  public static String computePeerExportPolicyName(Prefix remoteAddress) {
    return "~PEER_EXPORT_POLICY:" + remoteAddress + "~";
  }

  private void applyLocalRoutePolicy(RoutingInstance routingInstance, RoutingPolicy targetPolicy) {
    boolean lan = routingInstance.getExportLocalRoutesLan();
    boolean ptp = routingInstance.getExportLocalRoutesPointToPoint();
    if (lan && ptp) {
      // All local routes are allowed, so no need for filter
      return;
    }
    BooleanExpr matchProtocol = new MatchProtocol(RoutingProtocol.LOCAL);
    BooleanExpr match;
    if (!lan && !ptp) {
      // No need to check length, since all local routes will be rejected
      match = matchProtocol;
    } else {
      SubRange rejectedLength =
          !lan
              ? new SubRange(0, Prefix.MAX_PREFIX_LENGTH - 2)
              : new SubRange(Prefix.MAX_PREFIX_LENGTH - 1, Prefix.MAX_PREFIX_LENGTH - 1);
      match =
          new Conjunction(
              ImmutableList.of(
                  matchProtocol, new MatchLocalRouteSourcePrefixLength(rejectedLength)));
    }
    targetPolicy
        .getStatements()
        .add(new If(match, ImmutableList.of(Statements.ExitReject.toStaticStatement())));
  }

  private IsisProcess createIsisProcess(RoutingInstance routingInstance, IsoAddress netAddress) {
    IsisProcess.Builder newProc = IsisProcess.builder();
    newProc.setNetAddress(netAddress);
    IsisSettings settings = _masterLogicalSystem.getDefaultRoutingInstance().getIsisSettings();
    for (String policyName : settings.getExportPolicies()) {
      RoutingPolicy policy = _c.getRoutingPolicies().get(policyName);
      if (policy == null) {
        continue;
      } else {
        // TODO: support IS-IS export policy-statements
      }
    }
    boolean level1 = settings.getLevel1Settings().getEnabled();
    boolean level2 = settings.getLevel2Settings().getEnabled();
    if (!level1 && !level2) {
      return null;
    }
    if (level1) {
      newProc.setLevel1(toIsisLevelSettings(settings.getLevel1Settings()));
    }
    if (level2) {
      newProc.setLevel2(toIsisLevelSettings(settings.getLevel2Settings()));
    }
    processIsisInterfaceSettings(routingInstance, level1, level2);

    // If overload is set with a timeout, just pretend overload isn't set at all
    if (settings.getOverload() && settings.getOverloadTimeout() == null) {
      newProc.setOverload(true);
    }
    newProc.setReferenceBandwidth(settings.getReferenceBandwidth());
    return newProc.build();
  }

  @VisibleForTesting
  void processIsisInterfaceSettings(
      RoutingInstance routingInstance, boolean level1, boolean level2) {
    _c.getVrfs()
        .get(routingInstance.getName())
        .getInterfaces()
        .forEach(
            (ifaceName, newIface) ->
                newIface.setIsis(
                    toIsisInterfaceSettings(
                        routingInstance.getIsisSettings(),
                        routingInstance.getInterfaces().get(ifaceName),
                        level1,
                        level2)));
  }

  private org.batfish.datamodel.isis.IsisInterfaceSettings toIsisInterfaceSettings(
      @Nonnull IsisSettings settings, Interface iface, boolean level1, boolean level2) {
    IsisInterfaceSettings interfaceSettings = iface.getIsisSettings();
    if (!interfaceSettings.getEnabled()) {
      return null;
    }
    // If a reference bandwidth is set, calculate default cost as (reference bandwidth) / (interface
    // bandwidth). This will get overridden later if IS-IS level settings have cost set explicitly.
    long defaultCost = DEFAULT_ISIS_COST;
    if (settings.getReferenceBandwidth() != null) {
      if (iface.getBandwidth() == 0) {
        _w.pedantic(
            String.format(
                "Cannot use IS-IS reference bandwidth for interface '%s' because interface bandwidth is 0.",
                iface.getName()));
      } else {
        defaultCost = Math.max((long) (settings.getReferenceBandwidth() / iface.getBandwidth()), 1);
      }
    }
    org.batfish.datamodel.isis.IsisInterfaceSettings.Builder newInterfaceSettingsBuilder =
        org.batfish.datamodel.isis.IsisInterfaceSettings.builder();
    if (level1) {
      newInterfaceSettingsBuilder.setLevel1(
          toIsisInterfaceLevelSettings(
              settings.getLevel1Settings(),
              interfaceSettings,
              interfaceSettings.getLevel1Settings(),
              defaultCost));
    }
    if (level2) {
      newInterfaceSettingsBuilder.setLevel2(
          toIsisInterfaceLevelSettings(
              settings.getLevel2Settings(),
              interfaceSettings,
              interfaceSettings.getLevel2Settings(),
              defaultCost));
    }
    return newInterfaceSettingsBuilder
        .setBfdLivenessDetectionMinimumInterval(
            interfaceSettings.getBfdLivenessDetectionMinimumInterval())
        .setBfdLivenessDetectionMultiplier(interfaceSettings.getBfdLivenessDetectionMultiplier())
        .setIsoAddress(iface.getIsoAddress())
        .setPointToPoint(interfaceSettings.getPointToPoint())
        .build();
  }

  private org.batfish.datamodel.isis.IsisInterfaceLevelSettings toIsisInterfaceLevelSettings(
      IsisLevelSettings levelSettings,
      IsisInterfaceSettings interfaceSettings,
      IsisInterfaceLevelSettings interfaceLevelSettings,
      long defaultCost) {
    long cost = firstNonNull(interfaceLevelSettings.getMetric(), defaultCost);
    if (!levelSettings.getWideMetricsOnly()) {
      cost = Math.min(cost, MAX_ISIS_COST_WITHOUT_WIDE_METRICS);
    }
    return org.batfish.datamodel.isis.IsisInterfaceLevelSettings.builder()
        .setCost(cost)
        .setHelloAuthenticationKey(interfaceLevelSettings.getHelloAuthenticationKey())
        .setHelloAuthenticationType(interfaceLevelSettings.getHelloAuthenticationType())
        .setHelloInterval(interfaceLevelSettings.getHelloInterval())
        .setHoldTime(interfaceLevelSettings.getHoldTime())
        .setMode(
            interfaceSettings.getPassive() || interfaceLevelSettings.getPassive()
                ? IsisInterfaceMode.PASSIVE
                : IsisInterfaceMode.ACTIVE)
        .build();
  }

  private org.batfish.datamodel.isis.IsisLevelSettings toIsisLevelSettings(
      IsisLevelSettings levelSettings) {
    return org.batfish.datamodel.isis.IsisLevelSettings.builder()
        .setWideMetricsOnly(levelSettings.getWideMetricsOnly())
        .build();
  }

  @Nullable
  private OspfProcess createOspfProcess(RoutingInstance routingInstance) {
    if (firstNonNull(routingInstance.getOspfDisable(), Boolean.FALSE)) {
      return null;
    }
    OspfProcess newProc =
        OspfProcess.builder()
            // Use routing instance name since OSPF processes are not named
            .setProcessId(routingInstance.getName())
            .setReferenceBandwidth(routingInstance.getOspfReferenceBandwidth())
            .setAdminCosts(
                org.batfish.datamodel.ospf.OspfProcess.computeDefaultAdminCosts(
                    _c.getConfigurationFormat()))
            .setSummaryAdminCost(
                RoutingProtocol.OSPF_IA.getSummaryAdministrativeCost(_c.getConfigurationFormat()))
            .build();
    String vrfName = routingInstance.getName();
    // export policies
    String ospfExportPolicyName = computeOspfExportPolicyName(vrfName);
    RoutingPolicy ospfExportPolicy = new RoutingPolicy(ospfExportPolicyName, _c);
    applyLocalRoutePolicy(routingInstance, ospfExportPolicy);
    _c.getRoutingPolicies().put(ospfExportPolicyName, ospfExportPolicy);
    newProc.setExportPolicy(ospfExportPolicyName);
    If ospfExportPolicyConditional = new If();
    // TODO: set default metric-type for special cases based on ospf process
    // settings
    ospfExportPolicy.getStatements().add(new SetOspfMetricType(OspfMetricType.E2));
    ospfExportPolicy.getStatements().add(ospfExportPolicyConditional);
    Disjunction matchSomeExportPolicy = new Disjunction();
    ospfExportPolicyConditional.setGuard(matchSomeExportPolicy);
    ospfExportPolicyConditional.getTrueStatements().add(Statements.ExitAccept.toStaticStatement());
    ospfExportPolicyConditional.getFalseStatements().add(Statements.ExitReject.toStaticStatement());
    routingInstance
        .getOspfExportPolicies()
        .forEach(
            exportPolicyName -> {
              PolicyStatement exportPolicy =
                  _masterLogicalSystem.getPolicyStatements().get(exportPolicyName);
              if (exportPolicy != null) {
                setPolicyStatementReferent(exportPolicyName);
                CallExpr callPolicy = new CallExpr(exportPolicyName);
                matchSomeExportPolicy.getDisjuncts().add(callPolicy);
              }
            });
    // areas
    Map<Long, org.batfish.datamodel.ospf.OspfArea.Builder> newAreaBuilders =
        CollectionUtil.toImmutableMap(
            routingInstance.getOspfAreas(),
            Entry::getKey,
            e -> {
              String summaryFilterName =
                  "~OSPF_SUMMARY_FILTER:" + vrfName + ":" + e.getValue().getName() + "~";
              RouteFilterList summaryFilter = new RouteFilterList(summaryFilterName);
              _c.getRouteFilterLists().put(summaryFilterName, summaryFilter);
              return toOspfAreaBuilder(e.getValue(), summaryFilter);
            });
    // place interfaces into areas
    for (Entry<String, Interface> e : routingInstance.getInterfaces().entrySet()) {
      String name = e.getKey();
      Interface iface = e.getValue();
      placeInterfaceIntoArea(newAreaBuilders, name, iface, vrfName);
    }

    // Build areas
    newProc.setAreas(
        newAreaBuilders.entrySet().stream()
            .collect(
                ImmutableSortedMap.toImmutableSortedMap(
                    Comparator.naturalOrder(), Entry::getKey, entry -> entry.getValue().build())));

    // set pointers from interfaces to their parent areas (and process)
    newProc
        .getAreas()
        .values()
        .forEach(
            area ->
                area.getInterfaces()
                    .forEach(
                        ifaceName -> {
                          org.batfish.datamodel.Interface iface =
                              _c.getVrfs().get(vrfName).getInterfaces().get(ifaceName);
                          iface.setOspfArea(area);
                          iface.setOspfProcess(newProc.getProcessId());
                        }));

    newProc.setRouterId(getOspfRouterId(routingInstance));
    return newProc;
  }

  private org.batfish.datamodel.ospf.OspfArea.Builder toOspfAreaBuilder(
      OspfArea area, RouteFilterList summaryFilter) {
    org.batfish.datamodel.ospf.OspfArea.Builder newAreaBuilder =
        org.batfish.datamodel.ospf.OspfArea.builder();
    newAreaBuilder.setNumber(area.getName());
    newAreaBuilder.setNssaSettings(toNssaSettings(area.getNssaSettings()));
    newAreaBuilder.setStubSettings(toStubSettings(area.getStubSettings()));
    newAreaBuilder.setStubType(area.getStubType());
    newAreaBuilder.addSummaries(area.getSummaries());
    newAreaBuilder.setInjectDefaultRoute(area.getInjectDefaultRoute());
    newAreaBuilder.setMetricOfDefaultRoute(area.getMetricOfDefaultRoute());

    // Add summary filters for each area summary
    for (Entry<Prefix, OspfAreaSummary> e2 : area.getSummaries().entrySet()) {
      Prefix prefix = e2.getKey();
      OspfAreaSummary summary = e2.getValue();
      int prefixLength = prefix.getPrefixLength();
      int filterMinPrefixLength =
          summary.getAdvertised()
              ? Math.min(Prefix.MAX_PREFIX_LENGTH, prefixLength + 1)
              : prefixLength;
      summaryFilter.addLine(
          new org.batfish.datamodel.RouteFilterLine(
              LineAction.DENY,
              IpWildcard.create(prefix),
              new SubRange(filterMinPrefixLength, Prefix.MAX_PREFIX_LENGTH)));
    }
    summaryFilter.addLine(
        new org.batfish.datamodel.RouteFilterLine(
            LineAction.PERMIT,
            IpWildcard.create(Prefix.ZERO),
            new SubRange(0, Prefix.MAX_PREFIX_LENGTH)));
    newAreaBuilder.setSummaryFilter(summaryFilter.getName());
    return newAreaBuilder;
  }

  private org.batfish.datamodel.ospf.NssaSettings toNssaSettings(NssaSettings nssaSettings) {
    if (nssaSettings == null) {
      return null;
    }
    return org.batfish.datamodel.ospf.NssaSettings.builder()
        .setDefaultOriginateType(nssaSettings.getDefaultLsaType())
        .setSuppressType3(nssaSettings.getNoSummaries())
        .build();
  }

  private org.batfish.datamodel.ospf.StubSettings toStubSettings(StubSettings stubSettings) {
    if (stubSettings == null) {
      return null;
    }
    return org.batfish.datamodel.ospf.StubSettings.builder()
        .setSuppressType3(stubSettings.getNoSummaries())
        .build();
  }

  public static String computeOspfExportPolicyName(String vrfName) {
    return "~OSPF_EXPORT_POLICY:" + vrfName + "~";
  }

  public Set<StandardCommunity> getAllStandardCommunities() {
    return _allStandardCommunities;
  }

  /**
   * Generate a {@link RoutingPolicy} for use when importing routes from pseudo-protocols (direct,
   * static, aggregate, generated)
   */
  @VisibleForTesting
  static RoutingPolicy generateDefaultPseudoProtocolImportPolicy(@Nonnull Configuration c) {
    return RoutingPolicy.builder()
        .setOwner(c)
        .setName(DEFAULT_PSEUDO_PROTOCOL_IMPORT_POLICY_NAME)
        .setStatements(
            ImmutableList.of(
                new If(
                    new Disjunction(
                        new MatchProtocol(RoutingProtocol.CONNECTED),
                        new MatchProtocol(RoutingProtocol.LOCAL),
                        new MatchProtocol(RoutingProtocol.STATIC),
                        new MatchProtocol(RoutingProtocol.AGGREGATE)),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
                    ImmutableList.of(Statements.ReturnFalse.toStaticStatement()))))
        .build();
  }

  public Map<String, NodeDevice> getNodeDevices() {
    return _nodeDevices;
  }

  private void initDefaultBgpExportPolicy() {
    if (_c.getRoutingPolicies().containsKey(DEFAULT_BGP_EXPORT_POLICY_NAME)) {
      return;
    }
    // set up default export policy (accept bgp routes)
    RoutingPolicy defaultBgpExportPolicy = new RoutingPolicy(DEFAULT_BGP_EXPORT_POLICY_NAME, _c);
    _c.getRoutingPolicies().put(DEFAULT_BGP_EXPORT_POLICY_NAME, defaultBgpExportPolicy);

    If defaultBgpExportPolicyConditional = new If();
    defaultBgpExportPolicy.getStatements().add(defaultBgpExportPolicyConditional);

    // guard
    Disjunction isBgp = new Disjunction();
    isBgp.getDisjuncts().add(new MatchProtocol(RoutingProtocol.BGP));
    isBgp.getDisjuncts().add(new MatchProtocol(RoutingProtocol.IBGP));
    defaultBgpExportPolicyConditional.setGuard(isBgp);

    PsThenAccept.INSTANCE.applyTo(
        defaultBgpExportPolicyConditional.getTrueStatements(), this, _c, _w);
    PsThenReject.INSTANCE.applyTo(
        defaultBgpExportPolicyConditional.getFalseStatements(), this, _c, _w);
  }

  private void initDefaultBgpImportPolicy() {
    if (_c.getRoutingPolicies().containsKey(DEFAULT_BGP_IMPORT_POLICY_NAME)) {
      return;
    }
    // set up default import policy (accept all routes)
    RoutingPolicy defaultBgpImportPolicy = new RoutingPolicy(DEFAULT_BGP_IMPORT_POLICY_NAME, _c);
    _c.getRoutingPolicies().put(DEFAULT_BGP_IMPORT_POLICY_NAME, defaultBgpImportPolicy);
    PsThenAccept.INSTANCE.applyTo(defaultBgpImportPolicy.getStatements(), this, _c, _w);
  }

  /**
   * Initialize default pseudo-protocol import policy (if it does not exist) in the
   * vendor-independent {@link Configuration}.
   *
   * @return the name of the initialized policy
   */
  private void initDefaultPseudoProtocolImportPolicy() {
    if (!_c.getRoutingPolicies().containsKey(DEFAULT_PSEUDO_PROTOCOL_IMPORT_POLICY_NAME)) {
      generateDefaultPseudoProtocolImportPolicy(_c);
    }
  }

  private void initDefaultRejectPolicy() {
    if (!_c.getRoutingPolicies().containsKey(DEFAULT_REJECT_POLICY_NAME)) {
      RoutingPolicy defaultRejectPolicy = new RoutingPolicy(DEFAULT_REJECT_POLICY_NAME, _c);
      _c.getRoutingPolicies().put(DEFAULT_REJECT_POLICY_NAME, defaultRejectPolicy);
      PsThenReject.INSTANCE.applyTo(defaultRejectPolicy.getStatements(), this, _c, _w);
    }
  }

  private void initFirstLoopbackInterface() {
    if (_lo0Initialized) {
      return;
    }
    _lo0Initialized = true;
    _lo0 =
        _masterLogicalSystem
            .getDefaultRoutingInstance()
            .getInterfaces()
            .get(FIRST_LOOPBACK_INTERFACE_NAME);
    Pattern p = Pattern.compile("[A-Za-z0-9][A-Za-z0-9]*:lo[0-9][0-9]*\\.[0-9][0-9]*");
    if (_lo0 == null) {
      for (NodeDevice nd : _nodeDevices.values()) {
        for (Interface iface : nd.getInterfaces().values()) {
          for (Interface unit : iface.getUnits().values()) {
            if (p.matcher(unit.getName()).matches()) {
              _lo0 = unit;
              return;
            }
          }
        }
      }
    } else if (_lo0.getPrimaryAddress() == null) {
      Pattern q = Pattern.compile("lo[0-9][0-9]*\\.[0-9][0-9]*");
      for (Interface iface :
          _masterLogicalSystem.getDefaultRoutingInstance().getInterfaces().values()) {
        for (Interface unit : iface.getUnits().values()) {
          if (q.matcher(unit.getName()).matches()) {
            _lo0 = unit;
            return;
          }
        }
      }
    }
  }

  private void placeInterfaceIntoArea(
      Map<Long, org.batfish.datamodel.ospf.OspfArea.Builder> newAreas,
      String interfaceName,
      Interface iface,
      String vrfName) {
    Vrf vrf = _c.getVrfs().get(vrfName);
    org.batfish.datamodel.Interface newIface = vrf.getInterfaces().get(interfaceName);
    Ip ospfArea = iface.getOspfArea();
    if (ospfArea == null) {
      return;
    }
    if (newIface.getAddress() == null) {
      _w.redFlag(
          String.format(
              "Cannot assign interface %s to area %s because it has no IP address.",
              interfaceName, ospfArea));
      return;
    }
    long ospfAreaLong = ospfArea.asLong();
    org.batfish.datamodel.ospf.OspfArea.Builder newArea = newAreas.get(ospfAreaLong);
    newArea.addInterface(interfaceName);
    newIface.setOspfEnabled(!firstNonNull(iface.getOspfDisable(), Boolean.FALSE));
    newIface.setOspfPassive(iface.getOspfPassive());
    Integer ospfCost = iface.getOspfCost();
    if (ospfCost == null && newIface.isLoopback(ConfigurationFormat.FLAT_JUNIPER)) {
      ospfCost = 0;
    }
    newIface.setOspfCost(ospfCost);
  }

  private void setPolicyStatementReferent(String policyName) {
    PolicyStatement policy = _masterLogicalSystem.getPolicyStatements().get(policyName);
    if (policy == null) {
      return;
    }
    List<PsTerm> terms = new ArrayList<>();
    terms.add(policy.getDefaultTerm());
    terms.addAll(policy.getTerms().values());
    for (PsTerm term : terms) {
      for (PsFromPolicyStatement fromPolicyStatement : term.getFroms().getFromPolicyStatements()) {
        String subPolicyName = fromPolicyStatement.getPolicyStatement();
        setPolicyStatementReferent(subPolicyName);
      }
      for (PsFromPolicyStatementConjunction fromPolicyStatementConjunction :
          term.getFroms().getFromPolicyStatementConjunctions()) {
        for (String subPolicyName : fromPolicyStatementConjunction.getConjuncts()) {
          setPolicyStatementReferent(subPolicyName);
        }
      }
    }
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    _vendor = format;
  }

  private org.batfish.datamodel.GeneratedRoute toGeneratedRoute(GeneratedRoute route) {
    org.batfish.datamodel.GeneratedRoute.Builder newRoute =
        org.batfish.datamodel.GeneratedRoute.builder();

    newRoute.setGenerationPolicy(computeGenerationPolicy(route));
    newRoute.setAdmin(route.getPreference());
    if (route.getAsPath() != null) {
      newRoute.setAsPath(route.getAsPath());
    }
    newRoute.setCommunities(route.getCommunities());
    newRoute.setMetric(route.getMetric());
    newRoute.setNetwork(route.getPrefix());
    if (route.getTag() != null) {
      newRoute.setTag(route.getTag());
    }

    newRoute.setDiscard(firstNonNull(route.getDrop(), Boolean.FALSE));

    return newRoute.build();
  }

  private org.batfish.datamodel.GeneratedRoute toAggregateRoute(AggregateRoute route) {
    org.batfish.datamodel.GeneratedRoute.Builder newRoute =
        org.batfish.datamodel.GeneratedRoute.builder();

    newRoute.setGenerationPolicy(computeGenerationPolicy(route));
    newRoute.setAdmin(route.getPreference());
    if (route.getAsPath() != null) {
      newRoute.setAsPath(route.getAsPath());
    }
    newRoute.setCommunities(route.getCommunities());
    newRoute.setMetric(route.getMetric());
    newRoute.setNetwork(route.getPrefix());
    if (route.getTag() != null) {
      newRoute.setTag(route.getTag());
    }

    // sole semantic difference from generated route: aggregate routes are "reject" by default.
    // Note that this can be overridden to "discard", but we model both as discard in Batfish
    // semantics since the sole difference is whether ICMP unreachables are sent.
    newRoute.setDiscard(true);

    return newRoute.build();
  }

  private @Nullable String computeGenerationPolicy(AbstractAggregateRoute route) {
    // passive means it is installed whether or not there is a more specific route; active means the
    // more specific route must be present, and policy should also be checked if present.
    // https://www.juniper.net/documentation/en_US/junos/topics/reference/configuration-statement/active-edit-routing-options.html

    if (!route.getActive()) {
      return null;
    }
    Prefix prefix = route.getPrefix();
    String generationPolicyName = computeRouteGenerationPolicyName(route);
    RoutingPolicy generationPolicy = new RoutingPolicy(generationPolicyName, _c);
    _c.getRoutingPolicies().put(generationPolicyName, generationPolicy);

    // route filter list to match more specific contributing route
    String rflName = computeContributorRouteFilterListName(prefix);
    MatchPrefixSet isContributingRoute =
        new MatchPrefixSet(DestinationNetwork.instance(), new NamedPrefixSet(rflName));
    RouteFilterList rfList = new RouteFilterList(rflName);
    rfList.addLine(
        new org.batfish.datamodel.RouteFilterLine(
            LineAction.PERMIT,
            prefix,
            new SubRange(prefix.getPrefixLength() + 1, Prefix.MAX_PREFIX_LENGTH)));
    _c.getRouteFilterLists().put(rflName, rfList);

    // contributor check that exits for non-contributing routes
    If contributorCheck = new If();
    contributorCheck.setGuard(isContributingRoute);
    contributorCheck.setFalseStatements(
        ImmutableList.of(Statements.ExitReject.toStaticStatement()));
    generationPolicy.getStatements().add(contributorCheck);

    /*
     *  Evaluate policies in order:
     *  - If a policy accepts, stop evaluation and accept.
     *  - If a policy rejects, stop evaulation and reject.
     *  - If no policy takes an action, take default action.
     *  -- Initially, default action is accept.
     *  -- Policy can change default action and fall through.
     */
    generationPolicy.getStatements().add(Statements.SetDefaultActionAccept.toStaticStatement());
    if (!route.getPolicies().isEmpty()) {
      route
          .getPolicies()
          .forEach(
              policyName -> {
                PolicyStatement policy = _masterLogicalSystem.getPolicyStatements().get(policyName);
                boolean defined = policy != null;
                if (defined) {
                  setPolicyStatementReferent(policyName);
                  generationPolicy.getStatements().add(new CallStatement(policyName));
                } else {
                  generationPolicy
                      .getStatements()
                      .add(new Comment(String.format("Undefined reference to: %s", policyName)));
                }
              });
    }
    return generationPolicyName;
  }

  public static String computeContributorRouteFilterListName(Prefix prefix) {
    return String.format("~CONTRIBUTOR_TO_%s~", prefix);
  }

  private static String computeRouteGenerationPolicyName(AbstractAggregateRoute route) {
    Prefix prefix = route.getPrefix();
    return route instanceof AggregateRoute
        ? computeAggregatedRouteGenerationPolicyName(prefix)
        : computeGeneratedRouteGenerationPolicyName(prefix);
  }

  public static String computeAggregatedRouteGenerationPolicyName(Prefix prefix) {
    return String.format("~AGGREGATE_ROUTE_POLICY:%s~", prefix);
  }

  public static String computeGeneratedRouteGenerationPolicyName(Prefix prefix) {
    return String.format("~GENERATED_ROUTE_POLICY:%s~", prefix);
  }

  private org.batfish.datamodel.GeneratedRoute ospfSummaryToAggregateRoute(
      Prefix prefix, OspfAreaSummary summary) {
    int prefixLength = prefix.getPrefixLength();
    String policyNameSuffix = prefix.toString().replace('/', '_').replace('.', '_');
    String policyName = "~SUMMARY" + policyNameSuffix + "~";
    RoutingPolicy routingPolicy = new RoutingPolicy(policyName, _c);
    If routingPolicyConditional = new If();
    routingPolicy.getStatements().add(routingPolicyConditional);
    routingPolicyConditional.getTrueStatements().add(Statements.ExitAccept.toStaticStatement());
    routingPolicyConditional.getFalseStatements().add(Statements.ExitReject.toStaticStatement());
    String rflName = "~SUMMARY" + policyNameSuffix + "_RF~";
    MatchPrefixSet isContributingRoute =
        new MatchPrefixSet(DestinationNetwork.instance(), new NamedPrefixSet(rflName));
    routingPolicyConditional.setGuard(isContributingRoute);
    RouteFilterList rfList = new RouteFilterList(rflName);
    rfList.addLine(
        new org.batfish.datamodel.RouteFilterLine(
            LineAction.PERMIT, prefix, new SubRange(prefixLength + 1, Prefix.MAX_PREFIX_LENGTH)));
    org.batfish.datamodel.GeneratedRoute.Builder newRoute =
        org.batfish.datamodel.GeneratedRoute.builder();
    newRoute.setNetwork(prefix);
    newRoute.setAdmin(
        RoutingProtocol.OSPF_IA.getDefaultAdministrativeCost(ConfigurationFormat.JUNIPER));
    if (summary.getMetric() != null) {
      newRoute.setMetric(summary.getMetric());
    }
    newRoute.setDiscard(true);
    newRoute.setGenerationPolicy(policyName);
    _c.getRoutingPolicies().put(policyName, routingPolicy);
    _c.getRouteFilterLists().put(rflName, rfList);
    return newRoute.build();
  }

  private org.batfish.datamodel.CommunityList toCommunityList(CommunityList cl) {
    String name = cl.getName();
    List<org.batfish.datamodel.CommunityListLine> newLines = new ArrayList<>();
    for (CommunityListLine line : cl.getLines()) {
      String regex = line.getText();
      String javaRegex = communityRegexToJavaRegex(regex);
      org.batfish.datamodel.CommunityListLine newLine =
          new org.batfish.datamodel.CommunityListLine(
              LineAction.PERMIT, new RegexCommunitySet(javaRegex));
      newLines.add(newLine);
    }
    org.batfish.datamodel.CommunityList newCl =
        new org.batfish.datamodel.CommunityList(name, newLines, cl.getInvertMatch());
    return newCl;
  }

  /**
   * Converts {@link IkePolicy} to {@link IkePhase1Policy} and puts the used pre-shared key as a
   * {@link IkePhase1Key} in the passed-in {@code ikePhase1Keys}
   */
  private static IkePhase1Policy toIkePhase1Policy(
      IkePolicy ikePolicy, ImmutableSortedMap.Builder<String, IkePhase1Key> ikePhase1Keys) {
    String name = ikePolicy.getName();
    IkePhase1Policy ikePhase1Policy = new IkePhase1Policy(name);

    // pre-shared-key
    IkePhase1Key ikePhase1Key = new IkePhase1Key();
    ikePhase1Key.setKeyType(IkeKeyType.PRE_SHARED_KEY);
    ikePhase1Key.setKeyHash(ikePolicy.getPreSharedKeyHash());

    ikePhase1Keys.put(String.format("~IKE_PHASE1_KEY_%s~", ikePolicy.getName()), ikePhase1Key);

    ikePhase1Policy.setIkePhase1Key(ikePhase1Key);
    ImmutableList.Builder<String> ikePhase1ProposalBuilder = ImmutableList.builder();
    // ike proposals
    ikePolicy.getProposals().forEach(ikePhase1ProposalBuilder::add);
    ikePhase1Policy.setIkePhase1Proposals(ikePhase1ProposalBuilder.build());

    return ikePhase1Policy;
  }

  private IkePhase1Proposal toIkePhase1Proposal(IkeProposal ikeProposal) {
    IkePhase1Proposal ikePhase1Proposal = new IkePhase1Proposal(ikeProposal.getName());
    ikePhase1Proposal.setDiffieHellmanGroup(ikeProposal.getDiffieHellmanGroup());
    ikePhase1Proposal.setAuthenticationMethod(ikeProposal.getAuthenticationMethod());
    ikePhase1Proposal.setEncryptionAlgorithm(ikeProposal.getEncryptionAlgorithm());
    ikePhase1Proposal.setLifetimeSeconds(ikeProposal.getLifetimeSeconds());
    ikePhase1Proposal.setHashingAlgorithm(ikeProposal.getAuthenticationAlgorithm());
    return ikePhase1Proposal;
  }

  /**
   * Convert a non-unit interface to the VI {@link org.batfish.datamodel.Interface}.
   *
   * <p>Note that bulk of the configuration is stored at the logical interface level, see {@link
   * #toInterface(Interface)} for those conversions. Here we convert aggregation and bandwidth
   * settings; track VRF membership.
   */
  private org.batfish.datamodel.Interface toInterfaceNonUnit(Interface iface) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface = new org.batfish.datamodel.Interface(name, _c);
    newIface.setDeclaredNames(ImmutableSortedSet.of(name));
    newIface.setDescription(iface.getDescription());

    // 802.3ad link aggregation
    if (iface.get8023adInterface() != null) {
      newIface.setChannelGroup(iface.get8023adInterface());
    }
    // Redundant ethernet
    if (iface.getRedundantParentInterface() != null) {
      newIface.setChannelGroup(iface.getRedundantParentInterface());
    }

    newIface.setActive(iface.getActive());
    newIface.setBandwidth(iface.getBandwidth());
    if (iface.getMtu() != null) {
      newIface.setMtu(iface.getMtu());
    }
    newIface.setNativeVlan(iface.getNativeVlan());
    newIface.setVrf(_c.getVrfs().get(iface.getRoutingInstance()));
    return newIface;
  }

  private org.batfish.datamodel.Interface toInterface(Interface iface) {
    String name = iface.getName();
    org.batfish.datamodel.Interface newIface = new org.batfish.datamodel.Interface(name, _c);
    newIface.setDeclaredNames(ImmutableSortedSet.of(name));
    newIface.setDescription(iface.getDescription());
    Integer mtu = iface.getMtu();
    if (mtu != null) {
      newIface.setMtu(mtu);
    }
    newIface.setVrrpGroups(iface.getVrrpGroups());
    newIface.setVrf(_c.getVrfs().get(iface.getRoutingInstance()));
    newIface.setAdditionalArpIps(
        AclIpSpace.union(
            iface.getAdditionalArpIps().stream().map(Ip::toIpSpace).collect(Collectors.toList())));
    Zone zone = _masterLogicalSystem.getInterfaceZones().get(iface.getName());
    if (zone != null) {
      // filter for interface in zone
      FirewallFilter zoneInboundInterfaceFilter =
          zone.getInboundInterfaceFilters().get(iface.getName());
      if (zoneInboundInterfaceFilter != null) {
        String zoneInboundInterfaceFilterName = zoneInboundInterfaceFilter.getName();
        IpAccessList zoneInboundInterfaceFilterList =
            _c.getIpAccessLists().get(zoneInboundInterfaceFilterName);
        newIface.setInboundFilter(zoneInboundInterfaceFilterList);
      } else {
        // filter for zone
        FirewallFilter zoneInboundFilter = zone.getInboundFilter();
        String zoneInboundFilterName = zoneInboundFilter.getName();
        IpAccessList zoneInboundFilterList = _c.getIpAccessLists().get(zoneInboundFilterName);
        newIface.setInboundFilter(zoneInboundFilterList);
      }

      // create session info
      newIface.setFirewallSessionInterfaceInfo(
          new FirewallSessionInterfaceInfo(
              zone.getInterfaces(), iface.getIncomingFilter(), iface.getOutgoingFilter()));
    }

    String incomingFilterName = iface.getIncomingFilter();
    if (incomingFilterName != null) {
      IpAccessList inAcl = _c.getIpAccessLists().get(incomingFilterName);
      if (inAcl != null) {
        FirewallFilter inFilter = _masterLogicalSystem.getFirewallFilters().get(incomingFilterName);
        if (inFilter.isUsedForFBF()) {
          PacketPolicy routingPolicy = _c.getPacketPolicies().get(incomingFilterName);
          if (routingPolicy != null) {
            newIface.setRoutingPolicy(incomingFilterName);
          } else {
            newIface.setRoutingPolicy(null);
            _w.redFlag(
                String.format(
                    "Interface %s: cannot resolve applied filter %s, defaulting to no filter",
                    name, incomingFilterName));
          }
        }
      }
    }
    IpAccessList composedInAcl = buildIncomingFilter(iface);
    newIface.setIncomingFilter(composedInAcl);

    newIface.setIncomingTransformation(buildIncomingTransformation(iface));

    // Assume the config will need security policies only if it has zones
    IpAccessList securityPolicyAcl;
    if (!_masterLogicalSystem.getZones().isEmpty()) {
      String securityPolicyAclName = ACL_NAME_SECURITY_POLICY + iface.getName();
      securityPolicyAcl = buildSecurityPolicyAcl(securityPolicyAclName, zone);
      if (securityPolicyAcl != null) {
        _c.getIpAccessLists().put(securityPolicyAclName, securityPolicyAcl);
        newIface.setPreTransformationOutgoingFilter(securityPolicyAcl);
      }
    }

    // Set outgoing filter
    String outAclName = iface.getOutgoingFilter();
    IpAccessList outAcl = null;
    if (outAclName != null) {
      outAcl = _c.getIpAccessLists().get(outAclName);
    }
    newIface.setOutgoingFilter(outAcl);

    // Prefix primaryPrefix = iface.getPrimaryAddress();
    // Set<Prefix> allPrefixes = iface.getAllAddresses();
    // if (primaryPrefix != null) {
    // newIface.setAddress(primaryPrefix);
    // }
    // else {
    // if (!allPrefixes.isEmpty()) {
    // Prefix firstOfAllPrefixes = allPrefixes.toArray(new Prefix[] {})[0];
    // newIface.setAddress(firstOfAllPrefixes);
    // }
    // }
    // newIface.getAllAddresses().addAll(allPrefixes);

    if (iface.getPrimaryAddress() != null) {
      newIface.setAddress(iface.getPrimaryAddress());
    }
    newIface.setAllAddresses(iface.getAllAddresses());
    newIface.setActive(iface.getActive());
    if (iface.getSwitchportMode() == SwitchportMode.ACCESS && iface.getAccessVlan() != null) {
      Vlan vlan = _masterLogicalSystem.getNamedVlans().get(iface.getAccessVlan());
      if (vlan != null) {
        newIface.setAccessVlan(vlan.getVlanId());
      }
    }
    IntegerSpace.Builder vlanIdsBuilder = IntegerSpace.builder();
    Stream.concat(
            iface.getAllowedVlanNames().stream()
                .map(_masterLogicalSystem.getNamedVlans()::get)
                .filter(Objects::nonNull) // named vlan must exist
                .map(Vlan::getVlanId)
                .filter(Objects::nonNull) // named vlan must have assigned numeric id
                .map(SubRange::new),
            iface.getAllowedVlans().stream())
        .forEach(vlanIdsBuilder::including);
    newIface.setAllowedVlans(vlanIdsBuilder.build());

    if (iface.getSwitchportMode() == SwitchportMode.TRUNK) {
      newIface.setNativeVlan(firstNonNull(iface.getNativeVlan(), 1));
    }

    newIface.setSwitchportMode(iface.getSwitchportMode());
    SwitchportEncapsulationType swe = iface.getSwitchportTrunkEncapsulation();
    if (swe == null) {
      swe = SwitchportEncapsulationType.DOT1Q;
    }
    newIface.setSwitchportTrunkEncapsulation(swe);
    newIface.setBandwidth(iface.getBandwidth());
    // treat all non-broadcast interfaces as point to point
    newIface.setOspfPointToPoint(iface.getOspfInterfaceType() != OspfInterfaceType.BROADCAST);

    return newIface;
  }

  @Nullable
  Transformation buildOutgoingTransformation(
      Interface iface,
      Nat nat,
      List<NatRuleSet> orderedRuleSetList,
      Map<NatPacketLocation, AclLineMatchExpr> matchFromLocationExprs,
      @Nullable Transformation orElse) {
    if (orderedRuleSetList == null) {
      return orElse;
    }

    String name = iface.getName();
    String zone =
        Optional.ofNullable(_masterLogicalSystem.getInterfaceZones().get(name))
            .map(Zone::getName)
            .orElse(null);
    String routingInstance = iface.getRoutingInstance();

    List<NatRuleSet> ruleSets =
        orderedRuleSetList.stream()
            .filter(
                ruleSet -> {
                  NatPacketLocation toLocation = ruleSet.getToLocation();
                  return name.equals(toLocation.getInterface())
                      || (zone != null && zone.equals(toLocation.getZone()))
                      || (routingInstance.equals(toLocation.getRoutingInstance()));
                })
            .collect(Collectors.toList());

    if (ruleSets.isEmpty()) {
      return orElse;
    }

    if (iface.getPrimaryAddress() == null) {
      _w.redFlag(
          "Cannot build incoming transformation without an interface IP. Interface name = " + name);
      return orElse;
    }
    Ip interfaceIp = iface.getPrimaryAddress().getIp();

    Transformation transformation = orElse;
    for (NatRuleSet ruleSet : Lists.reverse(ruleSets)) {
      transformation =
          ruleSet
              .toOutgoingTransformation(
                  nat,
                  _masterLogicalSystem
                      .getAddressBooks()
                      .get(LogicalSystem.GLOBAL_ADDRESS_BOOK_NAME)
                      .getEntries(),
                  interfaceIp,
                  matchFromLocationExprs,
                  null,
                  transformation,
                  _w)
              .orElse(transformation);
    }
    return transformation;
  }

  @VisibleForTesting
  Map<NatPacketLocation, AclLineMatchExpr> fromNatPacketLocationMatchExprs() {
    ImmutableMap.Builder<NatPacketLocation, AclLineMatchExpr> builder = ImmutableMap.builder();
    _masterLogicalSystem
        .getInterfaces()
        .values()
        .forEach(
            iface ->
                iface
                    .getUnits()
                    .keySet()
                    .forEach(
                        ifaceUnit ->
                            builder.put(
                                interfaceLocation(ifaceUnit), matchSrcInterface(ifaceUnit))));
    _masterLogicalSystem
        .getZones()
        .values()
        .forEach(
            zone ->
                builder.put(
                    zoneLocation(zone.getName()), new MatchSrcInterface(zone.getInterfaces())));
    _masterLogicalSystem
        .getRoutingInstances()
        .values()
        .forEach(
            routingInstance ->
                builder.put(
                    routingInstanceLocation(routingInstance.getName()),
                    matchSrcInterface(
                        routingInstance.getInterfaces().values().stream()
                            .map(Interface::getName)
                            .toArray(String[]::new))));
    return builder.build();
  }

  @Nullable
  @VisibleForTesting
  static IpAccessList buildScreen(@Nullable Screen screen, String aclName) {
    if (screen == null || screen.getAction() == ScreenAction.ALARM_WITHOUT_DROP) {
      return null;
    }

    List<AclLineMatchExpr> matches =
        screen.getScreenOptions().stream()
            .map(ScreenOption::getAclLineMatchExpr)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (matches.isEmpty()) {
      return null;
    }

    return IpAccessList.builder()
        .setName(aclName)
        .setLines(
            ImmutableList.of(
                IpAccessListLine.rejecting(new OrMatchExpr(matches)), IpAccessListLine.ACCEPT_ALL))
        .build();
  }

  @Nullable
  @VisibleForTesting
  IpAccessList buildScreensPerZone(@Nonnull Zone zone, String aclName) {
    List<AclLineMatchExpr> matches =
        zone.getScreens().stream()
            .map(
                screenName -> {
                  Screen screen = _masterLogicalSystem.getScreens().get(screenName);
                  String screenAclName = ACL_NAME_SCREEN + screenName;
                  IpAccessList screenAcl =
                      _c.getIpAccessLists()
                          .computeIfAbsent(screenAclName, x -> buildScreen(screen, screenAclName));
                  return screenAcl != null ? new PermittedByAcl(screenAcl.getName(), false) : null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return matches.isEmpty()
        ? null
        : IpAccessList.builder()
            .setName(aclName)
            .setLines(ImmutableList.of(IpAccessListLine.accepting(new AndMatchExpr(matches))))
            .build();
  }

  @Nullable
  @VisibleForTesting
  IpAccessList buildScreensPerInterface(Interface iface) {
    Zone zone = _masterLogicalSystem.getInterfaceZones().get(iface.getName());
    if (zone == null) {
      return null;
    }

    // build a acl for each zone
    String zoneAclName = ACL_NAME_SCREEN_ZONE + zone.getName();
    IpAccessList zoneAcl =
        _c.getIpAccessLists()
            .computeIfAbsent(zoneAclName, x -> buildScreensPerZone(zone, zoneAclName));

    return zoneAcl == null
        ? null
        : IpAccessList.builder()
            .setName(ACL_NAME_SCREEN_INTERFACE + iface.getName())
            .setLines(ImmutableList.of(IpAccessListLine.accepting(new PermittedByAcl(zoneAclName))))
            .build();
  }

  @Nullable
  IpAccessList buildIncomingFilter(Interface iface) {
    String screenAclName = ACL_NAME_SCREEN_INTERFACE + iface.getName();
    IpAccessList screenAcl =
        _c.getIpAccessLists().computeIfAbsent(screenAclName, x -> buildScreensPerInterface(iface));
    // merge screen options to incoming filter
    // but keep both originial filters in the config, so we can run search filter queris on them
    String inAclName = iface.getIncomingFilter();
    IpAccessList inAcl = inAclName != null ? _c.getIpAccessLists().get(inAclName) : null;

    Set<AclLineMatchExpr> aclConjunctList;
    if (screenAcl == null) {
      return inAcl;
    } else if (inAcl == null) {
      aclConjunctList = ImmutableSet.of(new PermittedByAcl(screenAcl.getName(), false));
    } else {
      aclConjunctList =
          ImmutableSet.of(
              new PermittedByAcl(screenAcl.getName(), false), new PermittedByAcl(inAclName, false));
    }

    String combinedAclName = ACL_NAME_COMBINED_INCOMING + iface.getName();
    IpAccessList combinedAcl =
        IpAccessList.builder()
            .setName(combinedAclName)
            .setLines(
                ImmutableList.of(IpAccessListLine.accepting(new AndMatchExpr(aclConjunctList))))
            .build();

    _c.getIpAccessLists().put(combinedAclName, combinedAcl);
    return combinedAcl;
  }

  @Nullable
  private Transformation buildIncomingTransformation(
      Nat nat, Interface iface, Transformation orElse) {
    if (nat == null) {
      return orElse;
    }

    String ifaceName = iface.getName();
    String zone =
        Optional.ofNullable(_masterLogicalSystem.getInterfaceZones().get(ifaceName))
            .map(Zone::getName)
            .orElse(null);
    String routingInstance = iface.getRoutingInstance();

    /*
     * Precedence of rule set is by fromLocation: interface > zone > routing instance
     */
    NatRuleSet ifaceLocationRuleSet = null;
    NatRuleSet zoneLocationRuleSet = null;
    NatRuleSet routingInstanceRuleSet = null;
    for (Entry<String, NatRuleSet> entry : nat.getRuleSets().entrySet()) {
      NatRuleSet ruleSet = entry.getValue();
      NatPacketLocation fromLocation = ruleSet.getFromLocation();
      if (ifaceName.equals(fromLocation.getInterface())) {
        ifaceLocationRuleSet = ruleSet;
      } else if (zone != null && zone.equals(fromLocation.getZone())) {
        zoneLocationRuleSet = ruleSet;
      } else if (routingInstance.equals(fromLocation.getRoutingInstance())) {
        routingInstanceRuleSet = ruleSet;
      }
    }

    Transformation transformation = orElse;
    List<NatRuleSet> ruleSets =
        Stream.of(routingInstanceRuleSet, zoneLocationRuleSet, ifaceLocationRuleSet)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (ruleSets.isEmpty()) {
      return transformation;
    }

    if (iface.getPrimaryAddress() == null) {
      _w.redFlag(
          "Cannot build incoming transformation without an interface IP. Interface name = "
              + iface.getName());
      return null;
    }
    Ip interfaceIp = iface.getPrimaryAddress().getIp();

    for (NatRuleSet ruleSet : ruleSets) {
      transformation =
          ruleSet
              .toIncomingTransformation(
                  nat,
                  _masterLogicalSystem
                      .getAddressBooks()
                      .get(LogicalSystem.GLOBAL_ADDRESS_BOOK_NAME)
                      .getEntries(),
                  interfaceIp,
                  null,
                  transformation,
                  _w)
              .orElse(transformation);
    }
    return transformation;
  }

  @Nullable
  private Transformation buildIncomingTransformation(Interface iface) {
    Nat dnat = _masterLogicalSystem.getNatDestination();
    Transformation dstTransformation = buildIncomingTransformation(dnat, iface, null);
    Nat staticNat = _masterLogicalSystem.getNatStatic();
    return buildIncomingTransformation(staticNat, iface, dstTransformation);
  }

  /** Generate IpAccessList from the specified to-zone's security policies. */
  IpAccessList buildSecurityPolicyAcl(String name, Zone zone) {
    List<IpAccessListLine> zoneAclLines = new TreeList<>();

    /* Default ACL that allows existing connections should be added to all security policies */
    zoneAclLines.add(
        new IpAccessListLine(
            LineAction.PERMIT,
            new PermittedByAcl(ACL_NAME_EXISTING_CONNECTION, false),
            "EXISTING_CONNECTION"));

    /* Default policy allows traffic originating from the device to be accepted */
    zoneAclLines.add(
        new IpAccessListLine(LineAction.PERMIT, OriginatingFromDevice.INSTANCE, "HOST_OUTBOUND"));

    /* Zone specific policies */
    if (zone != null && !zone.getFromZonePolicies().isEmpty()) {
      for (Entry<String, FirewallFilter> e : zone.getFromZonePolicies().entrySet()) {
        /* Handle explicit accept lines from this policy */
        zoneAclLines.add(
            new IpAccessListLine(
                LineAction.PERMIT, new PermittedByAcl(e.getKey(), false), e.getKey() + "PERMIT"));
        /* Handle explicit deny lines from this policy, this is needed so only unmatched lines fall-through to the next lines */
        zoneAclLines.add(
            new IpAccessListLine(
                LineAction.DENY,
                new NotMatchExpr(new PermittedByAcl(e.getKey(), true)),
                e.getKey() + "DENY"));
      }
    }

    /* Global policy if applicable */
    if (_masterLogicalSystem.getFirewallFilters().get(ACL_NAME_GLOBAL_POLICY) != null) {
      /* Handle explicit accept lines for global policy */
      zoneAclLines.add(
          new IpAccessListLine(
              LineAction.PERMIT,
              new PermittedByAcl(ACL_NAME_GLOBAL_POLICY, false),
              "GLOBAL_POLICY_ACCEPT"));
      /* Handle explicit deny lines for global policy, this is needed so only unmatched lines fall-through to the next lines */
      zoneAclLines.add(
          new IpAccessListLine(
              LineAction.DENY,
              new NotMatchExpr(new PermittedByAcl(ACL_NAME_GLOBAL_POLICY, true)),
              "GLOBAL_POLICY_REJECT"));
    }

    /* Add catch-all line with default action */
    zoneAclLines.add(
        new IpAccessListLine(
            _masterLogicalSystem.getDefaultCrossZoneAction(), TrueExpr.INSTANCE, "DEFAULT_POLICY"));

    IpAccessList zoneAcl = IpAccessList.builder().setName(name).setLines(zoneAclLines).build();
    _c.getIpAccessLists().put(name, zoneAcl);
    return zoneAcl;
  }

  /**
   * Convert firewallFilter terms (headerSpace matching) and optional conjunctMatchExpr into a
   * single ACL.
   */
  private IpAccessList fwTermsToIpAccessList(
      String aclName, Collection<FwTerm> terms, @Nullable AclLineMatchExpr conjunctMatchExpr)
      throws VendorConversionException {
    List<IpAccessListLine> lines = new ArrayList<>();
    for (FwTerm term : terms) {
      // action
      LineAction action;
      if (term.getThens().contains(FwThenAccept.INSTANCE)) {
        action = LineAction.PERMIT;
      } else if (term.getThens().contains(FwThenDiscard.INSTANCE)) {
        action = LineAction.DENY;
      } else if (term.getThens().contains(FwThenNextTerm.INSTANCE)) {
        // TODO: throw error if any transformation is being done
        continue;
      } else if (term.getThens().contains(FwThenNop.INSTANCE)) {
        // we assume for now that any 'nop' operations imply acceptance
        action = LineAction.PERMIT;
      } else if (term.getThens().stream()
          .map(Object::getClass)
          .anyMatch(Predicate.isEqual(FwThenRoutingInstance.class))) {
        // Should be handled by packet policy, not applicable to ACLs
        continue;
      } else {
        _w.redFlag(
            "missing action in firewall filter: '" + aclName + "', term: '" + term.getName() + "'");
        action = LineAction.DENY;
      }
      HeaderSpace.Builder matchCondition = HeaderSpace.builder();
      for (FwFrom from : term.getFroms()) {
        from.applyTo(matchCondition, this, _w, _c);
      }
      boolean addLine =
          term.getFromApplicationSetMembers().isEmpty()
              && term.getFromHostProtocols().isEmpty()
              && term.getFromHostServices().isEmpty();
      for (FwFromHostProtocol from : term.getFromHostProtocols()) {
        from.applyTo(lines, _w);
      }
      for (FwFromHostService from : term.getFromHostServices()) {
        from.applyTo(lines, _w);
      }
      for (FwFromApplicationSetMember fromApplicationSetMember :
          term.getFromApplicationSetMembers()) {
        fromApplicationSetMember.applyTo(this, matchCondition, action, lines, _w);
      }
      if (addLine) {
        IpAccessListLine line =
            IpAccessListLine.builder()
                .setAction(action)
                .setMatchCondition(new MatchHeaderSpace(matchCondition.build()))
                .setName(term.getName())
                .build();
        lines.add(line);
      }
    }
    return IpAccessList.builder()
        .setName(aclName)
        .setLines(mergeIpAccessListLines(lines, conjunctMatchExpr))
        .setSourceName(aclName)
        .setSourceType(JuniperStructureType.FIREWALL_FILTER.getDescription())
        .build();
  }

  /** Merge the list of lines with the specified conjunct match expression. */
  private static List<IpAccessListLine> mergeIpAccessListLines(
      List<IpAccessListLine> lines, @Nullable AclLineMatchExpr conjunctMatchExpr) {
    if (conjunctMatchExpr == null) {
      return lines;
    } else {
      return lines.stream()
          .map(
              l ->
                  new IpAccessListLine(
                      l.getAction(),
                      new AndMatchExpr(ImmutableList.of(l.getMatchCondition(), conjunctMatchExpr)),
                      l.getName()))
          .collect(ImmutableList.toImmutableList());
    }
  }

  /** Convert a firewallFilter into an equivalent ACL. */
  IpAccessList toIpAccessList(FirewallFilter filter) throws VendorConversionException {
    String name = filter.getName();
    AclLineMatchExpr matchSrcInterface = null;

    /*
     * If srcInterfaces (from-zone) are filtered (this is the case for security policies), then
     * need to make a match condition for that
     */
    String zoneName = filter.getFromZone();
    if (zoneName != null) {
      matchSrcInterface =
          new MatchSrcInterface(_masterLogicalSystem.getZones().get(zoneName).getInterfaces());
    }

    /* Return an ACL that is the logical AND of srcInterface filter and headerSpace filter */
    return fwTermsToIpAccessList(name, filter.getTerms().values(), matchSrcInterface);
  }

  @Nullable
  private IpsecPeerConfig toIpsecPeerConfig(IpsecVpn ipsecVpn) {
    IpsecStaticPeerConfig.Builder ipsecStaticConfigBuilder = IpsecStaticPeerConfig.builder();
    ipsecStaticConfigBuilder.setTunnelInterface(ipsecVpn.getBindInterface());
    IkeGateway ikeGateway = _masterLogicalSystem.getIkeGateways().get(ipsecVpn.getGateway());

    if (ikeGateway == null) {
      _w.redFlag(
          String.format(
              "Cannot find the IKE gateway %s for ipsec vpn %s",
              ipsecVpn.getGateway(), ipsecVpn.getName()));
      return null;
    }
    ipsecStaticConfigBuilder.setDestinationAddress(ikeGateway.getAddress());

    String externalIfaceName = ikeGateway.getExternalInterface();
    String masterIfaceName = interfaceUnitMasterName(externalIfaceName);

    Interface externalIface =
        _masterLogicalSystem.getInterfaces().get(masterIfaceName).getUnits().get(externalIfaceName);

    ipsecStaticConfigBuilder.setSourceInterface(externalIfaceName);

    if (ikeGateway.getLocalAddress() != null) {
      ipsecStaticConfigBuilder.setLocalAddress(ikeGateway.getLocalAddress());
    } else if (externalIface != null && externalIface.getPrimaryAddress() != null) {
      ipsecStaticConfigBuilder.setLocalAddress(externalIface.getPrimaryAddress().getIp());
    } else {
      _w.redFlag(
          String.format(
              "External interface %s configured on IKE Gateway %s does not have any IP",
              externalIfaceName, ikeGateway.getName()));
      return null;
    }

    ipsecStaticConfigBuilder.setIpsecPolicy(ipsecVpn.getIpsecPolicy());
    ipsecStaticConfigBuilder.setIkePhase1Policy(ikeGateway.getIkePolicy());
    return ipsecStaticConfigBuilder.build();
  }

  private static IpsecPhase2Policy toIpsecPhase2Policy(IpsecPolicy ipsecPolicy) {
    IpsecPhase2Policy ipsecPhase2Policy = new IpsecPhase2Policy();
    ipsecPhase2Policy.setPfsKeyGroup(ipsecPolicy.getPfsKeyGroup());
    ipsecPhase2Policy.setProposals(ImmutableList.copyOf(ipsecPolicy.getProposals()));

    return ipsecPhase2Policy;
  }

  private static IpsecPhase2Proposal toIpsecPhase2Proposal(IpsecProposal oldIpsecProposal) {
    IpsecPhase2Proposal ipsecPhase2Proposal = new IpsecPhase2Proposal();
    ipsecPhase2Proposal.setAuthenticationAlgorithm(oldIpsecProposal.getAuthenticationAlgorithm());
    ipsecPhase2Proposal.setEncryptionAlgorithm(oldIpsecProposal.getEncryptionAlgorithm());
    ipsecPhase2Proposal.setProtocols(oldIpsecProposal.getProtocols());
    ipsecPhase2Proposal.setIpsecEncapsulationMode(oldIpsecProposal.getIpsecEncapsulationMode());

    return ipsecPhase2Proposal;
  }

  /** Convert address book into corresponding IpSpaces */
  private Map<String, IpSpace> toIpSpaces(String bookName, AddressBook book) {
    Map<String, IpSpace> ipSpaces = new TreeMap<>();
    book.getEntries()
        .forEach(
            (n, entry) -> {
              String entryName = bookName + "~" + n;

              // If this address book references other entries, add them to an AclIpSpace
              if (!entry.getEntries().isEmpty()) {
                AclIpSpace.Builder aclIpSpaceBuilder = AclIpSpace.builder();
                entry
                    .getEntries()
                    .keySet()
                    .forEach(
                        name -> {
                          String subEntryName = bookName + "~" + name;
                          aclIpSpaceBuilder.thenPermitting(new IpSpaceReference(subEntryName));
                        });
                ipSpaces.put(entryName, aclIpSpaceBuilder.build());
              } else {
                ipSpaces.put(
                    entryName,
                    IpWildcardSetIpSpace.builder().including(entry.getIpWildcards(_w)).build());
              }
            });
    return ipSpaces;
  }

  /**
   * Given a list of policy names, returns a list of the VRFs referenced in those policies, in the
   * order in which they appear (within each policy and within the list of policies). The list will
   * not have duplicates if a VRF is referenced multiple times, nor will it include undefined VRFs.
   *
   * <p>Used for generating the list of VRFs to be imported for instance-import policies.
   */
  @Nonnull
  private List<String> getVrfsReferencedByPolicies(List<String> instanceImportPolicies) {
    return instanceImportPolicies.stream()
        .filter(pName -> _c.getRoutingPolicies().containsKey(pName))
        .flatMap(pName -> _vrfReferencesInPolicies.getOrDefault(pName, ImmutableList.of()).stream())
        .distinct()
        .filter(vrfName -> _c.getVrfs().containsKey(vrfName))
        .collect(ImmutableList.toImmutableList());
  }

  @Nonnull
  private static RoutingPolicy buildInstanceImportRoutingPolicy(
      RoutingInstance ri, Configuration c, String vrfName) {
    String policyName = generateInstanceImportPolicyName(vrfName);
    List<BooleanExpr> policyCalls =
        ri.getInstanceImports().stream()
            .filter(c.getRoutingPolicies()::containsKey)
            .map(CallExpr::new)
            .collect(Collectors.toList());
    return RoutingPolicy.builder()
        .setOwner(c)
        .setName(policyName)
        .setStatements(
            ImmutableList.of(
                // TODO implement default policy for instance-import. For now default reject.
                // Once this is fixed, can throw away default reject policy infrastructure.
                new SetDefaultPolicy(DEFAULT_REJECT_POLICY_NAME),
                // Construct a policy chain based on defined instance-import policies
                new If(
                    new FirstMatchChain(policyCalls),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
                    ImmutableList.of(Statements.ReturnFalse.toStaticStatement()))))
        .build();
  }

  private static String generateInstanceImportPolicyName(String vrfName) {
    return String.format("~INSTANCE_IMPORT_POLICY_%s~", vrfName);
  }

  @Nonnull
  private static org.batfish.datamodel.dataplane.rib.RibGroup toRibGroup(
      RibGroup rg, RoutingProtocol protocol, Configuration c, String vrfName, Warnings w) {
    ImmutableList<RibId> importRibs =
        rg.getImportRibs().stream()
            .map(rib -> toRibId(c.getHostname(), rib, w))
            .filter(Objects::nonNull)
            // Filter out the primary rib for this rib group, since it's special and bypasses the
            // policy
            .filter(
                rib ->
                    !(rib.getRibName().equals(RibId.DEFAULT_RIB_NAME)
                        && rib.getVrfName().equals(vrfName)))
            .collect(ImmutableList.toImmutableList());

    RibId exportRib =
        rg.getExportRib() != null ? toRibId(c.getHostname(), rg.getExportRib(), w) : null;
    List<BooleanExpr> policyCalls =
        rg.getImportPolicies().stream().map(CallExpr::new).collect(ImmutableList.toImmutableList());

    String policyName = generateRibGroupImportPolicyName(rg, protocol);
    RoutingPolicy.builder()
        .setOwner(c)
        .setName(policyName)
        .setStatements(
            ImmutableList.of(
                // Add default policy
                new SetDefaultPolicy(DEFAULT_IMPORT_POLICIES.get(protocol)),
                // Construct a policy chain based on defined import policies
                new If(
                    new FirstMatchChain(policyCalls),
                    ImmutableList.of(Statements.ReturnTrue.toStaticStatement()),
                    ImmutableList.of(Statements.ReturnFalse.toStaticStatement()))))
        .build();
    return new org.batfish.datamodel.dataplane.rib.RibGroup(
        rg.getName(), importRibs, policyName, exportRib);
  }

  private static String generateRibGroupImportPolicyName(RibGroup rg, RoutingProtocol protocol) {
    return String.format("~RIB_GROUP_IMPORT_POLICY_%s_%s~", rg.getName(), protocol);
  }

  @VisibleForTesting
  @Nullable
  static RibId toRibId(String hostname, String rib, @Nullable Warnings w) {
    String[] parts = rib.split("\\.");
    if (parts.length < 2 || parts.length > 3) {
      throw new VendorConversionException(String.format("Invalid RIB identifier %s", rib));
    }
    String vrfName;
    String addressFamily;
    String ribNumber;
    if (parts.length == 3) {
      vrfName = parts[0];
      addressFamily = parts[1];
      ribNumber = parts[2];
    } else { // length == 2
      vrfName = Configuration.DEFAULT_VRF_NAME;
      addressFamily = parts[0];
      ribNumber = parts[1];
    }

    String ribName = addressFamily + "." + ribNumber;
    // Normalize the common case into vendor-independent language
    if (ribName.equals(RIB_IPV4_UNICAST)) {
      ribName = RibId.DEFAULT_RIB_NAME;
    }

    // We only support IPv4 unicast
    if (!addressFamily.equals("inet") && w != null) {
      w.unimplemented(
          String.format("Rib name conversion: %s address family is not supported", addressFamily));
      return null;
    }
    return new RibId(hostname, vrfName, ribName);
  }

  /**
   * Convert a firewall filter into a policy that can be used for policy-based routing (or
   * filter-based forwarding, in Juniper parlance).
   */
  private PacketPolicy toPacketPolicy(FirewallFilter filter) {
    ImmutableList.Builder<org.batfish.datamodel.packet_policy.Statement> builder =
        ImmutableList.builder();
    for (Entry<String, FwTerm> e : filter.getTerms().entrySet()) {
      FwTerm term = e.getValue();

      /*
       * Convert "from" statements. Currently, the only supported "from"s are the ones matching on
       * headerspace, and they are ANDed together, so we collapse them into one big
       * AclMatch expression.
       */
      HeaderSpace.Builder matchCondition = HeaderSpace.builder();
      for (FwFrom from : term.getFroms()) {
        from.applyTo(matchCondition, this, _w, _c);
      }

      // A term will become an If statement. If (matchCondition) -> execute "then" statements
      builder.add(
          new org.batfish.datamodel.packet_policy.If(
              new PacketMatchExpr(
                  new MatchHeaderSpace(
                      matchCondition.build(),
                      String.format("Firewall filter term %s", term.getName()))),
              TermFwThenToPacketPolicyStatement.convert(term, Configuration.DEFAULT_VRF_NAME)));
    }

    // Make the policy, with an implicit deny all at the end as the default action
    return new PacketPolicy(filter.getName(), builder.build(), new Return(Drop.instance()));
  }

  private RoutingPolicy toRoutingPolicy(PolicyStatement ps) {
    // Ensure map of VRFs referenced in routing policies is initialized
    if (_vrfReferencesInPolicies == null) {
      _vrfReferencesInPolicies = new TreeMap<>();
    }
    String name = ps.getName();
    RoutingPolicy routingPolicy = new RoutingPolicy(name, _c);
    List<Statement> statements = routingPolicy.getStatements();
    boolean hasDefaultTerm =
        ps.getDefaultTerm().hasAtLeastOneFrom() || ps.getDefaultTerm().getThens().size() > 0;
    List<PsTerm> terms = new ArrayList<>(ps.getTerms().values());
    if (hasDefaultTerm) {
      terms.add(ps.getDefaultTerm());
    }
    for (PsTerm term : terms) {
      List<Statement> thens = toStatements(term.getThens());
      if (term.hasAtLeastOneFrom()) {
        If ifStatement = new If();
        ifStatement.setComment(term.getName());
        PsFroms froms = term.getFroms();

        for (PsFromRouteFilter fromRouteFilter : froms.getFromRouteFilters()) {
          int actionLineCounter = 0;
          String routeFilterName = fromRouteFilter.getRouteFilterName();
          RouteFilter rf = _masterLogicalSystem.getRouteFilters().get(routeFilterName);
          for (RouteFilterLine line : rf.getLines()) {
            if (line.getThens().size() > 0) {
              String lineListName = name + "_ACTION_LINE_" + actionLineCounter;
              RouteFilterList lineSpecificList = new RouteFilterList(lineListName);
              line.applyTo(lineSpecificList);
              actionLineCounter++;
              _c.getRouteFilterLists().put(lineListName, lineSpecificList);
              If lineSpecificIfStatement = new If();
              String lineSpecificClauseName = routeFilterName + "_ACTION_LINE_" + actionLineCounter;
              lineSpecificIfStatement.setComment(lineSpecificClauseName);
              MatchPrefixSet mrf =
                  new MatchPrefixSet(
                      DestinationNetwork.instance(), new NamedPrefixSet(lineListName));
              lineSpecificIfStatement.setGuard(mrf);
              lineSpecificIfStatement.getTrueStatements().addAll(toStatements(line.getThens()));
              statements.add(lineSpecificIfStatement);
            }
          }
        }
        if (froms.getFromInstance() != null) {
          _vrfReferencesInPolicies
              .computeIfAbsent(name, n -> new ArrayList<>())
              .add(froms.getFromInstance().getRoutingInstanceName());
        }
        ifStatement.setGuard(toGuard(froms));
        ifStatement.getTrueStatements().addAll(thens);
        statements.add(ifStatement);
      } else {
        statements.addAll(thens);
      }
    }
    If endOfPolicy = new If();
    endOfPolicy.setGuard(BooleanExprs.CALL_EXPR_CONTEXT);
    endOfPolicy.setFalseStatements(
        Collections.singletonList(Statements.Return.toStaticStatement()));
    statements.add(endOfPolicy);
    return routingPolicy;
  }

  private BooleanExpr toGuard(PsFroms froms) {
    if (froms.getFromUnsupported() != null) {
      // Unsupported line will evaluate to BooleanExprs.FALSE. Don't bother continuing
      return froms.getFromUnsupported().toBooleanExpr(this, _c, _w);
    }

    Conjunction conj = new Conjunction();
    List<BooleanExpr> subroutines = new ArrayList<>();
    if (!froms.getFromAsPaths().isEmpty()) {
      conj.getConjuncts().add(new Disjunction(toBooleanExprs(froms.getFromAsPaths())));
    }
    if (froms.getFromColor() != null) {
      conj.getConjuncts().add(froms.getFromColor().toBooleanExpr(this, _c, _w));
    }
    if (!froms.getFromCommunities().isEmpty()) {
      conj.getConjuncts().add(new Disjunction(toBooleanExprs(froms.getFromCommunities())));
    }
    if (froms.getFromFamily() != null) {
      conj.getConjuncts().add(froms.getFromFamily().toBooleanExpr(this, _c, _w));
    }
    if (froms.getFromInstance() != null) {
      conj.getConjuncts().add(froms.getFromInstance().toBooleanExpr(this, _c, _w));
    }
    if (!froms.getFromInterfaces().isEmpty()) {
      conj.getConjuncts().add(new Disjunction(toBooleanExprs(froms.getFromInterfaces())));
    }
    if (froms.getFromLocalPreference() != null) {
      conj.getConjuncts().add(froms.getFromLocalPreference().toBooleanExpr(this, _c, _w));
    }
    if (froms.getFromMetric() != null) {
      conj.getConjuncts().add(froms.getFromMetric().toBooleanExpr(this, _c, _w));
    }
    for (PsFromPolicyStatement from : froms.getFromPolicyStatements()) {
      subroutines.add(from.toBooleanExpr(this, _c, _w));
    }
    for (PsFromPolicyStatementConjunction from : froms.getFromPolicyStatementConjunctions()) {
      subroutines.add(from.toBooleanExpr(this, _c, _w));
    }
    if (!froms.getFromPrefixLists().isEmpty()
        || !froms.getFromPrefixListFilterLongers().isEmpty()
        || !froms.getFromPrefixListFilterOrLongers().isEmpty()
        || !froms.getFromRouteFilters().isEmpty()) {
      // TODO check behavior for some edge cases: https://github.com/batfish/batfish/issues/2972
      Disjunction prefixListDisjunction = new Disjunction();
      prefixListDisjunction.getDisjuncts().addAll(toBooleanExprs(froms.getFromPrefixLists()));
      prefixListDisjunction
          .getDisjuncts()
          .addAll(toBooleanExprs(froms.getFromPrefixListFilterLongers()));
      prefixListDisjunction
          .getDisjuncts()
          .addAll(toBooleanExprs(froms.getFromPrefixListFilterOrLongers()));
      prefixListDisjunction.getDisjuncts().addAll(toBooleanExprs(froms.getFromRouteFilters()));
      conj.getConjuncts().add(prefixListDisjunction);
    }
    if (!froms.getFromProtocols().isEmpty()) {
      conj.getConjuncts().add(new Disjunction(toBooleanExprs(froms.getFromProtocols())));
    }
    if (!froms.getFromTags().isEmpty()) {
      conj.getConjuncts().add(new Disjunction(toBooleanExprs(froms.getFromTags())));
    }

    if (!subroutines.isEmpty()) {
      ConjunctionChain chain = new ConjunctionChain(subroutines);
      conj.getConjuncts().add(chain);
    }
    return conj.simplify();
  }

  private List<BooleanExpr> toBooleanExprs(Set<? extends PsFrom> froms) {
    return froms.stream()
        .map(f -> f.toBooleanExpr(this, _c, _w))
        .collect(ImmutableList.toImmutableList());
  }

  private List<Statement> toStatements(Set<PsThen> thens) {
    List<Statement> thenStatements = new ArrayList<>();
    List<PsThen> reorderedThens = new LinkedList<>();
    for (PsThen then : thens) {
      if (then instanceof PsThenAccept
          || then instanceof PsThenReject
          || then instanceof PsThenDefaultActionAccept
          || then instanceof PsThenDefaultActionReject
          || then instanceof PsThenNextPolicy) {
        reorderedThens.add(then);
      } else {
        reorderedThens.add(0, then);
      }
    }
    for (PsThen then : reorderedThens) {
      then.applyTo(thenStatements, this, _c, _w);
    }
    return thenStatements;
  }

  private org.batfish.datamodel.StaticRoute toStaticRoute(StaticRoute route) {
    String nextHopInterface =
        route.getDrop()
            ? org.batfish.datamodel.Interface.NULL_INTERFACE_NAME
            : route.getNextHopInterface();

    return org.batfish.datamodel.StaticRoute.builder()
        .setNetwork(route.getPrefix())
        .setNextHopIp(firstNonNull(route.getNextHopIp(), Route.UNSET_ROUTE_NEXT_HOP_IP))
        .setNextHopInterface(nextHopInterface)
        .setAdministrativeCost(route.getDistance())
        .setMetric(route.getMetric())
        .setTag(firstNonNull(route.getTag(), AbstractRoute.NO_TAG))
        .setNonForwarding(firstNonNull(route.getNoInstall(), Boolean.FALSE))
        .build();
  }

  @Override
  public List<Configuration> toVendorIndependentConfigurations() throws VendorConversionException {
    ImmutableList.Builder<Configuration> outputConfigurations = ImmutableList.builder();
    _logicalSystems.keySet().stream()
        .map(this::toVendorIndependentConfiguration)
        .forEach(outputConfigurations::add);
    outputConfigurations.add(toVendorIndependentConfiguration());
    return outputConfigurations.build();
  }

  /** Creates and returns a vendor-independent configuration for the named logical-system. */
  private Configuration toVendorIndependentConfiguration(@Nullable String logicalSystemName) {
    JuniperConfiguration lsConfig = cloneConfiguration();
    lsConfig.processLogicalSystemConfiguration(logicalSystemName, this);
    return lsConfig.toVendorIndependentConfiguration();
  }

  private void processLogicalSystemConfiguration(
      @Nonnull String logicalSystemName, @Nonnull JuniperConfiguration masterConfiguration) {
    // Note that 'this' is the cloned configuration

    LogicalSystem ls = _logicalSystems.get(logicalSystemName);

    // Delete logical systems since they are no longer in scope
    _logicalSystems.clear();

    // Apply logical system settings onto cloned master
    // TODO: review which structures are shadowed, and which replaced entirely.
    _masterLogicalSystem.getApplications().putAll(ls.getApplications());
    _masterLogicalSystem.getApplicationSets().putAll(ls.getApplicationSets());
    _masterLogicalSystem.getAsPathGroups().putAll(ls.getAsPathGroups());
    // inherited?
    _masterLogicalSystem.getAuthenticationKeyChains().putAll(ls.getAuthenticationKeyChains());
    _masterLogicalSystem.getCommunityLists().putAll(ls.getCommunityLists());
    _masterLogicalSystem.setDefaultAddressSelection(ls.getDefaultAddressSelection());
    if (ls.getDefaultCrossZoneAction() != null) {
      _masterLogicalSystem.setDefaultCrossZoneAction(ls.getDefaultCrossZoneAction());
    }
    if (ls.getDefaultInboundAction() != null) {
      _masterLogicalSystem.setDefaultInboundAction(ls.getDefaultInboundAction());
    }
    _masterLogicalSystem.setDefaultRoutingInstance(ls.getDefaultRoutingInstance());
    _masterLogicalSystem.getDnsServers().clear();
    _masterLogicalSystem.getDnsServers().addAll(ls.getDnsServers());
    _masterLogicalSystem.getFirewallFilters().putAll(ls.getFirewallFilters());
    _masterLogicalSystem.getAddressBooks().putAll(ls.getAddressBooks());
    _masterLogicalSystem.getIkeGateways().clear();
    _masterLogicalSystem.getIkeGateways().putAll(ls.getIkeGateways());
    _masterLogicalSystem.getIkePolicies().clear();
    _masterLogicalSystem.getIkePolicies().putAll(ls.getIkePolicies());
    _masterLogicalSystem.getIkeProposals().clear();
    _masterLogicalSystem.getIkeProposals().putAll(ls.getIkeProposals());
    ls.getInterfaces()
        .forEach(
            (ifaceName, lsMasterIface) -> {
              Interface masterPhysicalInterface =
                  _masterLogicalSystem.getInterfaces().get(ifaceName);
              if (masterPhysicalInterface == null) {
                // the physical interface is not mentioned globally, so just copy the whole thing
                // from the logical system
                _masterLogicalSystem.getInterfaces().put(ifaceName, lsMasterIface);
                return;
              }
              // copy units from logical system
              masterPhysicalInterface.getUnits().putAll(lsMasterIface.getUnits());
              // delete unassigned units
              masterPhysicalInterface
                  .getUnits()
                  .keySet()
                  .retainAll(lsMasterIface.getUnits().keySet());
              // reset parent on copied units to master physical interface
              masterPhysicalInterface
                  .getUnits()
                  .values()
                  .forEach(unit -> unit.setParent(masterPhysicalInterface));
            });
    // delete unassigned interfaces
    _masterLogicalSystem.getInterfaces().keySet().retainAll(ls.getInterfaces().keySet());
    // TODO: review SRX logical-systems zone semantics
    _masterLogicalSystem.getInterfaceZones().clear();
    _masterLogicalSystem.getInterfaceZones().putAll(ls.getInterfaceZones());
    _masterLogicalSystem.getIpsecPolicies().clear();
    _masterLogicalSystem.getIpsecPolicies().putAll(ls.getIpsecPolicies());
    _masterLogicalSystem.getIpsecProposals().clear();
    _masterLogicalSystem.getIpsecProposals().putAll(ls.getIpsecProposals());
    _masterLogicalSystem.setNatDestination(ls.getNatDestination());
    _masterLogicalSystem.setNatSource(ls.getNatSource());
    _masterLogicalSystem.setNatStatic(ls.getNatStatic());
    // TODO: something with NTP servers?
    _masterLogicalSystem.getPolicyStatements().putAll(ls.getPolicyStatements());
    _masterLogicalSystem.getPrefixLists().putAll(ls.getPrefixLists());
    _masterLogicalSystem.getRouteFilters().putAll(ls.getRouteFilters());
    _masterLogicalSystem.getRoutingInstances().clear();
    _masterLogicalSystem.getRoutingInstances().putAll(ls.getRoutingInstances());
    // TODO: something with syslog hosts?
    // TODO: something with tacplus servers?
    _masterLogicalSystem.getNamedVlans().clear();
    _masterLogicalSystem.getNamedVlans().putAll(ls.getNamedVlans());
    _masterLogicalSystem.getZones().clear();
    _masterLogicalSystem.getZones().putAll(ls.getZones());

    // Ensure unique hostname in case one has not been configured
    if (getHostname() == null) {
      setHostname(
          computeLogicalSystemDefaultHostname(
              masterConfiguration.getHostname(), logicalSystemName));
    }
  }

  public static String computeLogicalSystemDefaultHostname(
      String masterHostname, String logicalSystemName) {
    return String.format("%s~logical_system~%s", masterHostname, logicalSystemName).toLowerCase();
  }

  private @Nonnull JuniperConfiguration cloneConfiguration() {
    JuniperConfiguration clonedConfiguration = SerializationUtils.clone(this);
    clonedConfiguration.setAnswerElement(getAnswerElement());
    clonedConfiguration.setUnrecognized(getUnrecognized());
    clonedConfiguration.setWarnings(_w);
    return clonedConfiguration;
  }

  private Configuration toVendorIndependentConfiguration() throws VendorConversionException {
    String hostname = getHostname();
    _c = new Configuration(hostname, _vendor);
    _c.setAuthenticationKeyChains(
        convertAuthenticationKeyChains(_masterLogicalSystem.getAuthenticationKeyChains()));
    _c.setDnsServers(_masterLogicalSystem.getDnsServers());
    _c.setDomainName(_masterLogicalSystem.getDefaultRoutingInstance().getDomainName());
    _c.setLoggingServers(_masterLogicalSystem.getSyslogHosts());
    _c.setNtpServers(_masterLogicalSystem.getNtpServers());
    _c.setTacacsServers(_masterLogicalSystem.getTacplusServers());
    _c.getVendorFamily().setJuniper(_masterLogicalSystem.getJf());
    for (String riName : _masterLogicalSystem.getRoutingInstances().keySet()) {
      _c.getVrfs().put(riName, new Vrf(riName));
    }

    // process interface ranges. this changes the _interfaces map
    _masterLogicalSystem.expandInterfaceRanges();

    // convert prefix lists to route filter lists
    for (Entry<String, PrefixList> e : _masterLogicalSystem.getPrefixLists().entrySet()) {
      String name = e.getKey();
      PrefixList pl = e.getValue();
      RouteFilterList rfl = new RouteFilterList(name);
      for (Prefix prefix : pl.getPrefixes()) {
        int prefixLength = prefix.getPrefixLength();
        org.batfish.datamodel.RouteFilterLine line =
            new org.batfish.datamodel.RouteFilterLine(
                LineAction.PERMIT, prefix, new SubRange(prefixLength, prefixLength));
        rfl.addLine(line);
      }
      _c.getRouteFilterLists().put(name, rfl);
    }

    // Convert AddressBooks to IpSpaces
    _masterLogicalSystem
        .getAddressBooks()
        .forEach(
            (name, addressBook) -> {
              Map<String, IpSpace> ipspaces = toIpSpaces(name, addressBook);
              _c.getIpSpaces().putAll(ipspaces);
              ipspaces
                  .keySet()
                  .forEach(
                      ipSpaceName ->
                          _c.getIpSpaceMetadata()
                              .put(
                                  ipSpaceName,
                                  new IpSpaceMetadata(ipSpaceName, ADDRESS_BOOK.getDescription())));
            });

    // TODO: instead make both IpAccessList and Ip6AccessList instances from
    // such firewall filters
    // remove ipv6 lines from firewall filters
    for (FirewallFilter filter : _masterLogicalSystem.getFirewallFilters().values()) {
      Set<String> toRemove = new HashSet<>();
      for (Entry<String, FwTerm> e2 : filter.getTerms().entrySet()) {
        String termName = e2.getKey();
        FwTerm term = e2.getValue();
        if (term.getIpv6()) {
          toRemove.add(termName);
        }
      }
      for (String termName : toRemove) {
        filter.getTerms().remove(termName);
      }
    }

    // remove empty firewall filters (ipv6-only filters)
    Map<String, FirewallFilter> allFilters =
        new LinkedHashMap<>(_masterLogicalSystem.getFirewallFilters());
    for (Entry<String, FirewallFilter> e : allFilters.entrySet()) {
      String name = e.getKey();
      FirewallFilter filter = e.getValue();
      if (filter.getTerms().size() == 0) {
        _masterLogicalSystem.getFirewallFilters().remove(name);
      }
    }

    // convert firewall filters to ipaccesslists
    for (Entry<String, FirewallFilter> e : _masterLogicalSystem.getFirewallFilters().entrySet()) {
      String name = e.getKey();
      FirewallFilter filter = e.getValue();
      // TODO: support other filter families
      if (filter.getFamily() != Family.INET) {
        continue;
      }
      IpAccessList list = toIpAccessList(filter);
      _c.getIpAccessLists().put(name, list);
    }

    // convert firewall filters implementing packet policy to PacketPolicy objects
    for (Entry<String, FirewallFilter> e : _masterLogicalSystem.getFirewallFilters().entrySet()) {
      String name = e.getKey();
      FirewallFilter filter = e.getValue();
      if (filter.isUsedForFBF()) {
        // TODO: support other filter families
        if (filter.getFamily() != Family.INET) {
          continue;
        }
        _c.getPacketPolicies().put(name, toPacketPolicy(filter));
      }
    }

    // convert route filters to route filter lists
    for (Entry<String, RouteFilter> e : _masterLogicalSystem.getRouteFilters().entrySet()) {
      String name = e.getKey();
      RouteFilter rf = e.getValue();
      if (rf.getIpv4()) {
        RouteFilterList rfl = new RouteFilterList(name);
        for (RouteFilterLine line : rf.getLines()) {
          if (line.getThens().size() == 0) {
            line.applyTo(rfl);
          }
        }
        _c.getRouteFilterLists().put(name, rfl);
      }
      if (rf.getIpv6()) {
        Route6FilterList rfl = new Route6FilterList(name);
        for (RouteFilterLine line : rf.getLines()) {
          if (line.getThens().size() == 0) {
            line.applyTo(rfl);
          }
        }
        _c.getRoute6FilterLists().put(name, rfl);
      }
    }

    // convert community lists
    for (Entry<String, CommunityList> e : _masterLogicalSystem.getCommunityLists().entrySet()) {
      String name = e.getKey();
      CommunityList cl = e.getValue();
      org.batfish.datamodel.CommunityList newCl = toCommunityList(cl);
      _c.getCommunityLists().put(name, newCl);
    }

    // convert interfaces. Before policies because some policies depend on interfaces
    convertInterfaces();

    // convert policy-statements to RoutingPolicy objects
    for (Entry<String, PolicyStatement> e : _masterLogicalSystem.getPolicyStatements().entrySet()) {
      String name = e.getKey();
      PolicyStatement ps = e.getValue();
      RoutingPolicy routingPolicy = toRoutingPolicy(ps);
      _c.getRoutingPolicies().put(name, routingPolicy);
    }

    // set router-id
    if (_masterLogicalSystem.getDefaultRoutingInstance().getRouterId() == null) {
      Interface loopback0 =
          _masterLogicalSystem
              .getDefaultRoutingInstance()
              .getInterfaces()
              .get(FIRST_LOOPBACK_INTERFACE_NAME);
      if (loopback0 != null) {
        Interface loopback0unit0 = loopback0.getUnits().get(FIRST_LOOPBACK_INTERFACE_NAME + ".0");
        if (loopback0unit0 != null) {
          InterfaceAddress address = loopback0unit0.getPrimaryAddress();
          if (address != null) {
            // now we should set router-id
            Ip routerId = address.getIp();
            _masterLogicalSystem.getDefaultRoutingInstance().setRouterId(routerId);
          }
        }
      }
    }

    _masterLogicalSystem
        .getIkeProposals()
        .values()
        .forEach(
            ikeProposal ->
                _c.getIkePhase1Proposals()
                    .put(ikeProposal.getName(), toIkePhase1Proposal(ikeProposal)));

    ImmutableSortedMap.Builder<String, IkePhase1Key> ikePhase1KeysBuilder =
        ImmutableSortedMap.naturalOrder();

    // convert ike policies
    for (Entry<String, IkePolicy> e : _masterLogicalSystem.getIkePolicies().entrySet()) {
      String name = e.getKey();
      IkePolicy oldIkePolicy = e.getValue();
      // storing IKE phase 1 policy
      _c.getIkePhase1Policies().put(name, toIkePhase1Policy(oldIkePolicy, ikePhase1KeysBuilder));
    }

    _c.setIkePhase1Keys(ikePhase1KeysBuilder.build());

    // convert ipsec proposals
    ImmutableSortedMap.Builder<String, IpsecPhase2Proposal> ipsecPhase2ProposalsBuilder =
        ImmutableSortedMap.naturalOrder();
    _masterLogicalSystem
        .getIpsecProposals()
        .forEach(
            (ipsecProposalName, ipsecProposal) -> {
              ipsecPhase2ProposalsBuilder.put(
                  ipsecProposalName, toIpsecPhase2Proposal(ipsecProposal));
            });
    _c.setIpsecPhase2Proposals(ipsecPhase2ProposalsBuilder.build());

    // convert ipsec policies
    ImmutableSortedMap.Builder<String, IpsecPhase2Policy> ipsecPhase2PoliciesBuilder =
        ImmutableSortedMap.naturalOrder();
    for (Entry<String, IpsecPolicy> e : _masterLogicalSystem.getIpsecPolicies().entrySet()) {
      ipsecPhase2PoliciesBuilder.put(e.getKey(), toIpsecPhase2Policy(e.getValue()));
    }
    _c.setIpsecPhase2Policies(ipsecPhase2PoliciesBuilder.build());

    // convert Tunnels
    ImmutableSortedMap.Builder<String, IpsecPeerConfig> ipsecPeerConfigBuilder =
        ImmutableSortedMap.naturalOrder();
    for (Entry<String, IpsecVpn> e : _masterLogicalSystem.getIpsecVpns().entrySet()) {
      IpsecPeerConfig ipsecPeerConfig = toIpsecPeerConfig(e.getValue());
      if (ipsecPeerConfig != null) {
        ipsecPeerConfigBuilder.put(e.getKey(), ipsecPeerConfig);
      }
    }
    _c.setIpsecPeerConfigs(ipsecPeerConfigBuilder.build());

    // zones
    for (Zone zone : _masterLogicalSystem.getZones().values()) {
      org.batfish.datamodel.Zone newZone = toZone(zone);
      _c.getZones().put(zone.getName(), newZone);
      if (zone.getAddressBookType() == AddressBookType.INLINED) {
        Map<String, IpSpace> ipSpaces = toIpSpaces(zone.getName(), zone.getAddressBook());
        _c.getIpSpaces().putAll(ipSpaces);
        ipSpaces
            .keySet()
            .forEach(
                ipSpaceName ->
                    _c.getIpSpaceMetadata()
                        .put(
                            ipSpaceName,
                            new IpSpaceMetadata(ipSpaceName, ADDRESS_BOOK.getDescription())));
      }
    }
    // If there are zones, then assume we will need to support existing connection ACL
    if (!_masterLogicalSystem.getZones().isEmpty()) {
      _c.getIpAccessLists().put(ACL_NAME_EXISTING_CONNECTION, ACL_EXISTING_CONNECTION);
    }

    // default zone behavior
    _c.setDefaultCrossZoneAction(_masterLogicalSystem.getDefaultCrossZoneAction());
    _c.setDefaultInboundAction(_masterLogicalSystem.getDefaultInboundAction());

    for (Entry<String, RoutingInstance> e : _masterLogicalSystem.getRoutingInstances().entrySet()) {
      String riName = e.getKey();
      RoutingInstance ri = e.getValue();
      Vrf vrf = _c.getVrfs().get(riName);

      // dhcp relay
      for (Entry<String, DhcpRelayGroup> e2 : ri.getDhcpRelayGroups().entrySet()) {
        DhcpRelayGroup rg = e2.getValue();
        List<org.batfish.datamodel.Interface> interfaces = new ArrayList<>();
        if (rg.getAllInterfaces()) {
          interfaces.addAll(_c.getAllInterfaces().values());
        } else {
          rg.getInterfaces().stream()
              .map(_c.getAllInterfaces()::get)
              .filter(Objects::nonNull)
              .forEach(interfaces::add);
        }
        String asgName = rg.getActiveServerGroup();
        if (asgName != null) {
          DhcpRelayServerGroup asg = ri.getDhcpRelayServerGroups().get(asgName);
          if (asg != null) {
            for (org.batfish.datamodel.Interface iface : interfaces) {
              iface.setDhcpRelayAddresses(
                  ImmutableList.<Ip>builder()
                      .addAll(iface.getDhcpRelayAddresses())
                      .addAll(asg.getServers())
                      .build());
            }
          }
        }
      }

      // snmp
      SnmpServer snmpServer = ri.getSnmpServer();
      vrf.setSnmpServer(snmpServer);
      if (snmpServer != null) {
        _c.getSnmpTrapServers().addAll(snmpServer.getHosts().keySet());
      }

      // static routes
      for (StaticRoute route : ri.getRibs().get(RIB_IPV4_UNICAST).getStaticRoutes().values()) {
        org.batfish.datamodel.StaticRoute newStaticRoute = toStaticRoute(route);
        vrf.getStaticRoutes().add(newStaticRoute);
      }

      // aggregate routes
      for (AggregateRoute route :
          ri.getRibs().get(RIB_IPV4_UNICAST).getAggregateRoutes().values()) {
        route.inheritUnsetFields(ri.getAggregateRouteDefaults());
        org.batfish.datamodel.GeneratedRoute newAggregateRoute = toAggregateRoute(route);
        vrf.getGeneratedRoutes().add(newAggregateRoute);
      }

      // generated routes
      for (GeneratedRoute route :
          ri.getRibs().get(RIB_IPV4_UNICAST).getGeneratedRoutes().values()) {
        route.inheritUnsetFields(ri.getGeneratedRouteDefaults());
        org.batfish.datamodel.GeneratedRoute newGeneratedRoute = toGeneratedRoute(route);
        vrf.getGeneratedRoutes().add(newGeneratedRoute);
      }

      // Set up import policy for cross-VRF route leaking using instance-import
      // At this point configured policy-statements have already been added to _c as RoutingPolicies
      if (!ri.getInstanceImports().isEmpty()) {
        // Only routes from these VRFs will be considered for import
        List<String> referencedVrfs = getVrfsReferencedByPolicies(ri.getInstanceImports());
        initDefaultRejectPolicy();
        RoutingPolicy instanceImportPolicy = buildInstanceImportRoutingPolicy(ri, _c, riName);

        vrf.setCrossVrfImportVrfs(referencedVrfs);
        vrf.setCrossVrfImportPolicy(instanceImportPolicy.getName());
        _c.getRoutingPolicies().put(instanceImportPolicy.getName(), instanceImportPolicy);
      }

      /*
       * RIB groups applied to each protocol.
       *
       * 1. ensure default import policies exist
       * 2. convert VS rib groups to VI rib groups on a per-protocol basis
       */
      if (!ri.getAppliedRibGroups().isEmpty()) {
        initDefaultImportPolicies();
      }
      vrf.setAppliedRibGroups(
          ri.getAppliedRibGroups().entrySet().stream()
              .collect(
                  ImmutableMap.toImmutableMap(
                      Entry::getKey, // protocol
                      rgEntry ->
                          toRibGroup(
                              _masterLogicalSystem.getRibGroups().get(rgEntry.getValue()),
                              rgEntry.getKey(),
                              _c,
                              riName,
                              _w))));

      // Create OSPF process (oproc will be null iff disable is configured at process level)
      if (ri.getOspfAreas().size() > 0) {
        OspfProcess oproc = createOspfProcess(ri);
        if (oproc != null) {
          vrf.setOspfProcesses(ImmutableSortedMap.of(oproc.getProcessId(), oproc));
          // add discard routes for OSPF summaries
          oproc.getAreas().values().stream()
              .flatMap(a -> a.getSummaries().entrySet().stream())
              .forEach(
                  summaryEntry ->
                      vrf.getGeneratedRoutes()
                          .add(
                              ospfSummaryToAggregateRoute(
                                  summaryEntry.getKey(), summaryEntry.getValue())));
        }
      }

      // create is-is process
      // is-is runs only if at least one interface has an ISO address, check loopback first
      Optional<IsoAddress> isoAddress =
          _masterLogicalSystem.getDefaultRoutingInstance().getInterfaces().values().stream()
              .filter(i -> i.getName().startsWith(FIRST_LOOPBACK_INTERFACE_NAME))
              .map(Interface::getIsoAddress)
              .filter(Objects::nonNull)
              .min(Comparator.comparing(IsoAddress::toString));
      // Try all the other interfaces if no ISO address on Loopback
      if (!isoAddress.isPresent()) {
        isoAddress =
            _masterLogicalSystem.getDefaultRoutingInstance().getInterfaces().values().stream()
                .map(Interface::getIsoAddress)
                .filter(Objects::nonNull)
                .min(Comparator.comparing(IsoAddress::toString));
      }
      if (isoAddress.isPresent()) {
        // now we should create is-is process
        IsisProcess proc = createIsisProcess(ri, isoAddress.get());
        vrf.setIsisProcess(proc);
      }

      // create bgp process
      if (ri.getNamedBgpGroups().size() > 0 || ri.getIpBgpGroups().size() > 0) {
        BgpProcess proc = createBgpProcess(ri);
        vrf.setBgpProcess(proc);
      }
    }

    // static nats
    if (_masterLogicalSystem.getNatStatic() != null) {
      _w.unimplemented("Static NAT is not currently implemented");
    }

    // mark forwarding table export policy if it exists
    String forwardingTableExportPolicyName =
        _masterLogicalSystem.getDefaultRoutingInstance().getForwardingTableExportPolicy();
    if (forwardingTableExportPolicyName != null) {
      PolicyStatement forwardingTableExportPolicy =
          _masterLogicalSystem.getPolicyStatements().get(forwardingTableExportPolicyName);
      if (forwardingTableExportPolicy != null) {
        setPolicyStatementReferent(forwardingTableExportPolicyName);
      }
    }

    // Count and mark structure usages and identify undefined references
    markConcreteStructure(
        JuniperStructureType.ADDRESS_BOOK, JuniperStructureUsage.ADDRESS_BOOK_ATTACH_ZONE);
    markConcreteStructure(
        JuniperStructureType.AS_PATH, JuniperStructureUsage.POLICY_STATEMENT_FROM_AS_PATH);
    markConcreteStructure(
        JuniperStructureType.AUTHENTICATION_KEY_CHAIN,
        JuniperStructureUsage.AUTHENTICATION_KEY_CHAINS_POLICY);
    markAbstractStructure(
        JuniperStructureType.APPLICATION_OR_APPLICATION_SET,
        JuniperStructureUsage.SECURITY_POLICY_MATCH_APPLICATION,
        ImmutableList.of(JuniperStructureType.APPLICATION, JuniperStructureType.APPLICATION_SET));
    markAbstractStructure(
        JuniperStructureType.APPLICATION_OR_APPLICATION_SET,
        JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION,
        ImmutableList.of(JuniperStructureType.APPLICATION, JuniperStructureType.APPLICATION_SET));
    markConcreteStructure(
        JuniperStructureType.APPLICATION_SET,
        JuniperStructureUsage.APPLICATION_SET_MEMBER_APPLICATION_SET);
    markConcreteStructure(
        JuniperStructureType.BGP_GROUP,
        JuniperStructureUsage.BGP_ALLOW,
        JuniperStructureUsage.BGP_NEIGHBOR);
    markConcreteStructure(
        JuniperStructureType.FIREWALL_FILTER,
        JuniperStructureUsage.INTERFACE_FILTER,
        JuniperStructureUsage.INTERFACE_INCOMING_FILTER,
        JuniperStructureUsage.INTERFACE_OUTGOING_FILTER);
    markConcreteStructure(
        JuniperStructureType.INTERFACE,
        JuniperStructureUsage.FORWARDING_OPTIONS_DHCP_RELAY_GROUP_INTERFACE,
        JuniperStructureUsage.IKE_GATEWAY_EXTERNAL_INTERFACE,
        JuniperStructureUsage.INTERFACE_SELF_REFERENCE,
        JuniperStructureUsage.IPSEC_VPN_BIND_INTERFACE,
        JuniperStructureUsage.ISIS_INTERFACE,
        JuniperStructureUsage.OSPF_AREA_INTERFACE,
        JuniperStructureUsage.POLICY_STATEMENT_FROM_INTERFACE,
        JuniperStructureUsage.ROUTING_INSTANCE_INTERFACE,
        JuniperStructureUsage.SECURITY_ZONES_SECURITY_ZONES_INTERFACE,
        JuniperStructureUsage.STATIC_ROUTE_NEXT_HOP_INTERFACE,
        JuniperStructureUsage.VTEP_SOURCE_INTERFACE);
    markConcreteStructure(
        JuniperStructureType.POLICY_STATEMENT,
        JuniperStructureUsage.BGP_EXPORT_POLICY,
        JuniperStructureUsage.BGP_IMPORT_POLICY,
        JuniperStructureUsage.FORWARDING_TABLE_EXPORT_POLICY,
        JuniperStructureUsage.GENERATED_ROUTE_POLICY,
        JuniperStructureUsage.OSPF_EXPORT_POLICY,
        JuniperStructureUsage.POLICY_STATEMENT_POLICY,
        JuniperStructureUsage.ROUTING_INSTANCE_VRF_EXPORT,
        JuniperStructureUsage.ROUTING_INSTANCE_VRF_IMPORT,
        JuniperStructureUsage.ROUTING_OPTIONS_INSTANCE_IMPORT);
    markConcreteStructure(
        JuniperStructureType.PREFIX_LIST,
        JuniperStructureUsage.FIREWALL_FILTER_DESTINATION_PREFIX_LIST,
        JuniperStructureUsage.FIREWALL_FILTER_PREFIX_LIST,
        JuniperStructureUsage.FIREWALL_FILTER_SOURCE_PREFIX_LIST,
        JuniperStructureUsage.POLICY_STATEMENT_PREFIX_LIST,
        JuniperStructureUsage.POLICY_STATEMENT_PREFIX_LIST_FILTER,
        JuniperStructureUsage.SNMP_COMMUNITY_PREFIX_LIST);
    markConcreteStructure(JuniperStructureType.VLAN, JuniperStructureUsage.INTERFACE_VLAN);

    markConcreteStructure(
        JuniperStructureType.DHCP_RELAY_SERVER_GROUP,
        JuniperStructureUsage.DHCP_RELAY_GROUP_ACTIVE_SERVER_GROUP);

    markConcreteStructure(
        JuniperStructureType.IKE_GATEWAY, JuniperStructureUsage.IPSEC_VPN_IKE_GATEWAY);
    markConcreteStructure(
        JuniperStructureType.IKE_POLICY, JuniperStructureUsage.IKE_GATEWAY_IKE_POLICY);
    markConcreteStructure(
        JuniperStructureType.IKE_PROPOSAL, JuniperStructureUsage.IKE_POLICY_IKE_PROPOSAL);
    markConcreteStructure(
        JuniperStructureType.IPSEC_PROPOSAL, JuniperStructureUsage.IPSEC_POLICY_IPSEC_PROPOSAL);
    markConcreteStructure(
        JuniperStructureType.IPSEC_PROPOSAL, JuniperStructureUsage.IPSEC_VPN_IPSEC_POLICY);
    markConcreteStructure(
        JuniperStructureType.LOGICAL_SYSTEM, JuniperStructureUsage.SECURITY_PROFILE_LOGICAL_SYSTEM);

    markConcreteStructure(
        JuniperStructureType.NAT_POOL,
        JuniperStructureUsage.NAT_DESTINATINATION_RULE_SET_RULE_THEN,
        JuniperStructureUsage.NAT_SOURCE_RULE_SET_RULE_THEN,
        JuniperStructureUsage.NAT_STATIC_RULE_SET_RULE_THEN);

    markConcreteStructure(
        JuniperStructureType.ROUTING_INSTANCE,
        JuniperStructureUsage.POLICY_STATEMENT_FROM_INSTANCE);

    warnEmptyPrefixLists();

    _c.computeRoutingPolicySources(_w);

    return _c;
  }

  /** Initialize default protocol-specific import policies */
  private void initDefaultImportPolicies() {
    initDefaultBgpImportPolicy();
    initDefaultPseudoProtocolImportPolicy();
  }

  private void convertInterfaces() {
    // Set IRB vlan IDs by resolving l3-interface from named VLANs.
    // If more than one named vlan refers to a given l3-interface, we just keep the first assignment
    // and warn.
    Map<String, Integer> irbVlanIds = new HashMap<>();
    for (Vlan vlan : _masterLogicalSystem.getNamedVlans().values()) {
      Integer vlanId = vlan.getVlanId();
      String l3Interface = vlan.getL3Interface();
      if (l3Interface == null || vlanId == null) {
        continue;
      }
      if (irbVlanIds.containsKey(l3Interface)) {
        _w.redFlag(
            String.format(
                "Cannot assign '%s' as the l3-interface of vlan '%s' since it is already assigned to vlan '%s'",
                l3Interface, vlanId, irbVlanIds.get(l3Interface)));
        continue;
      }
      irbVlanIds.put(l3Interface, vlanId);
    }

    // Get a stream of all interfaces (including Node interfaces)
    Stream.concat(
            _masterLogicalSystem.getInterfaces().values().stream(),
            _nodeDevices.values().stream()
                .flatMap(nodeDevice -> nodeDevice.getInterfaces().values().stream()))
        .forEach(
            /*
             * For each interface, add it to the VI model. Since Juniper splits attributes
             * between physical and logical (unit) interfaces, do the conversion in two steps.
             * - Physical interface first, with physical attributes: speed, aggregation tracking, etc.
             * - Then all units of the interface. Units have the attributes batfish
             *   cares most about: IPs, MTUs, ACLs, etc.)
             */
            iface -> {
              // Process parent interface
              iface.inheritUnsetFields();
              org.batfish.datamodel.Interface newParentIface = toInterfaceNonUnit(iface);
              resolveInterfacePointers(iface.getName(), iface, newParentIface);

              // Process the units, which hold the bulk of the configuration
              iface
                  .getUnits()
                  .values()
                  .forEach(
                      unit -> {
                        unit.inheritUnsetFields();
                        org.batfish.datamodel.Interface newUnitInterface = toInterface(unit);
                        String name = newUnitInterface.getName();
                        // set IRB VLAN ID if assigned
                        newUnitInterface.setVlan(irbVlanIds.get(name));

                        // Don't create bind dependency for 'irb.XXX' interfcaes, since there isn't
                        // really an 'irb' interface
                        if (!name.startsWith("irb")) {
                          newUnitInterface.addDependency(
                              new Dependency(newParentIface.getName(), DependencyType.BIND));
                        }
                        resolveInterfacePointers(unit.getName(), unit, newUnitInterface);
                      });
            });

    /*
     * Do a second pass where we look over all interfaces
     * and set dependency pointers for aggregated interfaces in the VI configuration
     */
    Stream.concat(
            _masterLogicalSystem.getInterfaces().values().stream(),
            _nodeDevices.values().stream()
                .flatMap(nodeDevice -> nodeDevice.getInterfaces().values().stream()))
        .forEach(
            iface -> {
              if (iface.get8023adInterface() != null) {
                org.batfish.datamodel.Interface viIface =
                    _c.getAllInterfaces().get(iface.get8023adInterface());
                if (viIface == null) {
                  return;
                }
                viIface.addDependency(new Dependency(iface.getName(), DependencyType.AGGREGATE));
              }
              /*
               * TODO: reth interfaces are NOT aggregates in pure form, but for now approximate them
               * as such. Full support requires chassis clusters and redundancy group support.
               * https://www.juniper.net/documentation/en_US/junos/topics/topic-map/security-chassis-cluster-redundant-ethernet-interfaces.html
               */
              if (iface.getRedundantParentInterface() != null) {
                org.batfish.datamodel.Interface viIface =
                    _c.getAllInterfaces().get(iface.getRedundantParentInterface());
                if (viIface == null) {
                  return;
                }
                viIface.addDependency(new Dependency(iface.getName(), DependencyType.AGGREGATE));
              }
            });

    Nat snat = _masterLogicalSystem.getNatSource();
    Nat staticNat = _masterLogicalSystem.getNatStatic();

    if (snat == null && staticNat == null) {
      return;
    }

    List<NatRuleSet> sourceNatRuleSetList =
        snat == null
            ? null
            : snat.getRuleSets().values().stream()
                .sorted()
                .collect(ImmutableList.toImmutableList());

    Nat reversedStaticNat = staticNat == null ? null : ReverseStaticNat.reverseNat(staticNat);
    List<NatRuleSet> reversedStaticNatRuleSetList =
        reversedStaticNat == null
            ? null
            : reversedStaticNat.getRuleSets().values().stream()
                .sorted()
                .collect(ImmutableList.toImmutableList());

    Map<NatPacketLocation, AclLineMatchExpr> matchFromLocationExprs =
        fromNatPacketLocationMatchExprs();

    Stream.concat(
            _masterLogicalSystem.getInterfaces().values().stream(),
            _nodeDevices.values().stream()
                .flatMap(nodeDevice -> nodeDevice.getInterfaces().values().stream()))
        .forEach(
            iface ->
                iface
                    .getUnits()
                    .values()
                    .forEach(
                        unit -> {
                          org.batfish.datamodel.Interface newUnitInterface =
                              _c.getAllInterfaces().get(unit.getName());
                          Transformation srcTransformation =
                              buildOutgoingTransformation(
                                  unit, snat, sourceNatRuleSetList, matchFromLocationExprs, null);
                          Transformation staticTransformation =
                              buildOutgoingTransformation(
                                  unit,
                                  reversedStaticNat,
                                  reversedStaticNatRuleSetList,
                                  matchFromLocationExprs,
                                  srcTransformation);
                          newUnitInterface.setOutgoingTransformation(staticTransformation);
                        }));
  }

  /** Ensure that the interface is placed in VI {@link Configuration} and {@link Vrf} */
  private void resolveInterfacePointers(
      String ifaceName, Interface iface, org.batfish.datamodel.Interface viIface) {
    Vrf vrf = viIface.getVrf();
    String vrfName = vrf.getName();
    _masterLogicalSystem.getRoutingInstances().get(vrfName).getInterfaces().put(ifaceName, iface);
    if (ifaceName.equals("irb")) {
      // there is no 'irb' interface; it is just a namespace with no inheritable parameters
      return;
    }
    _c.getAllInterfaces().put(ifaceName, viIface);
    vrf.getInterfaces().put(ifaceName, viIface);
  }

  private org.batfish.datamodel.Zone toZone(Zone zone) {
    String zoneName = zone.getName();

    FirewallFilter inboundFilter = zone.getInboundFilter();
    IpAccessList inboundFilterList = null;
    if (inboundFilter != null) {
      inboundFilterList = _c.getIpAccessLists().get(inboundFilter.getName());
    }

    FirewallFilter fromHostFilter = zone.getFromHostFilter();
    IpAccessList fromHostFilterList = null;
    if (fromHostFilter != null) {
      fromHostFilterList = _c.getIpAccessLists().get(fromHostFilter.getName());
    }

    FirewallFilter toHostFilter = zone.getToHostFilter();
    IpAccessList toHostFilterList = null;
    if (toHostFilter != null) {
      toHostFilterList = _c.getIpAccessLists().get(toHostFilter.getName());
    }

    org.batfish.datamodel.Zone newZone = new org.batfish.datamodel.Zone(zoneName);
    if (fromHostFilterList != null) {
      newZone.setFromHostFilterName(fromHostFilterList.getName());
    }
    if (inboundFilterList != null) {
      newZone.setInboundFilterName(inboundFilterList.getName());
    }
    if (toHostFilterList != null) {
      newZone.setToHostFilterName(toHostFilterList.getName());
    }

    newZone.setInboundInterfaceFiltersNames(new TreeMap<>());
    for (Entry<String, FirewallFilter> e : zone.getInboundInterfaceFilters().entrySet()) {
      String inboundInterfaceName = e.getKey();
      FirewallFilter inboundInterfaceFilter = e.getValue();
      String inboundInterfaceFilterName = inboundInterfaceFilter.getName();
      org.batfish.datamodel.Interface newIface = _c.getAllInterfaces().get(inboundInterfaceName);
      newZone.getInboundInterfaceFiltersNames().put(newIface.getName(), inboundInterfaceFilterName);
    }

    newZone.setToZonePoliciesNames(new TreeMap<>());
    for (Entry<String, FirewallFilter> e : zone.getToZonePolicies().entrySet()) {
      String toZoneName = e.getKey();
      FirewallFilter toZoneFilter = e.getValue();
      String toZoneFilterName = toZoneFilter.getName();
      newZone.getToZonePoliciesNames().put(toZoneName, toZoneFilterName);
    }

    newZone.setInboundInterfaceFiltersNames(new TreeMap<>());
    for (String ifaceName : zone.getInterfaces()) {
      org.batfish.datamodel.Interface newIface = _c.getAllInterfaces().get(ifaceName);
      if (newIface == null) {
        // undefined reference to ifaceName
        continue;
      }
      newIface.setZoneName(zoneName);
      FirewallFilter inboundInterfaceFilter = zone.getInboundInterfaceFilters().get(ifaceName);
      if (inboundInterfaceFilter != null) {
        newZone
            .getInboundInterfaceFiltersNames()
            .put(newIface.getName(), inboundInterfaceFilter.getName());
      } else if (inboundFilterList != null) {
        newZone
            .getInboundInterfaceFiltersNames()
            .put(newIface.getName(), inboundFilterList.getName());
      }
    }

    return newZone;
  }

  private void warnEmptyPrefixLists() {
    for (Entry<String, PrefixList> e : _masterLogicalSystem.getPrefixLists().entrySet()) {
      String name = e.getKey();
      PrefixList prefixList = e.getValue();
      if (!prefixList.getIpv6() && prefixList.getPrefixes().isEmpty()) {
        _w.redFlag("Empty prefix-list: '" + name + "'");
      }
    }
  }

  private Ip getOspfRouterId(RoutingInstance routingInstance) {
    Ip routerId = routingInstance.getRouterId();
    if (routerId == null) {
      Map<String, Interface> interfacesToCheck;
      Map<String, Interface> allInterfaces = routingInstance.getInterfaces();
      Map<String, Interface> loopbackInterfaces =
          allInterfaces.entrySet().stream()
              .filter(
                  e ->
                      e.getKey().toLowerCase().startsWith("lo")
                          && e.getValue().getActive()
                          && e.getValue().getPrimaryAddress() != null)
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      interfacesToCheck = loopbackInterfaces.isEmpty() ? allInterfaces : loopbackInterfaces;

      Ip lowesetIp = Ip.MAX;
      for (Interface iface : interfacesToCheck.values()) {
        if (!iface.getActive()) {
          continue;
        }
        for (InterfaceAddress address : iface.getAllAddresses()) {
          Ip ip = address.getIp();
          if (lowesetIp.asLong() > ip.asLong()) {
            lowesetIp = ip;
          }
        }
      }
      if (lowesetIp == Ip.MAX) {
        _w.redFlag("No candidates for OSPF router-id");
        return null;
      }
      routerId = lowesetIp;
    }
    return routerId;
  }

  public @Nonnull Map<String, LogicalSystem> getLogicalSystems() {
    return _logicalSystems;
  }

  public LogicalSystem getMasterLogicalSystem() {
    return _masterLogicalSystem;
  }

  @Override
  public String getHostname() {
    return _masterLogicalSystem.getHostname();
  }

  @Override
  public void setHostname(String hostname) {
    _masterLogicalSystem.setHostname(hostname);
  }

  private static String interfaceUnitMasterName(String unitName) {
    int pos = unitName.indexOf('.');
    String master = unitName.substring(0, pos);
    return master;
  }
}

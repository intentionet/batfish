package org.batfish.representation.juniper;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.collections4.list.TreeList;
import org.batfish.common.BatfishException;
import org.batfish.common.VendorConversionException;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.AclIpSpaceLine;
import org.batfish.datamodel.AuthenticationKey;
import org.batfish.datamodel.AuthenticationKeyChain;
import org.batfish.datamodel.BgpActivePeerConfig;
import org.batfish.datamodel.BgpAuthenticationAlgorithm;
import org.batfish.datamodel.BgpAuthenticationSettings;
import org.batfish.datamodel.BgpPassivePeerConfig;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConfigurationFormat;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IkeKeyType;
import org.batfish.datamodel.IkePhase1Key;
import org.batfish.datamodel.IkePhase1Policy;
import org.batfish.datamodel.IkePhase1Proposal;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
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
import org.batfish.datamodel.Route;
import org.batfish.datamodel.Route6FilterList;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.SnmpServer;
import org.batfish.datamodel.State;
import org.batfish.datamodel.SubRange;
import org.batfish.datamodel.SwitchportEncapsulationType;
import org.batfish.datamodel.SwitchportMode;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.AndMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.MatchSrcInterface;
import org.batfish.datamodel.acl.NotMatchExpr;
import org.batfish.datamodel.acl.OriginatingFromDevice;
import org.batfish.datamodel.acl.PermittedByAcl;
import org.batfish.datamodel.acl.TrueExpr;
import org.batfish.datamodel.isis.IsisInterfaceMode;
import org.batfish.datamodel.isis.IsisProcess;
import org.batfish.datamodel.ospf.OspfMetricType;
import org.batfish.datamodel.ospf.OspfProcess;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.BooleanExpr;
import org.batfish.datamodel.routing_policy.expr.BooleanExprs;
import org.batfish.datamodel.routing_policy.expr.CallExpr;
import org.batfish.datamodel.routing_policy.expr.Conjunction;
import org.batfish.datamodel.routing_policy.expr.ConjunctionChain;
import org.batfish.datamodel.routing_policy.expr.DestinationNetwork;
import org.batfish.datamodel.routing_policy.expr.Disjunction;
import org.batfish.datamodel.routing_policy.expr.DisjunctionChain;
import org.batfish.datamodel.routing_policy.expr.LiteralOrigin;
import org.batfish.datamodel.routing_policy.expr.MatchLocalRouteSourcePrefixLength;
import org.batfish.datamodel.routing_policy.expr.MatchPrefixSet;
import org.batfish.datamodel.routing_policy.expr.MatchProtocol;
import org.batfish.datamodel.routing_policy.expr.NamedPrefixSet;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetDefaultPolicy;
import org.batfish.datamodel.routing_policy.statement.SetOrigin;
import org.batfish.datamodel.routing_policy.statement.SetOspfMetricType;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.datamodel.routing_policy.statement.Statements;
import org.batfish.datamodel.vendor_family.juniper.JuniperFamily;
import org.batfish.representation.juniper.BgpGroup.BgpGroupType;
import org.batfish.vendor.VendorConfiguration;

public final class JuniperConfiguration extends VendorConfiguration {

  public static final String ACL_NAME_COMBINED_OUTGOING = "~COMBINED_OUTGOING_FILTER~";

  public static final String ACL_NAME_EXISTING_CONNECTION = "~EXISTING_CONNECTION~";

  public static final String ACL_NAME_GLOBAL_POLICY = "~GLOBAL_SECURITY_POLICY~";

  public static final String ACL_NAME_SECURITY_POLICY = "~SECURITY_POLICIES_TO~";

  private static final IpAccessList ACL_EXISTING_CONNECTION =
      IpAccessList.builder()
          .setName(ACL_NAME_EXISTING_CONNECTION)
          .setLines(
              ImmutableList.of(
                  new IpAccessListLine(
                      LineAction.ACCEPT,
                      new MatchHeaderSpace(
                          HeaderSpace.builder()
                              .setStates(ImmutableList.of(State.ESTABLISHED))
                              .build()),
                      ACL_NAME_EXISTING_CONNECTION)))
          .build();

  private static final int DEFAULT_AGGREGATE_ROUTE_COST = 0;

  private static final int DEFAULT_AGGREGATE_ROUTE_PREFERENCE = 130;

  private static final BgpAuthenticationAlgorithm DEFAULT_BGP_AUTHENTICATION_ALGORITHM =
      BgpAuthenticationAlgorithm.HMAC_SHA_1_96;

  private static final String DEFAULT_BGP_EXPORT_POLICY_NAME = "~DEFAULT_BGP_EXPORT_POLICY~";

  private static final String DEFAULT_BGP_IMPORT_POLICY_NAME = "~DEFAULT_BGP_IMPORT_POLICY~";

  private static final String FIRST_LOOPBACK_INTERFACE_NAME = "lo0";

  /** */
  private static final long serialVersionUID = 1L;

  private static String communityRegexToJavaRegex(String regex) {
    String out = regex;
    out = out.replace(":*", ":.*");
    out = out.replaceFirst("^\\*", ".*");
    return out;
  }

  private final Set<Long> _allStandardCommunities;

  private final Map<String, BaseApplication> _applications;

  private final Map<String, ApplicationSet> _applicationSets;

  private final NavigableMap<String, JuniperAuthenticationKeyChain> _authenticationKeyChains;

  Configuration _c;

  private final Map<String, CommunityList> _communityLists;

  private boolean _defaultAddressSelection;

  private LineAction _defaultCrossZoneAction;

  private LineAction _defaultInboundAction;

  private final RoutingInstance _defaultRoutingInstance;

  private NavigableSet<String> _dnsServers;

  private final Map<String, FirewallFilter> _filters;

  private final Map<String, AddressBook> _globalAddressBooks;

  private final Set<String> _ignoredPrefixLists;

  private final Map<String, IkeGateway> _ikeGateways;

  private final Map<String, IkePolicy> _ikePolicies;

  private final Map<String, IkeProposal> _ikeProposals;

  private final Map<String, Interface> _interfaces;

  private final Map<Interface, Zone> _interfaceZones;

  private final Map<String, IpsecPolicy> _ipsecPolicies;

  private final Map<String, IpsecProposal> _ipsecProposals;

  private final Map<String, IpsecVpn> _ipsecVpns;

  private final JuniperFamily _jf;

  private transient Interface _lo0;

  private transient boolean _lo0Initialized;

  private final Map<String, NodeDevice> _nodeDevices;

  private NavigableSet<String> _ntpServers;

  private final Map<String, PolicyStatement> _policyStatements;

  private final Map<String, PrefixList> _prefixLists;

  private final Map<String, RouteFilter> _routeFilters;

  private final Map<String, RoutingInstance> _routingInstances;

  private NavigableSet<String> _syslogHosts;

  private NavigableSet<String> _tacplusServers;

  private transient Set<String> _unimplementedFeatures;

  private ConfigurationFormat _vendor;

  private final Map<String, Vlan> _vlanNameToVlan;

  private final Map<String, Zone> _zones;

  public JuniperConfiguration(Set<String> unimplementedFeatures) {
    _allStandardCommunities = new HashSet<>();
    _applications = new TreeMap<>();
    _applicationSets = new TreeMap<>();
    _authenticationKeyChains = new TreeMap<>();
    _communityLists = new TreeMap<>();
    _defaultCrossZoneAction = LineAction.ACCEPT;
    _defaultRoutingInstance = new RoutingInstance(Configuration.DEFAULT_VRF_NAME);
    _dnsServers = new TreeSet<>();
    _filters = new TreeMap<>();
    _globalAddressBooks = new TreeMap<>();
    _ignoredPrefixLists = new HashSet<>();
    _ikeGateways = new TreeMap<>();
    _ikePolicies = new TreeMap<>();
    _ikeProposals = new TreeMap<>();
    _interfaces = new TreeMap<>();
    _interfaceZones = new TreeMap<>();
    _ipsecPolicies = new TreeMap<>();
    _ipsecProposals = new TreeMap<>();
    _ipsecVpns = new TreeMap<>();
    _jf = new JuniperFamily();
    _nodeDevices = new TreeMap<>();
    _ntpServers = new TreeSet<>();
    _prefixLists = new TreeMap<>();
    _policyStatements = new TreeMap<>();
    _routeFilters = new TreeMap<>();
    _routingInstances = new TreeMap<>();
    _routingInstances.put(Configuration.DEFAULT_VRF_NAME, _defaultRoutingInstance);
    _syslogHosts = new TreeSet<>();
    _tacplusServers = new TreeSet<>();
    _unimplementedFeatures = unimplementedFeatures;
    _vlanNameToVlan = new TreeMap<>();
    _zones = new TreeMap<>();
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

  private BgpProcess createBgpProcess(RoutingInstance routingInstance) {
    initDefaultBgpExportPolicy();
    initDefaultBgpImportPolicy();
    String vrfName = routingInstance.getName();
    Vrf vrf = _c.getVrfs().get(vrfName);
    BgpProcess proc = new BgpProcess();
    Ip routerId = routingInstance.getRouterId();
    if (routerId == null) {
      routerId = _defaultRoutingInstance.getRouterId();
      if (routerId == null) {
        routerId = Ip.ZERO;
      }
    }
    proc.setRouterId(routerId);
    BgpGroup mg = routingInstance.getMasterBgpGroup();
    boolean multipathEbgp = false;
    boolean multipathIbgp = false;
    boolean multipathMultipleAs = false;
    boolean multipathEbgpSet = false;
    boolean multipathIbgpSet = false;
    boolean multipathMultipleAsSet = false;

    if (mg.getLocalAs() == null) {
      Integer routingInstanceAs = routingInstance.getAs();
      if (routingInstanceAs == null) {
        routingInstanceAs = _defaultRoutingInstance.getAs();
      }
      if (routingInstanceAs == null) {
        _w.redFlag("BGP BROKEN FOR THIS ROUTER: Cannot determine local autonomous system");
      } else {
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
    for (Entry<Prefix, IpBgpGroup> e : routingInstance.getIpBgpGroups().entrySet()) {
      Prefix prefix = e.getKey();
      IpBgpGroup ig = e.getValue();
      BgpPeerConfig.Builder<?, ?> neighbor;
      Long remoteAs = ig.getType() == BgpGroupType.INTERNAL ? ig.getLocalAs() : ig.getPeerAs();
      if (ig.getDynamic()) {
        neighbor =
            BgpPassivePeerConfig.builder()
                .setPeerPrefix(prefix)
                .setRemoteAs(ImmutableList.of(remoteAs));
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
      Integer loops = ig.getLoops();
      boolean allowLocalAsIn = loops != null && loops > 0;
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
                PolicyStatement importPolicy = _policyStatements.get(importPolicyName);
                if (importPolicy != null) {
                  setPolicyStatementReferent(importPolicyName);
                  CallExpr callPolicy = new CallExpr(importPolicyName);
                  importPolicyCalls.add(callPolicy);
                }
              });
      If peerImportPolicyConditional = new If();
      DisjunctionChain importPolicyChain = new DisjunctionChain(importPolicyCalls);
      peerImportPolicyConditional.setGuard(importPolicyChain);
      peerImportPolicy.getStatements().add(peerImportPolicyConditional);
      peerImportPolicyConditional
          .getTrueStatements()
          .add(Statements.ExitAccept.toStaticStatement());
      peerImportPolicyConditional
          .getFalseStatements()
          .add(Statements.ExitReject.toStaticStatement());

      // export policies
      String peerExportPolicyName = computePeerExportPolicyName(ig.getRemoteAddress());
      neighbor.setExportPolicy(peerExportPolicyName);
      RoutingPolicy peerExportPolicy = new RoutingPolicy(peerExportPolicyName, _c);
      _c.getRoutingPolicies().put(peerExportPolicyName, peerExportPolicy);
      peerExportPolicy.getStatements().add(new SetDefaultPolicy(DEFAULT_BGP_EXPORT_POLICY_NAME));
      applyLocalRoutePolicy(routingInstance, peerExportPolicy);

      /*
       * For new BGP advertisements, i.e. those that are created from non-BGP
       * routes, an origin code must be set. By default, Juniper sets the origin
       * code to IGP.
       */
      If setOriginForNonBgp = new If();
      Disjunction isBgp = new Disjunction();
      isBgp.getDisjuncts().add(new MatchProtocol(RoutingProtocol.BGP));
      isBgp.getDisjuncts().add(new MatchProtocol(RoutingProtocol.IBGP));
      setOriginForNonBgp.setGuard(isBgp);
      setOriginForNonBgp
          .getFalseStatements()
          .add(new SetOrigin(new LiteralOrigin(OriginType.IGP, null)));
      peerExportPolicy.getStatements().add(setOriginForNonBgp);
      List<BooleanExpr> exportPolicyCalls = new ArrayList<>();
      ig.getExportPolicies()
          .forEach(
              exportPolicyName -> {
                PolicyStatement exportPolicy = _policyStatements.get(exportPolicyName);
                if (exportPolicy != null) {
                  setPolicyStatementReferent(exportPolicyName);
                  CallExpr callPolicy = new CallExpr(exportPolicyName);
                  exportPolicyCalls.add(callPolicy);
                }
              });
      If peerExportPolicyConditional = new If();
      DisjunctionChain exportPolicyChain = new DisjunctionChain(exportPolicyCalls);
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

      /*
       * inherit peer-as, or use local-as if internal
       *
       * Also set multipath
       */
      if (ig.getType() == BgpGroupType.INTERNAL) {
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
      } else {
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
      if (localIp == null && _defaultAddressSelection) {
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
    IsisSettings settings = _defaultRoutingInstance.getIsisSettings();
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
    processIsisInterfaceSettings(routingInstance, settings, level1, level2);
    newProc.setOverloadTimeout(settings.getOverloadTimeout());
    newProc.setReferenceBandwidth(settings.getReferenceBandwidth());
    return newProc.build();
  }

  private void processIsisInterfaceSettings(
      RoutingInstance routingInstance, IsisSettings settings, boolean level1, boolean level2) {
    _c.getVrfs()
        .get(routingInstance.getName())
        .getInterfaces()
        .forEach(
            (ifaceName, newIface) -> {
              Interface iface = routingInstance.getInterfaces().get(ifaceName);
              newIface.setIsis(
                  toIsisInterfaceSettings(
                      settings, iface.getIsisSettings(), iface.getIsoAddress(), level1, level2));
            });
  }

  private org.batfish.datamodel.isis.IsisInterfaceSettings toIsisInterfaceSettings(
      IsisSettings settings,
      @Nonnull IsisInterfaceSettings interfaceSettings,
      IsoAddress isoAddress,
      boolean level1,
      boolean level2) {
    if (!interfaceSettings.getEnabled()) {
      return null;
    }
    org.batfish.datamodel.isis.IsisInterfaceSettings.Builder newInterfaceSettingsBuilder =
        org.batfish.datamodel.isis.IsisInterfaceSettings.builder();
    if (level1) {
      newInterfaceSettingsBuilder.setLevel1(
          toIsisInterfaceLevelSettings(interfaceSettings, interfaceSettings.getLevel1Settings()));
    }
    if (level2) {
      newInterfaceSettingsBuilder.setLevel2(
          toIsisInterfaceLevelSettings(interfaceSettings, interfaceSettings.getLevel2Settings()));
    }
    return newInterfaceSettingsBuilder
        .setBfdLivenessDetectionMinimumInterval(
            interfaceSettings.getBfdLivenessDetectionMinimumInterval())
        .setBfdLivenessDetectionMultiplier(interfaceSettings.getBfdLivenessDetectionMultiplier())
        .setIsoAddress(isoAddress)
        .setPointToPoint(interfaceSettings.getPointToPoint())
        .build();
  }

  private org.batfish.datamodel.isis.IsisInterfaceLevelSettings toIsisInterfaceLevelSettings(
      IsisInterfaceSettings interfaceSettings, IsisInterfaceLevelSettings settings) {
    return org.batfish.datamodel.isis.IsisInterfaceLevelSettings.builder()
        .setCost(settings.getMetric())
        .setHelloAuthenticationKey(settings.getHelloAuthenticationKey())
        .setHelloAuthenticationType(settings.getHelloAuthenticationType())
        .setHelloInterval(settings.getHelloInterval())
        .setHoldTime(settings.getHoldTime())
        .setMode(
            interfaceSettings.getPassive() ? IsisInterfaceMode.PASSIVE : IsisInterfaceMode.ACTIVE)
        .build();
  }

  private org.batfish.datamodel.isis.IsisLevelSettings toIsisLevelSettings(
      IsisLevelSettings levelSettings) {
    return org.batfish.datamodel.isis.IsisLevelSettings.builder()
        .setWideMetricsOnly(levelSettings.getWideMetricsOnly())
        .build();
  }

  private OspfProcess createOspfProcess(RoutingInstance routingInstance) {
    OspfProcess newProc = new OspfProcess();
    String vrfName = routingInstance.getName();
    // export policies
    String ospfExportPolicyName = computeOspfExportPolicyName(vrfName);
    RoutingPolicy ospfExportPolicy = new RoutingPolicy(ospfExportPolicyName, _c);
    applyLocalRoutePolicy(routingInstance, ospfExportPolicy);
    _c.getRoutingPolicies().put(ospfExportPolicyName, ospfExportPolicy);
    newProc.setExportPolicy(ospfExportPolicyName);
    If ospfExportPolicyConditional = new If();
    // TODO: set default metric-type for special cases based on ospf process
    // setttings
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
              PolicyStatement exportPolicy = _policyStatements.get(exportPolicyName);
              if (exportPolicy != null) {
                setPolicyStatementReferent(exportPolicyName);
                CallExpr callPolicy = new CallExpr(exportPolicyName);
                matchSomeExportPolicy.getDisjuncts().add(callPolicy);
              }
            });
    // areas
    Map<Long, org.batfish.datamodel.ospf.OspfArea> newAreas = newProc.getAreas();
    newAreas.putAll(
        CommonUtil.toImmutableMap(
            routingInstance.getOspfAreas(), Entry::getKey, e -> toOspfArea(e.getValue())));
    // place interfaces into areas
    for (Entry<String, Interface> e : routingInstance.getInterfaces().entrySet()) {
      String name = e.getKey();
      Interface iface = e.getValue();
      placeInterfaceIntoArea(newAreas, name, iface, vrfName);
    }
    newProc.setRouterId(getOspfRouterId(routingInstance));
    newProc.setReferenceBandwidth(routingInstance.getOspfReferenceBandwidth());
    return newProc;
  }

  private org.batfish.datamodel.ospf.OspfArea toOspfArea(OspfArea area) {
    org.batfish.datamodel.ospf.OspfArea newArea =
        new org.batfish.datamodel.ospf.OspfArea(area.getName());
    newArea.setNssa(toNssaSettings(area.getNssaSettings()));
    newArea.setStub(toStubSettings(area.getStubSettings()));
    newArea.setStubType(area.getStubType());
    newArea.setSummaries(area.getSummaries());
    return newArea;
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

  public Set<Long> getAllStandardCommunities() {
    return _allStandardCommunities;
  }

  public Map<String, BaseApplication> getApplications() {
    return _applications;
  }

  public Map<String, JuniperAuthenticationKeyChain> getAuthenticationKeyChains() {
    return _authenticationKeyChains;
  }

  public Map<String, CommunityList> getCommunityLists() {
    return _communityLists;
  }

  public LineAction getDefaultCrossZoneAction() {
    return _defaultCrossZoneAction;
  }

  public RoutingInstance getDefaultRoutingInstance() {
    return _defaultRoutingInstance;
  }

  public NavigableSet<String> getDnsServers() {
    return _dnsServers;
  }

  public Map<String, FirewallFilter> getFirewallFilters() {
    return _filters;
  }

  public Map<String, AddressBook> getGlobalAddressBooks() {
    return _globalAddressBooks;
  }

  public Interface getGlobalMasterInterface() {
    return _defaultRoutingInstance.getGlobalMasterInterface();
  }

  @Override
  public String getHostname() {
    return _defaultRoutingInstance.getHostname();
  }

  public Set<String> getIgnoredPrefixLists() {
    return _ignoredPrefixLists;
  }

  public Map<String, IkeGateway> getIkeGateways() {
    return _ikeGateways;
  }

  public Map<String, IkePolicy> getIkePolicies() {
    return _ikePolicies;
  }

  public Map<String, IkeProposal> getIkeProposals() {
    return _ikeProposals;
  }

  public Map<String, Interface> getInterfaces() {
    return _interfaces;
  }

  public Map<Interface, Zone> getInterfaceZones() {
    return _interfaceZones;
  }

  public Map<String, IpsecPolicy> getIpsecPolicies() {
    return _ipsecPolicies;
  }

  public Map<String, IpsecProposal> getIpsecProposals() {
    return _ipsecProposals;
  }

  public Map<String, IpsecVpn> getIpsecVpns() {
    return _ipsecVpns;
  }

  public JuniperFamily getJf() {
    return _jf;
  }

  public Map<String, NodeDevice> getNodeDevices() {
    return _nodeDevices;
  }

  public NavigableSet<String> getNtpServers() {
    return _ntpServers;
  }

  public Map<String, PolicyStatement> getPolicyStatements() {
    return _policyStatements;
  }

  public Map<String, PrefixList> getPrefixLists() {
    return _prefixLists;
  }

  public Map<String, RouteFilter> getRouteFilters() {
    return _routeFilters;
  }

  public Map<String, RoutingInstance> getRoutingInstances() {
    return _routingInstances;
  }

  public NavigableSet<String> getSyslogHosts() {
    return _syslogHosts;
  }

  public NavigableSet<String> getTacplusServers() {
    return _tacplusServers;
  }

  @Override
  public Set<String> getUnimplementedFeatures() {
    return _unimplementedFeatures;
  }

  public Map<String, Vlan> getVlanNameToVlan() {
    return _vlanNameToVlan;
  }

  public Map<String, Zone> getZones() {
    return _zones;
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

  private void initFirstLoopbackInterface() {
    if (_lo0Initialized) {
      return;
    }
    _lo0Initialized = true;
    _lo0 = _defaultRoutingInstance.getInterfaces().get(FIRST_LOOPBACK_INTERFACE_NAME);
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
      for (Interface iface : _defaultRoutingInstance.getInterfaces().values()) {
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
      Map<Long, org.batfish.datamodel.ospf.OspfArea> newAreas,
      String name,
      Interface iface,
      String vrfName) {
    Vrf vrf = _c.getVrfs().get(vrfName);
    org.batfish.datamodel.Interface newIface = vrf.getInterfaces().get(name);
    Ip ospfArea = iface.getOspfArea();
    if (ospfArea == null) {
      return;
    }
    if (newIface.getAddress() == null) {
      _w.redFlag(
          String.format(
              "Cannot assign interface %s to area %s because it has no IP address.",
              name, ospfArea));
      return;
    }
    long ospfAreaLong = ospfArea.asLong();
    org.batfish.datamodel.ospf.OspfArea newArea = newAreas.get(ospfAreaLong);
    newArea.getInterfaces().add(name);
    newIface.setOspfArea(newArea);
    newIface.setOspfEnabled(true);
    newIface.setOspfPassive(iface.getOspfPassive());
    Integer ospfCost = iface.getOspfCost();
    if (ospfCost == null && newIface.isLoopback(ConfigurationFormat.FLAT_JUNIPER)) {
      ospfCost = 0;
    }
    newIface.setOspfCost(ospfCost);
  }

  public void setDefaultAddressSelection(boolean defaultAddressSelection) {
    _defaultAddressSelection = defaultAddressSelection;
  }

  public void setDefaultCrossZoneAction(LineAction defaultCrossZoneAction) {
    _defaultCrossZoneAction = defaultCrossZoneAction;
  }

  public void setDefaultInboundAction(LineAction defaultInboundAction) {
    _defaultInboundAction = defaultInboundAction;
  }

  @Override
  public void setHostname(String hostname) {
    _defaultRoutingInstance.setHostname(hostname);
  }

  private void setPolicyStatementReferent(String policyName) {
    PolicyStatement policy = _policyStatements.get(policyName);
    if (policy == null) {
      return;
    }
    List<PsTerm> terms = new ArrayList<>();
    terms.add(policy.getDefaultTerm());
    terms.addAll(policy.getTerms().values());
    for (PsTerm term : terms) {
      for (PsFrom from : term.getFroms()) {
        if (from instanceof PsFromPolicyStatement) {
          PsFromPolicyStatement fromPolicyStatement = (PsFromPolicyStatement) from;
          String subPolicyName = fromPolicyStatement.getPolicyStatement();
          setPolicyStatementReferent(subPolicyName);
        } else if (from instanceof PsFromPolicyStatementConjunction) {
          PsFromPolicyStatementConjunction fromPolicyStatementConjunction =
              (PsFromPolicyStatementConjunction) from;
          for (String subPolicyName : fromPolicyStatementConjunction.getConjuncts()) {
            setPolicyStatementReferent(subPolicyName);
          }
        }
      }
    }
  }

  public void setSyslogHosts(NavigableSet<String> syslogHosts) {
    _syslogHosts = syslogHosts;
  }

  @Override
  public void setVendor(ConfigurationFormat format) {
    _vendor = format;
  }

  private org.batfish.datamodel.GeneratedRoute toAggregateRoute(AggregateRoute route) {
    Prefix prefix = route.getPrefix();
    int prefixLength = prefix.getPrefixLength();
    int administrativeCost = route.getMetric();
    String policyNameSuffix = route.getPrefix().toString().replace('/', '_').replace('.', '_');
    String policyName = "~AGGREGATE_" + policyNameSuffix + "~";
    RoutingPolicy routingPolicy = new RoutingPolicy(policyName, _c);
    If routingPolicyConditional = new If();
    routingPolicy.getStatements().add(routingPolicyConditional);
    routingPolicyConditional.getTrueStatements().add(Statements.ExitAccept.toStaticStatement());
    routingPolicyConditional.getFalseStatements().add(Statements.ExitReject.toStaticStatement());
    String rflName = "~AGGREGATE_" + policyNameSuffix + "_RF~";
    MatchPrefixSet isContributingRoute =
        new MatchPrefixSet(new DestinationNetwork(), new NamedPrefixSet(rflName));
    routingPolicyConditional.setGuard(isContributingRoute);
    RouteFilterList rfList = new RouteFilterList(rflName);
    rfList.addLine(
        new org.batfish.datamodel.RouteFilterLine(
            LineAction.ACCEPT, prefix, new SubRange(prefixLength + 1, Prefix.MAX_PREFIX_LENGTH)));
    org.batfish.datamodel.GeneratedRoute.Builder newRoute =
        new org.batfish.datamodel.GeneratedRoute.Builder();
    newRoute.setNetwork(prefix);
    newRoute.setAdmin(administrativeCost);
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
      String regex = line.getRegex();
      String javaRegex = communityRegexToJavaRegex(regex);
      org.batfish.datamodel.CommunityListLine newLine =
          new org.batfish.datamodel.CommunityListLine(LineAction.ACCEPT, javaRegex);
      newLines.add(newLine);
    }
    org.batfish.datamodel.CommunityList newCl =
        new org.batfish.datamodel.CommunityList(name, newLines);
    newCl.setInvertMatch(cl.getInvertMatch());
    return newCl;
  }

  private org.batfish.datamodel.GeneratedRoute toGeneratedRoute(GeneratedRoute route) {
    Prefix prefix = route.getPrefix();
    Integer administrativeCost = route.getPreference();
    if (administrativeCost == null) {
      administrativeCost = DEFAULT_AGGREGATE_ROUTE_PREFERENCE;
    }
    Integer metric = route.getMetric();
    if (metric == null) {
      metric = DEFAULT_AGGREGATE_ROUTE_COST;
    }
    String generationPolicyName = null;
    if (!route.getPolicies().isEmpty()) {
      generationPolicyName = "~GENERATED_ROUTE_POLICY:" + prefix + "~";
      RoutingPolicy generationPolicy = new RoutingPolicy(generationPolicyName, _c);
      _c.getRoutingPolicies().put(generationPolicyName, generationPolicy);
      If generationPolicyConditional = new If();
      Disjunction matchSomeGenerationPolicy = new Disjunction();
      generationPolicyConditional.setGuard(matchSomeGenerationPolicy);
      generationPolicyConditional
          .getTrueStatements()
          .add(Statements.ExitAccept.toStaticStatement());
      generationPolicyConditional
          .getFalseStatements()
          .add(Statements.ExitReject.toStaticStatement());
      generationPolicy.getStatements().add(generationPolicyConditional);
      route
          .getPolicies()
          .forEach(
              policyName -> {
                PolicyStatement policy = _policyStatements.get(policyName);
                if (policy != null) {
                  setPolicyStatementReferent(policyName);
                  CallExpr callPolicy = new CallExpr(policyName);
                  matchSomeGenerationPolicy.getDisjuncts().add(callPolicy);
                }
              });
    }
    org.batfish.datamodel.GeneratedRoute.Builder newRoute =
        new org.batfish.datamodel.GeneratedRoute.Builder();
    newRoute.setNetwork(prefix);
    newRoute.setAdmin(administrativeCost);
    newRoute.setMetric(metric);
    newRoute.setGenerationPolicy(generationPolicyName);
    return newRoute.build();
  }

  private org.batfish.datamodel.IkeGateway toIkeGateway(IkeGateway oldIkeGateway) {
    String name = oldIkeGateway.getName();
    org.batfish.datamodel.IkeGateway newIkeGateway = new org.batfish.datamodel.IkeGateway(name);

    // address
    newIkeGateway.setAddress(oldIkeGateway.getAddress());
    newIkeGateway.setLocalIp(oldIkeGateway.getLocalAddress());

    // external interface
    Interface oldExternalInterface = oldIkeGateway.getExternalInterface();
    if (oldExternalInterface != null) {
      org.batfish.datamodel.Interface newExternalInterface =
          _c.getInterfaces().get(oldExternalInterface.getName());
      if (newExternalInterface != null) {
        newIkeGateway.setExternalInterface(newExternalInterface);
      }
    } else {
      _w.redFlag("No external interface set for ike gateway: '" + name + "'");
    }

    // ike policy
    String ikePolicyName = oldIkeGateway.getIkePolicy();
    org.batfish.datamodel.IkePolicy newIkePolicy = _c.getIkePolicies().get(ikePolicyName);
    if (newIkePolicy != null) {
      newIkeGateway.setIkePolicy(newIkePolicy);
    }

    return newIkeGateway;
  }

  private org.batfish.datamodel.IkePolicy toIkePolicy(IkePolicy oldIkePolicy) {
    String name = oldIkePolicy.getName();
    org.batfish.datamodel.IkePolicy newIkePolicy = new org.batfish.datamodel.IkePolicy(name);

    // pre-shared-key
    newIkePolicy.setPreSharedKeyHash(oldIkePolicy.getPreSharedKeyHash());

    // ike proposals
    oldIkePolicy
        .getProposals()
        .forEach(
            ikeProposalName -> {
              org.batfish.datamodel.IkeProposal ikeProposal =
                  _c.getIkeProposals().get(ikeProposalName);
              if (ikeProposal != null) {
                newIkePolicy.getProposals().put(ikeProposalName, ikeProposal);
              }
            });

    return newIkePolicy;
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

  private org.batfish.datamodel.IkeProposal toIkeProposal(IkeProposal ikeProposal) {
    org.batfish.datamodel.IkeProposal newIkeProposal =
        new org.batfish.datamodel.IkeProposal(ikeProposal.getName());
    newIkeProposal.setDiffieHellmanGroup(ikeProposal.getDiffieHellmanGroup());
    newIkeProposal.setAuthenticationMethod(ikeProposal.getAuthenticationMethod());
    newIkeProposal.setEncryptionAlgorithm(ikeProposal.getEncryptionAlgorithm());
    newIkeProposal.setLifetimeSeconds(ikeProposal.getLifetimeSeconds());
    newIkeProposal.setAuthenticationAlgorithm(ikeProposal.getAuthenticationAlgorithm());
    return newIkeProposal;
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
    newIface.setAdditionalArpIps(iface.getAdditionalArpIps());
    Zone zone = _interfaceZones.get(iface);
    if (zone != null) {
      // filter for interface in zone
      FirewallFilter zoneInboundInterfaceFilter = zone.getInboundInterfaceFilters().get(iface);
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
    }
    String inAclName = iface.getIncomingFilter();
    if (inAclName != null) {
      IpAccessList inAcl = _c.getIpAccessLists().get(inAclName);
      if (inAcl != null) {
        FirewallFilter inFilter = _filters.get(inAclName);
        newIface.setIncomingFilter(inAcl);
        if (inFilter.getRoutingPolicy()) {
          RoutingPolicy routingPolicy = _c.getRoutingPolicies().get(inAclName);
          if (routingPolicy != null) {
            newIface.setRoutingPolicy(inAclName);
          } else {
            throw new BatfishException("Expected interface routing-policy to exist");
          }
        }
      }
    }

    // Assume the config will need security policies only if it has zones
    IpAccessList securityPolicyAcl = null;
    if (!_zones.isEmpty()) {
      String securityPolicyAclName = ACL_NAME_SECURITY_POLICY + iface.getName();
      securityPolicyAcl = buildSecurityPolicyAcl(securityPolicyAclName, zone);
      if (securityPolicyAcl != null) {
        _c.getIpAccessLists().put(securityPolicyAclName, securityPolicyAcl);
      }
    }
    newIface.setOutgoingFilter(buildOutgoingFilter(iface, securityPolicyAcl));

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
      Vlan vlan = getVlanNameToVlan().get(iface.getAccessVlan());
      if (vlan != null) {
        newIface.setAccessVlan(vlan.getVlanId());
      }
    }
    newIface.setAllowedVlans(iface.getAllowedVlans());
    newIface.setNativeVlan(iface.getNativeVlan());
    newIface.setSwitchportMode(iface.getSwitchportMode());
    SwitchportEncapsulationType swe = iface.getSwitchportTrunkEncapsulation();
    if (swe == null) {
      swe = SwitchportEncapsulationType.DOT1Q;
    }
    newIface.setSwitchportTrunkEncapsulation(swe);
    newIface.setBandwidth(iface.getBandwidth());
    newIface.setOspfPointToPoint(iface.getOspfPointToPoint());
    return newIface;
  }

  /** Generate IpAccessList from the specified to-zone's security policies. */
  IpAccessList buildSecurityPolicyAcl(String name, Zone zone) {
    List<IpAccessListLine> zoneAclLines = new TreeList<>();

    /* Default ACL that allows existing connections should be added to all security policies */
    zoneAclLines.add(
        new IpAccessListLine(
            LineAction.ACCEPT,
            new PermittedByAcl(ACL_NAME_EXISTING_CONNECTION, false),
            "EXISTING_CONNECTION"));

    /* Default policy allows traffic originating from the device to be accepted */
    zoneAclLines.add(
        new IpAccessListLine(LineAction.ACCEPT, OriginatingFromDevice.INSTANCE, "HOST_OUTBOUND"));

    /* Zone specific policies */
    if (zone != null && !zone.getFromZonePolicies().isEmpty()) {
      for (Entry<String, FirewallFilter> e : zone.getFromZonePolicies().entrySet()) {
        /* Handle explicit accept lines from this policy */
        zoneAclLines.add(
            new IpAccessListLine(
                LineAction.ACCEPT, new PermittedByAcl(e.getKey(), false), e.getKey() + "ACCEPT"));
        /* Handle explicit deny lines from this policy, this is needed so only unmatched lines fall-through to the next lines */
        zoneAclLines.add(
            new IpAccessListLine(
                LineAction.REJECT,
                new NotMatchExpr(new PermittedByAcl(e.getKey(), true)),
                e.getKey() + "REJECT"));
      }
    }

    /* Global policy if applicable */
    if (_filters.get(ACL_NAME_GLOBAL_POLICY) != null) {
      /* Handle explicit accept lines for global policy */
      zoneAclLines.add(
          new IpAccessListLine(
              LineAction.ACCEPT,
              new PermittedByAcl(ACL_NAME_GLOBAL_POLICY, false),
              "GLOBAL_POLICY_ACCEPT"));
      /* Handle explicit deny lines for global policy, this is needed so only unmatched lines fall-through to the next lines */
      zoneAclLines.add(
          new IpAccessListLine(
              LineAction.REJECT,
              new NotMatchExpr(new PermittedByAcl(ACL_NAME_GLOBAL_POLICY, true)),
              "GLOBAL_POLICY_REJECT"));
    }

    /* Add catch-all line with default action */
    zoneAclLines.add(
        new IpAccessListLine(_defaultCrossZoneAction, TrueExpr.INSTANCE, "DEFAULT_POLICY"));

    IpAccessList zoneAcl = IpAccessList.builder().setName(name).setLines(zoneAclLines).build();
    _c.getIpAccessLists().put(name, zoneAcl);
    return zoneAcl;
  }

  /** Generate outgoing filter for the interface (from existing outgoing filter and zone policy) */
  IpAccessList buildOutgoingFilter(Interface iface, @Nullable IpAccessList securityPolicyAcl) {
    String outAclName = iface.getOutgoingFilter();
    IpAccessList outAcl = null;
    if (outAclName != null) {
      outAcl = _c.getIpAccessLists().get(outAclName);
    }

    // Set outgoing filter based on the combination of zone policy and base outgoing filter
    Set<AclLineMatchExpr> aclConjunctList;
    if (securityPolicyAcl == null) {
      return outAcl;
    } else if (outAcl == null) {
      aclConjunctList = ImmutableSet.of(new PermittedByAcl(securityPolicyAcl.getName(), false));
    } else {
      aclConjunctList =
          ImmutableSet.of(
              new PermittedByAcl(outAcl.getName(), false),
              new PermittedByAcl(securityPolicyAcl.getName(), false));
    }

    String combinedAclName = ACL_NAME_COMBINED_OUTGOING + iface.getName();
    IpAccessList combinedAcl =
        IpAccessList.builder()
            .setName(combinedAclName)
            .setLines(
                ImmutableList.of(
                    new IpAccessListLine(
                        LineAction.ACCEPT, new AndMatchExpr(aclConjunctList), "ACCEPT")))
            .build();
    _c.getIpAccessLists().put(combinedAclName, combinedAcl);
    return combinedAcl;
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
        action = LineAction.ACCEPT;
      } else if (term.getThens().contains(FwThenDiscard.INSTANCE)) {
        action = LineAction.REJECT;
      } else if (term.getThens().contains(FwThenNextTerm.INSTANCE)) {
        // TODO: throw error if any transformation is being done
        continue;
      } else if (term.getThens().contains(FwThenNop.INSTANCE)) {
        // we assume for now that any 'nop' operations imply acceptance
        action = LineAction.ACCEPT;
      } else {
        _w.redFlag(
            "missing action in firewall filter: '" + aclName + "', term: '" + term.getName() + "'");
        action = LineAction.REJECT;
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
      return lines
          .stream()
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
          new MatchSrcInterface(
              _zones
                  .get(zoneName)
                  .getInterfaces()
                  .stream()
                  .map(Interface::getName)
                  .collect(ImmutableList.toImmutableList()));
    }

    /* Return an ACL that is the logical AND of srcInterface filter and headerSpace filter */
    return fwTermsToIpAccessList(name, filter.getTerms().values(), matchSrcInterface);
  }

  private org.batfish.datamodel.IpsecPolicy toIpsecPolicy(IpsecPolicy oldIpsecPolicy) {
    String name = oldIpsecPolicy.getName();
    org.batfish.datamodel.IpsecPolicy newIpsecPolicy = new org.batfish.datamodel.IpsecPolicy(name);

    // ipsec proposals
    oldIpsecPolicy
        .getProposals()
        .forEach(
            ipsecProposalName -> {
              org.batfish.datamodel.IpsecProposal ipsecProposal =
                  _c.getIpsecProposals().get(ipsecProposalName);
              if (ipsecProposal != null) {
                newIpsecPolicy.getProposals().add(ipsecProposal);
              }
            });

    // perfect-forward-secrecy diffie-hellman key group
    newIpsecPolicy.setPfsKeyGroup(oldIpsecPolicy.getPfsKeyGroup());

    return newIpsecPolicy;
  }

  @Nullable
  private IpsecPeerConfig toIpsecPeerConfig(IpsecVpn ipsecVpn) {
    IpsecStaticPeerConfig.Builder ipsecStaticConfigBuilder = IpsecStaticPeerConfig.builder();
    ipsecStaticConfigBuilder.setTunnelInterface(ipsecVpn.getBindInterface().getName());
    IkeGateway ikeGateway = _ikeGateways.get(ipsecVpn.getGateway());

    if (ikeGateway == null) {
      _w.redFlag(
          String.format(
              "Cannot find the IKE gateway %s for ipsec vpn %s",
              ipsecVpn.getGateway(), ipsecVpn.getName()));
      return null;
    }
    ipsecStaticConfigBuilder.setDestinationAddress(ikeGateway.getAddress());
    ipsecStaticConfigBuilder.setPhysicalInterface(ikeGateway.getExternalInterface().getName());

    if (ikeGateway.getLocalAddress() != null) {
      ipsecStaticConfigBuilder.setLocalAddress(ikeGateway.getLocalAddress());
    } else if (ikeGateway.getExternalInterface() != null
        && ikeGateway.getExternalInterface().getPrimaryAddress() != null) {
      ipsecStaticConfigBuilder.setLocalAddress(
          ikeGateway.getExternalInterface().getPrimaryAddress().getIp());
    } else {
      _w.redFlag(
          String.format(
              "External interface %s configured on IKE Gateway %s does not have any IP",
              ikeGateway.getExternalInterface().getName(), ikeGateway.getName()));
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

  private static org.batfish.datamodel.IpsecProposal toIpsecProposal(
      IpsecProposal oldIpsecProposal) {
    org.batfish.datamodel.IpsecProposal newIpsecProposal =
        new org.batfish.datamodel.IpsecProposal(oldIpsecProposal.getName());
    newIpsecProposal.setAuthenticationAlgorithm(oldIpsecProposal.getAuthenticationAlgorithm());
    newIpsecProposal.setEncryptionAlgorithm(oldIpsecProposal.getEncryptionAlgorithm());
    newIpsecProposal.setProtocols(oldIpsecProposal.getProtocols());

    return newIpsecProposal;
  }

  private static IpsecPhase2Proposal toIpsecPhase2Proposal(IpsecProposal oldIpsecProposal) {
    IpsecPhase2Proposal ipsecPhase2Proposal = new IpsecPhase2Proposal();
    ipsecPhase2Proposal.setAuthenticationAlgorithm(oldIpsecProposal.getAuthenticationAlgorithm());
    ipsecPhase2Proposal.setEncryptionAlgorithm(oldIpsecProposal.getEncryptionAlgorithm());
    ipsecPhase2Proposal.setProtocols(oldIpsecProposal.getProtocols());
    ipsecPhase2Proposal.setIpsecEncapsulationMode(oldIpsecProposal.getIpsecEncapsulationMode());

    return ipsecPhase2Proposal;
  }

  private org.batfish.datamodel.IpsecVpn toIpsecVpn(IpsecVpn oldIpsecVpn) {
    String name = oldIpsecVpn.getName();
    org.batfish.datamodel.IpsecVpn newIpsecVpn = new org.batfish.datamodel.IpsecVpn(name, _c);

    // bind interface
    Interface oldBindInterface = oldIpsecVpn.getBindInterface();
    if (oldBindInterface != null) {
      String bindInterfaceName = oldBindInterface.getName();
      org.batfish.datamodel.Interface newBindInterface = _c.getInterfaces().get(bindInterfaceName);
      if (newBindInterface != null) {
        newIpsecVpn.setBindInterface(newBindInterface);
      }
    } else {
      _w.redFlag("No bind interface set for ipsec vpn: '" + name + "'");
    }

    // ike gateway
    String ikeGatewayName = oldIpsecVpn.getGateway();
    if (ikeGatewayName != null) {
      org.batfish.datamodel.IkeGateway ikeGateway = _c.getIkeGateways().get(ikeGatewayName);
      if (ikeGateway != null) {
        newIpsecVpn.setIkeGateway(ikeGateway);
      }
    } else {
      _w.redFlag("No ike gateway set for ipsec vpn: '" + name + "'");
    }

    // ipsec policy
    String ipsecPolicyName = oldIpsecVpn.getIpsecPolicy();
    if (ipsecPolicyName != null) {
      org.batfish.datamodel.IpsecPolicy ipsecPolicy = _c.getIpsecPolicies().get(ipsecPolicyName);
      if (ipsecPolicy != null) {
        newIpsecVpn.setIpsecPolicy(ipsecPolicy);
      }
    } else {
      _w.redFlag("No ipsec policy set for ipsec vpn: '" + name + "'");
    }

    return newIpsecVpn;
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
                ImmutableList.Builder<AclIpSpaceLine> aclIpSpaceLineBuilder =
                    ImmutableList.builder();
                entry
                    .getEntries()
                    .forEach(
                        subEntry -> {
                          String subEntryName = bookName + "~" + subEntry.getName();
                          aclIpSpaceLineBuilder.add(
                              AclIpSpaceLine.builder()
                                  .setIpSpace(new IpSpaceReference(subEntryName))
                                  .setAction(LineAction.ACCEPT)
                                  .build());
                        });
                ipSpaces.put(
                    entryName,
                    AclIpSpace.builder().setLines(aclIpSpaceLineBuilder.build()).build());
              } else {
                ipSpaces.put(
                    entryName,
                    IpWildcardSetIpSpace.builder().including(entry.getIpWildcards(_w)).build());
              }
            });
    return ipSpaces;
  }

  private RoutingPolicy toRoutingPolicy(FirewallFilter filter) {
    String name = filter.getName();
    RoutingPolicy routingPolicy = new RoutingPolicy(name, _c);
    // for (Entry<String, FwTerm> e : filter.getTerms().entrySet()) {
    // String termName = e.getKey();
    // FwTerm term = e.getValue();
    // PolicyMapClause clause = new PolicyMapClause();
    // clause.setName(termName);
    // routingPolicy.getClauses().add(clause);
    // Set<Prefix> destinationPrefixes = new TreeSet<>();
    // ;
    // List<SubRange> destinationPortRanges = new ArrayList<>();
    // Set<Prefix> sourcePrefixes = new TreeSet<>();
    // List<SubRange> sourcePortRanges = new ArrayList<>();
    //
    // for (FwFrom from : term.getFroms()) {
    // if (from instanceof FwFromDestinationAddress) {
    // FwFromDestinationAddress fromDestinationAddress =
    // (FwFromDestinationAddress) from;
    // Prefix destinationPrefix = fromDestinationAddress.getIp();
    // destinationPrefixes.add(destinationPrefix);
    // }
    // if (from instanceof FwFromDestinationPort) {
    // FwFromDestinationPort fromDestinationPort = (FwFromDestinationPort)
    // from;
    // SubRange destinationPortRange = fromDestinationPort
    // .getPortRange();
    // destinationPortRanges.add(destinationPortRange);
    // }
    // else if (from instanceof FwFromSourceAddress) {
    // FwFromSourceAddress fromSourceAddress = (FwFromSourceAddress) from;
    // Prefix sourcePrefix = fromSourceAddress.getIp();
    // sourcePrefixes.add(sourcePrefix);
    // }
    // if (from instanceof FwFromSourcePort) {
    // FwFromSourcePort fromSourcePort = (FwFromSourcePort) from;
    // SubRange sourcePortRange = fromSourcePort.getPortRange();
    // sourcePortRanges.add(sourcePortRange);
    // }
    // }
    // if (!destinationPrefixes.isEmpty() || !destinationPortRanges.isEmpty()
    // || !sourcePrefixes.isEmpty() || !sourcePortRanges.isEmpty()) {
    // String termIpAccessListName = "~" + name + ":" + termName + "~";
    // IpAccessListLine line = new IpAccessListLine();
    // for (Prefix dstPrefix : destinationPrefixes) {
    // IpWildcard dstWildcard = new IpWildcard(dstPrefix);
    // line.getDstIps().add(dstWildcard);
    // }
    // line.getDstPorts().addAll(destinationPortRanges);
    // for (Prefix srcPrefix : sourcePrefixes) {
    // IpWildcard srcWildcard = new IpWildcard(srcPrefix);
    // line.getSrcIps().add(srcWildcard);
    // }
    // line.getDstPorts().addAll(sourcePortRanges);
    // line.setAction(LineAction.ACCEPT);
    // IpAccessList termIpAccessList = new IpAccessList(
    // termIpAccessListName, Collections.singletonList(line));
    // _c.getIpAccessLists().put(termIpAccessListName, termIpAccessList);
    // PolicyMapMatchIpAccessListLine matchListLine = new
    // PolicyMapMatchIpAccessListLine(
    // Collections.singleton(termIpAccessList));
    // clause.getMatchLines().add(matchListLine);
    // }
    // List<Prefix> nextPrefixes = new ArrayList<>();
    // for (FwThen then : term.getThens()) {
    // if (then instanceof FwThenNextIp) {
    // FwThenNextIp thenNextIp = (FwThenNextIp) then;
    // Prefix nextIp = thenNextIp.getNextPrefix();
    // nextPrefixes.add(nextIp);
    // }
    // else if (then == FwThenDiscard.INSTANCE) {
    // clause.setAction(PolicyMapAction.DENY);
    // }
    // else if (then == FwThenAccept.INSTANCE) {
    // clause.setAction(PolicyMapAction.PERMIT);
    // }
    // }
    // if (!nextPrefixes.isEmpty()) {
    // List<Ip> nextHopIps = new ArrayList<>();
    // for (Prefix nextPrefix : nextPrefixes) {
    // nextHopIps.add(nextPrefix.getIp());
    // int prefixLength = nextPrefix.getPrefixLength();
    // if (prefixLength != 32) {
    // _w.redFlag(
    // "Not sure how to interpret nextIp with prefix-length not equal to 32: "
    // + prefixLength);
    // }
    // }
    // PolicyMapSetNextHopLine setNextHop = new PolicyMapSetNextHopLine(
    // nextHopIps);
    // clause.getSetLines().add(setNextHop);
    // }
    // }
    return routingPolicy;
  }

  private RoutingPolicy toRoutingPolicy(PolicyStatement ps) {
    String name = ps.getName();
    RoutingPolicy routingPolicy = new RoutingPolicy(name, _c);
    List<Statement> statements = routingPolicy.getStatements();
    boolean hasDefaultTerm =
        ps.getDefaultTerm().getFroms().size() > 0 || ps.getDefaultTerm().getThens().size() > 0;
    List<PsTerm> terms = new ArrayList<>(ps.getTerms().values());
    if (hasDefaultTerm) {
      terms.add(ps.getDefaultTerm());
    }
    for (PsTerm term : terms) {
      List<Statement> thens = toStatements(term.getThens());
      if (!term.getFroms().isEmpty()) {
        If ifStatement = new If();
        ifStatement.setComment(term.getName());
        Conjunction conj = new Conjunction();
        List<BooleanExpr> subroutines = new ArrayList<>();
        for (PsFrom from : term.getFroms()) {
          if (from instanceof PsFromRouteFilter) {
            int actionLineCounter = 0;
            PsFromRouteFilter fromRouteFilter = (PsFromRouteFilter) from;
            String routeFilterName = fromRouteFilter.getRouteFilterName();
            RouteFilter rf = _routeFilters.get(routeFilterName);
            for (RouteFilterLine line : rf.getLines()) {
              if (line.getThens().size() > 0) {
                String lineListName = name + "_ACTION_LINE_" + actionLineCounter;
                RouteFilterList lineSpecificList = new RouteFilterList(lineListName);
                line.applyTo(lineSpecificList);
                actionLineCounter++;
                _c.getRouteFilterLists().put(lineListName, lineSpecificList);
                If lineSpecificIfStatement = new If();
                String lineSpecificClauseName =
                    routeFilterName + "_ACTION_LINE_" + actionLineCounter;
                lineSpecificIfStatement.setComment(lineSpecificClauseName);
                MatchPrefixSet mrf =
                    new MatchPrefixSet(new DestinationNetwork(), new NamedPrefixSet(lineListName));
                lineSpecificIfStatement.setGuard(mrf);
                lineSpecificIfStatement.getTrueStatements().addAll(toStatements(line.getThens()));
                statements.add(lineSpecificIfStatement);
              }
            }
          }
          BooleanExpr booleanExpr = from.toBooleanExpr(this, _c, _w);
          if (from instanceof PsFromPolicyStatement
              || from instanceof PsFromPolicyStatementConjunction) {
            subroutines.add(booleanExpr);
          } else {
            conj.getConjuncts().add(booleanExpr);
          }
        }
        if (!subroutines.isEmpty()) {
          ConjunctionChain chain = new ConjunctionChain(subroutines);
          conj.getConjuncts().add(chain);
        }
        BooleanExpr guard = conj.simplify();
        ifStatement.setGuard(guard);
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
    Ip nextHopIp = route.getNextHopIp();
    if (nextHopIp == null) {
      nextHopIp = Route.UNSET_ROUTE_NEXT_HOP_IP;
    }
    String nextHopInterface =
        route.getDrop()
            ? org.batfish.datamodel.Interface.NULL_INTERFACE_NAME
            : route.getNextHopInterface();
    int tag = route.getTag() != null ? route.getTag() : -1;

    org.batfish.datamodel.StaticRoute newStaticRoute =
        org.batfish.datamodel.StaticRoute.builder()
            .setNetwork(route.getPrefix())
            .setNextHopIp(nextHopIp)
            .setNextHopInterface(nextHopInterface)
            .setAdministrativeCost(route.getDistance())
            .setMetric(route.getMetric())
            .setTag(tag)
            .build();

    newStaticRoute.setNonRouting(firstNonNull(route.getNoInstall(), Boolean.FALSE));
    return newStaticRoute;
  }

  @Override
  public Configuration toVendorIndependentConfiguration() throws VendorConversionException {
    String hostname = getHostname();
    _c = new Configuration(hostname, _vendor);
    _c.setAuthenticationKeyChains(convertAuthenticationKeyChains(_authenticationKeyChains));
    _c.setDnsServers(_dnsServers);
    _c.setDomainName(_defaultRoutingInstance.getDomainName());
    _c.setLoggingServers(_syslogHosts);
    _c.setNtpServers(_ntpServers);
    _c.setTacacsServers(_tacplusServers);
    _c.getVendorFamily().setJuniper(_jf);
    for (String riName : _routingInstances.keySet()) {
      _c.getVrfs().put(riName, new Vrf(riName));
    }

    // convert prefix lists to route filter lists
    for (Entry<String, PrefixList> e : _prefixLists.entrySet()) {
      String name = e.getKey();
      PrefixList pl = e.getValue();
      RouteFilterList rfl = new RouteFilterList(name);
      for (Prefix prefix : pl.getPrefixes()) {
        int prefixLength = prefix.getPrefixLength();
        org.batfish.datamodel.RouteFilterLine line =
            new org.batfish.datamodel.RouteFilterLine(
                LineAction.ACCEPT, prefix, new SubRange(prefixLength, prefixLength));
        rfl.addLine(line);
      }
      _c.getRouteFilterLists().put(name, rfl);
    }

    // Convert AddressBooks to IpSpaces
    _globalAddressBooks.forEach(
        (name, addressBook) -> _c.getIpSpaces().putAll(toIpSpaces(name, addressBook)));

    // TODO: instead make both IpAccessList and Ip6AccessList instances from
    // such firewall filters
    // remove ipv6 lines from firewall filters
    for (FirewallFilter filter : _filters.values()) {
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
    Map<String, FirewallFilter> allFilters = new LinkedHashMap<>(_filters);
    for (Entry<String, FirewallFilter> e : allFilters.entrySet()) {
      String name = e.getKey();
      FirewallFilter filter = e.getValue();
      if (filter.getTerms().size() == 0) {
        _filters.remove(name);
      }
    }

    // convert firewall filters to ipaccesslists
    for (Entry<String, FirewallFilter> e : _filters.entrySet()) {
      String name = e.getKey();
      FirewallFilter filter = e.getValue();
      // TODO: support other filter families
      if (filter.getFamily() != Family.INET) {
        continue;
      }
      IpAccessList list = toIpAccessList(filter);
      _c.getIpAccessLists().put(name, list);
    }

    // convert firewall filters implementing routing policy to RoutingPolicy
    // objects
    for (Entry<String, FirewallFilter> e : _filters.entrySet()) {
      String name = e.getKey();
      FirewallFilter filter = e.getValue();
      if (filter.getRoutingPolicy()) {
        // TODO: support other filter families
        if (filter.getFamily() != Family.INET) {
          continue;
        }
        RoutingPolicy routingPolicy = toRoutingPolicy(filter);
        _c.getRoutingPolicies().put(name, routingPolicy);
      }
    }

    // convert route filters to route filter lists
    for (Entry<String, RouteFilter> e : _routeFilters.entrySet()) {
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
    for (Entry<String, CommunityList> e : _communityLists.entrySet()) {
      String name = e.getKey();
      CommunityList cl = e.getValue();
      org.batfish.datamodel.CommunityList newCl = toCommunityList(cl);
      _c.getCommunityLists().put(name, newCl);
    }

    // convert policy-statements to RoutingPolicy objects
    for (Entry<String, PolicyStatement> e : _policyStatements.entrySet()) {
      String name = e.getKey();
      PolicyStatement ps = e.getValue();
      RoutingPolicy routingPolicy = toRoutingPolicy(ps);
      _c.getRoutingPolicies().put(name, routingPolicy);
    }

    // convert interfaces
    Map<String, Interface> allInterfaces = new LinkedHashMap<>();

    for (Interface iface : _interfaces.values()) {
      allInterfaces.putAll(iface.getUnits());
    }
    for (NodeDevice nd : _nodeDevices.values()) {
      for (Interface iface : nd.getInterfaces().values()) {
        allInterfaces.putAll(iface.getUnits());
      }
    }
    for (Entry<String, Interface> eUnit : allInterfaces.entrySet()) {
      String unitName = eUnit.getKey();
      Interface unitIface = eUnit.getValue();
      unitIface.inheritUnsetFields();
      org.batfish.datamodel.Interface newUnitIface = toInterface(unitIface);
      _c.getInterfaces().put(unitName, newUnitIface);
      Vrf vrf = newUnitIface.getVrf();
      String vrfName = vrf.getName();
      vrf.getInterfaces().put(unitName, newUnitIface);
      _routingInstances.get(vrfName).getInterfaces().put(unitName, unitIface);
    }

    // set router-id
    if (_defaultRoutingInstance.getRouterId() == null) {
      Interface loopback0 =
          _defaultRoutingInstance.getInterfaces().get(FIRST_LOOPBACK_INTERFACE_NAME);
      if (loopback0 != null) {
        Interface loopback0unit0 = loopback0.getUnits().get(FIRST_LOOPBACK_INTERFACE_NAME + ".0");
        if (loopback0unit0 != null) {
          InterfaceAddress address = loopback0unit0.getPrimaryAddress();
          if (address != null) {
            // now we should set router-id
            Ip routerId = address.getIp();
            _defaultRoutingInstance.setRouterId(routerId);
          }
        }
      }
    }

    // convert IKE proposals
    _ikeProposals
        .values()
        .forEach(
            ikeProposal ->
                _c.getIkeProposals().put(ikeProposal.getName(), toIkeProposal(ikeProposal)));

    _ikeProposals
        .values()
        .forEach(
            ikeProposal ->
                _c.getIkePhase1Proposals()
                    .put(ikeProposal.getName(), toIkePhase1Proposal(ikeProposal)));

    ImmutableSortedMap.Builder<String, IkePhase1Key> ikePhase1KeysBuilder =
        ImmutableSortedMap.naturalOrder();

    // convert ike policies
    for (Entry<String, IkePolicy> e : _ikePolicies.entrySet()) {
      String name = e.getKey();
      IkePolicy oldIkePolicy = e.getValue();
      org.batfish.datamodel.IkePolicy newPolicy = toIkePolicy(oldIkePolicy);
      _c.getIkePolicies().put(name, newPolicy);
      // storing IKE phase 1 policy
      _c.getIkePhase1Policies().put(name, toIkePhase1Policy(oldIkePolicy, ikePhase1KeysBuilder));
    }

    _c.setIkePhase1Keys(ikePhase1KeysBuilder.build());

    // convert ike gateways
    for (Entry<String, IkeGateway> e : _ikeGateways.entrySet()) {
      String name = e.getKey();
      IkeGateway oldIkeGateway = e.getValue();
      org.batfish.datamodel.IkeGateway newIkeGateway = toIkeGateway(oldIkeGateway);
      _c.getIkeGateways().put(name, newIkeGateway);
    }

    // convert ipsec proposals
    ImmutableSortedMap.Builder<String, IpsecPhase2Proposal> ipsecPhase2ProposalsBuilder =
        ImmutableSortedMap.naturalOrder();
    _ipsecProposals.forEach(
        (ipsecProposalName, ipsecProposal) -> {
          _c.getIpsecProposals().put(ipsecProposalName, toIpsecProposal(ipsecProposal));
          ipsecPhase2ProposalsBuilder.put(ipsecProposalName, toIpsecPhase2Proposal(ipsecProposal));
        });
    _c.setIpsecPhase2Proposals(ipsecPhase2ProposalsBuilder.build());

    // convert ipsec policies
    ImmutableSortedMap.Builder<String, IpsecPhase2Policy> ipsecPhase2PoliciesBuilder =
        ImmutableSortedMap.naturalOrder();
    for (Entry<String, IpsecPolicy> e : _ipsecPolicies.entrySet()) {
      String name = e.getKey();
      IpsecPolicy oldIpsecPolicy = e.getValue();
      org.batfish.datamodel.IpsecPolicy newPolicy = toIpsecPolicy(oldIpsecPolicy);
      _c.getIpsecPolicies().put(name, newPolicy);
      ipsecPhase2PoliciesBuilder.put(name, toIpsecPhase2Policy(oldIpsecPolicy));
    }
    _c.setIpsecPhase2Policies(ipsecPhase2PoliciesBuilder.build());

    // convert ipsec vpns
    ImmutableSortedMap.Builder<String, IpsecPeerConfig> ipsecPeerConfigBuilder =
        ImmutableSortedMap.naturalOrder();
    for (Entry<String, IpsecVpn> e : _ipsecVpns.entrySet()) {
      String name = e.getKey();
      IpsecVpn oldIpsecVpn = e.getValue();
      org.batfish.datamodel.IpsecVpn newIpsecVpn = toIpsecVpn(oldIpsecVpn);
      _c.getIpsecVpns().put(name, newIpsecVpn);

      IpsecPeerConfig ipsecPeerConfig = toIpsecPeerConfig(oldIpsecVpn);
      if (ipsecPeerConfig != null) {
        ipsecPeerConfigBuilder.put(name, ipsecPeerConfig);
      }
    }
    _c.setIpsecPeerConfigs(ipsecPeerConfigBuilder.build());

    // zones
    for (Zone zone : _zones.values()) {
      org.batfish.datamodel.Zone newZone = toZone(zone);
      _c.getZones().put(zone.getName(), newZone);
      if (!zone.getAddressBook().getEntries().isEmpty()) {
        _c.getIpSpaces().putAll(toIpSpaces(zone.getName(), zone.getAddressBook()));
      }
    }
    // If there are zones, then assume we will need to support existing connection ACL
    if (!_zones.isEmpty()) {
      _c.getIpAccessLists().put(ACL_NAME_EXISTING_CONNECTION, ACL_EXISTING_CONNECTION);
    }

    // default zone behavior
    _c.setDefaultCrossZoneAction(_defaultCrossZoneAction);
    _c.setDefaultInboundAction(_defaultInboundAction);

    for (Entry<String, RoutingInstance> e : _routingInstances.entrySet()) {
      String riName = e.getKey();
      RoutingInstance ri = e.getValue();
      Vrf vrf = _c.getVrfs().get(riName);

      // dhcp relay
      for (Entry<String, DhcpRelayGroup> e2 : ri.getDhcpRelayGroups().entrySet()) {
        DhcpRelayGroup rg = e2.getValue();
        List<org.batfish.datamodel.Interface> interfaces = new ArrayList<>();
        if (rg.getAllInterfaces()) {
          interfaces.addAll(_c.getInterfaces().values());
        } else {
          for (String ifaceName : rg.getInterfaces()) {
            org.batfish.datamodel.Interface iface = _c.getInterfaces().get(ifaceName);
            interfaces.add(iface);
          }
        }
        String asgName = rg.getActiveServerGroup();
        if (asgName != null) {
          DhcpRelayServerGroup asg = ri.getDhcpRelayServerGroups().get(asgName);
          if (asg != null) {
            for (org.batfish.datamodel.Interface iface : interfaces) {
              iface.getDhcpRelayAddresses().addAll(asg.getServers());
            }
          }
        }
      }

      // snmp
      SnmpServer snmpServer = ri.getSnmpServer();
      vrf.setSnmpServer(snmpServer);

      // static routes
      for (StaticRoute route :
          ri.getRibs().get(RoutingInformationBase.RIB_IPV4_UNICAST).getStaticRoutes().values()) {
        org.batfish.datamodel.StaticRoute newStaticRoute = toStaticRoute(route);
        vrf.getStaticRoutes().add(newStaticRoute);
      }

      // aggregate routes
      for (AggregateRoute route :
          ri.getRibs().get(RoutingInformationBase.RIB_IPV4_UNICAST).getAggregateRoutes().values()) {
        org.batfish.datamodel.GeneratedRoute newAggregateRoute = toAggregateRoute(route);
        vrf.getGeneratedRoutes().add(newAggregateRoute);
      }

      // generated routes
      for (GeneratedRoute route :
          ri.getRibs().get(RoutingInformationBase.RIB_IPV4_UNICAST).getGeneratedRoutes().values()) {
        org.batfish.datamodel.GeneratedRoute newGeneratedRoute = toGeneratedRoute(route);
        vrf.getGeneratedRoutes().add(newGeneratedRoute);
      }

      // create ospf process
      if (ri.getOspfAreas().size() > 0) {
        OspfProcess oproc = createOspfProcess(ri);
        vrf.setOspfProcess(oproc);
      }

      // create is-is process
      // is-is runs only if at least one interface has an ISO address, check loopback first
      Optional<IsoAddress> isoAddress =
          _defaultRoutingInstance
              .getInterfaces()
              .values()
              .stream()
              .filter(i -> i.getName().startsWith(FIRST_LOOPBACK_INTERFACE_NAME))
              .map(Interface::getIsoAddress)
              .filter(Objects::nonNull)
              .min(Comparator.comparing(IsoAddress::toString));
      // Try all the other interfaces if no ISO address on Loopback
      if (!isoAddress.isPresent()) {
        isoAddress =
            _defaultRoutingInstance
                .getInterfaces()
                .values()
                .stream()
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

    // mark forwarding table export policy if it exists
    String forwardingTableExportPolicyName =
        _defaultRoutingInstance.getForwardingTableExportPolicy();
    if (forwardingTableExportPolicyName != null) {
      PolicyStatement forwardingTableExportPolicy =
          _policyStatements.get(forwardingTableExportPolicyName);
      if (forwardingTableExportPolicy != null) {
        setPolicyStatementReferent(forwardingTableExportPolicyName);
      }
    }

    // Count and mark structure usages and identify undefined references
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
        JuniperStructureUsage.IKE_GATEWAY_EXTERNAL_INTERFACE,
        JuniperStructureUsage.IPSEC_VPN_BIND_INTERFACE);
    markConcreteStructure(
        JuniperStructureType.POLICY_STATEMENT,
        JuniperStructureUsage.BGP_EXPORT_POLICY,
        JuniperStructureUsage.BGP_IMPORT_POLICY,
        JuniperStructureUsage.FORWARDING_TABLE_EXPORT_POLICY,
        JuniperStructureUsage.GENERATED_ROUTE_POLICY,
        JuniperStructureUsage.OSPF_EXPORT_POLICY,
        JuniperStructureUsage.POLICY_STATEMENT_POLICY);
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

    warnEmptyPrefixLists();

    _c.computeRoutingPolicySources(_w);

    return _c;
  }

  private org.batfish.datamodel.Zone toZone(Zone zone) {

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

    org.batfish.datamodel.Zone newZone =
        new org.batfish.datamodel.Zone(
            zone.getName(), inboundFilterList, fromHostFilterList, toHostFilterList);
    for (Entry<Interface, FirewallFilter> e : zone.getInboundInterfaceFilters().entrySet()) {
      Interface inboundInterface = e.getKey();
      FirewallFilter inboundInterfaceFilter = e.getValue();
      String inboundInterfaceName = inboundInterface.getName();
      String inboundInterfaceFilterName = inboundInterfaceFilter.getName();
      org.batfish.datamodel.Interface newIface = _c.getInterfaces().get(inboundInterfaceName);
      IpAccessList inboundInterfaceFilterList =
          _c.getIpAccessLists().get(inboundInterfaceFilterName);
      newZone.getInboundInterfaceFilters().put(newIface.getName(), inboundInterfaceFilterList);
    }

    for (Entry<String, FirewallFilter> e : zone.getToZonePolicies().entrySet()) {
      String toZoneName = e.getKey();
      FirewallFilter toZoneFilter = e.getValue();
      String toZoneFilterName = toZoneFilter.getName();
      IpAccessList toZoneFilterList = _c.getIpAccessLists().get(toZoneFilterName);
      newZone.getToZonePolicies().put(toZoneName, toZoneFilterList);
    }

    for (Interface iface : zone.getInterfaces()) {
      String ifaceName = iface.getName();
      org.batfish.datamodel.Interface newIface = _c.getInterfaces().get(ifaceName);
      newIface.setZone(newZone);
      FirewallFilter inboundInterfaceFilter = zone.getInboundInterfaceFilters().get(iface);
      IpAccessList inboundInterfaceFilterList;
      if (inboundInterfaceFilter != null) {
        String name = inboundInterfaceFilter.getName();
        inboundInterfaceFilterList = _c.getIpAccessLists().get(name);
      } else {
        inboundInterfaceFilterList = inboundFilterList;
      }
      newZone.getInboundInterfaceFilters().put(newIface.getName(), inboundInterfaceFilterList);
    }

    return newZone;
  }

  private void warnEmptyPrefixLists() {
    for (Entry<String, PrefixList> e : _prefixLists.entrySet()) {
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
          allInterfaces
              .entrySet()
              .stream()
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

  public Map<String, ApplicationSet> getApplicationSets() {
    return _applicationSets;
  }
}

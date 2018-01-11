package org.batfish.bdp;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.common.Pair;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfInternalRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfMetricType;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;

public class VirtualRouter extends ComparableStructure<String> {

  /** */
  private static final long serialVersionUID = 1L;

  transient BgpMultipathRib _baseEbgpRib;

  transient BgpMultipathRib _baseIbgpRib;

  transient BgpBestPathRib _bgpBestPathRib;

  transient BgpMultipathRib _bgpMultipathRib;

  final Configuration _c;

  transient ConnectedRib _connectedRib;

  transient BgpBestPathRib _ebgpBestPathRib;

  transient BgpMultipathRib _ebgpMultipathRib;

  transient BgpMultipathRib _ebgpStagingRib;

  Fib _fib;

  transient Rib _generatedRib;

  transient BgpBestPathRib _ibgpBestPathRib;

  transient BgpMultipathRib _ibgpMultipathRib;

  transient BgpMultipathRib _ibgpStagingRib;

  /**
   * The independent RIB contains connected and static routes, which are unaffected by BDP
   * iterations (hence, independent).
   */
  transient Rib _independentRib;

  /** The finalized RIB, a combination different protocol RIBs */
  Rib _mainRib;

  transient OspfExternalType1Rib _ospfExternalType1Rib;

  transient OspfExternalType1Rib _ospfExternalType1StagingRib;

  transient OspfExternalType2Rib _ospfExternalType2Rib;

  transient OspfExternalType2Rib _ospfExternalType2StagingRib;

  transient OspfInterAreaRib _ospfInterAreaRib;

  transient OspfInterAreaRib _ospfInterAreaStagingRib;

  transient OspfIntraAreaRib _ospfIntraAreaRib;

  transient OspfIntraAreaRib _ospfIntraAreaStagingRib;

  transient OspfRib _ospfRib;

  transient BgpBestPathRib _prevBgpBestPathRib;

  transient BgpMultipathRib _prevBgpMultipathRib;

  transient BgpBestPathRib _prevEbgpBestPathRib;

  transient BgpMultipathRib _prevEbgpMultipathRib;

  transient BgpBestPathRib _prevIbgpBestPathRib;

  transient BgpMultipathRib _prevIbgpMultipathRib;

  transient Rib _prevMainRib;

  transient OspfExternalType1Rib _prevOspfExternalType1Rib;

  transient OspfExternalType2Rib _prevOspfExternalType2Rib;

  Set<BgpAdvertisement> _receivedBgpAdvertisements;

  transient RipInternalRib _ripInternalRib;

  transient RipInternalRib _ripInternalStagingRib;

  transient RipRib _ripRib;

  Set<BgpAdvertisement> _sentBgpAdvertisements;

  transient StaticRib _staticInterfaceRib;

  transient StaticRib _staticRib;

  final Vrf _vrf;

  private Set<BgpAdvertisement> _prevSentBgpAdvertisements;

  VirtualRouter(String name, Configuration c) {
    super(name);
    _c = c;
    _vrf = c.getVrfs().get(name);
  }

  /**
   * Initializes easy-to-compute RIBs that are not affected by BDP iterations (e.g., static route
   * RIB, connected route RIB, etc.)
   *
   * @param ipOwners Mapping of IPs to nodes names as computed by batfish parser
   * @param externalAdverts a set of external BGP advertisements
   */
  void initRibsForBdp(Map<Ip, Set<String>> ipOwners, Set<BgpAdvertisement> externalAdverts) {
    initConnectedRib();
    initStaticRib();
    importRib(_independentRib, _connectedRib);
    importRib(_independentRib, _staticInterfaceRib);
    importRib(_mainRib, _independentRib);
    initIntraAreaOspfRoutes();
    initBaseRipRoutes();
    initEbgpTopology(ipOwners);
    initBaseBgpRibs(externalAdverts, ipOwners);
  }

  boolean activateGeneratedRoutes() {
    boolean changed = false;

    for (GeneratedRoute gr : _vrf.getGeneratedRoutes()) {
      boolean active = true;
      String generationPolicyName = gr.getGenerationPolicy();
      GeneratedRoute.Builder grb = new GeneratedRoute.Builder();
      grb.setNetwork(gr.getNetwork());
      grb.setAdmin(gr.getAdministrativeCost());
      grb.setMetric(gr.getMetric() != null ? gr.getMetric() : 0);
      grb.setAttributePolicy(gr.getAttributePolicy());
      grb.setGenerationPolicy(gr.getGenerationPolicy());
      boolean discard = gr.getDiscard();
      grb.setDiscard(discard);
      if (discard) {
        grb.setNextHopInterface(Interface.NULL_INTERFACE_NAME);
      }
      if (generationPolicyName != null) {
        RoutingPolicy generationPolicy = _c.getRoutingPolicies().get(generationPolicyName);
        if (generationPolicy != null) {
          active = false;
          for (AbstractRoute contributingRoute : _prevMainRib.getRoutes()) {
            boolean accept =
                generationPolicy.process(contributingRoute, grb, null, _key, Direction.OUT);
            if (accept) {
              if (!discard) {
                grb.setNextHopIp(contributingRoute.getNextHopIp());
              }
              active = true;
              break;
            }
          }
        }
      }
      if (active) {
        GeneratedRoute newGr = grb.build();
        if (_generatedRib.mergeRoute(newGr)) {
          changed = true;
        }
      }
    }
    return changed;
  }

  /**
   * Re-activate static routes at the beginning of an iteration. Directly adds a static route to the
   * main RIB if the route's next-hop-ip matches routes from the previous iterations.
   */
  boolean activateStaticRoutes() {
    boolean changed = false;
    for (StaticRoute sr : _staticRib.getRoutes()) {
      // See if we had (in the previous RIB) any routes matching this route's next hop IP
      Set<AbstractRoute> matchingRoutes = _prevMainRib.longestPrefixMatch(sr.getNextHopIp());
      Prefix staticRoutePrefix = sr.getNetwork();

      for (AbstractRoute matchingRoute : matchingRoutes) {
        Prefix matchingRoutePrefix = matchingRoute.getNetwork();
        // check to make sure matching route's prefix does not totally
        // contain this static route's prefix
        if (matchingRoutePrefix.getAddress().asLong() > staticRoutePrefix.getAddress().asLong()
            || matchingRoutePrefix.getEndAddress().asLong()
                < staticRoutePrefix.getEndAddress().asLong()) {
          changed |= _mainRib.mergeRoute(sr);
          break; // break out of the inner loop but continue processing static routes
        }
      }
    }
    return changed;
  }

  /** Compute the FIB from the main RIB */
  public void computeFib() {
    _fib = new Fib(_mainRib);
  }

  /**
   * Decides whether the current OSPF summary route metric needs to be changed based on the given
   * route's metric.
   *
   * <p>Routes from the same area or outside of areaPrefix have no effect on the summary metric.
   *
   * @param route The route in question, whose metric is considered
   * @param areaPrefix The Ip prefix of the OSPF area
   * @param currentMetric The current summary metric for the area
   * @param areaNum Area number.
   * @param useMin Whether to use the older RFC 1583 computation, which takes the minimum of metrics
   *     as opposed to the newer RFC 2328, which uses the maximum
   * @return the newly computed summary metric.
   */
  @Nullable
  static Long computeUpdatedOspfSummaryMetric(
      OspfInternalRoute route,
      Prefix areaPrefix,
      @Nullable Long currentMetric,
      long areaNum,
      boolean useMin) {
    Prefix contributingRoutePrefix = route.getNetwork();
    // Only update metric for different areas and if the area prefix contains the route prefix
    if (areaNum == route.getArea() || !areaPrefix.containsPrefix(contributingRoutePrefix)) {
      return currentMetric;
    }
    long contributingRouteMetric = route.getMetric();
    // Definitely update if we have no previous metric
    if (currentMetric == null) {
      return contributingRouteMetric;
    }
    // Take the best metric between the route's and current available.
    // Routers (at least Cisco and Juniper) default to min metric unless using RFC2328 with
    // RFC1583 compatibility explicitly disabled, in which case they default to max.
    if (useMin) {
      return Math.min(currentMetric, contributingRouteMetric);
    }
    return Math.max(currentMetric, contributingRouteMetric);
  }

  boolean computeInterAreaSummaries() {
    OspfProcess proc = _vrf.getOspfProcess();
    boolean changed = false;
    // Ensure we have a running OSPF process on the VRF, otherwise bail.
    if (proc == null) {
      return false;
    }
    // Admin cost for the given protocol
    int admin = RoutingProtocol.OSPF_IA.getSummaryAdministrativeCost(_c.getConfigurationFormat());

    // Determine whether to use min metric by default, based on RFC1583 compatibility setting.
    // Routers (at least Cisco and Juniper) default to min metric unless using RFC2328 with
    // RFC1583 compatibility explicitly disabled, in which case they default to max.
    boolean useMin = MoreObjects.firstNonNull(proc.getRfc1583Compatible(), true);

    // Compute summaries for each area
    for (Entry<Long, OspfArea> e : proc.getAreas().entrySet()) {
      long areaNum = e.getKey();
      OspfArea area = e.getValue();
      for (Entry<Prefix, Boolean> e2 : area.getSummaries().entrySet()) {
        Prefix prefix = e2.getKey();
        boolean advertise = e2.getValue();

        // Only advertised summaries can contribute
        if (!advertise) {
          continue;
        }

        // Compute the metric from any possible contributing routes
        Long metric = null;
        for (OspfIntraAreaRoute contributingRoute : _ospfIntraAreaRib.getRoutes()) {
          metric =
              computeUpdatedOspfSummaryMetric(contributingRoute, prefix, metric, areaNum, useMin);
        }
        for (OspfInterAreaRoute contributingRoute : _ospfInterAreaRib.getRoutes()) {
          metric =
              computeUpdatedOspfSummaryMetric(contributingRoute, prefix, metric, areaNum, useMin);
        }

        // No routes contributed to the summary, nothing to construct
        if (metric == null) {
          continue;
        }

        // Non-null metric means we generate a new summary and put it in the RIB
        OspfInterAreaRoute summaryRoute =
            new OspfInterAreaRoute(prefix, Ip.ZERO, admin, metric, areaNum);
        if (_ospfInterAreaStagingRib.mergeRoute(summaryRoute)) {
          changed = true;
        }
      }
    }
    return changed;
  }

  /**
   * Whether or not a given remote neighbor has priority to advertise to this neighbor this
   * iteration. Lexicographically-lower neighbors have priority to advertise in odd iterations,
   * while lexicographically-higher neighbors have priority to advertise only in even iterations.
   *
   * @param route The route to advertise
   * @param currentIteration The current dependent routes iteration
   * @param oscillatingPrefixes All prefixes known to oscillate over all recovery attempts
   * @param neighbor This neighbor
   * @param remoteBgpNeighbor The remote neighbor
   * @return Whether or not a given remote neighbor is allowed to advertise to this neighbor this
   *     iteration.
   */
  private boolean hasAdvertisementPriorityDuringRecovery(
      AbstractRoute route,
      int currentIteration,
      SortedSet<Prefix> oscillatingPrefixes,
      BgpNeighbor neighbor,
      BgpNeighbor remoteBgpNeighbor) {
    if (oscillatingPrefixes.contains(route.getNetwork())) {
      Pair<String, String> vrf = new Pair<>(neighbor.getOwner().getHostname(), neighbor.getVrf());
      Pair<String, String> remoteVrf =
          new Pair<>(remoteBgpNeighbor.getOwner().getHostname(), remoteBgpNeighbor.getVrf());
      boolean odd = currentIteration % 2 > 0;
      boolean lower = vrf.compareTo(remoteVrf) < 0;
      if (lower) {
        return odd;
      } else {
        return !odd;
      }
    } else {
      return true;
    }
  }

  <U extends AbstractRoute, T extends U> void importRib(
      AbstractRib<U> importingRib, AbstractRib<T> exportingRib) {
    for (T route : exportingRib.getRoutes()) {
      importingRib.mergeRoute(route);
    }
  }

  public void initBaseBgpRibs(
      Set<BgpAdvertisement> externalAdverts, Map<Ip, Set<String>> ipOwners) {
    if (_vrf.getBgpProcess() != null) {
      int ebgpAdmin = RoutingProtocol.BGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
      int ibgpAdmin =
          RoutingProtocol.IBGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());

      for (BgpAdvertisement advert : externalAdverts) {
        if (advert.getDstNode().equals(_c.getHostname())) {
          Ip dstIp = advert.getDstIp();
          Set<String> dstIpOwners = ipOwners.get(dstIp);
          String hostname = _c.getHostname();
          if (dstIpOwners == null || !dstIpOwners.contains(hostname)) {
            continue;
          }
          Ip srcIp = advert.getSrcIp();
          // TODO: support passive bgp connections
          Prefix srcPrefix = new Prefix(srcIp, Prefix.MAX_PREFIX_LENGTH);
          BgpNeighbor neighbor = _vrf.getBgpProcess().getNeighbors().get(srcPrefix);
          if (neighbor != null) {
            BgpAdvertisementType type = advert.getType();
            BgpRoute.Builder outgoingRouteBuilder = new BgpRoute.Builder();
            boolean ebgp;
            boolean received;
            switch (type) {
              case EBGP_RECEIVED:
                ebgp = true;
                received = true;
                break;

              case EBGP_SENT:
                ebgp = true;
                received = false;
                break;

              case IBGP_RECEIVED:
                ebgp = false;
                received = true;
                break;

              case IBGP_SENT:
                ebgp = false;
                received = false;
                break;

              case EBGP_ORIGINATED:
              case IBGP_ORIGINATED:
              default:
                throw new BatfishException("Missing or invalid bgp advertisement type");
            }
            BgpMultipathRib targetRib = ebgp ? _baseEbgpRib : _baseIbgpRib;
            RoutingProtocol targetProtocol = ebgp ? RoutingProtocol.BGP : RoutingProtocol.IBGP;

            if (received) {
              int admin = ebgp ? ebgpAdmin : ibgpAdmin;
              AsPath asPath = advert.getAsPath();
              SortedSet<Long> clusterList = advert.getClusterList();
              SortedSet<Long> communities = new TreeSet<>(advert.getCommunities());
              int localPreference = advert.getLocalPreference();
              long metric = advert.getMed();
              Prefix network = advert.getNetwork();
              Ip nextHopIp = advert.getNextHopIp();
              Ip originatorIp = advert.getOriginatorIp();
              OriginType originType = advert.getOriginType();
              RoutingProtocol srcProtocol = advert.getSrcProtocol();
              int weight = advert.getWeight();
              BgpRoute.Builder builder = new BgpRoute.Builder();
              builder.setAdmin(admin);
              builder.setAsPath(asPath.getAsSets());
              builder.setClusterList(clusterList);
              builder.setCommunities(communities);
              builder.setLocalPreference(localPreference);
              builder.setMetric(metric);
              builder.setNetwork(network);
              builder.setNextHopIp(nextHopIp);
              builder.setOriginatorIp(originatorIp);
              builder.setOriginType(originType);
              builder.setProtocol(targetProtocol);
              // TODO: support external route reflector clients
              builder.setReceivedFromRouteReflectorClient(false);
              builder.setSrcProtocol(srcProtocol);
              // TODO: possibly suppport setting tag
              builder.setWeight(weight);
              BgpRoute route = builder.build();
              targetRib.mergeRoute(route);
            } else {
              int localPreference;
              if (ebgp) {
                localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
              } else {
                localPreference = advert.getLocalPreference();
              }
              outgoingRouteBuilder.setAsPath(advert.getAsPath().getAsSets());
              outgoingRouteBuilder.setCommunities(new TreeSet<>(advert.getCommunities()));
              outgoingRouteBuilder.setLocalPreference(localPreference);
              outgoingRouteBuilder.setMetric(advert.getMed());
              outgoingRouteBuilder.setNetwork(advert.getNetwork());
              outgoingRouteBuilder.setNextHopIp(advert.getNextHopIp());
              outgoingRouteBuilder.setOriginatorIp(advert.getOriginatorIp());
              outgoingRouteBuilder.setOriginType(advert.getOriginType());
              outgoingRouteBuilder.setProtocol(targetProtocol);
              // TODO:
              // outgoingRouteBuilder.setReceivedFromRouteReflectorClient(...);
              outgoingRouteBuilder.setSrcProtocol(advert.getSrcProtocol());
              BgpRoute transformedOutgoingRoute = outgoingRouteBuilder.build();
              BgpRoute.Builder transformedIncomingRouteBuilder = new BgpRoute.Builder();

              // Incoming originatorIp
              transformedIncomingRouteBuilder.setOriginatorIp(
                  transformedOutgoingRoute.getOriginatorIp());

              // Incoming clusterList
              transformedIncomingRouteBuilder
                  .getClusterList()
                  .addAll(transformedOutgoingRoute.getClusterList());

              // Incoming receivedFromRouteReflectorClient
              transformedIncomingRouteBuilder.setReceivedFromRouteReflectorClient(
                  transformedOutgoingRoute.getReceivedFromRouteReflectorClient());

              // Incoming asPath
              transformedIncomingRouteBuilder.setAsPath(
                  transformedOutgoingRoute.getAsPath().getAsSets());

              // Incoming communities
              transformedIncomingRouteBuilder
                  .getCommunities()
                  .addAll(transformedOutgoingRoute.getCommunities());

              // Incoming protocol
              transformedIncomingRouteBuilder.setProtocol(targetProtocol);

              // Incoming network
              transformedIncomingRouteBuilder.setNetwork(transformedOutgoingRoute.getNetwork());

              // Incoming nextHopIp
              transformedIncomingRouteBuilder.setNextHopIp(transformedOutgoingRoute.getNextHopIp());

              // Incoming originType
              transformedIncomingRouteBuilder.setOriginType(
                  transformedOutgoingRoute.getOriginType());

              // Incoming localPreference
              transformedIncomingRouteBuilder.setLocalPreference(
                  transformedOutgoingRoute.getLocalPreference());

              // Incoming admin
              int admin = ebgp ? ebgpAdmin : ibgpAdmin;
              transformedIncomingRouteBuilder.setAdmin(admin);

              // Incoming metric
              transformedIncomingRouteBuilder.setMetric(transformedOutgoingRoute.getMetric());

              // Incoming srcProtocol
              transformedIncomingRouteBuilder.setSrcProtocol(targetProtocol);
              String importPolicyName = neighbor.getImportPolicy();
              // TODO: ensure there is always an import policy

              if (ebgp
                  && transformedOutgoingRoute.getAsPath().containsAs(neighbor.getLocalAs())
                  && !neighbor.getAllowLocalAsIn()) {
                // skip routes containing peer's AS unless
                // disable-peer-as-check (getAllowRemoteAsOut) is set
                continue;
              }

              /*
               * CREATE INCOMING ROUTE
               */
              boolean acceptIncoming = true;
              if (importPolicyName != null) {
                RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
                if (importPolicy != null) {
                  acceptIncoming =
                      importPolicy.process(
                          transformedOutgoingRoute,
                          transformedIncomingRouteBuilder,
                          advert.getSrcIp(),
                          _key,
                          Direction.IN);
                }
              }
              if (acceptIncoming) {
                BgpRoute transformedIncomingRoute = transformedIncomingRouteBuilder.build();
                targetRib.mergeRoute(transformedIncomingRoute);
              }
            }
          }
        }
      }
    }

    importRib(_ebgpMultipathRib, _baseEbgpRib);
    importRib(_ebgpBestPathRib, _baseEbgpRib);
    importRib(_bgpBestPathRib, _baseEbgpRib);
    importRib(_ibgpMultipathRib, _baseIbgpRib);
    importRib(_ibgpBestPathRib, _baseIbgpRib);
    importRib(_bgpBestPathRib, _baseIbgpRib);
  }

  /** Initialize Intra-area OSPF routes from the interface prefixes */
  private void initIntraAreaOspfRoutes() {
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return; // nothing to do
    }
    // init intra-area routes from connected routes
    // For each interface within an OSPF area and each interface prefix,
    // construct a new OSPF-IA route. Put it in the IA RIB.
    proc.getAreas()
        .forEach(
            (areaNum, area) -> {
              for (Interface iface : area.getInterfaces().values()) {
                if (iface.getActive()) {
                  Set<Prefix> allNetworkPrefixes =
                      iface
                          .getAllAddresses()
                          .stream()
                          .map(Prefix::getNetworkPrefix)
                          .collect(Collectors.toSet());
                  int interfaceOspfCost = iface.getOspfCost();
                  for (Prefix prefix : allNetworkPrefixes) {
                    long cost = interfaceOspfCost;
                    boolean stubNetwork = iface.getOspfPassive() || iface.getOspfPointToPoint();
                    if (stubNetwork) {
                      if (proc.getMaxMetricStubNetworks() != null) {
                        cost = proc.getMaxMetricStubNetworks();
                      }
                    } else if (proc.getMaxMetricTransitLinks() != null) {
                      cost = proc.getMaxMetricTransitLinks();
                    }
                    OspfIntraAreaRoute route =
                        new OspfIntraAreaRoute(
                            prefix,
                            null,
                            RoutingProtocol.OSPF.getDefaultAdministrativeCost(
                                _c.getConfigurationFormat()),
                            cost,
                            areaNum);
                    _ospfIntraAreaRib.mergeRoute(route);
                  }
                }
              }
            });
  }

  /** Initialize RIP routes from the interface prefixes */
  @VisibleForTesting
  void initBaseRipRoutes() {
    if (_vrf.getRipProcess() == null) {
      return; // nothing to do
    }
    // init internal routes from connected routes
    for (String ifaceName : _vrf.getRipProcess().getInterfaces()) {
      Interface iface = _vrf.getInterfaces().get(ifaceName);
      if (iface.getActive()) {
        Set<Prefix> allNetworkPrefixes =
            iface
                .getAllAddresses()
                .stream()
                .map(Prefix::getNetworkPrefix)
                .collect(Collectors.toSet());
        long cost = RipProcess.DEFAULT_RIP_COST;
        for (Prefix prefix : allNetworkPrefixes) {
          RipInternalRoute route =
              new RipInternalRoute(
                  prefix,
                  null,
                  RoutingProtocol.RIP.getDefaultAdministrativeCost(_c.getConfigurationFormat()),
                  cost);
          _ripInternalRib.mergeRoute(route);
        }
      }
    }
  }

  /**
   * This function creates BGP routes from generated routes that go into the BGP RIB, but cannot be
   * imported into the main RIB. The purpose of these routes is to prevent the local router from
   * accepting advertisements less desirable than the local generated ones for the given network.
   * They are not themselves advertised.
   */
  void initBgpAggregateRoutes() {
    // first import aggregates
    switch (_c.getConfigurationFormat()) {
      case JUNIPER:
      case JUNIPER_SWITCH:
        return;
        // $CASES-OMITTED$
      default:
        break;
    }
    for (AbstractRoute grAbstract : _generatedRib.getRoutes()) {
      GeneratedRoute gr = (GeneratedRoute) grAbstract;
      BgpRoute.Builder b = new BgpRoute.Builder();
      b.setAdmin(gr.getAdministrativeCost());
      b.setAsPath(gr.getAsPath().getAsSets());
      b.setMetric(gr.getMetric());
      b.setSrcProtocol(RoutingProtocol.AGGREGATE);
      b.setProtocol(RoutingProtocol.AGGREGATE);
      b.setNetwork(gr.getNetwork());
      b.setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE);
      /* Note: Origin type and originator IP should get overwritten, but are needed initially */
      b.setOriginatorIp(_vrf.getBgpProcess().getRouterId());
      b.setOriginType(OriginType.INCOMPLETE);
      BgpRoute br = b.build();
      br.setNonRouting(true);
      _bgpMultipathRib.mergeRoute(br);
      _bgpBestPathRib.mergeRoute(br);
    }
  }

  /**
   * Initialize the connected RIB -- a RIB containing connected routes (i.e., direct connections to
   * neighbors).
   */
  @VisibleForTesting
  void initConnectedRib() {
    // Look at all connected interfaces
    for (Interface i : _vrf.getInterfaces().values()) {
      if (i.getActive()) { // Make sure the interface is active
        // Create a route for each interface prefix
        for (Prefix ifacePrefix : i.getAllAddresses()) {
          Prefix prefix = ifacePrefix.getNetworkPrefix();
          ConnectedRoute cr = new ConnectedRoute(prefix, i.getName());
          _connectedRib.mergeRoute(cr);
        }
      }
    }
  }

  private void initEbgpTopology(Map<Ip, Set<String>> ipOwners) {
    if (_vrf.getBgpProcess() == null) {
      return; // Nothing to do
    }
    String hostname = _c.getHostname();
    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      // Ebgp iff the neighbor AS is different from our AS
      boolean ebgpSession = !neighbor.getRemoteAs().equals(neighbor.getLocalAs());
      // We don't handle IBGP here
      if (!ebgpSession) {
        continue;
      }

      BgpNeighbor remoteNeighbor = neighbor.getRemoteBgpNeighbor();
      if (remoteNeighbor == null) {
        continue;
      }

      Set<String> localIpOwners = ipOwners.get(neighbor.getLocalIp());
      boolean nodeOwnsLocalIp = localIpOwners.contains(hostname);
      Set<String> remoteLocalIpOwners = ipOwners.get(remoteNeighbor.getLocalIp());
      String remoteHostname = remoteNeighbor.getOwner().getHostname();
      boolean remoteNeighborOwnsRemoteLocalIp = remoteLocalIpOwners.contains(remoteHostname);
      if (nodeOwnsLocalIp && remoteNeighborOwnsRemoteLocalIp) {
        // pretty confident we have a bgp connection here
        // for now, just leave it alone
      } else {
        neighbor.setRemoteBgpNeighbor(null);
        remoteNeighbor.setRemoteBgpNeighbor(null);
      }
    }
  }

  public void initIbgpTopology(BdpDataPlane dp) {
    // TODO: implement
  }

  @Nullable
  @VisibleForTesting
  OspfExternalRoute computeOspfExportRoute(
      AbstractRoute potentialExportRoute, RoutingPolicy exportPolicy, OspfProcess proc) {
    OspfExternalRoute.Builder outputRouteBuilder = new OspfExternalRoute.Builder();
    // Export based on the policy result of processing the potentialExportRoute
    boolean accept =
        exportPolicy.process(potentialExportRoute, outputRouteBuilder, null, _key, Direction.OUT);
    if (!accept) {
      return null;
    }
    OspfMetricType metricType = outputRouteBuilder.getOspfMetricType();
    outputRouteBuilder.setAdmin(
        outputRouteBuilder
            .getOspfMetricType()
            .toRoutingProtocol()
            .getDefaultAdministrativeCost(_c.getConfigurationFormat()));
    outputRouteBuilder.setNetwork(potentialExportRoute.getNetwork());
    Long maxMetricExternalNetworks = proc.getMaxMetricExternalNetworks();
    long costToAdvertiser;
    if (maxMetricExternalNetworks != null) {
      if (metricType == OspfMetricType.E1) {
        outputRouteBuilder.setMetric(maxMetricExternalNetworks);
      }
      costToAdvertiser = maxMetricExternalNetworks;
    } else {
      costToAdvertiser = 0L;
    }
    outputRouteBuilder.setCostToAdvertiser(costToAdvertiser);
    outputRouteBuilder.setAdvertiser(_c.getHostname());
    outputRouteBuilder.setArea(OspfRoute.NO_AREA);
    outputRouteBuilder.setLsaMetric(outputRouteBuilder.getMetric());
    OspfExternalRoute outputRoute = outputRouteBuilder.build();
    outputRoute.setNonRouting(true);
    return outputRoute;
  }

  void initOspfExports() {
    OspfProcess proc = _vrf.getOspfProcess();
    // Nothing to do
    if (proc == null) {
      return;
    }

    // get OSPF export policy name
    String exportPolicyName = _vrf.getOspfProcess().getExportPolicy();
    if (exportPolicyName == null) {
      return; // nothing to export
    }

    RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(exportPolicyName);
    if (exportPolicy == null) {
      return; // nothing to export
    }

    // For each route in the previous RIB, compute an export route and add it to the appropriate
    // RIB.
    for (AbstractRoute potentialExport : _prevMainRib.getRoutes()) {
      OspfExternalRoute outputRoute = computeOspfExportRoute(potentialExport, exportPolicy, proc);
      if (outputRoute == null) {
        continue; // no need to export
      }
      if (outputRoute.getOspfMetricType() == OspfMetricType.E1) {
        _ospfExternalType1Rib.mergeRoute((OspfExternalType1Route) outputRoute);
      } else { // assuming here that MetricType exists. Or E2 is the default
        _ospfExternalType2Rib.mergeRoute((OspfExternalType2Route) outputRoute);
      }
    }
  }

  /** Initialize all ribs on this router. All RIBs will be empty */
  @VisibleForTesting
  void initRibs() {
    _bgpMultipathRib = new BgpMultipathRib(this);
    _connectedRib = new ConnectedRib(this);
    _ebgpMultipathRib = new BgpMultipathRib(this);
    _ebgpStagingRib = new BgpMultipathRib(this);
    _ibgpMultipathRib = new BgpMultipathRib(this);
    _ibgpStagingRib = new BgpMultipathRib(this);
    _independentRib = new Rib(this);
    _mainRib = new Rib(this);
    _ospfExternalType1Rib = new OspfExternalType1Rib(this);
    _ospfExternalType2Rib = new OspfExternalType2Rib(this);
    _ospfExternalType1StagingRib = new OspfExternalType1Rib(this);
    _ospfExternalType2StagingRib = new OspfExternalType2Rib(this);
    _ospfInterAreaRib = new OspfInterAreaRib(this);
    _ospfInterAreaStagingRib = new OspfInterAreaRib(this);
    _ospfIntraAreaRib = new OspfIntraAreaRib(this);
    _ospfIntraAreaStagingRib = new OspfIntraAreaRib(this);
    _ospfRib = new OspfRib(this);
    _ripInternalRib = new RipInternalRib(this);
    _ripInternalStagingRib = new RipInternalRib(this);
    _ripRib = new RipRib(this);
    _staticRib = new StaticRib(this);
    _staticInterfaceRib = new StaticRib(this);
    _bgpMultipathRib = new BgpMultipathRib(this);
    _baseEbgpRib = new BgpMultipathRib(this);
    _baseIbgpRib = new BgpMultipathRib(this);

    _ebgpMultipathRib = new BgpMultipathRib(this);
    _ibgpMultipathRib = new BgpMultipathRib(this);
    /*
     * We use prev-less best-path rib since it is read-only and will never contain anything during
     * age comparison.
     */
    _ebgpBestPathRib = BgpBestPathRib.initial(this);
    _ibgpBestPathRib = BgpBestPathRib.initial(this);
    _bgpBestPathRib = BgpBestPathRib.initial(this);
  }

  /** Initialize the static route RIB from the VRF config. The resulting RIB cannot be modified */
  void initStaticRib() {
    for (StaticRoute sr : _vrf.getStaticRoutes()) {
      String nextHopInt = sr.getNextHopInterface();
      if (!nextHopInt.equals(Route.UNSET_NEXT_HOP_INTERFACE)
          && !Interface.NULL_INTERFACE_NAME.equals(nextHopInt)
          && !_vrf.getInterfaces().get(nextHopInt).getActive()) {
        continue;
      }
      // interface route
      if (sr.getNextHopIp().equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
        _staticInterfaceRib.mergeRoute(sr);
      } else {
        _staticRib.mergeRoute(sr);
      }
    }
  }

  int computeBgpAdvertisementsToOutside(Map<Ip, Set<String>> ipOwners) {
    int numAdvertisements = 0;

    // If we have no BGP process, nothing to do
    if (_vrf.getBgpProcess() == null) {
      return numAdvertisements;
    }

    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      Ip localIp = neighbor.getLocalIp();
      Set<String> localIpOwners = ipOwners.get(localIp);
      String hostname = _c.getHostname();
      if (localIpOwners == null || !localIpOwners.contains(hostname)) {
        continue;
      }
      Prefix remotePrefix = neighbor.getPrefix();
      if (remotePrefix.getPrefixLength() != Prefix.MAX_PREFIX_LENGTH) {
        // Do not support dynamic outside neighbors
        continue;
      }
      Ip remoteIp = remotePrefix.getAddress();
      if (ipOwners.get(remoteIp) != null) {
        // Skip if neighbor is not outside the network
        continue;
      }

      int localAs = neighbor.getLocalAs();
      int remoteAs = neighbor.getRemoteAs();
      String remoteHostname = remoteIp.toString();
      String remoteVrfName = Configuration.DEFAULT_VRF_NAME;
      RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(neighbor.getExportPolicy());
      boolean ebgpSession = localAs != remoteAs;
      RoutingProtocol targetProtocol = ebgpSession ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
      Set<AbstractRoute> candidateRoutes = Collections.newSetFromMap(new IdentityHashMap<>());

      // Add IGP routes
      Set<AbstractRoute> activeRoutes = Collections.newSetFromMap(new IdentityHashMap<>());
      activeRoutes.addAll(_mainRib.getRoutes());
      for (AbstractRoute candidateRoute : activeRoutes) {
        if (candidateRoute.getProtocol() != RoutingProtocol.BGP
            && candidateRoute.getProtocol() != RoutingProtocol.IBGP) {
          candidateRoutes.add(candidateRoute);
        }
      }

      /*
       * bgp advertise-external
       *
       * When this is set, add best eBGP path independently of whether
       * it is preempted by an iBGP or IGP route. Only applicable to
       * iBGP sessions.
       */
      boolean advertiseExternal = !ebgpSession && neighbor.getAdvertiseExternal();
      if (advertiseExternal) {
        candidateRoutes.addAll(_ebgpBestPathRib.getRoutes());
      }

      /*
       * bgp advertise-inactive
       *
       * When this is set, add best BGP path independently of whether
       * it is preempted by an IGP route. Only applicable to eBGP
       * sessions.
       */
      boolean advertiseInactive = ebgpSession && neighbor.getAdvertiseInactive();
      /* Add best bgp paths if they are active, or if advertise-inactive */
      for (AbstractRoute candidateRoute : _bgpBestPathRib.getRoutes()) {
        if (advertiseInactive || activeRoutes.contains(candidateRoute)) {
          candidateRoutes.add(candidateRoute);
        }
      }

      /* Add all bgp paths if additional-paths active for this session */
      boolean additionalPaths =
          !ebgpSession
              && neighbor.getAdditionalPathsSend()
              && neighbor.getAdditionalPathsSelectAll();
      if (additionalPaths) {
        candidateRoutes.addAll(_bgpMultipathRib.getRoutes());
      }
      for (AbstractRoute route : candidateRoutes) {
        BgpRoute.Builder transformedOutgoingRouteBuilder = new BgpRoute.Builder();
        RoutingProtocol routeProtocol = route.getProtocol();
        boolean routeIsBgp =
            routeProtocol == RoutingProtocol.IBGP || routeProtocol == RoutingProtocol.BGP;

        // originatorIP
        Ip originatorIp;
        if (!ebgpSession && routeProtocol.equals(RoutingProtocol.IBGP)) {
          BgpRoute bgpRoute = (BgpRoute) route;
          originatorIp = bgpRoute.getOriginatorIp();
        } else {
          originatorIp = _vrf.getBgpProcess().getRouterId();
        }
        transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);

        // clusterList, receivedFromRouteReflectorClient, (originType
        // for bgp remote route)
        if (routeIsBgp) {
          BgpRoute bgpRoute = (BgpRoute) route;
          transformedOutgoingRouteBuilder.setOriginType(bgpRoute.getOriginType());
          if (ebgpSession
              && bgpRoute.getAsPath().containsAs(neighbor.getRemoteAs())
              && !neighbor.getAllowRemoteAsOut()) {
            // skip routes containing peer's AS unless
            // disable-peer-as-check (getAllowRemoteAsOut) is set
            continue;
          }
          /*
           * route reflection: reflect everything received from
           * clients to clients and non-clients. reflect everything
           * received from non-clients to clients. Do not reflect to
           * originator
           */

          Ip routeOriginatorIp = bgpRoute.getOriginatorIp();
          /*
           *  iBGP speaker should not send out routes to iBGP neighbor whose router-id is
           *  same as originator id of advertisement
           */
          if (!ebgpSession && routeOriginatorIp != null && remoteIp.equals(routeOriginatorIp)) {
            continue;
          }
          if (routeProtocol.equals(RoutingProtocol.IBGP) && !ebgpSession) {
            boolean routeReceivedFromRouteReflectorClient =
                bgpRoute.getReceivedFromRouteReflectorClient();
            boolean sendingToRouteReflectorClient = neighbor.getRouteReflectorClient();
            transformedOutgoingRouteBuilder.getClusterList().addAll(bgpRoute.getClusterList());
            if (!routeReceivedFromRouteReflectorClient && !sendingToRouteReflectorClient) {
              continue;
            }
            if (sendingToRouteReflectorClient) {
              // sender adds its local cluster id to clusterlist of
              // new route
              transformedOutgoingRouteBuilder.getClusterList().add(neighbor.getClusterId());
            }
          }
        }

        // Outgoing asPath
        // Outgoing communities
        if (routeIsBgp) {
          BgpRoute bgpRoute = (BgpRoute) route;
          transformedOutgoingRouteBuilder.setAsPath(bgpRoute.getAsPath().getAsSets());
          if (neighbor.getSendCommunity()) {
            transformedOutgoingRouteBuilder.getCommunities().addAll(bgpRoute.getCommunities());
          }
        }
        if (ebgpSession) {
          SortedSet<Integer> newAsPathElement = new TreeSet<>();
          newAsPathElement.add(localAs);
          transformedOutgoingRouteBuilder.getAsPath().add(0, newAsPathElement);
        }

        // Outgoing protocol
        transformedOutgoingRouteBuilder.setProtocol(targetProtocol);
        transformedOutgoingRouteBuilder.setNetwork(route.getNetwork());

        // Outgoing metric
        if (routeIsBgp) {
          transformedOutgoingRouteBuilder.setMetric(route.getMetric());
        }

        // Outgoing nextHopIp
        // Outgoing localPreference
        Ip nextHopIp;
        int localPreference;
        if (ebgpSession || !routeIsBgp) {
          nextHopIp = neighbor.getLocalIp();
          localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
        } else {
          nextHopIp = route.getNextHopIp();
          BgpRoute ibgpRoute = (BgpRoute) route;
          localPreference = ibgpRoute.getLocalPreference();
        }
        if (nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
          // should only happen for ibgp
          String nextHopInterface = route.getNextHopInterface();
          Prefix nextHopPrefix = _c.getInterfaces().get(nextHopInterface).getAddress();
          if (nextHopPrefix == null) {
            throw new BatfishException("route's nextHopInterface has no address");
          }
          nextHopIp = nextHopPrefix.getAddress();
        }
        transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
        transformedOutgoingRouteBuilder.setLocalPreference(localPreference);

        // Outgoing srcProtocol
        transformedOutgoingRouteBuilder.setSrcProtocol(route.getProtocol());

        /*
         * CREATE OUTGOING ROUTE
         */
        boolean acceptOutgoing =
            exportPolicy.process(
                route, transformedOutgoingRouteBuilder, remoteIp, remoteVrfName, Direction.OUT);
        if (acceptOutgoing) {
          BgpRoute transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
          // Record sent advertisement
          BgpAdvertisementType sentType =
              ebgpSession ? BgpAdvertisementType.EBGP_SENT : BgpAdvertisementType.IBGP_SENT;
          Ip sentOriginatorIp = transformedOutgoingRoute.getOriginatorIp();
          SortedSet<Long> sentClusterList = transformedOutgoingRoute.getClusterList();
          AsPath sentAsPath = transformedOutgoingRoute.getAsPath();
          SortedSet<Long> sentCommunities = transformedOutgoingRoute.getCommunities();
          Prefix sentNetwork = route.getNetwork();
          Ip sentNextHopIp;
          String sentSrcNode = hostname;
          String sentSrcVrf = _vrf.getName();
          Ip sentSrcIp = neighbor.getLocalIp();
          String sentDstNode = remoteHostname;
          String sentDstVrf = remoteVrfName;
          Ip sentDstIp = remoteIp;
          int sentWeight = -1;
          if (ebgpSession) {
            sentNextHopIp = nextHopIp;
          } else {
            sentNextHopIp = transformedOutgoingRoute.getNextHopIp();
          }
          int sentLocalPreference = transformedOutgoingRoute.getLocalPreference();
          long sentMed = transformedOutgoingRoute.getMetric();
          OriginType sentOriginType = transformedOutgoingRoute.getOriginType();
          RoutingProtocol sentSrcProtocol = targetProtocol;
          BgpAdvertisement sentAdvert =
              new BgpAdvertisement(
                  sentType,
                  sentNetwork,
                  sentNextHopIp,
                  sentSrcNode,
                  sentSrcVrf,
                  sentSrcIp,
                  sentDstNode,
                  sentDstVrf,
                  sentDstIp,
                  sentSrcProtocol,
                  sentOriginType,
                  sentLocalPreference,
                  sentMed,
                  sentOriginatorIp,
                  sentAsPath,
                  sentCommunities,
                  sentClusterList,
                  sentWeight);
          _sentBgpAdvertisements.add(sentAdvert);
          numAdvertisements++;
        }
      }
    }
    return numAdvertisements;
  }

  int propagateBgpRoutes(
      Map<Ip, Set<String>> ipOwners,
      int dependentRoutesIterations,
      SortedSet<Prefix> oscillatingPrefixes,
      Map<String, Node> nodes) {

    int numRoutes = 0;
    _receivedBgpAdvertisements = new LinkedHashSet<>();
    _prevSentBgpAdvertisements =
        _sentBgpAdvertisements != null ? _sentBgpAdvertisements : new LinkedHashSet<>();
    _sentBgpAdvertisements = new LinkedHashSet<>();

    // If we have no BGP process, nothing to do
    if (_vrf.getBgpProcess() == null) {
      return numRoutes;
    }

    int ebgpAdminCost =
        RoutingProtocol.BGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int ibgpAdminCost =
        RoutingProtocol.IBGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());

    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      Ip localIp = neighbor.getLocalIp();
      Set<String> localIpOwners = ipOwners.get(localIp);
      String hostname = _c.getHostname();
      if (localIpOwners == null || !localIpOwners.contains(hostname)) {
        continue;
      }

      BgpNeighbor remoteBgpNeighbor = neighbor.getRemoteBgpNeighbor();
      if (remoteBgpNeighbor == null) {
        continue;
      }
      int localAs = neighbor.getLocalAs();
      int remoteAs = neighbor.getRemoteAs();
      Configuration remoteConfig = remoteBgpNeighbor.getOwner();
      String remoteHostname = remoteConfig.getHostname();
      String remoteVrfName = remoteBgpNeighbor.getVrf();
      Vrf remoteVrf = remoteConfig.getVrfs().get(remoteVrfName);
      VirtualRouter remoteVirtualRouter =
          nodes.get(remoteHostname)._virtualRouters.get(remoteVrfName);
      RoutingPolicy remoteExportPolicy =
          remoteConfig.getRoutingPolicies().get(remoteBgpNeighbor.getExportPolicy());
      boolean ebgpSession = localAs != remoteAs;
      BgpMultipathRib targetRib = ebgpSession ? _ebgpStagingRib : _ibgpStagingRib;
      RoutingProtocol targetProtocol = ebgpSession ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
      Set<AbstractRoute> remoteCandidateRoutes = Collections.newSetFromMap(new IdentityHashMap<>());

      // Add IGP routes
      Set<AbstractRoute> activeRemoteRoutes = Collections.newSetFromMap(new IdentityHashMap<>());
      activeRemoteRoutes.addAll(remoteVirtualRouter._prevMainRib.getRoutes());
      for (AbstractRoute remoteCandidateRoute : activeRemoteRoutes) {
        if (remoteCandidateRoute.getProtocol() != RoutingProtocol.BGP
            && remoteCandidateRoute.getProtocol() != RoutingProtocol.IBGP) {
          remoteCandidateRoutes.add(remoteCandidateRoute);
        }
      }

      /*
       * bgp advertise-external
       *
       * When this is set, add best eBGP path independently of whether
       * it is preempted by an iBGP or IGP route. Only applicable to
       * iBGP sessions.
       */
      boolean advertiseExternal = !ebgpSession && remoteBgpNeighbor.getAdvertiseExternal();
      if (advertiseExternal) {
        remoteCandidateRoutes.addAll(remoteVirtualRouter._prevEbgpBestPathRib.getRoutes());
      }

      /*
       * bgp advertise-inactive
       *
       * When this is set, add best BGP path independently of whether
       * it is preempted by an IGP route. Only applicable to eBGP
       * sessions.
       */
      boolean advertiseInactive = ebgpSession && remoteBgpNeighbor.getAdvertiseInactive();
      /* Add best bgp paths if they are active, or if advertise-inactive */
      for (AbstractRoute remoteCandidateRoute :
          remoteVirtualRouter._prevBgpBestPathRib.getRoutes()) {
        if (advertiseInactive || activeRemoteRoutes.contains(remoteCandidateRoute)) {
          remoteCandidateRoutes.add(remoteCandidateRoute);
        }
      }

      /* Add all bgp paths if additional-paths active for this session */
      boolean additionalPaths =
          !ebgpSession
              && neighbor.getAdditionalPathsReceive()
              && remoteBgpNeighbor.getAdditionalPathsSend()
              && remoteBgpNeighbor.getAdditionalPathsSelectAll();
      if (additionalPaths) {
        remoteCandidateRoutes.addAll(remoteVirtualRouter._prevBgpMultipathRib.getRoutes());
      }
      for (AbstractRoute remoteRoute : remoteCandidateRoutes) {
        BgpRoute.Builder transformedOutgoingRouteBuilder = new BgpRoute.Builder();
        RoutingProtocol remoteRouteProtocol = remoteRoute.getProtocol();
        boolean remoteRouteIsBgp =
            remoteRouteProtocol == RoutingProtocol.IBGP
                || remoteRouteProtocol == RoutingProtocol.BGP;

        // originatorIP
        Ip originatorIp;
        if (!ebgpSession && remoteRouteProtocol.equals(RoutingProtocol.IBGP)) {
          BgpRoute bgpRemoteRoute = (BgpRoute) remoteRoute;
          originatorIp = bgpRemoteRoute.getOriginatorIp();
        } else {
          originatorIp = remoteVrf.getBgpProcess().getRouterId();
        }
        transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);

        // clusterList, receivedFromRouteReflectorClient, (originType
        // for bgp remote route)
        if (remoteRouteIsBgp) {
          BgpRoute bgpRemoteRoute = (BgpRoute) remoteRoute;
          transformedOutgoingRouteBuilder.setOriginType(bgpRemoteRoute.getOriginType());
          if (ebgpSession
              && bgpRemoteRoute.getAsPath().containsAs(remoteBgpNeighbor.getRemoteAs())
              && !remoteBgpNeighbor.getAllowRemoteAsOut()) {
            // skip routes containing peer's AS unless
            // disable-peer-as-check (getAllowRemoteAsOut) is set
            continue;
          }
          /*
           * route reflection: reflect everything received from
           * clients to clients and non-clients. reflect everything
           * received from non-clients to clients. Do not reflect to
           * originator
           */

          Ip remoteOriginatorIp = bgpRemoteRoute.getOriginatorIp();
          /*
           *  iBGP speaker should not send out routes to iBGP neighbor whose router-id is
           *  same as originator id of advertisement
           */
          if (!ebgpSession
              && remoteOriginatorIp != null
              && _vrf.getBgpProcess().getRouterId().equals(remoteOriginatorIp)) {
            continue;
          }
          if (remoteRouteProtocol.equals(RoutingProtocol.IBGP) && !ebgpSession) {
            boolean remoteRouteReceivedFromRouteReflectorClient =
                bgpRemoteRoute.getReceivedFromRouteReflectorClient();
            boolean sendingToRouteReflectorClient = remoteBgpNeighbor.getRouteReflectorClient();
            boolean newRouteReceivedFromRouteReflectorClient = neighbor.getRouteReflectorClient();
            transformedOutgoingRouteBuilder.setReceivedFromRouteReflectorClient(
                newRouteReceivedFromRouteReflectorClient);
            transformedOutgoingRouteBuilder
                .getClusterList()
                .addAll(bgpRemoteRoute.getClusterList());
            if (!remoteRouteReceivedFromRouteReflectorClient && !sendingToRouteReflectorClient) {
              continue;
            }
            if (sendingToRouteReflectorClient) {
              // sender adds its local cluster id to clusterlist of
              // new route
              transformedOutgoingRouteBuilder
                  .getClusterList()
                  .add(remoteBgpNeighbor.getClusterId());
            }
            if (transformedOutgoingRouteBuilder
                .getClusterList()
                .contains(neighbor.getClusterId())) {
              // receiver will reject new route if it contains its
              // local cluster id
              continue;
            }
          }
        }

        // Outgoing asPath
        // Outgoing communities
        if (remoteRouteIsBgp) {
          BgpRoute bgpRemoteRoute = (BgpRoute) remoteRoute;
          transformedOutgoingRouteBuilder.setAsPath(bgpRemoteRoute.getAsPath().getAsSets());
          if (remoteBgpNeighbor.getSendCommunity()) {
            transformedOutgoingRouteBuilder
                .getCommunities()
                .addAll(bgpRemoteRoute.getCommunities());
          }
        }
        if (ebgpSession) {
          SortedSet<Integer> newAsPathElement = new TreeSet<>();
          newAsPathElement.add(remoteAs);
          transformedOutgoingRouteBuilder.getAsPath().add(0, newAsPathElement);
        }

        // Outgoing protocol
        transformedOutgoingRouteBuilder.setProtocol(targetProtocol);
        transformedOutgoingRouteBuilder.setNetwork(remoteRoute.getNetwork());

        // Outgoing metric
        if (remoteRouteIsBgp) {
          transformedOutgoingRouteBuilder.setMetric(remoteRoute.getMetric());
        }

        // Outgoing nextHopIp
        // Outgoing localPreference
        Ip nextHopIp;
        int localPreference;
        if (ebgpSession || !remoteRouteIsBgp) {
          nextHopIp = remoteBgpNeighbor.getLocalIp();
          localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
        } else {
          nextHopIp = remoteRoute.getNextHopIp();
          BgpRoute remoteIbgpRoute = (BgpRoute) remoteRoute;
          localPreference = remoteIbgpRoute.getLocalPreference();
        }
        if (nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
          // should only happen for ibgp
          String nextHopInterface = remoteRoute.getNextHopInterface();
          Prefix nextHopPrefix = remoteVrf.getInterfaces().get(nextHopInterface).getAddress();
          if (nextHopPrefix == null) {
            throw new BatfishException("remote route's nextHopInterface has no address");
          }
          nextHopIp = nextHopPrefix.getAddress();
        }
        transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
        transformedOutgoingRouteBuilder.setLocalPreference(localPreference);

        // Outgoing srcProtocol
        transformedOutgoingRouteBuilder.setSrcProtocol(remoteRoute.getProtocol());

        /*
         * CREATE OUTGOING ROUTE
         */
        boolean acceptOutgoing =
            remoteExportPolicy.process(
                remoteRoute,
                transformedOutgoingRouteBuilder,
                localIp,
                remoteVrfName,
                Direction.OUT);
        if (acceptOutgoing) {
          BgpRoute transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
          // Record sent advertisement
          BgpAdvertisementType sentType =
              ebgpSession ? BgpAdvertisementType.EBGP_SENT : BgpAdvertisementType.IBGP_SENT;
          Ip sentOriginatorIp = transformedOutgoingRoute.getOriginatorIp();
          SortedSet<Long> sentClusterList = transformedOutgoingRoute.getClusterList();
          boolean sentReceivedFromRouteReflectorClient =
              transformedOutgoingRoute.getReceivedFromRouteReflectorClient();
          AsPath sentAsPath = transformedOutgoingRoute.getAsPath();
          SortedSet<Long> sentCommunities = transformedOutgoingRoute.getCommunities();
          Prefix sentNetwork = remoteRoute.getNetwork();
          Ip sentNextHopIp;
          String sentSrcNode = remoteHostname;
          String sentSrcVrf = remoteVrfName;
          Ip sentSrcIp = remoteBgpNeighbor.getLocalIp();
          String sentDstNode = hostname;
          String sentDstVrf = _vrf.getName();
          Ip sentDstIp = neighbor.getLocalIp();
          int sentWeight = -1;
          if (ebgpSession) {
            sentNextHopIp = nextHopIp;
          } else {
            sentNextHopIp = transformedOutgoingRoute.getNextHopIp();
          }
          int sentLocalPreference = transformedOutgoingRoute.getLocalPreference();
          long sentMed = transformedOutgoingRoute.getMetric();
          OriginType sentOriginType = transformedOutgoingRoute.getOriginType();
          RoutingProtocol sentSrcProtocol = targetProtocol;
          BgpRoute.Builder transformedIncomingRouteBuilder = new BgpRoute.Builder();

          // Incoming originatorIp
          transformedIncomingRouteBuilder.setOriginatorIp(sentOriginatorIp);

          // Incoming clusterList
          transformedIncomingRouteBuilder.getClusterList().addAll(sentClusterList);

          // Incoming receivedFromRouteReflectorClient
          transformedIncomingRouteBuilder.setReceivedFromRouteReflectorClient(
              sentReceivedFromRouteReflectorClient);

          // Incoming asPath
          transformedIncomingRouteBuilder.setAsPath(sentAsPath.getAsSets());

          // Incoming communities
          transformedIncomingRouteBuilder.getCommunities().addAll(sentCommunities);

          // Incoming protocol
          transformedIncomingRouteBuilder.setProtocol(targetProtocol);

          // Incoming network
          transformedIncomingRouteBuilder.setNetwork(sentNetwork);

          // Incoming nextHopIp
          transformedIncomingRouteBuilder.setNextHopIp(sentNextHopIp);

          // Incoming localPreference
          transformedIncomingRouteBuilder.setLocalPreference(sentLocalPreference);

          // Incoming admin
          int admin = ebgpSession ? ebgpAdminCost : ibgpAdminCost;
          transformedIncomingRouteBuilder.setAdmin(admin);

          // Incoming metric
          transformedIncomingRouteBuilder.setMetric(sentMed);

          // Incoming originType
          transformedIncomingRouteBuilder.setOriginType(sentOriginType);

          // Incoming srcProtocol
          transformedIncomingRouteBuilder.setSrcProtocol(sentSrcProtocol);
          String importPolicyName = neighbor.getImportPolicy();
          // TODO: ensure there is always an import policy

          if (transformedOutgoingRoute.getAsPath().containsAs(neighbor.getLocalAs())
              && !neighbor.getAllowLocalAsIn()) {
            // skip routes containing peer's AS unless
            // disable-peer-as-check (getAllowRemoteAsOut) is set
            continue;
          }

          BgpAdvertisement sentAdvert =
              new BgpAdvertisement(
                  sentType,
                  sentNetwork,
                  sentNextHopIp,
                  sentSrcNode,
                  sentSrcVrf,
                  sentSrcIp,
                  sentDstNode,
                  sentDstVrf,
                  sentDstIp,
                  sentSrcProtocol,
                  sentOriginType,
                  sentLocalPreference,
                  sentMed,
                  sentOriginatorIp,
                  sentAsPath,
                  sentCommunities,
                  sentClusterList,
                  sentWeight);

          Prefix prefix = remoteRoute.getNetwork();
          boolean isOscillatingPrefix = oscillatingPrefixes.contains(prefix);
          boolean hasAdvertisementPriorityDuringRecovery =
              hasAdvertisementPriorityDuringRecovery(
                  remoteRoute,
                  dependentRoutesIterations,
                  oscillatingPrefixes,
                  neighbor,
                  remoteBgpNeighbor);
          if (isOscillatingPrefix
              && !hasAdvertisementPriorityDuringRecovery
              && !_prevSentBgpAdvertisements.contains(sentAdvert)) {
            continue;
          }
          _sentBgpAdvertisements.add(sentAdvert);

          /*
           * CREATE INCOMING ROUTE
           */
          boolean acceptIncoming = true;
          if (importPolicyName != null) {
            RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
            if (importPolicy != null) {
              acceptIncoming =
                  importPolicy.process(
                      transformedOutgoingRoute,
                      transformedIncomingRouteBuilder,
                      remoteBgpNeighbor.getLocalIp(),
                      _key,
                      Direction.IN);
            }
          }
          if (acceptIncoming) {
            BgpRoute transformedIncomingRoute = transformedIncomingRouteBuilder.build();
            BgpAdvertisementType receivedType =
                ebgpSession
                    ? BgpAdvertisementType.EBGP_RECEIVED
                    : BgpAdvertisementType.IBGP_RECEIVED;
            Prefix receivedNetwork = sentNetwork;
            Ip receivedNextHopIp = sentNextHopIp;
            String receivedSrcNode = sentSrcNode;
            String receivedSrcVrf = sentSrcVrf;
            Ip receivedSrcIp = sentSrcIp;
            String receivedDstNode = sentDstNode;
            String receivedDstVrf = sentDstVrf;
            Ip receivedDstIp = sentDstIp;
            RoutingProtocol receivedSrcProtocol = sentSrcProtocol;
            OriginType receivedOriginType = transformedIncomingRoute.getOriginType();
            int receivedLocalPreference = transformedIncomingRoute.getLocalPreference();
            long receivedMed = transformedIncomingRoute.getMetric();
            Ip receivedOriginatorIp = sentOriginatorIp;
            AsPath receivedAsPath = transformedIncomingRoute.getAsPath();
            SortedSet<Long> receivedCommunities = transformedIncomingRoute.getCommunities();
            SortedSet<Long> receivedClusterList = sentClusterList;
            int receivedWeight = transformedIncomingRoute.getWeight();
            BgpAdvertisement receivedAdvert =
                new BgpAdvertisement(
                    receivedType,
                    receivedNetwork,
                    receivedNextHopIp,
                    receivedSrcNode,
                    receivedSrcVrf,
                    receivedSrcIp,
                    receivedDstNode,
                    receivedDstVrf,
                    receivedDstIp,
                    receivedSrcProtocol,
                    receivedOriginType,
                    receivedLocalPreference,
                    receivedMed,
                    receivedOriginatorIp,
                    receivedAsPath,
                    receivedCommunities,
                    receivedClusterList,
                    receivedWeight);
            if (targetRib.mergeRoute(transformedIncomingRoute)) {
              numRoutes++;
            }
            _receivedBgpAdvertisements.add(receivedAdvert);
          }
        }
      }
    }
    return numRoutes;
  }

  public boolean propagateOspfExternalRoutes(Map<String, Node> nodes, Topology topology) {
    boolean changed = false;
    String node = _c.getHostname();
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc != null) {
      int admin = RoutingProtocol.OSPF.getDefaultAdministrativeCost(_c.getConfigurationFormat());
      SortedSet<Edge> edges = topology.getNodeEdges().get(node);
      if (edges == null) {
        // there are no edges, so OSPF won't produce anything
        return false;
      }
      for (Edge edge : edges) {
        if (!edge.getNode1().equals(node)) {
          continue;
        }
        String connectingInterfaceName = edge.getInt1();
        Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
        if (connectingInterface == null) {
          // wrong vrf, so skip
          continue;
        }
        String neighborName = edge.getNode2();
        Node neighbor = nodes.get(neighborName);
        String neighborInterfaceName = edge.getInt2();
        OspfArea area = connectingInterface.getOspfArea();
        Configuration nc = neighbor._c;
        Interface neighborInterface = nc.getInterfaces().get(neighborInterfaceName);
        String neighborVrfName = neighborInterface.getVrfName();
        VirtualRouter neighborVirtualRouter =
            nodes.get(neighborName)._virtualRouters.get(neighborVrfName);

        OspfArea neighborArea = neighborInterface.getOspfArea();
        if (connectingInterface.getOspfEnabled()
            && !connectingInterface.getOspfPassive()
            && neighborInterface.getOspfEnabled()
            && !neighborInterface.getOspfPassive()
            && area != null
            && neighborArea != null
            && area.getName().equals(neighborArea.getName())) {
          /*
           * We have an ospf neighbor relationship on this edge. So we
           * should add all ospf external type 1(2) routes from this
           * neighbor into our ospf external type 1(2) staging rib. For
           * type 1, the cost of the route increases each time. For type 2,
           * the cost remains constant, but we must keep track of cost to
           * advertiser as a tie-breaker.
           */
          long connectingInterfaceCost = connectingInterface.getOspfCost();
          long incrementalCost =
              proc.getMaxMetricTransitLinks() != null
                  ? proc.getMaxMetricTransitLinks()
                  : connectingInterfaceCost;
          for (OspfExternalType1Route neighborRoute :
              neighborVirtualRouter._prevOspfExternalType1Rib.getRoutes()) {
            long oldArea = neighborRoute.getArea();
            long connectionArea = area.getName();
            long newArea;
            long baseMetric = neighborRoute.getMetric();
            long baseCostToAdvertiser = neighborRoute.getCostToAdvertiser();
            newArea = connectionArea;
            if (oldArea != OspfRoute.NO_AREA) {
              Long maxMetricSummaryNetworks =
                  neighborVirtualRouter._vrf.getOspfProcess().getMaxMetricSummaryNetworks();
              if (connectionArea != oldArea) {
                if (connectionArea != 0L && oldArea != 0L) {
                  continue;
                }
                if (maxMetricSummaryNetworks != null) {
                  baseMetric = maxMetricSummaryNetworks + neighborRoute.getLsaMetric();
                  baseCostToAdvertiser = maxMetricSummaryNetworks;
                }
              }
            }
            long newMetric = baseMetric + incrementalCost;
            long newCostToAdvertiser = baseCostToAdvertiser + incrementalCost;
            OspfExternalType1Route newRoute =
                new OspfExternalType1Route(
                    neighborRoute.getNetwork(),
                    neighborInterface.getAddress().getAddress(),
                    admin,
                    newMetric,
                    neighborRoute.getLsaMetric(),
                    newArea,
                    newCostToAdvertiser,
                    neighborRoute.getAdvertiser());
            if (_ospfExternalType1StagingRib.mergeRoute(newRoute)) {
              changed = true;
            }
          }
          for (OspfExternalType2Route neighborRoute :
              neighborVirtualRouter._prevOspfExternalType2Rib.getRoutes()) {
            long oldArea = neighborRoute.getArea();
            long connectionArea = area.getName();
            long newArea;
            long baseCostToAdvertiser = neighborRoute.getCostToAdvertiser();
            if (oldArea == OspfRoute.NO_AREA) {
              newArea = connectionArea;
            } else {
              newArea = oldArea;
              Long maxMetricSummaryNetworks =
                  neighborVirtualRouter._vrf.getOspfProcess().getMaxMetricSummaryNetworks();
              if (connectionArea != oldArea && maxMetricSummaryNetworks != null) {
                baseCostToAdvertiser = maxMetricSummaryNetworks;
              }
            }
            long newCostToAdvertiser = baseCostToAdvertiser + incrementalCost;
            OspfExternalType2Route newRoute =
                new OspfExternalType2Route(
                    neighborRoute.getNetwork(),
                    neighborInterface.getAddress().getAddress(),
                    admin,
                    neighborRoute.getMetric(),
                    neighborRoute.getLsaMetric(),
                    newArea,
                    newCostToAdvertiser,
                    neighborRoute.getAdvertiser());
            if (_ospfExternalType2StagingRib.mergeRoute(newRoute)) {
              changed = true;
            }
          }
        }
      }
    }
    return changed;
  }

  /**
   * Construct an OSPF Inter-Area route and put into our staging rib. Note, no route validity checks
   * are performed, (i.e., whether the route should even go into the staging rib). {@link
   * #propagateOspfInternalRoutesFromNeighbor} takes care of such logic.
   *
   * @param neighborRoute the route to propagate
   * @param nextHopIp nextHopIp for this route (the neighbor's IP)
   * @param incrementalCost OSPF cost of the interface from which this route came (added to route
   *     cost)
   * @param adminCost OSPF administrative distance
   * @param areaNum area number of the route
   * @return True if the route was added to the inter-area staging RIB
   */
  @VisibleForTesting
  boolean stageOspfInterAreaRoute(
      OspfInternalRoute neighborRoute,
      Long maxMetricSummaryNetworks,
      Ip nextHopIp,
      long incrementalCost,
      int adminCost,
      long areaNum) {
    long newCost;
    if (maxMetricSummaryNetworks != null) {
      newCost = maxMetricSummaryNetworks + incrementalCost;
    } else {
      newCost = neighborRoute.getMetric() + incrementalCost;
    }
    OspfInterAreaRoute newRoute =
        new OspfInterAreaRoute(neighborRoute.getNetwork(), nextHopIp, adminCost, newCost, areaNum);
    return _ospfInterAreaStagingRib.mergeRoute(newRoute);
  }

  private static boolean isOspfInterAreaFromInterAreaPropagationAllowed(
      long areaNum, Node neighbor, OspfInternalRoute neighborRoute, OspfArea neighborArea) {
    long neighborRouteAreaNum = neighborRoute.getArea();
    // May only propagate to or from area 0
    if (areaNum != neighborRouteAreaNum && areaNum != 0L && neighborRouteAreaNum != 0L) {
      return false;
    }
    Prefix neighborRouteNetwork = neighborRoute.getNetwork();
    String neighborSummaryFilterName = neighborArea.getSummaryFilter();
    boolean hasSummaryFilter = neighborSummaryFilterName != null;
    boolean allowed = !hasSummaryFilter;

    // If there is a summary filter, run the route through it
    if (hasSummaryFilter) {
      RouteFilterList neighborSummaryFilter =
          neighbor._c.getRouteFilterLists().get(neighborSummaryFilterName);
      allowed = neighborSummaryFilter.permits(neighborRouteNetwork);
    }
    return allowed;
  }

  private static boolean isOspfInterAreaFromIntraAreaPropagationAllowed(
      long areaNum, Node neighbor, OspfInternalRoute neighborRoute, OspfArea neighborArea) {
    long neighborRouteAreaNum = neighborRoute.getArea();
    // May only propagate to or from area 0
    if (areaNum == neighborRouteAreaNum || (areaNum != 0L && neighborRouteAreaNum != 0L)) {
      return false;
    }
    Prefix neighborRouteNetwork = neighborRoute.getNetwork();
    String neighborSummaryFilterName = neighborArea.getSummaryFilter();
    boolean hasSummaryFilter = neighborSummaryFilterName != null;
    boolean allowed = !hasSummaryFilter;

    // If there is a summary filter, run the route through it
    if (hasSummaryFilter) {
      RouteFilterList neighborSummaryFilter =
          neighbor._c.getRouteFilterLists().get(neighborSummaryFilterName);
      allowed = neighborSummaryFilter.permits(neighborRouteNetwork);
    }
    return allowed;
  }

  boolean propagateOspfInterAreaRouteFromIntraAreaRoute(
      Node neighbor,
      OspfIntraAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long areaNum) {
    return isOspfInterAreaFromIntraAreaPropagationAllowed(
            areaNum, neighbor, neighborRoute, neighborInterface.getOspfArea())
        && stageOspfInterAreaRoute(
            neighborRoute,
            neighborInterface.getVrf().getOspfProcess().getMaxMetricSummaryNetworks(),
            neighborInterface.getAddress().getAddress(),
            incrementalCost,
            adminCost,
            areaNum);
  }

  /**
   * Propagate OSPF Internal routes from a single neighbor.
   *
   * @param proc The receiving OSPF process
   * @param neighbor the neighbor
   * @param connectingInterface interface on which we are connected to the neighbor
   * @param neighborInterface interface that the neighbor uses to connect to us
   * @param adminCost route administrative distance
   * @return true if new routes have been added to our staging RIB
   */
  boolean propagateOspfInternalRoutesFromNeighbor(
      OspfProcess proc,
      Node neighbor,
      Interface connectingInterface,
      Interface neighborInterface,
      int adminCost) {
    OspfArea area = connectingInterface.getOspfArea();
    OspfArea neighborArea = neighborInterface.getOspfArea();
    // Ensure that the link (i.e., both interfaces) has OSPF enabled and OSPF areas are set
    if (!connectingInterface.getOspfEnabled()
        || connectingInterface.getOspfPassive()
        || !neighborInterface.getOspfEnabled()
        || neighborInterface.getOspfPassive()
        || area == null
        || neighborArea == null
        || !area.getName().equals(neighborArea.getName())) {
      return false;
    }
    /*
     * An OSPF neighbor relationship exists on this edge. So we examine all intra- and inter-area
     * routes belonging to the neighbor to see what should be propagated to this router. We add the
     * incremental cost associated with our settings and the connecting interface, and use the
     * neighborInterface's address as the next hop ip.
     */
    int connectingInterfaceCost = connectingInterface.getOspfCost();
    long incrementalCost =
        proc.getMaxMetricTransitLinks() != null
            ? proc.getMaxMetricTransitLinks()
            : connectingInterfaceCost;
    Long areaNum = area.getName();
    VirtualRouter neighborVirtualRouter =
        neighbor._virtualRouters.get(neighborInterface.getVrfName());
    boolean changed = false;
    for (OspfIntraAreaRoute neighborRoute : neighborVirtualRouter._ospfIntraAreaRib.getRoutes()) {
      changed |=
          propagateOspfIntraAreaRoute(
              neighborRoute, incrementalCost, neighborInterface, adminCost, areaNum);
      changed |=
          propagateOspfInterAreaRouteFromIntraAreaRoute(
              neighbor, neighborRoute, incrementalCost, neighborInterface, adminCost, areaNum);
    }
    for (OspfInterAreaRoute neighborRoute : neighborVirtualRouter._ospfInterAreaRib.getRoutes()) {
      changed |=
          propagateOspfInterAreaRouteFromInterAreaRoute(
              neighbor, neighborRoute, incrementalCost, neighborInterface, adminCost, areaNum);
    }
    return changed;
  }

  boolean propagateOspfInterAreaRouteFromInterAreaRoute(
      Node neighbor,
      OspfInterAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long areaNum) {
    return isOspfInterAreaFromInterAreaPropagationAllowed(
            areaNum, neighbor, neighborRoute, neighborInterface.getOspfArea())
        && stageOspfInterAreaRoute(
            neighborRoute,
            neighborInterface.getVrf().getOspfProcess().getMaxMetricSummaryNetworks(),
            neighborInterface.getAddress().getAddress(),
            incrementalCost,
            adminCost,
            areaNum);
  }

  boolean propagateOspfIntraAreaRoute(
      OspfIntraAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long areaNum) {
    long newCost = neighborRoute.getMetric() + incrementalCost;
    Ip nextHopIp = neighborInterface.getAddress().getAddress();
    OspfIntraAreaRoute newRoute =
        new OspfIntraAreaRoute(neighborRoute.getNetwork(), nextHopIp, adminCost, newCost, areaNum);
    return neighborRoute.getArea() == areaNum && _ospfIntraAreaStagingRib.mergeRoute(newRoute);
  }

  /**
   * Propagate OSPF internal routes from every valid OSPF neighbor
   *
   * @param nodes mapping of node names to instances.
   * @param topology network topology
   * @return true if new routes have been added to the staging RIB
   */
  boolean propagateOspfInternalRoutes(Map<String, Node> nodes, Topology topology) {
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return false; // nothing to do
    }

    boolean changed = false;
    String node = _c.getHostname();

    // Default OSPF admin cost for constructing new routes
    int adminCost = RoutingProtocol.OSPF.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so OSPF won't produce anything
      return false;
    }

    for (Edge edge : edges) {
      if (!edge.getNode1().equals(node)) {
        continue;
      }

      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }

      String neighborName = edge.getNode2();
      Node neighbor = nodes.get(neighborName);
      Interface neighborInterface = neighbor._c.getInterfaces().get(edge.getInt2());

      changed |=
          propagateOspfInternalRoutesFromNeighbor(
              proc, neighbor, connectingInterface, neighborInterface, adminCost);
    }
    return changed;
  }

  /**
   * Process RIP routes from our neighbors.
   *
   * @param nodes Mapping of node names to Node instances
   * @param topology The network topology
   * @return True if the rib has changed as a result of route propagation
   */
  boolean propagateRipInternalRoutes(Map<String, Node> nodes, Topology topology) {
    boolean changed = false;

    // No rip process, nothing to do
    if (_vrf.getRipProcess() == null) {
      return false;
    }

    String node = _c.getHostname();
    int admin = RoutingProtocol.RIP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so RIP won't produce anything
      return false;
    }

    for (Edge edge : edges) {
      // Do not accept routes from ourselves
      if (!edge.getNode1().equals(node)) {
        continue;
      }

      // Get interface
      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }

      // Get the neighbor and its interface + VRF
      String neighborName = edge.getNode2();
      Node neighbor = nodes.get(neighborName);
      String neighborInterfaceName = edge.getInt2();
      Interface neighborInterface = neighbor._c.getInterfaces().get(neighborInterfaceName);
      String neighborVrfName = neighborInterface.getVrfName();
      VirtualRouter neighborVirtualRouter =
          nodes.get(neighborName)._virtualRouters.get(neighborVrfName);

      if (connectingInterface.getRipEnabled()
          && !connectingInterface.getRipPassive()
          && neighborInterface.getRipEnabled()
          && !neighborInterface.getRipPassive()) {
        /*
         * We have a RIP neighbor relationship on this edge. So we should add all RIP routes
         * from this neighbor into our RIP internal staging rib, adding the incremental cost
         * (?), and using the neighborInterface's address as the next hop ip
         */
        for (RipInternalRoute neighborRoute : neighborVirtualRouter._ripInternalRib.getRoutes()) {
          long newCost = neighborRoute.getMetric() + RipProcess.DEFAULT_RIP_COST;
          Ip nextHopIp = neighborInterface.getAddress().getAddress();
          RipInternalRoute newRoute =
              new RipInternalRoute(neighborRoute.getNetwork(), nextHopIp, admin, newCost);
          if (_ripInternalStagingRib.mergeRoute(newRoute)) {
            changed = true;
          }
        }
      }
    }
    return changed;
  }

  public void finalizeBgpRoutes(boolean multipathEbgp, boolean multipathIbgp) {
    // Best-path RIBs
    importRib(_ebgpBestPathRib, _ebgpStagingRib);
    importRib(_ibgpBestPathRib, _ibgpStagingRib);
    importRib(_bgpBestPathRib, _ebgpBestPathRib);
    importRib(_bgpBestPathRib, _ibgpBestPathRib);

    // Multi-path RIBs
    _bgpMultipathRib.setBestAsPaths(_bgpBestPathRib.getBestAsPaths());
    if (multipathEbgp) {
      importRib(_ebgpMultipathRib, _ebgpStagingRib);
      importRib(_bgpMultipathRib, _ebgpMultipathRib);
    } else {
      importRib(_bgpMultipathRib, _ebgpBestPathRib);
    }
    if (multipathIbgp) {
      importRib(_ibgpMultipathRib, _ibgpStagingRib);
      importRib(_bgpMultipathRib, _ibgpMultipathRib);
    } else {
      importRib(_bgpMultipathRib, _ibgpBestPathRib);
    }
    importRib(_mainRib, _bgpMultipathRib);
  }

  /** Merges staged OSPF external routes into the "real" OSPF-external RIBs */
  void unstageOspfExternalRoutes() {
    importRib(_ospfExternalType1Rib, _ospfExternalType1StagingRib);
    importRib(_ospfExternalType2Rib, _ospfExternalType2StagingRib);
  }

  /** Merges staged OSPF internal routes into the "real" OSPF-internal RIBs */
  void unstageOspfInternalRoutes() {
    importRib(_ospfIntraAreaRib, _ospfIntraAreaStagingRib);
    importRib(_ospfInterAreaRib, _ospfInterAreaStagingRib);
  }

  /** Merges staged RIP routes into the "real" RIP RIB */
  void unstageRipInternalRoutes() {
    importRib(_ripInternalRib, _ripInternalStagingRib);
  }

  /**
   * For RIBs where we need to keep previous iteration for comparison. Update references of _prev*
   * RIBs. Also makes previous ribs unmodifiable.
   */
  void moveRibs() {
    _prevMainRib = _mainRib;
    _mainRib = new Rib(this);

    _prevOspfExternalType1Rib = _ospfExternalType1Rib;
    _ospfExternalType1Rib = new OspfExternalType1Rib(this);

    _prevOspfExternalType2Rib = _ospfExternalType2Rib;
    _ospfExternalType2Rib = new OspfExternalType2Rib(this);

    _prevBgpMultipathRib = _bgpMultipathRib;
    _bgpMultipathRib = new BgpMultipathRib(this);

    _prevBgpBestPathRib = _bgpBestPathRib;
    _bgpBestPathRib = new BgpBestPathRib(this, _prevBgpBestPathRib, true);

    _prevEbgpMultipathRib = _ebgpMultipathRib;
    _ebgpMultipathRib = new BgpMultipathRib(this);
    importRib(_ebgpMultipathRib, _baseEbgpRib);

    _prevEbgpBestPathRib = _ebgpBestPathRib;
    _ebgpBestPathRib = new BgpBestPathRib(this, _prevEbgpBestPathRib, true);
    importRib(_ebgpBestPathRib, _baseEbgpRib);

    _prevIbgpBestPathRib = _ibgpBestPathRib;
    _ibgpBestPathRib = new BgpBestPathRib(this, _prevIbgpBestPathRib, true);
    importRib(_ibgpBestPathRib, _baseIbgpRib);

    _prevIbgpMultipathRib = _ibgpMultipathRib;
    _ibgpMultipathRib = new BgpMultipathRib(this);
    importRib(_ibgpMultipathRib, _baseIbgpRib);
  }

  void reinitRibsNewIteration() {
    /*
     * RIBs not read from can just be re-initialized
     */
    _ospfRib = new OspfRib(this);
    _ripRib = new RipRib(this);

    /*
     * Staging RIBs can also be re-initialized
     */
    _ebgpStagingRib = new BgpMultipathRib(this);
    _ibgpStagingRib = new BgpMultipathRib(this);
    _ospfExternalType1StagingRib = new OspfExternalType1Rib(this);
    _ospfExternalType2StagingRib = new OspfExternalType2Rib(this);

    /*
     * Add routes that cannot change (does not affect below computation)
     */
    importRib(_mainRib, _independentRib);

    /*
     * Re-add independent OSPF routes to ospfRib for tie-breaking
     */
    importRib(_ospfRib, _ospfIntraAreaRib);
    importRib(_ospfRib, _ospfInterAreaRib);
    /*
     * Re-add independent RIP routes to ripRib for tie-breaking
     */
    importRib(_ripRib, _ripInternalRib);
  }

  /**
   * Compare main RIB and OSPF-external RIBs to their respective previous versions.
   *
   * @return true if there are any differences between the RIBs
   */
  boolean compareRibs() {
    return !_mainRib.equals(_prevMainRib)
        || !_ospfExternalType1Rib.equals(_prevOspfExternalType1Rib)
        || !_ospfExternalType2Rib.equals(_prevOspfExternalType2Rib);
  }

  /**
   * Merge intra/inter OSPF RIBs into a general OSPF RIB, then merge that into the independent RIB
   */
  void importOspfInternalRoutes() {
    importRib(_ospfRib, _ospfIntraAreaRib);
    importRib(_ospfRib, _ospfInterAreaRib);
    importRib(_independentRib, _ospfRib);
  }
}

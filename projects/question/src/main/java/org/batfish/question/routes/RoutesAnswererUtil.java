package org.batfish.question.routes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.batfish.datamodel.table.TableDiff.COL_BASE_PREFIX;
import static org.batfish.datamodel.table.TableDiff.COL_DELTA_PREFIX;
import static org.batfish.question.routes.RoutesAnswerer.COL_ADMIN_DISTANCE;
import static org.batfish.question.routes.RoutesAnswerer.COL_AS_PATH;
import static org.batfish.question.routes.RoutesAnswerer.COL_CLUSTER_LIST;
import static org.batfish.question.routes.RoutesAnswerer.COL_COMMUNITIES;
import static org.batfish.question.routes.RoutesAnswerer.COL_LOCAL_PREF;
import static org.batfish.question.routes.RoutesAnswerer.COL_METRIC;
import static org.batfish.question.routes.RoutesAnswerer.COL_NETWORK;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP_INTERFACE;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP_IP;
import static org.batfish.question.routes.RoutesAnswerer.COL_NODE;
import static org.batfish.question.routes.RoutesAnswerer.COL_ORIGINATOR_ID;
import static org.batfish.question.routes.RoutesAnswerer.COL_ORIGIN_PROTOCOL;
import static org.batfish.question.routes.RoutesAnswerer.COL_ORIGIN_TYPE;
import static org.batfish.question.routes.RoutesAnswerer.COL_PROTOCOL;
import static org.batfish.question.routes.RoutesAnswerer.COL_ROUTE_DISTINGUISHER;
import static org.batfish.question.routes.RoutesAnswerer.COL_ROUTE_ENTRY_PRESENCE;
import static org.batfish.question.routes.RoutesAnswerer.COL_STATUS;
import static org.batfish.question.routes.RoutesAnswerer.COL_TAG;
import static org.batfish.question.routes.RoutesAnswerer.COL_VRF_NAME;
import static org.batfish.question.routes.RoutesAnswerer.getDiffTableMetadata;
import static org.batfish.question.routes.RoutesAnswerer.getTableMetadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AbstractRouteDecorator;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.EvpnRoute;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.BgpRouteStatus;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableDiff;
import org.batfish.question.routes.DiffRoutesOutput.KeyPresenceStatus;
import org.batfish.question.routes.RoutesQuestion.RibProtocol;
import org.batfish.specifier.RoutingProtocolSpecifier;

public class RoutesAnswererUtil {

  /** IPs that are used internally and should not be exposed as next hop IPs */
  private static final Set<Ip> INTERNAL_USE_IPS =
      ImmutableSet.of(
          // BGP unnumbered IP
          Ip.parse("169.254.0.1"));

  public enum RouteEntryPresenceStatus {
    ONLY_IN_SNAPSHOT(TableDiff.COL_KEY_STATUS_ONLY_BASE),
    ONLY_IN_REFERENCE(TableDiff.COL_KEY_STATUS_ONLY_DELTA),
    CHANGED("Changed"),
    UNCHANGED("Unchanged");

    private static final Map<String, RouteEntryPresenceStatus> _map = buildMap();

    private static Map<String, RouteEntryPresenceStatus> buildMap() {
      ImmutableMap.Builder<String, RouteEntryPresenceStatus> map = ImmutableMap.builder();
      for (RouteEntryPresenceStatus value : RouteEntryPresenceStatus.values()) {
        map.put(value._name, value);
      }
      return map.build();
    }

    @JsonCreator
    public static RouteEntryPresenceStatus fromName(String name) {
      RouteEntryPresenceStatus instance = _map.get(name);
      if (instance == null) {
        throw new BatfishException(
            String.format(
                "No %s with name: '%s'", RouteEntryPresenceStatus.class.getSimpleName(), name));
      }
      return instance;
    }

    private final String _name;

    RouteEntryPresenceStatus(String name) {
      _name = name;
    }

    @JsonValue
    public String routeEntryPresenceName() {
      return _name;
    }
  }

  /** Compute the next hop node for a given next hop IP. */
  @Nullable
  static String computeNextHopNode(
      @Nullable Ip nextHopIp, @Nullable Map<Ip, Set<String>> ipOwners) {
    if (nextHopIp == null || ipOwners == null) {
      return null;
    }
    Set<String> nextNodes = ipOwners.getOrDefault(nextHopIp, ImmutableSet.of());
    if (nextNodes.size() == 1) {
      return Iterables.getOnlyElement(nextNodes);
    }
    // TODO: https://github.com/batfish/batfish/issues/1862 can try harder for multiple outs
    return null;
  }

  /**
   * Returns a {@link Multiset} of {@link Row}s for all routes present in all RIBs
   *
   * @param ribs {@link Map} representing all RIBs of all nodes
   * @param matchingNodes {@link Set} of hostnames of nodes whose routes are to be returned
   * @param network {@link Prefix} of the network used to filter the routes
   * @param protocolSpec {@link RoutingProtocolSpecifier} used to filter the routes
   * @param vrfRegex Regex used to filter the VRF of routes
   * @param ipOwners {@link Map} of {@link Ip} to {@link Set} of owner nodes
   * @return {@link Multiset} of {@link Row}s representing the routes
   */
  static <T extends AbstractRouteDecorator> Multiset<Row> getMainRibRoutes(
      SortedMap<String, SortedMap<String, GenericRib<T>>> ribs,
      Set<String> matchingNodes,
      @Nullable Prefix network,
      RoutingProtocolSpecifier protocolSpec,
      String vrfRegex,
      @Nullable Map<Ip, Set<String>> ipOwners) {
    Multiset<Row> rows = HashMultiset.create();
    Pattern compiledVrfRegex = Pattern.compile(vrfRegex);
    Map<String, ColumnMetadata> columnMetadataMap =
        getTableMetadata(RibProtocol.MAIN).toColumnMap();
    ribs.forEach(
        (node, vrfMap) -> {
          if (matchingNodes.contains(node)) {
            vrfMap.forEach(
                (vrfName, rib) -> {
                  if (compiledVrfRegex.matcher(vrfName).matches()) {
                    rib.getRoutes().stream()
                        .filter(
                            route ->
                                (network == null || network.equals(route.getNetwork()))
                                    && protocolSpec.getProtocols().contains(route.getProtocol()))
                        .forEach(
                            route ->
                                rows.add(
                                    abstractRouteToRow(
                                        node, vrfName, route, columnMetadataMap, ipOwners)));
                  }
                });
          }
        });
    return rows;
  }

  /**
   * Filters a {@link Table} of {@link Bgpv4Route}s to produce a {@link Multiset} of rows
   *
   * @param bgpRoutes {@link Table} of all {@link Bgpv4Route}s
   * @param ribProtocol {@link RibProtocol}, either {@link RibProtocol#BGP}
   * @param matchingNodes {@link Set} of nodes from which {@link Bgpv4Route}s are to be selected
   * @param network {@link Prefix} of the network used to filter the routes
   * @param protocolSpec {@link RoutingProtocolSpecifier} used to filter the {@link Bgpv4Route}s
   * @param vrfRegex Regex used to filter the routes based on {@link org.batfish.datamodel.Vrf}
   * @param statuses BGP route statuses that correspond to routes in {@code bgpRoutes}.
   * @return {@link Multiset} of {@link Row}s representing the routes
   */
  static Multiset<Row> getBgpRibRoutes(
      Table<String, String, Set<Bgpv4Route>> bgpRoutes,
      RibProtocol ribProtocol,
      Set<String> matchingNodes,
      @Nullable Prefix network,
      RoutingProtocolSpecifier protocolSpec,
      String vrfRegex,
      Set<BgpRouteStatus> statuses) {
    Multiset<Row> rows = HashMultiset.create();
    Map<String, ColumnMetadata> columnMetadataMap = getTableMetadata(ribProtocol).toColumnMap();
    Pattern compiledVrfRegex = Pattern.compile(vrfRegex);
    matchingNodes.forEach(
        hostname ->
            bgpRoutes
                .row(hostname)
                .forEach(
                    (vrfName, routes) -> {
                      if (compiledVrfRegex.matcher(vrfName).matches()) {
                        routes.stream()
                            .filter(
                                route ->
                                    (network == null || network.equals(route.getNetwork()))
                                        && protocolSpec
                                            .getProtocols()
                                            .contains(route.getProtocol()))
                            .forEach(
                                route ->
                                    rows.add(
                                        bgpRouteToRow(
                                            hostname,
                                            vrfName,
                                            route,
                                            statuses,
                                            columnMetadataMap)));
                      }
                    }));
    return rows;
  }

  static Multiset<Row> getEvpnRoutes(
      Table<String, String, Set<EvpnRoute<?, ?>>> evpnRoutes,
      RibProtocol ribProtocol,
      Set<String> matchingNodes,
      @Nullable Prefix network,
      RoutingProtocolSpecifier protocolSpec,
      String vrfRegex,
      Set<BgpRouteStatus> statuses) {
    Multiset<Row> rows = HashMultiset.create();
    Map<String, ColumnMetadata> columnMetadataMap = getTableMetadata(ribProtocol).toColumnMap();
    Pattern compiledVrfRegex = Pattern.compile(vrfRegex);
    matchingNodes.forEach(
        hostname ->
            evpnRoutes
                .row(hostname)
                .forEach(
                    (vrfName, routes) -> {
                      if (compiledVrfRegex.matcher(vrfName).matches()) {
                        routes.stream()
                            .filter(
                                route ->
                                    (network == null || network.equals(route.getNetwork()))
                                        && protocolSpec
                                            .getProtocols()
                                            .contains(route.getProtocol()))
                            .forEach(
                                route ->
                                    rows.add(
                                        evpnRouteToRow(
                                            hostname,
                                            vrfName,
                                            route,
                                            statuses,
                                            columnMetadataMap)));
                      }
                    }));
    return rows;
  }

  /**
   * Converts a {@link AbstractRoute} to a {@link Row}
   *
   * @param hostName {@link String} host-name of the node containing the route
   * @param vrfName {@link String} name of the VRF containing the route
   * @param abstractRoute {@link AbstractRoute} to convert
   * @param columnMetadataMap Column metadata of the columns for this {@link Row} c
   * @return {@link Row} representing the {@link AbstractRoute}
   */
  private static Row abstractRouteToRow(
      String hostName,
      String vrfName,
      AbstractRoute abstractRoute,
      Map<String, ColumnMetadata> columnMetadataMap,
      @Nullable Map<Ip, Set<String>> ipOwners) {
    // If the route's next hop IP is for internal use, do not show it in the row
    Ip nextHopIp =
        INTERNAL_USE_IPS.contains(abstractRoute.getNextHopIp())
            ? null
            : abstractRoute.getNextHopIp();
    return Row.builder(columnMetadataMap)
        .put(COL_NODE, new Node(hostName))
        .put(COL_VRF_NAME, vrfName)
        .put(COL_NETWORK, abstractRoute.getNetwork())
        .put(COL_NEXT_HOP_IP, nextHopIp)
        .put(COL_NEXT_HOP_INTERFACE, abstractRoute.getNextHopInterface())
        .put(COL_NEXT_HOP, computeNextHopNode(abstractRoute.getNextHopIp(), ipOwners))
        .put(COL_PROTOCOL, abstractRoute.getProtocol())
        .put(
            COL_TAG,
            abstractRoute.getTag() == Route.UNSET_ROUTE_TAG ? null : abstractRoute.getTag())
        .put(COL_ADMIN_DISTANCE, abstractRoute.getAdministrativeCost())
        .put(COL_METRIC, abstractRoute.getMetric())
        .build();
  }

  /**
   * Converts a {@link Bgpv4Route} to a {@link Row}
   *
   * @param hostName {@link String} host-name of the node containing the bgpv4Route
   * @param vrfName {@link String} name of the VRF containing the bgpv4Route
   * @param bgpv4Route {@link Bgpv4Route} BGP route to convert
   * @param statuses BGP route statuses applicable to the route
   * @param columnMetadataMap Column metadata of the columns for this {@link Row}
   * @return {@link Row} representing the {@link Bgpv4Route}
   */
  static Row bgpRouteToRow(
      String hostName,
      String vrfName,
      Bgpv4Route bgpv4Route,
      Set<BgpRouteStatus> statuses,
      Map<String, ColumnMetadata> columnMetadataMap) {
    // If the route's next hop IP is for internal use, do not show it in the row
    Ip nextHopIp =
        INTERNAL_USE_IPS.contains(bgpv4Route.getNextHopIp()) ? null : bgpv4Route.getNextHopIp();
    return Row.builder(columnMetadataMap)
        .put(COL_NODE, new Node(hostName))
        .put(COL_VRF_NAME, vrfName)
        .put(COL_NETWORK, bgpv4Route.getNetwork())
        .put(COL_NEXT_HOP_IP, nextHopIp)
        .put(COL_NEXT_HOP_INTERFACE, bgpv4Route.getNextHopInterface())
        .put(COL_PROTOCOL, bgpv4Route.getProtocol())
        .put(COL_AS_PATH, bgpv4Route.getAsPath().getAsPathString())
        .put(COL_METRIC, bgpv4Route.getMetric())
        .put(COL_LOCAL_PREF, bgpv4Route.getLocalPreference())
        .put(
            COL_COMMUNITIES,
            bgpv4Route.getCommunities().getCommunities().stream()
                .map(Community::toString)
                .collect(toImmutableList()))
        .put(COL_ORIGIN_PROTOCOL, bgpv4Route.getSrcProtocol())
        .put(COL_ORIGIN_TYPE, bgpv4Route.getOriginType())
        .put(COL_ORIGINATOR_ID, bgpv4Route.getOriginatorIp())
        .put(
            COL_CLUSTER_LIST,
            bgpv4Route.getClusterList().isEmpty() ? null : bgpv4Route.getClusterList())
        .put(COL_TAG, bgpv4Route.getTag() == Route.UNSET_ROUTE_TAG ? null : bgpv4Route.getTag())
        .put(COL_STATUS, statuses)
        .build();
  }

  static Row evpnRouteToRow(
      String hostName,
      String vrfName,
      EvpnRoute<?, ?> evpnRoute,
      Set<BgpRouteStatus> statuses,
      Map<String, ColumnMetadata> columnMetadataMap) {
    // If the route's next hop IP is for internal use, do not show it in the row
    Ip nextHopIp =
        INTERNAL_USE_IPS.contains(evpnRoute.getNextHopIp()) ? null : evpnRoute.getNextHopIp();
    return Row.builder(columnMetadataMap)
        .put(COL_NODE, new Node(hostName))
        .put(COL_VRF_NAME, vrfName)
        .put(COL_NETWORK, evpnRoute.getNetwork())
        .put(COL_NEXT_HOP_IP, nextHopIp)
        .put(COL_NEXT_HOP_INTERFACE, evpnRoute.getNextHopInterface())
        .put(COL_PROTOCOL, evpnRoute.getProtocol())
        .put(COL_AS_PATH, evpnRoute.getAsPath().getAsPathString())
        .put(COL_METRIC, evpnRoute.getMetric())
        .put(COL_LOCAL_PREF, evpnRoute.getLocalPreference())
        .put(
            COL_COMMUNITIES,
            evpnRoute.getCommunities().getCommunities().stream()
                .map(Community::toString)
                .collect(toImmutableList()))
        .put(COL_ORIGIN_PROTOCOL, evpnRoute.getSrcProtocol())
        .put(COL_ORIGIN_TYPE, evpnRoute.getOriginType())
        .put(COL_ORIGINATOR_ID, evpnRoute.getOriginatorIp())
        .put(
            COL_CLUSTER_LIST,
            evpnRoute.getClusterList().isEmpty() ? null : evpnRoute.getClusterList())
        .put(COL_TAG, evpnRoute.getTag() == Route.UNSET_ROUTE_TAG ? null : evpnRoute.getTag())
        .put(COL_ROUTE_DISTINGUISHER, evpnRoute.getRouteDistinguisher())
        .put(COL_STATUS, statuses)
        .build();
  }

  /**
   * Converts {@link List} of {@link DiffRoutesOutput} to {@link Row}s with one row corresponding to
   * each {@link DiffRoutesOutput#getDiffInAttributes} of the {@link DiffRoutesOutput}
   *
   * @param diffRoutesList {@link List} of {@link DiffRoutesOutput} for {@link Bgpv4Route}s
   * @return {@link Multiset} of {@link Row}s
   */
  static Multiset<Row> getBgpRouteRowsDiff(
      List<DiffRoutesOutput> diffRoutesList, RibProtocol ribProtocol) {
    Multiset<Row> rows = HashMultiset.create();
    Map<String, ColumnMetadata> columnMetadataMap = getDiffTableMetadata(ribProtocol).toColumnMap();
    for (DiffRoutesOutput diffRoutesOutput : diffRoutesList) {
      RouteRowKey routeRowKey = diffRoutesOutput.getRouteRowKey();
      String hostName = routeRowKey.getHostName();
      String vrfName = routeRowKey.getVrfName();
      Prefix network = routeRowKey.getPrefix();
      RouteRowSecondaryKey routeRowSecondaryKey = diffRoutesOutput.getRouteRowSecondaryKey();
      KeyPresenceStatus secondaryKeyPresenceStatus =
          diffRoutesOutput.getRouteRowSecondaryKeyStatus();

      for (List<RouteRowAttribute> routeRowAttributeInBaseAndRef :
          diffRoutesOutput.getDiffInAttributes()) {
        Row.RowBuilder rowBuilder = Row.builder(columnMetadataMap);
        rowBuilder
            .put(COL_NODE, new Node(hostName))
            .put(COL_VRF_NAME, vrfName)
            .put(COL_NETWORK, network);

        RouteRowAttribute routeRowAttributeBase = routeRowAttributeInBaseAndRef.get(0);
        RouteRowAttribute routeRowAttributeRef = routeRowAttributeInBaseAndRef.get(1);

        rowBuilder.put(
            COL_ROUTE_ENTRY_PRESENCE,
            getRouteEntryPresence(
                secondaryKeyPresenceStatus, routeRowAttributeBase, routeRowAttributeRef));

        populateSecondaryKeyAttrs(routeRowSecondaryKey, secondaryKeyPresenceStatus, rowBuilder);
        populateBgpRouteAttributes(rowBuilder, routeRowAttributeBase, true);
        populateBgpRouteAttributes(rowBuilder, routeRowAttributeRef, false);

        rows.add(rowBuilder.build());
      }
    }
    return rows;
  }

  /**
   * Populates the {@link RowBuilder} for differential answer of {@link RoutesAnswerer} with
   * attributes of {@link RouteRowSecondaryKey} for base and reference snapshot
   *
   * @param routeRowSecondaryKey {@link RouteRowSecondaryKey} for the current {@link Row} of the
   *     differential answer
   * @param secondaryKeyPresence {@link KeyPresenceStatus} for the routeRowSecondaryKey
   * @param rowBuilder {@link RowBuilder} for the current row
   */
  private static void populateSecondaryKeyAttrs(
      RouteRowSecondaryKey routeRowSecondaryKey,
      KeyPresenceStatus secondaryKeyPresence,
      RowBuilder rowBuilder) {
    Ip nextHopIp = routeRowSecondaryKey.getNextHopIp();
    String protocol = routeRowSecondaryKey.getProtocol();
    // populating base columns for secondary key if it is present in base snapshot or in both
    // snapshots
    if (secondaryKeyPresence == KeyPresenceStatus.IN_BOTH
        || secondaryKeyPresence == KeyPresenceStatus.ONLY_IN_SNAPSHOT) {
      rowBuilder
          .put(COL_BASE_PREFIX + COL_NEXT_HOP_IP, nextHopIp)
          .put(COL_BASE_PREFIX + COL_PROTOCOL, protocol);
    }
    // populating reference columns for secondary key if it is present in reference snapshot or in
    // both snapshots
    if (secondaryKeyPresence == KeyPresenceStatus.IN_BOTH
        || secondaryKeyPresence == KeyPresenceStatus.ONLY_IN_REFERENCE) {
      rowBuilder
          .put(COL_DELTA_PREFIX + COL_NEXT_HOP_IP, nextHopIp)
          .put(COL_DELTA_PREFIX + COL_PROTOCOL, protocol);
    }
  }

  /**
   * Computes RoutePresenceEntryStatus using secondary key status and pairs of routeRowAttributes in
   * base and reference
   */
  private static RouteEntryPresenceStatus getRouteEntryPresence(
      KeyPresenceStatus secondaryKeyStatus,
      RouteRowAttribute routeRowAttributeBase,
      RouteRowAttribute routeRowAttributeReference) {
    RouteEntryPresenceStatus routeEntryPresenceStatus;
    if (secondaryKeyStatus.equals(KeyPresenceStatus.IN_BOTH)) {
      if (routeRowAttributeBase != null && routeRowAttributeReference != null) {
        routeEntryPresenceStatus =
            routeRowAttributeBase.equals(routeRowAttributeReference)
                ? RouteEntryPresenceStatus.UNCHANGED
                : RouteEntryPresenceStatus.CHANGED;

      } else {
        routeEntryPresenceStatus =
            routeRowAttributeBase != null
                ? RouteEntryPresenceStatus.ONLY_IN_SNAPSHOT
                : RouteEntryPresenceStatus.ONLY_IN_REFERENCE;
      }
    } else {
      routeEntryPresenceStatus =
          secondaryKeyStatus == KeyPresenceStatus.ONLY_IN_REFERENCE
              ? RouteEntryPresenceStatus.ONLY_IN_REFERENCE
              : RouteEntryPresenceStatus.ONLY_IN_SNAPSHOT;
    }
    return routeEntryPresenceStatus;
  }

  private static void populateBgpRouteAttributes(
      RowBuilder rowBuilder, @Nullable RouteRowAttribute routeRowAttribute, boolean base) {
    String prefix = base ? COL_BASE_PREFIX : COL_DELTA_PREFIX;
    rowBuilder
        .put(
            prefix + COL_AS_PATH,
            routeRowAttribute != null && routeRowAttribute.getAsPath() != null
                ? routeRowAttribute.getAsPath().getAsPathString()
                : null)
        .put(prefix + COL_METRIC, routeRowAttribute != null ? routeRowAttribute.getMetric() : null)
        .put(
            prefix + COL_LOCAL_PREF,
            routeRowAttribute != null ? routeRowAttribute.getLocalPreference() : null)
        .put(
            prefix + COL_COMMUNITIES,
            routeRowAttribute != null ? routeRowAttribute.getCommunities() : null)
        .put(
            prefix + COL_ORIGIN_PROTOCOL,
            routeRowAttribute != null ? routeRowAttribute.getOriginProtocol() : null)
        .put(
            prefix + COL_ORIGIN_TYPE,
            routeRowAttribute != null ? routeRowAttribute.getOriginType() : null)
        .put(prefix + COL_TAG, routeRowAttribute != null ? routeRowAttribute.getTag() : null)
        .put(
            prefix + COL_STATUS,
            routeRowAttribute != null && routeRowAttribute.getStatus() != null
                ? ImmutableList.of(routeRowAttribute.getStatus())
                : null);
  }

  /**
   * Converts {@link List} of {@link DiffRoutesOutput} to {@link Row}s with one row corresponding to
   * each {@link DiffRoutesOutput#getDiffInAttributes} of the {@link DiffRoutesOutput}
   *
   * @param diffRoutesList {@link List} of {@link DiffRoutesOutput} for routes in Main RIB
   * @return {@link Multiset} of {@link Row}s
   */
  static Multiset<Row> getAbstractRouteRowsDiff(List<DiffRoutesOutput> diffRoutesList) {
    Map<String, ColumnMetadata> columnMetadataMap =
        getDiffTableMetadata(RibProtocol.MAIN).toColumnMap();
    Multiset<Row> rows = HashMultiset.create();
    for (DiffRoutesOutput diffRoutesOutput : diffRoutesList) {
      RouteRowKey routeRowKey = diffRoutesOutput.getRouteRowKey();
      String hostName = routeRowKey.getHostName();
      String vrfName = routeRowKey.getVrfName();
      Prefix network = routeRowKey.getPrefix();

      RouteRowSecondaryKey routeRowSecondaryKey = diffRoutesOutput.getRouteRowSecondaryKey();
      KeyPresenceStatus secondaryKeyPresenceStatus =
          diffRoutesOutput.getRouteRowSecondaryKeyStatus();

      for (List<RouteRowAttribute> routeRowAttributeInBaseAndRef :
          diffRoutesOutput.getDiffInAttributes()) {
        Row.RowBuilder rowBuilder = Row.builder(columnMetadataMap);
        rowBuilder
            .put(COL_NODE, new Node(hostName))
            .put(COL_VRF_NAME, vrfName)
            .put(COL_NETWORK, network);

        RouteRowAttribute routeRowAttributeBase = routeRowAttributeInBaseAndRef.get(0);
        RouteRowAttribute routeRowAttributeRef = routeRowAttributeInBaseAndRef.get(1);

        rowBuilder.put(
            COL_ROUTE_ENTRY_PRESENCE,
            getRouteEntryPresence(
                secondaryKeyPresenceStatus, routeRowAttributeBase, routeRowAttributeRef));

        populateSecondaryKeyAttrs(routeRowSecondaryKey, secondaryKeyPresenceStatus, rowBuilder);
        populateRouteAttributes(rowBuilder, routeRowAttributeBase, true);
        populateRouteAttributes(rowBuilder, routeRowAttributeRef, false);
        rows.add(rowBuilder.build());
      }
    }
    return rows;
  }

  static void populateRouteAttributes(
      RowBuilder rowBuilder, @Nullable RouteRowAttribute routeRowAttribute, boolean base) {
    rowBuilder
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_NEXT_HOP,
            routeRowAttribute != null ? routeRowAttribute.getNextHop() : null)
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_NEXT_HOP_INTERFACE,
            routeRowAttribute != null ? routeRowAttribute.getNextHopInterface() : null)
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_METRIC,
            routeRowAttribute != null ? routeRowAttribute.getMetric() : null)
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_ADMIN_DISTANCE,
            routeRowAttribute != null ? routeRowAttribute.getAdminDistance() : null)
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_TAG,
            routeRowAttribute != null ? routeRowAttribute.getTag() : null);
  }

  /**
   * Given a {@link Map} of all RIBs groups the routes in them by the fields of {@link RouteRowKey}
   * and further sub-groups them by {@link RouteRowSecondaryKey} and for routes in the same
   * sub-group, sorts them according to {@link RouteRowAttribute}s
   *
   * @param ribs {@link Map} of the RIBs
   * @param matchingNodes {@link Set} of nodes to be matched
   * @param network {@link Prefix}
   * @param vrfRegex Regex to filter the VRF
   * @param protocolSpec {@link RoutingProtocolSpecifier} to filter the protocols of the routes
   * @param ipOwners {@link Map} of {@link Ip} to {@link Set} of owner nodes
   * @return {@link Map} of {@link RouteRowKey}s to corresponding sub{@link Map}s of {@link
   *     RouteRowSecondaryKey} to {@link SortedSet} of {@link RouteRowAttribute}s
   */
  public static <T extends AbstractRouteDecorator>
      Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> groupRoutes(
          SortedMap<String, SortedMap<String, GenericRib<T>>> ribs,
          Set<String> matchingNodes,
          @Nullable Prefix network,
          String vrfRegex,
          RoutingProtocolSpecifier protocolSpec,
          @Nullable Map<Ip, Set<String>> ipOwners) {
    Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> routesGroups =
        new HashMap<>();
    Pattern compiledVrfRegex = Pattern.compile(vrfRegex);
    ribs.forEach(
        (node, vrfMap) -> {
          if (matchingNodes.contains(node)) {
            vrfMap.forEach(
                (vrfName, rib) -> {
                  if (compiledVrfRegex.matcher(vrfName).matches()) {
                    rib.getRoutes().stream()
                        .filter(
                            route ->
                                (network == null || network.equals(route.getNetwork()))
                                    && protocolSpec.getProtocols().contains(route.getProtocol()))
                        .forEach(
                            route ->
                                routesGroups
                                    .computeIfAbsent(
                                        new RouteRowKey(node, vrfName, route.getNetwork()),
                                        k -> new HashMap<>())
                                    .computeIfAbsent(
                                        new RouteRowSecondaryKey(
                                            route.getNextHopIp(),
                                            route.getProtocol().protocolName()),
                                        k -> new TreeSet<>())
                                    .add(
                                        RouteRowAttribute.builder()
                                            .setNextHop(
                                                computeNextHopNode(route.getNextHopIp(), ipOwners))
                                            .setNextHopInterface(route.getNextHopInterface())
                                            .setAdminDistance(route.getAdministrativeCost())
                                            .setMetric(route.getMetric())
                                            .setTag(route.getTag())
                                            .build()));
                  }
                });
          }
        });
    return routesGroups;
  }

  /**
   * Given a {@link Table} of {@link Bgpv4Route}s indexed by Node name and VRF name, applies given
   * filters and groups the routes by {@link RouteRowKey} and sub-groups them further by {@link
   * RouteRowSecondaryKey} and for the routes in same sub-groups, sorts them according to {@link
   * RouteRowAttribute}
   *
   * @param bgpBestRoutes {@link Table} of best BGP routes with rows per node and columns per VRF
   * @param bgpBackupRoutes {@link Table} of backup BGP routes with rows per node and columns per
   *     VRF
   * @param matchingNodes {@link Set} of nodes to be matched
   * @param vrfRegex Regex to filter the VRF
   * @param network {@link Prefix}
   * @param protocolSpec {@link RoutingProtocolSpecifier} to filter the protocols of the routes
   * @return {@link Map} of {@link RouteRowKey}s to corresponding sub{@link Map}s of {@link
   *     RouteRowSecondaryKey} to {@link SortedSet} of {@link RouteRowAttribute}s
   */
  static Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> groupBgpRoutes(
      @Nullable Table<String, String, Set<Bgpv4Route>> bgpBestRoutes,
      @Nullable Table<String, String, Set<Bgpv4Route>> bgpBackupRoutes,
      Set<String> matchingNodes,
      String vrfRegex,
      @Nullable Prefix network,
      RoutingProtocolSpecifier protocolSpec) {
    checkArgument(
        bgpBestRoutes != null || bgpBackupRoutes != null,
        "At least one of best routes or backup routes is required.");
    Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> routesGroups =
        new HashMap<>();
    Pattern compiledVrfRegex = Pattern.compile(vrfRegex);

    Map<BgpRouteStatus, Table<String, String, Set<Bgpv4Route>>> routesByStatus =
        new EnumMap<BgpRouteStatus, Table<String, String, Set<Bgpv4Route>>>(BgpRouteStatus.class);
    if (bgpBestRoutes != null) {
      routesByStatus.put(BgpRouteStatus.BEST, bgpBestRoutes);
    }
    if (bgpBackupRoutes != null) {
      routesByStatus.put(BgpRouteStatus.BACKUP, bgpBackupRoutes);
    }

    matchingNodes.forEach(
        hostname ->
            routesByStatus.forEach(
                (status, statusRoutes) ->
                    statusRoutes.row(hostname).entrySet().stream()
                        .filter(vrfEntry -> compiledVrfRegex.matcher(vrfEntry.getKey()).matches())
                        .forEach(
                            vrfEntry ->
                                vrfEntry.getValue().stream()
                                    .filter(
                                        route ->
                                            (network == null || network.equals(route.getNetwork()))
                                                && protocolSpec
                                                    .getProtocols()
                                                    .contains(route.getProtocol()))
                                    .forEach(
                                        route ->
                                            routesGroups
                                                .computeIfAbsent(
                                                    new RouteRowKey(
                                                        hostname,
                                                        vrfEntry.getKey(),
                                                        route.getNetwork()),
                                                    k -> new HashMap<>())
                                                .computeIfAbsent(
                                                    new RouteRowSecondaryKey(
                                                        route.getNextHopIp(),
                                                        route.getProtocol().protocolName()),
                                                    k -> new TreeSet<>())
                                                .add(
                                                    RouteRowAttribute.builder()
                                                        .setOriginProtocol(
                                                            route.getSrcProtocol() != null
                                                                ? route
                                                                    .getSrcProtocol()
                                                                    .protocolName()
                                                                : null)
                                                        .setAdminDistance(
                                                            route.getAdministrativeCost())
                                                        .setMetric(route.getMetric())
                                                        .setAsPath(route.getAsPath())
                                                        .setLocalPreference(
                                                            route.getLocalPreference())
                                                        .setCommunities(
                                                            route
                                                                .getCommunities()
                                                                .getCommunities()
                                                                .stream()
                                                                .map(Community::toString)
                                                                .collect(toImmutableList()))
                                                        .setOriginType(route.getOriginType())
                                                        .setTag(
                                                            route.getTag() == Route.UNSET_ROUTE_TAG
                                                                ? null
                                                                : route.getTag())
                                                        .setStatus(status)
                                                        .build())))));

    return routesGroups;
  }

  /**
   * Given two sorted {@link List}s of {@link RouteRowAttribute}s, outputs a 2-dimensional list
   * showing the diff in the two input lists
   *
   * <p>The aligning is done using two pointers method where we place the two pointers at the
   * beginning of both lists in the beginning and then keep incrementing the one pointing to a
   * smaller element. If the elements at the two pointers are equal, we found a match, otherwise it
   * is a missing route.
   *
   * <p>The output is a nested {@link List} with the inner list of always size 2. The inner list
   * contains {@link RouteRowAttribute} for matching routes and contains nulls for missing routes.
   *
   * @param routeRowAttributes1 First sorted {@link List} of {@link RouteRowAttribute}s
   * @param routeRowAttributes2 Second sorted {@link List} of {@link RouteRowAttribute}s
   * @return A 2-dimensional {@link List} of columns size two showing the diff
   */
  static List<List<RouteRowAttribute>> alignRouteRowAttributes(
      List<RouteRowAttribute> routeRowAttributes1, List<RouteRowAttribute> routeRowAttributes2) {
    List<List<RouteRowAttribute>> alignedRouteRowAttrs = new ArrayList<>();

    // i and j are two pointers initialized to the starting of both lists
    int i = 0;
    int j = 0;
    while (i < routeRowAttributes1.size() && j < routeRowAttributes2.size()) {
      RouteRowAttribute routeRowAttribute1 = routeRowAttributes1.get(i);
      RouteRowAttribute routeRowAttribute2 = routeRowAttributes2.get(j);
      if (routeRowAttribute1.compareTo(routeRowAttribute2) < 0) {
        i++;
        alignedRouteRowAttrs.add(Lists.newArrayList(routeRowAttribute1, null));
      } else if (routeRowAttribute1.compareTo(routeRowAttribute2) > 0) {
        j++;
        alignedRouteRowAttrs.add(Lists.newArrayList(null, routeRowAttribute2));
      } else {
        // the two pointers are pointing to equal elements, so we found a match
        i++;
        j++;
        alignedRouteRowAttrs.add(Lists.newArrayList(routeRowAttribute1, routeRowAttribute2));
      }
    }

    // adding one sided pairs for whichever list remains un-traversed, the other element in the
    // inner list will be null
    // only one of these loops will execute
    while (i < routeRowAttributes1.size()) {
      alignedRouteRowAttrs.add(Lists.newArrayList(routeRowAttributes1.get(i++), null));
    }
    while (j < routeRowAttributes2.size()) {
      alignedRouteRowAttrs.add(Lists.newArrayList(null, routeRowAttributes2.get(j++)));
    }

    return alignedRouteRowAttrs;
  }

  /**
   * Given two {@link Map}s containing mapping from {@link RouteRowKey} to {@link Map}s of {@link
   * RouteRowSecondaryKey} to {@link SortedSet} of {@link RouteRowAttribute}s, produces diff in the
   * form of a {@link List} of {@link DiffRoutesOutput}
   *
   * @param routesInBase {@link Map} from {@link RouteRowKey} to {@link Map} of {@link
   *     RouteRowSecondaryKey} to {@link SortedSet} of {@link RouteRowAttribute}s in the base
   *     snapshot
   * @param routesInRef {@link Map} from {@link RouteRowKey} to {@link Map} of {@link
   *     RouteRowSecondaryKey} to {@link SortedSet} of {@link RouteRowAttribute}s in the ref
   *     snapshot
   * @return {@link List} of {@link DiffRoutesOutput}
   */
  public static List<DiffRoutesOutput> getRoutesDiff(
      Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> routesInBase,
      Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> routesInRef) {
    Set<RouteRowKey> allRouteKeys = new HashSet<>(routesInBase.keySet());
    allRouteKeys.addAll(routesInRef.keySet());

    List<DiffRoutesOutput> listDiffs = new ArrayList<>();
    for (RouteRowKey routeRowKey : allRouteKeys) {
      Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> baseAttrsForRowKey =
          routesInBase.get(routeRowKey);
      Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> refAttrsForRowKey =
          routesInRef.get(routeRowKey);

      if (baseAttrsForRowKey != null && refAttrsForRowKey != null) {
        // this network is present in routesInBase and routesInRef and respective values are
        // different
        if (!baseAttrsForRowKey.equals(refAttrsForRowKey)) {
          listDiffs.addAll(getDiffPerKey(routeRowKey, baseAttrsForRowKey, refAttrsForRowKey));
        }
      } else if (baseAttrsForRowKey != null) {
        baseAttrsForRowKey.forEach(
            (key, value) -> {
              // the nested list contains list of pairs of RouteRowAttributes with fist element and
              // second element of the pair from base and reference snapshots respectively, second
              // element is null to account for absence of this network in the reference snapshot
              List<List<RouteRowAttribute>> diffMatrix =
                  value.stream()
                      .map(routeRowAttribute -> Lists.newArrayList(routeRowAttribute, null))
                      .collect(Collectors.toList());
              listDiffs.add(
                  new DiffRoutesOutput(
                      routeRowKey,
                      key,
                      KeyPresenceStatus.ONLY_IN_SNAPSHOT,
                      diffMatrix,
                      KeyPresenceStatus.ONLY_IN_SNAPSHOT));
            });
      } else {
        refAttrsForRowKey.forEach(
            (key, value) -> {
              // the nested list contains list of pairs of RouteRowAttributes with fist element and
              // second element of the pair from base and reference snapshots respectively, first
              // element is null to account for absence of this network in the base snapshot
              List<List<RouteRowAttribute>> diffMatrix =
                  value.stream()
                      .map(routeRowAttribute -> Lists.newArrayList(null, routeRowAttribute))
                      .collect(Collectors.toList());
              listDiffs.add(
                  new DiffRoutesOutput(
                      routeRowKey,
                      key,
                      KeyPresenceStatus.ONLY_IN_REFERENCE,
                      diffMatrix,
                      KeyPresenceStatus.ONLY_IN_REFERENCE));
            });
      }
    }
    return listDiffs;
  }

  /**
   * Gets the diff in the form of {@link List} of {@link DiffRoutesOutput} for {@link
   * RouteRowAttribute}s for a given {@link RouteRowKey}
   *
   * @param routeRowKey {@link RouteRowKey} for the {@link RouteRowAttribute}s in innerGroup1 and
   *     innerGroup2
   * @param innerGroup1 {@link Map} from {@link RouteRowSecondaryKey} to {@link SortedSet} of {@link
   *     RouteRowAttribute}s for a given {@link RouteRowKey} in base snapshot
   * @param innerGroup2 {@link Map} from {@link RouteRowSecondaryKey} to {@link SortedSet} of {@link
   *     RouteRowAttribute}s for a given {@link RouteRowKey} in ref snapshot
   * @return {@link List} of {@link DiffRoutesOutput}
   */
  @VisibleForTesting
  static List<DiffRoutesOutput> getDiffPerKey(
      RouteRowKey routeRowKey,
      Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> innerGroup1,
      Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>> innerGroup2) {
    Set<RouteRowSecondaryKey> allRouteKeys = new HashSet<>(innerGroup1.keySet());
    allRouteKeys.addAll(innerGroup2.keySet());
    List<DiffRoutesOutput> diffForThisKey = new ArrayList<>();
    for (RouteRowSecondaryKey secondaryKey : allRouteKeys) {
      // a nested list that contains list of pairs of RouteRowAttributes with fist element and
      // second element from innerGroup1 and innerGroup2 respectively
      List<List<RouteRowAttribute>> diffMatrix;
      KeyPresenceStatus routeRowSecondaryKeyStatus;

      if (innerGroup1.containsKey(secondaryKey) && innerGroup2.containsKey(secondaryKey)) {
        routeRowSecondaryKeyStatus = KeyPresenceStatus.IN_BOTH;
        // both groups have route for this next hop ip and protocol
        SortedSet<RouteRowAttribute> routeRowAttributes1 = innerGroup1.get(secondaryKey);
        SortedSet<RouteRowAttribute> routeRowAttributes2 = innerGroup2.get(secondaryKey);
        if (routeRowAttributes1.size() == 1 && routeRowAttributes2.size() == 1) {
          // no need to align since  only one RouteRowAttribute exists for this secondary key in
          // innerGroup1 and innerGroup2
          diffMatrix =
              ImmutableList.of(
                  ImmutableList.of(routeRowAttributes1.first(), routeRowAttributes2.first()));

        } else {
          // need to align since  number of RowRowAttributes for this secondary key is different in
          // innerGroup1 and innerGroup2
          diffMatrix =
              alignRouteRowAttributes(
                  new ArrayList<>(routeRowAttributes1), new ArrayList<>(routeRowAttributes2));
        }
      } else if (innerGroup1.containsKey(secondaryKey)) {
        routeRowSecondaryKeyStatus = KeyPresenceStatus.ONLY_IN_SNAPSHOT;
        // second element of RouteRowAttribute pairs will be null to account for absence of
        // secondary key in innerGroup2
        diffMatrix =
            innerGroup1.get(secondaryKey).stream()
                .map(routeRowAttribute -> Lists.newArrayList(routeRowAttribute, null))
                .collect(Collectors.toList());
      } else {
        routeRowSecondaryKeyStatus = KeyPresenceStatus.ONLY_IN_REFERENCE;
        // first element of RouteRowAttribute pairs will be null to account for absence of secondary
        // key in innerGroup1
        diffMatrix =
            innerGroup2.get(secondaryKey).stream()
                .map(routeRowAttribute -> Lists.newArrayList(null, routeRowAttribute))
                .collect(Collectors.toList());
      }
      diffForThisKey.add(
          new DiffRoutesOutput(
              routeRowKey,
              secondaryKey,
              routeRowSecondaryKeyStatus,
              diffMatrix,
              KeyPresenceStatus.IN_BOTH));
    }
    return diffForThisKey;
  }
}

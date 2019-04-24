package org.batfish.question.routes;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.batfish.datamodel.table.TableDiff.COL_BASE_PREFIX;
import static org.batfish.datamodel.table.TableDiff.COL_DELTA_PREFIX;
import static org.batfish.question.routes.RoutesAnswerer.COL_ADMIN_DISTANCE;
import static org.batfish.question.routes.RoutesAnswerer.COL_AS_PATH;
import static org.batfish.question.routes.RoutesAnswerer.COL_COMMUNITIES;
import static org.batfish.question.routes.RoutesAnswerer.COL_LOCAL_PREF;
import static org.batfish.question.routes.RoutesAnswerer.COL_METRIC;
import static org.batfish.question.routes.RoutesAnswerer.COL_NETWORK;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP;
import static org.batfish.question.routes.RoutesAnswerer.COL_NEXT_HOP_IP;
import static org.batfish.question.routes.RoutesAnswerer.COL_NODE;
import static org.batfish.question.routes.RoutesAnswerer.COL_ORIGIN_PROTOCOL;
import static org.batfish.question.routes.RoutesAnswerer.COL_PROTOCOL;
import static org.batfish.question.routes.RoutesAnswerer.COL_ROUTE_ENTRY_PRESENCE;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.GenericRib;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.bgp.community.Community;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.Row.RowBuilder;
import org.batfish.datamodel.table.TableDiff;
import org.batfish.question.routes.DiffRoutesOutput.KeyPresenceStatus;
import org.batfish.question.routes.RoutesQuestion.RibProtocol;
import org.batfish.specifier.RoutingProtocolSpecifier;

public class RoutesAnswererUtil {

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
    // TODO: https://github.com/batfish/batfish/issues/1862
    return ipOwners.getOrDefault(nextHopIp, ImmutableSet.of()).stream()
        .min(Comparator.naturalOrder())
        .orElse(null);
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
   * Filters a {@link Table} of {@link BgpRoute}s to produce a {@link Multiset} of rows
   *
   * @param bgpRoutes {@link Table} of all {@link BgpRoute}s
   * @param ribProtocol {@link RibProtocol}, either {@link RibProtocol#BGP} or {@link
   *     RibProtocol#BGPMP}
   * @param matchingNodes {@link Set} of nodes from which {@link BgpRoute}s are to be selected
   * @param network {@link Prefix} of the network used to filter the routes
   * @param protocolSpec {@link RoutingProtocolSpecifier} used to filter the {@link BgpRoute}s
   * @param vrfRegex Regex used to filter the routes based on {@link org.batfish.datamodel.Vrf}
   * @return {@link Multiset} of {@link Row}s representing the routes
   */
  static Multiset<Row> getBgpRibRoutes(
      Table<String, String, Set<BgpRoute>> bgpRoutes,
      RibProtocol ribProtocol,
      Set<String> matchingNodes,
      @Nullable Prefix network,
      RoutingProtocolSpecifier protocolSpec,
      String vrfRegex) {
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
                                            hostname, vrfName, route, columnMetadataMap)));
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
    return Row.builder(columnMetadataMap)
        .put(COL_NODE, new Node(hostName))
        .put(COL_VRF_NAME, vrfName)
        .put(COL_NETWORK, abstractRoute.getNetwork())
        .put(COL_NEXT_HOP_IP, abstractRoute.getNextHopIp())
        .put(COL_NEXT_HOP, computeNextHopNode(abstractRoute.getNextHopIp(), ipOwners))
        .put(COL_PROTOCOL, abstractRoute.getProtocol())
        .put(
            COL_TAG, abstractRoute.getTag() == AbstractRoute.NO_TAG ? null : abstractRoute.getTag())
        .put(COL_ADMIN_DISTANCE, abstractRoute.getAdministrativeCost())
        .put(COL_METRIC, abstractRoute.getMetric())
        .build();
  }

  /**
   * Converts a {@link BgpRoute} to a {@link Row}
   *
   * @param hostName {@link String} host-name of the node containing the bgpRoute
   * @param vrfName {@link String} name of the VRF containing the bgpRoute
   * @param bgpRoute {@link BgpRoute} BGP route to convert
   * @param columnMetadataMap Column metadata of the columns for this {@link Row}
   * @return {@link Row} representing the {@link BgpRoute}
   */
  static Row bgpRouteToRow(
      String hostName,
      String vrfName,
      BgpRoute bgpRoute,
      Map<String, ColumnMetadata> columnMetadataMap) {
    return Row.builder(columnMetadataMap)
        .put(COL_NODE, new Node(hostName))
        .put(COL_VRF_NAME, vrfName)
        .put(COL_NETWORK, bgpRoute.getNetwork())
        .put(COL_NEXT_HOP_IP, bgpRoute.getNextHopIp())
        .put(COL_PROTOCOL, bgpRoute.getProtocol())
        .put(COL_AS_PATH, bgpRoute.getAsPath().getAsPathString())
        .put(COL_METRIC, bgpRoute.getMetric())
        .put(COL_LOCAL_PREF, bgpRoute.getLocalPreference())
        .put(
            COL_COMMUNITIES,
            bgpRoute.getCommunities().stream().map(Community::toString).collect(toImmutableList()))
        .put(COL_ORIGIN_PROTOCOL, bgpRoute.getSrcProtocol())
        .put(COL_TAG, bgpRoute.getTag() == Route.UNSET_ROUTE_TAG ? null : bgpRoute.getTag())
        .build();
  }

  /**
   * Converts {@link List} of {@link DiffRoutesOutput} to {@link Row}s with one row corresponding to
   * each {@link DiffRoutesOutput#_diffInAttributes} of the {@link DiffRoutesOutput}
   *
   * @param diffRoutesList {@link List} of {@link DiffRoutesOutput} for {@link BgpRoute}s
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
    rowBuilder
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_AS_PATH,
            routeRowAttribute != null && routeRowAttribute.getAsPath() != null
                ? routeRowAttribute.getAsPath().getAsPathString()
                : null)
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_METRIC,
            routeRowAttribute != null ? routeRowAttribute.getMetric() : null)
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_LOCAL_PREF,
            routeRowAttribute != null ? routeRowAttribute.getLocalPreference() : null)
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_COMMUNITIES,
            routeRowAttribute != null ? routeRowAttribute.getCommunities() : null)
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_ORIGIN_PROTOCOL,
            routeRowAttribute != null ? routeRowAttribute.getOriginProtocol() : null)
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_TAG,
            routeRowAttribute != null ? routeRowAttribute.getTag() : null);
  }

  /**
   * Converts {@link List} of {@link DiffRoutesOutput} to {@link Row}s with one row corresponding to
   * each {@link DiffRoutesOutput#_diffInAttributes} of the {@link DiffRoutesOutput}
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

  private static void populateRouteAttributes(
      RowBuilder rowBuilder, @Nullable RouteRowAttribute routeRowAttribute, boolean base) {
    rowBuilder
        .put(
            (base ? COL_BASE_PREFIX : COL_DELTA_PREFIX) + COL_NEXT_HOP,
            routeRowAttribute != null ? routeRowAttribute.getNextHop() : null)
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
  static <T extends AbstractRouteDecorator>
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
   * Given a {@link Table} of {@link BgpRoute}s indexed by Node name and VRF name, applies given
   * filters and groups the routes by {@link RouteRowKey} and sub-groups them further by {@link
   * RouteRowSecondaryKey} and for the routes in same sub-groups, sorts them according to {@link
   * RouteRowAttribute}
   *
   * @param bgpRoutes {@link Table} of BGP routes with rows per node and columns per VRF * @param
   *     ribs {@link Map} of the RIBs
   * @param matchingNodes {@link Set} of nodes to be matched
   * @param vrfRegex Regex to filter the VRF
   * @param network {@link Prefix}
   * @param protocolRegex Regex to filter the protocols of the routes
   * @return {@link Map} of {@link RouteRowKey}s to corresponding sub{@link Map}s of {@link
   *     RouteRowSecondaryKey} to {@link SortedSet} of {@link RouteRowAttribute}s
   */
  static Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> groupBgpRoutes(
      Table<String, String, Set<BgpRoute>> bgpRoutes,
      Set<String> matchingNodes,
      String vrfRegex,
      @Nullable Prefix network,
      String protocolRegex) {
    Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>> routesGroups =
        new HashMap<>();
    Pattern compiledProtocolRegex = Pattern.compile(protocolRegex, Pattern.CASE_INSENSITIVE);
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
                                        && compiledProtocolRegex
                                            .matcher(route.getProtocol().protocolName())
                                            .matches())
                            .forEach(
                                route ->
                                    routesGroups
                                        .computeIfAbsent(
                                            new RouteRowKey(hostname, vrfName, route.getNetwork()),
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
                                                        ? route.getSrcProtocol().protocolName()
                                                        : null)
                                                .setAdminDistance(route.getAdministrativeCost())
                                                .setMetric(route.getMetric())
                                                .setAsPath(route.getAsPath())
                                                .setLocalPreference(route.getLocalPreference())
                                                .setCommunities(
                                                    route.getCommunities().stream()
                                                        .map(Community::toString)
                                                        .collect(toImmutableList()))
                                                .setTag(
                                                    route.getTag() == Route.UNSET_ROUTE_TAG
                                                        ? null
                                                        : route.getTag())
                                                .build()));
                      }
                    }));

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
  static List<DiffRoutesOutput> getRoutesDiff(
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

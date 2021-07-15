package org.batfish.question.routes;

import static org.batfish.common.topology.IpOwners.computeIpNodeOwners;
import static org.batfish.datamodel.questions.BgpRouteStatus.BACKUP;
import static org.batfish.datamodel.questions.BgpRouteStatus.BEST;
import static org.batfish.datamodel.table.TableDiff.COL_BASE_PREFIX;
import static org.batfish.datamodel.table.TableDiff.COL_DELTA_PREFIX;
import static org.batfish.question.routes.RoutesAnswererUtil.getAbstractRouteRowsDiff;
import static org.batfish.question.routes.RoutesAnswererUtil.getBgpRibRoutes;
import static org.batfish.question.routes.RoutesAnswererUtil.getBgpRouteRowsDiff;
import static org.batfish.question.routes.RoutesAnswererUtil.getEvpnRoutes;
import static org.batfish.question.routes.RoutesAnswererUtil.getMainRibRoutes;
import static org.batfish.question.routes.RoutesAnswererUtil.getRoutesDiff;
import static org.batfish.question.routes.RoutesAnswererUtil.groupBgpRoutes;
import static org.batfish.question.routes.RoutesAnswererUtil.groupRoutes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.common.Answerer;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.questions.BgpRouteStatus;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.question.routes.RoutesQuestion.RibProtocol;
import org.batfish.specifier.ConstantEnumSetSpecifier;
import org.batfish.specifier.RoutingProtocolSpecifier;
import org.batfish.specifier.SpecifierFactories;
import org.batfish.specifier.parboiled.Grammar;

/** Answerer for {@link RoutesQuestion} */
@ParametersAreNonnullByDefault
public class RoutesAnswerer extends Answerer {
  // Global (always present) columns
  static final String COL_NODE = "Node";
  static final String COL_VRF_NAME = "VRF";
  static final String COL_NETWORK = "Network";
  static final String COL_NEXT_HOP = "Next_Hop";
  static final String COL_NEXT_HOP_INTERFACE = "Next_Hop_Interface";
  static final String COL_NEXT_HOP_IP = "Next_Hop_IP";
  static final String COL_PROTOCOL = "Protocol";
  static final String COL_TAG = "Tag";

  // Present sometimes
  static final String COL_METRIC = "Metric";

  // Main RIB
  static final String COL_ADMIN_DISTANCE = "Admin_Distance";

  // BGP only
  static final String COL_AS_PATH = "AS_Path";
  static final String COL_LOCAL_PREF = "Local_Pref";
  static final String COL_COMMUNITIES = "Communities";
  static final String COL_ORIGIN_PROTOCOL = "Origin_Protocol";
  static final String COL_ORIGIN_TYPE = "Origin_Type";
  static final String COL_CLUSTER_LIST = "Cluster_List";
  static final String COL_ORIGINATOR_ID = "Originator_Id";
  static final String COL_STATUS = "Status";

  // EVPN BGP only
  static final String COL_ROUTE_DISTINGUISHER = "Route_Distinguisher";

  // Diff Only
  static final String COL_ROUTE_ENTRY_PRESENCE = "Entry_Presence";

  RoutesAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public AnswerElement answer(NetworkSnapshot snapshot) {
    RoutesQuestion question = (RoutesQuestion) _question;
    Set<BgpRouteStatus> expandedBgpRouteStatuses =
        SpecifierFactories.getEnumSetSpecifierOrDefault(
                question.getBgpRouteStatus(),
                Grammar.BGP_ROUTE_STATUS_SPECIFIER,
                new ConstantEnumSetSpecifier<>(ImmutableSet.of(BgpRouteStatus.BEST)))
            .resolve();

    TableAnswerElement answer = new TableAnswerElement(getTableMetadata(question.getRib()));

    DataPlane dp = _batfish.loadDataPlane(snapshot);
    Set<String> matchingNodes =
        question.getNodeSpecifier().resolve(_batfish.specifierContext(snapshot));
    Prefix network = question.getNetwork();
    RoutingProtocolSpecifier protocolSpec = question.getRoutingProtocolSpecifier();
    String vrfRegex = question.getVrfs();
    Map<Ip, Set<String>> ipOwners =
        computeIpNodeOwners(_batfish.loadConfigurations(snapshot), true);
    boolean bgpMultipathBest = expandedBgpRouteStatuses.contains(BEST);
    boolean bgpBackup = expandedBgpRouteStatuses.contains(BACKUP);
    Multiset<Row> rows;

    switch (question.getRib()) {
      case BGP:
        rows = HashMultiset.create();
        if (bgpBackup) {
          rows.addAll(
              getBgpRibRoutes(
                  dp.getBgpBackupRoutes(),
                  RibProtocol.BGP,
                  matchingNodes,
                  network,
                  protocolSpec,
                  vrfRegex,
                  ImmutableSet.of(BACKUP)));
        }
        if (bgpMultipathBest) {
          rows.addAll(
              getBgpRibRoutes(
                  dp.getBgpRoutes(),
                  RibProtocol.BGP,
                  matchingNodes,
                  network,
                  protocolSpec,
                  vrfRegex,
                  ImmutableSet.of(BEST)));
        }
        break;
      case EVPN:
        rows = HashMultiset.create();
        if (bgpBackup) {
          rows.addAll(
              getEvpnRoutes(
                  dp.getEvpnBackupRoutes(),
                  RibProtocol.EVPN,
                  matchingNodes,
                  network,
                  protocolSpec,
                  vrfRegex,
                  ImmutableSet.of(BACKUP)));
        }
        if (bgpMultipathBest) {
          rows.addAll(
              getEvpnRoutes(
                  dp.getEvpnRoutes(),
                  RibProtocol.EVPN,
                  matchingNodes,
                  network,
                  protocolSpec,
                  vrfRegex,
                  ImmutableSet.of(BEST)));
        }
        break;
      case MAIN:
      default:
        rows =
            getMainRibRoutes(
                dp.getRibs(), matchingNodes, network, protocolSpec, vrfRegex, ipOwners);
    }

    answer.postProcessAnswer(_question, rows);
    return answer;
  }

  @Override
  public AnswerElement answerDiff(NetworkSnapshot snapshot, NetworkSnapshot reference) {
    RoutesQuestion question = (RoutesQuestion) _question;
    TableAnswerElement diffAnswer = new TableAnswerElement(getDiffTableMetadata(question.getRib()));

    Set<String> matchingNodes =
        question.getNodeSpecifier().resolve(_batfish.specifierContext(snapshot));
    Prefix network = question.getNetwork();
    RoutingProtocolSpecifier protocolSpec = question.getRoutingProtocolSpecifier();
    String vrfRegex = question.getVrfs();
    Set<BgpRouteStatus> expandedBgpRouteStatuses =
        SpecifierFactories.getEnumSetSpecifierOrDefault(
                question.getBgpRouteStatus(),
                Grammar.BGP_ROUTE_STATUS_SPECIFIER,
                new ConstantEnumSetSpecifier<>(ImmutableSet.of(BgpRouteStatus.BEST)))
            .resolve();

    Multiset<Row> rows;
    Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>>
        routesGroupedByKeyInBase;
    Map<RouteRowKey, Map<RouteRowSecondaryKey, SortedSet<RouteRowAttribute>>>
        routesGroupedByKeyInDelta;
    Map<Ip, Set<String>> ipOwners;
    DataPlane dp;

    List<DiffRoutesOutput> routesDiffRaw;

    switch (question.getRib()) {
      case BGP:
        dp = _batfish.loadDataPlane(snapshot);
        routesGroupedByKeyInBase =
            groupBgpRoutes(
                expandedBgpRouteStatuses.contains(BEST) ? dp.getBgpRoutes() : null,
                expandedBgpRouteStatuses.contains(BACKUP) ? dp.getBgpBackupRoutes() : null,
                matchingNodes,
                vrfRegex,
                network,
                protocolSpec);

        dp = _batfish.loadDataPlane(reference);
        routesGroupedByKeyInDelta =
            groupBgpRoutes(
                expandedBgpRouteStatuses.contains(BEST) ? dp.getBgpRoutes() : null,
                expandedBgpRouteStatuses.contains(BACKUP) ? dp.getBgpBackupRoutes() : null,
                matchingNodes,
                vrfRegex,
                network,
                protocolSpec);
        routesDiffRaw = getRoutesDiff(routesGroupedByKeyInBase, routesGroupedByKeyInDelta);
        rows = getBgpRouteRowsDiff(routesDiffRaw, RibProtocol.BGP);
        break;

      case MAIN:
      default:
        dp = _batfish.loadDataPlane(snapshot);
        ipOwners = computeIpNodeOwners(_batfish.loadConfigurations(snapshot), true);
        routesGroupedByKeyInBase =
            groupRoutes(dp.getRibs(), matchingNodes, network, vrfRegex, protocolSpec, ipOwners);

        dp = _batfish.loadDataPlane(reference);
        ipOwners = computeIpNodeOwners(_batfish.loadConfigurations(reference), true);
        routesGroupedByKeyInDelta =
            groupRoutes(dp.getRibs(), matchingNodes, network, vrfRegex, protocolSpec, ipOwners);

        routesDiffRaw = getRoutesDiff(routesGroupedByKeyInBase, routesGroupedByKeyInDelta);
        rows = getAbstractRouteRowsDiff(routesDiffRaw);
    }

    diffAnswer.postProcessAnswer(_question, rows);
    return diffAnswer;
  }

  /** Generate the table metadata based on the {@code rib} we are pulling */
  @VisibleForTesting
  static TableMetadata getTableMetadata(RibProtocol rib) {
    ImmutableList.Builder<ColumnMetadata> columnBuilder = ImmutableList.builder();
    addCommonTableColumnsAtStart(columnBuilder);
    switch (rib) {
      case EVPN:
        columnBuilder
            .add(
                new ColumnMetadata(
                    COL_STATUS,
                    Schema.list(Schema.STRING),
                    "Route's statuses",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_ROUTE_DISTINGUISHER,
                    Schema.STRING,
                    "Route distinguisher",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_NEXT_HOP_IP, Schema.IP, "Route's Next Hop IP", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_NEXT_HOP_INTERFACE,
                    Schema.STRING,
                    "Route's Next Hop Interface",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_PROTOCOL, Schema.STRING, "Route's Protocol", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_AS_PATH, Schema.STRING, "Route's AS path", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_METRIC, Schema.LONG, "Route's Metric", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_LOCAL_PREF,
                    Schema.LONG,
                    "Route's Local Preference",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_COMMUNITIES,
                    Schema.list(Schema.STRING),
                    "Route's List of communities",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_ORIGIN_PROTOCOL,
                    Schema.STRING,
                    "Route's Origin protocol",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_ORIGIN_TYPE,
                    Schema.STRING,
                    "Route's Origin type",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_ORIGINATOR_ID,
                    Schema.STRING,
                    "Route's Originator ID",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_CLUSTER_LIST,
                    Schema.list(Schema.LONG),
                    "Route's Cluster List",
                    Boolean.FALSE,
                    Boolean.TRUE));
        break;
      case BGP:
        columnBuilder
            .add(
                new ColumnMetadata(
                    COL_STATUS,
                    Schema.list(Schema.STRING),
                    "Route's statuses",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_NEXT_HOP_IP, Schema.IP, "Route's Next Hop IP", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_NEXT_HOP_INTERFACE,
                    Schema.STRING,
                    "Route's Next Hop Interface",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_PROTOCOL, Schema.STRING, "Route's Protocol", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_AS_PATH, Schema.STRING, "Route's AS path", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_METRIC, Schema.LONG, "Route's Metric", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_LOCAL_PREF,
                    Schema.LONG,
                    "Route's Local Preference",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_COMMUNITIES,
                    Schema.list(Schema.STRING),
                    "Route's List of communities",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_ORIGIN_PROTOCOL,
                    Schema.STRING,
                    "Route's Origin protocol",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_ORIGIN_TYPE,
                    Schema.STRING,
                    "Route's Origin type",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_ORIGINATOR_ID,
                    Schema.STRING,
                    "Route's Originator ID",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_CLUSTER_LIST,
                    Schema.list(Schema.LONG),
                    "Route's Cluster List",
                    Boolean.FALSE,
                    Boolean.TRUE));
        break;
      case MAIN:
      default:
        columnBuilder
            .add(
                new ColumnMetadata(
                    COL_NEXT_HOP,
                    Schema.STRING,
                    "Inferred hostname of the next hop",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_NEXT_HOP_IP, Schema.IP, "Route's Next Hop IP", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_NEXT_HOP_INTERFACE,
                    Schema.STRING,
                    "Route's Next Hop Interface",
                    Boolean.FALSE,
                    Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_PROTOCOL, Schema.STRING, "Route's Protocol", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_METRIC, Schema.LONG, "Route's Metric", Boolean.FALSE, Boolean.TRUE))
            .add(
                new ColumnMetadata(
                    COL_ADMIN_DISTANCE,
                    Schema.INTEGER,
                    "Route's Admin distance",
                    Boolean.FALSE,
                    Boolean.TRUE));
    }
    addCommonTableColumnsAtEnd(columnBuilder);
    return new TableMetadata(columnBuilder.build(), "Display RIB routes");
  }

  /** Generate table columns that should be always present, at the end of table. */
  private static void addCommonTableColumnsAtEnd(
      ImmutableList.Builder<ColumnMetadata> columnBuilder) {
    columnBuilder.add(
        new ColumnMetadata(
            COL_TAG, Schema.LONG, "Tag for this route", Boolean.FALSE, Boolean.TRUE));
  }

  /** Generate table columns that should be always present, at the start of table. */
  private static void addCommonTableColumnsAtStart(
      ImmutableList.Builder<ColumnMetadata> columnBuilder) {
    columnBuilder.add(
        new ColumnMetadata(COL_NODE, Schema.NODE, "Node", Boolean.TRUE, Boolean.FALSE));
    columnBuilder.add(
        new ColumnMetadata(COL_VRF_NAME, Schema.STRING, "VRF name", Boolean.TRUE, Boolean.FALSE));
    columnBuilder.add(
        new ColumnMetadata(
            COL_NETWORK, Schema.PREFIX, "Network for this route", Boolean.TRUE, Boolean.TRUE));
  }

  /** Generate the table metadata based on the {@code rib} we are pulling */
  @VisibleForTesting
  static TableMetadata getDiffTableMetadata(RibProtocol rib) {
    ImmutableList.Builder<ColumnMetadata> columnBuilder = ImmutableList.builder();
    addCommonTableColumnsAtStart(columnBuilder);
    columnBuilder.add(
        new ColumnMetadata(
            COL_ROUTE_ENTRY_PRESENCE,
            Schema.STRING,
            "Presence of a Route for the given Network (Prefix)",
            Boolean.FALSE,
            Boolean.TRUE));
    switch (rib) {
      case BGP:
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_STATUS,
                Schema.list(Schema.STRING),
                "Route's statuses",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_STATUS,
                Schema.list(Schema.STRING),
                "Route's statuses",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_NEXT_HOP_IP,
                Schema.IP,
                "Route's Next Hop IP",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_NEXT_HOP_IP,
                Schema.IP,
                "Route's Next Hop IP",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_PROTOCOL,
                Schema.STRING,
                "Route's Protocol",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_PROTOCOL,
                Schema.STRING,
                "Route's Protocol",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_AS_PATH,
                Schema.STRING,
                "Route's AS path",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_AS_PATH,
                Schema.STRING,
                "Route's AS path",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_METRIC,
                Schema.LONG,
                "Route's Metric",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_METRIC,
                Schema.LONG,
                "Route's Metric",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_LOCAL_PREF,
                Schema.LONG,
                "Route's Local Preference",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_LOCAL_PREF,
                Schema.LONG,
                "Route's Local Preference",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_COMMUNITIES,
                Schema.list(Schema.STRING),
                "Route's List of communities",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_COMMUNITIES,
                Schema.list(Schema.STRING),
                "Route's List of communities",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_ORIGIN_PROTOCOL,
                Schema.STRING,
                "Route's Origin protocol",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_ORIGIN_PROTOCOL,
                Schema.STRING,
                "Route's Origin protocol",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_ORIGIN_TYPE,
                Schema.STRING,
                "Route's Origin type",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_ORIGIN_TYPE,
                Schema.STRING,
                "Route's Origin type",
                Boolean.FALSE,
                Boolean.TRUE));
        break;
      case MAIN:
      default:
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_NEXT_HOP,
                Schema.STRING,
                "Route's Next Hop's Hostname",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_NEXT_HOP,
                Schema.STRING,
                "Route's Next Hop's Hostname",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_NEXT_HOP_IP,
                Schema.IP,
                "Route's Next Hop IP",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_NEXT_HOP_IP,
                Schema.IP,
                "Route's Next Hop IP",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_PROTOCOL,
                Schema.STRING,
                "Route's Protocol",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_PROTOCOL,
                Schema.STRING,
                "Route's Protocol",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_NEXT_HOP_INTERFACE,
                Schema.STRING,
                "Route's Next Hop Interface",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_NEXT_HOP_INTERFACE,
                Schema.STRING,
                "Route's Next Hop Interface",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_METRIC,
                Schema.LONG,
                "Route's Metric",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_METRIC,
                Schema.LONG,
                "Route's Metric",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_BASE_PREFIX + COL_ADMIN_DISTANCE,
                Schema.INTEGER,
                "Route's Admin distance",
                Boolean.FALSE,
                Boolean.TRUE));
        columnBuilder.add(
            new ColumnMetadata(
                COL_DELTA_PREFIX + COL_ADMIN_DISTANCE,
                Schema.INTEGER,
                "Route's Admin distance",
                Boolean.FALSE,
                Boolean.TRUE));
    }
    columnBuilder.add(
        new ColumnMetadata(
            COL_BASE_PREFIX + COL_TAG,
            Schema.LONG,
            "Tag for this route",
            Boolean.FALSE,
            Boolean.TRUE));
    columnBuilder.add(
        new ColumnMetadata(
            COL_DELTA_PREFIX + COL_TAG,
            Schema.LONG,
            "Tag for this route",
            Boolean.FALSE,
            Boolean.TRUE));

    return new TableMetadata(columnBuilder.build(), "Display diff of RIB routes");
  }
}

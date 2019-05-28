package org.batfish.question.findmatchingfilterlines;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.toHeaderSpaceBuilder;
import static org.batfish.datamodel.table.TableMetadata.toColumnMap;
import static org.batfish.question.FilterQuestionUtils.getSpecifiedFilters;
import static org.batfish.question.findmatchingfilterlines.FindMatchingFilterLinesQuestion.PROP_IGNORE_COMPOSITES;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.common.Answerer;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.common.bdd.MemoizedIpAccessListToBdd;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.question.findmatchingfilterlines.FindMatchingFilterLinesQuestion.Action;
import org.batfish.specifier.ConstantIpSpaceSpecifier;
import org.batfish.specifier.IpSpaceAssignment.Entry;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.SpecifierFactories;

/** Answerer for FindMatchingFilterLinesQuestion */
@ParametersAreNonnullByDefault
public final class FindMatchingFilterLinesAnswerer extends Answerer {
  public static final String COL_NODE = "Node";
  public static final String COL_FILTER = "Filter";
  public static final String COL_LINE = "Line";
  public static final String COL_LINE_INDEX = "Line_Index";
  public static final String COL_ACTION = "Action";

  public static final List<ColumnMetadata> COLUMN_METADATA =
      ImmutableList.of(
          new ColumnMetadata(COL_NODE, Schema.STRING, "Node", true, false),
          new ColumnMetadata(COL_FILTER, Schema.STRING, "Filter name", true, false),
          new ColumnMetadata(COL_LINE, Schema.STRING, "Line text", true, false),
          new ColumnMetadata(COL_LINE_INDEX, Schema.INTEGER, "Index of line", true, false),
          new ColumnMetadata(
              COL_ACTION,
              Schema.STRING,
              "Action performed by the line (e.g., PERMIT or DENY)",
              true,
              false));

  private static final Map<String, ColumnMetadata> METADATA_MAP = toColumnMap(COLUMN_METADATA);

  FindMatchingFilterLinesAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  @Override
  public TableAnswerElement answer() {
    FindMatchingFilterLinesQuestion question = (FindMatchingFilterLinesQuestion) _question;

    SpecifierContext ctxt = _batfish.specifierContext();
    Multimap<String, String> specifiedAcls =
        getSpecifiedFilters(
            ctxt,
            question.getNodeSpecifier(),
            question.getFilterSpecifier(),
            question.getIgnoreComposites());

    // Throw if no filters matched
    if (specifiedAcls.values().isEmpty()) {
      throw new IllegalArgumentException(
          String.format(
              "Did not find any filters that meet the specified criteria. (Tips: Set '%s' to false if you want to analyze combined filters; use 'resolveFilterSpecifier' question to see which filters your nodes and filters match.)",
              PROP_IGNORE_COMPOSITES));
    }

    TableAnswerElement answer = new TableAnswerElement(createMetadata(question));
    getRows(question.getHeaderConstraints(), question.getAction(), specifiedAcls, ctxt)
        .forEach(answer::addRow);
    return answer;
  }

  @VisibleForTesting
  static List<Row> getRows(
      PacketHeaderConstraints phc,
      @Nullable Action action,
      Multimap<String, String> acls,
      SpecifierContext ctxt) {
    Map<String, Configuration> configs = ctxt.getConfigs();
    List<Row> rows = new ArrayList<>();

    HeaderSpace headerSpace =
        toHeaderSpaceBuilder(phc)
            .setSrcIps(resolveIpSpace(phc.getSrcIps(), ctxt))
            .setDstIps(resolveIpSpace(phc.getDstIps(), ctxt))
            .build();
    BDDPacket bddPacket = new BDDPacket();
    BDD headerSpaceBdd = toHeaderSpaceBdd(headerSpace, bddPacket);

    Map<String, BDDSourceManager> mgrMap = BDDSourceManager.forNetwork(bddPacket, configs);

    for (String nodeName : acls.keySet()) {
      rows.addAll(
          getRowsForNode(
              configs.get(nodeName),
              bddPacket,
              mgrMap.get(nodeName),
              acls.get(nodeName),
              headerSpaceBdd,
              action));
    }
    return rows;
  }

  private static List<Row> getRowsForNode(
      Configuration node,
      BDDPacket bddPacket,
      BDDSourceManager mgr,
      Collection<String> acls,
      BDD headerSpaceBdd,
      @Nullable Action action) {
    List<Row> rows = new ArrayList<>();
    String nodeName = node.getHostname();
    MemoizedIpAccessListToBdd bddConverter =
        new MemoizedIpAccessListToBdd(bddPacket, mgr, node.getIpAccessLists(), node.getIpSpaces());
    for (String aclName : acls) {
      List<IpAccessListLine> aclLines = node.getIpAccessLists().get(aclName).getLines();
      getRowsForAcl(aclLines, headerSpaceBdd, bddConverter, action)
          .forEach(
              lineIndex -> {
                IpAccessListLine line = aclLines.get(lineIndex);
                rows.add(
                    Row.builder(METADATA_MAP)
                        .put(COL_NODE, nodeName)
                        .put(COL_FILTER, aclName)
                        .put(COL_LINE, firstNonNull(line.getName(), line.toString()))
                        .put(COL_LINE_INDEX, lineIndex)
                        .put(COL_ACTION, line.getAction())
                        .build());
              });
    }
    return rows;
  }

  /**
   * Returns the indices of the lines in the given list of {@link IpAccessListLine}s that match the
   * given {@code headerSpaceBdd} and {@link Action}.
   */
  @VisibleForTesting
  static List<Integer> getRowsForAcl(
      List<IpAccessListLine> aclLines,
      BDD headerSpaceBdd,
      IpAccessListToBdd bddConverter,
      @Nullable Action action) {
    List<Integer> rowNumbers = new ArrayList<>();
    for (int lineIndex = 0; lineIndex < aclLines.size(); lineIndex++) {
      IpAccessListLine line = aclLines.get(lineIndex);
      if (!actionMatches(action, line.getAction())) {
        continue;
      }
      // If there is any overlap between the header space BDD and this line, include it
      BDD lineBdd = bddConverter.toBdd(line.getMatchCondition());
      if (!headerSpaceBdd.and(lineBdd).isZero()) {
        rowNumbers.add(lineIndex);
      }
    }
    return rowNumbers;
  }

  /** Creates {@link TableMetadata} from the question. */
  private static TableMetadata createMetadata(Question question) {
    String textDesc =
        String.format(
            "Filter {%s} on node {%s} has matching line at index {%s}: {%s}",
            COL_FILTER, COL_NODE, COL_LINE_INDEX, COL_LINE);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    return new TableMetadata(COLUMN_METADATA, textDesc);
  }

  private static IpSpace resolveIpSpace(@Nullable String ips, SpecifierContext ctx) {
    IpSpaceSpecifier specifier =
        SpecifierFactories.getIpSpaceSpecifierOrDefault(
            ips, new ConstantIpSpaceSpecifier(UniverseIpSpace.INSTANCE));
    return firstNonNull(
        AclIpSpace.union(
            specifier.resolve(ImmutableSet.of(), ctx).getEntries().stream()
                .map(Entry::getIpSpace)
                .collect(ImmutableList.toImmutableList())),
        EmptyIpSpace.INSTANCE);
  }

  private static boolean actionMatches(@Nullable Action action, LineAction lineAction) {
    return action == null
        || action == Action.PERMIT && lineAction == LineAction.PERMIT
        || action == Action.DENY && lineAction == LineAction.DENY;
  }

  @VisibleForTesting
  static BDD toHeaderSpaceBdd(HeaderSpace headerSpace, BDDPacket pkt) {
    AclLineMatchExpr headerSpaceMatcher = new MatchHeaderSpace(headerSpace);
    BDDSourceManager emptyMgr = BDDSourceManager.empty(pkt);
    return new IpAccessListToBddImpl(pkt, emptyMgr, ImmutableMap.of(), ImmutableMap.of())
        .toBdd(headerSpaceMatcher);
  }
}

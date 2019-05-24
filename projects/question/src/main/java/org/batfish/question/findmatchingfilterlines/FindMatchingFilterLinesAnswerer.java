package org.batfish.question.findmatchingfilterlines;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.datamodel.PacketHeaderConstraintsUtil.toHeaderSpaceBuilder;
import static org.batfish.datamodel.acl.SourcesReferencedByIpAccessLists.referencedSources;
import static org.batfish.datamodel.table.TableMetadata.toColumnMap;
import static org.batfish.question.FilterQuestionUtils.getSpecifiedFilters;
import static org.batfish.question.FilterQuestionUtils.resolveSources;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.sf.javabdd.BDD;
import org.batfish.common.Answerer;
import org.batfish.common.bdd.BDDPacket;
import org.batfish.common.bdd.BDDSourceManager;
import org.batfish.common.bdd.IpAccessListToBdd;
import org.batfish.common.bdd.IpAccessListToBddImpl;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IpAccessList;
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
import org.batfish.specifier.ConstantIpSpaceSpecifier;
import org.batfish.specifier.IpSpaceAssignment.Entry;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.LocationSpecifier;
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
          "Did not find any filters that meet the specified criteria. (Tips: Set 'ignoreGenerated' to false if you want to analyze combined filters; use 'resolveFilterSpecifier' question to see which filters your nodes and filters match.)");
    }

    TableAnswerElement answer = new TableAnswerElement(createMetadata(question));
    getRows(question.getHeaderConstraints(), question.getAction(), specifiedAcls, ctxt)
        .forEach(answer::addRow);
    return answer;
  }

  @VisibleForTesting
  static List<Row> getRows(
      PacketHeaderConstraints phc,
      @Nullable LineAction action,
      Multimap<String, String> acls,
      SpecifierContext ctxt) {
    Map<String, ColumnMetadata> metadataMap = toColumnMap(COLUMN_METADATA);
    Map<String, Configuration> configs = ctxt.getConfigs();
    List<Row> rows = new ArrayList<>();

    HeaderSpace headerSpace =
        toHeaderSpaceBuilder(phc)
            .setSrcIps(resolveIpSpace(phc.getSrcIps(), ctxt))
            .setDstIps(resolveIpSpace(phc.getDstIps(), ctxt))
            .build();
    AclLineMatchExpr headerSpaceMatcher = new MatchHeaderSpace(headerSpace);

    BDDPacket bddPacket = new BDDPacket();
    BDDSourceManager emptyMgr = BDDSourceManager.empty(bddPacket);
    BDD headerSpaceBdd =
        new IpAccessListToBddImpl(bddPacket, emptyMgr, ImmutableMap.of(), ImmutableMap.of())
            .toBdd(headerSpaceMatcher);

    for (String nodeName : acls.keySet()) {
      Configuration node = configs.get(nodeName);
      Set<String> inactiveIfaces =
          Sets.difference(node.getAllInterfaces().keySet(), node.activeInterfaces());
      Set<String> activeSources =
          Sets.difference(
              resolveSources(ctxt, LocationSpecifier.ALL_LOCATIONS, node.getHostname()),
              inactiveIfaces);
      for (String aclName : acls.get(nodeName)) {
        IpAccessList acl = node.getIpAccessLists().get(aclName);
        Set<String> referencedSources = referencedSources(node.getIpAccessLists(), acl);
        BDDSourceManager mgr =
            BDDSourceManager.forSources(bddPacket, activeSources, referencedSources);
        if (mgr.isValidValue().isZero()) {
          continue;
        }

        IpAccessListToBdd bddConverter =
            new IpAccessListToBddImpl(bddPacket, mgr, node.getIpAccessLists(), node.getIpSpaces());

        List<IpAccessListLine> lines = acl.getLines();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
          IpAccessListLine line = lines.get(lineIndex);
          if (action != null && action != line.getAction()) {
            continue;
          }
          BDD lineBdd = bddConverter.toBdd(line.getMatchCondition());

          // If there is any overlap between the header space BDD and this line, add it to getRows
          if (!headerSpaceBdd.and(lineBdd).isZero()) {
            rows.add(
                Row.builder(metadataMap)
                    .put(COL_NODE, nodeName)
                    .put(COL_FILTER, acl.getName())
                    .put(COL_LINE, firstNonNull(line.getName(), line.toString()))
                    .put(COL_LINE_INDEX, lineIndex)
                    .put(COL_ACTION, line.getAction())
                    .build());
          }
        }
      }
    }
    return rows;
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
}

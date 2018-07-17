package org.batfish.question.traceroute;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.batfish.common.Answerer;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.FlowHistory;
import org.batfish.datamodel.FlowHistory.FlowHistoryInfo;
import org.batfish.datamodel.FlowTrace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.answers.AnswerElement;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.datamodel.visitors.IpSpaceRepresentative;
import org.batfish.specifier.InterfaceLinkLocation;
import org.batfish.specifier.InterfaceLocation;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.IpSpaceSpecifierFactory;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationSpecifier;
import org.batfish.specifier.LocationSpecifierFactory;
import org.batfish.specifier.LocationVisitor;
import org.batfish.specifier.SpecifierContext;

public final class TracerouteAnswerer extends Answerer {
  static final String COL_DST_IP = "dstIp";
  static final String COL_FLOW = "flow";
  static final String COL_NODE = "node";
  static final String COL_NUM_PATHS = "numPaths";
  static final String COL_PATHS = "paths";
  static final String COL_RESULTS = "results";

  private final Map<String, Configuration> _configurations;
  private final IpSpaceRepresentative _ipSpaceRepresentative;
  private final IpSpaceAssignment _sourceIpAssignment;

  TracerouteAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);

    _configurations = batfish.loadConfigurations();
    _ipSpaceRepresentative = IpSpaceRepresentative.load();
    _sourceIpAssignment = initSourceIpAssignment();
  }

  @VisibleForTesting
  IpSpaceAssignment initSourceIpAssignment() {
    /* construct specifiers */
    TracerouteQuestion tracerouteQuestion = (TracerouteQuestion) _question;
    LocationSpecifier sourceLocationSpecifier =
        LocationSpecifierFactory.load(tracerouteQuestion.getSourceLocationSpecifierFactory())
            .buildLocationSpecifier(tracerouteQuestion.getSourceLocationSpecifierInput());

    IpSpaceSpecifier sourceIpSpaceSpecifier =
        IpSpaceSpecifierFactory.load(tracerouteQuestion.getSourceIpSpaceSpecifierFactory())
            .buildIpSpaceSpecifier(tracerouteQuestion.getSourceIpSpaceSpecifierInput());

    /* resolve specifiers */
    SpecifierContext ctxt = _batfish.specifierContext();
    Set<Location> sourceLocations = sourceLocationSpecifier.resolve(ctxt);
    return sourceIpSpaceSpecifier.resolve(sourceLocations, ctxt);
  }

  @Override
  public AnswerElement answer() {
    String tag = _batfish.getFlowTag();
    Set<Flow> flows = getFlows(tag);
    _batfish.processFlows(flows, ((TracerouteQuestion) _question).getIgnoreAcls());
    FlowHistory flowHistory = _batfish.getHistory();
    Multiset<Row> rows = flowHistoryToRows(flowHistory);
    TableAnswerElement table = new TableAnswerElement(createMetadata());
    table.postProcessAnswer(_question, rows);
    return table;
  }

  public static TableMetadata createMetadata() {
    List<ColumnMetadata> columnMetadata =
        ImmutableList.of(
            new ColumnMetadata(COL_NODE, Schema.NODE, "Ingress node", true, false),
            new ColumnMetadata(COL_DST_IP, Schema.IP, "Destination IP of the packet", true, false),
            new ColumnMetadata(COL_FLOW, Schema.FLOW, "The flow", true, false),
            new ColumnMetadata(COL_NUM_PATHS, Schema.INTEGER, "The number of paths", false, true),
            new ColumnMetadata(
                COL_RESULTS, Schema.set(Schema.STRING), "The set of outcomes", false, true),
            new ColumnMetadata(COL_PATHS, Schema.set(Schema.FLOW_TRACE), "The paths", false, true));

    DisplayHints dhints = new DisplayHints();
    dhints.setTextDesc(String.format("Paths for flow ${%s}", COL_FLOW));

    return new TableMetadata(columnMetadata, dhints);
  }

  /**
   * Converts {@code FlowHistoryInfo} into {@link Row}. Expects that the history object contains
   * traces for only one environment
   */
  static Row flowHistoryToRow(FlowHistoryInfo historyInfo) {
    // there should be only environment in this object
    checkArgument(
        historyInfo.getPaths().size() == 1,
        String.format(
            "Expect only one environment in flow history info. Found %d",
            historyInfo.getPaths().size()));
    Set<FlowTrace> paths =
        historyInfo.getPaths().values().stream().findAny().orElseGet(ImmutableSet::of);
    Set<String> results =
        paths.stream().map(path -> path.getDisposition().toString()).collect(Collectors.toSet());
    return Row.of(
        COL_NODE,
        new Node(historyInfo.getFlow().getIngressNode()),
        COL_DST_IP,
        historyInfo.getFlow().getDstIp(),
        COL_FLOW,
        historyInfo.getFlow(),
        COL_NUM_PATHS,
        paths.size(),
        COL_RESULTS,
        results,
        COL_PATHS,
        paths);
  }

  /**
   * Converts a flowHistory object into a set of Rows. Expects that the traces correspond to only
   * one environment.
   */
  public static Multiset<Row> flowHistoryToRows(FlowHistory flowHistory) {
    Multiset<Row> rows = LinkedHashMultiset.create();
    for (FlowHistoryInfo historyInfo : flowHistory.getTraces().values()) {
      rows.add(flowHistoryToRow(historyInfo));
    }
    return rows;
  }

  @VisibleForTesting
  Set<Flow> getFlows(String tag) {
    TracerouteQuestion question = (TracerouteQuestion) _question;
    ImmutableSet.Builder<Flow> setBuilder = ImmutableSet.builder();
    for (Flow.Builder flowBuilder : question.getFlowBuilders()) {
      flowBuilder.setTag(tag);
      setDstIp(flowBuilder);
      for (IpSpaceAssignment.Entry entry : _sourceIpAssignment.getEntries()) {
        Optional<Ip> optionalSrcIp = _ipSpaceRepresentative.getRepresentative(entry.getIpSpace());
        flowBuilder.setSrcIp(optionalSrcIp.orElse(Ip.ZERO));
        for (Location loc : entry.getLocations()) {
          setSourceLocation(flowBuilder, loc);
          setBuilder.add(flowBuilder.build());
        }
      }
    }
    return setBuilder.build();
  }

  private void setDstIp(Flow.Builder flowBuilder) {
    TracerouteQuestion question = (TracerouteQuestion) _question;
    if (flowBuilder.getDstIp().equals(Ip.AUTO)) {
      flowBuilder.setDstIp(question.createDstIpFromDst(_configurations));
    }
  }

  private void setSourceLocation(Flow.Builder flowBuilder, Location loc) {
    loc.accept(
        new LocationVisitor<Void>() {
          @Override
          public Void visitInterfaceLinkLocation(
              @Nonnull InterfaceLinkLocation interfaceLinkLocation) {
            flowBuilder
                .setIngressInterface(interfaceLinkLocation.getInterfaceName())
                .setIngressNode(interfaceLinkLocation.getNodeName())
                .setIngressVrf(null);
            return null;
          }

          @Override
          public Void visitInterfaceLocation(@Nonnull InterfaceLocation interfaceLocation) {
            flowBuilder
                .setIngressInterface(null)
                .setIngressNode(interfaceLocation.getNodeName())
                .setIngressVrf(
                    interfaceVrf(
                        interfaceLocation.getNodeName(), interfaceLocation.getInterfaceName()));
            return null;
          }
        });
  }

  private String interfaceVrf(String node, String iface) {
    return _configurations.get(node).getInterfaces().get(iface).getVrf().getName();
  }
}

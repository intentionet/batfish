package org.batfish.question.testfilters;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.datamodel.SetFlowStartLocation.setStartLocation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.batfish.common.Answerer;
import org.batfish.common.BatfishException;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.FilterResult;
import org.batfish.datamodel.Flow;
import org.batfish.datamodel.Flow.Builder;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.PacketHeaderConstraints;
import org.batfish.datamodel.PacketHeaderConstraintsUtil;
import org.batfish.datamodel.acl.AclTrace;
import org.batfish.datamodel.acl.AclTracer;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.datamodel.visitors.IpSpaceRepresentative;
import org.batfish.specifier.FilterSpecifier;
import org.batfish.specifier.InferFromLocationIpSpaceSpecifier;
import org.batfish.specifier.IpSpaceAssignment;
import org.batfish.specifier.IpSpaceAssignment.Entry;
import org.batfish.specifier.IpSpaceSpecifier;
import org.batfish.specifier.Location;
import org.batfish.specifier.LocationSpecifier;
import org.batfish.specifier.LocationVisitor;
import org.batfish.specifier.SpecifierContext;
import org.batfish.specifier.SpecifierFactories;

public class TestFiltersAnswerer extends Answerer {

  public static final String COL_NODE = "Node";
  public static final String COL_FILTER_NAME = "Filter_Name";
  public static final String COL_FLOW = "Flow";
  public static final String COL_ACTION = "Action";
  public static final String COL_LINE_CONTENT = "Line_Content";
  public static final String COL_TRACE = "Trace";
  private static final Ip DEFAULT_IP_ADDRESS = Ip.parse("8.8.8.8");

  private final IpSpaceRepresentative _ipSpaceRepresentative;
  private final IpSpaceAssignment _sourceIpAssignment;

  public TestFiltersAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
    _ipSpaceRepresentative = new IpSpaceRepresentative();
    _sourceIpAssignment =
        initSourceIpAssignment((TestFiltersQuestion) question, batfish.specifierContext());
  }

  private static IpSpaceAssignment initSourceIpAssignment(
      TestFiltersQuestion question, SpecifierContext ctxt) {
    /* construct specifiers */
    LocationSpecifier sourceLocationSpecifier = question.getStartLocationSpecifier();

    IpSpaceSpecifier sourceIpSpaceSpecifier =
        SpecifierFactories.getIpSpaceSpecifierOrDefault(
            question.getHeaders().getSrcIps(), InferFromLocationIpSpaceSpecifier.INSTANCE);

    /* resolve specifiers */
    Set<Location> sourceLocations = sourceLocationSpecifier.resolve(ctxt);
    return sourceIpSpaceSpecifier.resolve(sourceLocations, ctxt);
  }

  /**
   * Creates a {@link TableAnswerElement} object containing the right metadata
   *
   * @param question The question object for which the answer is being created, to borrow its {@link
   *     DisplayHints}
   * @return The creates the answer element object.
   */
  public static TableAnswerElement create(TestFiltersQuestion question) {
    List<ColumnMetadata> columnMetadata =
        ImmutableList.of(
            new ColumnMetadata(COL_NODE, Schema.NODE, "Node", true, false),
            new ColumnMetadata(COL_FILTER_NAME, Schema.STRING, "Filter name", true, false),
            new ColumnMetadata(COL_FLOW, Schema.FLOW, "Evaluated flow", true, false),
            new ColumnMetadata(COL_ACTION, Schema.STRING, "Outcome", false, true),
            new ColumnMetadata(COL_LINE_CONTENT, Schema.STRING, "Line content", false, true),
            new ColumnMetadata(COL_TRACE, Schema.ACL_TRACE, "ACL trace", false, true));
    String textDesc =
        String.format(
            "Filter ${%s} on node ${%s} will ${%s} flow ${%s} at line ${%s}",
            COL_FILTER_NAME, COL_NODE, COL_ACTION, COL_FLOW, COL_LINE_CONTENT);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    TableMetadata metadata = new TableMetadata(columnMetadata, textDesc);
    return new TableAnswerElement(metadata);
  }

  @Override
  public TableAnswerElement answer() {
    Multiset<Row> rows = getRows();

    TestFiltersQuestion question = (TestFiltersQuestion) _question;
    TableAnswerElement answer = create(question);
    answer.postProcessAnswer(question, rows);
    return answer;
  }

  private SortedSet<Flow> getFlows(Configuration c, ImmutableSet.Builder<String> allProblems) {
    TestFiltersQuestion question = (TestFiltersQuestion) _question;
    String node = c.getHostname();
    Set<Location> srcLocations =
        question.getStartLocationSpecifier().resolve(_batfish.specifierContext()).stream()
            .filter(LocationVisitor.onNode(node)::visit)
            .collect(Collectors.toSet());

    ImmutableSortedSet.Builder<Flow> setBuilder = ImmutableSortedSet.naturalOrder();

    // this will happen if the node has no interfaces, and someone is just testing their ACLs
    if (srcLocations.isEmpty() && question.getStartLocation() == null) {
      try {
        Flow.Builder flowBuilder = headerConstraintsToFlow(question.getHeaders(), null);
        flowBuilder.setIngressNode(node);
        flowBuilder.setIngressInterface(null);
        flowBuilder.setIngressVrf(
            Configuration.DEFAULT_VRF_NAME); // dummy because Flow needs non-null interface or vrf
        flowBuilder.setTag("FlowTag"); // dummy tag; consistent tags enable flow diffs
        setBuilder.add(flowBuilder.build());
      } catch (IllegalArgumentException e) {
        allProblems.add(e.getMessage());
      }
    }

    // Perform cross-product of all locations to flows
    for (Location srcLocation : srcLocations) {
      try {
        Flow.Builder flowBuilder = headerConstraintsToFlow(question.getHeaders(), srcLocation);
        setStartLocation(ImmutableMap.of(node, c), flowBuilder, srcLocation);
        flowBuilder.setTag("FlowTag"); // dummy tag; consistent tags enable flow diffs
        setBuilder.add(flowBuilder.build());
      } catch (IllegalArgumentException e) {
        // record this error but try to keep going
        allProblems.add(e.getMessage());
      }
    }

    return setBuilder.build();
  }

  /**
   * Returns a {@link Row} with results from injecting {@code flow} into {@code filter} at node
   * represented by {@code c}.
   */
  public static Row getRow(IpAccessList filter, Flow flow, Configuration c) {
    AclTrace trace =
        AclTracer.trace(
            filter,
            flow,
            flow.getIngressInterface(),
            c.getIpAccessLists(),
            c.getIpSpaces(),
            c.getIpSpaceMetadata());
    FilterResult result =
        filter.filter(flow, flow.getIngressInterface(), c.getIpAccessLists(), c.getIpSpaces());
    Integer matchLine = result.getMatchLine();
    String lineDesc = "no-match";
    if (matchLine != null) {
      lineDesc = filter.getLines().get(matchLine).getName();
      if (lineDesc == null) {
        lineDesc = "line:" + matchLine;
      }
    }
    return Row.builder()
        .put(COL_NODE, new Node(c.getHostname()))
        .put(COL_FILTER_NAME, filter.getName())
        .put(COL_FLOW, flow)
        .put(COL_ACTION, result.getAction())
        .put(COL_LINE_CONTENT, lineDesc)
        .put(COL_TRACE, trace)
        .build();
  }

  @VisibleForTesting
  Multiset<Row> getRows() {
    TestFiltersQuestion question = (TestFiltersQuestion) _question;
    Map<String, Configuration> configurations = _batfish.loadConfigurations();
    SortedSet<String> includeNodes =
        ImmutableSortedSet.copyOf(question.getNodeSpecifier().resolve(_batfish.specifierContext()));
    FilterSpecifier filterSpecifier = question.getFilterSpecifier();

    Multiset<Row> rows = HashMultiset.create();

    // collect all errors while building flows; return this set when no valid flow is found
    ImmutableSet.Builder<String> allProblems = ImmutableSet.builder();

    // keep track of whether any matching filters have been found; if none get found, throw error
    boolean foundMatchingFilter = false;

    for (String node : includeNodes) {
      Configuration c = configurations.get(node);
      SortedSet<Flow> flows = getFlows(c, allProblems);

      // there should be another for loop for v6 filters when we add v6 support
      SortedSet<IpAccessList> filtersByName =
          ImmutableSortedSet.copyOf(
              Comparator.comparing(IpAccessList::getName),
              filterSpecifier.resolve(node, _batfish.specifierContext()));
      for (IpAccessList filter : filtersByName) {
        foundMatchingFilter = true;
        for (Flow flow : flows) {
          rows.add(getRow(filter, flow, c));
        }
      }
    }
    if (!foundMatchingFilter) {
      throw new BatfishException("No matching filters");
    }
    checkArgument(
        rows.size() > 0,
        "No valid flow found for specified parameters. Potential problems: %s",
        String.join(",", allProblems.build()));
    return rows;
  }

  /**
   * Generate a flow builder given some set of packet header constraints.
   *
   * @param constraints {@link PacketHeaderConstraints}
   * @throws IllegalArgumentException if the {@code constraints} cannot be resolved to a single
   *     value.
   */
  private Flow.Builder headerConstraintsToFlow(
      PacketHeaderConstraints constraints, Location srcLocation) throws IllegalArgumentException {
    Flow.Builder builder = PacketHeaderConstraintsUtil.toFlow(constraints);
    setSrcIp(constraints, srcLocation, builder);
    setDstIp(constraints, builder);

    // Set defaults for protocol, and ports and packet lengths:
    if (builder.getIpProtocol() == null || builder.getIpProtocol() == IpProtocol.IP) {
      builder.setIpProtocol(IpProtocol.TCP);
    }
    if (builder.getDstPort() == null) {
      builder.setDstPort(NamedPort.HTTP.number());
    }
    if (builder.getSrcPort() == null) {
      builder.setSrcPort(NamedPort.EPHEMERAL_LOWEST.number());
    }
    return builder;
  }

  private void setDstIp(PacketHeaderConstraints constraints, Builder builder) {
    String headerDstIp = constraints.getDstIps();
    if (headerDstIp != null) {
      IpSpaceSpecifier dstIpSpecifier =
          SpecifierFactories.getIpSpaceSpecifierOrDefault(
              headerDstIp, InferFromLocationIpSpaceSpecifier.INSTANCE);
      IpSpaceAssignment dstIps =
          dstIpSpecifier.resolve(ImmutableSet.of(), _batfish.specifierContext());
      // Filter out empty IP assignments
      ImmutableList<Entry> nonEmptyIpSpaces =
          dstIps.getEntries().stream()
              .filter(e -> !e.getIpSpace().equals(EmptyIpSpace.INSTANCE))
              .collect(ImmutableList.toImmutableList());
      checkArgument(
          nonEmptyIpSpaces.size() > 0,
          "At least one destination IP is required, could not resolve any");
      checkArgument(
          nonEmptyIpSpaces.size() == 1,
          "Specified destination: %s, resolves to more than one IP",
          headerDstIp);
      IpSpace space = nonEmptyIpSpaces.iterator().next().getIpSpace();
      Optional<Ip> dstIp = _ipSpaceRepresentative.getRepresentative(space);
      checkArgument(dstIp.isPresent(), "Specified destination: %s has no IPs", headerDstIp);
      builder.setDstIp(dstIp.get());
    } else {
      builder.setDstIp(DEFAULT_IP_ADDRESS);
    }
  }

  private void setSrcIp(
      PacketHeaderConstraints constraints, Location srcLocation, Builder builder) {
    // Extract source IP from header constraints,
    String headerSrcIp = constraints.getSrcIps();
    if (headerSrcIp != null) {
      // interpret given Src IP flexibly
      IpSpaceSpecifier srcIpSpecifier =
          SpecifierFactories.getIpSpaceSpecifierOrDefault(
              headerSrcIp, InferFromLocationIpSpaceSpecifier.INSTANCE);
      // Resolve to set of locations/IPs
      IpSpaceAssignment srcIps =
          srcIpSpecifier.resolve(ImmutableSet.of(), _batfish.specifierContext());
      // Filter out empty IP assignments
      ImmutableList<Entry> nonEmptyIpSpaces =
          srcIps.getEntries().stream()
              .filter(e -> !e.getIpSpace().equals(EmptyIpSpace.INSTANCE))
              .collect(ImmutableList.toImmutableList());
      checkArgument(
          nonEmptyIpSpaces.size() > 0, "At least one source IP is required, could not resolve any");
      checkArgument(
          nonEmptyIpSpaces.size() == 1,
          "Specified source IP %s resolves to more than one location/IP: %s",
          headerSrcIp,
          nonEmptyIpSpaces);
      // Pick a representative from the remaining space
      IpSpace space = nonEmptyIpSpaces.iterator().next().getIpSpace();
      Optional<Ip> srcIp = _ipSpaceRepresentative.getRepresentative(space);
      checkArgument(srcIp.isPresent(), "Specified source: %s has no IPs", headerSrcIp);
      builder.setSrcIp(srcIp.get());
    } else if (srcLocation == null) {
      builder.setSrcIp(DEFAULT_IP_ADDRESS);
    } else {
      // Use source location to determine header Src IP
      Optional<Entry> entry =
          _sourceIpAssignment.getEntries().stream()
              .filter(e -> e.getLocations().contains(srcLocation))
              .findFirst();

      checkArgument(
          entry.isPresent(), "Cannot resolve a source IP address from location %s", srcLocation);
      Optional<Ip> srcIp = _ipSpaceRepresentative.getRepresentative(entry.get().getIpSpace());
      checkArgument(
          srcIp.isPresent(),
          "At least one source IP is required, location %s produced none",
          srcLocation);
      builder.setSrcIp(srcIp.get());
    }
  }
}
